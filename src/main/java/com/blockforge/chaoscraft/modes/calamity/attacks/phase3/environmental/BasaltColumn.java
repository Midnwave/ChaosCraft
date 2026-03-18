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
 * Phase 3 Environmental — GROUP 2: BASALT COLUMN EVENTS
 * 10 attacks (11-20) themed around basalt columns erupting, toppling, shattering.
 * Calamity Dweller (Boss 3) — boss3-dweller.md
 *
 * Design notes:
 * - No status effects
 * - Dweller palette: crimson glow (200,0,50), orange glow (255,100,0), soul blue (0,150,255)
 * - Materials: BASALT, SMOOTH_BASALT, MAGMA_BLOCK, NETHERRACK, BLACKSTONE
 * - Damage in HP (4.0 - 12.0 range)
 */
public final class BasaltColumn {

    private BasaltColumn() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ColumnSpikeEruption(plugin));
        registry.register(new ColumnTopple(plugin));
        registry.register(new FragmentBarrage(plugin));
        registry.register(new ColumnRingTrap(plugin));
        registry.register(new PillarOfAsh(plugin));
        registry.register(new ColumnWalk(plugin));
        registry.register(new BasaltMortar(plugin));
        registry.register(new BasaltCurtain(plugin));
        registry.register(new FaultColumn(plugin));
        registry.register(new BasaltShrapnelStorm(plugin));
    }

    // =========================================================================
    // ATTACK 11 — Column Spike Eruption
    // Basalt pillar erupts from floor to 10 blocks tall. Players on tile launched.
    // 5 hearts fall damage if launched, 3 hearts knockback to adjacent.
    // =========================================================================
    public static class ColumnSpikeEruption extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int columnHeight = 0;
        private boolean fullyErupted = false;

        public ColumnSpikeEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("column_spike_eruption", AttackType.ENVIRONMENTAL, 3));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts
            config.setImpactRadius(1.5);
            config.setDurationTicks(700); // 1.5s warn + 30s persist + 5s crumble
            config.setCooldownTicks(500); // 25s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning: cross crack pattern at base
            for (int i = -1; i <= 1; i++) {
                DisplayBuilder.dustParticles(center.clone().add(i, 0.1, 0), 3, 0.2, 220, 80, 10, 0.6f);
                DisplayBuilder.dustParticles(center.clone().add(0, 0.1, i), 3, 0.2, 220, 80, 10, 0.6f);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning phase: 30 ticks (1.5s)
            if (ticksAlive < 30) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 4, 0.5, 220, 80, 10, 0.5f);
                }
                return;
            }

            // Eruption: grow column 1 block per tick for 10 ticks
            if (!fullyErupted && columnHeight < 10) {
                columnHeight++;
                Location colLoc = center.clone().add(0, columnHeight - 1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(colLoc, Material.BASALT);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(1, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());

                // Accelerating sound
                DisplayBuilder.playSound(colLoc, Sound.BLOCK_STONE_PLACE, 0.5f + columnHeight * 0.08f, 0.6f + columnHeight * 0.05f);

                // Base smoke and ash
                if (columnHeight <= 3) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 8, 1.5, 80, 70, 65, 1.0f);
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 4, 1.0, 190, 175, 155, 0.8f);
                }

                // Launch players at eruption start
                if (columnHeight == 2) {
                    triggerImpactDamage(center);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 2.25) {
                            p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 1.8, 0)));
                        }
                    }
                }

                // Knockback adjacent players
                if (columnHeight == 3) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double distSq = p.getLocation().distanceSquared(center);
                        if (distSq > 2.25 && distSq <= 9.0) {
                            double dx = p.getLocation().getX() - center.getX();
                            double dz = p.getLocation().getZ() - center.getZ();
                            double dist = Math.sqrt(distSq);
                            p.setVelocity(p.getVelocity().add(
                                    new org.bukkit.util.Vector(dx / dist * 0.8, 0.3, dz / dist * 0.8)));
                            p.damage(6.0); // 3 hearts
                        }
                    }
                }

                if (columnHeight == 10) {
                    fullyErupted = true;
                    // Chimney flame at tip
                    DisplayBuilder.dustParticles(center.clone().add(0, 10.5, 0), 12, 0.5, 230, 110, 30, 1.2f);
                }
                return;
            }

            // Column persists — chimney flame particles at tip
            if (fullyErupted && ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 10.5, 0), 4, 0.3, 230, 110, 30, 0.8f);
            }

            // Crumble phase: last 100 ticks (5s)
            if (ticksAlive >= 600) {
                int crumbleTick = ticksAlive - 600;
                int blocksToRemove = crumbleTick / 10;
                if (blocksToRemove < handles.size()) {
                    // Remove from top down
                    int removeIdx = handles.size() - 1 - blocksToRemove;
                    if (removeIdx >= 0 && removeIdx < handles.size()) {
                        BlockDisplayHandle rem = handles.get(removeIdx);
                        DisplayBuilder.dustParticles(rem.entity().getLocation(), 6, 0.5, 190, 175, 155, 0.8f);
                        rem.entity().remove();
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ColumnSpikeEruption(plugin); }
    }

    // =========================================================================
    // ATTACK 12 — Column Topple
    // Existing column leans 3s, then falls in a direction. 12-block fall zone.
    // 8 hearts in fall zone, 6 hearts from debris cloud.
    // =========================================================================
    public static class ColumnTopple extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> columnHandles = new ArrayList<>();
        private boolean fallen = false;
        private double fallAngle;
        private int columnBlocks = 8;

        public ColumnTopple(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("column_topple", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(150); // 3s lean + 1.5s fall + 3s debris
            config.setCooldownTicks(700); // 35s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            fallAngle = Math.random() * 2 * Math.PI;
            // Spawn column to topple
            for (int y = 0; y < columnBlocks; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.BASALT);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(4, 0);
                columnHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.4f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Lean phase: 60 ticks (3s) — tilt column
            if (ticksAlive <= 60) {
                float tiltAngle = ticksAlive * 0.004f; // Slow lean
                // Ash particles from lean side
                if (ticksAlive % 10 == 0) {
                    Location ashLoc = center.clone().add(Math.cos(fallAngle) * 0.5, columnBlocks * 0.5, Math.sin(fallAngle) * 0.5);
                    DisplayBuilder.dustParticles(ashLoc, 5, 0.5, 190, 175, 155, 0.7f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.3f + ticksAlive * 0.01f, 0.5f);
                }

                // Animate lean via rotation on each block
                for (int i = 0; i < columnHandles.size(); i++) {
                    BlockDisplay bd = columnHandles.get(i).entity();
                    float blockTilt = tiltAngle * (i + 1) / columnBlocks;
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(blockTilt, (float)(-Math.sin(fallAngle)), 0, (float)(Math.cos(fallAngle))),
                            new Vector3f(1.0f, 1.0f, 1.0f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
                return;
            }

            // Fall: 30 ticks (1.5s) — rapid topple
            if (!fallen && ticksAlive <= 90) {
                int fallTick = ticksAlive - 60;
                float fallProgress = fallTick / 30.0f;
                float totalTilt = (float)(Math.PI / 2.0) * fallProgress;

                for (int i = 0; i < columnHandles.size(); i++) {
                    BlockDisplay bd = columnHandles.get(i).entity();
                    float blockTilt = 0.24f + totalTilt * (i + 1) / columnBlocks;
                    // Move blocks along fall direction as they topple
                    float fallOffset = (float)(Math.sin(blockTilt) * (i + 1));
                    float yOffset = (float)(Math.cos(blockTilt) * (i + 1)) - (i + 1);
                    Location newLoc = center.clone().add(
                            Math.cos(fallAngle) * fallOffset, yOffset, Math.sin(fallAngle) * fallOffset);
                    bd.teleport(newLoc);
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.5f, -0.5f, -0.5f),
                            new AxisAngle4f(blockTilt, (float)(-Math.sin(fallAngle)), 0, (float)(Math.cos(fallAngle))),
                            new Vector3f(1.0f, 1.0f, 1.0f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }

                // Impact at end of fall
                if (fallTick >= 28 && !fallen) {
                    fallen = true;
                    Location impactLoc = center.clone().add(
                            Math.cos(fallAngle) * columnBlocks, 0, Math.sin(fallAngle) * columnBlocks);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_STONE_PLACE, 1.5f, 0.3f);

                    // Damage in fall zone (12-block line)
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location ploc = p.getLocation();
                        double dx = ploc.getX() - center.getX();
                        double dz = ploc.getZ() - center.getZ();
                        double parallelDist = dx * Math.cos(fallAngle) + dz * Math.sin(fallAngle);
                        double perpDist = Math.abs(dx * (-Math.sin(fallAngle)) + dz * Math.cos(fallAngle));
                        if (parallelDist >= 0 && parallelDist <= 12 && perpDist <= 1.2) {
                            p.damage(12.0); // 6 hearts (scaled from 8)
                        }
                    }

                    // Debris cloud
                    DisplayBuilder.dustParticles(impactLoc, 30, 2.5, 90, 80, 70, 1.5f);
                    DisplayBuilder.dustParticles(impactLoc, 15, 2.0, 240, 120, 40, 1.0f);
                }
                return;
            }

            // Debris cloud damage for 60 ticks (3s)
            if (fallen && ticksAlive <= 150 && ticksAlive % 10 == 0) {
                Location impactLoc = center.clone().add(
                        Math.cos(fallAngle) * columnBlocks, 0, Math.sin(fallAngle) * columnBlocks);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(impactLoc) <= 9.0) {
                        p.damage(6.0); // 3 hearts from debris (scaled from 6)
                    }
                }
                DisplayBuilder.dustParticles(impactLoc, 8, 2.0, 90, 80, 70, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ColumnTopple(plugin); }
    }

    // =========================================================================
    // ATTACK 13 — Fragment Barrage
    // Tallest column shakes, top 3 blocks shear off as 3 projectiles in fan.
    // 7 hearts at landing, 3 hearts knockback in 4-block radius.
    // =========================================================================
    public static class FragmentBarrage extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> landingZones = new ArrayList<>();
        private boolean sheared = false;
        private int fragmentsLanded = 0;

        public FragmentBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fragment_barrage", AttackType.ENVIRONMENTAL, 3));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0); // 6 hearts (scaled from 7)
            config.setImpactRadius(2.0);
            config.setDurationTicks(500); // 1.5s shake + impact + 20s debris
            config.setCooldownTicks(440); // 22s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spawn a tall column to shear from
            for (int y = 0; y < 8; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.BASALT);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Pre-calculate 3 landing zones in a fan pattern
            double baseAngle = Math.random() * 2 * Math.PI;
            for (int i = 0; i < 3; i++) {
                double angle = baseAngle + (i - 1) * (Math.PI / 3.0); // 60-degree spread
                double dist = 5.0 + Math.random() * 4.0;
                landingZones.add(center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Shake phase: 30 ticks (1.5s)
            if (ticksAlive < 30) {
                if (ticksAlive % 2 == 0) {
                    double shakeX = (Math.random() - 0.5) * 0.15;
                    double shakeZ = (Math.random() - 0.5) * 0.15;
                    for (int i = 5; i < handles.size(); i++) {
                        BlockDisplay bd = handles.get(i).entity();
                        Location loc = center.clone().add(shakeX, i, shakeZ);
                        bd.teleport(loc);
                    }
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 6, 0), 5, 0.8, 85, 75, 65, 0.8f);
                }
                return;
            }

            // Shear: launch fragments toward landing zones
            if (!sheared) {
                sheared = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.5f);
                // Remove top 3 blocks from column
                for (int i = handles.size() - 1; i >= handles.size() - 3 && i >= 0; i--) {
                    handles.get(i).entity().remove();
                }
            }

            // Fragments falling — staggered landing
            if (fragmentsLanded < 3) {
                int landTick = 30 + fragmentsLanded * 8; // Stagger by ~0.4s each
                if (ticksAlive >= landTick && ticksAlive == landTick) {
                    Location landing = landingZones.get(fragmentsLanded);

                    // Landing indicator 0.5s before was the shear
                    DisplayBuilder.dustParticles(landing.clone().add(0, 0.1, 0), 6, 0.5, 220, 80, 10, 0.6f);

                    // Spawn debris at landing
                    int debrisHeight = 1 + (int)(Math.random() * 2);
                    for (int y = 0; y < debrisHeight; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(landing.clone().add(0, y, 0), Material.BASALT);
                        h.scale(0.8f, 0.8f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }

                    // ASH particle trail at landing
                    DisplayBuilder.dustParticles(landing.clone().add(0, 1, 0), 15, 1.5, 185, 170, 150, 1.0f);
                    DisplayBuilder.playSound(landing, Sound.BLOCK_STONE_PLACE, 1.0f, 0.6f);

                    // Impact damage
                    triggerImpactDamage(landing);

                    // Knockback in 4-block radius
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double distSq = p.getLocation().distanceSquared(landing);
                        if (distSq > 4.0 && distSq <= 16.0) {
                            double dx = p.getLocation().getX() - landing.getX();
                            double dz = p.getLocation().getZ() - landing.getZ();
                            double dist = Math.sqrt(distSq);
                            p.setVelocity(p.getVelocity().add(
                                    new org.bukkit.util.Vector(dx / dist * 0.6, 0.2, dz / dist * 0.6)));
                            p.damage(6.0); // 3 hearts knockback
                        }
                    }

                    fragmentsLanded++;
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FragmentBarrage(plugin); }
    }

    // =========================================================================
    // ATTACK 14 — Column Ring Trap
    // 6 columns erupt around the aggro player, forming a cage. Collapse inward at 12s.
    // 9 hearts if caught in collapse zone.
    // =========================================================================
    public static class ColumnRingTrap extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> columnPositions = new ArrayList<>();
        private boolean columnsSpawned = false;
        private boolean collapsed = false;

        public ColumnRingTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("column_ring_trap", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(300); // 1s warn + 12s hold + 2s collapse
            config.setCooldownTicks(800); // 40s
            config.setTicksBetweenDamage(999);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-calculate 6 column positions in a ring, 3 blocks from center
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                columnPositions.add(center.clone().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3));
            }
            // Warning: soul fire ring at column positions
            DisplayBuilder.particleRing(center, 3.0, Particle.DUST, 12,
                    new Particle.DustOptions(Color.fromRGB(0, 150, 255), 1.0f));
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.5f);
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

            // Erupt all 6 columns
            if (!columnsSpawned && ticksAlive >= 20) {
                columnsSpawned = true;
                for (Location colPos : columnPositions) {
                    for (int y = 0; y < 6; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(colPos.clone().add(0, y, 0), Material.BASALT);
                        h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    DisplayBuilder.playSound(colPos, Sound.BLOCK_STONE_PLACE, 1.2f, 0.7f);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 1.0f);

                // Inner flame particles
                for (Location colPos : columnPositions) {
                    DisplayBuilder.dustParticles(colPos.clone().add(0, 3, 0), 6, 0.5, 255, 100, 0, 0.8f);
                }
            }

            // Cage active — flame particles inside ring
            if (columnsSpawned && !collapsed && ticksAlive % 15 == 0) {
                DisplayBuilder.particleRing(center, 2.5, Particle.DUST, 8,
                        new Particle.DustOptions(Color.fromRGB(255, 100, 0), 0.8f));
            }

            // Collapse at tick 260 (12s after spawn)
            if (!collapsed && ticksAlive >= 260) {
                collapsed = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);

                // All columns topple inward — damage at center
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 12.25) { // 3.5 radius
                        p.damage(12.0); // 6 hearts (scaled from 9)
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 0.5, 0)));
                    }
                }

                // Visual collapse — rotate columns inward
                for (int i = 0; i < columnPositions.size(); i++) {
                    Location colPos = columnPositions.get(i);
                    double angle = Math.atan2(center.getZ() - colPos.getZ(), center.getX() - colPos.getX());
                    DisplayBuilder.dustParticles(colPos, 15, 1.5, 190, 175, 155, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ColumnRingTrap(plugin); }
    }

    // =========================================================================
    // ATTACK 15 — Pillar of Ash
    // Column disintegrates top-down over 4s, ash cloud obscures 5-block radius.
    // Base detonation: 6 hearts in 4-block radius.
    // =========================================================================
    public static class PillarOfAsh extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int dissolveLevel = 0;
        private boolean detonated = false;

        public PillarOfAsh(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pillar_of_ash", AttackType.ENVIRONMENTAL, 3));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0); // 6 hearts
            config.setImpactRadius(4.0);
            config.setDurationTicks(100); // 4s dissolution + instant detonation
            config.setCooldownTicks(360); // 18s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spawn column to dissolve
            for (int y = 0; y < 7; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.BASALT);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Initial ash seam crack
            DisplayBuilder.dustParticles(center.clone().add(0, 5, 0), 6, 0.5, 195, 178, 158, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.3f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Dissolution: dissolve 1 level every ~11 ticks (4s for 7 levels)
            if (!detonated && ticksAlive % 11 == 0 && dissolveLevel < 7) {
                int removeIdx = handles.size() - 1 - dissolveLevel;
                if (removeIdx >= 0 && removeIdx < handles.size()) {
                    BlockDisplay bd = handles.get(removeIdx).entity();
                    Location dissolveLoc = bd.getLocation();
                    // Ash burst at dissolving level
                    DisplayBuilder.dustParticles(dissolveLoc, 12, 1.0, 195, 178, 158, 1.0f);
                    DisplayBuilder.dustParticles(dissolveLoc, 6, 0.8, 80, 70, 60, 0.8f);
                    DisplayBuilder.playSound(dissolveLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.6f);
                    bd.remove();
                }
                dissolveLevel++;
            }

            // Ash cloud throughout dissolution radius
            if (!detonated && ticksAlive % 6 == 0) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = Math.random() * 5.0;
                DisplayBuilder.dustParticles(
                        center.clone().add(Math.cos(angle) * dist, Math.random() * 3, Math.sin(angle) * dist),
                        3, 0.5, 190, 175, 155, 0.6f);
            }

            // Base detonation when last block dissolves
            if (!detonated && dissolveLevel >= 7) {
                detonated = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
                DisplayBuilder.dustParticles(center, 30, 2.0, 235, 115, 35, 1.5f);
                DisplayBuilder.dustParticles(center, 20, 2.0, 225, 90, 15, 1.2f);
                triggerImpactDamage(center);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PillarOfAsh(plugin); }
    }

    // =========================================================================
    // ATTACK 16 — Column Walk
    // 8 columns erupt sequentially in a line, 0.5s apart, creating a moving wall.
    // 4 hearts if column erupts beneath player.
    // =========================================================================
    public static class ColumnWalk extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int columnsPlaced = 0;
        private double walkAngle;

        public ColumnWalk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("column_walk", AttackType.ENVIRONMENTAL, 3));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts
            config.setImpactRadius(1.5);
            config.setDurationTicks(480); // 1s warn + 4s walk + 20s persist
            config.setCooldownTicks(560); // 28s
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            walkAngle = Math.random() * Math.PI;
            // Warning: first column position
            Location firstLoc = center.clone().add(Math.cos(walkAngle) * -6, 0, Math.sin(walkAngle) * -6);
            DisplayBuilder.dustParticles(firstLoc.clone().add(0, 0.1, 0), 6, 0.3, 220, 80, 10, 0.8f);
            DisplayBuilder.playSound(firstLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.6f);
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

            // Place columns: one every 10 ticks (0.5s)
            if (columnsPlaced < 8 && (ticksAlive - 20) % 10 == 0) {
                double offset = (columnsPlaced - 3.5) * 2.0;
                Location colLoc = center.clone().add(
                        Math.cos(walkAngle) * offset, 0, Math.sin(walkAngle) * offset);

                // Spawn 7-block column
                for (int y = 0; y < 7; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(colLoc.clone().add(0, y, 0), Material.BASALT);
                    h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.playSound(colLoc, Sound.BLOCK_STONE_PLACE, 1.0f, 0.7f);
                DisplayBuilder.dustParticles(colLoc, 8, 1.0, 80, 70, 65, 0.8f);
                DisplayBuilder.dustParticles(colLoc.clone().add(0, 7, 0), 4, 0.3, 230, 110, 30, 0.6f);

                // Damage players at eruption point
                triggerImpactDamage(colLoc);
                columnsPlaced++;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ColumnWalk(plugin); }
    }

    // =========================================================================
    // ATTACK 17 — Basalt Mortar
    // Column tip fires 3 arcing projectiles at player positions.
    // 7 hearts direct hit (1-block), 4 hearts splash (3-block).
    // =========================================================================
    public static class BasaltMortar extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int shotsFired = 0;
        private int chargeComplete = 0;

        public BasaltMortar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("basalt_mortar", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(340); // 2s charge + 3 shots with 4s gaps
            config.setCooldownTicks(600); // 30s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spawn mortar column
            for (int y = 0; y < 8; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.BASALT);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Charge: glow at column tip for 40 ticks (2s)
            if (ticksAlive < 40) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 8.5, 0), 6, 0.3, 240, 110, 30, 1.0f);
                }
                return;
            }

            // Fire 3 shots with 80-tick (4s) gaps
            int fireTick = ticksAlive - 40;
            if (shotsFired < 3 && fireTick % 80 == 0) {
                // Find nearest player as target
                Player target = null;
                double closestDist = Double.MAX_VALUE;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distanceSquared(center);
                    if (dist < closestDist) {
                        closestDist = dist;
                        target = p;
                    }
                }

                if (target != null) {
                    Location targetLoc = target.getLocation().clone();
                    // Fire visual — particle arc
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 1.5f);
                    DisplayBuilder.dustParticles(center.clone().add(0, 8.5, 0), 12, 0.5, 255, 140, 30, 1.2f);

                    // Landing indicator
                    DisplayBuilder.dustParticles(targetLoc.clone().add(0, 0.1, 0), 8, 1.5, 220, 80, 10, 0.8f);

                    // Spawn impact block at target after delay (simulated via immediate for simplicity)
                    BlockDisplayHandle impactH = displayBuilder.spawnBlock(targetLoc, Material.MAGMA_BLOCK);
                    impactH.scale(1.5f, 0.3f, 1.5f).glow(255, 100, 0).interpolation(3, 0);
                    handles.add(impactH);
                    spawnedEntities.add(impactH.entity());

                    DisplayBuilder.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);

                    // Direct hit damage (1-block radius)
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double distSq = p.getLocation().distanceSquared(targetLoc);
                        if (distSq <= 1.0) {
                            p.damage(12.0); // 6 hearts direct
                        } else if (distSq <= 9.0) {
                            p.damage(8.0); // 4 hearts splash
                        }
                    }
                }
                shotsFired++;
            }

            // Ambient charge glow between shots
            if (shotsFired < 3 && fireTick % 10 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 8.5, 0), 3, 0.2, 240, 110, 30, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BasaltMortar(plugin); }
    }

    // =========================================================================
    // ATTACK 18 — Basalt Curtain
    // 8 columns in a line divide the arena. Flame fills gaps between columns.
    // 3 hearts if caught in eruption. 2 hearts passing through gap flames.
    // =========================================================================
    public static class BasaltCurtain extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean curtainUp = false;
        private double curtainAngle;
        private final List<Location> gapLocations = new ArrayList<>();

        public BasaltCurtain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("basalt_curtain", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(340); // 1.5s warn + 15s active
            config.setCooldownTicks(640); // 32s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            curtainAngle = Math.random() * Math.PI;
            // Warning: soul fire line tracing curtain path
            double perpAngle = curtainAngle + Math.PI / 2;
            for (int i = -7; i <= 7; i++) {
                DisplayBuilder.dustParticles(
                        center.clone().add(Math.cos(perpAngle) * i * 2, 0.1, Math.sin(perpAngle) * i * 2),
                        2, 0.2, 0, 150, 255, 0.6f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.6f);
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

            // Erupt curtain
            if (!curtainUp && ticksAlive >= 30) {
                curtainUp = true;
                double perpAngle = curtainAngle + Math.PI / 2;

                for (int col = 0; col < 8; col++) {
                    double offset = (col - 3.5) * 2.5;
                    Location colLoc = center.clone().add(Math.cos(perpAngle) * offset, 0, Math.sin(perpAngle) * offset);

                    for (int y = 0; y < 8; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(colLoc.clone().add(0, y, 0), Material.BASALT);
                        h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }

                    // Track gap between columns
                    if (col < 7) {
                        double gapOffset = offset + 1.25;
                        gapLocations.add(center.clone().add(Math.cos(perpAngle) * gapOffset, 0, Math.sin(perpAngle) * gapOffset));
                    }

                    DisplayBuilder.playSound(colLoc, Sound.BLOCK_STONE_PLACE, 1.0f, 0.7f);
                }

                // Eruption damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (int col = 0; col < 8; col++) {
                        double offset = (col - 3.5) * 2.5;
                        Location colLoc = center.clone().add(Math.cos(perpAngle) * offset, 0, Math.sin(perpAngle) * offset);
                        if (p.getLocation().distanceSquared(colLoc) <= 2.25) {
                            p.damage(6.0); // 3 hearts
                            break;
                        }
                    }
                }
            }

            // Flame fill in gaps + gap damage
            if (curtainUp && ticksAlive % 8 == 0) {
                for (Location gapLoc : gapLocations) {
                    DisplayBuilder.dustParticles(gapLoc.clone().add(0, 2, 0), 4, 1.5, 240, 120, 40, 0.8f);

                    // Damage players passing through gaps
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(gapLoc) <= 2.25) {
                            p.damage(4.0); // 2 hearts
                        }
                    }
                }

                // Lava drip from column tops
                if (ticksAlive % 16 == 0) {
                    int colIdx = (int)(Math.random() * 8);
                    double perpAngle = curtainAngle + Math.PI / 2;
                    double offset = (colIdx - 3.5) * 2.5;
                    Location dripLoc = center.clone().add(Math.cos(perpAngle) * offset, 8, Math.sin(perpAngle) * offset);
                    DisplayBuilder.dustParticles(dripLoc, 3, 0.3, 230, 95, 15, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BasaltCurtain(plugin); }
    }

    // =========================================================================
    // ATTACK 19 — Fault Column
    // Column rises revealing magma base. Magma base deals 3 hearts/s.
    // On descent, lava ring: 5 hearts in 4-block radius.
    // =========================================================================
    public static class FaultColumn extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean risen = false;
        private boolean descended = false;
        private int riseHeight = 0;

        public FaultColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fault_column", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(520); // 3s warn + 20s raised + descent
            config.setCooldownTicks(500); // 25s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spawn base column
            for (int y = 0; y < 8; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0), Material.BASALT);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 6, 0.5, 220, 80, 10, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 60 ticks (3s) — cracking below column
            if (ticksAlive < 60) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 5, 0.5, 220, 80, 10, 0.5f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.4f, 0.6f);
                }
                return;
            }

            // Rise phase: column lifts, revealing 3-block magma base
            if (!risen && ticksAlive < 120) {
                int riseTick = ticksAlive - 60;
                float riseOffset = riseTick * 0.1f; // ~6 blocks rise over 60 ticks

                for (int i = 0; i < handles.size(); i++) {
                    BlockDisplay bd = handles.get(i).entity();
                    Location newLoc = center.clone().add(0, i + riseOffset, 0);
                    bd.teleport(newLoc);
                }

                // Spawn magma base blocks as they become visible
                if (riseTick % 20 == 0 && riseHeight < 3) {
                    BlockDisplayHandle mh = displayBuilder.spawnBlock(center.clone().add(0, riseHeight, 0), Material.MAGMA_BLOCK);
                    mh.scale(1.0f, 1.0f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                    handles.add(mh);
                    spawnedEntities.add(mh.entity());
                    riseHeight++;
                }

                if (riseTick >= 55) {
                    risen = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);
                }
                return;
            }

            // Raised phase: magma base deals sustained damage
            if (risen && !descended) {
                if (ticksAlive % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location ploc = p.getLocation();
                        double dx = ploc.getX() - center.getX();
                        double dz = ploc.getZ() - center.getZ();
                        double dy = ploc.getY() - center.getY();
                        if (dx * dx + dz * dz <= 2.25 && dy >= 0 && dy <= 3) {
                            p.damage(6.0); // 3 hearts/s
                        }
                    }
                    // Lava and flame from magma base
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 5, 0.8, 230, 95, 18, 0.8f);
                }

                // Descent at tick 460 (20s after rise)
                if (ticksAlive >= 460) {
                    descended = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);

                    // Lava ring on descent
                    DisplayBuilder.particleRing(center, 4.0, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(230, 95, 18), 1.5f));
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 16.0) {
                            p.damage(10.0); // 5 hearts
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FaultColumn(plugin); }
    }

    // =========================================================================
    // ATTACK 20 — Basalt Shrapnel Storm
    // 3 columns shatter simultaneously, firing 15 shards across arena.
    // 5 hearts per shard hit (max 1 shard per player).
    // =========================================================================
    public static class BasaltShrapnelStorm extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> shardLandings = new ArrayList<>();
        private boolean shattered = false;
        private int shardsLanded = 0;

        public BasaltShrapnelStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("basalt_shrapnel_storm", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(400); // 1.5s warn + 1.5s travel + 15s debris
            config.setCooldownTicks(560); // 28s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spawn 3 columns at different positions
            double[] angles = {0, 2 * Math.PI / 3, 4 * Math.PI / 3};
            for (double angle : angles) {
                Location colLoc = center.clone().add(Math.cos(angle) * 5, 0, Math.sin(angle) * 5);
                for (int y = 0; y < 6; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(colLoc.clone().add(0, y, 0), Material.BASALT);
                    h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Pre-calculate 15 shard landing positions (5 per column, fan spread)
            for (double colAngle : angles) {
                Location colLoc = center.clone().add(Math.cos(colAngle) * 5, 0, Math.sin(colAngle) * 5);
                for (int s = 0; s < 5; s++) {
                    double shardAngle = colAngle + (s - 2) * 0.25;
                    double dist = 4.0 + Math.random() * 6.0;
                    shardLandings.add(colLoc.clone().add(Math.cos(shardAngle) * dist, 0, Math.sin(shardAngle) * dist));
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Shudder phase: 30 ticks (1.5s) — columns shake with ash
            if (ticksAlive < 30) {
                if (ticksAlive % 3 == 0) {
                    double[] angles = {0, 2 * Math.PI / 3, 4 * Math.PI / 3};
                    for (double angle : angles) {
                        Location colLoc = center.clone().add(Math.cos(angle) * 5, 3, Math.sin(angle) * 5);
                        DisplayBuilder.dustParticles(colLoc, 4, 0.5, 190, 175, 155, 0.7f);
                    }
                }
                return;
            }

            // Shatter
            if (!shattered) {
                shattered = true;
                // Remove top halves of all 3 columns
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.4f);
                double[] angles = {0, 2 * Math.PI / 3, 4 * Math.PI / 3};
                for (double angle : angles) {
                    Location colLoc = center.clone().add(Math.cos(angle) * 5, 4, Math.sin(angle) * 5);
                    DisplayBuilder.dustParticles(colLoc, 20, 1.5, 80, 70, 60, 1.2f);
                    DisplayBuilder.dustParticles(colLoc, 10, 1.0, 245, 125, 35, 0.8f);
                }
            }

            // Shard landings: staggered over 15 ticks (0.75s)
            if (shardsLanded < 15 && ticksAlive >= 30) {
                int landTick = 30 + shardsLanded;
                if (ticksAlive == landTick) {
                    Location landing = shardLandings.get(shardsLanded);

                    // Landing indicator
                    DisplayBuilder.dustParticles(landing.clone().add(0, 0.1, 0), 4, 0.3, 220, 80, 10, 0.5f);

                    // Spawn debris block
                    BlockDisplayHandle dh = displayBuilder.spawnBlock(landing, Material.BASALT);
                    dh.scale(0.6f, 0.6f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                    handles.add(dh);
                    spawnedEntities.add(dh.entity());

                    DisplayBuilder.dustParticles(landing, 8, 0.8, 185, 170, 150, 0.8f);

                    // Damage: only 1 shard can hit per player per event
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(landing) <= 1.5) {
                            p.damage(10.0); // 5 hearts
                        }
                    }
                    shardsLanded++;
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BasaltShrapnelStorm(plugin); }
    }
}
