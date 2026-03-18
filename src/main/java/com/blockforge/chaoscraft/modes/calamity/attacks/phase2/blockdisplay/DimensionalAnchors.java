package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.blockdisplay;

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
 * Phase 2 (4B) Block Display -- GROUP 9: DIMENSIONAL ANCHORS
 * 10 attacks featuring structures that pin dimensional space in place,
 * preventing DoG's reality from fully collapsing. Rift stabilizers,
 * planar locks, and void tethers made of crystalline plague material.
 *
 * Rules applied:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - DoG palette: cyan(0,200,255), violet(128,0,255), white(240,240,255)
 * - Damage in HP (4.0-12.0 range)
 */
public final class DimensionalAnchors {

    private DimensionalAnchors() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new RiftStake(plugin));
        registry.register(new PlanarLockPylon(plugin));
        registry.register(new VoidTether(plugin));
        registry.register(new DimensionalNail(plugin));
        registry.register(new RealityAnchorRing(plugin));
        registry.register(new PhaseClamp(plugin));
        registry.register(new CrystallinePinion(plugin));
        registry.register(new TemporalStaple(plugin));
        registry.register(new VoidSutureArray(plugin));
        registry.register(new DimensionalKeystone(plugin));
    }

    // ================================================================
    // 1. RIFT STAKE -- A massive spike driven into reality to pin a
    //    dimensional tear shut, radiating stabilization energy
    // ================================================================
    public static class RiftStake extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> stakeBlocks = new ArrayList<>();
        private BlockDisplayHandle headBlock;
        private final List<BlockDisplayHandle> energyRings = new ArrayList<>();

        public RiftStake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_stake", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Stake shaft: 5 polished blackstone blocks, tapering
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 8 + i * 1.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                float taper = 1.0f - (i * 0.15f);
                h.scale(taper, 1.5f, taper).glow(0, 200, 255).interpolation(3, 0);
                stakeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Head: crying obsidian cap
            headBlock = displayBuilder.spawnBlock(center.clone().add(0, 15.5, 0), Material.CRYING_OBSIDIAN);
            headBlock.scale(1.3f, 0.8f, 1.3f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(headBlock.entity());

            // Energy rings: 3 amethyst rings at different heights
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, 3 + i * 3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(1.8f, 0.15f, 1.8f).glow(0, 200, 255).interpolation(2, 0);
                energyRings.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Stake drives down over first 40 ticks
            float driveOffset;
            if (ticksAlive < 40) {
                float progress = ticksAlive / 40.0f;
                driveOffset = -(progress * 8.0f);
            } else {
                driveOffset = -8.0f;
                float tremble = 0.05f * (float) Math.sin(ticksAlive * 2.0);
                driveOffset += tremble;
            }

            for (int i = 0; i < stakeBlocks.size(); i++) {
                Location target = center.clone().add(0, 8 + i * 1.5 + driveOffset, 0);
                stakeBlocks.get(i).entity().teleport(target);
            }
            if (headBlock != null) {
                headBlock.entity().teleport(center.clone().add(0, 15.5 + driveOffset, 0));
            }

            // Energy rings rotate and pulse at different rates
            for (int i = 0; i < energyRings.size(); i++) {
                float ringRot = ticksAlive * (0.03f + i * 0.01f);
                float yBob = 0.2f * (float) Math.sin((ticksAlive + i * 20) * Math.PI / 25);
                Location target = center.clone().add(0, 3 + i * 3 + yBob + Math.max(driveOffset + 8, 0) * -0.3f, 0);
                energyRings.get(i).entity().teleport(target);
                energyRings.get(i).rotate(ringRot, 0, 1, 0);
                float pulse = 1.8f + 0.3f * (float) Math.sin((ticksAlive + i * 15) * Math.PI / 20);
                energyRings.get(i).scale(pulse, 0.15f, pulse);
                energyRings.get(i).interpolation(2, 0);
            }

            // Impact at drive completion
            if (ticksAlive == 39) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
                DisplayBuilder.cyanDust(center, 30, 3.0);
            }

            // Stabilization particles
            if (ticksAlive % 6 == 0 && ticksAlive > 40) {
                for (BlockDisplayHandle ring : energyRings) {
                    Location rLoc = ring.entity().getLocation();
                    DisplayBuilder.particleRing(rLoc, 1.8, Particle.ELECTRIC_SPARK, 6, null);
                }
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftStake(plugin); }
    }

    // ================================================================
    // 2. PLANAR LOCK PYLON -- Triangular pylon that generates a
    //    containment field between three anchor points
    // ================================================================
    public static class PlanarLockPylon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pylonA = new ArrayList<>();
        private final List<BlockDisplayHandle> pylonB = new ArrayList<>();
        private final List<BlockDisplayHandle> pylonC = new ArrayList<>();
        private final List<BlockDisplayHandle> fieldNodes = new ArrayList<>();

        public PlanarLockPylon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("planar_lock_pylon", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Three pylons in equilateral triangle, 5 blocks from center
            double[][] pylonPositions = {
                    {0, 0, -5}, {4.33, 0, 2.5}, {-4.33, 0, 2.5}
            };

            List<List<BlockDisplayHandle>> allPylons = List.of(pylonA, pylonB, pylonC);

            for (int p = 0; p < 3; p++) {
                double px = pylonPositions[p][0];
                double pz = pylonPositions[p][2];
                List<BlockDisplayHandle> pylon = allPylons.get(p);

                // 3-block pylon
                for (int i = 0; i < 3; i++) {
                    Location loc = center.clone().add(px, i, pz);
                    Material mat = (i == 2) ? Material.SEA_LANTERN : Material.DARK_PRISMARINE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    float taper = 0.8f - (i * 0.15f);
                    h.scale(taper, 1.0f, taper).glow(0, 200, 255).interpolation(3, 0);
                    pylon.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Field nodes: 3 amethyst at midpoints between pylons
            for (int i = 0; i < 3; i++) {
                int next = (i + 1) % 3;
                double mx = (pylonPositions[i][0] + pylonPositions[next][0]) / 2;
                double mz = (pylonPositions[i][2] + pylonPositions[next][2]) / 2;
                Location loc = center.clone().add(mx, 1.5, mz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                fieldNodes.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.9f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            double[][] pylonPositions = {
                    {0, 0, -5}, {4.33, 0, 2.5}, {-4.33, 0, 2.5}
            };

            // Field nodes orbit along the triangle edges
            for (int i = 0; i < fieldNodes.size(); i++) {
                int next = (i + 1) % 3;
                float t = ((ticksAlive * 0.01f + i * 0.33f) % 1.0f);
                double mx = pylonPositions[i][0] * (1 - t) + pylonPositions[next][0] * t;
                double mz = pylonPositions[i][2] * (1 - t) + pylonPositions[next][2] * t;
                float yBob = 1.5f + 0.3f * (float) Math.sin((ticksAlive + i * 20) * Math.PI / 25);
                Location target = center.clone().add(mx, yBob, mz);
                fieldNodes.get(i).entity().teleport(target);
                fieldNodes.get(i).interpolation(2, 0);
            }

            // Containment field lines between pylon tops
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < 3; i++) {
                    int next = (i + 1) % 3;
                    Location start = center.clone().add(pylonPositions[i][0], 2.5, pylonPositions[i][2]);
                    Location end = center.clone().add(pylonPositions[next][0], 2.5, pylonPositions[next][2]);
                    DisplayBuilder.particleLine(start, end, Particle.ELECTRIC_SPARK, 2, null);
                }
            }

            // Pylon top glow cycle
            List<List<BlockDisplayHandle>> allPylons = List.of(pylonA, pylonB, pylonC);
            int activeIndex = (ticksAlive / 30) % 3;
            for (int p = 0; p < 3; p++) {
                BlockDisplayHandle top = allPylons.get(p).get(2);
                if (p == activeIndex) {
                    top.glow(240, 240, 255);
                } else {
                    top.glow(0, 200, 255);
                }
            }

            // Center containment pulse
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 15, 3.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PlanarLockPylon(plugin); }
    }

    // ================================================================
    // 3. VOID TETHER -- Vertical chain of crystal linking ground to
    //    sky, holding a dimensional tear shut like stitches
    // ================================================================
    public static class VoidTether extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tetherLinks = new ArrayList<>();
        private BlockDisplayHandle groundAnchor;
        private BlockDisplayHandle skyAnchor;

        public VoidTether(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tether", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ground anchor
            groundAnchor = displayBuilder.spawnBlock(center, Material.POLISHED_BLACKSTONE);
            groundAnchor.scale(1.2f, 0.5f, 1.2f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(groundAnchor.entity());

            // Sky anchor
            skyAnchor = displayBuilder.spawnBlock(center.clone().add(0, 12, 0), Material.POLISHED_BLACKSTONE);
            skyAnchor.scale(1.2f, 0.5f, 1.2f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(skyAnchor.entity());

            // Tether links: 10 alternating amethyst and dark prismarine
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(0, 1 + i * 1.0, 0);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_BLOCK : Material.DARK_PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.4f, 0.6f, 0.4f).glow(128, 0, 255).interpolation(3, 0);
                tetherLinks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Tether links sway in a wave pattern
            for (int i = 0; i < tetherLinks.size(); i++) {
                float xSway = 0.3f * (float) Math.sin((ticksAlive - i * 5) * Math.PI / 20);
                float zSway = 0.2f * (float) Math.cos((ticksAlive - i * 7) * Math.PI / 25);
                Location target = center.clone().add(xSway, 1 + i * 1.0, zSway);
                tetherLinks.get(i).entity().teleport(target);
                tetherLinks.get(i).interpolation(3, 0);
            }

            // Energy pulse traveling up the tether
            int pulseLink = (ticksAlive / 3) % tetherLinks.size();
            for (int i = 0; i < tetherLinks.size(); i++) {
                if (i == pulseLink) {
                    tetherLinks.get(i).glow(240, 240, 255);
                    float s = 0.5f;
                    tetherLinks.get(i).scale(s, 0.6f, s);
                } else {
                    tetherLinks.get(i).glow(128, 0, 255);
                    tetherLinks.get(i).scale(0.4f, 0.6f, 0.4f);
                }
            }

            // Particles along the tether
            if (ticksAlive % 4 == 0) {
                Location pLoc = tetherLinks.get(pulseLink).entity().getLocation();
                DisplayBuilder.dustParticles(pLoc, 4, 0.2, 0, 200, 255, 1.0f);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTether(plugin); }
    }

    // ================================================================
    // 4. DIMENSIONAL NAIL -- Single enormous crystal driven through
    //    the arena at an angle, pinning two dimensions together
    // ================================================================
    public static class DimensionalNail extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> nailBlocks = new ArrayList<>();
        private BlockDisplayHandle headCap;

        public DimensionalNail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_nail", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(7.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(450);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Nail shaft: 8 blocks at 45-degree angle
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(i * 0.8 - 2, 10 + i * 0.8, i * 0.3);
                Material mat = (i < 2) ? Material.LARGE_AMETHYST_BUD : Material.AMETHYST_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float taper = (i < 2) ? (0.4f + i * 0.15f) : 0.7f;
                h.scale(taper, taper, taper).glow(128, 0, 255).interpolation(3, 0);
                nailBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Head cap: flat dark prismarine disc
            headCap = displayBuilder.spawnBlock(center.clone().add(4.4, 16.4, 2.4), Material.DARK_PRISMARINE);
            headCap.scale(1.5f, 0.4f, 1.5f).glow(0, 200, 255).interpolation(3, 0);
            spawnedEntities.add(headCap.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Drive the nail down over 50 ticks
            float driveOffset;
            if (ticksAlive < 50) {
                float progress = ticksAlive / 50.0f;
                driveOffset = -(progress * 10.0f);
            } else {
                driveOffset = -10.0f;
                float tremble = 0.04f * (float) Math.sin(ticksAlive * 2.5);
                driveOffset += tremble;
            }

            for (int i = 0; i < nailBlocks.size(); i++) {
                float driveFactor = driveOffset * 0.707f; // 45-degree angle component
                Location target = center.clone().add(
                        i * 0.8 - 2 + driveFactor * 0.5,
                        10 + i * 0.8 + driveFactor,
                        i * 0.3 + driveFactor * 0.2);
                nailBlocks.get(i).entity().teleport(target);
            }
            if (headCap != null) {
                float driveFactor = driveOffset * 0.707f;
                headCap.entity().teleport(center.clone().add(
                        4.4 + driveFactor * 0.5, 16.4 + driveFactor, 2.4 + driveFactor * 0.2));
            }

            // Impact at drive completion
            if (ticksAlive == 49) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.5f);
                DisplayBuilder.cyanDust(center, 30, 2.5);
                w.spawnParticle(Particle.ELECTRIC_SPARK, center, 15, 1.5, 0.5, 1.5, 0.08);
            }

            // Energy discharge along the nail
            if (ticksAlive % 5 == 0 && ticksAlive > 50) {
                int idx = (ticksAlive / 5) % nailBlocks.size();
                Location nLoc = nailBlocks.get(idx).entity().getLocation();
                DisplayBuilder.cyanDust(nLoc, 3, 0.3);
            }

            if (ticksAlive % 60 == 0 && ticksAlive > 50) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalNail(plugin); }
    }

    // ================================================================
    // 5. REALITY ANCHOR RING -- Horizontal ring of alternating
    //    crystal and prismarine blocks that locks dimensional space
    // ================================================================
    public static class RealityAnchorRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private BlockDisplayHandle centerNode;

        public RealityAnchorRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_anchor_ring", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ring: 16 blocks in a circle at Y+3
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 4, 3, Math.sin(angle) * 4);
                Material mat = (i % 2 == 0) ? Material.AMETHYST_BLOCK : Material.DARK_PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f, 0.5f, 0.7f).glow(0, 200, 255).interpolation(3, 0);
                ringBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center stabilization node
            centerNode = displayBuilder.spawnBlock(center.clone().add(0, 3, 0), Material.SEA_LANTERN);
            centerNode.scale(1.0f, 1.0f, 1.0f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(centerNode.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ring rotation
            float rotOffset = ticksAlive * 0.012f;
            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 16 + rotOffset;
                float yBob = 0.15f * (float) Math.sin((ticksAlive + i * 5) * Math.PI / 20);
                Location target = center.clone().add(Math.cos(angle) * 4, 3 + yBob, Math.sin(angle) * 4);
                ringBlocks.get(i).entity().teleport(target);
                ringBlocks.get(i).interpolation(2, 0);
            }

            // Center node pulses
            if (centerNode != null) {
                float s = 1.0f + 0.2f * (float) Math.sin(ticksAlive * Math.PI / 20);
                centerNode.scale(s, s, s);
                centerNode.rotate(ticksAlive * 0.03f, 0, 1, 0);
                centerNode.interpolation(2, 0);
            }

            // Anchor beams from center to ring
            if (ticksAlive % 6 == 0) {
                int beamIdx = (ticksAlive / 6) % 4;
                int ringIdx = beamIdx * 4;
                Location start = center.clone().add(0, 3, 0);
                Location end = ringBlocks.get(ringIdx).entity().getLocation();
                DisplayBuilder.particleLine(start, end, Particle.ELECTRIC_SPARK, 2, null);
            }

            // Containment field particles
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 3, 0), 4, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(0, 200, 255), 0.8f));
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityAnchorRing(plugin); }
    }

    // ================================================================
    // 6. PHASE CLAMP -- Two interlocking crystal jaws that clamp
    //    around a rift tear, slowly squeezing it shut
    // ================================================================
    public static class PhaseClamp extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftJaw = new ArrayList<>();
        private final List<BlockDisplayHandle> rightJaw = new ArrayList<>();
        private BlockDisplayHandle riftCore;

        public PhaseClamp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase_clamp", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(7.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left jaw: 4 dark prismarine blocks
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(-3, i + 1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.8f, 0.8f, 0.6f).glow(0, 200, 255).interpolation(3, 0);
                leftJaw.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right jaw: 4 dark prismarine blocks
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(3, i + 1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.8f, 0.8f, 0.6f).glow(0, 200, 255).interpolation(3, 0);
                rightJaw.add(h);
                spawnedEntities.add(h.entity());
            }

            // Rift core: cyan stained glass
            riftCore = displayBuilder.spawnBlock(center.clone().add(0, 2.5, 0), Material.CYAN_STAINED_GLASS);
            riftCore.scale(1.5f, 3.0f, 0.3f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(riftCore.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Jaws slowly close over 100 ticks
            float closeProgress = Math.min(1.0f, ticksAlive / 100.0f);
            float jawOffset = 3.0f - closeProgress * 2.5f;

            for (int i = 0; i < leftJaw.size(); i++) {
                Location target = center.clone().add(-jawOffset, i + 1, 0);
                leftJaw.get(i).entity().teleport(target);
                leftJaw.get(i).interpolation(3, 0);
            }
            for (int i = 0; i < rightJaw.size(); i++) {
                Location target = center.clone().add(jawOffset, i + 1, 0);
                rightJaw.get(i).entity().teleport(target);
                rightJaw.get(i).interpolation(3, 0);
            }

            // Rift core shrinks as jaws close
            if (riftCore != null) {
                float riftWidth = 1.5f * (1.0f - closeProgress * 0.8f);
                float pulse = 0.1f * (float) Math.sin(ticksAlive * Math.PI / 10);
                riftCore.scale(riftWidth + pulse, 3.0f, 0.3f);
                riftCore.interpolation(2, 0);
            }

            // Sparks between jaw faces
            if (ticksAlive % 6 == 0) {
                Location spark = center.clone().add(0, 2.5, 0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, spark, 4, jawOffset * 0.3, 1.0, 0.1, 0.03);
            }

            // Portal particles from rift
            if (ticksAlive % 4 == 0) {
                w.spawnParticle(Particle.PORTAL, center.clone().add(0, 2.5, 0), 3, 0.3, 1.0, 0.1, 0.1);
            }

            // Clamp sound when fully closed
            if (ticksAlive == 99) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.7f);
                DisplayBuilder.cyanDust(center.clone().add(0, 2.5, 0), 20, 1.5);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseClamp(plugin); }
    }

    // ================================================================
    // 7. CRYSTALLINE PINION -- Wing-like crystal formation that
    //    spreads open and generates a dimensional barrier
    // ================================================================
    public static class CrystallinePinion extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftWing = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWing = new ArrayList<>();
        private BlockDisplayHandle spineBlock;

        public CrystallinePinion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_pinion", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(450);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central spine
            spineBlock = displayBuilder.spawnBlock(center.clone().add(0, 2, 0), Material.POLISHED_BLACKSTONE);
            spineBlock.scale(0.4f, 3.0f, 0.4f).glow(0, 200, 255).interpolation(3, 0);
            spawnedEntities.add(spineBlock.entity());

            // Left wing: 5 amethyst blocks fanning out
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(-(i + 1) * 0.8, 2 + i * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                float s = 0.7f - (i * 0.08f);
                h.scale(s, 0.3f, s).glow(128, 0, 255).interpolation(3, 0);
                leftWing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right wing
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add((i + 1) * 0.8, 2 + i * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                float s = 0.7f - (i * 0.08f);
                h.scale(s, 0.3f, s).glow(128, 0, 255).interpolation(3, 0);
                rightWing.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Wings spread open over 40 ticks, then flap
            float spreadAngle;
            if (ticksAlive < 40) {
                spreadAngle = (ticksAlive / 40.0f) * 0.5f;
            } else {
                spreadAngle = 0.5f + 0.15f * (float) Math.sin((ticksAlive - 40) * Math.PI / 30);
            }

            for (int i = 0; i < leftWing.size(); i++) {
                float xOff = -(i + 1) * 0.8f * (1.0f + spreadAngle);
                float yOff = 2 + i * 0.3f + spreadAngle * i * 0.2f;
                Location target = center.clone().add(xOff, yOff, 0);
                leftWing.get(i).entity().teleport(target);
                leftWing.get(i).interpolation(3, 0);
            }
            for (int i = 0; i < rightWing.size(); i++) {
                float xOff = (i + 1) * 0.8f * (1.0f + spreadAngle);
                float yOff = 2 + i * 0.3f + spreadAngle * i * 0.2f;
                Location target = center.clone().add(xOff, yOff, 0);
                rightWing.get(i).entity().teleport(target);
                rightWing.get(i).interpolation(3, 0);
            }

            // Barrier particles between wing tips
            if (ticksAlive % 6 == 0 && ticksAlive > 40) {
                Location leftTip = leftWing.get(4).entity().getLocation();
                Location rightTip = rightWing.get(4).entity().getLocation();
                DisplayBuilder.particleLine(leftTip, rightTip, Particle.DUST, 3,
                        new Particle.DustOptions(Color.fromRGB(0, 200, 255), 0.8f));
            }

            // Ambient end rod particles from wing edges
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle h : leftWing) {
                    w.spawnParticle(Particle.END_ROD, h.entity().getLocation(), 1, 0.1, 0.1, 0.1, 0.005);
                }
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallinePinion(plugin); }
    }

    // ================================================================
    // 8. TEMPORAL STAPLE -- U-shaped crystal bracket driven over a
    //    dimensional fracture to hold it in place
    // ================================================================
    public static class TemporalStaple extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftLeg = new ArrayList<>();
        private final List<BlockDisplayHandle> rightLeg = new ArrayList<>();
        private final List<BlockDisplayHandle> crossbar = new ArrayList<>();

        public TemporalStaple(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("temporal_staple", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left leg: 3 blocks going down
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-2, 6 - i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.6f, 1.0f, 0.6f).glow(0, 200, 255).interpolation(3, 0);
                leftLeg.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right leg
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(2, 6 - i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.6f, 1.0f, 0.6f).glow(0, 200, 255).interpolation(3, 0);
                rightLeg.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crossbar: 5 amethyst blocks connecting the legs at top
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(i - 2, 7, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.8f, 0.5f, 0.6f).glow(128, 0, 255).interpolation(3, 0);
                crossbar.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Staple drives down over 35 ticks
            float driveOffset;
            if (ticksAlive < 35) {
                float progress = ticksAlive / 35.0f;
                driveOffset = -(progress * 6.0f);
            } else {
                driveOffset = -6.0f;
                float tremble = 0.04f * (float) Math.sin(ticksAlive * 2.0);
                driveOffset += tremble;
            }

            for (int i = 0; i < leftLeg.size(); i++) {
                Location target = center.clone().add(-2, 6 - i + driveOffset, 0);
                leftLeg.get(i).entity().teleport(target);
            }
            for (int i = 0; i < rightLeg.size(); i++) {
                Location target = center.clone().add(2, 6 - i + driveOffset, 0);
                rightLeg.get(i).entity().teleport(target);
            }
            for (int i = 0; i < crossbar.size(); i++) {
                Location target = center.clone().add(i - 2, 7 + driveOffset, 0);
                crossbar.get(i).entity().teleport(target);
            }

            // Drive impact
            if (ticksAlive == 34) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.6f);
                DisplayBuilder.cyanDust(center, 25, 2.0);
            }

            // Energy between legs (containment field)
            if (ticksAlive % 8 == 0 && ticksAlive > 35) {
                Location lBase = center.clone().add(-2, 0 + driveOffset + 4, 0);
                Location rBase = center.clone().add(2, 0 + driveOffset + 4, 0);
                DisplayBuilder.particleLine(lBase, rBase, Particle.ELECTRIC_SPARK, 2, null);
            }

            // Ambient from crossbar
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : crossbar) {
                    Location cLoc = h.entity().getLocation().add(0, 0.3, 0);
                    DisplayBuilder.dustParticles(cLoc, 2, 0.2, 128, 0, 255, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TemporalStaple(plugin); }
    }

    // ================================================================
    // 9. VOID SUTURE ARRAY -- Series of X-shaped crystal stitches
    //    sealing a long dimensional wound across the arena
    // ================================================================
    public static class VoidSutureArray extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> stitchBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> woundBlocks = new ArrayList<>();

        public VoidSutureArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_suture_array", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Wound line: 8 cyan stained glass blocks
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(i - 4, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.8f, 0.1f, 0.3f).glow(240, 240, 255).interpolation(2, 0);
                woundBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // X-stitches: 6 pairs of crossing amethyst blocks
            for (int i = 0; i < 6; i++) {
                double xPos = (i - 2.5) * 1.3;
                // Forward slash
                Location locA = center.clone().add(xPos - 0.3, 0.3, -0.5);
                BlockDisplayHandle hA = displayBuilder.spawnBlock(locA, Material.AMETHYST_BLOCK);
                hA.scale(0.2f, 0.2f, 1.2f).glow(128, 0, 255)
                        .rotate(0.8f, 0, 1, 0).interpolation(3, 0);
                stitchBlocks.add(hA);
                spawnedEntities.add(hA.entity());

                // Back slash
                Location locB = center.clone().add(xPos + 0.3, 0.3, -0.5);
                BlockDisplayHandle hB = displayBuilder.spawnBlock(locB, Material.AMETHYST_BLOCK);
                hB.scale(0.2f, 0.2f, 1.2f).glow(128, 0, 255)
                        .rotate(-0.8f, 0, 1, 0).interpolation(3, 0);
                stitchBlocks.add(hB);
                spawnedEntities.add(hB.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Stitches tighten sequentially (every 8 ticks, one stitch pair cinches)
            for (int i = 0; i < 6; i++) {
                int cinchStart = i * 8;
                float tightness;
                if (ticksAlive < cinchStart) {
                    tightness = 0;
                } else if (ticksAlive < cinchStart + 10) {
                    tightness = (ticksAlive - cinchStart) / 10.0f;
                } else {
                    tightness = 1.0f;
                }

                float yPos = 0.3f + (1.0f - tightness) * 0.5f;
                BlockDisplayHandle a = stitchBlocks.get(i * 2);
                BlockDisplayHandle b = stitchBlocks.get(i * 2 + 1);
                double xPos = (i - 2.5) * 1.3;

                Location targetA = center.clone().add(xPos - 0.3 + tightness * 0.15, yPos, -0.5 + tightness * 0.25);
                Location targetB = center.clone().add(xPos + 0.3 - tightness * 0.15, yPos, -0.5 + tightness * 0.25);
                a.entity().teleport(targetA);
                b.entity().teleport(targetB);

                // Cinch sound
                if (ticksAlive == cinchStart) {
                    DisplayBuilder.playSound(center.clone().add(xPos, 0, 0),
                            Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.0f + i * 0.1f);
                }
            }

            // Wound glow pulse
            int glowIdx = (ticksAlive / 4) % woundBlocks.size();
            for (int i = 0; i < woundBlocks.size(); i++) {
                if (i == glowIdx) {
                    woundBlocks.get(i).glow(240, 240, 255);
                } else {
                    woundBlocks.get(i).glow(0, 200, 255);
                }
            }

            // Dimensional leak from wound
            if (ticksAlive % 5 == 0) {
                int idx = (ticksAlive / 5) % 8;
                Location wLoc = center.clone().add(idx - 4, 0.2, 0);
                w.spawnParticle(Particle.PORTAL, wLoc, 2, 0.2, 0.1, 0.1, 0.05);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSutureArray(plugin); }
    }

    // ================================================================
    // 10. DIMENSIONAL KEYSTONE -- Central locking mechanism that
    //     rotates its crystal faces to align a dimensional seal
    // ================================================================
    public static class DimensionalKeystone extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerFaces = new ArrayList<>();
        private final List<BlockDisplayHandle> innerCore = new ArrayList<>();
        private BlockDisplayHandle keystoneLantern;

        public DimensionalKeystone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_keystone", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer rotating faces: 8 blocks in a cube-like arrangement
            double[][] faceOffsets = {
                    {1.5, 0, 0}, {-1.5, 0, 0}, {0, 0, 1.5}, {0, 0, -1.5},
                    {1, 1, 1}, {-1, 1, -1}, {1, -1, -1}, {-1, -1, 1}
            };
            for (double[] off : faceOffsets) {
                Location loc = center.clone().add(off[0], 3 + off[1], off[2]);
                Material mat = (Math.abs(off[1]) > 0) ? Material.AMETHYST_BLOCK : Material.DARK_PRISMARINE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
                outerFaces.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner core: 4 amethyst blocks
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 3, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                innerCore.add(h);
                spawnedEntities.add(h.entity());
            }

            // Keystone lantern at center
            keystoneLantern = displayBuilder.spawnBlock(center.clone().add(0, 3, 0), Material.SEA_LANTERN);
            keystoneLantern.scale(0.4f, 0.4f, 0.4f).glow(240, 240, 255).interpolation(2, 0);
            spawnedEntities.add(keystoneLantern.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.7f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Outer faces rotate around center on two axes
            float rotY = ticksAlive * 0.02f;
            float rotX = ticksAlive * 0.015f;

            double[][] faceOffsets = {
                    {1.5, 0, 0}, {-1.5, 0, 0}, {0, 0, 1.5}, {0, 0, -1.5},
                    {1, 1, 1}, {-1, 1, -1}, {1, -1, -1}, {-1, -1, 1}
            };

            for (int i = 0; i < outerFaces.size(); i++) {
                double ox = faceOffsets[i][0];
                double oy = faceOffsets[i][1];
                double oz = faceOffsets[i][2];

                // Rotate around Y axis
                double rx = ox * Math.cos(rotY) - oz * Math.sin(rotY);
                double rz = ox * Math.sin(rotY) + oz * Math.cos(rotY);
                // Rotate around X axis
                double ry = oy * Math.cos(rotX) - rz * Math.sin(rotX);
                double rz2 = oy * Math.sin(rotX) + rz * Math.cos(rotX);

                Location target = center.clone().add(rx, 3 + ry, rz2);
                outerFaces.get(i).entity().teleport(target);
                outerFaces.get(i).interpolation(2, 0);
            }

            // Inner core counter-rotates
            float innerRot = -ticksAlive * 0.04f;
            for (int i = 0; i < innerCore.size(); i++) {
                double angle = (Math.PI * 2 * i) / 4 + innerRot;
                Location target = center.clone().add(Math.cos(angle) * 0.5, 3, Math.sin(angle) * 0.5);
                innerCore.get(i).entity().teleport(target);
            }

            // Keystone glow pulse
            if (keystoneLantern != null) {
                float s = 0.4f + 0.15f * (float) Math.sin(ticksAlive * Math.PI / 15);
                keystoneLantern.scale(s, s, s);
                keystoneLantern.interpolation(2, 0);
            }

            // Alignment burst every 80 ticks when faces pass alignment
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.6f);
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 20, 2.0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 3, 0), 12, 1.5, 1.5, 1.5, 0.05);
            }

            // Connecting beams from core to outer faces
            if (ticksAlive % 10 == 0) {
                int beamIdx = (ticksAlive / 10) % outerFaces.size();
                Location coreLoc = center.clone().add(0, 3, 0);
                Location faceLoc = outerFaces.get(beamIdx).entity().getLocation();
                DisplayBuilder.particleLine(coreLoc, faceLoc, Particle.ELECTRIC_SPARK, 2, null);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalKeystone(plugin); }
    }
}
