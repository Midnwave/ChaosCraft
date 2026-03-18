package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.blockdisplay;

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
 * Phase 4D Block Display -- GROUP 5: DRAGON'S WING SHADOW STRUCTURES (#39-50)
 * 12 attacks forming shadow projections and spectral wing ghost arrays --
 * the visual echo of the Void Emperor's presence across every surface.
 *
 * Structures:
 *  #39 PrimaryWingShadowNorth      -- North ground shadow plane, SPELL_WITCH
 *  #40 PrimaryWingShadowSouth      -- South ground shadow plane, DRAGON_BREATH
 *  #41 SpectralWingGhostLayer1     -- 60% opacity trailing ghost, 2-tick lag
 *  #42 SpectralWingGhostLayer2     -- 35% opacity trailing ghost, 4-tick lag
 *  #43 SpectralWingGhostLayer3     -- 15% opacity trailing ghost, 6-tick lag
 *  #44 WingWallShadowEast          -- East wall vertical shadow projection
 *  #45 WingWallShadowWest          -- West wall vertical shadow projection
 *  #46 CrownGhostArray             -- Overhead wing tip ghost strip at Y+18-22
 *  #47 VoidMembraneFloorPanel      -- Landing zone pulse plane at Y+0.2
 *  #48 WingOrbitRing               -- Phase 3 permanent shadow ring at Y+25
 *  #49 TrailingWingtipLines        -- Phase 2 thin obsidian trail needles
 *  #50 FinalSanctumConvergence     -- Phase 3 convergence assembly, open/close cycle
 *
 * Rules applied:
 * - NO status effects
 * - Void Emperor palette: magenta(180,0,200), cyan(0,200,255), purple(128,0,255), crimson(200,0,50)
 * - AxisAngle4f ONLY, static DisplayBuilder methods
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Damage HP 4.0-12.0
 */
public final class DragonWingShadows {

    private DragonWingShadows() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PrimaryWingShadowNorth(plugin));
        registry.register(new PrimaryWingShadowSouth(plugin));
        registry.register(new SpectralWingGhostLayer1(plugin));
        registry.register(new SpectralWingGhostLayer2(plugin));
        registry.register(new SpectralWingGhostLayer3(plugin));
        registry.register(new WingWallShadowEast(plugin));
        registry.register(new WingWallShadowWest(plugin));
        registry.register(new CrownGhostArray(plugin));
        registry.register(new VoidMembraneFloorPanel(plugin));
        registry.register(new WingOrbitRing(plugin));
        registry.register(new TrailingWingtipLines(plugin));
        registry.register(new FinalSanctumConvergence(plugin));
    }

    // ================================================================
    // #39 -- PRIMARY WING SHADOW PLANE (North Ground)
    // Large thin dark plane at Y+0.1 representing the Dragon's left
    // wing shadow. 4x6 grid of dark prismarine tiles, SPELL_WITCH
    // particles consuming ambient light.
    // ================================================================
    public static class PrimaryWingShadowNorth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shadowTiles = new ArrayList<>();
        private static final int COLS = 4;
        private static final int ROWS = 6;

        public PrimaryWingShadowNorth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wing_shadow_north", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Shadow plane: north zone X-16 to X+16, Z-8 to Z-22, Y+0.1
            Location planeOrigin = center.clone().add(-16, 0.1, -22);
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    Location loc = planeOrigin.clone().add(c * 8.0, 0, r * 2.4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                    h.scale(8.0f, 0.05f, 8.0f).glow(128, 0, 255).interpolation(4, 0);
                    shadowTiles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // SPELL_WITCH particles drifting upward from tiles, consuming light
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < Math.min(shadowTiles.size(), 8); i++) {
                    int idx = (ticksAlive / 3 + i) % shadowTiles.size();
                    Location tileLoc = shadowTiles.get(idx).entity().getLocation().clone().add(4, 0.2, 4);
                    center.getWorld().spawnParticle(Particle.WITCH, tileLoc, 2, 2.0, 0.1, 2.0, 0.01);
                }
            }

            // Subtle purple ambient
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 0.5, -15), 4, 4.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrimaryWingShadowNorth(plugin); }
    }

    // ================================================================
    // #40 -- PRIMARY WING SHADOW PLANE (South Ground)
    // Right wing shadow. Mirror of north at Z+8 to Z+22.
    // DRAGON_BREATH particles instead of SPELL_WITCH.
    // ================================================================
    public static class PrimaryWingShadowSouth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shadowTiles = new ArrayList<>();
        private static final int COLS = 4;
        private static final int ROWS = 6;

        public PrimaryWingShadowSouth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wing_shadow_south", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location planeOrigin = center.clone().add(-16, 0.1, 8);
            for (int c = 0; c < COLS; c++) {
                for (int r = 0; r < ROWS; r++) {
                    Location loc = planeOrigin.clone().add(c * 8.0, 0, r * 2.4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                    h.scale(8.0f, 0.05f, 8.0f).glow(128, 0, 255).interpolation(4, 0);
                    shadowTiles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // DRAGON_BREATH particles (south distinction)
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < Math.min(shadowTiles.size(), 8); i++) {
                    int idx = (ticksAlive / 3 + i) % shadowTiles.size();
                    Location tileLoc = shadowTiles.get(idx).entity().getLocation().clone().add(4, 0.2, 4);
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, tileLoc, 2,
                            2.0, 0.1, 2.0, 0);
                }
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 0.5, 15), 4, 4.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrimaryWingShadowSouth(plugin); }
    }

    // ================================================================
    // #41 -- SPECTRAL WING GHOST LAYER 1 (60% Opacity)
    // 20 dark prismarine tiles in bat-wing silhouette shape.
    // Trailing 2 ticks behind simulated position. DRAGON_BREATH perimeter.
    // ================================================================
    public static class SpectralWingGhostLayer1 extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wingTiles = new ArrayList<>();
        private static final int TILE_COUNT = 20;

        public SpectralWingGhostLayer1(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_ghost_layer1", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Wing silhouette shape: approximate bat-wing with 20 tiles
            // Arranged in a roughly V pattern centered at Y+10
            Location wingCenter = center.clone().add(0, 10, 0);
            for (int i = 0; i < TILE_COUNT; i++) {
                // Spread tiles in a wing arc
                double t = (double) i / (TILE_COUNT - 1);
                double x = (t - 0.5) * 20; // -10 to +10
                double z = -Math.abs(x) * 0.3; // V-shape depth
                double y = Math.abs(x) * 0.15; // slight upward sweep at tips
                Location loc = wingCenter.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(4.0f, 0.1f, 4.0f).glow(128, 0, 255).interpolation(3, 0);
                wingTiles.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(wingCenter, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.35f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Simulate trailing position: orbital path with 2-tick lag
            double orbitAngle = Math.toRadians((ticksAlive - 2) * 0.5); // slow orbit
            double offsetX = Math.cos(orbitAngle) * 5;
            double offsetZ = Math.sin(orbitAngle) * 5;
            Location trailCenter = center.clone().add(offsetX, 10, offsetZ);

            // Update tile positions relative to trail center
            for (int i = 0; i < wingTiles.size(); i++) {
                double t = (double) i / (TILE_COUNT - 1);
                double x = (t - 0.5) * 20;
                double z = -Math.abs(x) * 0.3;
                double y = Math.abs(x) * 0.15;
                Location loc = trailCenter.clone().add(x, y, z);
                wingTiles.get(i).entity().teleport(loc);
            }

            // DRAGON_BREATH perimeter particles (60% density = 15/sec)
            if (ticksAlive % 2 == 0) {
                // Outer edge tiles (first 4, last 4)
                for (int i = 0; i < 4; i++) {
                    Location tipLoc = wingTiles.get(i).entity().getLocation().clone().add(2, 0.1, 2);
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, tipLoc, 4,
                            1.5, 0.1, 1.5, 0);
                }
                for (int i = TILE_COUNT - 4; i < TILE_COUNT; i++) {
                    Location tipLoc = wingTiles.get(i).entity().getLocation().clone().add(2, 0.1, 2);
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, tipLoc, 4,
                            1.5, 0.1, 1.5, 0);
                }
            }

            // Flap sound on wing cycle
            if (ticksAlive % 100 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(trailCenter, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.35f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpectralWingGhostLayer1(plugin); }
    }

    // ================================================================
    // #42 -- SPECTRAL WING GHOST LAYER 2 (35% Opacity)
    // Same construction, 4-tick lag, reduced particle density.
    // SPELL_WITCH perimeter for faint magenta tinge.
    // ================================================================
    public static class SpectralWingGhostLayer2 extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wingTiles = new ArrayList<>();
        private static final int TILE_COUNT = 20;

        public SpectralWingGhostLayer2(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_ghost_layer2", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location wingCenter = center.clone().add(0, 10, 0);
            for (int i = 0; i < TILE_COUNT; i++) {
                double t = (double) i / (TILE_COUNT - 1);
                double x = (t - 0.5) * 20;
                double z = -Math.abs(x) * 0.3;
                double y = Math.abs(x) * 0.15;
                Location loc = wingCenter.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(4.0f, 0.1f, 4.0f).glow(180, 0, 200).interpolation(3, 0);
                wingTiles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 4-tick lag trailing position
            double orbitAngle = Math.toRadians((ticksAlive - 4) * 0.5);
            double offsetX = Math.cos(orbitAngle) * 5;
            double offsetZ = Math.sin(orbitAngle) * 5;
            Location trailCenter = center.clone().add(offsetX, 10, offsetZ);

            for (int i = 0; i < wingTiles.size(); i++) {
                double t = (double) i / (TILE_COUNT - 1);
                double x = (t - 0.5) * 20;
                double z = -Math.abs(x) * 0.3;
                double y = Math.abs(x) * 0.15;
                Location loc = trailCenter.clone().add(x, y, z);
                wingTiles.get(i).entity().teleport(loc);
            }

            // SPELL_WITCH perimeter (35% density = ~9/sec)
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 3; i++) {
                    Location tipLoc = wingTiles.get(i).entity().getLocation().clone().add(2, 0.1, 2);
                    center.getWorld().spawnParticle(Particle.WITCH, tipLoc, 3,
                            1.5, 0.1, 1.5, 0.01);
                }
                for (int i = TILE_COUNT - 3; i < TILE_COUNT; i++) {
                    Location tipLoc = wingTiles.get(i).entity().getLocation().clone().add(2, 0.1, 2);
                    center.getWorld().spawnParticle(Particle.WITCH, tipLoc, 3,
                            1.5, 0.1, 1.5, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpectralWingGhostLayer2(plugin); }
    }

    // ================================================================
    // #43 -- SPECTRAL WING GHOST LAYER 3 (15% Opacity)
    // Most faded ghost, 6-tick lag, very low particles. Slightly
    // smaller scale (3.5) suggesting collapse as it dissipates.
    // ================================================================
    public static class SpectralWingGhostLayer3 extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wingTiles = new ArrayList<>();
        private static final int TILE_COUNT = 20;

        public SpectralWingGhostLayer3(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_ghost_layer3", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location wingCenter = center.clone().add(0, 10, 0);
            for (int i = 0; i < TILE_COUNT; i++) {
                double t = (double) i / (TILE_COUNT - 1);
                double x = (t - 0.5) * 20;
                double z = -Math.abs(x) * 0.3;
                double y = Math.abs(x) * 0.15;
                Location loc = wingCenter.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                // Slightly smaller than layers 1/2
                h.scale(3.5f, 0.1f, 3.5f).glow(180, 0, 200).interpolation(3, 0);
                wingTiles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 6-tick lag
            double orbitAngle = Math.toRadians((ticksAlive - 6) * 0.5);
            double offsetX = Math.cos(orbitAngle) * 5;
            double offsetZ = Math.sin(orbitAngle) * 5;
            Location trailCenter = center.clone().add(offsetX, 10, offsetZ);

            for (int i = 0; i < wingTiles.size(); i++) {
                double t = (double) i / (TILE_COUNT - 1);
                double x = (t - 0.5) * 20;
                double z = -Math.abs(x) * 0.3;
                double y = Math.abs(x) * 0.15;
                Location loc = trailCenter.clone().add(x, y, z);
                wingTiles.get(i).entity().teleport(loc);
            }

            // Very sparse particles (15% density = ~3/sec), perimeter only
            if (ticksAlive % 7 == 0) {
                int idx = ticksAlive % TILE_COUNT;
                if (idx < 2 || idx >= TILE_COUNT - 2) {
                    Location tipLoc = wingTiles.get(idx).entity().getLocation().clone().add(1.5, 0.1, 1.5);
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, tipLoc, 2,
                            1.0, 0.1, 1.0, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpectralWingGhostLayer3(plugin); }
    }

    // ================================================================
    // #44 -- WING WALL SHADOW (East Arena Wall)
    // 12 dark prismarine tiles in vertical wing silhouette on X+22 wall.
    // DRAGON_BREATH perimeter. Active when Dragon is in west half.
    // ================================================================
    public static class WingWallShadowEast extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shadowTiles = new ArrayList<>();

        public WingWallShadowEast(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wing_wall_shadow_east", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location wallCenter = center.clone().add(22, 8, 0);

            // 12 tiles in vertical wing silhouette
            for (int i = 0; i < 12; i++) {
                double t = (double) i / 11;
                double z = (t - 0.5) * 16; // Z-8 to Z+8
                double y = -Math.abs(z) * 0.5; // V-shape
                Location loc = wallCenter.clone().add(0, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.05f, 4.0f, 4.0f).glow(128, 0, 255).interpolation(4, 0);
                shadowTiles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Shadow breathing: X scale pulses +/-0.02
            float xPulse = 0.05f + 0.02f * (float) Math.sin(ticksAlive * Math.PI / 20);
            for (BlockDisplayHandle tile : shadowTiles) {
                tile.scale(xPulse, 4.0f, 4.0f);
                tile.interpolation(4, 0);
            }

            // DRAGON_BREATH from perimeter
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < shadowTiles.size(); i += 3) {
                    Location tileLoc = shadowTiles.get(i).entity().getLocation();
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, tileLoc, 3,
                            0.1, 1.5, 1.5, 0);
                }
            }

            // Ambient dragon sound every 400 ticks (20 sec)
            if (ticksAlive % 400 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center.clone().add(22, 8, 0),
                        Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.1f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WingWallShadowEast(plugin); }
    }

    // ================================================================
    // #45 -- WING WALL SHADOW (West Arena Wall)
    // Mirror of east at X-22. SPELL_WITCH particles (magenta).
    // ================================================================
    public static class WingWallShadowWest extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shadowTiles = new ArrayList<>();

        public WingWallShadowWest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wing_wall_shadow_west", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location wallCenter = center.clone().add(-22, 8, 0);
            for (int i = 0; i < 12; i++) {
                double t = (double) i / 11;
                double z = (t - 0.5) * 16;
                double y = -Math.abs(z) * 0.5;
                Location loc = wallCenter.clone().add(0, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.05f, 4.0f, 4.0f).glow(180, 0, 200).interpolation(4, 0);
                shadowTiles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            float xPulse = 0.05f + 0.02f * (float) Math.sin(ticksAlive * Math.PI / 20);
            for (BlockDisplayHandle tile : shadowTiles) {
                tile.scale(xPulse, 4.0f, 4.0f);
                tile.interpolation(4, 0);
            }

            // SPELL_WITCH from perimeter (west = magenta)
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < shadowTiles.size(); i += 3) {
                    Location tileLoc = shadowTiles.get(i).entity().getLocation();
                    center.getWorld().spawnParticle(Particle.WITCH, tileLoc, 3,
                            0.1, 1.5, 1.5, 0.01);
                }
            }

            if (ticksAlive % 400 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center.clone().add(-22, 8, 0),
                        Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.1f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WingWallShadowWest(plugin); }
    }

    // ================================================================
    // #46 -- CROWN GHOST ARRAY (Overhead Wing Tips)
    // 8 dark prismarine blocks (2 groups of 4) at Y+18-22.
    // Scale-flash when wing tip proximity triggers. END_ROD bursts.
    // ================================================================
    public static class CrownGhostArray extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ghostBlocks = new ArrayList<>();

        public CrownGhostArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crown_ghost_array", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left group (4 blocks) at Y+18-22, X-8 to X-2
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(-8 + i * 2, 20, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(2.0f, 0.1f, 2.0f).glow(128, 0, 255).interpolation(2, 0);
                ghostBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right group (4 blocks) at Y+18-22, X+2 to X+8
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(2 + i * 2, 20, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(2.0f, 0.1f, 2.0f).glow(128, 0, 255).interpolation(2, 0);
                ghostBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Scale oscillation +/-0.3 on X and Z, 2-sec (40-tick) cycle
            float scaleOsc = 2.0f + 0.3f * (float) Math.sin(ticksAlive * Math.PI / 20);

            for (BlockDisplayHandle block : ghostBlocks) {
                block.scale(scaleOsc, 0.1f, scaleOsc);
                block.interpolation(2, 0);
            }

            // Periodic scale-flash: simulate wing tip proximity every 40 ticks
            if (ticksAlive % 40 == 0) {
                int flashIdx = (ticksAlive / 40) % ghostBlocks.size();
                BlockDisplayHandle flashBlock = ghostBlocks.get(flashIdx);
                flashBlock.scale(4.0f, 0.5f, 4.0f);
                flashBlock.interpolation(2, 0);

                // END_ROD burst
                Location flashLoc = flashBlock.entity().getLocation();
                center.getWorld().spawnParticle(Particle.END_ROD, flashLoc, 20,
                        1.5, 0.5, 1.5, 0.03);

                DisplayBuilder.playSound(flashLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrownGhostArray(plugin); }
    }

    // ================================================================
    // #47 -- VOID MEMBRANE FLOOR PANEL (Landing Zone)
    // 4x4 dark prismarine tiles at Y+0.2. Expands from center on spawn,
    // pulses while active, contracts on cleanup. DRAGON_BREATH tiles.
    // ================================================================
    public static class VoidMembraneFloorPanel extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> floorTiles = new ArrayList<>();
        private boolean expandComplete = false;

        public VoidMembraneFloorPanel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_membrane_floor", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4x4 grid centered on arena, each tile starts small
            for (int x = 0; x < 4; x++) {
                for (int z = 0; z < 4; z++) {
                    Location loc = center.clone().add(-6 + x * 4, 0.2, -6 + z * 4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                    h.scale(0.5f, 0.05f, 0.5f).glow(128, 0, 255).interpolation(20, 0);
                    floorTiles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Expand animation over first 20 ticks
            if (!expandComplete && ticksAlive == 1) {
                for (BlockDisplayHandle tile : floorTiles) {
                    tile.scale(4.0f, 0.05f, 4.0f);
                    tile.interpolation(20, 0);
                }
                expandComplete = true;
            }

            // Pulse X/Z +/-0.3 on 3-sec (60-tick) cycle once expanded
            if (expandComplete && ticksAlive > 20) {
                float pulse = 4.0f + 0.3f * (float) Math.sin(ticksAlive * Math.PI / 30);
                for (BlockDisplayHandle tile : floorTiles) {
                    tile.scale(pulse, 0.05f, pulse);
                    tile.interpolation(3, 0);
                }
            }

            // DRAGON_BREATH from each tile while active
            if (ticksAlive % 4 == 0 && expandComplete) {
                for (int i = 0; i < floorTiles.size(); i += 4) {
                    Location tileLoc = floorTiles.get(i).entity().getLocation().clone().add(2, 0.2, 2);
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, tileLoc, 3,
                            1.5, 0.1, 1.5, 0);
                }
            }

            // Portal ambient during stationary phase
            if (ticksAlive % 60 == 0 && expandComplete) {
                DisplayBuilder.playSound(center.clone().add(0, 0.2, 0),
                        Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidMembraneFloorPanel(plugin); }
    }

    // ================================================================
    // #48 -- WING ORBIT RING (High Altitude Ring, Phase 3)
    // 16 dark prismarine blocks in a circle at radius 15, Y+25.
    // Independent Y oscillation per panel. Purple particle rain.
    // ================================================================
    public static class WingOrbitRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringPanels = new ArrayList<>();
        private final float[] oscillationPeriods = new float[16];
        private static final double RING_RADIUS = 15.0;

        public WingOrbitRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wing_orbit_ring", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                double x = Math.cos(angle) * RING_RADIUS;
                double z = Math.sin(angle) * RING_RADIUS;
                Location loc = center.clone().add(x, 25, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(6.0f, 0.1f, 6.0f).glow(128, 0, 255).interpolation(5, 0);
                ringPanels.add(h);
                spawnedEntities.add(h.entity());

                // Random period 3-7 sec (60-140 ticks)
                oscillationPeriods[i] = 60 + (float) (Math.random() * 80);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Independent Y oscillation +/-1.0 block per panel
            for (int i = 0; i < ringPanels.size(); i++) {
                double angle = (2 * Math.PI * i) / 16;
                double x = Math.cos(angle) * RING_RADIUS;
                double z = Math.sin(angle) * RING_RADIUS;
                float yOsc = 1.0f * (float) Math.sin(ticksAlive * Math.PI / oscillationPeriods[i]);
                Location loc = center.clone().add(x, 25 + yOsc, z);
                ringPanels.get(i).entity().teleport(loc);
            }

            // DRAGON_BREATH rain from each panel
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < ringPanels.size(); i += 2) {
                    Location panelLoc = ringPanels.get(i).entity().getLocation();
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, panelLoc, 5,
                            2.0, 0.1, 2.0, 0.02);
                }
            }

            // SPELL_WITCH added
            if (ticksAlive % 4 == 0) {
                for (int i = 1; i < ringPanels.size(); i += 4) {
                    Location panelLoc = ringPanels.get(i).entity().getLocation();
                    center.getWorld().spawnParticle(Particle.WITCH, panelLoc, 3,
                            2.0, 0.1, 2.0, 0.01);
                }
            }

            // Unsettling wither ambient sound every 300 ticks (15 sec)
            if (ticksAlive % 300 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center.clone().add(0, 25, 0),
                        Sound.ENTITY_WITHER_AMBIENT, 0.2f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WingOrbitRing(plugin); }
    }

    // ================================================================
    // #49 -- TRAILING WINGTIP LINES (Phase 2 Speed Ghosts)
    // 8 thin obsidian trail needles (4 per wing tip). Each spawns at
    // wing tip position, shrinks over lifespan. CRIT particles trail.
    // ================================================================
    public static class TrailingWingtipLines extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> trailSegments = new ArrayList<>();
        private int segmentPointer = 0;
        private static final int MAX_SEGMENTS = 8;

        public TrailingWingtipLines(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trailing_wingtip_lines", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pre-spawn 8 segments (recycled positions)
            for (int i = 0; i < MAX_SEGMENTS; i++) {
                // Initial positions spread out simulating a trail
                double angle = Math.toRadians(i * 15);
                double x = Math.cos(angle) * 10;
                double z = Math.sin(angle) * 10;
                double y = 10 + Math.sin(angle) * 3;
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.1f, 0.1f, 6.0f).glow(128, 0, 255).interpolation(10, 0);
                trailSegments.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Simulate wing tip orbit and spawn trail segments every 3 ticks
            if (ticksAlive % 3 == 0) {
                double orbitAngle = Math.toRadians(ticksAlive * 1.5);
                // Left wing tip
                double leftX = Math.cos(orbitAngle) * 12 - 5;
                double leftZ = Math.sin(orbitAngle) * 12;
                double leftY = 12 + Math.sin(orbitAngle * 0.7) * 4;

                int idx = segmentPointer % MAX_SEGMENTS;
                Location tipLoc = center.clone().add(leftX, leftY, leftZ);
                trailSegments.get(idx).entity().teleport(tipLoc);

                // Align with velocity vector (tangent to orbit)
                float tangent = (float) (orbitAngle + Math.PI / 2);
                trailSegments.get(idx).rotate(tangent, 0, 1, 0);
                trailSegments.get(idx).scale(0.1f, 0.1f, 6.0f);
                trailSegments.get(idx).interpolation(10, 0);

                segmentPointer++;
            }

            // Older segments shrink over time
            for (int i = 0; i < trailSegments.size(); i++) {
                int age = (segmentPointer - i + MAX_SEGMENTS) % MAX_SEGMENTS;
                float lengthFade = 6.0f - age * 0.7f;
                if (lengthFade < 0.5f) lengthFade = 0.5f;
                trailSegments.get(i).scale(0.1f, 0.1f, lengthFade);
                trailSegments.get(i).interpolation(10, 0);
            }

            // CRIT particles from each segment
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle seg : trailSegments) {
                    Location segLoc = seg.entity().getLocation();
                    center.getWorld().spawnParticle(Particle.CRIT, segLoc, 2,
                            0.1, 0.1, 0.5, 0.01);
                }
            }

            // Whistle sounds on segment spawn
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 12, 0),
                        Sound.ENTITY_ARROW_SHOOT, 0.15f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TrailingWingtipLines(plugin); }
    }

    // ================================================================
    // #50 -- FINAL SANCTUM CONVERGENCE STRUCTURE (Phase 3 Endgame)
    // The visual climax: orbiting amethyst, gold, crying obsidian blocks
    // around a central anchor. 12-second open/close cycle with particle
    // intensification at closed state peak.
    // ================================================================
    public static class FinalSanctumConvergence extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> amethystRing = new ArrayList<>();
        private final List<BlockDisplayHandle> goldRing = new ArrayList<>();
        private final List<BlockDisplayHandle> cardinalPillars = new ArrayList<>();
        private BlockDisplayHandle centralAnchor;

        // Orbital parameters
        private static final double AMETHYST_BASE_RADIUS = 3.0;
        private static final double GOLD_BASE_RADIUS = 5.0;
        private static final float AMETHYST_SPEED = 3.0f; // deg/tick
        private static final float GOLD_SPEED = -2.0f; // deg/tick, counter
        private static final int CYCLE_TICKS = 240; // 12 seconds

        public FinalSanctumConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("final_sanctum_convergence", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(12.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // (A) 8 amethyst blocks orbiting at radius 3, Y+10
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * AMETHYST_BASE_RADIUS;
                double z = Math.sin(angle) * AMETHYST_BASE_RADIUS;
                Location loc = center.clone().add(x, 10, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
                amethystRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // (B) 4 gold blocks orbiting at radius 5, Y+15
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                double x = Math.cos(angle) * GOLD_BASE_RADIUS;
                double z = Math.sin(angle) * GOLD_BASE_RADIUS;
                Location loc = center.clone().add(x, 15, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                h.scale(1.5f, 1.5f, 1.5f).glow(180, 0, 200).interpolation(3, 0);
                goldRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // (C) 4 crying obsidian pillars at cardinal points, radius 2, Y+0 to Y+8
            double[] cardinalAngles = {0, Math.PI / 2, Math.PI, 3 * Math.PI / 2};
            for (double angle : cardinalAngles) {
                double x = Math.cos(angle) * 2;
                double z = Math.sin(angle) * 2;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.8f, 8.0f, 0.8f).glow(180, 0, 200);
                cardinalPillars.add(h);
                spawnedEntities.add(h.entity());
            }

            // (E) Central anchor: large amethyst at Y+5
            centralAnchor = displayBuilder.spawnBlock(center.clone().add(0, 5, 0), Material.AMETHYST_BLOCK);
            centralAnchor.scale(3.0f, 3.0f, 3.0f).glow(128, 0, 255).interpolation(90, 0);
            spawnedEntities.add(centralAnchor.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 12-second open/close cycle: closed at 6 sec (120 ticks), open at 0/12 sec
            float cyclePhase = (ticksAlive % CYCLE_TICKS) / (float) CYCLE_TICKS;
            // closeness: 0 = open, 1 = fully closed
            float closeness = (float) Math.sin(cyclePhase * Math.PI);

            // Radius reduction: 50% at closed state
            double amethystRadius = AMETHYST_BASE_RADIUS * (1.0 - closeness * 0.5);
            double goldRadius = GOLD_BASE_RADIUS * (1.0 - closeness * 0.5);

            // Central anchor scale: 3.0 open, 5.0 closed
            float anchorScale = 3.0f + 2.0f * closeness;
            if (centralAnchor != null) {
                centralAnchor.scale(anchorScale, anchorScale, anchorScale);
                centralAnchor.interpolation(10, 0);
            }

            // (A) Amethyst ring orbit
            double amOrbit = Math.toRadians(ticksAlive * AMETHYST_SPEED);
            for (int i = 0; i < amethystRing.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 8;
                double angle = baseAngle + amOrbit;
                double x = Math.cos(angle) * amethystRadius;
                double z = Math.sin(angle) * amethystRadius;
                Location loc = center.clone().add(x, 10, z);
                amethystRing.get(i).entity().teleport(loc);
                amethystRing.get(i).interpolation(3, 0);
            }

            // (B) Gold ring orbit (counter-clockwise)
            double goldOrbit = Math.toRadians(ticksAlive * GOLD_SPEED);
            for (int i = 0; i < goldRing.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 4;
                double angle = baseAngle + goldOrbit;
                double x = Math.cos(angle) * goldRadius;
                double z = Math.sin(angle) * goldRadius;
                Location loc = center.clone().add(x, 15, z);
                goldRing.get(i).entity().teleport(loc);
                goldRing.get(i).interpolation(3, 0);
            }

            // Particle intensification at closed state
            int particleMult = 1 + (int) (closeness * 3);

            // DRAGON_BREATH from central orb
            if (ticksAlive % 2 == 0) {
                Location orbLoc = center.clone().add(0, 5, 0);
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, orbLoc,
                        10 * particleMult, 1.5, 1.5, 1.5, 0);
            }

            // SPELL_WITCH from amethyst ring
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < amethystRing.size(); i += 2) {
                    center.getWorld().spawnParticle(Particle.WITCH,
                            amethystRing.get(i).entity().getLocation(),
                            3 * particleMult, 0.5, 0.5, 0.5, 0.01);
                }
            }

            // TOTEM from gold ring
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle gold : goldRing) {
                    center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                            gold.entity().getLocation(),
                            2 * particleMult, 0.5, 0.5, 0.5, 0.01);
                }
            }

            // Sound design: dragon growl during contraction, ambient at peak
            int cyclePos = ticksAlive % CYCLE_TICKS;
            if (cyclePos == 0) {
                // Open state
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.7f);
            }
            if (cyclePos == 20) {
                // Contraction beginning: growl
                DisplayBuilder.playSound(center.clone().add(0, 10, 0),
                        Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.4f);
            }
            if (cyclePos == CYCLE_TICKS / 2) {
                // Closed-state peak: loud ambient
                DisplayBuilder.playSound(center.clone().add(0, 5, 0),
                        Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.8f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FinalSanctumConvergence(plugin); }
    }
}
