package com.blockforge.chaoscraft.modes.fluffy.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluffy Mode — BLOCK DISPLAY ATTACKS BANK 4 (Flora & Crystal)
 * Attacks 31–40. 10 flora-and-crystal-themed BlockDisplay attacks with 25+
 * block displays each. All animations via Transformation interpolation
 * (AxisAngle4f rotation, Vector3f scale/translation) — never teleport for
 * rotation.
 *
 * Fluffy palette (flora):
 *  - Daisy white / soft yellow
 *  - Floral pink / cherry blossom pink
 *  - Crystal violet / amethyst
 *  - Lily pad green / mossy stone gray
 *
 * Materials: WHITE_CONCRETE, YELLOW_CONCRETE, GREEN_CONCRETE, LIME_CONCRETE,
 *            PINK_CONCRETE, RED_CONCRETE, GOLD_BLOCK, QUARTZ, AMETHYST_BLOCK,
 *            CALCITE, BROWN_MUSHROOM_BLOCK, RED_MUSHROOM_BLOCK, BAMBOO_BLOCK,
 *            MOSSY_STONE_BRICKS, BROWN_CONCRETE, CHERRY_LEAVES, CHERRY_LOG,
 *            TERRACOTTA variants
 *
 * Particles: CHERRY_LEAVES, ENCHANT, CRIT, DUST (color-matched), MYCELIUM,
 *            SPORE_BLOSSOM_AIR, BUBBLE, FALLING_DUST
 * Sounds: BLOCK_GRASS_PLACE, BLOCK_AMETHYST_BLOCK_CHIME,
 *         BLOCK_AMETHYST_CLUSTER_BREAK, BLOCK_MUSHROOM_STEW_STEP,
 *         BLOCK_BAMBOO_BREAK, BLOCK_CHERRY_LEAVES_BREAK,
 *         ENTITY_FIREWORK_ROCKET_BLAST, BLOCK_WATER_AMBIENT,
 *         ENTITY_FISH_SWIM, BLOCK_VINE_PLACE
 *
 * Attacks:
 * 31. GiantDaisy             — 42 blocks, daisy with leaning bloom + stem
 * 32. FloralCrownDescend     — 68 blocks, descending royal crown w/ gem tips
 * 33. CrystalFlowerBurst     — 98 blocks, hex of crystal blooms, snap close
 * 34. MushroomRingEruption   — 96 blocks, ring of 8 mushrooms erupting
 * 35. BambooSpikePrison      — 64 blocks, octagon bamboo cage with sway
 * 36. GiantSunflower         — 47 blocks, tracking sunflower w/ seed bursts
 * 37. CherryBlossomShower    — 35 blocks, hovering tree w/ blossom bombs
 * 38. LilyPadMaze            — 74 blocks, 7 lily pads + lotus flowers
 * 39. TulipTidalWave         — 72 blocks, line of blooming tulips, rolling
 * 40. IvyWallCrawl           — 44 blocks, mossy wall w/ creeping ivy
 */
public final class FluffyBlockDisplay4 {
    private FluffyBlockDisplay4() {}

    private static final String MODE_PATH = "modes/fluffy/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GiantDaisy(plugin));
        registry.register(new FloralCrownDescend(plugin));
        registry.register(new CrystalFlowerBurst(plugin));
        registry.register(new MushroomRingEruption(plugin));
        registry.register(new BambooSpikePrison(plugin));
        registry.register(new GiantSunflower(plugin));
        registry.register(new CherryBlossomShower(plugin));
        registry.register(new LilyPadMaze(plugin));
        registry.register(new TulipTidalWave(plugin));
        registry.register(new IvyWallCrawl(plugin));
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
    // #31 — GIANT DAISY
    // 42 blocks: 7 YELLOW_CONCRETE disc (hex+center), 24 WHITE_CONCRETE
    // petals (8 petals × 3 segments each, scaled 0.7×0.3×1.8), 6 GREEN
    // leaf blocks (2 leaves × 3 segments), 5 LIME stem.
    // Stem rises, petals open from closed. Daisy leans toward player.
    // 2.5r constant in disc, 20dmg, 12t interval, 20t delay.
    // ================================================================
    public static class GiantDaisy extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> disc = new ArrayList<>();
        private final List<BlockDisplayHandle> petals = new ArrayList<>();
        private final List<BlockDisplayHandle> leaves = new ArrayList<>();
        private final List<BlockDisplayHandle> stem = new ArrayList<>();
        private float leanAngle = 0f;

        public GiantDaisy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_daisy", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(220);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Stem: 5 LIME_CONCRETE blocks rising
            for (int y = 0; y < 5; y++) {
                Location loc = center.clone().add(0, y * 1.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIME_CONCRETE);
                h.scale(0.4f, 1.0f, 0.4f).glow(80, 200, 80).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                stem.add(h);
            }

            // Disc: 1 center + 6 hex YELLOW_CONCRETE at top of stem (y=5.0)
            disc.add(spawnDiscBlock(center.clone().add(0, 5.0, 0)));
            double discRad = 0.6;
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * i) / 6.0;
                disc.add(spawnDiscBlock(center.clone().add(Math.cos(a) * discRad, 5.0, Math.sin(a) * discRad)));
            }

            // 8 petals × 3 segments = 24 WHITE_CONCRETE petal blocks at radius 1.4-2.4
            for (int p = 0; p < 8; p++) {
                double pAngle = (2.0 * Math.PI * p) / 8.0;
                for (int seg = 0; seg < 3; seg++) {
                    double r = 1.4 + seg * 0.5;
                    double px = Math.cos(pAngle) * r;
                    double pz = Math.sin(pAngle) * r;
                    Location loc = center.clone().add(px, 5.0, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                    h.scale(0.0f, 0.3f, 0.0f); // start closed (will open)
                    h.rotate((float) pAngle, 0, 1, 0);
                    h.glow(255, 240, 220).interpolation(15, p);
                    spawnedEntities.add(h.entity());
                    petals.add(h);
                }
            }

            // 2 leaves × 3 segments = 6 GREEN_CONCRETE blocks angled off stem
            double[] leafAngles = {Math.PI * 0.25, Math.PI * 1.25};
            double[] leafYs = {1.5, 2.5};
            for (int l = 0; l < 2; l++) {
                for (int seg = 0; seg < 3; seg++) {
                    double r = 0.6 + seg * 0.5;
                    double px = Math.cos(leafAngles[l]) * r;
                    double pz = Math.sin(leafAngles[l]) * r;
                    Location loc = center.clone().add(px, leafYs[l], pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GREEN_CONCRETE);
                    h.scale(0.4f, 0.2f, 0.5f);
                    h.rotate((float) leafAngles[l], 0, 1, 0);
                    h.glow(60, 180, 60).interpolation(8, 0);
                    spawnedEntities.add(h.entity());
                    leaves.add(h);
                }
            }

            // Open petals via interpolation animation (scale 0 → 0.7×0.3×1.8 staggered)
            for (int i = 0; i < petals.size(); i++) {
                BlockDisplay e = petals.get(i).entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(15);
                e.setInterpolationDelay(i);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(0.7f, 0.3f, 1.8f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRASS_PLACE, 1.0f, 1.4f);
            w.spawnParticle(Particle.CHERRY_LEAVES, center.clone().add(0, 5.0, 0), 30, 2, 0.3, 2, 0.01);
        }

        private BlockDisplayHandle spawnDiscBlock(Location loc) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.YELLOW_CONCRETE);
            h.scale(0.6f, 0.3f, 0.6f).glow(255, 230, 80).interpolation(5, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Lean toward player (Z-axis tilt 0 → 20° over 60 ticks)
            if (tick == 60) {
                Player target = findNearestPlayer(c, 12);
                if (target != null) {
                    double dx = target.getLocation().getX() - c.getX();
                    double dz = target.getLocation().getZ() - c.getZ();
                    double leanDir = Math.atan2(dz, dx);
                    leanAngle = (float) Math.toRadians(20);
                    // Apply lean to disc + petals via Z rotation around stem top
                    float axisX = (float) -Math.sin(leanDir);
                    float axisZ = (float) Math.cos(leanDir);
                    for (BlockDisplayHandle h : disc) applyLean(h, leanAngle, axisX, axisZ);
                    for (BlockDisplayHandle h : petals) applyLean(h, leanAngle, axisX, axisZ);
                }
            }

            // Petal drift / sparkle particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.CHERRY_LEAVES,
                        c.clone().add(0, 5.0, 0), 4, 2.5, 0.3, 2.5, 0.01);
            }
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT,
                        c.clone().add(0, 5.2, 0), 6, 0.8, 0.2, 0.8, 0.5);
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRASS_PLACE, 0.6f, 1.5f);
            }
        }

        private void applyLean(BlockDisplayHandle h, float angle, float axisX, float axisZ) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(60);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, axisX, 0, axisZ),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantDaisy(plugin); }
    }

    // ================================================================
    // #32 — FLORAL CROWN DESCEND
    // 68 blocks: 12 GOLD_BLOCK base ring, 24 QUARTZ spire blocks (8 spires
    // × 3 tapering), 24 PINK_CONCRETE fleur ornaments, 8 AMETHYST gem tips.
    // Descends from Y+14, rotates Y, hovers at Y+2 with spire pulse.
    // 4.0r constant under crown, 24dmg, 10t interval, 10t delay.
    // ================================================================
    public static class FloralCrownDescend extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spires = new ArrayList<>();
        private final List<BlockDisplayHandle> gems = new ArrayList<>();
        private float yRotation = 0f;
        private float spirePulse = 0f;

        public FloralCrownDescend(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floral_crown_descend", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(12.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(360);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double startY = 14.0;
            // Base ring: 12 GOLD_BLOCK at radius 3.0
            for (int i = 0; i < 12; i++) {
                double a = (2.0 * Math.PI * i) / 12.0;
                Location loc = center.clone().add(Math.cos(a) * 3.0, startY, Math.sin(a) * 3.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                h.scale(0.6f, 0.5f, 0.6f).glow(255, 215, 0).interpolation(40, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // 8 spires × 3 tapering blocks (0.5 → 0.35 → 0.2) at radius 3.0
            for (int s = 0; s < 8; s++) {
                double sa = (2.0 * Math.PI * s) / 8.0;
                double sx = Math.cos(sa) * 3.0;
                double sz = Math.sin(sa) * 3.0;
                for (int seg = 0; seg < 3; seg++) {
                    Location loc = center.clone().add(sx, startY + 0.5 + seg * 0.9, sz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ);
                    float taper = 0.5f - seg * 0.15f;
                    h.scale(taper, 0.9f, taper).glow(255, 255, 240).interpolation(40, 0);
                    spawnedEntities.add(h.entity());
                    allBlocks.add(h);
                    spires.add(h);
                }

                // Gem tip on top of each spire
                Location gemLoc = center.clone().add(sx, startY + 3.4, sz);
                BlockDisplayHandle gem = displayBuilder.spawnBlock(gemLoc, Material.AMETHYST_BLOCK);
                gem.scale(0.35f, 0.35f, 0.35f).glow(180, 100, 220).interpolation(40, 0);
                spawnedEntities.add(gem.entity());
                allBlocks.add(gem);
                gems.add(gem);
            }

            // 8 fleur-de-lis ornaments × 3 = 24 PINK_CONCRETE at radius 3.5 between spires
            for (int f = 0; f < 8; f++) {
                double fa = (2.0 * Math.PI * f) / 8.0 + Math.PI / 8.0;
                double fx = Math.cos(fa) * 3.5;
                double fz = Math.sin(fa) * 3.5;
                for (int seg = 0; seg < 3; seg++) {
                    double offY = 0.2 + seg * 0.6;
                    Location loc = center.clone().add(fx, startY + offY, fz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                    h.scale(0.4f, 0.4f, 0.4f).rotate((float) fa, 0, 1, 0);
                    h.glow(255, 180, 220).interpolation(40, 0);
                    spawnedEntities.add(h.entity());
                    allBlocks.add(h);
                }
            }

            // Animate descent: all blocks translate from Y+14 to Y+2 over 60 ticks
            float descendDelta = -12.0f;
            for (BlockDisplayHandle h : allBlocks) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(60);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        new Vector3f(t.getTranslation().x, t.getTranslation().y + descendDelta, t.getTranslation().z),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.8f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 8, 0), 40, 3, 3, 3, 1.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Continuous Y rotation of all blocks
            yRotation += 0.025f;
            if (tick % 3 == 0) {
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(3);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(yRotation, 0, 1, 0),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Spire pulse (1.0 → 1.1 cycle)
            spirePulse += 0.1f;
            if (tick % 4 == 0) {
                float pulse = 1.0f + 0.1f * (float) Math.sin(spirePulse);
                for (int i = 0; i < spires.size(); i++) {
                    BlockDisplay e = spires.get(i).entity();
                    Transformation t = e.getTransformation();
                    int seg = i % 3;
                    float taper = 0.5f - seg * 0.15f;
                    e.setInterpolationDuration(4);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(taper * pulse, 0.9f, taper * pulse),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 3, 0), 6, 2, 1, 2, 1.0);
            }
            if (tick % 6 == 0) {
                for (BlockDisplayHandle gem : gems) {
                    DisplayBuilder.dustParticles(gem.entity().getLocation(), 2, 0.3, 180, 100, 220, 1.0f);
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 2.0, 0), 8, 3.0, 255, 215, 0, 1.0f);
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FloralCrownDescend(plugin); }
    }

    // ================================================================
    // #33 — CRYSTAL FLOWER BURST
    // 98 blocks: 7 flowers in hex+center — each = 2 AMETHYST center + 12
    // CALCITE petals (6 angles × 2). Buds bloom slowly, petals snap closed
    // at tick 80 for impact damage.
    // Impact-only: 5.0r, 44 impact damage at snap.
    // ================================================================
    public static class CrystalFlowerBurst extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> flowers = new ArrayList<>();
        private boolean snapped = false;

        public CrystalFlowerBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_flower_burst", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.0);
            config.setImpactRadius(5.0);
            config.setDamage(0);
            config.setDurationTicks(160);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 7 flower positions: center + 6 hex at radius 3.0
            double[][] positions = new double[7][2];
            positions[0] = new double[]{0, 0};
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * i) / 6.0;
                positions[i + 1] = new double[]{Math.cos(a) * 3.0, Math.sin(a) * 3.0};
            }

            for (int f = 0; f < 7; f++) {
                List<BlockDisplayHandle> flower = new ArrayList<>();
                Location flowerLoc = center.clone().add(positions[f][0], 0.5, positions[f][1]);

                // 2 AMETHYST center blocks
                for (int s = 0; s < 2; s++) {
                    Location loc = flowerLoc.clone().add(0, s * 0.4, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                    h.scale(0.4f, 0.4f, 0.4f).glow(180, 100, 220).interpolation(20, 0);
                    spawnedEntities.add(h.entity());
                    flower.add(h);
                }

                // 6 petals × 2 segments at 60° each = 12 CALCITE
                for (int p = 0; p < 6; p++) {
                    double pAngle = (2.0 * Math.PI * p) / 6.0;
                    for (int seg = 0; seg < 2; seg++) {
                        double r = 0.5 + seg * 0.4;
                        double px = Math.cos(pAngle) * r;
                        double pz = Math.sin(pAngle) * r;
                        Location loc = flowerLoc.clone().add(px, 0.4, pz);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                        h.scale(0.0f, 0.5f, 0.1f); // start closed (folded)
                        h.rotate((float) pAngle, 0, 1, 0);
                        h.glow(220, 220, 240).interpolation(40, f * 2);
                        spawnedEntities.add(h.entity());
                        flower.add(h);
                    }
                }
                flowers.add(flower);
            }

            // Bloom animation: petals X-scale 0 → 0.5
            for (int f = 0; f < flowers.size(); f++) {
                List<BlockDisplayHandle> fl = flowers.get(f);
                for (int i = 2; i < fl.size(); i++) { // skip 2 amethyst centers
                    BlockDisplay e = fl.get(i).entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(40);
                    e.setInterpolationDelay(f * 2);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.5f, 0.5f, 0.1f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 1, 0), 50, 3, 1, 3, 1.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Z-axis slow rotate during bloom (ticks 0-79)
            if (tick < 80 && tick % 5 == 0) {
                float rot = tick * 0.02f;
                for (List<BlockDisplayHandle> flower : flowers) {
                    for (int i = 2; i < flower.size(); i++) {
                        BlockDisplay e = flower.get(i).entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(5);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(rot, 0, 0, 1),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Bloom particles
            if (tick < 80 && tick % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 6, 3.0, 180, 100, 220, 1.2f);
            }

            // SNAP at tick 80 → impact damage on each flower
            if (tick == 80 && !snapped) {
                snapped = true;
                // Petals X-scale 0.5 → 0 over 2 ticks
                for (List<BlockDisplayHandle> flower : flowers) {
                    for (int i = 2; i < flower.size(); i++) {
                        BlockDisplay e = flower.get(i).entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(0.0f, 0.5f, 0.1f),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
                // Trigger impact damage at center (radius 5 covers all 7 flowers nicely)
                triggerImpactDamage(c);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.2f, 0.8f);
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 1, 0), 60, 3, 1, 3, 0.5);
            }

            // Post-snap shatter: shrink remaining centers
            if (tick == 100) {
                for (List<BlockDisplayHandle> flower : flowers) {
                    for (BlockDisplayHandle h : flower) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(20);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(0, 0, 0),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalFlowerBurst(plugin); }
    }

    // ================================================================
    // #34 — MUSHROOM RING ERUPTION
    // 96 blocks: 8 mushrooms in ring at r=4.0. Each: 3 BROWN stem + 9 RED
    // cap (1 + 4 cardinal + 4 diagonal). Sequential 3-tick stagger rise.
    // Caps pulse. 3.5r constant in center, 20dmg, 12t interval, 30t delay.
    // ================================================================
    public static class MushroomRingEruption extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> mushrooms = new ArrayList<>();
        private float capPulse = 0f;

        public MushroomRingEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mushroom_ring_eruption", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(360);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 8 mushrooms in ring at radius 4.0
            for (int m = 0; m < 8; m++) {
                double a = (2.0 * Math.PI * m) / 8.0;
                double mx = Math.cos(a) * 4.0;
                double mz = Math.sin(a) * 4.0;
                List<BlockDisplayHandle> mushroom = new ArrayList<>();

                // Stem: 3 BROWN_MUSHROOM_BLOCK, start at Y=-0.5
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(mx, -0.5 + s * 0.8, mz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BROWN_MUSHROOM_BLOCK);
                    h.scale(0.8f, 1.0f, 0.8f).glow(170, 130, 90).interpolation(20, m * 3);
                    spawnedEntities.add(h.entity());
                    mushroom.add(h);
                }

                // Cap: 1 center + 4 cardinal + 4 diagonal RED_MUSHROOM at top
                double[][] capOffsets = {
                        {0, 0}, {0.6, 0}, {-0.6, 0}, {0, 0.6}, {0, -0.6},
                        {0.45, 0.45}, {-0.45, 0.45}, {0.45, -0.45}, {-0.45, -0.45}
                };
                for (double[] off : capOffsets) {
                    Location loc = center.clone().add(mx + off[0], 1.8, mz + off[1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_MUSHROOM_BLOCK);
                    h.scale(0.7f, 0.5f, 0.7f).glow(220, 60, 60).interpolation(20, m * 3);
                    spawnedEntities.add(h.entity());
                    mushroom.add(h);
                }
                mushrooms.add(mushroom);
            }

            // Animate eruption: each mushroom translates Y up by 3.0 with stagger
            for (int m = 0; m < mushrooms.size(); m++) {
                List<BlockDisplayHandle> mush = mushrooms.get(m);
                for (BlockDisplayHandle h : mush) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(20);
                    e.setInterpolationDelay(m * 3);
                    e.setTransformation(new Transformation(
                            new Vector3f(t.getTranslation().x, t.getTranslation().y + 3.0f, t.getTranslation().z),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                // Initial sound on first mushroom; staggered sounds in onTick
                if (m == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_FUNGUS_STEP, 1.0f, 0.8f);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Cap pulse (±0.05 scale)
            capPulse += 0.08f;
            if (tick > 30 && tick % 5 == 0) {
                float pulse = 0.7f + 0.05f * (float) Math.sin(capPulse);
                for (List<BlockDisplayHandle> mushroom : mushrooms) {
                    for (int i = 3; i < mushroom.size(); i++) { // caps only (skip 3 stem)
                        BlockDisplay e = mushroom.get(i).entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(5);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(pulse, 0.5f, pulse),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Stagger sound on rise
            if (tick > 0 && tick <= 24 && tick % 3 == 0) {
                int idx = tick / 3;
                if (idx < 8) {
                    double a = (2.0 * Math.PI * idx) / 8.0;
                    Location mLoc = c.clone().add(Math.cos(a) * 4.0, 1.0, Math.sin(a) * 4.0);
                    DisplayBuilder.playSound(mLoc, Sound.BLOCK_FUNGUS_STEP, 0.8f, 0.7f + idx * 0.05f);
                }
            }

            // Particles: MYCELIUM + SPORE_BLOSSOM_AIR from caps
            if (tick % 6 == 0) {
                for (int m = 0; m < 8; m++) {
                    double a = (2.0 * Math.PI * m) / 8.0;
                    Location capLoc = c.clone().add(Math.cos(a) * 4.0, 2.5, Math.sin(a) * 4.0);
                    c.getWorld().spawnParticle(Particle.MYCELIUM, capLoc, 4, 0.6, 0.3, 0.6, 0.01);
                    c.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, capLoc, 2, 0.5, 0.3, 0.5, 0.01);
                }
            }
            if (tick % 10 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_DUST,
                        c.clone().add(0, 2.5, 0), 10, 4, 1, 4, 0.05,
                        Material.RED_MUSHROOM_BLOCK.createBlockData());
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MushroomRingEruption(plugin); }
    }

    // ================================================================
    // #35 — BAMBOO SPIKE PRISON
    // 64 blocks: 8 stalks × 6 BAMBOO_BLOCK = 48, 8 MOSSY anchor ring,
    // 8 BAMBOO top canopy ring. Stalks rise rapidly, then top ring slides
    // down. Stalks sway inward ±3° on alternating cycles.
    // 3.0r constant, 22dmg, 10t interval, 15t delay.
    // ================================================================
    public static class BambooSpikePrison extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> stalks = new ArrayList<>();
        private final List<BlockDisplayHandle> anchors = new ArrayList<>();
        private final List<BlockDisplayHandle> canopy = new ArrayList<>();
        private float swayPhase = 0f;

        public BambooSpikePrison(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bamboo_spike_prison", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(11.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(340);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 8 anchors at ground (Y=0) + 8 stalks
            for (int s = 0; s < 8; s++) {
                double a = (2.0 * Math.PI * s) / 8.0;
                double sx = Math.cos(a) * 3.5;
                double sz = Math.sin(a) * 3.5;

                // MOSSY anchor at base
                Location aLoc = center.clone().add(sx, 0.0, sz);
                BlockDisplayHandle anchor = displayBuilder.spawnBlock(aLoc, Material.MOSSY_STONE_BRICKS);
                anchor.scale(0.6f, 0.4f, 0.6f).glow(120, 140, 90).interpolation(10, 0);
                spawnedEntities.add(anchor.entity());
                anchors.add(anchor);

                // 6 BAMBOO stalk segments, start compressed at Y=-1
                List<BlockDisplayHandle> stalk = new ArrayList<>();
                for (int seg = 0; seg < 6; seg++) {
                    Location loc = center.clone().add(sx, -1.0, sz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BAMBOO_BLOCK);
                    h.scale(0.3f, 1.0f, 0.3f).glow(200, 220, 100).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                    stalk.add(h);
                }
                stalks.add(stalk);
            }

            // Animate stalks rise: Y -1 → 5 over 5 ticks
            for (int s = 0; s < stalks.size(); s++) {
                List<BlockDisplayHandle> stalk = stalks.get(s);
                for (int seg = 0; seg < stalk.size(); seg++) {
                    BlockDisplay e = stalk.get(seg).entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(5);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(t.getTranslation().x, t.getTranslation().y + 1.0f + seg * 1.0f, t.getTranslation().z),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // 8 canopy ring blocks initially at Y=8 (will descend)
            for (int c = 0; c < 8; c++) {
                double a = (2.0 * Math.PI * c) / 8.0 + Math.PI / 8.0;
                double cx = Math.cos(a) * 3.5;
                double cz = Math.sin(a) * 3.5;
                Location loc = center.clone().add(cx, 8.0, cz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BAMBOO_BLOCK);
                h.scale(0.4f, 0.3f, 0.6f).rotate((float) a, 0, 1, 0).glow(200, 220, 100).interpolation(15, 5);
                spawnedEntities.add(h.entity());
                canopy.add(h);
            }

            // Canopy slides down to Y=6
            for (BlockDisplayHandle h : canopy) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(15);
                e.setInterpolationDelay(5);
                e.setTransformation(new Transformation(
                        new Vector3f(t.getTranslation().x, t.getTranslation().y - 2.0f, t.getTranslation().z),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BAMBOO_BREAK, 1.2f, 0.6f);
            w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, center.clone().add(0, 2, 0), 30, 2, 2, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sway: stalks tilt ±3° with 20-tick alternating period
            swayPhase += 0.314f; // 2π/20
            if (tick > 10 && tick % 4 == 0) {
                float sway = (float) Math.toRadians(3) * (float) Math.sin(swayPhase);
                for (int s = 0; s < stalks.size(); s++) {
                    double a = (2.0 * Math.PI * s) / 8.0;
                    // Inward sway axis perpendicular to radial direction
                    float axisX = (float) -Math.sin(a);
                    float axisZ = (float) Math.cos(a);
                    // Alternate stalks invert phase
                    float swayDir = (s % 2 == 0) ? sway : -sway;
                    for (BlockDisplayHandle h : stalks.get(s)) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(4);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(swayDir, axisX, 0, axisZ),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Bamboo splinter particles every 5 ticks
            if (tick % 5 == 0) {
                for (int s = 0; s < 8; s++) {
                    double a = (2.0 * Math.PI * s) / 8.0;
                    Location stalkLoc = c.clone().add(Math.cos(a) * 3.5, 2.0, Math.sin(a) * 3.5);
                    c.getWorld().spawnParticle(Particle.BLOCK,
                            stalkLoc, 3, 0.2, 0.5, 0.2, 0.02,
                            Material.BAMBOO_BLOCK.createBlockData());
                }
            }
            if (tick % 10 == 0) {
                c.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                        c.clone().add(0, 2.5, 0), 8, 2, 1.5, 2, 0.02);
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BAMBOO_BREAK, 0.7f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BambooSpikePrison(plugin); }
    }

    // ================================================================
    // #36 — GIANT SUNFLOWER
    // 47 blocks: 8 LIME stem, 6 GREEN leaves (2×3), 9 BROWN center disc
    // (3×3), 24 YELLOW petals (12×2). Stem grows, leaves unfurl, head
    // tracks player Y rotation. Seed bursts at tick 30/60.
    // 2.0r constant disc, 22dmg, 12t interval, 20t delay. Seed-burst
    // impact on player r=2.0, 28dmg.
    // ================================================================
    public static class GiantSunflower extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stem = new ArrayList<>();
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private float headYaw = 0f;
        private boolean burst30 = false;
        private boolean burst60 = false;

        public GiantSunflower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_sunflower", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(11.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(20);
            config.setImpactDamage(14.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 8 LIME_CONCRETE stem, scaled tall
            for (int s = 0; s < 8; s++) {
                Location loc = center.clone().add(0, s * 1.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIME_CONCRETE);
                h.scale(0.4f, 0.0f, 0.4f).glow(80, 200, 80).interpolation(15, 0);
                spawnedEntities.add(h.entity());
                stem.add(h);
            }
            // Animate stem grow Y scale 0 → 1.0
            for (BlockDisplayHandle h : stem) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(15);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(0.4f, 1.0f, 0.4f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            // 2 leaves × 3 = 6 GREEN_CONCRETE
            double[] leafA = {0, Math.PI};
            double[] leafY = {2.5, 4.5};
            for (int l = 0; l < 2; l++) {
                for (int seg = 0; seg < 3; seg++) {
                    double r = 0.6 + seg * 0.5;
                    double px = Math.cos(leafA[l]) * r;
                    double pz = Math.sin(leafA[l]) * r;
                    Location loc = center.clone().add(px, leafY[l], pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GREEN_CONCRETE);
                    h.scale(0.4f, 0.2f, 0.5f).rotate((float) leafA[l], 0, 1, 0);
                    h.glow(60, 180, 60).interpolation(15, 5);
                    spawnedEntities.add(h.entity());
                }
            }

            // 9 BROWN_CONCRETE 3x3 disc at Y=8
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Location loc = center.clone().add(dx * 0.5, 8.0, dz * 0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BROWN_CONCRETE);
                    h.scale(0.5f, 0.4f, 0.5f).glow(120, 80, 30).interpolation(10, 12);
                    spawnedEntities.add(h.entity());
                    head.add(h);
                }
            }

            // 12 petals × 2 = 24 YELLOW_CONCRETE around disc
            for (int p = 0; p < 12; p++) {
                double a = (2.0 * Math.PI * p) / 12.0;
                for (int seg = 0; seg < 2; seg++) {
                    double r = 1.0 + seg * 0.6;
                    double px = Math.cos(a) * r;
                    double pz = Math.sin(a) * r;
                    Location loc = center.clone().add(px, 8.0, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.YELLOW_CONCRETE);
                    h.scale(0.5f, 0.3f, 0.7f).rotate((float) a, 0, 1, 0);
                    h.glow(255, 220, 60).interpolation(10, 12);
                    spawnedEntities.add(h.entity());
                    head.add(h);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRASS_PLACE, 1.0f, 1.2f);
            w.spawnParticle(Particle.CHERRY_LEAVES, center.clone().add(0, 8, 0), 25, 2, 0.5, 2, 0.01);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Head tracks player Y-axis rotation (5-tick interp)
            if (tick > 30 && tick % 5 == 0) {
                Player target = findNearestPlayer(c, 16);
                if (target != null) {
                    double dx = target.getLocation().getX() - c.getX();
                    double dz = target.getLocation().getZ() - c.getZ();
                    headYaw = (float) Math.atan2(dz, dx);
                    for (BlockDisplayHandle h : head) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(5);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(headYaw, 0, 1, 0),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Seed burst at tick 30 and 60 → impact on nearest player
            if (tick == 30 && !burst30) {
                burst30 = true;
                fireSeedBurst(c);
            }
            if (tick == 60 && !burst60) {
                burst60 = true;
                fireSeedBurst(c);
            }

            // Pollen drift particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.CHERRY_LEAVES, c.clone().add(0, 8, 0), 4, 1.5, 0.4, 1.5, 0.01);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRASS_PLACE, 0.6f, 1.5f);
            }
        }

        private void fireSeedBurst(Location c) {
            Player target = findNearestPlayer(c, 20);
            if (target == null) return;
            Location impactLoc = target.getLocation();
            triggerImpactDamage(impactLoc);
            DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 1.6f);
            c.getWorld().spawnParticle(Particle.CRIT, impactLoc.clone().add(0, 1, 0), 30, 1, 1, 1, 0.4);
            c.getWorld().spawnParticle(Particle.BLOCK, impactLoc, 20, 1, 1, 1, 0.2,
                    Material.BROWN_CONCRETE.createBlockData());
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantSunflower(plugin); }
    }

    // ================================================================
    // #37 — CHERRY BLOSSOM SHOWER
    // 35 blocks: 30 CHERRY_LEAVES sphere + 5 CHERRY_LOG branches at Y+4.
    // Bombs of PINK_CONCRETE drop every 25 ticks (5 bombs/cycle).
    // BOTH constant + impact: 1.5r constant base 10dmg/20t. Bomb impact
    // r=1.5, 24dmg per bomb.
    // ================================================================
    public static class CherryBlossomShower extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> sphere = new ArrayList<>();
        private final List<BlockDisplayHandle> branches = new ArrayList<>();

        public CherryBlossomShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cherry_blossom_shower", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(15);
            config.setImpactDamage(12.0);
            config.setImpactRadius(1.5);
            config.setDurationTicks(360);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 5 CHERRY_LOG branch stubs at Y+4 (center, then 4 cardinal stubs)
            double[][] branchOffsets = {{0, 0}, {0.6, 0}, {-0.6, 0}, {0, 0.6}, {0, -0.6}};
            for (double[] off : branchOffsets) {
                Location loc = center.clone().add(off[0], 4.0, off[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHERRY_LOG);
                h.scale(0.4f, 0.4f, 0.4f).glow(150, 100, 80).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                branches.add(h);
            }

            // 30 CHERRY_LEAVES sphere using golden angle, radius 2.0
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 30; i++) {
                double y = 1.0 - (2.0 * i / 29.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * radiusAtY * 2.0;
                double pz = Math.sin(theta) * radiusAtY * 2.0;
                double py = y * 2.0 + 4.0;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHERRY_LEAVES);
                h.scale(0.0f, 0.0f, 0.0f); // start collapsed
                h.glow(255, 200, 220).interpolation(20, i);
                spawnedEntities.add(h.entity());
                sphere.add(h);
            }
            // Animate spiral materialize: scale 0 → 0.7
            for (int i = 0; i < sphere.size(); i++) {
                BlockDisplay e = sphere.get(i).entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(20);
                e.setInterpolationDelay(i);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(0.7f, 0.7f, 0.7f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHERRY_LEAVES_BREAK, 1.0f, 0.9f);
            w.spawnParticle(Particle.CHERRY_LEAVES, center.clone().add(0, 4, 0), 40, 2.5, 1, 2.5, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow rotation of sphere
            if (tick > 30 && tick % 5 == 0) {
                float rot = tick * 0.015f;
                for (BlockDisplayHandle h : sphere) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(5);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(rot, 0, 1, 0),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Bomb cycle every 25 ticks: 5 bombs drop and impact
            if (tick > 60 && tick % 25 == 0) {
                spawnBlossomBombs(c);
            }

            // Constant blossom rain
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.CHERRY_LEAVES,
                        c.clone().add(0, 4, 0), 8, 2.5, 1.5, 2.5, 0.02);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHERRY_LEAVES_BREAK, 0.5f, 0.9f);
            }
        }

        private void spawnBlossomBombs(Location c) {
            for (int b = 0; b < 5; b++) {
                double a = (2.0 * Math.PI * b) / 5.0 + Math.random() * 0.4;
                double rx = Math.cos(a) * 1.6 * Math.random();
                double rz = Math.sin(a) * 1.6 * Math.random();
                Location bombStart = c.clone().add(rx, 4.0, rz);
                Location bombImpact = c.clone().add(rx, 0.5, rz);

                BlockDisplayHandle bomb = displayBuilder.spawnBlock(bombStart, Material.PINK_CONCRETE);
                bomb.scale(0.5f, 0.5f, 0.5f).glow(255, 180, 220).interpolation(5, 0);
                spawnedEntities.add(bomb.entity());

                // Animate fall
                BlockDisplay e = bomb.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(5);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        new Vector3f(t.getTranslation().x, t.getTranslation().y - 3.5f, t.getTranslation().z),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));

                triggerImpactDamage(bombImpact);
                DisplayBuilder.playSound(bombImpact, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.8f);
                c.getWorld().spawnParticle(Particle.CRIT, bombImpact, 15, 0.8, 0.3, 0.8, 0.3);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CherryBlossomShower(plugin); }
    }

    // ================================================================
    // #38 — LILY PAD MAZE
    // 74 blocks: 7 lily pads × 8 LIME = 56, 3 lotus × 6 PINK = 18.
    // Pads rise, gently bob; lotuses rotate. Maze pattern.
    // 1.2r per pad zone (constant), 16dmg/15t. Lotus zones 24dmg.
    // ================================================================
    public static class LilyPadMaze extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> pads = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> lotuses = new ArrayList<>();
        private float bobPhase = 0f;
        private float lotusRot = 0f;

        public LilyPadMaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lily_pad_maze", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(5.0); // covers all pads from center; use multi-pad logic for vibe
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 7 lily pads scattered. Each pad = 8 LIME blocks in oval, flat scaled
            double[][] padPositions = {
                    {0, 0}, {2.5, 1.5}, {-2.5, 1.5}, {1.5, -2.5},
                    {-1.5, -2.5}, {3.0, -1.5}, {-3.0, 0.5}
            };
            int[] lotusIndices = {0, 2, 5}; // 3 random pads get lotus
            for (int p = 0; p < 7; p++) {
                List<BlockDisplayHandle> pad = new ArrayList<>();
                Location padCenter = center.clone().add(padPositions[p][0], -0.2, padPositions[p][1]);
                // 8 LIME blocks in oval arrangement
                for (int i = 0; i < 8; i++) {
                    double a = (2.0 * Math.PI * i) / 8.0;
                    double px = Math.cos(a) * 1.0;
                    double pz = Math.sin(a) * 0.8;
                    Location loc = padCenter.clone().add(px, 0, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIME_CONCRETE);
                    h.scale(0.5f, 0.2f, 0.5f).glow(80, 200, 80).interpolation(15, 0);
                    spawnedEntities.add(h.entity());
                    pad.add(h);
                }
                // Animate pad rise: Y -0.2 → 0.0
                for (BlockDisplayHandle h : pad) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(15);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(t.getTranslation().x, t.getTranslation().y + 0.2f, t.getTranslation().z),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                pads.add(pad);

                // 3 lotuses on selected pads
                boolean hasLotus = false;
                for (int li : lotusIndices) {
                    if (li == p) hasLotus = true;
                }
                if (hasLotus) {
                    List<BlockDisplayHandle> lotus = new ArrayList<>();
                    for (int li = 0; li < 6; li++) {
                        double a = (2.0 * Math.PI * li) / 6.0;
                        double px = Math.cos(a) * 0.3;
                        double pz = Math.sin(a) * 0.3;
                        Location loc = padCenter.clone().add(px, 0.4, pz);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                        h.scale(0.35f, 0.35f, 0.35f).rotate((float) a, 0, 1, 0);
                        h.glow(255, 180, 220).interpolation(15, 5);
                        spawnedEntities.add(h.entity());
                        lotus.add(h);
                    }
                    lotuses.add(lotus);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 1.2f, 0.8f);
            w.spawnParticle(Particle.BUBBLE, center.clone().add(0, 0.2, 0), 30, 4, 0.2, 4, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Bob: pads gently move Y 0.0 → 0.1 → 0.0 cycle (20-tick period)
            bobPhase += 0.314f; // 2π/20
            if (tick > 20 && tick % 4 == 0) {
                float bob = 0.05f * (float) Math.sin(bobPhase);
                for (int pi = 0; pi < pads.size(); pi++) {
                    float padBob = bob + 0.02f * (float) Math.sin(bobPhase + pi);
                    for (BlockDisplayHandle h : pads.get(pi)) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(4);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(t.getTranslation().x,
                                        -0.5f + padBob,
                                        t.getTranslation().z),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Lotuses rotate Y
            lotusRot += 0.03f;
            if (tick > 30 && tick % 4 == 0) {
                for (List<BlockDisplayHandle> lotus : lotuses) {
                    for (BlockDisplayHandle h : lotus) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(4);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(lotusRot, 0, 1, 0),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Bonus damage pulses for players standing on lotus pads (0,2,5)
            if (tick > 20 && tick % 15 == 0) {
                int[] lotusPadIdx = {0, 2, 5};
                for (int pi : lotusPadIdx) {
                    Location padLoc = c.clone().add(
                            (pi == 0) ? 0 : (pi == 2) ? -2.5 : 3.0,
                            0.2,
                            (pi == 0) ? 0 : (pi == 2) ? 1.5 : -1.5);
                    for (Player p : c.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        if (p.getLocation().distanceSquared(padLoc) <= 1.2 * 1.2) {
                            p.damage(24.0); // lotus zone bonus
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // Particles
            if (tick % 5 == 0) {
                for (int pi = 0; pi < 7; pi++) {
                    Location padLoc = c.clone().add(
                            (pi == 0) ? 0 : (pi == 1) ? 2.5 : (pi == 2) ? -2.5 :
                            (pi == 3) ? 1.5 : (pi == 4) ? -1.5 : (pi == 5) ? 3.0 : -3.0,
                            0.2,
                            (pi == 0) ? 0 : (pi == 1) ? 1.5 : (pi == 2) ? 1.5 :
                            (pi == 3) ? -2.5 : (pi == 4) ? -2.5 : (pi == 5) ? -1.5 : 0.5);
                    c.getWorld().spawnParticle(Particle.BUBBLE, padLoc, 3, 1, 0.1, 1, 0.02);
                }
            }
            if (tick % 8 == 0) {
                for (List<BlockDisplayHandle> lotus : lotuses) {
                    if (!lotus.isEmpty()) {
                        c.getWorld().spawnParticle(Particle.ENCHANT,
                                lotus.get(0).entity().getLocation(), 5, 0.4, 0.3, 0.4, 1.0);
                    }
                }
            }

            if (tick % 80 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_WATER_AMBIENT, 0.6f, 0.7f);
            }
            if (tick % 100 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_FISH_SWIM, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LilyPadMaze(plugin); }
    }

    // ================================================================
    // #39 — TULIP TIDAL WAVE
    // 72 blocks: 12 tulips × 6 = 72. Each: 2 LIME stem + 4 TERRACOTTA cup
    // (alternating RED/PINK/YELLOW/PURPLE). Wave blooms from one end.
    // 1.0r per tulip zone, 18dmg, 12t interval.
    // ================================================================
    public static class TulipTidalWave extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> tulips = new ArrayList<>();
        private final Material[] tulipColors = {
                Material.RED_TERRACOTTA, Material.PINK_CONCRETE,
                Material.YELLOW_TERRACOTTA, Material.PURPLE_TERRACOTTA
        };

        public TulipTidalWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tulip_tidal_wave", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(6.0); // wide line coverage
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(240);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 12 tulips in a line along X, spaced 1.0 apart
            for (int t = 0; t < 12; t++) {
                List<BlockDisplayHandle> tulip = new ArrayList<>();
                double tx = -5.5 + t * 1.0;
                Material color = tulipColors[t % 4];

                // 2 LIME stem segments
                for (int s = 0; s < 2; s++) {
                    Location loc = center.clone().add(tx, -0.2 + s * 0.8, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIME_CONCRETE);
                    h.scale(0.3f, 1.0f, 0.3f).glow(80, 200, 80).interpolation(15, 0);
                    spawnedEntities.add(h.entity());
                    tulip.add(h);
                }

                // 4 cup blocks (closed bud initially)
                double[][] cupOff = {{0, 0}, {0.25, 0}, {-0.25, 0}, {0, 0}};
                double[] cupY = {1.4, 1.55, 1.55, 1.7};
                for (int c = 0; c < 4; c++) {
                    Location loc = center.clone().add(tx + cupOff[c][0], cupY[c], cupOff[c][1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, color);
                    h.scale(0.3f, 0.3f, 0.3f); // closed bud
                    h.glow(220, 150, 200).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    tulip.add(h);
                }
                tulips.add(tulip);

                // Stems rise (Y translate +0.2)
                for (int s = 0; s < 2; s++) {
                    BlockDisplay e = tulip.get(s).entity();
                    Transformation tt = e.getTransformation();
                    e.setInterpolationDuration(15);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(tt.getTranslation().x, tt.getTranslation().y + 0.2f, tt.getTranslation().z),
                            new AxisAngle4f().set(tt.getLeftRotation()),
                            tt.getScale(),
                            new AxisAngle4f().set(tt.getRightRotation())
                    ));
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRASS_PLACE, 1.0f, 1.3f);
            w.spawnParticle(Particle.CHERRY_LEAVES, center.clone().add(0, 1, 0), 30, 6, 0.5, 1, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Bloom wave: each tulip blooms in 3-tick stagger from index 0 → 11
            // After full cycle (12*3=36 + cooldown), repeat
            int waveCycle = 60;
            int phase = tick % waveCycle;
            if (phase < 36 && phase % 3 == 0) {
                int idx = phase / 3;
                if (idx < tulips.size()) {
                    bloomTulip(tulips.get(idx), idx);
                }
            }
            // Reset to bud state at wave restart
            if (phase == 50) {
                for (List<BlockDisplayHandle> tulip : tulips) {
                    for (int i = 2; i < tulip.size(); i++) {
                        BlockDisplay e = tulip.get(i).entity();
                        Transformation tt = e.getTransformation();
                        e.setInterpolationDuration(8);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                tt.getTranslation(),
                                new AxisAngle4f().set(tt.getLeftRotation()),
                                new Vector3f(0.3f, 0.3f, 0.3f),
                                new AxisAngle4f().set(tt.getRightRotation())
                        ));
                    }
                }
            }

            // Particles per tulip (color-matched dust)
            if (tick % 6 == 0) {
                for (int t = 0; t < tulips.size(); t++) {
                    int colorIdx = t % 4;
                    int[] rgb = colorRGB(colorIdx);
                    Location tLoc = c.clone().add(-5.5 + t * 1.0, 1.5, 0);
                    DisplayBuilder.dustParticles(tLoc, 2, 0.4, rgb[0], rgb[1], rgb[2], 1.0f);
                    if (tick % 12 == 0) {
                        c.getWorld().spawnParticle(Particle.CHERRY_LEAVES, tLoc, 1, 0.3, 0.2, 0.3, 0.01);
                    }
                }
            }
        }

        private void bloomTulip(List<BlockDisplayHandle> tulip, int idx) {
            // Open cup: scale 0.3 → 0.55, slight Y stretch
            for (int i = 2; i < tulip.size(); i++) {
                BlockDisplay e = tulip.get(i).entity();
                Transformation tt = e.getTransformation();
                e.setInterpolationDuration(3);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        tt.getTranslation(),
                        new AxisAngle4f().set(tt.getLeftRotation()),
                        new Vector3f(0.55f, 0.6f, 0.55f),
                        new AxisAngle4f().set(tt.getRightRotation())
                ));
            }
            Location c = getCenter();
            if (c != null) {
                Location tLoc = c.clone().add(-5.5 + idx * 1.0, 1.5, 0);
                DisplayBuilder.playSound(tLoc, Sound.BLOCK_GRASS_PLACE, 0.7f, 1.4f + idx * 0.03f);
            }
        }

        private int[] colorRGB(int idx) {
            switch (idx) {
                case 0: return new int[]{220, 30, 30};   // RED
                case 1: return new int[]{255, 180, 220}; // PINK
                case 2: return new int[]{255, 220, 60};  // YELLOW
                default: return new int[]{160, 60, 200}; // PURPLE
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new TulipTidalWave(plugin); }
    }

    // ================================================================
    // #40 — IVY WALL CRAWL
    // 44 blocks: 20 MOSSY back wall (4×5), 20 thin GREEN ivy tendrils,
    // 4 RED rose accents. Wall rises, tendrils extend toward player.
    // 3.0r constant (wall + tendril reach), 20dmg, 10t interval, 15t delay.
    // ================================================================
    public static class IvyWallCrawl extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wall = new ArrayList<>();
        private final List<BlockDisplayHandle> tendrils = new ArrayList<>();
        private final List<BlockDisplayHandle> roses = new ArrayList<>();
        private float crawlOffset = 0f;
        private float wallYaw = 0f;

        public IvyWallCrawl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ivy_wall_crawl", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(360);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Determine wall facing direction toward nearest player
            Player target = findNearestPlayer(center, 16);
            double facingAngle = 0;
            if (target != null) {
                facingAngle = Math.atan2(target.getLocation().getZ() - center.getZ(),
                        target.getLocation().getX() - center.getX()) + Math.PI; // wall behind, ivy faces player
            }
            wallYaw = (float) facingAngle;
            // Wall offset: place wall on opposite side of player
            double wallOffsetX = -Math.cos(facingAngle - Math.PI) * 3.5;
            double wallOffsetZ = -Math.sin(facingAngle - Math.PI) * 3.5;

            // 4×5 MOSSY back wall (20 blocks). Aligned along axis perpendicular to facing
            float perpAngle = (float) (facingAngle + Math.PI / 2);
            float pX = (float) Math.cos(perpAngle);
            float pZ = (float) Math.sin(perpAngle);
            for (int wx = -2; wx < 2; wx++) {
                for (int wy = 0; wy < 5; wy++) {
                    double bx = wallOffsetX + pX * wx * 1.0;
                    double bz = wallOffsetZ + pZ * wx * 1.0;
                    Location loc = center.clone().add(bx, -1.0 + wy * 1.0, bz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MOSSY_STONE_BRICKS);
                    h.scale(1.0f, 1.0f, 0.4f).rotate(wallYaw, 0, 1, 0);
                    h.glow(110, 130, 80).interpolation(10, 0);
                    spawnedEntities.add(h.entity());
                    wall.add(h);
                }
            }
            // Wall rise Y: -1 → 0
            for (BlockDisplayHandle h : wall) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(10);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        new Vector3f(t.getTranslation().x, t.getTranslation().y + 1.0f, t.getTranslation().z),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            // 20 thin ivy tendrils extending from wall toward player
            for (int i = 0; i < 20; i++) {
                int row = i / 4;
                int col = i % 4 - 2;
                double bx = wallOffsetX + pX * col * 1.0;
                double bz = wallOffsetZ + pZ * col * 1.0;
                double yOff = 0.2 + row * 1.0;
                Location loc = center.clone().add(bx, yOff, bz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GREEN_CONCRETE);
                h.scale(0.1f, 0.1f, 0.0f); // start short
                h.rotate(wallYaw, 0, 1, 0);
                h.glow(60, 160, 60).interpolation(15, i);
                spawnedEntities.add(h.entity());
                tendrils.add(h);
            }
            // Tendrils extend Z scale 0 → 1.5
            for (int i = 0; i < tendrils.size(); i++) {
                BlockDisplay e = tendrils.get(i).entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(15);
                e.setInterpolationDelay(i);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        new Vector3f(0.1f, 0.1f, 1.5f),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            // 4 RED_CONCRETE roses at corners
            double[][] roseOffsets = {{-1.5, 1.0}, {1.0, 1.0}, {-1.5, 3.5}, {1.0, 3.5}};
            for (double[] off : roseOffsets) {
                double bx = wallOffsetX + pX * off[0];
                double bz = wallOffsetZ + pZ * off[0];
                Location loc = center.clone().add(bx, off[1], bz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                h.scale(0.4f, 0.4f, 0.4f).rotate(wallYaw, 0, 1, 0);
                h.glow(220, 40, 60).interpolation(10, 5);
                spawnedEntities.add(h.entity());
                roses.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_VINE_PLACE, 1.2f, 0.7f);
            w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, center.clone().add(0, 2, 0), 30, 3, 1.5, 3, 0.04);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Tendrils creep forward (+0.05 Z scale per tick, capped at 2.5)
            crawlOffset += 0.05f;
            float targetZ = Math.min(1.5f + crawlOffset, 2.5f);
            if (tick > 30 && tick % 6 == 0) {
                for (BlockDisplayHandle h : tendrils) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(6);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.1f, 0.1f, targetZ),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Roses bloom pulse
            if (tick > 30 && tick % 8 == 0) {
                float rosePulse = 0.4f + 0.08f * (float) Math.sin(tick * 0.15);
                for (BlockDisplayHandle h : roses) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(8);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(rosePulse, rosePulse, rosePulse),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Particles
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                        c.clone().add(0, 2.0, 0), 4, 2.5, 1.5, 2.5, 0.02);
            }
            if (tick % 8 == 0) {
                for (BlockDisplayHandle rose : roses) {
                    c.getWorld().spawnParticle(Particle.ENCHANT,
                            rose.entity().getLocation(), 4, 0.3, 0.3, 0.3, 1.0);
                }
            }

            // Tendril extension sounds
            if (tick > 0 && tick <= 60 && tick % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_VINE_PLACE, 0.7f, 0.6f + (tick / 60.0f) * 0.4f);
            }
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_VINE_PLACE, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IvyWallCrawl(plugin); }
    }
}
