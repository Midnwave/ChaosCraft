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
 * Phase 1 Block Display — GROUP 4: ENCLOSURES AND CAGES
 * 10 structures that trap or surround players (#31–40).
 * Adapted from boss1-voidmaw.md with Calamity attack rules applied:
 * - NO status effects (Blindness, Slowness, Wither, etc. removed → damage only)
 * - Always spawn straight (yaw=0, pitch=0)
 * - Calamity particle palette (purple, cyan, crimson)
 * - All values configurable via AttackConfig
 */
public final class EnclosuresCages {

    private EnclosuresCages() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidCage(plugin));
        registry.register(new SpiralThornPrison(plugin));
        registry.register(new ObsidianCoffin(plugin));
        registry.register(new ClosingMawEnclosure(plugin));
        registry.register(new SarcophagusArray(plugin));
        registry.register(new TendrilBasket(plugin));
        registry.register(new VoidSerpentSpine(plugin));
        registry.register(new TentacleSweep(plugin));
        registry.register(new BranchingMawRoot(plugin));
        registry.register(new TheLash(plugin));
    }

    // ================================================================
    // 31. VOID CAGE — Obsidian cage drops from above onto a player
    // ================================================================
    public static class VoidCage extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> cornerPillars = new ArrayList<>();
        private final List<BlockDisplayHandle> horizontalBars = new ArrayList<>();
        private BlockDisplayHandle floorPlate;
        private final List<BlockDisplayHandle> innerNodes = new ArrayList<>();
        private boolean landed = false;
        private float dropY;
        private int landTick = -1;

        public VoidCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_cage", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            dropY = 8.0f;

            // 4 corner pillars: obsidian, 4 blocks tall
            double[][] corners = {{-1.5, -1.5}, {-1.5, 1.5}, {1.5, -1.5}, {1.5, 1.5}};
            for (double[] c : corners) {
                for (int y = 0; y < 4; y++) {
                    Material mat = (y == 3) ? Material.NETHERITE_BLOCK : Material.OBSIDIAN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(c[0], dropY + y, c[1]), mat);
                    float glowR = (y == 3) ? 40 : 80;
                    h.scale(0.5f, 1.0f, 0.5f).glow((int) glowR, 0, (y == 3) ? 80 : 160).interpolation(1, 0);
                    cornerPillars.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Horizontal bars at levels 1 and 3 on each face (not corners)
            double[][][] barPositions = {
                    {{0, 1, -1.5}}, {{0, 1, 1.5}}, {{-1.5, 1, 0}}, {{1.5, 1, 0}},
                    {{0, 3, -1.5}}, {{0, 3, 1.5}}, {{-1.5, 3, 0}}, {{1.5, 3, 0}}
            };
            for (double[][] barGroup : barPositions) {
                for (double[] pos : barGroup) {
                    Material mat = (pos[1] == 3) ? Material.NETHERITE_BLOCK : Material.OBSIDIAN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(pos[0], dropY + pos[1], pos[2]), mat);
                    boolean isXAxis = (Math.abs(pos[2]) > 1.0);
                    float scaleX = isXAxis ? 2.5f : 0.4f;
                    float scaleZ = isXAxis ? 0.4f : 2.5f;
                    h.scale(scaleX, 0.4f, scaleZ).glow(60, 0, 120).interpolation(1, 0);
                    horizontalBars.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Floor plate: blackstone 3x3
            floorPlate = displayBuilder.spawnBlock(center.clone().add(0, dropY - 0.5, 0), Material.BLACKSTONE);
            floorPlate.scale(3.0f, 0.5f, 3.0f).glow(40, 40, 50).interpolation(1, 0);
            spawnedEntities.add(floorPlate.entity());

            // 4 crying obsidian inner nodes at 3rd level corners (weeping inward)
            for (double[] c : corners) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(c[0] * 0.6, dropY + 2, c[1] * 0.6), Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(1, 0);
                innerNodes.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Drop from above — 0.8 blocks/tick until ground
            if (!landed) {
                dropY -= 0.8f;
                if (dropY <= 0) {
                    dropY = 0;
                    landed = true;
                    landTick = ticksAlive;

                    // Slam sound and impact burst
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.6f);
                    c.getWorld().spawnParticle(Particle.BLOCK, c, 60, 2, 0.3, 2, 0,
                            Material.OBSIDIAN.createBlockData());
                    DisplayBuilder.purpleDust(c, 30, 2.0);
                }

                // Update all block positions during fall
                repositionAll(c);

                // Trailing smoke during fall
                if (ticksAlive % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                            c.clone().add(0, dropY + 5, 0), 5, 1.5, 0.5, 1.5, 0.02);
                }
                return;
            }

            // Landing bounce: scale to 1.05 then back to 1.0
            if (landTick > 0 && ticksAlive == landTick + 1) {
                for (BlockDisplayHandle h : cornerPillars) {
                    h.scale(0.525f, 1.05f, 0.525f);
                    h.interpolation(1, 0);
                }
            }
            if (landTick > 0 && ticksAlive == landTick + 2) {
                for (BlockDisplayHandle h : cornerPillars) {
                    h.scale(0.5f, 1.0f, 0.5f);
                    h.interpolation(2, 0);
                }
            }

            // Phase 2: Bars slowly close inward over 30 ticks
            int ticksSinceLand = ticksAlive - landTick;
            if (ticksSinceLand > 0 && ticksSinceLand <= 30) {
                float closeProgress = ticksSinceLand / 30.0f;
                float inset = closeProgress * 0.3f;
                for (BlockDisplayHandle bar : horizontalBars) {
                    Transformation t = bar.entity().getTransformation();
                    Vector3f currentScale = t.getScale();
                    float newX = currentScale.x * (1.0f - inset * 0.01f);
                    float newZ = currentScale.z * (1.0f - inset * 0.01f);
                    bar.scale(newX, currentScale.y, newZ);
                    bar.interpolation(2, 0);
                }
            }

            // Ongoing: reverse portal pooling on floor
            if (ticksSinceLand > 5 && ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        c.clone().add(0, 0.3, 0), 6, 0.8, 0.1, 0.8, 0.01);
            }

            // Ongoing: crying obsidian tears inside cage
            if (ticksSinceLand > 5 && ticksAlive % 5 == 0) {
                for (BlockDisplayHandle node : innerNodes) {
                    Location nLoc = node.entity().getLocation().add(0, 0.3, 0);
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, nLoc, 2, 0.1, 0.2, 0.1, 0);
                }
            }

            // Heartbeat sound loop
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.8f);
            }
        }

        private void repositionAll(Location c) {
            double[][] corners = {{-1.5, -1.5}, {-1.5, 1.5}, {1.5, -1.5}, {1.5, 1.5}};
            int idx = 0;
            for (double[] corner : corners) {
                for (int y = 0; y < 4; y++) {
                    if (idx < cornerPillars.size()) {
                        cornerPillars.get(idx).entity().teleport(
                                c.clone().add(corner[0], dropY + y, corner[1]));
                    }
                    idx++;
                }
            }

            double[][] barPos = {
                    {0, 1, -1.5}, {0, 1, 1.5}, {-1.5, 1, 0}, {1.5, 1, 0},
                    {0, 3, -1.5}, {0, 3, 1.5}, {-1.5, 3, 0}, {1.5, 3, 0}
            };
            for (int i = 0; i < Math.min(barPos.length, horizontalBars.size()); i++) {
                horizontalBars.get(i).entity().teleport(
                        c.clone().add(barPos[i][0], dropY + barPos[i][1], barPos[i][2]));
            }

            if (floorPlate != null) {
                floorPlate.entity().teleport(c.clone().add(0, dropY - 0.5, 0));
            }

            for (int i = 0; i < innerNodes.size(); i++) {
                innerNodes.get(i).entity().teleport(
                        c.clone().add(corners[i][0] * 0.6, dropY + 2, corners[i][1] * 0.6));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidCage(plugin); }
    }

    // ================================================================
    // 32. SPIRAL THORN PRISON — 7 helical arcs forming a rotating cylinder
    // ================================================================
    public static class SpiralThornPrison extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> arcs = new ArrayList<>();
        private BlockDisplayHandle amethystCenter;

        public SpiralThornPrison(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spiral_thorn_prison", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(700);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 7 spiral arcs around cylinder perimeter
            for (int a = 0; a < 7; a++) {
                List<BlockDisplayHandle> arc = new ArrayList<>();
                double baseAngle = (2 * Math.PI * a) / 7;

                for (int seg = 0; seg < 4; seg++) {
                    double spiralAngle = baseAngle + (seg * Math.PI / 6);
                    double radius = 2.5;
                    double x = Math.cos(spiralAngle) * radius;
                    double z = Math.sin(spiralAngle) * radius;
                    double y = seg * 1.5;

                    Location loc = center.clone().add(x, y, z);
                    boolean isPolished = (seg % 2 == 1);
                    Material mat = isPolished ? Material.POLISHED_BLACKSTONE : Material.BLACKSTONE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.6f, 0.8f, 0.6f).glow(50, 50, 60).interpolation(2, 0);
                    arc.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Anchor: obsidian at base
                Location baseLoc = center.clone().add(
                        Math.cos(baseAngle) * 2.5, 0, Math.sin(baseAngle) * 2.5);
                Material anchorMat = (a % 2 == 0) ? Material.NETHERITE_BLOCK : Material.OBSIDIAN;
                BlockDisplayHandle anchor = displayBuilder.spawnBlock(baseLoc, anchorMat);
                anchor.scale(0.5f, 0.5f, 0.5f).glow(40, 0, 80).interpolation(2, 0);
                arc.add(anchor);
                spawnedEntities.add(anchor.entity());

                // Crying obsidian tip on alternating arcs
                if (a % 2 == 0 && !arc.isEmpty()) {
                    Location tipLoc = arc.get(3).entity().getLocation().clone().add(0, 0.8, 0);
                    BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, Material.CRYING_OBSIDIAN);
                    tip.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                    arc.add(tip);
                    spawnedEntities.add(tip.entity());
                }

                arcs.add(arc);
            }

            // Amethyst center at ground level
            amethystCenter = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.AMETHYST_BLOCK);
            amethystCenter.scale(0.6f, 0.6f, 0.6f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(amethystCenter.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_STEP, 1.0f, 0.6f);
            DisplayBuilder.purpleDust(center, 20, 2.5);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Orbit rotation: 4°/sec on central Y axis = 0.00349 rad/tick
            float orbitAngle = ticksAlive * 0.00349f;
            // Each arc also corkscrews on its own spine axis: 3°/sec
            float spineAngle = ticksAlive * 0.00262f;

            for (int a = 0; a < arcs.size(); a++) {
                double baseAngle = (2 * Math.PI * a) / 7 + orbitAngle;
                List<BlockDisplayHandle> arc = arcs.get(a);

                for (int seg = 0; seg < Math.min(4, arc.size()); seg++) {
                    double spiralAngle = baseAngle + (seg * Math.PI / 6) + spineAngle;
                    double radius = 2.5;
                    double x = Math.cos(spiralAngle) * radius;
                    double z = Math.sin(spiralAngle) * radius;
                    double y = seg * 1.5;
                    arc.get(seg).entity().teleport(c.clone().add(x, y, z));
                }
            }

            // Amethyst center bob: 0.3 blocks, 20-tick cycle
            float bob = (float) Math.sin(ticksAlive * 0.314) * 0.3f;
            amethystCenter.entity().teleport(c.clone().add(0, 1.0 + bob, 0));

            // Deep violet dust tracing each arc
            if (ticksAlive % 6 == 0) {
                for (List<BlockDisplayHandle> arc : arcs) {
                    for (int seg = 0; seg < Math.min(4, arc.size()); seg++) {
                        Location arcLoc = arc.get(seg).entity().getLocation();
                        DisplayBuilder.darkPurpleDust(arcLoc, 2, 0.2);
                    }
                }
            }

            // Falling tears from crying obsidian tips
            if (ticksAlive % 5 == 0) {
                for (int a = 0; a < arcs.size(); a++) {
                    List<BlockDisplayHandle> arc = arcs.get(a);
                    if (a % 2 == 0 && arc.size() > 5) {
                        Location tipLoc = arc.get(5).entity().getLocation();
                        c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, tipLoc, 2, 0.1, 0.3, 0.1, 0);
                    }
                }
            }

            // Sculk soul rising from amethyst center
            if (ticksAlive % 8 == 0) {
                Location centerLoc = amethystCenter.entity().getLocation();
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, centerLoc, 3, 0.2, 0.4, 0.2, 0.01);
            }

            // Ambient sound loop
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpiralThornPrison(plugin); }
    }

    // ================================================================
    // 33. OBSIDIAN COFFIN — Hovering box with opening/slamming lid
    // ================================================================
    public static class ObsidianCoffin extends BlockDisplayAttack {

        private BlockDisplayHandle mainBox;
        private BlockDisplayHandle lid;
        private BlockDisplayHandle interiorLining;
        private final List<BlockDisplayHandle> cornerAnchors = new ArrayList<>();
        private BlockDisplayHandle innerGlass;
        private int cycleTimer = 0;
        private boolean lidOpen = false;

        public ObsidianCoffin(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obsidian_coffin", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(2.0);
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main coffin body: obsidian rectangular box
            mainBox = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.OBSIDIAN);
            mainBox.scale(2.0f, 2.0f, 3.0f).glow(40, 0, 60).interpolation(3, 0);
            spawnedEntities.add(mainBox.entity());

            // Interior crying obsidian lining (visible through slots)
            interiorLining = displayBuilder.spawnBlock(center.clone().add(0, 0.6, 0), Material.CRYING_OBSIDIAN);
            interiorLining.scale(1.6f, 1.6f, 2.6f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(interiorLining.entity());

            // Lid: polished blackstone, slightly oversized
            lid = displayBuilder.spawnBlock(center.clone().add(0, 2.7, 0), Material.POLISHED_BLACKSTONE);
            lid.scale(2.2f, 0.5f, 3.2f).glow(50, 50, 60).interpolation(3, 0);
            spawnedEntities.add(lid.entity());

            // Ornamental blackstone edge on lid
            BlockDisplayHandle lidEdge = displayBuilder.spawnBlock(center.clone().add(0, 2.5, 0), Material.BLACKSTONE);
            lidEdge.scale(2.4f, 0.3f, 3.4f).glow(40, 40, 50).interpolation(3, 0);
            spawnedEntities.add(lidEdge.entity());

            // 4 netherite corner anchors at base
            double[][] anchors = {{-1, -1.5}, {-1, 1.5}, {1, -1.5}, {1, 1.5}};
            for (double[] a : anchors) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(a[0], 0, a[1]), Material.NETHERITE_BLOCK);
                h.scale(0.4f, 0.5f, 0.4f).glow(20, 20, 30).interpolation(3, 0);
                cornerAnchors.add(h);
                spawnedEntities.add(h.entity());
            }

            // Purple stained glass behind viewing slots
            innerGlass = displayBuilder.spawnBlock(center.clone().add(0, 1.2, -1.6), Material.PURPLE_STAINED_GLASS);
            innerGlass.scale(0.8f, 0.8f, 0.1f).glow(160, 0, 200).interpolation(3, 0);
            spawnedEntities.add(innerGlass.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow Y rotation: 1°/sec = 0.000873 rad/tick
            float rot = ticksAlive * 0.000873f;
            mainBox.rotate(rot, 0, 1, 0);
            mainBox.interpolation(5, 0);

            // Lid open/close cycle every 20 seconds (400 ticks)
            cycleTimer++;
            int cyclePos = cycleTimer % 400;

            if (cyclePos == 200) {
                // Open lid: translate up 1.5 blocks over 15 ticks
                lidOpen = true;
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_FALL, 0.8f, 0.7f);
            }
            if (cyclePos >= 200 && cyclePos < 215) {
                // Lid opening animation
                float openProgress = (cyclePos - 200) / 15.0f;
                float lidY = 2.7f + openProgress * 1.5f;
                lid.entity().teleport(c.clone().add(0, lidY, 0));
                // Corner anchors extend downward during open
                double[][] anchors = {{-1, -1.5}, {-1, 1.5}, {1, -1.5}, {1, 1.5}};
                for (int i = 0; i < cornerAnchors.size(); i++) {
                    cornerAnchors.get(i).entity().teleport(
                            c.clone().add(anchors[i][0], -openProgress * 0.3, anchors[i][1]));
                }
            }
            if (cyclePos >= 215 && cyclePos < 220) {
                // Hover phase: lid stays open
                lid.entity().teleport(c.clone().add(0, 4.2, 0));
            }
            if (cyclePos == 220) {
                // Slam shut
                lidOpen = false;
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DEATH, 0.8f, 0.4f);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(0, 2.5, 0), 15, 1, 0.3, 1, 0.02);
            }
            if (cyclePos >= 220 && cyclePos < 225) {
                // Lid slam down
                float closeProgress = (cyclePos - 220) / 5.0f;
                float lidY = 4.2f - closeProgress * 1.5f;
                lid.entity().teleport(c.clone().add(0, lidY, 0));
            }
            if (cyclePos >= 225) {
                lid.entity().teleport(c.clone().add(0, 2.7, 0));
            }

            // Particles: reverse portal leaking from viewing slots when closed
            if (!lidOpen && ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        c.clone().add(0, 1.2, -1.7), 3, 0.3, 0.3, 0, 0.01);
            }

            // Large smoke from interior when lid is open
            if (lidOpen && ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(0, 2, 0), 8, 0.8, 0.5, 1.2, 0.03);
            }

            // Ash from top continuously
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.ASH,
                        c.clone().add(0, 3.5, 0), 4, 1, 0.3, 1.5, 0);
            }

            // Ambient warden sound loop
            if (ticksAlive % 120 == 60) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_AMBIENT, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ObsidianCoffin(plugin); }
    }

    // ================================================================
    // 34. CLOSING MAW ENCLOSURE — Two half-rings that slide shut
    // ================================================================
    public static class ClosingMawEnclosure extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftHalf = new ArrayList<>();
        private final List<BlockDisplayHandle> rightHalf = new ArrayList<>();
        private float gapDistance = 12.0f;
        private int phaseTimer = 0;
        private int closeCycleCount = 0;
        private boolean closed = false;

        public ClosingMawEnclosure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("closing_maw_enclosure", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(1200);
            config.setCooldownTicks(500);
            config.setDamageOnImpactOnly(false);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left half-ring: D-shape facing right
            for (int i = 0; i < 8; i++) {
                double angle = -Math.PI / 2 + (Math.PI * i / 7);
                double x = Math.cos(angle) * 3.0;
                double z = Math.sin(angle) * 3.0;
                for (int y = 0; y < 4; y++) {
                    Material mat;
                    if (y == 0 || y == 3) mat = Material.POLISHED_BLACKSTONE;
                    else if (i == 0 || i == 7) mat = Material.CRYING_OBSIDIAN;
                    else mat = Material.NETHERITE_BLOCK;

                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x - gapDistance / 2, y, z), mat);
                    int glowG = (mat == Material.CRYING_OBSIDIAN) ? 0 : 20;
                    int glowB = (mat == Material.CRYING_OBSIDIAN) ? 255 : 40;
                    h.scale(0.8f, 1.0f, 0.8f).glow(80, glowG, glowB).interpolation(2, 0);
                    leftHalf.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Right half-ring: D-shape facing left (mirrored)
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI / 2 + (Math.PI * i / 7);
                double x = Math.cos(angle) * 3.0;
                double z = Math.sin(angle) * 3.0;
                for (int y = 0; y < 4; y++) {
                    Material mat;
                    if (y == 0 || y == 3) mat = Material.POLISHED_BLACKSTONE;
                    else if (i == 0 || i == 7) mat = Material.CRYING_OBSIDIAN;
                    else mat = Material.NETHERITE_BLOCK;

                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x + gapDistance / 2, y, z), mat);
                    int glowG = (mat == Material.CRYING_OBSIDIAN) ? 0 : 20;
                    int glowB = (mat == Material.CRYING_OBSIDIAN) ? 255 : 40;
                    h.scale(0.8f, 1.0f, 0.8f).glow(80, glowG, glowB).interpolation(2, 0);
                    rightHalf.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Inner glow amethyst nodes on each half
            for (int side = 0; side < 2; side++) {
                double xOff = (side == 0) ? -gapDistance / 2 : gapDistance / 2;
                BlockDisplayHandle glow = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 2, 0), Material.AMETHYST_BLOCK);
                glow.scale(0.5f, 0.5f, 0.5f).glow(200, 100, 255).interpolation(2, 0);
                List<BlockDisplayHandle> targetList = (side == 0) ? leftHalf : rightHalf;
                targetList.add(glow);
                spawnedEntities.add(glow.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DIG, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            phaseTimer++;

            // Approach phase: slide toward each other at 0.3 blocks/sec = 0.015 blocks/tick
            if (!closed && gapDistance > 0) {
                gapDistance -= 0.015f;
                if (gapDistance <= 0) {
                    gapDistance = 0;
                    closed = true;
                    closeCycleCount++;
                    // Snap shut effect
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.6f);
                    c.getWorld().spawnParticle(Particle.BLOCK, c, 40, 1, 2, 1, 0,
                            Material.NETHERITE_BLOCK.createBlockData());
                    DisplayBuilder.crimsonDust(c, 20, 2.0);
                }
            }

            // Closed hold phase: 200 ticks (10 seconds), then reopen
            if (closed) {
                int closedTime = phaseTimer;
                if (closedTime > 200 && closeCycleCount < 2) {
                    // Reopen
                    closed = false;
                    gapDistance = 0.015f;
                    phaseTimer = 0;
                }
            }

            // Reopen phase: slide apart
            if (!closed && gapDistance > 0 && gapDistance < 12.0f && closeCycleCount > 0) {
                gapDistance += 0.03f;
                if (gapDistance >= 12.0f) {
                    gapDistance = 12.0f;
                    phaseTimer = 0;
                }
            }

            // Reposition all blocks
            repositionHalves(c);

            // Reverse portal streaming between halves during approach
            if (!closed && ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                        c.clone().add(0, 2, 0), 8, gapDistance / 4, 1, 1, 0.02);
            }

            // Dark purple dust rising from each half
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.darkPurpleDust(c.clone().add(-gapDistance / 2, 3, 0), 4, 1.0);
                DisplayBuilder.darkPurpleDust(c.clone().add(gapDistance / 2, 3, 0), 4, 1.0);
            }

            // Crying obsidian tears from lips when closed
            if (closed && ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                        c.clone().add(0, 2, 0), 6, 0.5, 1, 0.5, 0);
            }

            // Heartbeat while closed
            if (closed && ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.7f);
            }
        }

        private void repositionHalves(Location c) {
            int idx = 0;
            for (int i = 0; i < 8; i++) {
                double angle = -Math.PI / 2 + (Math.PI * i / 7);
                double x = Math.cos(angle) * 3.0;
                double z = Math.sin(angle) * 3.0;
                for (int y = 0; y < 4; y++) {
                    if (idx < leftHalf.size() - 1) {
                        leftHalf.get(idx).entity().teleport(
                                c.clone().add(x - gapDistance / 2, y, z));
                    }
                    idx++;
                }
            }
            // Amethyst glow node
            if (leftHalf.size() > idx) {
                leftHalf.get(leftHalf.size() - 1).entity().teleport(
                        c.clone().add(-gapDistance / 2, 2, 0));
            }

            idx = 0;
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI / 2 + (Math.PI * i / 7);
                double x = Math.cos(angle) * 3.0;
                double z = Math.sin(angle) * 3.0;
                for (int y = 0; y < 4; y++) {
                    if (idx < rightHalf.size() - 1) {
                        rightHalf.get(idx).entity().teleport(
                                c.clone().add(x + gapDistance / 2, y, z));
                    }
                    idx++;
                }
            }
            if (rightHalf.size() > idx) {
                rightHalf.get(rightHalf.size() - 1).entity().teleport(
                        c.clone().add(gapDistance / 2, 2, 0));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ClosingMawEnclosure(plugin); }
    }

    // ================================================================
    // 35. THE SARCOPHAGUS ARRAY — 4 walls rise sequentially, contract,
    //     then burst outward
    // ================================================================
    public static class SarcophagusArray extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> northWall = new ArrayList<>();
        private final List<BlockDisplayHandle> eastWall = new ArrayList<>();
        private final List<BlockDisplayHandle> southWall = new ArrayList<>();
        private final List<BlockDisplayHandle> westWall = new ArrayList<>();
        private BlockDisplayHandle floorBase;
        private final List<BlockDisplayHandle> innerCrying = new ArrayList<>();
        private final int[] wallStartTicks = {0, 10, 20, 30};
        private float contraction = 0;
        private boolean burst = false;

        public SarcophagusArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sarcophagus_array", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(1100);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Floor: cobbled deepslate 3x3
            floorBase = displayBuilder.spawnBlock(center.clone().add(0, -0.5, 0), Material.COBBLED_DEEPSLATE);
            floorBase.scale(3.0f, 0.5f, 3.0f).glow(50, 50, 60).interpolation(2, 0);
            spawnedEntities.add(floorBase.entity());

            // Floor amethyst accents
            BlockDisplayHandle floorGlow1 = displayBuilder.spawnBlock(
                    center.clone().add(-1, -0.3, -1), Material.AMETHYST_BLOCK);
            floorGlow1.scale(0.4f, 0.3f, 0.4f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(floorGlow1.entity());
            BlockDisplayHandle floorGlow2 = displayBuilder.spawnBlock(
                    center.clone().add(1, -0.3, 1), Material.AMETHYST_BLOCK);
            floorGlow2.scale(0.4f, 0.3f, 0.4f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(floorGlow2.entity());

            // 4 walls — each starts underground
            createWall(center, 0, 0, -1.5, northWall, 3.0f, 0.4f);  // North: along X
            createWall(center, 1.5, 0, 0, eastWall, 0.4f, 3.0f);   // East: along Z
            createWall(center, 0, 0, 1.5, southWall, 3.0f, 0.4f);  // South: along X
            createWall(center, -1.5, 0, 0, westWall, 0.4f, 3.0f);  // West: along Z

            // Inner crying obsidian nodes (1 per wall face, mid-height)
            double[][] innerPos = {{0, 1.5, -1.2}, {1.2, 1.5, 0}, {0, 1.5, 1.2}, {-1.2, 1.5, 0}};
            for (double[] pos : innerPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], -3 + pos[1], pos[2]), Material.CRYING_OBSIDIAN);
                h.scale(0.35f, 0.35f, 0.35f).glow(128, 0, 255).interpolation(2, 0);
                innerCrying.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        private void createWall(Location center, double offX, double offY, double offZ,
                                List<BlockDisplayHandle> wallList, float scaleX, float scaleZ) {
            // Main wall: blackstone
            for (int y = 0; y < 3; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(offX, -3 + y, offZ), Material.BLACKSTONE);
                h.scale(scaleX, 1.0f, scaleZ).glow(50, 50, 60).interpolation(2, 0);
                wallList.add(h);
                spawnedEntities.add(h.entity());
            }
            // Polished coping on top
            BlockDisplayHandle coping = displayBuilder.spawnBlock(
                    center.clone().add(offX, -3 + 3, offZ), Material.POLISHED_BLACKSTONE);
            coping.scale(scaleX + 0.2f, 0.3f, scaleZ + 0.2f).glow(60, 60, 70).interpolation(2, 0);
            wallList.add(coping);
            spawnedEntities.add(coping.entity());

            // Obsidian reinforcement on outer face
            BlockDisplayHandle reinforce = displayBuilder.spawnBlock(
                    center.clone().add(offX * 1.1, -3 + 1.5, offZ * 1.1), Material.OBSIDIAN);
            reinforce.scale(scaleX * 0.8f, 1.5f, scaleZ * 0.8f).glow(40, 0, 60).interpolation(2, 0);
            wallList.add(reinforce);
            spawnedEntities.add(reinforce.entity());

            // Netherite corner anchors
            BlockDisplayHandle anchor = displayBuilder.spawnBlock(
                    center.clone().add(offX, -3, offZ), Material.NETHERITE_BLOCK);
            anchor.scale(0.5f, 0.5f, 0.5f).glow(20, 20, 30).interpolation(2, 0);
            wallList.add(anchor);
            spawnedEntities.add(anchor.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (burst) return;

            List<List<BlockDisplayHandle>> walls = List.of(northWall, eastWall, southWall, westWall);
            double[][] wallOffsets = {{0, -1.5}, {1.5, 0}, {0, 1.5}, {-1.5, 0}};
            double[][] innerOffsets = {{0, -1.2}, {1.2, 0}, {0, 1.2}, {-1.2, 0}};
            float[] scaleXArr = {3.0f, 0.4f, 3.0f, 0.4f};
            float[] scaleZArr = {0.4f, 3.0f, 0.4f, 3.0f};

            // Phase 1: Staggered wall rise (ticks 0-40)
            for (int w = 0; w < 4; w++) {
                int wallAge = ticksAlive - wallStartTicks[w];
                if (wallAge < 0) continue;

                float riseProgress = Math.min(1.0f, wallAge / 20.0f);
                float yOffset = riseProgress * 3.0f;

                List<BlockDisplayHandle> wall = walls.get(w);
                double offX = wallOffsets[w][0] - contraction * Math.signum(wallOffsets[w][0]);
                double offZ = wallOffsets[w][1] - contraction * Math.signum(wallOffsets[w][1]);

                for (int i = 0; i < Math.min(3, wall.size()); i++) {
                    wall.get(i).entity().teleport(c.clone().add(offX, -3 + i + yOffset, offZ));
                }
                if (wall.size() > 3) { // Coping
                    wall.get(3).entity().teleport(c.clone().add(offX, -3 + 3 + yOffset, offZ));
                }
                if (wall.size() > 4) { // Reinforcement
                    wall.get(4).entity().teleport(c.clone().add(offX * 1.1, -3 + 1.5 + yOffset, offZ * 1.1));
                }
                if (wall.size() > 5) { // Anchor
                    wall.get(5).entity().teleport(c.clone().add(offX, -3 + yOffset, offZ));
                }

                // Inner crying node
                if (w < innerCrying.size()) {
                    double iX = innerOffsets[w][0] - contraction * 0.6 * Math.signum(innerOffsets[w][0]);
                    double iZ = innerOffsets[w][1] - contraction * 0.6 * Math.signum(innerOffsets[w][1]);
                    innerCrying.get(w).entity().teleport(c.clone().add(iX, -3 + 1.5 + yOffset, iZ));
                }

                // Sound per wall rise
                if (wallAge == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f);
                }
                // Block particles during rise
                if (wallAge > 0 && wallAge <= 20 && wallAge % 5 == 0) {
                    c.getWorld().spawnParticle(Particle.BLOCK,
                            c.clone().add(offX, yOffset, offZ), 15, 0.8, 0.3, 0.8, 0,
                            Material.BLACKSTONE.createBlockData());
                }
            }

            // Phase 2: Contraction (ticks 40 to 340 = 15 seconds at 0.2 blocks/sec)
            if (ticksAlive > 40 && contraction < 1.0f) {
                contraction += 0.01f; // 0.2 blocks/sec over ~300 ticks

                // Reverse portal intensifying inside
                if (ticksAlive % 4 == 0) {
                    c.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                            c.clone().add(0, 1.5, 0), 6 + (int) (contraction * 15),
                            1.5 - contraction, 1.5, 1.5 - contraction, 0.02);
                }

                // Accelerating heartbeat
                int heartbeatInterval = Math.max(10, (int) (40 - contraction * 30));
                if (ticksAlive % heartbeatInterval == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.6f + contraction * 0.4f);
                }

                // Tears from inner nodes
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle node : innerCrying) {
                        Location nLoc = node.entity().getLocation();
                        c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, nLoc, 2, 0.1, 0.2, 0.1, 0);
                    }
                }
            }

            // Phase 3: Burst explosion when fully contracted
            if (contraction >= 1.0f && !burst) {
                burst = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.7f);
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 1.5, 0), 200,
                        3, 2, 3, 0.5, Material.BLACKSTONE.createBlockData());
                DisplayBuilder.crimsonDust(c, 30, 4.0);
                DisplayBuilder.purpleDust(c, 20, 3.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SarcophagusArray(plugin); }
    }

    // ================================================================
    // 36. TENDRIL BASKET — 8 tendrils that open and close like a dome
    // ================================================================
    public static class TendrilBasket extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> tendrils = new ArrayList<>();
        private BlockDisplayHandle amethystBase;
        private float closeProgress = 0;
        private boolean closing = true;

        public TendrilBasket(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tendril_basket", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(1600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 tendrils radiating from center
            for (int t = 0; t < 8; t++) {
                List<BlockDisplayHandle> tendril = new ArrayList<>();
                double baseAngle = (2 * Math.PI * t) / 8;

                // Base anchor: blackstone
                Location baseLoc = center.clone().add(
                        Math.cos(baseAngle) * 0.5, 0, Math.sin(baseAngle) * 0.5);
                Material anchorMat = (t % 4 == 0) ? Material.NETHERITE_BLOCK : Material.BLACKSTONE;
                BlockDisplayHandle anchor = displayBuilder.spawnBlock(baseLoc, anchorMat);
                anchor.scale(0.4f, 0.4f, 0.4f).glow(40, 40, 50).interpolation(2, 0);
                tendril.add(anchor);
                spawnedEntities.add(anchor.entity());

                // 3 obsidian tendril segments curving outward and up
                for (int seg = 1; seg <= 3; seg++) {
                    double radius = seg * 0.7;
                    double height = seg * 0.7;
                    Location segLoc = center.clone().add(
                            Math.cos(baseAngle) * radius, height, Math.sin(baseAngle) * radius);
                    Material mat = Material.OBSIDIAN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, mat);
                    boolean hasPolished = (t % 2 == 0 && seg == 2);
                    if (hasPolished) {
                        h.scale(0.5f, 0.6f, 0.5f).glow(60, 60, 70);
                    } else {
                        h.scale(0.5f, 0.6f, 0.5f).glow(80, 0, 160);
                    }
                    h.interpolation(2, 0);
                    tendril.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Tip: crying obsidian or polished blackstone
                double tipRadius = 2.1;
                double tipHeight = 2.1;
                Location tipLoc = center.clone().add(
                        Math.cos(baseAngle) * tipRadius, tipHeight, Math.sin(baseAngle) * tipRadius);
                Material tipMat = (t < 6) ? Material.CRYING_OBSIDIAN : Material.POLISHED_BLACKSTONE;
                BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, tipMat);
                tip.scale(0.35f, 0.35f, 0.35f).glow(128, 0, 255).interpolation(2, 0);
                tendril.add(tip);
                spawnedEntities.add(tip.entity());

                tendrils.add(tendril);
            }

            // Amethyst base at center
            amethystBase = displayBuilder.spawnBlock(center.clone().add(0, 0.1, 0), Material.AMETHYST_BLOCK);
            amethystBase.scale(0.8f, 0.3f, 0.8f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(amethystBase.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_LISTENING, 0.6f, 0.8f);
            DisplayBuilder.purpleDust(center, 15, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Close/open cycle: close over 30 ticks, hold 20, open over 50 ticks
            if (closing) {
                closeProgress += 1.0f / 30.0f;
                if (closeProgress >= 1.0f) {
                    closeProgress = 1.0f;
                    closing = false;
                }
            } else {
                // Hold at close for 20 ticks, then open
                if (closeProgress >= 1.0f) {
                    closeProgress -= 1.0f / 20.0f; // brief hold then start opening
                }
                if (closeProgress < 1.0f) {
                    closeProgress -= 1.0f / 50.0f;
                    if (closeProgress <= 0) {
                        closeProgress = 0;
                        closing = true;
                    }
                }
            }

            // Reposition tendrils based on closeProgress
            for (int t = 0; t < 8; t++) {
                double baseAngle = (2 * Math.PI * t) / 8;
                int stagger = (t % 3); // ±2 tick variation for organic feel
                float adjustedProgress = Math.max(0, Math.min(1,
                        closeProgress + (float) Math.sin(ticksAlive * 0.1 + stagger) * 0.03f));
                List<BlockDisplayHandle> tendril = tendrils.get(t);

                for (int seg = 1; seg <= 3; seg++) {
                    if (seg >= tendril.size()) break;
                    // Open: segments curve outward; Closed: segments curve inward and up
                    double openRadius = seg * 0.7;
                    double closedRadius = seg * 0.15;
                    double openHeight = seg * 0.7;
                    double closedHeight = seg * 1.0;

                    double radius = openRadius + (closedRadius - openRadius) * adjustedProgress;
                    double height = openHeight + (closedHeight - openHeight) * adjustedProgress;

                    Location segLoc = c.clone().add(
                            Math.cos(baseAngle) * radius, height, Math.sin(baseAngle) * radius);
                    tendril.get(seg).entity().teleport(segLoc);
                }

                // Tip follows closure
                if (tendril.size() > 4) {
                    double openTipR = 2.1;
                    double closedTipR = 0.1;
                    double openTipH = 2.1;
                    double closedTipH = 3.0;
                    double tipR = openTipR + (closedTipR - openTipR) * adjustedProgress;
                    double tipH = openTipH + (closedTipH - openTipH) * adjustedProgress;
                    Location tipLoc = c.clone().add(
                            Math.cos(baseAngle) * tipR, tipH, Math.sin(baseAngle) * tipR);
                    tendril.get(4).entity().teleport(tipLoc);
                }
            }

            // Amethyst base slow rotation when open
            if (closeProgress < 0.3f && amethystBase != null) {
                float baseRot = ticksAlive * 0.02f;
                amethystBase.rotate(baseRot, 0, 1, 0);
                amethystBase.interpolation(3, 0);
            }

            // Falling tears from 6 crying obsidian tips
            if (ticksAlive % 4 == 0) {
                for (int t = 0; t < Math.min(6, tendrils.size()); t++) {
                    List<BlockDisplayHandle> tendril = tendrils.get(t);
                    if (tendril.size() > 4) {
                        Location tipLoc = tendril.get(4).entity().getLocation();
                        c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, tipLoc, 2, 0.1, 0.2, 0.1, 0);
                    }
                }
            }

            // Sculk soul from center
            if (ticksAlive % 8 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL,
                        c.clone().add(0, 0.5, 0), 3, 0.2, 0.5, 0.2, 0.01);
            }

            // Dark dust sweeping inward during close
            if (closing && ticksAlive % 5 == 0) {
                DisplayBuilder.darkPurpleDust(c.clone().add(0, 1.5, 0), 6, 1.5 - closeProgress);
            }

            // Sound: sculk spread per tendril during close
            if (closing && closeProgress > 0.1f && ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.8f);
            }

            // Ambient cluster sound
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TendrilBasket(plugin); }
    }

    // ================================================================
    // 37. VOID SERPENT SPINE — Undulating S-curve spine with ribs
    // ================================================================
    public static class VoidSerpentSpine extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spineBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> ribBlocks = new ArrayList<>();
        private BlockDisplayHandle eyeCluster;

        public VoidSerpentSpine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_serpent_spine", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(2000);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16-block-long horizontal S-curve spine
            for (int i = 0; i < 16; i++) {
                // S-curve: x follows sine wave
                double x = i - 8;
                double z = Math.sin(i * Math.PI / 8) * 3.0;
                boolean isNode = (i % 3 == 0);
                float scaleX = isNode ? 1.2f : 1.0f;

                Location loc = center.clone().add(x, 1, z);
                Material mat;
                if (i == 0 || i == 15 || i == 7 || i == 8) mat = Material.NETHERITE_BLOCK;
                else if (i % 5 == 0) mat = Material.POLISHED_BLACKSTONE;
                else mat = Material.OBSIDIAN;

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(scaleX, 0.8f, 0.8f).glow(80, 0, 160).interpolation(2, 0);
                spineBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 10 lateral ribs at alternating sides of spine
            int[] ribPositions = {1, 3, 5, 8, 10, 12, 14, 2, 6, 9};
            for (int r = 0; r < 10; r++) {
                int spineIdx = ribPositions[r];
                double x = spineIdx - 8;
                double z = Math.sin(spineIdx * Math.PI / 8) * 3.0;
                double ribSide = (r % 2 == 0) ? 1.0 : -1.0;

                Location ribLoc = center.clone().add(x, 1 + 1.0, z + ribSide);
                BlockDisplayHandle rib = displayBuilder.spawnBlock(ribLoc, Material.BLACKSTONE);
                rib.scale(0.4f, 1.5f, 0.4f).glow(50, 50, 60).interpolation(2, 0);
                ribBlocks.add(rib);
                spawnedEntities.add(rib.entity());

                // Crying obsidian between rib pairs (at mid-curve positions)
                if (r < 6 && r % 2 == 0) {
                    Location cryLoc = center.clone().add(x, 1.5, z);
                    BlockDisplayHandle cry = displayBuilder.spawnBlock(cryLoc, Material.CRYING_OBSIDIAN);
                    cry.scale(0.3f, 0.3f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                    spawnedEntities.add(cry.entity());
                }
            }

            // Head eye cluster: 2 amethyst blocks at front
            eyeCluster = displayBuilder.spawnBlock(center.clone().add(8, 1.3, 0), Material.AMETHYST_BLOCK);
            eyeCluster.scale(0.5f, 0.5f, 0.8f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(eyeCluster.entity());
            BlockDisplayHandle eye2 = displayBuilder.spawnBlock(center.clone().add(8, 1.3, 0.5), Material.AMETHYST_BLOCK);
            eye2.scale(0.3f, 0.3f, 0.3f).glow(180, 80, 255).interpolation(2, 0);
            spawnedEntities.add(eye2.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.4f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Undulating wave motion: sine wave propagating head to tail
            for (int i = 0; i < spineBlocks.size(); i++) {
                double x = i - 8;
                double z = Math.sin(i * Math.PI / 8) * 3.0;
                // Wave: ±1.5 blocks vertical, propagates at 0.5 blocks/tick with 2-tick delay per block
                double wavePhase = (ticksAlive - i * 2) * 0.15;
                double yOffset = Math.sin(wavePhase) * 1.5;

                Location target = c.clone().add(x, 1 + yOffset, z);
                spineBlocks.get(i).entity().teleport(target);
            }

            // Ribs perpendicular to spine, sway with it
            int[] ribPositions = {1, 3, 5, 8, 10, 12, 14, 2, 6, 9};
            for (int r = 0; r < Math.min(10, ribBlocks.size()); r++) {
                int spineIdx = ribPositions[r];
                double x = spineIdx - 8;
                double z = Math.sin(spineIdx * Math.PI / 8) * 3.0;
                double ribSide = (r % 2 == 0) ? 1.0 : -1.0;
                double wavePhase = (ticksAlive - spineIdx * 2) * 0.15;
                double yOffset = Math.sin(wavePhase) * 1.5;

                Location ribLoc = c.clone().add(x, 1 + yOffset + 1.0, z + ribSide);
                ribBlocks.get(r).entity().teleport(ribLoc);
            }

            // Eye cluster tracks nearest player position (counter-rotates to wave)
            if (eyeCluster != null) {
                double headWave = Math.sin((ticksAlive - 14 * 2) * 0.15) * 1.5;
                eyeCluster.entity().teleport(c.clone().add(8, 1.3 + headWave, 0));
            }

            // Particles: violet dust tracing spine
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < spineBlocks.size(); i += 3) {
                    Location sLoc = spineBlocks.get(i).entity().getLocation();
                    DisplayBuilder.darkPurpleDust(sLoc, 2, 0.3);
                }
            }

            // Crying obsidian tears between ribs
            if (ticksAlive % 6 == 0) {
                for (int i = 2; i < Math.min(8, spineBlocks.size()); i += 3) {
                    Location tearLoc = spineBlocks.get(i).entity().getLocation().add(0, 0.5, 0);
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, tearLoc, 2, 0.2, 0.3, 0.2, 0);
                }
            }

            // Sculk charge pop at head every 3 seconds
            if (ticksAlive % 60 == 0 && eyeCluster != null) {
                Location headLoc = eyeCluster.entity().getLocation();
                DisplayBuilder.cyanDust(headLoc, 4, 0.4);
            }

            // Enderman stare sound when player close to head
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c.clone().add(8, 1.5, 0), Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSerpentSpine(plugin); }
    }

    // ================================================================
    // 38. TENTACLE SWEEP — Massive single tentacle sweeping 180 degrees
    // ================================================================
    public static class TentacleSweep extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bodySegments = new ArrayList<>();
        private BlockDisplayHandle tipNode;
        private float sweepAngle = (float) (-Math.PI / 2);
        private boolean sweepForward = true;

        public TentacleSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tentacle_sweep", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base pad: netherite 3x2
            BlockDisplayHandle basePad = displayBuilder.spawnBlock(center, Material.NETHERITE_BLOCK);
            basePad.scale(2.0f, 0.6f, 1.5f).glow(20, 20, 30).interpolation(2, 0);
            spawnedEntities.add(basePad.entity());

            // Tentacle body: 14 segments along initial direction
            for (int seg = 0; seg < 14; seg++) {
                double distance = seg;
                double height = Math.sin(seg * 0.15) * 2.0 + 1.0; // Gentle rise
                float taper;
                if (seg < 3) taper = 1.4f - seg * 0.1f; // Thick base
                else if (seg < 7) taper = 1.1f - (seg - 3) * 0.08f; // Mid
                else taper = 0.8f - (seg - 7) * 0.07f; // Thin tip

                Location segLoc = center.clone().add(distance, height, 0);
                Material mat;
                if (seg < 7) mat = (seg % 2 == 0) ? Material.BLACKSTONE : Material.POLISHED_BLACKSTONE;
                else mat = (seg % 3 == 0) ? Material.OBSIDIAN : Material.BLACKSTONE;

                BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, mat);
                h.scale(taper, 0.6f, taper).glow(50, 50, 60).interpolation(1, 0);
                bodySegments.add(h);
                spawnedEntities.add(h.entity());

                // Crying obsidian sucker on underside (4 positions)
                if (seg == 3 || seg == 6 || seg == 9 || seg == 12) {
                    BlockDisplayHandle sucker = displayBuilder.spawnBlock(
                            segLoc.clone().add(0, -0.4, 0), Material.CRYING_OBSIDIAN);
                    sucker.scale(0.3f, 0.2f, 0.3f).glow(128, 0, 255).interpolation(1, 0);
                    spawnedEntities.add(sucker.entity());
                }
            }

            // Amethyst sensing tip
            tipNode = displayBuilder.spawnBlock(center.clone().add(14, 2, 0), Material.AMETHYST_BLOCK);
            tipNode.scale(0.4f, 0.4f, 0.4f).glow(200, 100, 255).interpolation(1, 0);
            spawnedEntities.add(tipNode.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DIG, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sweep rotation: 20°/sec = 0.01745 rad/tick, 180° arc
            float sweepSpeed = 0.01745f;
            if (sweepForward) {
                sweepAngle += sweepSpeed;
                if (sweepAngle >= Math.PI / 2) {
                    sweepAngle = (float) (Math.PI / 2);
                    sweepForward = false;
                    // Whip-crack at direction change
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.4f);
                }
            } else {
                sweepAngle -= sweepSpeed;
                if (sweepAngle <= -Math.PI / 2) {
                    sweepAngle = (float) (-Math.PI / 2);
                    sweepForward = true;
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.4f);
                }
            }

            // Reposition segments along sweep arc
            for (int seg = 0; seg < bodySegments.size(); seg++) {
                double distance = seg;
                double height = Math.sin(seg * 0.15) * 2.0 + 1.0;

                // Tip whip oscillation: last 4 segments get extra ±15° oscillation
                float segAngle = sweepAngle;
                if (seg >= 10) {
                    float whipExtra = (float) Math.sin(ticksAlive * 0.3) * 0.26f * ((seg - 9.0f) / 5.0f);
                    segAngle += whipExtra;
                }

                double x = Math.cos(segAngle) * distance;
                double z = Math.sin(segAngle) * distance;

                Location target = c.clone().add(x, height, z);
                bodySegments.get(seg).entity().teleport(target);
            }

            // Tip node follows last segment
            if (tipNode != null) {
                double tipDist = 14;
                float tipAngle = sweepAngle + (float) Math.sin(ticksAlive * 0.3) * 0.26f;
                double tipX = Math.cos(tipAngle) * tipDist;
                double tipZ = Math.sin(tipAngle) * tipDist;
                double tipH = Math.sin(14 * 0.15) * 2.0 + 1.0;
                tipNode.entity().teleport(c.clone().add(tipX, tipH, tipZ));
            }

            // Trailing smoke behind tip in sweep direction
            if (ticksAlive % 2 == 0 && tipNode != null) {
                Location tipLoc = tipNode.entity().getLocation();
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, tipLoc, 3, 0.3, 0.3, 0.3, 0.02);
            }

            // Falling tears from sucker positions
            if (ticksAlive % 5 == 0) {
                int[] suckerSegs = {3, 6, 9, 12};
                for (int seg : suckerSegs) {
                    if (seg < bodySegments.size()) {
                        Location sLoc = bodySegments.get(seg).entity().getLocation().add(0, -0.3, 0);
                        c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, sLoc, 2, 0.1, 0.1, 0.1, 0);
                    }
                }
            }

            // Dark dust from amethyst tip in sweep direction
            if (ticksAlive % 4 == 0 && tipNode != null) {
                Location tipLoc = tipNode.entity().getLocation();
                DisplayBuilder.darkPurpleDust(tipLoc, 3, 0.5);
            }

            // Dig sound during sweep motion
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_DIG, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TentacleSweep(plugin); }
    }

    // ================================================================
    // 39. BRANCHING MAW ROOT — Growing root-tree with peristaltic pulse
    // ================================================================
    public static class BranchingMawRoot extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> trunkBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> branch1Blocks = new ArrayList<>();
        private final List<BlockDisplayHandle> branch2Blocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> subBranches = new ArrayList<>();
        private BlockDisplayHandle rootBall;
        private BlockDisplayHandle veinNode;
        private int growthTick = 0;

        public BranchingMawRoot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("branching_maw_root", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(6.0);
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(1000);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Root ball: 4 netherite blocks at origin
            rootBall = displayBuilder.spawnBlock(center, Material.NETHERITE_BLOCK);
            rootBall.scale(1.5f, 1.0f, 1.5f).glow(20, 20, 30).interpolation(3, 0);
            spawnedEntities.add(rootBall.entity());

            // Soul soil gaps near origin
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3;
                Location soulLoc = center.clone().add(Math.cos(angle) * 0.8, 0.1, Math.sin(angle) * 0.8);
                BlockDisplayHandle soul = displayBuilder.spawnBlock(soulLoc, Material.SOUL_SOIL);
                soul.scale(0.4f, 0.2f, 0.4f).glow(40, 30, 20).interpolation(3, 0);
                spawnedEntities.add(soul.entity());
            }

            // Amethyst vein nodes in trunk
            veinNode = displayBuilder.spawnBlock(center.clone().add(2, 0.3, 0), Material.AMETHYST_BLOCK);
            veinNode.scale(0.4f, 0.3f, 0.4f).glow(200, 100, 255).interpolation(3, 0);
            spawnedEntities.add(veinNode.entity());

            // Pre-spawn all trunk blocks (hidden underground, will grow upward)
            // Trunk: 6 blocks forward
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(i + 1, -2, 0);
                Material mat;
                if (i % 3 == 0) mat = Material.OBSIDIAN;
                else if (i % 3 == 1) mat = Material.COBBLED_DEEPSLATE;
                else mat = Material.BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float thick = 1.0f - i * 0.08f;
                h.scale(thick, 0.6f, thick).glow(50, 50, 60).interpolation(3, 0);
                trunkBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Branch 1: splits at block 4, goes +40° for 4 blocks
            double branch1Angle = Math.toRadians(40);
            for (int i = 0; i < 4; i++) {
                double x = 5 + Math.cos(branch1Angle) * (i + 1);
                double z = Math.sin(branch1Angle) * (i + 1);
                Location loc = center.clone().add(x, -2, z);
                Material mat = (i % 2 == 0) ? Material.BLACKSTONE : Material.OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f - i * 0.08f, 0.5f, 0.7f - i * 0.08f).glow(50, 50, 60).interpolation(3, 0);
                branch1Blocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Branch 2: splits at block 4, goes -40° for 4 blocks
            double branch2Angle = Math.toRadians(-40);
            for (int i = 0; i < 4; i++) {
                double x = 5 + Math.cos(branch2Angle) * (i + 1);
                double z = Math.sin(branch2Angle) * (i + 1);
                Location loc = center.clone().add(x, -2, z);
                Material mat = (i % 2 == 0) ? Material.BLACKSTONE : Material.COBBLED_DEEPSLATE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f - i * 0.08f, 0.5f, 0.7f - i * 0.08f).glow(50, 50, 60).interpolation(3, 0);
                branch2Blocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Sub-branches: 2 per main branch (split at block 3 of each branch)
            double[] subAngles = {70, 10, -10, -70};
            for (int sb = 0; sb < 4; sb++) {
                List<BlockDisplayHandle> sub = new ArrayList<>();
                double parentAngle = (sb < 2) ? branch1Angle : branch2Angle;
                double subAngle = Math.toRadians(subAngles[sb]);
                double parentX = 5 + Math.cos(parentAngle) * 3;
                double parentZ = Math.sin(parentAngle) * 3;

                for (int i = 0; i < 2; i++) {
                    double x = parentX + Math.cos(subAngle) * (i + 1);
                    double z = parentZ + Math.sin(subAngle) * (i + 1);
                    Location loc = center.clone().add(x, -2, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.4f, 0.4f, 0.4f).glow(50, 50, 60).interpolation(3, 0);
                    sub.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Crying obsidian tip
                double tipX = parentX + Math.cos(subAngle) * 3;
                double tipZ = parentZ + Math.sin(subAngle) * 3;
                BlockDisplayHandle tip = displayBuilder.spawnBlock(
                        center.clone().add(tipX, -2, tipZ), Material.CRYING_OBSIDIAN);
                tip.scale(0.35f, 0.35f, 0.35f).glow(128, 0, 255).interpolation(3, 0);
                sub.add(tip);
                spawnedEntities.add(tip.entity());

                subBranches.add(sub);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            growthTick = ticksAlive;

            // Growth phase: trunk (0-10), branches (10-20), sub-branches (20-30), tips (30-40)
            float trunkProgress = Math.min(1.0f, ticksAlive / 10.0f);
            float branchProgress = Math.max(0, Math.min(1.0f, (ticksAlive - 10) / 10.0f));
            float subProgress = Math.max(0, Math.min(1.0f, (ticksAlive - 20) / 10.0f));
            float tipProgress = Math.max(0, Math.min(1.0f, (ticksAlive - 30) / 10.0f));

            // Peristaltic wave after full growth (tick > 40)
            boolean fullyGrown = ticksAlive > 40;

            // Trunk: rise to surface
            for (int i = 0; i < trunkBlocks.size(); i++) {
                float blockProgress = Math.min(1.0f, trunkProgress * trunkBlocks.size() / (i + 1));
                float yTarget = blockProgress * 2.0f;

                // Peristaltic pulse
                float pulse = 0;
                if (fullyGrown) {
                    float wavePos = (ticksAlive - 40 - i * 3) * 0.05f;
                    pulse = (float) Math.sin(wavePos) * 0.2f;
                }

                Location target = c.clone().add(i + 1, -2 + yTarget + pulse, 0);
                trunkBlocks.get(i).entity().teleport(target);

                // Block puff during growth
                if (ticksAlive <= 10 && ticksAlive == i + 1) {
                    c.getWorld().spawnParticle(Particle.BLOCK, target, 8, 0.2, 0.2, 0.2, 0,
                            Material.BLACKSTONE.createBlockData());
                }
            }

            // Branch 1: rise to surface
            double branch1Angle = Math.toRadians(40);
            for (int i = 0; i < branch1Blocks.size(); i++) {
                float blockProgress = Math.min(1.0f, branchProgress * branch1Blocks.size() / (i + 1));
                float yTarget = blockProgress * 2.0f;
                float pulse = 0;
                if (fullyGrown) {
                    float wavePos = (ticksAlive - 40 - (6 + i) * 3) * 0.05f;
                    pulse = (float) Math.sin(wavePos) * 0.2f;
                }
                double x = 5 + Math.cos(branch1Angle) * (i + 1);
                double z = Math.sin(branch1Angle) * (i + 1);
                branch1Blocks.get(i).entity().teleport(c.clone().add(x, -2 + yTarget + pulse, z));
            }

            // Branch 2: rise to surface
            double branch2Angle = Math.toRadians(-40);
            for (int i = 0; i < branch2Blocks.size(); i++) {
                float blockProgress = Math.min(1.0f, branchProgress * branch2Blocks.size() / (i + 1));
                float yTarget = blockProgress * 2.0f;
                float pulse = 0;
                if (fullyGrown) {
                    float wavePos = (ticksAlive - 40 - (6 + i) * 3) * 0.05f;
                    pulse = (float) Math.sin(wavePos) * 0.2f;
                }
                double x = 5 + Math.cos(branch2Angle) * (i + 1);
                double z = Math.sin(branch2Angle) * (i + 1);
                branch2Blocks.get(i).entity().teleport(c.clone().add(x, -2 + yTarget + pulse, z));
            }

            // Sub-branches and tips
            double[] subAngles = {70, 10, -10, -70};
            for (int sb = 0; sb < subBranches.size(); sb++) {
                List<BlockDisplayHandle> sub = subBranches.get(sb);
                double parentAngle = (sb < 2) ? branch1Angle : branch2Angle;
                double subAngle = Math.toRadians(subAngles[sb]);
                double parentX = 5 + Math.cos(parentAngle) * 3;
                double parentZ = Math.sin(parentAngle) * 3;

                for (int i = 0; i < sub.size(); i++) {
                    float progress = (i < 2) ? subProgress : tipProgress;
                    float blockProgress = Math.min(1.0f, progress);
                    float yTarget = blockProgress * 2.0f;

                    float pulse = 0;
                    if (fullyGrown) {
                        float wavePos = (ticksAlive - 40 - (10 + sb * 2 + i) * 3) * 0.05f;
                        pulse = (float) Math.sin(wavePos) * 0.2f;
                    }

                    double x, z;
                    if (i < 2) {
                        x = parentX + Math.cos(subAngle) * (i + 1);
                        z = parentZ + Math.sin(subAngle) * (i + 1);
                    } else {
                        x = parentX + Math.cos(subAngle) * 3;
                        z = parentZ + Math.sin(subAngle) * 3;
                    }

                    // Tip sway
                    if (i == 2 && fullyGrown) {
                        float sway = (float) Math.sin(ticksAlive * 0.07 + sb) * 0.3f;
                        x += sway * Math.cos(subAngle + Math.PI / 2);
                        z += sway * Math.sin(subAngle + Math.PI / 2);
                    }

                    sub.get(i).entity().teleport(c.clone().add(x, -2 + yTarget + pulse, z));
                }
            }

            // Sculk soul from vein node
            if (ticksAlive % 8 == 0 && veinNode != null) {
                Location vLoc = veinNode.entity().getLocation();
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, vLoc, 3, 0.2, 0.3, 0.2, 0.01);
            }

            // Falling tears from tips
            if (ticksAlive % 6 == 0 && fullyGrown) {
                for (List<BlockDisplayHandle> sub : subBranches) {
                    if (sub.size() > 2) {
                        Location tipLoc = sub.get(2).entity().getLocation();
                        c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, tipLoc, 2, 0.1, 0.2, 0.1, 0);
                    }
                }
            }

            // Soul fire from soul soil at origin
            if (ticksAlive % 10 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 0.5, 0), 3, 0.5, 0.2, 0.5, 0.01);
            }

            // Heartbeat in peristaltic rhythm
            if (fullyGrown && ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.5f);
            }

            // Sculk spread sound during growth
            if (!fullyGrown && ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.7f);
            }

            // Roots step ambient loop
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ROOTS_STEP, 0.3f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BranchingMawRoot(plugin); }
    }

    // ================================================================
    // 40. THE LASH — Thin whip chain with violent crack-the-whip snap
    // ================================================================
    public static class TheLash extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> chainBlocks = new ArrayList<>();
        private final int chainLength = 14;
        private int snapTimer = 0;
        private boolean snapping = false;
        private int snapTick = 0;

        public TheLash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_lash", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(8);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14-block lash: half-size obsidian chain with S-curves
            for (int i = 0; i < chainLength; i++) {
                double x = i;
                double z = Math.sin(i * Math.PI / 4) * 0.8;
                double y = 1 + Math.sin(i * Math.PI / 7) * 1.0;

                Location loc = center.clone().add(x, y, z);
                Material mat;
                if (i == 2 || i == 5 || i == 9 || i == 12) {
                    mat = Material.CRYING_OBSIDIAN;
                } else {
                    mat = Material.OBSIDIAN;
                }

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scale = 0.5f;
                int glowR = (mat == Material.CRYING_OBSIDIAN) ? 128 : 80;
                int glowB = (mat == Material.CRYING_OBSIDIAN) ? 255 : 160;
                h.scale(scale, scale, scale).glow(glowR, 0, glowB).interpolation(1, 0);
                chainBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            snapTimer++;

            // Snap cycle: every 30 ticks (1.5 seconds)
            if (snapTimer >= 30 && !snapping) {
                snapping = true;
                snapTick = 0;
            }

            if (snapping) {
                snapTick++;

                // Crack-the-whip: wave propagates base to tip over 8 ticks
                for (int i = 0; i < chainBlocks.size(); i++) {
                    double x = i;
                    double baseZ = Math.sin(i * Math.PI / 4) * 0.8;
                    double baseY = 1 + Math.sin(i * Math.PI / 7) * 1.0;

                    // Wave displacement: perpendicular to lash direction
                    int waveDelay = i; // 1 tick per block
                    int waveAge = snapTick - waveDelay;

                    double displacement = 0;
                    if (waveAge > 0 && waveAge <= 4) {
                        // Outward swing
                        displacement = Math.sin(waveAge * Math.PI / 4) * 2.5;
                    } else if (waveAge > 4 && waveAge <= 8) {
                        // Snap back
                        displacement = Math.sin((8 - waveAge) * Math.PI / 4) * 2.5;
                    }

                    // Amplify at tip (last 4 blocks)
                    if (i >= 10) {
                        displacement *= 1.0 + (i - 10) * 0.3;
                    }

                    Location target = c.clone().add(x, baseY, baseZ + displacement);
                    chainBlocks.get(i).entity().teleport(target);
                }

                // Sonic boom when wave reaches tip
                if (snapTick == chainLength) {
                    Location tipLoc = chainBlocks.get(chainLength - 1).entity().getLocation();
                    DisplayBuilder.playSound(tipLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.6f);

                    // Visual crack trail
                    for (int i = 0; i < chainLength; i++) {
                        Location bLoc = chainBlocks.get(i).entity().getLocation();
                        c.getWorld().spawnParticle(Particle.LARGE_SMOKE, bLoc, 2, 0.1, 0.1, 0.1, 0.03);
                        DisplayBuilder.dustParticles(bLoc, 2, 0.2, 220, 220, 220, 0.5f);
                    }
                }

                if (snapTick >= chainLength + 5) {
                    snapping = false;
                    snapTimer = 0;
                }
            } else {
                // Ambient undulation: gentle sway ±0.3 blocks
                for (int i = 0; i < chainBlocks.size(); i++) {
                    double x = i;
                    double baseZ = Math.sin(i * Math.PI / 4) * 0.8;
                    double baseY = 1 + Math.sin(i * Math.PI / 7) * 1.0;

                    double ambientSway = Math.sin(ticksAlive * 0.08 + i * 0.5) * 0.3;
                    Location target = c.clone().add(x, baseY, baseZ + ambientSway);
                    chainBlocks.get(i).entity().teleport(target);
                }
            }

            // Falling tears from 4 crying obsidian nodes
            if (ticksAlive % 5 == 0) {
                int[] cryingIndices = {2, 5, 9, 12};
                for (int idx : cryingIndices) {
                    if (idx < chainBlocks.size()) {
                        Location tearLoc = chainBlocks.get(idx).entity().getLocation();
                        c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, tearLoc, 2, 0.1, 0.2, 0.1, 0);
                    }
                }
            }

            // Crimson dust during snap for visual urgency
            if (snapping && snapTick % 2 == 0) {
                for (int i = 0; i < chainBlocks.size(); i += 3) {
                    Location bLoc = chainBlocks.get(i).entity().getLocation();
                    DisplayBuilder.crimsonDust(bLoc, 2, 0.3);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLash(plugin); }
    }
}
