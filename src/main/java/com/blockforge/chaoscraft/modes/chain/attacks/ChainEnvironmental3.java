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
 * Chain Mode — ENVIRONMENTAL ATTACKS 31-45 (Category 3: Falling & Drifting Effects)
 *
 * Mix of purely ambient falling/drifting visuals (chain segment shower, iron leaf
 * drift, slow weight bob, drape descent, spool unwind, etc.) AND a few lightly
 * damaging or impact-only falling effects (chain rope unravel cluster fall,
 * falling gear piece impact, falling anvil warning ghost that shadows a real
 * impact).
 *
 * ItemDisplays only (NEVER BlockDisplays). 3+ particle layers per attack.
 * Always-straight orientation (yaw=0, pitch=0). UNIQUE vs all other modes —
 * uses iron/chain/netherite/anvil ItemStacks (no ice, no fire, no calamity).
 *
 * Choreography contract:
 *   PHASE 1 — SPAWN    : telegraph cluster forms at Y+15 .. Y+25
 *   PHASE 2 — FALL/DRIFT: smooth descent / sway / spread via animateTo
 *   PHASE 3 — DISSIPATE: ground residual particles + scale-out
 */
public final class ChainEnvironmental3 {
    private ChainEnvironmental3() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SingleChainLinkFallLoop(plugin));
        registry.register(new IronShavingDrift(plugin));
        registry.register(new FallingGearPiece(plugin));
        registry.register(new ChainRopeUnravel(plugin));
        registry.register(new PendulumShadow(plugin));
        registry.register(new SlowWeightBob(plugin));
        registry.register(new FallingAnvilWarningGhost(plugin));
        registry.register(new ChainSegmentShower(plugin));
        registry.register(new MetalDustPlume(plugin));
        registry.register(new ChainDrapeDescent(plugin));
        registry.register(new OrbitTrailGhost(plugin));
        registry.register(new IronLeafDrift(plugin));
        registry.register(new ImpactCraterPersist(plugin));
        registry.register(new ChainSpoolUnwind(plugin));
        registry.register(new SlowChainDrapeSway(plugin));
    }

    // ================================================================
    // 31. SINGLE CHAIN LINK FALL LOOP — "The Drop" — AMBIENT (no damage)
    //   SPAWN  : 3 staggered CHAIN links materialize at Y+15
    //   ACTIVE : each slowly descends to ground, slow Y rotation
    //   DISSIPATE: scale-out on touchdown, IRON_BLOCK dust burst
    // ================================================================
    public static class SingleChainLinkFallLoop extends EnvironmentalAttack {
        private static final int LINK_COUNT = 3;
        private final List<ItemDisplayHandle> links = new ArrayList<>();
        private final double[] lX = new double[LINK_COUNT];
        private final double[] lZ = new double[LINK_COUNT];
        private final double[] lY = new double[LINK_COUNT];
        private final int[] lStart = new int[LINK_COUNT];
        private final boolean[] lLanded = new boolean[LINK_COUNT];
        private static final double START_Y = 15.0;
        private static final int FALL_DURATION = 120;

        public SingleChainLinkFallLoop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("single_chain_link_fall_loop", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(80);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — single chain link loop (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 1.6f);

            for (int i = 0; i < LINK_COUNT; i++) {
                lX[i] = (Math.random() - 0.5) * 5.0;
                lZ[i] = (Math.random() - 0.5) * 5.0;
                lY[i] = START_Y + Math.random() * 1.0;
                lStart[i] = i * 40;
                lLanded[i] = false;
                Location p = c.clone().add(lX[i], lY[i], lZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(130, 130, 140).interpolation(2, 0);
                links.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < LINK_COUNT; i++) {
                if (tick < lStart[i] || lLanded[i]) continue;
                int age = tick - lStart[i];
                double progress = Math.min(1.0, age / (double) FALL_DURATION);
                lY[i] = START_Y - (START_Y - 0.3) * progress;

                float sc = (float) (0.3 * Math.min(1.0, age / 8.0));
                links.get(i).animateTo(
                        new Vector3f((float) lX[i] - sc / 2, (float) lY[i], (float) lZ[i] - sc / 2),
                        new AxisAngle4f((float) (tick * 0.05 + i), 0, 1, 0),
                        new Vector3f(sc, sc, sc), 4);

                Location pos = c.clone().add(lX[i], lY[i], lZ[i]);
                w.spawnParticle(Particle.SMOKE, pos, 1, 0.08, 0.08, 0.08, 0.01);
                if (tick % 4 == 0) DisplayBuilder.dustParticles(pos, 1, 0.06, 150, 150, 160, 0.7f);
                if (tick % 6 == 0) w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.05, 0.05, 0.05, 0, Material.IRON_BLOCK.createBlockData());

                if (progress >= 1.0) {
                    lLanded[i] = true;
                    Location land = c.clone().add(lX[i], 0.3, lZ[i]);
                    DisplayBuilder.playSound(land, Sound.BLOCK_CHAIN_FALL, 0.4f, 1.4f);
                    w.spawnParticle(Particle.SMOKE, land, 4, 0.2, 0.05, 0.2, 0.02);
                    w.spawnParticle(Particle.FALLING_DUST, land, 5, 0.2, 0.05, 0.2, 0, Material.IRON_BLOCK.createBlockData());
                    DisplayBuilder.dustParticles(land, 3, 0.2, 130, 130, 140, 1.0f);
                    links.get(i).animateTo(new Vector3f((float) lX[i], 0.3f, (float) lZ[i]),
                            new AxisAngle4f((float) (tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.001f), 5);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SingleChainLinkFallLoop(plugin); }
    }

    // ================================================================
    // 32. IRON SHAVING DRIFT — "The Workshop Haze" — AMBIENT (no damage)
    //   Pure-particle attack with a few token ItemDisplay shaving anchors
    //   that subtly bob in the air, surrounded by drifting iron dust.
    // ================================================================
    public static class IronShavingDrift extends EnvironmentalAttack {
        private static final int ANCHOR_COUNT = 4;
        private final List<ItemDisplayHandle> anchors = new ArrayList<>();
        private final double[] aX = new double[ANCHOR_COUNT];
        private final double[] aZ = new double[ANCHOR_COUNT];
        private final double[] aBaseY = new double[ANCHOR_COUNT];

        public IronShavingDrift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_shaving_drift", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(100);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — iron shaving haze (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.3f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.2f, 1.7f);

            for (int i = 0; i < ANCHOR_COUNT; i++) {
                aX[i] = (Math.random() - 0.5) * 8.0;
                aZ[i] = (Math.random() - 0.5) * 8.0;
                aBaseY[i] = 3.0 + Math.random() * 1.5;
                Location p = c.clone().add(aX[i], aBaseY[i], aZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_NUGGET));
                h.scale(0.001f, 0.001f, 0.001f).glow(130, 130, 140).interpolation(2, 0);
                anchors.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float scale = Math.min(0.18f, tick * 0.005f);
            for (int i = 0; i < ANCHOR_COUNT; i++) {
                double yy = aBaseY[i] + Math.sin(tick * 0.04 + i) * 0.4;
                anchors.get(i).animateTo(
                        new Vector3f((float) aX[i] - scale / 2, (float) yy, (float) aZ[i] - scale / 2),
                        new AxisAngle4f((float) (tick * 0.03 + i), 0, 1, 0),
                        new Vector3f(scale), 4);
            }

            // Drifting iron-dust haze — 3 layers
            for (int j = 0; j < 5; j++) {
                double rx = (Math.random() - 0.5) * 14;
                double rz = (Math.random() - 0.5) * 14;
                double ry = 0.5 + Math.random() * 3.0;
                Location p = c.clone().add(rx, ry, rz);
                DisplayBuilder.dustParticles(p, 1, 0.1, 130, 130, 140, 0.6f);
            }
            if (tick % 2 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 12, 2.0 + Math.random() * 2, (Math.random() - 0.5) * 12);
                w.spawnParticle(Particle.SMOKE, p, 1, 0.4, 0.5, 0.4, 0.005);
            }
            if (tick % 3 == 0) {
                Location p = c.clone().add((Math.random() - 0.5) * 12, 2.5 + Math.random() * 1.5, (Math.random() - 0.5) * 12);
                w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.2, 0.2, 0.2, 0, Material.IRON_BLOCK.createBlockData());
            }
        }

        @Override public AbstractAttack newInstance() { return new IronShavingDrift(plugin); }
    }

    // ================================================================
    // 33. FALLING GEAR PIECE — "The Fragment" — DAMAGING (impact-only)
    //   A spinning iron gear-tooth fragment falls from Y+18. Heavy impact
    //   damage in a small radius. Real-feel hazard.
    //   Design: Drop-from-sky impact (read shadow, sidestep)
    // ================================================================
    public static class FallingGearPiece extends EnvironmentalAttack {
        private ItemDisplayHandle gear;
        private double gX, gZ, gY;
        private double gVy = 0.05;
        private boolean impacted = false;
        private static final double START_Y = 18.0;

        public FallingGearPiece(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("falling_gear_piece", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
            config.setChance(10.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(180.0);
            config.setImpactRadius(3.0);
            config.setDesignType("Drop-from-sky impact (read shadow, sidestep)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 1.0f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_FALL, 0.6f, 1.4f);

            gX = (Math.random() - 0.5) * 5.0;
            gZ = (Math.random() - 0.5) * 5.0;
            gY = START_Y;
            Location p = c.clone().add(gX, gY, gZ);
            gear = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
            gear.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 195).interpolation(2, 0);
            spawnedEntities.add(gear.entity());

            // Telegraph shadow column at landing position
            Location land = c.clone().add(gX, 0.1, gZ);
            DisplayBuilder.particleRing(land, 3.0, Particle.SMOKE, 24, null);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (impacted) return;

            // Telegraph: pulsing ring at landing position during first 20 ticks
            if (tick < 24) {
                Location land = c.clone().add(gX, 0.1, gZ);
                if (tick % 3 == 0) {
                    DisplayBuilder.particleRing(land, 2.5 + (tick % 6) * 0.1, Particle.ELECTRIC_SPARK, 16, null);
                    DisplayBuilder.dustParticles(land, 4, 2.5, 200, 200, 100, 1.1f);
                }
                w.spawnParticle(Particle.SMOKE, land, 2, 2.0, 0.05, 2.0, 0.01);
            }

            // Falling physics
            gVy = Math.min(1.5, gVy + 0.07);
            gY -= gVy;

            float scale = Math.min(0.45f, tick * 0.04f);
            gear.animateTo(new Vector3f((float) gX - scale / 2, (float) gY, (float) gZ - scale / 2),
                    new AxisAngle4f((float) (tick * 0.6), 0, 1, 0),
                    new Vector3f(scale), 2);

            Location pos = c.clone().add(gX, gY, gZ);
            // Layered fall trail
            w.spawnParticle(Particle.SMOKE, pos, 2, 0.1, 0.3, 0.1, 0.01);
            w.spawnParticle(Particle.CRIT, pos, 2, 0.1, 0.2, 0.1, 0.05);
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.1, 0.1, 0.1, 0, Material.IRON_BLOCK.createBlockData());
            }

            if (gY <= 0.6) {
                Location impact = c.clone().add(gX, 0.3, gZ);
                DisplayBuilder.playSound(impact, Sound.BLOCK_ANVIL_PLACE, 1.4f, 0.7f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_NETHERITE_BLOCK_FALL, 1.4f, 0.9f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_CHAIN_FALL, 1.2f, 1.2f);

                w.spawnParticle(Particle.EXPLOSION, impact, 1, 0.0, 0.0, 0.0, 0);
                w.spawnParticle(Particle.LARGE_SMOKE, impact, 18, 0.6, 0.3, 0.6, 0.06);
                w.spawnParticle(Particle.CRIT, impact, 24, 0.6, 0.3, 0.6, 0.2);
                w.spawnParticle(Particle.FALLING_DUST, impact, 20, 0.8, 0.2, 0.8, 0.05, Material.IRON_BLOCK.createBlockData());
                DisplayBuilder.dustParticles(impact, 12, 0.6, 180, 180, 195, 1.4f);
                DisplayBuilder.particleRing(impact, 3.0, Particle.ELECTRIC_SPARK, 32, null);

                triggerImpactDamage(impact);
                impacted = true;
                gear.animateTo(new Vector3f((float) gX, 0.2f, (float) gZ),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.001f), 6);
            }
        }

        @Override public AbstractAttack newInstance() { return new FallingGearPiece(plugin); }
    }

    // ================================================================
    // 34. CHAIN ROPE UNRAVEL — "The Fray" — LIGHTLY DAMAGING
    //   8 chain links start bundled at Y+5 on a wall edge, then slowly
    //   separate outward over 80 ticks. Player who stands in the bundle
    //   takes light damage as chains "fray" outward.
    //   Design: Drifting zone (timing or sidestep)
    // ================================================================
    public static class ChainRopeUnravel extends EnvironmentalAttack {
        private static final int LINK_COUNT = 8;
        private final List<ItemDisplayHandle> links = new ArrayList<>();
        private final double[] targetX = new double[LINK_COUNT];
        private final double[] targetZ = new double[LINK_COUNT];
        private final double[] targetY = new double[LINK_COUNT];
        private final double bundleX = (Math.random() - 0.5) * 6;
        private final double bundleZ = (Math.random() - 0.5) * 6;

        public ChainRopeUnravel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_rope_unravel", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(140.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(160);
            config.setCooldownTicks(90);
            config.setChance(10.0);
            config.setDesignType("Drifting zone (timing or sidestep)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 1.2f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.8f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 1.4f);

            for (int i = 0; i < LINK_COUNT; i++) {
                double a = Math.PI * 2 * i / LINK_COUNT;
                targetX[i] = bundleX + Math.cos(a) * 2.0;
                targetZ[i] = bundleZ + Math.sin(a) * 2.0;
                targetY[i] = 5.0 + Math.sin(a * 2) * 0.4;
                Location p = c.clone().add(bundleX, 5.0, bundleZ);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(140, 140, 150).interpolation(2, 0);
                links.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double progress = Math.min(1.0, tick / 80.0);
            float scale = (float) (0.18 * Math.min(1.0, tick / 12.0));
            for (int i = 0; i < LINK_COUNT; i++) {
                double x = bundleX + (targetX[i] - bundleX) * progress;
                double z = bundleZ + (targetZ[i] - bundleZ) * progress;
                double y = 5.0 + (targetY[i] - 5.0) * progress;
                links.get(i).animateTo(
                        new Vector3f((float) x - scale / 2, (float) y, (float) z - scale / 2),
                        new AxisAngle4f((float) (tick * 0.15 + i), 0, 1, 0),
                        new Vector3f(scale, (float) (scale * 3.0), scale), 3);

                Location pos = c.clone().add(x, y, z);
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.SMOKE, pos, 1, 0.1, 0.1, 0.1, 0.01);
                    DisplayBuilder.dustParticles(pos, 1, 0.1, 150, 150, 160, 0.9f);
                }
                if (tick % 4 == 0) {
                    w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.1, 0.5, 0.1, 0, Material.CHAIN.createBlockData());
                }
            }

            // Sound pulse during fray
            if (tick % 18 == 0 && tick < 100) {
                Location pulse = c.clone().add(bundleX, 5.0, bundleZ);
                DisplayBuilder.playSound(pulse, Sound.BLOCK_CHAIN_STEP, 0.8f, 1.2f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ChainRopeUnravel(plugin); }
    }

    // ================================================================
    // 35. PENDULUM SHADOW — "The Memory" — AMBIENT (no damage)
    //   A ghost arc trace of where a wrecking-ball pendulum just swung —
    //   16 ItemDisplay shadow markers along a 180° arc at Y=1.2, fading.
    // ================================================================
    public static class PendulumShadow extends EnvironmentalAttack {
        private static final int ARC_NODES = 16;
        private final List<ItemDisplayHandle> nodes = new ArrayList<>();
        private final double[] nX = new double[ARC_NODES];
        private final double[] nZ = new double[ARC_NODES];

        public PendulumShadow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pendulum_shadow", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(70);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — pendulum shadow trace (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.8f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 1.4f);

            double radius = 4.0;
            for (int i = 0; i < ARC_NODES; i++) {
                double t = i / (double) (ARC_NODES - 1);
                double angle = Math.PI * t; // 180-degree sweep
                nX[i] = Math.cos(angle) * radius;
                nZ[i] = Math.sin(angle) * radius;
                Location p = c.clone().add(nX[i], 1.2, nZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHERITE_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(40, 40, 50).interpolation(2, 0);
                nodes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float fade = Math.max(0f, 1f - tick / 60f);
            float scale = 0.18f * fade;
            for (int i = 0; i < nodes.size(); i++) {
                nodes.get(i).animateTo(
                        new Vector3f((float) nX[i] - scale / 2, 1.2f, (float) nZ[i] - scale / 2),
                        new AxisAngle4f((float) (tick * 0.05 + i * 0.3), 0, 1, 0),
                        new Vector3f(scale, scale * 0.3f, scale), 3);

                Location pos = c.clone().add(nX[i], 1.2, nZ[i]);
                if (tick % 3 == 0 && fade > 0.1f) {
                    DisplayBuilder.dustParticles(pos, 2, 0.15, 40, 40, 50, 1.0f * fade);
                    w.spawnParticle(Particle.SMOKE, pos, 1, 0.1, 0.1, 0.1, 0.005);
                }
                if (tick % 8 == 0 && fade > 0.2f) {
                    w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.1, 0.05, 0.1, 0, Material.NETHERITE_BLOCK.createBlockData());
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new PendulumShadow(plugin); }
    }

    // ================================================================
    // 36. SLOW WEIGHT BOB — "The Balance" — AMBIENT (no damage)
    //   3 iron weights hanging from chain bundles in the background,
    //   each bobbing on independent oscillation cycles.
    // ================================================================
    public static class SlowWeightBob extends EnvironmentalAttack {
        private static final int WEIGHT_COUNT = 3;
        private static final int LINKS_PER_WEIGHT = 4;
        private final List<ItemDisplayHandle> weights = new ArrayList<>();
        private final List<ItemDisplayHandle> chains = new ArrayList<>();
        private final double[] wX = new double[WEIGHT_COUNT];
        private final double[] wZ = new double[WEIGHT_COUNT];
        private final double[] wBaseY = new double[WEIGHT_COUNT];
        private final double[] wPeriod = new double[WEIGHT_COUNT];

        public SlowWeightBob(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("slow_weight_bob", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(120);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — slow weight bob (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.2f, 1.2f);

            for (int i = 0; i < WEIGHT_COUNT; i++) {
                double a = Math.PI * 2 * i / WEIGHT_COUNT;
                wX[i] = Math.cos(a) * 6.5;
                wZ[i] = Math.sin(a) * 6.5;
                wBaseY[i] = 3.0 + i * 0.6;
                wPeriod[i] = 60 + Math.random() * 40;

                Location wp = c.clone().add(wX[i], wBaseY[i], wZ[i]);
                ItemDisplayHandle weight = displayBuilder.spawnItem(wp, new ItemStack(Material.IRON_BLOCK));
                weight.scale(0.001f, 0.001f, 0.001f).glow(170, 170, 180).interpolation(2, 0);
                weights.add(weight);
                spawnedEntities.add(weight.entity());

                for (int j = 0; j < LINKS_PER_WEIGHT; j++) {
                    Location lp = c.clone().add(wX[i], wBaseY[i] + 0.5 + j * 0.5, wZ[i]);
                    ItemDisplayHandle link = displayBuilder.spawnItem(lp, new ItemStack(Material.CHAIN));
                    link.scale(0.001f, 0.001f, 0.001f).glow(140, 140, 150).interpolation(2, 0);
                    chains.add(link);
                    spawnedEntities.add(link.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < WEIGHT_COUNT; i++) {
                double bob = Math.sin(tick * (Math.PI * 2 / wPeriod[i])) * 0.4;
                double yy = wBaseY[i] + bob;
                weights.get(i).animateTo(
                        new Vector3f((float) wX[i] - 0.2f, (float) yy, (float) wZ[i] - 0.2f),
                        new AxisAngle4f((float) (tick * 0.02 + i), 0, 1, 0),
                        new Vector3f(0.4f, 0.5f, 0.4f), 4);

                for (int j = 0; j < LINKS_PER_WEIGHT; j++) {
                    int idx = i * LINKS_PER_WEIGHT + j;
                    double lyy = wBaseY[i] + 0.5 + j * 0.5 + bob * (1.0 - j * 0.2);
                    chains.get(idx).animateTo(
                            new Vector3f((float) wX[i] - 0.06f, (float) lyy, (float) wZ[i] - 0.06f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.12f, 0.4f, 0.12f), 4);
                }

                Location pos = c.clone().add(wX[i], wBaseY[i] + bob, wZ[i]);
                if (tick % 10 == 0) {
                    w.spawnParticle(Particle.SMOKE, pos, 1, 0.1, 0.1, 0.1, 0.005);
                    DisplayBuilder.dustParticles(pos, 1, 0.15, 140, 140, 150, 0.8f);
                }
                if (tick % 24 == 0) {
                    w.spawnParticle(Particle.FALLING_DUST, pos.clone().add(0, -0.3, 0), 1, 0.1, 0.05, 0.1, 0, Material.IRON_BLOCK.createBlockData());
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SlowWeightBob(plugin); }
    }

    // ================================================================
    // 37. FALLING ANVIL WARNING GHOST — "The Premonition" — DAMAGING (impact-only)
    //   A ghost ANVIL ItemDisplay materializes high and descends through
    //   a telegraphed shadow toward a marked floor zone. The shadow
    //   reinforces the warning; landing causes heavy impact damage.
    //   Design: Drop-from-sky impact (read shadow, sidestep)
    // ================================================================
    public static class FallingAnvilWarningGhost extends EnvironmentalAttack {
        private ItemDisplayHandle anvil;
        private double aX, aZ, aY;
        private boolean impacted = false;
        private static final double START_Y = 20.0;
        private static final int FALL_TICKS = 40;

        public FallingAnvilWarningGhost(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("falling_anvil_warning_ghost", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(110);
            config.setChance(10.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(220.0);
            config.setImpactRadius(3.5);
            config.setDesignType("Drop-from-sky impact (read shadow, sidestep)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 1.2f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 1.0f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_FALL, 0.8f, 1.0f);

            aX = (Math.random() - 0.5) * 6.0;
            aZ = (Math.random() - 0.5) * 6.0;
            aY = START_Y;
            Location p = c.clone().add(aX, aY, aZ);
            anvil = displayBuilder.spawnItem(p, new ItemStack(Material.ANVIL));
            anvil.scale(0.001f, 0.001f, 0.001f).glow(60, 60, 70).interpolation(2, 0);
            spawnedEntities.add(anvil.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (impacted) return;

            Location land = c.clone().add(aX, 0.1, aZ);

            // Telegraph phase (first 30t) — heavy shadow on ground
            if (tick < 30) {
                if (tick % 2 == 0) {
                    DisplayBuilder.particleRing(land, 3.5, Particle.LARGE_SMOKE, 24, null);
                    DisplayBuilder.dustParticles(land, 5, 3.0, 30, 30, 40, 1.3f);
                }
                if (tick % 4 == 0) {
                    DisplayBuilder.particleRing(land, 2.5, Particle.ELECTRIC_SPARK, 18, null);
                    w.spawnParticle(Particle.FALLING_DUST, land, 4, 3.0, 0.05, 3.0, 0, Material.ANVIL.createBlockData());
                }
            }

            // Begin fall after telegraph
            if (tick >= 30) {
                int fallTick = tick - 30;
                double fp = Math.min(1.0, fallTick / (double) FALL_TICKS);
                // Acceleration curve
                aY = START_Y - (START_Y - 0.5) * (fp * fp);

                float sc = Math.min(0.9f, fallTick * 0.06f);
                anvil.animateTo(
                        new Vector3f((float) aX - sc / 2, (float) aY, (float) aZ - sc / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sc), 2);

                Location pos = c.clone().add(aX, aY, aZ);
                w.spawnParticle(Particle.LARGE_SMOKE, pos, 3, 0.2, 0.4, 0.2, 0.02);
                w.spawnParticle(Particle.FALLING_DUST, pos, 2, 0.2, 0.5, 0.2, 0, Material.ANVIL.createBlockData());
                w.spawnParticle(Particle.CRIT, pos, 2, 0.2, 0.3, 0.2, 0.05);

                if (aY <= 0.7) {
                    Location impact = c.clone().add(aX, 0.4, aZ);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_ANVIL_PLACE, 2.0f, 0.6f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_NETHERITE_BLOCK_FALL, 1.6f, 0.7f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_CHAIN_FALL, 1.6f, 0.9f);

                    w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.LARGE_SMOKE, impact, 30, 1.0, 0.3, 1.0, 0.06);
                    w.spawnParticle(Particle.CRIT, impact, 30, 0.8, 0.3, 0.8, 0.25);
                    w.spawnParticle(Particle.FALLING_DUST, impact, 30, 1.0, 0.2, 1.0, 0.05, Material.ANVIL.createBlockData());
                    DisplayBuilder.dustParticles(impact, 14, 1.0, 60, 60, 70, 1.5f);
                    DisplayBuilder.particleRing(impact, 3.5, Particle.ELECTRIC_SPARK, 32, null);

                    triggerImpactDamage(impact);
                    impacted = true;
                    anvil.animateTo(new Vector3f((float) aX, 0.3f, (float) aZ),
                            new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.001f), 8);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FallingAnvilWarningGhost(plugin); }
    }

    // ================================================================
    // 38. CHAIN SEGMENT SHOWER — "The Light Debris" — AMBIENT (no damage)
    //   Bursts of tiny CHAIN ItemDisplays from a ceiling point flying
    //   outward and falling with gravity. Light debris atmosphere.
    // ================================================================
    public static class ChainSegmentShower extends EnvironmentalAttack {
        private static final int SEG_COUNT = 8;
        private final List<ItemDisplayHandle> segs = new ArrayList<>();
        private final double[] sVx = new double[SEG_COUNT];
        private final double[] sVz = new double[SEG_COUNT];
        private final double[] sVy = new double[SEG_COUNT];
        private final double[] sX = new double[SEG_COUNT];
        private final double[] sZ = new double[SEG_COUNT];
        private final double[] sY = new double[SEG_COUNT];
        private final boolean[] sLanded = new boolean[SEG_COUNT];
        private static final double CEILING_Y = 16.0;

        public ChainSegmentShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_segment_shower", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(70);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — chain segment shower (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.8f, 1.3f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.6f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 1.8f);

            double burstX = (Math.random() - 0.5) * 4;
            double burstZ = (Math.random() - 0.5) * 4;
            for (int i = 0; i < SEG_COUNT; i++) {
                double a = Math.PI * 2 * i / SEG_COUNT;
                sX[i] = burstX;
                sZ[i] = burstZ;
                sY[i] = CEILING_Y;
                sVx[i] = Math.cos(a) * (0.15 + Math.random() * 0.1);
                sVz[i] = Math.sin(a) * (0.15 + Math.random() * 0.1);
                sVy[i] = -0.02;
                sLanded[i] = false;
                Location p = c.clone().add(sX[i], sY[i], sZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(150, 150, 165).interpolation(2, 0);
                segs.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float scale = Math.min(0.12f, tick * 0.02f);
            for (int i = 0; i < SEG_COUNT; i++) {
                if (sLanded[i]) continue;
                sVy[i] -= 0.05;
                sX[i] += sVx[i];
                sZ[i] += sVz[i];
                sY[i] += sVy[i];

                segs.get(i).animateTo(
                        new Vector3f((float) sX[i] - scale / 2, (float) sY[i], (float) sZ[i] - scale / 2),
                        new AxisAngle4f((float) (tick * 0.4 + i), 0, 1, 0),
                        new Vector3f(scale), 2);

                Location pos = c.clone().add(sX[i], sY[i], sZ[i]);
                w.spawnParticle(Particle.SMOKE, pos, 1, 0.05, 0.1, 0.05, 0.01);
                if (tick % 2 == 0) {
                    DisplayBuilder.dustParticles(pos, 1, 0.06, 150, 150, 165, 0.7f);
                }
                if (tick % 3 == 0) {
                    w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.05, 0.05, 0.05, 0, Material.CHAIN.createBlockData());
                }

                if (sY[i] <= 0.3) {
                    sLanded[i] = true;
                    Location land = c.clone().add(sX[i], 0.2, sZ[i]);
                    DisplayBuilder.playSound(land, Sound.BLOCK_CHAIN_FALL, 0.3f, 1.7f);
                    w.spawnParticle(Particle.SMOKE, land, 3, 0.15, 0.05, 0.15, 0.02);
                    DisplayBuilder.dustParticles(land, 2, 0.2, 150, 150, 165, 0.9f);
                    segs.get(i).animateTo(new Vector3f((float) sX[i], 0.2f, (float) sZ[i]),
                            new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.001f), 4);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ChainSegmentShower(plugin); }
    }

    // ================================================================
    // 39. METAL DUST PLUME — "The Disturbance" — AMBIENT (no damage)
    //   Lingering vertical dust plume rising from a ground site, mimicking
    //   the aftermath of an eruption. Few small iron-fragment ItemDisplays
    //   slowly rise and fade.
    // ================================================================
    public static class MetalDustPlume extends EnvironmentalAttack {
        private static final int FRAG_COUNT = 5;
        private final List<ItemDisplayHandle> frags = new ArrayList<>();
        private final double[] fX = new double[FRAG_COUNT];
        private final double[] fZ = new double[FRAG_COUNT];
        private final double[] fY = new double[FRAG_COUNT];
        private final double siteX = (Math.random() - 0.5) * 6;
        private final double siteZ = (Math.random() - 0.5) * 6;

        public MetalDustPlume(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("metal_dust_plume", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(80);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — metal dust plume aftermath (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_FALL, 0.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 0.6f);

            for (int i = 0; i < FRAG_COUNT; i++) {
                double a = Math.PI * 2 * i / FRAG_COUNT;
                fX[i] = siteX + Math.cos(a) * (0.4 + Math.random() * 0.6);
                fZ[i] = siteZ + Math.sin(a) * (0.4 + Math.random() * 0.6);
                fY[i] = 0.3;
                Location p = c.clone().add(fX[i], fY[i], fZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_NUGGET));
                h.scale(0.001f, 0.001f, 0.001f).glow(160, 160, 175).interpolation(2, 0);
                frags.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double progress = Math.min(1.0, tick / 100.0);

            for (int i = 0; i < FRAG_COUNT; i++) {
                fY[i] = 0.3 + progress * 2.5;
                float sc = (float) (0.1 * (1.0 - progress));
                frags.get(i).animateTo(
                        new Vector3f((float) fX[i] - sc / 2, (float) fY[i], (float) fZ[i] - sc / 2),
                        new AxisAngle4f((float) (tick * 0.2 + i), 0, 1, 0),
                        new Vector3f(sc), 3);
            }

            // Plume body — 3 layered particles
            int density = (int) (8 * (1.0 - progress * 0.7));
            for (int j = 0; j < density; j++) {
                double rx = siteX + (Math.random() - 0.5) * 1.0;
                double rz = siteZ + (Math.random() - 0.5) * 1.0;
                double ry = 0.1 + Math.random() * (1.5 + progress * 2.0);
                Location p = c.clone().add(rx, ry, rz);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.1, 0.05, 0.1, 0.06);
                if (j % 2 == 0) {
                    w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.1, 0.05, 0.1, 0, Material.IRON_BLOCK.createBlockData());
                }
                if (j % 3 == 0) {
                    DisplayBuilder.dustParticles(p, 1, 0.1, 160, 160, 175, 0.9f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new MetalDustPlume(plugin); }
    }

    // ================================================================
    // 40. CHAIN DRAPE DESCENT — "The Decor" — AMBIENT (no damage)
    //   A bundle of 6 chains descending slowly from ceiling to mid-height,
    //   snapping back up periodically. Hypnotic.
    // ================================================================
    public static class ChainDrapeDescent extends EnvironmentalAttack {
        private static final int CHAIN_COUNT = 6;
        private final List<ItemDisplayHandle> chains = new ArrayList<>();
        private final double[] cX = new double[CHAIN_COUNT];
        private final double[] cZ = new double[CHAIN_COUNT];
        private static final double TOP_Y = 14.0;
        private static final double BOTTOM_Y = 4.5;
        private static final int DESCEND_DURATION = 200;
        private static final int RESET_DURATION = 5;

        public ChainDrapeDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_drape_descent", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(100);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — chain drape descent loop (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 1.4f);

            for (int i = 0; i < CHAIN_COUNT; i++) {
                double a = Math.PI * 2 * i / CHAIN_COUNT;
                cX[i] = Math.cos(a) * 1.0;
                cZ[i] = Math.sin(a) * 1.0;
                Location p = c.clone().add(cX[i], TOP_Y, cZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(140, 140, 150).interpolation(2, 0);
                chains.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int cycle = DESCEND_DURATION + RESET_DURATION;
            int phase = tick % cycle;

            double y;
            if (phase < DESCEND_DURATION) {
                double p = phase / (double) DESCEND_DURATION;
                y = TOP_Y - (TOP_Y - BOTTOM_Y) * p;
            } else {
                double p = (phase - DESCEND_DURATION) / (double) RESET_DURATION;
                y = BOTTOM_Y + (TOP_Y - BOTTOM_Y) * p;
                if (phase == DESCEND_DURATION) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.8f, 1.4f);
                }
            }

            for (int i = 0; i < CHAIN_COUNT; i++) {
                float sc = (float) Math.min(0.16, tick * 0.01);
                chains.get(i).animateTo(
                        new Vector3f((float) cX[i] - sc / 2, (float) y, (float) cZ[i] - sc / 2),
                        new AxisAngle4f((float) (tick * 0.03 + i), 0, 1, 0),
                        new Vector3f(sc, sc * 5.0f, sc), phase < DESCEND_DURATION ? 4 : 2);

                Location pos = c.clone().add(cX[i], y, cZ[i]);
                if (tick % 6 == 0) {
                    w.spawnParticle(Particle.SMOKE, pos, 1, 0.1, 0.2, 0.1, 0.005);
                    DisplayBuilder.dustParticles(pos, 1, 0.1, 140, 140, 150, 0.8f);
                }
                if (tick % 10 == 0) {
                    w.spawnParticle(Particle.FALLING_DUST, pos.clone().add(0, -0.4, 0), 1, 0.1, 0.05, 0.1, 0, Material.CHAIN.createBlockData());
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ChainDrapeDescent(plugin); }
    }

    // ================================================================
    // 41. ORBIT TRAIL GHOST — "The Path" — AMBIENT (no damage)
    //   After a wrecking ball orbit attack: 16 ghost ItemDisplays form
    //   a circle at Y=1.5 marking the recent danger zone. Fades over time.
    // ================================================================
    public static class OrbitTrailGhost extends EnvironmentalAttack {
        private static final int NODE_COUNT = 16;
        private final List<ItemDisplayHandle> nodes = new ArrayList<>();
        private final double[] nX = new double[NODE_COUNT];
        private final double[] nZ = new double[NODE_COUNT];
        private static final double ORBIT_RADIUS = 4.5;

        public OrbitTrailGhost(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbit_trail_ghost", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(80);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — orbit trail ghost (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.8f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 1.6f);

            for (int i = 0; i < NODE_COUNT; i++) {
                double a = Math.PI * 2 * i / NODE_COUNT;
                nX[i] = Math.cos(a) * ORBIT_RADIUS;
                nZ[i] = Math.sin(a) * ORBIT_RADIUS;
                Location p = c.clone().add(nX[i], 1.5, nZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_NUGGET));
                h.scale(0.001f, 0.001f, 0.001f).glow(60, 70, 90).interpolation(2, 0);
                nodes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float fade = Math.max(0f, 1f - tick / 40f);
            float scale = 0.14f * fade;

            for (int i = 0; i < NODE_COUNT; i++) {
                nodes.get(i).animateTo(
                        new Vector3f((float) nX[i] - scale / 2, 1.5f, (float) nZ[i] - scale / 2),
                        new AxisAngle4f((float) (tick * 0.1 + i * 0.4), 0, 1, 0),
                        new Vector3f(scale, scale * 0.4f, scale), 3);

                Location pos = c.clone().add(nX[i], 1.5, nZ[i]);
                if (tick % 2 == 0 && fade > 0.1f) {
                    DisplayBuilder.dustParticles(pos, 2, 0.1, 60, 70, 90, 1.0f * fade);
                }
                if (tick % 4 == 0 && fade > 0.2f) {
                    w.spawnParticle(Particle.SMOKE, pos, 1, 0.1, 0.05, 0.1, 0.005);
                    w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.1, 0.05, 0.1, 0, Material.IRON_BLOCK.createBlockData());
                }
            }

            // Connecting circle: smoke ring through nodes
            if (tick % 8 == 0 && fade > 0.3f) {
                Location center = c.clone().add(0, 1.5, 0);
                DisplayBuilder.particleRing(center, ORBIT_RADIUS, Particle.SMOKE, 24, null);
            }
        }

        @Override public AbstractAttack newInstance() { return new OrbitTrailGhost(plugin); }
    }

    // ================================================================
    // 42. IRON LEAF DRIFT — "The Fall" — AMBIENT (no damage)
    //   Flat iron-shard ItemDisplays drift slowly downward from Y+12,
    //   tumbling like falling leaves. Heavy particle layering.
    // ================================================================
    public static class IronLeafDrift extends EnvironmentalAttack {
        private static final int LEAF_COUNT = 10;
        private final List<ItemDisplayHandle> leaves = new ArrayList<>();
        private final double[] lX = new double[LEAF_COUNT];
        private final double[] lZ = new double[LEAF_COUNT];
        private final double[] lY = new double[LEAF_COUNT];
        private final double[] lDriftX = new double[LEAF_COUNT];
        private final double[] lDriftZ = new double[LEAF_COUNT];
        private final double[] lSpin = new double[LEAF_COUNT];
        private final int[] lStart = new int[LEAF_COUNT];
        private final boolean[] lLanded = new boolean[LEAF_COUNT];
        private static final double START_Y = 12.0;

        public IronLeafDrift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_leaf_drift", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(90);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — iron leaf drift (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 1.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.3f, 1.7f);

            for (int i = 0; i < LEAF_COUNT; i++) {
                lX[i] = (Math.random() - 0.5) * 10;
                lZ[i] = (Math.random() - 0.5) * 10;
                lY[i] = START_Y + Math.random() * 3;
                lDriftX[i] = (Math.random() - 0.5) * 0.02;
                lDriftZ[i] = (Math.random() - 0.5) * 0.02;
                lSpin[i] = (Math.random() - 0.5) * 0.3;
                lStart[i] = (int) (Math.random() * 80);
                lLanded[i] = false;
                Location p = c.clone().add(lX[i], lY[i], lZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_NUGGET));
                h.scale(0.001f, 0.001f, 0.001f).glow(170, 175, 190).interpolation(2, 0);
                leaves.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < LEAF_COUNT; i++) {
                if (tick < lStart[i] || lLanded[i]) continue;
                int age = tick - lStart[i];
                lY[i] -= 0.04;
                lX[i] += lDriftX[i] + Math.sin(age * 0.05) * 0.01;
                lZ[i] += lDriftZ[i] + Math.cos(age * 0.05) * 0.01;

                float sc = (float) Math.min(0.08, age * 0.01);
                leaves.get(i).animateTo(
                        new Vector3f((float) lX[i] - sc / 2, (float) lY[i], (float) lZ[i] - sc / 2),
                        new AxisAngle4f((float) (age * lSpin[i] + i), 0.5f, 0.5f, 0.5f),
                        new Vector3f(sc * 2.0f, sc * 0.1f, sc * 2.0f), 4);

                Location pos = c.clone().add(lX[i], lY[i], lZ[i]);
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.08, 0.05, 0.08, 0, Material.IRON_BLOCK.createBlockData());
                }
                if (tick % 4 == 0) {
                    DisplayBuilder.dustParticles(pos, 1, 0.06, 170, 175, 190, 0.7f);
                }
                if (tick % 6 == 0) {
                    w.spawnParticle(Particle.SMOKE, pos, 1, 0.06, 0.06, 0.06, 0.005);
                }

                if (lY[i] <= 0.2) {
                    lLanded[i] = true;
                    leaves.get(i).animateTo(new Vector3f((float) lX[i], 0.1f, (float) lZ[i]),
                            new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.001f), 3);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IronLeafDrift(plugin); }
    }

    // ================================================================
    // 43. IMPACT CRATER PERSIST — "The Scar" — AMBIENT (no damage)
    //   4 small IRON_BLOCK debris ItemDisplays scattered at an "impact"
    //   site, with NETHERITE_BLOCK dust rising. Persists then fades.
    // ================================================================
    public static class ImpactCraterPersist extends EnvironmentalAttack {
        private static final int DEBRIS_COUNT = 4;
        private final List<ItemDisplayHandle> debris = new ArrayList<>();
        private final double[] dX = new double[DEBRIS_COUNT];
        private final double[] dZ = new double[DEBRIS_COUNT];
        private final double[] dY = new double[DEBRIS_COUNT];
        private final double siteX = (Math.random() - 0.5) * 6;
        private final double siteZ = (Math.random() - 0.5) * 6;

        public ImpactCraterPersist(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("impact_crater_persist", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(70);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — impact crater scar (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_FALL, 0.7f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.4f, 0.8f);

            for (int i = 0; i < DEBRIS_COUNT; i++) {
                double a = Math.PI * 2 * i / DEBRIS_COUNT + Math.random() * 0.5;
                double r = 0.5 + Math.random() * 1.0;
                dX[i] = siteX + Math.cos(a) * r;
                dZ[i] = siteZ + Math.sin(a) * r;
                dY[i] = 0.15;
                Location p = c.clone().add(dX[i], dY[i], dZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(120, 120, 140).interpolation(2, 0);
                debris.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float fade = Math.max(0f, 1f - tick / 60f);
            float sc = 0.06f * fade;

            for (int i = 0; i < DEBRIS_COUNT; i++) {
                debris.get(i).animateTo(
                        new Vector3f((float) dX[i] - sc / 2, (float) dY[i], (float) dZ[i] - sc / 2),
                        new AxisAngle4f((float) (tick * 0.04 + i), 0.5f, 0.5f, 0.5f),
                        new Vector3f(sc), 4);
            }

            // Crater plume
            Location site = c.clone().add(siteX, 0.2, siteZ);
            if (tick % 2 == 0 && fade > 0.05f) {
                w.spawnParticle(Particle.FALLING_DUST, site, 4, 0.5, 0.5, 0.5, 0.05, Material.NETHERITE_BLOCK.createBlockData());
            }
            if (tick % 3 == 0 && fade > 0.1f) {
                w.spawnParticle(Particle.LARGE_SMOKE, site, 2, 0.4, 0.2, 0.4, 0.04);
                DisplayBuilder.dustParticles(site, 2, 0.4, 120, 120, 140, 1.0f * fade);
            }
            if (tick == 0) {
                DisplayBuilder.particleRing(site, 1.5, Particle.SMOKE, 16, null);
            }
        }

        @Override public AbstractAttack newInstance() { return new ImpactCraterPersist(plugin); }
    }

    // ================================================================
    // 44. CHAIN SPOOL UNWIND — "The Feed" — AMBIENT (no damage)
    //   A visible spool at ceiling: 2 DARK_OAK_LOG flanges + 6 CHAIN
    //   wraps slowly rotating on the Z-axis as if feeding chain down.
    // ================================================================
    public static class ChainSpoolUnwind extends EnvironmentalAttack {
        private static final int WRAP_COUNT = 6;
        private final List<ItemDisplayHandle> flanges = new ArrayList<>();
        private final List<ItemDisplayHandle> wraps = new ArrayList<>();
        private static final double SPOOL_Y = 14.0;

        public ChainSpoolUnwind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_spool_unwind", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(110);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — chain spool unwind (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.5f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.4f, 0.5f);

            // Two flanges (left/right of spool)
            for (int i = 0; i < 2; i++) {
                double off = i == 0 ? -0.6 : 0.6;
                Location p = c.clone().add(off, SPOOL_Y, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.DARK_OAK_LOG));
                h.scale(0.001f, 0.001f, 0.001f).glow(80, 50, 30).interpolation(2, 0);
                flanges.add(h);
                spawnedEntities.add(h.entity());
            }

            // Chain wraps in compressed helix
            for (int i = 0; i < WRAP_COUNT; i++) {
                double a = Math.PI * 2 * i / WRAP_COUNT;
                double off = -0.4 + (i / (double) (WRAP_COUNT - 1)) * 0.8;
                Location p = c.clone().add(off, SPOOL_Y, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(150, 150, 165).interpolation(2, 0);
                wraps.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float flScale = Math.min(0.8f, tick * 0.04f);
            for (int i = 0; i < flanges.size(); i++) {
                double off = i == 0 ? -0.6 : 0.6;
                flanges.get(i).animateTo(
                        new Vector3f((float) off - flScale / 2, (float) SPOOL_Y, -flScale / 2),
                        new AxisAngle4f((float) (tick * 0.04), 1, 0, 0),
                        new Vector3f(flScale, 0.4f, flScale), 4);
            }

            float wrScale = Math.min(0.18f, tick * 0.015f);
            for (int i = 0; i < WRAP_COUNT; i++) {
                double a = Math.PI * 2 * i / WRAP_COUNT + tick * 0.06;
                double off = -0.4 + (i / (double) (WRAP_COUNT - 1)) * 0.8;
                double yy = SPOOL_Y + Math.cos(a) * 0.4;
                double zz = Math.sin(a) * 0.4;
                wraps.get(i).animateTo(
                        new Vector3f((float) off - wrScale / 2, (float) yy, (float) zz - wrScale / 2),
                        new AxisAngle4f((float) a, 1, 0, 0),
                        new Vector3f(wrScale, wrScale, wrScale), 3);
            }

            // Feeding chain falling from spool
            Location spool = c.clone().add(0, SPOOL_Y - 0.5, 0);
            if (tick % 4 == 0) {
                w.spawnParticle(Particle.FALLING_DUST, spool, 1, 0.3, 0.3, 0.3, 0, Material.CHAIN.createBlockData());
                DisplayBuilder.dustParticles(spool, 1, 0.3, 150, 150, 165, 0.8f);
            }
            if (tick % 8 == 0) {
                w.spawnParticle(Particle.SMOKE, spool, 1, 0.2, 0.2, 0.2, 0.005);
            }
            if (tick % 60 == 0 && tick > 0) {
                DisplayBuilder.playSound(spool, Sound.BLOCK_GRINDSTONE_USE, 0.6f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ChainSpoolUnwind(plugin); }
    }

    // ================================================================
    // 45. SLOW CHAIN DRAPE SWAY — "The Curtain" — AMBIENT (no damage)
    //   12 long vertical CHAIN strands hanging from ceiling, each
    //   swaying on the X axis with independent oscillation periods.
    // ================================================================
    public static class SlowChainDrapeSway extends EnvironmentalAttack {
        private static final int DRAPE_COUNT = 12;
        private final List<ItemDisplayHandle> drapes = new ArrayList<>();
        private final double[] dX = new double[DRAPE_COUNT];
        private final double[] dZ = new double[DRAPE_COUNT];
        private final double[] dPeriod = new double[DRAPE_COUNT];
        private final double[] dPhase = new double[DRAPE_COUNT];
        private static final double TOP_Y = 12.0;

        public SlowChainDrapeSway(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("slow_chain_drape_sway", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(120);
            config.setChance(10.0);
            config.setDesignType("Ambient falling — slow chain drape sway (no damage)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.3f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.25f, 1.5f);

            for (int i = 0; i < DRAPE_COUNT; i++) {
                dX[i] = (Math.random() - 0.5) * 12;
                dZ[i] = (Math.random() - 0.5) * 12;
                dPeriod[i] = 60 + Math.random() * 60;
                dPhase[i] = Math.random() * Math.PI * 2;
                Location p = c.clone().add(dX[i], TOP_Y, dZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(140, 140, 155).interpolation(2, 0);
                drapes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            float sc = Math.min(0.12f, tick * 0.008f);
            for (int i = 0; i < DRAPE_COUNT; i++) {
                double swayAngle = Math.sin(tick * (Math.PI * 2 / dPeriod[i]) + dPhase[i]) * (Math.PI / 45.0); // ±4°
                drapes.get(i).animateTo(
                        new Vector3f((float) dX[i] - sc / 2, (float) TOP_Y, (float) dZ[i] - sc / 2),
                        new AxisAngle4f((float) swayAngle, 1, 0, 0),
                        new Vector3f(sc, 3.0f, sc), 4);

                Location pos = c.clone().add(dX[i], TOP_Y - 1.5, dZ[i]);
                if ((tick + i) % 14 == 0) {
                    w.spawnParticle(Particle.SMOKE, pos, 1, 0.15, 0.4, 0.15, 0.005);
                    DisplayBuilder.dustParticles(pos, 1, 0.12, 140, 140, 155, 0.7f);
                }
                if ((tick + i) % 20 == 0) {
                    w.spawnParticle(Particle.FALLING_DUST, pos, 1, 0.1, 0.4, 0.1, 0, Material.CHAIN.createBlockData());
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SlowChainDrapeSway(plugin); }
    }
}
