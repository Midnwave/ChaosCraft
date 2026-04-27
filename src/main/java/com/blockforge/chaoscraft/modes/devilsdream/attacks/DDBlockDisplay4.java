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
 * DevilsDream Mode — BLOCK DISPLAY ATTACKS (Set 4, 31–40).
 *
 * Ten ornate, multi-phase BlockDisplay structures with full lifecycle:
 * spawn → active → dissipate. Smooth interpolated animations only —
 * no per-tick teleports. Always spawn straight (yaw/pitch zeroed by
 * DisplayBuilder). Impact-only damage attacks call triggerImpactDamage
 * at the precise moment of impact.
 */
public final class DDBlockDisplay4 {
    private DDBlockDisplay4() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BlackIrisBloom(plugin));
        registry.register(new SorrowCageDrop(plugin));
        registry.register(new SigilPentagramSlam(plugin));
        registry.register(new DecayingArch(plugin));
        registry.register(new NightmareOrbit(plugin));
        registry.register(new HollowSermon(plugin));
        registry.register(new PestilenceBloomBurst(plugin));
        registry.register(new InvertedLighthouse(plugin));
        registry.register(new DamnationBellDrop(plugin));
        registry.register(new ForgottenShrine(plugin));
    }

    // ================================================================
    // Helper: trig-stable angles between [0, 2π)
    // ================================================================
    private static float wrap(double a) {
        double t = a % (Math.PI * 2);
        if (t < 0) t += Math.PI * 2;
        return (float) t;
    }

    // ================================================================
    // 31. BLACK IRIS BLOOM
    //     Giant flat overhead flower. 8 outer petals (blackstone), 8
    //     inner petals (obsidian), shroomlight stigma, crying obsidian
    //     sepal base. Contrarotates while descending, slams on impact.
    // ================================================================
    public static class BlackIrisBloom extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> outerPetals = new ArrayList<>();
        private final List<BlockDisplayHandle> innerPetals = new ArrayList<>();
        private final List<BlockDisplayHandle> stigma = new ArrayList<>();
        private final List<BlockDisplayHandle> sepal = new ArrayList<>();
        private float hoverY = 9f;
        private int phase = 0;

        public BlackIrisBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_iris_bloom", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(7.5);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(360);
            config.setCooldownTicks(330);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 1.2f, 0.6f);
            Location top = center.clone().add(0, hoverY, 0);

            // 8 outer blackstone petals (radius 5)
            for (int i = 0; i < 8; i++) {
                double a = i * (Math.PI * 2 / 8);
                Location loc = top.clone().add(Math.cos(a) * 5.0, 0, Math.sin(a) * 5.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(2.4f, 0.4f, 1.2f).glow(140, 0, 30).interpolation(20, 0)
                        .rotate((float) a, 0, 1, 0);
                outerPetals.add(h);
            }
            // 8 inner obsidian petals (radius 3)
            for (int i = 0; i < 8; i++) {
                double a = i * (Math.PI * 2 / 8) + (Math.PI / 8);
                Location loc = top.clone().add(Math.cos(a) * 3.0, -0.2, Math.sin(a) * 3.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.8f, 0.4f, 0.9f).glow(90, 0, 130).interpolation(20, 0)
                        .rotate((float) a, 0, 1, 0);
                innerPetals.add(h);
            }
            // Shroomlight stigma cluster — 5 displays
            double[][] stigmaOff = {{0,0,0},{0.4,0.1,0},{-0.4,0.1,0},{0,0.1,0.4},{0,0.1,-0.4}};
            for (double[] o : stigmaOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        top.clone().add(o[0], o[1], o[2]), Material.SHROOMLIGHT);
                h.scale(0.7f, 0.4f, 0.7f).glow(255, 200, 80).interpolation(15, 0);
                stigma.add(h);
            }
            // Crying obsidian sepal base — 5 displays beneath stigma
            for (int i = 0; i < 5; i++) {
                double a = i * (Math.PI * 2 / 5);
                Location loc = top.clone().add(Math.cos(a) * 1.0, -0.6, Math.sin(a) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.9f, 0.5f, 0.9f).glow(140, 0, 30).interpolation(20, 0);
                sepal.add(h);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            // Phase markers: descend until t=dur*0.7, then slam, then linger
            if (phase == 0 && t >= dur * 0.7) {
                phase = 1;
                // Slam: rotate petals 90° on X like spears
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_RAVAGER_ATTACK, 1.4f, 0.7f);
                for (int i = 0; i < outerPetals.size(); i++) {
                    double a = i * (Math.PI * 2 / 8);
                    float tx = (float) (Math.cos(a) * 5.0);
                    float tz = (float) (Math.sin(a) * 5.0);
                    outerPetals.get(i).animateTo(
                            new Vector3f(tx - 0.5f, -0.5f, tz - 0.5f),
                            new AxisAngle4f((float) (Math.PI / 2), (float) -Math.sin(a), 0, (float) Math.cos(a)),
                            new Vector3f(2.4f, 0.4f, 1.2f), 10);
                }
                for (int i = 0; i < innerPetals.size(); i++) {
                    double a = i * (Math.PI * 2 / 8) + (Math.PI / 8);
                    float tx = (float) (Math.cos(a) * 3.0);
                    float tz = (float) (Math.sin(a) * 3.0);
                    innerPetals.get(i).animateTo(
                            new Vector3f(tx - 0.5f, -0.5f, tz - 0.5f),
                            new AxisAngle4f((float) (Math.PI / 2), (float) -Math.sin(a), 0, (float) Math.cos(a)),
                            new Vector3f(1.8f, 0.4f, 0.9f), 10);
                }
            }

            // Smooth descent + contrarotate during phase 0
            if (phase == 0 && t % 6 == 0) {
                float curY = (float) (hoverY * (1.0 - (double) t / (dur * 0.7)));
                float pulse = (float) (Math.sin(t * 0.12) * 0.4);
                for (int i = 0; i < outerPetals.size(); i++) {
                    double a = i * (Math.PI * 2 / 8) + (t * 0.05); // CW
                    float r = 5.0f + pulse;
                    float tx = (float) (Math.cos(a) * r);
                    float tz = (float) (Math.sin(a) * r);
                    outerPetals.get(i).animateTo(
                            new Vector3f(tx - 0.5f, curY - 0.5f, tz - 0.5f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(2.4f, 0.4f, 1.2f), 6);
                }
                for (int i = 0; i < innerPetals.size(); i++) {
                    double a = i * (Math.PI * 2 / 8) + (Math.PI / 8) - (t * 0.07); // CCW
                    float r = 3.0f - pulse;
                    float tx = (float) (Math.cos(a) * r);
                    float tz = (float) (Math.sin(a) * r);
                    innerPetals.get(i).animateTo(
                            new Vector3f(tx - 0.5f, curY - 0.7f - 0.5f, tz - 0.5f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(1.8f, 0.4f, 0.9f), 6);
                }
                // Stigma + sepal follow
                for (BlockDisplayHandle s : stigma) {
                    s.animateTo(new Vector3f(-0.5f, curY - 0.5f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.7f, 0.4f, 0.7f), 6);
                }
            }

            // Particles
            if (t % 4 == 0) {
                Location pivot = getCenter().clone().add(0, hoverY * (1.0 - Math.min(1.0, (double) t / (dur * 0.7))), 0);
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, pivot, 4, 1.0, 0.2, 1.0, 0);
                DisplayBuilder.dustParticles(pivot, 6, 5.0, 30, 30, 30, 1.4f);
                if (phase == 0 && t % 16 == 0) {
                    DisplayBuilder.dustParticles(pivot, 14, 0.4, 255, 200, 80, 1.6f);
                }
            }

            if (phase == 1 && t == (int) (dur * 0.72)) {
                // Spear-impact damage on each petal location
                Location base = getCenter();
                for (int i = 0; i < 8; i++) {
                    double a = i * (Math.PI * 2 / 8);
                    triggerImpactDamage(base.clone().add(Math.cos(a) * 5, 0, Math.sin(a) * 5));
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new BlackIrisBloom(plugin); }
    }

    // ================================================================
    // 32. SORROW CAGE DROP (impact-only on initial slam + spring-snap)
    // ================================================================
    public static class SorrowCageDrop extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bars = new ArrayList<>();
        private final List<BlockDisplayHandle> floor = new ArrayList<>();
        private final List<BlockDisplayHandle> cap = new ArrayList<>();
        private int phase = 0;

        public SorrowCageDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sorrow_cage_drop", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(280);
            config.setCooldownTicks(310);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(27.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);
            Location top = center.clone().add(0, 18, 0);

            // 9 floor tiles (3x3) — start at top
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            top.clone().add(x, 0, z), Material.DEEPSLATE_TILES);
                    h.scale(1.0f, 0.3f, 1.0f).glow(50, 50, 60).interpolation(8, 0);
                    floor.add(h);
                }
            }
            // 8 vertical bars — corners + midpoints (height 6)
            double[][] barOff = {{1.4,1.4},{1.4,-1.4},{-1.4,1.4},{-1.4,-1.4},
                                  {1.4,0},{-1.4,0},{0,1.4},{0,-1.4}};
            for (double[] o : barOff) {
                Location bottom = top.clone().add(o[0], 0.5, o[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(bottom, Material.BLACKSTONE);
                h.scale(0.4f, 6.0f, 0.4f).glow(60, 0, 0).interpolation(10, 0);
                bars.add(h);
            }
            // Cap: 9 crying obsidian tiles roof (3x3)
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            top.clone().add(x, 7, z), Material.CRYING_OBSIDIAN);
                    h.scale(1.0f, 0.4f, 1.0f).glow(140, 0, 30).interpolation(8, 0);
                    cap.add(h);
                }
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            // Phase 0: Drop. All entities translate down 18 over 12 ticks.
            if (phase == 0 && t == 1) {
                for (BlockDisplayHandle h : floor) {
                    h.animateTo(new Vector3f(-0.5f, -18.0f - 0.5f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0), new Vector3f(1.0f, 0.3f, 1.0f), 12);
                }
                for (int i = 0; i < bars.size(); i++) {
                    bars.get(i).animateTo(new Vector3f(-0.5f, -18.0f - 0.5f + 0.5f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.4f, 6.0f, 0.4f), 12);
                }
                for (BlockDisplayHandle h : cap) {
                    h.animateTo(new Vector3f(-0.5f, -18.0f - 0.5f + 7.0f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0), new Vector3f(1.0f, 0.4f, 1.0f), 12);
                }
                phase = 1;
            }

            // Impact at slam landing (~t=14)
            if (phase == 1 && t == 14) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
                triggerImpactDamage(getCenter());
                w.spawnParticle(Particle.EXPLOSION, getCenter(), 4, 0.5, 0.1, 0.5, 0);
                DisplayBuilder.dustParticles(getCenter(), 40, 2.0, 80, 80, 80, 2.0f);
                phase = 2;
            }

            // Phase 2: Bars bend inward (rotate slightly toward center)
            if (phase == 2 && t == 50) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CHAIN_FALL, 1.0f, 0.7f);
                double[][] barOff = {{1.4,1.4},{1.4,-1.4},{-1.4,1.4},{-1.4,-1.4},
                        {1.4,0},{-1.4,0},{0,1.4},{0,-1.4}};
                for (int i = 0; i < bars.size(); i++) {
                    double angle = Math.atan2(barOff[i][1], barOff[i][0]);
                    bars.get(i).animateTo(
                            new Vector3f((float) (barOff[i][0] * 0.55) - 0.5f, -0.5f + 0.5f, (float) (barOff[i][1] * 0.55) - 0.5f),
                            new AxisAngle4f(0.18f, (float) -Math.sin(angle), 0, (float) Math.cos(angle)),
                            new Vector3f(0.4f, 6.0f, 0.4f), 30);
                }
                phase = 3;
            }

            // Phase 3: Spring snap outward
            if (phase == 3 && t == 110) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 1.2f);
                double[][] barOff = {{1.4,1.4},{1.4,-1.4},{-1.4,1.4},{-1.4,-1.4},
                        {1.4,0},{-1.4,0},{0,1.4},{0,-1.4}};
                for (int i = 0; i < bars.size(); i++) {
                    bars.get(i).animateTo(
                            new Vector3f((float) (barOff[i][0] * 2.4) - 0.5f, -0.5f + 0.5f, (float) (barOff[i][1] * 2.4) - 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.4f, 6.0f, 0.4f), 6);
                }
                // Radial snap impact
                triggerImpactDamage(getCenter());
                phase = 4;
            }

            // Phase 4: Sink wreckage into the ground (dissipate) — NOT rise
            if (phase == 4 && t == 180) {
                for (BlockDisplayHandle h : floor) {
                    h.animateTo(new Vector3f(-0.5f, -19.0f - 0.5f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.5f, 0.15f, 0.5f), 60);
                }
                for (int i = 0; i < bars.size(); i++) {
                    double[][] barOff = {{1.4,1.4},{1.4,-1.4},{-1.4,1.4},{-1.4,-1.4},
                            {1.4,0},{-1.4,0},{0,1.4},{0,-1.4}};
                    bars.get(i).animateTo(
                            new Vector3f((float) (barOff[i][0] * 2.4) - 0.5f, -19.0f - 0.5f + 0.5f, (float) (barOff[i][1] * 2.4) - 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.1f, 3.0f, 0.1f), 60);
                }
                for (BlockDisplayHandle h : cap) {
                    // cap entity is spawned 7 higher than floor, so subtract additional 7 to also sink it
                    h.animateTo(new Vector3f(-0.5f, -19.0f - 0.5f - 7.0f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.5f, 0.2f, 0.5f), 60);
                }
                phase = 5;
            }

            // Drip particles inside cage while trapped
            if (phase >= 1 && phase < 4 && t % 5 == 0) {
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, getCenter().clone().add(0, 4, 0),
                        6, 1.2, 2.5, 1.2, 0);
            }
        }

        @Override
        public AbstractAttack newInstance() { return new SorrowCageDrop(plugin); }
    }

    // ================================================================
    // 33. SIGIL PENTAGRAM SLAM (impact-only on collapse)
    //     5 sigil runes arranged in a pentagram, hovering high above.
    //     Energy beams (deepslate-tile pillars) connect the runes
    //     forming a 5-pointed star. Whole geometry rotates, then the
    //     entire structure collapses straight downward as a slam.
    // ================================================================
    public static class SigilPentagramSlam extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> runes = new ArrayList<>();
        private final List<BlockDisplayHandle> beams = new ArrayList<>();
        private final List<BlockDisplayHandle> coreRing = new ArrayList<>();
        private static final int RUNE_COUNT = 5;
        private static final double PENT_RADIUS = 5.5;
        private static final float HOVER_Y = 12f;
        private int phase = 0;

        public SigilPentagramSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sigil_pentagram_slam", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(380);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(30.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.2f, 0.5f);
            Location top = center.clone().add(0, HOVER_Y, 0);

            // 5 sigil runes (shroomlight) at pentagram vertices, pointing up
            for (int i = 0; i < RUNE_COUNT; i++) {
                double a = -Math.PI / 2 + i * (Math.PI * 2 / RUNE_COUNT);
                double rx = Math.cos(a) * PENT_RADIUS;
                double rz = Math.sin(a) * PENT_RADIUS;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        top.clone().add(rx, 0, rz), Material.SHROOMLIGHT);
                h.scale(1.4f, 0.4f, 1.4f).glow(255, 80, 30).interpolation(20, 0);
                runes.add(h);
            }

            // Connecting energy beams: pentagram star pattern (i -> i+2 mod 5)
            // Each beam is a thin horizontal deepslate_tiles strip
            for (int i = 0; i < RUNE_COUNT; i++) {
                int j = (i + 2) % RUNE_COUNT;
                double a1 = -Math.PI / 2 + i * (Math.PI * 2 / RUNE_COUNT);
                double a2 = -Math.PI / 2 + j * (Math.PI * 2 / RUNE_COUNT);
                double x1 = Math.cos(a1) * PENT_RADIUS;
                double z1 = Math.sin(a1) * PENT_RADIUS;
                double x2 = Math.cos(a2) * PENT_RADIUS;
                double z2 = Math.sin(a2) * PENT_RADIUS;
                double mx = (x1 + x2) / 2.0;
                double mz = (z1 + z2) / 2.0;
                double dx = x2 - x1;
                double dz = z2 - z1;
                double len = Math.sqrt(dx * dx + dz * dz);
                double yaw = Math.atan2(dz, dx);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        top.clone().add(mx, -0.05, mz), Material.CRYING_OBSIDIAN);
                h.scale((float) len, 0.18f, 0.35f).glow(140, 0, 30).interpolation(20, 0)
                        .rotate((float) yaw, 0, 1, 0);
                beams.add(h);
            }

            // Core ring: 8 blackstone segments around centerpoint
            for (int i = 0; i < 8; i++) {
                double a = i * (Math.PI * 2 / 8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        top.clone().add(Math.cos(a) * 1.5, 0.0, Math.sin(a) * 1.5), Material.BLACKSTONE);
                h.scale(0.6f, 0.25f, 0.6f).glow(60, 0, 0).interpolation(20, 0)
                        .rotate((float) a, 0, 1, 0);
                coreRing.add(h);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            int slamTick = (int) (dur * 0.55); // collapse begins
            int impactTick = slamTick + 14;     // ground contact

            // Phase 0: hover and rotate the entire sigil structure
            if (phase == 0 && t < slamTick && t % 6 == 0) {
                float rotAngle = (float) (t * 0.04);
                float bob = (float) Math.sin(t * 0.08) * 0.4f;
                float curY = HOVER_Y + bob;

                // Spin runes around the center while orbiting
                for (int i = 0; i < runes.size(); i++) {
                    double a = -Math.PI / 2 + i * (Math.PI * 2 / RUNE_COUNT) + rotAngle;
                    float rx = (float) (Math.cos(a) * PENT_RADIUS);
                    float rz = (float) (Math.sin(a) * PENT_RADIUS);
                    runes.get(i).animateTo(
                            new Vector3f(rx - 0.5f, curY - 0.5f, rz - 0.5f),
                            new AxisAngle4f(rotAngle * 2f, 0, 1, 0),
                            new Vector3f(1.4f, 0.4f, 1.4f), 6);
                }

                // Beams follow rotating runes
                for (int i = 0; i < beams.size(); i++) {
                    int j = (i + 2) % RUNE_COUNT;
                    double a1 = -Math.PI / 2 + i * (Math.PI * 2 / RUNE_COUNT) + rotAngle;
                    double a2 = -Math.PI / 2 + j * (Math.PI * 2 / RUNE_COUNT) + rotAngle;
                    double x1 = Math.cos(a1) * PENT_RADIUS;
                    double z1 = Math.sin(a1) * PENT_RADIUS;
                    double x2 = Math.cos(a2) * PENT_RADIUS;
                    double z2 = Math.sin(a2) * PENT_RADIUS;
                    float mx = (float) ((x1 + x2) / 2.0);
                    float mz = (float) ((z1 + z2) / 2.0);
                    double dx = x2 - x1;
                    double dz = z2 - z1;
                    double len = Math.sqrt(dx * dx + dz * dz);
                    float yaw = (float) Math.atan2(dz, dx);
                    beams.get(i).animateTo(
                            new Vector3f(mx - 0.5f, curY - 0.05f - 0.5f, mz - 0.5f),
                            new AxisAngle4f(yaw, 0, 1, 0),
                            new Vector3f((float) len, 0.18f, 0.35f), 6);
                }

                // Core ring counter-rotates
                for (int i = 0; i < coreRing.size(); i++) {
                    double a = i * (Math.PI * 2 / 8) - rotAngle * 1.5;
                    float rx = (float) (Math.cos(a) * 1.5);
                    float rz = (float) (Math.sin(a) * 1.5);
                    coreRing.get(i).animateTo(
                            new Vector3f(rx - 0.5f, curY - 0.5f, rz - 0.5f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(0.6f, 0.25f, 0.6f), 6);
                }
            }

            // Charge particles during hover phase
            if (phase == 0 && t % 5 == 0) {
                Location pivot = getCenter().clone().add(0, HOVER_Y, 0);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, pivot, 8, 3.0, 0.2, 3.0, 0.01);
                DisplayBuilder.dustParticles(pivot, 12, 5.0, 200, 30, 30, 1.6f);
            }

            // Phase 1: collapse straight down
            if (phase == 0 && t == slamTick) {
                phase = 1;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WITHER_SHOOT, 1.4f, 0.6f);

                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(runes); all.addAll(beams); all.addAll(coreRing);
                for (BlockDisplayHandle h : all) {
                    Vector3f tr = h.entity().getTransformation().getTranslation();
                    Vector3f sc = h.entity().getTransformation().getScale();
                    h.animateTo(
                            new Vector3f(tr.x, tr.y - HOVER_Y, tr.z),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(sc.x, sc.y, sc.z),
                            14);
                }
            }

            // Trail particles during collapse
            if (phase == 1 && t > slamTick && t < impactTick && t % 2 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                        getCenter().clone().add(0, HOVER_Y * (1.0 - (t - slamTick) / 14.0), 0),
                        14, 4.0, 0.3, 4.0, 0.02);
            }

            // Phase 2: impact slam
            if (phase == 1 && t == impactTick) {
                phase = 2;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.4f);
                triggerImpactDamage(getCenter());
                w.spawnParticle(Particle.EXPLOSION_EMITTER, getCenter(), 4, 2.0, 0.2, 2.0, 0);
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 60, 30), 2.0f);
                DisplayBuilder.particleRing(getCenter(), PENT_RADIUS, Particle.DUST, 60, dust);

                // Splay outward
                for (int i = 0; i < runes.size(); i++) {
                    Vector3f tr = runes.get(i).entity().getTransformation().getTranslation();
                    runes.get(i).animateTo(
                            new Vector3f(tr.x * 1.4f, tr.y, tr.z * 1.4f),
                            new AxisAngle4f((float) Math.PI / 2f, 1, 0, 0),
                            new Vector3f(1.4f, 0.2f, 1.4f), 18);
                }
            }

            // Phase 3: linger then fade
            if (phase == 2 && t == impactTick + 70) {
                phase = 3;
                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(runes); all.addAll(beams); all.addAll(coreRing);
                for (BlockDisplayHandle h : all) {
                    Vector3f tr = h.entity().getTransformation().getTranslation();
                    h.animateTo(
                            new Vector3f(tr.x, tr.y, tr.z),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.0f, 0.0f, 0.0f), 30);
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new SigilPentagramSlam(plugin); }
    }

    // ================================================================
    // 34. DECAYING ARCH (impact-only on full topple)
    // ================================================================
    public static class DecayingArch extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftPillar = new ArrayList<>();
        private final List<BlockDisplayHandle> rightPillar = new ArrayList<>();
        private final List<BlockDisplayHandle> arch = new ArrayList<>();
        private final List<BlockDisplayHandle> mold = new ArrayList<>();
        private final List<BlockDisplayHandle> cracks = new ArrayList<>();
        private int phase = 0;

        public DecayingArch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("decaying_arch", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(420);
            config.setCooldownTicks(360);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(25.5);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            // Two pillars (each 6 tall, 2 wide)
            for (int y = 0; y < 6; y++) {
                for (int x = 0; x < 2; x++) {
                    BlockDisplayHandle l = displayBuilder.spawnBlock(
                            center.clone().add(-3 + x, y, 0), Material.DEEPSLATE_BRICKS);
                    l.scale(1.0f, 1.0f, 1.0f).interpolation(15, 0);
                    leftPillar.add(l);
                    BlockDisplayHandle r = displayBuilder.spawnBlock(
                            center.clone().add(2 + x, y, 0), Material.DEEPSLATE_BRICKS);
                    r.scale(1.0f, 1.0f, 1.0f).interpolation(15, 0);
                    rightPillar.add(r);
                }
            }
            // Curved arch (10 displays following arc)
            for (int i = 0; i < 10; i++) {
                double t = (double) i / 9;
                double angle = Math.PI * (1 - t);
                double ax = Math.cos(angle) * 3;
                double ay = 6 + Math.sin(angle) * 2;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(ax, ay, 0), Material.DEEPSLATE_BRICKS);
                h.scale(0.9f, 0.9f, 0.9f).interpolation(15, 0)
                        .rotate((float) (angle - Math.PI / 2), 0, 0, 1);
                arch.add(h);
            }
            // Sculk mold patches
            double[][] moldOff = {{-3,2,0.5},{3,4,0.5},{-2.5,5,-0.4},{0,8,0.3},{2.8,1.5,-0.4},{-1.5,7,0.4}};
            for (double[] o : moldOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(o[0], o[1], o[2]), Material.SCULK);
                h.scale(0.6f, 0.6f, 0.3f).glow(50, 220, 200).interpolation(15, 0);
                mold.add(h);
            }
            // Crying obsidian crack lines (4 displays)
            double[][] crackOff = {{-3,3,0.5},{2.5,2,0.5},{0,7.5,0.5},{-2.8,4.5,0.5}};
            for (double[] o : crackOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(o[0], o[1], o[2]), Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 1.5f, 0.2f).glow(140, 0, 30).interpolation(15, 0);
                cracks.add(h);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            // Phase 0: crumble chunks (left+right pillar outer halves) over t=40-200
            if (phase == 0) {
                int crumbleIdx = (t - 40) / 14;
                int total = Math.min(leftPillar.size(), 6);
                if (crumbleIdx >= 0 && crumbleIdx < total && (t - 40) % 14 == 0) {
                    BlockDisplayHandle target = (crumbleIdx % 2 == 0)
                            ? leftPillar.get(crumbleIdx % leftPillar.size())
                            : rightPillar.get(crumbleIdx % rightPillar.size());
                    target.animateTo(new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f((float) Math.random(), 1, 1, 0),
                            new Vector3f(0.0f, 0.0f, 0.0f), 12);
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.6f);
                    w.spawnParticle(Particle.BLOCK, target.entity().getLocation(), 30,
                            0.5, 0.5, 0.5, 0, Material.DEEPSLATE_BRICKS.createBlockData());
                }
                if (t >= 220) phase = 1;
            }

            // Phase 1: leaning starts at t=220
            if (phase == 1 && t >= 220 && t < 320 && t % 6 == 0) {
                float lean = (t - 220) / 100f * 0.4f;
                for (BlockDisplayHandle h : arch) {
                    Vector3f tr = h.entity().getTransformation().getTranslation();
                    h.animateTo(new Vector3f(tr.x, tr.y, tr.z),
                            new AxisAngle4f(lean, 0, 0, 1),
                            new Vector3f(0.9f, 0.9f, 0.9f), 6);
                }
                if (t == 222) {
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_DEEPSLATE_HIT, 1.0f, 0.4f);
                }
            }

            // Phase 2: full topple at t=320
            if (phase == 1 && t == 320) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_IRON_GOLEM_DEATH, 1.5f, 0.6f);
                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(leftPillar); all.addAll(rightPillar); all.addAll(arch);
                all.addAll(mold); all.addAll(cracks);
                for (BlockDisplayHandle h : all) {
                    Vector3f tr = h.entity().getTransformation().getTranslation();
                    h.animateTo(new Vector3f(tr.x + 6, -0.5f, tr.z),
                            new AxisAngle4f((float) (Math.PI / 2), 0, 0, 1),
                            h.entity().getTransformation().getScale(), 18);
                }
                triggerImpactDamage(getCenter().clone().add(3, 0, 0));
                triggerImpactDamage(getCenter().clone().add(6, 0, 0));
                w.spawnParticle(Particle.SCULK_SOUL, getCenter().clone().add(0, 1, 0), 20, 4, 1, 4, 0);
                phase = 2;
            }

            // Particles: stone dust falling, crying obsidian seep, sculk pulse
            if (t % 6 == 0) {
                Location p = getCenter().clone().add(0, 5, 0);
                w.spawnParticle(Particle.BLOCK, p, 8, 3, 3, 0.5, 0, Material.DEEPSLATE_BRICKS.createBlockData());
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, p, 3, 3, 3, 0.5, 0);
            }
        }

        @Override
        public AbstractAttack newInstance() { return new DecayingArch(plugin); }
    }

    // ================================================================
    // 35. NIGHTMARE ORBIT (continuous radius)
    // ================================================================
    public static class NightmareOrbit extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> core = new ArrayList<>();
        private final List<BlockDisplayHandle> innerPlanet = new ArrayList<>();
        private final List<BlockDisplayHandle> outerPlanet = new ArrayList<>();
        private final List<BlockDisplayHandle> innerMoon = new ArrayList<>();
        private final List<BlockDisplayHandle> outerMoon = new ArrayList<>();
        private final List<BlockDisplayHandle> asteroids = new ArrayList<>();
        private int pulseTick = -999;

        public NightmareOrbit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_orbit", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(8.3);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(420);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.5f);
            // Crying obsidian core 4x4 cluster (16 blocks at center, stacked compactly)
            for (int x = -1; x <= 0; x++) for (int y = 0; y <= 1; y++) for (int z = -1; z <= 0; z++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y + 4, z), Material.CRYING_OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(90, 0, 130).interpolation(15, 0);
                core.add(h);
            }
            // Inner planet (radius 4) — 3 obsidian blocks for body
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(4, 4 + i * 0.4, 0), Material.OBSIDIAN);
                h.scale(0.9f, 0.9f, 0.9f).glow(40, 0, 60).interpolation(8, 0);
                innerPlanet.add(h);
            }
            // Inner moon (calcite)
            BlockDisplayHandle iM = displayBuilder.spawnBlock(
                    center.clone().add(5.5, 4.4, 0), Material.CALCITE);
            iM.scale(0.4f, 0.4f, 0.4f).glow(255, 255, 255).interpolation(6, 0);
            innerMoon.add(iM);
            // Outer planet (radius 7) — 3 obsidian blocks
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(7, 4 - i * 0.4, 0), Material.OBSIDIAN);
                h.scale(1.1f, 1.1f, 1.1f).glow(60, 0, 90).interpolation(8, 0);
                outerPlanet.add(h);
            }
            // Outer moon
            BlockDisplayHandle oM = displayBuilder.spawnBlock(
                    center.clone().add(8.8, 4, 0), Material.CALCITE);
            oM.scale(0.4f, 0.4f, 0.4f).glow(255, 255, 255).interpolation(6, 0);
            outerMoon.add(oM);
            // Asteroid belt — 12 between
            for (int i = 0; i < 12; i++) {
                double a = i * (Math.PI * 2 / 12);
                double r = 5.5 + (i % 3) * 0.4;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(a) * r, 4 + (i % 2) * 0.5, Math.sin(a) * r),
                        Material.BLACKSTONE);
                h.scale(0.5f, 0.5f, 0.5f).interpolation(8, 0);
                asteroids.add(h);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Pulse every 80 ticks
            if (t % 80 == 60) {
                pulseTick = t;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.8f);
            }
            float pulseFactor = 1.0f;
            if (t - pulseTick < 16 && t - pulseTick >= 0) {
                pulseFactor = 1.0f - (16 - (t - pulseTick)) / 16f * 0.5f;
            }

            if (t % 3 == 0) {
                double innerSpeed = 0.10;
                double outerSpeed = 0.05;

                // Inner planet orbit
                for (int i = 0; i < innerPlanet.size(); i++) {
                    double a = t * innerSpeed + i * 0.2;
                    double r = 4.0 * pulseFactor;
                    innerPlanet.get(i).animateTo(
                            new Vector3f((float) (Math.cos(a) * r) - 0.5f, 4 + i * 0.4f - 0.5f, (float) (Math.sin(a) * r) - 0.5f),
                            new AxisAngle4f((float) (t * 0.05), 0, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.9f), 3);
                }
                // Inner moon
                for (int i = 0; i < innerMoon.size(); i++) {
                    double a = t * innerSpeed;
                    double ma = t * 0.25;
                    double cx = Math.cos(a) * 4 * pulseFactor;
                    double cz = Math.sin(a) * 4 * pulseFactor;
                    double mx = cx + Math.cos(ma) * 1.2;
                    double mz = cz + Math.sin(ma) * 1.2;
                    innerMoon.get(i).animateTo(
                            new Vector3f((float) mx - 0.5f, 4.4f - 0.5f, (float) mz - 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.4f, 0.4f, 0.4f), 3);
                }
                // Outer planet
                for (int i = 0; i < outerPlanet.size(); i++) {
                    double a = t * outerSpeed + Math.PI + i * 0.2;
                    double r = 7.0 * pulseFactor;
                    outerPlanet.get(i).animateTo(
                            new Vector3f((float) (Math.cos(a) * r) - 0.5f, 4 - i * 0.4f - 0.5f, (float) (Math.sin(a) * r) - 0.5f),
                            new AxisAngle4f((float) (-t * 0.04), 0, 1, 0),
                            new Vector3f(1.1f, 1.1f, 1.1f), 3);
                }
                for (int i = 0; i < outerMoon.size(); i++) {
                    double a = t * outerSpeed + Math.PI;
                    double ma = -t * 0.18;
                    double cx = Math.cos(a) * 7 * pulseFactor;
                    double cz = Math.sin(a) * 7 * pulseFactor;
                    double mx = cx + Math.cos(ma) * 1.8;
                    double mz = cz + Math.sin(ma) * 1.8;
                    outerMoon.get(i).animateTo(
                            new Vector3f((float) mx - 0.5f, 4.0f - 0.5f, (float) mz - 0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.4f, 0.4f, 0.4f), 3);
                }
                // Asteroids — chaotic loose paths
                for (int i = 0; i < asteroids.size(); i++) {
                    double a = t * 0.07 + i * (Math.PI / 4);
                    double r = (5.0 + Math.sin(t * 0.08 + i) * 0.8) * pulseFactor;
                    asteroids.get(i).animateTo(
                            new Vector3f((float) (Math.cos(a) * r) - 0.5f, 4 + (i % 2) * 0.5f - 0.5f, (float) (Math.sin(a) * r) - 0.5f),
                            new AxisAngle4f((float) (t * 0.15 + i), 1, 1, 1),
                            new Vector3f(0.5f, 0.5f, 0.5f), 3);
                }
            }
            if (t % 4 == 0) {
                w.spawnParticle(Particle.PORTAL, getCenter().clone().add(0, 4, 0), 6, 0.5, 0.5, 0.5, 0.1);
            }
            if (t % 6 == 0) {
                for (BlockDisplayHandle a : asteroids) {
                    DisplayBuilder.dustParticles(a.entity().getLocation(), 2, 0.3, 90, 90, 90, 1.0f);
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new NightmareOrbit(plugin); }
    }

    // ================================================================
    // 36. THE HOLLOW SERMON (continuous rings + final beam)
    // ================================================================
    public static class HollowSermon extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> base = new ArrayList<>();
        private final List<BlockDisplayHandle> face = new ArrayList<>();
        private final List<BlockDisplayHandle> lectern = new ArrayList<>();
        private final List<BlockDisplayHandle> glowSides = new ArrayList<>();
        private final List<BlockDisplayHandle> beam = new ArrayList<>();
        private int phase = 0;

        public HollowSermon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hollow_sermon", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(6.8);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.6f);
            // Squat wide base 4x3x2 (24 blocks) — start under ground for rise
            for (int x = -2; x < 2; x++) for (int y = -2; y < 0; y++) for (int z = -1; z < 2; z++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y, z), Material.BLACKSTONE);
                h.scale(1.0f, 1.0f, 1.0f).interpolation(20, 0);
                base.add(h);
            }
            // Carved blackstone face panel (front, 4x2)
            for (int x = -2; x < 2; x++) for (int y = -2; y < 0; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y, 1.6), Material.CHISELED_POLISHED_BLACKSTONE);
                h.scale(1.0f, 1.0f, 0.2f).glow(140, 0, 30).interpolation(20, 0);
                face.add(h);
            }
            // Bone block lectern top (4x3 flat)
            for (int x = -2; x < 2; x++) for (int z = -1; z < 2; z++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0, z), Material.BONE_BLOCK);
                h.scale(1.0f, 0.3f, 1.0f).glow(240, 230, 200).interpolation(20, 0);
                lectern.add(h);
            }
            // Shroomlight glow sides (2 panels — left and right)
            for (int s : new int[]{-2, 1}) {
                for (int y = -2; y < 0; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(s, y, 0.5), Material.SHROOMLIGHT);
                    h.scale(0.2f, 1.0f, 1.0f).glow(255, 200, 80).interpolation(20, 0);
                    glowSides.add(h);
                }
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Phase 0: rise from ground over first 30 ticks
            if (phase == 0 && t == 1) {
                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(base); all.addAll(face); all.addAll(lectern); all.addAll(glowSides);
                for (BlockDisplayHandle h : all) {
                    Vector3f tr = h.entity().getTransformation().getTranslation();
                    h.animateTo(new Vector3f(tr.x, tr.y + 2.0f, tr.z),
                            new AxisAngle4f(0, 0, 1, 0),
                            h.entity().getTransformation().getScale(), 30);
                }
                phase = 1;
            }

            // Phase 1: lectern vibrate + ring pulses
            if (phase == 1 && t > 40) {
                if (t % Math.max(8, 30 - t / 20) == 0) {
                    Location pulseLoc = getCenter().clone().add(0, 2, 0);
                    int points = 28;
                    double r = ((t - 40) % 40) * 0.3 + 1;
                    Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 200, 80), 1.6f);
                    DisplayBuilder.particleRing(pulseLoc, r, Particle.DUST, points, dust);
                }
                if (t % 4 == 0) {
                    for (BlockDisplayHandle l : lectern) {
                        float jitter = (float) (Math.sin(t * 0.6) * 0.05);
                        Vector3f tr = l.entity().getTransformation().getTranslation();
                        l.animateTo(new Vector3f(tr.x, 2.0f + jitter - 0.5f, tr.z),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(1.0f, 0.3f, 1.0f), 4);
                    }
                }
            }

            // Phase 2: lurch forward + fire column at t=260
            if (phase == 1 && t == 260) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.6f, 0.7f);
                // Lurch
                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(base); all.addAll(face); all.addAll(lectern); all.addAll(glowSides);
                for (BlockDisplayHandle h : all) {
                    Vector3f tr = h.entity().getTransformation().getTranslation();
                    h.animateTo(new Vector3f(tr.x, tr.y, tr.z + 1.0f),
                            new AxisAngle4f(0, 0, 1, 0),
                            h.entity().getTransformation().getScale(), 6);
                }
                // Fire column — 8 red glass segments forward
                for (int i = 0; i < 8; i++) {
                    BlockDisplayHandle b = displayBuilder.spawnBlock(
                            getCenter().clone().add(0, 1, 2 + i * 1.5), Material.RED_STAINED_GLASS);
                    b.scale(1.5f, 1.5f, 1.5f).glow(255, 30, 30).interpolation(8, 0);
                    beam.add(b);
                }
                phase = 2;
            }

            // Beam particles
            if (phase >= 2 && t % 2 == 0) {
                Location s = getCenter().clone().add(0, 2, 1.5);
                Location e = getCenter().clone().add(0, 2, 14);
                DisplayBuilder.particleLine(s, e, Particle.SOUL_FIRE_FLAME, 4, null);
            }

            // Soul fire on lectern
            if (t % 3 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, getCenter().clone().add(0, 2.5, 0), 6, 1.5, 0.2, 1.0, 0.01);
            }
        }

        @Override
        public AbstractAttack newInstance() { return new HollowSermon(plugin); }
    }

    // ================================================================
    // 37. PESTILENCE BLOOM BURST (continuous radius — 3 mushrooms)
    // ================================================================
    public static class PestilenceBloomBurst extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> roots = new ArrayList<>();
        private final List<BlockDisplayHandle> stalks = new ArrayList<>();
        private final List<BlockDisplayHandle> caps = new ArrayList<>();
        private final List<BlockDisplayHandle> pores = new ArrayList<>();
        private int phase = 0;

        public PestilenceBloomBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pestilence_bloom_burst", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(7.5);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            // 3 mushrooms at offsets
            double[][] mushroomOff = {{-5, 0, -3}, {4, 0, 1}, {-1, 0, 5}};
            for (double[] mo : mushroomOff) {
                Location base = center.clone().add(mo[0], mo[1], mo[2]);
                // Root: 2x2 (4 blocks)
                for (int rx = 0; rx < 2; rx++) for (int rz = 0; rz < 2; rz++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            base.clone().add(rx, 0, rz), Material.BLACKSTONE);
                    h.scale(1.0f, 0.4f, 1.0f).interpolation(20, 0);
                    roots.add(h);
                }
                // Stalk: 1x1 tall (1 column, 0 height initially)
                BlockDisplayHandle s = displayBuilder.spawnBlock(
                        base.clone().add(0.5, 0.5, 0.5), Material.WARPED_PLANKS);
                s.scale(0.6f, 0.1f, 0.6f).interpolation(20, 0);
                stalks.add(s);
                // Cap (3x3x2 — top half) — 1 cap unit per mushroom acting as inflatable
                BlockDisplayHandle c = displayBuilder.spawnBlock(
                        base.clone().add(0.5, 1.5, 0.5), Material.SCULK);
                c.scale(0.1f, 0.1f, 0.1f).glow(50, 220, 200).interpolation(20, 0);
                caps.add(c);
                // 5 pores under each cap
                for (int i = 0; i < 5; i++) {
                    double a = i * (Math.PI * 2 / 5);
                    BlockDisplayHandle p = displayBuilder.spawnBlock(
                            base.clone().add(0.5 + Math.cos(a) * 0.4, 1.2, 0.5 + Math.sin(a) * 0.4),
                            Material.NETHER_WART_BLOCK);
                    p.scale(0.3f, 0.3f, 0.3f).glow(140, 0, 30).interpolation(15, 0);
                    pores.add(p);
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Phase 0: grow stalks over 40 ticks
            if (phase == 0 && t == 5) {
                for (BlockDisplayHandle s : stalks) {
                    Vector3f tr = s.entity().getTransformation().getTranslation();
                    s.animateTo(new Vector3f(tr.x, tr.y, tr.z),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.6f, 2.5f, 0.6f), 40);
                }
                phase = 1;
            }
            // Phase 1: inflate caps over 40 ticks (t=50-90)
            if (phase == 1 && t == 50) {
                for (BlockDisplayHandle c : caps) {
                    Vector3f tr = c.entity().getTransformation().getTranslation();
                    c.animateTo(new Vector3f(tr.x, tr.y, tr.z),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(2.5f, 1.4f, 2.5f), 40);
                }
                phase = 2;
            }
            // Phase 2: vibrate then BURST at t=170 (offset per mushroom)
            if (phase == 2 && t > 100 && t % 4 == 0) {
                for (int i = 0; i < caps.size(); i++) {
                    Vector3f tr = caps.get(i).entity().getTransformation().getTranslation();
                    float j = (float) (Math.sin(t * 0.8 + i) * 0.1);
                    caps.get(i).animateTo(new Vector3f(tr.x + j, tr.y, tr.z),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(2.5f, 1.4f, 2.5f), 4);
                }
            }

            if (phase == 2 && t == 170) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GHAST_SHOOT, 1.4f, 0.6f);
                // Caps split — top half translates up
                for (BlockDisplayHandle c : caps) {
                    Vector3f tr = c.entity().getTransformation().getTranslation();
                    c.animateTo(new Vector3f(tr.x, tr.y + 3, tr.z),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(2.5f, 0.4f, 2.5f), 14);
                }
                // Pores fly outward radially
                double[][] mushroomOff = {{-5, 0, -3}, {4, 0, 1}, {-1, 0, 5}};
                for (int m = 0; m < 3; m++) {
                    for (int i = 0; i < 5; i++) {
                        double a = i * (Math.PI * 2 / 5);
                        int idx = m * 5 + i;
                        if (idx >= pores.size()) continue;
                        pores.get(idx).animateTo(
                                new Vector3f((float) (mushroomOff[m][0] + 0.5 + Math.cos(a) * 4) - 0.5f,
                                        1.2f - 0.5f,
                                        (float) (mushroomOff[m][2] + 0.5 + Math.sin(a) * 4) - 0.5f),
                                new AxisAngle4f((float) (t * 0.2), 1, 1, 0),
                                new Vector3f(0.6f, 0.6f, 0.6f), 16);
                    }
                }
                phase = 3;
            }

            // Sculk charge particles before burst
            if (phase == 2 && t % 5 == 0 && t > 130) {
                for (BlockDisplayHandle c : caps) {
                    w.spawnParticle(Particle.SCULK_CHARGE_POP, c.entity().getLocation(), 5, 0.5, 0.3, 0.5, 0.05);
                }
            }
            if (phase == 3 && t % 4 == 0) {
                for (BlockDisplayHandle p : pores) {
                    w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, p.entity().getLocation(), 3, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new PestilenceBloomBurst(plugin); }
    }

    // ================================================================
    // 38. INVERTED LIGHTHOUSE (impact-only sweeping beam)
    // ================================================================
    public static class InvertedLighthouse extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> tower = new ArrayList<>();
        private final List<BlockDisplayHandle> lightRoom = new ArrayList<>();
        private final List<BlockDisplayHandle> beacon = new ArrayList<>();
        private final List<BlockDisplayHandle> trim = new ArrayList<>();
        private final List<BlockDisplayHandle> beamPanel = new ArrayList<>();
        private int phase = 0;

        public InvertedLighthouse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inverted_lighthouse", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(440);
            config.setCooldownTicks(360);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.5);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SHROOMLIGHT_STEP, 1.0f, 0.5f);
            // Tower body (8 tall taper) — TOP narrows to point (inverted), bottom is wide
            // Order: tip at top, wide at light room
            for (int y = 0; y < 8; y++) {
                double scaleXZ = 0.3 + (y * 0.18); // wider at base (=lower)
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, 14 - y, 0), Material.BLACKSTONE);
                h.scale((float) scaleXZ, 1.0f, (float) scaleXZ).glow(60, 0, 0).interpolation(15, 0);
                tower.add(h);
            }
            // Light room — 3x3x2 cylinder of red glass (point=up, room at bottom)
            for (int y = 0; y < 2; y++) {
                for (int i = 0; i < 8; i++) {
                    double a = i * (Math.PI * 2 / 8);
                    Location loc = center.clone().add(Math.cos(a) * 1.5, 4 + y, Math.sin(a) * 1.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_STAINED_GLASS);
                    h.scale(0.5f, 1.0f, 0.5f).glow(255, 30, 30).interpolation(15, 0)
                            .rotate((float) a, 0, 1, 0);
                    lightRoom.add(h);
                }
            }
            // Beacon shroomlight core
            for (int y = 0; y < 2; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, 4 + y, 0), Material.SHROOMLIGHT);
                h.scale(0.8f, 0.8f, 0.8f).glow(255, 200, 80).interpolation(15, 0);
                beacon.add(h);
            }
            // Crying obsidian trim — 4 pieces
            for (int i = 0; i < 4; i++) {
                double a = i * (Math.PI / 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(a) * 1.7, 3.5, Math.sin(a) * 1.7), Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.4f, 0.6f).glow(140, 0, 30).interpolation(15, 0);
                trim.add(h);
            }
            // Beam sweeping panel (1 long panel forward)
            BlockDisplayHandle bp = displayBuilder.spawnBlock(
                    center.clone().add(2, 4.5, 0), Material.RED_STAINED_GLASS);
            bp.scale(8.0f, 0.4f, 0.4f).glow(255, 0, 0).interpolation(8, 0);
            beamPanel.add(bp);
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Continuously rotate light room shell + beam
            if (t % 3 == 0) {
                float ang = (float) (t * 0.08);
                for (int i = 0; i < lightRoom.size(); i++) {
                    int slot = i % 8;
                    int yLayer = i / 8;
                    double a = slot * (Math.PI * 2 / 8) + ang;
                    lightRoom.get(i).animateTo(
                            new Vector3f((float) (Math.cos(a) * 1.5) - 0.5f, 4f + yLayer - 0.5f, (float) (Math.sin(a) * 1.5) - 0.5f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(0.5f, 1.0f, 0.5f), 3);
                }
                // Beam panel rotates around — sweeping
                for (BlockDisplayHandle bp : beamPanel) {
                    double a = ang;
                    bp.animateTo(
                            new Vector3f((float) (Math.cos(a) * 4) - 0.5f, 4.5f - 0.5f, (float) (Math.sin(a) * 4) - 0.5f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(8.0f, 0.4f, 0.4f), 3);
                }
            }

            // Sweeping beam impact: every 30 ticks, fire impact at where beam currently points
            if (t > 40 && t % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.7f);
                float ang = (float) (t * 0.08);
                Location impactLoc = getCenter().clone().add(Math.cos(ang) * 6, 0, Math.sin(ang) * 6);
                triggerImpactDamage(impactLoc);
                w.spawnParticle(Particle.FLAME, impactLoc, 30, 1, 0.5, 1, 0.05);
            }

            // Drips from tower
            if (t % 6 == 0) {
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, getCenter().clone().add(0, 10, 0), 4, 0.5, 4, 0.5, 0);
            }
            // Beam particle line
            if (t % 2 == 0) {
                float ang = (float) (t * 0.08);
                Location s = getCenter().clone().add(0, 4.5, 0);
                Location e = getCenter().clone().add(Math.cos(ang) * 8, 4.5, Math.sin(ang) * 8);
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.5f);
                DisplayBuilder.particleLine(s, e, Particle.DUST, 4, dust);
            }
        }

        @Override
        public AbstractAttack newInstance() { return new InvertedLighthouse(plugin); }
    }

    // ================================================================
    // 39. DAMNATION BELL DROP (impact-only — toll then plummet)
    //     A massive fallen-cathedral bell descends slowly from the sky,
    //     suspended by chains. It rings (toll) twice, swinging side-to-
    //     side with each toll causing a shockwave. On the third toll
    //     the chains snap and the bell plummets, slamming the ground
    //     for a heavy impact.
    // ================================================================
    public static class DamnationBellDrop extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bellShell = new ArrayList<>();
        private final List<BlockDisplayHandle> bellCrown = new ArrayList<>();
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private final List<BlockDisplayHandle> clapper = new ArrayList<>();
        private int phase = 0;
        private static final float HOVER_Y = 14f;

        public DamnationBellDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("damnation_bell_drop", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(330);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(31.5);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.35f);
            Location top = center.clone().add(0, HOVER_Y, 0);

            // Bell shell — bowl shape from rings of crying_obsidian
            // 4 rings descending in y, narrowing toward bottom
            int[] ringYOffsets = {3, 2, 1, 0};
            double[] ringRadii = {3.0, 3.4, 3.0, 2.2};
            for (int r = 0; r < ringYOffsets.length; r++) {
                int segments = 12;
                for (int i = 0; i < segments; i++) {
                    double a = i * (Math.PI * 2 / segments);
                    double bx = Math.cos(a) * ringRadii[r];
                    double bz = Math.sin(a) * ringRadii[r];
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            top.clone().add(bx, ringYOffsets[r], bz), Material.CRYING_OBSIDIAN);
                    h.scale(1.0f, 1.0f, 0.6f).glow(140, 0, 30).interpolation(20, 0)
                            .rotate((float) a, 0, 1, 0);
                    bellShell.add(h);
                }
            }

            // Bell crown — top cap (4 deepslate_tiles arranged in cross)
            double[][] crownOff = {{0.7, 4.2, 0}, {-0.7, 4.2, 0}, {0, 4.2, 0.7}, {0, 4.2, -0.7}};
            for (double[] o : crownOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        top.clone().add(o[0], o[1], o[2]), Material.DEEPSLATE_TILES);
                h.scale(1.0f, 0.8f, 1.0f).glow(60, 0, 0).interpolation(20, 0);
                bellCrown.add(h);
            }

            // Suspension chains — 4 chain pillars going up from crown
            double[][] chainOff = {{0.7, 0}, {-0.7, 0}, {0, 0.7}, {0, -0.7}};
            for (double[] o : chainOff) {
                for (int seg = 0; seg < 6; seg++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            top.clone().add(o[0], 5.5 + seg * 1.2, o[1]), Material.CHAIN);
                    h.scale(0.4f, 1.2f, 0.4f).glow(80, 80, 80).interpolation(20, 0);
                    chains.add(h);
                }
            }

            // Clapper — soul lantern hanging at center inside the bell
            BlockDisplayHandle c = displayBuilder.spawnBlock(
                    top.clone().add(0, 0.8, 0), Material.SOUL_LANTERN);
            c.scale(1.0f, 1.6f, 1.0f).glow(255, 80, 30).interpolation(15, 0);
            clapper.add(c);
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Phase 0: descent into position over 40 ticks (start above HOVER_Y, settle to HOVER_Y)
            if (phase == 0 && t == 1) {
                // already placed at HOVER_Y; let it slowly bob in place
                phase = 1;
            }

            // Phase 1: first toll at t=40 — swing left, ring shockwave
            if (phase == 1 && t == 40) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BELL_RESONATE, 1.6f, 0.4f);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BELL_USE, 1.4f, 0.6f);
                swingBell(-0.35f, 22);
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 80, 30), 1.8f);
                DisplayBuilder.particleRing(getCenter().clone().add(0, HOVER_Y, 0), 5.5, Particle.DUST, 40, dust);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, getCenter().clone().add(0, HOVER_Y, 0), 30, 4.0, 1.0, 4.0, 0.05);
                phase = 2;
            }

            // Phase 2: swing right at t=80
            if (phase == 2 && t == 80) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BELL_RESONATE, 1.6f, 0.5f);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BELL_USE, 1.4f, 0.7f);
                swingBell(0.35f, 22);
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(140, 0, 30), 1.8f);
                DisplayBuilder.particleRing(getCenter().clone().add(0, HOVER_Y, 0), 5.5, Particle.DUST, 40, dust);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, getCenter().clone().add(0, HOVER_Y, 0), 30, 4.0, 1.0, 4.0, 0.05);
                phase = 3;
            }

            // Phase 3: settle straight at t=120, then chains snap at t=140
            if (phase == 3 && t == 120) {
                swingBell(0f, 18);
            }
            if (phase == 3 && t == 140) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_IRON_GOLEM_DEATH, 1.6f, 0.4f);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CHAIN_BREAK, 1.8f, 0.5f);
                // Chains break and fall
                for (BlockDisplayHandle ch : chains) {
                    Vector3f tr = ch.entity().getTransformation().getTranslation();
                    ch.animateTo(new Vector3f(tr.x + (float) (Math.random() - 0.5) * 3f,
                                    tr.y - 4f,
                                    tr.z + (float) (Math.random() - 0.5) * 3f),
                            new AxisAngle4f((float) (Math.random() * Math.PI), 1, 0, 1),
                            new Vector3f(0.4f, 1.2f, 0.4f), 30);
                }
                phase = 4;
            }

            // Phase 4: bell plummets at t=160
            if (phase == 4 && t == 160) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.4f);
                List<BlockDisplayHandle> bellAll = new ArrayList<>();
                bellAll.addAll(bellShell); bellAll.addAll(bellCrown); bellAll.addAll(clapper);
                for (BlockDisplayHandle h : bellAll) {
                    Vector3f tr = h.entity().getTransformation().getTranslation();
                    Vector3f sc = h.entity().getTransformation().getScale();
                    h.animateTo(new Vector3f(tr.x, tr.y - HOVER_Y, tr.z),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(sc.x, sc.y, sc.z), 14);
                }
                phase = 5;
            }

            // Plummet trail
            if (phase == 5 && t > 160 && t < 174 && t % 2 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                        getCenter().clone().add(0, HOVER_Y * (1.0 - (t - 160) / 14.0), 0),
                        16, 4.5, 0.4, 4.5, 0.02);
            }

            // Phase 5: ground impact at t=174
            if (phase == 5 && t == 174) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BELL_RESONATE, 1.8f, 0.3f);
                triggerImpactDamage(getCenter());
                w.spawnParticle(Particle.EXPLOSION_EMITTER, getCenter(), 5, 2.5, 0.3, 2.5, 0);
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 60, 30), 2.4f);
                DisplayBuilder.particleRing(getCenter(), 7.5, Particle.DUST, 70, dust);

                // Bell flattens (squish)
                for (BlockDisplayHandle h : bellShell) {
                    Vector3f tr = h.entity().getTransformation().getTranslation();
                    Vector3f sc = h.entity().getTransformation().getScale();
                    h.animateTo(new Vector3f(tr.x * 1.15f, tr.y, tr.z * 1.15f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(sc.x, sc.y * 0.6f, sc.z * 1.1f), 12);
                }
                phase = 6;
            }

            // Phase 6: linger then fade
            if (phase == 6 && t == 250) {
                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(bellShell); all.addAll(bellCrown); all.addAll(chains); all.addAll(clapper);
                for (BlockDisplayHandle h : all) {
                    Vector3f tr = h.entity().getTransformation().getTranslation();
                    h.animateTo(new Vector3f(tr.x, tr.y, tr.z),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.0f, 0.0f, 0.0f), 40);
                }
                phase = 7;
            }

            // Soul fire ambient while bell hangs (phases 1-3)
            if (phase >= 1 && phase <= 3 && t % 6 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                        getCenter().clone().add(0, HOVER_Y, 0), 6, 2.5, 1.0, 2.5, 0.01);
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR,
                        getCenter().clone().add(0, HOVER_Y - 1, 0), 4, 2.0, 0.5, 2.0, 0);
            }
        }

        // Apply a yaw/tilt swing rotation to the bell shell + crown + clapper
        private void swingBell(float tiltAngle, int dur) {
            List<BlockDisplayHandle> bellAll = new ArrayList<>();
            bellAll.addAll(bellShell); bellAll.addAll(bellCrown); bellAll.addAll(clapper);
            for (BlockDisplayHandle h : bellAll) {
                Vector3f tr = h.entity().getTransformation().getTranslation();
                Vector3f sc = h.entity().getTransformation().getScale();
                h.animateTo(new Vector3f(tr.x + tiltAngle * 1.5f, tr.y, tr.z),
                        new AxisAngle4f(tiltAngle, 0, 0, 1),
                        new Vector3f(sc.x, sc.y, sc.z), dur);
            }
        }

        @Override
        public AbstractAttack newInstance() { return new DamnationBellDrop(plugin); }
    }

    // ================================================================
    // 40. THE FORGOTTEN SHRINE (impact-only on detonation)
    // ================================================================
    public static class ForgottenShrine extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> steps = new ArrayList<>();
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> faces = new ArrayList<>();
        private final List<BlockDisplayHandle> innerGlow = new ArrayList<>();
        private final List<BlockDisplayHandle> overgrowth = new ArrayList<>();
        private int phase = 0;

        public ForgottenShrine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("forgotten_shrine", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(420);
            config.setCooldownTicks(420);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(33.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.5f);
            // 2-step nether brick base
            for (int s = 0; s < 2; s++) {
                int span = 3 - s;
                for (int x = -span; x <= span; x++) for (int z = -span; z <= span; z++) {
                    if (Math.abs(x) == span || Math.abs(z) == span) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(x, s * 0.5, z), Material.NETHER_BRICKS);
                        h.scale(1.0f, 0.5f, 1.0f).interpolation(15, 0);
                        steps.add(h);
                    }
                }
            }
            // Shrine body 3x3x4 hollow shell
            for (int x = -1; x <= 1; x++) for (int y = 1; y <= 4; y++) for (int z = -1; z <= 1; z++) {
                if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, z), Material.BLACKSTONE);
                    h.scale(1.0f, 1.0f, 1.0f).interpolation(15, 0);
                    body.add(h);
                }
            }
            // Carved faces — 4 on each side at center heights
            int[][] facePos = {{0, 2, 1}, {0, 2, -1}, {1, 2, 0}, {-1, 2, 0},
                    {0, 3, 1}, {0, 3, -1}, {1, 3, 0}, {-1, 3, 0}};
            for (int[] p : facePos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(p[0] * 1.05, p[1], p[2] * 1.05), Material.CHISELED_POLISHED_BLACKSTONE);
                h.scale(0.95f, 1.0f, 0.95f).glow(140, 0, 30).interpolation(15, 0);
                faces.add(h);
            }
            // Shroomlight inner glow — 4 pieces
            for (int y = 1; y <= 4; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.SHROOMLIGHT);
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 200, 80).interpolation(15, 0);
                innerGlow.add(h);
            }
            // Crying obsidian overgrowth — 6 small pieces creeping
            double[][] ogOff = {{1, 1.5, 0}, {-1, 1.5, 0}, {0, 1, 1}, {1.1, 3, 0.5}, {-1.1, 2.5, -0.5}, {0, 3.5, -1}};
            for (double[] o : ogOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(o[0], o[1], o[2]), Material.CRYING_OBSIDIAN);
                h.scale(0.0f, 0.0f, 0.0f).glow(140, 0, 30).interpolation(15, 0);
                overgrowth.add(h);
            }
        }

        @Override
        protected void onTick(int t) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Phase 0: resonate — face panels glow pulse + overgrowth extends
            if (phase == 0 && t == 1) {
                for (BlockDisplayHandle og : overgrowth) {
                    Vector3f tr = og.entity().getTransformation().getTranslation();
                    og.animateTo(new Vector3f(tr.x, tr.y, tr.z),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 80);
                }
                phase = 1;
            }
            if (phase == 1 && t % 6 == 0) {
                float pulse = 0.6f + (float) Math.sin(t * 0.15) * 0.2f;
                for (BlockDisplayHandle ig : innerGlow) {
                    Vector3f tr = ig.entity().getTransformation().getTranslation();
                    ig.animateTo(new Vector3f(tr.x, tr.y, tr.z),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, pulse, pulse), 6);
                }
            }

            // Phase 2: shake before detonate (t=240-280)
            if (phase == 1 && t > 240 && t % 3 == 0) {
                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(body); all.addAll(faces);
                for (BlockDisplayHandle h : all) {
                    float jx = (float) (Math.random() - 0.5) * 0.15f;
                    float jz = (float) (Math.random() - 0.5) * 0.15f;
                    Vector3f tr = h.entity().getTransformation().getTranslation();
                    h.animateTo(new Vector3f(tr.x + jx, tr.y, tr.z + jz),
                            new AxisAngle4f(0, 0, 1, 0),
                            h.entity().getTransformation().getScale(), 3);
                }
            }

            // Phase 3: detonate at t=300
            if (phase == 1 && t == 300) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.5f);
                triggerImpactDamage(getCenter());
                w.spawnParticle(Particle.EXPLOSION_EMITTER, getCenter().clone().add(0, 2.5, 0), 3, 1.5, 1.5, 1.5, 0);

                List<BlockDisplayHandle> all = new ArrayList<>();
                all.addAll(steps); all.addAll(body); all.addAll(faces);
                all.addAll(innerGlow); all.addAll(overgrowth);
                for (BlockDisplayHandle h : all) {
                    Vector3f tr = h.entity().getTransformation().getTranslation();
                    // Outward unit vector from center
                    float dx = tr.x + 0.5f;
                    float dy = tr.y + 0.5f - 2.5f;
                    float dz = tr.z + 0.5f;
                    float mag = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (mag < 0.001f) mag = 1f;
                    float outX = dx / mag * 8f;
                    float outY = dy / mag * 6f + 2f;
                    float outZ = dz / mag * 8f;
                    h.animateTo(new Vector3f(tr.x + outX, tr.y + outY, tr.z + outZ),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 2), 1, 1, 0),
                            new Vector3f(0.4f, 0.4f, 0.4f), 30);
                }
                phase = 2;
            }

            // Soul fire glow + obsidian drips
            if (t % 4 == 0 && phase == 1) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, getCenter().clone().add(0, 2.5, 0), 6, 0.6, 1, 0.6, 0.01);
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, getCenter().clone().add(0, 3, 0), 4, 1.5, 1, 1.5, 0);
            }
            // Crying obsidian ambient
            if (t % 25 == 0 && phase == 1) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 0.5f);
            }
        }

        @Override
        public AbstractAttack newInstance() { return new ForgottenShrine(plugin); }
    }
}
