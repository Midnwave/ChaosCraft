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
 * Fluffy Mode — ENVIRONMENTAL FX BATCH 5 (entries 41-50).
 * "Abstract / Mythic Cute" — surreal sweet visuals dialed to chaos.
 * Pure ItemDisplay + particle attacks. NO BlockDisplays.
 * Each attack uses ItemDisplays only (per design rules) with layered
 * particle systems for atmosphere. Multi-phase lifecycle: spawn -> active -> dissipate.
 */
public final class FluffyEnvironmental5 {
    private FluffyEnvironmental5() {}

    private static final String MODE_PATH = "modes/fluffy/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CatnapMiasma(plugin));
        registry.register(new MushroomGas(plugin));
        registry.register(new GoldfishBowlDrop(plugin));
        registry.register(new CrystalPellets(plugin));
        registry.register(new TeddyBearDrop(plugin));
        registry.register(new FurballRoll(plugin));
        registry.register(new CuddleCrash(plugin));
        registry.register(new PinwheelStorm(plugin));
        registry.register(new FurballAvalanche(plugin));
        registry.register(new CozyChaos(plugin));
    }

    // ================================================================
    // 41. CATNAP MIASMA — SPORE_BLOSSOM_AIR + ENCHANT orbs drift across
    //     the arena nearly invisibly. RABBIT_HIDE ItemDisplays float at
    //     low opacity. Wide constant radius coverage, low-tick damage.
    // ================================================================
    public static class CatnapMiasma extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> hides = new ArrayList<>();
        private final double[] hideAng = new double[12];
        private final double[] hideR = new double[12];
        private final double[] hideY = new double[12];

        public CatnapMiasma(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("catnap_miasma", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(90.0); // 6 hearts
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(420);
            config.setCooldownTicks(180);
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 0.7f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.4f, 0.5f);

            for (int i = 0; i < 12; i++) {
                hideAng[i] = Math.random() * Math.PI * 2;
                hideR[i] = 0.8 + Math.random() * (config.getDamageRadius() - 0.5);
                hideY[i] = 0.6 + Math.random() * 3.0;
                Location p = c.clone().add(Math.cos(hideAng[i]) * hideR[i], hideY[i], Math.sin(hideAng[i]) * hideR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                h.scale(0.6f, 0.6f, 0.6f).glow(220, 200, 230).interpolation(40, 0);
                hides.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            double radius = config.getDamageRadius();

            // Drift the rabbit hides slowly around the area
            if (tick % 20 == 0) {
                for (int i = 0; i < hides.size(); i++) {
                    hideAng[i] += (Math.random() - 0.5) * 0.4;
                    hideR[i] = Math.max(0.5, Math.min(radius, hideR[i] + (Math.random() - 0.5) * 0.6));
                    hideY[i] = Math.max(0.4, Math.min(4.5, hideY[i] + (Math.random() - 0.5) * 0.6));
                    float tx = (float) (Math.cos(hideAng[i]) * hideR[i]);
                    float tz = (float) (Math.sin(hideAng[i]) * hideR[i]);
                    hides.get(i).animateTo(
                            new Vector3f(tx - 0.3f, (float) hideY[i], tz - 0.3f),
                            new AxisAngle4f((float) (tick * 0.02 + i), 0, 1, 0),
                            new Vector3f(0.6f, 0.6f, 0.6f), 20);
                }
            }

            // Wide drifting spore mist
            if (tick % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * radius;
                    Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 4.0, Math.sin(a) * r);
                    w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, p, 1, 0.4, 0.6, 0.4, 0.0);
                    if (Math.random() < 0.4) {
                        w.spawnParticle(Particle.ENCHANT, p, 1, 0.3, 0.3, 0.3, 0.4);
                    }
                }
            }

            // Faint cat-color dust orbs to suggest "sleep"
            if (tick % 6 == 0) {
                for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * radius;
                    Location p = c.clone().add(Math.cos(a) * r, 1.0 + Math.random() * 3.0, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(p, 1, 0.3, 220, 200, 235, 1.4f);
                }
            }

            // Periodic soft purr
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 0.5f, 0.9f);
            if (tick % 140 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_CAT_AMBIENT, 0.4f, 1.2f);
        }

        @Override public AbstractAttack newInstance() { return new CatnapMiasma(plugin); }
    }

    // ================================================================
    // 42. MUSHROOM GAS — 6 RED_MUSHROOM ItemDisplays at ground.
    //     MYCELIUM + SPORE columns rise from each. Constant short radius
    //     per column, frequent damage tick.
    // ================================================================
    public static class MushroomGas extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> caps = new ArrayList<>();
        private final List<Location> columnLocs = new ArrayList<>();
        private static final int COLUMNS = 6;

        public MushroomGas(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mushroom_gas", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(135.0); // 9 hearts
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(360);
            config.setCooldownTicks(160);
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_FUNGUS_PLACE, 1.1f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BIG_DRIPLEAF_PLACE, 0.9f, 0.7f);

            // Place 6 mushrooms in a hex around center
            for (int i = 0; i < COLUMNS; i++) {
                double a = Math.PI * 2 * i / COLUMNS;
                double r = 4.0;
                Location p = c.clone().add(Math.cos(a) * r, 0.4, Math.sin(a) * r);
                columnLocs.add(p);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RED_MUSHROOM));
                h.scale(1.6f, 1.6f, 1.6f).glow(255, 60, 60).interpolation(20, 0);
                caps.add(h);
                spawnedEntities.add(h.entity());
            }

            // Mushroom understory: 6 BROWN_MUSHROOM + 6 SPORE_BLOSSOM + 4 CRIMSON_FUNGUS + 4 WARPED_FUNGUS + 4 MOSS_BLOCK
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 6;
                Location p = c.clone().add(Math.cos(a) * 2.5, 0.4, Math.sin(a) * 2.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BROWN_MUSHROOM));
                h.scale(1.0f, 1.0f, 1.0f).glow(180, 130, 80).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 5.5, 1.5, Math.sin(a) * 5.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SPORE_BLOSSOM));
                h.scale(0.7f, 0.7f, 0.7f).glow(220, 130, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 6.5, 0.4, Math.sin(a) * 6.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CRIMSON_FUNGUS));
                h.scale(0.8f, 0.8f, 0.8f).glow(200, 50, 60).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 5, 0.4, Math.sin(a) * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WARPED_FUNGUS));
                h.scale(0.8f, 0.8f, 0.8f).glow(50, 200, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 1.5, 0.3, Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.MOSS_BLOCK));
                h.scale(0.6f, 0.6f, 0.6f).glow(120, 180, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Rising spore + mycelium columns from each mushroom cap
            if (tick % 1 == 0) {
                for (Location base : columnLocs) {
                    for (int yLayer = 0; yLayer < 5; yLayer++) {
                        double y = 0.5 + yLayer * 0.7 + (tick % 20) * 0.05;
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 1.0;
                        Location p = base.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                        if (Math.random() < 0.6) w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, p, 1, 0.15, 0.1, 0.15, 0.0);
                        if (Math.random() < 0.3) w.spawnParticle(Particle.MYCELIUM, p, 2, 0.2, 0.1, 0.2, 0.0);
                        if (Math.random() < 0.2) DisplayBuilder.dustParticles(p, 1, 0.2, 200, 70, 90, 1.3f);
                    }
                }
            }

            // Cap pulse animation
            if (tick % 20 == 0) {
                for (int i = 0; i < caps.size(); i++) {
                    float s = 1.4f + (float) Math.abs(Math.sin(tick * 0.1 + i)) * 0.4f;
                    caps.get(i).animateTo(
                            new Vector3f(-s / 2f, 0.4f, -s / 2f),
                            new AxisAngle4f((float) (tick * 0.04), 0, 1, 0),
                            new Vector3f(s, s, s), 20);
                }
            }

            if (tick % 40 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_FUNGUS_BREAK, 0.7f, 0.8f);
            if (tick % 90 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_MOSS_BREAK, 0.6f, 0.6f);

            // Custom multi-center damage — 1.5 radius per column
            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double rSq = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    for (Location base : columnLocs) {
                        double dx = pl.getX() - base.getX();
                        double dz = pl.getZ() - base.getZ();
                        if (dx * dx + dz * dz <= rSq && Math.abs(pl.getY() - base.getY()) <= 6.0) {
                            p.damage(config.getDamage());
                            p.setNoDamageTicks(0);
                            break;
                        }
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new MushroomGas(plugin); }
    }

    // ================================================================
    // 43. GOLDFISH BOWL DROP — Large GLASS_BOTTLE ItemDisplay drops from
    //     above. Orange DUST "fish" orbit inside during fall. Smashes on
    //     impact with LANDING_WATER + CRIT burst.
    // ================================================================
    public static class GoldfishBowlDrop extends EnvironmentalAttack {
        private ItemDisplayHandle bowl;
        private boolean impactFired = false;
        private final double startY = 16.0;
        private static final int FALL_TICKS = 50;

        public GoldfishBowlDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("goldfish_bowl_drop", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(360.0); // 24 hearts
            config.setImpactRadius(4.5);
            config.setDurationTicks(120);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.2f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 0.5f);

            Location top = c.clone().add(0, startY, 0);
            bowl = displayBuilder.spawnItem(top, new ItemStack(Material.GLASS_BOTTLE));
            bowl.scale(3.5f, 3.5f, 3.5f).glow(120, 200, 255).interpolation(0, 0);
            spawnedEntities.add(bowl.entity());

            // Goldfish + water gear: 6 SALMON + 6 PRISMARINE_SHARD + 4 NAUTILUS_SHELL + 4 SEA_PICKLE + 4 KELP at impact zone
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 3, 0.4, Math.sin(a) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SALMON));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 140, 60).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 6;
                Location p = c.clone().add(Math.cos(a) * 4, 0.4, Math.sin(a) * 4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PRISMARINE_SHARD));
                h.scale(0.65f, 0.65f, 0.65f).glow(180, 220, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 4.5, 0.4, Math.sin(a) * 4.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NAUTILUS_SHELL));
                h.scale(0.55f, 0.55f, 0.55f).glow(200, 230, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 2, 0.3, Math.sin(a) * 2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SEA_PICKLE));
                h.scale(0.5f, 0.5f, 0.5f).glow(120, 200, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 1.5, 0.4, Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.KELP));
                h.scale(0.5f, 1.0f, 0.5f).glow(80, 180, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick <= FALL_TICKS) {
                double t = tick / (double) FALL_TICKS;
                double y = startY * (1.0 - t * t); // accelerate
                // Move bowl
                if (bowl != null) {
                    bowl.animateTo(
                            new Vector3f(-1.75f, (float) y, -1.75f),
                            new AxisAngle4f((float) (tick * 0.04), 0, 1, 0),
                            new Vector3f(3.5f, 3.5f, 3.5f), 1);
                }

                // Orange "fish" orbit inside the bowl during fall
                if (tick % 1 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double a = tick * 0.3 + i * (Math.PI * 2 / 5);
                        double r = 1.1;
                        Location p = c.clone().add(Math.cos(a) * r, y + 1.5, Math.sin(a) * r);
                        DisplayBuilder.dustParticles(p, 1, 0.05, 255, 140, 30, 1.6f);
                        if (i % 2 == 0)
                            w.spawnParticle(Particle.WAX_OFF, p, 1, 0.1, 0.1, 0.1, 0.0);
                    }
                }

                // Whoosh trail
                if (tick % 3 == 0) {
                    Location trailP = c.clone().add(0, y + 1, 0);
                    w.spawnParticle(Particle.CLOUD, trailP, 4, 0.6, 0.2, 0.6, 0.05);
                    DisplayBuilder.dustParticles(trailP, 2, 0.5, 180, 220, 255, 1.0f);
                }

                if (tick == FALL_TICKS - 5) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_BIG_FALL, 1.2f, 0.6f);
                }
            } else if (tick == FALL_TICKS + 1 && !impactFired) {
                impactFired = true;
                Location impact = c.clone();
                triggerImpactDamage(impact);

                // Smash visuals
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.8f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_SPLASH, 1.5f, 0.7f);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.4f, 0.6f);

                w.spawnParticle(Particle.LANDING_HONEY, impact.clone().add(0, 0.5, 0), 30, 1.5, 0.3, 1.5, 0.1);
                w.spawnParticle(Particle.SPLASH, impact.clone().add(0, 0.6, 0), 80, 2.5, 1.0, 2.5, 0.0);
                w.spawnParticle(Particle.CRIT, impact.clone().add(0, 0.6, 0), 60, 2.0, 0.8, 2.0, 0.4);
                w.spawnParticle(Particle.ITEM_SLIME, impact.clone().add(0, 0.6, 0), 20, 1.6, 0.4, 1.6, 0.0);
                for (int i = 0; i < 36; i++) {
                    double a = Math.PI * 2 * i / 36;
                    Location p = impact.clone().add(Math.cos(a) * 4.5, 0.4, Math.sin(a) * 4.5);
                    DisplayBuilder.dustParticles(p, 2, 0.2, 255, 140, 30, 1.6f);
                }

                // Hide the bowl
                if (bowl != null) bowl.animateTo(
                        new Vector3f(-0.001f, 0, -0.001f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.001f, 0.001f, 0.001f), 4);
            } else if (tick > FALL_TICKS + 1) {
                // Lingering wet sparkle
                if (tick % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 4.5;
                        Location p = c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 0.5, Math.sin(a) * r);
                        w.spawnParticle(Particle.FALLING_WATER, p, 1, 0.1, 0.1, 0.1, 0.0);
                        if (Math.random() < 0.3) DisplayBuilder.dustParticles(p, 1, 0.2, 255, 140, 30, 1.2f);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new GoldfishBowlDrop(plugin); }
    }

    // ================================================================
    // 44. CRYSTAL PELLETS — 20 AMETHYST_SHARD ItemDisplays rain in a
    //     tight cluster. Each impacts on a small radius with CRIT trails
    //     and rapid-fire timing.
    // ================================================================
    public static class CrystalPellets extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] sX = new double[20];
        private final double[] sZ = new double[20];
        private final double[] sStartY = new double[20];
        private final int[] sFireTick = new int[20];
        private final boolean[] sFired = new boolean[20];
        private static final int COUNT = 20;

        public CrystalPellets(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_pellets", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(150.0); // 10 hearts each
            config.setImpactRadius(0.8);
            config.setDurationTicks(180);
            config.setCooldownTicks(130);
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.1f, 1.4f);

            for (int i = 0; i < COUNT; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 1.8; // tight cluster
                sX[i] = Math.cos(a) * r;
                sZ[i] = Math.sin(a) * r;
                sStartY[i] = 14.0 + Math.random() * 2.0;
                sFireTick[i] = (int) (Math.random() * 30); // staggered rapid-fire over 30 ticks

                Location p = c.clone().add(sX[i], sStartY[i], sZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                h.scale(0.7f, 1.1f, 0.7f).glow(190, 130, 230).interpolation(0, 0);
                shards.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ground crystal cluster: 6 AMETHYST_CLUSTER + 4 LARGE_AMETHYST_BUD + 4 GLOW_BERRIES around impact zone
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 2, 0.4, Math.sin(a) * 2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_CLUSTER));
                h.scale(0.7f, 0.7f, 0.7f).glow(200, 130, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 3, 0.4, Math.sin(a) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.LARGE_AMETHYST_BUD));
                h.scale(0.55f, 0.55f, 0.55f).glow(180, 110, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 1.0, 0.6, Math.sin(a) * 1.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                h.scale(0.65f, 0.65f, 0.65f).glow(255, 200, 100).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < COUNT; i++) {
                if (sFired[i]) continue;
                int local = tick - sFireTick[i];
                if (local < 0) continue;

                int fall = 18; // very fast
                if (local <= fall) {
                    double t = local / (double) fall;
                    double y = sStartY[i] * (1.0 - t * t);
                    shards.get(i).animateTo(
                            new Vector3f((float) sX[i] - 0.35f, (float) y, (float) sZ[i] - 0.35f),
                            new AxisAngle4f((float) (local * 0.5), 0.3f, 1, 0.2f),
                            new Vector3f(0.7f, 1.1f, 0.7f), 1);
                    // CRIT trail
                    Location tp = c.clone().add(sX[i], y + 0.4, sZ[i]);
                    w.spawnParticle(Particle.CRIT, tp, 1, 0.05, 0.1, 0.05, 0.05);
                    if (local % 2 == 0)
                        DisplayBuilder.dustParticles(tp, 1, 0.1, 200, 130, 240, 1.2f);
                } else {
                    sFired[i] = true;
                    Location impact = c.clone().add(sX[i], 0.4, sZ[i]);
                    triggerImpactDamage(impact);

                    DisplayBuilder.playSound(impact, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.9f, 1.6f);
                    w.spawnParticle(Particle.CRIT, impact.clone().add(0, 0.3, 0), 14, 0.3, 0.2, 0.3, 0.3);
                    w.spawnParticle(Particle.ITEM_SLIME, impact.clone().add(0, 0.3, 0), 4, 0.2, 0.2, 0.2, 0.0);
                    DisplayBuilder.dustParticles(impact, 6, 0.4, 200, 130, 240, 1.4f);

                    // Hide shard
                    shards.get(i).animateTo(
                            new Vector3f(-0.001f, 0, -0.001f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.001f), 2);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CrystalPellets(plugin); }
    }

    // ================================================================
    // 45. TEDDY BEAR DROP — Large CARVED_PUMPKIN ItemDisplay (proxy bear)
    //     drops from Y+18. Shadow DUST ring below. Big THUD + CRIT burst
    //     on impact.
    // ================================================================
    public static class TeddyBearDrop extends EnvironmentalAttack {
        private ItemDisplayHandle bear;
        private boolean impactFired = false;
        private final double startY = 18.0;
        private static final int FALL_TICKS = 55;

        public TeddyBearDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("teddy_bear_drop", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(390.0); // 26 hearts
            config.setImpactRadius(5.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(160);
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_POLAR_BEAR_WARNING, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_PUMPKIN_CARVE, 1.1f, 0.5f);

            Location top = c.clone().add(0, startY, 0);
            bear = displayBuilder.spawnItem(top, new ItemStack(Material.CARVED_PUMPKIN));
            bear.scale(4.0f, 4.0f, 4.0f).glow(180, 130, 90).interpolation(0, 0);
            spawnedEntities.add(bear.entity());

            // Teddy details: 4 BLACK_DYE button eyes + 4 BROWN_WOOL paws + 6 STRING stitches + 4 RABBIT_HIDE fur tufts trailing the bear
            for (int i = 0; i < 4; i++) {
                double yy = startY - 2 - i * 1.2;
                Location p = c.clone().add(-0.6 + (i % 2) * 1.2, yy, -0.6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLACK_DYE));
                h.scale(0.65f, 0.65f, 0.65f).glow(40, 30, 30).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 1.5, startY - 3, Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BROWN_WOOL));
                h.scale(0.7f, 0.7f, 0.7f).glow(140, 100, 70).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                double yy = startY - 1 - i * 0.6;
                Location p = c.clone().add(Math.cos(a) * 0.8, yy, Math.sin(a) * 0.8);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.45f, 0.1f, 0.1f).glow(220, 200, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 2, startY - 5, Math.sin(a) * 2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 130, 90).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick <= FALL_TICKS) {
                double t = tick / (double) FALL_TICKS;
                double y = startY * (1.0 - t * t);
                if (bear != null) {
                    bear.animateTo(
                            new Vector3f(-2.0f, (float) y, -2.0f),
                            new AxisAngle4f((float) (tick * 0.05), 0, 1, 0),
                            new Vector3f(4.0f, 4.0f, 4.0f), 1);
                }

                // Shadow dust ring on the ground beneath
                if (tick % 2 == 0) {
                    double shadowR = 1.5 + (1.0 - (y / startY)) * 3.5;
                    for (int i = 0; i < 36; i++) {
                        double a = Math.PI * 2 * i / 36;
                        Location p = c.clone().add(Math.cos(a) * shadowR, 0.15, Math.sin(a) * shadowR);
                        DisplayBuilder.dustParticles(p, 1, 0.05, 30, 30, 40, 1.5f);
                    }
                }

                // Falling trail — descending dust + smoke
                if (tick % 2 == 0) {
                    Location trailP = c.clone().add(0, y + 1, 0);
                    w.spawnParticle(Particle.LARGE_SMOKE, trailP, 2, 0.6, 0.2, 0.6, 0.02);
                    DisplayBuilder.dustParticles(trailP, 2, 0.6, 180, 130, 90, 1.4f);
                }

                if (tick == FALL_TICKS - 8) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_BIG_FALL, 1.6f, 0.4f);
                }
            } else if (tick == FALL_TICKS + 1 && !impactFired) {
                impactFired = true;
                Location impact = c.clone();
                triggerImpactDamage(impact);

                // THUD
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DEATH, 1.6f, 0.4f);
                DisplayBuilder.playSound(c, Sound.ENTITY_RAVAGER_STEP, 1.5f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_BREAK, 1.2f, 0.4f);

                // CRIT burst + shockwave
                w.spawnParticle(Particle.CRIT, impact.clone().add(0, 0.6, 0), 80, 3.0, 1.0, 3.0, 0.6);
                w.spawnParticle(Particle.LARGE_SMOKE, impact.clone().add(0, 0.5, 0), 60, 3.0, 0.8, 3.0, 0.05);
                w.spawnParticle(Particle.EXPLOSION, impact.clone().add(0, 1.0, 0), 4, 0.6, 0.6, 0.6, 0.0);
                for (int i = 0; i < 48; i++) {
                    double a = Math.PI * 2 * i / 48;
                    Location p = impact.clone().add(Math.cos(a) * 5.0, 0.5, Math.sin(a) * 5.0);
                    DisplayBuilder.dustParticles(p, 2, 0.3, 180, 130, 90, 1.7f);
                    if (i % 4 == 0) w.spawnParticle(Particle.CRIT, p, 4, 0.2, 0.2, 0.2, 0.5);
                }

                if (bear != null) bear.animateTo(
                        new Vector3f(-2.0f, 0.4f, -2.0f),
                        new AxisAngle4f((float) (tick * 0.05), 0, 1, 0),
                        new Vector3f(4.0f, 0.6f, 4.0f), 4);
            } else if (tick > FALL_TICKS + 1) {
                // Aftermath dust
                if (tick % 4 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 4.5;
                        Location p = c.clone().add(Math.cos(a) * r, 0.5, Math.sin(a) * r);
                        DisplayBuilder.dustParticles(p, 1, 0.3, 60, 60, 60, 1.4f);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new TeddyBearDrop(plugin); }
    }

    // ================================================================
    // 46. FURBALL ROLL — Tight gray DUST ball + 8 FEATHER ItemDisplays
    //     spiral around it. Whole thing tracks toward player. Constant
    //     contact damage on small radius.
    // ================================================================
    public static class FurballRoll extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> feathers = new ArrayList<>();
        private double curX;
        private double curZ;
        private static final int FEATHERS = 8;

        public FurballRoll(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("furball_roll", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(165.0); // 11 hearts
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(360);
            config.setCooldownTicks(140);
            config.setTracksPlayer(false); // we track manually for visuals
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_SLIME_SQUISH, 1.0f, 1.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_RABBIT_JUMP, 0.9f, 1.3f);

            // Start at the edge of damage radius
            double a0 = Math.random() * Math.PI * 2;
            curX = Math.cos(a0) * 6.0;
            curZ = Math.sin(a0) * 6.0;

            for (int i = 0; i < FEATHERS; i++) {
                double a = Math.PI * 2 * i / FEATHERS;
                Location p = c.clone().add(curX + Math.cos(a) * 0.9, 0.8, curZ + Math.sin(a) * 0.9);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                h.scale(0.7f, 0.7f, 0.7f).glow(255, 255, 255).interpolation(4, 0);
                feathers.add(h);
                spawnedEntities.add(h.entity());
            }

            // Static fur trail markers along the arena: 8 RABBIT_HIDE + 6 STRING + 4 GRAY_WOOL
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 7, 0.4, Math.sin(a) * 7);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 220, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 6;
                Location p = c.clone().add(Math.cos(a) * 5, 0.5, Math.sin(a) * 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.65f, 0.65f, 0.65f).glow(245, 245, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 3, 0.4, Math.sin(a) * 3);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GRAY_WOOL));
                h.scale(0.55f, 0.55f, 0.55f).glow(170, 170, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Pick nearest player and chase them in horizontal plane
            Player nearest = null;
            double bestSq = Double.MAX_VALUE;
            Location ballLoc = c.clone().add(curX, 0.7, curZ);
            for (Player pl : w.getPlayers()) {
                if (isExempt(pl)) continue;
                double dSq = pl.getLocation().distanceSquared(ballLoc);
                if (dSq < bestSq) { bestSq = dSq; nearest = pl; }
            }

            if (nearest != null) {
                double tx = nearest.getLocation().getX() - c.getX();
                double tz = nearest.getLocation().getZ() - c.getZ();
                double dx = tx - curX;
                double dz = tz - curZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0.001) {
                    double speed = 0.25;
                    curX += (dx / dist) * speed;
                    curZ += (dz / dist) * speed;
                }
            }

            // Update center to ball position so radius damage applies around it
            Location newCenter = c.getWorld().getBlockAt(c.clone().add(curX, 0, curZ).getBlockX(), c.getBlockY(), c.clone().add(curX, 0, curZ).getBlockZ()).getLocation();
            // Use a clean Location based on world coordinates for damage tracking
            Location ballAt = c.clone();
            ballAt.setX(c.getX() + curX);
            ballAt.setZ(c.getZ() + curZ);
            ballAt.setY(c.getY());
            setCenter(ballAt);

            // Render the dust ball
            if (tick % 1 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 0.7;
                    double y = 0.4 + Math.random() * 1.0;
                    Location p = ballAt.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(p, 1, 0.05, 200, 200, 210, 2.0f);
                    if (Math.random() < 0.3)
                        DisplayBuilder.dustParticles(p, 1, 0.05, 245, 245, 250, 2.2f);
                }
            }

            // Spiral feathers around the ball
            if (tick % 2 == 0) {
                for (int i = 0; i < feathers.size(); i++) {
                    double a = Math.PI * 2 * i / feathers.size() + tick * 0.25;
                    float fx = (float) (curX + Math.cos(a) * 0.95);
                    float fz = (float) (curZ + Math.sin(a) * 0.95);
                    float fy = 0.7f + (float) Math.sin(tick * 0.15 + i) * 0.3f;
                    feathers.get(i).animateTo(
                            new Vector3f(fx - 0.35f, fy, fz - 0.35f),
                            new AxisAngle4f((float) (tick * 0.2 + i), 0, 1, 0),
                            new Vector3f(0.7f, 0.7f, 0.7f), 2);
                }
            }

            // Ambient feather puffs
            if (tick % 4 == 0) {
                w.spawnParticle(Particle.CLOUD, ballAt.clone().add(0, 0.7, 0), 4, 0.6, 0.4, 0.6, 0.02);
                w.spawnParticle(Particle.ITEM_SLIME, ballAt.clone().add(0, 0.6, 0), 2, 0.5, 0.3, 0.5, 0.0);
            }

            if (tick % 8 == 0) DisplayBuilder.playSound(ballAt, Sound.ENTITY_SLIME_SQUISH, 0.8f, 1.5f);
        }

        @Override public AbstractAttack newInstance() { return new FurballRoll(plugin); }
    }

    // ================================================================
    // 47. CUDDLE CRASH — 3 pairs of large SLIME_BALL ItemDisplays
    //     approach from opposite sides of the player and clap together.
    //     Impact damage at clap point.
    // ================================================================
    public static class CuddleCrash extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> leftBalls = new ArrayList<>();
        private final List<ItemDisplayHandle> rightBalls = new ArrayList<>();
        private final boolean[] pairFired = new boolean[3];
        private final int[] pairFireTick = new int[]{30, 60, 90};
        private static final int PAIRS = 3;
        private static final double START_OFFSET = 8.0;

        public CuddleCrash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cuddle_crash", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(300.0); // 20 hearts per pair
            config.setImpactRadius(3.5);
            config.setDurationTicks(140);
            config.setCooldownTicks(150);
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_SLIME_JUMP, 1.0f, 0.7f);

            // 3 pairs, axes rotated 60deg apart
            for (int i = 0; i < PAIRS; i++) {
                double angle = Math.PI * (i / (double) PAIRS); // 0, 60deg, 120deg
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);

                Location l = c.clone().add(-dx * START_OFFSET, 1.4, -dz * START_OFFSET);
                Location r = c.clone().add(dx * START_OFFSET, 1.4, dz * START_OFFSET);

                ItemDisplayHandle lh = displayBuilder.spawnItem(l, new ItemStack(Material.SLIME_BALL));
                ItemDisplayHandle rh = displayBuilder.spawnItem(r, new ItemStack(Material.SLIME_BALL));
                lh.scale(2.4f, 2.4f, 2.4f).glow(120, 220, 140).interpolation(4, 0);
                rh.scale(2.4f, 2.4f, 2.4f).glow(120, 220, 140).interpolation(4, 0);
                leftBalls.add(lh);
                rightBalls.add(rh);
                spawnedEntities.add(lh.entity());
                spawnedEntities.add(rh.entity());
            }

            // Cuddle accessories at the meeting point: 6 PINK_PETALS + 4 PINK_WOOL + 4 RABBIT_HIDE + 4 STRING
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 1.5, 0.4, Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PINK_PETALS));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 180, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 2.5, 0.5, Math.sin(a) * 2.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PINK_WOOL));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 180, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 3.5, 0.5, Math.sin(a) * 3.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 200, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 0.8, 1.4, Math.sin(a) * 0.8);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 220, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < PAIRS; i++) {
                if (pairFired[i]) continue;
                int fireT = pairFireTick[i];
                int local = tick - (fireT - 25); // last 25 ticks before clap
                if (local < 0) continue;

                double angle = Math.PI * (i / (double) PAIRS);
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);

                if (tick < fireT) {
                    double t = local / 25.0; // 0..1
                    double dist = START_OFFSET * (1.0 - t);
                    leftBalls.get(i).animateTo(
                            new Vector3f((float) (-dx * dist) - 1.2f, 1.4f, (float) (-dz * dist) - 1.2f),
                            new AxisAngle4f((float) (tick * 0.2), 0, 1, 0),
                            new Vector3f(2.4f, 2.4f, 2.4f), 1);
                    rightBalls.get(i).animateTo(
                            new Vector3f((float) (dx * dist) - 1.2f, 1.4f, (float) (dz * dist) - 1.2f),
                            new AxisAngle4f((float) (-tick * 0.2), 0, 1, 0),
                            new Vector3f(2.4f, 2.4f, 2.4f), 1);

                    // Approach trail
                    Location lp = c.clone().add(-dx * dist, 1.6, -dz * dist);
                    Location rp = c.clone().add(dx * dist, 1.6, dz * dist);
                    DisplayBuilder.dustParticles(lp, 2, 0.3, 130, 230, 150, 1.4f);
                    DisplayBuilder.dustParticles(rp, 2, 0.3, 130, 230, 150, 1.4f);
                    w.spawnParticle(Particle.ITEM_SLIME, lp, 3, 0.4, 0.3, 0.4, 0.0);
                    w.spawnParticle(Particle.ITEM_SLIME, rp, 3, 0.4, 0.3, 0.4, 0.0);
                } else {
                    pairFired[i] = true;
                    Location clap = c.clone().add(0, 1.4, 0);
                    triggerImpactDamage(clap);

                    DisplayBuilder.playSound(c, Sound.ENTITY_SLIME_ATTACK, 1.4f, 0.5f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.6f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_SLIME_DEATH, 1.2f, 0.5f);

                    w.spawnParticle(Particle.CRIT, clap, 50, 1.5, 0.8, 1.5, 0.5);
                    w.spawnParticle(Particle.ITEM_SLIME, clap, 60, 1.8, 1.0, 1.8, 0.0);
                    w.spawnParticle(Particle.EXPLOSION, clap, 2, 0.5, 0.5, 0.5, 0.0);
                    for (int j = 0; j < 24; j++) {
                        double a = Math.PI * 2 * j / 24;
                        Location p = clap.clone().add(Math.cos(a) * 3.5, 0, Math.sin(a) * 3.5);
                        DisplayBuilder.dustParticles(p, 2, 0.2, 130, 230, 150, 1.6f);
                    }

                    // Hide the slimes after the clap
                    leftBalls.get(i).animateTo(
                            new Vector3f(-1.2f, 1.4f, -1.2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.001f), 6);
                    rightBalls.get(i).animateTo(
                            new Vector3f(-1.2f, 1.4f, -1.2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.001f), 6);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CuddleCrash(plugin); }
    }

    // ================================================================
    // 48. PINWHEEL STORM — 8 PAPER ItemDisplays scaled into thin
    //     pinwheel blades spin at Y+1 and move toward the player.
    //     Constant contact damage on small radius.
    // ================================================================
    public static class PinwheelStorm extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> blades = new ArrayList<>();
        private double curX;
        private double curZ;
        private static final int BLADES = 8;

        public PinwheelStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pinwheel_storm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(150.0); // 10 hearts
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(320);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 0.9f, 1.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BAMBOO_HIT, 1.0f, 1.6f);

            double a0 = Math.random() * Math.PI * 2;
            curX = Math.cos(a0) * 7.0;
            curZ = Math.sin(a0) * 7.0;

            for (int i = 0; i < BLADES; i++) {
                double a = Math.PI * 2 * i / BLADES;
                Location p = c.clone().add(curX + Math.cos(a) * 1.4, 1.0, curZ + Math.sin(a) * 1.4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PAPER));
                // Thin pinwheel blade: long + flat
                h.scale(1.8f, 0.05f, 0.4f).glow(255, 220, 140).interpolation(2, 0);
                blades.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 colored PINK_PETALS swirling above + 6 FEATHER + 4 STRING streamers in the wind path
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(curX + Math.cos(a) * 2.4, 2.5, curZ + Math.sin(a) * 2.4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PINK_PETALS));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 200, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 6;
                Location p = c.clone().add(curX + Math.cos(a) * 1.8, 1.6, curZ + Math.sin(a) * 1.8);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 240, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(curX + Math.cos(a) * 1.2, 0.5, curZ + Math.sin(a) * 1.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.6f, 0.1f, 0.1f).glow(255, 230, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Setpiece pinwheels: stay at spawn (no per-tick player tracking).
            Location bladeCenter = c.clone();
            bladeCenter.setX(c.getX() + curX);
            bladeCenter.setZ(c.getZ() + curZ);
            bladeCenter.setY(c.getY() + 1.0);

            // Spin blades
            if (tick % 1 == 0) {
                double spin = tick * 0.6;
                for (int i = 0; i < blades.size(); i++) {
                    double a = Math.PI * 2 * i / blades.size() + spin;
                    float bx = (float) (curX + Math.cos(a) * 1.5);
                    float bz = (float) (curZ + Math.sin(a) * 1.5);
                    blades.get(i).animateTo(
                            new Vector3f(bx - 0.9f, 1.0f, bz - 0.2f),
                            new AxisAngle4f((float) (a + Math.PI / 2), 0, 1, 0),
                            new Vector3f(1.8f, 0.05f, 0.4f), 1);
                }
            }

            // CRIT from blade tips
            if (tick % 2 == 0) {
                double spin = tick * 0.6;
                for (int i = 0; i < BLADES; i++) {
                    double a = Math.PI * 2 * i / BLADES + spin;
                    Location tip = c.clone().add(curX + Math.cos(a) * 2.4, 1.4, curZ + Math.sin(a) * 2.4);
                    w.spawnParticle(Particle.CRIT, tip, 2, 0.1, 0.1, 0.1, 0.2);
                    DisplayBuilder.dustParticles(tip, 1, 0.1, 255, 220, 140, 1.4f);
                }
            }

            // Wind whoosh
            if (tick % 6 == 0) {
                w.spawnParticle(Particle.CLOUD, bladeCenter, 5, 1.4, 0.4, 1.4, 0.05);
            }

            if (tick % 10 == 0) DisplayBuilder.playSound(bladeCenter, Sound.ITEM_ELYTRA_FLYING, 0.5f, 1.6f);
        }

        @Override public AbstractAttack newInstance() { return new PinwheelStorm(plugin); }
    }

    // ================================================================
    // 49. FURBALL AVALANCHE — 20 WHITE_WOOL ItemDisplays roll from the
    //     arena edge tumbling inward. Each impacts on small radius.
    // ================================================================
    public static class FurballAvalanche extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> balls = new ArrayList<>();
        private final double[] bAng = new double[20];
        private final double[] bSpeed = new double[20];
        private final int[] bSpawnTick = new int[20];
        private final boolean[] bFired = new boolean[20];
        private final boolean[] bSpawned = new boolean[20];
        private static final int COUNT = 20;
        private static final double EDGE_R = 8.5;

        public FurballAvalanche(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("furball_avalanche", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(150.0); // 10 hearts each
            config.setImpactRadius(1.2);
            config.setDurationTicks(220);
            config.setCooldownTicks(150);
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_WOOL_PLACE, 1.2f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 1.0f, 0.7f);

            for (int i = 0; i < COUNT; i++) {
                bAng[i] = Math.random() * Math.PI * 2;
                bSpeed[i] = 0.18 + Math.random() * 0.10;
                bSpawnTick[i] = (int) (Math.random() * 60); // staggered spawn
                bSpawned[i] = false;
            }
            // Pre-create handles tucked at start positions but invisible scale
            for (int i = 0; i < COUNT; i++) {
                Location p = c.clone().add(Math.cos(bAng[i]) * EDGE_R, 0.7, Math.sin(bAng[i]) * EDGE_R);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WHITE_WOOL));
                h.scale(0.001f, 0.001f, 0.001f).glow(255, 255, 255).interpolation(4, 0);
                balls.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < COUNT; i++) {
                if (bFired[i]) continue;
                if (tick < bSpawnTick[i]) continue;

                int local = tick - bSpawnTick[i];

                if (!bSpawned[i]) {
                    bSpawned[i] = true;
                    balls.get(i).animateTo(
                            new Vector3f((float) (Math.cos(bAng[i]) * EDGE_R) - 0.45f, 0.7f, (float) (Math.sin(bAng[i]) * EDGE_R) - 0.45f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.9f), 4);
                }

                // Roll inward
                double traveled = local * bSpeed[i];
                if (traveled >= EDGE_R) {
                    // Reached center -> impact
                    bFired[i] = true;
                    Location impact = c.clone();
                    triggerImpactDamage(impact);

                    DisplayBuilder.playSound(impact, Sound.BLOCK_WOOL_BREAK, 0.8f, 1.0f);
                    w.spawnParticle(Particle.CRIT, impact.clone().add(0, 0.5, 0), 12, 0.4, 0.3, 0.4, 0.3);
                    w.spawnParticle(Particle.SNOWFLAKE, impact.clone().add(0, 0.5, 0), 14, 0.5, 0.4, 0.5, 0.05);
                    DisplayBuilder.dustParticles(impact.clone().add(0, 0.5, 0), 8, 0.4, 255, 255, 255, 1.5f);

                    // Hide
                    balls.get(i).animateTo(
                            new Vector3f(-0.001f, 0.7f, -0.001f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.001f), 4);
                } else {
                    double r = EDGE_R - traveled;
                    float bx = (float) (Math.cos(bAng[i]) * r);
                    float bz = (float) (Math.sin(bAng[i]) * r);
                    balls.get(i).animateTo(
                            new Vector3f(bx - 0.45f, 0.7f, bz - 0.45f),
                            new AxisAngle4f((float) (local * 0.4), -(float) Math.sin(bAng[i]), 0, (float) Math.cos(bAng[i])),
                            new Vector3f(0.9f, 0.9f, 0.9f), 1);

                    // Tumble particles
                    Location bp = c.clone().add(bx, 0.7, bz);
                    if (local % 2 == 0) {
                        w.spawnParticle(Particle.SNOWFLAKE, bp, 1, 0.2, 0.1, 0.2, 0.0);
                        w.spawnParticle(Particle.CRIT, bp, 1, 0.2, 0.1, 0.2, 0.1);
                    }
                    if (local % 3 == 0) DisplayBuilder.dustParticles(bp, 1, 0.2, 255, 255, 255, 1.4f);
                }
            }

            if (tick % 20 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_WOOL_STEP, 0.8f, 1.0f);
        }

        @Override public AbstractAttack newInstance() { return new FurballAvalanche(plugin); }
    }

    // ================================================================
    // 50. COZY CHAOS — Combined: 10 FEATHER fall + 5 COOKIE arc + 6
    //     pawprint stamps (BROWN_DYE proxy ItemDisplays) + ENCHANT rings
    //     everywhere. Mixed continuous + impact damage on a wide zone.
    // ================================================================
    public static class CozyChaos extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> feathers = new ArrayList<>();
        private final List<ItemDisplayHandle> cookies = new ArrayList<>();
        private final List<ItemDisplayHandle> pawprints = new ArrayList<>();
        private final double[] fX = new double[10];
        private final double[] fZ = new double[10];
        private final double[] fStartY = new double[10];
        private final double[] cX = new double[5];
        private final double[] cZ = new double[5];
        private final int[] cFireTick = new int[5];
        private final boolean[] cFired = new boolean[5];
        private static final int FEATHERS = 10;
        private static final int COOKIES = 5;
        private static final int PAWS = 6;

        public CozyChaos(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cozy_chaos", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            // Continuous zone damage
            config.setDamage(105.0); // 7 hearts
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(10);
            // Cookies fire impact pings on top of constant zone
            config.setImpactDamage(105.0); // 7 hearts
            config.setImpactRadius(2.5);
            config.setDurationTicks(420);
            config.setCooldownTicks(190);
        }

        @Override
        protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.MUSIC_DISC_CAT, 0.0001f, 0.0f); // marker / no-op volume
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.9f, 1.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 1.0f, 1.2f);

            // 10 feathers slow-falling from above
            for (int i = 0; i < FEATHERS; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 6.0;
                fX[i] = Math.cos(a) * r;
                fZ[i] = Math.sin(a) * r;
                fStartY[i] = 12.0 + Math.random() * 4.0;
                Location p = c.clone().add(fX[i], fStartY[i], fZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.FEATHER));
                h.scale(0.9f, 0.9f, 0.9f).glow(255, 255, 255).interpolation(0, 0);
                feathers.add(h);
                spawnedEntities.add(h.entity());
            }

            // 5 cookies on staggered arcs
            for (int i = 0; i < COOKIES; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 5.5 + Math.random() * 1.5;
                cX[i] = Math.cos(a) * r;
                cZ[i] = Math.sin(a) * r;
                cFireTick[i] = 40 + i * 30;
                Location p = c.clone().add(cX[i], 6.0, cZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.COOKIE));
                h.scale(1.4f, 1.4f, 1.4f).glow(220, 170, 90).interpolation(0, 0);
                cookies.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 pawprint stamps around the ring (proxy: BROWN_DYE)
            for (int i = 0; i < PAWS; i++) {
                double a = Math.PI * 2 * i / PAWS + Math.random() * 0.3;
                double r = 4.0 + Math.random() * 1.5;
                Location p = c.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BROWN_DYE));
                h.scale(0.9f, 0.05f, 0.9f).glow(120, 80, 50).interpolation(20, 0);
                pawprints.add(h);
                spawnedEntities.add(h.entity());
            }

            // Cozy finale assortment: 6 STRING + 4 RABBIT_HIDE + 4 HONEY_BOTTLE + 4 PINK_PETALS + 4 SUGAR + 2 CAKE
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 5.5, 0.5, Math.sin(a) * 5.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.STRING));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 220, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 2.5, 0.5, Math.sin(a) * 2.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.RABBIT_HIDE));
                h.scale(0.55f, 0.55f, 0.55f).glow(220, 200, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 3.5, 0.5, Math.sin(a) * 3.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.HONEY_BOTTLE));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 200, 60).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 1.5, 0.5, Math.sin(a) * 1.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PINK_PETALS));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 180, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 8;
                Location p = c.clone().add(Math.cos(a) * 6.5, 0.5, Math.sin(a) * 6.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SUGAR));
                h.scale(0.6f, 0.6f, 0.6f).glow(255, 250, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 2; i++) {
                double a = i * Math.PI;
                Location p = c.clone().add(Math.cos(a) * 4.5, 0.5, Math.sin(a) * 4.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.CAKE));
                h.scale(0.7f, 0.7f, 0.7f).glow(255, 220, 200).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            double radius = config.getDamageRadius();

            // Feathers slow-fall and rotate
            if (tick % 2 == 0) {
                for (int i = 0; i < feathers.size(); i++) {
                    double y = Math.max(0.5, fStartY[i] - tick * 0.12);
                    feathers.get(i).animateTo(
                            new Vector3f((float) fX[i] - 0.45f, (float) y, (float) fZ[i] - 0.45f),
                            new AxisAngle4f((float) (tick * 0.15 + i), 0, 1, 0),
                            new Vector3f(0.9f, 0.9f, 0.9f), 2);
                    // gentle drift so the column doesn't stay stuck to one spot
                    fX[i] += (Math.random() - 0.5) * 0.05;
                    fZ[i] += (Math.random() - 0.5) * 0.05;
                }
            }

            // Cookie arcs fire one by one
            for (int i = 0; i < COOKIES; i++) {
                if (cFired[i]) continue;
                int fireT = cFireTick[i];
                int local = tick - (fireT - 25);
                if (local < 0) continue;

                if (tick < fireT) {
                    double t = local / 25.0;
                    double y = 6.0 - 5.5 * (t * t);
                    cookies.get(i).animateTo(
                            new Vector3f((float) cX[i] - 0.7f, (float) y, (float) cZ[i] - 0.7f),
                            new AxisAngle4f((float) (tick * 0.3), 0, 1, 0),
                            new Vector3f(1.4f, 1.4f, 1.4f), 1);
                    Location tp = c.clone().add(cX[i], y + 0.5, cZ[i]);
                    DisplayBuilder.dustParticles(tp, 2, 0.2, 220, 170, 90, 1.4f);
                    w.spawnParticle(Particle.CRIT, tp, 1, 0.15, 0.15, 0.15, 0.2);
                } else {
                    cFired[i] = true;
                    Location impact = c.clone().add(cX[i], 0.5, cZ[i]);
                    triggerImpactDamage(impact);

                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EAT, 1.2f, 0.7f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_PLAYER_BURP, 0.8f, 0.6f);
                    w.spawnParticle(Particle.CRIT, impact.clone().add(0, 0.5, 0), 24, 1.2, 0.6, 1.2, 0.4);
                    w.spawnParticle(Particle.ITEM, impact.clone().add(0, 0.6, 0), 16, 1.0, 0.5, 1.0, 0.0,
                            new ItemStack(Material.COOKIE));
                    for (int j = 0; j < 18; j++) {
                        double a = Math.PI * 2 * j / 18;
                        Location p = impact.clone().add(Math.cos(a) * 2.5, 0.2, Math.sin(a) * 2.5);
                        DisplayBuilder.dustParticles(p, 1, 0.2, 220, 170, 90, 1.5f);
                    }

                    cookies.get(i).animateTo(
                            new Vector3f((float) cX[i] - 0.7f, 0.4f, (float) cZ[i] - 0.7f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.001f), 4);
                }
            }

            // Pawprint flicker animation
            if (tick % 30 == 0) {
                for (int i = 0; i < pawprints.size(); i++) {
                    double a = Math.PI * 2 * i / PAWS + tick * 0.02;
                    double r = 4.0 + Math.sin(tick * 0.04 + i) * 0.6;
                    float fx = (float) (Math.cos(a) * r);
                    float fz = (float) (Math.sin(a) * r);
                    pawprints.get(i).animateTo(
                            new Vector3f(fx - 0.45f, 0.15f, fz - 0.45f),
                            new AxisAngle4f((float) a, 0, 1, 0),
                            new Vector3f(0.9f, 0.05f, 0.9f), 28);
                }
            }

            // ENCHANT rings everywhere
            if (tick % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * radius;
                    Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 4.0, Math.sin(a) * r);
                    w.spawnParticle(Particle.ENCHANT, p, 2, 0.4, 0.5, 0.4, 0.6);
                }
            }

            // Cozy fluff ambient
            if (tick % 4 == 0) {
                for (int i = 0; i < 4; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * radius;
                    Location p = c.clone().add(Math.cos(a) * r, 0.6 + Math.random() * 3.0, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(p, 1, 0.3, 255, 220, 200, 1.3f);
                    if (Math.random() < 0.3) w.spawnParticle(Particle.CLOUD, p, 1, 0.2, 0.2, 0.2, 0.0);
                }
            }

            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_CAT_PURR, 0.8f, 1.1f);
            if (tick % 110 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.5f);
        }

        @Override public AbstractAttack newInstance() { return new CozyChaos(plugin); }
    }
}
