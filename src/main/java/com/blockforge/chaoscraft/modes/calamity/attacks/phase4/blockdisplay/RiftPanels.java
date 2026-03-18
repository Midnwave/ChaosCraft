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
 * Phase 4D Block Display -- GROUP 4: MAGENTA/CYAN RIFT PANELS (#29-38)
 * 10 attacks forming dimensional tear frames -- visual evidence that the
 * Void Emperor is splitting the boundary between the End and beyond.
 *
 * Structures:
 *  #29 NorthRiftGate           -- Primary N gate, magenta membrane, crying obsidian frame
 *  #30 SouthRiftGate           -- Primary S gate, cyan membrane
 *  #31 EastRiftShard           -- Partial tear, incomplete L-shape, magenta
 *  #32 WestRiftShard           -- Partial tear, reversed L-shape, cyan
 *  #33 NortheastRiftPanel      -- Tilted 45 deg, magenta, slow Y orbit
 *  #34 SouthwestRiftPanel      -- Tilted 45 deg, cyan, opposite Y orbit
 *  #35 TwinRiftTowersNorth     -- Flanking towers for north gate
 *  #36 TwinRiftTowersSouth     -- Flanking towers for south gate, calcite crowns
 *  #37 RiftConnectorFilaments  -- Thin horizontal lines along E/W walls
 *  #38 RiftCollapseArray       -- 6 emergency tears at Phase 3 threshold
 *
 * Rules applied:
 * - NO status effects
 * - Void Emperor palette: magenta(180,0,200), cyan(0,200,255), purple(128,0,255)
 * - AxisAngle4f ONLY, static DisplayBuilder methods
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Damage HP 4.0-12.0
 */
public final class RiftPanels {

    private RiftPanels() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new NorthRiftGate(plugin));
        registry.register(new SouthRiftGate(plugin));
        registry.register(new EastRiftShard(plugin));
        registry.register(new WestRiftShard(plugin));
        registry.register(new NortheastRiftPanel(plugin));
        registry.register(new SouthwestRiftPanel(plugin));
        registry.register(new TwinRiftTowersNorth(plugin));
        registry.register(new TwinRiftTowersSouth(plugin));
        registry.register(new RiftConnectorFilaments(plugin));
        registry.register(new RiftCollapseArray(plugin));
    }

    // ================================================================
    // #29 -- NORTH RIFT GATE (Primary)
    // Crying obsidian frame (10x10 exterior), magenta stained glass
    // membrane in 4x4 grid. Membrane ripples, SPELL_WITCH leak.
    // ================================================================
    public static class NorthRiftGate extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> cornerStones = new ArrayList<>();
        private final List<BlockDisplayHandle> membraneGrid = new ArrayList<>();

        public NorthRiftGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("north_rift_gate", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location gateCenter = center.clone().add(0, 5, -18);

            // Left column: 10 blocks of crying obsidian
            for (int y = -5; y < 5; y++) {
                Location loc = gateCenter.clone().add(-5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.8f, 1.0f, 0.2f).glow(180, 0, 200);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right column
            for (int y = -5; y < 5; y++) {
                Location loc = gateCenter.clone().add(5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.8f, 1.0f, 0.2f).glow(180, 0, 200);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Top lintel
            BlockDisplayHandle lintel = displayBuilder.spawnBlock(
                    gateCenter.clone().add(0, 5, 0), Material.CRYING_OBSIDIAN);
            lintel.scale(10.4f, 0.8f, 0.2f).glow(180, 0, 200);
            frameBlocks.add(lintel);
            spawnedEntities.add(lintel.entity());

            // 4 corner keystones: amethyst block slightly oversized
            double[][] corners = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}};
            for (double[] c : corners) {
                Location loc = gateCenter.clone().add(c[0], c[1], 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(1.2f, 1.2f, 0.2f).glow(128, 0, 255);
                cornerStones.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4x4 magenta stained glass membrane
            for (int gx = 0; gx < 4; gx++) {
                for (int gy = 0; gy < 4; gy++) {
                    double mx = -3.0 + gx * 2.0;
                    double my = -3.0 + gy * 2.0;
                    Location loc = gateCenter.clone().add(mx, my, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGENTA_STAINED_GLASS);
                    h.scale(2.0f, 2.0f, 0.05f).glow(180, 0, 200).interpolation(4, 0);
                    membraneGrid.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(gateCenter, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location gateCenter = center.clone().add(0, 5, -18);

            // Membrane ripple: Z-position oscillation based on 2D grid position
            for (int i = 0; i < membraneGrid.size(); i++) {
                int gx = i / 4;
                int gy = i % 4;
                float zOsc = 0.02f * (float) Math.sin(ticksAlive * Math.PI / 20 + gx * 0.8 + gy * 0.6);
                membraneGrid.get(i).translate(-1.0f, -1.0f, -0.025f + zOsc);
                membraneGrid.get(i).interpolation(4, 0);
            }

            // SPELL_WITCH particles leaking through membrane toward arena
            if (ticksAlive % 2 == 0) {
                double rx = -3.0 + Math.random() * 6.0;
                double ry = -3.0 + Math.random() * 6.0;
                Location pLoc = gateCenter.clone().add(rx, ry, 0.5);
                center.getWorld().spawnParticle(Particle.WITCH, pLoc, 3, 0.3, 0.3, 0.5, 0.01);
            }

            // Portal ambient loop every 80 ticks (4 sec)
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(gateCenter, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.6f);
            }

            // Enderman stare every 900 ticks (45 sec)
            if (ticksAlive % 900 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(gateCenter, Sound.ENTITY_ENDERMAN_STARE, 0.1f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NorthRiftGate(plugin); }
    }

    // ================================================================
    // #30 -- SOUTH RIFT GATE (Primary)
    // Mirror of north gate at Z+18. Cyan stained glass membrane.
    // Phase-inverted ripple.
    // ================================================================
    public static class SouthRiftGate extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> cornerStones = new ArrayList<>();
        private final List<BlockDisplayHandle> membraneGrid = new ArrayList<>();

        public SouthRiftGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("south_rift_gate", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location gateCenter = center.clone().add(0, 5, 18);

            // Left column
            for (int y = -5; y < 5; y++) {
                Location loc = gateCenter.clone().add(-5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.8f, 1.0f, 0.2f).glow(0, 200, 255);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right column
            for (int y = -5; y < 5; y++) {
                Location loc = gateCenter.clone().add(5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.8f, 1.0f, 0.2f).glow(0, 200, 255);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Top lintel
            BlockDisplayHandle lintel = displayBuilder.spawnBlock(
                    gateCenter.clone().add(0, 5, 0), Material.CRYING_OBSIDIAN);
            lintel.scale(10.4f, 0.8f, 0.2f).glow(0, 200, 255);
            frameBlocks.add(lintel);
            spawnedEntities.add(lintel.entity());

            // Corner keystones
            double[][] corners = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}};
            for (double[] c : corners) {
                Location loc = gateCenter.clone().add(c[0], c[1], 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(1.2f, 1.2f, 0.2f).glow(0, 200, 255);
                cornerStones.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4x4 cyan stained glass membrane
            for (int gx = 0; gx < 4; gx++) {
                for (int gy = 0; gy < 4; gy++) {
                    double mx = -3.0 + gx * 2.0;
                    double my = -3.0 + gy * 2.0;
                    Location loc = gateCenter.clone().add(mx, my, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                    h.scale(2.0f, 2.0f, 0.05f).glow(0, 200, 255).interpolation(4, 0);
                    membraneGrid.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(gateCenter, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location gateCenter = center.clone().add(0, 5, 18);

            // Phase-inverted ripple (+ PI)
            for (int i = 0; i < membraneGrid.size(); i++) {
                int gx = i / 4;
                int gy = i % 4;
                float zOsc = 0.02f * (float) Math.sin(ticksAlive * Math.PI / 20 + gx * 0.8 + gy * 0.6 + Math.PI);
                membraneGrid.get(i).translate(-1.0f, -1.0f, -0.025f + zOsc);
                membraneGrid.get(i).interpolation(4, 0);
            }

            // Cyan-ish particles (END_ROD + cyan DUST)
            if (ticksAlive % 2 == 0) {
                double rx = -3.0 + Math.random() * 6.0;
                double ry = -3.0 + Math.random() * 6.0;
                Location pLoc = gateCenter.clone().add(rx, ry, -0.5);
                center.getWorld().spawnParticle(Particle.END_ROD, pLoc, 2, 0.3, 0.3, 0.3, 0.01);
                DisplayBuilder.cyanDust(pLoc, 3, 0.5);
            }

            // Portal ambient loop
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(gateCenter, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.6f);
            }

            // Enderman stare
            if (ticksAlive % 900 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(gateCenter, Sound.ENTITY_ENDERMAN_STARE, 0.1f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SouthRiftGate(plugin); }
    }

    // ================================================================
    // #31 -- EAST RIFT SHARD (Partial Tear, Small)
    // Incomplete L-shape frame at X+18, magenta membrane.
    // More agitated ripple, ragged particle spray.
    // ================================================================
    public static class EastRiftShard extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> membraneGrid = new ArrayList<>();

        public EastRiftShard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("east_rift_shard", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location shardCenter = center.clone().add(18, 4.5, 6);

            // Right vertical column: 5 blocks tall
            for (int y = 0; y < 5; y++) {
                Location loc = shardCenter.clone().add(1.5, y - 2.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 1.0f, 0.2f).glow(180, 0, 200);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Partial top lintel (right half only, 3 blocks)
            for (int x = 0; x < 3; x++) {
                Location loc = shardCenter.clone().add(1.5 - x * 1.0, 2.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(1.0f, 0.6f, 0.2f).glow(180, 0, 200);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2x4 magenta membrane covering bounded area
            for (int gx = 0; gx < 2; gx++) {
                for (int gy = 0; gy < 4; gy++) {
                    double mx = -0.5 + gx * 1.5;
                    double my = -1.5 + gy * 1.0;
                    Location loc = shardCenter.clone().add(mx, my, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGENTA_STAINED_GLASS);
                    h.scale(1.5f, 1.0f, 0.04f).glow(180, 0, 200).interpolation(3, 0);
                    membraneGrid.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location shardCenter = center.clone().add(18, 4.5, 6);

            // Agitated membrane ripple: faster frequency, larger amplitude
            for (int i = 0; i < membraneGrid.size(); i++) {
                int gx = i / 4;
                int gy = i % 4;
                float zOsc = 0.05f * (float) Math.sin(ticksAlive * Math.PI / 8 + gx * 1.2 + gy * 0.9);
                membraneGrid.get(i).translate(-0.75f, -0.5f, -0.02f + zOsc);
                membraneGrid.get(i).interpolation(3, 0);
            }

            // Ragged SPELL_WITCH spray from torn left edge
            if (ticksAlive % 2 == 0) {
                Location tornEdge = shardCenter.clone().add(-1.5, Math.random() * 4 - 2, 0);
                center.getWorld().spawnParticle(Particle.WITCH, tornEdge, 10,
                        2.0, 1.0, 2.0, 0.02);
            }

            // Glass cracking sound every 240 ticks (12 sec)
            if (ticksAlive % 240 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(shardCenter, Sound.BLOCK_GLASS_BREAK, 0.15f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EastRiftShard(plugin); }
    }

    // ================================================================
    // #32 -- WEST RIFT SHARD (Partial Tear, Small)
    // Mirror of east shard at X-18, cyan membrane. Reversed L-shape.
    // ================================================================
    public static class WestRiftShard extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> membraneGrid = new ArrayList<>();

        public WestRiftShard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("west_rift_shard", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location shardCenter = center.clone().add(-18, 4.5, -6);

            // Left vertical column: 5 blocks tall
            for (int y = 0; y < 5; y++) {
                Location loc = shardCenter.clone().add(-1.5, y - 2.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 1.0f, 0.2f).glow(0, 200, 255);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Partial top lintel (left half)
            for (int x = 0; x < 3; x++) {
                Location loc = shardCenter.clone().add(-1.5 + x * 1.0, 2.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(1.0f, 0.6f, 0.2f).glow(0, 200, 255);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2x4 cyan membrane
            for (int gx = 0; gx < 2; gx++) {
                for (int gy = 0; gy < 4; gy++) {
                    double mx = 0.5 - gx * 1.5;
                    double my = -1.5 + gy * 1.0;
                    Location loc = shardCenter.clone().add(mx, my, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                    h.scale(1.5f, 1.0f, 0.04f).glow(0, 200, 255).interpolation(3, 0);
                    membraneGrid.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location shardCenter = center.clone().add(-18, 4.5, -6);

            // Agitated membrane ripple
            for (int i = 0; i < membraneGrid.size(); i++) {
                int gx = i / 4;
                int gy = i % 4;
                float zOsc = 0.05f * (float) Math.sin(ticksAlive * Math.PI / 8 + gx * 1.2 + gy * 0.9);
                membraneGrid.get(i).translate(0.75f, -0.5f, -0.02f + zOsc);
                membraneGrid.get(i).interpolation(3, 0);
            }

            // Ragged END_ROD + cyan dust spray from torn right edge
            if (ticksAlive % 2 == 0) {
                Location tornEdge = shardCenter.clone().add(1.5, Math.random() * 4 - 2, 0);
                center.getWorld().spawnParticle(Particle.END_ROD, tornEdge, 4, 2.0, 1.0, 2.0, 0.02);
                DisplayBuilder.cyanDust(tornEdge, 6, 1.5);
            }

            // Glass cracking sound
            if (ticksAlive % 240 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(shardCenter, Sound.BLOCK_GLASS_BREAK, 0.15f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WestRiftShard(plugin); }
    }

    // ================================================================
    // #33 -- NORTHEAST RIFT CORNER PANEL (Tilted)
    // 6x7 rift frame rotated 45 deg on Y, tilted 15 deg from vertical.
    // Slow Y orbit at 0.1 deg/tick. Magenta membrane emits both sides.
    // ================================================================
    public static class NortheastRiftPanel extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> membraneGrid = new ArrayList<>();
        private Location panelCenter;

        public NortheastRiftPanel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("northeast_rift_panel", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            panelCenter = center.clone().add(16, 4.5, -16);

            // Frame: 4 crying obsidian blocks forming rectangle edges
            // Top and bottom horizontals
            for (int i = 0; i < 2; i++) {
                float yOff = (i == 0) ? -3.5f : 3.5f;
                Location loc = panelCenter.clone().add(0, yOff, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(6.0f, 0.6f, 0.2f)
                        .rotate((float) Math.toRadians(45), 0, 1, 0)
                        .glow(180, 0, 200);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Left and right verticals
            for (int i = 0; i < 2; i++) {
                float xOff = (i == 0) ? -3.0f : 3.0f;
                Location loc = panelCenter.clone().add(xOff, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 7.0f, 0.2f)
                        .rotate((float) Math.toRadians(45), 0, 1, 0)
                        .glow(180, 0, 200);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2x2 magenta membrane panels
            for (int gx = 0; gx < 2; gx++) {
                for (int gy = 0; gy < 2; gy++) {
                    double mx = -1.0 + gx * 2.0;
                    double my = -1.75 + gy * 3.5;
                    Location loc = panelCenter.clone().add(mx, my, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGENTA_STAINED_GLASS);
                    h.scale(2.0f, 3.5f, 0.04f)
                            .rotate((float) Math.toRadians(45), 0, 1, 0)
                            .glow(180, 0, 200).interpolation(5, 0);
                    membraneGrid.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Slow Y orbit at 0.1 deg/tick (clockwise)
            float yRot = (float) Math.toRadians(45 + ticksAlive * 0.1);

            // Update frame rotations
            for (BlockDisplayHandle frame : frameBlocks) {
                frame.rotate(yRot, 0, 1, 0);
            }

            // Update membrane rotations
            for (BlockDisplayHandle mem : membraneGrid) {
                mem.rotate(yRot, 0, 1, 0);
                mem.interpolation(5, 0);
            }

            // SPELL_WITCH from both faces
            if (ticksAlive % 2 == 0) {
                Location pCenter = center.clone().add(16, 4.5, -16);
                center.getWorld().spawnParticle(Particle.WITCH, pCenter, 8,
                        2.0, 2.5, 2.0, 0.01);
            }

            // Portal travel sound every 400 ticks (20 sec)
            if (ticksAlive % 400 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center.clone().add(16, 4.5, -16),
                        Sound.BLOCK_PORTAL_TRIGGER, 0.2f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NortheastRiftPanel(plugin); }
    }

    // ================================================================
    // #34 -- SOUTHWEST RIFT CORNER PANEL (Tilted)
    // Mirror of NE at X-16 Z+16. Cyan membrane.
    // Counter-clockwise Y orbit.
    // ================================================================
    public static class SouthwestRiftPanel extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> membraneGrid = new ArrayList<>();

        public SouthwestRiftPanel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("southwest_rift_panel", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location panelCenter = center.clone().add(-16, 4.5, 16);

            // Frame
            for (int i = 0; i < 2; i++) {
                float yOff = (i == 0) ? -3.5f : 3.5f;
                Location loc = panelCenter.clone().add(0, yOff, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(6.0f, 0.6f, 0.2f)
                        .rotate((float) Math.toRadians(45), 0, 1, 0)
                        .glow(0, 200, 255);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 2; i++) {
                float xOff = (i == 0) ? -3.0f : 3.0f;
                Location loc = panelCenter.clone().add(xOff, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 7.0f, 0.2f)
                        .rotate((float) Math.toRadians(45), 0, 1, 0)
                        .glow(0, 200, 255);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2x2 cyan membrane
            for (int gx = 0; gx < 2; gx++) {
                for (int gy = 0; gy < 2; gy++) {
                    double mx = -1.0 + gx * 2.0;
                    double my = -1.75 + gy * 3.5;
                    Location loc = panelCenter.clone().add(mx, my, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                    h.scale(2.0f, 3.5f, 0.04f)
                            .rotate((float) Math.toRadians(45), 0, 1, 0)
                            .glow(0, 200, 255).interpolation(5, 0);
                    membraneGrid.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Counter-clockwise Y orbit
            float yRot = (float) Math.toRadians(45 - ticksAlive * 0.1);

            for (BlockDisplayHandle frame : frameBlocks) {
                frame.rotate(yRot, 0, 1, 0);
            }
            for (BlockDisplayHandle mem : membraneGrid) {
                mem.rotate(yRot, 0, 1, 0);
                mem.interpolation(5, 0);
            }

            // Cyan particles
            if (ticksAlive % 2 == 0) {
                Location pCenter = center.clone().add(-16, 4.5, 16);
                center.getWorld().spawnParticle(Particle.END_ROD, pCenter, 4, 2.0, 2.5, 2.0, 0.01);
                DisplayBuilder.cyanDust(pCenter, 4, 1.5);
            }

            // Sound
            if (ticksAlive % 400 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center.clone().add(-16, 4.5, 16),
                        Sound.BLOCK_PORTAL_TRIGGER, 0.2f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SouthwestRiftPanel(plugin); }
    }

    // ================================================================
    // #35 -- TWIN RIFT TOWERS (North, Pair)
    // Two tall crying obsidian columns flanking the north gate at X+/-7 Z-18.
    // Amethyst crowns pulse synchronized. SPELL_WITCH drips downward.
    // ================================================================
    public static class TwinRiftTowersNorth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> towerPillars = new ArrayList<>();
        private final List<BlockDisplayHandle> towerCrowns = new ArrayList<>();

        public TwinRiftTowersNorth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("twin_rift_towers_north", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double[] xPositions = {-7, 7};
            for (double xOff : xPositions) {
                Location base = center.clone().add(xOff, 0, -18);

                // Main pillar: tall thin crying obsidian
                BlockDisplayHandle pillar = displayBuilder.spawnBlock(base, Material.CRYING_OBSIDIAN);
                pillar.scale(0.4f, 14.0f, 0.4f).glow(180, 0, 200);
                towerPillars.add(pillar);
                spawnedEntities.add(pillar.entity());

                // Amethyst crown at top
                Location crownLoc = base.clone().add(0, 14, 0);
                BlockDisplayHandle crown = displayBuilder.spawnBlock(crownLoc, Material.AMETHYST_BLOCK);
                crown.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
                towerCrowns.add(crown);
                spawnedEntities.add(crown.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Synchronized crown pulse: 0.8 to 1.2 on 3-sec (60-tick) cycle
            float pulse = 0.8f + 0.4f * (float) Math.sin(ticksAlive * Math.PI / 30);
            for (BlockDisplayHandle crown : towerCrowns) {
                crown.scale(pulse, pulse, pulse);
                crown.interpolation(3, 0);
            }

            // SPELL_WITCH dripping downward from crown
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle crown : towerCrowns) {
                    Location crownLoc = crown.entity().getLocation();
                    center.getWorld().spawnParticle(Particle.WITCH, crownLoc, 4,
                            0.1, 3.0, 0.1, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TwinRiftTowersNorth(plugin); }
    }

    // ================================================================
    // #36 -- TWIN RIFT TOWERS (South, Pair)
    // Flanking south gate at X+/-7 Z+18. Calcite crowns (white),
    // END_ROD drips downward. Phase-offset from north by 1.5 sec.
    // ================================================================
    public static class TwinRiftTowersSouth extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> towerPillars = new ArrayList<>();
        private final List<BlockDisplayHandle> towerCrowns = new ArrayList<>();

        public TwinRiftTowersSouth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("twin_rift_towers_south", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double[] xPositions = {-7, 7};
            for (double xOff : xPositions) {
                Location base = center.clone().add(xOff, 0, 18);

                BlockDisplayHandle pillar = displayBuilder.spawnBlock(base, Material.CRYING_OBSIDIAN);
                pillar.scale(0.4f, 14.0f, 0.4f).glow(0, 200, 255);
                towerPillars.add(pillar);
                spawnedEntities.add(pillar.entity());

                // Calcite crown (south distinction)
                Location crownLoc = base.clone().add(0, 14, 0);
                BlockDisplayHandle crown = displayBuilder.spawnBlock(crownLoc, Material.CALCITE);
                crown.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
                towerCrowns.add(crown);
                spawnedEntities.add(crown.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Phase-offset from north towers by 1.5 sec (30 ticks)
            float pulse = 0.8f + 0.4f * (float) Math.sin((ticksAlive - 30) * Math.PI / 30);
            for (BlockDisplayHandle crown : towerCrowns) {
                crown.scale(pulse, pulse, pulse);
                crown.interpolation(3, 0);
            }

            // END_ROD dripping downward from crown (white-blue)
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle crown : towerCrowns) {
                    Location crownLoc = crown.entity().getLocation();
                    center.getWorld().spawnParticle(Particle.END_ROD, crownLoc, 4,
                            0.1, 3.0, 0.1, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TwinRiftTowersSouth(plugin); }
    }

    // ================================================================
    // #37 -- RIFT CONNECTOR FILAMENTS (Gate to Gate)
    // Thin horizontal lines along E/W walls at Y+6, connecting N/S
    // rifts visually. Brightness wave propagates north to south.
    // ================================================================
    public static class RiftConnectorFilaments extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> eastFilaments = new ArrayList<>();
        private final List<BlockDisplayHandle> westFilaments = new ArrayList<>();
        private static final int FILAMENT_COUNT = 12;

        public RiftConnectorFilaments(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_connector_filaments", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // East wall filament (cyan): Z from -18 to +18 at X+22
            for (int i = 0; i < FILAMENT_COUNT; i++) {
                double z = -18.0 + (36.0 / (FILAMENT_COUNT - 1)) * i;
                Location loc = center.clone().add(22, 6, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.05f, 0.05f, 3.0f).glow(0, 200, 255).interpolation(3, 0);
                eastFilaments.add(h);
                spawnedEntities.add(h.entity());
            }

            // West wall filament (magenta)
            for (int i = 0; i < FILAMENT_COUNT; i++) {
                double z = -18.0 + (36.0 / (FILAMENT_COUNT - 1)) * i;
                Location loc = center.clone().add(-22, 6, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGENTA_STAINED_GLASS);
                h.scale(0.05f, 0.05f, 3.0f).glow(180, 0, 200).interpolation(3, 0);
                westFilaments.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Brightness wave: scale X/Y pulse from 0.05 to 0.12, staggered 3.4 ticks per block
            for (int i = 0; i < FILAMENT_COUNT; i++) {
                float phase = (float) ((ticksAlive - i * 3.4) * Math.PI / 20);
                float pulse = 0.05f + 0.035f * Math.max(0, (float) Math.sin(phase));

                eastFilaments.get(i).scale(pulse, pulse, 3.0f);
                eastFilaments.get(i).interpolation(3, 0);
                westFilaments.get(i).scale(pulse, pulse, 3.0f);
                westFilaments.get(i).interpolation(3, 0);
            }

            // Particles along filaments
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < FILAMENT_COUNT; i += 3) {
                    // East: END_ROD
                    center.getWorld().spawnParticle(Particle.END_ROD,
                            eastFilaments.get(i).entity().getLocation(), 3, 0.1, 0.1, 1.0, 0.01);
                    // West: SPELL_WITCH
                    center.getWorld().spawnParticle(Particle.WITCH,
                            westFilaments.get(i).entity().getLocation(), 3, 0.1, 0.1, 1.0, 0.01);
                }
            }

            // Note block bit sound on wave cycle completion
            // Wave traversal = 2 + 12*0.17 = ~4 sec = 80 ticks
            if (ticksAlive % 80 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center.clone().add(22, 6, 0),
                        Sound.BLOCK_NOTE_BLOCK_BIT, 0.2f, 1.0f);
                DisplayBuilder.playSound(center.clone().add(-22, 6, 0),
                        Sound.BLOCK_NOTE_BLOCK_BIT, 0.2f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftConnectorFilaments(plugin); }
    }

    // ================================================================
    // #38 -- RIFT COLLAPSE ARRAY (Phase 3 Emergency Tears)
    // 6 randomly positioned small rift fragments that rip open on spawn.
    // Unstable wobble, mixed magenta-purple glow particles.
    // ================================================================
    public static class RiftCollapseArray extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tearFrames = new ArrayList<>();
        private final List<BlockDisplayHandle> tearMembranes = new ArrayList<>();
        private final List<Location> tearPositions = new ArrayList<>();

        public RiftCollapseArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_collapse_array", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 6 random positions within 18-block radius
            for (int i = 0; i < 6; i++) {
                double angle = Math.random() * Math.PI * 2;
                double radius = 5.0 + Math.random() * 13.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.random() * 6;
                Location tearLoc = center.clone().add(x, y + 1, z);
                tearPositions.add(tearLoc);

                // Crying obsidian frame element (thin vertical)
                BlockDisplayHandle frame = displayBuilder.spawnBlock(tearLoc, Material.CRYING_OBSIDIAN);
                frame.scale(0.3f, 3.0f, 0.3f).glow(180, 0, 200).interpolation(3, 0);
                tearFrames.add(frame);
                spawnedEntities.add(frame.entity());

                // Small magenta membrane
                BlockDisplayHandle membrane = displayBuilder.spawnBlock(tearLoc.clone().add(0.2, 0, 0),
                        Material.MAGENTA_STAINED_GLASS);
                membrane.scale(2.0f, 3.0f, 0.04f).glow(180, 0, 200).interpolation(3, 0);
                tearMembranes.add(membrane);
                spawnedEntities.add(membrane.entity());

                // Spawn burst: mixed particles
                boolean isMagenta = Math.random() < 0.5;
                if (isMagenta) {
                    w.spawnParticle(Particle.WITCH, tearLoc, 40, 1.5, 1.5, 1.5, 0.02);
                } else {
                    w.spawnParticle(Particle.END_ROD, tearLoc, 40, 1.5, 1.5, 1.5, 0.02);
                }

                // Lightning impact sound
                DisplayBuilder.playSound(tearLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 1.5f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Unstable wobble: +/-2 deg on fast 1-sec (20-tick) cycle per tear
            for (int i = 0; i < tearFrames.size(); i++) {
                float wobble = (float) Math.toRadians(2) *
                        (float) Math.sin(ticksAlive * Math.PI / 10 + i * 0.7);
                tearFrames.get(i).rotate(wobble, 0, 1, 0);
                tearFrames.get(i).interpolation(3, 0);
                tearMembranes.get(i).rotate(wobble, 0, 1, 0);
                tearMembranes.get(i).interpolation(3, 0);
            }

            // SPELL_WITCH + DRAGON_BREATH from each tear
            if (ticksAlive % 3 == 0) {
                for (Location tearLoc : tearPositions) {
                    center.getWorld().spawnParticle(Particle.WITCH, tearLoc, 4,
                            0.5, 0.5, 0.5, 0.01);
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH, tearLoc, 4,
                            0.5, 0.5, 0.5, 0);
                }
            }

            // Ambient portal sound per tear (very low volume, staggered)
            for (int i = 0; i < tearPositions.size(); i++) {
                if ((ticksAlive + i * 30) % 160 == 0) {
                    DisplayBuilder.playSound(tearPositions.get(i),
                            Sound.BLOCK_PORTAL_AMBIENT, 0.15f, 1.3f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftCollapseArray(plugin); }
    }
}
