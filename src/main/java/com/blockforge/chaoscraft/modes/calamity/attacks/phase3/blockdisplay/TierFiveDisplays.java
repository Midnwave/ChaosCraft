package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.blockdisplay;

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
 * Phase 4C Block Display -- FINALE: "THE DWELLER'S DOMAIN"
 * Structures 91-100. The 10 most visually spectacular structures in Boss 3.
 * Deployed during the death sequence and final percent of health.
 * Structures 91-99 deploy in overlapping waves. Structure 100 is the
 * single most visually dense structure in all of Boss 3.
 *
 * Dweller palette: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255)
 * Rules: NO status effects, AxisAngle4f ONLY, damage 10.0-14.0 HP
 */
public final class TierFiveDisplays {

    private TierFiveDisplays() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TheBrimstoneThrone(plugin));
        registry.register(new EruptionFields(plugin));
        registry.register(new TheBrimstoneHorizon(plugin));
        registry.register(new TheDwellersGaze(plugin));
        registry.register(new TheBurningRiverOfVoid(plugin));
        registry.register(new TheCalamityColumn(plugin));
        registry.register(new TheMarchingDead(plugin));
        registry.register(new BrimstoneSkyCollapse(plugin));
        registry.register(new TheDwellersDescent(plugin));
        registry.register(new TheDomain(plugin));
    }

    // ================================================================
    // 91. THE BRIMSTONE THRONE — Colossal 20x28x12 throne structure,
    //     utterly motionless except for crown fire and eye projections
    // ================================================================
    public static class TheBrimstoneThrone extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> seatBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> armrestBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> backrestBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> veinBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> crownFires = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> legBlocks = new ArrayList<>();

        public TheBrimstoneThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_throne", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(10.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(1500);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Seat: 10x6x3 slab of blackstone
            for (int x = -5; x < 5; x++) {
                for (int z = -3; z < 3; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 4, z), Material.BLACKSTONE);
                    h.scale(1.0f, 0.5f, 1.0f).glow(40, 40, 50).interpolation(2, 0);
                    seatBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Throne legs: 4 basalt columns at base corners
            double[][] legPositions = {{-4, 0, -2}, {4, 0, -2}, {-4, 0, 2}, {4, 0, 2}};
            for (double[] lp : legPositions) {
                for (int y = 0; y < 4; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(lp[0], y, lp[2]), Material.BASALT);
                    h.scale(1.5f, 1.0f, 1.5f).glow(70, 70, 80).interpolation(2, 0);
                    legBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Armrests: 2 columns rising from seat sides
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 5; y < 18; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(side * 5, y, 0), Material.BASALT);
                    h.scale(1.0f, 1.0f, 1.0f).glow(70, 70, 80).interpolation(2, 0);
                    armrestBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Backrest: arched panel 14 wide x 16 tall
            for (int x = -7; x <= 6; x++) {
                for (int y = 5; y < 20; y++) {
                    // Arch shape: skip blocks outside the arch
                    double archRadius = 7.0;
                    double archCenter = 12.5;
                    if (y > 14) {
                        double dist = Math.sqrt(x * x + (y - archCenter) * (y - archCenter));
                        if (dist > archRadius) continue;
                    }
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, -3), Material.NETHERRACK);
                    h.scale(1.0f, 1.0f, 0.5f).glow(120, 60, 40).interpolation(2, 0);
                    backrestBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Arch frame: polished blackstone
            for (int i = 0; i < 16; i++) {
                double t = (double) i / 15;
                double x = -7 + t * 13;
                double y = 5 + (i < 8 ? i : 15 - i); // Simplified arch
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y + 10, -3.2), Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 0.8f, 0.3f).glow(50, 50, 60).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            // Magma vein inlay on backrest (branching crack motif)
            double[][] veinPositions = {{-3, 8, -2.8}, {-1, 10, -2.8}, {1, 9, -2.8}, {3, 11, -2.8},
                    {-2, 12, -2.8}, {0, 14, -2.8}, {2, 13, -2.8}, {-4, 7, -2.8},
                    {4, 8, -2.8}, {0, 16, -2.8}, {-1, 7, -2.8}, {1, 11, -2.8},
                    {-3, 15, -2.8}, {3, 14, -2.8}, {-5, 9, -2.8}, {5, 10, -2.8},
                    {-2, 6, -2.8}, {2, 7, -2.8}, {0, 18, -2.8}, {-1, 13, -2.8}};
            for (double[] vp : veinPositions) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(vp[0], vp[1], vp[2]), Material.MAGMA_BLOCK);
                h.scale(0.4f, 0.6f, 0.2f).glow(255, 100, 0).interpolation(3, 0);
                veinBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crown fires: 3 at top of arch (left, center, right)
            double[] crownX = {-3, 0, 3};
            for (double cx : crownX) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(cx, 21, -3), Material.MAGMA_BLOCK);
                h.scale(1.0f, 1.0f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                crownFires.add(h);
                spawnedEntities.add(h.entity());
            }

            // Dweller eyes in backrest: 2 orange stained glass
            BlockDisplayHandle eyeL = displayBuilder.spawnBlock(
                    center.clone().add(-2, 14, -2.7), Material.ORANGE_STAINED_GLASS);
            eyeL.scale(0.6f, 0.4f, 0.15f).glow(255, 150, 0).interpolation(3, 0);
            eyeBlocks.add(eyeL);
            spawnedEntities.add(eyeL.entity());

            BlockDisplayHandle eyeR = displayBuilder.spawnBlock(
                    center.clone().add(2, 14, -2.7), Material.ORANGE_STAINED_GLASS);
            eyeR.scale(0.6f, 0.4f, 0.15f).glow(255, 150, 0).interpolation(3, 0);
            eyeBlocks.add(eyeR);
            spawnedEntities.add(eyeR.entity());

            // Netherite sword decorations below armrests
            for (int side = -1; side <= 1; side += 2) {
                BlockDisplayHandle sword = displayBuilder.spawnBlock(
                        center.clone().add(side * 4.5, 5, 0), Material.BLACKSTONE);
                sword.scale(0.15f, 1.5f, 0.15f).glow(30, 30, 40)
                        .rotate(0.4f * side, 0, 0, 1).interpolation(2, 0);
                spawnedEntities.add(sword.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // THE THRONE DOES NOT MOVE. Only accents animate.

            // Crown fire rotation: 2.0°/tick each
            float crownRot = ticksAlive * 0.0349f;
            for (BlockDisplayHandle h : crownFires) {
                h.rotate(crownRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Eye pulse: alternating 60-tick cycle, scale 0.6 to 0.85
            float eyeLScale = 0.6f + 0.25f * (float) Math.sin(ticksAlive * Math.PI / 30);
            float eyeRScale = 0.6f + 0.25f * (float) Math.sin((ticksAlive + 30) * Math.PI / 30);
            if (eyeBlocks.size() >= 2) {
                eyeBlocks.get(0).scale(eyeLScale, 0.4f, 0.15f);
                eyeBlocks.get(0).interpolation(3, 0);
                eyeBlocks.get(1).scale(eyeRScale, 0.4f, 0.15f);
                eyeBlocks.get(1).interpolation(3, 0);
            }

            // Magma vein pulse every 120 ticks: all simultaneously
            if (ticksAlive % 120 < 10) {
                float veinScale = 0.4f + 0.06f * (float)((ticksAlive % 120) / 10.0);
                for (BlockDisplayHandle h : veinBlocks) {
                    h.scale(veinScale, 0.6f + veinScale * 0.2f, 0.2f);
                    h.interpolation(3, 0);
                }
            }

            // Flame from crown fires — dense columns
            if (ticksAlive % 2 == 0) {
                for (BlockDisplayHandle cf : crownFires) {
                    w.spawnParticle(Particle.FLAME, cf.entity().getLocation().add(0, 1, 0),
                            5, 0.1, 0.5, 0.1, 0.03);
                }
            }

            // Lava drips from magma veins
            if (ticksAlive % 4 == 0) {
                int vIdx = (ticksAlive / 4) % veinBlocks.size();
                w.spawnParticle(Particle.LAVA, veinBlocks.get(vIdx).entity().getLocation(), 1,
                        0.1, 0.3, 0.1, 0);
            }

            // Soul fire flame projections from eyes — 10-block reach
            if (ticksAlive % 2 == 0) {
                for (BlockDisplayHandle eye : eyeBlocks) {
                    Location eLoc = eye.entity().getLocation();
                    for (int d = 0; d < 10; d++) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, eLoc.clone().add(0, 0, d + 1),
                                1, 0.05, 0.05, 0.05, 0.01);
                    }
                }
            }

            // Dripping lava from armrest tops
            if (ticksAlive % 8 == 0) {
                for (int side = -1; side <= 1; side += 2) {
                    w.spawnParticle(Particle.DRIPPING_LAVA, center.clone().add(side * 5, 18, 0),
                            1, 0.1, 0, 0.1, 0);
                }
            }

            // Sound: low continuous rumble
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheBrimstoneThrone(plugin); }
    }

    // ================================================================
    // 92. ERUPTION FIELDS — 20 geysers across the arena floor in
    //     hexagonal packing with wave-pattern eruption cycle
    // ================================================================
    public static class EruptionFields extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> geysers = new ArrayList<>();
        private final List<BlockDisplayHandle> channels = new ArrayList<>();

        public EruptionFields(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eruption_fields", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(1300);
            config.setCooldownTicks(600);
            config.setImpactDamage(10.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 20 geysers in hex-ish distribution across 50-block arena
            double[][] geyserPositions = {
                    {0, 0}, {6, 0}, {-6, 0}, {3, 5.2}, {-3, 5.2}, {3, -5.2}, {-3, -5.2},
                    {9, 5.2}, {-9, 5.2}, {9, -5.2}, {-9, -5.2}, {12, 0}, {-12, 0},
                    {0, 10.4}, {0, -10.4}, {6, 10.4}, {-6, 10.4}, {6, -10.4}, {-6, -10.4}, {15, 5}
            };

            for (double[] gp : geyserPositions) {
                List<BlockDisplayHandle> geyser = new ArrayList<>();
                Location gBase = center.clone().add(gp[0], 0, gp[1]);

                // Chimney: 4-6 blocks of netherrack/basalt
                int chimH = 3 + (int) (Math.abs(gp[0] + gp[1]) % 4);
                for (int y = 0; y < chimH; y++) {
                    Material mat = y % 2 == 0 ? Material.NETHERRACK : Material.BASALT;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(gBase.clone().add(0, y, 0), mat);
                    h.scale(0.7f, 1.0f, 0.7f).glow(100, 50, 30).interpolation(3, 0);
                    geyser.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Magma cap
                BlockDisplayHandle cap = displayBuilder.spawnBlock(
                        gBase.clone().add(0, chimH, 0), Material.MAGMA_BLOCK);
                cap.scale(0.8f, 0.5f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
                geyser.add(cap);
                spawnedEntities.add(cap.entity());

                geysers.add(geyser);
            }

            // Connect adjacent geysers with channel trenches (simplified)
            for (int i = 0; i < 15; i++) {
                double[] from = geyserPositions[i];
                double[] to = geyserPositions[(i + 1) % geyserPositions.length];
                double dx = to[0] - from[0];
                double dz = to[1] - from[1];
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 15) continue;
                for (int seg = 1; seg < (int) dist; seg += 2) {
                    double t = seg / dist;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(from[0] + dx * t, 0, from[1] + dz * t), Material.NETHERRACK);
                    h.scale(0.6f, 0.3f, 0.6f).glow(100, 50, 30).interpolation(2, 0);
                    channels.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Wave eruption: ripple from center outward
            // Cycle: 80 ticks total, center fires first, then outward rings
            int cyclePos = ticksAlive % 80;

            for (int g = 0; g < geysers.size(); g++) {
                // Distance-based delay: closer geysers fire earlier in cycle
                List<BlockDisplayHandle> geyser = geysers.get(g);
                if (geyser.isEmpty()) continue;
                Location gLoc = geyser.get(0).entity().getLocation();
                double dist = gLoc.distance(center);
                int delay = (int) (dist / 5) * 10; // 10 ticks per 5-block ring

                if (cyclePos == delay % 80) {
                    // Fire this geyser
                    BlockDisplayHandle cap = geyser.get(geyser.size() - 1);
                    cap.scale(1.2f, 0.8f, 1.2f);
                    cap.interpolation(5, 0);
                    Location capLoc = cap.entity().getLocation();
                    w.spawnParticle(Particle.LAVA, capLoc.clone().add(0, 2, 0), 10, 1, 4, 1, 0.1);
                    triggerImpactDamage(capLoc);
                    DisplayBuilder.playSound(capLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.6f);
                }

                // Reset cap after 10 ticks
                if (cyclePos == (delay + 10) % 80) {
                    BlockDisplayHandle cap = geyser.get(geyser.size() - 1);
                    cap.scale(0.8f, 0.5f, 0.8f);
                    cap.interpolation(10, 0);
                }
            }

            // Geyser rotation: 0.3°/tick when off-cycle
            float gRot = ticksAlive * 0.00524f;
            for (List<BlockDisplayHandle> geyser : geysers) {
                for (BlockDisplayHandle h : geyser) {
                    h.rotate(gRot, 0, 1, 0);
                    h.interpolation(4, 0);
                }
            }

            // Flame from all geysers
            if (ticksAlive % 5 == 0) {
                for (List<BlockDisplayHandle> geyser : geysers) {
                    if (!geyser.isEmpty()) {
                        w.spawnParticle(Particle.FLAME, geyser.get(geyser.size() - 1).entity().getLocation().add(0, 0.5, 0),
                                1, 0.1, 0.2, 0.1, 0.01);
                    }
                }
            }

            // Dripping lava in channels
            if (ticksAlive % 6 == 0 && !channels.isEmpty()) {
                int cIdx = (ticksAlive / 6) % channels.size();
                w.spawnParticle(Particle.DRIPPING_LAVA, channels.get(cIdx).entity().getLocation(),
                        1, 0.1, 0, 0.1, 0);
            }

            // Sound
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.7f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EruptionFields(plugin); }
    }

    // ================================================================
    // 93. THE BRIMSTONE HORIZON — 4 walls closing inward simultaneously
    // ================================================================
    public static class TheBrimstoneHorizon extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> walls = new ArrayList<>();
        private float wallRadius = 30.0f;

        public TheBrimstoneHorizon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_horizon", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(1300);
            config.setCooldownTicks(600);
            config.setImpactDamage(8.0);
            config.setImpactRadius(10.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 walls at compass edges, each 40 long x 12 tall
            double[][] wallDirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (double[] dir : wallDirs) {
                List<BlockDisplayHandle> wall = new ArrayList<>();
                for (int seg = -20; seg < 20; seg += 2) {
                    for (int y = 0; y < 12; y += 2) {
                        double wx = dir[0] * 30 + (dir[1] != 0 ? seg : 0);
                        double wz = dir[1] * 30 + (dir[0] != 0 ? seg : 0);
                        Material mat;
                        if (y < 6) mat = (seg + y) % 4 == 0 ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                        else if (y < 10) mat = (seg + y) % 3 == 0 ? Material.BLACKSTONE : Material.BASALT;
                        else mat = Material.BASALT;

                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(wx, y, wz), mat);
                        h.scale(2.0f, 2.0f, 1.0f).glow(mat == Material.MAGMA_BLOCK ? 255 : 80,
                                mat == Material.MAGMA_BLOCK ? 100 : 60, mat == Material.MAGMA_BLOCK ? 0 : 50)
                                .interpolation(3, 0);
                        wall.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                walls.add(wall);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // All 4 walls translate inward at 0.03 blocks/tick
            float inwardStep = -0.03f;
            wallRadius += inwardStep;
            if (wallRadius < 12.0f) wallRadius = 12.0f;

            // Move wall blocks
            double[][] wallDirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int wi = 0; wi < walls.size() && wi < 4; wi++) {
                double[] dir = wallDirs[wi];
                for (BlockDisplayHandle h : walls.get(wi)) {
                    Location loc = h.entity().getLocation();
                    loc.add(dir[0] * inwardStep, 0, dir[1] * inwardStep);
                    h.entity().teleport(loc);
                }
            }

            // Warning smoke at 15-block radius
            if (Math.abs(wallRadius - 15) < 0.5f) {
                w.spawnParticle(Particle.SMOKE, center, 40, 7, 1, 7, 0.1);
            }

            // Impact at minimum radius
            if (wallRadius <= 12.5f && wallRadius > 12.0f) {
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.4f);
            }

            // Flame projections from inner wall faces
            if (ticksAlive % 3 == 0) {
                for (int wi = 0; wi < 4; wi++) {
                    double[] dir = wallDirs[wi];
                    Location flameLoc = center.clone().add(dir[0] * wallRadius, 4, dir[1] * wallRadius);
                    w.spawnParticle(Particle.FLAME, flameLoc, 3, -dir[0] * 2, 0.3, -dir[1] * 2, 0.02);
                }
            }

            // Lava bursts from magma rows
            if (ticksAlive % 5 == 0) {
                for (List<BlockDisplayHandle> wall : walls) {
                    if (!wall.isEmpty()) {
                        int idx = (ticksAlive / 5) % wall.size();
                        w.spawnParticle(Particle.LAVA, wall.get(idx).entity().getLocation(), 2,
                                0.5, 0.3, 0.5, 0);
                    }
                }
            }

            // Smoke from crown
            if (ticksAlive % 6 == 0) {
                for (List<BlockDisplayHandle> wall : walls) {
                    if (wall.size() > 5) {
                        w.spawnParticle(Particle.SMOKE, wall.get(wall.size() - 1).entity().getLocation().add(0, 1, 0),
                                2, 0.5, 0.3, 0.5, 0.01);
                    }
                }
            }

            // Sound: grinding
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheBrimstoneHorizon(plugin); }
    }

    // ================================================================
    // 94. THE DWELLER'S GAZE — Arena-scale 30x15 eye overhead at Y+20
    //     looking down with iris rotation and soul fire pupil beam
    // ================================================================
    public static class TheDwellersGaze extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> eyelidBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> irisBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> pupilBlocks = new ArrayList<>();
        private BlockDisplayHandle pupilCenter;

        public TheDwellersGaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dwellers_gaze", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(10.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(1400);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Eyelid ring: 52 netherrack in wide oval at Y+20, horizontal (facing down)
            for (int i = 0; i < 52; i++) {
                double angle = (Math.PI * 2 * i) / 52;
                double rx = 15.0, rz = 7.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * rx, 20, Math.sin(angle) * rz), Material.NETHERRACK);
                h.scale(1.5f, 0.5f, 1.5f).glow(120, 60, 40).interpolation(3, 0);
                eyelidBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Iris ring: 36 magma blocks
            for (int i = 0; i < 36; i++) {
                double angle = (Math.PI * 2 * i) / 36;
                double rx = 10.0, rz = 5.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * rx, 20, Math.sin(angle) * rz), Material.MAGMA_BLOCK);
                h.scale(1.2f, 0.5f, 1.2f).glow(255, 100, 0).interpolation(3, 0);
                irisBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Pupil: 16 blackstone blocks in tight oval
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                double rx = 4.0, rz = 2.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * rx, 20, Math.sin(angle) * rz), Material.BLACKSTONE);
                h.scale(1.0f, 0.5f, 1.0f).glow(40, 40, 50).interpolation(3, 0);
                pupilBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Pupil center: massive nether star (glowstone with cyan glow)
            pupilCenter = displayBuilder.spawnBlock(center.clone().add(0, 20, 0), Material.GLOWSTONE);
            pupilCenter.scale(2.5f, 1.0f, 2.5f).glow(0, 150, 255).interpolation(3, 0);
            spawnedEntities.add(pupilCenter.entity());

            // Crying obsidian corners (medial/lateral canthi)
            double[][] canthi = {{15, 20, 0}, {-15, 20, 0}, {0, 20, 7.5}, {0, 20, -7.5}};
            for (double[] c : canthi) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(c[0], c[1], c[2]), Material.CRYING_OBSIDIAN);
                h.scale(1.0f, 0.5f, 1.0f).glow(80, 0, 120).interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Iris counter-rotation: 0.8°/tick on Z-axis
            float irisRot = ticksAlive * 0.01396f;
            for (int i = 0; i < irisBlocks.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 36 + irisRot;
                Location loc = center.clone().add(Math.cos(baseAngle) * 10, 20, Math.sin(baseAngle) * 5);
                irisBlocks.get(i).entity().teleport(loc);
            }

            // Pupil ring counter-rotation: 0.5°/tick opposite
            float pupilRot = -ticksAlive * 0.00873f;
            for (int i = 0; i < pupilBlocks.size(); i++) {
                double baseAngle = (Math.PI * 2 * i) / 16 + pupilRot;
                Location loc = center.clone().add(Math.cos(baseAngle) * 4, 20, Math.sin(baseAngle) * 2.5);
                pupilBlocks.get(i).entity().teleport(loc);
            }

            // Pupil center tumble
            float centerRot = ticksAlive * 0.02618f;
            if (pupilCenter != null) {
                pupilCenter.rotate(centerRot, 0.6f, 1.5f, 0.3f);
                pupilCenter.interpolation(3, 0);
            }

            // Iris filament pulses (every 8th iris block)
            for (int i = 0; i < irisBlocks.size(); i += 4) {
                int offset = i * 15;
                float fScale = 1.2f + 0.5f * (float) Math.sin((ticksAlive + offset) * Math.PI / 7.5);
                irisBlocks.get(i).scale(fScale, 0.5f, fScale);
                irisBlocks.get(i).interpolation(3, 0);
            }

            // Soul fire beam from pupil center downward to ground
            if (ticksAlive % 2 == 0) {
                for (int h = 0; h < 20; h += 2) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, h, 0),
                            3, 1.0, 0.1, 1.0, 0.02);
                }
            }

            // Lava rain from rotating iris
            if (ticksAlive % 3 == 0) {
                int idx = (ticksAlive / 3) % irisBlocks.size();
                Location iLoc = irisBlocks.get(idx).entity().getLocation();
                double dx = iLoc.getX() - center.getX();
                double dz = iLoc.getZ() - center.getZ();
                w.spawnParticle(Particle.LAVA, iLoc, 1, dx * 0.1, -0.3, dz * 0.1, 0);
            }

            // Dripping lava from crying obsidian
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.DRIPPING_LAVA, center.clone().add(15, 20, 0), 1, 0.1, 0, 0.1, 0);
                w.spawnParticle(Particle.DRIPPING_LAVA, center.clone().add(-15, 20, 0), 1, 0.1, 0, 0.1, 0);
            }

            // Crimson spore from eyelid edge
            if (ticksAlive % 7 == 0) {
                int eIdx = (ticksAlive / 7) % eyelidBlocks.size();
                w.spawnParticle(Particle.CRIMSON_SPORE, eyelidBlocks.get(eIdx).entity().getLocation(),
                        2, 0.5, 0.3, 0.5, 0);
            }

            // Sound: deep stare
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.1f);
            }
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheDwellersGaze(plugin); }
    }

    // ================================================================
    // 95. THE BURNING RIVER OF VOID — 6-wide 40-long S-curve river
    //     with whirlpool structures and flowing lava pulse
    // ================================================================
    public static class TheBurningRiverOfVoid extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> channelFloor = new ArrayList<>();
        private final List<BlockDisplayHandle> lavaSurface = new ArrayList<>();
        private final List<BlockDisplayHandle> whirlpools = new ArrayList<>();
        private final List<BlockDisplayHandle> channelWalls = new ArrayList<>();

        public TheBurningRiverOfVoid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("burning_river_of_void", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(1300);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // S-curve river: 40 blocks along X with Z offset for curve
            for (int seg = 0; seg < 40; seg++) {
                double zCurve = Math.sin(seg * Math.PI / 20) * 6; // S-curve

                // Channel floor: magma blocks, 3 wide
                for (int dz = -1; dz <= 1; dz++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(seg - 20, 2, zCurve + dz), Material.MAGMA_BLOCK);
                    h.scale(1.0f, 0.5f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                    channelFloor.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Lava surface: orange stained glass (every 2 segments)
                if (seg % 2 == 0) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(seg - 20, 2.5, zCurve), Material.ORANGE_STAINED_GLASS);
                    h.scale(2.0f, 0.15f, 3.0f).glow(255, 150, 0).interpolation(3, 0);
                    lavaSurface.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Channel walls: blackstone, 2-block tall on both sides
                if (seg % 3 == 0) {
                    for (int side = -1; side <= 1; side += 2) {
                        for (int y = 2; y < 4; y++) {
                            BlockDisplayHandle h = displayBuilder.spawnBlock(
                                    center.clone().add(seg - 20, y, zCurve + side * 2.5), Material.BLACKSTONE);
                            h.scale(1.0f, 1.0f, 0.5f).glow(40, 40, 50).interpolation(2, 0);
                            channelWalls.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
            }

            // 5 whirlpool structures at intervals along the S-curve
            int[] whirlpoolSegs = {4, 12, 20, 28, 36};
            for (int ws : whirlpoolSegs) {
                double zCurve = Math.sin(ws * Math.PI / 20) * 6;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(ws - 20 + Math.cos(angle) * 3.5, 2, zCurve + Math.sin(angle) * 3.5),
                            Material.NETHERRACK);
                    h.scale(1.0f, 0.4f, 1.0f).glow(120, 60, 40).interpolation(3, 0);
                    whirlpools.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Soul soil center
                BlockDisplayHandle sc = displayBuilder.spawnBlock(
                        center.clone().add(ws - 20, 2, zCurve), Material.SOUL_SOIL);
                sc.scale(1.0f, 0.3f, 1.0f).glow(0, 150, 255).interpolation(2, 0);
                whirlpools.add(sc);
                spawnedEntities.add(sc.entity());
            }

            // Source fountain
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-20, y + 2, 0), Material.CRACKED_STONE_BRICKS);
                h.scale(1.0f + (4 - y) * 0.3f, 1.0f, 1.0f + (4 - y) * 0.3f).glow(70, 70, 80)
                        .interpolation(2, 0);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Flowing pulse: lava surface blocks scale wave from source to terminus
            for (int i = 0; i < lavaSurface.size(); i++) {
                float wavePhase = (ticksAlive - i * 8) % 150;
                float waveScale = 1.0f + 0.06f * (float) Math.sin(wavePhase * Math.PI / 75);
                lavaSurface.get(i).scale(2.0f * waveScale, 0.15f, 3.0f * waveScale);
                lavaSurface.get(i).interpolation(3, 0);
            }

            // Whirlpool rotation: 0.6°/tick
            float whirlRot = ticksAlive * 0.01047f;
            for (BlockDisplayHandle h : whirlpools) {
                h.rotate(whirlRot, 0, 1, 0);
                h.interpolation(3, 0);
            }

            // Lava particles flowing along channel
            if (ticksAlive % 3 == 0) {
                int pos = (ticksAlive / 3) % 40;
                double zCurve = Math.sin(pos * Math.PI / 20) * 6;
                w.spawnParticle(Particle.LAVA, center.clone().add(pos - 20, 3, zCurve), 1, 0.5, 0.1, 0.5, 0);
            }

            // Soul fire from whirlpool centers
            if (ticksAlive % 4 == 0) {
                for (int i = 8; i < whirlpools.size(); i += 9) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            whirlpools.get(i).entity().getLocation().add(0, 0.5, 0),
                            3, 0.2, 0.3, 0.2, 0.01);
                }
            }

            // Dripping lava from terminus
            if (ticksAlive % 5 == 0) {
                double zEnd = Math.sin(38 * Math.PI / 20) * 6;
                w.spawnParticle(Particle.DRIPPING_LAVA, center.clone().add(18, 2, zEnd), 2, 0.3, 0, 0.3, 0);
            }

            // Flame from fountain
            if (ticksAlive % 4 == 0) {
                w.spawnParticle(Particle.FLAME, center.clone().add(-20, 7, 0), 3, 0.3, 0.5, 0.3, 0.02);
            }

            // Sound
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheBurningRiverOfVoid(plugin); }
    }

    // ================================================================
    // 96. THE CALAMITY COLUMN — 45-block cosmic-scale vertical column
    //     with material bands and rotating blaze rod rings
    // ================================================================
    public static class TheCalamityColumn extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> columnBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> blazeRings = new ArrayList<>();

        public TheCalamityColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamity_column", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(10.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(1400);
            config.setCooldownTicks(600);
            config.setImpactDamage(12.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 45-block column, 5x5 profile, material bands
            Material[] bandMats = {Material.BLACKSTONE, Material.BASALT, Material.NETHERRACK,
                    Material.MAGMA_BLOCK, Material.CRACKED_STONE_BRICKS};
            for (int y = 0; y < 45; y++) {
                Material mat = bandMats[Math.min(y / 10, 4)];
                // Serpentine offset: alternating 0.1 blocks
                float xOff = (y % 10 < 5 ? 0.1f : -0.1f);
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(xOff, y, 0), mat);
                h.scale(2.0f, 1.0f, 2.0f).glow(mat == Material.MAGMA_BLOCK ? 255 : 70,
                        mat == Material.MAGMA_BLOCK ? 100 : 70, mat == Material.MAGMA_BLOCK ? 0 : 80)
                        .interpolation(3, 0);
                columnBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 blaze rod rings at heights 10, 20, 30, 40
            for (int ringH : new int[]{10, 20, 30, 40}) {
                List<BlockDisplayHandle> ring = new ArrayList<>();
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 * i) / 12;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 2.5, ringH, Math.sin(angle) * 2.5),
                            Material.NETHERRACK);
                    h.scale(0.12f, 0.6f, 0.12f).glow(255, 100, 0).interpolation(3, 0);
                    ring.add(h);
                    spawnedEntities.add(h.entity());
                }
                blazeRings.add(ring);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Column slow sink: 0.005 blocks/tick
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : columnBlocks) {
                    Location loc = h.entity().getLocation();
                    loc.add(0, -0.05, 0);
                    h.entity().teleport(loc);
                }
            }

            // Column Y-rotation: 0.1°/tick
            float colRot = ticksAlive * 0.00175f;

            // Ring rotations at different speeds
            float[] ringSpeeds = {0.01745f, -0.02618f, 0.0349f, -0.04363f};
            for (int r = 0; r < blazeRings.size(); r++) {
                float rRot = ticksAlive * ringSpeeds[r];
                for (int i = 0; i < blazeRings.get(r).size(); i++) {
                    double angle = (Math.PI * 2 * i) / 12 + rRot;
                    int ringH = new int[]{10, 20, 30, 40}[r];
                    Location loc = center.clone().add(Math.cos(angle) * 2.5, ringH, Math.sin(angle) * 2.5);
                    blazeRings.get(r).get(i).entity().teleport(loc);
                }
            }

            // Band transition pulse: boundary rows scale
            for (int boundary : new int[]{10, 20, 30, 40}) {
                if (boundary < columnBlocks.size()) {
                    float bScale = 2.0f + 0.16f * (float) Math.sin(ticksAlive * Math.PI / 20);
                    columnBlocks.get(boundary).scale(bScale, 1.0f, bScale);
                    columnBlocks.get(boundary).interpolation(3, 0);
                }
            }

            // Synchronized ring FLAME burst every 40 ticks
            if (ticksAlive % 40 == 0 && ticksAlive > 0) {
                for (List<BlockDisplayHandle> ring : blazeRings) {
                    for (BlockDisplayHandle h : ring) {
                        w.spawnParticle(Particle.FLAME, h.entity().getLocation(), 5, 0.5, 0.3, 0.5, 0.05);
                    }
                }
                triggerImpactDamage(center);
            }

            // Flame from magma band (blocks 30-40)
            if (ticksAlive % 3 == 0) {
                int fIdx = 30 + (ticksAlive / 3) % 10;
                if (fIdx < columnBlocks.size()) {
                    Location fLoc = columnBlocks.get(fIdx).entity().getLocation();
                    w.spawnParticle(Particle.FLAME, fLoc, 3, 1.5, 0.2, 1.5, 0.03);
                }
            }

            // Lava dripping down from ring 4
            if (ticksAlive % 4 == 0) {
                if (!blazeRings.isEmpty() && blazeRings.size() > 3) {
                    int idx = (ticksAlive / 4) % blazeRings.get(3).size();
                    w.spawnParticle(Particle.LAVA, blazeRings.get(3).get(idx).entity().getLocation(),
                            1, 0.1, -0.5, 0.1, 0);
                }
            }

            // Soul fire from below column
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1, 0), 3,
                        0.5, 1.5, 0.5, 0.02);
            }

            // Smoke from apex
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 44, 0), 3, 0.5, 0.5, 0.5, 0.02);
            }

            // Sound
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheCalamityColumn(plugin); }
    }

    // ================================================================
    // 97. THE MARCHING DEAD — 8 humanoid Dweller-echo silhouettes
    //     closing inward from arena perimeter with eye beams
    // ================================================================
    public static class TheMarchingDead extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> echoes = new ArrayList<>();
        private final List<BlockDisplayHandle> echoEyes = new ArrayList<>();
        private float echoRadius = 24.0f;

        public TheMarchingDead(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_marching_dead", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(1100);
            config.setCooldownTicks(500);
            config.setImpactDamage(14.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int e = 0; e < 8; e++) {
                List<BlockDisplayHandle> echo = new ArrayList<>();
                double angle = (Math.PI * 2 * e) / 8;
                Location echoBase = center.clone().add(Math.cos(angle) * 24, 0, Math.sin(angle) * 24);

                // Simplified 9-block humanoid silhouette
                // Legs: 2 basalt columns of 3 blocks
                for (int side = -1; side <= 1; side += 2) {
                    for (int y = 0; y < 3; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                echoBase.clone().add(side * 0.4, y, 0), Material.BASALT);
                        h.scale(0.5f, 1.0f, 0.5f).glow(70, 70, 80).interpolation(3, 0);
                        echo.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // Torso: 2x2 black concrete
                for (int tx = 0; tx <= 1; tx++) {
                    for (int ty = 3; ty <= 4; ty++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                echoBase.clone().add(tx - 0.5, ty, 0), Material.BLACK_CONCRETE);
                        h.scale(0.7f, 1.0f, 0.5f).glow(20, 20, 25).interpolation(3, 0);
                        echo.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // Arms: 2 netherrack columns of 3 blocks
                for (int side = -1; side <= 1; side += 2) {
                    for (int y = 3; y < 6; y++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                echoBase.clone().add(side * 1.0, y, 0), Material.NETHERRACK);
                        h.scale(0.3f, 1.0f, 0.3f).glow(120, 60, 40).interpolation(3, 0);
                        echo.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // Head: 2 blackstone blocks
                for (int y = 5; y < 7; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            echoBase.clone().add(0, y, 0), Material.BLACKSTONE);
                    h.scale(0.6f, 1.0f, 0.5f).glow(30, 30, 40).interpolation(3, 0);
                    echo.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Eyes: 2 magma blocks with cyan glow
                for (int side = -1; side <= 1; side += 2) {
                    BlockDisplayHandle eye = displayBuilder.spawnBlock(
                            echoBase.clone().add(side * 0.2, 5.8, -0.3), Material.MAGMA_BLOCK);
                    eye.scale(0.2f, 0.2f, 0.15f).glow(0, 150, 255).interpolation(3, 0);
                    echoEyes.add(eye);
                    echo.add(eye);
                    spawnedEntities.add(eye.entity());
                }

                // Shadow
                BlockDisplayHandle shadow = displayBuilder.spawnBlock(echoBase.clone().add(0, -0.5, 0), Material.SOUL_SOIL);
                shadow.scale(0.8f, 0.2f, 0.8f).glow(0, 150, 255).interpolation(2, 0);
                echo.add(shadow);
                spawnedEntities.add(shadow.entity());

                echoes.add(echo);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Translate all echoes inward at 0.04 blocks/tick
            echoRadius -= 0.04f;
            if (echoRadius < 8.0f) echoRadius = 8.0f;

            for (int e = 0; e < echoes.size(); e++) {
                double angle = (Math.PI * 2 * e) / 8;
                List<BlockDisplayHandle> echo = echoes.get(e);

                // Walking gate animation: stagger left/right by 0.2 blocks
                float walkOffset = 0.2f * (float) Math.sin(ticksAlive * Math.PI / 9);

                // Move all blocks (simplified: teleport base, rest via transform)
                for (BlockDisplayHandle h : echo) {
                    Location loc = h.entity().getLocation();
                    double dx = center.getX() + Math.cos(angle) * echoRadius - loc.getX();
                    double dz = center.getZ() + Math.sin(angle) * echoRadius - loc.getZ();
                    loc.add(dx * 0.05, 0, dz * 0.05);
                    h.entity().teleport(loc);
                }
            }

            // Eye pulse: 30-tick cycle, scale 0.2 to 0.35
            float eyeScale = 0.2f + 0.15f * (float) Math.sin(ticksAlive * Math.PI / 15);
            for (BlockDisplayHandle eye : echoEyes) {
                eye.scale(eyeScale, eyeScale, 0.15f);
                eye.interpolation(3, 0);
            }

            // At 8-block radius: fire beams and stop
            if (echoRadius <= 8.5f && echoRadius > 8.0f) {
                triggerImpactDamage(center);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 50, 4, 2, 4, 0.1);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
            }

            // Soul fire flame beams from eyes
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle eye : echoEyes) {
                    Location eLoc = eye.entity().getLocation();
                    double dx = center.getX() - eLoc.getX();
                    double dz = center.getZ() - eLoc.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, eLoc.clone().add(dx / dist, 0, dz / dist),
                                2, dx / dist * 0.3, 0.05, dz / dist * 0.3, 0.03);
                    }
                }
            }

            // Ash trails
            if (ticksAlive % 5 == 0) {
                for (List<BlockDisplayHandle> echo : echoes) {
                    if (!echo.isEmpty()) {
                        w.spawnParticle(Particle.ASH, echo.get(0).entity().getLocation(), 2, 0.2, 0.3, 0.2, 0);
                    }
                }
            }

            // Crimson spore from bodies
            if (ticksAlive % 8 == 0) {
                for (List<BlockDisplayHandle> echo : echoes) {
                    if (echo.size() > 6) {
                        w.spawnParticle(Particle.CRIMSON_SPORE, echo.get(6).entity().getLocation(),
                                1, 0.2, 0.3, 0.2, 0);
                    }
                }
            }

            // Footstep sound
            if (ticksAlive % 18 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheMarchingDead(plugin); }
    }

    // ================================================================
    // 98. BRIMSTONE SKY COLLAPSE — 50 meteors raining from Y+40
    //     in staggered deployment across the arena
    // ================================================================
    public static class BrimstoneSkyCollapse extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> meteors = new ArrayList<>();
        private final List<Location> targets = new ArrayList<>();
        private final List<Integer> startTicks = new ArrayList<>();

        public BrimstoneSkyCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_sky_collapse", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(600);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Material[] meteorMats = {Material.NETHERRACK, Material.MAGMA_BLOCK, Material.BASALT, Material.BLACKSTONE};

            for (int m = 0; m < 50; m++) {
                List<BlockDisplayHandle> meteor = new ArrayList<>();

                // Random target position within 25-block radius
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 25;
                Location target = center.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
                targets.add(target);

                // Start tick: staggered, 1 every 3 ticks
                startTicks.add(m * 3);

                // Meteor cluster: 3-7 blocks based on size
                int size = m < 20 ? 3 : (m < 40 ? 5 : 7);
                for (int b = 0; b < size; b++) {
                    Material mat = meteorMats[b % 4];
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            target.clone().add((Math.random() - 0.5) * 1.5, 40 + Math.random() * 2, (Math.random() - 0.5) * 1.5),
                            mat);
                    h.scale(0.8f, 0.8f, 0.8f).glow(mat == Material.MAGMA_BLOCK ? 255 : 100,
                            mat == Material.MAGMA_BLOCK ? 100 : 50, mat == Material.MAGMA_BLOCK ? 0 : 30)
                            .interpolation(3, 0);
                    meteor.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Tail blocks
                for (int t = 0; t < 3; t++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            target.clone().add(0, 42 + t * 1.5, 0), Material.NETHERRACK);
                    h.scale(0.4f, 0.4f, 0.4f).glow(120, 60, 40).interpolation(2, 0);
                    meteor.add(h);
                    spawnedEntities.add(h.entity());
                }

                meteors.add(meteor);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int m = 0; m < meteors.size(); m++) {
                if (ticksAlive < startTicks.get(m)) continue;

                int age = ticksAlive - startTicks.get(m);
                List<BlockDisplayHandle> meteor = meteors.get(m);
                if (meteor.isEmpty()) continue;

                // Fall speed based on size
                int size = m < 20 ? 3 : (m < 40 ? 5 : 7);
                float speed = size == 3 ? 0.12f : (size == 5 ? 0.09f : 0.06f);

                // Check if landed
                Location firstLoc = meteor.get(0).entity().getLocation();
                if (firstLoc.getY() <= targets.get(m).getY() + 1) {
                    // Impact
                    if (age > 1) {
                        triggerImpactDamage(targets.get(m));
                        w.spawnParticle(Particle.LAVA, targets.get(m), 15, 1.5, 0.5, 1.5, 0.1);
                        w.spawnParticle(Particle.FLAME, targets.get(m), 10, 1, 0.3, 1, 0.05);
                        w.spawnParticle(Particle.SMOKE, targets.get(m), 8, 1, 0.5, 1, 0.03);
                        if (size >= 7) {
                            DisplayBuilder.playSound(targets.get(m), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
                        } else {
                            DisplayBuilder.playSound(targets.get(m), Sound.BLOCK_STONE_PLACE, 0.6f, 0.8f);
                        }
                    }
                    continue;
                }

                // Fall
                float spinRate = size == 3 ? 0.04363f : (size == 5 ? 0.02618f : 0.01396f);
                for (BlockDisplayHandle h : meteor) {
                    Location loc = h.entity().getLocation();
                    loc.add(0, -speed, 0);
                    h.entity().teleport(loc);
                    h.rotate(age * spinRate, 0.5f, 0.3f, 0.2f);
                    h.interpolation(3, 0);
                }

                // Trail particles
                if (age % 3 == 0) {
                    w.spawnParticle(Particle.LAVA, firstLoc, 2, 0.3, 0.3, 0.3, 0);
                    w.spawnParticle(Particle.FLAME, firstLoc, 2, 0.2, 0.5, 0.2, 0.02);
                }

                // Dripping lava wake
                if (age % 5 == 0) {
                    w.spawnParticle(Particle.DRIPPING_LAVA, firstLoc.clone().add(0, 1, 0), 1, 0.1, 0.3, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneSkyCollapse(plugin); }
    }

    // ================================================================
    // 99. THE DWELLER'S DESCENT — 40-block-tall humanoid effigy descending
    //     from above with dual eye beams and lunge-vanish finale
    // ================================================================
    public static class TheDwellersDescent extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> effigyBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private float effigyY = 60.0f;
        private boolean lunged = false;

        public TheDwellersDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dwellers_descent", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(600);
            config.setImpactDamage(14.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Simplified effigy: key structural blocks at Y+60
            // Head: 4x4 black concrete
            for (int x = -2; x < 2; x++) {
                for (int z = -2; z < 2; z++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 96, z), Material.BLACK_CONCRETE);
                    h.scale(1.0f, 1.0f, 1.0f).glow(20, 20, 25).interpolation(3, 0);
                    effigyBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Eyes
            BlockDisplayHandle eyeL = displayBuilder.spawnBlock(
                    center.clone().add(-0.8, 96, -2.2), Material.MAGMA_BLOCK);
            eyeL.scale(0.6f, 0.6f, 0.3f).glow(0, 150, 255).interpolation(3, 0);
            eyeBlocks.add(eyeL);
            spawnedEntities.add(eyeL.entity());

            BlockDisplayHandle eyeR = displayBuilder.spawnBlock(
                    center.clone().add(0.8, 96, -2.2), Material.MAGMA_BLOCK);
            eyeR.scale(0.6f, 0.6f, 0.3f).glow(0, 150, 255).interpolation(3, 0);
            eyeBlocks.add(eyeR);
            spawnedEntities.add(eyeR.entity());

            // Torso: 5x3x12 mixed blocks (simplified)
            for (int y = 80; y < 92; y++) {
                for (int x = -2; x <= 2; x++) {
                    Material mat = (x + y) % 3 == 0 ? Material.MAGMA_BLOCK : Material.BLACK_CONCRETE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, 0), mat);
                    h.scale(1.0f, 1.0f, 0.8f).glow(mat == Material.MAGMA_BLOCK ? 255 : 20,
                            mat == Material.MAGMA_BLOCK ? 100 : 20, mat == Material.MAGMA_BLOCK ? 0 : 25)
                            .interpolation(3, 0);
                    effigyBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Arms: 2x2 netherrack, 16 blocks each side
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 76; y < 92; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(side * 4, y, 0), Material.NETHERRACK);
                    h.scale(0.8f, 1.0f, 0.8f).glow(120, 60, 40).interpolation(3, 0);
                    effigyBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Legs: 2x2 basalt, 18 blocks each
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 60; y < 78; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(side * 1.5, y, 0), Material.BASALT);
                    h.scale(0.8f, 1.0f, 0.8f).glow(70, 70, 80).interpolation(3, 0);
                    effigyBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Crown: cracked stone bricks
            for (int i = -2; i <= 1; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(i, 98, 0), Material.CRACKED_STONE_BRICKS);
                h.scale(0.8f, 0.8f, 0.8f).glow(70, 70, 80).interpolation(2, 0);
                effigyBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (lunged) return;

            // Descent: 0.08 blocks/tick with Y-rotation 0.2°/tick
            effigyY -= 0.08f;
            float yDelta = -0.08f;

            for (BlockDisplayHandle h : effigyBlocks) {
                Location loc = h.entity().getLocation();
                loc.add(0, yDelta, 0);
                h.entity().teleport(loc);
            }
            for (BlockDisplayHandle h : eyeBlocks) {
                Location loc = h.entity().getLocation();
                loc.add(0, yDelta, 0);
                h.entity().teleport(loc);
            }

            // Eyes grow as effigy descends: 0.6 to 1.5
            float eyeGrowth = 0.6f + (60 - effigyY) / 50.0f * 0.9f;
            if (eyeGrowth > 1.5f) eyeGrowth = 1.5f;
            for (BlockDisplayHandle eye : eyeBlocks) {
                eye.scale(eyeGrowth, eyeGrowth, 0.3f);
                eye.interpolation(3, 0);
            }

            // Magma vein pulse on torso
            if (ticksAlive % 40 == 0) {
                w.spawnParticle(Particle.FLAME, center.clone().add(0, effigyY + 26, 0), 15, 2.5, 1, 2.5, 0.05);
            }

            // Lava rising from ground to meet effigy (gravity-defying)
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.LAVA, center.clone().add(0, Math.random() * effigyY, 0),
                        2, 0.5, 2, 0.5, 0);
            }

            // Crimson spore from arms
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.CRIMSON_SPORE, center.clone().add(4, effigyY + 16, 0), 2, 0.3, 1, 0.3, 0);
                w.spawnParticle(Particle.CRIMSON_SPORE, center.clone().add(-4, effigyY + 16, 0), 2, 0.3, 1, 0.3, 0);
            }

            // Soul fire beams from eyes down to arena
            if (ticksAlive % 2 == 0) {
                for (BlockDisplayHandle eye : eyeBlocks) {
                    Location eLoc = eye.entity().getLocation();
                    for (int d = 0; d < (int) (eLoc.getY() - center.getY()); d += 3) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, eLoc.clone().add(0, -d, 0), 1,
                                0.3, 0.1, 0.3, 0.01);
                    }
                }
            }

            // FLAME from torso veins on pulse
            if (ticksAlive % 40 < 5) {
                w.spawnParticle(Particle.FLAME, center.clone().add(0, effigyY + 26, 0), 5,
                        2.5, 1.5, 2.5, 0.05);
            }

            // Lunge-and-vanish at Y+10
            if (effigyY <= 10) {
                lunged = true;
                // All scale to 0.5x then snap to 2.0x then vanish
                for (BlockDisplayHandle h : effigyBlocks) {
                    h.scale(0.1f, 0.1f, 0.1f);
                    h.interpolation(5, 0);
                }
                for (BlockDisplayHandle h : eyeBlocks) {
                    h.scale(0.1f, 0.1f, 0.1f);
                    h.interpolation(5, 0);
                }

                triggerImpactDamage(center);
                w.spawnParticle(Particle.LAVA, center, 100, 4, 2, 4, 0.2);
                w.spawnParticle(Particle.FLAME, center, 80, 4, 2, 4, 0.15);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 60, 4, 2, 4, 0.15);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
            }

            // Sound during descent
            if (ticksAlive % 40 == 0 && !lunged) {
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheDwellersDescent(plugin); }
    }

    // ================================================================
    // 100. THE DOMAIN — The Dweller's Final Throne. MASTERPIECE STRUCTURE.
    //      7-layer arena-covering display. The last thing players see.
    // ================================================================
    public static class TheDomain extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> floorLayer = new ArrayList<>();
        private final List<BlockDisplayHandle> veinLayer = new ArrayList<>();
        private final List<BlockDisplayHandle> pillarLayer = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeCanopy = new ArrayList<>();
        private final List<BlockDisplayHandle> archLayer = new ArrayList<>();
        private final List<BlockDisplayHandle> silhouetteLayer = new ArrayList<>();
        private final List<BlockDisplayHandle> crownLayer = new ArrayList<>();
        private BlockDisplayHandle crownStar;
        private int deployPhase = 0;

        public TheDomain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_domain", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(10.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(1600);
            config.setCooldownTicks(800);
            config.setImpactDamage(10.0);
            config.setImpactRadius(25.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Layer 1: Brimstone floor mosaic (sampled for performance)
            Material[] floorMats = {Material.NETHERRACK, Material.MAGMA_BLOCK, Material.BLACKSTONE,
                    Material.CRACKED_STONE_BRICKS, Material.BASALT};
            for (int x = -25; x < 25; x += 3) {
                for (int z = -25; z < 25; z += 3) {
                    if (x * x + z * z > 625) continue; // 25-radius circle
                    Material mat = floorMats[(Math.abs(x) + Math.abs(z)) % 5];
                    float yOff = (float) (Math.random() * 0.5 - 0.2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, yOff, z), mat);
                    h.scale(3.0f, 0.5f, 3.0f).glow(mat == Material.MAGMA_BLOCK ? 255 : 80,
                            mat == Material.MAGMA_BLOCK ? 100 : 60, mat == Material.MAGMA_BLOCK ? 0 : 50)
                            .interpolation(3, 0);
                    floorLayer.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Layer 2: Lava vein network (6 arteries radiating from center)
            for (int artery = 0; artery < 6; artery++) {
                double angle = (Math.PI * 2 * artery) / 6;
                for (int seg = 2; seg < 24; seg += 3) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * seg, 0.5, Math.sin(angle) * seg),
                            seg % 2 == 0 ? Material.RED_STAINED_GLASS : Material.ORANGE_STAINED_GLASS);
                    h.scale(1.5f, 0.15f, 1.5f).glow(255, 100, 0).interpolation(3, 0);
                    veinLayer.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Layer 3: Pillar forest (24 pillars, clustered toward center)
            for (int p = 0; p < 24; p++) {
                double angle = (Math.PI * 2 * p) / 24;
                double r = 5 + (p % 4) * 5;
                int height = 8 + p % 15;
                Location pBase = center.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
                for (int y = 0; y < height; y += 2) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(pBase.clone().add(0, y, 0), Material.BASALT);
                    h.scale(1.0f, 2.0f, 1.0f).glow(70, 70, 80).interpolation(3, 0);
                    pillarLayer.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Fire charge cap on taller pillars
                if (height > 12) {
                    BlockDisplayHandle cap = displayBuilder.spawnBlock(pBase.clone().add(0, height, 0), Material.MAGMA_BLOCK);
                    float capScale = 0.6f + (height / 22.0f) * 1.0f;
                    cap.scale(capScale, capScale, capScale).glow(255, 100, 0).interpolation(3, 0);
                    pillarLayer.add(cap);
                    spawnedEntities.add(cap.entity());
                }
            }

            // Layer 4: Eye canopy at Y+18 (60 eyes, simplified to 30 for performance)
            for (int e = 0; e < 30; e++) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 25;
                // Eye: magma + 4 cracked stone brick + fire charge center
                Location eLoc = center.clone().add(Math.cos(angle) * r, 18, Math.sin(angle) * r);
                BlockDisplayHandle eyeCenter = displayBuilder.spawnBlock(eLoc, Material.MAGMA_BLOCK);
                eyeCenter.scale(0.5f, 0.3f, 0.5f).glow(255, 100, 0).interpolation(3, 0);
                eyeCanopy.add(eyeCenter);
                spawnedEntities.add(eyeCenter.entity());
            }

            // Layer 5: 8 arch ribs at 45° intervals
            for (int a = 0; a < 8; a++) {
                double angle = (Math.PI * 2 * a) / 8;
                for (int seg = 0; seg < 16; seg++) {
                    double t = (double) seg / 15;
                    double x = Math.cos(angle) * (10 - 10 * t);
                    double y = 12 + 16 * Math.sin(t * Math.PI);
                    double z = Math.sin(angle) * (10 - 10 * t);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, z), Material.SMOOTH_BASALT);
                    h.scale(0.8f, 0.8f, 0.8f).glow(70, 70, 80).interpolation(3, 0);
                    archLayer.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Layer 6: 4 silhouettes on cardinal arches
            for (int s = 0; s < 4; s++) {
                double angle = (Math.PI * 2 * s) / 4;
                // Simplified silhouette: 7 blocks at arch midpoint
                for (int y = 0; y < 7; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 5, 20 + y, Math.sin(angle) * 5),
                            y < 5 ? Material.BLACK_CONCRETE : Material.BLACKSTONE);
                    h.scale(0.6f, 1.0f, 0.4f).glow(20, 20, 25).interpolation(3, 0);
                    silhouetteLayer.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Silhouette eyes
                for (int side = -1; side <= 1; side += 2) {
                    BlockDisplayHandle eye = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 5 + side * 0.2, 25, Math.sin(angle) * 5 - 0.3),
                            Material.MAGMA_BLOCK);
                    eye.scale(0.15f, 0.15f, 0.1f).glow(0, 150, 255).interpolation(3, 0);
                    silhouetteLayer.add(eye);
                    spawnedEntities.add(eye.entity());
                }
            }

            // Layer 7: Crown at Y+35
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 5, 35, Math.sin(angle) * 5),
                        Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 0.5f, 1.0f).glow(50, 50, 60).interpolation(3, 0);
                crownLayer.add(h);
                spawnedEntities.add(h.entity());

                // Blaze rod analog at each crown block
                BlockDisplayHandle rod = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 5, 36, Math.sin(angle) * 5),
                        Material.NETHERRACK);
                rod.scale(0.12f, 1.0f, 0.12f).glow(255, 100, 0).interpolation(3, 0);
                crownLayer.add(rod);
                spawnedEntities.add(rod.entity());
            }

            // Nether star at apex: massive glowstone
            crownStar = displayBuilder.spawnBlock(center.clone().add(0, 37, 0), Material.GLOWSTONE);
            crownStar.scale(4.0f, 4.0f, 4.0f).glow(255, 255, 200).interpolation(3, 0);
            spawnedEntities.add(crownStar.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Floor breathing: 150-tick sine, 0.06 blocks
            float breathe = 0.06f * (float) Math.sin(ticksAlive * Math.PI / 75);

            // Pillar rotation: 0.3°/tick
            float pillarRot = ticksAlive * 0.00524f;
            for (BlockDisplayHandle h : pillarLayer) {
                h.rotate(pillarRot, 0, 1, 0);
                h.interpolation(4, 0);
            }

            // Eye canopy tracking (tilt toward center)
            for (BlockDisplayHandle eye : eyeCanopy) {
                eye.rotate(ticksAlive * 0.00873f, 0, 1, 0);
                eye.interpolation(4, 0);
            }

            // Arch gentle flex: 0.15 blocks inward, 200-tick cycle
            float archFlex = 0.15f * (float) Math.sin(ticksAlive * Math.PI / 100);

            // Crown rotation: 0.8°/tick
            float crownRot = ticksAlive * 0.01396f;
            for (BlockDisplayHandle h : crownLayer) {
                h.rotate(crownRot, 0, 1, 0);
                h.interpolation(4, 0);
            }

            // Nether star tumble + pulse: 4.0 to 5.3 on 80-tick sine
            float starScale = 4.0f + 1.3f * (float) Math.sin(ticksAlive * Math.PI / 40);
            if (crownStar != null) {
                crownStar.rotate(ticksAlive * 0.04363f, 0.4f, 1.0f, 0.25f);
                crownStar.scale(starScale, starScale, starScale);
                crownStar.interpolation(3, 0);
            }

            // HEARTBEAT SHOCKWAVE: every 100 ticks
            if (ticksAlive % 100 == 0 && ticksAlive > 0) {
                triggerImpactDamage(center);
                DisplayBuilder.particleRing(center, 25, Particle.LAVA, 50, null);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.8f);
            }

            // Layer particles:
            // Floor: lava from magma patches
            if (ticksAlive % 3 == 0) {
                double fx = (Math.random() - 0.5) * 50;
                double fz = (Math.random() - 0.5) * 50;
                if (fx * fx + fz * fz < 625) {
                    w.spawnParticle(Particle.LAVA, center.clone().add(fx, 0.5, fz), 1, 0.2, 0.3, 0.2, 0);
                }
            }

            // Veins: flame traveling outward
            if (ticksAlive % 2 == 0 && !veinLayer.isEmpty()) {
                int vIdx = (ticksAlive / 2) % veinLayer.size();
                w.spawnParticle(Particle.FLAME, veinLayer.get(vIdx).entity().getLocation(), 1,
                        0.3, 0.05, 0.3, 0.02);
            }

            // Pillar dripping lava
            if (ticksAlive % 5 == 0 && !pillarLayer.isEmpty()) {
                int pIdx = (ticksAlive / 5) % pillarLayer.size();
                w.spawnParticle(Particle.DRIPPING_LAVA, pillarLayer.get(pIdx).entity().getLocation().add(0, 1, 0),
                        1, 0.1, 0, 0.1, 0);
            }

            // Eye canopy: soul fire beams downward
            if (ticksAlive % 3 == 0) {
                int eIdx = (ticksAlive / 3) % eyeCanopy.size();
                Location eLoc = eyeCanopy.get(eIdx).entity().getLocation();
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, eLoc.clone().add(0, -1, 0), 2,
                        0.1, 1.0, 0.1, 0.02);
            }

            // Arch ash falling
            if (ticksAlive % 4 == 0 && !archLayer.isEmpty()) {
                int aIdx = (ticksAlive / 4) % archLayer.size();
                w.spawnParticle(Particle.ASH, archLayer.get(aIdx).entity().getLocation(), 2,
                        0.3, 0.5, 0.3, 0);
            }

            // Silhouette crimson spore
            if (ticksAlive % 6 == 0 && !silhouetteLayer.isEmpty()) {
                int sIdx = (ticksAlive / 6) % silhouetteLayer.size();
                w.spawnParticle(Particle.CRIMSON_SPORE, silhouetteLayer.get(sIdx).entity().getLocation(),
                        2, 0.2, 0.3, 0.2, 0);
            }

            // Crown: lava eruption from nether star
            if (ticksAlive % 3 == 0 && crownStar != null) {
                w.spawnParticle(Particle.LAVA, crownStar.entity().getLocation(), 3, 3, 1, 3, 0.1);
            }

            // Crown blaze rod flame
            if (ticksAlive % 2 == 0) {
                for (int i = 1; i < crownLayer.size(); i += 2) {
                    w.spawnParticle(Particle.FLAME, crownLayer.get(i).entity().getLocation().add(0, 0.5, 0),
                            2, 0.05, 0.3, 0.05, 0.02);
                }
            }

            // Floor crimson spore at ground level
            if (ticksAlive % 5 == 0) {
                double cx = (Math.random() - 0.5) * 50;
                double cz = (Math.random() - 0.5) * 50;
                if (cx * cx + cz * cz < 625) {
                    w.spawnParticle(Particle.CRIMSON_SPORE, center.clone().add(cx, 0.1, cz), 2,
                            0.3, 0.05, 0.3, 0);
                }
            }

            // FADE at T+1200: eye beams stop, veins fade, then layers progressively
            if (ticksAlive > 1200) {
                // Eye canopy dims
                for (BlockDisplayHandle eye : eyeCanopy) {
                    eye.scale(0.1f, 0.1f, 0.1f);
                    eye.interpolation(20, 0);
                }
            }
            if (ticksAlive > 1400) {
                // Vein layer dims
                for (BlockDisplayHandle v : veinLayer) {
                    v.scale(0.1f, 0.1f, 0.1f);
                    v.interpolation(20, 0);
                }
            }
            if (ticksAlive > 1500 && crownStar != null) {
                // Nether star dies: pulse to 6.0 then dim to 0
                float deathScale = 6.0f * (1.0f - (ticksAlive - 1500) / 100.0f);
                if (deathScale < 0) deathScale = 0;
                crownStar.scale(deathScale, deathScale, deathScale);
                crownStar.interpolation(5, 0);
            }

            // Continuous ambient sounds
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 1.0f);
            }
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheDomain(plugin); }
    }
}
