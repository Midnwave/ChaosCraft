package com.blockforge.chaoscraft.modes.chain.attacks;

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
 * Chain Mode Block Display — ENCLOSURES AND CAGES
 * 15 chain-themed structures that trap, enclose, and crush players.
 *
 * Rules enforced:
 * - NO status effects (damage only)
 * - Always spawn straight (yaw=0, pitch=0)
 * - Chain color palette: Iron gray RGB(180,180,190), Dark iron RGB(100,100,110), Rust RGB(180,100,40)
 * - Metallic sounds: CHAIN_PLACE/BREAK, ANVIL_LAND, IRON_GOLEM_HURT
 * - Min 10 BlockDisplays per structure, min 40 HP damage
 * - All values configurable via AttackConfig
 * - Phase = 1, full animations with interpolation/teleport/rotate
 * - Materials: CHAIN, IRON_BLOCK, IRON_BARS, NETHERITE_BLOCK, DEEPSLATE, HEAVY_CORE, ANVIL
 */
public final class EnclosuresCages {

    private EnclosuresCages() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IronMaiden(plugin));
        registry.register(new ChainCageDescend(plugin));
        registry.register(new SuspendedGibbet(plugin));
        registry.register(new ChainWeb(plugin));
        registry.register(new ClosingWalls(plugin));
        registry.register(new ChainDome(plugin));
        registry.register(new PrisonBox(plugin));
        registry.register(new ChainVortexTrap(plugin));
        registry.register(new GallowsFrame(plugin));
        registry.register(new ChainCoil(plugin));
        registry.register(new LanternCage(plugin));
        registry.register(new ChainMaze(plugin));
        registry.register(new BarrelTrap(plugin));
        registry.register(new ChainPyramid(plugin));
        registry.register(new CollapsingSphere(plugin));
    }

    // ================================================================
    // 1. IRON MAIDEN — Coffin-shaped cage with closing bars and interior spikes
    // ================================================================
    public static class IronMaiden extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftBars = new ArrayList<>();
        private final List<BlockDisplayHandle> rightBars = new ArrayList<>();
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private BlockDisplayHandle topCap;
        private BlockDisplayHandle bottomCap;
        private boolean closed = false;
        private int closeTick = -1;

        public IronMaiden(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_maiden", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 6 left bars starting offset to the left
            for (int i = 0; i < 6; i++) {
                double yOff = i * 0.6;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-2.5, yOff, 0), Material.IRON_BARS);
                h.scale(0.3f, 0.6f, 1.2f).glow(180, 180, 190).interpolation(2, 0);
                leftBars.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 right bars starting offset to the right
            for (int i = 0; i < 6; i++) {
                double yOff = i * 0.6;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(2.5, yOff, 0), Material.IRON_BARS);
                h.scale(0.3f, 0.6f, 1.2f).glow(180, 180, 190).interpolation(2, 0);
                rightBars.add(h);
                spawnedEntities.add(h.entity());
            }

            // Top cap - iron block
            topCap = displayBuilder.spawnBlock(center.clone().add(0, 3.8, 0), Material.IRON_BLOCK);
            topCap.scale(2.0f, 0.4f, 1.5f).glow(100, 100, 110).interpolation(2, 0);
            spawnedEntities.add(topCap.entity());

            // Bottom cap - deepslate
            bottomCap = displayBuilder.spawnBlock(center.clone().add(0, -0.3, 0), Material.DEEPSLATE);
            bottomCap.scale(2.0f, 0.3f, 1.5f).glow(100, 100, 110).interpolation(2, 0);
            spawnedEntities.add(bottomCap.entity());

            // Interior spike chain blocks (6 spikes thrust inward after close)
            double[][] spikePositions = {
                    {-1.0, 0.5, 0}, {-1.0, 1.5, 0}, {-1.0, 2.5, 0},
                    {1.0, 0.5, 0},  {1.0, 1.5, 0},  {1.0, 2.5, 0}
            };
            for (double[] sp : spikePositions) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(sp[0] * 2.5, sp[1], sp[2]), Material.CHAIN);
                h.scale(0.15f, 0.15f, 0.8f).glow(180, 100, 40).interpolation(2, 0);
                spikes.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.5f);
            DisplayBuilder.dustParticles(center, 25, 2.0, 180, 180, 190, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Bars slide closed over 40 ticks (2 seconds)
            if (!closed && ticksAlive <= 40) {
                float progress = ticksAlive / 40.0f;
                float leftX = -2.5f + progress * 2.0f;  // -2.5 -> -0.5
                float rightX = 2.5f - progress * 2.0f;  // 2.5 -> 0.5

                for (int i = 0; i < leftBars.size(); i++) {
                    leftBars.get(i).entity().teleport(c.clone().add(leftX, i * 0.6, 0));
                    leftBars.get(i).interpolation(2, 0);
                }
                for (int i = 0; i < rightBars.size(); i++) {
                    rightBars.get(i).entity().teleport(c.clone().add(rightX, i * 0.6, 0));
                    rightBars.get(i).interpolation(2, 0);
                }

                // Move spikes with bars
                for (int i = 0; i < spikes.size(); i++) {
                    double sx = (i < 3) ? leftX + 0.3 : rightX - 0.3;
                    double sy = (i % 3) * 1.0 + 0.5;
                    spikes.get(i).entity().teleport(c.clone().add(sx, sy, 0));
                }

                // Grinding sound during close
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.6f + progress * 0.4f);
                }
            }

            // Snap closed at tick 40
            if (!closed && ticksAlive == 40) {
                closed = true;
                closeTick = ticksAlive;
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.7f);
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 1.5, 0), 40, 0.5, 1.5, 0.5, 0,
                        Material.IRON_BLOCK.createBlockData());
                DisplayBuilder.dustParticles(c, 30, 1.5, 180, 100, 40, 1.5f);
            }

            // Phase 2: After close, spikes thrust inward over 20 ticks
            if (closed && closeTick > 0) {
                int ticksSinceClose = ticksAlive - closeTick;
                if (ticksSinceClose > 0 && ticksSinceClose <= 20) {
                    float spikeProgress = ticksSinceClose / 20.0f;
                    for (int i = 0; i < spikes.size(); i++) {
                        float inward = (i < 3) ? 0.3f + spikeProgress * 0.5f : -0.3f - spikeProgress * 0.5f;
                        double sy = (i % 3) * 1.0 + 0.5;
                        double baseX = (i < 3) ? -0.5 : 0.5;
                        spikes.get(i).entity().teleport(c.clone().add(baseX + inward, sy, 0));
                        spikes.get(i).scale(0.15f + spikeProgress * 0.2f, 0.15f, 0.8f);
                        spikes.get(i).interpolation(2, 0);
                    }
                }

                // Spike impact sound
                if (ticksSinceClose == 20) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 1.2f);
                }
            }

            // Ongoing: rust particles dripping
            if (ticksAlive % 8 == 0 && closed) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3.5, 0), 4, 0.8, 180, 100, 40, 0.8f);
                c.getWorld().spawnParticle(Particle.DRIPPING_DRIPSTONE_LAVA,
                        c.clone().add(0, 3.0, 0), 3, 0.3, 0.1, 0.3, 0);
            }

            // Ambient metallic creak
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IronMaiden(plugin); }
    }

    // ================================================================
    // 2. CHAIN CAGE DESCEND — Cubic cage descends and contracts
    // ================================================================
    public static class ChainCageDescend extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> corners = new ArrayList<>();
        private final List<BlockDisplayHandle> edges = new ArrayList<>();
        private float currentY = 12.0f;
        private float currentSize = 3.0f;
        private boolean landed = false;
        private int landTick = -1;

        public ChainCageDescend(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_cage_descend", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(240);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 corner iron blocks
            double[][] cornerOffsets = {
                    {-1, 0, -1}, {-1, 0, 1}, {1, 0, -1}, {1, 0, 1},
                    {-1, 1, -1}, {-1, 1, 1}, {1, 1, -1}, {1, 1, 1}
            };
            for (double[] off : cornerOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0] * currentSize, currentY + off[1] * currentSize * 2, off[2] * currentSize),
                        Material.IRON_BLOCK);
                h.scale(0.6f, 0.6f, 0.6f).glow(180, 180, 190).interpolation(2, 0);
                corners.add(h);
                spawnedEntities.add(h.entity());
            }

            // 12 chain edge pillars (connecting corners)
            int[][] edgePairs = {
                    {0, 1}, {0, 2}, {1, 3}, {2, 3},   // bottom face
                    {4, 5}, {4, 6}, {5, 7}, {6, 7},   // top face
                    {0, 4}, {1, 5}, {2, 6}, {3, 7}    // verticals
            };
            for (int[] pair : edgePairs) {
                double mx = (cornerOffsets[pair[0]][0] + cornerOffsets[pair[1]][0]) / 2.0;
                double my = (cornerOffsets[pair[0]][1] + cornerOffsets[pair[1]][1]) / 2.0;
                double mz = (cornerOffsets[pair[0]][2] + cornerOffsets[pair[1]][2]) / 2.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(mx * currentSize, currentY + my * currentSize * 2, mz * currentSize),
                        Material.CHAIN);
                // Determine orientation for scale
                boolean isVertical = (pair[0] < 4 && pair[1] >= 4);
                boolean isXAxis = (cornerOffsets[pair[0]][2] == cornerOffsets[pair[1]][2] &&
                        cornerOffsets[pair[0]][0] != cornerOffsets[pair[1]][0]);
                float sx = isXAxis ? 1.8f : 0.3f;
                float sy = isVertical ? 1.8f : 0.3f;
                float sz = (!isXAxis && !isVertical) ? 1.8f : 0.3f;
                h.scale(sx, sy, sz).glow(100, 100, 110).interpolation(2, 0);
                edges.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, currentY, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double[][] cornerOffsets = {
                    {-1, 0, -1}, {-1, 0, 1}, {1, 0, -1}, {1, 0, 1},
                    {-1, 1, -1}, {-1, 1, 1}, {1, 1, -1}, {1, 1, 1}
            };
            int[][] edgePairs = {
                    {0, 1}, {0, 2}, {1, 3}, {2, 3},
                    {4, 5}, {4, 6}, {5, 7}, {6, 7},
                    {0, 4}, {1, 5}, {2, 6}, {3, 7}
            };

            // Phase 1: Descend and shrink
            if (!landed) {
                currentY -= 0.3f;
                currentSize = Math.max(1.0f, currentSize - 0.02f);

                if (currentY <= 0) {
                    currentY = 0;
                    landed = true;
                    landTick = ticksAlive;
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
                    c.getWorld().spawnParticle(Particle.BLOCK, c, 50, 1.5, 0.3, 1.5, 0,
                            Material.IRON_BLOCK.createBlockData());
                }

                // Reposition all blocks
                for (int i = 0; i < corners.size(); i++) {
                    corners.get(i).entity().teleport(c.clone().add(
                            cornerOffsets[i][0] * currentSize,
                            currentY + cornerOffsets[i][1] * currentSize * 2,
                            cornerOffsets[i][2] * currentSize));
                }
                for (int e = 0; e < edges.size(); e++) {
                    int[] pair = edgePairs[e];
                    double mx = (cornerOffsets[pair[0]][0] + cornerOffsets[pair[1]][0]) / 2.0;
                    double my = (cornerOffsets[pair[0]][1] + cornerOffsets[pair[1]][1]) / 2.0;
                    double mz = (cornerOffsets[pair[0]][2] + cornerOffsets[pair[1]][2]) / 2.0;
                    edges.get(e).entity().teleport(c.clone().add(
                            mx * currentSize, currentY + my * currentSize * 2, mz * currentSize));
                }

                // Chain rattle during descent
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, currentY + 3, 0),
                            Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.8f + (float) Math.random() * 0.4f);
                }
            }

            // Phase 2: Contract to 2x2 after landing
            if (landed && landTick > 0) {
                int ticksSinceLand = ticksAlive - landTick;
                if (ticksSinceLand <= 60) {
                    float contractProgress = ticksSinceLand / 60.0f;
                    currentSize = 1.0f - contractProgress * 0.5f; // 1.0 -> 0.5

                    for (int i = 0; i < corners.size(); i++) {
                        corners.get(i).entity().teleport(c.clone().add(
                                cornerOffsets[i][0] * currentSize,
                                cornerOffsets[i][1] * currentSize * 2,
                                cornerOffsets[i][2] * currentSize));
                    }
                    for (int e = 0; e < edges.size(); e++) {
                        int[] pair = edgePairs[e];
                        double mx = (cornerOffsets[pair[0]][0] + cornerOffsets[pair[1]][0]) / 2.0;
                        double my = (cornerOffsets[pair[0]][1] + cornerOffsets[pair[1]][1]) / 2.0;
                        double mz = (cornerOffsets[pair[0]][2] + cornerOffsets[pair[1]][2]) / 2.0;
                        edges.get(e).entity().teleport(c.clone().add(
                                mx * currentSize, my * currentSize * 2, mz * currentSize));
                    }

                    // Crushing metallic groaning
                    if (ticksSinceLand % 15 == 0) {
                        DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 0.3f + contractProgress * 0.5f);
                    }
                }
            }

            // Ongoing: spark particles at corners
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < Math.min(4, corners.size()); i++) {
                    Location cLoc = corners.get(i).entity().getLocation();
                    DisplayBuilder.dustParticles(cLoc, 3, 0.3, 180, 180, 190, 0.8f);
                }
            }

            // Ambient chain clinking
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainCageDescend(plugin); }
    }

    // ================================================================
    // 3. SUSPENDED GIBBET — Hanging bird-cage that drops, lands, then rises
    // ================================================================
    public static class SuspendedGibbet extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> curvedBars = new ArrayList<>();
        private BlockDisplayHandle topRing;
        private BlockDisplayHandle bottomRing;
        private BlockDisplayHandle hookChain;
        private float cageY = 15.0f;
        private boolean landed = false;
        private boolean rising = false;
        private int landTick = -1;
        private int riseTick = -1;

        public SuspendedGibbet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("suspended_gibbet", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Top ring - iron block
            topRing = displayBuilder.spawnBlock(center.clone().add(0, cageY + 3.5, 0), Material.IRON_BLOCK);
            topRing.scale(2.0f, 0.3f, 2.0f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(topRing.entity());

            // Bottom ring - iron block
            bottomRing = displayBuilder.spawnBlock(center.clone().add(0, cageY, 0), Material.IRON_BLOCK);
            bottomRing.scale(2.5f, 0.3f, 2.5f).glow(100, 100, 110).interpolation(2, 0);
            spawnedEntities.add(bottomRing.entity());

            // Hook chain above
            hookChain = displayBuilder.spawnBlock(center.clone().add(0, cageY + 4.0, 0), Material.CHAIN);
            hookChain.scale(0.3f, 2.0f, 0.3f).glow(180, 100, 40).interpolation(2, 0);
            spawnedEntities.add(hookChain.entity());

            // 8 curved bars forming bird-cage shape
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 1.2;
                double z = Math.sin(angle) * 1.2;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, cageY + 1.75, z), Material.CHAIN);
                h.scale(0.2f, 3.0f, 0.2f).glow(180, 180, 190).interpolation(2, 0);
                // Slight outward lean at middle using rotation
                float lean = (float) (Math.PI * 0.08);
                h.rotate(lean, (float) -Math.sin(angle), 0, (float) Math.cos(angle));
                curvedBars.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, cageY, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.3f);
            DisplayBuilder.dustParticles(center.clone().add(0, cageY, 0), 15, 1.5, 100, 100, 110, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Drop from sky
            if (!landed) {
                cageY -= 0.5f;
                if (cageY <= 0) {
                    cageY = 0;
                    landed = true;
                    landTick = ticksAlive;
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.9f, 0.6f);
                    c.getWorld().spawnParticle(Particle.BLOCK, c, 35, 1.0, 0.2, 1.0, 0,
                            Material.IRON_BLOCK.createBlockData());
                }
                repositionGibbet(c);

                // Creaking on descent
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, cageY + 2, 0),
                            Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.3f + (float) Math.random() * 0.3f);
                }
                return;
            }

            // Phase 2: Sit on ground for 60 ticks, then begin rising
            if (landed && !rising) {
                int ticksSinceLand = ticksAlive - landTick;
                if (ticksSinceLand >= 60) {
                    rising = true;
                    riseTick = ticksAlive;
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.5f);
                }

                // Eerie creak while sitting
                if (ticksSinceLand % 25 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, 2, 0),
                            Sound.BLOCK_CHAIN_BREAK, 0.6f, 0.2f);
                }
            }

            // Phase 3: Rise back up
            if (rising) {
                cageY += 0.15f;
                repositionGibbet(c);

                // Strained chain sounds
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, cageY + 4, 0),
                            Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.6f + cageY * 0.02f);
                }
            }

            // Ongoing: swaying motion
            float sway = (float) Math.sin(ticksAlive * 0.05) * 0.15f;
            for (BlockDisplayHandle bar : curvedBars) {
                Location bLoc = bar.entity().getLocation();
                bar.entity().teleport(bLoc.clone().add(sway, 0, 0));
            }

            // Iron dust trailing below cage
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, cageY + 0.5, 0), 5, 1.0, 180, 180, 190, 0.6f);
            }
        }

        private void repositionGibbet(Location c) {
            topRing.entity().teleport(c.clone().add(0, cageY + 3.5, 0));
            bottomRing.entity().teleport(c.clone().add(0, cageY, 0));
            hookChain.entity().teleport(c.clone().add(0, cageY + 4.0, 0));
            for (int i = 0; i < curvedBars.size(); i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 1.2;
                double z = Math.sin(angle) * 1.2;
                curvedBars.get(i).entity().teleport(c.clone().add(x, cageY + 1.75, z));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SuspendedGibbet(plugin); }
    }

    // ================================================================
    // 4. CHAIN WEB — Spider-web pattern of chains on the ground, rises to waist height
    // ================================================================
    public static class ChainWeb extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> radialChains = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private BlockDisplayHandle centerNode;
        private float webHeight = 0.0f;
        private boolean raised = false;

        public ChainWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_web", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(250);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center node - heavy core
            centerNode = displayBuilder.spawnBlock(center.clone().add(0, 0.1, 0), Material.HEAVY_CORE);
            centerNode.scale(0.5f, 0.3f, 0.5f).glow(100, 100, 110).interpolation(2, 0);
            spawnedEntities.add(centerNode.entity());

            // 16 radial chains from center outward
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                double x = Math.cos(angle) * 3.0;
                double z = Math.sin(angle) * 3.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x * 0.5, 0.1, z * 0.5), Material.CHAIN);
                // Scale along radial direction
                float sx = (float) Math.abs(Math.cos(angle)) * 2.5f + 0.2f;
                float sz = (float) Math.abs(Math.sin(angle)) * 2.5f + 0.2f;
                h.scale(sx, 0.15f, sz).glow(180, 180, 190).interpolation(2, 0);
                radialChains.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner concentric ring at radius 1.5 — 6 chain segments
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0.1, z), Material.CHAIN);
                h.scale(0.8f, 0.15f, 0.8f).glow(100, 100, 110).interpolation(2, 0);
                innerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Outer concentric ring at radius 3.5 — 8 chain segments
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 3.5;
                double z = Math.sin(angle) * 3.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0.1, z), Material.CHAIN);
                h.scale(1.2f, 0.15f, 1.2f).glow(180, 100, 40).interpolation(2, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Rise to waist height (y=1.0) over 30 ticks
            if (!raised && ticksAlive <= 30) {
                webHeight = (ticksAlive / 30.0f) * 1.0f;

                centerNode.entity().teleport(c.clone().add(0, webHeight, 0));

                for (int i = 0; i < radialChains.size(); i++) {
                    double angle = (2 * Math.PI * i) / 16;
                    radialChains.get(i).entity().teleport(
                            c.clone().add(Math.cos(angle) * 1.5, webHeight, Math.sin(angle) * 1.5));
                }
                for (int i = 0; i < innerRing.size(); i++) {
                    double angle = (2 * Math.PI * i) / 6;
                    innerRing.get(i).entity().teleport(
                            c.clone().add(Math.cos(angle) * 1.5, webHeight, Math.sin(angle) * 1.5));
                }
                for (int i = 0; i < outerRing.size(); i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    outerRing.get(i).entity().teleport(
                            c.clone().add(Math.cos(angle) * 3.5, webHeight, Math.sin(angle) * 3.5));
                }

                // Chain stretching sound
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 1.0f + webHeight * 0.3f);
                }

                if (ticksAlive == 30) {
                    raised = true;
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.5f);
                }
            }

            // Ongoing: sticky web particles (white dust dripping down)
            if (ticksAlive % 4 == 0 && raised) {
                for (int i = 0; i < 4; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = Math.random() * 3.5;
                    Location pLoc = c.clone().add(Math.cos(angle) * radius, webHeight, Math.sin(angle) * radius);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.3, 200, 200, 210, 0.5f);
                }
            }

            // Undulating height variation
            if (raised && ticksAlive % 3 == 0) {
                float wave = (float) Math.sin(ticksAlive * 0.1) * 0.1f;
                centerNode.entity().teleport(c.clone().add(0, webHeight + wave, 0));
            }

            // Ambient sound
            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.3f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainWeb(plugin); }
    }

    // ================================================================
    // 5. CLOSING WALLS — 4 walls of chains closing inward
    // ================================================================
    public static class ClosingWalls extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> walls = new ArrayList<>();
        private float wallDistance = 4.0f;
        private static final int WALL_WIDTH = 4;
        private static final int WALL_HEIGHT = 4;

        public ClosingWalls(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("closing_walls", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(260);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 walls: North (-Z), South (+Z), West (-X), East (+X)
            // Each wall = 4 wide x 4 tall = 16 chains, but use representative blocks
            // We use 4 chains per wall to stay within budget, each scaled large
            double[][] wallDirs = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

            for (double[] dir : wallDirs) {
                List<BlockDisplayHandle> wall = new ArrayList<>();
                for (int col = 0; col < WALL_WIDTH; col++) {
                    for (int row = 0; row < WALL_HEIGHT; row++) {
                        double baseX = dir[0] * wallDistance;
                        double baseZ = dir[2] * wallDistance;

                        // Perpendicular offset for width
                        double perpX = (dir[2] != 0) ? (col - 1.5) * 1.0 : 0;
                        double perpZ = (dir[0] != 0) ? (col - 1.5) * 1.0 : 0;

                        Material mat = ((col + row) % 3 == 0) ? Material.IRON_BARS : Material.CHAIN;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                center.clone().add(baseX + perpX, row * 1.0, baseZ + perpZ), mat);
                        int glowR = ((col + row) % 2 == 0) ? 180 : 100;
                        int glowG = ((col + row) % 2 == 0) ? 180 : 100;
                        int glowB = ((col + row) % 2 == 0) ? 190 : 110;
                        h.scale(1.0f, 1.0f, 0.2f).glow(glowR, glowG, glowB).interpolation(2, 0);
                        wall.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                walls.add(wall);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.4f);
            DisplayBuilder.dustParticles(center, 30, 4.0, 180, 180, 190, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double[][] wallDirs = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

            // Walls slowly close from distance 4 to 0.3 over 180 ticks
            if (wallDistance > 0.3f) {
                wallDistance -= 0.02f;
                if (wallDistance < 0.3f) wallDistance = 0.3f;

                for (int w = 0; w < walls.size(); w++) {
                    List<BlockDisplayHandle> wall = walls.get(w);
                    double[] dir = wallDirs[w];
                    int idx = 0;
                    for (int col = 0; col < WALL_WIDTH; col++) {
                        for (int row = 0; row < WALL_HEIGHT; row++) {
                            if (idx >= wall.size()) break;
                            double baseX = dir[0] * wallDistance;
                            double baseZ = dir[2] * wallDistance;
                            double perpX = (dir[2] != 0) ? (col - 1.5) * 1.0 : 0;
                            double perpZ = (dir[0] != 0) ? (col - 1.5) * 1.0 : 0;
                            wall.get(idx).entity().teleport(
                                    c.clone().add(baseX + perpX, row * 1.0, baseZ + perpZ));
                            idx++;
                        }
                    }
                }
            }

            // Grinding sounds that increase in pitch as walls close
            if (ticksAlive % 15 == 0) {
                float pitch = 0.3f + (1.0f - wallDistance / 4.0f) * 0.8f;
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, pitch);
            }

            // Scraping chain sound
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.6f + (float) Math.random() * 0.4f);
            }

            // Spark particles at wall edges
            if (ticksAlive % 5 == 0) {
                for (double[] dir : wallDirs) {
                    Location spark = c.clone().add(dir[0] * wallDistance, 2.0, dir[2] * wallDistance);
                    DisplayBuilder.dustParticles(spark, 4, 0.5, 255, 200, 100, 0.7f);
                }
            }

            // Rust dust from walls
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 6, wallDistance, 180, 100, 40, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ClosingWalls(plugin); }
    }

    // ================================================================
    // 6. CHAIN DOME — Hemispherical dome of chains, interior chains fall inward
    // ================================================================
    public static class ChainDome extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> domeChains = new ArrayList<>();
        private final List<BlockDisplayHandle> fallingInterior = new ArrayList<>();
        private boolean domeComplete = false;
        private int completeTick = -1;

        public ChainDome(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_dome", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(280);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Dome: 24 chains arranged in hemisphere, radius 6
            // 3 latitude rings: ground (8), mid (8), top (8) converging to apex
            // Ground ring at y=0, radius=6
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 6.0;
                double z = Math.sin(angle) * 6.0;
                // Spawn at ground, will animate upward
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0, z), Material.CHAIN);
                h.scale(0.3f, 0.5f, 0.3f).glow(180, 180, 190).interpolation(3, 0);
                domeChains.add(h);
                spawnedEntities.add(h.entity());
            }

            // Mid ring at y=3, radius=4.5
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8 + Math.PI / 8;
                double x = Math.cos(angle) * 4.5;
                double z = Math.sin(angle) * 4.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0, z), Material.CHAIN);
                h.scale(0.3f, 0.5f, 0.3f).glow(100, 100, 110).interpolation(3, 0);
                domeChains.add(h);
                spawnedEntities.add(h.entity());
            }

            // Top ring at y=5, radius=2
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0, z), Material.IRON_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(180, 100, 40).interpolation(3, 0);
                domeChains.add(h);
                spawnedEntities.add(h.entity());
            }

            // Apex block
            BlockDisplayHandle apex = displayBuilder.spawnBlock(
                    center.clone().add(0, 0, 0), Material.HEAVY_CORE);
            apex.scale(0.6f, 0.6f, 0.6f).glow(100, 100, 110).interpolation(3, 0);
            domeChains.add(apex);
            spawnedEntities.add(apex.entity());

            // Interior falling chains (spawned above, will fall later)
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6 + Math.PI / 6;
                double x = Math.cos(angle) * 3.0;
                double z = Math.sin(angle) * 3.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 6.0, z), Material.CHAIN);
                h.scale(0.2f, 1.5f, 0.2f).glow(180, 180, 190).interpolation(2, 0);
                fallingInterior.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Form dome from ground up over 60 ticks
            if (!domeComplete && ticksAlive <= 60) {
                float progress = ticksAlive / 60.0f;

                // Ground ring stays at y=0
                // Mid ring rises to y=3
                for (int i = 8; i < 16; i++) {
                    double angle = (2 * Math.PI * (i - 8)) / 8 + Math.PI / 8;
                    double x = Math.cos(angle) * 4.5;
                    double z = Math.sin(angle) * 4.5;
                    domeChains.get(i).entity().teleport(c.clone().add(x, 3.0 * progress, z));
                    domeChains.get(i).scale(0.3f, 0.5f + progress * 1.5f, 0.3f);
                    domeChains.get(i).interpolation(3, 0);
                }

                // Top ring rises to y=5
                for (int i = 16; i < 22; i++) {
                    double angle = (2 * Math.PI * (i - 16)) / 6;
                    double x = Math.cos(angle) * 2.0;
                    double z = Math.sin(angle) * 2.0;
                    domeChains.get(i).entity().teleport(c.clone().add(x, 5.0 * progress, z));
                }

                // Apex rises to y=6
                if (domeChains.size() > 22) {
                    domeChains.get(22).entity().teleport(c.clone().add(0, 6.0 * progress, 0));
                }

                // Ground ring vertical bars grow
                for (int i = 0; i < 8; i++) {
                    domeChains.get(i).scale(0.3f, 0.5f + progress * 2.5f, 0.3f);
                    domeChains.get(i).interpolation(3, 0);
                }

                // Construction sounds
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.5f + progress * 0.5f);
                }

                if (ticksAlive == 60) {
                    domeComplete = true;
                    completeTick = ticksAlive;
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.8f);
                }
            }

            // Phase 2: Interior chains fall inward after dome is complete
            if (domeComplete && completeTick > 0) {
                int ticksSinceComplete = ticksAlive - completeTick;

                // Each chain falls at a staggered time
                for (int i = 0; i < fallingInterior.size(); i++) {
                    int startFall = i * 10; // Stagger by 10 ticks each
                    if (ticksSinceComplete > startFall) {
                        int fallProgress = ticksSinceComplete - startFall;
                        double fallY = Math.max(0.5, 6.0 - fallProgress * 0.15);
                        double angle = (2 * Math.PI * i) / 6 + Math.PI / 6;
                        double pullInRadius = Math.max(0.5, 3.0 - fallProgress * 0.05);
                        double x = Math.cos(angle) * pullInRadius;
                        double z = Math.sin(angle) * pullInRadius;
                        fallingInterior.get(i).entity().teleport(c.clone().add(x, fallY, z));

                        // Impact sound when reaching ground
                        if (fallY <= 0.6 && fallProgress > 0 && (fallProgress - 1) * 0.15 < 5.4) {
                            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.8f);
                        }
                    }
                }
            }

            // Ominous drone
            if (ticksAlive % 40 == 0 && domeComplete) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 0.2f);
            }

            // Dark iron dust swirling inside dome
            if (ticksAlive % 6 == 0 && domeComplete) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 4.0;
                Location dustLoc = c.clone().add(Math.cos(angle) * r, Math.random() * 4, Math.sin(angle) * r);
                DisplayBuilder.dustParticles(dustLoc, 3, 0.5, 100, 100, 110, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainDome(plugin); }
    }

    // ================================================================
    // 7. PRISON BOX — Massive box appearing piece by piece
    // ================================================================
    public static class PrisonBox extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> floorBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> ceilingBlocks = new ArrayList<>();
        private int buildPhase = 0; // 0=floor, 1=walls, 2=ceiling

        public PrisonBox(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prison_box", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Floor: 4 iron blocks in a 2x2 grid (representative of 3x3 face)
            double[][] floorPos = {{-1, 0, -1}, {-1, 0, 1}, {1, 0, -1}, {1, 0, 1}};
            for (double[] pos : floorPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], -0.5, pos[2]), Material.DEEPSLATE);
                h.scale(1.5f, 0.5f, 1.5f).glow(100, 100, 110).interpolation(4, 0);
                // Start invisible (scale 0) and grow
                h.scale(0.0f, 0.0f, 0.0f);
                floorBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Walls: 2 blocks per face, 4 faces = 8 wall blocks
            double[][] wallPos = {
                    {-1.5, 1.5, 0}, {-1.5, 3.0, 0},  // West
                    {1.5, 1.5, 0},  {1.5, 3.0, 0},    // East
                    {0, 1.5, -1.5}, {0, 3.0, -1.5},   // North
                    {0, 1.5, 1.5},  {0, 3.0, 1.5}     // South
            };
            for (int i = 0; i < wallPos.length; i++) {
                double[] pos = wallPos[i];
                Material mat = (i % 2 == 0) ? Material.IRON_BARS : Material.CHAIN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], pos[1], pos[2]), mat);
                boolean isNS = (Math.abs(pos[2]) > 1.0);
                float sx = isNS ? 2.5f : 0.3f;
                float sz = isNS ? 0.3f : 2.5f;
                h.scale(0.0f, 0.0f, 0.0f).glow(180, 180, 190).interpolation(4, 0);
                wallBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ceiling: 4 blocks
            double[][] ceilPos = {{-1, 4.5, -1}, {-1, 4.5, 1}, {1, 4.5, -1}, {1, 4.5, 1}};
            for (double[] pos : ceilPos) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(pos[0], pos[1], pos[2]), Material.IRON_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(180, 100, 40).interpolation(4, 0);
                ceilingBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Floor appears (ticks 0-30)
            if (buildPhase == 0 && ticksAlive <= 30) {
                float progress = ticksAlive / 30.0f;
                for (BlockDisplayHandle h : floorBlocks) {
                    h.scale(1.5f * progress, 0.5f * progress, 1.5f * progress);
                    h.interpolation(3, 0);
                }

                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.7f);
                }

                if (ticksAlive == 30) {
                    buildPhase = 1;
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.0f);
                }
            }

            // Phase 2: Walls rise (ticks 31-80)
            if (buildPhase == 1 && ticksAlive > 30 && ticksAlive <= 80) {
                float progress = (ticksAlive - 30) / 50.0f;

                for (int i = 0; i < wallBlocks.size(); i++) {
                    boolean isNS = (i >= 4);
                    float sx = isNS ? 2.5f * progress : 0.3f * progress;
                    float sz = isNS ? 0.3f * progress : 2.5f * progress;
                    wallBlocks.get(i).scale(sx, 1.5f * progress, sz);
                    wallBlocks.get(i).interpolation(3, 0);
                }

                // Metallic building sounds
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.8f + progress * 0.3f);
                }

                if (ticksAlive == 80) {
                    buildPhase = 2;
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 0.6f);
                }
            }

            // Phase 3: Ceiling locks in (ticks 81-100)
            if (buildPhase == 2 && ticksAlive > 80 && ticksAlive <= 100) {
                float progress = (ticksAlive - 80) / 20.0f;
                for (BlockDisplayHandle h : ceilingBlocks) {
                    h.scale(1.5f * progress, 0.5f * progress, 1.5f * progress);
                    h.interpolation(3, 0);
                }

                if (ticksAlive == 100) {
                    // Lock-in slam
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
                    c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 4.5, 0), 30, 1.0, 0.2, 1.0, 0,
                            Material.IRON_BLOCK.createBlockData());
                }
            }

            // Ongoing effects after built
            if (ticksAlive > 100) {
                // Interior rust particles
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(
                            c.clone().add((Math.random() - 0.5) * 2, Math.random() * 4, (Math.random() - 0.5) * 2),
                            3, 0.3, 180, 100, 40, 0.5f);
                }

                // Metallic groaning
                if (ticksAlive % 50 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.4f, 0.3f);
                }

                // Dripping particles from ceiling
                if (ticksAlive % 8 == 0) {
                    c.getWorld().spawnParticle(Particle.DRIPPING_DRIPSTONE_LAVA,
                            c.clone().add((Math.random() - 0.5) * 2, 4.0, (Math.random() - 0.5) * 2),
                            2, 0.1, 0.05, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PrisonBox(plugin); }
    }

    // ================================================================
    // 8. CHAIN VORTEX TRAP — Spiraling tornado of chains that tightens
    // ================================================================
    public static class ChainVortexTrap extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spiralChains = new ArrayList<>();
        private float vortexRadius = 5.0f;
        private float spinSpeed = 0.05f;

        public ChainVortexTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_vortex_trap", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(240);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 15 chain blocks in a spiral formation
            for (int i = 0; i < 15; i++) {
                double angle = (2 * Math.PI * i) / 8; // ~2 full rotations
                double height = i * 0.5;
                double x = Math.cos(angle) * vortexRadius;
                double z = Math.sin(angle) * vortexRadius;
                Material mat = (i % 3 == 0) ? Material.IRON_BLOCK : Material.CHAIN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, height, z), mat);
                float scale = 0.4f + (i / 15.0f) * 0.3f;
                h.scale(scale, 0.8f, scale).glow(180, 180, 190).interpolation(2, 0);
                spiralChains.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
            DisplayBuilder.dustParticles(center, 30, 3.0, 180, 180, 190, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Contract vortex over time
            if (vortexRadius > 0.5f) {
                vortexRadius -= 0.015f;
                if (vortexRadius < 0.5f) vortexRadius = 0.5f;
            }

            // Spin speed increases as vortex contracts
            spinSpeed = 0.05f + (1.0f - vortexRadius / 5.0f) * 0.15f;

            // Update all chain positions — spiraling and contracting
            for (int i = 0; i < spiralChains.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 8;
                double spinOffset = ticksAlive * spinSpeed;
                double angle = baseAngle + spinOffset;
                double height = i * 0.5;

                // Slight vertical wave
                double vertWave = Math.sin(ticksAlive * 0.08 + i * 0.5) * 0.3;

                double x = Math.cos(angle) * vortexRadius;
                double z = Math.sin(angle) * vortexRadius;
                spiralChains.get(i).entity().teleport(c.clone().add(x, height + vertWave, z));

                // Rotate blocks to face center
                float faceAngle = (float) (angle + Math.PI);
                spiralChains.get(i).rotate(faceAngle, 0, 1, 0);
                spiralChains.get(i).interpolation(2, 0);
            }

            // Whooshing wind sound increasing with speed
            if (ticksAlive % 8 == 0) {
                float pitch = 0.5f + spinSpeed * 3.0f;
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.8f, Math.min(pitch, 2.0f));
            }

            // Wind particles spiraling
            if (ticksAlive % 3 == 0) {
                double pAngle = ticksAlive * spinSpeed * 2;
                double px = Math.cos(pAngle) * (vortexRadius + 0.5);
                double pz = Math.sin(pAngle) * (vortexRadius + 0.5);
                c.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                        c.clone().add(px, Math.random() * 6, pz), 2, 0.2, 0.5, 0.2, 0.01);
            }

            // Iron dust in vortex center
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 5, vortexRadius * 0.5, 180, 180, 190, 0.6f);
            }

            // Chain rattle sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 1.0f + (float) Math.random() * 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainVortexTrap(plugin); }
    }

    // ================================================================
    // 9. GALLOWS FRAME — Classic gallows with upright posts, crossbeam, and hanging loop
    // ================================================================
    public static class GallowsFrame extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftPost = new ArrayList<>();
        private final List<BlockDisplayHandle> rightPost = new ArrayList<>();
        private final List<BlockDisplayHandle> crossbeam = new ArrayList<>();
        private final List<BlockDisplayHandle> nooseLoop = new ArrayList<>();
        private int buildStep = 0; // 0-3 stages of assembly
        private int buildTick = 0;

        public GallowsFrame(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gallows_frame", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(250);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left upright post (4 chains tall) — starts at zero scale
            for (int y = 0; y < 4; y++) {
                Material mat = (y == 0) ? Material.DEEPSLATE : Material.CHAIN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-2, y, 0), mat);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 100, 110).interpolation(3, 0);
                leftPost.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right upright post (4 chains tall)
            for (int y = 0; y < 4; y++) {
                Material mat = (y == 0) ? Material.DEEPSLATE : Material.CHAIN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(2, y, 0), mat);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 100, 110).interpolation(3, 0);
                rightPost.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crossbeam (3 chains wide at top)
            for (int x = -1; x <= 1; x++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 4, 0), Material.IRON_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(180, 180, 190).interpolation(3, 0);
                crossbeam.add(h);
                spawnedEntities.add(h.entity());
            }

            // Hanging noose loop (3 chains hanging down from center of crossbeam)
            for (int y = 0; y < 3; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, 3.5 - y * 0.8, 0), Material.CHAIN);
                h.scale(0.0f, 0.0f, 0.0f).glow(180, 100, 40).interpolation(3, 0);
                nooseLoop.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Assembly phase 1: Left post appears (ticks 0-25)
            if (buildStep == 0) {
                float progress = Math.min(1.0f, ticksAlive / 25.0f);
                for (int i = 0; i < leftPost.size(); i++) {
                    float segProgress = Math.min(1.0f, progress * leftPost.size() - i);
                    if (segProgress > 0) {
                        leftPost.get(i).scale(0.5f * segProgress, 1.0f * segProgress, 0.5f * segProgress);
                        leftPost.get(i).interpolation(3, 0);
                    }
                }
                if (ticksAlive == 5 || ticksAlive == 15) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.6f);
                }
                if (ticksAlive >= 25) { buildStep = 1; buildTick = ticksAlive; }
            }

            // Assembly phase 2: Right post (ticks 25-50)
            if (buildStep == 1) {
                int elapsed = ticksAlive - buildTick;
                float progress = Math.min(1.0f, elapsed / 25.0f);
                for (int i = 0; i < rightPost.size(); i++) {
                    float segProgress = Math.min(1.0f, progress * rightPost.size() - i);
                    if (segProgress > 0) {
                        rightPost.get(i).scale(0.5f * segProgress, 1.0f * segProgress, 0.5f * segProgress);
                        rightPost.get(i).interpolation(3, 0);
                    }
                }
                if (elapsed == 5 || elapsed == 15) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.7f);
                }
                if (elapsed >= 25) { buildStep = 2; buildTick = ticksAlive; }
            }

            // Assembly phase 3: Crossbeam (ticks 50-70)
            if (buildStep == 2) {
                int elapsed = ticksAlive - buildTick;
                float progress = Math.min(1.0f, elapsed / 20.0f);
                for (int i = 0; i < crossbeam.size(); i++) {
                    float segProgress = Math.min(1.0f, progress * crossbeam.size() - i);
                    if (segProgress > 0) {
                        crossbeam.get(i).scale(1.2f * segProgress, 0.6f * segProgress, 0.6f * segProgress);
                        crossbeam.get(i).interpolation(3, 0);
                    }
                }
                if (elapsed == 10) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.8f);
                }
                if (elapsed >= 20) { buildStep = 3; buildTick = ticksAlive; }
            }

            // Assembly phase 4: Noose drops (ticks 70-90)
            if (buildStep == 3) {
                int elapsed = ticksAlive - buildTick;
                float progress = Math.min(1.0f, elapsed / 20.0f);
                for (int i = 0; i < nooseLoop.size(); i++) {
                    float segProgress = Math.min(1.0f, progress * 3 - i);
                    if (segProgress > 0) {
                        nooseLoop.get(i).scale(0.25f * segProgress, 0.7f * segProgress, 0.25f * segProgress);
                        nooseLoop.get(i).interpolation(3, 0);
                    }
                }
                if (elapsed == 5) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.4f);
                }
            }

            // Ongoing: Noose sways after assembly
            if (buildStep == 3 && (ticksAlive - buildTick) > 20) {
                float sway = (float) Math.sin(ticksAlive * 0.04) * 0.3f;
                for (int i = 0; i < nooseLoop.size(); i++) {
                    nooseLoop.get(i).entity().teleport(
                            c.clone().add(sway * (i + 1) * 0.3, 3.5 - i * 0.8, 0));
                }
            }

            // Creaking wood/chain sounds
            if (ticksAlive % 35 == 0 && buildStep >= 3) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.25f);
            }

            // Rust dust particles under noose (damage zone indicator)
            if (ticksAlive % 6 == 0 && buildStep >= 3) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 4, 1.0, 180, 100, 40, 0.8f);
            }

            // Ground smoke under drop
            if (ticksAlive % 10 == 0 && buildStep >= 3) {
                c.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                        c.clone().add(0, 0.1, 0), 3, 0.8, 0.05, 0.8, 0.005);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GallowsFrame(plugin); }
    }

    // ================================================================
    // 10. CHAIN COIL — Helical spring that compresses then violently expands
    // ================================================================
    public static class ChainCoil extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> coilBlocks = new ArrayList<>();
        private float compressionProgress = 0.0f;
        private boolean compressed = false;
        private boolean exploded = false;
        private int compressTick = -1;
        private static final int COIL_COUNT = 14;

        public ChainCoil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_coil", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14 chain blocks in a helical pattern (2 full rotations, radius 2)
            for (int i = 0; i < COIL_COUNT; i++) {
                double angle = (4 * Math.PI * i) / COIL_COUNT; // 2 full rotations
                double height = i * 0.6;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                Material mat = (i % 4 == 0) ? Material.IRON_BLOCK : Material.CHAIN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, height, z), mat);
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 180, 190).interpolation(2, 0);
                coilBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Compress downward over 80 ticks
            if (!compressed && ticksAlive <= 80) {
                compressionProgress = ticksAlive / 80.0f;

                for (int i = 0; i < coilBlocks.size(); i++) {
                    double angle = (4 * Math.PI * i) / COIL_COUNT;
                    // Height compresses: full height -> 1 block
                    double fullHeight = i * 0.6;
                    double compressedHeight = i * 0.1;
                    double height = fullHeight - (fullHeight - compressedHeight) * compressionProgress;

                    // Radius stays constant during compression
                    double x = Math.cos(angle) * 2.0;
                    double z = Math.sin(angle) * 2.0;
                    coilBlocks.get(i).entity().teleport(c.clone().add(x, height, z));

                    // Glow increasingly red as tension builds
                    int red = (int) (180 + compressionProgress * 75);
                    int green = (int) (180 - compressionProgress * 80);
                    int blue = (int) (190 - compressionProgress * 100);
                    coilBlocks.get(i).glow(Math.min(255, red), Math.max(0, green), Math.max(40, blue));
                }

                // Tension creaking sounds
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.4f + compressionProgress * 0.6f);
                }

                // Strain particles
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 4, 1.5,
                            (int) (180 + compressionProgress * 75), 100, 40, 0.7f);
                }

                if (ticksAlive == 80) {
                    compressed = true;
                    compressTick = ticksAlive;
                    // Tension at maximum - high pitched ping
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.5f);
                }
            }

            // Phase 2: Hold compressed for 20 ticks with vibration
            if (compressed && !exploded && (ticksAlive - compressTick) <= 20) {
                int holdTicks = ticksAlive - compressTick;
                float vibration = (float) Math.sin(holdTicks * 1.5) * 0.1f;

                for (int i = 0; i < coilBlocks.size(); i++) {
                    double angle = (4 * Math.PI * i) / COIL_COUNT;
                    double height = i * 0.1;
                    double x = Math.cos(angle) * 2.0 + vibration;
                    double z = Math.sin(angle) * 2.0 + vibration;
                    coilBlocks.get(i).entity().teleport(c.clone().add(x, height, z));
                }

                // Rising tension sound
                if (holdTicks % 5 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.5f, 1.5f + holdTicks * 0.03f);
                }
            }

            // Phase 3: Explosive expansion
            if (compressed && !exploded && (ticksAlive - compressTick) > 20) {
                exploded = true;
                triggerImpactDamage(c);

                // Launch blocks upward and outward
                for (int i = 0; i < coilBlocks.size(); i++) {
                    double angle = (4 * Math.PI * i) / COIL_COUNT;
                    double launchX = Math.cos(angle) * 5.0;
                    double launchZ = Math.sin(angle) * 5.0;
                    double launchY = 8.0 + Math.random() * 4.0;
                    coilBlocks.get(i).animateTo(
                            new Vector3f((float) launchX, (float) launchY, (float) launchZ),
                            new AxisAngle4f((float) (Math.random() * Math.PI), 1, 0, 0),
                            new Vector3f(0.5f, 0.5f, 0.5f),
                            15);
                }

                // Explosion effects
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.3f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.5f);
                c.getWorld().spawnParticle(Particle.BLOCK, c, 80, 3, 2, 3, 0.5,
                        Material.CHAIN.createBlockData());
                DisplayBuilder.dustParticles(c, 40, 4.0, 255, 180, 80, 2.0f);
            }

            // Post-explosion: particles linger
            if (exploded && ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 5, 3.0, 180, 100, 40, 0.5f);
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            DisplayBuilder.dustParticles(impactLocation, 50, 5.0, 255, 200, 100, 2.0f);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainCoil(plugin); }
    }

    // ================================================================
    // 11. LANTERN CAGE — Ornate lantern shape with damaging pulse waves
    // ================================================================
    public static class LanternCage extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> topPyramid = new ArrayList<>();
        private final List<BlockDisplayHandle> middleRing = new ArrayList<>();
        private final List<BlockDisplayHandle> bottomPyramid = new ArrayList<>();
        private BlockDisplayHandle centralGlow;
        private float rotationAngle = 0;
        private int pulseCount = 0;

        public LanternCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lantern_cage", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Top pyramid: 4 chains angling inward to apex
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4 + Math.PI / 4;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 4.0, z), Material.CHAIN);
                h.scale(0.25f, 2.0f, 0.25f).glow(180, 180, 190).interpolation(2, 0);
                // Lean inward
                float lean = (float) (Math.PI * 0.2);
                h.rotate(lean, (float) -Math.sin(angle), 0, (float) Math.cos(angle));
                topPyramid.add(h);
                spawnedEntities.add(h.entity());
            }

            // Middle ring: 6 chains forming hexagonal outline
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                Material mat = (i % 2 == 0) ? Material.IRON_BARS : Material.CHAIN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 2.5, z), mat);
                h.scale(0.3f, 2.5f, 0.3f).glow(100, 100, 110).interpolation(2, 0);
                middleRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Bottom pyramid: 4 chains angling inward to bottom point
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4 + Math.PI / 4;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 1.0, z), Material.CHAIN);
                h.scale(0.25f, 1.5f, 0.25f).glow(180, 100, 40).interpolation(2, 0);
                float lean = (float) (-Math.PI * 0.2);
                h.rotate(lean, (float) -Math.sin(angle), 0, (float) Math.cos(angle));
                bottomPyramid.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central glowing block
            centralGlow = displayBuilder.spawnBlock(center.clone().add(0, 2.5, 0), Material.HEAVY_CORE);
            centralGlow.scale(0.8f, 0.8f, 0.8f).glow(255, 200, 100).brightness(15, 15).interpolation(2, 0);
            spawnedEntities.add(centralGlow.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.8f);
            DisplayBuilder.dustParticles(center.clone().add(0, 2.5, 0), 20, 2.0, 255, 200, 100, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow rotation of entire lantern
            rotationAngle += 0.02f;

            // Rotate middle ring
            for (int i = 0; i < middleRing.size(); i++) {
                double angle = (2 * Math.PI * i) / 6 + rotationAngle;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                middleRing.get(i).entity().teleport(c.clone().add(x, 2.5, z));
            }

            // Rotate top pyramid
            for (int i = 0; i < topPyramid.size(); i++) {
                double angle = (2 * Math.PI * i) / 4 + Math.PI / 4 + rotationAngle;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                topPyramid.get(i).entity().teleport(c.clone().add(x, 4.0, z));
            }

            // Rotate bottom pyramid
            for (int i = 0; i < bottomPyramid.size(); i++) {
                double angle = (2 * Math.PI * i) / 4 + Math.PI / 4 + rotationAngle;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                bottomPyramid.get(i).entity().teleport(c.clone().add(x, 1.0, z));
            }

            // Central glow pulsing
            float pulse = (float) (0.8 + Math.sin(ticksAlive * 0.15) * 0.3);
            centralGlow.scale(pulse, pulse, pulse);
            centralGlow.interpolation(3, 0);

            // Damaging pulse wave every 40 ticks
            if (ticksAlive % 40 == 0 && ticksAlive > 20) {
                pulseCount++;

                // Expanding ring of dust particles
                DisplayBuilder.particleRing(c.clone().add(0, 2.5, 0), 1.0, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(255, 200, 100), 1.5f));

                // Delayed expanding rings
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    DisplayBuilder.particleRing(c.clone().add(0, 2.5, 0), 2.5, Particle.DUST, 24,
                            new Particle.DustOptions(Color.fromRGB(255, 180, 80), 1.2f));
                }, 3L);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    DisplayBuilder.particleRing(c.clone().add(0, 2.5, 0), 4.0, Particle.DUST, 32,
                            new Particle.DustOptions(Color.fromRGB(255, 150, 50), 1.0f));
                }, 6L);

                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.9f, 1.2f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.5f, 1.5f);
            }

            // Ethereal glow particles around lantern
            if (ticksAlive % 4 == 0) {
                double pAngle = Math.random() * Math.PI * 2;
                double pR = 1.5 + Math.random();
                Location pLoc = c.clone().add(Math.cos(pAngle) * pR, 2.5 + (Math.random() - 0.5) * 3, Math.sin(pAngle) * pR);
                DisplayBuilder.dustParticles(pLoc, 2, 0.2, 255, 200, 100, 0.6f);
            }

            // Ambient hum
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.3f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LanternCage(plugin); }
    }

    // ================================================================
    // 12. CHAIN MAZE — Shifting maze of chain walls
    // ================================================================
    public static class ChainMaze extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallSegments = new ArrayList<>();
        private final boolean[] wallVisible;
        private final double[][] wallPositions;
        private final boolean[] wallIsXAxis;
        private int shuffleTimer = 0;

        public ChainMaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_maze", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);

            // 10 wall segments in a grid pattern
            wallPositions = new double[][] {
                    {-2, 0, 0}, {2, 0, 0}, {0, 0, -2}, {0, 0, 2},   // Inner cross
                    {-3, 0, -3}, {3, 0, -3}, {-3, 0, 3}, {3, 0, 3}, // Corners
                    {0, 0, -4}, {0, 0, 4}                             // Far walls
            };
            wallIsXAxis = new boolean[] {
                    false, false, true, true,
                    true, true, true, true,
                    true, true
            };
            wallVisible = new boolean[10];
            for (int i = 0; i < 10; i++) wallVisible[i] = true;
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 10 wall segments, each 2 blocks long, 2 tall (2 BlockDisplays per segment)
            for (int seg = 0; seg < 10; seg++) {
                for (int row = 0; row < 2; row++) {
                    double[] pos = wallPositions[seg];
                    Material mat = (seg % 3 == 0) ? Material.IRON_BARS : Material.CHAIN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(pos[0], row * 1.0, pos[2]), mat);
                    float sx = wallIsXAxis[seg] ? 2.0f : 0.2f;
                    float sz = wallIsXAxis[seg] ? 0.2f : 2.0f;
                    h.scale(sx, 1.0f, sz).glow(180, 180, 190).interpolation(4, 0);
                    wallSegments.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
            DisplayBuilder.dustParticles(center, 20, 4.0, 100, 100, 110, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Shuffle walls every 35 ticks: randomly hide/show walls
            shuffleTimer++;
            if (shuffleTimer >= 35) {
                shuffleTimer = 0;

                // Toggle 3-4 random walls
                int toggleCount = 3 + (int) (Math.random() * 2);
                for (int t = 0; t < toggleCount; t++) {
                    int wallIdx = (int) (Math.random() * 10);
                    wallVisible[wallIdx] = !wallVisible[wallIdx];

                    int baseIdx = wallIdx * 2;
                    for (int row = 0; row < 2; row++) {
                        if (baseIdx + row < wallSegments.size()) {
                            BlockDisplayHandle h = wallSegments.get(baseIdx + row);
                            if (wallVisible[wallIdx]) {
                                float sx = wallIsXAxis[wallIdx] ? 2.0f : 0.2f;
                                float sz = wallIsXAxis[wallIdx] ? 0.2f : 2.0f;
                                h.scale(sx, 1.0f, sz);
                            } else {
                                h.scale(0.0f, 0.0f, 0.0f);
                            }
                            h.interpolation(4, 0);
                        }
                    }
                }

                // Shuffle sound
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.7f, 0.8f + (float) Math.random() * 0.4f);
            }

            // Confusing particle trails along active walls
            if (ticksAlive % 4 == 0) {
                for (int seg = 0; seg < 10; seg++) {
                    if (wallVisible[seg]) {
                        double[] pos = wallPositions[seg];
                        Location wallLoc = c.clone().add(pos[0], 0.5, pos[2]);
                        DisplayBuilder.dustParticles(wallLoc, 2, 0.8, 180, 180, 190, 0.4f);
                    }
                }
            }

            // Ambient disorientation particles
            if (ticksAlive % 6 == 0) {
                double pAngle = Math.random() * Math.PI * 2;
                double pR = Math.random() * 4.0;
                c.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                        c.clone().add(Math.cos(pAngle) * pR, 0.1 + Math.random(), Math.sin(pAngle) * pR),
                        1, 0.1, 0.1, 0.1, 0.002);
            }

            // Metallic ambience
            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.3f + (float) Math.random() * 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainMaze(plugin); }
    }

    // ================================================================
    // 13. BARREL TRAP — Chain cylinder with iron caps that rolls and bursts
    // ================================================================
    public static class BarrelTrap extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> cylinderChains = new ArrayList<>();
        private BlockDisplayHandle topCap;
        private BlockDisplayHandle bottomCap;
        private double rollAngle = 0;
        private double rollX;
        private double rollZ;
        private double dirX;
        private double dirZ;
        private boolean burst = false;

        public BarrelTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("barrel_trap", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(180);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Start 8 blocks away from center, roll toward it
            double spawnAngle = Math.random() * Math.PI * 2;
            rollX = center.getX() + Math.cos(spawnAngle) * 8;
            rollZ = center.getZ() + Math.sin(spawnAngle) * 8;
            dirX = -Math.cos(spawnAngle) * 0.25;
            dirZ = -Math.sin(spawnAngle) * 0.25;

            Location barrelCenter = new Location(w, rollX, center.getY() + 1.0, rollZ);

            // 12 chains forming cylinder ring pattern
            for (int ring = 0; ring < 3; ring++) {
                for (int i = 0; i < 4; i++) {
                    double angle = (2 * Math.PI * i) / 4 + ring * (Math.PI / 6);
                    double x = Math.cos(angle) * 1.2;
                    double y = Math.sin(angle) * 1.2;
                    Material mat = (ring == 1) ? Material.IRON_BARS : Material.CHAIN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            barrelCenter.clone().add(x, y, ring * 0.8 - 0.8), mat);
                    h.scale(0.4f, 0.4f, 0.8f).glow(180, 180, 190).interpolation(2, 0);
                    cylinderChains.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Top cap
            topCap = displayBuilder.spawnBlock(
                    barrelCenter.clone().add(0, 0, 1.2), Material.IRON_BLOCK);
            topCap.scale(1.5f, 1.5f, 0.3f).glow(100, 100, 110).interpolation(2, 0);
            spawnedEntities.add(topCap.entity());

            // Bottom cap
            bottomCap = displayBuilder.spawnBlock(
                    barrelCenter.clone().add(0, 0, -1.2), Material.IRON_BLOCK);
            bottomCap.scale(1.5f, 1.5f, 0.3f).glow(100, 100, 110).interpolation(2, 0);
            spawnedEntities.add(bottomCap.entity());

            DisplayBuilder.playSound(barrelCenter, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (burst) {
                // Post-burst: scatter particles
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(
                            new Location(c.getWorld(), rollX, c.getY() + 1, rollZ),
                            5, 3.0, 180, 100, 40, 0.5f);
                }
                return;
            }

            // Roll forward
            rollX += dirX;
            rollZ += dirZ;
            rollAngle += 0.15;

            Location barrelCenter = new Location(c.getWorld(), rollX, c.getY() + 1.0, rollZ);

            // Check if reached target area (within 1.5 blocks of original center)
            double distToCenter = Math.sqrt(
                    (rollX - c.getX()) * (rollX - c.getX()) +
                    (rollZ - c.getZ()) * (rollZ - c.getZ()));

            if (distToCenter < 1.5) {
                burst = true;
                triggerImpactDamage(barrelCenter);

                // Burst: scatter all chains outward
                for (int i = 0; i < cylinderChains.size(); i++) {
                    double burstAngle = (2 * Math.PI * i) / cylinderChains.size();
                    float bx = (float) (Math.cos(burstAngle) * 4);
                    float by = (float) (Math.random() * 3 + 1);
                    float bz = (float) (Math.sin(burstAngle) * 4);
                    cylinderChains.get(i).animateTo(
                            new Vector3f(bx, by, bz),
                            new AxisAngle4f((float) (Math.random() * Math.PI * 2), 1, 0, 0),
                            new Vector3f(0.3f, 0.3f, 0.3f),
                            10);
                }

                // Caps fly off
                topCap.animateTo(new Vector3f(0, 5, 3), new AxisAngle4f(1.0f, 1, 0, 0),
                        new Vector3f(1.0f, 1.0f, 0.3f), 10);
                bottomCap.animateTo(new Vector3f(0, 4, -3), new AxisAngle4f(-1.0f, 1, 0, 0),
                        new Vector3f(1.0f, 1.0f, 0.3f), 10);

                DisplayBuilder.playSound(barrelCenter, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
                DisplayBuilder.playSound(barrelCenter, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.6f);
                barrelCenter.getWorld().spawnParticle(Particle.BLOCK, barrelCenter, 60, 2, 1, 2, 0.3,
                        Material.CHAIN.createBlockData());
                DisplayBuilder.dustParticles(barrelCenter, 40, 3.0, 255, 180, 80, 2.0f);
                return;
            }

            // Update chain positions with barrel roll rotation
            for (int ring = 0; ring < 3; ring++) {
                for (int i = 0; i < 4; i++) {
                    int idx = ring * 4 + i;
                    double angle = (2 * Math.PI * i) / 4 + ring * (Math.PI / 6) + rollAngle;
                    double cx = Math.cos(angle) * 1.2;
                    double cy = Math.sin(angle) * 1.2;
                    if (idx < cylinderChains.size()) {
                        cylinderChains.get(idx).entity().teleport(
                                barrelCenter.clone().add(cx, cy, ring * 0.8 - 0.8));
                    }
                }
            }

            // Caps follow
            topCap.entity().teleport(barrelCenter.clone().add(0, 0, 1.2));
            bottomCap.entity().teleport(barrelCenter.clone().add(0, 0, -1.2));

            // Rolling sound
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.playSound(barrelCenter, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.8f + (float) Math.random() * 0.3f);
            }

            // Ground trail particles
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(barrelCenter.clone().add(0, -0.5, 0), 3, 0.5, 100, 100, 110, 0.6f);
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            DisplayBuilder.dustParticles(impactLocation, 50, 4.0, 255, 180, 80, 2.0f);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BarrelTrap(plugin); }
    }

    // ================================================================
    // 14. CHAIN PYRAMID — Inverted pyramid descends point-first with ground impact
    // ================================================================
    public static class ChainPyramid extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> pyramidEdges = new ArrayList<>();
        private BlockDisplayHandle apexBlock;
        private float pyramidY = 15.0f;
        private boolean inverted = false;
        private boolean impacted = false;
        private int invertTick = -1;

        public ChainPyramid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_pyramid", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 edges with 3 chains each = 12 edge chains
            double[][] baseCorners = {
                    {-2, 0, -2}, {2, 0, -2}, {2, 0, 2}, {-2, 0, 2}
            };

            for (int edge = 0; edge < 4; edge++) {
                List<BlockDisplayHandle> edgeChains = new ArrayList<>();
                double[] from = baseCorners[edge];
                double[] to = baseCorners[(edge + 1) % 4];

                // Midpoint goes up to apex for pyramid sides
                for (int seg = 0; seg < 3; seg++) {
                    double t = seg / 2.0;
                    // Base line interpolation
                    double bx = from[0] + (to[0] - from[0]) * ((double) seg / 3);
                    double bz = from[2] + (to[2] - from[2]) * ((double) seg / 3);
                    // Rise toward apex at center, height 4
                    double heightFactor = 1.0 - Math.abs(seg - 1.0) / 1.5;
                    double y = heightFactor * 2.0;

                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(bx, pyramidY + y, bz), Material.CHAIN);
                    h.scale(0.35f, 0.35f, 0.35f).glow(180, 180, 190).interpolation(2, 0);
                    edgeChains.add(h);
                    spawnedEntities.add(h.entity());
                }
                pyramidEdges.add(edgeChains);
            }

            // Apex block at top center
            apexBlock = displayBuilder.spawnBlock(center.clone().add(0, pyramidY + 4, 0), Material.NETHERITE_BLOCK);
            apexBlock.scale(0.6f, 0.6f, 0.6f).glow(100, 100, 110).interpolation(2, 0);
            spawnedEntities.add(apexBlock.entity());

            DisplayBuilder.playSound(center.clone().add(0, pyramidY, 0), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double[][] baseCorners = {
                    {-2, 0, -2}, {2, 0, -2}, {2, 0, 2}, {-2, 0, 2}
            };

            // Phase 1: Descend normally for 30 ticks, then invert
            if (!inverted && ticksAlive <= 30) {
                pyramidY -= 0.2f;
                repositionPyramid(c, baseCorners, false);

                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(c.clone().add(0, pyramidY, 0),
                            Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.6f);
                }
            }

            // Invert at tick 30
            if (!inverted && ticksAlive == 30) {
                inverted = true;
                invertTick = ticksAlive;
                DisplayBuilder.playSound(c.clone().add(0, pyramidY, 0),
                        Sound.BLOCK_CHAIN_BREAK, 0.9f, 0.3f);
                DisplayBuilder.dustParticles(c.clone().add(0, pyramidY + 2, 0), 20, 2.0, 180, 100, 40, 1.2f);
            }

            // Phase 2: Fall inverted (point-down) accelerating
            if (inverted && !impacted) {
                int fallTicks = ticksAlive - invertTick;
                float fallSpeed = 0.1f + fallTicks * 0.02f; // Accelerating
                pyramidY -= fallSpeed;

                repositionPyramid(c, baseCorners, true);

                // Whistle/whoosh as it falls
                if (fallTicks % 5 == 0) {
                    float pitch = Math.min(2.0f, 0.5f + fallTicks * 0.04f);
                    DisplayBuilder.playSound(c.clone().add(0, pyramidY + 2, 0),
                            Sound.BLOCK_CHAIN_BREAK, 0.7f, pitch);
                }

                // Wind particles
                if (fallTicks % 3 == 0) {
                    c.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                            c.clone().add(0, pyramidY + 4, 0), 4, 1.5, 0.5, 1.5, 0.01);
                }

                // Impact when apex reaches ground
                if (pyramidY <= 0) {
                    impacted = true;
                    pyramidY = 0;
                    triggerImpactDamage(c);

                    // Ground crack impact effects
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.3f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 0.4f);
                    c.getWorld().spawnParticle(Particle.BLOCK, c, 80, 3, 0.5, 3, 0.5,
                            Material.DEEPSLATE.createBlockData());
                    DisplayBuilder.dustParticles(c, 50, 4.0, 180, 180, 190, 2.0f);

                    // Ground crack ring
                    DisplayBuilder.particleRing(c, 3.0, Particle.DUST, 24,
                            new Particle.DustOptions(Color.fromRGB(180, 100, 40), 2.0f));
                    DisplayBuilder.particleRing(c, 5.0, Particle.DUST, 32,
                            new Particle.DustOptions(Color.fromRGB(100, 100, 110), 1.5f));
                }
            }

            // Post-impact: structure crumbles
            if (impacted && ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 5, 2.0, 180, 100, 40, 0.6f);
            }
        }

        private void repositionPyramid(Location c, double[][] baseCorners, boolean inverted) {
            for (int edge = 0; edge < pyramidEdges.size(); edge++) {
                List<BlockDisplayHandle> edgeChains = pyramidEdges.get(edge);
                double[] from = baseCorners[edge];
                double[] to = baseCorners[(edge + 1) % 4];

                for (int seg = 0; seg < edgeChains.size(); seg++) {
                    double bx = from[0] + (to[0] - from[0]) * ((double) seg / 3);
                    double bz = from[2] + (to[2] - from[2]) * ((double) seg / 3);
                    double heightFactor = 1.0 - Math.abs(seg - 1.0) / 1.5;
                    double y;
                    if (inverted) {
                        // Inverted: base at top, point at bottom
                        y = pyramidY + 4.0 - heightFactor * 2.0;
                    } else {
                        y = pyramidY + heightFactor * 2.0;
                    }
                    edgeChains.get(seg).entity().teleport(c.clone().add(bx, y, bz));
                }
            }

            if (inverted) {
                apexBlock.entity().teleport(c.clone().add(0, pyramidY, 0));
            } else {
                apexBlock.entity().teleport(c.clone().add(0, pyramidY + 4, 0));
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            DisplayBuilder.dustParticles(impactLocation, 60, 5.0, 255, 180, 80, 2.5f);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainPyramid(plugin); }
    }

    // ================================================================
    // 15. COLLAPSING SPHERE — Sphere of chains contracting with glowing implosion
    // ================================================================
    public static class CollapsingSphere extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> sphereChains = new ArrayList<>();
        private float currentRadius = 4.0f;
        private boolean imploded = false;

        public CollapsingSphere(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("collapsing_sphere", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(220);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16 chains distributed in a sphere using golden angle
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 16; i++) {
                double y = 1 - (2.0 * i / 15.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double x = Math.cos(theta) * radiusAtY * currentRadius;
                double z = Math.sin(theta) * radiusAtY * currentRadius;
                double yPos = y * currentRadius;

                Material mat;
                if (i % 4 == 0) mat = Material.IRON_BLOCK;
                else if (i % 4 == 2) mat = Material.NETHERITE_BLOCK;
                else mat = Material.CHAIN;

                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, yPos + 3.0, z), mat);
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 180, 190).interpolation(2, 0);
                sphereChains.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
            DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 25, 4.0, 180, 180, 190, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (imploded) {
                // Post-implosion lingering effects
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 8, 2.0, 255, 200, 100, 0.8f);
                }
                return;
            }

            // Contract sphere: radius from 4 to 0.5 over 160 ticks
            if (currentRadius > 0.5f) {
                currentRadius -= 0.022f;
                if (currentRadius < 0.5f) currentRadius = 0.5f;
            }

            float compressionRatio = 1.0f - (currentRadius / 4.0f); // 0 -> 1

            // Reposition all chains
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < sphereChains.size(); i++) {
                double y = 1 - (2.0 * i / 15.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;

                // Add slow rotation
                double rotOffset = ticksAlive * 0.03;

                double x = Math.cos(theta + rotOffset) * radiusAtY * currentRadius;
                double z = Math.sin(theta + rotOffset) * radiusAtY * currentRadius;
                double yPos = y * currentRadius;

                sphereChains.get(i).entity().teleport(c.clone().add(x, yPos + 3.0, z));

                // Glow brighter as compressed: gray -> orange -> white
                int red = (int) (180 + compressionRatio * 75);
                int green = (int) (180 - compressionRatio * 30 + compressionRatio * compressionRatio * 75);
                int blue = (int) (190 - compressionRatio * 100);
                sphereChains.get(i).glow(Math.min(255, red), Math.min(255, Math.max(0, green)), Math.max(0, blue));
                sphereChains.get(i).interpolation(2, 0);

                // Scale increases slightly as chains compress (more dense)
                float scaleBoost = 0.5f + compressionRatio * 0.3f;
                sphereChains.get(i).scale(scaleBoost, scaleBoost, scaleBoost);
            }

            // Compression sounds escalating
            if (ticksAlive % 8 == 0) {
                float pitch = 0.4f + compressionRatio * 1.2f;
                DisplayBuilder.playSound(c.clone().add(0, 3, 0),
                        Sound.BLOCK_CHAIN_PLACE, 0.6f + compressionRatio * 0.4f, Math.min(pitch, 2.0f));
            }

            // Crackling energy particles as compression increases
            if (compressionRatio > 0.3f && ticksAlive % 4 == 0) {
                double pAngle = Math.random() * Math.PI * 2;
                double pElev = (Math.random() - 0.5) * 2;
                double pr = currentRadius + 0.5;
                Location pLoc = c.clone().add(Math.cos(pAngle) * pr, 3 + pElev, Math.sin(pAngle) * pr);
                DisplayBuilder.dustParticles(pLoc, 3, 0.3,
                        (int) (200 + compressionRatio * 55),
                        (int) (180 + compressionRatio * 50),
                        (int) (80 + compressionRatio * 100), 0.8f + compressionRatio * 0.5f);
            }

            // Inner core glow at high compression
            if (compressionRatio > 0.6f && ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 4, currentRadius * 0.5,
                        255, 255, (int) (200 * (1 - compressionRatio)), 1.0f + compressionRatio);
            }

            // Implosion trigger when fully compressed
            if (currentRadius <= 0.5f) {
                imploded = true;
                triggerImpactDamage(c.clone().add(0, 3, 0));

                // Massive implosion burst
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.2f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.5f);

                // Outward shockwave particle rings at multiple heights
                for (int h = 0; h < 5; h++) {
                    final int height = h;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        DisplayBuilder.particleRing(c.clone().add(0, 1 + height, 0), 2.0 + height * 1.5,
                                Particle.DUST, 24 + height * 8,
                                new Particle.DustOptions(Color.fromRGB(255, 220, 150), 2.0f));
                    }, height * 2L);
                }

                // Block crack particles
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 3, 0), 100, 3, 3, 3, 0.5,
                        Material.CHAIN.createBlockData());

                // Flash glow: scale all chains huge for 1 tick then remove via cleanup
                for (BlockDisplayHandle chain : sphereChains) {
                    chain.animateTo(
                            new Vector3f(0, 3, 0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(3.0f, 3.0f, 3.0f),
                            5);
                    chain.glow(255, 255, 200);
                }

                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 60, 5.0, 255, 255, 200, 2.5f);
            }
        }

        @Override
        protected void onImpact(Location impactLocation) {
            DisplayBuilder.dustParticles(impactLocation, 80, 6.0, 255, 200, 100, 3.0f);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CollapsingSphere(plugin); }
    }
}
