package com.blockforge.chaoscraft.modes.corruption.attacks;

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
 * Corruption Mode -- GROUP: ABYSSAL CONSTRUCTS (Ground Corruption & Eruptions)
 * 15 corruption-themed BlockDisplay attacks that corrupt, crack, and erupt from the ground.
 *
 * Rules:
 * - Min 10 BlockDisplays per attack, min 40 HP (20 hearts) damage
 * - NO status effects -- damage only
 * - AxisAngle4f rotations only
 * - Dark corruption color palette:
 *   - Dark purple: RGB(45, 0, 64)
 *   - Crimson: RGB(74, 0, 0)
 *   - Void blue: RGB(10, 0, 48)
 * - Phase = 1, full animations
 * - Materials: SCULK, DEEPSLATE, BLACKSTONE, CRYING_OBSIDIAN, OBSIDIAN, COAL_BLOCK, NETHERRACK, SOUL_SOIL
 * - Sounds: BLOCK_SCULK_SPREAD, BLOCK_SCULK_BREAK, BLOCK_DEEPSLATE_BREAK, ENTITY_WARDEN_HEARTBEAT,
 *           BLOCK_RESPAWN_ANCHOR_DEPLETE
 */
public final class AbyssalConstructs {

    private AbyssalConstructs() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CorruptionFissure(plugin));
        registry.register(new DarkGeyserField(plugin));
        registry.register(new CorruptionMine(plugin));
        registry.register(new VoidSinkhole(plugin));
        registry.register(new ShadowEruption(plugin));
        registry.register(new DarkQuicksand(plugin));
        registry.register(new CorruptionRootNetwork(plugin));
        registry.register(new VoidCaldera(plugin));
        registry.register(new ShadowMinefield(plugin));
        registry.register(new DarkBloom(plugin));
        registry.register(new CorruptionCrater(plugin));
        registry.register(new VoidQuicksandPool(plugin));
        registry.register(new ShadowLandslide(plugin));
        registry.register(new CorruptionGeyserRing(plugin));
        registry.register(new DarkApocalypse(plugin));
    }

    // ================================================================
    // 106. CORRUPTION FISSURE -- 16 blocks crack in the ground in a
    //      jagged line, widen over time, dark particles rise from the gap
    // ================================================================
    public static class CorruptionFissure extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> fissureBlocks = new ArrayList<>();
        private final List<Double> xOffsets = new ArrayList<>();
        private final List<Double> zOffsets = new ArrayList<>();
        private static final int BLOCK_COUNT = 16;
        private static final int WIDEN_TICKS = 40;

        public CorruptionFissure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_fissure", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random fissure direction
            double dirAngle = Math.random() * Math.PI * 2;
            double dirX = Math.cos(dirAngle);
            double dirZ = Math.sin(dirAngle);

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double t = (i - BLOCK_COUNT / 2.0) * 0.7;
                double jag = Math.sin(i * 2.3) * 0.4;
                double ox = dirX * t + (-dirZ) * jag;
                double oz = dirZ * t + dirX * jag;
                xOffsets.add(ox);
                zOffsets.add(oz);

                Location loc = center.clone().add(ox, -0.5, oz);
                Material mat = (i % 3 == 0) ? Material.DEEPSLATE : (i % 3 == 1) ? Material.BLACKSTONE : Material.SCULK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.8f, 0.2f, 0.3f).glow(45, 0, 64).interpolation(3, 0);
                fissureBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float widenProgress = Math.min(1.0f, ticksAlive / (float) WIDEN_TICKS);

            for (int i = 0; i < fissureBlocks.size(); i++) {
                BlockDisplayHandle h = fissureBlocks.get(i);

                // Widen the fissure gap over time
                float scaleX = 0.8f + widenProgress * 0.6f;
                float scaleY = 0.2f + widenProgress * 0.4f;
                h.scale(scaleX, scaleY, 0.3f + widenProgress * 0.3f);
                h.interpolation(3, 0);

                // Slight upward drift as fissure opens
                float yShift = widenProgress * 0.3f + (float) Math.sin(ticksAlive * 0.1 + i * 0.5) * 0.05f;
                Location target = c.clone().add(xOffsets.get(i), -0.5 + yShift, zOffsets.get(i));
                h.entity().teleport(target);

                // Tilt blocks along the crack
                float tiltAngle = (float) Math.sin(i * 1.3) * 0.2f * widenProgress;
                h.rotate(tiltAngle, 0, 0, 1);
                h.interpolation(3, 0);

                // Dark particles rising from fissure
                if (ticksAlive % 4 == 0 && widenProgress > 0.3f) {
                    Location particleLoc = target.clone().add(0, 0.3, 0);
                    DisplayBuilder.dustParticles(particleLoc, 3, 0.3, 45, 0, 64, 1.2f);
                }
            }

            // Ambient corruption spread sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.4f);
            }

            // Warden heartbeat as fissure pulses
            if (ticksAlive % 40 == 0 && ticksAlive > WIDEN_TICKS) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.7f);
                // Pulse glow on all blocks
                for (BlockDisplayHandle h : fissureBlocks) {
                    h.glow(74, 0, 0);
                }
            }
            if (ticksAlive % 40 == 10) {
                for (BlockDisplayHandle h : fissureBlocks) {
                    h.glow(45, 0, 64);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionFissure(plugin); }
    }

    // ================================================================
    // 107. DARK GEYSER FIELD -- 15 blocks (5 geysers x 3 segments),
    //      randomly erupt in staggered sequence, dark particle spray
    // ================================================================
    public static class DarkGeyserField extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> geyserBlocks = new ArrayList<>();
        private final List<Double> geyserX = new ArrayList<>();
        private final List<Double> geyserZ = new ArrayList<>();
        private final List<Integer> eruptDelays = new ArrayList<>();
        private final List<Boolean> erupted = new ArrayList<>();
        private static final int GEYSER_COUNT = 5;
        private static final int SEGMENTS_PER = 3;
        private static final float MAX_HEIGHT = 12.0f;

        public DarkGeyserField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_geyser_field", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(44.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int g = 0; g < GEYSER_COUNT; g++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 2.0 + Math.random() * 5.0;
                double gx = Math.cos(angle) * dist;
                double gz = Math.sin(angle) * dist;
                geyserX.add(gx);
                geyserZ.add(gz);
                eruptDelays.add(g * 15 + (int) (Math.random() * 10));
                erupted.add(false);

                // 3 segments per geyser, start underground
                for (int s = 0; s < SEGMENTS_PER; s++) {
                    Location loc = center.clone().add(gx, -3 + s, gz);
                    Material mat = (s == 0) ? Material.SCULK : (s == 1) ? Material.DEEPSLATE : Material.CRYING_OBSIDIAN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.7f, 1.5f, 0.7f).glow(10, 0, 48).interpolation(3, 0);
                    geyserBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int g = 0; g < GEYSER_COUNT; g++) {
                if (ticksAlive < eruptDelays.get(g)) continue;

                int localTick = ticksAlive - eruptDelays.get(g);
                float eruptProgress;

                if (localTick < 15) {
                    // Rising phase
                    eruptProgress = localTick / 15.0f;
                    float eased = eruptProgress * eruptProgress;
                    float yOffset = eased * MAX_HEIGHT;

                    for (int s = 0; s < SEGMENTS_PER; s++) {
                        int idx = g * SEGMENTS_PER + s;
                        Location target = c.clone().add(geyserX.get(g), yOffset + s * 1.5, geyserZ.get(g));
                        geyserBlocks.get(idx).entity().teleport(target);
                    }

                    // Eruption particles
                    if (localTick % 2 == 0) {
                        Location base = c.clone().add(geyserX.get(g), 0, geyserZ.get(g));
                        DisplayBuilder.dustParticles(base, 15, 1.5, 45, 0, 64, 1.5f);
                        DisplayBuilder.playSound(base, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.5f, 0.3f + eruptProgress * 0.5f);
                    }

                    // Trigger damage at eruption point
                    if (!erupted.get(g) && eruptProgress > 0.5f) {
                        erupted.set(g, true);
                        Location impactLoc = c.clone().add(geyserX.get(g), 1, geyserZ.get(g));
                        triggerImpactDamage(impactLoc);
                    }
                } else if (localTick < 50) {
                    // Hovering phase with tremor
                    float tremble = (float) Math.sin(localTick * 2.0) * 0.2f;
                    for (int s = 0; s < SEGMENTS_PER; s++) {
                        int idx = g * SEGMENTS_PER + s;
                        Location target = c.clone().add(geyserX.get(g), MAX_HEIGHT + s * 1.5 + tremble, geyserZ.get(g));
                        geyserBlocks.get(idx).entity().teleport(target);
                        geyserBlocks.get(idx).rotate(tremble * 0.3f, 0, 1, 0);
                        geyserBlocks.get(idx).interpolation(2, 0);
                    }

                    // Dripping void particles
                    if (localTick % 5 == 0) {
                        Location apex = c.clone().add(geyserX.get(g), MAX_HEIGHT, geyserZ.get(g));
                        DisplayBuilder.dustParticles(apex, 6, 1.0, 10, 0, 48, 1.0f);
                    }
                } else {
                    // Collapse phase
                    float fallProgress = Math.min(1.0f, (localTick - 50) / 12.0f);
                    float fallEased = fallProgress * fallProgress * fallProgress;
                    float yOffset = MAX_HEIGHT * (1.0f - fallEased);

                    for (int s = 0; s < SEGMENTS_PER; s++) {
                        int idx = g * SEGMENTS_PER + s;
                        Location target = c.clone().add(geyserX.get(g), yOffset + s * 1.5, geyserZ.get(g));
                        geyserBlocks.get(idx).entity().teleport(target);
                    }

                    // Ground slam
                    if (fallProgress > 0.95f && localTick == 62) {
                        Location slamLoc = c.clone().add(geyserX.get(g), 0, geyserZ.get(g));
                        DisplayBuilder.particleRing(slamLoc, 3.0, Particle.DUST, 20,
                                new Particle.DustOptions(Color.fromRGB(45, 0, 64), 1.5f));
                        DisplayBuilder.playSound(slamLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
                    }
                }
            }

            // Ambient heartbeat
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkGeyserField(plugin); }
    }

    // ================================================================
    // 108. CORRUPTION MINE -- 12 blocks at ground level in a scattered
    //      pattern, proximity-triggered explosion with shockwave ring
    // ================================================================
    public static class CorruptionMine extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mines = new ArrayList<>();
        private final List<Double> mineX = new ArrayList<>();
        private final List<Double> mineZ = new ArrayList<>();
        private final List<Boolean> detonated = new ArrayList<>();
        private final List<Integer> armDelays = new ArrayList<>();
        private static final int MINE_COUNT = 12;

        public CorruptionMine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_mine", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(48.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(48.0);
            config.setImpactRadius(3.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < MINE_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1.5 + Math.random() * 6.0;
                double mx = Math.cos(angle) * dist;
                double mz = Math.sin(angle) * dist;
                mineX.add(mx);
                mineZ.add(mz);
                detonated.add(false);
                armDelays.add(10 + i * 3);

                Location loc = center.clone().add(mx, -0.3, mz);
                Material mat = (i % 2 == 0) ? Material.SCULK : Material.COAL_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 0.15f, 0.6f).glow(74, 0, 0).interpolation(3, 0);
                mines.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < MINE_COUNT; i++) {
                if (detonated.get(i)) continue;
                if (ticksAlive < armDelays.get(i)) continue;

                Location mineLoc = c.clone().add(mineX.get(i), 0, mineZ.get(i));

                // Pulse glow to warn
                boolean pulse = (ticksAlive % 10 < 5);
                mines.get(i).glow(pulse ? 74 : 45, 0, pulse ? 0 : 64);

                // Subtle warning particle
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(mineLoc.clone().add(0, 0.2, 0), 2, 0.2, 74, 0, 0, 0.6f);
                }

                // Check proximity to any player (detonate at 2.5 blocks)
                boolean shouldDetonate = false;
                for (org.bukkit.entity.Player p : w.getPlayers()) {
                    if (p.getLocation().distance(mineLoc) < 2.5) {
                        shouldDetonate = true;
                        break;
                    }
                }

                // Also detonate sequentially after a long delay
                if (ticksAlive > 250 + i * 8) {
                    shouldDetonate = true;
                }

                if (shouldDetonate) {
                    detonated.set(i, true);

                    // Explosion visual
                    triggerImpactDamage(mineLoc);
                    DisplayBuilder.particleRing(mineLoc, 3.5, Particle.DUST, 24,
                            new Particle.DustOptions(Color.fromRGB(74, 0, 0), 2.0f));
                    DisplayBuilder.dustParticles(mineLoc.clone().add(0, 1, 0), 20, 2.0, 45, 0, 64, 1.5f);
                    w.spawnParticle(Particle.BLOCK, mineLoc, 40, 1.5, 0.5, 1.5, 0.1,
                            Material.SCULK.createBlockData());
                    DisplayBuilder.playSound(mineLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.2f, 0.3f);
                    DisplayBuilder.playSound(mineLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);

                    // Scale up the mine then remove
                    mines.get(i).scale(2.0f, 2.0f, 2.0f);
                    mines.get(i).glow(74, 0, 0);
                    mines.get(i).interpolation(3, 0);
                }
            }

            // Ambient sculk sounds
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionMine(plugin); }
    }

    // ================================================================
    // 109. VOID SINKHOLE -- 14 blocks spiral downward into the ground,
    //      forming a descending helix, particle vortex pulls toward center
    // ================================================================
    public static class VoidSinkhole extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spiralBlocks = new ArrayList<>();
        private static final int BLOCK_COUNT = 14;
        private static final float SPIRAL_RADIUS = 3.0f;
        private static final float DEPTH = 8.0f;

        public VoidSinkhole(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_sinkhole", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = (i / (double) BLOCK_COUNT) * Math.PI * 4; // 2 full rotations
                double r = SPIRAL_RADIUS * (1.0 - i / (double) BLOCK_COUNT * 0.6);
                double ox = Math.cos(angle) * r;
                double oz = Math.sin(angle) * r;
                Location loc = center.clone().add(ox, 0.5, oz);
                Material mat = (i % 4 == 0) ? Material.OBSIDIAN : (i % 4 == 1) ? Material.DEEPSLATE
                        : (i % 4 == 2) ? Material.SCULK : Material.BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f, 0.7f, 0.7f).glow(10, 0, 48).interpolation(3, 0);
                spiralBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.3f);
            DisplayBuilder.dustParticles(center, 30, 3.0, 10, 0, 48, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float sinkProgress = Math.min(1.0f, ticksAlive / 60.0f);
            float rotationOffset = ticksAlive * 0.05f;

            for (int i = 0; i < spiralBlocks.size(); i++) {
                double baseAngle = (i / (double) BLOCK_COUNT) * Math.PI * 4 + rotationOffset;
                double r = SPIRAL_RADIUS * (1.0 - i / (double) BLOCK_COUNT * 0.6) * (1.0 - sinkProgress * 0.3);
                double ox = Math.cos(baseAngle) * r;
                double oz = Math.sin(baseAngle) * r;
                float yDrop = -sinkProgress * DEPTH * (i / (float) BLOCK_COUNT);

                Location target = c.clone().add(ox, 0.5 + yDrop, oz);
                spiralBlocks.get(i).entity().teleport(target);

                // Tilt blocks as they descend
                float tiltAngle = sinkProgress * 0.5f + (float) Math.sin(ticksAlive * 0.1 + i) * 0.15f;
                spiralBlocks.get(i).rotate(tiltAngle, (float) Math.cos(baseAngle), 0, (float) Math.sin(baseAngle));
                spiralBlocks.get(i).interpolation(3, 0);
            }

            // Vortex particle spiral pulling inward
            if (ticksAlive % 3 == 0) {
                double pAngle = ticksAlive * 0.3;
                double pRadius = 4.0 - sinkProgress * 2.0;
                for (int p = 0; p < 4; p++) {
                    double a = pAngle + p * (Math.PI / 2);
                    Location pLoc = c.clone().add(Math.cos(a) * pRadius, 0.5, Math.sin(a) * pRadius);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.2, 10, 0, 48, 1.0f);
                }
            }

            // Deepening darkness at center
            if (ticksAlive % 6 == 0 && sinkProgress > 0.3f) {
                DisplayBuilder.dustParticles(c.clone().add(0, -sinkProgress * 2, 0), 8, 0.5, 45, 0, 64, 1.8f);
            }

            // Warden heartbeat grows louder as sinkhole deepens
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f + sinkProgress * 0.6f, 0.5f);
            }

            // Sculk spreading sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSinkhole(plugin); }
    }

    // ================================================================
    // 110. SHADOW ERUPTION -- 16 blocks burst from the ground staggered
    //      in a cluster, dark hot particles spray outward
    // ================================================================
    public static class ShadowEruption extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> eruptionBlocks = new ArrayList<>();
        private final List<Double> ofsX = new ArrayList<>();
        private final List<Double> ofsZ = new ArrayList<>();
        private final List<Integer> burstDelays = new ArrayList<>();
        private final List<Float> maxHeights = new ArrayList<>();
        private static final int BLOCK_COUNT = 16;

        public ShadowEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_eruption", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 4.0;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                ofsX.add(ox);
                ofsZ.add(oz);
                burstDelays.add((int) (Math.random() * 30));
                maxHeights.add(4.0f + (float) (Math.random() * 8.0));

                Location loc = center.clone().add(ox, -3, oz);
                Material mat = (i % 3 == 0) ? Material.NETHERRACK : (i % 3 == 1) ? Material.BLACKSTONE : Material.SOUL_SOIL;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.8f, 1.0f, 0.8f).glow(74, 0, 0).interpolation(3, 0);
                eruptionBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < BLOCK_COUNT; i++) {
                int delay = burstDelays.get(i);
                if (ticksAlive < delay) continue;

                int localTick = ticksAlive - delay;
                float mh = maxHeights.get(i);

                float yOffset;
                if (localTick < 12) {
                    // Erupting upward
                    float progress = localTick / 12.0f;
                    yOffset = -3.0f + (3.0f + mh) * progress * progress;

                    // Hot dark particles spraying out
                    if (localTick % 2 == 0) {
                        Location base = c.clone().add(ofsX.get(i), yOffset, ofsZ.get(i));
                        DisplayBuilder.dustParticles(base, 10, 1.5, 74, 0, 0, 1.3f);
                        w.spawnParticle(Particle.BLOCK, base, 10, 0.5, 0.3, 0.5, 0,
                                Material.NETHERRACK.createBlockData());
                    }

                    if (localTick == 1) {
                        DisplayBuilder.playSound(c.clone().add(ofsX.get(i), 0, ofsZ.get(i)),
                                Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 0.4f + (float) Math.random() * 0.3f);
                    }
                } else if (localTick < 50) {
                    // Floating with tremor
                    float tremble = (float) Math.sin(localTick * 1.5 + i) * 0.15f;
                    yOffset = mh + tremble;

                    // Spin while floating
                    float spin = localTick * 0.08f;
                    eruptionBlocks.get(i).rotate(spin, 0.3f, 1, 0.3f);
                    eruptionBlocks.get(i).interpolation(2, 0);
                } else {
                    // Falling back
                    float fallProgress = Math.min(1.0f, (localTick - 50) / 15.0f);
                    yOffset = mh * (1.0f - fallProgress * fallProgress);
                }

                Location target = c.clone().add(ofsX.get(i), yOffset, ofsZ.get(i));
                eruptionBlocks.get(i).entity().teleport(target);
            }

            // Ambient ground rumble
            if (ticksAlive % 15 == 0 && ticksAlive < 60) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.7f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowEruption(plugin); }
    }

    // ================================================================
    // 111. DARK QUICKSAND -- 12 flat blocks on ground, bob up/down in
    //      an organic motion, visual trap that pulses darkness
    // ================================================================
    public static class DarkQuicksand extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> sandBlocks = new ArrayList<>();
        private final List<Double> posX = new ArrayList<>();
        private final List<Double> posZ = new ArrayList<>();
        private final List<Float> phaseOffsets = new ArrayList<>();
        private static final int BLOCK_COUNT = 12;

        public DarkQuicksand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_quicksand", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(25);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Clustered pool shape
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = (i / (double) BLOCK_COUNT) * Math.PI * 2;
                double dist = 1.0 + (i % 3) * 1.2;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                posX.add(ox);
                posZ.add(oz);
                phaseOffsets.add((float) (Math.random() * Math.PI * 2));

                Location loc = center.clone().add(ox, -0.2, oz);
                Material mat = (i % 2 == 0) ? Material.SOUL_SOIL : Material.SCULK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.2f, 0.15f, 1.2f).glow(45, 0, 64).interpolation(3, 0);
                sandBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < sandBlocks.size(); i++) {
                // Organic bobbing motion
                float phase = phaseOffsets.get(i);
                float bob = (float) Math.sin(ticksAlive * 0.08 + phase) * 0.25f;
                float bob2 = (float) Math.sin(ticksAlive * 0.12 + phase * 1.5) * 0.1f;
                float yOffset = -0.2f + bob + bob2;

                // Subtle scale breathing
                float scaleBreath = 1.2f + (float) Math.sin(ticksAlive * 0.06 + phase) * 0.15f;

                Location target = c.clone().add(posX.get(i), yOffset, posZ.get(i));
                sandBlocks.get(i).entity().teleport(target);
                sandBlocks.get(i).scale(scaleBreath, 0.15f + Math.abs(bob) * 0.3f, scaleBreath);
                sandBlocks.get(i).interpolation(3, 0);

                // Pulsing glow between dark purple and crimson
                boolean crimsonPulse = (Math.sin(ticksAlive * 0.1 + phase) > 0.3);
                sandBlocks.get(i).glow(crimsonPulse ? 74 : 45, 0, crimsonPulse ? 0 : 64);
            }

            // Sucking downward particles at center
            if (ticksAlive % 5 == 0) {
                for (int p = 0; p < 3; p++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 1.0 + Math.random() * 2.5;
                    Location pLoc = c.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.1, 10, 0, 48, 0.8f);
                }
            }

            // Gurgling sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.5f, 0.3f);
            }

            // Periodic heartbeat
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkQuicksand(plugin); }
    }

    // ================================================================
    // 112. CORRUPTION ROOT NETWORK -- 20 blocks spread across ground
    //      branching from center like corrupted roots, sequential spread
    // ================================================================
    public static class CorruptionRootNetwork extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> roots = new ArrayList<>();
        private final List<Double> rootX = new ArrayList<>();
        private final List<Double> rootZ = new ArrayList<>();
        private final List<Integer> spreadDelays = new ArrayList<>();
        private final List<Boolean> revealed = new ArrayList<>();
        private static final int ROOT_COUNT = 20;

        public CorruptionRootNetwork(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_root_network", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Generate branching root positions
            // 4 main branches, each with 5 segments growing outward
            for (int branch = 0; branch < 4; branch++) {
                double branchAngle = (branch / 4.0) * Math.PI * 2 + Math.random() * 0.5;
                for (int seg = 0; seg < 5; seg++) {
                    double dist = 0.8 + seg * 1.6;
                    double wobble = Math.sin(seg * 2.1 + branch) * 0.6;
                    double ox = Math.cos(branchAngle) * dist + Math.cos(branchAngle + Math.PI / 2) * wobble;
                    double oz = Math.sin(branchAngle) * dist + Math.sin(branchAngle + Math.PI / 2) * wobble;
                    rootX.add(ox);
                    rootZ.add(oz);
                    spreadDelays.add(branch * 5 + seg * 4);
                    revealed.add(false);

                    Location loc = center.clone().add(ox, -0.8, oz);
                    Material mat = (seg % 3 == 0) ? Material.SCULK : (seg % 3 == 1) ? Material.DEEPSLATE : Material.COAL_BLOCK;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.1f, 0.1f, 0.1f).glow(45, 0, 64).interpolation(5, 0);
                    roots.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.2f, 0.3f);
            DisplayBuilder.dustParticles(center, 20, 1.0, 45, 0, 64, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < ROOT_COUNT; i++) {
                if (ticksAlive < spreadDelays.get(i)) continue;

                int localTick = ticksAlive - spreadDelays.get(i);

                if (!revealed.get(i)) {
                    revealed.set(i, true);
                    // Emergence sound
                    Location rootLoc = c.clone().add(rootX.get(i), 0, rootZ.get(i));
                    DisplayBuilder.playSound(rootLoc, Sound.BLOCK_SCULK_BREAK, 0.4f, 0.5f + (float) Math.random() * 0.3f);
                }

                // Grow from hidden to full size
                float growProgress = Math.min(1.0f, localTick / 15.0f);
                float scaleX = 0.1f + growProgress * 1.0f;
                float scaleY = 0.1f + growProgress * 0.3f;
                float scaleZ = 0.1f + growProgress * 0.5f;

                roots.get(i).scale(scaleX, scaleY, scaleZ);
                roots.get(i).interpolation(4, 0);

                // Rise slightly from underground
                float yOffset = -0.8f + growProgress * 0.8f;
                Location target = c.clone().add(rootX.get(i), yOffset, rootZ.get(i));
                roots.get(i).entity().teleport(target);

                // Orient root along branch direction
                double dirAngle = Math.atan2(rootZ.get(i), rootX.get(i));
                roots.get(i).rotate((float) dirAngle, 0, 1, 0);
                roots.get(i).interpolation(4, 0);

                // Corruption particles at root tips
                if (localTick < 20 && localTick % 3 == 0) {
                    DisplayBuilder.dustParticles(target.clone().add(0, 0.2, 0), 3, 0.3, 45, 0, 64, 0.8f);
                }
            }

            // Pulsing corruption wave through the network
            if (ticksAlive > 60 && ticksAlive % 30 == 0) {
                for (int i = 0; i < ROOT_COUNT; i++) {
                    if (!revealed.get(i)) continue;
                    roots.get(i).glow(74, 0, 0);
                }
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.6f);
            }
            if (ticksAlive > 60 && ticksAlive % 30 == 10) {
                for (int i = 0; i < ROOT_COUNT; i++) {
                    if (!revealed.get(i)) continue;
                    roots.get(i).glow(45, 0, 64);
                }
            }

            // Ambient spread sounds
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionRootNetwork(plugin); }
    }

    // ================================================================
    // 113. VOID CALDERA -- 18 blocks form a crater rim in a circle,
    //      center bubbles with dark particle lava, periodic eruption bursts
    // ================================================================
    public static class VoidCaldera extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> rimBlocks = new ArrayList<>();
        private static final int RIM_COUNT = 18;
        private static final float RIM_RADIUS = 4.0f;

        public VoidCaldera(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_caldera", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(340);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < RIM_COUNT; i++) {
                double angle = (i / (double) RIM_COUNT) * Math.PI * 2;
                double ox = Math.cos(angle) * RIM_RADIUS;
                double oz = Math.sin(angle) * RIM_RADIUS;

                // Vary height for natural crater rim
                float heightVar = (float) (Math.sin(i * 1.7) * 0.4 + 0.6);

                Location loc = center.clone().add(ox, heightVar, oz);
                Material mat = (i % 4 == 0) ? Material.OBSIDIAN : (i % 4 == 1) ? Material.DEEPSLATE
                        : (i % 4 == 2) ? Material.BLACKSTONE : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.0f, 0.8f + heightVar, 1.0f).glow(10, 0, 48).interpolation(3, 0);

                // Tilt outward from center
                float tiltAngle = 0.15f + heightVar * 0.1f;
                float tiltAxisX = (float) -Math.sin(angle);
                float tiltAxisZ = (float) Math.cos(angle);
                h.rotate(tiltAngle, tiltAxisX, 0, tiltAxisZ);
                h.interpolation(3, 0);

                rimBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Rim blocks breathe/pulse
            for (int i = 0; i < rimBlocks.size(); i++) {
                float pulse = (float) Math.sin(ticksAlive * 0.06 + i * 0.4) * 0.1f;
                double angle = (i / (double) RIM_COUNT) * Math.PI * 2;
                float baseHeight = (float) (Math.sin(i * 1.7) * 0.4 + 0.6);
                Location target = c.clone().add(
                        Math.cos(angle) * (RIM_RADIUS + pulse),
                        baseHeight + pulse,
                        Math.sin(angle) * (RIM_RADIUS + pulse)
                );
                rimBlocks.get(i).entity().teleport(target);
            }

            // Bubbling dark "lava" particles at center
            if (ticksAlive % 2 == 0) {
                for (int p = 0; p < 4; p++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 2.5;
                    Location bubbleLoc = c.clone().add(Math.cos(a) * r, 0.2 + Math.random() * 0.5, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(bubbleLoc, 2, 0.2, 74, 0, 0, 1.2f);
                }
            }

            // Periodic eruption bursts from center
            if (ticksAlive % 50 == 0 && ticksAlive > 20) {
                // Spray upward
                for (int p = 0; p < 15; p++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 1.5;
                    Location sprayLoc = c.clone().add(Math.cos(a) * r, 1 + Math.random() * 4, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(sprayLoc, 3, 0.5, 45, 0, 64, 1.5f);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.5f);

                // Flash rim crimson
                for (BlockDisplayHandle h : rimBlocks) {
                    h.glow(74, 0, 0);
                }
            }
            if (ticksAlive % 50 == 15) {
                for (BlockDisplayHandle h : rimBlocks) {
                    h.glow(10, 0, 48);
                }
            }

            // Void blue ambient ring
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), RIM_RADIUS - 0.5, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(10, 0, 48), 1.0f));
            }

            // Heartbeat rumble
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidCaldera(plugin); }
    }

    // ================================================================
    // 114. SHADOW MINEFIELD -- 15 small blocks on ground, flash with a
    //      warning glow before detonating one by one in sequence
    // ================================================================
    public static class ShadowMinefield extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> mineBlocks = new ArrayList<>();
        private final List<Double> mineX = new ArrayList<>();
        private final List<Double> mineZ = new ArrayList<>();
        private final List<Integer> detonateOrder = new ArrayList<>();
        private final List<Boolean> detonated = new ArrayList<>();
        private static final int MINE_COUNT = 15;
        private static final int FLASH_DURATION = 15;
        private static final int DETONATE_INTERVAL = 12;

        public ShadowMinefield(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_minefield", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(44.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Place mines in scattered positions
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < MINE_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1.0 + Math.random() * 6.0;
                mineX.add(Math.cos(angle) * dist);
                mineZ.add(Math.sin(angle) * dist);
                detonated.add(false);
                order.add(i);

                Location loc = center.clone().add(mineX.get(i), -0.2, mineZ.get(i));
                Material mat = (i % 3 == 0) ? Material.SCULK : (i % 3 == 1) ? Material.COAL_BLOCK : Material.BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.5f, 0.12f, 0.5f).glow(10, 0, 48).interpolation(3, 0);
                mineBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Shuffle detonation order for unpredictability
            java.util.Collections.shuffle(order);
            detonateOrder.addAll(order);

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Determine which mine is currently in warning/detonation phase
            int startTick = 40; // initial calm before detonation sequence begins
            if (ticksAlive < startTick) {
                // Idle pulse on all mines
                boolean pulse = (ticksAlive % 12 < 6);
                for (int i = 0; i < MINE_COUNT; i++) {
                    mineBlocks.get(i).glow(pulse ? 45 : 10, 0, pulse ? 64 : 48);
                }
                return;
            }

            int elapsed = ticksAlive - startTick;
            int currentMineSeq = elapsed / DETONATE_INTERVAL;

            for (int seq = 0; seq <= currentMineSeq && seq < MINE_COUNT; seq++) {
                int mineIdx = detonateOrder.get(seq);
                if (detonated.get(mineIdx)) continue;

                int seqTick = elapsed - seq * DETONATE_INTERVAL;

                if (seqTick < FLASH_DURATION) {
                    // Warning flash: rapid blink
                    boolean flash = (seqTick % 4 < 2);
                    mineBlocks.get(mineIdx).glow(flash ? 74 : 10, 0, flash ? 0 : 48);
                    mineBlocks.get(mineIdx).scale(0.5f + (seqTick / (float) FLASH_DURATION) * 0.3f, 0.12f, 0.5f + (seqTick / (float) FLASH_DURATION) * 0.3f);
                    mineBlocks.get(mineIdx).interpolation(1, 0);

                    // Warning sound
                    if (seqTick == 0) {
                        Location mineLoc = c.clone().add(mineX.get(mineIdx), 0, mineZ.get(mineIdx));
                        DisplayBuilder.playSound(mineLoc, Sound.BLOCK_SCULK_BREAK, 0.6f, 1.5f);
                    }
                } else {
                    // DETONATE
                    detonated.set(mineIdx, true);
                    Location mineLoc = c.clone().add(mineX.get(mineIdx), 0, mineZ.get(mineIdx));

                    triggerImpactDamage(mineLoc);

                    // Explosion visuals
                    DisplayBuilder.particleRing(mineLoc, 3.0, Particle.DUST, 20,
                            new Particle.DustOptions(Color.fromRGB(74, 0, 0), 1.8f));
                    DisplayBuilder.dustParticles(mineLoc.clone().add(0, 1, 0), 15, 1.5, 45, 0, 64, 1.5f);
                    w.spawnParticle(Particle.BLOCK, mineLoc, 25, 1.0, 0.5, 1.0, 0.05,
                            Material.SCULK.createBlockData());
                    DisplayBuilder.playSound(mineLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.4f);
                    DisplayBuilder.playSound(mineLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.5f);

                    // Expand and fade
                    mineBlocks.get(mineIdx).scale(1.8f, 1.8f, 1.8f);
                    mineBlocks.get(mineIdx).glow(74, 0, 0);
                    mineBlocks.get(mineIdx).interpolation(5, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowMinefield(plugin); }
    }

    // ================================================================
    // 115. DARK BLOOM -- 14 blocks in a flower shape, petals open
    //      outward and upward, releases particle burst at full bloom
    // ================================================================
    public static class DarkBloom extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> petalBlocks = new ArrayList<>();
        private final List<Double> petalAngles = new ArrayList<>();
        private static final int PETAL_COUNT = 12;
        private static final int CENTER_COUNT = 2; // 12 petals + 2 center = 14
        private BlockDisplayHandle centerCore;
        private BlockDisplayHandle centerTop;

        public DarkBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_bloom", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(42.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Center core blocks
            centerCore = displayBuilder.spawnBlock(center.clone().add(0, 0, 0), Material.CRYING_OBSIDIAN);
            centerCore.scale(0.8f, 0.8f, 0.8f).glow(74, 0, 0).interpolation(3, 0);
            spawnedEntities.add(centerCore.entity());

            centerTop = displayBuilder.spawnBlock(center.clone().add(0, 0.8, 0), Material.SCULK);
            centerTop.scale(0.5f, 0.5f, 0.5f).glow(45, 0, 64).interpolation(3, 0);
            spawnedEntities.add(centerTop.entity());

            // 12 petals in two rings (inner 6, outer 6)
            for (int i = 0; i < PETAL_COUNT; i++) {
                double angle = (i / 6.0) * Math.PI * 2;
                if (i >= 6) angle += Math.PI / 6; // offset outer ring
                petalAngles.add(angle);

                Location loc = center.clone().add(Math.cos(angle) * 0.3, 0, Math.sin(angle) * 0.3);
                Material mat = (i < 6) ? Material.DEEPSLATE : Material.BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 0.2f, 0.3f).glow(45, 0, 64).interpolation(3, 0);
                petalBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float bloomProgress = Math.min(1.0f, ticksAlive / 50.0f);
            boolean fullBloom = ticksAlive > 50;

            // Petals open outward
            for (int i = 0; i < petalBlocks.size(); i++) {
                double angle = petalAngles.get(i);
                boolean isOuter = (i >= 6);
                float ringDist = isOuter ? 1.5f : 0.8f;
                float openDist = ringDist + bloomProgress * (isOuter ? 2.5f : 1.5f);
                float yLift = bloomProgress * (isOuter ? 0.4f : 0.8f);

                // Petals tilt upward as they open
                float tiltAngle = bloomProgress * (isOuter ? 0.6f : 0.4f);

                Location petalLoc = c.clone().add(
                        Math.cos(angle) * openDist,
                        yLift,
                        Math.sin(angle) * openDist
                );
                petalBlocks.get(i).entity().teleport(petalLoc);

                float tiltAxisX = (float) -Math.sin(angle);
                float tiltAxisZ = (float) Math.cos(angle);
                petalBlocks.get(i).rotate(tiltAngle, tiltAxisX, 0, tiltAxisZ);
                petalBlocks.get(i).interpolation(3, 0);

                // Grow petals as bloom opens
                float scaleGrow = 0.6f + bloomProgress * 0.6f;
                petalBlocks.get(i).scale(scaleGrow, 0.2f + bloomProgress * 0.15f, 0.3f + bloomProgress * 0.3f);
            }

            // Center core pulses
            if (centerCore != null) {
                float cPulse = 0.8f + (float) Math.sin(ticksAlive * 0.15) * 0.15f;
                centerCore.scale(cPulse, cPulse, cPulse);
                centerCore.interpolation(2, 0);
            }
            if (centerTop != null) {
                float tRise = bloomProgress * 1.2f;
                centerTop.entity().teleport(c.clone().add(0, 0.8 + tRise, 0));
                float tPulse = 0.5f + (float) Math.sin(ticksAlive * 0.2) * 0.2f;
                centerTop.scale(tPulse, tPulse, tPulse);
                centerTop.interpolation(2, 0);
            }

            // Full bloom: particle burst release
            if (fullBloom && ticksAlive % 25 == 0) {
                for (int p = 0; p < 20; p++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 4.0;
                    double h = 0.5 + Math.random() * 3.0;
                    Location pLoc = c.clone().add(Math.cos(a) * r, h, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.3, 45, 0, 64, 1.2f);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.6f, 0.8f);
            }

            // Heartbeat when fully bloomed
            if (fullBloom && ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.7f);
                // Crimson pulse on all blocks
                for (BlockDisplayHandle h : petalBlocks) { h.glow(74, 0, 0); }
                if (centerCore != null) centerCore.glow(74, 0, 0);
            }
            if (fullBloom && ticksAlive % 40 == 12) {
                for (BlockDisplayHandle h : petalBlocks) { h.glow(45, 0, 64); }
                if (centerCore != null) centerCore.glow(74, 0, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkBloom(plugin); }
    }

    // ================================================================
    // 116. CORRUPTION CRATER -- 16 blocks form an impact crater shape,
    //      debris chunks orbit above the crater center
    // ================================================================
    public static class CorruptionCrater extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> craterRim = new ArrayList<>();
        private final List<BlockDisplayHandle> debrisOrbit = new ArrayList<>();
        private static final int RIM_COUNT = 11;
        private static final int DEBRIS_COUNT = 5; // 11 + 5 = 16
        private static final float CRATER_RADIUS = 3.5f;

        public CorruptionCrater(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_crater", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Crater rim
            for (int i = 0; i < RIM_COUNT; i++) {
                double angle = (i / (double) RIM_COUNT) * Math.PI * 2;
                double ox = Math.cos(angle) * CRATER_RADIUS;
                double oz = Math.sin(angle) * CRATER_RADIUS;
                float heightVar = (float) (Math.sin(i * 2.3) * 0.3 + 0.4);

                Location loc = center.clone().add(ox, heightVar - 0.3, oz);
                Material mat = (i % 3 == 0) ? Material.OBSIDIAN : (i % 3 == 1) ? Material.DEEPSLATE : Material.BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.1f, 0.5f + heightVar, 1.1f).glow(10, 0, 48).interpolation(3, 0);

                // Tilt inward toward crater center
                float tiltAngle = -0.2f;
                float tiltAxisX = (float) -Math.sin(angle);
                float tiltAxisZ = (float) Math.cos(angle);
                h.rotate(tiltAngle, tiltAxisX, 0, tiltAxisZ);
                h.interpolation(3, 0);

                craterRim.add(h);
                spawnedEntities.add(h.entity());
            }

            // Debris chunks hovering above
            for (int i = 0; i < DEBRIS_COUNT; i++) {
                Location debrisLoc = center.clone().add(0, 3 + i * 0.5, 0);
                Material mat = (i % 2 == 0) ? Material.SCULK : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle d = displayBuilder.spawnBlock(debrisLoc, mat);
                d.scale(0.4f + (float) Math.random() * 0.3f, 0.4f + (float) Math.random() * 0.3f, 0.4f + (float) Math.random() * 0.3f);
                d.glow(45, 0, 64).interpolation(3, 0);
                debrisOrbit.add(d);
                spawnedEntities.add(d.entity());
            }

            // Impact sound
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
            w.spawnParticle(Particle.BLOCK, center, 60, 2, 0.5, 2, 0.1,
                    Material.DEEPSLATE.createBlockData());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Debris orbits above crater
            for (int i = 0; i < debrisOrbit.size(); i++) {
                double orbitAngle = ticksAlive * 0.04 + (i / (double) DEBRIS_COUNT) * Math.PI * 2;
                double orbitRadius = 1.5 + Math.sin(ticksAlive * 0.02 + i) * 0.5;
                float orbitY = 3.0f + (float) Math.sin(ticksAlive * 0.06 + i * 1.3) * 0.5f;

                Location target = c.clone().add(
                        Math.cos(orbitAngle) * orbitRadius,
                        orbitY,
                        Math.sin(orbitAngle) * orbitRadius
                );
                debrisOrbit.get(i).entity().teleport(target);

                // Spin debris
                float spin = ticksAlive * 0.1f + i * 1.2f;
                debrisOrbit.get(i).rotate(spin, 0.5f, 1, 0.3f);
                debrisOrbit.get(i).interpolation(2, 0);
            }

            // Crater rim pulses
            if (ticksAlive % 35 == 0) {
                for (BlockDisplayHandle h : craterRim) { h.glow(74, 0, 0); }
            }
            if (ticksAlive % 35 == 12) {
                for (BlockDisplayHandle h : craterRim) { h.glow(10, 0, 48); }
            }

            // Center void particles
            if (ticksAlive % 3 == 0) {
                Location centerGround = c.clone().add(0, -0.2, 0);
                DisplayBuilder.dustParticles(centerGround, 5, 1.5, 10, 0, 48, 1.0f);
            }

            // Particle trail behind debris
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < debrisOrbit.size(); i++) {
                    Location debrisLoc = debrisOrbit.get(i).entity().getLocation();
                    DisplayBuilder.dustParticles(debrisLoc, 2, 0.2, 45, 0, 64, 0.8f);
                }
            }

            // Heartbeat
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.6f);
            }

            // Sculk ambient
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionCrater(plugin); }
    }

    // ================================================================
    // 117. VOID QUICKSAND POOL -- 12 flat blocks forming a dark pool,
    //      particles pull downward simulating sinking, periodic bubbles
    // ================================================================
    public static class VoidQuicksandPool extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> poolBlocks = new ArrayList<>();
        private final List<Double> poolX = new ArrayList<>();
        private final List<Double> poolZ = new ArrayList<>();
        private static final int BLOCK_COUNT = 12;

        public VoidQuicksandPool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_quicksand_pool", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(380);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Irregular pool shape
            for (int i = 0; i < BLOCK_COUNT; i++) {
                double angle = (i / (double) BLOCK_COUNT) * Math.PI * 2;
                double dist = 1.2 + Math.sin(i * 1.9) * 0.8;
                double ox = Math.cos(angle) * dist;
                double oz = Math.sin(angle) * dist;
                poolX.add(ox);
                poolZ.add(oz);

                Location loc = center.clone().add(ox, -0.35, oz);
                Material mat = (i % 3 == 0) ? Material.OBSIDIAN : (i % 3 == 1) ? Material.SCULK : Material.COAL_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(1.3f, 0.1f, 1.3f).glow(10, 0, 48).interpolation(3, 0);
                poolBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.9f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Pool surface undulates
            for (int i = 0; i < poolBlocks.size(); i++) {
                float wave = (float) Math.sin(ticksAlive * 0.07 + i * 0.9) * 0.08f;
                float wave2 = (float) Math.cos(ticksAlive * 0.05 + i * 1.3) * 0.04f;
                Location target = c.clone().add(poolX.get(i), -0.35 + wave + wave2, poolZ.get(i));
                poolBlocks.get(i).entity().teleport(target);

                // Slight scale variation for liquid feel
                float sVar = 1.3f + (float) Math.sin(ticksAlive * 0.04 + i) * 0.1f;
                poolBlocks.get(i).scale(sVar, 0.1f, sVar);
                poolBlocks.get(i).interpolation(3, 0);
            }

            // Downward-pulling particles (simulating sinking)
            if (ticksAlive % 4 == 0) {
                for (int p = 0; p < 3; p++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 2.0;
                    Location pullLoc = c.clone().add(Math.cos(a) * r, 0.5, Math.sin(a) * r);
                    // Spawn at slight height, particle visually "falls"
                    DisplayBuilder.dustParticles(pullLoc, 2, 0.1, 10, 0, 48, 0.7f);
                }
            }

            // Bubble bursts -- random locations in pool
            if (ticksAlive % 12 == 0) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 1.5;
                Location bubbleLoc = c.clone().add(Math.cos(a) * r, -0.1, Math.sin(a) * r);
                DisplayBuilder.dustParticles(bubbleLoc, 4, 0.3, 45, 0, 64, 1.0f);
                DisplayBuilder.playSound(bubbleLoc, Sound.BLOCK_SCULK_BREAK, 0.3f, 0.6f + (float) Math.random() * 0.4f);
            }

            // Corruption spreading rings
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, -0.2, 0), 2.5, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(10, 0, 48), 1.0f));
            }

            // Heartbeat
            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidQuicksandPool(plugin); }
    }

    // ================================================================
    // 118. SHADOW LANDSLIDE -- 18 blocks tumble from an elevated
    //      position across the ground in a rolling cascade
    // ================================================================
    public static class ShadowLandslide extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> slideBlocks = new ArrayList<>();
        private final List<Float> slideDelays = new ArrayList<>();
        private final List<Float> slideSpeedX = new ArrayList<>();
        private final List<Float> slideSpeedZ = new ArrayList<>();
        private final List<Float> currentX = new ArrayList<>();
        private final List<Float> currentY = new ArrayList<>();
        private final List<Float> currentZ = new ArrayList<>();
        private final List<Boolean> settled = new ArrayList<>();
        private static final int BLOCK_COUNT = 18;
        private static final float START_HEIGHT = 10.0f;

        public ShadowLandslide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_landslide", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(46.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(46.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Slide direction (random)
            float dirAngle = (float) (Math.random() * Math.PI * 2);
            float dirX = (float) Math.cos(dirAngle);
            float dirZ = (float) Math.sin(dirAngle);

            for (int i = 0; i < BLOCK_COUNT; i++) {
                // Start clustered at elevated position behind center
                float startOffX = -dirX * 5.0f + (float) (Math.random() - 0.5) * 3.0f;
                float startOffZ = -dirZ * 5.0f + (float) (Math.random() - 0.5) * 3.0f;
                float startY = START_HEIGHT + (float) (Math.random() * 3.0);

                currentX.add(startOffX);
                currentY.add(startY);
                currentZ.add(startOffZ);
                slideDelays.add(i * 2.0f + (float) (Math.random() * 6));
                slideSpeedX.add(dirX * (0.3f + (float) Math.random() * 0.25f));
                slideSpeedZ.add(dirZ * (0.3f + (float) Math.random() * 0.25f));
                settled.add(false);

                Location loc = center.clone().add(startOffX, startY, startOffZ);
                Material mat;
                switch (i % 4) {
                    case 0: mat = Material.DEEPSLATE; break;
                    case 1: mat = Material.BLACKSTONE; break;
                    case 2: mat = Material.OBSIDIAN; break;
                    default: mat = Material.NETHERRACK; break;
                }
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float s = 0.6f + (float) Math.random() * 0.5f;
                h.scale(s, s, s).glow(45, 0, 64).interpolation(2, 0);
                slideBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(-dirX * 5, START_HEIGHT, -dirZ * 5),
                    Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < BLOCK_COUNT; i++) {
                if (ticksAlive < slideDelays.get(i)) continue;
                if (settled.get(i)) continue;

                // Gravity + slide momentum
                float x = currentX.get(i) + slideSpeedX.get(i);
                float y = currentY.get(i) - 0.35f; // gravity
                float z = currentZ.get(i) + slideSpeedZ.get(i);

                // Ground collision
                if (y <= 0) {
                    y = 0;
                    settled.set(i, true);

                    Location impactLoc = c.clone().add(x, 0, z);
                    triggerImpactDamage(impactLoc);

                    // Impact dust
                    DisplayBuilder.dustParticles(impactLoc, 8, 1.0, 45, 0, 64, 1.2f);
                    w.spawnParticle(Particle.BLOCK, impactLoc, 15, 0.5, 0.2, 0.5, 0,
                            Material.DEEPSLATE.createBlockData());
                    DisplayBuilder.playSound(impactLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.4f + (float) Math.random() * 0.3f);
                }

                currentX.set(i, x);
                currentY.set(i, y);
                currentZ.set(i, z);

                Location target = c.clone().add(x, y, z);
                slideBlocks.get(i).entity().teleport(target);

                // Tumble rotation
                if (!settled.get(i)) {
                    float tumble = ticksAlive * 0.15f + i * 0.7f;
                    slideBlocks.get(i).rotate(tumble, slideSpeedZ.get(i), 0.2f, -slideSpeedX.get(i));
                    slideBlocks.get(i).interpolation(2, 0);
                }

                // Trailing dust
                if (!settled.get(i) && ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(target, 2, 0.3, 10, 0, 48, 0.7f);
                }
            }

            // Rumbling sound during slide
            if (ticksAlive % 10 == 0 && ticksAlive < 80) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowLandslide(plugin); }
    }

    // ================================================================
    // 119. CORRUPTION GEYSER RING -- 16 blocks in a circle, erupt one
    //      by one clockwise, each shoots up and falls back down
    // ================================================================
    public static class CorruptionGeyserRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> geyserBlocks = new ArrayList<>();
        private final List<Float> geyserYPositions = new ArrayList<>();
        private final List<Boolean> hasErupted = new ArrayList<>();
        private static final int GEYSER_COUNT = 16;
        private static final float RING_RADIUS = 5.0f;
        private static final float ERUPT_HEIGHT = 10.0f;
        private static final int TICKS_PER_GEYSER = 10;

        public CorruptionGeyserRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_geyser_ring", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(44.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(44.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < GEYSER_COUNT; i++) {
                double angle = (i / (double) GEYSER_COUNT) * Math.PI * 2;
                double ox = Math.cos(angle) * RING_RADIUS;
                double oz = Math.sin(angle) * RING_RADIUS;

                Location loc = center.clone().add(ox, -1.0, oz);
                Material mat = (i % 4 == 0) ? Material.SCULK : (i % 4 == 1) ? Material.DEEPSLATE
                        : (i % 4 == 2) ? Material.CRYING_OBSIDIAN : Material.BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f, 1.2f, 0.7f).glow(45, 0, 64).interpolation(3, 0);
                geyserBlocks.add(h);
                spawnedEntities.add(h.entity());
                geyserYPositions.add(-1.0f);
                hasErupted.add(false);
            }

            // Warning ring
            DisplayBuilder.particleRing(center, RING_RADIUS, Particle.DUST, 32,
                    new Particle.DustOptions(Color.fromRGB(45, 0, 64), 1.2f));
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int startDelay = 20; // brief calm before eruptions begin
            if (ticksAlive < startDelay) {
                // Pre-eruption rumble
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.4f, 0.5f);
                }
                return;
            }

            int elapsed = ticksAlive - startDelay;

            for (int i = 0; i < GEYSER_COUNT; i++) {
                double angle = (i / (double) GEYSER_COUNT) * Math.PI * 2;
                double ox = Math.cos(angle) * RING_RADIUS;
                double oz = Math.sin(angle) * RING_RADIUS;

                int geyserStart = i * TICKS_PER_GEYSER;
                if (elapsed < geyserStart) continue;

                int localTick = elapsed - geyserStart;

                float y;
                if (localTick < 8) {
                    // Erupting upward
                    float progress = localTick / 8.0f;
                    y = -1.0f + (1.0f + ERUPT_HEIGHT) * progress * progress;

                    // First tick: eruption sound + particles
                    if (!hasErupted.get(i)) {
                        hasErupted.set(i, true);
                        Location eruptLoc = c.clone().add(ox, 0, oz);
                        DisplayBuilder.playSound(eruptLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f, 0.5f + i * 0.03f);
                        DisplayBuilder.dustParticles(eruptLoc, 12, 1.0, 74, 0, 0, 1.5f);
                        triggerImpactDamage(eruptLoc);
                    }

                    // Rising particles
                    if (localTick % 2 == 0) {
                        Location particleLoc = c.clone().add(ox, y * 0.5, oz);
                        DisplayBuilder.dustParticles(particleLoc, 5, 0.5, 45, 0, 64, 1.2f);
                    }
                } else if (localTick < 25) {
                    // Hover at apex with tremor
                    float tremble = (float) Math.sin(localTick * 2.0) * 0.2f;
                    y = ERUPT_HEIGHT + tremble;

                    // Glow shift at apex
                    geyserBlocks.get(i).glow(74, 0, 0);
                } else if (localTick < 38) {
                    // Fall back
                    float fallProgress = (localTick - 25) / 13.0f;
                    y = ERUPT_HEIGHT * (1.0f - fallProgress * fallProgress);
                    geyserBlocks.get(i).glow(10, 0, 48);
                } else {
                    y = 0;
                    // Settled tremor
                    y = (float) Math.sin(localTick * 0.3) * 0.05f;
                }

                geyserYPositions.set(i, y);
                Location target = c.clone().add(ox, y, oz);
                geyserBlocks.get(i).entity().teleport(target);

                // Rotation as it erupts/falls
                if (localTick < 38) {
                    float spin = localTick * 0.08f;
                    geyserBlocks.get(i).rotate(spin, 0, 1, 0);
                    geyserBlocks.get(i).interpolation(2, 0);
                }
            }

            // Ambient sculk
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionGeyserRing(plugin); }
    }

    // ================================================================
    // 120. DARK APOCALYPSE -- FINALE ATTACK
    //      25+ blocks: 4 pillars + connecting arches + floating core
    //      Rotates, pulses, 60+ HP damage, massive corruption construct
    // ================================================================
    public static class DarkApocalypse extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>(); // 4 pillars x 4 = 16
        private final List<BlockDisplayHandle> archBlocks = new ArrayList<>();   // 4 arches x 2 = 8
        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();   // 3 core blocks
        private static final int PILLARS = 4;
        private static final int BLOCKS_PER_PILLAR = 4;
        private static final int ARCHES_PER_PAIR = 2;
        private static final float PILLAR_RADIUS = 5.0f;
        private static final float PILLAR_HEIGHT = 8.0f;
        private static final float CORE_HEIGHT = 10.0f;
        private float rotationAngle = 0;

        public DarkApocalypse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_apocalypse", AttackType.BLOCK_DISPLAY, 1, "modes/corruption/attacks"));
            config.setDamage(64.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(450);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 pillars, each 4 blocks tall
            for (int p = 0; p < PILLARS; p++) {
                double angle = (p / (double) PILLARS) * Math.PI * 2;
                double px = Math.cos(angle) * PILLAR_RADIUS;
                double pz = Math.sin(angle) * PILLAR_RADIUS;

                for (int h = 0; h < BLOCKS_PER_PILLAR; h++) {
                    Location loc = center.clone().add(px, h * 2.0, pz);
                    Material mat;
                    switch (h % 4) {
                        case 0: mat = Material.OBSIDIAN; break;
                        case 1: mat = Material.DEEPSLATE; break;
                        case 2: mat = Material.BLACKSTONE; break;
                        default: mat = Material.CRYING_OBSIDIAN; break;
                    }
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                    block.scale(1.2f, 2.0f, 1.2f).glow(10, 0, 48).interpolation(3, 0);
                    pillarBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            // 4 arches connecting adjacent pillars (2 segments each)
            for (int a = 0; a < PILLARS; a++) {
                double angle1 = (a / (double) PILLARS) * Math.PI * 2;
                double angle2 = ((a + 1) / (double) PILLARS) * Math.PI * 2;
                double midAngle = (angle1 + angle2) / 2.0;

                for (int s = 0; s < ARCHES_PER_PAIR; s++) {
                    double blendAngle = angle1 + (angle2 - angle1) * ((s + 0.5) / ARCHES_PER_PAIR);
                    double ar = PILLAR_RADIUS * 0.85;
                    double ax = Math.cos(blendAngle) * ar;
                    double az = Math.sin(blendAngle) * ar;
                    float archY = PILLAR_HEIGHT - 0.5f + (float) Math.sin((s + 0.5) / ARCHES_PER_PAIR * Math.PI) * 1.5f;

                    Location loc = center.clone().add(ax, archY, az);
                    Material mat = (s == 0) ? Material.SCULK : Material.NETHERRACK;
                    BlockDisplayHandle block = displayBuilder.spawnBlock(loc, mat);
                    block.scale(1.5f, 0.6f, 0.6f).glow(45, 0, 64).interpolation(3, 0);

                    // Rotate arch to align between pillars
                    float archAngle = (float) blendAngle;
                    block.rotate(archAngle, 0, 1, 0);
                    block.interpolation(3, 0);

                    archBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            // Floating core: 3 blocks (main core + 2 orbiting shards)
            BlockDisplayHandle mainCore = displayBuilder.spawnBlock(center.clone().add(0, CORE_HEIGHT, 0), Material.CRYING_OBSIDIAN);
            mainCore.scale(1.5f, 1.5f, 1.5f).glow(74, 0, 0).interpolation(3, 0);
            coreBlocks.add(mainCore);
            spawnedEntities.add(mainCore.entity());

            for (int s = 0; s < 2; s++) {
                double sAngle = s * Math.PI;
                Location sLoc = center.clone().add(Math.cos(sAngle) * 1.5, CORE_HEIGHT, Math.sin(sAngle) * 1.5);
                Material sMat = (s == 0) ? Material.SCULK : Material.OBSIDIAN;
                BlockDisplayHandle shard = displayBuilder.spawnBlock(sLoc, sMat);
                shard.scale(0.6f, 0.6f, 0.6f).glow(45, 0, 64).interpolation(3, 0);
                coreBlocks.add(shard);
                spawnedEntities.add(shard.entity());
            }

            // Grand emergence sounds
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.5f, 0.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.3f);
            DisplayBuilder.dustParticles(center, 50, 5.0, 45, 0, 64, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slow rotation of entire structure
            rotationAngle += 0.015f;

            // Rotate pillars around center
            for (int p = 0; p < PILLARS; p++) {
                double baseAngle = (p / (double) PILLARS) * Math.PI * 2 + rotationAngle;
                double px = Math.cos(baseAngle) * PILLAR_RADIUS;
                double pz = Math.sin(baseAngle) * PILLAR_RADIUS;

                for (int h = 0; h < BLOCKS_PER_PILLAR; h++) {
                    int idx = p * BLOCKS_PER_PILLAR + h;
                    // Pillar sway
                    float sway = (float) Math.sin(ticksAlive * 0.04 + p * 1.5) * 0.15f;
                    Location target = c.clone().add(px + sway, h * 2.0, pz + sway);
                    pillarBlocks.get(idx).entity().teleport(target);
                }
            }

            // Rotate arches
            for (int a = 0; a < PILLARS; a++) {
                double angle1 = (a / (double) PILLARS) * Math.PI * 2 + rotationAngle;
                double angle2 = ((a + 1) / (double) PILLARS) * Math.PI * 2 + rotationAngle;

                for (int s = 0; s < ARCHES_PER_PAIR; s++) {
                    int idx = a * ARCHES_PER_PAIR + s;
                    double blendAngle = angle1 + (angle2 - angle1) * ((s + 0.5) / ARCHES_PER_PAIR);
                    double ar = PILLAR_RADIUS * 0.85;
                    double ax = Math.cos(blendAngle) * ar;
                    double az = Math.sin(blendAngle) * ar;
                    float archY = PILLAR_HEIGHT - 0.5f + (float) Math.sin((s + 0.5) / ARCHES_PER_PAIR * Math.PI) * 1.5f;

                    Location target = c.clone().add(ax, archY, az);
                    archBlocks.get(idx).entity().teleport(target);
                    archBlocks.get(idx).rotate((float) blendAngle, 0, 1, 0);
                    archBlocks.get(idx).interpolation(2, 0);
                }
            }

            // Core: levitate with bob + orbiting shards
            float coreBob = (float) Math.sin(ticksAlive * 0.06) * 0.5f;
            Location corePos = c.clone().add(0, CORE_HEIGHT + coreBob, 0);
            coreBlocks.get(0).entity().teleport(corePos);

            // Core rotation
            float coreSpin = ticksAlive * 0.05f;
            coreBlocks.get(0).rotate(coreSpin, 0.3f, 1, 0.3f);
            coreBlocks.get(0).interpolation(2, 0);

            // Orbiting shards
            for (int s = 0; s < 2; s++) {
                double sAngle = ticksAlive * 0.08 + s * Math.PI;
                double orbitR = 1.5 + Math.sin(ticksAlive * 0.03 + s) * 0.3;
                Location shardLoc = c.clone().add(
                        Math.cos(sAngle) * orbitR,
                        CORE_HEIGHT + coreBob + Math.sin(ticksAlive * 0.1 + s * 2) * 0.8,
                        Math.sin(sAngle) * orbitR
                );
                coreBlocks.get(1 + s).entity().teleport(shardLoc);
                float shardSpin = ticksAlive * 0.12f + s * 1.5f;
                coreBlocks.get(1 + s).rotate(shardSpin, 0.5f, 1, 0.5f);
                coreBlocks.get(1 + s).interpolation(2, 0);
            }

            // === PULSE EFFECTS ===
            // Major corruption pulse every 40 ticks
            if (ticksAlive % 40 == 0) {
                // Flash all blocks crimson
                for (BlockDisplayHandle h : pillarBlocks) { h.glow(74, 0, 0); }
                for (BlockDisplayHandle h : archBlocks) { h.glow(74, 0, 0); }
                coreBlocks.get(0).glow(74, 0, 0);

                // Expanding corruption ring
                DisplayBuilder.particleRing(c, PILLAR_RADIUS + 2, Particle.DUST, 40,
                        new Particle.DustOptions(Color.fromRGB(74, 0, 0), 2.0f));

                // Heartbeat
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.4f);
            }
            if (ticksAlive % 40 == 15) {
                for (BlockDisplayHandle h : pillarBlocks) { h.glow(10, 0, 48); }
                for (BlockDisplayHandle h : archBlocks) { h.glow(45, 0, 64); }
                coreBlocks.get(0).glow(74, 0, 0);
            }

            // Core energy beams (particles from core to each pillar top)
            if (ticksAlive % 6 == 0) {
                for (int p = 0; p < PILLARS; p++) {
                    double baseAngle = (p / (double) PILLARS) * Math.PI * 2 + rotationAngle;
                    double px = Math.cos(baseAngle) * PILLAR_RADIUS;
                    double pz = Math.sin(baseAngle) * PILLAR_RADIUS;

                    // 3 particles along beam
                    for (int b = 0; b < 3; b++) {
                        float t = b / 3.0f;
                        Location beamLoc = c.clone().add(
                                px * t,
                                PILLAR_HEIGHT * (1 - t) + CORE_HEIGHT * t + coreBob * t,
                                pz * t
                        );
                        DisplayBuilder.dustParticles(beamLoc, 2, 0.1, 45, 0, 64, 1.0f);
                    }
                }
            }

            // Ground corruption particles spreading outward
            if (ticksAlive % 4 == 0) {
                for (int p = 0; p < 5; p++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * (PILLAR_RADIUS + 2);
                    Location groundLoc = c.clone().add(Math.cos(a) * r, 0.1, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(groundLoc, 2, 0.3, 10, 0, 48, 0.8f);
                }
            }

            // Sculk spread ambient
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.7f, 0.3f);
            }

            // Respawn anchor deplete for ominous power surge
            if (ticksAlive % 60 == 30) {
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkApocalypse(plugin); }
    }
}
