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
 * Damage = 0, damage-radius = 0 across the board — the framework's damage
 * pass is effectively a no-op for this category. All sensory feedback is
 * cosmetic: ItemDisplays + layered particles + atmospheric sound.
 *
 * Choreography per attack:
 *   PHASE 1 — SPAWN     : scale 0 → full, materialise from nothing
 *   PHASE 2 — IDLE      : continuous sway / bob / rotate via animateTo
 *   PHASE 3 — DISSIPATE : ambient close-out particles, fade
 *
 * Three or more particle layers per active tick (SMOKE, LARGE_SMOKE, DUST,
 * CRIT, ELECTRIC_SPARK, SOUL_FIRE_FLAME, FLAME, LAVA, FALLING_DUST). Sounds
 * limited to chain/lantern/grindstone/anvil ambient at low volume.
 *
 * NO BlockDisplays. NO potion effects. NO damage. Pure atmosphere.
 */
public final class ChainEnvironmental {
    private ChainEnvironmental() {}

    private static final String MODE_PATH = "modes/chain/attacks";

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

    /** Layered ambient particle pass — forge-orange dust + smoke + sparks. */
    private static void ambientParticles(World w, Location p, int tick) {
        if (tick % 3 == 0) {
            DisplayBuilder.dustParticles(p, 1, 0.25, 200, 110, 50, 1.0f);
        }
        if (tick % 5 == 0) {
            w.spawnParticle(Particle.SMOKE, p, 1, 0.2, 0.15, 0.2, 0.005);
        }
        if (tick % 11 == 0) {
            w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.15, 0.1, 0.15, 0.01);
        }
    }

    // ================================================================
    // 1. HANGING CHAIN CLUSTERS — "The Ceiling"
    //    14 anchor points, 4-6 chains each (~70 chains total). Each cluster
    //    sways on X with its own period. Scale-in over 20t on spawn.
    // ================================================================
    public static class HangingChainClusters extends EnvironmentalAttack {
        private static final int ANCHORS = 14;
        private final List<ItemDisplayHandle> chains = new ArrayList<>();
        private final List<double[]> chainData = new ArrayList<>(); // {ax, ay, az, baseAngleDeg, periodTicks, tiltDeg}
        private static final int SPAWN_END = 20;
        private static final int DISSIPATE_LEN = 30;

        public HangingChainClusters(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hanging_chain_clusters", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1200);
            config.setCooldownTicks(80);
            config.getClass(); // keep import
            config.setDesignType("Ambient — hanging chains (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.7f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.6f);

            for (int a = 0; a < ANCHORS; a++) {
                double aAng = Math.PI * 2 * a / ANCHORS + Math.random() * 0.2;
                double aRad = 8.0 + Math.random() * 4.5;
                double ax = Math.cos(aAng) * aRad;
                double az = Math.sin(aAng) * aRad;
                double ay = 5.5 + Math.random() * 1.5;
                int count = 4 + (int) (Math.random() * 3); // 4-6
                double basePeriod = 80 + Math.random() * 40;
                for (int i = 0; i < count; i++) {
                    double off = (i - count / 2.0) * 0.18;
                    double cx = ax + off;
                    double cz = az + (Math.random() - 0.5) * 0.18;
                    double cy = ay - Math.random() * 0.4;
                    double tilt = (Math.random() - 0.5) * 10.0; // ±5°
                    Location p = c.clone().add(cx, cy, cz);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(90, 90, 110).interpolation(2, 0);
                    chains.add(h);
                    chainData.add(new double[]{cx, cy, cz, 0, basePeriod + (Math.random() - 0.5) * 20, tilt});
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;

            float baseScaleX = 0.15f, baseScaleY = 1.5f, baseScaleZ = 0.15f;

            // Determine fade scale
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            for (int i = 0; i < chains.size(); i++) {
                double[] d = chainData.get(i);
                double swayDeg = 3.0 * Math.sin(tick / d[4] * Math.PI * 2 + i * 0.3);
                double swayRad = Math.toRadians(swayDeg + d[5]);
                float sx = baseScaleX * fade;
                float sy = baseScaleY * fade;
                float sz = baseScaleZ * fade;
                chains.get(i).animateTo(
                        new Vector3f((float)d[0] - sx / 2, (float)d[1] - sy / 2, (float)d[2] - sz / 2),
                        new AxisAngle4f((float) swayRad, 0, 0, 1),
                        new Vector3f(sx, sy, sz), 4);
                if (i % 7 == tick % 7) {
                    Location p = c.clone().add(d[0], d[1], d[2]);
                    ambientParticles(w, p, tick);
                }
            }

            if (tick == SPAWN_END) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.5f);
            }
            if (tick == dissipateStart) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new HangingChainClusters(plugin); }
    }

    // ================================================================
    // 2. SWAYING LANTERNS — "The Light"
    //    12 lanterns around perimeter, each on a CHAIN link, sway Z ±8°.
    // ================================================================
    public static class SwayingLanterns extends EnvironmentalAttack {
        private static final int LANTERNS = 12;
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();
        private final List<ItemDisplayHandle> chains = new ArrayList<>();
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
            DisplayBuilder.playSound(c, Sound.BLOCK_LANTERN_PLACE, 0.7f, 0.9f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.7f);

            for (int i = 0; i < LANTERNS; i++) {
                double ang = Math.PI * 2 * i / LANTERNS;
                double r = 9.0;
                lx[i] = Math.cos(ang) * r;
                ly[i] = 3.5;
                lz[i] = Math.sin(ang) * r;
                period[i] = 60 + Math.random() * 40;
                phase[i] = Math.random() * Math.PI * 2;

                Location lp = c.clone().add(lx[i], ly[i], lz[i]);
                ItemDisplayHandle lh = displayBuilder.spawnItem(lp, new ItemStack(Material.LANTERN));
                lh.scale(0.001f, 0.001f, 0.001f).glow(255, 180, 80).interpolation(2, 0);
                lanterns.add(lh);
                spawnedEntities.add(lh.entity());

                Location cp = c.clone().add(lx[i], ly[i] + 0.5, lz[i]);
                ItemDisplayHandle ch = displayBuilder.spawnItem(cp, new ItemStack(Material.CHAIN));
                ch.scale(0.001f, 0.001f, 0.001f).glow(90, 90, 110).interpolation(2, 0);
                chains.add(ch);
                spawnedEntities.add(ch.entity());
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

            float lScale = 0.4f * fade;
            float chScaleY = 0.5f * fade;
            float chScaleXZ = 0.1f * fade;

            for (int i = 0; i < LANTERNS; i++) {
                double sway = Math.toRadians(8.0 * Math.sin(tick / period[i] * Math.PI * 2 + phase[i]));
                lanterns.get(i).animateTo(
                        new Vector3f((float)lx[i] - lScale / 2, (float)ly[i] - lScale / 2, (float)lz[i] - lScale / 2),
                        new AxisAngle4f((float) sway, 1, 0, 0),
                        new Vector3f(lScale), 4);
                chains.get(i).animateTo(
                        new Vector3f((float)lx[i] - chScaleXZ / 2, (float)(ly[i] + 0.5) - chScaleY / 2, (float)lz[i] - chScaleXZ / 2),
                        new AxisAngle4f((float) sway, 1, 0, 0),
                        new Vector3f(chScaleXZ, chScaleY, chScaleXZ), 4);
                if (tick % 6 == i % 6) {
                    Location lp = c.clone().add(lx[i], ly[i], lz[i]);
                    w.spawnParticle(Particle.FLAME, lp, 1, 0.05, 0.05, 0.05, 0.005);
                    if (tick % 12 == 0) {
                        DisplayBuilder.dustParticles(lp, 1, 0.15, 255, 200, 100, 0.8f);
                    }
                    if (tick % 18 == 0) {
                        w.spawnParticle(Particle.SMOKE, lp.clone().add(0, 0.4, 0), 1, 0.05, 0.05, 0.05, 0.005);
                    }
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SwayingLanterns(plugin); }
    }

    // ================================================================
    // 3. DANGLING IRON WEIGHTS — "The Measures"
    //    9 weights, IRON_BLOCK item + 3 CHAIN above, bob Y ±0.1.
    // ================================================================
    public static class DanglingIronWeights extends EnvironmentalAttack {
        private static final int WEIGHTS = 9;
        private final List<ItemDisplayHandle> weights = new ArrayList<>();
        private final List<ItemDisplayHandle> chainsAbove = new ArrayList<>(); // 3 per weight
        private final double[] wx = new double[WEIGHTS];
        private final double[] wy = new double[WEIGHTS];
        private final double[] wz = new double[WEIGHTS];
        private final double[] period = new double[WEIGHTS];
        private final double[] phase = new double[WEIGHTS];
        private final int[] stagger = new int[WEIGHTS];
        private static final int SPAWN_LEN = 100; // staggered over 5s
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
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.6f);

            for (int i = 0; i < WEIGHTS; i++) {
                double ang = Math.PI * 2 * i / WEIGHTS + Math.random() * 0.2;
                double r = 6.0 + Math.random() * 4.0;
                wx[i] = Math.cos(ang) * r;
                wz[i] = Math.sin(ang) * r;
                wy[i] = 1.5 + Math.random() * 2.0;
                period[i] = 60 + Math.random() * 20;
                phase[i] = Math.random() * Math.PI * 2;
                stagger[i] = (int) (Math.random() * SPAWN_LEN * 0.7);

                Location wp = c.clone().add(wx[i], wy[i], wz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(wp, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 200).interpolation(2, 0);
                weights.add(h);
                spawnedEntities.add(h.entity());

                for (int k = 0; k < 3; k++) {
                    Location cp = c.clone().add(wx[i], wy[i] + 0.6 + k * 0.5, wz[i]);
                    ItemDisplayHandle ch = displayBuilder.spawnItem(cp, new ItemStack(Material.CHAIN));
                    ch.scale(0.001f, 0.001f, 0.001f).glow(90, 90, 110).interpolation(2, 0);
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

                double yOff = 0.1 * Math.sin(tick / period[i] * Math.PI * 2 + phase[i]);
                float sx = 0.35f * fade, sy = 0.55f * fade, sz = 0.35f * fade;
                weights.get(i).animateTo(
                        new Vector3f((float)wx[i] - sx / 2, (float)(wy[i] + yOff) - sy / 2, (float)wz[i] - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 4);
                for (int k = 0; k < 3; k++) {
                    float cxs = 0.12f * fade, cys = (0.5f - (float) Math.abs(yOff) * 0.3f) * fade;
                    chainsAbove.get(i * 3 + k).animateTo(
                            new Vector3f((float)wx[i] - cxs / 2, (float)(wy[i] + 0.6 + k * 0.45 + yOff * 0.5) - cys / 2, (float)wz[i] - cxs / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(cxs, cys, cxs), 4);
                }
                if (tick % 8 == i % 8) {
                    Location wp = c.clone().add(wx[i], wy[i] + yOff, wz[i]);
                    ambientParticles(w, wp, tick);
                    if (tick % 80 == i * 8 % 80) {
                        DisplayBuilder.playSound(wp, Sound.BLOCK_CHAIN_HIT, 0.3f, 0.7f + (float) Math.random() * 0.2f);
                    }
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DanglingIronWeights(plugin); }
    }

    // ================================================================
    // 4. CHAIN LOOP WALL MOUNTS — "The Decoration"
    //    12 chain-ring loops mounted on walls, each rotates on wall-perp axis.
    // ================================================================
    public static class ChainLoopWallMounts extends EnvironmentalAttack {
        private static final int LOOPS = 12;
        private static final int LINKS_PER_LOOP = 6;
        private final List<ItemDisplayHandle> links = new ArrayList<>();
        private final double[] loopCx = new double[LOOPS];
        private final double[] loopCy = new double[LOOPS];
        private final double[] loopCz = new double[LOOPS];
        private final double[] loopAxisX = new double[LOOPS]; // outward normal
        private final double[] loopAxisZ = new double[LOOPS];
        private final double[] rotSpeed = new double[LOOPS];
        private static final double LOOP_RADIUS = 0.4;
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
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.7f);

            for (int l = 0; l < LOOPS; l++) {
                // place on 4 walls at varying heights/positions
                int wall = l % 4;
                double along = -4.0 + Math.random() * 8.0;
                double height = 1.5 + Math.random() * 3.0;
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

                // Compute tangent vectors (perpendicular to axis, in the wall plane)
                double tanAx = -axisZ;
                double tanAz = axisX;
                double tanBx = 0;
                double tanBy = 1;
                double tanBz = 0;
                for (int i = 0; i < LINKS_PER_LOOP; i++) {
                    double a = Math.PI * 2 * i / LINKS_PER_LOOP;
                    double px = cx + (tanAx * Math.cos(a) + tanBx * Math.sin(a)) * LOOP_RADIUS;
                    double py = height + (0 * Math.cos(a) + tanBy * Math.sin(a)) * LOOP_RADIUS;
                    double pz = cz + (tanAz * Math.cos(a) + tanBz * Math.sin(a)) * LOOP_RADIUS;
                    Location lp = c.clone().add(px, py, pz);
                    ItemDisplayHandle h = displayBuilder.spawnItem(lp, new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(90, 90, 110).interpolation(2, 0);
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
                    float s = 0.16f * fade;
                    int idx = l * LINKS_PER_LOOP + i;
                    links.get(idx).animateTo(
                            new Vector3f((float)px - s / 2, (float)py - s / 2, (float)pz - s / 2),
                            new AxisAngle4f((float) a, (float) loopAxisX[l], 0, (float) loopAxisZ[l]),
                            new Vector3f(s, s, s), 3);
                }
                if (tick % 15 == l % 15) {
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
    //    8 IRON_INGOT lazy-orbiting at Y=2, each at own radius/speed.
    // ================================================================
    public static class IronIngotField extends EnvironmentalAttack {
        private static final int INGOTS = 8;
        private final List<ItemDisplayHandle> ingots = new ArrayList<>();
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
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 1.3f);
            for (int i = 0; i < INGOTS; i++) {
                radius[i] = 2.0 + Math.random() * 2.5;
                phase[i] = Math.random() * Math.PI * 2;
                angSpeed[i] = (0.3 + Math.random() * 0.7) * (Math.random() < 0.5 ? -1 : 1) * Math.PI / 180.0;
                Location p = c.clone().add(Math.cos(phase[i]) * radius[i], 2.0, Math.sin(phase[i]) * radius[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_INGOT));
                h.scale(0.001f, 0.001f, 0.001f).glow(220, 220, 235).interpolation(2, 0);
                ingots.add(h);
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

            float s = 0.3f * fade;
            for (int i = 0; i < INGOTS; i++) {
                double ang = phase[i] + angSpeed[i] * tick;
                double x = Math.cos(ang) * radius[i];
                double z = Math.sin(ang) * radius[i];
                double y = 2.0 + Math.sin(tick * 0.05 + i) * 0.1;
                float spin = (float) Math.toRadians(10.0 * tick);
                ingots.get(i).animateTo(
                        new Vector3f((float)x - s / 2, (float)y - s / 2, (float)z - s / 2),
                        new AxisAngle4f(spin, 0, 1, 0),
                        new Vector3f(s, s, s), 4);
                Location p = c.clone().add(x, y, z);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.05, 0.05, 0.05, 0.01);
                if (tick % 6 == i % 6) {
                    DisplayBuilder.dustParticles(p, 1, 0.1, 220, 220, 235, 0.7f);
                }
                if (tick % 14 == i % 14) {
                    w.spawnParticle(Particle.SMOKE, p, 1, 0.1, 0.05, 0.1, 0.005);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronIngotField(plugin); }
    }

    // ================================================================
    // 6. CHAIN HOOK WALL — "The Workshop"
    //    8 IRON_PICKAXE hooks mounted on wall, sway Z ±5°.
    // ================================================================
    public static class ChainHookWall extends EnvironmentalAttack {
        private static final int HOOKS = 8;
        private final List<ItemDisplayHandle> hooks = new ArrayList<>();
        private final double[] hx = new double[HOOKS];
        private final double[] hz = new double[HOOKS];
        private final double[] period = new double[HOOKS];
        private final double[] phase = new double[HOOKS];
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
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 0.4f);

            double wallZ = 11.0;
            double startX = -5.25;
            for (int i = 0; i < HOOKS; i++) {
                hx[i] = startX + i * 1.5;
                hz[i] = wallZ;
                period[i] = 70 + Math.random() * 30;
                phase[i] = Math.random() * Math.PI * 2;
                Location p = c.clone().add(hx[i], 2.0, hz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_PICKAXE));
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 200).interpolation(2, 0);
                hooks.add(h);
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

            float s = 0.7f * fade;
            for (int i = 0; i < HOOKS; i++) {
                double sway = Math.toRadians(5.0 * Math.sin(tick / period[i] * Math.PI * 2 + phase[i]));
                hooks.get(i).animateTo(
                        new Vector3f((float)hx[i] - s / 2, (float)2.0 - s / 2, (float)hz[i] - s / 2),
                        new AxisAngle4f((float) sway, 1, 0, 0),
                        new Vector3f(s, s, s), 4);
                if (tick % 10 == i % 10) {
                    Location p = c.clone().add(hx[i], 2.0, hz[i]);
                    ambientParticles(w, p, tick);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainHookWall(plugin); }
    }

    // ================================================================
    // 7. ANCHOR DISPLAY — "The Trophy"
    //    Large anchor on pedestal, slowly rotates Y, CHAIN hanging.
    // ================================================================
    public static class AnchorDisplay extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> parts = new ArrayList<>();
        private final List<double[]> partPos = new ArrayList<>(); // {x,y,z,scale}
        private ItemDisplayHandle chainPart;
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
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.5f);

            // Anchor geometry: vertical shaft (3 stack), top ring (4 around), bottom hooks (2 angled)
            double offX = 8.0, offZ = 0;
            // shaft 3 stacked IRON_BLOCK
            for (int s = 0; s < 3; s++) {
                addPart(c, offX, 1.5 + s * 0.4, offZ, Material.IRON_BLOCK, 0.3f);
            }
            // top ring 4 IRON_BLOCK around
            double[][] ring = {{0.3, 0}, {-0.3, 0}, {0, 0.3}, {0, -0.3}};
            for (double[] r : ring) {
                addPart(c, offX + r[0], 3.0, offZ + r[1], Material.IRON_BLOCK, 0.18f);
            }
            // bottom hooks: 2 angled IRON_BLOCK
            addPart(c, offX + 0.5, 1.3, offZ, Material.IRON_BLOCK, 0.22f);
            addPart(c, offX - 0.5, 1.3, offZ, Material.IRON_BLOCK, 0.22f);
            // central crossbar
            addPart(c, offX, 2.5, offZ, Material.IRON_BLOCK, 0.28f);

            // CHAIN hanging from top ring
            Location ch = c.clone().add(offX, 3.4, offZ);
            chainPart = displayBuilder.spawnItem(ch, new ItemStack(Material.CHAIN));
            chainPart.scale(0.001f, 0.001f, 0.001f).glow(90, 90, 110).interpolation(2, 0);
            spawnedEntities.add(chainPart.entity());
        }

        private void addPart(Location c, double x, double y, double z, Material m, float sc) {
            Location p = c.clone().add(x, y, z);
            ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(m));
            h.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 200).interpolation(2, 0);
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

            // 360° per 120t → 2π/120 per tick = 0.0524 rad/tick
            float rotY = (float) ((tick / 120.0) * Math.PI * 2);
            // Rotate parts around anchor pivot (offset 8, 0)
            double pivotX = 8.0;
            for (int i = 0; i < parts.size(); i++) {
                double[] d = partPos.get(i);
                double localX = d[0] - pivotX;
                double cosR = Math.cos(rotY), sinR = Math.sin(rotY);
                double nx = pivotX + localX * cosR - d[2] * sinR;
                double nz = d[2] * cosR + localX * sinR;
                float sc = (float) d[3] * fade;
                parts.get(i).animateTo(
                        new Vector3f((float)nx - sc / 2, (float)d[1] - sc / 2, (float)nz - sc / 2),
                        new AxisAngle4f(rotY, 0, 1, 0),
                        new Vector3f(sc), 4);
            }
            float chs = 0.4f * fade;
            chainPart.animateTo(
                    new Vector3f((float)pivotX - chs / 2, 3.4f - chs * 1.5f, -chs / 2),
                    new AxisAngle4f(rotY, 0, 1, 0),
                    new Vector3f(chs * 0.5f, chs * 1.5f, chs * 0.5f), 4);

            if (tick % 12 == 0) {
                Location p = c.clone().add(pivotX, 2.0, 0);
                ambientParticles(w, p, tick);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AnchorDisplay(plugin); }
    }

    // ================================================================
    // 8. SWINGING CAGE DOOR — "The Entrance"
    //    Frame (4 IRON_BLOCK) + door (4 IRON_BLOCK bars) rotates ±60°.
    // ================================================================
    public static class SwingingCageDoor extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> frame = new ArrayList<>();
        private final List<ItemDisplayHandle> bars = new ArrayList<>();
        private final double[][] frameOff = new double[4][3];
        private final double[][] barOff = new double[4][3];
        private static final int SPAWN_END = 25;
        private static final int DISSIPATE_LEN = 30;
        private static final double DOOR_X = -8.0;
        private static final double DOOR_Z = 0;
        private static final double DOOR_Y = 2.0;

        public SwingingCageDoor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("swinging_cage_door", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            applyAmbientDefaults(config);
            config.setDurationTicks(1100);
            config.setCooldownTicks(100);
            config.setDesignType("Ambient — cage door (decorative, no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.5f);
            // Frame edges: top, bottom, left, right
            double[][] f = {{0, 1.0, 0}, {0, -1.0, 0}, {-0.6, 0, 0}, {0.6, 0, 0}};
            for (int i = 0; i < 4; i++) {
                frameOff[i] = f[i];
                Location p = c.clone().add(DOOR_X + f[i][0], DOOR_Y + f[i][1], DOOR_Z + f[i][2]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(120, 120, 140).interpolation(2, 0);
                frame.add(h);
                spawnedEntities.add(h.entity());
            }
            // Door bars (4 vertical bars, will pivot around hinge at left edge)
            double[][] b = {{-0.4, 0, 0}, {-0.15, 0, 0}, {0.15, 0, 0}, {0.4, 0, 0}};
            for (int i = 0; i < 4; i++) {
                barOff[i] = b[i];
                Location p = c.clone().add(DOOR_X + b[i][0], DOOR_Y, DOOR_Z);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 200).interpolation(2, 0);
                bars.add(h);
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

            // Frame: static (with scale fade)
            for (int i = 0; i < 4; i++) {
                double[] o = frameOff[i];
                float sx = (Math.abs(o[0]) > 0.5 ? 0.08f : 1.0f) * fade;
                float sy = (Math.abs(o[1]) > 0.5 ? 0.08f : 1.0f) * fade;
                float sz = 0.08f * fade;
                frame.get(i).animateTo(
                        new Vector3f((float)(DOOR_X + o[0]) - sx / 2, (float)(DOOR_Y + o[1]) - sy / 2, (float)(DOOR_Z + o[2]) - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 4);
            }

            // Door: rotate ±60° on Y (100-tick period). Hinge at left edge (x = -0.6 local)
            double doorAngle = Math.toRadians(60.0 * Math.sin(tick / 100.0 * Math.PI * 2));
            for (int i = 0; i < 4; i++) {
                double[] o = barOff[i];
                double localX = o[0] - (-0.6); // distance from hinge
                double cosA = Math.cos(doorAngle), sinA = Math.sin(doorAngle);
                double nx = -0.6 + localX * cosA;
                double nz = localX * sinA;
                float sx = 0.08f * fade, sy = 0.8f * fade, sz = 0.08f * fade;
                bars.get(i).animateTo(
                        new Vector3f((float)(DOOR_X + nx) - sx / 2, (float)DOOR_Y - sy / 2, (float)(DOOR_Z + nz) - sz / 2),
                        new AxisAngle4f((float) doorAngle, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 4);
            }

            if (tick % 100 == 50 || tick % 100 == 0) {
                Location p = c.clone().add(DOOR_X, DOOR_Y, DOOR_Z);
                DisplayBuilder.playSound(p, Sound.BLOCK_CHAIN_HIT, 0.4f, 0.6f);
            }
            if (tick % 8 == 0) {
                Location p = c.clone().add(DOOR_X, DOOR_Y, DOOR_Z);
                ambientParticles(w, p, tick);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SwingingCageDoor(plugin); }
    }

    // ================================================================
    // 9. IRON GEAR DISPLAY — "The Machine"
    //    3 gears on wall, slowly rotate, adjacent opposite direction.
    // ================================================================
    public static class IronGearDisplay extends EnvironmentalAttack {
        private static final int GEARS = 3;
        private static final int TEETH = 8;
        private static final int SPOKES = 4;
        private final List<ItemDisplayHandle> teeth = new ArrayList<>();
        private final List<ItemDisplayHandle> spokes = new ArrayList<>();
        private final double[] gx = new double[GEARS];
        private final double[] gy = new double[GEARS];
        private final double[] gz = new double[GEARS];
        private final double[] gRot = new double[GEARS]; // rad/tick
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
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.7f);

            double[] xs = {-3.0, 0.0, 3.0};
            for (int g = 0; g < GEARS; g++) {
                gx[g] = xs[g];
                gy[g] = 4.0;
                gz[g] = 10.0;
                gRot[g] = (g % 2 == 0 ? 1 : -1) * (Math.PI / 180.0) * (0.6 + g * 0.2);

                for (int t = 0; t < TEETH; t++) {
                    double a = Math.PI * 2 * t / TEETH;
                    double px = gx[g] + Math.cos(a) * 0.7;
                    double py = gy[g] + Math.sin(a) * 0.7;
                    double pz = gz[g];
                    Location p = c.clone().add(px, py, pz);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE));
                    h.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 200).interpolation(2, 0);
                    teeth.add(h);
                    spawnedEntities.add(h.entity());
                }
                for (int s = 0; s < SPOKES; s++) {
                    double a = Math.PI * 2 * s / SPOKES + Math.PI / 4;
                    double px = gx[g] + Math.cos(a) * 0.35;
                    double py = gy[g] + Math.sin(a) * 0.35;
                    double pz = gz[g];
                    Location p = c.clone().add(px, py, pz);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                    h.scale(0.001f, 0.001f, 0.001f).glow(160, 160, 180).interpolation(2, 0);
                    spokes.add(h);
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
                    double px = gx[g] + Math.cos(a) * 0.7;
                    double py = gy[g] + Math.sin(a) * 0.7;
                    double pz = gz[g];
                    float sx = 0.5f * fade, sy = 0.18f * fade, sz = 0.1f * fade;
                    int idx = g * TEETH + t;
                    teeth.get(idx).animateTo(
                            new Vector3f((float)px - sx / 2, (float)py - sy / 2, (float)pz - sz / 2),
                            new AxisAngle4f((float) a, 0, 0, 1),
                            new Vector3f(sx, sy, sz), 4);
                }
                for (int s = 0; s < SPOKES; s++) {
                    double a = Math.PI * 2 * s / SPOKES + Math.PI / 4 + rot;
                    double px = gx[g] + Math.cos(a) * 0.35;
                    double py = gy[g] + Math.sin(a) * 0.35;
                    double pz = gz[g];
                    float sc = 0.18f * fade;
                    int idx = g * SPOKES + s;
                    spokes.get(idx).animateTo(
                            new Vector3f((float)px - sc / 2, (float)py - sc / 2, (float)pz - sc / 2),
                            new AxisAngle4f((float) a, 0, 0, 1),
                            new Vector3f(sc), 4);
                }
                if (tick % 20 == g % 20) {
                    Location p = c.clone().add(gx[g], gy[g], gz[g]);
                    ambientParticles(w, p, tick);
                    DisplayBuilder.playSound(p, Sound.BLOCK_GRINDSTONE_USE, 0.2f, 0.5f);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronGearDisplay(plugin); }
    }

    // ================================================================
    // 10. CHAIN SPIRAL AMBIENT — "The Coil"
    //     12 CHAIN in vertical helix (r=0.5, 2 turns, height 1.2). Rotates Y.
    // ================================================================
    public static class ChainSpiralAmbient extends EnvironmentalAttack {
        private static final int LINKS = 12;
        private static final int SPIRALS = 3;
        private final List<ItemDisplayHandle> links = new ArrayList<>();
        private final double[] sx = new double[SPIRALS];
        private final double[] sz = new double[SPIRALS];
        private final double[] sy = new double[SPIRALS];
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
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.6f);

            double[][] corners = {{8.0, 8.0}, {-8.0, 8.0}, {8.0, -8.0}};
            for (int s = 0; s < SPIRALS; s++) {
                sx[s] = corners[s][0];
                sz[s] = corners[s][1];
                sy[s] = 3.8;
                for (int i = 0; i < LINKS; i++) {
                    double t = i / (double)(LINKS - 1);
                    double a = t * Math.PI * 4; // 2 turns
                    double y = sy[s] - t * 1.2;
                    double px = sx[s] + Math.cos(a) * 0.5;
                    double pz = sz[s] + Math.sin(a) * 0.5;
                    Location p = c.clone().add(px, y, pz);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(100, 100, 120).interpolation(2, 0);
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

            double rot = tick / 80.0 * Math.PI * 2; // one revolution / 80t
            double bob = Math.sin(tick / 40.0 * Math.PI * 2) * 0.05;

            for (int s = 0; s < SPIRALS; s++) {
                for (int i = 0; i < LINKS; i++) {
                    double tt = i / (double)(LINKS - 1);
                    double a = tt * Math.PI * 4 + rot;
                    double y = sy[s] + bob - tt * 1.2;
                    double px = sx[s] + Math.cos(a) * 0.5;
                    double pz = sz[s] + Math.sin(a) * 0.5;
                    float sc = 0.16f * fade;
                    float scy = 0.32f * fade;
                    int idx = s * LINKS + i;
                    links.get(idx).animateTo(
                            new Vector3f((float)px - sc / 2, (float)y - scy / 2, (float)pz - sc / 2),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(sc, scy, sc), 4);
                }
                if (tick % 12 == s % 12) {
                    Location p = c.clone().add(sx[s], sy[s], sz[s]);
                    ambientParticles(w, p, tick);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainSpiralAmbient(plugin); }
    }

    // ================================================================
    // 11. IRON SHACKLE RING — "The Binding"
    //     12 CHAIN circle (r=0.8) on floor + 2 IRON_BLOCK break points.
    // ================================================================
    public static class IronShackleRing extends EnvironmentalAttack {
        private static final int LINKS = 12;
        private final List<ItemDisplayHandle> links = new ArrayList<>();
        private final List<ItemDisplayHandle> breakPts = new ArrayList<>();
        private static final double RING_R = 0.8;
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
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 0.4f, 0.4f);

            for (int i = 0; i < LINKS; i++) {
                if (i == 0 || i == 1) continue; // gap = break point
                double a = Math.PI * 2 * i / LINKS;
                double px = RING_X + Math.cos(a) * RING_R;
                double pz = RING_Z + Math.sin(a) * RING_R;
                Location p = c.clone().add(px, 0.05, pz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(90, 90, 110).interpolation(2, 0);
                links.add(h);
                spawnedEntities.add(h.entity());
            }
            // 2 IRON_BLOCK at break point
            for (int k = 0; k < 2; k++) {
                double a = Math.PI * 2 * (k * 0.6 - 0.3) / LINKS;
                double px = RING_X + Math.cos(a) * RING_R + (k == 0 ? 0.12 : -0.12);
                double pz = RING_Z + Math.sin(a) * RING_R + (k == 0 ? 0.08 : -0.08);
                Location p = c.clone().add(px, 0.05, pz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 200).interpolation(2, 0);
                breakPts.add(h);
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

            double rot = Math.toRadians(0.5 * tick); // 0.5°/tick
            int idx = 0;
            for (int i = 0; i < LINKS; i++) {
                if (i == 0 || i == 1) continue;
                double a = Math.PI * 2 * i / LINKS + rot;
                double px = RING_X + Math.cos(a) * RING_R;
                double pz = RING_Z + Math.sin(a) * RING_R;
                float sc = 0.16f * fade;
                links.get(idx).animateTo(
                        new Vector3f((float)px - sc / 2, 0.05f - sc / 2, (float)pz - sc / 2),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(sc, sc, sc), 4);
                idx++;
            }
            // break points
            for (int k = 0; k < 2; k++) {
                double a = Math.PI * 2 * (k * 0.6 - 0.3) / LINKS + rot;
                double px = RING_X + Math.cos(a) * RING_R + (k == 0 ? 0.12 : -0.12);
                double pz = RING_Z + Math.sin(a) * RING_R + (k == 0 ? 0.08 : -0.08);
                float sc = 0.18f * fade;
                breakPts.get(k).animateTo(
                        new Vector3f((float)px - sc / 2, 0.05f - sc / 2, (float)pz - sc / 2),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(sc), 4);
            }

            if (tick % 15 == 0) {
                Location p = c.clone().add(RING_X, 0.1, RING_Z);
                ambientParticles(w, p, tick);
                w.spawnParticle(Particle.FALLING_DUST, p, 1, RING_R, 0.05, RING_R, 0.0, Material.IRON_BLOCK.createBlockData());
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronShackleRing(plugin); }
    }

    // ================================================================
    // 12. AMBIENT WRECKING BALL — "The Resting Beast"
    //     Suspended ball (NETHERITE_BLOCK x 7) + 8 CHAIN cable. ±15° swing.
    // ================================================================
    public static class AmbientWreckingBall extends EnvironmentalAttack {
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
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.7f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.4f, 0.4f);

            pivot = c.clone().add(0, 6.5, 0);
            // Sphere geometry (scaled 0.7): 1 core + 6 poles
            double r = 0.45;
            double[][] poles = {{0, 0, 0}, {r, 0, 0}, {-r, 0, 0}, {0, r, 0}, {0, -r, 0}, {0, 0, r}, {0, 0, -r}};
            for (double[] o : poles) {
                Location p = pivot.clone().add(0, -3.0, 0).add(o[0], o[1], o[2]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHERITE_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(40, 40, 50).interpolation(2, 0);
                ballParts.add(h);
                ballOffsets.add(o);
                spawnedEntities.add(h.entity());
            }
            // 8 CHAIN cable from pivot down
            for (int i = 0; i < 8; i++) {
                Location p = pivot.clone().add(0, -i * 0.4 - 0.3, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(90, 90, 110).interpolation(2, 0);
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

            // ±15° at 180-tick period
            double swayAng = Math.toRadians(15.0 * Math.sin(tick / 180.0 * Math.PI * 2));
            double cableLen = 3.0;

            // Cable links
            for (int i = 0; i < cable.size(); i++) {
                double t = (i + 1) / (double) cable.size();
                double linkLen = cableLen * t;
                double bx = Math.sin(swayAng) * linkLen;
                double by = -Math.cos(swayAng) * linkLen;
                float sx = 0.16f * fade, sy = 0.32f * fade, sz = 0.16f * fade;
                cable.get(i).animateTo(
                        new Vector3f((float)bx - sx / 2, (float)by - sy / 2, -sz / 2),
                        new AxisAngle4f((float) swayAng, 0, 0, 1),
                        new Vector3f(sx, sy, sz), 4);
                // Note: cable items are relative to pivot's spawn location;
                // we re-base via teleport for stable position
                Location lp = pivot.clone().add(bx, by, 0);
                cable.get(i).entity().teleport(lp);
            }
            // Ball at end of cable
            double bx = Math.sin(swayAng) * cableLen;
            double by = -Math.cos(swayAng) * cableLen;
            for (int i = 0; i < ballParts.size(); i++) {
                double[] o = ballOffsets.get(i);
                Location bp = pivot.clone().add(bx + o[0], by + o[1], o[2]);
                ballParts.get(i).entity().teleport(bp);
                float sc = (i == 0 ? 0.65f : 0.42f) * fade;
                ballParts.get(i).animateTo(
                        new Vector3f(-sc / 2, -sc / 2, -sc / 2),
                        new AxisAngle4f((float) swayAng, 0, 0, 1),
                        new Vector3f(sc), 4);
            }
            // Ambient
            if (tick % 8 == 0) {
                Location p = pivot.clone().add(bx, by, 0);
                ambientParticles(w, p, tick);
            }
            if (tick % 180 == 90) {
                DisplayBuilder.playSound(pivot.clone().add(bx, by, 0), Sound.BLOCK_CHAIN_HIT, 0.4f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AmbientWreckingBall(plugin); }
    }

    // ================================================================
    // 13. CHAIN CURTAIN BACKGROUND — "The Veil"
    //     20 CHAIN side-by-side hanging chains. Sine-wave sway across.
    // ================================================================
    public static class ChainCurtainBackground extends EnvironmentalAttack {
        private static final int CHAINS = 20;
        private final List<ItemDisplayHandle> curtain = new ArrayList<>();
        private final double[] cx = new double[CHAINS];
        private static final double WALL_Z = -11.0;
        private static final double CHAIN_Y = 3.5;
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
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 0.45f);
            double startX = -3.8;
            for (int i = 0; i < CHAINS; i++) {
                cx[i] = startX + i * 0.4;
                Location p = c.clone().add(cx[i], CHAIN_Y, WALL_Z);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(80, 80, 100).interpolation(2, 0);
                curtain.add(h);
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
                float sx = 0.14f * fade, sy = 2.5f * fade, sz = 0.14f * fade;
                curtain.get(i).animateTo(
                        new Vector3f((float)cx[i] - sx / 2, (float)CHAIN_Y - sy / 2, (float)WALL_Z - sz / 2),
                        new AxisAngle4f((float) swayRad, 1, 0, 0),
                        new Vector3f(sx, sy, sz), 4);
            }
            if (tick % 10 == 0) {
                int j = (tick / 10) % CHAINS;
                Location p = c.clone().add(cx[j], CHAIN_Y - 0.5, WALL_Z);
                ambientParticles(w, p, tick);
            }
            if (tick % 24 == 0) {
                Location p = c.clone().add(0, CHAIN_Y, WALL_Z);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 3.0, 0.5, 0.2, 0.005);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainCurtainBackground(plugin); }
    }

    // ================================================================
    // 14. NETHERITE INGOT CLUSTER — "The Hoard"
    //     6 NETHERITE_INGOT scattered, slow Y-rotate, ELECTRIC_SPARK pulses.
    // ================================================================
    public static class NetheriteIngotCluster extends EnvironmentalAttack {
        private static final int INGOTS = 6;
        private final List<ItemDisplayHandle> ingots = new ArrayList<>();
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
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.4f, 0.3f);
            for (int i = 0; i < INGOTS; i++) {
                ix[i] = CORNER_X + (Math.random() - 0.5) * 1.0;
                iy[i] = 0.15 + Math.random() * 0.3;
                iz[i] = CORNER_Z + (Math.random() - 0.5) * 1.0;
                rotSpeed[i] = (1.0 + Math.random() * 2.0) * Math.PI / 180.0;
                Location p = c.clone().add(ix[i], iy[i], iz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHERITE_INGOT));
                h.scale(0.001f, 0.001f, 0.001f).glow(80, 60, 100).interpolation(2, 0);
                ingots.add(h);
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

            float sc = 0.4f * fade;
            for (int i = 0; i < INGOTS; i++) {
                float rot = (float) (rotSpeed[i] * tick);
                ingots.get(i).animateTo(
                        new Vector3f((float)ix[i] - sc / 2, (float)iy[i] - sc / 2, (float)iz[i] - sc / 2),
                        new AxisAngle4f(rot, 0, 1, 0),
                        new Vector3f(sc), 4);
            }
            if (tick % 30 == 0) {
                Location p = c.clone().add(CORNER_X, 0.4, CORNER_Z);
                w.spawnParticle(Particle.ELECTRIC_SPARK, p, 3, 0.5, 0.15, 0.5, 0.02);
                ambientParticles(w, p, tick);
                if (tick % 90 == 0) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 2, 0.4, 0.15, 0.4, 0.01);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new NetheriteIngotCluster(plugin); }
    }

    // ================================================================
    // 15. IRON AXE WEAPON RACK — "The Arsenal"
    //     Wall rack: 2 IRON_AXE + 1 IRON_SWORD + 1 IRON_PICKAXE + 1 CHAIN bar.
    //     Static (no animation), pure visual.
    // ================================================================
    public static class IronAxeWeaponRack extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> weapons = new ArrayList<>();
        private final List<double[]> weaponPos = new ArrayList<>(); // {x,y,z,tilt}
        private ItemDisplayHandle barChain;
        private static final double RACK_X = 0.0;
        private static final double RACK_Z = -10.5;
        private static final double RACK_Y = 2.5;
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
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.7f);

            Material[] mats = {Material.IRON_AXE, Material.IRON_SWORD, Material.IRON_AXE, Material.IRON_PICKAXE};
            double[] xs = {-0.9, -0.3, 0.3, 0.9};
            for (int i = 0; i < 4; i++) {
                double wx = RACK_X + xs[i];
                double wy = RACK_Y + (Math.random() - 0.5) * 0.15;
                double tilt = (Math.random() - 0.5) * 0.2;
                Location p = c.clone().add(wx, wy, RACK_Z);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(mats[i]));
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 200).interpolation(2, 0);
                weapons.add(h);
                weaponPos.add(new double[]{wx, wy, RACK_Z, tilt});
                spawnedEntities.add(h.entity());
            }

            Location bp = c.clone().add(RACK_X, RACK_Y + 0.6, RACK_Z);
            barChain = displayBuilder.spawnItem(bp, new ItemStack(Material.CHAIN));
            barChain.scale(0.001f, 0.001f, 0.001f).glow(90, 90, 110).interpolation(2, 0);
            spawnedEntities.add(barChain.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            int duration = config.getDurationTicks();
            int dissipateStart = duration - DISSIPATE_LEN;
            float fade = 1.0f;
            if (tick < SPAWN_END) fade = tick / (float) SPAWN_END;
            else if (tick >= dissipateStart) fade = Math.max(0f, (duration - tick) / (float) DISSIPATE_LEN);

            float sc = 0.7f * fade;
            for (int i = 0; i < 4; i++) {
                double[] d = weaponPos.get(i);
                weapons.get(i).animateTo(
                        new Vector3f((float)d[0] - sc / 2, (float)d[1] - sc / 2, (float)d[2] - sc / 2),
                        new AxisAngle4f((float) d[3], 0, 0, 1),
                        new Vector3f(sc), 4);
            }
            float bx = 2.4f * fade, by = 0.08f * fade, bz = 0.08f * fade;
            barChain.animateTo(
                    new Vector3f((float)RACK_X - bx / 2, (float)(RACK_Y + 0.6) - by / 2, (float)RACK_Z - bz / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(bx, by, bz), 4);

            if (tick % 18 == 0) {
                Location p = c.clone().add(RACK_X, RACK_Y, RACK_Z);
                ambientParticles(w, p, tick);
            }
            if (tick % 60 == 0) {
                Location p = c.clone().add(RACK_X, RACK_Y, RACK_Z);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 1.0, 0.3, 0.2, 0.005);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronAxeWeaponRack(plugin); }
    }
}
