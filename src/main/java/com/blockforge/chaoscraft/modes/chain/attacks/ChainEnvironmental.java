package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain Mode — ENVIRONMENTAL ATTACKS 1-15 (Hanging & Ambient Item Displays)
 *
 * AMBIENT SETPIECES — these are NOT damage attacks. They are always-on
 * atmospheric decorations that fill the arena with heavy-industrial iron/chain
 * presence: hanging chain clusters, swaying lanterns, dangling weights, wall
 * mounts, ambient fields, gears, anchors, cages, spirals, etc.
 *
 * === OVERHAUL (visibility pass) ===
 * Player feedback: "make the chains more visible and bigger, add things."
 * Every chain segment is now THICK (>= 0.4 x 1.2 x 0.4 instead of ~0.15),
 * chain counts are doubled (4-6 -> 8-12), every setpiece carries extra accent
 * ItemDisplays (LANTERN, IRON_BLOCK weights, IRON_BARS, ANVIL) at readable
 * 0.4-0.8 scale, every display glows for visibility (rust-orange 200,110,40 for
 * industrial, amethyst-purple for energy), and every active tick lays down 3+
 * heavy particle layers (5-15 particles each, not 1). Nothing is static —
 * everything sways / bobs / rotates / drifts continuously via animateTo.
 *
 * Damage = 0, damage-radius = 0 across the board — the framework's damage
 * pass is effectively a no-op for this category. All sensory feedback is
 * cosmetic: ItemDisplays + layered particles + atmospheric sound.
 *
 * Choreography per attack:
 *   PHASE 1 — SPAWN     : scale 0 -> full, materialise from nothing
 *   PHASE 2 — IDLE      : continuous sway / bob / rotate via animateTo
 *   PHASE 3 — DISSIPATE : ambient close-out particles, fade
 *
 * NO BlockDisplays. NO potion effects. NO damage. Pure atmosphere.
 */
public final class ChainEnvironmental {
    private ChainEnvironmental() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    // Glow palettes (visibility)
    private static final int[] RUST = {200, 110, 40};      // industrial rust-orange
    private static final int[] IRON = {210, 210, 230};     // bright iron
    private static final int[] AMETHYST = {170, 80, 230};  // energy purple
    private static final int[] LANTERN_GLOW = {255, 180, 80};
    private static final int[] DARK_IRON = {120, 120, 150};

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new HangingChainClusters(plugin));
        registry.register(new SwayingLanterns(plugin));
        registry.register(new DanglingIronWeights(plugin));
        registry.register(new ChainLoopWallMounts(plugin));
        registry.register(new IronIngotField(plugin));
        registry.register(new ChainHookWall(plugin));
        registry.register(new AnchorDisplay(plugin));
        registry.register(new SwingingCageDoor(plugin));
        registry.register(new IronGearDisplay(plugin));
        registry.register(new ChainSpiralAmbient(plugin));
        registry.register(new IronShackleRing(plugin));
        registry.register(new AmbientWreckingBall(plugin));
        registry.register(new ChainCurtainBackground(plugin));
        registry.register(new NetheriteIngotCluster(plugin));
        registry.register(new IronAxeWeaponRack(plugin));
    }

    // ================================================================
    // Shared helpers
    // ================================================================

    /** Configure an ambient attack: no damage, long duration, high chance, short cooldown. */
    private static void applyAmbientDefaults(AttackConfig config) {
        config.setDamage(0.0);
        config.setDamageRadius(0.0);
        config.setTicksBetweenDamage(20);
        config.setDamageDelayTicks(0);
        config.setDamageOnImpactOnly(false);
        config.setImpactDamage(0.0);
        config.setImpactRadius(0.0);
        config.setTracksPlayer(false);
        config.setFollowAiEnabled(false);
        config.setEnabled(true);
        config.setChance(10.0);
    }

    /**
     * HEAVY layered ambient particle pass — forge-orange rust dust drip +
     * industrial smoke + electric sparks. 3 layers, several particles each,
     * not the old ~1/tick. Call this at a setpiece anchor every tick.
     */
    private static void ambientParticles(World w, Location p, int tick) {
        // Layer 1: rust dust drip (falls downward)
        DisplayBuilder.dustParticles(p.clone().add(0, -0.2, 0), 4, 0.3, 200, 110, 40, 1.4f);
        // Layer 2: industrial smoke haze
        w.spawnParticle(Particle.SMOKE, p, 3, 0.3, 0.25, 0.3, 0.01);
        // Layer 3: electric sparks (metal stress)
        if (tick % 2 == 0) {
            w.spawnParticle(Particle.ELECTRIC_SPARK, p, 4, 0.25, 0.2, 0.25, 0.02);
        }
        // Layer 4: occasional crit shimmer + soul flame accent
        if (tick % 5 == 0) {
            w.spawnParticle(Particle.CRIT, p, 3, 0.25, 0.2, 0.25, 0.05);
        }
    }

    /** Rust drip beneath a chain segment — the "everything is rusting" look. */
    private static void rustDrip(World w, Location p, int tick) {
        DisplayBuilder.dustParticles(p.clone().add(0, -0.4, 0), 2, 0.12, 190, 100, 35, 1.2f);
        if (tick % 3 == 0) {
            w.spawnParticle(Particle.FALLING_DUST, p.clone().add(0, -0.5, 0), 1, 0.08, 0.1, 0.08, 0.0,
                    Material.IRON_BLOCK.createBlockData());
        }
        if (tick % 7 == 0) {
            w.spawnParticle(Particle.CRIT, p, 2, 0.1, 0.15, 0.1, 0.03);
        }
    }

    // ================================================================
    // 1. HANGING CHAIN CLUSTERS — "The Ceiling"
    //    14 anchor points, 6-8 THICK chains each (~100 chains total). Each
    //    chain scaled 0.4 x 1.2 x 0.4, staggered heights, sways +-5 deg, rust
    //    drips from every chain, glows rust-orange. Each anchor carries an
    //    IRON_BARS hook plate so the ceiling reads as a real mount.
    // ================================================================
    public static class HangingChainClusters extends EnvironmentalAttack {
        private static final int ANCHORS = 14;
        private final List<ItemDisplayHandle> chains = new ArrayList<>();
        private final List<double[]> chainData = new ArrayList<>(); // {cx, cy, cz, periodTicks, tiltDeg, phase}
        private final List<ItemDisplayHandle> hooks = new ArrayList<>();
        private final List<double[]> hookPos = new ArrayList<>();    // {hx, hy, hz}
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 30;

        public HangingChainClusters(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hanging_chain_clusters", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1200);
            config.setCooldownTicks(80);
            config.setDesignType("Ambient — hanging chains (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.8f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.6f);

            for (int a = 0; a < ANCHORS; a++) {
                double aAng = Math.PI * 2 * a / ANCHORS + Math.random() * 0.2;
                double aRad = 8.0 + Math.random() * 4.5;
                double ax = Math.cos(aAng) * aRad;
                double az = Math.sin(aAng) * aRad;
                double ay = 6.5 + Math.random() * 1.5;
                double basePeriod = 80 + Math.random() * 40;

                // IRON_BARS hook plate at the anchor (the ceiling mount)
                Location hp = c.clone().add(ax, ay + 0.2, az);
                ItemDisplayHandle hook = displayBuilder.spawnItem(hp, new ItemStack(Material.IRON_BARS));
                hook.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                hooks.add(hook);
                hookPos.add(new double[]{ax, ay + 0.2, az});
                spawnedEntities.add(hook.entity());

                int count = 6 + (int) (Math.random() * 3); // 6-8 thick chains
                for (int i = 0; i < count; i++) {
                    double off = (i - count / 2.0) * 0.28;
                    double cx = ax + off;
                    double cz = az + (Math.random() - 0.5) * 0.4;
                    double cy = ay - 0.3 - Math.random() * 0.6;
                    double tilt = (Math.random() - 0.5) * 10.0; // +-5 deg
                    Location p = c.clone().add(cx, cy, cz);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                    chains.add(h);
                    chainData.add(new double[]{cx, cy, cz, basePeriod + (Math.random() - 0.5) * 20, tilt, Math.random() * Math.PI * 2});
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;

            // THICK chain base scale
            float baseScaleX = 0.42f, baseScaleY = 1.3f, baseScaleZ = 0.42f;

            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            for (int i = 0; i < chains.size(); i++) {
                double[] d = chainData.get(i);
                double swayDeg = 5.0 * Math.sin(tick / d[3] * Math.PI * 2 + d[5]);
                double swayRad = Math.toRadians(swayDeg + d[4]);
                float sx = baseScaleX * fade;
                float sy = baseScaleY * fade;
                float sz = baseScaleZ * fade;
                chains.get(i).animateTo(
                        new Vector3f((float)d[0] - sx / 2, (float)d[1] - sy / 2, (float)d[2] - sz / 2),
                        new AxisAngle4f((float) swayRad, 0, 0, 1),
                        new Vector3f(sx, sy, sz), 4);
                // Rust drip from every 3rd chain each tick (rotating coverage)
                if (i % 3 == tick % 3) {
                    Location p = c.clone().add(d[0], d[1] - sy * 0.5, d[2]);
                    rustDrip(w, p, tick);
                }
            }

            // Hook plates — tiny bob
            float hs = 0.55f * fade, hsy = 0.4f * fade;
            for (int i = 0; i < hooks.size(); i++) {
                double[] hp = hookPos.get(i);
                double bob = Math.sin(tick / 70.0 * Math.PI * 2 + i) * 0.03;
                hooks.get(i).animateTo(
                        new Vector3f((float)hp[0] - hs / 2, (float)(hp[1] + bob) - hsy / 2, (float)hp[2] - hs / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(hs, hsy, hs), 4);
                if (i % 4 == tick % 4) {
                    ambientParticles(w, c.clone().add(hp[0], hp[1], hp[2]), tick);
                }
            }

            if (tick == SPAWN_END) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.6f, 0.5f);
            if (tick == dissipateStart) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 0.4f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new HangingChainClusters(plugin); }
    }

    // ================================================================
    // 2. SWAYING LANTERNS — "The Light"
    //    14 lanterns around perimeter, each = LANTERN (0.6) + 3 THICK CHAIN
    //    links above. Sway +-8 deg. Warm-orange glow + dense FLAME/light aura.
    // ================================================================
    public static class SwayingLanterns extends EnvironmentalAttack {
        private static final int LANTERNS = 14;
        private static final int LINKS = 3;
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();
        private final List<ItemDisplayHandle> chains = new ArrayList<>(); // LINKS per lantern
        private final double[] lx = new double[LANTERNS];
        private final double[] ly = new double[LANTERNS];
        private final double[] lz = new double[LANTERNS];
        private final double[] period = new double[LANTERNS];
        private final double[] phase = new double[LANTERNS];
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 30;

        public SwayingLanterns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("swaying_lanterns", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1200);
            config.setCooldownTicks(80);
            config.setDesignType("Ambient — swaying lanterns (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_LANTERN_PLACE, 0.8f, 0.9f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.7f);

            for (int i = 0; i < LANTERNS; i++) {
                double ang = Math.PI * 2 * i / LANTERNS;
                double r = 9.0 + (i % 2) * 1.5;
                lx[i] = Math.cos(ang) * r;
                ly[i] = 4.0;
                lz[i] = Math.sin(ang) * r;
                period[i] = 60 + Math.random() * 40;
                phase[i] = Math.random() * Math.PI * 2;

                Location lp = c.clone().add(lx[i], ly[i], lz[i]);
                ItemDisplayHandle lh = displayBuilder.spawnItem(lp, new ItemStack(Material.LANTERN));
                lh.scale(0.001f, 0.001f, 0.001f).glow(LANTERN_GLOW[0], LANTERN_GLOW[1], LANTERN_GLOW[2]).interpolation(2, 0);
                lanterns.add(lh);
                spawnedEntities.add(lh.entity());

                for (int k = 0; k < LINKS; k++) {
                    Location cp = c.clone().add(lx[i], ly[i] + 0.6 + k * 0.5, lz[i]);
                    ItemDisplayHandle ch = displayBuilder.spawnItem(cp, new ItemStack(Material.CHAIN));
                    ch.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                    chains.add(ch);
                    spawnedEntities.add(ch.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            float lScale = 0.6f * fade;
            float chScaleY = 0.5f * fade;
            float chScaleXZ = 0.4f * fade; // THICK links

            for (int i = 0; i < LANTERNS; i++) {
                double sway = Math.toRadians(8.0 * Math.sin(tick / period[i] * Math.PI * 2 + phase[i]));
                lanterns.get(i).animateTo(
                        new Vector3f((float)lx[i] - lScale / 2, (float)ly[i] - lScale / 2, (float)lz[i] - lScale / 2),
                        new AxisAngle4f((float) sway, 1, 0, 0),
                        new Vector3f(lScale), 4);
                for (int k = 0; k < LINKS; k++) {
                    double cy = ly[i] + 0.6 + k * 0.5;
                    chains.get(i * LINKS + k).animateTo(
                            new Vector3f((float)lx[i] - chScaleXZ / 2, (float)cy - chScaleY / 2, (float)lz[i] - chScaleXZ / 2),
                            new AxisAngle4f((float) sway, 1, 0, 0),
                            new Vector3f(chScaleXZ, chScaleY, chScaleXZ), 4);
                }
                // Dense warm light aura around EVERY lantern, every tick (8/tick)
                Location lp = c.clone().add(lx[i], ly[i], lz[i]);
                w.spawnParticle(Particle.FLAME, lp, 4, 0.18, 0.18, 0.18, 0.005);
                DisplayBuilder.dustParticles(lp, 4, 0.3, 255, 200, 110, 1.3f);
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.END_ROD, lp, 1, 0.15, 0.15, 0.15, 0.005);
                }
                if (tick % 10 == i % 10) {
                    w.spawnParticle(Particle.SMOKE, lp.clone().add(0, 0.5, 0), 2, 0.1, 0.15, 0.1, 0.005);
                }
            }
            if (tick == SPAWN_END) DisplayBuilder.playSound(c, Sound.BLOCK_LANTERN_PLACE, 0.5f, 1.1f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SwayingLanterns(plugin); }
    }

    // ================================================================
    // 3. DANGLING IRON WEIGHTS — "The Measures"
    //    11 weights, each = IRON_BLOCK (0.6 x 0.9 x 0.6) + 4 THICK chains + an
    //    ANVIL counterweight every 3rd. Bob, metal-scrape SMOKE + sparks.
    // ================================================================
    public static class DanglingIronWeights extends EnvironmentalAttack {
        private static final int WEIGHTS = 11;
        private static final int CHAINS_EACH = 4;
        private final List<ItemDisplayHandle> weights = new ArrayList<>();
        private final List<ItemDisplayHandle> chainsAbove = new ArrayList<>(); // CHAINS_EACH per weight
        private final double[] wx = new double[WEIGHTS];
        private final double[] wy = new double[WEIGHTS];
        private final double[] wz = new double[WEIGHTS];
        private final boolean[] heavy = new boolean[WEIGHTS]; // anvil instead of iron block
        private final double[] period = new double[WEIGHTS];
        private final double[] phase = new double[WEIGHTS];
        private final int[] stagger = new int[WEIGHTS];
        private static final int SPAWN_LEN = 100;
        private static final int DISSIPATE_LEN = 30;

        public DanglingIronWeights(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dangling_iron_weights", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1000);
            config.setCooldownTicks(90);
            config.setDesignType("Ambient — dangling weights (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.6f);

            for (int i = 0; i < WEIGHTS; i++) {
                double ang = Math.PI * 2 * i / WEIGHTS + Math.random() * 0.2;
                double r = 6.0 + Math.random() * 4.0;
                wx[i] = Math.cos(ang) * r;
                wz[i] = Math.sin(ang) * r;
                wy[i] = 2.0 + Math.random() * 2.0;
                heavy[i] = (i % 3 == 0);
                period[i] = 60 + Math.random() * 20;
                phase[i] = Math.random() * Math.PI * 2;
                stagger[i] = (int) (Math.random() * SPAWN_LEN * 0.7);

                Location wp = c.clone().add(wx[i], wy[i], wz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(wp,
                        new ItemStack(heavy[i] ? Material.ANVIL : Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                weights.add(h);
                spawnedEntities.add(h.entity());

                for (int k = 0; k < CHAINS_EACH; k++) {
                    Location cp = c.clone().add(wx[i], wy[i] + 0.7 + k * 0.5, wz[i]);
                    ItemDisplayHandle ch = displayBuilder.spawnItem(cp, new ItemStack(Material.CHAIN));
                    ch.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                    chainsAbove.add(ch);
                    spawnedEntities.add(ch.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;

            for (int i = 0; i < WEIGHTS; i++) {
                float fade;
                if (tick < stagger[i]) fade = 0.0f;
                else if (tick < stagger[i] + 15) fade = (tick - stagger[i]) / 15.0f;
                else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);
                else fade = 1.0f;

                double yOff = 0.12 * Math.sin(tick / period[i] * Math.PI * 2 + phase[i]);
                float sx = 0.6f * fade, sy = 0.9f * fade, sz = 0.6f * fade;
                weights.get(i).animateTo(
                        new Vector3f((float)wx[i] - sx / 2, (float)(wy[i] + yOff) - sy / 2, (float)wz[i] - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 4);
                for (int k = 0; k < CHAINS_EACH; k++) {
                    float cxs = 0.4f * fade; // THICK
                    float cys = 0.5f * fade;
                    double cy = wy[i] + 0.7 + k * 0.5 + yOff * 0.5;
                    chainsAbove.get(i * CHAINS_EACH + k).animateTo(
                            new Vector3f((float)wx[i] - cxs / 2, (float)cy - cys / 2, (float)wz[i] - cxs / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(cxs, cys, cxs), 4);
                }
                if (fade > 0.01f) {
                    Location wp = c.clone().add(wx[i], wy[i] + yOff, wz[i]);
                    // metal-scrape smoke + sparks every tick on rotating subset
                    if (i % 2 == tick % 2) {
                        w.spawnParticle(Particle.SMOKE, wp, 3, 0.25, 0.2, 0.25, 0.005);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, wp, 3, 0.2, 0.15, 0.2, 0.02);
                        DisplayBuilder.dustParticles(wp.clone().add(0, -0.3, 0), 2, 0.2, 200, 110, 40, 1.3f);
                    }
                    if (tick % 80 == i * 8 % 80) {
                        DisplayBuilder.playSound(wp, Sound.BLOCK_CHAIN_HIT, 0.4f, 0.6f + (float) Math.random() * 0.2f);
                    }
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DanglingIronWeights(plugin); }
    }

    // ================================================================
    // 4. CHAIN LOOP WALL MOUNTS — "The Decoration"
    //    12 chain-ring loops on walls, 8 THICK links each, rotating on the
    //    wall-perpendicular axis, each loop centered on an IRON_BARS plate.
    // ================================================================
    public static class ChainLoopWallMounts extends EnvironmentalAttack {
        private static final int LOOPS = 12;
        private static final int LINKS_PER_LOOP = 8;
        private final List<ItemDisplayHandle> links = new ArrayList<>();
        private final List<ItemDisplayHandle> plates = new ArrayList<>();
        private final double[] loopCx = new double[LOOPS];
        private final double[] loopCy = new double[LOOPS];
        private final double[] loopCz = new double[LOOPS];
        private final double[] loopAxisX = new double[LOOPS];
        private final double[] loopAxisZ = new double[LOOPS];
        private final double[] rotSpeed = new double[LOOPS];
        private static final double LOOP_RADIUS = 0.7;
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 30;

        public ChainLoopWallMounts(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_loop_wall_mounts", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1000);
            config.setCooldownTicks(100);
            config.setDesignType("Ambient — wall decoration (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.7f);

            for (int l = 0; l < LOOPS; l++) {
                int wall = l % 4;
                double along = -4.0 + Math.random() * 8.0;
                double height = 2.0 + Math.random() * 3.0;
                double dist = 12.0;
                double cx, cz, axisX, axisZ;
                switch (wall) {
                    case 0 -> { cx = along; cz = dist; axisX = 0; axisZ = 1; }
                    case 1 -> { cx = along; cz = -dist; axisX = 0; axisZ = -1; }
                    case 2 -> { cx = dist; cz = along; axisX = 1; axisZ = 0; }
                    default -> { cx = -dist; cz = along; axisX = -1; axisZ = 0; }
                }
                loopCx[l] = cx;
                loopCy[l] = height;
                loopCz[l] = cz;
                loopAxisX[l] = axisX;
                loopAxisZ[l] = axisZ;
                rotSpeed[l] = (0.5 + Math.random() * 1.0) * (Math.random() < 0.5 ? -1 : 1) * Math.PI / 90.0;

                // IRON_BARS backing plate behind each loop
                Location pp = c.clone().add(cx + axisX * 0.05, height, cz + axisZ * 0.05);
                ItemDisplayHandle plate = displayBuilder.spawnItem(pp, new ItemStack(Material.IRON_BARS));
                plate.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                plates.add(plate);
                spawnedEntities.add(plate.entity());

                double tanAx = -axisZ;
                double tanAz = axisX;
                double tanBy = 1;
                for (int i = 0; i < LINKS_PER_LOOP; i++) {
                    double a = Math.PI * 2 * i / LINKS_PER_LOOP;
                    double px = cx + (tanAx * Math.cos(a)) * LOOP_RADIUS;
                    double py = height + (tanBy * Math.sin(a)) * LOOP_RADIUS;
                    double pz = cz + (tanAz * Math.cos(a)) * LOOP_RADIUS;
                    Location lp = c.clone().add(px, py, pz);
                    ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                    links.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            for (int l = 0; l < LOOPS; l++) {
                double rot = rotSpeed[l] * tick;
                double tanAx = -loopAxisZ[l];
                double tanAz = loopAxisX[l];
                double tanBy = 1;
                for (int i = 0; i < LINKS_PER_LOOP; i++) {
                    double a = Math.PI * 2 * i / LINKS_PER_LOOP + rot;
                    double px = loopCx[l] + (tanAx * Math.cos(a)) * LOOP_RADIUS;
                    double py = loopCy[l] + (tanBy * Math.sin(a)) * LOOP_RADIUS;
                    double pz = loopCz[l] + (tanAz * Math.cos(a)) * LOOP_RADIUS;
                    float s = 0.4f * fade; // THICK
                    int idx = l * LINKS_PER_LOOP + i;
                    links.get(idx).animateTo(
                            new Vector3f((float)px - s / 2, (float)py - s / 2, (float)pz - s / 2),
                            new AxisAngle4f((float) a, (float) loopAxisX[l], 0, (float) loopAxisZ[l]),
                            new Vector3f(s, s, s), 3);
                }
                float ps = 0.7f * fade, psThin = 0.1f * fade;
                plates.get(l).animateTo(
                        new Vector3f((float)(loopCx[l]) - (loopAxisX[l] != 0 ? psThin : ps) / 2,
                                (float)loopCy[l] - ps / 2,
                                (float)(loopCz[l]) - (loopAxisZ[l] != 0 ? psThin : ps) / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(loopAxisX[l] != 0 ? psThin : ps, ps, loopAxisZ[l] != 0 ? psThin : ps), 4);
                if (l % 3 == tick % 3) {
                    Location p = c.clone().add(loopCx[l], loopCy[l], loopCz[l]);
                    ambientParticles(w, p, tick);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainLoopWallMounts(plugin); }
    }

    // ================================================================
    // 5. IRON INGOT FIELD — "The Harvest"
    //    10 IRON_INGOT orbiting at Y=2.5 (scale 0.5), each trailing a THICK
    //    CHAIN tail + dense spark/dust field so it reads as a glowing forge.
    // ================================================================
    public static class IronIngotField extends EnvironmentalAttack {
        private static final int INGOTS = 10;
        private final List<ItemDisplayHandle> ingots = new ArrayList<>();
        private final List<ItemDisplayHandle> tails = new ArrayList<>();
        private final double[] radius = new double[INGOTS];
        private final double[] phase = new double[INGOTS];
        private final double[] angSpeed = new double[INGOTS];
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 30;

        public IronIngotField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_ingot_field", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1000);
            config.setCooldownTicks(70);
            config.setDesignType("Ambient — ingot scatter (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 1.3f);
            for (int i = 0; i < INGOTS; i++) {
                radius[i] = 2.5 + Math.random() * 3.0;
                phase[i] = Math.random() * Math.PI * 2;
                angSpeed[i] = (0.3 + Math.random() * 0.7) * (Math.random() < 0.5 ? -1 : 1) * Math.PI / 180.0;
                Location p = c.clone().add(Math.cos(phase[i]) * radius[i], 2.5, Math.sin(phase[i]) * radius[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_INGOT));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                ingots.add(h);
                spawnedEntities.add(h.entity());

                // THICK chain tail dragging behind each ingot
                ItemDisplayHandle t = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                t.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                tails.add(t);
                spawnedEntities.add(t.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            float s = 0.5f * fade;
            for (int i = 0; i < INGOTS; i++) {
                double ang = phase[i] + angSpeed[i] * tick;
                double x = Math.cos(ang) * radius[i];
                double z = Math.sin(ang) * radius[i];
                double y = 2.5 + Math.sin(tick * 0.05 + i) * 0.25;
                float spin = (float) Math.toRadians(10.0 * tick);
                ingots.get(i).animateTo(
                        new Vector3f((float)x - s / 2, (float)y - s / 2, (float)z - s / 2),
                        new AxisAngle4f(spin, 0, 1, 0),
                        new Vector3f(s, s, s), 4);
                // thick chain tail just behind (lagging angle)
                double tang = ang - angSpeed[i] * 8;
                double tx = Math.cos(tang) * radius[i];
                double tz = Math.sin(tang) * radius[i];
                float ts = 0.4f * fade, tsy = 0.8f * fade;
                tails.get(i).animateTo(
                        new Vector3f((float)tx - ts / 2, (float)(y - 0.1) - tsy / 2, (float)tz - ts / 2),
                        new AxisAngle4f(spin, 0, 1, 0),
                        new Vector3f(ts, tsy, ts), 4);

                Location p = c.clone().add(x, y, z);
                // dense forge field: sparks + dust + smoke every tick
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 4, 0.15, 0.15, 0.15, 0.02);
                DisplayBuilder.dustParticles(p, 3, 0.2, 220, 150, 60, 1.2f);
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.SMOKE, p, 2, 0.12, 0.1, 0.12, 0.005);
                }
                if (tick % 6 == i % 6) {
                    w.spawnParticle(Particle.CRIT, p, 3, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronIngotField(plugin); }
    }

    // ================================================================
    // 6. CHAIN HOOK WALL — "The Workshop"
    //    10 IRON_PICKAXE hooks on a wall, each hung from 3 THICK CHAIN links,
    //    sway +-7 deg, heavy rust/spark dressing.
    // ================================================================
    public static class ChainHookWall extends EnvironmentalAttack {
        private static final int HOOKS = 10;
        private static final int LINKS = 3;
        private final List<ItemDisplayHandle> hooks = new ArrayList<>();
        private final List<ItemDisplayHandle> chains = new ArrayList<>(); // LINKS per hook
        private final double[] hx = new double[HOOKS];
        private final double[] hz = new double[HOOKS];
        private final double[] period = new double[HOOKS];
        private final double[] phase = new double[HOOKS];
        private static final double HOOK_Y = 3.0;
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 30;

        public ChainHookWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_hook_wall", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1100);
            config.setCooldownTicks(90);
            config.setDesignType("Ambient — wall hooks (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.4f);

            double wallZ = 11.0;
            double startX = -6.75;
            for (int i = 0; i < HOOKS; i++) {
                hx[i] = startX + i * 1.5;
                hz[i] = wallZ;
                period[i] = 70 + Math.random() * 30;
                phase[i] = Math.random() * Math.PI * 2;
                Location p = c.clone().add(hx[i], HOOK_Y, hz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_PICKAXE));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                hooks.add(h);
                spawnedEntities.add(h.entity());

                for (int k = 0; k < LINKS; k++) {
                    Location cp = c.clone().add(hx[i], HOOK_Y + 0.6 + k * 0.45, hz[i]);
                    ItemDisplayHandle ch = displayBuilder.spawnItem(cp, new ItemStack(Material.CHAIN));
                    ch.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                    chains.add(ch);
                    spawnedEntities.add(ch.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            float s = 0.8f * fade;
            for (int i = 0; i < HOOKS; i++) {
                double sway = Math.toRadians(7.0 * Math.sin(tick / period[i] * Math.PI * 2 + phase[i]));
                hooks.get(i).animateTo(
                        new Vector3f((float)hx[i] - s / 2, (float)HOOK_Y - s / 2, (float)hz[i] - s / 2),
                        new AxisAngle4f((float) sway, 1, 0, 0),
                        new Vector3f(s, s, s), 4);
                for (int k = 0; k < LINKS; k++) {
                    float cxs = 0.4f * fade, cys = 0.45f * fade; // THICK
                    double cy = HOOK_Y + 0.6 + k * 0.45;
                    chains.get(i * LINKS + k).animateTo(
                            new Vector3f((float)hx[i] - cxs / 2, (float)cy - cys / 2, (float)hz[i] - cxs / 2),
                            new AxisAngle4f((float) sway, 1, 0, 0),
                            new Vector3f(cxs, cys, cxs), 4);
                }
                if (i % 2 == tick % 2) {
                    Location p = c.clone().add(hx[i], HOOK_Y, hz[i]);
                    ambientParticles(w, p, tick);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainHookWall(plugin); }
    }

    // ================================================================
    // 7. ANCHOR DISPLAY — "The Trophy"
    //    Large iron anchor on a pedestal, slow Y-rotate, with a 6-link THICK
    //    CHAIN festoon hanging off the top ring. Bigger parts, rust glow.
    // ================================================================
    public static class AnchorDisplay extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> parts = new ArrayList<>();
        private final List<double[]> partPos = new ArrayList<>(); // {x,y,z,scale}
        private final List<ItemDisplayHandle> chainHang = new ArrayList<>(); // 6 thick links
        private static final int HANG_LINKS = 6;
        private static final double PIVOT_X = 8.0;
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 30;

        public AnchorDisplay(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("anchor_display", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1200);
            config.setCooldownTicks(120);
            config.setDesignType("Ambient — anchor trophy (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.5f);

            double offX = PIVOT_X, offZ = 0;
            // shaft 4 stacked IRON_BLOCK (taller, bigger)
            for (int s = 0; s < 4; s++) {
                addPart(c, offX, 2.0 + s * 0.55, offZ, Material.IRON_BLOCK, 0.5f);
            }
            // top ring 4 IRON_BLOCK around
            double[][] ring = {{0.5, 0}, {-0.5, 0}, {0, 0.5}, {0, -0.5}};
            for (double[] r : ring) {
                addPart(c, offX + r[0], 4.0, offZ + r[1], Material.IRON_BLOCK, 0.32f);
            }
            // bottom flukes (hooks): 2 angled IRON_BLOCK
            addPart(c, offX + 0.8, 1.7, offZ, Material.IRON_BLOCK, 0.4f);
            addPart(c, offX - 0.8, 1.7, offZ, Material.IRON_BLOCK, 0.4f);
            // central crossbar (stock)
            addPart(c, offX, 3.3, offZ, Material.IRON_BLOCK, 0.5f);

            // THICK CHAIN festoon hanging from the top ring
            for (int k = 0; k < HANG_LINKS; k++) {
                Location ch = c.clone().add(offX, 4.4 + k * 0.0 - k * 0.45, offZ);
                ItemDisplayHandle h = displayBuilder.spawnItem(ch, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                chainHang.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        private void addPart(Location c, double x, double y, double z, Material m, float sc) {
            Location p = c.clone().add(x, y, z);
            ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(m));
            h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
            parts.add(h);
            partPos.add(new double[]{x, y, z, sc});
            spawnedEntities.add(h.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            float rotY = (float) ((tick / 120.0) * Math.PI * 2);
            double cosR = Math.cos(rotY), sinR = Math.sin(rotY);
            for (int i = 0; i < parts.size(); i++) {
                double[] d = partPos.get(i);
                double localX = d[0] - PIVOT_X;
                double nx = PIVOT_X + localX * cosR - d[2] * sinR;
                double nz = d[2] * cosR + localX * sinR;
                float sc = (float) d[3] * fade;
                parts.get(i).animateTo(
                        new Vector3f((float)nx - sc / 2, (float)d[1] - sc / 2, (float)nz - sc / 2),
                        new AxisAngle4f(rotY, 0, 1, 0),
                        new Vector3f(sc), 4);
            }
            // chain festoon sways gently + rotates with anchor
            for (int k = 0; k < HANG_LINKS; k++) {
                double swing = Math.toRadians(6.0 * Math.sin(tick / 90.0 * Math.PI * 2 + k * 0.3));
                double y = 4.4 - k * 0.45;
                float chs = 0.4f * fade, chsy = 0.5f * fade;
                chainHang.get(k).animateTo(
                        new Vector3f((float)PIVOT_X - chs / 2, (float)y - chsy / 2, -chs / 2),
                        new AxisAngle4f((float) swing, 0, 0, 1),
                        new Vector3f(chs, chsy, chs), 4);
            }

            Location p = c.clone().add(PIVOT_X, 2.5, 0);
            ambientParticles(w, p, tick);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AnchorDisplay(plugin); }
    }

    // ================================================================
    // 8. SWINGING CAGE DOOR — "The Entrance"
    //    Big IRON_BARS frame + 5 IRON_BARS door slats swinging +-60 deg, plus
    //    2 THICK CHAIN drapes on the hinge side. Bigger, rust-glowing.
    // ================================================================
    public static class SwingingCageDoor extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> frame = new ArrayList<>();
        private final List<ItemDisplayHandle> bars = new ArrayList<>();
        private final List<ItemDisplayHandle> drapes = new ArrayList<>();
        private final double[][] frameOff = new double[4][3];
        private final double[][] barOff = new double[5][3];
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 30;
        private static final double DOOR_X = -8.0;
        private static final double DOOR_Z = 0;
        private static final double DOOR_Y = 2.5;

        public SwingingCageDoor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("swinging_cage_door", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1100);
            config.setCooldownTicks(100);
            config.setDesignType("Ambient — cage door (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_IRON_DOOR_OPEN, 0.5f, 0.4f);
            // Frame edges: top, bottom, left, right (bigger)
            double[][] f = {{0, 1.5, 0}, {0, -1.5, 0}, {-0.9, 0, 0}, {0.9, 0, 0}};
            for (int i = 0; i < 4; i++) {
                frameOff[i] = f[i];
                Location p = c.clone().add(DOOR_X + f[i][0], DOOR_Y + f[i][1], DOOR_Z + f[i][2]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BARS));
                h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                frame.add(h);
                spawnedEntities.add(h.entity());
            }
            // Door slats (5 vertical IRON_BARS, pivot at left hinge)
            double[][] b = {{-0.6, 0, 0}, {-0.3, 0, 0}, {0, 0, 0}, {0.3, 0, 0}, {0.6, 0, 0}};
            for (int i = 0; i < 5; i++) {
                barOff[i] = b[i];
                Location p = c.clone().add(DOOR_X + b[i][0], DOOR_Y, DOOR_Z);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BARS));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                bars.add(h);
                spawnedEntities.add(h.entity());
            }
            // 2 THICK CHAIN drapes on the hinge side
            for (int k = 0; k < 2; k++) {
                Location p = c.clone().add(DOOR_X - 0.9, DOOR_Y + 0.8 - k * 0.6, DOOR_Z);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                drapes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            // Frame: static (with scale fade) — bigger members
            for (int i = 0; i < 4; i++) {
                double[] o = frameOff[i];
                float sx = (Math.abs(o[0]) > 0.5 ? 0.16f : 2.0f) * fade;
                float sy = (Math.abs(o[1]) > 0.5 ? 0.16f : 3.2f) * fade;
                float sz = 0.16f * fade;
                frame.get(i).animateTo(
                        new Vector3f((float)(DOOR_X + o[0]) - sx / 2, (float)(DOOR_Y + o[1]) - sy / 2, (float)(DOOR_Z + o[2]) - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 4);
            }

            // Door: +-60 deg on Y (100-tick period). Hinge at left edge (x = -0.9 local)
            double doorAngle = Math.toRadians(60.0 * Math.sin(tick / 100.0 * Math.PI * 2));
            double cosA = Math.cos(doorAngle), sinA = Math.sin(doorAngle);
            for (int i = 0; i < 5; i++) {
                double[] o = barOff[i];
                double localX = o[0] - (-0.9);
                double nx = -0.9 + localX * cosA;
                double nz = localX * sinA;
                float sx = 0.14f * fade, sy = 2.6f * fade, sz = 0.14f * fade;
                bars.get(i).animateTo(
                        new Vector3f((float)(DOOR_X + nx) - sx / 2, (float)DOOR_Y - sy / 2, (float)(DOOR_Z + nz) - sz / 2),
                        new AxisAngle4f((float) doorAngle, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 4);
            }
            // chain drapes sway with the door
            for (int k = 0; k < 2; k++) {
                double swing = Math.toRadians(8.0 * Math.sin(tick / 100.0 * Math.PI * 2 + k * 0.5));
                double y = DOOR_Y + 0.8 - k * 0.6;
                float chs = 0.4f * fade, chsy = 0.55f * fade;
                drapes.get(k).animateTo(
                        new Vector3f((float)(DOOR_X - 0.9) - chs / 2, (float)y - chsy / 2, (float)DOOR_Z - chs / 2),
                        new AxisAngle4f((float) swing, 0, 0, 1),
                        new Vector3f(chs, chsy, chs), 4);
            }

            if (tick % 100 == 50 || tick % 100 == 0) {
                Location p = c.clone().add(DOOR_X, DOOR_Y, DOOR_Z);
                DisplayBuilder.playSound(p, Sound.BLOCK_CHAIN_HIT, 0.5f, 0.6f);
            }
            ambientParticles(w, c.clone().add(DOOR_X, DOOR_Y, DOOR_Z), tick);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SwingingCageDoor(plugin); }
    }

    // ================================================================
    // 9. IRON GEAR DISPLAY — "The Machine"
    //    3 big gears on a wall, counter-rotating, with THICK CHAIN drive belts
    //    looping between them. Bigger teeth, grindstone dressing.
    // ================================================================
    public static class IronGearDisplay extends EnvironmentalAttack {
        private static final int GEARS = 3;
        private static final int TEETH = 10;
        private static final int SPOKES = 4;
        private static final int BELT_LINKS = 6;
        private final List<ItemDisplayHandle> teeth = new ArrayList<>();
        private final List<ItemDisplayHandle> spokes = new ArrayList<>();
        private final List<ItemDisplayHandle> belt = new ArrayList<>();
        private final double[] gx = new double[GEARS];
        private final double[] gy = new double[GEARS];
        private final double[] gz = new double[GEARS];
        private final double[] gRot = new double[GEARS];
        private static final double GEAR_R = 1.0;
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 30;

        public IronGearDisplay(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_gear_display", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1100);
            config.setCooldownTicks(100);
            config.setDesignType("Ambient — gear machinery (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.7f);

            double[] xs = {-3.5, 0.0, 3.5};
            for (int g = 0; g < GEARS; g++) {
                gx[g] = xs[g];
                gy[g] = 4.5;
                gz[g] = 10.0;
                gRot[g] = (g % 2 == 0 ? 1 : -1) * (Math.PI / 180.0) * (0.6 + g * 0.2);

                for (int t = 0; t < TEETH; t++) {
                    double a = Math.PI * 2 * t / TEETH;
                    double px = gx[g] + Math.cos(a) * GEAR_R;
                    double py = gy[g] + Math.sin(a) * GEAR_R;
                    Location p = c.clone().add(px, py, gz[g]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE));
                    h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                    teeth.add(h);
                    spawnedEntities.add(h.entity());
                }
                for (int s = 0; s < SPOKES; s++) {
                    double a = Math.PI * 2 * s / SPOKES + Math.PI / 4;
                    double px = gx[g] + Math.cos(a) * 0.5;
                    double py = gy[g] + Math.sin(a) * 0.5;
                    Location p = c.clone().add(px, py, gz[g]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                    h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                    spokes.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // THICK CHAIN drive belts between adjacent gears (2 spans x BELT_LINKS)
            for (int span = 0; span < GEARS - 1; span++) {
                for (int k = 0; k < BELT_LINKS; k++) {
                    Location p = c.clone().add((gx[span] + gx[span + 1]) / 2, gy[span] + GEAR_R, gz[span]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                    belt.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            for (int g = 0; g < GEARS; g++) {
                double rot = gRot[g] * tick;
                for (int t = 0; t < TEETH; t++) {
                    double a = Math.PI * 2 * t / TEETH + rot;
                    double px = gx[g] + Math.cos(a) * GEAR_R;
                    double py = gy[g] + Math.sin(a) * GEAR_R;
                    float sx = 0.6f * fade, sy = 0.28f * fade, sz = 0.18f * fade; // bigger teeth
                    int idx = g * TEETH + t;
                    teeth.get(idx).animateTo(
                            new Vector3f((float)px - sx / 2, (float)py - sy / 2, (float)gz[g] - sz / 2),
                            new AxisAngle4f((float) a, 0, 0, 1),
                            new Vector3f(sx, sy, sz), 4);
                }
                for (int s = 0; s < SPOKES; s++) {
                    double a = Math.PI * 2 * s / SPOKES + Math.PI / 4 + rot;
                    double px = gx[g] + Math.cos(a) * 0.5;
                    double py = gy[g] + Math.sin(a) * 0.5;
                    float sc = 0.32f * fade;
                    int idx = g * SPOKES + s;
                    spokes.get(idx).animateTo(
                            new Vector3f((float)px - sc / 2, (float)py - sc / 2, (float)gz[g] - sc / 2),
                            new AxisAngle4f((float) a, 0, 0, 1),
                            new Vector3f(sc), 4);
                }
                if (g % 2 == tick % 2) {
                    Location p = c.clone().add(gx[g], gy[g], gz[g]);
                    ambientParticles(w, p, tick);
                    if (tick % 20 == g % 20) DisplayBuilder.playSound(p, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 0.5f);
                }
            }
            // Drive belts loop over the gear tops, links cycling sideways
            int beltIdx = 0;
            for (int span = 0; span < GEARS - 1; span++) {
                double x0 = gx[span], x1 = gx[span + 1];
                for (int k = 0; k < BELT_LINKS; k++) {
                    double t = (k / (double) BELT_LINKS + tick / 60.0) % 1.0;
                    double px = x0 + (x1 - x0) * t;
                    double py = gy[span] + GEAR_R + Math.sin(t * Math.PI) * 0.1;
                    float bs = 0.4f * fade, bsy = 0.4f * fade;
                    belt.get(beltIdx++).animateTo(
                            new Vector3f((float)px - bs / 2, (float)py - bsy / 2, (float)gz[span] - bs / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(bs, bsy, bs), 4);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronGearDisplay(plugin); }
    }

    // ================================================================
    // 10. CHAIN SPIRAL AMBIENT — "The Coil"
    //     4 vertical helices of THICK CHAIN (16 links each, r=0.7, 2 turns,
    //     height 2.0). Rotates Y. Amethyst energy glow + dense spark column.
    // ================================================================
    public static class ChainSpiralAmbient extends EnvironmentalAttack {
        private static final int LINKS = 16;
        private static final int SPIRALS = 4;
        private final List<ItemDisplayHandle> links = new ArrayList<>();
        private final double[] sx = new double[SPIRALS];
        private final double[] sz = new double[SPIRALS];
        private final double[] sy = new double[SPIRALS];
        private static final double SPIRAL_R = 0.7;
        private static final double SPIRAL_H = 2.0;
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 30;

        public ChainSpiralAmbient(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_spiral_ambient", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1000);
            config.setCooldownTicks(80);
            config.setDesignType("Ambient — chain spiral (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 0.6f);

            double[][] corners = {{8.0, 8.0}, {-8.0, 8.0}, {8.0, -8.0}, {-8.0, -8.0}};
            for (int s = 0; s < SPIRALS; s++) {
                sx[s] = corners[s][0];
                sz[s] = corners[s][1];
                sy[s] = 4.8;
                for (int i = 0; i < LINKS; i++) {
                    double t = i / (double)(LINKS - 1);
                    double a = t * Math.PI * 4; // 2 turns
                    double y = sy[s] - t * SPIRAL_H;
                    double px = sx[s] + Math.cos(a) * SPIRAL_R;
                    double pz = sz[s] + Math.sin(a) * SPIRAL_R;
                    Location p = c.clone().add(px, y, pz);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(AMETHYST[0], AMETHYST[1], AMETHYST[2]).interpolation(2, 0);
                    links.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            double rot = tick / 80.0 * Math.PI * 2;
            double bob = Math.sin(tick / 40.0 * Math.PI * 2) * 0.08;

            for (int s = 0; s < SPIRALS; s++) {
                for (int i = 0; i < LINKS; i++) {
                    double tt = i / (double)(LINKS - 1);
                    double a = tt * Math.PI * 4 + rot;
                    double y = sy[s] + bob - tt * SPIRAL_H;
                    double px = sx[s] + Math.cos(a) * SPIRAL_R;
                    double pz = sz[s] + Math.sin(a) * SPIRAL_R;
                    float sc = 0.4f * fade;   // THICK
                    float scy = 0.5f * fade;
                    int idx = s * LINKS + i;
                    links.get(idx).animateTo(
                            new Vector3f((float)px - sc / 2, (float)y - scy / 2, (float)pz - sc / 2),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(sc, scy, sc), 4);
                }
                // dense amethyst energy column up each spiral
                Location base = c.clone().add(sx[s], sy[s] - SPIRAL_H / 2, sz[s]);
                w.spawnParticle(Particle.ELECTRIC_SPARK, base, 5, 0.3, SPIRAL_H * 0.5, 0.3, 0.02);
                DisplayBuilder.dustParticles(base, 5, 0.4, 170, 80, 230, 1.4f);
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.END_ROD, base.clone().add(0, bob, 0), 2, 0.25, SPIRAL_H * 0.4, 0.25, 0.01);
                }
                if (tick % 6 == s % 6) {
                    w.spawnParticle(Particle.WITCH, base, 2, 0.3, 0.4, 0.3, 0.0);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainSpiralAmbient(plugin); }
    }

    // ================================================================
    // 11. IRON SHACKLE RING — "The Binding"
    //     16 THICK CHAIN floor circle (r=1.2) with a broken gap + 2 IRON_BLOCK
    //     break points + a central ANVIL. Rust glow, heavy floor haze.
    // ================================================================
    public static class IronShackleRing extends EnvironmentalAttack {
        private static final int LINKS = 16;
        private final List<ItemDisplayHandle> links = new ArrayList<>();
        private final List<ItemDisplayHandle> breakPts = new ArrayList<>();
        private ItemDisplayHandle centerAnvil;
        private static final double RING_R = 1.2;
        private static final double RING_X = 10.0;
        private static final double RING_Z = -6.0;
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 30;

        public IronShackleRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_shackle_ring", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1000);
            config.setCooldownTicks(90);
            config.setDesignType("Ambient — broken shackle ring (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.4f);

            for (int i = 0; i < LINKS; i++) {
                if (i == 0 || i == 1) continue; // gap = break point
                double a = Math.PI * 2 * i / LINKS;
                double px = RING_X + Math.cos(a) * RING_R;
                double pz = RING_Z + Math.sin(a) * RING_R;
                Location p = c.clone().add(px, 0.2, pz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                links.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int k = 0; k < 2; k++) {
                double a = Math.PI * 2 * (k * 0.6 - 0.3) / LINKS;
                double px = RING_X + Math.cos(a) * RING_R + (k == 0 ? 0.2 : -0.2);
                double pz = RING_Z + Math.sin(a) * RING_R + (k == 0 ? 0.12 : -0.12);
                Location p = c.clone().add(px, 0.2, pz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                breakPts.add(h);
                spawnedEntities.add(h.entity());
            }
            // central anvil — the thing that was shackled
            Location ap = c.clone().add(RING_X, 0.3, RING_Z);
            centerAnvil = displayBuilder.spawnItem(ap, new ItemStack(Material.ANVIL));
            centerAnvil.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
            spawnedEntities.add(centerAnvil.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            double rot = Math.toRadians(0.5 * tick);
            int idx = 0;
            for (int i = 0; i < LINKS; i++) {
                if (i == 0 || i == 1) continue;
                double a = Math.PI * 2 * i / LINKS + rot;
                double px = RING_X + Math.cos(a) * RING_R;
                double pz = RING_Z + Math.sin(a) * RING_R;
                float sc = 0.4f * fade; // THICK
                links.get(idx).animateTo(
                        new Vector3f((float)px - sc / 2, 0.2f - sc / 2, (float)pz - sc / 2),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(sc, sc, sc), 4);
                idx++;
            }
            for (int k = 0; k < 2; k++) {
                double a = Math.PI * 2 * (k * 0.6 - 0.3) / LINKS + rot;
                double px = RING_X + Math.cos(a) * RING_R + (k == 0 ? 0.2 : -0.2);
                double pz = RING_Z + Math.sin(a) * RING_R + (k == 0 ? 0.12 : -0.12);
                float sc = 0.45f * fade;
                breakPts.get(k).animateTo(
                        new Vector3f((float)px - sc / 2, 0.2f - sc / 2, (float)pz - sc / 2),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(sc), 4);
            }
            float as = 0.6f * fade, asy = 0.5f * fade;
            double abob = Math.sin(tick / 80.0 * Math.PI * 2) * 0.04;
            centerAnvil.animateTo(
                    new Vector3f((float)RING_X - as / 2, (float)(0.3 + abob) - asy / 2, (float)RING_Z - as / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(as, asy, as), 4);

            Location p = c.clone().add(RING_X, 0.25, RING_Z);
            ambientParticles(w, p, tick);
            if (tick % 6 == 0) {
                w.spawnParticle(Particle.FALLING_DUST, p, 3, RING_R, 0.1, RING_R, 0.0, Material.IRON_BLOCK.createBlockData());
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronShackleRing(plugin); }
    }

    // ================================================================
    // 12. AMBIENT WRECKING BALL — "The Resting Beast"
    //     Big suspended NETHERITE_BLOCK sphere (1 core + 6 poles) on a 10-link
    //     THICK CHAIN cable. +-15 deg swing, heavy rust/spark wash.
    // ================================================================
    public static class AmbientWreckingBall extends EnvironmentalAttack {
        private static final int CABLE_LINKS = 10;
        private final List<ItemDisplayHandle> ballParts = new ArrayList<>();
        private final List<double[]> ballOffsets = new ArrayList<>();
        private final List<ItemDisplayHandle> cable = new ArrayList<>();
        private Location pivot;
        private static final int SPAWN_END = 30;
        private static final int DISSIPATE_LEN = 30;

        public AmbientWreckingBall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ambient_wrecking_ball", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1200);
            config.setCooldownTicks(120);
            config.setDesignType("Ambient — resting wrecking ball (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.8f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.4f);

            pivot = c.clone().add(0, 7.5, 0);
            double r = 0.55;
            double[][] poles = {{0, 0, 0}, {r, 0, 0}, {-r, 0, 0}, {0, r, 0}, {0, -r, 0}, {0, 0, r}, {0, 0, -r}};
            for (double[] o : poles) {
                Location p = pivot.clone().add(0, -3.5, 0).add(o[0], o[1], o[2]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHERITE_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(80, 70, 90).interpolation(2, 0);
                ballParts.add(h);
                ballOffsets.add(o);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < CABLE_LINKS; i++) {
                Location p = pivot.clone().add(0, -i * 0.35 - 0.3, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                cable.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null || pivot == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            double swayAng = Math.toRadians(15.0 * Math.sin(tick / 180.0 * Math.PI * 2));
            double cableLen = 3.5;

            for (int i = 0; i < cable.size(); i++) {
                double t = (i + 1) / (double) cable.size();
                double linkLen = cableLen * t;
                double bx = Math.sin(swayAng) * linkLen;
                double by = -Math.cos(swayAng) * linkLen;
                float sx = 0.4f * fade, sy = 0.42f * fade, sz = 0.4f * fade; // THICK
                cable.get(i).animateTo(
                        new Vector3f(-sx / 2, -sy / 2, -sz / 2),
                        new AxisAngle4f((float) swayAng, 0, 0, 1),
                        new Vector3f(sx, sy, sz), 4);
                Location lp = pivot.clone().add(bx, by, 0);
                cable.get(i).entity().teleport(lp);
            }
            double bx = Math.sin(swayAng) * cableLen;
            double by = -Math.cos(swayAng) * cableLen;
            for (int i = 0; i < ballParts.size(); i++) {
                double[] o = ballOffsets.get(i);
                Location bp = pivot.clone().add(bx + o[0], by + o[1], o[2]);
                ballParts.get(i).entity().teleport(bp);
                float sc = (i == 0 ? 0.85f : 0.55f) * fade; // bigger ball
                ballParts.get(i).animateTo(
                        new Vector3f(-sc / 2, -sc / 2, -sc / 2),
                        new AxisAngle4f((float) swayAng, 0, 0, 1),
                        new Vector3f(sc), 4);
            }
            Location p = pivot.clone().add(bx, by, 0);
            ambientParticles(w, p, tick);
            if (tick % 4 == 0) {
                // heavy chain whoosh trail along the cable
                DisplayBuilder.dustParticles(pivot.clone().add(bx * 0.6, by * 0.6, 0), 3, 0.3, 200, 110, 40, 1.4f);
            }
            if (tick % 180 == 90) {
                DisplayBuilder.playSound(p, Sound.BLOCK_CHAIN_HIT, 0.5f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AmbientWreckingBall(plugin); }
    }

    // ================================================================
    // 13. CHAIN CURTAIN BACKGROUND — "The Veil"
    //     24 THICK CHAIN side-by-side hanging chains forming a wall veil.
    //     Sine-wave sway across, lantern accents, heavy haze.
    // ================================================================
    public static class ChainCurtainBackground extends EnvironmentalAttack {
        private static final int CHAINS = 24;
        private final List<ItemDisplayHandle> curtain = new ArrayList<>();
        private final List<ItemDisplayHandle> accentLanterns = new ArrayList<>();
        private final double[] cx = new double[CHAINS];
        private final double[] accX = new double[4];
        private static final double WALL_Z = -11.0;
        private static final double CHAIN_Y = 4.5;
        private static final int SPAWN_END = 30;
        private static final int DISSIPATE_LEN = 30;

        public ChainCurtainBackground(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_curtain_background", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1100);
            config.setCooldownTicks(90);
            config.setDesignType("Ambient — chain curtain (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.7f, 0.45f);
            double startX = -5.75;
            for (int i = 0; i < CHAINS; i++) {
                cx[i] = startX + i * 0.5;
                Location p = c.clone().add(cx[i], CHAIN_Y, WALL_Z);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                curtain.add(h);
                spawnedEntities.add(h.entity());
            }
            // 4 lantern accents tucked into the veil
            for (int k = 0; k < 4; k++) {
                accX[k] = -4.5 + k * 3.0;
                Location p = c.clone().add(accX[k], CHAIN_Y - 0.5, WALL_Z + 0.4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.LANTERN));
                h.scale(0.001f, 0.001f, 0.001f).glow(LANTERN_GLOW[0], LANTERN_GLOW[1], LANTERN_GLOW[2]).interpolation(2, 0);
                accentLanterns.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            for (int i = 0; i < CHAINS; i++) {
                int group = i / 4;
                double swayDeg = 5.0 * Math.sin(tick / 60.0 * Math.PI * 2 - group * 0.6);
                double swayRad = Math.toRadians(swayDeg);
                float sx = 0.4f * fade, sy = 3.2f * fade, sz = 0.4f * fade; // THICK + tall
                curtain.get(i).animateTo(
                        new Vector3f((float)cx[i] - sx / 2, (float)CHAIN_Y - sy / 2, (float)WALL_Z - sz / 2),
                        new AxisAngle4f((float) swayRad, 1, 0, 0),
                        new Vector3f(sx, sy, sz), 4);
            }
            float ls = 0.6f * fade;
            for (int k = 0; k < 4; k++) {
                double sway = Math.toRadians(8.0 * Math.sin(tick / 60.0 * Math.PI * 2 - k * 0.6));
                Location lp = c.clone().add(accX[k], CHAIN_Y - 0.5, WALL_Z + 0.4);
                accentLanterns.get(k).animateTo(
                        new Vector3f((float)accX[k] - ls / 2, (float)(CHAIN_Y - 0.5) - ls / 2, (float)(WALL_Z + 0.4) - ls / 2),
                        new AxisAngle4f((float) sway, 0, 0, 1),
                        new Vector3f(ls), 4);
                w.spawnParticle(Particle.FLAME, lp, 3, 0.15, 0.15, 0.15, 0.005);
                DisplayBuilder.dustParticles(lp, 3, 0.25, 255, 200, 110, 1.2f);
            }
            // wide haze + rust drip across the veil every tick
            for (int s = 0; s < 4; s++) {
                double rx = -5.0 + Math.random() * 11.0;
                Location p = c.clone().add(rx, CHAIN_Y - 1.0 - Math.random() * 1.5, WALL_Z);
                rustDrip(w, p, tick);
            }
            w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, CHAIN_Y, WALL_Z), 2, 4.0, 0.8, 0.3, 0.005);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainCurtainBackground(plugin); }
    }

    // ================================================================
    // 14. NETHERITE INGOT CLUSTER — "The Hoard"
    //     8 NETHERITE_INGOT (scale 0.55) on a corner pile around a NETHERITE
    //     _BLOCK, slow Y-rotate, with THICK CHAIN coils + dense amethyst/soul
    //     spark aura so the hoard glows from across the arena.
    // ================================================================
    public static class NetheriteIngotCluster extends EnvironmentalAttack {
        private static final int INGOTS = 8;
        private static final int COILS = 3;
        private final List<ItemDisplayHandle> ingots = new ArrayList<>();
        private final List<ItemDisplayHandle> coils = new ArrayList<>();
        private ItemDisplayHandle block;
        private final double[] ix = new double[INGOTS];
        private final double[] iy = new double[INGOTS];
        private final double[] iz = new double[INGOTS];
        private final double[] rotSpeed = new double[INGOTS];
        private static final double CORNER_X = -10.0;
        private static final double CORNER_Z = 10.0;
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 30;

        public NetheriteIngotCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("netherite_ingot_cluster", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1200);
            config.setCooldownTicks(110);
            config.setDesignType("Ambient — netherite hoard (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.3f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.5f);

            // central netherite block (the hoard core)
            Location bp = c.clone().add(CORNER_X, 0.4, CORNER_Z);
            block = displayBuilder.spawnItem(bp, new ItemStack(Material.NETHERITE_BLOCK));
            block.scale(0.001f, 0.001f, 0.001f).glow(120, 90, 150).interpolation(2, 0);
            spawnedEntities.add(block.entity());

            for (int i = 0; i < INGOTS; i++) {
                double a = Math.PI * 2 * i / INGOTS;
                ix[i] = CORNER_X + Math.cos(a) * (0.8 + Math.random() * 0.5);
                iy[i] = 0.2 + Math.random() * 0.5;
                iz[i] = CORNER_Z + Math.sin(a) * (0.8 + Math.random() * 0.5);
                rotSpeed[i] = (1.0 + Math.random() * 2.0) * Math.PI / 180.0;
                Location p = c.clone().add(ix[i], iy[i], iz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHERITE_INGOT));
                h.scale(0.001f, 0.001f, 0.001f).glow(AMETHYST[0], AMETHYST[1], AMETHYST[2]).interpolation(2, 0);
                ingots.add(h);
                spawnedEntities.add(h.entity());
            }
            // THICK chain coils draped over the pile
            for (int k = 0; k < COILS; k++) {
                Location p = c.clone().add(CORNER_X, 0.6 + k * 0.4, CORNER_Z);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                coils.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            float bs = 0.8f * fade;
            float brot = (float) ((tick / 200.0) * Math.PI * 2);
            block.animateTo(
                    new Vector3f((float)CORNER_X - bs / 2, 0.4f - bs / 2, (float)CORNER_Z - bs / 2),
                    new AxisAngle4f(brot, 0, 1, 0),
                    new Vector3f(bs), 4);

            float sc = 0.55f * fade;
            for (int i = 0; i < INGOTS; i++) {
                float rot = (float) (rotSpeed[i] * tick);
                double bob = Math.sin(tick * 0.05 + i) * 0.06;
                ingots.get(i).animateTo(
                        new Vector3f((float)ix[i] - sc / 2, (float)(iy[i] + bob) - sc / 2, (float)iz[i] - sc / 2),
                        new AxisAngle4f(rot, 0, 1, 0),
                        new Vector3f(sc), 4);
            }
            float chs = 0.4f * fade, chsxz = 0.4f * fade;
            for (int k = 0; k < COILS; k++) {
                double a = tick / 100.0 * Math.PI * 2 + k * 2.0;
                double px = CORNER_X + Math.cos(a) * 0.6;
                double pz = CORNER_Z + Math.sin(a) * 0.6;
                coils.get(k).animateTo(
                        new Vector3f((float)px - chsxz / 2, (float)(0.6 + k * 0.4) - chs / 2, (float)pz - chsxz / 2),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(chsxz, chs, chsxz), 4);
            }

            // dense glowing hoard aura
            Location p = c.clone().add(CORNER_X, 0.5, CORNER_Z);
            w.spawnParticle(Particle.ELECTRIC_SPARK, p, 5, 0.7, 0.3, 0.7, 0.02);
            DisplayBuilder.dustParticles(p, 4, 0.6, 170, 80, 230, 1.4f);
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 3, 0.6, 0.25, 0.6, 0.01);
            }
            if (tick % 5 == 0) {
                w.spawnParticle(Particle.END_ROD, p.clone().add(0, 0.4, 0), 2, 0.5, 0.3, 0.5, 0.01);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new NetheriteIngotCluster(plugin); }
    }

    // ================================================================
    // 15. IRON AXE WEAPON RACK — "The Arsenal"
    //     Wall rack: 5 weapons (axes/sword/pickaxe) at scale 0.8 hung from a
    //     THICK CHAIN bar, plus 3 short THICK CHAIN drops. Gentle sway, rust
    //     glow, grindstone/smoke dressing.
    // ================================================================
    public static class IronAxeWeaponRack extends EnvironmentalAttack {
        private static final int WEAPONS = 5;
        private static final int DROPS = 3;
        private final List<ItemDisplayHandle> weapons = new ArrayList<>();
        private final List<double[]> weaponPos = new ArrayList<>(); // {x,y,z,tilt}
        private final List<ItemDisplayHandle> bar = new ArrayList<>();   // 3-seg thick bar
        private final List<ItemDisplayHandle> drops = new ArrayList<>(); // short chain drops
        private final double[] dropX = new double[DROPS];
        private static final double RACK_X = 0.0;
        private static final double RACK_Z = -10.5;
        private static final double RACK_Y = 3.0;
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 30;

        public IronAxeWeaponRack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_axe_weapon_rack", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1200);
            config.setCooldownTicks(110);
            config.setDesignType("Ambient — weapon rack (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.7f);

            Material[] mats = {Material.IRON_AXE, Material.IRON_SWORD, Material.IRON_AXE, Material.IRON_PICKAXE, Material.IRON_SHOVEL};
            double[] xs = {-1.2, -0.6, 0.0, 0.6, 1.2};
            for (int i = 0; i < WEAPONS; i++) {
                double wx = RACK_X + xs[i];
                double wy = RACK_Y + (Math.random() - 0.5) * 0.15;
                double tilt = (Math.random() - 0.5) * 0.2;
                Location p = c.clone().add(wx, wy, RACK_Z);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(mats[i]));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                weapons.add(h);
                weaponPos.add(new double[]{wx, wy, RACK_Z, tilt});
                spawnedEntities.add(h.entity());
            }

            // 3-segment THICK chain support bar across the top
            for (int k = 0; k < 3; k++) {
                Location bp = c.clone().add(RACK_X + (k - 1) * 0.9, RACK_Y + 0.8, RACK_Z);
                ItemDisplayHandle h = displayBuilder.spawnItem(bp, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                bar.add(h);
                spawnedEntities.add(h.entity());
            }
            // short THICK chain drops between weapons
            for (int k = 0; k < DROPS; k++) {
                dropX[k] = RACK_X + (k - 1) * 0.9;
                Location dp = c.clone().add(dropX[k], RACK_Y + 0.4, RACK_Z);
                ItemDisplayHandle h = displayBuilder.spawnItem(dp, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                drops.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            float sc = 0.8f * fade; // bigger weapons
            for (int i = 0; i < WEAPONS; i++) {
                double[] d = weaponPos.get(i);
                double sway = Math.toRadians(4.0 * Math.sin(tick / 80.0 * Math.PI * 2 + i * 0.4));
                weapons.get(i).animateTo(
                        new Vector3f((float)d[0] - sc / 2, (float)d[1] - sc / 2, (float)d[2] - sc / 2),
                        new AxisAngle4f((float) (d[3] + sway), 0, 0, 1),
                        new Vector3f(sc), 4);
            }
            // thick chain support bar (3 segments)
            float bs = 1.0f * fade, bsy = 0.4f * fade, bsz = 0.4f * fade;
            for (int k = 0; k < 3; k++) {
                double bx = RACK_X + (k - 1) * 0.9;
                bar.get(k).animateTo(
                        new Vector3f((float)bx - bs / 2, (float)(RACK_Y + 0.8) - bsy / 2, (float)RACK_Z - bsz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(bs, bsy, bsz), 4);
            }
            // short chain drops sway slightly
            float ds = 0.4f * fade, dsy = 0.5f * fade;
            for (int k = 0; k < DROPS; k++) {
                double sway = Math.toRadians(5.0 * Math.sin(tick / 70.0 * Math.PI * 2 + k));
                drops.get(k).animateTo(
                        new Vector3f((float)dropX[k] - ds / 2, (float)(RACK_Y + 0.4) - dsy / 2, (float)RACK_Z - ds / 2),
                        new AxisAngle4f((float) sway, 0, 0, 1),
                        new Vector3f(ds, dsy, ds), 4);
            }

            Location p = c.clone().add(RACK_X, RACK_Y, RACK_Z);
            ambientParticles(w, p, tick);
            if (tick % 30 == 0) {
                w.spawnParticle(Particle.LARGE_SMOKE, p, 2, 1.2, 0.4, 0.3, 0.005);
            }
            if (tick % 90 == 0) {
                DisplayBuilder.playSound(p, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronAxeWeaponRack(plugin); }
    }
}
