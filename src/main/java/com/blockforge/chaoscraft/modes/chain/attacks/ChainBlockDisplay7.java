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
import java.util.Iterator;
import java.util.List;

/**
 * Chain Mode — BLOCK DISPLAY ATTACKS 61-70 (Signature Complex, part 1)
 *
 * Signature mode-defining showcase mechanics. Each attack is a full multi-piece
 * mechanical contraption / monstrous entity built from 30-60+ BlockDisplays.
 * These are the "remember this fight" attacks — high entity counts, multi-phase
 * lifecycle, distinctive shape vocabulary so the player can read each one apart.
 *
 * Block palette: IRON_BLOCK, NETHERITE_BLOCK, CHAIN, POLISHED_BLACKSTONE_BRICKS,
 *                DARK_OAK_LOG, POLISHED_BLACKSTONE
 *
 * Particles: BLOCK(IRON_BLOCK/NETHERITE_BLOCK/CHAIN), CRIT, SMOKE, LARGE_SMOKE,
 *            ELECTRIC_SPARK, SOUL_FIRE_FLAME, EXPLOSION, LAVA, END_ROD, FLAME
 * Sounds:    BLOCK_CHAIN_*, BLOCK_ANVIL_*, BLOCK_NETHERITE_BLOCK_*,
 *            ENTITY_IRON_GOLEM_*, BLOCK_GRINDSTONE_USE, BLOCK_PISTON_*,
 *            BLOCK_BEACON_AMBIENT, ENTITY_BLAZE_AMBIENT
 *
 * Attacks:
 *  61. ChainCoffin               — sealing coffin (escape before lid lands)
 *  62. IronCoffinArray           — 5 coffins in a row (find the safe slot)
 *  63. ChainClock                — rotating hour/minute/second hands (timing dodge)
 *  64. IronCentipede             — slithering chase, FOLLOW-AI 0.08
 *  65. WreckingBallConstellation — 5 orbital wrecking balls (thread the system)
 *  66. ChainScorpion             — pincers + tail strike, FOLLOW-AI 0.07
 *  67. ChainPulleySystem         — descending weight + rising counterweight
 *  68. IronGrinder               — twin counter-rotating wheels (gap between)
 *  69. IronDragon                — serpentine flying chase, FOLLOW-AI 0.09
 *  70. IronChimney               — vertical column firing expanding rings upward
 */
public final class ChainBlockDisplay7 {
    private ChainBlockDisplay7() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ChainCoffin(plugin));
        registry.register(new IronCoffinArray(plugin));
        registry.register(new ChainClock(plugin));
        registry.register(new IronCentipede(plugin));
        registry.register(new WreckingBallConstellation(plugin));
        registry.register(new ChainScorpion(plugin));
        registry.register(new ChainPulleySystem(plugin));
        registry.register(new IronGrinder(plugin));
        registry.register(new IronDragon(plugin));
        registry.register(new IronChimney(plugin));
    }

    // ================================================================
    // Shared helpers
    // ================================================================

    /** Find the nearest non-exempt survival player within range. */
    @SuppressWarnings("unused")
    private static Player findNearestPlayer(Location center, double range) {
        if (center == null || center.getWorld() == null) return null;
        Player nearest = null;
        double nearestDist = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dist = p.getLocation().distanceSquared(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    /** Smoothly set the scale on a BlockDisplay over duration ticks. */
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

    /** Smoothly rotate a BlockDisplay around a given axis over duration ticks. */
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

    /** Smoothly shrink any handle to zero scale over duration. */
    private static void shrinkToZero(BlockDisplayHandle h, int dur) {
        if (h == null) return;
        setScale(h.entity(), 0f, 0f, 0f, dur);
    }

    /** Teleport a display by an absolute world delta. */
    @SuppressWarnings("unused")
    private static void teleportBy(BlockDisplay e, double dx, double dy, double dz) {
        if (e == null || !e.isValid()) return;
        try {
            e.teleport(e.getLocation().add(dx, dy, dz));
        } catch (Throwable ignored) {}
    }

    // ================================================================
    // #61 — CHAIN COFFIN ("The End")
    // Single long coffin assembled around player. Lid descends and seals after
    // a 60-tick escape window, then slides open at end of life.
    // 42 blocks: 12 long walls + 4 end caps + 6 lid slabs + 2 cross chains
    //          + 4 chain handles + 4 corner posts + 6 floor slabs + 4 candles.
    // ================================================================
    public static class ChainCoffin extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<BlockDisplayHandle> endCaps = new ArrayList<>();
        private final List<BlockDisplayHandle> lid = new ArrayList<>();
        private final List<BlockDisplayHandle> cross = new ArrayList<>();
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<BlockDisplayHandle> posts = new ArrayList<>();
        private final List<BlockDisplayHandle> floor = new ArrayList<>();
        private final List<BlockDisplayHandle> candles = new ArrayList<>();

        public ChainCoffin(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_coffin", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(280.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(220);
            config.setCooldownTicks(280);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Closing coffin (escape before sealed)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 0.05;

            // 6 floor slabs (POLISHED_BLACKSTONE) along the coffin base (3 long × 2 wide)
            for (int xi = -1; xi <= 1; xi++) {
                for (int zi = 0; zi <= 1; zi++) {
                    double x = xi * 0.9;
                    double z = -1.5 + zi * 3.0;
                    Location loc = center.clone().add(x, cy, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(0f, 0f, 0f).glow(40, 40, 50).interpolation(8, 0);
                    spawnedEntities.add(h.entity());
                    floor.add(h);
                    setScale(h.entity(), 0.9f, 0.08f, 1.5f, 10);
                }
            }

            // 4 corner posts (IRON_BLOCK) — coffin frame
            double[][] postOffs = {
                    {-1.0, 0.0, -1.5}, {1.0, 0.0, -1.5},
                    {-1.0, 0.0,  1.5}, {1.0, 0.0,  1.5}
            };
            for (double[] off : postOffs) {
                Location loc = center.clone().add(off[0], cy + 0.4, off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                posts.add(h);
                setScale(h.entity(), 0.16f, 0.85f, 0.16f, 12);
            }

            // 12 long walls — 3 POLISHED_BLACKSTONE_BRICKS per long side × 2 sides × 2 rows
            for (int side = 0; side < 2; side++) {
                double x = (side == 0) ? -0.95 : 0.95;
                for (int seg = 0; seg < 3; seg++) {
                    double z = -1.0 + seg * 1.0;
                    for (int row = 0; row < 2; row++) {
                        double y = cy + 0.35 + row * 0.45;
                        Location loc = center.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE_BRICKS);
                        h.scale(0f, 0f, 0f).glow(45, 45, 55).interpolation(10, seg);
                        spawnedEntities.add(h.entity());
                        walls.add(h);
                        setScale(h.entity(), 0.18f, 0.45f, 0.95f, 14);
                    }
                }
            }

            // 4 end caps — 2 per short wall
            for (int side = 0; side < 2; side++) {
                double z = (side == 0) ? -1.45 : 1.45;
                for (int xi = 0; xi < 2; xi++) {
                    double x = -0.45 + xi * 0.9;
                    Location loc = center.clone().add(x, cy + 0.55, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE_BRICKS);
                    h.scale(0f, 0f, 0f).glow(45, 45, 55).interpolation(10, 0);
                    spawnedEntities.add(h.entity());
                    endCaps.add(h);
                    setScale(h.entity(), 0.85f, 0.85f, 0.16f, 14);
                }
            }

            // 4 chain handles (one per side, 2 per long wall)
            double[][] handleOffs = {
                    {-1.05, 0.6, -0.8}, {-1.05, 0.6, 0.8},
                    {1.05, 0.6, -0.8},  {1.05, 0.6, 0.8}
            };
            for (double[] off : handleOffs) {
                Location loc = center.clone().add(off[0], cy + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(200, 200, 210).interpolation(10, 1);
                spawnedEntities.add(h.entity());
                handles.add(h);
                setScale(h.entity(), 0.12f, 0.3f, 0.16f, 12);
            }

            // 4 corner candles (END_ROD-like NETHERITE_BLOCK markers)
            double[][] candleOffs = {
                    {-1.1, 1.4, -1.6}, {1.1, 1.4, -1.6},
                    {-1.1, 1.4, 1.6},  {1.1, 1.4, 1.6}
            };
            for (double[] off : candleOffs) {
                Location loc = center.clone().add(off[0], cy + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(160, 90, 30).interpolation(10, 2);
                spawnedEntities.add(h.entity());
                candles.add(h);
                setScale(h.entity(), 0.1f, 0.2f, 0.1f, 12);
            }

            // 6 lid slabs (NETHERITE_BLOCK) — initially HIGH above the coffin, descends later
            for (int xi = 0; xi < 2; xi++) {
                for (int zi = 0; zi < 3; zi++) {
                    double x = -0.45 + xi * 0.9;
                    double z = -1.0 + zi * 1.0;
                    Location loc = center.clone().add(x, cy + 4.5, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                    h.scale(0f, 0f, 0f).glow(55, 55, 70).interpolation(10, 0);
                    spawnedEntities.add(h.entity());
                    lid.add(h);
                    setScale(h.entity(), 0.9f, 0.06f, 0.95f, 14);
                }
            }

            // 2 cross chains pressed into lid (initially up with lid)
            for (int i = 0; i < 2; i++) {
                double zOff = (i == 0) ? 0.0 : 0.0;
                double xOff = (i == 0) ? 0.0 : 0.0;
                float sx = (i == 0) ? 0.12f : 1.6f;
                float sz = (i == 0) ? 1.6f : 0.12f;
                Location loc = center.clone().add(xOff, cy + 4.55, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(10, 1);
                spawnedEntities.add(h.entity());
                cross.add(h);
                setScale(h.entity(), sx, 0.04f, sz, 12);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.4f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 0.5, 0), 40, 1.5, 0.4, 2.0, 0.02);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.3, 0), 20, 1.2, 0.3, 1.8, 0.01);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double cy = 0.05;
            int dur = config.getDurationTicks();

            // Lid descends from +4.5 -> +1.1 between ticks 30 and 60 (escape window)
            if (tick >= 30 && tick <= 60) {
                double t = (tick - 30) / 30.0;
                double yOff = 4.5 - t * 3.4; // 4.5 -> 1.1
                for (int i = 0; i < lid.size(); i++) {
                    int xi = i / 3;
                    int zi = i % 3;
                    double x = -0.45 + xi * 0.9;
                    double z = -1.0 + zi * 1.0;
                    BlockDisplay e = lid.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x, cy + yOff, z)); } catch (Throwable ignored) {}
                }
                for (int i = 0; i < cross.size(); i++) {
                    BlockDisplay e = cross.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(0, cy + yOff + 0.05, 0)); } catch (Throwable ignored) {}
                }
                if (tick == 30) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.7f);
                }
            }

            // Lid impact / seal at tick 60
            if (tick == 60) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.4f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, 1.1, 0), 6, 1.2, 0.2, 1.5, 0.04);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 1.1, 0), 30, 1.5, 0.4, 2.0, 0.03);
            }

            // Sealed phase: lid stays at +1.1, ambient soul flames on candles
            if (tick > 60 && tick < dur - 25) {
                if (tick % 6 == 0) {
                    for (BlockDisplayHandle h : candles) {
                        c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, h.entity().getLocation().add(0, 0.2, 0), 2, 0.05, 0.05, 0.05, 0.01);
                    }
                }
                if (tick % 15 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.5f);
                }
                if (tick % 40 == 0) {
                    c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 0.8, 0), 8, 1.0, 0.4, 1.5, 0.01);
                }
            }

            // Dissipate: lid slides aside (+X 2.5 over 10 ticks) then everything sinks
            if (tick == dur - 25) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.3f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_EXTEND, 1.2f, 0.4f);
            }
            if (tick >= dur - 25 && tick <= dur - 15) {
                double t = (tick - (dur - 25)) / 10.0;
                for (int i = 0; i < lid.size(); i++) {
                    int xi = i / 3;
                    int zi = i % 3;
                    double x = -0.45 + xi * 0.9 + t * 2.5;
                    double z = -1.0 + zi * 1.0;
                    BlockDisplay e = lid.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x, cy + 1.1, z)); } catch (Throwable ignored) {}
                }
                for (BlockDisplayHandle h : cross) {
                    BlockDisplay e = h.entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(t * 2.5, cy + 1.15, 0)); } catch (Throwable ignored) {}
                }
            }
            if (tick == dur - 15) {
                for (BlockDisplayHandle h : lid) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : cross) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : walls) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : endCaps) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : handles) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : posts) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : floor) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : candles) shrinkToZero(h, 12);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 0.5, 0), 40, 1.5, 0.4, 2.0, 0.04);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainCoffin(plugin); }
    }

    // ================================================================
    // #62 — IRON COFFIN ARRAY ("The Row")
    // 5 coffins in a row materialize sequentially. Find the safe slot.
    // 50 blocks total: each coffin has 4 corner posts + 4 horizontal bars + 2 lids = 10 blocks.
    // ================================================================
    public static class IronCoffinArray extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> coffins = new ArrayList<>();
        private final List<BlockDisplayHandle> allParts = new ArrayList<>();
        private final int coffinCount = 5;
        private final double spacing = 2.5;

        public IronCoffinArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_coffin_array", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(240.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(240);
            config.setCooldownTicks(280);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Multi-coffin (find safe slot)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Pre-create handles for all 5 coffins with scale 0; they will pop in sequentially in onTick.
            for (int c = 0; c < coffinCount; c++) {
                double offsetX = (c - 2) * spacing; // center the row of 5
                List<BlockDisplayHandle> coffin = new ArrayList<>();

                // 4 corner posts (IRON_BLOCK)
                double[][] postOffs = {
                        {-0.9, 0.0, -1.3}, {0.9, 0.0, -1.3},
                        {-0.9, 0.0,  1.3}, {0.9, 0.0,  1.3}
                };
                for (double[] off : postOffs) {
                    Location loc = center.clone().add(offsetX + off[0], 0.5, off[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(200, 200, 210).interpolation(8, 0);
                    spawnedEntities.add(h.entity());
                    coffin.add(h);
                    allParts.add(h);
                }
                // 4 horizontal bars (2 long sides × 2 rows)
                for (int side = 0; side < 2; side++) {
                    double xOff = (side == 0) ? -0.85 : 0.85;
                    for (int row = 0; row < 2; row++) {
                        double y = 0.4 + row * 0.45;
                        Location loc = center.clone().add(offsetX + xOff, y, 0);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                        h.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(8, 0);
                        spawnedEntities.add(h.entity());
                        coffin.add(h);
                        allParts.add(h);
                    }
                }
                // 2 lid slabs (NETHERITE_BLOCK) — present but hidden until spawn moment
                for (int xi = 0; xi < 2; xi++) {
                    double xOff = -0.45 + xi * 0.9;
                    Location loc = center.clone().add(offsetX + xOff, 1.1, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                    h.scale(0f, 0f, 0f).glow(60, 60, 75).interpolation(10, 0);
                    spawnedEntities.add(h.entity());
                    coffin.add(h);
                    allParts.add(h);
                }
                coffins.add(coffin);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.9f, 0.5f);
            w.spawnParticle(Particle.LARGE_SMOKE, center, 30, 5.0, 0.4, 1.5, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            int dur = config.getDurationTicks();

            // Sequential pop-in: coffin i appears at tick i*20
            for (int i = 0; i < coffins.size(); i++) {
                int spawnTick = i * 20;
                if (tick == spawnTick) {
                    List<BlockDisplayHandle> coffin = coffins.get(i);
                    // posts (4): tall + thin
                    for (int p = 0; p < 4; p++) {
                        setScale(coffin.get(p).entity(), 0.2f, 1.5f, 0.2f, 12);
                    }
                    // bars (4): long + thin
                    for (int b = 4; b < 8; b++) {
                        setScale(coffin.get(b).entity(), 0.16f, 0.4f, 2.5f, 12);
                    }
                    // lid (2): flat slab
                    for (int l = 8; l < 10; l++) {
                        setScale(coffin.get(l).entity(), 0.9f, 0.08f, 2.6f, 14);
                    }
                    double offX = (i - 2) * spacing;
                    DisplayBuilder.playSound(c.clone().add(offX, 0.5, 0), Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.65f);
                    DisplayBuilder.playSound(c.clone().add(offX, 0.5, 0), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
                    c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(offX, 0.5, 0), 20, 1.0, 0.4, 1.2, 0.02);
                    c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(offX, 0.5, 0), 15, 1.0, 0.4, 1.2, 0.02, Material.IRON_BLOCK.createBlockData());
                }
            }

            // Mid-life ambient: chain rattle sweeps down the line every 30t
            if (tick > 100 && tick % 30 == 0) {
                int idx = ((tick - 100) / 30) % 5;
                double offX = (idx - 2) * spacing;
                DisplayBuilder.playSound(c.clone().add(offX, 0.5, 0), Sound.BLOCK_CHAIN_HIT, 0.8f, 0.6f);
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(offX, 1.0, 0), 6, 0.4, 0.3, 0.4, 0.01);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.45f);
            }

            // Soul-flame ambient between coffins
            if (tick > 100 && tick % 8 == 0) {
                for (int i = 0; i < coffinCount; i++) {
                    double offX = (i - 2) * spacing;
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(offX, 1.5, 0), 2, 0.2, 0.1, 0.2, 0.005);
                }
            }

            // Dissipate: coffin parts collapse staggered
            if (tick == dur - 20) {
                for (List<BlockDisplayHandle> coffin : coffins) {
                    for (BlockDisplayHandle h : coffin) shrinkToZero(h, 16);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.4f, 0.5f);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c, 60, 5.5, 0.5, 1.5, 0.03);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronCoffinArray(plugin); }
    }

    // ================================================================
    // #63 — CHAIN CLOCK ("The Mechanism")
    // Flat clock face at chest height. 3 hands rotating at distinct speeds:
    // hour hand (slow + thick), minute hand (medium + medium), second hand
    // (fast + thin). 12 outer numerals + 12 inner ticks + hub.
    // 50 blocks: 12 outer numerals + 12 inner tick marks + 12 ring slabs
    //          + 3 hands (1 each, but each hand is a multi-piece beam)
    //          + 4 hand segments hour + 5 minute + 6 second + 1 hub = 50+.
    // Simplified: 12 ring + 12 numerals + 12 ticks + 4 hourSeg + 5 minSeg + 6 secSeg + 1 hub = 56.
    // ================================================================
    public static class ChainClock extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ring = new ArrayList<>();
        private final List<BlockDisplayHandle> numerals = new ArrayList<>();
        private final List<BlockDisplayHandle> ticks = new ArrayList<>();
        private final List<BlockDisplayHandle> hourHand = new ArrayList<>();
        private final List<BlockDisplayHandle> minuteHand = new ArrayList<>();
        private final List<BlockDisplayHandle> secondHand = new ArrayList<>();
        private final List<BlockDisplayHandle> hub = new ArrayList<>();

        private float hourAngle = 0f;   // rad
        private float minuteAngle = 0f; // rad
        private float secondAngle = 0f; // rad

        public ChainClock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_clock", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(340.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(300);
            config.setCooldownTicks(320);
            config.setChance(4);
            config.setEnabled(true);
            config.setDesignType("Rotating clock arms (timing dodge)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 1.5; // chest height pivot

            // Outer ring: 12 IRON_BLOCK flat slabs at r=2.5
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12.0;
                double x = Math.cos(a) * 2.5;
                double z = Math.sin(a) * 2.5;
                Location loc = center.clone().add(x, cy, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(10, i / 3);
                spawnedEntities.add(h.entity());
                ring.add(h);
                setScale(h.entity(), 0.55f, 0.1f, 0.18f, 14);
            }

            // 12 outer numerals (NETHERITE_BLOCK) at r=2.85, marking 12-hour positions
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12.0;
                double x = Math.cos(a) * 2.85;
                double z = Math.sin(a) * 2.85;
                Location loc = center.clone().add(x, cy, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(80, 80, 100).interpolation(10, i / 3);
                spawnedEntities.add(h.entity());
                numerals.add(h);
                setScale(h.entity(), 0.14f, 0.14f, 0.14f, 12);
            }

            // 12 inner tick marks at r=2.1 (small chain ticks)
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12.0;
                double x = Math.cos(a) * 2.1;
                double z = Math.sin(a) * 2.1;
                Location loc = center.clone().add(x, cy, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(8, i / 4);
                spawnedEntities.add(h.entity());
                ticks.add(h);
                setScale(h.entity(), 0.08f, 0.06f, 0.08f, 10);
            }

            // Hub: 1 NETHERITE_BLOCK center
            BlockDisplayHandle hubCore = displayBuilder.spawnBlock(center.clone().add(0, cy, 0), Material.NETHERITE_BLOCK);
            hubCore.scale(0f, 0f, 0f).glow(60, 60, 80).interpolation(10, 0);
            spawnedEntities.add(hubCore.entity());
            hub.add(hubCore);
            setScale(hubCore.entity(), 0.32f, 0.18f, 0.32f, 12);

            // Hour hand: 4 IRON_BLOCK segments pointing +Z initially, r 0.3, 0.9, 1.5, 1.85
            double[] hourRs = {0.3, 0.9, 1.5, 1.85};
            float[] hourScales = {0.18f, 0.16f, 0.14f, 0.12f};
            for (int i = 0; i < hourRs.length; i++) {
                Location loc = center.clone().add(0, cy, hourRs[i]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(230, 230, 240).interpolation(10, i);
                spawnedEntities.add(h.entity());
                hourHand.add(h);
                setScale(h.entity(), hourScales[i], 0.1f, 0.3f, 12);
            }

            // Minute hand: 5 NETHERITE segments pointing +X initially, r 0.4, 1.0, 1.6, 2.0, 2.25
            double[] minRs = {0.4, 1.0, 1.6, 2.0, 2.25};
            float[] minScales = {0.11f, 0.10f, 0.09f, 0.09f, 0.09f};
            for (int i = 0; i < minRs.length; i++) {
                Location loc = center.clone().add(minRs[i], cy, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(90, 90, 110).interpolation(10, i);
                spawnedEntities.add(h.entity());
                minuteHand.add(h);
                setScale(h.entity(), 0.22f, 0.08f, minScales[i], 12);
            }

            // Second hand: 6 CHAIN segments pointing -Z initially, r 0.35, 0.85, 1.35, 1.75, 2.1, 2.4
            double[] secRs = {0.35, 0.85, 1.35, 1.75, 2.1, 2.4};
            for (int i = 0; i < secRs.length; i++) {
                Location loc = center.clone().add(0, cy, -secRs[i]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(255, 60, 60).interpolation(10, i);
                spawnedEntities.add(h.entity());
                secondHand.add(h);
                setScale(h.entity(), 0.07f, 0.06f, 0.22f, 12);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.0f, 0.6f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, cy, 0), 30, 2.5, 0.5, 2.5, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double cy = 1.5;
            int dur = config.getDurationTicks();

            if (tick > 25 && tick < dur - 25) {
                // hour:   1.5 deg/tick
                // minute: 6 deg/tick
                // second: 18 deg/tick
                hourAngle   += (float) Math.toRadians(1.5);
                minuteAngle += (float) Math.toRadians(6.0);
                secondAngle += (float) Math.toRadians(18.0);

                // Hour hand sweep
                double[] hourRs = {0.3, 0.9, 1.5, 1.85};
                for (int i = 0; i < hourHand.size(); i++) {
                    double r = hourRs[i];
                    double x = Math.sin(hourAngle) * r;
                    double z = Math.cos(hourAngle) * r;
                    BlockDisplay e = hourHand.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x, cy, z)); } catch (Throwable ignored) {}
                    rotateOn(e, hourAngle, 0f, 1f, 0f, 4);
                }
                // Minute hand sweep — offset 90deg head-start
                double[] minRs = {0.4, 1.0, 1.6, 2.0, 2.25};
                for (int i = 0; i < minuteHand.size(); i++) {
                    double r = minRs[i];
                    double a = minuteAngle + (Math.PI / 2.0);
                    double x = Math.sin(a) * r;
                    double z = Math.cos(a) * r;
                    BlockDisplay e = minuteHand.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x, cy, z)); } catch (Throwable ignored) {}
                    rotateOn(e, (float) a, 0f, 1f, 0f, 4);
                }
                // Second hand sweep — offset 180deg head-start
                double[] secRs = {0.35, 0.85, 1.35, 1.75, 2.1, 2.4};
                for (int i = 0; i < secondHand.size(); i++) {
                    double r = secRs[i];
                    double a = secondAngle + Math.PI;
                    double x = Math.sin(a) * r;
                    double z = Math.cos(a) * r;
                    BlockDisplay e = secondHand.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x, cy, z)); } catch (Throwable ignored) {}
                    rotateOn(e, (float) a, 0f, 1f, 0f, 4);
                }
            }

            // Ticking sound every 20t (1 sec)
            if (tick > 25 && tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.8f, 1.2f);
            }
            // Hour chime every 240t
            if (tick > 25 && tick % 240 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 1.2f, 0.7f);
            }
            // Hand-cross detection: spark when second crosses minute hand
            if (tick > 25 && !secondHand.isEmpty() && !minuteHand.isEmpty()) {
                BlockDisplay sec = secondHand.get(secondHand.size() - 1).entity();
                BlockDisplay min = minuteHand.get(minuteHand.size() - 1).entity();
                if (sec != null && sec.isValid() && min != null && min.isValid()) {
                    if (sec.getLocation().distanceSquared(min.getLocation()) < 0.8) {
                        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, sec.getLocation(), 8, 0.2, 0.2, 0.2, 0.15);
                        if (tick % 4 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_HIT, 0.6f, 1.4f);
                    }
                }
            }
            // Tip sparks every 6t
            if (tick > 25 && tick % 6 == 0) {
                if (!hourHand.isEmpty())
                    c.getWorld().spawnParticle(Particle.CRIT, hourHand.get(hourHand.size() - 1).entity().getLocation(), 3, 0.15, 0.15, 0.15, 0.1);
                if (!minuteHand.isEmpty())
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, minuteHand.get(minuteHand.size() - 1).entity().getLocation(), 3, 0.15, 0.15, 0.15, 0.1);
                if (!secondHand.isEmpty())
                    c.getWorld().spawnParticle(Particle.END_ROD, secondHand.get(secondHand.size() - 1).entity().getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
            }

            // Dissipate
            if (tick == dur - 25) {
                for (BlockDisplayHandle h : hourHand) shrinkToZero(h, 14);
                for (BlockDisplayHandle h : minuteHand) shrinkToZero(h, 14);
                for (BlockDisplayHandle h : secondHand) shrinkToZero(h, 14);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.3f, 0.4f);
            }
            if (tick == dur - 12) {
                for (BlockDisplayHandle h : ring) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : numerals) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : ticks) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : hub) shrinkToZero(h, 10);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, cy, 0), 6, 1.0, 0.4, 1.0, 0.03);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, cy, 0), 30, 2.5, 0.5, 2.5, 0.04);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainClock(plugin); }
    }

    // ================================================================
    // #64 — IRON CENTIPEDE ("The Crawler")
    // 12-segment iron centipede with head + mandibles + legs + tail.
    // Uses follow-AI 0.08 + segment history pattern.
    // 50 blocks: 1 head + 2 mandibles + 2 eyes + 10 body + 20 legs (2 per body)
    //          + 1 tail + 6 antennae extras + 8 spine spikes.
    // ================================================================
    public static class IronCentipede extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> mandibles = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>(); // 2 per body, stored sequentially
        private final List<BlockDisplayHandle> tail = new ArrayList<>();
        private final List<BlockDisplayHandle> antennae = new ArrayList<>();
        private final List<BlockDisplayHandle> spines = new ArrayList<>();
        private final List<double[]> history = new ArrayList<>();
        private float undulation = 0f;
        private float mandibleAngle = 0f;

        public IronCentipede(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_centipede", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(280);
            config.setCooldownTicks(280);
            config.setChance(5);
            config.setEnabled(true);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.08);
            config.setDesignType("Slithering chase (read serpent path)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            double cy = 0.5;

            // Head
            BlockDisplayHandle h0 = displayBuilder.spawnBlock(center.clone().add(0, cy, 0), Material.IRON_BLOCK);
            h0.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(10, 0);
            spawnedEntities.add(h0.entity());
            head.add(h0);
            setScale(h0.entity(), 0.5f, 0.33f, 0.62f, 12);

            // 2 mandibles (NETHERITE_BLOCK angled outward)
            for (int i = 0; i < 2; i++) {
                double px = (i == 0) ? -0.2 : 0.2;
                Location loc = center.clone().add(px, cy + 0.05, 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(60, 60, 80).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                mandibles.add(h);
                setScale(h.entity(), 0.07f, 0.2f, 0.24f, 12);
                rotateOn(h.entity(), (float) Math.toRadians(i == 0 ? -30 : 30), 0f, 1f, 0f, 12);
            }
            // 2 eyes
            for (int i = 0; i < 2; i++) {
                double px = (i == 0) ? -0.15 : 0.15;
                Location loc = center.clone().add(px, cy + 0.15, 0.25);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(255, 40, 40).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                eyes.add(h);
                setScale(h.entity(), 0.08f, 0.08f, 0.05f, 12);
            }

            // 10 body segments + 2 legs per segment + 1 spine spike per segment
            for (int i = 0; i < 10; i++) {
                double z = -0.5 - i * 0.45;
                Location loc = center.clone().add(0, cy, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 210).interpolation(8, i / 2);
                spawnedEntities.add(h.entity());
                body.add(h);
                setScale(h.entity(), 0.42f, 0.26f, 0.46f, 10);

                // 2 legs (1 per side)
                for (int s = 0; s < 2; s++) {
                    double lx = (s == 0) ? -0.3 : 0.3;
                    Location ll = center.clone().add(lx, cy - 0.1, z);
                    BlockDisplayHandle hl = displayBuilder.spawnBlock(ll, Material.IRON_BLOCK);
                    hl.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(8, i / 2);
                    spawnedEntities.add(hl.entity());
                    legs.add(hl);
                    setScale(hl.entity(), 0.18f, 0.04f, 0.12f, 10);
                }

                // spine spike on top
                if (i < 8) {
                    Location sl = center.clone().add(0, cy + 0.2, z);
                    BlockDisplayHandle hs = displayBuilder.spawnBlock(sl, Material.CHAIN);
                    hs.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(8, i / 2);
                    spawnedEntities.add(hs.entity());
                    spines.add(hs);
                    setScale(hs.entity(), 0.07f, 0.18f, 0.07f, 10);
                }
            }

            // Tail
            BlockDisplayHandle ht = displayBuilder.spawnBlock(center.clone().add(0, cy, -5.5), Material.IRON_BLOCK);
            ht.scale(0f, 0f, 0f).glow(180, 180, 190).interpolation(8, 8);
            spawnedEntities.add(ht.entity());
            tail.add(ht);
            setScale(ht.entity(), 0.28f, 0.22f, 0.36f, 10);

            // 6 antennae chains on head
            double[][] antOffs = {
                    {-0.18, 0.32, 0.25}, {0.18, 0.32, 0.25},
                    {-0.12, 0.38, 0.18}, {0.12, 0.38, 0.18},
                    {-0.22, 0.28, 0.15}, {0.22, 0.28, 0.15}
            };
            for (double[] off : antOffs) {
                Location loc = center.clone().add(off[0], cy + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                antennae.add(h);
                setScale(h.entity(), 0.05f, 0.18f, 0.05f, 12);
            }

            for (int i = 0; i < 60; i++) {
                history.add(new double[]{center.getX(), center.getY() + cy, center.getZ()});
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 1.2f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.9f, 0.5f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, cy, 0), 25, 1.2, 0.4, 1.2, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double cy = 0.5;
            int dur = config.getDurationTicks();

            undulation += 0.18f;
            mandibleAngle = (float) Math.toRadians(15.0) * (float) Math.sin(tick * 0.25);

            history.add(new double[]{c.getX(), c.getY() + cy, c.getZ()});
            if (history.size() > 200) history.remove(0);

            // Head
            BlockDisplay headE = head.get(0).entity();
            if (headE != null && headE.isValid()) {
                try { headE.teleport(c.clone().add(0, cy, 0)); } catch (Throwable ignored) {}
            }
            // Mandibles open/close
            for (int i = 0; i < mandibles.size(); i++) {
                BlockDisplay e = mandibles.get(i).entity();
                if (e == null || !e.isValid()) continue;
                double px = (i == 0) ? -0.2 : 0.2;
                try { e.teleport(c.clone().add(px, cy + 0.05, 0.4)); } catch (Throwable ignored) {}
                float ma = (i == 0) ? -mandibleAngle : mandibleAngle;
                rotateOn(e, (float) (Math.toRadians(i == 0 ? -30 : 30) + ma), 0f, 1f, 0f, 4);
            }
            for (int i = 0; i < eyes.size(); i++) {
                BlockDisplay e = eyes.get(i).entity();
                if (e == null || !e.isValid()) continue;
                double px = (i == 0) ? -0.15 : 0.15;
                try { e.teleport(c.clone().add(px, cy + 0.15, 0.25)); } catch (Throwable ignored) {}
            }
            // Antennae follow head
            double[][] antOffs = {
                    {-0.18, 0.32, 0.25}, {0.18, 0.32, 0.25},
                    {-0.12, 0.38, 0.18}, {0.12, 0.38, 0.18},
                    {-0.22, 0.28, 0.15}, {0.22, 0.28, 0.15}
            };
            for (int i = 0; i < antennae.size(); i++) {
                BlockDisplay e = antennae.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(c.clone().add(antOffs[i][0], cy + antOffs[i][1], antOffs[i][2])); } catch (Throwable ignored) {}
            }
            // Body segments lag 2 ticks each with horizontal sine undulation
            for (int i = 0; i < body.size(); i++) {
                int back = (i + 1) * 2;
                int idx = history.size() - 1 - back;
                if (idx < 0) idx = 0;
                double[] pos = history.get(idx);
                double off = Math.sin(undulation + i * 0.55) * 0.4;
                BlockDisplay e = body.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(new Location(c.getWorld(), pos[0] + off, pos[1], pos[2])); } catch (Throwable ignored) {}
                // Each body's 2 legs
                for (int s = 0; s < 2; s++) {
                    int legIdx = i * 2 + s;
                    if (legIdx >= legs.size()) continue;
                    BlockDisplay le = legs.get(legIdx).entity();
                    if (le == null || !le.isValid()) continue;
                    double lx = (s == 0) ? -0.3 : 0.3;
                    double lyBob = Math.sin(undulation * 1.6 + i * 0.7 + s * Math.PI) * 0.08;
                    try { le.teleport(new Location(c.getWorld(), pos[0] + off + lx, pos[1] - 0.1 + lyBob, pos[2])); } catch (Throwable ignored) {}
                }
                // Spine spike
                if (i < spines.size()) {
                    BlockDisplay sp = spines.get(i).entity();
                    if (sp != null && sp.isValid()) {
                        try { sp.teleport(new Location(c.getWorld(), pos[0] + off, pos[1] + 0.2, pos[2])); } catch (Throwable ignored) {}
                    }
                }
            }
            // Tail
            {
                int back = (body.size() + 1) * 2;
                int idx = history.size() - 1 - back;
                if (idx < 0) idx = 0;
                double[] pos = history.get(idx);
                double off = Math.sin(undulation + body.size() * 0.55) * 0.4;
                BlockDisplay e = tail.get(0).entity();
                if (e != null && e.isValid()) {
                    try { e.teleport(new Location(c.getWorld(), pos[0] + off, pos[1], pos[2])); } catch (Throwable ignored) {}
                }
            }

            // Footfall sounds + leg sparks
            if (tick > 0 && tick % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.6f, 0.9f);
                for (int i = 0; i < legs.size(); i += 6) {
                    BlockDisplay le = legs.get(i).entity();
                    if (le != null && le.isValid()) {
                        c.getWorld().spawnParticle(Particle.BLOCK, le.getLocation(), 3, 0.2, 0.05, 0.2, 0.02, Material.IRON_BLOCK.createBlockData());
                    }
                }
            }
            if (tick > 0 && tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 0.5f);
            }
            if (tick > 0 && tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.7f, 0.7f);
            }
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, cy, 0), 2, 0.4, 0.2, 0.4, 0.01);
            }

            // Dissipate
            if (tick >= dur - 30) {
                int dt = tick - (dur - 30);
                if (dt < body.size()) {
                    int bi = body.size() - 1 - dt;
                    if (bi >= 0) shrinkToZero(body.get(bi), 4);
                    if (bi * 2 + 1 < legs.size()) {
                        shrinkToZero(legs.get(bi * 2), 4);
                        shrinkToZero(legs.get(bi * 2 + 1), 4);
                    }
                    if (bi < spines.size()) shrinkToZero(spines.get(bi), 4);
                }
                if (dt == 24) {
                    for (BlockDisplayHandle h : head) shrinkToZero(h, 5);
                    for (BlockDisplayHandle h : mandibles) shrinkToZero(h, 5);
                    for (BlockDisplayHandle h : eyes) shrinkToZero(h, 5);
                    for (BlockDisplayHandle h : antennae) shrinkToZero(h, 5);
                    for (BlockDisplayHandle h : tail) shrinkToZero(h, 5);
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.2f, 0.6f);
                    c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, cy, 0), 6, 1.0, 0.4, 1.0, 0.03);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronCentipede(plugin); }
    }

    // ================================================================
    // #65 — WRECKING BALL CONSTELLATION ("The System")
    // 5 wrecking balls orbiting a central hub in pentagon formation, each
    // connected by chain links. Rotation has a mid-life double-speed burst.
    // 51 blocks: 1 central hub + 5 ball cores + 5*4 pole spikes (20)
    //          + 5*3 connecting chains (15) + 10 ambient ring chains.
    // ================================================================
    public static class WreckingBallConstellation extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> hub = new ArrayList<>();
        private final List<BlockDisplayHandle> ballCores = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> ballPoles = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> chainLinks = new ArrayList<>(); // links[i] between ball i and ball i+1
        private final List<BlockDisplayHandle> ambientRing = new ArrayList<>();
        private float ringAngle = 0f;
        private final int ballCount = 5;
        private final double orbitR = 4.5;

        public WreckingBallConstellation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wrecking_ball_constellation", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(380.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(320);
            config.setCooldownTicks(340);
            config.setChance(4);
            config.setEnabled(true);
            config.setDesignType("Multi-orbit (thread through)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            double cy = 1.4;

            // 1 central hub (NETHERITE_BLOCK)
            BlockDisplayHandle hubCore = displayBuilder.spawnBlock(center.clone().add(0, cy, 0), Material.NETHERITE_BLOCK);
            hubCore.scale(0f, 0f, 0f).glow(70, 70, 90).interpolation(12, 0);
            spawnedEntities.add(hubCore.entity());
            hub.add(hubCore);
            setScale(hubCore.entity(), 0.45f, 0.45f, 0.45f, 14);

            // 5 balls in pentagon
            for (int i = 0; i < ballCount; i++) {
                double a = (Math.PI * 2.0 * i) / ballCount;
                double x = Math.cos(a) * orbitR;
                double z = Math.sin(a) * orbitR;

                BlockDisplayHandle core = displayBuilder.spawnBlock(center.clone().add(x, cy, z), Material.NETHERITE_BLOCK);
                core.scale(0f, 0f, 0f).glow(80, 80, 100).interpolation(12, 2);
                spawnedEntities.add(core.entity());
                ballCores.add(core);
                setScale(core.entity(), 0.6f, 0.6f, 0.6f, 14);

                // 4 IRON_BLOCK poles per ball (cross spike pattern)
                List<BlockDisplayHandle> poles = new ArrayList<>();
                double[][] poleOffs = {{0.35, 0, 0}, {-0.35, 0, 0}, {0, 0.35, 0}, {0, -0.35, 0}};
                for (double[] off : poleOffs) {
                    Location loc = center.clone().add(x + off[0], cy + off[1], z + off[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(12, 3);
                    spawnedEntities.add(h.entity());
                    poles.add(h);
                    setScale(h.entity(), 0.22f, 0.22f, 0.22f, 14);
                }
                ballPoles.add(poles);
            }

            // 5 sets of 3 chain links between adjacent balls
            for (int i = 0; i < ballCount; i++) {
                int next = (i + 1) % ballCount;
                double a1 = (Math.PI * 2.0 * i) / ballCount;
                double a2 = (Math.PI * 2.0 * next) / ballCount;
                double x1 = Math.cos(a1) * orbitR, z1 = Math.sin(a1) * orbitR;
                double x2 = Math.cos(a2) * orbitR, z2 = Math.sin(a2) * orbitR;
                List<BlockDisplayHandle> chainSet = new ArrayList<>();
                for (int k = 0; k < 3; k++) {
                    double t = (k + 1) / 4.0;
                    double cx = x1 + (x2 - x1) * t;
                    double cz = z1 + (z2 - z1) * t;
                    Location loc = center.clone().add(cx, cy, cz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(10, 4);
                    spawnedEntities.add(h.entity());
                    chainSet.add(h);
                    setScale(h.entity(), 0.16f, 0.16f, 0.16f, 12);
                }
                chainLinks.add(chainSet);
            }

            // 10 ambient ring chains low to ground at r=orbitR-0.5
            for (int i = 0; i < 10; i++) {
                double a = (Math.PI * 2.0 * i) / 10.0;
                double x = Math.cos(a) * (orbitR - 0.5);
                double z = Math.sin(a) * (orbitR - 0.5);
                Location loc = center.clone().add(x, 0.2, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(10, 5);
                spawnedEntities.add(h.entity());
                ambientRing.add(h);
                setScale(h.entity(), 0.18f, 0.06f, 0.18f, 14);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.6f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.2f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.6f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, cy, 0), 40, 4.0, 0.5, 4.0, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double cy = 1.4;
            int dur = config.getDurationTicks();

            // Rotation speed: 5 deg/tick normally, 10 deg/tick during ticks 140-180 (burst)
            float baseSpeed = 5.0f;
            if (tick >= dur / 2 - 20 && tick <= dur / 2 + 20) {
                baseSpeed = 10.0f;
            }
            if (tick > 25) ringAngle += (float) Math.toRadians(baseSpeed);

            // Update each ball position
            for (int i = 0; i < ballCount; i++) {
                double a = (Math.PI * 2.0 * i) / ballCount + ringAngle;
                double x = Math.cos(a) * orbitR;
                double z = Math.sin(a) * orbitR;
                BlockDisplay core = ballCores.get(i).entity();
                if (core != null && core.isValid()) {
                    try { core.teleport(c.clone().add(x, cy, z)); } catch (Throwable ignored) {}
                }
                // poles follow
                double[][] poleOffs = {{0.35, 0, 0}, {-0.35, 0, 0}, {0, 0.35, 0}, {0, -0.35, 0}};
                List<BlockDisplayHandle> poles = ballPoles.get(i);
                for (int p = 0; p < poles.size(); p++) {
                    BlockDisplay e = poles.get(p).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x + poleOffs[p][0], cy + poleOffs[p][1], z + poleOffs[p][2])); } catch (Throwable ignored) {}
                }
            }
            // Update chain links between adjacent balls
            for (int i = 0; i < ballCount; i++) {
                int next = (i + 1) % ballCount;
                double a1 = (Math.PI * 2.0 * i) / ballCount + ringAngle;
                double a2 = (Math.PI * 2.0 * next) / ballCount + ringAngle;
                double x1 = Math.cos(a1) * orbitR, z1 = Math.sin(a1) * orbitR;
                double x2 = Math.cos(a2) * orbitR, z2 = Math.sin(a2) * orbitR;
                List<BlockDisplayHandle> set = chainLinks.get(i);
                for (int k = 0; k < set.size(); k++) {
                    double t = (k + 1) / 4.0;
                    double cx = x1 + (x2 - x1) * t;
                    double cz = z1 + (z2 - z1) * t;
                    BlockDisplay e = set.get(k).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(cx, cy, cz)); } catch (Throwable ignored) {}
                }
            }

            // Particles: CRIT trail on each ball
            if (tick > 25 && tick % 3 == 0) {
                for (BlockDisplayHandle b : ballCores) {
                    c.getWorld().spawnParticle(Particle.CRIT, b.entity().getLocation(), 4, 0.3, 0.3, 0.3, 0.1);
                }
            }
            if (tick > 25 && tick % 5 == 0) {
                for (BlockDisplayHandle b : ballCores) {
                    c.getWorld().spawnParticle(Particle.BLOCK, b.entity().getLocation(), 3, 0.3, 0.3, 0.3, 0.05, Material.NETHERITE_BLOCK.createBlockData());
                }
            }
            // Burst FX
            if (tick == dur / 2 - 20) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.6f, 0.4f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.5f);
                for (BlockDisplayHandle b : ballCores) {
                    c.getWorld().spawnParticle(Particle.EXPLOSION, b.entity().getLocation(), 4, 0.4, 0.4, 0.4, 0.05);
                }
            }
            if (tick > 25 && tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.7f, 0.7f);
            }
            if (tick > 25 && tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 0.7f);
            }

            // Dissipate
            if (tick == dur - 25) {
                for (BlockDisplayHandle b : ballCores) shrinkToZero(b, 14);
                for (List<BlockDisplayHandle> poles : ballPoles) for (BlockDisplayHandle h : poles) shrinkToZero(h, 14);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.4f, 0.4f);
            }
            if (tick == dur - 12) {
                for (List<BlockDisplayHandle> set : chainLinks) for (BlockDisplayHandle h : set) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : ambientRing) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : hub) shrinkToZero(h, 10);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, cy, 0), 50, 4.0, 0.5, 4.0, 0.04);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new WreckingBallConstellation(plugin); }
    }

    // ================================================================
    // #66 — CHAIN SCORPION ("The Sting")
    // Iron scorpion: body + head + pincers + 6 legs + arching tail + stinger.
    // Tail strikes every 40 ticks. Uses follow-AI 0.07.
    // 50 blocks: 3 body + 1 head + 2 eyes + 4 pincer parts + 6 legs
    //          + 5 tail segs + 1 stinger + 4 extra carapace + 24 misc segments.
    // Total roster (cleaned): 3+1+2+4+6+5+1+4 = 26. Add: 8 underbelly + 8 spines + 8 chitin spikes = 50.
    // ================================================================
    public static class ChainScorpion extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final List<BlockDisplayHandle> pincers = new ArrayList<>(); // 4 pieces
        private final List<BlockDisplayHandle> legs = new ArrayList<>(); // 6
        private final List<BlockDisplayHandle> tail = new ArrayList<>(); // 5
        private final List<BlockDisplayHandle> stinger = new ArrayList<>();
        private final List<BlockDisplayHandle> carapace = new ArrayList<>(); // 4
        private final List<BlockDisplayHandle> underbelly = new ArrayList<>(); // 8
        private final List<BlockDisplayHandle> spines = new ArrayList<>(); // 8
        private final List<BlockDisplayHandle> chitin = new ArrayList<>(); // 8
        private float pincerAngle = 0f;
        private int strikePhase = 0; // 0=idle, 1=windup, 2=strike, 3=reset
        private int strikeCounter = 0;

        public ChainScorpion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_scorpion", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(360.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
            config.setChance(5);
            config.setEnabled(true);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.07);
            config.setDesignType("Sting + claws (read 3 hit zones)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            double cy = 0.6;

            // 3 body segments
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, cy, -i * 0.55);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 210).interpolation(10, i);
                spawnedEntities.add(h.entity());
                body.add(h);
                setScale(h.entity(), 0.52f, 0.38f, 0.55f, 12);
            }
            // 1 head
            BlockDisplayHandle hh = displayBuilder.spawnBlock(center.clone().add(0, cy, 0.55), Material.IRON_BLOCK);
            hh.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(10, 0);
            spawnedEntities.add(hh.entity());
            head.add(hh);
            setScale(hh.entity(), 0.46f, 0.33f, 0.52f, 12);
            // 2 eyes
            for (int i = 0; i < 2; i++) {
                double px = (i == 0) ? -0.12 : 0.12;
                Location loc = center.clone().add(px, cy + 0.18, 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(255, 60, 30).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                eyes.add(h);
                setScale(h.entity(), 0.1f, 0.08f, 0.05f, 12);
            }
            // 2 pincers — 2 halves each (4 displays total)
            for (int side = 0; side < 2; side++) {
                double sx = (side == 0) ? -0.55 : 0.55;
                for (int half = 0; half < 2; half++) {
                    double hyOff = (half == 0) ? 0.05 : -0.05;
                    Location loc = center.clone().add(sx, cy + hyOff, 0.85);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(225, 225, 230).interpolation(10, 1);
                    spawnedEntities.add(h.entity());
                    pincers.add(h);
                    setScale(h.entity(), 0.12f, 0.08f, 0.35f, 12);
                }
            }
            // 6 legs: 3 per side as CHAIN flat slabs
            for (int side = 0; side < 2; side++) {
                double sx = (side == 0) ? -0.45 : 0.45;
                for (int leg = 0; leg < 3; leg++) {
                    double zOff = 0.15 - leg * 0.45;
                    Location loc = center.clone().add(sx, cy - 0.15, zOff);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(10, 1);
                    spawnedEntities.add(h.entity());
                    legs.add(h);
                    setScale(h.entity(), 0.2f, 0.04f, 0.12f, 12);
                    rotateOn(h.entity(), (float) Math.toRadians(side == 0 ? -30 : 30), 0f, 0f, 1f, 12);
                }
            }
            // 5 tail segments arching upward then forward
            for (int i = 0; i < 5; i++) {
                double yOff = 0.4 + i * 0.42;
                double zOff = -1.7 - i * 0.28 + (i >= 3 ? (i - 2) * 0.42 : 0);
                Location loc = center.clone().add(0, cy + yOff, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(210, 210, 225).interpolation(10, i + 2);
                spawnedEntities.add(h.entity());
                tail.add(h);
                setScale(h.entity(), 0.18f, 0.18f, 0.32f, 12);
            }
            // Stinger (NETHERITE_BLOCK) at tip
            BlockDisplayHandle st = displayBuilder.spawnBlock(center.clone().add(0, cy + 2.5, -1.5), Material.NETHERITE_BLOCK);
            st.scale(0f, 0f, 0f).glow(80, 80, 100).interpolation(10, 7);
            spawnedEntities.add(st.entity());
            stinger.add(st);
            setScale(st.entity(), 0.12f, 0.28f, 0.12f, 12);
            // 4 carapace plates on body
            for (int i = 0; i < 4; i++) {
                double zOff = 0.3 - i * 0.4;
                Location loc = center.clone().add(0, cy + 0.25, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(60, 60, 80).interpolation(10, 1);
                spawnedEntities.add(h.entity());
                carapace.add(h);
                setScale(h.entity(), 0.4f, 0.06f, 0.4f, 12);
            }
            // 8 underbelly chain segments
            for (int i = 0; i < 8; i++) {
                double zOff = 0.4 - i * 0.2;
                Location loc = center.clone().add(0, cy - 0.22, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(170, 170, 185).interpolation(8, 1);
                spawnedEntities.add(h.entity());
                underbelly.add(h);
                setScale(h.entity(), 0.3f, 0.04f, 0.16f, 10);
            }
            // 8 dorsal spines along body
            for (int i = 0; i < 8; i++) {
                double zOff = 0.35 - i * 0.2;
                Location loc = center.clone().add(0, cy + 0.4, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(225, 225, 235).interpolation(10, 2);
                spawnedEntities.add(h.entity());
                spines.add(h);
                setScale(h.entity(), 0.08f, 0.18f, 0.08f, 12);
            }
            // 8 chitin spikes on sides
            for (int i = 0; i < 8; i++) {
                int side = i % 2;
                double sx = (side == 0) ? -0.4 : 0.4;
                double zOff = 0.3 - (i / 2) * 0.4;
                Location loc = center.clone().add(sx, cy + 0.05, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(70, 70, 95).interpolation(10, 2);
                spawnedEntities.add(h.entity());
                chitin.add(h);
                setScale(h.entity(), 0.08f, 0.12f, 0.08f, 12);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 1.2f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.9f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.3f, 0.5f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, cy, 0), 30, 1.5, 0.5, 1.5, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double cy = 0.6;
            int dur = config.getDurationTicks();

            pincerAngle = (float) Math.toRadians(15.0) * (float) Math.sin(tick * 0.18);

            // Place body parts at center
            for (int i = 0; i < body.size(); i++) {
                BlockDisplay e = body.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(c.clone().add(0, cy, -i * 0.55)); } catch (Throwable ignored) {}
            }
            for (int i = 0; i < head.size(); i++) {
                BlockDisplay e = head.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(c.clone().add(0, cy, 0.55)); } catch (Throwable ignored) {}
            }
            for (int i = 0; i < eyes.size(); i++) {
                BlockDisplay e = eyes.get(i).entity();
                if (e == null || !e.isValid()) continue;
                double px = (i == 0) ? -0.12 : 0.12;
                try { e.teleport(c.clone().add(px, cy + 0.18, 0.7)); } catch (Throwable ignored) {}
            }
            // Pincers — open/close
            for (int i = 0; i < pincers.size(); i++) {
                int side = i / 2;
                int half = i % 2;
                double sx = (side == 0) ? -0.55 : 0.55;
                double hyOff = (half == 0) ? 0.05 : -0.05;
                BlockDisplay e = pincers.get(i).entity();
                if (e == null || !e.isValid()) continue;
                double extraX = (half == 0 ? -1 : 1) * Math.abs(Math.sin(pincerAngle)) * 0.05;
                try { e.teleport(c.clone().add(sx + extraX, cy + hyOff, 0.85)); } catch (Throwable ignored) {}
                float pa = (half == 0 ? -1 : 1) * pincerAngle;
                rotateOn(e, pa, 0f, 1f, 0f, 4);
            }
            // Legs (static relative offsets)
            int legIdx = 0;
            for (int side = 0; side < 2; side++) {
                double sx = (side == 0) ? -0.45 : 0.45;
                for (int leg = 0; leg < 3; leg++) {
                    double zOff = 0.15 - leg * 0.45;
                    BlockDisplay e = legs.get(legIdx).entity();
                    if (e != null && e.isValid()) {
                        double bob = Math.sin(tick * 0.4 + legIdx) * 0.05;
                        try { e.teleport(c.clone().add(sx, cy - 0.15 + bob, zOff)); } catch (Throwable ignored) {}
                    }
                    legIdx++;
                }
            }
            for (int i = 0; i < carapace.size(); i++) {
                BlockDisplay e = carapace.get(i).entity();
                if (e == null || !e.isValid()) continue;
                double zOff = 0.3 - i * 0.4;
                try { e.teleport(c.clone().add(0, cy + 0.25, zOff)); } catch (Throwable ignored) {}
            }
            for (int i = 0; i < underbelly.size(); i++) {
                BlockDisplay e = underbelly.get(i).entity();
                if (e == null || !e.isValid()) continue;
                double zOff = 0.4 - i * 0.2;
                try { e.teleport(c.clone().add(0, cy - 0.22, zOff)); } catch (Throwable ignored) {}
            }
            for (int i = 0; i < spines.size(); i++) {
                BlockDisplay e = spines.get(i).entity();
                if (e == null || !e.isValid()) continue;
                double zOff = 0.35 - i * 0.2;
                try { e.teleport(c.clone().add(0, cy + 0.4, zOff)); } catch (Throwable ignored) {}
            }
            for (int i = 0; i < chitin.size(); i++) {
                BlockDisplay e = chitin.get(i).entity();
                if (e == null || !e.isValid()) continue;
                int side = i % 2;
                double sx = (side == 0) ? -0.4 : 0.4;
                double zOff = 0.3 - (i / 2) * 0.4;
                try { e.teleport(c.clone().add(sx, cy + 0.05, zOff)); } catch (Throwable ignored) {}
            }

            // Tail / stinger strike cycle. period 40 ticks.
            // Idle pos: above body curling back. Strike pos: forward and down (z+0.5, y close to ground).
            int cycle = tick % 40;
            double strikeT = 0.0;
            if (cycle < 12) {
                strikeT = 0; // idle (windup pause)
            } else if (cycle < 24) {
                strikeT = (cycle - 12) / 12.0; // strike 0->1
            } else if (cycle < 35) {
                strikeT = 1.0 - (cycle - 24) / 11.0; // reset 1->0
            } else {
                strikeT = 0;
            }
            // Strike animation: lerp tail from arching-back to extending-forward
            for (int i = 0; i < tail.size(); i++) {
                double yIdle = 0.4 + i * 0.42;
                double zIdle = -1.7 - i * 0.28 + (i >= 3 ? (i - 2) * 0.42 : 0);
                double yStrike = 0.3 + i * 0.05;
                double zStrike = 0.4 + i * 0.42;
                double y = yIdle + (yStrike - yIdle) * strikeT;
                double z = zIdle + (zStrike - zIdle) * strikeT;
                BlockDisplay e = tail.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(c.clone().add(0, cy + y, z)); } catch (Throwable ignored) {}
            }
            // Stinger at last tail position + offset
            if (!stinger.isEmpty()) {
                double yIdle = 2.5;
                double zIdle = -1.5;
                double yStrike = 0.3;
                double zStrike = 2.5;
                double y = yIdle + (yStrike - yIdle) * strikeT;
                double z = zIdle + (zStrike - zIdle) * strikeT;
                BlockDisplay e = stinger.get(0).entity();
                if (e != null && e.isValid()) {
                    try { e.teleport(c.clone().add(0, cy + y, z)); } catch (Throwable ignored) {}
                }
                // Impact at peak strike
                if (cycle == 23) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.4f, 0.7f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.4f);
                    if (e != null && e.isValid()) {
                        c.getWorld().spawnParticle(Particle.BLOCK, e.getLocation(), 12, 0.4, 0.3, 0.4, 0.08, Material.NETHERITE_BLOCK.createBlockData());
                        c.getWorld().spawnParticle(Particle.EXPLOSION, e.getLocation(), 2, 0.2, 0.2, 0.2, 0.05);
                    }
                    strikeCounter++;
                }
            }

            // Step/movement sounds
            if (tick > 0 && tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.6f, 0.8f);
            }
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, cy, 0), 2, 0.5, 0.2, 0.5, 0.01);
            }

            // Dissipate
            if (tick == dur - 25) {
                for (BlockDisplayHandle h : tail) shrinkToZero(h, 14);
                for (BlockDisplayHandle h : stinger) shrinkToZero(h, 14);
                for (BlockDisplayHandle h : pincers) shrinkToZero(h, 14);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.3f, 0.5f);
            }
            if (tick == dur - 12) {
                for (BlockDisplayHandle h : body) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : head) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : eyes) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : legs) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : carapace) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : underbelly) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : spines) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : chitin) shrinkToZero(h, 10);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, cy, 0), 30, 1.5, 0.5, 1.5, 0.04);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainScorpion(plugin); }
    }

    // ================================================================
    // #67 — CHAIN PULLEY SYSTEM ("The Machine")
    // Two wheels at Y=5, descending weight on left chain + rising counterweight
    // on right chain. Weight cycles ground impact every 80 ticks.
    // 52 blocks: 2 wheels × (8 rim + 4 spokes = 12) = 24
    //          + 2 DARK_OAK posts (2 each = 4 segs) = 4
    //          + 2 chain columns × 8 links = 16
    //          + 1 weight + 1 counterweight + 4 hub bolts + 2 axles = 8.
    // ================================================================
    public static class ChainPulleySystem extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wheelA = new ArrayList<>(); // 8 rim + 4 spokes
        private final List<BlockDisplayHandle> wheelB = new ArrayList<>();
        private final List<BlockDisplayHandle> posts = new ArrayList<>(); // 4 (2 stacked per side)
        private final List<BlockDisplayHandle> chainsL = new ArrayList<>(); // 8 links
        private final List<BlockDisplayHandle> chainsR = new ArrayList<>(); // 8 links
        private final List<BlockDisplayHandle> weight = new ArrayList<>();
        private final List<BlockDisplayHandle> counter = new ArrayList<>();
        private final List<BlockDisplayHandle> bolts = new ArrayList<>(); // 4
        private final List<BlockDisplayHandle> axles = new ArrayList<>(); // 2
        private float wheelAngle = 0f;

        public ChainPulleySystem(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_pulley_system", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(300);
            config.setCooldownTicks(320);
            config.setChance(4);
            config.setEnabled(true);
            config.setDesignType("Mechanism (read timing pattern)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double topY = 5.0;
            double leftX = -1.0;
            double rightX = 1.0;

            // 2 posts (left and right). Each: 2 stacked log segments.
            for (int side = 0; side < 2; side++) {
                double px = (side == 0) ? leftX : rightX;
                for (int s = 0; s < 2; s++) {
                    double py = 1.1 + s * 2.25;
                    Location loc = center.clone().add(px, py, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_OAK_LOG);
                    h.scale(0f, 0f, 0f).glow(80, 60, 40).interpolation(12, 0);
                    spawnedEntities.add(h.entity());
                    posts.add(h);
                    setScale(h.entity(), 0.18f, 2.2f, 0.18f, 14);
                }
            }
            // 2 axles connecting the two wheel hubs (visible cross-bars)
            for (int s = 0; s < 2; s++) {
                double ay = topY + (s == 0 ? 0.0 : 0.0);
                Location loc = center.clone().add(0, ay - (s == 0 ? 0 : 0.4), 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_OAK_LOG);
                h.scale(0f, 0f, 0f).glow(70, 50, 30).interpolation(12, 0);
                spawnedEntities.add(h.entity());
                axles.add(h);
                setScale(h.entity(), 2.0f, 0.1f, 0.1f, 14);
            }

            // Wheel A (left): 8 rim slabs + 4 spokes
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2.0 * i) / 8.0;
                double x = leftX + Math.cos(a) * 0.5;
                double z = Math.sin(a) * 0.5;
                Location loc = center.clone().add(x, topY, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(12, 1);
                spawnedEntities.add(h.entity());
                wheelA.add(h);
                setScale(h.entity(), 0.16f, 0.16f, 0.16f, 14);
            }
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2.0 * i) / 4.0;
                double x = leftX + Math.cos(a) * 0.25;
                double z = Math.sin(a) * 0.25;
                Location loc = center.clone().add(x, topY, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(12, 1);
                spawnedEntities.add(h.entity());
                wheelA.add(h);
                setScale(h.entity(), 0.1f, 0.1f, 0.5f, 14);
                rotateOn(h.entity(), (float) a, 0f, 0f, 1f, 14);
            }
            // Wheel B (right): same construction
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2.0 * i) / 8.0;
                double x = rightX + Math.cos(a) * 0.5;
                double z = Math.sin(a) * 0.5;
                Location loc = center.clone().add(x, topY, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(12, 1);
                spawnedEntities.add(h.entity());
                wheelB.add(h);
                setScale(h.entity(), 0.16f, 0.16f, 0.16f, 14);
            }
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2.0 * i) / 4.0;
                double x = rightX + Math.cos(a) * 0.25;
                double z = Math.sin(a) * 0.25;
                Location loc = center.clone().add(x, topY, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(12, 1);
                spawnedEntities.add(h.entity());
                wheelB.add(h);
                setScale(h.entity(), 0.1f, 0.1f, 0.5f, 14);
                rotateOn(h.entity(), (float) a, 0f, 0f, 1f, 14);
            }
            // 4 hub bolts (NETHERITE_BLOCK) — 2 per wheel center
            for (int side = 0; side < 2; side++) {
                double px = (side == 0) ? leftX : rightX;
                for (int b = 0; b < 2; b++) {
                    Location loc = center.clone().add(px, topY, (b == 0 ? -0.1 : 0.1));
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                    h.scale(0f, 0f, 0f).glow(60, 60, 80).interpolation(12, 2);
                    spawnedEntities.add(h.entity());
                    bolts.add(h);
                    setScale(h.entity(), 0.16f, 0.16f, 0.16f, 14);
                }
            }

            // Chain columns
            for (int i = 0; i < 8; i++) {
                double cyLink = topY - 0.4 - i * 0.5;
                Location locL = center.clone().add(leftX, cyLink, 0);
                BlockDisplayHandle hl = displayBuilder.spawnBlock(locL, Material.CHAIN);
                hl.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(10, 2);
                spawnedEntities.add(hl.entity());
                chainsL.add(hl);
                setScale(hl.entity(), 0.16f, 0.4f, 0.16f, 12);

                Location locR = center.clone().add(rightX, cyLink, 0);
                BlockDisplayHandle hr = displayBuilder.spawnBlock(locR, Material.CHAIN);
                hr.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(10, 2);
                spawnedEntities.add(hr.entity());
                chainsR.add(hr);
                setScale(hr.entity(), 0.16f, 0.4f, 0.16f, 12);
            }

            // Weight (NETHERITE_BLOCK) — on left chain
            BlockDisplayHandle wt = displayBuilder.spawnBlock(center.clone().add(leftX, topY - 0.8, 0), Material.NETHERITE_BLOCK);
            wt.scale(0f, 0f, 0f).glow(70, 70, 90).interpolation(12, 3);
            spawnedEntities.add(wt.entity());
            weight.add(wt);
            setScale(wt.entity(), 0.85f, 0.85f, 0.85f, 14);

            // Counterweight (IRON_BLOCK) — on right chain (starts near ground)
            BlockDisplayHandle cw = displayBuilder.spawnBlock(center.clone().add(rightX, 0.5, 0), Material.IRON_BLOCK);
            cw.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(12, 3);
            spawnedEntities.add(cw.entity());
            counter.add(cw);
            setScale(cw.entity(), 0.55f, 0.55f, 0.55f, 14);

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.9f, 0.6f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, topY, 0), 25, 1.5, 0.5, 1.5, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            int dur = config.getDurationTicks();
            double topY = 5.0;
            double leftX = -1.0;
            double rightX = 1.0;

            // Weight position cycle (80-tick period): descend 80 ticks, stay impact 5, rise 80, etc.
            // Use a 160-tick periodic cycle: 0-80 descend, 80-160 rise.
            int cycle = tick % 160;
            double weightY; // 0=top, 1=ground
            if (cycle < 80) {
                weightY = topY - 0.8 - (cycle / 80.0) * (topY - 1.6); // top to near-ground
            } else {
                weightY = topY - 0.8 - ((160 - cycle) / 80.0) * (topY - 1.6);
            }
            double counterY = (topY - 0.8) - (weightY - 0.5); // mirror (counterweight rises as weight falls)

            // Apply weight + counterweight teleports
            if (!weight.isEmpty()) {
                BlockDisplay e = weight.get(0).entity();
                if (e != null && e.isValid()) {
                    try { e.teleport(c.clone().add(leftX, weightY, 0)); } catch (Throwable ignored) {}
                }
            }
            if (!counter.isEmpty()) {
                BlockDisplay e = counter.get(0).entity();
                if (e != null && e.isValid()) {
                    try { e.teleport(c.clone().add(rightX, counterY, 0)); } catch (Throwable ignored) {}
                }
            }

            // Chain links: distribute 8 links from top of wheel down to weight/counter
            for (int i = 0; i < chainsL.size(); i++) {
                double tFrac = (i + 1) / 9.0;
                double yLink = topY - 0.4 - tFrac * (topY - weightY - 0.7);
                BlockDisplay e = chainsL.get(i).entity();
                if (e != null && e.isValid()) {
                    try { e.teleport(c.clone().add(leftX, yLink, 0)); } catch (Throwable ignored) {}
                }
            }
            for (int i = 0; i < chainsR.size(); i++) {
                double tFrac = (i + 1) / 9.0;
                double yLink = topY - 0.4 - tFrac * (topY - counterY - 0.5);
                BlockDisplay e = chainsR.get(i).entity();
                if (e != null && e.isValid()) {
                    try { e.teleport(c.clone().add(rightX, yLink, 0)); } catch (Throwable ignored) {}
                }
            }

            // Spin wheels in opposite directions
            if (tick > 25) {
                wheelAngle += (float) Math.toRadians(8.0);
                // Wheel A
                for (int i = 0; i < 8; i++) {
                    double a = (Math.PI * 2.0 * i) / 8.0 + wheelAngle;
                    double x = leftX + Math.cos(a) * 0.5;
                    double z = Math.sin(a) * 0.5;
                    BlockDisplay e = wheelA.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x, topY, z)); } catch (Throwable ignored) {}
                }
                for (int i = 0; i < 4; i++) {
                    double a = (Math.PI * 2.0 * i) / 4.0 + wheelAngle;
                    double x = leftX + Math.cos(a) * 0.25;
                    double z = Math.sin(a) * 0.25;
                    BlockDisplay e = wheelA.get(8 + i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x, topY, z)); } catch (Throwable ignored) {}
                    rotateOn(e, (float) a, 0f, 0f, 1f, 4);
                }
                // Wheel B (opposite direction)
                for (int i = 0; i < 8; i++) {
                    double a = (Math.PI * 2.0 * i) / 8.0 - wheelAngle;
                    double x = rightX + Math.cos(a) * 0.5;
                    double z = Math.sin(a) * 0.5;
                    BlockDisplay e = wheelB.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x, topY, z)); } catch (Throwable ignored) {}
                }
                for (int i = 0; i < 4; i++) {
                    double a = (Math.PI * 2.0 * i) / 4.0 - wheelAngle;
                    double x = rightX + Math.cos(a) * 0.25;
                    double z = Math.sin(a) * 0.25;
                    BlockDisplay e = wheelB.get(8 + i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x, topY, z)); } catch (Throwable ignored) {}
                    rotateOn(e, (float) a, 0f, 0f, 1f, 4);
                }
            }

            // Impact when weight hits ground (cycle ~80)
            if (cycle == 78) {
                DisplayBuilder.playSound(c.clone().add(leftX, 0, 0), Sound.BLOCK_ANVIL_LAND, 1.6f, 0.5f);
                DisplayBuilder.playSound(c.clone().add(leftX, 0, 0), Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.4f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(leftX, 0.5, 0), 4, 0.6, 0.2, 0.6, 0.04);
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(leftX, 0.5, 0), 18, 0.8, 0.2, 0.8, 0.05, Material.IRON_BLOCK.createBlockData());
            }
            // Falling dust trail
            if (tick > 25 && tick % 4 == 0) {
                if (!weight.isEmpty()) {
                    c.getWorld().spawnParticle(Particle.BLOCK, weight.get(0).entity().getLocation(), 3, 0.3, 0.05, 0.3, 0.02, Material.IRON_BLOCK.createBlockData());
                }
            }
            // Continuous chain rattle
            if (tick > 25 && tick % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.7f);
            }
            if (tick > 25 && tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.9f);
            }

            // Dissipate
            if (tick == dur - 22) {
                for (BlockDisplayHandle h : weight) shrinkToZero(h, 14);
                for (BlockDisplayHandle h : counter) shrinkToZero(h, 14);
                for (BlockDisplayHandle h : chainsL) shrinkToZero(h, 14);
                for (BlockDisplayHandle h : chainsR) shrinkToZero(h, 14);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.4f);
            }
            if (tick == dur - 10) {
                for (BlockDisplayHandle h : wheelA) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : wheelB) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : posts) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : bolts) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : axles) shrinkToZero(h, 8);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, topY * 0.5, 0), 40, 2.0, 2.0, 1.0, 0.03);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainPulleySystem(plugin); }
    }

    // ================================================================
    // #68 — IRON GRINDER ("The Mill")
    // Twin counter-rotating wheels with a narrow grinding zone between.
    // 50 blocks: 2 wheels × (12 rim slabs + 6 spokes + 1 hub = 19) = 38
    //          + 2 axles + 4×2 housing brackets = 10
    //          + 2 base supports = 2.
    // ================================================================
    public static class IronGrinder extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wheelA = new ArrayList<>(); // 12 rim + 6 spokes
        private final List<BlockDisplayHandle> wheelB = new ArrayList<>();
        private final List<BlockDisplayHandle> hubs = new ArrayList<>();
        private final List<BlockDisplayHandle> axles = new ArrayList<>();
        private final List<BlockDisplayHandle> housingA = new ArrayList<>();
        private final List<BlockDisplayHandle> housingB = new ArrayList<>();
        private final List<BlockDisplayHandle> bases = new ArrayList<>();
        private float wheelAngle = 0f;
        private final double wheelR = 1.6;
        private final double wheelGap = 2.2;

        public IronGrinder(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_grinder", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(380.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Multi-wheel pass (sprint between gaps)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            double cy = 2.0;
            double leftX = -wheelGap / 2.0;
            double rightX = wheelGap / 2.0;

            // Wheel A (12 rim + 6 spokes)
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12.0;
                double y = cy + Math.sin(a) * wheelR;
                double z = Math.cos(a) * wheelR;
                Location loc = center.clone().add(leftX, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(12, 0);
                spawnedEntities.add(h.entity());
                wheelA.add(h);
                setScale(h.entity(), 0.1f, 0.65f, 0.2f, 14);
                rotateOn(h.entity(), (float) a, 1f, 0f, 0f, 14);
            }
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2.0 * i) / 6.0;
                double y = cy + Math.sin(a) * (wheelR / 2.0);
                double z = Math.cos(a) * (wheelR / 2.0);
                Location loc = center.clone().add(leftX, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(12, 1);
                spawnedEntities.add(h.entity());
                wheelA.add(h);
                setScale(h.entity(), 0.08f, 0.08f, 1.5f, 14);
                rotateOn(h.entity(), (float) a, 1f, 0f, 0f, 14);
            }
            // Wheel B
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12.0;
                double y = cy + Math.sin(a) * wheelR;
                double z = Math.cos(a) * wheelR;
                Location loc = center.clone().add(rightX, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(12, 0);
                spawnedEntities.add(h.entity());
                wheelB.add(h);
                setScale(h.entity(), 0.1f, 0.65f, 0.2f, 14);
                rotateOn(h.entity(), (float) a, 1f, 0f, 0f, 14);
            }
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2.0 * i) / 6.0;
                double y = cy + Math.sin(a) * (wheelR / 2.0);
                double z = Math.cos(a) * (wheelR / 2.0);
                Location loc = center.clone().add(rightX, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(12, 1);
                spawnedEntities.add(h.entity());
                wheelB.add(h);
                setScale(h.entity(), 0.08f, 0.08f, 1.5f, 14);
                rotateOn(h.entity(), (float) a, 1f, 0f, 0f, 14);
            }
            // Hubs (1 each, NETHERITE_BLOCK)
            for (int side = 0; side < 2; side++) {
                double px = (side == 0) ? leftX : rightX;
                Location loc = center.clone().add(px, cy, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(70, 70, 90).interpolation(12, 2);
                spawnedEntities.add(h.entity());
                hubs.add(h);
                setScale(h.entity(), 0.3f, 0.3f, 0.3f, 14);
            }
            // 2 axles (DARK_OAK_LOG) connecting hubs
            for (int side = 0; side < 2; side++) {
                double yOff = (side == 0) ? 0.0 : 0.0;
                double zOff = (side == 0) ? -0.2 : 0.2;
                Location loc = center.clone().add(0, cy + yOff, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_OAK_LOG);
                h.scale(0f, 0f, 0f).glow(70, 50, 30).interpolation(12, 2);
                spawnedEntities.add(h.entity());
                axles.add(h);
                setScale(h.entity(), 1.1f, 0.1f, 0.1f, 14);
            }
            // Housing brackets — 4 per wheel
            double[][] housingOffs = {{0, wheelR + 0.2, 0}, {0, -(wheelR + 0.2), 0}, {0, 0, wheelR + 0.2}, {0, 0, -(wheelR + 0.2)}};
            for (double[] off : housingOffs) {
                Location loc = center.clone().add(leftX + off[0], cy + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(12, 3);
                spawnedEntities.add(h.entity());
                housingA.add(h);
                setScale(h.entity(), 0.35f, 0.15f, 0.35f, 14);
            }
            for (double[] off : housingOffs) {
                Location loc = center.clone().add(rightX + off[0], cy + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(12, 3);
                spawnedEntities.add(h.entity());
                housingB.add(h);
                setScale(h.entity(), 0.35f, 0.15f, 0.35f, 14);
            }
            // 2 base supports
            for (int side = 0; side < 2; side++) {
                double px = (side == 0) ? leftX : rightX;
                Location loc = center.clone().add(px, 0.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0f, 0f, 0f).glow(60, 60, 70).interpolation(12, 3);
                spawnedEntities.add(h.entity());
                bases.add(h);
                setScale(h.entity(), 0.85f, 0.25f, 1.5f, 14);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.6f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.2f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.1f, 0.5f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, cy, 0), 30, 2.0, 1.0, 1.0, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            int dur = config.getDurationTicks();
            double cy = 2.0;
            double leftX = -wheelGap / 2.0;
            double rightX = wheelGap / 2.0;

            if (tick > 25) {
                wheelAngle += (float) Math.toRadians(10.0);
                // Wheel A clockwise
                for (int i = 0; i < 12; i++) {
                    double a = (Math.PI * 2.0 * i) / 12.0 + wheelAngle;
                    double y = cy + Math.sin(a) * wheelR;
                    double z = Math.cos(a) * wheelR;
                    BlockDisplay e = wheelA.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(leftX, y, z)); } catch (Throwable ignored) {}
                    rotateOn(e, (float) a, 1f, 0f, 0f, 4);
                }
                for (int i = 0; i < 6; i++) {
                    double a = (Math.PI * 2.0 * i) / 6.0 + wheelAngle;
                    double y = cy + Math.sin(a) * (wheelR / 2.0);
                    double z = Math.cos(a) * (wheelR / 2.0);
                    BlockDisplay e = wheelA.get(12 + i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(leftX, y, z)); } catch (Throwable ignored) {}
                    rotateOn(e, (float) a, 1f, 0f, 0f, 4);
                }
                // Wheel B counter
                for (int i = 0; i < 12; i++) {
                    double a = (Math.PI * 2.0 * i) / 12.0 - wheelAngle;
                    double y = cy + Math.sin(a) * wheelR;
                    double z = Math.cos(a) * wheelR;
                    BlockDisplay e = wheelB.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(rightX, y, z)); } catch (Throwable ignored) {}
                    rotateOn(e, (float) a, 1f, 0f, 0f, 4);
                }
                for (int i = 0; i < 6; i++) {
                    double a = (Math.PI * 2.0 * i) / 6.0 - wheelAngle;
                    double y = cy + Math.sin(a) * (wheelR / 2.0);
                    double z = Math.cos(a) * (wheelR / 2.0);
                    BlockDisplay e = wheelB.get(12 + i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(rightX, y, z)); } catch (Throwable ignored) {}
                    rotateOn(e, (float) a, 1f, 0f, 0f, 4);
                }
            }

            // CRIT sparks flying from grinding faces (between wheels)
            if (tick > 25 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, cy, 0), 12, 0.4, 1.0, 0.6, 0.4);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, cy, 0), 8, 0.3, 1.0, 0.3, 0.3);
            }
            // LARGE_SMOKE between wheels (constant grinding smoke)
            if (tick > 25 && tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, cy, 0), 4, 0.4, 0.6, 0.3, 0.02);
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, cy - 0.5, 0), 5, 0.6, 0.4, 0.4, 0.02);
            }
            // Lava sparks for menacing effect
            if (tick > 25 && tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.LAVA, c.clone().add(0, cy, 0), 1, 0.3, 0.3, 0.3, 0);
            }
            // Grindstone loop
            if (tick > 25 && tick % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.9f, 1.0f);
            }
            if (tick > 25 && tick % 18 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 0.7f, 0.5f);
            }
            if (tick > 25 && tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.7f, 0.5f);
            }

            // Dissipate
            if (tick == dur - 25) {
                for (BlockDisplayHandle h : wheelA) shrinkToZero(h, 14);
                for (BlockDisplayHandle h : wheelB) shrinkToZero(h, 14);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.4f, 0.4f);
            }
            if (tick == dur - 12) {
                for (BlockDisplayHandle h : hubs) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : axles) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : housingA) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : housingB) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : bases) shrinkToZero(h, 10);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, cy, 0), 6, 1.0, 0.5, 1.0, 0.05);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, cy, 0), 40, 2.0, 1.0, 1.0, 0.04);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronGrinder(plugin); }
    }

    // ================================================================
    // #69 — IRON DRAGON ("The Serpent of Iron")
    // Long airborne serpentine dragon: head + horns + neck + body + spine + tail.
    // Body follows 3D sine curve via head-position history. Follow-AI 0.09.
    // 54 blocks: 3 head + 2 horns + 4 neck + 10 body + 10 spine + 4 tail
    //          + 6 wing flaps + 2 jaw teeth + 4 dorsal fins + 9 belly chains.
    // ================================================================
    public static class IronDragon extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> headParts = new ArrayList<>(); // 3 snout
        private final List<BlockDisplayHandle> horns = new ArrayList<>();
        private final List<BlockDisplayHandle> neck = new ArrayList<>();
        private final List<BlockDisplayHandle> bodySeg = new ArrayList<>();
        private final List<BlockDisplayHandle> spineSeg = new ArrayList<>();
        private final List<BlockDisplayHandle> tailSeg = new ArrayList<>();
        private final List<BlockDisplayHandle> wings = new ArrayList<>();
        private final List<BlockDisplayHandle> jawTeeth = new ArrayList<>();
        private final List<BlockDisplayHandle> dorsalFins = new ArrayList<>();
        private final List<BlockDisplayHandle> belly = new ArrayList<>();
        private final List<double[]> history = new ArrayList<>();
        private float bodyPhase = 0f;

        public IronDragon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_dragon", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(420.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(320);
            config.setCooldownTicks(340);
            config.setChance(4);
            config.setEnabled(true);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.09);
            config.setDesignType("Serpentine chase (read path + sprint perpendicular)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            double cy = 2.5; // airborne dragon

            // Head: 3 IRON_BLOCK cubes forming snout
            float[][] headScales = {{0.45f, 0.32f, 0.5f}, {0.4f, 0.32f, 0.42f}, {0.32f, 0.28f, 0.34f}};
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, cy, 0.6 + i * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(225, 225, 235).interpolation(12, i);
                spawnedEntities.add(h.entity());
                headParts.add(h);
                setScale(h.entity(), headScales[i][0], headScales[i][1], headScales[i][2], 14);
            }
            // 2 horns (NETHERITE_BLOCK)
            for (int i = 0; i < 2; i++) {
                double px = (i == 0) ? -0.18 : 0.18;
                Location loc = center.clone().add(px, cy + 0.35, 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(80, 80, 100).interpolation(12, 2);
                spawnedEntities.add(h.entity());
                horns.add(h);
                setScale(h.entity(), 0.06f, 0.3f, 0.06f, 14);
                rotateOn(h.entity(), (float) Math.toRadians(-25), 1f, 0f, 0f, 14);
            }
            // 4 neck segments
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, cy, -0.05 - i * 0.42);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(215, 215, 225).interpolation(12, 3 + i);
                spawnedEntities.add(h.entity());
                neck.add(h);
                setScale(h.entity(), 0.38f, 0.3f, 0.42f, 14);
            }
            // 10 body segments + 10 spine pieces
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(0, cy, -1.8 - i * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(10, 5 + i);
                spawnedEntities.add(h.entity());
                bodySeg.add(h);
                setScale(h.entity(), 0.45f, 0.32f, 0.48f, 12);

                Location sloc = center.clone().add(0, cy + 0.25, -1.8 - i * 0.5);
                BlockDisplayHandle s = displayBuilder.spawnBlock(sloc, Material.CHAIN);
                s.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(10, 5 + i);
                spawnedEntities.add(s.entity());
                spineSeg.add(s);
                setScale(s.entity(), 0.12f, 0.15f, 0.04f, 12);
            }
            // 4 tail narrowing
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, cy, -7.0 - i * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(195, 195, 205).interpolation(10, 15 + i);
                spawnedEntities.add(h.entity());
                tailSeg.add(h);
                float s = 0.32f - i * 0.06f;
                setScale(h.entity(), s, s * 0.7f, s * 1.2f, 12);
            }
            // 6 wing flaps (3 per side) — IRON_BLOCK flat slabs at body mid
            for (int side = 0; side < 2; side++) {
                double sx = (side == 0) ? -0.6 : 0.6;
                for (int w2 = 0; w2 < 3; w2++) {
                    double zOff = -2.3 - w2 * 0.4;
                    Location loc = center.clone().add(sx, cy + 0.1, zOff);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(10, 5);
                    spawnedEntities.add(h.entity());
                    wings.add(h);
                    setScale(h.entity(), 0.6f, 0.04f, 0.35f, 12);
                    rotateOn(h.entity(), (float) Math.toRadians(side == 0 ? 30 : -30), 0f, 0f, 1f, 12);
                }
            }
            // 2 jaw teeth (NETHERITE_BLOCK)
            for (int i = 0; i < 2; i++) {
                double px = (i == 0) ? -0.1 : 0.1;
                Location loc = center.clone().add(px, cy - 0.15, 1.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(255, 60, 30).interpolation(12, 2);
                spawnedEntities.add(h.entity());
                jawTeeth.add(h);
                setScale(h.entity(), 0.07f, 0.15f, 0.07f, 14);
            }
            // 4 dorsal fins (larger spine pieces)
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, cy + 0.5, -2.5 - i * 1.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(70, 70, 90).interpolation(10, 6);
                spawnedEntities.add(h.entity());
                dorsalFins.add(h);
                setScale(h.entity(), 0.1f, 0.45f, 0.18f, 12);
            }
            // 9 belly chain segments
            for (int i = 0; i < 9; i++) {
                Location loc = center.clone().add(0, cy - 0.3, -1.8 - i * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(10, 7);
                spawnedEntities.add(h.entity());
                belly.add(h);
                setScale(h.entity(), 0.32f, 0.05f, 0.12f, 12);
            }

            for (int i = 0; i < 80; i++) {
                history.add(new double[]{center.getX(), center.getY() + cy, center.getZ()});
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.6f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.5f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, cy, 0), 40, 2.0, 1.0, 4.0, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double cy = 2.5;
            int dur = config.getDurationTicks();

            bodyPhase += 0.1f;
            // Head bob: head Y oscillates with sine wave for 3D coil
            double headBob = Math.sin(bodyPhase) * 0.4;
            history.add(new double[]{c.getX(), c.getY() + cy + headBob, c.getZ()});
            if (history.size() > 250) history.remove(0);

            // Place head and immediate parts
            for (int i = 0; i < headParts.size(); i++) {
                BlockDisplay e = headParts.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(c.clone().add(0, cy + headBob, 0.6 + i * 0.4)); } catch (Throwable ignored) {}
            }
            for (int i = 0; i < horns.size(); i++) {
                BlockDisplay e = horns.get(i).entity();
                if (e == null || !e.isValid()) continue;
                double px = (i == 0) ? -0.18 : 0.18;
                try { e.teleport(c.clone().add(px, cy + headBob + 0.35, 0.5)); } catch (Throwable ignored) {}
            }
            for (int i = 0; i < jawTeeth.size(); i++) {
                BlockDisplay e = jawTeeth.get(i).entity();
                if (e == null || !e.isValid()) continue;
                double px = (i == 0) ? -0.1 : 0.1;
                try { e.teleport(c.clone().add(px, cy + headBob - 0.15, 1.4)); } catch (Throwable ignored) {}
            }
            // Neck lag
            for (int i = 0; i < neck.size(); i++) {
                int back = (i + 1) * 2;
                int idx = history.size() - 1 - back;
                if (idx < 0) idx = 0;
                double[] pos = history.get(idx);
                double sideOff = Math.sin(bodyPhase + i * 0.5) * 0.3;
                BlockDisplay e = neck.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(new Location(c.getWorld(), pos[0] + sideOff, pos[1], pos[2])); } catch (Throwable ignored) {}
            }
            // Body, spine, belly
            for (int i = 0; i < bodySeg.size(); i++) {
                int back = (neck.size() + i + 1) * 2;
                int idx = history.size() - 1 - back;
                if (idx < 0) idx = 0;
                double[] pos = history.get(idx);
                double sideOff = Math.sin(bodyPhase + (neck.size() + i) * 0.5) * 0.5;
                double yWave = Math.sin(bodyPhase * 0.8 + i * 0.3) * 0.3;
                BlockDisplay e = bodySeg.get(i).entity();
                if (e != null && e.isValid()) {
                    try { e.teleport(new Location(c.getWorld(), pos[0] + sideOff, pos[1] + yWave, pos[2])); } catch (Throwable ignored) {}
                }
                BlockDisplay s = spineSeg.get(i).entity();
                if (s != null && s.isValid()) {
                    try { s.teleport(new Location(c.getWorld(), pos[0] + sideOff, pos[1] + yWave + 0.25, pos[2])); } catch (Throwable ignored) {}
                }
                if (i < belly.size()) {
                    BlockDisplay b = belly.get(i).entity();
                    if (b != null && b.isValid()) {
                        try { b.teleport(new Location(c.getWorld(), pos[0] + sideOff, pos[1] + yWave - 0.3, pos[2])); } catch (Throwable ignored) {}
                    }
                }
            }
            // Tail
            for (int i = 0; i < tailSeg.size(); i++) {
                int back = (neck.size() + bodySeg.size() + i + 1) * 2;
                int idx = history.size() - 1 - back;
                if (idx < 0) idx = 0;
                double[] pos = history.get(idx);
                double sideOff = Math.sin(bodyPhase + (neck.size() + bodySeg.size() + i) * 0.5) * 0.6;
                double yWave = Math.sin(bodyPhase * 0.8 + (bodySeg.size() + i) * 0.3) * 0.4;
                BlockDisplay e = tailSeg.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(new Location(c.getWorld(), pos[0] + sideOff, pos[1] + yWave, pos[2])); } catch (Throwable ignored) {}
            }
            // Wings flap
            double wingFlap = Math.sin(bodyPhase * 1.5) * 0.4;
            for (int i = 0; i < wings.size(); i++) {
                int side = i / 3;
                int wingIdx = i % 3;
                double sx = (side == 0) ? -0.6 : 0.6;
                int back = (neck.size() + 2) * 2;
                int idx = history.size() - 1 - back;
                if (idx < 0) idx = 0;
                double[] pos = history.get(idx);
                BlockDisplay e = wings.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(new Location(c.getWorld(), pos[0] + sx, pos[1] + 0.1 + wingFlap * (side == 0 ? 1 : -1), pos[2] - wingIdx * 0.4)); } catch (Throwable ignored) {}
                rotateOn(e, (float) (Math.toRadians(side == 0 ? 30 : -30) + wingFlap), 0f, 0f, 1f, 4);
            }
            // Dorsal fins
            for (int i = 0; i < dorsalFins.size(); i++) {
                int back = (neck.size() + 2 + i * 2) * 2;
                int idx = history.size() - 1 - back;
                if (idx < 0) idx = 0;
                double[] pos = history.get(idx);
                double sideOff = Math.sin(bodyPhase + (neck.size() + i * 2) * 0.5) * 0.5;
                double yWave = Math.sin(bodyPhase * 0.8 + i * 0.6) * 0.3;
                BlockDisplay e = dorsalFins.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(new Location(c.getWorld(), pos[0] + sideOff, pos[1] + yWave + 0.5, pos[2])); } catch (Throwable ignored) {}
            }

            // Particle: head smoke, body joint sparks
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, cy + headBob, 1.5), 3, 0.3, 0.2, 0.3, 0.02);
                c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(0, cy + headBob - 0.15, 1.4), 3, 0.1, 0.05, 0.1, 0.02);
            }
            if (tick % 6 == 0) {
                for (int i = 0; i < bodySeg.size(); i += 3) {
                    Location bloc = bodySeg.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.BLOCK, bloc, 2, 0.2, 0.2, 0.2, 0.02, Material.IRON_BLOCK.createBlockData());
                }
            }
            // Sounds
            if (tick > 0 && tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.4f);
            }
            if (tick > 0 && tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 0.6f);
            }
            if (tick > 0 && tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.7f, 0.5f);
            }

            // Dissipate (tail-first)
            if (tick >= dur - 32) {
                int dt = tick - (dur - 32);
                if (dt < tailSeg.size()) {
                    shrinkToZero(tailSeg.get(tailSeg.size() - 1 - dt), 5);
                } else if (dt - tailSeg.size() < bodySeg.size()) {
                    int bi = bodySeg.size() - 1 - (dt - tailSeg.size());
                    shrinkToZero(bodySeg.get(bi), 5);
                    if (bi < spineSeg.size()) shrinkToZero(spineSeg.get(bi), 5);
                    if (bi < belly.size()) shrinkToZero(belly.get(bi), 5);
                    if (bi < dorsalFins.size()) shrinkToZero(dorsalFins.get(bi), 5);
                }
                if (dt == 28) {
                    for (BlockDisplayHandle h : headParts) shrinkToZero(h, 6);
                    for (BlockDisplayHandle h : horns) shrinkToZero(h, 6);
                    for (BlockDisplayHandle h : neck) shrinkToZero(h, 6);
                    for (BlockDisplayHandle h : wings) shrinkToZero(h, 6);
                    for (BlockDisplayHandle h : jawTeeth) shrinkToZero(h, 6);
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.3f);
                    c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, cy, 0), 8, 1.2, 0.5, 1.2, 0.04);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronDragon(plugin); }
    }

    // ================================================================
    // #70 — IRON CHIMNEY ("The Exhaust")
    // Stationary vertical chimney that fires expanding rings out the top.
    // 50+ blocks: 5 chimney rings × 8 = 40 base
    //          + dynamic ring entities (up to 3 active × 12) = up to 36 extra
    //          + 4 base flange + 2 cap details = 6
    //          Static roster: 40 + 6 = 46. Plus ring entities are alloc'd dynamically.
    // ================================================================
    public static class IronChimney extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> chimneyRings = new ArrayList<>(); // 5 rings of 8
        private final List<BlockDisplayHandle> base = new ArrayList<>();
        private final List<BlockDisplayHandle> cap = new ArrayList<>();
        // Active fired rings
        private final List<Ring> activeRings = new ArrayList<>();

        private static class Ring {
            final List<BlockDisplayHandle> slabs;
            int age;
            final int maxAge;
            Ring(List<BlockDisplayHandle> s, int m) { slabs = s; age = 0; maxAge = m; }
        }

        public IronChimney(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_chimney", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(400.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(240);
            config.setCooldownTicks(280);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Vertical eruption (sidestep clear)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 5 chimney rings stacked (8 displays each)
            double[] rs = {0.6, 0.65, 0.7, 0.65, 0.6};
            double[] ys = {0.5, 1.0, 1.5, 2.0, 2.5};
            for (int ri = 0; ri < 5; ri++) {
                List<BlockDisplayHandle> ringSegs = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    double a = (Math.PI * 2.0 * i) / 8.0;
                    double r = rs[ri];
                    double x = Math.cos(a) * r;
                    double z = Math.sin(a) * r;
                    Location loc = center.clone().add(x, ys[ri], z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(12, ri);
                    spawnedEntities.add(h.entity());
                    ringSegs.add(h);
                    setScale(h.entity(), 0.32f, 0.5f, 0.2f, 14);
                    rotateOn(h.entity(), (float) a, 0f, 1f, 0f, 14);
                }
                chimneyRings.add(ringSegs);
            }
            // 4 base flange (POLISHED_BLACKSTONE)
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2.0 * i) / 4.0;
                double x = Math.cos(a) * 0.85;
                double z = Math.sin(a) * 0.85;
                Location loc = center.clone().add(x, 0.15, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0f, 0f, 0f).glow(60, 60, 70).interpolation(12, 0);
                spawnedEntities.add(h.entity());
                base.add(h);
                setScale(h.entity(), 0.35f, 0.2f, 0.35f, 14);
            }
            // 2 cap details on top (NETHERITE accents)
            for (int i = 0; i < 2; i++) {
                double px = (i == 0) ? -0.4 : 0.4;
                Location loc = center.clone().add(px, 2.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(70, 70, 90).interpolation(12, 1);
                spawnedEntities.add(h.entity());
                cap.add(h);
                setScale(h.entity(), 0.16f, 0.16f, 0.5f, 14);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 1.2f, 0.4f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 1.5, 0), 25, 0.8, 1.0, 0.8, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            int dur = config.getDurationTicks();

            // Fire a new ring every 20 ticks after warmup
            if (tick > 25 && tick % 20 == 0 && tick < dur - 35) {
                // Don't exceed 3 active rings
                if (activeRings.size() < 3) {
                    List<BlockDisplayHandle> segs = new ArrayList<>();
                    double startY = 3.0;
                    double startR = 0.5;
                    for (int i = 0; i < 12; i++) {
                        double a = (Math.PI * 2.0 * i) / 12.0;
                        Location loc = c.clone().add(Math.cos(a) * startR, startY, Math.sin(a) * startR);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                        h.scale(0f, 0f, 0f).glow(255, 130, 50).interpolation(6, 0);
                        spawnedEntities.add(h.entity());
                        segs.add(h);
                        setScale(h.entity(), 0.28f, 0.18f, 0.18f, 8);
                        rotateOn(h.entity(), (float) a, 0f, 1f, 0f, 8);
                    }
                    activeRings.add(new Ring(segs, 16));
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.8f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_EXTEND, 1.3f, 0.6f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_AMBIENT, 1.4f, 0.7f);
                    c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, 3.0, 0), 3, 0.5, 0.1, 0.5, 0.03);
                    c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 3.0, 0), 25, 0.8, 0.4, 0.8, 0.04);
                }
            }

            // Update active rings: expand outward (0.5 -> 3.5) + rise (Y+0.1/tick)
            Iterator<Ring> it = activeRings.iterator();
            while (it.hasNext()) {
                Ring ring = it.next();
                ring.age++;
                double t = ring.age / (double) ring.maxAge;
                double radius = 0.5 + t * 3.0;
                double y = 3.0 + ring.age * 0.18;
                float yScale = (float) Math.max(0, 0.18 * (1.0 - t));
                for (int i = 0; i < ring.slabs.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / ring.slabs.size();
                    BlockDisplay e = ring.slabs.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(Math.cos(a) * radius, y, Math.sin(a) * radius)); } catch (Throwable ignored) {}
                    setScale(e, 0.28f, yScale, 0.18f, 2);
                }
                if (ring.age >= ring.maxAge) {
                    for (BlockDisplayHandle h : ring.slabs) shrinkToZero(h, 4);
                    it.remove();
                }
                // Trail particles on each segment
                if (tick % 2 == 0) {
                    for (int i = 0; i < ring.slabs.size(); i += 3) {
                        BlockDisplay e = ring.slabs.get(i).entity();
                        if (e == null || !e.isValid()) continue;
                        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, e.getLocation(), 2, 0.15, 0.15, 0.15, 0.05);
                        c.getWorld().spawnParticle(Particle.SMOKE, e.getLocation(), 1, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }

            // Chimney top smoke + flames
            if (tick > 25 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 2.8, 0), 5, 0.4, 0.4, 0.4, 0.03);
                c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(0, 2.8, 0), 4, 0.3, 0.1, 0.3, 0.05);
            }
            // Base rumble
            if (tick > 0 && tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_STEP, 0.6f, 0.4f);
            }
            if (tick > 0 && tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.5f);
            }

            // Dissipate: chimney sinks
            if (tick == dur - 30) {
                for (List<BlockDisplayHandle> ring : chimneyRings) {
                    for (BlockDisplayHandle h : ring) shrinkToZero(h, 18);
                }
                for (BlockDisplayHandle h : base) shrinkToZero(h, 18);
                for (BlockDisplayHandle h : cap) shrinkToZero(h, 18);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.3f, 0.5f);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 1.5, 0), 50, 1.2, 1.5, 1.2, 0.04);
            }
            if (tick == dur - 10) {
                // Clean up any still-active fired rings
                for (Ring ring : activeRings) {
                    for (BlockDisplayHandle h : ring.slabs) shrinkToZero(h, 8);
                }
                activeRings.clear();
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronChimney(plugin); }
    }
}
