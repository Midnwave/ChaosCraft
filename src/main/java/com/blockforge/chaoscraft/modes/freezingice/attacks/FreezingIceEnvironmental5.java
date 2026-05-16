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
 * FreezingIce Mode — ENVIRONMENTAL FX BATCH 5 (entries 41-50).
 * Surreal + Mobile Chaser attacks. Pure ItemDisplay + particle attacks. NO BlockDisplays.
 * Themes: echoing bells, drifting specters, hunting zones, prowling orbs,
 * roaming beasts, shivering chainpetals, frostfire orbitals, cryostasis domes,
 * subzero vortexes, and the climactic absolute-zero implosion.
 *
 * Follow-AI attacks (3): DriftingFrostSpecter (0.09), HuntingColdZone (0.08),
 * RoamingHoarfrostBeast (0.11).
 */
public final class FreezingIceEnvironmental5 {
    private FreezingIceEnvironmental5() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new EchoingFrostBell(plugin));
        registry.register(new DriftingFrostSpecter(plugin));
        registry.register(new HuntingColdZone(plugin));
        registry.register(new ProwlingBlizzardOrb(plugin));
        registry.register(new RoamingHoarfrostBeast(plugin));
        registry.register(new ShiveringChainpetals(plugin));
        registry.register(new FrostfireOrbital(plugin));
        registry.register(new CryostasisDome(plugin));
        registry.register(new SubzeroVortex(plugin));
        registry.register(new AbsoluteZeroPoint(plugin));
    }

    // ================================================================
    // 41. ECHOING FROST BELL — 1 NAUTILUS_SHELL bell pulses scale
    //     1.0->1.3->1.0 every 30 ticks emitting SONIC_BOOM ring pulse.
    //     Constant pulse damage radius 10.0, 4000hp, 30-tick interval.
    // ================================================================
    public static class EchoingFrostBell extends EnvironmentalAttack {
        private ItemDisplayHandle bell;

        public EchoingFrostBell(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echoing_frost_bell", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(48000.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(400);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_USE, 1.6f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 0.8f);

            Location bellPos = c.clone().add(0, 4.0, 0);
            bell = displayBuilder.spawnItem(bellPos, new ItemStack(Material.NAUTILUS_SHELL));
            bell.scale(1.4f, 1.6f, 1.4f).glow(180, 220, 255).interpolation(8, 0);
            spawnedEntities.add(bell.entity());

            // Bell rim decorations: 8 NAUTILUS_SHELL in a ring around the base,
            // 6 ECHO_SHARD floating above as resonator crystals.
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 1.6, 3.2, Math.sin(a) * 1.6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NAUTILUS_SHELL));
                h.scale(0.45f, 0.45f, 0.45f).glow(160, 200, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.2;
                Location p = c.clone().add(Math.cos(a) * 0.9, 5.4 + Math.sin(a) * 0.4, Math.sin(a) * 0.9);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.5f, 0.5f, 0.5f).glow(140, 200, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (bell == null) return;

            // Pulse bell scale every 30 ticks: 1.0 -> 1.3 -> 1.0
            double pulsePhase = (tick % 30) / 30.0;
            double scaleMul = 1.0 + Math.sin(pulsePhase * Math.PI) * 0.3;
            float sx = (float)(1.4 * scaleMul);
            float sy = (float)(1.6 * scaleMul);
            float sz = (float)(1.4 * scaleMul);
            bell.animateTo(
                    new Vector3f(-sx / 2, 4.0f - sy / 2, -sz / 2),
                    new AxisAngle4f((float)(tick * 0.04), 0, 1, 0),
                    new Vector3f(sx, sy, sz), 4);

            // SONIC_BOOM ring pulse every 30 ticks at the pulse peak
            if (tick % 30 == 15) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BELL_USE, 1.8f, 0.7f);
                DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 1.4f, 0.9f);
                Location bellLoc = c.clone().add(0, 4.0, 0);
                for (int r = 1; r <= 10; r++) {
                    final int rr = r;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        DisplayBuilder.particleRing(c.clone().add(0, 0.4, 0), rr, Particle.SONIC_BOOM, 24, null);
                        DisplayBuilder.dustParticles(bellLoc, 6, 1.2, 180, 220, 255, 1.4f);
                    }, r * 2L);
                }
            }

            // Continuous shimmer at bell
            if (tick % 4 == 0) {
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 4.0, 0), 2, 0.6, 0.4, 0.6, 0.02);
                DisplayBuilder.dustParticles(c.clone().add(0, 4.0, 0), 2, 0.5, 180, 220, 255, 1.0f);
            }
            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);

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

        @Override public AbstractAttack newInstance() { return new EchoingFrostBell(plugin); }
    }

    // ================================================================
    // 42. DRIFTING FROST SPECTER [FOLLOW-AI 0.09] — 9 PHANTOM_MEMBRANE in
    //     humanoid form (3 head + 3 body + 3 limbs cluster) drifts toward
    //     player. Constant radius 4.5, 3600hp, 12-tick interval.
    // ================================================================
    public static class DriftingFrostSpecter extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> parts = new ArrayList<>();
        // Layout indices: 0-2 head, 3-5 body, 6-8 limbs
        private final double[] partOX = new double[9];
        private final double[] partOY = new double[9];
        private final double[] partOZ = new double[9];

        public DriftingFrostSpecter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("drifting_frost_specter", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(43200.0);
            config.setDamageRadius(6.75);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
            // FOLLOW-AI: drift toward player at sub-walk pace.
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.09);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_AMBIENT, 1.2f, 0.7f);

            // 3 head cluster at top
            partOX[0] = 0;    partOY[0] = 3.8; partOZ[0] = 0;
            partOX[1] = 0.25; partOY[1] = 3.9; partOZ[1] = 0.1;
            partOX[2] = -0.25; partOY[2] = 3.7; partOZ[2] = -0.1;
            // 3 body cluster at center
            partOX[3] = 0;    partOY[3] = 2.5; partOZ[3] = 0;
            partOX[4] = 0.2;  partOY[4] = 2.0; partOZ[4] = 0;
            partOX[5] = -0.2; partOY[5] = 1.6; partOZ[5] = 0;
            // 3 limb cluster at bottom (arms + leg trail)
            partOX[6] = 0.55;  partOY[6] = 2.2; partOZ[6] = 0;
            partOX[7] = -0.55; partOY[7] = 2.2; partOZ[7] = 0;
            partOX[8] = 0;     partOY[8] = 0.8; partOZ[8] = 0;

            for (int i = 0; i < 9; i++) {
                Location p = c.clone().add(partOX[i], partOY[i], partOZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                float s = (i < 3) ? 0.85f : (i < 6) ? 0.95f : 0.7f;
                h.scale(s, s, s).glow(180, 200, 240).interpolation(4, 0);
                parts.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ghostly aura: 6 ECHO_SHARD orbiting + 4 PHANTOM_MEMBRANE drifting trails
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 1.4, 2.0 + Math.sin(a) * 0.6, Math.sin(a) * 1.4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.4f, 0.4f, 0.4f).glow(140, 180, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + 0.3;
                Location p = c.clone().add(Math.cos(a) * 0.8, 1.0 + i * 0.7, Math.sin(a) * 0.8);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.55f, 0.55f, 0.55f).glow(170, 210, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Float-bob each part
            for (int i = 0; i < parts.size(); i++) {
                double bobY = partOY[i] + Math.sin(tick * 0.08 + i * 0.5) * 0.18;
                double sway = Math.sin(tick * 0.05 + i) * 0.08;
                float s = (i < 3) ? 0.85f : (i < 6) ? 0.95f : 0.7f;
                parts.get(i).animateTo(
                        new Vector3f((float)(partOX[i] + sway) - s / 2, (float)bobY, (float)partOZ[i] - s / 2),
                        new AxisAngle4f((float)(tick * 0.04 + i), 0, 1, 0),
                        new Vector3f(s), 4);
            }

            // SCULK_SOUL + END_ROD shimmer
            if (tick % 2 == 0) {
                for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 1.0;
                    double y = 0.5 + Math.random() * 3.5;
                    w.spawnParticle(Particle.SCULK_SOUL, c.clone().add(Math.cos(a) * r, y, Math.sin(a) * r),
                            1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            if (tick % 3 == 0) {
                for (int i = 0; i < 3; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 1.2;
                    double y = 1.0 + Math.random() * 3.0;
                    w.spawnParticle(Particle.END_ROD, c.clone().add(Math.cos(a) * r, y, Math.sin(a) * r),
                            1, 0.05, 0.05, 0.05, 0.005);
                }
            }
            if (tick % 40 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.9f, 0.8f);

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

        @Override public AbstractAttack newInstance() { return new DriftingFrostSpecter(plugin); }
    }

    // ================================================================
    // 43. HUNTING COLD ZONE [FOLLOW-AI 0.08] — 24 BLUE_ICE perimeter ring
    //     (8-block radius) slides toward player. Constant 6.0r, 3200hp,
    //     10-tick interval.
    // ================================================================
    public static class HuntingColdZone extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> ring = new ArrayList<>();
        private final double[] rAng = new double[24];
        private static final double RING_RADIUS = 8.0;

        public HuntingColdZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hunting_cold_zone", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(38400.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(420);
            config.setCooldownTicks(60);
            // FOLLOW-AI: zone hunts the player.
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.08);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_GHAST_AMBIENT, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.9f);

            for (int i = 0; i < 24; i++) {
                rAng[i] = Math.PI * 2 * i / 24;
                Location p = c.clone().add(Math.cos(rAng[i]) * RING_RADIUS, 0.4, Math.sin(rAng[i]) * RING_RADIUS);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.7f, 0.4f, 0.7f).glow(120, 180, 255).interpolation(10, 0);
                ring.add(h);
                spawnedEntities.add(h.entity());
            }

            // Interior chill: 8 PHANTOM_MEMBRANE floating at mid-radius + 6 ECHO_SHARD at center
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double rr = 4.0;
                Location p = c.clone().add(Math.cos(a) * rr, 1.6 + Math.sin(a) * 0.4, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.55f, 0.55f, 0.55f).glow(140, 180, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 1.0, 1.0 + Math.sin(a) * 0.3, Math.sin(a) * 1.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.5f, 0.5f, 0.5f).glow(160, 200, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slow ring rotation
            for (int i = 0; i < ring.size(); i++) {
                rAng[i] += 0.012;
                float bx = (float)(Math.cos(rAng[i]) * RING_RADIUS);
                float bz = (float)(Math.sin(rAng[i]) * RING_RADIUS);
                float by = (float)(0.4 + Math.sin(tick * 0.05 + i) * 0.15);
                ring.get(i).animateTo(
                        new Vector3f(bx - 0.35f, by, bz - 0.35f),
                        new AxisAngle4f((float)(tick * 0.04 + i * 0.2), 0, 1, 0),
                        new Vector3f(0.7f, 0.4f, 0.7f), 10);
            }

            // SNOWFLAKE at perimeter
            if (tick % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    Location p = c.clone().add(Math.cos(a) * RING_RADIUS, 0.5 + Math.random() * 0.6, Math.sin(a) * RING_RADIUS);
                    w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.2, 0.2, 0.2, 0.02);
                }
            }
            // Interior chill mist
            if (tick % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * RING_RADIUS;
                    Location p = c.clone().add(Math.cos(a) * r, 0.6 + Math.random() * 1.2, Math.sin(a) * r);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.01);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 140, 200, 240, 0.9f);
                }
            }
            if (tick % 50 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GHAST_AMBIENT, 0.8f, 0.7f);

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

        @Override public AbstractAttack newInstance() { return new HuntingColdZone(plugin); }
    }

    // ================================================================
    // 44. PROWLING BLIZZARD ORB — 16 SNOWBALL 3-axis orbiting sphere
    //     (X-orbit, Y-orbit, Z-orbit ~5 each). No follow-AI; random
    //     walk drift. Constant 5.0r, 3400hp, 11-tick interval.
    // ================================================================
    public static class ProwlingBlizzardOrb extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> orbs = new ArrayList<>();
        private final double[] oAng = new double[16];
        private final int[] oAxis = new int[16]; // 0=X-axis orbit, 1=Y-axis orbit, 2=Z-axis orbit
        private double offX = 0, offZ = 0;
        private double driftAng = Math.random() * Math.PI * 2;
        private static final double ORB_R = 2.6;
        private static final double ORB_CENTER_Y = 2.2;

        public ProwlingBlizzardOrb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prowling_blizzard_orb", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(40800.0);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_HORSE_BREATHE, 1.3f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 1.2f, 1.2f);

            for (int i = 0; i < 16; i++) {
                oAxis[i] = i % 3; // 0,1,2,0,1,2,...
                oAng[i] = Math.PI * 2 * (i / 3.0) / 5.5;
                Location p = computeOrbPos(c, i, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 240, 255).interpolation(2, 0);
                orbs.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner core: 4 GLASS_BOTTLE swirling tight, 4 NETHER_STAR cardinal points
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 0.6, ORB_CENTER_Y + Math.sin(a) * 0.4, Math.sin(a) * 0.6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 230, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * ORB_R, ORB_CENTER_Y, Math.sin(a) * ORB_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHER_STAR));
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 220, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        private Location computeOrbPos(Location c, int i, int tick) {
            double a = oAng[i];
            double bx, by, bz;
            // Sphere parameterized as 3 great circles
            switch (oAxis[i]) {
                case 0: // X-axis orbit: rotate in YZ plane
                    bx = 0;
                    by = ORB_CENTER_Y + Math.cos(a) * ORB_R;
                    bz = Math.sin(a) * ORB_R;
                    break;
                case 1: // Y-axis orbit: rotate in XZ plane
                    bx = Math.cos(a) * ORB_R;
                    by = ORB_CENTER_Y;
                    bz = Math.sin(a) * ORB_R;
                    break;
                default: // Z-axis orbit: rotate in XY plane
                    bx = Math.cos(a) * ORB_R;
                    by = ORB_CENTER_Y + Math.sin(a) * ORB_R;
                    bz = 0;
                    break;
            }
            return c.clone().add(offX + bx, by, offZ + bz);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Random walk drift — change direction every 60 ticks
            if (tick % 60 == 0) driftAng += (Math.random() - 0.5) * Math.PI;
            offX += Math.cos(driftAng) * 0.04;
            offZ += Math.sin(driftAng) * 0.04;
            // Soft clamp — keep within 8 blocks of spawn
            double dd = Math.sqrt(offX * offX + offZ * offZ);
            if (dd > 8.0) {
                offX *= 8.0 / dd;
                offZ *= 8.0 / dd;
                driftAng = Math.atan2(-offZ, -offX); // turn back toward center
            }

            // Spin each orb
            for (int i = 0; i < orbs.size(); i++) {
                oAng[i] += 0.10;
                double a = oAng[i];
                double bx, by, bz;
                switch (oAxis[i]) {
                    case 0: bx = 0; by = ORB_CENTER_Y + Math.cos(a) * ORB_R; bz = Math.sin(a) * ORB_R; break;
                    case 1: bx = Math.cos(a) * ORB_R; by = ORB_CENTER_Y; bz = Math.sin(a) * ORB_R; break;
                    default: bx = Math.cos(a) * ORB_R; by = ORB_CENTER_Y + Math.sin(a) * ORB_R; bz = 0; break;
                }
                orbs.get(i).animateTo(
                        new Vector3f((float)(offX + bx) - 0.25f, (float)by, (float)(offZ + bz) - 0.25f),
                        new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                        new Vector3f(0.5f), 2);
            }

            // CLOUD swirl
            if (tick % 2 == 0) {
                for (int i = 0; i < 5; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * ORB_R;
                    Location p = c.clone().add(offX + Math.cos(a) * r, ORB_CENTER_Y + (Math.random() - 0.5) * 2, offZ + Math.sin(a) * r);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.1, 0.1, 0.1, 0.02);
                    if (Math.random() < 0.4) w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            if (tick % 35 == 0) DisplayBuilder.playSound(c.clone().add(offX, ORB_CENTER_Y, offZ), Sound.ENTITY_HORSE_BREATHE, 0.9f, 1.0f);

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                Location dmgC = c.clone().add(offX, 0, offZ);
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(dmgC) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ProwlingBlizzardOrb(plugin); }
    }

    // ================================================================
    // 45. ROAMING HOARFROST BEAST [FOLLOW-AI 0.11] — 12 ICE in quadruped
    //     form (1 head + 1 body + 4 legs + 2 horns + tail + jaw + 2 eyes)
    //     stalks player. Constant 5.5r, 3800hp, 10-tick interval.
    // ================================================================
    public static class RoamingHoarfrostBeast extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> parts = new ArrayList<>();
        // 12-part layout: 0 head, 1 body, 2-5 legs (FL, FR, BL, BR), 6-7 horns, 8 tail, 9 jaw, 10-11 eyes
        private final double[] partOX = new double[12];
        private final double[] partOY = new double[12];
        private final double[] partOZ = new double[12];
        private final float[] partSX = new float[12];
        private final float[] partSY = new float[12];
        private final float[] partSZ = new float[12];

        public RoamingHoarfrostBeast(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("roaming_hoarfrost_beast", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(45600.0);
            config.setDamageRadius(8.25);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(380);
            config.setCooldownTicks(60);
            // FOLLOW-AI: quadruped predator stalks player.
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.11);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_POLAR_BEAR_AMBIENT, 1.6f, 0.9f);
            DisplayBuilder.playSound(c, Sound.ENTITY_POLAR_BEAR_WARNING, 1.2f, 0.8f);

            // Quadruped layout — facing +X
            // Head
            partOX[0] = 1.6;  partOY[0] = 2.0; partOZ[0] = 0; partSX[0] = 1.0f; partSY[0] = 1.0f; partSZ[0] = 1.0f;
            // Body
            partOX[1] = 0;    partOY[1] = 1.8; partOZ[1] = 0; partSX[1] = 1.8f; partSY[1] = 1.2f; partSZ[1] = 1.1f;
            // Legs
            partOX[2] = 0.8;  partOY[2] = 0.7; partOZ[2] = 0.5;  partSX[2] = 0.4f; partSY[2] = 1.2f; partSZ[2] = 0.4f; // FL
            partOX[3] = 0.8;  partOY[3] = 0.7; partOZ[3] = -0.5; partSX[3] = 0.4f; partSY[3] = 1.2f; partSZ[3] = 0.4f; // FR
            partOX[4] = -0.8; partOY[4] = 0.7; partOZ[4] = 0.5;  partSX[4] = 0.4f; partSY[4] = 1.2f; partSZ[4] = 0.4f; // BL
            partOX[5] = -0.8; partOY[5] = 0.7; partOZ[5] = -0.5; partSX[5] = 0.4f; partSY[5] = 1.2f; partSZ[5] = 0.4f; // BR
            // Horns
            partOX[6] = 1.7;  partOY[6] = 2.7; partOZ[6] = 0.35; partSX[6] = 0.25f; partSY[6] = 0.7f; partSZ[6] = 0.25f;
            partOX[7] = 1.7;  partOY[7] = 2.7; partOZ[7] = -0.35; partSX[7] = 0.25f; partSY[7] = 0.7f; partSZ[7] = 0.25f;
            // Tail
            partOX[8] = -1.5; partOY[8] = 1.9; partOZ[8] = 0;   partSX[8] = 0.8f; partSY[8] = 0.3f; partSZ[8] = 0.3f;
            // Jaw
            partOX[9] = 2.0;  partOY[9] = 1.7; partOZ[9] = 0;   partSX[9] = 0.6f; partSY[9] = 0.3f; partSZ[9] = 0.8f;
            // Eyes
            partOX[10] = 1.85; partOY[10] = 2.2; partOZ[10] = 0.25; partSX[10] = 0.2f; partSY[10] = 0.2f; partSZ[10] = 0.2f;
            partOX[11] = 1.85; partOY[11] = 2.2; partOZ[11] = -0.25; partSX[11] = 0.2f; partSY[11] = 0.2f; partSZ[11] = 0.2f;

            for (int i = 0; i < 12; i++) {
                Location p = c.clone().add(partOX[i], partOY[i], partOZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(partSX[i], partSY[i], partSZ[i]).glow(190, 220, 255).interpolation(4, 0);
                parts.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fur/frost accents: 6 PACKED_ICE shoulder humps + 4 BLUE_ICE claws
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 0.5, 2.4, Math.sin(a) * 0.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(0.4f, 0.3f, 0.4f).glow(200, 220, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                int leg = i;
                double xx = (leg < 2 ? 0.8 : -0.8);
                double zz = (leg % 2 == 0 ? 0.55 : -0.55);
                Location p = c.clone().add(xx, 0.2, zz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.25f, 0.2f, 0.25f).glow(150, 200, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Leg gait — alternating diagonals lift/drop on 20-tick cycle
            double gait = Math.sin(tick * 0.25) * 0.25;
            double gaitAlt = Math.sin(tick * 0.25 + Math.PI) * 0.25;
            double[] legBob = { 0, 0, gait, gaitAlt, gaitAlt, gait, 0, 0, Math.sin(tick * 0.1) * 0.2, 0, 0, 0 };
            // Head sway + body wave
            double headSway = Math.sin(tick * 0.08) * 0.1;
            double bodyWave = Math.sin(tick * 0.1) * 0.05;

            for (int i = 0; i < parts.size(); i++) {
                double bobY = partOY[i] + legBob[i] + bodyWave;
                double swayZ = (i == 0 || i == 9 || i == 10 || i == 11) ? headSway : 0;
                parts.get(i).animateTo(
                        new Vector3f((float)partOX[i] - partSX[i] / 2, (float)bobY, (float)(partOZ[i] + swayZ) - partSZ[i] / 2),
                        new AxisAngle4f((float)(Math.sin(tick * 0.06 + i) * 0.06), 0, 1, 0),
                        new Vector3f(partSX[i], partSY[i], partSZ[i]), 4);
            }

            // SNOWFLAKE breath from mouth
            if (tick % 3 == 0) {
                Location mouth = c.clone().add(2.2, 1.9, 0);
                w.spawnParticle(Particle.SNOWFLAKE, mouth, 4, 0.3, 0.2, 0.3, 0.04);
                w.spawnParticle(Particle.CLOUD, mouth, 2, 0.2, 0.2, 0.2, 0.03);
            }
            // Frost trail at feet
            if (tick % 4 == 0) {
                for (int i = 2; i <= 5; i++) {
                    Location foot = c.clone().add(partOX[i], 0.3, partOZ[i]);
                    DisplayBuilder.dustParticles(foot, 1, 0.2, 200, 220, 240, 1.0f);
                }
            }
            if (tick % 50 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_POLAR_BEAR_AMBIENT, 0.9f, 0.9f);

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

        @Override public AbstractAttack newInstance() { return new RoamingHoarfrostBeast(plugin); }
    }

    // ================================================================
    // 46. SHIVERING CHAINPETALS — 20 GLOW_BERRIES chained linked sine-snaking
    //     across arena. Constant 8.0r, 2600hp, 14-tick interval.
    // ================================================================
    public static class ShiveringChainpetals extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> petals = new ArrayList<>();
        private double chainAng = 0;
        private static final int LINK_COUNT = 20;
        private static final double CHAIN_LEN = 12.0;

        public ShiveringChainpetals(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shivering_chainpetals", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(31200.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(340);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SWEET_BERRY_BUSH_PLACE, 1.2f, 1.2f);

            chainAng = Math.random() * Math.PI * 2;
            double cosA = Math.cos(chainAng);
            double sinA = Math.sin(chainAng);
            double step = CHAIN_LEN / (LINK_COUNT - 1);
            double startOff = -CHAIN_LEN / 2;
            for (int i = 0; i < LINK_COUNT; i++) {
                double t = startOff + step * i;
                Location p = c.clone().add(cosA * t, 1.2, sinA * t);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                h.scale(0.55f, 0.55f, 0.55f).glow(200, 240, 180).interpolation(4, 0);
                petals.add(h);
                spawnedEntities.add(h.entity());
            }

            // Chain anchor accents: 6 AMETHYST_SHARD link sparkles + 4 GLOW_BERRIES clusters at ends
            for (int i = 0; i < 6; i++) {
                double t = startOff + step * (i * 3.5);
                double bx = cosA * t;
                double bz = sinA * t;
                Location p = c.clone().add(bx, 1.6, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                h.scale(0.4f, 0.4f, 0.4f).glow(180, 240, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double sign = (i < 2 ? -1 : 1);
                double off = sign * (CHAIN_LEN / 2 + 0.6 + (i % 2) * 0.4);
                Location p = c.clone().add(cosA * off, 1.2, sinA * off);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                h.scale(0.7f, 0.7f, 0.7f).glow(220, 250, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double cosA = Math.cos(chainAng);
            double sinA = Math.sin(chainAng);
            double pCosA = -sinA;
            double pSinA = cosA;
            double step = CHAIN_LEN / (LINK_COUNT - 1);
            double startOff = -CHAIN_LEN / 2;

            // Snake sine wave traveling along chain
            for (int i = 0; i < LINK_COUNT; i++) {
                double t = startOff + step * i;
                double snake = Math.sin(i * 0.5 - tick * 0.15) * 1.5;
                double bobY = Math.cos(i * 0.4 + tick * 0.1) * 0.4;
                double bx = cosA * t + pCosA * snake;
                double bz = sinA * t + pSinA * snake;
                petals.get(i).animateTo(
                        new Vector3f((float)bx - 0.275f, (float)(1.2 + bobY), (float)bz - 0.275f),
                        new AxisAngle4f((float)(tick * 0.08 + i), 0, 1, 0),
                        new Vector3f(0.55f), 4);

                if (tick % 4 == 0 && i % 2 == 0) {
                    Location p = c.clone().add(bx, 1.2 + bobY, bz);
                    w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, p, 1, 0.1, 0.1, 0.1, 0.02);
                    w.spawnParticle(Particle.ENCHANT, p, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }

            // Periodic chime — chain link progression every 8 ticks
            if (tick % 8 == 0) {
                int idx = (tick / 8) % LINK_COUNT;
                double t = startOff + step * idx;
                double snake = Math.sin(idx * 0.5 - tick * 0.15) * 1.5;
                Location chimeLoc = c.clone().add(cosA * t + pCosA * snake, 1.2, sinA * t + pSinA * snake);
                float pitch = 0.6f + (idx / (float)LINK_COUNT) * 1.6f;
                DisplayBuilder.playSound(chimeLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, pitch);
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                // Hit any player within radius of any link (uses snake-wave positions).
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (int i = 0; i < LINK_COUNT; i++) {
                    double t = startOff + step * i;
                    double snake = Math.sin(i * 0.5 - tick * 0.15) * 1.5;
                    Location link = c.clone().add(cosA * t + pCosA * snake, 1.2, sinA * t + pSinA * snake);
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(link) <= r2) {
                            p.damage(config.getDamage());
                            p.setNoDamageTicks(0);
                            break;
                        }
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ShiveringChainpetals(plugin); }
    }

    // ================================================================
    // 47. FROSTFIRE ORBITAL — 6 NETHER_STAR vertical helix Y+1 to Y+9
    //     spiraling upward. Constant 7.5r, 3200hp, 11-tick interval.
    // ================================================================
    public static class FrostfireOrbital extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> stars = new ArrayList<>();
        private final double[] sAng = new double[6];
        private final double[] sY = new double[6];
        private static final double HELIX_R = 2.5;

        public FrostfireOrbital(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frostfire_orbital", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(38400.0);
            config.setDamageRadius(11.25);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 1.4f, 0.9f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.2f, 1.0f);

            for (int i = 0; i < 6; i++) {
                sAng[i] = Math.PI * 2 * i / 6;
                sY[i] = 1.0 + i * (8.0 / 5);
                Location p = c.clone().add(Math.cos(sAng[i]) * HELIX_R, sY[i], Math.sin(sAng[i]) * HELIX_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHER_STAR));
                h.scale(0.6f, 0.6f, 0.6f).glow(160, 220, 255).interpolation(4, 0);
                stars.add(h);
                spawnedEntities.add(h.entity());
            }

            // Helix supports: 8 ECHO_SHARD between stars + 6 BLUE_ICE rotating shell at base
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + 0.3;
                double yy = 1.0 + i * (8.0 / 7);
                Location p = c.clone().add(Math.cos(a) * (HELIX_R - 0.5), yy, Math.sin(a) * (HELIX_R - 0.5));
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.4f, 0.4f, 0.4f).glow(140, 200, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * (HELIX_R + 0.6), 0.5, Math.sin(a) * (HELIX_R + 0.6));
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.6f, 0.3f, 0.6f).glow(140, 200, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Spiral upward — each star rotates around its current Y, with a slow Y drift
            for (int i = 0; i < stars.size(); i++) {
                sAng[i] += 0.12;
                // Slight Y oscillation around base Y
                double yy = sY[i] + Math.sin(tick * 0.06 + i) * 0.4;
                float bx = (float)(Math.cos(sAng[i]) * HELIX_R);
                float bz = (float)(Math.sin(sAng[i]) * HELIX_R);
                stars.get(i).animateTo(
                        new Vector3f(bx - 0.3f, (float)yy, bz - 0.3f),
                        new AxisAngle4f((float)(tick * 0.18 + i), 0, 1, 0),
                        new Vector3f(0.6f), 4);
            }

            // SOUL_FIRE_FLAME cold flame + END_ROD shimmer along helix
            if (tick % 1 == 0) {
                for (int i = 0; i < stars.size(); i++) {
                    Location p = c.clone().add(Math.cos(sAng[i]) * HELIX_R, sY[i], Math.sin(sAng[i]) * HELIX_R);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.1, 0.1, 0.1, 0.02);
                    if (tick % 2 == 0) w.spawnParticle(Particle.END_ROD, p, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
            // Beacon glow at base
            if (tick % 3 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.4, 0), HELIX_R + 0.6, Particle.END_ROD, 12, null);
            }
            if (tick % 45 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.9f, 1.0f);

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

        @Override public AbstractAttack newInstance() { return new FrostfireOrbital(plugin); }
    }

    // ================================================================
    // 48. CRYOSTASIS DOME — 32 GLASS_BOTTLE hemisphere dome over player.
    //     Constant inside dome: 8.0r, 2800hp, 14-tick. On collapse:
    //     8.0r, 5000hp impact.
    // ================================================================
    public static class CryostasisDome extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shells = new ArrayList<>();
        private final double[] shAng = new double[32];
        private final double[] shTheta = new double[32]; // polar angle
        private boolean collapsed = false;
        private static final double DOME_R = 8.0;
        private static final int COLLAPSE_TICK = 240;

        public CryostasisDome(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryostasis_dome", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(33600.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDurationTicks(320);
            config.setCooldownTicks(60);
            config.setImpactDamage(60000.0);
            config.setImpactRadius(12.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.9f);

            // 32 GLASS_BOTTLE distributed across hemisphere using fibonacci-like spread
            for (int i = 0; i < 32; i++) {
                double phi = Math.acos(1.0 - (i / 31.0)); // 0..pi/2 polar
                double theta = i * 2.39996; // golden angle
                shAng[i] = theta;
                shTheta[i] = phi;
                double bx = DOME_R * Math.sin(phi) * Math.cos(theta);
                double by = DOME_R * Math.cos(phi);
                double bz = DOME_R * Math.sin(phi) * Math.sin(theta);
                Location p = c.clone().add(bx, 0.4 + by, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                h.scale(0.7f, 0.7f, 0.7f).glow(180, 220, 255).interpolation(8, 0);
                shells.add(h);
                spawnedEntities.add(h.entity());
            }

            // Dome interior: 8 ECHO_SHARD pillars + 6 PHANTOM_MEMBRANE drifting
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 1.6, 0.6 + (i % 4) * 0.8, Math.sin(a) * 1.6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.5f, 0.5f, 0.5f).glow(160, 200, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 3.5, 2.0 + Math.sin(a) * 0.5, Math.sin(a) * 3.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 210, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (!collapsed && tick < COLLAPSE_TICK) {
                // Dome shimmer — small angular wobble per shell
                for (int i = 0; i < shells.size(); i++) {
                    double phi = shTheta[i] + Math.sin(tick * 0.04 + i) * 0.02;
                    double theta = shAng[i] + Math.cos(tick * 0.03 + i) * 0.015;
                    double bx = DOME_R * Math.sin(phi) * Math.cos(theta);
                    double by = DOME_R * Math.cos(phi);
                    double bz = DOME_R * Math.sin(phi) * Math.sin(theta);
                    shells.get(i).animateTo(
                            new Vector3f((float)bx - 0.35f, (float)(0.4 + by), (float)bz - 0.35f),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.7f), 8);
                }

                // END_ROD shimmer along dome surface
                if (tick % 2 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double phi = Math.random() * Math.PI / 2;
                        double theta = Math.random() * Math.PI * 2;
                        double bx = DOME_R * Math.sin(phi) * Math.cos(theta);
                        double by = DOME_R * Math.cos(phi);
                        double bz = DOME_R * Math.sin(phi) * Math.sin(theta);
                        w.spawnParticle(Particle.END_ROD, c.clone().add(bx, 0.4 + by, bz), 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                // Periodic chime — rising pitch as collapse approaches
                if (tick % 20 == 0) {
                    float pitch = 1.0f + (tick / (float)COLLAPSE_TICK) * 0.8f;
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, pitch);
                }

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
            } else if (!collapsed) {
                // Collapse impact
                collapsed = true;
                Location impact = c.clone().add(0, 0.4, 0);
                DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.8f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.6f, 0.7f);
                DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.9f);

                // Glass barrage across dome surface
                for (int i = 0; i < shells.size(); i++) {
                    double phi = shTheta[i];
                    double theta = shAng[i];
                    double bx = DOME_R * Math.sin(phi) * Math.cos(theta);
                    double by = DOME_R * Math.cos(phi);
                    double bz = DOME_R * Math.sin(phi) * Math.sin(theta);
                    Location shellLoc = c.clone().add(bx, 0.4 + by, bz);
                    w.spawnParticle(Particle.BLOCK, shellLoc, 8, 0.3, 0.3, 0.3, 0.1, Material.GLASS.createBlockData());
                    w.spawnParticle(Particle.END_ROD, shellLoc, 4, 0.2, 0.2, 0.2, 0.05);
                    // Collapse shell inward
                    shells.get(i).animateTo(
                            new Vector3f(-0.35f, 0.4f, -0.35f),
                            new AxisAngle4f((float)(tick * 0.3), 1, 0.5f, 0),
                            new Vector3f(0.7f), 12);
                }
                w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.SONIC_BOOM, impact, 1, 0, 0, 0, 0);
                DisplayBuilder.dustParticles(impact, 36, 1.5, 200, 220, 255, 1.6f);

                triggerImpactDamage(impact);
            } else {
                // Post-collapse particle echo
                if (tick % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 6.0;
                        Location p = c.clone().add(Math.cos(a) * r, 0.4 + Math.random() * 0.5, Math.sin(a) * r);
                        w.spawnParticle(Particle.BLOCK, p, 1, 0.1, 0.1, 0.1, 0.02, Material.GLASS.createBlockData());
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CryostasisDome(plugin); }
    }

    // ================================================================
    // 49. SUBZERO VORTEX — 36 SNOWBALL tornado spiral Y+0 to Y+12, 0.1
    //     rad/tick around vertical axis. Constant 7.0r, 3000hp, 10-tick.
    // ================================================================
    public static class SubzeroVortex extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> flakes = new ArrayList<>();
        private final double[] fAng = new double[36];
        private final double[] fY = new double[36];
        private final double[] fR = new double[36];

        public SubzeroVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("subzero_vortex", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(36000.0);
            config.setDamageRadius(10.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(380);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_HORSE_BREATHE, 1.6f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 0.5f);

            for (int i = 0; i < 36; i++) {
                fAng[i] = Math.PI * 2 * (i / 6.0); // 6 per layer
                fY[i] = (i / 6) * 2.0; // 6 layers Y=0..10
                // Tornado profile: tight at base, wider at top
                fR[i] = 1.5 + (fY[i] / 12.0) * 3.0;
                Location p = c.clone().add(Math.cos(fAng[i]) * fR[i], fY[i], Math.sin(fAng[i]) * fR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 240, 255).interpolation(2, 0);
                flakes.add(h);
                spawnedEntities.add(h.entity());
            }

            // Outer wind veil: 10 FEATHER ascending spiral + 8 PHANTOM_MEMBRANE wraith trails
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * 2 * i / 5 + 0.2;
                double yy = 0.5 + i * 1.1;
                double rr = 2.0 + (yy / 12.0) * 3.0;
                Location p = c.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                h.scale(0.6f, 0.6f, 0.6f).glow(220, 230, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double yy = 2.0 + (i % 4) * 2.5;
                double rr = 2.5 + (yy / 12.0) * 2.5;
                Location p = c.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PHANTOM_MEMBRANE));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 230, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Vortex spin — 0.1 rad/tick
            for (int i = 0; i < flakes.size(); i++) {
                fAng[i] += 0.10;
                double localR = fR[i] + Math.sin(tick * 0.06 + i) * 0.25;
                double bobY = fY[i] + Math.sin(tick * 0.08 + i) * 0.3;
                float bx = (float)(Math.cos(fAng[i]) * localR);
                float bz = (float)(Math.sin(fAng[i]) * localR);
                flakes.get(i).animateTo(
                        new Vector3f(bx - 0.25f, (float)bobY, bz - 0.25f),
                        new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                        new Vector3f(0.5f), 2);
            }

            // CLOUD vortex column + SNOWFLAKE wisps
            if (tick % 1 == 0) {
                for (double y = 0.3; y < 12.5; y += 0.6) {
                    double a = (y * 1.4) + tick * 0.3;
                    double localR = 1.5 + (y / 12.0) * 3.0;
                    Location p = c.clone().add(Math.cos(a) * localR, y, Math.sin(a) * localR);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.05, 0.05, 0.05, 0.02);
                    if (Math.random() < 0.4) w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.03);
                }
            }
            // Outer dust ring at top
            if (tick % 4 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 11.0, 0), 4.5, Particle.SNOWFLAKE, 20, null);
            }
            if (tick % 30 == 0) DisplayBuilder.playSound(c.clone().add(0, 6, 0), Sound.ENTITY_HORSE_BREATHE, 0.9f, 0.6f);

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

        @Override public AbstractAttack newInstance() { return new SubzeroVortex(plugin); }
    }

    // ================================================================
    // 50. ABSOLUTE ZERO POINT — 1 NETHER_STAR core + 8 BLUE_ICE orbiting
    //     pull inward over 100t then IMPLODE. Pull: 8.0r, 1800hp, 12-tick.
    //     Implosion: 12.0r, 6000hp impact. SHOWCASE FINALE.
    // ================================================================
    public static class AbsoluteZeroPoint extends EnvironmentalAttack {
        private ItemDisplayHandle core;
        private final List<ItemDisplayHandle> orbs = new ArrayList<>();
        private final double[] oAng = new double[8];
        private double currentR = 6.0;
        private boolean imploded = false;
        private static final int PULL_DURATION = 100;
        private static final double START_R = 6.0;
        private static final double END_R = 0.4;

        public AbsoluteZeroPoint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("absolute_zero_point", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(21600.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(280);
            config.setCooldownTicks(60);
            config.setImpactDamage(72000.0);
            config.setImpactRadius(18.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.6f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.8f);

            // Central core
            Location corePos = c.clone().add(0, 3.0, 0);
            core = displayBuilder.spawnItem(corePos, new ItemStack(Material.NETHER_STAR));
            core.scale(1.0f, 1.0f, 1.0f).glow(180, 220, 255).interpolation(6, 0);
            spawnedEntities.add(core.entity());

            // 8 BLUE_ICE orbiters at START_R
            for (int i = 0; i < 8; i++) {
                oAng[i] = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(oAng[i]) * START_R, 3.0 + Math.sin(oAng[i]) * 0.6, Math.sin(oAng[i]) * START_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.8f, 0.8f, 0.8f).glow(140, 200, 255).interpolation(4, 0);
                orbs.add(h);
                spawnedEntities.add(h.entity());
            }

            // Anchor ring beneath: 8 ECHO_SHARD ground + 6 PACKED_ICE pillars
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + Math.PI / 8;
                Location p = c.clone().add(Math.cos(a) * 5.0, 0.4, Math.sin(a) * 5.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ECHO_SHARD));
                h.scale(0.5f, 0.5f, 0.5f).glow(140, 200, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 6.0, 1.0 + (i % 3) * 0.6, Math.sin(a) * 6.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(0.45f, 1.2f, 0.45f).glow(180, 220, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (!imploded && tick < PULL_DURATION) {
                // Pull-in phase: orbs spiral inward from START_R -> END_R
                double t = tick / (double)PULL_DURATION;
                currentR = START_R + (END_R - START_R) * t;
                double spinSpeed = 0.08 + t * 0.25; // accelerate as we close in

                // Core pulse — scale up slightly
                double coreScale = 1.0 + t * 0.6;
                core.animateTo(
                        new Vector3f(-(float)coreScale / 2, 3.0f - (float)coreScale / 2, -(float)coreScale / 2),
                        new AxisAngle4f((float)(tick * 0.2), 0, 1, 0),
                        new Vector3f((float)coreScale), 4);

                for (int i = 0; i < orbs.size(); i++) {
                    oAng[i] += spinSpeed;
                    double yy = 3.0 + Math.sin(oAng[i] + tick * 0.05) * (0.6 * (1 - t)); // bob flattens as we close in
                    float bx = (float)(Math.cos(oAng[i]) * currentR);
                    float bz = (float)(Math.sin(oAng[i]) * currentR);
                    orbs.get(i).animateTo(
                            new Vector3f(bx - 0.4f, (float)yy, bz - 0.4f),
                            new AxisAngle4f((float)(tick * 0.3 + i), 0, 1, 0),
                            new Vector3f(0.8f), 4);
                }

                // REVERSE_PORTAL pull from periphery toward core
                if (tick % 1 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = 1.0 + Math.random() * 6.0;
                        Location p = c.clone().add(Math.cos(a) * rr, 1.0 + Math.random() * 4.0, Math.sin(a) * rr);
                        w.spawnParticle(Particle.REVERSE_PORTAL, p, 1, 0.1, 0.1, 0.1, 0.04);
                    }
                }
                // SCULK_SOUL accent
                if (tick % 3 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = Math.random() * 4.0;
                        Location p = c.clone().add(Math.cos(a) * rr, 1.5 + Math.random() * 3.5, Math.sin(a) * rr);
                        w.spawnParticle(Particle.SCULK_SOUL, p, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
                // Rising pitch chime as we approach implosion
                if (tick % 10 == 0) {
                    float pitch = 0.6f + (float)(t * 1.6f);
                    DisplayBuilder.playSound(c.clone().add(0, 3, 0), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.1f, pitch);
                }

                // Pull-phase damage
                if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                    double r2 = config.getDamageRadius() * config.getDamageRadius();
                    Location dmgC = c.clone().add(0, 3.0, 0);
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(dmgC) <= r2) {
                            p.damage(config.getDamage());
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            } else if (!imploded) {
                // IMPLOSION
                imploded = true;
                Location impact = c.clone().add(0, 3.0, 0);
                DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.8f, 0.6f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.8f, 0.8f);
                DisplayBuilder.playSound(impact, Sound.ITEM_TRIDENT_THUNDER, 1.6f, 0.5f);

                // Burst particles — layered
                w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 3, 1.0, 1.0, 1.0, 0);
                w.spawnParticle(Particle.SONIC_BOOM, impact, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.FLASH, impact, 2, 0.5, 0.5, 0.5, 0);

                // Outward burst rings of SOUL_FIRE_FLAME + ELECTRIC_SPARK + END_ROD
                for (int r = 1; r <= 12; r++) {
                    final int rr = r;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        DisplayBuilder.particleRing(impact, rr, Particle.SOUL_FIRE_FLAME, 28, null);
                        DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), rr, Particle.ELECTRIC_SPARK, 24, null);
                        DisplayBuilder.particleRing(impact, rr * 0.8, Particle.END_ROD, 20, null);
                        DisplayBuilder.dustParticles(impact, 8, rr * 0.5, 200, 230, 255, 1.6f);
                    }, r * 2L);
                }

                // Collapse orbs into core
                core.animateTo(
                        new Vector3f(-1.5f, 1.5f, -1.5f),
                        new AxisAngle4f((float)(tick * 0.5), 0, 1, 0),
                        new Vector3f(3.0f), 8);
                for (int i = 0; i < orbs.size(); i++) {
                    orbs.get(i).animateTo(
                            new Vector3f(-0.4f, 3.0f, -0.4f),
                            new AxisAngle4f((float)(tick * 0.4 + i), 1, 1, 0),
                            new Vector3f(0.1f), 6);
                }

                triggerImpactDamage(impact);
            } else {
                // Post-implosion echo — fade
                int postT = tick - PULL_DURATION;
                if (postT % 4 == 0 && postT < 60) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double rr = Math.random() * 8.0;
                        Location p = c.clone().add(Math.cos(a) * rr, 0.5 + Math.random() * 4.0, Math.sin(a) * rr);
                        w.spawnParticle(Particle.END_ROD, p, 1, 0.1, 0.1, 0.1, 0.02);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                if (postT == 20) DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 0.6f);
            }
        }

        @Override public AbstractAttack newInstance() { return new AbsoluteZeroPoint(plugin); }
    }
}
