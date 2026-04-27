package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Devil's Dream Mode — ENVIRONMENTAL EFFECT ATTACKS (set 3 — entries 21–30).
 * Pure particle + ItemDisplay attacks. NO BlockDisplays. Damage tuned +50%.
 *
 * 21. Gravity Inversion Field   — obsidian-item ring, drifting debris items rise.
 * 22. The Flayed Highway        — corridor of bone-pillars + redstone banner items.
 * 23. Comet Impact Site         — half-buried obsidian-item core, basalt rim.
 * 24. Swarm of Souls            — drifting soul-lantern items, soul flames.
 * 25. The Devil's Signature     — burning cursive "MINE" lantern letters.
 * 26. Void Peel                 — peeling wall strips revealing void.
 * 27. The Infernal Compass Storm— rotating NSEW elemental beams.
 * 28. Cascading Staircase       — 15-step staircase to nowhere.
 * 29. The Heartbeat Floor       — pulsing red heart-grid.
 * 30. Collapsing Dimension Pillars — three 12-tall pillars sinking on timers.
 */
public final class DDEnvFX3 {
    private DDEnvFX3() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GravityInversionField(plugin));
        registry.register(new FlayedHighway(plugin));
        registry.register(new CometImpactSite(plugin));
        registry.register(new SwarmOfSouls(plugin));
        registry.register(new DevilsSignature(plugin));
        registry.register(new VoidPeel(plugin));
        registry.register(new InfernalCompassStorm(plugin));
        registry.register(new StaircaseToNowhere(plugin));
        registry.register(new HeartbeatFloor(plugin));
        registry.register(new CollapsingDimensionPillars(plugin));
    }

    // ============================================================
    // 21. GRAVITY INVERSION FIELD — active hazard
    //     Obsidian-item ring + 8 floating debris items drift upward.
    //     Inverted particles configured to RISE.
    // ============================================================
    public static class GravityInversionField extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> ring = new ArrayList<>();
        private final List<ItemDisplayHandle> debris = new ArrayList<>();
        private final List<Float> debrisY = new ArrayList<>();

        public GravityInversionField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_inversion_field", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.25);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(360);
            config.setCooldownTicks(440);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.4f);

            // 12 obsidian-item ring markers on the floor
            int count = 12;
            for (int i = 0; i < count; i++) {
                double a = (Math.PI * 2 * i) / count;
                double x = Math.cos(a) * 5.5;
                double z = Math.sin(a) * 5.5;
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(x, 0.2, z), new ItemStack(Material.OBSIDIAN));
                h.scale(0.95f, 0.25f, 0.95f).glow(80, 0, 180).interpolation(15, 0);
                ring.add(h);
            }

            // 8 floating debris items — flint, cobbled deepslate, basalt items
            Material[] mats = { Material.FLINT, Material.COBBLED_DEEPSLATE, Material.CALCITE,
                    Material.BLACKSTONE, Material.FLINT, Material.TUFF,
                    Material.DEEPSLATE_TILES, Material.BASALT };
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 * i) / 8;
                double rx = Math.cos(a) * 3.0;
                double rz = Math.sin(a) * 3.0;
                float startY = (float) (Math.random() * 5.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(rx, startY, rz), new ItemStack(mats[i]));
                h.scale(0.6f, 0.6f, 0.6f).glow(160, 80, 220).interpolation(20, 0);
                debris.add(h);
                debrisY.add(startY);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            double R = 5.5;

            // Ring item glow pulse
            if (tick % 30 == 0) {
                for (ItemDisplayHandle h : ring) {
                    h.glow(60 + (int) (Math.random() * 80), 0, 180 + (int) (Math.random() * 60));
                }
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.6f);
            }

            // Inverted particles — upward velocity
            if (tick % 2 == 0) {
                for (int i = 0; i < 18; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * R;
                    double x = c.getX() + Math.cos(a) * r;
                    double z = c.getZ() + Math.sin(a) * r;
                    double y = c.getY() + Math.random() * 6.0;
                    w.spawnParticle(Particle.LARGE_SMOKE, x, y, z, 1, 0.05, 0.4, 0.05, 0.0);
                    w.spawnParticle(Particle.SMOKE,      x, y, z, 1, 0.05, 0.5, 0.05, 0.0);
                    if (Math.random() < 0.4)
                        w.spawnParticle(Particle.FLAME,  x, y, z, 1, 0.02, 0.6, 0.02, 0.0);
                    if (Math.random() < 0.3)
                        w.spawnParticle(Particle.REVERSE_PORTAL, x, y, z, 1, 0.1, 0.5, 0.1, 0.0);
                }
            }

            // Perimeter purple dust pillar
            if (tick % 4 == 0) {
                for (int i = 0; i < 18; i++) {
                    double a = (Math.PI * 2 * i) / 18 + tick * 0.04;
                    double y = c.getY() + (tick * 0.05) % 6.0;
                    Location p = new Location(w,
                            c.getX() + Math.cos(a) * R, y, c.getZ() + Math.sin(a) * R);
                    DisplayBuilder.dustParticles(p, 1, 0.05, 128, 48, 192, 1.2f);
                    if (Math.random() < 0.4)
                        w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0, 0, 0, 0);
                }
            }

            // Float debris up; recycle to floor when reaches "ceiling" (8)
            if (tick % 4 == 0) {
                for (int i = 0; i < debris.size(); i++) {
                    float y = debrisY.get(i) + 0.35f;
                    if (y > 8.0f) y = 0.0f;
                    debrisY.set(i, y);
                    double a = (Math.PI * 2 * i) / debris.size() + tick * 0.02;
                    float tx = (float) (Math.cos(a) * 3.0) - 0.3f;
                    float tz = (float) (Math.sin(a) * 3.0) - 0.3f;
                    debris.get(i).animateTo(
                            new Vector3f(tx, y - 0.3f, tz),
                            new AxisAngle4f((float) (tick * 0.06), 0.3f, 1f, 0.2f),
                            new Vector3f(0.6f, 0.6f, 0.6f), 4);
                }
            }

            // Damage
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new GravityInversionField(plugin); }
    }

    // ============================================================
    // 22. THE FLAYED HIGHWAY — active hazard
    //     2 rows × 6 bone-pillar item stacks + 6 redstone banner items.
    // ============================================================
    public static class FlayedHighway extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> pillars = new ArrayList<>();
        private final List<ItemDisplayHandle> banners = new ArrayList<>();
        private final List<ItemDisplayHandle> skulls = new ArrayList<>();

        public FlayedHighway(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("flayed_highway", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.75);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(22);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(380);
            config.setCooldownTicks(460);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_WARN, 0.7f, 0.8f);

            // 2 rows of 6 tall bone pillars (stacked bone items vertically)
            for (int row = 0; row < 2; row++) {
                double zSide = row == 0 ? -2.0 : 2.0;
                for (int i = 0; i < 6; i++) {
                    double x = -7.5 + i * 3.0;
                    ItemDisplayHandle base = displayBuilder.spawnItem(
                            center.clone().add(x, 0, zSide), new ItemStack(Material.BONE));
                    base.scale(1.0f, 4.0f, 1.0f).glow(220, 220, 200).interpolation(15, 0);
                    pillars.add(base);
                    // Skeleton skull on top of each pillar
                    ItemDisplayHandle skull = displayBuilder.spawnItem(
                            center.clone().add(x, 4.2, zSide), new ItemStack(Material.SKELETON_SKULL));
                    skull.scale(0.8f, 0.8f, 0.8f).glow(255, 240, 200).interpolation(15, 0);
                    skulls.add(skull);
                }
            }

            // 6 banners (redstone block items) stretched between pairs at top
            for (int i = 0; i < 6; i++) {
                double x = -7.5 + i * 3.0;
                ItemDisplayHandle banner = displayBuilder.spawnItem(
                        center.clone().add(x, 3.5, 0), new ItemStack(Material.REDSTONE_BLOCK));
                banner.scale(0.6f, 2.5f, 4.0f).glow(180, 0, 0).interpolation(20, 0);
                banners.add(banner);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Banner sway — subtle rotation animation
            if (tick % 20 == 0) {
                float swing = (float) Math.sin(tick * 0.05) * 0.08f;
                for (int i = 0; i < banners.size(); i++) {
                    double x = -7.5 + i * 3.0;
                    banners.get(i).animateTo(
                            new Vector3f((float) x - 0.3f, 3.0f, -2.0f),
                            new AxisAngle4f(swing, 0, 0, 1),
                            new Vector3f(0.6f, 2.5f, 4.0f), 20);
                }
                // Skull bobbing
                for (int i = 0; i < skulls.size(); i++) {
                    int row = i / 6;
                    int col = i % 6;
                    double zSide = row == 0 ? -2.0 : 2.0;
                    double x = -7.5 + col * 3.0;
                    float bob = (float) Math.sin(tick * 0.04 + i) * 0.08f;
                    skulls.get(i).animateTo(
                            new Vector3f((float) x - 0.4f, 4.2f + bob, (float) zSide - 0.4f),
                            new AxisAngle4f((float) (tick * 0.02 + i), 0, 1, 0),
                            new Vector3f(0.8f, 0.8f, 0.8f), 20);
                }
            }

            // Dripping curtains from each banner — dripping_obsidian_tear + blood dust
            if (tick % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double x = c.getX() + (-7.5 + i * 3.0);
                    for (int n = 0; n < 8; n++) {
                        double dz = -2.0 + (n / 7.0) * 4.0;
                        for (int dy = 0; dy < 4; dy++) {
                            if (Math.random() < 0.35)
                                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR,
                                        x, c.getY() + 4.5 - dy, c.getZ() + dz,
                                        1, 0, 0, 0, 0);
                            if (Math.random() < 0.18)
                                DisplayBuilder.dustParticles(
                                        new Location(w, x, c.getY() + 4.0 - dy, c.getZ() + dz),
                                        1, 0.05, 140, 16, 16, 1.4f);
                        }
                    }
                }
            }

            // Floor soul flame line down corridor
            if (tick % 3 == 0) {
                for (double xo = -8.0; xo <= 8.0; xo += 0.7) {
                    double dz = (Math.random() - 0.5) * 1.5;
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            c.getX() + xo, c.getY() + 0.1, c.getZ() + dz,
                            1, 0, 0.02, 0, 0.0);
                }
            }

            // Skull rattle
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_SKELETON_AMBIENT, 0.7f, 0.6f);
                DisplayBuilder.playSound(c, Sound.BLOCK_BONE_BLOCK_HIT, 0.6f, 0.5f);
            }

            // Damage
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new FlayedHighway(plugin); }
    }

    // ============================================================
    // 23. COMET IMPACT SITE — atmosphere
    //     Half-buried obsidian-item sphere + 6 tilted basalt rim items
    //     + crack line of 4 blackstone slivers + crying obsidian core.
    // ============================================================
    public static class CometImpactSite extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> comet = new ArrayList<>();
        private final List<ItemDisplayHandle> rim = new ArrayList<>();
        private final List<ItemDisplayHandle> crack = new ArrayList<>();

        public CometImpactSite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("comet_impact_site", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.25);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(420);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.2f, 0.5f);

            // Comet "sphere" — 1 large obsidian item core + 6 surrounding crying obsidian items
            ItemDisplayHandle core = displayBuilder.spawnItem(
                    center.clone().add(0, 0.6, 0), new ItemStack(Material.OBSIDIAN));
            core.scale(2.4f, 2.4f, 2.4f).glow(255, 80, 0).interpolation(25, 0);
            comet.add(core);
            double[][] cluster = { {0.6, 0.5, 0}, {-0.6, 0.5, 0}, {0, 0.5, 0.6},
                                   {0, 0.5, -0.6}, {0, 1.4, 0}, {0.4, 1.0, 0.4} };
            for (double[] o : cluster) {
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(o[0], o[1], o[2]), new ItemStack(Material.CRYING_OBSIDIAN));
                h.scale(1.1f, 1.1f, 1.1f).glow(255, 60, 60).interpolation(25, 0);
                comet.add(h);
            }

            // 6 tilted basalt rim items
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 * i) / 6;
                double rx = Math.cos(a) * 3.0;
                double rz = Math.sin(a) * 3.0;
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(rx, 0.2, rz), new ItemStack(Material.BASALT));
                h.scale(1.2f, 0.5f, 1.2f).rotate((float) (a + Math.PI / 6), 1, 0, 1)
                        .glow(60, 60, 80).interpolation(30, 0);
                rim.add(h);
            }

            // Crack line — 4 blackstone slivers running away from impact
            for (int i = 0; i < 4; i++) {
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(2.5 + i * 1.2, 0.05, 0.3 * i), new ItemStack(Material.BLACKSTONE));
                h.scale(1.0f, 0.1f, 0.4f).glow(255, 60, 0).interpolation(25, 0);
                crack.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Comet slow rotation
            if (tick % 8 == 0) {
                comet.get(0).animateTo(
                        new Vector3f(-1.2f, 0.6f - 1.2f, -1.2f),
                        new AxisAngle4f((float) (tick * 0.02), 0.3f, 1f, 0.2f),
                        new Vector3f(2.4f, 2.4f, 2.4f), 8);
            }

            // Lava seep from comet cracks
            if (tick % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 1.4 + Math.random() * 0.4;
                    w.spawnParticle(Particle.LAVA,
                            c.getX() + Math.cos(a) * r,
                            c.getY() + 0.6 + Math.random() * 1.2,
                            c.getZ() + Math.sin(a) * r,
                            1, 0, 0, 0, 0);
                    if (Math.random() < 0.35)
                        w.spawnParticle(Particle.DRIPPING_DRIPSTONE_LAVA,
                                c.getX() + Math.cos(a) * r,
                                c.getY() + 1.4,
                                c.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
            }

            // Billowing large_smoke off the top
            if (tick % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    w.spawnParticle(Particle.LARGE_SMOKE,
                            c.getX() + (Math.random() - 0.5) * 2,
                            c.getY() + 1.8 + Math.random() * 1.5,
                            c.getZ() + (Math.random() - 0.5) * 2,
                            1, 0.1, 0.3, 0.1, 0.02);
                }
            }

            // Ash drift in cone (direction = +X)
            if (tick % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double dx = 1.5 + Math.random() * 5.0;
                    double dz = (Math.random() - 0.5) * dx * 0.6;
                    w.spawnParticle(Particle.ASH,
                            c.getX() + dx, c.getY() + 0.5 + Math.random() * 2.0,
                            c.getZ() + dz, 1, 0.1, 0.1, 0.1, 0.0);
                }
            }

            // Falling lava trail along crack
            if (tick % 5 == 0) {
                for (int i = 0; i < 4; i++) {
                    w.spawnParticle(Particle.FALLING_LAVA,
                            c.getX() + 2.5 + i * 1.2,
                            c.getY() + 0.2,
                            c.getZ() + 0.3 * i + (Math.random() - 0.5) * 0.4,
                            1, 0.1, 0.05, 0.1, 0.0);
                }
            }

            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BASALT_STEP, 0.8f, 0.3f);
            }

            // Damage
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0); p.setFireTicks(40);
                    }
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new CometImpactSite(plugin); }
    }

    // ============================================================
    // 24. SWARM OF SOULS — atmosphere
    //     12 drifting soul-lantern item displays as glowing source
    //     markers; soul flame particles emit from each one.
    // ============================================================
    public static class SwarmOfSouls extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> sources = new ArrayList<>();
        private final List<Vector> sourceVel = new ArrayList<>();

        public SwarmOfSouls(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("swarm_of_souls", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.25);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(400);
            config.setCooldownTicks(480);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_WARN, 0.7f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.4f);

            // 12 drifting source lanterns (used as soft glow markers)
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 * i) / 12;
                double r = 3.0 + Math.random() * 4.0;
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r;
                double y = 1.4 + Math.random() * 1.6;
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(x, y, z), new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.45f, 0.45f, 0.45f).glow(150, 220, 255).interpolation(30, 0);
                sources.add(h);
                sourceVel.add(new Vector(
                        (Math.random() - 0.5) * 0.08,
                        (Math.random() - 0.5) * 0.05,
                        (Math.random() - 0.5) * 0.08));
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Drift sources gently — bounce within a 7-block sphere
            if (tick % 8 == 0) {
                for (int i = 0; i < sources.size(); i++) {
                    Vector v = sourceVel.get(i);
                    org.bukkit.entity.ItemDisplay e = sources.get(i).entity();
                    Location l = e.getLocation();
                    Vector rel = l.toVector().subtract(c.toVector());
                    if (rel.length() > 7.0) v.multiply(-1);
                    float tx = (float) (rel.getX() + v.getX() * 8) - 0.225f;
                    float ty = (float) (rel.getY() + v.getY() * 8) - 0.225f;
                    float tz = (float) (rel.getZ() + v.getZ() * 8) - 0.225f;
                    sources.get(i).animateTo(
                            new Vector3f(tx, ty, tz),
                            new AxisAngle4f((float) (tick * 0.05), 0, 1, 0),
                            new Vector3f(0.45f, 0.45f, 0.45f), 8);
                }
            }

            // Soul flames from each source — random directions w/ persistent velocity
            for (int i = 0; i < sources.size(); i++) {
                Location l = sources.get(i).entity().getLocation();
                for (int n = 0; n < 2; n++) {
                    double dx = (Math.random() - 0.5) * 0.4;
                    double dy = (Math.random() - 0.5) * 0.3;
                    double dz = (Math.random() - 0.5) * 0.4;
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            l.getX(), l.getY(), l.getZ(),
                            1, dx, dy, dz, 0.08);
                }
                if (tick % 5 == 0) {
                    w.spawnParticle(Particle.SOUL,
                            l.getX(), l.getY(), l.getZ(),
                            1, 0.15, 0.15, 0.15, 0.02);
                }
            }

            // Pursuit cluster — every 60 ticks all souls converge then explode
            if (tick % 60 == 30) {
                Player tgt = getTargetPlayer();
                Location focus = (tgt != null) ? tgt.getLocation() : c.clone().add(0, 1.5, 0);
                for (int i = 0; i < 60; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = 3.0 + Math.random() * 3.0;
                    Location from = focus.clone().add(
                            Math.cos(a) * r, (Math.random() - 0.5) * 2, Math.sin(a) * r);
                    Vector dir = focus.toVector().subtract(from.toVector()).normalize().multiply(0.4);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            from.getX(), from.getY(), from.getZ(),
                            1, dir.getX(), dir.getY(), dir.getZ(), 0.0);
                }
                DisplayBuilder.playSound(focus, Sound.ENTITY_WARDEN_AMBIENT, 0.9f, 1.4f);
            }

            if (tick % 80 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GHAST_WARN, 0.6f, 0.6f);
            }

            // Damage
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new SwarmOfSouls(plugin); }
    }

    // ============================================================
    // 25. THE DEVIL'S SIGNATURE — atmosphere
    //     "MINE" word laid out: 4 letters, soul-lanterns at turning
    //     points (~17 lanterns) + soul fire particle stroke retracing.
    // ============================================================
    public static class DevilsSignature extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> lanterns = new ArrayList<>();
        private final List<Vector> letterPath = new ArrayList<>();
        private int retraceLetter = -1;
        private int retraceProgress = 0;

        public DevilsSignature(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_signature", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(40);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(400);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 0.6f);

            // Build relative coords for the letters M, I, N, E along X axis
            addLetter(-6.0, new double[][]{ {0,0}, {0,2.5}, {1.0,1.0}, {2.0,2.5}, {2.0,0} });
            addLetter(-2.5, new double[][]{ {0,0}, {0,2.5} });
            addLetter(0.0,  new double[][]{ {0,0}, {0,2.5}, {2.0,0}, {2.0,2.5} });
            addLetter(3.5,  new double[][]{ {0,0}, {0,2.5}, {2.0,2.5}, {0,1.25}, {1.5,1.25}, {2.0,0} });

            // Place lanterns at turning points
            for (Vector p : letterPath) {
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(p.getX(), 0.4, p.getZ()),
                        new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.6f, 0.6f, 0.6f).glow(120, 220, 255).interpolation(15, 0);
                lanterns.add(h);
            }
        }

        private void addLetter(double offsetX, double[][] nodes) {
            for (double[] n : nodes) {
                letterPath.add(new Vector(offsetX + n[0], 0.2, -1.2 + n[1]));
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Constant: trace letters with soul fire flame between consecutive lanterns
            if (tick % 2 == 0) {
                double pulse = 0.7 + 0.3 * Math.sin(tick * 0.1);
                for (int i = 0; i < letterPath.size() - 1; i++) {
                    Vector a = letterPath.get(i);
                    Vector b = letterPath.get(i + 1);
                    if (a.distance(b) > 4.5) continue;
                    int steps = (int) (a.distance(b) * 4);
                    for (int s = 0; s <= steps; s++) {
                        if (Math.random() > pulse) continue;
                        double t = (double) s / Math.max(1, steps);
                        double x = c.getX() + a.getX() + (b.getX() - a.getX()) * t;
                        double z = c.getZ() + a.getZ() + (b.getZ() - a.getZ()) * t;
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                                x, c.getY() + 0.15, z, 1, 0, 0.02, 0, 0.0);
                    }
                }
            }

            // Periodically pick a letter to "extinguish then relight" with crit trail
            if (tick % 80 == 0) {
                retraceLetter = (int) (Math.random() * 4);
                retraceProgress = 0;
                DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.4f);
            }
            if (retraceLetter >= 0) {
                int startIdx = letterStartIdx(retraceLetter);
                int endIdx   = letterStartIdx(retraceLetter + 1);
                int len = endIdx - startIdx - 1;
                if (len > 0 && retraceProgress / 4 < len) {
                    Vector a = letterPath.get(startIdx + retraceProgress / 4);
                    Vector b = letterPath.get(startIdx + retraceProgress / 4 + 1);
                    double t = (retraceProgress % 4) / 4.0;
                    double x = c.getX() + a.getX() + (b.getX() - a.getX()) * t;
                    double z = c.getZ() + a.getZ() + (b.getZ() - a.getZ()) * t;
                    w.spawnParticle(Particle.CRIT, x, c.getY() + 0.5, z, 6, 0.2, 0.2, 0.2, 0.05);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, x, c.getY() + 0.2, z, 3, 0.1, 0.05, 0.1, 0.0);
                    retraceProgress++;
                } else {
                    retraceLetter = -1;
                }
            }

            // Lantern flicker
            if (tick % 12 == 0) {
                for (ItemDisplayHandle h : lanterns) {
                    int g = 180 + (int) (Math.random() * 75);
                    h.glow(120, g, 255);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_SOUL_SAND_STEP, 0.5f, 0.7f);
            }

            // Damage
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        private int letterStartIdx(int letter) {
            int[] sizes = { 5, 2, 4, 6 };
            int idx = 0;
            for (int i = 0; i < letter && i < 4; i++) idx += sizes[i];
            if (letter >= 4) idx = letterPath.size();
            return idx;
        }

        @Override
        public AbstractAttack newInstance() { return new DevilsSignature(plugin); }
    }

    // ============================================================
    // 26. VOID PEEL — active hazard
    //     6 tilted blackstone-item peels growing outward + 8 dark
    //     stained-glass-item rim accents revealing void portal particles.
    // ============================================================
    public static class VoidPeel extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> peels = new ArrayList<>();
        private final List<ItemDisplayHandle> rim = new ArrayList<>();

        public VoidPeel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_peel", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(28);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(420);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.7f, 0.5f);

            // 6 peel strips around perimeter — start small, grow over time
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 * i) / 6;
                double rx = Math.cos(a) * 6.5;
                double rz = Math.sin(a) * 6.5;
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(rx, 1.8, rz), new ItemStack(Material.BLACKSTONE));
                h.scale(0.1f, 0.1f, 0.1f).rotate((float) (a + Math.PI / 5), 0, 0, 1)
                        .glow(40, 0, 80).interpolation(40, 0);
                peels.add(h);
            }

            // Rim backing — 8 deep purple stained glass items behind peels for "void" depth
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 * i) / 8;
                double rx = Math.cos(a) * 7.3;
                double rz = Math.sin(a) * 7.3;
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(rx, 1.8, rz), new ItemStack(Material.BLACK_STAINED_GLASS));
                h.scale(0.05f, 0.05f, 0.05f).glow(60, 0, 120).interpolation(40, 0);
                rim.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Phase: peels grow over first ~180 ticks
            if (tick % 30 == 0 && tick <= 180) {
                float scale = Math.min(1.5f, 0.1f + (tick / 180f) * 1.5f);
                for (int i = 0; i < peels.size(); i++) {
                    double a = (Math.PI * 2 * i) / 6;
                    float tx = (float) (Math.cos(a) * 6.5) - 0.2f;
                    float tz = (float) (Math.sin(a) * 6.5) - 0.6f;
                    peels.get(i).animateTo(
                            new Vector3f(tx, 1.3f, tz),
                            new AxisAngle4f((float) (a + Math.PI / 4 + tick * 0.005), 0, 1, 0),
                            new Vector3f(0.4f, 2.5f, scale * 1.2f), 30);
                }
                for (int i = 0; i < rim.size(); i++) {
                    double a = (Math.PI * 2 * i) / 8;
                    float tx = (float) (Math.cos(a) * 7.3) - 0.15f;
                    float tz = (float) (Math.sin(a) * 7.3) - 0.75f;
                    rim.get(i).animateTo(
                            new Vector3f(tx, 1.3f, tz),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.3f, 3.0f, scale * 1.5f), 30);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.9f, 0.5f + tick * 0.001f);
            }

            // Per-tick particles: portal + end_rod + reverse_portal in the void gaps
            if (tick % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = (Math.PI * 2 * i) / 6;
                    double rx = c.getX() + Math.cos(a) * 6.5;
                    double rz = c.getZ() + Math.sin(a) * 6.5;
                    for (int n = 0; n < 4; n++) {
                        double oy = (Math.random() - 0.5) * 2.5;
                        w.spawnParticle(Particle.PORTAL, rx, c.getY() + 1.8 + oy, rz, 1,
                                0.3, 0.3, 0.3, 0.3);
                        if (Math.random() < 0.4)
                            w.spawnParticle(Particle.END_ROD, rx, c.getY() + 1.8 + oy, rz, 1,
                                    0.05, 0.05, 0.05, 0.02);
                        if (Math.random() < 0.3)
                            w.spawnParticle(Particle.REVERSE_PORTAL, rx, c.getY() + 1.8 + oy, rz, 1,
                                    0.05, 0.05, 0.05, 0.0);
                    }
                    if (Math.random() < 0.5)
                        w.spawnParticle(Particle.ELECTRIC_SPARK,
                                rx + (Math.random() - 0.5) * 0.5,
                                c.getY() + 1.8 + (Math.random() - 0.5) * 2.5,
                                rz + (Math.random() - 0.5) * 0.5,
                                1, 0, 0, 0, 0.05);
                }
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 0.4f);
            }

            // Damage
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new VoidPeel(plugin); }
    }

    // ============================================================
    // 27. THE INFERNAL COMPASS STORM — atmosphere
    //     Central magma-item hub + 4 NSEW marker items at radius 8.
    //     4 elemental beam particle streams rotate.
    // ============================================================
    public static class InfernalCompassStorm extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> hub = new ArrayList<>();
        private final List<ItemDisplayHandle> markers = new ArrayList<>();

        public InfernalCompassStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_compass_storm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(35);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(420);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.5f);

            // Central hub — magma-item core + 4 shroomlight item satellites, raised
            Location hubCenter = center.clone().add(0, 5.5, 0);
            ItemDisplayHandle core = displayBuilder.spawnItem(hubCenter, new ItemStack(Material.MAGMA_BLOCK));
            core.scale(1.2f, 1.2f, 1.2f).glow(255, 100, 0).interpolation(20, 0);
            hub.add(core);
            double[][] off = { {0.6, 0, 0}, {-0.6, 0, 0}, {0, 0, 0.6}, {0, 0, -0.6} };
            for (double[] o : off) {
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        hubCenter.clone().add(o[0], 0, o[2]), new ItemStack(Material.SHROOMLIGHT));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 200, 80).interpolation(20, 0);
                hub.add(h);
            }

            // 4 directional markers (NSEW) at radius 8 on floor
            Material[] mm = { Material.SOUL_LANTERN, Material.MAGMA_BLOCK,
                    Material.AMETHYST_SHARD, Material.OBSIDIAN };
            int[][] mc = { {255, 255, 255}, {255, 100, 0}, {200, 220, 80}, {120, 120, 120} };
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI / 2) * i;
                double x = Math.cos(a) * 8.0;
                double z = Math.sin(a) * 8.0;
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(x, 0.3, z), new ItemStack(mm[i]));
                h.scale(0.6f, 0.4f, 0.6f).glow(mc[i][0], mc[i][1], mc[i][2]).interpolation(15, 0);
                markers.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            Location hubCenter = c.clone().add(0, 5.5, 0);
            // Rotation accelerates
            double omega = 0.02 + tick * 0.00012;
            double angle = tick * omega;

            // 4 streams sweep outward from hub down to floor — particle line
            for (int s = 0; s < 4; s++) {
                double a = angle + (Math.PI / 2) * s;
                Vector dir = new Vector(Math.cos(a), 0, Math.sin(a));
                Particle p;
                switch (s) {
                    case 0: p = Particle.SOUL_FIRE_FLAME; break;
                    case 1: p = Particle.LAVA;           break;
                    case 2: p = Particle.ELECTRIC_SPARK;  break;
                    default: p = Particle.LARGE_SMOKE;   break;
                }
                int steps = 28;
                for (int i = 0; i <= steps; i++) {
                    double t = i / (double) steps;
                    double bx = hubCenter.getX() + dir.getX() * t * 8.5;
                    double by = hubCenter.getY() - t * 5.4;
                    double bz = hubCenter.getZ() + dir.getZ() * t * 8.5;
                    w.spawnParticle(p, bx, by, bz, 1, 0, 0, 0, 0);
                }
            }

            // Floor sweep paint trail behind each beam — dust ring at sweep point
            if (tick % 2 == 0) {
                for (int s = 0; s < 4; s++) {
                    double a = angle + (Math.PI / 2) * s;
                    double fx = c.getX() + Math.cos(a) * 8.5;
                    double fz = c.getZ() + Math.sin(a) * 8.5;
                    int cr, cg, cb;
                    switch (s) {
                        case 0:  cr = 255; cg = 150; cb = 200; break;
                        case 1:  cr = 255; cg = 127; cb = 0;   break;
                        case 2:  cr = 144; cg = 208; cb = 128; break;
                        default: cr = 120; cg = 120; cb = 140; break;
                    }
                    DisplayBuilder.dustParticles(new Location(w, fx, c.getY() + 0.1, fz),
                            5, 0.4, cr, cg, cb, 1.4f);
                }
            }

            // Hub idle particles
            if (tick % 4 == 0) {
                w.spawnParticle(Particle.LAVA, hubCenter.getX(), hubCenter.getY() + 0.5,
                        hubCenter.getZ(), 2, 0.4, 0.2, 0.4, 0.0);
                w.spawnParticle(Particle.FLAME, hubCenter.getX(), hubCenter.getY() + 0.5,
                        hubCenter.getZ(), 4, 0.3, 0.2, 0.3, 0.02);
            }

            // Hub slow rotation
            if (tick % 10 == 0) {
                hub.get(0).animateTo(
                        new Vector3f(-0.6f, 5.5f - 0.6f, -0.6f),
                        new AxisAngle4f((float) angle, 0, 1, 0),
                        new Vector3f(1.2f, 1.2f, 1.2f), 10);
            }

            // Cardinal cross-overs — when beam passes a marker, thunder
            int quadrant = (int) ((angle / (Math.PI / 2)) % 4 + 4) % 4;
            if (Math.abs((angle % (Math.PI / 2))) < omega * 1.5) {
                DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.2f);
            }

            // Pulse marker glow when beam crosses
            if (tick % 6 == 0 && quadrant >= 0 && quadrant < markers.size()) {
                markers.get(quadrant).glow(255, 220, 80);
            }

            // Damage
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new InfernalCompassStorm(plugin); }
    }

    // ============================================================
    // 28. CASCADING STAIRCASE TO NOWHERE — atmosphere
    //     15 stair-step cobbled-deepslate item displays climbing at 45°.
    //     Top step periodically scales to 0 (crumbles) and reforms.
    // ============================================================
    public static class StaircaseToNowhere extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> steps = new ArrayList<>();
        private int crumbleStep = 14;

        public StaircaseToNowhere(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("staircase_to_nowhere", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(40);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(480);
            config.setCooldownTicks(560);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.5f);

            // 15 steps at 45° heading +X/+Y
            for (int i = 0; i < 15; i++) {
                Location l = center.clone().add(i * 0.8, i * 0.45, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(l, new ItemStack(Material.COBBLED_DEEPSLATE));
                h.scale(1.2f, 0.45f, 1.4f).glow(60, 60, 80).interpolation(20, 0);
                steps.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Periodically crumble top step — every 60 ticks scale-to-0 then back
            if (tick % 60 == 0) {
                crumbleStep = 14;
                steps.get(crumbleStep).animateTo(
                        new Vector3f(14 * 0.8f - 0.6f, 14 * 0.45f - 0.225f, -0.7f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.01f, 0.01f, 0.01f), 15);
                Location top = c.clone().add(14 * 0.8, 14 * 0.45 + 0.4, 0);
                w.spawnParticle(Particle.LARGE_SMOKE, top, 30, 0.6, 0.3, 0.6, 0.04);
                w.spawnParticle(Particle.BLOCK,       top, 25, 0.6, 0.3, 0.6, 0,
                        Material.COBBLED_DEEPSLATE.createBlockData());
                DisplayBuilder.playSound(top, Sound.BLOCK_DEEPSLATE_BREAK, 1.1f, 0.7f);
                DisplayBuilder.playSound(top, Sound.ENTITY_GHAST_WARN, 0.6f, 1.1f);
            }
            if (tick % 60 == 30) {
                // Reform
                steps.get(crumbleStep).animateTo(
                        new Vector3f(14 * 0.8f - 0.6f, 14 * 0.45f - 0.225f, -0.7f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.2f, 0.45f, 1.4f), 15);
            }

            // Ash from sides of each step
            if (tick % 3 == 0) {
                for (int i = 0; i < steps.size(); i++) {
                    double sx = c.getX() + i * 0.8;
                    double sy = c.getY() + i * 0.45;
                    if (Math.random() < 0.6) {
                        w.spawnParticle(Particle.ASH,
                                sx + (Math.random() - 0.5) * 1.3,
                                sy + 0.3,
                                c.getZ() + (Math.random() - 0.5) * 1.3,
                                1, 0.05, 0.05, 0.05, 0.0);
                    }
                }
            }

            // Underside large_smoke clinging
            if (tick % 4 == 0) {
                for (int i = 0; i < steps.size(); i++) {
                    double sx = c.getX() + i * 0.8;
                    double sy = c.getY() + i * 0.45 - 0.4;
                    w.spawnParticle(Particle.LARGE_SMOKE,
                            sx + (Math.random() - 0.5) * 0.8,
                            sy,
                            c.getZ() + (Math.random() - 0.5) * 0.8,
                            1, 0.05, 0.0, 0.05, 0.01);
                }
            }

            // Soul wisps escaping each step's seams
            if (tick % 6 == 0) {
                for (int i = 0; i < steps.size(); i++) {
                    if (Math.random() < 0.4) {
                        double sx = c.getX() + i * 0.8;
                        double sy = c.getY() + i * 0.45 + 0.3;
                        w.spawnParticle(Particle.SCULK_SOUL,
                                sx, sy, c.getZ() + (Math.random() - 0.5) * 0.6,
                                1, 0.05, 0.1, 0.05, 0.0);
                    }
                }
            }

            // Invisible footstep echo every 35 ticks moving up the stairs
            if (tick % 35 == 0) {
                int idx = (tick / 35) % 15;
                Location f = c.clone().add(idx * 0.8, idx * 0.45 + 0.6, 0);
                DisplayBuilder.playSound(f, Sound.BLOCK_DEEPSLATE_STEP, 1.0f, 0.8f);
            }

            // Damage
            if (tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage()); p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new StaircaseToNowhere(plugin); }
    }

    // ============================================================
    // 29. THE HEARTBEAT FLOOR — active hazard
    //     14 red-concrete item displays in a heart-shape around center.
    //     Pulses (ba-BUM) sweep soul flame outward; each beat triggers
    //     impact damage. Tempo accelerates over time.
    // ============================================================
    public static class HeartbeatFloor extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> heartShape = new ArrayList<>();
        private int beatCounter = 0;

        public HeartbeatFloor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("heartbeat_floor", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(25);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(440);
            config.setCooldownTicks(520);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.7f);

            // 14-point heart-shape using parametric heart curve (scaled)
            for (int i = 0; i < 14; i++) {
                double t = (Math.PI * 2 * i) / 14;
                double hx = 16 * Math.pow(Math.sin(t), 3);
                double hz = 13 * Math.cos(t)
                        - 5 * Math.cos(2 * t)
                        - 2 * Math.cos(3 * t)
                        - Math.cos(4 * t);
                hx *= 0.18;
                hz *= 0.18;
                ItemDisplayHandle h = displayBuilder.spawnItem(
                        center.clone().add(hx, 0.3, hz),
                        new ItemStack(Material.RED_CONCRETE));
                h.scale(0.7f, 0.3f, 0.7f).glow(220, 0, 30).interpolation(8, 0);
                heartShape.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Tempo: starts ~40 ticks/beat, accelerates to ~18 ticks/beat by end
            int beatInterval = Math.max(18, 40 - tick / 24);

            // Double-beat (ba-BUM)
            if (tick % beatInterval == 0) {
                beatCounter++;
                doPulse(c, w, false);
            }
            if (tick % beatInterval == 5) {
                doPulse(c, w, true);
            }

            // Constant heart edge dripping_obsidian_tear + blood dust
            if (tick % 3 == 0) {
                for (ItemDisplayHandle h : heartShape) {
                    Location l = h.entity().getLocation();
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR,
                            l.getX(), c.getY() + 0.5, l.getZ(),
                            1, 0.1, 0.1, 0.1, 0.0);
                    if (Math.random() < 0.25)
                        DisplayBuilder.dustParticles(
                                new Location(w, l.getX(), c.getY() + 0.4, l.getZ()),
                                1, 0.1, 140, 16, 16, 1.4f);
                }
            }

            // Pulse heart concrete glow with the beat
            if (tick % beatInterval == 2) {
                for (ItemDisplayHandle h : heartShape) {
                    h.glow(255, 40, 60);
                }
            } else if (tick % beatInterval == beatInterval / 2) {
                for (ItemDisplayHandle h : heartShape) {
                    h.glow(140, 0, 30);
                }
            }
        }

        private void doPulse(Location c, World w, boolean secondary) {
            // Outward soul flame ring — wave sweeping out
            double maxR = secondary ? 5.0 : 8.0;
            for (double r = 0.5; r <= maxR; r += 0.6) {
                int pts = (int) (r * 6);
                for (int i = 0; i < pts; i++) {
                    double a = (Math.PI * 2 * i) / pts;
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME,
                            c.getX() + Math.cos(a) * r,
                            c.getY() + 0.2 + Math.random() * 0.3,
                            c.getZ() + Math.sin(a) * r,
                            1, 0, 0.05, 0, 0.0);
                }
            }
            // Click sound
            DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SENSOR_CLICKING,
                    secondary ? 0.6f : 1.0f, secondary ? 1.4f : 0.9f);
            if (!secondary) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.35f, 0.4f);
            }

            // Pulse-only damage
            triggerImpactDamage(c.clone());
        }

        @Override
        public AbstractAttack newInstance() { return new HeartbeatFloor(plugin); }
    }

    // ============================================================
    // 30. COLLAPSING DIMENSION PILLARS — active hazard
    //     3 pillars × 12 obsidian-item tall + 4 corner accents (16/pillar).
    //     Each sinks on its own timer; on full vanish: impact damage + particle FX.
    // ============================================================
    public static class CollapsingDimensionPillars extends EnvironmentalAttack {
        private final List<List<ItemDisplayHandle>> pillars = new ArrayList<>();
        private final double[] pillarYBase = new double[3];
        private final double[] pillarSinkSpeed = new double[3];
        private final boolean[] pillarVanished = new boolean[3];
        private final Vector[] pillarOffsets = {
                new Vector(-7, 0, 0),
                new Vector(0, 0, 7),
                new Vector(7, 0, -3)
        };

        public CollapsingDimensionPillars(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("collapsing_dimension_pillars", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(7.5);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(30);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(560);
            config.setCooldownTicks(640);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.6f);

            for (int p = 0; p < 3; p++) {
                List<ItemDisplayHandle> pl = new ArrayList<>();
                Location base = center.clone().add(pillarOffsets[p].getX(), 8, pillarOffsets[p].getZ());
                pillarYBase[p] = 8.0;
                pillarSinkSpeed[p] = 0.012 + p * 0.004;
                pillarVanished[p] = false;

                // 12-block central column (alternate obsidian / crying obsidian items)
                for (int y = 0; y < 12; y++) {
                    Material m = (y % 3 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                    ItemDisplayHandle h = displayBuilder.spawnItem(
                            base.clone().add(0, y, 0), new ItemStack(m));
                    h.scale(1.3f, 1.0f, 1.3f).glow(80, 0, 200).interpolation(20, 0);
                    pl.add(h);
                }
                // 4 corner accents at heights 2, 5, 8, 11
                int[] accH = { 2, 5, 8, 11 };
                double[][] corners = { {0.7,0.7}, {-0.7,0.7}, {0.7,-0.7}, {-0.7,-0.7} };
                for (int i = 0; i < 4; i++) {
                    ItemDisplayHandle h = displayBuilder.spawnItem(
                            base.clone().add(corners[i][0], accH[i], corners[i][1]),
                            new ItemStack(Material.CRYING_OBSIDIAN));
                    h.scale(0.6f, 0.6f, 0.6f).glow(150, 50, 230).interpolation(20, 0);
                    pl.add(h);
                }
                pillars.add(pl);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Sink each pillar by its own rate
            for (int p = 0; p < 3; p++) {
                if (pillarVanished[p]) continue;

                if (tick % 20 == 0) {
                    pillarYBase[p] -= pillarSinkSpeed[p] * 20;
                    Vector off = pillarOffsets[p];

                    List<ItemDisplayHandle> pl = pillars.get(p);
                    for (int i = 0; i < pl.size(); i++) {
                        if (i < 12) {
                            float ty = (float) pillarYBase[p] + i;
                            pl.get(i).animateTo(
                                    new Vector3f((float) off.getX() - 0.65f,
                                                 ty - 0.5f,
                                                 (float) off.getZ() - 0.65f),
                                    new AxisAngle4f(0, 0, 1, 0),
                                    new Vector3f(1.3f, 1.0f, 1.3f), 20);
                        } else {
                            int idx = i - 12;
                            int[] accH = { 2, 5, 8, 11 };
                            double[][] corners = { {0.7,0.7}, {-0.7,0.7}, {0.7,-0.7}, {-0.7,-0.7} };
                            float ty = (float) pillarYBase[p] + accH[idx];
                            pl.get(i).animateTo(
                                    new Vector3f((float) (off.getX() + corners[idx][0]) - 0.3f,
                                                 ty - 0.3f,
                                                 (float) (off.getZ() + corners[idx][1]) - 0.3f),
                                    new AxisAngle4f(0, 0, 1, 0),
                                    new Vector3f(0.6f, 0.6f, 0.6f), 20);
                        }
                    }
                }

                // Check vanish — top of pillar reaches floor (y_base + 12 < 0.5)
                if (pillarYBase[p] + 12 < 0.5) {
                    pillarVanished[p] = true;
                    Location vanishLoc = c.clone().add(pillarOffsets[p].getX(), 0,
                            pillarOffsets[p].getZ());

                    // Massive smoke + reverse_portal + electric_spark explosion
                    w.spawnParticle(Particle.LARGE_SMOKE, vanishLoc, 80, 1.5, 1.0, 1.5, 0.1);
                    w.spawnParticle(Particle.REVERSE_PORTAL, vanishLoc, 60, 1.2, 0.8, 1.2, 0.1);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, vanishLoc, 60, 1.5, 1.0, 1.5, 0.4);
                    w.spawnParticle(Particle.PORTAL,      vanishLoc, 100, 2.0, 1.5, 2.0, 1.0);
                    DisplayBuilder.playSound(vanishLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 0.7f);
                    DisplayBuilder.playSound(vanishLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.5f);

                    // Impact damage
                    triggerImpactDamage(vanishLoc);

                    // Hide pillar's items
                    for (ItemDisplayHandle h : pillars.get(p)) {
                        h.scale(0.0f, 0.0f, 0.0f);
                    }
                }
            }

            // Per-pillar swirling portal particles
            if (tick % 2 == 0) {
                for (int p = 0; p < 3; p++) {
                    if (pillarVanished[p]) continue;
                    double yb = pillarYBase[p];
                    Vector off = pillarOffsets[p];
                    for (int n = 0; n < 6; n++) {
                        double a = (tick * 0.15) + (n * Math.PI / 3) + p * 0.7;
                        double r = 1.2 + Math.sin(tick * 0.05 + n) * 0.3;
                        double x = c.getX() + off.getX() + Math.cos(a) * r;
                        double y = c.getY() + yb + (n * 2.0);
                        double z = c.getZ() + off.getZ() + Math.sin(a) * r;
                        w.spawnParticle(Particle.PORTAL, x, y, z, 1, 0.05, 0.5, 0.05, 0.4);
                        if (Math.random() < 0.3)
                            w.spawnParticle(Particle.DRAGON_BREATH, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
                    }
                }
            }

            // Periodic ambient
            if (tick % 60 == 0) {
                for (int p = 0; p < 3; p++) {
                    if (pillarVanished[p]) continue;
                    Vector off = pillarOffsets[p];
                    DisplayBuilder.playSound(
                            c.clone().add(off.getX(), 4, off.getZ()),
                            Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.5f);
                }
            }
        }

        @Override
        public AbstractAttack newInstance() { return new CollapsingDimensionPillars(plugin); }
    }
}
