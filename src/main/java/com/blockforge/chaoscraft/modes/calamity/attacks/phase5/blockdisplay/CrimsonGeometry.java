package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4E Block Display -- GROUP 7: CRIMSON GEOMETRY (Structures #63-72)
 * Supreme Calamitas (Boss 5) arena: descending crown, crimson lattice walls,
 * brimstone compass arrows. Massive architectural elements defining the
 * arena boundaries and Calamitas's dominion.
 *
 * Palette: crimson(200,10,10), blackstone dark, red glass translucent,
 *          magma orange(255,100,0), nether brick brown.
 * NO status effects. Damage in HP (not hearts). AxisAngle4f only.
 * triggerImpactDamage() for proximity hits. spawnedEntities.add() always.
 */
public final class CrimsonGeometry {

    private CrimsonGeometry() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new DescendingCrown(plugin));
        registry.register(new CrownPhaseFlare(plugin));
        registry.register(new NorthCrimsonLatticeWall(plugin));
        registry.register(new SouthCrimsonLatticeWall(plugin));
        registry.register(new EastCrimsonLatticeWall(plugin));
        registry.register(new WestCrimsonLatticeWall(plugin));
        registry.register(new BrimstoneCompassNorth(plugin));
        registry.register(new BrimstoneCompassSouth(plugin));
        registry.register(new BrimstoneCompassEast(plugin));
        registry.register(new BrimstoneCompassWest(plugin));
    }

    // ================================================================
    // #63 -- THE DESCENDING CROWN
    // ~120 entity floating crown: outer BLACKSTONE wall ring (48 entities,
    // radius 15), inner RED_STAINED_GLASS ring (48, radius 11), 8 crown
    // spikes (24 entities), 16 CRYING_OBSIDIAN cap panes. Rotates at
    // 0.3 deg/tick. Slowly descends from Y+35 to Y+20 over fight duration.
    // DRAGON_BREATH from spike tips. Phase 4: rotation doubles to 0.6 deg/tick.
    // ================================================================
    public static class DescendingCrown extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerWall = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private final List<BlockDisplayHandle> capPanes = new ArrayList<>();

        public DescendingCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("descending_crown", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location crownCenter = center.clone().add(0, 35, 0);

            // Outer wall ring: 48 BLACKSTONE at radius 15
            for (int i = 0; i < 48; i++) {
                double angle = Math.toRadians(i * 7.5);
                double x = Math.cos(angle) * 15.0;
                double z = Math.sin(angle) * 15.0;
                Location loc = crownCenter.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.8f, 4.0f, 0.8f).glow(200, 10, 10).interpolation(3, 0);
                outerWall.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner decorative ring: 48 RED_STAINED_GLASS at radius 11
            for (int i = 0; i < 48; i++) {
                double angle = Math.toRadians(i * 7.5);
                double x = Math.cos(angle) * 11.0;
                double z = Math.sin(angle) * 11.0;
                Location loc = crownCenter.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_STAINED_GLASS);
                h.scale(0.3f, 3.0f, 0.3f).glow(200, 10, 10).interpolation(3, 0);
                innerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 crown spikes at 45-deg intervals, radius 13
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45.0);
                double x = Math.cos(angle) * 13.0;
                double z = Math.sin(angle) * 13.0;
                // 3 blocks stacked: base 0.6, mid 0.5, tip 0.4
                float[] scales = {0.6f, 0.5f, 0.4f};
                for (int j = 0; j < 3; j++) {
                    Location loc = crownCenter.clone().add(x, 4 + j, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(scales[j], 1.0f, scales[j]).glow(200, 10, 10).interpolation(3, 0);
                    spikes.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 16 CRYING_OBSIDIAN cap panes at Y+4, radius 12.5
            for (int i = 0; i < 16; i++) {
                double angle = Math.toRadians(i * 22.5);
                double x = Math.cos(angle) * 12.5;
                double z = Math.sin(angle) * 12.5;
                Location loc = crownCenter.clone().add(x, 4, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(1.8f, 0.2f, 1.8f).glow(180, 40, 255).interpolation(3, 0);
                capPanes.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(crownCenter, Sound.BLOCK_BEACON_AMBIENT, 0.4f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Descent: Y+35 to Y+20 over 14400 ticks (~0.00104 blocks/tick)
            double yOffset = 35.0 - (15.0 * ticksAlive / 14400.0);
            if (yOffset < 20.0) yOffset = 20.0;

            // Rotation: 0.3 deg/tick
            double rotAngle = Math.toRadians(ticksAlive * 0.3);

            // Update outer wall
            for (int i = 0; i < outerWall.size(); i++) {
                double baseAngle = Math.toRadians(i * 7.5) + rotAngle;
                double x = Math.cos(baseAngle) * 15.0;
                double z = Math.sin(baseAngle) * 15.0;
                Location loc = center.clone().add(x, yOffset, z);
                outerWall.get(i).entity().teleport(loc);
            }

            // Update inner ring
            for (int i = 0; i < innerRing.size(); i++) {
                double baseAngle = Math.toRadians(i * 7.5) + rotAngle;
                double x = Math.cos(baseAngle) * 11.0;
                double z = Math.sin(baseAngle) * 11.0;
                Location loc = center.clone().add(x, yOffset, z);
                innerRing.get(i).entity().teleport(loc);

                // Inner ring crimson DUST every 4 entities staggered
                if ((ticksAlive + i) % 4 == 0) {
                    w.spawnParticle(Particle.DUST, loc, 1, 0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(180, 10, 10), 1.0f));
                }
            }

            // Update spikes (3 per spike, 8 spikes)
            for (int i = 0; i < 8; i++) {
                double baseAngle = Math.toRadians(i * 45.0) + rotAngle;
                double x = Math.cos(baseAngle) * 13.0;
                double z = Math.sin(baseAngle) * 13.0;
                for (int j = 0; j < 3; j++) {
                    int idx = i * 3 + j;
                    Location loc = center.clone().add(x, yOffset + 4 + j, z);
                    spikes.get(idx).entity().teleport(loc);
                }
                // DRAGON_BREATH from spike tips
                Location tipLoc = center.clone().add(x, yOffset + 7, z);
                w.spawnParticle(Particle.DRAGON_BREATH, tipLoc, 1, 0.1, 0.1, 0.1, 0.04);
            }

            // Update cap panes
            for (int i = 0; i < capPanes.size(); i++) {
                double baseAngle = Math.toRadians(i * 22.5) + rotAngle;
                double x = Math.cos(baseAngle) * 12.5;
                double z = Math.sin(baseAngle) * 12.5;
                Location loc = center.clone().add(x, yOffset + 4, z);
                capPanes.get(i).entity().teleport(loc);
            }

            // Ambient sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, yOffset, 0),
                        Sound.BLOCK_BEACON_AMBIENT, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DescendingCrown(plugin); }
    }

    // ================================================================
    // #64 -- CROWN PHASE FLARE
    // Triggered on phase transitions. All 8 spike tips emit synchronized
    // CRIT + crimson DUST bursts (15 each). Crown rotation reverses for
    // 40 ticks. FIREWORKS_SPARK trails for 30 ticks post-transition.
    // ================================================================
    public static class CrownPhaseFlare extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> flareMarkers = new ArrayList<>();

        public CrownPhaseFlare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crown_phase_flare", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(10.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Place 8 flare marker blocks at spike tip positions
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45.0);
                double x = Math.cos(angle) * 13.0;
                double z = Math.sin(angle) * 13.0;
                Location tipLoc = center.clone().add(x, 41, z); // Y+35 + 6 (spike top)

                BlockDisplayHandle marker = displayBuilder.spawnBlock(tipLoc, Material.CRYING_OBSIDIAN);
                marker.scale(0.4f, 0.4f, 0.4f).glow(255, 20, 0).interpolation(3, 0);
                flareMarkers.add(marker);
                spawnedEntities.add(marker.entity());

                // Immediate burst: 15 CRIT + 15 crimson DUST per spike tip
                w.spawnParticle(Particle.CRIT, tipLoc, 15, 1.0, 1.0, 1.0, 0.1);
                w.spawnParticle(Particle.DUST, tipLoc, 15, 1.0, 1.0, 1.0, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 20, 0), 2.0f));
            }

            DisplayBuilder.playSound(center.clone().add(0, 35, 0),
                    Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // FIREWORKS_SPARK trail for 30 ticks
            if (ticksAlive < 30) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45.0);
                    double x = Math.cos(angle) * 13.0;
                    double z = Math.sin(angle) * 13.0;
                    Location tipLoc = center.clone().add(x, 41, z);
                    w.spawnParticle(Particle.FIREWORK, tipLoc, 3, 0.5, 0.5, 0.5, 0.05);
                }
            }

            // Shrink and fade markers over duration
            float markerScale = Math.max(0.05f, 0.4f * (1.0f - ticksAlive / 80.0f));
            for (BlockDisplayHandle marker : flareMarkers) {
                marker.scale(markerScale, markerScale, markerScale);
                marker.interpolation(3, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrownPhaseFlare(plugin); }
    }

    // ================================================================
    // #65 -- NORTH CRIMSON LATTICE WALL
    // 15x15 grid of RED_STAINED_GLASS ItemDisplays (225 entities) at
    // arena north edge (Z-30). Each pane scaled 1.0x1.0x0.05. Checkerboard
    // bright/dim alternation. Diagonal wave materialization from bottom-left
    // to top-right. Crimson DUST emission from bright panes.
    // Phase 4: strobing lattice inversion every 20 ticks.
    // ================================================================
    public static class NorthCrimsonLatticeWall extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> panes = new ArrayList<>();
        private static final int GRID_SIZE = 15;

        public NorthCrimsonLatticeWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("north_crimson_lattice_wall", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location wallBase = center.clone().add(-7, 0, -30);

            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    Location loc = wallBase.clone().add(col, row, 0);
                    ItemDisplayHandle h = displayBuilder.spawnItem(loc,
                            new ItemStack(Material.RED_STAINED_GLASS));
                    h.scale(1.0f, 1.0f, 0.05f).interpolation(3, 0);

                    // Checkerboard bright/dim
                    boolean bright = (row + col) % 2 == 0;
                    if (bright) {
                        h.glow(200, 20, 0);
                    } else {
                        h.glow(80, 8, 0);
                    }

                    panes.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center.clone().add(0, 7, -30),
                    Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Crimson DUST from bright panes (staggered)
            for (int i = 0; i < panes.size(); i++) {
                int row = i / GRID_SIZE;
                int col = i % GRID_SIZE;
                boolean bright = (row + col) % 2 == 0;

                if (bright && (ticksAlive + i) % 3 == 0) {
                    Location paneLoc = panes.get(i).entity().getLocation();
                    w.spawnParticle(Particle.DUST, paneLoc.clone().add(0, 0, 0.1), 1,
                            0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(200, 20, 0), 0.8f));
                }
            }

            // Ambient sound on materialization complete
            if (ticksAlive == 60) {
                DisplayBuilder.playSound(center.clone().add(0, 7, -30),
                        Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NorthCrimsonLatticeWall(plugin); }
    }

    // ================================================================
    // #66 -- SOUTH CRIMSON LATTICE WALL
    // Mirror of #65, positioned at Z+30 facing north. Materialization
    // sweeps from bottom-right to top-left. Phase 4 strobe synced with
    // north wall.
    // ================================================================
    public static class SouthCrimsonLatticeWall extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> panes = new ArrayList<>();
        private static final int GRID_SIZE = 15;

        public SouthCrimsonLatticeWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("south_crimson_lattice_wall", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location wallBase = center.clone().add(-7, 0, 30);

            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    Location loc = wallBase.clone().add(col, row, 0);
                    ItemDisplayHandle h = displayBuilder.spawnItem(loc,
                            new ItemStack(Material.RED_STAINED_GLASS));
                    h.scale(1.0f, 1.0f, 0.05f).interpolation(3, 0);

                    boolean bright = (row + col) % 2 == 0;
                    if (bright) {
                        h.glow(200, 20, 0);
                    } else {
                        h.glow(80, 8, 0);
                    }

                    panes.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center.clone().add(0, 7, 30),
                    Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Crimson DUST from bright panes -- velocity pushes toward center (Z-)
            for (int i = 0; i < panes.size(); i++) {
                int row = i / GRID_SIZE;
                int col = i % GRID_SIZE;
                boolean bright = (row + col) % 2 == 0;

                if (bright && (ticksAlive + i) % 3 == 0) {
                    Location paneLoc = panes.get(i).entity().getLocation();
                    w.spawnParticle(Particle.DUST, paneLoc.clone().add(0, 0, -0.1), 1,
                            0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(200, 20, 0), 0.8f));
                }
            }

            if (ticksAlive == 60) {
                DisplayBuilder.playSound(center.clone().add(0, 7, 30),
                        Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SouthCrimsonLatticeWall(plugin); }
    }

    // ================================================================
    // #67 -- EAST CRIMSON LATTICE WALL
    // Same construction as #65 but at X+30. Faces west. Materializes
    // 10 ticks after north/south walls. Particle velocity toward center
    // (-X direction).
    // ================================================================
    public static class EastCrimsonLatticeWall extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> panes = new ArrayList<>();
        private static final int GRID_SIZE = 15;

        public EastCrimsonLatticeWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("east_crimson_lattice_wall", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location wallBase = center.clone().add(30, 0, -7);

            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    Location loc = wallBase.clone().add(0, row, col);
                    ItemDisplayHandle h = displayBuilder.spawnItem(loc,
                            new ItemStack(Material.RED_STAINED_GLASS));
                    h.scale(0.05f, 1.0f, 1.0f).interpolation(3, 0);

                    boolean bright = (row + col) % 2 == 0;
                    if (bright) {
                        h.glow(200, 20, 0);
                    } else {
                        h.glow(80, 8, 0);
                    }

                    panes.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center.clone().add(30, 7, 0),
                    Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < panes.size(); i++) {
                int row = i / GRID_SIZE;
                int col = i % GRID_SIZE;
                boolean bright = (row + col) % 2 == 0;

                if (bright && (ticksAlive + i) % 3 == 0) {
                    Location paneLoc = panes.get(i).entity().getLocation();
                    w.spawnParticle(Particle.DUST, paneLoc.clone().add(-0.1, 0, 0), 1,
                            0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(200, 20, 0), 0.8f));
                }
            }

            if (ticksAlive == 60) {
                DisplayBuilder.playSound(center.clone().add(30, 7, 0),
                        Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EastCrimsonLatticeWall(plugin); }
    }

    // ================================================================
    // #68 -- WEST CRIMSON LATTICE WALL
    // Mirror of #67 at X-30. Faces east. Particle velocity toward
    // center (+X direction). Materialization diagonal mirrored from east.
    // ================================================================
    public static class WestCrimsonLatticeWall extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> panes = new ArrayList<>();
        private static final int GRID_SIZE = 15;

        public WestCrimsonLatticeWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("west_crimson_lattice_wall", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location wallBase = center.clone().add(-30, 0, -7);

            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    Location loc = wallBase.clone().add(0, row, col);
                    ItemDisplayHandle h = displayBuilder.spawnItem(loc,
                            new ItemStack(Material.RED_STAINED_GLASS));
                    h.scale(0.05f, 1.0f, 1.0f).interpolation(3, 0);

                    boolean bright = (row + col) % 2 == 0;
                    if (bright) {
                        h.glow(200, 20, 0);
                    } else {
                        h.glow(80, 8, 0);
                    }

                    panes.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center.clone().add(-30, 7, 0),
                    Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < panes.size(); i++) {
                int row = i / GRID_SIZE;
                int col = i % GRID_SIZE;
                boolean bright = (row + col) % 2 == 0;

                if (bright && (ticksAlive + i) % 3 == 0) {
                    Location paneLoc = panes.get(i).entity().getLocation();
                    w.spawnParticle(Particle.DUST, paneLoc.clone().add(0.1, 0, 0), 1,
                            0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(200, 20, 0), 0.8f));
                }
            }

            if (ticksAlive == 60) {
                DisplayBuilder.playSound(center.clone().add(-30, 7, 0),
                        Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WestCrimsonLatticeWall(plugin); }
    }

    // ================================================================
    // #69 -- BRIMSTONE COMPASS: NORTH ARROW
    // ~18 entities: 8 NETHER_BRICKS shaft + 5 MAGMA_BLOCK arrowhead +
    // 4 NETHER_BRICK_FENCE fletchings. Floating at Y+2, pointing due
    // north (away from center at Z-18). Vertical oscillation period 100
    // ticks, amplitude +-0.3. LAVA particles from magma arrowhead.
    // Phase 4: tracks Calamitas at 1.5 deg/tick.
    // ================================================================
    public static class BrimstoneCompassNorth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> arrowheadBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fletchingBlocks = new ArrayList<>();

        public BrimstoneCompassNorth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_compass_north", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location arrowBase = center.clone().add(0, 2, -18);

            // Arrow shaft: 8 NETHER_BRICKS along Z axis pointing north
            for (int i = 0; i < 8; i++) {
                Location loc = arrowBase.clone().add(0, 0, i * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                h.scale(1.2f, 1.2f, 1.2f).glow(200, 10, 10).interpolation(3, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Arrowhead: 5 MAGMA_BLOCK forming wedge tip at north end
            float[] xOffsets = {0, -0.6f, 0.6f, -1.2f, 1.2f};
            float[] scales = {1.6f, 1.2f, 1.2f, 0.8f, 0.8f};
            for (int i = 0; i < 5; i++) {
                Location loc = arrowBase.clone().add(xOffsets[i], 0, -1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(scales[i], scales[i], 0.6f).glow(255, 100, 0).interpolation(3, 0);
                arrowheadBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fletching: 4 NETHER_BRICK_FENCE at tail end
            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 2; j++) {
                    Location loc = arrowBase.clone().add(side * 0.5, 0, 8.4 + j * 0.6);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICK_FENCE);
                    h.scale(0.4f, 1.4f, 0.4f).glow(200, 10, 10).interpolation(3, 0);
                    fletchingBlocks.add(h);
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

            // Vertical oscillation: period 100 ticks, amplitude +-0.3
            double yOsc = 0.3 * Math.sin(2.0 * Math.PI * ticksAlive / 100.0);

            // Teleport all entities with Y offset
            Location arrowBase = center.clone().add(0, 2 + yOsc, -18);

            for (int i = 0; i < shaftBlocks.size(); i++) {
                Location loc = arrowBase.clone().add(0, 0, i * 1.2);
                shaftBlocks.get(i).entity().teleport(loc);
            }

            float[] xOffsets = {0, -0.6f, 0.6f, -1.2f, 1.2f};
            for (int i = 0; i < arrowheadBlocks.size(); i++) {
                Location loc = arrowBase.clone().add(xOffsets[i], 0, -1.5);
                arrowheadBlocks.get(i).entity().teleport(loc);
            }

            for (int i = 0; i < fletchingBlocks.size(); i++) {
                int side = (i < 2) ? -1 : 1;
                int j = i % 2;
                Location loc = arrowBase.clone().add(side * 0.5, 0, 8.4 + j * 0.6);
                fletchingBlocks.get(i).entity().teleport(loc);
            }

            // LAVA particles from magma arrowhead every 8 ticks
            if (ticksAlive % 8 == 0) {
                Location tipLoc = arrowBase.clone().add(0, 0.3, -1.5);
                w.spawnParticle(Particle.LAVA, tipLoc, 1, 0.2, 0.2, 0.2, 0);
            }

            // Sound once per oscillation cycle
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(arrowBase, Sound.BLOCK_NETHER_BRICKS_STEP, 0.3f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCompassNorth(plugin); }
    }

    // ================================================================
    // #70 -- BRIMSTONE COMPASS: SOUTH ARROW
    // Same construction as #69, pointing due south at Z+18. Oscillation
    // period 110 ticks (offset from north). LAVA particles from tip.
    // ================================================================
    public static class BrimstoneCompassSouth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> arrowheadBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fletchingBlocks = new ArrayList<>();

        public BrimstoneCompassSouth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_compass_south", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location arrowBase = center.clone().add(0, 2, 18);

            // Shaft pointing south (-Z toward center, +Z away)
            for (int i = 0; i < 8; i++) {
                Location loc = arrowBase.clone().add(0, 0, -i * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                h.scale(1.2f, 1.2f, 1.2f).glow(200, 10, 10).interpolation(3, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Arrowhead pointing south (+Z)
            float[] xOffsets = {0, -0.6f, 0.6f, -1.2f, 1.2f};
            float[] scales = {1.6f, 1.2f, 1.2f, 0.8f, 0.8f};
            for (int i = 0; i < 5; i++) {
                Location loc = arrowBase.clone().add(xOffsets[i], 0, 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(scales[i], scales[i], 0.6f).glow(255, 100, 0).interpolation(3, 0);
                arrowheadBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fletching at tail
            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 2; j++) {
                    Location loc = arrowBase.clone().add(side * 0.5, 0, -8.4 - j * 0.6);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICK_FENCE);
                    h.scale(0.4f, 1.4f, 0.4f).glow(200, 10, 10).interpolation(3, 0);
                    fletchingBlocks.add(h);
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

            // Vertical oscillation: period 110 ticks
            double yOsc = 0.3 * Math.sin(2.0 * Math.PI * ticksAlive / 110.0);
            Location arrowBase = center.clone().add(0, 2 + yOsc, 18);

            for (int i = 0; i < shaftBlocks.size(); i++) {
                Location loc = arrowBase.clone().add(0, 0, -i * 1.2);
                shaftBlocks.get(i).entity().teleport(loc);
            }
            float[] xOffsets = {0, -0.6f, 0.6f, -1.2f, 1.2f};
            for (int i = 0; i < arrowheadBlocks.size(); i++) {
                Location loc = arrowBase.clone().add(xOffsets[i], 0, 1.5);
                arrowheadBlocks.get(i).entity().teleport(loc);
            }
            for (int i = 0; i < fletchingBlocks.size(); i++) {
                int side = (i < 2) ? -1 : 1;
                int j = i % 2;
                Location loc = arrowBase.clone().add(side * 0.5, 0, -8.4 - j * 0.6);
                fletchingBlocks.get(i).entity().teleport(loc);
            }

            if (ticksAlive % 8 == 0) {
                Location tipLoc = arrowBase.clone().add(0, 0.3, 1.5);
                w.spawnParticle(Particle.LAVA, tipLoc, 1, 0.2, 0.2, 0.2, 0);
            }

            if (ticksAlive % 110 == 0) {
                DisplayBuilder.playSound(arrowBase, Sound.BLOCK_NETHER_BRICKS_STEP, 0.3f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCompassSouth(plugin); }
    }

    // ================================================================
    // #71 -- BRIMSTONE COMPASS: EAST ARROW
    // Same construction rotated 90 deg, pointing due east at X+18.
    // Shaft runs along X axis. Oscillation period 95 ticks.
    // ================================================================
    public static class BrimstoneCompassEast extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> arrowheadBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fletchingBlocks = new ArrayList<>();

        public BrimstoneCompassEast(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_compass_east", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location arrowBase = center.clone().add(18, 2, 0);

            // Shaft along X axis (pointing east, +X away from center)
            for (int i = 0; i < 8; i++) {
                Location loc = arrowBase.clone().add(-i * 1.2, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                h.scale(1.2f, 1.2f, 1.2f).glow(200, 10, 10).interpolation(3, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Arrowhead pointing east (+X)
            float[] zOffsets = {0, -0.6f, 0.6f, -1.2f, 1.2f};
            float[] scales = {1.6f, 1.2f, 1.2f, 0.8f, 0.8f};
            for (int i = 0; i < 5; i++) {
                Location loc = arrowBase.clone().add(1.5, 0, zOffsets[i]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.6f, scales[i], scales[i]).glow(255, 100, 0).interpolation(3, 0);
                arrowheadBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fletching at tail (-X end)
            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 2; j++) {
                    Location loc = arrowBase.clone().add(-8.4 - j * 0.6, 0, side * 0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICK_FENCE);
                    h.scale(0.4f, 1.4f, 0.4f).glow(200, 10, 10).interpolation(3, 0);
                    fletchingBlocks.add(h);
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

            double yOsc = 0.3 * Math.sin(2.0 * Math.PI * ticksAlive / 95.0);
            Location arrowBase = center.clone().add(18, 2 + yOsc, 0);

            for (int i = 0; i < shaftBlocks.size(); i++) {
                Location loc = arrowBase.clone().add(-i * 1.2, 0, 0);
                shaftBlocks.get(i).entity().teleport(loc);
            }
            float[] zOffsets = {0, -0.6f, 0.6f, -1.2f, 1.2f};
            for (int i = 0; i < arrowheadBlocks.size(); i++) {
                Location loc = arrowBase.clone().add(1.5, 0, zOffsets[i]);
                arrowheadBlocks.get(i).entity().teleport(loc);
            }
            for (int i = 0; i < fletchingBlocks.size(); i++) {
                int side = (i < 2) ? -1 : 1;
                int j = i % 2;
                Location loc = arrowBase.clone().add(-8.4 - j * 0.6, 0, side * 0.5);
                fletchingBlocks.get(i).entity().teleport(loc);
            }

            if (ticksAlive % 8 == 0) {
                Location tipLoc = arrowBase.clone().add(1.5, 0.3, 0);
                w.spawnParticle(Particle.LAVA, tipLoc, 1, 0.2, 0.2, 0.2, 0);
            }

            if (ticksAlive % 95 == 0) {
                DisplayBuilder.playSound(arrowBase, Sound.BLOCK_NETHER_BRICKS_STEP, 0.3f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCompassEast(plugin); }
    }

    // ================================================================
    // #72 -- BRIMSTONE COMPASS: WEST ARROW
    // Same construction rotated, pointing due west at X-18. Shaft runs
    // along X axis, tip in -X direction. Oscillation period 105 ticks.
    // All four compass arrows form a + shape at arena center level.
    // ================================================================
    public static class BrimstoneCompassWest extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> arrowheadBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fletchingBlocks = new ArrayList<>();

        public BrimstoneCompassWest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_compass_west", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location arrowBase = center.clone().add(-18, 2, 0);

            // Shaft pointing west (tip in -X direction)
            for (int i = 0; i < 8; i++) {
                Location loc = arrowBase.clone().add(i * 1.2, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICKS);
                h.scale(1.2f, 1.2f, 1.2f).glow(200, 10, 10).interpolation(3, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Arrowhead pointing west (-X)
            float[] zOffsets = {0, -0.6f, 0.6f, -1.2f, 1.2f};
            float[] scales = {1.6f, 1.2f, 1.2f, 0.8f, 0.8f};
            for (int i = 0; i < 5; i++) {
                Location loc = arrowBase.clone().add(-1.5, 0, zOffsets[i]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.6f, scales[i], scales[i]).glow(255, 100, 0).interpolation(3, 0);
                arrowheadBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fletching at tail (+X end)
            for (int side = -1; side <= 1; side += 2) {
                for (int j = 0; j < 2; j++) {
                    Location loc = arrowBase.clone().add(8.4 + j * 0.6, 0, side * 0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_BRICK_FENCE);
                    h.scale(0.4f, 1.4f, 0.4f).glow(200, 10, 10).interpolation(3, 0);
                    fletchingBlocks.add(h);
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

            double yOsc = 0.3 * Math.sin(2.0 * Math.PI * ticksAlive / 105.0);
            Location arrowBase = center.clone().add(-18, 2 + yOsc, 0);

            for (int i = 0; i < shaftBlocks.size(); i++) {
                Location loc = arrowBase.clone().add(i * 1.2, 0, 0);
                shaftBlocks.get(i).entity().teleport(loc);
            }
            float[] zOffsets = {0, -0.6f, 0.6f, -1.2f, 1.2f};
            for (int i = 0; i < arrowheadBlocks.size(); i++) {
                Location loc = arrowBase.clone().add(-1.5, 0, zOffsets[i]);
                arrowheadBlocks.get(i).entity().teleport(loc);
            }
            for (int i = 0; i < fletchingBlocks.size(); i++) {
                int side = (i < 2) ? -1 : 1;
                int j = i % 2;
                Location loc = arrowBase.clone().add(8.4 + j * 0.6, 0, side * 0.5);
                fletchingBlocks.get(i).entity().teleport(loc);
            }

            if (ticksAlive % 8 == 0) {
                Location tipLoc = arrowBase.clone().add(-1.5, 0.3, 0);
                w.spawnParticle(Particle.LAVA, tipLoc, 1, 0.2, 0.2, 0.2, 0);
            }

            if (ticksAlive % 105 == 0) {
                DisplayBuilder.playSound(arrowBase, Sound.BLOCK_NETHER_BRICKS_STEP, 0.3f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCompassWest(plugin); }
    }
}
