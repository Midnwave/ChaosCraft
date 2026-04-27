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
 * Devil's Dream Mode — BLOCK DISPLAY ATTACKS, batch 5 (entries 41–50).
 * Each attack assembles 25+ BlockDisplay/ItemDisplay entities into a
 * recognizable structure with multi-phase animation (spawn → active →
 * dissipate) using interpolated transforms.
 *
 * Entries:
 *   41. Charnel Spiral
 *   42. Weeping Lattice
 *   43. Hellhound Silhouette
 *   44. Crumbling Obelisk
 *   45. The Cursed Clock Hands
 *   46. The Dreamer's Ribcage Heart
 *   47. Petrified Forest
 *   48. The Living Portrait
 *   49. Falling Angel
 *   50. The Damnation Sundial
 */
public final class DDBlockDisplay5 {
    private DDBlockDisplay5() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CharnelSpiral(plugin));
        registry.register(new WeepingLattice(plugin));
        registry.register(new HellhoundSilhouette(plugin));
        registry.register(new CrumblingObelisk(plugin));
        registry.register(new CursedClockHands(plugin));
        registry.register(new DreamersRibcageHeart(plugin));
        registry.register(new PetrifiedForest(plugin));
        registry.register(new LivingPortrait(plugin));
        registry.register(new FallingAngel(plugin));
        registry.register(new DamnationSundial(plugin));
    }

    // ================================================================
    // 41. CHARNEL SPIRAL
    //     Massive vertical 3D helix — bone block primary coil + opposite-
    //     direction blackstone coil + calcite tip highlights. Slowly spins,
    //     translates Y, periodically compresses and snaps.
    // ================================================================
    public static class CharnelSpiral extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bonecoil = new ArrayList<>();
        private final List<BlockDisplayHandle> blackcoil = new ArrayList<>();
        private final List<BlockDisplayHandle> tips = new ArrayList<>();
        private int phase = 0;

        public CharnelSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("charnel_spiral", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_BONE_BLOCK_PLACE, 1.4f, 0.4f);
            int turns = 5;
            int perTurn = 3;
            int steps = turns * perTurn; // 15 helix steps
            double height = 20.0;
            double r = 2.6;
            for (int i = 0; i < steps; i++) {
                double t = (double) i / (steps - 1);
                double angle = t * turns * Math.PI * 2;
                double y = t * height;
                Location p = center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BONE_BLOCK);
                h.scale(0.85f, 0.85f, 0.85f).glow(240, 230, 200).interpolation(10, 0).viewRange(2.5f);
                bonecoil.add(h);

                // opposite blackstone coil
                double angle2 = -angle;
                Location p2 = center.clone().add(Math.cos(angle2) * r, y, Math.sin(angle2) * r);
                BlockDisplayHandle h2 = displayBuilder.spawnBlock(p2, Material.BLACKSTONE);
                h2.scale(0.7f, 0.7f, 0.7f).interpolation(10, 0).viewRange(2.5f);
                blackcoil.add(h2);
            }
            // Calcite outer tips at every other step
            for (int i = 0; i < steps; i += 2) {
                double t = (double) i / (steps - 1);
                double angle = t * turns * Math.PI * 2;
                double y = t * height;
                double rt = r + 0.9;
                Location pt = center.clone().add(Math.cos(angle) * rt, y, Math.sin(angle) * rt);
                BlockDisplayHandle ht = displayBuilder.spawnBlock(pt, Material.CALCITE);
                ht.scale(0.45f, 0.45f, 0.45f).glow(240, 240, 240).interpolation(10, 0);
                tips.add(ht);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            if (tick < dur * 0.15) phase = 0;        // spawn settle
            else if (tick < dur * 0.85) phase = 1;   // active spin
            else phase = 2;                          // dissipate

            int turns = 5;
            int steps = bonecoil.size();
            double height = 20.0;
            double r = 2.6;
            double yOffset = Math.sin(tick * 0.04) * 1.5;
            double globalSpin = tick * 0.05;
            boolean compressNow = (tick > 0 && tick % 80 == 0 && phase == 1);
            float ySquish = compressNow ? 0.5f : 1.0f;

            if (tick % 4 == 0 && phase != 2) {
                for (int i = 0; i < steps; i++) {
                    double t = (double) i / (steps - 1);
                    double angle = t * turns * Math.PI * 2 + globalSpin;
                    double y = t * height * ySquish + yOffset;
                    float tx = (float) (Math.cos(angle) * r);
                    float tz = (float) (Math.sin(angle) * r);
                    bonecoil.get(i).animateTo(
                            new Vector3f(tx - 0.425f, (float) y, tz - 0.425f),
                            new AxisAngle4f((float) (globalSpin * 2), 0, 1, 0),
                            new Vector3f(0.85f, 0.85f, 0.85f), 4);

                    double angle2 = -t * turns * Math.PI * 2 - globalSpin;
                    float tx2 = (float) (Math.cos(angle2) * r);
                    float tz2 = (float) (Math.sin(angle2) * r);
                    blackcoil.get(i).animateTo(
                            new Vector3f(tx2 - 0.35f, (float) y, tz2 - 0.35f),
                            new AxisAngle4f((float) (-globalSpin * 2), 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 4);
                }
                for (int i = 0; i < tips.size(); i++) {
                    int srcIdx = i * 2;
                    double t = (double) srcIdx / (steps - 1);
                    double angle = t * turns * Math.PI * 2 + globalSpin;
                    double y = t * height * ySquish + yOffset;
                    double rt = r + 0.9;
                    float tx = (float) (Math.cos(angle) * rt);
                    float tz = (float) (Math.sin(angle) * rt);
                    tips.get(i).animateTo(new Vector3f(tx - 0.225f, (float) y, tz - 0.225f),
                            new AxisAngle4f((float) (globalSpin * 3), 0, 1, 0),
                            new Vector3f(0.45f, 0.45f, 0.45f), 4);
                }
            }

            if (compressNow) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 0.5f);
                w.spawnParticle(Particle.EXPLOSION, getCenter().clone().add(0, 8, 0), 1, 0, 0, 0, 0);
            }

            if (tick % 6 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double y = Math.random() * height;
                    Location loc = getCenter().clone().add(Math.cos(angle) * (r + 1.2),
                            y, Math.sin(angle) * (r + 1.2));
                    DisplayBuilder.dustParticles(loc, 2, 0.4, 240, 230, 200, 1.4f);
                }
            }
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BONE_BLOCK_STEP, 0.7f, 0.5f);
            }

            // dissipate phase: shrink bonecoil/blackcoil/tips
            if (phase == 2 && tick % 6 == 0) {
                float s = Math.max(0.05f, 1.0f - (tick - dur * 0.85f) / (dur * 0.15f));
                for (BlockDisplayHandle h : bonecoil) h.animateTo(
                        new Vector3f(0, 8, 0), new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(s * 0.85f, s * 0.85f, s * 0.85f), 6);
                for (BlockDisplayHandle h : blackcoil) h.animateTo(
                        new Vector3f(0, 8, 0), new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(s * 0.7f, s * 0.7f, s * 0.7f), 6);
                for (BlockDisplayHandle h : tips) h.animateTo(
                        new Vector3f(0, 8, 0), new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(s * 0.45f, s * 0.45f, s * 0.45f), 6);
            }
        }

        @Override public AbstractAttack newInstance() { return new CharnelSpiral(plugin); }
    }

    // ================================================================
    // 42. WEEPING LATTICE
    //     6x6x6 wireframe cube — 8 corner shroomlight nodes, 12 crying-
    //     obsidian edge beams, internal cross-bracing. Tumbles, then
    //     periodically collapses inward to dense cube and re-expands.
    // ================================================================
    public static class WeepingLattice extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> corners = new ArrayList<>();
        private final List<BlockDisplayHandle> edges = new ArrayList<>();
        private final List<BlockDisplayHandle> braces = new ArrayList<>();
        private int phase = 0;

        public WeepingLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("weeping_lattice", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(7.5);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(340);
            config.setCooldownTicks(330);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(27.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.4f);
            double s = 3.0; // half-edge
            // 8 corner shroomlight clusters (each 1 display)
            int[][] signs = {
                    {-1,-1,-1},{ 1,-1,-1},{-1, 1,-1},{ 1, 1,-1},
                    {-1,-1, 1},{ 1,-1, 1},{-1, 1, 1},{ 1, 1, 1}
            };
            for (int[] sg : signs) {
                Location p = center.clone().add(sg[0]*s, sg[1]*s + 5, sg[2]*s);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SHROOMLIGHT);
                h.scale(0.7f,0.7f,0.7f).glow(255, 220, 90).interpolation(10, 0);
                corners.add(h);
            }
            // 12 edges — each edge spawned as 2 crying obsidian segments along it
            int[][] edgePairs = {
                    {0,1},{2,3},{4,5},{6,7},
                    {0,2},{1,3},{4,6},{5,7},
                    {0,4},{1,5},{2,6},{3,7}
            };
            for (int[] e : edgePairs) {
                int[] a = signs[e[0]];
                int[] b = signs[e[1]];
                for (int k = 1; k <= 2; k++) {
                    double f = k / 3.0;
                    Location p = center.clone().add(
                            (a[0]+(b[0]-a[0])*f)*s,
                            (a[1]+(b[1]-a[1])*f)*s + 5,
                            (a[2]+(b[2]-a[2])*f)*s);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                    h.scale(0.55f,0.55f,0.55f).glow(90, 0, 130).interpolation(10, 0);
                    edges.add(h);
                }
            }
            // 8 internal bracing segments (blackstone)
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = center.clone().add(Math.cos(a)*1.5, 5, Math.sin(a)*1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.4f,0.4f,0.4f).interpolation(10, 0);
                braces.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            if (tick < dur * 0.1) phase = 0;
            else if (tick < dur * 0.9) phase = 1;
            else phase = 2;

            // Tumble rotation on all 3 axes
            float rx = (float) (tick * 0.05);
            float ry = (float) (tick * 0.07);

            boolean collapseCycle = (tick % 90 < 25 && phase == 1);
            float scaleBias = collapseCycle ? 0.18f : 1.0f;

            if (tick % 4 == 0) {
                double s = 3.0 * scaleBias;
                int[][] signs = {
                        {-1,-1,-1},{ 1,-1,-1},{-1, 1,-1},{ 1, 1,-1},
                        {-1,-1, 1},{ 1,-1, 1},{-1, 1, 1},{ 1, 1, 1}
                };
                for (int i = 0; i < corners.size(); i++) {
                    int[] sg = signs[i];
                    // apply tumble: rotate corner around y then x
                    double cx = sg[0]*s, cy = sg[1]*s, cz = sg[2]*s;
                    double ny = cy * Math.cos(rx) - cz * Math.sin(rx);
                    double nz = cy * Math.sin(rx) + cz * Math.cos(rx);
                    double nx = cx * Math.cos(ry) + nz * Math.sin(ry);
                    double fnz = -cx * Math.sin(ry) + nz * Math.cos(ry);
                    corners.get(i).animateTo(
                            new Vector3f((float)(nx)-0.35f, (float)(ny)+5f, (float)(fnz)-0.35f),
                            new AxisAngle4f(rx, 1, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 4);
                }

                int[][] edgePairs = {
                        {0,1},{2,3},{4,5},{6,7},
                        {0,2},{1,3},{4,6},{5,7},
                        {0,4},{1,5},{2,6},{3,7}
                };
                int idx = 0;
                for (int[] e : edgePairs) {
                    int[] a = signs[e[0]];
                    int[] b = signs[e[1]];
                    for (int k = 1; k <= 2; k++) {
                        double f = k / 3.0;
                        double cx = (a[0]+(b[0]-a[0])*f)*s;
                        double cy = (a[1]+(b[1]-a[1])*f)*s;
                        double cz = (a[2]+(b[2]-a[2])*f)*s;
                        double ny = cy * Math.cos(rx) - cz * Math.sin(rx);
                        double nz = cy * Math.sin(rx) + cz * Math.cos(rx);
                        double nx = cx * Math.cos(ry) + nz * Math.sin(ry);
                        double fnz = -cx * Math.sin(ry) + nz * Math.cos(ry);
                        edges.get(idx++).animateTo(
                                new Vector3f((float)(nx)-0.275f, (float)(ny)+5f, (float)(fnz)-0.275f),
                                new AxisAngle4f(rx + ry, 1, 1, 1),
                                new Vector3f(0.55f, 0.55f, 0.55f), 4);
                    }
                }

                for (int i = 0; i < braces.size(); i++) {
                    double a = Math.PI * 2 * i / 8 + tick * 0.1;
                    double bx = Math.cos(a) * 1.5 * scaleBias;
                    double bz = Math.sin(a) * 1.5 * scaleBias;
                    braces.get(i).animateTo(
                            new Vector3f((float)bx-0.2f, 5f, (float)bz-0.2f),
                            new AxisAngle4f((float)(tick*0.15), 0, 1, 0),
                            new Vector3f(0.4f, 0.4f, 0.4f), 4);
                }
            }

            // Constant tear drip particles
            if (tick % 3 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (Math.random()-0.5)*6;
                    double oy = 5 + (Math.random()-0.5)*6;
                    double oz = (Math.random()-0.5)*6;
                    Location loc = getCenter().clone().add(ox, oy, oz);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, loc, 2, 0.2, 0.2, 0.2, 0);
                    if (i % 3 == 0) DisplayBuilder.dustParticles(loc, 1, 0.2, 90, 0, 130, 1.4f);
                }
            }

            if (tick % 90 == 0 && phase == 1) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.3f, 0.6f);
                triggerImpactDamage(getCenter().clone().add(0, 5, 0));
            }
            if (tick % 18 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_GLASS_HIT, 0.6f, 0.4f);
            }

            if (phase == 2 && tick % 6 == 0) {
                float s = Math.max(0.05f, 1.0f - (tick - dur * 0.9f) / (dur * 0.1f));
                for (BlockDisplayHandle h : corners) h.animateTo(new Vector3f(0,5,0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.7f*s,0.7f*s,0.7f*s), 6);
                for (BlockDisplayHandle h : edges) h.animateTo(new Vector3f(0,5,0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.55f*s,0.55f*s,0.55f*s), 6);
                for (BlockDisplayHandle h : braces) h.animateTo(new Vector3f(0,5,0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.4f*s,0.4f*s,0.4f*s), 6);
            }
        }

        @Override public AbstractAttack newInstance() { return new WeepingLattice(plugin); }
    }

    // ================================================================
    // 43. HELLHOUND SILHOUETTE
    //     Recognizable 4-legged hound: body, 4 leg pairs (2 segs each),
    //     neck, head with shroomlight eyes, tail. Prowls, lunges, snaps.
    // ================================================================
    public static class HellhoundSilhouette extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>();   // 8 segments
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final List<BlockDisplayHandle> tail = new ArrayList<>();
        private int phase = 0;

        public HellhoundSilhouette(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellhound_silhouette", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(10.5);
            config.setDamageRadius(6.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(3);
            config.setDurationTicks(320);
            config.setCooldownTicks(310);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_AMBIENT, 1.3f, 0.6f);
            // body: 5 long stretching displays at Y=2, 3 across, total ~10 displays
            for (int i = 0; i < 10; i++) {
                double bx = -2.5 + (i % 5);
                double bz = (i < 5) ? -0.5 : 0.5;
                Location p = center.clone().add(bx, 2.0, bz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.95f,0.85f,0.85f).interpolation(10,0);
                body.add(h);
            }
            // legs: 4 legs * 2 segments = 8
            double[][] legBase = {
                    {-2.0, -0.7}, {-2.0, 0.7},
                    { 2.0, -0.7}, { 2.0, 0.7}
            };
            for (double[] lb : legBase) {
                for (int seg = 0; seg < 2; seg++) {
                    Location p = center.clone().add(lb[0], 0.5 + seg, lb[1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p,
                            seg == 0 ? Material.OBSIDIAN : Material.DEEPSLATE_TILES);
                    h.scale(0.55f, 0.95f, 0.55f).interpolation(10, 0);
                    legs.add(h);
                }
            }
            // neck — 1 block
            Location neck = center.clone().add(2.6, 2.6, 0);
            BlockDisplayHandle nk = displayBuilder.spawnBlock(neck, Material.BLACKSTONE);
            nk.scale(0.7f, 0.95f, 0.85f).interpolation(10, 0);
            body.add(nk);
            // head: 4-block skull cluster
            for (int i = 0; i < 4; i++) {
                double ox = 3.0 + (i % 2) * 0.6;
                double oy = 3.2 + (i / 2) * 0.5;
                Location p = center.clone().add(ox, oy, (i % 2 == 0) ? -0.4 : 0.4);
                Material mat = (i == 3) ? Material.POLISHED_BLACKSTONE : Material.BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                h.scale(0.7f,0.7f,0.7f).interpolation(10,0);
                head.add(h);
            }
            // 2 shroomlight eyes
            for (int i = 0; i < 2; i++) {
                Location e = center.clone().add(3.7, 3.6, (i == 0) ? -0.45 : 0.45);
                BlockDisplayHandle eh = displayBuilder.spawnBlock(e, Material.SHROOMLIGHT);
                eh.scale(0.3f,0.3f,0.3f).glow(255, 60, 0).interpolation(10, 0);
                eyes.add(eh);
            }
            // tail: 3 segments backwards & up
            for (int i = 0; i < 3; i++) {
                double bx = -3.0 - i * 0.5;
                double by = 2.0 + i * 0.3;
                Location p = center.clone().add(bx, by, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.45f - i*0.08f, 0.45f - i*0.08f, 0.45f - i*0.08f).interpolation(10, 0);
                tail.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            if (tick < dur * 0.12) phase = 0;
            else if (tick < dur * 0.88) phase = 1;
            else phase = 2;

            float rock = (float) Math.sin(tick * 0.18) * 0.05f;
            boolean lunge = (tick % 80 > 60 && tick % 80 < 70 && phase == 1);
            boolean snap = (tick % 60 > 50 && tick % 60 < 56 && phase == 1);
            float lungeOff = lunge ? 1.6f : 0f;
            float snapOff = snap ? 0.8f : 0f;

            if (tick % 4 == 0) {
                // body
                for (int i = 0; i < body.size() - 1; i++) {
                    double bx = -2.5 + (i % 5);
                    double bz = (i < 5) ? -0.5 : 0.5;
                    body.get(i).animateTo(new Vector3f((float)bx + lungeOff - 0.475f, 2.0f + rock, (float)bz - 0.425f),
                            new AxisAngle4f(rock, 0, 0, 1),
                            new Vector3f(0.95f, 0.85f, 0.85f), 4);
                }
                // neck (last in body)
                body.get(body.size()-1).animateTo(new Vector3f(2.6f + lungeOff - 0.35f, 2.6f + rock, -0.425f),
                        new AxisAngle4f(rock, 0, 0, 1), new Vector3f(0.7f, 0.95f, 0.85f), 4);
                // legs alternate prowl
                double[][] legBase = {
                        {-2.0, -0.7}, {-2.0, 0.7},
                        { 2.0, -0.7}, { 2.0, 0.7}
                };
                for (int li = 0; li < 4; li++) {
                    boolean odd = (li == 0 || li == 3);
                    double offY = (Math.sin(tick * 0.25 + (odd ? 0 : Math.PI)) * 0.3);
                    for (int seg = 0; seg < 2; seg++) {
                        legs.get(li * 2 + seg).animateTo(
                                new Vector3f((float)legBase[li][0] + lungeOff - 0.275f,
                                        0.5f + seg + (float)offY,
                                        (float)legBase[li][1] - 0.275f),
                                new AxisAngle4f((float)(offY * 1.5), 0, 0, 1),
                                new Vector3f(0.55f, 0.95f, 0.55f), 4);
                    }
                }
                // head
                for (int i = 0; i < head.size(); i++) {
                    double ox = 3.0 + (i % 2) * 0.6;
                    double oy = 3.2 + (i / 2) * 0.5;
                    double oz = (i % 2 == 0) ? -0.4 : 0.4;
                    head.get(i).animateTo(
                            new Vector3f((float)ox + lungeOff + snapOff - 0.35f, (float)oy + rock, (float)oz - 0.35f),
                            new AxisAngle4f(rock, 0, 0, 1),
                            new Vector3f(0.7f, 0.7f, 0.7f), 4);
                }
                for (int i = 0; i < eyes.size(); i++) {
                    eyes.get(i).animateTo(new Vector3f(3.7f + lungeOff + snapOff - 0.15f, 3.6f + rock,
                                    ((i == 0) ? -0.45f : 0.45f) - 0.15f),
                            new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.3f, 0.3f, 0.3f), 4);
                }
                for (int i = 0; i < tail.size(); i++) {
                    double tailWag = Math.sin(tick * 0.22 + i * 0.5) * 0.4;
                    double bx = -3.0 - i * 0.5;
                    double by = 2.0 + i * 0.3;
                    tail.get(i).animateTo(new Vector3f((float)bx + lungeOff - 0.225f + 0.04f * i,
                                    (float)by, (float)tailWag - 0.225f),
                            new AxisAngle4f((float)tailWag, 0, 1, 0),
                            new Vector3f(0.45f - i*0.08f, 0.45f - i*0.08f, 0.45f - i*0.08f), 4);
                }
            }

            // ember eyes & smoke breath
            if (tick % 4 == 0) {
                for (BlockDisplayHandle e : eyes) {
                    Location el = getCenter().clone().add(3.7 + lungeOff + snapOff, 3.6, 0);
                    DisplayBuilder.dustParticles(el, 2, 0.3, 255, 60, 0, 1.5f);
                }
                Location mouth = getCenter().clone().add(3.9 + lungeOff + snapOff, 3.3, 0);
                w.spawnParticle(Particle.SMOKE, mouth, 4, 0.2, 0.1, 0.2, 0.02);
            }
            if (tick % 18 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WOLF_GROWL, 0.7f, 0.4f);
            }
            if (lunge && tick % 80 == 65) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_RAVAGER_ATTACK, 1.4f, 0.8f);
                w.spawnParticle(Particle.LARGE_SMOKE, getCenter().clone().add(4, 2, 0), 12, 0.5, 0.5, 0.5, 0.05);
            }

            if (phase == 2 && tick % 6 == 0) {
                float s = Math.max(0.05f, 1.0f - (tick - dur * 0.88f) / (dur * 0.12f));
                for (BlockDisplayHandle h : body) h.animateTo(new Vector3f(0,2,0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.95f*s, 0.85f*s, 0.85f*s), 6);
                for (BlockDisplayHandle h : legs) h.animateTo(new Vector3f(0,1,0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.55f*s,0.95f*s,0.55f*s), 6);
                for (BlockDisplayHandle h : head) h.animateTo(new Vector3f(3,3,0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.7f*s,0.7f*s,0.7f*s), 6);
                for (BlockDisplayHandle h : eyes) h.animateTo(new Vector3f(3.7f,3.6f,0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.3f*s,0.3f*s,0.3f*s), 6);
                for (BlockDisplayHandle h : tail) h.animateTo(new Vector3f(-3,2,0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.45f*s,0.45f*s,0.45f*s), 6);
            }
        }

        @Override public AbstractAttack newInstance() { return new HellhoundSilhouette(plugin); }
    }

    // ================================================================
    // 44. CRUMBLING OBELIX
    //     Tall 4-sided tapering obelisk (2x2 base → 1x1 top, ~10 high) with
    //     pyramid cap, crying-obsidian crack veins, tuff rubble at base.
    //     Top-down sequential detonation (impact-only damage).
    // ================================================================
    public static class CrumblingObelisk extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shaft = new ArrayList<>();
        private final List<BlockDisplayHandle> cap = new ArrayList<>();
        private final List<BlockDisplayHandle> cracks = new ArrayList<>();
        private final List<BlockDisplayHandle> rubble = new ArrayList<>();
        private int phase = 0;

        public CrumblingObelisk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crumbling_obelisk", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(27.0);
            config.setImpactRadius(7.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(330);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
            // shaft: 5 levels, lower 2 are 2x2 (4 each), upper 2 are 1x1 thinning, top 1 is shaft
            for (int level = 0; level < 5; level++) {
                double y = 0.5 + level * 2.0;
                if (level < 2) {
                    for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) {
                        Location p = center.clone().add(dx - 0.5, y, dz - 0.5);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                        h.scale(1.0f, 1.95f, 1.0f).interpolation(10, 0);
                        shaft.add(h);
                    }
                } else {
                    for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) {
                        double scale = 1.0 - (level - 1) * 0.18;
                        Location p = center.clone().add((dx - 0.5) * scale, y, (dz - 0.5) * scale);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                        h.scale((float)scale, 1.95f, (float)scale).interpolation(10, 0);
                        shaft.add(h);
                    }
                }
            }
            // pyramid cap (4 deepslate tile pieces tilted inward)
            for (int i = 0; i < 4; i++) {
                double ang = Math.PI * 2 * i / 4 + Math.PI/4;
                Location p = center.clone().add(Math.cos(ang)*0.3, 11, Math.sin(ang)*0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.DEEPSLATE_TILES);
                h.scale(0.6f, 0.6f, 0.6f).interpolation(10, 0);
                cap.add(h);
            }
            // crack veins: 6 crying-obsidian flecks scaled thin along faces
            for (int i = 0; i < 6; i++) {
                double y = 1.5 + i * 1.5;
                double ang = Math.PI * 2 * i / 6;
                Location p = center.clone().add(Math.cos(ang)*0.6, y, Math.sin(ang)*0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(0.15f, 0.4f, 0.15f).glow(90, 0, 130).interpolation(0, 0);
                cracks.add(h);
            }
            // tuff rubble (5 chunks at base)
            for (int i = 0; i < 5; i++) {
                double ang = Math.PI * 2 * i / 5;
                Location p = center.clone().add(Math.cos(ang)*1.4, 0.2, Math.sin(ang)*1.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.TUFF);
                h.scale(0.4f,0.4f,0.4f).interpolation(10, 0);
                rubble.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            if (tick < dur * 0.45) phase = 0; // intact, fracture spreading
            else if (tick < dur * 0.85) phase = 1; // sequential explosion
            else phase = 2;

            // fracture: cracks scale up over time
            if (tick % 8 == 0 && phase == 0) {
                float crack = Math.min(1.0f, (float) tick / (dur * 0.45f));
                for (int i = 0; i < cracks.size(); i++) {
                    cracks.get(i).animateTo(new Vector3f(0,0,0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.15f + crack*0.4f, 0.4f + crack*0.6f, 0.15f + crack*0.4f), 8);
                }
                if (tick % 16 == 0) {
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.4f);
                    w.spawnParticle(Particle.BLOCK, getCenter().clone().add(0, 5, 0),
                            8, 1, 3, 1, 0, Material.POLISHED_BLACKSTONE_BRICKS.createBlockData());
                }
                // unsettling slow rotation
                float rot = (float) (tick * 0.02);
                if (tick % 8 == 0) {
                    for (int i = 0; i < shaft.size(); i++) {
                        int level = i / 4;
                        int sub = i % 4;
                        int dx = sub / 2;
                        int dz = sub % 2;
                        double y = 0.5 + level * 2.0;
                        double scale = (level < 2) ? 1.0 : 1.0 - (level - 1) * 0.18;
                        // rotate the level around y
                        double cx = (dx - 0.5) * scale;
                        double cz = (dz - 0.5) * scale;
                        double nx = cx * Math.cos(rot) - cz * Math.sin(rot);
                        double nz = cx * Math.sin(rot) + cz * Math.cos(rot);
                        shaft.get(i).animateTo(
                                new Vector3f((float)nx - (float)scale/2f, (float)y, (float)nz - (float)scale/2f),
                                new AxisAngle4f(rot, 0, 1, 0),
                                new Vector3f((float)scale, 1.95f, (float)scale), 8);
                    }
                }
            }

            // sequential detonation: cap first, then shaft top→bottom, every 12 ticks
            if (phase == 1) {
                int explodeTick = tick - (int)(dur * 0.45);
                // cap detonates at explodeTick==0
                if (explodeTick == 0) {
                    DisplayBuilder.playSound(getCenter().clone().add(0, 11, 0), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, getCenter().clone().add(0, 11, 0), 1, 0,0,0,0);
                    for (BlockDisplayHandle h : cap) {
                        double ang = Math.random() * Math.PI * 2;
                        h.animateTo(new Vector3f((float)Math.cos(ang)*4f, 13f, (float)Math.sin(ang)*4f),
                                new AxisAngle4f((float)(Math.random()*Math.PI*4), 1, 1, 0),
                                new Vector3f(0.6f,0.6f,0.6f), 18);
                    }
                    triggerImpactDamage(getCenter().clone().add(0, 8, 0));
                }
                // detonate level (4-i) at explodeTick == (i+1)*16
                int currentLevel = (explodeTick / 16) - 1;
                if (currentLevel >= 0 && currentLevel < 5 && explodeTick % 16 == 0) {
                    int level = 4 - currentLevel;
                    DisplayBuilder.playSound(getCenter().clone().add(0, 0.5 + level*2, 0),
                            Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.5f);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, getCenter().clone().add(0, 0.5 + level*2, 0),
                            1, 0,0,0,0);
                    for (int sub = 0; sub < 4; sub++) {
                        int idx = level * 4 + sub;
                        if (idx < shaft.size()) {
                            double ang = Math.random()*Math.PI*2;
                            shaft.get(idx).animateTo(new Vector3f(
                                            (float)Math.cos(ang)*5f, level*2f + 1f, (float)Math.sin(ang)*5f),
                                    new AxisAngle4f((float)(Math.random()*Math.PI*4), 1, 1, 1),
                                    new Vector3f(1.0f, 0.5f, 1.0f), 18);
                        }
                    }
                    triggerImpactDamage(getCenter().clone().add(0, level*2 + 1, 0));
                }
                // rubble shake
                if (tick % 5 == 0) {
                    for (int i = 0; i < rubble.size(); i++) {
                        double ang = Math.PI*2*i/5;
                        rubble.get(i).animateTo(new Vector3f(
                                        (float)(Math.cos(ang)*1.4) - 0.2f + (float)(Math.random()*0.3),
                                        0.2f, (float)(Math.sin(ang)*1.4) - 0.2f + (float)(Math.random()*0.3)),
                                new AxisAngle4f((float)(Math.random()*Math.PI), 0, 1, 0),
                                new Vector3f(0.4f, 0.4f, 0.4f), 5);
                    }
                }
            }

            if (tick % 4 == 0) {
                w.spawnParticle(Particle.BLOCK, getCenter().clone().add(0, 4, 0),
                        4, 1.5, 3, 1.5, 0, Material.POLISHED_BLACKSTONE_BRICKS.createBlockData());
            }
        }

        @Override public AbstractAttack newInstance() { return new CrumblingObelisk(plugin); }
    }

    // ================================================================
    // 45. CURSED CLOCK HANDS
    //     Two flat hands rotating at clock-relative speeds around a
    //     shroomlight pivot, hovering at head height. Periodic 12-snap +
    //     reverse spin. Continuous sweep damage.
    // ================================================================
    public static class CursedClockHands extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> minute = new ArrayList<>();
        private final List<BlockDisplayHandle> hour = new ArrayList<>();
        private final List<BlockDisplayHandle> pivot = new ArrayList<>();
        private double minAngle = 0, hourAngle = 0;
        private boolean reverse = false;
        private int snapHoldTicks = 0;
        private int phase = 0;

        public CursedClockHands(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cursed_clock_hands", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(7.5);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(380);
            config.setCooldownTicks(360);
            config.setImpactDamage(30.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.4f);
            // central shroomlight pivot cluster (8 displays)
            for (int i = 0; i < 8; i++) {
                double a = Math.PI*2*i/8;
                Location p = center.clone().add(Math.cos(a)*0.25, 2.2, Math.sin(a)*0.25);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SHROOMLIGHT);
                h.scale(0.4f,0.4f,0.4f).glow(255, 220, 90).interpolation(8, 0);
                pivot.add(h);
            }
            // minute hand: 12 red glass blocks along +x (8 main + 4 width fill)
            for (int i = 0; i < 8; i++) {
                Location p = center.clone().add(0.5 + i, 2.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.RED_STAINED_GLASS);
                h.scale(0.85f, 0.25f, 1.4f).glow(255, 30, 30).interpolation(4, 0);
                minute.add(h);
            }
            for (int i = 0; i < 4; i++) {
                Location p = center.clone().add(1.5 + i * 1.5, 2.25, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.RED_STAINED_GLASS);
                h.scale(0.5f, 0.2f, 0.5f).glow(255, 60, 60).interpolation(4, 0);
                minute.add(h);
            }
            // hour hand: 8 blackstone blocks along +x (5 main + 3 accent at slightly different Y)
            for (int i = 0; i < 5; i++) {
                Location p = center.clone().add(0.5 + i, 2.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.75f, 0.25f, 1.4f).interpolation(4, 0);
                hour.add(h);
            }
            for (int i = 0; i < 3; i++) {
                Location p = center.clone().add(1.0 + i * 1.2, 1.95, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE);
                h.scale(0.45f, 0.2f, 0.5f).interpolation(4, 0);
                hour.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            if (tick < dur * 0.1) phase = 0;
            else if (tick < dur * 0.9) phase = 1;
            else phase = 2;

            // 12-snap event every 90 ticks: hold for 10, then reverse
            if (phase == 1 && tick % 90 == 0 && tick > 0) {
                minAngle = 0; hourAngle = 0; snapHoldTicks = 12;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.4f, 0.5f);
                triggerImpactDamage(getCenter().clone().add(8, 2.2, 0));
                reverse = !reverse;
            }

            double speedM = reverse ? -0.18 : 0.07;
            double speedH = reverse ? -0.022 : 0.009;
            if (snapHoldTicks > 0) { snapHoldTicks--; }
            else { minAngle += speedM; hourAngle += speedH; }

            if (tick % 2 == 0) {
                for (int i = 0; i < minute.size(); i++) {
                    double dist = 0.5 + i;
                    float tx = (float) (Math.cos(minAngle) * dist);
                    float tz = (float) (Math.sin(minAngle) * dist);
                    minute.get(i).animateTo(new Vector3f(tx - 0.425f, 2.2f, tz - 0.7f),
                            new AxisAngle4f((float) minAngle, 0, 1, 0),
                            new Vector3f(0.85f, 0.25f, 1.4f), 2);
                }
                for (int i = 0; i < hour.size(); i++) {
                    double dist = 0.5 + i;
                    float tx = (float) (Math.cos(hourAngle) * dist);
                    float tz = (float) (Math.sin(hourAngle) * dist);
                    hour.get(i).animateTo(new Vector3f(tx - 0.375f, 2.0f, tz - 0.7f),
                            new AxisAngle4f((float) hourAngle, 0, 1, 0),
                            new Vector3f(0.75f, 0.25f, 1.4f), 2);
                }
                // pivot pulse
                float ps = 0.4f + (float) Math.sin(tick * 0.2) * 0.15f;
                for (int i = 0; i < pivot.size(); i++) {
                    double a = Math.PI*2*i/5 + tick*0.15;
                    float tx = (float)(Math.cos(a)*0.25);
                    float tz = (float)(Math.sin(a)*0.25);
                    pivot.get(i).animateTo(new Vector3f(tx - ps/2f, 2.2f, tz - ps/2f),
                            new AxisAngle4f((float)(tick*0.15), 0, 1, 0),
                            new Vector3f(ps, ps, ps), 2);
                }
            }

            // particle trails
            if (tick % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double dist = 0.5 + i;
                    Location ml = getCenter().clone().add(Math.cos(minAngle)*dist, 2.4, Math.sin(minAngle)*dist);
                    DisplayBuilder.dustParticles(ml, 1, 0.1, 255, 30, 30, 1.4f);
                }
                for (int i = 0; i < 5; i++) {
                    double dist = 0.5 + i;
                    Location hl = getCenter().clone().add(Math.cos(hourAngle)*dist, 2.0, Math.sin(hourAngle)*dist);
                    w.spawnParticle(Particle.SMOKE, hl, 1, 0.1, 0.1, 0.1, 0.005);
                }
            }

            if (tick % 20 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CHAIN_FALL, 0.6f, 0.5f);
            }

            if (phase == 2 && tick % 6 == 0) {
                float s = Math.max(0.05f, 1.0f - (tick - dur * 0.9f) / (dur * 0.1f));
                for (BlockDisplayHandle h : minute) h.animateTo(new Vector3f(0,2.2f,0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.85f*s,0.25f*s,1.4f*s), 6);
                for (BlockDisplayHandle h : hour) h.animateTo(new Vector3f(0,2.0f,0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.75f*s,0.25f*s,1.4f*s), 6);
                for (BlockDisplayHandle h : pivot) h.animateTo(new Vector3f(0,2.2f,0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.4f*s,0.4f*s,0.4f*s), 6);
            }
        }

        @Override public AbstractAttack newInstance() { return new CursedClockHands(plugin); }
    }

    // ================================================================
    // 46. DREAMER'S RIBCAGE HEART
    //     Heart silhouette of crimson planks (with red-glass valves +
    //     wart-block vessels) inside a 4-rib cage. Beats. Cage constricts.
    //     Final crush = impact AoE.
    // ================================================================
    public static class DreamersRibcageHeart extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> heart = new ArrayList<>();
        private final List<BlockDisplayHandle> valves = new ArrayList<>();
        private final List<BlockDisplayHandle> vessels = new ArrayList<>();
        private final List<BlockDisplayHandle> ribs = new ArrayList<>();
        private int phase = 0;

        public DreamersRibcageHeart(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dreamers_ribcage_heart", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(7.5);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(5);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(33.0);
            config.setImpactRadius(7.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.2f, 0.4f);
            // heart shape — pixel-style heart in xz plane (lobes + point), 12 displays
            double[][] heartOffsets = {
                    {-1.0, 4.5, 0}, {1.0, 4.5, 0},     // top lobes
                    {-1.5, 4.0, 0}, {-0.5, 4.0, 0}, {0.5, 4.0, 0}, {1.5, 4.0, 0},
                    {-1.5, 3.5, 0}, {0, 3.5, 0}, {1.5, 3.5, 0},
                    {-1.0, 3.0, 0}, {1.0, 3.0, 0},
                    {0, 2.5, 0} // tip
            };
            for (double[] o : heartOffsets) {
                Location p = center.clone().add(o[0], o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRIMSON_PLANKS);
                h.scale(0.85f, 0.85f, 0.85f).glow(180, 30, 30).interpolation(4, 0);
                heart.add(h);
            }
            // 4 valve red-glass highlights
            double[][] valveOffsets = {{0, 4.2, 0.4}, {-0.6, 3.7, 0.4}, {0.6, 3.7, 0.4}, {0, 3.0, 0.4}};
            for (double[] o : valveOffsets) {
                Location p = center.clone().add(o[0], o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.RED_STAINED_GLASS);
                h.scale(0.4f, 0.4f, 0.4f).glow(255, 30, 30).interpolation(4, 0);
                valves.add(h);
            }
            // 3 vessels at top — nether wart blocks
            for (int i = 0; i < 3; i++) {
                Location p = center.clone().add(-0.6 + i * 0.6, 5.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.NETHER_WART_BLOCK);
                h.scale(0.45f, 0.6f, 0.45f).interpolation(4, 0);
                vessels.add(h);
            }
            // 4 visible front ribs (curved arc of 4 segments each = 16 displays)
            for (int rib = 0; rib < 4; rib++) {
                double yBase = 2.5 + rib * 0.7;
                for (int seg = 0; seg < 4; seg++) {
                    double t = seg / 3.0;
                    double angle = (Math.PI * 0.7) * (t - 0.5);
                    double x = Math.sin(angle) * 2.6;
                    double z = -Math.cos(angle) * 2.0;
                    Location p = center.clone().add(x, yBase + 1, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                    h.scale(0.4f, 0.4f, 0.4f).interpolation(4, 0);
                    ribs.add(h);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            if (tick < dur * 0.1) phase = 0;
            else if (tick < dur * 0.85) phase = 1;
            else phase = 2;

            float beatPeriod = 24f - Math.min(15f, (tick / (dur / 30f))); // BPM ramps up
            float beat = (float) Math.sin(tick * (2 * Math.PI / Math.max(beatPeriod, 8f)));
            float beatScale = 1.0f + beat * 0.25f;
            // ribs constrict (squeeze inward) over time toward end
            float constrict = Math.min(1.0f, (float) tick / (dur * 0.85f));
            float ribInward = 1.0f - constrict * 0.6f;

            if (tick % 3 == 0) {
                double[][] heartOffsets = {
                        {-1.0, 4.5, 0}, {1.0, 4.5, 0},
                        {-1.5, 4.0, 0}, {-0.5, 4.0, 0}, {0.5, 4.0, 0}, {1.5, 4.0, 0},
                        {-1.5, 3.5, 0}, {0, 3.5, 0}, {1.5, 3.5, 0},
                        {-1.0, 3.0, 0}, {1.0, 3.0, 0},
                        {0, 2.5, 0}
                };
                for (int i = 0; i < heart.size(); i++) {
                    heart.get(i).animateTo(new Vector3f(
                                    (float)heartOffsets[i][0] * beatScale - 0.425f,
                                    (float)heartOffsets[i][1],
                                    (float)heartOffsets[i][2] * beatScale - 0.425f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.85f * beatScale, 0.85f * beatScale, 0.85f * beatScale), 3);
                }
                double[][] valveOffsets = {{0, 4.2, 0.4}, {-0.6, 3.7, 0.4}, {0.6, 3.7, 0.4}, {0, 3.0, 0.4}};
                for (int i = 0; i < valves.size(); i++) {
                    float vs = (beat > 0) ? 0.6f : 0.3f;
                    valves.get(i).animateTo(new Vector3f(
                                    (float)valveOffsets[i][0] - vs/2f,
                                    (float)valveOffsets[i][1],
                                    (float)valveOffsets[i][2] - vs/2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(vs, vs, vs), 3);
                }
                for (int i = 0; i < vessels.size(); i++) {
                    float vs = 0.45f * beatScale;
                    vessels.get(i).animateTo(new Vector3f(
                                    -0.6f + i * 0.6f - vs/2f, 5.2f, -vs/2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(vs, 0.6f, vs), 3);
                }
                // ribs constrict inward
                for (int rib = 0; rib < 4; rib++) {
                    double yBase = 2.5 + rib * 0.7;
                    for (int seg = 0; seg < 4; seg++) {
                        int idx = rib * 4 + seg;
                        double t = seg / 3.0;
                        double angle = (Math.PI * 0.7) * (t - 0.5);
                        double x = Math.sin(angle) * 2.6 * ribInward;
                        double z = -Math.cos(angle) * 2.0 * ribInward;
                        ribs.get(idx).animateTo(new Vector3f(
                                        (float)x - 0.2f, (float)yBase + 1f, (float)z - 0.2f),
                                new AxisAngle4f((float)(constrict * 0.4), 0, 0, 1),
                                new Vector3f(0.4f, 0.4f, 0.4f), 3);
                    }
                }
            }

            // particles: red pulse on beat, soul fire inside
            if (beat > 0.9f && tick % 2 == 0) {
                Location heartCenter = getCenter().clone().add(0, 3.7, 0);
                w.spawnParticle(Particle.HEART, heartCenter, 4, 1.5, 1.5, 1.5, 0);
                DisplayBuilder.dustParticles(heartCenter, 6, 1.5, 255, 30, 30, 2.0f);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f,
                        0.5f + (1 - beatPeriod / 24f) * 0.8f);
            }
            if (tick % 4 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, getCenter().clone().add(0, 3.7, 0),
                        2, 1.0, 1.5, 1.0, 0.005);
                for (BlockDisplayHandle v : vessels) {
                    Location vl = getCenter().clone().add(0, 5.5, 0);
                    w.spawnParticle(Particle.CRIMSON_SPORE, vl, 3, 1.0, 0.3, 1.0, 0.02);
                }
            }

            // crush at end of phase 1
            if (tick == (int)(dur * 0.85) - 1) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.5f);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, getCenter().clone().add(0, 3.7, 0),
                        2, 1, 1, 1, 0);
                triggerImpactDamage(getCenter().clone().add(0, 3.7, 0));
                for (BlockDisplayHandle h : heart) h.animateTo(new Vector3f(0, 3.7f, 0),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f,0.05f,0.05f), 8);
                for (BlockDisplayHandle h : valves) h.animateTo(new Vector3f(0, 3.7f, 0),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f,0.05f,0.05f), 8);
                for (BlockDisplayHandle h : vessels) h.animateTo(new Vector3f(0, 3.7f, 0),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f,0.05f,0.05f), 8);
                for (BlockDisplayHandle h : ribs) h.animateTo(new Vector3f(0, 3.7f, 0),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.05f,0.05f,0.05f), 8);
            }
        }

        @Override public AbstractAttack newInstance() { return new DreamersRibcageHeart(plugin); }
    }

    // ================================================================
    // 47. PETRIFIED FOREST
    //     5 dead petrified trunks of varying heights with calcite branch
    //     stubs. Branches detach (project outward), trunks topple.
    // ================================================================
    public static class PetrifiedForest extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> trunks = new ArrayList<>();
        private final List<BlockDisplayHandle> branches = new ArrayList<>();
        private final List<BlockDisplayHandle> roots = new ArrayList<>();
        private final List<BlockDisplayHandle> caps = new ArrayList<>();
        private final int[] heights = {3, 4, 5, 6, 7};
        private int phase = 0;

        public PetrifiedForest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("petrified_forest", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(7.5);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(6);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(27.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_PLACE, 1.3f, 0.4f);
            // 5 trunks placed in a small cluster
            double[][] basePos = {
                    {-3, 0, -2.5}, {2.5, 0, -2}, {3, 0, 2.5}, {-2.5, 0, 2.8}, {0.5, 0, 0.5}
            };
            for (int t = 0; t < 5; t++) {
                int hgt = heights[t];
                for (int y = 0; y < hgt; y++) {
                    Location p = center.clone().add(basePos[t][0], 0.5 + y, basePos[t][2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                    h.scale(0.85f, 0.95f, 0.85f).interpolation(8, 0);
                    trunks.add(h);
                }
                // top cap deepslate
                Location capLoc = center.clone().add(basePos[t][0], 0.5 + hgt, basePos[t][2]);
                BlockDisplayHandle cap = displayBuilder.spawnBlock(capLoc, Material.DEEPSLATE_TILES);
                cap.scale(0.95f, 0.5f, 0.95f).interpolation(8, 0);
                caps.add(cap);
                // 3 branch stubs at top per trunk
                for (int b = 0; b < 3; b++) {
                    double bAng = Math.PI * 2 * b / 3 + t * 0.3;
                    Location bp = center.clone().add(
                            basePos[t][0] + Math.cos(bAng) * 0.9,
                            0.5 + hgt - 0.3,
                            basePos[t][2] + Math.sin(bAng) * 0.9);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(bp, Material.CALCITE);
                    h.scale(0.35f, 0.35f, 0.35f).glow(240, 240, 240).interpolation(8, 0);
                    branches.add(h);
                }
                // root cluster (1 tuff per trunk)
                Location rl = center.clone().add(basePos[t][0], 0.1, basePos[t][2]);
                BlockDisplayHandle r = displayBuilder.spawnBlock(rl, Material.TUFF);
                r.scale(1.2f, 0.3f, 1.2f).interpolation(8, 0);
                roots.add(r);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            if (tick < dur * 0.15) phase = 0;
            else if (tick < dur * 0.9) phase = 1;
            else phase = 2;

            // branch slow rotation
            if (tick % 4 == 0 && phase < 2) {
                double[][] basePos = {
                        {-3, 0, -2.5}, {2.5, 0, -2}, {3, 0, 2.5}, {-2.5, 0, 2.8}, {0.5, 0, 0.5}
                };
                for (int b = 0; b < branches.size(); b++) {
                    int t = b / 3;
                    int sub = b % 3;
                    double bAng = Math.PI * 2 * sub / 3 + t * 0.3 + tick * 0.05;
                    int hgt = heights[t];
                    float bx = (float)(basePos[t][0] + Math.cos(bAng) * 0.9);
                    float bz = (float)(basePos[t][2] + Math.sin(bAng) * 0.9);
                    branches.get(b).animateTo(
                            new Vector3f(bx - 0.175f, 0.5f + hgt - 0.3f, bz - 0.175f),
                            new AxisAngle4f((float)(tick * 0.1), 0, 1, 0),
                            new Vector3f(0.35f, 0.35f, 0.35f), 4);
                }
            }

            // sequential crack/topple — each trunk topples at tick = dur*0.4 + i*40
            if (phase == 1) {
                for (int t = 0; t < 5; t++) {
                    int toppleTick = (int)(dur * 0.35) + t * 40;
                    if (tick == toppleTick) {
                        DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_BREAK, 1.4f, 0.4f);
                        // detach branches
                        for (int b = 0; b < 3; b++) {
                            int idx = t * 3 + b;
                            if (idx < branches.size()) {
                                double ang = Math.PI*2*b/3 + Math.random()*0.3;
                                branches.get(idx).animateTo(new Vector3f(
                                                (float)Math.cos(ang)*7f, 1f + (float)Math.random()*3f, (float)Math.sin(ang)*7f),
                                        new AxisAngle4f((float)(Math.random()*Math.PI*4), 1, 1, 0),
                                        new Vector3f(0.35f, 0.35f, 0.35f), 30);
                            }
                        }
                        // topple direction
                        double topAng = Math.PI*2*t/5;
                        // each trunk segment falls
                        int trunkStart = 0;
                        for (int tt = 0; tt < t; tt++) trunkStart += heights[tt];
                        double[][] basePos = {
                                {-3, 0, -2.5}, {2.5, 0, -2}, {3, 0, 2.5}, {-2.5, 0, 2.8}, {0.5, 0, 0.5}
                        };
                        for (int y = 0; y < heights[t]; y++) {
                            double radial = y * 0.8;
                            float fx = (float)(basePos[t][0] + Math.cos(topAng) * radial);
                            float fz = (float)(basePos[t][2] + Math.sin(topAng) * radial);
                            trunks.get(trunkStart + y).animateTo(
                                    new Vector3f(fx - 0.425f, 0.3f, fz - 0.425f),
                                    new AxisAngle4f((float)(Math.PI / 2), (float)Math.sin(topAng), 0, (float)Math.cos(topAng)),
                                    new Vector3f(0.85f, 0.95f, 0.85f), 24);
                        }
                        // cap also falls
                        if (t < caps.size()) {
                            float fx = (float)(basePos[t][0] + Math.cos(topAng) * heights[t] * 0.8);
                            float fz = (float)(basePos[t][2] + Math.sin(topAng) * heights[t] * 0.8);
                            caps.get(t).animateTo(new Vector3f(fx - 0.475f, 0.3f, fz - 0.475f),
                                    new AxisAngle4f((float)(Math.PI / 2), (float)Math.sin(topAng), 0, (float)Math.cos(topAng)),
                                    new Vector3f(0.95f, 0.5f, 0.95f), 24);
                        }
                        triggerImpactDamage(getCenter().clone().add(
                                basePos[t][0] + Math.cos(topAng) * heights[t] * 0.5, 1,
                                basePos[t][2] + Math.sin(topAng) * heights[t] * 0.5));
                    }
                }
            }

            if (tick % 3 == 0) {
                double[][] basePos = {
                        {-3, 0, -2.5}, {2.5, 0, -2}, {3, 0, 2.5}, {-2.5, 0, 2.8}, {0.5, 0, 0.5}
                };
                for (double[] bp : basePos) {
                    Location loc = getCenter().clone().add(bp[0], 1.5 + Math.random()*4, bp[2]);
                    DisplayBuilder.dustParticles(loc, 1, 0.3, 110, 110, 110, 1.2f);
                }
            }
            if (tick % 22 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new PetrifiedForest(plugin); }
    }

    // ================================================================
    // 48. THE LIVING PORTRAIT
    //     6x8 obsidian canvas in blackstone-brick frame, crying-obsidian
    //     figure outline that reaches outward then steps OUT of the canvas.
    //     Frame fires inward projectiles, figure deals burst damage.
    // ================================================================
    public static class LivingPortrait extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> frame = new ArrayList<>();
        private final List<BlockDisplayHandle> canvas = new ArrayList<>();
        private final List<BlockDisplayHandle> figure = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private int phase = 0;
        private float reachOff = 0f;

        public LivingPortrait(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("living_portrait", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(30.0);
            config.setImpactRadius(7.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.4f);
            // frame: 6x8 outer border (top/bottom 6 wide, sides 6 tall = 6+6+8+8 - 4 corners shared = 24)
            // Simplified: top row(6), bottom row(6), left col 6, right col 6 -- some overlap, ~24 unique displays
            // top + bottom rows
            for (int dx = 0; dx < 8; dx++) {
                Location top = center.clone().add(dx - 3.5, 8, 0);
                BlockDisplayHandle ht = displayBuilder.spawnBlock(top, Material.POLISHED_BLACKSTONE_BRICKS);
                ht.scale(1.0f, 1.0f, 0.5f).interpolation(6, 0);
                frame.add(ht);
                Location bot = center.clone().add(dx - 3.5, 1, 0);
                BlockDisplayHandle hb = displayBuilder.spawnBlock(bot, Material.POLISHED_BLACKSTONE_BRICKS);
                hb.scale(1.0f, 1.0f, 0.5f).interpolation(6, 0);
                frame.add(hb);
            }
            for (int dy = 1; dy < 8; dy++) {
                Location lf = center.clone().add(-3.5, 1 + dy, 0);
                BlockDisplayHandle hl = displayBuilder.spawnBlock(lf, Material.POLISHED_BLACKSTONE_BRICKS);
                hl.scale(1.0f, 1.0f, 0.5f).interpolation(6, 0);
                frame.add(hl);
                Location rt = center.clone().add(3.5, 1 + dy, 0);
                BlockDisplayHandle hr = displayBuilder.spawnBlock(rt, Material.POLISHED_BLACKSTONE_BRICKS);
                hr.scale(1.0f, 1.0f, 0.5f).interpolation(6, 0);
                frame.add(hr);
            }
            // canvas — coarse 3x4 obsidian fill (12 displays scaled wide)
            for (int dy = 0; dy < 4; dy++) {
                for (int dx = 0; dx < 3; dx++) {
                    Location p = center.clone().add(dx * 2 - 2, 2.5 + dy * 1.5, -0.1);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.OBSIDIAN);
                    h.scale(2.0f, 1.5f, 0.3f).interpolation(6, 0);
                    canvas.add(h);
                }
            }
            // figure outline — abstract tall figure, ~10 crying-obsidian displays
            double[][] fig = {
                    {0, 7.0, -0.2},                    // head
                    {-0.5, 6.3, -0.2}, {0.5, 6.3, -0.2}, // shoulders
                    {0, 5.5, -0.2},                    // chest
                    {-0.7, 5.0, -0.2}, {0.7, 5.0, -0.2}, // hands
                    {0, 4.3, -0.2},                    // belly
                    {-0.4, 3.2, -0.2}, {0.4, 3.2, -0.2}, // hips
                    {0, 2.0, -0.2}                     // feet
            };
            for (double[] f : fig) {
                Location p = center.clone().add(f[0], f[1], f[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(0.55f, 0.55f, 0.55f).glow(90, 0, 130).interpolation(4, 0);
                figure.add(h);
            }
            // 2 shroomlight eyes
            for (int i = 0; i < 2; i++) {
                Location e = center.clone().add((i == 0) ? -0.18 : 0.18, 7.1, -0.2);
                BlockDisplayHandle eh = displayBuilder.spawnBlock(e, Material.SHROOMLIGHT);
                eh.scale(0.18f, 0.18f, 0.18f).glow(255, 220, 90).interpolation(4, 0);
                eyes.add(eh);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            if (tick < dur * 0.3) phase = 0;       // hanging, reaching
            else if (tick < dur * 0.5) phase = 1;  // tearing out
            else if (tick < dur * 0.9) phase = 2;  // 3D figure attacking
            else phase = 3;                        // dissipate

            // figure reaches outward (Z+) during phase 0
            if (phase == 0) {
                reachOff = (float)(tick / (dur * 0.3f)) * 1.2f;
            } else if (phase == 1) {
                reachOff = 1.2f + ((tick - dur * 0.3f) / (dur * 0.2f)) * 4f;
            } else if (phase == 2) {
                reachOff = 5.2f + (float) Math.sin(tick * 0.2) * 0.6f;
            }

            if (tick % 3 == 0) {
                double[][] fig = {
                        {0, 7.0, -0.2},
                        {-0.5, 6.3, -0.2}, {0.5, 6.3, -0.2},
                        {0, 5.5, -0.2},
                        {-0.7, 5.0, -0.2}, {0.7, 5.0, -0.2},
                        {0, 4.3, -0.2},
                        {-0.4, 3.2, -0.2}, {0.4, 3.2, -0.2},
                        {0, 2.0, -0.2}
                };
                for (int i = 0; i < figure.size(); i++) {
                    float flailX = (phase == 2 && (i == 4 || i == 5)) ? (float) Math.sin(tick * 0.3 + i) * 0.7f : 0f;
                    figure.get(i).animateTo(new Vector3f(
                                    (float)fig[i][0] + flailX - 0.275f, (float)fig[i][1],
                                    (float)fig[i][2] + reachOff - 0.275f),
                            new AxisAngle4f(flailX, 1, 0, 0),
                            new Vector3f(0.55f, 0.55f, 0.55f), 3);
                }
                for (int i = 0; i < eyes.size(); i++) {
                    eyes.get(i).animateTo(new Vector3f(
                                    ((i == 0) ? -0.18f : 0.18f) - 0.09f, 7.1f,
                                    -0.2f + reachOff - 0.09f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.18f, 0.18f, 0.18f), 3);
                }
            }

            // eye-tracking particles
            if (tick % 3 == 0) {
                Location eyeLoc = getCenter().clone().add(0, 7.1, reachOff - 0.2);
                DisplayBuilder.dustParticles(eyeLoc, 2, 0.2, 255, 220, 90, 1.4f);
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, getCenter().clone().add(0, 5, 0),
                        2, 3, 3, 0.3, 0);
            }

            // phase 1: step-out flash
            if (phase == 1 && tick == (int)(dur * 0.35)) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, getCenter().clone().add(0, 4, 1), 30, 1, 2, 1, 0.05);
                triggerImpactDamage(getCenter().clone().add(0, 4, reachOff));
            }

            // phase 2: frame fires inward projectile pulses
            if (phase == 2 && tick % 18 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_HURT, 0.8f, 0.4f);
                for (int i = 0; i < 4; i++) {
                    double ang = Math.PI*2*i/4;
                    Location start = getCenter().clone().add(Math.cos(ang)*4, 4.5, Math.sin(ang)*4);
                    Location end = getCenter().clone().add(0, 4.5, 0);
                    DisplayBuilder.particleLine(start, end, Particle.SCULK_SOUL, 8, null);
                }
                triggerImpactDamage(getCenter().clone().add(0, 4.5, 0));
            }
            if (phase == 2 && tick % 20 == 0) {
                triggerImpactDamage(getCenter().clone().add(0, 4, reachOff));
            }

            if (phase == 3 && tick % 6 == 0) {
                float s = Math.max(0.05f, 1.0f - (tick - dur * 0.9f) / (dur * 0.1f));
                for (BlockDisplayHandle h : frame) h.animateTo(new Vector3f(0, 4.5f, 0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(s, s, 0.5f * s), 6);
                for (BlockDisplayHandle h : canvas) h.animateTo(new Vector3f(0, 4.5f, 0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(2*s, 1.5f*s, 0.3f*s), 6);
                for (BlockDisplayHandle h : figure) h.animateTo(new Vector3f(0, 4.5f, reachOff),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.55f*s, 0.55f*s, 0.55f*s), 6);
                for (BlockDisplayHandle h : eyes) h.animateTo(new Vector3f(0, 7.1f, reachOff - 0.2f),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.18f*s, 0.18f*s, 0.18f*s), 6);
            }
        }

        @Override public AbstractAttack newInstance() { return new LivingPortrait(plugin); }
    }

    // ================================================================
    // 49. FALLING ANGEL
    //     Bone/calcite angel silhouette plummets from sky — wide curved
    //     wings, body column, head, dark vein patterns. Wings fold at
    //     terminal velocity, slams ground = impact AoE + shockwave ring.
    // ================================================================
    public static class FallingAngel extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wingsL = new ArrayList<>();
        private final List<BlockDisplayHandle> wingsR = new ArrayList<>();
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> veins = new ArrayList<>();
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private int phase = 0;
        private double startY = 22;

        public FallingAngel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("falling_angel", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(33.0);
            config.setImpactRadius(8.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.3f, 0.4f);
            // body column — 5 segments of bone/calcite
            for (int i = 0; i < 5; i++) {
                Location p = center.clone().add(0, startY + i * 0.7, 0);
                Material mat = (i % 2 == 0) ? Material.BONE_BLOCK : Material.CALCITE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                h.scale(0.7f, 0.7f, 0.7f).glow(240, 230, 200).interpolation(6, 0);
                body.add(h);
            }
            // head cluster
            for (int i = 0; i < 3; i++) {
                Location p = center.clone().add((i - 1) * 0.4, startY + 4.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BONE_BLOCK);
                h.scale(0.5f,0.5f,0.5f).glow(240, 230, 200).interpolation(6, 0);
                head.add(h);
            }
            // wings — left + right curved arcs of 6 displays each
            for (int side = -1; side <= 1; side += 2) {
                List<BlockDisplayHandle> lst = (side == -1) ? wingsL : wingsR;
                for (int i = 0; i < 6; i++) {
                    double t = i / 5.0;
                    double dx = side * (0.8 + t * 5.5);
                    double dy = startY + 2.0 + Math.sin(t * Math.PI) * 1.6;
                    double dz = 0;
                    Location p = center.clone().add(dx, dy, dz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BONE_BLOCK);
                    h.scale(0.7f, 0.45f, 0.55f).glow(240, 230, 200).interpolation(6, 0);
                    lst.add(h);
                }
            }
            // veins along wings — 8 crying-obsidian flecks
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 4; i++) {
                    double t = (i + 0.5) / 4.0;
                    double dx = side * (0.8 + t * 5.5);
                    double dy = startY + 2.0 + Math.sin(t * Math.PI) * 1.6;
                    Location p = center.clone().add(dx, dy + 0.2, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                    h.scale(0.25f, 0.25f, 0.25f).glow(90, 0, 130).interpolation(4, 0);
                    veins.add(h);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            int impactAt = (int)(dur * 0.55);
            if (tick < impactAt - 30) phase = 0;            // descent, wings open
            else if (tick < impactAt) phase = 1;            // wings fold
            else if (tick < dur * 0.85) phase = 2;          // shattered + reassemble
            else phase = 3;                                 // dissipate

            double progress = Math.min(1.0, (double) tick / impactAt);
            double curY = startY * (1.0 - progress * progress);
            // Force ground-touch in the final 4 ticks before impact so the angel actually
            // reaches the ground regardless of % 3 modulo skip on animation tick.
            if (phase < 2 && tick >= impactAt - 4) curY = 0.0;
            float wingFold = (phase >= 1) ? Math.min(1.0f, (tick - (impactAt - 30)) / 30f) : 0f;

            // Falling debris trail column — embers/dust streaming from angel's height down to ground
            if (phase < 2 && tick % 2 == 0) {
                Location c = getCenter();
                for (int trail = 0; trail < 4; trail++) {
                    double tx = (Math.random() - 0.5) * 6;
                    double tz = (Math.random() - 0.5) * 6;
                    double ty = Math.random() * Math.max(1.0, curY + 4);
                    DisplayBuilder.dustParticles(c.clone().add(tx, ty, tz), 1, 0.2, 240, 230, 200, 1.2f);
                }
            }

            if ((tick % 3 == 0 || tick == impactAt - 1) && phase < 2) {
                for (int i = 0; i < body.size(); i++) {
                    body.get(i).animateTo(new Vector3f(-0.35f, (float)curY + i * 0.7f, -0.35f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 3);
                }
                for (int i = 0; i < head.size(); i++) {
                    head.get(i).animateTo(new Vector3f((i - 1) * 0.4f - 0.25f, (float)curY + 4f, -0.25f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.5f, 0.5f, 0.5f), 3);
                }
                for (int side = -1; side <= 1; side += 2) {
                    List<BlockDisplayHandle> lst = (side == -1) ? wingsL : wingsR;
                    for (int i = 0; i < 6; i++) {
                        double t = i / 5.0;
                        double extX = (1.0 - wingFold) * (0.8 + t * 5.5)
                                + wingFold * (0.5 + t * 1.0);
                        double dx = side * extX;
                        double dy = curY + 2.0 + Math.sin(t * Math.PI) * 1.6 * (1.0 - wingFold * 0.7);
                        lst.get(i).animateTo(new Vector3f((float)dx - 0.35f, (float)dy, -0.275f),
                                new AxisAngle4f(wingFold * (float)Math.PI / 4f * side, 0, 0, 1),
                                new Vector3f(0.7f, 0.45f, 0.55f), 3);
                    }
                }
                int idx = 0;
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < 4; i++) {
                        double t = (i + 0.5) / 4.0;
                        double extX = (1.0 - wingFold) * (0.8 + t * 5.5)
                                + wingFold * (0.5 + t * 1.0);
                        double dx = side * extX;
                        double dy = curY + 2.0 + Math.sin(t * Math.PI) * 1.6 * (1.0 - wingFold * 0.7) + 0.2;
                        veins.get(idx++).animateTo(new Vector3f((float)dx - 0.125f, (float)dy, -0.125f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.25f, 0.25f, 0.25f), 3);
                    }
                }
            }

            // particles during fall
            if (phase == 0 && tick % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    Location loc = getCenter().clone().add(
                            (Math.random()-0.5)*8, curY + Math.random()*4, (Math.random()-0.5)*8);
                    DisplayBuilder.dustParticles(loc, 1, 0.3, 240, 230, 200, 1.4f);
                }
                w.spawnParticle(Particle.SCULK_CHARGE_POP, getCenter().clone().add(0, curY + 2, 0),
                        4, 5, 1, 5, 0);
            }

            // impact — ground hit, angel detonates AT the ground
            if (tick == impactAt) {
                Location ground = getCenter();
                DisplayBuilder.playSound(ground, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.4f);
                DisplayBuilder.playSound(ground, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.5f);
                // ground-level explosion + sonic boom right at impact point
                w.spawnParticle(Particle.EXPLOSION_EMITTER, ground.clone().add(0, 0.2, 0), 5, 2.5, 0.3, 2.5, 0);
                w.spawnParticle(Particle.SONIC_BOOM, ground.clone().add(0, 0.5, 0), 2, 0.5, 0.2, 0.5, 0);
                w.spawnParticle(Particle.FLASH, ground.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
                // expanding shockwave rings at GROUND level — multiple radii
                for (int r = 1; r <= 10; r++) {
                    DisplayBuilder.particleRing(ground.clone().add(0, 0.2, 0), r,
                            Particle.LARGE_SMOKE, 28, null);
                    DisplayBuilder.particleRing(ground.clone().add(0, 0.4, 0), r,
                            Particle.CAMPFIRE_COSY_SMOKE, 18, null);
                }
                // ground-level dust ring (bone-white)
                for (int r = 1; r <= 8; r++) {
                    Location ring = ground.clone().add(0, 0.15, 0);
                    DisplayBuilder.dustParticles(ring, 36, r, 240, 230, 200, 1.6f);
                }
                // vertical pillar of upward smoke/sparks (visualises the impact reaching from ground UP)
                for (int hI = 0; hI <= 12; hI++) {
                    Location col = ground.clone().add(0, hI * 0.6, 0);
                    w.spawnParticle(Particle.LARGE_SMOKE, col, 6, 0.8, 0.1, 0.8, 0.02);
                    if (hI < 6) w.spawnParticle(Particle.LAVA, col, 2, 0.5, 0.1, 0.5, 0);
                }
                triggerImpactDamage(ground);
                // shatter — scatter all body parts AT GROUND level (Y=0) outward
                for (BlockDisplayHandle h : body) {
                    double a = Math.random() * Math.PI * 2;
                    h.animateTo(new Vector3f((float) Math.cos(a) * 3 - 0.35f, 0.1f, (float) Math.sin(a) * 3 - 0.35f),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 4), 1, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 20);
                }
                // wings shatter outward at ground
                for (BlockDisplayHandle h : wingsL) {
                    double a = Math.PI + Math.random() * Math.PI * 0.6 - 0.3;
                    h.animateTo(new Vector3f((float) Math.cos(a) * 5 - 0.35f, 0.1f, (float) Math.sin(a) * 5 - 0.275f),
                            new AxisAngle4f((float) (Math.random() * Math.PI), 0, 1, 0),
                            new Vector3f(0.7f, 0.45f, 0.55f), 20);
                }
                for (BlockDisplayHandle h : wingsR) {
                    double a = Math.random() * Math.PI * 0.6 - 0.3;
                    h.animateTo(new Vector3f((float) Math.cos(a) * 5 - 0.35f, 0.1f, (float) Math.sin(a) * 5 - 0.275f),
                            new AxisAngle4f((float) (Math.random() * Math.PI), 0, 1, 0),
                            new Vector3f(0.7f, 0.45f, 0.55f), 20);
                }
                // head crashes to ground
                for (BlockDisplayHandle h : head) {
                    double a = Math.random() * Math.PI * 2;
                    h.animateTo(new Vector3f((float) Math.cos(a) * 1.5f - 0.25f, 0.1f, (float) Math.sin(a) * 1.5f - 0.25f),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 2), 1, 1, 1),
                            new Vector3f(0.5f, 0.5f, 0.5f), 16);
                }
                // veins scatter at ground
                for (BlockDisplayHandle h : veins) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 1 + Math.random() * 4;
                    h.animateTo(new Vector3f((float) (Math.cos(a) * r) - 0.125f, 0.05f, (float) (Math.sin(a) * r) - 0.125f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.25f, 0.25f, 0.25f), 18);
                }
            }

            // phase 2 — settle at ground level, NOT mid-air
            if (phase == 2 && tick == impactAt + 30) {
                for (int i = 0; i < body.size(); i++) {
                    body.get(i).animateTo(new Vector3f(-0.35f, 0.1f + i * 0.15f, -0.35f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 30);
                }
            }

            if (tick % 14 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_PHANTOM_FLAP, 0.7f, 0.5f);
            }

            // dissipate — sink into the ground (NOT rise up)
            if (phase == 3 && tick % 6 == 0) {
                float s = Math.max(0.05f, 1.0f - (tick - dur * 0.85f) / (dur * 0.15f));
                for (BlockDisplayHandle h : body) h.animateTo(new Vector3f(0, -1, 0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.7f*s,0.7f*s,0.7f*s), 6);
                for (BlockDisplayHandle h : head) h.animateTo(new Vector3f(0, -1, 0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.5f*s,0.5f*s,0.5f*s), 6);
                for (BlockDisplayHandle h : wingsL) h.animateTo(new Vector3f(0, -1, 0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.7f*s,0.45f*s,0.55f*s), 6);
                for (BlockDisplayHandle h : wingsR) h.animateTo(new Vector3f(0, -1, 0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.7f*s,0.45f*s,0.55f*s), 6);
                for (BlockDisplayHandle h : veins) h.animateTo(new Vector3f(0, -1, 0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.25f*s,0.25f*s,0.25f*s), 6);
            }
        }

        @Override public AbstractAttack newInstance() { return new FallingAngel(plugin); }
    }

    // ================================================================
    // 50. THE DAMNATION SUNDIAL
    //     Horizontal sundial face (~8 block diameter blackstone) with 12
    //     red-glass hour markings, nether-brick rim, diagonal shroomlight
    //     gnomon. Whole sundial spins; each hour mark lights as gnomon
    //     passes. Per rotation, one mark shatters outward as a projectile.
    // ================================================================
    public static class DamnationSundial extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> face = new ArrayList<>();
        private final List<BlockDisplayHandle> rim = new ArrayList<>();
        private final List<BlockDisplayHandle> hours = new ArrayList<>();
        private final List<BlockDisplayHandle> gnomon = new ArrayList<>();
        private final boolean[] shattered = new boolean[12];
        private int phase = 0;
        private int rotations = 0;
        private double prevSweep = 0;

        public DamnationSundial(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("damnation_sundial", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(5);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(27.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(420);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SHROOMLIGHT_PLACE, 1.4f, 0.4f);
            // face: 7 displays in plus pattern + corners (cross fill)
            double[][] faceOff = {
                    {0,0},{1,0},{-1,0},{0,1},{0,-1},
                    {1,1},{-1,1},{1,-1},{-1,-1},
                    {2,0},{-2,0},{0,2},{0,-2},
                    {2,1},{-2,1},{2,-1},{-2,-1}
            };
            for (double[] o : faceOff) {
                Location p = center.clone().add(o[0], 0.3, o[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                h.scale(1.0f, 0.3f, 1.0f).interpolation(8, 0);
                face.add(h);
            }
            // rim: 12 nether-brick around outer ring
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12;
                Location p = center.clone().add(Math.cos(a) * 4, 0.4, Math.sin(a) * 4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.NETHER_BRICKS);
                h.scale(0.7f, 0.5f, 0.7f).interpolation(8, 0);
                rim.add(h);
            }
            // 12 red-glass hour markings just inside rim
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12;
                Location p = center.clone().add(Math.cos(a) * 3.5, 0.5, Math.sin(a) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.RED_STAINED_GLASS);
                h.scale(0.4f, 0.4f, 0.4f).glow(255, 30, 30).interpolation(4, 0);
                hours.add(h);
            }
            // gnomon: 6 shroomlight blocks at 45° from center to top edge
            for (int i = 0; i < 6; i++) {
                double r = i * 0.6;
                Location p = center.clone().add(r * 0.707, 0.4 + r * 0.707, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SHROOMLIGHT);
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 220, 90).interpolation(6, 0);
                gnomon.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            if (tick < dur * 0.1) phase = 0;
            else if (tick < dur * 0.9) phase = 1;
            else phase = 2;

            double sweep = tick * 0.05;
            if (sweep / (Math.PI * 2) > rotations + 1 && phase == 1) {
                rotations++;
                // shatter one hour marker
                int target = (int)(Math.random() * 12);
                int tries = 0;
                while (shattered[target] && tries < 12) { target = (target + 1) % 12; tries++; }
                if (!shattered[target] && target < hours.size()) {
                    shattered[target] = true;
                    double a = Math.PI * 2 * target / 12;
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
                    hours.get(target).animateTo(new Vector3f(
                                    (float)(Math.cos(a) * 9) - 0.2f, 0.5f, (float)(Math.sin(a) * 9) - 0.2f),
                            new AxisAngle4f((float)(Math.PI * 4), 1, 1, 0),
                            new Vector3f(0.4f, 0.4f, 0.4f), 30);
                    triggerImpactDamage(getCenter().clone().add(Math.cos(a) * 6, 0.5, Math.sin(a) * 6));
                    // face cracks (violent shake on whole face)
                    w.spawnParticle(Particle.BLOCK, getCenter().clone().add(0, 0.4, 0), 30, 3, 0.2, 3, 0,
                            Material.POLISHED_BLACKSTONE_BRICKS.createBlockData());
                }
            }

            // gnomon rotates about Y
            if (tick % 3 == 0) {
                for (int i = 0; i < gnomon.size(); i++) {
                    double r = i * 0.6;
                    double bx = r * 0.707;
                    double by = r * 0.707;
                    // rotate (bx, *, 0) by sweep around y axis
                    double rx = bx * Math.cos(sweep);
                    double rz = bx * Math.sin(sweep);
                    gnomon.get(i).animateTo(new Vector3f((float)rx - 0.275f, (float)by + 0.4f, (float)rz - 0.275f),
                            new AxisAngle4f((float)sweep, 0, 1, 0),
                            new Vector3f(0.55f, 0.55f, 0.55f), 3);
                }

                // face slow rotation (subtle)
                double[][] faceOff = {
                        {0,0},{1,0},{-1,0},{0,1},{0,-1},
                        {1,1},{-1,1},{1,-1},{-1,-1},
                        {2,0},{-2,0},{0,2},{0,-2},
                        {2,1},{-2,1},{2,-1},{-2,-1}
                };
                for (int i = 0; i < face.size(); i++) {
                    double cx = faceOff[i][0];
                    double cz = faceOff[i][1];
                    double rx = cx * Math.cos(sweep) - cz * Math.sin(sweep);
                    double rz = cx * Math.sin(sweep) + cz * Math.cos(sweep);
                    face.get(i).animateTo(new Vector3f((float)rx - 0.5f, 0.3f, (float)rz - 0.5f),
                            new AxisAngle4f((float)sweep, 0, 1, 0),
                            new Vector3f(1.0f, 0.3f, 1.0f), 3);
                }
                // rim follow rotation
                for (int i = 0; i < rim.size(); i++) {
                    double a = Math.PI * 2 * i / 12 + sweep;
                    rim.get(i).animateTo(new Vector3f((float)(Math.cos(a)*4) - 0.35f, 0.4f, (float)(Math.sin(a)*4) - 0.35f),
                            new AxisAngle4f((float)sweep, 0, 1, 0),
                            new Vector3f(0.7f, 0.5f, 0.7f), 3);
                }
                for (int i = 0; i < hours.size(); i++) {
                    if (shattered[i]) continue;
                    double a = Math.PI * 2 * i / 12 + sweep;
                    // light up if gnomon currently passing this hour
                    double angDelta = ((sweep % (Math.PI*2)) - (Math.PI * 2 * i / 12) + Math.PI * 4) % (Math.PI * 2);
                    boolean lit = angDelta < 0.5 || angDelta > (Math.PI*2 - 0.5);
                    float scl = lit ? 0.8f : 0.4f;
                    hours.get(i).animateTo(new Vector3f((float)(Math.cos(a)*3.5) - scl/2f, 0.5f, (float)(Math.sin(a)*3.5) - scl/2f),
                            new AxisAngle4f((float)sweep, 0, 1, 0),
                            new Vector3f(scl, scl, scl), 3);
                    if (lit && !shattered[i] && (Math.abs(angDelta) < 0.05 || Math.abs(angDelta - Math.PI*2) < 0.05)) {
                        DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SHROOMLIGHT_STEP, 0.9f, 0.7f);
                        DisplayBuilder.dustParticles(getCenter().clone().add(Math.cos(a)*3.5, 1, Math.sin(a)*3.5),
                                4, 0.2, 255, 30, 30, 1.6f);
                    }
                }
            }

            // gnomon trail particles + shadow line damage trace
            if (tick % 2 == 0) {
                Location tip = getCenter().clone().add(Math.cos(sweep) * 3, 2.5, Math.sin(sweep) * 3);
                DisplayBuilder.dustParticles(tip, 3, 0.3, 255, 220, 90, 1.4f);
                Location ground = getCenter().clone().add(Math.cos(sweep) * 4, 0.5, Math.sin(sweep) * 4);
                DisplayBuilder.particleLine(getCenter().clone().add(0, 0.5, 0), ground,
                        Particle.SMOKE, 14, null);
            }

            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SHROOMLIGHT_HIT, 0.7f, 0.5f);
            }

            if (phase == 2 && tick % 6 == 0) {
                float s = Math.max(0.05f, 1.0f - (tick - dur * 0.9f) / (dur * 0.1f));
                for (BlockDisplayHandle h : face) h.animateTo(new Vector3f(0, 0.3f, 0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(s, 0.3f*s, s), 6);
                for (BlockDisplayHandle h : rim) h.animateTo(new Vector3f(0, 0.4f, 0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.7f*s, 0.5f*s, 0.7f*s), 6);
                for (BlockDisplayHandle h : hours) h.animateTo(new Vector3f(0, 0.5f, 0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.4f*s, 0.4f*s, 0.4f*s), 6);
                for (BlockDisplayHandle h : gnomon) h.animateTo(new Vector3f(0, 0.4f, 0),
                        new AxisAngle4f(0,0,1,0), new Vector3f(0.55f*s, 0.55f*s, 0.55f*s), 6);
            }

            prevSweep = sweep;
        }

        @Override public AbstractAttack newInstance() { return new DamnationSundial(plugin); }
    }
}
