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
 * Chain Mode Block Display -- GROUP: GROUND ERUPTIONS
 * 15 chain-themed structures that erupt from the ground.
 *
 * Rules:
 * - Min 10 BlockDisplays per attack, min 40 HP (20 hearts) damage
 * - NO status effects -- damage only
 * - Iron/chain color palette (dark iron grays, rust orange, steel blue)
 * - Phase = 1, full animations, metallic sounds
 * - Materials: CHAIN, IRON_BLOCK, NETHERITE_BLOCK, DEEPSLATE, HEAVY_CORE, ANVIL, IRON_BARS
 */
public final class GroundEruptions {

    private GroundEruptions() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ChainGeyser(plugin));
        registry.register(new RupturedChainVein(plugin));
        registry.register(new ChainSpikeField(plugin));
        registry.register(new IronRootSystem(plugin));
        registry.register(new ChainVolcano(plugin));
        registry.register(new ShackleEruption(plugin));
        registry.register(new ChainPillarBurst(plugin));
        registry.register(new MoltenChainPool(plugin));
        registry.register(new ChainMineExplosion(plugin));
        registry.register(new EruptingChainRing(plugin));
        registry.register(new ChainQuake(plugin));
        registry.register(new AnchorEruption(plugin));
        registry.register(new ChainGeyserField(plugin));
        registry.register(new RisingChainWall(plugin));
        registry.register(new ChainFountain(plugin));
    }

    // ================================================================
    // 1. CHAIN GEYSER -- 12 chains erupt upward in a column like a
    //    water geyser. Shoots 15 blocks high, hangs, crashes back down.
    // ================================================================
    public static class ChainGeyser extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> chainColumn = new ArrayList<>();
        private static final int RISE_TICKS = 20;
        private static final int HANG_TICKS = 40;
        private static final int FALL_TICKS = 15;
        private static final float MAX_HEIGHT = 15.0f;

        public ChainGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_geyser", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 chain blocks stacked in a tight column, starting underground
            for (int i = 0; i < 12; i++) {
                Location loc = center.clone().add(0, -12 + i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.8f, 1.2f, 0.8f).glow(120, 120, 130).interpolation(3, 0);
                chainColumn.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.5f);
            DisplayBuilder.dustParticles(center, 40, 1.5, 160, 160, 170, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float yOffset;
            if (ticksAlive <= RISE_TICKS) {
                // Phase 1: Erupt upward
                float progress = ticksAlive / (float) RISE_TICKS;
                float eased = progress * progress; // accelerate
                yOffset = eased * MAX_HEIGHT;

                // Spray iron dust outward during eruption
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, yOffset * 0.5, 0), 15, 2.0, 180, 140, 100, 1.2f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.6f, 0.4f + progress * 0.6f);
                }
            } else if (ticksAlive <= RISE_TICKS + HANG_TICKS) {
                // Phase 2: Hang at apex with trembling
                float tremble = (float) Math.sin(ticksAlive * 2.5) * 0.15f;
                yOffset = MAX_HEIGHT + tremble;

                // Subtle particle mist at apex
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, MAX_HEIGHT + 6, 0), 8, 1.5, 140, 140, 150, 0.8f);
                }
            } else if (ticksAlive <= RISE_TICKS + HANG_TICKS + FALL_TICKS) {
                // Phase 3: Crash back down
                float fallProgress = (ticksAlive - RISE_TICKS - HANG_TICKS) / (float) FALL_TICKS;
                float easedFall = fallProgress * fallProgress * fallProgress; // accelerating fall
                yOffset = MAX_HEIGHT * (1.0f - easedFall);

                if (fallProgress > 0.9f) {
                    // Impact
                    triggerImpactDamage(c);
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DEATH, 1.2f, 0.4f);
                    c.getWorld().spawnParticle(Particle.BLOCK, c, 80, 2, 0.5, 2, 0,
                            Material.IRON_BLOCK.createBlockData());
                }
            } else {
                yOffset = 0;
                // Idle tremor post-impact
                float shake = (float) Math.sin(ticksAlive * 1.8) * 0.08f;
                yOffset = shake;
            }

            // Teleport all column blocks
            for (int i = 0; i < chainColumn.size(); i++) {
                BlockDisplay bd = chainColumn.get(i).entity();
                Location target = c.clone().add(0, i + yOffset, 0);
                bd.teleport(target);
            }

            // Ground-shake particles at base
            if (ticksAlive % 6 == 0 && ticksAlive <= RISE_TICKS) {
                c.getWorld().spawnParticle(Particle.BLOCK, c, 20, 1.5, 0.2, 1.5, 0,
                        Material.DEEPSLATE.createBlockData());
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainGeyser(plugin); }
    }

    // ================================================================
    // 2. RUPTURED CHAIN VEIN -- A jagged crack reveals 14 chains
    //    emerging in a line 8 blocks long, staggered timing.
    // ================================================================
    public static class RupturedChainVein extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> veinChains = new ArrayList<>();
        private final double[] spawnDelays = new double[14];

        public RupturedChainVein(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ruptured_chain_vein", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(25);

            // Staggered emergence: each chain has a random delay 0-15 ticks
            for (int i = 0; i < 14; i++) {
                spawnDelays[i] = i * 2 + Math.random() * 4;
            }
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14 chains along an 8-block jagged line (Z-axis)
            for (int i = 0; i < 14; i++) {
                double zOff = (i / 13.0) * 8.0 - 4.0; // -4 to +4
                double xJag = Math.sin(i * 1.7) * 0.6; // jagged offset
                Location loc = center.clone().add(xJag, -3, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.6f, 0.3f, 0.6f).glow(100, 90, 80).interpolation(3, 0);
                veinChains.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 0.8f, 0.6f);
            DisplayBuilder.dustParticles(center, 30, 3.0, 140, 120, 90, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < veinChains.size(); i++) {
                BlockDisplayHandle h = veinChains.get(i);
                double delay = spawnDelays[i];

                double zOff = (i / 13.0) * 8.0 - 4.0;
                double xJag = Math.sin(i * 1.7) * 0.6;

                if (ticksAlive < delay) {
                    // Not yet emerged
                    continue;
                }

                float emergeTime = 20.0f;
                float localProgress = Math.min(1.0f, (float) (ticksAlive - delay) / emergeTime);

                // Rise from underground
                float yOffset = localProgress * 3.5f;
                float scaleY = 0.3f + localProgress * 2.7f; // grow tall

                Location target = c.clone().add(xJag, -3 + yOffset, zOff);
                h.entity().teleport(target);
                h.scale(0.6f, scaleY, 0.6f);
                h.interpolation(3, 0);

                // Debris particles at fissure point
                if (localProgress > 0 && localProgress < 0.8f && ticksAlive % 3 == 0) {
                    Location fissure = c.clone().add(xJag, 0, zOff);
                    c.getWorld().spawnParticle(Particle.BLOCK, fissure, 5, 0.3, 0.1, 0.3, 0,
                            Material.DEEPSLATE.createBlockData());
                }
            }

            // Tectonic rumble sound during emergence
            if (ticksAlive % 10 == 0 && ticksAlive < 40) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.4f, 0.3f);
            }

            // Ongoing rust dust along the fissure
            if (ticksAlive % 8 == 0 && ticksAlive > 30) {
                for (int i = 0; i < 3; i++) {
                    double zOff = Math.random() * 8.0 - 4.0;
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.5, zOff), 4, 0.4, 170, 100, 50, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RupturedChainVein(plugin); }
    }

    // ================================================================
    // 3. CHAIN SPIKE FIELD -- 15 chain spikes in a 5x3 grid, each a
    //    1x3 pillar, popping up in a front-to-back wave.
    // ================================================================
    public static class ChainSpikeField extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> spikePillars = new ArrayList<>();
        private static final int COLS = 5;
        private static final int ROWS = 3;

        public ChainSpikeField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_spike_field", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5x3 grid of spikes (each spike = 3 chain blocks stacked)
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    List<BlockDisplayHandle> pillar = new ArrayList<>();
                    double xOff = (col - 2) * 1.8;
                    double zOff = (row - 1) * 1.8;

                    for (int h = 0; h < 3; h++) {
                        Location loc = center.clone().add(xOff, -4 + h, zOff);
                        BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, Material.CHAIN);
                        float taper = 1.0f - (h * 0.2f);
                        handle.scale(taper, 1.0f, taper).glow(110, 110, 120).interpolation(2, 0);
                        pillar.add(handle);
                        spawnedEntities.add(handle.entity());
                    }
                    spikePillars.add(pillar);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int row = 0; row < ROWS; row++) {
                // Wave pattern: front row first, then middle, then back
                int rowDelay = row * 8; // 8 ticks between rows
                if (ticksAlive < rowDelay) continue;

                int localTick = ticksAlive - rowDelay;
                float riseTime = 10.0f;
                float riseProgress = Math.min(1.0f, localTick / riseTime);

                // Quick stab-upward easing
                float eased = (float) Math.sin(riseProgress * Math.PI * 0.5);
                float yOffset = eased * 4.5f;

                for (int col = 0; col < COLS; col++) {
                    int idx = row * COLS + col;
                    if (idx >= spikePillars.size()) continue;

                    List<BlockDisplayHandle> pillar = spikePillars.get(idx);
                    double xOff = (col - 2) * 1.8;
                    double zOff = (row - 1) * 1.8;

                    for (int h = 0; h < pillar.size(); h++) {
                        Location target = c.clone().add(xOff, -4 + h + yOffset, zOff);
                        pillar.get(h).entity().teleport(target);
                    }

                    // Stab sound on full emergence
                    if (localTick == (int) riseTime) {
                        Location spikeLoc = c.clone().add(xOff, 0, zOff);
                        DisplayBuilder.playSound(spikeLoc, Sound.BLOCK_CHAIN_BREAK, 0.5f, 1.2f);
                    }
                }
            }

            // Continuous iron particles in the field
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < 4; i++) {
                    double rx = (Math.random() - 0.5) * 8;
                    double rz = (Math.random() - 0.5) * 4;
                    DisplayBuilder.dustParticles(c.clone().add(rx, 0.5, rz), 3, 0.3, 150, 150, 160, 0.6f);
                }
            }

            // Post-rise tremble
            if (ticksAlive > 30) {
                float tremble = (float) Math.sin(ticksAlive * 3.0) * 0.05f;
                for (List<BlockDisplayHandle> pillar : spikePillars) {
                    for (BlockDisplayHandle h : pillar) {
                        BlockDisplay bd = h.entity();
                        Location loc = bd.getLocation();
                        bd.teleport(loc.clone().add(0, tremble, 0));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainSpikeField(plugin); }
    }

    // ================================================================
    // 4. IRON ROOT SYSTEM -- 12 chains spread outward from center like
    //    tree roots, curving along the ground surface.
    // ================================================================
    public static class IronRootSystem extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> rootBlocks = new ArrayList<>();
        private final double[] rootAngles = new double[12];

        public IronRootSystem(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_root_system", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);

            for (int i = 0; i < 12; i++) {
                rootAngles[i] = (Math.PI * 2 * i) / 12.0 + (Math.random() - 0.5) * 0.3;
            }
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 12; i++) {
                Location loc = center.clone().add(0, -1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.5f, 0.4f, 0.5f).glow(160, 90, 40).interpolation(3, 0); // rust orange glow
                rootBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ROOTED_DIRT_BREAK, 1.0f, 0.5f);
            // Ground crack particles at center
            w.spawnParticle(Particle.BLOCK, center, 40, 0.5, 0.1, 0.5, 0,
                    Material.DEEPSLATE.createBlockData());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slowly expand radius over 60 ticks
            float expandProgress = Math.min(1.0f, ticksAlive / 60.0f);
            float maxRadius = 6.0f;
            float currentRadius = expandProgress * maxRadius;

            for (int i = 0; i < rootBlocks.size(); i++) {
                double angle = rootAngles[i];
                // Curve along ground: slight sine wave in Y
                double dist = currentRadius * ((i % 3 + 1) / 3.0);
                double x = Math.cos(angle) * dist;
                double z = Math.sin(angle) * dist;
                double yCurve = Math.sin(dist * 0.5) * 0.3 - 0.3; // stays at/below ground

                Location target = c.clone().add(x, yCurve, z);
                rootBlocks.get(i).entity().teleport(target);

                // Scale grows as root extends
                float scaleX = 0.5f + expandProgress * 0.4f;
                rootBlocks.get(i).scale(scaleX, 0.4f, scaleX);
                rootBlocks.get(i).interpolation(3, 0);

                // Crack particles at root tips during expansion
                if (expandProgress < 1.0f && ticksAlive % 4 == 0 && i % 3 == 0) {
                    c.getWorld().spawnParticle(Particle.BLOCK, target.clone().add(0, 0.5, 0),
                            3, 0.2, 0.1, 0.2, 0, Material.DEEPSLATE.createBlockData());
                }
            }

            // Rust glow pulse
            if (ticksAlive % 10 == 0) {
                int pulse = (int) (140 + Math.sin(ticksAlive * 0.3) * 40);
                for (BlockDisplayHandle h : rootBlocks) {
                    h.glow(pulse, 70, 30);
                }
            }

            // Ambient metallic groan
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IronRootSystem(plugin); }
    }

    // ================================================================
    // 5. CHAIN VOLCANO -- Conical mound builds up, then erupts
    //    launching 6 chain blocks upward. Impact damage from falling.
    // ================================================================
    public static class ChainVolcano extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> coneBase = new ArrayList<>();
        private final List<BlockDisplayHandle> lavaStreams = new ArrayList<>();
        private final List<BlockDisplayHandle> eruptedChains = new ArrayList<>();
        private boolean erupted = false;

        public ChainVolcano(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_volcano", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(46.0);
            config.setImpactRadius(3.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 iron blocks forming cone base (ring at ground, smaller ring above)
            double[][] basePositions = {
                {2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2},
                {1.4, 0, 1.4}, {-1.4, 0, 1.4}, {1.4, 0, -1.4}, {-1.4, 0, -1.4}
            };
            for (double[] pos : basePositions) {
                Location loc = center.clone().add(pos[0], -4 + pos[1], pos[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(1.2f, 0.8f, 1.2f).glow(100, 100, 110).interpolation(4, 0);
                coneBase.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 chain "lava streams" running down sides
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, -3, Math.sin(angle) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.4f, 2.0f, 0.4f).glow(200, 100, 30).interpolation(3, 0); // molten glow
                lavaStreams.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 chain blocks that will be erupted (start hidden inside cone)
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, -2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.6f, 0.6f, 0.6f).glow(220, 120, 40).interpolation(2, 0);
                eruptedChains.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Cone builds up over 40 ticks
            if (ticksAlive <= 40) {
                float buildProgress = ticksAlive / 40.0f;
                float yOffset = buildProgress * 4.0f;

                double[][] basePositions = {
                    {2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2},
                    {1.4, 0, 1.4}, {-1.4, 0, 1.4}, {1.4, 0, -1.4}, {-1.4, 0, -1.4}
                };
                for (int i = 0; i < coneBase.size(); i++) {
                    // Contract toward center as it rises (cone shape)
                    float shrink = 1.0f - buildProgress * 0.3f;
                    Location target = c.clone().add(
                            basePositions[i][0] * shrink,
                            -4 + yOffset + (i < 4 ? 0 : 1),
                            basePositions[i][2] * shrink
                    );
                    coneBase.get(i).entity().teleport(target);
                }

                for (int i = 0; i < lavaStreams.size(); i++) {
                    double angle = (Math.PI * 2 * i) / 4;
                    Location target = c.clone().add(Math.cos(angle) * (1.0 - buildProgress * 0.3), -3 + yOffset + 1, Math.sin(angle) * (1.0 - buildProgress * 0.3));
                    lavaStreams.get(i).entity().teleport(target);
                }

                // Building rumble
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.4f);
                }
            }
            // Phase 2: Eruption at tick 40
            else if (!erupted) {
                erupted = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
                c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, 3, 0), 60, 1.5, 2, 1.5, 0);
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 30, 2.0, 220, 120, 40, 2.0f);
            }

            // Phase 3: Erupted chains fly upward then fall
            if (ticksAlive > 40) {
                int eruptTick = ticksAlive - 40;

                for (int i = 0; i < eruptedChains.size(); i++) {
                    double angle = (Math.PI * 2 * i) / 6;
                    double horizSpeed = 0.2 + (i * 0.05);
                    double x = Math.cos(angle) * horizSpeed * eruptTick;
                    double z = Math.sin(angle) * horizSpeed * eruptTick;
                    // Parabolic arc: up then down
                    double y = 3.0 + eruptTick * 0.6 - (eruptTick * eruptTick * 0.02);
                    if (y < 0) y = 0;

                    Location target = c.clone().add(x, y, z);
                    eruptedChains.get(i).entity().teleport(target);

                    // Spin while airborne
                    float spin = eruptTick * 0.15f;
                    eruptedChains.get(i).rotate(spin, 0.5f, 1, 0.3f);
                    eruptedChains.get(i).interpolation(2, 0);

                    // Impact when landing
                    if (y <= 0.1 && eruptTick > 10) {
                        triggerImpactDamage(target);
                        c.getWorld().spawnParticle(Particle.BLOCK, target, 10, 0.3, 0.1, 0.3, 0,
                                Material.IRON_BLOCK.createBlockData());
                    }
                }

                // Lava particle aftermath
                if (eruptTick % 5 == 0 && eruptTick < 30) {
                    c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, 2, 0), 10, 1, 1, 1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainVolcano(plugin); }
    }

    // ================================================================
    // 6. SHACKLE ERUPTION -- 10 pairs of shackle shapes burst from
    //    ground in a circle, spring up and snap shut like bear traps.
    // ================================================================
    public static class ShackleEruption extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shackleIrons = new ArrayList<>();
        private final List<BlockDisplayHandle> shackleChainL = new ArrayList<>();
        private final List<BlockDisplayHandle> shackleChainR = new ArrayList<>();

        public ShackleEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shackle_eruption", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double radius = 3.5;
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                // Iron block base
                Location baseLoc = center.clone().add(x, -3, z);
                BlockDisplayHandle iron = displayBuilder.spawnBlock(baseLoc, Material.IRON_BLOCK);
                iron.scale(0.6f, 0.4f, 0.6f).glow(130, 130, 140).interpolation(2, 0);
                shackleIrons.add(iron);
                spawnedEntities.add(iron.entity());

                // Left jaw chain
                Location leftLoc = center.clone().add(x - 0.3, -3, z);
                BlockDisplayHandle left = displayBuilder.spawnBlock(leftLoc, Material.CHAIN);
                left.scale(0.3f, 0.8f, 0.3f).glow(120, 120, 130).interpolation(2, 0);
                shackleChainL.add(left);
                spawnedEntities.add(left.entity());

                // Right jaw chain
                Location rightLoc = center.clone().add(x + 0.3, -3, z);
                BlockDisplayHandle right = displayBuilder.spawnBlock(rightLoc, Material.CHAIN);
                right.scale(0.3f, 0.8f, 0.3f).glow(120, 120, 130).interpolation(2, 0);
                shackleChainR.add(right);
                spawnedEntities.add(right.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double radius = 3.5;

            // Phase 1: Spring up (0-15 ticks)
            // Phase 2: Jaws open (15-25 ticks)
            // Phase 3: Snap shut (25-28 ticks)
            // Phase 4: Idle clamp

            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                float yOffset;
                float jawAngle;

                if (ticksAlive <= 15) {
                    // Spring up
                    float p = ticksAlive / 15.0f;
                    yOffset = p * 3.5f;
                    jawAngle = 0;
                } else if (ticksAlive <= 25) {
                    // Jaws open wide
                    yOffset = 3.5f;
                    float openP = (ticksAlive - 15) / 10.0f;
                    jawAngle = openP * 0.8f; // open ~45 degrees
                } else if (ticksAlive <= 28) {
                    // Snap shut
                    yOffset = 3.5f;
                    float snapP = (ticksAlive - 25) / 3.0f;
                    jawAngle = 0.8f * (1.0f - snapP);
                    if (ticksAlive == 28) {
                        DisplayBuilder.playSound(c.clone().add(x, 1, z), Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.5f);
                    }
                } else {
                    yOffset = 3.5f;
                    jawAngle = 0;
                    // Idle tremble
                    yOffset += (float) Math.sin(ticksAlive * 2.0 + i) * 0.04f;
                }

                // Teleport base
                Location baseTgt = c.clone().add(x, -3 + yOffset, z);
                shackleIrons.get(i).entity().teleport(baseTgt);

                // Teleport jaw chains with rotation for open/close
                Location leftTgt = c.clone().add(x - 0.3, -3 + yOffset + 0.4, z);
                shackleChainL.get(i).entity().teleport(leftTgt);
                shackleChainL.get(i).rotate(-jawAngle, 0, 0, 1);
                shackleChainL.get(i).interpolation(2, 0);

                Location rightTgt = c.clone().add(x + 0.3, -3 + yOffset + 0.4, z);
                shackleChainR.get(i).entity().teleport(rightTgt);
                shackleChainR.get(i).rotate(jawAngle, 0, 0, 1);
                shackleChainR.get(i).interpolation(2, 0);
            }

            // Crunching metal sound on snap
            if (ticksAlive == 27) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.6f);
            }

            // Sparks from clamped shackles
            if (ticksAlive > 28 && ticksAlive % 8 == 0) {
                for (int i = 0; i < 10; i += 3) {
                    double angle = (Math.PI * 2 * i) / 10;
                    Location sparkLoc = c.clone().add(Math.cos(angle) * radius, 1, Math.sin(angle) * radius);
                    DisplayBuilder.dustParticles(sparkLoc, 5, 0.2, 200, 180, 100, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShackleEruption(plugin); }
    }

    // ================================================================
    // 7. CHAIN PILLAR BURST -- 5 thick pillars rise in a pentagon,
    //    then tilt outward and fall like dominos.
    // ================================================================
    public static class ChainPillarBurst extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> pillars = new ArrayList<>();
        private final boolean[] fallen = new boolean[5];
        private static final int RISE_DURATION = 30;
        private static final int FALL_START = 50;

        public ChainPillarBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_pillar_burst", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(48.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(340);
            config.setTicksBetweenDamage(25);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(48.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double radius = 4.0;
            for (int p = 0; p < 5; p++) {
                List<BlockDisplayHandle> pillar = new ArrayList<>();
                double angle = (Math.PI * 2 * p) / 5;
                double bx = Math.cos(angle) * radius;
                double bz = Math.sin(angle) * radius;

                // Each pillar: 3 chain blocks stacked, 2x2 scale
                for (int h = 0; h < 3; h++) {
                    Location loc = center.clone().add(bx, -6 + h * 2, bz);
                    BlockDisplayHandle handle = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    handle.scale(2.0f, 2.0f, 2.0f).glow(100, 100, 110).interpolation(3, 0);
                    pillar.add(handle);
                    spawnedEntities.add(handle.entity());
                }
                pillars.add(pillar);
                fallen[p] = false;
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.3f);
            w.spawnParticle(Particle.BLOCK, center, 50, 3, 0.5, 3, 0,
                    Material.DEEPSLATE.createBlockData());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double pentRadius = 4.0;

            // Phase 1: Rise from ground
            if (ticksAlive <= RISE_DURATION) {
                float riseP = ticksAlive / (float) RISE_DURATION;
                float yOff = riseP * 6.0f;

                for (int p = 0; p < 5; p++) {
                    double angle = (Math.PI * 2 * p) / 5;
                    double bx = Math.cos(angle) * pentRadius;
                    double bz = Math.sin(angle) * pentRadius;

                    List<BlockDisplayHandle> pillar = pillars.get(p);
                    for (int h = 0; h < pillar.size(); h++) {
                        Location target = c.clone().add(bx, -6 + h * 2 + yOff, bz);
                        pillar.get(h).entity().teleport(target);
                    }
                }

                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.6f, 0.5f);
                }
            }
            // Phase 2: Pillars tilt and fall outward sequentially
            else if (ticksAlive >= FALL_START) {
                for (int p = 0; p < 5; p++) {
                    int pillarFallStart = FALL_START + p * 8; // 8 ticks between each domino

                    if (ticksAlive < pillarFallStart) continue;

                    double angle = (Math.PI * 2 * p) / 5;
                    double bx = Math.cos(angle) * pentRadius;
                    double bz = Math.sin(angle) * pentRadius;

                    int fallTick = ticksAlive - pillarFallStart;
                    float fallProgress = Math.min(1.0f, fallTick / 12.0f);
                    float tiltAngle = fallProgress * 1.57f; // tilt to 90 degrees (PI/2)

                    // Outward direction for tilt
                    float tiltAxisX = (float) -Math.sin(angle);
                    float tiltAxisZ = (float) Math.cos(angle);

                    List<BlockDisplayHandle> pillar = pillars.get(p);
                    for (int h = 0; h < pillar.size(); h++) {
                        // Pivot from base: blocks translate outward as they tilt
                        float pivotHeight = h * 2.0f;
                        float fallX = (float) (Math.cos(angle) * Math.sin(tiltAngle) * pivotHeight);
                        float fallZ = (float) (Math.sin(angle) * Math.sin(tiltAngle) * pivotHeight);
                        float fallY = (float) (Math.cos(tiltAngle) * pivotHeight);

                        Location target = c.clone().add(bx + fallX, fallY, bz + fallZ);
                        pillar.get(h).entity().teleport(target);
                        pillar.get(h).rotate(tiltAngle, tiltAxisX, 0, tiltAxisZ);
                        pillar.get(h).interpolation(2, 0);
                    }

                    // Impact on landing
                    if (fallProgress >= 1.0f && !fallen[p]) {
                        fallen[p] = true;
                        double impactX = bx + Math.cos(angle) * 6;
                        double impactZ = bz + Math.sin(angle) * 6;
                        Location impactLoc = c.clone().add(impactX, 0, impactZ);
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.5f);
                        c.getWorld().spawnParticle(Particle.BLOCK, impactLoc, 30, 1, 0.3, 1, 0,
                                Material.IRON_BLOCK.createBlockData());
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainPillarBurst(plugin); }
    }

    // ================================================================
    // 8. MOLTEN CHAIN POOL -- 12 chains spread flat in a circle at
    //    ground level with rust-orange glow, bubbling and undulating.
    // ================================================================
    public static class MoltenChainPool extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> poolChains = new ArrayList<>();
        private final double[] chainAngles = new double[12];
        private final double[] chainDist = new double[12];

        public MoltenChainPool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("molten_chain_pool", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);

            for (int i = 0; i < 12; i++) {
                chainAngles[i] = (Math.PI * 2 * i) / 12.0 + (Math.random() - 0.5) * 0.4;
                chainDist[i] = 1.0 + Math.random() * 2.5;
            }
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 12; i++) {
                double x = Math.cos(chainAngles[i]) * chainDist[i];
                double z = Math.sin(chainAngles[i]) * chainDist[i];
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                // Flat orientation (wide and thin)
                h.scale(1.2f, 0.15f, 1.2f).glow(200, 100, 30).interpolation(3, 0);
                poolChains.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.5f);
            DisplayBuilder.dustParticles(center, 40, 3.0, 220, 120, 30, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Bubble/undulate: each chain oscillates in Y at different phases
            for (int i = 0; i < poolChains.size(); i++) {
                double x = Math.cos(chainAngles[i]) * chainDist[i];
                double z = Math.sin(chainAngles[i]) * chainDist[i];
                // Sine wave with offset per chain
                float yBubble = (float) Math.sin(ticksAlive * 0.15 + i * 0.8) * 0.35f;

                Location target = c.clone().add(x, yBubble, z);
                poolChains.get(i).entity().teleport(target);

                // Pulsing glow intensity
                int glowR = (int) (180 + Math.sin(ticksAlive * 0.1 + i) * 40);
                int glowG = (int) (80 + Math.sin(ticksAlive * 0.1 + i) * 30);
                poolChains.get(i).glow(glowR, glowG, 20);
            }

            // Lava particles rising from pool
            if (ticksAlive % 4 == 0) {
                double rx = (Math.random() - 0.5) * 6;
                double rz = (Math.random() - 0.5) * 6;
                c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(rx, 0.3, rz), 3, 0.3, 0.2, 0.3, 0);
            }

            // Ambient lava bubble sound
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.5f, 0.8f + (float) Math.random() * 0.4f);
            }

            // Smoke rising
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 0.5, 0), 5, 2, 0.5, 2, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoltenChainPool(plugin); }
    }

    // ================================================================
    // 9. CHAIN MINE EXPLOSION -- Single iron block "mine" with 2s
    //    warning timer, then 12 chain shrapnel fly outward.
    // ================================================================
    public static class ChainMineExplosion extends BlockDisplayAttack {

        private BlockDisplayHandle mine;
        private final List<BlockDisplayHandle> shrapnel = new ArrayList<>();
        private boolean exploded = false;
        private static final int FUSE_TICKS = 40; // 2 seconds
        private final double[] shrapnelAngles = new double[12];
        private final double[] shrapnelPitch = new double[12];

        public ChainMineExplosion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_mine_explosion", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(40);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(5.0);

            for (int i = 0; i < 12; i++) {
                shrapnelAngles[i] = (Math.PI * 2 * i) / 12.0 + (Math.random() - 0.5) * 0.3;
                shrapnelPitch[i] = 0.3 + Math.random() * 0.5; // upward arc angle
            }
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Single iron block mine at ground level
            mine = displayBuilder.spawnBlock(center, Material.IRON_BLOCK);
            mine.scale(0.8f, 0.8f, 0.8f).glow(200, 50, 50).interpolation(2, 0);
            spawnedEntities.add(mine.entity());

            // Pre-spawn shrapnel blocks hidden inside the mine
            for (int i = 0; i < 12; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center, Material.CHAIN);
                h.scale(0.01f, 0.01f, 0.01f).glow(150, 150, 160).interpolation(1, 0);
                shrapnel.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Fuse countdown
            if (ticksAlive < FUSE_TICKS) {
                // Ticking sound accelerating
                int tickInterval = Math.max(2, 10 - ticksAlive / 5);
                if (ticksAlive % tickInterval == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.5f);
                }

                // Mine pulsing glow (faster as fuse burns)
                float pulse = (float) Math.sin(ticksAlive * (0.3 + ticksAlive * 0.01)) * 0.5f + 0.5f;
                int r = (int) (150 + pulse * 105);
                mine.glow(r, 30, 30);

                // Mine scale pulse
                float scalePulse = 0.8f + pulse * 0.15f;
                mine.scale(scalePulse, scalePulse, scalePulse);
                mine.interpolation(2, 0);

                // Warning particles
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.8, 0), 6, 0.3, 255, 80, 30, 0.8f);
                }
            }
            // Phase 2: Explosion
            else if (!exploded) {
                exploded = true;

                // Make mine invisible
                mine.scale(0.01f, 0.01f, 0.01f);

                // Explosion effects
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c, 5, 1, 1, 1, 0);
                c.getWorld().spawnParticle(Particle.SMOKE, c, 40, 2, 1, 2, 0.1);
                DisplayBuilder.dustParticles(c, 50, 3.0, 255, 150, 50, 2.0f);

                triggerImpactDamage(c);

                // Reveal shrapnel at full size
                for (BlockDisplayHandle h : shrapnel) {
                    h.scale(0.5f, 0.5f, 0.5f);
                    h.interpolation(1, 0);
                }
            }

            // Phase 3: Shrapnel flying outward
            if (exploded) {
                int flyTick = ticksAlive - FUSE_TICKS;

                for (int i = 0; i < shrapnel.size(); i++) {
                    double angle = shrapnelAngles[i];
                    double speed = 0.5 + (i * 0.05);
                    double x = Math.cos(angle) * speed * flyTick;
                    double z = Math.sin(angle) * speed * flyTick;
                    // Parabolic arc
                    double y = shrapnelPitch[i] * flyTick - 0.02 * flyTick * flyTick;
                    if (y < 0) y = 0;

                    Location target = c.clone().add(x, y + 0.5, z);
                    shrapnel.get(i).entity().teleport(target);

                    // Spin
                    float spin = flyTick * 0.2f;
                    shrapnel.get(i).rotate(spin, (float) Math.cos(angle), 0.5f, (float) Math.sin(angle));
                    shrapnel.get(i).interpolation(1, 0);
                }

                // Trailing smoke
                if (flyTick % 3 == 0 && flyTick < 20) {
                    for (int i = 0; i < shrapnel.size(); i += 3) {
                        Location trailLoc = shrapnel.get(i).entity().getLocation();
                        c.getWorld().spawnParticle(Particle.SMOKE, trailLoc, 2, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainMineExplosion(plugin); }
    }

    // ================================================================
    // 10. ERUPTING CHAIN RING -- Ring of 16 chains erupts up from
    //     underground, tilts outward 45 degrees forming a crown.
    // ================================================================
    public static class EruptingChainRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringChains = new ArrayList<>();
        private static final int RING_COUNT = 16;
        private static final double RING_RADIUS = 4.0;

        public EruptingChainRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("erupting_chain_ring", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < RING_COUNT; i++) {
                double angle = (Math.PI * 2 * i) / RING_COUNT;
                double x = Math.cos(angle) * RING_RADIUS;
                double z = Math.sin(angle) * RING_RADIUS;

                Location loc = center.clone().add(x, -4, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.7f, 1.5f, 0.7f).glow(130, 130, 145).interpolation(3, 0);
                ringChains.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 0.9f, 0.5f);
            DisplayBuilder.particleRing(center, RING_RADIUS, Particle.SMOKE, 24, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float riseProgress = Math.min(1.0f, ticksAlive / 25.0f);
            float tiltProgress = ticksAlive > 25 ? Math.min(1.0f, (ticksAlive - 25) / 20.0f) : 0;
            float tiltAngle = tiltProgress * 0.785f; // 45 degrees = PI/4

            for (int i = 0; i < RING_COUNT; i++) {
                double angle = (Math.PI * 2 * i) / RING_COUNT;

                // Base position on ring
                double baseX = Math.cos(angle) * RING_RADIUS;
                double baseZ = Math.sin(angle) * RING_RADIUS;

                // Rise offset
                float yOff = riseProgress * 4.0f;

                // Tilt outward: translate outward and adjust rotation
                double tiltOutward = Math.sin(tiltAngle) * 1.5;
                double tiltX = baseX + Math.cos(angle) * tiltOutward;
                double tiltZ = baseZ + Math.sin(angle) * tiltOutward;
                double tiltY = yOff - (1.0 - Math.cos(tiltAngle)) * 0.5;

                Location target = c.clone().add(tiltX, -4 + tiltY, tiltZ);
                ringChains.get(i).entity().teleport(target);

                // Rotate chain to face outward
                float rotAngle = tiltAngle;
                float rotAxisX = (float) -Math.sin(angle);
                float rotAxisZ = (float) Math.cos(angle);
                ringChains.get(i).rotate(rotAngle, rotAxisX, 0, rotAxisZ);
                ringChains.get(i).interpolation(3, 0);
            }

            // Expanding damage radius effect particles as crown opens
            if (ticksAlive > 25 && ticksAlive % 5 == 0) {
                double expandedRadius = RING_RADIUS + tiltProgress * 1.5;
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), expandedRadius, Particle.CRIT, 16, null);
            }

            // Rising particles during eruption
            if (ticksAlive <= 25 && ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, riseProgress * 2, 0), RING_RADIUS,
                        Particle.SMOKE, 12, null);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.4f, 0.5f + riseProgress * 0.5f);
            }

            // Crown hum
            if (ticksAlive > 45 && ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EruptingChainRing(plugin); }
    }

    // ================================================================
    // 11. CHAIN QUAKE -- 14 chain blocks pop up sequentially in a
    //     straight line extending 10 blocks, racing fissure effect.
    // ================================================================
    public static class ChainQuake extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> quakeChains = new ArrayList<>();
        private static final int CHAIN_COUNT = 14;

        public ChainQuake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_quake", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14 chains along a 10-block line (X-axis)
            for (int i = 0; i < CHAIN_COUNT; i++) {
                double xOff = (i / (double) (CHAIN_COUNT - 1)) * 10.0 - 5.0; // -5 to +5
                Location loc = center.clone().add(xOff, -2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.7f, 0.1f, 0.7f).glow(110, 110, 120).interpolation(1, 0);
                quakeChains.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < CHAIN_COUNT; i++) {
                // Each chain pops up 1 tick after the previous
                int popTick = i; // 1 tick delay per chain
                double xOff = (i / (double) (CHAIN_COUNT - 1)) * 10.0 - 5.0;

                if (ticksAlive <= popTick) {
                    // Not yet triggered
                    continue;
                }

                int localTick = ticksAlive - popTick;

                // Pop-up: fast rise over 5 ticks, then hold, then sink
                float riseHeight;
                if (localTick <= 5) {
                    float p = localTick / 5.0f;
                    riseHeight = p * 2.5f; // fast stab up
                } else if (localTick <= 30) {
                    riseHeight = 2.5f; // hold at peak
                    // Tremble
                    riseHeight += (float) Math.sin(localTick * 3.0) * 0.1f;
                } else if (localTick <= 50) {
                    float sinkP = (localTick - 30) / 20.0f;
                    riseHeight = 2.5f * (1.0f - sinkP);
                } else {
                    riseHeight = 0;
                }

                Location target = c.clone().add(xOff, -2 + riseHeight, 0);
                quakeChains.get(i).entity().teleport(target);

                // Scale animation: thin → full on pop
                float scaleY = localTick <= 5 ? 0.1f + (localTick / 5.0f) * 1.9f : 2.0f;
                quakeChains.get(i).scale(0.7f, scaleY, 0.7f);
                quakeChains.get(i).interpolation(1, 0);

                // Pop particle and sound
                if (localTick == 3) {
                    Location popLoc = c.clone().add(xOff, 0.5, 0);
                    c.getWorld().spawnParticle(Particle.BLOCK, popLoc, 8, 0.2, 0.3, 0.2, 0,
                            Material.DEEPSLATE.createBlockData());
                    DisplayBuilder.playSound(popLoc, Sound.BLOCK_CHAIN_BREAK, 0.4f, 1.0f + i * 0.05f);
                }
            }

            // Racing fissure ground particles ahead of wave
            if (ticksAlive < CHAIN_COUNT) {
                double aheadX = (ticksAlive / (double) (CHAIN_COUNT - 1)) * 10.0 - 5.0 + 1.0;
                Location aheadLoc = c.clone().add(aheadX, 0, 0);
                c.getWorld().spawnParticle(Particle.BLOCK, aheadLoc, 10, 0.3, 0.1, 0.3, 0,
                        Material.DEEPSLATE.createBlockData());
                DisplayBuilder.dustParticles(aheadLoc, 5, 0.3, 140, 140, 150, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainQuake(plugin); }
    }

    // ================================================================
    // 12. ANCHOR ERUPTION -- 4 massive anchor shapes at compass points
    //     burst up, rotate outward, then slam back down.
    // ================================================================
    public static class AnchorEruption extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> anchorCores = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> anchorFlukes = new ArrayList<>();

        public AnchorEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("anchor_eruption", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(48.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(340);
            config.setTicksBetweenDamage(25);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(48.0);
            config.setImpactRadius(3.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double[][] directions = {{5, 0}, {-5, 0}, {0, 5}, {0, -5}}; // NESW

            for (int a = 0; a < 4; a++) {
                double dx = directions[a][0];
                double dz = directions[a][1];

                // Heavy core (anchor head)
                Location coreLoc = center.clone().add(dx, -5, dz);
                BlockDisplayHandle core = displayBuilder.spawnBlock(coreLoc, Material.HEAVY_CORE);
                core.scale(1.5f, 1.5f, 1.5f).glow(80, 80, 90).interpolation(3, 0);
                anchorCores.add(core);
                spawnedEntities.add(core.entity());

                // 3 chain flukes extending from the core
                List<BlockDisplayHandle> flukes = new ArrayList<>();
                for (int f = 0; f < 3; f++) {
                    double flukeAngle = (f - 1) * 0.5; // -0.5, 0, 0.5 spread
                    Location flukeLoc = center.clone().add(
                            dx + Math.cos(flukeAngle) * 0.8,
                            -5 - 1 - f * 0.8,
                            dz + Math.sin(flukeAngle) * 0.8
                    );
                    BlockDisplayHandle fluke = displayBuilder.spawnBlock(flukeLoc, Material.CHAIN);
                    fluke.scale(0.6f, 1.2f, 0.6f).glow(100, 100, 110).interpolation(3, 0);
                    flukes.add(fluke);
                    spawnedEntities.add(fluke.entity());
                }
                anchorFlukes.add(flukes);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.3f);
            w.spawnParticle(Particle.BLOCK, center, 60, 4, 0.5, 4, 0,
                    Material.DEEPSLATE.createBlockData());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double[][] directions = {{5, 0}, {-5, 0}, {0, 5}, {0, -5}};

            // Phase 1: Rise (0-25 ticks)
            // Phase 2: Rotate outward (25-45 ticks)
            // Phase 3: Slam down (45-55 ticks)

            for (int a = 0; a < 4; a++) {
                double dx = directions[a][0];
                double dz = directions[a][1];

                float yOff;
                float rotAngle;

                if (ticksAlive <= 25) {
                    float riseP = ticksAlive / 25.0f;
                    yOff = riseP * 6.0f;
                    rotAngle = 0;
                } else if (ticksAlive <= 45) {
                    yOff = 6.0f;
                    float rotP = (ticksAlive - 25) / 20.0f;
                    rotAngle = rotP * 1.0f; // rotate ~57 degrees outward
                } else if (ticksAlive <= 55) {
                    float slamP = (ticksAlive - 45) / 10.0f;
                    float easedSlam = slamP * slamP; // accelerating slam
                    yOff = 6.0f * (1.0f - easedSlam);
                    rotAngle = 1.0f * (1.0f - easedSlam);

                    // Impact on landing
                    if (ticksAlive == 55) {
                        Location impactLoc = c.clone().add(dx, 0, dz);
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.playSound(impactLoc, Sound.ENTITY_IRON_GOLEM_DEATH, 1.2f, 0.4f);
                        c.getWorld().spawnParticle(Particle.BLOCK, impactLoc, 30, 1, 0.3, 1, 0,
                                Material.IRON_BLOCK.createBlockData());
                    }
                } else {
                    yOff = 0;
                    rotAngle = 0;
                    // Post-slam tremble
                    yOff = (float) Math.sin(ticksAlive * 1.5) * 0.06f;
                }

                // Core position
                Location coreTarget = c.clone().add(dx, -5 + yOff, dz);
                anchorCores.get(a).entity().teleport(coreTarget);

                // Rotate outward from center
                float outAngle = (float) Math.atan2(dz, dx);
                float rotAxisX = (float) -Math.sin(outAngle);
                float rotAxisZ = (float) Math.cos(outAngle);
                anchorCores.get(a).rotate(rotAngle, rotAxisX, 0, rotAxisZ);
                anchorCores.get(a).interpolation(3, 0);

                // Fluke positions follow core
                List<BlockDisplayHandle> flukes = anchorFlukes.get(a);
                for (int f = 0; f < flukes.size(); f++) {
                    double flukeAngle = (f - 1) * 0.5;
                    Location flukeTarget = coreTarget.clone().add(
                            Math.cos(flukeAngle) * 0.8,
                            -1 - f * 0.8,
                            Math.sin(flukeAngle) * 0.8
                    );
                    flukes.get(f).entity().teleport(flukeTarget);
                    flukes.get(f).rotate(rotAngle, rotAxisX, 0, rotAxisZ);
                    flukes.get(f).interpolation(3, 0);
                }
            }

            // Rising dust during eruption
            if (ticksAlive <= 25 && ticksAlive % 4 == 0) {
                for (double[] dir : directions) {
                    Location dustLoc = c.clone().add(dir[0], 0, dir[1]);
                    c.getWorld().spawnParticle(Particle.BLOCK, dustLoc, 8, 0.5, 0.2, 0.5, 0,
                            Material.DEEPSLATE.createBlockData());
                }
            }

            // Rotation wind sound
            if (ticksAlive > 25 && ticksAlive <= 45 && ticksAlive % 5 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AnchorEruption(plugin); }
    }

    // ================================================================
    // 13. CHAIN GEYSER FIELD -- 6 small geysers (each 3 chains) erupt
    //     in random positions, popcorn-like eruption field.
    // ================================================================
    public static class ChainGeyserField extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> geysers = new ArrayList<>();
        private final double[] geyserX = new double[6];
        private final double[] geyserZ = new double[6];
        private final int[] geyserDelay = new int[6];

        public ChainGeyserField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_geyser_field", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(15);

            for (int i = 0; i < 6; i++) {
                geyserX[i] = (Math.random() - 0.5) * 10.0;
                geyserZ[i] = (Math.random() - 0.5) * 10.0;
                geyserDelay[i] = (int) (Math.random() * 30); // 0-30 tick random delay
            }
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int g = 0; g < 6; g++) {
                List<BlockDisplayHandle> geyser = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    Location loc = center.clone().add(geyserX[g], -3 + i, geyserZ[g]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0.6f, 1.0f, 0.6f).glow(120, 120, 135).interpolation(2, 0);
                    geyser.add(h);
                    spawnedEntities.add(h.entity());
                }
                geysers.add(geyser);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int g = 0; g < 6; g++) {
                if (ticksAlive < geyserDelay[g]) continue;

                int localTick = ticksAlive - geyserDelay[g];
                List<BlockDisplayHandle> geyser = geysers.get(g);

                // Pop-up cycle: each geyser erupts, holds, sinks, re-erupts
                int cycleLength = 40;
                int cycleTick = localTick % cycleLength;

                float height;
                if (cycleTick <= 8) {
                    // Erupt up
                    float p = cycleTick / 8.0f;
                    height = p * p * 5.0f; // quadratic acceleration
                } else if (cycleTick <= 20) {
                    // Hold at top with tremble
                    height = 5.0f + (float) Math.sin(cycleTick * 2.0) * 0.3f;
                } else if (cycleTick <= 30) {
                    // Sink back
                    float sinkP = (cycleTick - 20) / 10.0f;
                    height = 5.0f * (1.0f - sinkP);
                } else {
                    height = 0;
                }

                for (int i = 0; i < geyser.size(); i++) {
                    Location target = c.clone().add(geyserX[g], -3 + i + height, geyserZ[g]);
                    geyser.get(i).entity().teleport(target);
                }

                // Mini particle burst on each eruption
                if (cycleTick == 5) {
                    Location burstLoc = c.clone().add(geyserX[g], height, geyserZ[g]);
                    DisplayBuilder.dustParticles(burstLoc, 12, 0.8, 160, 160, 175, 1.0f);
                    DisplayBuilder.playSound(burstLoc, Sound.BLOCK_CHAIN_BREAK, 0.4f, 0.8f + (float) Math.random() * 0.4f);
                    c.getWorld().spawnParticle(Particle.SMOKE, burstLoc, 6, 0.3, 0.5, 0.3, 0.02);
                }
            }

            // Ambient rumble
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainGeyserField(plugin); }
    }

    // ================================================================
    // 14. RISING CHAIN WALL -- A wall of 15 chains (5 wide, 3 tall)
    //     rises from underground and advances forward 5 blocks.
    // ================================================================
    public static class RisingChainWall extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private static final int WALL_COLS = 5;
        private static final int WALL_ROWS = 3;

        public RisingChainWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rising_chain_wall", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 wide x 3 tall wall, starting underground
            for (int row = 0; row < WALL_ROWS; row++) {
                for (int col = 0; col < WALL_COLS; col++) {
                    double xOff = (col - 2) * 1.2; // spread across X
                    Location loc = center.clone().add(xOff, -4 + row, -3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(1.0f, 1.0f, 0.8f).glow(100, 100, 115).interpolation(3, 0);
                    wallBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rising + advancing simultaneously
            float riseProgress = Math.min(1.0f, ticksAlive / 40.0f);
            float advanceProgress = Math.min(1.0f, ticksAlive / 80.0f);

            float yOff = riseProgress * 4.5f; // rise 4.5 blocks
            float zAdvance = advanceProgress * 5.0f; // advance 5 blocks forward (+Z)

            for (int row = 0; row < WALL_ROWS; row++) {
                for (int col = 0; col < WALL_COLS; col++) {
                    int idx = row * WALL_COLS + col;
                    if (idx >= wallBlocks.size()) continue;

                    double xOff = (col - 2) * 1.2;
                    Location target = c.clone().add(xOff, -4 + row + yOff, -3 + zAdvance);
                    wallBlocks.get(idx).entity().teleport(target);
                }
            }

            // Grinding stone sound during emergence
            if (ticksAlive % 10 == 0 && ticksAlive < 45) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.6f, 0.4f + riseProgress * 0.3f);
            }

            // Ground scraping particles at wall base
            if (ticksAlive % 4 == 0) {
                for (int col = 0; col < WALL_COLS; col++) {
                    double xOff = (col - 2) * 1.2;
                    Location scrapeLoc = c.clone().add(xOff, 0, -3 + zAdvance);
                    c.getWorld().spawnParticle(Particle.BLOCK, scrapeLoc, 3, 0.2, 0.1, 0.2, 0,
                            Material.DEEPSLATE.createBlockData());
                }
            }

            // Dust trail behind the wall
            if (ticksAlive % 6 == 0 && advanceProgress > 0.1f) {
                Location trailLoc = c.clone().add(0, 0.5, -3 + zAdvance - 1.5);
                DisplayBuilder.dustParticles(trailLoc, 8, 2.0, 130, 130, 140, 0.7f);
            }

            // Wall tremble when fully risen
            if (ticksAlive > 40) {
                float tremble = (float) Math.sin(ticksAlive * 1.5) * 0.04f;
                for (BlockDisplayHandle h : wallBlocks) {
                    BlockDisplay bd = h.entity();
                    Location loc = bd.getLocation();
                    bd.teleport(loc.clone().add(tremble, 0, 0));
                }
            }

            // Heavy advancing sound
            if (ticksAlive % 20 == 0 && advanceProgress < 1.0f) {
                DisplayBuilder.playSound(c.clone().add(0, 1, -3 + zAdvance), Sound.BLOCK_CHAIN_STEP, 0.8f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RisingChainWall(plugin); }
    }

    // ================================================================
    // 15. CHAIN FOUNTAIN -- Central iron pedestal with 10 chains arcing
    //     upward and outward like a fountain, continuous cycling.
    // ================================================================
    public static class ChainFountain extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pedestalBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fountainChains = new ArrayList<>();

        public ChainFountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_fountain", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central pedestal: 3 iron blocks stacked
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                float taper = 1.0f - i * 0.15f; // slight taper upward
                h.scale(taper, 1.0f, taper).glow(140, 140, 150).interpolation(3, 0);
                pedestalBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 10 fountain chains, evenly distributed around
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(0, 3, 0); // start at pedestal top
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0.5f, 0.8f, 0.5f).glow(130, 130, 145).interpolation(2, 0);
                fountainChains.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.4f);
            DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 30, 1.0, 160, 160, 175, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pedestal idle glow pulse
            if (ticksAlive % 15 == 0) {
                int pulse = (int) (120 + Math.sin(ticksAlive * 0.15) * 30);
                for (BlockDisplayHandle h : pedestalBlocks) {
                    h.glow(pulse, pulse, pulse + 15);
                }
            }

            // Fountain chains arc in continuous parabolic arcs
            for (int i = 0; i < fountainChains.size(); i++) {
                double angle = (Math.PI * 2 * i) / 10.0;

                // Each chain has a cycling phase offset
                float cycleLength = 50.0f; // ticks per full arc cycle
                float phase = ((ticksAlive + i * (cycleLength / 10.0f)) % cycleLength) / cycleLength;

                // Parabolic fountain path: rise from center, arc outward, fall at edge
                double outDist = phase * 5.0; // extends to radius 5
                double x = Math.cos(angle) * outDist;
                double z = Math.sin(angle) * outDist;
                // Y = parabolic arc: peak at halfway
                double y = 3.0 + 4.0 * phase * (1.0 - phase) * 4.0; // peak at ~7 blocks
                if (phase > 0.8) {
                    // Falling phase: accelerate downward
                    double fallPhase = (phase - 0.8) / 0.2;
                    y = 3.0 + (1.0 - fallPhase * fallPhase) * 2.0;
                }

                Location target = c.clone().add(x, y, z);
                fountainChains.get(i).entity().teleport(target);

                // Rotate as they fly
                float spin = (float) (phase * Math.PI * 4);
                fountainChains.get(i).rotate(spin, (float) Math.cos(angle), 0.3f, (float) Math.sin(angle));
                fountainChains.get(i).interpolation(2, 0);

                // Water-like particle trail following chain paths
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.dustParticles(target, 2, 0.15, 150, 150, 170, 0.5f);
                }
            }

            // Ambient fountain spray particles at top
            if (ticksAlive % 3 == 0) {
                Location sprayLoc = c.clone().add(0, 5, 0);
                c.getWorld().spawnParticle(Particle.DRIPPING_WATER, sprayLoc, 3, 0.5, 0.3, 0.5, 0);
                DisplayBuilder.dustParticles(sprayLoc, 4, 0.8, 160, 160, 180, 0.6f);
            }

            // Metallic water sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 1.2f);
            }

            // Base splash particles at landing radius
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), 4.5, Particle.CRIT, 10, null);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainFountain(plugin); }
    }
}
