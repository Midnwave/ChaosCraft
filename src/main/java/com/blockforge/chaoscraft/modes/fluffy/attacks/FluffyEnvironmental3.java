package com.blockforge.chaoscraft.modes.fluffy.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.AbstractAttack;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackConfig;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.calamity.attacks.EnvironmentalAttack;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluffy Mode — ENVIRONMENTAL FX BATCH 3 (entries 21-30).
 * ItemDisplay-only attacks (no BlockDisplays).
 * Cute-to-deadly twist visuals: rainbows, cat eyes, plush meteors,
 * paw prints, scent trails, baby monster reveals, cuddle puddles,
 * toy knife throws, plush fireworks, angry bee clouds.
 */
public final class FluffyEnvironmental3 {
    private FluffyEnvironmental3() {}

    private static final String MODE_PATH = "modes/fluffy/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new RainbowArc(plugin));
        registry.register(new CatEyeGlare(plugin));
        registry.register(new PlushMeteorShower(plugin));
        registry.register(new BigPawPrints(plugin));
        registry.register(new ScentTrail(plugin));
        registry.register(new BabyMonsterReveal(plugin));
        registry.register(new CuddlePuddle(plugin));
        registry.register(new ToyKnifeThrow(plugin));
        registry.register(new PlushFirework(plugin));
        registry.register(new AngryBeeCloud(plugin));
    }

    // Rainbow color palette (R, G, B)
    private static final int[][] RAINBOW = {
            {255, 0, 0},      // red
            {255, 140, 0},    // orange
            {255, 235, 0},    // yellow
            {0, 200, 60},     // green
            {0, 130, 255},    // blue
            {110, 0, 220},    // indigo
            {200, 0, 220}     // violet
    };

    // ================================================================
    // 21. RAINBOW ARC — 7 colored DUST streams arc across the sky.
    //     7 ItemDisplays (CONCRETE) mark the apexes. The arcs then
    //     "fall" and impact below — radius 2.0 each, 14 hearts.
    // ================================================================
    public static class RainbowArc extends EnvironmentalAttack {
        private static final Material[] APEX_MATS = {
                Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE,
                Material.GREEN_CONCRETE, Material.BLUE_CONCRETE, Material.PURPLE_CONCRETE,
                Material.MAGENTA_CONCRETE
        };
        private final List<ItemDisplayHandle> apexes = new ArrayList<>();
        private final double[] arcAngle = new double[7];
        private final double[] arcRadius = new double[7];
        private double fallProgress = 0.0;
        private boolean falling = false;
        private boolean impacted = false;

        public RainbowArc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rainbow_arc", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0); // 14 hearts
            config.setImpactRadius(2.0);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(360);
        }

        @Override protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1.2f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);

            for (int i = 0; i < 7; i++) {
                arcAngle[i] = Math.PI * 2 * i / 7.0 + Math.random() * 0.4;
                arcRadius[i] = 6.0 + Math.random() * 2.5;
                Location apex = center.clone().add(
                        Math.cos(arcAngle[i]) * arcRadius[i],
                        9.0,
                        Math.sin(arcAngle[i]) * arcRadius[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(apex, new ItemStack(APEX_MATS[i]));
                int[] c = RAINBOW[i];
                h.scale(0.9f, 0.9f, 0.9f).glow(c[0], c[1], c[2]).interpolation(20, 0);
                apexes.add(h);
                spawnedEntities.add(h.entity());
            }

            // 7 GLOW_BERRIES rainbow accents at mid-height + 7 AMETHYST_SHARD prisms at the top
            for (int i = 0; i < 7; i++) {
                double a = arcAngle[i] + Math.PI / 7;
                Location p = center.clone().add(Math.cos(a) * arcRadius[i] * 0.6, 5.0, Math.sin(a) * arcRadius[i] * 0.6);
                int[] col = RAINBOW[i];
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                h.scale(0.5f, 0.5f, 0.5f).glow(col[0], col[1], col[2]).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 7; i++) {
                double a = Math.PI * 2 * i / 7;
                Location p = center.clone().add(Math.cos(a) * 2, 11.0, Math.sin(a) * 2);
                int[] col = RAINBOW[i];
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                h.scale(0.55f, 0.8f, 0.55f).glow(col[0], col[1], col[2]).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Build phase: arcs trace upward to apex (ticks 1-50)
            if (tick < 50) {
                double t = tick / 50.0;
                for (int i = 0; i < 7; i++) {
                    int[] col = RAINBOW[i];
                    // Trace arc from ground edge to apex
                    Location start = center.clone().add(
                            Math.cos(arcAngle[i] + Math.PI) * arcRadius[i],
                            0.5,
                            Math.sin(arcAngle[i] + Math.PI) * arcRadius[i]);
                    Location end = center.clone().add(
                            Math.cos(arcAngle[i]) * arcRadius[i],
                            9.0,
                            Math.sin(arcAngle[i]) * arcRadius[i]);
                    int steps = 8;
                    for (int s = 0; s < steps; s++) {
                        double localT = Math.min(1.0, t + s * 0.02);
                        double x = start.getX() + (end.getX() - start.getX()) * localT;
                        double z = start.getZ() + (end.getZ() - start.getZ()) * localT;
                        // parabolic Y for the arc visual
                        double y = 0.5 + (4.0 * localT * (1.0 - localT)) * 7.0 + localT * 8.5;
                        DisplayBuilder.dustParticles(new Location(w, x, y, z), 2, 0.1, col[0], col[1], col[2], 1.4f);
                    }
                }
                // Apex sparkle
                if (tick % 3 == 0) {
                    for (ItemDisplayHandle h : apexes) {
                        w.spawnParticle(Particle.END_ROD, h.entity().getLocation(), 1, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            } else if (tick == 60) {
                // Begin falling
                falling = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 1.4f);
            }

            // Falling phase
            if (falling && !impacted) {
                fallProgress += 0.18;
                for (int i = 0; i < 7; i++) {
                    ItemDisplayHandle h = apexes.get(i);
                    int[] col = RAINBOW[i];
                    double x = Math.cos(arcAngle[i]) * arcRadius[i];
                    double z = Math.sin(arcAngle[i]) * arcRadius[i];
                    float y = (float)(9.0 - fallProgress * 9.0);
                    h.animateTo(new Vector3f((float)x - 0.45f, y, (float)z - 0.45f),
                            new AxisAngle4f((float)(tick * 0.3), 1, 1, 0), new Vector3f(0.9f), 2);
                    Location streamLoc = center.clone().add(x, y, z);
                    DisplayBuilder.dustParticles(streamLoc, 4, 0.3, col[0], col[1], col[2], 1.6f);
                }

                if (fallProgress >= 1.0) {
                    impacted = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 1.2f);
                    for (int i = 0; i < 7; i++) {
                        Location impact = center.clone().add(
                                Math.cos(arcAngle[i]) * arcRadius[i], 0.4,
                                Math.sin(arcAngle[i]) * arcRadius[i]);
                        int[] col = RAINBOW[i];
                        w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                        DisplayBuilder.dustParticles(impact, 30, 1.6, col[0], col[1], col[2], 1.6f);
                        triggerImpactDamage(impact);
                    }
                }
            }
        }

        @Override protected void onCleanup() {
            super.onCleanup();
            apexes.clear();
        }

        @Override public AbstractAttack newInstance() { return new RainbowArc(plugin); }
    }

    // ================================================================
    // 22. CAT EYE GLARE — 2 GREEN DUST orbs (cat eyes) form at Y+6,
    //     stare 3s, then fire CRIT beam straight down from each eye.
    //     Beam impact radius 1.5 each, 18 hearts.
    // ================================================================
    public static class CatEyeGlare extends EnvironmentalAttack {
        private ItemDisplayHandle leftEye;
        private ItemDisplayHandle rightEye;
        private boolean fired = false;
        private int firedTick = -1;

        public CatEyeGlare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cat_eye_glare", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0); // 18 hearts
            config.setImpactRadius(1.5);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(360);
        }

        @Override protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            DisplayBuilder.playSound(center, Sound.ENTITY_CAT_HISS, 1.2f, 0.5f);
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.9f, 0.6f);

            // Eyes use ENDER_PEARL items as glowing orbs
            leftEye = displayBuilder.spawnItem(center.clone().add(-1.0, 6.0, 0), new ItemStack(Material.ENDER_PEARL));
            leftEye.scale(0.6f, 0.6f, 0.6f).glow(40, 220, 40).interpolation(10, 0);
            spawnedEntities.add(leftEye.entity());

            rightEye = displayBuilder.spawnItem(center.clone().add(1.0, 6.0, 0), new ItemStack(Material.ENDER_PEARL));
            rightEye.scale(0.6f, 0.6f, 0.6f).glow(40, 220, 40).interpolation(10, 0);
            spawnedEntities.add(rightEye.entity());

            // Cat shape: 4 STRING whiskers + 6 RABBIT_HIDE fur tufts + 4 BONE fangs + 2 NETHER_WART nose
            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 2; j++) {
                    Location p = center.clone().add(side * (1.5 + j * 0.5), 5.5 - j * 0.4, 0);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                    h.scale(0.6f, 0.1f, 0.1f).glow(240, 240, 240).interpolation(40, 0);
                    spawnedEntities.add(h.entity());
                }
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = center.clone().add(Math.cos(a) * 2, 6.5 + Math.sin(a) * 0.5, Math.sin(a) * 2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 220, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                Location p = center.clone().add(-0.4 + i * 0.25, 5.0, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BONE));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 255, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 2; i++) {
                Location p = center.clone().add(-0.2 + i * 0.4, 5.5, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHER_WART));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 100, 130).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Pulsing eye DUST orbs at Y+6
            if (tick % 2 == 0) {
                Location lp = center.clone().add(-1.0, 6.0, 0);
                Location rp = center.clone().add(1.0, 6.0, 0);
                DisplayBuilder.dustParticles(lp, 6, 0.4, 40, 220, 40, 1.6f);
                DisplayBuilder.dustParticles(rp, 6, 0.4, 40, 220, 40, 1.6f);
            }

            // Pupil slit pulse
            if (tick % 6 == 0) {
                float s = 0.55f + (float)Math.abs(Math.sin(tick * 0.08)) * 0.2f;
                if (leftEye != null) leftEye.animateTo(new Vector3f(-1.0f - s/2f, 6.0f, -s/2f), new AxisAngle4f(0, 0, 1, 0), new Vector3f(s), 6);
                if (rightEye != null) rightEye.animateTo(new Vector3f(1.0f - s/2f, 6.0f, -s/2f), new AxisAngle4f(0, 0, 1, 0), new Vector3f(s), 6);
            }

            // Stare-charge sound
            if (tick == 30) DisplayBuilder.playSound(center, Sound.ENTITY_CAT_STRAY_AMBIENT, 1.0f, 0.5f);

            // Fire beams at tick 60 (3 seconds)
            if (tick == 60 && !fired) {
                fired = true;
                firedTick = tick;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.4f, 1.4f);
            }

            // Beam render & impact
            if (fired) {
                int beamTick = tick - firedTick;
                if (beamTick <= 6) {
                    // Beam straight down from each eye
                    for (int side = -1; side <= 1; side += 2) {
                        Location eyeLoc = center.clone().add(side * 1.0, 6.0, 0);
                        Location ground = center.clone().add(side * 1.0, 0.5, 0);
                        DisplayBuilder.particleLine(eyeLoc, ground, Particle.END_ROD, 6, null);
                        DisplayBuilder.particleLine(eyeLoc, ground, Particle.CRIT, 4, null);
                    }
                }
                if (beamTick == 6) {
                    // impact at each eye's ground point
                    Location leftImpact = center.clone().add(-1.0, 0.5, 0);
                    Location rightImpact = center.clone().add(1.0, 0.5, 0);
                    w.spawnParticle(Particle.EXPLOSION, leftImpact, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.EXPLOSION, rightImpact, 1, 0, 0, 0, 0);
                    DisplayBuilder.dustParticles(leftImpact, 20, 1.2, 40, 220, 40, 1.6f);
                    DisplayBuilder.dustParticles(rightImpact, 20, 1.2, 40, 220, 40, 1.6f);
                    triggerImpactDamage(leftImpact);
                    triggerImpactDamage(rightImpact);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CatEyeGlare(plugin); }
    }

    // ================================================================
    // 23. PLUSH METEOR SHOWER — 10 colored CONCRETE ItemDisplays
    //     fall from Y+25 with DUST tails. Each impacts radius 2.0,
    //     16 hearts.
    // ================================================================
    public static class PlushMeteorShower extends EnvironmentalAttack {
        private static final Material[] METEORS = {
                Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE,
                Material.LIME_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.PINK_CONCRETE,
                Material.MAGENTA_CONCRETE, Material.PURPLE_CONCRETE, Material.CYAN_CONCRETE,
                Material.WHITE_CONCRETE
        };
        private final List<ItemDisplayHandle> meteors = new ArrayList<>();
        private final double[] mAng = new double[10];
        private final double[] mR = new double[10];
        private final double[] mY = new double[10];
        private final boolean[] mImpacted = new boolean[10];

        public PlushMeteorShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plush_meteor_shower", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0); // 16 hearts
            config.setImpactRadius(2.0);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(380);
        }

        @Override protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.2f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);

            for (int i = 0; i < 10; i++) {
                mAng[i] = Math.random() * Math.PI * 2;
                mR[i] = 1.5 + Math.random() * 7.0;
                mY[i] = 25.0 + Math.random() * 4.0 + i * 1.2;
                Location pos = center.clone().add(Math.cos(mAng[i]) * mR[i], mY[i], Math.sin(mAng[i]) * mR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(pos, new ItemStack(METEORS[i]));
                int[] col = RAINBOW[i % RAINBOW.length];
                h.scale(0.9f, 0.9f, 0.9f).glow(col[0], col[1], col[2]).interpolation(2, 0);
                meteors.add(h);
                spawnedEntities.add(h.entity());
            }

            // Plush comet trails: 8 WHITE_WOOL + 6 STRING + 4 RABBIT_HIDE high above
            Material[] trail = {Material.WHITE_WOOL, Material.WHITE_WOOL, Material.WHITE_WOOL, Material.WHITE_WOOL,
                    Material.WHITE_WOOL, Material.WHITE_WOOL, Material.WHITE_WOOL, Material.WHITE_WOOL,
                    Material.STRING, Material.STRING, Material.STRING, Material.STRING, Material.STRING, Material.STRING,
                    Material.RABBIT_HIDE, Material.RABBIT_HIDE, Material.RABBIT_HIDE, Material.RABBIT_HIDE};
            for (int i = 0; i < trail.length; i++) {
                double a = Math.random() * Math.PI * 2;
                double rr = Math.random() * 7;
                double yy = 18 + Math.random() * 8;
                Location p = center.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                int[] col = RAINBOW[i % RAINBOW.length];
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(trail[i]));
                h.scale(0.5f, 0.5f, 0.5f).glow(col[0], col[1], col[2]).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (int i = 0; i < meteors.size(); i++) {
                if (mImpacted[i]) continue;
                mY[i] -= 0.45 + (i % 3) * 0.05;
                Location pos = center.clone().add(Math.cos(mAng[i]) * mR[i], mY[i], Math.sin(mAng[i]) * mR[i]);
                meteors.get(i).animateTo(
                        new Vector3f((float)(Math.cos(mAng[i]) * mR[i]) - 0.45f, (float)mY[i], (float)(Math.sin(mAng[i]) * mR[i]) - 0.45f),
                        new AxisAngle4f((float)(tick * 0.3 + i), 1, 1, 0), new Vector3f(0.9f), 2);

                // Color trail
                int[] col = RAINBOW[i % RAINBOW.length];
                DisplayBuilder.dustParticles(pos, 3, 0.2, col[0], col[1], col[2], 1.6f);
                w.spawnParticle(Particle.CLOUD, pos, 1, 0.1, 0.1, 0.1, 0.01);

                if (mY[i] <= 0.6) {
                    mImpacted[i] = true;
                    Location impact = center.clone().add(Math.cos(mAng[i]) * mR[i], 0.4, Math.sin(mAng[i]) * mR[i]);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.4f);
                    w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                    DisplayBuilder.dustParticles(impact, 25, 1.4, col[0], col[1], col[2], 1.6f);
                    triggerImpactDamage(impact);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); meteors.clear(); }
        @Override public AbstractAttack newInstance() { return new PlushMeteorShower(plugin); }
    }

    // ================================================================
    // 24. BIG PAW PRINTS — 8 sequential pink DUST circles (3-block
    //     diameter) appear as footsteps moving toward the player.
    //     Each is a temporary 2.0 radius zone, 9 hearts constant.
    // ================================================================
    public static class BigPawPrints extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> paws = new ArrayList<>();
        private final List<Location> pawCenters = new ArrayList<>();
        private final double[] pawAng = new double[8];
        private final double[] pawDist = new double[8];

        public BigPawPrints(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("big_paw_prints", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(9.0); // 9 hearts
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(220);
            config.setCooldownTicks(320);
        }

        @Override protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            DisplayBuilder.playSound(center, Sound.ENTITY_CAT_AMBIENT, 1.0f, 0.7f);

            // Player to track for "moving toward player"
            Player tp = getTargetPlayer();
            double targetAngle;
            if (tp != null) {
                double dx = tp.getLocation().getX() - center.getX();
                double dz = tp.getLocation().getZ() - center.getZ();
                targetAngle = Math.atan2(dz, dx);
            } else {
                targetAngle = Math.random() * Math.PI * 2;
            }

            for (int i = 0; i < 8; i++) {
                pawAng[i] = targetAngle + (Math.random() - 0.5) * 0.4;
                pawDist[i] = 8.0 - i * 1.0;
                double x = Math.cos(pawAng[i]) * pawDist[i];
                double z = Math.sin(pawAng[i]) * pawDist[i];
                Location pawLoc = center.clone().add(x, 0.4, z);
                pawCenters.add(pawLoc);

                // ItemDisplay marker — PINK_WOOL puff at the print
                ItemDisplayHandle h = displayBuilder.spawnItem(pawLoc, new ItemStack(Material.PINK_WOOL));
                h.scale(1.2f, 0.15f, 1.2f).glow(255, 150, 200).interpolation(6, i * 4);
                paws.add(h);
                spawnedEntities.add(h.entity());

                // 4 toe-bean PINK_CONCRETE per pawprint
                double[][] toes = {{-0.55, 0.85}, {0.55, 0.85}, {-0.85, 0.25}, {0.85, 0.25}};
                for (double[] off : toes) {
                    Location toeLoc = pawLoc.clone().add(off[0], 0, off[1]);
                    ItemDisplayHandle toe = displayBuilder.spawnItem(toeLoc, new ItemStack(Material.PINK_CONCRETE));
                    toe.scale(0.35f, 0.1f, 0.4f).glow(255, 130, 180).interpolation(6, i * 4);
                    spawnedEntities.add(toe.entity());
                }
            }

            // Cat clutter: 6 STRING + 4 RABBIT_FOOT scattered along the trail
            for (int i = 0; i < 6; i++) {
                double a = targetAngle + (Math.random() - 0.5) * 0.6;
                double r = 1 + i * 1.2;
                Location p = center.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.65f, 0.65f, 0.65f).glow(255, 220, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = targetAngle + Math.PI / 2 + i * 0.4;
                double r = 2 + i * 1.5;
                Location p = center.clone().add(Math.cos(a) * r, 0.4, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_FOOT));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 200, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Sequentially "stamp" each paw print every 12 ticks
            int active = Math.min(8, tick / 12);
            for (int i = 0; i < active && i < paws.size(); i++) {
                Location p = pawCenters.get(i);
                // Paw circle DUST
                if (tick % 2 == 0) {
                    int points = 14;
                    for (int s = 0; s < points; s++) {
                        double a = Math.PI * 2 * s / points;
                        double rr = 1.5;
                        Location ringPt = p.clone().add(Math.cos(a) * rr, 0.05, Math.sin(a) * rr);
                        DisplayBuilder.dustParticles(ringPt, 1, 0.05, 255, 150, 200, 1.5f);
                    }
                    // 4 toe-bean dots
                    for (int t2 = 0; t2 < 4; t2++) {
                        double a = Math.PI * 2 * t2 / 4 + 0.3;
                        Location toe = p.clone().add(Math.cos(a) * 1.0, 0.05, Math.sin(a) * 1.0);
                        DisplayBuilder.dustParticles(toe, 2, 0.1, 255, 100, 180, 1.6f);
                    }
                }
            }

            // Cat step sounds when stamping
            if (tick % 12 == 0 && tick / 12 <= 8 && tick / 12 >= 1) {
                int idx = tick / 12 - 1;
                if (idx < pawCenters.size()) {
                    DisplayBuilder.playSound(pawCenters.get(idx), Sound.ENTITY_CAT_AMBIENT, 0.8f, 1.2f);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); paws.clear(); pawCenters.clear(); }
        @Override public AbstractAttack newInstance() { return new BigPawPrints(plugin); }
    }

    // ================================================================
    // 25. SCENT TRAIL — CHERRY_LEAVES + SPORE_BLOSSOM_AIR trail moves
    //     toward the player's last position, then detonates.
    //     Impact at end of trail, radius 4.0, 20 hearts.
    // ================================================================
    // Chase-by-design: scent_trail follows the player toward their last known position
    // (targetEnd is captured at spawn — the trail intentionally points at the player's
    // spawn-time location, then detonates there).
    public static class ScentTrail extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> trail = new ArrayList<>();
        private Location targetEnd;
        private boolean detonated = false;

        public ScentTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scent_trail", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0); // 20 hearts
            config.setImpactRadius(4.0);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(380);
        }

        @Override protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            DisplayBuilder.playSound(center, Sound.ENTITY_CAT_PURR, 1.2f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SPORE_BLOSSOM_BREAK, 1.0f, 0.6f);

            Player tp = getTargetPlayer();
            if (tp != null) {
                targetEnd = tp.getLocation().clone();
            } else {
                double a = Math.random() * Math.PI * 2;
                targetEnd = center.clone().add(Math.cos(a) * 7, 0, Math.sin(a) * 7);
            }

            // Trail of 9 cherry leaf bunches
            int n = 9;
            for (int i = 0; i < n; i++) {
                double t = i / (double)(n - 1);
                Location p = new Location(center.getWorld(),
                        center.getX() + (targetEnd.getX() - center.getX()) * t,
                        center.getY() + 0.6,
                        center.getZ() + (targetEnd.getZ() - center.getZ()) * t);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHERRY_LEAVES));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 180, 220).interpolation(4, i * 2);
                trail.add(h);
                spawnedEntities.add(h.entity());
            }

            // Side decoration along the scent trail: 9 PINK_PETALS + 6 SPORE_BLOSSOM + 4 AZALEA_LEAVES offset perpendicular
            double dx = (targetEnd.getX() - center.getX());
            double dz = (targetEnd.getZ() - center.getZ());
            double mag = Math.sqrt(dx * dx + dz * dz);
            if (mag > 0.01) {
                double perpX = -dz / mag;
                double perpZ = dx / mag;
                for (int i = 0; i < 9; i++) {
                    double t = i / 8.0;
                    double off = (i % 2 == 0 ? 0.6 : -0.6);
                    Location p = new Location(center.getWorld(),
                            center.getX() + dx * t + perpX * off,
                            center.getY() + 0.7,
                            center.getZ() + dz * t + perpZ * off);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PINK_PETALS));
                    h.scale(0.65f, 0.65f, 0.65f).glow(255, 180, 220).interpolation(40, 0);
                    spawnedEntities.add(h.entity());
                }
                for (int i = 0; i < 6; i++) {
                    double t = (i + 0.5) / 6.0;
                    double off = (i % 2 == 0 ? 1.2 : -1.2);
                    Location p = new Location(center.getWorld(),
                            center.getX() + dx * t + perpX * off,
                            center.getY() + 1.5,
                            center.getZ() + dz * t + perpZ * off);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SPORE_BLOSSOM));
                    h.scale(0.55f, 0.55f, 0.55f).glow(220, 130, 200).interpolation(40, 0);
                    spawnedEntities.add(h.entity());
                }
                for (int i = 0; i < 4; i++) {
                    double t = (i + 1) / 5.0;
                    Location p = new Location(center.getWorld(),
                            center.getX() + dx * t,
                            center.getY() + 2.5,
                            center.getZ() + dz * t);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AZALEA_LEAVES));
                    h.scale(0.5f, 0.5f, 0.5f).glow(255, 200, 230).interpolation(40, 0);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (targetEnd == null) return;

            // Particle trail along the path while building
            if (tick < 80) {
                int progress = Math.min(trail.size(), tick / 8 + 1);
                for (int i = 0; i < progress; i++) {
                    Location p = trail.get(i).entity().getLocation();
                    if (tick % 2 == 0) {
                        DisplayBuilder.dustParticles(p, 2, 0.3, 255, 180, 220, 1.5f);
                        w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, p, 2, 0.4, 0.3, 0.4, 0.01);
                        if (Math.random() < 0.3) {
                            w.spawnParticle(Particle.CHERRY_LEAVES, p, 2, 0.4, 0.3, 0.4, 0.01);
                        }
                    }
                }
            }

            // Detonate at tick 90
            if (tick == 90 && !detonated) {
                detonated = true;
                Location impact = targetEnd.clone();
                DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 1.1f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_AZALEA_LEAVES_BREAK, 1.4f, 0.8f);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                for (int i = 0; i < 60; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 4.0;
                    Location burst = impact.clone().add(Math.cos(a) * r, Math.random() * 2, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(burst, 1, 0.3, 255, 180, 220, 1.6f);
                    w.spawnParticle(Particle.CHERRY_LEAVES, burst, 2, 0.3, 0.3, 0.3, 0.05);
                    w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, burst, 1, 0.4, 0.4, 0.4, 0.02);
                }
                triggerImpactDamage(impact);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); trail.clear(); }
        @Override public AbstractAttack newInstance() { return new ScentTrail(plugin); }
    }

    // ================================================================
    // 26. BABY MONSTER REVEAL — DUST particles form a cat face
    //     silhouette in midair (4s build). Then ALL particles explode
    //     outward. Impact radius 5.0, 22 hearts.
    // ================================================================
    public static class BabyMonsterReveal extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> features = new ArrayList<>();
        private boolean exploded = false;
        private Location facePos;

        public BabyMonsterReveal(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("baby_monster_reveal", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.0); // 22 hearts
            config.setImpactRadius(5.0);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(420);
        }

        @Override protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            facePos = center.clone().add(0, 4.0, 0);
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.2f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_CAT_PURREOW, 1.0f, 0.5f);

            // Two glowing eyes (ENDER_PEARL items) and a tiny nose (NETHER_WART)
            ItemDisplayHandle leftEye = displayBuilder.spawnItem(facePos.clone().add(-0.7, 0.3, 0), new ItemStack(Material.ENDER_PEARL));
            leftEye.scale(0.45f, 0.45f, 0.45f).glow(255, 200, 50).interpolation(20, 0);
            features.add(leftEye); spawnedEntities.add(leftEye.entity());

            ItemDisplayHandle rightEye = displayBuilder.spawnItem(facePos.clone().add(0.7, 0.3, 0), new ItemStack(Material.ENDER_PEARL));
            rightEye.scale(0.45f, 0.45f, 0.45f).glow(255, 200, 50).interpolation(20, 0);
            features.add(rightEye); spawnedEntities.add(rightEye.entity());

            ItemDisplayHandle nose = displayBuilder.spawnItem(facePos.clone().add(0, -0.1, 0), new ItemStack(Material.NETHER_WART));
            nose.scale(0.3f, 0.3f, 0.3f).glow(255, 100, 130).interpolation(20, 0);
            features.add(nose); spawnedEntities.add(nose.entity());

            // Build the rest of the cat face with items: 6 STRING whiskers + 4 BONE fangs + 4 RABBIT_HIDE ears + 2 PINK_CONCRETE inner ears
            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 3; j++) {
                    Location p = facePos.clone().add(side * (0.6 + j * 0.4), -0.1 + (j - 1) * 0.15, 0);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                    h.scale(0.4f, 0.06f, 0.06f).glow(240, 240, 240).interpolation(40, 0);
                    spawnedEntities.add(h.entity());
                }
            }
            for (int i = 0; i < 4; i++) {
                Location p = facePos.clone().add(-0.4 + i * 0.25, -0.4, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BONE));
                h.scale(0.18f, 0.4f, 0.18f).glow(255, 255, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 2; j++) {
                    Location p = facePos.clone().add(side * (1.0 + j * 0.3), 1.4 + j * 0.3, 0);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                    h.scale(0.5f, 0.5f, 0.5f).glow(220, 200, 180).interpolation(40, 0);
                    spawnedEntities.add(h.entity());
                }
            }
            for (int side = -1; side <= 1; side += 2) {
                Location p = facePos.clone().add(side * 1.1, 1.5, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PINK_CONCRETE));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 130, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (facePos == null) return;

            // Build phase 0-80: form cat face DUST silhouette
            if (tick <= 80) {
                double progress = Math.min(1.0, tick / 80.0);

                // Outline circle (face)
                if (tick % 2 == 0) {
                    int points = (int)(28 * progress) + 4;
                    for (int i = 0; i < points; i++) {
                        double a = Math.PI * 2 * i / Math.max(points, 1);
                        Location p = facePos.clone().add(Math.cos(a) * 1.3, Math.sin(a) * 1.3, 0);
                        DisplayBuilder.dustParticles(p, 1, 0.05, 255, 220, 100, 1.5f);
                    }
                    // Triangle ears (top corners)
                    for (int side = -1; side <= 1; side += 2) {
                        for (int j = 0; j < 5; j++) {
                            double t = j / 4.0;
                            Location earBase = facePos.clone().add(side * 0.9, 1.0, 0);
                            Location earTip = facePos.clone().add(side * 1.5, 2.0, 0);
                            Location p = earBase.clone().add(
                                    (earTip.getX() - earBase.getX()) * t,
                                    (earTip.getY() - earBase.getY()) * t,
                                    0);
                            DisplayBuilder.dustParticles(p, 1, 0.05, 255, 200, 80, 1.4f);
                        }
                    }
                    // Whiskers
                    for (int side = -1; side <= 1; side += 2) {
                        for (int wk = -1; wk <= 1; wk++) {
                            for (int j = 0; j < 4; j++) {
                                double t = j / 3.0;
                                Location whiskBase = facePos.clone().add(side * 0.4, 0.0 + wk * 0.2, 0);
                                Location whiskTip = facePos.clone().add(side * 1.8, 0.0 + wk * 0.3, 0);
                                Location p = whiskBase.clone().add(
                                        (whiskTip.getX() - whiskBase.getX()) * t,
                                        (whiskTip.getY() - whiskBase.getY()) * t,
                                        0);
                                DisplayBuilder.dustParticles(p, 1, 0.03, 240, 240, 240, 1.2f);
                            }
                        }
                    }
                }
                // Eye glints
                if (tick % 4 == 0) {
                    w.spawnParticle(Particle.END_ROD, facePos.clone().add(-0.7, 0.3, 0), 1, 0.05, 0.05, 0.05, 0);
                    w.spawnParticle(Particle.END_ROD, facePos.clone().add(0.7, 0.3, 0), 1, 0.05, 0.05, 0.05, 0);
                }
            }

            // Charging hum
            if (tick == 60) DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_WARN, 1.4f, 1.4f);

            // Explode at tick 90
            if (tick == 90 && !exploded) {
                exploded = true;
                DisplayBuilder.playSound(facePos, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.8f);
                DisplayBuilder.playSound(facePos, Sound.ENTITY_CAT_HISS, 1.4f, 0.5f);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, facePos, 2, 0.3, 0.3, 0.3, 0);
                for (int i = 0; i < 80; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double phi = Math.random() * Math.PI;
                    double r = Math.random() * 5.0;
                    Location burst = facePos.clone().add(
                            Math.cos(a) * Math.sin(phi) * r,
                            Math.cos(phi) * r,
                            Math.sin(a) * Math.sin(phi) * r);
                    DisplayBuilder.dustParticles(burst, 1, 0.3, 255, 200, 80, 1.6f);
                    if (i % 4 == 0) w.spawnParticle(Particle.LARGE_SMOKE, burst, 1, 0.2, 0.2, 0.2, 0.02);
                }
                triggerImpactDamage(facePos);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); features.clear(); }
        @Override public AbstractAttack newInstance() { return new BabyMonsterReveal(plugin); }
    }

    // ================================================================
    // 27. CUDDLE PUDDLE — Soft pink DUST circle expanding/contracting
    //     at ground. 4 ROSE_BUSH ItemDisplays at perimeter.
    //     Constant radius 3.5, 9 hearts, every 12 ticks.
    // ================================================================
    public static class CuddlePuddle extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> roses = new ArrayList<>();

        public CuddlePuddle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cuddle_puddle", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(9.0); // 9 hearts
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(260);
            config.setCooldownTicks(320);
        }

        @Override protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.7f);
            DisplayBuilder.playSound(center, Sound.ENTITY_CAT_PURR, 1.4f, 0.8f);

            // 4 rose bush at perimeter
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = center.clone().add(Math.cos(a) * 3.0, 0.5, Math.sin(a) * 3.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ROSE_BUSH));
                h.scale(1.0f, 1.4f, 1.0f).glow(255, 100, 160).interpolation(20, i * 4);
                roses.add(h);
                spawnedEntities.add(h.entity());
            }

            // Comfy cuddle props: 8 PINK_PETALS + 6 STRING + 4 RABBIT_HIDE + 4 PINK_WOOL
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = center.clone().add(Math.cos(a) * 2.0, 0.3, Math.sin(a) * 2.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PINK_PETALS));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 180, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 6;
                Location p = center.clone().add(Math.cos(a) * 1.0, 0.4, Math.sin(a) * 1.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 220, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = center.clone().add(Math.cos(a) * 2.5, 0.4, Math.sin(a) * 2.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                h.scale(0.55f, 0.55f, 0.55f).glow(230, 200, 210).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = center.clone().add(Math.cos(a) * 1.5, 0.6, Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PINK_WOOL));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 180, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Pulsing pink puddle
            double pulse = 0.85 + Math.sin(tick * 0.08) * 0.15;
            double rPuddle = 3.5 * pulse;

            if (tick % 1 == 0) {
                int points = 36;
                for (int i = 0; i < points; i++) {
                    double a = Math.PI * 2 * i / points;
                    Location p = center.clone().add(Math.cos(a) * rPuddle, 0.1, Math.sin(a) * rPuddle);
                    DisplayBuilder.dustParticles(p, 1, 0.05, 255, 130, 200, 1.5f);
                }
            }
            // Inner pulse fill
            if (tick % 2 == 0) {
                for (int i = 0; i < 14; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * rPuddle;
                    Location p = center.clone().add(Math.cos(a) * r, 0.1 + Math.random() * 0.3, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 255, 180, 220, 1.3f);
                }
            }
            // Rose bush sway
            if (tick % 6 == 0) {
                for (int i = 0; i < roses.size(); i++) {
                    double a = Math.PI * 2 * i / 4;
                    float s = 1.0f + (float)Math.sin(tick * 0.1 + i) * 0.1f;
                    roses.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * 3.0) - 0.5f, 0.5f, (float)(Math.sin(a) * 3.0) - 0.5f),
                            new AxisAngle4f((float)(Math.sin(tick * 0.05 + i) * 0.2), 0, 0, 1),
                            new Vector3f(s, 1.4f, s), 6);
                }
            }
            // Heart particles drifting up
            if (tick % 6 == 0) {
                for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 3.0;
                    w.spawnParticle(Particle.HEART, center.clone().add(Math.cos(a) * r, 1.0 + Math.random() * 1.5, Math.sin(a) * r), 1, 0.1, 0.1, 0.1, 0);
                }
            }

            if (tick % 80 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_CAT_PURR, 1.0f, 0.8f);
        }

        @Override protected void onCleanup() { super.onCleanup(); roses.clear(); }
        @Override public AbstractAttack newInstance() { return new CuddlePuddle(plugin); }
    }

    // ================================================================
    // 28. TOY KNIFE THROW — IRON_SWORD ItemDisplay spinning in arc
    //     from arena edge toward random player position. Impact at
    //     landing, radius 2.0, 20 hearts.
    // ================================================================
    public static class ToyKnifeThrow extends EnvironmentalAttack {
        private ItemDisplayHandle knife;
        private Location startPos;
        private Location endPos;
        private boolean impacted = false;

        public ToyKnifeThrow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("toy_knife_throw", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0); // 20 hearts
            config.setImpactRadius(2.0);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(280);
        }

        @Override protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THROW, 1.4f, 1.4f);

            // Random arena edge spawn
            double edgeAngle = Math.random() * Math.PI * 2;
            startPos = center.clone().add(Math.cos(edgeAngle) * 12.0, 6.0, Math.sin(edgeAngle) * 12.0);

            Player tp = getTargetPlayer();
            if (tp != null) {
                endPos = tp.getLocation().clone();
            } else {
                endPos = center.clone();
            }

            knife = displayBuilder.spawnItem(startPos, new ItemStack(Material.IRON_SWORD));
            knife.scale(1.4f, 1.4f, 1.4f).glow(220, 220, 255).interpolation(2, 0);
            spawnedEntities.add(knife.entity());

            // Toy box on the ground around the player: 6 STRING + 4 BONE + 4 RABBIT_FOOT + 4 STICK
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = endPos.clone().add(Math.cos(a) * 3.5, 0.3, Math.sin(a) * 3.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 220, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = endPos.clone().add(Math.cos(a) * 2.5, 0.4, Math.sin(a) * 2.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BONE));
                h.scale(0.6f, 0.6f, 0.6f).glow(240, 230, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = endPos.clone().add(Math.cos(a) * 4.5, 0.4, Math.sin(a) * 4.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_FOOT));
                h.scale(0.55f, 0.55f, 0.55f).glow(220, 200, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 8;
                Location p = endPos.clone().add(Math.cos(a) * 5.5, 0.4, Math.sin(a) * 5.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STICK));
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 140, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (startPos == null || endPos == null || knife == null) return;

            double duration = 40.0;
            if (tick <= duration && !impacted) {
                double t = tick / duration;
                double x = startPos.getX() + (endPos.getX() - startPos.getX()) * t;
                double z = startPos.getZ() + (endPos.getZ() - startPos.getZ()) * t;
                // arc apex
                double y = startPos.getY() + (endPos.getY() - startPos.getY()) * t + (4.0 * t * (1.0 - t)) * 5.0;

                // animateTo relative to spawn-center anchor
                float rx = (float)(x - center.getX()) - 0.7f;
                float ry = (float)(y - center.getY());
                float rz = (float)(z - center.getZ()) - 0.7f;
                knife.animateTo(new Vector3f(rx, ry, rz),
                        new AxisAngle4f((float)(tick * 0.8), 1, 0, 1),
                        new Vector3f(1.4f), 2);

                // CRIT spinning trail
                Location trailLoc = new Location(w, x, y, z);
                w.spawnParticle(Particle.CRIT, trailLoc, 4, 0.15, 0.15, 0.15, 0.05);
                if (tick % 2 == 0) DisplayBuilder.dustParticles(trailLoc, 2, 0.2, 220, 220, 255, 1.4f);
            }

            if (tick == 40 && !impacted) {
                impacted = true;
                Location impact = endPos.clone();
                DisplayBuilder.playSound(impact, Sound.ITEM_TRIDENT_HIT_GROUND, 1.4f, 1.0f);
                DisplayBuilder.playSound(impact, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.4f);
                w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.CRIT, impact, 30, 1.5, 0.5, 1.5, 0.4);
                triggerImpactDamage(impact);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ToyKnifeThrow(plugin); }
    }

    // ================================================================
    // 29. PLUSH FIREWORK — 6 FIREWORK_STAR ItemDisplays arc upward,
    //     FIREWORK_SPARK explosions at the peak. Impact each burst
    //     point, radius 2.5, 16 hearts.
    // ================================================================
    public static class PlushFirework extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> stars = new ArrayList<>();
        private final double[] sAng = new double[6];
        private final double[] sR = new double[6];
        private final double[] sPeakY = new double[6];
        private final boolean[] sBurst = new boolean[6];

        public PlushFirework(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plush_firework", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0); // 16 hearts
            config.setImpactRadius(2.5);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(340);
        }

        @Override protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.4f, 1.0f);

            for (int i = 0; i < 6; i++) {
                sAng[i] = Math.random() * Math.PI * 2;
                sR[i] = 2.0 + Math.random() * 4.5;
                sPeakY[i] = 8.0 + Math.random() * 4.0;
                Location p = center.clone().add(Math.cos(sAng[i]) * sR[i], 0.5, Math.sin(sAng[i]) * sR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FIREWORK_STAR));
                int[] col = RAINBOW[i % RAINBOW.length];
                h.scale(0.8f, 0.8f, 0.8f).glow(col[0], col[1], col[2]).interpolation(2, 0);
                stars.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 GUNPOWDER + 6 PAPER firework launch tubes + 4 GLOW_BERRIES sparks on the ground
            for (int i = 0; i < 6; i++) {
                Location p = center.clone().add(Math.cos(sAng[i]) * sR[i], 0.3, Math.sin(sAng[i]) * sR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GUNPOWDER));
                h.scale(0.65f, 0.65f, 0.65f).glow(150, 150, 160).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                Location p = center.clone().add(Math.cos(sAng[i]) * sR[i], 0.6, Math.sin(sAng[i]) * sR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PAPER));
                h.scale(0.4f, 0.6f, 0.4f).glow(255, 240, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = center.clone().add(Math.cos(a) * 5, 0.3, Math.sin(a) * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                int[] col = RAINBOW[i % RAINBOW.length];
                h.scale(0.5f, 0.5f, 0.5f).glow(col[0], col[1], col[2]).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            double riseDuration = 30.0;

            for (int i = 0; i < stars.size(); i++) {
                if (sBurst[i]) continue;
                double localTick = tick - i * 4;
                if (localTick < 0) continue;
                double t = Math.min(1.0, localTick / riseDuration);
                double y = 0.5 + (sPeakY[i] - 0.5) * t;

                float rx = (float)(Math.cos(sAng[i]) * sR[i]) - 0.4f;
                float rz = (float)(Math.sin(sAng[i]) * sR[i]) - 0.4f;
                stars.get(i).animateTo(new Vector3f(rx, (float)y, rz),
                        new AxisAngle4f((float)(tick * 0.4 + i), 1, 1, 1),
                        new Vector3f(0.8f), 2);

                Location pos = center.clone().add(Math.cos(sAng[i]) * sR[i], y, Math.sin(sAng[i]) * sR[i]);
                int[] col = RAINBOW[i % RAINBOW.length];
                w.spawnParticle(Particle.FIREWORK, pos, 2, 0.1, 0.1, 0.1, 0.02);
                DisplayBuilder.dustParticles(pos, 2, 0.15, col[0], col[1], col[2], 1.4f);

                if (t >= 1.0) {
                    sBurst[i] = true;
                    Location burst = center.clone().add(Math.cos(sAng[i]) * sR[i], sPeakY[i], Math.sin(sAng[i]) * sR[i]);
                    DisplayBuilder.playSound(burst, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.4f, 1.2f);
                    DisplayBuilder.playSound(burst, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.2f, 1.0f);
                    w.spawnParticle(Particle.EXPLOSION, burst, 1, 0, 0, 0, 0);
                    for (int s = 0; s < 40; s++) {
                        double a = Math.random() * Math.PI * 2;
                        double phi = Math.random() * Math.PI;
                        double r = Math.random() * 2.5;
                        Location sp = burst.clone().add(Math.cos(a) * Math.sin(phi) * r, Math.cos(phi) * r, Math.sin(a) * Math.sin(phi) * r);
                        w.spawnParticle(Particle.FIREWORK, sp, 1, 0.05, 0.05, 0.05, 0.05);
                        DisplayBuilder.dustParticles(sp, 1, 0.1, col[0], col[1], col[2], 1.5f);
                    }
                    triggerImpactDamage(burst);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); stars.clear(); }
        @Override public AbstractAttack newInstance() { return new PlushFirework(plugin); }
    }

    // ================================================================
    // 30. ANGRY BEE CLOUD — 15 HONEYCOMB ItemDisplays drift in swarm
    //     toward nearest player. Constant radius 4.0, 9 hearts every
    //     8 ticks.
    // ================================================================
    public static class AngryBeeCloud extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> bees = new ArrayList<>();
        private final double[] bAng = new double[15];
        private final double[] bR = new double[15];
        private final double[] bY = new double[15];
        private final double[] bSpd = new double[15];

        public AngryBeeCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("angry_bee_cloud", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(9.0); // 9 hearts
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(280);
            config.setCooldownTicks(360);
            // Setpiece swarm: stays at spawn (tracksPlayer disabled).
            config.setTracksPlayer(false);
        }

        @Override protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            DisplayBuilder.playSound(center, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 1.4f, 1.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BEE_HURT, 1.0f, 1.4f);

            for (int i = 0; i < 15; i++) {
                bAng[i] = Math.random() * Math.PI * 2;
                bR[i] = 1.5 + Math.random() * 2.5;
                bY[i] = 1.5 + Math.random() * 2.0;
                bSpd[i] = 0.08 + Math.random() * 0.06;
                Location p = center.clone().add(Math.cos(bAng[i]) * bR[i], bY[i], Math.sin(bAng[i]) * bR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.HONEYCOMB));
                h.scale(0.4f, 0.4f, 0.4f).glow(255, 200, 40).interpolation(3, 0);
                bees.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Each bee orbits a wandering attractor near the cloud center
            for (int i = 0; i < bees.size(); i++) {
                bAng[i] += bSpd[i];
                bR[i] = 1.2 + Math.sin(tick * 0.05 + i) * 1.2 + Math.random() * 0.3;
                bY[i] = 1.5 + Math.sin(tick * 0.08 + i * 0.7) * 0.7;
                float rx = (float)(Math.cos(bAng[i]) * bR[i]) - 0.2f;
                float rz = (float)(Math.sin(bAng[i]) * bR[i]) - 0.2f;
                bees.get(i).animateTo(new Vector3f(rx, (float)bY[i], rz),
                        new AxisAngle4f((float)(tick * 0.6 + i), 1, 1, 0),
                        new Vector3f(0.4f), 3);

                // Trail / buzz particles
                if (tick % 2 == 0) {
                    Location bp = center.clone().add(Math.cos(bAng[i]) * bR[i], bY[i], Math.sin(bAng[i]) * bR[i]);
                    w.spawnParticle(Particle.CRIT, bp, 1, 0.05, 0.05, 0.05, 0.02);
                    if (Math.random() < 0.3) DisplayBuilder.dustParticles(bp, 1, 0.05, 255, 200, 40, 1.2f);
                }
            }

            // Buzz layer
            if (tick % 12 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.9f, 1.2f);
            if (tick % 30 == 0) DisplayBuilder.playSound(center, Sound.ENTITY_BEE_STING, 0.9f, 1.2f);
        }

        @Override protected void onCleanup() { super.onCleanup(); bees.clear(); }
        @Override public AbstractAttack newInstance() { return new AngryBeeCloud(plugin); }
    }
}
