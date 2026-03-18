package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.environmental;

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
 * Phase 3 Environmental -- GROUP 9: THE VOID BELOW
 * 10 attacks (#81-90) where island edges crumble into void.
 * Lava meeting void at edges. Extreme edge hazards.
 * Structural integrity of the arena is failing.
 *
 * Design notes:
 * - No status effects (velocity reduction for slow, no Slowness potion)
 * - Dweller palette: crimson (200,0,50), orange glow (255,100,0), soul blue (0,150,255)
 * - Void materials: OBSIDIAN, CRYING_OBSIDIAN, BLACK_CONCRETE, BLACKSTONE
 * - Void particle colors: soul (0,255,240), warped (0,175,155)
 * - Very high damage (10-12 HP base), short warnings (1-2 seconds)
 */
public final class VoidBelow {

    private VoidBelow() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new EdgeFracture(plugin));
        registry.register(new VoidSurge(plugin));
        registry.register(new LavaVoidInterface(plugin));
        registry.register(new CrumbleCascade(plugin));
        registry.register(new VoidTendrils(plugin));
        registry.register(new IslandTiltAttack(plugin));
        registry.register(new EdgeInferno(plugin));
        registry.register(new VoidQuake(plugin));
        registry.register(new TheLastLedge(plugin));
        registry.register(new Threshold(plugin));
    }

    // =========================================================================
    // 81. EDGE FRACTURE -- section of edge cracks and drops into void
    // =========================================================================
    public static class EdgeFracture extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> fractureHandles = new ArrayList<>();
        private Location fractureCenter;
        private double fractureAngle;
        private boolean dropped = false;

        public EdgeFracture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("edge_fracture", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(60); // 3 seconds
            config.setCooldownTicks(280); // 14 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            fractureAngle = Math.random() * 2 * Math.PI;
            fractureCenter = center.clone().add(Math.cos(fractureAngle) * 15, 0, Math.sin(fractureAngle) * 15);

            DisplayBuilder.playSound(fractureCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

            // Spawn sagging edge section -- 5 blocks wide
            double perpAngle = fractureAngle + Math.PI / 2;
            for (int i = -2; i <= 2; i++) {
                Location loc = fractureCenter.clone().add(
                        Math.cos(perpAngle) * i, 0.05, Math.sin(perpAngle) * i);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(1.0f, 0.2f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                fractureHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 20 ticks (1 second) -- very short! Section tilts
            if (ticksAlive <= 20) {
                float sag = ticksAlive * 0.015f;
                for (BlockDisplayHandle h : fractureHandles) {
                    Location loc = h.entity().getLocation();
                    loc.setY(fractureCenter.getY() + 0.05 - sag);
                    h.entity().teleport(loc);
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-0.5f, -0.1f, -0.5f),
                            new AxisAngle4f(sag * 0.5f, 0, 0, 1),
                            new Vector3f(1.0f, 0.2f, 1.0f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(3);
                }
                return;
            }

            // DROP
            if (!dropped) {
                dropped = true;
                DisplayBuilder.playSound(fractureCenter, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.3f);

                // Blocks fall into void
                for (BlockDisplayHandle h : fractureHandles) {
                    Location loc = h.entity().getLocation();
                    loc.setY(loc.getY() - 20);
                    h.entity().teleport(loc);
                }

                // Lava flowing over edge into void
                w.spawnParticle(Particle.LAVA, fractureCenter, 15, 2.0, 0.5, 2.0, 0);

                // Soul particles rising from below
                DisplayBuilder.dustParticles(fractureCenter.clone().add(0, -1, 0), 12, 1.5, 0, 245, 255, 1.0f);
                DisplayBuilder.dustParticles(fractureCenter.clone().add(0, -2, 0), 8, 1.0, 0, 175, 155, 0.8f);

                // Soul fire laser connecting fracture endpoints
                double perpAngle = fractureAngle + Math.PI / 2;
                Location endA = fractureCenter.clone().add(Math.cos(perpAngle) * 2.5, 0, Math.sin(perpAngle) * 2.5);
                Location endB = fractureCenter.clone().add(Math.cos(perpAngle) * -2.5, 0, Math.sin(perpAngle) * -2.5);
                DisplayBuilder.dustParticles(endA, 6, 0.2, 0, 150, 255, 0.8f);
                DisplayBuilder.dustParticles(endB, 6, 0.2, 0, 150, 255, 0.8f);

                // Damage players standing on the section
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(fractureCenter) <= 9.0) {
                        p.damage(20.0); // 10 hearts
                        p.setVelocity(p.getVelocity().setY(-0.5));
                        DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                    }
                }
            }

            // Void particles continue rising from gap
            int dropTick = ticksAlive - 20;
            if (dropTick % 8 == 0) {
                DisplayBuilder.dustParticles(fractureCenter.clone().add(0, -0.5, 0), 5, 1.0, 0, 245, 255, 0.6f);
                w.spawnParticle(Particle.DRIPPING_LAVA, fractureCenter, 3, 1.0, 0, 1.0, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EdgeFracture(plugin); }
    }

    // =========================================================================
    // 82. VOID SURGE -- void energy rises through edge gaps
    // =========================================================================
    public static class VoidSurge extends EnvironmentalAttack {

        private final List<Location> gapLocations = new ArrayList<>();
        private boolean surging = false;

        public VoidSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_surge", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(260); // 13 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.4f);

            // 3-4 void gap positions at edges
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i + Math.random() * 0.3;
                gapLocations.add(center.clone().add(Math.cos(angle) * 15, -0.5, Math.sin(angle) * 15));
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
                if (ticksAlive % 6 == 0) {
                    for (Location gap : gapLocations) {
                        DisplayBuilder.dustParticles(gap.clone().add(0, 0.5, 0), 4, 0.5, 0, 180, 160, 0.5f);
                    }
                }
                return;
            }

            if (!surging) {
                surging = true;
                for (Location gap : gapLocations) {
                    DisplayBuilder.playSound(gap, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
                }
            }

            int surgeTick = ticksAlive - 30;

            // Void energy columns rising 10 blocks from each gap
            if (surgeTick % 3 == 0) {
                for (Location gap : gapLocations) {
                    for (int y = 0; y < 10; y++) {
                        Location colLoc = gap.clone().add(0, y + 0.5, 0);
                        DisplayBuilder.dustParticles(colLoc, 3, 0.3, 0, 180, 160, 0.7f);
                        DisplayBuilder.dustParticles(colLoc, 2, 0.2, 0, 255, 200, 0.6f);
                    }

                    // Crimson spore spirals where void meets brimstone
                    double spiralAngle = surgeTick * 0.15;
                    for (int s = 0; s < 4; s++) {
                        double sAngle = spiralAngle + (Math.PI / 2) * s;
                        Location spiralLoc = gap.clone().add(Math.cos(sAngle) * 2, 0.5, Math.sin(sAngle) * 2);
                        DisplayBuilder.dustParticles(spiralLoc, 2, 0.2, 185, 25, 15, 0.5f);
                    }
                }
            }

            // Spawn column displays
            if (surgeTick == 5) {
                for (Location gap : gapLocations) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(gap.clone().add(0, 5, 0), Material.CRYING_OBSIDIAN);
                    h.scale(1.5f, 8.0f, 1.5f).glow(0, 150, 255).interpolation(10, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            // Knockback + damage: players within 3 blocks of any gap edge
            if (surgeTick % 5 == 0) {
                for (Location gap : gapLocations) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(gap) <= 9.0) {
                            p.damage(18.0); // 9 hearts
                            // Knockback inward (toward center)
                            double dx = center.getX() - p.getLocation().getX();
                            double dz = center.getZ() - p.getLocation().getZ();
                            double dist = Math.sqrt(dx * dx + dz * dz);
                            if (dist > 0.1) {
                                p.setVelocity(p.getVelocity().add(
                                        new org.bukkit.util.Vector(dx / dist * 0.8, 0.3, dz / dist * 0.8)));
                            }
                            DisplayBuilder.dustParticles(p.getLocation(), 8, 0.4, 0, 255, 200, 1.0f);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSurge(plugin); }
    }

    // =========================================================================
    // 83. LAVA-VOID INTERFACE -- edge seam ejects projectile bursts
    // =========================================================================
    public static class LavaVoidInterface extends EnvironmentalAttack {

        private final List<Location> seamLocations = new ArrayList<>();
        private boolean eruptionActive = false;

        public LavaVoidInterface(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_void_interface", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(300); // 15 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Rapid basalt break series
            for (int i = 0; i < 7; i++) {
                int delay = i * 6;
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.7f, 0.6f), delay);
            }

            // Seam positions at island edges
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6 + Math.random() * 0.3;
                seamLocations.add(center.clone().add(Math.cos(angle) * 14.5, 0, Math.sin(angle) * 14.5));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5 seconds) -- seam brightens
            if (ticksAlive <= 30) {
                if (ticksAlive % 5 == 0) {
                    for (Location seam : seamLocations) {
                        DisplayBuilder.dustParticles(seam, 4, 0.3, 255, 80, 0, 0.8f);
                        DisplayBuilder.dustParticles(seam, 3, 0.2, 0, 255, 240, 0.6f);
                    }
                }
                return;
            }

            if (!eruptionActive) {
                eruptionActive = true;

                // Crying obsidian displays at seam points
                for (Location seam : seamLocations) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(seam, Material.CRYING_OBSIDIAN);
                    h.scale(1.2f, 0.3f, 1.2f).glow(200, 0, 50).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            int eruptTick = ticksAlive - 30;

            // Pulsing seam visuals
            if (eruptTick % 4 == 0) {
                boolean lavaPhase = (eruptTick / 8) % 2 == 0;
                for (Location seam : seamLocations) {
                    if (lavaPhase) {
                        DisplayBuilder.dustParticles(seam, 4, 0.3, 255, 80, 0, 0.9f);
                    } else {
                        DisplayBuilder.dustParticles(seam, 4, 0.3, 0, 255, 240, 0.9f);
                    }
                }
            }

            // Irregular explosions + ejection projectiles
            if (eruptTick % 5 == 0) {
                for (Location seam : seamLocations) {
                    if (Math.random() < 0.4) { // 2-3 ejections per second per seam
                        // Direction: inward from edge
                        double inAngle = Math.atan2(center.getZ() - seam.getZ(), center.getX() - seam.getX());
                        inAngle += (Math.random() - 0.5) * 0.6; // Spread

                        double range = 5 + Math.random() * 3; // 5-8 blocks inward
                        Location projectileTarget = seam.clone().add(
                                Math.cos(inAngle) * range, 0.5, Math.sin(inAngle) * range);

                        // Visual: burst particles from seam to target
                        w.spawnParticle(Particle.LAVA, projectileTarget, 5, 0.5, 0.3, 0.5, 0);
                        DisplayBuilder.dustParticles(projectileTarget, 4, 0.3, 255, 100, 0, 0.8f);
                        DisplayBuilder.dustParticles(seam, 3, 0.3, 0, 175, 155, 0.6f);

                        // Damage players in ejection path
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(projectileTarget) <= 4.0) {
                                p.damage(16.0); // 8 hearts
                                DisplayBuilder.crimsonDust(p.getLocation(), 8, 0.4);
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LavaVoidInterface(plugin); }
    }

    // =========================================================================
    // 84. CRUMBLE CASCADE -- chain-reaction wave of crumbling from corner
    // =========================================================================
    public static class CrumbleCascade extends EnvironmentalAttack {

        private Location cascadeOrigin;
        private double cascadeAngle;
        private boolean cascadeStarted = false;

        public CrumbleCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crumble_cascade", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(400); // 20 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random corner
            double[] corners = {Math.PI / 4, 3 * Math.PI / 4, 5 * Math.PI / 4, 7 * Math.PI / 4};
            cascadeAngle = corners[(int) (Math.random() * 4)];
            cascadeOrigin = center.clone().add(Math.cos(cascadeAngle) * 16, 0, Math.sin(cascadeAngle) * 16);

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds) -- corner flashes crimson
            if (ticksAlive <= 40) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(cascadeOrigin, 10, 1.0, 210, 40, 20, 0.8f);
                }
                return;
            }

            if (!cascadeStarted) {
                cascadeStarted = true;
                DisplayBuilder.playSound(cascadeOrigin, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
            }

            int cascTick = ticksAlive - 40;

            // Wave front advances at ~3 blocks/sec diagonally toward center
            double waveDist = cascTick * 0.15; // ~3 blocks per second
            double inwardAngle = Math.atan2(center.getZ() - cascadeOrigin.getZ(),
                    center.getX() - cascadeOrigin.getX());

            Location waveFront = cascadeOrigin.clone().add(
                    Math.cos(inwardAngle) * waveDist, 0, Math.sin(inwardAngle) * waveDist);

            // Wave visual -- spreading zone of disintegration
            if (cascTick % 3 == 0 && waveDist <= 12) {
                double perpAngle = inwardAngle + Math.PI / 2;
                double waveWidth = 2 + waveDist * 0.3;

                for (double w2 = -waveWidth; w2 <= waveWidth; w2 += 1.0) {
                    Location tileLoc = waveFront.clone().add(
                            Math.cos(perpAngle) * w2, 0.02, Math.sin(perpAngle) * w2);

                    // Sagging display
                    BlockDisplayHandle h = displayBuilder.spawnBlock(tileLoc, Material.BLACKSTONE);
                    h.scale(0.9f, 0.1f, 0.9f).glow(200, 0, 50).interpolation(3, 0);
                    spawnedEntities.add(h.entity());

                    // Lava below, soul above
                    w.spawnParticle(Particle.LAVA, tileLoc, 2, 0.3, 0.2, 0.3, 0);
                    DisplayBuilder.dustParticles(tileLoc.clone().add(0, 0.5, 0), 3, 0.3, 0, 240, 255, 0.7f);

                    // Make tiles sink
                    BlockDisplay bd = h.entity();
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (bd.isValid()) {
                            Location sinkLoc = bd.getLocation();
                            sinkLoc.setY(sinkLoc.getY() - 5);
                            bd.teleport(sinkLoc);
                        }
                    }, 15L);
                }

                // Wave front sound
                if (cascTick % 6 == 0) {
                    DisplayBuilder.playSound(waveFront, Sound.BLOCK_STONE_PLACE, 0.7f, 0.4f);
                }

                // Sky dims
                DisplayBuilder.dustParticles(waveFront.clone().add(0, 8, 0), 5, 3.0, 60, 30, 10, 1.0f);
            }

            // Damage players at wave front
            if (cascTick % 4 == 0 && waveDist <= 12) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(waveFront) <= 6.25) {
                        p.damage(20.0); // 10 hearts
                        DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrumbleCascade(plugin); }
    }

    // =========================================================================
    // 85. VOID TENDRILS -- tracking tendrils from edge gaps toward players
    // =========================================================================
    public static class VoidTendrils extends EnvironmentalAttack {

        private final Location[] tendrilPositions = new Location[5];
        private final Location[] tendrilOrigins = new Location[5];
        private boolean tendrilsActive = false;

        public VoidTendrils(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tendrils", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(280); // 14 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);

            // 5 tendril origins at edge gaps
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5 + Math.random() * 0.3;
                tendrilOrigins[i] = center.clone().add(Math.cos(angle) * 15, 0, Math.sin(angle) * 15);
                tendrilPositions[i] = tendrilOrigins[i].clone();
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
                if (ticksAlive % 6 == 0) {
                    for (Location origin : tendrilOrigins) {
                        if (origin == null) continue;
                        DisplayBuilder.dustParticles(origin.clone().add(0, 0.3, 0), 4, 0.5, 0, 255, 230, 0.5f);
                    }
                }
                return;
            }

            if (!tendrilsActive) {
                tendrilsActive = true;
            }

            // Find nearest player for tracking
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double d = p.getLocation().distanceSquared(center);
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = p;
                }
            }

            // Move tendrils toward nearest player at 3 bps with gentle curve
            for (int i = 0; i < 5; i++) {
                if (tendrilPositions[i] == null || tendrilOrigins[i] == null) continue;

                Location target = (nearest != null) ? nearest.getLocation() : center;
                double dx = target.getX() - tendrilPositions[i].getX();
                double dz = target.getZ() - tendrilPositions[i].getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist > 0.5) {
                    // Gentle curve correction (not perfect tracking)
                    tendrilPositions[i] = tendrilPositions[i].clone().add(
                            dx / dist * 0.15, 0, dz / dist * 0.15);
                }

                // Draw tendril line from origin to current position
                if (ticksAlive % 3 == 0) {
                    Location from = tendrilOrigins[i];
                    Location to = tendrilPositions[i];

                    // Leading edge: soul particles
                    DisplayBuilder.dustParticles(to, 4, 0.3, 0, 255, 230, 0.8f);
                    // Trailing: warped spore
                    DisplayBuilder.dustParticles(to.clone().add(
                            (from.getX() - to.getX()) * 0.1, 0,
                            (from.getZ() - to.getZ()) * 0.1), 3, 0.2, 0, 165, 145, 0.6f);
                }

                // Damage + slow on contact
                if (ticksAlive % 5 == 0 && nearest != null) {
                    if (nearest.getLocation().distanceSquared(tendrilPositions[i]) <= 2.25) {
                        nearest.damage(16.0); // 8 hearts
                        // Slow via velocity reduction
                        org.bukkit.util.Vector vel = nearest.getVelocity();
                        nearest.setVelocity(vel.setX(vel.getX() * 0.5).setZ(vel.getZ() * 0.5));
                        DisplayBuilder.dustParticles(nearest.getLocation(), 8, 0.4, 0, 255, 230, 1.0f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTendrils(plugin); }
    }

    // =========================================================================
    // 86. ISLAND TILT -- entire island appears to tilt, lava pools on low side
    // =========================================================================
    public static class IslandTiltAttack extends EnvironmentalAttack {

        private double tiltAngle;
        private boolean tilted = false;

        public IslandTiltAttack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("island_tilt", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140); // 7 seconds
            config.setCooldownTicks(340); // 17 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            tiltAngle = Math.random() * 2 * Math.PI;

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 8 == 0) {
                    // Pulsed particles suggesting directional shake
                    Location lowSide = center.clone().add(Math.cos(tiltAngle) * 10, 0, Math.sin(tiltAngle) * 10);
                    DisplayBuilder.dustParticles(lowSide, 6, 2.0, 255, 100, 0, 0.6f);
                }
                return;
            }

            if (!tilted) {
                tilted = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.4f);
            }

            int tiltTick = ticksAlive - 40;

            // Low-side lava particle flow
            Location lowSide = center.clone().add(Math.cos(tiltAngle) * 12, -0.5, Math.sin(tiltAngle) * 12);

            if (tiltTick % 3 == 0) {
                // Lava flowing "downhill"
                for (int i = 0; i < 10; i++) {
                    double perpAngle = tiltAngle + Math.PI / 2;
                    double perpDist = (Math.random() - 0.5) * 10;
                    double downDist = 6 + Math.random() * 8;
                    Location flowLoc = center.clone().add(
                            Math.cos(tiltAngle) * downDist + Math.cos(perpAngle) * perpDist,
                            -0.2,
                            Math.sin(tiltAngle) * downDist + Math.sin(perpAngle) * perpDist);
                    w.spawnParticle(Particle.LAVA, flowLoc.clone().add(0, 0.3, 0), 2, 0.3, 0.2, 0.3, 0);
                    DisplayBuilder.dustParticles(flowLoc, 2, 0.3, 255, 100, 0, 0.6f);
                }

                // Dripping lava over low-side edge
                for (int i = 0; i < 4; i++) {
                    double perpAngle = tiltAngle + Math.PI / 2;
                    Location dripLoc = lowSide.clone().add(
                            Math.cos(perpAngle) * (Math.random() - 0.5) * 10, 2, Math.sin(perpAngle) * (Math.random() - 0.5) * 10);
                    w.spawnParticle(Particle.DRIPPING_LAVA, dripLoc, 3, 0.3, 1.0, 0.3, 0);
                }
            }

            // High-side: subtle upward push on players (floaty feel via velocity)
            if (tiltTick % 5 == 0) {
                Location highSide = center.clone().add(Math.cos(tiltAngle + Math.PI) * 8, 0, Math.sin(tiltAngle + Math.PI) * 8);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = p.getLocation().getX() - center.getX();
                    double dz = p.getLocation().getZ() - center.getZ();
                    // Project player position onto tilt axis
                    double projection = dx * Math.cos(tiltAngle) + dz * Math.sin(tiltAngle);

                    if (projection < -3) {
                        // High side: floaty
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 0.08, 0)));
                    }
                }
            }

            // Lava pool damage on low side
            if (tiltTick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = p.getLocation().getX() - center.getX();
                    double dz = p.getLocation().getZ() - center.getZ();
                    double projection = dx * Math.cos(tiltAngle) + dz * Math.sin(tiltAngle);

                    if (projection > 6) { // Low side
                        p.damage(18.0); // 9 hearts in lava pool zone
                        DisplayBuilder.crimsonDust(p.getLocation(), 8, 0.5);
                    }
                }
            }

            // Tilt visual: blocks on floor shift slightly
            if (tiltTick == 5) {
                for (int i = 0; i < 8; i++) {
                    double dist = 8 + Math.random() * 6;
                    Location blockLoc = center.clone().add(
                            Math.cos(tiltAngle) * dist + (Math.random() - 0.5) * 4, 0.02,
                            Math.sin(tiltAngle) * dist + (Math.random() - 0.5) * 4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(blockLoc, Material.MAGMA_BLOCK);
                    float tilt = 0.1f;
                    h.scale(1.0f, 0.1f, 1.0f).glow(255, 100, 0)
                            .rotate(tilt, (float) Math.cos(tiltAngle), 0, (float) Math.sin(tiltAngle))
                            .interpolation(10, 0);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IslandTiltAttack(plugin); }
    }

    // =========================================================================
    // 87. EDGE INFERNO -- perimeter fire wall trapping players inside
    // =========================================================================
    public static class EdgeInferno extends EnvironmentalAttack {

        private boolean wallActive = false;

        public EdgeInferno(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("edge_inferno", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(380); // 19 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds) sound-only
            if (ticksAlive <= 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f + ticksAlive / 40.0f * 0.5f, 0.5f);
                }
                return;
            }

            // Instant wall appearance
            if (!wallActive) {
                wallActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);

                // Spawn fire wall ring displays
                int ringPoints = 20;
                for (int i = 0; i < ringPoints; i++) {
                    double angle = (2 * Math.PI * i) / ringPoints;
                    for (int y = 0; y < 6; y++) {
                        Location wallLoc = center.clone().add(Math.cos(angle) * 14, y, Math.sin(angle) * 14);
                        Material mat = (y % 2 == 0) ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(wallLoc, mat);
                        h.scale(2.0f, 1.0f, 0.5f).glow(255, 100, 0)
                                .rotate((float) angle, 0, 1, 0).interpolation(3, 0);
                        spawnedEntities.add(h.entity());
                    }

                    // Top fire charge displays at 4-block intervals
                    if (i % 5 == 0) {
                        Location topLoc = center.clone().add(Math.cos(angle) * 14, 6, Math.sin(angle) * 14);
                        BlockDisplayHandle topH = displayBuilder.spawnBlock(topLoc, Material.NETHER_WART_BLOCK);
                        topH.scale(0.8f, 0.8f, 0.8f).glow(200, 0, 50)
                                .rotate((float) (angle + ticksAlive * 0.05), 0, 1, 0).interpolation(10, 0);
                        spawnedEntities.add(topH.entity());
                    }
                }
            }

            // Continuous wall particle effects
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI * i) / 16;
                    Location wallLoc = center.clone().add(Math.cos(angle) * 14, Math.random() * 6, Math.sin(angle) * 14);
                    DisplayBuilder.dustParticles(wallLoc, 3, 0.3, 255, 90, 0, 0.8f);
                    DisplayBuilder.dustParticles(wallLoc, 2, 0.2, 0, 250, 215, 0.7f);
                }

                // Inner moat: 2-block-wide burning band
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI * i) / 12;
                    Location moatLoc = center.clone().add(Math.cos(angle) * 12.5, 0.3, Math.sin(angle) * 12.5);
                    w.spawnParticle(Particle.LAVA, moatLoc, 2, 0.5, 0.2, 0.5, 0);
                }
            }

            // Damage zones
            if (ticksAlive % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);

                    // Fire wall: passing through
                    if (dist >= 13.5 && dist <= 15) {
                        p.damage(12.0); // 12 hearts/sec through wall (devastating)
                        DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                    }
                    // Inner moat band
                    else if (dist >= 11.5 && dist <= 13.5) {
                        p.damage(14.0); // 7 hearts/sec in moat
                        DisplayBuilder.dustParticles(p.getLocation(), 6, 0.3, 255, 90, 0, 0.8f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EdgeInferno(plugin); }
    }

    // =========================================================================
    // 88. VOID QUAKE -- island shakes, three interior void holes open
    // =========================================================================
    public static class VoidQuake extends EnvironmentalAttack {

        private final Location[] voidHoles = new Location[3];
        private boolean quakeStarted = false;

        public VoidQuake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_quake", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(400); // 20 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.5f);

            // Three interior void hole positions (NOT at edges)
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3 + Math.random() * 0.5;
                double dist = 3 + Math.random() * 6; // Interior, not edges
                voidHoles[i] = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds) -- visual pulse + shaking
            if (ticksAlive <= 40) {
                if (ticksAlive % 4 == 0) {
                    // Visual shaking: random offset particles across arena
                    for (int i = 0; i < 12; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 14;
                        Location loc = center.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);
                        w.spawnParticle(Particle.LAVA, loc, 2, 0.5, 0.2, 0.5, 0);
                    }
                }
                return;
            }

            if (!quakeStarted) {
                quakeStarted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);

                // Open three interior void gaps
                for (Location hole : voidHoles) {
                    if (hole == null) continue;

                    // 2x2 void gap display
                    for (int x = 0; x <= 1; x++) {
                        for (int z = 0; z <= 1; z++) {
                            Location hloc = hole.clone().add(x - 0.5, -0.5, z - 0.5);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(hloc, Material.BLACK_CONCRETE);
                            h.scale(1.0f, 0.5f, 1.0f).glow(0, 150, 255).interpolation(5, 0);
                            spawnedEntities.add(h.entity());
                        }
                    }

                    // Soul + warped particles rising from void
                    DisplayBuilder.dustParticles(hole, 15, 1.0, 0, 255, 235, 1.0f);
                    DisplayBuilder.dustParticles(hole, 10, 0.8, 0, 175, 155, 0.8f);
                }

                // Soul fire laser triangle connecting the three holes
                for (int i = 0; i < 3; i++) {
                    Location from = voidHoles[i];
                    Location to = voidHoles[(i + 1) % 3];
                    if (from == null || to == null) continue;

                    int linePoints = 10;
                    for (int p = 0; p <= linePoints; p++) {
                        double t = (double) p / linePoints;
                        Location lineLoc = from.clone().add(
                                (to.getX() - from.getX()) * t, 0.2,
                                (to.getZ() - from.getZ()) * t);
                        DisplayBuilder.dustParticles(lineLoc, 2, 0.1, 0, 150, 255, 0.7f);
                    }
                }
            }

            int quakeTick = ticksAlive - 40;

            // Continuous vibration effects
            if (quakeTick % 3 == 0) {
                // Crack patterns across floor
                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 12;
                    Location crackLoc = center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(crackLoc, 2, 0.5, 140, 135, 125, 0.5f);
                }

                // Scattered lava from shaking
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 14;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 0.3, Math.sin(angle) * dist);
                    w.spawnParticle(Particle.LAVA, loc, 2, 0.5, 0.3, 0.5, 0);
                }
            }

            // Void hole particles continue
            if (quakeTick % 6 == 0) {
                for (Location hole : voidHoles) {
                    if (hole == null) continue;
                    DisplayBuilder.dustParticles(hole.clone().add(0, 0.5, 0), 5, 0.8, 0, 255, 235, 0.7f);
                }
            }

            // Damage: stepping into void holes
            if (quakeTick % 4 == 0) {
                for (Location hole : voidHoles) {
                    if (hole == null) continue;
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(hole) <= 2.25) {
                            p.damage(24.0); // 12 hearts contact
                            p.setVelocity(p.getVelocity().setY(-0.5)); // Void pull
                            DisplayBuilder.dustParticles(p.getLocation(), 10, 0.5, 0, 255, 235, 1.0f);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidQuake(plugin); }
    }

    // =========================================================================
    // 89. THE LAST LEDGE -- three simultaneous edge collapses
    // =========================================================================
    public static class TheLastLedge extends EnvironmentalAttack {

        private final Location[] collapseLocations = new Location[3];
        private boolean collapsed = false;

        public TheLastLedge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_last_ledge", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(200); // 10 seconds (brief before Group 10)
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.5f);

            // Three edge collapse positions
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3 + Math.random() * 0.3;
                collapseLocations[i] = center.clone().add(Math.cos(angle) * 14, 0, Math.sin(angle) * 14);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 60 ticks (3 seconds) -- sustained scream
            if (ticksAlive <= 60) {
                if (ticksAlive % 10 == 0) {
                    for (Location loc : collapseLocations) {
                        if (loc == null) continue;
                        // Lava + soul fire at edges
                        w.spawnParticle(Particle.LAVA, loc.clone().add(0, 0.5, 0), 5, 2.0, 0.3, 2.0, 0);
                        DisplayBuilder.dustParticles(loc, 6, 1.5, 0, 240, 220, 0.7f);
                        // Crimson spore ash falling
                        DisplayBuilder.dustParticles(loc.clone().add(0, 3, 0), 4, 2.0, 200, 35, 18, 0.5f);
                    }
                }
                return;
            }

            // Three simultaneous collapses
            if (!collapsed) {
                collapsed = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);

                for (Location loc : collapseLocations) {
                    if (loc == null) continue;

                    double angle = Math.atan2(loc.getZ() - center.getZ(), loc.getX() - center.getX());
                    double perpAngle = angle + Math.PI / 2;

                    // Collapsing section displays: 5 blocks wide, sinking
                    for (int i = -2; i <= 2; i++) {
                        Location tileLoc = loc.clone().add(Math.cos(perpAngle) * i, 0.05, Math.sin(perpAngle) * i);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(tileLoc, Material.BLACKSTONE);
                        h.scale(1.0f, 0.15f, 1.0f).glow(200, 0, 50).interpolation(5, 0);
                        spawnedEntities.add(h.entity());

                        // Sink after short delay
                        BlockDisplay bd = h.entity();
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (bd.isValid()) {
                                Location sinkLoc = bd.getLocation();
                                sinkLoc.setY(sinkLoc.getY() - 15);
                                bd.teleport(sinkLoc);
                            }
                        }, 10L);
                    }

                    // Soul fire halo on remaining edge blocks
                    DisplayBuilder.dustParticles(loc, 15, 2.0, 0, 240, 220, 1.0f);

                    // Lava + dripping lava from exposed edges
                    w.spawnParticle(Particle.LAVA, loc, 15, 2.0, 0.5, 2.0, 0);
                    w.spawnParticle(Particle.DRIPPING_LAVA, loc.clone().add(0, -1, 0), 10, 2.0, 0, 2.0, 0);

                    // Ash rising from void
                    DisplayBuilder.dustParticles(loc.clone().add(0, -2, 0), 8, 1.5, 140, 135, 125, 0.6f);

                    // Damage players on collapsing sections
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(loc) <= 9.0) {
                            p.damage(20.0); // 10 hearts
                            DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                            // Push inward
                            double dx = center.getX() - p.getLocation().getX();
                            double dz = center.getZ() - p.getLocation().getZ();
                            double dist = Math.sqrt(dx * dx + dz * dz);
                            if (dist > 0.1) {
                                p.setVelocity(p.getVelocity().add(
                                        new org.bukkit.util.Vector(dx / dist * 0.6, 0.3, dz / dist * 0.6)));
                            }
                        }
                    }
                }
            }

            int collapseTick = ticksAlive - 60;

            // Continued void particles from gaps
            if (collapseTick % 8 == 0) {
                for (Location loc : collapseLocations) {
                    if (loc == null) continue;
                    DisplayBuilder.dustParticles(loc.clone().add(0, -0.5, 0), 5, 2.0, 0, 255, 240, 0.6f);
                }

                // Crimson ash from sky
                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 14;
                    Location ashLoc = center.clone().add(Math.cos(angle) * dist, 5, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(ashLoc, 2, 0.5, 200, 35, 18, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLastLedge(plugin); }
    }

    // =========================================================================
    // 90. THRESHOLD -- narrative transition, Dweller exhale cloud
    // =========================================================================
    public static class Threshold extends EnvironmentalAttack {

        private boolean exhaled = false;

        public Threshold(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("threshold", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(160); // 8 seconds (Group 10 immediately after)
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // 3 seconds of total stillness -- no sounds, no particles
            // Silence IS the warning
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Stillness: 60 ticks (3 seconds) -- absolute silence
            if (ticksAlive <= 60) {
                // Nothing. Eerie calm. Sky pulses once at tick 50
                if (ticksAlive == 50) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 15, 0), 20, 12.0, 150, 40, 10, 2.0f);
                }
                return;
            }

            // THE EXHALE
            if (!exhaled) {
                exhaled = true;

                // Maximum volume wither ambient
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.3f);

                // Massive breath cloud: soul fire + crimson spore
                for (int i = 0; i < 40; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 15;
                    Location cloudLoc = center.clone().add(
                            Math.cos(angle) * dist, 1.5 + Math.random() * 1.0, Math.sin(angle) * dist);

                    DisplayBuilder.dustParticles(cloudLoc, 4, 0.5, 0, 255, 200, 1.0f);
                    DisplayBuilder.dustParticles(cloudLoc, 3, 0.4, 230, 50, 20, 0.8f);
                }

                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 2, 0), 40, 10.0, 1.0, 10.0, 0.05);

                // Cloud damage: 8 hearts to all
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    p.damage(16.0); // 8 hearts
                    DisplayBuilder.dustParticles(p.getLocation(), 8, 0.5, 0, 255, 200, 1.0f);
                    DisplayBuilder.dustParticles(p.getLocation(), 6, 0.4, 230, 50, 20, 0.8f);
                }
            }

            int exhaleTick = ticksAlive - 60;

            // Cloud dissipation over 2 seconds, revealing 150% particle output
            if (exhaleTick % 3 == 0 && exhaleTick <= 40) {
                float density = Math.max(0.3f, 1.0f - exhaleTick / 40.0f);
                for (int i = 0; i < (int) (15 * density); i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 14;
                    Location cloudLoc = center.clone().add(
                            Math.cos(angle) * dist, 1.5, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(cloudLoc, 2, 0.5, 0, 255, 200, 0.6f * density);
                }
            }

            // After cloud clears: all particle systems restart at 150%
            if (exhaleTick > 30 && exhaleTick % 4 == 0) {
                for (int i = 0; i < 15; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 14;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 0.3, Math.sin(angle) * dist);
                    w.spawnParticle(Particle.LAVA, loc.clone().add(0, 0.5, 0), 3, 0.3, 0.5, 0.3, 0);
                    DisplayBuilder.dustParticles(loc, 3, 0.3, 255, 100, 0, 0.8f);
                }
            }

            // Item displays reorient toward nearest player
            if (exhaleTick == 35) {
                for (int i = 0; i < 6; i++) {
                    double angle = (2 * Math.PI * i) / 6;
                    double dist = 4 + Math.random() * 8;
                    Location dLoc = center.clone().add(Math.cos(angle) * dist, 0.05, Math.sin(angle) * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(dLoc, Material.MAGMA_BLOCK);
                    h.scale(1.0f, 0.1f, 1.0f).glow(200, 0, 50).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Threshold(plugin); }
    }
}
