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
 * DevilsDream Mode — ENVIRONMENTAL FX BATCH 1 (entries 1-10).
 * Particle / atmosphere / sound-design focused. Lighter on heavy
 * BlockDisplay structure than block-display attacks; emphasizes
 * dense, layered particle systems with item display accents.
 */
public final class DDEnvFX1 {
    private DDEnvFX1() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ScreamingAbyss(plugin));
        registry.register(new BleedingWall(plugin));
        registry.register(new PhantomBonfire(plugin));
        registry.register(new LocustStorm(plugin));
        registry.register(new MirrorShardsRain(plugin));
        registry.register(new SunkenCathedral(plugin));
        registry.register(new DevilsRain(plugin));
        registry.register(new ShiveringDark(plugin));
        registry.register(new AshenCathedralVaults(plugin));
        registry.register(new BloodTide(plugin));
    }

    // ================================================================
    // 1. THE SCREAMING ABYSS
    //    Layered illusion of an infinite pit. Cracked crying-obsidian
    //    rim (12 BlockDisplays) + 8 soul lantern ItemDisplays at
    //    descending depths inside a black squid-ink fog column.
    //    Spiraling soul_fire_flame inhale at the rim. "Breathing" pull.
    // ================================================================
    public static class ScreamingAbyss extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> rim = new ArrayList<>();
        private final List<ItemDisplayHandle> deepLanterns = new ArrayList<>();

        public ScreamingAbyss(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("screaming_abyss", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(420);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.4f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.4f);

            // 12 cracked rim crying-obsidian blocks tilted inward
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2 * i / 12;
                Location p = center.clone().add(Math.cos(angle) * 5.0, 0.05, Math.sin(angle) * 5.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(1.0f, 0.6f, 1.0f).glow(50, 220, 200).interpolation(12, 0);
                rim.add(h);
            }

            // 8 soul lanterns at descending depths inside the abyss column
            for (int i = 0; i < 8; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 1.8;
                Location p = center.clone().add(Math.cos(angle) * r, -1 - i * 0.7, Math.sin(angle) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.6f + (float) Math.random() * 0.4f, 0.6f, 0.6f)
                        .glow(50, 220, 200).interpolation(20, 0);
                deepLanterns.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            int phase = (tick < dur / 4) ? 0 : (tick < dur * 3 / 4) ? 1 : 2;

            // Rim breathes inward / outward
            if (tick % 6 == 0) {
                double breath = Math.sin(tick * 0.05) * 0.4;
                for (int i = 0; i < rim.size(); i++) {
                    double angle = Math.PI * 2 * i / rim.size() + tick * 0.005;
                    double r = 5.0 + breath;
                    float tx = (float) (Math.cos(angle) * r);
                    float tz = (float) (Math.sin(angle) * r);
                    rim.get(i).animateTo(new Vector3f(tx - 0.5f, 0.05f, tz - 0.5f),
                            new AxisAngle4f((float) angle, 0, 1, 0),
                            new Vector3f(1.0f, 0.6f, 1.0f), 6);
                }
            }

            // Deep lanterns slowly drift in depth (illusion of bottomless pit)
            if (tick % 10 == 0) {
                for (int i = 0; i < deepLanterns.size(); i++) {
                    double angle = tick * 0.02 + i * 0.7;
                    double r = 0.5 + Math.sin(tick * 0.04 + i) * 1.2;
                    float tx = (float) (Math.cos(angle) * r);
                    float ty = -1f - i * 0.7f + (float) Math.sin(tick * 0.06 + i) * 0.4f;
                    float tz = (float) (Math.sin(angle) * r);
                    float s = 0.6f + (float) Math.sin(tick * 0.08 + i) * 0.15f;
                    deepLanterns.get(i).animateTo(new Vector3f(tx - s / 2f, ty, tz - s / 2f),
                            new AxisAngle4f((float) (tick * 0.05), 0, 1, 0),
                            new Vector3f(s, s, s), 10);
                }
            }

            // Black squid-ink + smoke fog column rising
            if (tick % 2 == 0) {
                for (int i = 0; i < 16; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * 4.5;
                    double y = Math.random() * 6;
                    Location loc = getCenter().clone().add(Math.cos(angle) * r, -2 + y, Math.sin(angle) * r);
                    w.spawnParticle(Particle.SQUID_INK, loc, 1, 0.3, 0.3, 0.3, 0.02);
                    w.spawnParticle(Particle.SMOKE, loc, 1, 0.3, 0.4, 0.3, 0.04);
                    if (Math.random() < 0.25) {
                        w.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 0.4, 0.2, 0.4, 0.01);
                    }
                }
            }

            // Spiral inhale soul_fire_flame at rim
            if (tick % 2 == 0) {
                for (int i = 0; i < 24; i++) {
                    double angle = (Math.PI * 2 * i / 24) + tick * 0.08;
                    double r = 5.5 - (tick % 30) * 0.18;
                    if (r < 0.5) r = 5.5;
                    Location loc = getCenter().clone().add(Math.cos(angle) * r, 0.6, Math.sin(angle) * r);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }

            // Drip particles falling inward from rim
            if (tick % 3 == 0) {
                for (int i = 0; i < 12; i++) {
                    double angle = Math.PI * 2 * i / 12;
                    Location dripLoc = getCenter().clone().add(Math.cos(angle) * 5, 0.3, Math.sin(angle) * 5);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, dripLoc, 2, 0.2, 0.1, 0.2, 0);
                }
            }

            // The abyss "breathes" — pull players on rim toward center
            if (tick % 8 == 0) {
                double radius = config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double d2 = p.getLocation().distanceSquared(getCenter());
                    if (d2 <= radius * radius && d2 > 1.0) {
                        Vector pull = getCenter().toVector().subtract(p.getLocation().toVector())
                                .normalize().multiply(0.18 + (phase == 2 ? 0.12 : 0));
                        pull.setY(-0.05);
                        p.setVelocity(p.getVelocity().add(pull));
                    }
                }
            }

            // Random ghast scream from "inside"
            if (tick % 60 == 0 && Math.random() < 0.6) {
                DisplayBuilder.playSound(getCenter().clone().add(0, -3, 0), Sound.ENTITY_GHAST_WARN,
                        0.8f, 0.4f + (float) Math.random() * 0.3f);
            }
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 1.1f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ScreamingAbyss(plugin); }
    }

    // ================================================================
    // 2. THE BLEEDING WALL
    //    Tall flat deepslate-brick wall (12 displays) with a central
    //    carved blackstone face + 2 crying-obsidian "eye" item displays.
    //    Vertical streaming dripping_obsidian_tear curtains. Soul fire
    //    backlit through gaps. Pure atmosphere.
    // ================================================================
    public static class BleedingWall extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> wallPanels = new ArrayList<>();
        private final List<ItemDisplayHandle> eyes = new ArrayList<>();

        public BleedingWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bleeding_wall", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(560);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 0.7f, 0.3f);

            // Wall is a 4-wide x 3-tall panel grid, plus a central face
            for (int x = -2; x <= 1; x++) {
                for (int y = 0; y < 3; y++) {
                    Location p = center.clone().add(x * 1.05, 0.5 + y * 1.05, -3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.DEEPSLATE_BRICKS);
                    h.scale(1.0f, 1.0f, 0.3f).glow(20, 0, 0).interpolation(20, 0);
                    wallPanels.add(h);
                }
            }
            // Central carved face block
            Location faceLoc = center.clone().add(-0.5, 1.7, -2.85);
            BlockDisplayHandle face = displayBuilder.spawnBlock(faceLoc, Material.BLACKSTONE);
            face.scale(1.6f, 1.6f, 0.3f).glow(140, 0, 30).interpolation(20, 0);
            wallPanels.add(face);

            // 2 crying-obsidian "eyes" as item displays
            for (int i = 0; i < 2; i++) {
                Location e = center.clone().add(-0.85 + i * 0.7, 2.0, -2.7);
                ItemDisplayHandle eye = displayBuilder.spawnItem(e, new ItemStack(Material.CRYING_OBSIDIAN));
                eye.scale(0.35f, 0.35f, 0.35f).glow(180, 30, 30).interpolation(15, 0);
                eyes.add(eye);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Eye pulse
            if (tick % 8 == 0) {
                for (int i = 0; i < eyes.size(); i++) {
                    float s = 0.35f + (float) Math.sin(tick * 0.1 + i) * 0.08f;
                    eyes.get(i).animateTo(
                            new Vector3f(-0.85f + i * 0.7f - s / 2f, 2.0f, -2.7f - 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s, s, s), 8);
                }
            }

            // Vertical staggered tear streams across full wall width
            if (tick % 2 == 0) {
                for (int col = 0; col < 12; col++) {
                    double offsetX = -2.5 + col * 0.45;
                    int phaseOff = (col * 7) % 30;
                    double yPhase = ((tick + phaseOff) % 30) / 30.0;
                    double y = 3.5 - yPhase * 4.0;
                    Location dripLoc = getCenter().clone().add(offsetX, y, -2.7);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, dripLoc, 2, 0.04, 0.05, 0.04, 0);
                    if (col % 3 == 0) {
                        DisplayBuilder.dustParticles(dripLoc, 1, 0.1, 180, 30, 30, 1.2f);
                    }
                }
            }

            // Backlit soul fire glowing through gaps
            if (tick % 4 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = -2.5 + Math.random() * 4.5;
                    double oy = 0.5 + Math.random() * 3;
                    Location bl = getCenter().clone().add(ox, oy, -3.4);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, bl, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }

            // Tear streams from the eyes specifically — perfectly vertical
            if (tick % 2 == 0) {
                for (int i = 0; i < 2; i++) {
                    double ex = -0.85 + i * 0.7;
                    for (int seg = 0; seg < 5; seg++) {
                        double y = 1.95 - seg * 0.4;
                        Location eyeDrip = getCenter().clone().add(ex, y, -2.65);
                        w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, eyeDrip, 1, 0.02, 0.02, 0.02, 0);
                    }
                }
            }

            // Wet drip ambience
            if (tick % 22 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_WET_GRASS_STEP,
                        0.6f, 0.5f + (float) Math.random() * 0.4f);
            }
            if (tick % 110 == 0 && Math.random() < 0.7) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_AMBIENT, 0.5f, 0.25f);
            }
        }

        @Override public AbstractAttack newInstance() { return new BleedingWall(plugin); }
    }

    // ================================================================
    // 3. PHANTOM BONFIRE
    //    Ring of 6 soul-lantern ItemDisplays at ground + spinning bone
    //    block "log" cluster (4 displays) + enormous particle eruption
    //    8+ blocks high. Heat haze ground smoke. Players warmed/damaged.
    // ================================================================
    public static class PhantomBonfire extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();
        private final List<BlockDisplayHandle> logs = new ArrayList<>();

        public PhantomBonfire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_bonfire", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(480);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CAMPFIRE_CRACKLE, 1.3f, 0.6f);

            // 6 soul lanterns in ring at ground
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * 2 * i / 6;
                Location p = center.clone().add(Math.cos(angle) * 2.5, 0.4, Math.sin(angle) * 2.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.9f, 0.9f, 0.9f).glow(50, 220, 200).interpolation(10, 0);
                lanterns.add(h);
            }

            // Spinning bone log cluster (4 displays) above center
            for (int i = 0; i < 4; i++) {
                double angle = Math.PI * 2 * i / 4;
                Location p = center.clone().add(Math.cos(angle) * 0.5, 1.2, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BONE_BLOCK);
                h.scale(0.4f, 0.9f, 0.4f).glow(240, 230, 200).interpolation(10, 0);
                logs.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            int phase = (tick < dur / 4) ? 0 : (tick < dur * 3 / 4) ? 1 : 2;

            // Lantern flicker / float
            if (tick % 6 == 0) {
                for (int i = 0; i < lanterns.size(); i++) {
                    double angle = Math.PI * 2 * i / 6;
                    float ty = 0.4f + (float) Math.sin(tick * 0.08 + i) * 0.15f;
                    float s = 0.9f + (float) Math.sin(tick * 0.12 + i) * 0.08f;
                    lanterns.get(i).animateTo(
                            new Vector3f((float) Math.cos(angle) * 2.5f - s / 2f, ty,
                                    (float) Math.sin(angle) * 2.5f - s / 2f),
                            new AxisAngle4f((float) (tick * 0.05), 0, 1, 0),
                            new Vector3f(s, s, s), 6);
                }
            }

            // Spinning bone logs crumble rotation
            if (tick % 3 == 0) {
                for (int i = 0; i < logs.size(); i++) {
                    double angle = tick * 0.18 + Math.PI * 2 * i / 4;
                    float tx = (float) (Math.cos(angle) * 0.5);
                    float ty = 1.2f + (float) Math.sin(tick * 0.1 + i) * 0.15f;
                    float tz = (float) (Math.sin(angle) * 0.5);
                    logs.get(i).animateTo(new Vector3f(tx - 0.2f, ty, tz - 0.2f),
                            new AxisAngle4f((float) (tick * 0.2), 1, 0.4f, 0.6f),
                            new Vector3f(0.4f, 0.9f, 0.4f), 3);
                }
            }

            // Massive particle eruption — 8+ blocks high
            if (tick % 2 == 0) {
                for (int y = 0; y < 9; y++) {
                    double radius = 0.8 - y * 0.05;
                    int strands = 6;
                    for (int s = 0; s < strands; s++) {
                        double a = (Math.PI * 2 * s / strands) + tick * 0.15 + y * 0.4;
                        double px = Math.cos(a) * radius;
                        double pz = Math.sin(a) * radius;
                        Location loc = getCenter().clone().add(px, 0.5 + y * 1.0, pz);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.1, 0.05, 0.1, 0.0);
                        if (y % 2 == 0) {
                            w.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 0.2, 0.1, 0.2, 0.01);
                        }
                        if (y < 4) {
                            w.spawnParticle(Particle.FLAME, loc, 1, 0.15, 0.1, 0.15, 0.02);
                        }
                    }
                }
                // Outward drifting ash
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = 1 + Math.random() * 4;
                    Location loc = getCenter().clone().add(Math.cos(angle) * r, 0.3 + Math.random() * 1.5,
                            Math.sin(angle) * r);
                    w.spawnParticle(Particle.ASH, loc, 1, 0.2, 0.1, 0.2, 0.01);
                }
            }

            // Heat haze ground smoke
            if (tick % 4 == 0) {
                for (int i = 0; i < 14; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * 4;
                    Location loc = getCenter().clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                    w.spawnParticle(Particle.SMOKE, loc, 1, 0.2, 0.05, 0.2, 0.01);
                }
                DisplayBuilder.dustParticles(getCenter().clone().add(0, 0.5, 0), 4, 3.5,
                        50, 220, 200, 1.4f);
            }

            // Looping crackle + occasional howl
            if (tick % 18 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CAMPFIRE_CRACKLE, 0.9f, 0.55f);
            }
            if (tick % 70 == 0 && phase >= 1) {
                DisplayBuilder.playSound(getCenter().clone().add(0, 4, 0), Sound.ENTITY_BLAZE_AMBIENT,
                        0.7f, 0.3f + (float) Math.random() * 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new PhantomBonfire(plugin); }
    }

    // ================================================================
    // 4. LOCUST STORM
    //    No structure. Directional sweeping wave of crit + entity_effect
    //    + smoke particles travels arena in ~30s waves. Dead-bush item
    //    displays rattle along path. Buzzing layered sound design.
    // ================================================================
    public static class LocustStorm extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> bushes = new ArrayList<>();
        private double waveDirX = 1.0, waveDirZ = 0.0;
        private int waveStart = 0;
        private final int waveLength = 100; // ticks for full sweep
        private final int wavePeriod = 600; // 30s
        private static final double WAVE_RADIUS = 12.0;

        public LocustStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("locust_storm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.5);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(600);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BAT_AMBIENT, 1.2f, 0.6f);

            // Pick a wave direction
            double a = Math.random() * Math.PI * 2;
            waveDirX = Math.cos(a);
            waveDirZ = Math.sin(a);

            // 10 dead bushes scattered along wave corridor
            for (int i = 0; i < 10; i++) {
                double t = -1 + (i / 9.0) * 2; // -1..1
                double perp = (Math.random() - 0.5) * 8;
                double bx = waveDirX * t * 8 - waveDirZ * perp;
                double bz = waveDirZ * t * 8 + waveDirX * perp;
                Location p = center.clone().add(bx, 0.3, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.DEAD_BUSH));
                h.scale(0.7f, 0.9f, 0.7f).glow(80, 60, 30).interpolation(6, 0);
                bushes.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Wave trigger every wavePeriod ticks
            if (tick % wavePeriod == 0) {
                waveStart = tick;
                // Sometimes flip direction
                if (Math.random() < 0.5) {
                    double a = Math.random() * Math.PI * 2;
                    waveDirX = Math.cos(a);
                    waveDirZ = Math.sin(a);
                }
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GHAST_WARN, 1.2f, 0.7f);
            }

            int waveAge = tick - waveStart;
            double waveProgress = waveAge / (double) waveLength; // 0..1+
            // Wave front position (sweeps from -waveRadius to +waveRadius along direction)
            double frontDist = -WAVE_RADIUS + waveProgress * WAVE_RADIUS * 2;

            // Bush rattle continually — small Y-translate animation
            if (tick % 3 == 0) {
                for (int i = 0; i < bushes.size(); i++) {
                    float ty = 0.3f + (float) Math.sin(tick * 0.6 + i * 1.3) * 0.06f;
                    float rotZ = (float) Math.sin(tick * 0.4 + i) * 0.2f;
                    bushes.get(i).animateTo(new Vector3f(-0.35f, ty, -0.35f),
                            new AxisAngle4f(rotZ, 0, 0, 1),
                            new Vector3f(0.7f, 0.9f, 0.7f), 3);
                }
            }

            // Dense particle wave front when in active sweep
            if (waveAge >= 0 && waveAge <= waveLength) {
                if (tick % 1 == 0) {
                    for (int i = 0; i < 50; i++) {
                        // perpendicular spread along wave line
                        double perp = (Math.random() - 0.5) * 14;
                        double frontJitter = (Math.random() - 0.5) * 1.5;
                        double dist = frontDist + frontJitter;
                        double px = waveDirX * dist - waveDirZ * perp;
                        double pz = waveDirZ * dist + waveDirX * perp;
                        double py = Math.random() * 2.5;
                        Location loc = getCenter().clone().add(px, py, pz);
                        w.spawnParticle(Particle.CRIT, loc, 1, 0.1, 0.1, 0.1, 0.05);
                        if (Math.random() < 0.5) {
                            w.spawnParticle(Particle.ENCHANTED_HIT, loc, 1, 0.15, 0.15, 0.15, 0.02);
                        }
                        if (Math.random() < 0.3) {
                            w.spawnParticle(Particle.SMOKE, loc, 1, 0.2, 0.1, 0.2, 0.01);
                        }
                    }
                    // Trailing smoke streak behind front
                    for (int i = 0; i < 15; i++) {
                        double behind = frontDist - 1 - Math.random() * 4;
                        double perp = (Math.random() - 0.5) * 12;
                        double px = waveDirX * behind - waveDirZ * perp;
                        double pz = waveDirZ * behind + waveDirX * perp;
                        Location loc = getCenter().clone().add(px, Math.random() * 2, pz);
                        w.spawnParticle(Particle.SMOKE, loc, 1, 0.3, 0.2, 0.3, 0.02);
                    }
                }

                // Buzzing rises with proximity to player
                if (tick % 4 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        // Distance from front along wave direction
                        Vector rel = p.getLocation().toVector().subtract(getCenter().toVector());
                        double along = rel.getX() * waveDirX + rel.getZ() * waveDirZ;
                        double diff = Math.abs(along - frontDist);
                        if (diff < 6) {
                            float vol = (float) (1.0 - diff / 6.0) * 1.4f;
                            DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_BAT_AMBIENT,
                                    vol, 0.8f + (float) Math.random() * 0.4f);
                        }
                    }
                }
            } else {
                // Quiet ambient between waves — light buzz
                if (tick % 25 == 0) {
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_BAT_AMBIENT, 0.4f, 0.6f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new LocustStorm(plugin); }
    }

    // ================================================================
    // 5. MIRROR SHARDS RAIN
    //    Continuous rain of 12 amethyst-shard ItemDisplays falling from
    //    sky on random axes. Impact: crit burst + electric_spark + sound.
    //    Impact-only damage. Constant arena coverage.
    // ================================================================
    public static class MirrorShardsRain extends EnvironmentalAttack {
        private static final double COVERAGE = 12.0;

        public MirrorShardsRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mirror_shards_rain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(4.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(440);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.3f, 0.7f);

            // Pre-spawn 12 shards mid-fall to create instant coverage
            for (int i = 0; i < 12; i++) {
                spawnShard(center, 4 + Math.random() * 8);
            }
        }

        private void spawnShard(Location center, double startY) {
            double ox = (Math.random() - 0.5) * COVERAGE * 2;
            double oz = (Math.random() - 0.5) * COVERAGE * 2;
            Location start = center.clone().add(ox, startY, oz);
            boolean calcite = Math.random() < 0.4;
            Material mat = calcite ? Material.CALCITE : Material.AMETHYST_BLOCK;
            // Use BlockDisplay scaled small for "shard" look (with rotation)
            BlockDisplayHandle h = displayBuilder.spawnBlock(start, mat);
            float s = 0.25f + (float) Math.random() * 0.2f;
            h.scale(s, s * 1.6f, s).glow(220, 200, 255).interpolation(50, 0);
            // Tumble fall
            float ax = (float) (Math.random() - 0.5);
            float ay = (float) (Math.random() - 0.5);
            float az = (float) (Math.random() - 0.5);
            float angleMag = (float) (Math.PI * (3 + Math.random() * 3));
            double driftX = (Math.random() - 0.5) * 1.5;
            double driftZ = (Math.random() - 0.5) * 1.5;
            float fall = (float) (-startY - 0.5);
            int dur = 50;
            h.animateTo(new Vector3f((float) (driftX - s / 2f), fall,
                            (float) (driftZ - s / 2f)),
                    new AxisAngle4f(angleMag, ax, ay, az),
                    new Vector3f(s, s * 1.6f, s), dur);

            // Schedule the impact effect at landing
            int delayTicks = dur;
            Location impactLoc = center.clone().add(ox + driftX, 0.1, oz + driftZ);
            World w = center.getWorld();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (w == null) return;
                w.spawnParticle(Particle.CRIT, impactLoc, 18, 0.4, 0.2, 0.4, 0.3);
                w.spawnParticle(Particle.ELECTRIC_SPARK, impactLoc, 12, 0.4, 0.2, 0.4, 0.4);
                w.spawnParticle(Particle.ENCHANTED_HIT, impactLoc, 8, 0.3, 0.2, 0.3, 0.1);
                DisplayBuilder.dustParticles(impactLoc, 4, 0.4, 220, 200, 255, 1.6f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK,
                        0.9f, 0.8f + (float) Math.random() * 0.4f);
                if (Math.random() < 0.25) {
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.2f);
                }
                triggerImpactDamage(impactLoc);
            }, delayTicks);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Spawn 1-2 new shards every 6 ticks (sparse but constant)
            if (tick % 6 == 0) {
                int n = 1 + (int) (Math.random() * 2);
                for (int i = 0; i < n; i++) {
                    spawnShard(getCenter(), 11 + Math.random() * 4);
                }
            }

            // Crystal wind ambient particles drifting
            if (tick % 4 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (Math.random() - 0.5) * COVERAGE * 2;
                    double oz = (Math.random() - 0.5) * COVERAGE * 2;
                    double oy = 2 + Math.random() * 8;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.END_ROD, loc, 1, 0.2, 0.2, 0.2, 0.005);
                    if (Math.random() < 0.5) {
                        DisplayBuilder.dustParticles(loc, 1, 0.3, 220, 200, 255, 1.0f);
                    }
                }
            }

            // Crystal resonate ambient
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE,
                        0.7f, 0.6f + (float) Math.random() * 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new MirrorShardsRain(plugin); }
    }

    // ================================================================
    // 6. THE SUNKEN CATHEDRAL
    //    Two rows of 6 tall blackstone-brick pillars (each is a 4-block
    //    pillar = 24 BlockDisplays total) leading to a raised altar.
    //    Hanging soul lantern chandeliers (4 ItemDisplays). Pillar-base
    //    soul fire, pillar-gap large smoke, altar ash curtain.
    // ================================================================
    public static class SunkenCathedral extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> pillars = new ArrayList<>();
        private final List<ItemDisplayHandle> chandeliers = new ArrayList<>();

        public SunkenCathedral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sunken_cathedral", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(720);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.3f);

            // 6 pillars per side, each pillar 4 blocks tall = 24 BlockDisplays + altar
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 6; i++) {
                    double z = -5 + i * 1.8;
                    double x = side * 3.0;
                    for (int y = 0; y < 4; y++) {
                        Location p = center.clone().add(x, 0.5 + y * 1.05, z);
                        Material mat = (y == 0 || y == 3) ? Material.POLISHED_BLACKSTONE_BRICKS
                                : Material.BLACKSTONE;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                        h.scale(0.9f, 1.0f, 0.9f).glow(40, 0, 0).interpolation(20, 0);
                        pillars.add(h);
                    }
                }
            }

            // Altar (raised area at the back) — 1 large block
            Location altar = center.clone().add(-1, 0.5, 6);
            BlockDisplayHandle alt = displayBuilder.spawnBlock(altar, Material.POLISHED_BLACKSTONE_BRICKS);
            alt.scale(2.0f, 0.5f, 2.0f).glow(50, 220, 200).interpolation(20, 0);
            pillars.add(alt);

            // 4 hanging soul-lantern chandeliers between pillar rows
            for (int i = 0; i < 4; i++) {
                double z = -3 + i * 2.5;
                double y = 3.5 + (i % 2) * 0.6;
                Location p = center.clone().add(0, y, z);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                h.scale(1.0f, 1.0f, 1.0f).glow(50, 220, 200).interpolation(15, 0);
                chandeliers.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Chandeliers swing
            if (tick % 6 == 0) {
                for (int i = 0; i < chandeliers.size(); i++) {
                    double z = -3 + i * 2.5;
                    float ty = 3.5f + (i % 2) * 0.6f + (float) Math.sin(tick * 0.06 + i) * 0.2f;
                    float rotZ = (float) Math.sin(tick * 0.08 + i) * 0.2f;
                    chandeliers.get(i).animateTo(
                            new Vector3f(-0.5f, ty, (float) z - 0.5f),
                            new AxisAngle4f(rotZ, 0, 0, 1),
                            new Vector3f(1.0f, 1.0f, 1.0f), 6);
                }
            }

            // Pillar-base soul fire (12 columns)
            if (tick % 2 == 0) {
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < 6; i++) {
                        double z = -5 + i * 1.8;
                        double x = side * 3.0;
                        for (int s = 0; s < 3; s++) {
                            double a = tick * 0.15 + s * 2.1 + i * 0.5;
                            double pxo = Math.cos(a) * 0.3;
                            double pzo = Math.sin(a) * 0.3;
                            Location loc = getCenter().clone().add(x + pxo, 0.5 + s * 0.5, z + pzo);
                            w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.05, 0.05, 0.05, 0.0);
                        }
                    }
                }
            }

            // Pillar-gap large smoke columns
            if (tick % 4 == 0) {
                for (int i = 0; i < 6; i++) {
                    double z = -5 + i * 1.8;
                    for (int y = 0; y < 5; y++) {
                        Location loc = getCenter().clone().add(0, 0.5 + y * 0.9, z);
                        w.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 0.4, 0.2, 0.4, 0.005);
                    }
                }
            }

            // Altar ash curtain — dense vertical sheet of ASH
            if (tick % 1 == 0) {
                for (int i = 0; i < 16; i++) {
                    double ox = -2 + Math.random() * 4;
                    double oy = 1 + Math.random() * 5;
                    Location loc = getCenter().clone().add(ox, oy, 5 + Math.random() * 1.5);
                    w.spawnParticle(Particle.ASH, loc, 0, 0, -0.4, 0, 1.0); // velocity downward
                }
                for (int i = 0; i < 8; i++) {
                    double ox = -2 + Math.random() * 4;
                    double oy = 4 + Math.random() * 2;
                    Location loc = getCenter().clone().add(ox, oy, 5.5 + Math.random());
                    w.spawnParticle(Particle.WHITE_ASH, loc, 0, 0, -0.3, 0, 1.0);
                }
            }

            // Lantern dust glow halos
            if (tick % 5 == 0) {
                for (int i = 0; i < chandeliers.size(); i++) {
                    double z = -3 + i * 2.5;
                    Location loc = getCenter().clone().add(0, 3.5 + (i % 2) * 0.6, z);
                    DisplayBuilder.dustParticles(loc, 3, 0.6, 50, 220, 200, 1.4f);
                }
            }

            // Drone + occasional wither ambient from altar
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.3f);
            }
            if (tick % 140 == 0 && Math.random() < 0.6) {
                DisplayBuilder.playSound(getCenter().clone().add(0, 1, 6), Sound.ENTITY_WITHER_AMBIENT,
                        0.5f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new SunkenCathedral(plugin); }
    }

    // ================================================================
    // 7. DEVIL'S RAIN
    //    Ceiling-grid red rain. dripping_lava + dripping_obsidian_tear
    //    rain in a 5x5 grid pattern. Magma-block ItemDisplays at each
    //    impact column glow on the floor. Active hazard tick damage.
    // ================================================================
    public static class DevilsRain extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> floorMagma = new ArrayList<>();
        private static final int GRID = 5;
        private static final double SPACING = 2.2;

        public DevilsRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_rain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(440);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.2f, 0.4f);

            // 5x5 = 25 magma block item displays glowing at floor impact points
            double half = (GRID - 1) * 0.5;
            for (int gx = 0; gx < GRID; gx++) {
                for (int gz = 0; gz < GRID; gz++) {
                    double x = (gx - half) * SPACING;
                    double z = (gz - half) * SPACING;
                    Location p = center.clone().add(x, 0.05, z);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.MAGMA_BLOCK));
                    h.scale(0.9f, 0.15f, 0.9f).glow(255, 60, 0).interpolation(12, 0);
                    floorMagma.add(h);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            int phase = (tick < dur / 4) ? 0 : (tick < dur * 3 / 4) ? 1 : 2;

            // Floor magma pulse
            if (tick % 6 == 0) {
                for (int i = 0; i < floorMagma.size(); i++) {
                    int gx = i / GRID;
                    int gz = i % GRID;
                    double half = (GRID - 1) * 0.5;
                    double x = (gx - half) * SPACING;
                    double z = (gz - half) * SPACING;
                    float pulse = 0.9f + (float) Math.sin(tick * 0.15 + i * 0.3) * 0.15f;
                    floorMagma.get(i).animateTo(
                            new Vector3f((float) x - pulse / 2f, 0.05f, (float) z - pulse / 2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, 0.15f, pulse), 6);
                }
            }

            // Rain in grid pattern from ceiling
            if (tick % 1 == 0) {
                double half = (GRID - 1) * 0.5;
                for (int gx = 0; gx < GRID; gx++) {
                    for (int gz = 0; gz < GRID; gz++) {
                        // Each column rains at offset phase
                        if ((tick + gx * 3 + gz * 2) % 3 != 0) continue;
                        double x = (gx - half) * SPACING + (Math.random() - 0.5) * 0.4;
                        double z = (gz - half) * SPACING + (Math.random() - 0.5) * 0.4;
                        for (int y = 0; y < 8; y++) {
                            Location loc = getCenter().clone().add(x, 1 + y * 1.0, z);
                            if (y == 0) {
                                w.spawnParticle(Particle.LAVA, loc, 1, 0.1, 0, 0.1, 0.0);
                            } else if (y % 2 == 0) {
                                w.spawnParticle(Particle.DRIPPING_LAVA, loc, 1, 0.05, 0.05, 0.05, 0);
                            } else {
                                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, loc, 1, 0.05, 0.05, 0.05, 0);
                            }
                        }
                    }
                }
            }

            // Splash particles + drip sound at landing
            if (tick % 3 == 0) {
                int dropsThisTick = 3 + phase * 2;
                for (int d = 0; d < dropsThisTick; d++) {
                    int gx = (int) (Math.random() * GRID);
                    int gz = (int) (Math.random() * GRID);
                    double half = (GRID - 1) * 0.5;
                    double x = (gx - half) * SPACING;
                    double z = (gz - half) * SPACING;
                    Location land = getCenter().clone().add(x, 0.2, z);
                    w.spawnParticle(Particle.LAVA, land, 1, 0.1, 0.05, 0.1, 0.02);
                    w.spawnParticle(Particle.FALLING_LAVA, land, 2, 0.2, 0.05, 0.2, 0);
                    DisplayBuilder.dustParticles(land, 2, 0.3, 255, 60, 0, 1.3f);
                    if (Math.random() < 0.4) {
                        DisplayBuilder.playSound(land, Sound.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON,
                                0.7f, 0.6f + (float) Math.random() * 0.4f);
                    }
                }
            }

            // Lava ambient looping
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_AMBIENT, 0.9f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new DevilsRain(plugin); }
    }

    // ================================================================
    // 8. THE SHIVERING DARK
    //    Pure darkness — no structure. Pulse rings of squid_ink expand
    //    every 3s. Between pulses: silence. At pulse: simultaneous
    //    sphere of smoke + soul_fire_flame + entity_effect. Center has
    //    1 slowly rotating ender-eye ItemDisplay (the only constant
    //    light). Heartbeat-synced sound. Sonic boom every 5 pulses.
    // ================================================================
    public static class ShiveringDark extends EnvironmentalAttack {
        private ItemDisplayHandle eye;
        private static final int PULSE_PERIOD = 60; // 3s
        private int pulseCount = 0;

        public ShiveringDark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shivering_dark", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(35);
            config.setDurationTicks(540);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.5f);
            // Single ender eye in center
            eye = displayBuilder.spawnItem(center.clone().add(0, 1.6, 0), new ItemStack(Material.ENDER_EYE));
            eye.scale(0.6f, 0.6f, 0.6f).glow(90, 0, 130).interpolation(20, 0);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Eye constantly rotating — only constant element
            if (tick % 4 == 0 && eye != null) {
                eye.animateTo(new Vector3f(-0.3f, 1.6f, -0.3f),
                        new AxisAngle4f((float) (tick * 0.06), 0, 1, 0),
                        new Vector3f(0.6f, 0.6f, 0.6f), 4);
            }

            // Eye constant subtle dust halo (the only constant light)
            if (tick % 6 == 0) {
                DisplayBuilder.dustParticles(getCenter().clone().add(0, 1.6, 0),
                        2, 0.2, 90, 0, 130, 1.0f);
                w.spawnParticle(Particle.PORTAL, getCenter().clone().add(0, 1.6, 0),
                        2, 0.2, 0.2, 0.2, 0.05);
            }

            // Heartbeat sync — every PULSE_PERIOD/2 there's a pre-beat warning, then pulse
            int phaseInPulse = tick % PULSE_PERIOD;

            // Heartbeat sound (synced to pulse)
            if (phaseInPulse == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.6f, 0.5f);
            }
            if (phaseInPulse == 30) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.45f);
            }

            // The pulse itself — at start of period
            if (phaseInPulse == 0) {
                pulseCount++;
                // Simultaneous sphere of smoke + soul_fire_flame + entity_effect
                int points = 60;
                for (int i = 0; i < points; i++) {
                    double theta = Math.random() * Math.PI * 2;
                    double phi = Math.acos(2 * Math.random() - 1);
                    double rr = 0.5;
                    double px = rr * Math.sin(phi) * Math.cos(theta);
                    double py = rr * Math.cos(phi);
                    double pz = rr * Math.sin(phi) * Math.sin(theta);
                    Location loc = getCenter().clone().add(px, 1.6 + py, pz);
                    w.spawnParticle(Particle.SMOKE, loc, 1, 0, 0, 0, 0.05);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0, 0, 0, 0.05);
                    if (i % 3 == 0) {
                        w.spawnParticle(Particle.ENCHANT, loc, 1, 0, 0, 0, 0.04);
                    }
                }
                // Sonic boom every 5 pulses
                if (pulseCount % 5 == 0) {
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.6f);
                    w.spawnParticle(Particle.SONIC_BOOM, getCenter().clone().add(0, 1.6, 0),
                            1, 0, 0, 0, 0);
                }
            }

            // Expanding ring of squid_ink during the early portion of pulse
            if (phaseInPulse < 20 && phaseInPulse % 2 == 0) {
                double r = 0.5 + phaseInPulse * 0.35;
                int ringPts = 28;
                for (int i = 0; i < ringPts; i++) {
                    double a = Math.PI * 2 * i / ringPts;
                    Location loc = getCenter().clone().add(Math.cos(a) * r, 1.0, Math.sin(a) * r);
                    w.spawnParticle(Particle.SQUID_INK, loc, 1, 0.05, 0.1, 0.05, 0.01);
                }
            }

            // Contracting sphere visualization mid-pulse
            if (phaseInPulse >= 20 && phaseInPulse < 30 && phaseInPulse % 2 == 0) {
                double r = 4.0 - (phaseInPulse - 20) * 0.4;
                int pts = 18;
                for (int i = 0; i < pts; i++) {
                    double a = Math.PI * 2 * i / pts;
                    Location loc = getCenter().clone().add(Math.cos(a) * r, 1.6, Math.sin(a) * r);
                    w.spawnParticle(Particle.SMOKE, loc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Long silent gap — enderman stare in deep silence
            if (phaseInPulse == 45 && Math.random() < 0.4) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDERMAN_STARE, 0.7f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ShiveringDark(plugin); }
    }

    // ================================================================
    // 9. ASHEN SNOWFALL CATHEDRAL VAULTS
    //    Overhead Gothic vaulted arches built from blackstone
    //    BlockDisplays. Multiple arches across length of arena.
    //    Each apex has shroomlight ItemDisplay behind black-glass
    //    "stained glass" — light beams via dense ash density.
    //    25+ displays. Ash + white_ash rain heavily.
    // ================================================================
    public static class AshenCathedralVaults extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> arches = new ArrayList<>();
        private final List<ItemDisplayHandle> windowLights = new ArrayList<>();
        private final List<BlockDisplayHandle> stainedGlass = new ArrayList<>();

        public AshenCathedralVaults(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ashen_cathedral_vaults", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(700);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SHROOMLIGHT_STEP, 0.8f, 0.3f);
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.3f);

            // 4 arches along Z-axis. Each arch = 7 blocks (arc shoulders + apex).
            int arches4 = 4;
            for (int a = 0; a < arches4; a++) {
                double zPos = -6 + a * 4.0;
                int segments = 7;
                for (int s = 0; s < segments; s++) {
                    // Arch parametric: theta in [0..PI], from left shoulder to right shoulder
                    double theta = Math.PI * s / (segments - 1);
                    double archRadius = 4.0;
                    double x = -Math.cos(theta) * archRadius;
                    double y = 6.5 + Math.sin(theta) * 3.0;
                    Location p = center.clone().add(x, y, zPos);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                    h.scale(0.9f, 0.9f, 0.9f).glow(60, 60, 60).interpolation(20, 0);
                    // Tilt slightly along arc tangent
                    h.rotate((float) (theta - Math.PI / 2), 0, 0, 1);
                    arches.add(h);
                }
                // Apex stained glass + shroomlight
                Location apexBlock = center.clone().add(0, 9.5, zPos);
                BlockDisplayHandle gl = displayBuilder.spawnBlock(apexBlock, Material.BLACK_STAINED_GLASS);
                gl.scale(1.4f, 1.4f, 0.4f).glow(80, 30, 30).interpolation(20, 0);
                stainedGlass.add(gl);

                // 2 shroomlight item displays behind each apex
                for (int sl = 0; sl < 2; sl++) {
                    Location lp = center.clone().add(-0.4 + sl * 0.8, 9.5, zPos - 0.3);
                    ItemDisplayHandle li = displayBuilder.spawnItem(lp, new ItemStack(Material.SHROOMLIGHT));
                    li.scale(0.8f, 0.8f, 0.8f).glow(255, 200, 100).interpolation(15, 0);
                    windowLights.add(li);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Window lights pulse warmly
            if (tick % 8 == 0) {
                for (int i = 0; i < windowLights.size(); i++) {
                    int archIdx = i / 2;
                    int slIdx = i % 2;
                    double zPos = -6 + archIdx * 4.0;
                    float s = 0.8f + (float) Math.sin(tick * 0.05 + i) * 0.15f;
                    windowLights.get(i).animateTo(
                            new Vector3f(-0.4f + slIdx * 0.8f - s / 2f, 9.5f - s / 2f,
                                    (float) zPos - 0.3f - s / 2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s, s, s), 8);
                }
            }

            // Heavy ash fall from every arch intersection
            if (tick % 1 == 0) {
                for (int a = 0; a < 4; a++) {
                    double zPos = -6 + a * 4.0;
                    // 3 ash columns per arch
                    for (int c = 0; c < 3; c++) {
                        double x = -2.5 + c * 2.5 + (Math.random() - 0.5) * 0.6;
                        double y = 7 + Math.random() * 2.5;
                        Location loc = getCenter().clone().add(x, y, zPos + (Math.random() - 0.5) * 0.5);
                        w.spawnParticle(Particle.ASH, loc, 0, 0, -0.5, 0, 1.0); // velocity downward
                    }
                }
            }

            // White ash drifts from vault shoulders (more sideways drift)
            if (tick % 2 == 0) {
                for (int a = 0; a < 4; a++) {
                    double zPos = -6 + a * 4.0;
                    for (int side = -1; side <= 1; side += 2) {
                        double x = side * 4.0 + (Math.random() - 0.5) * 0.5;
                        double y = 6.5 + Math.random() * 1.0;
                        Location loc = getCenter().clone().add(x, y, zPos);
                        double vx = side * 0.05 + (Math.random() - 0.5) * 0.1;
                        w.spawnParticle(Particle.WHITE_ASH, loc, 0, vx, -0.3, 0, 1.0);
                    }
                }
            }

            // Light beam columns through ash (downward dust streaks at apex)
            if (tick % 2 == 0) {
                for (int a = 0; a < 4; a++) {
                    double zPos = -6 + a * 4.0;
                    for (int yStep = 0; yStep < 8; yStep++) {
                        double y = 9.0 - yStep * 1.0;
                        double jitter = (Math.random() - 0.5) * 0.15;
                        Location loc = getCenter().clone().add(jitter, y, zPos + jitter);
                        // Warm "beam" dust
                        DisplayBuilder.dustParticles(loc, 1, 0.08, 255, 200, 110, 1.4f);
                    }
                }
            }

            // Subtle "floor catch" of ash
            if (tick % 5 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (Math.random() - 0.5) * 12;
                    double oz = (Math.random() - 0.5) * 12;
                    Location loc = getCenter().clone().add(ox, 0.3, oz);
                    w.spawnParticle(Particle.ASH, loc, 1, 0.4, 0.05, 0.4, 0.005);
                }
            }

            // Bone creak from vaults
            if (tick % 90 == 0 && Math.random() < 0.7) {
                DisplayBuilder.playSound(getCenter().clone().add(0, 8, 0), Sound.BLOCK_BONE_BLOCK_STEP,
                        0.7f, 0.4f + (float) Math.random() * 0.3f);
            }
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SHROOMLIGHT_STEP, 0.5f, 0.3f);
            }
            if (tick % 120 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.9f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new AshenCathedralVaults(plugin); }
    }

    // ================================================================
    // 10. BLOOD TIDE
    //     Sweeping ground wave from one wall — large_smoke roll precedes
    //     the tide, dripping_lava + falling_lava flood floor. Magma
    //     ItemDisplays in residue zone glow. Wave retreats. Active
    //     hazard during tide: ground contact damages via impact tick.
    // ================================================================
    public static class BloodTide extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> residueMagma = new ArrayList<>();
        private double dirX = 1.0, dirZ = 0.0;
        private static final int CYCLE_LENGTH = 240; // 12s out + 12s back
        private static final double WAVE_RADIUS = 12.0;

        public BloodTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blood_tide", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(3.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.4f, 0.3f);
            double a = Math.random() * Math.PI * 2;
            dirX = Math.cos(a);
            dirZ = Math.sin(a);

            // 12 residue magma spots forming a ground "stain" trail
            for (int i = 0; i < 12; i++) {
                double t = -1 + (i / 11.0) * 2;
                double perp = (Math.random() - 0.5) * 8;
                double bx = dirX * t * 6 - dirZ * perp;
                double bz = dirZ * t * 6 + dirX * perp;
                Location p = center.clone().add(bx, 0.05, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.MAGMA_BLOCK));
                h.scale(1.2f, 0.1f, 1.2f).glow(180, 30, 30).interpolation(12, 0);
                residueMagma.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            int cyclePhase = tick % CYCLE_LENGTH;
            // Out-going (0..120) and retreating (120..240)
            boolean outgoing = cyclePhase < CYCLE_LENGTH / 2;
            double phaseProgress;
            if (outgoing) {
                phaseProgress = cyclePhase / (double) (CYCLE_LENGTH / 2);
            } else {
                phaseProgress = 1.0 - (cyclePhase - CYCLE_LENGTH / 2.0) / (CYCLE_LENGTH / 2.0);
            }
            double frontDist = -WAVE_RADIUS + phaseProgress * WAVE_RADIUS * 2;

            // Wave start sound
            if (cyclePhase == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.3f);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_AMBIENT, 1.5f, 0.4f);
            }
            if (cyclePhase == CYCLE_LENGTH / 2) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 0.5f);
            }

            // Residue magma pulse
            if (tick % 8 == 0) {
                for (int i = 0; i < residueMagma.size(); i++) {
                    float pulse = 1.2f + (float) Math.sin(tick * 0.1 + i * 0.4) * 0.2f;
                    int index = i;
                    double t = -1 + (index / 11.0) * 2;
                    // recompute residue pos (residue stays put but pulses)
                    residueMagma.get(i).animateTo(
                            new Vector3f(-pulse / 2f, 0.05f, -pulse / 2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, 0.1f, pulse), 8);
                    // suppress unused warning
                    @SuppressWarnings("unused") double _t = t;
                }
            }

            // Rolling wall of large_smoke just ahead of front
            if (tick % 1 == 0) {
                double smokeOff = outgoing ? 0.8 : -0.8;
                for (int i = 0; i < 22; i++) {
                    double perp = (Math.random() - 0.5) * 14;
                    double dist = frontDist + smokeOff;
                    double px = dirX * dist - dirZ * perp;
                    double pz = dirZ * dist + dirX * perp;
                    double py = Math.random() * 1.8;
                    Location loc = getCenter().clone().add(px, py, pz);
                    w.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 0.3, 0.3, 0.3, 0.02);
                    if (Math.random() < 0.4) {
                        w.spawnParticle(Particle.SMOKE, loc, 1, 0.3, 0.2, 0.3, 0.04);
                    }
                }
            }

            // Lava flood at the front ribbon
            if (tick % 1 == 0) {
                for (int i = 0; i < 26; i++) {
                    double perp = (Math.random() - 0.5) * 14;
                    double jitter = (Math.random() - 0.5) * 1.0;
                    double dist = frontDist + jitter;
                    double px = dirX * dist - dirZ * perp;
                    double pz = dirZ * dist + dirX * perp;
                    Location loc = getCenter().clone().add(px, 0.2, pz);
                    w.spawnParticle(Particle.DRIPPING_LAVA, loc, 1, 0.1, 0.05, 0.1, 0);
                    w.spawnParticle(Particle.FALLING_LAVA, loc, 1, 0.2, 0.05, 0.2, 0);
                    if (Math.random() < 0.4) {
                        w.spawnParticle(Particle.LAVA, loc, 1, 0.1, 0.05, 0.1, 0.02);
                    }
                    if (Math.random() < 0.2) {
                        DisplayBuilder.dustParticles(loc, 1, 0.2, 180, 30, 30, 1.4f);
                    }
                }
            }

            // Behind the wave: residue twinkling at ground
            if (tick % 2 == 0) {
                double behindLimit = outgoing ? frontDist : -WAVE_RADIUS;
                for (int i = 0; i < 16; i++) {
                    double rPerp = (Math.random() - 0.5) * 14;
                    double rDist = -WAVE_RADIUS + Math.random() * (behindLimit + WAVE_RADIUS);
                    double px = dirX * rDist - dirZ * rPerp;
                    double pz = dirZ * rDist + dirX * rPerp;
                    Location loc = getCenter().clone().add(px, 0.1, pz);
                    w.spawnParticle(Particle.LAVA, loc, 1, 0.2, 0.02, 0.2, 0);
                    if (Math.random() < 0.3) {
                        DisplayBuilder.dustParticles(loc, 1, 0.2, 180, 30, 30, 1.0f);
                    }
                }
            }

            // Damage ground-touching players in wave path
            if (tick % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    Vector rel = p.getLocation().toVector().subtract(getCenter().toVector());
                    double along = rel.getX() * dirX + rel.getZ() * dirZ;
                    double perpDist = Math.abs(-rel.getX() * dirZ + rel.getZ() * dirX);
                    if (perpDist > 8) continue;
                    boolean inWaveBand = Math.abs(along - frontDist) < 2.5;
                    boolean grounded = p.isOnGround();
                    if (inWaveBand && grounded) {
                        triggerImpactDamage(p.getLocation());
                        DisplayBuilder.playSound(p.getLocation(), Sound.BLOCK_LAVA_POP, 0.8f, 0.6f);
                    }
                }
            }

            // Lava ambient roar building
            if (tick % 30 == 0) {
                float vol = (float) (0.6 + Math.sin(cyclePhase / (double) CYCLE_LENGTH * Math.PI) * 0.5);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_AMBIENT, vol, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new BloodTide(plugin); }
    }
}
