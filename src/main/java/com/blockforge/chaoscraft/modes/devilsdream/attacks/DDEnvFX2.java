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
 * DevilsDream Mode — ENVIRONMENTAL ATTACKS Pack 2 (entries 11–20).
 *
 * Heavily detailed atmosphere/sound-design pieces. Every effect uses dense
 * BlockDisplay structure + ItemDisplay props + multi-layer particle work
 * with smooth animateTo() interpolations and multi-phase lifecycles
 * (spawn -> active -> dissipate).
 */
public final class DDEnvFX2 {
    private DDEnvFX2() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WeepingIcon(plugin));
        registry.register(new MeteorGraveyard(plugin));
        registry.register(new StormEye(plugin));
        registry.register(new InfernalMarchingBand(plugin));
        registry.register(new EclipseEvent(plugin));
        registry.register(new DissectionTable(plugin));
        registry.register(new CosmicTear(plugin));
        registry.register(new ProphetsThrone(plugin));
        registry.register(new RainOfEyes(plugin));
        registry.register(new Ossuary(plugin));
    }

    // ================================================================
    // 11. THE WEEPING ICON
    //     Tall stylized religious figure (bone + deepslate). Two vertical
    //     dripping_obsidian_tear streams from eyes, soul_fire ring at base,
    //     outstretched arms with falling water from palms, glowing totem
    //     embedded at chest. Pure atmosphere. Phase: rise -> weep -> dim.
    // ================================================================
    public static class WeepingIcon extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> figure = new ArrayList<>();
        private ItemDisplayHandle chestTotem;
        private int phase = 0;

        public WeepingIcon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("weeping_icon", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(40);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(700);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_WARN, 0.4f, 0.3f);
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.7f, 0.4f);

            // Spine / body — vertical column of deepslate (10 high)
            for (int y = 0; y < 10; y++) {
                Location p = center.clone().add(0, y * 0.8, 0);
                Material mat = (y < 7) ? Material.DEEPSLATE_TILES : Material.BONE_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                h.scale(0.01f, 0.01f, 0.01f).interpolation(40, y * 2);
                h.animateTo(new Vector3f(-0.4f, y * 0.8f, -0.4f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.8f, 0.8f, 0.8f), 40);
                figure.add(h);
            }

            // Head — bone block on top, slightly larger
            BlockDisplayHandle head = displayBuilder.spawnBlock(
                    center.clone().add(0, 8.0, 0), Material.BONE_BLOCK);
            head.scale(0.01f, 0.01f, 0.01f).glow(220, 220, 200).interpolation(40, 20);
            head.animateTo(new Vector3f(-0.55f, 8.0f, -0.55f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(1.1f, 1.1f, 1.1f), 40);
            figure.add(head);

            // Outstretched arms — 6 segments per side
            for (int i = 1; i <= 6; i++) {
                Location lp = center.clone().add(-i * 0.6, 5.5 - i * 0.1, 0);
                Location rp = center.clone().add(i * 0.6, 5.5 - i * 0.1, 0);
                BlockDisplayHandle l = displayBuilder.spawnBlock(lp, Material.BONE_BLOCK);
                BlockDisplayHandle r = displayBuilder.spawnBlock(rp, Material.BONE_BLOCK);
                l.scale(0.4f, 0.4f, 0.4f).glow(210, 210, 190).interpolation(30, 30 + i);
                r.scale(0.4f, 0.4f, 0.4f).glow(210, 210, 190).interpolation(30, 30 + i);
                figure.add(l);
                figure.add(r);
            }

            // Chest totem — glowing item
            chestTotem = displayBuilder.spawnItem(
                    center.clone().add(0, 4.5, 0.4),
                    new ItemStack(Material.TOTEM_OF_UNDYING));
            chestTotem.scale(1.2f, 1.2f, 1.2f).glow(255, 220, 80).interpolation(20, 30);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            // Phase transitions
            if (phase == 0 && tick > 60) phase = 1;
            if (phase == 1 && tick > dur - 80) phase = 2;

            // Eye tears — two perfectly vertical streams from head height (y ~ 8.6)
            if (phase >= 1 && tick % 2 == 0) {
                Location leftEye = getCenter().clone().add(-0.25, 8.6, 0.45);
                Location rightEye = getCenter().clone().add(0.25, 8.6, 0.45);
                for (int y = 0; y < 9; y++) {
                    Location lp = leftEye.clone().subtract(0, y * 0.95, 0);
                    Location rp = rightEye.clone().subtract(0, y * 0.95, 0);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, lp, 1, 0.02, 0.05, 0.02, 0);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, rp, 1, 0.02, 0.05, 0.02, 0);
                }
                // Pool at base
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    Location pool = getCenter().clone().add(Math.cos(a) * 0.6, 0.1, Math.sin(a) * 0.6);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, pool, 1, 0.1, 0, 0.1, 0);
                }
            }

            // Soul fire ring at base — perpetually burning circle
            if (phase >= 1 && tick % 3 == 0) {
                int pts = 20;
                for (int i = 0; i < pts; i++) {
                    double a = Math.PI * 2 * i / pts + tick * 0.02;
                    Location p = getCenter().clone().add(Math.cos(a) * 1.8, 0.1, Math.sin(a) * 1.8);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.05, 0.1, 0.05, 0.005);
                }
            }

            // Palms drip — falling_water particles
            if (phase >= 1 && tick % 4 == 0) {
                Location lPalm = getCenter().clone().add(-3.6, 4.9, 0);
                Location rPalm = getCenter().clone().add(3.6, 4.9, 0);
                for (int y = 0; y < 5; y++) {
                    w.spawnParticle(Particle.FALLING_WATER,
                            lPalm.clone().subtract(0, y * 0.8, 0), 2, 0.1, 0.1, 0.1, 0);
                    w.spawnParticle(Particle.FALLING_WATER,
                            rPalm.clone().subtract(0, y * 0.8, 0), 2, 0.1, 0.1, 0.1, 0);
                }
            }

            // Chest totem soft pulse
            if (chestTotem != null && tick % 10 == 0) {
                float s = 1.2f + (float) Math.sin(tick * 0.08) * 0.15f;
                chestTotem.animateTo(new Vector3f(-s / 2f, 4.5f, 0.4f),
                        new AxisAngle4f((float) (tick * 0.04), 0, 1, 0),
                        new Vector3f(s, s, s), 10);
            }

            // Looped ambient
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GHAST_WARN, 0.3f, 0.3f);
            }
            if (tick % 22 == 0) {
                DisplayBuilder.playSound(getCenter().clone().add(0, 1, 0),
                        Sound.BLOCK_WET_GRASS_STEP, 0.4f, 0.4f);
            }

            // Dissipate
            if (phase == 2 && tick % 10 == 0) {
                w.spawnParticle(Particle.LARGE_SMOKE, getCenter().clone().add(0, 4, 0),
                        6, 1.5, 3, 1.5, 0.02);
            }
        }

        @Override public AbstractAttack newInstance() { return new WeepingIcon(plugin); }
    }

    // ================================================================
    // 12. METEOR GRAVEYARD
    //     6 impact craters scattered around arena. Each: ejecta of basalt
    //     shards splayed outward + central buried obsidian meteor. Smoke
    //     and small flames. Largest crater pumps ash + large_smoke column.
    // ================================================================
    public static class MeteorGraveyard extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> rocks = new ArrayList<>();
        private Location largestCrater;
        private int phase = 0;

        public MeteorGraveyard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("meteor_graveyard", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(40);
            config.setDamageDelayTicks(60);
            config.setDurationTicks(800);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.3f);
            // 6 impact sites in a ring
            for (int s = 0; s < 6; s++) {
                double a = Math.PI * 2 * s / 6;
                double r = 6 + Math.random() * 3;
                Location site = center.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                if (s == 0) largestCrater = site.clone();

                // Buried meteor — 4 obsidian blocks clustered
                for (int m = 0; m < 4; m++) {
                    Location mp = site.clone().add(
                            (Math.random() - 0.5) * 0.7, -0.2 + m * 0.25, (Math.random() - 0.5) * 0.7);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(mp, Material.OBSIDIAN);
                    h.scale(0.01f, 0.01f, 0.01f).glow(40, 0, 60).interpolation(30, s * 6 + m);
                    h.animateTo(new Vector3f(-0.45f, -0.2f + m * 0.25f, -0.45f),
                            new AxisAngle4f((float) (Math.random() * Math.PI), 1, 0, 1),
                            new Vector3f(0.9f, 0.9f, 0.9f), 30);
                    rocks.add(h);
                }
                // Ejecta ring — 8 basalt shards splayed outward
                for (int e = 0; e < 8; e++) {
                    double ea = Math.PI * 2 * e / 8;
                    double er = 1.5 + Math.random() * 1.2;
                    Location ep = site.clone().add(Math.cos(ea) * er, 0.1, Math.sin(ea) * er);
                    Material mat = (e % 2 == 0) ? Material.BASALT : Material.BLACKSTONE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(ep, mat);
                    h.scale(0.01f, 0.01f, 0.01f).interpolation(25, s * 4 + e);
                    h.animateTo(new Vector3f(-0.25f, 0.1f, -0.25f),
                            new AxisAngle4f((float) (ea + Math.PI / 2), 0, 1, 0),
                            new Vector3f(0.5f, 0.3f, 0.7f), 25);
                    rocks.add(h);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (phase == 0 && tick > 50) phase = 1;
            if (phase == 1 && tick > config.getDurationTicks() - 80) phase = 2;

            // All craters: smoke & small flame
            if (tick % 5 == 0) {
                for (int s = 0; s < 6; s++) {
                    double a = Math.PI * 2 * s / 6;
                    double r = 6;
                    Location site = getCenter().clone().add(Math.cos(a) * r, 0.4, Math.sin(a) * r);
                    w.spawnParticle(Particle.SMOKE, site, 2, 0.6, 0.3, 0.6, 0.01);
                    w.spawnParticle(Particle.SMALL_FLAME, site, 1, 0.5, 0.1, 0.5, 0.005);
                }
            }

            // Largest crater — ash + large_smoke column rising
            if (largestCrater != null && tick % 3 == 0) {
                for (int y = 0; y < 8; y++) {
                    double jx = (Math.random() - 0.5) * (0.4 + y * 0.15);
                    double jz = (Math.random() - 0.5) * (0.4 + y * 0.15);
                    Location p = largestCrater.clone().add(jx, 0.5 + y * 0.9, jz);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.15, 0.2, 0.15, 0.008);
                    w.spawnParticle(Particle.ASH, p, 2, 0.3, 0.25, 0.3, 0.01);
                }
            }

            // Occasional cooling rock creak
            if (tick % 70 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BASALT_STEP, 0.5f, 0.4f);
            }
            if (tick % 95 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_EXTINGUISH, 0.4f, 0.3f);
            }
            if (tick % 140 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 0.2f);
            }
        }

        @Override public AbstractAttack newInstance() { return new MeteorGraveyard(plugin); }
    }

    // ================================================================
    // 13. THE STORM EYE
    //     Wide swirling perimeter ring of smoke spiraling inward, calm
    //     center. Lightning flashes around edge. Boundary obsidian
    //     markers. The eye SHRINKS over time — forced movement.
    // ================================================================
    public static class StormEye extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> markers = new ArrayList<>();
        private double currentEyeRadius = 6.0;
        private int phase = 0;

        public StormEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("storm_eye", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(500);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.5f);
            // 16 ground markers around eye perimeter
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                Location p = center.clone().add(Math.cos(a) * 6, 0.05, Math.sin(a) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.OBSIDIAN);
                h.scale(0.01f, 0.01f, 0.01f).glow(60, 0, 80).interpolation(20, i);
                h.animateTo(new Vector3f(-0.3f, 0.05f, -0.3f),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(0.6f, 0.15f, 0.6f), 20);
                markers.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            if (phase == 0 && tick > 40) phase = 1;
            if (phase == 1 && tick > dur - 60) phase = 2;

            // Shrinking eye
            double progress = Math.min(1.0, (double) tick / dur);
            currentEyeRadius = 6.0 - progress * 4.0; // shrinks to 2.0
            double outerRadius = currentEyeRadius + 4.0;

            // Animate markers inward
            if (tick % 20 == 0) {
                for (int i = 0; i < markers.size(); i++) {
                    double a = Math.PI * 2 * i / markers.size() + tick * 0.005;
                    float tx = (float) (Math.cos(a) * currentEyeRadius);
                    float tz = (float) (Math.sin(a) * currentEyeRadius);
                    markers.get(i).animateTo(new Vector3f(tx - 0.3f, 0.05f, tz - 0.3f),
                            new AxisAngle4f((float) (tick * 0.04), 0, 1, 0),
                            new Vector3f(0.6f, 0.15f, 0.6f), 20);
                }
            }

            // Spiral smoke ring INWARD (from outer radius toward eye edge)
            if (tick % 1 == 0) {
                int pts = 36;
                for (int i = 0; i < pts; i++) {
                    double base = Math.PI * 2 * i / pts;
                    double swirl = base + tick * 0.12;
                    // density gradient — rings at multiple radii
                    for (int ring = 0; ring < 4; ring++) {
                        double ringR = currentEyeRadius + 0.5 + ring * 1.0;
                        if (ringR > outerRadius) continue;
                        double y = Math.sin(swirl + ring) * 0.3 + 0.6 + ring * 0.2;
                        Location p = getCenter().clone().add(
                                Math.cos(swirl + ring * 0.3) * ringR, y,
                                Math.sin(swirl + ring * 0.3) * ringR);
                        if (i % 3 == 0) {
                            w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.05, 0.05, 0.05, 0.002);
                        }
                        if (i % 4 == 0) {
                            w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.05, 0.1, 0.005);
                        }
                    }
                }
            }

            // Lightning flashes at random outer perimeter positions
            if (tick % 14 == 0) {
                double a = Math.random() * Math.PI * 2;
                Location strike = getCenter().clone().add(Math.cos(a) * outerRadius, 0,
                        Math.sin(a) * outerRadius);
                for (int y = 0; y < 12; y++) {
                    Location lp = strike.clone().add(
                            (Math.random() - 0.5) * 0.4, y, (Math.random() - 0.5) * 0.4);
                    DisplayBuilder.dustParticles(lp, 1, 0.05, 220, 220, 255, 1.6f);
                }
                w.spawnParticle(Particle.ELECTRIC_SPARK, strike, 14, 0.6, 0.6, 0.6, 0.4);
                w.spawnParticle(Particle.FLASH, strike.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
                DisplayBuilder.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                        1.0f, 0.7f + (float) Math.random() * 0.4f);
            }

            // High pitched circling wind
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GHAST_AMBIENT, 0.5f, 1.7f);
            }

            // Damage anyone OUTSIDE the eye but within outer storm
            if (tick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double d2 = p.getLocation().distanceSquared(getCenter());
                    if (d2 > currentEyeRadius * currentEyeRadius
                            && d2 <= outerRadius * outerRadius) {
                        p.damage(config.getDamage());
                        // Pull inward... no, the eye is calm, push outward to keep tension
                        Vector toCenter = getCenter().toVector().subtract(p.getLocation().toVector())
                                .normalize().multiply(-0.2);
                        p.setVelocity(p.getVelocity().add(toCenter));
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new StormEye(plugin); }
    }

    // ================================================================
    // 14. THE INFERNAL MARCHING BAND
    //     Rows of "musicians" — each is a note block item display + a
    //     bone/blackstone instrument silhouette. Note particles burst on
    //     stagger. Soul fire rises. Dissonant note sounds.
    // ================================================================
    public static class InfernalMarchingBand extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> noteBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> instruments = new ArrayList<>();
        private int phase = 0;

        public InfernalMarchingBand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_marching_band", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(700);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.3f);
            // 4 rows of 4 musicians, formation oriented +Z toward boss
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    double xo = (col - 1.5) * 1.6;
                    double zo = -row * 2.0 - 2.0;
                    Location nlp = center.clone().add(xo, 1.2, zo);
                    ItemDisplayHandle nh = displayBuilder.spawnItem(nlp,
                            new ItemStack(Material.NOTE_BLOCK));
                    nh.scale(0.01f, 0.01f, 0.01f).glow(180, 60, 200)
                            .interpolation(20, row * 6 + col);
                    nh.animateTo(new Vector3f(-0.4f, 1.2f, 0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.8f, 0.8f, 0.8f), 20);
                    noteBlocks.add(nh);

                    // Instrument silhouette behind it — bone block + blackstone stack
                    Location ip = center.clone().add(xo, 0.3, zo - 0.4);
                    BlockDisplayHandle b1 = displayBuilder.spawnBlock(ip, Material.BLACKSTONE);
                    b1.scale(0.4f, 0.6f, 0.4f).interpolation(15, row * 6 + col);
                    instruments.add(b1);
                    BlockDisplayHandle b2 = displayBuilder.spawnBlock(
                            ip.clone().add(0, 0.7, 0), Material.BONE_BLOCK);
                    b2.scale(0.3f, 0.5f, 0.3f).glow(220, 220, 200).interpolation(15, row * 6 + col + 2);
                    instruments.add(b2);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (phase == 0 && tick > 30) phase = 1;
            if (phase == 1 && tick > config.getDurationTicks() - 60) phase = 2;

            // Note particles — staggered per musician
            if (phase >= 1) {
                for (int i = 0; i < noteBlocks.size(); i++) {
                    int stagger = i * 7;
                    if ((tick + stagger) % 22 == 0) {
                        int row = i / 4;
                        int col = i % 4;
                        double xo = (col - 1.5) * 1.6;
                        double zo = -row * 2.0 - 2.0;
                        Location p = getCenter().clone().add(xo, 1.6, zo);
                        // Vertical note burst
                        for (int y = 0; y < 4; y++) {
                            w.spawnParticle(Particle.NOTE,
                                    p.clone().add(0, y * 0.4, 0), 1,
                                    0.15, 0.05, 0.15, Math.random());
                        }
                        // Soul fire rise
                        for (int y = 0; y < 3; y++) {
                            w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                    p.clone().add(0, y * 0.5, 0), 1, 0.1, 0.05, 0.1, 0.005);
                        }

                        // Dissonant note tone
                        if (Math.random() < 0.35) {
                            float pitch = 0.3f + (float) Math.random() * 1.4f;
                            Sound s = Math.random() < 0.5
                                    ? Sound.BLOCK_NOTE_BLOCK_BASS
                                    : Sound.BLOCK_NOTE_BLOCK_HARP;
                            DisplayBuilder.playSound(p, s, 0.8f, pitch);
                        }
                    }
                }
            }

            // Wither ambient occasional
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.25f);
            }

            // Note blocks gently bob
            if (tick % 16 == 0) {
                for (int i = 0; i < noteBlocks.size(); i++) {
                    int row = i / 4;
                    int col = i % 4;
                    double xo = (col - 1.5) * 1.6;
                    double zo = -row * 2.0 - 2.0;
                    float ty = 1.2f + (float) Math.sin(tick * 0.07 + i) * 0.15f;
                    noteBlocks.get(i).animateTo(
                            new Vector3f((float) xo - 0.4f, ty, (float) zo),
                            new AxisAngle4f((float) (tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.8f, 0.8f, 0.8f), 16);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new InfernalMarchingBand(plugin); }
    }

    // ================================================================
    // 15. ECLIPSE EVENT
    //     Sun (shroomlight cluster) gradually covered by translating
    //     obsidian disk. At totality: massive ash cascade, soul fire on
    //     all torch displays, electric spark walls. Then disk passes,
    //     light returns. IMPACT-ONLY damage on totality moment.
    // ================================================================
    public static class EclipseEvent extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> sun = new ArrayList<>();
        private final List<BlockDisplayHandle> disk = new ArrayList<>();
        private final List<ItemDisplayHandle> floorTorches = new ArrayList<>();
        private int phase = 0;
        private boolean totalityFired = false;

        public EclipseEvent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eclipse_event", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(8.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.4f);

            // Sun — 7 shroomlight blocks clustered overhead
            double sunY = 18;
            sun.add(makeBlock(center.clone().add(0, sunY, 0), Material.SHROOMLIGHT, 1.4f, 255, 200, 80));
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = center.clone().add(Math.cos(a) * 1.0, sunY, Math.sin(a) * 1.0);
                sun.add(makeBlock(p, Material.SHROOMLIGHT, 1.0f, 255, 220, 100));
            }

            // Eclipse disk — 9 obsidian blocks tightly packed, starts off-screen on -X side
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Location p = center.clone().add(-15 + dx * 1.0, sunY, dz * 1.0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.OBSIDIAN);
                    h.scale(1.1f, 0.4f, 1.1f).glow(0, 0, 0).interpolation(20, 0);
                    disk.add(h);
                }
            }

            // Floor torches — 8 around perimeter, pre-extinguished (dead bush)
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = center.clone().add(Math.cos(a) * 6, 0.5, Math.sin(a) * 6);
                ItemDisplayHandle t = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                t.scale(0.7f, 0.7f, 0.7f).glow(40, 40, 40).interpolation(12, i);
                floorTorches.add(t);
            }
        }

        private BlockDisplayHandle makeBlock(Location loc, Material m, float s, int r, int g, int b) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, m);
            h.scale(s, s, s).glow(r, g, b).interpolation(20, 0);
            return h;
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            int approachEnd = dur / 3;            // ~200
            int totalityEnd = (dur * 2) / 3;       // ~400

            // Phase logic
            if (phase == 0 && tick > approachEnd) phase = 1;       // totality
            if (phase == 1 && tick > totalityEnd) phase = 2;       // recede
            if (phase == 2 && tick > dur - 60) phase = 3;          // restored

            double sunY = 18;

            // Move disk across sun: -15 -> 0 -> +15
            if (tick % 6 == 0) {
                double progress = (double) tick / dur; // 0..1
                double diskX = -15 + progress * 30; // sweep across
                int idx = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (idx < disk.size()) {
                            float tx = (float) (diskX + dx * 1.0);
                            float tz = (float) (dz * 1.0);
                            disk.get(idx).animateTo(
                                    new Vector3f(tx - 0.55f, (float) sunY, tz - 0.55f),
                                    new AxisAngle4f((float) (tick * 0.02), 0, 1, 0),
                                    new Vector3f(1.1f, 0.4f, 1.1f), 6);
                            idx++;
                        }
                    }
                }
            }

            // Approach phase — building dragon growl
            if (phase == 0 && tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDER_DRAGON_GROWL,
                        0.5f + tick * 0.001f, 0.3f);
            }

            // Totality
            if (phase == 1) {
                if (!totalityFired) {
                    totalityFired = true;
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.4f);
                    triggerImpactDamage(getCenter());
                    // Ignite all soul lantern displays
                    for (ItemDisplayHandle t : floorTorches) {
                        t.glow(80, 200, 255);
                    }
                }

                // Dense ash cascade from ceiling
                if (tick % 2 == 0) {
                    double radius = 9.0;
                    for (int i = 0; i < 24; i++) {
                        double ox = (Math.random() - 0.5) * radius * 2;
                        double oz = (Math.random() - 0.5) * radius * 2;
                        Location p = getCenter().clone().add(ox, 14, oz);
                        w.spawnParticle(Particle.ASH, p, 2, 0.5, 0.4, 0.5, 0.02);
                        w.spawnParticle(Particle.WHITE_ASH, p, 1, 0.5, 0.4, 0.5, 0.01);
                    }
                }

                // Soul fire on torches
                if (tick % 4 == 0) {
                    for (int i = 0; i < floorTorches.size(); i++) {
                        double a = Math.PI * 2 * i / 8;
                        Location p = getCenter().clone().add(Math.cos(a) * 6, 1.0, Math.sin(a) * 6);
                        for (int y = 0; y < 3; y++) {
                            w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                    p.clone().add(0, y * 0.4, 0), 2, 0.15, 0.1, 0.15, 0.01);
                        }
                    }
                }

                // Electric spark walls
                if (tick % 6 == 0) {
                    int pts = 28;
                    double radius = 9.5;
                    for (int i = 0; i < pts; i++) {
                        double a = Math.PI * 2 * i / pts;
                        for (int y = 0; y < 8; y += 2) {
                            Location p = getCenter().clone().add(
                                    Math.cos(a) * radius, y * 0.8, Math.sin(a) * radius);
                            w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1,
                                    0.1, 0.1, 0.1, 0.15);
                        }
                    }
                }
            }

            // Recede / restored
            if (phase == 2 && tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 0.6f);
            }
            if (phase == 3 && tick % 20 == 0) {
                w.spawnParticle(Particle.END_ROD, getCenter().clone().add(0, 4, 0),
                        4, 3, 2, 3, 0.05);
            }
        }

        @Override public AbstractAttack newInstance() { return new EclipseEvent(plugin); }
    }

    // ================================================================
    // 16. THE DISSECTION TABLE
    //     Wide deepslate-tile slab. On surface: bone meal, blaze rods,
    //     nether stars. Restraint straps at corners. Drips below.
    //     Soul fire glows underneath. 25+ displays. Pure horror atmosphere.
    // ================================================================
    public static class DissectionTable extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> structure = new ArrayList<>();
        private final List<ItemDisplayHandle> instruments = new ArrayList<>();
        private int phase = 0;

        public DissectionTable(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dissection_table", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(800);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f);

            // Tabletop — 5x3 deepslate tile grid at Y=1.0
            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location p = center.clone().add(x * 0.95, 1.0, z * 0.95);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.DEEPSLATE_TILES);
                    h.scale(0.01f, 0.01f, 0.01f).interpolation(25, x + z + 5);
                    h.animateTo(new Vector3f(-0.475f, 1.0f, -0.475f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.95f, 0.15f, 0.95f), 25);
                    structure.add(h);
                }
            }

            // Legs — 4 deepslate brick columns
            for (int lx = -1; lx <= 1; lx += 2) {
                for (int lz = -1; lz <= 1; lz += 2) {
                    for (int y = 0; y < 4; y++) {
                        Location p = center.clone().add(lx * 1.7, y * 0.25, lz * 0.85);
                        BlockDisplayHandle leg = displayBuilder.spawnBlock(p, Material.DEEPSLATE_BRICKS);
                        leg.scale(0.25f, 0.25f, 0.25f).interpolation(20, y * 2);
                        structure.add(leg);
                    }
                }
            }

            // Restraint straps — 4 blackstone corner displays
            double[][] corners = {{-1.7, -0.85}, {1.7, -0.85}, {-1.7, 0.85}, {1.7, 0.85}};
            for (double[] c : corners) {
                BlockDisplayHandle s = displayBuilder.spawnBlock(
                        center.clone().add(c[0], 1.15, c[1]), Material.POLISHED_BLACKSTONE_BRICKS);
                s.scale(0.4f, 0.15f, 0.4f).interpolation(20, 5);
                structure.add(s);
            }

            // Instruments on table — bone meal, blaze rods, nether stars (8)
            Material[] toolItems = {
                    Material.BONE_MEAL, Material.BLAZE_ROD, Material.NETHER_STAR,
                    Material.BONE, Material.BLAZE_ROD, Material.BONE_MEAL,
                    Material.NETHER_STAR, Material.BONE
            };
            int[] glow = {220, 220, 220};
            for (int i = 0; i < toolItems.length; i++) {
                double xo = -1.6 + (i * 0.45);
                Location p = center.clone().add(xo, 1.25, (i % 2 == 0) ? -0.3 : 0.3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(toolItems[i]));
                h.scale(0.5f, 0.5f, 0.5f);
                if (toolItems[i] == Material.NETHER_STAR) {
                    h.glow(255, 255, 200);
                } else if (toolItems[i] == Material.BLAZE_ROD) {
                    h.glow(255, 180, 60);
                } else {
                    h.glow(glow[0], glow[1], glow[2]);
                }
                h.interpolation(15, i * 2 + 25);
                instruments.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (phase == 0 && tick > 50) phase = 1;
            if (phase == 1 && tick > config.getDurationTicks() - 80) phase = 2;

            // Drips from table center to floor
            if (tick % 3 == 0) {
                Location tableCenter = getCenter().clone().add(0, 1.0, 0);
                for (int y = 0; y < 5; y++) {
                    Location p = tableCenter.clone().subtract(0,
                            y * 0.2 + Math.random() * 0.1, 0);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, p, 1,
                            0.15, 0.05, 0.15, 0);
                }
            }

            // Soul fire glow under table
            if (tick % 4 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = Math.PI * 2 * i / 6;
                    Location p = getCenter().clone().add(
                            Math.cos(a) * 0.8, 0.05, Math.sin(a) * 0.8);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.2, 0.1, 0.2, 0.005);
                }
            }

            // Burning spots on table — large smoke
            if (tick % 8 == 0) {
                for (int i = 0; i < 3; i++) {
                    double xo = (Math.random() - 0.5) * 3.5;
                    double zo = (Math.random() - 0.5) * 1.6;
                    Location p = getCenter().clone().add(xo, 1.25, zo);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.1, 0.3, 0.1, 0.005);
                }
            }

            // Instruments slowly rotate / float
            if (tick % 12 == 0) {
                for (int i = 0; i < instruments.size(); i++) {
                    double xo = -1.6 + (i * 0.45);
                    float ty = 1.25f + (float) Math.sin(tick * 0.05 + i) * 0.08f;
                    instruments.get(i).animateTo(
                            new Vector3f((float) xo - 0.25f, ty,
                                    (i % 2 == 0) ? -0.55f : 0.05f),
                            new AxisAngle4f((float) (tick * 0.04 + i * 0.5), 0, 1, 0),
                            new Vector3f(0.5f, 0.5f, 0.5f), 12);
                }
            }

            // Sounds
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BONE_BLOCK_STEP, 0.5f, 0.4f);
            }
            if (tick % 90 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.25f);
            }
            if (tick % 18 == 0) {
                DisplayBuilder.playSound(getCenter().clone().add(0, 0.5, 0),
                        Sound.BLOCK_WET_GRASS_STEP, 0.4f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new DissectionTable(plugin); }
    }

    // ================================================================
    // 17. COSMIC TEAR
    //     Vertical jagged "tear" in reality made of crying obsidian +
    //     obsidian. Dense portal swirl interior. Electric spark edges.
    //     End rod drift through. End stone backing with calcite sparkles.
    //     The tear pulses — widens & narrows. ACTIVE HAZARD.
    // ================================================================
    public static class CosmicTear extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> tearEdges = new ArrayList<>();
        private final List<BlockDisplayHandle> backing = new ArrayList<>();
        private final List<ItemDisplayHandle> sparkles = new ArrayList<>();
        private int phase = 0;

        public CosmicTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_tear", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(500);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.4f);

            // Backing — 4x3 end stone grid behind the tear
            for (int y = 0; y < 6; y++) {
                for (int x = -2; x <= 2; x++) {
                    Location p = center.clone().add(x * 0.9, 1 + y * 0.9, -0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.END_STONE_BRICKS);
                    h.scale(0.01f, 0.01f, 0.01f).glow(20, 0, 30).interpolation(30, x + y);
                    h.animateTo(new Vector3f(-0.45f, 1f + y * 0.9f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.2f), 30);
                    backing.add(h);
                }
            }

            // Tear edges — jagged left/right columns of obsidian + crying obsidian (10 each)
            for (int y = 0; y < 7; y++) {
                double xJitterL = -0.7 - Math.random() * 0.5;
                double xJitterR = 0.7 + Math.random() * 0.5;
                Material lMat = (y % 2 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                Material rMat = (y % 2 == 1) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                BlockDisplayHandle l = displayBuilder.spawnBlock(
                        center.clone().add(xJitterL, 1 + y * 0.85, -0.3), lMat);
                BlockDisplayHandle r = displayBuilder.spawnBlock(
                        center.clone().add(xJitterR, 1 + y * 0.85, -0.3), rMat);
                l.scale(0.6f, 0.85f, 0.4f).glow(80, 20, 100).interpolation(25, y);
                r.scale(0.6f, 0.85f, 0.4f).glow(80, 20, 100).interpolation(25, y);
                tearEdges.add(l);
                tearEdges.add(r);
            }

            // Calcite sparkles — 6 in backing
            for (int i = 0; i < 6; i++) {
                Location p = center.clone().add(
                        (Math.random() - 0.5) * 4,
                        1.5 + Math.random() * 4.5,
                        -0.3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                h.scale(0.4f, 0.4f, 0.4f).glow(180, 220, 255).interpolation(15, i * 2);
                sparkles.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            if (phase == 0 && tick > 50) phase = 1;
            if (phase == 1 && tick > dur - 60) phase = 2;

            // Tear pulse — widen / narrow over 80-tick cycle
            if (tick % 10 == 0) {
                double pulse = Math.sin(tick * 0.04) * 0.5 + 1.0; // 0.5..1.5
                int half = tearEdges.size() / 2;
                for (int y = 0; y < half; y++) {
                    BlockDisplayHandle l = tearEdges.get(y * 2);
                    BlockDisplayHandle r = tearEdges.get(y * 2 + 1);
                    float lx = (float) (-0.8 * pulse - 0.3);
                    float rx = (float) (0.8 * pulse - 0.3);
                    l.animateTo(new Vector3f(lx, 1f + y * 0.85f, -0.5f),
                            new AxisAngle4f((float) (tick * 0.03), 0, 0, 1),
                            new Vector3f(0.6f, 0.85f, 0.4f), 10);
                    r.animateTo(new Vector3f(rx, 1f + y * 0.85f, -0.5f),
                            new AxisAngle4f((float) (-tick * 0.03), 0, 0, 1),
                            new Vector3f(0.6f, 0.85f, 0.4f), 10);
                }
                // Pulse sound
                if (tick % 40 == 0) {
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f,
                            0.4f + (float) (pulse - 1.0) * 0.3f);
                }
            }

            // Dense portal interior
            if (tick % 1 == 0) {
                double pulse = Math.sin(tick * 0.04) * 0.5 + 1.0;
                for (int i = 0; i < 30; i++) {
                    double ox = (Math.random() - 0.5) * 1.6 * pulse;
                    double oy = Math.random() * 6;
                    Location p = getCenter().clone().add(ox, 1 + oy, -0.4);
                    w.spawnParticle(Particle.PORTAL, p, 1, 0.1, 0.3, 0.1, 0.05);
                }
            }

            // Edge sparks
            if (tick % 3 == 0) {
                for (int y = 0; y < 7; y++) {
                    Location lp = getCenter().clone().add(-0.9, 1 + y * 0.85, -0.3);
                    Location rp = getCenter().clone().add(0.9, 1 + y * 0.85, -0.3);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, lp, 1, 0.05, 0.05, 0.05, 0.15);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, rp, 1, 0.05, 0.05, 0.05, 0.15);
                }
            }

            // End rod drift through
            if (tick % 6 == 0) {
                for (int i = 0; i < 4; i++) {
                    double ox = (Math.random() - 0.5) * 1.2;
                    double oy = Math.random() * 5;
                    Location p = getCenter().clone().add(ox, 1.5 + oy, -0.4);
                    w.spawnParticle(Particle.END_ROD, p, 1, 0.05, 0.05, 0.4, 0.02);
                }
            }

            // Sparkle bob
            if (tick % 14 == 0) {
                for (int i = 0; i < sparkles.size(); i++) {
                    double xo = (Math.random() - 0.5) * 4;
                    float ty = 1.8f + (float) Math.sin(tick * 0.06 + i) * 1.5f + i * 0.3f;
                    sparkles.get(i).animateTo(
                            new Vector3f((float) xo - 0.2f, ty, -0.5f),
                            new AxisAngle4f((float) (tick * 0.1 + i), 0, 1, 0),
                            new Vector3f(0.4f, 0.4f, 0.4f), 14);
                }
            }

            // Distant dragon ambient
            if (tick % 110 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.4f, 0.3f);
            }
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new CosmicTear(plugin); }
    }

    // ================================================================
    // 18. THE PROPHET'S THRONE
    //     Ornate empty throne (blackstone + nether brick), long lectern.
    //     Floating glowing book, slowly rotating. Carved glyph back panel
    //     channeling soul fire. Ash floor in front. Book "opens" with crit
    //     bursts periodically.
    // ================================================================
    public static class ProphetsThrone extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> throne = new ArrayList<>();
        private ItemDisplayHandle book;
        private int phase = 0;

        public ProphetsThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prophets_throne", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(750);
            config.setCooldownTicks(440);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.6f, 0.3f);

            // Seat — 2x2 nether brick at Y=1.0
            for (int sx = -1; sx <= 0; sx++) {
                for (int sz = -1; sz <= 0; sz++) {
                    Location p = center.clone().add(sx * 0.9 + 0.45, 1.0, sz * 0.9 + 0.45);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.NETHER_BRICKS);
                    h.scale(0.01f, 0.01f, 0.01f).interpolation(25, 0);
                    h.animateTo(new Vector3f(-0.45f, 1.0f, -0.45f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.9f, 0.4f, 0.9f), 25);
                    throne.add(h);
                }
            }

            // Back panel — 4 wide x 5 tall blackstone with carved glyph centerline
            for (int y = 0; y < 5; y++) {
                for (int x = -2; x <= 1; x++) {
                    Location p = center.clone().add(x * 0.9 + 0.45, 1.5 + y * 0.9, -0.95);
                    Material mat = (x == -1 || x == 0)
                            ? Material.POLISHED_BLACKSTONE_BRICKS
                            : Material.BLACKSTONE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                    h.scale(0.01f, 0.01f, 0.01f).interpolation(20, x + y);
                    h.animateTo(new Vector3f(-0.45f, 1.5f + y * 0.9f, -0.95f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.3f), 20);
                    if (x == -1 || x == 0) h.glow(40, 0, 60);
                    throne.add(h);
                }
            }

            // Armrests — 2 blackstone columns each side
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 0; y < 2; y++) {
                    Location p = center.clone().add(side * 1.2, 1.4 + y * 0.4, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                    h.scale(0.4f, 0.4f, 0.7f).glow(50, 0, 70).interpolation(15, y);
                    throne.add(h);
                }
            }

            // Lectern — 3 nether brick blocks extending forward
            for (int z = 0; z < 3; z++) {
                Location p = center.clone().add(0, 0.4, 1.5 + z * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.NETHER_BRICKS);
                h.scale(0.7f, 0.4f, 0.7f).interpolation(15, z);
                throne.add(h);
            }

            // Floating book on throne seat
            book = displayBuilder.spawnItem(
                    center.clone().add(0, 1.8, 0), new ItemStack(Material.WRITTEN_BOOK));
            book.scale(0.01f, 0.01f, 0.01f).glow(255, 220, 100).interpolation(30, 30);
            book.animateTo(new Vector3f(-0.4f, 1.8f, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.8f, 0.8f, 0.8f), 30);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (phase == 0 && tick > 50) phase = 1;
            if (phase == 1 && tick > config.getDurationTicks() - 60) phase = 2;

            // Book rotation + bob
            if (book != null && tick % 4 == 0) {
                float bobY = 1.8f + (float) Math.sin(tick * 0.06) * 0.15f;
                book.animateTo(new Vector3f(-0.4f, bobY, 0),
                        new AxisAngle4f((float) (tick * 0.1), 0, 1, 0),
                        new Vector3f(0.8f, 0.8f, 0.8f), 4);
            }

            // Enchanted hit emanates from book in all directions
            if (tick % 3 == 0) {
                Location bookLoc = getCenter().clone().add(0, 1.95, 0);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8 + tick * 0.05;
                    Location dst = bookLoc.clone().add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5);
                    w.spawnParticle(Particle.ENCHANTED_HIT, dst, 1, 0.1, 0.1, 0.1, 0.05);
                }
                w.spawnParticle(Particle.ENCHANT, bookLoc, 4, 0.6, 0.4, 0.6, 0.3);
            }

            // Soul fire from carved glyph channels (back panel center two columns, all 5 rows)
            if (tick % 5 == 0) {
                for (int y = 0; y < 5; y++) {
                    Location lp = getCenter().clone().add(-0.45, 1.6 + y * 0.9, -0.6);
                    Location rp = getCenter().clone().add(0.45, 1.6 + y * 0.9, -0.6);
                    for (int up = 0; up < 3; up++) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                lp.clone().add(0, up * 0.15, 0), 1, 0.05, 0.05, 0.05, 0.005);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                rp.clone().add(0, up * 0.15, 0), 1, 0.05, 0.05, 0.05, 0.005);
                    }
                }
            }

            // Ash on floor before throne
            if (tick % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double xo = (Math.random() - 0.5) * 4;
                    double zo = 1 + Math.random() * 4;
                    Location p = getCenter().clone().add(xo, 0.2, zo);
                    w.spawnParticle(Particle.ASH, p, 1, 0.4, 0.05, 0.4, 0.005);
                }
            }

            // Periodic "book opens" — scale increase + crit burst
            if (tick % 80 == 50 && book != null) {
                Location bookLoc = getCenter().clone().add(0, 1.95, 0);
                book.animateTo(new Vector3f(-0.7f, 1.9f, 0),
                        new AxisAngle4f((float) (tick * 0.1), 0, 1, 0),
                        new Vector3f(1.4f, 1.4f, 1.4f), 8);
                w.spawnParticle(Particle.CRIT, bookLoc, 30, 1.2, 1.2, 1.2, 0.3);
                DisplayBuilder.playSound(bookLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.9f, 0.5f);
            }
            if (tick % 80 == 65 && book != null) {
                book.animateTo(new Vector3f(-0.4f, 1.8f, 0),
                        new AxisAngle4f((float) (tick * 0.1), 0, 1, 0),
                        new Vector3f(0.8f, 0.8f, 0.8f), 8);
            }

            // Sounds
            if (tick % 100 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.25f);
            }
            if (tick % 70 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.4f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ProphetsThrone(plugin); }
    }

    // ================================================================
    // 19. RAIN OF EYES
    //     Slowly descending ender eye item displays at staggered heights
    //     fill arena. Rotate as they drift. Reach ground -> teleport back
    //     up. End rod glow each, portal drift between. ACTIVE HAZARD.
    // ================================================================
    public static class RainOfEyes extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> eyes = new ArrayList<>();
        private final List<Double> eyeXs = new ArrayList<>();
        private final List<Double> eyeZs = new ArrayList<>();
        private final List<Double> eyeBaseHeights = new ArrayList<>();
        private final List<Double> eyeSpeeds = new ArrayList<>();
        private int phase = 0;
        private static final int EYE_COUNT = 22;

        public RainOfEyes(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rain_of_eyes", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.5);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(25);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(540);
            config.setCooldownTicks(440);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 1.2f, 0.4f);
            for (int i = 0; i < EYE_COUNT; i++) {
                double xo = (Math.random() - 0.5) * 14;
                double zo = (Math.random() - 0.5) * 14;
                double startY = 13 + Math.random() * 3;
                double speed = 0.04 + Math.random() * 0.05; // blocks/tick
                eyeXs.add(xo);
                eyeZs.add(zo);
                eyeBaseHeights.add(startY);
                eyeSpeeds.add(speed);

                Location p = center.clone().add(xo, startY, zo);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ENDER_EYE));
                h.scale(0.6f, 0.6f, 0.6f).glow(60, 0, 200).interpolation(15, i);
                eyes.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (phase == 0 && tick > 30) phase = 1;
            if (phase == 1 && tick > config.getDurationTicks() - 60) phase = 2;

            // Animate eyes downward; teleport back up when at ground
            if (tick % 4 == 0) {
                for (int i = 0; i < eyes.size(); i++) {
                    double cycleLen = 200 + i * 7;
                    double t = (tick + i * 11) % cycleLen;
                    double progress = t / cycleLen;
                    double startY = eyeBaseHeights.get(i);
                    double yPos = startY - progress * (startY + 1.0);
                    if (yPos < -0.5) yPos = -0.5;

                    double xo = eyeXs.get(i);
                    double zo = eyeZs.get(i);
                    // tiny horizontal drift
                    xo += Math.sin(tick * 0.02 + i) * 0.3;
                    zo += Math.cos(tick * 0.02 + i * 1.3) * 0.3;

                    eyes.get(i).animateTo(
                            new Vector3f((float) xo - 0.3f, (float) yPos, (float) zo - 0.3f),
                            new AxisAngle4f((float) (tick * 0.1 + i * 0.5), 0, 1, 0),
                            new Vector3f(0.6f, 0.6f, 0.6f), 4);
                }
            }

            // End rod radiation per eye + portal between
            if (tick % 3 == 0) {
                for (int i = 0; i < eyes.size(); i++) {
                    double xo = eyeXs.get(i) + Math.sin(tick * 0.02 + i) * 0.3;
                    double zo = eyeZs.get(i) + Math.cos(tick * 0.02 + i * 1.3) * 0.3;
                    double startY = eyeBaseHeights.get(i);
                    double cycleLen = 200 + i * 7;
                    double t = (tick + i * 11) % cycleLen;
                    double progress = t / cycleLen;
                    double yPos = Math.max(-0.5, startY - progress * (startY + 1.0));
                    Location p = getCenter().clone().add(xo, yPos + 0.3, zo);
                    w.spawnParticle(Particle.END_ROD, p, 1, 0.1, 0.1, 0.1, 0.005);
                    if (i % 3 == 0) {
                        w.spawnParticle(Particle.PORTAL, p, 2, 0.4, 0.4, 0.4, 0.05);
                    }
                }
            }

            // Sounds
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.3f);
            }
            if (tick % 110 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.6f, 0.4f);
            }
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new RainOfEyes(plugin); }
    }

    // ================================================================
    // 20. THE OSSUARY
    //     Wall of niches (3x4 blackstone frames) filled with stacked bones
    //     + skulls. Bone meal dust drifts from each. Soul fire under each.
    //     Smoke ground fog. Candle item displays before each niche.
    //     30+ displays. Pure horror catacomb atmosphere.
    // ================================================================
    public static class Ossuary extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<BlockDisplayHandle> bones = new ArrayList<>();
        private final List<ItemDisplayHandle> candles = new ArrayList<>();
        private int phase = 0;
        private static final int NICHE_COLS = 3;
        private static final int NICHE_ROWS = 2;

        public Ossuary(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ossuary", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(50);
            config.setDurationTicks(800);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.6f, 0.2f);

            // Build wall background — 9 wide x 7 tall polished blackstone bricks
            for (int wy = 0; wy < 7; wy++) {
                for (int wx = -4; wx <= 4; wx++) {
                    Location p = center.clone().add(wx * 0.9, 0.5 + wy * 0.9, -1.2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                    h.scale(0.01f, 0.01f, 0.01f).interpolation(30, wx + wy);
                    h.animateTo(new Vector3f(-0.45f, 0.5f + wy * 0.9f, -1.2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.3f), 30);
                    walls.add(h);
                }
            }

            // 3x2 niches, each 3 wide x 4 tall
            for (int nr = 0; nr < NICHE_ROWS; nr++) {
                for (int nc = 0; nc < NICHE_COLS; nc++) {
                    double nicheCenterX = (nc - 1) * 2.6;
                    double nicheBaseY = 1.0 + nr * 2.8;

                    // Niche frame — 4 corner blackstone displays (deeper recess)
                    for (int fy = 0; fy < 4; fy++) {
                        for (int fx = -1; fx <= 1; fx += 2) {
                            Location p = center.clone().add(
                                    nicheCenterX + fx * 0.9, nicheBaseY + fy * 0.6, -0.8);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                            h.scale(0.3f, 0.6f, 0.3f).interpolation(20, fy);
                            walls.add(h);
                        }
                    }

                    // Stacked bone pile — 5 bone blocks per niche at varied positions
                    for (int b = 0; b < 5; b++) {
                        double bx = nicheCenterX + (Math.random() - 0.5) * 1.2;
                        double by = nicheBaseY + b * 0.4 + Math.random() * 0.1;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(bx, by, -0.8 - Math.random() * 0.3),
                                Material.BONE_BLOCK);
                        h.scale(0.4f, 0.3f, 0.4f).glow(220, 220, 200).interpolation(20, b * 3);
                        h.animateTo(new Vector3f(-0.2f, 0, -0.2f),
                                new AxisAngle4f((float) (Math.random() * Math.PI),
                                        (float) Math.random(), 1, (float) Math.random()),
                                new Vector3f(0.4f, 0.3f, 0.4f), 20);
                        bones.add(h);
                    }

                    // Skull on top — carved blackstone facing out
                    BlockDisplayHandle skull = displayBuilder.spawnBlock(
                            center.clone().add(nicheCenterX, nicheBaseY + 2.2, -0.8),
                            Material.POLISHED_BLACKSTONE_BRICKS);
                    skull.scale(0.5f, 0.5f, 0.5f).glow(100, 100, 100).interpolation(15, 0);
                    bones.add(skull);

                    // Candle in front of niche on floor
                    Location cp = center.clone().add(nicheCenterX, 0.6, 0.5);
                    ItemDisplayHandle candle = displayBuilder.spawnItem(cp,
                            new ItemStack(Material.CANDLE));
                    candle.scale(0.7f, 0.7f, 0.7f).glow(255, 180, 80).interpolation(15, nc + nr);
                    candles.add(candle);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (phase == 0 && tick > 60) phase = 1;
            if (phase == 1 && tick > config.getDurationTicks() - 80) phase = 2;

            // Bone meal dust drift downward from each niche
            if (tick % 3 == 0) {
                for (int nr = 0; nr < NICHE_ROWS; nr++) {
                    for (int nc = 0; nc < NICHE_COLS; nc++) {
                        double nicheCenterX = (nc - 1) * 2.6;
                        double nicheBaseY = 1.0 + nr * 2.8;
                        for (int d = 0; d < 4; d++) {
                            double jx = nicheCenterX + (Math.random() - 0.5) * 1.4;
                            double jy = nicheBaseY + Math.random() * 2.5;
                            Location p = getCenter().clone().add(jx, jy, -0.7);
                            w.spawnParticle(Particle.WHITE_ASH, p, 1, 0.05, 0.2, 0.05, 0.005);
                            if (d == 0) {
                                w.spawnParticle(Particle.SQUID_INK, p, 1, 0.1, 0.1, 0.1, 0.005);
                            }
                        }
                    }
                }
            }

            // Soul fire under each niche
            if (tick % 4 == 0) {
                for (int nr = 0; nr < NICHE_ROWS; nr++) {
                    for (int nc = 0; nc < NICHE_COLS; nc++) {
                        double nicheCenterX = (nc - 1) * 2.6;
                        double nicheBaseY = 1.0 + nr * 2.8;
                        for (int sf = 0; sf < 3; sf++) {
                            Location p = getCenter().clone().add(
                                    nicheCenterX + (sf - 1) * 0.5,
                                    nicheBaseY - 0.2, -0.7);
                            w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1,
                                    0.1, 0.05, 0.1, 0.005);
                        }
                    }
                }
            }

            // Smoke ground fog along entire wall
            if (tick % 5 == 0) {
                for (int i = 0; i < 14; i++) {
                    double xo = (i - 7) * 0.8 + (Math.random() - 0.5) * 0.4;
                    Location p = getCenter().clone().add(xo, 0.1, 0.3 + Math.random() * 0.6);
                    w.spawnParticle(Particle.SMOKE, p, 1, 0.3, 0.15, 0.3, 0.005);
                    w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.2, 0.05, 0.2, 0.002);
                }
            }

            // Candle flame flicker
            if (tick % 6 == 0) {
                for (int i = 0; i < candles.size(); i++) {
                    int nc = i % NICHE_COLS;
                    int nr = i / NICHE_COLS;
                    double nicheCenterX = (nc - 1) * 2.6;
                    Location flame = getCenter().clone().add(nicheCenterX, 1.0, 0.5);
                    w.spawnParticle(Particle.SMALL_FLAME, flame, 1, 0.05, 0.1, 0.05, 0.005);
                    if (Math.random() < 0.15) {
                        w.spawnParticle(Particle.SMOKE, flame.clone().add(0, 0.2, 0),
                                1, 0.05, 0.05, 0.05, 0.005);
                    }
                    // Bob candle
                    float bobY = 0.6f + (float) Math.sin(tick * 0.1 + i) * 0.05f;
                    candles.get(i).animateTo(
                            new Vector3f((float) nicheCenterX - 0.35f, bobY, 0.15f),
                            new AxisAngle4f((float) (tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 6);
                }
            }

            // Sounds
            if (tick % 90 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BONE_BLOCK_STEP, 0.5f, 0.3f);
            }
            if (tick % 200 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_SKELETON_AMBIENT, 0.4f, 0.5f);
            }
            if (tick % 120 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.5f, 0.2f);
            }
        }

        @Override public AbstractAttack newInstance() { return new Ossuary(plugin); }
    }
}
