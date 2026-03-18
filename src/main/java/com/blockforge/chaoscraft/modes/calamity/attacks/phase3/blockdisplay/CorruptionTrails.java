package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3 (4C) Block Display -- GROUP 5: CORRUPTION TRAIL MANIFESTATIONS
 * Structures 36-50: Structures that emerge from old corruption footsteps.
 * Tier 1 through Tier 5: "What The Ground Remembers"
 *
 * Dweller palette: crimson glow(200,0,50), orange glow(255,100,0), soul blue glow(0,150,255)
 * Materials: NETHERRACK, MAGMA_BLOCK, BASALT, SMOOTH_BASALT, BLACKSTONE,
 *            POLISHED_BLACKSTONE, CRIMSON_NYLIUM, NETHER_WART_BLOCK, SOUL_SOIL,
 *            CRACKED_STONE_BRICKS, OBSIDIAN, CRYING_OBSIDIAN, BLACK_CONCRETE
 *
 * Rules applied:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - Damage in HP (4.0-12.0 range)
 * - AxisAngle4f for all rotations, never Quaternionf
 * - Impact damage via triggerImpactDamage()
 */
public final class CorruptionTrails {

    private CorruptionTrails() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new NetherrackSpikeBloom(plugin));
        registry.register(new SoulFireCreepLine(plugin));
        registry.register(new AshColumnGeyser(plugin));
        registry.register(new BrimstoneHandBreach(plugin));
        registry.register(new SoulFireFootprintBloom(plugin));
        registry.register(new CorruptionPoolVent(plugin));
        registry.register(new CharredLatticeSpread(plugin));
        registry.register(new BrimstoneCrustCollapse(plugin));
        registry.register(new TrailLatticeNetwork(plugin));
        registry.register(new NetherSporeColumn(plugin));
        registry.register(new SootStreakMarker(plugin));
        registry.register(new MoltenRootWeb(plugin));
        registry.register(new SoulWitherGlyph(plugin));
        registry.register(new CorruptionSinkholeCrown(plugin));
        registry.register(new FinalTrailIgnition(plugin));
    }

    // ================================================================
    // 36. NETHERRACK SPIKE BLOOM -- 7 netherrack spikes erupting from
    //     a corruption zone at irregular angles with magma ground fill
    // ================================================================
    public static class NetherrackSpikeBloom extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private final List<BlockDisplayHandle> groundFills = new ArrayList<>();
        private boolean emerged = false;

        public NetherrackSpikeBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("netherrack_spike_bloom", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 7 netherrack spikes at different angles, starting underground
            double[][] spikeData = {
                // {angleFromVertical(deg), azimuth(deg), heightScale}
                {0, 0, 4.0},       // vertical
                {20, 45, 3.0},     // 20 deg NE
                {20, 225, 3.0},    // 20 deg SW
                {35, 135, 2.0},    // 35 deg SE
                {35, 315, 2.0},    // 35 deg NW
                {70, 0, 1.0},      // near horizontal N
                {70, 180, 1.0}     // near horizontal S
            };
            for (double[] sd : spikeData) {
                double tilt = Math.toRadians(sd[0]);
                double azimuth = Math.toRadians(sd[1]);
                double dx = Math.sin(tilt) * Math.cos(azimuth) * 0.5;
                double dz = Math.sin(tilt) * Math.sin(azimuth) * 0.5;
                Location loc = center.clone().add(dx, -4, dz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(1.0f, (float) sd[2], 1.0f)
                 .rotate((float) tilt, (float) Math.sin(azimuth), 0, (float) Math.cos(azimuth))
                 .glow(200, 0, 50)
                 .interpolation(3, 0);
                spikes.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 magma ground fills starting invisible
            double[][] magmaOff = {{0.8, 0, 0.8}, {-0.8, 0, 0.8}, {0.8, 0, -0.8}, {-0.8, 0, -0.8}};
            for (double[] off : magmaOff) {
                Location loc = center.clone().add(off[0], 0, off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(255, 100, 0).interpolation(3, 0);
                groundFills.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: Ground fills scale in (0-10 ticks)
            if (ticksAlive <= 10) {
                float fillScale = ticksAlive / 10.0f;
                for (BlockDisplayHandle fill : groundFills) {
                    fill.scale(fillScale, 0.3f * fillScale, fillScale);
                    fill.interpolation(2, 0);
                }
            }

            // Phase 2: Spikes emerge upward (0-25 ticks)
            if (ticksAlive <= 25) {
                float emerge = ticksAlive / 25.0f;
                float yOffset = emerge * 4.0f;
                for (BlockDisplayHandle spike : spikes) {
                    BlockDisplay bd = spike.entity();
                    Location baseLoc = bd.getLocation();
                    bd.teleport(baseLoc.clone().add(0, yOffset / 25.0, 0));
                }
                // Impact on full emergence
                if (ticksAlive == 25 && !emerged) {
                    emerged = true;
                    triggerImpactDamage(center);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);
                    w.spawnParticle(Particle.CRIMSON_SPORE, center, 40, 1.5, 0.2, 1.5, 0.05);
                }
            }

            // Phase 3: Micro-oscillation vibration
            if (ticksAlive > 25) {
                for (BlockDisplayHandle spike : spikes) {
                    float vibrate = 0.08f * (float) Math.sin(ticksAlive * 2 * Math.PI / 10);
                    BlockDisplay bd = spike.entity();
                    Location loc = bd.getLocation();
                    bd.teleport(loc.clone().add(0, vibrate - 0.08f * (float) Math.sin((ticksAlive - 1) * 2 * Math.PI / 10), 0));
                }
            }

            // FLAME from spike tips
            if (ticksAlive % 3 == 0 && ticksAlive > 25) {
                for (BlockDisplayHandle spike : spikes) {
                    Location tip = spike.entity().getLocation().add(0, 2, 0);
                    w.spawnParticle(Particle.FLAME, tip, 2, 0.05, 0.3, 0.05, 0.01);
                }
            }

            // LAVA from magma ground
            if (ticksAlive % 7 == 0 && ticksAlive > 10) {
                for (BlockDisplayHandle fill : groundFills) {
                    Location fLoc = fill.entity().getLocation().add(0, 0.2, 0);
                    w.spawnParticle(Particle.LAVA, fLoc, 1, 0.2, 0.1, 0.2, 0);
                }
            }

            // Sound loop
            if (ticksAlive % 50 == 0 && ticksAlive > 25) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 1.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NetherrackSpikeBloom(plugin); }
    }

    // ================================================================
    // 37. SOUL FIRE CREEP LINE -- Chain of soul soil nodes connected by
    //     soul fire laser lines along a corruption trail path
    // ================================================================
    public static class SoulFireCreepLine extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> soulNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> obsidianEmbed = new ArrayList<>();
        private final List<BlockDisplayHandle> soulSandFlanks = new ArrayList<>();
        private int revealedCount = 0;

        public SoulFireCreepLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_creep_line", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 zones in a chain along a line
            for (int i = 0; i < 5; i++) {
                double offset = (i - 2) * 2.5;

                // 2 soul soil per zone
                Location ss1 = center.clone().add(offset, 0, -0.3);
                BlockDisplayHandle s1 = displayBuilder.spawnBlock(ss1, Material.SOUL_SOIL);
                s1.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(3, 0);
                soulNodes.add(s1);
                spawnedEntities.add(s1.entity());

                Location ss2 = center.clone().add(offset, 0, 0.3);
                BlockDisplayHandle s2 = displayBuilder.spawnBlock(ss2, Material.SOUL_SOIL);
                s2.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(3, 0);
                soulNodes.add(s2);
                spawnedEntities.add(s2.entity());

                // 1 soul sand flank per zone
                Location sandLoc = center.clone().add(offset, 0, 0.8);
                BlockDisplayHandle sand = displayBuilder.spawnBlock(sandLoc, Material.SOUL_SAND);
                sand.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(3, 0);
                soulSandFlanks.add(sand);
                spawnedEntities.add(sand.entity());

                // 1 obsidian embed per zone, slightly below ground
                Location obsLoc = center.clone().add(offset, -0.2, 0);
                BlockDisplayHandle obs = displayBuilder.spawnBlock(obsLoc, Material.OBSIDIAN);
                obs.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(3, 0);
                obsidianEmbed.add(obs);
                spawnedEntities.add(obs.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Progressive reveal: each zone appears over 15 ticks, spaced 21 ticks apart
            int zoneToReveal = ticksAlive / 21;
            if (zoneToReveal < 5 && zoneToReveal >= revealedCount) {
                int localTick = ticksAlive % 21;
                if (localTick <= 15) {
                    float revealScale = localTick / 15.0f;
                    int idx = zoneToReveal;
                    // Soul nodes (2 per zone)
                    soulNodes.get(idx * 2).scale(revealScale, 0.3f * revealScale, revealScale);
                    soulNodes.get(idx * 2).interpolation(3, 0);
                    soulNodes.get(idx * 2 + 1).scale(revealScale, 0.3f * revealScale, revealScale);
                    soulNodes.get(idx * 2 + 1).interpolation(3, 0);
                    // Soul sand flank
                    soulSandFlanks.get(idx).scale(revealScale, 0.5f * revealScale, revealScale);
                    soulSandFlanks.get(idx).interpolation(3, 0);
                    // Obsidian embed
                    obsidianEmbed.get(idx).scale(0.5f * revealScale, 0.5f * revealScale, 0.5f * revealScale);
                    obsidianEmbed.get(idx).interpolation(3, 0);

                    if (localTick == 15) {
                        revealedCount = zoneToReveal + 1;
                        DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.4f, 0.4f);
                    }
                }
            }

            // Soul fire laser lines between revealed zones
            if (ticksAlive % 3 == 0 && revealedCount > 1) {
                float densityPhase = (float) Math.sin(ticksAlive * 2 * Math.PI / 40);
                for (int i = 0; i < revealedCount - 1; i++) {
                    Location from = obsidianEmbed.get(i).entity().getLocation().add(0, 0.3, 0);
                    Location to = obsidianEmbed.get(i + 1).entity().getLocation().add(0, 0.3, 0);
                    DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 4, null);
                }
            }

            // ASH rising from revealed nodes
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < revealedCount * 2 && i < soulNodes.size(); i++) {
                    Location nLoc = soulNodes.get(i).entity().getLocation().add(0, 0.3, 0);
                    w.spawnParticle(Particle.ASH, nLoc, 1, 0.1, 0.2, 0.1, 0);
                }
            }

            // SMOKE along chain
            if (ticksAlive % 5 == 0 && revealedCount > 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 0.2, 0), 2, 4, 0.2, 0.5, 0.01);
            }

            // Sinking in last 20 ticks
            int remaining = config.getDurationTicks() - ticksAlive;
            if (remaining <= 20 && remaining > 0) {
                float sinkProgress = (20 - remaining) / 20.0f;
                float sinkY = -0.1f * sinkProgress;
                for (BlockDisplayHandle node : soulNodes) {
                    node.translate(-0.5f, -0.5f + sinkY, -0.5f);
                    node.interpolation(3, 0);
                }
            }

            // Ambient sound
            if (ticksAlive % 100 == 0 && revealedCount >= 5) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFireCreepLine(plugin); }
    }

    // ================================================================
    // 38. ASH COLUMN GEYSER -- Tall soul sand column that coalesces
    //     from particles, then sways with an ash mushroom cloud
    // ================================================================
    public static class AshColumnGeyser extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> columnBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> capBlocks = new ArrayList<>();
        private int formedBlocks = 0;

        public AshColumnGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ash_column_geyser", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Column: 6 soul sand blocks (recessed 1 into ground)
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, -1 + i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SAND);
                h.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(5, 0);
                columnBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 cracked stone ring supports at mid-height
            for (int side = -1; side <= 1; side += 2) {
                Location loc = center.clone().add(side * 0.6, 2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRACKED_STONE_BRICKS);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(5, 0);
                ringBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 blackstone cap blocks
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, 5 + i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(5, 0);
                capBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1 (0-20): Particle coalescing only
            if (ticksAlive <= 20) {
                w.spawnParticle(Particle.ASH, center.clone().add(0, 3, 0), 8, 0.5, 2, 0.5, 0.02);
                DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 5, 1.0, 0, 150, 255, 1.0f);
                return;
            }

            // Phase 2 (20-50): Column blocks form, each 4 ticks after previous
            int blockIdx = (ticksAlive - 20) / 5;
            if (blockIdx < columnBlocks.size() && blockIdx >= formedBlocks) {
                int localTick = (ticksAlive - 20) % 5;
                float formProgress = localTick / 4.0f;
                columnBlocks.get(blockIdx).scale(formProgress, formProgress, formProgress);
                columnBlocks.get(blockIdx).interpolation(5, 0);
                if (localTick == 4) {
                    formedBlocks = blockIdx + 1;
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 0.5f);
                }
            }

            // Ring blocks at tick 35
            if (ticksAlive >= 35 && ticksAlive < 40) {
                float ringScale = (ticksAlive - 35) / 4.0f;
                for (BlockDisplayHandle ring : ringBlocks) {
                    ring.scale(0.8f * ringScale, 0.8f * ringScale, 0.8f * ringScale);
                    ring.interpolation(5, 0);
                }
            }

            // Cap blocks at tick 45
            if (ticksAlive >= 45 && ticksAlive < 50) {
                float capScale = (ticksAlive - 45) / 4.0f;
                for (BlockDisplayHandle cap : capBlocks) {
                    cap.scale(capScale, capScale, capScale);
                    cap.interpolation(5, 0);
                }
            }

            // After formation: sway on Z-axis
            if (ticksAlive > 50) {
                float sway = (float) Math.sin(ticksAlive * 2 * Math.PI / 90) * 0.0524f;
                for (BlockDisplayHandle block : columnBlocks) {
                    block.rotate(sway, 0, 0, 1);
                    block.interpolation(5, 0);
                }
                for (BlockDisplayHandle cap : capBlocks) {
                    cap.rotate(sway, 0, 0, 1);
                    // Cap pulse: 1.0 to 1.15
                    float capPulse = 1.0f + 0.075f * (float) Math.sin(ticksAlive * 2 * Math.PI / 60) + 0.075f;
                    cap.scale(capPulse, capPulse, capPulse);
                    cap.interpolation(5, 0);
                }
            }

            // ASH blast from column top
            if (ticksAlive % 2 == 0 && ticksAlive > 50) {
                Location top = center.clone().add(0, 6.5, 0);
                w.spawnParticle(Particle.ASH, top, 6, 0.2, 1.5, 0.2, 0.03);
            }

            // Mushroom cloud smoke at dispersal height
            if (ticksAlive % 3 == 0 && ticksAlive > 50) {
                Location cloudHeight = center.clone().add(0, 8, 0);
                w.spawnParticle(Particle.SMOKE, cloudHeight, 2, 2, 0.2, 2, 0.01);
            }

            // Soul fire at column base
            if (ticksAlive % 4 == 0 && ticksAlive > 20) {
                DisplayBuilder.particleRing(center.clone().add(0, 0, 0), 0.5, Particle.SOUL_FIRE_FLAME, 6, null);
            }

            // Ambient sound
            if (ticksAlive % 120 == 0 && ticksAlive > 50) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AshColumnGeyser(plugin); }
    }

    // ================================================================
    // 39. BRIMSTONE HAND BREACH -- Giant hand erupting from corruption
    //     zone with 5 magma palm blocks and 5 finger columns
    // ================================================================
    public static class BrimstoneHandBreach extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> palmBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> fingers = new ArrayList<>();
        private final List<BlockDisplayHandle> wristBlocks = new ArrayList<>();
        private BlockDisplayHandle fingerTipCharge;
        private boolean emerged = false;

        public BrimstoneHandBreach(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_hand_breach", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 magma palm blocks in rough pentagon, starting underground
            double[][] palmPositions = {
                {0, 0}, {0.7, 0.5}, {-0.7, 0.5}, {0.5, -0.7}, {-0.5, -0.7}
            };
            for (double[] pos : palmPositions) {
                Location loc = center.clone().add(pos[0], -5, pos[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.9f, 0.5f, 0.9f).glow(255, 100, 0).interpolation(3, 0);
                palmBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 5 fingers: each is 3 blocks (netherrack base, blackstone mid, polished blackstone tip)
            double[] fingerAngles = {10, 20, 30, 20, 10};
            double[] fingerAzimuths = {-40, -20, 0, 20, 40};
            Material[][] fingerMats = {
                {Material.NETHERRACK, Material.BLACKSTONE, Material.POLISHED_BLACKSTONE},
                {Material.NETHERRACK, Material.BLACKSTONE, Material.POLISHED_BLACKSTONE},
                {Material.NETHERRACK, Material.BLACKSTONE, Material.POLISHED_BLACKSTONE},
                {Material.NETHERRACK, Material.BLACKSTONE, Material.POLISHED_BLACKSTONE},
                {Material.NETHERRACK, Material.BLACKSTONE, Material.POLISHED_BLACKSTONE}
            };
            for (int f = 0; f < 5; f++) {
                List<BlockDisplayHandle> finger = new ArrayList<>();
                double tilt = Math.toRadians(fingerAngles[f]);
                double azimuth = Math.toRadians(fingerAzimuths[f]);
                for (int seg = 0; seg < 3; seg++) {
                    double dx = Math.sin(tilt) * Math.cos(azimuth) * seg * 0.4;
                    double dz = Math.sin(tilt) * Math.sin(azimuth) * seg * 0.4;
                    Location loc = center.clone().add(dx + palmPositions[f][0], -5 + seg, dz + palmPositions[f][1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, fingerMats[f][seg]);
                    float taper = 0.9f - seg * 0.1f;
                    h.scale(taper, 1.0f, taper).glow(200, 0, 50).interpolation(3, 0);
                    finger.add(h);
                    spawnedEntities.add(h.entity());
                }
                fingers.add(finger);
            }

            // 2 wrist blocks
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, -7 + i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRACKED_STONE_BRICKS);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                wristBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fire charge at center finger tip
            Location tipLoc = center.clone().add(0, -3, 0);
            fingerTipCharge = displayBuilder.spawnBlock(tipLoc, Material.MAGMA_BLOCK);
            fingerTipCharge.scale(1.2f, 1.2f, 1.2f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(fingerTipCharge.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Emergence: 0-30 ticks, hand rises 5 blocks
            if (ticksAlive <= 30) {
                float emerge = ticksAlive / 30.0f;
                float yOffset = emerge * 5.0f;

                for (BlockDisplayHandle palm : palmBlocks) {
                    BlockDisplay bd = palm.entity();
                    Location loc = bd.getLocation();
                    bd.teleport(loc.clone().add(0, yOffset / 30.0, 0));
                }
                for (List<BlockDisplayHandle> finger : fingers) {
                    for (BlockDisplayHandle seg : finger) {
                        BlockDisplay bd = seg.entity();
                        Location loc = bd.getLocation();
                        bd.teleport(loc.clone().add(0, yOffset / 30.0, 0));
                    }
                }
                for (BlockDisplayHandle wrist : wristBlocks) {
                    BlockDisplay bd = wrist.entity();
                    Location loc = bd.getLocation();
                    bd.teleport(loc.clone().add(0, yOffset / 30.0, 0));
                }
                if (fingerTipCharge != null) {
                    BlockDisplay bd = fingerTipCharge.entity();
                    Location loc = bd.getLocation();
                    bd.teleport(loc.clone().add(0, yOffset / 30.0, 0));
                }

                // Impact at full emergence
                if (ticksAlive == 30 && !emerged) {
                    emerged = true;
                    triggerImpactDamage(center);
                    w.spawnParticle(Particle.LAVA, center, 50, 1.5, 0.5, 1.5, 0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
                }
            }

            // Finger flex: each oscillates inward 8 degrees, staggered
            if (ticksAlive > 30) {
                for (int f = 0; f < fingers.size(); f++) {
                    float flexAngle = (float) Math.sin((ticksAlive + f * 14) * 2 * Math.PI / 70) * 0.1396f;
                    List<BlockDisplayHandle> finger = fingers.get(f);
                    for (int seg = 0; seg < finger.size(); seg++) {
                        float segFlex = flexAngle * (seg + 1) / 3.0f;
                        finger.get(seg).rotate(segFlex, 1, 0, 0);
                        finger.get(seg).interpolation(3, 0);
                    }
                }
            }

            // Fire charge tip pulse: 1.2 to 1.8
            if (fingerTipCharge != null) {
                float tipPulse = 1.2f + 0.3f * (float) Math.sin(ticksAlive * 2 * Math.PI / 40) + 0.3f;
                fingerTipCharge.scale(tipPulse, tipPulse, tipPulse);
                fingerTipCharge.interpolation(3, 0);
            }

            // Palm magma pulse
            float palmPulse = 1.0f + 0.025f * (float) Math.sin(ticksAlive * 2 * Math.PI / 30) + 0.025f;
            for (BlockDisplayHandle palm : palmBlocks) {
                palm.scale(0.9f * palmPulse, 0.5f, 0.9f * palmPulse);
                palm.interpolation(3, 0);
            }

            // FLAME from finger tips
            if (ticksAlive % 4 == 0 && ticksAlive > 30) {
                for (List<BlockDisplayHandle> finger : fingers) {
                    if (finger.size() >= 3) {
                        Location tip = finger.get(2).entity().getLocation().add(0, 1, 0);
                        w.spawnParticle(Particle.FLAME, tip, 1, 0.05, 0.2, 0.05, 0.01);
                    }
                }
            }

            // Dripping lava from center finger tip charge
            if (ticksAlive % 7 == 0 && fingerTipCharge != null) {
                Location chargeLoc = fingerTipCharge.entity().getLocation();
                w.spawnParticle(Particle.DRIPPING_LAVA, chargeLoc, 1, 0, 0, 0, 0);
            }

            // Crimson spore from wrist
            if (ticksAlive % 4 == 0 && ticksAlive > 30) {
                w.spawnParticle(Particle.CRIMSON_SPORE, center.clone().add(0, -0.5, 0), 2, 0.5, 0.2, 0.5, 0.01);
            }

            // Fire charge burst every 40 ticks
            if (ticksAlive % 40 == 0 && ticksAlive > 30) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.8f);
                DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 10, 1.0);
            }

            // Ambient creak
            if (ticksAlive % 160 == 0 && ticksAlive > 30) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneHandBreach(plugin); }
    }

    // ================================================================
    // 40. SOUL FIRE FOOTPRINT BLOOM -- Dweller footprint shape at
    //     ground level with soul sand ovals and magma cream toes
    // ================================================================
    public static class SoulFireFootprintBloom extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mainFoot = new ArrayList<>();
        private final List<BlockDisplayHandle> toeBlocks = new ArrayList<>();
        private BlockDisplayHandle heelBlock;
        private boolean stamped = false;

        public SoulFireFootprintBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_footprint_bloom", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main foot: 6 soul sand blocks in 3x2 oval, 0.05 above ground
            double[][] footPositions = {
                {-0.5, 0.05, -0.3}, {0, 0.05, -0.3}, {0.5, 0.05, -0.3},
                {-0.5, 0.05, 0.3}, {0, 0.05, 0.3}, {0.5, 0.05, 0.3}
            };
            for (double[] pos : footPositions) {
                Location loc = center.clone().add(pos[0], pos[1], pos[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SAND);
                h.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(2, 0);
                mainFoot.add(h);
                spawnedEntities.add(h.entity());
            }

            // 5 toe blocks in forward row (magma block representing magma cream)
            for (int i = 0; i < 5; i++) {
                double xOff = (i - 2) * 0.4;
                Location loc = center.clone().add(xOff, 0.05, -0.9);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(255, 100, 0).interpolation(2, 0);
                toeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Heel: soul soil
            heelBlock = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0.7), Material.SOUL_SOIL);
            heelBlock.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(2, 0);
            spawnedEntities.add(heelBlock.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Stamp animation: snap to full over 8 ticks
            if (ticksAlive <= 8 && !stamped) {
                float stampScale = ticksAlive / 8.0f;
                for (BlockDisplayHandle foot : mainFoot) {
                    foot.scale(stampScale, 0.2f * stampScale, stampScale);
                    foot.interpolation(2, 0);
                }
                for (BlockDisplayHandle toe : toeBlocks) {
                    toe.scale(0.7f * stampScale, 0.2f * stampScale, 0.7f * stampScale);
                    toe.interpolation(2, 0);
                }
                if (heelBlock != null) {
                    heelBlock.scale(stampScale, 0.3f * stampScale, stampScale);
                    heelBlock.interpolation(2, 0);
                }
                if (ticksAlive == 8) {
                    stamped = true;
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f);
                }
            }

            // Main foot pulse: 1.0 to 1.2
            if (stamped) {
                float footPulse = 1.0f + 0.1f * (float) Math.sin(ticksAlive * 2 * Math.PI / 45) + 0.1f;
                for (BlockDisplayHandle foot : mainFoot) {
                    foot.scale(footPulse, 0.2f, footPulse);
                    foot.interpolation(3, 0);
                }
            }

            // Toe ripple: each oscillates with delay
            if (stamped) {
                for (int i = 0; i < toeBlocks.size(); i++) {
                    float toePulse = 0.7f + 0.15f * (float) Math.sin((ticksAlive + i * 8) * 2 * Math.PI / 40) + 0.15f;
                    toeBlocks.get(i).scale(toePulse, 0.2f, toePulse);
                    toeBlocks.get(i).interpolation(3, 0);
                }
            }

            // Lift-off in last 30 ticks
            int remaining = config.getDurationTicks() - ticksAlive;
            if (remaining <= 30 && remaining > 0) {
                float liftProgress = (30 - remaining) / 30.0f;
                float liftY = liftProgress * 0.3f;
                for (BlockDisplayHandle foot : mainFoot) {
                    foot.translate(-0.5f, -0.5f + liftY, -0.5f);
                    foot.interpolation(3, 0);
                }
                for (BlockDisplayHandle toe : toeBlocks) {
                    toe.translate(-0.5f, -0.5f + liftY, -0.5f);
                    toe.interpolation(3, 0);
                }
            }

            // Soul fire from main foot components
            if (ticksAlive % 3 == 0 && stamped) {
                for (BlockDisplayHandle foot : mainFoot) {
                    Location fLoc = foot.entity().getLocation().add(0, 0.2, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, fLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Toe laser segments
            if (ticksAlive % 4 == 0 && stamped) {
                for (int i = 0; i < toeBlocks.size() - 1; i++) {
                    Location from = toeBlocks.get(i).entity().getLocation().add(0, 0.1, 0);
                    Location to = toeBlocks.get(i + 1).entity().getLocation().add(0, 0.1, 0);
                    DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 3, null);
                }
            }

            // ASH from heel
            if (ticksAlive % 4 == 0 && stamped && heelBlock != null) {
                Location heelLoc = heelBlock.entity().getLocation().add(0, 0.3, 0);
                w.spawnParticle(Particle.ASH, heelLoc, 1, 0.2, 0.2, 0.2, 0);
            }

            // Ambient soul mood
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFireFootprintBloom(plugin); }
    }

    // ================================================================
    // 41. CORRUPTION POOL VENT -- 5x5 soul sand depression with central
    //     fire charge vent, blaze rod corner vents, basalt walls
    // ================================================================
    public static class CorruptionPoolVent extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> poolFloor = new ArrayList<>();
        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> cornerVents = new ArrayList<>();
        private BlockDisplayHandle centralVent;

        public CorruptionPoolVent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_pool_vent", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(1100);
            config.setCooldownTicks(550);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pool floor: 12 soul sand (outer ring) + 4 magma corners
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                        Location loc = center.clone().add(x, -1, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SAND);
                        h.scale(1.0f, 0.5f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
                        poolFloor.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
            // 4 magma corners inside
            int[][] magmaCorners = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
            for (int[] mc : magmaCorners) {
                Location loc = center.clone().add(mc[0], -1, mc[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(1.0f, 0.5f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                poolFloor.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 basalt walls
            int[][] wallPositions = {{0, -3}, {0, 3}, {-3, 0}, {3, 0}};
            for (int[] wp : wallPositions) {
                Location loc = center.clone().add(wp[0], 0, wp[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                wallBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central fire charge (magma block, oversized)
            centralVent = displayBuilder.spawnBlock(center.clone().add(0, 0, 0), Material.MAGMA_BLOCK);
            centralVent.scale(4.0f, 4.0f, 4.0f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(centralVent.entity());

            // 4 blaze rod corner vents (thin netherrack pillars)
            int[][] ventPositions = {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}};
            for (int[] vp : ventPositions) {
                Location loc = center.clone().add(vp[0], 0, vp[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(0.3f, 2.5f, 0.3f).glow(255, 100, 0).interpolation(3, 0);
                cornerVents.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Central vent rotation: Y 1.0 deg/tick, X 0.3 deg/tick
            if (centralVent != null) {
                float yRot = ticksAlive * 0.01745f;
                float xRot = ticksAlive * 0.00524f;
                centralVent.rotate(yRot + xRot, 0.2f, 0.9f, 0);
                centralVent.interpolation(3, 0);
            }

            // Corner vent Y-axis spin: 1.4 deg/tick
            float ventRot = ticksAlive * 0.02443f;
            for (BlockDisplayHandle vent : cornerVents) {
                vent.rotate(ventRot, 0, 1, 0);
                vent.interpolation(3, 0);
            }

            // Pool floor pulse from center outward
            for (int i = 0; i < poolFloor.size(); i++) {
                float pulse = 1.0f + 0.05f * (float) Math.sin((ticksAlive - i * 2) * 2 * Math.PI / 25);
                poolFloor.get(i).scale(pulse, 0.5f, pulse);
                poolFloor.get(i).interpolation(3, 0);
            }

            // Wall tilt outward: 3 degrees over 200-tick cycle
            float tiltAngle = (float) Math.sin(ticksAlive * 2 * Math.PI / 200) * 0.0524f;
            for (int i = 0; i < wallBlocks.size(); i++) {
                float tiltDir = (i < 2) ? tiltAngle : -tiltAngle;
                wallBlocks.get(i).rotate(tiltDir, (i % 2 == 0) ? 0 : 1, 0, (i % 2 == 0) ? 1 : 0);
                wallBlocks.get(i).interpolation(5, 0);
            }

            // LAVA from pool floor
            if (ticksAlive % 3 == 0) {
                w.spawnParticle(Particle.LAVA, center.clone().add(0, -0.5, 0), 4, 2, 0.2, 2, 0);
            }

            // FLAME from central vent
            if (ticksAlive % 3 == 0 && centralVent != null) {
                Location ventLoc = centralVent.entity().getLocation();
                w.spawnParticle(Particle.FLAME, ventLoc.clone().add(0, 2, 0), 3, 0.3, 1, 0.3, 0.02);
            }

            // Soul fire from corner vents
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle vent : cornerVents) {
                    Location vLoc = vent.entity().getLocation().add(0, 2, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, vLoc, 2, 0.8, 0.1, 0.8, 0.02);
                }
            }

            // Dripping lava from vent tips
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle vent : cornerVents) {
                    Location tipLoc = vent.entity().getLocation().add(0, 2.5, 0);
                    w.spawnParticle(Particle.DRIPPING_LAVA, tipLoc, 1, 0, 0, 0, 0);
                }
            }

            // Lava explosion every 300 ticks (15 seconds)
            if (ticksAlive % 300 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
                w.spawnParticle(Particle.LAVA, center, 30, 2, 1.5, 2, 0);
                DisplayBuilder.crimsonDust(center, 20, 3.0);
            }

            // Ambient
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionPoolVent(plugin); }
    }

    // ================================================================
    // 42. CHARRED LATTICE SPREAD -- Blackstone node with cracked stone
    //     arms extending along corruption trail intersections
    // ================================================================
    public static class CharredLatticeSpread extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> centralNode = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> arms = new ArrayList<>();
        private final List<BlockDisplayHandle> endCaps = new ArrayList<>();
        private int grownSegments = 0;

        public CharredLatticeSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("charred_lattice_spread", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 9-block central node (3x3 blackstone at ground)
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x, 0.1, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(3, 0);
                    centralNode.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 3 arms extending in three directions, 4 blocks each
            double[][] armDirections = {{1, 0}, {-0.5, 0.87}, {-0.5, -0.87}};
            for (double[] dir : armDirections) {
                List<BlockDisplayHandle> arm = new ArrayList<>();
                for (int i = 1; i <= 4; i++) {
                    Location loc = center.clone().add(dir[0] * i * 1.2, 0.1, dir[1] * i * 1.2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRACKED_STONE_BRICKS);
                    h.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(3, 0);
                    arm.add(h);
                    spawnedEntities.add(h.entity());
                }
                arms.add(arm);

                // End cap: polished blackstone
                Location capLoc = center.clone().add(dir[0] * 5.0, 0.1, dir[1] * 5.0);
                BlockDisplayHandle cap = displayBuilder.spawnBlock(capLoc, Material.POLISHED_BLACKSTONE);
                cap.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(3, 0);
                endCaps.add(cap);
                spawnedEntities.add(cap.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.7f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: Central node appears (0-10 ticks)
            if (ticksAlive <= 10) {
                float nodeScale = ticksAlive / 10.0f;
                for (BlockDisplayHandle node : centralNode) {
                    node.scale(nodeScale, 0.3f * nodeScale, nodeScale);
                    node.interpolation(3, 0);
                }
            }

            // Phase 2: Arms grow block by block (10+ ticks, 5 ticks per block)
            if (ticksAlive > 10) {
                int armProgress = (ticksAlive - 10) / 5;
                if (armProgress < 12 && armProgress >= grownSegments) {
                    int armIdx = armProgress / 4;
                    int segIdx = armProgress % 4;
                    if (armIdx < arms.size() && segIdx < arms.get(armIdx).size()) {
                        arms.get(armIdx).get(segIdx).scale(1.0f, 0.3f, 1.0f);
                        arms.get(armIdx).get(segIdx).interpolation(3, 0);
                        grownSegments = armProgress + 1;
                        DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.3f, 0.7f);
                    }

                    // End cap appears after arm finishes
                    if (segIdx == 3 && armIdx < endCaps.size()) {
                        endCaps.get(armIdx).scale(1.0f, 0.5f, 1.0f);
                        endCaps.get(armIdx).interpolation(3, 0);
                    }
                }
            }

            // After full formation: node slow Y rotation
            if (ticksAlive > 70) {
                float nodeRot = (ticksAlive - 70) * 0.00175f;
                for (BlockDisplayHandle node : centralNode) {
                    node.rotate(nodeRot, 0, 1, 0);
                    node.interpolation(5, 0);
                }
            }

            // Arm blocks gentle breathing
            if (ticksAlive > 70) {
                for (List<BlockDisplayHandle> arm : arms) {
                    for (int i = 0; i < arm.size(); i++) {
                        float breath = 0.05f * (float) Math.sin(ticksAlive * 2 * Math.PI / 120);
                        arm.get(i).translate(-0.5f, -0.5f + breath, -0.5f);
                        arm.get(i).interpolation(5, 0);
                    }
                }
            }

            // Soul fire laser lines along arms (ramping density)
            if (ticksAlive % 3 == 0 && ticksAlive > 20) {
                for (int a = 0; a < arms.size(); a++) {
                    List<BlockDisplayHandle> arm = arms.get(a);
                    Location prev = center.clone().add(0, 0.3, 0);
                    for (int i = 0; i < arm.size(); i++) {
                        if (grownSegments > a * 4 + i) {
                            Location curr = arm.get(i).entity().getLocation().add(0, 0.2, 0);
                            DisplayBuilder.particleLine(prev, curr, Particle.SOUL_FIRE_FLAME, 3, null);
                            prev = curr;
                        }
                    }
                    if (grownSegments >= (a + 1) * 4 && a < endCaps.size()) {
                        Location capLoc = endCaps.get(a).entity().getLocation().add(0, 0.2, 0);
                        DisplayBuilder.particleLine(prev, capLoc, Particle.SOUL_FIRE_FLAME, 3, null);
                    }
                }
            }

            // Crimson spore from central node
            if (ticksAlive % 4 == 0 && ticksAlive > 10) {
                w.spawnParticle(Particle.CRIMSON_SPORE, center.clone().add(0, 0.5, 0), 2, 0.5, 0.3, 0.5, 0.01);
            }

            // ASH from end caps
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle cap : endCaps) {
                    Location capLoc = cap.entity().getLocation().add(0, 0.3, 0);
                    w.spawnParticle(Particle.ASH, capLoc, 1, 0.3, 0.1, 0.3, 0);
                }
            }

            // Ambient
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CharredLatticeSpread(plugin); }
    }

    // ================================================================
    // 43. BRIMSTONE CRUST COLLAPSE -- 3x3 netherrack surface that cracks
    //     and drops into a magma pit with lava column
    // ================================================================
    public static class BrimstoneCrustCollapse extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> surfaceBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> pitBottom = new ArrayList<>();
        private final List<BlockDisplayHandle> rimBlocks = new ArrayList<>();
        private boolean collapsed = false;

        public BrimstoneCrustCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_crust_collapse", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(350);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(4.0);
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 9 netherrack blocks in 3x3 grid at ground level
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x, 0, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                    h.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(2, 0);
                    surfaceBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Pit bottom: 4 magma + 1 soul sand at Y-2
            int[][] magmaPos = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
            for (int[] mp : magmaPos) {
                Location loc = center.clone().add(mp[0] * 0.5, -2, mp[1] * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(255, 100, 0).interpolation(3, 0);
                pitBottom.add(h);
                spawnedEntities.add(h.entity());
            }
            BlockDisplayHandle pitCenter = displayBuilder.spawnBlock(center.clone().add(0, -2, 0), Material.SOUL_SAND);
            pitCenter.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(3, 0);
            pitBottom.add(pitCenter);
            spawnedEntities.add(pitCenter.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1 (0-5): 3x3 blocks appear
            if (ticksAlive <= 5) {
                float scale = ticksAlive / 5.0f;
                for (BlockDisplayHandle block : surfaceBlocks) {
                    block.scale(scale, scale, scale);
                    block.interpolation(2, 0);
                }
            }

            // Phase 2 (10-22): 6 inner blocks drop, 2 ticks each
            if (ticksAlive >= 10 && ticksAlive <= 22 && !collapsed) {
                int dropIdx = (ticksAlive - 10) / 2;
                // Inner 6 blocks (indices 1,3,4,5,7 and center 4 is dropped)
                int[] dropOrder = {0, 1, 2, 3, 5, 8}; // Skip corners 0,2,6,8 -> keep 3 as rim
                // Actually keep indices 0, 6, 2 as rim (corners of 3x3)
                int[] innerIndices = {1, 3, 4, 5, 7, 8};
                if (dropIdx < innerIndices.length) {
                    int idx = innerIndices[dropIdx];
                    if (idx < surfaceBlocks.size()) {
                        BlockDisplay bd = surfaceBlocks.get(idx).entity();
                        float dropProgress = ((ticksAlive - 10) % 2) / 1.0f;
                        Location loc = bd.getLocation();
                        bd.teleport(loc.clone().add(0, -dropProgress, 0));
                        surfaceBlocks.get(idx).scale(1.0f, 0.9f, 1.0f);
                        surfaceBlocks.get(idx).interpolation(2, 0);

                        if (dropIdx == innerIndices.length - 1 && (ticksAlive - 10) % 2 == 1) {
                            collapsed = true;
                            triggerImpactDamage(center);
                            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.4f);
                        }
                    }
                }
            }

            // Show pit bottom after collapse
            if (collapsed && ticksAlive < 30) {
                float pitScale = (ticksAlive - 22) / 8.0f;
                pitScale = Math.min(1.0f, Math.max(0, pitScale));
                for (BlockDisplayHandle pit : pitBottom) {
                    pit.scale(pitScale, pitScale, pitScale);
                    pit.interpolation(3, 0);
                }
            }

            // Rim blocks tilt outward
            if (collapsed) {
                int[] rimIndices = {0, 2, 6};
                float tiltAngle = 0.0873f; // 5 degrees
                for (int i = 0; i < rimIndices.length; i++) {
                    if (rimIndices[i] < surfaceBlocks.size()) {
                        // Change to cracked stone
                        surfaceBlocks.get(rimIndices[i]).rotate(tiltAngle, (i == 0) ? -1 : (i == 1 ? 1 : 0), 0, (i == 2) ? 1 : 0);
                        surfaceBlocks.get(rimIndices[i]).interpolation(5, 0);
                    }
                }
            }

            // Magma pit bottom pulse
            if (collapsed) {
                float pitPulse = 1.0f + 0.05f * (float) Math.sin(ticksAlive * 2 * Math.PI / 30) + 0.05f;
                for (BlockDisplayHandle pit : pitBottom) {
                    pit.scale(pitPulse, pitPulse, pitPulse);
                    pit.interpolation(3, 0);
                }
            }

            // LAVA column from pit
            if (ticksAlive % 3 == 0 && collapsed) {
                w.spawnParticle(Particle.LAVA, center.clone().add(0, 0, 0), 4, 0.5, 0.5, 0.5, 0);
            }

            // FLAME burst from dropped blocks
            if (ticksAlive >= 10 && ticksAlive <= 22 && ticksAlive % 2 == 0) {
                w.spawnParticle(Particle.FLAME, center, 10, 0.5, 0.3, 0.5, 0.02);
            }

            // SMOKE from pit rim
            if (ticksAlive % 4 == 0 && collapsed) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 0.2, 0), 3, 1, 0.1, 1, 0.01);
            }

            // Ambient
            if (ticksAlive % 80 == 0 && collapsed) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCrustCollapse(plugin); }
    }

    // ================================================================
    // 44. TRAIL LATTICE NETWORK -- Network of soul soil nodes connected
    //     by soul fire laser lines across multiple corruption zones
    // ================================================================
    public static class TrailLatticeNetwork extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> networkNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> anchorSwords = new ArrayList<>();
        private BlockDisplayHandle frontCharge;

        public TrailLatticeNetwork(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trail_lattice_network", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Up to 12 soul soil network nodes spread across the area
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                double radius = 2.5 + (i % 3) * 2.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                h.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(3, 0);
                networkNodes.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3 anchor swords at oldest nodes (polished blackstone pillars)
            for (int i = 0; i < 3; i++) {
                double angle = (2 * Math.PI * i) / 3;
                double x = Math.cos(angle) * 6.0;
                double z = Math.sin(angle) * 6.0;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.2f, 2.0f, 0.2f).glow(200, 0, 50).interpolation(3, 0);
                anchorSwords.add(h);
                spawnedEntities.add(h.entity());
            }

            // Front charge: fire charge at newest zone (magma block)
            frontCharge = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.MAGMA_BLOCK);
            frontCharge.scale(2.5f, 2.5f, 2.5f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(frontCharge.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Materialize all nodes over 12 ticks
            if (ticksAlive <= 12) {
                float nodeScale = ticksAlive / 12.0f;
                for (BlockDisplayHandle node : networkNodes) {
                    node.scale(nodeScale, 0.3f * nodeScale, nodeScale);
                    node.interpolation(3, 0);
                }
            }

            // Anchor sword Y-axis rotation: 0.8 deg/tick
            float swordRot = ticksAlive * 0.01396f;
            for (BlockDisplayHandle sword : anchorSwords) {
                sword.rotate(swordRot, 0, 1, 0);
                sword.interpolation(3, 0);
            }

            // Front charge: Y 1.5 deg/tick, X 0.5 deg/tick
            if (frontCharge != null) {
                float fcYRot = ticksAlive * 0.02618f;
                float fcXRot = ticksAlive * 0.00873f;
                frontCharge.rotate(fcYRot + fcXRot, 0.3f, 0.9f, 0);
                frontCharge.interpolation(3, 0);
            }

            // Network node pulse: propagating from oldest to newest
            for (int i = 0; i < networkNodes.size(); i++) {
                float pulse = 1.0f + 0.05f * (float) Math.sin((ticksAlive - i * 4) * 2 * Math.PI / 50) + 0.05f;
                networkNodes.get(i).scale(pulse, 0.3f, pulse);
                networkNodes.get(i).interpolation(3, 0);
            }

            // Soul fire laser lines between adjacent nodes (ramping density)
            float densityRamp = Math.min(40, 15 + ticksAlive / 30.0f);
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < networkNodes.size() - 1; i++) {
                    Location from = networkNodes.get(i).entity().getLocation().add(0, 0.2, 0);
                    Location to = networkNodes.get(i + 1).entity().getLocation().add(0, 0.2, 0);
                    if (from.distanceSquared(to) < 36) {
                        DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 3, null);
                    }
                }
                // Connect last to first
                if (networkNodes.size() >= 2) {
                    Location from = networkNodes.get(networkNodes.size() - 1).entity().getLocation().add(0, 0.2, 0);
                    Location to = networkNodes.get(0).entity().getLocation().add(0, 0.2, 0);
                    DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 3, null);
                }
            }

            // LAVA burst from anchor swords every 20 seconds (400 ticks)
            if (ticksAlive % 400 == 200) {
                for (BlockDisplayHandle sword : anchorSwords) {
                    Location sLoc = sword.entity().getLocation();
                    w.spawnParticle(Particle.LAVA, sLoc, 15, 1, 0.5, 1, 0);
                    DisplayBuilder.playSound(sLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
                }
            }

            // ASH drift from all nodes
            if (ticksAlive % 7 == 0) {
                for (BlockDisplayHandle node : networkNodes) {
                    Location nLoc = node.entity().getLocation().add(0, 0.3, 0);
                    w.spawnParticle(Particle.ASH, nLoc, 1, 0.1, 0.1, 0.1, 0);
                }
            }

            // Ambient
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TrailLatticeNetwork(plugin); }
    }

    // ================================================================
    // 45. NETHER SPORE COLUMN -- 7-block-tall crimson nylium and nether
    //     wart column with tapered stalk and fungal branch arms
    // ================================================================
    public static class NetherSporeColumn extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> stalkBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> branchArms = new ArrayList<>();
        private final List<BlockDisplayHandle> baseSpread = new ArrayList<>();
        private int grownTiers = 0;

        public NetherSporeColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_spore_column", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 7-block column: alternating crimson nylium and nether wart
            float[] widths = {1.0f, 1.0f, 0.9f, 0.9f, 0.8f, 0.8f, 0.7f};
            for (int i = 0; i < 7; i++) {
                Material mat = (i % 2 == 0) ? Material.CRIMSON_NYLIUM : Material.NETHER_WART_BLOCK;
                Location loc = center.clone().add(0, i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(3, 0);
                stalkBlocks.add(h);
                spawnedEntities.add(h.entity());

                // Branch arms at nether wart tiers (odd indices)
                if (i % 2 == 1) {
                    for (int side = -1; side <= 1; side += 2) {
                        Location branchLoc = center.clone().add(side * 0.8, i, 0);
                        BlockDisplayHandle branch = displayBuilder.spawnBlock(branchLoc, Material.NETHER_WART_BLOCK);
                        branch.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50)
                              .rotate(side * 0.785f, 0, 0, 1)
                              .interpolation(3, 0);
                        branchArms.add(branch);
                        spawnedEntities.add(branch.entity());
                    }
                }
            }

            // 3x3 base crimson nylium spread
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    Location loc = center.clone().add(x, 0, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRIMSON_NYLIUM);
                    h.scale(1.0f, 0.3f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                    baseSpread.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Growth: each tier over 8 ticks, next at 50% height
            int tierProgress = ticksAlive / 8;
            if (tierProgress < 7 && tierProgress >= grownTiers) {
                float[] widths = {1.0f, 1.0f, 0.9f, 0.9f, 0.8f, 0.8f, 0.7f};
                int localTick = ticksAlive % 8;
                float growScale = localTick / 7.0f;

                stalkBlocks.get(tierProgress).scale(widths[tierProgress] * growScale, growScale, widths[tierProgress] * growScale);
                stalkBlocks.get(tierProgress).interpolation(3, 0);

                if (localTick == 7) {
                    grownTiers = tierProgress + 1;
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.6f);
                }
            }

            // Branch arms appear with their tier
            if (grownTiers > 0) {
                for (int i = 0; i < branchArms.size(); i++) {
                    int tierIdx = (i / 2) * 2 + 1; // Maps to tier 1,3,5
                    if (tierIdx < grownTiers) {
                        branchArms.get(i).scale(0.6f, 0.6f, 0.6f);
                        branchArms.get(i).interpolation(3, 0);
                    }
                }
            }

            // After full growth: column sway
            if (grownTiers >= 7) {
                float sway = (float) Math.sin(ticksAlive * 2 * Math.PI / 100) * 0.0698f;
                for (BlockDisplayHandle block : stalkBlocks) {
                    block.rotate(sway, 0, 0, 1);
                    block.interpolation(5, 0);
                }

                // Branch arm oscillation
                for (int i = 0; i < branchArms.size(); i++) {
                    int pairIdx = i / 2;
                    float armOsc = (float) Math.sin((ticksAlive + pairIdx * 20) * 2 * Math.PI / 60) * 0.0873f;
                    int side = (i % 2 == 0) ? -1 : 1;
                    branchArms.get(i).rotate(side * 0.785f + armOsc, 0, 0, 1);
                    branchArms.get(i).interpolation(5, 0);
                }

                // Tip pulse
                if (!stalkBlocks.isEmpty()) {
                    float tipPulse = 0.7f + 0.1f * (float) Math.sin(ticksAlive * 2 * Math.PI / 70) + 0.1f;
                    stalkBlocks.get(6).scale(tipPulse, tipPulse, tipPulse);
                    stalkBlocks.get(6).interpolation(5, 0);
                }
            }

            // Crimson spore from branch arms
            if (ticksAlive % 4 == 0 && grownTiers >= 3) {
                for (BlockDisplayHandle arm : branchArms) {
                    Location armLoc = arm.entity().getLocation();
                    w.spawnParticle(Particle.CRIMSON_SPORE, armLoc, 2, 0.4, 0.2, 0.4, 0.01);
                }
            }

            // Crimson spore from base spread
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle base : baseSpread) {
                    Location bLoc = base.entity().getLocation().add(0, 0.2, 0);
                    w.spawnParticle(Particle.CRIMSON_SPORE, bLoc, 2, 0.3, 0.1, 0.3, 0.01);
                }
            }

            // FLAME from tip
            if (ticksAlive % 5 == 0 && grownTiers >= 7) {
                Location tipLoc = stalkBlocks.get(6).entity().getLocation().add(0, 0.8, 0);
                w.spawnParticle(Particle.FLAME, tipLoc, 1, 0.05, 0.3, 0.05, 0.01);
            }

            // Ambient
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NetherSporeColumn(plugin); }
    }

    // ================================================================
    // 46. SOOT STREAK MARKER -- Directional ground stain of black
    //     concrete between corruption zones with terminus anchors
    // ================================================================
    public static class SootStreakMarker extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> stainBlocks = new ArrayList<>();
        private BlockDisplayHandle departureAnchor;
        private BlockDisplayHandle arrivalAnchor;
        private int revealedStains = 0;

        public SootStreakMarker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soot_streak_marker", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Departure terminus: soul sand full height
            departureAnchor = displayBuilder.spawnBlock(center.clone().add(-4, 0, 0), Material.SOUL_SAND);
            departureAnchor.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(3, 0);
            spawnedEntities.add(departureAnchor.entity());

            // 6 black concrete flat stains between zones
            for (int i = 0; i < 6; i++) {
                double xOff = -3 + i * 1.2;
                Location loc = center.clone().add(xOff, 0.1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(3, 0);
                stainBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Arrival terminus: magma block full height
            arrivalAnchor = displayBuilder.spawnBlock(center.clone().add(4, 0, 0), Material.MAGMA_BLOCK);
            arrivalAnchor.scale(0.0f, 0.0f, 0.0f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(arrivalAnchor.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Departure terminus appears first
            if (ticksAlive <= 4) {
                float scale = ticksAlive / 4.0f;
                if (departureAnchor != null) {
                    departureAnchor.scale(scale, scale, scale);
                    departureAnchor.interpolation(3, 0);
                }
            }

            // Stain blocks appear one by one, 4 ticks apart, 6 ticks spacing
            if (ticksAlive > 4) {
                int stainIdx = (ticksAlive - 4) / 6;
                if (stainIdx < stainBlocks.size() && stainIdx >= revealedStains) {
                    int localTick = (ticksAlive - 4) % 6;
                    if (localTick <= 4) {
                        float stainScale = localTick / 4.0f;
                        stainBlocks.get(stainIdx).scale(stainScale, 0.1f * stainScale, stainScale);
                        stainBlocks.get(stainIdx).interpolation(3, 0);
                    }
                    if (localTick == 4) {
                        revealedStains = stainIdx + 1;
                    }
                }
            }

            // Arrival anchor appears after all stains
            if (ticksAlive > 40 && arrivalAnchor != null) {
                float arrScale = Math.min(1.0f, (ticksAlive - 40) / 4.0f);
                arrivalAnchor.scale(arrScale, arrScale, arrScale);
                arrivalAnchor.interpolation(3, 0);
            }

            // Stain seething: Y-scale oscillation
            for (int i = 0; i < revealedStains; i++) {
                float seethe = 0.1f + 0.035f * (float) Math.sin((ticksAlive + i * 5) * 2 * Math.PI / 30);
                stainBlocks.get(i).scale(1.0f, seethe, 1.0f);
                stainBlocks.get(i).interpolation(3, 0);
            }

            // Departure soul sand spin at 0.5 deg/tick
            if (departureAnchor != null && ticksAlive > 4) {
                float depRot = ticksAlive * 0.00873f;
                departureAnchor.rotate(depRot, 0, 1, 0);
                departureAnchor.interpolation(3, 0);
            }

            // Arrival magma pulse: 1.0 to 1.2
            if (arrivalAnchor != null && ticksAlive > 44) {
                float arrPulse = 1.0f + 0.1f * (float) Math.sin(ticksAlive * 2 * Math.PI / 40) + 0.1f;
                arrivalAnchor.scale(arrPulse, arrPulse, arrPulse);
                arrivalAnchor.interpolation(3, 0);
            }

            // ASH from stain blocks
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < revealedStains; i++) {
                    Location sLoc = stainBlocks.get(i).entity().getLocation().add(0, 0.1, 0);
                    w.spawnParticle(Particle.ASH, sLoc, 1, 0.2, 0.05, 0.2, 0);
                }
            }

            // Soul fire laser along entire path
            if (ticksAlive % 4 == 0 && revealedStains > 0 && departureAnchor != null && arrivalAnchor != null) {
                Location from = departureAnchor.entity().getLocation().add(0, 0.15, 0);
                Location to = arrivalAnchor.entity().getLocation().add(0, 0.15, 0);
                DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 3, null);
            }

            // SMOKE from arrival
            if (ticksAlive % 4 == 0 && arrivalAnchor != null && ticksAlive > 44) {
                Location arrLoc = arrivalAnchor.entity().getLocation().add(0, 0.5, 0);
                w.spawnParticle(Particle.SMOKE, arrLoc, 1, 0.2, 0.3, 0.2, 0.01);
            }

            // Ambient
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SootStreakMarker(plugin); }
    }

    // ================================================================
    // 47. MOLTEN ROOT WEB -- Magma root nodes connected by basalt root
    //     veins between corruption zones with a fire charge heartnode
    // ================================================================
    public static class MoltenRootWeb extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> rootNodes = new ArrayList<>();
        private final List<BlockDisplayHandle> connectorRoots = new ArrayList<>();
        private final List<BlockDisplayHandle> branchOffshoots = new ArrayList<>();
        private BlockDisplayHandle heartnode;
        private int grownConnectors = 0;

        public MoltenRootWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("molten_root_web", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3 root nodes in a triangle, 3 magma blocks each
            double[][] nodePositions = {{0, 4}, {-3.5, -2}, {3.5, -2}};
            for (double[] nodePos : nodePositions) {
                List<BlockDisplayHandle> node = new ArrayList<>();
                for (int j = 0; j < 3; j++) {
                    double jx = (j - 1) * 0.6;
                    Location loc = center.clone().add(nodePos[0] + jx, 0, nodePos[1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    h.scale(1.0f, 0.5f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                    node.add(h);
                    spawnedEntities.add(h.entity());
                }
                rootNodes.add(node);
            }

            // 12 connector roots (4 per edge of triangle) - basalt, thin
            for (int edge = 0; edge < 3; edge++) {
                double[] from = nodePositions[edge];
                double[] to = nodePositions[(edge + 1) % 3];
                for (int seg = 1; seg <= 4; seg++) {
                    double t = seg / 5.0;
                    double x = from[0] + (to[0] - from[0]) * t;
                    double z = from[1] + (to[1] - from[1]) * t;
                    double yVariation = (seg == 2) ? 0.3 : ((seg == 3) ? -0.15 : 0);
                    Location loc = center.clone().add(x, yVariation, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                    h.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(3, 0);
                    connectorRoots.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Branch offshoots on 3 connectors
            for (int i = 0; i < 3; i++) {
                int connIdx = i * 4 + 2;
                if (connIdx < connectorRoots.size()) {
                    Location connLoc = connectorRoots.get(connIdx).entity().getLocation();
                    Location branchLoc = connLoc.clone().add(0.6, 0, 0.6);
                    BlockDisplayHandle branch = displayBuilder.spawnBlock(branchLoc, Material.POLISHED_BLACKSTONE);
                    branch.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50)
                          .rotate(0.785f, 0, 0, 1)
                          .interpolation(3, 0);
                    branchOffshoots.add(branch);
                    spawnedEntities.add(branch.entity());
                }
            }

            // Heartnode: fire charge at first node (magma block, oversized)
            heartnode = displayBuilder.spawnBlock(center.clone().add(nodePositions[0][0], 0.5, nodePositions[0][1]), Material.MAGMA_BLOCK);
            heartnode.scale(1.8f, 1.8f, 1.8f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(heartnode.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Connectors grow from source nodes
            if (ticksAlive > 10) {
                int connProgress = (ticksAlive - 10) / 4;
                if (connProgress < connectorRoots.size() && connProgress >= grownConnectors) {
                    connectorRoots.get(connProgress).scale(0.5f, 0.5f, 1.0f);
                    connectorRoots.get(connProgress).interpolation(3, 0);
                    grownConnectors = connProgress + 1;
                }
            }

            // Branch offshoots appear after their connector
            for (int i = 0; i < branchOffshoots.size(); i++) {
                int requiredConn = i * 4 + 3;
                if (grownConnectors > requiredConn) {
                    branchOffshoots.get(i).scale(0.5f, 0.5f, 0.5f);
                    branchOffshoots.get(i).interpolation(3, 0);
                }
            }

            // Node and connector breathing pulse
            if (ticksAlive > 60) {
                float pulse = 1.0f + 0.075f * (float) Math.sin(ticksAlive * 2 * Math.PI / 80) + 0.075f;
                for (List<BlockDisplayHandle> node : rootNodes) {
                    for (BlockDisplayHandle block : node) {
                        block.scale(pulse, 0.5f, pulse);
                        block.interpolation(3, 0);
                    }
                }
            }

            // Heartnode: Y rotation 1.2 deg/tick, X oscillation
            if (heartnode != null) {
                float hRot = ticksAlive * 0.02094f;
                float hOsc = (float) Math.sin(ticksAlive * 2 * Math.PI / 60) * 0.0873f;
                heartnode.rotate(hRot + hOsc, 0.1f, 0.9f, 0);
                heartnode.interpolation(3, 0);
            }

            // Branch offshoot vibration
            for (BlockDisplayHandle branch : branchOffshoots) {
                float vibX = 0.05f * (float) Math.sin(ticksAlive * 2 * Math.PI / 8);
                branch.translate(-0.5f + vibX, -0.5f, -0.5f);
                branch.interpolation(2, 0);
            }

            // LAVA from root nodes
            if (ticksAlive % 4 == 0) {
                for (List<BlockDisplayHandle> node : rootNodes) {
                    for (BlockDisplayHandle block : node) {
                        Location bLoc = block.entity().getLocation().add(0, 0.3, 0);
                        w.spawnParticle(Particle.LAVA, bLoc, 1, 0.3, 0.1, 0.3, 0);
                    }
                }
            }

            // FLAME along connectors
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < grownConnectors; i++) {
                    Location cLoc = connectorRoots.get(i).entity().getLocation().add(0, 0.2, 0);
                    w.spawnParticle(Particle.FLAME, cLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Dripping lava from heartnode
            if (ticksAlive % 7 == 0 && heartnode != null) {
                Location hLoc = heartnode.entity().getLocation();
                w.spawnParticle(Particle.DRIPPING_LAVA, hLoc, 1, 0, 0, 0, 0);
            }

            // Heartnode radial LAVA burst every 15 seconds
            if (ticksAlive % 300 == 0 && ticksAlive > 0 && heartnode != null) {
                Location hLoc = heartnode.entity().getLocation();
                DisplayBuilder.playSound(hLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.9f);
                w.spawnParticle(Particle.LAVA, hLoc, 20, 2, 1, 2, 0);
            }

            // Ambient
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoltenRootWeb(plugin); }
    }

    // ================================================================
    // 48. SOUL WITHER GLYPH -- 5x5 ground-level glyph with soul sand
    //     frame, soul soil diamond, wither skull center, and diagonal
    //     laser X pattern
    // ================================================================
    public static class SoulWitherGlyph extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerDiamond = new ArrayList<>();
        private final List<BlockDisplayHandle> basaltInlays = new ArrayList<>();
        private BlockDisplayHandle skullCenter;
        private final List<BlockDisplayHandle> cornerOrbs = new ArrayList<>();

        public SoulWitherGlyph(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_wither_glyph", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring: 8 soul sand in a square frame at ground level
            int[][] ringPos = {
                {-2, -2}, {-1, -2}, {0, -2}, {1, -2},
                {2, -2}, {2, 0}, {-2, 0}, {-2, 2}
            };
            // Full outer ring (12 positions for a 5x5 frame)
            int[][] fullRing = {
                {-2,-2},{-1,-2},{0,-2},{1,-2},{2,-2},
                {2,-1},{2,0},{2,1},{2,2},
                {1,2},{0,2},{-1,2},{-2,2},
                {-2,1},{-2,0},{-2,-1}
            };
            // Just use 8 evenly spaced for soul sand
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 2.2;
                double z = Math.sin(angle) * 2.2;
                Location loc = center.clone().add(x, 0.1, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SAND);
                h.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(3, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner diamond: 4 soul soil blocks
            int[][] diamondPos = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
            for (int[] dp : diamondPos) {
                Location loc = center.clone().add(dp[0], 0.1, dp[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                h.scale(0.0f, 0.0f, 0.0f).glow(0, 150, 255).interpolation(3, 0);
                innerDiamond.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 basalt inlays at inner cardinal positions
            int[][] basaltPos = {{1, -1}, {1, 1}, {-1, 1}, {-1, -1}};
            for (int[] bp : basaltPos) {
                Location loc = center.clone().add(bp[0], 0.1, bp[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BASALT);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(3, 0);
                basaltInlays.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center: wither skull (represented as nether wart block, oversized)
            skullCenter = displayBuilder.spawnBlock(center.clone().add(0, 0.2, 0), Material.NETHER_WART_BLOCK);
            skullCenter.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(skullCenter.entity());

            // 2 corner orbs at NE and SW (magma block for magma cream)
            Location ne = center.clone().add(2.2, 0.2, -2.2);
            BlockDisplayHandle neOrb = displayBuilder.spawnBlock(ne, Material.MAGMA_BLOCK);
            neOrb.scale(0.0f, 0.0f, 0.0f).glow(255, 100, 0).interpolation(3, 0);
            cornerOrbs.add(neOrb);
            spawnedEntities.add(neOrb.entity());

            Location sw = center.clone().add(-2.2, 0.2, 2.2);
            BlockDisplayHandle swOrb = displayBuilder.spawnBlock(sw, Material.MAGMA_BLOCK);
            swOrb.scale(0.0f, 0.0f, 0.0f).glow(255, 100, 0).interpolation(3, 0);
            cornerOrbs.add(swOrb);
            spawnedEntities.add(swOrb.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1 (0-8): All blocks stamp in
            if (ticksAlive <= 8) {
                float stampScale = ticksAlive / 8.0f;
                for (BlockDisplayHandle ring : outerRing) {
                    ring.scale(stampScale, 0.3f * stampScale, stampScale);
                    ring.interpolation(2, 0);
                }
                for (BlockDisplayHandle diamond : innerDiamond) {
                    diamond.scale(stampScale, 0.3f * stampScale, stampScale);
                    diamond.interpolation(2, 0);
                }
                for (BlockDisplayHandle basalt : basaltInlays) {
                    basalt.scale(stampScale, 0.3f * stampScale, stampScale);
                    basalt.interpolation(2, 0);
                }
                for (BlockDisplayHandle orb : cornerOrbs) {
                    orb.scale(0.9f * stampScale, 0.9f * stampScale, 0.9f * stampScale);
                    orb.interpolation(2, 0);
                }
            }

            // Phase 2 (8-28): Skull rises from flat to full 3D
            if (ticksAlive >= 8 && ticksAlive <= 28 && skullCenter != null) {
                float skullProgress = (ticksAlive - 8) / 20.0f;
                float xzScale = 3.0f;
                float yScale = 3.0f * skullProgress;
                skullCenter.scale(xzScale, Math.max(0.1f, yScale), xzScale);
                skullCenter.interpolation(3, 0);
            }

            // Skull Y-rotation: 0.3 deg/tick, X-axis pendulum
            if (ticksAlive > 28 && skullCenter != null) {
                float skullRot = ticksAlive * 0.00524f;
                float skullNod = (float) Math.sin(ticksAlive * 2 * Math.PI / 90) * 0.1745f;
                skullCenter.rotate(skullRot + skullNod, 0.5f, 0.9f, 0);
                skullCenter.interpolation(3, 0);
            }

            // Outer ring: slow Y rotation as unit
            float ringRot = ticksAlive * 0.00175f;
            for (BlockDisplayHandle ring : outerRing) {
                ring.rotate(ringRot, 0, 1, 0);
                ring.interpolation(5, 0);
            }

            // Basalt inlay: staggered upward wave
            for (int i = 0; i < basaltInlays.size(); i++) {
                float upWave = 0.1f * (float) Math.sin((ticksAlive + i * 12.5) * 2 * Math.PI / 50);
                basaltInlays.get(i).translate(-0.5f, -0.5f + upWave, -0.5f);
                basaltInlays.get(i).interpolation(3, 0);
            }

            // Soul fire from outer ring
            if (ticksAlive % 3 == 0 && ticksAlive > 8) {
                for (BlockDisplayHandle ring : outerRing) {
                    Location rLoc = ring.entity().getLocation().add(0, 0.3, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, rLoc, 1, 0.1, 0.2, 0.1, 0.01);
                }
            }

            // Diagonal X laser lines across the glyph
            if (ticksAlive % 3 == 0 && ticksAlive > 8) {
                Location neLoc = center.clone().add(2.2, 0.15, -2.2);
                Location swLoc = center.clone().add(-2.2, 0.15, 2.2);
                Location nwLoc = center.clone().add(-2.2, 0.15, -2.2);
                Location seLoc = center.clone().add(2.2, 0.15, 2.2);
                DisplayBuilder.particleLine(neLoc, swLoc, Particle.SOUL_FIRE_FLAME, 4, null);
                DisplayBuilder.particleLine(nwLoc, seLoc, Particle.SOUL_FIRE_FLAME, 4, null);
            }

            // ASH from skull
            if (ticksAlive % 4 == 0 && ticksAlive > 28) {
                w.spawnParticle(Particle.ASH, center.clone().add(0, 0.8, 0), 2, 0.5, 0.2, 0.5, 0);
            }

            // SMOKE from basalt inlays
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle basalt : basaltInlays) {
                    Location bLoc = basalt.entity().getLocation().add(0, 0.3, 0);
                    w.spawnParticle(Particle.SMOKE, bLoc, 1, 0.1, 0.2, 0.1, 0.01);
                }
            }

            // Wither pulse at skull nod peak: every 90 ticks
            if (ticksAlive % 90 == 45 && ticksAlive > 28) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.4f);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 15, 2.5, 0, 150, 255, 1.5f);
            }

            // Ambient
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulWitherGlyph(plugin); }
    }

    // ================================================================
    // 49. CORRUPTION SINKHOLE CROWN -- Large obsidian column ring with
    //     crying obsidian inlays, soul soil floor, shield center, and
    //     blaze rod orbit
    // ================================================================
    public static class CorruptionSinkholeCrown extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerColumns = new ArrayList<>();
        private final List<BlockDisplayHandle> cryingInlays = new ArrayList<>();
        private final List<BlockDisplayHandle> floorTiles = new ArrayList<>();
        private BlockDisplayHandle centralShield;
        private final List<BlockDisplayHandle> blazeOrbiters = new ArrayList<>();

        public CorruptionSinkholeCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_sinkhole_crown", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(10.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(1200);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 obsidian columns in circle, radius 4, each 3 tall, starting underground
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add(x, -4 + y, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                    outerColumns.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 8 crying obsidian inlays between columns at mid-height
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8 + Math.PI / 8;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                Location loc = center.clone().add(x, -3, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
                cryingInlays.add(h);
                spawnedEntities.add(h.entity());
            }

            // 12 soul soil floor tiles inside the crown
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x * 1.3, -4, z * 1.3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                    h.scale(1.0f, 0.3f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
                    floorTiles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Additional floor tiles
            double[][] extraFloor = {{2,0}, {-2,0}, {0,2}, {0,-2}};
            for (double[] ef : extraFloor) {
                Location loc = center.clone().add(ef[0], -4, ef[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                h.scale(1.0f, 0.3f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
                floorTiles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central shield (polished blackstone block, shield-like)
            centralShield = displayBuilder.spawnBlock(center.clone().add(0, -3.5, 0), Material.POLISHED_BLACKSTONE);
            centralShield.scale(3.5f, 3.5f, 0.3f).glow(0, 150, 255).interpolation(3, 0);
            spawnedEntities.add(centralShield.entity());

            // 4 blaze rod orbiters (thin netherrack pillars)
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                double x = Math.cos(angle) * 1.0;
                double z = Math.sin(angle) * 1.0;
                Location loc = center.clone().add(x, -3.5, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(0.3f, 2.0f, 0.3f).glow(255, 100, 0).interpolation(3, 0);
                blazeOrbiters.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1 (0-35): Columns rise from underground
            if (ticksAlive <= 35) {
                float emerge = ticksAlive / 35.0f;
                float yOffset = emerge * 4.3f;
                for (int i = 0; i < outerColumns.size(); i++) {
                    BlockDisplay bd = outerColumns.get(i).entity();
                    int colIdx = i / 3;
                    int tier = i % 3;
                    double angle = (2 * Math.PI * colIdx) / 8;
                    double x = Math.cos(angle) * 4.0;
                    double z = Math.sin(angle) * 4.0;
                    bd.teleport(center.clone().add(x, -4 + tier + yOffset, z));
                }

                // Sound per column emerging
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.4f, 0.5f);
                }
            }

            // Crying obsidian inlays rise at tick 20
            if (ticksAlive >= 20 && ticksAlive <= 35) {
                float inlayEmerge = (ticksAlive - 20) / 15.0f;
                float inlayY = inlayEmerge * 4.3f;
                for (int i = 0; i < cryingInlays.size(); i++) {
                    double angle = (2 * Math.PI * i) / 8 + Math.PI / 8;
                    double x = Math.cos(angle) * 4.0;
                    double z = Math.sin(angle) * 4.0;
                    cryingInlays.get(i).entity().teleport(center.clone().add(x, -3 + inlayY, z));
                }
            }

            // Floor tiles at tick 30
            if (ticksAlive >= 30 && ticksAlive <= 35) {
                float floorEmerge = (ticksAlive - 30) / 5.0f;
                float floorY = floorEmerge * 4.3f;
                for (BlockDisplayHandle tile : floorTiles) {
                    BlockDisplay bd = tile.entity();
                    Location loc = bd.getLocation();
                    bd.teleport(loc.clone().add(0, floorY / 5.0, 0));
                }
            }

            // Center shield and blaze rods at tick 40
            if (ticksAlive >= 40 && ticksAlive <= 45) {
                float centerEmerge = (ticksAlive - 40) / 5.0f;
                if (centralShield != null) {
                    centralShield.entity().teleport(center.clone().add(0, 0.5, 0));
                }
                for (int i = 0; i < blazeOrbiters.size(); i++) {
                    double angle = (2 * Math.PI * i) / 4;
                    double x = Math.cos(angle) * 1.0;
                    double z = Math.sin(angle) * 1.0;
                    blazeOrbiters.get(i).entity().teleport(center.clone().add(x, 0.5, z));
                }
            }

            // After formation: blaze rods orbit at 0.5 deg/tick
            if (ticksAlive > 45) {
                float orbitRot = ticksAlive * 0.00873f;
                for (int i = 0; i < blazeOrbiters.size(); i++) {
                    double angle = (2 * Math.PI * i) / 4 + orbitRot;
                    double x = Math.cos(angle) * 1.0;
                    double z = Math.sin(angle) * 1.0;
                    blazeOrbiters.get(i).entity().teleport(center.clone().add(x, 0.5, z));
                    blazeOrbiters.get(i).interpolation(3, 0);
                }
            }

            // Shield: Y rotation 1.0 deg/tick, Z oscillation
            if (ticksAlive > 45 && centralShield != null) {
                float shieldRot = ticksAlive * 0.01745f;
                float shieldOsc = (float) Math.sin(ticksAlive * 2 * Math.PI / 60) * 0.0873f;
                centralShield.rotate(shieldRot + shieldOsc, 0, 0.9f, 0.1f);
                centralShield.interpolation(3, 0);
            }

            // Obsidian column slow individual Y rotation
            if (ticksAlive > 45) {
                float colRot = ticksAlive * 0.00262f;
                for (BlockDisplayHandle col : outerColumns) {
                    col.rotate(colRot, 0, 1, 0);
                    col.interpolation(5, 0);
                }
            }

            // Soul fire from crying obsidian inlays directed inward
            if (ticksAlive % 3 == 0 && ticksAlive > 35) {
                for (BlockDisplayHandle inlay : cryingInlays) {
                    Location iLoc = inlay.entity().getLocation();
                    double dx = center.getX() - iLoc.getX();
                    double dz = center.getZ() - iLoc.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, iLoc.clone().add(0, 0.5, 0), 2,
                                dx / dist * 0.5, 0.05, dz / dist * 0.5, 0.02);
                    }
                }
            }

            // LAVA from floor
            if (ticksAlive % 4 == 0 && ticksAlive > 35) {
                w.spawnParticle(Particle.LAVA, center.clone().add(0, 0.3, 0), 2, 1.5, 0.1, 1.5, 0);
            }

            // FLAME from blaze rods
            if (ticksAlive % 3 == 0 && ticksAlive > 45) {
                for (BlockDisplayHandle rod : blazeOrbiters) {
                    Location rLoc = rod.entity().getLocation().add(0, 1.5, 0);
                    w.spawnParticle(Particle.FLAME, rLoc, 2, 0.1, 0.3, 0.1, 0.01);
                }
            }

            // Soul fire laser ring at column tops
            if (ticksAlive % 4 == 0 && ticksAlive > 35) {
                for (int i = 0; i < 8; i++) {
                    int fromIdx = i * 3 + 2;
                    int toIdx = ((i + 1) % 8) * 3 + 2;
                    if (fromIdx < outerColumns.size() && toIdx < outerColumns.size()) {
                        Location from = outerColumns.get(fromIdx).entity().getLocation().add(0, 1, 0);
                        Location to = outerColumns.get(toIdx).entity().getLocation().add(0, 1, 0);
                        DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 3, null);
                    }
                }
            }

            // Ambient
            if (ticksAlive % 200 == 0 && ticksAlive > 45) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionSinkholeCrown(plugin); }
    }

    // ================================================================
    // 50. FINAL TRAIL IGNITION -- Activated at 10% HP. Pillars erupt on
    //     every corruption zone with dual-height laser grid networks
    // ================================================================
    public static class FinalTrailIgnition extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillars = new ArrayList<>();
        private final List<BlockDisplayHandle> fireChargeTops = new ArrayList<>();
        private final List<BlockDisplayHandle> groundCollars = new ArrayList<>();
        private boolean ashStormActive = false;

        public FinalTrailIgnition(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("final_trail_ignition", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(2400); // Does not expire normally; long duration
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn up to 16 pillars across the arena (budget limit: ~150 block displays)
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                double radius = 4 + (i % 4) * 3.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                // 3-block pillar: netherrack base, magma mid, soul soil cap
                Material[] pillarMats = {Material.NETHERRACK, Material.MAGMA_BLOCK, Material.SOUL_SOIL};
                for (int y = 0; y < 3; y++) {
                    Location loc = center.clone().add(x, y, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, pillarMats[y]);
                    h.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(3, 0);
                    pillars.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Fire charge top (magma block, oversized)
                Location topLoc = center.clone().add(x, 3, z);
                BlockDisplayHandle top = displayBuilder.spawnBlock(topLoc, Material.MAGMA_BLOCK);
                top.scale(0.0f, 0.0f, 0.0f).glow(255, 100, 0).interpolation(3, 0);
                fireChargeTops.add(top);
                spawnedEntities.add(top.entity());

                // Crimson nylium ground collar
                Location collarLoc = center.clone().add(x, 0, z);
                BlockDisplayHandle collar = displayBuilder.spawnBlock(collarLoc, Material.CRIMSON_NYLIUM);
                collar.scale(0.0f, 0.0f, 0.0f).glow(200, 0, 50).interpolation(3, 0);
                groundCollars.add(collar);
                spawnedEntities.add(collar.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Eruption: all pillars from 0.0 to 1.0 scale over 15 ticks
            if (ticksAlive <= 15) {
                float eruptScale = ticksAlive / 15.0f;
                for (BlockDisplayHandle pillar : pillars) {
                    pillar.scale(eruptScale, eruptScale, eruptScale);
                    pillar.interpolation(2, 0);
                }
                for (BlockDisplayHandle top : fireChargeTops) {
                    top.scale(2.0f * eruptScale, 2.0f * eruptScale, 2.0f * eruptScale);
                    top.interpolation(2, 0);
                }
                for (BlockDisplayHandle collar : groundCollars) {
                    collar.scale(1.3f * eruptScale, 0.3f * eruptScale, 1.3f * eruptScale);
                    collar.interpolation(2, 0);
                }
                if (ticksAlive == 15) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.5f);
                }
            }

            // Fire charge tops: individual Y rotation at 1.5 deg/tick
            float topRot = ticksAlive * 0.02618f;
            for (BlockDisplayHandle top : fireChargeTops) {
                top.rotate(topRot, 0, 1, 0);
                top.interpolation(3, 0);
            }

            // Netherrack base blocks: synchronized pulse
            float basePulse = 1.0f + 0.075f * (float) Math.sin(ticksAlive * 2 * Math.PI / 20) + 0.075f;
            for (int i = 0; i < pillars.size(); i += 3) {
                pillars.get(i).scale(basePulse, basePulse, basePulse);
                pillars.get(i).interpolation(3, 0);
            }

            // Pillar bob: each offset 4 ticks
            for (int p = 0; p < fireChargeTops.size(); p++) {
                float bobY = 0.2f * (float) Math.sin((ticksAlive + p * 4) * 2 * Math.PI / 80);
                // Update pillar blocks
                for (int y = 0; y < 3; y++) {
                    int idx = p * 3 + y;
                    if (idx < pillars.size()) {
                        int pillarIdx = p;
                        double angle = (2 * Math.PI * pillarIdx) / 16;
                        double radius = 4 + (pillarIdx % 4) * 3.0;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        pillars.get(idx).entity().teleport(center.clone().add(x, y + bobY, z));
                    }
                }
                // Update fire charge top
                int topP = p;
                double angle = (2 * Math.PI * topP) / 16;
                double radius = 4 + (topP % 4) * 3.0;
                double topX = Math.cos(angle) * radius;
                double topZ = Math.sin(angle) * radius;
                fireChargeTops.get(p).entity().teleport(center.clone().add(topX, 3 + bobY, topZ));
            }

            // LAVA laser lines at 3-block height between nearest pillars (intensifying)
            if (ticksAlive % 3 == 0 && ticksAlive > 15) {
                for (int i = 0; i < fireChargeTops.size(); i++) {
                    int next = (i + 1) % fireChargeTops.size();
                    Location from = fireChargeTops.get(i).entity().getLocation();
                    Location to = fireChargeTops.get(next).entity().getLocation();
                    if (from.distanceSquared(to) < 100) {
                        DisplayBuilder.particleLine(from, to, Particle.LAVA, 3, null);
                    }
                }
            }

            // Soul fire laser lines at ground level
            if (ticksAlive % 4 == 0 && ticksAlive > 15) {
                for (int i = 0; i < groundCollars.size(); i++) {
                    int next = (i + 1) % groundCollars.size();
                    Location from = groundCollars.get(i).entity().getLocation().add(0, 0.2, 0);
                    Location to = groundCollars.get(next).entity().getLocation().add(0, 0.2, 0);
                    if (from.distanceSquared(to) < 100) {
                        DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 3, null);
                    }
                }
            }

            // Crimson spore from ground collars
            if (ticksAlive % 3 == 0 && ticksAlive > 15) {
                for (BlockDisplayHandle collar : groundCollars) {
                    Location cLoc = collar.entity().getLocation().add(0, 0.2, 0);
                    w.spawnParticle(Particle.CRIMSON_SPORE, cLoc, 1, 0.3, 0.1, 0.3, 0.01);
                }
            }

            // FLAME from fire charge tops
            if (ticksAlive % 3 == 0 && ticksAlive > 15) {
                for (BlockDisplayHandle top : fireChargeTops) {
                    Location tLoc = top.entity().getLocation().add(0, 0.5, 0);
                    w.spawnParticle(Particle.FLAME, tLoc, 2, 0.1, 0.5, 0.1, 0.02);
                }
            }

            // ASH storm at 20 seconds in (400 ticks)
            if (ticksAlive >= 400 && !ashStormActive) {
                ashStormActive = true;
            }
            if (ashStormActive && ticksAlive % 2 == 0) {
                w.spawnParticle(Particle.ASH, center.clone().add(0, 6, 0), 10, 10, 2, 10, 0.05);
            }

            // Fire charge burst every 500 ticks (25 seconds)
            if (ticksAlive % 500 == 250 && ticksAlive > 15) {
                for (BlockDisplayHandle top : fireChargeTops) {
                    Location tLoc = top.entity().getLocation();
                    w.spawnParticle(Particle.LAVA, tLoc, 10, 1, 0.5, 1, 0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
            }

            // Ambient
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.5f);
            }
            if (ticksAlive % 80 == 0 && ticksAlive > 15) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FinalTrailIgnition(plugin); }
    }
}
