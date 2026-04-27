package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
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
 * Devil's Dream Mode — ENVIRONMENTAL EFFECTS pack 4 (entries 31–40).
 * Pure particle + ItemDisplay attacks. No BlockDisplays.
 * Each attack is a multi-phase, recognizable, unique horror set-piece
 * built entirely from ItemDisplays + layered particle systems.
 */
public final class DDEnvFX4 {
    private DDEnvFX4() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new AshTornado(plugin));
        registry.register(new FracturedMirrorHall(plugin));
        registry.register(new ProfaneLibraryCollapse(plugin));
        registry.register(new SoulRiver(plugin));
        registry.register(new InfernalFogBank(plugin));
        registry.register(new WeepingIdolGarden(plugin));
        registry.register(new HangingForest(plugin));
        registry.register(new BloodGeyserField(plugin));
        registry.register(new InfinityMirror(plugin));
        registry.register(new SmolderingReliquary(plugin));
    }

    // ================================================================
    // 31. ASH TORNADO — Slow-moving rotating ash column orbiting the
    //     arena. Helix of BLACKSTONE/BASALT items + soul lantern core.
    // ================================================================
    public static class AshTornado extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shroud = new ArrayList<>();
        private final List<ItemDisplayHandle> innerEmbers = new ArrayList<>();
        private double pathAngle;
        private final double pathRadius = 9.0;
        private int phase = 0;

        public AshTornado(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ash_tornado", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.75);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_AMBIENT, 1.1f, 1.6f);
            pathAngle = Math.random() * Math.PI * 2;
            for (int i = 0; i < 12; i++) {
                Material mat = (i % 2 == 0) ? Material.COBBLED_DEEPSLATE : Material.BLACKSTONE;
                Location p = center.clone().add(
                        Math.cos(pathAngle) * pathRadius, 0.2 + i * 0.85,
                        Math.sin(pathAngle) * pathRadius);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(mat));
                h.scale(0.7f, 1.0f, 0.7f).glow(60, 50, 45).interpolation(20, 0);
                shroud.add(h);
            }
            for (int i = 0; i < 8; i++) {
                Location p = center.clone().add(
                        Math.cos(pathAngle) * pathRadius, 1.2 + i * 1.1,
                        Math.sin(pathAngle) * pathRadius);
                ItemDisplayHandle h = displayBuilder.spawnItem(p,
                        new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.4f, 0.4f, 0.4f).glow(120, 200, 255).interpolation(20, 0);
                innerEmbers.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (phase == 0 && tick > 30) phase = 1;
            if (phase == 1 && tick > config.getDurationTicks() - 40) phase = 2;

            pathAngle += 0.012;
            double cx = Math.cos(pathAngle) * pathRadius;
            double cz = Math.sin(pathAngle) * pathRadius;

            if (tick % 2 == 0) {
                for (int i = 0; i < shroud.size(); i++) {
                    double helixAngle = tick * 0.45 + i * 0.7;
                    double helixR = 1.4 + Math.sin(tick * 0.05 + i) * 0.25;
                    float tx = (float) (cx + Math.cos(helixAngle) * helixR);
                    float ty = 0.2f + i * 0.85f;
                    float tz = (float) (cz + Math.sin(helixAngle) * helixR);
                    float taper = 1.1f - (i * 0.04f);
                    float scale = (phase == 2) ? 0.05f : (0.7f * taper);
                    shroud.get(i).animateTo(
                            new Vector3f(tx - scale / 2f, ty, tz - scale / 2f),
                            new AxisAngle4f((float) (tick * 0.25 + i * 0.4), 0, 1, 0),
                            new Vector3f(scale, scale * 1.4f, scale), 2);
                }
                for (int i = 0; i < innerEmbers.size(); i++) {
                    double a = -tick * 0.55 + i * 0.9;
                    float tx = (float) (cx + Math.cos(a) * 0.6);
                    float ty = 1.2f + i * 1.1f;
                    float tz = (float) (cz + Math.sin(a) * 0.6);
                    innerEmbers.get(i).animateTo(
                            new Vector3f(tx - 0.2f, ty, tz - 0.2f),
                            new AxisAngle4f((float) (tick * 0.4), 0, 1, 0),
                            new Vector3f(0.4f, 0.4f, 0.4f), 2);
                }
            }

            if (phase >= 1) {
                Location columnCenter = getCenter().clone().add(cx, 0, cz);
                for (int y = 0; y < 11; y++) {
                    double yAngle = tick * 0.35 + y * 0.8;
                    for (int s = 0; s < 6; s++) {
                        double rotated = yAngle + s * (Math.PI / 3);
                        double rr = 1.3 + Math.sin(tick * 0.04 + y) * 0.2;
                        Location ringLoc = columnCenter.clone().add(
                                Math.cos(rotated) * rr, y * 0.9, Math.sin(rotated) * rr);
                        w.spawnParticle(Particle.LARGE_SMOKE, ringLoc, 1, 0.1, 0.1, 0.1, 0.01);
                        if (s % 2 == 0) {
                            w.spawnParticle(Particle.ASH, ringLoc, 1, 0.1, 0.1, 0.1, 0.005);
                        }
                    }
                    Location coreLoc = columnCenter.clone().add(
                            (Math.random() - 0.5) * 0.4, y * 0.9, (Math.random() - 0.5) * 0.4);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, coreLoc, 1, 0.08, 0.08, 0.08, 0.005);
                    DisplayBuilder.dustParticles(coreLoc, 1, 0.15, 90, 70, 50, 1.4f);
                }
                Location trail = columnCenter.clone();
                w.spawnParticle(Particle.WHITE_ASH, trail, 6, 1.2, 0.05, 1.2, 0.005);
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, trail.clone().add(0, 0.1, 0),
                        2, 1.0, 0.05, 1.0, 0.002);
            }

            if (tick % 12 == 0) {
                Location columnCenter = getCenter().clone().add(cx, 1, cz);
                Player target = getTargetPlayer();
                float pitch = 1.4f + (float) Math.random() * 0.3f;
                float vol = 0.6f;
                if (target != null) {
                    double d = Math.sqrt(target.getLocation().distanceSquared(columnCenter));
                    vol = (float) Math.max(0.4f, Math.min(1.3f, 1.5f - d * 0.1f));
                }
                DisplayBuilder.playSound(columnCenter, Sound.ENTITY_GHAST_AMBIENT, vol, pitch);
            }
            if (tick % 38 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.WEATHER_RAIN, 0.4f, 1.6f);
            }

            // Damage zone moves with the column
            if (tick % config.getTicksBetweenDamage() == 0) {
                Location columnCenter = getCenter().clone().add(cx, 0.5, cz);
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(columnCenter) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new AshTornado(plugin); }
    }

    // ================================================================
    // 32. THE FRACTURED MIRROR HALL — 6 amethyst cluster monoliths in a
    //     hexagon. Reactive electric arcs lash out at nearby players.
    // ================================================================
    public static class FracturedMirrorHall extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> mirrors = new ArrayList<>();
        private final List<ItemDisplayHandle> clusters = new ArrayList<>();
        private final List<Location> panelCenters = new ArrayList<>();
        private int phase = 0;

        public FracturedMirrorHall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fractured_mirror_hall", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.25);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(3);
            config.setDurationTicks(320);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.1f, 0.6f);
            double r = 6.5;
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = center.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                panelCenters.add(p);
                // Tall mirror monolith — large amethyst shard item
                ItemDisplayHandle mirror = displayBuilder.spawnItem(p,
                        new ItemStack(Material.AMETHYST_SHARD));
                float yaw = (float) (a + Math.PI / 2);
                mirror.scale(0.05f, 0.05f, 0.05f).glow(220, 200, 255).interpolation(20, 0);
                mirror.animateTo(new Vector3f(-1.2f, 0.2f, -0.6f),
                        new AxisAngle4f(yaw, 0, 1, 0),
                        new Vector3f(2.4f, 4.0f, 1.2f), 20);
                mirrors.add(mirror);

                // Embedded amethyst cluster
                ItemDisplayHandle cluster = displayBuilder.spawnItem(
                        p.clone().add(0, 1.8, 0),
                        new ItemStack(Material.AMETHYST_CLUSTER));
                cluster.scale(1.3f, 1.3f, 1.3f).glow(180, 120, 255).interpolation(20, 0);
                clusters.add(cluster);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (phase == 0 && tick > 30) phase = 1;
            if (phase == 1 && tick > config.getDurationTicks() - 30) phase = 2;

            // Cluster slow rotation + breathing scale
            if (tick % 5 == 0) {
                for (int i = 0; i < clusters.size(); i++) {
                    float s = 1.3f + (float) Math.sin(tick * 0.07 + i) * 0.15f;
                    clusters.get(i).animateTo(
                            new Vector3f(-s / 2f, 1.8f, -s / 2f),
                            new AxisAngle4f((float) (tick * 0.06 + i), 0, 1, 0),
                            new Vector3f(s, s, s), 5);
                }
            }
            // Mirror sway
            if (tick % 8 == 0) {
                for (int i = 0; i < mirrors.size(); i++) {
                    double a = Math.PI * 2 * i / 6;
                    float yaw = (float) (a + Math.PI / 2 + Math.sin(tick * 0.04 + i) * 0.05);
                    mirrors.get(i).animateTo(new Vector3f(-1.2f, 0.2f, -0.6f),
                            new AxisAngle4f(yaw, 0, 1, 0),
                            new Vector3f(2.4f, 4.0f, 1.2f), 8);
                }
            }

            // Crit sparkle drift across each panel surface
            if (tick % 3 == 0) {
                for (int i = 0; i < panelCenters.size(); i++) {
                    Location pc = panelCenters.get(i);
                    double a = Math.PI * 2 * i / 6;
                    double tx = -Math.sin(a);
                    double tz = Math.cos(a);
                    for (int s = 0; s < 5; s++) {
                        double along = (Math.random() - 0.5) * 2.4;
                        double up = Math.random() * 4.0;
                        Location pt = pc.clone().add(tx * along, up, tz * along);
                        w.spawnParticle(Particle.CRIT, pt, 1, 0.05, 0.05, 0.05, 0.2);
                        if (s == 0) {
                            DisplayBuilder.dustParticles(pt, 1, 0.1, 200, 150, 255, 1.3f);
                        }
                    }
                }
            }

            // Reactive arcs — when target player is within 4 blocks of any panel
            Player target = getTargetPlayer();
            if (phase >= 1 && target != null && tick % 8 == 0) {
                Location pl = target.getLocation();
                for (int i = 0; i < panelCenters.size(); i++) {
                    Location pc = panelCenters.get(i);
                    if (pc.distanceSquared(pl) < 16) {
                        Location arcStart = pc.clone().add(0, 1.5, 0);
                        Location arcEnd = pl.clone().add(0, 1.0, 0);
                        Vector dir = arcEnd.toVector().subtract(arcStart.toVector());
                        double dist = dir.length();
                        int steps = Math.max(4, (int) (dist * 2));
                        for (int s = 0; s < steps; s++) {
                            double t = (double) s / steps;
                            double jx = (Math.random() - 0.5) * 0.3;
                            double jy = (Math.random() - 0.5) * 0.3;
                            double jz = (Math.random() - 0.5) * 0.3;
                            Location pt = arcStart.clone().add(
                                    dir.getX() * t + jx,
                                    dir.getY() * t + jy,
                                    dir.getZ() * t + jz);
                            w.spawnParticle(Particle.ELECTRIC_SPARK, pt, 1, 0, 0, 0, 0.05);
                        }
                        DisplayBuilder.playSound(pc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.7f, 1.3f);
                    }
                }
            }

            if (tick % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 12;
                    double oz = (Math.random() - 0.5) * 12;
                    Location loc = getCenter().clone().add(ox, 0.1 + Math.random() * 0.6, oz);
                    w.spawnParticle(Particle.WHITE_ASH, loc, 2, 0.4, 0.1, 0.4, 0.005);
                }
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE,
                        0.7f, 0.5f + (float) Math.random() * 0.3f);
            }
            if (tick % 55 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_PHANTOM_AMBIENT, 0.5f, 0.7f);
            }

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FracturedMirrorHall(plugin); }
    }

    // ================================================================
    // 33. THE PROFANE LIBRARY COLLAPSE — Wall of OBSIDIAN tomes with
    //     SHROOMLIGHT spines. Progressive lean + periodic shelf collapse
    //     flinging BONE "books" outward.
    // ================================================================
    public static class ProfaneLibraryCollapse extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shelves = new ArrayList<>();
        private final List<ItemDisplayHandle> spines = new ArrayList<>();
        private final List<ItemDisplayHandle> flyingBooks = new ArrayList<>();
        private int nextCollapseAt = 60;
        private int collapsedIdx = -1;

        public ProfaneLibraryCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("profane_library_collapse", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.4f);
            for (int i = 0; i < 8; i++) {
                double offsetX = (i - 3.5) * 1.6;
                Location p = center.clone().add(offsetX, 0.1, -5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p,
                        new ItemStack(Material.OBSIDIAN));
                h.scale(0.05f, 0.05f, 0.05f).glow(40, 30, 30).interpolation(25, 0);
                h.animateTo(new Vector3f(-0.7f, 0.1f, -0.25f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.4f, 5.0f, 0.5f), 25);
                shelves.add(h);

                for (int row = 0; row < 3; row++) {
                    Location spineLoc = p.clone().add(0, 1.0 + row * 1.3, 0.3);
                    ItemDisplayHandle spine = displayBuilder.spawnItem(
                            spineLoc, new ItemStack(Material.SHROOMLIGHT));
                    spine.scale(0.6f, 0.6f, 0.3f).glow(255, 180, 80).interpolation(25, 0);
                    spines.add(spine);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Progressive lean — entire shelf wall slowly tips forward
            if (tick % 30 == 0) {
                float leanProgress = Math.min(1.0f,
                        (float) tick / config.getDurationTicks());
                float leanAngle = leanProgress * 0.25f;
                for (int i = 0; i < shelves.size(); i++) {
                    if (i == collapsedIdx) continue;
                    shelves.get(i).animateTo(
                            new Vector3f(-0.7f, 0.1f, -0.25f),
                            new AxisAngle4f(leanAngle, 1, 0, 0),
                            new Vector3f(1.4f, 5.0f, 0.5f), 30);
                }
            }

            // Periodic single-shelf collapse
            if (tick == nextCollapseAt && shelves.size() > 0) {
                int idx = (int) (Math.random() * shelves.size());
                collapsedIdx = idx;
                ItemDisplayHandle s = shelves.get(idx);
                double offsetX = (idx - 3.5) * 1.6;
                s.animateTo(new Vector3f(-0.7f, 0.1f, -0.25f),
                        new AxisAngle4f((float) (Math.PI / 2), 1, 0, 0),
                        new Vector3f(1.4f, 5.0f, 0.5f), 14);
                Location origin = getCenter().clone().add(offsetX, 0.1, -5);

                w.spawnParticle(Particle.LARGE_SMOKE, origin.clone().add(0, 2, 0),
                        40, 0.7, 1.5, 0.7, 0.05);
                w.spawnParticle(Particle.ASH, origin.clone().add(0, 2, 0),
                        30, 0.7, 1.5, 0.7, 0.02);
                DisplayBuilder.playSound(origin, Sound.BLOCK_DEEPSLATE_BREAK, 1.3f, 0.5f);
                DisplayBuilder.playSound(origin, Sound.ENTITY_IRON_GOLEM_HURT, 0.9f, 0.6f);

                // Fling 5 BONE "books" outward
                for (int b = 0; b < 5; b++) {
                    Location bookStart = origin.clone().add(0, 1 + Math.random() * 3, 0);
                    ItemDisplayHandle book = displayBuilder.spawnItem(
                            bookStart, new ItemStack(Material.BONE));
                    book.scale(0.3f, 0.5f, 0.4f).glow(220, 210, 180).interpolation(40, 0);
                    float fx = (float) ((Math.random() - 0.5) * 6);
                    float fy = (float) (1 + Math.random() * 2);
                    float fz = (float) (3 + Math.random() * 3);
                    book.animateTo(new Vector3f(fx - 0.15f, fy, fz - 0.2f),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 4), 1, 1, 0),
                            new Vector3f(0.3f, 0.5f, 0.4f), 40);
                    flyingBooks.add(book);

                    Location land = bookStart.clone().add(fx, 0, fz);
                    triggerImpactDamage(land);
                }
                nextCollapseAt = tick + 100 + (int) (Math.random() * 80);
                collapsedIdx = -1;
            }

            // Ash rain from shelf tops
            if (tick % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double offsetX = (i - 3.5) * 1.6;
                    Location top = getCenter().clone().add(offsetX, 5.2, -5);
                    w.spawnParticle(Particle.WHITE_ASH, top, 2, 0.6, 0.1, 0.4, 0.005);
                    w.spawnParticle(Particle.SMOKE, top, 1, 0.5, 0.1, 0.3, 0.005);
                }
            }
            // Spine flicker — sickly yellow dust over each spine
            if (tick % 6 == 0) {
                for (int i = 0; i < spines.size(); i++) {
                    int shelfIdx = i / 3;
                    int row = i % 3;
                    double offsetX = (shelfIdx - 3.5) * 1.6;
                    Location sp = getCenter().clone().add(offsetX, 1.0 + row * 1.3, -4.65);
                    DisplayBuilder.dustParticles(sp, 1, 0.15, 176, 160, 32, 0.9f);
                }
            }
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BONE_BLOCK_STEP, 0.6f, 0.7f);
            }
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.7f, 0.4f);
            }
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                Location wall = getCenter().clone().add(0, 1, -5);
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(wall) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ProfaneLibraryCollapse(plugin); }
    }

    // ================================================================
    // 34. SOUL RIVER — Wide directional river of soul fire. 10 SOUL_SOIL
    //     stones in the riverbed. Splash arcs at the downstream wall.
    // ================================================================
    public static class SoulRiver extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> stones = new ArrayList<>();
        private double flowX = 1, flowZ = 0;

        public SoulRiver(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_river", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.25);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.9f, 0.4f);
            double a = Math.random() * Math.PI * 2;
            flowX = Math.cos(a);
            flowZ = Math.sin(a);
            double perpX = -flowZ;
            double perpZ = flowX;

            for (int i = 0; i < 10; i++) {
                double along = -8 + i * 1.8;
                double off = (Math.random() - 0.5) * 4.0;
                Location p = center.clone().add(
                        flowX * along + perpX * off,
                        0.1,
                        flowZ * along + perpZ * off);
                ItemDisplayHandle stone = displayBuilder.spawnItem(p,
                        new ItemStack(Material.SOUL_SOIL));
                stone.scale(0.85f, 0.5f, 0.85f).glow(120, 200, 255).interpolation(15, 0);
                stones.add(stone);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double perpX = -flowZ;
            double perpZ = flowX;

            // Stone subtle bob — water-pushed feel
            if (tick % 8 == 0) {
                for (int i = 0; i < stones.size(); i++) {
                    double along = -8 + i * 1.8;
                    double off = Math.sin(along * 0.15) * 1.0;
                    float sy = 0.1f + (float) Math.sin(tick * 0.06 + i) * 0.05f;
                    stones.get(i).animateTo(
                            new Vector3f(
                                    (float) (flowX * along + perpX * off) - 0.425f,
                                    sy,
                                    (float) (flowZ * along + perpZ * off) - 0.425f),
                            new AxisAngle4f((float) (Math.sin(tick * 0.04 + i) * 0.1), 0, 1, 0),
                            new Vector3f(0.85f, 0.5f, 0.85f), 8);
                }
            }

            // River body — soul flame current along the flow vector
            for (int s = 0; s < 18; s++) {
                double along = -10 + Math.random() * 20;
                double curve = Math.sin(along * 0.15) * 1.0;
                double width = (Math.random() - 0.5) * 3.5;
                Location pt = getCenter().clone().add(
                        flowX * along + perpX * (width + curve),
                        0.15 + Math.random() * 0.4,
                        flowZ * along + perpZ * (width + curve));
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, pt, 0,
                        flowX * 0.3, 0, flowZ * 0.3, 0.18);
            }

            // Eddy flames around stones
            if (tick % 2 == 0) {
                for (int i = 0; i < stones.size(); i++) {
                    double along = -8 + i * 1.8;
                    double off = Math.sin(along * 0.15) * 1.0;
                    Location stoneLoc = getCenter().clone().add(
                            flowX * along + perpX * off,
                            0.4,
                            flowZ * along + perpZ * off);
                    for (int e = 0; e < 4; e++) {
                        double eAng = tick * 0.2 + e * (Math.PI / 2);
                        Location ep = stoneLoc.clone().add(
                                Math.cos(eAng) * 0.7, 0.05, Math.sin(eAng) * 0.7);
                        w.spawnParticle(Particle.SOUL, ep, 1, 0.04, 0.04, 0.04, 0.002);
                    }
                }
            }

            // Splash where the river hits the "wall" downstream
            if (tick % 4 == 0) {
                Location splash = getCenter().clone().add(flowX * 11, 0.3, flowZ * 11);
                for (int s = 0; s < 6; s++) {
                    double upAng = Math.PI / 2 + (Math.random() - 0.5) * 0.6;
                    double sideAng = (Math.random() - 0.5) * Math.PI;
                    double vx = Math.sin(upAng) * Math.cos(sideAng) * 0.6;
                    double vy = Math.cos(upAng) * 0.7;
                    double vz = Math.sin(upAng) * Math.sin(sideAng) * 0.6;
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, splash, 0, vx, vy, vz, 0.4);
                }
                DisplayBuilder.dustParticles(splash, 3, 0.6, 120, 200, 255, 1.5f);
            }

            if (tick % 22 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SOUL_SAND_STEP, 0.5f, 0.7f);
            }
            if (tick % 55 == 0) {
                Location distant = getCenter().clone().add(flowX * 12, 1, flowZ * 12);
                DisplayBuilder.playSound(distant, Sound.ENTITY_GHAST_WARN, 0.5f, 0.6f);
            }
            if (tick % 70 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.4f);
            }

            // Damage anyone touching the river path
            if (tick % config.getTicksBetweenDamage() == 0) {
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    Location pl = p.getLocation();
                    Vector toP = pl.toVector().subtract(getCenter().toVector());
                    double along = toP.getX() * flowX + toP.getZ() * flowZ;
                    double across = toP.getX() * perpX + toP.getZ() * perpZ;
                    double curve = Math.sin(along * 0.15) * 1.0;
                    double sideDelta = across - curve;
                    if (Math.abs(along) < 11 && Math.abs(sideDelta) < 2.5 && Math.abs(toP.getY()) < 2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                        p.setFireTicks(40);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SoulRiver(plugin); }
    }

    // ================================================================
    // 35. INFERNAL FOG BANK — Dense ground-level fog. 8 soul-lantern
    //     reference points scattered through the fog. Soul flame blinks
    //     suggesting a stalker. Warden/enderman ambient sounds.
    // ================================================================
    public static class InfernalFogBank extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();
        private double rollProgress = 0;
        private double rollX = 1, rollZ = 0;

        public InfernalFogBank(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_fog_bank", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.25);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(7);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(380);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.2f, 0.3f);
            double a = Math.random() * Math.PI * 2;
            rollX = Math.cos(a);
            rollZ = Math.sin(a);

            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8 + Math.random() * 0.3;
                double r = 3 + Math.random() * 6;
                Location p = center.clone().add(Math.cos(angle) * r, 0.3, Math.sin(angle) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p,
                        new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.7f, 0.7f, 0.7f).glow(120, 200, 255).interpolation(15, 0);
                lanterns.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            rollProgress += 0.04;
            double rollOff = Math.sin(rollProgress * 0.05) * 6.0;

            if (tick % 6 == 0) {
                for (int i = 0; i < lanterns.size(); i++) {
                    float yBob = 0.3f + (float) Math.sin(tick * 0.08 + i) * 0.1f;
                    float s = 0.7f + (float) Math.sin(tick * 0.12 + i * 1.3) * 0.1f;
                    lanterns.get(i).animateTo(
                            new Vector3f(-s / 2f, yBob, -s / 2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s, s, s), 6);
                }
            }

            for (int i = 0; i < 22; i++) {
                double ox = (Math.random() - 0.5) * 18 + rollX * rollOff;
                double oz = (Math.random() - 0.5) * 18 + rollZ * rollOff;
                double oy = 0.2 + Math.random() * 1.2;
                Location loc = getCenter().clone().add(ox, oy, oz);
                w.spawnParticle(Particle.LARGE_SMOKE, loc, 1, 0.6, 0.15, 0.6, 0.01);
                if (i % 3 == 0) {
                    w.spawnParticle(Particle.SQUID_INK, loc, 1, 0.4, 0.1, 0.4, 0.005);
                }
            }

            // Random soul flame blinks — "something moves"
            if (tick % 10 == 0) {
                int blinks = 2 + (int) (Math.random() * 3);
                for (int b = 0; b < blinks; b++) {
                    double ox = (Math.random() - 0.5) * 16;
                    double oz = (Math.random() - 0.5) * 16;
                    Location flick = getCenter().clone().add(ox, 0.5 + Math.random() * 1.0, oz);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, flick, 4, 0.2, 0.1, 0.2, 0.01);
                    DisplayBuilder.dustParticles(flick, 2, 0.2, 120, 200, 255, 1.3f);
                }
            }

            if (tick % 45 == 0) {
                double ox = (Math.random() - 0.5) * 14;
                double oz = (Math.random() - 0.5) * 14;
                Location src = getCenter().clone().add(ox, 0.5, oz);
                DisplayBuilder.playSound(src, Sound.ENTITY_WARDEN_AMBIENT, 0.7f, 0.5f);
            }
            if (tick % 70 == 0) {
                double ox = (Math.random() - 0.5) * 14;
                double oz = (Math.random() - 0.5) * 14;
                Location src = getCenter().clone().add(ox, 0.5, oz);
                DisplayBuilder.playSound(src, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 0.6f);
            }
            if (tick % 90 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.6f, 0.3f);
            }

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new InfernalFogBank(plugin); }
    }

    // ================================================================
    // 36. THE WEEPING IDOL GARDEN — 6 humanoid idols built from item
    //     stacks (BONE / END_ROD / SKELETON_SKULL). Phase 1: rotate to
    //     face center. Eyes weep obsidian-tear particles, soul fire base.
    // ================================================================
    public static class WeepingIdolGarden extends EnvironmentalAttack {
        private final List<List<ItemDisplayHandle>> idols = new ArrayList<>();
        private final List<ItemDisplayHandle> ceilingDrips = new ArrayList<>();
        private final double[] idolAngles = new double[6];
        private final Location[] idolCenters = new Location[6];
        private int phase = 0;
        private static final int IDOL_COUNT = 6;

        public WeepingIdolGarden(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("weeping_idol_garden", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.25);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(400);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_WARN, 1.0f, 0.5f);
            double r = 6.0;
            for (int i = 0; i < IDOL_COUNT; i++) {
                double a = Math.PI * 2 * i / IDOL_COUNT;
                idolAngles[i] = a;
                Location base = center.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                idolCenters[i] = base;
                List<ItemDisplayHandle> parts = new ArrayList<>();

                // Body — END_ROD column (tall vertical accent)
                ItemDisplayHandle body = displayBuilder.spawnItem(base,
                        new ItemStack(Material.BLACKSTONE));
                body.scale(0.05f, 0.05f, 0.05f).glow(40, 30, 30).interpolation(20, 0);
                body.animateTo(new Vector3f(-0.4f, 0.1f, -0.3f),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(0.8f, 2.5f, 0.6f), 20);
                parts.add(body);

                // Head — skeleton skull on top
                ItemDisplayHandle head = displayBuilder.spawnItem(
                        base.clone().add(0, 2.6, 0),
                        new ItemStack(Material.SKELETON_SKULL));
                head.scale(0.05f, 0.05f, 0.05f).glow(220, 210, 180).interpolation(20, 0);
                head.animateTo(new Vector3f(-0.5f, 2.6f, -0.5f),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(1.0f, 1.0f, 1.0f), 20);
                parts.add(head);

                // Left arm — END_ROD outcrop
                ItemDisplayHandle armL = displayBuilder.spawnItem(
                        base.clone().add(0, 1.5, 0),
                        new ItemStack(Material.END_ROD));
                armL.scale(0.05f, 0.05f, 0.05f).glow(220, 210, 200).interpolation(20, 0);
                armL.animateTo(new Vector3f(-0.55f, 1.5f, -0.15f),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(0.3f, 1.2f, 0.3f), 20);
                parts.add(armL);

                // Right arm
                ItemDisplayHandle armR = displayBuilder.spawnItem(
                        base.clone().add(0, 1.5, 0),
                        new ItemStack(Material.END_ROD));
                armR.scale(0.05f, 0.05f, 0.05f).glow(220, 210, 200).interpolation(20, 0);
                armR.animateTo(new Vector3f(0.25f, 1.5f, -0.15f),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(0.3f, 1.2f, 0.3f), 20);
                parts.add(armR);

                idols.add(parts);
            }

            // Ceiling clusters — REDSTONE_TORCH bleeding lanterns
            for (int i = 0; i < IDOL_COUNT; i++) {
                double a = Math.PI * 2 * i / IDOL_COUNT;
                Location ceil = center.clone().add(
                        Math.cos(a) * 3.5, 8.5, Math.sin(a) * 3.5);
                ItemDisplayHandle drip = displayBuilder.spawnItem(ceil,
                        new ItemStack(Material.REDSTONE_TORCH));
                drip.scale(0.9f, 0.9f, 0.9f).glow(140, 8, 16).interpolation(20, 0);
                ceilingDrips.add(drip);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (phase == 0 && tick >= 100) {
                phase = 1;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.4f);
                for (int i = 0; i < IDOL_COUNT; i++) {
                    double inward = Math.PI * 2 * i / IDOL_COUNT + Math.PI;
                    idolAngles[i] = inward;
                    List<ItemDisplayHandle> parts = idols.get(i);
                    parts.get(0).animateTo(new Vector3f(-0.4f, 0.1f, -0.3f),
                            new AxisAngle4f((float) inward, 0, 1, 0),
                            new Vector3f(0.8f, 2.5f, 0.6f), 40);
                    parts.get(1).animateTo(new Vector3f(-0.5f, 2.6f, -0.5f),
                            new AxisAngle4f((float) inward, 0, 1, 0),
                            new Vector3f(1.0f, 1.0f, 1.0f), 40);
                    parts.get(2).animateTo(new Vector3f(-0.55f, 1.5f, -0.15f),
                            new AxisAngle4f((float) inward, 0, 1, 0),
                            new Vector3f(0.3f, 1.2f, 0.3f), 40);
                    parts.get(3).animateTo(new Vector3f(0.25f, 1.5f, -0.15f),
                            new AxisAngle4f((float) inward, 0, 1, 0),
                            new Vector3f(0.3f, 1.2f, 0.3f), 40);
                }
            }
            if (phase == 1 && tick >= 240) phase = 2;
            if (phase == 2 && tick >= config.getDurationTicks() - 40) phase = 3;

            for (int i = 0; i < IDOL_COUNT; i++) {
                Location base = idolCenters[i];
                if (base == null) continue;
                double facing = idolAngles[i];

                Location eyeL = base.clone().add(
                        Math.cos(facing) * 0.4 - Math.sin(facing) * 0.18,
                        3.0,
                        Math.sin(facing) * 0.4 + Math.cos(facing) * 0.18);
                Location eyeR = base.clone().add(
                        Math.cos(facing) * 0.4 + Math.sin(facing) * 0.18,
                        3.0,
                        Math.sin(facing) * 0.4 - Math.cos(facing) * 0.18);

                if (tick % 4 == 0) {
                    for (int k = 0; k < 3; k++) {
                        Location dripPt = (k % 2 == 0 ? eyeL : eyeR).clone()
                                .add(0, -k * 0.3, 0);
                        w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, dripPt,
                                1, 0.05, 0.05, 0.05, 0);
                    }
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            base.clone().add(0, 1.5, 0), 2, 0.3, 1.0, 0.3, 0);
                }

                if (tick % 3 == 0) {
                    for (int b = 0; b < 4; b++) {
                        double bAng = tick * 0.1 + b * (Math.PI / 2);
                        Location bp = base.clone().add(
                                Math.cos(bAng) * 0.6, 0.2, Math.sin(bAng) * 0.6);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, bp, 1, 0.05, 0.05, 0.05, 0.005);
                    }
                }
            }

            if (tick % 5 == 0) {
                for (int i = 0; i < ceilingDrips.size(); i++) {
                    double a = Math.PI * 2 * i / IDOL_COUNT;
                    Location ceil = getCenter().clone().add(
                            Math.cos(a) * 3.5, 8.5, Math.sin(a) * 3.5);
                    for (int d = 0; d < 4; d++) {
                        Location dp = ceil.clone().add(
                                (Math.random() - 0.5) * 0.6,
                                -d * 0.5,
                                (Math.random() - 0.5) * 0.6);
                        w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, dp, 1, 0, 0, 0, 0);
                        if (d == 0) DisplayBuilder.dustParticles(dp, 1, 0.1, 140, 16, 16, 1.0f);
                    }
                    float s = 0.9f + (float) Math.sin(tick * 0.06 + i) * 0.08f;
                    ceilingDrips.get(i).animateTo(
                            new Vector3f(-s / 2f, 8.5f, -s / 2f),
                            new AxisAngle4f((float) (tick * 0.05), 0, 1, 0),
                            new Vector3f(s, s, s), 5);
                }
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GHAST_WARN, 0.7f, 0.5f);
            }
            if (phase >= 1 && tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.4f);
            }
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 0.6f);
            }

            // Each idol does AOE damage at its base when phase >= 1
            if (phase >= 1 && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (int i = 0; i < IDOL_COUNT; i++) {
                    Location base = idolCenters[i];
                    if (base == null) continue;
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(base) <= r2) {
                            p.damage(config.getDamage());
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new WeepingIdolGarden(plugin); }
    }

    // ================================================================
    // 37. THE HANGING FOREST — Inverted forest hanging from the ceiling.
    //     5 BAMBOO/BLAZE_ROD trunks + DEAD_BUSH/KELP/FERN hangings.
    //     Particles drift downward.
    // ================================================================
    public static class HangingForest extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> trunks = new ArrayList<>();
        private final List<ItemDisplayHandle> hangings = new ArrayList<>();
        private final double[] trunkBaseX = new double[5];
        private final double[] trunkBaseZ = new double[5];
        private static final int TRUNK_COUNT = 5;
        private static final int CEILING_Y = 9;

        public HangingForest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hanging_forest", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.25);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(380);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.9f, 0.4f);
            for (int t = 0; t < TRUNK_COUNT; t++) {
                double angle = Math.PI * 2 * t / TRUNK_COUNT
                        + (Math.random() - 0.5) * 0.4;
                double r = 4 + Math.random() * 4;
                double bx = Math.cos(angle) * r;
                double bz = Math.sin(angle) * r;
                trunkBaseX[t] = bx;
                trunkBaseZ[t] = bz;
                Material trunkMat = (t % 2 == 0)
                        ? Material.BAMBOO : Material.BLAZE_ROD;
                for (int seg = 0; seg < 3; seg++) {
                    Location p = center.clone().add(bx,
                            CEILING_Y - seg * 1.6, bz);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(trunkMat));
                    h.scale(0.05f, 0.05f, 0.05f)
                            .glow(80, 60, 70).interpolation(25, 0);
                    h.animateTo(new Vector3f(-0.3f, CEILING_Y - seg * 1.6f, -0.3f),
                            new AxisAngle4f((float) (Math.random() * 0.3), 1, 0, 1),
                            new Vector3f(0.6f, 1.6f, 0.6f), 25);
                    trunks.add(h);
                }
                int hangCount = 2 + (int) (Math.random() * 2);
                for (int hb = 0; hb < hangCount; hb++) {
                    Material itemMat;
                    int pick = hb % 3;
                    itemMat = (pick == 0) ? Material.DEAD_BUSH
                            : (pick == 1) ? Material.KELP
                            : Material.FERN;
                    Location hp = center.clone().add(
                            bx + (Math.random() - 0.5) * 1.0,
                            CEILING_Y - 5 - Math.random() * 1.5,
                            bz + (Math.random() - 0.5) * 1.0);
                    ItemDisplayHandle h = displayBuilder.spawnItem(hp,
                            new ItemStack(itemMat));
                    h.scale(0.7f, 1.0f, 0.7f).glow(120, 100, 80).interpolation(25, 0);
                    h.animateTo(new Vector3f(-0.35f, 0f, -0.35f),
                            new AxisAngle4f((float) Math.PI, 1, 0, 0),
                            new Vector3f(0.7f, 1.0f, 0.7f), 25);
                    hangings.add(h);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            if (tick % 8 == 0) {
                for (int i = 0; i < hangings.size(); i++) {
                    float swayAng = (float) (Math.PI + Math.sin(tick * 0.04 + i) * 0.1);
                    hangings.get(i).animateTo(
                            new Vector3f(-0.35f, 0f, -0.35f),
                            new AxisAngle4f(swayAng, 1, 0, 0.2f),
                            new Vector3f(0.7f, 1.0f, 0.7f), 8);
                }
            }

            if (tick % 2 == 0) {
                for (int i = 0; i < 18; i++) {
                    double ox = (Math.random() - 0.5) * 14;
                    double oz = (Math.random() - 0.5) * 14;
                    double oy = CEILING_Y - 0.5 - Math.random() * 2.5;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    if (i % 2 == 0) {
                        w.spawnParticle(Particle.WHITE_ASH, loc, 1, 0.4, 0.05, 0.4, 0.005);
                    } else {
                        w.spawnParticle(Particle.WARPED_SPORE, loc, 1, 0.4, 0.05, 0.4, 0.002);
                    }
                }
            }

            // Soul fire from inverted "roots" at top
            if (tick % 3 == 0) {
                for (int t = 0; t < TRUNK_COUNT; t++) {
                    Location rootLoc = getCenter().clone().add(
                            trunkBaseX[t] + 0.4, CEILING_Y + 0.5, trunkBaseZ[t] + 0.4);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            rootLoc, 2, 0.3, 0.1, 0.3, 0.005);
                    DisplayBuilder.dustParticles(rootLoc, 1, 0.2, 120, 200, 255, 1.2f);
                }
            }

            // Sickly yellow witch wisps drifting through the canopy
            if (tick % 5 == 0) {
                for (int i = 0; i < 6; i++) {
                    double ox = (Math.random() - 0.5) * 12;
                    double oz = (Math.random() - 0.5) * 12;
                    double oy = CEILING_Y - 3 - Math.random() * 1.5;
                    Location wp = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.WITCH, wp, 1, 0.3, 0.1, 0.3, 0.01);
                    DisplayBuilder.dustParticles(wp, 1, 0.1, 176, 160, 32, 0.9f);
                }
            }

            if (tick % 35 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_WART_BLOCK_PLACE, 0.6f, 0.6f);
            }
            if (tick % 55 == 0) {
                DisplayBuilder.playSound(getCenter().clone().add(0, CEILING_Y - 1, 0),
                        Sound.ENTITY_BAT_AMBIENT, 0.7f, 0.7f);
            }
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_PHANTOM_AMBIENT, 0.6f, 0.4f);
            }

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (int t = 0; t < TRUNK_COUNT; t++) {
                    Location base = getCenter().clone().add(trunkBaseX[t], 0, trunkBaseZ[t]);
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(base) <= r2) {
                            p.damage(config.getDamage());
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new HangingForest(plugin); }
    }

    // ================================================================
    // 38. BLOOD GEYSER FIELD — 5 vent points (REDSTONE_TORCH items) on
    //     staggered timers. Eruptions = redstone column + impact damage.
    // ================================================================
    public static class BloodGeyserField extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> vents = new ArrayList<>();
        private final List<Location> ventLocs = new ArrayList<>();
        private final int[] nextEruption = new int[5];
        private final int[] eruptionEnd = new int[5];

        public BloodGeyserField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blood_geyser_field", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(7.5);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(7.5);
            config.setImpactRadius(2.5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(380);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.5f);
            for (int i = 0; i < 5; i++) {
                double angle = Math.PI * 2 * i / 5
                        + (Math.random() - 0.5) * 0.5;
                double r = 4 + Math.random() * 4;
                Location p = center.clone().add(
                        Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                ventLocs.add(p);
                ItemDisplayHandle vent = displayBuilder.spawnItem(p,
                        new ItemStack(Material.REDSTONE_TORCH));
                vent.scale(0.95f, 0.4f, 0.95f).glow(140, 8, 16).interpolation(15, 0);
                vents.add(vent);
                nextEruption[i] = 30 + i * 18 + (int) (Math.random() * 10);
                eruptionEnd[i] = -1;
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            for (int i = 0; i < ventLocs.size(); i++) {
                Location v = ventLocs.get(i);

                // Pre-eruption rumble
                if (tick >= nextEruption[i] - 20 && tick < nextEruption[i]) {
                    if (tick % 4 == 0) {
                        DisplayBuilder.dustParticles(v, 3, 0.5, 200, 60, 60, 1.4f);
                        w.spawnParticle(Particle.SMOKE, v.clone().add(0, 0.5, 0),
                                3, 0.4, 0.2, 0.4, 0.02);
                    }
                    if (tick == nextEruption[i] - 20) {
                        DisplayBuilder.playSound(v, Sound.BLOCK_LAVA_AMBIENT, 0.8f, 0.3f);
                    }
                }

                // Trigger eruption
                if (tick == nextEruption[i]) {
                    eruptionEnd[i] = tick + 60;
                    DisplayBuilder.playSound(v, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.5f);
                    vents.get(i).animateTo(new Vector3f(-0.475f, 0.1f, -0.475f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.95f, 0.6f, 0.95f), 6);
                    triggerImpactDamage(v);
                }

                // Active eruption — blood/redstone column
                if (eruptionEnd[i] > 0 && tick < eruptionEnd[i]) {
                    for (int y = 0; y < 10; y++) {
                        Location col = v.clone().add(
                                (Math.random() - 0.5) * 1.0,
                                0.4 + y * 1.0,
                                (Math.random() - 0.5) * 1.0);
                        w.spawnParticle(Particle.DRIPPING_DRIPSTONE_LAVA, col, 1, 0.15, 0.15, 0.15, 0);
                        DisplayBuilder.dustParticles(col, 1, 0.15, 140, 16, 16, 1.4f);
                        if (y % 2 == 0) {
                            w.spawnParticle(Particle.SMOKE, col, 1,
                                    0.25, 0.15, 0.25, 0.02);
                        }
                        if (y == 0) {
                            w.spawnParticle(Particle.LANDING_OBSIDIAN_TEAR, col, 2, 0.4, 0.1, 0.4, 0.05);
                        }
                    }
                    if (tick % 20 == 0 && tick != nextEruption[i]) {
                        triggerImpactDamage(v);
                    }
                }

                // Eruption ends
                if (eruptionEnd[i] > 0 && tick == eruptionEnd[i]) {
                    DisplayBuilder.playSound(v, Sound.BLOCK_LAVA_EXTINGUISH, 0.9f, 0.6f);
                    w.spawnParticle(Particle.WHITE_ASH, v.clone().add(0, 0.2, 0),
                            30, 1.2, 0.05, 1.2, 0.005);
                    DisplayBuilder.dustParticles(v.clone().add(0, 0.15, 0),
                            8, 1.2, 60, 40, 40, 1.5f);
                    vents.get(i).animateTo(new Vector3f(-0.475f, 0.1f, -0.475f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.95f, 0.4f, 0.95f), 8);
                    eruptionEnd[i] = -1;
                    nextEruption[i] = tick + 70 + (int) (Math.random() * 50);
                }
            }

            if (tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_AMBIENT, 0.5f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new BloodGeyserField(plugin); }
    }

    // ================================================================
    // 39. THE INFINITY MIRROR — 2 facing OBSIDIAN slab portals + 8
    //     ENDER_EYE corridor markers. Spark arcs leap between portals.
    // ================================================================
    public static class InfinityMirror extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> panels = new ArrayList<>();
        private final List<ItemDisplayHandle> eyes = new ArrayList<>();
        private double axisX = 1, axisZ = 0;

        public InfinityMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infinity_mirror", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.4f, 0.4f);
            double a = Math.random() * Math.PI * 2;
            axisX = Math.cos(a);
            axisZ = Math.sin(a);
            for (int side = 0; side < 2; side++) {
                int sign = (side == 0) ? 1 : -1;
                Location p = center.clone().add(axisX * 4 * sign, 0.1, axisZ * 4 * sign);
                ItemDisplayHandle h = displayBuilder.spawnItem(p,
                        new ItemStack(Material.OBSIDIAN));
                float yaw = (float) (a + Math.PI / 2);
                h.scale(0.05f, 0.05f, 0.05f).glow(140, 0, 200).interpolation(20, 0);
                h.animateTo(new Vector3f(-2.5f, 0.1f, -0.15f),
                        new AxisAngle4f(yaw, 0, 1, 0),
                        new Vector3f(5.0f, 5.0f, 0.3f), 20);
                panels.add(h);
            }
            double perpX = -axisZ;
            double perpZ = axisX;
            for (int e = 0; e < 8; e++) {
                int side = (e < 4) ? 1 : -1;
                int slot = e % 4;
                double along = -3 + slot * 2;
                Location p = center.clone().add(
                        axisX * along + perpX * 1.4 * side,
                        1.2 + (slot % 2) * 1.2,
                        axisZ * along + perpZ * 1.4 * side);
                ItemDisplayHandle eye = displayBuilder.spawnItem(p,
                        new ItemStack(Material.ENDER_EYE));
                eye.scale(0.7f, 0.7f, 0.7f).glow(100, 255, 100).interpolation(15, 0);
                eyes.add(eye);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double perpX = -axisZ;
            double perpZ = axisX;

            if (tick % 5 == 0) {
                for (int i = 0; i < eyes.size(); i++) {
                    float s = 0.7f + (float) Math.sin(tick * 0.1 + i) * 0.1f;
                    eyes.get(i).animateTo(
                            new Vector3f(-s / 2f, 0f, -s / 2f),
                            new AxisAngle4f((float) (tick * 0.15 + i), 0, 1, 0),
                            new Vector3f(s, s, s), 5);
                }
            }

            // Panel surface shimmer — purple dust + reverse_portal pull
            if (tick % 6 == 0) {
                for (int side = 0; side < 2; side++) {
                    int sign = (side == 0) ? 1 : -1;
                    Location pc = getCenter().clone().add(axisX * 4 * sign, 2.5, axisZ * 4 * sign);
                    for (int s = 0; s < 6; s++) {
                        double along = (Math.random() - 0.5) * 4.0;
                        double up = (Math.random() - 0.5) * 4.0;
                        Location pt = pc.clone().add(perpX * along, up, perpZ * along);
                        w.spawnParticle(Particle.REVERSE_PORTAL, pt, 1, 0.05, 0.05, 0.05, 0.05);
                        DisplayBuilder.dustParticles(pt, 1, 0.1, 128, 48, 192, 1.2f);
                    }
                }
            }

            // Corridor particles — portal + end_rod filling the space between panels
            for (int s = 0; s < 14; s++) {
                double along = -3.5 + Math.random() * 7;
                double width = (Math.random() - 0.5) * 2.4;
                double height = Math.random() * 4.5;
                Location pt = getCenter().clone().add(
                        axisX * along + perpX * width,
                        0.2 + height,
                        axisZ * along + perpZ * width);
                w.spawnParticle(Particle.PORTAL, pt, 1, 0.1, 0.1, 0.1, 0.5);
                if (s % 2 == 0) {
                    w.spawnParticle(Particle.END_ROD, pt, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Spark arcs between panels every 14 ticks
            if (tick % 14 == 0) {
                Location p1 = getCenter().clone().add(axisX * 4, 1 + Math.random() * 3, axisZ * 4);
                Location p2 = getCenter().clone().add(-axisX * 4, 1 + Math.random() * 3, -axisZ * 4);
                Vector dir = p2.toVector().subtract(p1.toVector());
                int steps = 16;
                for (int s = 0; s < steps; s++) {
                    double t = (double) s / steps;
                    double jx = (Math.random() - 0.5) * 0.4;
                    double jy = (Math.random() - 0.5) * 0.4;
                    double jz = (Math.random() - 0.5) * 0.4;
                    Location pt = p1.clone().add(
                            dir.getX() * t + jx,
                            dir.getY() * t + jy,
                            dir.getZ() * t + jz);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, pt, 1, 0, 0, 0, 0.05);
                    if (s % 4 == 0) DisplayBuilder.dustParticles(pt, 1, 0.1, 200, 100, 255, 1.4f);
                }
                DisplayBuilder.playSound(p1, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.4f);
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.4f);
            }
            if (tick % 55 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDERMAN_STARE, 0.6f, 0.5f);
            }

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(getCenter()) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new InfinityMirror(plugin); }
    }

    // ================================================================
    // 40. THE SMOLDERING RELIQUARY — Massive cursed reliquary altar:
    //     4-pillar tarnished frame + locked iron lid + glowing relic
    //     levitating inside + magma seams between segments + lava drips
    //     from the box's cracks. Pure environmental religious horror.
    // ================================================================
    public static class SmolderingReliquary extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> frame = new ArrayList<>();
        private final List<ItemDisplayHandle> pillars = new ArrayList<>();
        private final List<ItemDisplayHandle> seams = new ArrayList<>();
        private ItemDisplayHandle lid;
        private ItemDisplayHandle relic;
        private final List<Location> seamPoints = new ArrayList<>();

        public SmolderingReliquary(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("smoldering_reliquary", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.5);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(380);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.4f);
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.3f);

            // Box body — 5 stacked tarnished segments, BLACKSTONE/DEEPSLATE_BRICKS alternating
            for (int y = 0; y < 5; y++) {
                Material mat = (y % 2 == 0) ? Material.BLACKSTONE : Material.DEEPSLATE_BRICKS;
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(0, -2.0 + y * 0.6, 0),
                        new ItemStack(mat));
                h.scale(0.05f, 0.05f, 0.05f).glow(60, 30, 20).interpolation(40, y * 3);
                h.animateTo(new Vector3f(-1.5f, (float)(0.2 + y * 0.6), -1.5f),
                        new AxisAngle4f((float)(y * 0.05), 0, 1, 0),
                        new Vector3f(3.0f, 0.6f, 2.4f), 40);
                frame.add(h);
                seamPoints.add(center.clone().add(0, 0.2 + y * 0.6, 0));
            }

            // 4 corner pillars — NETHERITE_INGOT (tarnished iron)
            double[][] corners = {{1.7, 1.4}, {-1.7, 1.4}, {1.7, -1.4}, {-1.7, -1.4}};
            for (int i = 0; i < 4; i++) {
                ItemDisplayHandle p = displayBuilder.spawnItem(
                        center.clone().add(corners[i][0], -1.0, corners[i][1]),
                        new ItemStack(Material.NETHERITE_INGOT));
                p.scale(0.05f, 0.05f, 0.05f).glow(120, 70, 50).interpolation(45, 8);
                p.animateTo(new Vector3f((float)corners[i][0] - 0.25f, 0.0f, (float)corners[i][1] - 0.25f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.5f, 3.4f, 0.5f), 45);
                pillars.add(p);
            }

            // Locked iron lid — GOLD_BLOCK item, slightly raised
            lid = displayBuilder.spawnItem(
                    center.clone().add(0, 4.0, 0),
                    new ItemStack(Material.GOLD_INGOT));
            lid.scale(0.05f, 0.05f, 0.05f).glow(255, 200, 80).interpolation(50, 25);
            lid.animateTo(new Vector3f(-1.6f, 3.4f, -1.3f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(3.2f, 0.4f, 2.6f), 50);
            frame.add(lid);

            // Levitating cursed relic inside — NETHER_STAR
            relic = displayBuilder.spawnItem(
                    center.clone().add(0, 1.6, 0),
                    new ItemStack(Material.NETHER_STAR));
            relic.scale(0.05f, 0.05f, 0.05f).glow(255, 60, 200).interpolation(60, 40);
            relic.animateTo(new Vector3f(-0.4f, 1.6f, -0.4f),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.8f, 0.8f, 0.8f), 60);

            // 6 magma seam lights — SHROOMLIGHT items in box seams
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location s = center.clone().add(Math.cos(a) * 1.4, 0.5 + (i % 3) * 0.6, Math.sin(a) * 1.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(s, new ItemStack(Material.SHROOMLIGHT));
                h.scale(0.05f, 0.05f, 0.05f).glow(255, 100, 0).interpolation(35, 15);
                h.animateTo(new Vector3f((float)(Math.cos(a)*1.4) - 0.2f, (float)(0.5 + (i%3)*0.6), (float)(Math.sin(a)*1.2) - 0.2f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.4f, 0.4f, 0.4f), 35);
                seams.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            Location c = getCenter();

            // Relic levitates and rotates — Lissajous up/down + dual-axis spin
            if (tick % 3 == 0 && relic != null) {
                float yOff = 1.6f + (float) Math.sin(tick * 0.05) * 0.25f;
                float spin = tick * 0.06f;
                relic.animateTo(new Vector3f(-0.4f, yOff, -0.4f),
                        new AxisAngle4f(spin, 0.5f, 1f, 0.3f),
                        new Vector3f(0.8f + (float) Math.sin(tick * 0.08) * 0.08f), 3);
            }

            // Lid rattles — small tilt oscillation, suggesting it's straining to open
            if (tick % 6 == 0 && lid != null) {
                float tilt = (float) Math.sin(tick * 0.18) * 0.12f;
                lid.animateTo(new Vector3f(-1.6f, 3.4f + (float) Math.abs(Math.sin(tick * 0.18)) * 0.12f, -1.3f),
                        new AxisAngle4f(tilt, 1, 0, 0),
                        new Vector3f(3.2f, 0.4f, 2.6f), 6);
            }

            // Seam shroomlights pulse with phase delay (wave around the box)
            if (tick % 4 == 0) {
                for (int i = 0; i < seams.size(); i++) {
                    float s = 0.4f + (float) Math.sin(tick * 0.15 + i * 1.05) * 0.12f;
                    double a = Math.PI * 2 * i / 6;
                    seams.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a)*1.4) - s/2f, (float)(0.5 + (i%3)*0.6), (float)(Math.sin(a)*1.2) - s/2f),
                            new AxisAngle4f(tick * 0.04f, 0, 1, 0),
                            new Vector3f(s), 4);
                }
            }

            // Halo of soul-fire above lid — rotating ring
            if (tick % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8 + tick * 0.05;
                    double r = 1.6 + Math.sin(tick * 0.06 + i) * 0.2;
                    Location pt = c.clone().add(Math.cos(a) * r, 4.4 + Math.sin(tick * 0.04 + i) * 0.2, Math.sin(a) * r);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, pt, 1, 0.05, 0.05, 0.05, 0.001);
                }
            }

            // Vertical helical column of cursed energy from relic, upward through lid gap
            if (tick % 2 == 0) {
                for (int i = 0; i < 4; i++) {
                    double phase = tick * 0.18 + i * (Math.PI / 2);
                    double r = 0.35;
                    double yy = 1.6 + (((tick + i * 6) % 60) / 60.0) * 2.6;
                    Location pt = c.clone().add(Math.cos(phase) * r, yy, Math.sin(phase) * r);
                    w.spawnParticle(Particle.SCULK_SOUL, pt, 1, 0, 0, 0, 0);
                    if (i % 2 == 0) {
                        w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(196, 60, 220), 1.2f));
                    }
                }
            }

            // Magma drips from box seams — SCULK_CHARGE_POP and FALLING_LAVA
            if (tick % 3 == 0) {
                for (Location sp : seamPoints) {
                    if (Math.random() < 0.45) {
                        double a = Math.random() * Math.PI * 2;
                        Location dropPt = sp.clone().add(Math.cos(a) * 1.55, 0, Math.sin(a) * 1.25);
                        w.spawnParticle(Particle.FALLING_LAVA, dropPt, 1, 0.05, 0.05, 0.05, 0);
                        w.spawnParticle(Particle.DRIPPING_DRIPSTONE_LAVA, dropPt, 1, 0.05, 0.05, 0.05, 0);
                    }
                }
            }

            // Smoke + ash rising from box top
            if (tick % 2 == 0) {
                Location top = c.clone().add(0, 4.0, 0);
                for (int i = 0; i < 3; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 1.4;
                    Location pt = top.clone().add(Math.cos(a) * r, Math.random() * 1.2, Math.sin(a) * r);
                    w.spawnParticle(Particle.LARGE_SMOKE, pt, 1, 0.05, 0.05, 0.05, 0.005);
                    if (Math.random() < 0.4) w.spawnParticle(Particle.ASH, pt, 1, 0.1, 0.1, 0.1, 0.003);
                }
            }

            // Dust ring at base — slow rotation
            if (tick % 5 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12 + tick * 0.012;
                    double r = 2.2 + Math.sin(tick * 0.04 + i) * 0.2;
                    Location pt = c.clone().add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
                    w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(140, 16, 16), 1.4f));
                }
            }

            // Ambient sounds — bell, stone, lava
            if (tick % 70 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.7f, 0.4f);
            if (tick % 40 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.3f);
            if (tick % 55 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.7f, 0.4f);
            if (tick % 90 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, 0.5f, 0.3f);

            // Damage zone — anyone within radius of the box
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                        p.setFireTicks(30);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SmolderingReliquary(plugin); }
    }
}
