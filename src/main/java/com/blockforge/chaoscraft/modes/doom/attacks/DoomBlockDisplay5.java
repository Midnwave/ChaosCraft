package com.blockforge.chaoscraft.modes.doom.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Doom Mode -- BLOCK DISPLAY ATTACKS (Part 5, attacks 41-50)
 * 10 boss-tier BlockDisplay attacks. 25+ blocks each.
 * All use Transformation animation (scale reshaping), no teleport rotation.
 * 5-10 block damage radii.
 *
 * Doom palette:
 * - Lava orange: RGB(255, 100, 20)
 * - Hellfire red: RGB(200, 50, 10)
 * - Charred black: RGB(20, 10, 5)
 * - Ember glow: RGB(240, 80, 30)
 * - Brimstone yellow: RGB(220, 180, 30)
 * - Nether purple: RGB(120, 20, 80)
 * - Blood red: RGB(180, 20, 20)
 *
 * Materials: MAGMA_BLOCK, NETHERRACK, NETHER_BRICKS, RED_NETHER_BRICKS,
 *            BLACKSTONE, POLISHED_BLACKSTONE, CRYING_OBSIDIAN, SHROOMLIGHT,
 *            BASALT, DEEPSLATE, COAL_BLOCK, OBSIDIAN, RED_CONCRETE,
 *            BLACK_CONCRETE, ORANGE_CONCRETE, GRAY_CONCRETE, IRON_BLOCK
 */
public final class DoomBlockDisplay5 {
    private DoomBlockDisplay5() {}

    private static final String MODE_PATH = "modes/doom/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new InfernalColossusHand(plugin));    // 41
        registry.register(new DoomVolcanoEruption(plugin));      // 42
        registry.register(new InfernalCrownOfThorns(plugin));    // 43
        registry.register(new MagmaBlackHole(plugin));           // 44
        registry.register(new HellfireClockTower(plugin));       // 45
        registry.register(new DoomGiantSword(plugin));           // 46
        registry.register(new InfernalOrgan(plugin));            // 47
        registry.register(new MagmaKrakenTentacles(plugin));     // 48
        registry.register(new HellfireThrone(plugin));           // 49
        registry.register(new DoomApocalypseEngine(plugin));     // 50
    }

    // ================================================================
    // Helper: find nearest non-exempt survival player within range
    // ================================================================
    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
        Player nearest = null;
        double nearestDist = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dist = p.getLocation().distanceSquared(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    // ================================================================
    // 41. INFERNAL COLOSSUS HAND (35 displays)
    //     Giant demonic hand: palm 4 (scale 1.0,0.3,0.8), 5 fingers
    //     (3 segments each = 15, scale 0.25,0.5,0.25), wrist 3,
    //     fire cuff ring 8. Fingers curl/uncurl via Transform.
    //     Grabs toward player. 5r, 40/20t.
    // ================================================================
    public static class InfernalColossusHand extends BlockDisplayAttack {
        private Location center;
        private BlockDisplayHandle palmRef;
        private final List<BlockDisplayHandle> palmBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fingerSegments = new ArrayList<>();
        private final List<BlockDisplayHandle> wristBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> cuffBlocks = new ArrayList<>();
        private boolean grabbing = false;
        private int grabTick = 0;

        public InfernalColossusHand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_colossus_hand", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setChance(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Palm: 4 blocks in a flat 2x2 arrangement
            double[][] palmOffsets = {{-0.5, 3.0, -0.4}, {0.5, 3.0, -0.4}, {-0.5, 3.0, 0.4}, {0.5, 3.0, 0.4}};
            for (double[] off : palmOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.NETHERRACK);
                h.scale(1.0f, 0.3f, 0.8f).glow(200, 50, 10).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                palmBlocks.add(h);
                if (palmRef == null) palmRef = h;
            }

            // 5 Fingers: 3 segments each = 15 blocks
            double[] fingerXPositions = {-1.2, -0.6, 0.0, 0.6, 1.2};
            for (int f = 0; f < 5; f++) {
                for (int seg = 0; seg < 3; seg++) {
                    double yOff = 3.3 + seg * 0.55;
                    Location fLoc = center.clone().add(fingerXPositions[f], yOff, -0.8 - seg * 0.4);
                    BlockDisplayHandle finger = displayBuilder.spawnBlock(fLoc, Material.BLACKSTONE);
                    finger.scale(0.25f, 0.5f, 0.25f).glow(20, 10, 5).interpolation(10, 0);
                    spawnedEntities.add(finger.entity());
                    fingerSegments.add(finger);
                }
            }

            // Wrist: 3 blocks
            for (int i = -1; i <= 1; i++) {
                BlockDisplayHandle wr = displayBuilder.spawnBlock(center.clone().add(i * 0.6, 2.2, 0), Material.DEEPSLATE);
                wr.scale(0.6f, 0.5f, 0.6f).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(wr.entity());
                wristBlocks.add(wr);
            }

            // Fire cuff ring: 8 blocks around the wrist
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double cx = Math.cos(angle) * 1.2;
                double cz = Math.sin(angle) * 1.2;
                BlockDisplayHandle cuff = displayBuilder.spawnBlock(center.clone().add(cx, 2.0, cz), Material.MAGMA_BLOCK);
                cuff.scale(0.3f, 0.3f, 0.3f).glow(255, 100, 20).interpolation(10, 0);
                spawnedEntities.add(cuff.entity());
                cuffBlocks.add(cuff);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Finger curl/uncurl animation via Transform
            boolean curling = (tick % 80) < 40;
            float curlAngle = curling ? (float) Math.toRadians(30.0 * Math.sin(tick * 0.15)) : 0f;

            for (int i = 0; i < fingerSegments.size(); i++) {
                BlockDisplayHandle seg = fingerSegments.get(i);
                int segIndex = i % 3; // which segment in finger (0=base, 1=mid, 2=tip)
                float segCurl = curlAngle * (segIndex + 1) * 0.5f;
                Transformation t = seg.entity().getTransformation();
                seg.entity().setInterpolationDuration(10);
                seg.entity().setInterpolationDelay(0);
                seg.entity().setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(segCurl, 1f, 0f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }

            // Grab toward nearest player every 60 ticks
            if (tick % 60 == 30) {
                Player target = findNearestPlayer(center, 10);
                if (target != null) {
                    grabbing = true;
                    grabTick = tick;
                }
            }

            // Grabbing: scale palm larger for 20 ticks
            if (grabbing && tick - grabTick < 20) {
                float scaleBoost = 1.0f + (float) Math.sin((tick - grabTick) * Math.PI / 20) * 0.4f;
                if (palmRef != null) {
                    Transformation t = palmRef.entity().getTransformation();
                    palmRef.entity().setInterpolationDuration(5);
                    palmRef.entity().setInterpolationDelay(0);
                    palmRef.entity().setTransformation(new Transformation(
                            t.getTranslation(), new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(1.0f * scaleBoost, 0.3f * scaleBoost, 0.8f * scaleBoost),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            } else {
                grabbing = false;
            }

            // Cuff fire particles
            if (tick % 5 == 0) {
                for (BlockDisplayHandle cuff : cuffBlocks) {
                    Location cLoc = cuff.entity().getLocation();
                    w.spawnParticle(Particle.FLAME, cLoc, 2, 0.1, 0.2, 0.1, 0.01);
                }
            }

            // Ambient sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new InfernalColossusHand(plugin); }
    }

    // ================================================================
    // 42. DOOM VOLCANO ERUPTION (35 displays)
    //     Volcanic cone with lava bombs that arc parabolically.
    //     Cone: 8 base + 5 mid + 3 upper + 4 crater rim.
    //     6 arcing lava bombs (parabolic Y+X translation).
    //     6 lava flow blocks descending sides.
    //     3 smoke accent blocks.
    //     Bombs deal impact-only 4r, 35 damage. ENTITY_GENERIC_EXPLODE.
    // ================================================================
    public static class DoomVolcanoEruption extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> coneBase = new ArrayList<>();
        private final List<BlockDisplayHandle> coneMid = new ArrayList<>();
        private final List<BlockDisplayHandle> coneUpper = new ArrayList<>();
        private final List<BlockDisplayHandle> craterRim = new ArrayList<>();
        private final List<BlockDisplayHandle> lavaBombs = new ArrayList<>();
        private final List<BlockDisplayHandle> lavaFlows = new ArrayList<>();
        private final double[][] bombTargets = new double[6][3];
        private final int[] bombLaunchTick = new int[6];

        public DoomVolcanoEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_volcano_eruption", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(35.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(250);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(35.0);
            config.setImpactRadius(4.0);
            config.setChance(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Base ring: 8 blocks radius 2.5
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 0, Math.sin(angle) * 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                h.scale(1.2f, 1.0f, 1.2f).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                coneBase.add(h);
            }

            // Mid ring: 5 blocks radius 1.8, Y+1.5
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, 1.5, Math.sin(angle) * 1.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(1.0f, 0.8f, 1.0f).glow(200, 50, 10).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                coneMid.add(h);
            }

            // Upper ring: 3 blocks radius 1.0, Y+3
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 3.0, Math.sin(angle) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                h.scale(0.8f, 0.6f, 0.8f).glow(200, 50, 10).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                coneUpper.add(h);
            }

            // Crater rim: 4 blocks radius 0.7, Y+4
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4 + Math.PI / 4;
                Location loc = center.clone().add(Math.cos(angle) * 0.7, 4.0, Math.sin(angle) * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.5f, 0.3f, 0.5f).glow(255, 100, 20).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                craterRim.add(h);
            }

            // 6 lava bombs (start at crater, will arc outward)
            for (int i = 0; i < 6; i++) {
                BlockDisplayHandle bomb = displayBuilder.spawnBlock(center.clone().add(0, 4.5, 0), Material.MAGMA_BLOCK);
                bomb.scale(0.4f, 0.4f, 0.4f).glow(255, 100, 20).interpolation(5, 0);
                spawnedEntities.add(bomb.entity());
                lavaBombs.add(bomb);

                // Random target offset 3-8 blocks away
                double bAngle = Math.random() * 2 * Math.PI;
                double bDist = 3.0 + Math.random() * 5.0;
                bombTargets[i] = new double[]{Math.cos(bAngle) * bDist, 0, Math.sin(bAngle) * bDist};
                bombLaunchTick[i] = 40 + i * 25; // stagger launches
            }

            // 6 lava flow blocks on cone sides
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double r = 1.5 + Math.random() * 1.0;
                double y = 2.0 + Math.random() * 1.5;
                Location loc = center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                BlockDisplayHandle flow = displayBuilder.spawnBlock(loc, Material.ORANGE_CONCRETE);
                flow.scale(0.3f, 0.6f, 0.3f).glow(240, 80, 30).interpolation(15, 0);
                spawnedEntities.add(flow.entity());
                lavaFlows.add(flow);
            }

            // 3 smoke accents at top
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle smoke = displayBuilder.spawnBlock(
                        center.clone().add((Math.random() - 0.5) * 1.5, 5.0 + i * 0.5, (Math.random() - 0.5) * 1.5),
                        Material.GRAY_CONCRETE);
                smoke.scale(0.5f, 0.3f, 0.5f).glow(80, 80, 80).interpolation(20, 0);
                spawnedEntities.add(smoke.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Lava bomb parabolic arcs
            for (int i = 0; i < 6; i++) {
                if (tick < bombLaunchTick[i]) continue;
                int bombTick = tick - bombLaunchTick[i];
                int flightDuration = 40;

                if (bombTick <= flightDuration) {
                    double progress = (double) bombTick / flightDuration;
                    double px = bombTargets[i][0] * progress;
                    double pz = bombTargets[i][2] * progress;
                    // Parabolic Y: starts at 4.5, arcs up then down to ground
                    double py = 4.5 + 6.0 * progress * (1 - progress) * 4 - 4.5 * progress;

                    Transformation t = lavaBombs.get(i).entity().getTransformation();
                    lavaBombs.get(i).entity().setInterpolationDuration(5);
                    lavaBombs.get(i).entity().setInterpolationDelay(0);
                    lavaBombs.get(i).entity().setTransformation(new Transformation(
                            new Vector3f((float) px, (float) py, (float) pz),
                            new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                    ));

                    // Trail particles
                    if (bombTick % 3 == 0) {
                        Location bombLoc = center.clone().add(px, py, pz);
                        w.spawnParticle(Particle.FLAME, bombLoc, 3, 0.1, 0.1, 0.1, 0.02);
                    }
                } else if (bombTick == flightDuration + 1) {
                    // Impact
                    Location impactLoc = center.clone().add(bombTargets[i][0], 0, bombTargets[i][2]);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                    w.spawnParticle(Particle.LAVA, impactLoc, 20, 1.5, 0.5, 1.5, 0.1);
                    w.spawnParticle(Particle.FLAME, impactLoc, 15, 1.0, 0.3, 1.0, 0.05);
                    triggerImpactDamage(impactLoc);

                    // Shrink bomb to nothing
                    Transformation t = lavaBombs.get(i).entity().getTransformation();
                    lavaBombs.get(i).entity().setInterpolationDuration(5);
                    lavaBombs.get(i).entity().setInterpolationDelay(0);
                    lavaBombs.get(i).entity().setTransformation(new Transformation(
                            t.getTranslation(), new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.01f, 0.01f, 0.01f), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Lava flows descend slowly via transform translation
            if (tick % 20 == 0) {
                for (BlockDisplayHandle flow : lavaFlows) {
                    Transformation t = flow.entity().getTransformation();
                    float newY = t.getTranslation().y - 0.3f;
                    if (newY < -3.0f) newY = 2.0f; // reset to top
                    flow.entity().setInterpolationDuration(15);
                    flow.entity().setInterpolationDelay(0);
                    flow.entity().setTransformation(new Transformation(
                            new Vector3f(t.getTranslation().x, newY, t.getTranslation().z),
                            new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Smoke particles at top
            if (tick % 8 == 0) {
                w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center.clone().add(0, 5, 0), 5, 0.8, 0.5, 0.8, 0.01);
            }

            // Rumble sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DoomVolcanoEruption(plugin); }
    }

    // ================================================================
    // 43. INFERNAL CROWN OF THORNS (30 displays)
    //     Crown at Y=8: 8 thorns (2 blocks each, varying heights, tilted 30deg),
    //     8 band blocks ring, 6 jewel accents (shroomlight), 4 inner fire blocks,
    //     4 dripping blocks below. Descends to player Y+2. Wobble per spike.
    //     7r below, 30/25t.
    // ================================================================
    public static class InfernalCrownOfThorns extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> thorns = new ArrayList<>();
        private final List<BlockDisplayHandle> band = new ArrayList<>();
        private final List<BlockDisplayHandle> jewels = new ArrayList<>();
        private final List<BlockDisplayHandle> innerFire = new ArrayList<>();
        private final List<BlockDisplayHandle> drips = new ArrayList<>();
        private boolean descending = true;
        private float currentY = 8.0f;

        public InfernalCrownOfThorns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_crown_of_thorns", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(25);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(220);
            config.setCooldownTicks(280);
            config.setChance(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            float crownY = 8.0f;
            double crownRadius = 2.0;

            // 8 thorns: 2 segments each, tilted outward 30deg
            float[] heights = {0.8f, 0.6f, 1.0f, 0.7f, 0.9f, 0.65f, 1.1f, 0.75f};
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                float tiltAngle = (float) Math.toRadians(30);

                for (int seg = 0; seg < 2; seg++) {
                    Location loc = center.clone().add(
                            Math.cos(angle) * (crownRadius + seg * 0.3),
                            crownY + seg * heights[i],
                            Math.sin(angle) * (crownRadius + seg * 0.3));
                    BlockDisplayHandle thorn = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                    thorn.scale(0.2f, 0.8f, 0.2f)
                            .rotate(tiltAngle, (float) Math.cos(angle), 0f, (float) Math.sin(angle))
                            .glow(20, 10, 5).interpolation(10, 0);
                    spawnedEntities.add(thorn.entity());
                    thorns.add(thorn);
                }
            }

            // 8 band blocks in a ring
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8 + Math.PI / 8;
                Location loc = center.clone().add(Math.cos(angle) * crownRadius, crownY - 0.2, Math.sin(angle) * crownRadius);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                b.scale(0.6f, 0.3f, 0.6f).glow(180, 20, 20).interpolation(10, 0);
                spawnedEntities.add(b.entity());
                band.add(b);
            }

            // 6 jewel accents (shroomlight)
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * (crownRadius + 0.1), crownY + 0.1, Math.sin(angle) * (crownRadius + 0.1));
                BlockDisplayHandle j = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                j.scale(0.15f, 0.15f, 0.15f).glow(240, 80, 30).interpolation(10, 0);
                spawnedEntities.add(j.entity());
                jewels.add(j);
            }

            // 4 inner fire blocks
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, crownY + 0.3, Math.sin(angle) * 1.0);
                BlockDisplayHandle f = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                f.scale(0.3f, 0.3f, 0.3f).glow(255, 100, 20).interpolation(10, 0);
                spawnedEntities.add(f.entity());
                innerFire.add(f);
            }

            // 4 dripping blocks below crown
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4 + Math.PI / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, crownY - 1.0, Math.sin(angle) * 1.5);
                BlockDisplayHandle d = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                d.scale(0.15f, 0.6f, 0.15f).glow(120, 20, 80).interpolation(10, 0);
                spawnedEntities.add(d.entity());
                drips.add(d);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Descend from Y+8 to Y+2 over first 60 ticks
            if (descending && currentY > 2.0f) {
                currentY -= 0.1f;
                if (currentY <= 2.0f) {
                    currentY = 2.0f;
                    descending = false;
                }

                // Shift all display blocks downward via Transform translation
                float yShift = -(8.0f - currentY) * 0.02f;
                for (BlockDisplayHandle h : thorns) {
                    Transformation t = h.entity().getTransformation();
                    h.entity().setInterpolationDuration(5);
                    h.entity().setInterpolationDelay(0);
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(t.getTranslation().x, t.getTranslation().y + yShift, t.getTranslation().z),
                            new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Thorn wobble: each thorn oscillates slightly
            if (tick % 10 == 0) {
                for (int i = 0; i < thorns.size(); i++) {
                    BlockDisplayHandle thorn = thorns.get(i);
                    float wobble = (float) Math.toRadians(5.0 * Math.sin(tick * 0.2 + i * 0.5));
                    float baseAngle = (float) Math.toRadians(30);
                    Transformation t = thorn.entity().getTransformation();
                    thorn.entity().setInterpolationDuration(10);
                    thorn.entity().setInterpolationDelay(0);
                    thorn.entity().setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(baseAngle + wobble, 1f, 0f, 0f),
                            t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Drip particles below
            if (tick % 8 == 0) {
                for (BlockDisplayHandle d : drips) {
                    Location dLoc = d.entity().getLocation();
                    w.spawnParticle(Particle.DRIPPING_LAVA, dLoc.clone().add(0, -0.5, 0), 2, 0.1, 0.2, 0.1, 0);
                }
            }

            // Inner fire particle pulse
            if (tick % 6 == 0) {
                for (BlockDisplayHandle f : innerFire) {
                    w.spawnParticle(Particle.FLAME, f.entity().getLocation(), 3, 0.2, 0.2, 0.2, 0.01);
                }
            }

            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new InfernalCrownOfThorns(plugin); }
    }

    // ================================================================
    // 44. MAGMA BLACK HOLE (32 displays)
    //     Accretion disc 16 (tilted 30deg, scale 0.5,0.1,0.5),
    //     core 4 blackstone (scale 0.3,0.3,0.3),
    //     jets 8 (4 up + 4 down, scale 0.15,1.5,0.15),
    //     4 spiral debris orbiting.
    //     Disc rapid rotation. Jets pulse. Player velocity pull.
    //     8r, 45/20t.
    // ================================================================
    public static class MagmaBlackHole extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> disc = new ArrayList<>();
        private final List<BlockDisplayHandle> core = new ArrayList<>();
        private final List<BlockDisplayHandle> jetsUp = new ArrayList<>();
        private final List<BlockDisplayHandle> jetsDown = new ArrayList<>();
        private final List<BlockDisplayHandle> debris = new ArrayList<>();

        public MagmaBlackHole(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_black_hole", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(45.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(200);
            config.setCooldownTicks(350);
            config.setChance(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone().add(0, 5, 0);
            World w = center.getWorld();
            if (w == null) return;

            Location coreCenter = this.center;
            float tiltAngle = (float) Math.toRadians(30);

            // Accretion disc: 16 blocks in tilted ring
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                double dx = Math.cos(angle) * 3.5;
                double dz = Math.sin(angle) * 3.5;
                double dy = Math.sin(angle) * 3.5 * Math.sin(tiltAngle) * 0.3;
                Location loc = coreCenter.clone().add(dx, dy, dz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.5f, 0.1f, 0.5f).glow(255, 100, 20).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                disc.add(h);
            }

            // Core: 4 blackstone blocks
            double[][] coreOffsets = {{0.15, 0, 0.15}, {-0.15, 0, 0.15}, {0.15, 0, -0.15}, {-0.15, 0, -0.15}};
            for (double[] off : coreOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(coreCenter.clone().add(off[0], off[1], off[2]), Material.BLACKSTONE);
                h.scale(0.3f, 0.3f, 0.3f).glow(10, 5, 15).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                core.add(h);
            }

            // Jets up: 4 blocks stretching upward
            for (int i = 0; i < 4; i++) {
                Location loc = coreCenter.clone().add((i - 1.5) * 0.2, 1.0 + i * 1.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.15f, 1.5f, 0.15f).glow(240, 80, 30).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                jetsUp.add(h);
            }

            // Jets down: 4 blocks stretching downward
            for (int i = 0; i < 4; i++) {
                Location loc = coreCenter.clone().add((i - 1.5) * 0.2, -1.0 - i * 1.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.15f, 1.5f, 0.15f).glow(240, 80, 30).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                jetsDown.add(h);
            }

            // 4 spiral debris
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                Location loc = coreCenter.clone().add(Math.cos(angle) * 2.0, 0, Math.sin(angle) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(0.25f, 0.25f, 0.25f).glow(200, 50, 10).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                debris.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Disc rotation via Transform translation
            float discRotSpeed = 0.08f;
            for (int i = 0; i < disc.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 16;
                double currentAngle = baseAngle + tick * discRotSpeed;
                float dx = (float) (Math.cos(currentAngle) * 3.5);
                float dz = (float) (Math.sin(currentAngle) * 3.5);
                float dy = (float) (Math.sin(currentAngle) * 3.5 * Math.sin(Math.toRadians(30)) * 0.3);

                Transformation t = disc.get(i).entity().getTransformation();
                disc.get(i).entity().setInterpolationDuration(5);
                disc.get(i).entity().setInterpolationDelay(0);
                disc.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(dx, dy, dz),
                        new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            // Debris orbit
            for (int i = 0; i < debris.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 4;
                double currentAngle = baseAngle + tick * 0.12;
                float dx = (float) (Math.cos(currentAngle) * 2.0);
                float dz = (float) (Math.sin(currentAngle) * 2.0);
                float dy = (float) (Math.sin(currentAngle * 2) * 0.5);

                Transformation t = debris.get(i).entity().getTransformation();
                debris.get(i).entity().setInterpolationDuration(5);
                debris.get(i).entity().setInterpolationDelay(0);
                debris.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(dx, dy, dz),
                        new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            // Jets pulse: scale Y oscillation
            if (tick % 10 == 0) {
                float pulse = 1.5f + (float) Math.sin(tick * 0.2) * 0.5f;
                for (BlockDisplayHandle j : jetsUp) {
                    Transformation t = j.entity().getTransformation();
                    j.entity().setInterpolationDuration(8);
                    j.entity().setInterpolationDelay(0);
                    j.entity().setTransformation(new Transformation(
                            t.getTranslation(), new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.15f, pulse, 0.15f), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                for (BlockDisplayHandle j : jetsDown) {
                    Transformation t = j.entity().getTransformation();
                    j.entity().setInterpolationDuration(8);
                    j.entity().setInterpolationDelay(0);
                    j.entity().setTransformation(new Transformation(
                            t.getTranslation(), new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.15f, pulse, 0.15f), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Player velocity pull toward center
            if (tick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist < 10.0 && dist > 0.5) {
                        org.bukkit.util.Vector dir = center.toVector().subtract(p.getLocation().toVector()).normalize();
                        double pullStrength = 0.15 * (1.0 - dist / 10.0);
                        p.setVelocity(p.getVelocity().add(dir.multiply(pullStrength)));
                    }
                }
            }

            // Core particle effect
            if (tick % 4 == 0) {
                w.spawnParticle(Particle.PORTAL, center, 10, 0.3, 0.3, 0.3, 0.5);
                DisplayBuilder.dustParticles(center, 5, 0.5, 10, 5, 15, 2.0f);
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MagmaBlackHole(plugin); }
    }

    // ================================================================
    // 45. HELLFIRE CLOCK TOWER (35 displays)
    //     Tower 12 tapered, clock face 8 circle, hands 2, spire 4,
    //     bells 4, buttress 5. Hands rotate at different speeds.
    //     Bells swing +/-15deg every 60t. 6r, 25/20t.
    //     BLOCK_BELL_USE sound.
    // ================================================================
    public static class HellfireClockTower extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> tower = new ArrayList<>();
        private final List<BlockDisplayHandle> clockFace = new ArrayList<>();
        private BlockDisplayHandle hourHand;
        private BlockDisplayHandle minuteHand;
        private final List<BlockDisplayHandle> spire = new ArrayList<>();
        private final List<BlockDisplayHandle> bells = new ArrayList<>();
        private final List<BlockDisplayHandle> buttress = new ArrayList<>();

        public HellfireClockTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_clock_tower", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(240);
            config.setCooldownTicks(300);
            config.setChance(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Tower: 12 blocks tapering upward
            float[] towerWidths = {1.2f, 1.2f, 1.1f, 1.1f, 1.0f, 1.0f, 0.9f, 0.9f, 0.8f, 0.8f, 0.7f, 0.7f};
            for (int i = 0; i < 12; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, i, 0), Material.NETHER_BRICKS);
                h.scale(towerWidths[i], 1.0f, towerWidths[i]).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                tower.add(h);
            }

            // Clock face: 8 blocks in circle at Y=10
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 0.9, 10, Math.sin(angle) * 0.9);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.3f, 0.3f, 0.1f).glow(80, 80, 80).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                clockFace.add(h);
            }

            // Hour hand: shorter, thicker
            hourHand = displayBuilder.spawnBlock(center.clone().add(0, 10, 0), Material.IRON_BLOCK);
            hourHand.scale(0.1f, 0.5f, 0.1f).glow(180, 20, 20).interpolation(10, 0);
            spawnedEntities.add(hourHand.entity());

            // Minute hand: longer, thinner
            minuteHand = displayBuilder.spawnBlock(center.clone().add(0, 10, 0), Material.IRON_BLOCK);
            minuteHand.scale(0.1f, 0.8f, 0.1f).glow(180, 20, 20).interpolation(10, 0);
            spawnedEntities.add(minuteHand.entity());

            // Spire: 4 blocks above clock
            for (int i = 0; i < 4; i++) {
                float spireWidth = 0.5f - i * 0.1f;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 12 + i, 0), Material.RED_NETHER_BRICKS);
                h.scale(spireWidth, 1.0f, spireWidth).glow(200, 50, 10).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                spire.add(h);
            }

            // Bells: 4 at Y=9
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.2, 9, Math.sin(angle) * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.3f, 0.4f, 0.3f).glow(220, 180, 30).interpolation(15, 0);
                spawnedEntities.add(h.entity());
                bells.add(h);
            }

            // Buttress: 5 support blocks at base
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, 0.5, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(0.4f, 1.0f, 0.4f).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                buttress.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 2.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Hour hand rotation (slow)
            float hourAngle = (float) Math.toRadians(tick * 0.5);
            Transformation ht = hourHand.entity().getTransformation();
            hourHand.entity().setInterpolationDuration(10);
            hourHand.entity().setInterpolationDelay(0);
            hourHand.entity().setTransformation(new Transformation(
                    ht.getTranslation(),
                    new AxisAngle4f(hourAngle, 0f, 0f, 1f),
                    ht.getScale(), new AxisAngle4f().set(ht.getRightRotation())
            ));

            // Minute hand rotation (fast)
            float minuteAngle = (float) Math.toRadians(tick * 3.0);
            Transformation mt = minuteHand.entity().getTransformation();
            minuteHand.entity().setInterpolationDuration(10);
            minuteHand.entity().setInterpolationDelay(0);
            minuteHand.entity().setTransformation(new Transformation(
                    mt.getTranslation(),
                    new AxisAngle4f(minuteAngle, 0f, 0f, 1f),
                    mt.getScale(), new AxisAngle4f().set(mt.getRightRotation())
            ));

            // Bells swing every 60 ticks
            if (tick % 60 < 30) {
                float swingAngle = (float) Math.toRadians(15.0 * Math.sin(tick * Math.PI / 30));
                for (BlockDisplayHandle bell : bells) {
                    Transformation bt = bell.entity().getTransformation();
                    bell.entity().setInterpolationDuration(15);
                    bell.entity().setInterpolationDelay(0);
                    bell.entity().setTransformation(new Transformation(
                            bt.getTranslation(),
                            new AxisAngle4f(swingAngle, 0f, 0f, 1f),
                            bt.getScale(), new AxisAngle4f().set(bt.getRightRotation())
                    ));
                }
            }

            // Bell sound
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 1.5f, 0.8f);
            }

            // Fire particles along tower
            if (tick % 10 == 0) {
                for (int y = 0; y < 12; y += 3) {
                    w.spawnParticle(Particle.FLAME, center.clone().add(0.8, y, 0), 2, 0.2, 0.3, 0.2, 0.01);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new HellfireClockTower(plugin); }
    }

    // ================================================================
    // 46. DOOM GIANT SWORD (30 displays)
    //     Blade 12 tapered (scale 0.8,0.1,0.3 -> 0.15,0.1,0.15),
    //     fuller 4, cross-guard 4 (scale 0.15,0.3,1.0), grip 4, pommel 2,
    //     ground cracks 4. Blade micro-vibration Transform.
    //     Pull = burst 10r, 50 damage.
    // ================================================================
    public static class DoomGiantSword extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> blade = new ArrayList<>();
        private final List<BlockDisplayHandle> fuller = new ArrayList<>();
        private final List<BlockDisplayHandle> crossGuard = new ArrayList<>();
        private final List<BlockDisplayHandle> grip = new ArrayList<>();
        private final List<BlockDisplayHandle> pommel = new ArrayList<>();
        private final List<BlockDisplayHandle> groundCracks = new ArrayList<>();
        private boolean pulled = false;
        private int pullTick = -1;

        public DoomGiantSword(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_giant_sword", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(50.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(200);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(10.0);
            config.setChance(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Blade: 12 blocks tapering from base to tip
            float[] bladeWidths = {0.8f, 0.75f, 0.7f, 0.65f, 0.6f, 0.55f, 0.5f, 0.4f, 0.35f, 0.3f, 0.2f, 0.15f};
            float[] bladeDepths = {0.3f, 0.3f, 0.28f, 0.28f, 0.25f, 0.25f, 0.22f, 0.2f, 0.18f, 0.18f, 0.15f, 0.15f};
            for (int i = 0; i < 12; i++) {
                Location loc = center.clone().add(0, 0.5 + i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(bladeWidths[i], 0.1f, bladeDepths[i]).glow(180, 180, 200).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                blade.add(h);
            }

            // Fuller: 4 dark stripe blocks along blade center
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 2.0 + i * 1.6, 0.01);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.COAL_BLOCK);
                h.scale(0.15f, 0.1f, 0.05f).glow(20, 10, 5).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                fuller.add(h);
            }

            // Cross-guard: 4 blocks extending horizontally
            for (int i = 0; i < 4; i++) {
                double zOff = (i < 2) ? -0.5 - i * 0.5 : 0.5 + (i - 2) * 0.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 0.3, zOff), Material.RED_NETHER_BRICKS);
                h.scale(0.15f, 0.3f, 1.0f).glow(200, 50, 10).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                crossGuard.add(h);
            }

            // Grip: 4 blocks below cross-guard
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, -0.2 - i * 0.4, 0), Material.NETHER_BRICKS);
                h.scale(0.2f, 0.4f, 0.2f).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                grip.add(h);
            }

            // Pommel: 2 blocks at bottom
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, -1.8 - i * 0.3, 0), Material.MAGMA_BLOCK);
                h.scale(0.35f, 0.3f, 0.35f).glow(255, 100, 20).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                pommel.add(h);
            }

            // Ground cracks: 4 radiating from base
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4 + Math.PI / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, -0.4, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.8f, 0.05f, 0.15f).glow(10, 5, 15).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                groundCracks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Blade micro-vibration via Transform
            if (tick % 2 == 0) {
                float vibX = (float) (Math.random() * 0.02 - 0.01);
                float vibZ = (float) (Math.random() * 0.02 - 0.01);
                for (BlockDisplayHandle b : blade) {
                    Transformation t = b.entity().getTransformation();
                    b.entity().setInterpolationDuration(2);
                    b.entity().setInterpolationDelay(0);
                    b.entity().setTransformation(new Transformation(
                            new Vector3f(t.getTranslation().x + vibX, t.getTranslation().y, t.getTranslation().z + vibZ),
                            new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // "Pull" event at tick 100: burst damage
            if (tick == 100 && !pulled) {
                pulled = true;
                pullTick = tick;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 2.0f, 0.5f);
                w.spawnParticle(Particle.EXPLOSION, center, 5, 2.0, 2.0, 2.0, 0.1);
                w.spawnParticle(Particle.FLAME, center, 40, 5.0, 3.0, 5.0, 0.05);
                triggerImpactDamage(center);
            }

            // After pull: sword rises slowly via transform
            if (pulled && tick > pullTick) {
                for (BlockDisplayHandle b : blade) {
                    Transformation t = b.entity().getTransformation();
                    b.entity().setInterpolationDuration(5);
                    b.entity().setInterpolationDelay(0);
                    b.entity().setTransformation(new Transformation(
                            new Vector3f(t.getTranslation().x, t.getTranslation().y + 0.02f, t.getTranslation().z),
                            new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Hum sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.3f);
            }

            // Glow particles along blade
            if (tick % 6 == 0) {
                for (int i = 0; i < 12; i += 3) {
                    Location pLoc = center.clone().add(0, 0.5 + i * 0.8, 0);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.2, 180, 180, 200, 1.0f);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DoomGiantSword(plugin); }
    }

    // ================================================================
    // 47. INFERNAL ORGAN (32 displays)
    //     12 pipes (scale 0.2,varying,0.2), console 6, keyboard 4
    //     (scale 1.0,0.1,0.3), 6 accents, 4 fire-tops.
    //     Pipes emit FLAME sequentially (organ playing).
    //     6r, 20/15t. BLOCK_NOTE_BLOCK_BASS sequence.
    // ================================================================
    public static class InfernalOrgan extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> pipes = new ArrayList<>();
        private final List<BlockDisplayHandle> console = new ArrayList<>();
        private final List<BlockDisplayHandle> keyboard = new ArrayList<>();
        private final List<BlockDisplayHandle> accents = new ArrayList<>();
        private final List<BlockDisplayHandle> fireTops = new ArrayList<>();
        private static final float[] PIPE_HEIGHTS = {3.0f, 4.5f, 3.5f, 5.0f, 2.5f, 4.0f, 5.5f, 3.0f, 4.5f, 2.0f, 3.8f, 5.2f};

        public InfernalOrgan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_organ", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(220);
            config.setCooldownTicks(280);
            config.setChance(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // 12 pipes of varying heights
            for (int i = 0; i < 12; i++) {
                double xOff = (i - 5.5) * 0.45;
                Location loc = center.clone().add(xOff, 1.5, -1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                h.scale(0.2f, PIPE_HEIGHTS[i], 0.2f).glow(80, 80, 80).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                pipes.add(h);
            }

            // Console: 6 blocks forming the organ body
            double[][] consoleOffsets = {{-1.5, 0, 0}, {-0.5, 0, 0}, {0.5, 0, 0}, {1.5, 0, 0}, {-0.5, 1, 0}, {0.5, 1, 0}};
            for (double[] off : consoleOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.NETHER_BRICKS);
                h.scale(1.0f, 1.0f, 0.8f).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                console.add(h);
            }

            // Keyboard: 4 blocks at front
            for (int i = 0; i < 4; i++) {
                double xOff = (i - 1.5) * 0.8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(xOff, 1.0, 0.8), Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 0.1f, 0.3f).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                keyboard.add(h);
            }

            // 6 accents (decorative trim)
            for (int i = 0; i < 6; i++) {
                double xOff = (i - 2.5) * 0.7;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(xOff, 1.3, -1.2), Material.RED_NETHER_BRICKS);
                h.scale(0.15f, 0.15f, 0.15f).glow(200, 50, 10).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                accents.add(h);
            }

            // 4 fire-topped pipes (at pipes 0, 3, 6, 9)
            int[] fireIndices = {0, 3, 6, 9};
            for (int pipeIdx : fireIndices) {
                double xOff = (pipeIdx - 5.5) * 0.45;
                float topY = 1.5f + PIPE_HEIGHTS[pipeIdx] + 0.2f;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(xOff, topY, -1.5), Material.SHROOMLIGHT);
                h.scale(0.25f, 0.25f, 0.25f).glow(255, 100, 20).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                fireTops.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BASS, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Sequential pipe flame emission (organ playing)
            int activePipe = (tick / 8) % 12;
            if (tick % 8 == 0) {
                double xOff = (activePipe - 5.5) * 0.45;
                Location pipeTop = center.clone().add(xOff, 1.5 + PIPE_HEIGHTS[activePipe], -1.5);
                w.spawnParticle(Particle.FLAME, pipeTop, 5, 0.05, 0.3, 0.05, 0.02);

                // Scale pulse on active pipe
                BlockDisplayHandle pipe = pipes.get(activePipe);
                Transformation t = pipe.entity().getTransformation();
                pipe.entity().setInterpolationDuration(4);
                pipe.entity().setInterpolationDelay(0);
                pipe.entity().setTransformation(new Transformation(
                        t.getTranslation(), new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(0.25f, t.getScale().y * 1.05f, 0.25f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));

                // Note sound with pitch based on pipe index
                float pitch = 0.5f + activePipe * 0.1f;
                DisplayBuilder.playSound(pipeTop, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, pitch);
            }

            // Reset previous pipe scale
            if (tick % 8 == 4 && tick > 8) {
                int prevPipe = ((tick / 8) - 1 + 12) % 12;
                BlockDisplayHandle pipe = pipes.get(prevPipe);
                Transformation t = pipe.entity().getTransformation();
                pipe.entity().setInterpolationDuration(4);
                pipe.entity().setInterpolationDelay(0);
                pipe.entity().setTransformation(new Transformation(
                        t.getTranslation(), new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(0.2f, PIPE_HEIGHTS[prevPipe], 0.2f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            // Fire top flicker
            if (tick % 5 == 0) {
                for (BlockDisplayHandle ft : fireTops) {
                    w.spawnParticle(Particle.FLAME, ft.entity().getLocation(), 2, 0.05, 0.15, 0.05, 0.01);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new InfernalOrgan(plugin); }
    }

    // ================================================================
    // 48. MAGMA KRAKEN TENTACLES (35 displays)
    //     5 tentacles (6 segments each = 30, scale decreasing 0.6->0.2),
    //     maw 5 blocks. Independent sine wave per tentacle with phase offset.
    //     Sweeps for players. Tentacle 4r, 30 damage. Maw 3r, 50/30t.
    // ================================================================
    public static class MagmaKrakenTentacles extends BlockDisplayAttack {
        private Location center;
        private final List<List<BlockDisplayHandle>> tentacles = new ArrayList<>();
        private final List<BlockDisplayHandle> maw = new ArrayList<>();
        private final double[] tentaclePhases = new double[5];
        private final double[] tentacleAngles = new double[5];

        public MagmaKrakenTentacles(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_kraken_tentacles", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(30.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(30);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(240);
            config.setCooldownTicks(320);
            config.setChance(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Maw: 5 blocks at center forming mouth
            double[][] mawOffsets = {{0, 0, 0}, {0.5, 0.3, 0}, {-0.5, 0.3, 0}, {0, 0.3, 0.5}, {0, 0.3, -0.5}};
            for (double[] off : mawOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.MAGMA_BLOCK);
                h.scale(0.6f, 0.4f, 0.6f).glow(255, 100, 20).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                maw.add(h);
            }

            // 5 tentacles, each with 6 segments
            float[] segScales = {0.6f, 0.5f, 0.45f, 0.35f, 0.25f, 0.2f};
            for (int t = 0; t < 5; t++) {
                double baseAngle = (2 * Math.PI * t) / 5;
                tentaclePhases[t] = Math.random() * Math.PI * 2;
                tentacleAngles[t] = baseAngle;
                List<BlockDisplayHandle> segments = new ArrayList<>();

                for (int s = 0; s < 6; s++) {
                    double dist = 1.0 + s * 1.0;
                    Location loc = center.clone().add(Math.cos(baseAngle) * dist, 0.5 + s * 0.3, Math.sin(baseAngle) * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, s < 3 ? Material.NETHERRACK : Material.RED_NETHER_BRICKS);
                    h.scale(segScales[s], segScales[s], segScales[s]).glow(200, 50, 10).interpolation(8, 0);
                    spawnedEntities.add(h.entity());
                    segments.add(h);
                }
                tentacles.add(segments);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Animate each tentacle with independent sine wave
            for (int t = 0; t < 5; t++) {
                List<BlockDisplayHandle> segments = tentacles.get(t);
                double baseAngle = tentacleAngles[t];
                double phase = tentaclePhases[t];

                for (int s = 0; s < segments.size(); s++) {
                    double dist = 1.0 + s * 1.0;
                    // Sine wave: each segment has increasing amplitude
                    double waveY = Math.sin(tick * 0.1 + phase + s * 0.8) * (0.3 + s * 0.2);
                    double waveX = Math.sin(tick * 0.07 + phase + s * 0.5) * s * 0.15;

                    float tx = (float) (Math.cos(baseAngle) * dist + waveX);
                    float ty = (float) (0.5 + s * 0.3 + waveY);
                    float tz = (float) (Math.sin(baseAngle) * dist + waveX);

                    BlockDisplayHandle seg = segments.get(s);
                    Transformation tr = seg.entity().getTransformation();
                    seg.entity().setInterpolationDuration(8);
                    seg.entity().setInterpolationDelay(0);
                    seg.entity().setTransformation(new Transformation(
                            new Vector3f(tx, ty, tz),
                            tr.getLeftRotation(), tr.getScale(), tr.getRightRotation()
                    ));
                }
            }

            // Maw pulse
            if (tick % 20 == 0) {
                float pulse = 0.6f + (float) Math.sin(tick * 0.3) * 0.15f;
                for (BlockDisplayHandle m : maw) {
                    Transformation t = m.entity().getTransformation();
                    m.entity().setInterpolationDuration(10);
                    m.entity().setInterpolationDelay(0);
                    m.entity().setTransformation(new Transformation(
                            t.getTranslation(), new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(pulse, 0.4f, pulse), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Particle trail on tentacle tips
            if (tick % 4 == 0) {
                for (List<BlockDisplayHandle> segs : tentacles) {
                    Location tipLoc = segs.get(5).entity().getLocation();
                    w.spawnParticle(Particle.FLAME, tipLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_AMBIENT, 0.8f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MagmaKrakenTentacles(plugin); }
    }

    // ================================================================
    // 49. HELLFIRE THRONE (30 displays)
    //     Seat 4, high back 8 arch tapered, armrests 4 each side,
    //     base steps 6, crown hover 4 shroomlight ring.
    //     Rotates to face nearest player. Fire aura particles.
    //     8r, 25/25t.
    // ================================================================
    public static class HellfireThrone extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> seat = new ArrayList<>();
        private final List<BlockDisplayHandle> back = new ArrayList<>();
        private final List<BlockDisplayHandle> armrestL = new ArrayList<>();
        private final List<BlockDisplayHandle> armrestR = new ArrayList<>();
        private final List<BlockDisplayHandle> steps = new ArrayList<>();
        private final List<BlockDisplayHandle> crown = new ArrayList<>();
        private float currentFacingAngle = 0;

        public HellfireThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_throne", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(25.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(25);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(220);
            config.setCooldownTicks(280);
            config.setChance(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Seat: 4 blocks (2x2)
            double[][] seatOffsets = {{-0.4, 1.5, -0.3}, {0.4, 1.5, -0.3}, {-0.4, 1.5, 0.3}, {0.4, 1.5, 0.3}};
            for (double[] off : seatOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], off[1], off[2]), Material.RED_NETHER_BRICKS);
                h.scale(0.6f, 0.2f, 0.6f).glow(180, 20, 20).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                seat.add(h);
            }

            // High back: 8 blocks tapering upward in arch
            float[] backWidths = {1.0f, 0.95f, 0.9f, 0.85f, 0.75f, 0.65f, 0.5f, 0.3f};
            for (int i = 0; i < 8; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 1.8 + i * 0.6, -0.6), Material.NETHER_BRICKS);
                h.scale(backWidths[i], 0.6f, 0.3f).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                back.add(h);
            }

            // Left armrest: 4 blocks
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(-0.8, 1.5 + i * 0.3, -0.3 + i * 0.25), Material.DEEPSLATE);
                h.scale(0.2f, 0.3f, 0.5f).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                armrestL.add(h);
            }

            // Right armrest: 4 blocks
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0.8, 1.5 + i * 0.3, -0.3 + i * 0.25), Material.DEEPSLATE);
                h.scale(0.2f, 0.3f, 0.5f).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                armrestR.add(h);
            }

            // Base steps: 6 blocks
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.8f, 0.5f, 0.8f).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                steps.add(h);
            }

            // Crown: 4 shroomlight blocks hovering above
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 6.5, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.2f, 0.2f, 0.2f).glow(240, 80, 30).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                crown.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Rotate back to face nearest player via Transform
            Player target = findNearestPlayer(center, 15);
            if (target != null && tick % 10 == 0) {
                double dx = target.getLocation().getX() - center.getX();
                double dz = target.getLocation().getZ() - center.getZ();
                float targetAngle = (float) Math.atan2(dz, dx);

                // Smooth rotation
                float angleDiff = targetAngle - currentFacingAngle;
                while (angleDiff > Math.PI) angleDiff -= (float) (2 * Math.PI);
                while (angleDiff < -Math.PI) angleDiff += (float) (2 * Math.PI);
                currentFacingAngle += angleDiff * 0.15f;

                // Apply rotation to back pieces via Transform
                for (BlockDisplayHandle b : back) {
                    Transformation t = b.entity().getTransformation();
                    b.entity().setInterpolationDuration(10);
                    b.entity().setInterpolationDelay(0);
                    b.entity().setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(currentFacingAngle, 0f, 1f, 0f),
                            t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Crown hover bob
            if (tick % 8 == 0) {
                float bob = (float) Math.sin(tick * 0.1) * 0.15f;
                for (BlockDisplayHandle c : crown) {
                    Transformation t = c.entity().getTransformation();
                    c.entity().setInterpolationDuration(8);
                    c.entity().setInterpolationDelay(0);
                    c.entity().setTransformation(new Transformation(
                            new Vector3f(t.getTranslation().x, t.getTranslation().y + bob, t.getTranslation().z),
                            new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Fire aura particles
            if (tick % 5 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double r = 1.0 + Math.random() * 1.5;
                    Location pLoc = center.clone().add(Math.cos(angle) * r, 0.5 + Math.random() * 3, Math.sin(angle) * r);
                    w.spawnParticle(Particle.FLAME, pLoc, 1, 0.05, 0.1, 0.05, 0.01);
                }
            }

            if (tick % 35 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new HellfireThrone(plugin); }
    }

    // ================================================================
    // 50. DOOM APOCALYPSE ENGINE (40 displays)
    //     Core 6 pulsing, 3 orbital rings (8 each on different axes),
    //     6 piston arms (2 blocks each, scale 0.2,1.0,0.2, extend/retract),
    //     4 exhaust flame blocks, 6 debris orbit.
    //     Everything moves. 10r, 35/15t.
    // ================================================================
    public static class DoomApocalypseEngine extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> orbitalRings = new ArrayList<>();
        private final List<BlockDisplayHandle> pistonArms = new ArrayList<>();
        private final List<BlockDisplayHandle> exhaust = new ArrayList<>();
        private final List<BlockDisplayHandle> debrisOrbit = new ArrayList<>();

        public DoomApocalypseEngine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_apocalypse_engine", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(35.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(250);
            config.setCooldownTicks(400);
            config.setChance(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone().add(0, 5, 0);
            World w = center.getWorld();
            if (w == null) return;

            Location core = this.center;

            // Core: 6 blocks clustered
            double[][] coreOffsets = {{0, 0, 0}, {0.4, 0.2, 0}, {-0.4, 0.2, 0}, {0, 0, 0.4}, {0, 0, -0.4}, {0, 0.4, 0}};
            for (double[] off : coreOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(core.clone().add(off[0], off[1], off[2]), Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(120, 20, 80).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                coreBlocks.add(h);
            }

            // 3 orbital rings, 8 blocks each, on different axis orientations
            for (int ring = 0; ring < 3; ring++) {
                List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    double radius = 3.0;
                    double x, y, z;
                    if (ring == 0) { // XZ plane
                        x = Math.cos(angle) * radius;
                        y = 0;
                        z = Math.sin(angle) * radius;
                    } else if (ring == 1) { // XY plane
                        x = Math.cos(angle) * radius;
                        y = Math.sin(angle) * radius;
                        z = 0;
                    } else { // YZ plane
                        x = 0;
                        y = Math.cos(angle) * radius;
                        z = Math.sin(angle) * radius;
                    }
                    BlockDisplayHandle h = displayBuilder.spawnBlock(core.clone().add(x, y, z), Material.MAGMA_BLOCK);
                    h.scale(0.35f, 0.35f, 0.35f).glow(255, 100, 20).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                    ringBlocks.add(h);
                }
                orbitalRings.add(ringBlocks);
            }

            // 6 piston arms (2 blocks each = 12 blocks)
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                for (int seg = 0; seg < 2; seg++) {
                    double dist = 1.5 + seg * 1.2;
                    Location loc = core.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0.2f, 1.0f, 0.2f).glow(180, 180, 200).interpolation(8, 0);
                    spawnedEntities.add(h.entity());
                    pistonArms.add(h);
                }
            }

            // 4 exhaust flame blocks
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4 + Math.PI / 4;
                Location loc = core.clone().add(Math.cos(angle) * 2.0, -2.0, Math.sin(angle) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                h.scale(0.3f, 0.5f, 0.3f).glow(240, 80, 30).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                exhaust.add(h);
            }

            // 6 debris orbit
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                Location loc = core.clone().add(Math.cos(angle) * 4.0, Math.sin(angle * 0.5) * 1.5, Math.sin(angle) * 4.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(0.2f, 0.2f, 0.2f).glow(200, 50, 10).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                debrisOrbit.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Core pulsing scale
            float corePulse = 0.5f + (float) Math.sin(tick * 0.15) * 0.15f;
            if (tick % 5 == 0) {
                for (BlockDisplayHandle c : coreBlocks) {
                    Transformation t = c.entity().getTransformation();
                    c.entity().setInterpolationDuration(5);
                    c.entity().setInterpolationDelay(0);
                    c.entity().setTransformation(new Transformation(
                            t.getTranslation(), new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(corePulse, corePulse, corePulse),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Orbital rings rotation via Transform translation
            float[] ringSpeeds = {0.06f, 0.08f, 0.1f};
            for (int ring = 0; ring < 3; ring++) {
                List<BlockDisplayHandle> ringBlocks = orbitalRings.get(ring);
                float speed = ringSpeeds[ring];
                for (int i = 0; i < ringBlocks.size(); i++) {
                    double baseAngle = (2 * Math.PI * i) / 8;
                    double currentAngle = baseAngle + tick * speed;
                    double radius = 3.0;
                    float x, y, z;

                    if (ring == 0) {
                        x = (float) (Math.cos(currentAngle) * radius);
                        y = 0;
                        z = (float) (Math.sin(currentAngle) * radius);
                    } else if (ring == 1) {
                        x = (float) (Math.cos(currentAngle) * radius);
                        y = (float) (Math.sin(currentAngle) * radius);
                        z = 0;
                    } else {
                        x = 0;
                        y = (float) (Math.cos(currentAngle) * radius);
                        z = (float) (Math.sin(currentAngle) * radius);
                    }

                    BlockDisplayHandle h = ringBlocks.get(i);
                    Transformation t = h.entity().getTransformation();
                    h.entity().setInterpolationDuration(5);
                    h.entity().setInterpolationDelay(0);
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(x, y, z),
                            new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Piston arms extend/retract via Transform
            if (tick % 10 == 0) {
                float extension = (float) Math.sin(tick * 0.1) * 0.8f;
                for (int i = 0; i < pistonArms.size(); i++) {
                    int armIdx = i / 2;
                    int segIdx = i % 2;
                    double angle = (2 * Math.PI * armIdx) / 6;
                    float baseDist = 1.5f + segIdx * 1.2f;
                    float extendedDist = baseDist + extension * (segIdx + 1);

                    BlockDisplayHandle arm = pistonArms.get(i);
                    Transformation t = arm.entity().getTransformation();
                    arm.entity().setInterpolationDuration(8);
                    arm.entity().setInterpolationDelay(0);
                    arm.entity().setTransformation(new Transformation(
                            new Vector3f((float) (Math.cos(angle) * extendedDist), 0, (float) (Math.sin(angle) * extendedDist)),
                            new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Debris orbit
            for (int i = 0; i < debrisOrbit.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 6;
                double currentAngle = baseAngle + tick * 0.05;
                float dx = (float) (Math.cos(currentAngle) * 4.0);
                float dz = (float) (Math.sin(currentAngle) * 4.0);
                float dy = (float) (Math.sin(currentAngle * 2) * 1.5);

                BlockDisplayHandle d = debrisOrbit.get(i);
                Transformation t = d.entity().getTransformation();
                d.entity().setInterpolationDuration(5);
                d.entity().setInterpolationDelay(0);
                d.entity().setTransformation(new Transformation(
                        new Vector3f(dx, dy, dz),
                        new AxisAngle4f().set(t.getLeftRotation()), t.getScale(), new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            // Exhaust particles
            if (tick % 4 == 0) {
                for (BlockDisplayHandle ex : exhaust) {
                    Location eLoc = ex.entity().getLocation();
                    w.spawnParticle(Particle.FLAME, eLoc, 3, 0.1, 0.3, 0.1, 0.02);
                    w.spawnParticle(Particle.SMOKE, eLoc, 2, 0.15, 0.2, 0.15, 0.01);
                }
            }

            // Engine hum
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 0.3f);
            }

            // Core particles
            if (tick % 3 == 0) {
                w.spawnParticle(Particle.PORTAL, center, 8, 0.3, 0.3, 0.3, 0.5);
                DisplayBuilder.dustParticles(center, 3, 0.4, 120, 20, 80, 1.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DoomApocalypseEngine(plugin); }
    }
}
