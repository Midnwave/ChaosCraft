package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain Mode Block Display -- WEAPONS AND TOOLS
 * 10 chain-themed weapon structures with metallic animations.
 * Materials: CHAIN, IRON_BLOCK, NETHERITE_BLOCK, DEEPSLATE, HEAVY_CORE, ANVIL, IRON_BARS
 * Rules:
 * - Min 10 BlockDisplays per attack, min 40 HP damage
 * - NO status effects -- damage only
 * - Always spawn straight (yaw=0, pitch=0)
 * - Phase 1, metallic sounds, full animations
 */
public final class WeaponsTools {

    private WeaponsTools() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ChainSword(plugin));
        registry.register(new ChainMace(plugin));
        registry.register(new ChainScythe(plugin));
        registry.register(new ChainSpear(plugin));
        registry.register(new ChainShield(plugin));
        registry.register(new ChainAxe(plugin));
        registry.register(new ChainTrident(plugin));
        registry.register(new ChainCrossbow(plugin));
        registry.register(new ChainWarHammer(plugin));
        registry.register(new ChainFlailStar(plugin));
    }

    // ================================================================
    // 1. CHAIN SWORD -- Massive greatsword with chain handle
    // Blade = 6 deepslate (tapered column), crossguard = 2 iron,
    // handle = 3 chain, pommel = 1 netherite (12 total)
    // Rises from ground, 180-degree sweeping slash, embeds in ground
    // ================================================================
    public static class ChainSword extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bladeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> crossguardBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();
        private BlockDisplayHandle pommel;
        private float riseProgress = 0;
        private boolean risen = false;
        private int slashCycle = 0;

        public ChainSword(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_sword", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(63.0);
            config.setDamageRadius(16.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(60);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Blade: 6 deepslate blocks in a tapered column rising upward
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, -2 + i * 1.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                float width = 1.35f - (i * 0.15f); // Taper from 1.35 to 0.6
                h.scale(width, 1.65f, 0.38f).glow(80, 80, 90).interpolation(2, 0);
                bladeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crossguard: 2 iron blocks extending perpendicular
            BlockDisplayHandle guardL = displayBuilder.spawnBlock(
                    center.clone().add(-1.2, -2.2, 0), Material.IRON_BLOCK);
            guardL.scale(1.8f, 0.5f, 0.5f).glow(180, 180, 190).interpolation(2, 0);
            crossguardBlocks.add(guardL);
            spawnedEntities.add(guardL.entity());

            BlockDisplayHandle guardR = displayBuilder.spawnBlock(
                    center.clone().add(1.2, -2.2, 0), Material.IRON_BLOCK);
            guardR.scale(1.8f, 0.5f, 0.5f).glow(180, 180, 190).interpolation(2, 0);
            crossguardBlocks.add(guardR);
            spawnedEntities.add(guardR.entity());

            // Handle: 3 chain blocks below crossguard
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, -3.0 - i * 0.9, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.6f, 1.2f, 0.6f).glow(120, 120, 130).interpolation(2, 0);
                handleBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Pommel: 1 netherite block at base
            pommel = displayBuilder.spawnBlock(center.clone().add(0, -5.8, 0), Material.NETHERITE_BLOCK);
            pommel.scale(0.75f, 0.75f, 0.75f).glow(40, 40, 50).interpolation(2, 0);
            spawnedEntities.add(pommel.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Rise from ground (first 40 ticks)
            if (ticksAlive < 40) {
                riseProgress = ticksAlive / 40.0f;
                float yOffset = riseProgress * 4.0f; // Rise 4 blocks
                teleportAllRelative(c, yOffset, 0);
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.7f, 0.6f);
                }
                return;
            }

            if (!risen) {
                risen = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
            }

            // Phase 2: Sweeping slash cycles
            float baseY = 4.0f;
            int cycleTick = (ticksAlive - 40) % 80;

            // 180-degree sweep over 30 ticks, then pause, then return
            float slashAngle = 0;
            if (cycleTick < 30) {
                // Forward sweep
                slashAngle = (cycleTick / 30.0f) * (float) Math.PI;
                if (cycleTick == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
                    slashCycle++;
                }
            } else if (cycleTick >= 50 && cycleTick < 80) {
                // Return sweep (slower)
                slashAngle = (float) Math.PI - ((cycleTick - 50) / 30.0f) * (float) Math.PI;
            }

            // Apply rotation to all parts (Y-axis sweep)
            for (BlockDisplayHandle h : bladeBlocks) {
                h.rotate(slashAngle, 0, 1, 0);
                h.interpolation(2, 0);
            }
            for (BlockDisplayHandle h : crossguardBlocks) {
                h.rotate(slashAngle, 0, 1, 0);
                h.interpolation(2, 0);
            }
            for (BlockDisplayHandle h : handleBlocks) {
                h.rotate(slashAngle, 0, 1, 0);
                h.interpolation(2, 0);
            }
            if (pommel != null) {
                pommel.rotate(slashAngle, 0, 1, 0);
                pommel.interpolation(2, 0);
            }

            // Sweep attack particles during slash
            if (cycleTick < 30 && ticksAlive % 2 == 0) {
                for (int i = 0; i < bladeBlocks.size(); i += 2) {
                    Location bladeLoc = bladeBlocks.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, bladeLoc, 1, 0.3, 0.3, 0.3, 0);
                }
            }

            // Metallic sparks from blade edge
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < Math.min(bladeBlocks.size(), 6); i += 2) {
                    Location edgeLoc = bladeBlocks.get(i).entity().getLocation();
                    DisplayBuilder.dustParticles(edgeLoc, 3, 0.2, 200, 200, 210, 0.8f);
                }
            }

            // Chain rattle sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.8f);
            }
        }

        private void teleportAllRelative(Location center, float yOffset, float rotAngle) {
            for (int i = 0; i < bladeBlocks.size(); i++) {
                Location loc = center.clone().add(0, -2 + i * 1.2 + yOffset, 0);
                bladeBlocks.get(i).entity().teleport(loc);
            }
            for (int i = 0; i < crossguardBlocks.size(); i++) {
                double xOff = (i == 0) ? -1.2 : 1.2;
                crossguardBlocks.get(i).entity().teleport(center.clone().add(xOff, -2.2 + yOffset, 0));
            }
            for (int i = 0; i < handleBlocks.size(); i++) {
                handleBlocks.get(i).entity().teleport(center.clone().add(0, -3.0 - i * 0.9 + yOffset, 0));
            }
            if (pommel != null) {
                pommel.entity().teleport(center.clone().add(0, -5.8 + yOffset, 0));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainSword(plugin); }
    }

    // ================================================================
    // 2. CHAIN MACE -- Shaft = 4 chains, head = 8 iron sphere cluster
    // Rises up, slams down with shockwave ring. 3 slams with decreasing intervals.
    // ================================================================
    public static class ChainMace extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private int slamCount = 0;
        private float headBaseY = 6.0f;
        private static final int[] SLAM_INTERVALS = {100, 70, 50};

        public ChainMace(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_mace", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(72.0);
            config.setDamageRadius(18.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(50);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Shaft: 4 chain blocks
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 1.0 + i * 1.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.5f, 1.35f, 0.5f).glow(120, 120, 130).interpolation(2, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Head: 8 iron blocks in sphere cluster
            double[][] headOffsets = {
                {0, 0, 0}, {0.7, 0, 0}, {-0.7, 0, 0}, {0, 0, 0.7},
                {0, 0, -0.7}, {0, 0.7, 0}, {0.5, 0.5, 0.5}, {-0.5, 0.5, -0.5}
            };
            for (double[] off : headOffsets) {
                Location loc = center.clone().add(off[0], headBaseY + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(1.2f, 1.2f, 1.2f).glow(190, 190, 200).interpolation(2, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Determine which slam we are on
            int interval = SLAM_INTERVALS[Math.min(slamCount, SLAM_INTERVALS.length - 1)];
            int cycleTick = ticksAlive % interval;

            // Rise phase: first half of interval, head rises
            float headY = headBaseY;
            if (cycleTick < interval / 2) {
                // Rising higher
                float riseFrac = cycleTick / (float) (interval / 2);
                headY = headBaseY + riseFrac * 4.0f;
            } else {
                // Slam down
                int slamTick = cycleTick - interval / 2;
                int slamDuration = interval / 4;
                if (slamTick < slamDuration) {
                    float slamFrac = slamTick / (float) slamDuration;
                    headY = headBaseY + 4.0f - slamFrac * (4.0f + headBaseY - 0.5f);
                    // Impact at bottom
                    if (slamTick == slamDuration - 1) {
                        slamCount++;
                        DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.5f);
                        DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.6f);
                        // Shockwave ring
                        Location impact = c.clone().add(0, 0.5, 0);
                        DisplayBuilder.particleRing(impact, 5.0, Particle.DUST, 24,
                                new Particle.DustOptions(Color.fromRGB(200, 200, 210), 2.0f));
                        c.getWorld().spawnParticle(Particle.EXPLOSION, impact, 3, 1.0, 0.3, 1.0, 0);
                        triggerImpactDamage(impact);
                    }
                } else {
                    // Post-slam rest at ground level
                    headY = 0.5f + ((cycleTick - interval / 2 - slamDuration) / (float) (interval - interval / 2 - slamDuration)) * (headBaseY - 0.5f);
                }
            }

            // Teleport head blocks to new Y
            double[][] headOffsets = {
                {0, 0, 0}, {0.7, 0, 0}, {-0.7, 0, 0}, {0, 0, 0.7},
                {0, 0, -0.7}, {0, 0.7, 0}, {0.5, 0.5, 0.5}, {-0.5, 0.5, -0.5}
            };
            for (int i = 0; i < headBlocks.size(); i++) {
                Location loc = c.clone().add(headOffsets[i][0], headY + headOffsets[i][1], headOffsets[i][2]);
                headBlocks.get(i).entity().teleport(loc);
                headBlocks.get(i).interpolation(2, 0);
            }

            // Shaft stretches between base and head
            for (int i = 0; i < shaftBlocks.size(); i++) {
                float t = (i + 1) / (float) (shaftBlocks.size() + 1);
                float shaftY = 0.5f + t * (headY - 0.5f);
                shaftBlocks.get(i).entity().teleport(c.clone().add(0, shaftY, 0));
                shaftBlocks.get(i).interpolation(2, 0);
            }

            // Iron sparks from head
            if (ticksAlive % 3 == 0) {
                Location headLoc = c.clone().add(0, headY, 0);
                DisplayBuilder.dustParticles(headLoc, 4, 0.8, 200, 200, 220, 1.0f);
            }

            // Chain rattle during movement
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainMace(plugin); }
    }

    // ================================================================
    // 3. CHAIN SCYTHE -- Curved blade = 5 iron arc, handle = 5 chain,
    // grip = 2 deepslate (12 total). 270-degree horizontal arc sweep.
    // ================================================================
    public static class ChainScythe extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bladeArc = new ArrayList<>();
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> gripBlocks = new ArrayList<>();
        private float sweepAngle = 0;

        public ChainScythe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_scythe", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(18.0);
            config.setDurationTicks(380);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(50);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Curved blade: 5 iron blocks in an arc shape at chest height
            for (int i = 0; i < 5; i++) {
                double angle = (i / 4.0) * Math.PI * 0.7; // ~126-degree arc for the blade curve
                double x = Math.cos(angle) * 2.5;
                double y = Math.sin(angle) * 1.5 + 1.5; // Chest height
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                float taper = 1.05f - (Math.abs(i - 2) * 0.15f);
                h.scale(taper, 0.5f, 0.3f).glow(200, 200, 210).interpolation(2, 0);
                bladeArc.add(h);
                spawnedEntities.add(h.entity());
            }

            // Handle: 5 chain blocks descending from blade base
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 1.2 - i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.45f, 1.05f, 0.45f).glow(120, 120, 130).interpolation(2, 0);
                handleBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Grip: 2 deepslate blocks at base
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, -2.8 - i * 0.7, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(0.68f, 0.9f, 0.68f).glow(70, 70, 80).interpolation(2, 0);
                gripBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // 270-degree sweep over 60 ticks, then 40 tick pause
            int cycleTick = ticksAlive % 100;
            if (cycleTick < 60) {
                sweepAngle = (cycleTick / 60.0f) * (float)(Math.PI * 1.5); // 270 degrees
                if (cycleTick == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.5f);
                }
                if (cycleTick == 30) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.7f);
                }
            }
            // During pause: hold at final angle then reset
            if (cycleTick >= 60 && cycleTick < 90) {
                sweepAngle = (float)(Math.PI * 1.5);
            }
            if (cycleTick >= 90) {
                float resetProgress = (cycleTick - 90) / 10.0f;
                sweepAngle = (float)(Math.PI * 1.5) * (1.0f - resetProgress);
            }

            // Apply Y-axis rotation to all parts
            for (BlockDisplayHandle h : bladeArc) {
                h.rotate(sweepAngle, 0, 1, 0);
                h.interpolation(2, 0);
            }
            for (BlockDisplayHandle h : handleBlocks) {
                h.rotate(sweepAngle, 0, 1, 0);
                h.interpolation(2, 0);
            }
            for (BlockDisplayHandle h : gripBlocks) {
                h.rotate(sweepAngle, 0, 1, 0);
                h.interpolation(2, 0);
            }

            // Sweep particles during active sweep
            if (cycleTick < 60 && ticksAlive % 2 == 0) {
                for (int i = 0; i < bladeArc.size(); i += 2) {
                    Location bladeLoc = bladeArc.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, bladeLoc, 2, 0.2, 0.2, 0.2, 0);
                }
            }

            // Metallic dust trail
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle h : bladeArc) {
                    DisplayBuilder.dustParticles(h.entity().getLocation(), 2, 0.15, 190, 190, 200, 0.7f);
                }
            }

            // Chain ambient
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainScythe(plugin); }
    }

    // ================================================================
    // 4. CHAIN SPEAR -- Tip = 2 netherite (tapered), shaft = 8 chain (10 total)
    // Launches horizontally 15 blocks, retracts. Repeats in different directions.
    // ================================================================
    public static class ChainSpear extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tipBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private int launchDirection = 0; // 0=north, 1=east, 2=south, 3=west
        private float travelProgress = 0;
        private boolean retracting = false;
        private static final double TRAVEL_DIST = 15.0;

        public ChainSpear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_spear", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(69.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(450);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(40);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Tip: 2 netherite blocks with tapering scale
            BlockDisplayHandle tipFront = displayBuilder.spawnBlock(center.clone().add(0, 1.5, -1.2), Material.NETHERITE_BLOCK);
            tipFront.scale(0.45f, 0.45f, 1.2f).glow(40, 40, 50).interpolation(2, 0);
            tipBlocks.add(tipFront);
            spawnedEntities.add(tipFront.entity());

            BlockDisplayHandle tipRear = displayBuilder.spawnBlock(center.clone().add(0, 1.5, -0.4), Material.NETHERITE_BLOCK);
            tipRear.scale(0.75f, 0.75f, 0.9f).glow(40, 40, 50).interpolation(2, 0);
            tipBlocks.add(tipRear);
            spawnedEntities.add(tipRear.entity());

            // Shaft: 8 chain blocks extending behind
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, 1.5, 0.2 + i * 0.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.45f, 0.45f, 1.05f).glow(120, 120, 130).interpolation(2, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Each launch cycle: 80 ticks total
            int cycleTick = ticksAlive % 80;

            if (cycleTick == 0) {
                retracting = false;
                travelProgress = 0;
                launchDirection = (ticksAlive / 80) % 4;
                DisplayBuilder.playSound(c, Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.8f, 1.2f);
            }

            // Direction vectors for 4 compass directions
            double[] dirX = {0, 1, 0, -1};
            double[] dirZ = {-1, 0, 1, 0};
            double dx = dirX[launchDirection];
            double dz = dirZ[launchDirection];

            if (cycleTick < 25) {
                // Launch forward: 25 ticks to travel 15 blocks
                travelProgress = (cycleTick / 25.0f) * (float) TRAVEL_DIST;
            } else if (cycleTick < 40) {
                // Hold at max
                travelProgress = (float) TRAVEL_DIST;
            } else if (cycleTick < 65) {
                // Retract: 25 ticks to return
                float retractFrac = (cycleTick - 40) / 25.0f;
                travelProgress = (float) TRAVEL_DIST * (1.0f - retractFrac);
                if (cycleTick == 40) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.8f, 0.6f);
                }
            } else {
                // Rest at origin
                travelProgress = 0;
            }

            // Rotate the entire spear to face the launch direction
            float yawAngle = launchDirection * (float)(Math.PI / 2.0);

            // Move tip blocks
            for (int i = 0; i < tipBlocks.size(); i++) {
                double baseZ = (i == 0) ? -1.2 : -0.4;
                Location loc = c.clone().add(dx * travelProgress, 1.5, dz * travelProgress);
                loc.add(dx * baseZ, 0, dz * baseZ);
                tipBlocks.get(i).entity().teleport(loc);
                tipBlocks.get(i).rotate(yawAngle, 0, 1, 0);
                tipBlocks.get(i).interpolation(2, 0);
            }

            // Move shaft blocks
            for (int i = 0; i < shaftBlocks.size(); i++) {
                double baseOffset = 0.2 + i * 0.8;
                // Shaft trails behind during travel, stretching between origin and tip
                float shaftTravel = Math.max(0, travelProgress - (shaftBlocks.size() - i) * 0.5f);
                Location loc = c.clone().add(dx * shaftTravel, 1.5, dz * shaftTravel);
                loc.add(-dx * baseOffset * 0.3, 0, -dz * baseOffset * 0.3);
                shaftBlocks.get(i).entity().teleport(loc);
                shaftBlocks.get(i).rotate(yawAngle, 0, 1, 0);
                shaftBlocks.get(i).interpolation(2, 0);
            }

            // Piercing whistle sound during travel
            if (cycleTick < 25 && cycleTick % 5 == 0) {
                DisplayBuilder.playSound(c.clone().add(dx * travelProgress, 1.5, dz * travelProgress),
                        Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.4f, 1.8f);
            }

            // Particles at tip during flight
            if (cycleTick < 40 && ticksAlive % 2 == 0 && !tipBlocks.isEmpty()) {
                Location tipLoc = tipBlocks.get(0).entity().getLocation();
                c.getWorld().spawnParticle(Particle.CRIT, tipLoc, 4, 0.1, 0.1, 0.1, 0.2);
                DisplayBuilder.dustParticles(tipLoc, 2, 0.1, 160, 160, 170, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainSpear(plugin); }
    }

    // ================================================================
    // 5. CHAIN SHIELD -- 12 iron blocks in 4x3 wall + 4 chain border (16 total)
    // Advances forward slowly, pushing damage zone. Crushing damage against walls.
    // ================================================================
    public static class ChainShield extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> borderChains = new ArrayList<>();
        private float advanceProgress = 0;
        private int advanceDirection = 0;

        public ChainShield(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_shield", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4x3 iron block wall
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 4; col++) {
                    double x = (col - 1.5) * 1.1;
                    double y = row * 1.1 + 0.5;
                    Location loc = center.clone().add(x, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(1.5f, 1.5f, 0.6f).glow(190, 190, 200).interpolation(2, 0);
                    wallBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Chain border: top edge
            BlockDisplayHandle topChain = displayBuilder.spawnBlock(
                    center.clone().add(0, 3.8, 0), Material.CHAIN);
            topChain.scale(7.5f, 0.45f, 0.45f).glow(120, 120, 130).interpolation(2, 0);
            borderChains.add(topChain);
            spawnedEntities.add(topChain.entity());

            // Bottom edge
            BlockDisplayHandle bottomChain = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.2, 0), Material.CHAIN);
            bottomChain.scale(7.5f, 0.45f, 0.45f).glow(120, 120, 130).interpolation(2, 0);
            borderChains.add(bottomChain);
            spawnedEntities.add(bottomChain.entity());

            // Left edge
            BlockDisplayHandle leftChain = displayBuilder.spawnBlock(
                    center.clone().add(-2.3, 2.0, 0), Material.CHAIN);
            leftChain.scale(0.45f, 5.7f, 0.45f).glow(120, 120, 130).interpolation(2, 0);
            borderChains.add(leftChain);
            spawnedEntities.add(leftChain.entity());

            // Right edge
            BlockDisplayHandle rightChain = displayBuilder.spawnBlock(
                    center.clone().add(2.3, 2.0, 0), Material.CHAIN);
            rightChain.scale(0.45f, 5.7f, 0.45f).glow(120, 120, 130).interpolation(2, 0);
            borderChains.add(rightChain);
            spawnedEntities.add(rightChain.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Advance cycle: 120 ticks forward, 30 ticks pause, change direction
            int cycleTick = ticksAlive % 150;

            if (cycleTick == 0) {
                advanceDirection = (ticksAlive / 150) % 4;
                advanceProgress = 0;
            }

            double[] dirX = {0, 1, 0, -1};
            double[] dirZ = {-1, 0, 1, 0};
            double dx = dirX[advanceDirection];
            double dz = dirZ[advanceDirection];

            if (cycleTick < 120) {
                // Slow advance: 10 blocks over 120 ticks
                advanceProgress = (cycleTick / 120.0f) * 10.0f;
            }
            // Pause during last 30 ticks

            float yawAngle = advanceDirection * (float)(Math.PI / 2.0);

            // Move wall blocks
            for (int idx = 0; idx < wallBlocks.size(); idx++) {
                int row = idx / 4;
                int col = idx % 4;
                double x = (col - 1.5) * 1.1;
                double y = row * 1.1 + 0.5;
                Location loc = c.clone().add(dx * advanceProgress + x * Math.cos(yawAngle),
                        y, dz * advanceProgress + x * Math.sin(yawAngle));
                wallBlocks.get(idx).entity().teleport(loc);
                wallBlocks.get(idx).rotate(yawAngle, 0, 1, 0);
                wallBlocks.get(idx).interpolation(2, 0);
            }

            // Move border chains
            double[][] chainOffsets = {
                {0, 3.8, 0}, {0, 0.2, 0}, {-2.3, 2.0, 0}, {2.3, 2.0, 0}
            };
            for (int i = 0; i < borderChains.size(); i++) {
                double ox = chainOffsets[i][0];
                double oy = chainOffsets[i][1];
                Location loc = c.clone().add(
                        dx * advanceProgress + ox * Math.cos(yawAngle),
                        oy,
                        dz * advanceProgress + ox * Math.sin(yawAngle));
                borderChains.get(i).entity().teleport(loc);
                borderChains.get(i).rotate(yawAngle, 0, 1, 0);
                borderChains.get(i).interpolation(2, 0);
            }

            // Grinding sound during advance
            if (cycleTick < 120 && ticksAlive % 20 == 0) {
                Location frontLoc = c.clone().add(dx * advanceProgress, 1.5, dz * advanceProgress);
                DisplayBuilder.playSound(frontLoc, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 0.4f);
            }

            // Sparks at bottom edge during advance
            if (cycleTick < 120 && ticksAlive % 3 == 0) {
                Location bottomLoc = c.clone().add(dx * advanceProgress, 0.3, dz * advanceProgress);
                c.getWorld().spawnParticle(Particle.LAVA, bottomLoc, 2, 1.5, 0.1, 0.3, 0);
                DisplayBuilder.dustParticles(bottomLoc, 3, 1.5, 200, 200, 210, 0.8f);
            }

            // Chain rattle
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c.clone().add(dx * advanceProgress, 1.5, dz * advanceProgress),
                        Sound.BLOCK_CHAIN_STEP, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainShield(plugin); }
    }

    // ================================================================
    // 6. CHAIN AXE -- Double-headed blade = 6 iron (3/side), handle = 5 chain (11 total)
    // Spins end-over-end like a thrown axe, arcs through the air. Each rotation = damage.
    // ================================================================
    public static class ChainAxe extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bladeLeft = new ArrayList<>();
        private final List<BlockDisplayHandle> bladeRight = new ArrayList<>();
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();
        private float spinAngle = 0;
        private float arcAngle = 0;

        public ChainAxe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_axe", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(14.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(30);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left blade: 3 iron blocks forming axe head
            double[][] leftOffsets = {{-0.8, 0.5, 0}, {-1.2, 0, 0}, {-0.8, -0.5, 0}};
            for (double[] off : leftOffsets) {
                Location loc = center.clone().add(off[0], 3 + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(1.2f, 0.9f, 0.38f).glow(200, 200, 210).interpolation(2, 0);
                bladeLeft.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right blade: 3 iron blocks (mirror)
            double[][] rightOffsets = {{0.8, 0.5, 0}, {1.2, 0, 0}, {0.8, -0.5, 0}};
            for (double[] off : rightOffsets) {
                Location loc = center.clone().add(off[0], 3 + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(1.2f, 0.9f, 0.38f).glow(200, 200, 210).interpolation(2, 0);
                bladeRight.add(h);
                spawnedEntities.add(h.entity());
            }

            // Handle: 5 chain blocks
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 3 - 0.8 - i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.45f, 1.05f, 0.45f).glow(120, 120, 130).interpolation(2, 0);
                handleBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // End-over-end spin: Z-axis rotation
            spinAngle = ticksAlive * 0.15f; // Fast spin

            // Arc through the air: circular path around the center
            arcAngle = ticksAlive * 0.04f; // Slower arc
            double arcRadius = 5.0;
            double arcX = Math.cos(arcAngle) * arcRadius;
            double arcZ = Math.sin(arcAngle) * arcRadius;
            double arcY = 3.0 + Math.sin(arcAngle * 2) * 2.0; // Undulating height

            Location arcCenter = c.clone().add(arcX, arcY, arcZ);

            // Teleport and rotate all blade/handle parts relative to arc position
            // Left blade
            for (int i = 0; i < bladeLeft.size(); i++) {
                double[][] offsets = {{-0.8, 0.5, 0}, {-1.2, 0, 0}, {-0.8, -0.5, 0}};
                // Apply spin rotation to the offset
                double oY = offsets[i][1] * Math.cos(spinAngle) - offsets[i][0] * Math.sin(spinAngle);
                double oX = offsets[i][1] * Math.sin(spinAngle) + offsets[i][0] * Math.cos(spinAngle);
                Location loc = arcCenter.clone().add(oX, oY, offsets[i][2]);
                bladeLeft.get(i).entity().teleport(loc);
                bladeLeft.get(i).rotate(spinAngle, 0, 0, 1);
                bladeLeft.get(i).interpolation(2, 0);
            }

            // Right blade
            for (int i = 0; i < bladeRight.size(); i++) {
                double[][] offsets = {{0.8, 0.5, 0}, {1.2, 0, 0}, {0.8, -0.5, 0}};
                double oY = offsets[i][1] * Math.cos(spinAngle) - offsets[i][0] * Math.sin(spinAngle);
                double oX = offsets[i][1] * Math.sin(spinAngle) + offsets[i][0] * Math.cos(spinAngle);
                Location loc = arcCenter.clone().add(oX, oY, offsets[i][2]);
                bladeRight.get(i).entity().teleport(loc);
                bladeRight.get(i).rotate(spinAngle, 0, 0, 1);
                bladeRight.get(i).interpolation(2, 0);
            }

            // Handle
            for (int i = 0; i < handleBlocks.size(); i++) {
                double baseY = -0.8 - i * 0.8;
                double oY = baseY * Math.cos(spinAngle);
                double oX = baseY * Math.sin(spinAngle);
                Location loc = arcCenter.clone().add(oX, oY, 0);
                handleBlocks.get(i).entity().teleport(loc);
                handleBlocks.get(i).rotate(spinAngle, 0, 0, 1);
                handleBlocks.get(i).interpolation(2, 0);
            }

            // Whistling spin sound
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(arcCenter, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.2f);
            }

            // Sparks from blade edges
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, arcCenter, 5, 0.5, 0.5, 0.5, 0.2);
                DisplayBuilder.dustParticles(arcCenter, 3, 0.4, 200, 200, 220, 0.6f);
            }

            // Chain rattle
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(arcCenter, Sound.BLOCK_CHAIN_STEP, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainAxe(plugin); }
    }

    // ================================================================
    // 7. CHAIN TRIDENT -- 3 prongs (2 chain each), shaft = 6 chain,
    // base = 1 iron (13 total). Hovers, aims, launches. END_ROD particles. 3 launches.
    // ================================================================
    public static class ChainTrident extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> prongBlocks = new ArrayList<>(); // 6 total (3 prongs x 2)
        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private BlockDisplayHandle baseBlock;
        private int launchCount = 0;
        private int launchDir = 0;
        private float travelProgress = 0;

        public ChainTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_trident", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(69.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(420);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(45);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3 prongs, each 2 chain blocks, angled outward
            double[] prongXOff = {-0.5, 0, 0.5};
            for (int p = 0; p < 3; p++) {
                for (int seg = 0; seg < 2; seg++) {
                    double spread = prongXOff[p] * (1.0 + seg * 0.3);
                    Location loc = center.clone().add(spread, 3.0 + seg * 0.9, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    float taper = 0.5f - seg * 0.12f;
                    h.scale(taper, 1.2f, 0.38f).glow(120, 120, 140).interpolation(2, 0);
                    prongBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Shaft: 6 chain blocks descending
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, 2.5 - i * 0.7, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.45f, 0.9f, 0.45f).glow(120, 120, 130).interpolation(2, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Base: 1 iron block
            baseBlock = displayBuilder.spawnBlock(center.clone().add(0, -1.7, 0), Material.IRON_BLOCK);
            baseBlock.scale(0.6f, 0.6f, 0.6f).glow(190, 190, 200).interpolation(2, 0);
            spawnedEntities.add(baseBlock.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Launch cycle: 30 ticks hover/aim, 20 ticks launch, 20 ticks hold, 20 ticks retract, 30 ticks rest
            int cycleTick = ticksAlive % 120;

            if (cycleTick == 0) {
                launchDir = (ticksAlive / 120) % 4;
                travelProgress = 0;
                launchCount++;
            }

            double[] dirX = {0, 1, 0, -1};
            double[] dirZ = {-1, 0, 1, 0};
            double dx = dirX[launchDir];
            double dz = dirZ[launchDir];
            float yawAngle = launchDir * (float)(Math.PI / 2.0);

            // Hover/aim: bob gently
            float hoverY = 0;
            if (cycleTick < 30) {
                hoverY = (float) Math.sin(cycleTick * 0.2) * 0.3f;
                // Aim rotation toward launch direction
            } else if (cycleTick < 50) {
                // Launch forward 12 blocks
                float launchFrac = (cycleTick - 30) / 20.0f;
                travelProgress = launchFrac * 12.0f;
                if (cycleTick == 30) {
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.2f, 0.7f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.8f, 1.5f);
                }
            } else if (cycleTick < 70) {
                // Hold at max distance
                travelProgress = 12.0f;
            } else if (cycleTick < 90) {
                // Retract
                float retractFrac = (cycleTick - 70) / 20.0f;
                travelProgress = 12.0f * (1.0f - retractFrac);
                if (cycleTick == 70) {
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RETURN, 0.8f, 0.8f);
                }
            } else {
                travelProgress = 0;
            }

            // Move all trident parts
            // Prongs
            double[] prongXOff = {-0.5, 0, 0.5};
            for (int p = 0; p < 3; p++) {
                for (int seg = 0; seg < 2; seg++) {
                    int idx = p * 2 + seg;
                    if (idx >= prongBlocks.size()) break;
                    double spread = prongXOff[p] * (1.0 + seg * 0.3);
                    Location loc = c.clone().add(
                            dx * travelProgress + spread * Math.cos(yawAngle),
                            3.0 + seg * 0.9 + hoverY,
                            dz * travelProgress + spread * Math.sin(yawAngle));
                    prongBlocks.get(idx).entity().teleport(loc);
                    prongBlocks.get(idx).rotate(yawAngle, 0, 1, 0);
                    prongBlocks.get(idx).interpolation(2, 0);
                }
            }

            // Shaft
            for (int i = 0; i < shaftBlocks.size(); i++) {
                float shaftTravel = Math.max(0, travelProgress - (shaftBlocks.size() - i) * 0.3f);
                Location loc = c.clone().add(
                        dx * shaftTravel, 2.5 - i * 0.7 + hoverY, dz * shaftTravel);
                shaftBlocks.get(i).entity().teleport(loc);
                shaftBlocks.get(i).rotate(yawAngle, 0, 1, 0);
                shaftBlocks.get(i).interpolation(2, 0);
            }

            // Base
            if (baseBlock != null) {
                float baseTravel = Math.max(0, travelProgress - shaftBlocks.size() * 0.3f);
                baseBlock.entity().teleport(c.clone().add(
                        dx * baseTravel, -1.7 + hoverY, dz * baseTravel));
                baseBlock.rotate(yawAngle, 0, 1, 0);
                baseBlock.interpolation(2, 0);
            }

            // END_ROD particles along prongs during flight
            if (cycleTick >= 30 && cycleTick < 70 && ticksAlive % 2 == 0) {
                for (int p = 0; p < 3; p++) {
                    int idx = p * 2 + 1; // Tip of each prong
                    if (idx < prongBlocks.size()) {
                        Location tipLoc = prongBlocks.get(idx).entity().getLocation();
                        c.getWorld().spawnParticle(Particle.END_ROD, tipLoc, 3, 0.05, 0.1, 0.05, 0.02);
                    }
                }
            }

            // Electrifying particles along shaft
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < shaftBlocks.size(); i += 2) {
                    Location shaftLoc = shaftBlocks.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.END_ROD, shaftLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Impact damage at max travel
            if (cycleTick == 50) {
                Location impactLoc = c.clone().add(dx * 12, 2.0, dz * 12);
                triggerImpactDamage(impactLoc);
                c.getWorld().spawnParticle(Particle.FLASH, impactLoc, 1, 0, 0, 0, 0);
                DisplayBuilder.playSound(impactLoc, Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainTrident(plugin); }
    }

    // ================================================================
    // 8. CHAIN CROSSBOW -- Arms = 2 chain beams (3 each), stock = 4 iron,
    // string = particle line (10 blocks total). Fires 4 chain bolts (2 chain each).
    // ================================================================
    public static class ChainCrossbow extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftArm = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArm = new ArrayList<>();
        private final List<BlockDisplayHandle> stockBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> bolts = new ArrayList<>();
        private int boltsFired = 0;
        private final float[] boltProgress = new float[4];

        public ChainCrossbow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_crossbow", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(63.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(30);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left arm: 3 chain blocks horizontal
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-0.5 - i * 0.8, 2.0 + i * 0.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                float taper = 0.5f - i * 0.08f;
                h.scale(1.05f, taper, 0.3f).glow(120, 120, 130).interpolation(2, 0);
                leftArm.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right arm: 3 chain blocks horizontal (mirror)
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0.5 + i * 0.8, 2.0 + i * 0.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                float taper = 0.5f - i * 0.08f;
                h.scale(1.05f, taper, 0.3f).glow(120, 120, 130).interpolation(2, 0);
                rightArm.add(h);
                spawnedEntities.add(h.entity());
            }

            // Stock: 4 iron blocks in a column behind
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 1.5, 0.5 + i * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.6f, 0.75f, 0.9f).glow(190, 190, 200).interpolation(2, 0);
                stockBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Pre-create 4 bolt pairs (hidden initially below ground)
            for (int b = 0; b < 4; b++) {
                List<BlockDisplayHandle> bolt = new ArrayList<>();
                for (int seg = 0; seg < 2; seg++) {
                    Location loc = center.clone().add(0, -10, 0); // Hidden below
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0.3f, 0.3f, 0.9f).glow(150, 150, 160).interpolation(2, 0);
                    bolt.add(h);
                    spawnedEntities.add(h.entity());
                }
                bolts.add(bolt);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Crossbow body: gentle bobbing
            float bobY = (float) Math.sin(ticksAlive * 0.08) * 0.15f;

            // Slow Y rotation
            float baseRot = ticksAlive * 0.005f;

            // Rotate/bob body parts
            for (BlockDisplayHandle h : leftArm) {
                h.rotate(baseRot, 0, 1, 0);
                h.interpolation(2, 0);
            }
            for (BlockDisplayHandle h : rightArm) {
                h.rotate(baseRot, 0, 1, 0);
                h.interpolation(2, 0);
            }
            for (BlockDisplayHandle h : stockBlocks) {
                h.rotate(baseRot, 0, 1, 0);
                h.interpolation(2, 0);
            }

            // String particle line between arm tips
            if (ticksAlive % 3 == 0) {
                Location leftTip = leftArm.get(leftArm.size() - 1).entity().getLocation();
                Location rightTip = rightArm.get(rightArm.size() - 1).entity().getLocation();
                DisplayBuilder.particleLine(leftTip, rightTip, Particle.DUST, 3,
                        new Particle.DustOptions(Color.fromRGB(180, 180, 190), 0.5f));
            }

            // Fire bolts in sequence: bolt every 60 ticks
            int boltCycle = ticksAlive % 60;
            int currentBolt = (ticksAlive / 60) % 4;

            if (boltCycle == 0 && currentBolt < 4) {
                boltsFired = Math.max(boltsFired, currentBolt + 1);
                boltProgress[currentBolt] = 0;
                DisplayBuilder.playSound(c, Sound.ITEM_CROSSBOW_SHOOT, 1.2f, 0.8f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.6f, 1.2f);
            }

            // Animate fired bolts
            for (int b = 0; b < boltsFired && b < 4; b++) {
                int boltAge = ticksAlive - (b * 60);
                if (boltAge < 0) continue;

                int boltTick = boltAge % 60;
                if (boltTick < 30) {
                    boltProgress[b] = (boltTick / 30.0f) * 20.0f; // Travel 20 blocks
                } else {
                    boltProgress[b] = -100; // Hidden
                }

                if (boltProgress[b] >= 0 && b < bolts.size()) {
                    // Bolt fires in the direction crossbow is facing
                    float boltAngle = baseRot + b * 0.3f; // Slight spread
                    double bx = Math.sin(boltAngle) * boltProgress[b];
                    double bz = -Math.cos(boltAngle) * boltProgress[b];
                    for (int seg = 0; seg < bolts.get(b).size(); seg++) {
                        Location loc = c.clone().add(bx, 2.0 + bobY, bz - seg * 0.5);
                        bolts.get(b).get(seg).entity().teleport(loc);
                        bolts.get(b).get(seg).rotate(boltAngle, 0, 1, 0);
                        bolts.get(b).get(seg).interpolation(2, 0);
                    }
                    // Bolt trail particles
                    if (boltTick % 2 == 0) {
                        Location boltLoc = c.clone().add(bx, 2.0, bz);
                        c.getWorld().spawnParticle(Particle.CRIT, boltLoc, 3, 0.05, 0.05, 0.05, 0.1);
                    }
                } else if (b < bolts.size()) {
                    // Hide bolt underground
                    for (BlockDisplayHandle seg : bolts.get(b)) {
                        seg.entity().teleport(c.clone().add(0, -10, 0));
                    }
                }

                // Impact damage when bolt reaches max range
                if (boltTick == 29 && boltProgress[b] > 0) {
                    float hitAngle = baseRot + b * 0.3f;
                    double hx = Math.sin(hitAngle) * 20.0;
                    double hz = -Math.cos(hitAngle) * 20.0;
                    Location impactLoc = c.clone().add(hx, 2.0, hz);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.playSound(impactLoc, Sound.ITEM_CROSSBOW_HIT, 1.0f, 0.7f);
                }
            }

            // Chain ambient sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainCrossbow(plugin); }
    }

    // ================================================================
    // 9. CHAIN WAR HAMMER -- Rectangular head = 6 iron (3x2), spike = 1 netherite,
    // handle = 5 chain (12 total). Overhead swing arcs down with crater impact.
    // ================================================================
    public static class ChainWarHammer extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private BlockDisplayHandle spikeBlock;
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();
        private float swingAngle = 0;
        private boolean impactThisCycle = false;

        public ChainWarHammer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_war_hammer", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(75.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(420);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(75.0);
            config.setImpactRadius(16.0);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Rectangular head: 3x2 = 6 iron blocks
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 3; col++) {
                    double x = (col - 1) * 0.9;
                    double y = 6.0 + row * 0.8;
                    Location loc = center.clone().add(x, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(1.2f, 1.05f, 0.9f).glow(190, 190, 200).interpolation(2, 0);
                    headBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Spike on back of head: 1 netherite
            spikeBlock = displayBuilder.spawnBlock(center.clone().add(0, 6.4, 0.5), Material.NETHERITE_BLOCK);
            spikeBlock.scale(0.45f, 0.45f, 1.2f).glow(40, 40, 50).interpolation(2, 0);
            spawnedEntities.add(spikeBlock.entity());

            // Handle: 5 chain blocks below head
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 5.2 - i * 0.9, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.5f, 1.2f, 0.5f).glow(120, 120, 130).interpolation(2, 0);
                handleBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Overhead swing cycle: 90 ticks
            // 0-40: Rise up (swing back)
            // 40-55: Fast forward arc down (the swing)
            // 55-60: Impact + hold
            // 60-90: Slow recover back up
            int cycleTick = ticksAlive % 90;

            if (cycleTick == 0) {
                impactThisCycle = false;
            }

            if (cycleTick < 40) {
                // Rise up and tilt back: X rotation from 0 to -PI/2
                swingAngle = -(cycleTick / 40.0f) * (float)(Math.PI / 2.0);
                if (cycleTick == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.7f, 0.5f);
                }
            } else if (cycleTick < 55) {
                // Fast forward swing: X rotation from -PI/2 to PI/2
                float swingFrac = (cycleTick - 40) / 15.0f;
                swingAngle = -(float)(Math.PI / 2.0) + swingFrac * (float) Math.PI;
                if (cycleTick == 40) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.5f);
                }
            } else if (cycleTick < 60) {
                // Impact hold
                swingAngle = (float)(Math.PI / 2.0);
                if (!impactThisCycle) {
                    impactThisCycle = true;
                    Location impactLoc = c.clone().add(0, 0.5, -3);
                    triggerImpactDamage(impactLoc);
                    // Devastating impact effects
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.4f);
                    c.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 5, 2.0, 0.5, 2.0, 0);
                    // Crater particles
                    DisplayBuilder.particleRing(impactLoc, 4.0, Particle.DUST, 30,
                            new Particle.DustOptions(Color.fromRGB(120, 120, 130), 2.5f));
                    c.getWorld().spawnParticle(Particle.LAVA, impactLoc, 10, 2.0, 0.3, 2.0, 0);
                    // Ground crack particles
                    for (int i = 0; i < 8; i++) {
                        double angle = (i / 8.0) * Math.PI * 2;
                        Location crackLoc = impactLoc.clone().add(
                                Math.cos(angle) * 3, 0.1, Math.sin(angle) * 3);
                        DisplayBuilder.dustParticles(crackLoc, 3, 0.5, 80, 80, 90, 1.5f);
                    }
                }
            } else {
                // Recovery: X rotation from PI/2 back to 0
                float recoverFrac = (cycleTick - 60) / 30.0f;
                swingAngle = (float)(Math.PI / 2.0) * (1.0f - recoverFrac);
            }

            // Calculate pivoted positions for head around a pivot point
            double pivotY = 3.0; // Pivot at mid-handle height
            double headDist = 4.0; // Distance from pivot to head center

            // Head position based on swing angle (pivoting around X axis)
            double headY = pivotY + Math.cos(swingAngle) * headDist;
            double headZ = Math.sin(swingAngle) * headDist;

            // Move head blocks
            for (int idx = 0; idx < headBlocks.size(); idx++) {
                int row = idx / 3;
                int col = idx % 3;
                double x = (col - 1) * 0.9;
                double y = headY + row * 0.8 * Math.cos(swingAngle);
                double z = headZ + row * 0.8 * Math.sin(swingAngle);
                headBlocks.get(idx).entity().teleport(c.clone().add(x, y, z));
                headBlocks.get(idx).rotate(swingAngle, 1, 0, 0);
                headBlocks.get(idx).interpolation(2, 0);
            }

            // Move spike
            if (spikeBlock != null) {
                double spikeY = headY + 0.4 * Math.cos(swingAngle);
                double spikeZ = headZ + 0.5 + 0.4 * Math.sin(swingAngle);
                spikeBlock.entity().teleport(c.clone().add(0, spikeY, spikeZ));
                spikeBlock.rotate(swingAngle, 1, 0, 0);
                spikeBlock.interpolation(2, 0);
            }

            // Move handle blocks (distributed between pivot and head)
            for (int i = 0; i < handleBlocks.size(); i++) {
                float t = (i + 1) / (float)(handleBlocks.size() + 1);
                double hY = pivotY + Math.cos(swingAngle) * headDist * t - pivotY * (1 - t) + pivotY;
                double hZ = Math.sin(swingAngle) * headDist * t;
                handleBlocks.get(i).entity().teleport(c.clone().add(0, pivotY + (headY - pivotY) * t, headZ * t));
                handleBlocks.get(i).rotate(swingAngle, 1, 0, 0);
                handleBlocks.get(i).interpolation(2, 0);
            }

            // Metallic dust from head
            if (ticksAlive % 4 == 0) {
                Location headLoc = c.clone().add(0, headY, headZ);
                DisplayBuilder.dustParticles(headLoc, 4, 0.6, 200, 200, 220, 1.0f);
            }

            // Wind-up sound during rise
            if (cycleTick < 40 && cycleTick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.5f + cycleTick * 0.02f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainWarHammer(plugin); }
    }

    // ================================================================
    // 10. CHAIN FLAIL STAR -- Spiked sphere: 1 iron block center + 6 iron bars
    // as spikes, chain = 4 chain blocks (11 total). Swings in circles, then slams.
    // ================================================================
    public static class ChainFlailStar extends BlockDisplayAttack {

        private BlockDisplayHandle coreBlock;
        private final List<BlockDisplayHandle> spikeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> chainBlocks = new ArrayList<>();
        private float swingAngle = 0;
        private float swingRadius = 3.0f;
        private boolean slamming = false;
        private int slamTick = 0;

        public ChainFlailStar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_flail_star", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(16.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(25);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Core: 1 iron block (morning star center)
            coreBlock = displayBuilder.spawnBlock(center.clone().add(0, 4, 0), Material.IRON_BLOCK);
            coreBlock.scale(1.35f, 1.35f, 1.35f).glow(190, 190, 200).interpolation(2, 0);
            spawnedEntities.add(coreBlock.entity());

            // 6 spikes radiating outward (iron bars) in 6 directions
            double[][] spikeDirections = {
                {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
            };
            for (double[] dir : spikeDirections) {
                Location loc = center.clone().add(dir[0] * 0.7, 4 + dir[1] * 0.7, dir[2] * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BARS);
                h.scale(0.45f, 0.45f, 0.45f).glow(200, 200, 210).interpolation(2, 0);
                spikeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Chain: 4 chain blocks connecting to overhead anchor point
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 5.0 + i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.38f, 1.05f, 0.38f).glow(120, 120, 130).interpolation(2, 0);
                chainBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Cycle: 60 ticks overhead spin, 20 tick slam, 20 tick recover
            int cycleTick = ticksAlive % 100;

            double starX, starZ, starY;
            float anchorY = 8.0f; // Chain anchor point high above

            if (cycleTick < 60) {
                // Overhead spinning: circle in horizontal plane
                swingAngle += 0.12f; // Accelerating feel via constant speed
                starX = Math.cos(swingAngle) * swingRadius;
                starZ = Math.sin(swingAngle) * swingRadius;
                starY = 4.0 + Math.sin(cycleTick * 0.15) * 0.5; // Gentle bob

                slamming = false;

                if (cycleTick % 8 == 0) {
                    DisplayBuilder.playSound(c.clone().add(starX, starY, starZ),
                            Sound.BLOCK_CHAIN_STEP, 0.5f, 1.0f + cycleTick * 0.005f);
                }
            } else if (cycleTick < 80) {
                // Slam down in the last direction of spin
                slamTick = cycleTick - 60;
                float slamFrac = slamTick / 20.0f;

                // Star travels from spin position down to ground
                double lastX = Math.cos(swingAngle) * swingRadius;
                double lastZ = Math.sin(swingAngle) * swingRadius;
                starX = lastX * (1.0 + slamFrac * 0.5); // Extend outward slightly
                starZ = lastZ * (1.0 + slamFrac * 0.5);
                starY = 4.0 * (1.0 - slamFrac) + 0.5; // Drop to ground

                if (!slamming) {
                    slamming = true;
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.4f);
                }

                // Impact at bottom
                if (slamTick == 19) {
                    Location impactLoc = c.clone().add(starX, 0.5, starZ);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
                    c.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 3, 1.0, 0.3, 1.0, 0);
                    DisplayBuilder.particleRing(impactLoc, 3.0, Particle.DUST, 20,
                            new Particle.DustOptions(Color.fromRGB(180, 180, 190), 2.0f));
                }
            } else {
                // Recover: pull back up to spinning height
                float recoverFrac = (cycleTick - 80) / 20.0f;
                double lastX = Math.cos(swingAngle) * swingRadius;
                double lastZ = Math.sin(swingAngle) * swingRadius;
                starX = lastX * (1.5 - recoverFrac * 0.5);
                starZ = lastZ * (1.5 - recoverFrac * 0.5);
                starY = 0.5 + recoverFrac * 3.5;

                if (cycleTick == 80) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.7f, 0.6f);
                }
            }

            // Move core block
            Location starLoc = c.clone().add(starX, starY, starZ);
            if (coreBlock != null) {
                coreBlock.entity().teleport(starLoc);
                coreBlock.rotate(ticksAlive * 0.1f, 1, 1, 0);
                coreBlock.interpolation(2, 0);
            }

            // Move spikes relative to core
            double[][] spikeDirections = {
                {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
            };
            for (int i = 0; i < spikeBlocks.size(); i++) {
                Location spikeLoc = starLoc.clone().add(
                        spikeDirections[i][0] * 0.7,
                        spikeDirections[i][1] * 0.7,
                        spikeDirections[i][2] * 0.7);
                spikeBlocks.get(i).entity().teleport(spikeLoc);
                spikeBlocks.get(i).rotate(ticksAlive * 0.1f, 1, 1, 0);
                spikeBlocks.get(i).interpolation(2, 0);
            }

            // Chain links: distributed between star and anchor point above center
            Location anchorLoc = c.clone().add(0, anchorY, 0);
            for (int i = 0; i < chainBlocks.size(); i++) {
                float t = (i + 1) / (float)(chainBlocks.size() + 1);
                // Catenary-like droop in the chain
                double chainX = starX * (1.0 - t);
                double chainZ = starZ * (1.0 - t);
                double chainY = starY + (anchorY - starY) * t - Math.sin(t * Math.PI) * 0.8;
                chainBlocks.get(i).entity().teleport(c.clone().add(chainX, chainY, chainZ));
                chainBlocks.get(i).interpolation(2, 0);
            }

            // Continuous iron sparks from star
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, starLoc, 4, 0.3, 0.3, 0.3, 0.15);
                DisplayBuilder.dustParticles(starLoc, 3, 0.4, 190, 190, 200, 0.7f);
            }

            // Chain rattle
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(starLoc, Sound.BLOCK_CHAIN_STEP, 0.4f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainFlailStar(plugin); }
    }
}
