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
 * Phase 2 Environmental — GROUP 4: ISLAND DETERIORATION
 * 10 attacks where the island cracks, sections collapse, geometry shifts.
 * Attacks 31-40 from boss2-dog.md.
 *
 * Design notes:
 * - No status effects
 * - DoG crystalline plague palette: cyan (0,200,255), violet (128,0,255), white (240,240,255)
 * - Island geology materials: POLISHED_BLACKSTONE, DARK_PRISMARINE, AMETHYST_BLOCK, AMETHYST_CLUSTER
 * - Structural sounds: BLOCK_STONE_PLACE, BLOCK_DEEPSLATE_BREAK
 */
public final class IslandDeterioration {

    private IslandDeterioration() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new EdgeCalving(plugin));
        registry.register(new FloorSubsidence(plugin));
        registry.register(new PillarShear(plugin));
        registry.register(new SurfaceRupture(plugin));
        registry.register(new SinkholeExpansion(plugin));
        registry.register(new AmethystBloomEruption(plugin));
        registry.register(new IslandTilt(plugin));
        registry.register(new GroundDetonationGrid(plugin));
        registry.register(new FaultReactivation(plugin));
        registry.register(new PlatformFracture(plugin));
    }

    // =========================================================================
    // 31. EDGE CALVING — section of island edge cracks, tilts, falls
    // =========================================================================
    public static class EdgeCalving extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> calvingHandles = new ArrayList<>();
        private Location edgeLoc;
        private boolean calvingTriggered = false;

        public EdgeCalving(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("edge_calving", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // damage handled manually
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // ~4 seconds (2s warning + 2s event)
            config.setCooldownTicks(700); // 35 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random edge direction
            double angle = Math.random() * 2 * Math.PI;
            edgeLoc = center.clone().add(Math.cos(angle) * 18, 0, Math.sin(angle) * 18);

            DisplayBuilder.playSound(edgeLoc, Sound.BLOCK_STONE_PLACE, 0.9f, 0.5f);

            // Spawn edge section blocks — 8 block wide platform tilting outward
            for (int i = -3; i <= 3; i++) {
                double perpAngle = angle + Math.PI / 2;
                Location loc = edgeLoc.clone().add(Math.cos(perpAngle) * i, 0.1, Math.sin(perpAngle) * i);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 0.3f, 1.0f).glow(128, 0, 255).interpolation(5, 0);
                calvingHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fracture line indicator particles
            DisplayBuilder.dustParticles(edgeLoc, 15, 4.0, 240, 240, 255, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(edgeLoc, 8, 3.0, 240, 240, 255, 0.6f);
                    DisplayBuilder.playSound(edgeLoc, Sound.BLOCK_STONE_PLACE, 0.5f, 0.6f);
                }
                return;
            }

            // CALVING: tilt and fall
            if (!calvingTriggered) {
                calvingTriggered = true;
                DisplayBuilder.playSound(edgeLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
            }

            int calveTick = ticksAlive - 40;
            float tiltAngle = Math.min(calveTick * 0.04f, 0.5f);
            float drop = calveTick * 0.15f;

            for (BlockDisplayHandle h : calvingHandles) {
                BlockDisplay bd = (BlockDisplay) h.entity();
                Location loc = bd.getLocation();
                loc.setY(edgeLoc.getY() + 0.1 - drop);
                bd.teleport(loc);
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.15f, -0.5f),
                        new AxisAngle4f(tiltAngle, 0, 0, 1),
                        new Vector3f(1.0f, Math.max(0.01f, 0.3f - calveTick * 0.01f), 1.0f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
            }

            // Knockback players at edge inward + damage
            if (calveTick == 1) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(edgeLoc) <= 9.0) {
                        // Push inward (toward center)
                        double dx = center.getX() - p.getLocation().getX();
                        double dz = center.getZ() - p.getLocation().getZ();
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 0.1) {
                            p.setVelocity(p.getVelocity().add(
                                    new org.bukkit.util.Vector(dx / dist * 0.5, 0.3, dz / dist * 0.5)));
                        }
                        p.damage(4.0); // 2 hearts
                    }
                }
            }

            // Particle dust on calving
            if (calveTick % 4 == 0) {
                DisplayBuilder.dustParticles(edgeLoc, 10, 3.0, 200, 200, 200, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EdgeCalving(plugin); }
    }

    // =========================================================================
    // 32. FLOOR SUBSIDENCE — circular zone sinks, pushes players down
    // =========================================================================
    public static class FloorSubsidence extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> subsidenceHandles = new ArrayList<>();
        private boolean snapped = false;

        public FloorSubsidence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floor_subsidence", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // damage on snap
            config.setDamageRadius(0.0);
            config.setDurationTicks(360); // ~18 seconds (3s warning + 15s zone)
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.6f);

            // 8-block-diameter circle of subsidence tiles
            for (int x = -4; x <= 3; x++) {
                for (int z = -4; z <= 3; z++) {
                    if (x * x + z * z <= 16) {
                        Location loc = center.clone().add(x, 0.02, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                        h.scale(0.9f, 0.04f, 0.9f).glow(80, 80, 100).interpolation(4, 0);
                        subsidenceHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Particle field builds for 60 ticks (3 seconds)
            if (ticksAlive <= 60) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center, 10, 4.0, 100, 100, 110, 0.5f);
                }
                return;
            }

            // SNAP at tick 60
            if (ticksAlive == 61 && !snapped) {
                snapped = true;

                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
                DisplayBuilder.dustParticles(center, 40, 4.0, 100, 100, 110, 1.2f);

                // All tiles drop slightly
                for (BlockDisplayHandle h : subsidenceHandles) {
                    Location loc = h.entity().getLocation();
                    loc.setY(loc.getY() - 0.3);
                    h.entity().teleport(loc);
                    h.entity().setGlowColorOverride(Color.fromRGB(60, 60, 80));
                }

                // Damage + slow players in zone: 3 HP (1.5 hearts)
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 16.0) { // 4 block radius
                        p.damage(3.0);
                        p.setVelocity(p.getVelocity().setY(-0.3));
                    }
                }
            }

            // Subsidence zone active — slow movement in zone
            if (snapped && ticksAlive % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 16.0) {
                        org.bukkit.util.Vector vel = p.getVelocity();
                        p.setVelocity(vel.setX(vel.getX() * 0.7).setZ(vel.getZ() * 0.7));
                    }
                }
            }

            // Ambient dust from edges
            if (ticksAlive % 12 == 0 && snapped) {
                DisplayBuilder.dustParticles(center, 5, 4.0, 100, 100, 110, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloorSubsidence(plugin); }
    }

    // =========================================================================
    // 33. PILLAR SHEAR — upper pillar section detaches and flies outward
    // =========================================================================
    public static class PillarShear extends EnvironmentalAttack {

        private BlockDisplayHandle pillarBase;
        private BlockDisplayHandle shearSection;
        private Location pillarLoc;
        private Location flyDirection;
        private boolean sheared = false;

        public PillarShear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pillar_shear", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // 4 seconds
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random pillar location on island periphery
            double angle = Math.random() * 2 * Math.PI;
            pillarLoc = center.clone().add(Math.cos(angle) * 12, 0, Math.sin(angle) * 12);

            // Direction: outward toward nearest player
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double d = p.getLocation().distanceSquared(pillarLoc);
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = p;
                }
            }
            if (nearest != null) {
                flyDirection = nearest.getLocation().clone();
            } else {
                flyDirection = center.clone();
            }

            // Warning: cracking sounds
            DisplayBuilder.playSound(pillarLoc, Sound.BLOCK_STONE_PLACE, 0.9f, 0.6f);
            DisplayBuilder.playSound(pillarLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.8f);

            // Pillar base — stays in place
            pillarBase = displayBuilder.spawnBlock(pillarLoc, Material.POLISHED_BLACKSTONE);
            pillarBase.scale(1.5f, 4.0f, 1.5f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(pillarBase.entity());

            // Shear section — will detach and fly
            Location topLoc = pillarLoc.clone().add(0, 4, 0);
            shearSection = displayBuilder.spawnBlock(topLoc, Material.POLISHED_BLACKSTONE);
            shearSection.scale(1.5f, 3.0f, 1.5f).glow(0, 200, 255).interpolation(3, 0);
            spawnedEntities.add(shearSection.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(pillarLoc, Sound.BLOCK_STONE_PLACE, 0.4f, 0.7f);
                    DisplayBuilder.dustParticles(pillarLoc.clone().add(0, 4, 0), 6, 1.0, 200, 200, 200, 0.6f);
                }
                return;
            }

            // SHEAR at tick 40
            if (!sheared) {
                sheared = true;
                DisplayBuilder.playSound(pillarLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
                DisplayBuilder.cyanDust(pillarLoc.clone().add(0, 4, 0), 25, 1.5);
            }

            // Fly section toward player direction
            int flyTick = ticksAlive - 40;
            float flyProgress = Math.min(1.0f, flyTick / 30.0f);

            double dx = flyDirection.getX() - pillarLoc.getX();
            double dz = flyDirection.getZ() - pillarLoc.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 0.1) dist = 1;

            Location flyLoc = pillarLoc.clone().add(
                    (dx / dist) * flyProgress * 8, 4 - flyProgress * 3, (dz / dist) * flyProgress * 8);
            shearSection.entity().teleport(flyLoc);

            // Rotation during flight
            float rot = flyTick * 0.15f;
            float scale = Math.max(0.1f, 1.5f * (1f - flyProgress));
            shearSection.entity().setTransformation(new Transformation(
                    new Vector3f(-scale / 2, -1.5f, -scale / 2),
                    new AxisAngle4f(rot, 0.3f, 1, 0),
                    new Vector3f(scale, 3.0f * (1f - flyProgress * 0.5f), scale),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
            shearSection.entity().setInterpolationDelay(0);
            shearSection.entity().setInterpolationDuration(3);

            // Damage players in flight path: 10 HP (5 hearts)
            if (flyTick % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(flyLoc) <= 4.0) {
                        p.damage(10.0);
                        DisplayBuilder.cyanDust(p.getLocation(), 10, 0.5);
                    }
                }
            }

            // Dissolve into particles at end
            if (flyProgress >= 0.9f) {
                DisplayBuilder.cyanDust(flyLoc, 15, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PillarShear(plugin); }
    }

    // =========================================================================
    // 34. SURFACE RUPTURE — short fissure opens across island surface
    // =========================================================================
    public static class SurfaceRupture extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> ruptureHandles = new ArrayList<>();

        public SurfaceRupture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("surface_rupture", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(4.0); // 2 hearts on contact
            config.setDamageRadius(1.5);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(600); // 30 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.5f);

            // 3-block-long rupture gap with upward-erupting particles
            for (int i = -1; i <= 1; i++) {
                Location loc = center.clone().add(i, 0.01, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.9f, 0.02f, 0.5f).glow(0, 200, 255).interpolation(5, 0);
                ruptureHandles.add(h);
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
                    DisplayBuilder.dustParticles(center, 6, 1.5, 200, 200, 200, 0.5f);
                }
                return;
            }

            // Full open: widen rupture
            if (ticksAlive == 31) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
                for (BlockDisplayHandle h : ruptureHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.45f, -0.01f, -0.4f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.9f, 0.03f, 0.8f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                    bd.setGlowColorOverride(Color.fromRGB(0, 200, 255));
                }
            }

            // Upward erupting particles from the gap
            if (ticksAlive % 5 == 0 && ticksAlive > 30) {
                for (int i = -1; i <= 1; i++) {
                    Location pLoc = center.clone().add(i, 0.3 + Math.random() * 2, 0);
                    DisplayBuilder.dustParticles(pLoc, 3, 0.2, 200, 200, 200, 0.8f);
                }
            }

            // Push players who step on it upward
            if (ticksAlive % 10 == 0 && ticksAlive > 30) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 2.25) {
                        p.setVelocity(p.getVelocity().setY(0.4));
                    }
                }
            }

            // Heal over last 50 ticks (rupture narrows)
            if (ticksAlive > 150) {
                float heal = (ticksAlive - 150) / 50.0f;
                float width = Math.max(0.05f, 0.8f * (1f - heal));
                for (BlockDisplayHandle h : ruptureHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.45f, -0.01f, -width / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.9f, 0.03f, width),
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
        public AbstractAttack newInstance() { return new SurfaceRupture(plugin); }
    }

    // =========================================================================
    // 35. SINKHOLE EXPANSION — central sinkhole grows outward permanently
    // =========================================================================
    public static class SinkholeExpansion extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> expansionHandles = new ArrayList<>();

        public SinkholeExpansion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sinkhole_expansion", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // hazard zone damage handled by existing sinkhole
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds (3s rumble + 3s expansion)
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Deep rumbling warning
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.9f, 0.3f);

            // Expansion ring — new sinkhole outer edge
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI * i) / 20;
                Location loc = center.clone().add(Math.cos(angle) * 5, 0.02, Math.sin(angle) * 5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.04f, 0.8f).glow(128, 0, 255).interpolation(6, 0);
                expansionHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rumble buildup: 60 ticks (3 seconds)
            if (ticksAlive <= 60) {
                // Accelerating rumble sounds
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f + ticksAlive * 0.005f, 0.3f);
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center, 6, 4.0, 128, 0, 255, 0.8f);
                }
                return;
            }

            // Expansion: ring moves outward from radius 5 to 7 over 60 ticks
            float expandProgress = (ticksAlive - 60) / 60.0f;
            double radius = 5.0 + expandProgress * 2.0;

            for (int i = 0; i < expansionHandles.size(); i++) {
                double angle = (2 * Math.PI * i) / expansionHandles.size();
                Location newLoc = center.clone().add(Math.cos(angle) * radius, 0.02, Math.sin(angle) * radius);
                expansionHandles.get(i).entity().teleport(newLoc);
            }

            // Upward particles from new edge
            if (ticksAlive % 6 == 0) {
                double a = Math.random() * 2 * Math.PI;
                Location pLoc = center.clone().add(Math.cos(a) * radius, 0.5, Math.sin(a) * radius);
                DisplayBuilder.dustParticles(pLoc, 4, 0.3, 128, 0, 255, 1.0f);
            }

            // Sound on completion
            if (ticksAlive == 100) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SinkholeExpansion(plugin); }
    }

    // =========================================================================
    // 36. AMETHYST BLOOM ERUPTION — cluster of crystal obstacles erupts from floor
    // =========================================================================
    public static class AmethystBloomEruption extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> bloomHandles = new ArrayList<>();

        public AmethystBloomEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("amethyst_bloom_eruption", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(6.0); // 3 hearts if shattered by DoG
            config.setDamageRadius(3.0);
            config.setDurationTicks(500); // 25 seconds
            config.setCooldownTicks(760); // 38 seconds
            config.setTicksBetweenDamage(100); // infrequent
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 30 ticks (1.5 seconds)
            if (ticksAlive <= 30) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center, 6, 2.0);
                }
                return;
            }

            // Eruption: spawn clusters in ripple pattern over 40 ticks
            if (ticksAlive > 30 && ticksAlive <= 70) {
                int spawnIndex = (ticksAlive - 30) / 4;
                if ((ticksAlive - 30) % 4 == 0 && spawnIndex < 10) {
                    double angle = (2 * Math.PI * spawnIndex) / 10;
                    double dist = 1 + (spawnIndex % 3) * 1.5;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                    h.scale(0.0f, 0.0f, 0.0f).glow(0, 200, 255).interpolation(10, 0);
                    bloomHandles.add(h);
                    spawnedEntities.add(h.entity());

                    // Scale up burst
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    float size = 0.5f + (float)(Math.random() * 0.5f);
                    bd.setTransformation(new Transformation(
                            new Vector3f(-size / 2, 0, -size / 2),
                            new AxisAngle4f((float)(Math.random() * 0.5f), 0, 1, 0),
                            new Vector3f(size, size * 2, size),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(10);

                    DisplayBuilder.cyanDust(loc, 8, 0.5);
                    DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.5f + spawnIndex * 0.05f);
                }
            }

            // Persistent ambient glow
            if (ticksAlive % 15 == 0 && ticksAlive > 70) {
                DisplayBuilder.cyanDust(center, 6, 3.0);
            }

            // Fade over last 60 ticks
            if (ticksAlive > 440) {
                float fade = Math.max(0f, 1f - (ticksAlive - 440) / 60.0f);
                for (BlockDisplayHandle h : bloomHandles) {
                    Transformation t = h.entity().getTransformation();
                    Vector3f s = t.getScale();
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-s.x * fade / 2, 0, -s.z * fade / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s.x * fade, s.y * fade, s.z * fade),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AmethystBloomEruption(plugin); }
    }

    // =========================================================================
    // 37. ISLAND TILT — illusory tilt with lateral force on all players
    // =========================================================================
    public static class IslandTilt extends EnvironmentalAttack {

        private double tiltDirX;
        private double tiltDirZ;

        public IslandTilt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("island_tilt", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // no damage, positional hazard
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random tilt direction
            double angle = Math.random() * 2 * Math.PI;
            tiltDirX = Math.cos(angle);
            tiltDirZ = Math.sin(angle);

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center, 12, 15.0, 150, 150, 160, 0.5f);
                }
                return;
            }

            // Active tilt: lateral force on all players
            if (ticksAlive % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 900) { // 30 block radius
                        // 0.3 blocks per second = 0.015 per tick * 4 = 0.06
                        p.setVelocity(p.getVelocity().add(
                                new org.bukkit.util.Vector(tiltDirX * 0.06, 0, tiltDirZ * 0.06)));
                    }
                }
            }

            // Visual drift particles across surface in tilt direction
            if (ticksAlive % 3 == 0) {
                double ox = (Math.random() - 0.5) * 20;
                double oz = (Math.random() - 0.5) * 20;
                Location pLoc = center.clone().add(ox, 0.3, oz);
                DisplayBuilder.dustParticles(pLoc, 2, 0.3, 150, 150, 160, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IslandTilt(plugin); }
    }

    // =========================================================================
    // 38. GROUND DETONATION GRID — 9 evenly spaced points detonate simultaneously
    // =========================================================================
    public static class GroundDetonationGrid extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> gridHandles = new ArrayList<>();
        private final Location[] gridPoints = new Location[9];
        private boolean detonated = false;

        public GroundDetonationGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ground_detonation_grid", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // handled manually for multi-point hits
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // 4 seconds (3s warning + detonation)
            config.setCooldownTicks(1100); // 55 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3x3 grid of evenly spaced points
            int idx = 0;
            for (int gx = -1; gx <= 1; gx++) {
                for (int gz = -1; gz <= 1; gz++) {
                    gridPoints[idx] = center.clone().add(gx * 7, 0, gz * 7);

                    // Circle indicators at each point
                    for (int i = 0; i < 8; i++) {
                        double angle = (2 * Math.PI * i) / 8;
                        Location loc = gridPoints[idx].clone().add(Math.cos(angle) * 1, 0.03, Math.sin(angle) * 1);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                        h.scale(0.2f, 0.03f, 0.2f).glow(0, 200, 255).interpolation(3, 0);
                        gridHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    idx++;
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: circles pulse for 60 ticks (3 seconds)
            if (ticksAlive <= 60) {
                if (ticksAlive % 10 == 0) {
                    for (Location gp : gridPoints) {
                        if (gp != null) DisplayBuilder.cyanDust(gp, 6, 1.0);
                    }
                }
                // Intensify pulse toward detonation
                if (ticksAlive % 20 == 0) {
                    float pulse = 0.2f + (ticksAlive / 60.0f) * 0.15f;
                    for (BlockDisplayHandle h : gridHandles) {
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        bd.setTransformation(new Transformation(
                                new Vector3f(-pulse / 2, -0.015f, -pulse / 2),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(pulse, 0.03f, pulse),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(5);
                    }
                }
                return;
            }

            // DETONATION at tick 60
            if (ticksAlive == 61 && !detonated) {
                detonated = true;

                for (Location gp : gridPoints) {
                    if (gp == null) continue;

                    DisplayBuilder.cyanDust(gp, 30, 2.0);
                    DisplayBuilder.playSound(gp, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.0f);

                    // Damage: 6 HP (3 hearts) per point, 2-block radius
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(gp) <= 4.0) {
                            p.damage(6.0);
                        }
                    }
                }

                // Flash all indicators bright white
                for (BlockDisplayHandle h : gridHandles) {
                    h.entity().setGlowColorOverride(Color.fromRGB(240, 240, 255));
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-0.2f, -0.015f, -0.2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.4f, 0.06f, 0.4f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(3);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundDetonationGrid(plugin); }
    }

    // =========================================================================
    // 39. FAULT REACTIVATION — 4 main fault lines light up as damage conduits
    // =========================================================================
    public static class FaultReactivation extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> faultHandles = new ArrayList<>();

        public FaultReactivation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fault_reactivation", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(6.0); // 3 hearts per crossing
            config.setDamageRadius(1.5);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(900); // 45 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.9f);

            // 4 fault lines radiating from center (N, E, S, W)
            double[][] dirs = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
            for (double[] dir : dirs) {
                for (int step = 1; step <= 15; step++) {
                    Location loc = center.clone().add(dir[0] * step, 0.02, dir[1] * step);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                    h.scale(0.3f, 0.04f, 0.3f).glow(0, 200, 255).interpolation(4, 0);
                    faultHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fault lines pulse
            if (ticksAlive % 10 == 0) {
                float pulse = 0.3f + (float)(Math.sin(ticksAlive * 0.2) * 0.1f);
                for (BlockDisplayHandle h : faultHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-pulse / 2, -0.02f, -pulse / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, 0.04f, pulse),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
            }

            // Spark particles along the faults
            if (ticksAlive % 6 == 0) {
                double[][] dirs = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
                int dirIdx = (ticksAlive / 6) % 4;
                double[] dir = dirs[dirIdx];
                double dist = 2 + Math.random() * 12;
                Location pLoc = center.clone().add(dir[0] * dist, 0.2, dir[1] * dist);
                DisplayBuilder.cyanDust(pLoc, 4, 0.3);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FaultReactivation(plugin); }
    }

    // =========================================================================
    // 40. PLATFORM FRACTURE — raised area cracks and collapses, launches players
    // =========================================================================
    public static class PlatformFracture extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> platformHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> crackHandles = new ArrayList<>();
        private boolean collapsed = false;

        public PlatformFracture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("platform_fracture", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // displacement damage
            config.setDamageRadius(0.0);
            config.setDurationTicks(180); // 9 seconds (4s crack + 5s collapse)
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Raised platform visual (3x3 blocks, slightly elevated)
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x, 0.5, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(0.9f, 0.4f, 0.9f).glow(128, 0, 255).interpolation(3, 0);
                    platformHandles.add(h);
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

            // Phase 1: Cracks develop over 80 ticks (4 seconds)
            if (ticksAlive <= 80) {
                // Spawn crack lines progressively
                if (ticksAlive % 20 == 0) {
                    int crackIdx = ticksAlive / 20;
                    double[] offsets = {-0.5, 0, 0.5, -0.3};
                    if (crackIdx < offsets.length) {
                        for (int i = -1; i <= 1; i++) {
                            Location loc = center.clone().add(i, 0.92, offsets[crackIdx]);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                            h.scale(0.8f, 0.02f, 0.1f).glow(0, 200, 255).interpolation(4, 0);
                            crackHandles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                        DisplayBuilder.dustParticles(center, 6, 1.5, 150, 150, 160, 0.5f);
                    }
                }
                return;
            }

            // COLLAPSE at tick 80
            if (ticksAlive == 81 && !collapsed) {
                collapsed = true;

                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.3f);
                DisplayBuilder.dustParticles(center, 40, 2.0, 150, 150, 160, 1.2f);

                // Platform blocks collapse — scale to 0
                for (BlockDisplayHandle h : platformHandles) {
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(15);
                }

                // Launch players on platform: 3 blocks up + random horizontal displacement
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 6.25) { // ~2.5 block radius
                        double randX = (Math.random() - 0.5) * 0.6;
                        double randZ = (Math.random() - 0.5) * 0.6;
                        p.setVelocity(new org.bukkit.util.Vector(randX, 0.8, randZ));
                        p.damage(4.0); // 2 hearts displacement impact
                    }
                }
            }

            // Fall-damage zone indicator for 5 seconds after collapse
            if (collapsed && ticksAlive > 81 && ticksAlive <= 181 && ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(center, 4, 1.5, 200, 0, 50, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PlatformFracture(plugin); }
    }
}
