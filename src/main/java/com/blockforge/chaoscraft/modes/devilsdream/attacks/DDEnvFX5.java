package com.blockforge.chaoscraft.modes.devilsdream.attacks;

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
 * DevilsDream Mode — ENVIRONMENTAL ATTACKS 41–50.
 * Atmosphere/particle/sound focused: Plague Wind, Architect's Nightmare,
 * Celestial Wound, Drowning Room, Cursed Candelabra Array, Devil's Exhale,
 * Petrified Congregation, Temporal Shatter, Lullaby Machine, The Last Dream.
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
    //     Horizontal directional wind: warped_spore + ash + smoke blast
    //     across the arena. 12 ground item-displays (dead bushes, gravel)
    //     translate with the wind. Build-up roar -> blast -> fade.
    // ================================================================
    public static class PlagueWind extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> debris = new ArrayList<>();
        private double windAngle = 0;

        public PlagueWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plague_wind", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(25);
            config.setDamageDelayTicks(60);
            config.setDurationTicks(300);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            windAngle = Math.random() * Math.PI * 2;
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_AMBIENT, 1.4f, 0.4f);
            // 12 floor debris item-displays (alternating dead bush + gravel)
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12 + Math.random() * 0.4;
                double r = 2 + Math.random() * 5;
                Location p = center.clone().add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
                Material mat = (i % 2 == 0) ? Material.DEAD_BUSH : Material.GRAVEL;
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(mat));
                h.scale(0.6f, 0.6f, 0.6f).interpolation(20, 0);
                debris.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            // Phase: build-up (0-50), blast (50-220), fade (220-300)
            int phase = (tick < 50) ? 0 : (tick < 220) ? 1 : 2;
            double dx = Math.cos(windAngle);
            double dz = Math.sin(windAngle);

            if (phase == 0) {
                // light pre-wind whispers
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
                // BLAST — dense horizontal particles
                if (tick % 1 == 0) {
                    for (int i = 0; i < 28; i++) {
                        double off = (Math.random() - 0.5) * radius * 2;
                        // perpendicular spread
                        double px = -dz * off;
                        double pz = dx * off;
                        double sx = (Math.random() - 0.5) * radius * 0.8 * dx;
                        double sz = (Math.random() - 0.5) * radius * 0.8 * dz;
                        double y = 0.5 + Math.random() * 3.5;
                        Location p = getCenter().clone().add(px + sx - dx * radius, y, pz + sz - dz * radius);
                        w.spawnParticle(Particle.WARPED_SPORE, p, 2,
                                dx * 0.6, 0.0, dz * 0.6, 1.6);
                        w.spawnParticle(Particle.ASH, p, 2,
                                dx * 0.5, 0.05, dz * 0.5, 1.4);
                        w.spawnParticle(Particle.LARGE_SMOKE, p, 1,
                                dx * 0.4, 0.0, dz * 0.4, 1.0);
                    }
                }
                // shift debris with the wind via interpolation every 20t
                if (tick % 20 == 0) {
                    for (int i = 0; i < debris.size(); i++) {
                        float tx = (float) (dx * 0.6 * ((tick - 50) / 20.0));
                        float tz = (float) (dz * 0.6 * ((tick - 50) / 20.0));
                        debris.get(i).animateTo(
                                new Vector3f(tx - 0.5f + (i % 4) * 0.1f, 0f, tz - 0.5f + (i % 3) * 0.1f),
                                new AxisAngle4f((float) (windAngle + Math.sin(tick * 0.05 + i) * 0.4), 0, 1, 0),
                                new Vector3f(0.6f, 0.6f, 0.6f), 20);
                    }
                }
                if (tick % 18 == 0) {
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GHAST_AMBIENT, 1.6f, 0.4f);
                }
                if (tick % 35 == 0) {
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.5f);
                }
            } else {
                // fade haze
                if (tick % 3 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double ox = (Math.random() - 0.5) * radius * 2;
                        double oz = (Math.random() - 0.5) * radius * 2;
                        Location p = getCenter().clone().add(ox, 1 + Math.random() * 2, oz);
                        w.spawnParticle(Particle.ASH, p, 1, dx * 0.1, 0, dz * 0.1, 0.1);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new PlagueWind(plugin); }
    }

    // ================================================================
    // 42. THE ARCHITECT'S NIGHTMARE
    //     Half-built structures at wrong angles: 14 block displays
    //     (walls, stairs, doorways) phasing in/out. Construction smoke.
    //     Random stone item-displays as "dropped supplies".
    // ================================================================
    public static class ArchitectsNightmare extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> structure = new ArrayList<>();
        private final List<ItemDisplayHandle> supplies = new ArrayList<>();

        public ArchitectsNightmare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("architects_nightmare", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.5);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(800);
            config.setCooldownTicks(900);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 0.3f);
            Material[] palette = { Material.POLISHED_BLACKSTONE_BRICKS, Material.DEEPSLATE_BRICKS,
                    Material.DEEPSLATE_TILES, Material.NETHER_BRICKS, Material.TUFF };

            // 14 block displays at wrong angles around the arena
            for (int i = 0; i < 14; i++) {
                double a = Math.PI * 2 * i / 14;
                double r = 4 + Math.random() * 4;
                Location p = center.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 3, Math.sin(a) * r);
                Material m = palette[i % palette.length];
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, m);
                float scale = 0.7f + (float) Math.random() * 0.8f;
                float tilt = (float) (Math.random() * 0.6 - 0.3);
                h.scale(scale, scale * (0.6f + (float) Math.random() * 1.2f), scale)
                        .rotate(tilt, 0, 0, 1)
                        .rotate((float) (Math.random() * 0.4), 1, 0, 0)
                        .interpolation(20, 0);
                structure.add(h);
            }

            // 8 stone "supplies" item-displays
            Material[] items = { Material.COBBLESTONE, Material.STONE_BRICKS, Material.DEEPSLATE,
                    Material.BLACKSTONE };
            for (int i = 0; i < 8; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1 + Math.random() * 5;
                Location p = center.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(items[i % items.length]));
                h.scale(0.45f, 0.45f, 0.45f)
                        .rotate((float) (Math.random() * Math.PI * 2), 0, 1, 0)
                        .interpolation(20, 0);
                supplies.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // construction-edge smoke + ash from each structure
            if (tick % 4 == 0) {
                for (BlockDisplayHandle h : structure) {
                    if (h.entity() == null) continue;
                    Location pos = h.entity().getLocation();
                    w.spawnParticle(Particle.LARGE_SMOKE, pos.clone().add(0.5, 1.0, 0.5),
                            1, 0.3, 0.2, 0.3, 0.01);
                    w.spawnParticle(Particle.ASH, pos.clone().add(0.5, 0.8, 0.5),
                            2, 0.4, 0.3, 0.4, 0.02);
                }
            }

            // phase in/out random structure pieces
            if (tick % 30 == 0 && !structure.isEmpty()) {
                int idx = (int) (Math.random() * structure.size());
                BlockDisplayHandle h = structure.get(idx);
                boolean phaseOut = ((tick / 30) + idx) % 2 == 0;
                float s = phaseOut ? 0.001f : 0.7f + (float) Math.random() * 0.8f;
                h.animateTo(new Vector3f(0, 0, 0),
                        new AxisAngle4f((float) (Math.random() * 0.6 - 0.3), 0, 0, 1),
                        new Vector3f(s, s * 1.2f, s), 20);
            }

            // supplies subtle nudge
            if (tick % 60 == 0) {
                for (int i = 0; i < supplies.size(); i++) {
                    supplies.get(i).animateTo(
                            new Vector3f((float) (Math.random() * 0.2 - 0.1) - 0.5f,
                                    0f,
                                    (float) (Math.random() * 0.2 - 0.1) - 0.5f),
                            new AxisAngle4f((float) (tick * 0.01 + i), 0, 1, 0),
                            new Vector3f(0.45f, 0.45f, 0.45f), 30);
                }
            }

            if (tick % 25 == 0) {
                Location p = getCenter().clone().add(
                        (Math.random() - 0.5) * 12, 1 + Math.random() * 3, (Math.random() - 0.5) * 12);
                DisplayBuilder.playSound(p, Sound.BLOCK_STONE_PLACE, 0.6f, 0.7f);
            }
            if (tick % 90 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 0.4f);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.4f, 0.6f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ArchitectsNightmare(plugin); }
    }

    // ================================================================
    // 43. CELESTIAL WOUND
    //     Massive ragged hole in sky from crying_obsidian + obsidian
    //     block displays (14 forming a ring). Constant end_rod column
    //     descends from center to floor — narrow spotlight.
    // ================================================================
    public static class CelestialWound extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> wound = new ArrayList<>();
        private static final double SKY_HEIGHT = 16.0;

        public CelestialWound(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("celestial_wound", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(35);
            config.setDurationTicks(900);
            config.setCooldownTicks(1000);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.5f);
            // 14 blocks forming irregular ring overhead
            for (int i = 0; i < 14; i++) {
                double a = Math.PI * 2 * i / 14;
                double r = 3.5 + Math.sin(a * 3) * 0.8 + Math.random() * 0.5;
                Location p = center.clone().add(Math.cos(a) * r, SKY_HEIGHT, Math.sin(a) * r);
                Material m = (i % 2 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, m);
                float s = 1.0f + (float) Math.random() * 0.7f;
                h.scale(s, s, s).glow(60, 20, 120).interpolation(20, 0);
                wound.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // edge particles streaming inward + downward
            if (tick % 2 == 0) {
                for (int i = 0; i < 14; i++) {
                    double a = Math.PI * 2 * i / 14 + tick * 0.01;
                    double r = 3.8;
                    Location edge = getCenter().clone().add(
                            Math.cos(a) * r, SKY_HEIGHT, Math.sin(a) * r);
                    Vector inward = new Vector(-Math.cos(a) * 0.3, -0.4, -Math.sin(a) * 0.3);
                    w.spawnParticle(Particle.PORTAL, edge, 3,
                            inward.getX(), inward.getY(), inward.getZ(), 1.0);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, edge, 1,
                            inward.getX() * 0.5, inward.getY() * 0.5, inward.getZ() * 0.5, 0.6);
                    w.spawnParticle(Particle.SQUID_INK, edge, 1, 0.2, 0.0, 0.2, 0.05);
                }
            }

            // central descending column of end_rod — narrow spotlight
            if (tick % 1 == 0) {
                for (double y = 0.3; y < SKY_HEIGHT; y += 0.4) {
                    Location p = getCenter().clone().add(
                            (Math.random() - 0.5) * 0.3, y, (Math.random() - 0.5) * 0.3);
                    w.spawnParticle(Particle.END_ROD, p, 1, 0.02, 0.0, 0.02, 0.0);
                }
            }

            // slow rotation of wound ring
            if (tick % 10 == 0) {
                for (int i = 0; i < wound.size(); i++) {
                    double a = Math.PI * 2 * i / wound.size() + tick * 0.005;
                    double r = 3.5 + Math.sin(a * 3 + tick * 0.02) * 0.8;
                    float tx = (float) (Math.cos(a) * r);
                    float tz = (float) (Math.sin(a) * r);
                    wound.get(i).animateTo(
                            new Vector3f(tx - 0.5f, 0f, tz - 0.5f),
                            new AxisAngle4f((float) (tick * 0.02), 0, 1, 0),
                            new Vector3f(1.2f, 1.2f, 1.2f), 10);
                }
            }

            if (tick % 40 == 0) {
                DisplayBuilder.playSound(getCenter().clone().add(0, SKY_HEIGHT, 0),
                        Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.6f);
            }
            if (tick % 600 == 0 && tick > 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.5f);
            }
            if (tick % 200 == 0) {
                DisplayBuilder.playSound(getCenter().clone().add(0, SKY_HEIGHT, 0),
                        Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.8f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new CelestialWound(plugin); }
    }

    // ================================================================
    // 44. THE DROWNING ROOM
    //     Rising dark fog (large_smoke + squid_ink) up to ~3 blocks.
    //     8 ground-level soul lantern item-displays glow through fog.
    //     soul_fire_flame swims through. Underwater ambience louder
    //     as level rises.
    // ================================================================
    public static class DrowningRoom extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();

        public DrowningRoom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("drowning_room", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.5);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(30);
            config.setDamageDelayTicks(200);
            config.setDurationTicks(400);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP, 0.4f, 0.7f);
            // 8 soul lantern item-displays at ground level
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double r = 3 + (i % 2) * 2;
                Location p = center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.7f, 0.7f, 0.7f).glow(80, 200, 240).interpolation(20, 0);
                lanterns.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();
            // rising level: 0 -> 3 over duration
            double level = Math.min(3.0, (tick / (double) config.getDurationTicks()) * 3.0);

            // dense fog up to current level
            if (tick % 2 == 0) {
                for (int i = 0; i < 32; i++) {
                    double ox = (Math.random() - 0.5) * radius * 2;
                    double oz = (Math.random() - 0.5) * radius * 2;
                    double oy = Math.random() * level;
                    Location p = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.3, 0.05, 0.3, 0.01);
                    if (i % 3 == 0)
                        w.spawnParticle(Particle.SQUID_INK, p, 1, 0.2, 0.05, 0.2, 0.02);
                    if (i % 5 == 0)
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.15, 0.1, 0.15, 0.02);
                }
            }

            // lanterns gentle bob
            if (tick % 30 == 0) {
                for (int i = 0; i < lanterns.size(); i++) {
                    float yOff = (float) (Math.sin(tick * 0.03 + i) * 0.15);
                    lanterns.get(i).animateTo(
                            new Vector3f(-0.5f, yOff, -0.5f),
                            new AxisAngle4f((float) (tick * 0.01 + i), 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 30);
                }
            }

            // underwater ambience louder as fog rises
            if (tick % 60 == 0) {
                float vol = 0.4f + (float) (level / 3.0) * 0.8f;
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_UNDERWATER_LOOP, vol, 0.7f);
            }
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS, 0.6f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new DrowningRoom(plugin); }
    }

    // ================================================================
    // 45. CURSED CANDELABRA ARRAY
    //     5x5 grid of blackstone pole columns (5 tall each = 125 pieces),
    //     plus 25 candle item-display tops. Candles flicker out/in;
    //     pattern slowly converges to spell a shape.
    // ================================================================
    public static class CursedCandelabraArray extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> poles = new ArrayList<>();
        private final List<ItemDisplayHandle> candles = new ArrayList<>();
        private final boolean[] lit = new boolean[25];
        // Final pattern: 'X' shape (true=stay lit)
        private final boolean[] finalPattern = {
                true, false, false, false, true,
                false, true, false, true, false,
                false, false, true, false, false,
                false, true, false, true, false,
                true, false, false, false, true
        };

        public CursedCandelabraArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cursed_candelabra_array", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.5);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(1000);
            config.setCooldownTicks(1100);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 25; i++) lit[i] = true;
            DisplayBuilder.playSound(center, Sound.BLOCK_CANDLE_AMBIENT, 1.2f, 0.7f);
            // 5x5 grid, 2-block spacing
            int idx = 0;
            for (int gx = 0; gx < 5; gx++) {
                for (int gz = 0; gz < 5; gz++) {
                    double x = (gx - 2) * 2.2;
                    double z = (gz - 2) * 2.2;
                    // 5-tall pole
                    for (int y = 0; y < 5; y++) {
                        Location p = center.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                        h.scale(0.35f, 1.0f, 0.35f).interpolation(15, 0);
                        poles.add(h);
                    }
                    // candle item-display on top
                    Location top = center.clone().add(x, 5.1, z);
                    ItemDisplayHandle c = displayBuilder.spawnItem(top, new ItemStack(Material.RED_CANDLE));
                    c.scale(0.7f, 0.7f, 0.7f).glow(255, 60, 30).interpolation(15, 0);
                    candles.add(c);
                    idx++;
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // flame particles rise from each LIT candle
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
                        }
                        idx++;
                    }
                }
            }

            // Every 200 ticks (10s), extinguish or relight one
            if (tick > 0 && tick % 200 == 0) {
                double progress = Math.min(1.0, tick / (double) config.getDurationTicks());
                int targetIdx;
                if (progress < 0.7) {
                    // random extinguish
                    targetIdx = (int) (Math.random() * 25);
                } else {
                    // converge: extinguish anything not in finalPattern
                    targetIdx = -1;
                    for (int i = 0; i < 25; i++) {
                        if (lit[i] && !finalPattern[i]) { targetIdx = i; break; }
                    }
                    if (targetIdx == -1) targetIdx = (int) (Math.random() * 25);
                }
                if (targetIdx >= 0 && targetIdx < 25) {
                    boolean wasLit = lit[targetIdx];
                    lit[targetIdx] = !wasLit;
                    Material m = lit[targetIdx] ? Material.RED_CANDLE : Material.BLACK_CANDLE;
                    // Cannot swap material easily — animate scale to suggest flicker
                    ItemDisplayHandle c = candles.get(targetIdx);
                    int gx = targetIdx / 5; int gz = targetIdx % 5;
                    Location flame = getCenter().clone().add((gx - 2) * 2.2, 5.6, (gz - 2) * 2.2);
                    if (wasLit) {
                        // extinguish
                        c.glow(20, 20, 20);
                        c.animateTo(new Vector3f(-0.5f, 0f, -0.5f),
                                new AxisAngle4f(0f, 0, 1, 0),
                                new Vector3f(0.55f, 0.55f, 0.55f), 10);
                        DisplayBuilder.playSound(flame, Sound.ENTITY_BLAZE_DEATH, 1.0f, 1.4f);
                        for (int p = 0; p < 8; p++)
                            w.spawnParticle(Particle.LARGE_SMOKE, flame, 1, 0.1, 0.1, 0.1, 0.05);
                    } else {
                        c.glow(255, 60, 30);
                        c.animateTo(new Vector3f(-0.5f, 0f, -0.5f),
                                new AxisAngle4f(0f, 0, 1, 0),
                                new Vector3f(0.7f, 0.7f, 0.7f), 10);
                        DisplayBuilder.playSound(flame, Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 1.6f);
                        w.spawnParticle(Particle.CRIT, flame, 16, 0.3, 0.3, 0.3, 0.4);
                    }
                }
            }

            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CANDLE_AMBIENT, 0.8f, 0.7f);
            }
        }

        @Override public AbstractAttack newInstance() { return new CursedCandelabraArray(plugin); }
    }

    // ================================================================
    // 46. THE DEVIL'S EXHALE
    //     Stillness phase, then a huge directional cone blast of
    //     large_smoke + soul_fire_flame from boss direction outward.
    //     Impact-only damage on the wave moment. 8 indicator displays
    //     telegraph the cone direction.
    // ================================================================
    public static class DevilsExhale extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> indicators = new ArrayList<>();
        private double exhaleAngle = 0;
        private boolean impactFired = false;

        public DevilsExhale(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_exhale", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(50);
            config.setDurationTicks(280);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            exhaleAngle = Math.random() * Math.PI * 2;
            // 8 dim indicator blocks along cone centerline
            for (int i = 0; i < 8; i++) {
                double r = 1 + i * 0.9;
                Location p = center.clone().add(
                        Math.cos(exhaleAngle) * r, 1.3, Math.sin(exhaleAngle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SOUL_FIRE);
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 80, 220).interpolation(20, 0);
                indicators.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            // Phase 0: stillness (0-50)
            // Phase 1: BLAST (50-90)
            // Phase 2: haze (90-280)
            if (tick == 5) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_NOTE_BLOCK_SNARE, 0.001f, 0.5f);
            }
            if (tick < 50) {
                // grow indicators slowly as warning
                if (tick % 10 == 0) {
                    float grow = Math.min(1.0f, tick / 50.0f) * 0.8f;
                    for (int i = 0; i < indicators.size(); i++) {
                        indicators.get(i).animateTo(
                                new Vector3f(-0.5f, 0f, -0.5f),
                                new AxisAngle4f((float) (tick * 0.05 + i * 0.5), 0, 1, 0),
                                new Vector3f(grow * (0.4f + i * 0.05f), grow, grow * (0.4f + i * 0.05f)), 10);
                    }
                }
                if (tick == 30) DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 0.4f);
            } else if (tick < 90) {
                // BLAST — full cone
                double dx = Math.cos(exhaleAngle);
                double dz = Math.sin(exhaleAngle);
                double progress = (tick - 50) / 40.0;
                double frontR = progress * (radius * 1.4);

                for (int i = 0; i < 50; i++) {
                    double r = Math.random() * frontR;
                    double spread = Math.random() * (Math.PI / 4); // 45deg cone
                    double s = (Math.random() < 0.5 ? -1 : 1) * spread;
                    double a = exhaleAngle + s;
                    double y = 0.5 + Math.random() * 3.0;
                    Location p = getCenter().clone().add(
                            Math.cos(a) * r, y, Math.sin(a) * r);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 2,
                            dx * 0.5, 0, dz * 0.5, 1.4);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 2,
                            dx * 0.3, 0.05, dz * 0.3, 1.0);
                    if (i % 4 == 0)
                        w.spawnParticle(Particle.SOUL, p, 1, 0.15, 0.2, 0.15, 0.05);
                }
                if (tick == 50) {
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.4f, 0.4f);
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_SHOOT, 1.4f, 0.5f);
                }
                // impact damage at the front of the cone — once
                if (!impactFired && tick == 60) {
                    impactFired = true;
                    Location impact = getCenter().clone().add(dx * radius * 0.7, 1.0, dz * radius * 0.7);
                    triggerImpactDamage(impact);
                }
            } else {
                // haze
                if (tick % 4 == 0) {
                    double dx = Math.cos(exhaleAngle);
                    double dz = Math.sin(exhaleAngle);
                    for (int i = 0; i < 8; i++) {
                        double r = Math.random() * radius;
                        double a = exhaleAngle + (Math.random() - 0.5) * (Math.PI / 4);
                        Location p = getCenter().clone().add(Math.cos(a) * r, 0.5 + Math.random() * 2.5, Math.sin(a) * r);
                        w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.2, 0.1, 0.2, 0.02);
                    }
                }
                if (tick % 40 == 0) {
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CAMPFIRE_CRACKLE, 0.6f, 0.6f);
                }
                // shrink indicators
                if (tick == 100) {
                    for (BlockDisplayHandle h : indicators) {
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
    //     12 humanoid figures (bone_block + deepslate stacked) facing
    //     a focal point. Soul flame rises beneath each. Orbiting soul
    //     lantern item-display above each head. Heads turn when player
    //     walks between them.
    // ================================================================
    public static class PetrifiedCongregation extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> bodies = new ArrayList<>();
        private final List<BlockDisplayHandle> heads = new ArrayList<>();
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();
        private final List<Location> figureLocs = new ArrayList<>();
        private static final int FIGURES = 12;

        public PetrifiedCongregation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("petrified_congregation", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(900);
            config.setCooldownTicks(1000);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.7f, 0.4f);
            // Arrange in semicircle facing the boss/focal at center
            for (int i = 0; i < FIGURES; i++) {
                double a = -Math.PI / 2 + (Math.PI * i / (FIGURES - 1)); // 180 arc
                double r = 5 + (i % 2) * 0.8;
                Location p = center.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                figureLocs.add(p);
                // body block (kneeling — short)
                BlockDisplayHandle body = displayBuilder.spawnBlock(p.clone(), Material.DEEPSLATE);
                body.scale(0.6f, 1.1f, 0.6f).interpolation(20, 0);
                bodies.add(body);
                // head: bone block stacked
                BlockDisplayHandle head = displayBuilder.spawnBlock(p.clone().add(0, 1.15, 0), Material.BONE_BLOCK);
                // face inward
                double facingYaw = Math.atan2(center.getZ() - p.getZ(), center.getX() - p.getX());
                head.scale(0.5f, 0.5f, 0.5f)
                        .rotate((float) -facingYaw, 0, 1, 0)
                        .interpolation(20, 0);
                heads.add(head);
                // orbiting soul lantern
                ItemDisplayHandle lantern = displayBuilder.spawnItem(
                        p.clone().add(0, 2.2, 0), new ItemStack(Material.SOUL_LANTERN));
                lantern.scale(0.4f, 0.4f, 0.4f).glow(80, 220, 240).interpolation(20, 0);
                lanterns.add(lantern);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // soul flame beneath every figure
            if (tick % 3 == 0) {
                for (Location p : figureLocs) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p.clone().add(0, 0.1, 0),
                            2, 0.2, 0.05, 0.2, 0.01);
                    w.spawnParticle(Particle.SOUL, p.clone().add(0, 0.3, 0),
                            1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            // ash drift
            if (tick % 4 == 0) {
                for (int i = 0; i < 12; i++) {
                    double ox = (Math.random() - 0.5) * 14;
                    double oz = (Math.random() - 0.5) * 14;
                    double oy = 1 + Math.random() * 3;
                    w.spawnParticle(Particle.ASH, getCenter().clone().add(ox, oy, oz),
                            1, 0.3, 0.2, 0.3, 0.01);
                }
            }

            // orbit lanterns above heads
            if (tick % 5 == 0) {
                for (int i = 0; i < lanterns.size(); i++) {
                    double a = tick * 0.04 + i * 0.5;
                    float ox = (float) (Math.cos(a) * 0.6);
                    float oz = (float) (Math.sin(a) * 0.6);
                    lanterns.get(i).animateTo(
                            new Vector3f(ox - 0.5f + (float) (Math.cos(a) * 0.0), 1.7f, oz - 0.5f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(0.4f, 0.4f, 0.4f), 5);
                }
            }

            // head-turn reaction to nearby player
            if (tick % 15 == 0) {
                Player target = getTargetPlayer();
                if (target != null) {
                    Location pl = target.getLocation();
                    for (int i = 0; i < heads.size(); i++) {
                        Location fl = figureLocs.get(i);
                        if (fl.distance(pl) < 4.0) {
                            double yaw = Math.atan2(pl.getZ() - fl.getZ(), pl.getX() - fl.getX());
                            // limit head rotation
                            float rot = (float) -yaw;
                            heads.get(i).animateTo(
                                    new Vector3f(-0.25f, 0.15f, -0.25f),
                                    new AxisAngle4f(rot, 0, 1, 0),
                                    new Vector3f(0.5f, 0.5f, 0.5f), 12);
                            if (Math.random() < 0.15)
                                DisplayBuilder.playSound(fl, Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.6f);
                        }
                    }
                }
            }

            if (tick % 70 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f);
            }
            if (tick % 110 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.6f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new PetrifiedCongregation(plugin); }
    }

    // ================================================================
    // 48. THE TEMPORAL SHATTER
    //     3 frozen "moments": (a) mid-fall bone block at head height,
    //     (b) frozen lava droplet stream (8 displays),
    //     (c) frozen smoke explosion sphere (12 displays at radius 2).
    //     Total = 8+1+12 + 3 anchor displays = 24 displays.
    // ================================================================
    public static class TemporalShatter extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> sphereDisplays = new ArrayList<>();
        private final List<ItemDisplayHandle> lavaStream = new ArrayList<>();
        private BlockDisplayHandle fallingBone;

        public TemporalShatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("temporal_shatter", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(800);
            config.setCooldownTicks(900);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.4f);

            // (a) frozen falling bone block at head height, rotated as if mid-tumble
            Location aLoc = center.clone().add(-3, 1.6, 0);
            fallingBone = displayBuilder.spawnBlock(aLoc, Material.BONE_BLOCK);
            fallingBone.scale(0.9f, 0.9f, 0.9f)
                    .rotate(0.6f, 1, 0.5f, 0)
                    .glow(220, 220, 200).interpolation(40, 0);

            // (b) frozen lava droplet stream — 8 magma_cream item-displays
            for (int i = 0; i < 8; i++) {
                Location p = center.clone().add(2.5, 4.0 - i * 0.4, 1.5 + i * 0.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.MAGMA_CREAM));
                h.scale(0.35f, 0.55f, 0.35f).glow(255, 100, 30).interpolation(40, 0);
                lavaStream.add(h);
            }

            // (c) frozen explosion bloom — 12 deepslate displays in a sphere shell at r=2
            Location cLoc = center.clone().add(2, 1.5, -3);
            for (int i = 0; i < 12; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 12);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double sx = 2.0 * Math.sin(phi) * Math.cos(theta);
                double sy = 2.0 * Math.cos(phi);
                double sz = 2.0 * Math.sin(phi) * Math.sin(theta);
                Location p = cLoc.clone().add(sx, sy, sz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACK_STAINED_GLASS);
                h.scale(0.45f, 0.45f, 0.45f).glow(60, 60, 60).interpolation(40, 0);
                sphereDisplays.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // (a) frozen falling: stationary lava-shimmer particles around bone
            if (tick % 3 == 0) {
                Location aLoc = getCenter().clone().add(-3, 1.6, 0);
                w.spawnParticle(Particle.PORTAL, aLoc.clone().add(0.5, 0.5, 0.5),
                        3, 0.6, 0.4, 0.6, 0.0);
                w.spawnParticle(Particle.ENCHANT, aLoc.clone().add(0.5, 0.5, 0.5),
                        2, 0.5, 0.4, 0.5, 0.0);
            }

            // (b) frozen lava stream — particles stationary at each handle
            if (tick % 2 == 0) {
                for (int i = 0; i < lavaStream.size(); i++) {
                    Location p = getCenter().clone().add(2.5, 4.0 - i * 0.4, 1.5 + i * 0.2);
                    w.spawnParticle(Particle.LAVA, p.clone().add(0, 0, 0),
                            1, 0.0, 0.0, 0.0, 0.0);
                    w.spawnParticle(Particle.DRIPPING_LAVA, p.clone().add(0, 0.2, 0),
                            1, 0.05, 0.05, 0.05, 0.0);
                }
            }

            // (c) frozen explosion bloom — smoke at each sphere position, motionless
            if (tick % 2 == 0) {
                Location cLoc = getCenter().clone().add(2, 1.5, -3);
                for (int i = 0; i < 12; i++) {
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / 12);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    double sx = 2.0 * Math.sin(phi) * Math.cos(theta);
                    double sy = 2.0 * Math.cos(phi);
                    double sz = 2.0 * Math.sin(phi) * Math.sin(theta);
                    Location p = cLoc.clone().add(sx, sy, sz);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }

            // subtle teleport/portal ambience in each frozen zone
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter().clone().add(-3, 1.6, 0),
                        Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.5f);
            }
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.3f);
            }
            // very rare "skip" tick — frozen displays jitter once
            if (tick % 200 == 50) {
                if (fallingBone != null) {
                    fallingBone.animateTo(new Vector3f(-0.5f, 0f, -0.5f),
                            new AxisAngle4f(0.6f + (float) (Math.random() * 0.2 - 0.1), 1, 0.5f, 0),
                            new Vector3f(0.9f, 0.9f, 0.9f), 6);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new TemporalShatter(plugin); }
    }

    // ================================================================
    // 49. THE LULLABY MACHINE
    //     16 note block item-displays in 4x4 grid at mixed heights.
    //     Each pulses with NOTE particles at independent timings,
    //     dissonant chord every ~30t. Smoke wisps between.
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
            config.setTicksBetweenDamage(40);
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
                    double y = 1.5 + ((gx + gz) % 3) * 0.7; // mixed heights
                    Location p = center.clone().add(x, y, z);
                    noteLocs.add(p);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NOTE_BLOCK));
                    int rgb = (int) (Math.random() * 255);
                    h.scale(0.55f, 0.55f, 0.55f).glow(rgb, 255 - rgb, 200).interpolation(15, 0);
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

            // each note pulses independently
            for (int i = 0; i < 16; i++) {
                if (tick > 0 && tick % timings[i] == 0) {
                    Location p = noteLocs.get(i);
                    // note particle (data is hue Float)
                    w.spawnParticle(Particle.NOTE, p.clone().add(0, 0.6, 0), 0, 0.0, 0.0, 0.0, hues[i]);
                    for (int k = 0; k < 4; k++) {
                        w.spawnParticle(Particle.NOTE, p.clone().add(
                                (Math.random() - 0.5) * 0.4, 0.6 + Math.random() * 0.6, (Math.random() - 0.5) * 0.4),
                                0, 0.0, 0.0, 0.0, hues[i]);
                    }
                    // pulse scale
                    notes.get(i).animateTo(
                            new Vector3f(-0.5f, 0f, -0.5f),
                            new AxisAngle4f((float) (tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 6);
                    // single note sound
                    Sound s;
                    int sel = i % 3;
                    if (sel == 0) s = Sound.BLOCK_NOTE_BLOCK_BASS;
                    else if (sel == 1) s = Sound.BLOCK_NOTE_BLOCK_HARP;
                    else s = Sound.BLOCK_NOTE_BLOCK_PLING;
                    // dissonant minor scale pitches
                    float[] pitches = { 0.5f, 0.595f, 0.667f, 0.749f, 0.841f, 0.891f, 1.059f };
                    float pitch = pitches[i % pitches.length];
                    DisplayBuilder.playSound(p, s, 0.7f, pitch);
                    // shrink back
                    notes.get(i).animateTo(
                            new Vector3f(-0.5f, 0f, -0.5f),
                            new AxisAngle4f((float) (tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.55f, 0.55f, 0.55f), 8);
                }
            }

            // smoke wisps between
            if (tick % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    Location p = noteLocs.get((int) (Math.random() * 16));
                    w.spawnParticle(Particle.SMOKE, p.clone().add(
                            (Math.random() - 0.5) * 0.6, 0.4, (Math.random() - 0.5) * 0.6),
                            1, 0.1, 0.2, 0.1, 0.005);
                }
            }

            // attack telegraph: bright synced flash every 80t
            if (tick > 0 && tick % 80 == 0) {
                for (int i = 0; i < 16; i++) {
                    Location p = noteLocs.get(i);
                    for (int k = 0; k < 6; k++) {
                        w.spawnParticle(Particle.NOTE, p.clone().add(0, 0.6, 0),
                                0, 0.0, 0.0, 0.0, (i / 16.0f));
                    }
                }
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.2f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new LullabyMachine(plugin); }
    }

    // ================================================================
    // 50. THE LAST DREAM
    //     Finale event: 3 ticks of silence, then absolute chaos burst,
    //     then slow ambient state with one dim soul lantern remaining.
    //     20 surface lanterns ignite, each at floor/wall/ceiling spots.
    // ================================================================
    public static class TheLastDream extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();
        private final List<Location> lanternLocs = new ArrayList<>();
        private ItemDisplayHandle lastLantern;
        private boolean burstFired = false;
        private boolean impactFired = false;

        public TheLastDream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("last_dream", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(40);
            config.setDamageDelayTicks(60);
            config.setDurationTicks(1000);
            config.setCooldownTicks(1200);
        }

        @Override
        protected void onSpawn(Location center) {
            // 20 surface lanterns: pre-spawned but invisible (scale 0). Ignited at burst.
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
            // The "last lantern" near the boss/center, hidden initially
            lastLantern = displayBuilder.spawnItem(
                    center.clone().add(1.5, 1.0, 0), new ItemStack(Material.SOUL_LANTERN));
            lastLantern.scale(0.001f, 0.001f, 0.001f).glow(80, 220, 240).interpolation(20, 0);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double radius = config.getDamageRadius();

            // Phase A: silence (0-60t = 3 sec)
            // Phase B: burst (60-90t)
            // Phase C: ambient slow fade (90-1000t)

            if (tick == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.001f, 0.1f);
            }
            if (tick < 60) {
                // absolute silence — no particles
                return;
            }

            if (tick == 60 && !burstFired) {
                burstFired = true;
                // CHAOS MOMENT — every effect at once
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_DEATH, 1.6f, 0.6f);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.6f, 0.5f);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 0.6f);
                // ignite all 20 lanterns
                for (int i = 0; i < lanterns.size(); i++) {
                    lanterns.get(i).animateTo(
                            new Vector3f(-0.5f, 0f, -0.5f),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 2), 0, 1, 0),
                            new Vector3f(0.8f, 0.8f, 0.8f), 6);
                }
                // dense particle eruption from floor / walls / ceiling
                for (int i = 0; i < 220; i++) {
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
                }
            }

            if (tick > 60 && tick < 90) {
                // continued chaos
                for (int i = 0; i < 80; i++) {
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
                // fade lanterns down to dim
                for (int i = 0; i < lanterns.size(); i++) {
                    lanterns.get(i).animateTo(
                            new Vector3f(-0.5f, 0f, -0.5f),
                            new AxisAngle4f(0f, 0, 1, 0),
                            new Vector3f(0.001f, 0.001f, 0.001f), 60);
                }
                // reveal last lantern
                if (lastLantern != null) {
                    lastLantern.animateTo(
                            new Vector3f(-0.5f, 0f, -0.5f),
                            new AxisAngle4f(0f, 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 30);
                }
            }

            if (tick > 90) {
                // ambient — only ash drifting, last lantern slowly going out
                if (tick % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double ox = (Math.random() - 0.5) * radius * 2;
                        double oz = (Math.random() - 0.5) * radius * 2;
                        double oy = Math.random() * 4;
                        w.spawnParticle(Particle.ASH, getCenter().clone().add(ox, oy, oz),
                                1, 0.3, 0.2, 0.3, 0.01);
                    }
                }
                // last lantern slow flicker dimming
                if (lastLantern != null) {
                    if (tick % 6 == 0) {
                        Location p = getCenter().clone().add(1.5, 1.4, 0);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.05, 0.1, 0.05, 0.005);
                    }
                    if (tick % 80 == 0) {
                        DisplayBuilder.playSound(getCenter().clone().add(1.5, 1.0, 0),
                                Sound.BLOCK_CANDLE_AMBIENT, 0.6f, 0.6f);
                    }
                    // slow shrink across the duration
                    if (tick % 200 == 0) {
                        float remain = Math.max(0.05f, 0.7f * (1.0f - (tick - 90) / 900.0f));
                        lastLantern.animateTo(
                                new Vector3f(-0.5f, 0f, -0.5f),
                                new AxisAngle4f(0f, 0, 1, 0),
                                new Vector3f(remain, remain, remain), 80);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new TheLastDream(plugin); }
    }
}
