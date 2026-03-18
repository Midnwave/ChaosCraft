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
 * Phase 1 Block Display -- GROUP 3: WALLS AND BARRIERS
 * 10 flat or curved barrier structures (#21-30).
 * Adapted from boss1-voidmaw.md with Calamity attack rules applied:
 * - NO status effects (Blindness, Slowness, etc. removed -> damage only)
 * - Always spawn straight (yaw=0, pitch=0)
 * - Calamity particle palette (purple, cyan, crimson)
 * - All values configurable via AttackConfig
 */
public final class WallsBarriers {

    private WallsBarriers() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidBulwark(plugin));
        registry.register(new CurvedSpineWall(plugin));
        registry.register(new NetheriteGate(plugin));
        registry.register(new ObsidianCurtain(plugin));
        registry.register(new FractureShieldArray(plugin));
        registry.register(new BlackstoneBulgeWall(plugin));
        registry.register(new DeepPrismarineTideWall(plugin));
        registry.register(new SoulVeil(plugin));
        registry.register(new JaggedPalisade(plugin));
        registry.register(new MirrorVoidPanel(plugin));
    }

    // ================================================================
    // 21. VOID BULWARK -- Solid obsidian wall rising from below
    // ================================================================
    public static class VoidBulwark extends BlockDisplayAttack {

        private BlockDisplayHandle wallFace;
        private BlockDisplayHandle borderFrame;
        private BlockDisplayHandle foundation;
        private BlockDisplayHandle coping;
        private final List<BlockDisplayHandle> cryingNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> glassPanes = new ArrayList<>();
        private final List<BlockDisplayHandle> cornerBlocks = new ArrayList<>();
        private float emergeProgress = 0;

        public VoidBulwark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_bulwark", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main wall face: 9 wide x 4 tall obsidian slab
            wallFace = displayBuilder.spawnBlock(center.clone().add(0, -4, 0), Material.OBSIDIAN);
            wallFace.scale(9.0f, 4.0f, 1.0f).glow(40, 0, 60).interpolation(3, 0);
            spawnedEntities.add(wallFace.entity());

            // Blackstone border frame: wraps top and sides
            borderFrame = displayBuilder.spawnBlock(center.clone().add(0, -4, 0), Material.BLACKSTONE);
            borderFrame.scale(9.4f, 4.4f, 0.6f).glow(50, 50, 60).interpolation(3, 0);
            spawnedEntities.add(borderFrame.entity());

            // Cobbled deepslate foundation base row
            foundation = displayBuilder.spawnBlock(center.clone().add(0, -4.5, 0), Material.COBBLED_DEEPSLATE);
            foundation.scale(9.0f, 0.8f, 1.2f).glow(40, 40, 50).interpolation(3, 0);
            spawnedEntities.add(foundation.entity());

            // Polished blackstone coping along top
            coping = displayBuilder.spawnBlock(center.clone().add(0, -0.2, 0), Material.POLISHED_BLACKSTONE);
            coping.scale(9.0f, 0.4f, 1.1f).glow(60, 60, 70).interpolation(3, 0);
            spawnedEntities.add(coping.entity());

            // 8 crying obsidian nodes in 3x3 grid pattern on wall face
            double[][] cryingOffsets = {
                {-3, -1, 0.5}, {0, -1, 0.5}, {3, -1, 0.5}, {-3, -3, 0.5},
                {0, -3, 0.5}, {3, -3, 0.5}, {-1.5, -2, 0.5}, {1.5, -2, 0.5}
            };
            for (double[] off : cryingOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.6f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                cryingNodes.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 black stained glass panes as dark windows
            for (double xOff : new double[]{-1.5, 1.5}) {
                BlockDisplayHandle g = displayBuilder.spawnBlock(
                        center.clone().add(xOff, -2, 0.5), Material.BLACK_STAINED_GLASS);
                g.scale(1.0f, 1.2f, 0.15f).glow(20, 0, 40).interpolation(3, 0);
                glassPanes.add(g);
                spawnedEntities.add(g.entity());
            }

            // 4 netherite corner blocks
            double[][] cornerPos = {{-4.5, -4.5, 0}, {4.5, -4.5, 0}, {-4.5, 0, 0}, {4.5, 0, 0}};
            for (double[] pos : cornerPos) {
                BlockDisplayHandle c2 = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], pos[1], pos[2]), Material.NETHERITE_BLOCK);
                c2.scale(0.6f, 0.6f, 0.6f).glow(20, 20, 30).interpolation(3, 0);
                cornerBlocks.add(c2);
                spawnedEntities.add(c2.entity());
            }

            // Spawn sound + particle burst
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.9f);
            w.spawnParticle(Particle.BLOCK, center, 50, 4, 2, 1, 0, Material.OBSIDIAN.createBlockData());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 25 ticks
            if (ticksAlive <= 25) {
                emergeProgress = ticksAlive / 25.0f;
                float yOffset = emergeProgress * 4.5f;
                wallFace.entity().teleport(c.clone().add(0, -4 + yOffset, 0));
                borderFrame.entity().teleport(c.clone().add(0, -4 + yOffset, 0));
                foundation.entity().teleport(c.clone().add(0, -4.5 + yOffset, 0));
                coping.entity().teleport(c.clone().add(0, -0.2 + yOffset - (4.5f - yOffset), 0));

                double[][] cryingOffsets = {
                    {-3, -1, 0.5}, {0, -1, 0.5}, {3, -1, 0.5}, {-3, -3, 0.5},
                    {0, -3, 0.5}, {3, -3, 0.5}, {-1.5, -2, 0.5}, {1.5, -2, 0.5}
                };
                for (int i = 0; i < cryingNodes.size(); i++) {
                    cryingNodes.get(i).entity().teleport(c.clone().add(
                            cryingOffsets[i][0], cryingOffsets[i][1] + yOffset, cryingOffsets[i][2]));
                }
            }

            // Crying obsidian vibration: independent Z-axis oscillation, 4-tick cycle
            if (ticksAlive > 25) {
                for (int i = 0; i < cryingNodes.size(); i++) {
                    float vibrate = (float) Math.sin((ticksAlive + i * 7) * 1.57) * 0.05f;
                    cryingNodes.get(i).translate(-0.3f, -0.3f, vibrate);
                    cryingNodes.get(i).interpolation(2, 0);
                }
            }

            // Glass pane breathing: 30-tick brightness cycle
            if (ticksAlive > 25 && ticksAlive % 15 == 0) {
                int brightness = (int) (10 + Math.sin(ticksAlive * 0.209) * 5);
                for (BlockDisplayHandle g : glassPanes) {
                    g.brightness(brightness, brightness);
                }
            }

            // Tears weeping down from each crying obsidian node
            if (ticksAlive % 4 == 0 && ticksAlive > 25) {
                for (BlockDisplayHandle node : cryingNodes) {
                    Location nLoc = node.entity().getLocation().add(0, -0.5, 0);
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, nLoc, 2, 0.1, 0.5, 0.05, 0);
                }
            }

            // Large smoke behind wall
            if (ticksAlive % 6 == 0 && ticksAlive > 25) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 1.5, -1), 3, 3, 1, 0.2, 0.01);
            }

            // Ambient cave sound
            if (ticksAlive % 80 == 40) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidBulwark(plugin); }
    }

    // ================================================================
    // 22. CURVED SPINE WALL -- Concave arc barrier that sweeps sideways
    // ================================================================
    public static class CurvedSpineWall extends BlockDisplayAttack {

        private BlockDisplayHandle arcWall;
        private BlockDisplayHandle innerCladding;
        private final List<BlockDisplayHandle> buttresses = new ArrayList<>();
        private final List<BlockDisplayHandle> anchorBlocks = new ArrayList<>();
        private BlockDisplayHandle prismarineInlay;
        private float emergeProgress = 0;
        private float sweepOffset = 0;
        private boolean sweepForward = true;

        public CurvedSpineWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("curved_spine_wall", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(700);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main curved wall: blackstone arc, 10 wide, 3 tall
            arcWall = displayBuilder.spawnBlock(center.clone().add(0, -3, 0), Material.BLACKSTONE);
            arcWall.scale(10.0f, 3.0f, 1.5f).glow(50, 50, 60).interpolation(3, 0);
            spawnedEntities.add(arcWall.entity());

            // Inner polished cladding on concave face
            innerCladding = displayBuilder.spawnBlock(center.clone().add(0, -3, 0.6), Material.POLISHED_BLACKSTONE);
            innerCladding.scale(9.5f, 2.8f, 0.4f).glow(60, 60, 70).interpolation(3, 0);
            spawnedEntities.add(innerCladding.entity());

            // 3 buttresses on convex face
            for (int i = -1; i <= 1; i++) {
                BlockDisplayHandle b = displayBuilder.spawnBlock(
                        center.clone().add(i * 3.0, -3, -0.8), Material.COBBLED_DEEPSLATE);
                b.scale(1.5f, 2.5f, 1.5f).glow(40, 40, 50).interpolation(3, 0);
                buttresses.add(b);
                spawnedEntities.add(b.entity());
            }

            // Netherite anchors: 2 endpoints + top center
            double[][] anchorPos = {{-5, -0.5, 0}, {5, -0.5, 0}, {0, -0.3, 0}};
            for (double[] pos : anchorPos) {
                BlockDisplayHandle a = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], pos[1], pos[2]), Material.NETHERITE_BLOCK);
                a.scale(0.7f, 0.7f, 0.7f).glow(20, 20, 30).interpolation(3, 0);
                anchorBlocks.add(a);
                spawnedEntities.add(a.entity());
            }

            // Dark prismarine inlays at arc center
            prismarineInlay = displayBuilder.spawnBlock(center.clone().add(0, -1.5, 0.7), Material.DARK_PRISMARINE);
            prismarineInlay.scale(1.2f, 1.0f, 0.3f).glow(0, 150, 180).interpolation(3, 0);
            spawnedEntities.add(prismarineInlay.entity());

            // Obsidian ribs on convex face
            for (int i = 0; i < 5; i++) {
                double xPos = -4 + i * 2;
                BlockDisplayHandle rib = displayBuilder.spawnBlock(
                        center.clone().add(xPos, -1.5, -0.6), Material.OBSIDIAN);
                rib.scale(0.4f, 3.0f, 0.4f).glow(40, 0, 60).interpolation(3, 0);
                spawnedEntities.add(rib.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 25 ticks
            if (ticksAlive <= 25) {
                emergeProgress = ticksAlive / 25.0f;
                float yOffset = emergeProgress * 3.0f;
                arcWall.entity().teleport(c.clone().add(0, -3 + yOffset, 0));
                innerCladding.entity().teleport(c.clone().add(0, -3 + yOffset, 0.6));
                for (int i = 0; i < buttresses.size(); i++) {
                    buttresses.get(i).entity().teleport(c.clone().add((i - 1) * 3.0, -3 + yOffset, -0.8));
                }
            }

            // Sweeping motion: translate sideways 0.5 blocks/sec, reverse every 10 sec
            if (ticksAlive > 25) {
                float sweepSpeed = 0.025f; // 0.5 blocks/sec at 20 tps
                if (sweepForward) {
                    sweepOffset += sweepSpeed;
                    if (sweepOffset >= 5.0f) sweepForward = false;
                } else {
                    sweepOffset -= sweepSpeed;
                    if (sweepOffset <= -5.0f) sweepForward = true;
                }

                arcWall.entity().teleport(c.clone().add(sweepOffset, 0, 0));
                innerCladding.entity().teleport(c.clone().add(sweepOffset, 0, 0.6));
                for (int i = 0; i < buttresses.size(); i++) {
                    buttresses.get(i).entity().teleport(c.clone().add(sweepOffset + (i - 1) * 3.0, 0, -0.8));
                }

                // Netherite sweep sound
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_STEP, 0.3f, 0.5f);
                }
            }

            // Dark dust streaming along concave face in sweep direction
            if (ticksAlive % 5 == 0 && ticksAlive > 25) {
                DisplayBuilder.dustParticles(c.clone().add(sweepOffset, 1, 0.8), 4, 2.0,
                        51, 51, 51, 1.0f);
            }

            // Sculk soul from buttress bases
            if (ticksAlive % 8 == 0 && ticksAlive > 25) {
                for (BlockDisplayHandle b : buttresses) {
                    Location bLoc = b.entity().getLocation().add(0, -0.3, 0);
                    c.getWorld().spawnParticle(Particle.SCULK_SOUL, bLoc, 2, 0.3, 0.1, 0.3, 0.01);
                }
            }

            // Large smoke at arc endpoints
            if (ticksAlive % 10 == 0 && ticksAlive > 25) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(sweepOffset - 5, 1, 0), 2, 0.2, 0.3, 0.2, 0.01);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(sweepOffset + 5, 1, 0), 2, 0.2, 0.3, 0.2, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CurvedSpineWall(plugin); }
    }

    // ================================================================
    // 23. NETHERITE GATE -- Massive rectangular gate with cross-gap
    // ================================================================
    public static class NetheriteGate extends BlockDisplayAttack {

        private BlockDisplayHandle leftPillar;
        private BlockDisplayHandle rightPillar;
        private BlockDisplayHandle lintel;
        private BlockDisplayHandle interiorPanel;
        private final List<BlockDisplayHandle> cryingEdge = new ArrayList<>();
        private final List<BlockDisplayHandle> glassDepth = new ArrayList<>();
        private final List<BlockDisplayHandle> plinths = new ArrayList<>();
        private final List<BlockDisplayHandle> amethystEyes = new ArrayList<>();
        private float fadeProgress = 0;

        public NetheriteGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("netherite_gate", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left pillar: 2x7x2 netherite
            leftPillar = displayBuilder.spawnBlock(center.clone().add(-3, 0, 0), Material.NETHERITE_BLOCK);
            leftPillar.scale(2.0f, 7.0f, 2.0f).glow(20, 20, 30).interpolation(5, 0);
            spawnedEntities.add(leftPillar.entity());

            // Right pillar: 2x7x2 netherite
            rightPillar = displayBuilder.spawnBlock(center.clone().add(3, 0, 0), Material.NETHERITE_BLOCK);
            rightPillar.scale(2.0f, 7.0f, 2.0f).glow(20, 20, 30).interpolation(5, 0);
            spawnedEntities.add(rightPillar.entity());

            // Top lintel: 8x3x2
            lintel = displayBuilder.spawnBlock(center.clone().add(0, 5, 0), Material.NETHERITE_BLOCK);
            lintel.scale(8.0f, 2.0f, 2.0f).glow(25, 25, 35).interpolation(5, 0);
            spawnedEntities.add(lintel.entity());

            // Interior obsidian panel (sealed gate with cross gap)
            interiorPanel = displayBuilder.spawnBlock(center.clone().add(0, 2, 0), Material.OBSIDIAN);
            interiorPanel.scale(4.0f, 5.0f, 0.6f).glow(40, 0, 60).interpolation(5, 0);
            spawnedEntities.add(interiorPanel.entity());

            // Crying obsidian on inner edges of frame
            double[][] cryingPos = {
                {-2, 1, 0.5}, {-2, 3, 0.5}, {2, 1, 0.5}, {2, 3, 0.5},
                {-1, 4.5, 0.5}, {0, 4.5, 0.5}, {1, 4.5, 0.5}, {0, 0.3, 0.5}
            };
            for (double[] pos : cryingPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], pos[1], pos[2]), Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(5, 0);
                cryingEdge.add(h);
                spawnedEntities.add(h.entity());
            }

            // Purple stained glass behind obsidian for depth in 3 quadrants
            double[][] glassPos = {{-1, 1.5, -0.2}, {1, 1.5, -0.2}, {-1, 3.5, -0.2}};
            for (double[] pos : glassPos) {
                BlockDisplayHandle g = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], pos[1], pos[2]), Material.PURPLE_STAINED_GLASS);
                g.scale(1.5f, 1.5f, 0.3f).glow(160, 0, 200).interpolation(5, 0);
                glassDepth.add(g);
                spawnedEntities.add(g.entity());
            }

            // Base plinths at pillar bottoms
            double[][] plinthPos = {{-3.5, -0.3, -0.5}, {-3.5, -0.3, 0.5}, {3.5, -0.3, -0.5}, {3.5, -0.3, 0.5}};
            for (double[] pos : plinthPos) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], pos[1], pos[2]), Material.POLISHED_BLACKSTONE);
                p.scale(0.7f, 0.4f, 0.7f).glow(60, 60, 70).interpolation(5, 0);
                plinths.add(p);
                spawnedEntities.add(p.entity());
            }

            // Amethyst pillar eyes at eye level
            for (double xOff : new double[]{-4, 4}) {
                BlockDisplayHandle eye = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 3, 0), Material.AMETHYST_BLOCK);
                eye.scale(0.5f, 0.5f, 0.5f).glow(200, 100, 255).interpolation(5, 0);
                amethystEyes.add(eye);
                spawnedEntities.add(eye.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Materialization: fade in over 40 ticks (scale from 0.1 to full)
            if (ticksAlive <= 40) {
                fadeProgress = ticksAlive / 40.0f;
                float s = 0.1f + fadeProgress * 0.9f;
                leftPillar.scale(2.0f * s, 7.0f * s, 2.0f * s).interpolation(3, 0);
                rightPillar.scale(2.0f * s, 7.0f * s, 2.0f * s).interpolation(3, 0);
                lintel.scale(8.0f * s, 2.0f * s, 2.0f * s).interpolation(3, 0);
                interiorPanel.scale(4.0f * s, 5.0f * s, 0.6f * s).interpolation(3, 0);
            }

            // Interior panel Y-axis rotation: 3 deg/sec (frame stays still)
            if (ticksAlive > 40) {
                float rot = (ticksAlive - 40) * 0.00262f;
                interiorPanel.rotate(rot, 0, 1, 0);
                interiorPanel.interpolation(3, 0);
            }

            // Cross-gap breathing: pulse the interior panel scale
            if (ticksAlive > 40) {
                float pulse = (float) Math.sin(ticksAlive * 0.314) * 0.02f; // +-0.2 blocks effect
                interiorPanel.scale(4.0f + pulse * 10, 5.0f + pulse * 10, 0.6f);
                interiorPanel.interpolation(3, 0);
            }

            // Reverse portal through the cross-gap in both directions
            if (ticksAlive % 3 == 0 && ticksAlive > 40) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        c.clone().add(0, 2.5, 0), 6, 0.3, 1.5, 0.3, 0.05);
            }

            // Tears from crying obsidian frame
            if (ticksAlive % 5 == 0 && ticksAlive > 40) {
                for (BlockDisplayHandle cry : cryingEdge) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            cry.entity().getLocation(), 1, 0.1, 0.2, 0.1, 0);
                }
            }

            // Enchant particles from amethyst eyes
            if (ticksAlive % 4 == 0 && ticksAlive > 40) {
                for (BlockDisplayHandle eye : amethystEyes) {
                    c.getWorld().spawnParticle(Particle.ENCHANT,
                            eye.entity().getLocation().add(0, 0.3, 0), 5, 0.2, 0.2, 0.2, 0.3);
                }
            }

            // Enderman stare looping
            if (ticksAlive % 60 == 30 && ticksAlive > 40) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_STARE, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NetheriteGate(plugin); }
    }

    // ================================================================
    // 24. OBSIDIAN CURTAIN -- Swaying hanging curtain barrier
    // ================================================================
    public static class ObsidianCurtain extends BlockDisplayAttack {

        private BlockDisplayHandle topRod;
        private final List<BlockDisplayHandle> upperRow = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerRow = new ArrayList<>();
        private final List<BlockDisplayHandle> anchorRings = new ArrayList<>();

        public ObsidianCurtain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obsidian_curtain", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Top rod/rail: 14-block horizontal blackstone beam
            topRod = displayBuilder.spawnBlock(center.clone().add(0, 3, 0), Material.BLACKSTONE);
            topRod.scale(14.0f, 0.5f, 0.5f).glow(50, 50, 60).interpolation(3, 0);
            spawnedEntities.add(topRod.entity());

            // Upper row: 14 obsidian blocks (staggered)
            for (int i = 0; i < 14; i++) {
                double xOff = -6.5 + i;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 2, 0), Material.OBSIDIAN);
                h.scale(0.9f, 0.9f, 0.3f).glow(40, 0, 60).interpolation(2, 0);
                upperRow.add(h);
                spawnedEntities.add(h.entity());
            }

            // Lower row: alternating obsidian and crying obsidian (offset 0.5)
            for (int i = 0; i < 14; i++) {
                double xOff = -6.5 + i + 0.5; // stagger offset
                Material mat = (i % 2 == 0) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 1, 0), mat);
                int r = (mat == Material.CRYING_OBSIDIAN) ? 128 : 40;
                int g = 0;
                int b = (mat == Material.CRYING_OBSIDIAN) ? 255 : 60;
                h.scale(0.9f, 0.9f, 0.3f).glow(r, g, b).interpolation(2, 0);
                lowerRow.add(h);
                spawnedEntities.add(h.entity());
            }

            // 5 black stained glass panes in stagger gaps
            for (int i = 0; i < 5; i++) {
                double xOff = -4.5 + i * 2.5;
                BlockDisplayHandle g = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 1.5, 0), Material.BLACK_STAINED_GLASS_PANE);
                g.scale(0.3f, 0.8f, 0.15f).glow(20, 0, 40).interpolation(2, 0);
                spawnedEntities.add(g.entity());
            }

            // 3 polished blackstone anchor rings on the rod
            for (int i = -1; i <= 1; i++) {
                BlockDisplayHandle ring = displayBuilder.spawnBlock(
                        center.clone().add(i * 5, 3.2, 0), Material.POLISHED_BLACKSTONE);
                ring.scale(0.5f, 0.5f, 0.5f).glow(60, 60, 70).interpolation(2, 0);
                anchorRings.add(ring);
                spawnedEntities.add(ring.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Full curtain sway: X-axis sine wave, +-8 degrees over 60-tick cycle
            float swayAngle = (float) Math.sin(ticksAlive * 0.1047) * 0.14f; // +-8 degrees

            // Apply sway to upper row
            for (int i = 0; i < upperRow.size(); i++) {
                upperRow.get(i).rotate(swayAngle, 1, 0, 0);
                upperRow.get(i).interpolation(3, 0);
            }

            // Lower row: base sway + individual shimmer perpendicular to face
            for (int i = 0; i < lowerRow.size(); i++) {
                float shimmer = (float) Math.sin((ticksAlive + i * 5) * 0.785) * 0.1f; // +-0.1 blocks, 8-tick cycle
                lowerRow.get(i).rotate(swayAngle, 1, 0, 0);
                lowerRow.get(i).translate(-0.45f + (i % 2 == 0 ? 0.5f : 0f), -0.45f, shimmer);
                lowerRow.get(i).interpolation(2, 0);
            }

            // Dynamic fold depth: stagger offset oscillation
            if (ticksAlive % 10 == 0) {
                float foldShift = (float) Math.sin(ticksAlive * 0.05) * 0.2f;
                for (int i = 0; i < lowerRow.size(); i++) {
                    Location newLoc = c.clone().add(-6.0 + i + 0.5 + foldShift, 1, 0);
                    lowerRow.get(i).entity().teleport(newLoc);
                }
            }

            // Crying obsidian hem drip
            if (ticksAlive % 3 == 0) {
                for (int i = 1; i < lowerRow.size(); i += 2) {
                    Location dripLoc = lowerRow.get(i).entity().getLocation().add(0, -0.5, 0);
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, dripLoc, 3, 0.1, 0.3, 0.05, 0);
                }
            }

            // Large smoke behind curtain
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(0, 2, -0.5), 4, 5, 0.5, 0.2, 0.01);
            }

            // Ash from rod
            if (ticksAlive % 8 == 0) {
                c.getWorld().spawnParticle(Particle.ASH,
                        c.clone().add(0, 3.5, 0), 6, 6, 0.1, 0.3, 0);
            }

            // Chain creak sound every 3 seconds
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ObsidianCurtain(plugin); }
    }

    // ================================================================
    // 25. FRACTURE SHIELD ARRAY -- 5 overlapping hexagonal shields
    // ================================================================
    public static class FractureShieldArray extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shields = new ArrayList<>();
        private final List<BlockDisplayHandle> amethystCores = new ArrayList<>();
        private final List<BlockDisplayHandle> bridgePieces = new ArrayList<>();
        private BlockDisplayHandle leftAnchor;
        private BlockDisplayHandle rightAnchor;

        public FractureShieldArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fracture_shield_array", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 hexagonal shields in a staggered overlapping line
            for (int i = 0; i < 5; i++) {
                double xOff = -4 + i * 2; // Overlapping by 2 blocks
                Location sLoc = center.clone().add(xOff, 5, 0);
                BlockDisplayHandle shield = displayBuilder.spawnBlock(sLoc, Material.COBBLED_DEEPSLATE);
                shield.scale(2.5f, 2.5f, 0.5f).glow(60, 60, 80).interpolation(3, 0);
                shields.add(shield);
                spawnedEntities.add(shield.entity());

                // Obsidian rim on outer edges (3 per shield)
                for (int r = 0; r < 3; r++) {
                    double rimAngle = (Math.PI * 2 * r) / 3 + (i * 0.4);
                    BlockDisplayHandle rim = displayBuilder.spawnBlock(
                            sLoc.clone().add(Math.cos(rimAngle) * 1.3, Math.sin(rimAngle) * 1.3, 0.25),
                            Material.OBSIDIAN);
                    rim.scale(0.5f, 0.5f, 0.3f).glow(40, 0, 60).interpolation(3, 0);
                    spawnedEntities.add(rim.entity());
                }

                // Amethyst core at center of each shield
                BlockDisplayHandle core = displayBuilder.spawnBlock(sLoc.clone(), Material.AMETHYST_BLOCK);
                core.scale(0.6f, 0.6f, 0.4f).glow(200, 100, 255).interpolation(3, 0);
                amethystCores.add(core);
                spawnedEntities.add(core.entity());
            }

            // Blackstone connecting bridges between shields
            for (int i = 0; i < 4; i++) {
                double xOff = -3 + i * 2;
                BlockDisplayHandle bridge = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 5, 0), Material.BLACKSTONE);
                bridge.scale(1.0f, 0.5f, 0.4f).glow(50, 50, 60).interpolation(3, 0);
                bridgePieces.add(bridge);
                spawnedEntities.add(bridge.entity());
            }

            // Dark prismarine on outer shields
            for (int side = 0; side < 2; side++) {
                double xOff = (side == 0) ? -4.5 : 4.5;
                BlockDisplayHandle dp = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 5, 0), Material.DARK_PRISMARINE);
                dp.scale(0.6f, 1.0f, 0.4f).glow(0, 150, 180).interpolation(3, 0);
                spawnedEntities.add(dp.entity());
            }

            // Netherite terminal anchors at far ends
            leftAnchor = displayBuilder.spawnBlock(center.clone().add(-5.5, 5, 0), Material.NETHERITE_BLOCK);
            leftAnchor.scale(0.5f, 0.5f, 0.5f).glow(20, 20, 30).interpolation(3, 0);
            spawnedEntities.add(leftAnchor.entity());

            rightAnchor = displayBuilder.spawnBlock(center.clone().add(5.5, 5, 0), Material.NETHERITE_BLOCK);
            rightAnchor.scale(0.5f, 0.5f, 0.5f).glow(20, 20, 30).interpolation(3, 0);
            spawnedEntities.add(rightAnchor.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_HIT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Each shield rotates on its own perpendicular axis (72 deg apart)
            // creating a rolling-wave weave effect
            for (int i = 0; i < shields.size(); i++) {
                float baseAngle = (float) (i * Math.PI * 2 / 5); // 72 degrees apart
                float rotSpeed = ticksAlive * 0.00436f; // 5 deg/sec
                float rot = baseAngle + rotSpeed;

                // Rotate on unique axis per shield (tilted perpendicular)
                float axisX = (float) Math.cos(baseAngle);
                float axisZ = (float) Math.sin(baseAngle);
                shields.get(i).rotate(rot, axisX, 0, axisZ);
                shields.get(i).interpolation(3, 0);
            }

            // Array vertical drift: up and down 100-tick cycle
            float driftY = (float) Math.sin(ticksAlive * 0.0628) * 0.5f; // +-0.5 blocks
            for (int i = 0; i < shields.size(); i++) {
                double xOff = -4 + i * 2;
                shields.get(i).entity().teleport(c.clone().add(xOff, 5 + driftY, 0));
                amethystCores.get(i).entity().teleport(c.clone().add(xOff, 5 + driftY, 0));
            }

            // Amethyst core pulse: 0.9 to 1.15 scale over 20-tick cycle
            for (int i = 0; i < amethystCores.size(); i++) {
                float pulse = 0.6f + (float) Math.sin((ticksAlive + i * 4) * 0.314) * 0.075f;
                amethystCores.get(i).scale(pulse, pulse, 0.4f);
                amethystCores.get(i).interpolation(3, 0);
            }

            // Enchant from amethyst centers
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle core : amethystCores) {
                    c.getWorld().spawnParticle(Particle.ENCHANT,
                            core.entity().getLocation(), 4, 0.3, 0.3, 0.3, 0.4);
                }
            }

            // Grey-blue dust orbiting each shield
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < shields.size(); i++) {
                    Location sLoc = shields.get(i).entity().getLocation();
                    double orbitAngle = ticksAlive * 0.05 + i * 1.2;
                    Location orbitPt = sLoc.clone().add(
                            Math.cos(orbitAngle) * 1.8, Math.sin(orbitAngle) * 1.8, 0);
                    DisplayBuilder.dustParticles(orbitPt, 2, 0.3, 100, 120, 160, 0.8f);
                }
            }

            // Deepslate chip particles when shields weave through same plane
            if (ticksAlive % 15 == 0) {
                for (int i = 0; i < shields.size() - 1; i++) {
                    Location midpoint = shields.get(i).entity().getLocation().clone().add(1, 0, 0);
                    c.getWorld().spawnParticle(Particle.BLOCK, midpoint, 6, 0.5, 0.5, 0.2, 0,
                            Material.COBBLED_DEEPSLATE.createBlockData());
                }
            }

            // Staggered deepslate hit sounds every 5 seconds
            if (ticksAlive % 100 == (ticksAlive / 100 % 5) * 20) {
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_HIT, 0.5f, 0.7f);
            }

            // Elder guardian infrasound during downward drift
            if (ticksAlive % 200 == 100 && driftY < -0.2f) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.3f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FractureShieldArray(plugin); }
    }

    // ================================================================
    // 26. BLACKSTONE BULGE WALL -- Wall with pulsing central protrusion
    // ================================================================
    public static class BlackstoneBulgeWall extends BlockDisplayAttack {

        private BlockDisplayHandle flatLeft;
        private BlockDisplayHandle flatRight;
        private BlockDisplayHandle bulge;
        private BlockDisplayHandle bulgeSmooth;
        private final List<BlockDisplayHandle> reinforcements = new ArrayList<>();
        private final List<BlockDisplayHandle> impactStuds = new ArrayList<>();
        private final List<BlockDisplayHandle> cryingTop = new ArrayList<>();
        private final List<BlockDisplayHandle> edgeMarkers = new ArrayList<>();
        private float emergeProgress = 0;

        public BlackstoneBulgeWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blackstone_bulge_wall", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Flat left section: blackstone + deepslate
            flatLeft = displayBuilder.spawnBlock(center.clone().add(-3, -4, 0), Material.BLACKSTONE);
            flatLeft.scale(3.5f, 4.0f, 1.0f).glow(50, 50, 60).interpolation(3, 0);
            spawnedEntities.add(flatLeft.entity());

            // Flat right section
            flatRight = displayBuilder.spawnBlock(center.clone().add(3, -4, 0), Material.BLACKSTONE);
            flatRight.scale(3.5f, 4.0f, 1.0f).glow(50, 50, 60).interpolation(3, 0);
            spawnedEntities.add(flatRight.entity());

            // Central bulge: protrudes 2 blocks out from wall plane
            bulge = displayBuilder.spawnBlock(center.clone().add(0, -4, 1), Material.BLACKSTONE);
            bulge.scale(3.0f, 4.0f, 3.0f).glow(55, 50, 65).interpolation(2, 0);
            spawnedEntities.add(bulge.entity());

            // Polished blackstone smooth face on bulge
            bulgeSmooth = displayBuilder.spawnBlock(center.clone().add(0, -4, 2.5), Material.POLISHED_BLACKSTONE);
            bulgeSmooth.scale(2.8f, 3.8f, 0.4f).glow(65, 60, 75).interpolation(2, 0);
            spawnedEntities.add(bulgeSmooth.entity());

            // Obsidian reinforcements at shoulder joints
            double[][] reinforcePos = {{-1.5, -1, 1.2}, {1.5, -1, 1.2}, {-1.5, -3, 1.2}, {1.5, -3, 1.2},
                                       {-2, -2, 0.5}, {2, -2, 0.5}};
            for (double[] pos : reinforcePos) {
                BlockDisplayHandle r = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], pos[1], pos[2]), Material.OBSIDIAN);
                r.scale(0.7f, 0.7f, 0.7f).glow(40, 0, 60).interpolation(3, 0);
                reinforcements.add(r);
                spawnedEntities.add(r.entity());
            }

            // Netherite impact studs in 4 corners of bulge face
            double[][] studPos = {{-0.8, -1, 3}, {0.8, -1, 3}, {-0.8, -3, 3}, {0.8, -3, 3}};
            for (double[] pos : studPos) {
                BlockDisplayHandle s = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], pos[1], pos[2]), Material.NETHERITE_BLOCK);
                s.scale(0.4f, 0.4f, 0.4f).glow(20, 20, 30).interpolation(2, 0);
                impactStuds.add(s);
                spawnedEntities.add(s.entity());
            }

            // 3 crying obsidian at top edge of bulge
            for (int i = -1; i <= 1; i++) {
                BlockDisplayHandle cry = displayBuilder.spawnBlock(
                        center.clone().add(i * 0.8, -0.5, 2), Material.CRYING_OBSIDIAN);
                cry.scale(0.5f, 0.4f, 0.5f).glow(128, 0, 255).interpolation(3, 0);
                cryingTop.add(cry);
                spawnedEntities.add(cry.entity());
            }

            // Amethyst edge markers at flat wall outer corners
            for (double xOff : new double[]{-4.5, 4.5}) {
                BlockDisplayHandle marker = displayBuilder.spawnBlock(
                        center.clone().add(xOff, -2, 0), Material.AMETHYST_BLOCK);
                marker.scale(0.5f, 0.5f, 0.5f).glow(200, 100, 255).interpolation(3, 0);
                edgeMarkers.add(marker);
                spawnedEntities.add(marker.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise over 20 ticks
            if (ticksAlive <= 20) {
                emergeProgress = ticksAlive / 20.0f;
                float yOffset = emergeProgress * 4.0f;
                flatLeft.entity().teleport(c.clone().add(-3, -4 + yOffset, 0));
                flatRight.entity().teleport(c.clone().add(3, -4 + yOffset, 0));
                bulge.entity().teleport(c.clone().add(0, -4 + yOffset, 1));
                bulgeSmooth.entity().teleport(c.clone().add(0, -4 + yOffset, 2.5));
            }

            // Bulge pulse: extends +1 block outward over 15 ticks, retreats over 20 ticks
            if (ticksAlive > 20) {
                int pulseCycle = (ticksAlive - 20) % 35;
                float bulgeExtend;
                if (pulseCycle < 15) {
                    bulgeExtend = (pulseCycle / 15.0f); // 0 -> 1
                } else {
                    bulgeExtend = 1.0f - ((pulseCycle - 15) / 20.0f); // 1 -> 0
                }
                float zOffset = 1.0f + bulgeExtend;
                bulge.entity().teleport(c.clone().add(0, 0, zOffset));
                bulgeSmooth.entity().teleport(c.clone().add(0, 0, zOffset + 1.5f));

                // Flat sections micro-vibrate with each pulse
                float vibrate = bulgeExtend * 0.03f * (float) Math.sin(ticksAlive * 3);
                flatLeft.translate(-0.5f, -0.5f, vibrate);
                flatLeft.interpolation(2, 0);
                flatRight.translate(-0.5f, -0.5f, -vibrate);
                flatRight.interpolation(2, 0);

                // Heartbeat sound locked to pulse
                if (pulseCycle == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.9f);
                }
                if (pulseCycle == 14) {
                    DisplayBuilder.playSound(c.clone().add(0, 0, 2), Sound.BLOCK_STONE_PLACE, 0.5f, 0.5f);
                }

                // Crying obsidian tear intensity doubles at max extension
                if (ticksAlive % 3 == 0) {
                    int tearCount = (pulseCycle < 15 && pulseCycle > 10) ? 6 : 3;
                    for (BlockDisplayHandle cry : cryingTop) {
                        Location cryLoc = cry.entity().getLocation().add(0, -0.3, 0);
                        c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, cryLoc,
                                tearCount, 0.1, 0.5, 0.1, 0);
                    }
                }
            }

            // Large smoke from flat wall tops
            if (ticksAlive % 7 == 0 && ticksAlive > 20) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(-3, 1, 0), 2, 1, 0.2, 0.2, 0.01);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(3, 1, 0), 2, 1, 0.2, 0.2, 0.01);
            }

            // Sculk soul from wall base
            if (ticksAlive % 12 == 0 && ticksAlive > 20) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL,
                        c.clone().add(0, -0.3, 0), 3, 3, 0.1, 0.5, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlackstoneBulgeWall(plugin); }
    }

    // ================================================================
    // 27. DEEP PRISMARINE TIDE WALL -- Rolling wave barrier
    // ================================================================
    public static class DeepPrismarineTideWall extends BlockDisplayAttack {

        private BlockDisplayHandle waveBody;
        private BlockDisplayHandle waveCrest;
        private BlockDisplayHandle baseUndertow;
        private final List<BlockDisplayHandle> hollowFill = new ArrayList<>();
        private final List<BlockDisplayHandle> glassPanes = new ArrayList<>();
        private final List<BlockDisplayHandle> soulPatches = new ArrayList<>();
        private BlockDisplayHandle whitecap;
        private float rollOffset = 0;
        private int rollCycleCount = 0;

        public DeepPrismarineTideWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("deep_prismarine_tide_wall", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main wave body: dark prismarine, curved wave shape
            waveBody = displayBuilder.spawnBlock(center.clone().add(0, 0, 0), Material.DARK_PRISMARINE);
            waveBody.scale(8.0f, 5.0f, 1.5f).glow(0, 120, 140).interpolation(3, 0);
            spawnedEntities.add(waveBody.entity());

            // Wave crest: top 3 blocks fold forward
            waveCrest = displayBuilder.spawnBlock(center.clone().add(0, 3.5, 1), Material.DARK_PRISMARINE);
            waveCrest.scale(6.0f, 2.0f, 2.0f)
                    .rotate(0.35f, 1, 0, 0) // Tilted forward to simulate breaking crest
                    .glow(0, 140, 160).interpolation(3, 0);
            spawnedEntities.add(waveCrest.entity());

            // Obsidian undertow base
            baseUndertow = displayBuilder.spawnBlock(center.clone().add(0, -1.5, 0), Material.OBSIDIAN);
            baseUndertow.scale(8.0f, 1.5f, 1.0f).glow(40, 0, 60).interpolation(3, 0);
            spawnedEntities.add(baseUndertow.entity());

            // Blackstone hollow behind crest curl
            for (int i = 0; i < 4; i++) {
                double xOff = -2 + i * 1.5;
                BlockDisplayHandle fill = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 3, 0.5), Material.BLACKSTONE);
                fill.scale(1.2f, 1.5f, 1.0f).glow(40, 40, 50).interpolation(3, 0);
                hollowFill.add(fill);
                spawnedEntities.add(fill.entity());
            }

            // Soul soil patches in base
            for (int i = 0; i < 3; i++) {
                double xOff = -2.5 + i * 2.5;
                BlockDisplayHandle soul = displayBuilder.spawnBlock(
                        center.clone().add(xOff, -1.5, 0.2), Material.SOUL_SOIL);
                soul.scale(0.8f, 0.6f, 0.6f).glow(60, 40, 30).interpolation(3, 0);
                soulPatches.add(soul);
                spawnedEntities.add(soul.entity());
            }

            // Black stained glass in crest openings
            for (int i = 0; i < 4; i++) {
                double xOff = -1.5 + i;
                BlockDisplayHandle glass = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 4, 1.5), Material.BLACK_STAINED_GLASS_PANE);
                glass.scale(0.6f, 0.8f, 0.2f).glow(20, 0, 40).interpolation(3, 0);
                glassPanes.add(glass);
                spawnedEntities.add(glass.entity());
            }

            // Amethyst whitecap at wave peak
            whitecap = displayBuilder.spawnBlock(center.clone().add(0, 5, 1.5), Material.AMETHYST_BLOCK);
            whitecap.scale(1.0f, 0.5f, 0.5f).glow(200, 100, 255).interpolation(3, 0);
            spawnedEntities.add(whitecap.entity());

            DisplayBuilder.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rolling wave motion: body moves forward at 0.2 blocks/sec,
            // crest moves at 0.5 blocks/sec
            float bodySpeed = 0.01f;  // 0.2 blocks/sec at 20 tps
            float crestSpeed = 0.025f; // 0.5 blocks/sec at 20 tps

            rollOffset += bodySpeed;
            float crestExtraOffset = (ticksAlive % 120) * (crestSpeed - bodySpeed);

            // Reset when crest has moved 6 blocks forward
            if (crestExtraOffset >= 6.0f) {
                rollOffset = 0;
                rollCycleCount++;
                // Reset burst particle
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        c.clone().add(0, 2, 0), 20, 3, 2, 1, 0.1);
                DisplayBuilder.playSound(c, Sound.BLOCK_STONE_BREAK, 0.5f, 0.4f);
            }

            // Move wave body forward
            waveBody.entity().teleport(c.clone().add(0, 0, rollOffset));
            // Base stays put
            // Crest moves faster
            float totalCrestZ = rollOffset + Math.min(crestExtraOffset, 6.0f);
            waveCrest.entity().teleport(c.clone().add(0, 3.5, 1 + totalCrestZ * 0.5f));
            whitecap.entity().teleport(c.clone().add(0, 5, 1.5 + totalCrestZ * 0.5f));

            // Glass pane ripple: tint oscillation
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < glassPanes.size(); i++) {
                    int brightness = (int) (8 + Math.sin((ticksAlive + i * 10) * 0.4) * 5);
                    glassPanes.get(i).brightness(brightness, brightness);
                }
            }

            // Bubble pop at wave base
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.BUBBLE_POP,
                        c.clone().add(0, -0.5, rollOffset), 4, 3, 0.2, 0.5, 0.02);
            }

            // Tears from crest folding over
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                        c.clone().add(0, 4, 1 + totalCrestZ * 0.4f), 3, 2, 0.3, 0.5, 0);
            }

            // Large smoke in hollow behind crest
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(0, 3, -0.5 + rollOffset), 3, 2, 0.5, 0.3, 0.01);
            }

            // Underwater ambient loop + splash
            if (ticksAlive % 120 == 60) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_SPLASH, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeepPrismarineTideWall(plugin); }
    }

    // ================================================================
    // 28. SOUL VEIL -- Permeable soul soil veil with rotating blocks
    // ================================================================
    public static class SoulVeil extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> veilBlocks = new ArrayList<>();
        private final List<Boolean> rotateClockwise = new ArrayList<>();
        private final List<BlockDisplayHandle> anchorPosts = new ArrayList<>();
        private final List<BlockDisplayHandle> cryingInserts = new ArrayList<>();
        private final List<BlockDisplayHandle> hiddenAmethyst = new ArrayList<>();
        private BlockDisplayHandle denseSectionFill;

        public SoulVeil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_veil", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(1100);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 20 soul soil blocks: 10 wide x 2 tall, staggered with 0.5-block gaps
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 10; col++) {
                    double xOff = -4.5 + col;
                    double yOff = row * 1.5;
                    Location loc = center.clone().add(xOff, yOff, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                    h.scale(0.8f, 0.8f, 0.4f).glow(60, 40, 30).interpolation(2, 0);
                    veilBlocks.add(h);
                    spawnedEntities.add(h.entity());
                    // Left half clockwise, right half counterclockwise
                    rotateClockwise.add(col < 5);
                }
            }

            // 8 black concrete blocks filling left half more densely
            for (int i = 0; i < 4; i++) {
                double xOff = -4.5 + i;
                for (int row = 0; row < 2; row++) {
                    BlockDisplayHandle bc = displayBuilder.spawnBlock(
                            center.clone().add(xOff + 0.5, row * 1.5 + 0.3, 0.1), Material.BLACK_CONCRETE);
                    bc.scale(0.6f, 0.6f, 0.3f).glow(25, 25, 30).interpolation(2, 0);
                    spawnedEntities.add(bc.entity());
                }
            }

            // 6 obsidian anchor posts at corners and top-center
            double[][] anchorPos = {
                {-4.5, -0.3, 0}, {4.5, -0.3, 0}, {-4.5, 2.5, 0},
                {4.5, 2.5, 0}, {-0.5, 2.5, 0}, {0.5, 2.5, 0}
            };
            for (double[] pos : anchorPos) {
                BlockDisplayHandle a = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], pos[1], pos[2]), Material.OBSIDIAN);
                a.scale(0.5f, 0.5f, 0.5f).glow(40, 0, 60).interpolation(3, 0);
                anchorPosts.add(a);
                spawnedEntities.add(a.entity());
            }

            // 4 crying obsidian woven into center pattern
            for (int i = 0; i < 4; i++) {
                double xOff = -1.5 + i;
                BlockDisplayHandle cry = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 0.75, 0), Material.CRYING_OBSIDIAN);
                cry.scale(0.5f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                cryingInserts.add(cry);
                spawnedEntities.add(cry.entity());
            }

            // 2 hidden amethyst behind veil
            for (double xOff : new double[]{-1, 1}) {
                BlockDisplayHandle am = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 0.75, -0.3), Material.AMETHYST_BLOCK);
                am.scale(0.5f, 0.5f, 0.3f).glow(200, 100, 255).interpolation(3, 0);
                hiddenAmethyst.add(am);
                spawnedEntities.add(am.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_STEP, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Individual block Y-axis rotation: 2 deg/sec, alternating directions
            for (int i = 0; i < veilBlocks.size(); i++) {
                float direction = rotateClockwise.get(i) ? 1.0f : -1.0f;
                float rot = ticksAlive * 0.00175f * direction; // 2 deg/sec
                veilBlocks.get(i).rotate(rot, 0, 1, 0);
                veilBlocks.get(i).interpolation(3, 0);
            }

            // Whole veil drifts slowly downward: 0.02 blocks/sec
            float drift = ticksAlive * 0.001f; // 0.02 blocks/sec at 20 tps
            if (drift > 2.0f) drift = 2.0f; // Cap to prevent it going underground

            for (int i = 0; i < veilBlocks.size(); i++) {
                int row = i / 10;
                int col = i % 10;
                double xOff = -4.5 + col;
                double yOff = row * 1.5 - drift;
                veilBlocks.get(i).entity().teleport(c.clone().add(xOff, yOff, 0));
            }

            // Soul fire from every soul soil face at low density
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < veilBlocks.size(); i += 3) {
                    Location bLoc = veilBlocks.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, bLoc, 2, 0.2, 0.2, 0.1, 0.01);
                }
            }

            // Sculk soul drifting through gaps toward players
            if (ticksAlive % 8 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL,
                        c.clone().add(0, 0.75, 0.5), 3, 3, 0.5, 0.3, 0.02);
            }

            // Ash falling from top edge
            if (ticksAlive % 10 == 0) {
                c.getWorld().spawnParticle(Particle.ASH,
                        c.clone().add(0, 2.5, 0), 5, 4, 0.1, 0.3, 0);
            }

            // Ambient soul sand sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SOUL_SAND_STEP, 0.3f, 0.5f);
            }

            // Wither ambient every 10 seconds
            if (ticksAlive % 200 == 100) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_AMBIENT, 0.2f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulVeil(plugin); }
    }

    // ================================================================
    // 29. JAGGED PALISADE -- Row of 11 irregular posts rising in wave
    // ================================================================
    public static class JaggedPalisade extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> posts = new ArrayList<>();
        private final int[] postHeights = {5, 3, 7, 4, 6, 3, 5, 7, 4, 3, 6};
        private final int POST_COUNT = 11;
        private final List<BlockDisplayHandle> rubbleFill = new ArrayList<>();
        private boolean allRisen = false;

        public JaggedPalisade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("jagged_palisade", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 11 posts at 1-block spacing, each varying height
            for (int p = 0; p < POST_COUNT; p++) {
                List<BlockDisplayHandle> post = new ArrayList<>();
                double xOff = -5.5 + p;
                int height = postHeights[p];

                for (int y = 0; y < height; y++) {
                    // Base is 1x2, narrows to 1x1
                    float taper = 1.0f - (y * 0.08f);
                    Location loc = center.clone().add(xOff, -height + y, 0);
                    Material mat;
                    if (y == height - 1) {
                        // Cap with polished blackstone on alternating posts
                        mat = (p % 2 == 0) ? Material.POLISHED_BLACKSTONE : Material.BLACKSTONE;
                    } else if (p < 3 || p == 5 || p == 8) {
                        // Tallest 3 posts get obsidian reinforcement plates
                        mat = (y == height / 2) ? Material.OBSIDIAN : Material.BLACKSTONE;
                    } else {
                        mat = Material.BLACKSTONE;
                    }
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(taper, 1.0f, taper * 0.6f).glow(50, 50, 60).interpolation(2, 0);
                    post.add(h);
                    spawnedEntities.add(h.entity());
                }
                posts.add(post);

                // Netherite anchor at base of tallest 4 posts
                if (height >= 5) {
                    BlockDisplayHandle anchor = displayBuilder.spawnBlock(
                            center.clone().add(xOff, -height, 0), Material.NETHERITE_BLOCK);
                    anchor.scale(0.5f, 0.5f, 0.5f).glow(20, 20, 30).interpolation(2, 0);
                    spawnedEntities.add(anchor.entity());
                }
            }

            // Cobbled deepslate rubble between post bases
            for (int i = 0; i < 8; i++) {
                double xOff = -4.5 + i * 1.2;
                BlockDisplayHandle r = displayBuilder.spawnBlock(
                        center.clone().add(xOff, -0.3, 0), Material.COBBLED_DEEPSLATE);
                r.scale(0.5f, 0.3f, 0.5f).glow(40, 40, 50).interpolation(2, 0);
                rubbleFill.add(r);
                spawnedEntities.add(r.entity());
            }

            // Crying obsidian on posts 2 and 9 tips
            BlockDisplayHandle cry1 = displayBuilder.spawnBlock(
                    center.clone().add(-4.5, -postHeights[1], 0), Material.CRYING_OBSIDIAN);
            cry1.scale(0.4f, 0.5f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(cry1.entity());

            BlockDisplayHandle cry2 = displayBuilder.spawnBlock(
                    center.clone().add(2.5, -postHeights[8], 0), Material.CRYING_OBSIDIAN);
            cry2.scale(0.4f, 0.5f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(cry2.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Staggered rise: each post starts 3 ticks after the previous, left to right
            // Each post takes 20 ticks to fully extend
            boolean allUp = true;
            for (int p = 0; p < POST_COUNT; p++) {
                int startTick = p * 3;
                int postAge = ticksAlive - startTick;

                if (postAge < 0) {
                    allUp = false;
                    continue;
                }

                List<BlockDisplayHandle> post = posts.get(p);
                int height = postHeights[p];

                if (postAge <= 20) {
                    allUp = false;
                    float progress = postAge / 20.0f;
                    float yOffset = progress * height;

                    for (int y = 0; y < post.size(); y++) {
                        double xOff = -5.5 + p;
                        Location target = c.clone().add(xOff, -height + y + yOffset, 0);
                        post.get(y).entity().teleport(target);
                    }

                    // Rise particles per post
                    if (postAge % 5 == 0) {
                        double xOff = -5.5 + p;
                        c.getWorld().spawnParticle(Particle.BLOCK,
                                c.clone().add(xOff, yOffset, 0), 10, 0.3, 0.3, 0.3, 0,
                                Material.BLACKSTONE.createBlockData());
                    }

                    // Sound on first tick of each post
                    if (postAge == 0) {
                        DisplayBuilder.playSound(c.clone().add(-5.5 + p, 0, 0),
                                Sound.BLOCK_STONE_PLACE, 0.6f, 0.8f + p * 0.03f);
                    }
                }
            }

            if (allUp && !allRisen) allRisen = true;

            // After all posts are up: uniform sway on Z-axis, +-4 degrees, 50-tick cycle
            if (allRisen) {
                float sway = (float) Math.sin(ticksAlive * 0.1257) * 0.07f; // +-4 degrees

                for (int p = 0; p < POST_COUNT; p++) {
                    List<BlockDisplayHandle> post = posts.get(p);
                    // Individual micro-variation +-0.5 degrees
                    float microVar = (float) Math.sin((ticksAlive + p * 11) * 0.2) * 0.0087f;
                    for (BlockDisplayHandle h : post) {
                        h.rotate(sway + microVar, 0, 0, 1);
                        h.interpolation(3, 0);
                    }
                }

                // Heartbeat during sway
                if (ticksAlive % 60 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.7f);
                }
            }

            // Dark dust drifting upward along palisade line
            if (ticksAlive % 5 == 0 && allRisen) {
                DisplayBuilder.darkPurpleDust(c.clone().add(0, 2, 0), 4, 5.0);
            }

            // Tears from crying obsidian post tips
            if (ticksAlive % 4 == 0 && allRisen) {
                c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                        c.clone().add(-4.5, postHeights[1], 0), 2, 0.1, 0.2, 0.1, 0);
                c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                        c.clone().add(2.5, postHeights[8], 0), 2, 0.1, 0.2, 0.1, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new JaggedPalisade(plugin); }
    }

    // ================================================================
    // 30. MIRROR VOID PANEL -- Slowly rotating glazed terracotta mirror
    // ================================================================
    public static class MirrorVoidPanel extends BlockDisplayAttack {

        private BlockDisplayHandle panelFace;
        private BlockDisplayHandle borderFrame;
        private final List<BlockDisplayHandle> fins = new ArrayList<>();
        private final List<BlockDisplayHandle> amethystNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> cornerReinforcements = new ArrayList<>();
        private BlockDisplayHandle backGlow;

        public MirrorVoidPanel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mirror_void_panel", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn 3 blocks above ground, floating 2 blocks away from surface
            Location panelCenter = center.clone().add(0, 3, 2);

            // Main panel: 5x5 chamfered octagonal face (black glazed terracotta)
            panelFace = displayBuilder.spawnBlock(panelCenter, Material.BLACK_GLAZED_TERRACOTTA);
            panelFace.scale(4.5f, 4.5f, 0.3f).glow(15, 15, 20).interpolation(3, 0);
            spawnedEntities.add(panelFace.entity());

            // Polished blackstone border frame
            borderFrame = displayBuilder.spawnBlock(panelCenter, Material.POLISHED_BLACKSTONE);
            borderFrame.scale(5.0f, 5.0f, 0.2f).glow(60, 60, 70).interpolation(3, 0);
            spawnedEntities.add(borderFrame.entity());

            // 6 netherite reinforcements: 4 corners + 2 mid-edges
            double[][] reinforcePos = {
                {-2.3, -2.3, 0}, {2.3, -2.3, 0}, {-2.3, 2.3, 0}, {2.3, 2.3, 0},
                {0, -2.5, 0}, {0, 2.5, 0}
            };
            for (double[] pos : reinforcePos) {
                BlockDisplayHandle r = displayBuilder.spawnBlock(
                        panelCenter.clone().add(pos[0], pos[1], pos[2]), Material.NETHERITE_BLOCK);
                r.scale(0.5f, 0.5f, 0.4f).glow(20, 20, 30).interpolation(3, 0);
                cornerReinforcements.add(r);
                spawnedEntities.add(r.entity());
            }

            // 4 obsidian fins extending from chamfered corners
            double[][] finPos = {{-2, -2, 0.3}, {2, -2, 0.3}, {-2, 2, 0.3}, {2, 2, 0.3}};
            for (double[] pos : finPos) {
                BlockDisplayHandle fin = displayBuilder.spawnBlock(
                        panelCenter.clone().add(pos[0], pos[1], pos[2]), Material.OBSIDIAN);
                fin.scale(0.4f, 0.4f, 1.0f).glow(40, 0, 60).interpolation(3, 0);
                fins.add(fin);
                spawnedEntities.add(fin.entity());
            }

            // 3 amethyst imperfections in asymmetric triangle pattern
            double[][] amethystPos = {{-0.8, -0.5, 0.15}, {1.0, 0.8, 0.15}, {-0.3, 1.5, 0.15}};
            for (double[] pos : amethystPos) {
                BlockDisplayHandle am = displayBuilder.spawnBlock(
                        panelCenter.clone().add(pos[0], pos[1], pos[2]), Material.AMETHYST_BLOCK);
                am.scale(0.5f, 0.5f, 0.3f).glow(200, 100, 255).interpolation(3, 0);
                amethystNodes.add(am);
                spawnedEntities.add(am.entity());
            }

            // Purple stained glass backlit glow layer
            backGlow = displayBuilder.spawnBlock(panelCenter.clone().add(0, 0, -0.5), Material.PURPLE_STAINED_GLASS);
            backGlow.scale(4.0f, 4.0f, 0.15f).glow(160, 0, 200).interpolation(3, 0);
            spawnedEntities.add(backGlow.entity());

            DisplayBuilder.playSound(panelCenter, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            Location panelCenter = c.clone().add(0, 3, 2);

            // Very slow Y-axis rotation: 1.5 deg/sec
            float panelRot = ticksAlive * 0.00131f; // 1.5 deg/sec

            panelFace.rotate(panelRot, 0, 1, 0);
            panelFace.interpolation(3, 0);
            borderFrame.rotate(panelRot, 0, 1, 0);
            borderFrame.interpolation(3, 0);
            backGlow.rotate(panelRot, 0, 1, 0);
            backGlow.interpolation(3, 0);

            // Fins extend and retract: +-0.5 blocks on 25-tick cycle, angled outward
            for (int i = 0; i < fins.size(); i++) {
                float finPulse = (float) Math.sin((ticksAlive + i * 6) * 0.251) * 0.5f; // 25-tick cycle
                double[][] finBasePos = {{-2, -2, 0.3}, {2, -2, 0.3}, {-2, 2, 0.3}, {2, 2, 0.3}};
                double zOff = finBasePos[i][2] + finPulse;
                fins.get(i).entity().teleport(panelCenter.clone().add(
                        finBasePos[i][0], finBasePos[i][1], zOff));
                fins.get(i).rotate(panelRot, 0, 1, 0);
                fins.get(i).interpolation(3, 0);
            }

            // Amethyst sequential pulse: node 1, then 2, then 3, each 10 ticks apart
            for (int i = 0; i < amethystNodes.size(); i++) {
                int pulsePhase = (ticksAlive + i * 10) % 30;
                float pulseScale = 0.5f + (float) Math.sin(pulsePhase * 0.209) * 0.1f; // 0.4 to 0.6
                amethystNodes.get(i).scale(pulseScale, pulseScale, 0.3f);
                amethystNodes.get(i).interpolation(3, 0);
            }

            // Enchant particles streaming from amethyst nodes
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle am : amethystNodes) {
                    c.getWorld().spawnParticle(Particle.ENCHANT,
                            am.entity().getLocation(), 5, 0.2, 0.2, 0.2, 0.5);
                }
            }

            // Black dust halo orbiting the panel
            if (ticksAlive % 5 == 0) {
                double haloAngle = ticksAlive * 0.03;
                Location haloPt = panelCenter.clone().add(
                        Math.cos(haloAngle) * 3.0, Math.sin(haloAngle) * 3.0, 0);
                DisplayBuilder.dustParticles(haloPt, 2, 0.3, 5, 5, 5, 2.5f);
            }

            // Reverse portal pooling at panel base during rotation
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        panelCenter.clone().add(0, -2.5, 0), 4, 0.5, 0.1, 0.5, 0.02);
            }

            // Amethyst resonate sound when each node pulses
            if (ticksAlive % 30 == 0 || ticksAlive % 30 == 10 || ticksAlive % 30 == 20) {
                DisplayBuilder.playSound(panelCenter, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.4f, 0.5f);
            }

            // Enderman stare when panel faces toward spawn center
            float facingAngle = (panelRot * 180 / (float) Math.PI) % 360;
            if (facingAngle > 170 && facingAngle < 190 && ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(panelCenter, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MirrorVoidPanel(plugin); }
    }
}
