package com.blockforge.chaoscraft.modes.fluffy.attacks;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fluffy Mode — ENVIRONMENTAL FX BATCH 2 (entries 11-20).
 * Pure particle + ItemDisplay attacks. NO BlockDisplays.
 * Cute / animal / soft-horror flavored layered VFX.
 * Spawn at center.setYaw(0); center.setPitch(0).
 * Hearts pass directly (e.g. 8♥ -> setDamage(8.0)).
 * NO potion effects.
 */
public final class FluffyEnvironmental2 {
    private FluffyEnvironmental2() {}

    private static final String MODE_PATH = "modes/fluffy/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ButterflySwarm(plugin));
        registry.register(new CottonBombardment(plugin));
        registry.register(new PawPrintStamp(plugin));
        registry.register(new TreatToss(plugin));
        registry.register(new FeatherFall(plugin));
        registry.register(new SqueakyBomb(plugin));
        registry.register(new LaserPointerDot(plugin));
        registry.register(new CatnipHaze(plugin));
        registry.register(new StarfishField(plugin));
        registry.register(new CloudBurst(plugin));
    }

    // ================================================================
    // 11. BUTTERFLY SWARM — 20 FEATHER ItemDisplay pairs orbit a
    //     figure-8 (lemniscate) at varying heights. Constant damage in
    //     swarm radius. ENCHANT trails.
    // ================================================================
    public static class ButterflySwarm extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> butterflies = new ArrayList<>();
        private final double[] phaseOffset = new double[20];
        private final double[] heightBase = new double[20];

        public ButterflySwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("butterfly_swarm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(8.0); config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(15); config.setDamageDelayTicks(15);
            config.setDurationTicks(420); config.setCooldownTicks(360);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 0.9f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEEHIVE_DRIP, 0.7f, 1.4f);

            for (int i = 0; i < 20; i++) {
                phaseOffset[i] = Math.PI * 2 * i / 20.0;
                heightBase[i] = 1.5 + (i % 5) * 0.6;
                Location p = c.clone().add(0, heightBase[i], 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                int r = 220 + (i % 3) * 10;
                int g = 120 + (i * 7) % 100;
                int b = 220 - (i * 5) % 80;
                h.scale(0.35f, 0.35f, 0.35f).glow(r, g, b).interpolation(2, 0);
                butterflies.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 PINK_PETALS + 6 OXEYE_DAISY drifting between butterflies
            Material[] floral = { Material.PINK_PETALS, Material.PINK_PETALS, Material.PINK_PETALS, Material.PINK_PETALS,
                    Material.PINK_PETALS, Material.PINK_PETALS, Material.PINK_PETALS, Material.PINK_PETALS,
                    Material.OXEYE_DAISY, Material.OXEYE_DAISY, Material.OXEYE_DAISY,
                    Material.OXEYE_DAISY, Material.OXEYE_DAISY, Material.OXEYE_DAISY };
            for (int i = 0; i < floral.length; i++) {
                double a = Math.random() * Math.PI * 2;
                double rr = 1 + Math.random() * 4;
                double yy = 1.5 + Math.random() * 3.0;
                Location p = c.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(floral[i]));
                h.scale(0.4f, 0.4f, 0.4f).glow(255, 200, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Figure-8 lemniscate: x = a*cos(t)/(1+sin^2(t)), z = a*sin(t)cos(t)/(1+sin^2(t))
            double a = 4.0;
            for (int i = 0; i < butterflies.size(); i++) {
                double t = tick * 0.10 + phaseOffset[i];
                double denom = 1 + Math.sin(t) * Math.sin(t);
                double x = a * Math.cos(t) / denom;
                double z = a * Math.sin(t) * Math.cos(t) / denom;
                double y = heightBase[i] + Math.sin(tick * 0.15 + i) * 0.4;

                float fx = (float)x - 0.175f;
                float fy = (float)y;
                float fz = (float)z - 0.175f;

                if (tick % 2 == 0) {
                    butterflies.get(i).animateTo(
                            new Vector3f(fx, fy, fz),
                            new AxisAngle4f((float)(tick * 0.4 + i), 0, 1, 0),
                            new Vector3f(0.35f, 0.35f, 0.35f), 2);
                }

                // Enchant trails
                if (tick % 3 == 0) {
                    Location pos = c.clone().add(x, y, z);
                    w.spawnParticle(Particle.ENCHANT, pos, 1, 0.05, 0.05, 0.05, 0);
                    if (Math.random() < 0.25)
                        DisplayBuilder.dustParticles(pos, 1, 0.05, 240, 180, 250, 0.7f);
                }
            }

            // Sparse cherry leaf petals drifting through swarm
            if (tick % 6 == 0) for (int i = 0; i < 3; i++) {
                double ang = Math.random() * Math.PI * 2;
                double r = Math.random() * 4.5;
                w.spawnParticle(Particle.CHERRY_LEAVES,
                        c.clone().add(Math.cos(ang) * r, 1.5 + Math.random() * 3, Math.sin(ang) * r),
                        1, 0.1, 0.2, 0.1, 0.01);
            }

            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 0.5f, 1.7f);
        }

        @Override public AbstractAttack newInstance() { return new ButterflySwarm(plugin); }
    }

    // ================================================================
    // 12. COTTON BOMBARDMENT — 15 SNOWBALL ItemDisplays arc in from
    //     above randomly. Each impacts with snowflake explosion.
    // ================================================================
    public static class CottonBombardment extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> cottons = new ArrayList<>();
        private final double[] cottonY = new double[15];
        private final double[] cottonAng = new double[15];
        private final double[] cottonR = new double[15];
        private final boolean[] landed = new boolean[15];

        public CottonBombardment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cotton_bombardment", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(1.8);
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(0);
            config.setDurationTicks(280); config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.7f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SNOW_PLACE, 1.0f, 1.3f);

            for (int i = 0; i < 15; i++) {
                cottonAng[i] = Math.random() * Math.PI * 2;
                cottonR[i] = 1.5 + Math.random() * 6.0;
                cottonY[i] = 14 + Math.random() * 5 + i * 0.8;
                landed[i] = false;
                Location p = c.clone().add(Math.cos(cottonAng[i]) * cottonR[i], cottonY[i], Math.sin(cottonAng[i]) * cottonR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 255, 255).interpolation(2, 0);
                cottons.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 WHITE_WOOL + 6 STRING + 4 RABBIT_HIDE puffs floating mid-air as cotton bolls
            for (int i = 0; i < 8; i++) {
                double a = Math.random() * Math.PI * 2;
                double rr = Math.random() * 7;
                Location p = c.clone().add(Math.cos(a) * rr, 8 + Math.random() * 5, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WHITE_WOOL));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 255, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double rr = Math.random() * 7;
                Location p = c.clone().add(Math.cos(a) * rr, 5 + Math.random() * 6, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.4f, 0.4f, 0.4f).glow(245, 245, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 5, 9 + Math.random() * 3, Math.sin(a) * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                h.scale(0.55f, 0.55f, 0.55f).glow(245, 240, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < cottons.size(); i++) {
                if (landed[i]) continue;
                cottonY[i] -= 0.32 + (i % 4) * 0.04;
                Location pos = c.clone().add(Math.cos(cottonAng[i]) * cottonR[i], cottonY[i], Math.sin(cottonAng[i]) * cottonR[i]);
                cottons.get(i).animateTo(
                        new Vector3f((float)(Math.cos(cottonAng[i]) * cottonR[i]) - 0.3f,
                                (float)cottonY[i],
                                (float)(Math.sin(cottonAng[i]) * cottonR[i]) - 0.3f),
                        new AxisAngle4f((float)(tick * 0.25 + i), 0, 1, 0),
                        new Vector3f(0.6f), 2);

                // Drift trail
                w.spawnParticle(Particle.SNOWFLAKE, pos, 2, 0.1, 0.1, 0.1, 0.005);
                if (tick % 2 == 0) DisplayBuilder.dustParticles(pos, 1, 0.05, 255, 240, 250, 0.6f);

                if (cottonY[i] < 0.4) {
                    Location impact = c.clone().add(Math.cos(cottonAng[i]) * cottonR[i], 0.2, Math.sin(cottonAng[i]) * cottonR[i]);
                    landed[i] = true;
                    DisplayBuilder.playSound(impact, Sound.BLOCK_SNOW_BREAK, 1.0f, 1.6f);
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 30, 0.6, 0.4, 0.6, 0.15);
                    w.spawnParticle(Particle.CLOUD, impact, 10, 0.4, 0.2, 0.4, 0.05);
                    DisplayBuilder.dustParticles(impact, 12, 0.5, 255, 240, 255, 1.0f);
                    triggerImpactDamage(impact);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CottonBombardment(plugin); }
    }

    // ================================================================
    // 13. PAW PRINT STAMP — 6 sequential PINK_CONCRETE pawprint clusters
    //     (1 large pad + 4 small toes scaled ovals) appear then explode
    //     upward. Pink dust shimmer then CRIT.
    // ================================================================
    public static class PawPrintStamp extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> stamps = new ArrayList<>();
        private final List<Location> stampLocs = new ArrayList<>();
        private final boolean[] detonated = new boolean[6];
        private int nextStampTick = 0;
        private int stampIndex = 0;

        public PawPrintStamp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("paw_print_stamp", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(15.0);
            config.setImpactRadius(2.0);
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(0);
            config.setDurationTicks(320); config.setCooldownTicks(340);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 0.9f, 0.8f);

            // Ambient cat-toy clutter: 6 STRING + 4 RABBIT_FOOT + 4 NAME_TAG + 2 LEAD on the ground
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 6, 0.3, Math.sin(a) * 6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 230, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 5, 0.4, Math.sin(a) * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_FOOT));
                h.scale(0.55f, 0.55f, 0.55f).glow(220, 200, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 3.5, 0.5, Math.sin(a) * 3.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NAME_TAG));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 220, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 2; i++) {
                double a = Math.PI + i * Math.PI;
                Location p = c.clone().add(Math.cos(a) * 7, 0.3, Math.sin(a) * 7);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.LEAD));
                h.scale(0.7f, 0.7f, 0.7f).glow(180, 150, 120).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Spawn a new pawprint every ~30 ticks until we have 6
            if (stampIndex < 6 && tick >= nextStampTick) {
                double ang = Math.random() * Math.PI * 2;
                double r = 2 + Math.random() * 5;
                Location stampLoc = c.clone().add(Math.cos(ang) * r, 0.1, Math.sin(ang) * r);
                stampLocs.add(stampLoc);

                // Big pad
                ItemDisplayHandle pad = displayBuilder.spawnItem(stampLoc, new ItemStack(Material.PINK_CONCRETE));
                pad.scale(1.3f, 0.15f, 1.0f).glow(255, 150, 200).interpolation(6, 0);
                stamps.add(pad);
                spawnedEntities.add(pad.entity());

                // 4 toe ovals around the pad
                double[][] toes = {{-0.55, 0.85}, {0.55, 0.85}, {-0.85, 0.25}, {0.85, 0.25}};
                for (double[] off : toes) {
                    Location toeLoc = stampLoc.clone().add(off[0], 0, off[1]);
                    ItemDisplayHandle toe = displayBuilder.spawnItem(toeLoc, new ItemStack(Material.PINK_CONCRETE));
                    toe.scale(0.4f, 0.12f, 0.5f).glow(255, 170, 210).interpolation(6, 0);
                    stamps.add(toe);
                    spawnedEntities.add(toe.entity());
                }

                DisplayBuilder.playSound(stampLoc, Sound.ENTITY_CAT_STRAY_AMBIENT, 0.7f, 1.1f);
                w.spawnParticle(Particle.HEART, stampLoc.clone().add(0, 0.6, 0), 2, 0.3, 0.2, 0.3, 0);

                stampIndex++;
                nextStampTick = tick + 25;
            }

            // Detonate each stamp ~50 ticks after it appeared
            for (int i = 0; i < stampLocs.size(); i++) {
                if (detonated[i]) continue;
                int spawnedAt = (i * 25);
                if (tick >= spawnedAt + 60) {
                    detonated[i] = true;
                    Location impact = stampLocs.get(i);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.4f);
                    w.spawnParticle(Particle.CRIT, impact, 25, 0.6, 0.6, 0.6, 0.3);
                    w.spawnParticle(Particle.HEART, impact, 6, 0.5, 0.6, 0.5, 0);
                    DisplayBuilder.dustParticles(impact, 30, 1.0, 255, 130, 200, 1.4f);
                    triggerImpactDamage(impact);
                }
            }

            // Shimmer over each undetonated stamp
            if (tick % 3 == 0) for (int i = 0; i < stampLocs.size(); i++) {
                if (detonated[i]) continue;
                Location pos = stampLocs.get(i).clone().add((Math.random() - 0.5) * 1.6, 0.4 + Math.random() * 0.4, (Math.random() - 0.5) * 1.6);
                DisplayBuilder.dustParticles(pos, 1, 0.05, 255, 180, 220, 0.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new PawPrintStamp(plugin); }
    }

    // ================================================================
    // 14. TREAT TOSS — 8 COOKIE ItemDisplays arc in parabolic paths from
    //     arena edge. Impact each with CRIT + pink DUST landing.
    // ================================================================
    public static class TreatToss extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> cookies = new ArrayList<>();
        private final double[] startAng = new double[8];
        private final double[] startR = new double[8];
        private final double[] endAng = new double[8];
        private final double[] endR = new double[8];
        private final int[] startTick = new int[8];
        private final boolean[] landed = new boolean[8];

        public TreatToss(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("treat_toss", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(13.0);
            config.setImpactRadius(1.5);
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(0);
            config.setDurationTicks(260); config.setCooldownTicks(280);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_BURP, 0.6f, 1.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BAMBOO_HIT, 1.0f, 1.6f);

            for (int i = 0; i < 8; i++) {
                startAng[i] = Math.PI * 2 * i / 8.0 + Math.random() * 0.3;
                startR[i] = 8.0 + Math.random() * 1.5;
                endAng[i] = Math.random() * Math.PI * 2;
                endR[i] = Math.random() * 4.0;
                startTick[i] = i * 10;
                landed[i] = false;

                Location p = c.clone().add(Math.cos(startAng[i]) * startR[i], 0.5, Math.sin(startAng[i]) * startR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.COOKIE));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 160, 100).interpolation(2, 0);
                cookies.add(h);
                spawnedEntities.add(h.entity());
            }

            // Treat assortment scattered around: 6 SWEET_BERRIES + 4 SUGAR + 4 HONEY_BOTTLE + 2 CAKE
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 6, 0.3, Math.sin(a) * 6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SWEET_BERRIES));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 70, 90).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + 0.2;
                Location p = c.clone().add(Math.cos(a) * 4, 0.3, Math.sin(a) * 4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SUGAR));
                h.scale(0.45f, 0.45f, 0.45f).glow(255, 250, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 5, 0.5, Math.sin(a) * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.HONEY_BOTTLE));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 200, 60).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 2; i++) {
                double a = i * Math.PI;
                Location p = c.clone().add(Math.cos(a) * 2, 0.4, Math.sin(a) * 2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CAKE));
                h.scale(0.7f, 0.7f, 0.7f).glow(255, 220, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            int flightDur = 40;
            for (int i = 0; i < cookies.size(); i++) {
                if (landed[i]) continue;
                if (tick < startTick[i]) continue;
                int local = tick - startTick[i];
                if (local > flightDur) continue;
                double t = (double) local / flightDur;

                double sx = Math.cos(startAng[i]) * startR[i];
                double sz = Math.sin(startAng[i]) * startR[i];
                double ex = Math.cos(endAng[i]) * endR[i];
                double ez = Math.sin(endAng[i]) * endR[i];

                double x = sx + (ex - sx) * t;
                double z = sz + (ez - sz) * t;
                double y = 0.4 + Math.sin(t * Math.PI) * 5.5; // parabolic arc

                cookies.get(i).animateTo(
                        new Vector3f((float)x - 0.25f, (float)y, (float)z - 0.25f),
                        new AxisAngle4f((float)(tick * 0.6 + i), 0, 1, 0),
                        new Vector3f(0.5f), 2);

                Location pos = c.clone().add(x, y, z);
                if (tick % 2 == 0) DisplayBuilder.dustParticles(pos, 1, 0.05, 200, 130, 90, 0.7f);

                if (local >= flightDur) {
                    landed[i] = true;
                    Location impact = c.clone().add(ex, 0.3, ez);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EAT, 1.0f, 1.6f);
                    w.spawnParticle(Particle.CRIT, impact, 18, 0.5, 0.4, 0.5, 0.3);
                    DisplayBuilder.dustParticles(impact, 18, 0.6, 255, 150, 200, 1.2f);
                    triggerImpactDamage(impact);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new TreatToss(plugin); }
    }

    // ================================================================
    // 15. FEATHER FALL — 30 FEATHER ItemDisplays fall slowly from Y+15,
    //     each spinning on Y axis. Constant in landing zone radius 5.0.
    //     SNOWFLAKE trails per feather.
    // ================================================================
    public static class FeatherFall extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> feathers = new ArrayList<>();
        private final double[] featherY = new double[30];
        private final double[] featherX = new double[30];
        private final double[] featherZ = new double[30];
        private final double[] swayPhase = new double[30];

        public FeatherFall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("feather_fall", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(7.0); config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20); config.setDamageDelayTicks(20);
            config.setDurationTicks(440); config.setCooldownTicks(360);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_CHICKEN_EGG, 0.6f, 1.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SNOW_FALL, 1.0f, 1.4f);

            for (int i = 0; i < 30; i++) {
                double ang = Math.random() * Math.PI * 2;
                double r = Math.random() * 4.6;
                featherX[i] = Math.cos(ang) * r;
                featherZ[i] = Math.sin(ang) * r;
                featherY[i] = 15 + Math.random() * 3;
                swayPhase[i] = Math.random() * Math.PI * 2;

                Location p = c.clone().add(featherX[i], featherY[i], featherZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                h.scale(0.45f, 0.45f, 0.45f).glow(250, 250, 240).interpolation(2, 0);
                feathers.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < feathers.size(); i++) {
                featherY[i] -= 0.10 + (i % 5) * 0.01;
                if (featherY[i] < 0.2) featherY[i] = 0.2;
                double sway = Math.sin(tick * 0.06 + swayPhase[i]) * 0.3;
                double sx = featherX[i] + sway;
                double sz = featherZ[i] + Math.cos(tick * 0.06 + swayPhase[i]) * 0.3;

                feathers.get(i).animateTo(
                        new Vector3f((float)sx - 0.225f, (float)featherY[i], (float)sz - 0.225f),
                        new AxisAngle4f((float)(tick * 0.35 + i), 0, 1, 0),
                        new Vector3f(0.45f), 2);

                if (tick % 4 == 0) {
                    Location pos = c.clone().add(sx, featherY[i], sz);
                    w.spawnParticle(Particle.SNOWFLAKE, pos, 1, 0.05, 0.05, 0.05, 0);
                    if (Math.random() < 0.2) DisplayBuilder.dustParticles(pos, 1, 0.05, 250, 245, 230, 0.6f);
                }
            }

            // Soft landing-zone glow at center
            if (tick % 3 == 0) for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 5.0;
                Location pos = c.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
                w.spawnParticle(Particle.CLOUD, pos, 1, 0.1, 0.05, 0.1, 0.005);
            }

            if (tick % 90 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_PARROT_FLY, 0.5f, 1.6f);
        }

        @Override public AbstractAttack newInstance() { return new FeatherFall(plugin); }
    }

    // ================================================================
    // 16. SQUEAKY BOMB — 1 SLIME_BALL ItemDisplay invisible 2s then
    //     large white DUST sphere explosion. Impact radius 6.0, 28♥.
    // ================================================================
    public static class SqueakyBomb extends EnvironmentalAttack {
        private ItemDisplayHandle bomb;
        private boolean detonated = false;
        private static final int FUSE_TICKS = 40; // 2s

        public SqueakyBomb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("squeaky_bomb", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(28.0);
            config.setImpactRadius(6.0);
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(0);
            config.setDurationTicks(120); config.setCooldownTicks(360);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_SLIME_BLOCK_PLACE, 1.0f, 1.7f);
            DisplayBuilder.playSound(c, Sound.ENTITY_RABBIT_AMBIENT, 0.8f, 1.6f);

            bomb = displayBuilder.spawnItem(c.clone().add(0, 1.0, 0), new ItemStack(Material.SLIME_BALL));
            bomb.scale(0.0f, 0.0f, 0.0f).glow(255, 255, 255).interpolation(8, 0);
            // Animate from invisible to small over the fuse
            bomb.animateTo(new Vector3f(-0.4f, 1.0f, -0.4f),
                    new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.8f, 0.8f, 0.8f), FUSE_TICKS);
            spawnedEntities.add(bomb.entity());

            // Squeaky-toy clutter around the bomb: 6 STRING + 4 RABBIT_FOOT + 4 BONE + 4 NAME_TAG
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 4, 0.4, Math.sin(a) * 4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 240, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 5.5, 0.4, Math.sin(a) * 5.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_FOOT));
                h.scale(0.55f, 0.55f, 0.55f).glow(220, 200, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 3, 0.4, Math.sin(a) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BONE));
                h.scale(0.6f, 0.6f, 0.6f).glow(240, 230, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 8;
                Location p = c.clone().add(Math.cos(a) * 2.2, 0.5, Math.sin(a) * 2.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NAME_TAG));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 220, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (!detonated) {
                // Tick squeak warning
                if (tick % 5 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_SLIME_SQUISH_SMALL, 0.7f, 1.8f);
                if (tick % 2 == 0) DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 1, 0.2, 255, 255, 255, 0.6f);

                if (tick >= FUSE_TICKS) {
                    detonated = true;
                    Location impact = c.clone().add(0, 1.0, 0);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.4f, 1.6f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.8f);

                    // White DUST sphere explosion
                    int rings = 14;
                    for (int ring = 0; ring < rings; ring++) {
                        double theta = Math.PI * ring / rings;
                        double r = 6.0 * Math.sin(theta);
                        double yOff = 6.0 * Math.cos(theta);
                        int pts = Math.max(8, (int)(r * 6));
                        for (int p = 0; p < pts; p++) {
                            double phi = Math.PI * 2 * p / pts;
                            Location pos = impact.clone().add(Math.cos(phi) * r, yOff, Math.sin(phi) * r);
                            DisplayBuilder.dustParticles(pos, 1, 0.05, 255, 255, 255, 1.4f);
                        }
                    }
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.CLOUD, impact, 60, 2.5, 2.5, 2.5, 0.1);

                    // Pop the bomb visual
                    bomb.animateTo(new Vector3f(-3.0f, 1.0f, -3.0f),
                            new AxisAngle4f((float)(tick * 0.5), 0, 1, 0), new Vector3f(6.0f, 6.0f, 6.0f), 6);

                    triggerImpactDamage(impact);
                }
            } else {
                // Fade aftermath
                if (tick % 2 == 0) for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2, r = Math.random() * 6.0;
                    DisplayBuilder.dustParticles(c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 3, Math.sin(a) * r),
                            1, 0.1, 255, 250, 255, 0.9f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SqueakyBomb(plugin); }
    }

    // ================================================================
    // 17. LASER POINTER DOT — RED DUST dot moves rapidly across ground
    //     via animateTo. If a player stands on it for 3 ticks straight,
    //     CRIT + impact damage 10♥ at radius 1.0.
    // ================================================================
    public static class LaserPointerDot extends EnvironmentalAttack {
        private ItemDisplayHandle dot;
        private double dotX, dotZ;
        private double tgtX, tgtZ;
        private final Map<UUID, Integer> sitTicks = new HashMap<>();

        public LaserPointerDot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("laser_pointer_dot", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(1.0);
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setTicksBetweenDamage(3); config.setDamageDelayTicks(0);
            config.setDurationTicks(380); config.setCooldownTicks(320);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_STRAY_AMBIENT, 0.9f, 1.5f);
            dotX = 0; dotZ = 0; tgtX = 0; tgtZ = 0;

            // Tiny invisible item-display anchor at the dot position (RED dust does the visuals)
            dot = displayBuilder.spawnItem(c.clone().add(0, 0.1, 0), new ItemStack(Material.REDSTONE));
            dot.scale(0.15f, 0.05f, 0.15f).glow(255, 30, 30).interpolation(2, 0);
            spawnedEntities.add(dot.entity());

            // Cat-attention props scattered around: 6 STRING balls, 4 RABBIT_HIDE, 4 FEATHER, 2 NAME_TAG
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 5, 0.3, Math.sin(a) * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 200, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 6.5, 0.4, Math.sin(a) * 6.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                h.scale(0.55f, 0.55f, 0.55f).glow(220, 200, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 4, 0.5, Math.sin(a) * 4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 230, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 2; i++) {
                double a = i * Math.PI;
                Location p = c.clone().add(Math.cos(a) * 3, 0.3, Math.sin(a) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NAME_TAG));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 220, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Pick a new random target every ~8 ticks for rapid motion
            if (tick % 8 == 0) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 6.5;
                tgtX = Math.cos(a) * r;
                tgtZ = Math.sin(a) * r;
            }

            // Move dot rapidly toward target
            dotX += (tgtX - dotX) * 0.45;
            dotZ += (tgtZ - dotZ) * 0.45;

            dot.animateTo(
                    new Vector3f((float)dotX - 0.075f, 0.1f, (float)dotZ - 0.075f),
                    new AxisAngle4f((float)(tick * 0.4), 0, 1, 0),
                    new Vector3f(0.15f, 0.05f, 0.15f), 2);

            Location pos = c.clone().add(dotX, 0.1, dotZ);

            // Visual: RED DUST cluster on dot
            DisplayBuilder.dustParticles(pos, 4, 0.12, 255, 20, 20, 1.6f);
            DisplayBuilder.dustParticles(pos, 2, 0.05, 255, 80, 80, 1.0f);

            // Check players standing on dot
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) { sitTicks.remove(p.getUniqueId()); continue; }
                double dx = p.getLocation().getX() - pos.getX();
                double dz = p.getLocation().getZ() - pos.getZ();
                if (dx * dx + dz * dz <= 1.0 * 1.0) {
                    int prev = sitTicks.getOrDefault(p.getUniqueId(), 0);
                    int now = prev + 1;
                    sitTicks.put(p.getUniqueId(), now);
                    if (now >= 3) {
                        DisplayBuilder.playSound(pos, Sound.ENTITY_CAT_HISS, 1.2f, 1.4f);
                        w.spawnParticle(Particle.CRIT, p.getLocation().clone().add(0, 1.0, 0), 22, 0.4, 0.6, 0.4, 0.4);
                        DisplayBuilder.dustParticles(pos, 12, 0.4, 255, 30, 30, 1.4f);
                        triggerImpactDamage(pos);
                        sitTicks.put(p.getUniqueId(), 0);
                    }
                } else {
                    sitTicks.remove(p.getUniqueId());
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new LaserPointerDot(plugin); }
    }

    // ================================================================
    // 18. CATNIP HAZE — 9 TALL_GRASS items + GREEN DUST + SPORE_BLOSSOM_AIR
    //     fog. Constant wide radius 6.0, 6♥, 20-tick interval.
    // ================================================================
    public static class CatnipHaze extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> grasses = new ArrayList<>();
        private final double[] gx = new double[9];
        private final double[] gz = new double[9];

        public CatnipHaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("catnip_haze", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0); config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20); config.setDamageDelayTicks(20);
            config.setDurationTicks(500); config.setCooldownTicks(380);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRASS_PLACE, 1.0f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AZALEA_LEAVES_BREAK, 0.8f, 1.2f);

            for (int i = 0; i < 9; i++) {
                double a = Math.PI * 2 * i / 9.0;
                double r = 1.5 + (i % 3) * 1.3;
                gx[i] = Math.cos(a) * r;
                gz[i] = Math.sin(a) * r;
                Location p = c.clone().add(gx[i], 0.5, gz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.TALL_GRASS));
                h.scale(0.9f, 1.4f, 0.9f).glow(140, 220, 100).interpolation(8, 0);
                grasses.add(h);
                spawnedEntities.add(h.entity());
            }

            // Garden of catnip props: 6 LARGE_FERN + 5 GLOW_BERRIES + 4 SPORE_BLOSSOM + 3 BAMBOO
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 5.5, 0.5, Math.sin(a) * 5.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.LARGE_FERN));
                h.scale(0.9f, 1.5f, 0.9f).glow(120, 200, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 5; i++) {
                double a = Math.PI * 2 * i / 5;
                Location p = c.clone().add(Math.cos(a) * 4, 1.0, Math.sin(a) * 4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 200, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 3, 2.0, Math.sin(a) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SPORE_BLOSSOM));
                h.scale(0.7f, 0.7f, 0.7f).glow(220, 130, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 3; i++) {
                double a = Math.PI * 2 * i / 3;
                Location p = c.clone().add(Math.cos(a) * 2, 0.5, Math.sin(a) * 2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BAMBOO));
                h.scale(0.6f, 1.4f, 0.6f).glow(140, 220, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Sway grasses
            if (tick % 4 == 0) {
                for (int i = 0; i < grasses.size(); i++) {
                    float sway = (float)(Math.sin(tick * 0.08 + i) * Math.toRadians(15));
                    grasses.get(i).animateTo(
                            new Vector3f((float)gx[i] - 0.45f, 0.5f, (float)gz[i] - 0.45f),
                            new AxisAngle4f(sway, 0, 0, 1),
                            new Vector3f(0.9f, 1.4f, 0.9f), 4);
                }
            }

            // Spore blossom air fog over wide radius
            if (tick % 2 == 0) for (int i = 0; i < 8; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 6.0;
                Location pos = c.clone().add(Math.cos(a) * r, 0.4 + Math.random() * 2.5, Math.sin(a) * r);
                w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, pos, 1, 0.2, 0.3, 0.2, 0);
            }

            // Green dust haze
            if (tick % 3 == 0) for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 6.0;
                Location pos = c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 2, Math.sin(a) * r);
                DisplayBuilder.dustParticles(pos, 1, 0.1, 100, 220, 80, 1.1f);
            }

            // Composter sparkle near grasses
            if (tick % 6 == 0) for (int i = 0; i < grasses.size(); i++) {
                Location p = c.clone().add(gx[i] + (Math.random() - 0.5) * 0.4, 1.2 + Math.random() * 0.4, gz[i] + (Math.random() - 0.5) * 0.4);
                w.spawnParticle(Particle.COMPOSTER, p, 1, 0.1, 0.1, 0.1, 0);
            }

            if (tick % 90 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 0.8f, 1.5f);
        }

        @Override public AbstractAttack newInstance() { return new CatnipHaze(plugin); }
    }

    // ================================================================
    // 19. STARFISH FIELD — 6 star clusters of 5 GOLD_INGOT ItemDisplays
    //     at star-point positions rising from ground. Constant radius
    //     1.2 per star, 9♥, 12-tick interval. ENCHANT sparkle.
    // ================================================================
    public static class StarfishField extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> stars = new ArrayList<>();
        private final double[] starX = new double[6];
        private final double[] starZ = new double[6];

        public StarfishField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("starfish_field", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(9.0); config.setDamageRadius(1.2);
            config.setTicksBetweenDamage(12); config.setDamageDelayTicks(15);
            config.setDurationTicks(360); config.setCooldownTicks(340);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.6f);

            // Place 6 stars around arena
            for (int s = 0; s < 6; s++) {
                double a = Math.PI * 2 * s / 6.0 + Math.random() * 0.2;
                double r = 1.5 + Math.random() * 5;
                starX[s] = Math.cos(a) * r;
                starZ[s] = Math.sin(a) * r;
                Location starCenter = c.clone().add(starX[s], 0.1, starZ[s]);

                // 5 gold ingots at 5-pointed star vertices
                for (int p = 0; p < 5; p++) {
                    double pa = Math.PI * 2 * p / 5.0 - Math.PI / 2.0;
                    double pr = 0.8;
                    double dx = Math.cos(pa) * pr;
                    double dz = Math.sin(pa) * pr;
                    Location pt = starCenter.clone().add(dx, 0, dz);
                    ItemDisplayHandle h = displayBuilder.spawnItem(pt, new ItemStack(Material.GOLD_INGOT));
                    h.scale(0.45f, 0.1f, 0.45f).glow(255, 220, 80).interpolation(20, 0);
                    // Animate up from ground
                    h.animateTo(
                            new Vector3f((float)dx - 0.225f, 0.6f, (float)dz - 0.225f),
                            new AxisAngle4f((float)pa, 0, 1, 0),
                            new Vector3f(0.55f, 0.2f, 0.55f), 20);
                    stars.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slow rotation of each star
            if (tick % 4 == 0) {
                for (int s = 0; s < 6; s++) {
                    for (int p = 0; p < 5; p++) {
                        int idx = s * 5 + p;
                        if (idx >= stars.size()) continue;
                        double pa = Math.PI * 2 * p / 5.0 - Math.PI / 2.0 + tick * 0.04;
                        double pr = 0.8 + Math.sin(tick * 0.1 + s) * 0.1;
                        double dx = starX[s] + Math.cos(pa) * pr;
                        double dz = starZ[s] + Math.sin(pa) * pr;
                        stars.get(idx).animateTo(
                                new Vector3f((float)dx - 0.225f, 0.6f + (float)Math.sin(tick * 0.08 + s) * 0.2f, (float)dz - 0.225f),
                                new AxisAngle4f((float)pa, 0, 1, 0),
                                new Vector3f(0.55f, 0.2f, 0.55f), 4);
                    }
                }
            }

            // Enchant sparkle around each star
            if (tick % 3 == 0) for (int s = 0; s < 6; s++) {
                Location sp = c.clone().add(starX[s] + (Math.random() - 0.5) * 1.6, 0.7 + Math.random() * 0.6, starZ[s] + (Math.random() - 0.5) * 1.6);
                w.spawnParticle(Particle.ENCHANT, sp, 1, 0.05, 0.05, 0.05, 0);
                if (Math.random() < 0.2) DisplayBuilder.dustParticles(sp, 1, 0.05, 255, 230, 100, 0.8f);
            }

            // Damage: 9♥ within radius 1.2 of any star (every 12 ticks)
            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = 1.2 * 1.2;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    for (int s = 0; s < 6; s++) {
                        double dx = pl.getX() - (c.getX() + starX[s]);
                        double dz = pl.getZ() - (c.getZ() + starZ[s]);
                        if (dx * dx + dz * dz <= r2) {
                            p.damage(9.0);
                            p.setNoDamageTicks(0);
                            break;
                        }
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new StarfishField(plugin); }
    }

    // ================================================================
    // 20. CLOUD BURST — 9 WHITE_STAINED_GLASS items at Y+8 form a cloud.
    //     DRIZZLE rain beneath. Constant radius 4.0/8♥. Lightning impact
    //     drops with radius 6.0/24♥.
    // ================================================================
    public static class CloudBurst extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> cloudPieces = new ArrayList<>();
        private final double[] cx = new double[9];
        private final double[] cz = new double[9];
        private int nextLightningTick = 50;

        public CloudBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cloud_burst", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            // Constant cloud rain damage
            config.setDamage(8.0); config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(20); config.setDamageDelayTicks(20);
            // Impact lightning damage
            config.setImpactDamage(24.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(440); config.setCooldownTicks(380);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.0f, 1.0f);
            DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.2f);

            for (int i = 0; i < 9; i++) {
                double a = Math.PI * 2 * i / 9.0;
                double r = 1.5 + (i % 3) * 0.6;
                cx[i] = Math.cos(a) * r;
                cz[i] = Math.sin(a) * r;
                Location p = c.clone().add(cx[i], 8.0, cz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WHITE_STAINED_GLASS));
                h.scale(1.6f, 0.6f, 1.6f).glow(255, 255, 255).interpolation(10, 0);
                cloudPieces.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 SNOWBALL + 6 WHITE_WOOL + 4 PRISMARINE_SHARD raindrops + 4 NAUTILUS_SHELL bubbles around the cloud
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 3, 7.5 + Math.random() * 0.6, Math.sin(a) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.45f, 0.45f, 0.45f).glow(245, 245, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.2;
                Location p = c.clone().add(Math.cos(a) * 2.5, 8.5, Math.sin(a) * 2.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WHITE_WOOL));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 255, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 4, 5 + Math.random() * 2, Math.sin(a) * 4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PRISMARINE_SHARD));
                h.scale(0.4f, 0.4f, 0.4f).glow(180, 220, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 3.5, 3.5, Math.sin(a) * 3.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NAUTILUS_SHELL));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 230, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Subtle cloud drift / pulse
            if (tick % 5 == 0) for (int i = 0; i < cloudPieces.size(); i++) {
                float yOff = 8.0f + (float)Math.sin(tick * 0.05 + i) * 0.3f;
                float scaleY = 0.6f + (float)Math.sin(tick * 0.08 + i) * 0.1f;
                cloudPieces.get(i).animateTo(
                        new Vector3f((float)cx[i] - 0.8f, yOff, (float)cz[i] - 0.8f),
                        new AxisAngle4f((float)(tick * 0.02 + i), 0, 1, 0),
                        new Vector3f(1.6f, scaleY, 1.6f), 5);
            }

            // DRIZZLE rain beneath cloud
            if (tick % 1 == 0) for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 4.0;
                Location pos = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 7.0, Math.sin(a) * r);
                w.spawnParticle(Particle.RAIN, pos, 1, 0.1, 0.2, 0.1, 0);
                if (Math.random() < 0.4) w.spawnParticle(Particle.FALLING_WATER, pos, 1, 0.05, 0.1, 0.05, 0);
            }

            // Cloud surface dust
            if (tick % 2 == 0) for (int i = 0; i < 5; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 3.5;
                Location pos = c.clone().add(Math.cos(a) * r, 7.8 + Math.random() * 0.6, Math.sin(a) * r);
                DisplayBuilder.dustParticles(pos, 1, 0.1, 240, 240, 250, 1.0f);
            }

            // Periodic lightning impact drops within cloud area
            if (tick >= nextLightningTick) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 4.0;
                Location strike = c.clone().add(Math.cos(a) * r, 0.4, Math.sin(a) * r);

                // Visual lightning bolt particle column
                for (int y = 0; y < 16; y++) {
                    Location lp = strike.clone().add((Math.random() - 0.5) * 0.4, y * 0.5, (Math.random() - 0.5) * 0.4);
                    w.spawnParticle(Particle.END_ROD, lp, 1, 0.05, 0.05, 0.05, 0);
                    DisplayBuilder.dustParticles(lp, 1, 0.05, 200, 230, 255, 1.4f);
                }
                w.spawnParticle(Particle.FLASH, strike.clone().add(0, 1.0, 0), 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.EXPLOSION, strike, 1, 0, 0, 0, 0);
                DisplayBuilder.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.4f, 1.1f);
                DisplayBuilder.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 1.2f);

                triggerImpactDamage(strike);
                nextLightningTick = tick + 70 + (int)(Math.random() * 50);
            }

            if (tick % 100 == 0) DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 1.0f, 1.0f);
        }

        @Override public AbstractAttack newInstance() { return new CloudBurst(plugin); }
    }
}
