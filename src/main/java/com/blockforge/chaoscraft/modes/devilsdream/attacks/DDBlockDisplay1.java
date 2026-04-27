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
 * DevilsDream Mode — BlockDisplay Attacks Set 1 (10 nightmare/demonic structures).
 *
 * Each structure uses 25+ block/item display entities, smooth interpolated
 * animation, multi-phase lifecycle (spawn -> active -> dissipate), and
 * thematic particle/sound design.
 */
public final class DDBlockDisplay1 {
    private DDBlockDisplay1() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SinnersCompass(plugin));
        registry.register(new CollapsedStar(plugin));
        registry.register(new SkinwalkerGait(plugin));
        registry.register(new GlassConfessional(plugin));
        registry.register(new BlackTide(plugin));
        registry.register(new RibCageTrap(plugin));
        registry.register(new Pendulum(plugin));
        registry.register(new SearingTetrad(plugin));
        registry.register(new LamentPillar(plugin));
        registry.register(new DreamingClock(plugin));
    }

    // ================================================================
    // 1. SINNER'S COMPASS — 32 displays
    //    Floor compass rose: 4 diamond needle arms (NSEW), 16 dial ticks,
    //    center medallion, 4 cardinal blocks. Spins, periodically locks.
    // ================================================================
    public static class SinnersCompass extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> dialTicks = new ArrayList<>();
        private final List<BlockDisplayHandle> needleN = new ArrayList<>();
        private final List<BlockDisplayHandle> needleE = new ArrayList<>();
        private final List<BlockDisplayHandle> needleS = new ArrayList<>();
        private final List<BlockDisplayHandle> needleW = new ArrayList<>();
        private final List<BlockDisplayHandle> cardinals = new ArrayList<>();
        private final List<BlockDisplayHandle> medallion = new ArrayList<>();
        private double currentRotation = 0;
        private double lockedRotation = 0;
        private boolean locked = false;
        private int lockTimer = 0;

        public SinnersCompass(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sinners_compass", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(8.3);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(360);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.5f);

            // 16 dial tick marks around outer ring (radius 6)
            for (int i = 0; i < 16; i++) {
                double angle = Math.PI * 2 * i / 16;
                Location p = center.clone().add(Math.cos(angle) * 6, 0.1, Math.sin(angle) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(0.01f, 0.01f, 0.01f).glow(140, 0, 30).interpolation(15, i);
                h.animateTo(new Vector3f(-0.2f, 0.1f, -0.2f),
                        new AxisAngle4f((float) angle, 0, 1, 0),
                        new Vector3f(0.4f, 0.25f, 0.4f), 15);
                dialTicks.add(h);
            }

            // 4 needle arms (each 6 tapering blocks) — N, E, S, W
            spawnNeedle(center, needleN, 0);
            spawnNeedle(center, needleE, Math.PI / 2);
            spawnNeedle(center, needleS, Math.PI);
            spawnNeedle(center, needleW, 3 * Math.PI / 2);

            // 4 cardinal point blocks (red nether brick, just outside ticks)
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI / 2;
                Location p = center.clone().add(Math.cos(angle) * 7, 0.15, Math.sin(angle) * 7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.RED_NETHER_BRICKS);
                h.scale(0.01f, 0.01f, 0.01f).glow(255, 60, 0).interpolation(20, 4);
                h.animateTo(new Vector3f(-0.4f, 0.15f, -0.4f),
                        new AxisAngle4f((float) angle, 0, 1, 0),
                        new Vector3f(0.8f, 0.5f, 0.8f), 20);
                cardinals.add(h);
            }

            // Center medallion cluster — 4 shroomlight + 1 center crying obsidian
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI / 2 + Math.PI / 4;
                Location p = center.clone().add(Math.cos(angle) * 0.6, 0.25, Math.sin(angle) * 0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SHROOMLIGHT);
                h.scale(0.01f, 0.01f, 0.01f).glow(50, 220, 200).interpolation(15, 6);
                h.animateTo(new Vector3f(-0.2f, 0.25f, -0.2f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.4f, 0.4f, 0.4f), 15);
                medallion.add(h);
            }
            BlockDisplayHandle hub = displayBuilder.spawnBlock(center.clone().add(0, 0.3, 0),
                    Material.CRYING_OBSIDIAN);
            hub.scale(0.6f, 0.4f, 0.6f).glow(140, 0, 30).interpolation(10, 0);
            medallion.add(hub);
        }

        private void spawnNeedle(Location center, List<BlockDisplayHandle> list, double dirAngle) {
            // 6 tapering blackstone slab segments along this direction.
            double cos = Math.cos(dirAngle);
            double sin = Math.sin(dirAngle);
            for (int i = 0; i < 6; i++) {
                double dist = 0.6 + i * 0.85;
                Location p = center.clone().add(cos * dist, 0.2, sin * dist);
                Material mat = (i == 5) ? Material.RED_NETHER_BRICKS : Material.POLISHED_BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                float taper = 0.85f - i * 0.1f;
                h.scale(0.01f, 0.01f, 0.01f).glow(140, 0, 30).interpolation(18, i + 2);
                h.animateTo(new Vector3f(-taper / 2f, 0.2f, -taper / 2f),
                        new AxisAngle4f((float) dirAngle, 0, 1, 0),
                        new Vector3f(taper, 0.35f, taper * 0.45f), 18);
                list.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            int phase = (tick < 25) ? 0 : (tick > dur - 30) ? 2 : 1;

            // Phase 1: spinning + periodic lock
            if (phase == 1) {
                if (locked) {
                    lockTimer++;
                    if (lockTimer > 20) { locked = false; lockTimer = 0; }
                } else {
                    currentRotation += 0.12;
                    if (tick % 60 == 0) {
                        locked = true;
                        lockedRotation = currentRotation + (Math.random() * Math.PI * 0.5);
                        currentRotation = lockedRotation;
                        DisplayBuilder.playSound(getCenter(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.4f, 0.7f);
                        // Cardinal fire bursts
                        for (int i = 0; i < 4; i++) {
                            double a = currentRotation + i * Math.PI / 2;
                            Location burst = getCenter().clone().add(Math.cos(a) * 7, 0.5, Math.sin(a) * 7);
                            w.spawnParticle(Particle.FLAME, burst, 18, 0.5, 0.4, 0.5, 0.05);
                            w.spawnParticle(Particle.LAVA, burst, 4, 0.3, 0.2, 0.3, 0.02);
                        }
                    }
                }

                // Update needle rotations (interpolate to new angle)
                if (tick % 4 == 0) {
                    rotateNeedle(needleN, currentRotation, locked);
                    rotateNeedle(needleE, currentRotation + Math.PI / 2, locked);
                    rotateNeedle(needleS, currentRotation + Math.PI, locked);
                    rotateNeedle(needleW, currentRotation + 3 * Math.PI / 2, locked);
                }

                // Crying obsidian drips on dial rim
                if (tick % 3 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.random() * Math.PI * 2;
                        Location drip = getCenter().clone().add(Math.cos(a) * 6, 0.3, Math.sin(a) * 6);
                        w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, drip, 1, 0.1, 0.05, 0.1, 0);
                    }
                }
                if (tick % 5 == 0) {
                    // Red dust trace on needle tips
                    for (int i = 0; i < 4; i++) {
                        double a = currentRotation + i * Math.PI / 2;
                        Location tip = getCenter().clone().add(Math.cos(a) * 5.5, 0.4, Math.sin(a) * 5.5);
                        DisplayBuilder.dustParticles(tip, 3, 0.3, 200, 20, 30, 1.4f);
                    }
                }
                if (tick % 8 == 0) {
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CHAIN_FALL, 0.6f,
                            0.7f + (float) Math.random() * 0.3f);
                }
            }
            // Phase 2: dissipate — fire ring outward (lightning bolt impact + needle scatter)
            else if (phase == 2 && tick == dur - 30) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.6f, 0.7f);
                w.spawnParticle(Particle.FLASH, getCenter().clone().add(0, 1, 0), 1, 0, 0, 0, 0);
                // Animate ticks outward
                for (int i = 0; i < dialTicks.size(); i++) {
                    double a = Math.PI * 2 * i / 16;
                    dialTicks.get(i).animateTo(
                            new Vector3f((float) (Math.cos(a) * 6 - 0.2f), 0.1f, (float) (Math.sin(a) * 6 - 0.2f)),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(0.01f, 0.01f, 0.01f), 28);
                }
                particleRingFire(w, getCenter(), 6, 32);
            }
        }

        private void rotateNeedle(List<BlockDisplayHandle> needle, double angle, boolean locked) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            float extension = locked ? 2f : 0f;
            for (int i = 0; i < needle.size(); i++) {
                double dist = 0.6 + i * 0.85 + (extension * (i / 5.0));
                float taper = 0.85f - i * 0.1f;
                needle.get(i).animateTo(
                        new Vector3f((float) (cos * dist) - taper / 2f, 0.2f, (float) (sin * dist) - taper / 2f),
                        new AxisAngle4f((float) angle, 0, 1, 0),
                        new Vector3f(taper, 0.35f, taper * 0.45f), 4);
            }
        }

        private void particleRingFire(World w, Location c, double r, int points) {
            for (int i = 0; i < points; i++) {
                double a = Math.PI * 2 * i / points;
                Location p = c.clone().add(Math.cos(a) * r, 0.5, Math.sin(a) * r);
                w.spawnParticle(Particle.FLAME, p, 4, 0.2, 0.3, 0.2, 0.04);
                w.spawnParticle(Particle.LAVA, p, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        @Override public AbstractAttack newInstance() { return new SinnersCompass(plugin); }
    }

    // ================================================================
    // 2. COLLAPSED STAR — 34 displays
    //    Sphere core + 8 spike clusters + calcite halo + end-stone debris.
    //    Implodes, then explodes outward overshooting.
    // ================================================================
    public static class CollapsedStar extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> core = new ArrayList<>();
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private final List<BlockDisplayHandle> halo = new ArrayList<>();
        private final List<BlockDisplayHandle> debris = new ArrayList<>();
        private final List<double[]> spikeDirs = new ArrayList<>();
        private final List<double[]> debrisOffsets = new ArrayList<>();

        public CollapsedStar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("collapsed_star", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(280);
            config.setCooldownTicks(330);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);

            // Sphere core — 14 blackstone displays in a tight cluster
            double[][] coreOffsets = {
                {0,2,0},{0,3.2,0},{0,1.4,0},
                {0.9,2.3,0},{-0.9,2.3,0},{0,2.3,0.9},{0,2.3,-0.9},
                {0.6,2.8,0.6},{-0.6,2.8,-0.6},{0.6,1.7,-0.6},{-0.6,1.7,0.6},
                {0.4,2.5,0.4},{-0.4,2.0,0.4},{0,2.5,-0.4}
            };
            for (double[] o : coreOffsets) {
                Location p = center.clone().add(o[0], o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.55f, 0.55f, 0.55f).glow(90, 0, 130).interpolation(12, 0);
                core.add(h);
            }

            // 8 spike clusters at varied angles (amethyst spikes)
            for (int i = 0; i < 8; i++) {
                double yaw = Math.PI * 2 * i / 8 + (Math.random() - 0.5) * 0.4;
                double pitch = (Math.random() - 0.5) * Math.PI * 0.6;
                double dx = Math.cos(yaw) * Math.cos(pitch);
                double dy = Math.sin(pitch);
                double dz = Math.sin(yaw) * Math.cos(pitch);
                spikeDirs.add(new double[]{dx, dy, dz});
                Location p = center.clone().add(dx * 2.3, 2.3 + dy * 2.3, dz * 2.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(180, 100, 255).interpolation(14, 0);
                spikes.add(h);
            }

            // Calcite halo — 6 ring blocks
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = center.clone().add(Math.cos(a) * 3.5, 2.3, Math.sin(a) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CALCITE);
                h.scale(0.45f, 0.3f, 0.45f).glow(240, 230, 200).interpolation(16, 0);
                halo.add(h);
            }

            // 6 end-stone debris pieces in loose scatter
            for (int i = 0; i < 6; i++) {
                double dx = (Math.random() - 0.5) * 6;
                double dy = (Math.random() - 0.5) * 4 + 2.3;
                double dz = (Math.random() - 0.5) * 6;
                debrisOffsets.add(new double[]{dx, dy, dz});
                Location p = center.clone().add(dx, dy, dz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.END_STONE_BRICKS);
                h.scale(0.35f, 0.35f, 0.35f).glow(240, 230, 180).interpolation(14, 0);
                debris.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            int cycleLen = 80;
            int cyclePhase = tick % cycleLen;

            // Phase A: implosion (0-30) — everything translates to center
            if (cyclePhase == 0 && tick < dur - 40) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDER_DRAGON_HURT, 1.2f, 0.5f);
                for (BlockDisplayHandle h : core) {
                    h.animateTo(new Vector3f(-0.275f, 2.3f, -0.275f),
                            new AxisAngle4f((float) Math.random(), 0, 1, 0),
                            new Vector3f(0.1f, 0.1f, 0.1f), 30);
                }
                for (BlockDisplayHandle h : spikes) {
                    h.animateTo(new Vector3f(-0.2f, 2.3f, -0.2f),
                            new AxisAngle4f((float) Math.random(), 1, 1, 0),
                            new Vector3f(0.1f, 0.1f, 0.1f), 30);
                }
                for (BlockDisplayHandle h : halo) {
                    h.animateTo(new Vector3f(-0.225f, 2.3f, -0.225f),
                            new AxisAngle4f((float) Math.random(), 0, 1, 0),
                            new Vector3f(0.1f, 0.05f, 0.1f), 30);
                }
                for (BlockDisplayHandle h : debris) {
                    h.animateTo(new Vector3f(-0.175f, 2.3f, -0.175f),
                            new AxisAngle4f((float) Math.random() * 6, 1, 1, 0),
                            new Vector3f(0.1f, 0.1f, 0.1f), 30);
                }
            }

            // Phase B: explosion overshoot (32) — everything shoots out past origin
            if (cyclePhase == 32 && tick < dur - 40) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.5f);
                w.spawnParticle(Particle.FLASH, getCenter().clone().add(0, 2.3, 0), 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, getCenter().clone().add(0, 2.3, 0), 1, 0, 0, 0, 0);

                double[][] coreOffsets = {
                    {0,2,0},{0,3.2,0},{0,1.4,0},
                    {0.9,2.3,0},{-0.9,2.3,0},{0,2.3,0.9},{0,2.3,-0.9},
                    {0.6,2.8,0.6},{-0.6,2.8,-0.6},{0.6,1.7,-0.6},{-0.6,1.7,0.6},
                    {0.4,2.5,0.4},{-0.4,2.0,0.4},{0,2.5,-0.4}
                };
                for (int i = 0; i < core.size(); i++) {
                    double[] o = coreOffsets[i];
                    float ox = (float) (o[0] * 1.5);
                    float oy = (float) (o[1]);
                    float oz = (float) (o[2] * 1.5);
                    core.get(i).animateTo(new Vector3f(ox - 0.275f, oy, oz - 0.275f),
                            new AxisAngle4f((float) (tick * 0.1), 1, 1, 0),
                            new Vector3f(0.55f, 0.55f, 0.55f), 18);
                }
                for (int i = 0; i < spikes.size(); i++) {
                    double[] d = spikeDirs.get(i);
                    float ox = (float) (d[0] * 4.5);
                    float oy = (float) (2.3 + d[1] * 4.5);
                    float oz = (float) (d[2] * 4.5);
                    spikes.get(i).animateTo(new Vector3f(ox - 0.2f, oy, oz - 0.2f),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 2), 1, 0, 1),
                            new Vector3f(0.4f, 0.4f, 0.4f), 18);
                }
                for (int i = 0; i < halo.size(); i++) {
                    double a = Math.PI * 2 * i / halo.size();
                    halo.get(i).animateTo(new Vector3f((float) (Math.cos(a) * 5.5 - 0.225), 2.3f,
                            (float) (Math.sin(a) * 5.5 - 0.225)),
                            new AxisAngle4f((float) (tick * 0.05), 0, 1, 0),
                            new Vector3f(0.45f, 0.3f, 0.45f), 18);
                }
                for (int i = 0; i < debris.size(); i++) {
                    double[] o = debrisOffsets.get(i);
                    debris.get(i).animateTo(new Vector3f(
                            (float) (o[0] * 1.7 - 0.175),
                            (float) (o[1]),
                            (float) (o[2] * 1.7 - 0.175)),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 4), 1, 1, 0),
                            new Vector3f(0.35f, 0.35f, 0.35f), 18);
                }
            }

            // Phase C: snap back (52)
            if (cyclePhase == 52 && tick < dur - 40) {
                double[][] coreOffsets = {
                    {0,2,0},{0,3.2,0},{0,1.4,0},
                    {0.9,2.3,0},{-0.9,2.3,0},{0,2.3,0.9},{0,2.3,-0.9},
                    {0.6,2.8,0.6},{-0.6,2.8,-0.6},{0.6,1.7,-0.6},{-0.6,1.7,0.6},
                    {0.4,2.5,0.4},{-0.4,2.0,0.4},{0,2.5,-0.4}
                };
                for (int i = 0; i < core.size(); i++) {
                    double[] o = coreOffsets[i];
                    core.get(i).animateTo(new Vector3f((float) o[0] - 0.275f, (float) o[1], (float) o[2] - 0.275f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.55f, 0.55f, 0.55f), 14);
                }
                for (int i = 0; i < spikes.size(); i++) {
                    double[] d = spikeDirs.get(i);
                    spikes.get(i).animateTo(new Vector3f(
                            (float) (d[0] * 2.3 - 0.2),
                            (float) (2.3 + d[1] * 2.3),
                            (float) (d[2] * 2.3 - 0.2)),
                            new AxisAngle4f(0, 1, 0, 0),
                            new Vector3f(0.4f, 0.4f, 0.4f), 14);
                }
                for (int i = 0; i < halo.size(); i++) {
                    double a = Math.PI * 2 * i / halo.size();
                    halo.get(i).animateTo(new Vector3f((float) (Math.cos(a) * 3.5 - 0.225), 2.3f,
                            (float) (Math.sin(a) * 3.5 - 0.225)),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.45f, 0.3f, 0.45f), 14);
                }
                for (int i = 0; i < debris.size(); i++) {
                    double[] o = debrisOffsets.get(i);
                    debris.get(i).animateTo(new Vector3f(
                            (float) o[0] - 0.175f, (float) o[1], (float) o[2] - 0.175f),
                            new AxisAngle4f(0, 1, 1, 0),
                            new Vector3f(0.35f, 0.35f, 0.35f), 14);
                }
            }

            // Continuous: white flash + amethyst particles + chaotic debris rotation
            if (tick % 4 == 0) {
                Location c = getCenter().clone().add(0, 2.3, 0);
                w.spawnParticle(Particle.END_ROD, c, 6, 1.5, 1.5, 1.5, 0.05);
                DisplayBuilder.dustParticles(c, 4, 2.5, 240, 240, 255, 1.2f);
                for (BlockDisplayHandle s : spikes) {
                    Location sl = s.entity().getLocation();
                    w.spawnParticle(Particle.WAX_OFF, sl, 2, 0.2, 0.2, 0.2, 0.02);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CollapsedStar(plugin); }
    }

    // ================================================================
    // 3. SKINWALKER'S GAIT — 26 displays
    //    Pair of disembodied legs walking. Bone segments + joint caps + hooves.
    //    Uses impact damage on stomp.
    // ================================================================
    public static class SkinwalkerGait extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftLeg = new ArrayList<>();
        private final List<BlockDisplayHandle> rightLeg = new ArrayList<>();
        private double walkX = 0;
        private double walkZ = 0;
        private double dirX = 1, dirZ = 0;
        private boolean leftLifted = false;
        private boolean frozen = false;
        private int freezeUntil = -1;
        private int nextStepTick = 30;

        public SkinwalkerGait(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("skinwalker_gait", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.5);
            config.setImpactRadius(8.8);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, 0.4f);
            double a = Math.random() * Math.PI * 2;
            dirX = Math.cos(a);
            dirZ = Math.sin(a);

            // Each leg: 4 upper bone segments, 3 lower bone, 1 joint cap, 1 hoof = 9 displays.
            // 2 legs * ~13 = 26 total (we'll do 13 per leg).
            buildLeg(center, leftLeg, -1);
            buildLeg(center, rightLeg, +1);
        }

        private void buildLeg(Location center, List<BlockDisplayHandle> list, int side) {
            // Lateral offset perpendicular to walk direction
            double px = -dirZ * 0.9 * side;
            double pz = dirX * 0.9 * side;
            // 4 upper bone segments
            for (int i = 0; i < 4; i++) {
                Location p = center.clone().add(px, 4.5 + i * 0.6, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BONE_BLOCK);
                h.scale(0.5f, 0.7f, 0.5f).glow(240, 230, 200).interpolation(20, i);
                list.add(h);
            }
            // joint cap (deepslate tiles, knee)
            Location knee = center.clone().add(px, 3.2, pz);
            BlockDisplayHandle k = displayBuilder.spawnBlock(knee, Material.DEEPSLATE_TILES);
            k.scale(0.7f, 0.5f, 0.7f).glow(80, 80, 100).interpolation(20, 5);
            list.add(k);
            // 4 lower bone segments (extra one for stretched-unnaturally-long look)
            for (int i = 0; i < 4; i++) {
                Location p = center.clone().add(px, 1.0 + i * 0.55, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BONE_BLOCK);
                h.scale(0.45f, 0.65f, 0.45f).glow(220, 215, 195).interpolation(20, 6 + i);
                list.add(h);
            }
            // ankle joint cap
            Location ankle = center.clone().add(px, 0.55, pz);
            BlockDisplayHandle a = displayBuilder.spawnBlock(ankle, Material.DEEPSLATE_TILES);
            a.scale(0.55f, 0.4f, 0.55f).glow(80, 80, 100).interpolation(20, 11);
            list.add(a);
            // 3 hoof/foot blocks (blackstone)
            for (int i = 0; i < 3; i++) {
                Location p = center.clone().add(px + (i - 1) * 0.3, 0.15, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.55f, 0.3f, 0.55f).glow(40, 40, 50).interpolation(20, 12 + i);
                list.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            if (tick > dur - 20) return;

            // Random freeze mid-stride
            if (frozen) {
                if (tick >= freezeUntil) {
                    // RESUME: leg comes DOWN HARD — 2x impact radius
                    frozen = false;
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_HORSE_STEP, 1.6f, 0.4f);
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_IRON_GOLEM_DEATH, 0.8f, 0.3f);
                    Location stomp = getCenter().clone().add(walkX, 0, walkZ);
                    config.setImpactRadius(17.6);
                    triggerImpactDamage(stomp);
                    config.setImpactRadius(8.8);
                    w.spawnParticle(Particle.BLOCK, stomp, 80, 3, 0.3, 3, 0.1,
                            Material.BLACKSTONE.createBlockData());
                    w.spawnParticle(Particle.LARGE_SMOKE, stomp, 20, 2, 0.3, 2, 0.05);
                    plantLeg(leftLifted ? leftLeg : rightLeg);
                    nextStepTick = tick + 35;
                }
                return;
            }

            if (tick == nextStepTick) {
                // Step
                leftLifted = !leftLifted;
                if (Math.random() < 0.18) {
                    // Freeze with leg in air for 40 ticks
                    frozen = true;
                    freezeUntil = tick + 40;
                    liftLeg(leftLifted ? leftLeg : rightLeg, 2.5f);
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BONE_BLOCK_STEP, 1.0f, 0.3f);
                } else {
                    // Normal step: lift, advance, plant
                    liftLeg(leftLifted ? leftLeg : rightLeg, 1.4f);
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BONE_BLOCK_STEP, 1.0f, 0.3f);
                    walkX += dirX * 1.3;
                    walkZ += dirZ * 1.3;
                    nextStepTick = tick + 35;
                }
            }

            if (tick == nextStepTick - 18 && !frozen) {
                // Mid-step plant
                Location stomp = getCenter().clone().add(walkX, 0, walkZ);
                plantLeg(leftLifted ? leftLeg : rightLeg);
                triggerImpactDamage(stomp);
                DisplayBuilder.playSound(stomp, Sound.ENTITY_HORSE_STEP, 1.5f, 0.4f);
                w.spawnParticle(Particle.BLOCK, stomp, 35, 1.8, 0.2, 1.8, 0.05,
                        Material.BLACKSTONE.createBlockData());
                w.spawnParticle(Particle.LARGE_SMOKE, stomp, 8, 1.2, 0.2, 1.2, 0.02);
            }

            // Joint cap bone dust
            if (tick % 6 == 0) {
                for (BlockDisplayHandle h : leftLeg) {
                    if (Math.random() < 0.15) {
                        Location l = h.entity().getLocation();
                        DisplayBuilder.dustParticles(l, 1, 0.2, 240, 230, 200, 1.0f);
                    }
                }
                for (BlockDisplayHandle h : rightLeg) {
                    if (Math.random() < 0.15) {
                        Location l = h.entity().getLocation();
                        DisplayBuilder.dustParticles(l, 1, 0.2, 240, 230, 200, 1.0f);
                    }
                }
            }
        }

        private void liftLeg(List<BlockDisplayHandle> leg, float lift) {
            // Translate every segment up by lift amount (relative to current walkX/walkZ)
            for (int i = 0; i < leg.size(); i++) {
                BlockDisplayHandle h = leg.get(i);
                Location ent = h.entity().getLocation();
                Location origin = getCenter();
                if (origin == null) return;
                float baseY = (float) (ent.getY() - origin.getY());
                float dx = (float) walkX;
                float dz = (float) walkZ;
                Vector3f cur = h.entity().getTransformation().getTranslation();
                h.animateTo(new Vector3f(cur.x + dx * 0.0f, baseY + lift, cur.z + dz * 0.0f),
                        new AxisAngle4f(0.2f * lift, 1, 0, 0),
                        h.entity().getTransformation().getScale(), 12);
            }
        }

        private void plantLeg(List<BlockDisplayHandle> leg) {
            for (BlockDisplayHandle h : leg) {
                Vector3f cur = h.entity().getTransformation().getTranslation();
                h.animateTo(new Vector3f(cur.x, cur.y - 1.4f, cur.z),
                        new AxisAngle4f(0, 1, 0, 0),
                        h.entity().getTransformation().getScale(), 8);
            }
        }

        @Override public AbstractAttack newInstance() { return new SkinwalkerGait(plugin); }
    }

    // ================================================================
    // 4. GLASS CONFESSIONAL — 30 displays
    //    Tall narrow booth: 4 glass walls, 4 frame posts, ceiling caps,
    //    floor tiles, interior shroomlight. Implodes.
    // ================================================================
    public static class GlassConfessional extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> floor = new ArrayList<>();
        private final List<BlockDisplayHandle> frame = new ArrayList<>();
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<BlockDisplayHandle> ceiling = new ArrayList<>();
        private final List<BlockDisplayHandle> interior = new ArrayList<>();

        public GlassConfessional(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glass_confessional", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(30.0);
            config.setImpactRadius(5.6);
            config.setDurationTicks(280);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_PLACE, 1.0f, 0.6f);

            // Floor 2x2 blackstone bricks (4)
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    Location p = center.clone().add(x - 0.5, 0.05, z - 0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                    h.scale(0.01f, 0.01f, 0.01f).glow(60, 60, 70).interpolation(15, 0);
                    h.animateTo(new Vector3f(-0.45f, 0.05f, -0.45f),
                            new AxisAngle4f(0, 1, 0, 0),
                            new Vector3f(0.95f, 0.15f, 0.95f), 15);
                    floor.add(h);
                }
            }
            // 4 frame posts (crying obsidian, 4 tall) at corners — spawn after floor
            for (int i = 0; i < 4; i++) {
                int dx = (i % 2 == 0) ? 1 : -1;
                int dz = (i < 2) ? 1 : -1;
                Location p = center.clone().add(dx, 2, dz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(0.01f, 0.01f, 0.01f).glow(60, 0, 100).interpolation(20, 18);
                h.animateTo(new Vector3f(-0.15f, 0.2f, -0.15f),
                        new AxisAngle4f(0, 1, 0, 0),
                        new Vector3f(0.3f, 4.0f, 0.3f), 20);
                frame.add(h);
            }
            // 4 glass walls (1x4 panels, slot in after frame)
            double[][] wallPos = {{0,2,1.05},{0,2,-1.05},{1.05,2,0},{-1.05,2,0}};
            float[][] wallScale = {{1.9f,3.8f,0.1f},{1.9f,3.8f,0.1f},{0.1f,3.8f,1.9f},{0.1f,3.8f,1.9f}};
            for (int i = 0; i < 4; i++) {
                Location p = center.clone().add(wallPos[i][0], wallPos[i][1], wallPos[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.RED_STAINED_GLASS);
                h.scale(0.01f, 0.01f, 0.01f).glow(200, 30, 30).interpolation(20, 38);
                h.animateTo(new Vector3f(-wallScale[i][0]/2f, 0.2f, -wallScale[i][2]/2f),
                        new AxisAngle4f(0, 1, 0, 0),
                        new Vector3f(wallScale[i][0], wallScale[i][1], wallScale[i][2]), 20);
                walls.add(h);
            }
            // 4 ceiling caps (blackstone) drop in last
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    Location p = center.clone().add(x - 0.5, 4.1, z - 0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                    h.scale(0.01f, 0.01f, 0.01f).glow(80, 80, 90).interpolation(15, 58);
                    h.animateTo(new Vector3f(-0.45f, 4.1f, -0.45f),
                            new AxisAngle4f(0, 1, 0, 0),
                            new Vector3f(0.95f, 0.2f, 0.95f), 15);
                    ceiling.add(h);
                }
            }
            // 8 shroomlight interior panels (subtle inner glow) + 6 sculk floor accents
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = center.clone().add(Math.cos(a) * 0.45, 1 + Math.random() * 2.5, Math.sin(a) * 0.45);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SHROOMLIGHT);
                h.scale(0.01f, 0.01f, 0.01f).glow(255, 100, 30).interpolation(20, 50);
                h.animateTo(new Vector3f(-0.1f, (float) (1 + Math.random() * 2.5), -0.1f),
                        new AxisAngle4f(0, 1, 0, 0),
                        new Vector3f(0.2f, 0.2f, 0.2f), 20);
                interior.add(h);
            }
            for (int i = 0; i < 6; i++) {
                Location p = center.clone().add((Math.random() - 0.5) * 1.5, 0.2, (Math.random() - 0.5) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SCULK);
                h.scale(0.01f, 0.01f, 0.01f).glow(50, 220, 200).interpolation(15, 60);
                h.animateTo(new Vector3f(-0.15f, 0.2f, -0.15f),
                        new AxisAngle4f(0, 1, 0, 0),
                        new Vector3f(0.3f, 0.05f, 0.3f), 15);
                interior.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            int crackStart = 80;
            int implodeAt = 180;
            int frameCollapseAt = 210;

            // Phase: assembly sounds
            if (tick == 20) DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 1.0f, 0.5f);
            if (tick == 40) DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_BRICKS_PLACE, 1.0f, 0.6f);
            if (tick == 60) DisplayBuilder.playSound(getCenter(), Sound.BLOCK_GLASS_PLACE, 1.0f, 0.5f);

            // Crack phase: walls shake + glass shard particles
            if (tick >= crackStart && tick < implodeAt) {
                if (tick % 4 == 0) {
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_GLASS_BREAK, 0.7f,
                            0.7f + (float) Math.random() * 0.5f);
                    for (BlockDisplayHandle wall : walls) {
                        Vector3f cur = wall.entity().getTransformation().getTranslation();
                        Vector3f scl = wall.entity().getTransformation().getScale();
                        float jx = (float) ((Math.random() - 0.5) * 0.15);
                        float jz = (float) ((Math.random() - 0.5) * 0.15);
                        wall.animateTo(new Vector3f(cur.x + jx, cur.y, cur.z + jz),
                                new AxisAngle4f(0, 0, 1, 0), scl, 4);
                    }
                }
                if (tick % 2 == 0) {
                    Location c = getCenter().clone().add(0, 2, 0);
                    w.spawnParticle(Particle.BLOCK, c, 8, 1.2, 1.5, 1.2, 0.05,
                            Material.RED_STAINED_GLASS.createBlockData());
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR,
                            c.clone().add(0, 1, 0), 3, 0.8, 0.6, 0.8, 0);
                    w.spawnParticle(Particle.FLAME, c, 4, 0.7, 1.2, 0.7, 0.01);
                }
            }

            // Implosion: glass walls translate to center
            if (tick == implodeAt) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_HURT, 1.5f, 0.5f);
                for (BlockDisplayHandle wall : walls) {
                    wall.animateTo(new Vector3f(-0.1f, 2f, -0.1f),
                            new AxisAngle4f((float) Math.PI, 1, 1, 0),
                            new Vector3f(0.2f, 0.2f, 0.2f), 14);
                }
                triggerImpactDamage(getCenter().clone().add(0, 2, 0));
                w.spawnParticle(Particle.EXPLOSION_EMITTER, getCenter().clone().add(0, 2, 0), 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.BLOCK, getCenter().clone().add(0, 2, 0), 60, 1, 1, 1, 0.1,
                        Material.RED_STAINED_GLASS.createBlockData());
            }

            // Frame collapse
            if (tick == frameCollapseAt) {
                for (BlockDisplayHandle f : frame) {
                    f.animateTo(new Vector3f(-0.1f, 2f, -0.1f),
                            new AxisAngle4f((float) Math.PI / 2, 0, 0, 1),
                            new Vector3f(0.15f, 0.4f, 0.15f), 14);
                }
                for (BlockDisplayHandle c : ceiling) {
                    c.animateTo(new Vector3f(-0.2f, 2f, -0.2f),
                            new AxisAngle4f((float) Math.PI / 3, 1, 0, 1),
                            new Vector3f(0.4f, 0.1f, 0.4f), 14);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new GlassConfessional(plugin); }
    }

    // ================================================================
    // 5. BLACK TIDE — 28 displays
    //    Curved wave shape with sculk-foam crest. Travels forward,
    //    crashes into wall.
    // ================================================================
    public static class BlackTide extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> trough = new ArrayList<>();
        private final List<BlockDisplayHandle> wall = new ArrayList<>();
        private final List<BlockDisplayHandle> crest = new ArrayList<>();
        private double dirX = 1, dirZ = 0;

        public BlackTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_tide", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(7.5);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(220);
            config.setCooldownTicks(310);
        }

        @Override
        protected void onSpawn(Location center) {
            double a = Math.random() * Math.PI * 2;
            dirX = Math.cos(a);
            dirZ = Math.sin(a);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.4f);

            // Wave starts -8 behind center along dir, builds horizontal 8-wide wave
            // Perpendicular axis for width
            double perpX = -dirZ;
            double perpZ = dirX;
            Location origin = center.clone().add(-dirX * 8, 0, -dirZ * 8);

            // Trough base — 8-wide horizontal row at ground level (8 displays of deepslate tiles)
            for (int i = 0; i < 8; i++) {
                double off = i - 3.5;
                Location p = origin.clone().add(perpX * off, 0.3, perpZ * off);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.DEEPSLATE_TILES);
                h.scale(0.7f, 0.4f, 0.7f).glow(20, 20, 30).interpolation(15, 0);
                trough.add(h);
            }
            // Wall body — 8-wide x 3-tall blackstone (24 entries... we'll do 8x2 = 16 to keep budget)
            for (int row = 0; row < 2; row++) {
                for (int i = 0; i < 8; i++) {
                    double off = i - 3.5;
                    Location p = origin.clone().add(perpX * off, 1 + row * 1.0, perpZ * off);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                    h.scale(0.7f, 0.7f, 0.7f).glow(20, 20, 25).interpolation(15, row * 2);
                    wall.add(h);
                }
            }
            // Crest — 4 sculk irregular foam blocks with forward overhang
            for (int i = 0; i < 4; i++) {
                double off = (i - 1.5) * 1.3;
                Location p = origin.clone().add(perpX * off + dirX * 0.6, 3.2, perpZ * off + dirZ * 0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SCULK);
                h.scale(0.85f, 0.6f, 0.85f).glow(50, 220, 200).interpolation(15, 5);
                crest.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            // Travel: tick 0-160 advance forward over 16 blocks
            int travelLen = dur - 60;
            double progress = Math.min(1.0, (double) tick / travelLen);

            if (tick % 4 == 0 && tick < travelLen) {
                double forward = progress * 16;
                double perpX = -dirZ;
                double perpZ = dirX;
                Location origin = getCenter().clone().add(-dirX * 8 + dirX * forward, 0, -dirZ * 8 + dirZ * forward);

                for (int i = 0; i < trough.size(); i++) {
                    double off = i - 3.5;
                    Location target = origin.clone().add(perpX * off, 0.3, perpZ * off);
                    Location ctr = getCenter();
                    Vector3f cur = trough.get(i).entity().getTransformation().getTranslation();
                    trough.get(i).animateTo(new Vector3f(
                            (float) (target.getX() - ctr.getX()) - 0.35f,
                            0.3f,
                            (float) (target.getZ() - ctr.getZ()) - 0.35f),
                            new AxisAngle4f(0, 1, 0, 0),
                            new Vector3f(0.7f, 0.4f, 0.7f), 4);
                }
                for (int row = 0; row < 2; row++) {
                    for (int i = 0; i < 8; i++) {
                        double off = i - 3.5;
                        Location target = origin.clone().add(perpX * off, 1 + row * 1.0, perpZ * off);
                        BlockDisplayHandle h = wall.get(row * 8 + i);
                        h.animateTo(new Vector3f(
                                (float) (target.getX() - getCenter().getX()) - 0.35f,
                                1f + row * 1.0f,
                                (float) (target.getZ() - getCenter().getZ()) - 0.35f),
                                new AxisAngle4f(0, 1, 0, 0),
                                new Vector3f(0.7f, 0.7f, 0.7f), 4);
                    }
                }
                // Crest curls forward more as it travels
                float curlAngle = (float) (progress * Math.PI * 0.6);
                for (int i = 0; i < crest.size(); i++) {
                    double off = (i - 1.5) * 1.3;
                    Location target = origin.clone().add(perpX * off + dirX * (0.6 + progress * 1.5),
                            3.2 + Math.sin(progress * Math.PI) * 0.5,
                            perpZ * off + dirZ * (0.6 + progress * 1.5));
                    crest.get(i).animateTo(new Vector3f(
                            (float) (target.getX() - getCenter().getX()) - 0.425f,
                            (float) (target.getY() - getCenter().getY()),
                            (float) (target.getZ() - getCenter().getZ()) - 0.425f),
                            new AxisAngle4f(curlAngle, (float) -dirZ, 0, (float) dirX),
                            new Vector3f(0.85f, 0.6f, 0.85f), 4);
                }

                // Sculk foam trail patches on the ground behind
                if (tick % 8 == 0) {
                    Location trail = origin.clone().add(perpX * (Math.random() - 0.5) * 7, 0.05,
                            perpZ * (Math.random() - 0.5) * 7);
                    w.spawnParticle(Particle.SCULK_SOUL, trail, 4, 0.5, 0.3, 0.5, 0.02);
                }
            }

            if (tick % 3 == 0 && tick < travelLen) {
                // Sculk soul rising from crest
                for (BlockDisplayHandle c : crest) {
                    Location l = c.entity().getLocation().clone().add(0.4, 0.5, 0.4);
                    w.spawnParticle(Particle.SCULK_SOUL, l, 1, 0.4, 0.3, 0.4, 0.03);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, l, 1, 0.3, 0.2, 0.3, 0.005);
                }
            }
            if (tick % 12 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_SPREAD, 0.7f, 0.4f);
            }

            // Crash phase
            if (tick == travelLen) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.3f);
                Location crash = getCenter().clone().add(dirX * 10, 1, dirZ * 10);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, crash, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.SCULK_SOUL, crash, 50, 3, 2, 3, 0.2);
                w.spawnParticle(Particle.LARGE_SMOKE, crash, 30, 4, 2, 4, 0.1);

                // Scatter all pieces forward
                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(trough); all.addAll(wall); all.addAll(crest);
                for (BlockDisplayHandle h : all) {
                    Vector3f cur = h.entity().getTransformation().getTranslation();
                    float fx = (float) (dirX * (4 + Math.random() * 4));
                    float fz = (float) (dirZ * (4 + Math.random() * 4));
                    h.animateTo(new Vector3f(cur.x + fx, cur.y + (float) (Math.random() * 2 - 0.5),
                            cur.z + fz),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 2), 1, 1, 0),
                            h.entity().getTransformation().getScale(), 30);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new BlackTide(plugin); }
    }

    // ================================================================
    // 6. RIB CAGE TRAP — 30 displays
    //    Spine column + 6 rib pairs + sternum. Opens, snaps shut.
    // ================================================================
    public static class RibCageTrap extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spine = new ArrayList<>();
        private final List<BlockDisplayHandle> ribs = new ArrayList<>(); // 12 ribs (6 pairs)
        private final List<BlockDisplayHandle> sternum = new ArrayList<>();
        private boolean cageOpen = false;

        public RibCageTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rib_cage_trap", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(27.0);
            config.setImpactRadius(7.2);
            config.setDurationTicks(360);
            config.setCooldownTicks(330);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, 0.5f);

            // Spine — 6 deepslate tiles vertical (back of cage at z=-0.5)
            for (int i = 0; i < 6; i++) {
                Location p = center.clone().add(0, 1.2 + i * 0.85, -0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.DEEPSLATE_TILES);
                h.scale(0.4f, 0.85f, 0.4f).glow(80, 80, 100).interpolation(10, i);
                spine.add(h);
            }
            // 12 rib bones (6 pairs L/R) starting flat (horizontal, scale 0)
            for (int row = 0; row < 6; row++) {
                for (int side = -1; side <= 1; side += 2) {
                    Location p = center.clone().add(side * 0.4, 1.5 + row * 0.85, -0.4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BONE_BLOCK);
                    h.scale(0.01f, 0.01f, 0.01f).glow(240, 230, 200).interpolation(15, 8 + row * 2);
                    // Initial: arc outward
                    float armLen = 1.4f - row * 0.05f;
                    h.animateTo(new Vector3f(side * 0.4f - armLen / 2f * side, 1.5f + row * 0.85f, -0.4f),
                            new AxisAngle4f((float) (side * Math.PI / 4), 0, 0, 1),
                            new Vector3f(armLen, 0.3f, 0.3f), 15);
                    ribs.add(h);
                }
            }
            // 7 sternum blackstone segments (front, vertical)
            for (int i = 0; i < 7; i++) {
                Location p = center.clone().add(0, 1.3 + i * 0.78, 0.85);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.01f, 0.01f, 0.01f).glow(60, 60, 70).interpolation(15, 22);
                h.animateTo(new Vector3f(-0.2f, 1.3f + i * 0.78f, 0.7f),
                        new AxisAngle4f(0, 1, 0, 0),
                        new Vector3f(0.4f, 0.65f, 0.3f), 15);
                sternum.add(h);
            }
            // 6 soul-fire wisp accents (sculk-vein) inside the cage to push display count over threshold
            for (int i = 0; i < 6; i++) {
                Location p = center.clone().add((Math.random() - 0.5) * 0.8,
                        1.5 + Math.random() * 4, (Math.random() - 0.5) * 0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SCULK_VEIN);
                h.scale(0.01f, 0.01f, 0.01f).glow(50, 220, 200).interpolation(15, 25);
                h.animateTo(new Vector3f(-0.15f, p.getY() < center.getY() ? 1.5f : (float) (p.getY() - center.getY()), -0.1f),
                        new AxisAngle4f((float) (Math.random() * Math.PI), 0, 1, 0),
                        new Vector3f(0.3f, 0.3f, 0.3f), 15);
                sternum.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            // 80-tick cycle: open 0-40, snap shut 40, hold-closed 40-80
            int phase = tick % 80;

            if (phase == 0 && tick > 30 && tick < dur - 30) {
                // Open ribs outward (top to bottom progressive)
                cageOpen = true;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_SKELETON_HURT, 1.2f, 0.5f);
                for (int row = 0; row < 6; row++) {
                    float armLen = 2.4f - row * 0.1f;
                    int delay = (5 - row) * 3;
                    for (int s = 0; s < 2; s++) {
                        int idx = row * 2 + s;
                        int side = (s == 0) ? -1 : 1;
                        BlockDisplayHandle h = ribs.get(idx);
                        h.interpolation(20, delay);
                        h.animateTo(new Vector3f(side * 0.4f - armLen / 2f * side,
                                1.5f + row * 0.85f, -0.2f),
                                new AxisAngle4f((float) (side * Math.PI / 2.5), 0, 0, 1),
                                new Vector3f(armLen, 0.3f, 0.3f), 20);
                    }
                }
            }

            if (phase == 40 && tick > 30 && tick < dur - 30) {
                // Snap shut — all ribs collide at center
                cageOpen = false;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_RAVAGER_ATTACK, 1.6f, 0.6f);
                for (int row = 0; row < 6; row++) {
                    for (int s = 0; s < 2; s++) {
                        int idx = row * 2 + s;
                        int side = (s == 0) ? -1 : 1;
                        BlockDisplayHandle h = ribs.get(idx);
                        h.interpolation(8, 0);
                        h.animateTo(new Vector3f(-0.2f, 1.5f + row * 0.85f, -0.15f),
                                new AxisAngle4f((float) (side * Math.PI / 8), 0, 0, 1),
                                new Vector3f(0.4f, 0.3f, 0.3f), 8);
                    }
                }
                triggerImpactDamage(getCenter().clone().add(0, 2.5, 0));
                w.spawnParticle(Particle.BLOCK, getCenter().clone().add(0, 2.5, 0), 50, 0.8, 1.8, 0.8, 0.05,
                        Material.BONE_BLOCK.createBlockData());
            }

            // Spine pulse (Y scale breathing)
            if (tick % 6 == 0) {
                for (int i = 0; i < spine.size(); i++) {
                    BlockDisplayHandle h = spine.get(i);
                    float baseY = 1.2f + i * 0.85f;
                    float scaleY = 0.85f + (float) Math.sin(tick * 0.15 + i * 0.5) * 0.08f;
                    h.animateTo(new Vector3f(-0.2f, baseY, -0.7f),
                            new AxisAngle4f(0, 1, 0, 0),
                            new Vector3f(0.4f, scaleY, 0.4f), 6);
                }
            }
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BONE_BLOCK_STEP, 0.7f, 0.4f);
            }

            // Particles by phase
            if (cageOpen && tick % 3 == 0) {
                Location c = getCenter().clone().add(0, 2.5, 0);
                DisplayBuilder.dustParticles(c, 4, 1.0, 240, 230, 200, 1.4f);
                w.spawnParticle(Particle.BLOCK, c, 3, 0.8, 1.5, 0.8, 0.02,
                        Material.BONE_BLOCK.createBlockData());
            } else if (!cageOpen && tick % 5 == 0) {
                // Soul fire wisps between ribs
                for (int i = 0; i < 4; i++) {
                    Location wisp = getCenter().clone().add(
                            (Math.random() - 0.5) * 1.5,
                            1.5 + Math.random() * 4,
                            (Math.random() - 0.5) * 1.5);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, wisp, 1, 0.1, 0.1, 0.1, 0.005);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new RibCageTrap(plugin); }
    }

    // ================================================================
    // 7. THE PENDULUM — 27 displays
    //    Pivot mount + 6 rod segments + wide blade. Swings increasing,
    //    full rotates, snaps back.
    // ================================================================
    public static class Pendulum extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pivot = new ArrayList<>();
        private final List<BlockDisplayHandle> rod = new ArrayList<>();
        private final List<BlockDisplayHandle> blade = new ArrayList<>();
        private double swingPhase = 0;
        private boolean fullSpin = false;
        private double prevAngle = 0;

        public Pendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pendulum", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(24.0);
            config.setImpactRadius(8.8);
            config.setDurationTicks(400);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.4f);

            // Pivot mount — 3-block cluster at top (Y = 8)
            for (int i = 0; i < 3; i++) {
                Location p = center.clone().add((i - 1) * 0.6, 8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.7f, 0.7f, 0.7f).glow(60, 60, 70).interpolation(10, 0);
                pivot.add(h);
            }
            // 6 rod segments hanging down (we'll animate as a chain)
            for (int i = 0; i < 6; i++) {
                Location p = center.clone().add(0, 7 - i * 0.95, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                h.scale(0.35f, 0.95f, 0.35f).glow(80, 80, 90).interpolation(8, 0);
                rod.add(h);
            }
            // Blade weight at bottom (5 wide x 2 tall)
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 2; y++) {
                    Location p = center.clone().add((x - 2) * 0.9, 0.6 + y * 0.7, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.NETHER_BRICKS);
                    h.scale(0.85f, 0.7f, 0.4f).glow(140, 0, 30).interpolation(10, 0);
                    blade.add(h);
                }
            }
            // 8 amethyst edge accents (sparks at blade)
            for (int i = 0; i < 8; i++) {
                Location p = center.clone().add(((i - 3.5) * 0.55), 0.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.RED_NETHER_BRICKS);
                h.scale(0.5f, 0.2f, 0.4f).glow(255, 30, 30).interpolation(10, 0);
                blade.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            // 4 stages: short swing (0-100), medium (100-180), large (180-260), full spin (260-340), then random snap
            double angle;
            if (tick < 100) {
                double amp = 0.3;
                angle = Math.sin(tick * 0.18) * amp;
                fullSpin = false;
            } else if (tick < 180) {
                double amp = 0.7;
                angle = Math.sin(tick * 0.22) * amp;
                fullSpin = false;
            } else if (tick < 260) {
                double amp = 1.3;
                angle = Math.sin(tick * 0.25) * amp;
                fullSpin = false;
            } else if (tick < 340) {
                fullSpin = true;
                angle = (tick - 260) * 0.35;
            } else {
                // Snap back to swing
                fullSpin = false;
                angle = Math.sin(tick * 0.3) * 1.4;
            }

            // Direction change detection (sweep sound)
            if (Math.signum(angle) != Math.signum(prevAngle) && Math.abs(prevAngle) > 0.1) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
                if (fullSpin) {
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.7f);
                }
            }
            prevAngle = angle;

            // Animate rod + blade as one rigid arm rotating about pivot at Y=8
            if (tick % 2 == 0) {
                float a = (float) angle;
                float sin = (float) Math.sin(a);
                float cos = (float) Math.cos(a);
                // Rod segments: position on a slight curve
                for (int i = 0; i < rod.size(); i++) {
                    float dist = 1f + i * 0.95f; // distance from pivot
                    float tx = sin * dist;
                    float ty = 8f - cos * dist;
                    rod.get(i).animateTo(new Vector3f(tx - 0.175f, ty, -0.175f),
                            new AxisAngle4f(a, 0, 0, 1),
                            new Vector3f(0.35f, 0.95f, 0.35f), 2);
                }
                // Blade — 10 main pieces + 8 accents at distance ~7.5
                for (int i = 0; i < blade.size(); i++) {
                    float baseDist = 7.5f;
                    int idx = i;
                    if (idx < 10) {
                        int x = idx / 2;
                        int y = idx % 2;
                        float lx = (x - 2) * 0.9f;
                        float ly = 0.6f + y * 0.7f;
                        // Rotate (lx, -baseDist + ly) about pivot
                        float dx = lx;
                        float dy = -baseDist + ly;
                        float rx = dx * cos - dy * sin;
                        float ry = dx * sin + dy * cos;
                        blade.get(i).animateTo(new Vector3f(rx - 0.425f, 8 + ry, -0.2f),
                                new AxisAngle4f(a, 0, 0, 1),
                                new Vector3f(0.85f, 0.7f, 0.4f), 2);
                    } else {
                        int eIdx = idx - 10;
                        float lx = (eIdx - 3.5f) * 0.55f;
                        float ly = 0.2f;
                        float dx = lx;
                        float dy = -baseDist + ly;
                        float rx = dx * cos - dy * sin;
                        float ry = dx * sin + dy * cos;
                        blade.get(i).animateTo(new Vector3f(rx - 0.25f, 8 + ry, -0.2f),
                                new AxisAngle4f(a, 0, 0, 1),
                                new Vector3f(0.5f, 0.2f, 0.4f), 2);
                    }
                }
            }

            // Air slash particles along blade + sparks at pivot
            if (tick % 2 == 0) {
                float a = (float) angle;
                float sin = (float) Math.sin(a);
                float cos = (float) Math.cos(a);
                for (int i = -2; i <= 2; i++) {
                    float lx = i * 0.9f;
                    float dx = lx;
                    float dy = -7.5f;
                    float rx = dx * cos - dy * sin;
                    float ry = dx * sin + dy * cos;
                    Location bladePart = getCenter().clone().add(rx, 8 + ry, 0);
                    w.spawnParticle(Particle.SWEEP_ATTACK, bladePart, 1, 0.2, 0.2, 0.2, 0);
                    DisplayBuilder.dustParticles(bladePart, 1, 0.3, 200, 30, 30, 1.2f);
                }
                Location pivotL = getCenter().clone().add(0, 8, 0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, pivotL, 2, 0.4, 0.2, 0.4, 0.05);
            }

            if (tick % 20 == 0) {
                DisplayBuilder.playSound(getCenter().clone().add(0, 8, 0), Sound.BLOCK_CHAIN_FALL, 0.7f, 0.4f);
            }

            // Trigger impact damage at swing extremes (when angle reverses)
            if (Math.abs(angle) > 1.0 && tick % 4 == 0) {
                float a = (float) angle;
                float sin = (float) Math.sin(a);
                float cos = (float) Math.cos(a);
                Location bladePos = getCenter().clone().add(sin * 7.5, 8 - cos * 7.5, 0);
                triggerImpactDamage(bladePos);
            }
            if (fullSpin && tick % 6 == 0) {
                // Cylindrical damage beneath pivot
                triggerImpactDamage(getCenter().clone().add(0, 1, 0));
            }
        }

        @Override public AbstractAttack newInstance() { return new Pendulum(plugin); }
    }

    // ================================================================
    // 8. SEARING TETRAD — 28 displays
    //    4 pyramids of 7 displays each in 2x2 pattern, tips inward.
    //    Orbit, periodically launch toward center.
    // ================================================================
    public static class SearingTetrad extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> pyramids = new ArrayList<>();
        private final double[][] basePositions = {
            {-3,3},{3,3},{-3,-3},{3,-3}
        };
        private boolean launching = false;
        private int launchStart = -100;

        public SearingTetrad(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("searing_tetrad", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(6.8);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(340);
            config.setCooldownTicks(330);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 0.5f);

            for (int p = 0; p < 4; p++) {
                List<BlockDisplayHandle> pyr = new ArrayList<>();
                double bx = basePositions[p][0];
                double bz = basePositions[p][1];
                // 4 magma base
                double[][] baseOffs = {{-0.5,-0.5},{0.5,-0.5},{-0.5,0.5},{0.5,0.5}};
                for (double[] o : baseOffs) {
                    Location loc = center.clone().add(bx + o[0], 2.5, bz + o[1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    h.scale(0.8f, 0.6f, 0.8f).glow(255, 120, 30).interpolation(15, 0);
                    pyr.add(h);
                }
                // 3 rising basalt sides
                for (int i = 0; i < 3; i++) {
                    double a = i * 2 * Math.PI / 3;
                    Location loc = center.clone().add(bx + Math.cos(a) * 0.4, 3.4, bz + Math.sin(a) * 0.4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                    h.scale(0.7f, 0.6f, 0.7f).glow(40, 40, 50).interpolation(15, 0);
                    pyr.add(h);
                }
                // Red nether brick tip (pointing inward toward center)
                double dirX = -bx / Math.sqrt(bx * bx + bz * bz);
                double dirZ = -bz / Math.sqrt(bx * bx + bz * bz);
                Location tip = center.clone().add(bx + dirX * 0.3, 4.2, bz + dirZ * 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(tip, Material.RED_NETHER_BRICKS);
                h.scale(0.45f, 0.7f, 0.45f).glow(255, 30, 30).interpolation(15, 0);
                pyr.add(h);
                pyramids.add(pyr);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            // Periodic launch every 100 ticks
            if (tick % 100 == 80 && tick < dur - 40) {
                launching = true;
                launchStart = tick;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.7f);
                // Launch all 4 toward center
                for (List<BlockDisplayHandle> pyr : pyramids) {
                    for (BlockDisplayHandle h : pyr) {
                        Vector3f cur = h.entity().getTransformation().getTranslation();
                        h.animateTo(new Vector3f(-cur.x * 0.0f - 0.5f, cur.y, -cur.z * 0.0f - 0.5f),
                                new AxisAngle4f((float) (tick * 0.3), 1, 1, 0),
                                h.entity().getTransformation().getScale(), 14);
                    }
                }
            }
            // Bounce back after 14 ticks
            if (launching && tick == launchStart + 14) {
                launching = false;
                w.spawnParticle(Particle.EXPLOSION_EMITTER, getCenter().clone().add(0, 3, 0), 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.LARGE_SMOKE, getCenter().clone().add(0, 3, 0), 30, 1.5, 1.5, 1.5, 0.1);
                rebuildPyramidsAtBase(tick);
            }

            // Idle: orbit + Y rotation
            if (!launching && tick % 4 == 0) {
                double orbitAngle = tick * 0.015;
                double cos = Math.cos(orbitAngle);
                double sin = Math.sin(orbitAngle);
                for (int p = 0; p < 4; p++) {
                    double baseX = basePositions[p][0];
                    double baseZ = basePositions[p][1];
                    double bx = baseX * cos - baseZ * sin;
                    double bz = baseX * sin + baseZ * cos;
                    List<BlockDisplayHandle> pyr = pyramids.get(p);
                    // Rebuild relative to (bx, bz) with self-rotation on Y
                    float spin = (float) (tick * 0.12 + p * Math.PI / 2);
                    // 4 base
                    double[][] baseOffs = {{-0.5,-0.5},{0.5,-0.5},{-0.5,0.5},{0.5,0.5}};
                    for (int i = 0; i < 4; i++) {
                        double ox = baseOffs[i][0] * Math.cos(spin) - baseOffs[i][1] * Math.sin(spin);
                        double oz = baseOffs[i][0] * Math.sin(spin) + baseOffs[i][1] * Math.cos(spin);
                        pyr.get(i).animateTo(new Vector3f((float) (bx + ox - 0.4), 2.5f, (float) (bz + oz - 0.4)),
                                new AxisAngle4f(spin, 0, 1, 0),
                                new Vector3f(0.8f, 0.6f, 0.8f), 4);
                    }
                    // 3 sides
                    for (int i = 0; i < 3; i++) {
                        double a = i * 2 * Math.PI / 3 + spin;
                        pyr.get(4 + i).animateTo(new Vector3f(
                                (float) (bx + Math.cos(a) * 0.4 - 0.35), 3.4f,
                                (float) (bz + Math.sin(a) * 0.4 - 0.35)),
                                new AxisAngle4f(spin, 0, 1, 0),
                                new Vector3f(0.7f, 0.6f, 0.7f), 4);
                    }
                    // Tip pointing inward toward CURRENT center (always center 0,0)
                    double r = Math.sqrt(bx * bx + bz * bz);
                    double dirX = (r > 0.001) ? -bx / r : 0;
                    double dirZ = (r > 0.001) ? -bz / r : 0;
                    pyr.get(7).animateTo(new Vector3f(
                            (float) (bx + dirX * 0.3 - 0.225), 4.2f,
                            (float) (bz + dirZ * 0.3 - 0.225)),
                            new AxisAngle4f(spin, 0, 1, 0),
                            new Vector3f(0.45f, 0.7f, 0.45f), 4);
                }
            }

            // Fire from each magma face + smoke on launch
            if (tick % 3 == 0) {
                for (List<BlockDisplayHandle> pyr : pyramids) {
                    for (int i = 0; i < 4; i++) {
                        Location l = pyr.get(i).entity().getLocation();
                        w.spawnParticle(Particle.FLAME, l.clone().add(0.4, 0.4, 0.4), 2, 0.3, 0.3, 0.3, 0.02);
                    }
                }
            }
            if (launching && tick % 2 == 0) {
                w.spawnParticle(Particle.LARGE_SMOKE, getCenter().clone().add(0, 3, 0), 4, 1, 1, 1, 0.05);
            }
            if (tick % 30 == 0) {
                for (int p = 0; p < 4; p++) {
                    Location l = getCenter().clone().add(basePositions[p][0], 3, basePositions[p][1]);
                    DisplayBuilder.playSound(l, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.5f);
                }
            }
        }

        private void rebuildPyramidsAtBase(int tick) {
            // Snap-back animateTo their orbit positions — simply reset interpolation; the next idle tick will handle it
            for (int p = 0; p < 4; p++) {
                for (BlockDisplayHandle h : pyramids.get(p)) {
                    h.interpolation(8, 0);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SearingTetrad(plugin); }
    }

    // ================================================================
    // 9. LAMENT PILLAR — 32 displays
    //    Wide base + tapered shaft + ornate capital + carved face panels.
    //    Rises in segments, then tips over.
    // ================================================================
    public static class LamentPillar extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> base = new ArrayList<>();
        private final List<BlockDisplayHandle> shaft = new ArrayList<>();
        private final List<BlockDisplayHandle> capital = new ArrayList<>();
        private final List<BlockDisplayHandle> faces = new ArrayList<>();
        private final List<BlockDisplayHandle> crown = new ArrayList<>();
        private boolean tippedOver = false;

        public LamentPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lament_pillar", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(30.0);
            config.setImpactRadius(11.2);
            config.setDurationTicks(380);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_PLACE, 1.4f, 0.4f);

            // Base: 3x3 wide, 2 tall = 9 displays of carved blackstone
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    Location p = center.clone().add(x - 1, 0.5, z - 1);
                    Material mat = (x + z) % 2 == 0 ? Material.CHISELED_POLISHED_BLACKSTONE
                            : Material.POLISHED_BLACKSTONE_BRICKS;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                    h.scale(0.01f, 0.01f, 0.01f).glow(50, 50, 60).interpolation(15, 0);
                    h.animateTo(new Vector3f(-0.45f, 0.5f + (x + z) * 0.5f, -0.45f),
                            new AxisAngle4f(0, 1, 0, 0),
                            new Vector3f(0.95f, 0.95f, 0.95f), 15);
                    base.add(h);
                }
            }
            // Shaft: 2x2 wide, 8 tall — but we use a single 2x2 strip of 8 deepslate brick segments x4 = 32... too many.
            // We do 8 levels x 1 center display = 8 (shaft column)
            for (int i = 0; i < 8; i++) {
                Location p = center.clone().add(0, 1.5 + i * 0.95, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.DEEPSLATE_BRICKS);
                h.scale(0.01f, 0.01f, 0.01f).glow(60, 60, 80).interpolation(15, 30 + i * 4);
                h.animateTo(new Vector3f(-0.85f, 1.5f + i * 0.95f, -0.85f),
                        new AxisAngle4f(0, 1, 0, 0),
                        new Vector3f(1.7f, 0.95f, 1.7f), 15);
                shaft.add(h);
            }
            // Capital: 3x3, 2 tall = 9 displays drop from above
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    Location p = center.clone().add(x - 1, 9.5, z - 1);
                    Material mat = (x == 1 && z == 1) ? Material.SHROOMLIGHT
                            : Material.CHISELED_POLISHED_BLACKSTONE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, mat);
                    h.scale(0.01f, 0.01f, 0.01f).interpolation(15, 70);
                    if (mat == Material.SHROOMLIGHT) h.glow(50, 220, 200);
                    else h.glow(70, 70, 80);
                    h.animateTo(new Vector3f(-0.45f, 9.5f - 0.5f * (x + z) * 0.05f, -0.45f),
                            new AxisAngle4f(0, 1, 0, 0),
                            new Vector3f(0.95f, 0.85f, 0.95f), 15);
                    capital.add(h);
                }
            }
            // 3 crown shroomlights on top (peeking through capital gaps)
            for (int i = 0; i < 3; i++) {
                double a = i * 2 * Math.PI / 3;
                Location p = center.clone().add(Math.cos(a) * 0.7, 11.2, Math.sin(a) * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SHROOMLIGHT);
                h.scale(0.01f, 0.01f, 0.01f).glow(50, 220, 200).interpolation(15, 80);
                h.animateTo(new Vector3f(-0.2f, 11.2f, -0.2f),
                        new AxisAngle4f(0, 1, 0, 0),
                        new Vector3f(0.4f, 0.4f, 0.4f), 15);
                crown.add(h);
            }
            // 3 carved face panels on sides of shaft (light up sequentially)
            for (int i = 0; i < 3; i++) {
                double a = i * 2 * Math.PI / 3;
                Location p = center.clone().add(Math.cos(a) * 1.1, 5, Math.sin(a) * 1.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(0.01f, 0.01f, 0.01f).glow(60, 0, 100).interpolation(15, 90);
                h.animateTo(new Vector3f((float) (Math.cos(a) * 1.1 - 0.3),
                        5f, (float) (Math.sin(a) * 1.1 - 0.3)),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(0.6f, 3.0f, 0.6f), 15);
                faces.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            // Rise sounds during 0-150
            if (tick > 0 && tick < 150 && tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_PLACE, 1.0f, 0.4f);
            }
            // Base eruption AoE
            if (tick == 5) {
                triggerImpactDamage(getCenter().clone().add(0, 1, 0));
                w.spawnParticle(Particle.BLOCK, getCenter(), 60, 2.5, 0.3, 2.5, 0.05,
                        Material.POLISHED_BLACKSTONE.createBlockData());
            }

            // Carved face light-up sequence (after rise) at ticks 160, 175, 190
            if (tick == 160 || tick == 175 || tick == 190) {
                int i = (tick - 160) / 15;
                if (i < faces.size()) {
                    Location l = faces.get(i).entity().getLocation();
                    DisplayBuilder.playSound(l, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.4f);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, l.clone().add(0, 1.5, 0), 25, 0.4, 1.5, 0.4, 0.04);
                    DisplayBuilder.dustParticles(l.clone().add(0, 1.5, 0), 8, 0.4, 60, 0, 100, 1.6f);
                }
            }

            // Tilt 30 degrees at tick 240
            if (tick == 240) {
                tiltAll((float) Math.toRadians(30), 30);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_HIT, 1.4f, 0.3f);
            }

            // Tip over at tick 290
            if (tick == 290 && !tippedOver) {
                tippedOver = true;
                tiltAll((float) Math.toRadians(90), 25);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_IRON_GOLEM_DEATH, 1.8f, 0.3f);
            }

            // Land impact at tick 315
            if (tick == 315) {
                Location landingZone = getCenter().clone().add(7, 0.5, 0);
                DisplayBuilder.playSound(landingZone, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.3f);
                config.setImpactRadius(14.4);
                triggerImpactDamage(landingZone);
                config.setImpactRadius(11.2);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, landingZone, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.BLOCK, landingZone, 100, 5, 0.3, 2, 0.1,
                        Material.DEEPSLATE_BRICKS.createBlockData());
            }

            // Crying obsidian drips down shaft constantly
            if (tick % 4 == 0 && !tippedOver) {
                for (int i = 0; i < 3; i++) {
                    double a = Math.random() * Math.PI * 2;
                    Location drip = getCenter().clone().add(Math.cos(a) * 1.0,
                            1.5 + Math.random() * 7, Math.sin(a) * 1.0);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, drip, 1, 0.1, 0.1, 0.1, 0);
                }
            }
            // Shroomlight glow from top
            if (tick % 5 == 0) {
                Location top = getCenter().clone().add(0, 11.5, 0);
                DisplayBuilder.dustParticles(top, 3, 0.6, 50, 220, 200, 1.4f);
            }
        }

        private void tiltAll(float angleRad, int dur) {
            // Tilt every block around base (Y=0, X-axis tilt to +X)
            // Approximated: animate to tilted positions. We'll just rotate each.
            List<List<BlockDisplayHandle>> all = new ArrayList<>();
            all.add(base); all.add(shaft); all.add(capital); all.add(crown); all.add(faces);
            float cos = (float) Math.cos(angleRad);
            float sin = (float) Math.sin(angleRad);
            for (List<BlockDisplayHandle> group : all) {
                for (BlockDisplayHandle h : group) {
                    Vector3f cur = h.entity().getTransformation().getTranslation();
                    Vector3f scl = h.entity().getTransformation().getScale();
                    // Rotate (cur.x + scl.x/2, cur.y + scl.y/2) about (0,0)
                    float px = cur.x + scl.x / 2f;
                    float py = cur.y + scl.y / 2f;
                    float pz = cur.z + scl.z / 2f;
                    float rx = px * cos + py * sin;
                    float ry = -px * sin + py * cos;
                    h.interpolation(dur, 0);
                    h.animateTo(new Vector3f(rx - scl.x / 2f, ry - scl.y / 2f, pz - scl.z / 2f),
                            new AxisAngle4f(angleRad, 0, 0, 1), scl, dur);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new LamentPillar(plugin); }
    }

    // ================================================================
    // 10. THE DREAMING CLOCK — 32 displays
    //     12 numerals on ring, 2 hands (long/short), shroomlight backing,
    //     hour hand jumps + ring pulse, shatter at 12.
    // ================================================================
    public static class DreamingClock extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> numerals = new ArrayList<>();
        private final List<BlockDisplayHandle> hourHand = new ArrayList<>();
        private final List<BlockDisplayHandle> minuteHand = new ArrayList<>();
        private final List<BlockDisplayHandle> backingGlow = new ArrayList<>();
        private final List<BlockDisplayHandle> faceRing = new ArrayList<>();
        private double minuteAngle = 0;
        private double hourAngle = 0;
        private int hourJumpCount = 0;
        private boolean shattered = false;

        public DreamingClock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dreaming_clock", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(6.8);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.7f);
            // Vertical clock face plane in XZ at Y=4 (we'll make it vertical along XY plane facing +Z)
            // 12 numeral markers (crying obsidian) on a ring of radius 4 in XY plane
            for (int i = 0; i < 12; i++) {
                double a = -Math.PI / 2 + 2 * Math.PI * i / 12; // 12 at top
                double x = Math.cos(a) * 4;
                double y = 4 + Math.sin(a) * 4;
                Location p = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(0.01f, 0.01f, 0.01f).glow(140, 0, 30).interpolation(20, i);
                h.animateTo(new Vector3f((float) x - 0.25f, (float) y - 0.25f, -0.1f),
                        new AxisAngle4f((float) a, 0, 0, 1),
                        new Vector3f(0.5f, 0.5f, 0.2f), 20);
                numerals.add(h);
            }
            // Backing glow — 7 shroomlight blocks behind face for glow-through
            for (int i = 0; i < 7; i++) {
                double a = i * 2 * Math.PI / 7;
                double x = Math.cos(a) * 1.8;
                double y = 4 + Math.sin(a) * 1.8;
                Location p = center.clone().add(x, y, -0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SHROOMLIGHT);
                h.scale(0.01f, 0.01f, 0.01f).glow(50, 220, 200).interpolation(20, 14);
                h.animateTo(new Vector3f((float) x - 0.4f, (float) y - 0.4f, -0.4f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.8f, 0.8f, 0.3f), 20);
                backingGlow.add(h);
            }
            // Face ring — 8 blackstone bricks on outer rim
            for (int i = 0; i < 8; i++) {
                double a = 2 * Math.PI * i / 8;
                double x = Math.cos(a) * 4.6;
                double y = 4 + Math.sin(a) * 4.6;
                Location p = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                h.scale(0.01f, 0.01f, 0.01f).glow(60, 60, 70).interpolation(20, 22);
                h.animateTo(new Vector3f((float) x - 0.4f, (float) y - 0.4f, -0.1f),
                        new AxisAngle4f((float) a, 0, 0, 1),
                        new Vector3f(0.8f, 0.8f, 0.2f), 20);
                faceRing.add(h);
            }
            // Long minute hand — 4 red glass segments
            for (int i = 0; i < 4; i++) {
                Location p = center.clone().add(0, 4 + i * 0.85, 0.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.RED_STAINED_GLASS);
                h.scale(0.01f, 0.01f, 0.01f).glow(255, 30, 30).interpolation(20, 30);
                h.animateTo(new Vector3f(-0.1f, 4f + i * 0.85f, 0.1f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.2f, 0.85f, 0.15f), 20);
                minuteHand.add(h);
            }
            // Short hour hand — 2 red glass segments
            for (int i = 0; i < 2; i++) {
                Location p = center.clone().add(0, 4 + i * 0.85, 0.15);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.RED_STAINED_GLASS);
                h.scale(0.01f, 0.01f, 0.01f).glow(200, 0, 30).interpolation(20, 34);
                h.animateTo(new Vector3f(-0.15f, 4f + i * 0.85f, 0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.3f, 0.85f, 0.15f), 20);
                hourHand.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            if (shattered) return;

            // Minute hand: smooth constant rotation
            minuteAngle += 0.08;

            // Hour hand: jump every 60 ticks
            if (tick > 30 && tick % 60 == 0) {
                hourJumpCount++;
                hourAngle += Math.PI / 6; // 30deg per jump
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CHAIN_FALL, 1.4f, 1.2f);
                // Ring pulse: expanding red dust circle
                for (int radIdx = 0; radIdx < 5; radIdx++) {
                    int finalRad = radIdx;
                    final double finalAngle = minuteAngle;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (getCenter() == null || getCenter().getWorld() == null) return;
                        double r = 3 + finalRad * 1.5;
                        for (int p = 0; p < 36; p++) {
                            double a = 2 * Math.PI * p / 36;
                            Location l = getCenter().clone().add(Math.cos(a) * r, 4 + Math.sin(a) * r, 0);
                            DisplayBuilder.dustParticles(l, 1, 0.1, 255, 30, 30, 1.4f);
                        }
                    }, finalRad * 4L);
                }
                w.spawnParticle(Particle.FLAME, getCenter().clone().add(0, 4, 0.5), 20, 1, 1, 0.3, 0.05);
            }

            // Update minute hand
            if (tick % 2 == 0) {
                animateHand(minuteHand, minuteAngle, 0.85f, 0.2f, 2);
                animateHand(hourHand, hourAngle, 0.85f, 0.3f, 2);
            }

            // Fire at hand tips
            if (tick % 4 == 0) {
                Location tipMin = handTip(minuteAngle, minuteHand.size() * 0.85);
                Location tipHour = handTip(hourAngle, hourHand.size() * 0.85);
                w.spawnParticle(Particle.FLAME, tipMin, 3, 0.15, 0.15, 0.15, 0.02);
                w.spawnParticle(Particle.FLAME, tipHour, 3, 0.15, 0.15, 0.15, 0.02);
            }

            // Trigger shatter when both hands sweep close to 12 simultaneously
            // Or just at fixed tick dur-30
            if (tick == dur - 30 && !shattered) {
                shatter(w);
            }
            // Subtle ambient
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CHAIN_FALL, 0.4f, 1.0f);
            }
        }

        private Location handTip(double angle, double length) {
            // Hand is a vertical strip rotating about center; tip is at offset = length along rotated +Y
            double dx = -Math.sin(angle) * length;
            double dy = Math.cos(angle) * length;
            return getCenter().clone().add(dx, 4 + dy, 0.15);
        }

        private void animateHand(List<BlockDisplayHandle> hand, double angle, float segLen, float width, int dur) {
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            for (int i = 0; i < hand.size(); i++) {
                float along = i * segLen;
                // Origin of segment when angle=0 is (0, along, 0). Rotate about z: (along*-sin, along*cos, 0)
                float rx = -sin * along;
                float ry = cos * along;
                hand.get(i).animateTo(new Vector3f(rx - width / 2f, 4f + ry, 0.1f),
                        new AxisAngle4f((float) angle, 0, 0, 1),
                        new Vector3f(width, segLen, 0.15f), dur);
            }
        }

        private void shatter(World w) {
            shattered = true;
            DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.8f, 0.5f);
            DisplayBuilder.playSound(getCenter(), Sound.BLOCK_GLASS_BREAK, 1.6f, 0.6f);
            w.spawnParticle(Particle.EXPLOSION_EMITTER, getCenter().clone().add(0, 4, 0), 1, 0, 0, 0, 0);

            List<List<BlockDisplayHandle>> all = new ArrayList<>();
            all.add(numerals); all.add(hourHand); all.add(minuteHand);
            all.add(backingGlow); all.add(faceRing);
            for (List<BlockDisplayHandle> group : all) {
                for (BlockDisplayHandle h : group) {
                    Vector3f cur = h.entity().getTransformation().getTranslation();
                    float fx = (float) ((Math.random() - 0.5) * 12);
                    float fy = (float) ((Math.random() - 0.5) * 8);
                    float fz = (float) ((Math.random() - 0.5) * 8);
                    h.interpolation(28, 0);
                    h.animateTo(new Vector3f(cur.x + fx, cur.y + fy, cur.z + fz),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 4), 1, 1, 0),
                            new Vector3f(cur.x, 0.1f, 0.1f),
                            28);
                }
            }
            w.spawnParticle(Particle.BLOCK, getCenter().clone().add(0, 4, 0), 80, 3, 3, 1, 0.1,
                    Material.RED_STAINED_GLASS.createBlockData());

            // Wide AoE damage on shatter
            double oldRadius = config.getDamageRadius();
            config.setDamageRadius(10.0);
            // Trigger continuous damage at higher radius for the shatter window via a one-shot impact-style call:
            // But we use continuous radius mode; we'll just expand it. The base class handles ticking damage.
            // (We don't reset; remaining 30 ticks will keep wider damage.)
        }

        @Override public AbstractAttack newInstance() { return new DreamingClock(plugin); }
    }
}
