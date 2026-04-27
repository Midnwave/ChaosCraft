package com.blockforge.chaoscraft.modes.devilsdream.attacks;

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
 * DevilsDream Mode — ENVIRONMENTAL FX BATCH 2 (entries 11-20).
 * Pure particle + ItemDisplay attacks. NO BlockDisplays.
 * Each attack is a particle system with item-display accents,
 * unique animation patterns, and layered sound design.
 * Multi-phase lifecycle: spawn -> active -> dissipate.
 */
public final class DDEnvFX2 {
    private DDEnvFX2() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WeepingIcon(plugin));
        registry.register(new MeteorGraveyard(plugin));
        registry.register(new StormEye(plugin));
        registry.register(new InfernalMarchingBand(plugin));
        registry.register(new EclipseEvent(plugin));
        registry.register(new DissectionTable(plugin));
        registry.register(new CosmicTear(plugin));
        registry.register(new ProphetsThrone(plugin));
        registry.register(new RainOfEyes(plugin));
        registry.register(new Ossuary(plugin));
    }

    // ================================================================
    // 11. THE WEEPING ICON — Tall stylized religious figure that weeps
    //     purple tears. Skull head, bone-spike body, soul lantern halo,
    //     dripping particle tears, soul-flame ring at the feet.
    // ================================================================
    public static class WeepingIcon extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> body = new ArrayList<>();
        private final List<ItemDisplayHandle> halo = new ArrayList<>();
        private ItemDisplayHandle skull;
        private ItemDisplayHandle heart;

        public WeepingIcon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("weeping_icon", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5); config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(40); config.setDamageDelayTicks(40);
            config.setDurationTicks(700); config.setCooldownTicks(420);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.ENTITY_GHAST_WARN, 0.6f, 0.3f);
            DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.9f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.8f, 0.5f);

            // Spine — 8 vertical bone segments
            for (int y = 0; y < 8; y++) {
                Location p = c.clone().add(0, 0.5 + y * 0.7, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BONE));
                h.scale(0.5f, 0.7f, 0.5f).glow(220, 210, 190).interpolation(40, y * 2);
                h.animateTo(new Vector3f(-0.25f, 0.5f + y * 0.7f, -0.25f),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.5f, 0.7f, 0.5f), 40);
                body.add(h);
            }

            // Skull head
            skull = displayBuilder.spawnItem(c.clone().add(0, 6.4, 0),
                    new ItemStack(Material.SKELETON_SKULL));
            skull.scale(1.2f, 1.2f, 1.2f).glow(230, 220, 200).interpolation(40, 20);

            // Outstretched arms — bone-meal segments
            for (int i = 1; i <= 5; i++) {
                Location lp = c.clone().add(-i * 0.55, 5.0 - i * 0.08, 0);
                Location rp = c.clone().add(i * 0.55, 5.0 - i * 0.08, 0);
                ItemDisplayHandle l = displayBuilder.spawnItem(lp, new ItemStack(Material.BONE));
                ItemDisplayHandle r = displayBuilder.spawnItem(rp, new ItemStack(Material.BONE));
                l.scale(0.4f, 0.3f, 0.3f).glow(210, 200, 180).interpolation(30, 30 + i);
                r.scale(0.4f, 0.3f, 0.3f).glow(210, 200, 180).interpolation(30, 30 + i);
                body.add(l); body.add(r);
            }

            // Soul lantern halo above head
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location p = c.clone().add(Math.cos(a) * 1.2, 7.4, Math.sin(a) * 1.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.4f, 0.4f, 0.4f).glow(140, 80, 220).interpolation(20, 40);
                halo.add(h);
            }

            // Glowing nether star at chest (the "heart")
            heart = displayBuilder.spawnItem(c.clone().add(0, 4.5, 0),
                    new ItemStack(Material.NETHER_STAR));
            heart.scale(0.7f, 0.7f, 0.7f).glow(180, 60, 220).interpolation(30, 30);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Skull subtle bob + tilt
            if (tick % 6 == 0 && skull != null) {
                float ty = 6.4f + (float)Math.sin(tick * 0.04) * 0.1f;
                skull.animateTo(new Vector3f(-0.6f, ty, -0.6f),
                        new AxisAngle4f((float)(Math.sin(tick * 0.03) * 0.2), 0, 1, 0),
                        new Vector3f(1.2f), 6);
            }
            // Heart pulses
            if (tick % 4 == 0 && heart != null) {
                float s = 0.6f + (float)Math.abs(Math.sin(tick * 0.15)) * 0.25f;
                heart.animateTo(new Vector3f(-s/2f, 4.5f, -s/2f),
                        new AxisAngle4f((float)(tick * 0.05), 0, 1, 0), new Vector3f(s), 4);
            }
            // Halo rotates
            if (tick % 3 == 0) {
                for (int i = 0; i < halo.size(); i++) {
                    double a = Math.PI * 2 * i / halo.size() + tick * 0.04;
                    float ty = 7.4f + (float)Math.sin(tick * 0.06 + i) * 0.15f;
                    halo.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * 1.2) - 0.2f, ty, (float)(Math.sin(a) * 1.2) - 0.2f),
                            new AxisAngle4f((float)(tick * 0.08), 0, 1, 0), new Vector3f(0.4f), 3);
                }
            }
            // Purple tear streams from eyes (skull at y=6.6)
            if (tick % 1 == 0) for (int eye = -1; eye <= 1; eye += 2) {
                Location p = c.clone().add(eye * 0.25, 6.6, 0.4);
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, p, 1, 0.05, 0.05, 0.05, 0);
                DisplayBuilder.dustParticles(p.clone().add(0, -Math.random() * 4, 0), 1, 0.05, 128, 48, 192, 0.9f);
                if (Math.random() < 0.3)
                    w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, p.clone().add(0, -Math.random() * 5, 0), 1, 0.05, 0.05, 0.05, 0);
            }
            // Soul fire ring at base
            if (tick % 2 == 0) for (int i = 0; i < 18; i++) {
                double a = Math.PI * 2 * i / 18 + tick * 0.05;
                Location p = c.clone().add(Math.cos(a) * 2.2, 0.2, Math.sin(a) * 2.2);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.04, 0.06, 0.04, 0);
                if (Math.random() < 0.15) w.spawnParticle(Particle.SOUL, p, 1, 0.1, 0.4, 0.1, 0.02);
            }
            // Halo enchant sparkle
            if (tick % 3 == 0) for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2, r = 1.0 + Math.random() * 0.3;
                w.spawnParticle(Particle.ENCHANT, c.clone().add(Math.cos(a) * r, 7.5 + Math.random() * 0.4, Math.sin(a) * r), 1, 0.05, 0.05, 0.05, 0);
            }
            // Body smoke aura
            if (tick % 4 == 0) for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2;
                w.spawnParticle(Particle.LARGE_SMOKE, c.clone().add(Math.cos(a) * 0.6, Math.random() * 6, Math.sin(a) * 0.6), 1, 0.1, 0.1, 0.1, 0.005);
            }
            // Periodic resonant bell
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BELL_RESONATE, 0.7f, 0.4f);
            if (tick % 120 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_GHAST_AMBIENT, 0.5f, 0.4f);

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new WeepingIcon(plugin); }
    }

    // ================================================================
    // 12. METEOR GRAVEYARD — Multiple comet trails crash down at random
    //     spots. Each impact leaves a smoldering ItemDisplay tombstone
    //     (skull on basalt cluster) with smoke + ember particles.
    // ================================================================
    public static class MeteorGraveyard extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> tombstones = new ArrayList<>();
        private final List<ItemDisplayHandle> meteors = new ArrayList<>();
        private final double[] meteorY = new double[8];
        private final double[] meteorAng = new double[8];
        private final double[] meteorR = new double[8];

        public MeteorGraveyard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("meteor_graveyard", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0); config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(20); config.setDurationTicks(400); config.setCooldownTicks(360);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SHOOT, 1.4f, 0.3f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.0f, 0.5f);
            for (int i = 0; i < 8; i++) {
                meteorAng[i] = Math.random() * Math.PI * 2;
                meteorR[i] = 1 + Math.random() * 6.5;
                meteorY[i] = 12 + Math.random() * 4 + i * 1.5;
                Location p = c.clone().add(Math.cos(meteorAng[i]) * meteorR[i], meteorY[i], Math.sin(meteorAng[i]) * meteorR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.MAGMA_CREAM));
                h.scale(0.7f, 0.7f, 0.7f).glow(255, 90, 20).interpolation(2, 0);
                meteors.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            // Meteor descent with comet trails
            for (int i = 0; i < meteors.size(); i++) {
                meteorY[i] -= 0.32 + (i % 3) * 0.05;
                Location pos = c.clone().add(Math.cos(meteorAng[i]) * meteorR[i], meteorY[i], Math.sin(meteorAng[i]) * meteorR[i]);
                meteors.get(i).animateTo(
                        new Vector3f((float)(Math.cos(meteorAng[i]) * meteorR[i]) - 0.35f, (float)meteorY[i], (float)(Math.sin(meteorAng[i]) * meteorR[i]) - 0.35f),
                        new AxisAngle4f((float)(tick * 0.3 + i), 1, 1, 0), new Vector3f(0.7f), 2);

                // Trail
                w.spawnParticle(Particle.FLAME, pos, 4, 0.15, 0.15, 0.15, 0.02);
                w.spawnParticle(Particle.LARGE_SMOKE, pos, 2, 0.2, 0.2, 0.2, 0.01);
                w.spawnParticle(Particle.LAVA, pos, 1, 0.1, 0.1, 0.1, 0);

                if (meteorY[i] < 0.6) {
                    // Impact — create a tombstone here
                    Location impact = c.clone().add(Math.cos(meteorAng[i]) * meteorR[i], 0.4, Math.sin(meteorAng[i]) * meteorR[i]);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.4f);
                    w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.LAVA, impact, 8, 0.3, 0.2, 0.3, 0);

                    if (tombstones.size() < 14) {
                        // Tombstone stack
                        ItemDisplayHandle base = displayBuilder.spawnItem(impact, new ItemStack(Material.BLACKSTONE));
                        base.scale(0.9f, 0.4f, 0.9f).glow(60, 30, 20).interpolation(20, 0);
                        base.animateTo(new Vector3f(-0.45f, 0.0f, -0.45f), new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.9f, 0.4f, 0.9f), 20);
                        tombstones.add(base);

                        ItemDisplayHandle slab = displayBuilder.spawnItem(impact.clone().add(0, 0.55, 0),
                                new ItemStack(Material.COBBLED_DEEPSLATE));
                        slab.scale(0.6f, 1.1f, 0.18f).glow(80, 60, 60).interpolation(20, 5);
                        slab.animateTo(new Vector3f(-0.3f, 0.55f, -0.09f), new AxisAngle4f((float)meteorAng[i], 0, 1, 0), new Vector3f(0.6f, 1.1f, 0.18f), 20);
                        tombstones.add(slab);

                        ItemDisplayHandle skull = displayBuilder.spawnItem(impact.clone().add(0, 1.5, 0),
                                new ItemStack(Material.SKELETON_SKULL));
                        skull.scale(0.55f, 0.55f, 0.55f).glow(220, 200, 180).interpolation(20, 10);
                        skull.animateTo(new Vector3f(-0.275f, 1.5f, -0.275f), new AxisAngle4f((float)meteorAng[i], 0, 1, 0), new Vector3f(0.55f), 20);
                        tombstones.add(skull);
                    }

                    // Reset meteor
                    meteorAng[i] = Math.random() * Math.PI * 2;
                    meteorR[i] = 1 + Math.random() * 6.5;
                    meteorY[i] = 12 + Math.random() * 4;

                    // Damage on impact
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(impact) < 5) { p.damage(config.getDamage()); p.setNoDamageTicks(0); p.setFireTicks(60); }
                    }
                }
            }

            // Smoldering smoke at each tombstone
            if (tick % 4 == 0) for (int i = 0; i < tombstones.size(); i += 3) {
                Location p = tombstones.get(i).entity().getLocation();
                w.spawnParticle(Particle.LARGE_SMOKE, p.clone().add(0, 1.0, 0), 1, 0.2, 0.4, 0.2, 0.01);
                if (Math.random() < 0.4) w.spawnParticle(Particle.SOUL_FIRE_FLAME, p.clone().add(0, 0.5, 0), 1, 0.15, 0.05, 0.15, 0.005);
            }
            // Ash drifting
            if (tick % 3 == 0) for (int i = 0; i < 8; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 7;
                w.spawnParticle(Particle.ASH, c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 4, Math.sin(a) * r), 1, 0.2, 0.2, 0.2, 0.01);
            }

            if (tick % 100 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_NETHER_WASTES_LOOP, 0.7f, 0.3f);
        }

        @Override public AbstractAttack newInstance() { return new MeteorGraveyard(plugin); }
    }

    // ================================================================
    // 13. STORM EYE — Calm eye of a storm hovers above. Rotating particle
    //     ring with ItemDisplay debris (sticks/feathers) cycling around.
    //     Lightning particles inward, pull effect at edge.
    // ================================================================
    public static class StormEye extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> debris = new ArrayList<>();
        private final double[] debrisAng = new double[16];
        private final double[] debrisR = new double[16];
        private final double[] debrisY = new double[16];

        public StormEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("storm_eye", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.5); config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20); config.setDurationTicks(420); config.setCooldownTicks(380);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 0.4f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.4f);
            Material[] mats = {Material.STICK, Material.FEATHER, Material.LEATHER, Material.OAK_SAPLING, Material.STRING, Material.HAY_BLOCK};
            for (int i = 0; i < 16; i++) {
                debrisAng[i] = Math.PI * 2 * i / 16;
                debrisR[i] = 5.5 + Math.random() * 1.5;
                debrisY[i] = 5 + Math.random() * 2;
                Location p = c.clone().add(Math.cos(debrisAng[i]) * debrisR[i], debrisY[i], Math.sin(debrisAng[i]) * debrisR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(mats[i % mats.length]));
                h.scale(0.4f + (float)Math.random() * 0.3f, 0.4f, 0.4f).glow(180, 180, 200).interpolation(4, 0);
                debris.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Debris orbits the eye, fast outer rotation
            for (int i = 0; i < debris.size(); i++) {
                debrisAng[i] += 0.15 + (i % 3) * 0.02;
                debrisR[i] = 5.0 + Math.sin(tick * 0.04 + i) * 1.2;
                debrisY[i] = 5.0 + Math.sin(tick * 0.06 + i * 0.7) * 0.6;
                float tx = (float)(Math.cos(debrisAng[i]) * debrisR[i]);
                float tz = (float)(Math.sin(debrisAng[i]) * debrisR[i]);
                debris.get(i).animateTo(new Vector3f(tx - 0.25f, (float)debrisY[i], tz - 0.25f),
                        new AxisAngle4f((float)(tick * 0.4 + i), 1, 1, 1), new Vector3f(0.5f), 2);
            }

            // Calm center — soft glow
            if (tick % 3 == 0) for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 0.8;
                w.spawnParticle(Particle.END_ROD, c.clone().add(Math.cos(a) * r, 5.0 + Math.random() * 0.6, Math.sin(a) * r), 1, 0.05, 0.05, 0.05, 0);
            }

            // Storm wall — concentric rotating particle rings
            if (tick % 1 == 0) for (int ring = 0; ring < 3; ring++) {
                double rr = 4.0 + ring * 0.8;
                int count = 14 + ring * 4;
                for (int i = 0; i < count; i++) {
                    double a = Math.PI * 2 * i / count + tick * (0.06 + ring * 0.02);
                    Location p = c.clone().add(Math.cos(a) * rr, 4.5 + Math.sin(tick * 0.1 + i) * 1.0, Math.sin(a) * rr);
                    DisplayBuilder.dustParticles(p, 1, 0.05, 80, 80, 100, 0.9f);
                    if (ring == 2 && Math.random() < 0.05) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0);
                }
            }

            // Cloud ash above
            if (tick % 2 == 0) for (int i = 0; i < 10; i++) {
                double a = Math.random() * Math.PI * 2, r = 2 + Math.random() * 5;
                w.spawnParticle(Particle.CLOUD, c.clone().add(Math.cos(a) * r, 7 + Math.random() * 1.5, Math.sin(a) * r), 1, 0.2, 0.2, 0.2, 0.01);
            }

            // Random lightning crack inward
            if (tick % 22 == 0 && Math.random() < 0.7) {
                double a = Math.random() * Math.PI * 2;
                for (int seg = 0; seg < 12; seg++) {
                    Location p = c.clone().add(Math.cos(a) * (5 - seg * 0.4) + (Math.random() - 0.5) * 0.4,
                            5.0 + (Math.random() - 0.5) * 0.4,
                            Math.sin(a) * (5 - seg * 0.4) + (Math.random() - 0.5) * 0.4);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.05, 0.05, 0.05, 0.05);
                }
                DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.0f + (float)Math.random() * 0.5f);
            }

            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 1.0f, 0.5f);
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                double inner2 = 3.5 * 3.5;
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    double d2 = p.getLocation().distanceSquared(c);
                    if (d2 <= r2 && d2 >= inner2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new StormEye(plugin); }
    }

    // ================================================================
    // 14. INFERNAL MARCHING BAND — Ghostly procession of drums and horns
    //     cycling in a parade pattern around the center, with rhythmic
    //     percussion sound effects and trailing soul flames.
    // ================================================================
    public static class InfernalMarchingBand extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> band = new ArrayList<>();
        private final Material[] instruments = {
                Material.NOTE_BLOCK, Material.GOAT_HORN, Material.BELL, Material.NOTE_BLOCK,
                Material.GOAT_HORN, Material.NOTE_BLOCK, Material.BELL, Material.GOAT_HORN
        };

        public InfernalMarchingBand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_marching_band", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.5); config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(25); config.setDurationTicks(440); config.setCooldownTicks(380);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.EVENT_RAID_HORN, 1.4f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.4f, 0.5f);
            for (int i = 0; i < instruments.length; i++) {
                double a = Math.PI * 2 * i / instruments.length;
                Location p = c.clone().add(Math.cos(a) * 4, 1.2, Math.sin(a) * 4);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(instruments[i]));
                h.scale(0.7f, 0.7f, 0.7f).glow(220, 100, 30).interpolation(20, i * 2);
                band.add(h);

                // Each band member also gets a "cap" item floating above (skull)
                ItemDisplayHandle cap = displayBuilder.spawnItem(p.clone().add(0, 1.0, 0), new ItemStack(Material.SKELETON_SKULL));
                cap.scale(0.4f, 0.4f, 0.4f).glow(200, 180, 150).interpolation(20, i * 2);
                band.add(cap);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Procession marches in circle
            int count = instruments.length;
            for (int i = 0; i < count; i++) {
                double a = Math.PI * 2 * i / count + tick * 0.025;
                double r = 4.0 + Math.sin(tick * 0.08 + i) * 0.2;
                // March bounce
                float marchBounce = (float)Math.abs(Math.sin(tick * 0.2 + i * 0.6)) * 0.35f;
                float ty = 1.0f + marchBounce;
                ItemDisplayHandle inst = band.get(i * 2);
                inst.animateTo(
                        new Vector3f((float)(Math.cos(a) * r) - 0.35f, ty, (float)(Math.sin(a) * r) - 0.35f),
                        new AxisAngle4f((float)(a + Math.PI / 2), 0, 1, 0), new Vector3f(0.7f), 4);

                ItemDisplayHandle cap = band.get(i * 2 + 1);
                float capY = ty + 1.1f + (float)Math.sin(tick * 0.15 + i) * 0.1f;
                cap.animateTo(
                        new Vector3f((float)(Math.cos(a) * r) - 0.2f, capY, (float)(Math.sin(a) * r) - 0.2f),
                        new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0), new Vector3f(0.4f), 4);
            }

            // Soul fire trail behind each
            if (tick % 2 == 0) for (int i = 0; i < count; i++) {
                double a = Math.PI * 2 * i / count + tick * 0.025 - 0.2;
                Location p = c.clone().add(Math.cos(a) * 4.0, 1.0, Math.sin(a) * 4.0);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.1, 0.1, 0.1, 0);
                w.spawnParticle(Particle.SOUL, p, 1, 0.1, 0.2, 0.1, 0.01);
                if (Math.random() < 0.2) DisplayBuilder.dustParticles(p, 1, 0.1, 220, 100, 30, 0.8f);
            }

            // Footsteps dust ring on ground
            if (tick % 4 == 0) for (int i = 0; i < count; i++) {
                double a = Math.PI * 2 * i / count + tick * 0.025;
                Location p = c.clone().add(Math.cos(a) * 4.0, 0.1, Math.sin(a) * 4.0);
                w.spawnParticle(Particle.LARGE_SMOKE, p, 2, 0.2, 0.05, 0.2, 0.01);
                DisplayBuilder.dustParticles(p, 1, 0.1, 100, 80, 60, 1.0f);
            }

            // Drumbeat sounds on rhythm
            if (tick % 12 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.2f, 0.5f);
            if (tick % 24 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_SNARE, 1.0f, 0.7f);
            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.EVENT_RAID_HORN, 0.8f, 0.5f);
            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BELL_USE, 0.7f, 0.6f);

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new InfernalMarchingBand(plugin); }
    }

    // ================================================================
    // 15. ECLIPSE EVENT — Black sun in the sky with corona of orbiting
    //     ItemDisplay items (gold blocks/coal) and dense ash particles.
    //     Heavy darkness vibe.
    // ================================================================
    public static class EclipseEvent extends EnvironmentalAttack {
        private ItemDisplayHandle disc;
        private final List<ItemDisplayHandle> corona = new ArrayList<>();

        public EclipseEvent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eclipse_event", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.75); config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(30); config.setDurationTicks(500); config.setCooldownTicks(420);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.4f, 0.2f);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_AMBIENT, 0.9f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_DEACTIVATE, 1.2f, 0.4f);

            // Black sun disc — coal block-like
            disc = displayBuilder.spawnItem(c.clone().add(0, 9, 0), new ItemStack(Material.COAL_BLOCK));
            disc.scale(0.3f, 0.3f, 0.3f).glow(20, 0, 30).interpolation(60, 0);
            disc.animateTo(new Vector3f(-1.5f, 9.0f, -1.5f),
                    new AxisAngle4f(0, 0, 1, 0), new Vector3f(3.0f), 60);

            // Corona — alternating gold ingots and amethyst shards
            for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16;
                Material mat = (i % 2 == 0) ? Material.GOLD_INGOT : Material.AMETHYST_SHARD;
                Location p = c.clone().add(Math.cos(a) * 3.5, 9, Math.sin(a) * 3.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(mat));
                h.scale(0.45f, 0.45f, 0.45f).glow(255, 200, 80).interpolation(20, 30 + i);
                corona.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Disc slow rotation + slight pulse
            if (tick % 6 == 0 && disc != null) {
                float s = 3.0f + (float)Math.sin(tick * 0.03) * 0.15f;
                disc.animateTo(new Vector3f(-s/2f, 9.0f, -s/2f),
                        new AxisAngle4f((float)(tick * 0.01), 0, 1, 0), new Vector3f(s), 6);
            }
            // Corona orbit
            if (tick % 3 == 0) {
                for (int i = 0; i < corona.size(); i++) {
                    double a = Math.PI * 2 * i / corona.size() + tick * 0.03;
                    double r = 3.5 + Math.sin(tick * 0.04 + i) * 0.3;
                    float ty = 9.0f + (float)Math.sin(tick * 0.06 + i * 0.4) * 0.4f;
                    corona.get(i).animateTo(
                            new Vector3f((float)(Math.cos(a) * r) - 0.225f, ty, (float)(Math.sin(a) * r) - 0.225f),
                            new AxisAngle4f((float)(tick * 0.1 + i), 0, 1, 0), new Vector3f(0.45f), 3);
                }
            }

            // Corona flame ring particles
            if (tick % 1 == 0) for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24 + tick * 0.04;
                double r = 3.5 + Math.sin(tick * 0.08 + i) * 0.25;
                Location p = c.clone().add(Math.cos(a) * r, 9.0 + (Math.random() - 0.5) * 0.6, Math.sin(a) * r);
                w.spawnParticle(Particle.FLAME, p, 1, 0.05, 0.05, 0.05, 0);
                if (Math.random() < 0.2) DisplayBuilder.dustParticles(p, 1, 0.05, 255, 180, 80, 1.2f);
            }

            // Heavy ash falling below the sun
            if (tick % 1 == 0) for (int i = 0; i < 16; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 8;
                Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.random() * 8, Math.sin(a) * r);
                w.spawnParticle(Particle.ASH, p, 1, 0.3, 0.3, 0.3, 0.01);
                if (Math.random() < 0.2) w.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.4, 0.3, 0.4, 0.01);
            }

            // Beams of dark light from sun toward ground
            if (tick % 4 == 0) for (int b = 0; b < 6; b++) {
                double a = Math.PI * 2 * b / 6 + tick * 0.01;
                for (int seg = 0; seg < 18; seg++) {
                    double t = seg / 18.0;
                    Location p = c.clone().add(Math.cos(a) * (1.0 + t * 4.0), 9.0 - t * 8.5, Math.sin(a) * (1.0 + t * 4.0));
                    DisplayBuilder.dustParticles(p, 1, 0.05, 60, 30, 80, 0.7f);
                }
            }

            if (tick % 100 == 0) DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.0f, 0.3f);
            if (tick % 140 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_AMBIENT, 0.7f, 0.3f);
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DARKNESS, 80, 0, false, false, false));
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new EclipseEvent(plugin); }
    }

    // ================================================================
    // 16. DISSECTION TABLE — Surgical horror scene. Iron-tool ItemDisplays
    //     (shears, hooks, axes) float around a central polished operating
    //     table prop with redstone-blood splatters.
    // ================================================================
    public static class DissectionTable extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> tableParts = new ArrayList<>();
        private final List<ItemDisplayHandle> tools = new ArrayList<>();
        private final double[] toolAngle = new double[10];
        private final double[] toolHeight = new double[10];

        public DissectionTable(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dissection_table", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(7.5); config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(20); config.setDurationTicks(420); config.setCooldownTicks(380);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.3f);
            DisplayBuilder.playSound(c, Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 0.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT, 0.6f, 0.3f);

            // Table top — polished iron ingot tiles
            for (int i = -2; i <= 2; i++) for (int j = -1; j <= 1; j++) {
                Location p = c.clone().add(i * 0.7, 1.0, j * 0.7);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.IRON_INGOT));
                h.scale(0.7f, 0.1f, 0.7f).glow(200, 200, 220).interpolation(25, Math.abs(i) + Math.abs(j));
                h.animateTo(new Vector3f((float)(i * 0.7) - 0.35f, 1.0f, (float)(j * 0.7) - 0.35f),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.7f, 0.1f, 0.7f), 25);
                tableParts.add(h);
            }
            // Table legs — netherite ingots at corners
            int[][] legs = {{-2, -1}, {-2, 1}, {2, -1}, {2, 1}};
            for (int[] leg : legs) {
                Location p = c.clone().add(leg[0] * 0.7, 0.5, leg[1] * 0.7);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHERITE_INGOT));
                h.scale(0.2f, 1.0f, 0.2f).glow(60, 60, 70).interpolation(25, 5);
                h.animateTo(new Vector3f((float)(leg[0] * 0.7) - 0.1f, 0.0f, (float)(leg[1] * 0.7) - 0.1f),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.2f, 1.0f, 0.2f), 25);
                tableParts.add(h);
            }
            // Body on table — a skeleton skull + bones
            ItemDisplayHandle skull = displayBuilder.spawnItem(c.clone().add(-1.2, 1.3, 0), new ItemStack(Material.SKELETON_SKULL));
            skull.scale(0.6f, 0.6f, 0.6f).glow(220, 210, 200).interpolation(30, 20);
            tableParts.add(skull);
            for (int i = 0; i < 4; i++) {
                Location p = c.clone().add((i - 1) * 0.5, 1.2, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BONE));
                h.scale(0.5f, 0.2f, 0.2f).glow(220, 210, 200).interpolation(30, 22);
                tableParts.add(h);
            }

            // Surgical tools floating
            Material[] toolMats = {Material.SHEARS, Material.IRON_AXE, Material.IRON_SWORD, Material.IRON_PICKAXE,
                    Material.IRON_HOE, Material.FISHING_ROD, Material.FLINT, Material.IRON_NUGGET, Material.SHEARS, Material.IRON_SWORD};
            for (int i = 0; i < 10; i++) {
                toolAngle[i] = Math.PI * 2 * i / 10;
                toolHeight[i] = 2.5 + Math.random() * 0.8;
                Location p = c.clone().add(Math.cos(toolAngle[i]) * 2.2, toolHeight[i], Math.sin(toolAngle[i]) * 2.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(toolMats[i]));
                h.scale(0.7f, 0.7f, 0.7f).glow(220, 220, 240).interpolation(4, 0);
                tools.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Tools orbit + tip toward center menacingly
            for (int i = 0; i < tools.size(); i++) {
                toolAngle[i] += 0.04;
                toolHeight[i] = 2.5 + Math.sin(tick * 0.06 + i * 0.7) * 0.4;
                double r = 2.2 + Math.sin(tick * 0.04 + i) * 0.3;
                float tx = (float)(Math.cos(toolAngle[i]) * r);
                float tz = (float)(Math.sin(toolAngle[i]) * r);
                tools.get(i).animateTo(
                        new Vector3f(tx - 0.35f, (float)toolHeight[i], tz - 0.35f),
                        new AxisAngle4f((float)(toolAngle[i] + Math.PI), 0, 1, 0),
                        new Vector3f(0.7f), 4);
            }

            // Blood splatter particles on table surface
            if (tick % 3 == 0) for (int i = 0; i < 10; i++) {
                double x = (Math.random() - 0.5) * 2.6;
                double z = (Math.random() - 0.5) * 1.4;
                Location p = c.clone().add(x, 1.2, z);
                DisplayBuilder.dustParticles(p, 1, 0.08, 150, 10, 10, 1.4f);
                if (Math.random() < 0.2) w.spawnParticle(Particle.LANDING_OBSIDIAN_TEAR, p, 1, 0.05, 0.02, 0.05, 0);
            }

            // Drips from table edge
            if (tick % 4 == 0) for (int i = 0; i < 4; i++) {
                double x = (Math.random() - 0.5) * 2.8;
                double z = (Math.random() - 0.5) * 1.6;
                if (Math.abs(x) < 1.2 && Math.abs(z) < 0.7) continue;
                Location p = c.clone().add(x, 0.8, z);
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, p, 1, 0.05, 0.05, 0.05, 0);
                DisplayBuilder.dustParticles(p, 1, 0.05, 180, 10, 10, 1.0f);
            }

            // Subtle enchant on the tools
            if (tick % 4 == 0) for (int i = 0; i < tools.size(); i++) {
                Location p = tools.get(i).entity().getLocation();
                w.spawnParticle(Particle.ENCHANT, p, 1, 0.05, 0.05, 0.05, 0);
            }

            // Periodic stab sound
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 0.6f);
                w.spawnParticle(Particle.DAMAGE_INDICATOR, c.clone().add(-1.2, 1.5, 0), 4, 0.2, 0.1, 0.2, 0.05);
            }
            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_HURT, 0.6f, 0.4f);
            if (tick % 90 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_IRON_DOOR_CLOSE, 0.7f, 0.3f);

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new DissectionTable(plugin); }
    }

    // ================================================================
    // 17. COSMIC TEAR — Vertical rip in space. ItemDisplay shards
    //     (amethyst/end-rod/glass) spilling out + portal particles.
    // ================================================================
    public static class CosmicTear extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final List<ItemDisplayHandle> tearEdges = new ArrayList<>();

        public CosmicTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_tear", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0); config.setDamageRadius(5.5);
            config.setTicksBetweenDamage(20); config.setDurationTicks(420); config.setCooldownTicks(380);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.BLOCK_END_PORTAL_SPAWN, 1.2f, 0.5f);
            DisplayBuilder.playSound(c, Sound.AMBIENT_BASALT_DELTAS_LOOP, 1.0f, 0.5f);

            // The "tear" — vertical line of dark obsidian items getting taller
            for (int y = 0; y < 12; y++) {
                Location p = c.clone().add(0, 0.5 + y * 0.6, 0);
                Material mat = (y % 2 == 0) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(mat));
                h.scale(0.01f, 0.6f, 0.18f).glow(120, 30, 200).interpolation(40, y * 2);
                h.animateTo(new Vector3f(-0.075f, 0.5f + y * 0.6f, -0.09f),
                        new AxisAngle4f((float)((Math.random() - 0.5) * 0.3), 0, 0, 1),
                        new Vector3f(0.15f, 0.6f, 0.18f), 40);
                tearEdges.add(h);
            }
            // Spilling shards — amethyst / end rod / phantom membrane
            Material[] shardMats = {Material.AMETHYST_SHARD, Material.END_ROD, Material.PHANTOM_MEMBRANE,
                    Material.AMETHYST_SHARD, Material.PRISMARINE_SHARD};
            for (int i = 0; i < 14; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 0.3 + Math.random() * 0.8;
                Location p = c.clone().add(Math.cos(a) * r, 1 + Math.random() * 6, Math.sin(a) * r);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(shardMats[i % shardMats.length]));
                h.scale(0.35f, 0.35f, 0.35f).glow(180, 80, 220).interpolation(4, 0);
                shards.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Tear edges shimmer / oscillate width
            if (tick % 4 == 0) {
                for (int y = 0; y < tearEdges.size(); y++) {
                    float w2 = 0.12f + (float)Math.abs(Math.sin(tick * 0.15 + y * 0.4)) * 0.18f;
                    tearEdges.get(y).animateTo(
                            new Vector3f(-w2/2f, 0.5f + y * 0.6f, -0.09f),
                            new AxisAngle4f((float)(Math.sin(tick * 0.05 + y) * 0.3), 0, 0, 1),
                            new Vector3f(w2, 0.6f, 0.18f), 4);
                }
            }

            // Shards drift outward then back inward (orbit-like)
            for (int i = 0; i < shards.size(); i++) {
                double ang = (tick * 0.04 + i * 0.6);
                double r = 0.5 + Math.abs(Math.sin(tick * 0.03 + i)) * 1.6;
                float ty = 1.0f + ((tick / 2 + i * 8) % 80) * 0.08f;
                if (ty > 7.5f) ty -= 7.5f;
                shards.get(i).animateTo(
                        new Vector3f((float)(Math.cos(ang) * r) - 0.175f, ty, (float)(Math.sin(ang) * r) - 0.175f),
                        new AxisAngle4f((float)(tick * 0.2 + i), 1, 1, 1),
                        new Vector3f(0.35f), 4);
            }

            // Portal particles streaming through the tear
            if (tick % 1 == 0) for (int y = 0; y < 12; y++) {
                Location p = c.clone().add((Math.random() - 0.5) * 0.2, 0.5 + y * 0.6 + Math.random() * 0.4, (Math.random() - 0.5) * 0.2);
                w.spawnParticle(Particle.PORTAL, p, 2, 0.05, 0.1, 0.05, 0.5);
                if (Math.random() < 0.2) w.spawnParticle(Particle.REVERSE_PORTAL, p, 1, 0.05, 0.1, 0.05, 0.4);
            }

            // Sparking edge — dust + electric
            if (tick % 2 == 0) for (int i = 0; i < 8; i++) {
                double y = Math.random() * 7;
                Location p = c.clone().add((Math.random() - 0.5) * 0.4, 0.5 + y, (Math.random() - 0.5) * 0.4);
                DisplayBuilder.dustParticles(p, 1, 0.1, 128, 48, 192, 1.2f);
                if (Math.random() < 0.15) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.2, 0.1, 0.2, 0.05);
            }

            // Outward "leak" particles
            if (tick % 3 == 0) for (int i = 0; i < 12; i++) {
                double a = Math.random() * Math.PI * 2, r = 1 + Math.random() * 3.5;
                Location p = c.clone().add(Math.cos(a) * r, 1 + Math.random() * 5, Math.sin(a) * r);
                w.spawnParticle(Particle.SCULK_CHARGE_POP, p, 1, 0.1, 0.1, 0.1, 0.02);
            }

            if (tick % 60 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.4f);
            if (tick % 110 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_SCREAM, 0.6f, 0.3f);

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CosmicTear(plugin); }
    }

    // ================================================================
    // 18. PROPHET'S THRONE — Levitating throne (bone + gold items) with
    //     a floating crown above, soul particles all around it.
    // ================================================================
    public static class ProphetsThrone extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> throne = new ArrayList<>();
        private ItemDisplayHandle crown;

        public ProphetsThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("prophets_throne", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.25); config.setDamageRadius(6.5);
            config.setTicksBetweenDamage(25); config.setDurationTicks(480); config.setCooldownTicks(420);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.5f);

            // Throne base — gold ingot platform
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                Location p = c.clone().add(dx * 0.7, 2.0, dz * 0.7);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GOLD_INGOT));
                h.scale(0.7f, 0.15f, 0.7f).glow(255, 220, 60).interpolation(40, 0);
                h.animateTo(new Vector3f((float)(dx * 0.7) - 0.35f, 2.0f, (float)(dz * 0.7) - 0.35f),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.7f, 0.15f, 0.7f), 40);
                throne.add(h);
            }
            // Throne back — vertical bones + central gold
            for (int y = 0; y < 5; y++) {
                Location p = c.clone().add(0, 2.5 + y * 0.5, -0.7);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BONE));
                h.scale(1.2f, 0.5f, 0.15f).glow(220, 200, 180).interpolation(40, 4 + y);
                h.animateTo(new Vector3f(-0.6f, 2.5f + y * 0.5f, -0.775f),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(1.2f, 0.5f, 0.15f), 40);
                throne.add(h);
            }
            // Throne armrests
            for (int side = -1; side <= 1; side += 2) {
                Location p = c.clone().add(side * 0.7, 2.6, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GOLD_INGOT));
                h.scale(0.18f, 0.5f, 1.4f).glow(255, 220, 60).interpolation(40, 8);
                h.animateTo(new Vector3f((float)(side * 0.7) - 0.09f, 2.6f, -0.7f),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.18f, 0.5f, 1.4f), 40);
                throne.add(h);
            }
            // Crown (golden helmet) floating above
            crown = displayBuilder.spawnItem(c.clone().add(0, 5.5, 0), new ItemStack(Material.GOLDEN_HELMET));
            crown.scale(1.0f, 1.0f, 1.0f).glow(255, 230, 80).interpolation(40, 20);
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Crown levitates and rotates
            if (tick % 3 == 0 && crown != null) {
                float ty = 5.5f + (float)Math.sin(tick * 0.05) * 0.3f;
                crown.animateTo(new Vector3f(-0.5f, ty, -0.5f),
                        new AxisAngle4f((float)(tick * 0.06), 0, 1, 0), new Vector3f(1.0f), 3);
            }

            // Soul particles streaming up around throne
            if (tick % 2 == 0) for (int i = 0; i < 14; i++) {
                double a = Math.random() * Math.PI * 2, r = 0.4 + Math.random() * 1.2;
                Location p = c.clone().add(Math.cos(a) * r, 2.0 + Math.random() * 4, Math.sin(a) * r);
                w.spawnParticle(Particle.SOUL, p, 1, 0.1, 0.2, 0.1, 0.02);
            }

            // Holy enchant ring around the crown
            if (tick % 1 == 0) for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + tick * 0.08;
                Location p = c.clone().add(Math.cos(a) * 0.8, 5.7, Math.sin(a) * 0.8);
                w.spawnParticle(Particle.ENCHANT, p, 1, 0.05, 0.1, 0.05, 0);
                if (Math.random() < 0.2) w.spawnParticle(Particle.END_ROD, p, 1, 0.05, 0.05, 0.05, 0);
            }

            // Cursed yellow glow at base
            if (tick % 2 == 0) for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12 + tick * 0.04;
                Location p = c.clone().add(Math.cos(a) * 1.2, 1.95, Math.sin(a) * 1.2);
                DisplayBuilder.dustParticles(p, 1, 0.05, 255, 200, 60, 1.0f);
            }

            // Beam from crown down to throne seat
            if (tick % 1 == 0) for (int seg = 0; seg < 10; seg++) {
                double t = seg / 10.0;
                Location p = c.clone().add((Math.random() - 0.5) * 0.05, 2.5 + (5.5 - 2.5) * t, (Math.random() - 0.5) * 0.05);
                if (Math.random() < 0.5) w.spawnParticle(Particle.END_ROD, p, 1, 0.02, 0.02, 0.02, 0);
            }

            // Witch puffs (corruption beneath the holy)
            if (tick % 6 == 0) for (int i = 0; i < 4; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 2.5;
                w.spawnParticle(Particle.WITCH, c.clone().add(Math.cos(a) * r, 1.0 + Math.random() * 1.5, Math.sin(a) * r), 1, 0.1, 0.1, 0.1, 0);
            }

            if (tick % 80 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.5f);
            if (tick % 120 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.7f, 0.6f);

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) { p.damage(config.getDamage()); p.setNoDamageTicks(0); }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new ProphetsThrone(plugin); }
    }

    // ================================================================
    // 19. RAIN OF EYES — Ender eyes raining from the sky with trailing
    //     particles. Each one impacts and bursts into shadow particles.
    // ================================================================
    public static class RainOfEyes extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> eyes = new ArrayList<>();
        private final double[] eyeY = new double[18];
        private final double[] eyeAng = new double[18];
        private final double[] eyeR = new double[18];
        private final Material[] eyeMats = {Material.ENDER_EYE, Material.GLOW_INK_SAC, Material.ENDER_PEARL,
                Material.ENDER_EYE, Material.GLOW_INK_SAC};

        public RainOfEyes(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rain_of_eyes", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.5); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(15); config.setDurationTicks(380); config.setCooldownTicks(340);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_AMBIENT, 1.4f, 0.4f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.2f, 0.5f);
            for (int i = 0; i < 18; i++) {
                eyeAng[i] = Math.random() * Math.PI * 2;
                eyeR[i] = Math.random() * 7.5;
                eyeY[i] = 9 + Math.random() * 4;
                Location p = c.clone().add(Math.cos(eyeAng[i]) * eyeR[i], eyeY[i], Math.sin(eyeAng[i]) * eyeR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(eyeMats[i % eyeMats.length]));
                h.scale(0.4f, 0.4f, 0.4f).glow(140, 200, 60).interpolation(2, 0);
                eyes.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < eyes.size(); i++) {
                eyeY[i] -= 0.22 + (i % 3) * 0.04;
                Location pos = c.clone().add(Math.cos(eyeAng[i]) * eyeR[i], eyeY[i], Math.sin(eyeAng[i]) * eyeR[i]);
                eyes.get(i).animateTo(
                        new Vector3f((float)(Math.cos(eyeAng[i]) * eyeR[i]) - 0.2f, (float)eyeY[i], (float)(Math.sin(eyeAng[i]) * eyeR[i]) - 0.2f),
                        new AxisAngle4f((float)(tick * 0.4 + i), 0, 1, 0), new Vector3f(0.4f), 2);

                // Trail
                w.spawnParticle(Particle.PORTAL, pos, 1, 0.1, 0.1, 0.1, 0.2);
                w.spawnParticle(Particle.WITCH, pos, 1, 0.05, 0.05, 0.05, 0);
                if (Math.random() < 0.2) DisplayBuilder.dustParticles(pos, 1, 0.05, 140, 200, 60, 0.8f);

                if (eyeY[i] < 0.3) {
                    // Impact burst
                    Location impact = pos.clone();
                    DisplayBuilder.playSound(impact, Sound.ENTITY_ENDERMAN_HURT, 0.6f, 0.7f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_SLIME_BLOCK_BREAK, 0.5f, 1.2f);
                    w.spawnParticle(Particle.SQUID_INK, impact, 12, 0.4, 0.2, 0.4, 0.05);
                    w.spawnParticle(Particle.WITCH, impact, 8, 0.4, 0.2, 0.4, 0.02);
                    w.spawnParticle(Particle.LARGE_SMOKE, impact, 6, 0.3, 0.2, 0.3, 0.01);

                    // Damage on impact
                    for (Player p : w.getPlayers()) {
                        if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                        if (p.getLocation().distanceSquared(impact) < 3.5) {
                            p.damage(config.getDamage()); p.setNoDamageTicks(0);
                            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0, false, false, false));
                        }
                    }

                    eyeAng[i] = Math.random() * Math.PI * 2;
                    eyeR[i] = Math.random() * 7.5;
                    eyeY[i] = 9 + Math.random() * 4;
                }
            }

            // Background eye-stares — sporadic squid_ink puffs that linger
            if (tick % 5 == 0) for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 7;
                Location p = c.clone().add(Math.cos(a) * r, 6 + Math.random() * 3, Math.sin(a) * r);
                w.spawnParticle(Particle.SQUID_INK, p, 1, 0.1, 0.1, 0.1, 0.01);
            }

            if (tick % 90 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_SCREAM, 0.6f, 0.4f);
        }

        @Override public AbstractAttack newInstance() { return new RainOfEyes(plugin); }
    }

    // ================================================================
    // 20. OSSUARY — Bone catacomb structure of skull+bone piles arranged
    //     in a ring, swirling bone-dust particles. Solemn atmosphere.
    // ================================================================
    public static class Ossuary extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> piles = new ArrayList<>();

        public Ossuary(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ossuary", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.75); config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(25); config.setDurationTicks(500); config.setCooldownTicks(420);
        }

        @Override protected void onSpawn(Location c) {
            DisplayBuilder.playSound(c, Sound.BLOCK_BONE_BLOCK_BREAK, 1.4f, 0.3f);
            DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 1.0f, 0.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_SKELETON_AMBIENT, 0.8f, 0.5f);

            // 8 bone piles arranged in a circle
            for (int p = 0; p < 8; p++) {
                double a = Math.PI * 2 * p / 8;
                double cx = Math.cos(a) * 5.0;
                double cz = Math.sin(a) * 5.0;
                // Skull on top
                Location skullLoc = c.clone().add(cx, 1.4, cz);
                ItemDisplayHandle skull = displayBuilder.spawnItem(skullLoc, new ItemStack(Material.SKELETON_SKULL));
                skull.scale(0.6f, 0.6f, 0.6f).glow(220, 210, 200).interpolation(30, p * 2);
                skull.animateTo(new Vector3f((float)cx - 0.3f, 1.4f, (float)cz - 0.3f),
                        new AxisAngle4f((float)a, 0, 1, 0), new Vector3f(0.6f), 30);
                piles.add(skull);

                // Crossed bones (4 around the skull)
                for (int b = 0; b < 4; b++) {
                    double ba = a + b * (Math.PI / 2);
                    Location bp = c.clone().add(cx + Math.cos(ba) * 0.4, 0.5 + (b % 2) * 0.2, cz + Math.sin(ba) * 0.4);
                    ItemDisplayHandle bone = displayBuilder.spawnItem(bp, new ItemStack(Material.BONE));
                    bone.scale(0.7f, 0.18f, 0.18f).glow(210, 200, 180).interpolation(30, p * 2 + b);
                    bone.animateTo(new Vector3f((float)(cx + Math.cos(ba) * 0.4) - 0.35f, 0.5f + (b % 2) * 0.2f, (float)(cz + Math.sin(ba) * 0.4) - 0.09f),
                            new AxisAngle4f((float)ba, 0, 1, 0), new Vector3f(0.7f, 0.18f, 0.18f), 30);
                    piles.add(bone);
                }

                // Bone meal pile dust at base
                Location dustBase = c.clone().add(cx, 0.15, cz);
                ItemDisplayHandle dust = displayBuilder.spawnItem(dustBase, new ItemStack(Material.BONE_MEAL));
                dust.scale(0.9f, 0.18f, 0.9f).glow(220, 220, 200).interpolation(30, p * 2);
                dust.animateTo(new Vector3f((float)cx - 0.45f, 0.05f, (float)cz - 0.45f),
                        new AxisAngle4f(0, 0, 1, 0), new Vector3f(0.9f, 0.18f, 0.9f), 30);
                piles.add(dust);
            }

            // Central altar — bone block-equivalent (use bone item large)
            ItemDisplayHandle center = displayBuilder.spawnItem(c.clone().add(0, 0.7, 0), new ItemStack(Material.BONE));
            center.scale(2.0f, 0.6f, 2.0f).glow(230, 220, 200).interpolation(40, 20);
            center.animateTo(new Vector3f(-1.0f, 0.4f, -1.0f),
                    new AxisAngle4f(0, 0, 1, 0), new Vector3f(2.0f, 0.6f, 2.0f), 40);
            piles.add(center);

            // Skull pyramid on the altar (3 stacked)
            for (int s = 0; s < 3; s++) {
                Location sp = c.clone().add(0, 1.1 + s * 0.5, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(sp, new ItemStack(Material.SKELETON_SKULL));
                h.scale(0.7f - s * 0.1f, 0.7f - s * 0.1f, 0.7f - s * 0.1f).glow(220, 200, 180).interpolation(40, 25 + s * 3);
                piles.add(h);
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Skulls slowly turn — just the 8 outer skulls (every 6th piece in our list, indexed at 0,6,12...)
            // Our pattern: per outer station: [skull, bone, bone, bone, bone, dust] = 6 items per station, then center+3 skulls
            if (tick % 8 == 0) {
                for (int p = 0; p < 8; p++) {
                    int idx = p * 6;
                    if (idx >= piles.size()) break;
                    double a = Math.PI * 2 * p / 8;
                    double cx = Math.cos(a) * 5.0;
                    double cz = Math.sin(a) * 5.0;
                    float ty = 1.4f + (float)Math.sin(tick * 0.03 + p) * 0.08f;
                    piles.get(idx).animateTo(
                            new Vector3f((float)cx - 0.3f, ty, (float)cz - 0.3f),
                            new AxisAngle4f((float)(a + tick * 0.02), 0, 1, 0),
                            new Vector3f(0.6f), 8);
                }
            }

            // Bone dust swirling rings
            if (tick % 1 == 0) for (int i = 0; i < 16; i++) {
                double a = Math.PI * 2 * i / 16 + tick * 0.04;
                double r = 5.0 + Math.sin(tick * 0.06 + i) * 0.6;
                Location p = c.clone().add(Math.cos(a) * r, 0.5 + Math.sin(tick * 0.05 + i) * 0.5, Math.sin(a) * r);
                DisplayBuilder.dustParticles(p, 1, 0.1, 216, 208, 192, 1.0f);
            }

            // White ash drifting up from the altar
            if (tick % 2 == 0) for (int i = 0; i < 8; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 1.0;
                Location p = c.clone().add(Math.cos(a) * r, 1.5 + Math.random() * 3, Math.sin(a) * r);
                w.spawnParticle(Particle.WHITE_ASH, p, 1, 0.15, 0.2, 0.15, 0.005);
                if (Math.random() < 0.3) w.spawnParticle(Particle.SOUL, p, 1, 0.1, 0.1, 0.1, 0.01);
            }

            // Sculk soul wisps from each skull — hint of haunting
            if (tick % 5 == 0) for (int p = 0; p < 8; p++) {
                double a = Math.PI * 2 * p / 8;
                Location sp = c.clone().add(Math.cos(a) * 5.0, 1.6, Math.sin(a) * 5.0);
                w.spawnParticle(Particle.SCULK_SOUL, sp, 1, 0.1, 0.2, 0.1, 0.02);
            }

            // Center beam of soul fire flame upward from altar
            if (tick % 2 == 0) for (int i = 0; i < 4; i++) {
                Location p = c.clone().add((Math.random() - 0.5) * 0.4, 1 + Math.random() * 4, (Math.random() - 0.5) * 0.4);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.05, 0.1, 0.05, 0);
            }

            if (tick % 70 == 0) DisplayBuilder.playSound(c, Sound.ENTITY_SKELETON_AMBIENT, 0.5f, 0.4f);
            if (tick % 110 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_BONE_BLOCK_HIT, 0.7f, 0.3f);

            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 100, 0, false, false, false));
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new Ossuary(plugin); }
    }
}
