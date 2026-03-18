package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.boss;

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
 * Phase 2 Boss Attacks - GROUP 8: REALITY TEAR (#71-80)
 * Attacks that warp the arena, delete safe zones, create inescapable patterns.
 * Phase 2's spatial distortion language.
 * NO status effects - damage only.
 */
public final class RealityTear {

    private RealityTear() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ArenaInversion(plugin));
        registry.register(new SafeZoneErasure(plugin));
        registry.register(new SpatialFractureWeb(plugin));
        registry.register(new DimensionalBleed(plugin));
        registry.register(new RealityInversionStrike(plugin));
        registry.register(new RiftPrison(plugin));
        registry.register(new TemporalEcho(plugin));
        registry.register(new FloorDeletion(plugin));
        registry.register(new RealityCompression(plugin));
        registry.register(new LatticeOfRuin(plugin));
    }

    // ================================================================
    // 71. ARENA INVERSION - Gravity reversal visual, DoG charges during float
    // PORTAL particles flood upward, players lifted then dropped
    // ================================================================
    public static class ArenaInversion extends BossAttack {
        private final List<BlockDisplayHandle> inversionHandles = new ArrayList<>();
        private boolean inverted = false;

        public ArenaInversion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("arena_inversion", AttackType.BOSS, 2), "dog");
            config.setDamage(16.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ground-level ascending particle columns
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 / 8) * i;
                Location loc = center.clone().add(Math.cos(a) * 6, 0.1, Math.sin(a) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.15f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                inversionHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Buildup: portal particles rise from ground (0-20 ticks = 1 sec)
            if (ticksAlive < 20 && !inverted) {
                float rise = ticksAlive / 20.0f;
                for (BlockDisplayHandle h : inversionHandles) {
                    Location loc = h.entity().getLocation();
                    h.entity().teleport(loc.clone().add(0, rise * 0.5, 0));
                }
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double x = (Math.random() - 0.5) * 16;
                        double z = (Math.random() - 0.5) * 16;
                        DisplayBuilder.purpleDust(center.clone().add(x, rise * 4, z), 4, 1.0);
                    }
                }
            }
            // Inversion (tick 20): visual flip
            else if (ticksAlive == 20 && !inverted) {
                inverted = true;
                for (BlockDisplayHandle h : inversionHandles) h.entity().remove();
                inversionHandles.clear();

                // Ceiling blocks appearing overhead (simulated inverted ground)
                for (int x = -8; x <= 8; x += 3) {
                    for (int z = -8; z <= 8; z += 3) {
                        Location loc = center.clone().add(x, 10, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                        h.scale(3.0f, 0.3f, 3.0f).glow(128, 0, 255).interpolation(2, 0);
                        inversionHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // DoG charge streak through arena
                for (int seg = 0; seg < 12; seg++) {
                    Location segLoc = center.clone().add(-12 + seg * 2, 4, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(1, 0);
                    inversionHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.purpleDust(center.clone().add(0, 5, 0), 40, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
            }
            // Inverted phase: particles float upward (20-60 ticks = 2 sec)
            else if (inverted && ticksAlive < 60) {
                if (ticksAlive % 6 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double x = (Math.random() - 0.5) * 18;
                        double z = (Math.random() - 0.5) * 18;
                        DisplayBuilder.purpleDust(center.clone().add(x, 2 + Math.random() * 6, z), 3, 0.8);
                    }
                }
            }
            // Return to normal (tick 60): ceiling dissolves
            else if (ticksAlive == 60) {
                for (BlockDisplayHandle h : inversionHandles) h.entity().remove();
                inversionHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 30, 8.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ArenaInversion(plugin); }
    }

    // ================================================================
    // 72. SAFE ZONE ERASURE - Corruption fog-wall advances across arena
    // Two walls from opposite sides, 3 blocks/sec, continuous damage behind
    // ================================================================
    public static class SafeZoneErasure extends BossAttack {
        private final List<BlockDisplayHandle> wall1Handles = new ArrayList<>();
        private final List<BlockDisplayHandle> wall2Handles = new ArrayList<>();
        private float wall1Pos = -18;
        private float wall2Pos = 18;
        private boolean wall2Spawned = false;

        public SafeZoneErasure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("safe_zone_erasure", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wall 1: corruption fog from negative X
            for (int z = -12; z <= 12; z += 3) {
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add(-18, y, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                    h.scale(3.0f, 1.0f, 3.0f).glow(128, 0, 255).interpolation(2, 0);
                    wall1Handles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Wall 1 advances at ~3 blocks/sec
            if (ticksAlive > 0 && wall1Pos < 18) {
                wall1Pos += 0.15f;
                for (BlockDisplayHandle h : wall1Handles) {
                    Location loc = h.entity().getLocation();
                    h.entity().teleport(center.clone().add(wall1Pos,
                        loc.getY() - center.getY(), loc.getZ() - center.getZ()));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(wall1Pos, 2, 0), 10, 6.0);
                }
            }

            // Wall 2 spawns from opposite side after 120 ticks (6 sec)
            if (ticksAlive == 120 && !wall2Spawned) {
                wall2Spawned = true;
                wall2Pos = 18;
                for (int z = -12; z <= 12; z += 3) {
                    for (int y = 0; y < 4; y++) {
                        Location loc = center.clone().add(18, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                        h.scale(3.0f, 1.0f, 3.0f).glow(128, 0, 255).interpolation(2, 0);
                        wall2Handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.6f);
            }

            // Wall 2 advances
            if (wall2Spawned && wall2Pos > -18) {
                wall2Pos -= 0.15f;
                for (BlockDisplayHandle h : wall2Handles) {
                    Location loc = h.entity().getLocation();
                    h.entity().teleport(center.clone().add(wall2Pos,
                        loc.getY() - center.getY(), loc.getZ() - center.getZ()));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(wall2Pos, 2, 0), 10, 6.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SafeZoneErasure(plugin); }
    }

    // ================================================================
    // 73. SPATIAL FRACTURE WEB - Ground cracks spread from center
    // Network of damaging lines covering ~60% of the surface
    // ================================================================
    public static class SpatialFractureWeb extends BossAttack {
        private final List<BlockDisplayHandle> crackHandles = new ArrayList<>();
        private boolean cracksActive = false;
        private int pulsesCompleted = 0;

        public SpatialFractureWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spatial_fracture_web", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Origin fracture point
            BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0), Material.SEA_LANTERN);
            h.scale(0.5f, 0.08f, 0.5f).glow(240, 240, 255).interpolation(2, 0);
            crackHandles.add(h);
            spawnedEntities.add(h.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fractures spread from center outward (0-40 ticks = 2 sec)
            if (ticksAlive < 40 && !cracksActive) {
                if (ticksAlive % 4 == 0) {
                    float radius = (ticksAlive / 40.0f) * 14;
                    // Add crack lines in web pattern
                    int cracksToAdd = 3;
                    for (int c = 0; c < cracksToAdd; c++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = Math.random() * radius;
                        Location crackLoc = center.clone().add(
                            Math.cos(angle) * dist, 0.05, Math.sin(angle) * dist);
                        double crackAngle = angle + (Math.random() - 0.5) * 1.0;
                        // Crack line segment
                        for (int seg = 0; seg < 3; seg++) {
                            Location segLoc = crackLoc.clone().add(
                                Math.cos(crackAngle) * seg, 0, Math.sin(crackAngle) * seg);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.SEA_LANTERN);
                            h.scale(0.15f, 0.06f, 1.2f).glow(240, 240, 255).interpolation(2, 0);
                            crackHandles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 6, radius * 0.5);
                }
            }
            // Cracks become active (tick 40)
            else if (ticksAlive == 40 && !cracksActive) {
                cracksActive = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
            }
            // Cracks pulse every 40 ticks (2 sec), expanding momentarily
            else if (cracksActive && ticksAlive % 40 == 0 && pulsesCompleted < 5) {
                pulsesCompleted++;
                // Scale up all cracks briefly
                for (BlockDisplayHandle h : crackHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.2f, -0.04f, -0.8f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.4f, 0.08f, 1.6f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 20, 10.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);
            }
            // Scale back after pulse
            else if (cracksActive && ticksAlive % 40 == 10) {
                for (BlockDisplayHandle h : crackHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.075f, -0.03f, -0.6f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.15f, 0.06f, 1.2f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpatialFractureWeb(plugin); }
    }

    // ================================================================
    // 74. DIMENSIONAL BLEED - 8-10 random bleed fountains across arena
    // Each occupies 3-block sphere of damage, persists 12 sec
    // ================================================================
    public static class DimensionalBleed extends BossAttack {
        private final List<BlockDisplayHandle> bleedHandles = new ArrayList<>();
        private int fountainsSpawned = 0;

        public DimensionalBleed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_bleed", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(440);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Bleed points appear sequentially, 6 ticks apart (0-60 ticks = 3 sec)
            int fountainIdx = ticksAlive / 6;
            if (fountainIdx >= 0 && fountainIdx < 10 && fountainsSpawned <= fountainIdx && ticksAlive % 6 == 0) {
                fountainsSpawned = fountainIdx + 1;

                // Random position weighted toward corridors
                double angle = Math.random() * Math.PI * 2;
                double dist = 4 + Math.random() * 10;
                Location bleedLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                // Fountain pillar
                for (int y = 0; y < 4; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        bleedLoc.clone().add(0, y + 1, 0), Material.CRYING_OBSIDIAN);
                    h.scale(0.4f, 0.8f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                    bleedHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Base ring
                for (int r = 0; r < 4; r++) {
                    double a = (Math.PI * 2 / 4) * r;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        bleedLoc.clone().add(Math.cos(a) * 1.5, 0.05, Math.sin(a) * 1.5), Material.DARK_PRISMARINE);
                    h.scale(0.4f, 0.1f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                    bleedHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.purpleDust(bleedLoc.clone().add(0, 2, 0), 12, 2.0);
                DisplayBuilder.playSound(bleedLoc, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 1.2f);
            }
            // Active fountains pulse particles
            else if (fountainsSpawned > 0 && ticksAlive % 12 == 0) {
                for (int i = 0; i < bleedHandles.size(); i += 8) {
                    DisplayBuilder.purpleDust(bleedHandles.get(i).entity().getLocation(), 5, 1.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalBleed(plugin); }
    }

    // ================================================================
    // 75. REALITY INVERSION STRIKE - Colors invert, DoG appears as
    // negative-image white outline and charges during inversion
    // ================================================================
    public static class RealityInversionStrike extends BossAttack {
        private final List<BlockDisplayHandle> invertHandles = new ArrayList<>();
        private boolean inverted = false;

        public RealityInversionStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_inversion_strike", AttackType.BOSS, 2), "dog");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Brief white flash (0.5 sec telegraph)
            DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 40, 15.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Flash telegraph (0-10 ticks = 0.5 sec)
            if (ticksAlive < 10 && !inverted) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 30, 12.0);
                }
            }
            // Inversion begins (tick 10)
            else if (ticksAlive == 10 && !inverted) {
                inverted = true;
                // Negative-image DoG outline (white segments)
                for (int seg = 0; seg < 8; seg++) {
                    Location segLoc = center.clone().add(-8 + seg * 2, 2, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.SEA_LANTERN);
                    h.scale(1.0f, 1.0f, 1.0f).glow(240, 240, 255).interpolation(1, 0);
                    invertHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Dark overlay blocks across arena
                for (int x = -10; x <= 10; x += 5) {
                    for (int z = -10; z <= 10; z += 5) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 6, z), Material.POLISHED_BLACKSTONE);
                        h.scale(5.0f, 0.2f, 5.0f).glow(0, 200, 255).interpolation(2, 0);
                        invertHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.3f);
            }
            // Inverted charge (10-70 ticks = 3 sec)
            else if (inverted && ticksAlive > 10 && ticksAlive < 70) {
                float progress = (ticksAlive - 10) / 60.0f;
                // White outline sweeps through arena
                for (int i = 0; i < 8 && i < invertHandles.size(); i++) {
                    double x = -8 + i * 2 + progress * 20;
                    invertHandles.get(i).entity().teleport(center.clone().add(x, 2, 0));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(progress * 20 - 8, 2, 0), 8, 2.0);
                }
            }
            // Inversion ends (tick 70)
            else if (ticksAlive == 70) {
                for (BlockDisplayHandle h : invertHandles) h.entity().remove();
                invertHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 25, 10.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityInversionStrike(plugin); }
    }

    // ================================================================
    // 76. RIFT PRISON - 4 rifts form a 12x12 block prison around player
    // Walls connect after 2 sec exit window, DoG enters to charge
    // ================================================================
    public static class RiftPrison extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private boolean wallsFormed = false;
        private boolean dogEntered = false;

        public RiftPrison(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_prison", AttackType.BOSS, 2), "dog");
            config.setDamage(18.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 4 cardinal rifts forming sequentially
            double[][] offsets = {{0, 0, -6}, {6, 0, 0}, {0, 0, 6}, {-6, 0, 0}};
            for (double[] off : offsets) {
                for (int y = 0; y < 6; y++) {
                    Location loc = center.clone().add(off[0], y, off[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                    h.scale(0.5f, 1.0f, 0.15f).glow(0, 200, 255).interpolation(3, 0);
                    riftHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rifts pulse (0-40 ticks = 2 sec exit window)
            if (ticksAlive < 40 && !wallsFormed) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, -6), 5, 1.0);
                    DisplayBuilder.cyanDust(center.clone().add(6, 3, 0), 5, 1.0);
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 6), 5, 1.0);
                    DisplayBuilder.cyanDust(center.clone().add(-6, 3, 0), 5, 1.0);
                }
            }
            // Walls form (tick 40): connect rifts with portal walls
            else if (ticksAlive == 40 && !wallsFormed) {
                wallsFormed = true;
                // North wall (z=-6)
                for (int x = -6; x <= 6; x += 2) {
                    for (int y = 0; y < 6; y += 2) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, -6), Material.CYAN_STAINED_GLASS);
                        h.scale(2.0f, 2.0f, 0.2f).glow(0, 200, 255).interpolation(2, 0);
                        wallHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                // South wall
                for (int x = -6; x <= 6; x += 2) {
                    for (int y = 0; y < 6; y += 2) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, 6), Material.CYAN_STAINED_GLASS);
                        h.scale(2.0f, 2.0f, 0.2f).glow(0, 200, 255).interpolation(2, 0);
                        wallHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                // East wall
                for (int z = -6; z <= 6; z += 2) {
                    for (int y = 0; y < 6; y += 2) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(6, y, z), Material.CYAN_STAINED_GLASS);
                        h.scale(0.2f, 2.0f, 2.0f).glow(0, 200, 255).interpolation(2, 0);
                        wallHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                // West wall
                for (int z = -6; z <= 6; z += 2) {
                    for (int y = 0; y < 6; y += 2) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(-6, y, z), Material.CYAN_STAINED_GLASS);
                        h.scale(0.2f, 2.0f, 2.0f).glow(0, 200, 255).interpolation(2, 0);
                        wallHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
            }
            // DoG enters prison (tick 60)
            else if (ticksAlive == 60 && !dogEntered) {
                dogEntered = true;
                // DoG body segments entering from north rift
                for (int seg = 0; seg < 6; seg++) {
                    Location segLoc = center.clone().add(0, 2, -6 + seg * 2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(1, 0);
                    wallHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 20, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.6f);
            }
            // Prison pulses while active (60-220 ticks = 8 sec)
            else if (wallsFormed && ticksAlive > 60 && ticksAlive < 220) {
                if (ticksAlive % 15 == 0) {
                    for (int i = 0; i < wallHandles.size(); i += 8) {
                        DisplayBuilder.cyanDust(wallHandles.get(i).entity().getLocation(), 3, 0.5);
                    }
                }
            }
            // Prison drops (tick 220)
            else if (ticksAlive == 220) {
                for (BlockDisplayHandle h : wallHandles) h.entity().remove();
                wallHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 30, 8.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftPrison(plugin); }
    }

    // ================================================================
    // 77. TEMPORAL ECHO - DoG charges, then 3 sec later an exact
    // echo replays the same path. No warning for the echo.
    // ================================================================
    public static class TemporalEcho extends BossAttack {
        private final List<BlockDisplayHandle> chargeHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> echoHandles = new ArrayList<>();
        private boolean charged = false;
        private boolean echoFired = false;
        private double chargeAngle = 0;

        public TemporalEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("temporal_echo", AttackType.BOSS, 2), "dog");
            config.setDamage(16.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            chargeAngle = Math.random() * Math.PI * 2;
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Initial charge (tick 20): normal telegraphed attack
            if (ticksAlive == 20 && !charged) {
                charged = true;
                for (int seg = 0; seg < 8; seg++) {
                    double dist = seg * 2;
                    Location segLoc = center.clone().add(
                        Math.cos(chargeAngle) * dist, 1.5, Math.sin(chargeAngle) * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(1, 0);
                    chargeHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 15, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.7f);
            }
            // Charge moves (20-40 ticks)
            else if (charged && ticksAlive > 20 && ticksAlive < 40) {
                float progress = (ticksAlive - 20) / 20.0f;
                for (int seg = 0; seg < chargeHandles.size(); seg++) {
                    double dist = seg * 2 + progress * 12;
                    chargeHandles.get(seg).entity().teleport(
                        center.clone().add(Math.cos(chargeAngle) * dist, 1.5, Math.sin(chargeAngle) * dist));
                }
            }
            // Charge fades (tick 40)
            else if (ticksAlive == 40) {
                for (BlockDisplayHandle h : chargeHandles) h.entity().remove();
                chargeHandles.clear();
            }
            // Echo fires 3 sec later (tick 80): same path, translucent blue tint
            else if (ticksAlive == 80 && !echoFired) {
                echoFired = true;
                for (int seg = 0; seg < 8; seg++) {
                    double dist = seg * 2;
                    Location segLoc = center.clone().add(
                        Math.cos(chargeAngle) * dist, 1.5, Math.sin(chargeAngle) * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.CYAN_STAINED_GLASS);
                    h.scale(0.9f, 0.9f, 0.9f).glow(0, 200, 255).interpolation(1, 0);
                    echoHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            }
            // Echo moves (80-100 ticks): same trajectory
            else if (echoFired && ticksAlive > 80 && ticksAlive < 100) {
                float progress = (ticksAlive - 80) / 20.0f;
                for (int seg = 0; seg < echoHandles.size(); seg++) {
                    double dist = seg * 2 + progress * 12;
                    echoHandles.get(seg).entity().teleport(
                        center.clone().add(Math.cos(chargeAngle) * dist, 1.5, Math.sin(chargeAngle) * dist));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(
                        Math.cos(chargeAngle) * (progress * 12), 1.5,
                        Math.sin(chargeAngle) * (progress * 12)), 6, 1.5);
                }
            }
            // Echo dissolves (tick 100)
            else if (ticksAlive == 100) {
                for (BlockDisplayHandle h : echoHandles) h.entity().remove();
                echoHandles.clear();
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TemporalEcho(plugin); }
    }

    // ================================================================
    // 78. FLOOR DELETION - 15-block radius circle of floor dissolves
    // Players fall through, teleported back to edge, 8 sec duration
    // ================================================================
    public static class FloorDeletion extends BossAttack {
        private final List<BlockDisplayHandle> flickerHandles = new ArrayList<>();
        private boolean dissolved = false;

        public FloorDeletion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floor_deletion", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Flickering ground indicators
            for (int x = -14; x <= 14; x += 3) {
                for (int z = -14; z <= 14; z += 3) {
                    if (x * x + z * z <= 196) {
                        Location loc = center.clone().add(x, 0.1, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                        h.scale(3.0f, 0.15f, 3.0f).glow(128, 0, 255).interpolation(2, 0);
                        flickerHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Flickering telegraph (0-30 ticks = 1.5 sec)
            if (ticksAlive < 30 && !dissolved) {
                // Toggle opacity on flickering blocks (2 Hz pulse)
                if (ticksAlive % 10 < 5) {
                    for (BlockDisplayHandle h : flickerHandles) {
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        bd.setTransformation(new Transformation(
                            new Vector3f(-1.5f, -0.075f, -1.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(3.0f, 0.15f, 3.0f),
                            new AxisAngle4f(0, 0, 1, 0)));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(3);
                    }
                } else {
                    for (BlockDisplayHandle h : flickerHandles) {
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        bd.setTransformation(new Transformation(
                            new Vector3f(-1.5f, -0.01f, -1.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(3.0f, 0.02f, 3.0f),
                            new AxisAngle4f(0, 0, 1, 0)));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(3);
                    }
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.purpleDust(center, 12, 10.0);
                }
            }
            // Floor dissolves (tick 30)
            else if (ticksAlive == 30 && !dissolved) {
                dissolved = true;
                // Change all floor blocks to dark void appearance
                for (BlockDisplayHandle h : flickerHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setBlock(Material.POLISHED_BLACKSTONE.createBlockData());
                    bd.setTransformation(new Transformation(
                        new Vector3f(-1.5f, -0.5f, -1.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(3.0f, 0.5f, 3.0f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(10);
                }
                DisplayBuilder.purpleDust(center, 40, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.3f);
            }
            // Void zone active with pulsing darkness (30-190 ticks = 8 sec)
            else if (dissolved && ticksAlive < 190) {
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.purpleDust(center, 8, 12.0);
                }
            }
            // Floor restores (tick 190)
            else if (ticksAlive == 190) {
                for (BlockDisplayHandle h : flickerHandles) h.entity().remove();
                flickerHandles.clear();
                DisplayBuilder.cyanDust(center, 20, 12.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloorDeletion(plugin); }
    }

    // ================================================================
    // 79. REALITY COMPRESSION - Advancing perimeter wall ring
    // Shrinks from 25-block to 8-block radius over 8 sec
    // ================================================================
    public static class RealityCompression extends BossAttack {
        private final List<BlockDisplayHandle> wallRingHandles = new ArrayList<>();
        private float wallRadius = 25.0f;
        private boolean compressionActive = false;

        public RealityCompression(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_compression", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Outer perimeter wall ring
            for (int i = 0; i < 32; i++) {
                double a = (Math.PI * 2 / 32) * i;
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add(Math.cos(a) * 25, y, Math.sin(a) * 25);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                    h.scale(2.0f, 1.0f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                    wallRingHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Wall visible at edge (0-30 ticks = 1.5 sec telegraph)
            if (ticksAlive < 30 && !compressionActive) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < wallRingHandles.size(); i += 12) {
                        DisplayBuilder.purpleDust(wallRingHandles.get(i).entity().getLocation(), 4, 1.0);
                    }
                }
            }
            // Compression begins (tick 30)
            else if (ticksAlive == 30 && !compressionActive) {
                compressionActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.4f);
            }
            // Wall advances inward (30-190 ticks = 8 sec, 25 -> 8 radius)
            else if (compressionActive && ticksAlive < 190) {
                // Starts slow (2 blocks/sec), accelerates to 5 blocks/sec
                float elapsed = (ticksAlive - 30) / 160.0f;
                float speedCurve = elapsed * elapsed; // Quadratic acceleration
                wallRadius = 25.0f - speedCurve * 17.0f;
                if (wallRadius < 8.0f) wallRadius = 8.0f;

                int segIdx = 0;
                for (int i = 0; i < 32; i++) {
                    double a = (Math.PI * 2 / 32) * i;
                    for (int y = 0; y < 3; y++) {
                        if (segIdx < wallRingHandles.size()) {
                            wallRingHandles.get(segIdx).entity().teleport(
                                center.clone().add(Math.cos(a) * wallRadius, y, Math.sin(a) * wallRadius));
                            segIdx++;
                        }
                    }
                }
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.purpleDust(center, 10, wallRadius);
                }
            }
            // Hold at 8-block radius (190-270 ticks = 4 sec)
            else if (ticksAlive >= 190 && ticksAlive < 270) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.purpleDust(center, 8, 8.0);
                }
            }
            // Release (tick 270)
            else if (ticksAlive == 270) {
                for (BlockDisplayHandle h : wallRingHandles) h.entity().remove();
                wallRingHandles.clear();
                DisplayBuilder.cyanDust(center, 30, 15.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityCompression(plugin); }
    }

    // ================================================================
    // 80. LATTICE OF RUIN - 3D beam grid fills the arena volume
    // Horizontal, vertical, and diagonal beams with 2x2 gaps
    // ================================================================
    public static class LatticeOfRuin extends BossAttack {
        private final List<BlockDisplayHandle> latticeHandles = new ArrayList<>();
        private int layersBuilt = 0;
        private boolean latticeActive = false;

        public LatticeOfRuin(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lattice_of_ruin", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(440);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Build lattice layer by layer (0-40 ticks = 2 sec)
            if (ticksAlive < 40 && !latticeActive) {
                if (ticksAlive % 8 == 0) {
                    int layer = ticksAlive / 8;
                    layersBuilt++;

                    if (layer < 2) {
                        // Horizontal beams (bottom to top)
                        double y = layer * 3;
                        for (int x = -10; x <= 10; x += 4) {
                            for (int z = -10; z <= 10; z += 4) {
                                BlockDisplayHandle h = displayBuilder.spawnBlock(
                                    center.clone().add(x, y, z), Material.END_ROD);
                                h.scale(0.15f, 0.15f, 4.0f).glow(240, 240, 255).interpolation(2, 0);
                                latticeHandles.add(h);
                                spawnedEntities.add(h.entity());
                            }
                        }
                    } else if (layer < 4) {
                        // Vertical beams
                        int vLayer = layer - 2;
                        for (int x = -10; x <= 10; x += 4) {
                            for (int z = -10; z <= 10; z += 4) {
                                BlockDisplayHandle h = displayBuilder.spawnBlock(
                                    center.clone().add(x, vLayer * 3, z), Material.END_ROD);
                                h.scale(0.15f, 3.0f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                                latticeHandles.add(h);
                                spawnedEntities.add(h.entity());
                            }
                        }
                    } else {
                        // Diagonal beams
                        for (int x = -8; x <= 8; x += 4) {
                            for (int y = 0; y <= 6; y += 3) {
                                BlockDisplayHandle h = displayBuilder.spawnBlock(
                                    center.clone().add(x, y, x), Material.SEA_LANTERN);
                                h.scale(0.15f, 0.15f, 2.0f).glow(0, 200, 255).interpolation(2, 0);
                                latticeHandles.add(h);
                                spawnedEntities.add(h.entity());
                            }
                        }
                    }
                    DisplayBuilder.cyanDust(center.clone().add(0, layer, 0), 10, 6.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f + layer * 0.2f);
                }
            }
            // Lattice activates (tick 40)
            else if (ticksAlive == 40 && !latticeActive) {
                latticeActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.5f);
            }
            // Lattice active with pulsing glow (40-240 ticks = 10 sec)
            else if (latticeActive && ticksAlive < 240) {
                if (ticksAlive % 15 == 0) {
                    for (int i = 0; i < latticeHandles.size(); i += 8) {
                        DisplayBuilder.cyanDust(latticeHandles.get(i).entity().getLocation(), 2, 0.3);
                    }
                }
            }
            // Lattice collapses (tick 240)
            else if (ticksAlive == 240) {
                for (BlockDisplayHandle h : latticeHandles) h.entity().remove();
                latticeHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 30, 10.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LatticeOfRuin(plugin); }
    }
}
