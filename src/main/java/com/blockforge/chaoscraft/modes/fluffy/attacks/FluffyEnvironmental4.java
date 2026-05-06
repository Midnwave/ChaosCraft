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
 * Fluffy Mode — ENVIRONMENTAL FX BATCH 4 (entries 31-40).
 * ItemDisplay-only attacks. Pure particle + ItemDisplay accents.
 * Each attack uses the cute / fluffy palette: pinks, pastels,
 * cottons, candy, plush.
 */
public final class FluffyEnvironmental4 {
    private FluffyEnvironmental4() {}

    private static final String MODE_PATH = "modes/fluffy/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CottonCandyStorm(plugin));
        registry.register(new FluorescentJellybeans(plugin));
        registry.register(new ToyCarChase(plugin));
        registry.register(new PlayfulLeap(plugin));
        registry.register(new FluffQuake(plugin));
        registry.register(new GummyBearSlam(plugin));
        registry.register(new WhistlingFeather(plugin));
        registry.register(new FloralTsunami(plugin));
        registry.register(new BirdsongShockwave(plugin));
        registry.register(new HairballExplosion(plugin));
    }

    // ================================================================
    // 31. COTTON CANDY STORM — Pink + white DUST spirals from 3 spawn
    //     points across the arena. 6 SUGAR_CANE ItemDisplays spin at
    //     the spiral centers. Constant-zone damage in each spiral.
    // ================================================================
    public static class CottonCandyStorm extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> sugarStalks = new ArrayList<>();
        private final double[] spiralX = new double[3];
        private final double[] spiralZ = new double[3];

        public CottonCandyStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cotton_candy_storm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(10.0); config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(10); config.setDamageDelayTicks(10);
            config.setDurationTicks(360); config.setCooldownTicks(360);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_WOOL_PLACE, 1.0f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEEHIVE_DRIP, 1.0f, 1.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 0.8f, 1.5f);

            // Three spiral spawn points around the center
            for (int s = 0; s < 3; s++) {
                double a = (Math.PI * 2 * s / 3) + Math.random() * 0.4;
                spiralX[s] = Math.cos(a) * 5.5;
                spiralZ[s] = Math.sin(a) * 5.5;
            }

            // 6 sugar-cane ItemDisplays — 2 at each of the 3 spiral centers
            for (int s = 0; s < 3; s++) {
                for (int k = 0; k < 2; k++) {
                    Location p = c.clone().add(spiralX[s], 1.5 + k * 1.2, spiralZ[s]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SUGAR_CANE));
                    h.scale(0.7f, 1.4f, 0.7f).glow(255, 200, 230).interpolation(20, 0);
                    sugarStalks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Candy accents per spiral: 4 SUGAR + 4 PINK_WOOL + 4 WHITE_WOOL fluff balls per pole
            for (int s = 0; s < 3; s++) {
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI * 2 * k / 4;
                    Location p = c.clone().add(spiralX[s] + Math.cos(a) * 0.8, 0.4 + k * 0.3, spiralZ[s] + Math.sin(a) * 0.8);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SUGAR));
                    h.scale(0.6f, 0.6f, 0.6f).glow(255, 250, 250).interpolation(40, 0);
                    spawnedEntities.add(h.entity());
                }
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI * 2 * k / 4 + Math.PI / 4;
                    Location p = c.clone().add(spiralX[s] + Math.cos(a) * 1.2, 2.5, spiralZ[s] + Math.sin(a) * 1.2);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PINK_WOOL));
                    h.scale(0.55f, 0.55f, 0.55f).glow(255, 180, 220).interpolation(40, 0);
                    spawnedEntities.add(h.entity());
                }
                for (int k = 0; k < 4; k++) {
                    double a = Math.PI * 2 * k / 4 + Math.PI / 8;
                    Location p = c.clone().add(spiralX[s] + Math.cos(a) * 1.0, 4.0, spiralZ[s] + Math.sin(a) * 1.0);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WHITE_WOOL));
                    h.scale(0.6f, 0.6f, 0.6f).glow(255, 250, 252).interpolation(40, 0);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Spin the sugar canes
            if (tick % 2 == 0) {
                for (int i = 0; i < sugarStalks.size(); i++) {
                    int s = i / 2; int k = i % 2;
                    float ty = (float)(1.5 + k * 1.2 + Math.sin(tick * 0.07 + i) * 0.2);
                    sugarStalks.get(i).animateTo(
                            new Vector3f((float)spiralX[s] - 0.35f, ty, (float)spiralZ[s] - 0.35f),
                            new AxisAngle4f((float)(tick * 0.18 + i), 0, 1, 0),
                            new Vector3f(0.7f, 1.4f, 0.7f), 2);
                }
            }

            // Pink + white DUST spirals — 3 spawn points with rising helix
            for (int s = 0; s < 3; s++) {
                Location anchor = c.clone().add(spiralX[s], 0, spiralZ[s]);
                for (int helix = 0; helix < 3; helix++) {
                    double a = tick * 0.22 + helix * (Math.PI * 2 / 3) + s * 0.6;
                    double y = (tick * 0.18 + helix * 1.5 + s * 0.7) % 6.0;
                    double r = 1.6 + Math.sin(tick * 0.05 + s) * 0.4;
                    Location p = anchor.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                    if ((tick + helix + s) % 2 == 0) {
                        DisplayBuilder.dustParticles(p, 1, 0.04, 255, 180, 220, 1.4f); // pink
                    } else {
                        DisplayBuilder.dustParticles(p, 1, 0.04, 255, 250, 252, 1.3f); // white
                    }
                }

                // Sparse fluffy plume at the very top
                if (tick % 3 == 0) {
                    w.spawnParticle(Particle.CLOUD, anchor.clone().add(0, 5.5, 0),
                            2, 0.5, 0.2, 0.5, 0.01);
                }
            }

            // Periodic fluffy ambience
            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BEEHIVE_DRIP, 0.6f, 1.4f);
            if (tick % 100 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_RABBIT_AMBIENT, 0.5f, 1.6f);
        }

        @Override public AbstractAttack newInstance() { return new CottonCandyStorm(plugin); }
    }

    // ================================================================
    // 32. FLUORESCENT JELLYBEANS — 12 SLIME_BALL ItemDisplays in bright
    //     candy colors bounce in random arcs at ground level. Each
    //     bounce-impact deals damage in a small radius.
    // ================================================================
    public static class FluorescentJellybeans extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> beans = new ArrayList<>();
        private final double[] bx = new double[12];
        private final double[] bz = new double[12];
        private final double[] by = new double[12];
        private final double[] vy = new double[12];
        private final double[] vx = new double[12];
        private final double[] vz = new double[12];

        public FluorescentJellybeans(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fluorescent_jellybeans", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(11.0); config.setImpactRadius(1.2);
            config.setDurationTicks(280); config.setCooldownTicks(320);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_SLIME_BLOCK_HIT, 1.0f, 1.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_SLIME_SQUISH, 0.9f, 1.5f);

            int[][] colors = {
                    {255, 80, 120}, {255, 180, 60}, {255, 240, 90}, {120, 240, 100},
                    {90, 200, 255}, {180, 120, 255}, {255, 130, 200}, {120, 255, 220},
                    {255, 100, 80}, {200, 255, 100}, {220, 100, 255}, {255, 220, 130}
            };

            for (int i = 0; i < 12; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1.5 + Math.random() * 5.0;
                bx[i] = Math.cos(a) * r;
                bz[i] = Math.sin(a) * r;
                by[i] = 4.0 + Math.random() * 2.0;
                vy[i] = 0.0;
                vx[i] = (Math.random() - 0.5) * 0.15;
                vz[i] = (Math.random() - 0.5) * 0.15;

                Location p = c.clone().add(bx[i], by[i], bz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SLIME_BALL));
                h.scale(0.55f, 0.55f, 0.55f)
                        .glow(colors[i][0], colors[i][1], colors[i][2])
                        .interpolation(2, 0);
                beans.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < beans.size(); i++) {
                // Apply gravity + drift
                vy[i] -= 0.05;
                by[i] += vy[i];
                bx[i] += vx[i];
                bz[i] += vz[i];

                // Bounce on ground
                if (by[i] <= 0.3 && vy[i] < 0) {
                    by[i] = 0.3;
                    vy[i] = 0.45 + Math.random() * 0.15; // bounce up
                    vx[i] *= 0.8;
                    vz[i] *= 0.8;

                    Location impact = c.clone().add(bx[i], 0.2, bz[i]);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_SLIME_SQUISH, 0.7f, 1.6f);
                    w.spawnParticle(Particle.CRIT, impact, 8, 0.3, 0.15, 0.3, 0.05);
                    DisplayBuilder.dustParticles(impact, 6, 0.3, 255, 200, 230, 1.2f);
                    triggerImpactDamage(impact);
                }

                Location pos = c.clone().add(bx[i], by[i], bz[i]);
                beans.get(i).animateTo(
                        new Vector3f((float)bx[i] - 0.275f, (float)by[i], (float)bz[i] - 0.275f),
                        new AxisAngle4f((float)(tick * 0.25 + i), 1, 1, 0),
                        new Vector3f(0.55f), 2);

                // Falling sparkle trail
                if (vy[i] < -0.1 && tick % 2 == 0) {
                    w.spawnParticle(Particle.END_ROD, pos, 1, 0.05, 0.05, 0.05, 0);
                }
            }

            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_SLIME_BLOCK_FALL, 0.7f, 1.6f);
        }

        @Override public AbstractAttack newInstance() { return new FluorescentJellybeans(plugin); }
    }

    // ================================================================
    // 33. TOY CAR CHASE — A MINECART ItemDisplay zooms across the arena
    //     in a straight line. Damages the player if they're on the path.
    // ================================================================
    public static class ToyCarChase extends EnvironmentalAttack {
        private ItemDisplayHandle car;
        private double dirX, dirZ;
        private double posX, posZ;
        private final double speed = 0.55;

        public ToyCarChase(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("toy_car_chase", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0); config.setImpactRadius(1.5);
            config.setDurationTicks(120); config.setCooldownTicks(280);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_MINECART_RIDING, 1.4f, 1.8f);

            double a = Math.random() * Math.PI * 2;
            dirX = Math.cos(a);
            dirZ = Math.sin(a);
            // Start from one side of the arena, drive across
            posX = -dirX * 9.0;
            posZ = -dirZ * 9.0;

            Location p = c.clone().add(posX, 0.6, posZ);
            car = displayBuilder.spawnItem(p, new ItemStack(Material.MINECART));
            car.scale(1.4f, 1.4f, 1.4f).glow(255, 80, 80).interpolation(2, 0);
            spawnedEntities.add(car.entity());

            // Toy car parts strewn along the path: 6 IRON_NUGGET wheels + 4 STICK axles + 4 RED_WOOL + 4 STRING tow ropes
            for (int i = 0; i < 6; i++) {
                double t = i / 5.0;
                Location pp = c.clone().add(posX + dirX * t * 18, 0.3, posZ + dirZ * t * 18);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.IRON_NUGGET));
                h.scale(0.65f, 0.65f, 0.65f).glow(180, 180, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double t = (i + 0.5) / 4.0;
                Location pp = c.clone().add(posX + dirX * t * 18, 0.3, posZ + dirZ * t * 18);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.STICK));
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 140, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double t = i / 3.0;
                double perp = (i % 2 == 0) ? 0.8 : -0.8;
                Location pp = c.clone().add(posX + dirX * t * 18 + (-dirZ) * perp, 0.4, posZ + dirZ * t * 18 + dirX * perp);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.RED_WOOL));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 80, 80).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double t = i / 3.0;
                Location pp = c.clone().add(posX + dirX * t * 18, 0.5, posZ + dirZ * t * 18);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.STRING));
                h.scale(0.6f, 0.1f, 0.1f).glow(255, 200, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null || car == null) return;
            World w = c.getWorld();

            posX += dirX * speed;
            posZ += dirZ * speed;

            Location pos = c.clone().add(posX, 0.6, posZ);
            car.animateTo(
                    new Vector3f((float)posX - 0.7f, 0.6f, (float)posZ - 0.7f),
                    new AxisAngle4f((float)(tick * 0.6), 0, 1, 0),
                    new Vector3f(1.4f), 2);

            // Trail particles
            w.spawnParticle(Particle.CRIT, pos, 6, 0.3, 0.2, 0.3, 0.1);
            w.spawnParticle(Particle.LARGE_SMOKE, pos.clone().add(0, 0.2, 0), 2, 0.2, 0.1, 0.2, 0.02);
            DisplayBuilder.dustParticles(pos, 4, 0.3, 255, 80, 80, 1.4f);

            // Tire skid marks
            if (tick % 2 == 0) {
                DisplayBuilder.dustParticles(pos.clone().add(0, -0.3, 0), 2, 0.1, 60, 60, 60, 1.0f);
            }

            // Damage on path contact
            if (tick % 2 == 0) {
                triggerImpactDamage(pos);
            }

            if (tick % 8 == 0) DisplayBuilder.playSound(pos, Sound.ENTITY_MINECART_RIDING, 1.0f, 1.8f);
        }

        @Override public AbstractAttack newInstance() { return new ToyCarChase(plugin); }
    }

    // ================================================================
    // 34. PLAYFUL LEAP — A large BONE ItemDisplay arcs across the
    //     entire arena in a high parabolic path. Impact damage on
    //     landing.
    // ================================================================
    public static class PlayfulLeap extends EnvironmentalAttack {
        private ItemDisplayHandle bone;
        private double startX, startZ;
        private double endX, endZ;
        private boolean landed = false;

        public PlayfulLeap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("playful_leap", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.0); config.setImpactRadius(4.0);
            config.setDurationTicks(80); config.setCooldownTicks(360);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_HOWL, 1.0f, 1.4f);
            DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 1.0f, 1.5f);

            double a = Math.random() * Math.PI * 2;
            startX = Math.cos(a) * 9.0;
            startZ = Math.sin(a) * 9.0;
            endX = -startX + (Math.random() - 0.5) * 3.0;
            endZ = -startZ + (Math.random() - 0.5) * 3.0;

            Location p = c.clone().add(startX, 1.0, startZ);
            bone = displayBuilder.spawnItem(p, new ItemStack(Material.BONE));
            bone.scale(2.0f, 2.0f, 2.0f).glow(240, 230, 200).interpolation(2, 0);
            spawnedEntities.add(bone.entity());

            // Dog-toy spread between start and end: 6 BONE_MEAL + 4 LEAD + 4 STRING + 4 NAME_TAG
            for (int i = 0; i < 6; i++) {
                double t = i / 5.0;
                Location pp = c.clone().add(startX + (endX - startX) * t, 0.3, startZ + (endZ - startZ) * t);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.BONE_MEAL));
                h.scale(0.65f, 0.65f, 0.65f).glow(255, 250, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double aa = Math.PI * 2 * i / 4;
                Location pp = c.clone().add(endX + Math.cos(aa) * 3, 0.4, endZ + Math.sin(aa) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.LEAD));
                h.scale(0.6f, 0.6f, 0.6f).glow(180, 150, 120).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double aa = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location pp = c.clone().add(startX + Math.cos(aa) * 2, 0.4, startZ + Math.sin(aa) * 2);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.STRING));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 220, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double t = (i + 0.5) / 4.0;
                Location pp = c.clone().add(startX + (endX - startX) * t, 0.5, startZ + (endZ - startZ) * t);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.NAME_TAG));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 220, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null || bone == null) return;
            World w = c.getWorld();

            int durationT = 60;
            double t = Math.min(1.0, tick / (double)durationT);
            double curX = startX + (endX - startX) * t;
            double curZ = startZ + (endZ - startZ) * t;
            // Parabolic arc: peak around t=0.5
            double peak = 9.0;
            double curY = 1.0 + 4.0 * peak * t * (1.0 - t);

            Location pos = c.clone().add(curX, curY, curZ);
            bone.animateTo(
                    new Vector3f((float)curX - 1.0f, (float)curY, (float)curZ - 1.0f),
                    new AxisAngle4f((float)(tick * 0.5), 1, 1, 0),
                    new Vector3f(2.0f), 2);

            // Trail
            w.spawnParticle(Particle.CRIT, pos, 4, 0.3, 0.3, 0.3, 0.05);
            w.spawnParticle(Particle.ENCHANT, pos, 6, 0.4, 0.4, 0.4, 0.5);
            DisplayBuilder.dustParticles(pos, 3, 0.3, 255, 240, 220, 1.3f);

            // Land
            if (!landed && tick >= durationT) {
                landed = true;
                Location impact = c.clone().add(endX, 0.5, endZ);
                DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.6f);
                DisplayBuilder.playSound(impact, Sound.ENTITY_WOLF_GROWL, 1.2f, 1.4f);
                w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.CRIT, impact, 30, 1.5, 0.4, 1.5, 0.3);
                DisplayBuilder.dustParticles(impact, 30, 1.5, 255, 240, 220, 1.5f);
                triggerImpactDamage(impact);
            }
        }

        @Override public AbstractAttack newInstance() { return new PlayfulLeap(plugin); }
    }

    // ================================================================
    // 35. FLUFF QUAKE — 5 concentric rings of BLOCK(DIRT) particle
    //     eruptions fire outward in sequence. Constant zone damage
    //     during eruption.
    // ================================================================
    public static class FluffQuake extends EnvironmentalAttack {
        private static final int[] RING_TICKS = {0, 8, 16, 24, 32};
        private static final double[] RING_RADII = {1.5, 3.0, 4.5, 6.0, 7.5};

        public FluffQuake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fluff_quake", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(12.0); config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(8); config.setDamageDelayTicks(0);
            config.setDurationTicks(80); config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRASS_BREAK, 1.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_RAVAGER_STEP, 1.2f, 0.7f);

            // Quake debris: 8 DIRT + 6 GRASS_BLOCK + 4 ROOTED_DIRT + 4 PODZOL chunks scattered concentrically
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 1.5, 0.3, Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.DIRT));
                h.scale(0.5f, 0.5f, 0.5f).glow(150, 100, 60).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 6;
                Location p = c.clone().add(Math.cos(a) * 3.5, 0.3, Math.sin(a) * 3.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GRASS_BLOCK));
                h.scale(0.6f, 0.6f, 0.6f).glow(120, 200, 80).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 5, 0.3, Math.sin(a) * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ROOTED_DIRT));
                h.scale(0.55f, 0.55f, 0.55f).glow(140, 90, 50).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 6.5, 0.3, Math.sin(a) * 6.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PODZOL));
                h.scale(0.55f, 0.55f, 0.55f).glow(120, 80, 50).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Fire each concentric ring at its scheduled tick
            for (int i = 0; i < RING_TICKS.length; i++) {
                if (tick == RING_TICKS[i]) {
                    double r = RING_RADII[i];
                    int points = (int)(r * 12);
                    for (int p = 0; p < points; p++) {
                        double a = (Math.PI * 2 * p) / points;
                        Location pt = c.clone().add(Math.cos(a) * r, 0.4, Math.sin(a) * r);
                        w.spawnParticle(Particle.BLOCK, pt, 8, 0.3, 0.6, 0.3, 0.15,
                                Material.DIRT.createBlockData());
                        if (p % 3 == 0) {
                            w.spawnParticle(Particle.BLOCK, pt.clone().add(0, 1.0, 0), 4, 0.3, 0.5, 0.3, 0.1,
                                    Material.GRASS_BLOCK.createBlockData());
                        }
                        DisplayBuilder.dustParticles(pt, 2, 0.3, 180, 120, 70, 1.3f);
                    }
                    DisplayBuilder.playSound(c, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.6f + i * 0.1f);
                }
            }

            // Ongoing crumble particles within current frontier
            int activeIdx = Math.min(RING_RADII.length - 1, tick / 8);
            if (tick % 2 == 0 && activeIdx < RING_RADII.length) {
                double r = RING_RADII[activeIdx];
                for (int k = 0; k < 6; k++) {
                    double a = Math.random() * Math.PI * 2;
                    double rr = Math.random() * r;
                    Location pt = c.clone().add(Math.cos(a) * rr, 0.2 + Math.random() * 0.5, Math.sin(a) * rr);
                    w.spawnParticle(Particle.BLOCK, pt, 2, 0.2, 0.2, 0.2, 0.05,
                            Material.DIRT.createBlockData());
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FluffQuake(plugin); }
    }

    // ================================================================
    // 36. GUMMY BEAR SLAM — 5 small colored CONCRETE ItemDisplays
    //     simultaneously fall from above and bounce once on impact.
    //     Each individual landing applies impact damage.
    // ================================================================
    public static class GummyBearSlam extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> bears = new ArrayList<>();
        private final double[] bx = new double[5];
        private final double[] bz = new double[5];
        private final double[] by = new double[5];
        private final double[] vy = new double[5];
        private final boolean[] bounced = new boolean[5];
        private final boolean[] landed = new boolean[5];
        private final Material[] colors = {
                Material.RED_CONCRETE, Material.YELLOW_CONCRETE, Material.GREEN_CONCRETE,
                Material.PINK_CONCRETE, Material.PURPLE_CONCRETE
        };
        private final int[][] glow = {
                {255, 80, 100}, {255, 220, 80}, {120, 240, 120},
                {255, 160, 200}, {200, 120, 255}
        };

        public GummyBearSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gummy_bear_slam", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(13.0); config.setImpactRadius(1.5);
            config.setDurationTicks(140); config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_HONEY_BLOCK_PLACE, 1.0f, 1.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.7f, 1.5f);

            for (int i = 0; i < 5; i++) {
                double a = (Math.PI * 2 * i / 5) + Math.random() * 0.5;
                double r = 2.0 + Math.random() * 4.0;
                bx[i] = Math.cos(a) * r;
                bz[i] = Math.sin(a) * r;
                by[i] = 9.0 + Math.random() * 2.0;
                vy[i] = 0.0;
                bounced[i] = false;
                landed[i] = false;

                Location p = c.clone().add(bx[i], by[i], bz[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(colors[i]));
                h.scale(0.7f, 0.7f, 0.7f)
                        .glow(glow[i][0], glow[i][1], glow[i][2])
                        .interpolation(2, 0);
                bears.add(h);
                spawnedEntities.add(h.entity());
            }

            // Candy spread: 8 SUGAR + 6 SLIME_BALL + 4 HONEY_BOTTLE + 4 SWEET_BERRIES at ground
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 5, 0.3, Math.sin(a) * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SUGAR));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 250, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 6;
                Location p = c.clone().add(Math.cos(a) * 3.5, 0.4, Math.sin(a) * 3.5);
                int[] col = glow[i % glow.length];
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SLIME_BALL));
                h.scale(0.5f, 0.5f, 0.5f).glow(col[0], col[1], col[2]).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 2, 0.4, Math.sin(a) * 2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.HONEY_BOTTLE));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 200, 60).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 6.5, 0.3, Math.sin(a) * 6.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SWEET_BERRIES));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 70, 90).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < bears.size(); i++) {
                if (landed[i]) continue;

                vy[i] -= 0.06;
                by[i] += vy[i];

                Location pos = c.clone().add(bx[i], by[i], bz[i]);
                bears.get(i).animateTo(
                        new Vector3f((float)bx[i] - 0.35f, (float)by[i], (float)bz[i] - 0.35f),
                        new AxisAngle4f((float)(tick * 0.2 + i), 1, 1, 0),
                        new Vector3f(0.7f), 2);

                // Trail dust
                if (tick % 2 == 0) {
                    DisplayBuilder.dustParticles(pos, 2, 0.2, glow[i][0], glow[i][1], glow[i][2], 1.3f);
                }

                if (by[i] <= 0.4) {
                    if (!bounced[i]) {
                        bounced[i] = true;
                        by[i] = 0.4;
                        vy[i] = 0.42;

                        Location impact = c.clone().add(bx[i], 0.3, bz[i]);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_HONEY_BLOCK_FALL, 0.9f, 1.4f);
                        w.spawnParticle(Particle.CRIT, impact, 14, 0.5, 0.2, 0.5, 0.1);
                        DisplayBuilder.dustParticles(impact, 16, 0.6, glow[i][0], glow[i][1], glow[i][2], 1.5f);
                        triggerImpactDamage(impact);
                    } else {
                        // Final landing — settle
                        landed[i] = true;
                        by[i] = 0.4;
                        vy[i] = 0;
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new GummyBearSlam(plugin); }
    }

    // ================================================================
    // 37. WHISTLING FEATHER — A FEATHER ItemDisplay rises spinning to
    //     Y+8, then fires horizontally toward the target player at
    //     speed. Impact damages on contact.
    // ================================================================
    public static class WhistlingFeather extends EnvironmentalAttack {
        private ItemDisplayHandle feather;
        private double posX, posY, posZ;
        private double dirX, dirZ;
        private int phase = 0; // 0 = rising, 1 = firing
        private boolean impacted = false;
        // Cached spawn-time fire direction (set in onSpawn, applied at phase 1).
        private double cachedDirX = 0, cachedDirZ = 0;
        private boolean hasCachedDir = false;

        public WhistlingFeather(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("whistling_feather", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0); config.setImpactRadius(2.0);
            config.setDurationTicks(140); config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_PARROT_FLY, 1.0f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1.0f, 1.8f);

            posX = 0;
            posY = 1.0;
            posZ = 0;
            dirX = 0;
            dirZ = 0;

            Location p = c.clone().add(posX, posY, posZ);
            feather = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
            feather.scale(1.4f, 1.4f, 1.4f).glow(255, 240, 250).interpolation(2, 0);
            spawnedEntities.add(feather.entity());

            // Cache spawn-time fire direction (used when feather reaches Y+8).
            Player initialTarget = getTargetPlayer();
            if (initialTarget != null && initialTarget.isOnline()) {
                double tx = initialTarget.getLocation().getX() - c.getX();
                double tz = initialTarget.getLocation().getZ() - c.getZ();
                double mag = Math.max(0.001, Math.sqrt(tx * tx + tz * tz));
                cachedDirX = tx / mag;
                cachedDirZ = tz / mag;
                hasCachedDir = true;
            }

            // Musical accents around the launch site: 6 NOTE_BLOCK + 4 GOAT_HORN + 4 BELL + 4 FEATHER companions
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location pp = c.clone().add(Math.cos(a) * 4, 0.5, Math.sin(a) * 4);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.NOTE_BLOCK));
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 130, 80).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location pp = c.clone().add(Math.cos(a) * 5.5, 1.0, Math.sin(a) * 5.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.GOAT_HORN));
                h.scale(0.6f, 0.6f, 0.6f).glow(200, 180, 130).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location pp = c.clone().add(Math.cos(a) * 3, 2.5, Math.sin(a) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.BELL));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 220, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location pp = c.clone().add(Math.cos(a) * 1.5, 2.0, Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.FEATHER));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 240, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null || feather == null) return;
            World w = c.getWorld();

            if (phase == 0) {
                // Rise spinning to Y+8
                posY += 0.22;
                if (posY >= 8.0) {
                    posY = 8.0;
                    phase = 1;
                    // Aim using the cached spawn-time direction (no live tracking).
                    if (hasCachedDir) {
                        dirX = cachedDirX;
                        dirZ = cachedDirZ;
                    } else {
                        double a = Math.random() * Math.PI * 2;
                        dirX = Math.cos(a);
                        dirZ = Math.sin(a);
                    }
                    DisplayBuilder.playSound(c.clone().add(0, 8, 0), Sound.ENTITY_ARROW_SHOOT, 1.4f, 1.6f);
                    DisplayBuilder.playSound(c.clone().add(0, 8, 0), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                }
            } else {
                // Horizontal fire toward target
                posX += dirX * 0.7;
                posZ += dirZ * 0.7;
            }

            Location pos = c.clone().add(posX, posY, posZ);
            feather.animateTo(
                    new Vector3f((float)posX - 0.7f, (float)posY, (float)posZ - 0.7f),
                    new AxisAngle4f((float)(tick * 0.85), 0, 1, 0),
                    new Vector3f(1.4f), 2);

            // Spin trail particles
            w.spawnParticle(Particle.CRIT, pos, 3, 0.2, 0.2, 0.2, 0.05);
            DisplayBuilder.dustParticles(pos, 4, 0.2, 255, 240, 250, 1.3f);
            if (phase == 1) {
                w.spawnParticle(Particle.CLOUD, pos, 2, 0.15, 0.15, 0.15, 0.02);
            }

            // Impact check during firing phase
            if (phase == 1 && !impacted && tick % 2 == 0) {
                double r2 = config.getImpactRadius() * config.getImpactRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(pos) <= r2) {
                        impacted = true;
                        DisplayBuilder.playSound(pos, Sound.ENTITY_ARROW_HIT, 1.2f, 1.4f);
                        DisplayBuilder.playSound(pos, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.8f);
                        w.spawnParticle(Particle.EXPLOSION, pos, 1, 0, 0, 0, 0);
                        DisplayBuilder.dustParticles(pos, 20, 0.6, 255, 240, 250, 1.5f);
                        triggerImpactDamage(pos);
                        break;
                    }
                }
            }

            // Whistle audio loop
            if (tick % 6 == 0) DisplayBuilder.playSound(pos, Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.5f, 1.7f + (float)Math.random() * 0.2f);
        }

        @Override public AbstractAttack newInstance() { return new WhistlingFeather(plugin); }
    }

    // ================================================================
    // 38. FLORAL TSUNAMI — A wall of CHERRY_LEAVES particles + pink
    //     DUST sweeps across the full arena from one edge to the
    //     other. 8 POPPY ItemDisplays embedded in the wall ride along.
    //     Constant contact damage near the wall.
    // ================================================================
    public static class FloralTsunami extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> poppies = new ArrayList<>();
        private double waveOffset = -10.0; // start past one edge
        private double dirX, dirZ;
        private double perpX, perpZ;
        private final double speed = 0.32;

        public FloralTsunami(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floral_tsunami", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(14.0); config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(8); config.setDamageDelayTicks(0);
            config.setDurationTicks(200); config.setCooldownTicks(340);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.4f, 1.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AZALEA_LEAVES_PLACE, 1.2f, 1.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_BREATH, 0.8f, 1.6f);

            double a = Math.random() * Math.PI * 2;
            dirX = Math.cos(a);
            dirZ = Math.sin(a);
            perpX = -dirZ;
            perpZ = dirX;

            // 8 poppies spread along the wall
            for (int i = 0; i < 8; i++) {
                double offset = -7.0 + i * 2.0;
                Location p = c.clone().add(perpX * offset + dirX * waveOffset,
                        0.6, perpZ * offset + dirZ * waveOffset);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.POPPY));
                h.scale(0.9f, 1.2f, 0.9f).glow(255, 100, 140).interpolation(2, 0);
                poppies.add(h);
                spawnedEntities.add(h.entity());
            }

            // Embedded in the wave: 8 PINK_PETALS + 6 ROSE_BUSH + 4 AZALEA + 4 OXEYE_DAISY at varying heights
            Material[] waveItems = {
                    Material.PINK_PETALS, Material.PINK_PETALS, Material.PINK_PETALS, Material.PINK_PETALS,
                    Material.PINK_PETALS, Material.PINK_PETALS, Material.PINK_PETALS, Material.PINK_PETALS,
                    Material.ROSE_BUSH, Material.ROSE_BUSH, Material.ROSE_BUSH, Material.ROSE_BUSH, Material.ROSE_BUSH, Material.ROSE_BUSH,
                    Material.AZALEA, Material.AZALEA, Material.AZALEA, Material.AZALEA,
                    Material.OXEYE_DAISY, Material.OXEYE_DAISY, Material.OXEYE_DAISY, Material.OXEYE_DAISY };
            for (int i = 0; i < waveItems.length; i++) {
                double off = -7.0 + (i / (double) waveItems.length) * 14.0;
                double yy = 1.0 + (i % 4) * 0.8;
                Location p = c.clone().add(perpX * off + dirX * waveOffset,
                        yy, perpZ * off + dirZ * waveOffset);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(waveItems[i]));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 150, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            waveOffset += speed;

            // Move the poppies
            for (int i = 0; i < poppies.size(); i++) {
                double offset = -7.0 + i * 2.0;
                double tx = perpX * offset + dirX * waveOffset;
                double tz = perpZ * offset + dirZ * waveOffset;
                double ty = 0.6 + Math.sin(tick * 0.15 + i) * 0.25;
                poppies.get(i).animateTo(
                        new Vector3f((float)tx - 0.45f, (float)ty, (float)tz - 0.45f),
                        new AxisAngle4f((float)(tick * 0.18 + i), 0, 1, 0),
                        new Vector3f(0.9f, 1.2f, 0.9f), 2);
            }

            // The wall: a vertical plane perpendicular to the travel direction
            for (int slice = -8; slice <= 8; slice++) {
                double offset = slice * 0.9;
                for (int h = 0; h < 8; h++) {
                    double yy = 0.4 + h * 0.55;
                    Location p = c.clone().add(perpX * offset + dirX * waveOffset,
                            yy, perpZ * offset + dirZ * waveOffset);
                    if ((slice + h + tick) % 2 == 0) {
                        w.spawnParticle(Particle.CHERRY_LEAVES, p, 1, 0.2, 0.2, 0.2, 0.02);
                    }
                    if ((slice + h + tick) % 3 == 0) {
                        DisplayBuilder.dustParticles(p, 1, 0.25, 255, 160, 200, 1.3f);
                    }
                }
            }

            // Swirling petals just behind the wave
            if (tick % 2 == 0) for (int i = 0; i < 6; i++) {
                double offset = -7.0 + Math.random() * 14.0;
                double back = waveOffset - 1.5 - Math.random() * 1.5;
                Location p = c.clone().add(perpX * offset + dirX * back,
                        0.5 + Math.random() * 3.0, perpZ * offset + dirZ * back);
                w.spawnParticle(Particle.CHERRY_LEAVES, p, 1, 0.2, 0.2, 0.2, 0.01);
            }

            // Override base radius damage with wall-line damage
            // (constant damage is already handled by AbstractAttack.applyRadiusDamage,
            // but we want it to track the wave). Update center to follow the wall:
            Location waveCenter = c.clone().add(dirX * waveOffset, 0, dirZ * waveOffset);
            setCenter(waveCenter);

            if (tick % 30 == 0) DisplayBuilder.playSound(waveCenter, Sound.WEATHER_RAIN, 1.0f, 1.5f);
        }

        @Override public AbstractAttack newInstance() { return new FloralTsunami(plugin); }
    }

    // ================================================================
    // 39. BIRDSONG SHOCKWAVE — A PARROT ItemDisplay at center spins
    //     rapidly. Then a SONIC_BOOM + ENCHANT ring expands outward.
    //     Damage is at the ring's frontier, not the whole zone.
    // ================================================================
    public static class BirdsongShockwave extends EnvironmentalAttack {
        private ItemDisplayHandle parrot;
        private double ringRadius = 0.0;
        private final double maxRadius = 8.0;
        private boolean ringTriggered = false;

        public BirdsongShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("birdsong_shockwave", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0); config.setImpactRadius(6.0);
            config.setDurationTicks(140); config.setCooldownTicks(320);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_PARROT_AMBIENT, 1.4f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1.0f, 1.8f);

            parrot = displayBuilder.spawnItem(c.clone().add(0, 1.6, 0), new ItemStack(Material.FEATHER));
            parrot.scale(1.6f, 1.6f, 1.6f).glow(120, 200, 255).interpolation(2, 0);
            spawnedEntities.add(parrot.entity());

            // Bird flock around the parrot: 8 FEATHER + 6 EGG + 4 GOAT_HORN + 4 NOTE_BLOCK
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 2.5, 1.6 + Math.sin(a) * 0.6, Math.sin(a) * 2.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                int[] cols = {255, 200, 150};
                if (i % 3 == 1) cols = new int[]{120, 200, 255};
                if (i % 3 == 2) cols = new int[]{255, 100, 100};
                h.scale(0.6f, 0.6f, 0.6f).glow(cols[0], cols[1], cols[2]).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 6;
                Location p = c.clone().add(Math.cos(a) * 4, 0.4, Math.sin(a) * 4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.EGG));
                h.scale(0.5f, 0.5f, 0.5f).glow(245, 230, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 5, 1.2, Math.sin(a) * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GOAT_HORN));
                h.scale(0.55f, 0.55f, 0.55f).glow(200, 180, 130).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 3, 0.6, Math.sin(a) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NOTE_BLOCK));
                h.scale(0.55f, 0.55f, 0.55f).glow(180, 130, 80).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Parrot spin
            if (parrot != null) {
                float ty = 1.6f + (float)Math.sin(tick * 0.2) * 0.25f;
                parrot.animateTo(
                        new Vector3f(-0.8f, ty, -0.8f),
                        new AxisAngle4f((float)(tick * 0.55), 0, 1, 0),
                        new Vector3f(1.6f), 2);
            }

            // Charge-up phase: spin plus enchant + dust at center
            if (tick < 30) {
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.ENCHANT, c.clone().add(0, 1.6, 0),
                            6, 1.0, 0.5, 1.0, 1.0);
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.6, 0), 4, 0.6,
                            120, 200, 255, 1.4f);
                }
                if (tick == 28) {
                    // Pre-shockwave roar
                    DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 1.6f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_PARROT_IMITATE_GHAST, 1.4f, 1.6f);
                }
            }

            // Expanding ring frontier
            if (tick >= 30) {
                ringRadius += 0.32;
                if (ringRadius > maxRadius) ringRadius = maxRadius;

                // Render the ring frontier
                int points = (int)(ringRadius * 14);
                for (int i = 0; i < points; i++) {
                    double a = (Math.PI * 2 * i) / points;
                    Location p = c.clone().add(Math.cos(a) * ringRadius, 1.0, Math.sin(a) * ringRadius);
                    if (i % 4 == 0) {
                        w.spawnParticle(Particle.SONIC_BOOM, p, 1, 0.05, 0.05, 0.05, 0);
                    }
                    w.spawnParticle(Particle.ENCHANT, p, 1, 0.1, 0.4, 0.1, 0.5);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 120, 200, 255, 1.4f);
                }

                // Damage at the frontier — narrow band
                if (tick % 2 == 0 && !ringTriggered) {
                    double r2_outer = (ringRadius + 0.6) * (ringRadius + 0.6);
                    double r2_inner = Math.max(0, ringRadius - 0.6) * Math.max(0, ringRadius - 0.6);
                    for (Player pl : w.getPlayers()) {
                        if (pl.getGameMode() != GameMode.SURVIVAL || pl.isInvulnerable()) continue;
                        double d2 = pl.getLocation().distanceSquared(c);
                        if (d2 <= r2_outer && d2 >= r2_inner) {
                            triggerImpactDamage(pl.getLocation());
                            DisplayBuilder.playSound(pl.getLocation(), Sound.ENTITY_PARROT_HURT, 1.0f, 1.6f);
                        }
                    }
                }

                if ((int)ringRadius != (int)(ringRadius - 0.32)) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.7f, 1.5f + (float)ringRadius * 0.05f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new BirdsongShockwave(plugin); }
    }

    // ================================================================
    // 40. HAIRBALL EXPLOSION — Dense gray/brown DUST burst from center.
    //     8 RABBIT_FOOT ItemDisplays fly outward in a starburst.
    //     Impact damage in a wide radius.
    // ================================================================
    public static class HairballExplosion extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> feet = new ArrayList<>();
        private final double[] footAng = new double[8];
        private double radius = 0.0;
        private boolean detonated = false;

        public HairballExplosion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hairball_explosion", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0); config.setImpactRadius(4.5);
            config.setDurationTicks(120); config.setCooldownTicks(320);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_HISS, 1.4f, 1.0f);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURREOW, 1.0f, 0.8f);

            for (int i = 0; i < 8; i++) {
                footAng[i] = (Math.PI * 2 * i / 8);
                Location p = c.clone().add(0, 1.0, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_FOOT));
                h.scale(0.7f, 0.7f, 0.7f).glow(160, 130, 100).interpolation(2, 0);
                feet.add(h);
                spawnedEntities.add(h.entity());
            }

            // Fur cloud: 8 RABBIT_HIDE + 6 STRING + 4 FEATHER + 4 GRAY_WOOL clumps surrounding the hairball
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 1.2, 0.8, Math.sin(a) * 1.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                h.scale(0.55f, 0.55f, 0.55f).glow(160, 130, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 6;
                Location p = c.clone().add(Math.cos(a) * 0.8, 1.2, Math.sin(a) * 0.8);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.6f, 0.6f, 0.6f).glow(180, 160, 140).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 1.6, 1.5, Math.sin(a) * 1.6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 180, 160).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 1.0, 0.6, Math.sin(a) * 1.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GRAY_WOOL));
                h.scale(0.5f, 0.5f, 0.5f).glow(140, 120, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Charge-up: rabbit feet swirl tightly at center, dust gathers
            if (tick < 24) {
                double rr = 0.5 + tick * 0.04;
                for (int i = 0; i < feet.size(); i++) {
                    double a = footAng[i] + tick * 0.4;
                    float tx = (float)(Math.cos(a) * rr);
                    float tz = (float)(Math.sin(a) * rr);
                    feet.get(i).animateTo(
                            new Vector3f(tx - 0.35f, 1.0f, tz - 0.35f),
                            new AxisAngle4f((float)(tick * 0.3 + i), 1, 1, 0),
                            new Vector3f(0.7f), 2);
                }
                if (tick % 2 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 8, 0.6, 100, 80, 60, 1.5f);
                    DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 6, 0.5, 160, 140, 120, 1.4f);
                }
                if (tick == 22) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_CAT_HISS, 1.4f, 0.8f);
                }
                return;
            }

            // Detonation tick
            if (!detonated) {
                detonated = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 1.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_CAT_AMBIENT, 1.2f, 0.7f);
                w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 1.0, 0), 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 1.0, 0),
                        40, 1.5, 1.5, 1.5, 0.1);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 60, 2.0, 100, 80, 60, 1.8f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 50, 2.0, 80, 60, 50, 1.6f);
                triggerImpactDamage(c.clone().add(0, 1.0, 0));
            }

            // Outward fly: feet expand from center
            radius = Math.min(8.0, radius + 0.42);
            for (int i = 0; i < feet.size(); i++) {
                double a = footAng[i];
                float tx = (float)(Math.cos(a) * radius);
                float ty = 1.0f + (float)Math.sin(tick * 0.1 + i) * 0.4f;
                float tz = (float)(Math.sin(a) * radius);
                feet.get(i).animateTo(
                        new Vector3f(tx - 0.35f, ty, tz - 0.35f),
                        new AxisAngle4f((float)(tick * 0.5 + i), 1, 1, 0),
                        new Vector3f(0.7f), 2);

                // Trailing dust per foot
                Location pos = c.clone().add(tx, ty, tz);
                w.spawnParticle(Particle.LARGE_SMOKE, pos, 2, 0.2, 0.2, 0.2, 0.02);
                DisplayBuilder.dustParticles(pos, 3, 0.2, 130, 110, 90, 1.3f);
            }

            // Lingering smoke cloud
            if (tick % 3 == 0) for (int k = 0; k < 6; k++) {
                double a = Math.random() * Math.PI * 2;
                double rr = Math.random() * 4.0;
                Location p = c.clone().add(Math.cos(a) * rr, 0.5 + Math.random() * 2.5, Math.sin(a) * rr);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.2, 0.2, 0.2, 0.01);
            }
        }

        @Override public AbstractAttack newInstance() { return new HairballExplosion(plugin); }
    }
}
