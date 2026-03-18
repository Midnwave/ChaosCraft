package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 1 Environmental — GROUP 10: CORRUPTION SPREAD
 * 10 attacks: slowly expanding zones of danger that grow outward over time.
 * Attacks 91-100 from boss1-voidmaw.md.
 *
 * Design notes:
 * - All attacks expand their zone progressively via ticksAlive gates
 * - Zone sizes increase over time; players must stay ahead of the spread
 * - No status effects — damage on zone contact only
 * - tracksPlayer = false (spreading zones are terrain-based)
 * - Calamity color palette: purple (128,0,255), cyan (0,200,255), crimson (200,0,50)
 */
public final class CorruptionSpread {

    private CorruptionSpread() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BlackRoot(plugin));
        registry.register(new SpreadingBlight(plugin));
        registry.register(new VoidBloomSpread(plugin));
        registry.register(new InkTide(plugin));
        registry.register(new RuneNetwork(plugin));
        registry.register(new WrathCreep(plugin));
        registry.register(new FractureWeb(plugin));
        registry.register(new ShadowStain(plugin));
        registry.register(new VoidCrystallization(plugin));
        registry.register(new TotalCorruption(plugin));
    }

    // =========================================================================
    // 91. BLACK ROOT — organic root network spreads from single tile
    // =========================================================================
    public static class BlackRoot extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> rootTiles = new ArrayList<>();
        private final List<Location> rootLocations = new ArrayList<>();
        private int lastGrowthTick = 0;
        private static final int MAX_ROOT_TILES = 50;

        public BlackRoot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_root", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(1.0); // Poison I proxy — 0.5 hearts per second
            config.setDamageRadius(1.2);
            config.setDurationTicks(1100); // 25s growth + 20s persist + 10s decay
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Seed tile
            BlockDisplayHandle seed = displayBuilder.spawnBlock(center.clone().add(0, 0.01, 0), Material.BLACK_CONCRETE);
            seed.scale(0.85f, 0.02f, 0.85f).glow(40, 0, 80).interpolation(3, 0);
            rootTiles.add(seed);
            rootLocations.add(center.clone());
            spawnedEntities.add(seed.entity());

            // Soft corruption sound
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Grow one tile per second (20 ticks) for up to 500 ticks (25 seconds)
            if (ticksAlive < 500 && ticksAlive - lastGrowthTick >= 20 && rootTiles.size() < MAX_ROOT_TILES) {
                lastGrowthTick = ticksAlive;

                // Branch from 2-3 existing root tips
                int branchCount = Math.min(3, rootLocations.size());
                for (int b = 0; b < branchCount; b++) {
                    if (rootLocations.isEmpty()) break;
                    int baseIdx = (rootLocations.size() - 1 - b);
                    if (baseIdx < 0) break;
                    Location baseLoc = rootLocations.get(baseIdx);

                    // Random neighboring tile
                    double[] dx = {2, -2, 0, 0, 1.4, -1.4};
                    double[] dz = {0, 0, 2, -2, 1.4, -1.4};
                    int dirIdx = (int)(Math.random() * 6);
                    Location newLoc = baseLoc.clone().add(dx[dirIdx], 0.01, dz[dirIdx]);

                    // Don't overlap
                    boolean overlapping = false;
                    for (Location existing : rootLocations) {
                        if (existing.distanceSquared(newLoc) < 1.0) {
                            overlapping = true;
                            break;
                        }
                    }
                    if (overlapping) continue;

                    BlockDisplayHandle root = displayBuilder.spawnBlock(newLoc, Material.BLACK_CONCRETE);
                    root.scale(0.85f, 0.02f, 0.85f).glow(60, 0, 120).interpolation(3, 0);
                    rootTiles.add(root);
                    rootLocations.add(newLoc);
                    spawnedEntities.add(root.entity());
                }
            }

            // Pulse root tiles
            if (ticksAlive % 20 == 0) {
                for (BlockDisplayHandle h : rootTiles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setGlowColorOverride(Color.fromRGB(
                            (int)(40 + Math.random() * 30),
                            0,
                            (int)(80 + Math.random() * 60)
                    ));
                    bd.setGlowing(true);
                }
            }

            // Decay after tick 900 — roots fade
            if (ticksAlive > 900 && ticksAlive % 30 == 0) {
                int decayCount = Math.min(5, rootTiles.size());
                for (int i = 0; i < decayCount; i++) {
                    if (!rootTiles.isEmpty()) {
                        BlockDisplay bd = (BlockDisplay) rootTiles.get(0).entity();
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.425f, -0.01f, -0.425f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.01f, 0.01f, 0.01f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(10);
                        rootTiles.remove(0);
                        if (!rootLocations.isEmpty()) rootLocations.remove(0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlackRoot(plugin); }
    }

    // =========================================================================
    // 92. SPREADING BLIGHT — 3x3 dead zone grows by 1 ring every 5 seconds
    // =========================================================================
    public static class SpreadingBlight extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> blightTiles = new ArrayList<>();
        private int lastExpansionTick = 0;
        private int blightRadius = 1; // starts 3x3 (radius 1 = -1 to +1)
        private static final int MAX_RADIUS = 7;

        public SpreadingBlight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spreading_blight", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // replaces debuffs — direct damage
            config.setDamageRadius(3.5); // current blight radius check per tick
            config.setDurationTicks(1100); // 35s growth + 20s persist
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Initial 3x3 dead zone
            spawnBlightRing(center, blightRadius);

            // "Wrong silence" — a momentary soft absence of ambient sound
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.15f, 0.1f);
        }

        private void spawnBlightRing(Location center, int radius) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Only spawn the outer ring edge
                    if (Math.abs(x) != radius && Math.abs(z) != radius) continue;
                    Location loc = center.clone().add(x * 1.8, 0.02, z * 1.8);
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(loc, Material.GRAY_CONCRETE);
                    tile.scale(1.7f, 0.04f, 1.7f).glow(30, 30, 30).interpolation(5, 0);
                    blightTiles.add(tile);
                    spawnedEntities.add(tile.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Update damage radius to match blight spread
            config.setDamageRadius(blightRadius * 1.8 + 1.5);

            // Expand every 5 seconds (100 ticks), up to max
            if (ticksAlive - lastExpansionTick >= 100 && blightRadius < MAX_RADIUS) {
                lastExpansionTick = ticksAlive;
                blightRadius++;
                spawnBlightRing(center, blightRadius);

                // Encroachment sound
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.3f);
                DisplayBuilder.purpleDust(center, 10, blightRadius * 1.5);
            }

            // Blight pulsing visual
            if (ticksAlive % 25 == 0) {
                for (BlockDisplayHandle h : blightTiles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    float pulse = 0.03f + (float)(Math.sin(ticksAlive * 0.1) * 0.01f);
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.85f, -pulse / 2, -0.85f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.7f, pulse * 2, 1.7f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(12);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpreadingBlight(plugin); }
    }

    // =========================================================================
    // 93. VOID BLOOM SPREAD — flowers double per generation, spore visible 5s ahead
    // =========================================================================
    public static class VoidBloomSpread extends EnvironmentalAttack {

        private static class VoidFlower {
            BlockDisplayHandle stem;
            BlockDisplayHandle petals;
            Location loc;
            int spawnTick;
            boolean sporesReleased;

            VoidFlower(BlockDisplayHandle stem, BlockDisplayHandle petals, Location loc, int spawnTick) {
                this.stem = stem;
                this.petals = petals;
                this.loc = loc;
                this.spawnTick = spawnTick;
                this.sporesReleased = false;
            }
        }

        private final List<VoidFlower> flowers = new ArrayList<>();
        private final List<BlockDisplayHandle> sporeMarkers = new ArrayList<>();

        public VoidBloomSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_bloom_spread", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts on flower contact
            config.setDamageRadius(1.2);
            config.setDurationTicks(480); // 4 generations * 5s + ramp
            config.setCooldownTicks(1400); // 70 seconds
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Initial void flower — black petals, purple center
            BlockDisplayHandle stem = displayBuilder.spawnBlock(center.clone().add(0, 0.3, 0), Material.BLACK_CONCRETE);
            stem.scale(0.1f, 0.5f, 0.1f).glow(50, 0, 100).interpolation(3, 0);
            spawnedEntities.add(stem.entity());

            BlockDisplayHandle petals = displayBuilder.spawnBlock(center.clone().add(0, 0.8, 0), Material.PURPLE_CONCRETE);
            petals.scale(0.8f, 0.15f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(petals.entity());

            flowers.add(new VoidFlower(stem, petals, center.clone(), 0));

            // Soft organic pop
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Check each flower for spore release (every 100 ticks = 5 seconds per generation)
            List<VoidFlower> newFlowers = new ArrayList<>();
            for (VoidFlower flower : flowers) {
                int age = ticksAlive - flower.spawnTick;

                // Flowers bloom fully after 20 ticks
                if (age == 20) {
                    BlockDisplay petalBd = (BlockDisplay) flower.petals.entity();
                    petalBd.setTransformation(new Transformation(
                            new Vector3f(-0.45f, -0.075f, -0.45f),
                            new AxisAngle4f(0, 1, 0, ticksAlive * 0.05f),
                            new Vector3f(0.9f, 0.15f, 0.9f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    petalBd.setInterpolationDelay(0);
                    petalBd.setInterpolationDuration(10);
                }

                // At age 100: release spores (spawn up to 3 new flowers within range)
                if (age == 100 && !flower.sporesReleased && flowers.size() < 20) {
                    flower.sporesReleased = true;

                    double[] sporeAngles = {0, Math.PI * 2 / 3, Math.PI * 4 / 3};
                    for (double angle : sporeAngles) {
                        double sporeRadius = 3.0 + Math.random() * 2.0;
                        Location sporeLoc = flower.loc.clone().add(
                                Math.cos(angle) * sporeRadius,
                                0,
                                Math.sin(angle) * sporeRadius
                        );

                        // Spore visual (floating marker) visible 5s ahead
                        BlockDisplayHandle spore = displayBuilder.spawnBlock(sporeLoc.clone().add(0, 0.5, 0), Material.PURPLE_STAINED_GLASS);
                        spore.scale(0.15f, 0.15f, 0.15f).glow(80, 0, 160).interpolation(3, 0);
                        sporeMarkers.add(spore);
                        spawnedEntities.add(spore.entity());

                        // New flower blooms at this location 100 ticks later
                        BlockDisplayHandle newStem = displayBuilder.spawnBlock(sporeLoc.clone().add(0, 0.3, 0), Material.BLACK_CONCRETE);
                        newStem.scale(0.01f, 0.01f, 0.01f).glow(30, 0, 60).interpolation(3, 0); // start invisible
                        spawnedEntities.add(newStem.entity());

                        BlockDisplayHandle newPetals = displayBuilder.spawnBlock(sporeLoc.clone().add(0, 0.8, 0), Material.PURPLE_CONCRETE);
                        newPetals.scale(0.01f, 0.01f, 0.01f).glow(80, 0, 160).interpolation(3, 0);
                        spawnedEntities.add(newPetals.entity());

                        newFlowers.add(new VoidFlower(newStem, newPetals, sporeLoc, ticksAlive + 100));
                    }

                    DisplayBuilder.purpleDust(flower.loc, 15, 2.0);
                    DisplayBuilder.playSound(flower.loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.5f);
                }

                // Grow stem and petals at the flower's bloom time
                if (age == flower.spawnTick + 100) {
                    BlockDisplay stemBd = (BlockDisplay) flower.stem.entity();
                    stemBd.setTransformation(new Transformation(
                            new Vector3f(-0.05f, -0.25f, -0.05f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.1f, 0.5f, 0.1f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    stemBd.setInterpolationDelay(0);
                    stemBd.setInterpolationDuration(10);

                    BlockDisplay petalBd = (BlockDisplay) flower.petals.entity();
                    petalBd.setTransformation(new Transformation(
                            new Vector3f(-0.4f, -0.075f, -0.4f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.8f, 0.15f, 0.8f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    petalBd.setInterpolationDelay(0);
                    petalBd.setInterpolationDuration(10);
                }
            }
            flowers.addAll(newFlowers);

            // Petals gently rotate
            if (ticksAlive % 15 == 0) {
                for (VoidFlower flower : flowers) {
                    BlockDisplay petalBd = (BlockDisplay) flower.petals.entity();
                    petalBd.setTransformation(new Transformation(
                            new Vector3f(-0.45f, -0.075f, -0.45f),
                            new AxisAngle4f(ticksAlive * 0.03f, 0, 1, 0),
                            new Vector3f(0.9f, 0.15f, 0.9f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    petalBd.setInterpolationDelay(0);
                    petalBd.setInterpolationDuration(8);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidBloomSpread(plugin); }
    }

    // =========================================================================
    // 94. INK TIDE — viscous void ink spreads from island edge every 3 seconds
    // =========================================================================
    public static class InkTide extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> inkTiles = new ArrayList<>();
        private final List<Location> inkLocations = new ArrayList<>();
        private int lastSpreadTick = 0;
        private static final int MAX_TILES = 60;

        public InkTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ink_tide", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(1.0); // 0.5 hearts per 2 seconds
            config.setDamageRadius(1.5);
            config.setDurationTicks(1300); // 30s spread + 20s persist + 15s dry
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Start from island edge — one corner tile
            Location edgeLoc = center.clone().add(12, 0.01, -12);
            spawnInkTile(edgeLoc);

            // Wet sound
            DisplayBuilder.playSound(edgeLoc, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.2f);
        }

        private void spawnInkTile(Location loc) {
            BlockDisplayHandle tile = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
            tile.scale(1.6f, 0.03f, 1.6f).glow(20, 0, 40).interpolation(8, 0); // slow flow
            inkTiles.add(tile);
            inkLocations.add(loc);
            spawnedEntities.add(tile.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spread every 60 ticks (3 seconds)
            if (ticksAlive < 600 && ticksAlive - lastSpreadTick >= 60 && inkTiles.size() < MAX_TILES) {
                lastSpreadTick = ticksAlive;

                // Spread to adjacent tiles from existing ink
                int spreadFrom = Math.max(0, inkLocations.size() - 3);
                for (int i = spreadFrom; i < inkLocations.size(); i++) {
                    Location base = inkLocations.get(i);
                    double[][] dirs = {{1.8, 0}, {-1.8, 0}, {0, 1.8}, {0, -1.8}};
                    for (double[] dir : dirs) {
                        Location newLoc = base.clone().add(dir[0], 0, dir[1]);
                        // Bounds check — stay within arena
                        if (Math.abs(newLoc.getX() - center.getX()) > 14) continue;
                        if (Math.abs(newLoc.getZ() - center.getZ()) > 14) continue;
                        // No overlap
                        boolean alreadyInked = false;
                        for (Location existing : inkLocations) {
                            if (existing.distanceSquared(newLoc) < 1.5) {
                                alreadyInked = true;
                                break;
                            }
                        }
                        if (!alreadyInked && inkTiles.size() < MAX_TILES) {
                            spawnInkTile(newLoc);
                        }
                    }
                    break; // spread one tile at a time for viscous effect
                }

                DisplayBuilder.purpleDust(center, 4, inkLocations.size() * 0.1);
            }

            // Viscous shimmer on ink tiles
            if (ticksAlive % 30 == 0) {
                for (BlockDisplayHandle h : inkTiles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    float shimmer = 0.025f + (float)(Math.random() * 0.01f);
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.8f, -shimmer / 2, -0.8f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.6f, shimmer * 2, 1.6f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(15);
                }
            }

            // Drying phase (tick 1000+): tiles fade and shrink
            if (ticksAlive > 1000 && ticksAlive % 50 == 0) {
                int dryCount = Math.min(5, inkTiles.size());
                for (int i = 0; i < dryCount; i++) {
                    if (!inkTiles.isEmpty()) {
                        BlockDisplay bd = (BlockDisplay) inkTiles.get(0).entity();
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.8f, -0.005f, -0.8f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(1.6f, 0.005f, 1.6f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(20);
                        inkTiles.remove(0);
                        if (!inkLocations.isEmpty()) inkLocations.remove(0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InkTide(plugin); }
    }

    // =========================================================================
    // 95. RUNE NETWORK — geometric circuit grows then fully activates for damage
    // =========================================================================
    public static class RuneNetwork extends EnvironmentalAttack {

        private static class RuneNode {
            BlockDisplayHandle display;
            Location loc;
            int placedTick;
            boolean fullyActive;

            RuneNode(BlockDisplayHandle display, Location loc, int placedTick) {
                this.display = display;
                this.loc = loc;
                this.placedTick = placedTick;
                this.fullyActive = false;
            }
        }

        private final List<RuneNode> nodes = new ArrayList<>();
        private final List<BlockDisplayHandle> connectorLines = new ArrayList<>();
        private int lastNodeTick = 0;
        private static final int MAX_NODES = 16;

        public RuneNetwork(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rune_network", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts per second in active node radius
            config.setDamageRadius(1.5);
            config.setDurationTicks(900); // 20s build + 25s active + fade
            config.setCooldownTicks(1600); // 80 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Initial rune node
            BlockDisplayHandle initial = displayBuilder.spawnBlock(center.clone().add(0, 0.01, 0), Material.PURPLE_CONCRETE);
            initial.scale(0.6f, 0.02f, 0.6f).glow(60, 0, 120).interpolation(4, 0);
            nodes.add(new RuneNode(initial, center.clone(), 0));
            spawnedEntities.add(initial.entity());

            // Distinctive rune glyph sound
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.6f);
            DisplayBuilder.purpleDust(center, 10, 1.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Network construction: add a node every 25 ticks
            if (ticksAlive < 400 && ticksAlive - lastNodeTick >= 25 && nodes.size() < MAX_NODES) {
                lastNodeTick = ticksAlive;

                // Angular circuit pattern — geometric and angular
                int nodeIdx = nodes.size();
                double baseAngle = nodeIdx * (Math.PI / 4); // 45-degree steps
                double radius = 2.0 + (nodeIdx / 4) * 2.5; // grows in rings
                Location newLoc = center.clone().add(
                        Math.cos(baseAngle) * radius,
                        0.01,
                        Math.sin(baseAngle) * radius
                );

                BlockDisplayHandle newNode = displayBuilder.spawnBlock(newLoc, Material.PURPLE_CONCRETE);
                newNode.scale(0.5f, 0.02f, 0.5f).glow(70, 0, 140).interpolation(3, 0);
                nodes.add(new RuneNode(newNode, newLoc, ticksAlive));
                spawnedEntities.add(newNode.entity());

                // Connector line from last node to this one
                if (nodes.size() >= 2) {
                    Location prevLoc = nodes.get(nodes.size() - 2).loc;
                    Location midLoc = prevLoc.clone().add(
                            (newLoc.getX() - prevLoc.getX()) * 0.5,
                            0,
                            (newLoc.getZ() - prevLoc.getZ()) * 0.5
                    );
                    BlockDisplayHandle line = displayBuilder.spawnBlock(midLoc, Material.CRYING_OBSIDIAN);
                    line.scale(0.1f, 0.015f, (float)(newLoc.distance(prevLoc))).glow(80, 0, 160).interpolation(3, 0);
                    connectorLines.add(line);
                    spawnedEntities.add(line.entity());
                }

                // Extending line sound
                DisplayBuilder.playSound(newLoc, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 1.5f);
            }

            // Nodes fully activate 5 seconds (100 ticks) after placement
            for (RuneNode node : nodes) {
                if (!node.fullyActive && ticksAlive - node.placedTick >= 100) {
                    node.fullyActive = true;
                    BlockDisplay bd = (BlockDisplay) node.display.entity();
                    bd.setGlowColorOverride(Color.fromRGB(128, 0, 255));
                    bd.setGlowing(true);
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.35f, -0.015f, -0.35f),
                            new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                            new Vector3f(0.7f, 0.03f, 0.7f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                    DisplayBuilder.purpleDust(node.loc, 10, 1.5);
                }
            }

            // Active nodes deal damage per tick (delegated to parent radius system
            // but center shifts to each node manually every few ticks)
            if (ticksAlive % 20 == 0) {
                for (RuneNode node : nodes) {
                    if (!node.fullyActive) continue;
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(node.loc) <= 1.5 * 1.5) {
                            p.damage(4.0);
                        }
                    }
                }
            }

            // Network fade after tick 700
            if (ticksAlive > 700 && ticksAlive % 40 == 0) {
                for (RuneNode node : nodes) {
                    if (node.fullyActive) {
                        BlockDisplay bd = (BlockDisplay) node.display.entity();
                        bd.setGlowColorOverride(Color.fromRGB(30, 0, 60));
                        bd.setGlowing(true);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RuneNetwork(plugin); }
    }

    // =========================================================================
    // 96. WRATH CREEP — tiles transform dark, older tiles deal more damage
    // =========================================================================
    public static class WrathCreep extends EnvironmentalAttack {

        private static class CreepTile {
            BlockDisplayHandle display;
            Location loc;
            int transformedTick;

            CreepTile(BlockDisplayHandle display, Location loc, int transformedTick) {
                this.display = display;
                this.loc = loc;
                this.transformedTick = transformedTick;
            }
        }

        private final List<CreepTile> creepTiles = new ArrayList<>();
        private int lastCreepTick = 0;

        public WrathCreep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wrath_creep", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0); // custom damage handled per tick
            config.setDamageRadius(0.0);
            config.setDurationTicks(900); // 45 seconds
            config.setCooldownTicks(2000); // 100 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Slow start — unnoticeable for first 5 seconds. Spawn seed tile.
            Location seedLoc = center.clone().add(2, 0.01, 1);
            BlockDisplayHandle seed = displayBuilder.spawnBlock(seedLoc, Material.DEEPSLATE_TILES);
            seed.scale(1.8f, 0.02f, 1.8f).glow(15, 0, 15).interpolation(10, 0);
            creepTiles.add(new CreepTile(seed, seedLoc, 0));
            spawnedEntities.add(seed.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spread new tile every 15 ticks
            if (ticksAlive - lastCreepTick >= 15 && creepTiles.size() < 40) {
                lastCreepTick = ticksAlive;

                if (!creepTiles.isEmpty()) {
                    int baseIdx = Math.max(0, creepTiles.size() - 2);
                    Location baseLoc = creepTiles.get(baseIdx).loc;

                    double angle = Math.random() * 2 * Math.PI;
                    Location newLoc = baseLoc.clone().add(Math.cos(angle) * 2.0, 0.01, Math.sin(angle) * 2.0);
                    if (Math.abs(newLoc.getX() - center.getX()) > 13) return;
                    if (Math.abs(newLoc.getZ() - center.getZ()) > 13) return;

                    BlockDisplayHandle tile = displayBuilder.spawnBlock(newLoc, Material.DEEPSLATE_TILES);
                    tile.scale(1.8f, 0.02f, 1.8f).glow(15, 0, 15).interpolation(8, 0);
                    creepTiles.add(new CreepTile(tile, newLoc, ticksAlive));
                    spawnedEntities.add(tile.entity());
                }
            }

            // Scale damage and visual based on tile age
            if (ticksAlive % 10 == 0) {
                for (CreepTile tile : creepTiles) {
                    int age = ticksAlive - tile.transformedTick;
                    float ageProgress = Math.min(1.0f, age / 400.0f); // max damage at 20 seconds

                    // Visual darkens with age
                    int redVein = (int)(50 + ageProgress * 100); // 50 -> 150 (deep red-purple vein)
                    int blueVein = (int)(10 + ageProgress * 40);
                    BlockDisplay bd = (BlockDisplay) tile.display.entity();
                    bd.setGlowColorOverride(Color.fromRGB(redVein, 0, blueVein));
                    bd.setGlowing(true);

                    // Pulse amplitude increases with age
                    float pulse = 0.02f + ageProgress * 0.02f;
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.9f, -pulse, -0.9f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.8f, pulse * 2, 1.8f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);

                    // Damage scales 0.5->2 hearts/sec based on age
                    double dmgPerSec = 1.0 + ageProgress * 3.0; // 1 -> 4 per second
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(tile.loc) <= 2) {
                            p.damage(dmgPerSec * 0.5); // per 10 ticks
                        }
                    }
                }
            }

            // Cool-down: tiles gradually reset after tick 600
            if (ticksAlive > 600 && ticksAlive % 50 == 0) {
                if (!creepTiles.isEmpty()) {
                    BlockDisplay bd = (BlockDisplay) creepTiles.get(0).display.entity();
                    bd.setGlowColorOverride(Color.fromRGB(20, 20, 40));
                    bd.setGlowing(true);
                    creepTiles.remove(0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WrathCreep(plugin); }
    }

    // =========================================================================
    // 97. THE FRACTURE WEB — hairline cracks spread, stepped tiles trigger void vents
    // =========================================================================
    public static class FractureWeb extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> fractures = new ArrayList<>();
        private final List<Location> fractureLocs = new ArrayList<>();
        private int lastFractureTick = 0;

        public FractureWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fracture_web", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts + upward push on vent trigger
            config.setDamageRadius(1.5);
            config.setDurationTicks(1500); // 30s spread + 30s persist + 15s fade
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(999); // custom random triggering below
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central fracture point
            BlockDisplayHandle centralFracture = displayBuilder.spawnBlock(center.clone().add(0, 0.01, 0), Material.CRACKED_DEEPSLATE_TILES);
            centralFracture.scale(0.8f, 0.015f, 0.8f).glow(50, 0, 100).interpolation(3, 0);
            fractures.add(centralFracture);
            fractureLocs.add(center.clone());
            spawnedEntities.add(centralFracture.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spread fractures outward — 1 new fracture every 10 ticks for 600 ticks
            if (ticksAlive < 600 && ticksAlive - lastFractureTick >= 10 && fractures.size() < 80) {
                lastFractureTick = ticksAlive;

                if (!fractureLocs.isEmpty()) {
                    int baseIdx = (int)(Math.random() * Math.min(fractureLocs.size(), 10));
                    Location baseLoc = fractureLocs.get(baseIdx);

                    double angle = Math.random() * 2 * Math.PI;
                    double length = 1.5 + Math.random() * 2.0;
                    Location fractureLoc = baseLoc.clone().add(
                            Math.cos(angle) * length,
                            0.01,
                            Math.sin(angle) * length
                    );

                    if (Math.abs(fractureLoc.getX() - center.getX()) > 15) return;
                    if (Math.abs(fractureLoc.getZ() - center.getZ()) > 15) return;

                    BlockDisplayHandle frac = displayBuilder.spawnBlock(fractureLoc, Material.CRACKED_DEEPSLATE_TILES);
                    frac.scale(0.5f, 0.012f, 0.5f).glow(40, 0, 80).interpolation(4, 0);
                    fractures.add(frac);
                    fractureLocs.add(fractureLoc);
                    spawnedEntities.add(frac.entity());
                }
            }

            // Random void vent triggering on fractured tiles
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < fractureLocs.size(); i++) {
                    Location fracLoc = fractureLocs.get(i);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(fracLoc) <= 2.25) { // 1.5 block radius
                            // 20% chance per step to trigger vent
                            if (Math.random() < 0.20) {
                                // Void vent: 2 hearts + upward push visual
                                p.damage(4.0);
                                p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 0.8, 0)));
                                DisplayBuilder.cyanDust(fracLoc, 15, 1.5);
                                DisplayBuilder.playSound(fracLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1.5f);
                            }
                        }
                    }
                }
            }

            // Fracture glow pulses
            if (ticksAlive % 30 == 0 && ticksAlive < 600) {
                for (BlockDisplayHandle h : fractures) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setGlowColorOverride(Color.fromRGB(
                            40 + (int)(Math.random() * 30),
                            0,
                            80 + (int)(Math.random() * 50)
                    ));
                    bd.setGlowing(true);
                }
            }

            // Fade after tick 1200
            if (ticksAlive > 1200 && ticksAlive % 60 == 0) {
                int fadeCount = Math.min(10, fractures.size());
                for (int i = 0; i < fadeCount; i++) {
                    if (!fractures.isEmpty()) {
                        BlockDisplay bd = (BlockDisplay) fractures.get(0).entity();
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.25f, -0.005f, -0.25f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.01f, 0.01f, 0.01f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(15);
                        fractures.remove(0);
                        if (!fractureLocs.isEmpty()) fractureLocs.remove(0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FractureWeb(plugin); }
    }

    // =========================================================================
    // 98. SHADOW STAIN — growing black shadow absorbs all effects within it
    // =========================================================================
    public static class ShadowStain extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> stainTiles = new ArrayList<>();
        private double currentRadius = 1.0;
        private int lastGrowthTick = 0;

        public ShadowStain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_stain", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(4.0); // 2 hearts on boundary crossing (entry/exit)
            config.setDamageRadius(1.5); // boundary detection — not interior
            config.setDurationTicks(1300); // 30s grow + 15s max + 20s recede
            config.setCooldownTicks(1700); // 85 seconds
            config.setTicksBetweenDamage(5); // frequent boundary check
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Initial small shadow stain
            BlockDisplayHandle seed = displayBuilder.spawnBlock(center.clone().add(0, 0.01, 0), Material.BLACK_CONCRETE);
            seed.scale(1.8f, 0.03f, 1.8f).glow(0, 0, 10).interpolation(5, 0);
            stainTiles.add(seed);
            spawnedEntities.add(seed.entity());

            // Horrible silencing sound
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Grow at 1 block per 2 seconds (40 ticks) for 30 seconds (600 ticks)
            if (ticksAlive < 600 && ticksAlive - lastGrowthTick >= 40) {
                lastGrowthTick = ticksAlive;
                currentRadius += 1.0;

                // Spawn ring of shadow tiles at new radius
                int ringCount = (int)(currentRadius * 4);
                for (int i = 0; i < ringCount; i++) {
                    double angle = (2 * Math.PI * i) / ringCount;
                    Location tileLoc = center.clone().add(
                            Math.cos(angle) * currentRadius,
                            0.01,
                            Math.sin(angle) * currentRadius
                    );
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(tileLoc, Material.BLACK_CONCRETE);
                    tile.scale(1.6f, 0.03f, 1.6f).glow(0, 0, 10).interpolation(5, 0);
                    stainTiles.add(tile);
                    spawnedEntities.add(tile.entity());
                }
            }

            // Boundary zone: deal damage at the edge
            if (ticksAlive % 5 == 0) {
                double boundaryMin = (currentRadius - 2.0) * (currentRadius - 2.0);
                double boundaryMax = (currentRadius + 1.0) * (currentRadius + 1.0);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double distSq = p.getLocation().distanceSquared(center);
                    if (distSq >= boundaryMin && distSq <= boundaryMax) {
                        p.damage(4.0); // 2 hearts boundary transition
                    }
                }
            }

            // Stain pulse visual — very slow
            if (ticksAlive % 40 == 0) {
                for (BlockDisplayHandle h : stainTiles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setGlowColorOverride(Color.fromRGB(0, 0, Math.min(40, (int)(ticksAlive * 0.05))));
                    bd.setGlowing(true);
                }
            }

            // Recession after tick 900
            if (ticksAlive > 900 && ticksAlive % 40 == 0 && !stainTiles.isEmpty()) {
                currentRadius = Math.max(0, currentRadius - 0.5);
                int removeCount = Math.min(8, stainTiles.size());
                for (int i = 0; i < removeCount; i++) {
                    if (!stainTiles.isEmpty()) {
                        BlockDisplay bd = (BlockDisplay) stainTiles.get(0).entity();
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.8f, -0.015f, -0.8f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.01f, 0.01f, 0.01f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(15);
                        stainTiles.remove(0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowStain(plugin); }
    }

    // =========================================================================
    // 99. VOID CRYSTALLIZATION — tiles crystallize, shrinking passable space
    // =========================================================================
    public static class VoidCrystallization extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> crystalTiles = new ArrayList<>();
        private final List<Location> crystalLocs = new ArrayList<>();
        private int lastCrystalTick = 0;
        private boolean shatterPhase = false;

        public VoidCrystallization(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_crystallization", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // 1 heart/sec pressed against crystal
            config.setDamageRadius(1.2);
            config.setDurationTicks(1100); // 35s spread + 20s persist + shatter
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Crystal tips just beginning to emerge — 2 second early warning
            Location seedLoc = center.clone().add(3, 0, 2);
            BlockDisplayHandle seedCrystal = displayBuilder.spawnBlock(seedLoc, Material.AMETHYST_CLUSTER);
            seedCrystal.scale(0.3f, 0.3f, 0.3f).glow(80, 0, 160).interpolation(5, 0);
            crystalTiles.add(seedCrystal);
            crystalLocs.add(seedLoc);
            spawnedEntities.add(seedCrystal.entity());

            DisplayBuilder.playSound(seedLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spread every 80 ticks (4 seconds)
            if (ticksAlive < 700 && ticksAlive - lastCrystalTick >= 80 && !shatterPhase) {
                lastCrystalTick = ticksAlive;

                // Tips emerge on adjacent tiles 2s before full crystallization
                for (int newTile = 0; newTile < Math.min(3, crystalLocs.size()); newTile++) {
                    int baseIdx = Math.max(0, crystalLocs.size() - 1 - newTile);
                    Location baseLoc = crystalLocs.get(baseIdx);

                    double[] angles = {0, Math.PI / 2, Math.PI, Math.PI * 3 / 2};
                    double angle = angles[(int)(Math.random() * 4)];
                    Location newLoc = baseLoc.clone().add(Math.cos(angle) * 2.0, 0, Math.sin(angle) * 2.0);

                    if (Math.abs(newLoc.getX() - center.getX()) > 13) continue;
                    if (Math.abs(newLoc.getZ() - center.getZ()) > 13) continue;

                    // Check not too close to existing
                    boolean tooClose = false;
                    for (Location existing : crystalLocs) {
                        if (existing.distanceSquared(newLoc) < 3.0) {
                            tooClose = true;
                            break;
                        }
                    }
                    if (tooClose) continue;

                    // Tip emerging first (small)
                    BlockDisplayHandle tip = displayBuilder.spawnBlock(newLoc.clone().add(0, 0.1, 0), Material.AMETHYST_CLUSTER);
                    tip.scale(0.2f, 0.5f, 0.2f).glow(60, 0, 120).interpolation(5, 0);
                    crystalTiles.add(tip);
                    crystalLocs.add(newLoc);
                    spawnedEntities.add(tip.entity());

                    DisplayBuilder.playSound(newLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.7f);
                    DisplayBuilder.cyanDust(newLoc, 8, 1.0);
                }
            }

            // Crystals grow taller over time
            if (ticksAlive % 20 == 0 && !shatterPhase) {
                int age = ticksAlive;
                for (int i = 0; i < crystalTiles.size(); i++) {
                    int tileAge = age - (i * 80 / Math.max(1, crystalTiles.size()));
                    float growProgress = Math.min(1.0f, tileAge / 200.0f);
                    float height = 0.3f + growProgress * 2.0f;
                    float width = 0.2f + growProgress * 0.6f;

                    BlockDisplay bd = (BlockDisplay) crystalTiles.get(i).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-width / 2, -0.5f, -width / 2),
                            new AxisAngle4f(ticksAlive * 0.02f + i, 0, 1, 0),
                            new Vector3f(width, height, width),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(10);
                }
            }

            // Shatter phase at tick 700+
            if (ticksAlive == 700 && !shatterPhase) {
                shatterPhase = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.5f);
                DisplayBuilder.cyanDust(center, 80, 15.0);

                // Crystal shatter explosion: 2 hearts per adjacent player per tile
                for (Location crystalLoc : crystalLocs) {
                    DisplayBuilder.cyanDust(crystalLoc, 20, 2.5);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(crystalLoc) <= 4) {
                            p.damage(4.0); // 2 hearts per shattering tile
                        }
                    }
                }
            }

            // Crystal shatter visuals — scatter outward
            if (shatterPhase && ticksAlive > 700 && ticksAlive % 5 == 0) {
                for (int i = 0; i < Math.min(3, crystalTiles.size()); i++) {
                    BlockDisplay bd = (BlockDisplay) crystalTiles.get(0).entity();
                    Location scatter = bd.getLocation().clone().add(
                            (Math.random() - 0.5) * 8,
                            Math.random() * 5,
                            (Math.random() - 0.5) * 8
                    );
                    bd.teleport(scatter);
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                    crystalTiles.remove(0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidCrystallization(plugin); }
    }

    // =========================================================================
    // 100. TOTAL CORRUPTION — all four corners spread inward simultaneously
    // =========================================================================
    public static class TotalCorruption extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> corruptionTiles = new ArrayList<>();
        private final Map<BlockDisplayHandle, Integer> tileSpawnTick = new HashMap<>();
        private int lastWaveTick = 0;
        private int waveNumber = 0;
        private static final int WAVE_INTERVAL = 20; // 1 second per wave
        private static final int MAX_WAVES = 12;

        public TotalCorruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("total_corruption", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0); // custom progressive damage
            config.setDamageRadius(0.0);
            config.setDurationTicks(1400); // 60 second max duration
            config.setCooldownTicks(Integer.MAX_VALUE); // once per fight
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3-second warning: all four corners flash simultaneously
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.4f);

            double[][] corners = {{-12, -12}, {12, -12}, {-12, 12}, {12, 12}};
            for (double[] corner : corners) {
                Location cornerLoc = center.clone().add(corner[0], 0, corner[1]);
                DisplayBuilder.crimsonDust(cornerLoc, 30, 3.0);
                DisplayBuilder.purpleDust(cornerLoc, 30, 3.0);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Advance wave after 3-second announcement (60 ticks)
            if (ticksAlive > 60 && ticksAlive - lastWaveTick >= WAVE_INTERVAL && waveNumber < MAX_WAVES) {
                lastWaveTick = ticksAlive;
                float inwardProgress = waveNumber / (float) MAX_WAVES; // 0 -> 1 (corners to center)
                waveNumber++;

                // Spawn 4 diagonal arcs from each corner, moving inward
                double[][] corners = {{-12, -12}, {12, -12}, {-12, 12}, {12, 12}};
                for (double[] corner : corners) {
                    // Interpolate from corner toward center
                    double cx = corner[0] * (1.0 - inwardProgress);
                    double cz = corner[1] * (1.0 - inwardProgress);

                    // Spread arc perpendicular to inward direction
                    double perpX = -corner[1] / 12.0;
                    double perpZ = corner[0] / 12.0;

                    for (int spread = -2; spread <= 2; spread++) {
                        Location tileLoc = center.clone().add(
                                cx + perpX * spread * 1.5,
                                0.015,
                                cz + perpZ * spread * 1.5
                        );

                        BlockDisplayHandle tile = displayBuilder.spawnBlock(tileLoc, Material.PURPLE_CONCRETE);
                        tile.scale(1.4f, 0.03f, 1.4f).glow(40, 0, 80).interpolation(4, 0);
                        corruptionTiles.add(tile);
                        tileSpawnTick.put(tile, ticksAlive);
                        spawnedEntities.add(tile.entity());
                    }
                }

                // Corner wave announcement
                if (waveNumber % 3 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.4f + waveNumber * 0.03f);
                }
                DisplayBuilder.crimsonDust(center, 8 + waveNumber * 3, 12.0 - waveNumber);
            }

            // Progressive damage: longer-corrupted tiles deal more damage
            if (ticksAlive % 20 == 0) {
                for (BlockDisplayHandle tile : corruptionTiles) {
                    Integer spawnT = tileSpawnTick.get(tile);
                    if (spawnT == null) continue;
                    int age = ticksAlive - spawnT;
                    float ageProgress = Math.min(1.0f, age / 600.0f); // maxes out at 30 seconds

                    // Visual intensity
                    int glowR = (int)(40 + ageProgress * 88); // 40 -> 128
                    int glowB = (int)(80 + ageProgress * 175); // 80 -> 255
                    BlockDisplay bd = (BlockDisplay) tile.entity();
                    bd.setGlowColorOverride(Color.fromRGB(glowR, 0, glowB));
                    bd.setGlowing(true);

                    // Pulsing scale
                    float pulse = 0.03f + ageProgress * 0.02f;
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.7f, -pulse, -0.7f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.4f, pulse * 2, 1.4f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(8);

                    // Damage: 1 heart at front, up to 3 hearts deep corruption
                    double damage = 2.0 + ageProgress * 4.0; // 2-6 per second
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(tile.entity().getLocation()) <= 2.25) {
                            p.damage(damage * 0.1); // spread over ticks
                        }
                    }
                }
            }

            // Ambient corruption visual throughout
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.purpleDust(center, 6, 12.0);
                if (ticksAlive % 60 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f);
                }
            }

            // Recession: tiles disappear once safe zone reaches 8x8
            if (waveNumber >= MAX_WAVES && ticksAlive % 40 == 0) {
                int removeCount = Math.min(8, corruptionTiles.size());
                for (int i = 0; i < removeCount; i++) {
                    if (!corruptionTiles.isEmpty()) {
                        BlockDisplay bd = (BlockDisplay) corruptionTiles.get(0).entity();
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.7f, -0.015f, -0.7f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.01f, 0.01f, 0.01f),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(15);
                        tileSpawnTick.remove(corruptionTiles.get(0));
                        corruptionTiles.remove(0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TotalCorruption(plugin); }
    }
}
