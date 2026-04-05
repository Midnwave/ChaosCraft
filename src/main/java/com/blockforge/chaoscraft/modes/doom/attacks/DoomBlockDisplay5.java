package com.blockforge.chaoscraft.modes.doom.attacks;

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
 * Doom Mode — BLOCK DISPLAY ATTACKS (Part 5, attacks 41-50)
 * 10 infernal doom-themed BlockDisplay attacks featuring fountains,
 * skulls, bells, blood, celestial judgment, shadow spikes, drills,
 * corruption, lava cages, and undying flame altars.
 *
 * Categories:
 *   41-42: Fountain / Skull structures
 *   43-44: Bell / Blood structures
 *   45-46: Celestial / Shadow burst
 *   47-48: Drill / Miasma
 *   49-50: Cage / Altar
 *
 * Doom palette:
 * - Lava orange: RGB(255, 100, 20)
 * - Hellfire red: RGB(200, 50, 10)
 * - Charred black: RGB(20, 10, 5)
 * - Ember glow: RGB(240, 80, 30)
 * - Brimstone yellow: RGB(220, 180, 30)
 * - Nether purple: RGB(120, 20, 80)
 * - Blood red: RGB(180, 20, 20)
 * - Corruption teal: RGB(30, 80, 100)
 *
 * Materials: MAGMA_BLOCK, NETHERRACK, NETHER_BRICKS, RED_NETHER_BRICKS,
 *            BLACKSTONE, POLISHED_BLACKSTONE, GILDED_BLACKSTONE,
 *            CHISELED_POLISHED_BLACKSTONE, CRYING_OBSIDIAN, SHROOMLIGHT,
 *            BASALT, DEEPSLATE, COAL_BLOCK, OBSIDIAN, RED_CONCRETE,
 *            RED_TERRACOTTA, BLACK_CONCRETE, ORANGE_CONCRETE, GLOWSTONE,
 *            BONE_BLOCK, CALCITE, IRON_BLOCK, GOLD_BLOCK, SCULK,
 *            AMETHYST_BLOCK, CHAIN, NETHER_BRICK_FENCE
 */
public final class DoomBlockDisplay5 {
    private DoomBlockDisplay5() {}

    private static final String MODE_PATH = "modes/doom/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // Fountain / Skull (41-42)
        registry.register(new LavaFountainRing(plugin));
        registry.register(new SkullWall(plugin));
        // Bell / Blood (43-44)
        registry.register(new DoomBell(plugin));
        registry.register(new BloodFountain(plugin));
        // Celestial / Shadow (45-46)
        registry.register(new NetherStarBurst(plugin));
        registry.register(new ShadowSpikeBurst(plugin));
        // Drill / Miasma (47-48)
        registry.register(new InfernalDrill(plugin));
        registry.register(new CorruptionMiasma(plugin));
        // Cage / Altar (49-50)
        registry.register(new LavaCageMelt(plugin));
        registry.register(new TheUndyingFlame(plugin));
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
    // 41. LAVA FOUNTAIN RING — "The Cascade" (42 displays)
    //     6 fountains in a hexagonal ring. Each fountain has:
    //     - 4 MAGMA_BLOCK base-to-top (decreasing scale 0.7 -> 0.35)
    //     - 3 SHROOMLIGHT spray angled outward at the top
    //     Spray arcs outward via position oscillation. Ring zone is
    //     danger; center and outer are safe. Constant annulus damage.
    //     6 × (4 + 3) = 42 blocks
    // ================================================================
    public static class LavaFountainRing extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> sprayBlocks = new ArrayList<>();
        private final double[] fountainAngles = new double[6];

        public LavaFountainRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_fountain_ring", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
            config.setChance(7);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            double ringRadius = 5.0;
            float[] baseScales = {0.7f, 0.55f, 0.45f, 0.35f};

            for (int f = 0; f < 6; f++) {
                double angle = (2 * Math.PI * f) / 6.0;
                fountainAngles[f] = angle;
                double fx = Math.cos(angle) * ringRadius;
                double fz = Math.sin(angle) * ringRadius;

                // 4 stacked MAGMA_BLOCK segments (base to top, decreasing scale)
                for (int seg = 0; seg < 4; seg++) {
                    float s = baseScales[seg];
                    Location loc = center.clone().add(fx, 0.1 + seg * 0.7, fz);
                    BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    b.scale(s, 0.7f, s).glow(255, 100, 20).interpolation(3, 0);
                    spawnedEntities.add(b.entity());
                    baseBlocks.add(b);
                }

                // 3 SHROOMLIGHT spray cubes angled outward at the top
                double outAngle = angle; // direction outward from center
                for (int sp = 0; sp < 3; sp++) {
                    double spreadAngle = outAngle + (sp - 1) * 0.3;
                    double outDist = 0.6 + sp * 0.3;
                    double sx = fx + Math.cos(spreadAngle) * outDist;
                    double sz = fz + Math.sin(spreadAngle) * outDist;
                    Location loc = center.clone().add(sx, 2.8 + sp * 0.4, sz);
                    BlockDisplayHandle spray = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                    spray.scale(0.25f, 0.25f, 0.25f).glow(240, 80, 30).interpolation(3, 0);
                    spawnedEntities.add(spray.entity());
                    sprayBlocks.add(spray);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.2f, 0.5f);
            w.spawnParticle(Particle.LAVA, center.clone().add(0, 2, 0), 30, 5, 1, 5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double ringRadius = 5.0;

            // Spray oscillation: arc outward and upward, then back
            for (int f = 0; f < 6; f++) {
                double angle = fountainAngles[f];
                double fx = Math.cos(angle) * ringRadius;
                double fz = Math.sin(angle) * ringRadius;

                for (int sp = 0; sp < 3; sp++) {
                    int idx = f * 3 + sp;
                    if (idx >= sprayBlocks.size()) break;

                    double spreadAngle = angle + (sp - 1) * 0.3;
                    // Oscillate outward distance and height
                    double phase = tick * 0.08 + f * 0.5;
                    double outPulse = 0.6 + sp * 0.3 + Math.sin(phase) * 1.2;
                    double yPulse = 2.8 + sp * 0.4 + Math.abs(Math.sin(phase)) * 1.5;
                    double sx = fx + Math.cos(spreadAngle) * outPulse;
                    double sz = fz + Math.sin(spreadAngle) * outPulse;
                    Location loc = center.clone().add(sx, yPulse, sz);
                    loc.setYaw(0);
                    loc.setPitch(0);
                    sprayBlocks.get(idx).entity().teleport(loc);
                }
            }

            // Lava drip particles at spray tips
            if (tick % 4 == 0) {
                for (int i = 0; i < sprayBlocks.size(); i += 3) {
                    Location sLoc = sprayBlocks.get(i).entity().getLocation();
                    center.getWorld().spawnParticle(Particle.LAVA, sLoc, 2, 0.2, 0.3, 0.2, 0.01);
                }
            }

            // Ember particles around ring zone
            if (tick % 6 == 0) {
                double pAngle = Math.random() * 2 * Math.PI;
                double px = Math.cos(pAngle) * ringRadius;
                double pz = Math.sin(pAngle) * ringRadius;
                DisplayBuilder.dustParticles(center.clone().add(px, 1, pz), 3, 0.5, 255, 100, 20, 1.2f);
            }

            // Ambient lava bubble sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.7f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LavaFountainRing(plugin); }
    }

    // ================================================================
    // 42. SKULL WALL — "The Advance of the Dead" (40 displays)
    //     5 wide x 2 high = 10 skulls. Each skull:
    //     - 1 BONE_BLOCK head (0.5x0.5x0.4)
    //     - 2 CALCITE eyes (0.12x0.1x0.05)
    //     - 1 BONE_BLOCK jaw (0.4x0.15x0.1) angled -15 deg
    //     Wall marches toward player. Jaws chatter (open/close).
    //     Wide but slow. 10 x 4 = 40 blocks.
    // ================================================================
    public static class SkullWall extends BlockDisplayAttack {
        private Location center;
        private Player target;
        private double advanceDist = 0;
        private double dirX, dirZ;
        private final List<BlockDisplayHandle> heads = new ArrayList<>();
        private final List<BlockDisplayHandle> leftEyes = new ArrayList<>();
        private final List<BlockDisplayHandle> rightEyes = new ArrayList<>();
        private final List<BlockDisplayHandle> jaws = new ArrayList<>();

        public SkullWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("skull_wall", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(7.0);
            config.setDamageRadius(0.8);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(180);
            config.setCooldownTicks(200);
            config.setChance(7);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Find target player to march toward
            target = findNearestPlayer(center, 30);
            if (target == null) {
                dirX = 0;
                dirZ = 1;
            } else {
                Location tLoc = target.getLocation();
                double dx = tLoc.getX() - center.getX();
                double dz = tLoc.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len < 0.01) { dirX = 0; dirZ = 1; }
                else { dirX = dx / len; dirZ = dz / len; }
            }

            // Perpendicular direction for wall spread
            double perpX = -dirZ;
            double perpZ = dirX;

            // 5 wide x 2 high = 10 skulls
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 5; col++) {
                    double spreadX = (col - 2) * 1.2 * perpX;
                    double spreadZ = (col - 2) * 1.2 * perpZ;
                    double baseY = 0.5 + row * 1.3;

                    // Head
                    Location headLoc = center.clone().add(spreadX, baseY, spreadZ);
                    BlockDisplayHandle head = displayBuilder.spawnBlock(headLoc, Material.BONE_BLOCK);
                    head.scale(0.5f, 0.5f, 0.4f).glow(220, 200, 180).interpolation(3, 0);
                    spawnedEntities.add(head.entity());
                    heads.add(head);

                    // Left eye
                    Location lEyeLoc = headLoc.clone().add(dirX * 0.21 + perpX * 0.1, 0.2, dirZ * 0.21 + perpZ * 0.1);
                    BlockDisplayHandle lEye = displayBuilder.spawnBlock(lEyeLoc, Material.CALCITE);
                    lEye.scale(0.12f, 0.1f, 0.05f).glow(200, 50, 10).interpolation(2, 0);
                    spawnedEntities.add(lEye.entity());
                    leftEyes.add(lEye);

                    // Right eye
                    Location rEyeLoc = headLoc.clone().add(dirX * 0.21 - perpX * 0.1, 0.2, dirZ * 0.21 - perpZ * 0.1);
                    BlockDisplayHandle rEye = displayBuilder.spawnBlock(rEyeLoc, Material.CALCITE);
                    rEye.scale(0.12f, 0.1f, 0.05f).glow(200, 50, 10).interpolation(2, 0);
                    spawnedEntities.add(rEye.entity());
                    rightEyes.add(rEye);

                    // Jaw (angled -15 degrees)
                    Location jawLoc = headLoc.clone().add(dirX * 0.05, -0.15, dirZ * 0.05);
                    BlockDisplayHandle jaw = displayBuilder.spawnBlock(jawLoc, Material.BONE_BLOCK);
                    jaw.scale(0.4f, 0.15f, 0.1f).glow(220, 200, 180).interpolation(2, 0);
                    // Apply initial jaw rotation
                    BlockDisplay jawEntity = jaw.entity();
                    jawEntity.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f((float) Math.toRadians(-15), 1, 0, 0),
                        new Vector3f(0.4f, 0.15f, 0.1f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    spawnedEntities.add(jawEntity);
                    jaws.add(jaw);
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_SKELETON_AMBIENT, 1.0f, 0.5f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 1.5, 0), 20, 3, 1, 0.5, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // March forward
            advanceDist += 0.6 / 15.0; // 0.6 blocks per update cycle with teleportDuration
            double advX = dirX * advanceDist;
            double advZ = dirZ * advanceDist;

            double perpX = -dirZ;
            double perpZ = dirX;

            // Jaw chatter angle
            float jawAngle = (float) Math.toRadians(-15 + Math.sin(tick * 0.3) * 15);

            for (int i = 0; i < heads.size(); i++) {
                int col = i % 5;
                int row = i / 5;
                double spreadX = (col - 2) * 1.2 * perpX;
                double spreadZ = (col - 2) * 1.2 * perpZ;
                double baseY = 0.5 + row * 1.3;

                // Head position
                Location headLoc = center.clone().add(spreadX + advX, baseY, spreadZ + advZ);
                headLoc.setYaw(0);
                headLoc.setPitch(0);
                BlockDisplay headEnt = heads.get(i).entity();
                headEnt.setTeleportDuration(15);
                headEnt.teleport(headLoc);

                // Eyes follow head
                Location lEyeLoc = headLoc.clone().add(dirX * 0.21 + perpX * 0.1, 0.2, dirZ * 0.21 + perpZ * 0.1);
                lEyeLoc.setYaw(0);
                lEyeLoc.setPitch(0);
                BlockDisplay lEyeEnt = leftEyes.get(i).entity();
                lEyeEnt.setTeleportDuration(15);
                lEyeEnt.teleport(lEyeLoc);

                Location rEyeLoc = headLoc.clone().add(dirX * 0.21 - perpX * 0.1, 0.2, dirZ * 0.21 - perpZ * 0.1);
                rEyeLoc.setYaw(0);
                rEyeLoc.setPitch(0);
                BlockDisplay rEyeEnt = rightEyes.get(i).entity();
                rEyeEnt.setTeleportDuration(15);
                rEyeEnt.teleport(rEyeLoc);

                // Jaw follows head with chatter rotation
                Location jawLoc = headLoc.clone().add(dirX * 0.05, -0.15, dirZ * 0.05);
                jawLoc.setYaw(0);
                jawLoc.setPitch(0);
                BlockDisplay jawEnt = jaws.get(i).entity();
                jawEnt.setTeleportDuration(15);
                jawEnt.teleport(jawLoc);

                // Animate jaw chatter via transformation
                jawEnt.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(jawAngle, 1, 0, 0),
                    new Vector3f(0.4f, 0.15f, 0.1f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                jawEnt.setInterpolationDuration(4);
                jawEnt.setInterpolationDelay(0);
            }

            // Update damage center to wall midpoint
            setCenter(center.clone().add(advX, 1.0, advZ));

            // Bone rattle particles
            if (tick % 5 == 0) {
                for (int i = 0; i < heads.size(); i += 3) {
                    Location hLoc = heads.get(i).entity().getLocation();
                    DisplayBuilder.dustParticles(hLoc, 2, 0.3, 220, 200, 180, 0.8f);
                }
            }

            // Clacking sound
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(center.clone().add(advX, 1, advZ),
                    Sound.ENTITY_SKELETON_STEP, 0.8f, 0.6f);
            }

            // Creaking march sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center.clone().add(advX, 1, advZ),
                    Sound.ENTITY_SKELETON_AMBIENT, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new SkullWall(plugin); }
    }

    // ================================================================
    // 43. DOOM BELL — "The Toll" (18 displays)
    //     Bell body: 5 IRON_BLOCK flat slabs in bell shape (wide top
    //     1.0 -> narrow bottom 0.2, 0.05 Y each, stacked)
    //     Bell rim: 8 IRON_BLOCK cubes in ring at bottom edge
    //     Clapper: 1 BLACKSTONE (0.1x0.3x0.1) hanging inside
    //     Chain: 3 CHAIN blocks stacked above
    //     Bell swings on Z (+/-20 deg, 50-tick period). On max swing,
    //     ground shockwave ring expands. Rings 3 times total.
    //     Impact-only damage: 14.0 dmg, 4.0 radius.
    //     5 body + 8 rim + 1 clapper + 3 chain + 1 crown = 18 blocks
    // ================================================================
    public static class DoomBell extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> bodySlabs = new ArrayList<>();
        private final List<BlockDisplayHandle> rimBlocks = new ArrayList<>();
        private BlockDisplayHandle clapper;
        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private int ringCount = 0;
        private boolean lastSwingPositive = false;
        private static final double BELL_Y = 5.0;

        public DoomBell(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doom_bell", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(220);
            config.setChance(6);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Bell body: 5 flat slabs wide-to-narrow (bell profile)
            float[] bodyWidths = {1.0f, 0.85f, 0.65f, 0.4f, 0.2f};
            for (int i = 0; i < 5; i++) {
                float bw = bodyWidths[i];
                Location loc = center.clone().add(0, BELL_Y + (4 - i) * 0.5, 0);
                BlockDisplayHandle slab = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                slab.scale(bw, 0.5f, bw).glow(180, 170, 160).interpolation(3, 0);
                spawnedEntities.add(slab.entity());
                bodySlabs.add(slab);
            }

            // Bell rim: 8 small cubes in a ring at the bottom edge
            double rimRadius = 0.55;
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8.0;
                double rx = Math.cos(angle) * rimRadius;
                double rz = Math.sin(angle) * rimRadius;
                Location loc = center.clone().add(rx, BELL_Y - 0.15, rz);
                BlockDisplayHandle rim = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                rim.scale(0.18f, 0.12f, 0.18f).glow(200, 190, 170).interpolation(2, 0);
                spawnedEntities.add(rim.entity());
                rimBlocks.add(rim);
            }

            // Clapper hanging inside bell
            Location clapperLoc = center.clone().add(0, BELL_Y - 0.1, 0);
            clapper = displayBuilder.spawnBlock(clapperLoc, Material.BLACKSTONE);
            clapper.scale(0.1f, 0.3f, 0.1f).glow(20, 10, 5).interpolation(2, 0);
            spawnedEntities.add(clapper.entity());

            // Chain: 3 blocks stacked above the bell
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, BELL_Y + 2.6 + i * 0.5, 0);
                BlockDisplayHandle link = displayBuilder.spawnBlock(loc, Material.CHAIN);
                link.scale(0.15f, 0.5f, 0.15f).glow(100, 90, 80).interpolation(2, 0);
                spawnedEntities.add(link.entity());
                chainLinks.add(link);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.3f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, BELL_Y, 0), 15, 1, 1, 1, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Bell swings on Z axis: angle = 20 * sin(2*PI*tick/50)
            double swingAngle = Math.toRadians(20.0 * Math.sin(2 * Math.PI * tick / 50.0));
            boolean swingPositive = Math.sin(2 * Math.PI * tick / 50.0) > 0;

            // Pivot point is at the top of the chain
            double pivotY = BELL_Y + 4.1;

            // Update bell body positions (swing around pivot)
            float[] bodyWidths = {1.0f, 0.85f, 0.65f, 0.4f, 0.2f};
            for (int i = 0; i < bodySlabs.size(); i++) {
                double localY = BELL_Y + (4 - i) * 0.5 - pivotY;
                double swungZ = Math.sin(swingAngle) * (-localY);
                double swungY = pivotY + Math.cos(swingAngle) * localY;
                Location loc = center.clone().add(0, swungY, swungZ);
                loc.setYaw(0);
                loc.setPitch(0);

                BlockDisplay entity = bodySlabs.get(i).entity();
                entity.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) swingAngle, 1, 0, 0),
                    new Vector3f(bodyWidths[i], 0.5f, bodyWidths[i]),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                entity.setInterpolationDuration(3);
                entity.setInterpolationDelay(0);
                entity.teleport(loc);
            }

            // Update rim blocks (swing with bell)
            double rimRadius = 0.55;
            for (int i = 0; i < rimBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 8.0;
                double rx = Math.cos(angle) * rimRadius;
                double rz0 = Math.sin(angle) * rimRadius;
                double localY = BELL_Y - 0.15 - pivotY;
                double swungZ = Math.sin(swingAngle) * (-localY) + rz0;
                double swungY = pivotY + Math.cos(swingAngle) * localY;
                Location loc = center.clone().add(rx, swungY, swungZ);
                loc.setYaw(0);
                loc.setPitch(0);
                rimBlocks.get(i).entity().teleport(loc);
            }

            // Update clapper (slightly delayed swing for visual effect)
            double clapperSwing = Math.toRadians(25.0 * Math.sin(2 * Math.PI * tick / 50.0 + 0.3));
            double cLocalY = BELL_Y - 0.1 - pivotY;
            double cSwungZ = Math.sin(clapperSwing) * (-cLocalY);
            double cSwungY = pivotY + Math.cos(clapperSwing) * cLocalY;
            Location cLoc = center.clone().add(0, cSwungY, cSwungZ);
            cLoc.setYaw(0);
            cLoc.setPitch(0);
            clapper.entity().teleport(cLoc);

            // Chain links stay at pivot (fixed point)
            for (int i = 0; i < chainLinks.size(); i++) {
                Location loc = center.clone().add(0, BELL_Y + 2.6 + i * 0.5, 0);
                loc.setYaw(0);
                loc.setPitch(0);
                chainLinks.get(i).entity().teleport(loc);
            }

            // Detect max swing for impact shockwave (3 tolls total)
            if (swingPositive != lastSwingPositive && ringCount < 3) {
                ringCount++;
                lastSwingPositive = swingPositive;

                // Ground shockwave at impact
                Location impactLoc = center.clone().add(0, 0.5, 0);

                // Expanding ring particles
                for (int p = 0; p < 24; p++) {
                    double pAngle = (2 * Math.PI * p) / 24.0;
                    double radius = config.getImpactRadius();
                    double px = Math.cos(pAngle) * radius;
                    double pz = Math.sin(pAngle) * radius;
                    DisplayBuilder.dustParticles(impactLoc.clone().add(px, 0, pz), 2, 0.2, 180, 170, 160, 1.5f);
                }

                center.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 3, 2, 0.5, 2, 0.01);
                DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);

                // Damage nearby players
                for (Player p : center.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(impactLoc) <= config.getImpactRadius() * config.getImpactRadius()) {
                        p.damage(config.getImpactDamage());
                    }
                }
            }
            lastSwingPositive = swingPositive;

            // Ambient creaking
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_STEP, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DoomBell(plugin); }
    }

    // ================================================================
    // 44. BLOOD FOUNTAIN — "The Eruption" (16 displays)
    //     Lower basin: 4 RED_TERRACOTTA flat ring (1.0x0.1x0.1)
    //     Column: 1 RED_CONCRETE (0.3x1.5x0.3)
    //     Upper basin: smaller ring above (not counted, reusing column top)
    //     Spray: 6 RED_CONCRETE sphere cubes at apex
    //     Arc streams: 4 RED_CONCRETE flat slabs curving outward in
    //     4 directions (1 per direction)
    //     Builds bottom to top. Spray pulses Y +/-0.3. Arcs land at
    //     impact zones. Impact damage at arc landings, constant at column.
    //     4 basin + 1 column + 6 spray + 4 arcs + 1 upper = 16 blocks
    // ================================================================
    public static class BloodFountain extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> basinBlocks = new ArrayList<>();
        private BlockDisplayHandle column;
        private BlockDisplayHandle upperBasin;
        private final List<BlockDisplayHandle> sprayBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> arcBlocks = new ArrayList<>();
        private int buildPhase = 0; // 0=building, 1=active

        public BloodFountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blood_fountain", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(1.0);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(20);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(8.0);
            config.setImpactRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(170);
            config.setChance(8);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Lower basin: 4 RED_TERRACOTTA flat pieces in a ring
            double[][] basinOffsets = {{0.5, 0, 0}, {-0.5, 0, 0}, {0, 0, 0.5}, {0, 0, -0.5}};
            for (double[] off : basinOffsets) {
                Location loc = center.clone().add(off[0], 0.05, off[2]);
                BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.RED_TERRACOTTA);
                b.scale(1.0f, 0.1f, 0.1f).glow(180, 20, 20).interpolation(5, 0);
                spawnedEntities.add(b.entity());
                basinBlocks.add(b);
            }

            // Column: 1 RED_CONCRETE tall pillar (starts at ground, grows)
            Location colLoc = center.clone().add(0, 0.1, 0);
            column = displayBuilder.spawnBlock(colLoc, Material.RED_CONCRETE);
            column.scale(0.3f, 0.01f, 0.3f).glow(180, 20, 20).interpolation(10, 5);
            spawnedEntities.add(column.entity());

            // Upper basin (smaller)
            Location upperLoc = center.clone().add(0, 1.6, 0);
            upperBasin = displayBuilder.spawnBlock(upperLoc, Material.RED_TERRACOTTA);
            upperBasin.scale(0.01f, 0.01f, 0.01f).glow(180, 20, 20).interpolation(10, 10);
            spawnedEntities.add(upperBasin.entity());

            // Spray: 6 RED_CONCRETE sphere cubes at apex (start invisible/small)
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6.0;
                double sx = Math.cos(angle) * 0.2;
                double sz = Math.sin(angle) * 0.2;
                Location loc = center.clone().add(sx, 2.5, sz);
                BlockDisplayHandle spray = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                spray.scale(0.01f, 0.01f, 0.01f).glow(200, 30, 30).interpolation(10, 15);
                spawnedEntities.add(spray.entity());
                sprayBlocks.add(spray);
            }

            // Arc streams: 4 flat slabs curving outward (start invisible)
            double[][] arcDirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (double[] dir : arcDirs) {
                Location loc = center.clone().add(dir[0] * 1.5, 2.0, dir[1] * 1.5);
                BlockDisplayHandle arc = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                arc.scale(0.01f, 0.01f, 0.01f).glow(180, 20, 20).interpolation(10, 18);
                spawnedEntities.add(arc.entity());
                arcBlocks.add(arc);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WET_SPONGE_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Build-up phase: grow column and reveal elements
            if (tick == 5) {
                // Grow column to full height
                BlockDisplay colEnt = column.entity();
                colEnt.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.3f, 1.5f, 0.3f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                colEnt.setInterpolationDuration(15);
                colEnt.setInterpolationDelay(0);

                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 1.2f);
            }

            if (tick == 12) {
                // Reveal upper basin
                BlockDisplay ubEnt = upperBasin.entity();
                ubEnt.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.6f, 0.08f, 0.6f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                ubEnt.setInterpolationDuration(8);
                ubEnt.setInterpolationDelay(0);
            }

            if (tick == 18) {
                // Reveal spray cubes
                for (int i = 0; i < sprayBlocks.size(); i++) {
                    BlockDisplay spEnt = sprayBlocks.get(i).entity();
                    spEnt.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.15f, 0.15f, 0.15f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    spEnt.setInterpolationDuration(8);
                    spEnt.setInterpolationDelay(0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_SPLASH, 0.8f, 0.6f);
            }

            if (tick == 22) {
                // Reveal arc streams
                for (BlockDisplayHandle arc : arcBlocks) {
                    BlockDisplay arcEnt = arc.entity();
                    arcEnt.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f((float) Math.toRadians(45), 1, 0, 0),
                        new Vector3f(0.12f, 1.2f, 0.12f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    arcEnt.setInterpolationDuration(8);
                    arcEnt.setInterpolationDelay(0);
                }
                buildPhase = 1;
            }

            // Active phase: animate spray and arcs
            if (buildPhase == 1) {
                // Spray cubes pulse height
                for (int i = 0; i < sprayBlocks.size(); i++) {
                    double angle = (2 * Math.PI * i) / 6.0;
                    double sx = Math.cos(angle) * 0.2;
                    double sz = Math.sin(angle) * 0.2;
                    double yPulse = 2.5 + Math.sin(tick * 0.1 + i * 0.5) * 0.3;
                    Location loc = center.clone().add(sx, yPulse, sz);
                    loc.setYaw(0);
                    loc.setPitch(0);
                    sprayBlocks.get(i).entity().teleport(loc);
                }

                // Arc landing positions (oscillate slightly)
                double[][] arcDirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int i = 0; i < arcBlocks.size(); i++) {
                    double dist = 1.5 + Math.sin(tick * 0.06 + i) * 0.4;
                    double arcY = 2.0 - Math.abs(Math.sin(tick * 0.06 + i)) * 1.2;
                    Location loc = center.clone().add(arcDirs[i][0] * dist, arcY, arcDirs[i][1] * dist);
                    loc.setYaw(0);
                    loc.setPitch(0);
                    arcBlocks.get(i).entity().teleport(loc);
                }

                // Impact damage at arc landing zones
                if (tick % 15 == 0) {
                    for (int i = 0; i < arcBlocks.size(); i++) {
                        Location arcLoc = arcBlocks.get(i).entity().getLocation();
                        for (Player p : center.getWorld().getPlayers()) {
                            if (p.getGameMode() != GameMode.SURVIVAL) continue;
                            if (p.getLocation().distanceSquared(arcLoc) <= config.getImpactRadius() * config.getImpactRadius()) {
                                p.damage(config.getImpactDamage());
                            }
                        }
                    }
                }

                // Blood drip particles
                if (tick % 3 == 0) {
                    for (BlockDisplayHandle spray : sprayBlocks) {
                        Location sLoc = spray.entity().getLocation();
                        DisplayBuilder.dustParticles(sLoc, 2, 0.3, 180, 20, 20, 1.0f);
                    }
                }
            }

            // Ambient dripping sound
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BloodFountain(plugin); }
    }

    // ================================================================
    // 45. NETHER STAR BURST — "The Celestial Judgment" (17 displays)
    //     5 GOLD_BLOCK star points (0.12x0.5x0.12) at 72-deg intervals,
    //       tilted 35 deg Z outward
    //     5 SHROOMLIGHT inner pentagon slabs connecting
    //     1 GLOWSTONE center
    //     5 SHROOMLIGHT beams (0.08x2.0x0.08) descending from tips
    //     Star rotates on Y (full per 5s = 100 ticks). Beams pulse
    //     Y scale. Impact damage where beams touch ground per-beam.
    //     5 + 5 + 1 + 5 + 1(center glow) = 17 blocks (center counted once)
    // ================================================================
    public static class NetherStarBurst extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> starPoints = new ArrayList<>();
        private final List<BlockDisplayHandle> pentagonSlabs = new ArrayList<>();
        private BlockDisplayHandle centerGlow;
        private final List<BlockDisplayHandle> beams = new ArrayList<>();
        private static final double STAR_Y = 6.0;
        private static final double STAR_RADIUS = 2.5;

        public NetherStarBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_star_burst", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(9.0);
            config.setImpactRadius(1.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
            config.setChance(7);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Center glowstone
            Location cLoc = center.clone().add(0, STAR_Y, 0);
            centerGlow = displayBuilder.spawnBlock(cLoc, Material.GLOWSTONE);
            centerGlow.scale(0.4f, 0.4f, 0.4f).glow(220, 180, 30).interpolation(3, 0);
            spawnedEntities.add(centerGlow.entity());

            // 5 star points at 72-degree intervals
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5.0;
                double px = Math.cos(angle) * STAR_RADIUS;
                double pz = Math.sin(angle) * STAR_RADIUS;
                Location loc = center.clone().add(px, STAR_Y, pz);
                BlockDisplayHandle point = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                point.scale(0.12f, 0.5f, 0.12f).glow(220, 180, 30).interpolation(3, 0);

                // Tilt outward at 35 degrees on Z axis relative to star direction
                BlockDisplay pEnt = point.entity();
                float tiltAngle = (float) Math.toRadians(35);
                pEnt.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(tiltAngle, (float) Math.cos(angle), 0, (float) Math.sin(angle)),
                    new Vector3f(0.12f, 0.5f, 0.12f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                pEnt.setInterpolationDuration(3);
                pEnt.setInterpolationDelay(0);
                spawnedEntities.add(pEnt);
                starPoints.add(point);
            }

            // 5 inner pentagon slabs connecting the star
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5.0 + Math.PI / 5.0;
                double innerR = STAR_RADIUS * 0.5;
                double sx = Math.cos(angle) * innerR;
                double sz = Math.sin(angle) * innerR;
                Location loc = center.clone().add(sx, STAR_Y - 0.1, sz);
                BlockDisplayHandle slab = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                slab.scale(0.5f, 0.08f, 0.2f).glow(240, 200, 50).interpolation(3, 0);
                spawnedEntities.add(slab.entity());
                pentagonSlabs.add(slab);
            }

            // 5 beams descending from star tips to ground
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5.0;
                double bx = Math.cos(angle) * STAR_RADIUS;
                double bz = Math.sin(angle) * STAR_RADIUS;
                Location loc = center.clone().add(bx, STAR_Y / 2.0, bz);
                BlockDisplayHandle beam = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                beam.scale(0.08f, 2.0f, 0.08f).glow(255, 220, 60).interpolation(3, 0);
                spawnedEntities.add(beam.entity());
                beams.add(beam);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f, 0.4f);
            w.spawnParticle(Particle.END_ROD, center.clone().add(0, STAR_Y, 0), 20, 2, 1, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Star rotates on Y — full rotation every 100 ticks (5s)
            double rotAngle = (2 * Math.PI * tick) / 100.0;

            // Update star points
            for (int i = 0; i < 5; i++) {
                double baseAngle = (2 * Math.PI * i) / 5.0;
                double angle = baseAngle + rotAngle;
                double px = Math.cos(angle) * STAR_RADIUS;
                double pz = Math.sin(angle) * STAR_RADIUS;
                Location loc = center.clone().add(px, STAR_Y, pz);
                loc.setYaw(0);
                loc.setPitch(0);

                BlockDisplay pEnt = starPoints.get(i).entity();
                float tiltAngle = (float) Math.toRadians(35);
                pEnt.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(tiltAngle, (float) Math.cos(angle), 0, (float) Math.sin(angle)),
                    new Vector3f(0.12f, 0.5f, 0.12f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                pEnt.setInterpolationDuration(3);
                pEnt.setInterpolationDelay(0);
                pEnt.teleport(loc);
            }

            // Update pentagon slabs
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5.0 + Math.PI / 5.0 + rotAngle;
                double innerR = STAR_RADIUS * 0.5;
                double sx = Math.cos(angle) * innerR;
                double sz = Math.sin(angle) * innerR;
                Location loc = center.clone().add(sx, STAR_Y - 0.1, sz);
                loc.setYaw(0);
                loc.setPitch(0);
                pentagonSlabs.get(i).entity().teleport(loc);
            }

            // Center glow stays put
            Location cLoc = center.clone().add(0, STAR_Y, 0);
            cLoc.setYaw(0);
            cLoc.setPitch(0);
            centerGlow.entity().teleport(cLoc);

            // Update beams — follow star rotation, pulse Y scale
            for (int i = 0; i < 5; i++) {
                double baseAngle = (2 * Math.PI * i) / 5.0;
                double angle = baseAngle + rotAngle;
                double bx = Math.cos(angle) * STAR_RADIUS;
                double bz = Math.sin(angle) * STAR_RADIUS;
                // Pulse beam height
                float beamHeight = 2.0f + (float) Math.sin(tick * 0.08 + i) * 0.8f;
                double beamBaseY = STAR_Y - beamHeight;
                Location loc = center.clone().add(bx, beamBaseY + beamHeight / 2.0, bz);
                loc.setYaw(0);
                loc.setPitch(0);

                BlockDisplay bEnt = beams.get(i).entity();
                bEnt.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(0.08f, beamHeight, 0.08f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                bEnt.setInterpolationDuration(4);
                bEnt.setInterpolationDelay(0);
                bEnt.teleport(loc);

                // Ground impact point for each beam
                Location groundImpact = center.clone().add(bx, 0.5, bz);

                // Per-beam impact damage
                if (tick % 20 == 0) {
                    for (Player p : center.getWorld().getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL) continue;
                        if (p.getLocation().distanceSquared(groundImpact) <= config.getImpactRadius() * config.getImpactRadius()) {
                            p.damage(config.getImpactDamage());
                        }
                    }
                    // Impact particles at ground
                    DisplayBuilder.dustParticles(groundImpact, 4, 0.3, 255, 220, 60, 1.2f);
                    center.getWorld().spawnParticle(Particle.END_ROD, groundImpact, 3, 0.2, 0.5, 0.2, 0.02);
                }
            }

            // Celestial hum
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, STAR_Y, 0), Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.5f);
            }

            // Star sparkles
            if (tick % 6 == 0) {
                double sparkAngle = Math.random() * 2 * Math.PI;
                double sparkR = Math.random() * STAR_RADIUS;
                center.getWorld().spawnParticle(Particle.END_ROD,
                    center.clone().add(Math.cos(sparkAngle) * sparkR, STAR_Y + 0.5, Math.sin(sparkAngle) * sparkR),
                    1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new NetherStarBurst(plugin); }
    }

    // ================================================================
    // 46. SHADOW SPIKE BURST — "The Eruption" (36 displays)
    //     12 spikes in circle pointing inward at 45 deg toward center.
    //     Each spike: 3 OBSIDIAN segments decreasing in size
    //       (0.3x0.5x0.3), (0.2x0.4x0.2), (0.12x0.3x0.12)
    //     All erupt simultaneously from underground angled inward.
    //     Impact damage on eruption, then constant inside ring.
    //     12 x 3 = 36 blocks
    // ================================================================
    public static class ShadowSpikeBurst extends BlockDisplayAttack {
        private Location center;
        private final List<List<BlockDisplayHandle>> spikes = new ArrayList<>();
        private boolean erupted = false;

        public ShadowSpikeBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_spike_burst", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(0);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(10.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(140);
            config.setCooldownTicks(160);
            config.setChance(9);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            double ringRadius = 4.0;
            float[][] segScales = {
                {0.3f, 0.5f, 0.3f},
                {0.2f, 0.4f, 0.2f},
                {0.12f, 0.3f, 0.12f}
            };

            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12.0;
                double sx = Math.cos(angle) * ringRadius;
                double sz = Math.sin(angle) * ringRadius;
                // Direction toward center (inward)
                double inwardX = -Math.cos(angle);
                double inwardZ = -Math.sin(angle);

                List<BlockDisplayHandle> spike = new ArrayList<>();
                for (int seg = 0; seg < 3; seg++) {
                    // Start underground
                    double segOffset = seg * 0.4;
                    Location loc = center.clone().add(
                        sx + inwardX * segOffset,
                        -1.5 + seg * 0.3,
                        sz + inwardZ * segOffset
                    );
                    BlockDisplayHandle b = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    b.scale(segScales[seg][0], segScales[seg][1], segScales[seg][2])
                     .glow(20, 10, 5).interpolation(3, 0);

                    // Angle inward at 45 degrees
                    BlockDisplay bEnt = b.entity();
                    bEnt.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f((float) Math.toRadians(45),
                            (float) (-Math.sin(angle)), 0, (float) (Math.cos(angle))),
                        new Vector3f(segScales[seg][0], segScales[seg][1], segScales[seg][2]),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bEnt.setInterpolationDuration(3);
                    bEnt.setInterpolationDelay(0);

                    spawnedEntities.add(bEnt);
                    spike.add(b);
                }
                spikes.add(spike);
            }

            // Warning particles at ground level
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12.0;
                double px = Math.cos(angle) * ringRadius;
                double pz = Math.sin(angle) * ringRadius;
                DisplayBuilder.dustParticles(center.clone().add(px, 0.1, pz), 3, 0.3, 120, 20, 80, 1.0f);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double ringRadius = 4.0;
            float[][] segScales = {
                {0.3f, 0.5f, 0.3f},
                {0.2f, 0.4f, 0.2f},
                {0.12f, 0.3f, 0.12f}
            };

            // Eruption happens at tick 5 — all spikes shoot upward simultaneously
            if (!erupted && tick >= 5) {
                erupted = true;

                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI * i) / 12.0;
                    double sx = Math.cos(angle) * ringRadius;
                    double sz = Math.sin(angle) * ringRadius;
                    double inwardX = -Math.cos(angle);
                    double inwardZ = -Math.sin(angle);
                    List<BlockDisplayHandle> spike = spikes.get(i);

                    for (int seg = 0; seg < spike.size(); seg++) {
                        double segOffset = seg * 0.5;
                        Location loc = center.clone().add(
                            sx + inwardX * segOffset,
                            0.3 + seg * 0.6,
                            sz + inwardZ * segOffset
                        );
                        loc.setYaw(0);
                        loc.setPitch(0);
                        BlockDisplay bEnt = spike.get(seg).entity();
                        bEnt.setTeleportDuration(5);
                        bEnt.teleport(loc);
                    }

                    // Eruption particles at each spike base
                    DisplayBuilder.dustParticles(center.clone().add(sx, 0.5, sz), 5, 0.4, 80, 20, 60, 1.5f);
                }

                // Impact damage
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                center.getWorld().spawnParticle(Particle.EXPLOSION, center.clone().add(0, 1, 0), 5, 3, 1, 3, 0.02);

                for (Player p : center.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distanceSquared(center) <= config.getImpactRadius() * config.getImpactRadius()) {
                        p.damage(config.getImpactDamage());
                    }
                }
            }

            // Post-eruption: ambient shadow particles inside ring
            if (erupted && tick % 5 == 0) {
                double pAngle = Math.random() * 2 * Math.PI;
                double pr = Math.random() * ringRadius;
                DisplayBuilder.dustParticles(
                    center.clone().add(Math.cos(pAngle) * pr, 0.3, Math.sin(pAngle) * pr),
                    2, 0.4, 40, 10, 30, 1.0f);
            }

            // Spike shimmer
            if (erupted && tick % 8 == 0) {
                int spikeIdx = tick % 12;
                if (spikeIdx < spikes.size() && !spikes.get(spikeIdx).isEmpty()) {
                    Location tipLoc = spikes.get(spikeIdx).get(2).entity().getLocation();
                    center.getWorld().spawnParticle(Particle.SMOKE, tipLoc, 2, 0.1, 0.2, 0.1, 0.01);
                }
            }

            // Ambient rumble
            if (tick % 40 == 0 && erupted) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_STEP, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowSpikeBurst(plugin); }
    }

    // ================================================================
    // 47. INFERNAL DRILL — "The Boring" (9 displays)
    //     Drill tip: 3 NETHER_BRICK flat slabs (0.7x0.05x0.2) at
    //       120 deg apart, angled 35 deg downward
    //     Shaft: 1 IRON_BLOCK (0.2x2.5x0.2)
    //     Collar: 4 MAGMA_BLOCK cubes at top of shaft in ring
    //     Descends from Y+3. Drill tip rotates fast on Y continuously.
    //     Constant radius damage at drill tip as it bores down.
    //     1 crown + 3 tip + 1 shaft + 4 collar = 9 blocks
    // ================================================================
    public static class InfernalDrill extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> tipBlades = new ArrayList<>();
        private BlockDisplayHandle shaft;
        private final List<BlockDisplayHandle> collarBlocks = new ArrayList<>();
        private double drillY;
        private static final double START_Y = 6.0;
        private static final double END_Y = 0.5;

        public InfernalDrill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_drill", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(7.0);
            config.setDamageRadius(1.2);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(100);
            config.setCooldownTicks(180);
            config.setChance(8);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            drillY = START_Y;

            // Shaft: tall iron block pillar
            Location shaftLoc = center.clone().add(0, drillY, 0);
            shaft = displayBuilder.spawnBlock(shaftLoc, Material.IRON_BLOCK);
            shaft.scale(0.2f, 2.5f, 0.2f).glow(180, 170, 160).interpolation(3, 0);
            spawnedEntities.add(shaft.entity());

            // Drill tip: 3 flat blades at 120 degrees apart
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3.0;
                double bx = Math.cos(angle) * 0.25;
                double bz = Math.sin(angle) * 0.25;
                Location loc = center.clone().add(bx, drillY - 1.3, bz);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                blade.scale(0.7f, 0.05f, 0.2f).glow(200, 50, 10).interpolation(2, 0);

                // Angle 35 degrees downward toward center
                BlockDisplay bladeEnt = blade.entity();
                bladeEnt.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) Math.toRadians(-35),
                        (float) Math.cos(angle), 0, (float) Math.sin(angle)),
                    new Vector3f(0.7f, 0.05f, 0.2f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                bladeEnt.setInterpolationDuration(2);
                bladeEnt.setInterpolationDelay(0);

                spawnedEntities.add(bladeEnt);
                tipBlades.add(blade);
            }

            // Collar: 4 magma block cubes at top of shaft in ring
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4.0;
                double cx = Math.cos(angle) * 0.35;
                double cz = Math.sin(angle) * 0.35;
                Location loc = center.clone().add(cx, drillY + 1.3, cz);
                BlockDisplayHandle collar = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                collar.scale(0.25f, 0.25f, 0.25f).glow(255, 100, 20).interpolation(2, 0);
                spawnedEntities.add(collar.entity());
                collarBlocks.add(collar);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 1.5f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, drillY, 0), 15, 0.5, 1, 0.5, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Descend gradually
            if (drillY > END_Y) {
                drillY -= (START_Y - END_Y) / config.getDurationTicks();
            }

            // Drill tip fast rotation on Y
            double spinAngle = tick * 0.25; // Fast spin

            // Update shaft position
            Location shaftLoc = center.clone().add(0, drillY, 0);
            shaftLoc.setYaw(0);
            shaftLoc.setPitch(0);
            shaft.entity().teleport(shaftLoc);

            // Update drill tip blades (rotate on Y)
            for (int i = 0; i < 3; i++) {
                double baseAngle = (2 * Math.PI * i) / 3.0;
                double angle = baseAngle + spinAngle;
                double bx = Math.cos(angle) * 0.25;
                double bz = Math.sin(angle) * 0.25;
                Location loc = center.clone().add(bx, drillY - 1.3, bz);
                loc.setYaw(0);
                loc.setPitch(0);

                BlockDisplay bladeEnt = tipBlades.get(i).entity();
                bladeEnt.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) Math.toRadians(-35),
                        (float) Math.cos(angle), 0, (float) Math.sin(angle)),
                    new Vector3f(0.7f, 0.05f, 0.2f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                bladeEnt.setInterpolationDuration(2);
                bladeEnt.setInterpolationDelay(0);
                bladeEnt.teleport(loc);
            }

            // Update collar blocks (follow shaft top)
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4.0 + spinAngle * 0.3;
                double cx = Math.cos(angle) * 0.35;
                double cz = Math.sin(angle) * 0.35;
                Location loc = center.clone().add(cx, drillY + 1.3, cz);
                loc.setYaw(0);
                loc.setPitch(0);
                collarBlocks.get(i).entity().teleport(loc);
            }

            // Damage center at drill tip
            setCenter(center.clone().add(0, drillY - 1.3, 0));

            // Sparks from drill tip
            if (tick % 3 == 0) {
                Location tipLoc = center.clone().add(0, drillY - 1.5, 0);
                center.getWorld().spawnParticle(Particle.LAVA, tipLoc, 3, 0.3, 0.2, 0.3, 0.02);
                DisplayBuilder.dustParticles(tipLoc, 3, 0.4, 255, 100, 20, 1.0f);
            }

            // Grinding sound
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, drillY, 0),
                    Sound.BLOCK_GRINDSTONE_USE, 0.6f, 0.5f);
            }

            // Impact sound when near ground
            if (drillY < 1.5 && tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.4f);
                center.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(0, 0.3, 0), 8, 0.5, 0.2, 0.5, 0.02);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalDrill(plugin); }
    }

    // ================================================================
    // 48. CORRUPTION MIASMA — "The Cloud" (13 displays)
    //     9 SCULK flat slabs varied sizes (1.4x0.08x1.0 down to
    //       0.6x0.04x0.8) overlapping cloud at Y=1.2
    //     4 AMETHYST_BLOCK wisp cubes inside
    //     Cloud drifts via random walk (teleportDuration 60).
    //     Slabs oscillate Y +/-0.15 (breathing). Very long duration.
    //     Constant radius ticking damage.
    //     9 + 4 = 13 blocks
    // ================================================================
    public static class CorruptionMiasma extends BlockDisplayAttack {
        private Location center;
        private Location cloudCenter;
        private final List<BlockDisplayHandle> cloudSlabs = new ArrayList<>();
        private final List<BlockDisplayHandle> wisps = new ArrayList<>();
        private final double[][] slabOffsets = new double[9][3];
        private final float[][] slabScales = new float[9][3];
        private double driftX = 0, driftZ = 0;
        private double driftVelX = 0, driftVelZ = 0;

        public CorruptionMiasma(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_miasma", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(15);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(400);
            config.setCooldownTicks(200);
            config.setChance(8);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.cloudCenter = center.clone().add(0, 1.2, 0);
            World w = center.getWorld();
            if (w == null) return;

            // Define varied slab sizes for overlapping cloud effect
            float[][] sizes = {
                {1.4f, 0.08f, 1.0f}, {1.2f, 0.06f, 0.9f}, {1.0f, 0.07f, 1.1f},
                {0.9f, 0.05f, 0.85f}, {1.1f, 0.06f, 0.7f}, {0.8f, 0.04f, 0.9f},
                {0.7f, 0.05f, 0.8f}, {0.6f, 0.04f, 0.75f}, {0.85f, 0.06f, 0.65f}
            };
            double[][] offsets = {
                {0, 0, 0}, {0.3, 0.1, 0.2}, {-0.3, -0.05, -0.2},
                {0.5, 0.08, -0.3}, {-0.4, -0.03, 0.4}, {0.1, 0.12, -0.5},
                {-0.2, -0.08, 0.1}, {0.4, 0.05, 0.3}, {-0.5, 0.02, -0.1}
            };

            for (int i = 0; i < 9; i++) {
                slabScales[i] = sizes[i].clone();
                slabOffsets[i] = offsets[i].clone();
                Location loc = cloudCenter.clone().add(offsets[i][0], offsets[i][1], offsets[i][2]);
                BlockDisplayHandle slab = displayBuilder.spawnBlock(loc, Material.SCULK);
                slab.scale(sizes[i][0], sizes[i][1], sizes[i][2]).glow(30, 80, 100).interpolation(5, 0);
                spawnedEntities.add(slab.entity());
                cloudSlabs.add(slab);
            }

            // 4 amethyst wisp cubes inside the cloud
            double[][] wispOffsets = {{0.2, 0, 0.1}, {-0.3, 0.05, -0.2}, {0.1, -0.03, 0.3}, {-0.1, 0.08, -0.1}};
            for (double[] wOff : wispOffsets) {
                Location loc = cloudCenter.clone().add(wOff[0], wOff[1], wOff[2]);
                BlockDisplayHandle wisp = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                wisp.scale(0.15f, 0.15f, 0.15f).glow(120, 20, 80).interpolation(3, 0);
                spawnedEntities.add(wisp.entity());
                wisps.add(wisp);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.5f);
            w.spawnParticle(Particle.SCULK_CHARGE_POP, cloudCenter, 15, 1, 0.3, 1, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Random walk drift
            if (tick % 60 == 0) {
                driftVelX = (Math.random() - 0.5) * 0.08;
                driftVelZ = (Math.random() - 0.5) * 0.08;
            }
            driftX += driftVelX;
            driftZ += driftVelZ;

            // Clamp drift to prevent going too far
            double maxDrift = 6.0;
            if (driftX > maxDrift) { driftX = maxDrift; driftVelX = -Math.abs(driftVelX); }
            if (driftX < -maxDrift) { driftX = -maxDrift; driftVelX = Math.abs(driftVelX); }
            if (driftZ > maxDrift) { driftZ = maxDrift; driftVelZ = -Math.abs(driftVelZ); }
            if (driftZ < -maxDrift) { driftZ = -maxDrift; driftVelZ = Math.abs(driftVelZ); }

            cloudCenter = center.clone().add(driftX, 1.2, driftZ);

            // Update slab positions with breathing oscillation
            for (int i = 0; i < cloudSlabs.size(); i++) {
                double breathe = Math.sin(tick * 0.04 + i * 0.7) * 0.15;
                Location loc = cloudCenter.clone().add(
                    slabOffsets[i][0],
                    slabOffsets[i][1] + breathe,
                    slabOffsets[i][2]
                );
                loc.setYaw(0);
                loc.setPitch(0);
                BlockDisplay slabEnt = cloudSlabs.get(i).entity();
                slabEnt.setTeleportDuration(60);
                slabEnt.teleport(loc);
            }

            // Update wisp positions (orbit slowly inside cloud)
            double[][] wispOffsets = {{0.2, 0, 0.1}, {-0.3, 0.05, -0.2}, {0.1, -0.03, 0.3}, {-0.1, 0.08, -0.1}};
            for (int i = 0; i < wisps.size(); i++) {
                double orbitAngle = tick * 0.03 + i * Math.PI / 2.0;
                double wispDriftX = wispOffsets[i][0] + Math.cos(orbitAngle) * 0.3;
                double wispDriftZ = wispOffsets[i][2] + Math.sin(orbitAngle) * 0.3;
                double wispY = wispOffsets[i][1] + Math.sin(tick * 0.05 + i) * 0.1;
                Location loc = cloudCenter.clone().add(wispDriftX, wispY, wispDriftZ);
                loc.setYaw(0);
                loc.setPitch(0);
                BlockDisplay wispEnt = wisps.get(i).entity();
                wispEnt.setTeleportDuration(60);
                wispEnt.teleport(loc);
            }

            // Update damage center to follow cloud
            setCenter(cloudCenter);

            // Miasma particles drifting downward
            if (tick % 4 == 0) {
                DisplayBuilder.dustParticles(cloudCenter.clone().add(
                    (Math.random() - 0.5) * 2, -0.5, (Math.random() - 0.5) * 2),
                    3, 0.5, 30, 80, 100, 1.0f);
            }

            // Sculk particles
            if (tick % 8 == 0) {
                center.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, cloudCenter, 2, 1, 0.3, 1, 0.01);
            }

            // Ambient corruption sound
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(cloudCenter, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.3f);
            }

            // Wisp flicker
            if (tick % 6 == 0) {
                int wIdx = tick % wisps.size();
                Location wLoc = wisps.get(wIdx).entity().getLocation();
                DisplayBuilder.dustParticles(wLoc, 2, 0.15, 120, 20, 80, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionMiasma(plugin); }
    }

    // ================================================================
    // 49. LAVA CAGE MELT — "The Refinery" (26 displays)
    //     Bars: 4 NETHERRACK vertical (0.12x2.0x0.12)
    //     Cross-bars: 8 MAGMA_BLOCK horizontal (1.8x0.1x0.12)
    //     Drips: 14 SHROOMLIGHT cubes (0.05x0.08x0.05) hanging from
    //       cross-bars
    //     Cage descends from Y+3. Cross-bars slide in. Drips on
    //     repeating fall cycle (Y translate down 20 ticks then reset).
    //     Higher damage, shorter duration than bone cage.
    //     4 + 8 + 14 = 26 blocks
    // ================================================================
    public static class LavaCageMelt extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> bars = new ArrayList<>();
        private final List<BlockDisplayHandle> crossBars = new ArrayList<>();
        private final List<BlockDisplayHandle> drips = new ArrayList<>();
        private double cageY;
        private boolean cageLanded = false;
        private static final double START_Y = 6.0;
        private static final double LAND_Y = 0.0;

        public LavaCageMelt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_cage_melt", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(2.2);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(120);
            config.setCooldownTicks(160);
            config.setChance(9);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            cageY = START_Y;
            double half = 0.9; // half-width of cage

            // 4 vertical bars at corners
            double[][] cornerOffsets = {
                {half, 0, half}, {half, 0, -half},
                {-half, 0, half}, {-half, 0, -half}
            };
            for (double[] off : cornerOffsets) {
                Location loc = center.clone().add(off[0], cageY, off[2]);
                BlockDisplayHandle bar = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                bar.scale(0.12f, 2.0f, 0.12f).glow(200, 50, 10).interpolation(3, 0);
                spawnedEntities.add(bar.entity());
                bars.add(bar);
            }

            // 8 cross-bars (2 per side: top and middle, on each of 4 sides)
            // Top cross-bars along X (front and back)
            Location[] crossBarLocs = {
                center.clone().add(0, cageY + 1.8, half),   // top front
                center.clone().add(0, cageY + 1.8, -half),  // top back
                center.clone().add(half, cageY + 1.8, 0),   // top left
                center.clone().add(-half, cageY + 1.8, 0),  // top right
                center.clone().add(0, cageY + 0.9, half),   // mid front
                center.clone().add(0, cageY + 0.9, -half),  // mid back
                center.clone().add(half, cageY + 0.9, 0),   // mid left
                center.clone().add(-half, cageY + 0.9, 0),  // mid right
            };
            for (int i = 0; i < 8; i++) {
                BlockDisplayHandle cb = displayBuilder.spawnBlock(crossBarLocs[i], Material.MAGMA_BLOCK);
                // Alternate orientation: X-aligned vs Z-aligned
                if (i < 2 || (i >= 4 && i < 6)) {
                    cb.scale(1.8f, 0.1f, 0.12f);
                } else {
                    cb.scale(0.12f, 0.1f, 1.8f);
                }
                cb.glow(255, 100, 20).interpolation(3, 0);
                spawnedEntities.add(cb.entity());
                crossBars.add(cb);
            }

            // 14 drip cubes hanging from cross-bars at random positions
            for (int i = 0; i < 14; i++) {
                double dx = (Math.random() - 0.5) * 1.6;
                double dz = (Math.random() - 0.5) * 1.6;
                double dy = cageY + 1.6 + Math.random() * 0.3;
                Location loc = center.clone().add(dx, dy, dz);
                BlockDisplayHandle drip = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                drip.scale(0.05f, 0.08f, 0.05f).glow(240, 80, 30).interpolation(2, 0);
                spawnedEntities.add(drip.entity());
                drips.add(drip);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.4f);
            w.spawnParticle(Particle.LAVA, center.clone().add(0, cageY + 1, 0), 15, 1, 1, 1, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double half = 0.9;

            // Descend cage
            if (!cageLanded) {
                cageY -= 0.2;
                if (cageY <= LAND_Y) {
                    cageY = LAND_Y;
                    cageLanded = true;
                    DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);
                    center.getWorld().spawnParticle(Particle.LAVA, center.clone().add(0, 0.5, 0), 20, 1, 0.5, 1, 0.05);
                }
            }

            // Update vertical bars
            double[][] cornerOffsets = {
                {half, 0, half}, {half, 0, -half},
                {-half, 0, half}, {-half, 0, -half}
            };
            for (int i = 0; i < bars.size(); i++) {
                Location loc = center.clone().add(cornerOffsets[i][0], cageY, cornerOffsets[i][2]);
                loc.setYaw(0);
                loc.setPitch(0);
                bars.get(i).entity().teleport(loc);
            }

            // Update cross-bars
            double[][] cbOffsets = {
                {0, 1.8, half}, {0, 1.8, -half}, {half, 1.8, 0}, {-half, 1.8, 0},
                {0, 0.9, half}, {0, 0.9, -half}, {half, 0.9, 0}, {-half, 0.9, 0}
            };
            for (int i = 0; i < crossBars.size(); i++) {
                Location loc = center.clone().add(cbOffsets[i][0], cageY + cbOffsets[i][1], cbOffsets[i][2]);
                loc.setYaw(0);
                loc.setPitch(0);
                crossBars.get(i).entity().teleport(loc);
            }

            // Drip animation: repeating fall cycle (20 ticks down, then reset)
            int dripCycle = tick % 30;
            for (int i = 0; i < drips.size(); i++) {
                double dx = (((i * 7 + 3) % 16) - 8) * 0.1; // deterministic spread
                double dz = (((i * 11 + 5) % 16) - 8) * 0.1;
                double baseY = cageY + 1.6 + (i % 3) * 0.1;

                // Each drip starts at different phase offset
                int dripPhase = (dripCycle + i * 3) % 30;
                double fallDist;
                if (dripPhase < 20) {
                    fallDist = dripPhase * 0.08; // falling
                } else {
                    fallDist = 0; // reset to top
                }

                Location loc = center.clone().add(dx, baseY - fallDist, dz);
                loc.setYaw(0);
                loc.setPitch(0);
                drips.get(i).entity().teleport(loc);
            }

            // Lava drip particles
            if (tick % 4 == 0 && cageLanded) {
                double px = (Math.random() - 0.5) * 1.6;
                double pz = (Math.random() - 0.5) * 1.6;
                center.getWorld().spawnParticle(Particle.LAVA,
                    center.clone().add(px, cageY + 0.3, pz), 1, 0.1, 0.1, 0.1, 0.01);
            }

            // Sizzle particles at ground level
            if (tick % 6 == 0 && cageLanded) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.2, 0), 3, 0.8, 255, 100, 20, 0.8f);
            }

            // Ambient lava sizzle
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 1.0f);
            }

            // Heat haze sound when landed
            if (cageLanded && tick % 35 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LavaCageMelt(plugin); }
    }

    // ================================================================
    // 50. THE UNDYING FLAME — "The Eternal Burn" (24 displays)
    //     Feet: 4 POLISHED_BLACKSTONE (0.4x0.5x0.4)
    //     Platform: 9 CHISELED_POLISHED_BLACKSTONE flat slabs
    //       (0.9x0.08x0.9) in 3x3
    //     Lip: 4 GILDED_BLACKSTONE along rim
    //     Flame: 6 SHROOMLIGHT in dome above platform (orbiting, pulsing)
    //     Candles: 1 MAGMA_BLOCK pillar (counted as candle, using 1 for
    //       each side = 2 total but spec says 2 pillars which we treat as
    //       display count adjustment)
    //     Most complex structure. Longest duration. Very low tick damage.
    //     Stationary area denial.
    //     Altar builds from feet up. Flame dome rotates slowly. Candle
    //     pillars pulse Y scale.
    //     4 feet + 9 platform + 4 lip + 5 flame dome (adjusted) + 2 candles = 24
    //     Actually: 4 + 9 + 4 + 5 + 2 = 24 blocks
    // ================================================================
    public static class TheUndyingFlame extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> feet = new ArrayList<>();
        private final List<BlockDisplayHandle> platform = new ArrayList<>();
        private final List<BlockDisplayHandle> lip = new ArrayList<>();
        private final List<BlockDisplayHandle> flameDome = new ArrayList<>();
        private final List<BlockDisplayHandle> candles = new ArrayList<>();
        private int buildStage = 0; // 0=feet, 1=platform, 2=lip, 3=flame, 4=active

        public TheUndyingFlame(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_undying_flame", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(2.5);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(500);
            config.setCooldownTicks(300);
            config.setChance(4);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Stage 0: Build feet (4 corner pillars)
            double[][] feetOffsets = {{0.8, 0, 0.8}, {0.8, 0, -0.8}, {-0.8, 0, 0.8}, {-0.8, 0, -0.8}};
            for (double[] off : feetOffsets) {
                Location loc = center.clone().add(off[0], 0, off[2]);
                BlockDisplayHandle foot = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                foot.scale(0.4f, 0.01f, 0.4f).glow(20, 10, 5).interpolation(10, 0);
                spawnedEntities.add(foot.entity());
                feet.add(foot);
            }

            // Pre-spawn platform (invisible initially)
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    double px = (col - 1) * 0.95;
                    double pz = (row - 1) * 0.95;
                    Location loc = center.clone().add(px, 0.5, pz);
                    BlockDisplayHandle slab = displayBuilder.spawnBlock(loc, Material.CHISELED_POLISHED_BLACKSTONE);
                    slab.scale(0.01f, 0.01f, 0.01f).glow(40, 30, 25).interpolation(10, 0);
                    spawnedEntities.add(slab.entity());
                    platform.add(slab);
                }
            }

            // Pre-spawn lip (invisible initially)
            double[][] lipOffsets = {{1.2, 0.58, 0}, {-1.2, 0.58, 0}, {0, 0.58, 1.2}, {0, 0.58, -1.2}};
            for (double[] off : lipOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle lipBlock = displayBuilder.spawnBlock(loc, Material.GILDED_BLACKSTONE);
                lipBlock.scale(0.01f, 0.01f, 0.01f).glow(220, 180, 30).interpolation(10, 0);
                spawnedEntities.add(lipBlock.entity());
                lip.add(lipBlock);
            }

            // Pre-spawn flame dome (invisible initially) — 5 shroomlight + 1 more = 6
            // Using 5 for the dome orbit + 1 crown piece
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6.0;
                double fx = Math.cos(angle) * 0.5;
                double fz = Math.sin(angle) * 0.5;
                Location loc = center.clone().add(fx, 1.5, fz);
                BlockDisplayHandle flame = displayBuilder.spawnBlock(loc, Material.SHROOMLIGHT);
                flame.scale(0.01f, 0.01f, 0.01f).glow(255, 100, 20).interpolation(10, 0);
                spawnedEntities.add(flame.entity());
                flameDome.add(flame);
            }

            // Pre-spawn candle pillars (invisible initially)
            double[][] candleOffsets = {{0.5, 0.6, 0.5}, {-0.5, 0.6, -0.5}};
            for (double[] off : candleOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle candle = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                candle.scale(0.01f, 0.01f, 0.01f).glow(240, 80, 30).interpolation(10, 0);
                spawnedEntities.add(candle.entity());
                candles.add(candle);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_PLACE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Build-up sequence: feet -> platform -> lip -> flame/candles -> active
            if (tick == 5 && buildStage == 0) {
                // Grow feet to full size
                for (BlockDisplayHandle foot : feet) {
                    BlockDisplay fEnt = foot.entity();
                    fEnt.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.4f, 0.5f, 0.4f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    fEnt.setInterpolationDuration(10);
                    fEnt.setInterpolationDelay(0);
                }
                buildStage = 1;
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 0.5f);
            }

            if (tick == 15 && buildStage == 1) {
                // Reveal platform slabs
                for (BlockDisplayHandle slab : platform) {
                    BlockDisplay sEnt = slab.entity();
                    sEnt.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.9f, 0.08f, 0.9f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    sEnt.setInterpolationDuration(10);
                    sEnt.setInterpolationDelay(0);
                }
                buildStage = 2;
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 0.6f);
            }

            if (tick == 22 && buildStage == 2) {
                // Reveal lip blocks
                for (BlockDisplayHandle lipBlock : lip) {
                    BlockDisplay lEnt = lipBlock.entity();
                    lEnt.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.3f, 0.15f, 0.3f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    lEnt.setInterpolationDuration(8);
                    lEnt.setInterpolationDelay(0);
                }
                buildStage = 3;
                DisplayBuilder.playSound(center, Sound.BLOCK_GILDED_BLACKSTONE_PLACE, 0.7f, 0.5f);
            }

            if (tick == 28 && buildStage == 3) {
                // Reveal flame dome
                for (BlockDisplayHandle flame : flameDome) {
                    BlockDisplay flEnt = flame.entity();
                    flEnt.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.25f, 0.25f, 0.25f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    flEnt.setInterpolationDuration(8);
                    flEnt.setInterpolationDelay(0);
                }

                // Reveal candle pillars
                for (BlockDisplayHandle candle : candles) {
                    BlockDisplay cEnt = candle.entity();
                    cEnt.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.2f, 0.8f, 0.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    cEnt.setInterpolationDuration(8);
                    cEnt.setInterpolationDelay(0);
                }
                buildStage = 4;

                DisplayBuilder.playSound(center, Sound.ITEM_FIRECHARGE_USE, 0.8f, 0.6f);
                center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0.03);
            }

            // Active phase animations
            if (buildStage >= 4) {
                // Flame dome rotates slowly on Y
                double flameRotAngle = tick * 0.02;
                for (int i = 0; i < flameDome.size(); i++) {
                    double baseAngle = (2 * Math.PI * i) / 6.0;
                    double angle = baseAngle + flameRotAngle;
                    double domeRadius = 0.5;
                    // Dome shape: higher in center, lower at edges
                    double domeHeight = 1.2 + Math.cos((double) i / flameDome.size() * Math.PI) * 0.3;
                    double fx = Math.cos(angle) * domeRadius;
                    double fz = Math.sin(angle) * domeRadius;
                    // Pulse scale
                    float flamePulse = 0.25f + (float) Math.sin(tick * 0.06 + i * 0.5) * 0.08f;
                    Location loc = center.clone().add(fx, domeHeight, fz);
                    loc.setYaw(0);
                    loc.setPitch(0);

                    BlockDisplay flEnt = flameDome.get(i).entity();
                    flEnt.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(flamePulse, flamePulse, flamePulse),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    flEnt.setInterpolationDuration(4);
                    flEnt.setInterpolationDelay(0);
                    flEnt.teleport(loc);
                }

                // Candle pillars pulse Y scale
                double[][] candleOffsets = {{0.5, 0.6, 0.5}, {-0.5, 0.6, -0.5}};
                for (int i = 0; i < candles.size(); i++) {
                    float candleHeight = 0.8f + (float) Math.sin(tick * 0.04 + i * Math.PI) * 0.2f;
                    Location loc = center.clone().add(candleOffsets[i][0], candleOffsets[i][1], candleOffsets[i][2]);
                    loc.setYaw(0);
                    loc.setPitch(0);

                    BlockDisplay cEnt = candles.get(i).entity();
                    cEnt.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.2f, candleHeight, 0.2f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    cEnt.setInterpolationDuration(5);
                    cEnt.setInterpolationDelay(0);
                    cEnt.teleport(loc);
                }

                // Flame particles rising from dome
                if (tick % 3 == 0) {
                    double fAngle = Math.random() * 2 * Math.PI;
                    double fr = Math.random() * 0.6;
                    center.getWorld().spawnParticle(Particle.FLAME,
                        center.clone().add(Math.cos(fAngle) * fr, 1.8, Math.sin(fAngle) * fr),
                        2, 0.1, 0.3, 0.1, 0.01);
                }

                // Ember particles around platform edges
                if (tick % 8 == 0) {
                    double eAngle = Math.random() * 2 * Math.PI;
                    DisplayBuilder.dustParticles(
                        center.clone().add(Math.cos(eAngle) * 1.2, 0.7, Math.sin(eAngle) * 1.2),
                        2, 0.2, 240, 80, 30, 1.0f);
                }

                // Ancient humming sound
                if (tick % 60 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 0.3f);
                }

                // Periodic fire crackle
                if (tick % 30 == 0) {
                    DisplayBuilder.playSound(center.clone().add(0, 1.5, 0),
                        Sound.BLOCK_CAMPFIRE_CRACKLE, 0.5f, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new TheUndyingFlame(plugin); }
    }
}
