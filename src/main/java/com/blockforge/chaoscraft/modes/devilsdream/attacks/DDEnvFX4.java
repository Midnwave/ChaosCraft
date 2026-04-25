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
 * Devil's Dream Mode — ENVIRONMENTAL EFFECTS pack 4 (entries 31–40).
 * Particle / atmosphere / sound focused. Each effect uses 8+ displays
 * (some 25+) plus dense layered particle systems and multi-phase lifecycle.
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
        registry.register(new SmolderingGiantsHand(plugin));
    }

    // ================================================================
    // 31. ASH TORNADO
    //     Slow-moving rotating ash column orbiting the arena perimeter.
    //     12 BlockDisplay basalt/blackstone "shroud" segments stacked in
    //     a tight 3-block-wide helix, ~10 blocks tall, with inner soul
    //     fire glow and outer large_smoke + ash residue trail.
    // ================================================================
    public static class AshTornado extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> shroud = new ArrayList<>();
        private final List<ItemDisplayHandle> innerEmbers = new ArrayList<>();
        private double pathAngle;
        private final double pathRadius = 9.0;
        private int phase = 0; // 0=spawn, 1=active, 2=dissipate

        public AshTornado(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ash_tornado", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.5);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_AMBIENT, 1.1f, 1.6f);
            pathAngle = Math.random() * Math.PI * 2;
            // 12 stacked shroud segments — basalt/blackstone alternating
            for (int i = 0; i < 12; i++) {
                Material mat = (i % 2 == 0) ? Material.BASALT : Material.BLACKSTONE;
                Location p = center.clone().add(
                        Math.cos(pathAngle) * pathRadius, 0.2 + i * 0.85,
                        Math.sin(pathAngle) * pathRadius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                h.scale(0.05f, 0.05f, 0.05f).glow(60, 50, 45).interpolation(20, 0);
                shroud.add(h);
            }
            // 8 inner embers — soul lantern items glowing through smoke
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

            // Phase progression
            if (phase == 0 && tick > 30) phase = 1;
            if (phase == 1 && tick > config.getDurationTicks() - 40) phase = 2;

            // Tornado moves along a circular arena path at walking pace
            pathAngle += 0.012;
            double cx = Math.cos(pathAngle) * pathRadius;
            double cz = Math.sin(pathAngle) * pathRadius;

            // Update shroud segments — each segment lags slightly to look like a twisting helix
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

            // Particle column — outer smoke + inner soul flame core
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
                            w.spawnParticle(Particle.SMOKE, ringLoc, 1, 0.1, 0.1, 0.1, 0.005);
                        }
                    }
                    // Inner soul flame core
                    Location coreLoc = columnCenter.clone().add(
                            (Math.random() - 0.5) * 0.4, y * 0.9, (Math.random() - 0.5) * 0.4);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, coreLoc, 1, 0.08, 0.08, 0.08, 0.005);
                    DisplayBuilder.dustParticles(coreLoc, 1, 0.15, 90, 70, 50, 1.4f);
                }
                // Ash residue trail at ground level behind tornado
                Location trail = columnCenter.clone();
                w.spawnParticle(Particle.WHITE_ASH, trail, 6, 1.2, 0.05, 1.2, 0.005);
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, trail.clone().add(0, 0.1, 0),
                        2, 1.0, 0.05, 1.0, 0.002);
            }

            // Dynamic wind sound — louder when tornado is near a player
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
        }

        @Override public AbstractAttack newInstance() { return new AshTornado(plugin); }
    }

    // ================================================================
    // 32. THE FRACTURED MIRROR HALL
    //     6 large flat calcite/amethyst panels in a hexagon facing inward.
    //     Each panel = block display "wall" + amethyst cluster item display
    //     embedded in face. Crit sparkles drift across surfaces. When a
    //     player approaches, electric sparks arc out toward them.
    // ================================================================
    public static class FracturedMirrorHall extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> panels = new ArrayList<>();
        private final List<ItemDisplayHandle> clusters = new ArrayList<>();
        private final List<Location> panelCenters = new ArrayList<>();
        private int phase = 0;

        public FracturedMirrorHall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fractured_mirror_hall", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.5);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(25);
            config.setDamageDelayTicks(15);
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
                // Flat panel — wide x-axis, tall y-axis, thin z-axis, rotated to face center
                Material mat = (i % 2 == 0) ? Material.CALCITE : Material.AMETHYST_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                float yaw = (float) (a + Math.PI / 2);
                h.scale(2.4f, 4.0f, 0.25f).glow(220, 200, 255).interpolation(20, 0);
                h.animateTo(new Vector3f(-1.2f, 0.2f, -0.125f),
                        new AxisAngle4f(yaw, 0, 1, 0),
                        new Vector3f(2.4f, 4.0f, 0.25f), 20);
                panels.add(h);

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

            // Crit sparkle drift across each panel surface
            if (tick % 3 == 0) {
                for (int i = 0; i < panelCenters.size(); i++) {
                    Location pc = panelCenters.get(i);
                    double a = Math.PI * 2 * i / 6;
                    // Tangent vector along the panel face
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
                        // Arc particle line from panel face to player chest
                        Location arcStart = pc.clone().add(0, 1.5, 0);
                        Location arcEnd = pl.clone().add(0, 1.0, 0);
                        Vector dir = arcEnd.toVector().subtract(arcStart.toVector());
                        double dist = dir.length();
                        int steps = Math.max(4, (int) (dist * 2));
                        for (int s = 0; s < steps; s++) {
                            double t = (double) s / steps;
                            // Add slight zig-zag jitter
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

            // White ash drift between panels at ground level
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
        }

        @Override public AbstractAttack newInstance() { return new FracturedMirrorHall(plugin); }
    }

    // ================================================================
    // 33. THE PROFANE LIBRARY COLLAPSE
    //     A wall of 8 tall blackstone shelves with shroomlight book spines.
    //     Slow progressive lean over the fight, periodic shelf "collapse"
    //     events that pitch one unit forward and fling bone-block books.
    //     20+ displays.
    // ================================================================
    public static class ProfaneLibraryCollapse extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> shelves = new ArrayList<>();
        private final List<ItemDisplayHandle> spines = new ArrayList<>();
        private final List<ItemDisplayHandle> flyingBooks = new ArrayList<>();
        private int nextCollapseAt = 60;
        private int collapsedIdx = -1;

        public ProfaneLibraryCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("profane_library_collapse", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.4f);
            // Wall of 8 shelves spaced along x-axis, each at z = -5
            for (int i = 0; i < 8; i++) {
                double offsetX = (i - 3.5) * 1.6;
                Location p = center.clone().add(offsetX, 0.1, -5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p,
                        Material.POLISHED_BLACKSTONE_BRICKS);
                h.scale(0.05f, 0.05f, 0.05f).glow(40, 30, 30).interpolation(25, 0);
                h.animateTo(new Vector3f(-0.7f, 0.1f, -0.25f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.4f, 5.0f, 0.5f), 25);
                shelves.add(h);

                // 3 shroomlight book spines per shelf at varying heights
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
                float leanAngle = leanProgress * 0.25f; // up to ~14 degrees
                for (int i = 0; i < shelves.size(); i++) {
                    if (i == collapsedIdx) continue;
                    double offsetX = (i - 3.5) * 1.6;
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
                BlockDisplayHandle s = shelves.get(idx);
                double offsetX = (idx - 3.5) * 1.6;
                // Pitch forward to horizontal
                s.animateTo(new Vector3f(-0.7f, 0.1f, -0.25f),
                        new AxisAngle4f((float) (Math.PI / 2), 1, 0, 0),
                        new Vector3f(1.4f, 5.0f, 0.5f), 14);
                Location origin = getCenter().clone().add(offsetX, 0.1, -5);

                // Smoke burst + sound
                w.spawnParticle(Particle.LARGE_SMOKE, origin.clone().add(0, 2, 0),
                        40, 0.7, 1.5, 0.7, 0.05);
                w.spawnParticle(Particle.BLOCK,
                        origin.clone().add(0, 2, 0), 30, 0.7, 1.5, 0.7, 0.1,
                        Material.POLISHED_BLACKSTONE_BRICKS.createBlockData());
                DisplayBuilder.playSound(origin, Sound.BLOCK_DEEPSLATE_BREAK, 1.3f, 0.5f);
                DisplayBuilder.playSound(origin, Sound.ENTITY_IRON_GOLEM_HURT, 0.9f, 0.6f);

                // Fling 5 bone-block "books" outward
                for (int b = 0; b < 5; b++) {
                    Location bookStart = origin.clone().add(0, 1 + Math.random() * 3, 0);
                    ItemDisplayHandle book = displayBuilder.spawnItem(
                            bookStart, new ItemStack(Material.BONE_BLOCK));
                    book.scale(0.3f, 0.5f, 0.4f).glow(220, 210, 180).interpolation(40, 0);
                    float fx = (float) ((Math.random() - 0.5) * 6);
                    float fy = (float) (1 + Math.random() * 2);
                    float fz = (float) (3 + Math.random() * 3);
                    book.animateTo(new Vector3f(fx - 0.15f, fy, fz - 0.2f),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 4), 1, 1, 0),
                            new Vector3f(0.3f, 0.5f, 0.4f), 40);
                    flyingBooks.add(book);

                    // Trigger impact damage at landing zone
                    Location land = bookStart.clone().add(fx, 0, fz);
                    triggerImpactDamage(land);
                }
                nextCollapseAt = tick + 100 + (int) (Math.random() * 80);
                collapsedIdx = -1; // allow re-leaning of others
            }

            // Ash rain from shelf tops
            if (tick % 3 == 0) {
                for (int i = 0; i < shelves.size(); i++) {
                    double offsetX = (i - 3.5) * 1.6;
                    Location top = getCenter().clone().add(offsetX, 5.2, -5);
                    w.spawnParticle(Particle.WHITE_ASH, top, 2, 0.6, 0.1, 0.4, 0.005);
                    w.spawnParticle(Particle.SMOKE, top, 1, 0.5, 0.1, 0.3, 0.005);
                }
            }
            // Bone step accents
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BONE_BLOCK_STEP, 0.6f, 0.7f);
            }
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.7f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ProfaneLibraryCollapse(plugin); }
    }

    // ================================================================
    // 34. SOUL RIVER
    //     A wide directional river of soul flame particles. 10 soul-sand
    //     "stones" placed in the riverbed as item displays. Particles flow
    //     in a current; where it hits the wall, a splash arcs upward.
    // ================================================================
    public static class SoulRiver extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> stones = new ArrayList<>();
        private double flowX = 1, flowZ = 0;

        public SoulRiver(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_river", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.9f, 0.4f);
            double a = Math.random() * Math.PI * 2;
            flowX = Math.cos(a);
            flowZ = Math.sin(a);
            // Perpendicular axis for river width
            double perpX = -flowZ;
            double perpZ = flowX;

            // 10 soul-sand stones along the river path with small offsets
            for (int i = 0; i < 10; i++) {
                double along = -8 + i * 1.8;
                double off = (Math.random() - 0.5) * 4.0; // curve
                Location p = center.clone().add(
                        flowX * along + perpX * off,
                        0.1,
                        flowZ * along + perpZ * off);
                ItemDisplayHandle stone = displayBuilder.spawnItem(p,
                        new ItemStack(Material.SOUL_SAND));
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

            // River body — soul flame current along the flow vector
            if (tick % 1 == 0) {
                for (int s = 0; s < 18; s++) {
                    double along = -10 + Math.random() * 20;
                    // Slight river curve via sin
                    double curve = Math.sin(along * 0.15) * 1.0;
                    double width = (Math.random() - 0.5) * 3.5;
                    Location pt = getCenter().clone().add(
                            flowX * along + perpX * (width + curve),
                            0.15 + Math.random() * 0.4,
                            flowZ * along + perpZ * (width + curve));
                    // Set particle velocity along flow
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, pt, 0,
                            flowX * 0.3, 0, flowZ * 0.3, 0.18);
                }
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
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, ep, 1, 0.04, 0.04, 0.04, 0.002);
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

            // Sound
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
        }

        @Override public AbstractAttack newInstance() { return new SoulRiver(plugin); }
    }

    // ================================================================
    // 35. INFERNAL FOG BANK
    //     Dense ground-level fog rolling across the arena. 8 soul-lantern
    //     reference points scattered through the fog. Random soul flame
    //     blinks suggesting a stalker. Warden/enderman ambient sound.
    // ================================================================
    public static class InfernalFogBank extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();
        private double rollProgress = 0;
        private double rollX = 1, rollZ = 0;

        public InfernalFogBank(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_fog_bank", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.5);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(28);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(380);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.2f, 0.3f);
            double a = Math.random() * Math.PI * 2;
            rollX = Math.cos(a);
            rollZ = Math.sin(a);

            // 8 soul lantern reference points scattered through the fog half
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

            // Roll progress — fog slowly migrates across the arena
            rollProgress += 0.04;
            double rollOff = Math.sin(rollProgress * 0.05) * 6.0;

            // Lantern subtle bob — flickering reference points
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

            // Dense fog particles — large_smoke + squid_ink at ground
            if (tick % 1 == 0) {
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

            // Warden ambient + enderman stare from random fog positions
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
        }

        @Override public AbstractAttack newInstance() { return new InfernalFogBank(plugin); }
    }

    // ================================================================
    // 36. THE WEEPING IDOL GARDEN
    //     6 humanoid idols (4 BlockDisplays each = 24 + 6 ceiling clusters
    //     = 30 total displays). Idols start facing outward, then all six
    //     simultaneously rotate to face the center. Eyes drip obsidian
    //     tear particles, soul fire at base.
    // ================================================================
    public static class WeepingIdolGarden extends EnvironmentalAttack {
        private final List<List<BlockDisplayHandle>> idols = new ArrayList<>();
        private final List<ItemDisplayHandle> ceilingDrips = new ArrayList<>();
        private final double[] idolAngles = new double[6];
        private final Location[] idolCenters = new Location[6];
        private int phase = 0;
        private static final int IDOL_COUNT = 6;

        public WeepingIdolGarden(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("weeping_idol_garden", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(40);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(400);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_WARN, 1.0f, 0.5f);
            double r = 6.0;
            for (int i = 0; i < IDOL_COUNT; i++) {
                double a = Math.PI * 2 * i / IDOL_COUNT;
                idolAngles[i] = a; // facing outward
                Location base = center.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                idolCenters[i] = base;
                List<BlockDisplayHandle> parts = new ArrayList<>();

                // Body (blackstone) — tall trunk
                BlockDisplayHandle body = displayBuilder.spawnBlock(base, Material.BLACKSTONE);
                body.scale(0.8f, 2.5f, 0.6f).glow(40, 30, 30).interpolation(20, 0);
                body.animateTo(new Vector3f(-0.4f, 0.1f, -0.3f),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(0.8f, 2.5f, 0.6f), 20);
                parts.add(body);

                // Head (bone block) — on top
                BlockDisplayHandle head = displayBuilder.spawnBlock(
                        base.clone().add(0, 2.6, 0), Material.BONE_BLOCK);
                head.scale(0.7f, 0.7f, 0.7f).glow(220, 210, 180).interpolation(20, 0);
                head.animateTo(new Vector3f(-0.35f, 2.6f, -0.35f),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(0.7f, 0.7f, 0.7f), 20);
                parts.add(head);

                // Arms — two thinner blackstone outcrops
                BlockDisplayHandle armL = displayBuilder.spawnBlock(
                        base.clone().add(0, 1.5, 0), Material.BLACKSTONE);
                armL.scale(0.3f, 1.2f, 0.3f).glow(40, 30, 30).interpolation(20, 0);
                armL.animateTo(new Vector3f(-0.55f, 1.5f, -0.15f),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(0.3f, 1.2f, 0.3f), 20);
                parts.add(armL);

                BlockDisplayHandle armR = displayBuilder.spawnBlock(
                        base.clone().add(0, 1.5, 0), Material.BLACKSTONE);
                armR.scale(0.3f, 1.2f, 0.3f).glow(40, 30, 30).interpolation(20, 0);
                armR.animateTo(new Vector3f(0.25f, 1.5f, -0.15f),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(0.3f, 1.2f, 0.3f), 20);
                parts.add(armR);

                idols.add(parts);
            }

            // Ceiling crying-obsidian clusters
            for (int i = 0; i < IDOL_COUNT; i++) {
                double a = Math.PI * 2 * i / IDOL_COUNT;
                Location ceil = center.clone().add(
                        Math.cos(a) * 3.5, 8.5, Math.sin(a) * 3.5);
                ItemDisplayHandle drip = displayBuilder.spawnItem(ceil,
                        new ItemStack(Material.CRYING_OBSIDIAN));
                drip.scale(0.9f, 0.9f, 0.9f).glow(80, 0, 160).interpolation(20, 0);
                ceilingDrips.add(drip);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Phase 0: idols outward (intro). Phase 1 (after tick 100): rotate all to face center simultaneously.
            // Phase 2 (after 240): hold + intensify. Phase 3: dissipate.
            if (phase == 0 && tick >= 100) {
                phase = 1;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.4f);
                // Reorient all idols to face center
                for (int i = 0; i < IDOL_COUNT; i++) {
                    double inward = Math.PI * 2 * i / IDOL_COUNT + Math.PI; // flipped
                    idolAngles[i] = inward;
                    List<BlockDisplayHandle> parts = idols.get(i);
                    // body
                    parts.get(0).animateTo(new Vector3f(-0.4f, 0.1f, -0.3f),
                            new AxisAngle4f((float) inward, 0, 1, 0),
                            new Vector3f(0.8f, 2.5f, 0.6f), 40);
                    // head
                    parts.get(1).animateTo(new Vector3f(-0.35f, 2.6f, -0.35f),
                            new AxisAngle4f((float) inward, 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 40);
                    // arms
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

            // Per-idol effects: tear streams from "eyes", soul fire base, ambient
            for (int i = 0; i < IDOL_COUNT; i++) {
                Location base = idolCenters[i];
                if (base == null) continue;
                double facing = idolAngles[i];

                // Eye position — front of head, slightly offset by facing
                Location eyeL = base.clone().add(
                        Math.cos(facing) * 0.4 - Math.sin(facing) * 0.18,
                        3.0,
                        Math.sin(facing) * 0.4 + Math.cos(facing) * 0.18);
                Location eyeR = base.clone().add(
                        Math.cos(facing) * 0.4 + Math.sin(facing) * 0.18,
                        3.0,
                        Math.sin(facing) * 0.4 - Math.cos(facing) * 0.18);

                // Synchronized dripping — every 4 ticks
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

                // Soul fire base
                if (tick % 3 == 0) {
                    for (int b = 0; b < 4; b++) {
                        double bAng = tick * 0.1 + b * (Math.PI / 2);
                        Location bp = base.clone().add(
                                Math.cos(bAng) * 0.6, 0.2, Math.sin(bAng) * 0.6);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, bp, 1, 0.05, 0.05, 0.05, 0.005);
                    }
                }
            }

            // Ceiling clusters drip downward
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
                    }
                    // Slow rotation hover
                    float s = 0.9f + (float) Math.sin(tick * 0.06 + i) * 0.08f;
                    ceilingDrips.get(i).animateTo(
                            new Vector3f(-s / 2f, 8.5f, -s / 2f),
                            new AxisAngle4f((float) (tick * 0.05), 0, 1, 0),
                            new Vector3f(s, s, s), 5);
                }
            }

            // Sound layers
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GHAST_WARN, 0.7f, 0.5f);
            }
            if (phase >= 1 && tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.4f);
            }
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 0.6f);
            }
        }

        @Override public AbstractAttack newInstance() { return new WeepingIdolGarden(plugin); }
    }

    // ================================================================
    // 37. THE HANGING FOREST
    //     Inverted forest: 5 trunk columns hanging from the ceiling (3
    //     plank blocks each = 15 trunks) + 12 dead-bush/warped-stem item
    //     hangings = 27 displays. Particles drift downward.
    // ================================================================
    public static class HangingForest extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> trunks = new ArrayList<>();
        private final List<ItemDisplayHandle> hangings = new ArrayList<>();
        private static final int TRUNK_COUNT = 5;
        private static final int CEILING_Y = 9;

        public HangingForest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hanging_forest", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(380);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.9f, 0.4f);
            // 5 inverted trunk columns at varying perimeter positions
            for (int t = 0; t < TRUNK_COUNT; t++) {
                double angle = Math.PI * 2 * t / TRUNK_COUNT
                        + (Math.random() - 0.5) * 0.4;
                double r = 4 + Math.random() * 4;
                double bx = Math.cos(angle) * r;
                double bz = Math.sin(angle) * r;
                Material trunkMat = (t % 2 == 0)
                        ? Material.WARPED_PLANKS : Material.CRIMSON_PLANKS;
                // 3 stacked block displays per trunk descending from ceiling
                for (int seg = 0; seg < 3; seg++) {
                    Location p = center.clone().add(bx,
                            CEILING_Y - seg * 1.6, bz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, trunkMat);
                    h.scale(0.05f, 0.05f, 0.05f)
                            .glow(80, 60, 70).interpolation(25, 0);
                    h.animateTo(new Vector3f(-0.4f, CEILING_Y - seg * 1.6f, -0.4f),
                            new AxisAngle4f((float) (Math.random() * 0.3), 1, 0, 1),
                            new Vector3f(0.8f, 1.6f, 0.8f), 25);
                    trunks.add(h);
                }
                // 2-3 dead-bush "branches" hanging beneath each trunk tip
                int hangCount = 2 + (int) (Math.random() * 2);
                for (int hb = 0; hb < hangCount; hb++) {
                    Material itemMat = (hb % 2 == 0)
                            ? Material.DEAD_BUSH : Material.WARPED_STEM;
                    Location hp = center.clone().add(
                            bx + (Math.random() - 0.5) * 1.0,
                            CEILING_Y - 5 - Math.random() * 1.5,
                            bz + (Math.random() - 0.5) * 1.0);
                    ItemDisplayHandle h = displayBuilder.spawnItem(hp,
                            new ItemStack(itemMat));
                    h.scale(0.7f, 1.0f, 0.7f).glow(120, 100, 80).interpolation(25, 0);
                    // Inverted (rotated 180° on X)
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

            // Subtle sway of hangings
            if (tick % 8 == 0) {
                for (int i = 0; i < hangings.size(); i++) {
                    float swayAng = (float) (Math.PI + Math.sin(tick * 0.04 + i) * 0.1);
                    hangings.get(i).animateTo(
                            new Vector3f(-0.35f, 0f, -0.35f),
                            new AxisAngle4f(swayAng, 1, 0, 0.2f),
                            new Vector3f(0.7f, 1.0f, 0.7f), 8);
                }
            }

            // Ash + warped spore drifting downward from canopy
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
                for (int i = 0; i < trunks.size(); i += 3) {
                    // every 3rd trunk = the topmost root segment
                    BlockDisplayHandle h = trunks.get(i);
                    if (h.entity() != null) {
                        Location rootLoc = h.entity().getLocation().clone().add(0.4, 1.5, 0.4);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                rootLoc, 2, 0.3, 0.1, 0.3, 0.005);
                        DisplayBuilder.dustParticles(rootLoc, 1, 0.2, 120, 200, 255, 1.2f);
                    }
                }
            }

            // Sounds
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
        }

        @Override public AbstractAttack newInstance() { return new HangingForest(plugin); }
    }

    // ================================================================
    // 38. BLOOD GEYSER FIELD
    //     5 vent points on staggered timers. When a vent erupts: tall
    //     lava+smoke column for 60 ticks, magma block at vent mouth
    //     glows during eruption, leaves an ash scorch after. Impact-only
    //     damage at each eruption tick.
    // ================================================================
    public static class BloodGeyserField extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> vents = new ArrayList<>();
        private final List<Location> ventLocs = new ArrayList<>();
        private final int[] nextEruption = new int[5];
        private final int[] eruptionEnd = new int[5];

        public BloodGeyserField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blood_geyser_field", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(5.0);
            config.setImpactRadius(2.5);
            config.setDamageDelayTicks(20);
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
                        new ItemStack(Material.MAGMA_BLOCK));
                vent.scale(0.95f, 0.4f, 0.95f).glow(120, 30, 30).interpolation(15, 0);
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

                // Pre-eruption rumble: 20 ticks before eruption
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
                    // Vent magma block glow + scale up
                    vents.get(i).animateTo(new Vector3f(-0.475f, 0.1f, -0.475f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.95f, 0.6f, 0.95f), 6);
                    // Initial impact damage at vent
                    triggerImpactDamage(v);
                }

                // Active eruption — lava column
                if (eruptionEnd[i] > 0 && tick < eruptionEnd[i]) {
                    if (tick % 1 == 0) {
                        for (int y = 0; y < 10; y++) {
                            Location col = v.clone().add(
                                    (Math.random() - 0.5) * 1.0,
                                    0.4 + y * 1.0,
                                    (Math.random() - 0.5) * 1.0);
                            w.spawnParticle(Particle.DRIPPING_LAVA, col, 1, 0.15, 0.15, 0.15, 0);
                            w.spawnParticle(Particle.FALLING_LAVA, col, 1, 0.15, 0.15, 0.15, 0);
                            if (y % 2 == 0) {
                                w.spawnParticle(Particle.SMOKE, col, 1,
                                        0.25, 0.15, 0.25, 0.02);
                            }
                            if (y == 0) {
                                w.spawnParticle(Particle.LAVA, col, 2, 0.4, 0.1, 0.4, 0.05);
                            }
                        }
                    }
                    // Re-trigger impact damage at vent every 20 ticks during eruption
                    if (tick % 20 == 0 && tick != nextEruption[i]) {
                        triggerImpactDamage(v);
                    }
                }

                // Eruption ends
                if (eruptionEnd[i] > 0 && tick == eruptionEnd[i]) {
                    DisplayBuilder.playSound(v, Sound.BLOCK_LAVA_EXTINGUISH, 0.9f, 0.6f);
                    // Ash scorch
                    w.spawnParticle(Particle.WHITE_ASH, v.clone().add(0, 0.2, 0),
                            30, 1.2, 0.05, 1.2, 0.005);
                    DisplayBuilder.dustParticles(v.clone().add(0, 0.15, 0),
                            8, 1.2, 60, 40, 40, 1.5f);
                    // Vent shrinks back
                    vents.get(i).animateTo(new Vector3f(-0.475f, 0.1f, -0.475f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.95f, 0.4f, 0.95f), 8);
                    eruptionEnd[i] = -1;
                    nextEruption[i] = tick + 70 + (int) (Math.random() * 50);
                }
            }

            // Ambient
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_AMBIENT, 0.5f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new BloodGeyserField(plugin); }
    }

    // ================================================================
    // 39. THE INFINITY MIRROR
    //     2 tall flat black-stained-glass panels facing each other ~8 blocks
    //     apart. Between them: a corridor of portal + end_rod particles.
    //     8 ender-eye item displays line the corridor walls. Periodic
    //     spark arcs between the panels.
    //     2 panels + 8 eyes = 10 displays.
    // ================================================================
    public static class InfinityMirror extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> panels = new ArrayList<>();
        private final List<ItemDisplayHandle> eyes = new ArrayList<>();
        private double axisX = 1, axisZ = 0;

        public InfinityMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infinity_mirror", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.4f, 0.4f);
            double a = Math.random() * Math.PI * 2;
            axisX = Math.cos(a);
            axisZ = Math.sin(a);
            // 2 panels at ±4 blocks along axis, perpendicular faces
            for (int side = 0; side < 2; side++) {
                int sign = (side == 0) ? 1 : -1;
                Location p = center.clone().add(axisX * 4 * sign, 0.1, axisZ * 4 * sign);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p,
                        Material.BLACK_STAINED_GLASS);
                float yaw = (float) (a + Math.PI / 2);
                h.scale(0.05f, 0.05f, 0.05f).glow(140, 0, 200).interpolation(20, 0);
                h.animateTo(new Vector3f(-2.5f, 0.1f, -0.15f),
                        new AxisAngle4f(yaw, 0, 1, 0),
                        new Vector3f(5.0f, 5.0f, 0.3f), 20);
                panels.add(h);
            }
            // 8 ender-eye item displays along corridor walls (4 per side)
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

            // Eye floating bob and rotation
            if (tick % 5 == 0) {
                for (int i = 0; i < eyes.size(); i++) {
                    float s = 0.7f + (float) Math.sin(tick * 0.1 + i) * 0.1f;
                    eyes.get(i).animateTo(
                            new Vector3f(-s / 2f, 0f, -s / 2f),
                            new AxisAngle4f((float) (tick * 0.15 + i), 0, 1, 0),
                            new Vector3f(s, s, s), 5);
                }
            }

            // Corridor particles — portal + end_rod filling the space between panels
            if (tick % 1 == 0) {
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

            // Sounds
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.4f);
            }
            if (tick % 55 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDERMAN_STARE, 0.6f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new InfinityMirror(plugin); }
    }

    // ================================================================
    // 40. THE SMOLDERING GIANT'S HAND
    //     Massive hand reaching from the wall: palm + 5 fingers (each finger
    //     3 segments) + wrist. Total = 1 palm + 15 finger blocks + 4 magma
    //     glowing knuckles + 6 hidden detail = 26 BlockDisplays. Lava and
    //     smoke seep from cracks; falling lava streams from the wrist.
    // ================================================================
    public static class SmolderingGiantsHand extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handParts = new ArrayList<>();
        private final List<ItemDisplayHandle> magmaKnuckles = new ArrayList<>();
        private double facingX = 1, facingZ = 0;
        private final List<Location> crackPoints = new ArrayList<>();

        public SmolderingGiantsHand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("smoldering_giants_hand", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(380);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_DEATH, 1.2f, 0.3f);

            double a = Math.random() * Math.PI * 2;
            facingX = Math.cos(a);
            facingZ = Math.sin(a);
            float yaw = (float) (a);
            // Wrist origin — at the wall
            Location wristOrigin = center.clone().add(facingX * -7, 0.1, facingZ * -7);
            // Palm — large basalt block ~3x2x3
            BlockDisplayHandle palm = displayBuilder.spawnBlock(
                    wristOrigin.clone().add(facingX * 3, 0.5, facingZ * 3), Material.BASALT);
            palm.scale(0.05f, 0.05f, 0.05f).glow(50, 30, 30).interpolation(30, 0);
            palm.animateTo(new Vector3f(-1.5f, 0.5f, -1.5f),
                    new AxisAngle4f(yaw, 0, 1, 0),
                    new Vector3f(3.0f, 1.5f, 3.0f), 30);
            handParts.add(palm);

            // Wrist — slightly thinner, blackstone, leading from wall to palm
            BlockDisplayHandle wrist = displayBuilder.spawnBlock(
                    wristOrigin.clone().add(facingX * 1.0, 0.5, facingZ * 1.0), Material.BLACKSTONE);
            wrist.scale(0.05f, 0.05f, 0.05f).glow(40, 25, 25).interpolation(30, 0);
            wrist.animateTo(new Vector3f(-1.0f, 0.5f, -1.0f),
                    new AxisAngle4f(yaw, 0, 1, 0),
                    new Vector3f(2.0f, 1.5f, 2.0f), 30);
            handParts.add(wrist);

            // Wrist scorch (extra detail block at base)
            BlockDisplayHandle wristGlow = displayBuilder.spawnBlock(
                    wristOrigin.clone().add(facingX * 0.4, 0.3, facingZ * 0.4),
                    Material.MAGMA_BLOCK);
            wristGlow.scale(0.05f, 0.05f, 0.05f).glow(255, 80, 0).interpolation(30, 0);
            wristGlow.animateTo(new Vector3f(-1.0f, 0.3f, -1.0f),
                    new AxisAngle4f(yaw, 0, 1, 0),
                    new Vector3f(2.0f, 1.0f, 2.0f), 30);
            handParts.add(wristGlow);

            // 5 fingers — each 3 segments. Spread across the palm front edge.
            double perpX = -facingZ;
            double perpZ = facingX;
            for (int f = 0; f < 5; f++) {
                double sideOffset = (f - 2) * 1.0;
                // Tapering segment lengths
                for (int seg = 0; seg < 3; seg++) {
                    double segDist = 4.5 + seg * 1.2;
                    double bendY = -seg * 0.15; // gentle downward curl
                    Location segLoc = wristOrigin.clone().add(
                            facingX * segDist + perpX * sideOffset,
                            0.5 + bendY,
                            facingZ * segDist + perpZ * sideOffset);
                    Material mat = (seg == 0) ? Material.BASALT : Material.BLACKSTONE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, mat);
                    float taper = 0.85f - seg * 0.1f;
                    h.scale(0.05f, 0.05f, 0.05f).glow(50, 30, 30).interpolation(30, 0);
                    h.animateTo(new Vector3f(-taper / 2f, 0.5f + (float) bendY, -taper / 2f),
                            new AxisAngle4f(yaw, 0, 1, 0),
                            new Vector3f(taper, 1.0f, taper), 30);
                    handParts.add(h);
                    crackPoints.add(segLoc);
                }
                // Magma knuckle item display between segment 0 and segment 1
                Location knuckleLoc = wristOrigin.clone().add(
                        facingX * 5.1 + perpX * sideOffset,
                        0.55,
                        facingZ * 5.1 + perpZ * sideOffset);
                ItemDisplayHandle knuckle = displayBuilder.spawnItem(knuckleLoc,
                        new ItemStack(Material.MAGMA_BLOCK));
                knuckle.scale(0.55f, 0.55f, 0.55f).glow(255, 80, 0).interpolation(30, 0);
                magmaKnuckles.add(knuckle);
            }
            crackPoints.add(wristOrigin.clone().add(facingX * 3, 0.5, facingZ * 3));
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double perpX = -facingZ;
            double perpZ = facingX;

            // Knuckle pulse glow
            if (tick % 6 == 0) {
                for (int i = 0; i < magmaKnuckles.size(); i++) {
                    float s = 0.55f + (float) Math.sin(tick * 0.12 + i) * 0.1f;
                    magmaKnuckles.get(i).animateTo(
                            new Vector3f(-s / 2f, 0f, -s / 2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s, s, s), 6);
                }
            }

            // Smoke + ash rising from every crack
            if (tick % 2 == 0) {
                for (Location cp : crackPoints) {
                    if (Math.random() < 0.5) {
                        Location pt = cp.clone().add(
                                (Math.random() - 0.5) * 1.0,
                                0.6 + Math.random() * 0.4,
                                (Math.random() - 0.5) * 1.0);
                        w.spawnParticle(Particle.LARGE_SMOKE, pt, 1, 0.15, 0.1, 0.15, 0.01);
                        w.spawnParticle(Particle.WHITE_ASH, pt, 1, 0.3, 0.1, 0.3, 0.005);
                    }
                }
            }

            // Lava seeping between finger segments
            if (tick % 3 == 0) {
                for (int f = 0; f < 5; f++) {
                    double sideOffset = (f - 2) * 1.0;
                    for (int seam = 0; seam < 2; seam++) {
                        double segDist = 5.1 + seam * 1.2;
                        Location seamLoc = getCenter().clone().add(
                                facingX * (segDist - 7) + perpX * sideOffset,
                                0.5,
                                facingZ * (segDist - 7) + perpZ * sideOffset);
                        w.spawnParticle(Particle.LAVA, seamLoc, 1, 0.15, 0.1, 0.15, 0.02);
                        w.spawnParticle(Particle.DRIPPING_LAVA, seamLoc, 1, 0.1, 0.05, 0.1, 0);
                    }
                }
            }

            // Falling-lava streams from the wrist
            if (tick % 4 == 0) {
                Location wrist = getCenter().clone().add(
                        facingX * -6, 1.0, facingZ * -6);
                for (int s = 0; s < 4; s++) {
                    Location pt = wrist.clone().add(
                            (Math.random() - 0.5) * 2.0,
                            -Math.random() * 1.0,
                            (Math.random() - 0.5) * 2.0);
                    w.spawnParticle(Particle.FALLING_LAVA, pt, 1, 0.1, 0.05, 0.1, 0);
                    w.spawnParticle(Particle.DRIPPING_LAVA, pt, 1, 0.1, 0.1, 0.1, 0);
                }
                DisplayBuilder.dustParticles(wrist.clone().add(0, -0.3, 0),
                        3, 1.0, 200, 60, 30, 1.4f);
            }

            // Sounds
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BASALT_STEP, 1.0f, 0.3f);
            }
            if (tick % 65 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_IRON_GOLEM_DEATH, 0.7f, 0.3f);
            }
            if (tick % 50 == 0) {
                Location wrist = getCenter().clone().add(
                        facingX * -6, 0.5, facingZ * -6);
                DisplayBuilder.playSound(wrist, Sound.BLOCK_LAVA_AMBIENT, 0.8f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new SmolderingGiantsHand(plugin); }
    }
}
