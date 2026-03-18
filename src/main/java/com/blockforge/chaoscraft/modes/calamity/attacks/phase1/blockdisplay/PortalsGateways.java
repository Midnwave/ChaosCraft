package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 Block Display — GROUP 7: PORTALS AND GATEWAYS
 * 10 portal/gateway structures: arches, rift tears, shrines,
 * dimensional doorways, and thresholds between worlds.
 * Adapted from boss1-voidmaw.md with Calamity attack rules applied:
 * - NO status effects (Blindness, Slowness, Nausea, etc. removed -> damage only)
 * - Always spawn straight (yaw=0, pitch=0)
 * - Calamity particle palette (purple, cyan, crimson)
 * - All values configurable via AttackConfig
 */
public final class PortalsGateways {

    private PortalsGateways() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidArch(plugin));
        registry.register(new RiftTear(plugin));
        registry.register(new ShrineOfDescent(plugin));
        registry.register(new DoorwayOfUnmaking(plugin));
        registry.register(new RuinedGateway(plugin));
        registry.register(new EyeOfTheAbyssPortal(plugin));
        registry.register(new ShatteredMirrorGate(plugin));
        registry.register(new VoidlingThreshold(plugin));
        registry.register(new GateBetweenWorlds(plugin));
        registry.register(new NullThreshold(plugin));
    }

    // ================================================================
    // 61. THE VOID ARCH — Classic portal arch with shimmering glass veil
    // ================================================================
    public static class VoidArch extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> keystoneBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> veilPanes = new ArrayList<>();
        private BlockDisplayHandle amethystKeystone;

        public VoidArch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_arch", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left pillar: 4 obsidian blocks rising vertically
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(-2, i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 0.8f).glow(80, 0, 160).interpolation(3, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Right pillar: 4 obsidian blocks
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(2, i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 0.8f).glow(80, 0, 160).interpolation(3, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Left pillar capitals: polished blackstone
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-2 + i, 4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.6f, 0.8f).glow(60, 60, 80).interpolation(3, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Keystone arch: crying obsidian curved span
            double[] archAngles = {-1.5, -1.0, -0.5, 0, 0.5, 1.0, 1.5};
            for (int i = 0; i < archAngles.length; i++) {
                double x = archAngles[i];
                double y = 4.5 + Math.cos((x / 1.5) * Math.PI * 0.5) * 1.5;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.7f, 0.6f, 0.7f).glow(128, 0, 255).interpolation(3, 0);
                keystoneBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Amethyst keystone at the apex
            amethystKeystone = displayBuilder.spawnBlock(center.clone().add(0, 6.2, 0), Material.AMETHYST_BLOCK);
            amethystKeystone.scale(0.8f, 0.8f, 0.8f).glow(200, 100, 255).interpolation(3, 0);
            spawnedEntities.add(amethystKeystone.entity());

            // Black concrete base plinths
            double[][] basePlinths = {{-2.5, 0, 0}, {-1.5, 0, 0}, {2.5, 0, 0}, {1.5, 0, 0}};
            for (double[] pos : basePlinths) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(pos[0], pos[1], pos[2]),
                        Material.BLACK_CONCRETE);
                h.scale(0.5f, 0.3f, 0.6f).glow(30, 0, 50).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // 3 purple glass veil panes in the arch opening
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-0.5 + i * 0.5, 2 + i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.6f, 1.8f, 0.1f).glow(160, 0, 200).interpolation(3, 0);
                veilPanes.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.6f);
            DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 40, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Veil panes ripple: Z-axis oscillation with phase offsets
            for (int i = 0; i < veilPanes.size(); i++) {
                float offset = (float) Math.sin((ticksAlive + i * 15) * 0.42f) * 0.1f;
                veilPanes.get(i).translate(-0.3f + i * 0.25f, 0.9f + i * 0.3f, offset);
                veilPanes.get(i).interpolation(3, 0);
            }

            // Amethyst keystone pulse: scale 1.0 -> 1.4, 60-tick cycle
            float keystoneScale = 0.8f + (float) Math.sin(ticksAlive * 0.1047f) * 0.24f;
            amethystKeystone.scale(keystoneScale, keystoneScale, keystoneScale);
            amethystKeystone.interpolation(3, 0);

            // Slow Y-axis rotation of the whole structure: 0.2 deg/tick
            float rot = ticksAlive * 0.00349f;
            for (BlockDisplayHandle h : pillarBlocks) {
                h.rotate(rot, 0, 1, 0);
                h.interpolation(5, 0);
            }
            for (BlockDisplayHandle h : keystoneBlocks) {
                h.rotate(rot, 0, 1, 0);
                h.interpolation(5, 0);
            }

            // Portal particles flooding through the arch
            if (ticksAlive % 2 == 0) {
                for (int p = 0; p < 5; p++) {
                    double px = (Math.random() - 0.5) * 2.5;
                    double py = 1 + Math.random() * 4;
                    c.getWorld().spawnParticle(Particle.PORTAL, c.clone().add(px, py, 0), 2, 0.1, 0.1, 0.1, 0.5);
                }
            }

            // Purple dust tracing arch curve
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.purpleDust(c.clone().add(0, 5.5, 0), 4, 1.5);
            }

            // Reverse portal burst from keystone every 40 ticks
            if (ticksAlive % 40 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 6.2, 0), 12, 0.3, 0.3, 0.3, 0.05);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 0.4f);
            }

            // End rod shimmer along pillars
            if (ticksAlive % 6 == 0) {
                double side = (ticksAlive % 12 == 0) ? -2 : 2;
                double py = (ticksAlive % 80) / 20.0;
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(side, py, 0), 2, 0.1, 0.3, 0.1, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidArch(plugin); }
    }

    // ================================================================
    // 62. THE RIFT TEAR — Jagged vertical tear that breathes open/closed
    // ================================================================
    public static class RiftTear extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftEdge = new ArrayList<>();
        private final List<BlockDisplayHandle> rightEdge = new ArrayList<>();
        private final List<BlockDisplayHandle> interiorGlass = new ArrayList<>();
        private BlockDisplayHandle topAnchor;
        private BlockDisplayHandle bottomAnchor;

        public RiftTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_tear", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(12.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left jagged edge: obsidian at irregular heights
            double[] leftXOffsets = {-0.3, -0.5, -0.8, -0.6, -0.9, -0.4, -0.7, -0.3, -0.6, -0.4};
            for (int i = 0; i < leftXOffsets.length; i++) {
                Location loc = center.clone().add(leftXOffsets[i], i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.4f, 0.6f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
                leftEdge.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right jagged edge: blackstone at irregular heights
            double[] rightXOffsets = {0.3, 0.6, 0.9, 0.5, 0.8, 0.4, 0.7, 0.5, 0.6, 0.3};
            for (int i = 0; i < rightXOffsets.length; i++) {
                Location loc = center.clone().add(rightXOffsets[i], i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.4f, 0.6f, 0.5f).glow(60, 0, 120).interpolation(3, 0);
                rightEdge.add(h);
                spawnedEntities.add(h.entity());
            }

            // Interior lining: crying obsidian where edges curl back
            double[] innerY = {1.2, 2.4, 3.6, 4.8, 1.8, 3.0};
            for (double y : innerY) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, y, 0.15),
                        Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // Void-mouth lining: dark prismarine at widest midpoint
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add((i % 2 == 0 ? -0.15 : 0.15), 2.5 + i * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.35f, 0.4f, 0.3f).glow(0, 150, 180).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // Top and bottom netherite anchors
            topAnchor = displayBuilder.spawnBlock(center.clone().add(0, 6, 0), Material.NETHERITE_BLOCK);
            topAnchor.scale(0.5f, 0.5f, 0.5f).glow(30, 0, 50).interpolation(3, 0);
            spawnedEntities.add(topAnchor.entity());

            bottomAnchor = displayBuilder.spawnBlock(center.clone().add(0, -0.3, 0), Material.NETHERITE_BLOCK);
            bottomAnchor.scale(0.5f, 0.5f, 0.5f).glow(30, 0, 50).interpolation(3, 0);
            spawnedEntities.add(bottomAnchor.entity());

            // Magenta glass interior fill
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 1.2 + i * 1.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGENTA_STAINED_GLASS);
                h.scale(0.5f, 0.9f, 0.1f).glow(200, 0, 160).interpolation(3, 0);
                interiorGlass.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 0.7f, 0.5f);
            DisplayBuilder.crimsonDust(center.clone().add(0, 3, 0), 30, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Breathing animation: left/right edges move apart and together over 80 ticks
            float breathe = (float) Math.sin(ticksAlive * 0.0785f) * 0.4f; // 80-tick cycle
            float breatheOffset = 0.2f + breathe;

            for (int i = 0; i < leftEdge.size(); i++) {
                double baseX = -0.3 - (i % 3) * 0.2;
                leftEdge.get(i).entity().teleport(c.clone().add(baseX - breatheOffset, i * 0.6, 0));
            }
            for (int i = 0; i < rightEdge.size(); i++) {
                double baseX = 0.3 + (i % 3) * 0.2;
                rightEdge.get(i).entity().teleport(c.clone().add(baseX + breatheOffset, i * 0.6, 0));
            }

            // Interior glass scales to match tear width
            float glassScaleX = 0.5f + breathe * 0.8f;
            for (BlockDisplayHandle h : interiorGlass) {
                h.scale(glassScaleX, 0.9f, 0.1f);
                h.interpolation(3, 0);
            }

            // Netherite anchors counter-oscillate vertically
            float anchorBob = (float) Math.sin(ticksAlive * 0.0785f) * 0.15f;
            topAnchor.entity().teleport(c.clone().add(0, 6 + anchorBob, 0));
            bottomAnchor.entity().teleport(c.clone().add(0, -0.3 - anchorBob, 0));

            // Reverse portal erupting from glass interior
            if (ticksAlive % 2 == 0) {
                for (int p = 0; p < 6; p++) {
                    double py = 0.5 + Math.random() * 5;
                    c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(
                            (Math.random() - 0.5) * 0.6, py, (Math.random() - 0.5) * 0.3
                    ), 2, 0, 0, 0, 0.02);
                }
            }

            // Crimson dust bleeding from edges
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.crimsonDust(c.clone().add(-breatheOffset - 0.5, 3, 0), 3, 0.3);
                DisplayBuilder.crimsonDust(c.clone().add(breatheOffset + 0.5, 3, 0), 3, 0.3);
            }

            // Falling obsidian tears from top anchor
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, c.clone().add(0, 6, 0),
                        3, 0.1, 0.2, 0.1, 0);
            }

            // Sound: enderman scream on full tear-open, deepslate break on close
            if (ticksAlive % 80 == 20) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_SCREAM, 0.5f, 0.5f);
            }
            if (ticksAlive % 80 == 60) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.8f);
            }

            // End rod particles at the rift seam
            if (ticksAlive % 5 == 0) {
                double ey = Math.random() * 5.5;
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, ey, 0), 2, 0.05, 0.1, 0.05, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftTear(plugin); }
    }

    // ================================================================
    // 63. THE SHRINE OF DESCENT — Small niche shrine with rotating relic
    // ================================================================
    public static class ShrineOfDescent extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> sideWalls = new ArrayList<>();
        private BlockDisplayHandle backWall;
        private BlockDisplayHandle floatingRelic;
        private final List<BlockDisplayHandle> offerings = new ArrayList<>();
        private final List<BlockDisplayHandle> runeStones = new ArrayList<>();

        public ShrineOfDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shrine_of_descent", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left side wall: 6 blackstone blocks (3 wide x 2 tall)
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 2; y++) {
                    Location loc = center.clone().add(-1.5, y, -0.5 + x * 0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.5f, 1.0f, 0.5f).glow(50, 50, 60).interpolation(3, 0);
                    sideWalls.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Right side wall
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 2; y++) {
                    Location loc = center.clone().add(1.5, y, -0.5 + x * 0.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.5f, 1.0f, 0.5f).glow(50, 50, 60).interpolation(3, 0);
                    sideWalls.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Back wall + floor: polished blackstone
            backWall = displayBuilder.spawnBlock(center.clone().add(0, 0.5, -1), Material.POLISHED_BLACKSTONE);
            backWall.scale(3.0f, 2.5f, 0.5f).glow(60, 60, 70).interpolation(3, 0);
            spawnedEntities.add(backWall.entity());

            // Corner obsidian pillars
            double[][] corners = {{-1.5, 0, 1}, {1.5, 0, 1}, {-1.5, 0, -1}, {1.5, 0, -1}};
            for (double[] pos : corners) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(pos[0], pos[1], pos[2]),
                        Material.OBSIDIAN);
                h.scale(0.4f, 2.5f, 0.4f).glow(80, 0, 160).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // Back wall decorative panels: glazed terracotta
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-0.8 + i * 0.8, 0.8, -0.7), Material.BLACK_GLAZED_TERRACOTTA);
                h.scale(0.6f, 0.6f, 0.2f).glow(40, 40, 50).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // Crying obsidian offerings on floor
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-0.5 + i, 0.0, 0.3), Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                offerings.add(h);
                spawnedEntities.add(h.entity());
            }

            // Floating amethyst relic
            floatingRelic = displayBuilder.spawnBlock(center.clone().add(0, 1.5, 0), Material.AMETHYST_BLOCK);
            floatingRelic.scale(0.5f, 0.5f, 0.5f).glow(200, 100, 255).interpolation(3, 0);
            spawnedEntities.add(floatingRelic.entity());

            // Upper lintel: dark prismarine
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-1 + i, 2.3, 0), Material.DARK_PRISMARINE);
                h.scale(1.0f, 0.3f, 1.0f).glow(0, 150, 180).interpolation(3, 0);
                runeStones.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Relic rotation: all 3 axes at 1 degree/tick each
            float relicRot = ticksAlive * 0.01745f;
            floatingRelic.rotate(relicRot, 1, 1, 1);
            floatingRelic.interpolation(3, 0);

            // Offerings slowly sink: 0.0 -> -0.3 over 200 ticks, snap back
            float sinkProgress = (ticksAlive % 200) / 200.0f;
            float sinkY = -sinkProgress * 0.3f;
            for (int i = 0; i < offerings.size(); i++) {
                offerings.get(i).entity().teleport(c.clone().add(-0.5 + i, sinkY, 0.3));
            }

            // Shrine tilts ±3 degrees on X-axis over 150 ticks
            float shrineRot = (float) Math.sin(ticksAlive * 0.0419f) * 0.0524f;
            backWall.rotate(shrineRot, 1, 0, 0);
            backWall.interpolation(5, 0);

            // Indigo dust from amethyst relic
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 4, 0.5, 60, 0, 160, 1.0f);
            }

            // Dripping tears from offerings
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle h : offerings) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            h.entity().getLocation().add(0, 0.2, 0), 2, 0.1, 0.1, 0.1, 0);
                }
            }

            // Ash drifting through shrine interior
            if (ticksAlive % 8 == 0) {
                c.getWorld().spawnParticle(Particle.ASH, c.clone().add(0, 2.0, 0), 4, 0.8, 0.5, 0.8, 0);
            }

            // End rod flickers from relic
            if (ticksAlive % 10 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 1.5, 0), 3, 0.2, 0.2, 0.2, 0.02);
            }

            // Shriek sound every 6 seconds
            if (ticksAlive % 120 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShrineOfDescent(plugin); }
    }

    // ================================================================
    // 64. THE DOORWAY OF UNMAKING — Huge rectangular portal doorframe
    // ================================================================
    public static class DoorwayOfUnmaking extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> netheriteFacing = new ArrayList<>();
        private final List<BlockDisplayHandle> membraneGlass = new ArrayList<>();
        private final List<BlockDisplayHandle> cornerCaps = new ArrayList<>();
        private float breatheProgress = 0;

        public DoorwayOfUnmaking(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("doorway_of_unmaking", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(16.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(440);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left doorpost: 5 obsidian blocks tall, 2 blocks thick
            for (int y = 0; y < 5; y++) {
                for (int d = 0; d < 2; d++) {
                    Location loc = center.clone().add(-1.5, y, d * 0.5 - 0.25);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.8f, 1.0f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
                    frameBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Right doorpost
            for (int y = 0; y < 5; y++) {
                for (int d = 0; d < 2; d++) {
                    Location loc = center.clone().add(1.5, y, d * 0.5 - 0.25);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.8f, 1.0f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
                    frameBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Top lintel: 3 blocks wide
            for (int x = -1; x <= 1; x++) {
                Location loc = center.clone().add(x, 5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f, 0.8f, 0.8f).glow(80, 0, 160).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Netherite reinforcing facing on outer edge
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle hL = displayBuilder.spawnBlock(center.clone().add(-2.0, y, 0), Material.NETHERITE_BLOCK);
                hL.scale(0.3f, 1.0f, 0.8f).glow(20, 20, 30).interpolation(3, 0);
                netheriteFacing.add(hL);
                spawnedEntities.add(hL.entity());

                BlockDisplayHandle hR = displayBuilder.spawnBlock(center.clone().add(2.0, y, 0), Material.NETHERITE_BLOCK);
                hR.scale(0.3f, 1.0f, 0.8f).glow(20, 20, 30).interpolation(3, 0);
                netheriteFacing.add(hR);
                spawnedEntities.add(hR.entity());
            }

            // Crying obsidian inner corners
            double[][] cryPos = {{-1.2, 0, 0}, {1.2, 0, 0}, {-1.2, 4.8, 0}, {1.2, 4.8, 0},
                    {-1.0, 2.5, 0}, {1.0, 2.5, 0}};
            for (double[] pos : cryPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(pos[0], pos[1], pos[2]),
                        Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // 4 purple glass membrane panes in 2x2 arrangement
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    Location loc = center.clone().add(-0.5 + x, 1 + y * 1.8, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                    h.scale(1.0f, 1.8f, 0.05f).glow(160, 0, 200).interpolation(3, 0);
                    membraneGlass.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Corner cap blocks: black concrete
            double[][] capPos = {{-1.5, 5, 0}, {1.5, 5, 0}, {-1.5, 5, 0.5}, {1.5, 5, 0.5}};
            for (double[] pos : capPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(pos[0], pos[1], pos[2]),
                        Material.BLACK_CONCRETE);
                h.scale(0.6f, 0.4f, 0.4f).glow(30, 0, 50).interpolation(3, 0);
                cornerCaps.add(h);
                spawnedEntities.add(h.entity());
            }

            // Threshold stones
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(-0.5 + i, -0.1, 0),
                        Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 0.2f, 0.8f).glow(60, 60, 70).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.4f);
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 2.5, 0), 35, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Doorframe breathing: scale 1.0 -> 1.3 over 100 ticks
            breatheProgress = (float) Math.sin(ticksAlive * 0.0628f) * 0.15f;
            float frameScale = 1.0f + breatheProgress;
            for (BlockDisplayHandle h : frameBlocks) {
                Transformation t = h.entity().getTransformation();
                Vector3f origScale = t.getScale();
                h.scale(origScale.x * frameScale / (1.0f + (float) Math.sin((ticksAlive - 1) * 0.0628f) * 0.15f),
                        origScale.y, origScale.z);
                h.interpolation(5, 0);
            }

            // Membrane glass shimmer: independent scale oscillation
            for (int i = 0; i < membraneGlass.size(); i++) {
                float shimmer = 1.0f + (float) Math.sin((ticksAlive + i * 17) * 0.15f) * 0.05f;
                membraneGlass.get(i).scale(shimmer, 1.8f * shimmer, 0.05f);
                membraneGlass.get(i).interpolation(3, 0);
            }

            // Netherite facing vibrates perpendicular to frame
            for (BlockDisplayHandle h : netheriteFacing) {
                float vibrate = ((ticksAlive % 3 == 0) ? 0.05f : -0.05f);
                h.translate(-0.15f, -0.5f, vibrate);
                h.interpolation(2, 0);
            }

            // Portal particles flooding through membrane
            if (ticksAlive % 2 == 0) {
                for (int p = 0; p < 10; p++) {
                    double px = (Math.random() - 0.5) * 2;
                    double py = Math.random() * 4 + 0.5;
                    c.getWorld().spawnParticle(Particle.PORTAL, c.clone().add(px, py, 0), 1, 0, 0, 0, 0.3);
                }
            }

            // White-violet dust bleeding from frame edges
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(-1.5, 2.5, 0), 3, 0.3, 240, 230, 255, 1.0f);
                DisplayBuilder.dustParticles(c.clone().add(1.5, 2.5, 0), 3, 0.3, 240, 230, 255, 1.0f);
            }

            // Reverse portal from threshold stones
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 0.1, 0), 3, 0.5, 0.1, 0.3, 0.01);
            }

            // End rod particles at membrane intersections
            if (ticksAlive % 8 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 2.5, 0), 4, 0.8, 1.5, 0.1, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoorwayOfUnmaking(plugin); }
    }

    // ================================================================
    // 65. THE RUINED GATEWAY — Collapsed arch slowly restoring itself
    // ================================================================
    public static class RuinedGateway extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftPillar = new ArrayList<>();
        private final List<BlockDisplayHandle> rightPillar = new ArrayList<>();
        private final List<BlockDisplayHandle> rubble = new ArrayList<>();
        private BlockDisplayHandle brokenKeystone;
        private BlockDisplayHandle crackedAmethyst;
        private final List<BlockDisplayHandle> archSpan = new ArrayList<>();

        public RuinedGateway(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ruined_gateway", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left pillar: intact at 4 blocks (cobbled deepslate)
            for (int y = 0; y < 4; y++) {
                Location loc = center.clone().add(-1.5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.COBBLED_DEEPSLATE);
                h.scale(0.8f, 1.0f, 0.8f).glow(50, 40, 60).interpolation(3, 0);
                leftPillar.add(h);
                spawnedEntities.add(h.entity());
            }
            // Right pillar: reduced to 2 blocks (partially collapsed)
            for (int y = 0; y < 2; y++) {
                Location loc = center.clone().add(1.5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.COBBLED_DEEPSLATE);
                h.scale(0.8f, 1.0f, 0.8f).glow(50, 40, 60).interpolation(3, 0);
                rightPillar.add(h);
                spawnedEntities.add(h.entity());
            }

            // Obsidian foundation courses
            for (int i = 0; i < 4; i++) {
                double x = (i < 2) ? -1.5 : 1.5;
                double z = (i % 2 == 0) ? -0.3 : 0.3;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, -0.2, z), Material.OBSIDIAN);
                h.scale(0.5f, 0.3f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // Dark prismarine surviving arch span segments
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-1 + i * 0.8, 4.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.7f, 0.4f, 0.6f).glow(0, 150, 180).interpolation(3, 0);
                archSpan.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fallen rubble at random orientations: blackstone
            double[][] rubblePos = {{0.5, 0.1, 0.8}, {1.0, 0.2, -0.5}, {-0.3, 0.0, 1.0},
                    {0.8, 0.15, 0.3}, {-0.7, 0.1, -0.7}, {1.3, 0.0, 0.5}};
            float[] rubbleAngles = {0.5f, 1.0f, 0.3f, 0.8f, 1.2f, 0.6f};
            for (int i = 0; i < rubblePos.length; i++) {
                Location loc = center.clone().add(rubblePos[i][0], rubblePos[i][1], rubblePos[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.5f, 0.4f, 0.5f).rotate(rubbleAngles[i], 0.5f, 0.3f, 0.7f)
                        .glow(40, 40, 50).interpolation(3, 0);
                rubble.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crying obsidian embedded in rubble
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0.3 + i * 0.8, 0.1, 0.5), Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // Broken keystone tilted at an angle
            brokenKeystone = displayBuilder.spawnBlock(center.clone().add(0.5, 0.3, 0.6), Material.COBBLED_DEEPSLATE);
            brokenKeystone.scale(0.8f, 0.5f, 0.6f).rotate(0.7f, 0.3f, 0.5f, 0.2f).glow(50, 40, 60).interpolation(3, 0);
            spawnedEntities.add(brokenKeystone.entity());

            // Cracked amethyst at keystone center
            crackedAmethyst = displayBuilder.spawnBlock(center.clone().add(0.5, 0.6, 0.6), Material.AMETHYST_BLOCK);
            crackedAmethyst.scale(0.3f, 0.3f, 0.3f).glow(200, 100, 255).interpolation(3, 0);
            spawnedEntities.add(crackedAmethyst.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_FALL, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rubble slowly rotating back toward upright: 0.01 deg/tick
            for (int i = 0; i < rubble.size(); i++) {
                float currentAngle = 0.5f + (i % 3) * 0.3f;
                float restoration = currentAngle - (ticksAlive * 0.000175f);
                rubble.get(i).rotate(restoration, 0.5f, 0.3f, 0.7f);
                rubble.get(i).interpolation(5, 0);
            }

            // Cracked amethyst pulse: scale 0.3 -> 0.55 over 50 ticks
            float amethystScale = 0.3f + (float) Math.sin(ticksAlive * 0.1257f) * 0.12f;
            crackedAmethyst.scale(amethystScale, amethystScale, amethystScale);
            crackedAmethyst.interpolation(3, 0);

            // Whole ruin translates downward, snaps back every 300 ticks (sinking)
            int sinkCycle = ticksAlive % 300;
            float sinkY = -sinkCycle * 0.002f;
            // Apply to keystone only to show the sinking
            brokenKeystone.entity().teleport(c.clone().add(0.5, 0.3 + sinkY, 0.6));

            // Falling obsidian tears from broken arch edges
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle h : archSpan) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            h.entity().getLocation().add(0, 0.2, 0), 2, 0.2, 0.1, 0.2, 0);
                }
            }

            // Ancient grey dust rising from rubble
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0.5, 0.5, 0.5), 3, 0.8, 50, 40, 60, 0.8f);
            }

            // End rod flickers from cracked amethyst
            if (ticksAlive % 25 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0.5, 0.8, 0.6), 2, 0.1, 0.1, 0.1, 0.02);
            }

            // Reverse portal seeping from rubble gaps
            if (ticksAlive % 8 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 0.3, 0), 4, 1.0, 0.2, 1.0, 0.01);
            }

            // Ambient cave sound
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_FALL, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RuinedGateway(plugin); }
    }

    // ================================================================
    // 66. THE EYE OF THE ABYSS PORTAL — Rolling oval eye-portal
    // ================================================================
    public static class EyeOfTheAbyssPortal extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ovalFrame = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRim = new ArrayList<>();
        private final List<BlockDisplayHandle> upperIris = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerIris = new ArrayList<>();
        private BlockDisplayHandle pupilBlock;

        public EyeOfTheAbyssPortal(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eye_of_the_abyss_portal", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(12.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Oval frame: obsidian blocks in an eye shape (6 wide, 4 tall at center)
            int frameCount = 16;
            for (int i = 0; i < frameCount; i++) {
                double angle = (2 * Math.PI * i) / frameCount;
                double x = Math.cos(angle) * 3.0;
                double y = Math.sin(angle) * 2.0 + 3.0;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.6f, 0.6f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
                ovalFrame.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner rim: crying obsidian
            int rimCount = 12;
            for (int i = 0; i < rimCount; i++) {
                double angle = (2 * Math.PI * i) / rimCount;
                double x = Math.cos(angle) * 2.2;
                double y = Math.sin(angle) * 1.4 + 3.0;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                innerRim.add(h);
                spawnedEntities.add(h.entity());
            }

            // Outer frame accents: dark prismarine at widest points
            double[][] accentPos = {{3.2, 3, 0}, {-3.2, 3, 0}, {2.5, 4.8, 0}, {-2.5, 4.8, 0},
                    {2.5, 1.2, 0}, {-2.5, 1.2, 0}};
            for (double[] pos : accentPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(pos[0], pos[1], pos[2]),
                        Material.DARK_PRISMARINE);
                h.scale(0.4f, 0.4f, 0.4f).glow(0, 150, 180).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // Decorative corner insets: black glazed terracotta
            double[][] cornerPos = {{2.8, 4.2, 0}, {-2.8, 4.2, 0}, {2.8, 1.8, 0}, {-2.8, 1.8, 0}};
            for (double[] pos : cornerPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(pos[0], pos[1], pos[2]),
                        Material.BLACK_GLAZED_TERRACOTTA);
                h.scale(0.35f, 0.35f, 0.35f).glow(40, 40, 50).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // Upper iris: purple stained glass (upper half of eye)
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-0.8 + i * 0.8, 3.5 + i * 0.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.8f, 0.6f, 0.1f).glow(160, 0, 200).interpolation(3, 0);
                upperIris.add(h);
                spawnedEntities.add(h.entity());
            }
            // Lower iris: magenta stained glass
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-0.8 + i * 0.8, 2.3 + i * 0.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGENTA_STAINED_GLASS);
                h.scale(0.8f, 0.6f, 0.1f).glow(200, 0, 160).interpolation(3, 0);
                lowerIris.add(h);
                spawnedEntities.add(h.entity());
            }

            // Netherite pupil at center
            pupilBlock = displayBuilder.spawnBlock(center.clone().add(0, 3, 0), Material.NETHERITE_BLOCK);
            pupilBlock.scale(0.8f, 0.8f, 0.5f).glow(20, 20, 30).interpolation(3, 0);
            spawnedEntities.add(pupilBlock.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.5f);
            DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 30, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Oval frame rolls on Z-axis: 0.3 deg/tick
            float frameRot = ticksAlive * 0.00524f;
            for (BlockDisplayHandle h : ovalFrame) {
                h.rotate(frameRot, 0, 0, 1);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : innerRim) {
                h.rotate(frameRot, 0, 0, 1);
                h.interpolation(3, 0);
            }

            // Upper/lower iris tilt: ±5 degrees on Z with 30-tick phase
            float irisTilt = (float) Math.sin(ticksAlive * 0.209f) * 0.0873f;
            for (BlockDisplayHandle h : upperIris) {
                h.rotate(irisTilt, 0, 0, 1);
                h.interpolation(3, 0);
            }
            for (BlockDisplayHandle h : lowerIris) {
                h.rotate(-irisTilt, 0, 0, 1);
                h.interpolation(3, 0);
            }

            // Netherite pupil blink: scale 0.5 -> 1.5 over 60 ticks
            float pupilScale = 0.8f + (float) Math.sin(ticksAlive * 0.1047f) * 0.5f;
            pupilBlock.scale(pupilScale, pupilScale, 0.5f);
            pupilBlock.interpolation(3, 0);

            // Reverse portal pouring from iris
            if (ticksAlive % 2 == 0) {
                for (int p = 0; p < 8; p++) {
                    double px = (Math.random() - 0.5) * 2;
                    double py = 2.5 + Math.random() * 1.5;
                    c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(px, py, 0), 2, 0, 0, 0, 0.03);
                }
            }

            // Purple dust tracing oval frame
            if (ticksAlive % 4 == 0) {
                int idx = (ticksAlive / 4) % ovalFrame.size();
                Location frameLoc = ovalFrame.get(idx).entity().getLocation();
                DisplayBuilder.dustParticles(frameLoc, 4, 0.2, 120, 0, 180, 1.0f);
            }

            // Falling tears from pupil — the eye weeps
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                        c.clone().add(0, 3, 0), 2, 0.1, 0.3, 0.1, 0);
            }

            // End rod particles orbiting the eye
            if (ticksAlive % 5 == 0) {
                double angle = ticksAlive * 0.1;
                double ex = Math.cos(angle) * 3.5;
                double ey = Math.sin(angle) * 2.5 + 3;
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(ex, ey, 0), 2, 0, 0, 0, 0.01);
            }

            // Enderman stare sound loop
            if (ticksAlive % 100 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_STARE, 0.7f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EyeOfTheAbyssPortal(plugin); }
    }

    // ================================================================
    // 67. THE SHATTERED MIRROR GATE — Spinning broken glass shards
    // ================================================================
    public static class ShatteredMirrorGate extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerFrame = new ArrayList<>();
        private final List<BlockDisplayHandle> innerFrame = new ArrayList<>();
        private final List<BlockDisplayHandle> mirrorShards = new ArrayList<>();
        private final List<BlockDisplayHandle> amethystInclusions = new ArrayList<>();
        private final float[] shardSpeeds = {0.035f, 0.05f, 0.07f, 0.04f, 0.088f, 0.06f, 0.045f,
                0.08f, 0.055f, 0.065f};
        private final float[] shardTiltX = {0.3f, 0.7f, 0.1f, 0.5f, 0.9f, 0.2f, 0.6f, 0.4f, 0.8f, 0.15f};
        private final float[] shardTiltZ = {0.6f, 0.2f, 0.8f, 0.4f, 0.1f, 0.7f, 0.3f, 0.9f, 0.5f, 0.65f};

        public ShatteredMirrorGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shattered_mirror_gate", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer frame: 4x4 black concrete square frame (hollow center)
            // Top row
            for (int x = 0; x < 4; x++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(-1.5 + x, 4, 0), Material.BLACK_CONCRETE);
                h.scale(1.0f, 0.8f, 0.5f).glow(30, 30, 40).interpolation(3, 0);
                outerFrame.add(h);
                spawnedEntities.add(h.entity());
            }
            // Bottom row
            for (int x = 0; x < 4; x++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(-1.5 + x, 0, 0), Material.BLACK_CONCRETE);
                h.scale(1.0f, 0.8f, 0.5f).glow(30, 30, 40).interpolation(3, 0);
                outerFrame.add(h);
                spawnedEntities.add(h.entity());
            }
            // Left/right columns
            for (int y = 1; y < 4; y++) {
                BlockDisplayHandle hL = displayBuilder.spawnBlock(center.clone().add(-1.5, y, 0), Material.BLACK_CONCRETE);
                hL.scale(0.8f, 1.0f, 0.5f).glow(30, 30, 40).interpolation(3, 0);
                outerFrame.add(hL);
                spawnedEntities.add(hL.entity());

                BlockDisplayHandle hR = displayBuilder.spawnBlock(center.clone().add(2.5, y, 0), Material.BLACK_CONCRETE);
                hR.scale(0.8f, 1.0f, 0.5f).glow(30, 30, 40).interpolation(3, 0);
                outerFrame.add(hR);
                spawnedEntities.add(hR.entity());
            }

            // Inner frame lining: polished blackstone
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 1.2;
                double y = Math.sin(angle) * 1.2 + 2;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, y, 0),
                        Material.POLISHED_BLACKSTONE);
                h.scale(0.4f, 0.4f, 0.3f).glow(60, 60, 70).interpolation(3, 0);
                innerFrame.add(h);
                spawnedEntities.add(h.entity());
            }

            // Corner obsidian blocks
            double[][] obsCorners = {{-1.5, 0, 0}, {2.5, 0, 0}, {-1.5, 4, 0}, {2.5, 4, 0}};
            for (double[] pos : obsCorners) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(pos[0], pos[1], pos[2]),
                        Material.OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // 10 mirror fragment shards: glazed terracotta at various sizes/angles
            float[][] shardScales = {{0.8f, 0.8f}, {0.6f, 0.7f}, {0.4f, 0.5f}, {0.7f, 0.9f},
                    {0.5f, 0.4f}, {0.6f, 0.6f}, {0.3f, 0.4f}, {0.8f, 0.5f},
                    {0.4f, 0.7f}, {0.5f, 0.5f}};
            double[][] shardPos = {{-0.5, 2.5}, {0.3, 1.5}, {-0.3, 3.2}, {0.7, 2.8},
                    {-0.8, 1.8}, {0.1, 2.0}, {0.5, 3.0}, {-0.6, 2.2},
                    {0.4, 1.2}, {-0.1, 3.5}};
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(shardPos[i][0], shardPos[i][1], 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_GLAZED_TERRACOTTA);
                h.scale(shardScales[i][0], shardScales[i][1], 0.1f).glow(80, 70, 100).interpolation(2, 0);
                mirrorShards.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3 amethyst inclusions in the largest shards
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(shardPos[i][0], shardPos[i][1], 0.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.25f, 0.25f, 0.15f).glow(200, 100, 255).interpolation(3, 0);
                amethystInclusions.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.4f);
            DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 25, 2.0, 180, 160, 220, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Each mirror shard spins on its own axis at varying speeds
            for (int i = 0; i < mirrorShards.size(); i++) {
                float angle = ticksAlive * shardSpeeds[i];
                mirrorShards.get(i).rotate(angle, shardTiltX[i], 0.5f, shardTiltZ[i]);
                mirrorShards.get(i).interpolation(2, 0);
            }

            // Frame structure counter-rotates on Y-axis: 0.4 deg/tick
            float frameRot = -ticksAlive * 0.00698f;
            for (BlockDisplayHandle h : outerFrame) {
                h.rotate(frameRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Amethyst inclusions pulse at different frequencies
            float[] pulseFreqs = {0.314f, 0.209f, 0.157f};
            for (int i = 0; i < amethystInclusions.size(); i++) {
                float pulse = 0.25f + (float) Math.sin(ticksAlive * pulseFreqs[i]) * 0.12f;
                amethystInclusions.get(i).scale(pulse, pulse, 0.15f);
                amethystInclusions.get(i).interpolation(3, 0);
            }

            // End rod sparkling from mirror fragments
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle shard : mirrorShards) {
                    Location sLoc = shard.entity().getLocation();
                    c.getWorld().spawnParticle(Particle.END_ROD, sLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Silver-void dust filling the gate opening
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 5, 1.2, 180, 160, 220, 0.8f);
            }

            // Crit particles from frame corners
            if (ticksAlive % 30 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(-1.5, 0, 0), 3, 0.2, 0.2, 0.2, 0.1);
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(2.5, 4, 0), 3, 0.2, 0.2, 0.2, 0.1);
            }

            // Reverse portal from shattered center
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 2.5, 0), 5, 0.8, 1.0, 0.3, 0.02);
            }

            // Sounds: glass break + enderman teleport
            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.4f);
            }
            if (ticksAlive % 80 == 40) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShatteredMirrorGate(plugin); }
    }

    // ================================================================
    // 68. THE VOIDLING'S THRESHOLD — Crude monolithic gate with windchime
    // ================================================================
    public static class VoidlingThreshold extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> soulCrescent = new ArrayList<>();
        private final List<BlockDisplayHandle> jambs = new ArrayList<>();
        private final List<BlockDisplayHandle> lintelStones = new ArrayList<>();
        private final List<BlockDisplayHandle> stalactites = new ArrayList<>();
        private final List<BlockDisplayHandle> floorRunes = new ArrayList<>();
        private BlockDisplayHandle apexStone;

        public VoidlingThreshold(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("voidling_threshold", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(440);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Soul soil crescent at base
            for (int i = 0; i < 10; i++) {
                double angle = Math.PI * 0.2 + (Math.PI * 0.6 * i / 9);
                double x = Math.cos(angle) * 2.5;
                double z = Math.sin(angle) * 1.5;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                h.scale(0.5f, 0.2f, 0.5f).glow(60, 40, 30).interpolation(3, 0);
                soulCrescent.add(h);
                spawnedEntities.add(h.entity());
            }

            // Monolithic blackstone jambs: 4 blocks per side
            for (int y = 0; y < 4; y++) {
                BlockDisplayHandle hL = displayBuilder.spawnBlock(center.clone().add(-1.5, y, 0),
                        Material.BLACKSTONE);
                hL.scale(0.8f, 1.0f, 0.8f).glow(50, 50, 60).interpolation(3, 0);
                jambs.add(hL);
                spawnedEntities.add(hL.entity());

                BlockDisplayHandle hR = displayBuilder.spawnBlock(center.clone().add(1.5, y, 0),
                        Material.BLACKSTONE);
                hR.scale(0.8f, 1.0f, 0.8f).glow(50, 50, 60).interpolation(3, 0);
                jambs.add(hR);
                spawnedEntities.add(hR.entity());
            }

            // Irregular lintel stones balanced across top
            double[] lintelX = {-1.2, -0.4, 0.4, 1.2, -0.8, 0.8};
            for (int i = 0; i < lintelX.length; i++) {
                Location loc = center.clone().add(lintelX[i], 4.0 + (i % 2) * 0.15, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.COBBLED_DEEPSLATE);
                h.scale(0.7f, 0.4f, 0.6f).glow(50, 40, 60).interpolation(3, 0);
                lintelStones.add(h);
                spawnedEntities.add(h.entity());
            }

            // Netherite central lintel keystones
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-0.3 + i * 0.6, 4.3, 0), Material.NETHERITE_BLOCK);
                h.scale(0.5f, 0.4f, 0.5f).glow(20, 20, 30).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // 4 crying obsidian stalactites hanging from lintel
            double[] stalX = {-0.9, -0.3, 0.3, 0.9};
            for (double x : stalX) {
                Location loc = center.clone().add(x, 3.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.25f, 0.6f, 0.25f).glow(128, 0, 255).interpolation(3, 0);
                stalactites.add(h);
                spawnedEntities.add(h.entity());
            }

            // Floor rune prismarine blocks
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-0.8 + i * 0.8, 0.05, 0.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.4f, 0.1f, 0.4f).glow(0, 150, 180).interpolation(3, 0);
                floorRunes.add(h);
                spawnedEntities.add(h.entity());
            }

            // Floating apex stone: amethyst
            apexStone = displayBuilder.spawnBlock(center.clone().add(0, 5.0, 0), Material.AMETHYST_BLOCK);
            apexStone.scale(0.4f, 0.4f, 0.4f).glow(200, 100, 255).interpolation(3, 0);
            spawnedEntities.add(apexStone.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_HIT, 0.7f, 0.6f);
            DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 25, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Lintel stones sway ±4 degrees on Z-axis over 200 ticks
            float lintelSway = (float) Math.sin(ticksAlive * 0.0314f) * 0.0698f;
            for (BlockDisplayHandle h : lintelStones) {
                h.rotate(lintelSway, 0, 0, 1);
                h.interpolation(5, 0);
            }

            // Stalactite windchime: independent swinging
            float[] stalactiteSpeeds = {0.025f, 0.035f, 0.025f, 0.035f};
            for (int i = 0; i < stalactites.size(); i++) {
                float swing = (float) Math.sin(ticksAlive * stalactiteSpeeds[i]) * 0.04f;
                stalactites.get(i).rotate(swing, (i < 2 ? 1 : 0), 0, (i < 2 ? 0 : 1));
                stalactites.get(i).interpolation(3, 0);
            }

            // Floor rune pulsing: sequential scale 1.0 -> 1.15 with phase
            for (int i = 0; i < floorRunes.size(); i++) {
                float runeScale = 0.4f + (float) Math.sin((ticksAlive + i * 13) * 0.157f) * 0.03f;
                floorRunes.get(i).scale(runeScale, 0.1f, runeScale);
                floorRunes.get(i).interpolation(3, 0);
            }

            // Apex stone orbits the gate front face
            float apexAngle = ticksAlive * 0.00262f;
            double ax = Math.cos(apexAngle) * 0.6;
            double az = Math.sin(apexAngle) * 0.6;
            apexStone.entity().teleport(c.clone().add(ax, 5.0, az));

            // Soul fire flames rising from crescent
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < soulCrescent.size(); i += 3) {
                    Location sLoc = soulCrescent.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, sLoc.add(0, 0.1, 0),
                            2, 0.05, 0.15, 0.05, 0.01);
                }
            }

            // Dripping tears from stalactites
            if (ticksAlive % 6 == 0) {
                for (BlockDisplayHandle h : stalactites) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            h.entity().getLocation(), 1, 0.05, 0.1, 0.05, 0);
                }
            }

            // Threshold blue dust from floor runes
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle h : floorRunes) {
                    DisplayBuilder.dustParticles(h.entity().getLocation().add(0, 0.1, 0),
                            2, 0.2, 0, 30, 120, 0.8f);
                }
            }

            // Reverse portal in the doorway
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 2, 0),
                        4, 0.6, 1.2, 0.3, 0.01);
            }

            // End rod shimmer from apex
            if (ticksAlive % 8 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(ax, 5.0, az),
                        2, 0.1, 0.1, 0.1, 0.01);
            }

            // Sound: soul sand hit + basalt ambient
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SOUL_SAND_HIT, 0.3f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidlingThreshold(plugin); }
    }

    // ================================================================
    // 69. THE GATE BETWEEN WORLDS — Massive triple-arched gateway
    // ================================================================
    public static class GateBetweenWorlds extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mainPillars = new ArrayList<>();
        private final List<BlockDisplayHandle> archInlays = new ArrayList<>();
        private final List<BlockDisplayHandle> keystones = new ArrayList<>();
        private final List<BlockDisplayHandle> pylonAmethysts = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> archGlassPanes = new ArrayList<>();
        private final List<BlockDisplayHandle> thresholdBlocks = new ArrayList<>();

        public GateBetweenWorlds(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gate_between_worlds", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(18.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 netherite pillars forming triple-arch structure
            double[] pillarX = {-3.5, -1.0, 1.0, 3.5};
            for (double px : pillarX) {
                for (int y = 0; y < 5; y++) {
                    Location loc = center.clone().add(px, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                    h.scale(0.8f, 1.0f, 0.6f).glow(20, 20, 30).interpolation(3, 0);
                    mainPillars.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Obsidian arch spans (3 arches)
            double[][] archCenters = {{-2.25, 5.5}, {0, 5.5}, {2.25, 5.5}};
            for (double[] ac : archCenters) {
                for (int i = 0; i < 5; i++) {
                    double angle = Math.PI * (0.2 + 0.6 * i / 4);
                    double x = ac[0] + Math.cos(angle) * 1.2;
                    double y = ac[1] + Math.sin(angle) * 0.8;
                    Location loc = center.clone().add(x, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.5f, 0.4f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
                    archInlays.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Crying obsidian keystones at each arch apex (3 per arch)
            for (double[] ac : archCenters) {
                for (int i = -1; i <= 1; i++) {
                    Location loc = center.clone().add(ac[0] + i * 0.3, 6.3, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                    h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                    keystones.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Amethyst pylon blocks: 3 per side above lintel
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add(side * 4.2, 5.5 + y * 0.6, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                    h.scale(0.4f, 0.5f, 0.4f).glow(200, 100, 255).interpolation(3, 0);
                    pylonAmethysts.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Purple glass panes filling each arch (2 per opening)
            for (double[] ac : archCenters) {
                List<BlockDisplayHandle> panes = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    Location loc = center.clone().add(ac[0] - 0.3 + i * 0.6, 3 + i * 1.2, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                    h.scale(0.9f, 1.5f, 0.05f).glow(160, 0, 200).interpolation(3, 0);
                    panes.add(h);
                    spawnedEntities.add(h.entity());
                }
                archGlassPanes.add(panes);
            }

            // Ground-level threshold courses: dark prismarine
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-2 + i * 2, -0.1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.8f, 0.2f, 0.6f).glow(0, 150, 180).interpolation(3, 0);
                thresholdBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central tympanum decoration
            BlockDisplayHandle tympanum = displayBuilder.spawnBlock(center.clone().add(0, 6.5, 0),
                    Material.BLACK_GLAZED_TERRACOTTA);
            tympanum.scale(0.5f, 0.5f, 0.3f).glow(40, 40, 50).interpolation(3, 0);
            spawnedEntities.add(tympanum.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.8f);
            DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 40, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Entire gate rotates on Y-axis: 0.25 deg/tick
            float gateRot = ticksAlive * 0.00436f;
            for (BlockDisplayHandle h : mainPillars) {
                h.rotate(gateRot, 0, 1, 0);
                h.interpolation(5, 0);
            }
            for (BlockDisplayHandle h : archInlays) {
                h.rotate(gateRot, 0, 1, 0);
                h.interpolation(5, 0);
            }

            // Left arch panes tilt 5 degrees
            if (!archGlassPanes.isEmpty() && archGlassPanes.size() > 0) {
                float leftTilt = (float) Math.sin(ticksAlive * 0.1f) * 0.0873f;
                for (BlockDisplayHandle h : archGlassPanes.get(0)) {
                    h.rotate(leftTilt, 0, 0, 1);
                    h.interpolation(3, 0);
                }
            }
            // Center arch panes ripple in scale
            if (archGlassPanes.size() > 1) {
                float centerScale = 1.0f + (float) Math.sin(ticksAlive * 0.157f) * 0.05f;
                for (BlockDisplayHandle h : archGlassPanes.get(1)) {
                    h.scale(0.9f * centerScale, 1.5f * centerScale, 0.05f);
                    h.interpolation(3, 0);
                }
            }
            // Right arch panes counter-rotate
            if (archGlassPanes.size() > 2) {
                float rightRot = (float) Math.sin(ticksAlive * 0.08f) * 0.0524f;
                for (BlockDisplayHandle h : archGlassPanes.get(2)) {
                    h.rotate(-rightRot, 0, 0, 1);
                    h.interpolation(3, 0);
                }
            }

            // Pylon amethyst orbit: 0.3 block radius at 1 deg/tick
            float pylonAngle = ticksAlive * 0.01745f;
            for (int i = 0; i < pylonAmethysts.size(); i++) {
                double orbX = Math.cos(pylonAngle + i * 1.047f) * 0.3;
                double orbZ = Math.sin(pylonAngle + i * 1.047f) * 0.3;
                BlockDisplay bd = pylonAmethysts.get(i).entity();
                Location baseLoc = bd.getLocation();
                // Apply small orbit offset
                pylonAmethysts.get(i).translate(-0.2f + (float) orbX, -0.5f, (float) orbZ);
                pylonAmethysts.get(i).interpolation(3, 0);
            }

            // Portal particles from all 3 arch openings
            if (ticksAlive % 2 == 0) {
                double[][] archCenters = {{-2.25, 3}, {0, 3}, {2.25, 3}};
                for (double[] ac : archCenters) {
                    for (int p = 0; p < 4; p++) {
                        double px = ac[0] + (Math.random() - 0.5) * 1.5;
                        double py = ac[1] + Math.random() * 2;
                        c.getWorld().spawnParticle(Particle.PORTAL, c.clone().add(px, py, 0), 2, 0, 0, 0, 0.3);
                    }
                }
            }

            // Reverse portal from pylon tops
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(-4.2, 7, 0), 5, 0.2, 0.3, 0.2, 0.03);
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(4.2, 7, 0), 5, 0.2, 0.3, 0.2, 0.03);
            }

            // Grey-violet dust filling the structure
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 6, 3.0, 90, 70, 130, 1.0f);
            }

            // End rod particles from keystones
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < keystones.size(); i += 3) {
                    Location kLoc = keystones.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.END_ROD, kLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Sound: respawn anchor charge + portal ambient
            if (ticksAlive % 60 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GateBetweenWorlds(plugin); }
    }

    // ================================================================
    // 70. THE NULL THRESHOLD — Eerily static minimalist void rectangle
    // ================================================================
    public static class NullThreshold extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> netheriteBand = new ArrayList<>();
        private final List<BlockDisplayHandle> glassPanes = new ArrayList<>();
        private BlockDisplayHandle orbitingAmethyst;
        private boolean silenceBroken = false;
        private int silenceBreakTick = 160; // 8 seconds at 20 tps

        public NullThreshold(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("null_threshold", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(20.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(450);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(10.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Minimalist rectangular frame: black concrete, 3 wide x 5 tall, 1 block thick
            // Left column
            for (int y = 0; y < 5; y++) {
                Location loc = center.clone().add(-1.5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.5f, 1.0f, 0.5f).glow(10, 10, 15).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Right column
            for (int y = 0; y < 5; y++) {
                Location loc = center.clone().add(1.5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.5f, 1.0f, 0.5f).glow(10, 10, 15).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Top beam
            for (int x = -1; x <= 1; x++) {
                Location loc = center.clone().add(x, 5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(1.0f, 0.5f, 0.5f).glow(10, 10, 15).interpolation(3, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Netherite reinforcing outer band on 3 sides (left, top, right)
            // Left band
            for (int y = 0; y < 5; y++) {
                Location loc = center.clone().add(-2.0, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0.3f, 1.0f, 0.5f).glow(15, 15, 20).interpolation(3, 0);
                netheriteBand.add(h);
                spawnedEntities.add(h.entity());
            }
            // Right band
            for (int y = 0; y < 5; y++) {
                Location loc = center.clone().add(2.0, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0.3f, 1.0f, 0.5f).glow(15, 15, 20).interpolation(3, 0);
                netheriteBand.add(h);
                spawnedEntities.add(h.entity());
            }
            // Top band
            for (int x = -1; x <= 1; x++) {
                Location loc = center.clone().add(x, 5.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(1.0f, 0.3f, 0.5f).glow(15, 15, 20).interpolation(3, 0);
                netheriteBand.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 obsidian corner blocks
            double[][] obsCorners = {{-1.5, 0, 0}, {1.5, 0, 0}, {-1.5, 5, 0}, {1.5, 5, 0}};
            for (double[] pos : obsCorners) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(pos[0], pos[1], pos[2]),
                        Material.OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.5f).glow(30, 0, 40).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // 5 magenta glass panes filling the frame opening
            for (int y = 0; y < 5; y++) {
                Location loc = center.clone().add(0, y + 0.25, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGENTA_STAINED_GLASS);
                h.scale(2.5f, 1.0f, 0.02f).glow(80, 0, 60).interpolation(3, 0);
                glassPanes.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crying obsidian floor anchors
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-1.2 + i * 2.4, -0.1, 0), Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.3f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
            }

            // Orbiting amethyst: 1 block in front of frame center
            orbitingAmethyst = displayBuilder.spawnBlock(center.clone().add(0, 2.5, 1), Material.AMETHYST_BLOCK);
            orbitingAmethyst.scale(0.4f, 0.4f, 0.4f).glow(200, 100, 255).interpolation(3, 0);
            spawnedEntities.add(orbitingAmethyst.entity());

            // No sound on spawn — silence IS the attack
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Glass panes shift nearly imperceptibly: 0.001 blocks/tick on Z
            float glassShift = (ticksAlive * 0.001f) % 0.05f;
            for (BlockDisplayHandle h : glassPanes) {
                h.translate(-1.25f, -0.25f, glassShift);
                h.interpolation(5, 0);
            }

            // Orbiting amethyst: slow 5-block radius circle at 0.15 deg/tick
            float orbitAngle = ticksAlive * 0.00262f;
            double orbX = Math.cos(orbitAngle) * 2.5;
            double orbZ = Math.sin(orbitAngle) * 2.5;
            orbitingAmethyst.entity().teleport(c.clone().add(orbX, 2.5, orbZ));

            // Near-invisible black dust from frame interior: very sparse
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2.5, 0), 1, 0.8, 5, 5, 5, 0.5f);
            }

            // Reverse portal burst: once per 40 ticks from frame interior
            if (ticksAlive % 40 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 2.5, 0),
                        30, 0.8, 1.5, 0.3, 0.02);
            }

            // THE SILENCE BREAK: At 8 seconds, sonic boom + massive damage
            if (ticksAlive == silenceBreakTick && !silenceBroken) {
                silenceBroken = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.3f);
                triggerImpactDamage(c.clone().add(0, 2.5, 0));

                // Explosion of particles on silence break
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 2.5, 0),
                        80, 2, 3, 2, 0.1);
                DisplayBuilder.darkPurpleDust(c.clone().add(0, 2.5, 0), 40, 4.0);
                DisplayBuilder.crimsonDust(c.clone().add(0, 2.5, 0), 30, 3.0);

                // End rod explosion
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 2.5, 0),
                        50, 3, 3, 3, 0.1);
            }

            // After silence break: the portal becomes more visually active
            if (silenceBroken) {
                // Continuous reverse portal streams
                if (ticksAlive % 3 == 0) {
                    c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 2.5, 0),
                            8, 1.0, 2.0, 0.5, 0.03);
                }
                // End rod tendrils
                if (ticksAlive % 5 == 0) {
                    c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(
                            (Math.random() - 0.5) * 3, Math.random() * 5, (Math.random() - 0.5) * 0.5
                    ), 3, 0, 0, 0, 0.02);
                }
                // Purple dust halo
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.purpleDust(c.clone().add(0, 2.5, 0), 5, 2.0);
                }
            }

            // The frame remains perfectly, unnervingly still — that's the whole point.
            // No rotation. No movement. Just the subtle glass and the orbiting amethyst.
        }

        @Override
        protected void onImpact(Location impactLocation) {
            // Additional impact visual effects handled in onTick at silence break
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NullThreshold(plugin); }
    }
}
