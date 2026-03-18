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
 * Phase 2 (DoG) Block Display -- GROUP 3: DIMENSIONAL RIFTS AND GATES
 * 10 structures: portals, gates, and dimensional tears.
 * Devourer of Gods palette: amethyst, cyan, dark prismarine, polished blackstone.
 *
 * Design rules:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - DoG glow colors: cyan(0,200,255), violet(128,0,255), white(240,240,255)
 */
public final class DimensionalRifts {

    private DimensionalRifts() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PrimaryRift(plugin));
        registry.register(new FractureGate(plugin));
        registry.register(new MirrorRift(plugin));
        registry.register(new UnstableVortex(plugin));
        registry.register(new SerpentDoor(plugin));
        registry.register(new DimensionalTear(plugin));
        registry.register(new CollapsingPortal(plugin));
        registry.register(new RiftBeacon(plugin));
        registry.register(new VoidWindow(plugin));
        registry.register(new PhaseRipple(plugin));
    }

    // ================================================================
    // 21. PRIMARY RIFT -- Main vertical oval portal frame
    // ================================================================
    public static class PrimaryRift extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerFrame = new ArrayList<>();
        private final List<BlockDisplayHandle> innerFrame = new ArrayList<>();
        private final List<BlockDisplayHandle> portalGlass = new ArrayList<>();

        public PrimaryRift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("primary_rift", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer frame: 14 crying obsidian blocks in vertical oval (5 tall x 3 wide)
            double[][] ovalPositions = {
                    {-1.5, 0}, {-1.5, 1}, {-1, 2}, {-0.5, 3}, {0, 4}, {0.5, 4}, {1, 3},
                    {1.5, 2}, {1.5, 1}, {1.5, 0}, {1, -0.5}, {0.5, -0.5}, {0, -0.5}, {-1, -0.5}
            };
            for (double[] pos : ovalPositions) {
                Location loc = center.clone().add(pos[0], pos[1], 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.7f, 0.7f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                outerFrame.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner frame: 10 dark prismarine blocks
            double[][] innerPositions = {
                    {-1, 0.5}, {-1, 1.5}, {-0.5, 2.5}, {0, 3.5}, {0.5, 3.5},
                    {1, 2.5}, {1, 1.5}, {1, 0.5}, {0.5, 0}, {-0.5, 0}
            };
            for (double[] pos : innerPositions) {
                Location loc = center.clone().add(pos[0], pos[1], 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.5f, 0.5f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                innerFrame.add(h);
                spawnedEntities.add(h.entity());
            }

            // Portal interior: cyan + light blue stained glass
            for (int y = 0; y <= 3; y++) {
                for (int x = -1; x <= 0; x++) {
                    Material mat = ((x + y) % 2 == 0) ? Material.CYAN_STAINED_GLASS : Material.LIGHT_BLUE_STAINED_GLASS;
                    Location loc = center.clone().add(x * 0.6 + 0.3, y + 0.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.6f, 0.7f, 0.15f).glow(0, 200, 255).interpolation(2, 0);
                    portalGlass.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Top arch cluster
            BlockDisplayHandle arch = displayBuilder.spawnBlock(center.clone().add(0, 4.3, 0), Material.AMETHYST_CLUSTER);
            arch.scale(0.5f, 0.7f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(arch.entity());

            // Base blocks
            for (int x = -1; x <= 0; x++) {
                BlockDisplayHandle base = displayBuilder.spawnBlock(center.clone().add(x, -0.5, 0), Material.POLISHED_BLACKSTONE);
                base.scale(0.8f, 0.5f, 0.6f).glow(80, 80, 100).interpolation(2, 0);
                spawnedEntities.add(base.entity());
            }

            // Sea lanterns inside at Y+1 and Y+3
            for (int y : new int[]{1, 3}) {
                BlockDisplayHandle sl = displayBuilder.spawnBlock(center.clone().add(0, y, -0.2), Material.SEA_LANTERN);
                sl.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(sl.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 1.0f);
            DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 25, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Portal glass cycles opacity: fully opaque to 50% on 120-tick transition
            float opacityCycle = (float) Math.sin(ticksAlive * (2 * Math.PI / 120));
            float glassScale = 0.6f + opacityCycle * 0.05f;
            for (BlockDisplayHandle h : portalGlass) {
                h.scale(glassScale, 0.7f, 0.15f);
                h.interpolation(3, 0);
            }

            // Dragon breath billowing from portal interior
            if (ticksAlive % 3 == 0) {
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        center.clone().add(0, 2, 0.5), 5, 0.6, 1.0, 0.3, 0.02);
            }

            // Portal particles spiraling inside frame
            if (ticksAlive % 2 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 2, 0), 4, 0.5, 1.5, 0.3, 0.1);
            }

            // Horizontal crossbeam laser between sentinel positions
            if (ticksAlive % 5 == 0) {
                Location left = center.clone().add(-1.5, 2, 0.5);
                Location right = center.clone().add(1.5, 2, 0.5);
                for (int p = 0; p < 6; p++) {
                    double t = p / 5.0;
                    Location particle = left.clone().add(
                            (right.getX() - left.getX()) * t,
                            0, 0
                    );
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0.1, 0, 0);
                }
            }

            // Sound loop every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrimaryRift(plugin); }
    }

    // ================================================================
    // 22. FRACTURE GATE -- Asymmetric broken-gate structure
    // ================================================================
    public static class FractureGate extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftPillar = new ArrayList<>();
        private final List<BlockDisplayHandle> rightRubble = new ArrayList<>();
        private final List<BlockDisplayHandle> membrane = new ArrayList<>();
        private final List<BlockDisplayHandle> archClusters = new ArrayList<>();

        public FractureGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fracture_gate", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left pillar: 4 blocks tall, alternating polished blackstone + amethyst
            Material[] pillarMats = {Material.POLISHED_BLACKSTONE, Material.AMETHYST_BLOCK,
                    Material.POLISHED_BLACKSTONE, Material.AMETHYST_BLOCK};
            for (int y = 0; y < 4; y++) {
                Location loc = center.clone().add(-2, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, pillarMats[y]);
                h.scale(0.8f, 0.9f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                leftPillar.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right pillar: only 2 blocks tall (broken)
            for (int y = 0; y < 2; y++) {
                Location loc = center.clone().add(2, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.9f, 0.8f).glow(80, 80, 100).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Rubble from collapsed right pillar
            double[][] rubblePos = {{2.5, 0.3, 0.5}, {1.8, 0.2, -0.4}, {2.3, 0.1, 0.8}, {2.7, 0.2, -0.3}};
            for (double[] pos : rubblePos) {
                Location loc = center.clone().add(pos[0], pos[1], pos[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                rightRubble.add(h);
                spawnedEntities.add(h.entity());
            }

            // Arch on left pillar: 3 large amethyst clusters
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-2 + i * 0.8, 4 + (i == 1 ? 0.3 : 0), 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.5f, 0.6f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                archClusters.add(h);
                spawnedEntities.add(h.entity());
            }

            // Gate membrane: 9 dark blocks filling the 3x3 opening
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add(x * 0.8, y + 0.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(0.6f, 0.8f, 0.2f).glow(40, 40, 60).interpolation(2, 0);
                    membrane.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 8 end rods pointing inward from frame edges
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, 1.5, Math.sin(angle) * 0.3);
                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc, Material.END_ROD);
                rod.scale(0.15f, 0.5f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rubble orbits collapsed pillar stump at 2 deg/tick
            float rubbleRot = ticksAlive * 0.035f;
            for (int i = 0; i < rightRubble.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / rightRubble.size() + rubbleRot;
                double dist = 0.5 + (i % 2) * 0.3;
                Location loc = center.clone().add(
                        2 + Math.cos(baseAngle) * dist,
                        0.2 + (i % 3) * 0.15,
                        Math.sin(baseAngle) * dist
                );
                rightRubble.get(i).entity().teleport(loc);
            }

            // Membrane ripple: blocks oscillate in/out 0.2 blocks on staggered cycles
            for (int i = 0; i < membrane.size(); i++) {
                int cycle = 40 + i * 5;
                float zOff = 0.2f * (float) Math.sin(ticksAlive * (2 * Math.PI / cycle));
                BlockDisplay bd = membrane.get(i).entity();
                Location base = bd.getLocation();
                bd.teleport(center.clone().add(
                        ((i / 3) - 1) * 0.8,
                        (i % 3) + 0.5,
                        zOff
                ));
            }

            // Arch clusters scale pulse: 0.9 to 1.1, 70-tick cycles
            for (int i = 0; i < archClusters.size(); i++) {
                float scale = 0.5f + 0.05f * (float) Math.sin(ticksAlive * (2 * Math.PI / 70));
                archClusters.get(i).scale(scale, 0.6f, scale);
                archClusters.get(i).interpolation(2, 0);
            }

            // Portal particles leaking from membrane
            if (ticksAlive % 3 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 1.5, 0.3), 3, 0.5, 1.0, 0.1, 0.05);
            }

            // Electric spark arc between left pillar top and right stump
            if (ticksAlive % 10 == 0) {
                Location from = center.clone().add(-2, 4, 0);
                Location to = center.clone().add(2, 2, 0);
                for (int p = 0; p < 5; p++) {
                    double t = p / 4.0;
                    Location particle = from.clone().add(
                            (to.getX() - from.getX()) * t,
                            (to.getY() - from.getY()) * t + Math.sin(t * Math.PI) * 0.5,
                            0
                    );
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0.1, 0.1, 0.1, 0);
                }
            }

            // Dragon breath from membrane center
            if (ticksAlive % 6 == 0) {
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        center.clone().add(0, 1.5, 0), 3, 0.4, 0.8, 0.2, 0.01);
            }

            // Sound every 160 ticks
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FractureGate(plugin); }
    }

    // ================================================================
    // 23. MIRROR RIFT -- Two paired portal frames facing each other
    // ================================================================
    public static class MirrorRift extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frame1 = new ArrayList<>();
        private final List<BlockDisplayHandle> frame2 = new ArrayList<>();
        private final List<BlockDisplayHandle> cageCeiling = new ArrayList<>();

        public MirrorRift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mirror_rift", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Frame 1 at Z-4: 3 tall x 2 wide crying obsidian border
            buildFrame(center.clone().add(0, 0, -4), frame1);

            // Frame 2 at Z+4
            buildFrame(center.clone().add(0, 0, 4), frame2);

            // Tunnel floor: cyan glazed terracotta strip
            for (int z = -3; z <= 3; z++) {
                for (int x = -1; x <= 0; x++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x + 0.5, -0.3, z), Material.PRISMARINE);
                    h.scale(0.9f, 0.2f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            // Dark prismarine walkway connecting bases
            for (int z = -4; z <= 4; z++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, -0.5, z), Material.DARK_PRISMARINE);
                h.scale(1.5f, 0.3f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // 6 amethyst blocks floating above tunnel
            for (int i = 0; i < 6; i++) {
                double z = -3 + i * 1.2;
                float y = 3.5f + (i % 2) * 0.5f;
                Location loc = center.clone().add(0, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                cageCeiling.add(h);
                spawnedEntities.add(h.entity());
            }

            // End rod connections in ceiling
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 3.3, -3 + i * 1.2 + 0.6);
                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc, Material.END_ROD);
                rod.scale(0.15f, 0.15f, 1.0f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.8f);
        }

        private void buildFrame(Location frameLoc, List<BlockDisplayHandle> frameList) {
            // Crying obsidian border for 3x2 frame
            int[][] border = {{-1, 0}, {-1, 1}, {-1, 2}, {-1, 3}, {1, 0}, {1, 1}, {1, 2}, {1, 3}, {0, 0}, {0, 3}};
            for (int[] pos : border) {
                Location loc = frameLoc.clone().add(pos[0] * 0.5, pos[1] * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.7f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                frameList.add(h);
                spawnedEntities.add(h.entity());
            }

            // Interior: white stained glass (using light blue for DoG palette)
            for (int y = 1; y <= 2; y++) {
                Location loc = frameLoc.clone().add(0, y * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                h.scale(0.8f, 0.7f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                frameList.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Glass in both frames pulses in synchrony: 80-tick cycle
            float pulseScale = 0.8f + 0.1f * (float) Math.sin(ticksAlive * (2 * Math.PI / 80));
            for (BlockDisplayHandle h : frame1) {
                if (frame1.indexOf(h) >= 10) { // Glass blocks
                    h.scale(pulseScale, 0.7f, 0.15f);
                    h.interpolation(3, 0);
                }
            }
            for (BlockDisplayHandle h : frame2) {
                if (frame2.indexOf(h) >= 10) {
                    h.scale(pulseScale, 0.7f, 0.15f);
                    h.interpolation(3, 0);
                }
            }

            // Cage ceiling rotates at 0.3 deg/tick
            float cageRot = ticksAlive * 0.00524f;
            for (int i = 0; i < cageCeiling.size(); i++) {
                double z = -3 + i * 1.2;
                double rotX = Math.cos(cageRot) * 0 - Math.sin(cageRot) * z;
                double rotZ = Math.sin(cageRot) * 0 + Math.cos(cageRot) * z;
                float y = 3.5f + (i % 2) * 0.5f;
                // Simplified: just apply subtle rotation transform
                BlockDisplay bd = cageCeiling.get(i).entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.25f, -0.25f, -0.25f),
                        new AxisAngle4f(cageRot, 0, 1, 0),
                        new Vector3f(0.5f, 0.5f, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Persistent laser beam through tunnel
            if (ticksAlive % 3 == 0) {
                for (int z = -4; z <= 4; z++) {
                    Location beamPt = center.clone().add(0, 1.5, z);
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, beamPt, 1, 0.05, 0.05, 0, 0);
                }
            }

            // Portal particles traveling from frame1 to frame2
            if (ticksAlive % 4 == 0) {
                int step = (ticksAlive / 4) % 8;
                Location particleLoc = center.clone().add(0, 1.5, -4 + step);
                center.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 2, 0.1, 0.1, 0.1, 0.05);
            }

            // End rod particles from ceiling
            if (ticksAlive % 8 == 0) {
                center.getWorld().spawnParticle(Particle.END_ROD,
                        center.clone().add(0, 3.5, 0), 2, 0.5, 0.1, 2.0, 0.01);
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MirrorRift(plugin); }
    }

    // ================================================================
    // 24. UNSTABLE VORTEX -- Horizontal ground vortex with pull
    // ================================================================
    public static class UnstableVortex extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> middleRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private BlockDisplayHandle centerEye;

        public UnstableVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("unstable_vortex", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring: 12 crying obsidian, 7-block diameter
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.7f, 0.4f, 0.7f).glow(128, 0, 255).interpolation(2, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Middle ring: 8 dark prismarine
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 2.2, 0, Math.sin(angle) * 2.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.6f, 0.4f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                middleRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner ring: 4 amethyst
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 0, Math.sin(angle) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.4f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                innerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center eye: sea lantern hovering
            centerEye = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.SEA_LANTERN);
            centerEye.scale(1.0f, 0.8f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(centerEye.entity());

            // 4 end rod arms in X pattern at ground level
            double[][] rodOffsets = {{1.5, 0, 1.5}, {-1.5, 0, 1.5}, {1.5, 0, -1.5}, {-1.5, 0, -1.5}};
            for (double[] off : rodOffsets) {
                BlockDisplayHandle rod = displayBuilder.spawnBlock(center.clone().add(off[0], 0, off[2]), Material.END_ROD);
                rod.scale(1.0f, 0.2f, 0.2f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Counter-rotating rings
            float outerAngle = ticksAlive * 0.0175f;        // 1 deg/tick clockwise
            float middleAngle = -ticksAlive * 0.0262f;       // 1.5 deg/tick counter
            float innerAngle = ticksAlive * 0.035f;           // 2 deg/tick clockwise
            float eyeAngle = -ticksAlive * 0.0524f;           // 3 deg/tick counter

            for (int i = 0; i < outerRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 12 + outerAngle;
                outerRing.get(i).entity().teleport(center.clone().add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5));
            }
            for (int i = 0; i < middleRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8 + middleAngle;
                middleRing.get(i).entity().teleport(center.clone().add(Math.cos(angle) * 2.2, 0, Math.sin(angle) * 2.2));
            }
            for (int i = 0; i < innerRing.size(); i++) {
                double angle = (Math.PI * 2 * i) / 4 + innerAngle;
                innerRing.get(i).entity().teleport(center.clone().add(Math.cos(angle) * 1.0, 0, Math.sin(angle) * 1.0));
            }

            // Eye rotation
            if (centerEye != null) {
                BlockDisplay bd = centerEye.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.4f, -0.5f),
                        new AxisAngle4f(eyeAngle, 0, 1, 0),
                        new Vector3f(1.0f, 0.8f, 1.0f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Portal particles spiraling inward
            if (ticksAlive % 2 == 0) {
                double angle = ticksAlive * 0.1;
                double dist = 3.5 - (ticksAlive % 20) * 0.175;
                Location particleLoc = center.clone().add(Math.cos(angle) * dist, 0.3, Math.sin(angle) * dist);
                center.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 3, 0.1, 0.1, 0.1, 0.05);
            }

            // Laser from inner ring arms to eye
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle h : innerRing) {
                    Location from = h.entity().getLocation().add(0, 0.3, 0);
                    Location to = center.clone().add(0, 0.5, 0);
                    for (int p = 0; p < 3; p++) {
                        double t = p / 2.0;
                        Location particle = from.clone().add(
                                (to.getX() - from.getX()) * t,
                                (to.getY() - from.getY()) * t,
                                (to.getZ() - from.getZ()) * t
                        );
                        center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0, 0, 0, 0);
                    }
                }
            }

            // Dragon breath rising from center
            if (ticksAlive % 6 == 0) {
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        center.clone().add(0, 0.5, 0), 3, 0.2, 0.5, 0.2, 0.02);
            }

            // Electric spark bursts every 30 ticks
            if (ticksAlive % 30 == 0) {
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        center.clone().add(0, 0.5, 0), 15, 2.0, 0.5, 2.0, 0.05);
            }

            // Sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new UnstableVortex(plugin); }
    }

    // ================================================================
    // 25. SERPENT DOOR -- Maw-shaped gateway honoring DoG
    // ================================================================
    public static class SerpentDoor extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> upperJaw = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerJaw = new ArrayList<>();
        private final List<BlockDisplayHandle> upperTeeth = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerTeeth = new ArrayList<>();

        public SerpentDoor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("serpent_door", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Upper jaw: 8 polished blackstone in curved row
            for (int i = 0; i < 8; i++) {
                double x = (i - 3.5) * 0.5;
                double y = 3.5 + Math.sin((i / 7.0) * Math.PI) * 1.0;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.5f, 0.6f, 0.8f).glow(80, 80, 100).interpolation(2, 0);
                upperJaw.add(h);
                spawnedEntities.add(h.entity());
            }

            // Lower jaw: 8 polished blackstone
            for (int i = 0; i < 8; i++) {
                double x = (i - 3.5) * 0.5;
                double y = -Math.sin((i / 7.0) * Math.PI) * 0.5;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.5f, 0.6f, 0.8f).glow(80, 80, 100).interpolation(2, 0);
                lowerJaw.add(h);
                spawnedEntities.add(h.entity());
            }

            // Upper teeth: 6 large amethyst clusters pointing downward
            for (int i = 0; i < 6; i++) {
                double x = (i - 2.5) * 0.6;
                Location loc = center.clone().add(x, 3.0, 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.3f, 0.6f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                upperTeeth.add(h);
                spawnedEntities.add(h.entity());
            }

            // Lower teeth: 6 large amethyst clusters pointing upward
            for (int i = 0; i < 6; i++) {
                double x = (i - 2.5) * 0.6;
                Location loc = center.clone().add(x, 0.5, 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.3f, 0.6f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                lowerTeeth.add(h);
                spawnedEntities.add(h.entity());
            }

            // Eyes: 2 sea lanterns at gate opening corners
            for (int side = -1; side <= 1; side += 2) {
                BlockDisplayHandle eye = displayBuilder.spawnBlock(
                        center.clone().add(side * 1.8, 3.0, 0), Material.SEA_LANTERN);
                eye.scale(0.5f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(eye.entity());
            }

            // 4 flanking dark prismarine pillars
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 0; y < 3; y++) {
                    BlockDisplayHandle pillar = displayBuilder.spawnBlock(
                            center.clone().add(side * 2.5, y, 0), Material.DARK_PRISMARINE);
                    pillar.scale(0.6f, 0.9f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                    spawnedEntities.add(pillar.entity());
                }
            }

            // Crown: 4 amethyst blocks + 4 end rods
            for (int i = 0; i < 4; i++) {
                double x = (i - 1.5) * 0.6;
                BlockDisplayHandle crown = displayBuilder.spawnBlock(
                        center.clone().add(x, 5, 0), Material.AMETHYST_BLOCK);
                crown.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(crown.entity());

                BlockDisplayHandle rod = displayBuilder.spawnBlock(
                        center.clone().add(x, 5.5, 0), Material.END_ROD);
                rod.scale(0.15f, 0.5f, 0.15f).glow(240, 240, 255).interpolation(2, 0);
                spawnedEntities.add(rod.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Jaws chew slowly: upper descends 0.3, lower rises 0.3, 60-tick cycle
            float jawClose = 0.3f * (float) Math.sin(ticksAlive * (2 * Math.PI / 60));

            for (int i = 0; i < upperJaw.size(); i++) {
                double x = (i - 3.5) * 0.5;
                double y = 3.5 + Math.sin((i / 7.0) * Math.PI) * 1.0 - jawClose;
                upperJaw.get(i).entity().teleport(center.clone().add(x, y, 0));
            }
            for (int i = 0; i < lowerJaw.size(); i++) {
                double x = (i - 3.5) * 0.5;
                double y = -Math.sin((i / 7.0) * Math.PI) * 0.5 + jawClose;
                lowerJaw.get(i).entity().teleport(center.clone().add(x, y, 0));
            }

            // Teeth follow jaws
            for (int i = 0; i < upperTeeth.size(); i++) {
                double x = (i - 2.5) * 0.6;
                upperTeeth.get(i).entity().teleport(center.clone().add(x, 3.0 - jawClose, 0.3));
            }
            for (int i = 0; i < lowerTeeth.size(); i++) {
                double x = (i - 2.5) * 0.6;
                lowerTeeth.get(i).entity().teleport(center.clone().add(x, 0.5 + jawClose, 0.3));
            }

            // Electric spark arcs between opposing teeth
            if (ticksAlive % 10 == 0) {
                int toothIdx = (ticksAlive / 10) % upperTeeth.size();
                Location from = upperTeeth.get(toothIdx).entity().getLocation();
                Location to = lowerTeeth.get(toothIdx).entity().getLocation();
                for (int p = 0; p < 4; p++) {
                    double t = p / 3.0;
                    Location particle = from.clone().add(
                            0, (to.getY() - from.getY()) * t, 0
                    );
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0.05, 0, 0.05, 0);
                }
            }

            // Dragon breath from throat (behind teeth)
            if (ticksAlive % 4 == 0) {
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                        center.clone().add(0, 1.8, -0.5), 4, 0.5, 0.5, 0.2, 0.02);
            }

            // Sound every 300 ticks
            if (ticksAlive % 300 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SerpentDoor(plugin); }
    }

    // ================================================================
    // 26. DIMENSIONAL TEAR -- Vertical crack in reality
    // ================================================================
    public static class DimensionalTear extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tearBlocks = new ArrayList<>();

        public DimensionalTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_tear", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Jagged vertical tear: alternating dark blocks
            for (int y = 0; y < 6; y++) {
                float xOff = (y % 2 == 0) ? -0.2f : 0.2f;
                Location loc = center.clone().add(xOff, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 0.8f, 0.15f).glow(128, 0, 255).interpolation(2, 0);
                tearBlocks.add(h);
                spawnedEntities.add(h.entity());

                // Interior glow
                BlockDisplayHandle glow = displayBuilder.spawnBlock(loc.clone().add(0, 0, 0.05), Material.CYAN_STAINED_GLASS);
                glow.scale(0.15f, 0.6f, 0.05f).glow(0, 200, 255).interpolation(2, 0);
                tearBlocks.add(glow);
                spawnedEntities.add(glow.entity());
            }

            // Amethyst shards radiating from tear edges
            for (int i = 0; i < 8; i++) {
                double y = 0.5 + i * 0.7;
                double xOff = ((i % 2 == 0) ? -1 : 1) * (0.4 + (i % 3) * 0.15);
                Location loc = center.clone().add(xOff, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 20, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Tear pulses width on 40-tick cycle
            float pulse = 0.3f + 0.1f * (float) Math.sin(ticksAlive * (2 * Math.PI / 40));
            for (int i = 0; i < tearBlocks.size(); i += 2) {
                tearBlocks.get(i).scale(pulse, 0.8f, 0.15f);
                tearBlocks.get(i).interpolation(2, 0);
            }

            // Portal particles seeping from tear
            if (ticksAlive % 3 == 0) {
                int y = (ticksAlive / 3) % 6;
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, y + 0.5, 0.3), 3, 0.1, 0.2, 0.1, 0.1);
            }

            // Electric spark at tear edges
            if (ticksAlive % 8 == 0) {
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        center.clone().add(0, 3, 0), 4, 0.2, 2.5, 0.1, 0.02);
            }

            // White dust falling from top edge
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 2, 0.3);
            }

            // Sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalTear(plugin); }
    }

    // ================================================================
    // 27. COLLAPSING PORTAL -- Shrinking portal frame
    // ================================================================
    public static class CollapsingPortal extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> debrisBlocks = new ArrayList<>();

        public CollapsingPortal(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("collapsing_portal", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Circular portal frame: 12 amethyst blocks in ring
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, Math.sin(angle) * 2.5 + 2.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.6f, 0.6f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                frameBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Interior: dark prismarine glass-like fill
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.2, Math.sin(angle) * 1.2 + 2.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.8f, 0.8f, 0.1f).glow(0, 200, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Debris scattered around base
            for (int i = 0; i < 8; i++) {
                double x = (Math.random() - 0.5) * 4;
                double z = (Math.random() - 0.5) * 4;
                Location loc = center.clone().add(x, 0, z);
                Material mat = (i % 2 == 0) ? Material.DARK_PRISMARINE : Material.AMETHYST_CLUSTER;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
                debrisBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Portal frame slowly collapses: radius shrinks over time
            float collapseProgress = Math.min(ticksAlive / 500.0f, 1.0f);
            float radius = 2.5f * (1.0f - collapseProgress * 0.6f);

            for (int i = 0; i < frameBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 12 + ticksAlive * 0.01;
                Location loc = center.clone().add(
                        Math.cos(angle) * radius,
                        Math.sin(angle) * radius + 2.5,
                        0
                );
                frameBlocks.get(i).entity().teleport(loc);
            }

            // Portal particles
            if (ticksAlive % 3 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 2.5, 0), 5, radius * 0.5, radius * 0.5, 0.1, 0.1);
            }

            // Electric sparks as frame destabilizes
            if (ticksAlive % 6 == 0 && frameBlocks.size() >= 2) {
                int idx = (ticksAlive / 6) % frameBlocks.size();
                int next = (idx + 1) % frameBlocks.size();
                Location from = frameBlocks.get(idx).entity().getLocation();
                Location to = frameBlocks.get(next).entity().getLocation();
                for (int p = 0; p < 3; p++) {
                    double t = p / 2.0;
                    Location particle = from.clone().add(
                            (to.getX() - from.getX()) * t,
                            (to.getY() - from.getY()) * t,
                            (to.getZ() - from.getZ()) * t
                    );
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particle, 1, 0.1, 0.1, 0.1, 0);
                }
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CollapsingPortal(plugin); }
    }

    // ================================================================
    // 28. RIFT BEACON -- Tall pillar emitting dimensional signal
    // ================================================================
    public static class RiftBeacon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private BlockDisplayHandle beaconTop;

        public RiftBeacon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_beacon", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pillar: 8 blocks tall, alternating dark prismarine and amethyst
            for (int y = 0; y < 8; y++) {
                Material mat = (y % 2 == 0) ? Material.DARK_PRISMARINE : Material.AMETHYST_BLOCK;
                Location loc = center.clone().add(0, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float taper = 1.0f - (y * 0.05f);
                h.scale(taper, 1.0f, taper).glow(0, 200, 255).interpolation(2, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Beacon top: sea lantern
            beaconTop = displayBuilder.spawnBlock(center.clone().add(0, 8, 0), Material.SEA_LANTERN);
            beaconTop.scale(1.2f, 0.8f, 1.2f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(beaconTop.entity());

            // 4 amethyst clusters at corners of base
            double[][] corners = {{1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1}};
            for (double[] c : corners) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(c[0], c[1], c[2]), Material.AMETHYST_CLUSTER);
                h.scale(0.5f, 0.6f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Crying obsidian base ring
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8;
                Location loc = center.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.3f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Beacon top pulses scale: 1.2 to 1.6, 60-tick cycle
            float topScale = 1.2f + 0.4f * (float) Math.sin(ticksAlive * (2 * Math.PI / 60));
            if (beaconTop != null) {
                beaconTop.scale(topScale, 0.8f, topScale);
                beaconTop.interpolation(2, 0);
            }

            // Vertical beam from beacon top upward
            if (ticksAlive % 3 == 0) {
                for (int y = 8; y < 18; y++) {
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            center.clone().add(0, y, 0), 1, 0.1, 0, 0.1, 0);
                }
            }

            // Portal particles spiraling up pillar
            if (ticksAlive % 4 == 0) {
                double spiralAngle = ticksAlive * 0.15;
                int spiralY = (ticksAlive / 4) % 8;
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(Math.cos(spiralAngle) * 0.8, spiralY, Math.sin(spiralAngle) * 0.8),
                        2, 0.1, 0.1, 0.1, 0.02);
            }

            // Cyan dust at base
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 4, 1.5);
            }

            // Sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftBeacon(plugin); }
    }

    // ================================================================
    // 29. VOID WINDOW -- Flat horizontal rift in the air
    // ================================================================
    public static class VoidWindow extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> windowFrame = new ArrayList<>();
        private final List<BlockDisplayHandle> windowPane = new ArrayList<>();

        public VoidWindow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_window", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location windowCenter = center.clone().add(0, 6, 0);

            // Frame: rectangular border of dark prismarine at Y+6
            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(x) < 2 && Math.abs(z) < 1) continue; // Skip inner
                    Location loc = windowCenter.clone().add(x, 0, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                    h.scale(0.9f, 0.2f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                    windowFrame.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Window pane: crying obsidian + cyan glass interior
            for (int x = -1; x <= 1; x++) {
                Location loc = windowCenter.clone().add(x, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                h.scale(0.9f, 0.1f, 0.9f).glow(0, 200, 255).interpolation(2, 0);
                windowPane.add(h);
                spawnedEntities.add(h.entity());
            }

            // Corner amethyst clusters
            double[][] corners = {{-2.5, 0, -1.5}, {-2.5, 0, 1.5}, {2.5, 0, -1.5}, {2.5, 0, 1.5}};
            for (double[] c : corners) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(windowCenter.clone().add(c[0], c[1], c[2]),
                        Material.AMETHYST_CLUSTER);
                h.scale(0.4f, 0.5f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Window tilts back and forth: 10 degrees, 100-tick cycle
            float tilt = 0.175f * (float) Math.sin(ticksAlive * (2 * Math.PI / 100));

            for (BlockDisplayHandle h : windowPane) {
                BlockDisplay bd = h.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.45f, -0.05f, -0.45f),
                        new AxisAngle4f(tilt, 1, 0, 0),
                        new Vector3f(0.9f, 0.1f, 0.9f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Portal particles seeping downward from window
            if (ticksAlive % 3 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 6, 0), 4, 1.5, 0.1, 1.0, 0.2);
            }

            // Electric spark at window edges
            if (ticksAlive % 8 == 0) {
                center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        center.clone().add(0, 6, 0), 3, 2.0, 0.1, 1.0, 0.02);
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidWindow(plugin); }
    }

    // ================================================================
    // 30. PHASE RIPPLE -- Concentric expanding rings on ground
    // ================================================================
    public static class PhaseRipple extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> rings = new ArrayList<>();

        public PhaseRipple(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase_ripple", AttackType.BLOCK_DISPLAY, 2));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 concentric rings at ground level
            Material[] ringMats = {Material.SEA_LANTERN, Material.AMETHYST_BLOCK, Material.DARK_PRISMARINE, Material.CRYING_OBSIDIAN};
            int[] ringCounts = {6, 10, 14, 18};
            double[] ringRadii = {1.0, 2.5, 4.0, 5.5};

            for (int ring = 0; ring < 4; ring++) {
                List<BlockDisplayHandle> ringList = new ArrayList<>();
                for (int i = 0; i < ringCounts[ring]; i++) {
                    double angle = (Math.PI * 2 * i) / ringCounts[ring];
                    Location loc = center.clone().add(Math.cos(angle) * ringRadii[ring], 0, Math.sin(angle) * ringRadii[ring]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, ringMats[ring]);
                    h.scale(0.5f, 0.2f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                    ringList.add(h);
                    spawnedEntities.add(h.entity());
                }
                rings.add(ringList);
            }

            // Central amethyst crystal
            BlockDisplayHandle core = displayBuilder.spawnBlock(center, Material.AMETHYST_CLUSTER);
            core.scale(0.6f, 1.0f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(core.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.6f);
            DisplayBuilder.cyanDust(center, 30, 5.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            double[] ringRadii = {1.0, 2.5, 4.0, 5.5};
            int[] ringCounts = {6, 10, 14, 18};

            // Rings expand outward in a wave pattern
            for (int ring = 0; ring < rings.size(); ring++) {
                float wave = 0.5f * (float) Math.sin((ticksAlive - ring * 15) * (2 * Math.PI / 80));
                float currentRadius = (float) ringRadii[ring] + wave;
                float yBob = 0.15f * (float) Math.sin((ticksAlive - ring * 10) * (2 * Math.PI / 60));

                List<BlockDisplayHandle> ringBlocks = rings.get(ring);
                float ringRot = ticksAlive * 0.005f * (ring % 2 == 0 ? 1 : -1);
                for (int i = 0; i < ringBlocks.size(); i++) {
                    double angle = (Math.PI * 2 * i) / ringCounts[ring] + ringRot;
                    Location loc = center.clone().add(
                            Math.cos(angle) * currentRadius, yBob, Math.sin(angle) * currentRadius);
                    ringBlocks.get(i).entity().teleport(loc);
                }
            }

            // Electric spark ripple wave from center outward
            if (ticksAlive % 20 == 0) {
                double rippleRadius = (ticksAlive % 80) / 80.0 * 6.0;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    Location loc = center.clone().add(Math.cos(angle) * rippleRadius, 0.2, Math.sin(angle) * rippleRadius);
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 2, 0.1, 0.1, 0.1, 0);
                }
            }

            // Portal particles at center
            if (ticksAlive % 5 == 0) {
                center.getWorld().spawnParticle(Particle.PORTAL,
                        center.clone().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0.1);
            }

            // Sound every 100 ticks
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseRipple(plugin); }
    }
}
