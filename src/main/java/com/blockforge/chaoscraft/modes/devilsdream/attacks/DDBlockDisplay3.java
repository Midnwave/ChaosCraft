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
 * Devil's Dream Mode — BLOCK DISPLAY ATTACKS Volume 3 (entries 21-30).
 * Each attack is a recognizable, multi-phase BlockDisplay structure built
 * from at least 25 displays, with smooth interpolated animations,
 * thematic particles + sound, and damage tuned per the design doc.
 *
 * Lifecycle phases used per attack:
 *   phase 0 = spawn / rise / assemble
 *   phase 1 = active / idle loop
 *   phase 2 = climactic action (slam, hurl, implode, etc.)
 *   phase 3 = dissipate / fade
 */
public final class DDBlockDisplay3 {
    private DDBlockDisplay3() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CursedLantern(plugin));
        registry.register(new PlagueBlossoms(plugin));
        registry.register(new VoidColosseumRing(plugin));
        registry.register(new HellfireMarionette(plugin));
        registry.register(new DevilsDreamcatcher(plugin));
        registry.register(new VoidSerpentShed(plugin));
        registry.register(new BurningThrone(plugin));
        registry.register(new ThornMeridian(plugin));
        registry.register(new CarrionWheel(plugin));
        registry.register(new UndertakersShovel(plugin));
    }

    // ================================================================
    // 21. CURSED LANTERN — hanging lantern, sways, then hurls itself.
    //     ~30 displays.
    // ================================================================
    public static class CursedLantern extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cage = new ArrayList<>();
        private final List<BlockDisplayHandle> bars = new ArrayList<>();
        private final List<BlockDisplayHandle> panes = new ArrayList<>();
        private final List<BlockDisplayHandle> core = new ArrayList<>();
        private Location anchor;
        private Vector hurlDir = new Vector(1, 0, 0);
        private Location impactLoc;
        private int phase = 0;

        public CursedLantern(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cursed_lantern", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(27.0);
            config.setImpactRadius(10.4);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(280);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            anchor = center.clone().add(0, 5.5, 0);
            DisplayBuilder.playSound(center, Sound.BLOCK_LANTERN_STEP, 1.2f, 0.6f);

            // Cage frame (8 blackstone corner posts arranged in 3-tall column = 24)
            double[] dx = {-1.2, 1.2, -1.2, 1.2};
            double[] dz = {-1.2, -1.2, 1.2, 1.2};
            for (int c = 0; c < 4; c++) {
                for (int y = 0; y < 4; y++) {
                    Location p = anchor.clone().add(dx[c], -y * 0.8, dz[c]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                    h.scale(0.5f, 0.5f, 0.5f).interpolation(8, 0);
                    cage.add(h);
                }
            }
            // Top + bottom caps (4 blackstone)
            for (double[] off : new double[][]{{0, 0.6, 0}, {0, -2.8, 0}, {0, 0.3, 0}, {0, -2.5, 0}}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        anchor.clone().add(off[0], off[1], off[2]), Material.POLISHED_BLACKSTONE_BRICKS);
                h.scale(1.3f, 0.4f, 1.3f).interpolation(8, 0);
                cage.add(h);
            }
            // Crying obsidian vertical bars (6)
            double[] bx = {-1.4, 1.4, 0, 0, -1.0, 1.0};
            double[] bz = {0, 0, -1.4, 1.4, -1.0, 1.0};
            for (int i = 0; i < 6; i++) {
                Location p = anchor.clone().add(bx[i], -1.0, bz[i]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(0.35f, 2.4f, 0.35f).glow(140, 0, 30).interpolation(8, 0);
                bars.add(h);
            }
            // 4 red-glass panes
            for (double[] off : new double[][]{{0, -1, -1.05}, {0, -1, 1.05}, {-1.05, -1, 0}, {1.05, -1, 0}}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        anchor.clone().add(off[0], off[1], off[2]), Material.RED_STAINED_GLASS);
                h.scale(1.6f, 1.6f, 0.15f).glow(180, 30, 30).interpolation(8, 0);
                panes.add(h);
            }
            // Shroomlight core (3 stacked)
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        anchor.clone().add(0, -0.6 - i * 0.6, 0), Material.SHROOMLIGHT);
                h.scale(0.7f, 0.7f, 0.7f).glow(255, 120, 30).interpolation(8, 0);
                core.add(h);
            }
            // Chain link line up (4 chain blocks)
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        anchor.clone().add(0, 1.0 + i * 0.6, 0), Material.CHAIN);
                h.scale(0.3f, 0.6f, 0.3f).interpolation(8, 0);
                cage.add(h);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null) return;
            int dur = config.getDurationTicks();
            World w = anchor.getWorld();
            if (w == null) return;

            // Phase progression
            if (t < dur * 0.55) phase = 1;       // sway
            else if (t < dur * 0.7) phase = 2;   // wind up spin
            else if (t < dur * 0.95) phase = 3;  // hurl
            else phase = 4;                       // impact / fade

            // Sway pendulum
            if (phase == 1 && t % 3 == 0) {
                float swing = (float) (Math.sin(t * 0.08) * Math.toRadians(30));
                for (BlockDisplayHandle h : cage)
                    h.animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(swing, 0, 0, 1),
                            new Vector3f(h.entity().getTransformation().getScale()), 3);
                for (BlockDisplayHandle h : bars)
                    h.animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(swing, 0, 0, 1),
                            new Vector3f(0.35f, 2.4f, 0.35f), 3);
                for (BlockDisplayHandle h : panes)
                    h.animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(swing, 0, 0, 1),
                            new Vector3f(1.6f, 1.6f, 0.15f), 3);
                // flicker core
                float s = 0.6f + (float) Math.sin(t * 0.4) * 0.15f;
                for (BlockDisplayHandle h : core)
                    h.animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s, s, s), 3);
            }
            if (phase == 2 && t % 2 == 0) {
                float spin = t * 0.6f;
                for (BlockDisplayHandle h : cage)
                    h.animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(spin, 0, 1, 0),
                            new Vector3f(h.entity().getTransformation().getScale()), 2);
            }
            if (phase == 3 && t % 2 == 0) {
                Player tgt = getTargetPlayer();
                if (tgt != null) {
                    hurlDir = tgt.getLocation().toVector().subtract(anchor.toVector()).setY(0);
                    if (hurlDir.lengthSquared() < 0.01) hurlDir = new Vector(1, 0, 0);
                    hurlDir.normalize();
                }
                float prog = (float) ((t - dur * 0.7) / (dur * 0.25));
                float dist = prog * 14f;
                float fx = (float) hurlDir.getX() * dist;
                float fz = (float) hurlDir.getZ() * dist;
                float spin = t * 0.8f;
                for (BlockDisplayHandle h : cage)
                    h.animateTo(new Vector3f(fx - 0.5f, -0.5f, fz - 0.5f),
                            new AxisAngle4f(spin, 0, 1, 0),
                            new Vector3f(h.entity().getTransformation().getScale()), 2);
                for (BlockDisplayHandle h : bars)
                    h.animateTo(new Vector3f(fx - 0.5f, -0.5f, fz - 0.5f),
                            new AxisAngle4f(spin, 0, 1, 0),
                            new Vector3f(0.35f, 2.4f, 0.35f), 2);
                for (BlockDisplayHandle h : panes)
                    h.animateTo(new Vector3f(fx - 0.5f, -0.5f, fz - 0.5f),
                            new AxisAngle4f(spin, 0, 1, 0),
                            new Vector3f(1.6f, 1.6f, 0.15f), 2);
                for (BlockDisplayHandle h : core)
                    h.animateTo(new Vector3f(fx - 0.5f, -0.5f, fz - 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 2);
                impactLoc = anchor.clone().add(hurlDir.clone().multiply(dist));
            }
            if (phase == 4 && t == (int) (dur * 0.95)) {
                if (impactLoc == null) impactLoc = anchor.clone();
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0f, 0.5f);
                triggerImpactDamage(impactLoc);
            }

            // Constant ember rain & creak
            if (t % 4 == 0) {
                Location lo = anchor.clone();
                w.spawnParticle(Particle.FLAME, lo, 6, 1.0, 0.4, 1.0, 0.02);
                w.spawnParticle(Particle.LAVA, lo.clone().add(0, -1, 0), 2, 0.6, 0.1, 0.6, 0.0);
                DisplayBuilder.dustParticles(lo, 5, 1.2, 180, 30, 30, 1.4f);
            }
            if (t % 30 == 0) DisplayBuilder.playSound(anchor, Sound.BLOCK_LANTERN_STEP, 0.7f, 0.4f);
            if (t % 50 == 0) DisplayBuilder.playSound(anchor, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.6f);
        }

        @Override public AbstractAttack newInstance() { return new CursedLantern(plugin); }
    }

    // ================================================================
    // 22. PLAGUE BLOSSOMS — 5 flowers w/ sequenced petal snaps.
    //     5 flowers x 9 displays each = 45 displays.
    // ================================================================
    public static class PlagueBlossoms extends BlockDisplayAttack {
        private final List<Flower> flowers = new ArrayList<>();
        private static class Flower {
            Location pos;
            BlockDisplayHandle stem, sculk, pollen;
            List<BlockDisplayHandle> petals = new ArrayList<>();
            int snapTick;
            boolean snapped = false;
        }

        public PlagueBlossoms(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plague_blossoms", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(8.3);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_WART_BLOCK_PLACE, 1.2f, 0.6f);
            int dur = config.getDurationTicks();
            for (int i = 0; i < 5; i++) {
                double angle = Math.PI * 2 * i / 5;
                double dist = 5 + Math.random() * 2;
                Location p = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                Flower f = new Flower();
                f.pos = p;
                // stem
                f.stem = displayBuilder.spawnBlock(p.clone(), Material.BLACKSTONE);
                f.stem.scale(0.4f, 1.6f, 0.4f).interpolation(8, 0);
                // sculk center
                f.sculk = displayBuilder.spawnBlock(p.clone().add(0, 1.6, 0), Material.SCULK);
                f.sculk.scale(0.9f, 0.9f, 0.9f).glow(50, 220, 200).interpolation(8, 0);
                // pollen
                f.pollen = displayBuilder.spawnBlock(p.clone().add(0, 2.3, 0), Material.NETHER_WART_BLOCK);
                f.pollen.scale(0.7f, 0.7f, 0.7f).interpolation(8, 0);
                // 6 petals
                for (int k = 0; k < 6; k++) {
                    double pa = Math.PI * 2 * k / 6;
                    Location pp = p.clone().add(Math.cos(pa) * 0.9, 1.6, Math.sin(pa) * 0.9);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(pp, Material.WARPED_PLANKS);
                    h.scale(0.6f, 0.2f, 0.9f).interpolation(10, 0);
                    h.rotate((float) pa, 0, 1, 0);
                    f.petals.add(h);
                }
                f.snapTick = 80 + (int) (Math.random() * (dur - 200));
                flowers.add(f);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null) return;
            World w = getCenter().getWorld();
            if (w == null) return;

            for (int idx = 0; idx < flowers.size(); idx++) {
                Flower f = flowers.get(idx);
                int local = t + idx * 17;
                // gentle open animation
                if (t < f.snapTick - 30 && t % 6 == 0) {
                    float open = (float) Math.toRadians(35 + Math.sin(local * 0.06) * 10);
                    for (int k = 0; k < f.petals.size(); k++) {
                        double pa = Math.PI * 2 * k / 6;
                        f.petals.get(k).animateTo(
                                new Vector3f((float) Math.cos(pa) * 0.9f - 0.5f, -0.5f, (float) Math.sin(pa) * 0.9f - 0.5f),
                                new AxisAngle4f(open, (float) -Math.sin(pa), 0, (float) Math.cos(pa)),
                                new Vector3f(0.6f, 0.2f, 0.9f), 6);
                    }
                    float s = 0.7f + (float) Math.sin(local * 0.2) * 0.15f;
                    f.pollen.animateTo(new Vector3f(-0.5f, 1.8f, -0.5f),
                            new AxisAngle4f(local * 0.3f, 0, 1, 0),
                            new Vector3f(s, s, s), 6);
                }
                // snap
                if (!f.snapped && t >= f.snapTick) {
                    f.snapped = true;
                    DisplayBuilder.playSound(f.pos, Sound.ENTITY_RAVAGER_ATTACK, 1.6f, 1.2f);
                    for (int k = 0; k < f.petals.size(); k++) {
                        double pa = Math.PI * 2 * k / 6;
                        f.petals.get(k).animateTo(
                                new Vector3f((float) Math.cos(pa) * 0.4f - 0.5f, 0.6f, (float) Math.sin(pa) * 0.4f - 0.5f),
                                new AxisAngle4f((float) Math.toRadians(110), (float) -Math.sin(pa), 0, (float) Math.cos(pa)),
                                new Vector3f(0.6f, 0.2f, 0.9f), 3);
                    }
                    w.spawnParticle(Particle.SCULK_SOUL, f.pos.clone().add(0, 1.6, 0), 30, 1, 1, 1, 0.05);
                }
            }
            // ambient particles
            if (t % 5 == 0) {
                for (Flower f : flowers) {
                    w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, f.pos.clone().add(0, 2.4, 0), 4, 0.4, 0.2, 0.4, 0.01);
                    DisplayBuilder.dustParticles(f.pos.clone().add(0, 1.6, 0), 3, 0.6, 50, 220, 80, 1.0f);
                }
            }
            if (t % 40 == 0)
                for (Flower f : flowers)
                    DisplayBuilder.playSound(f.pos, Sound.BLOCK_SCULK_HIT, 0.6f, 0.7f);
        }

        @Override public AbstractAttack newInstance() { return new PlagueBlossoms(plugin); }
    }

    // ================================================================
    // 23. VOID COLOSSEUM RING — ring of wall segments rises, contracts,
    //     overshoots. ~40 displays.
    // ================================================================
    public static class VoidColosseumRing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<BlockDisplayHandle> arches = new ArrayList<>();
        private final List<BlockDisplayHandle> battlements = new ArrayList<>();
        private final double[] segAngles = new double[8];

        public VoidColosseumRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_colosseum_ring", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(380);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_STEP, 1.6f, 0.4f);
            double R = 9.0;
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                segAngles[i] = a;
                Location wallBase = center.clone().add(Math.cos(a) * R, -2, Math.sin(a) * R);
                // wall segment (2 wide x 3 tall = 6 obsidian blocks)
                for (int wy = 0; wy < 3; wy++) {
                    for (int wx = -1; wx <= 0; wx++) {
                        // tangential offset
                        double tx = -Math.sin(a) * (wx + 0.5);
                        double tz = Math.cos(a) * (wx + 0.5);
                        Location p = wallBase.clone().add(tx, wy, tz);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.OBSIDIAN);
                        h.scale(0.95f, 0.95f, 0.95f).interpolation(20, 0);
                        walls.add(h);
                    }
                }
                // arch pillar between segments (blackstone bricks, 3 tall)
                double a2 = a + Math.PI / 8;
                Location archBase = center.clone().add(Math.cos(a2) * R, -2, Math.sin(a2) * R);
                for (int ay = 0; ay < 3; ay++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(archBase.clone().add(0, ay, 0),
                            Material.POLISHED_BLACKSTONE_BRICKS);
                    h.scale(0.5f, 0.95f, 0.5f).interpolation(20, 0);
                    arches.add(h);
                }
                // battlement on top (crying obsidian)
                BlockDisplayHandle b = displayBuilder.spawnBlock(wallBase.clone().add(0, 3, 0),
                        Material.CRYING_OBSIDIAN);
                b.scale(1.6f, 0.5f, 0.6f).glow(140, 0, 30).interpolation(20, 0);
                b.rotate((float) a, 0, 1, 0);
                battlements.add(b);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null) return;
            World w = getCenter().getWorld();
            if (w == null) return;
            int dur = config.getDurationTicks();
            double R = 9.0;

            // phases: 0–60 rise, 60–180 contract, 180–230 hold, 230–300 overshoot, 300+ collapse
            double scale;
            if (t < 60) scale = 1.0;
            else if (t < 180) scale = 1.0 - 0.5 * ((t - 60) / 120.0);
            else if (t < 230) scale = 0.5;
            else if (t < 300) scale = 0.5 + 0.9 * ((t - 230) / 70.0); // overshoot to 1.4
            else scale = 1.4 - 0.4 * Math.min(1.0, (t - 300) / 60.0);

            if (t % 4 == 0) {
                int wallIdx = 0;
                for (int i = 0; i < 8; i++) {
                    double a = segAngles[i];
                    double r = R * scale;
                    float fx = (float) (Math.cos(a) * r) - (float) (Math.cos(a) * R);
                    float fz = (float) (Math.sin(a) * r) - (float) (Math.sin(a) * R);
                    float fy = (t < 60) ? -2f + (t / 30f) : 0f;
                    for (int k = 0; k < 6; k++) {
                        BlockDisplayHandle h = walls.get(wallIdx++);
                        h.animateTo(new Vector3f(fx - 0.5f, fy - 0.5f, fz - 0.5f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.95f, 0.95f, 0.95f), 4);
                    }
                    // arch
                    double a2 = a + Math.PI / 8;
                    double ar = R * scale;
                    float ax = (float) (Math.cos(a2) * ar) - (float) (Math.cos(a2) * R);
                    float az = (float) (Math.sin(a2) * ar) - (float) (Math.sin(a2) * R);
                    for (int k = 0; k < 3; k++) {
                        arches.get(i * 3 + k).animateTo(
                                new Vector3f(ax - 0.5f, -0.5f, az - 0.5f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.5f, 0.95f, 0.5f), 4);
                    }
                    // battlement falls during overshoot
                    BlockDisplayHandle b = battlements.get(i);
                    float bx = (float) (Math.cos(a) * (R * scale)) - (float) (Math.cos(a) * R);
                    float bz = (float) (Math.sin(a) * (R * scale)) - (float) (Math.sin(a) * R);
                    float bs = (t > 280) ? Math.max(0f, 1.0f - (t - 280) / 30f) : 1.0f;
                    b.animateTo(new Vector3f(bx - 0.5f, -0.5f, bz - 0.5f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(1.6f * bs, 0.5f * bs, 0.6f * bs), 4);
                }
            }

            if (t == 230) DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);

            if (t % 3 == 0) {
                // void particles inside ring
                Location p = getCenter().clone().add(
                        (Math.random() - 0.5) * R, Math.random() * 3, (Math.random() - 0.5) * R);
                w.spawnParticle(Particle.PORTAL, p, 4, 0.2, 0.2, 0.2, 0.05);
                // stone dust falling from battlements
                for (int i = 0; i < 8; i++) {
                    double a = segAngles[i];
                    Location bp = getCenter().clone().add(Math.cos(a) * R, 3, Math.sin(a) * R);
                    DisplayBuilder.dustParticles(bp, 1, 0.4, 80, 80, 80, 1.2f);
                }
            }
            if (t % 35 == 0) DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_STEP, 1.0f, 0.3f);
        }

        @Override public AbstractAttack newInstance() { return new VoidColosseumRing(plugin); }
    }

    // ================================================================
    // 24. HELLFIRE MARIONETTE — hanging puppet, dangling limbs, snap.
    //     ~38 displays.
    // ================================================================
    public static class HellfireMarionette extends BlockDisplayAttack {
        private BlockDisplayHandle bar1, bar2;
        private final List<BlockDisplayHandle> torso = new ArrayList<>();
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final List<BlockDisplayHandle> limbA = new ArrayList<>();
        private final List<BlockDisplayHandle> limbB = new ArrayList<>();
        private final List<BlockDisplayHandle> limbC = new ArrayList<>();
        private final List<BlockDisplayHandle> limbD = new ArrayList<>();
        private final List<BlockDisplayHandle> strings = new ArrayList<>();
        private Location anchor;

        public HellfireMarionette(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_marionette", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(7.5);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(6);
            config.setDurationTicks(420);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            anchor = center.clone().add(0, 6, 0);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.5f, 0.6f);

            // handler bars (cross-bar): 2 perpendicular nether brick bars
            bar1 = displayBuilder.spawnBlock(anchor.clone(), Material.NETHER_BRICKS);
            bar1.scale(4.0f, 0.25f, 0.25f).interpolation(10, 0);
            bar2 = displayBuilder.spawnBlock(anchor.clone(), Material.NETHER_BRICKS);
            bar2.scale(0.25f, 0.25f, 4.0f).interpolation(10, 0);

            // torso 3x4 cluster
            for (int x = -1; x <= 1; x++)
                for (int y = -1; y <= 2; y++) {
                    Location p = anchor.clone().add(x * 0.5, -2 - y * 0.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                    h.scale(0.5f, 0.5f, 0.5f).interpolation(6, 0);
                    torso.add(h);
                }
            // head 3x3
            double[][] headOff = {{-0.5, -1.4, 0}, {0, -1.4, 0}, {0.5, -1.4, 0},
                                   {-0.5, -1.0, 0}, {0, -1.0, 0}, {0.5, -1.0, 0},
                                   {-0.5, -0.6, 0}, {0, -0.6, 0}, {0.5, -0.6, 0}};
            for (double[] o : headOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(anchor.clone().add(o[0], o[1], o[2]),
                        Material.BLACKSTONE);
                h.scale(0.45f, 0.45f, 0.45f).interpolation(6, 0);
                head.add(h);
            }
            // shroomlight eyes
            for (double[] o : new double[][]{{-0.3, -1.0, 0.25}, {0.3, -1.0, 0.25}}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(anchor.clone().add(o[0], o[1], o[2]),
                        Material.SHROOMLIGHT);
                h.scale(0.25f, 0.25f, 0.25f).glow(255, 80, 0).interpolation(6, 0);
                eyes.add(h);
            }
            // 4 limbs, each 3 segments of bone block
            buildLimb(limbA, anchor.clone().add(-0.8, -3.2, 0));
            buildLimb(limbB, anchor.clone().add(0.8, -3.2, 0));
            buildLimb(limbC, anchor.clone().add(-0.8, -4.4, 0));
            buildLimb(limbD, anchor.clone().add(0.8, -4.4, 0));
            // strings: nether brick connectors from bar to head/limbs
            for (double[] o : new double[][]{{-1.5, -0.8, 0}, {1.5, -0.8, 0},
                                              {-2.0, -3.2, 0}, {2.0, -3.2, 0}}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(anchor.clone().add(o[0], o[1], o[2]),
                        Material.NETHER_BRICKS);
                h.scale(0.1f, 1.5f, 0.1f).interpolation(6, 0);
                strings.add(h);
            }
        }

        private void buildLimb(List<BlockDisplayHandle> limb, Location origin) {
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone().add(0, -i * 0.8, 0),
                        Material.BONE_BLOCK);
                h.scale(0.4f, 0.7f, 0.4f).interpolation(6, 0);
                limb.add(h);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || anchor == null) return;
            World w = anchor.getWorld();
            if (w == null) return;
            int dur = config.getDurationTicks();

            // every 90 ticks: slack droop, then snap upright
            int cycle = t % 120;
            float droop = 0f;
            float snap = 0f;
            if (cycle < 80) droop = (float) Math.toRadians(Math.sin(t * 0.05) * 12);
            else if (cycle < 95) droop = (float) Math.toRadians(60); // slack droop
            else snap = (float) Math.toRadians(45); // snap upright outward

            if (t % 3 == 0) {
                // limbs swing with delayed motion
                animateLimb(limbA, droop + (float) Math.sin(t * 0.08) * 0.2f, snap);
                animateLimb(limbB, droop - (float) Math.sin(t * 0.08) * 0.2f, snap);
                animateLimb(limbC, droop + (float) Math.cos(t * 0.07) * 0.3f, snap);
                animateLimb(limbD, droop - (float) Math.cos(t * 0.07) * 0.3f, snap);
            }
            // bar slowly rotates
            if (t % 4 == 0) {
                bar1.animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f(t * 0.03f, 0, 1, 0),
                        new Vector3f(4.0f, 0.25f, 0.25f), 4);
                bar2.animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f(t * 0.03f, 0, 1, 0),
                        new Vector3f(0.25f, 0.25f, 4.0f), 4);
            }

            if (t % 4 == 0) {
                for (BlockDisplayHandle e : eyes)
                    w.spawnParticle(Particle.FLAME, e.entity().getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
                w.spawnParticle(Particle.SMALL_FLAME, anchor, 6, 1, 1, 1, 0.02);
                DisplayBuilder.dustParticles(anchor.clone().add(0, -2, 0), 4, 1.0, 140, 0, 30, 1.4f);
            }
            if (cycle == 95) DisplayBuilder.playSound(anchor, Sound.ENTITY_WITHER_AMBIENT, 1.4f, 1.2f);
            if (t % 35 == 0) DisplayBuilder.playSound(anchor, Sound.BLOCK_BONE_BLOCK_STEP, 0.8f, 0.5f);
            if (t % 80 == 0) DisplayBuilder.playSound(anchor, Sound.BLOCK_CHAIN_FALL, 1.0f, 0.7f);
        }

        private void animateLimb(List<BlockDisplayHandle> limb, float a, float snap) {
            for (int i = 0; i < limb.size(); i++) {
                float ang = a + i * 0.05f + snap;
                limb.get(i).animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f(ang, 0, 0, 1),
                        new Vector3f(0.4f, 0.7f, 0.4f), 3);
            }
        }

        @Override public AbstractAttack newInstance() { return new HellfireMarionette(plugin); }
    }

    // ================================================================
    // 25. DEVIL'S DREAMCATCHER — circle, web, feathers, then implodes.
    //     ~36 displays.
    // ================================================================
    public static class DevilsDreamcatcher extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ring = new ArrayList<>();
        private final List<BlockDisplayHandle> web = new ArrayList<>();
        private final List<BlockDisplayHandle> nodes = new ArrayList<>();
        private final List<BlockDisplayHandle> feathers = new ArrayList<>();
        private Location anchor;
        private boolean retracted = false;

        public DevilsDreamcatcher(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_dreamcatcher", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(30.0);
            config.setImpactRadius(11.2);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(360);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            anchor = center.clone().add(0, 5, 0);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.4f, 0.8f);

            // outer ring (16 blackstone)
            double R = 5.0;
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                Location p = anchor.clone().add(Math.cos(a) * R, 0, Math.sin(a) * R);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.6f, 0.6f, 0.6f).interpolation(8, 0);
                h.rotate((float) a, 0, 1, 0);
                ring.add(h);
            }
            // geometric spiral web (12 strand displays)
            for (int i = 0; i < 12; i++) {
                double tp = i / 12.0;
                double r = R * (1 - tp);
                double a = tp * Math.PI * 4;
                Location p = anchor.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(0.2f, 0.1f, 1.2f).glow(90, 0, 130).interpolation(8, 0);
                h.rotate((float) a, 0, 1, 0);
                web.add(h);
            }
            // shroomlight nodes at junctions (6)
            for (int i = 0; i < 6; i++) {
                double tp = (i + 0.5) / 6.0;
                double r = R * (1 - tp);
                double a = tp * Math.PI * 4;
                Location p = anchor.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SHROOMLIGHT);
                h.scale(0.3f, 0.3f, 0.3f).glow(255, 200, 100).interpolation(8, 0);
                nodes.add(h);
            }
            // 4 red glass feathers below
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location base = anchor.clone().add(Math.cos(a) * 2, -1.5, Math.sin(a) * 2);
                for (int s = 0; s < 3; s++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            base.clone().add(0, -s * 0.6, 0), Material.RED_STAINED_GLASS);
                    h.scale(0.15f, 0.7f, 0.4f).glow(180, 30, 30).interpolation(8, 0);
                    feathers.add(h);
                }
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || anchor == null) return;
            World w = anchor.getWorld();
            if (w == null) return;
            int dur = config.getDurationTicks();
            double R = 5.0;

            // Slow vertical-plane rotation
            if (t % 4 == 0) {
                float spin = t * 0.04f;
                for (int i = 0; i < ring.size(); i++) {
                    double a = Math.PI * 2 * i / 16 + spin;
                    float fx = (float) (Math.cos(a) * R);
                    float fz = (float) (Math.sin(a) * R);
                    ring.get(i).animateTo(new Vector3f(fx - 0.5f, -0.5f, fz - 0.5f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(0.6f, 0.6f, 0.6f), 4);
                }
                // web pulsing
                for (int i = 0; i < web.size(); i++) {
                    float s = 1.0f + (float) Math.sin(t * 0.15 + i) * 0.2f;
                    double tp = i / 12.0;
                    double r = R * (1 - tp);
                    double a = tp * Math.PI * 4 + spin;
                    float fx = (float) (Math.cos(a) * r);
                    float fz = (float) (Math.sin(a) * r);
                    web.get(i).animateTo(new Vector3f(fx - 0.5f, -0.5f, fz - 0.5f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(0.2f * s, 0.1f, 1.2f * s), 4);
                }
                // feathers sway
                for (int i = 0; i < feathers.size(); i++) {
                    float sway = (float) Math.sin(t * 0.1 + i * 0.5) * 0.3f;
                    feathers.get(i).animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(sway, 0, 0, 1),
                            new Vector3f(0.15f, 0.7f, 0.4f), 4);
                }
            }

            // retract trigger
            if (!retracted && t > dur * 0.7) {
                retracted = true;
                DisplayBuilder.playSound(anchor, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);
                for (int i = 0; i < web.size(); i++) {
                    web.get(i).animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.001f, 0.001f, 0.001f), 14);
                }
            }
            if (t == (int) (dur * 0.85)) {
                triggerImpactDamage(anchor);
                w.spawnParticle(Particle.SCULK_SOUL, anchor, 80, 2, 2, 2, 0.2);
                w.spawnParticle(Particle.EXPLOSION, anchor, 1, 0, 0, 0, 0);
                DisplayBuilder.playSound(anchor, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
            }

            if (t % 5 == 0) {
                w.spawnParticle(Particle.SCULK_SOUL, anchor, 5, R * 0.8, 0.3, R * 0.8, 0.02);
                for (BlockDisplayHandle f : feathers)
                    DisplayBuilder.dustParticles(f.entity().getLocation(), 1, 0.3, 180, 30, 30, 1.3f);
            }
            if (t % 25 == 0) DisplayBuilder.playSound(anchor, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.6f);
        }

        @Override public AbstractAttack newInstance() { return new DevilsDreamcatcher(plugin); }
    }

    // ================================================================
    // 26. VOID SERPENT SHED — flat S-curve skin that inflates and slithers.
    //     ~30 displays.
    // ================================================================
    public static class VoidSerpentShed extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> jaw = new ArrayList<>();
        private final List<BlockDisplayHandle> eye = new ArrayList<>();
        private final List<Vector> bodyOrigins = new ArrayList<>();

        public VoidSerpentShed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_serpent_shed", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(6.8);
            config.setDamageRadius(5.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(400);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_WARN, 1.6f, 0.5f);
            // 12-block S-curve along X with sinusoidal Z offset
            for (int i = 0; i < 12; i++) {
                double zOff = Math.sin(i * Math.PI / 4) * 1.6;
                Location p = center.clone().add(i - 6, 0.1, zOff);
                bodyOrigins.add(p.toVector().subtract(center.toVector()));
                // alternating sculk / obsidian pair (skin + scale)
                BlockDisplayHandle skin = displayBuilder.spawnBlock(p,
                        (i % 2 == 0) ? Material.SCULK : Material.OBSIDIAN);
                skin.scale(1.0f, 0.3f, 1.0f).interpolation(8, 0);
                body.add(skin);
                BlockDisplayHandle scale = displayBuilder.spawnBlock(p.clone().add(0, 0.2, 0),
                        (i % 2 == 0) ? Material.OBSIDIAN : Material.SCULK);
                scale.scale(0.8f, 0.2f, 0.8f).interpolation(8, 0);
                body.add(scale);
                bodyOrigins.add(p.toVector().subtract(center.toVector()).add(new Vector(0, 0.2, 0)));
            }
            // jaw at front (4 blackstone displays, tapered)
            for (int j = 0; j < 4; j++) {
                Location p = center.clone().add(7 + j * 0.5, 0.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.6f - j * 0.1f, 0.5f - j * 0.05f, 1.0f - j * 0.15f).interpolation(8, 0);
                jaw.add(h);
            }
            // eye (crying obsidian, 1 with glow)
            BlockDisplayHandle ey = displayBuilder.spawnBlock(center.clone().add(7, 0.4, 0.4),
                    Material.CRYING_OBSIDIAN);
            ey.scale(0.25f, 0.25f, 0.25f).glow(140, 0, 30).interpolation(8, 0);
            eye.add(ey);
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null) return;
            World w = getCenter().getWorld();
            if (w == null) return;
            int dur = config.getDurationTicks();

            // 100-tick cycles: 0-30 inflate, 30-70 slither, 70-90 deflate, 90-100 hold
            int cycle = t % 100;
            float inflate = 1.0f;
            if (cycle < 30) inflate = 1.0f + cycle / 30f * 0.6f;
            else if (cycle < 70) inflate = 1.6f;
            else if (cycle < 90) inflate = 1.6f - (cycle - 70) / 20f * 1.5f;
            else inflate = 0.1f;

            float slide = 0;
            if (cycle >= 30 && cycle < 70) slide = (cycle - 30) * 0.2f;

            if (t % 3 == 0) {
                for (int i = 0; i < body.size(); i++) {
                    Vector v = bodyOrigins.get(i);
                    float wiggle = (float) Math.sin(t * 0.15 + i * 0.4) * 0.4f;
                    body.get(i).animateTo(
                            new Vector3f((float) v.getX() + slide - 0.5f,
                                    (float) v.getY() - 0.5f,
                                    (float) v.getZ() + wiggle - 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.0f, 0.3f * inflate, 1.0f), 3);
                }
                for (int i = 0; i < jaw.size(); i++) {
                    jaw.get(i).animateTo(
                            new Vector3f(7f + i * 0.5f + slide - 0.5f, -0.3f, -0.5f),
                            new AxisAngle4f((float) Math.sin(t * 0.15) * 0.2f, 0, 1, 0),
                            new Vector3f(0.6f - i * 0.1f, 0.5f - i * 0.05f, 1.0f - i * 0.15f), 3);
                }
                eye.get(0).animateTo(new Vector3f(7f + slide - 0.5f, -0.1f, -0.1f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.25f * inflate, 0.25f * inflate, 0.25f * inflate), 3);
            }
            if (cycle == 30) DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_SPREAD, 1.6f, 0.7f);
            if (cycle == 70) DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GHAST_WARN, 1.4f, 0.8f);

            if (t % 4 == 0) {
                w.spawnParticle(Particle.SCULK_SOUL, getCenter().clone().add(slide, 0.3, 0), 6,
                        4, 0.2, 1, 0.02);
                w.spawnParticle(Particle.SMOKE,
                        getCenter().clone().add(7 + slide, 0.4, 0.4), 3, 0.1, 0.1, 0.1, 0.01);
            }
        }

        @Override public AbstractAttack newInstance() { return new VoidSerpentShed(plugin); }
    }

    // ================================================================
    // 27. BURNING THRONE — levitates, flips, launches fire orbs.
    //     ~38 displays.
    // ================================================================
    public static class BurningThrone extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> crown = new ArrayList<>();
        private final List<ItemDisplayHandle> orbs = new ArrayList<>();
        private final List<Vector> orbDirs = new ArrayList<>();
        private final List<Location> orbImpacts = new ArrayList<>();
        private boolean orbsLaunched = false;
        private Location anchor;

        public BurningThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("burning_throne", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(24.0);
            config.setImpactRadius(8.8);
            config.setDamage(6.8);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(6);
            config.setDurationTicks(440);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            anchor = center.clone();
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.8f, 0.4f);

            // seat (4x2 magma)
            for (int x = -1; x <= 2; x++)
                for (int z = -1; z <= 0; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            anchor.clone().add(x * 0.6, 0.6, z * 0.6), Material.MAGMA_BLOCK);
                    h.scale(0.6f, 0.4f, 0.6f).glow(255, 120, 30).interpolation(10, 0);
                    body.add(h);
                }
            // back panel (3 wide x 5 tall = 15 carved blackstone)
            for (int x = -1; x <= 1; x++)
                for (int y = 0; y < 5; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            anchor.clone().add(x * 0.6, 1.0 + y * 0.6, -0.7), Material.POLISHED_BLACKSTONE_BRICKS);
                    h.scale(0.6f, 0.6f, 0.3f).interpolation(10, 0);
                    body.add(h);
                }
            // armrests (2 + 2 = 4 nether brick)
            for (double[] o : new double[][]{{-1.0, 1.0, -0.2}, {-1.0, 1.4, -0.2},
                                              {2.0, 1.0, -0.2}, {2.0, 1.4, -0.2}}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(anchor.clone().add(o[0], o[1], o[2]),
                        Material.NETHER_BRICKS);
                h.scale(0.5f, 0.5f, 1.0f).interpolation(10, 0);
                body.add(h);
            }
            // base (3 blackstone)
            for (int x = 0; x < 3; x++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(anchor.clone().add((x - 1) * 0.6, 0.0, 0),
                        Material.BLACKSTONE);
                h.scale(0.6f, 0.4f, 1.4f).interpolation(10, 0);
                body.add(h);
            }
            // crown points (5 shroomlight along top of back panel)
            for (int x = 0; x < 5; x++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        anchor.clone().add(-1.0 + x * 0.6, 4.2, -0.7), Material.SHROOMLIGHT);
                h.scale(0.3f, 0.5f, 0.3f).glow(255, 150, 30).interpolation(10, 0);
                crown.add(h);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || anchor == null) return;
            World w = anchor.getWorld();
            if (w == null) return;
            int dur = config.getDurationTicks();

            // Phase 1 (0-100): glow + float
            // Phase 2 (100-260): slow rocking, then full 360 flip
            // Phase 3 (260-340): orb launches
            float lift = 0f;
            float roll = 0f;
            if (t < 100) lift = (float) (t / 100.0) * 1.2f;
            else if (t < 200) lift = 1.2f + (float) Math.sin((t - 100) * 0.05) * 0.4f;
            else if (t < 260) {
                lift = 1.5f;
                roll = (float) ((t - 200) / 60.0 * Math.PI * 2);
            } else lift = 1.0f;

            if (t % 3 == 0) {
                for (BlockDisplayHandle h : body) {
                    Vector3f sc = new Vector3f(h.entity().getTransformation().getScale());
                    h.animateTo(new Vector3f(-0.5f, -0.5f + lift, -0.5f),
                            new AxisAngle4f(roll, 1, 0, 0),
                            sc, 3);
                }
                for (BlockDisplayHandle h : crown)
                    h.animateTo(new Vector3f(-0.5f, -0.5f + lift, -0.5f),
                            new AxisAngle4f(roll, 1, 0, 0),
                            new Vector3f(0.3f, 0.5f, 0.3f), 3);
            }

            if (t == 200) DisplayBuilder.playSound(anchor, Sound.ENTITY_WITHER_AMBIENT, 1.6f, 0.7f);

            if (!orbsLaunched && t >= 260) {
                orbsLaunched = true;
                DisplayBuilder.playSound(anchor, Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.6f);
                for (int i = 0; i < 6; i++) {
                    double a = Math.PI * 2 * i / 6;
                    Vector dir = new Vector(Math.cos(a), 0.2, Math.sin(a));
                    ItemDisplayHandle orb = displayBuilder.spawnItem(
                            anchor.clone().add(0, 1.2, 0), new ItemStack(Material.MAGMA_CREAM));
                    orb.scale(0.7f, 0.7f, 0.7f).glow(255, 100, 0).interpolation(40, 0);
                    orbs.add(orb);
                    orbDirs.add(dir);
                    orbImpacts.add(anchor.clone().add(dir.clone().multiply(8)));
                }
            }
            if (orbsLaunched && t % 4 == 0) {
                int travel = t - 260;
                float prog = Math.min(1.0f, travel / 30f);
                for (int i = 0; i < orbs.size(); i++) {
                    Vector d = orbDirs.get(i).clone().multiply(8 * prog);
                    orbs.get(i).animateTo(new Vector3f((float) d.getX() - 0.5f, 1.2f - prog * 1.5f - 0.5f,
                                    (float) d.getZ() - 0.5f),
                            new AxisAngle4f(t * 0.3f, 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 4);
                }
            }
            if (t == 295) {
                for (Location p : orbImpacts) {
                    triggerImpactDamage(p);
                    w.spawnParticle(Particle.EXPLOSION, p, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.LAVA, p, 12, 1, 0.5, 1, 0);
                }
                DisplayBuilder.playSound(anchor, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.8f);
            }

            if (t % 3 == 0) {
                for (BlockDisplayHandle c : crown)
                    w.spawnParticle(Particle.FLAME, c.entity().getLocation(), 4, 0.2, 0.2, 0.2, 0.02);
                w.spawnParticle(Particle.LAVA, anchor.clone().add(0.5, 0.6, -0.3), 1, 0.8, 0.1, 0.4, 0);
                DisplayBuilder.dustParticles(anchor.clone().add(0, 1, -0.7), 4, 1.0, 255, 80, 30, 1.4f);
            }
            if (t % 30 == 0) DisplayBuilder.playSound(anchor, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.4f);
        }

        @Override public AbstractAttack newInstance() { return new BurningThrone(plugin); }
    }

    // ================================================================
    // 28. THORN MERIDIAN — vertical ring with 8 thorn nodes; switches axes.
    //     ~32 displays.
    // ================================================================
    public static class ThornMeridian extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringSegs = new ArrayList<>();
        private final List<BlockDisplayHandle> thorns = new ArrayList<>();
        private final List<BlockDisplayHandle> spineBase = new ArrayList<>();
        private Location anchor;

        public ThornMeridian(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thorn_meridian", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(10.5);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(420);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            anchor = center.clone().add(0, 4, 0);
            DisplayBuilder.playSound(center, Sound.BLOCK_WART_BLOCK_PLACE, 1.4f, 0.5f);

            double R = 4.0;
            // 16 ring segments (vertical XY plane initially)
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                Location p = anchor.clone().add(Math.cos(a) * R, Math.sin(a) * R, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRIMSON_PLANKS);
                h.scale(1.0f, 0.4f, 0.4f).interpolation(6, 0);
                h.rotate((float) a, 0, 0, 1);
                ringSegs.add(h);
            }
            // 8 thorn nodes outward
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = anchor.clone().add(Math.cos(a) * (R + 0.5), Math.sin(a) * (R + 0.5), 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.NETHER_WART_BLOCK);
                h.scale(0.7f, 0.7f, 0.7f).glow(140, 0, 30).interpolation(6, 0);
                thorns.add(h);
            }
            // 8 base spine blackstone (decorative)
            for (int i = 0; i < 8; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(i * 0.78) * 1.5, 0, Math.sin(i * 0.78) * 1.5),
                        Material.BLACKSTONE);
                h.scale(0.5f, 1.5f, 0.5f).interpolation(6, 0);
                spineBase.add(h);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || anchor == null) return;
            World w = anchor.getWorld();
            if (w == null) return;
            int dur = config.getDurationTicks();

            // axis switch at 50%
            boolean xAxis = t < dur * 0.5;
            float spin = t * 0.12f;
            double R = 4.0;

            if (t % 3 == 0) {
                for (int i = 0; i < ringSegs.size(); i++) {
                    double base = Math.PI * 2 * i / 16;
                    double a = base + spin;
                    float fx, fy, fz;
                    if (xAxis) {
                        fx = (float) (Math.cos(a) * R);
                        fy = (float) (Math.sin(a) * R);
                        fz = 0f;
                    } else {
                        fx = 0f;
                        fy = (float) (Math.cos(a) * R);
                        fz = (float) (Math.sin(a) * R);
                    }
                    ringSegs.get(i).animateTo(
                            new Vector3f(fx - 0.5f, fy - 0.5f, fz - 0.5f),
                            new AxisAngle4f((float) a, xAxis ? 0 : 1, xAxis ? 0 : 0, xAxis ? 1 : 0),
                            new Vector3f(1.0f, 0.4f, 0.4f), 3);
                }
                for (int i = 0; i < thorns.size(); i++) {
                    double base = Math.PI * 2 * i / 8;
                    double a = base + spin;
                    // extend when node is at outermost (cos>0)
                    float ext = (float) (0.7 + Math.max(0, Math.cos(a)) * 0.8);
                    float fx, fy, fz;
                    if (xAxis) {
                        fx = (float) (Math.cos(a) * (R + ext));
                        fy = (float) (Math.sin(a) * (R + ext));
                        fz = 0f;
                    } else {
                        fx = 0f;
                        fy = (float) (Math.cos(a) * (R + ext));
                        fz = (float) (Math.sin(a) * (R + ext));
                    }
                    thorns.get(i).animateTo(
                            new Vector3f(fx - 0.5f, fy - 0.5f, fz - 0.5f),
                            new AxisAngle4f((float) a, xAxis ? 0 : 1, xAxis ? 0 : 0, xAxis ? 1 : 0),
                            new Vector3f(0.5f + ext * 0.4f, 0.5f, 0.5f), 3);
                }
            }
            if (t == (int) (dur * 0.5))
                DisplayBuilder.playSound(anchor, Sound.ENTITY_RAVAGER_ATTACK, 2.0f, 0.7f);

            if (t % 4 == 0) {
                for (BlockDisplayHandle h : thorns)
                    w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, h.entity().getLocation(), 3, 0.2, 0.2, 0.2, 0.02);
                DisplayBuilder.dustParticles(anchor, 6, 3, 200, 0, 50, 1.4f);
            }
            if (t % 24 == 0)
                DisplayBuilder.playSound(anchor, Sound.BLOCK_WART_BLOCK_PLACE, 0.8f, 0.5f);
        }

        @Override public AbstractAttack newInstance() { return new ThornMeridian(plugin); }
    }

    // ================================================================
    // 29. THE CARRION WHEEL — rolls, rears, slams forward.
    //     ~38 displays.
    // ================================================================
    public static class CarrionWheel extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> rim = new ArrayList<>();
        private final List<BlockDisplayHandle> spokes = new ArrayList<>();
        private final List<BlockDisplayHandle> hub = new ArrayList<>();
        private final List<BlockDisplayHandle> flesh = new ArrayList<>();
        private Location anchor;
        private Vector rollDir = new Vector(1, 0, 0);
        private Location slamLoc;
        private boolean slammed = false;

        public CarrionWheel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("carrion_wheel", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(25.5);
            config.setImpactRadius(9.6);
            config.setDamage(7.5);
            config.setDamageRadius(5.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(360);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            anchor = center.clone().add(0, 4, 0);
            DisplayBuilder.playSound(center, Sound.BLOCK_BONE_BLOCK_STEP, 1.6f, 0.5f);

            double R = 4.0;
            // 16 rim segments (vertical wheel — XY plane)
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                Location p = anchor.clone().add(Math.cos(a) * R, Math.sin(a) * R, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.7f, 0.7f, 0.4f).interpolation(6, 0);
                h.rotate((float) a, 0, 0, 1);
                rim.add(h);
            }
            // 8 spokes (bone block, radial)
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = anchor.clone().add(Math.cos(a) * R / 2, Math.sin(a) * R / 2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BONE_BLOCK);
                h.scale((float) R, 0.25f, 0.25f).interpolation(6, 0);
                h.rotate((float) a, 0, 0, 1);
                spokes.add(h);
            }
            // 4 hub deepslate
            for (double[] o : new double[][]{{-0.3, -0.3, 0}, {0.3, -0.3, 0}, {-0.3, 0.3, 0}, {0.3, 0.3, 0}}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(anchor.clone().add(o[0], o[1], o[2]),
                        Material.DEEPSLATE_TILES);
                h.scale(0.6f, 0.6f, 0.6f).interpolation(6, 0);
                hub.add(h);
            }
            // 6 hanging "flesh" red glass
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = anchor.clone().add(Math.cos(a) * R, Math.sin(a) * R, 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.RED_STAINED_GLASS);
                h.scale(0.3f, 1.2f, 0.1f).glow(180, 30, 30).interpolation(6, 0);
                flesh.add(h);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || anchor == null) return;
            World w = anchor.getWorld();
            if (w == null) return;
            int dur = config.getDurationTicks();
            double R = 4.0;

            // pick roll dir toward target
            if (t == 30) {
                Player tgt = getTargetPlayer();
                if (tgt != null) {
                    Vector v = tgt.getLocation().toVector().subtract(anchor.toVector()).setY(0);
                    if (v.lengthSquared() > 0.01) rollDir = v.normalize();
                }
            }
            float prog = Math.min(1.0f, t / (dur * 0.7f));
            float rollDist = prog * 14f;
            float ox = (float) rollDir.getX() * rollDist;
            float oz = (float) rollDir.getZ() * rollDist;
            float spin = -t * 0.18f;

            // rear at 75%
            float lean = 0f;
            if (t > dur * 0.75 && t < dur * 0.85)
                lean = (float) Math.toRadians(45 * Math.sin((t - dur * 0.75) / (dur * 0.1) * Math.PI));

            if (t % 2 == 0) {
                for (int i = 0; i < rim.size(); i++) {
                    double a = Math.PI * 2 * i / 16 + spin;
                    float fx = (float) (Math.cos(a) * R);
                    float fy = (float) (Math.sin(a) * R);
                    rim.get(i).animateTo(
                            new Vector3f(fx + ox - 0.5f, fy - 0.5f, oz - 0.5f),
                            new AxisAngle4f(lean, 1, 0, 0),
                            new Vector3f(0.7f, 0.7f, 0.4f), 2);
                }
                for (int i = 0; i < spokes.size(); i++) {
                    double a = Math.PI * 2 * i / 8 + spin;
                    spokes.get(i).animateTo(
                            new Vector3f(ox - 0.5f, -0.5f, oz - 0.5f),
                            new AxisAngle4f((float) a, 0, 0, 1),
                            new Vector3f((float) R, 0.25f, 0.25f), 2);
                }
                for (int i = 0; i < hub.size(); i++) {
                    Vector3f sc = new Vector3f(hub.get(i).entity().getTransformation().getScale());
                    hub.get(i).animateTo(
                            new Vector3f(ox - 0.5f, -0.5f, oz - 0.5f),
                            new AxisAngle4f(spin, 0, 0, 1),
                            sc, 2);
                }
                for (int i = 0; i < flesh.size(); i++) {
                    double a = Math.PI * 2 * i / 6 + spin;
                    float fx = (float) (Math.cos(a) * R);
                    float fy = (float) (Math.sin(a) * R);
                    float flap = (float) Math.sin(t * 0.3 + i) * 0.4f;
                    flesh.get(i).animateTo(
                            new Vector3f(fx + ox - 0.5f, fy - 0.5f, oz - 0.5f + flap),
                            new AxisAngle4f((float) a + flap, 0, 0, 1),
                            new Vector3f(0.3f, 1.2f, 0.1f), 2);
                }
            }
            // slam impact at 88%
            if (!slammed && t > dur * 0.85) {
                slammed = true;
                slamLoc = anchor.clone().add(rollDir.clone().multiply(rollDist + 2));
                triggerImpactDamage(slamLoc);
                DisplayBuilder.playSound(slamLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 0.5f);
                w.spawnParticle(Particle.EXPLOSION, slamLoc, 2, 0.5, 0.5, 0.5, 0);
                w.spawnParticle(Particle.BLOCK, slamLoc, 30, 2, 0.2, 2, 0,
                        Material.BLACKSTONE.createBlockData());
            }

            if (t % 5 == 0) {
                Location lo = anchor.clone().add(ox, 0, oz);
                w.spawnParticle(Particle.WHITE_ASH, lo, 6, 1, 1, 1, 0.05);
                DisplayBuilder.dustParticles(lo, 4, 1.5, 200, 0, 50, 1.3f);
            }
            if (t % 12 == 0)
                DisplayBuilder.playSound(anchor.clone().add(ox, 0, oz),
                        Sound.BLOCK_BONE_BLOCK_STEP, 1.0f, 0.7f);
        }

        @Override public AbstractAttack newInstance() { return new CarrionWheel(plugin); }
    }

    // ================================================================
    // 30. THE UNDERTAKER'S SHOVEL — lifts, plunges, deposits.
    //     ~32 displays.
    // ================================================================
    public static class UndertakersShovel extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> blade = new ArrayList<>();
        private final List<BlockDisplayHandle> shaft = new ArrayList<>();
        private final List<BlockDisplayHandle> grip = new ArrayList<>();
        private Location anchor;
        private boolean plunged = false;
        private Location plungeLoc;

        public UndertakersShovel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("undertakers_shovel", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(28.5);
            config.setImpactRadius(9.6);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(6);
            config.setDurationTicks(420);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            anchor = center.clone().add(0, 1, 0);
            DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_HIT, 1.4f, 0.5f);

            // blade — 5 wide x 4 tall trapezoid, plus a back-layer for thickness, blackstone bricks
            // Layout: row 0 (top): 5 wide, narrowing through 4,4,3 (16 front faces)
            int[] widths = {5, 5, 4, 4};
            for (int row = 0; row < 4; row++) {
                int width = widths[row];
                for (int x = 0; x < width; x++) {
                    double dx = (x - (width - 1) / 2.0) * 0.6;
                    // front face
                    BlockDisplayHandle hf = displayBuilder.spawnBlock(
                            anchor.clone().add(dx, -row * 0.5, -2),
                            Material.POLISHED_BLACKSTONE_BRICKS);
                    hf.scale(0.6f, 0.5f, 0.2f).interpolation(8, 0);
                    blade.add(hf);
                }
            }
            // 4 reinforcement displays (deepslate brick edge)
            for (double[] o : new double[][]{{-1.5, 0.0, -2}, {1.5, 0.0, -2}, {-1.5, -1.5, -2}, {1.5, -1.5, -2}}) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(anchor.clone().add(o[0], o[1], o[2]),
                        Material.DEEPSLATE_BRICKS);
                h.scale(0.4f, 0.4f, 0.25f).interpolation(8, 0);
                blade.add(h);
            }
            // shaft — 6 polished deepslate segments tapering toward grip
            for (int i = 0; i < 6; i++) {
                Location p = anchor.clone().add(0, 0, -2 + 0.7 + i * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_DEEPSLATE);
                h.scale(0.35f - i * 0.02f, 0.35f - i * 0.02f, 0.7f).interpolation(8, 0);
                shaft.add(h);
            }
            // grip — 3 bone blocks
            for (int i = 0; i < 3; i++) {
                Location p = anchor.clone().add(0, 0, 2.5 + i * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BONE_BLOCK);
                h.scale(0.5f, 0.5f, 0.4f).interpolation(8, 0);
                grip.add(h);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || anchor == null) return;
            World w = anchor.getWorld();
            if (w == null) return;
            int dur = config.getDurationTicks();

            // Phases:
            //  0-100   : lift to vertical  (rotate from 0 → 90° on X)
            //  100-160 : pause vertical
            //  160-260 : swing forward arc, plunge (rotate from 90° back → -45° on X, translate toward target)
            //  260-340 : lift with "dirt"
            //  340-420 : tip and deposit
            float ang = 0f;
            float ty = 0f;
            float fwd = 0f;

            if (t < 100) ang = (float) Math.toRadians(t * 0.9);
            else if (t < 160) ang = (float) Math.toRadians(90);
            else if (t < 260) {
                float p = (t - 160) / 100f;
                ang = (float) Math.toRadians(90 - p * 135);
                fwd = p * 4f;
                ty = -p * 1.2f;
            } else if (t < 340) {
                float p = (t - 260) / 80f;
                ang = (float) Math.toRadians(-45 + p * 45);
                fwd = 4f;
                ty = -1.2f + p * 2.0f + (float) Math.sin(p * 6) * 0.15f;
            } else {
                float p = (t - 340) / 80f;
                ang = (float) Math.toRadians(0 + p * 70);
                fwd = 4f - p * 2;
                ty = 0.8f - p * 0.3f;
            }

            // Single rotation pivot at the front center; we apply rotation to everything around shaft axis
            if (t % 2 == 0) {
                // Build rotated transforms relative to anchor
                applyShovelTransform(blade, ang, ty, fwd, 4);
                applyShovelTransform(shaft, ang, ty, fwd, 4);
                applyShovelTransform(grip, ang, ty, fwd, 4);
            }

            // plunge impact
            if (!plunged && t == 260) {
                plunged = true;
                plungeLoc = anchor.clone().add(0, ty, fwd - 2);
                triggerImpactDamage(plungeLoc);
                DisplayBuilder.playSound(plungeLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 0.4f);
                w.spawnParticle(Particle.EXPLOSION, plungeLoc, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.BLOCK, plungeLoc, 40, 2, 0.5, 2, 0,
                        Material.DIRT.createBlockData());
            }

            // ambient particles
            if (t % 3 == 0) {
                if (t > 260 && t < 340) {
                    Location lift = anchor.clone().add(0, ty, fwd - 2);
                    w.spawnParticle(Particle.BLOCK, lift, 5, 0.5, 0.3, 0.5, 0,
                            Material.SOUL_SAND.createBlockData());
                    w.spawnParticle(Particle.ASH, lift, 6, 0.6, 0.6, 0.6, 0.02);
                }
                if (t > 340) {
                    Location dep = anchor.clone().add(0, 0.2, fwd - 2);
                    w.spawnParticle(Particle.ASH, dep, 8, 1, 0.5, 1, 0.05);
                    DisplayBuilder.playSound(dep, Sound.BLOCK_SOUL_SAND_STEP, 0.5f, 0.7f);
                }
            }
            if (t == 30) DisplayBuilder.playSound(anchor, Sound.BLOCK_GRAVEL_HIT, 1.0f, 0.6f);
            if (t % 40 == 0) DisplayBuilder.playSound(anchor, Sound.BLOCK_GRAVEL_BREAK, 0.8f, 0.5f);
        }

        private void applyShovelTransform(List<BlockDisplayHandle> parts, float ang, float ty, float fwd, int dur) {
            for (BlockDisplayHandle h : parts) {
                Vector3f sc = new Vector3f(h.entity().getTransformation().getScale());
                Vector3f tr = h.entity().getTransformation().getTranslation();
                // Keep their original local layout (already baked into spawn position).
                // Apply pivot rotation around X axis at anchor by composing translation.
                h.animateTo(new Vector3f(tr.x, tr.y + ty, tr.z + fwd),
                        new AxisAngle4f(ang, 1, 0, 0),
                        sc, dur);
            }
        }

        @Override public AbstractAttack newInstance() { return new UndertakersShovel(plugin); }
    }
}
