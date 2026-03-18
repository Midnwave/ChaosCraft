package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.environmental;

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
 * Phase 2 Environmental — GROUP 3: DIMENSIONAL INSTABILITY
 * 10 attacks involving reality glitches, rifts, and space warping.
 * Attacks 21-30 from boss2-dog.md.
 *
 * Design notes:
 * - No status effects (all debuffs translated to velocity/damage)
 * - DoG crystalline plague palette: cyan (0,200,255), violet (128,0,255), white (240,240,255)
 * - Materials: DARK_PRISMARINE, PRISMARINE, END_ROD, CYAN_STAINED_GLASS
 */
public final class DimensionalInstability {

    private DimensionalInstability() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ProximityRift(plugin));
        registry.register(new RealityStutter(plugin));
        registry.register(new SpatialCompressionZone(plugin));
        registry.register(new RiftCluster(plugin));
        registry.register(new GravitationalRipple(plugin));
        registry.register(new DimensionalEcho(plugin));
        registry.register(new SpaceFold(plugin));
        registry.register(new InstabilityCrack(plugin));
        registry.register(new RiftMagnetism(plugin));
        registry.register(new PhaseBleed(plugin));
    }

    // =========================================================================
    // 21. PROXIMITY RIFT — vertical portal oval, teleports players who approach
    // =========================================================================
    public static class ProximityRift extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();

        public ProximityRift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("proximity_rift", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // no direct damage, teleport hazard
            config.setDamageRadius(0.0);
            config.setDurationTicks(240); // 12 seconds
            config.setCooldownTicks(500); // 25 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: teleport sound 1.5 seconds before full open
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.6f);

            // Vertical oval rift — 2 blocks tall, 1 block wide rendered as stacked prismarine
            for (int y = 0; y < 4; y++) {
                float width = y <= 1 ? 0.6f + y * 0.2f : 0.8f - (y - 2) * 0.3f;
                Location loc = center.clone().add(0, y * 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(width, 0.45f, 0.15f).glow(128, 0, 255).interpolation(5, 0);
                riftHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning phase: 30 ticks
            if (ticksAlive <= 30) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 8, 0.5, 128, 0, 255, 1.0f);
                }
                return;
            }

            // Active: flicker rift edges
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 4, 0.8, 128, 0, 255, 0.8f);
            }

            // Teleport players who walk within 1 block
            if (ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 2.25) { // 1.5 block radius
                        // Random horizontal teleport 8-12 blocks away
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = 8 + Math.random() * 4;
                        Location teleportLoc = p.getLocation().add(
                                Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                        // Ensure not teleporting into void — set to highest block
                        teleportLoc.setY(Math.max(teleportLoc.getY(),
                                w.getHighestBlockYAt(teleportLoc) + 1));
                        p.teleport(teleportLoc);
                        DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.8f);
                        DisplayBuilder.cyanDust(p.getLocation(), 15, 1.0);
                    }
                }
            }

            // Pulse rift visual
            if (ticksAlive % 20 == 0) {
                for (BlockDisplayHandle h : riftHandles) {
                    float pulse = (float)(Math.sin(ticksAlive * 0.15) * 0.05f);
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    Transformation t = bd.getTransformation();
                    Vector3f s = t.getScale();
                    bd.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s.x + pulse, s.y, s.z + pulse),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ProximityRift(plugin); }
    }

    // =========================================================================
    // 22. REALITY STUTTER — brief visual inversion, movement disruption
    // =========================================================================
    public static class RealityStutter extends EnvironmentalAttack {

        private boolean stutterFired = false;

        public RealityStutter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_stutter", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(16); // 0.8 seconds
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // No warning — instantaneous
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Visual stutter: dense orange dust burst island-wide for 6 ticks
            if (ticksAlive == 1 && !stutterFired) {
                stutterFired = true;

                // Orange inversion flash
                DisplayBuilder.dustParticles(center, 80, 25.0, 255, 165, 0, 0.8f);
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.3f);
            }

            // Movement reversal: oppose player velocity for 16 ticks
            if (ticksAlive <= 16) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 900) { // 30 block radius
                        org.bukkit.util.Vector vel = p.getVelocity();
                        p.setVelocity(vel.setX(-vel.getX() * 0.5).setZ(-vel.getZ() * 0.5));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityStutter(plugin); }
    }

    // =========================================================================
    // 23. SPATIAL COMPRESSION ZONE — inward spiral pull, slows movement
    // =========================================================================
    public static class SpatialCompressionZone extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> zoneHandles = new ArrayList<>();
        private BlockDisplayHandle centerColumn;

        public SpatialCompressionZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spatial_compression_zone", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // no damage, movement hazard
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(700); // 35 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Zone forms over 2 seconds
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.3f);

            // Ring of prismarine at zone boundary (6-block diameter)
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 3, 0.02, Math.sin(angle) * 3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                h.scale(0.4f, 0.03f, 0.4f).glow(128, 0, 255).interpolation(5, 0);
                zoneHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center column — 8 blocks high
            centerColumn = displayBuilder.spawnBlock(center.clone().add(0, 0.1, 0), Material.SEA_LANTERN);
            centerColumn.scale(0.15f, 8.0f, 0.15f).glow(0, 200, 255).interpolation(8, 0);
            spawnedEntities.add(centerColumn.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Formation: first 40 ticks
            if (ticksAlive <= 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center, 8, 3.0, 128, 0, 255, 1.0f);
                }
                return;
            }

            // Active zone: slow players + pull toward center
            if (ticksAlive % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(p.getLocation().distanceSquared(center));
                    if (dist <= 3.0 && dist > 0.3) {
                        // Speed reduction: dampen velocity by 40%
                        org.bukkit.util.Vector vel = p.getVelocity();
                        p.setVelocity(vel.setX(vel.getX() * 0.6).setZ(vel.getZ() * 0.6));

                        // Pull toward center: 0.3 blocks per second = 0.015 per tick * 4
                        double pullX = (center.getX() - p.getLocation().getX()) / dist * 0.06;
                        double pullZ = (center.getZ() - p.getLocation().getZ()) / dist * 0.06;
                        p.setVelocity(p.getVelocity().add(
                                new org.bukkit.util.Vector(pullX, 0, pullZ)));
                    }
                }
            }

            // Spiral particles
            if (ticksAlive % 4 == 0) {
                double a = ticksAlive * 0.2;
                for (double r = 0.5; r <= 3.0; r += 0.5) {
                    Location pLoc = center.clone().add(Math.cos(a + r) * r, 0.3, Math.sin(a + r) * r);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.1, 128, 0, 255, 0.8f);
                }
            }

            // Pulse center column
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 6, 0.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpatialCompressionZone(plugin); }
    }

    // =========================================================================
    // 24. RIFT CLUSTER — 5 small rifts in radial pattern, scatter players outward
    // =========================================================================
    public static class RiftCluster extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private final List<Location> riftLocations = new ArrayList<>();

        public RiftCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_cluster", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(840); // 42 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: faint enderman ambient
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.4f);

            // 5 small rifts in radial pattern, 3-block radius from center
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5 + Math.random() * 0.3;
                double dist = 2 + Math.random() * 2;
                Location riftLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                riftLocations.add(riftLoc);

                // Half-size rift — 1 block tall
                for (int y = 0; y < 2; y++) {
                    Location loc = riftLoc.clone().add(0, y * 0.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                    h.scale(0.3f, 0.4f, 0.1f).glow(128, 0, 255).interpolation(4, 0);
                    riftHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks
            if (ticksAlive <= 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center, 6, 3.0, 128, 0, 255, 0.6f);
                }
                return;
            }

            // Active: each rift teleports players outward from cluster center
            if (ticksAlive % 10 == 0) {
                for (Location riftLoc : riftLocations) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(riftLoc) <= 1.5) {
                            // Teleport outward from center
                            double angle = Math.atan2(
                                    riftLoc.getZ() - center.getZ(),
                                    riftLoc.getX() - center.getX());
                            double dist = 8 + Math.random() * 4;
                            Location teleportLoc = p.getLocation().add(
                                    Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                            teleportLoc.setY(Math.max(teleportLoc.getY(),
                                    w.getHighestBlockYAt(teleportLoc) + 1));
                            p.teleport(teleportLoc);
                            DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.9f);
                            DisplayBuilder.cyanDust(p.getLocation(), 10, 0.8);
                        }
                    }
                }
            }

            // Flicker rifts
            if (ticksAlive % 8 == 0) {
                for (Location riftLoc : riftLocations) {
                    DisplayBuilder.dustParticles(riftLoc, 3, 0.3, 128, 0, 255, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftCluster(plugin); }
    }

    // =========================================================================
    // 25. GRAVITATIONAL RIPPLE — expanding ring at Y+15, launches players upward
    // =========================================================================
    public static class GravitationalRipple extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private double ringRadius = 0;

        public GravitationalRipple(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravitational_ripple", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // damage handled manually
            config.setDamageRadius(0.0);
            config.setDurationTicks(190); // ~9.5 seconds (1.5s warning + 8s expand)
            config.setCooldownTicks(600); // 30 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.6f);

            // Ring of sea lantern at Y+15
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI * i) / 20;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 15, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.2f, 0.1f, 0.2f).glow(0, 200, 255).interpolation(3, 0);
                ringHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5 seconds)
            if (ticksAlive <= 30) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 15, 0), 10, 1.0);
                }
                return;
            }

            // Ring expands at 5 blocks per second (0.25 blocks/tick)
            ringRadius += 0.25;
            float alpha = Math.max(0f, 1f - (float)(ringRadius / 30.0));

            for (int i = 0; i < ringHandles.size(); i++) {
                double angle = (2 * Math.PI * i) / ringHandles.size();
                Location newLoc = center.clone().add(
                        Math.cos(angle) * ringRadius, 15, Math.sin(angle) * ringRadius);
                ringHandles.get(i).entity().teleport(newLoc);
                ringHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-0.1f, -0.05f, -0.1f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.2f * alpha, 0.1f * alpha, 0.2f * alpha),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                ringHandles.get(i).entity().setInterpolationDelay(0);
                ringHandles.get(i).entity().setInterpolationDuration(2);
            }

            // Ground-level effect: teal dust floats upward as ring passes overhead
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double r = ringRadius + (Math.random() - 0.5) * 2;
                    Location pLoc = center.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                    DisplayBuilder.cyanDust(pLoc, 2, 0.3);
                }
            }

            // Launch players in ring path upward + 2 HP damage
            if (ticksAlive % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(p.getLocation().distanceSquared(center));
                    if (Math.abs(dist - ringRadius) <= 2.0) {
                        p.setVelocity(p.getVelocity().setY(0.6)); // 2 block lift
                        p.damage(2.0); // 1 heart gravitational impact
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravitationalRipple(plugin); }
    }

    // =========================================================================
    // 26. DIMENSIONAL ECHO — ghostly player duplicates confuse DoG targeting
    // =========================================================================
    public static class DimensionalEcho extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> echoHandles = new ArrayList<>();

        public DimensionalEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_echo", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // no damage — player benefit
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // 4 seconds
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // No warning — echoes simply appear
            // Spawn ghostly duplicates 3 blocks offset from each player
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double angle = Math.random() * 2 * Math.PI;
                Location echoLoc = p.getLocation().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);

                // Player-shaped cluster of spark particles (represented as prismarine block)
                BlockDisplayHandle h = displayBuilder.spawnBlock(echoLoc, Material.PRISMARINE);
                h.scale(0.4f, 1.6f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
                echoHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Echoes flicker — scale pulsing
            if (ticksAlive % 6 == 0) {
                float flicker = 0.4f + (float)(Math.random() * 0.15f);
                for (BlockDisplayHandle h : echoHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-flicker / 2, 0, -flicker / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(flicker, 1.6f, flicker),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
            }

            // Spark particles on each echo
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle h : echoHandles) {
                    DisplayBuilder.cyanDust(h.entity().getLocation().add(0, 0.8, 0), 4, 0.3);
                }
            }

            // Fade out near end
            if (ticksAlive > 60) {
                float fade = Math.max(0f, 1f - (ticksAlive - 60) / 20.0f);
                for (BlockDisplayHandle h : echoHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.2f * fade, 0, -0.2f * fade),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.4f * fade, 1.6f * fade, 0.4f * fade),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalEcho(plugin); }
    }

    // =========================================================================
    // 27. SPACE FOLD — two markers connected by beam, contracts pushing players inward
    // =========================================================================
    public static class SpaceFold extends EnvironmentalAttack {

        private BlockDisplayHandle markerA;
        private BlockDisplayHandle markerB;
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private Location pointA;
        private Location pointB;

        public SpaceFold(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("space_fold", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // positional hazard only
            config.setDamageRadius(0.0);
            config.setDurationTicks(150); // ~7.5 seconds (2.5s warning + 5s fold)
            config.setCooldownTicks(960); // 48 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Two markers on opposite sides
            double angle = Math.random() * Math.PI;
            pointA = center.clone().add(Math.cos(angle) * 15, 0.5, Math.sin(angle) * 15);
            pointB = center.clone().add(-Math.cos(angle) * 15, 0.5, -Math.sin(angle) * 15);

            markerA = displayBuilder.spawnBlock(pointA, Material.PRISMARINE);
            markerA.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(4, 0);
            spawnedEntities.add(markerA.entity());

            markerB = displayBuilder.spawnBlock(pointB, Material.PRISMARINE);
            markerB.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(4, 0);
            spawnedEntities.add(markerB.entity());

            // Connecting beam
            int beamCount = 15;
            for (int i = 0; i <= beamCount; i++) {
                float t = (float) i / beamCount;
                Location loc = pointA.clone().add(
                        (pointB.getX() - pointA.getX()) * t, 0,
                        (pointB.getZ() - pointA.getZ()) * t);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.1f, 0.1f, 0.1f).glow(0, 200, 255).interpolation(3, 0);
                beamHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(pointA, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
            DisplayBuilder.playSound(pointB, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 50 ticks (2.5 seconds)
            if (ticksAlive <= 50) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(pointA, 6, 0.5, 128, 0, 255, 0.8f);
                    DisplayBuilder.dustParticles(pointB, 6, 0.5, 128, 0, 255, 0.8f);
                }
                return;
            }

            // Fold: markers move inward over 100 ticks (5 seconds)
            float foldProgress = Math.min(1.0f, (ticksAlive - 50) / 100.0f);
            Location currentA = pointA.clone().add(
                    (center.getX() - pointA.getX()) * foldProgress, 0,
                    (center.getZ() - pointA.getZ()) * foldProgress);
            Location currentB = pointB.clone().add(
                    (center.getX() - pointB.getX()) * foldProgress, 0,
                    (center.getZ() - pointB.getZ()) * foldProgress);

            markerA.entity().teleport(currentA);
            markerB.entity().teleport(currentB);

            // Update beam positions
            for (int i = 0; i < beamHandles.size(); i++) {
                float t = (float) i / (beamHandles.size() - 1);
                Location loc = currentA.clone().add(
                        (currentB.getX() - currentA.getX()) * t, 0,
                        (currentB.getZ() - currentA.getZ()) * t);
                beamHandles.get(i).entity().teleport(loc);
            }

            // Push players inward along the fold line
            if (ticksAlive % 4 == 0) {
                double midX = (currentA.getX() + currentB.getX()) / 2;
                double midZ = (currentA.getZ() + currentB.getZ()) / 2;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    // Check if player is between the two markers
                    double distToLine = distanceToLine(pLoc, currentA, currentB);
                    if (distToLine <= 3.0) {
                        // Push toward midpoint
                        double dx = midX - pLoc.getX();
                        double dz = midZ - pLoc.getZ();
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 0.5) {
                            p.setVelocity(p.getVelocity().add(
                                    new org.bukkit.util.Vector(dx / dist * 0.08, 0, dz / dist * 0.08)));
                        }
                    }
                }
            }

            // Ambient particles
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(currentA, 4, 0.5);
                DisplayBuilder.cyanDust(currentB, 4, 0.5);
            }
        }

        private double distanceToLine(Location point, Location lineA, Location lineB) {
            double dx = lineB.getX() - lineA.getX();
            double dz = lineB.getZ() - lineA.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.01) return point.distance(lineA);
            double t = Math.max(0, Math.min(1,
                    ((point.getX() - lineA.getX()) * dx + (point.getZ() - lineA.getZ()) * dz) / (len * len)));
            double closestX = lineA.getX() + t * dx;
            double closestZ = lineA.getZ() + t * dz;
            return Math.sqrt(Math.pow(point.getX() - closestX, 2) + Math.pow(point.getZ() - closestZ, 2));
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpaceFold(plugin); }
    }

    // =========================================================================
    // 28. INSTABILITY CRACK — narrow fissure, hard to see, stuns on contact
    // =========================================================================
    public static class InstabilityCrack extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> crackHandles = new ArrayList<>();

        public InstabilityCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("instability_crack", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(4.0); // 2 hearts on contact
            config.setDamageRadius(1.0); // very narrow
            config.setDurationTicks(400); // 20 seconds
            config.setCooldownTicks(500); // 25 seconds
            config.setTicksBetweenDamage(30); // 1.5 second between hits
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Geological crack sound
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.8f);

            // Narrow 0.5-block fissure, 6 blocks long
            for (int i = -3; i <= 3; i++) {
                Location loc = center.clone().add(i, 0.01, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.9f, 0.02f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                crackHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 1 second (20 ticks) before active
            if (ticksAlive <= 20) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center, 4, 1.0, 128, 0, 255, 0.5f);
                }
                return;
            }

            // Rapid portal strobing effect
            if (ticksAlive % 6 == 0) {
                for (BlockDisplayHandle h : crackHandles) {
                    float flicker = (float)(Math.random() * 0.03f);
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.45f, -0.01f, -0.15f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.9f, 0.02f + flicker, 0.3f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
            }

            // Stun players who step in — velocity zeroed briefly
            if (ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 2.25) {
                        p.setVelocity(new org.bukkit.util.Vector(0, p.getVelocity().getY(), 0));
                    }
                }
            }

            // Faint dimensional particle
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.dustParticles(center, 3, 1.5, 128, 0, 255, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InstabilityCrack(plugin); }
    }

    // =========================================================================
    // 29. RIFT MAGNETISM — all active rifts intensify and pull players toward them
    // =========================================================================
    public static class RiftMagnetism extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> pulseHandles = new ArrayList<>();

        public RiftMagnetism(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_magnetism", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // no damage, pull hazard
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Multiple rifts intensify — simulate with pulse rings at center
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

            // Visual: outward magnetic wave ring at center
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 4, 0.1, Math.sin(angle) * 4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.3f, 0.15f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                pulseHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Pulse ring expands and contracts (breathing)
            float breathe = (float)(4.0 + Math.sin(ticksAlive * 0.15) * 3.0);
            for (int i = 0; i < pulseHandles.size(); i++) {
                double angle = (2 * Math.PI * i) / pulseHandles.size();
                Location loc = center.clone().add(Math.cos(angle) * breathe, 0.1, Math.sin(angle) * breathe);
                pulseHandles.get(i).entity().teleport(loc);
            }

            // Pull players toward center (0.5 blocks per second = 0.025/tick)
            if (ticksAlive % 2 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(p.getLocation().distanceSquared(center));
                    if (dist <= 8.0 && dist > 0.5) {
                        double pullX = (center.getX() - p.getLocation().getX()) / dist * 0.05;
                        double pullZ = (center.getZ() - p.getLocation().getZ()) / dist * 0.05;
                        p.setVelocity(p.getVelocity().add(
                                new org.bukkit.util.Vector(pullX, 0, pullZ)));
                    }
                }
            }

            // Ambient particles
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(center, 10, breathe, 128, 0, 255, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftMagnetism(plugin); }
    }

    // =========================================================================
    // 30. PHASE BLEED — distorted zone with random damage ticks
    // =========================================================================
    public static class PhaseBleed extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> tearHandles = new ArrayList<>();

        public PhaseBleed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase_bleed", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(4.0); // 2 hearts — replaces random debuff pool
            config.setDamageRadius(7.5); // 15-block-diameter zone
            config.setDurationTicks(300); // 15 seconds
            config.setCooldownTicks(1100); // 55 seconds
            config.setTicksBetweenDamage(40); // every 2 seconds (~20% chance proxy)
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Zone forms over 3 seconds
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.4f);

            // Scattered portal tears floating in midair
            for (int i = 0; i < 12; i++) {
                double ox = (Math.random() - 0.5) * 14;
                double oy = 1 + Math.random() * 4;
                double oz = (Math.random() - 0.5) * 14;
                Location loc = center.clone().add(ox, oy, oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.15f, 0.3f, 0.05f).glow(128, 0, 255).interpolation(5, 0);
                tearHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Formation phase: 60 ticks (3 seconds)
            if (ticksAlive <= 60) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center, 8, 7.0, 128, 0, 255, 0.6f);
                }
                return;
            }

            // Active: tears flicker at irregular intervals
            if (ticksAlive % 8 == 0) {
                int idx = (int)(Math.random() * tearHandles.size());
                if (idx < tearHandles.size()) {
                    float flicker = 0.1f + (float)(Math.random() * 0.2f);
                    BlockDisplay bd = (BlockDisplay) tearHandles.get(idx).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-flicker / 2, -0.15f, -0.025f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(flicker, 0.3f, 0.05f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);

                    DisplayBuilder.dustParticles(bd.getLocation(), 3, 0.3, 128, 0, 255, 0.5f);
                }
            }

            // Ambient distortion particles
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.dustParticles(center, 6, 6.0, 128, 0, 255, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseBleed(plugin); }
    }
}
