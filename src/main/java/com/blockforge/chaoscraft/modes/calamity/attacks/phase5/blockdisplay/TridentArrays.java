package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 5E Block Display -- GROUP 2: TRIDENT ARRAYS (#9-18)
 * 10 trident-themed structures orbiting the arena at various altitudes.
 * Supreme Calamitas wields tridents as her signature weapon -- these
 * arrays represent her arsenal, from inner defense rings to outer
 * threat perimeters.
 *
 * NOTE: Tridents are implemented as END_ROD BlockDisplay entities scaled
 * into elongated rod shapes to approximate trident silhouettes, since
 * ItemDisplay is not used in the block display attack system. The
 * glowing end rod material provides the correct luminous weapon aesthetic.
 *
 * Structures:
 *  #9   InnerTridentRing       -- 8 tridents at radius 8, Y+5, counterclockwise 3 deg/tick
 *  #10  MiddleTridentRing      -- 12 tridents at radius 14, Y+12, clockwise 2 deg/tick
 *  #11  OuterTridentRing       -- 16 tridents at radius 22, Y+20, counterclockwise 1.5 deg/tick
 *  #12  TridentSpineCluster    -- 5 stacked vertical tridents, 12N of center, dormant
 *  #13  TridentHalo            -- 6 oversized tridents at Y+30 above her
 *  #14  TridentWallEast        -- 8 tridents in 4x2 grid, 25 east, aimed center
 *  #15  TridentWallWest        -- Mirror of #14, 25 west
 *  #16  SpiralTridentTower     -- 18 tridents in ascending helix, 20N
 *  #17  TridentPendulum        -- Single oversized swinging trident, 15E
 *  #18  RotatingTridentCrown   -- 20 fast tridents at radius 18, Y+8, Phase 4 only
 *
 * Rules applied:
 * - NO status effects
 * - Calamitas palette: crimson(200,0,50), brimstone(255,100,0), soul blue(0,150,255)
 * - Damage HP 4.0-14.0
 * - AxisAngle4f ONLY (never Quaternionf)
 * - Static DisplayBuilder methods
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Location center = getCenter(); if (center == null) return; EVERY onTick
 */
public final class TridentArrays {

    private TridentArrays() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new InnerTridentRing(plugin));
        registry.register(new MiddleTridentRing(plugin));
        registry.register(new OuterTridentRing(plugin));
        registry.register(new TridentSpineCluster(plugin));
        registry.register(new TridentHalo(plugin));
        registry.register(new TridentWallEast(plugin));
        registry.register(new TridentWallWest(plugin));
        registry.register(new SpiralTridentTower(plugin));
        registry.register(new TridentPendulum(plugin));
        registry.register(new RotatingTridentCrown(plugin));
    }

    // ================================================================
    // #9 -- INNER TRIDENT RING
    // 8 end rod "tridents" at radius 8, Y+5. Orbit counterclockwise
    // at 3 deg/tick (120-tick revolution). CRIT + ELECTRIC_SPARK trails.
    // ================================================================
    public static class InnerTridentRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tridents = new ArrayList<>();
        private final double[] phaseOffsets = new double[8];
        private static final double RADIUS = 8.0;
        private static final double Y_ALT = 5.0;

        public InnerTridentRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_inner_trident_ring", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            java.util.Random rng = new java.util.Random(42);
            for (int i = 0; i < 8; i++) {
                double baseAngle = (2.0 * Math.PI * i) / 8.0;
                // Slight phase jitter for organic feel
                phaseOffsets[i] = baseAngle + Math.toRadians(rng.nextDouble() * 10.0 - 5.0);

                double px = Math.cos(phaseOffsets[i]) * RADIUS;
                double pz = Math.sin(phaseOffsets[i]) * RADIUS;
                Location loc = center.clone().add(px, Y_ALT, pz);

                BlockDisplayHandle trident = displayBuilder.spawnBlock(loc, Material.END_ROD);
                // Elongated rod shape: trident silhouette
                trident.scale(0.15f, 1.5f, 0.15f);
                // Blade pointing outward at 15 deg upward
                float tiltAngle = (float) Math.toRadians(75); // 90 - 15 = 75 from vertical
                float axisX = (float) -Math.sin(phaseOffsets[i]);
                float axisZ = (float) Math.cos(phaseOffsets[i]);
                trident.rotate(tiltAngle, axisX, 0, axisZ);
                trident.glow(240, 240, 255).interpolation(2, 0);
                tridents.add(trident);
                spawnedEntities.add(trident.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Orbit at 3 deg/tick counterclockwise
            double orbitAngle = Math.toRadians(ticksAlive * 3.0);

            for (int i = 0; i < tridents.size(); i++) {
                double angle = phaseOffsets[i] + orbitAngle;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;

                Location newLoc = center.clone().add(px, Y_ALT, pz);
                tridents.get(i).entity().teleport(newLoc);

                // Update rotation to face outward
                float tiltAngle = (float) Math.toRadians(75);
                float axisX = (float) -Math.sin(angle);
                float axisZ = (float) Math.cos(angle);
                tridents.get(i).rotate(tiltAngle, axisX, 0, axisZ);
                tridents.get(i).interpolation(2, 0);

                // CRIT trail from blade tip
                if (ticksAlive % 4 == i % 4) {
                    Location tipLoc = newLoc.clone().add(Math.cos(angle) * 0.8, 0.6, Math.sin(angle) * 0.8);
                    center.getWorld().spawnParticle(Particle.CRIT, tipLoc, 2, 0.1, 0.05, 0.1, 0.02);
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, tipLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Revolution sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, Y_ALT, 0), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InnerTridentRing(plugin); }
    }

    // ================================================================
    // #10 -- MIDDLE TRIDENT RING
    // 12 tridents at radius 14, Y+12. Clockwise orbit at 2 deg/tick.
    // Blades point inward-down at 25 deg. Crimson dust trailing.
    // ================================================================
    public static class MiddleTridentRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tridents = new ArrayList<>();
        private static final int COUNT = 12;
        private static final double RADIUS = 14.0;
        private static final double Y_ALT = 12.0;

        public MiddleTridentRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_middle_trident_ring", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(7.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double angle = (2.0 * Math.PI * i) / COUNT;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;
                Location loc = center.clone().add(px, Y_ALT, pz);

                BlockDisplayHandle trident = displayBuilder.spawnBlock(loc, Material.END_ROD);
                trident.scale(0.12f, 1.3f, 0.12f);
                // Blade pointing inward and down at 25 deg from horizontal
                float tiltAngle = (float) Math.toRadians(25);
                float axisX = (float) Math.sin(angle);
                float axisZ = (float) -Math.cos(angle);
                trident.rotate(tiltAngle, axisX, 0, axisZ);
                trident.glow(200, 0, 50).interpolation(2, 0);
                tridents.add(trident);
                spawnedEntities.add(trident.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.3f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Clockwise orbit at 2 deg/tick
            double orbitAngle = -Math.toRadians(ticksAlive * 2.0);

            for (int i = 0; i < tridents.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / COUNT;
                double angle = baseAngle + orbitAngle;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;

                Location newLoc = center.clone().add(px, Y_ALT, pz);
                tridents.get(i).entity().teleport(newLoc);

                // Rotation: inward and down
                float tiltAngle = (float) Math.toRadians(25);
                float axisX = (float) Math.sin(angle);
                float axisZ = (float) -Math.cos(angle);
                tridents.get(i).rotate(tiltAngle, axisX, 0, axisZ);
                tridents.get(i).interpolation(2, 0);

                // Crimson dust trail
                if (ticksAlive % 5 == i % 5) {
                    Location trailLoc = newLoc.clone().add(-Math.cos(angle) * 0.5, -0.3, -Math.sin(angle) * 0.5);
                    DisplayBuilder.crimsonDust(trailLoc, 2, 0.3);
                }
            }

            // Sound every 180 ticks
            if (ticksAlive % 180 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, Y_ALT, 0), Sound.BLOCK_STONE_PLACE, 0.15f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MiddleTridentRing(plugin); }
    }

    // ================================================================
    // #11 -- OUTER TRIDENT RING
    // 16 tridents at radius 22, Y+20. Counterclockwise 1.5 deg/tick.
    // Blades point straight up. Phase-in over 5 seconds.
    // ================================================================
    public static class OuterTridentRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tridents = new ArrayList<>();
        private static final int COUNT = 16;
        private static final double RADIUS = 22.0;
        private static final double Y_ALT = 20.0;

        public OuterTridentRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_outer_trident_ring", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double angle = (2.0 * Math.PI * i) / COUNT;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;
                Location loc = center.clone().add(px, Y_ALT, pz);

                BlockDisplayHandle trident = displayBuilder.spawnBlock(loc, Material.END_ROD);
                // Start small, grow in over 100 ticks (5 seconds)
                trident.scale(0.1f, 0.1f, 0.1f);
                trident.glow(240, 240, 255).interpolation(100, 0);
                tridents.add(trident);
                spawnedEntities.add(trident.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.3f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Phase-in animation during first 100 ticks
            if (ticksAlive <= 100) {
                float scaleFactor = ticksAlive / 100.0f;
                float s = 0.12f * scaleFactor;
                float h = 1.2f * scaleFactor;
                for (BlockDisplayHandle t : tridents) {
                    t.scale(s, h, s);
                }
            }

            // Orbit at 1.5 deg/tick counterclockwise
            double orbitAngle = Math.toRadians(ticksAlive * 1.5);

            for (int i = 0; i < tridents.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / COUNT;
                double angle = baseAngle + orbitAngle;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;

                Location newLoc = center.clone().add(px, Y_ALT, pz);
                tridents.get(i).entity().teleport(newLoc);

                // Particles: magenta spell + end rod sparkle
                if (ticksAlive % 3 == i % 3) {
                    center.getWorld().spawnParticle(Particle.WITCH, newLoc.clone().add(0, 0.5, 0), 2, 0.1, 0.1, 0.1, 0.02);
                    center.getWorld().spawnParticle(Particle.END_ROD, newLoc.clone().add(0, 0.8, 0), 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Sound
            if (ticksAlive % 60 == 0 && ticksAlive > 100) {
                DisplayBuilder.playSound(center.clone().add(0, Y_ALT, 0), Sound.ENTITY_WITHER_SHOOT, 0.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OuterTridentRing(plugin); }
    }

    // ================================================================
    // #12 -- TRIDENT SPINE CLUSTER
    // 5 stacked vertical end rod "tridents" at 12N of center.
    // Static, dormant display. Gentle spiral twist. Activates at Phase 4
    // with Y oscillation. CRIT at 2/sec from tips.
    // ================================================================
    public static class TridentSpineCluster extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spine = new ArrayList<>();
        private static final int COUNT = 5;
        private static final double[] Y_POSITIONS = {3, 5, 7, 9, 11};

        public TridentSpineCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_spine", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location spineBase = center.clone().add(0, 0, -12);

            for (int i = 0; i < COUNT; i++) {
                Location loc = spineBase.clone().add(0, Y_POSITIONS[i], 0);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(loc, Material.END_ROD);
                trident.scale(0.1f, 1.0f, 0.1f);
                // Each tilted 5 deg clockwise from the one below in YZ plane
                float twistAngle = (float) Math.toRadians(5 * i);
                trident.rotate(twistAngle, 0, 0, 1);
                trident.glow(240, 240, 255);
                spine.add(trident);
                spawnedEntities.add(trident.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location spineBase = center.clone().add(0, 0, -12);

            // Faint CRIT from each tip
            if (ticksAlive % 10 == 0) {
                for (int i = 0; i < COUNT; i++) {
                    Location tipLoc = spineBase.clone().add(0, Y_POSITIONS[i] + 0.5, -0.3);
                    center.getWorld().spawnParticle(Particle.CRIT, tipLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Phase 4 activation: oscillation (triggered after tick 3000 as proxy)
            if (ticksAlive > 3000) {
                float yDisp = 0.3f * (float) Math.sin(ticksAlive * 2.0 * Math.PI / 30.0);
                for (int i = 0; i < spine.size(); i++) {
                    Location newLoc = spineBase.clone().add(0, Y_POSITIONS[i] + yDisp, 0);
                    spine.get(i).entity().teleport(newLoc);
                }

                // Increased CRIT during activation
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < COUNT; i++) {
                        Location tipLoc = spineBase.clone().add(0, Y_POSITIONS[i] + 0.5 + yDisp, -0.3);
                        center.getWorld().spawnParticle(Particle.CRIT, tipLoc, 3, 0.1, 0.05, 0.1, 0.02);
                    }
                }

                // Sound during oscillation
                if (ticksAlive % 40 == 0) {
                    DisplayBuilder.playSound(spineBase, Sound.BLOCK_STONE_PLACE, 0.1f, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentSpineCluster(plugin); }
    }

    // ================================================================
    // #13 -- TRIDENT HALO
    // 6 oversized end rod tridents in flat ring at Y+30 above her.
    // Orbit 4 deg/tick clockwise. One fires downward every 200 ticks.
    // FLAME + gold glow from each.
    // ================================================================
    public static class TridentHalo extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> haloTridents = new ArrayList<>();
        private static final int COUNT = 6;
        private static final double RADIUS = 4.0;
        private static final double Y_ALT = 30.0;
        private int nextFireIndex = 0;

        public TridentHalo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_halo", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double angle = (2.0 * Math.PI * i) / COUNT;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;
                Location loc = center.clone().add(px, Y_ALT, pz);

                BlockDisplayHandle trident = displayBuilder.spawnBlock(loc, Material.END_ROD);
                trident.scale(0.2f, 2.0f, 0.2f);
                // Horizontal orientation, blade outward
                float rotAngle = (float) (angle + Math.PI / 2.0);
                trident.rotate((float) Math.toRadians(90), 1, 0, 0);
                trident.glow(255, 100, 0).interpolation(2, 0);
                haloTridents.add(trident);
                spawnedEntities.add(trident.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, Y_ALT, 0), Sound.ENTITY_WITHER_SHOOT, 0.4f, 0.85f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Orbit at 4 deg/tick clockwise
            double orbitAngle = -Math.toRadians(ticksAlive * 4.0);

            for (int i = 0; i < haloTridents.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / COUNT;
                double angle = baseAngle + orbitAngle;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;

                Location newLoc = center.clone().add(px, Y_ALT, pz);
                haloTridents.get(i).entity().teleport(newLoc);

                // FLAME + gold particle trail
                if (ticksAlive % 5 == i % 5) {
                    center.getWorld().spawnParticle(Particle.FLAME, newLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    DisplayBuilder.dustParticles(newLoc, 1, 0.1, 255, 200, 50, 0.8f);
                }
            }

            // Fire event: one trident descends every 200 ticks (visual only)
            if (ticksAlive % 200 == 0 && ticksAlive > 0) {
                int fireIdx = nextFireIndex % COUNT;
                nextFireIndex++;

                // CRIT burst at the trident's position before "firing"
                BlockDisplayHandle fired = haloTridents.get(fireIdx);
                Location fireLoc = fired.entity().getLocation();
                center.getWorld().spawnParticle(Particle.CRIT, fireLoc, 20, 0.5, 0.5, 0.5, 0.1);
                DisplayBuilder.playSound(fireLoc, Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.85f);
            }

            // Sound every 200 ticks
            if (ticksAlive % 200 == 100) {
                DisplayBuilder.playSound(center.clone().add(0, Y_ALT, 0), Sound.ENTITY_WITHER_SHOOT, 0.3f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentHalo(plugin); }
    }

    // ================================================================
    // #14 -- TRIDENT WALL EAST
    // 8 end rod tridents in a 4x2 grid, 25E of center, Y+4 to Y+18.
    // All aimed west (at center). ELECTRIC_SPARK + SPELL_WITCH.
    // Random vibration every 20 ticks.
    // ================================================================
    public static class TridentWallEast extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallTridents = new ArrayList<>();
        private final List<Location> wallPositions = new ArrayList<>();
        private static final int COLUMNS = 4;
        private static final int ROWS = 2;

        public TridentWallEast(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_wall_east", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(7.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int col = 0; col < COLUMNS; col++) {
                for (int row = 0; row < ROWS; row++) {
                    double z = -4.5 + col * 3.0;
                    double y = 7.0 + row * 7.0;
                    Location loc = center.clone().add(25, y, z);
                    wallPositions.add(loc.clone());

                    BlockDisplayHandle trident = displayBuilder.spawnBlock(loc, Material.END_ROD);
                    trident.scale(0.12f, 1.3f, 0.12f);
                    // Aimed west: rotate 90 deg on Z to point horizontally toward -X
                    trident.rotate((float) Math.toRadians(90), 0, 0, 1);
                    trident.glow(240, 240, 255);
                    wallTridents.add(trident);
                    spawnedEntities.add(trident.entity());
                }
            }

            DisplayBuilder.playSound(center.clone().add(25, 10, 0), Sound.BLOCK_STONE_PLACE, 0.3f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Random vibration every 20 ticks
            if (ticksAlive % 20 == 0) {
                java.util.Random rng = new java.util.Random(ticksAlive);
                int vibrateIdx = rng.nextInt(wallTridents.size());
                BlockDisplayHandle vibrating = wallTridents.get(vibrateIdx);
                Location baseLoc = wallPositions.get(vibrateIdx);

                // Quick X offset for vibration effect
                float xJitter = 0.15f * (rng.nextFloat() * 2 - 1);
                Location jitterLoc = baseLoc.clone().add(xJitter, 0, 0);
                vibrating.entity().teleport(jitterLoc);

                DisplayBuilder.playSound(jitterLoc, Sound.BLOCK_STONE_PLACE, 0.1f, 1.1f);
            }

            // Return vibrated tridents to rest position
            if (ticksAlive % 20 == 8) {
                for (int i = 0; i < wallTridents.size(); i++) {
                    wallTridents.get(i).entity().teleport(wallPositions.get(i));
                }
            }

            // ELECTRIC_SPARK from blade tips + SPELL_WITCH from handles
            if (ticksAlive % 7 == 0) {
                for (int i = 0; i < wallPositions.size(); i++) {
                    Location pos = wallPositions.get(i);
                    // Blade tip: west side
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pos.clone().add(-0.8, 0, 0), 1, 0.05, 0.05, 0.05, 0.01);
                    // Handle: east side
                    center.getWorld().spawnParticle(Particle.WITCH, pos.clone().add(0.8, 0, 0), 1, 0.05, 0.05, 0.05, 0.005);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentWallEast(plugin); }
    }

    // ================================================================
    // #15 -- TRIDENT WALL WEST
    // Mirror of #14 on the west side. Blades aim east (at center).
    // Vibration offset from east wall by 8 ticks.
    // ================================================================
    public static class TridentWallWest extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallTridents = new ArrayList<>();
        private final List<Location> wallPositions = new ArrayList<>();
        private static final int COLUMNS = 4;
        private static final int ROWS = 2;

        public TridentWallWest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_wall_west", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(7.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int col = 0; col < COLUMNS; col++) {
                for (int row = 0; row < ROWS; row++) {
                    double z = -4.5 + col * 3.0;
                    double y = 7.0 + row * 7.0;
                    Location loc = center.clone().add(-25, y, z);
                    wallPositions.add(loc.clone());

                    BlockDisplayHandle trident = displayBuilder.spawnBlock(loc, Material.END_ROD);
                    trident.scale(0.12f, 1.3f, 0.12f);
                    // Aimed east: rotate -90 deg on Z
                    trident.rotate((float) Math.toRadians(-90), 0, 0, 1);
                    trident.glow(240, 240, 255);
                    wallTridents.add(trident);
                    spawnedEntities.add(trident.entity());
                }
            }

            DisplayBuilder.playSound(center.clone().add(-25, 10, 0), Sound.BLOCK_STONE_PLACE, 0.3f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Vibration: offset from east wall by 8 ticks
            if (ticksAlive % 20 == 8) {
                java.util.Random rng = new java.util.Random(ticksAlive + 1000);
                int vibrateIdx = rng.nextInt(wallTridents.size());
                BlockDisplayHandle vibrating = wallTridents.get(vibrateIdx);
                Location baseLoc = wallPositions.get(vibrateIdx);

                float xJitter = 0.15f * (rng.nextFloat() * 2 - 1);
                Location jitterLoc = baseLoc.clone().add(xJitter, 0, 0);
                vibrating.entity().teleport(jitterLoc);

                DisplayBuilder.playSound(jitterLoc, Sound.BLOCK_STONE_PLACE, 0.1f, 1.1f);
            }

            // Return to rest
            if (ticksAlive % 20 == 16) {
                for (int i = 0; i < wallTridents.size(); i++) {
                    wallTridents.get(i).entity().teleport(wallPositions.get(i));
                }
            }

            // ELECTRIC_SPARK + SPELL_WITCH
            if (ticksAlive % 7 == 0) {
                for (int i = 0; i < wallPositions.size(); i++) {
                    Location pos = wallPositions.get(i);
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pos.clone().add(0.8, 0, 0), 1, 0.05, 0.05, 0.05, 0.01);
                    center.getWorld().spawnParticle(Particle.WITCH, pos.clone().add(-0.8, 0, 0), 1, 0.05, 0.05, 0.05, 0.005);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentWallWest(plugin); }
    }

    // ================================================================
    // #16 -- SPIRAL TRIDENT TOWER
    // 18 end rod tridents in ascending double helix, 20N of center.
    // Y+1 to Y+19. Radius oscillation (breathing). Whole spiral
    // rotates at 0.5 deg/tick. Crimson dust trail.
    // ================================================================
    public static class SpiralTridentTower extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> helixTridents = new ArrayList<>();
        private static final int COUNT = 18;
        private static final double HELIX_RADIUS = 2.0;

        public SpiralTridentTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_spiral_trident_tower", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location towerBase = center.clone().add(0, 0, -20);

            for (int i = 0; i < COUNT; i++) {
                double helixAngle = (720.0 / COUNT) * i; // Two full rotations
                double angleRad = Math.toRadians(helixAngle);
                double px = Math.cos(angleRad) * HELIX_RADIUS;
                double pz = Math.sin(angleRad) * HELIX_RADIUS;
                double py = 1.0 + i * 1.0;

                Location loc = towerBase.clone().add(px, py, pz);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(loc, Material.END_ROD);
                trident.scale(0.08f, 0.9f, 0.08f);
                // Orient tangentially to helix curve
                float tangentAngle = (float) (angleRad + Math.PI / 2.0);
                trident.rotate(tangentAngle, 0, 1, 0);
                trident.glow(200, 0, 50).interpolation(3, 0);
                helixTridents.add(trident);
                spawnedEntities.add(trident.entity());
            }

            DisplayBuilder.playSound(towerBase, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.08f, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location towerBase = center.clone().add(0, 0, -20);

            // Whole spiral rotates at 0.5 deg/tick
            double globalRotation = Math.toRadians(ticksAlive * 0.5);

            for (int i = 0; i < helixTridents.size(); i++) {
                double helixAngle = Math.toRadians((720.0 / COUNT) * i) + globalRotation;

                // Radius breathing: 1.8 to 2.2, phase offset per trident
                double phaseOffset = (2.0 * Math.PI * i) / COUNT;
                double radius = HELIX_RADIUS + 0.2 * Math.sin(ticksAlive * 2.0 * Math.PI / 40.0 + phaseOffset);

                double px = Math.cos(helixAngle) * radius;
                double pz = Math.sin(helixAngle) * radius;
                double py = 1.0 + i * 1.0;

                Location newLoc = towerBase.clone().add(px, py, pz);
                helixTridents.get(i).entity().teleport(newLoc);

                float tangentAngle = (float) (helixAngle + Math.PI / 2.0);
                helixTridents.get(i).rotate(tangentAngle, 0, 1, 0);
                helixTridents.get(i).interpolation(3, 0);

                // Crimson dust trail
                if (ticksAlive % 5 == i % 5) {
                    DisplayBuilder.crimsonDust(newLoc, 1, 0.15);
                }
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(towerBase, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.08f, 1.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpiralTridentTower(plugin); }
    }

    // ================================================================
    // #17 -- TRIDENT PENDULUM
    // Single oversized end rod at scale 2.5, hanging from Y+28.
    // Swings N-S axis through position 15E. 120-tick period.
    // SOUL_FIRE_FLAME arc trail, ELECTRIC_SPARK from handle.
    // ================================================================
    public static class TridentPendulum extends BlockDisplayAttack {

        private BlockDisplayHandle pendulumTrident;
        private static final double SWING_AMPLITUDE = 10.0; // blocks lateral
        private static final double TOP_Y = 28.0;
        private static final double BOT_Y = 16.0;
        private static final int PERIOD = 120;

        public TridentPendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_pendulum", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(10.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Starting position: top of swing, 15E of center
            Location startLoc = center.clone().add(15, TOP_Y, 0);
            pendulumTrident = displayBuilder.spawnBlock(startLoc, Material.END_ROD);
            pendulumTrident.scale(0.35f, 3.5f, 0.35f);
            pendulumTrident.glow(0, 150, 255).interpolation(2, 0);
            spawnedEntities.add(pendulumTrident.entity());

            DisplayBuilder.playSound(startLoc, Sound.BLOCK_STONE_PLACE, 0.4f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Hang still for first 60 ticks (3 seconds)
            if (ticksAlive < 60) return;

            int swingTick = ticksAlive - 60;
            double phase = (2.0 * Math.PI * swingTick) / PERIOD;

            // Sinusoidal swing: lateral (Z) and vertical (Y)
            double lateralOffset = SWING_AMPLITUDE * Math.sin(phase);
            double yOffset = (TOP_Y - BOT_Y) * Math.abs(Math.cos(phase));
            double currentY = BOT_Y + yOffset;

            Location swingLoc = center.clone().add(15, currentY, lateralOffset);
            pendulumTrident.entity().teleport(swingLoc);

            // SOUL_FIRE_FLAME arc trail
            if (swingTick % 2 == 0) {
                Location tipLoc = swingLoc.clone().add(0, -1.5, 0);
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, tipLoc, 2, 0.1, 0.1, 0.1, 0.02);
            }

            // ELECTRIC_SPARK from handle
            if (swingTick % 4 == 0) {
                Location handleLoc = swingLoc.clone().add(0, 1.5, 0);
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, handleLoc, 1, 0.1, 0.1, 0.1, 0.01);
            }

            // Impact damage at maximum swing extent
            double velocity = Math.cos(phase);
            if (Math.abs(velocity) < 0.05 && swingTick % PERIOD != 0) {
                triggerImpactDamage(swingLoc);
            }

            // Sound at maximum swing extent
            if (swingTick % (PERIOD / 2) == 0) {
                DisplayBuilder.playSound(swingLoc, Sound.BLOCK_STONE_PLACE, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentPendulum(plugin); }
    }

    // ================================================================
    // #18 -- ROTATING TRIDENT CROWN
    // 20 end rod tridents at radius 18, Y+8. Very fast: 6 deg/tick
    // counterclockwise (60-tick revolution). Blades inward-down 30 deg.
    // Phase 4 only -- most aggressive ring. Fires one downward every 30 ticks.
    // SPELL_WITCH + CRIT + crimson dust triple trail.
    // ================================================================
    public static class RotatingTridentCrown extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crownTridents = new ArrayList<>();
        private static final int COUNT = 20;
        private static final double RADIUS = 18.0;
        private static final double Y_ALT = 8.0;
        private int fireIndex = 0;

        public RotatingTridentCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_crown", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(10.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(4000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double angle = (2.0 * Math.PI * i) / COUNT;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;
                Location loc = center.clone().add(px, Y_ALT, pz);

                BlockDisplayHandle trident = displayBuilder.spawnBlock(loc, Material.END_ROD);
                trident.scale(0.14f, 1.5f, 0.14f);
                // Inward and downward at 30 deg from vertical
                float tiltAngle = (float) Math.toRadians(30);
                float axisX = (float) Math.sin(angle);
                float axisZ = (float) -Math.cos(angle);
                trident.rotate(tiltAngle, axisX, 0, axisZ);
                trident.glow(200, 0, 50).interpolation(2, 0);
                crownTridents.add(trident);
                spawnedEntities.add(trident.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, Y_ALT, 0), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fast orbit: 6 deg/tick counterclockwise
            double orbitAngle = Math.toRadians(ticksAlive * 6.0);

            for (int i = 0; i < crownTridents.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / COUNT;
                double angle = baseAngle + orbitAngle;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;

                Location newLoc = center.clone().add(px, Y_ALT, pz);
                crownTridents.get(i).entity().teleport(newLoc);

                // Update tilt rotation
                float tiltAngle = (float) Math.toRadians(30);
                float axisX = (float) Math.sin(angle);
                float axisZ = (float) -Math.cos(angle);
                crownTridents.get(i).rotate(tiltAngle, axisX, 0, axisZ);
                crownTridents.get(i).interpolation(2, 0);

                // Triple particle trail: SPELL_WITCH + CRIT + crimson dust
                if (ticksAlive % 2 == i % 2) {
                    Location trailLoc = newLoc.clone().add(-Math.cos(angle) * 0.5, 0, -Math.sin(angle) * 0.5);
                    center.getWorld().spawnParticle(Particle.WITCH, trailLoc, 2, 0.15, 0.05, 0.15, 0.02);
                    center.getWorld().spawnParticle(Particle.CRIT, trailLoc, 1, 0.1, 0.05, 0.1, 0.02);
                    DisplayBuilder.crimsonDust(trailLoc, 1, 0.2);
                }
            }

            // Fire one trident downward every 30 ticks (visual CRIT burst)
            if (ticksAlive % 30 == 0 && ticksAlive > 0) {
                int idx = fireIndex % COUNT;
                fireIndex++;
                Location fireLoc = crownTridents.get(idx).entity().getLocation();
                // Ground strike CRIT burst
                Location groundLoc = center.clone();
                groundLoc.setY(center.getY());
                center.getWorld().spawnParticle(Particle.CRIT, groundLoc, 15, 1.0, 0.2, 1.0, 0.1);
                triggerImpactDamage(groundLoc);
            }

            // Revolution sound every 60 ticks
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, Y_ALT, 0), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RotatingTridentCrown(plugin); }
    }
}
