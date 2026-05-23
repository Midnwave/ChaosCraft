package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain Mode — BLOCK DISPLAY ATTACKS 71-75 (Signature, part 2 — 5 flagship setpieces)
 *
 * The mode-defining setpieces. Each is intentionally larger and more
 * dramatic than the surrounding pendulum/falling/reaching attacks: spawn
 * buildup → spectacle phase → dramatic dissipate.
 *
 * 71. ChainTsunami    — 60-block wave that sweeps across the arena
 * 72. IronGolemFist   — 50-block descending fist + shockwave impact
 * 73. ChainWebTrap    — 56-block ground web tightening around player
 * 74. IronGiant       — 52-block colossus that lumbers via follow-AI
 * 75. IronCathedral   — 70-block cathedral, multi-phase grand finale
 *
 * Material palette: NETHERITE_BLOCK, CHAIN, IRON_BLOCK, GRAY_CONCRETE,
 *                   POLISHED_BLACKSTONE, CHISELED_POLISHED_BLACKSTONE,
 *                   DARK_OAK_LOG, CRYING_OBSIDIAN, RED_CONCRETE
 * Particles (signature: 12-20/tick across multiple types per attack):
 *   BLOCK(IRON_BLOCK/NETHERITE_BLOCK/CHAIN/POLISHED_BLACKSTONE), CRIT,
 *   SMOKE, LARGE_SMOKE, ELECTRIC_SPARK, SOUL_FIRE_FLAME, EXPLOSION,
 *   LAVA, END_ROD, FLAME, GLOW
 * Sounds: BLOCK_CHAIN_*, BLOCK_ANVIL_*, BLOCK_NETHERITE_BLOCK_*,
 *         ENTITY_IRON_GOLEM_*, BLOCK_GRINDSTONE_USE, BLOCK_PISTON_*,
 *         BLOCK_BEACON_*, ENTITY_WARDEN_*, BLOCK_BELL_RESONATE
 */
public final class ChainBlockDisplay8 {
    private ChainBlockDisplay8() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ChainTsunami(plugin));
        registry.register(new IronGolemFist(plugin));
        registry.register(new ChainWebTrap(plugin));
        registry.register(new IronGiant(plugin));
        registry.register(new IronCathedral(plugin));
    }

    // ================================================================
    // Shared helpers
    // ================================================================

    private static void setScale(BlockDisplay e, float sx, float sy, float sz, int dur) {
        if (e == null || !e.isValid()) return;
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f().set(t.getLeftRotation()),
                new Vector3f(sx, sy, sz),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    private static void rotateOn(BlockDisplay e, float angle, float ax, float ay, float az, int dur) {
        if (e == null || !e.isValid()) return;
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f(angle, ax, ay, az),
                t.getScale(),
                new AxisAngle4f(0, 0, 1, 0)
        ));
    }

    private static void shrinkToZero(BlockDisplayHandle h, int dur) {
        if (h == null) return;
        setScale(h.entity(), 0f, 0f, 0f, dur);
    }

    private static void teleportEntity(BlockDisplayHandle h, Location loc) {
        if (h == null) return;
        try { h.entity().teleport(loc); } catch (Throwable ignored) {}
    }

    /** Find the nearest non-exempt survival player within range (XZ-projected). */
    private static Player findNearestPlayer(Location center, double range) {
        if (center == null || center.getWorld() == null) return null;
        Player nearest = null;
        double bestSq = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dx = p.getLocation().getX() - center.getX();
            double dz = p.getLocation().getZ() - center.getZ();
            double dsq = dx * dx + dz * dz;
            if (dsq < bestSq) { bestSq = dsq; nearest = p; }
        }
        return nearest;
    }

    // ================================================================
    // #71 — CHAIN TSUNAMI ("The Wave")
    // 60 blocks: 12 CHAIN front-wall (3 high × 4 wide front rank),
    //            12 CHAIN second-rank (curving over),
    //            8 IRON_BLOCK curl-crests (rotated forward),
    //            12 CHAIN trailing body (2 high × 6 wide rear rank),
    //            8 GRAY_CONCRETE ground-churn slabs,
    //            8 NETHERITE_BLOCK undertow accents = 60.
    // Wave spawns at one arena edge, sweeps across via teleport, dissipates
    // at the far side. Survive by sprinting perpendicular or getting above.
    // ================================================================
    public static class ChainTsunami extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> frontWall = new ArrayList<>();
        private final List<BlockDisplayHandle> secondRank = new ArrayList<>();
        private final List<BlockDisplayHandle> curlCrests = new ArrayList<>();
        private final List<BlockDisplayHandle> rearRank = new ArrayList<>();
        private final List<BlockDisplayHandle> churn = new ArrayList<>();
        private final List<BlockDisplayHandle> undertow = new ArrayList<>();
        private final List<BlockDisplayHandle> allParts = new ArrayList<>();
        private Location anchor;        // current wave position (front of wave)
        private double advanceZ = 0.0;  // cumulative forward distance
        private double sweepLen = 24.0; // total travel distance
        private int activeStartTick = 30;
        private int dissipateTick = -1;

        public ChainTsunami(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_tsunami", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(380.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(280);
            config.setCooldownTicks(360);
            config.setChance(4);
            config.setEnabled(true);
            config.setDesignType("Tsunami wave (sprint perpendicular or get high ground)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Anchor 12 blocks back from center along -Z so the wave sweeps toward and past the player.
            anchor = center.clone().add(0, 0, -12.0);
            double y0 = 0.6;

            // Front wall: 12 CHAIN displays — 4 wide (X = -3,-1,+1,+3) × 3 high (Y = +0,+1,+2).
            for (int xi = 0; xi < 4; xi++) {
                for (int yi = 0; yi < 3; yi++) {
                    double lx = -3.0 + xi * 2.0;
                    double ly = y0 + yi * 1.1;
                    Location loc = anchor.clone().add(lx, ly, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0f, 0f, 0f).glow(80, 90, 110).interpolation(12, xi + yi);
                    spawnedEntities.add(h.entity());
                    frontWall.add(h); allParts.add(h);
                    setScale(h.entity(), 0.55f, 1.0f, 0.35f, 14);
                }
            }

            // Second rank: 12 CHAIN displays — 4 wide × 3 high, +Z 1.2 offset, sloping back-up to form the curl.
            for (int xi = 0; xi < 4; xi++) {
                for (int yi = 0; yi < 3; yi++) {
                    double lx = -3.0 + xi * 2.0;
                    double ly = y0 + 0.4 + yi * 1.05;
                    Location loc = anchor.clone().add(lx, ly, -1.2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0f, 0f, 0f).glow(60, 70, 90).interpolation(12, 2 + xi + yi);
                    spawnedEntities.add(h.entity());
                    secondRank.add(h); allParts.add(h);
                    setScale(h.entity(), 0.5f, 0.95f, 0.35f, 14);
                }
            }

            // Curl crests: 8 IRON_BLOCK angled slabs at the very top of the wave (curl-over).
            for (int i = 0; i < 8; i++) {
                double lx = -3.5 + (i % 4) * 2.3;
                double ly = y0 + 3.2 + (i / 4) * 0.6;
                double lz = -0.6 - (i / 4) * 1.0;
                Location loc = anchor.clone().add(lx, ly, lz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(170, 180, 200).interpolation(12, 6 + i);
                spawnedEntities.add(h.entity());
                curlCrests.add(h); allParts.add(h);
                setScale(h.entity(), 1.1f, 0.18f, 0.55f, 14);
                rotateOn(h.entity(), (float) Math.toRadians(-45), 1f, 0f, 0f, 14);
            }

            // Rear rank: 12 CHAIN displays — 6 wide (X = -3.5..+3.5) × 2 high, -Z 2.4 (the receding body).
            for (int xi = 0; xi < 6; xi++) {
                for (int yi = 0; yi < 2; yi++) {
                    double lx = -3.5 + xi * 1.4;
                    double ly = y0 + 0.2 + yi * 0.9;
                    Location loc = anchor.clone().add(lx, ly, -2.4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0f, 0f, 0f).glow(50, 60, 80).interpolation(12, 10 + xi + yi);
                    spawnedEntities.add(h.entity());
                    rearRank.add(h); allParts.add(h);
                    setScale(h.entity(), 0.45f, 0.8f, 0.32f, 14);
                }
            }

            // Ground churn: 8 GRAY_CONCRETE flat slabs at base, where wave meets ground.
            for (int i = 0; i < 8; i++) {
                double lx = -3.5 + (i % 4) * 2.3;
                double lz = (i < 4) ? 0.3 : -2.0;
                Location loc = anchor.clone().add(lx, 0.05, lz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GRAY_CONCRETE);
                h.scale(0f, 0f, 0f).glow(110, 110, 120).interpolation(10, 12 + i);
                spawnedEntities.add(h.entity());
                churn.add(h); allParts.add(h);
                setScale(h.entity(), 2.2f, 0.06f, 1.2f, 12);
            }

            // Undertow: 8 NETHERITE_BLOCK dark accents tucked under the curl (depth visual).
            for (int i = 0; i < 8; i++) {
                double lx = -3.0 + (i % 4) * 2.0;
                double ly = y0 + 1.4 + (i / 4) * 0.5;
                double lz = -0.5 - (i / 4) * 0.4;
                Location loc = anchor.clone().add(lx, ly, lz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(40, 40, 50).interpolation(12, 14 + i);
                spawnedEntities.add(h.entity());
                undertow.add(h); allParts.add(h);
                setScale(h.entity(), 0.4f, 0.4f, 0.4f, 14);
            }

            // Spawn fanfare — distant chain rumble + ground rumble + warden thud.
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.6f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 1.6f, 0.55f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.4f, 0.4f);

            w.spawnParticle(Particle.LARGE_SMOKE, anchor.clone().add(0, 2.0, 0), 35, 4.0, 1.5, 0.8, 0.02);
            w.spawnParticle(Particle.ELECTRIC_SPARK, anchor.clone().add(0, 3.0, 0), 30, 3.0, 0.5, 0.5, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || anchor == null) return;
            World w = c.getWorld();

            int dur = config.getDurationTicks();
            int dissipateStart = dur - 30;

            // Build-up phase: anchor stays still, blocks materialise.
            if (tick < activeStartTick) {
                // Telegraph particle column at anchor edge.
                if (tick % 3 == 0) {
                    w.spawnParticle(Particle.LARGE_SMOKE, anchor.clone().add(0, 2.0, 0), 8, 3.5, 1.5, 0.5, 0.02);
                    w.spawnParticle(Particle.BLOCK, anchor.clone().add(0, 0.3, 0), 14, 3.5, 0.1, 1.0, 0.05,
                            Material.GRAY_CONCRETE.createBlockData());
                }
                return;
            }

            // Active phase: advance the wave forward at 0.18 blocks/tick.
            if (tick < dissipateStart) {
                double prevZ = advanceZ;
                advanceZ = Math.min(sweepLen, (tick - activeStartTick) * 0.18);
                double deltaZ = advanceZ - prevZ;

                if (deltaZ > 0.0001) {
                    // Move every part by deltaZ on Z (current wave position = anchor + advanceZ).
                    for (BlockDisplayHandle h : allParts) {
                        try {
                            Location current = h.entity().getLocation();
                            h.entity().teleport(current.add(0, 0, deltaZ));
                        } catch (Throwable ignored) {}
                    }
                }

                // Center follows the wave front for damage radius.
                setCenter(anchor.clone().add(0, 1.2, advanceZ));

                // Crest curl pulses scale ±0.06 — water swell visual.
                if (tick % 4 == 0) {
                    float pulse = (float) (1.0 + Math.sin(tick * 0.18) * 0.06);
                    for (BlockDisplayHandle h : curlCrests) {
                        setScale(h.entity(), 1.1f * pulse, 0.18f * pulse, 0.55f * pulse, 4);
                    }
                }

                // Particles: layered spray + ground churn + sparks (12-20/tick total).
                if (tick % 2 == 0) {
                    Location front = anchor.clone().add(0, 1.8, advanceZ);
                    w.spawnParticle(Particle.BLOCK, front, 8, 3.5, 1.6, 0.4, 0.05,
                            Material.CHAIN.createBlockData());
                    w.spawnParticle(Particle.LARGE_SMOKE, front, 4, 3.0, 1.0, 0.3, 0.02);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, front.clone().add(0, 1.2, 0), 5,
                            3.5, 0.8, 0.3, 0.15);
                    w.spawnParticle(Particle.BLOCK, anchor.clone().add(0, 0.1, advanceZ + 0.3), 6,
                            3.5, 0.05, 0.6, 0.05, Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.CRIT, front, 4, 3.0, 0.8, 0.3, 0.2);
                }

                // Sound loop: thunderous chain fall + ground rumble.
                if (tick % 8 == 0) {
                    Location front = anchor.clone().add(0, 0.5, advanceZ);
                    DisplayBuilder.playSound(front, Sound.BLOCK_CHAIN_FALL, 1.4f, 0.5f);
                    DisplayBuilder.playSound(front, Sound.ENTITY_IRON_GOLEM_STEP, 1.4f, 0.55f);
                }
                if (tick % 24 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.2f, 0.5f);
                }
            }

            // Dissipate phase: wave collapses in cascade.
            if (tick == dissipateStart) {
                dissipateTick = tick;
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.6f, 0.7f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.4f, 0.5f);
                w.spawnParticle(Particle.LARGE_SMOKE, c, 45, 3.5, 1.5, 1.0, 0.05);
                w.spawnParticle(Particle.EXPLOSION, c, 6, 2.5, 0.5, 1.5, 0.0);
            }
            if (dissipateTick > 0) {
                int dt = tick - dissipateTick;
                if (dt == 2) for (BlockDisplayHandle h : curlCrests) shrinkToZero(h, 8);
                if (dt == 6) for (BlockDisplayHandle h : undertow) shrinkToZero(h, 8);
                if (dt == 10) {
                    for (BlockDisplayHandle h : frontWall) shrinkToZero(h, 8);
                    for (BlockDisplayHandle h : secondRank) shrinkToZero(h, 8);
                }
                if (dt == 16) {
                    for (BlockDisplayHandle h : rearRank) shrinkToZero(h, 8);
                    for (BlockDisplayHandle h : churn) shrinkToZero(h, 8);
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.4f);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainTsunami(plugin); }
    }

    // ================================================================
    // #72 — IRON GOLEM FIST ("The Punch")
    // 50 blocks: 1 palm core + 6 palm slabs + 12 finger segments
    //          + 6 thumb segments + 4 wrist + 8 forearm + 10 wrist chain
    //          + 3 arm-pit accents = 50.
    // Impact-only: fist descends from the sky on a 30-tick wind-up, then
    // SLAMS the ground with a 6.0-radius shockwave for 480 damage.
    // ================================================================
    public static class IronGolemFist extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> palmCore = new ArrayList<>();
        private final List<BlockDisplayHandle> palmSlabs = new ArrayList<>();
        private final List<BlockDisplayHandle> fingers = new ArrayList<>();
        private final List<BlockDisplayHandle> thumb = new ArrayList<>();
        private final List<BlockDisplayHandle> wrist = new ArrayList<>();
        private final List<BlockDisplayHandle> forearm = new ArrayList<>();
        private final List<BlockDisplayHandle> wristChain = new ArrayList<>();
        private final List<BlockDisplayHandle> accents = new ArrayList<>();
        private final List<BlockDisplayHandle> allParts = new ArrayList<>();
        private Location impactGround;
        private double fistY = 22.0;          // start high in the sky
        private double velocity = 0.0;
        private boolean impacted = false;
        private int impactTick = -1;
        private static final double TARGET_Y = 1.0;
        private static final double START_Y = 22.0;

        public IronGolemFist(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_golem_fist", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(500.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(320);
            config.setChance(5);
            config.setTracksPlayer(true);
            config.setEnabled(true);
            config.setDesignType("Massive punch (sidestep wide AoE)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            impactGround = center.clone();
            fistY = START_Y;

            // Palm core: 1 large IRON_BLOCK.
            BlockDisplayHandle core = displayBuilder.spawnBlock(center.clone().add(0, fistY, 0), Material.IRON_BLOCK);
            core.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(10, 0);
            spawnedEntities.add(core.entity());
            palmCore.add(core); allParts.add(core);
            setScale(core.entity(), 1.5f, 1.5f, 1.6f, 12);

            // Palm slabs: 6 IRON_BLOCK (knuckle ridge + palm border).
            double[][] palmOff = {
                    {0.0, 0.85, 0.0},  // upper knuckle ridge
                    {0.0, -0.85, 0.0}, // base of palm
                    {0.95, 0.0, 0.0},  // right edge
                    {-0.95, 0.0, 0.0}, // left edge
                    {0.0, 0.0, 0.95},  // front edge
                    {0.0, 0.0, -0.95}  // back edge
            };
            for (double[] o : palmOff) {
                Location loc = center.clone().add(o[0], fistY + o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(10, 2);
                spawnedEntities.add(h.entity());
                palmSlabs.add(h); allParts.add(h);
                setScale(h.entity(), 0.6f, 0.6f, 0.6f, 12);
            }

            // 4 fingers × 3 segments = 12 IRON_BLOCK finger segments curled forward.
            double[] fingerX = { 0.7, 0.23, -0.23, -0.7 };
            for (double fx : fingerX) {
                for (int seg = 0; seg < 3; seg++) {
                    double fy = 0.55 - seg * 0.45;
                    double fz = 0.8 + seg * 0.35;
                    Location loc = center.clone().add(fx, fistY + fy, fz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(190, 190, 210).interpolation(10, 4 + seg);
                    spawnedEntities.add(h.entity());
                    fingers.add(h); allParts.add(h);
                    float s = 0.34f - seg * 0.04f;
                    float sy = 0.48f - seg * 0.06f;
                    setScale(h.entity(), s, sy, s, 12);
                    rotateOn(h.entity(), (float) Math.toRadians(15 + seg * 18), 1f, 0f, 0f, 12);
                }
            }

            // Thumb: 6 segments (2 angled segments per piece — IRON_BLOCK).
            for (int seg = 0; seg < 6; seg++) {
                double tx = 1.0 + (seg % 3) * 0.18;
                double ty = 0.25 - (seg / 3) * 0.4;
                double tz = 0.25 + seg * 0.18;
                Location loc = center.clone().add(tx, fistY + ty, tz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(190, 190, 210).interpolation(10, 5);
                spawnedEntities.add(h.entity());
                thumb.add(h); allParts.add(h);
                setScale(h.entity(), 0.3f, 0.35f, 0.3f, 12);
                rotateOn(h.entity(), (float) Math.toRadians(35), 0f, 0f, 1f, 12);
            }

            // Wrist: 4 IRON_BLOCK chunkier segments above the palm.
            for (int i = 0; i < 4; i++) {
                double wx = (i % 2 == 0) ? -0.5 : 0.5;
                double wy = 1.4 + (i / 2) * 0.45;
                Location loc = center.clone().add(wx, fistY + wy, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(170, 170, 195).interpolation(10, 6);
                spawnedEntities.add(h.entity());
                wrist.add(h); allParts.add(h);
                setScale(h.entity(), 0.7f, 0.55f, 0.65f, 12);
            }

            // Forearm: 8 IRON_BLOCK going up into the sky from wrist.
            for (int i = 0; i < 8; i++) {
                double fy = 2.3 + i * 0.55;
                Location loc = center.clone().add(0, fistY + fy, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(160, 160, 190).interpolation(10, 7 + i);
                spawnedEntities.add(h.entity());
                forearm.add(h); allParts.add(h);
                setScale(h.entity(), 0.55f, 0.5f, 0.55f, 12);
            }

            // Wrist chain: 10 CHAIN trailing behind/above wrist.
            for (int i = 0; i < 10; i++) {
                double cy = 6.8 + i * 0.55;
                Location loc = center.clone().add(0, fistY + cy, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(90, 90, 105).interpolation(10, 10 + i);
                spawnedEntities.add(h.entity());
                wristChain.add(h); allParts.add(h);
                setScale(h.entity(), 0.22f, 0.55f, 0.22f, 12);
            }

            // 3 NETHERITE_BLOCK accents on the knuckles (dark studs).
            for (int i = 0; i < 3; i++) {
                double ax = -0.45 + i * 0.45;
                Location loc = center.clone().add(ax, fistY + 0.85, 0.65);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(50, 50, 60).interpolation(10, 12);
                spawnedEntities.add(h.entity());
                accents.add(h); allParts.add(h);
                setScale(h.entity(), 0.22f, 0.22f, 0.22f, 12);
            }

            // Spawn fanfare: warden roar + iron golem roar + warning chain.
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 1.5f, 0.7f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.6f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.6f);

            // Warning shadow: spawn a flat soul-flame circle on the ground showing impact zone.
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.2, 0), 50,
                    config.getImpactRadius(), 0.05, config.getImpactRadius(), 0.01);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, fistY, 0), 30, 1.5, 1.0, 1.5, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || impactGround == null) return;
            World w = c.getWorld();

            // Wind-up: hover for first 25 ticks, fist visible high in the sky.
            if (tick < 25 && !impacted) {
                if (tick % 3 == 0) {
                    // Telegraph the impact zone on the ground.
                    int n = 24;
                    for (int i = 0; i < n; i++) {
                        double a = (Math.PI * 2.0 * i) / n;
                        double r = config.getImpactRadius();
                        Location ring = impactGround.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, ring, 1, 0, 0, 0, 0);
                    }
                    w.spawnParticle(Particle.LARGE_SMOKE, impactGround.clone().add(0, fistY, 0), 12, 1.5, 0.8, 1.5, 0.03);
                    w.spawnParticle(Particle.CRIT, impactGround.clone().add(0, fistY - 1.0, 0), 10, 1.2, 0.4, 1.2, 0.2);
                }
                if (tick == 12) DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 1.4f, 0.5f);
                if (tick == 20) DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.6f);
                return;
            }

            // Punch phase: accelerate down toward the ground.
            if (!impacted) {
                velocity -= 0.18;
                if (velocity < -3.5) velocity = -3.5;
                fistY = Math.max(TARGET_Y, fistY + velocity);

                // Teleport all parts: their X/Z offset is preserved, Y = relative offset + new fistY.
                // We do this by computing each part's stored offset from the original fist origin.
                // Since they were all spawned at center + (lx, fistY + ly, lz), we move their Y by the delta of fistY.
                // We simply teleport each entity to (curX, curY + dy, curZ).
                double dy = velocity;
                for (BlockDisplayHandle h : allParts) {
                    try {
                        Location current = h.entity().getLocation();
                        h.entity().teleport(current.add(0, dy, 0));
                    } catch (Throwable ignored) {}
                }

                // Trail particles streaking down the wrist column.
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.BLOCK, impactGround.clone().add(0, fistY + 5.0, 0), 14,
                            1.0, 3.0, 1.0, 0.0, Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.LARGE_SMOKE, impactGround.clone().add(0, fistY + 2.5, 0), 8,
                            0.8, 1.5, 0.8, 0.05);
                    w.spawnParticle(Particle.FLAME, impactGround.clone().add(0, fistY, 0), 4, 0.4, 0.4, 0.4, 0.08);
                }

                // Whoosh sound — pitch climbs as speed increases.
                if (tick % 4 == 0) {
                    float pitch = (float) Math.min(1.6, 0.4 + (Math.abs(velocity) / 3.5) * 1.2);
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 1.6f, pitch);
                }

                // Impact!
                if (fistY <= TARGET_Y + 0.05 && !impacted) {
                    impacted = true;
                    impactTick = tick;
                    triggerImpactDamage(impactGround.clone());
                    // Spectacular impact spectacle: layered particles, multiple sounds.
                    w.spawnParticle(Particle.EXPLOSION, impactGround, 12, 3.0, 0.5, 3.0, 0.0);
                    w.spawnParticle(Particle.BLOCK, impactGround, 60, 3.0, 0.5, 3.0, 0.35,
                            Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.BLOCK, impactGround, 40, 3.0, 0.3, 3.0, 0.3,
                            Material.POLISHED_BLACKSTONE.createBlockData());
                    w.spawnParticle(Particle.LARGE_SMOKE, impactGround.clone().add(0, 0.5, 0), 30,
                            2.0, 0.8, 2.0, 0.08);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, impactGround.clone().add(0, 0.5, 0), 35,
                            2.5, 0.4, 2.5, 0.3);
                    w.spawnParticle(Particle.CRIT, impactGround.clone().add(0, 0.4, 0), 40,
                            2.2, 0.3, 2.2, 0.4);
                    DisplayBuilder.playSound(impactGround, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.5f);
                    DisplayBuilder.playSound(impactGround, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.8f, 0.6f);
                    DisplayBuilder.playSound(impactGround, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.7f);
                    DisplayBuilder.playSound(impactGround, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 0.7f);
                    // Expand the palm core briefly on impact.
                    if (!palmCore.isEmpty()) setScale(palmCore.get(0).entity(), 1.9f, 1.9f, 1.95f, 3);
                }
                return;
            }

            // Settle + shockwave aftermath.
            int dt = tick - impactTick;
            if (dt % 3 == 0 && dt < 30) {
                // Expanding shockwave ring (visual only, damage already dealt on impact).
                double r = (dt / 30.0) * config.getImpactRadius();
                int n = 28;
                for (int i = 0; i < n; i++) {
                    double a = (Math.PI * 2.0 * i) / n;
                    Location ring = impactGround.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                    w.spawnParticle(Particle.BLOCK, ring, 1, 0, 0, 0, 0, Material.IRON_BLOCK.createBlockData());
                    if (i % 2 == 0) w.spawnParticle(Particle.LARGE_SMOKE, ring, 1, 0, 0, 0, 0);
                }
            }

            if (dt == 8) {
                for (BlockDisplayHandle h : palmCore) setScale(h.entity(), 1.5f, 1.5f, 1.6f, 6);
            }
            if (dt == 20) {
                for (BlockDisplayHandle h : fingers) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : thumb) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : accents) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1.4f, 0.6f);
            }
            if (dt == 35) {
                for (BlockDisplayHandle h : palmCore) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : palmSlabs) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : wrist) shrinkToZero(h, 12);
            }
            if (dt == 50) {
                for (BlockDisplayHandle h : forearm) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : wristChain) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.4f, 0.6f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronGolemFist(plugin); }
    }

    // ================================================================
    // #73 — CHAIN WEB TRAP ("The Spider Snare")
    // 56 blocks: 8 inner-ring CHAIN + 12 mid-ring CHAIN + 16 outer-ring CHAIN
    //          + 8 radial spokes CHAIN + 8 IRON_BLOCK outer anchor pegs
    //          + 4 NETHERITE_BLOCK central hub stones = 56.
    // Web radiates outward from a central anchor under the player, then
    // TIGHTENS — outer ring shrinks inward, spokes pull taut, anchors lock.
    // Escape window: tick 0-40 (before fully woven).
    // ================================================================
    public static class ChainWebTrap extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> midRing = new ArrayList<>();
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> spokes = new ArrayList<>();
        private final List<BlockDisplayHandle> anchors = new ArrayList<>();
        private final List<BlockDisplayHandle> hubStones = new ArrayList<>();
        private Location webCenter;
        private double currentInnerR = 0.0;
        private double currentMidR = 0.0;
        private double currentOuterR = 0.0;
        private float webYaw = 0f;

        public ChainWebTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_web_trap", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(5.5);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(300);
            config.setCooldownTicks(340);
            config.setChance(4);
            config.setEnabled(true);
            config.setDesignType("Web enclosure (escape before all anchored)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            webCenter = center.clone();
            double y = 0.08;

            // Hub stones: 4 NETHERITE_BLOCK at the very center forming a small dark altar.
            double[][] hubOff = {{0.18, 0, 0.18}, {-0.18, 0, 0.18}, {0.18, 0, -0.18}, {-0.18, 0, -0.18}};
            for (double[] o : hubOff) {
                Location loc = webCenter.clone().add(o[0], y + 0.05, o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(40, 40, 50).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                hubStones.add(h);
                setScale(h.entity(), 0.22f, 0.18f, 0.22f, 12);
            }

            // Inner ring: 8 CHAIN. Begin compressed near center, grow out to r=1.0.
            currentInnerR = 0.3;
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2.0 * i) / 8;
                double x = Math.cos(a) * currentInnerR;
                double z = Math.sin(a) * currentInnerR;
                Location loc = webCenter.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(110, 110, 130).interpolation(8, 2 + i / 2);
                spawnedEntities.add(h.entity());
                innerRing.add(h);
                setScale(h.entity(), 0.22f, 0.05f, 0.22f, 10);
                rotateOn(h.entity(), (float) a, 0f, 1f, 0f, 10);
            }

            // Mid ring: 12 CHAIN.
            currentMidR = 0.5;
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12;
                double x = Math.cos(a) * currentMidR;
                double z = Math.sin(a) * currentMidR;
                Location loc = webCenter.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(100, 100, 120).interpolation(8, 6 + i / 3);
                spawnedEntities.add(h.entity());
                midRing.add(h);
                setScale(h.entity(), 0.22f, 0.05f, 0.22f, 10);
                rotateOn(h.entity(), (float) a, 0f, 1f, 0f, 10);
            }

            // Outer ring: 16 CHAIN.
            currentOuterR = 0.7;
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2.0 * i) / 16;
                double x = Math.cos(a) * currentOuterR;
                double z = Math.sin(a) * currentOuterR;
                Location loc = webCenter.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(90, 90, 110).interpolation(8, 12 + i / 4);
                spawnedEntities.add(h.entity());
                outerRing.add(h);
                setScale(h.entity(), 0.22f, 0.05f, 0.22f, 10);
                rotateOn(h.entity(), (float) a, 0f, 1f, 0f, 10);
            }

            // 8 spoke chains radiating from hub outward (initially compressed near hub).
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2.0 * i) / 8;
                double x = Math.cos(a) * 0.5;
                double z = Math.sin(a) * 0.5;
                Location loc = webCenter.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(120, 120, 140).interpolation(8, 18 + i);
                spawnedEntities.add(h.entity());
                spokes.add(h);
                setScale(h.entity(), 0.1f, 0.04f, 0.6f, 10);
                rotateOn(h.entity(), (float) a + (float) (Math.PI / 2.0), 0f, 1f, 0f, 10);
            }

            // 8 IRON_BLOCK outer anchor pegs (slightly above ground — pegs).
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2.0 * i) / 8 + Math.PI / 16.0; // offset from spoke nodes
                double x = Math.cos(a) * 0.8;
                double z = Math.sin(a) * 0.8;
                Location loc = webCenter.clone().add(x, y + 0.12, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 230).interpolation(8, 24 + i);
                spawnedEntities.add(h.entity());
                anchors.add(h);
                setScale(h.entity(), 0.22f, 0.35f, 0.22f, 10);
            }

            // Spawn fanfare — escalating chain weave.
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.9f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.4f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.5f);

            w.spawnParticle(Particle.SOUL_FIRE_FLAME, webCenter.clone().add(0, 0.2, 0), 35,
                    1.0, 0.05, 1.0, 0.02);
            w.spawnParticle(Particle.ELECTRIC_SPARK, webCenter.clone().add(0, 0.4, 0), 25,
                    0.5, 0.2, 0.5, 0.15);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || webCenter == null) return;
            World w = c.getWorld();

            int dur = config.getDurationTicks();
            int weaveEnd = 70;       // web fully woven by tick 70
            int tightenStart = 110;  // web begins to constrict
            int tightenEnd = 180;    // web fully tight
            int dissipateStart = dur - 30;

            // Phase 1 (0-70): web weaves outward.
            if (tick <= weaveEnd) {
                double p = Math.min(1.0, tick / (double) weaveEnd);
                currentInnerR = 0.3 + p * 0.7;   // → 1.0
                currentMidR = 0.5 + p * 1.8;     // → 2.3
                currentOuterR = 0.7 + p * 4.2;   // → 4.9
                updateRingPositions(tick);
            }

            // Phase 2 (70-110): held at full extent, rotating slowly + breathing.
            if (tick > weaveEnd && tick <= tightenStart) {
                webYaw += (float) Math.toRadians(1);
                updateRingPositions(tick);
            }

            // Phase 3 (110-180): constrict — outer ring contracts inward, escape closes.
            if (tick > tightenStart && tick <= tightenEnd) {
                double p = (tick - tightenStart) / (double) (tightenEnd - tightenStart);
                double targetOuter = 4.9 - p * 2.4; // → 2.5
                double targetMid = 2.3 - p * 0.8;   // → 1.5
                currentOuterR = targetOuter;
                currentMidR = targetMid;
                webYaw += (float) Math.toRadians(2.5);
                updateRingPositions(tick);
                if (tick % 8 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.0f, 0.7f + (float) p * 0.5f);
                }
            }

            // Phase 4 (180-dissipateStart): held tight, anchors bob.
            if (tick > tightenEnd && tick < dissipateStart) {
                webYaw += (float) Math.toRadians(1.5);
                updateRingPositions(tick);
                // Anchor pulse: nearest 3 anchors to player vibrate scale ±0.08.
                if (tick % 3 == 0) {
                    Player p = findNearestPlayer(webCenter, 12.0);
                    if (p != null) {
                        Location pl = p.getLocation();
                        for (BlockDisplayHandle h : anchors) {
                            Location al = h.entity().getLocation();
                            double dx = al.getX() - pl.getX();
                            double dz = al.getZ() - pl.getZ();
                            double d2 = dx * dx + dz * dz;
                            if (d2 < 2.2 * 2.2) {
                                float pulse = (float) (1.0 + Math.sin(tick * 0.5) * 0.08);
                                setScale(h.entity(), 0.22f * pulse, 0.35f * pulse, 0.22f * pulse, 3);
                            }
                        }
                    }
                }
            }

            // Particles: ELECTRIC_SPARK at anchor pegs + chain DUST trails along rings (12-16/tick).
            if (tick > 30 && tick % 3 == 0) {
                for (BlockDisplayHandle h : anchors) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, h.entity().getLocation(), 2, 0.2, 0.2, 0.2, 0.1);
                }
            }
            if (tick > 30 && tick % 5 == 0) {
                int n = 12;
                for (int i = 0; i < n; i++) {
                    double a = (Math.PI * 2.0 * i) / n + webYaw;
                    double r = currentOuterR;
                    Location p = webCenter.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r);
                    w.spawnParticle(Particle.BLOCK, p, 1, 0, 0, 0, 0, Material.CHAIN.createBlockData());
                    if (i % 3 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            // Central hub spark every 10t.
            if (tick > 30 && tick % 10 == 0) {
                w.spawnParticle(Particle.GLOW, webCenter.clone().add(0, 0.4, 0), 8, 0.3, 0.2, 0.3, 0.05);
            }
            // Step sound looping during weaving.
            if (tick > 30 && tick % 14 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.9f, 0.9f);
            }
            // Loop chain place sound as new rings form.
            if (tick == 10) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.7f);
            if (tick == 30) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.9f);
            if (tick == 50) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 1.1f);
            if (tick == 110) {
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.6f, 0.6f);
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.3f, 0.7f);
            }
            if (tick == tightenEnd) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.6f);
                w.spawnParticle(Particle.EXPLOSION, webCenter, 4, 1.5, 0.2, 1.5, 0.0);
            }

            // Dissipate: web shatters from center outward.
            if (tick == dissipateStart) {
                for (BlockDisplayHandle h : hubStones) shrinkToZero(h, 6);
                for (BlockDisplayHandle h : innerRing) shrinkToZero(h, 8);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.5f, 0.5f);
            }
            if (tick == dissipateStart + 8) {
                for (BlockDisplayHandle h : midRing) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : spokes) shrinkToZero(h, 8);
            }
            if (tick == dissipateStart + 16) {
                for (BlockDisplayHandle h : outerRing) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : anchors) shrinkToZero(h, 8);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.7f);
            }
        }

        private void updateRingPositions(int tick) {
            double y = 0.08;
            for (int i = 0; i < innerRing.size(); i++) {
                double a = (Math.PI * 2.0 * i) / innerRing.size() + webYaw;
                double x = Math.cos(a) * currentInnerR;
                double z = Math.sin(a) * currentInnerR;
                teleportEntity(innerRing.get(i), webCenter.clone().add(x, y, z));
            }
            for (int i = 0; i < midRing.size(); i++) {
                double a = (Math.PI * 2.0 * i) / midRing.size() + webYaw;
                double x = Math.cos(a) * currentMidR;
                double z = Math.sin(a) * currentMidR;
                teleportEntity(midRing.get(i), webCenter.clone().add(x, y, z));
            }
            for (int i = 0; i < outerRing.size(); i++) {
                double a = (Math.PI * 2.0 * i) / outerRing.size() + webYaw;
                double x = Math.cos(a) * currentOuterR;
                double z = Math.sin(a) * currentOuterR;
                teleportEntity(outerRing.get(i), webCenter.clone().add(x, y, z));
            }
            // Spokes scale lengthwise to span from inner to outer (midpoint located at midR).
            for (int i = 0; i < spokes.size(); i++) {
                double a = (Math.PI * 2.0 * i) / spokes.size() + webYaw;
                double midR = (currentInnerR + currentOuterR) * 0.5;
                double x = Math.cos(a) * midR;
                double z = Math.sin(a) * midR;
                teleportEntity(spokes.get(i), webCenter.clone().add(x, y, z));
                float spanLen = (float) Math.max(0.2, (currentOuterR - currentInnerR));
                setScale(spokes.get(i).entity(), 0.1f, 0.04f, spanLen, 4);
            }
            // Anchors at outer ring nodes (offset between spokes), bob Y ±0.04.
            for (int i = 0; i < anchors.size(); i++) {
                double a = (Math.PI * 2.0 * i) / anchors.size() + Math.PI / 16.0 + webYaw;
                double x = Math.cos(a) * (currentOuterR + 0.05);
                double z = Math.sin(a) * (currentOuterR + 0.05);
                double yBob = y + 0.12 + Math.sin((tick + i * 4) * 0.2) * 0.04;
                teleportEntity(anchors.get(i), webCenter.clone().add(x, yBob, z));
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainWebTrap(plugin); }
    }

    // ================================================================
    // #74 — THE IRON GIANT ("The Colossus")
    // 52 blocks: 1 head IRON + 2 NETHERITE eye accents + 5 torso IRON
    //          + 2 × 4 arms (8) + 2 × 4 legs (8) + 4 CHAIN shoulder pads
    //          + 4 belt CHAIN + 4 chest IRON plates + 6 CHAIN trailing
    //          + 4 IRON boot/foot + 4 helmet ridge IRON = 52.
    // Lumbers via follow-AI 0.06. Each footstep triggers impactDamage in a
    // 4.0 radius at the foot position.
    // ================================================================
    public static class IronGiant extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final List<BlockDisplayHandle> torso = new ArrayList<>();
        private final List<BlockDisplayHandle> chestPlates = new ArrayList<>();
        private final List<BlockDisplayHandle> leftArm = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArm = new ArrayList<>();
        private final List<BlockDisplayHandle> leftLeg = new ArrayList<>();
        private final List<BlockDisplayHandle> rightLeg = new ArrayList<>();
        private final List<BlockDisplayHandle> shoulderPads = new ArrayList<>();
        private final List<BlockDisplayHandle> belt = new ArrayList<>();
        private final List<BlockDisplayHandle> trailingChains = new ArrayList<>();
        private final List<BlockDisplayHandle> feet = new ArrayList<>();
        private final List<BlockDisplayHandle> helmetRidge = new ArrayList<>();
        private float armSwing = 0f;
        private int lastStepTick = -100;
        private boolean nextStepLeft = true;

        public IronGiant(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_giant", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(420.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(360);
            config.setCooldownTicks(380);
            config.setChance(3);
            config.setEnabled(true);
            // Stomps also do impact damage on each footfall.
            config.setImpactDamage(480.0);
            config.setImpactRadius(4.0);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.06);
            config.setDesignType("Colossus stomping (read leg location)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Body anchor: feet at Y=0, head ~5.5 high.
            double y = 0.0;

            // Helmet ridge (4 small IRON_BLOCK above head).
            double[][] ridgeOff = {{-0.3, 5.95, 0}, {0.3, 5.95, 0}, {0, 5.95, 0.3}, {0, 5.95, -0.3}};
            for (double[] o : ridgeOff) {
                Location loc = center.clone().add(o[0], y + o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(190, 190, 215).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                helmetRidge.add(h);
                setScale(h.entity(), 0.22f, 0.18f, 0.22f, 12);
            }

            // Head: 1 IRON_BLOCK.
            BlockDisplayHandle hh = displayBuilder.spawnBlock(center.clone().add(0, y + 5.4, 0), Material.IRON_BLOCK);
            hh.scale(0f, 0f, 0f).glow(200, 200, 220).interpolation(10, 1);
            spawnedEntities.add(hh.entity()); head.add(hh);
            setScale(hh.entity(), 0.85f, 0.85f, 0.85f, 12);

            // 2 NETHERITE eye accents.
            BlockDisplayHandle eL = displayBuilder.spawnBlock(center.clone().add(-0.22, y + 5.5, 0.43), Material.NETHERITE_BLOCK);
            eL.scale(0f, 0f, 0f).glow(60, 60, 80).interpolation(10, 2);
            spawnedEntities.add(eL.entity()); eyes.add(eL);
            setScale(eL.entity(), 0.18f, 0.15f, 0.08f, 12);
            BlockDisplayHandle eR = displayBuilder.spawnBlock(center.clone().add(0.22, y + 5.5, 0.43), Material.NETHERITE_BLOCK);
            eR.scale(0f, 0f, 0f).glow(60, 60, 80).interpolation(10, 2);
            spawnedEntities.add(eR.entity()); eyes.add(eR);
            setScale(eR.entity(), 0.18f, 0.15f, 0.08f, 12);

            // Torso: 5 IRON_BLOCK (chest core + 4 ribs).
            double[][] torsoOff = {
                    {0, 4.3, 0},     // chest core
                    {-0.45, 4.3, 0}, // left rib
                    {0.45, 4.3, 0},  // right rib
                    {0, 3.6, 0},     // belly
                    {0, 4.8, 0}      // upper chest
            };
            for (int i = 0; i < torsoOff.length; i++) {
                double[] o = torsoOff[i];
                Location loc = center.clone().add(o[0], y + o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(10, 3 + i);
                spawnedEntities.add(h.entity()); torso.add(h);
                float sx = (i == 0 || i == 4) ? 0.95f : 0.55f;
                float sy = (i == 4) ? 0.5f : 0.7f;
                float sz = 0.6f;
                setScale(h.entity(), sx, sy, sz, 12);
            }

            // 4 chest plate IRON_BLOCK (front accents).
            double[][] plateOff = {{-0.35, 4.2, 0.35}, {0.35, 4.2, 0.35}, {-0.35, 4.7, 0.35}, {0.35, 4.7, 0.35}};
            for (double[] o : plateOff) {
                Location loc = center.clone().add(o[0], y + o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 220, 235).interpolation(10, 6);
                spawnedEntities.add(h.entity()); chestPlates.add(h);
                setScale(h.entity(), 0.35f, 0.32f, 0.1f, 12);
            }

            // Left arm: 4 IRON_BLOCK segments (shoulder→hand).
            for (int i = 0; i < 4; i++) {
                double ay = y + 4.85 - i * 0.6;
                double ax = -0.85;
                Location loc = center.clone().add(ax, ay, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(10, 7 + i);
                spawnedEntities.add(h.entity()); leftArm.add(h);
                float s = 0.32f - i * 0.02f;
                float sy = 0.55f - i * 0.05f;
                setScale(h.entity(), s, sy, s, 12);
            }
            // Right arm.
            for (int i = 0; i < 4; i++) {
                double ay = y + 4.85 - i * 0.6;
                double ax = 0.85;
                Location loc = center.clone().add(ax, ay, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(180, 180, 200).interpolation(10, 7 + i);
                spawnedEntities.add(h.entity()); rightArm.add(h);
                float s = 0.32f - i * 0.02f;
                float sy = 0.55f - i * 0.05f;
                setScale(h.entity(), s, sy, s, 12);
            }

            // Left leg: 4 IRON_BLOCK (hip→ankle).
            for (int i = 0; i < 4; i++) {
                double ly = y + 3.0 - i * 0.7;
                double lx = -0.35;
                Location loc = center.clone().add(lx, ly, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(170, 170, 190).interpolation(10, 12 + i);
                spawnedEntities.add(h.entity()); leftLeg.add(h);
                float s = 0.36f - i * 0.025f;
                float sy = 0.62f - i * 0.05f;
                setScale(h.entity(), s, sy, s, 12);
            }
            // Right leg.
            for (int i = 0; i < 4; i++) {
                double ly = y + 3.0 - i * 0.7;
                double lx = 0.35;
                Location loc = center.clone().add(lx, ly, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(170, 170, 190).interpolation(10, 12 + i);
                spawnedEntities.add(h.entity()); rightLeg.add(h);
                float s = 0.36f - i * 0.025f;
                float sy = 0.62f - i * 0.05f;
                setScale(h.entity(), s, sy, s, 12);
            }

            // 4 CHAIN shoulder pads (drape over shoulders).
            double[][] padOff = {{-0.85, 4.65, 0.0}, {0.85, 4.65, 0.0}, {-0.65, 4.6, 0.25}, {0.65, 4.6, 0.25}};
            for (double[] o : padOff) {
                Location loc = center.clone().add(o[0], y + o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(90, 90, 110).interpolation(10, 16);
                spawnedEntities.add(h.entity()); shoulderPads.add(h);
                setScale(h.entity(), 0.4f, 0.25f, 0.32f, 12);
            }

            // 4 belt CHAIN around waist.
            double[][] beltOff = {{-0.4, 3.4, 0}, {0.4, 3.4, 0}, {0, 3.4, 0.4}, {0, 3.4, -0.4}};
            for (double[] o : beltOff) {
                Location loc = center.clone().add(o[0], y + o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(100, 100, 120).interpolation(10, 17);
                spawnedEntities.add(h.entity()); belt.add(h);
                setScale(h.entity(), 0.22f, 0.18f, 0.22f, 12);
            }

            // 6 trailing CHAIN behind giant (cape effect).
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, y + 4.0 - i * 0.55, -0.5 - i * 0.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(70, 70, 90).interpolation(8, 18 + i);
                spawnedEntities.add(h.entity()); trailingChains.add(h);
                setScale(h.entity(), 0.2f, 0.5f, 0.18f, 12);
            }

            // 4 boots/feet: 2 IRON_BLOCK per foot for chunky boots.
            double[][] footOff = {{-0.35, 0.25, 0.05}, {-0.35, 0.25, -0.05}, {0.35, 0.25, 0.05}, {0.35, 0.25, -0.05}};
            for (double[] o : footOff) {
                Location loc = center.clone().add(o[0], y + o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(160, 160, 185).interpolation(10, 22);
                spawnedEntities.add(h.entity()); feet.add(h);
                setScale(h.entity(), 0.45f, 0.32f, 0.5f, 12);
            }

            // Spawn fanfare — heavy thuds + roar.
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.6f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 1.4f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.6f);

            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 2.0, 0), 35, 1.2, 2.0, 1.2, 0.03);
            w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 4.5, 0), 25, 0.8, 0.8, 0.8, 0.15);
        }

        @Override
        protected void onTick(int tick) {
            // Follow-AI moves the center toward nearest player at 0.06/tick.
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Arm + leg swing (walking cycle: 60-tick period).
            armSwing = (float) (Math.sin(tick * 0.105) * Math.toRadians(25));
            float legSwing = (float) (Math.sin(tick * 0.105 + Math.PI) * Math.toRadians(15));

            if (tick > 20) {
                // Update arm rotations (X-axis swing around shoulder).
                for (BlockDisplayHandle h : leftArm) rotateOn(h.entity(), armSwing, 1f, 0f, 0f, 3);
                for (BlockDisplayHandle h : rightArm) rotateOn(h.entity(), -armSwing, 1f, 0f, 0f, 3);
                for (BlockDisplayHandle h : leftLeg) rotateOn(h.entity(), legSwing, 1f, 0f, 0f, 3);
                for (BlockDisplayHandle h : rightLeg) rotateOn(h.entity(), -legSwing, 1f, 0f, 0f, 3);

                // Footstep detection: at max +legSwing or min -legSwing.
                boolean stepNow = false;
                if (tick - lastStepTick > 25) {
                    if (legSwing > Math.toRadians(13) && !nextStepLeft) {
                        // right foot plants
                        stepNow = true;
                        nextStepLeft = true;
                    } else if (legSwing < -Math.toRadians(13) && nextStepLeft) {
                        // left foot plants
                        stepNow = true;
                        nextStepLeft = false;
                    }
                }
                if (stepNow) {
                    lastStepTick = tick;
                    // Foot position is c (the giant's base) ± 0.35 on X depending on which leg planted.
                    double footX = nextStepLeft ? 0.35 : -0.35; // the foot that JUST planted is the opposite of next
                    Location footLoc = c.clone().add(footX, 0.05, 0);
                    triggerImpactDamage(footLoc);
                    // Massive thud particles.
                    w.spawnParticle(Particle.EXPLOSION, footLoc, 4, 1.0, 0.1, 1.0, 0.0);
                    w.spawnParticle(Particle.BLOCK, footLoc, 50, 2.0, 0.4, 2.0, 0.3,
                            Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.LARGE_SMOKE, footLoc, 20, 1.5, 0.4, 1.5, 0.06);
                    w.spawnParticle(Particle.CRIT, footLoc, 22, 1.6, 0.3, 1.6, 0.3);
                    DisplayBuilder.playSound(footLoc, Sound.ENTITY_IRON_GOLEM_STEP, 1.8f, 0.5f);
                    DisplayBuilder.playSound(footLoc, Sound.ENTITY_WARDEN_STEP, 1.6f, 0.6f);
                    DisplayBuilder.playSound(footLoc, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.4f);
                }
            }

            // Combat ambience.
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.7f, 0.6f);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.55f);
            }

            // Constant atmospheric particles around the giant (12-18/tick total).
            if (tick % 2 == 0) {
                // Shoulder smoke.
                w.spawnParticle(Particle.SMOKE, c.clone().add(-0.85, 4.7, 0), 2, 0.2, 0.2, 0.2, 0.02);
                w.spawnParticle(Particle.SMOKE, c.clone().add(0.85, 4.7, 0), 2, 0.2, 0.2, 0.2, 0.02);
                // Eye glow.
                w.spawnParticle(Particle.GLOW, c.clone().add(-0.22, 5.5, 0.45), 1, 0.05, 0.05, 0.05, 0);
                w.spawnParticle(Particle.GLOW, c.clone().add(0.22, 5.5, 0.45), 1, 0.05, 0.05, 0.05, 0);
                // Belt sparks.
                w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 3.4, 0), 2, 0.5, 0.1, 0.5, 0.1);
                // Trailing dust.
                w.spawnParticle(Particle.BLOCK, c.clone().add(0, 0.4, 0), 4, 0.6, 0.1, 0.6, 0.02,
                        Material.GRAY_CONCRETE.createBlockData());
            }

            // Dissipate: giant falls apart from feet up.
            int dur = config.getDurationTicks();
            if (tick == dur - 40) {
                for (BlockDisplayHandle h : feet) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : leftLeg) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : rightLeg) shrinkToZero(h, 12);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DEATH, 1.8f, 0.5f);
                w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 1.0, 0), 6, 1.5, 0.4, 1.5, 0.0);
            }
            if (tick == dur - 25) {
                for (BlockDisplayHandle h : belt) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : trailingChains) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : leftArm) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : rightArm) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : shoulderPads) shrinkToZero(h, 10);
            }
            if (tick == dur - 12) {
                for (BlockDisplayHandle h : torso) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : chestPlates) shrinkToZero(h, 8);
            }
            if (tick == dur - 5) {
                for (BlockDisplayHandle h : head) shrinkToZero(h, 4);
                for (BlockDisplayHandle h : eyes) shrinkToZero(h, 4);
                for (BlockDisplayHandle h : helmetRidge) shrinkToZero(h, 4);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronGiant(plugin); }
    }

    // ================================================================
    // #75 — THE IRON CATHEDRAL ("The Grand Finale")
    // 70 blocks: 4 corner pillars × 4 segments (16 POLISHED_BLACKSTONE)
    //          + 6 arch ribs × 2 arches (12 IRON_BLOCK)
    //          + 16 curtain CHAIN (4 per wall)
    //          + 1 NETHERITE chandelier core + 4 chandelier poles
    //          + 8 chandelier dangle CHAIN
    //          + 4 ground anchor CHAIN (one per pillar base)
    //          + 4 buttress IRON_BLOCK (corner braces)
    //          + 1 CHISELED_POLISHED_BLACKSTONE central altar
    //          + 4 altar candle SOUL_FIRE accents (CRYING_OBSIDIAN blocks)
    //          = 70.
    // Multi-phase:
    //   Tick 0-30: pillars rise from ground.
    //   Tick 30-60: arch ribs connect across.
    //   Tick 60-90: chain curtains unroll.
    //   Tick 90-110: chandelier descends.
    //   Tick 110+: cathedral active, chandelier sways, altar pulses.
    //   Last 40 ticks: grand collapse outward.
    // ================================================================
    public static class IronCathedral extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pillars = new ArrayList<>();      // 16
        private final List<BlockDisplayHandle> archRibs = new ArrayList<>();    // 12
        private final List<BlockDisplayHandle> curtains = new ArrayList<>();    // 16
        private BlockDisplayHandle chandelierCore;
        private final List<BlockDisplayHandle> chandelierPoles = new ArrayList<>(); // 4
        private final List<BlockDisplayHandle> chandelierChains = new ArrayList<>(); // 8
        private final List<BlockDisplayHandle> groundAnchors = new ArrayList<>(); // 4
        private final List<BlockDisplayHandle> buttresses = new ArrayList<>();    // 4
        private BlockDisplayHandle altar;
        private final List<BlockDisplayHandle> candles = new ArrayList<>();       // 4
        private Location cathedralCenter;
        private float chandelierSway = 0f;
        private float chandelierY = 6.5f;

        public IronCathedral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_cathedral", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(360.0);
            config.setDamageRadius(6.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(35);
            config.setDurationTicks(400);
            config.setCooldownTicks(420);
            config.setChance(3);
            config.setEnabled(true);
            config.setDesignType("Grand finale (multi-phase, read each phase)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            cathedralCenter = center.clone();

            // 4 corner pillars. Each pillar has 4 stacked POLISHED_BLACKSTONE segments.
            double[][] pillarBase = {{-2.5, 0, -2.5}, {2.5, 0, -2.5}, {-2.5, 0, 2.5}, {2.5, 0, 2.5}};
            for (int p = 0; p < 4; p++) {
                for (int seg = 0; seg < 4; seg++) {
                    double py = pillarBase[p][1] + seg * 1.05;
                    Location loc = cathedralCenter.clone().add(pillarBase[p][0], py, pillarBase[p][2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    // Start shrunk underground — pillars rise.
                    h.scale(0f, 0f, 0f).glow(45, 45, 55).interpolation(0, 0);
                    spawnedEntities.add(h.entity());
                    pillars.add(h);
                    // Pillar rises in phase 1 (delay = seg * 2 ticks).
                    h.entity().setInterpolationDelay(p * 2 + seg * 3);
                    setScale(h.entity(), 0.32f, 1.05f, 0.32f, 20);
                }
            }

            // 2 arch ribs (one per pair of opposite pillars). Each rib = 6 IRON_BLOCK in an arc.
            // Arch A: spans -X pillar pair (front-back along Z).
            // Arch B: spans +X pillar pair.
            for (int arch = 0; arch < 2; arch++) {
                double archX = (arch == 0) ? -2.5 : 2.5;
                for (int seg = 0; seg < 6; seg++) {
                    // Position from z=-2.5 to z=+2.5 along the arch, arcing up over the top.
                    double t = seg / 5.0;
                    double zPos = -2.5 + t * 5.0;
                    double yPos = 4.4 + Math.sin(t * Math.PI) * 1.3; // peak +1.3 at center
                    Location loc = cathedralCenter.clone().add(archX, yPos, zPos);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(190, 190, 215).interpolation(0, 0);
                    spawnedEntities.add(h.entity());
                    archRibs.add(h);
                    // Delay arch spawn to tick ~30.
                    h.entity().setInterpolationDelay(30 + arch * 4 + seg * 3);
                    setScale(h.entity(), 0.65f, 0.5f, 0.65f, 20);
                }
            }

            // 16 chain curtains: 4 per wall, hanging from arch height down to ground.
            // Walls: -X, +X, -Z, +Z. Each wall: 4 curtains at evenly spaced positions.
            double[][] wallNormals = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}};
            for (int wall = 0; wall < 4; wall++) {
                for (int curtainIdx = 0; curtainIdx < 4; curtainIdx++) {
                    double along = -1.5 + curtainIdx * 1.0;
                    // Determine X/Z based on which wall.
                    double cx = (wallNormals[wall][0] != 0) ? 2.5 * wallNormals[wall][0] : along;
                    double cz = (wallNormals[wall][2] != 0) ? 2.5 * wallNormals[wall][2] : along;
                    Location loc = cathedralCenter.clone().add(cx, 2.4, cz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0f, 0f, 0f).glow(85, 85, 105).interpolation(0, 0);
                    spawnedEntities.add(h.entity());
                    curtains.add(h);
                    // Delay curtain unfurl to tick ~60.
                    h.entity().setInterpolationDelay(60 + wall * 2 + curtainIdx * 3);
                    setScale(h.entity(), 0.22f, 2.4f, 0.1f, 25);
                }
            }

            // Chandelier core: 1 NETHERITE_BLOCK at ceiling.
            chandelierCore = displayBuilder.spawnBlock(cathedralCenter.clone().add(0, 6.5, 0), Material.NETHERITE_BLOCK);
            chandelierCore.scale(0f, 0f, 0f).glow(60, 60, 75).interpolation(0, 0);
            spawnedEntities.add(chandelierCore.entity());
            chandelierCore.entity().setInterpolationDelay(90);
            setScale(chandelierCore.entity(), 0.65f, 0.65f, 0.65f, 18);

            // 4 chandelier poles (NETHERITE around the core).
            double[][] polesOff = {{0.5, 6.5, 0}, {-0.5, 6.5, 0}, {0, 6.5, 0.5}, {0, 6.5, -0.5}};
            for (double[] o : polesOff) {
                Location loc = cathedralCenter.clone().add(o[0], o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(50, 50, 65).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                chandelierPoles.add(h);
                h.entity().setInterpolationDelay(92);
                setScale(h.entity(), 0.3f, 0.3f, 0.3f, 16);
            }

            // 8 chandelier draping chains.
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2.0 * i) / 8;
                double cx = Math.cos(a) * 0.8;
                double cz = Math.sin(a) * 0.8;
                Location loc = cathedralCenter.clone().add(cx, 6.0, cz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(120, 120, 140).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                chandelierChains.add(h);
                h.entity().setInterpolationDelay(95 + i);
                setScale(h.entity(), 0.18f, 0.7f, 0.18f, 16);
            }

            // 4 ground anchor chains (one at each pillar base).
            for (int p = 0; p < 4; p++) {
                Location loc = cathedralCenter.clone().add(pillarBase[p][0] * 0.85, 0.4, pillarBase[p][2] * 0.85);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(95, 95, 115).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                groundAnchors.add(h);
                h.entity().setInterpolationDelay(20 + p * 4);
                setScale(h.entity(), 0.28f, 0.8f, 0.28f, 16);
            }

            // 4 IRON_BLOCK buttresses at corners (angled supports).
            for (int p = 0; p < 4; p++) {
                Location loc = cathedralCenter.clone().add(pillarBase[p][0] * 0.7, 1.8, pillarBase[p][2] * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(165, 165, 190).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                buttresses.add(h);
                h.entity().setInterpolationDelay(15 + p * 5);
                setScale(h.entity(), 0.4f, 1.4f, 0.4f, 18);
                // Angle buttress toward the pillar.
                rotateOn(h.entity(), (float) Math.toRadians(15), 1f, 0f, 1f, 18);
            }

            // Central altar: 1 CHISELED_POLISHED_BLACKSTONE.
            altar = displayBuilder.spawnBlock(cathedralCenter.clone().add(0, 0.6, 0), Material.CHISELED_POLISHED_BLACKSTONE);
            altar.scale(0f, 0f, 0f).glow(80, 80, 95).interpolation(0, 0);
            spawnedEntities.add(altar.entity());
            altar.entity().setInterpolationDelay(40);
            setScale(altar.entity(), 1.1f, 0.7f, 1.1f, 20);

            // 4 CRYING_OBSIDIAN candles around the altar.
            double[][] candleOff = {{0.6, 1.1, 0.6}, {-0.6, 1.1, 0.6}, {0.6, 1.1, -0.6}, {-0.6, 1.1, -0.6}};
            for (double[] o : candleOff) {
                Location loc = cathedralCenter.clone().add(o[0], o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0f, 0f, 0f).glow(55, 30, 70).interpolation(0, 0);
                spawnedEntities.add(h.entity());
                candles.add(h);
                h.entity().setInterpolationDelay(45);
                setScale(h.entity(), 0.22f, 0.35f, 0.22f, 18);
            }

            // GRAND FANFARE — escalating chain place sequence + bell + warden roar.
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.6f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.6f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.6f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 1.4f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BELL_RESONATE, 1.6f, 0.6f);

            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 1.0, 0), 50, 3.0, 0.5, 3.0, 0.05);
            w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 4.0, 0), 50, 3.0, 2.0, 3.0, 0.15);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.5, 0), 30,
                    2.5, 0.1, 2.5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || cathedralCenter == null) return;
            World w = c.getWorld();

            int dur = config.getDurationTicks();
            int dissipateStart = dur - 40;

            // PHASE 1 (0-30): Pillars rise. SOUL_FIRE rising from each pillar base.
            if (tick <= 30) {
                if (tick % 3 == 0) {
                    double[][] pillarBase = {{-2.5, 0, -2.5}, {2.5, 0, -2.5}, {-2.5, 0, 2.5}, {2.5, 0, 2.5}};
                    for (double[] o : pillarBase) {
                        Location pl = cathedralCenter.clone().add(o[0], 0.2, o[2]);
                        w.spawnParticle(Particle.LARGE_SMOKE, pl, 4, 0.3, 0.5, 0.3, 0.03);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, pl, 3, 0.3, 0.2, 0.3, 0.05);
                        w.spawnParticle(Particle.BLOCK, pl, 4, 0.4, 0.1, 0.4, 0.05,
                                Material.POLISHED_BLACKSTONE.createBlockData());
                    }
                }
                if (tick == 10) DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_EXTEND, 1.5f, 0.5f);
                if (tick == 20) DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_EXTEND, 1.5f, 0.7f);
            }

            // PHASE 2 (30-60): Arches connect.
            if (tick == 30) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 1.5f, 0.6f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.4f, 0.7f);
            }
            if (tick > 30 && tick <= 60 && tick % 4 == 0) {
                // Trail particles along the rising arches.
                for (int arch = 0; arch < 2; arch++) {
                    double archX = (arch == 0) ? -2.5 : 2.5;
                    double tProg = (tick - 30) / 30.0;
                    double zPos = -2.5 + tProg * 5.0;
                    double yPos = 4.4 + Math.sin(tProg * Math.PI) * 1.3;
                    Location arc = cathedralCenter.clone().add(archX, yPos, zPos);
                    w.spawnParticle(Particle.BLOCK, arc, 6, 0.3, 0.3, 0.3, 0.05,
                            Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.CRIT, arc, 4, 0.2, 0.2, 0.2, 0.15);
                }
            }

            // PHASE 3 (60-90): Curtains unfurl. Chain step looping.
            if (tick == 60) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 1.6f, 0.7f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 1.0f);
            }
            if (tick > 60 && tick <= 90 && tick % 5 == 0) {
                // Curtain whispers — chain particles drift down at each curtain.
                for (BlockDisplayHandle h : curtains) {
                    Location ll = h.entity().getLocation();
                    w.spawnParticle(Particle.BLOCK, ll, 2, 0.1, 0.5, 0.1, 0.02,
                            Material.CHAIN.createBlockData());
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.7f, 0.8f);
            }

            // PHASE 4 (90-110): Chandelier descends.
            if (tick == 90) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 1.5f, 0.6f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.5f);
            }
            if (tick > 90 && tick <= 110) {
                double p = (tick - 90) / 20.0;
                chandelierY = (float) (6.5 - p * 2.0); // descend from 6.5 → 4.5
                if (chandelierCore != null) {
                    try {
                        chandelierCore.entity().teleport(cathedralCenter.clone().add(0, chandelierY, 0));
                    } catch (Throwable ignored) {}
                }
                if (tick % 3 == 0) {
                    w.spawnParticle(Particle.END_ROD, cathedralCenter.clone().add(0, chandelierY, 0), 8,
                            0.6, 0.3, 0.6, 0.05);
                }
            }

            // PHASE 5 (110-dissipateStart): Cathedral active — chandelier sways, altar pulses.
            if (tick > 110 && tick < dissipateStart) {
                chandelierSway += (float) Math.toRadians(1.2);
                float swayX = (float) (Math.sin(tick * 0.05) * Math.toRadians(5));
                float swayZ = (float) (Math.cos(tick * 0.05) * Math.toRadians(5));
                if (chandelierCore != null) {
                    rotateOn(chandelierCore.entity(), chandelierSway, 0f, 1f, 0f, 4);
                    double dx = Math.sin(tick * 0.05) * 0.15;
                    double dz = Math.cos(tick * 0.05) * 0.15;
                    try {
                        chandelierCore.entity().teleport(cathedralCenter.clone().add(dx, chandelierY, dz));
                    } catch (Throwable ignored) {}
                }
                // Move chandelier sub-pieces to follow the core.
                if (tick % 2 == 0) {
                    double dx = Math.sin(tick * 0.05) * 0.15;
                    double dz = Math.cos(tick * 0.05) * 0.15;
                    double[][] polesOff = {{0.5, 0, 0}, {-0.5, 0, 0}, {0, 0, 0.5}, {0, 0, -0.5}};
                    for (int i = 0; i < chandelierPoles.size() && i < polesOff.length; i++) {
                        teleportEntity(chandelierPoles.get(i),
                                cathedralCenter.clone().add(polesOff[i][0] + dx, chandelierY, polesOff[i][2] + dz));
                    }
                    for (int i = 0; i < chandelierChains.size(); i++) {
                        double a = (Math.PI * 2.0 * i) / chandelierChains.size();
                        double cx = Math.cos(a) * 0.8;
                        double cz = Math.sin(a) * 0.8;
                        teleportEntity(chandelierChains.get(i),
                                cathedralCenter.clone().add(cx + dx, chandelierY - 0.5, cz + dz));
                    }
                }

                // Altar pulse — scale ±0.05.
                if (tick % 4 == 0 && altar != null) {
                    float pulse = (float) (1.0 + Math.sin(tick * 0.12) * 0.05);
                    setScale(altar.entity(), 1.1f * pulse, 0.7f * pulse, 1.1f * pulse, 4);
                }

                // Active sounds.
                if (tick % 40 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 0.6f);
                }
                if (tick % 35 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.6f, 0.9f);
                }
                if (tick % 80 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 1.2f, 0.6f);
                }
                if (tick % 60 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.9f, 0.55f);
                }

                // Heavy active particles (12-20/tick layered).
                if (tick % 2 == 0) {
                    // Pillar base smoke.
                    double[][] pillarBase = {{-2.5, 0, -2.5}, {2.5, 0, -2.5}, {-2.5, 0, 2.5}, {2.5, 0, 2.5}};
                    for (double[] o : pillarBase) {
                        Location pl = cathedralCenter.clone().add(o[0], 0.2, o[2]);
                        w.spawnParticle(Particle.LARGE_SMOKE, pl, 1, 0.2, 0.1, 0.2, 0.02);
                    }
                    // Chandelier glow + sparks.
                    w.spawnParticle(Particle.ELECTRIC_SPARK, cathedralCenter.clone().add(0, chandelierY, 0), 4,
                            0.6, 0.3, 0.6, 0.1);
                    w.spawnParticle(Particle.END_ROD, cathedralCenter.clone().add(0, chandelierY, 0), 2,
                            0.5, 0.2, 0.5, 0.03);
                    // Arch detail.
                    w.spawnParticle(Particle.BLOCK, cathedralCenter.clone().add(0, 5.7, 0), 3,
                            2.5, 0.2, 2.5, 0.03, Material.NETHERITE_BLOCK.createBlockData());
                    // Altar flame.
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, cathedralCenter.clone().add(0, 1.1, 0), 3,
                            0.5, 0.2, 0.5, 0.04);
                    w.spawnParticle(Particle.FLAME, cathedralCenter.clone().add(0, 1.4, 0), 2,
                            0.4, 0.2, 0.4, 0.03);
                    // Candle glow.
                    for (BlockDisplayHandle ca : candles) {
                        w.spawnParticle(Particle.FLAME, ca.entity().getLocation().add(0, 0.3, 0), 1,
                                0.1, 0.1, 0.1, 0.02);
                    }
                }
            }

            // DISSIPATE PHASE (last 40 ticks): grand collapse outward.
            if (tick == dissipateStart) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.8f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 1.8f, 0.4f);
                w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 3.0, 0), 12, 3.0, 2.0, 3.0, 0.0);
                w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 3.0, 0), 60, 3.0, 2.0, 3.0, 0.05);
            }
            if (tick == dissipateStart + 4) {
                // Chandelier first.
                if (chandelierCore != null) shrinkToZero(chandelierCore, 10);
                for (BlockDisplayHandle h : chandelierPoles) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : chandelierChains) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.6f, 0.5f);
            }
            if (tick == dissipateStart + 12) {
                // Arches fall.
                for (BlockDisplayHandle h : archRibs) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.5f);
            }
            if (tick == dissipateStart + 20) {
                // Curtains shred, altar shatters.
                for (BlockDisplayHandle h : curtains) shrinkToZero(h, 8);
                if (altar != null) shrinkToZero(altar, 8);
                for (BlockDisplayHandle h : candles) shrinkToZero(h, 8);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.6f, 0.6f);
            }
            if (tick == dissipateStart + 28) {
                // Pillars topple outward + ground anchors snap.
                double[][] pillarBase = {{-2.5, 0, -2.5}, {2.5, 0, -2.5}, {-2.5, 0, 2.5}, {2.5, 0, 2.5}};
                for (int p = 0; p < 4; p++) {
                    // Tilt each pillar segment outward (toward its corner direction).
                    float tiltAxisX = (float) pillarBase[p][2];
                    float tiltAxisZ = -(float) pillarBase[p][0];
                    for (int seg = 0; seg < 4; seg++) {
                        int idx = p * 4 + seg;
                        if (idx < pillars.size()) {
                            rotateOn(pillars.get(idx).entity(), (float) Math.toRadians(35),
                                    tiltAxisX, 0f, tiltAxisZ, 8);
                            shrinkToZero(pillars.get(idx), 12);
                        }
                    }
                }
                for (BlockDisplayHandle h : buttresses) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : groundAnchors) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.4f);
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 0.6f);
                w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 2.0, 0), 20, 3.5, 1.5, 3.5, 0.0);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronCathedral(plugin); }
    }
}
