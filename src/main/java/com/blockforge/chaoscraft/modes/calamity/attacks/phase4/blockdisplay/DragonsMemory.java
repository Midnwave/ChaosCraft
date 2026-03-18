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
 * Phase 4D Block Display -- GROUP 9: THE DRAGON'S MEMORY (Structures #81-90)
 * Void Emperor (Boss 4) arena: echo-structures of the three absorbed bosses.
 * Floating at Y+6 to Y+18, spectral aesthetic via PORTAL particle floods.
 * Each echo bobs asynchronously with sinusoidal Y oscillation.
 *
 * Boss echoes: Voidmaw (fracture gate, spine), DoG (crystal lattice, crown),
 * Dweller (brimstone throne, armrest, pillar).
 * NO status effects. Damage in HP (not hearts). AxisAngle4f only.
 */
public final class DragonsMemory {

    private DragonsMemory() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidmawFractureGateA(plugin));
        registry.register(new VoidmawFractureGateB(plugin));
        registry.register(new VoidmawSpiralRib(plugin));
        registry.register(new DogCrystalLatticeNodeA(plugin));
        registry.register(new DogCrystalLatticeNodeB(plugin));
        registry.register(new DogBrokenCrown(plugin));
        registry.register(new DwellerBrimstoneThrone(plugin));
        registry.register(new DwellerArmrestShard(plugin));
        registry.register(new DwellerSanctumPillar(plugin));
        registry.register(new MemoryConvergenceAura(plugin));
    }

    // ================================================================
    // #81 -- VOIDMAW'S ECHO: FRACTURE GATE (FRAGMENT 1)
    //        Partial portal archway. Crying_obsidian pillars, obsidian
    //        lintel, purple_stained_glass fill. Tilted 15 deg X, 8 deg Z.
    //        NW of center, radius 20, Y+8. 157-tick bob period.
    // ================================================================
    public static class VoidmawFractureGateA extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> gateBlocks = new ArrayList<>();

        public VoidmawFractureGateA(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("voidmaw_fracture_gate_a", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location gateCenter = center.clone().add(-14, 8, -14); // NW

            // Left pillar: 2 crying_obsidian stacked
            BlockDisplayHandle leftLower = displayBuilder.spawnBlock(gateCenter.clone().add(-2.5, 0, 0), Material.CRYING_OBSIDIAN);
            leftLower.scale(1.4f, 3.0f, 1.4f).glow(128, 0, 255).interpolation(3, 0);
            gateBlocks.add(leftLower);
            spawnedEntities.add(leftLower.entity());

            BlockDisplayHandle leftUpper = displayBuilder.spawnBlock(gateCenter.clone().add(-2.5, 3, 0), Material.CRYING_OBSIDIAN);
            leftUpper.scale(1.2f, 2.5f, 1.2f).glow(128, 0, 255).interpolation(3, 0);
            gateBlocks.add(leftUpper);
            spawnedEntities.add(leftUpper.entity());

            // Right pillar: lower only (broken -- upper half missing)
            BlockDisplayHandle rightLower = displayBuilder.spawnBlock(gateCenter.clone().add(2.5, 0, 0), Material.CRYING_OBSIDIAN);
            rightLower.scale(1.4f, 3.0f, 1.4f).glow(128, 0, 255).interpolation(3, 0);
            gateBlocks.add(rightLower);
            spawnedEntities.add(rightLower.entity());

            // Broken upper-right remnant floating offset
            BlockDisplayHandle brokenRemnant = displayBuilder.spawnBlock(gateCenter.clone().add(2.8, 4.5, 0), Material.OBSIDIAN);
            brokenRemnant.scale(0.8f, 0.5f, 1.4f).glow(60, 0, 120).interpolation(3, 0);
            gateBlocks.add(brokenRemnant);
            spawnedEntities.add(brokenRemnant.entity());

            // Lintel: slightly tilted 3 deg on Z
            BlockDisplayHandle lintel = displayBuilder.spawnBlock(gateCenter.clone().add(0, 5.5, 0), Material.OBSIDIAN);
            lintel.scale(5.0f, 0.8f, 1.2f).rotate((float) Math.toRadians(3), 0, 0, 1)
                  .glow(60, 0, 120).interpolation(3, 0);
            gateBlocks.add(lintel);
            spawnedEntities.add(lintel.entity());

            // Gate interior: 3 purple_stained_glass panes
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle glass = displayBuilder.spawnBlock(
                        gateCenter.clone().add(0, 1.0 + i * 1.8, 0), Material.PURPLE_STAINED_GLASS);
                glass.scale(1.8f, 2.0f, 0.3f).glow(180, 0, 255).interpolation(3, 0);
                gateBlocks.add(glass);
                spawnedEntities.add(glass.entity());
            }

            // Floating fragment cluster: 5 crying_obsidian chunks
            for (int i = 0; i < 5; i++) {
                double fx = 2.5 + (Math.random() - 0.5) * 3.0;
                double fy = 3.0 + (Math.random() - 0.5) * 2.0;
                double fz = (Math.random() - 0.5) * 2.0;
                float fScale = 0.3f + (float) (Math.random() * 0.4);
                BlockDisplayHandle frag = displayBuilder.spawnBlock(
                        gateCenter.clone().add(fx, fy, fz), Material.CRYING_OBSIDIAN);
                frag.scale(fScale, fScale, fScale).glow(128, 0, 255).interpolation(3, 0);
                gateBlocks.add(frag);
                spawnedEntities.add(frag.entity());
            }

            // Corner amethyst accents
            BlockDisplayHandle cornerA = displayBuilder.spawnBlock(gateCenter.clone().add(-2.5, 5.5, 0), Material.AMETHYST_BLOCK);
            cornerA.scale(0.8f, 0.8f, 0.8f).glow(180, 0, 255).interpolation(3, 0);
            gateBlocks.add(cornerA);
            spawnedEntities.add(cornerA.entity());

            BlockDisplayHandle cornerB = displayBuilder.spawnBlock(gateCenter.clone().add(2.5, 5.5, 0), Material.AMETHYST_BLOCK);
            cornerB.scale(0.8f, 0.8f, 0.8f).glow(180, 0, 255).interpolation(3, 0);
            gateBlocks.add(cornerB);
            spawnedEntities.add(cornerB.entity());

            DisplayBuilder.playSound(gateCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 0.15f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Y oscillation: 0.6 * sin(tick * 0.04), period 157 ticks
            float yOsc = 0.6f * (float) Math.sin(ticksAlive * 0.04);
            float prevYOsc = 0.6f * (float) Math.sin((ticksAlive - 1) * 0.04);
            float yDelta = yOsc - prevYOsc;

            for (BlockDisplayHandle h : gateBlocks) {
                BlockDisplay bd = h.entity();
                bd.teleport(bd.getLocation().clone().add(0, yDelta, 0));
            }

            // PORTAL flood inside gate
            Location gateCenter = center.clone().add(-14, 8 + yOsc, -14);
            w.spawnParticle(Particle.PORTAL, gateCenter, 8, 1.8, 2.0, 0.5, 0);
            w.spawnParticle(Particle.WITCH, gateCenter, 3, 2.5, 2.5, 2.5, 0);
            w.spawnParticle(Particle.DRAGON_BREATH, gateCenter, 2, 3.0, 3.0, 3.0, 0);

            if (ticksAlive % 180 == 0) {
                DisplayBuilder.playSound(gateCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 0.15f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidmawFractureGateA(plugin); }
    }

    // ================================================================
    // #82 -- VOIDMAW'S ECHO: FRACTURE GATE (FRAGMENT 2)
    //        Missing right-side pieces. 25 blocks NE of Fragment 1,
    //        Y+11, tilted opposite. Phase-shifted bob from Fragment 1.
    // ================================================================
    public static class VoidmawFractureGateB extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> fragmentBlocks = new ArrayList<>();

        public VoidmawFractureGateB(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("voidmaw_fracture_gate_b", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location fragCenter = center.clone().add(11, 11, -14); // NE of Fragment 1

            // Floating right upper pillar
            BlockDisplayHandle rightUpper = displayBuilder.spawnBlock(fragCenter, Material.CRYING_OBSIDIAN);
            rightUpper.scale(1.2f, 2.5f, 1.2f).glow(128, 0, 255).interpolation(3, 0);
            fragmentBlocks.add(rightUpper);
            spawnedEntities.add(rightUpper.entity());

            // Right half of broken lintel
            BlockDisplayHandle lintelShard = displayBuilder.spawnBlock(fragCenter.clone().add(0, 2.5, 0), Material.OBSIDIAN);
            lintelShard.scale(2.2f, 0.8f, 1.2f).glow(60, 0, 120).interpolation(3, 0);
            fragmentBlocks.add(lintelShard);
            spawnedEntities.add(lintelShard.entity());

            // 6 small obsidian shard fragments
            for (int i = 0; i < 6; i++) {
                double dx = (Math.random() - 0.5) * 8.0;
                double dy = (Math.random() - 0.5) * 4.0;
                double dz = (Math.random() - 0.5) * 4.0;
                float scale = 0.2f + (float) (Math.random() * 0.3);
                BlockDisplayHandle shard = displayBuilder.spawnBlock(
                        fragCenter.clone().add(dx, dy, dz), Material.OBSIDIAN);
                shard.scale(scale, scale, scale).glow(60, 0, 120).interpolation(3, 0);
                fragmentBlocks.add(shard);
                spawnedEntities.add(shard.entity());
            }

            // Purple stained glass pane fragment
            BlockDisplayHandle glassFrag = displayBuilder.spawnBlock(fragCenter.clone().add(1, -1, 0), Material.PURPLE_STAINED_GLASS);
            glassFrag.scale(1.2f, 1.5f, 0.3f).glow(180, 0, 255).interpolation(3, 0);
            fragmentBlocks.add(glassFrag);
            spawnedEntities.add(glassFrag.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Y oscillation: same period as Fragment 1 but phase shifted by 1.5 rad
            float yOsc = 0.6f * (float) Math.sin(ticksAlive * 0.04 + 1.5);
            float prevYOsc = 0.6f * (float) Math.sin((ticksAlive - 1) * 0.04 + 1.5);
            float yDelta = yOsc - prevYOsc;

            for (BlockDisplayHandle h : fragmentBlocks) {
                BlockDisplay bd = h.entity();
                bd.teleport(bd.getLocation().clone().add(0, yDelta, 0));
            }

            Location fragCenter = center.clone().add(11, 11 + yOsc, -14);
            w.spawnParticle(Particle.PORTAL, fragCenter, 6, 2.0, 2.0, 2.0, 0);

            if (ticksAlive % 180 == 0) {
                DisplayBuilder.playSound(fragCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 0.15f, 0.65f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidmawFractureGateB(plugin); }
    }

    // ================================================================
    // #83 -- VOIDMAW'S ECHO: SPIRAL RIB (Voidmaw's Spine)
    //        Curved spine of 9 black_concrete rib segments + 8 crying
    //        obsidian intercostals. South of center, radius 18, Y+6-Y+13.
    //        Rotates 0.08 deg/tick. 180-tick bob.
    // ================================================================
    public static class VoidmawSpiralRib extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spineBlocks = new ArrayList<>();

        public VoidmawSpiralRib(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("voidmaw_spiral_rib", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location spineBase = center.clone().add(0, 6, 18); // South

            // 9 rib segments forming a helical arc
            for (int i = 0; i < 9; i++) {
                double arcAngle = Math.toRadians(i * 18.0);
                double yOff = (i / 8.0) * 7.0; // Y+6 to Y+13
                float segRot = (float) Math.toRadians(i * 18.0);

                BlockDisplayHandle rib = displayBuilder.spawnBlock(
                        spineBase.clone().add(Math.sin(arcAngle) * 2.0, yOff, Math.cos(arcAngle) * 0.5),
                        Material.BLACK_CONCRETE);
                rib.scale(0.8f, 0.8f, 3.5f).rotate(segRot, 1, 0, 0)
                   .glow(30, 0, 60).interpolation(3, 0);
                spineBlocks.add(rib);
                spawnedEntities.add(rib.entity());

                // Intercostal crying obsidian between ribs
                if (i < 8) {
                    BlockDisplayHandle interc = displayBuilder.spawnBlock(
                            spineBase.clone().add(0, yOff + 0.4, 0), Material.CRYING_OBSIDIAN);
                    interc.scale(0.3f, 2.0f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                    spineBlocks.add(interc);
                    spawnedEntities.add(interc.entity());
                }
            }

            DisplayBuilder.playSound(spineBase, Sound.ENTITY_WARDEN_AMBIENT, 0.08f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Slow rotation: 0.08 deg/tick
            float rot = (float) Math.toRadians(ticksAlive * 0.08);
            for (BlockDisplayHandle h : spineBlocks) {
                h.rotate(rot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Y oscillation: 0.4 * sin(tick * 0.035), period 180 ticks
            float yOsc = 0.4f * (float) Math.sin(ticksAlive * 0.035);
            Location spineCenter = center.clone().add(0, 9.5 + yOsc, 18);

            w.spawnParticle(Particle.PORTAL, spineCenter, 6, 2.0, 4.0, 2.0, 0);
            w.spawnParticle(Particle.SMOKE, spineCenter, 3, 1.5, 1.5, 1.5, -0.02);

            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(spineCenter, Sound.ENTITY_WARDEN_AMBIENT, 0.08f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidmawSpiralRib(plugin); }
    }

    // ================================================================
    // #84 -- DOG'S ECHO: CRYSTAL LATTICE NODE A (Collapsed octahedron)
    //        East of center, radius 22, Y+9. Partially collapsed --
    //        western equatorial block displaced, diagonal fills missing.
    //        Rotates 0.3 deg/tick. 165-tick bob.
    // ================================================================
    public static class DogCrystalLatticeNodeA extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> nodeBlocks = new ArrayList<>();

        public DogCrystalLatticeNodeA(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_crystal_lattice_node_a", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location nodeCenter = center.clone().add(22, 9, 0);

            // Top vertex: calcite
            BlockDisplayHandle topV = displayBuilder.spawnBlock(nodeCenter.clone().add(0, 2.0, 0), Material.CALCITE);
            topV.scale(0.8f, 2.0f, 0.8f).glow(200, 200, 255).interpolation(3, 0);
            nodeBlocks.add(topV);
            spawnedEntities.add(topV.entity());

            // Bottom vertex: calcite
            BlockDisplayHandle botV = displayBuilder.spawnBlock(nodeCenter.clone().add(0, -2.0, 0), Material.CALCITE);
            botV.scale(0.8f, 2.0f, 0.8f).glow(200, 200, 255).interpolation(3, 0);
            nodeBlocks.add(botV);
            spawnedEntities.add(botV.entity());

            // 4 equatorial blocks: white_concrete (3 in place, 1 displaced)
            double[][] eqPos = {{0, 0, 1.5}, {0, 0, -1.5}, {1.5, 0, 0}};
            for (double[] off : eqPos) {
                BlockDisplayHandle eq = displayBuilder.spawnBlock(nodeCenter.clone().add(off[0], off[1], off[2]), Material.WHITE_CONCRETE);
                eq.scale(1.8f, 1.8f, 1.8f).glow(220, 220, 255).interpolation(3, 0);
                nodeBlocks.add(eq);
                spawnedEntities.add(eq.entity());
            }

            // Displaced western block
            BlockDisplayHandle displaced = displayBuilder.spawnBlock(nodeCenter.clone().add(-4.0, 0, 0), Material.WHITE_CONCRETE);
            displaced.scale(1.8f, 1.8f, 1.8f).glow(220, 220, 255).interpolation(3, 0);
            nodeBlocks.add(displaced);
            spawnedEntities.add(displaced.entity());

            // 6 diagonal fills (2 missing on west side)
            double[][] diagPos = {
                {1.0, 1.0, 1.0}, {1.0, 1.0, -1.0}, {1.0, -1.0, 1.0},
                {1.0, -1.0, -1.0}, {-1.0, 1.0, 1.0}, {-1.0, -1.0, -1.0}
            };
            for (double[] off : diagPos) {
                BlockDisplayHandle diag = displayBuilder.spawnBlock(nodeCenter.clone().add(off[0], off[1], off[2]), Material.AMETHYST_BLOCK);
                diag.scale(1.2f, 1.2f, 1.2f).glow(180, 0, 255).interpolation(3, 0);
                nodeBlocks.add(diag);
                spawnedEntities.add(diag.entity());
            }

            // Horizontal calcite spike (collapsed orientation)
            BlockDisplayHandle hSpike = displayBuilder.spawnBlock(nodeCenter.clone().add(-2.0, 0.5, 0), Material.CALCITE);
            hSpike.scale(0.8f, 0.8f, 2.0f).rotate((float) Math.toRadians(90), 0, 0, 1)
                  .glow(200, 200, 255).interpolation(3, 0);
            nodeBlocks.add(hSpike);
            spawnedEntities.add(hSpike.entity());

            DisplayBuilder.playSound(nodeCenter, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.1f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Self-rotate 0.3 deg/tick
            float rot = (float) Math.toRadians(ticksAlive * 0.3);
            for (BlockDisplayHandle h : nodeBlocks) {
                h.rotate(rot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Y oscillation: 0.5 * sin(tick * 0.038), period 165 ticks
            float yOsc = 0.5f * (float) Math.sin(ticksAlive * 0.038);
            Location nodePos = center.clone().add(22, 9 + yOsc, 0);

            w.spawnParticle(Particle.TOTEM_OF_UNDYING, nodePos, 4, 2.0, 2.0, 2.0, 0);
            w.spawnParticle(Particle.CRIT, nodePos, 2, 1.5, 1.5, 1.5, 0);
            w.spawnParticle(Particle.PORTAL, nodePos, 3, 2.0, 2.0, 2.0, 0);

            if (ticksAlive % 150 == 0) {
                DisplayBuilder.playSound(nodePos, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.1f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DogCrystalLatticeNodeA(plugin); }
    }

    // ================================================================
    // #85 -- DOG'S ECHO: CRYSTAL LATTICE NODE B (Intact octahedron)
    //        ENE of center, radius 17, Y+13. Fully intact with cyan
    //        glass panels. Lattice arm connecting to Node A (broken).
    //        Counter-rotates at -0.3 deg/tick. Phase-shifted bob.
    // ================================================================
    public static class DogCrystalLatticeNodeB extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> nodeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> armSegments = new ArrayList<>();

        public DogCrystalLatticeNodeB(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_crystal_lattice_node_b", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location nodeCenter = center.clone().add(17, 13, -8); // ENE

            // Full octahedron: vertices + equatorial
            BlockDisplayHandle topV = displayBuilder.spawnBlock(nodeCenter.clone().add(0, 2.0, 0), Material.CALCITE);
            topV.scale(0.8f, 2.0f, 0.8f).glow(200, 200, 255).interpolation(3, 0);
            nodeBlocks.add(topV);
            spawnedEntities.add(topV.entity());

            BlockDisplayHandle botV = displayBuilder.spawnBlock(nodeCenter.clone().add(0, -2.0, 0), Material.CALCITE);
            botV.scale(0.8f, 2.0f, 0.8f).glow(200, 200, 255).interpolation(3, 0);
            nodeBlocks.add(botV);
            spawnedEntities.add(botV.entity());

            // 4 equatorial amethyst_block masses
            double[][] eqPos = {{1.5, 0, 0}, {-1.5, 0, 0}, {0, 0, 1.5}, {0, 0, -1.5}};
            for (double[] off : eqPos) {
                BlockDisplayHandle eq = displayBuilder.spawnBlock(nodeCenter.clone().add(off[0], off[1], off[2]), Material.AMETHYST_BLOCK);
                eq.scale(1.8f, 1.8f, 1.8f).glow(180, 0, 255).interpolation(3, 0);
                nodeBlocks.add(eq);
                spawnedEntities.add(eq.entity());
            }

            // 8 cyan_stained_glass face panels
            double[][] panelPos = {
                {0.8, 1.0, 0.8}, {0.8, 1.0, -0.8}, {-0.8, 1.0, 0.8}, {-0.8, 1.0, -0.8},
                {0.8, -1.0, 0.8}, {0.8, -1.0, -0.8}, {-0.8, -1.0, 0.8}, {-0.8, -1.0, -0.8}
            };
            for (double[] off : panelPos) {
                BlockDisplayHandle panel = displayBuilder.spawnBlock(nodeCenter.clone().add(off[0], off[1], off[2]),
                        Material.CYAN_STAINED_GLASS);
                panel.scale(1.5f, 1.5f, 0.15f).glow(0, 200, 255).interpolation(3, 0);
                nodeBlocks.add(panel);
                spawnedEntities.add(panel.entity());
            }

            // Broken lattice arm: 3 calcite segments toward Node A
            for (int i = 0; i < 3; i++) {
                double t = (i + 1) / 4.0;
                double ax = 22 + (17 - 22) * t;
                double az = 0 + (-8 - 0) * t;
                double ay = 9 + (13 - 9) * t;
                BlockDisplayHandle seg = displayBuilder.spawnBlock(center.clone().add(ax, ay, az), Material.CALCITE);
                seg.scale(0.4f, 0.4f, 2.5f).glow(200, 200, 255).interpolation(3, 0);
                armSegments.add(seg);
                spawnedEntities.add(seg.entity());
            }

            DisplayBuilder.playSound(nodeCenter, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.2f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Counter-rotate at -0.3 deg/tick
            float rot = (float) Math.toRadians(ticksAlive * -0.3);
            for (BlockDisplayHandle h : nodeBlocks) {
                h.rotate(rot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Phase-shifted Y oscillation
            float yOsc = 0.5f * (float) Math.sin(ticksAlive * 0.038 + 0.6);
            Location nodePos = center.clone().add(17, 13 + yOsc, -8);

            // Lattice arm Y jitter
            for (BlockDisplayHandle seg : armSegments) {
                float segYOsc = 0.3f * (float) Math.sin(ticksAlive * 0.05);
                BlockDisplay bd = seg.entity();
                bd.teleport(bd.getLocation().clone().add(0, segYOsc * 0.01, 0));
            }

            w.spawnParticle(Particle.TOTEM_OF_UNDYING, nodePos, 5, 2.0, 2.0, 2.0, 0);
            w.spawnParticle(Particle.CRIT, nodePos, 3, 1.5, 1.5, 1.5, 0);
            w.spawnParticle(Particle.END_ROD, nodePos, 2, 2.0, 2.0, 2.0, 0);

            if (ticksAlive % 130 == 0) {
                DisplayBuilder.playSound(nodePos, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.2f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DogCrystalLatticeNodeB(plugin); }
    }

    // ================================================================
    // #86 -- DOG'S ECHO: BROKEN CROWN (Hexagonal crown)
    //        Above Nodes A+B, Y+18, radius 19. 6 spire points (3 intact,
    //        3 broken stumps), crown ring, purpur cap, cyan glass windows.
    //        Slowest bob: 196-tick period.
    // ================================================================
    public static class DogBrokenCrown extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crownBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> intactSpires = new ArrayList<>();

        public DogBrokenCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_broken_crown", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location crownCenter = center.clone().add(19, 18, -4);

            // 6 spire points at 60-deg intervals, radius 4
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(i * 60.0);
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                boolean intact = (i % 2 == 0); // 3 intact, 3 broken
                float yScale = intact ? 3.5f : 0.5f;

                BlockDisplayHandle spire = displayBuilder.spawnBlock(
                        crownCenter.clone().add(x, 0, z), Material.AMETHYST_BLOCK);
                spire.scale(0.9f, yScale, 0.9f).glow(180, 0, 255).interpolation(3, 0);
                crownBlocks.add(spire);
                if (intact) intactSpires.add(spire);
                spawnedEntities.add(spire.entity());
            }

            // Crown ring: 6 white_concrete blocks
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(i * 60.0 + 30);
                double x = Math.cos(angle) * 3.5;
                double z = Math.sin(angle) * 3.5;
                BlockDisplayHandle ring = displayBuilder.spawnBlock(
                        crownCenter.clone().add(x, -1.5, z), Material.WHITE_CONCRETE);
                ring.scale(1.5f, 0.8f, 1.5f).glow(220, 220, 255).interpolation(3, 0);
                crownBlocks.add(ring);
                spawnedEntities.add(ring.entity());
            }

            // Crown cap: wide purpur_block
            BlockDisplayHandle cap = displayBuilder.spawnBlock(crownCenter.clone().add(0, 2.0, 0), Material.PURPUR_BLOCK);
            cap.scale(5.0f, 0.4f, 5.0f).glow(128, 0, 255).interpolation(3, 0);
            crownBlocks.add(cap);
            spawnedEntities.add(cap.entity());

            // 3 cyan stained glass interior windows
            for (int i = 0; i < 3; i++) {
                double angle = Math.toRadians(i * 120.0);
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                BlockDisplayHandle glass = displayBuilder.spawnBlock(
                        crownCenter.clone().add(x, 0, z), Material.CYAN_STAINED_GLASS);
                glass.scale(1.8f, 1.8f, 0.15f).rotate((float) Math.toRadians(i * 120), 0, 1, 0)
                     .glow(0, 200, 255).interpolation(3, 0);
                crownBlocks.add(glass);
                spawnedEntities.add(glass.entity());
            }

            DisplayBuilder.playSound(crownCenter, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Crown rotates 0.25 deg/tick
            float rot = (float) Math.toRadians(ticksAlive * 0.25);
            for (BlockDisplayHandle h : crownBlocks) {
                h.rotate(rot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Intact spire scale oscillation: 3.5 + 0.4 * sin(tick * 0.09)
            for (BlockDisplayHandle spire : intactSpires) {
                float osc = 3.5f + 0.4f * (float) Math.sin(ticksAlive * 0.09);
                spire.scale(0.9f, osc, 0.9f);
                spire.interpolation(3, 0);
            }

            // Slowest Y oscillation: 0.7 * sin(tick * 0.032), period 196 ticks
            float yOsc = 0.7f * (float) Math.sin(ticksAlive * 0.032);
            Location crownPos = center.clone().add(19, 18 + yOsc, -4);

            // Dense PORTAL interior + TOTEM
            w.spawnParticle(Particle.PORTAL, crownPos, 10, 3.0, 2.0, 3.0, 0);
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, crownPos, 4, 2.0, 2.0, 2.0, 0);

            if (ticksAlive % 90 == 0) {
                DisplayBuilder.playSound(crownPos, Sound.BLOCK_BEACON_AMBIENT, 0.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DogBrokenCrown(plugin); }
    }

    // ================================================================
    // #87 -- DWELLER'S ECHO: BRIMSTONE THRONE (Seat)
    //        Partial reconstruction. West of center, radius 16, Y+7.
    //        Blackstone seat + gilded trim + broken right arm + gold finials
    //        + magma veins. 149-tick bob. Faces arena center.
    // ================================================================
    public static class DwellerBrimstoneThrone extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> throneBlocks = new ArrayList<>();

        public DwellerBrimstoneThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_brimstone_throne", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location throneCenter = center.clone().add(-16, 7, 0);

            // Seat: blackstone
            BlockDisplayHandle seat = displayBuilder.spawnBlock(throneCenter, Material.BLACKSTONE);
            seat.scale(4.0f, 1.2f, 3.5f).glow(60, 0, 60).interpolation(3, 0);
            throneBlocks.add(seat);
            spawnedEntities.add(seat.entity());

            // Seat front edge: gilded_blackstone trim
            BlockDisplayHandle frontTrim = displayBuilder.spawnBlock(throneCenter.clone().add(0, 0, 1.75), Material.GILDED_BLACKSTONE);
            frontTrim.scale(4.0f, 0.5f, 0.4f).glow(200, 150, 0).interpolation(3, 0);
            throneBlocks.add(frontTrim);
            spawnedEntities.add(frontTrim.entity());

            // Left arm: full
            BlockDisplayHandle leftArm = displayBuilder.spawnBlock(throneCenter.clone().add(-2.0, 1.0, 0), Material.BLACKSTONE);
            leftArm.scale(0.8f, 2.0f, 3.5f).glow(60, 0, 60).interpolation(3, 0);
            throneBlocks.add(leftArm);
            spawnedEntities.add(leftArm.entity());

            // Right arm: broken (only front half, Z-scale 1.5)
            BlockDisplayHandle rightArm = displayBuilder.spawnBlock(throneCenter.clone().add(2.0, 1.0, 0.8), Material.BLACKSTONE);
            rightArm.scale(0.8f, 2.0f, 1.5f).glow(60, 0, 60).interpolation(3, 0);
            throneBlocks.add(rightArm);
            spawnedEntities.add(rightArm.entity());

            // Throne back
            BlockDisplayHandle back = displayBuilder.spawnBlock(throneCenter.clone().add(0, 3.0, -1.75), Material.BLACKSTONE);
            back.scale(4.0f, 5.0f, 1.0f).glow(60, 0, 60).interpolation(3, 0);
            throneBlocks.add(back);
            spawnedEntities.add(back.entity());

            // Back top trim
            BlockDisplayHandle backTrim = displayBuilder.spawnBlock(throneCenter.clone().add(0, 5.5, -1.75), Material.GILDED_BLACKSTONE);
            backTrim.scale(4.0f, 0.5f, 1.0f).glow(200, 150, 0).interpolation(3, 0);
            throneBlocks.add(backTrim);
            spawnedEntities.add(backTrim.entity());

            // Gold finials at top corners
            BlockDisplayHandle finialL = displayBuilder.spawnBlock(throneCenter.clone().add(-2.0, 6.0, -1.75), Material.GOLD_BLOCK);
            finialL.scale(1.0f, 1.5f, 1.0f).glow(255, 200, 0).interpolation(3, 0);
            throneBlocks.add(finialL);
            spawnedEntities.add(finialL.entity());

            BlockDisplayHandle finialR = displayBuilder.spawnBlock(throneCenter.clone().add(2.0, 6.0, -1.75), Material.GOLD_BLOCK);
            finialR.scale(1.0f, 1.5f, 1.0f).glow(255, 200, 0).interpolation(3, 0);
            throneBlocks.add(finialR);
            spawnedEntities.add(finialR.entity());

            // 3 magma inlay veins across seat
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle vein = displayBuilder.spawnBlock(
                        throneCenter.clone().add(-1.0 + i, 0.6, 0), Material.MAGMA_BLOCK);
                vein.scale(0.6f, 0.5f, 3.2f).glow(255, 100, 0).interpolation(3, 0);
                throneBlocks.add(vein);
                spawnedEntities.add(vein.entity());
            }

            DisplayBuilder.playSound(throneCenter, Sound.BLOCK_NETHER_BRICKS_PLACE, 0.1f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Y oscillation: 0.4 * sin(tick * 0.042), period 149 ticks
            float yOsc = 0.4f * (float) Math.sin(ticksAlive * 0.042);
            float prevYOsc = 0.4f * (float) Math.sin((ticksAlive - 1) * 0.042);
            float yDelta = yOsc - prevYOsc;

            for (BlockDisplayHandle h : throneBlocks) {
                BlockDisplay bd = h.entity();
                bd.teleport(bd.getLocation().clone().add(0, yDelta, 0));
            }

            Location thronePos = center.clone().add(-16, 7 + yOsc, 0);
            w.spawnParticle(Particle.PORTAL, thronePos, 8, 2.5, 2.5, 2.5, 0);
            w.spawnParticle(Particle.FLAME, thronePos, 4, 0.8, 0.8, 0.8, 0);
            w.spawnParticle(Particle.SMOKE, thronePos, 2, 1.5, 1.5, 1.5, 0);

            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(thronePos, Sound.BLOCK_NETHER_BRICKS_PLACE, 0.1f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DwellerBrimstoneThrone(plugin); }
    }

    // ================================================================
    // #88 -- DWELLER'S ECHO: ARMREST SHARD (Broken right arm)
    //        Floats 4 blocks from throne, Y+9. Opposite-phase bob.
    //        Tumbles slowly at 0.12 deg/tick on all axes.
    //        Drifts 0.002 b/tick away from throne.
    // ================================================================
    public static class DwellerArmrestShard extends BlockDisplayAttack {

        private BlockDisplayHandle armBlock;
        private BlockDisplayHandle goldShard;
        private double driftOffset = 0.0;

        public DwellerArmrestShard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_armrest_shard", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location shardPos = center.clone().add(-12, 9, 0); // 4 blocks from throne

            // Broken arm section
            armBlock = displayBuilder.spawnBlock(shardPos, Material.BLACKSTONE);
            armBlock.scale(0.8f, 2.0f, 1.8f).glow(60, 0, 60).interpolation(3, 0);
            spawnedEntities.add(armBlock.entity());

            // Gold trim piece floating nearby
            goldShard = displayBuilder.spawnBlock(shardPos.clone().add(0.3, 0.5, 0.3), Material.GOLD_BLOCK);
            goldShard.scale(0.5f, 0.5f, 0.5f).glow(255, 200, 0).interpolation(3, 0);
            spawnedEntities.add(goldShard.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Opposite-phase bob from throne: + PI phase offset
            float yOsc = 0.4f * (float) Math.sin(ticksAlive * 0.042 + Math.PI);

            // Slow tumble: 0.12 deg/tick on combined axes
            float tumble = (float) Math.toRadians(ticksAlive * 0.12);

            // Drift away from throne
            driftOffset += 0.002;
            Location shardPos = center.clone().add(-12 + driftOffset, 9 + yOsc, 0);

            if (armBlock != null) {
                armBlock.entity().teleport(shardPos);
                armBlock.rotate(tumble, 1, 1, 0);
                armBlock.interpolation(3, 0);
            }

            if (goldShard != null) {
                goldShard.entity().teleport(shardPos.clone().add(0.3, 0.5, 0.3));
                goldShard.rotate(tumble * 1.2f, 0, 1, 1);
                goldShard.interpolation(3, 0);
            }

            w.spawnParticle(Particle.PORTAL, shardPos, 3, 0.8, 0.8, 0.8, 0);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DwellerArmrestShard(plugin); }
    }

    // ================================================================
    // #89 -- DWELLER'S ECHO: SANCTUM PILLAR
    //        Nearly complete tall pillar. WSW, radius 24, Y+4 to Y+14.
    //        blackstone_bricks + gilded_blackstone bands + nether_bricks
    //        + gold_block finials. Smallest bob (0.3 amplitude).
    //        Rotates 0.05 deg/tick.
    // ================================================================
    public static class DwellerSanctumPillar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();

        public DwellerSanctumPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_sanctum_pillar", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location pillarBase = center.clone().add(-22, 4, 10); // WSW

            // Stacked pillar sections
            BlockDisplayHandle base = displayBuilder.spawnBlock(pillarBase, Material.BLACKSTONE);
            base.scale(3.0f, 1.5f, 3.0f).glow(60, 0, 60).interpolation(3, 0);
            pillarBlocks.add(base);
            spawnedEntities.add(base.entity());

            BlockDisplayHandle lowerShaft = displayBuilder.spawnBlock(pillarBase.clone().add(0, 2, 0), Material.NETHER_BRICKS);
            lowerShaft.scale(2.2f, 3.0f, 2.2f).glow(80, 20, 20).interpolation(3, 0);
            pillarBlocks.add(lowerShaft);
            spawnedEntities.add(lowerShaft.entity());

            BlockDisplayHandle lowerBand = displayBuilder.spawnBlock(pillarBase.clone().add(0, 4, 0), Material.GILDED_BLACKSTONE);
            lowerBand.scale(2.6f, 0.8f, 2.6f).glow(200, 150, 0).interpolation(3, 0);
            pillarBlocks.add(lowerBand);
            spawnedEntities.add(lowerBand.entity());

            BlockDisplayHandle midShaft = displayBuilder.spawnBlock(pillarBase.clone().add(0, 5, 0), Material.BLACKSTONE);
            midShaft.scale(2.0f, 3.0f, 2.0f).glow(60, 0, 60).interpolation(3, 0);
            pillarBlocks.add(midShaft);
            spawnedEntities.add(midShaft.entity());

            BlockDisplayHandle midBand = displayBuilder.spawnBlock(pillarBase.clone().add(0, 7, 0), Material.GILDED_BLACKSTONE);
            midBand.scale(2.4f, 0.8f, 2.4f).glow(200, 150, 0).interpolation(3, 0);
            pillarBlocks.add(midBand);
            spawnedEntities.add(midBand.entity());

            BlockDisplayHandle upperShaft = displayBuilder.spawnBlock(pillarBase.clone().add(0, 8.5, 0), Material.NETHER_BRICKS);
            upperShaft.scale(1.8f, 2.5f, 1.8f).glow(80, 20, 20).interpolation(3, 0);
            pillarBlocks.add(upperShaft);
            spawnedEntities.add(upperShaft.entity());

            // Capital: wider than shaft
            BlockDisplayHandle capital = displayBuilder.spawnBlock(pillarBase.clone().add(0, 10, 0), Material.BLACKSTONE);
            capital.scale(2.8f, 1.0f, 2.8f).glow(60, 0, 60).interpolation(3, 0);
            pillarBlocks.add(capital);
            spawnedEntities.add(capital.entity());

            // 4 gold corner finials on capital
            double[][] finialOffsets = {{1.0, 10.5, 1.0}, {-1.0, 10.5, 1.0}, {1.0, 10.5, -1.0}, {-1.0, 10.5, -1.0}};
            for (double[] off : finialOffsets) {
                BlockDisplayHandle finial = displayBuilder.spawnBlock(pillarBase.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                finial.scale(0.6f, 0.5f, 0.6f).glow(255, 200, 0).interpolation(3, 0);
                pillarBlocks.add(finial);
                spawnedEntities.add(finial.entity());
            }

            DisplayBuilder.playSound(pillarBase, Sound.BLOCK_NETHER_BRICKS_BREAK, 0.06f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Smallest bob: 0.3 * sin(tick * 0.044), period 143 ticks
            float yOsc = 0.3f * (float) Math.sin(ticksAlive * 0.044);
            float prevYOsc = 0.3f * (float) Math.sin((ticksAlive - 1) * 0.044);
            float yDelta = yOsc - prevYOsc;

            // Near-imperceptible rotation: 0.05 deg/tick
            float rot = (float) Math.toRadians(ticksAlive * 0.05);
            for (BlockDisplayHandle h : pillarBlocks) {
                BlockDisplay bd = h.entity();
                bd.teleport(bd.getLocation().clone().add(0, yDelta, 0));
                h.rotate(rot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            Location pillarCenter = center.clone().add(-22, 9 + yOsc, 10);
            w.spawnParticle(Particle.PORTAL, pillarCenter, 6, 1.5, 5.0, 1.5, 0);
            w.spawnParticle(Particle.FLAME, pillarCenter.clone().add(0, 5, 0), 2, 1.2, 1.2, 1.2, 0);

            if (ticksAlive % 240 == 0) {
                DisplayBuilder.playSound(pillarCenter, Sound.BLOCK_NETHER_BRICKS_BREAK, 0.06f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DwellerSanctumPillar(plugin); }
    }

    // ================================================================
    // #90 -- MEMORY CONVERGENCE AURA: Unifying particle overlay for
    //        all 8 echo structures. PORTAL 15/tick + SPELL_WITCH 4/tick
    //        + END_ROD 2/tick upward per echo position.
    //        On death: all echoes scale to 0 over 20 ticks + burst.
    // ================================================================
    public static class MemoryConvergenceAura extends BlockDisplayAttack {

        // Echo structure approximate positions (relative to center)
        private static final double[][] ECHO_POSITIONS = {
            {-14, 8, -14},   // #81 Fracture Gate A
            {11, 11, -14},    // #82 Fracture Gate B
            {0, 9.5, 18},     // #83 Spiral Rib
            {22, 9, 0},       // #84 Lattice Node A
            {17, 13, -8},     // #85 Lattice Node B
            {19, 18, -4},     // #86 Broken Crown
            {-16, 7, 0},      // #87 Brimstone Throne + #88 Armrest
            {-22, 9, 10},     // #89 Sanctum Pillar
        };

        public MemoryConvergenceAura(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("memory_convergence_aura", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Pure particle overlay -- no blocks to spawn
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Emit aura particles from each echo structure position
            for (double[] pos : ECHO_POSITIONS) {
                Location echoPos = center.clone().add(pos[0], pos[1], pos[2]);

                // PORTAL: primary ghost effect
                w.spawnParticle(Particle.PORTAL, echoPos, 15, 5.0, 5.0, 5.0, 0);
                // SPELL_WITCH: haunted magical quality
                w.spawnParticle(Particle.WITCH, echoPos, 4, 4.0, 4.0, 4.0, 0);
                // END_ROD: upward dissolve
                w.spawnParticle(Particle.END_ROD, echoPos, 2, 2.0, 2.0, 2.0, 0.06);
            }

            // Ambient soundscape every 200 ticks
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 10, 0),
                        Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 0.3f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MemoryConvergenceAura(plugin); }
    }
}
