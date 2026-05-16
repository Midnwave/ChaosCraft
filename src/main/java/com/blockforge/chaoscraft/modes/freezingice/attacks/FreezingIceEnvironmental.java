package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * FreezingIce Mode — ENVIRONMENTAL FX BATCH 1 (entries 1-10).
 * Frost Projectile theme. Pure ItemDisplay + particle attacks. NO BlockDisplays.
 * Icicles, hailstones, frozen javelins, subzero arrows, glass shards,
 * cryo mortars, polar spears, sleet, frostbite darts, and comet strikes.
 */
public final class FreezingIceEnvironmental {
    private FreezingIceEnvironmental() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IcicleVolley(plugin));
        registry.register(new HailstoneShower(plugin));
        registry.register(new FrozenJavelinRain(plugin));
        registry.register(new SubzeroArrowSwarm(plugin));
        registry.register(new GlassShardRain(plugin));
        registry.register(new CryoMortar(plugin));
        registry.register(new PolarSpear(plugin));
        registry.register(new SleetVolley(plugin));
        registry.register(new FrostbiteDarts(plugin));
        registry.register(new CometStrike(plugin));
    }

    // ================================================================
    // Helper: find nearest non-exempt survival player within range
    // ================================================================
    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
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

    // ================================================================
    // 1. ICICLE VOLLEY — 18 PACKED_ICE tapered icicles fall from Y+22
    //    in a tight cluster, each rotating point-down. Impact only:
    //    radius 2.5, 4200hp per icicle.
    // ================================================================
    public static class IcicleVolley extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> icicles = new ArrayList<>();
        private final double[] iX = new double[18];
        private final double[] iZ = new double[18];
        private final double[] iY = new double[18];
        private final int[] iStartTick = new int[18];
        private final boolean[] iImpacted = new boolean[18];
        private static final double START_Y = 22.0;

        public IcicleVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("icicle_volley", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(50400.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(200);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50400.0);
            config.setImpactRadius(3.75);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.4f, 1.6f);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.0f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 1.5f);

            // Tight cluster of 18 icicles within 3.5 block radius
            for (int i = 0; i < 18; i++) {
                double a = Math.random() * Math.PI * 2;
                double rr = Math.random() * 3.5;
                iX[i] = Math.cos(a) * rr;
                iZ[i] = Math.sin(a) * rr;
                iY[i] = START_Y + Math.random() * 2.5;
                iStartTick[i] = (int)(Math.random() * 14); // small stagger
                iImpacted[i] = false;
                Location p = c.clone().add(iX[i], iY[i], iZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                // Tapered icicle: thin at top/bottom, slightly wider middle. Pointed-down feel
                // via tall narrow vertical scale.
                h.scale(0.45f, 1.6f, 0.45f).glow(180, 230, 255).interpolation(2, 0);
                icicles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Frost dressing: 6 ICE shards + 4 BLUE_ICE chunks floating at the spawn cloud
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.2;
                double rr = 2.0 + Math.random();
                double yy = START_Y + 2.5 + Math.random() * 2;
                Location p = c.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 3.0, START_Y + 4.0, Math.sin(a) * 3.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.5f, 0.5f, 0.5f).glow(160, 210, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < icicles.size(); i++) {
                if (iImpacted[i] || tick < iStartTick[i]) continue;
                iY[i] -= 0.95; // fast fall
                Location pos = c.clone().add(iX[i], iY[i], iZ[i]);

                // Each icicle stays oriented point-down (rotation around X for slight wobble)
                // via slight rotation each tick around vertical axis but base is pointed down
                // (achieved with tall narrow scale). Add a subtle wobble.
                icicles.get(i).animateTo(
                        new Vector3f((float)iX[i] - 0.225f, (float)iY[i], (float)iZ[i] - 0.225f),
                        new AxisAngle4f((float)(Math.sin(tick * 0.4 + i) * 0.15), 0, 0, 1),
                        new Vector3f(0.45f, 1.6f, 0.45f), 2);

                // SNOWFLAKE trail per icicle (4/tick)
                w.spawnParticle(Particle.SNOWFLAKE, pos, 4, 0.08, 0.4, 0.08, 0.01);
                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(pos, 1, 0.05, 200, 230, 255, 1.0f);
                }

                if (iY[i] <= 0.6) {
                    Location impact = c.clone().add(iX[i], 0.4, iZ[i]);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.4f, 1.2f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_PLACE, 1.2f, 1.4f);

                    // Snowball impact burst (24)
                    w.spawnParticle(Particle.ITEM_SNOWBALL, impact, 24, 0.5, 0.3, 0.5, 0.25);
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 12, 0.4, 0.3, 0.4, 0.1);
                    w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                    DisplayBuilder.dustParticles(impact, 12, 0.5, 180, 230, 255, 1.4f);

                    triggerImpactDamage(impact);
                    iImpacted[i] = true;
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IcicleVolley(plugin); }
    }

    // ================================================================
    // 2. HAILSTONE SHOWER — 30 SNOWBALL ItemDisplays scatter-fall over
    //    200t across 10-block radius. Impact: radius 1.8, 2200hp.
    // ================================================================
    public static class HailstoneShower extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> stones = new ArrayList<>();
        private final double[] sX = new double[30];
        private final double[] sZ = new double[30];
        private final double[] sY = new double[30];
        private final double[] sVy = new double[30];
        private final double[] sSpin = new double[30];
        private final int[] sStartTick = new int[30];
        private final boolean[] sImpacted = new boolean[30];

        public HailstoneShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hailstone_shower", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(26400.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(260);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(26400.0);
            config.setImpactRadius(2.7);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 1.6f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 1.4f, 1.0f);

            for (int i = 0; i < 30; i++) {
                double a = Math.random() * Math.PI * 2;
                double rr = Math.random() * 10.0;
                sX[i] = Math.cos(a) * rr;
                sZ[i] = Math.sin(a) * rr;
                sY[i] = 14 + Math.random() * 8;
                sVy[i] = 0.35 + Math.random() * 0.35; // varied fall speed for scatter feel
                sSpin[i] = (Math.random() - 0.5) * 0.6;
                sStartTick[i] = (int)(Math.random() * 180); // spread across 200t window
                sImpacted[i] = false;
                Location p = c.clone().add(sX[i], sY[i], sZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.4f, 0.4f, 0.4f).glow(220, 240, 255).interpolation(2, 0);
                stones.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 SNOW_BLOCK + 6 POWDER_SNOW_BUCKET drift around at varying heights
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double rr = 6 + Math.random() * 3;
                Location p = c.clone().add(Math.cos(a) * rr, 6 + Math.random() * 4, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOW_BLOCK));
                h.scale(0.35f, 0.35f, 0.35f).glow(240, 250, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.4;
                double rr = 4 + Math.random() * 4;
                Location p = c.clone().add(Math.cos(a) * rr, 4 + Math.random() * 6, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.POWDER_SNOW_BUCKET));
                h.scale(0.45f, 0.45f, 0.45f).glow(230, 245, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < stones.size(); i++) {
                if (sImpacted[i] || tick < sStartTick[i]) continue;
                sY[i] -= sVy[i];
                Location pos = c.clone().add(sX[i], sY[i], sZ[i]);

                stones.get(i).animateTo(
                        new Vector3f((float)sX[i] - 0.2f, (float)sY[i], (float)sZ[i] - 0.2f),
                        new AxisAngle4f((float)(tick * sSpin[i] + i), 0, 1, 0),
                        new Vector3f(0.4f), 2);

                // ITEM_SNOWBALL streaks (2/tick per stone)
                w.spawnParticle(Particle.ITEM_SNOWBALL, pos, 2, 0.05, 0.2, 0.05, 0.02);

                if (sY[i] <= 0.5) {
                    Location impact = c.clone().add(sX[i], 0.3, sZ[i]);
                    float pitch = 0.7f + (float)Math.random() * 0.4f;
                    DisplayBuilder.playSound(impact, Sound.BLOCK_POWDER_SNOW_BREAK, 1.2f, pitch);

                    // FALLING_DUST (WHITE_CONCRETE) impact
                    w.spawnParticle(Particle.FALLING_DUST, impact, 12, 0.4, 0.2, 0.4, 0.05,
                            Material.WHITE_CONCRETE.createBlockData());
                    w.spawnParticle(Particle.ITEM_SNOWBALL, impact, 8, 0.3, 0.2, 0.3, 0.1);
                    DisplayBuilder.dustParticles(impact, 6, 0.3, 230, 245, 255, 1.1f);

                    triggerImpactDamage(impact);
                    sImpacted[i] = true;
                }
            }

            // Ambient rain hiss
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.7f, 1.6f);
            }
        }

        @Override public AbstractAttack newInstance() { return new HailstoneShower(plugin); }
    }

    // ================================================================
    // 3. FROZEN JAVELIN RAIN — 8 elongated AMETHYST_SHARD javelins
    //    at 45deg from sky. Impact: radius 3.0, 5000hp each.
    // ================================================================
    public static class FrozenJavelinRain extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> javelins = new ArrayList<>();
        private final double[] jX = new double[8];
        private final double[] jZ = new double[8];
        private final double[] jDx = new double[8];
        private final double[] jDz = new double[8];
        private final double[] jY = new double[8];
        private final double[] jYaw = new double[8];
        private final int[] jStartTick = new int[8];
        private final boolean[] jImpacted = new boolean[8];
        private static final double START_Y = 18.0;
        // 45-degree downward angle: horizontal speed equals vertical fall speed
        private static final double V = 0.7;

        public FrozenJavelinRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_javelin_rain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(60000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(220);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(60000.0);
            config.setImpactRadius(4.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.9f);

            for (int i = 0; i < 8; i++) {
                // Spawn at ring 7 blocks away from impact target near center.
                jYaw[i] = Math.PI * 2 * i / 8 + Math.random() * 0.3;
                jDx[i] = -Math.cos(jYaw[i]); // moves inward toward center
                jDz[i] = -Math.sin(jYaw[i]);
                jX[i] = Math.cos(jYaw[i]) * 7.0;
                jZ[i] = Math.sin(jYaw[i]) * 7.0;
                jY[i] = START_Y;
                jStartTick[i] = i * 5;
                jImpacted[i] = false;
                Location p = c.clone().add(jX[i], jY[i], jZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                // Elongated javelin (0.3 x 0.3 x 2.2)
                h.scale(0.3f, 0.3f, 2.2f).glow(180, 220, 255).interpolation(2, 0);
                javelins.add(h);
                spawnedEntities.add(h.entity());
            }

            // Frost decoration: 6 AMETHYST_CLUSTER + 4 PRISMARINE_CRYSTALS float at spawn cloud
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 8.0, START_Y + 1.5, Math.sin(a) * 8.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_CLUSTER));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 200, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 8;
                Location p = c.clone().add(Math.cos(a) * 6.0, START_Y + 3, Math.sin(a) * 6.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PRISMARINE_CRYSTALS));
                h.scale(0.55f, 0.55f, 0.55f).glow(150, 220, 240).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < javelins.size(); i++) {
                if (jImpacted[i] || tick < jStartTick[i]) continue;
                jY[i] -= V;
                jX[i] += jDx[i] * V;
                jZ[i] += jDz[i] * V;
                Location pos = c.clone().add(jX[i], jY[i], jZ[i]);

                // Rotate javelin so the long Z-axis points along travel vector.
                // Pitch -45deg (pointing down-forward). Yaw to face inward.
                // Use AxisAngle4f around an axis perpendicular to the down-forward plane.
                float yaw = (float) jYaw[i];
                javelins.get(i).animateTo(
                        new Vector3f((float)jX[i] - 0.15f, (float)jY[i], (float)jZ[i] - 0.15f),
                        new AxisAngle4f(yaw, 0, 1, 0),
                        new Vector3f(0.3f, 0.3f, 2.2f), 2);

                // ELECTRIC_SPARK trail (3/tick)
                w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 3, 0.1, 0.1, 0.1, 0.05);
                w.spawnParticle(Particle.SNOWFLAKE, pos, 1, 0.1, 0.1, 0.1, 0.02);

                if (jY[i] <= 0.5) {
                    Location impact = c.clone().add(jX[i], 0.4, jZ[i]);
                    DisplayBuilder.playSound(impact, Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 0.6f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.3f, 1.0f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.8f);

                    // END_ROD impact (16)
                    w.spawnParticle(Particle.END_ROD, impact, 16, 0.5, 0.3, 0.5, 0.3);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, impact, 20, 0.6, 0.4, 0.6, 0.5);
                    w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                    DisplayBuilder.dustParticles(impact, 12, 0.5, 180, 220, 255, 1.4f);

                    triggerImpactDamage(impact);
                    jImpacted[i] = true;
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrozenJavelinRain(plugin); }
    }

    // ================================================================
    // 4. SUBZERO ARROW SWARM — 24 SNOWBALL streak horizontally inward
    //    from a 12-block ring toward center over 80t. Constant damage:
    //    radius 6.0, 2400hp, ticksBetween=14.
    // ================================================================
    public static class SubzeroArrowSwarm extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> arrows = new ArrayList<>();
        private final double[] aAng = new double[24];
        private final double[] aR = new double[24];
        private final double[] aY = new double[24];
        private final int[] aStartTick = new int[24];
        private final boolean[] aDone = new boolean[24];

        public SubzeroArrowSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("subzero_arrow_swarm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(28800.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(140);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_ARROW_SHOOT, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.2f, 2.0f);

            for (int i = 0; i < 24; i++) {
                aAng[i] = Math.PI * 2 * i / 24 + Math.random() * 0.1;
                aR[i] = 12.0;
                aY[i] = 1.0 + (i % 4) * 0.4; // staggered heights
                aStartTick[i] = (i % 6) * 4; // stagger waves
                aDone[i] = false;
                Location p = c.clone().add(Math.cos(aAng[i]) * aR[i], aY[i], Math.sin(aAng[i]) * aR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                // Streak shape — slightly elongated along the radial direction
                h.scale(0.35f, 0.35f, 0.8f).glow(210, 235, 255).interpolation(2, 0);
                arrows.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 ARROW + 6 SPECTRAL_ARROW ring at the outside of the firing arc
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 13.0, 1.5, Math.sin(a) * 13.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ARROW));
                h.scale(0.45f, 0.45f, 0.45f).glow(200, 220, 240).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.3;
                Location p = c.clone().add(Math.cos(a) * 11.5, 2.5, Math.sin(a) * 11.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SPECTRAL_ARROW));
                h.scale(0.4f, 0.4f, 0.4f).glow(180, 220, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Arrows traverse inward over 80 ticks per arrow (after their start)
            for (int i = 0; i < arrows.size(); i++) {
                if (aDone[i] || tick < aStartTick[i]) continue;
                aR[i] -= 0.18; // inward
                if (aR[i] <= 0.5) {
                    aR[i] = 0.5;
                    aDone[i] = true;
                    Location p = c.clone().add(Math.cos(aAng[i]) * aR[i], aY[i], Math.sin(aAng[i]) * aR[i]);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 8, 0.3, 0.2, 0.3, 0.1);
                    DisplayBuilder.dustParticles(p, 6, 0.3, 210, 235, 255, 1.0f);
                    continue;
                }
                float yawRot = (float) aAng[i];
                arrows.get(i).animateTo(
                        new Vector3f((float)(Math.cos(aAng[i]) * aR[i]) - 0.175f,
                                (float)aY[i],
                                (float)(Math.sin(aAng[i]) * aR[i]) - 0.175f),
                        new AxisAngle4f(yawRot, 0, 1, 0),
                        new Vector3f(0.35f, 0.35f, 0.8f), 2);

                // CLOUD trail per arrow
                Location pos = c.clone().add(Math.cos(aAng[i]) * aR[i], aY[i], Math.sin(aAng[i]) * aR[i]);
                w.spawnParticle(Particle.CLOUD, pos, 1, 0.05, 0.05, 0.05, 0.01);
                w.spawnParticle(Particle.SNOWFLAKE, pos, 1, 0.05, 0.05, 0.05, 0.02);
            }

            // Chime per 8 ticks
            if (tick % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 2.0f);
            }

            // Constant damage in center radius 6
            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SubzeroArrowSwarm(plugin); }
    }

    // ================================================================
    // 5. GLASS SHARD RAIN — 40 GLASS_BOTTLE drift down slowly from Y+12
    //    across a 14-block square in 220t. Constant: radius 8.0, 2800hp,
    //    ticksBetween=12.
    // ================================================================
    public static class GlassShardRain extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] gX = new double[40];
        private final double[] gZ = new double[40];
        private final double[] gY = new double[40];
        private final double[] gDrift = new double[40];
        private final boolean[] gLanded = new boolean[40];
        private final int[] gStartTick = new int[40];

        public GlassShardRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glass_shard_rain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(33600.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(280);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.4f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.5f);

            for (int i = 0; i < 40; i++) {
                // Square 14x14 around center
                gX[i] = (Math.random() - 0.5) * 14.0;
                gZ[i] = (Math.random() - 0.5) * 14.0;
                gY[i] = 12 + Math.random() * 2.5;
                gDrift[i] = 0.06 + Math.random() * 0.05; // slow drift
                gStartTick[i] = (int)(Math.random() * 100);
                gLanded[i] = false;
                Location p = c.clone().add(gX[i], gY[i], gZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 230, 255).interpolation(6, 0);
                shards.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 GLASS_PANE + 6 ICE float at the cloud level
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double rr = 4 + Math.random() * 3;
                Location p = c.clone().add(Math.cos(a) * rr, 10 + Math.random() * 2, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_PANE));
                h.scale(0.45f, 0.45f, 0.05f).glow(220, 240, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.3;
                Location p = c.clone().add(Math.cos(a) * 5.5, 11 + Math.random() * 2, Math.sin(a) * 5.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 240, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < shards.size(); i++) {
                if (gLanded[i] || tick < gStartTick[i]) continue;
                gY[i] -= gDrift[i];
                // Slight wobble in X/Z as it drifts
                double wobbleX = Math.sin(tick * 0.07 + i) * 0.02;
                double wobbleZ = Math.cos(tick * 0.07 + i) * 0.02;
                gX[i] += wobbleX;
                gZ[i] += wobbleZ;

                Location pos = c.clone().add(gX[i], gY[i], gZ[i]);
                shards.get(i).animateTo(
                        new Vector3f((float)gX[i] - 0.2f, (float)gY[i], (float)gZ[i] - 0.2f),
                        new AxisAngle4f((float)(tick * 0.08 + i), 0, 1, 0),
                        new Vector3f(0.4f), 6);

                // GLOW particle per shard (sparse)
                if (tick % 3 == 0) {
                    w.spawnParticle(Particle.GLOW, pos, 1, 0.05, 0.1, 0.05, 0.01);
                }

                if (gY[i] <= 0.4) {
                    Location impact = c.clone().add(gX[i], 0.3, gZ[i]);
                    w.spawnParticle(Particle.END_ROD, impact, 8, 0.3, 0.2, 0.3, 0.1);
                    DisplayBuilder.dustParticles(impact, 4, 0.2, 220, 240, 255, 1.0f);
                    gLanded[i] = true;
                }
            }

            // Random tinkle sounds 4-8 ticks
            if (tick % (4 + (int)(Math.random() * 5)) == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.2f + (float)Math.random() * 0.6f);
            }

            // Constant damage radius 8
            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new GlassShardRain(plugin); }
    }

    // ================================================================
    // 6. CRYO MORTAR — 1 NETHER_STAR ItemDisplay (scale 1.4) lobs in a
    //    parabolic arc from outside the radius to center. Single huge
    //    impact: radius 9.0, 5800hp.
    // ================================================================
    public static class CryoMortar extends EnvironmentalAttack {
        private ItemDisplayHandle mortar;
        private double t = 0;
        private double startX, startZ;
        private boolean impacted = false;
        private static final double APEX_Y = 16.0;
        private static final int FLIGHT_TICKS = 60;

        public CryoMortar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryo_mortar", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(69600.0);
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(140);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(69600.0);
            config.setImpactRadius(13.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.6f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);

            // Start at outside ring, fixed angle
            double startAng = Math.random() * Math.PI * 2;
            startX = Math.cos(startAng) * 14.0;
            startZ = Math.sin(startAng) * 14.0;
            Location p = c.clone().add(startX, 1.0, startZ);
            mortar = displayBuilder.spawnItem(p, new ItemStack(Material.NETHER_STAR));
            mortar.scale(1.4f, 1.4f, 1.4f).glow(180, 230, 255).interpolation(2, 0);
            spawnedEntities.add(mortar.entity());

            // Frosty trail dressing: 8 SNOWBALL + 6 PRISMARINE_SHARD around the launch
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double rr = 1.0 + Math.random() * 0.8;
                Location pp = c.clone().add(startX + Math.cos(a) * rr, 1.0 + Math.random() * 0.5, startZ + Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.SNOWBALL));
                h.scale(0.4f, 0.4f, 0.4f).glow(220, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.2;
                double rr = 1.5 + Math.random();
                Location pp = c.clone().add(startX + Math.cos(a) * rr, 0.6 + Math.random(), startZ + Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.PRISMARINE_SHARD));
                h.scale(0.5f, 0.5f, 0.5f).glow(160, 220, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (mortar == null || impacted) return;

            t = Math.min(1.0, tick / (double) FLIGHT_TICKS);
            // Linear interpolation X/Z, parabolic Y
            double x = startX * (1.0 - t);
            double z = startZ * (1.0 - t);
            // Parabola — apex at t=0.5
            double y = 1.0 + 4 * APEX_Y * t * (1.0 - t);
            Location pos = c.clone().add(x, y, z);

            mortar.animateTo(
                    new Vector3f((float)x - 0.7f, (float)y, (float)z - 0.7f),
                    new AxisAngle4f((float)(tick * 0.25), 0, 1, 0),
                    new Vector3f(1.4f), 2);

            // SNOWFLAKE dense trail
            w.spawnParticle(Particle.SNOWFLAKE, pos, 6, 0.25, 0.25, 0.25, 0.04);
            // SOUL_FIRE_FLAME core
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 4, 0.12, 0.12, 0.12, 0.02);
            DisplayBuilder.dustParticles(pos, 2, 0.15, 180, 230, 255, 1.3f);

            // Howling sweep during flight
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(pos, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.7f, 0.6f);
            }

            if (t >= 1.0) {
                Location impact = c.clone().add(0, 0.4, 0);
                impacted = true;

                DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.6f);
                DisplayBuilder.playSound(impact, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.6f, 0.5f);
                DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.7f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 0.5f);

                // ITEM_SNOWBALL explosion (60)
                w.spawnParticle(Particle.ITEM_SNOWBALL, impact, 60, 1.2, 0.8, 1.2, 0.6);
                // END_ROD burst (30)
                w.spawnParticle(Particle.END_ROD, impact, 30, 1.0, 0.5, 1.0, 0.3);
                w.spawnParticle(Particle.SNOWFLAKE, impact, 40, 1.0, 0.5, 1.0, 0.2);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, impact, 20, 0.8, 0.4, 0.8, 0.15);
                DisplayBuilder.dustParticles(impact, 40, 1.5, 180, 230, 255, 1.8f);

                // Outer shockwave ring
                for (int r = 1; r <= 9; r++) {
                    final int rr = r;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        DisplayBuilder.particleRing(impact, rr, Particle.SNOWFLAKE, 36, null);
                        DisplayBuilder.particleRing(impact, rr, Particle.ITEM_SNOWBALL, 24, null);
                    }, r * 2L);
                }

                triggerImpactDamage(impact);
            }
        }

        @Override public AbstractAttack newInstance() { return new CryoMortar(plugin); }
    }

    // ================================================================
    // 7. POLAR SPEAR — 1 huge ICE ItemDisplay hovers at Y+15 for 40t,
    //    rotates to point at center, then descends fast. Impact: radius
    //    5.0, 5500hp.
    // ================================================================
    public static class PolarSpear extends EnvironmentalAttack {
        private ItemDisplayHandle spear;
        private double sY = 15.0;
        private boolean descending = false;
        private boolean impacted = false;
        private static final int HOVER_TICKS = 40;

        public PolarSpear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("polar_spear", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(66000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(160);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(66000.0);
            config.setImpactRadius(7.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CONDUIT_ACTIVATE, 1.0f, 0.8f);

            Location p = c.clone().add(0, sY, 0);
            spear = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
            // Spear-like elongated scale: 0.6 x 0.6 x 3.5 (tall/long)
            spear.scale(0.6f, 0.6f, 3.5f).glow(180, 230, 255).interpolation(4, 0);
            spawnedEntities.add(spear.entity());

            // 8 PACKED_ICE + 6 BLUE_ICE chunks float in a sigil around the spear
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location pp = c.clone().add(Math.cos(a) * 2.5, sY + Math.sin(a) * 0.6, Math.sin(a) * 2.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.PACKED_ICE));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.2;
                Location pp = c.clone().add(Math.cos(a) * 1.5, sY - 1.5, Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.BLUE_ICE));
                h.scale(0.55f, 0.55f, 0.55f).glow(160, 220, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (spear == null || impacted) return;

            if (!descending && tick < HOVER_TICKS) {
                // Pre-launch hover, rotating to face center (point-down)
                // Building tension: ELECTRIC_SPARK 8/tick around the spear
                double phase = tick / (double) HOVER_TICKS;
                // Pitch from horizontal (0) to fully vertical (-90deg / point-down)
                // We rotate around X-axis. Long axis was Z; rotating PI/2 around X makes Z point down.
                float pitch = (float) (phase * Math.PI / 2);

                // Rising chime pitch 0.5 → 1.8 over 40t
                if (tick % 4 == 0) {
                    float chime = 0.5f + (float) phase * 1.3f;
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, chime);
                }

                spear.animateTo(
                        new Vector3f(-0.3f, (float)sY, -1.75f),
                        new AxisAngle4f(pitch, 1, 0, 0),
                        new Vector3f(0.6f, 0.6f, 3.5f), 4);

                Location pos = c.clone().add(0, sY, 0);
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 0.8 + Math.random() * 0.6;
                    Location sp = pos.clone().add(Math.cos(a) * r, (Math.random() - 0.5) * 2.0, Math.sin(a) * r);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, sp, 1, 0.05, 0.05, 0.05, 0.05);
                }
                DisplayBuilder.dustParticles(pos, 2, 0.4, 180, 230, 255, 1.2f);
            } else if (!descending) {
                descending = true;
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.6f, 0.5f);
            } else {
                // Fast descent
                sY -= 1.2;

                spear.animateTo(
                        new Vector3f(-0.3f, (float)sY, -1.75f),
                        new AxisAngle4f((float)(Math.PI / 2), 1, 0, 0),
                        new Vector3f(0.6f, 0.6f, 3.5f), 2);

                Location pos = c.clone().add(0, sY, 0);
                // SNOWFLAKE descent trail
                w.spawnParticle(Particle.SNOWFLAKE, pos, 8, 0.2, 0.4, 0.2, 0.05);
                w.spawnParticle(Particle.CLOUD, pos, 4, 0.2, 0.3, 0.2, 0.03);

                if (sY <= 1.0) {
                    Location impact = c.clone().add(0, 0.4, 0);
                    impacted = true;

                    DisplayBuilder.playSound(impact, Sound.ITEM_TRIDENT_HIT_GROUND, 1.6f, 0.4f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.6f);

                    // FALLING_DUST impact
                    w.spawnParticle(Particle.FALLING_DUST, impact, 40, 1.0, 0.5, 1.0, 0.1,
                            Material.PACKED_ICE.createBlockData());
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 40, 1.0, 0.4, 1.0, 0.2);
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                    DisplayBuilder.dustParticles(impact, 30, 1.0, 180, 230, 255, 1.6f);

                    // Outward shockwave rings
                    for (int r = 1; r <= 5; r++) {
                        final int rr = r;
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            DisplayBuilder.particleRing(impact, rr, Particle.SNOWFLAKE, 30, null);
                        }, r * 3L);
                    }

                    triggerImpactDamage(impact);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new PolarSpear(plugin); }
    }

    // ================================================================
    // 8. SLEET VOLLEY — 50 tiny SNOWBALL fan-shaped horizontal volley
    //    sweeping a 90deg arc over 100t. Constant radius 10.0, 1800hp,
    //    ticksBetween=8.
    // ================================================================
    public static class SleetVolley extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> sleets = new ArrayList<>();
        private final double[] slR = new double[50];
        private final double[] slY = new double[50];
        private final double[] slOffset = new double[50]; // angular offset within the fan
        private final int[] slStartTick = new int[50];
        private final boolean[] slDone = new boolean[50];
        private double baseAng; // direction the fan is centered on

        public SleetVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sleet_volley", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(21600.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(160);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.6f, 1.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_SWOOP, 1.0f, 1.5f);

            // Aim fan at nearest player or random direction
            Player target = findNearestPlayer(c, 30.0);
            if (target != null) {
                double dx = target.getLocation().getX() - c.getX();
                double dz = target.getLocation().getZ() - c.getZ();
                baseAng = Math.atan2(dz, dx);
            } else {
                baseAng = Math.random() * Math.PI * 2;
            }

            for (int i = 0; i < 50; i++) {
                // 50 sleet pellets distributed across the fan (-45deg to +45deg)
                slOffset[i] = (Math.random() - 0.5) * Math.PI / 2;
                slR[i] = 0.5 + Math.random() * 1.5;
                slY[i] = 1.0 + Math.random() * 2.5;
                slStartTick[i] = (int)(Math.random() * 90);
                slDone[i] = false;
                double a = baseAng + slOffset[i];
                Location p = c.clone().add(Math.cos(a) * slR[i], slY[i], Math.sin(a) * slR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.25f, 0.25f, 0.25f).glow(220, 240, 255).interpolation(2, 0);
                sleets.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 SNOW_BLOCK + 4 ICE chunks behind the launch as backdrop
            for (int i = 0; i < 8; i++) {
                double a = baseAng + Math.PI + (Math.random() - 0.5) * 0.6;
                double rr = 1 + Math.random() * 1.5;
                Location p = c.clone().add(Math.cos(a) * rr, 1.5 + Math.random() * 1.0, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOW_BLOCK));
                h.scale(0.4f, 0.4f, 0.4f).glow(240, 250, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = baseAng + Math.PI + (Math.random() - 0.5) * 0.4;
                double rr = 0.6 + Math.random() * 0.8;
                Location p = c.clone().add(Math.cos(a) * rr, 1.0 + Math.random() * 1.0, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.45f, 0.45f, 0.45f).glow(200, 240, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Add subtle sweep — fan center rotates across 90deg over 100t
            double sweepProgress = Math.min(1.0, tick / 100.0);
            double sweepAng = baseAng + (sweepProgress - 0.5) * Math.PI / 2;

            for (int i = 0; i < sleets.size(); i++) {
                if (slDone[i] || tick < slStartTick[i]) continue;
                slR[i] += 0.35; // outward velocity
                double a = sweepAng + slOffset[i];
                double x = Math.cos(a) * slR[i];
                double z = Math.sin(a) * slR[i];
                Location pos = c.clone().add(x, slY[i], z);

                sleets.get(i).animateTo(
                        new Vector3f((float)x - 0.125f, (float)slY[i], (float)z - 0.125f),
                        new AxisAngle4f((float)(tick * 0.5 + i), 0, 1, 0),
                        new Vector3f(0.25f), 2);

                // CLOUD streak per sleet
                w.spawnParticle(Particle.CLOUD, pos, 1, 0.05, 0.05, 0.05, 0.01);
                if (Math.random() < 0.15) {
                    w.spawnParticle(Particle.DRIPPING_WATER, pos, 1, 0.05, 0.05, 0.05, 0);
                }

                if (slR[i] > 11.0) {
                    slDone[i] = true;
                    DisplayBuilder.dustParticles(pos, 2, 0.15, 220, 240, 255, 1.0f);
                }
            }

            // Looped rain sound
            if (tick % 18 == 0) {
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.6f, 1.6f);
            }

            // Constant damage in radius 10
            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SleetVolley(plugin); }
    }

    // ================================================================
    // 9. FROSTBITE DARTS — 12 AMETHYST_SHARD darts spawn at Y+3 in a
    //    circle, fire inward toward center staggered. Impact each:
    //    radius 2.0, 3000hp.
    // ================================================================
    public static class FrostbiteDarts extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> darts = new ArrayList<>();
        private final double[] dAng = new double[12];
        private final double[] dR = new double[12];
        private final int[] dStartTick = new int[12];
        private final boolean[] dFired = new boolean[12];
        private final boolean[] dImpacted = new boolean[12];
        private static final double START_R = 7.0;

        public FrostbiteDarts(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frostbite_darts", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(36000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(200);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(36000.0);
            config.setImpactRadius(3.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.0f);

            for (int i = 0; i < 12; i++) {
                dAng[i] = Math.PI * 2 * i / 12;
                dR[i] = START_R;
                dStartTick[i] = i * 6; // staggered fire
                dFired[i] = false;
                dImpacted[i] = false;
                Location p = c.clone().add(Math.cos(dAng[i]) * dR[i], 3.0, Math.sin(dAng[i]) * dR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                // Dart-like shape
                h.scale(0.25f, 0.25f, 0.7f).glow(220, 180, 255).interpolation(2, 0);
                darts.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 AMETHYST_CLUSTER hovering above the dart circle as anchors
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * START_R, 4.5, Math.sin(a) * START_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_CLUSTER));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 180, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < darts.size(); i++) {
                if (dImpacted[i]) continue;
                if (tick < dStartTick[i]) continue;

                if (!dFired[i]) {
                    // Rising chime as dart locks on
                    float chime = 1.0f + (tick - dStartTick[i]) * 0.05f;
                    if ((tick - dStartTick[i]) % 3 == 0 && (tick - dStartTick[i]) < 15) {
                        DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, Math.min(2.0f, chime));
                    }
                    if (tick - dStartTick[i] >= 12) {
                        dFired[i] = true;
                        Location pos = c.clone().add(Math.cos(dAng[i]) * dR[i], 3.0, Math.sin(dAng[i]) * dR[i]);
                        DisplayBuilder.playSound(pos, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.2f, 1.6f);
                    }
                    // Hover wiggle
                    Location pos = c.clone().add(Math.cos(dAng[i]) * dR[i], 3.0 + Math.sin(tick * 0.2 + i) * 0.2, Math.sin(dAng[i]) * dR[i]);
                    darts.get(i).animateTo(
                            new Vector3f((float)(Math.cos(dAng[i]) * dR[i]) - 0.125f, (float)(pos.getY() - c.getY()), (float)(Math.sin(dAng[i]) * dR[i]) - 0.125f),
                            new AxisAngle4f((float)dAng[i] + (float)Math.PI, 0, 1, 0),
                            new Vector3f(0.25f, 0.25f, 0.7f), 2);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 2, 0.1, 0.1, 0.1, 0.03);
                } else {
                    // Fired — fly inward
                    dR[i] -= 0.55;
                    Location pos = c.clone().add(Math.cos(dAng[i]) * dR[i], 3.0, Math.sin(dAng[i]) * dR[i]);
                    darts.get(i).animateTo(
                            new Vector3f((float)(Math.cos(dAng[i]) * dR[i]) - 0.125f, 3.0f, (float)(Math.sin(dAng[i]) * dR[i]) - 0.125f),
                            new AxisAngle4f((float)dAng[i] + (float)Math.PI, 0, 1, 0),
                            new Vector3f(0.25f, 0.25f, 0.7f), 2);

                    // ELECTRIC_SPARK trail
                    w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 3, 0.05, 0.05, 0.05, 0.05);
                    w.spawnParticle(Particle.SNOWFLAKE, pos, 1, 0.05, 0.05, 0.05, 0.02);

                    if (dR[i] <= 0.4) {
                        Location impact = c.clone().add(Math.cos(dAng[i]) * 0.2, 2.5, Math.sin(dAng[i]) * 0.2);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.4f, 1.4f);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.3f);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, impact, 24, 0.5, 0.4, 0.5, 0.4);
                        w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.SNOWFLAKE, impact, 12, 0.4, 0.3, 0.4, 0.1);
                        DisplayBuilder.dustParticles(impact, 10, 0.4, 220, 180, 255, 1.4f);
                        triggerImpactDamage(impact);
                        dImpacted[i] = true;
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostbiteDarts(plugin); }
    }

    // ================================================================
    // 10. COMET STRIKE — 1 DIAMOND ItemDisplay (scale 1.8) sweeps down
    //     a steep diagonal from outside radius with a long tail. Impact:
    //     radius 11.0, 6000hp.
    // ================================================================
    public static class CometStrike extends EnvironmentalAttack {
        private ItemDisplayHandle comet;
        private double cX, cY, cZ;
        private double dx, dy, dz;
        private boolean impacted = false;

        public CometStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("comet_strike", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(72000.0);
            config.setDamageRadius(16.5);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(180);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(72000.0);
            config.setImpactRadius(16.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.6f, 0.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.7f);

            // Start at outside radius (15 blocks) and Y+20; steep diagonal toward center
            double ang = Math.random() * Math.PI * 2;
            cX = Math.cos(ang) * 15.0;
            cZ = Math.sin(ang) * 15.0;
            cY = 20.0;

            // Travel vector toward (0, 0.4, 0) over ~50 ticks
            double targetX = 0, targetY = 0.4, targetZ = 0;
            double dist = Math.sqrt((cX - targetX) * (cX - targetX)
                    + (cY - targetY) * (cY - targetY)
                    + (cZ - targetZ) * (cZ - targetZ));
            double travelTicks = 50.0;
            dx = (targetX - cX) / travelTicks;
            dy = (targetY - cY) / travelTicks;
            dz = (targetZ - cZ) / travelTicks;
            // dist is incidental, but compiler-friendly use:
            if (dist <= 0) dist = 1; // guard

            Location p = c.clone().add(cX, cY, cZ);
            comet = displayBuilder.spawnItem(p, new ItemStack(Material.DIAMOND));
            comet.scale(1.8f, 1.8f, 1.8f).glow(160, 230, 255).interpolation(2, 0);
            spawnedEntities.add(comet.entity());

            // Long tail trailing entities: 10 DIAMOND + 8 SNOWBALL stretching behind
            for (int i = 0; i < 10; i++) {
                double back = (i + 1) * 0.4;
                Location pp = c.clone().add(cX - dx * back * 20, cY - dy * back * 20, cZ - dz * back * 20);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.DIAMOND));
                h.scale(0.4f - i * 0.025f, 0.4f - i * 0.025f, 0.4f - i * 0.025f).glow(160, 230, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 8; i++) {
                double back = (i + 1) * 0.6;
                Location pp = c.clone().add(cX - dx * back * 25, cY - dy * back * 25, cZ - dz * back * 25);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.SNOWBALL));
                h.scale(0.35f, 0.35f, 0.35f).glow(220, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (comet == null || impacted) return;

            cX += dx;
            cY += dy;
            cZ += dz;
            Location pos = c.clone().add(cX, cY, cZ);

            comet.animateTo(
                    new Vector3f((float)cX - 0.9f, (float)cY, (float)cZ - 0.9f),
                    new AxisAngle4f((float)(tick * 0.4), 1, 1, 0),
                    new Vector3f(1.8f), 2);

            // SOUL_FIRE_FLAME core + SNOWFLAKE dense trail (15/tick)
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 6, 0.25, 0.25, 0.25, 0.04);
            w.spawnParticle(Particle.SNOWFLAKE, pos, 15, 0.4, 0.4, 0.4, 0.06);
            w.spawnParticle(Particle.FLAME, pos, 4, 0.2, 0.2, 0.2, 0.04);
            DisplayBuilder.dustParticles(pos, 4, 0.3, 180, 230, 255, 1.5f);

            // Long pull-in howl
            if (tick % 8 == 0) {
                DisplayBuilder.playSound(pos, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.8f, 0.5f);
            }

            if (cY <= 0.6) {
                Location impact = c.clone().add(0, 0.4, 0);
                impacted = true;

                DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.5f);
                DisplayBuilder.playSound(impact, Sound.ITEM_TRIDENT_THUNDER, 1.6f, 0.6f);
                DisplayBuilder.playSound(impact, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.4f, 0.7f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.6f);

                // ELECTRIC_SPARK burst (50)
                w.spawnParticle(Particle.ELECTRIC_SPARK, impact, 50, 1.5, 1.0, 1.5, 0.6);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, impact, 40, 1.2, 0.8, 1.2, 0.3);
                w.spawnParticle(Particle.SNOWFLAKE, impact, 60, 1.5, 0.6, 1.5, 0.2);
                w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 2, 0.5, 0.2, 0.5, 0);
                w.spawnParticle(Particle.FLASH, impact, 2, 0.1, 0.1, 0.1, 0);
                DisplayBuilder.dustParticles(impact, 60, 2.0, 180, 230, 255, 2.0f);

                // Massive outward rings
                for (int r = 1; r <= 11; r++) {
                    final int rr = r;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        DisplayBuilder.particleRing(impact, rr, Particle.SNOWFLAKE, 40, null);
                        DisplayBuilder.particleRing(impact, rr, Particle.ELECTRIC_SPARK, 30, null);
                        if (rr % 2 == 0) DisplayBuilder.particleRing(impact, rr, Particle.SOUL_FIRE_FLAME, 24, null);
                    }, r * 2L);
                }

                triggerImpactDamage(impact);
            }
        }

        @Override public AbstractAttack newInstance() { return new CometStrike(plugin); }
    }
}
