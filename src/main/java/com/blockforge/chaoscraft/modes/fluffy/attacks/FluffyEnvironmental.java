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
import java.util.List;

/**
 * Fluffy Mode — ENVIRONMENTAL FX BATCH 1 (entries 1-10).
 * Pure ItemDisplay + particle attacks. NO BlockDisplays.
 * Cute-but-deadly theme: cherry blossoms, kittens, milk splashes,
 * wool bursts, honey, purrs, fur tornadoes, glitter, yarn strands,
 * and floral explosions.
 */
public final class FluffyEnvironmental {
    private FluffyEnvironmental() {}

    private static final String MODE_PATH = "modes/fluffy/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PetalStorm(plugin));
        registry.register(new KittenRain(plugin));
        registry.register(new MilkSplash(plugin));
        registry.register(new SheepWoolBurst(plugin));
        registry.register(new HoneyDripField(plugin));
        registry.register(new PurrFrequency(plugin));
        registry.register(new FurTornado(plugin));
        registry.register(new GlitterBurst(plugin));
        registry.register(new WoolStrand(plugin));
        registry.register(new FloralExplosion(plugin));
    }

    // ================================================================
    // 1. PETAL STORM — 24 CHERRY_LEAVES item displays in a double helix
    //    spiral from ground to Y+8, contracting inward to constant
    //    radius 4.0. Cherry petal + snowflake particle system.
    // ================================================================
    public static class PetalStorm extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> petals = new ArrayList<>();
        private final double[] petalAng = new double[24];
        private final double[] petalY = new double[24];
        private final int[] petalStrand = new int[24];

        public PetalStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("petal_storm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(360);
            config.setCooldownTicks(320);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHERRY_LEAVES_PLACE, 1.2f, 0.8f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.5f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AZALEA_PLACE, 1.0f, 1.2f);

            for (int i = 0; i < 24; i++) {
                int strand = i % 2;
                petalStrand[i] = strand;
                petalAng[i] = (Math.PI * 2 * i / 12) + (strand * Math.PI);
                petalY[i] = (i / 2) * (8.0 / 12);
                double r = 5.5; // start wide
                Location p = c.clone().add(Math.cos(petalAng[i]) * r, petalY[i], Math.sin(petalAng[i]) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CHERRY_LEAVES));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 180, 220).interpolation(8, 0);
                petals.add(h);
                spawnedEntities.add(h.entity());
            }

            // Variety: 6 PINK_PETALS + 4 PALE_OAK_LEAVES + 4 AZALEA_LEAVES sprinkled around the helix
            Material[] variety = { Material.PINK_PETALS, Material.PINK_PETALS, Material.PINK_PETALS,
                    Material.PALE_OAK_LEAVES, Material.PALE_OAK_LEAVES, Material.AZALEA_LEAVES,
                    Material.AZALEA_LEAVES, Material.PINK_PETALS, Material.PINK_PETALS,
                    Material.PINK_PETALS, Material.PALE_OAK_LEAVES, Material.PALE_OAK_LEAVES,
                    Material.AZALEA_LEAVES, Material.AZALEA_LEAVES };
            for (int v = 0; v < variety.length; v++) {
                double a = Math.random() * Math.PI * 2;
                double rr = 1.5 + Math.random() * 4.0;
                double yy = 0.5 + Math.random() * 7.5;
                Location vp = c.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                ItemDisplayHandle vh = displayBuilder.spawnItem(vp, new ItemStack(variety[v]));
                vh.scale(0.5f, 0.5f, 0.5f).glow(255, 200, 230).interpolation(40, 0);
                spawnedEntities.add(vh.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Petal spiral — contracts from r=5.5 to r=4.0 over first 60 ticks
            double targetR = 4.0;
            double startR = 5.5;
            double t = Math.min(1.0, tick / 60.0);
            double curR = startR + (targetR - startR) * t;

            if (tick % 2 == 0) {
                for (int i = 0; i < petals.size(); i++) {
                    petalAng[i] += 0.06 + (petalStrand[i] == 0 ? 0.0 : 0.02);
                    double bobY = petalY[i] + Math.sin(tick * 0.08 + i) * 0.15;
                    float fx = (float)(Math.cos(petalAng[i]) * curR);
                    float fz = (float)(Math.sin(petalAng[i]) * curR);
                    petals.get(i).animateTo(
                            new Vector3f(fx - 0.275f, (float)bobY, fz - 0.275f),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.55f), 2);
                }
            }

            // Cherry petal particles — falling from above
            if (tick % 1 == 0) for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * curR;
                w.spawnParticle(Particle.CHERRY_LEAVES,
                        c.clone().add(Math.cos(a) * r, 5 + Math.random() * 4, Math.sin(a) * r),
                        1, 0.2, 0.3, 0.2, 0.02);
            }
            // Snowflake wisps along helix
            if (tick % 2 == 0) for (int i = 0; i < petals.size(); i += 3) {
                Location p = petals.get(i).entity().getLocation();
                w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.01);
            }
            // Pink dust ring at base
            if (tick % 4 == 0) for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12 + tick * 0.04;
                Location p = c.clone().add(Math.cos(a) * curR, 0.3, Math.sin(a) * curR);
                DisplayBuilder.dustParticles(p, 1, 0.05, 255, 180, 220, 1.1f);
            }
            // Periodic chime
            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_CHERRY_LEAVES_HIT, 0.8f, 1.4f);

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

        @Override public AbstractAttack newInstance() { return new PetalStorm(plugin); }
    }

    // ================================================================
    // 2. KITTEN RAIN — 10 PUMPKIN_PIE item displays (proxy kittens)
    //    fall from Y+20 staggered every 10 ticks. Each impact triggers
    //    its own AOE radius 2.0, 12 hearts.
    // ================================================================
    public static class KittenRain extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> kittens = new ArrayList<>();
        private final double[] kAng = new double[10];
        private final double[] kR = new double[10];
        private final double[] kY = new double[10];
        private final int[] kStartTick = new int[10];
        private final boolean[] kImpacted = new boolean[10];

        public KittenRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("kitten_rain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(260);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(2.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_AMBIENT, 1.0f, 1.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURREOW, 1.2f, 1.6f);

            for (int i = 0; i < 10; i++) {
                kAng[i] = Math.random() * Math.PI * 2;
                kR[i] = 1 + Math.random() * 5.5;
                kY[i] = 20 + Math.random() * 3;
                kStartTick[i] = i * 10; // staggered
                kImpacted[i] = false;
                Location p = c.clone().add(Math.cos(kAng[i]) * kR[i], kY[i], Math.sin(kAng[i]) * kR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PUMPKIN_PIE));
                h.scale(0.7f, 0.7f, 0.7f).glow(255, 200, 160).interpolation(2, 0);
                kittens.add(h);
                spawnedEntities.add(h.entity());
            }

            // Kitten essence: 8 STRING + 6 RABBIT_HIDE + 4 EGG floating in the rain zone
            Material[] kEssence = { Material.STRING, Material.STRING, Material.STRING, Material.STRING,
                    Material.STRING, Material.STRING, Material.STRING, Material.STRING,
                    Material.RABBIT_HIDE, Material.RABBIT_HIDE, Material.RABBIT_HIDE,
                    Material.RABBIT_HIDE, Material.RABBIT_HIDE, Material.RABBIT_HIDE,
                    Material.EGG, Material.EGG, Material.EGG, Material.EGG };
            for (Material mat : kEssence) {
                double a = Math.random() * Math.PI * 2;
                double rr = Math.random() * 6.5;
                double yy = 5 + Math.random() * 14;
                Location vp = c.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                ItemDisplayHandle vh = displayBuilder.spawnItem(vp, new ItemStack(mat));
                vh.scale(0.45f, 0.45f, 0.45f).glow(255, 220, 200).interpolation(60, 0);
                spawnedEntities.add(vh.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < kittens.size(); i++) {
                if (tick < kStartTick[i] || kImpacted[i]) continue;
                kY[i] -= 0.45;
                Location pos = c.clone().add(Math.cos(kAng[i]) * kR[i], kY[i], Math.sin(kAng[i]) * kR[i]);
                kittens.get(i).animateTo(
                        new Vector3f((float)(Math.cos(kAng[i]) * kR[i]) - 0.35f, (float)kY[i], (float)(Math.sin(kAng[i]) * kR[i]) - 0.35f),
                        new AxisAngle4f((float)(tick * 0.4 + i), 1, 0.5f, 0), new Vector3f(0.7f), 2);

                // Trail
                w.spawnParticle(Particle.CLOUD, pos, 2, 0.1, 0.1, 0.1, 0.01);
                DisplayBuilder.dustParticles(pos, 1, 0.1, 255, 200, 200, 0.9f);
                if (Math.random() < 0.3) w.spawnParticle(Particle.HEART, pos, 1, 0.1, 0.1, 0.1, 0);

                if (kY[i] < 0.6) {
                    Location impact = c.clone().add(Math.cos(kAng[i]) * kR[i], 0.4, Math.sin(kAng[i]) * kR[i]);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_SMALL_FALL, 1.3f, 1.6f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_CAT_HISS, 1.0f, 1.4f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_WOOL_BREAK, 1.4f, 1.0f);

                    // Impact particles — white DUST + landing burst
                    w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                    DisplayBuilder.dustParticles(impact, 24, 0.6, 255, 255, 255, 1.4f);
                    w.spawnParticle(Particle.SPLASH, impact, 16, 0.6, 0.2, 0.6, 0.1);
                    w.spawnParticle(Particle.HEART, impact, 6, 0.6, 0.4, 0.6, 0);

                    triggerImpactDamage(impact);
                    kImpacted[i] = true;
                }
            }

            if (tick % 20 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_CAT_BEG_FOR_FOOD, 0.7f, 1.4f);
        }

        @Override public AbstractAttack newInstance() { return new KittenRain(plugin); }
    }

    // ================================================================
    // 3. MILK SPLASH — 1 large WHITE_CONCRETE ItemDisplay (scale 1.8)
    //    rises then slams down. Single huge impact, radius 5.0, 22 hearts.
    // ================================================================
    public static class MilkSplash extends EnvironmentalAttack {
        private ItemDisplayHandle drop;
        private double dropY = 0;
        private boolean rising = true;
        private boolean impacted = false;

        public MilkSplash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("milk_splash", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(22.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(160);
            config.setCooldownTicks(360);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.0);
            config.setImpactRadius(5.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_COW_MILK, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1.2f, 0.6f);

            dropY = 0.5;
            drop = displayBuilder.spawnItem(c.clone().add(0, dropY, 0), new ItemStack(Material.WHITE_CONCRETE));
            drop.scale(1.8f, 1.8f, 1.8f).glow(255, 255, 255).interpolation(8, 0);
            spawnedEntities.add(drop.entity());

            // Milk droplets: 8 SNOWBALL + 4 WHITE_DYE bottles arranged in a ring around the splash zone
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                double rr = 4.0 + Math.random() * 1.5;
                Location p = c.clone().add(Math.cos(a) * rr, 0.5 + Math.random() * 0.6, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 255, 255).interpolation(20, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                double rr = 2.5;
                Location p = c.clone().add(Math.cos(a) * rr, 0.6, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WHITE_DYE));
                h.scale(0.55f, 0.55f, 0.55f).glow(245, 245, 255).interpolation(20, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            if (drop == null) return;

            if (rising) {
                // Rise 0.5 -> 12 over 30 ticks
                dropY = 0.5 + Math.min(11.5, tick * 0.4);
                if (tick >= 30) { rising = false; }
                drop.animateTo(
                        new Vector3f(-0.9f, (float)dropY, -0.9f),
                        new AxisAngle4f((float)(tick * 0.1), 0, 1, 0),
                        new Vector3f(1.8f), 4);
                w.spawnParticle(Particle.CLOUD, c.clone().add(0, dropY, 0), 2, 0.4, 0.2, 0.4, 0.02);
                DisplayBuilder.dustParticles(c.clone().add(0, dropY, 0), 4, 0.4, 255, 255, 255, 1.2f);
            } else if (!impacted) {
                // Slam down rapid
                dropY -= 0.85;
                if (dropY <= 0.5) {
                    dropY = 0.5;
                    impacted = true;
                    Location impact = c.clone().add(0, 0.5, 0);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_SPLASH, 1.6f, 0.8f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_PLAYER_SPLASH, 1.5f, 0.7f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.9f);

                    // Splash particles — white DUST ring + splash
                    for (int i = 0; i < 36; i++) {
                        double a = Math.PI * 2 * i / 36;
                        Location p = impact.clone().add(Math.cos(a) * 5.0, 0.3, Math.sin(a) * 5.0);
                        DisplayBuilder.dustParticles(p, 2, 0.2, 255, 255, 255, 1.6f);
                        w.spawnParticle(Particle.SPLASH, p, 4, 0.2, 0.4, 0.2, 0.2);
                        w.spawnParticle(Particle.CLOUD, p, 1, 0.3, 0.3, 0.3, 0.05);
                    }
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);

                    triggerImpactDamage(impact);
                }
                drop.animateTo(
                        new Vector3f(-0.9f, (float)dropY, -0.9f),
                        new AxisAngle4f((float)(tick * 0.2), 0, 1, 0),
                        new Vector3f(1.8f), 1);
            } else {
                // Settled — flatten slowly
                drop.animateTo(
                        new Vector3f(-1.5f, 0.0f, -1.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(3.0f, 0.15f, 3.0f), 20);
                if (tick % 6 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * Math.PI * 2, r = Math.random() * 5;
                        DisplayBuilder.dustParticles(c.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r),
                                1, 0.1, 255, 255, 255, 1.0f);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new MilkSplash(plugin); }
    }

    // ================================================================
    // 4. SHEEP WOOL BURST — 16 WHITE_WOOL ItemDisplays orbit center
    //    (radius 3.0) for a wind-up phase, then fly outward.
    //    Impact on each landing, radius 1.5, 14 hearts each.
    // ================================================================
    public static class SheepWoolBurst extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> wools = new ArrayList<>();
        private final double[] wAng = new double[16];
        private final double[] wR = new double[16];
        private final double[] wY = new double[16];
        private final boolean[] wLaunched = new boolean[16];
        private final boolean[] wImpacted = new boolean[16];
        private final double[] wDx = new double[16];
        private final double[] wDz = new double[16];
        private static final int LAUNCH_TICK = 50;

        public SheepWoolBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sheep_wool_burst", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(220);
            config.setCooldownTicks(320);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(1.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_SHEEP_AMBIENT, 1.4f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_WOOL_PLACE, 1.2f, 0.8f);

            for (int i = 0; i < 16; i++) {
                wAng[i] = Math.PI * 2 * i / 16;
                wR[i] = 3.0;
                wY[i] = 1.5;
                Location p = c.clone().add(Math.cos(wAng[i]) * wR[i], wY[i], Math.sin(wAng[i]) * wR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WHITE_WOOL));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 240, 230).interpolation(4, 0);
                wools.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 STRING (yarn) + 4 BONE_MEAL (fluff) + 2 SHEARS resting in the ring
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + 0.2;
                Location p = c.clone().add(Math.cos(a) * 2.0, 0.6, Math.sin(a) * 2.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.65f, 0.65f, 0.65f).glow(255, 240, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 1.2, 0.4, Math.sin(a) * 1.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BONE_MEAL));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 250, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 2; i++) {
                double a = Math.PI + i * Math.PI;
                Location p = c.clone().add(Math.cos(a) * 4.5, 0.5, Math.sin(a) * 4.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SHEARS));
                h.scale(0.6f, 0.6f, 0.6f).glow(220, 220, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < LAUNCH_TICK) {
                // Orbit phase — accelerate spin
                double speed = 0.04 + (tick / (double)LAUNCH_TICK) * 0.12;
                for (int i = 0; i < wools.size(); i++) {
                    wAng[i] += speed;
                    float bx = (float)(Math.cos(wAng[i]) * wR[i]);
                    float bz = (float)(Math.sin(wAng[i]) * wR[i]);
                    float by = (float)(wY[i] + Math.sin(tick * 0.1 + i) * 0.2);
                    wools.get(i).animateTo(
                            new Vector3f(bx - 0.275f, by, bz - 0.275f),
                            new AxisAngle4f((float)(tick * 0.1 + i), 0, 1, 0),
                            new Vector3f(0.55f), 2);
                }
                if (tick % 4 == 0) {
                    for (int i = 0; i < wools.size(); i++) {
                        Location p = c.clone().add(Math.cos(wAng[i]) * wR[i], wY[i], Math.sin(wAng[i]) * wR[i]);
                        w.spawnParticle(Particle.SNOWFLAKE, p, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            } else if (tick == LAUNCH_TICK) {
                DisplayBuilder.playSound(c, Sound.ENTITY_SHEEP_SHEAR, 1.6f, 1.4f);
                DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.6f);
                // Lock in launch directions
                for (int i = 0; i < wools.size(); i++) {
                    wDx[i] = Math.cos(wAng[i]);
                    wDz[i] = Math.sin(wAng[i]);
                }
            } else {
                // Launched — fly outward
                double t = (tick - LAUNCH_TICK);
                for (int i = 0; i < wools.size(); i++) {
                    if (wImpacted[i]) continue;
                    wLaunched[i] = true;
                    double r = 3.0 + t * 0.45;
                    double yArc = wY[i] + Math.sin(t * 0.08) * 0.6 - t * 0.04;
                    if (yArc < 0.4 || r > 12) {
                        // Land
                        Location impact = c.clone().add(wDx[i] * r, 0.4, wDz[i] * r);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_WOOL_FALL, 1.2f, 1.2f);
                        w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                        DisplayBuilder.dustParticles(impact, 12, 0.4, 255, 240, 230, 1.3f);
                        w.spawnParticle(Particle.SNOWFLAKE, impact, 8, 0.4, 0.3, 0.4, 0.05);
                        triggerImpactDamage(impact);
                        wImpacted[i] = true;
                    } else {
                        Location pos = c.clone().add(wDx[i] * r, yArc, wDz[i] * r);
                        wools.get(i).animateTo(
                                new Vector3f((float)(wDx[i] * r) - 0.275f, (float)yArc, (float)(wDz[i] * r) - 0.275f),
                                new AxisAngle4f((float)(t * 0.3 + i), 1, 0.5f, 0),
                                new Vector3f(0.55f), 2);
                        w.spawnParticle(Particle.CLOUD, pos, 1, 0.1, 0.1, 0.1, 0.01);
                        DisplayBuilder.dustParticles(pos, 1, 0.1, 255, 245, 235, 1.0f);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SheepWoolBurst(plugin); }
    }

    // ================================================================
    // 5. HONEY DRIP FIELD — 9 HONEY_BOTTLE ItemDisplays in 3x3 grid at
    //    Y+3, dripping honey columns beneath each, then descend and
    //    explode. Impact each, radius 1.8, 16 hearts.
    // ================================================================
    public static class HoneyDripField extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> bottles = new ArrayList<>();
        private final double[] bX = new double[9];
        private final double[] bZ = new double[9];
        private final double[] bY = new double[9];
        private final boolean[] bImpacted = new boolean[9];
        private static final int DROP_TICK = 60;

        public HoneyDripField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("honey_drip_field", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(16.0);
            config.setDamageRadius(1.8);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(240);
            config.setCooldownTicks(340);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(1.8);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_HONEY_BLOCK_PLACE, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_BEE_LOOP, 1.2f, 1.0f);

            int idx = 0;
            for (int gx = -1; gx <= 1; gx++) {
                for (int gz = -1; gz <= 1; gz++) {
                    bX[idx] = gx * 1.8;
                    bZ[idx] = gz * 1.8;
                    bY[idx] = 3.0;
                    Location p = c.clone().add(bX[idx], bY[idx], bZ[idx]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.HONEY_BOTTLE));
                    h.scale(0.65f, 0.65f, 0.65f).glow(255, 200, 60).interpolation(6, 0);
                    bottles.add(h);
                    spawnedEntities.add(h.entity());
                    idx++;
                }
            }

            // 6 HONEYCOMB + 4 CAKE on the floor + 4 SUGAR cubes around the field
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                double rr = 3.5;
                Location p = c.clone().add(Math.cos(a) * rr, 0.4, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.HONEYCOMB));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 180, 50).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 4.0, 0.4, Math.sin(a) * 4.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CAKE));
                h.scale(0.7f, 0.7f, 0.7f).glow(255, 220, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 1.0, 0.3, Math.sin(a) * 1.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SUGAR));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 250, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < DROP_TICK) {
                // Hovering, dripping honey columns
                for (int i = 0; i < bottles.size(); i++) {
                    float by = (float)(bY[i] + Math.sin(tick * 0.1 + i) * 0.15);
                    bottles.get(i).animateTo(
                            new Vector3f((float)bX[i] - 0.325f, by, (float)bZ[i] - 0.325f),
                            new AxisAngle4f((float)(Math.sin(tick * 0.05 + i) * 0.25), 0, 0, 1),
                            new Vector3f(0.65f), 4);
                }
                if (tick % 2 == 0) {
                    for (int i = 0; i < bottles.size(); i++) {
                        for (double y = bY[i] - 0.5; y > 0.4; y -= 0.5) {
                            if (Math.random() < 0.3) {
                                w.spawnParticle(Particle.DRIPPING_HONEY,
                                        c.clone().add(bX[i], y, bZ[i]), 1, 0.05, 0.05, 0.05, 0);
                            }
                        }
                    }
                }
                if (tick == DROP_TICK - 10) DisplayBuilder.playSound(c, Sound.BLOCK_HONEY_BLOCK_HIT, 1.4f, 0.7f);
            } else {
                // Drop and explode
                for (int i = 0; i < bottles.size(); i++) {
                    if (bImpacted[i]) continue;
                    bY[i] -= 0.35;
                    if (bY[i] <= 0.5) {
                        Location impact = c.clone().add(bX[i], 0.4, bZ[i]);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_HONEY_BLOCK_BREAK, 1.3f, 0.9f);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_SLIME_BLOCK_BREAK, 1.0f, 0.7f);
                        w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.FALLING_HONEY, impact, 24, 0.6, 0.4, 0.6, 0.05);
                        DisplayBuilder.dustParticles(impact, 16, 0.5, 255, 200, 60, 1.4f);
                        w.spawnParticle(Particle.LANDING_HONEY, impact, 12, 0.5, 0.2, 0.5, 0.1);
                        triggerImpactDamage(impact);
                        bImpacted[i] = true;
                    } else {
                        Location pos = c.clone().add(bX[i], bY[i], bZ[i]);
                        bottles.get(i).animateTo(
                                new Vector3f((float)bX[i] - 0.325f, (float)bY[i], (float)bZ[i] - 0.325f),
                                new AxisAngle4f((float)(tick * 0.15), 0, 1, 0),
                                new Vector3f(0.65f), 2);
                        w.spawnParticle(Particle.DRIPPING_HONEY, pos, 1, 0.1, 0.1, 0.1, 0);
                        w.spawnParticle(Particle.FALLING_HONEY, pos, 1, 0.05, 0.05, 0.05, 0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new HoneyDripField(plugin); }
    }

    // ================================================================
    // 6. PURR FREQUENCY — 8 NAUTILUS_SHELL ItemDisplays slowly rotating
    //    around center. ENCHANT rings fire outward in sequence every
    //    15 ticks. Constant center damage radius 3.0, 9 hearts, 12-tick.
    // ================================================================
    public static class PurrFrequency extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shells = new ArrayList<>();
        private final double[] sAng = new double[8];
        private final double[] sY = new double[8];

        public PurrFrequency(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("purr_frequency", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(380);
            config.setCooldownTicks(340);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 1.5f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CONDUIT_AMBIENT, 1.2f, 1.0f);

            for (int i = 0; i < 8; i++) {
                sAng[i] = Math.PI * 2 * i / 8;
                sY[i] = 2.5;
                Location p = c.clone().add(Math.cos(sAng[i]) * 2.5, sY[i], Math.sin(sAng[i]) * 2.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NAUTILUS_SHELL));
                h.scale(0.6f, 0.6f, 0.6f).glow(180, 220, 255).interpolation(8, 0);
                shells.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 STRING in a rising spiral + 4 RABBIT_FOOT orbiting + 4 NAME_TAG floating
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 3;
                double yy = 0.5 + i * 0.6;
                Location p = c.clone().add(Math.cos(a) * 1.2, yy, Math.sin(a) * 1.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.6f, 0.6f, 0.6f).glow(200, 230, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 1.5, 1.5, Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_FOOT));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 200, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 3.0, 3.5, Math.sin(a) * 3.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NAME_TAG));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 230, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slow shell orbit
            if (tick % 2 == 0) {
                for (int i = 0; i < shells.size(); i++) {
                    sAng[i] += 0.04;
                    float by = (float)(sY[i] + Math.sin(tick * 0.06 + i) * 0.25);
                    float bx = (float)(Math.cos(sAng[i]) * 2.5);
                    float bz = (float)(Math.sin(sAng[i]) * 2.5);
                    shells.get(i).animateTo(
                            new Vector3f(bx - 0.3f, by, bz - 0.3f),
                            new AxisAngle4f((float)(tick * 0.08 + i), 0, 1, 0),
                            new Vector3f(0.6f), 2);
                }
            }

            // ENCHANT rings fire outward in sequence every 15 ticks
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURREOW, 1.0f, 0.9f);
                int ringIdx = (tick / 15) % 8;
                Location anchor = c.clone().add(Math.cos(sAng[ringIdx]) * 2.5, sY[ringIdx], Math.sin(sAng[ringIdx]) * 2.5);
                for (int r = 1; r <= 6; r++) {
                    final int rr = r;
                    final Location a = anchor;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        DisplayBuilder.particleRing(a, rr * 0.8, Particle.ENCHANT, 24, null);
                        DisplayBuilder.dustParticles(a, 1, 0.05, 180, 220, 255, 1.0f);
                    }, r * 2L);
                }
            }

            // Continuous purr ripple at center
            if (tick % 4 == 0) {
                double rippleR = 1.0 + ((tick / 4) % 8) * 0.4;
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), rippleR, Particle.NOTE, 18, null);
            }
            // Sparkle aura
            if (tick % 2 == 0) for (int i = 0; i < 3; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 3.0;
                w.spawnParticle(Particle.ENCHANT, c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 3.5, Math.sin(a) * r),
                        1, 0.1, 0.1, 0.1, 0.05);
            }
            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 1.0f, 0.7f);

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

        @Override public AbstractAttack newInstance() { return new PurrFrequency(plugin); }
    }

    // ================================================================
    // 7. FUR TORNADO — 12 FEATHER ItemDisplays spinning vortex column
    //    from ground to Y+6, tracking the closest player at 0.06 b/tick.
    //    Constant radius 2.0, 10 hearts, 8-tick.
    // ================================================================
    public static class FurTornado extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> feathers = new ArrayList<>();
        private final double[] fAng = new double[12];
        private final double[] fY = new double[12];
        private final List<ItemDisplayHandle> strands = new ArrayList<>();
        private final double[] sAng = new double[10];
        private final double[] sY = new double[10];
        private final double[] sR = new double[10];
        private final List<ItemDisplayHandle> furs = new ArrayList<>();
        private final double[] furAng = new double[6];
        private final double[] furY = new double[6];
        private final double[] furR = new double[6];
        private double offX = 0, offZ = 0;

        public FurTornado(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fur_tornado", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
            // Tornado intentionally tracks the player — chase mechanic.
            config.setTracksPlayer(true);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 1.5f, 1.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 1.2f, 1.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_HORSE_BREATHE, 1.0f, 1.4f);

            for (int i = 0; i < 12; i++) {
                fAng[i] = Math.PI * 2 * i / 6 + (i / 6) * 0.5;
                fY[i] = 0.5 + (i * 0.5);
                double r = 1.5 + (Math.sin(fY[i] * 0.3) * 0.4);
                Location p = c.clone().add(Math.cos(fAng[i]) * r, fY[i], Math.sin(fAng[i]) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                h.scale(0.6f, 0.6f, 0.6f).glow(220, 220, 230).interpolation(2, 0);
                feathers.add(h);
                spawnedEntities.add(h.entity());
            }

            // 10 STRING strands spinning at varying radii throughout the column
            for (int i = 0; i < 10; i++) {
                sAng[i] = Math.PI * 2 * i / 10 + Math.random() * 0.4;
                sY[i] = 0.4 + i * 0.6;
                sR[i] = 1.0 + (i % 3) * 0.6; // alternating radii
                Location p = c.clone().add(Math.cos(sAng[i]) * sR[i], sY[i], Math.sin(sAng[i]) * sR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 200, 210).interpolation(2, 0);
                strands.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 RABBIT_HIDE puffs as fur clumps spinning at the outer edge
            for (int i = 0; i < 6; i++) {
                furAng[i] = Math.PI * 2 * i / 6;
                furY[i] = 0.8 + i * 0.9;
                furR[i] = 1.8;
                Location p = c.clone().add(Math.cos(furAng[i]) * furR[i], furY[i], Math.sin(furAng[i]) * furR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                h.scale(0.65f, 0.65f, 0.65f).glow(230, 220, 200).interpolation(2, 0);
                furs.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Setpiece tornado: stays at spawn (no per-tick player tracking).

            // Spin feathers in vortex
            for (int i = 0; i < feathers.size(); i++) {
                fAng[i] += 0.18;
                double localR = 1.5 + Math.sin(fY[i] * 0.4 + tick * 0.05) * 0.5;
                float bx = (float)(offX + Math.cos(fAng[i]) * localR);
                float bz = (float)(offZ + Math.sin(fAng[i]) * localR);
                float by = (float)fY[i];
                feathers.get(i).animateTo(
                        new Vector3f(bx - 0.3f, by, bz - 0.3f),
                        new AxisAngle4f((float)(tick * 0.25 + i), 0, 1, 0),
                        new Vector3f(0.6f), 2);
            }

            // Spin STRING strands in vortex (slightly different speed)
            for (int i = 0; i < strands.size(); i++) {
                sAng[i] += 0.22;
                double localR = sR[i] + Math.sin(sY[i] * 0.5 + tick * 0.06) * 0.4;
                float bx = (float)(offX + Math.cos(sAng[i]) * localR);
                float bz = (float)(offZ + Math.sin(sAng[i]) * localR);
                strands.get(i).animateTo(
                        new Vector3f(bx - 0.25f, (float)sY[i], bz - 0.25f),
                        new AxisAngle4f((float)(tick * 0.4 + i), 0, 1, 0),
                        new Vector3f(0.5f), 2);
            }

            // Spin RABBIT_HIDE fur tufts at the outer edge
            for (int i = 0; i < furs.size(); i++) {
                furAng[i] += 0.14;
                double localR = furR[i] + Math.sin(furY[i] * 0.3 + tick * 0.04) * 0.5;
                float bx = (float)(offX + Math.cos(furAng[i]) * localR);
                float bz = (float)(offZ + Math.sin(furAng[i]) * localR);
                float by = (float)(furY[i] + Math.sin(tick * 0.08 + i) * 0.3);
                furs.get(i).animateTo(
                        new Vector3f(bx - 0.325f, by, bz - 0.325f),
                        new AxisAngle4f((float)(tick * 0.18 + i), 0, 1, 0),
                        new Vector3f(0.65f), 2);
            }

            // Gray/white DUST spiral particles
            if (tick % 1 == 0) {
                for (double y = 0.3; y < 6.5; y += 0.4) {
                    double a = (y * 1.6) + tick * 0.3;
                    double localR = 1.5 + Math.sin(y * 0.4 + tick * 0.05) * 0.5;
                    Location p = c.clone().add(offX + Math.cos(a) * localR, y, offZ + Math.sin(a) * localR);
                    int gray = 200 + (int)(Math.random() * 55);
                    DisplayBuilder.dustParticles(p, 1, 0.05, gray, gray, gray, 1.0f);
                    if (Math.random() < 0.15) w.spawnParticle(Particle.CLOUD, p, 1, 0.05, 0.05, 0.05, 0.02);
                }
            }
            // Feather drift particles
            if (tick % 3 == 0) for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 2.0;
                w.spawnParticle(Particle.WHITE_ASH,
                        c.clone().add(offX + Math.cos(a) * r, Math.random() * 6, offZ + Math.sin(a) * r),
                        1, 0.1, 0.1, 0.1, 0.02);
            }

            if (tick % 40 == 0) DisplayBuilder.playSound(c.clone().add(offX, 3, offZ), Sound.ITEM_ELYTRA_FLYING, 0.8f, 1.6f);

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                Location dmgCenter = c.clone().add(offX, 0, offZ);
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(dmgCenter) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FurTornado(plugin); }
    }

    // ================================================================
    // 8. GLITTER BURST — 6 GOLD_NUGGET ItemDisplays fly outward from
    //    center as shrapnel. Impact each, radius 1.5, 12 hearts each.
    //    CRIT + ENCHANT + FIREWORK_SPARK burst.
    // ================================================================
    public static class GlitterBurst extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> nuggets = new ArrayList<>();
        private final double[] gAng = new double[6];
        private final double[] gPitch = new double[6];
        private final double[] gR = new double[6];
        private final boolean[] gImpacted = new boolean[6];

        public GlitterBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glitter_burst", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(180);
            config.setCooldownTicks(280);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(1.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.4f, 1.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.6f, 1.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.6f);

            // Initial burst flash
            DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 30, 0.6, 255, 215, 0, 1.6f);
            c.getWorld().spawnParticle(Particle.FLASH, c.clone().add(0, 1.5, 0), 1, 0, 0, 0, 0);

            for (int i = 0; i < 6; i++) {
                gAng[i] = Math.PI * 2 * i / 6 + (Math.random() - 0.5) * 0.4;
                gPitch[i] = 0.25 + Math.random() * 0.4;
                gR[i] = 0;
                Location p = c.clone().add(0, 1.5, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GOLD_NUGGET));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 215, 0).interpolation(2, 0);
                nuggets.add(h);
                spawnedEntities.add(h.entity());
            }

            // Glitter scatter: 12 AMETHYST_SHARD + 8 GOLD_INGOT around the burst point
            for (int i = 0; i < 12; i++) {
                double a = Math.random() * Math.PI * 2;
                double rr = 0.6 + Math.random() * 1.2;
                double yy = 1.0 + Math.random() * 1.0;
                Location p = c.clone().add(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                h.scale(0.6f, 0.6f, 0.6f).glow(220, 130, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 1.6, 1.5 + Math.sin(a) * 0.4, Math.sin(a) * 1.6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GOLD_INGOT));
                h.scale(0.65f, 0.65f, 0.65f).glow(255, 215, 0).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < nuggets.size(); i++) {
                if (gImpacted[i]) continue;
                gR[i] += 0.55;
                double y = 1.5 + gPitch[i] * gR[i] - 0.05 * gR[i] * gR[i];
                if (y < 0.4 || gR[i] > 14) {
                    Location impact = c.clone().add(Math.cos(gAng[i]) * gR[i], 0.4, Math.sin(gAng[i]) * gR[i]);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.4f, 1.6f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.3f, 1.6f);
                    w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.CRIT, impact, 24, 0.5, 0.4, 0.5, 0.4);
                    w.spawnParticle(Particle.ENCHANT, impact, 24, 0.5, 0.4, 0.5, 0.4);
                    w.spawnParticle(Particle.FIREWORK, impact, 16, 0.4, 0.3, 0.4, 0.2);
                    DisplayBuilder.dustParticles(impact, 18, 0.5, 255, 215, 0, 1.4f);
                    triggerImpactDamage(impact);
                    gImpacted[i] = true;
                } else {
                    Location pos = c.clone().add(Math.cos(gAng[i]) * gR[i], y, Math.sin(gAng[i]) * gR[i]);
                    nuggets.get(i).animateTo(
                            new Vector3f((float)(Math.cos(gAng[i]) * gR[i]) - 0.25f, (float)y, (float)(Math.sin(gAng[i]) * gR[i]) - 0.25f),
                            new AxisAngle4f((float)(tick * 0.4 + i), 1, 1, 0),
                            new Vector3f(0.5f), 2);
                    w.spawnParticle(Particle.CRIT, pos, 2, 0.1, 0.1, 0.1, 0.05);
                    w.spawnParticle(Particle.ENCHANT, pos, 1, 0.1, 0.1, 0.1, 0.1);
                    w.spawnParticle(Particle.FIREWORK, pos, 1, 0.05, 0.05, 0.05, 0.02);
                    DisplayBuilder.dustParticles(pos, 2, 0.1, 255, 215, 0, 1.0f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new GlitterBurst(plugin); }
    }

    // ================================================================
    // 9. WOOL STRAND — 12 thin WHITE_WOOL ItemDisplays in a line,
    //    oscillating like a plucked string. Constant radius 0.8, 14
    //    hearts, 6-tick. ENCHANT from each bead.
    // ================================================================
    public static class WoolStrand extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> beads = new ArrayList<>();
        private double strandAng;
        private static final int BEAD_COUNT = 12;
        private static final double STRAND_LEN = 8.0;

        public WoolStrand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wool_strand", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(14.0);
            config.setDamageRadius(0.8);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(280);
            config.setCooldownTicks(340);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_HARP, 1.5f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_TRIPWIRE_ATTACH, 1.2f, 1.3f);

            strandAng = Math.random() * Math.PI * 2;
            double cosA = Math.cos(strandAng);
            double sinA = Math.sin(strandAng);
            double step = STRAND_LEN / (BEAD_COUNT - 1);
            double startOff = -STRAND_LEN / 2;
            for (int i = 0; i < BEAD_COUNT; i++) {
                double t = startOff + step * i;
                double bx = cosA * t;
                double bz = sinA * t;
                Location p = c.clone().add(bx, 1.2, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WHITE_WOOL));
                h.scale(0.12f, 0.12f, 0.9f).glow(255, 240, 230).interpolation(4, 0);
                beads.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 STRING (literal yarn) running parallel above the strand + 6 SHEARS at endpoints
            for (int i = 0; i < 8; i++) {
                double t = startOff + step * (i + 2);
                double bx = cosA * t;
                double bz = sinA * t;
                Location p = c.clone().add(bx, 1.8, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.15f, 0.15f, 0.7f).glow(255, 250, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double pos = i < 3 ? -STRAND_LEN / 2 - 0.6 - i * 0.5 : STRAND_LEN / 2 + 0.6 + (i - 3) * 0.5;
                double bx = cosA * pos;
                double bz = sinA * pos;
                Location p = c.clone().add(bx, 1.2, bz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SHEARS));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 220, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double cosA = Math.cos(strandAng);
            double sinA = Math.sin(strandAng);
            // Perpendicular axis for plucking displacement
            double pCosA = -sinA;
            double pSinA = cosA;

            double step = STRAND_LEN / (BEAD_COUNT - 1);
            double startOff = -STRAND_LEN / 2;
            for (int i = 0; i < BEAD_COUNT; i++) {
                double t = startOff + step * i;
                double bx = cosA * t;
                double bz = sinA * t;
                // Pluck shape — sin envelope across length, oscillating in time
                double envelope = Math.sin(Math.PI * (i / (double)(BEAD_COUNT - 1)));
                double plucked = envelope * Math.sin(tick * 0.35) * 0.9;
                double pluckedY = envelope * Math.cos(tick * 0.35) * 0.4;
                float fx = (float)(bx + pCosA * plucked);
                float fz = (float)(bz + pSinA * plucked);
                float fy = (float)(1.2 + pluckedY);
                beads.get(i).animateTo(
                        new Vector3f(fx - 0.06f, fy, fz - 0.45f),
                        new AxisAngle4f((float)strandAng, 0, 1, 0),
                        new Vector3f(0.12f, 0.12f, 0.9f), 4);
                if (tick % 4 == 0) {
                    w.spawnParticle(Particle.ENCHANT, c.clone().add(fx, fy, fz), 1, 0.1, 0.1, 0.1, 0.05);
                }
                if (tick % 8 == 0 && Math.random() < 0.4) {
                    DisplayBuilder.dustParticles(c.clone().add(fx, fy, fz), 1, 0.05, 255, 240, 230, 0.9f);
                }
            }

            // Periodic harp pluck sound
            if (tick % 35 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.2f + (float)Math.random() * 0.4f);

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (int i = 0; i < BEAD_COUNT; i++) {
                    double t = startOff + step * i;
                    double envelope = Math.sin(Math.PI * (i / (double)(BEAD_COUNT - 1)));
                    double plucked = envelope * Math.sin(tick * 0.35) * 0.9;
                    double pluckedY = envelope * Math.cos(tick * 0.35) * 0.4;
                    Location bead = c.clone().add(cosA * t + pCosA * plucked, 1.2 + pluckedY, sinA * t + pSinA * plucked);
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(bead) <= r2) {
                            p.damage(config.getDamage());
                            p.setNoDamageTicks(0);
                            break; // one bead damages this player per cycle
                        }
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new WoolStrand(plugin); }
    }

    // ================================================================
    // 10. FLORAL EXPLOSION — 16 POPPY ItemDisplays in ring (radius 3.5)
    //     expand outward then retract rhythmically. Constant in ring
    //     band on outward push, 10 hearts, 12-tick. CRIT particles on push.
    // ================================================================
    public static class FloralExplosion extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> flowers = new ArrayList<>();
        private final double[] flAng = new double[16];
        private static final double BASE_R = 3.5;
        private static final int CYCLE = 40; // 40 ticks per breathe cycle

        public FloralExplosion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floral_explosion", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(5.5);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(360);
            config.setCooldownTicks(340);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AZALEA_PLACE, 1.4f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_FLOWERING_AZALEA_PLACE, 1.2f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRASS_PLACE, 1.0f, 1.4f);

            for (int i = 0; i < 16; i++) {
                flAng[i] = Math.PI * 2 * i / 16;
                Location p = c.clone().add(Math.cos(flAng[i]) * BASE_R, 1.2, Math.sin(flAng[i]) * BASE_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.POPPY));
                h.scale(0.7f, 0.7f, 0.7f).glow(220, 50, 50).interpolation(6, 0);
                flowers.add(h);
                spawnedEntities.add(h.entity());
            }

            // Variety: 8 DANDELION + 6 AZURE_BLUET + 4 OXEYE_DAISY + 4 WILDFLOWERS scattered around
            Material[] variety = { Material.DANDELION, Material.DANDELION, Material.DANDELION, Material.DANDELION,
                    Material.DANDELION, Material.DANDELION, Material.DANDELION, Material.DANDELION,
                    Material.AZURE_BLUET, Material.AZURE_BLUET, Material.AZURE_BLUET,
                    Material.AZURE_BLUET, Material.AZURE_BLUET, Material.AZURE_BLUET,
                    Material.OXEYE_DAISY, Material.OXEYE_DAISY, Material.OXEYE_DAISY, Material.OXEYE_DAISY,
                    Material.LILY_OF_THE_VALLEY, Material.LILY_OF_THE_VALLEY, Material.LILY_OF_THE_VALLEY, Material.LILY_OF_THE_VALLEY };
            for (int v = 0; v < variety.length; v++) {
                double a = Math.PI * 2 * v / variety.length + Math.random() * 0.3;
                double rr = BASE_R - 0.6 + Math.random() * 1.4;
                Location p = c.clone().add(Math.cos(a) * rr, 0.5 + Math.random() * 1.6, Math.sin(a) * rr);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(variety[v]));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 200, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Breathe cycle: 0..CYCLE/2 = expand, CYCLE/2..CYCLE = contract
            double phase = (tick % CYCLE) / (double)CYCLE; // 0..1
            // Sin from 0 -> 1 -> 0
            double pulse = Math.sin(phase * Math.PI);
            double curR = BASE_R + pulse * 2.0; // 3.5 -> 5.5 -> 3.5
            boolean isPushing = (tick % CYCLE) <= (CYCLE / 2);

            for (int i = 0; i < flowers.size(); i++) {
                flAng[i] += 0.015;
                float by = (float)(1.2 + Math.sin(tick * 0.08 + i) * 0.2);
                float bx = (float)(Math.cos(flAng[i]) * curR);
                float bz = (float)(Math.sin(flAng[i]) * curR);
                flowers.get(i).animateTo(
                        new Vector3f(bx - 0.35f, by, bz - 0.35f),
                        new AxisAngle4f((float)(tick * 0.06 + i), 0, 1, 0),
                        new Vector3f(0.7f), 2);
            }

            // CRIT particles on outward push (peak velocity is at quarter cycle)
            if (isPushing && tick % 2 == 0) {
                for (int i = 0; i < flowers.size(); i++) {
                    Location p = c.clone().add(Math.cos(flAng[i]) * curR, 1.3, Math.sin(flAng[i]) * curR);
                    w.spawnParticle(Particle.CRIT, p, 2, 0.2, 0.1, 0.2, 0.1);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 220, 80, 100, 1.0f);
                }
            }
            // Background flora particles
            if (tick % 4 == 0) for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * curR;
                Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 1.5, Math.sin(a) * r);
                w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, p, 1, 0.1, 0.1, 0.1, 0.02);
                if (Math.random() < 0.3) w.spawnParticle(Particle.HAPPY_VILLAGER, p, 1, 0.1, 0.1, 0.1, 0);
            }
            // Pulse boom sound at expansion peaks
            if (tick % CYCLE == CYCLE / 2) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FLOWERING_AZALEA_BREAK, 1.3f, 1.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.7f, 1.6f);
            }

            // Damage in expanding ring band only when pushing outward
            if (isPushing && tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double inner = Math.max(0, curR - 1.2);
                double outer = curR + 1.2;
                double inner2 = inner * inner;
                double outer2 = outer * outer;
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double d2 = p.getLocation().distanceSquared(c);
                    if (d2 >= inner2 && d2 <= outer2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FloralExplosion(plugin); }
    }
}
