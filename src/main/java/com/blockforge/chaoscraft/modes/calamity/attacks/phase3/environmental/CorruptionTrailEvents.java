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
 * Phase 3 Environmental — GROUP 4: CORRUPTION TRAIL EVENTS
 * 10 attacks (31-40) themed around Dweller's movement trails, footprints, corruption spreading.
 * Calamity Dweller (Boss 3) — boss3-dweller.md
 *
 * Design notes:
 * - No status effects
 * - Dweller palette: crimson glow (200,0,50), orange glow (255,100,0), soul blue (0,150,255)
 * - Materials: NETHERRACK, CRIMSON_NYLIUM, NETHER_WART_BLOCK, MAGMA_BLOCK
 * - Damage in HP (4.0 - 12.0 range)
 */
public final class CorruptionTrailEvents {

    private CorruptionTrailEvents() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FootstepBrand(plugin));
        registry.register(new CorruptionCreep(plugin));
        registry.register(new TrailIgnition(plugin));
        registry.register(new LivingTrail(plugin));
        registry.register(new BurningPath(plugin));
        registry.register(new EchoTrails(plugin));
        registry.register(new CorruptionBloom(plugin));
        registry.register(new ContaminationPulse(plugin));
        registry.register(new LashTrail(plugin));
        registry.register(new TrailMaze(plugin));
    }

    // =========================================================================
    // ATTACK 31 — Footstep Brand
    // Dweller walks 6 steps, each leaves 1x1 netherrack brand that detonates after 5s.
    // 6 hearts per detonation. 2-block radius.
    // =========================================================================
    public static class FootstepBrand extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> brandLocations = new ArrayList<>();
        private final List<Integer> brandPlaceTicks = new ArrayList<>();
        private int stepsPlaced = 0;

        public FootstepBrand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("footstep_brand", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(240); // 6 steps + 5s detonation windows
            config.setCooldownTicks(480); // 24s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Place footsteps every 20 ticks (~1s per step)
            if (stepsPlaced < 6 && ticksAlive % 20 == 0 && ticksAlive >= 10) {
                double stepAngle = stepsPlaced * 0.8 + Math.random() * 0.4;
                double stepDist = 2.0 + stepsPlaced * 1.8;
                Location stepLoc = center.clone().add(Math.cos(stepAngle) * stepDist, 0, Math.sin(stepAngle) * stepDist);

                BlockDisplayHandle h = displayBuilder.spawnBlock(stepLoc, Material.NETHERRACK);
                h.scale(1.0f, 0.08f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());

                brandLocations.add(stepLoc.clone());
                brandPlaceTicks.add(ticksAlive);
                stepsPlaced++;

                // Footstep flash
                DisplayBuilder.dustParticles(stepLoc.clone().add(0, 0.1, 0), 4, 0.2, 230, 95, 18, 0.6f);
                DisplayBuilder.playSound(stepLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.7f);
            }

            // Update brand glow intensity (growing brighter toward detonation)
            for (int i = 0; i < brandLocations.size(); i++) {
                int placeTick = brandPlaceTicks.get(i);
                int ticksSincePlaced = ticksAlive - placeTick;

                // Glow intensifies over 5s = 100 ticks
                if (ticksSincePlaced < 100 && ticksSincePlaced % 15 == 0) {
                    float intensity = ticksSincePlaced / 100.0f;
                    Location loc = brandLocations.get(i);
                    DisplayBuilder.dustParticles(loc.clone().add(0, 0.15, 0),
                            (int)(3 + intensity * 5), 0.3, 230, 95, 18, 0.5f + intensity * 0.8f);
                }

                // Detonate at 100 ticks after placement
                if (ticksSincePlaced == 100) {
                    Location loc = brandLocations.get(i);
                    // Geyser explosion
                    DisplayBuilder.dustParticles(loc.clone().add(0, 2, 0), 20, 1.0, 240, 120, 40, 1.5f);
                    DisplayBuilder.dustParticles(loc.clone().add(0, 2, 0), 10, 0.8, 230, 95, 18, 1.0f);
                    DisplayBuilder.playSound(loc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.7f);

                    // Spawn geyser column display
                    for (int y = 0; y < 4; y++) {
                        BlockDisplayHandle gh = displayBuilder.spawnBlock(loc.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                        gh.scale(0.6f, 1.0f, 0.6f).glow(255, 100, 0).interpolation(2, 0);
                        handles.add(gh);
                        spawnedEntities.add(gh.entity());
                    }

                    // Damage in 2-block radius
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(loc) <= 4.0) {
                            p.damage(12.0); // 6 hearts
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FootstepBrand(plugin); }
    }

    // =========================================================================
    // ATTACK 32 — Corruption Creep
    // Line of corruption advances from Dweller to target at 2 blocks/s.
    // 5 hearts on corruption front, 2 hearts from glowing netherrack for 8s.
    // =========================================================================
    public static class CorruptionCreep extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double creepProgress = 0;
        private double creepAngle;
        private int creepLength = 0;

        public CorruptionCreep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_creep", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(4.0); // 2 hearts from walking on corrupted floor
            config.setDamageRadius(1.5);
            config.setDurationTicks(280); // 1s warn + 6s advance + 8s glow
            config.setCooldownTicks(360); // 18s
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Target nearest player
            Player nearest = null;
            double closestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dist = p.getLocation().distanceSquared(center);
                if (dist < closestDist) {
                    closestDist = dist;
                    nearest = p;
                }
            }
            if (nearest != null) {
                creepAngle = Math.atan2(
                        nearest.getLocation().getZ() - center.getZ(),
                        nearest.getLocation().getX() - center.getX());
                creepLength = (int) Math.sqrt(closestDist);
            } else {
                creepAngle = Math.random() * 2 * Math.PI;
                creepLength = 10;
            }
            // Warning particles at path origin
            DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 5, 0.3, 220, 88, 14, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 20 ticks (1s)
            if (ticksAlive < 20) {
                return;
            }

            // Advance at 2 blocks/second = 0.1 blocks/tick
            creepProgress += 0.1;
            if (creepProgress > creepLength) creepProgress = creepLength;

            // Spawn corruption blocks at the front every other tick
            if (ticksAlive % 2 == 0 && creepProgress <= creepLength) {
                Location frontLoc = center.clone().add(
                        Math.cos(creepAngle) * creepProgress, 0.01,
                        Math.sin(creepAngle) * creepProgress);
                BlockDisplayHandle h = displayBuilder.spawnBlock(frontLoc, Material.NETHERRACK);
                h.scale(1.0f, 0.06f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());

                DisplayBuilder.dustParticles(frontLoc.clone().add(0, 0.2, 0), 3, 0.3, 220, 88, 14, 0.6f);

                // Front damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(frontLoc) <= 2.25) {
                        p.damage(10.0); // 5 hearts
                    }
                }
            }

            // Smoke from corrupted blocks
            if (ticksAlive % 12 == 0) {
                int randBlock = (int)(Math.random() * creepProgress);
                Location smokeLoc = center.clone().add(
                        Math.cos(creepAngle) * randBlock, 0.3,
                        Math.sin(creepAngle) * randBlock);
                DisplayBuilder.dustParticles(smokeLoc, 2, 0.3, 85, 72, 60, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionCreep(plugin); }
    }

    // =========================================================================
    // ATTACK 33 — Trail Ignition
    // Dweller's trail from last 8 seconds ignites simultaneously.
    // 4 hearts per contact. 1 heart/s sustained. 6 seconds active.
    // =========================================================================
    public static class TrailIgnition extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> trailLocations = new ArrayList<>();
        private boolean ignited = false;

        public TrailIgnition(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trail_ignition", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 1s warn + 6s active
            config.setCooldownTicks(400); // 20s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Simulate Dweller's past 8 seconds of movement (random walk pattern)
            Location walkPos = center.clone();
            for (int step = 0; step < 16; step++) {
                double stepAngle = Math.random() * 2 * Math.PI;
                walkPos = walkPos.clone().add(Math.cos(stepAngle) * 1.5, 0, Math.sin(stepAngle) * 1.5);
                trailLocations.add(walkPos.clone());
            }
            // Warning flash on trail blocks
            for (Location loc : trailLocations) {
                DisplayBuilder.dustParticles(loc.clone().add(0, 0.1, 0), 2, 0.2, 220, 80, 10, 0.4f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 20 ticks (1s)
            if (ticksAlive < 20) {
                return;
            }

            // Ignite all trail blocks simultaneously
            if (!ignited) {
                ignited = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 1.0f);
                for (Location loc : trailLocations) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    h.scale(0.8f, 0.15f, 0.8f).glow(255, 100, 0).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());

                    // 2-block tall flame particles
                    DisplayBuilder.dustParticles(loc.clone().add(0, 1, 0), 5, 0.4, 240, 120, 40, 0.8f);
                }
            }

            // Contact damage on trail
            if (ticksAlive % 8 == 0) {
                for (Location loc : trailLocations) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(loc) <= 1.5) {
                            p.damage(8.0); // 4 hearts initial, then ~1 heart/s sustained
                        }
                    }
                }
            }

            // Smoke from ignited trail
            if (ticksAlive % 10 == 0) {
                int idx = (int)(Math.random() * trailLocations.size());
                DisplayBuilder.dustParticles(trailLocations.get(idx).clone().add(0, 0.5, 0),
                        3, 0.3, 80, 70, 60, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TrailIgnition(plugin); }
    }

    // =========================================================================
    // ATTACK 34 — Living Trail
    // Footsteps leave crimson nylium patches that grow nether wart stalks.
    // Patches explode after 10s for 4 hearts. Movement impediment while active.
    // =========================================================================
    public static class LivingTrail extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> patchLocations = new ArrayList<>();
        private final List<Integer> patchPlaceTicks = new ArrayList<>();
        private int stepsPlaced = 0;

        public LivingTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("living_trail", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(340); // 8 steps over ~8s + 10s explosion timers
            config.setCooldownTicks(440); // 22s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Place patches every 20 ticks (~1s per step) for 8 steps
            if (stepsPlaced < 8 && ticksAlive % 20 == 0 && ticksAlive >= 10) {
                double angle = stepsPlaced * 0.7 + Math.random() * 0.3;
                double dist = 2.0 + stepsPlaced * 1.5;
                Location patchLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                // Spawn 2x2 crimson nylium patch
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(patchLoc.clone().add(dx, 0.01, dz), Material.CRIMSON_NYLIUM);
                        h.scale(1.0f, 0.06f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // Nether wart stalk display on each patch
                BlockDisplayHandle stalk = displayBuilder.spawnBlock(patchLoc.clone().add(0.5, 0.1, 0.5), Material.NETHER_WART_BLOCK);
                stalk.scale(0.5f, 1.5f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                handles.add(stalk);
                spawnedEntities.add(stalk.entity());

                patchLocations.add(patchLoc.clone());
                patchPlaceTicks.add(ticksAlive);
                stepsPlaced++;

                DisplayBuilder.dustParticles(patchLoc.clone().add(0.5, 0.3, 0.5), 6, 0.5, 180, 30, 30, 0.8f);
                DisplayBuilder.playSound(patchLoc, Sound.BLOCK_STONE_PLACE, 0.6f, 0.8f);
            }

            // Slow players in patches (velocity reduction)
            if (ticksAlive % 6 == 0) {
                for (Location patch : patchLocations) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - patch.getX();
                        double dz = p.getLocation().getZ() - patch.getZ();
                        if (dx >= -0.5 && dx <= 2.0 && dz >= -0.5 && dz <= 2.0) {
                            org.bukkit.util.Vector vel = p.getVelocity();
                            p.setVelocity(vel.setX(vel.getX() * 0.7).setZ(vel.getZ() * 0.7));
                        }
                    }
                }
            }

            // Explode patches 200 ticks (10s) after placement
            for (int i = 0; i < patchLocations.size(); i++) {
                int placeTick = patchPlaceTicks.get(i);
                if (ticksAlive - placeTick == 200) {
                    Location patch = patchLocations.get(i);
                    DisplayBuilder.dustParticles(patch.clone().add(0.5, 1.5, 0.5), 15, 1.5, 180, 30, 30, 1.2f);
                    DisplayBuilder.playSound(patch, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);

                    // Damage in 3-block burst
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(patch) <= 9.0) {
                            p.damage(8.0); // 4 hearts
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LivingTrail(plugin); }
    }

    // =========================================================================
    // ATTACK 35 — Burning Path
    // Dweller walks path twice: first slow (harmless warning), second fast (ignites).
    // 7 hearts on ignition contact.
    // =========================================================================
    public static class BurningPath extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> pathLocations = new ArrayList<>();
        private boolean firstPassDone = false;
        private boolean secondPassDone = false;
        private int firstPassProgress = 0;
        private double pathAngle;

        public BurningPath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("burning_path", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 6s first pass + 3s second pass
            config.setCooldownTicks(560); // 28s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            pathAngle = Math.random() * 2 * Math.PI;
            // Pre-generate path (serpentine walk)
            Location walkPos = center.clone();
            for (int step = 0; step < 12; step++) {
                double sway = Math.sin(step * 0.5) * 2.0;
                double perpAngle = pathAngle + Math.PI / 2;
                walkPos = center.clone().add(
                        Math.cos(pathAngle) * (step * 1.5 - 9) + Math.cos(perpAngle) * sway,
                        0,
                        Math.sin(pathAngle) * (step * 1.5 - 9) + Math.sin(perpAngle) * sway);
                pathLocations.add(walkPos.clone());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.4f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // First pass (slow): 120 ticks (6s) — dripping lava trails, no damage
            if (!firstPassDone && ticksAlive < 120) {
                int idx = (int)((ticksAlive / 120.0) * pathLocations.size());
                if (idx < pathLocations.size() && ticksAlive % 10 == 0) {
                    Location loc = pathLocations.get(idx);
                    DisplayBuilder.dustParticles(loc.clone().add(0, 0.1, 0), 3, 0.2, 220, 80, 10, 0.5f);
                    // Dripping lava marker
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                    h.scale(0.8f, 0.04f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                    firstPassProgress = idx;
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.3f, 0.6f);
                }
                return;
            }

            if (!firstPassDone) {
                firstPassDone = true;
            }

            // Second pass (fast): 60 ticks (3s) — full ignition
            if (!secondPassDone) {
                int secondPassTick = ticksAlive - 120;
                if (secondPassTick == 1) {
                    // Instant ignition of ALL path blocks
                    secondPassDone = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 1.0f);

                    for (Location loc : pathLocations) {
                        BlockDisplayHandle fh = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                        fh.scale(1.0f, 0.2f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                        handles.add(fh);
                        spawnedEntities.add(fh.entity());
                        DisplayBuilder.dustParticles(loc.clone().add(0, 1, 0), 6, 0.5, 245, 125, 35, 1.0f);
                    }

                    // Damage all players on the path
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location loc : pathLocations) {
                            if (p.getLocation().distanceSquared(loc) <= 1.5) {
                                p.damage(12.0); // ~6 hearts (scaled from 7)
                                break;
                            }
                        }
                    }
                }
            }

            // Sustained flame particles on path after ignition
            if (secondPassDone && ticksAlive % 10 == 0) {
                int idx = (int)(Math.random() * pathLocations.size());
                DisplayBuilder.dustParticles(pathLocations.get(idx).clone().add(0, 0.5, 0),
                        3, 0.3, 245, 125, 35, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BurningPath(plugin); }
    }

    // =========================================================================
    // ATTACK 36 — Echo Trails
    // Ghost echo of the Dweller replays its path from 5s ago. Trail deals damage.
    // 4 hearts on echo trail contact. 5-second replay.
    // =========================================================================
    public static class EchoTrails extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> echoPath = new ArrayList<>();
        private int echoStep = 0;

        public EchoTrails(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_trails", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5s replay
            config.setCooldownTicks(400); // 20s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Simulate Dweller's path from 5s ago
            Location walkPos = center.clone();
            for (int step = 0; step < 10; step++) {
                double angle = Math.random() * 2 * Math.PI;
                walkPos = walkPos.clone().add(Math.cos(angle) * 2.0, 0, Math.sin(angle) * 2.0);
                echoPath.add(walkPos.clone());
            }
            // No warning per design — instant appearance
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Walk echo path: 1 step every 10 ticks (0.5s per step)
            if (echoStep < echoPath.size() && ticksAlive % 10 == 0) {
                Location echoLoc = echoPath.get(echoStep);

                // Echo ghost display — translucent soul fire block
                BlockDisplayHandle h = displayBuilder.spawnBlock(echoLoc, Material.SOUL_SAND);
                h.scale(0.8f, 1.8f, 0.8f).glow(0, 150, 255).interpolation(3, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());

                // Trail mark on floor
                BlockDisplayHandle trail = displayBuilder.spawnBlock(echoLoc, Material.SOUL_SOIL);
                trail.scale(0.6f, 0.04f, 0.6f).glow(0, 150, 255).interpolation(2, 0);
                handles.add(trail);
                spawnedEntities.add(trail.entity());

                DisplayBuilder.dustParticles(echoLoc.clone().add(0, 1, 0), 6, 0.5, 0, 150, 255, 0.8f);
                DisplayBuilder.playSound(echoLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.2f, 0.8f);

                // Trail damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(echoLoc) <= 2.25) {
                        p.damage(8.0); // 4 hearts
                    }
                }

                echoStep++;
            }

            // Damage on existing trail marks
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < echoStep && i < echoPath.size(); i++) {
                    Location trailLoc = echoPath.get(i);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(trailLoc) <= 1.0) {
                            p.damage(4.0); // 2 hearts
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EchoTrails(plugin); }
    }

    // =========================================================================
    // ATTACK 37 — Corruption Bloom
    // Longest corruption scar erupts in flame columns. Scales with prior corruption.
    // 5 hearts initial contact, 2 hearts/s sustained. 6 seconds.
    // =========================================================================
    public static class CorruptionBloom extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> scarLocations = new ArrayList<>();
        private boolean bloomed = false;

        public CorruptionBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_bloom", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(170); // 1.5s warn + 6s active
            config.setCooldownTicks(440); // 22s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Simulate longest corruption scar (8-16 blocks)
            int scarLength = 8 + (int)(Math.random() * 8);
            double scarAngle = Math.random() * 2 * Math.PI;
            for (int i = 0; i < scarLength; i++) {
                scarLocations.add(center.clone().add(
                        Math.cos(scarAngle) * (i - scarLength / 2.0), 0,
                        Math.sin(scarAngle) * (i - scarLength / 2.0)));
            }
            // Warning pulse
            for (Location loc : scarLocations) {
                DisplayBuilder.dustParticles(loc.clone().add(0, 0.1, 0), 2, 0.2, 230, 95, 18, 0.5f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5s)
            if (ticksAlive < 30) {
                return;
            }

            // Bloom eruption
            if (!bloomed) {
                bloomed = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 0.8f);
                for (Location loc : scarLocations) {
                    // 5-block tall flame column
                    for (int y = 0; y < 5; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                        float scale = 0.8f - y * 0.1f;
                        h.scale(scale, 1.0f, scale).glow(255, 100, 0).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    DisplayBuilder.dustParticles(loc.clone().add(0, 2.5, 0), 8, 0.6, 240, 120, 40, 1.0f);

                    // Initial contact damage
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(loc) <= 2.25) {
                            p.damage(10.0); // 5 hearts
                        }
                    }
                }
            }

            // Sustained damage in bloom zone
            if (bloomed && ticksAlive % 10 == 0) {
                for (Location loc : scarLocations) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location ploc = p.getLocation();
                        double dx = ploc.getX() - loc.getX();
                        double dz = ploc.getZ() - loc.getZ();
                        double dy = ploc.getY() - loc.getY();
                        if (dx * dx + dz * dz <= 1.5 && dy >= 0 && dy <= 5) {
                            p.damage(4.0); // 2 hearts/s
                        }
                    }
                }
            }

            // Dripping lava from column tops
            if (ticksAlive % 12 == 0) {
                int idx = (int)(Math.random() * scarLocations.size());
                DisplayBuilder.dustParticles(scarLocations.get(idx).clone().add(0, 5, 0),
                        3, 0.3, 220, 80, 10, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionBloom(plugin); }
    }

    // =========================================================================
    // ATTACK 38 — Contamination Pulse
    // Dweller slams floor, radial pulse wave expands at 3 blocks/s.
    // 6 hearts from pulse wave. Jumpable.
    // =========================================================================
    public static class ContaminationPulse extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean pulsed = false;
        private double pulseRadius = 0;

        public ContaminationPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("contamination_pulse", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140); // 2s crouch + 5s pulse travel
            config.setCooldownTicks(600); // 30s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller crouch indicator
            DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 6, 0.3, 225, 90, 15, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Crouch: 40 ticks (2s)
            if (ticksAlive < 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 4, 0.4, 225, 90, 15, 0.7f);
                }
                return;
            }

            // Slam — fire pulse
            if (!pulsed) {
                pulsed = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.2f, 0.5f);
                DisplayBuilder.dustParticles(center, 25, 1.5, 180, 30, 30, 1.5f);
            }

            // Pulse expands at 3 blocks/second = 0.15 blocks/tick
            pulseRadius += 0.15;

            // Pulse ring visual
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.particleRing(center, pulseRadius, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(200, 0, 50), 1.2f));
                // Secondary crimson spore ring
                DisplayBuilder.particleRing(center, pulseRadius - 0.5, Particle.DUST, 8,
                        new Particle.DustOptions(Color.fromRGB(180, 30, 30), 0.8f));
            }

            // Corruption floor left behind pulse
            if (ticksAlive % 6 == 0 && pulseRadius > 2) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = Math.random() * pulseRadius;
                Location corruptLoc = center.clone().add(Math.cos(angle) * dist, 0.01, Math.sin(angle) * dist);
                BlockDisplayHandle h = displayBuilder.spawnBlock(corruptLoc, Material.NETHERRACK);
                h.scale(1.0f, 0.04f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Damage players at pulse wave front
            if (ticksAlive % 3 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(p.getLocation().distanceSquared(center));
                    if (Math.abs(dist - pulseRadius) <= 1.5) {
                        // Jumpable: check if player is above ground level
                        double playerY = p.getLocation().getY();
                        double centerY = center.getY();
                        if (playerY - centerY < 0.6) {
                            p.damage(12.0); // 6 hearts
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ContaminationPulse(plugin); }
    }

    // =========================================================================
    // ATTACK 39 — Lash Trail
    // Fast horizontal flame arc, 10 blocks long. Leaves 5s floor scar.
    // 6 hearts from lash. 1.5 hearts/s from floor scar.
    // =========================================================================
    public static class LashTrail extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> arcLocations = new ArrayList<>();
        private boolean lashed = false;
        private double lashAngle;

        public LashTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lash_trail", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(130); // 0.8s windup + 0.4s lash + 5s scar
            config.setCooldownTicks(280); // 14s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Face nearest player
            Player nearest = null;
            double closestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dist = p.getLocation().distanceSquared(center);
                if (dist < closestDist) {
                    closestDist = dist;
                    nearest = p;
                }
            }
            if (nearest != null) {
                lashAngle = Math.atan2(
                        nearest.getLocation().getZ() - center.getZ(),
                        nearest.getLocation().getX() - center.getX());
            } else {
                lashAngle = Math.random() * 2 * Math.PI;
            }

            // Pre-calculate arc positions (curved sweep)
            for (int i = 0; i < 10; i++) {
                double sweepAngle = lashAngle + (i - 5) * 0.12; // ~60-degree arc
                double dist = 2.0 + i * 0.8;
                arcLocations.add(center.clone().add(Math.cos(sweepAngle) * dist, 0, Math.sin(sweepAngle) * dist));
            }

            // Windup indicator
            DisplayBuilder.dustParticles(center.clone().add(Math.cos(lashAngle) * 1.5, 1.5, Math.sin(lashAngle) * 1.5),
                    4, 0.2, 250, 130, 30, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Windup: 16 ticks (0.8s)
            if (ticksAlive < 16) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(
                            center.clone().add(Math.cos(lashAngle) * 1.5, 1.5, Math.sin(lashAngle) * 1.5),
                            3, 0.2, 250, 130, 30, 0.5f);
                }
                return;
            }

            // Lash: 8 ticks (0.4s) — instant arc
            if (!lashed) {
                lashed = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.5f);

                // Arc visual and damage
                for (Location arcLoc : arcLocations) {
                    DisplayBuilder.dustParticles(arcLoc.clone().add(0, 1, 0), 5, 0.3, 250, 130, 30, 1.0f);
                    DisplayBuilder.dustParticles(arcLoc.clone().add(0, 1, 0), 3, 0.2, 230, 95, 15, 0.8f);
                }

                // Lash damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (Location arcLoc : arcLocations) {
                        double dx = p.getLocation().getX() - arcLoc.getX();
                        double dy = p.getLocation().getY() - arcLoc.getY();
                        double dz = p.getLocation().getZ() - arcLoc.getZ();
                        if (dx * dx + dz * dz <= 1.5 && dy >= 0 && dy <= 2.0) {
                            p.damage(12.0); // 6 hearts
                            break;
                        }
                    }
                }

                // Spawn floor scar
                for (Location arcLoc : arcLocations) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(arcLoc, Material.MAGMA_BLOCK);
                    h.scale(0.8f, 0.08f, 0.8f).glow(255, 100, 0).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Floor scar sustained damage
            if (lashed && ticksAlive % 10 == 0) {
                for (Location arcLoc : arcLocations) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(arcLoc) <= 1.0) {
                            p.damage(4.0); // ~1.5 hearts/s
                        }
                    }
                }
            }

            // Scar particle glow
            if (lashed && ticksAlive % 12 == 0) {
                int idx = (int)(Math.random() * arcLocations.size());
                DisplayBuilder.dustParticles(arcLocations.get(idx).clone().add(0, 0.15, 0),
                        2, 0.2, 250, 130, 30, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LashTrail(plugin); }
    }

    // =========================================================================
    // ATTACK 40 — Trail Maze
    // Dweller walks figure-eight, leaving flame trails for 20s.
    // 4 hearts per trail contact. 12s walk, 20s persistence.
    // =========================================================================
    public static class TrailMaze extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> mazeTrail = new ArrayList<>();
        private int walkStep = 0;
        private static final int TOTAL_STEPS = 24;

        public TrailMaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trail_maze", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(640); // 12s walk + 20s trail persistence
            config.setCooldownTicks(760); // 38s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Walk figure-eight pattern: 1 step every 10 ticks (0.5s)
            if (walkStep < TOTAL_STEPS && ticksAlive % 10 == 0) {
                // Figure-eight parametric: x = sin(t), z = sin(2t)/2
                double t = (walkStep / (double) TOTAL_STEPS) * 2 * Math.PI;
                double x = Math.sin(t) * 8.0;
                double z = Math.sin(2 * t) * 4.0;
                Location stepLoc = center.clone().add(x, 0, z);

                // Spawn trail block
                BlockDisplayHandle h = displayBuilder.spawnBlock(stepLoc, Material.MAGMA_BLOCK);
                h.scale(1.0f, 0.12f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
                mazeTrail.add(stepLoc.clone());

                // Trail flame particles
                DisplayBuilder.dustParticles(stepLoc.clone().add(0, 0.3, 0), 4, 0.3, 240, 120, 40, 0.7f);
                DisplayBuilder.playSound(stepLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.3f, 0.8f);

                walkStep++;
            }

            // Trail damage
            if (ticksAlive % 8 == 0) {
                for (Location trailLoc : mazeTrail) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(trailLoc) <= 1.0) {
                            p.damage(8.0); // 4 hearts
                        }
                    }
                }
            }

            // Ambient smoke from trail segments
            if (ticksAlive % 15 == 0 && !mazeTrail.isEmpty()) {
                int idx = (int)(Math.random() * mazeTrail.size());
                DisplayBuilder.dustParticles(mazeTrail.get(idx).clone().add(0, 0.4, 0),
                        2, 0.3, 80, 70, 60, 0.5f);
            }

            // Full trail ambient fire sound
            if (ticksAlive % 30 == 0 && walkStep >= TOTAL_STEPS) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TrailMaze(plugin); }
    }
}
