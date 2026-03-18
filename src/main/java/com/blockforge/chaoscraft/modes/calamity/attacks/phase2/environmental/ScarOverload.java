package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.environmental;

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
 * GROUP 6: SCAR OVERLOAD EVENTS (Attacks 51-60)
 * Phase 2 (50% HP) — accumulated scars from Phase 1 becoming devastating.
 * DoG's passage scars weaponized: chain detonations, convergence beams,
 * lattice webs, memory paths, overcharges, pulse networks, eruption volleys,
 * meltdowns, total ignitions, and singularities.
 *
 * Palette: cyan (0,200,255), violet (128,0,255), white (240,240,255)
 * Materials: AMETHYST_BLOCK, SEA_LANTERN, END_ROD, CRYING_OBSIDIAN
 */
public final class ScarOverload {

    private ScarOverload() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ScarDetonationChain(plugin));
        registry.register(new ScarConvergence(plugin));
        registry.register(new ScarLattice(plugin));
        registry.register(new ScarMemory(plugin));
        registry.register(new OverchargedScar(plugin));
        registry.register(new ScarPulseNetwork(plugin));
        registry.register(new ScarEruptionVolley(plugin));
        registry.register(new ScarMeltdown(plugin));
        registry.register(new TotalScarIgnition(plugin));
        registry.register(new ScarSingularity(plugin));
    }

    // =========================================================================
    // 51. SCAR DETONATION CHAIN
    // A chain reaction fires from scar to scar across the arena. Each scar
    // detonates in sequence with white spark bursts, traveling outward like a
    // chain of white lightning. Players must keep moving to stay ahead.
    // 12 HP per detonation hit, 50s cooldown.
    // =========================================================================
    public static class ScarDetonationChain extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> scarPoints = new ArrayList<>();
        private int currentScar = 0;
        private int ticksSinceLastDetonation = 0;

        public ScarDetonationChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_detonation_chain", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(1000);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Generate 10 scar detonation points in a chain radiating outward
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10 + Math.random() * 0.3;
                double dist = 4.0 + i * 2.0;
                Location scarLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                scarPoints.add(scarLoc);
                // Scar marker block
                BlockDisplayHandle h = displayBuilder.spawnBlock(scarLoc, Material.AMETHYST_BLOCK);
                h.scale(1.2f, 0.15f, 1.2f).glow(0, 200, 255).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // First 40 ticks: warning — all scars pulse cyan
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    for (Location sc : scarPoints) {
                        DisplayBuilder.cyanDust(sc, 6, 0.8);
                    }
                }
                return;
            }

            // Chain detonation: one scar every 6 ticks
            ticksSinceLastDetonation++;
            if (currentScar < scarPoints.size() && ticksSinceLastDetonation >= 6) {
                ticksSinceLastDetonation = 0;
                Location det = scarPoints.get(currentScar);
                // Detonation burst
                DisplayBuilder.dustParticles(det, 30, 2.5, 240, 240, 255, 1.5f);
                DisplayBuilder.cyanDust(det, 20, 2.0);
                DisplayBuilder.playSound(det, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);

                // Flash the block white then shrink
                if (currentScar < handles.size()) {
                    handles.get(currentScar).glow(240, 240, 255);
                    handles.get(currentScar).scale(2.0f, 0.5f, 2.0f).interpolation(3, 0);
                }

                // Draw spark line to next scar
                if (currentScar + 1 < scarPoints.size()) {
                    Location next = scarPoints.get(currentScar + 1);
                    DisplayBuilder.particleLine(det, next, Particle.ELECTRIC_SPARK, 3, null);
                }

                // Damage at detonation point
                World w = det.getWorld();
                if (w != null) {
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(det) <= 16.0) {
                            p.damage(config.getDamage());
                        }
                    }
                }
                currentScar++;
            }

            // Ambient crackling particles on remaining undetonated scars
            if (ticksAlive % 10 == 0) {
                for (int i = currentScar; i < scarPoints.size(); i++) {
                    DisplayBuilder.cyanDust(scarPoints.get(i), 4, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarDetonationChain(plugin); }
    }

    // =========================================================================
    // 52. SCAR CONVERGENCE
    // All scars emit white particle beams toward the arena center, forming a
    // star pattern of hazard lines. Contact with any beam deals heavy damage.
    // 14 HP per beam intersection, 55s cooldown.
    // =========================================================================
    public static class ScarConvergence extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> beamOrigins = new ArrayList<>();
        private boolean beamsActive = false;

        public ScarConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_convergence", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Create 8 scar beam origins in a ring
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double dist = 16.0;
                Location origin = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                beamOrigins.add(origin);
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin, Material.SEA_LANTERN);
                h.scale(0.8f, 0.3f, 0.8f).glow(240, 240, 255).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning phase: 60 ticks (3s) — scar nodes pulse
            if (ticksAlive < 60) {
                if (ticksAlive % 10 == 0) {
                    for (Location origin : beamOrigins) {
                        DisplayBuilder.dustParticles(origin, 8, 1.0, 240, 240, 255, 1.4f);
                    }
                }
                return;
            }

            beamsActive = true;

            // Draw convergence beams every 4 ticks
            if (ticksAlive % 4 == 0) {
                for (Location origin : beamOrigins) {
                    DisplayBuilder.particleLine(origin.clone().add(0, 0.5, 0),
                            center.clone().add(0, 0.5, 0), Particle.END_ROD, 2, null);
                    DisplayBuilder.cyanDust(origin, 4, 0.6);
                }
                DisplayBuilder.dustParticles(center, 12, 1.5, 240, 240, 255, 1.6f);
            }

            // Beam contact damage check — check players near any beam line
            if (ticksAlive % 20 == 0 && beamsActive) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    for (Location origin : beamOrigins) {
                        if (isNearLine(pLoc, origin, center, 2.0)) {
                            p.damage(config.getDamage());
                            break;
                        }
                    }
                }
            }

            // Ambient hum
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
            }
        }

        /** Check if a point is near a line between two locations. */
        private boolean isNearLine(Location point, Location lineStart, Location lineEnd, double threshold) {
            double dx = lineEnd.getX() - lineStart.getX();
            double dz = lineEnd.getZ() - lineStart.getZ();
            double len2 = dx * dx + dz * dz;
            if (len2 < 0.01) return point.distanceSquared(lineStart) <= threshold * threshold;
            double t = Math.max(0, Math.min(1,
                    ((point.getX() - lineStart.getX()) * dx + (point.getZ() - lineStart.getZ()) * dz) / len2));
            double closestX = lineStart.getX() + t * dx;
            double closestZ = lineStart.getZ() + t * dz;
            double distSq = (point.getX() - closestX) * (point.getX() - closestX)
                    + (point.getZ() - closestZ) * (point.getZ() - closestZ);
            return distSq <= threshold * threshold;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarConvergence(plugin); }
    }

    // =========================================================================
    // 53. SCAR LATTICE
    // White spark lines form a web between scars at knee height (0.8 blocks),
    // creating a dense floor hazard that forces continuous jumping. Contact
    // with any lattice line deals damage.
    // 12 HP per line contact, 60s cooldown.
    // =========================================================================
    public static class ScarLattice extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> latticeNodes = new ArrayList<>();
        private boolean latticeActive = false;

        public ScarLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_lattice", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Create 12 lattice nodes in staggered pattern
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12 + (i % 2 == 0 ? 0.2 : -0.2);
                double dist = 5.0 + (i % 3) * 5.0;
                Location node = center.clone().add(Math.cos(angle) * dist, 0.8, Math.sin(angle) * dist);
                latticeNodes.add(node);
                BlockDisplayHandle h = displayBuilder.spawnBlock(node, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.1f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: nodes glow for 40 ticks
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    for (Location node : latticeNodes) {
                        DisplayBuilder.dustParticles(node, 5, 0.5, 240, 240, 255, 1.0f);
                    }
                }
                return;
            }

            latticeActive = true;

            // Draw lattice lines between adjacent nodes every 5 ticks
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < latticeNodes.size(); i++) {
                    int next = (i + 1) % latticeNodes.size();
                    DisplayBuilder.particleLine(latticeNodes.get(i), latticeNodes.get(next),
                            Particle.ELECTRIC_SPARK, 2, null);
                    // Cross-connections for denser web
                    if (i + 3 < latticeNodes.size()) {
                        DisplayBuilder.particleLine(latticeNodes.get(i), latticeNodes.get(i + 3),
                                Particle.ELECTRIC_SPARK, 1, null);
                    }
                }
            }

            // Lattice contact damage — players at 0.8-block height near any line
            if (ticksAlive % 15 == 0 && latticeActive) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    // Only triggers if player is near lattice height (on ground, not jumping)
                    if (Math.abs(pLoc.getY() - latticeNodes.get(0).getY()) > 1.5) continue;
                    for (int i = 0; i < latticeNodes.size(); i++) {
                        int next = (i + 1) % latticeNodes.size();
                        if (isNearLine2D(pLoc, latticeNodes.get(i), latticeNodes.get(next), 1.5)) {
                            p.damage(config.getDamage());
                            DisplayBuilder.cyanDust(pLoc, 8, 0.5);
                            break;
                        }
                    }
                }
            }

            // Ambient crackle
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 1.4f);
            }
        }

        private boolean isNearLine2D(Location p, Location a, Location b, double threshold) {
            double dx = b.getX() - a.getX();
            double dz = b.getZ() - a.getZ();
            double len2 = dx * dx + dz * dz;
            if (len2 < 0.01) return p.distanceSquared(a) <= threshold * threshold;
            double t = Math.max(0, Math.min(1,
                    ((p.getX() - a.getX()) * dx + (p.getZ() - a.getZ()) * dz) / len2));
            double cx = a.getX() + t * dx;
            double cz = a.getZ() + t * dz;
            double dist2 = (p.getX() - cx) * (p.getX() - cx) + (p.getZ() - cz) * (p.getZ() - cz);
            return dist2 <= threshold * threshold;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarLattice(plugin); }
    }

    // =========================================================================
    // 54. SCAR MEMORY
    // Every previous DoG path lights up simultaneously — a dense white glow
    // network segments the island into small safe zones. All movement paths
    // become contact-damage zones.
    // 12 HP per second of contact, 70s cooldown.
    // =========================================================================
    public static class ScarMemory extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> pathPoints = new ArrayList<>();
        private boolean memoryActive = false;

        public ScarMemory(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_memory", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(1400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Generate a dense network of "memory" path segments
            for (int i = 0; i < 20; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 3.0 + Math.random() * 14.0;
                Location point = center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                pathPoints.add(point);
                BlockDisplayHandle h = displayBuilder.spawnBlock(point, Material.SEA_LANTERN);
                h.scale(1.5f, 0.08f, 0.6f)
                 .rotate((float)(angle), 0, 1, 0)
                 .glow(240, 240, 255)
                 .interpolation(3, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 40 ticks — paths begin to glow
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    for (Location pt : pathPoints) {
                        DisplayBuilder.dustParticles(pt, 4, 0.8, 240, 240, 255, 1.0f);
                    }
                }
                return;
            }

            memoryActive = true;

            // Active phase: paths glow intensely
            if (ticksAlive % 6 == 0) {
                for (Location pt : pathPoints) {
                    DisplayBuilder.dustParticles(pt, 6, 1.0, 240, 240, 255, 1.4f);
                    DisplayBuilder.cyanDust(pt, 3, 0.6);
                }
            }

            // Ambient circuit-board crackling
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarMemory(plugin); }
    }

    // =========================================================================
    // 55. OVERCHARGED SCAR
    // One scar expands to 3 blocks wide, triples spark density, then fires
    // two perpendicular shockwaves that travel outward at 6 blocks/second.
    // 16 HP per shockwave hit, 45s cooldown.
    // =========================================================================
    public static class OverchargedScar extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean firstWaveFired = false;
        private boolean secondWaveFired = false;
        private double scarAngle;
        private Location scarCenter;

        public OverchargedScar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("overcharged_scar", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(16.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(340);
            config.setCooldownTicks(900);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            scarAngle = Math.random() * Math.PI;
            scarCenter = center.clone();
            // Scar body: wide amethyst strip
            for (int i = -4; i <= 4; i++) {
                Location loc = center.clone().add(
                        Math.cos(scarAngle) * i, 0,
                        Math.sin(scarAngle) * i);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(3.0f, 0.2f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Light column
            for (int y = 0; y < 12; y++) {
                Location colLoc = center.clone().add(0, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(colLoc, Material.SEA_LANTERN);
                h.scale(0.3f, 1.0f, 0.3f).glow(240, 240, 255).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning pulses: 3 pulses over 60 ticks
            if (ticksAlive < 60) {
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.dustParticles(scarCenter, 30, 3.0, 240, 240, 255, 1.6f);
                    DisplayBuilder.playSound(scarCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f,
                            0.6f + (ticksAlive / 60.0f) * 0.8f);
                }
                // Triple spark density on scar
                if (ticksAlive % 4 == 0) {
                    for (int i = -4; i <= 4; i++) {
                        Location loc = scarCenter.clone().add(
                                Math.cos(scarAngle) * i, 0.3,
                                Math.sin(scarAngle) * i);
                        DisplayBuilder.cyanDust(loc, 8, 0.5);
                    }
                }
                return;
            }

            // First shockwave at tick 60
            if (!firstWaveFired) {
                firstWaveFired = true;
                fireShockwave(ticksAlive);
            }

            // Second shockwave at tick 200
            if (ticksAlive >= 200 && !secondWaveFired) {
                secondWaveFired = true;
                fireShockwave(ticksAlive);
            }

            // Animate perpendicular shockwave expansion
            int waveTick1 = ticksAlive - 60;
            if (waveTick1 > 0 && waveTick1 < 60) {
                double waveRadius = waveTick1 * 0.3; // ~6 blocks/second at 20tps
                double perpAngle = scarAngle + Math.PI / 2;
                for (int side = -1; side <= 1; side += 2) {
                    Location wavePoint = scarCenter.clone().add(
                            Math.cos(perpAngle) * waveRadius * side, 0.5,
                            Math.sin(perpAngle) * waveRadius * side);
                    DisplayBuilder.dustParticles(wavePoint, 10, 1.5, 240, 240, 255, 1.5f);
                    DisplayBuilder.cyanDust(wavePoint, 6, 1.0);
                }
            }

            // Column glow
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(scarCenter.clone().add(0, 6, 0), 8, 2.0,
                        240, 240, 255, 1.2f);
            }
        }

        private void fireShockwave(int tick) {
            DisplayBuilder.playSound(scarCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
            DisplayBuilder.dustParticles(scarCenter, 40, 4.0, 240, 240, 255, 2.0f);
            World w = scarCenter.getWorld();
            if (w == null) return;
            for (var p : w.getPlayers()) {
                if (isExempt(p)) continue;
                if (p.getLocation().distanceSquared(scarCenter) <= 12.25) {
                    p.damage(config.getDamage());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OverchargedScar(plugin); }
    }

    // =========================================================================
    // 56. SCAR PULSE NETWORK
    // Three scars form a triangle. Pulse beams travel from scar to scar,
    // and every 2 seconds a radial damage wave emanates from the triangle's
    // centroid. Players inside the triangle take full damage per pulse.
    // 12 HP per pulse wave, 50s cooldown.
    // =========================================================================
    public static class ScarPulseNetwork extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final Location[] triangle = new Location[3];
        private Location centroid;
        private int pulseCount = 0;

        public ScarPulseNetwork(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_pulse_network", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(1000);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double baseAngle = Math.random() * 2 * Math.PI;
            double cx = 0, cz = 0;
            for (int i = 0; i < 3; i++) {
                double angle = baseAngle + (2 * Math.PI * i) / 3;
                double dist = 8.0 + Math.random() * 4.0;
                triangle[i] = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                cx += triangle[i].getX();
                cz += triangle[i].getZ();
                BlockDisplayHandle h = displayBuilder.spawnBlock(triangle[i], Material.AMETHYST_BLOCK);
                h.scale(1.5f, 0.3f, 1.5f).glow(0, 200, 255).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            centroid = center.clone();
            centroid.setX(cx / 3.0);
            centroid.setZ(cz / 3.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 40 ticks — triangle nodes flash
            if (ticksAlive < 40) {
                if (ticksAlive % 10 == 0) {
                    for (Location node : triangle) {
                        DisplayBuilder.dustParticles(node, 8, 1.0, 240, 240, 255, 1.2f);
                    }
                }
                return;
            }

            // Draw triangle edges every 4 ticks
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 3; i++) {
                    int next = (i + 1) % 3;
                    DisplayBuilder.particleLine(triangle[i].clone().add(0, 0.3, 0),
                            triangle[next].clone().add(0, 0.3, 0), Particle.END_ROD, 2, null);
                }
            }

            // Pulse wave from centroid every 40 ticks (2 seconds)
            if ((ticksAlive - 40) % 40 == 0 && pulseCount < 6) {
                pulseCount++;
                DisplayBuilder.dustParticles(centroid, 25, 3.0, 240, 240, 255, 1.8f);
                DisplayBuilder.cyanDust(centroid, 15, 2.5);
                DisplayBuilder.particleRing(centroid, 5.0, Particle.ELECTRIC_SPARK, 24, null);
                DisplayBuilder.playSound(centroid, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.2f);

                // Damage players inside triangle radius
                World w = centroid.getWorld();
                if (w != null) {
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(centroid) <= 25.0) {
                            p.damage(config.getDamage());
                        }
                    }
                }
            }

            // Traveling pulse visual along edges
            if (ticksAlive % 6 == 0) {
                float t = ((ticksAlive - 40) % 40) / 40.0f;
                for (int i = 0; i < 3; i++) {
                    int next = (i + 1) % 3;
                    double px = triangle[i].getX() + (triangle[next].getX() - triangle[i].getX()) * t;
                    double pz = triangle[i].getZ() + (triangle[next].getZ() - triangle[i].getZ()) * t;
                    Location pulsePoint = centroid.clone();
                    pulsePoint.setX(px);
                    pulsePoint.setZ(pz);
                    pulsePoint.setY(triangle[i].getY() + 0.5);
                    DisplayBuilder.cyanDust(pulsePoint, 6, 0.4);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarPulseNetwork(plugin); }
    }

    // =========================================================================
    // 57. SCAR ERUPTION VOLLEY
    // 8 scar segments erupt crystal columns straight up to Y+25. Each column
    // is a contact-damage pillar of white light driving aerial players down.
    // 12 HP per pillar contact, 40s cooldown.
    // =========================================================================
    public static class ScarEruptionVolley extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> pillarBases = new ArrayList<>();
        private boolean erupted = false;

        public ScarEruptionVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_eruption_volley", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8 + Math.random() * 0.3;
                double dist = 4.0 + Math.random() * 10.0;
                Location base = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                pillarBases.add(base);
                // Scar segment marker
                BlockDisplayHandle h = displayBuilder.spawnBlock(base, Material.AMETHYST_BLOCK);
                h.scale(1.0f, 0.15f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 30 ticks — all 8 segments flash white
            if (ticksAlive < 30) {
                if (ticksAlive % 6 == 0) {
                    for (Location base : pillarBases) {
                        DisplayBuilder.dustParticles(base, 10, 0.8, 240, 240, 255, 1.3f);
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.0f);
                }
                return;
            }

            // Eruption: spawn pillar columns
            if (!erupted) {
                erupted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
                for (Location base : pillarBases) {
                    for (int y = 0; y < 25; y += 2) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                base.clone().add(0, y, 0), Material.SEA_LANTERN);
                        h.scale(0.5f, 2.0f, 0.5f).glow(240, 240, 255).interpolation(3, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Pillar particle effects
            if (ticksAlive % 5 == 0) {
                for (Location base : pillarBases) {
                    double y = Math.random() * 25;
                    DisplayBuilder.dustParticles(base.clone().add(0, y, 0), 6, 0.8,
                            240, 240, 255, 1.4f);
                    DisplayBuilder.cyanDust(base.clone().add(0, y, 0), 4, 0.5);
                }
            }

            // Pillar contact damage — full column
            if (ticksAlive % 20 == 0) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    for (Location base : pillarBases) {
                        double dx = pLoc.getX() - base.getX();
                        double dz = pLoc.getZ() - base.getZ();
                        double hDist2 = dx * dx + dz * dz;
                        double py = pLoc.getY() - base.getY();
                        if (hDist2 <= 4.0 && py >= 0 && py <= 25) {
                            p.damage(config.getDamage());
                            break;
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarEruptionVolley(plugin); }
    }

    // =========================================================================
    // 58. SCAR MELTDOWN
    // A scar dissolves into liquid — cyan particles stream downward and a
    // glowing pool expands outward at 1 block/second. The pool is a contact-
    // damage zone that slowly cuts off escape routes.
    // 14 HP per second of contact, 55s cooldown.
    // =========================================================================
    public static class ScarMeltdown extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location meltCenter;
        private double poolRadius = 0;
        private boolean spreading = false;

        public ScarMeltdown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_meltdown", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(14.0);
            config.setDamageRadius(0.0); // Custom radius check
            config.setDurationTicks(300);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * 2 * Math.PI;
            double dist = 3.0 + Math.random() * 8.0;
            meltCenter = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            // Initial scar block
            BlockDisplayHandle h = displayBuilder.spawnBlock(meltCenter, Material.AMETHYST_BLOCK);
            h.scale(2.0f, 0.2f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
            handles.add(h);
            spawnedEntities.add(h.entity());
            DisplayBuilder.playSound(meltCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 40 ticks — scar begins dissolving
            if (ticksAlive < 40) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(meltCenter.clone().add(0, 0.5, 0), 8, 1.0);
                    DisplayBuilder.dustParticles(meltCenter, 4, 0.5, 0, 200, 255, 1.0f);
                }
                return;
            }

            spreading = true;

            // Spread: 1 block per second = 1 block per 20 ticks
            if (ticksAlive < 200) {
                poolRadius = (ticksAlive - 40) / 20.0;
            }
            // Hold phase (tick 200-300): pool stays at max radius

            // Pool visual — rings of glowing blocks at expanding radius
            if (ticksAlive % 10 == 0 && spreading) {
                // Add pool edge blocks as it grows
                if (poolRadius > 0) {
                    int segments = Math.max(8, (int)(poolRadius * 4));
                    for (int i = 0; i < segments; i++) {
                        double a = (2 * Math.PI * i) / segments;
                        Location edgeLoc = meltCenter.clone().add(
                                Math.cos(a) * poolRadius, 0.05,
                                Math.sin(a) * poolRadius);
                        DisplayBuilder.cyanDust(edgeLoc, 4, 0.3);
                    }
                    // Pool surface particles
                    DisplayBuilder.particleRing(meltCenter.clone().add(0, 0.1, 0),
                            poolRadius * 0.6, Particle.ELECTRIC_SPARK, segments / 2, null);
                }
            }

            // Pool contact damage
            if (ticksAlive % 20 == 0 && spreading && poolRadius > 0.5) {
                World w = meltCenter.getWorld();
                if (w == null) return;
                double r2 = poolRadius * poolRadius;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = p.getLocation().getX() - meltCenter.getX();
                    double dz = p.getLocation().getZ() - meltCenter.getZ();
                    if (dx * dx + dz * dz <= r2 &&
                        Math.abs(p.getLocation().getY() - meltCenter.getY()) < 2.0) {
                        p.damage(config.getDamage());
                        DisplayBuilder.cyanDust(p.getLocation(), 6, 0.4);
                    }
                }
            }

            // Ambient drip sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(meltCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarMeltdown(plugin); }
    }

    // =========================================================================
    // 59. TOTAL SCAR IGNITION
    // Every scar on the island ignites — the surface is lit by white spark
    // discharge from every groove. Safe zones are only unscarred patches.
    // 12 HP per second of contact, 70s cooldown.
    // =========================================================================
    public static class TotalScarIgnition extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> ignitionPoints = new ArrayList<>();
        private boolean ignited = false;

        public TotalScarIgnition(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("total_scar_ignition", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(1400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dense grid of ignition scars — 16 points radiating outward
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                for (int r = 3; r <= 15; r += 4) {
                    Location pt = center.clone().add(Math.cos(angle) * r, 0.05, Math.sin(angle) * r);
                    ignitionPoints.add(pt);
                }
            }
            // Spawn visible scar strips
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                Location stripLoc = center.clone().add(Math.cos(angle) * 8, 0, Math.sin(angle) * 8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(stripLoc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.1f, 8.0f)
                 .rotate((float)angle, 0, 1, 0)
                 .glow(0, 200, 255)
                 .interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 40 ticks
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center, 20, 8.0, 240, 240, 255, 1.2f);
                }
                return;
            }

            // Ignition flash
            if (!ignited) {
                ignited = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                // Switch all scar blocks to white glow
                for (BlockDisplayHandle h : handles) {
                    h.glow(240, 240, 255);
                }
            }

            // Active ignition: dense spark discharge everywhere
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    int idx = (int)(Math.random() * ignitionPoints.size());
                    Location pt = ignitionPoints.get(idx);
                    DisplayBuilder.dustParticles(pt, 8, 0.6, 240, 240, 255, 1.5f);
                }
            }

            // Electric spark bursts
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < 4; i++) {
                    int idx = (int)(Math.random() * ignitionPoints.size());
                    Location pt = ignitionPoints.get(idx).clone().add(0, 0.3, 0);
                    World w = pt.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.ELECTRIC_SPARK, pt, 12, 0.5, 0.3, 0.5, 0.02);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TotalScarIgnition(plugin); }
    }

    // =========================================================================
    // 60. SCAR SINGULARITY
    // All scar particles reverse — sucked inward toward the scars. Players
    // are pulled toward the nearest scar at 0.5 blocks/sec. After 5 seconds,
    // the singularity releases, dealing burst damage from each scar.
    // 16 HP from release burst in 6-block radius per scar, 75s cooldown.
    // =========================================================================
    public static class ScarSingularity extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> scarLocations = new ArrayList<>();
        private boolean released = false;

        public ScarSingularity(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_singularity", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(16.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(1500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 6 scar points arranged around center
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double dist = 5.0 + Math.random() * 8.0;
                Location scar = center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                scarLocations.add(scar);
                BlockDisplayHandle h = displayBuilder.spawnBlock(scar, Material.AMETHYST_BLOCK);
                h.scale(1.5f, 0.15f, 1.5f).glow(0, 200, 255).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 40 ticks — reverse portal pulse
            if (ticksAlive < 40) {
                if (ticksAlive % 10 == 0) {
                    World w = center.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.REVERSE_PORTAL, center, 30, 10, 2, 10, 0.01);
                    }
                }
                return;
            }

            // Pull phase: 100 ticks (5 seconds) — ticks 40-140
            if (ticksAlive >= 40 && ticksAlive < 140 && !released) {
                // Inward particle streams toward each scar
                if (ticksAlive % 4 == 0) {
                    for (Location scar : scarLocations) {
                        // Particles stream inward from 6 blocks out
                        for (int i = 0; i < 4; i++) {
                            double a = Math.random() * 2 * Math.PI;
                            double d = 4.0 + Math.random() * 4.0;
                            Location origin = scar.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d);
                            DisplayBuilder.dustParticles(origin, 4, 0.3, 240, 240, 255, 1.0f);
                        }
                        DisplayBuilder.cyanDust(scar, 6, 0.8);
                    }
                }

                // Pull players toward nearest scar — 0.5 blocks/second = 0.025 blocks/tick
                if (ticksAlive % 4 == 0) {
                    World w = center.getWorld();
                    if (w == null) return;
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location pLoc = p.getLocation();
                        Location nearest = null;
                        double nearestDist = Double.MAX_VALUE;
                        for (Location scar : scarLocations) {
                            double d2 = pLoc.distanceSquared(scar);
                            if (d2 < nearestDist) {
                                nearestDist = d2;
                                nearest = scar;
                            }
                        }
                        if (nearest != null && nearestDist > 1.0) {
                            double dx = nearest.getX() - pLoc.getX();
                            double dz = nearest.getZ() - pLoc.getZ();
                            double dist = Math.sqrt(dx * dx + dz * dz);
                            double pullStrength = 0.1; // ~0.5 blocks/sec at every 4 ticks
                            p.setVelocity(p.getVelocity().add(
                                    new org.bukkit.util.Vector(
                                            dx / dist * pullStrength, 0,
                                            dz / dist * pullStrength)));
                        }
                    }
                }

                // Ambient pull sound
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 1.2f);
                }
            }

            // Release burst at tick 140
            if (ticksAlive >= 140 && !released) {
                released = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
                World w = center.getWorld();
                if (w == null) return;

                for (Location scar : scarLocations) {
                    // Visual burst
                    DisplayBuilder.dustParticles(scar, 40, 4.0, 240, 240, 255, 2.0f);
                    DisplayBuilder.cyanDust(scar, 25, 3.5);
                    if (w != null) {
                        w.spawnParticle(Particle.ELECTRIC_SPARK, scar.clone().add(0, 0.5, 0),
                                30, 3, 1, 3, 0.05);
                    }
                    // Damage in 6 block radius per scar
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(scar) <= 36.0) {
                            p.damage(config.getDamage());
                        }
                    }
                }
            }

            // Post-release dissipation
            if (ticksAlive > 140 && ticksAlive % 10 == 0) {
                for (Location scar : scarLocations) {
                    DisplayBuilder.cyanDust(scar, 4, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarSingularity(plugin); }
    }
}
