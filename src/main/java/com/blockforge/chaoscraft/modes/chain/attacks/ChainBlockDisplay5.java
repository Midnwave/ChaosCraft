package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain Mode — BLOCK DISPLAY ATTACKS 41-50 (Reaching & Projectile, part 1)
 *
 * Long-reach projectile-style chain attacks. Spawn at arena perimeter, travel
 * across the arena, impact at the target point. Two of these (Chain Drill and
 * Chain Serpent) use the AbstractAttack.tickFollowAI() infrastructure for
 * sub-walk pursuit instead of a one-shot ballistic trajectory.
 *
 * Block palette: IRON_BLOCK, NETHERITE_BLOCK, CHAIN, GRAY_CONCRETE,
 *                DARK_OAK_LOG, POLISHED_BLACKSTONE, CHISELED_POLISHED_BLACKSTONE
 *
 * Particles: BLOCK_CRACK(IRON_BLOCK/NETHERITE_BLOCK/CHAIN), CRIT, SMOKE_NORMAL,
 *            LARGE_SMOKE, ELECTRIC_SPARK, LAVA, SOUL_FIRE_FLAME, EXPLOSION
 * Sounds: BLOCK_CHAIN_FALL, BLOCK_CHAIN_HIT, BLOCK_ANVIL_LAND,
 *         BLOCK_NETHERITE_BLOCK_HIT, ENTITY_IRON_GOLEM_ATTACK,
 *         BLOCK_GRINDSTONE_USE, ITEM_TRIDENT_RIPTIDE_1, ITEM_TRIDENT_RIPTIDE_3
 *
 * Attacks:
 *  41. ChainCarousel       — 4-arm rotating ride (timing-through-gaps), constant
 *  42. IronSphereLattice   — caged latitude/longitude sphere, constant
 *  43. ChainGrinderWheels  — 4 wheel zone, constant
 *  44. ChainLasso          — flying contracting ring, impact-only
 *  45. IronJavelin         — predictive harpoon, impact-only
 *  46. HookAndChain        — pull-toward hook (applies velocity), impact-only
 *  47. ChainComet          — streaking ball + tail, impact-only
 *  48. IronChakram         — boomerang disc, constant return arc
 *  49. ChainDrill          — advancing drill, constant + FOLLOW-AI 0.08
 *  50. ChainSerpent        — slithering chain snake, constant + FOLLOW-AI 0.06
 */
public final class ChainBlockDisplay5 {
    private ChainBlockDisplay5() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ChainCarousel(plugin));
        registry.register(new IronSphereLattice(plugin));
        registry.register(new ChainGrinderWheels(plugin));
        registry.register(new ChainLasso(plugin));
        registry.register(new IronJavelin(plugin));
        registry.register(new HookAndChain(plugin));
        registry.register(new ChainComet(plugin));
        registry.register(new IronChakram(plugin));
        registry.register(new ChainDrill(plugin));
        registry.register(new ChainSerpent(plugin));
    }

    // ================================================================
    // Shared helpers
    // ================================================================

    /** Find the nearest non-exempt survival player within range. */
    private static Player findNearestPlayer(Location center, double range) {
        if (center == null || center.getWorld() == null) return null;
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

    /** Smoothly set the scale on a BlockDisplay over duration ticks. */
    private static void setScale(BlockDisplay e, float sx, float sy, float sz, int dur) {
        if (e == null || !e.isValid()) return;
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f().set(t.getLeftRotation()),
                new Vector3f(sx, sy, sz),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    /** Smoothly rotate a BlockDisplay around a given axis over duration ticks. */
    private static void rotateOn(BlockDisplay e, float angle, float ax, float ay, float az, int dur) {
        if (e == null || !e.isValid()) return;
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f(angle, ax, ay, az),
                t.getScale(),
                new AxisAngle4f(0, 0, 1, 0)
        ));
    }

    /** Smoothly shrink any handle to zero scale over duration. */
    private static void shrinkToZero(BlockDisplayHandle h, int dur) {
        if (h == null) return;
        setScale(h.entity(), 0f, 0f, 0f, dur);
    }

    /** Teleport a display by an absolute world delta (used for projectile motion). */
    private static void teleportBy(BlockDisplay e, double dx, double dy, double dz) {
        if (e == null || !e.isValid()) return;
        try {
            e.teleport(e.getLocation().add(dx, dy, dz));
        } catch (Throwable ignored) {}
    }

    // ================================================================
    // #41 — CHAIN CAROUSEL ("The Ride")
    // 4 horizontal arms rotating around a central hub at shoulder height.
    // Players time their step through the gap between arms.
    // 32 blocks: 1 hub + 4 ceiling chains + 4×(4 chain links + 1 netherite tip) + 7 ring beams.
    // ================================================================
    public static class ChainCarousel extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> hub = new ArrayList<>();
        private final List<BlockDisplayHandle> ceilingChains = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> arms = new ArrayList<>();
        private final List<BlockDisplayHandle> armTips = new ArrayList<>();
        private final List<BlockDisplayHandle> ringBeams = new ArrayList<>();
        private float carouselAngle = 0f;

        public ChainCarousel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_carousel", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(280.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(280);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Rotating ride (timing through gaps)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Hub at shoulder height (y+1.4), netherite block 0.0 -> 0.65 grow
            BlockDisplayHandle hubCore = displayBuilder.spawnBlock(
                    center.clone().add(0, 1.4, 0), Material.NETHERITE_BLOCK);
            hubCore.scale(0f, 0f, 0f).glow(70, 70, 80).interpolation(10, 0);
            spawnedEntities.add(hubCore.entity());
            hub.add(hubCore);
            setScale(hubCore.entity(), 0.65f, 0.5f, 0.65f, 12);

            // 4 ceiling chains tying hub to the sky
            for (int i = 0; i < 4; i++) {
                double y = 2.2 + i * 0.7;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(8, i * 2);
                spawnedEntities.add(h.entity());
                ceilingChains.add(h);
                setScale(h.entity(), 0.18f, 0.5f, 0.18f, 10);
            }

            // 4 arms, 90 degrees apart, each: 4 chain links + 1 netherite tip
            for (int armIdx = 0; armIdx < 4; armIdx++) {
                List<BlockDisplayHandle> arm = new ArrayList<>();
                double baseAngle = armIdx * (Math.PI / 2.0);
                for (int link = 0; link < 4; link++) {
                    double r = 0.6 + link * 0.6;
                    double x = Math.cos(baseAngle) * r;
                    double z = Math.sin(baseAngle) * r;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 1.4, z), Material.CHAIN);
                    h.scale(0f, 0f, 0f).glow(170, 170, 185).interpolation(10, 2 + link);
                    spawnedEntities.add(h.entity());
                    arm.add(h);
                    setScale(h.entity(), 0.18f, 0.18f, 0.5f, 12);
                }
                // Netherite tip at 3.0 radius
                double tipX = Math.cos(baseAngle) * 3.0;
                double tipZ = Math.sin(baseAngle) * 3.0;
                BlockDisplayHandle tip = displayBuilder.spawnBlock(
                        center.clone().add(tipX, 1.4, tipZ), Material.NETHERITE_BLOCK);
                tip.scale(0f, 0f, 0f).glow(60, 60, 75).interpolation(10, 6);
                spawnedEntities.add(tip.entity());
                arm.add(tip);
                armTips.add(tip);
                setScale(tip.entity(), 0.48f, 0.48f, 0.48f, 14);
                arms.add(arm);
            }

            // 7 small iron ring beams under the hub for ride-platform feel
            for (int i = 0; i < 7; i++) {
                double angle = (Math.PI * 2.0 * i) / 7.0;
                Location loc = center.clone().add(Math.cos(angle) * 0.55, 0.6, Math.sin(angle) * 0.55);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 210).interpolation(8, 4);
                spawnedEntities.add(h.entity());
                ringBeams.add(h);
                setScale(h.entity(), 0.22f, 0.1f, 0.22f, 10);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.4f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.0f, 0.6f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 1.4, 0), 30, 1.5, 0.6, 1.5, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Carousel rotation: 8 deg/tick after warm-up at tick 20
            if (tick > 20) {
                carouselAngle += (float) Math.toRadians(8.0);
                for (int armIdx = 0; armIdx < arms.size(); armIdx++) {
                    List<BlockDisplayHandle> arm = arms.get(armIdx);
                    double baseAngle = armIdx * (Math.PI / 2.0) + carouselAngle;
                    for (int link = 0; link < arm.size(); link++) {
                        double r = (link == arm.size() - 1) ? 3.0 : 0.6 + link * 0.6;
                        double x = Math.cos(baseAngle) * r;
                        double z = Math.sin(baseAngle) * r;
                        double bob = (link == arm.size() - 1)
                                ? Math.sin((tick + armIdx * 6) * 0.18) * 0.08
                                : 0.0;
                        Location dest = c.clone().add(x, 1.4 + bob, z);
                        BlockDisplay e = arm.get(link).entity();
                        if (e == null || !e.isValid()) continue;
                        try { e.teleport(dest); } catch (Throwable ignored) {}
                    }
                }
            }

            // CRIT trails on tips every 2 ticks
            if (tick > 20 && tick % 2 == 0) {
                for (BlockDisplayHandle tip : armTips) {
                    c.getWorld().spawnParticle(Particle.CRIT, tip.entity().getLocation(), 4, 0.2, 0.2, 0.2, 0.05);
                }
            }

            // Mechanical chain step every 10t
            if (tick > 20 && tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.9f);
            }
            // Heavy creak every 35t
            if (tick > 20 && tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.5f);
            }
            // Ambient smoke at hub
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 1.4, 0), 3, 0.4, 0.3, 0.4, 0.01);
            }

            // Dissipate: arms collapse outward + shrink, ticks 235-260
            int duration = config.getDurationTicks();
            if (tick == duration - 25) {
                for (BlockDisplayHandle tip : armTips) shrinkToZero(tip, 10);
            }
            if (tick == duration - 15) {
                for (List<BlockDisplayHandle> arm : arms) {
                    for (BlockDisplayHandle h : arm) shrinkToZero(h, 12);
                }
                for (BlockDisplayHandle h : ceilingChains) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : ringBeams) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : hub) shrinkToZero(h, 12);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.3f, 0.4f);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 1.4, 0), 25, 1.5, 0.8, 1.5, 0.04);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainCarousel(plugin); }
    }

    // ================================================================
    // #42 — IRON SPHERE LATTICE ("The Cage Ball")
    // Caged latitude/longitude sphere with a central core. Tilted axis spin.
    // 34 blocks: 12 equator + 8 meridA + 8 meridB + 5 core + 1 anchor (collar at top).
    // ================================================================
    public static class IronSphereLattice extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> equator = new ArrayList<>();
        private final List<BlockDisplayHandle> meridA = new ArrayList<>();
        private final List<BlockDisplayHandle> meridB = new ArrayList<>();
        private final List<BlockDisplayHandle> core = new ArrayList<>();
        private final List<BlockDisplayHandle> anchors = new ArrayList<>();
        private float spinAngle = 0f;

        public IronSphereLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_sphere_lattice", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(260.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
            config.setChance(6);
            config.setEnabled(true);
            config.setDesignType("Caged sphere (enclosure escape)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double r = 2.4;
            double cy = 2.4;

            // 12 equator rings (IRON_BLOCK flat slabs)
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12.0;
                double x = Math.cos(a) * r;
                double z = Math.sin(a) * r;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, cy, z), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(12, i / 2);
                spawnedEntities.add(h.entity());
                equator.add(h);
                setScale(h.entity(), 0.55f, 0.16f, 0.18f, 14);
            }
            // 8 meridian A (vertical great circle in XY plane)
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2.0 * i) / 8.0;
                double y = cy + Math.cos(a) * r;
                double xz = Math.sin(a) * r;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(xz, y, 0), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(195, 195, 205).interpolation(12, 5 + i);
                spawnedEntities.add(h.entity());
                meridA.add(h);
                setScale(h.entity(), 0.18f, 0.55f, 0.16f, 14);
            }
            // 8 meridian B (vertical great circle in ZY plane)
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2.0 * i) / 8.0;
                double y = cy + Math.cos(a) * r;
                double xz = Math.sin(a) * r;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, xz), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(195, 195, 205).interpolation(12, 9 + i);
                spawnedEntities.add(h.entity());
                meridB.add(h);
                setScale(h.entity(), 0.16f, 0.55f, 0.18f, 14);
            }
            // 5-block central core
            BlockDisplayHandle cCore = displayBuilder.spawnBlock(
                    center.clone().add(0, cy, 0), Material.NETHERITE_BLOCK);
            cCore.scale(0f, 0f, 0f).glow(60, 60, 70).interpolation(10, 4);
            spawnedEntities.add(cCore.entity());
            core.add(cCore);
            setScale(cCore.entity(), 0.5f, 0.5f, 0.5f, 12);
            double[][] poles = {{0.45, 0, 0}, {-0.45, 0, 0}, {0, 0, 0.45}, {0, 0, -0.45}};
            for (double[] off : poles) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(
                        center.clone().add(off[0], cy, off[2]), Material.NETHERITE_BLOCK);
                p.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(10, 6);
                spawnedEntities.add(p.entity());
                core.add(p);
                setScale(p.entity(), 0.32f, 0.32f, 0.32f, 12);
            }
            // Top anchor
            BlockDisplayHandle a = displayBuilder.spawnBlock(
                    center.clone().add(0, cy + r + 0.3, 0), Material.CHISELED_POLISHED_BLACKSTONE);
            a.scale(0f, 0f, 0f).glow(120, 120, 130).interpolation(10, 0);
            spawnedEntities.add(a.entity());
            anchors.add(a);
            setScale(a.entity(), 0.5f, 0.5f, 0.5f, 12);

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.3f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.1f, 0.7f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, cy, 0), 30, 2, 1.5, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double cy = 2.4;
            double r = 2.4;

            // Tilted spin (axis tilted 30 deg from vertical, around Y+X axis)
            if (tick > 20) {
                spinAngle += (float) Math.toRadians(3.5);
                // Update each ring point using a rotation matrix around tilted axis
                // For simplicity we rotate each ring as a whole: equator on Y, meridA on Y+slow tilt
                for (int i = 0; i < equator.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / 12.0 + spinAngle;
                    double x = Math.cos(a) * r;
                    double z = Math.sin(a) * r;
                    double tilt = Math.sin(spinAngle * 0.3) * 0.4;
                    BlockDisplay e = equator.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x, cy + tilt, z)); } catch (Throwable ignored) {}
                }
                for (int i = 0; i < meridA.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / 8.0;
                    double y = cy + Math.cos(a) * r;
                    double xz = Math.sin(a) * r;
                    // Spin the entire ring around Y
                    double sx = xz * Math.cos(spinAngle);
                    double sz = xz * Math.sin(spinAngle);
                    BlockDisplay e = meridA.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(sx, y, sz)); } catch (Throwable ignored) {}
                }
                for (int i = 0; i < meridB.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / 8.0;
                    double y = cy + Math.cos(a) * r;
                    double xz = Math.sin(a) * r;
                    // Spin the entire ring around Y with 90 deg offset
                    double sx = -xz * Math.sin(spinAngle);
                    double sz = xz * Math.cos(spinAngle);
                    BlockDisplay e = meridB.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(sx, y, sz)); } catch (Throwable ignored) {}
                }
            }

            // ELECTRIC_SPARK at ring intersections (top/bottom poles)
            if (tick > 20 && tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(0, cy + r, 0), 3, 0.4, 0.2, 0.4, 0.05);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(0, cy - r, 0), 3, 0.4, 0.2, 0.4, 0.05);
            }
            // Smoke envelope every 8t
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, cy, 0), 6, 1.5, 1.5, 1.5, 0.02);
            }
            // Mechanical hum every 20t
            if (tick > 20 && tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.4f, 0.5f);
            }
            if (tick > 20 && tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.7f, 0.6f);
            }

            // Dissipate: cage collapses into core, then implodes
            int duration = config.getDurationTicks();
            if (tick == duration - 20) {
                for (BlockDisplayHandle h : equator) shrinkToZero(h, 10);
                for (BlockDisplayHandle h : meridA) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : meridB) shrinkToZero(h, 12);
            }
            if (tick == duration - 8) {
                for (BlockDisplayHandle h : core) shrinkToZero(h, 6);
                for (BlockDisplayHandle h : anchors) shrinkToZero(h, 6);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.6f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, cy, 0), 6, 0.6, 0.6, 0.6, 0.05);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronSphereLattice(plugin); }
    }

    // ================================================================
    // #43 — CHAIN GRINDER WHEELS ("The Mill")
    // 3 vertical iron wheels spinning around the arena. Players must read
    // the wheels and find a lane between them.
    // 36 blocks: 3 wheels × (10 rim + 2 spokes) + 1 axle.
    // Wheels arranged in a line; each spins on its own axis.
    // ================================================================
    public static class ChainGrinderWheels extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> wheels = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> spokes = new ArrayList<>();
        private final List<BlockDisplayHandle> axle = new ArrayList<>();
        private final int[] wheelDirs = {1, -1, 1};
        private float[] wheelAngles = {0f, 0f, 0f};

        public ChainGrinderWheels(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_grinder_wheels", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(340.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(220);
            config.setCooldownTicks(260);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Multi-wheel zone (read wheels, find lane)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double wheelR = 1.5;
            double yMid = 2.0;
            // 3 wheels, spaced -3, 0, +3 along X axis (vertical wheels facing Z)
            for (int wi = 0; wi < 3; wi++) {
                double cx = -3.0 + wi * 3.0;
                List<BlockDisplayHandle> rim = new ArrayList<>();
                List<BlockDisplayHandle> spk = new ArrayList<>();
                // 10 rim slabs forming a circle in the XY plane (wheel faces Z)
                for (int i = 0; i < 10; i++) {
                    double a = (Math.PI * 2.0 * i) / 10.0;
                    double x = cx + Math.cos(a) * wheelR;
                    double y = yMid + Math.sin(a) * wheelR;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, y, 0), Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(12, wi * 4 + i / 2);
                    spawnedEntities.add(h.entity());
                    rim.add(h);
                    setScale(h.entity(), 0.55f, 0.16f, 0.22f, 14);
                }
                // 2 spokes per wheel (cross pattern)
                for (int s = 0; s < 2; s++) {
                    double a = s * Math.PI / 2.0;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(cx, yMid, 0), Material.IRON_BLOCK);
                    h.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(12, wi * 4 + 8);
                    spawnedEntities.add(h.entity());
                    spk.add(h);
                    setScale(h.entity(), 0.1f, (float) (wheelR * 0.95 * 2.0), 0.1f, 14);
                    rotateOn(h.entity(), (float) a, 0f, 0f, 1f, 14);
                }
                wheels.add(rim);
                spokes.add(spk);
            }

            // Axle (dark oak log spanning all 3 wheels)
            BlockDisplayHandle a = displayBuilder.spawnBlock(
                    center.clone().add(0, yMid, 0), Material.DARK_OAK_LOG);
            a.scale(0f, 0f, 0f).glow(110, 80, 50).interpolation(10, 0);
            spawnedEntities.add(a.entity());
            axle.add(a);
            setScale(a.entity(), 6.5f, 0.1f, 0.1f, 12);

            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.5f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_HIT, 1.0f, 0.5f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, yMid, 0), 35, 3, 1, 1.5, 0.04);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double wheelR = 1.5;
            double yMid = 2.0;

            // Spin wheels (alternating directions)
            if (tick > 20) {
                for (int wi = 0; wi < wheels.size(); wi++) {
                    wheelAngles[wi] += (float) Math.toRadians(7.5 * wheelDirs[wi]);
                    double cx = -3.0 + wi * 3.0;
                    List<BlockDisplayHandle> rim = wheels.get(wi);
                    for (int i = 0; i < rim.size(); i++) {
                        double a = (Math.PI * 2.0 * i) / 10.0 + wheelAngles[wi];
                        double x = cx + Math.cos(a) * wheelR;
                        double y = yMid + Math.sin(a) * wheelR;
                        BlockDisplay e = rim.get(i).entity();
                        if (e == null || !e.isValid()) continue;
                        try { e.teleport(c.clone().add(x, y, 0)); } catch (Throwable ignored) {}
                    }
                    // Spin spokes around the hub
                    List<BlockDisplayHandle> spk = spokes.get(wi);
                    for (int s = 0; s < spk.size(); s++) {
                        float angle = (float) (wheelAngles[wi] + s * Math.PI / 2.0);
                        rotateOn(spk.get(s).entity(), angle, 0f, 0f, 1f, 3);
                    }
                }
            }

            // Sparks fly outward from rims
            if (tick > 20 && tick % 3 == 0) {
                for (int wi = 0; wi < wheels.size(); wi++) {
                    double cx = -3.0 + wi * 3.0;
                    c.getWorld().spawnParticle(Particle.CRIT,
                            c.clone().add(cx, yMid, 0), 5, wheelR * 0.8, wheelR * 0.8, 0.2, 0.15);
                }
            }
            // Smoke between wheels
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(-1.5, yMid, 0), 4, 0.5, 0.6, 0.4, 0.02);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        c.clone().add(1.5, yMid, 0), 4, 0.5, 0.6, 0.4, 0.02);
            }
            // Grindstone continuous sound
            if (tick > 20 && tick % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.9f, 0.7f);
            }
            // Heavy chain rev every 30t
            if (tick > 20 && tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.7f, 0.6f);
            }

            // Dissipate: wheels fly apart radially then vanish
            int duration = config.getDurationTicks();
            if (tick == duration - 18) {
                for (List<BlockDisplayHandle> rim : wheels) {
                    for (BlockDisplayHandle h : rim) shrinkToZero(h, 12);
                }
                for (List<BlockDisplayHandle> spk : spokes) {
                    for (BlockDisplayHandle h : spk) shrinkToZero(h, 12);
                }
                for (BlockDisplayHandle h : axle) shrinkToZero(h, 12);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.3f, 0.5f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, yMid, 0), 8, 3, 0.5, 0.5, 0.05);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainGrinderWheels(plugin); }
    }

    // ================================================================
    // #44 — CHAIN LASSO ("The Throw")
    // A spinning chain ring contracts as it flies at the player. Impact-only.
    // 32 blocks: 12 ring + 8 trailing rope + 1 handle + 11 supporting structures
    // (anchor brace + grow-in particles).
    // ================================================================
    public static class ChainLasso extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ring = new ArrayList<>();
        private final List<BlockDisplayHandle> trail = new ArrayList<>();
        private final List<BlockDisplayHandle> handle = new ArrayList<>();
        private final List<BlockDisplayHandle> brace = new ArrayList<>();
        private Location origin;
        private Location targetLoc;
        private float spinAngle = 0f;

        public ChainLasso(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_lasso", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(280.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(200);
            config.setChance(7);
            config.setEnabled(true);
            config.setDesignType("Lassoing projectile (sidestep + tight window)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            Player target = findNearestPlayer(center, 50.0);
            targetLoc = (target != null) ? target.getLocation().clone() : center.clone();
            // Origin at perimeter, 14 blocks away on +X side
            origin = center.clone().add(14.0, 1.6, 0.0);

            // 12 chain ring at origin
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12.0;
                Location loc = origin.clone().add(Math.cos(a) * 1.2, 0, Math.sin(a) * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(8, i / 2);
                spawnedEntities.add(h.entity());
                ring.add(h);
                setScale(h.entity(), 0.22f, 0.22f, 0.5f, 10);
            }
            // 8 trailing rope chain links behind ring
            for (int i = 0; i < 8; i++) {
                Location loc = origin.clone().add(0.5 + i * 0.35, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(170, 170, 185).interpolation(8, 2 + i);
                spawnedEntities.add(h.entity());
                trail.add(h);
                setScale(h.entity(), 0.14f, 0.14f, 0.5f, 10);
            }
            // 1 handle (iron block)
            BlockDisplayHandle hand = displayBuilder.spawnBlock(
                    origin.clone().add(3.5, 0, 0), Material.IRON_BLOCK);
            hand.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(8, 0);
            spawnedEntities.add(hand.entity());
            handle.add(hand);
            setScale(hand.entity(), 0.36f, 0.36f, 0.36f, 10);
            // 11 anchor brace structures around origin to thicken effect
            double[][] braceOffsets = {
                    {3.5, 0.4, 0.2}, {3.5, 0.4, -0.2}, {3.5, -0.4, 0.2}, {3.5, -0.4, -0.2},
                    {3.8, 0.0, 0.5}, {3.8, 0.0, -0.5}, {4.0, 0.2, 0.0}, {4.0, -0.2, 0.0},
                    {3.6, 0.3, 0.0}, {3.6, -0.3, 0.0}, {3.4, 0.0, 0.0}
            };
            for (double[] off : braceOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        origin.clone().add(off[0], off[1], off[2]), Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(60, 60, 70).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                brace.add(h);
                setScale(h.entity(), 0.12f, 0.12f, 0.12f, 8);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.4f, 1.2f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.2f, 1.0f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, origin, 20, 0.8, 0.8, 0.8, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (origin == null || targetLoc == null) return;

            int flightTicks = 50;
            int impactTick = 55;

            // Flight phase: lerp ring + trail toward target, ring shrinks from r=1.2 -> 0.4
            if (tick >= 5 && tick <= flightTicks) {
                double progress = (tick - 5) / (double) (flightTicks - 5);
                progress = Math.min(1.0, progress);
                double ringRadius = 1.2 - 0.8 * progress;
                spinAngle += (float) Math.toRadians(25.0);

                double mx = origin.getX() + (targetLoc.getX() - origin.getX()) * progress;
                double my = origin.getY() + (targetLoc.getY() + 1.0 - origin.getY()) * progress;
                double mz = origin.getZ() + (targetLoc.getZ() - origin.getZ()) * progress;

                for (int i = 0; i < ring.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / 12.0 + spinAngle;
                    BlockDisplay e = ring.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try {
                        e.teleport(new Location(c.getWorld(),
                                mx + Math.cos(a) * ringRadius,
                                my,
                                mz + Math.sin(a) * ringRadius));
                    } catch (Throwable ignored) {}
                }
                // Trail spread back from ring toward origin
                for (int i = 0; i < trail.size(); i++) {
                    double frac = (i + 1.0) / (trail.size() + 1.0);
                    double tx = mx + (origin.getX() - mx) * frac;
                    double ty = my + (origin.getY() - my) * frac;
                    double tz = mz + (origin.getZ() - mz) * frac;
                    BlockDisplay e = trail.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(new Location(c.getWorld(), tx, ty, tz)); } catch (Throwable ignored) {}
                }

                // Particles trailing ring
                if (tick % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            new Location(c.getWorld(), mx, my, mz), 4, 0.4, 0.4, 0.4, 0.08);
                    c.getWorld().spawnParticle(Particle.CRIT,
                            new Location(c.getWorld(), mx, my, mz), 3, 0.3, 0.3, 0.3, 0.05);
                }
            }

            // Flight sound
            if (tick % 6 == 0 && tick < flightTicks) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 1.1f);
            }

            // Impact
            if (tick == impactTick) {
                triggerImpactDamage(targetLoc);
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_CHAIN_HIT, 1.5f, 0.9f);
                DisplayBuilder.playSound(targetLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.2f, 1.4f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 8, 0.8, 0.4, 0.8, 0.05);
                c.getWorld().spawnParticle(Particle.CRIT, targetLoc, 15, 1.0, 0.6, 1.0, 0.2);
                // Ring contraction to 0
                for (BlockDisplayHandle h : ring) shrinkToZero(h, 5);
            }
            // Cleanup vanish
            if (tick == 65) {
                for (BlockDisplayHandle h : trail) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : handle) shrinkToZero(h, 8);
                for (BlockDisplayHandle h : brace) shrinkToZero(h, 8);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainLasso(plugin); }
    }

    // ================================================================
    // #45 — IRON JAVELIN ("The Harpoon")
    // A heavy javelin spirals toward a predicted lead position.
    // 30 blocks: 10 shaft + 6 tip (2 core + 4 fins) + 1 tail fin + 6 chain trail
    //          + 7 ring fittings.
    // ================================================================
    public static class IronJavelin extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shaft = new ArrayList<>();
        private final List<BlockDisplayHandle> tip = new ArrayList<>();
        private final List<BlockDisplayHandle> tail = new ArrayList<>();
        private final List<BlockDisplayHandle> trail = new ArrayList<>();
        private final List<BlockDisplayHandle> rings = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private Location targetLoc;
        private float spiralAngle = 0f;
        private Vector flightDir;

        public IronJavelin(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_javelin", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(380.0);
            config.setImpactRadius(1.8);
            config.setDurationTicks(60);
            config.setCooldownTicks(180);
            config.setChance(7);
            config.setEnabled(true);
            config.setDesignType("Predictive harpoon (read lead aim)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            Player target = findNearestPlayer(center, 50.0);
            Location aim = (target != null) ? target.getLocation().clone() : center.clone();
            // Predictive lead: add 1s of velocity to aim
            if (target != null) {
                Vector v = target.getVelocity();
                aim = aim.clone().add(v.getX() * 20, 0, v.getZ() * 20);
            }
            targetLoc = aim.clone().add(0, 1.0, 0);
            // Origin at perimeter -14X side
            origin = center.clone().add(-14.0, 3.0, 0.0);

            // Direction
            double dx = targetLoc.getX() - origin.getX();
            double dy = targetLoc.getY() - origin.getY();
            double dz = targetLoc.getZ() - origin.getZ();
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < 0.01) dist = 1.0;
            flightDir = new Vector(dx / dist, dy / dist, dz / dist);

            // 10 shaft (DARK_OAK_LOG) along travel axis (we place at origin and animate)
            for (int i = 0; i < 10; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.DARK_OAK_LOG);
                h.scale(0f, 0f, 0f).glow(110, 80, 50).interpolation(4, i / 2);
                spawnedEntities.add(h.entity());
                shaft.add(h);
                all.add(h);
                setScale(h.entity(), 0.16f, 0.5f, 0.16f, 6);
            }
            // 2 core netherite tip
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(60, 60, 70).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                tip.add(h);
                all.add(h);
                setScale(h.entity(), 0.22f, 0.22f, 0.4f, 6);
            }
            // 4 iron fin tip slabs
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                tip.add(h);
                all.add(h);
                setScale(h.entity(), 0.08f, 0.2f, 0.08f, 6);
            }
            // Tail fin
            BlockDisplayHandle tf = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
            tf.scale(0f, 0f, 0f).glow(200, 200, 210).interpolation(4, 0);
            spawnedEntities.add(tf.entity());
            tail.add(tf);
            all.add(tf);
            setScale(tf.entity(), 0.3f, 0.16f, 0.3f, 6);
            // 6 chain trail behind
            for (int i = 0; i < 6; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(170, 170, 185).interpolation(4, i);
                spawnedEntities.add(h.entity());
                trail.add(h);
                all.add(h);
                float ts = 0.16f - i * 0.018f;
                setScale(h.entity(), ts, ts, ts, 6);
            }
            // 7 ring fittings (decorative iron nuggets surrounding spawn)
            for (int i = 0; i < 7; i++) {
                double a = (Math.PI * 2.0 * i) / 7.0;
                Location loc = origin.clone().add(Math.cos(a) * 0.7, Math.sin(a) * 0.7, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                rings.add(h);
                all.add(h);
                setScale(h.entity(), 0.12f, 0.12f, 0.12f, 6);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.4f, 0.8f);
            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.2f, 1.0f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, origin, 25, 0.6, 0.6, 0.6, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (origin == null || targetLoc == null) return;

            int flightStart = 5;
            int flightEnd = 40;
            int impactTick = 42;

            if (tick >= flightStart && tick <= flightEnd) {
                double progress = (tick - flightStart) / (double) (flightEnd - flightStart);
                progress = Math.min(1.0, progress);
                spiralAngle += (float) Math.toRadians(15.0);

                double cx = origin.getX() + (targetLoc.getX() - origin.getX()) * progress;
                double cy = origin.getY() + (targetLoc.getY() - origin.getY()) * progress;
                double cz = origin.getZ() + (targetLoc.getZ() - origin.getZ()) * progress;
                Location anchor = new Location(c.getWorld(), cx, cy, cz);

                // Place shaft along flight axis
                for (int i = 0; i < shaft.size(); i++) {
                    double offset = -0.5 + (i / (double) shaft.size()) * 1.5;
                    Location loc = anchor.clone().add(flightDir.getX() * offset, flightDir.getY() * offset, flightDir.getZ() * offset);
                    BlockDisplay e = shaft.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                    rotateOn(e, spiralAngle, (float) flightDir.getX(), (float) flightDir.getY(), (float) flightDir.getZ(), 3);
                }
                // Tip pieces ahead of anchor
                for (int i = 0; i < tip.size(); i++) {
                    double forward = 1.0 + (i * 0.05);
                    Location loc = anchor.clone().add(flightDir.getX() * forward, flightDir.getY() * forward, flightDir.getZ() * forward);
                    BlockDisplay e = tip.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Tail behind anchor
                for (BlockDisplayHandle t : tail) {
                    Location loc = anchor.clone().add(-flightDir.getX() * 1.0, -flightDir.getY() * 1.0, -flightDir.getZ() * 1.0);
                    BlockDisplay e = t.entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Chain trail behind
                for (int i = 0; i < trail.size(); i++) {
                    double back = -1.5 - i * 0.45;
                    Location loc = anchor.clone().add(flightDir.getX() * back, flightDir.getY() * back, flightDir.getZ() * back);
                    BlockDisplay e = trail.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Rings travel near tail
                for (int i = 0; i < rings.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / 7.0 + spiralAngle;
                    Location loc = anchor.clone().add(
                            -flightDir.getX() * 1.5 + Math.cos(a) * 0.35,
                            -flightDir.getY() * 1.5 + Math.sin(a) * 0.35,
                            -flightDir.getZ() * 1.5);
                    BlockDisplay e = rings.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }

                // BLOCK_CRACK trail
                if (tick % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.BLOCK, anchor, 4,
                            0.4, 0.4, 0.4, 0.05, Material.IRON_BLOCK.createBlockData());
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, anchor, 2, 0.2, 0.2, 0.2, 0.05);
                }
            }

            // Flight whoosh
            if (tick > flightStart && tick < flightEnd && tick % 4 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 1.3f);
            }

            // Impact
            if (tick == impactTick) {
                triggerImpactDamage(targetLoc);
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.7f);
                DisplayBuilder.playSound(targetLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.3f, 0.9f);
                c.getWorld().spawnParticle(Particle.CRIT, targetLoc, 15, 1.0, 0.5, 1.0, 0.3);
                c.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 5, 0.6, 0.3, 0.6, 0.05);
            }
            // Vanish
            if (tick == 50) {
                for (BlockDisplayHandle h : all) shrinkToZero(h, 8);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronJavelin(plugin); }
    }

    // ================================================================
    // #46 — HOOK AND CHAIN ("The Catch")
    // Big iron hook fires on a chain, pulls player toward hook on hit.
    // 32 blocks: 4 hook curve + 1 hook point + 10 chain trail + 1 wall mount
    //          + 16 supplementary chain harness/segments.
    // ================================================================
    public static class HookAndChain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> hook = new ArrayList<>();
        private final List<BlockDisplayHandle> point = new ArrayList<>();
        private final List<BlockDisplayHandle> chain = new ArrayList<>();
        private final List<BlockDisplayHandle> mount = new ArrayList<>();
        private final List<BlockDisplayHandle> harness = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private Location targetLoc;
        private Vector flightDir;
        private Location lastHookPos;

        public HookAndChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hook_and_chain", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(320.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(200);
            config.setChance(6);
            config.setEnabled(true);
            config.setDesignType("Pull-toward hook (sidestep + counter pull)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            Player target = findNearestPlayer(center, 50.0);
            targetLoc = (target != null) ? target.getLocation().clone() : center.clone();
            origin = center.clone().add(0.0, 4.0, -14.0); // perimeter -Z, elevated

            // Direction
            double dx = targetLoc.getX() - origin.getX();
            double dy = targetLoc.getY() + 1.0 - origin.getY();
            double dz = targetLoc.getZ() - origin.getZ();
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < 0.01) dist = 1.0;
            flightDir = new Vector(dx / dist, dy / dist, dz / dist);

            // 4 hook curve (IRON_BLOCK cubes rotated at increasing Z-angles)
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(6, i);
                spawnedEntities.add(h.entity());
                hook.add(h);
                all.add(h);
                setScale(h.entity(), 0.25f, 0.32f, 0.25f, 8);
            }
            // Hook point (NETHERITE_BLOCK micro)
            BlockDisplayHandle hp = displayBuilder.spawnBlock(origin.clone(), Material.NETHERITE_BLOCK);
            hp.scale(0f, 0f, 0f).glow(60, 60, 70).interpolation(6, 5);
            spawnedEntities.add(hp.entity());
            point.add(hp);
            all.add(hp);
            setScale(hp.entity(), 0.18f, 0.18f, 0.18f, 8);
            // 10 chain links trailing
            for (int i = 0; i < 10; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(4, i);
                spawnedEntities.add(h.entity());
                chain.add(h);
                all.add(h);
                setScale(h.entity(), 0.18f, 0.18f, 0.5f, 6);
            }
            // Wall mount
            BlockDisplayHandle wm = displayBuilder.spawnBlock(origin.clone(), Material.POLISHED_BLACKSTONE);
            wm.scale(0f, 0f, 0f).glow(120, 120, 130).interpolation(6, 0);
            spawnedEntities.add(wm.entity());
            mount.add(wm);
            all.add(wm);
            setScale(wm.entity(), 0.5f, 0.5f, 0.5f, 8);
            // 16 harness segments (small iron blocks reinforcing chain near mount)
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2.0 * i) / 16.0;
                Location loc = origin.clone().add(Math.cos(a) * 0.5, Math.sin(a) * 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GRAY_CONCRETE);
                h.scale(0f, 0f, 0f).glow(120, 120, 130).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                harness.add(h);
                all.add(h);
                setScale(h.entity(), 0.08f, 0.08f, 0.08f, 6);
            }
            lastHookPos = origin.clone();

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.4f, 0.8f);
            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.2f, 0.8f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, origin, 20, 0.5, 0.5, 0.5, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (origin == null || targetLoc == null) return;

            int flightStart = 5;
            int flightEnd = 50;
            int impactTick = 52;

            if (tick >= flightStart && tick <= flightEnd) {
                double progress = (tick - flightStart) / (double) (flightEnd - flightStart);
                progress = Math.min(1.0, progress);

                double hx = origin.getX() + (targetLoc.getX() - origin.getX()) * progress;
                double hy = origin.getY() + (targetLoc.getY() + 1.0 - origin.getY()) * progress;
                double hz = origin.getZ() + (targetLoc.getZ() - origin.getZ()) * progress;
                Location hookPos = new Location(c.getWorld(), hx, hy, hz);
                lastHookPos = hookPos.clone();

                // 4 hook curve forming C shape; each block offset perpendicular
                for (int i = 0; i < hook.size(); i++) {
                    double curveAngle = Math.toRadians(30 + i * 30);
                    Location loc = hookPos.clone().add(
                            Math.cos(curveAngle) * 0.4,
                            -Math.sin(curveAngle) * 0.4,
                            0);
                    BlockDisplay e = hook.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                    rotateOn(e, (float) curveAngle, 0f, 0f, 1f, 3);
                }
                // Hook point at curve tip
                BlockDisplay hpE = point.get(0).entity();
                if (hpE != null && hpE.isValid()) {
                    Location loc = hookPos.clone().add(0.4, -0.4, 0);
                    try { hpE.teleport(loc); } catch (Throwable ignored) {}
                }
                // Chain links: from origin to hook (materialize one new link every 2 ticks)
                int matLinks = Math.min(chain.size(), (tick - flightStart) / 2 + 1);
                for (int i = 0; i < chain.size(); i++) {
                    if (i < matLinks) {
                        double frac = (i + 1.0) / (matLinks + 1.0);
                        double lx = origin.getX() + (hx - origin.getX()) * frac;
                        double ly = origin.getY() + (hy - origin.getY()) * frac;
                        double lz = origin.getZ() + (hz - origin.getZ()) * frac;
                        BlockDisplay e = chain.get(i).entity();
                        if (e == null || !e.isValid()) continue;
                        try { e.teleport(new Location(c.getWorld(), lx, ly, lz)); } catch (Throwable ignored) {}
                    }
                }

                // Flight sound
                if (tick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 1.0f);
                }
                if (tick % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.CRIT, hookPos, 3, 0.3, 0.3, 0.3, 0.05);
                }
            }

            // Impact: pull players toward hook position
            if (tick == impactTick && lastHookPos != null) {
                triggerImpactDamage(lastHookPos);
                DisplayBuilder.playSound(lastHookPos, Sound.BLOCK_CHAIN_HIT, 1.5f, 0.8f);
                DisplayBuilder.playSound(lastHookPos, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.6f);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, lastHookPos, 25, 1.0, 0.5, 1.0, 0.2);
                c.getWorld().spawnParticle(Particle.CRIT, lastHookPos, 20, 0.8, 0.5, 0.8, 0.3);

                // Apply pull-toward velocity on any player within 2x impact radius
                double pullRange = config.getImpactRadius() * 2.0;
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(lastHookPos) > pullRange * pullRange) continue;
                    Vector pull = origin.toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.4);
                    pull.setY(0.4);
                    try { p.setVelocity(pull); } catch (Throwable ignored) {}
                }
            }
            // Vanish
            if (tick == 65) {
                for (BlockDisplayHandle h : all) shrinkToZero(h, 10);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HookAndChain(plugin); }
    }

    // ================================================================
    // #47 — CHAIN COMET ("The Streak")
    // Iron core + long chain tail streaks straight across the arena.
    // 32 blocks: 1 core + 4 poles + 4 corners + 14 tail (taper) + 3 glow shrooms
    //          + 6 trailing sparks (gray concrete).
    // ================================================================
    public static class ChainComet extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> tail = new ArrayList<>();
        private final List<BlockDisplayHandle> glow = new ArrayList<>();
        private final List<BlockDisplayHandle> sparks = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private Location targetLoc;
        private final List<double[]> history = new ArrayList<>(); // {x,y,z} of head each tick

        public ChainComet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_comet", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(420.0);
            config.setImpactRadius(3.5);
            config.setDurationTicks(80);
            config.setCooldownTicks(220);
            config.setChance(6);
            config.setEnabled(true);
            config.setDesignType("Streaking projectile (read arc, sidestep)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            Player target = findNearestPlayer(center, 50.0);
            targetLoc = (target != null) ? target.getLocation().clone().add(0, 1.5, 0) : center.clone();
            origin = center.clone().add(0.0, 5.0, 14.0);

            // 1 core + 4 poles + 4 corners
            BlockDisplayHandle core = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
            core.scale(0f, 0f, 0f).glow(255, 180, 80).interpolation(4, 0);
            spawnedEntities.add(core.entity());
            head.add(core);
            all.add(core);
            setScale(core.entity(), 0.7f, 0.7f, 0.7f, 6);
            double[][] poleOffsets = {{0.5, 0, 0}, {-0.5, 0, 0}, {0, 0.5, 0}, {0, -0.5, 0}};
            for (double[] off : poleOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone().add(off[0], off[1], off[2]), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(255, 160, 60).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                head.add(h);
                all.add(h);
                setScale(h.entity(), 0.32f, 0.32f, 0.32f, 6);
            }
            double[][] cornerOffsets = {{0.4, 0.4, 0}, {-0.4, 0.4, 0}, {0.4, -0.4, 0}, {-0.4, -0.4, 0}};
            for (double[] off : cornerOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone().add(off[0], off[1], off[2]), Material.GRAY_CONCRETE);
                h.scale(0f, 0f, 0f).glow(255, 120, 40).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                head.add(h);
                all.add(h);
                setScale(h.entity(), 0.24f, 0.24f, 0.24f, 6);
            }
            // 14 tail (decreasing scale)
            for (int i = 0; i < 14; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(255, 120, 60).interpolation(4, i / 2);
                spawnedEntities.add(h.entity());
                tail.add(h);
                all.add(h);
                float ts = 0.34f - i * 0.022f;
                setScale(h.entity(), ts, ts, ts, 6);
            }
            // 3 glow accents (SHROOMLIGHT)
            for (int i = 0; i < 3; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.SHROOMLIGHT);
                h.scale(0f, 0f, 0f).glow(255, 220, 100).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                glow.add(h);
                all.add(h);
                setScale(h.entity(), 0.14f, 0.06f, 0.14f, 6);
            }
            // 6 trailing sparks
            for (int i = 0; i < 6; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.GRAY_CONCRETE);
                h.scale(0f, 0f, 0f).glow(255, 100, 30).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                sparks.add(h);
                all.add(h);
                setScale(h.entity(), 0.1f, 0.1f, 0.1f, 6);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.6f, 1.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 1.2f);
            w.spawnParticle(Particle.LAVA, origin, 12, 0.5, 0.5, 0.5, 0.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (origin == null || targetLoc == null) return;

            int flightStart = 4;
            int flightEnd = 50;
            int impactTick = 52;

            if (tick >= flightStart && tick <= flightEnd) {
                double progress = (tick - flightStart) / (double) (flightEnd - flightStart);
                progress = Math.min(1.0, progress);

                double hx = origin.getX() + (targetLoc.getX() - origin.getX()) * progress;
                double hy = origin.getY() + (targetLoc.getY() - origin.getY()) * progress;
                double hz = origin.getZ() + (targetLoc.getZ() - origin.getZ()) * progress;
                history.add(new double[]{hx, hy, hz});

                // Move head pieces
                for (int i = 0; i < head.size(); i++) {
                    BlockDisplay e = head.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    // Maintain original relative offset (we computed offsets at spawn time)
                    Location loc = new Location(c.getWorld(), hx, hy, hz);
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                    // Tumble spin
                    rotateOn(e, (float) Math.toRadians(tick * 10.0), 1f, 0f, 0f, 3);
                }
                // Tail follows history (each link 2 ticks behind head)
                for (int i = 0; i < tail.size(); i++) {
                    int hi = history.size() - 1 - (i + 1) * 2;
                    if (hi < 0) hi = 0;
                    double[] pos = history.get(hi);
                    BlockDisplay e = tail.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(new Location(c.getWorld(), pos[0], pos[1], pos[2])); } catch (Throwable ignored) {}
                }
                // Glow accents at every 4th tail position
                for (int i = 0; i < glow.size(); i++) {
                    int hi = history.size() - 1 - (i * 4 + 4);
                    if (hi < 0) hi = 0;
                    double[] pos = history.get(hi);
                    BlockDisplay e = glow.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(new Location(c.getWorld(), pos[0], pos[1], pos[2])); } catch (Throwable ignored) {}
                }
                // Sparks at random tail positions
                for (int i = 0; i < sparks.size(); i++) {
                    int hi = history.size() - 1 - (i * 3 + 1);
                    if (hi < 0) hi = 0;
                    double[] pos = history.get(hi);
                    BlockDisplay e = sparks.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try {
                        e.teleport(new Location(c.getWorld(),
                                pos[0] + (Math.random() - 0.5) * 0.2,
                                pos[1] + (Math.random() - 0.5) * 0.2,
                                pos[2] + (Math.random() - 0.5) * 0.2));
                    } catch (Throwable ignored) {}
                }

                // Heat-trail particles
                if (tick % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.LAVA, new Location(c.getWorld(), hx, hy, hz), 2, 0.3, 0.3, 0.3, 0.0);
                    c.getWorld().spawnParticle(Particle.CRIT, new Location(c.getWorld(), hx, hy, hz), 5, 0.3, 0.3, 0.3, 0.1);
                    c.getWorld().spawnParticle(Particle.BLOCK, new Location(c.getWorld(), hx, hy, hz), 4,
                            0.3, 0.3, 0.3, 0.05, Material.IRON_BLOCK.createBlockData());
                }
                if (tick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 1.5f);
                }
            }

            // Impact
            if (tick == impactTick) {
                triggerImpactDamage(targetLoc);
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.6f);
                DisplayBuilder.playSound(targetLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.4f, 0.8f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 12, 1.5, 0.8, 1.5, 0.05);
                c.getWorld().spawnParticle(Particle.LAVA, targetLoc, 25, 1.2, 1.0, 1.2, 0.0);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, targetLoc, 30, 1.5, 0.8, 1.5, 0.1);
            }
            // Vanish
            if (tick == 65) {
                for (BlockDisplayHandle h : all) shrinkToZero(h, 12);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainComet(plugin); }
    }

    // ================================================================
    // #48 — IRON CHAKRAM ("The Disc")
    // Spinning iron disc flies horizontally, reaches max distance, returns.
    // 32 blocks: 12 rim + 4 notches + 4 center ring chain + 1 center hub
    //          + 11 supporting decorative shards.
    // ================================================================
    public static class IronChakram extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> rim = new ArrayList<>();
        private final List<BlockDisplayHandle> notches = new ArrayList<>();
        private final List<BlockDisplayHandle> centerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> hub = new ArrayList<>();
        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private Location apex; // turn-around point
        private float spinAngle = 0f;

        public IronChakram(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_chakram", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(280.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(160);
            config.setCooldownTicks(220);
            config.setChance(7);
            config.setEnabled(true);
            config.setDesignType("Returning disc (read arc + dodge twice)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            Player target = findNearestPlayer(center, 50.0);
            Location aim = (target != null) ? target.getLocation().clone() : center.clone();
            origin = center.clone().add(-12.0, 1.5, 0.0);
            apex = new Location(c2World(aim), aim.getX() + 6.0, 1.5, aim.getZ());

            // 12 rim (IRON_BLOCK flat slabs in horizontal circle)
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12.0;
                Location loc = origin.clone().add(Math.cos(a) * 1.0, 0, Math.sin(a) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(6, i / 2);
                spawnedEntities.add(h.entity());
                rim.add(h);
                all.add(h);
                setScale(h.entity(), 0.48f, 0.08f, 0.18f, 8);
            }
            // 4 netherite notches at cardinal positions
            double[][] cardinal = {{1.0, 0, 0}, {-1.0, 0, 0}, {0, 0, 1.0}, {0, 0, -1.0}};
            for (double[] off : cardinal) {
                Location loc = origin.clone().add(off[0], 0, off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(60, 60, 70).interpolation(6, 4);
                spawnedEntities.add(h.entity());
                notches.add(h);
                all.add(h);
                setScale(h.entity(), 0.14f, 0.1f, 0.1f, 8);
            }
            // 4 center ring chain
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2.0 * i) / 4.0;
                Location loc = origin.clone().add(Math.cos(a) * 0.25, 0, Math.sin(a) * 0.25);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                centerRing.add(h);
                all.add(h);
                setScale(h.entity(), 0.14f, 0.08f, 0.14f, 8);
            }
            // 1 hub
            BlockDisplayHandle hb = displayBuilder.spawnBlock(origin.clone(), Material.NETHERITE_BLOCK);
            hb.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(6, 0);
            spawnedEntities.add(hb.entity());
            hub.add(hb);
            all.add(hb);
            setScale(hb.entity(), 0.18f, 0.12f, 0.18f, 8);
            // 11 supporting shards at intermediate rim positions
            for (int i = 0; i < 11; i++) {
                double a = (Math.PI * 2.0 * i) / 11.0;
                Location loc = origin.clone().add(Math.cos(a) * 0.7, 0, Math.sin(a) * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GRAY_CONCRETE);
                h.scale(0f, 0f, 0f).glow(160, 160, 170).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                shards.add(h);
                all.add(h);
                setScale(h.entity(), 0.08f, 0.05f, 0.12f, 8);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.4f, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.0f, 1.6f);
            w.spawnParticle(Particle.CRIT, origin, 20, 0.8, 0.3, 0.8, 0.1);
        }

        private World c2World(Location l) {
            return l != null ? l.getWorld() : null;
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (origin == null || apex == null) return;

            int outEnd = 70;   // outward flight ends
            int retEnd = 140;  // returns to origin
            spinAngle += (float) Math.toRadians(25.0);

            // Decide phase
            double mx, my, mz;
            if (tick < outEnd) {
                double progress = tick / (double) outEnd;
                mx = origin.getX() + (apex.getX() - origin.getX()) * progress;
                my = origin.getY() + (apex.getY() - origin.getY()) * progress;
                mz = origin.getZ() + (apex.getZ() - origin.getZ()) * progress;
            } else if (tick < retEnd) {
                double progress = (tick - outEnd) / (double) (retEnd - outEnd);
                mx = apex.getX() + (origin.getX() - apex.getX()) * progress;
                my = apex.getY() + (origin.getY() - apex.getY()) * progress;
                mz = apex.getZ() + (origin.getZ() - apex.getZ()) * progress;
            } else {
                mx = origin.getX(); my = origin.getY(); mz = origin.getZ();
            }

            // Place rim around moving center
            for (int i = 0; i < rim.size(); i++) {
                double a = (Math.PI * 2.0 * i) / 12.0 + spinAngle;
                double x = mx + Math.cos(a) * 1.0;
                double z = mz + Math.sin(a) * 1.0;
                BlockDisplay e = rim.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(new Location(c.getWorld(), x, my, z)); } catch (Throwable ignored) {}
            }
            // Notches at cardinal positions
            for (int i = 0; i < notches.size(); i++) {
                double a = i * (Math.PI / 2.0) + spinAngle;
                double x = mx + Math.cos(a) * 1.0;
                double z = mz + Math.sin(a) * 1.0;
                BlockDisplay e = notches.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(new Location(c.getWorld(), x, my, z)); } catch (Throwable ignored) {}
            }
            // Center ring
            for (int i = 0; i < centerRing.size(); i++) {
                double a = (Math.PI * 2.0 * i) / 4.0 + spinAngle;
                double x = mx + Math.cos(a) * 0.25;
                double z = mz + Math.sin(a) * 0.25;
                BlockDisplay e = centerRing.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(new Location(c.getWorld(), x, my, z)); } catch (Throwable ignored) {}
            }
            // Hub at center
            BlockDisplay hbE = hub.get(0).entity();
            if (hbE != null && hbE.isValid()) {
                try { hbE.teleport(new Location(c.getWorld(), mx, my, mz)); } catch (Throwable ignored) {}
            }
            // Shards
            for (int i = 0; i < shards.size(); i++) {
                double a = (Math.PI * 2.0 * i) / 11.0 - spinAngle * 0.5;
                double x = mx + Math.cos(a) * 0.7;
                double z = mz + Math.sin(a) * 0.7;
                BlockDisplay e = shards.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(new Location(c.getWorld(), x, my, z)); } catch (Throwable ignored) {}
            }

            // Update center to track disc so damageRadius hits on actual disc
            setCenter(new Location(c.getWorld(), mx, my, mz));

            // Particles
            if (tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, new Location(c.getWorld(), mx, my, mz), 6, 1.0, 0.2, 1.0, 0.2);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, new Location(c.getWorld(), mx, my, mz), 3, 0.5, 0.2, 0.5, 0.1);
            }
            // Flight sound
            if (tick % 5 == 0) {
                DisplayBuilder.playSound(new Location(c.getWorld(), mx, my, mz), Sound.BLOCK_CHAIN_FALL, 0.6f, 1.6f);
            }
            // Reverse sound on turn-around
            if (tick == outEnd) {
                DisplayBuilder.playSound(new Location(c.getWorld(), mx, my, mz), Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 1.1f);
                DisplayBuilder.playSound(new Location(c.getWorld(), mx, my, mz), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.2f, 1.3f);
            }

            // Dissipate at end
            int duration = config.getDurationTicks();
            if (tick == duration - 12) {
                for (BlockDisplayHandle h : all) shrinkToZero(h, 10);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.0f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronChakram(plugin); }
    }

    // ================================================================
    // #49 — CHAIN DRILL ("The Auger")
    // Three fins spiral around a netherite core, drifting toward player.
    // Uses follow-AI at speed 0.08.
    // 32 blocks: 1 core + 3 fins + 1 tip core + 4 micro bevels + 8 trail
    //          + 15 helical extension blocks for shape readability.
    // ================================================================
    public static class ChainDrill extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> core = new ArrayList<>();
        private final List<BlockDisplayHandle> fins = new ArrayList<>();
        private final List<BlockDisplayHandle> tipCore = new ArrayList<>();
        private final List<BlockDisplayHandle> bevels = new ArrayList<>();
        private final List<BlockDisplayHandle> trail = new ArrayList<>();
        private final List<BlockDisplayHandle> helixExt = new ArrayList<>();
        private float drillAngle = 0f;

        public ChainDrill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_drill", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(360.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
            config.setChance(6);
            config.setEnabled(true);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.08);
            config.setDesignType("Advancing drill (sprint perpendicular)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 1.5;
            // 1 core (NETHERITE_BLOCK)
            BlockDisplayHandle co = displayBuilder.spawnBlock(center.clone().add(0, cy, 0), Material.NETHERITE_BLOCK);
            co.scale(0f, 0f, 0f).glow(70, 70, 80).interpolation(10, 0);
            spawnedEntities.add(co.entity());
            core.add(co);
            setScale(co.entity(), 0.55f, 0.55f, 0.55f, 12);
            // 3 fins (CHAIN flat slabs at 120 deg, tilted 35 deg forward on X)
            for (int i = 0; i < 3; i++) {
                double a = (Math.PI * 2.0 * i) / 3.0;
                Location loc = center.clone().add(Math.cos(a) * 0.55, cy, Math.sin(a) * 0.55);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(10, 2);
                spawnedEntities.add(h.entity());
                fins.add(h);
                setScale(h.entity(), 0.45f, 0.06f, 0.36f, 12);
                rotateOn(h.entity(), (float) Math.toRadians(35), 1f, 0f, 0f, 12);
            }
            // 1 tip core (forward of main core)
            BlockDisplayHandle tc = displayBuilder.spawnBlock(center.clone().add(0, cy, 0.6), Material.NETHERITE_BLOCK);
            tc.scale(0f, 0f, 0f).glow(80, 80, 95).interpolation(10, 4);
            spawnedEntities.add(tc.entity());
            tipCore.add(tc);
            setScale(tc.entity(), 0.32f, 0.32f, 0.45f, 12);
            // 4 micro bevel pieces around tip
            double[][] bevelOffs = {{0.1, 0, 0.7}, {-0.1, 0, 0.7}, {0, 0.1, 0.7}, {0, -0.1, 0.7}};
            for (double[] off : bevelOffs) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], cy + off[1], off[2]), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(10, 4);
                spawnedEntities.add(h.entity());
                bevels.add(h);
                setScale(h.entity(), 0.08f, 0.18f, 0.08f, 12);
            }
            // 8 trail chain links behind
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, cy, -0.4 - i * 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(8, i);
                spawnedEntities.add(h.entity());
                trail.add(h);
                float ts = 0.2f - i * 0.018f;
                setScale(h.entity(), ts, ts, ts, 10);
            }
            // 15 helical extension blocks (iron blocks tracing helix around drill body)
            for (int i = 0; i < 15; i++) {
                double angle = (Math.PI * 2.0 * i) / 5.0;
                double zOff = -0.3 + i * 0.08;
                Location loc = center.clone().add(Math.cos(angle) * 0.4, cy + Math.sin(angle) * 0.4, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(8, i / 2);
                spawnedEntities.add(h.entity());
                helixExt.add(h);
                setScale(h.entity(), 0.1f, 0.1f, 0.1f, 10);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.5f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.0f, 0.6f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, cy, 0), 30, 0.8, 0.6, 0.8, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double cy = 1.5;
            // Spin fins around core
            if (tick > 15) {
                drillAngle += (float) Math.toRadians(18.0);
                for (int i = 0; i < fins.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / 3.0 + drillAngle;
                    double x = Math.cos(a) * 0.55;
                    double z = Math.sin(a) * 0.55;
                    BlockDisplay e = fins.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(x, cy, z)); } catch (Throwable ignored) {}
                    rotateOn(e, drillAngle + (float) (i * 2.0 * Math.PI / 3.0), 0f, 0f, 1f, 3);
                }
                // Helix extension spirals
                for (int i = 0; i < helixExt.size(); i++) {
                    double angle = (Math.PI * 2.0 * i) / 5.0 + drillAngle;
                    double zOff = -0.3 + i * 0.08;
                    BlockDisplay e = helixExt.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try {
                        e.teleport(c.clone().add(Math.cos(angle) * 0.4, cy + Math.sin(angle) * 0.4, zOff));
                    } catch (Throwable ignored) {}
                }
            }

            // ELECTRIC_SPARK off fin tips
            if (tick > 15 && tick % 3 == 0) {
                for (BlockDisplayHandle h : fins) {
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, h.entity().getLocation(), 3, 0.3, 0.3, 0.3, 0.1);
                }
            }
            // CRIT burst at tip area
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, cy, 0.7), 6, 0.4, 0.4, 0.2, 0.15);
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, cy, 0.7), 4,
                        0.3, 0.3, 0.3, 0.05, Material.IRON_BLOCK.createBlockData());
            }
            // Grindstone loop sound
            if (tick > 15 && tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.8f, 1.0f);
            }
            // Anvil thud every 40t
            if (tick > 15 && tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.7f);
            }

            // Dissipate: drill sparks out, fins fly off
            int duration = config.getDurationTicks();
            if (tick == duration - 20) {
                for (BlockDisplayHandle h : fins) shrinkToZero(h, 12);
                for (BlockDisplayHandle h : helixExt) shrinkToZero(h, 12);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.4f, 0.6f);
            }
            if (tick == duration - 8) {
                for (BlockDisplayHandle h : core) shrinkToZero(h, 6);
                for (BlockDisplayHandle h : tipCore) shrinkToZero(h, 6);
                for (BlockDisplayHandle h : bevels) shrinkToZero(h, 6);
                for (BlockDisplayHandle h : trail) shrinkToZero(h, 6);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, cy, 0), 8, 0.8, 0.5, 0.8, 0.05);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainDrill(plugin); }
    }

    // ================================================================
    // #50 — CHAIN SERPENT ("The Iron Snake")
    // Slithering chain snake. Body is 20 chain links following head with sine
    // undulation. Uses follow-AI at speed 0.06.
    // 32 blocks: 1 head iron + 2 eye netherite + 20 body chain + 6 tail chain
    //          + 3 horn iron details.
    // ================================================================
    public static class ChainSerpent extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> tailSegs = new ArrayList<>();
        private final List<BlockDisplayHandle> horns = new ArrayList<>();
        private final List<double[]> history = new ArrayList<>(); // head position history
        private float bodyPhase = 0f;

        public ChainSerpent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_serpent", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(280);
            config.setChance(5);
            config.setEnabled(true);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.06);
            config.setDesignType("Slithering chase (read serpent path)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 0.5;
            // Head
            BlockDisplayHandle h0 = displayBuilder.spawnBlock(center.clone().add(0, cy, 0), Material.IRON_BLOCK);
            h0.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(10, 0);
            spawnedEntities.add(h0.entity());
            head.add(h0);
            setScale(h0.entity(), 0.5f, 0.32f, 0.6f, 12);
            // Eyes (NETHERITE_BLOCK)
            for (int i = 0; i < 2; i++) {
                double px = (i == 0) ? -0.15 : 0.15;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(px, cy + 0.1, 0.3), Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(255, 30, 30).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                eyes.add(h);
                setScale(h.entity(), 0.1f, 0.1f, 0.08f, 12);
            }
            // 20 body chain links behind head
            for (int i = 0; i < 20; i++) {
                Location loc = center.clone().add(0, cy, -0.5 - i * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(190, 190, 205).interpolation(8, i / 2);
                spawnedEntities.add(h.entity());
                body.add(h);
                setScale(h.entity(), 0.22f, 0.22f, 0.4f, 10);
            }
            // 6 tail chain (tapered)
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, cy, -8.5 - i * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(170, 170, 185).interpolation(8, 12 + i);
                spawnedEntities.add(h.entity());
                tailSegs.add(h);
                float ts = 0.18f - i * 0.018f;
                setScale(h.entity(), ts, ts, ts, 10);
            }
            // 3 horns (small iron pieces on head)
            double[][] hornOffs = {{-0.2, 0.25, 0.0}, {0.2, 0.25, 0.0}, {0.0, 0.3, -0.15}};
            for (double[] off : hornOffs) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], cy + off[1], off[2]), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                horns.add(h);
                setScale(h.entity(), 0.1f, 0.25f, 0.1f, 12);
            }

            // Seed history with starting head position
            for (int i = 0; i < 50; i++) {
                history.add(new double[]{center.getX(), center.getY() + cy, center.getZ()});
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.4f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.9f, 0.5f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, cy, 0), 30, 1.5, 0.5, 1.5, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double cy = 0.5;
            bodyPhase += 0.08f;

            // Record head position history
            history.add(new double[]{c.getX(), c.getY() + cy, c.getZ()});
            if (history.size() > 200) history.remove(0);

            // Place head at center
            BlockDisplay h0 = head.get(0).entity();
            if (h0 != null && h0.isValid()) {
                try { h0.teleport(c.clone().add(0, cy, 0)); } catch (Throwable ignored) {}
            }
            // Eyes follow head
            for (int i = 0; i < eyes.size(); i++) {
                double px = (i == 0) ? -0.15 : 0.15;
                BlockDisplay e = eyes.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(c.clone().add(px, cy + 0.1, 0.3)); } catch (Throwable ignored) {}
            }
            // Horns follow head
            double[][] hornOffs = {{-0.2, 0.25, 0.0}, {0.2, 0.25, 0.0}, {0.0, 0.3, -0.15}};
            for (int i = 0; i < horns.size(); i++) {
                BlockDisplay e = horns.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(c.clone().add(hornOffs[i][0], cy + hornOffs[i][1], hornOffs[i][2])); } catch (Throwable ignored) {}
            }

            // Body: each segment at head history position from i*1.5 ticks ago, plus sine offset
            for (int i = 0; i < body.size(); i++) {
                int back = (int) ((i + 1) * 1.5);
                int idx = history.size() - 1 - back;
                if (idx < 0) idx = 0;
                double[] pos = history.get(idx);
                double offset = Math.sin(bodyPhase + i * 0.7) * 0.35;
                double dxOff = -offset; // perpendicular to mainly-Z motion (we use X)
                BlockDisplay e = body.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(new Location(c.getWorld(), pos[0] + dxOff, pos[1], pos[2])); } catch (Throwable ignored) {}
            }
            // Tail follows further back
            for (int i = 0; i < tailSegs.size(); i++) {
                int back = (int) ((body.size() + i + 1) * 1.5);
                int idx = history.size() - 1 - back;
                if (idx < 0) idx = 0;
                double[] pos = history.get(idx);
                double offset = Math.sin(bodyPhase + (body.size() + i) * 0.7) * 0.35;
                BlockDisplay e = tailSegs.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(new Location(c.getWorld(), pos[0] - offset, pos[1], pos[2])); } catch (Throwable ignored) {}
            }

            // BLOCK_CRACK from body every 5t
            if (tick % 5 == 0) {
                for (int i = 0; i < body.size(); i += 4) {
                    Location loc = body.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.BLOCK, loc, 2,
                            0.2, 0.2, 0.2, 0.02, Material.CHAIN.createBlockData());
                }
            }
            // Smoke trail
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, cy, 0), 3, 0.4, 0.2, 0.4, 0.01);
            }
            // Slithering sound
            if (tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.7f, 0.6f);
            }
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.8f, 0.7f);
            }
            if (tick > 0 && tick % 80 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 0.4f);
            }

            // Dissipate: body collapses tail-first
            int duration = config.getDurationTicks();
            if (tick >= duration - 30) {
                int dt = tick - (duration - 30);
                // Shrink one segment per tick from tail forward
                if (dt < tailSegs.size()) {
                    shrinkToZero(tailSegs.get(tailSegs.size() - 1 - dt), 4);
                } else {
                    int bodyIdx = body.size() - 1 - (dt - tailSegs.size());
                    if (bodyIdx >= 0 && bodyIdx < body.size()) {
                        shrinkToZero(body.get(bodyIdx), 4);
                    }
                }
                if (dt == 26) {
                    for (BlockDisplayHandle h : head) shrinkToZero(h, 5);
                    for (BlockDisplayHandle h : eyes) shrinkToZero(h, 5);
                    for (BlockDisplayHandle h : horns) shrinkToZero(h, 5);
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.2f, 0.5f);
                    c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, cy, 0), 6, 0.8, 0.4, 0.8, 0.05);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainSerpent(plugin); }
    }
}
