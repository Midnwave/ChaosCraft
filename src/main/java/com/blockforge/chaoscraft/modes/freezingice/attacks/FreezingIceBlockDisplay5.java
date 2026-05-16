package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * FreezingIce Mode — BLOCK DISPLAY ATTACKS file 5/8 (Cosmic / Magical theme, attacks 41-50).
 *
 * Frozen cosmic-horror spectacle: meteors with tails, novas, bolts, vortexes,
 * sleeping dragons, singularities, moonlets, constellations, time-rifts, and
 * star-shard clouds. Every attack uses 30+ BlockDisplay entities animated via
 * Transformation interpolation for smooth multi-phase motion.
 *
 * Cosmic-ice palette:
 *  - Frozen blue: RGB(120, 200, 255) — PACKED_ICE / BLUE_ICE
 *  - Magic purple: RGB(200, 120, 255) — AMETHYST_BLOCK / PURPLE_STAINED_GLASS
 *  - Cool core: RGB(100, 180, 255) — DIAMOND_BLOCK / BLUE_STAINED_GLASS
 *  - Void black: RGB(40, 40, 60) — OBSIDIAN / BLACK_CONCRETE
 *  - Star white: RGB(245, 245, 245) — WHITE_CONCRETE / CALCITE
 *
 * Materials: PACKED_ICE, BLUE_ICE, ICE, TINTED_GLASS, BLUE_STAINED_GLASS,
 *            PURPLE_STAINED_GLASS, AMETHYST_BLOCK, DIAMOND_BLOCK, END_STONE,
 *            BLACK_CONCRETE, WHITE_CONCRETE, OBSIDIAN, CALCITE, SNOW_BLOCK,
 *            LIGHT_BLUE_CONCRETE
 * Particles: SOUL_FIRE_FLAME, END_ROD, ELECTRIC_SPARK, GLOW, SCULK_SOUL,
 *            REVERSE_PORTAL, SNOWFLAKE, CLOUD, FALLING_DUST
 * Sounds: BLOCK_AMETHYST_BLOCK_CHIME, BLOCK_BEACON_ACTIVATE,
 *         ENTITY_GLOW_SQUID_AMBIENT, BLOCK_GLASS_BREAK, ITEM_TRIDENT_RIPTIDE,
 *         ENTITY_GENERIC_EXPLODE
 *
 * Attacks:
 *  41. FrozenMeteor              — 39 blocks, head+tail+wings dive impact
 *  42. IceNovaExpand             — 45 blocks, singularity + 12 radial spear-arms
 *  43. FrozenLightningBolt       — 44 blocks, zigzag bolt + forks + cloud
 *  44. AuroraVortex              — 40 blocks, spiraling ribbon helix column
 *  45. HibernatingIceDragon      — 35 blocks, curled sleeping dragon + wake burst
 *  46. FrozenComet               — 43 blocks, ice singularity black-hole, dual rings
 *  47. IceSingularity            — 38 blocks, mini-moon descending with rings
 *  48. GlacialBlackHole          — 44 blocks, constellation of 12 stars + lines
 *  49. EternalIceConstellation   — 39 blocks, vertical time-rift with shards
 *  50. FrozenNebula              — 60 blocks, 30 star-shard pairs orbiting cloud
 *
 * None use follow-AI.
 */
public final class FreezingIceBlockDisplay5 {
    private FreezingIceBlockDisplay5() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FrozenMeteor(plugin));
        registry.register(new IceNovaExpand(plugin));
        registry.register(new FrozenLightningBolt(plugin));
        registry.register(new AuroraVortex(plugin));
        registry.register(new HibernatingIceDragon(plugin));
        registry.register(new FrozenComet(plugin));
        registry.register(new IceSingularity(plugin));
        registry.register(new GlacialBlackHole(plugin));
        registry.register(new EternalIceConstellation(plugin));
        registry.register(new FrozenNebula(plugin));
    }

    // ================================================================
    // Shared transform helpers
    // ================================================================

    private static void setFullTransform(BlockDisplay e, Vector3f translation, AxisAngle4f rotation,
                                          Vector3f scale, int duration) {
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                translation,
                rotation,
                scale,
                new AxisAngle4f(0, 0, 1, 0)
        ));
    }

    private static void translateBlock(BlockDisplay e, float x, float y, float z, int duration) {
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                new AxisAngle4f().set(t.getLeftRotation()),
                t.getScale(),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    private static void scaleBlock(BlockDisplay e, float sx, float sy, float sz, int duration) {
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f().set(t.getLeftRotation()),
                new Vector3f(sx, sy, sz),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    private static void rotateBlock(BlockDisplay e, float angle, float ax, float ay, float az, int duration) {
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f(angle, ax, ay, az),
                t.getScale(),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    // ================================================================
    // #41 — FROZEN METEOR
    // 39 blocks: 1 PACKED_ICE head sphere center, 8 OBSIDIAN head-spikes
    // radial, 24 BLUE_ICE tapered tail, 6 BLUE_STAINED_GLASS wing-fan trail.
    // Spawns Y+25 with tail formed, streaks down diagonal, spins, impact burst.
    // Impact-only 12.0r, 280 hearts (=14 hearts * 20 default scale...) tuned 280.
    // ================================================================
    public static class FrozenMeteor extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spikeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tailBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> wingBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private boolean impacted = false;
        private float spinAngle = 0f;

        public FrozenMeteor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_meteor", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(3360.0);
            config.setImpactRadius(18.0);
            config.setDamage(0.0);
            config.setDurationTicks(110);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 1 head sphere (PACKED_ICE 1.5)
            Location headLoc = center.clone().add(0, 25, 0);
            BlockDisplayHandle head = displayBuilder.spawnBlock(headLoc, Material.PACKED_ICE);
            head.scale(1.5f, 1.5f, 1.5f).glow(100, 180, 255).interpolation(4, 0);
            spawnedEntities.add(head.entity());
            headBlocks.add(head);
            allBlocks.add(head);

            // 8 OBSIDIAN head-spikes radial around the head
            for (int i = 0; i < 8; i++) {
                double a = (2.0 * Math.PI * i) / 8;
                double px = Math.cos(a) * 1.2;
                double pz = Math.sin(a) * 1.2;
                Location loc = center.clone().add(px, 25, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.3f, 0.9f, 0.3f).glow(40, 40, 60).interpolation(4, 0);
                h.rotate((float) a, 0f, 1f, 0f);
                spawnedEntities.add(h.entity());
                spikeBlocks.add(h);
                allBlocks.add(h);
            }

            // 24 BLUE_ICE tail blocks tapered back along trajectory (+X +Z dir, +Y up)
            // Tail extends "behind" the meteor — opposite of travel direction (which is -X -Y -Z).
            for (int i = 0; i < 24; i++) {
                double t = i / 23.0;
                double tx = t * 6.0;
                double ty = 25 + t * 3.5;
                double tz = t * 6.0;
                Location loc = center.clone().add(tx, ty, tz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                float sc = 0.8f - (float) (t * 0.7);
                h.scale(sc, sc, sc).glow(120, 200, 255).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                tailBlocks.add(h);
                allBlocks.add(h);
            }

            // 6 BLUE_STAINED_GLASS wing-fan flame-trails angled out (3 per side)
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 3; i++) {
                    double t = (i + 1) / 4.0;
                    double tx = t * 4.5 + side * 1.5;
                    double ty = 25 + t * 2.0;
                    double tz = t * 4.5 - side * 1.5;
                    Location loc = center.clone().add(tx, ty, tz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                    h.scale(0.3f, 0.3f, 0.3f).glow(200, 120, 255).interpolation(4, 0);
                    h.rotate((float) Math.toRadians(side * 30.0), 0f, 0f, 1f);
                    spawnedEntities.add(h.entity());
                    wingBlocks.add(h);
                    allBlocks.add(h);
                }
            }

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.4f, 0.6f);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, headLoc, 30, 1.5, 1.5, 1.5, 0.05);
            w.spawnParticle(Particle.SNOWFLAKE, headLoc, 20, 1.2, 1.2, 1.2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Descent: ticks 10-60, translate -XYZ at 0.6/tick (so 30 ticks ≈ 18 blocks travel)
            if (tick >= 10 && tick < 60 && !impacted) {
                float dx = -(tick - 10) * 0.4f;
                float dy = -(tick - 10) * 0.8f;
                float dz = -(tick - 10) * 0.4f;
                spinAngle += 0.18f;
                if (tick % 2 == 0) {
                    for (BlockDisplayHandle h : allBlocks) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        setFullTransform(e,
                                new Vector3f(dx - 0.5f, dy - 0.5f, dz - 0.5f),
                                new AxisAngle4f(spinAngle, 1f, 0.4f, 0f),
                                t.getScale(), 2);
                    }
                }
                // Trail particles
                if (tick % 2 == 0) {
                    Location trailLoc = c.clone().add(dx, 25 + dy, dz);
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, trailLoc, 8, 0.8, 0.8, 0.8, 0.05);
                    c.getWorld().spawnParticle(Particle.CLOUD, trailLoc, 6, 1.0, 1.0, 1.0, 0.05);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, trailLoc, 10, 1.2, 1.2, 1.2, 0.05);
                }
            }

            // Impact at tick 55
            if (tick == 55 && !impacted) {
                impacted = true;
                Location impactLoc = c.clone();
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, impactLoc, 2, 1, 1, 1, 0);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, impactLoc, 120, 5, 1, 5, 0.3);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, impactLoc, 150, 6, 1, 6, 0.3);
                c.getWorld().spawnParticle(Particle.FALLING_DUST,
                        impactLoc.clone().add(0, 0.5, 0), 100, 5, 1, 5, 0,
                        Material.WHITE_CONCRETE.createBlockData());

                // Burst-radial: blocks fly outward
                for (int i = 0; i < allBlocks.size(); i++) {
                    BlockDisplayHandle h = allBlocks.get(i);
                    double a = (2.0 * Math.PI * i) / allBlocks.size();
                    float ox = (float) (Math.cos(a) * 7.0);
                    float oz = (float) (Math.sin(a) * 7.0);
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f sc = t.getScale();
                    setFullTransform(e,
                            new Vector3f(ox - 0.5f, 1.5f - 0.5f, oz - 0.5f),
                            new AxisAngle4f((float) Math.PI, (float) Math.random(), (float) Math.random(), (float) Math.random()),
                            new Vector3f(sc.x * 0.3f, sc.y * 0.3f, sc.z * 0.3f), 20);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenMeteor(plugin); }
    }

    // ================================================================
    // #42 — ICE NOVA EXPAND
    // 45 blocks: 1 DIAMOND_BLOCK singularity, 12×3=36 BLUE_ICE radial spear-arms,
    // 8 TINTED_GLASS mid-radius shards. Singularity pulses, spears expand
    // out to r=8, hold, contract back, then collapse.
    // Impact at full expansion + constant central damage.
    // ================================================================
    public static class IceNovaExpand extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> spearArms = new ArrayList<>();
        private final List<BlockDisplayHandle> shardBlocks = new ArrayList<>();
        private boolean burstTriggered = false;

        public IceNovaExpand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_nova_expand", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(720.0); // 3.0 hearts central
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(3120.0); // 13 hearts at full expansion
            config.setImpactRadius(15.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 1 DIAMOND_BLOCK central singularity at Y+1.5
            Location coreLoc = center.clone().add(0, 1.5, 0);
            BlockDisplayHandle core = displayBuilder.spawnBlock(coreLoc, Material.DIAMOND_BLOCK);
            core.scale(0.3f, 0.3f, 0.3f).glow(100, 180, 255).interpolation(15, 0);
            spawnedEntities.add(core.entity());
            coreBlocks.add(core);
            // Animate core pulse-grow to 1.0 over 15t (warning phase)
            scaleBlock(core.entity(), 1.0f, 1.0f, 1.0f, 15);

            // 12 radial spear-arms (each 3 BLUE_ICE tapered)
            for (int i = 0; i < 12; i++) {
                double a = (2.0 * Math.PI * i) / 12;
                List<BlockDisplayHandle> arm = new ArrayList<>();
                for (int s = 0; s < 3; s++) {
                    double r = 0.8 + s * 0.3;
                    double px = Math.cos(a) * r;
                    double pz = Math.sin(a) * r;
                    Location loc = center.clone().add(px, 1.5, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    float sc = 0.6f - s * 0.12f;
                    h.scale(sc, sc, sc).glow(120, 200, 255).interpolation(6, 0);
                    h.rotate((float) a, 0f, 1f, 0f);
                    spawnedEntities.add(h.entity());
                    arm.add(h);
                }
                spearArms.add(arm);
            }

            // 8 TINTED_GLASS mid-radius shards orbit ring at r=3
            for (int i = 0; i < 8; i++) {
                double a = (2.0 * Math.PI * i) / 8 + Math.PI / 16;
                double px = Math.cos(a) * 3.0;
                double pz = Math.sin(a) * 3.0;
                Location loc = center.clone().add(px, 1.8, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 120, 255).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                shardBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.4f, 0.7f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, coreLoc, 25, 1.0, 1.0, 1.0, 0.2);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, coreLoc, 20, 0.8, 0.8, 0.8, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: warning (0-15) — core pulses bigger, no spear movement.
            // Phase 2: burst expand (15-35) — spears translate outward from r=1..8 over 20t.
            // Phase 3: hold (35-50).
            // Phase 4: contract (50-75).
            // Phase 5: collapse to point (75-110).
            // Phase 6: idle until duration end.

            if (tick >= 15 && tick < 35 && tick % 2 == 0) {
                float progress = (tick - 15) / 20f;
                float radius = 1.0f + progress * 7.0f;
                applySpearRadius(radius, 2);
                if (tick == 16) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.5f);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 1.5, 0),
                            80, 4, 1, 4, 0.6);
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 1.5, 0),
                            50, 3, 1, 3, 0.3);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0),
                            120, 5, 1, 5, 0.4);
                }
            }

            // Trigger impact damage at full expansion
            if (tick == 34 && !burstTriggered) {
                burstTriggered = true;
                triggerImpactDamage(c.clone().add(0, 1.0, 0));
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.5f);
            }

            // Hold phase 35-50 — spin spears
            if (tick >= 35 && tick < 50 && tick % 3 == 0) {
                float angle = (tick - 35) * 0.08f;
                for (int i = 0; i < spearArms.size(); i++) {
                    double baseAngle = (2.0 * Math.PI * i) / 12 + angle;
                    List<BlockDisplayHandle> arm = spearArms.get(i);
                    for (BlockDisplayHandle h : arm) {
                        rotateBlock(h.entity(), (float) baseAngle, 0f, 1f, 0f, 3);
                    }
                }
            }

            // Contract 50-75 — spears pull back
            if (tick >= 50 && tick < 75 && tick % 2 == 0) {
                float progress = 1.0f - (tick - 50) / 25f;
                float radius = 1.0f + progress * 7.0f;
                applySpearRadius(radius, 2);
            }

            // Collapse 75-110 — core shrinks, shards converge
            if (tick == 75) {
                for (BlockDisplayHandle h : coreBlocks) {
                    scaleBlock(h.entity(), 0.0f, 0.0f, 0.0f, 20);
                }
                for (BlockDisplayHandle h : shardBlocks) {
                    translateBlock(h.entity(), 0f, 1.5f, 0f, 20);
                    scaleBlock(h.entity(), 0.0f, 0.0f, 0.0f, 20);
                }
                for (List<BlockDisplayHandle> arm : spearArms) {
                    for (BlockDisplayHandle h : arm) {
                        scaleBlock(h.entity(), 0.0f, 0.0f, 0.0f, 20);
                    }
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.4f);
            }

            // Shard orbit
            if (tick % 2 == 0 && tick < 75) {
                float orbitAngle = tick * 0.05f;
                for (int i = 0; i < shardBlocks.size(); i++) {
                    double a = (2.0 * Math.PI * i) / 8 + orbitAngle;
                    float px = (float) (Math.cos(a) * 3.0);
                    float pz = (float) (Math.sin(a) * 3.0);
                    translateBlock(shardBlocks.get(i).entity(), px, 1.8f, pz, 2);
                }
            }
        }

        private void applySpearRadius(float radius, int duration) {
            for (int i = 0; i < spearArms.size(); i++) {
                double a = (2.0 * Math.PI * i) / 12;
                List<BlockDisplayHandle> arm = spearArms.get(i);
                for (int s = 0; s < arm.size(); s++) {
                    double r = radius + s * 0.5;
                    float px = (float) (Math.cos(a) * r);
                    float pz = (float) (Math.sin(a) * r);
                    translateBlock(arm.get(s).entity(), px, 1.5f, pz, duration);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceNovaExpand(plugin); }
    }

    // ================================================================
    // #43 — FROZEN LIGHTNING BOLT
    // 44 blocks: 20 BLUE_ICE zigzag, 8 PACKED_ICE fork-branches, 4 SNOW_BLOCK
    // cloud top, 4 DIAMOND_BLOCK impact-crack ground, 8 TINTED_GLASS micro-arcs.
    // Flashes down 12t, holds 80t pulsing, dissolves bottom-up.
    // Impact-only at strike + constant near impact.
    // ================================================================
    public static class FrozenLightningBolt extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> boltBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> forkBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> cloudBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> impactCrackBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> arcBlocks = new ArrayList<>();
        private boolean struck = false;

        public FrozenLightningBolt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_lightning_bolt", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(720.0); // 3.0 hearts constant
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(2880.0); // 12 hearts strike
            config.setImpactRadius(12.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 4 SNOW_BLOCK thunder-cloud at top Y+18
            double[][] cloudOffsets = {{-1.0, 18, 0}, {1.0, 18, 0}, {0, 18, 1.0}, {0, 18, -1.0}};
            for (double[] off : cloudOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SNOW_BLOCK);
                h.scale(1.2f, 0.6f, 1.2f).glow(245, 245, 245).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                cloudBlocks.add(h);
            }

            // 20 BLUE_ICE zigzag from Y+18 to Y+0 — start hidden (scale 0) and flash in tick 0-12
            for (int i = 0; i < 20; i++) {
                double progress = i / 19.0;
                double zigX = Math.sin(progress * Math.PI * 4) * 0.5; // zigzag
                double zigZ = Math.cos(progress * Math.PI * 3) * 0.4;
                double py = 18.0 - progress * 18.0;
                Location loc = center.clone().add(zigX, py, zigZ);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                boltBlocks.add(h);
            }

            // 8 PACKED_ICE fork-branches splaying off the main bolt
            for (int i = 0; i < 8; i++) {
                double progress = (i + 1) / 9.0;
                double zigX = Math.sin(progress * Math.PI * 4) * 0.5;
                double zigZ = Math.cos(progress * Math.PI * 3) * 0.4;
                double py = 18.0 - progress * 18.0;
                double forkDir = (i % 2 == 0) ? 1.0 : -1.0;
                Location loc = center.clone().add(zigX + forkDir * 0.8, py - 0.3, zigZ + forkDir * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 120, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                forkBlocks.add(h);
            }

            // 4 DIAMOND_BLOCK impact-crack at ground
            double[][] crackOffsets = {{-0.8, 0.1, 0.5}, {0.8, 0.1, -0.4}, {-0.4, 0.1, -0.8}, {0.5, 0.1, 0.7}};
            for (double[] off : crackOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                impactCrackBlocks.add(h);
            }

            // 8 TINTED_GLASS micro-arcs along path
            for (int i = 0; i < 8; i++) {
                double progress = (i + 0.5) / 8.0;
                double zigX = Math.sin(progress * Math.PI * 4) * 0.5;
                double zigZ = Math.cos(progress * Math.PI * 3) * 0.4;
                double py = 18.0 - progress * 18.0;
                Location loc = center.clone().add(zigX, py + 0.5, zigZ);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 120, 255).interpolation(2, 0);
                spawnedEntities.add(h.entity());
                arcBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.2f, 0.5f);
            w.spawnParticle(Particle.CLOUD, center.clone().add(0, 18, 0), 30, 2, 0.5, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Flash-in: bolt appears block-by-block over 12t (top to bottom)
            if (tick < 12) {
                int blockIdx = (tick * boltBlocks.size()) / 12;
                int forkIdx = (tick * forkBlocks.size()) / 12;
                int arcIdx = (tick * arcBlocks.size()) / 12;
                for (int i = 0; i <= Math.min(blockIdx, boltBlocks.size() - 1); i++) {
                    scaleBlock(boltBlocks.get(i).entity(), 0.5f, 0.5f, 0.8f, 2);
                }
                for (int i = 0; i <= Math.min(forkIdx, forkBlocks.size() - 1); i++) {
                    scaleBlock(forkBlocks.get(i).entity(), 0.3f, 0.3f, 0.3f, 2);
                }
                for (int i = 0; i <= Math.min(arcIdx, arcBlocks.size() - 1); i++) {
                    scaleBlock(arcBlocks.get(i).entity(), 0.2f, 0.2f, 0.2f, 2);
                }

                // ELECTRIC_SPARK trail along the descending front
                double frontProgress = tick / 12.0;
                double frontY = 18.0 - frontProgress * 18.0;
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(0, frontY, 0), 12, 0.5, 0.3, 0.5, 0.3);
            }

            // Strike at tick 12 — impact damage + ground cracks appear
            if (tick == 12 && !struck) {
                struck = true;
                triggerImpactDamage(c.clone());
                for (BlockDisplayHandle h : impactCrackBlocks) {
                    scaleBlock(h.entity(), 1.0f, 0.3f, 1.0f, 4);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.6f, 1.2f);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c, 60, 2, 0.5, 2, 0.2);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c, 100, 4, 1, 4, 0.3);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c, 120, 4, 2, 4, 0.5);
            }

            // Active phase: ticks 12-92 — bolt pulses bright every 15t
            if (tick >= 12 && tick < 92) {
                if ((tick - 12) % 15 == 0) {
                    for (BlockDisplayHandle h : boltBlocks) {
                        scaleBlock(h.entity(), 0.7f, 0.7f, 1.2f, 7);
                    }
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.4f);
                } else if ((tick - 12) % 15 == 7) {
                    for (BlockDisplayHandle h : boltBlocks) {
                        scaleBlock(h.entity(), 0.5f, 0.5f, 0.8f, 8);
                    }
                }
                // Continual sparks along the bolt
                if (tick % 3 == 0) {
                    for (BlockDisplayHandle h : boltBlocks) {
                        if (Math.random() < 0.3) {
                            Location bloc = h.entity().getLocation();
                            c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, bloc, 2, 0.2, 0.2, 0.2, 0.3);
                        }
                    }
                }
            }

            // Dissolve bottom-up: ticks 92-110
            if (tick >= 92 && tick < 110) {
                int dissolveFront = ((tick - 92) * boltBlocks.size()) / 18;
                for (int i = 0; i <= Math.min(dissolveFront, boltBlocks.size() - 1); i++) {
                    scaleBlock(boltBlocks.get(boltBlocks.size() - 1 - i).entity(), 0.0f, 0.0f, 0.0f, 2);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenLightningBolt(plugin); }
    }

    // ================================================================
    // #44 — AURORA VORTEX
    // 40 blocks: 18 BLUE_STAINED_GLASS ribbon helix, 12 TINTED_GLASS curtain
    // panels overlapping spiral, 6 DIAMOND_BLOCK glow points, 4 PACKED_ICE
    // vortex spine. Helix Y+0..Y+12, rotates 10 deg/tick, ribbons shimmer.
    // Constant column damage. No impact.
    // ================================================================
    public static class AuroraVortex extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ribbonBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> curtainBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> glowBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spineBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float rotationY = 0f;

        public AuroraVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("aurora_vortex", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1200.0); // 5.0 hearts
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(280);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 18 BLUE_STAINED_GLASS ribbon helix segments (0.6 x 0.15 x 2.0), Y=0..12
            for (int i = 0; i < 18; i++) {
                double t = i / 17.0;
                double a = t * Math.PI * 4; // 2 full revolutions
                double radius = 4.0 - t * 1.0; // tapers slightly
                double px = Math.cos(a) * radius;
                double py = t * 12.0;
                double pz = Math.sin(a) * radius;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(30, i);
                h.rotate((float) a, 0f, 1f, 0f);
                spawnedEntities.add(h.entity());
                ribbonBlocks.add(h);
                allBlocks.add(h);
                // Unfurl scale-in
                scaleBlock(h.entity(), 0.6f, 0.15f, 2.0f, 30);
            }

            // 12 TINTED_GLASS curtain panels overlapping spiral
            for (int i = 0; i < 12; i++) {
                double t = (i + 0.5) / 12.0;
                double a = t * Math.PI * 4 + Math.PI / 6;
                double radius = 3.6 - t * 0.6;
                double px = Math.cos(a) * radius;
                double py = t * 12.0;
                double pz = Math.sin(a) * radius;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 120, 255).interpolation(30, i * 2);
                h.rotate((float) a, 0f, 1f, 0f);
                spawnedEntities.add(h.entity());
                curtainBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.4f, 3.0f, 0.15f, 30);
            }

            // 6 DIAMOND_BLOCK glow points along path
            for (int i = 0; i < 6; i++) {
                double t = (i + 0.5) / 6.0;
                double a = t * Math.PI * 4;
                double radius = 3.6 - t * 0.8;
                double px = Math.cos(a) * radius;
                double py = t * 12.0;
                double pz = Math.sin(a) * radius;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(20, i * 3);
                spawnedEntities.add(h.entity());
                glowBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.25f, 0.25f, 0.25f, 20);
            }

            // 4 PACKED_ICE central vortex-spine
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, i * 3.0 + 1.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(15, i * 2);
                spawnedEntities.add(h.entity());
                spineBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.4f, 2.5f, 0.4f, 15);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.7f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 6, 0), 40, 3, 4, 3, 0.4);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, 6, 0), 30, 3, 4, 3, 0.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Whole vortex rotates Y-axis (10 deg/tick = 0.175 rad/tick)
            rotationY += 0.175f;
            if (tick % 2 == 0 && tick > 30) {
                for (BlockDisplayHandle h : ribbonBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    setFullTransform(e, t.getTranslation(),
                            new AxisAngle4f(rotationY, 0f, 1f, 0f),
                            t.getScale(), 2);
                }
                for (BlockDisplayHandle h : curtainBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    setFullTransform(e, t.getTranslation(),
                            new AxisAngle4f(rotationY, 0f, 1f, 0f),
                            t.getScale(), 2);
                }
            }

            // Ribbons shimmer scale-pulse (0.95..1.05) every 8t
            if (tick > 50 && tick < 240 && tick % 8 == 0) {
                float pulse = 1.0f + 0.05f * (float) Math.sin(tick * 0.2);
                for (BlockDisplayHandle h : ribbonBlocks) {
                    scaleBlock(h.entity(), 0.6f * pulse, 0.15f * pulse, 2.0f * pulse, 8);
                }
            }

            // Aurora particle bands
            if (tick % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = rotationY + i * Math.PI / 3;
                    double r = 3.8;
                    double y = (tick * 0.3 + i * 2) % 12;
                    Location p = c.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.2, 0.2, 0.2, 0.3);
                    c.getWorld().spawnParticle(Particle.GLOW, p, 1, 0.1, 0.1, 0.1, 0.2);
                    if (i % 2 == 0) {
                        c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            }

            // Ambient sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f,
                        0.6f + (float) Math.random() * 0.4f);
            }

            // Dissipate top-down (last 30t)
            if (tick >= 250 && tick % 2 == 0) {
                float collapseProgress = (tick - 250) / 30f;
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    // Top-down: dissolve based on Y position
                    float blockY = (float) h.entity().getLocation().getY() - (float) c.getY();
                    float threshold = 12.0f - collapseProgress * 12.0f;
                    if (blockY > threshold) {
                        Vector3f s = t.getScale();
                        scaleBlock(e, s.x * 0.7f, s.y * 0.7f, s.z * 0.7f, 2);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new AuroraVortex(plugin); }
    }

    // ================================================================
    // #45 — HIBERNATING ICE DRAGON
    // 35 blocks: 12 BLUE_ICE body-coil C-curve, 1 PACKED_ICE head, 4 DIAMOND
    // horns, 6 BLUE_STAINED_GLASS folded wings, 6 ICE tapered tail, 4 OBSIDIAN
    // chest-spikes, 2 CALCITE closed eyes. Dragon breathes slowly, then wakes.
    // Constant near-radius, plus wake-up burst at end.
    // ================================================================
    public static class HibernatingIceDragon extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bodyBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> hornBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> wingBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tailBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> spikeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private boolean woke = false;

        public HibernatingIceDragon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hibernating_ice_dragon", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1560.0); // 6.5 hearts
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setImpactDamage(2400.0); // 10 hearts wake-burst
            config.setImpactRadius(18.0);
            config.setDamageOnImpactOnly(false);
            config.setDurationTicks(250);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 12 BLUE_ICE body-coil in tight C-curve (curve in XZ at Y+1)
            for (int i = 0; i < 12; i++) {
                double t = i / 11.0;
                double a = -Math.PI / 2 + t * Math.PI * 1.2; // C-curve, not full circle
                double radius = 2.5;
                double px = Math.cos(a) * radius;
                double pz = Math.sin(a) * radius;
                double py = 1.0 + Math.sin(t * Math.PI) * 0.3;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 200, 255).interpolation(25, i);
                spawnedEntities.add(h.entity());
                bodyBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 1.2f, 1.2f, 1.2f, 25);
            }

            // 1 PACKED_ICE head tucked at C-curve end
            double headA = -Math.PI / 2 + 1.0 * Math.PI * 1.2;
            double hx = Math.cos(headA) * 2.5;
            double hz = Math.sin(headA) * 2.5;
            Location headLoc = center.clone().add(hx + 0.5, 1.0, hz + 0.5);
            BlockDisplayHandle head = displayBuilder.spawnBlock(headLoc, Material.PACKED_ICE);
            head.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(25, 12);
            spawnedEntities.add(head.entity());
            headBlocks.add(head);
            allBlocks.add(head);
            scaleBlock(head.entity(), 1.0f, 1.0f, 1.0f, 25);

            // 4 DIAMOND_BLOCK horns on head
            double[][] hornOffsets = {{0.3, 0.6, 0.0}, {-0.3, 0.6, 0.0}, {0.5, 0.5, 0.3}, {-0.5, 0.5, 0.3}};
            for (double[] off : hornOffsets) {
                Location loc = headLoc.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(20, 14);
                spawnedEntities.add(h.entity());
                hornBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.3f, 0.5f, 0.3f, 20);
            }

            // 6 BLUE_STAINED_GLASS wings folded over body
            for (int i = 0; i < 6; i++) {
                double a = -Math.PI / 4 + (i / 5.0) * (Math.PI / 2);
                double px = Math.cos(a) * 1.8;
                double pz = Math.sin(a) * 1.8;
                Location loc = center.clone().add(px, 2.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 120, 255).interpolation(22, 16);
                h.rotate((float) a, 0f, 1f, 0f);
                spawnedEntities.add(h.entity());
                wingBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 1.5f, 0.2f, 1.0f, 22);
            }

            // 6 ICE tail wrapped around, tapering 0.5..0.2
            for (int i = 0; i < 6; i++) {
                double t = i / 5.0;
                double a = -Math.PI / 2 - t * Math.PI * 0.5; // tail extends opposite direction
                double radius = 2.5 + t * 0.5;
                double px = Math.cos(a) * radius;
                double pz = Math.sin(a) * radius;
                Location loc = center.clone().add(px, 0.7, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 200, 255).interpolation(22, 18);
                spawnedEntities.add(h.entity());
                tailBlocks.add(h);
                allBlocks.add(h);
                float sc = 0.5f - (float) t * 0.3f;
                scaleBlock(h.entity(), sc, sc, sc, 22);
            }

            // 4 OBSIDIAN chest-spikes
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI / 4) + i * (Math.PI / 8);
                double px = Math.cos(a) * 1.5;
                double pz = Math.sin(a) * 1.5;
                Location loc = center.clone().add(px, 1.2, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.0f, 0.0f, 0.0f).glow(40, 40, 60).interpolation(22, 20);
                spawnedEntities.add(h.entity());
                spikeBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.3f, 0.3f, 0.3f, 22);
            }

            // 2 CALCITE closed eyes
            for (int i = 0; i < 2; i++) {
                Location loc = headLoc.clone().add(0.3 - i * 0.6, 0.2, 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.0f, 0.0f, 0.0f).glow(245, 245, 245).interpolation(20, 22);
                spawnedEntities.add(h.entity());
                eyeBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.2f, 0.2f, 0.2f, 20);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.2f, 0.4f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 2, 0), 60, 3, 2, 3, 0.05);
            w.spawnParticle(Particle.CLOUD, center.clone().add(0, 2, 0), 30, 2.5, 1.5, 2.5, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Breathing scale-pulse cycle 40t, ticks 40-200
            if (tick > 40 && tick < 200 && tick % 4 == 0) {
                float breath = 1.0f + 0.05f * (float) Math.sin((tick - 40) * 0.157); // period 40t
                for (BlockDisplayHandle h : bodyBlocks) {
                    scaleBlock(h.entity(), 1.2f * breath, 1.2f * breath, 1.2f * breath, 4);
                }
                if (tick % 40 == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.9f, 0.4f);
                    c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 2, 1.5), 8, 0.3, 0.3, 0.3, 0.02);
                }
            }

            // Periodic tail twitch (every 60t)
            if (tick > 50 && tick % 60 == 0) {
                for (int i = 0; i < tailBlocks.size(); i++) {
                    BlockDisplayHandle h = tailBlocks.get(i);
                    float angle = (float) Math.toRadians(8.0 * Math.sin(tick * 0.1));
                    rotateBlock(h.entity(), angle, 0f, 1f, 0f, 8);
                }
            }

            // Periodic wing twitch
            if (tick > 80 && tick % 80 == 0) {
                for (int i = 0; i < wingBlocks.size(); i++) {
                    BlockDisplayHandle h = wingBlocks.get(i);
                    float angle = (float) Math.toRadians(15.0);
                    rotateBlock(h.entity(), angle, 1f, 0f, 0f, 10);
                }
            }

            // Wake warning: ticks 200-208 — eyes flash open (scale up + glow)
            if (tick == 200) {
                for (BlockDisplayHandle h : eyeBlocks) {
                    scaleBlock(h.entity(), 0.5f, 0.5f, 0.5f, 8);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.6f);
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 2.5, 0), 40, 1, 0.5, 1, 0.5);
            }

            // Wake burst at tick 208
            if (tick == 208 && !woke) {
                woke = true;
                triggerImpactDamage(c.clone());
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.6f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 2.0f, 1.8f);

                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 2, 0),
                        180, 6, 3, 6, 0.6);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 2, 0),
                        100, 5, 3, 5, 0.3);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2, 0),
                        200, 7, 3, 7, 0.4);

                // Dragon bursts apart — blocks scatter
                for (int i = 0; i < allBlocks.size(); i++) {
                    BlockDisplayHandle h = allBlocks.get(i);
                    double a = (2.0 * Math.PI * i) / allBlocks.size();
                    float ox = (float) (Math.cos(a) * 6.0);
                    float oz = (float) (Math.sin(a) * 6.0);
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f sc = t.getScale();
                    setFullTransform(e,
                            new Vector3f(ox - 0.5f, 3.0f - 0.5f, oz - 0.5f),
                            new AxisAngle4f((float) Math.PI, (float) Math.random(), (float) Math.random(), (float) Math.random()),
                            new Vector3f(sc.x * 0.3f, sc.y * 0.3f, sc.z * 0.3f), 22);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HibernatingIceDragon(plugin); }
    }

    // ================================================================
    // #46 — FROZEN COMET (alt: ice singularity black-hole)
    // 43 blocks: 1 OBSIDIAN dark core, 16 BLUE_ICE accretion disc ring,
    // 8 PACKED_ICE inner ring, 12 TINTED_GLASS event-horizon shroud,
    // 6 DIAMOND_BLOCK jet-bursts. Disc spins 15deg/tick, inner counter-spins,
    // core pulses. Constant 11r damage, stronger inside r=4.
    // ================================================================
    public static class FrozenComet extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> shroudBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> jetBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float outerSpin = 0f;
        private float innerSpin = 0f;

        public FrozenComet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_comet", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1440.0); // 6.0 hearts at edge (9.0 inside handled via second impact pulse)
            config.setDamageRadius(16.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(2160.0); // 9.0 hearts pulse for inner radius
            config.setImpactRadius(6.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 1 OBSIDIAN dark heart at Y+2
            Location coreLoc = center.clone().add(0, 2.0, 0);
            BlockDisplayHandle core = displayBuilder.spawnBlock(coreLoc, Material.OBSIDIAN);
            core.scale(0.0f, 0.0f, 0.0f).glow(40, 40, 60).interpolation(25, 0);
            spawnedEntities.add(core.entity());
            coreBlocks.add(core);
            allBlocks.add(core);
            scaleBlock(core.entity(), 0.4f, 0.4f, 0.4f, 25);

            // 16 BLUE_ICE accretion-disc in flat ring at r=4
            for (int i = 0; i < 16; i++) {
                double a = (2.0 * Math.PI * i) / 16;
                double px = Math.cos(a) * 4.0;
                double pz = Math.sin(a) * 4.0;
                Location loc = center.clone().add(px, 2.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 200, 255).interpolation(20, i);
                spawnedEntities.add(h.entity());
                outerRing.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.5f, 0.5f, 0.5f, 20);
            }

            // 8 PACKED_ICE inner ring at r=2
            for (int i = 0; i < 8; i++) {
                double a = (2.0 * Math.PI * i) / 8;
                double px = Math.cos(a) * 2.0;
                double pz = Math.sin(a) * 2.0;
                Location loc = center.clone().add(px, 2.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(18, i);
                spawnedEntities.add(h.entity());
                innerRing.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.3f, 0.3f, 0.3f, 18);
            }

            // 12 TINTED_GLASS event-horizon shroud (hemispherical above disc)
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 12; i++) {
                double y = i / 11.0; // 0..1
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * radiusAtY * 3.0;
                double py = y * 1.8 + 2.0;
                double pz = Math.sin(theta) * radiusAtY * 3.0;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 120, 255).interpolation(22, i + 4);
                spawnedEntities.add(h.entity());
                shroudBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.4f, 0.4f, 0.4f, 22);
            }

            // 6 DIAMOND_BLOCK jet-bursts (3 up, 3 down)
            for (int i = 0; i < 3; i++) {
                Location up = center.clone().add(0, 3.0 + i * 0.8, 0);
                BlockDisplayHandle hu = displayBuilder.spawnBlock(up, Material.DIAMOND_BLOCK);
                hu.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(20, 18 + i);
                spawnedEntities.add(hu.entity());
                jetBlocks.add(hu);
                allBlocks.add(hu);
                scaleBlock(hu.entity(), 0.3f, 0.3f, 0.3f, 20);

                Location down = center.clone().add(0, 1.0 - i * 0.8, 0);
                BlockDisplayHandle hd = displayBuilder.spawnBlock(down, Material.DIAMOND_BLOCK);
                hd.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(20, 18 + i);
                spawnedEntities.add(hd.entity());
                jetBlocks.add(hd);
                allBlocks.add(hd);
                scaleBlock(hd.entity(), 0.3f, 0.3f, 0.3f, 20);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.6f, 0.3f);
            w.spawnParticle(Particle.SCULK_SOUL, coreLoc, 50, 1.5, 1.5, 1.5, 0.05);
            w.spawnParticle(Particle.REVERSE_PORTAL, coreLoc, 30, 2, 2, 2, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Outer disc rotates fast (15 deg/tick = 0.262 rad/tick)
            outerSpin += 0.262f;
            innerSpin -= 0.262f;

            // Hungry-pull: every tick, blocks creep inward by small amount (visual translation)
            // Use cycle-based small radial pull
            if (tick > 25 && tick % 2 == 0) {
                // Outer disc rotation
                for (int i = 0; i < outerRing.size(); i++) {
                    double a = (2.0 * Math.PI * i) / 16 + outerSpin;
                    // Slight inward pull cycling
                    double pullCycle = 4.0 + 0.15 * Math.sin(tick * 0.15);
                    float px = (float) (Math.cos(a) * pullCycle);
                    float pz = (float) (Math.sin(a) * pullCycle);
                    translateBlock(outerRing.get(i).entity(), px, 2.0f, pz, 2);
                }
                // Inner ring counter
                for (int i = 0; i < innerRing.size(); i++) {
                    double a = (2.0 * Math.PI * i) / 8 + innerSpin;
                    double pullCycle = 2.0 + 0.1 * Math.sin(tick * 0.15);
                    float px = (float) (Math.cos(a) * pullCycle);
                    float pz = (float) (Math.sin(a) * pullCycle);
                    translateBlock(innerRing.get(i).entity(), px, 2.0f, pz, 2);
                }
            }

            // Core pulses: scale-pulse 0.4 -> 0.55
            if (tick > 25 && tick < 250 && tick % 6 == 0) {
                float pulse = 0.4f + 0.15f * (float) Math.abs(Math.sin(tick * 0.1));
                for (BlockDisplayHandle h : coreBlocks) {
                    scaleBlock(h.entity(), pulse, pulse, pulse, 6);
                }
            }

            // Jets pulse
            if (tick % 10 == 0 && tick > 25) {
                for (BlockDisplayHandle h : jetBlocks) {
                    float pulse = 0.3f + 0.1f * (float) Math.random();
                    scaleBlock(h.entity(), pulse, pulse, pulse, 5);
                }
            }

            // Inner-radius impact pulse every 30t (deals 9 hearts inside r=4)
            if (tick > 25 && tick % 30 == 0) {
                triggerImpactDamage(c.clone().add(0, 2.0, 0));
            }

            // Particles
            if (tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 2.0, 0),
                        15, 1.5, 0.5, 1.5, 0.1);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 2.0, 0),
                        10, 3, 0.3, 3, 0.4);
            }
            if (tick % 4 == 0) {
                // Jets soul-fire flame
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 4.5, 0),
                        8, 0.3, 0.8, 0.3, 0.1);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, -0.5, 0),
                        8, 0.3, 0.8, 0.3, 0.1);
            }

            // Ambient drone
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.6f, 0.4f);
            }
            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.6f, 0.5f);
            }

            // Collapse final 18t
            if (tick >= 262 && tick % 2 == 0) {
                float collapse = 1.0f - (tick - 262) / 18f;
                if (collapse < 0) collapse = 0;
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    // Pull inward + shrink
                    Vector3f trans = t.getTranslation();
                    setFullTransform(e,
                            new Vector3f(trans.x * collapse, 2.0f * collapse - 0.5f + 0.5f, trans.z * collapse),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(s.x * collapse, s.y * collapse, s.z * collapse), 2);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenComet(plugin); }
    }

    // ================================================================
    // #47 — ICE SINGULARITY (celestial moonlet)
    // 38 blocks: 16 BLUE_ICE sphere-body radius 2, 8 CALCITE crater-pits,
    // 4 BLUE_STAINED_GLASS ring-bands orbiting, 4 OBSIDIAN+4 ICE floating
    // debris, 2 WHITE_CONCRETE polar caps. Descends Y-0.08/tick, rotates,
    // rings counter-spin. Lands and cracks open.
    // Constant + landing impact.
    // ================================================================
    public static class IceSingularity extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bodyBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> craterBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> debrisBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> polarBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float bodyYaw = 0f;
        private float ringSpin = 0f;
        private boolean landed = false;
        private float descentY = 15f;

        public IceSingularity(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_singularity", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1320.0); // 5.5 hearts
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(2880.0); // 12 hearts landing
            config.setImpactRadius(18.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 16 BLUE_ICE sphere body radius 2 (fibonacci sphere)
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 16; i++) {
                double y = 1.0 - (2.0 * i / 15.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * radiusAtY * 2.0;
                double py = y * 2.0;
                double pz = Math.sin(theta) * radiusAtY * 2.0;
                Location loc = center.clone().add(px, py + descentY, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 200, 255).interpolation(22, i);
                spawnedEntities.add(h.entity());
                bodyBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.6f, 0.6f, 0.6f, 22);
            }

            // 8 CALCITE crater-pits on surface
            for (int i = 0; i < 8; i++) {
                double a = (2.0 * Math.PI * i) / 8;
                double py = (i % 2 == 0) ? 0.6 : -0.6;
                double radiusAtY = Math.sqrt(1 - py * py / 4) * 2.0;
                double px = Math.cos(a) * radiusAtY;
                double pz = Math.sin(a) * radiusAtY;
                Location loc = center.clone().add(px * 0.85, py * 0.9 + descentY, pz * 0.85);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.0f, 0.0f, 0.0f).glow(245, 245, 245).interpolation(20, i + 4);
                spawnedEntities.add(h.entity());
                craterBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.3f, 0.3f, 0.3f, 20);
            }

            // 4 BLUE_STAINED_GLASS ring-bands orbiting (one ring approximated by 4 segments at r=2.8)
            for (int i = 0; i < 4; i++) {
                double a = (2.0 * Math.PI * i) / 4;
                double px = Math.cos(a) * 2.8;
                double pz = Math.sin(a) * 2.8;
                Location loc = center.clone().add(px, descentY, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 120, 255).interpolation(20, i + 8);
                h.rotate((float) a, 0f, 1f, 0f);
                spawnedEntities.add(h.entity());
                ringBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.2f, 0.1f, 3.0f, 20);
            }

            // 4 OBSIDIAN + 4 ICE debris nearby
            for (int i = 0; i < 4; i++) {
                double a = (2.0 * Math.PI * i) / 4 + Math.PI / 4;
                double r = 3.5;
                double px = Math.cos(a) * r;
                double pz = Math.sin(a) * r;
                Location loc = center.clone().add(px, descentY + 0.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.0f, 0.0f, 0.0f).glow(40, 40, 60).interpolation(20, i + 12);
                spawnedEntities.add(h.entity());
                debrisBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.3f, 0.3f, 0.3f, 20);
            }
            for (int i = 0; i < 4; i++) {
                double a = (2.0 * Math.PI * i) / 4 + Math.PI / 8;
                double r = 3.2;
                double px = Math.cos(a) * r;
                double pz = Math.sin(a) * r;
                Location loc = center.clone().add(px, descentY - 0.3, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 200, 255).interpolation(20, i + 14);
                spawnedEntities.add(h.entity());
                debrisBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.2f, 0.2f, 0.2f, 20);
            }

            // 2 WHITE_CONCRETE polar caps
            Location north = center.clone().add(0, descentY + 1.7, 0);
            BlockDisplayHandle pn = displayBuilder.spawnBlock(north, Material.WHITE_CONCRETE);
            pn.scale(0.0f, 0.0f, 0.0f).glow(245, 245, 245).interpolation(20, 18);
            spawnedEntities.add(pn.entity());
            polarBlocks.add(pn);
            allBlocks.add(pn);
            scaleBlock(pn.entity(), 0.5f, 0.3f, 0.5f, 20);

            Location south = center.clone().add(0, descentY - 1.7, 0);
            BlockDisplayHandle ps = displayBuilder.spawnBlock(south, Material.WHITE_CONCRETE);
            ps.scale(0.0f, 0.0f, 0.0f).glow(245, 245, 245).interpolation(20, 18);
            spawnedEntities.add(ps.entity());
            polarBlocks.add(ps);
            allBlocks.add(ps);
            scaleBlock(ps.entity(), 0.5f, 0.3f, 0.5f, 20);

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.6f);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, descentY, 0),
                    40, 2, 2, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Descent: -0.08/tick — full body translates down
            if (tick > 22 && tick < 200 && !landed) {
                descentY -= 0.08f;
                bodyYaw += 0.0875f; // 5 deg/tick
                ringSpin -= 0.13f;

                if (tick % 2 == 0) {
                    // Body: translate down with yaw rotation
                    for (int i = 0; i < bodyBlocks.size(); i++) {
                        BlockDisplay e = bodyBlocks.get(i).entity();
                        Transformation t = e.getTransformation();
                        Vector3f trans = t.getTranslation();
                        // Re-derive original offset (translation already has -0.5 baseline)
                        // We translate to (origX, descentY+localY, origZ)
                        // Use entity location as anchor — keep XZ, update Y
                        // Simpler: just apply yaw rotation, and translate Y to descentY+localY
                        double y = 1.0 - (2.0 * i / 15.0);
                        double localY = y * 2.0;
                        double radiusAtY = Math.sqrt(1 - y * y);
                        double theta = Math.PI * (3 - Math.sqrt(5)) * i;
                        float px = (float) (Math.cos(theta) * radiusAtY * 2.0);
                        float pz = (float) (Math.sin(theta) * radiusAtY * 2.0);
                        // Apply body yaw rotation via Y axis
                        setFullTransform(e,
                                new Vector3f(px - 0.5f, (float) (descentY + localY) - 0.5f, pz - 0.5f),
                                new AxisAngle4f(bodyYaw, 0f, 1f, 0f),
                                t.getScale(), 2);
                    }
                    // Craters move with body
                    for (int i = 0; i < craterBlocks.size(); i++) {
                        double a = (2.0 * Math.PI * i) / 8 + bodyYaw;
                        double pyLocal = (i % 2 == 0) ? 0.6 : -0.6;
                        double radiusAtY = Math.sqrt(1 - pyLocal * pyLocal / 4) * 2.0;
                        float px = (float) (Math.cos(a) * radiusAtY * 0.85);
                        float pz = (float) (Math.sin(a) * radiusAtY * 0.85);
                        translateBlock(craterBlocks.get(i).entity(),
                                px, (float) (descentY + pyLocal * 0.9), pz, 2);
                    }
                    // Polar caps move down with body
                    translateBlock(polarBlocks.get(0).entity(), 0f, descentY + 1.7f, 0f, 2);
                    translateBlock(polarBlocks.get(1).entity(), 0f, descentY - 1.7f, 0f, 2);

                    // Rings counter-spin
                    for (int i = 0; i < ringBlocks.size(); i++) {
                        double a = (2.0 * Math.PI * i) / 4 + ringSpin;
                        float px = (float) (Math.cos(a) * 2.8);
                        float pz = (float) (Math.sin(a) * 2.8);
                        BlockDisplay e = ringBlocks.get(i).entity();
                        Transformation t = e.getTransformation();
                        setFullTransform(e,
                                new Vector3f(px - 0.5f, descentY - 0.5f, pz - 0.5f),
                                new AxisAngle4f((float) a, 0f, 1f, 0f),
                                t.getScale(), 2);
                    }
                    // Debris orbits faster
                    for (int i = 0; i < debrisBlocks.size(); i++) {
                        boolean isObsidian = i < 4;
                        double baseAngle = isObsidian ? (Math.PI / 4) : (Math.PI / 8);
                        double a = (2.0 * Math.PI * (i % 4)) / 4 + baseAngle + tick * 0.08;
                        double r = isObsidian ? 3.5 : 3.2;
                        float px = (float) (Math.cos(a) * r);
                        float pz = (float) (Math.sin(a) * r);
                        float yOff = isObsidian ? 0.5f : -0.3f;
                        translateBlock(debrisBlocks.get(i).entity(),
                                px, descentY + yOff, pz, 2);
                    }
                }

                if (tick % 8 == 0) {
                    c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, descentY, 0),
                            15, 2, 1, 2, 0.05);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, descentY, 0),
                            8, 2.5, 0.3, 2.5, 0.3);
                }
            }

            // Land at descent reaching close to player Y (~descentY <= 2)
            if (!landed && descentY <= 2.0f) {
                landed = true;
                Location landLoc = c.clone();
                triggerImpactDamage(landLoc);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.4f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.5f);
                c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, landLoc, 2, 1, 1, 1, 0);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, landLoc, 200, 6, 1, 6, 0.4);
                c.getWorld().spawnParticle(Particle.FALLING_DUST,
                        landLoc.clone().add(0, 0.5, 0), 100, 5, 1, 5, 0,
                        Material.WHITE_CONCRETE.createBlockData());

                // Split body in half — left half goes -X, right half +X
                for (int i = 0; i < bodyBlocks.size(); i++) {
                    BlockDisplayHandle h = bodyBlocks.get(i);
                    float ox = (i % 2 == 0) ? -4f : 4f;
                    h.animateTo(new Vector3f(ox - 0.5f, 0.5f - 0.5f, (float) Math.random() - 0.5f),
                            new AxisAngle4f((float) Math.PI, 1f, (float) Math.random(), 0f),
                            new Vector3f(0.3f, 0.3f, 0.3f), 25);
                }
                for (BlockDisplayHandle h : craterBlocks) {
                    scaleBlock(h.entity(), 0.0f, 0.0f, 0.0f, 25);
                }
                for (BlockDisplayHandle h : ringBlocks) {
                    scaleBlock(h.entity(), 0.0f, 0.0f, 0.0f, 25);
                }
                for (BlockDisplayHandle h : polarBlocks) {
                    scaleBlock(h.entity(), 0.0f, 0.0f, 0.0f, 25);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceSingularity(plugin); }
    }

    // ================================================================
    // #48 — GLACIAL BLACK HOLE (Constellation of Winter — design source)
    // 44 blocks: 12 DIAMOND_BLOCK star-points constellation, 18 BLUE_STAINED_GLASS
    // connecting lines, 8 AMETHYST_BLOCK main-stars, 6 TINTED_GLASS nebula
    // backdrop. Stars twinkle in, lines connect, rotates 2 deg/tick.
    // ================================================================
    public static class GlacialBlackHole extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> starBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> mainStarBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> lineBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> nebulaBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float rotation = 0f;

        // Constellation pattern (dipper-like shape) — local offsets
        private static final double[][] CONSTELLATION_POINTS = {
                {-4, 8, 2}, {-2, 9, 1}, {0, 9.5, 0}, {2, 9, -1},
                {3, 7, -2}, {2, 5, -1}, {0, 4, 0}, {-2, 5, 1},
                {-3, 7, 2}, {-1, 6, 0}, {1, 6, 0}, {-3, 9, 0}
        };

        public GlacialBlackHole(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_black_hole", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1320.0); // 5.5 hearts
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(280);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 12 DIAMOND star-points twinkle in
            for (int i = 0; i < 12; i++) {
                double[] pt = CONSTELLATION_POINTS[i];
                Location loc = center.clone().add(pt[0], pt[1], pt[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(6, i * 2);
                spawnedEntities.add(h.entity());
                starBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.4f, 0.4f, 0.4f, 6);
            }

            // 8 brighter main stars (AMETHYST_BLOCK 0.5) at first 8 positions, offset slightly
            for (int i = 0; i < 8; i++) {
                double[] pt = CONSTELLATION_POINTS[i];
                Location loc = center.clone().add(pt[0] + 0.15, pt[1] + 0.15, pt[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 120, 255).interpolation(6, i * 2 + 1);
                spawnedEntities.add(h.entity());
                mainStarBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 0.5f, 0.5f, 0.5f, 6);
            }

            // 18 BLUE_STAINED_GLASS connecting lines (delayed in)
            int[][] connections = {
                    {0,1}, {1,2}, {2,3}, {3,4}, {4,5}, {5,6}, {6,7}, {7,8}, {8,0},
                    {1,9}, {9,7}, {2,10}, {10,6}, {0,11}, {11,3}, {4,10}, {5,9}, {6,11}
            };
            for (int i = 0; i < 18; i++) {
                double[] a = CONSTELLATION_POINTS[connections[i][0]];
                double[] b = CONSTELLATION_POINTS[connections[i][1]];
                double mx = (a[0] + b[0]) / 2.0;
                double my = (a[1] + b[1]) / 2.0;
                double mz = (a[2] + b[2]) / 2.0;
                double dx = b[0] - a[0];
                double dy = b[1] - a[1];
                double dz = b[2] - a[2];
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                float yaw = (float) Math.atan2(dz, dx);
                float pitch = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
                Location loc = center.clone().add(mx, my, mz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(8, 25 + i);
                spawnedEntities.add(h.entity());
                lineBlocks.add(h);
                allBlocks.add(h);

                // Configure rotation + scale
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(8);
                e.setInterpolationDelay(25 + i);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(yaw, 0f, 1f, 0f),
                        new Vector3f(0.1f, 0.1f, (float) length),
                        new AxisAngle4f(-pitch, (float) Math.sin(yaw), 0f, (float) Math.cos(yaw))
                ));
            }

            // 6 TINTED_GLASS nebula-cloud backdrop
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * i) / 6;
                double r = 4.5;
                double px = Math.cos(a) * r;
                double pz = Math.sin(a) * r;
                double py = 6.5 + Math.sin(a) * 1.5;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 120, 255).interpolation(15, i * 3);
                spawnedEntities.add(h.entity());
                nebulaBlocks.add(h);
                allBlocks.add(h);
                scaleBlock(h.entity(), 1.0f, 1.0f, 1.0f, 15);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.0f, 0.8f);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, 7, 0), 60, 4, 3, 4, 0.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow rotation 2 deg/tick = 0.035 rad/tick
            rotation += 0.035f;
            if (tick > 50 && tick % 3 == 0) {
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    // Skip lines because they need their own rotation; do all in group
                    if (lineBlocks.contains(h)) continue;
                    setFullTransform(e, t.getTranslation(),
                            new AxisAngle4f(rotation, 0f, 1f, 0f),
                            t.getScale(), 3);
                }
            }

            // Stars twinkle individually — random pulse
            if (tick > 30 && tick % 5 == 0) {
                int idx = (int) (Math.random() * starBlocks.size());
                BlockDisplayHandle h = starBlocks.get(idx);
                scaleBlock(h.entity(), 0.6f, 0.6f, 0.6f, 5);
                // Scale back after small delay (re-schedule via tick later)
                final BlockDisplayHandle finalH = h;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (finalH.entity().isValid()) {
                        scaleBlock(finalH.entity(), 0.4f, 0.4f, 0.4f, 5);
                    }
                }, 5L);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f,
                        1.0f + (float) Math.random() * 1.0f);
            }

            // Lines re-trace (scale-pulse along path) every 40t
            if (tick > 60 && tick % 40 == 0) {
                for (BlockDisplayHandle h : lineBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    scaleBlock(e, s.x * 1.5f, s.y * 1.5f, s.z, 8);
                }
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.8f, 1.0f);
            }
            if (tick > 60 && (tick % 40) == 8) {
                for (BlockDisplayHandle h : lineBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    scaleBlock(e, 0.1f, 0.1f, s.z, 5);
                }
            }

            // Particles
            if (tick % 2 == 0) {
                for (int i = 0; i < CONSTELLATION_POINTS.length; i++) {
                    if (Math.random() < 0.3) {
                        double[] pt = CONSTELLATION_POINTS[i];
                        Location p = c.clone().add(pt[0], pt[1], pt[2]);
                        c.getWorld().spawnParticle(Particle.GLOW, p, 3, 0.3, 0.3, 0.3, 0.3);
                        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.2);
                    }
                }
            }
            if (tick % 4 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = (2.0 * Math.PI * i) / 6 + tick * 0.02;
                    Location p = c.clone().add(Math.cos(a) * 4.5, 6.5, Math.sin(a) * 4.5);
                    c.getWorld().spawnParticle(Particle.SCULK_SOUL, p, 2, 0.4, 0.4, 0.4, 0.05);
                }
            }

            // Dissipate last 25t — lines first, then stars
            if (tick >= 255 && tick < 270) {
                for (BlockDisplayHandle h : lineBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    scaleBlock(e, s.x * 0.8f, s.y * 0.8f, s.z * 0.8f, 2);
                }
            }
            if (tick >= 265 && tick < 280) {
                for (BlockDisplayHandle h : starBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    scaleBlock(e, s.x * 0.8f, s.y * 0.8f, s.z * 0.8f, 2);
                }
                for (BlockDisplayHandle h : mainStarBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    scaleBlock(e, s.x * 0.8f, s.y * 0.8f, s.z * 0.8f, 2);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GlacialBlackHole(plugin); }
    }

    // ================================================================
    // #49 — ETERNAL ICE CONSTELLATION (Frozen Time Rift — design source)
    // 39 blocks: 1 OBSIDIAN rift-core slit, 12 PACKED_ICE jagged-edge crystals,
    // 8 BLUE_ICE clockwork fragments, 8 TINTED_GLASS reality shards,
    // 6 DIAMOND_BLOCK anchor-glow, 4 LIGHT_BLUE_CONCRETE markers.
    // Vertical slit, edge-crystals shudder, fragments rotate, periodically
    // extends damage-tendrils.
    // ================================================================
    public static class EternalIceConstellation extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> edgeCrystals = new ArrayList<>();
        private final List<BlockDisplayHandle> fragmentBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> shardBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> anchorBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> markerBlocks = new ArrayList<>();

        public EternalIceConstellation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eternal_ice_constellation", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1560.0); // 6.5 hearts
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(265);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 1 OBSIDIAN rift-core slit at Y+2 (0.4 x 4.0 x 0.2) — starts as point, tears open over 18t
            Location coreLoc = center.clone().add(0, 4.0, 0);
            BlockDisplayHandle core = displayBuilder.spawnBlock(coreLoc, Material.OBSIDIAN);
            core.scale(0.0f, 0.0f, 0.0f).glow(40, 40, 60).interpolation(18, 0);
            spawnedEntities.add(core.entity());
            coreBlocks.add(core);
            scaleBlock(core.entity(), 0.4f, 4.0f, 0.2f, 18);

            // 12 PACKED_ICE jagged-edge crystals lining the tear (left+right of slit)
            for (int i = 0; i < 12; i++) {
                double side = (i < 6) ? -0.4 : 0.4;
                double yOff = 2.0 + (i % 6) * 0.7;
                double zOff = (Math.random() - 0.5) * 0.3;
                Location loc = center.clone().add(side, yOff, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(15, 2 + i);
                spawnedEntities.add(h.entity());
                edgeCrystals.add(h);
                scaleBlock(h.entity(), 0.4f, 0.4f, 0.4f, 15);
            }

            // 8 BLUE_ICE clockwork-fragments at various rotations (frozen mid-fall)
            for (int i = 0; i < 8; i++) {
                double a = (2.0 * Math.PI * i) / 8;
                double r = 2.0;
                double px = Math.cos(a) * r;
                double py = 4.0 + Math.sin(i * 0.7) * 1.5;
                double pz = Math.sin(a) * r;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 200, 255).interpolation(15, 6 + i);
                h.rotate((float) (Math.random() * Math.PI), 1f, (float) Math.random(), 0f);
                spawnedEntities.add(h.entity());
                fragmentBlocks.add(h);
                scaleBlock(h.entity(), 0.3f, 0.3f, 0.3f, 15);
            }

            // 8 TINTED_GLASS reality-shards surrounding
            for (int i = 0; i < 8; i++) {
                double a = (2.0 * Math.PI * i) / 8 + Math.PI / 8;
                double r = 3.0;
                double px = Math.cos(a) * r;
                double py = 3.5 + (i % 2) * 1.5;
                double pz = Math.sin(a) * r;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 120, 255).interpolation(15, 10 + i);
                spawnedEntities.add(h.entity());
                shardBlocks.add(h);
                scaleBlock(h.entity(), 0.5f, 0.5f, 0.5f, 15);
            }

            // 6 DIAMOND_BLOCK anchor-glow stabilizing
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * i) / 6;
                double r = 3.6;
                double px = Math.cos(a) * r;
                double py = 4.0;
                double pz = Math.sin(a) * r;
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(15, 14 + i);
                spawnedEntities.add(h.entity());
                anchorBlocks.add(h);
                scaleBlock(h.entity(), 0.3f, 0.3f, 0.3f, 15);
            }

            // 4 LIGHT_BLUE_CONCRETE markers
            double[][] markerOffsets = {{1.6, 0.3, 1.6}, {-1.6, 0.3, 1.6}, {1.6, 0.3, -1.6}, {-1.6, 0.3, -1.6}};
            for (double[] off : markerOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_CONCRETE);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 120, 255).interpolation(15, 20);
                spawnedEntities.add(h.entity());
                markerBlocks.add(h);
                scaleBlock(h.entity(), 0.2f, 0.2f, 0.2f, 15);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 4, 0), 50, 0.5, 2, 0.5, 0.05);
            w.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, 4, 0), 30, 0.5, 2, 0.5, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Edge-crystal shudder (small random translate)
            if (tick > 25 && tick % 5 == 0) {
                for (BlockDisplayHandle h : edgeCrystals) {
                    float jitterX = (float) ((Math.random() - 0.5) * 0.2);
                    float jitterZ = (float) ((Math.random() - 0.5) * 0.2);
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f trans = t.getTranslation();
                    setFullTransform(e,
                            new Vector3f(trans.x + jitterX, trans.y, trans.z + jitterZ),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(), 5);
                }
            }

            // Fragments rotate slowly
            if (tick > 25 && tick % 3 == 0) {
                for (int i = 0; i < fragmentBlocks.size(); i++) {
                    BlockDisplayHandle h = fragmentBlocks.get(i);
                    float angle = tick * 0.04f + i * 0.7f;
                    rotateBlock(h.entity(), angle, 1f, 1f, 0f, 3);
                }
            }

            // Shards orbit slowly
            if (tick > 25 && tick % 2 == 0) {
                float orbit = tick * 0.04f;
                for (int i = 0; i < shardBlocks.size(); i++) {
                    double a = (2.0 * Math.PI * i) / 8 + Math.PI / 8 + orbit;
                    float px = (float) (Math.cos(a) * 3.0);
                    float pz = (float) (Math.sin(a) * 3.0);
                    float py = 3.5f + (i % 2) * 1.5f;
                    translateBlock(shardBlocks.get(i).entity(), px, py, pz, 2);
                }
            }

            // Anchor pulse
            if (tick > 25 && tick % 8 == 0) {
                float pulse = 0.3f + 0.1f * (float) Math.abs(Math.sin(tick * 0.1));
                for (BlockDisplayHandle h : anchorBlocks) {
                    scaleBlock(h.entity(), pulse, pulse, pulse, 8);
                }
            }

            // Tendril extension every 40t — anchor briefly extends out
            if (tick > 50 && tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.4f);
                for (int i = 0; i < anchorBlocks.size(); i++) {
                    double a = (2.0 * Math.PI * i) / 6;
                    float px = (float) (Math.cos(a) * 6.0);
                    float pz = (float) (Math.sin(a) * 6.0);
                    BlockDisplayHandle h = anchorBlocks.get(i);
                    translateBlock(h.entity(), px, 4.0f, pz, 8);
                }
                // Tendril particles
                for (int i = 0; i < 6; i++) {
                    double a = (2.0 * Math.PI * i) / 6;
                    Location end = c.clone().add(Math.cos(a) * 6, 4, Math.sin(a) * 6);
                    Location start = c.clone().add(0, 4, 0);
                    DisplayBuilder.particleLine(start, end, Particle.ELECTRIC_SPARK, 5, null);
                }
            }
            if (tick > 50 && (tick % 40) == 10) {
                // Pull anchors back
                for (int i = 0; i < anchorBlocks.size(); i++) {
                    double a = (2.0 * Math.PI * i) / 6;
                    float px = (float) (Math.cos(a) * 3.6);
                    float pz = (float) (Math.sin(a) * 3.6);
                    translateBlock(anchorBlocks.get(i).entity(), px, 4.0f, pz, 8);
                }
            }

            // Particles
            if (tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 4, 0),
                        15, 0.4, 2, 0.4, 0.05);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 4, 0),
                        8, 0.3, 1.5, 0.3, 0.05);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 4, 0),
                        10, 1, 2, 1, 0.3);
            }

            // Ambient drone
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.7f, 0.5f);
            }

            // Close from edges inward (last 22t)
            if (tick >= 243 && tick % 2 == 0) {
                float closeProgress = 1.0f - (tick - 243) / 22f;
                if (closeProgress < 0) closeProgress = 0;
                for (BlockDisplayHandle h : edgeCrystals) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    scaleBlock(e, s.x * closeProgress, s.y * closeProgress, s.z * closeProgress, 2);
                }
                for (BlockDisplayHandle h : shardBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    scaleBlock(e, s.x * closeProgress, s.y * closeProgress, s.z * closeProgress, 2);
                }
                for (BlockDisplayHandle h : coreBlocks) {
                    BlockDisplay e = h.entity();
                    scaleBlock(e, 0.4f * closeProgress, 4.0f * closeProgress, 0.2f * closeProgress, 2);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new EternalIceConstellation(plugin); }
    }

    // ================================================================
    // #50 — FROZEN NEBULA (star-shard cloud)
    // 60 blocks: 30 shard-pairs (each = 1 DIAMOND_BLOCK 0.25 + 1 BLUE_ICE 0.15)
    // in chaotic 3D distribution around center (radius 8). Shards orbit, 5
    // periodically fall toward ground as projectiles. Constant + impact.
    // ================================================================
    public static class FrozenNebula extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shardA = new ArrayList<>();
        private final List<BlockDisplayHandle> shardB = new ArrayList<>();
        private final double[][] shardOffsets = new double[30][3];
        private float orbit = 0f;
        private final int[] fallingShardIdx = new int[5];
        private int fallStartTick = -1;

        public FrozenNebula(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_nebula", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1080.0); // 4.5 hearts
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(960.0); // 4.0 hearts per falling shard
            config.setImpactRadius(3.75);
            config.setDurationTicks(245);
            config.setCooldownTicks(60);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 30 shard-pairs in chaotic 3D distribution radius 8 (centered at Y+5 height)
            for (int i = 0; i < 30; i++) {
                double theta = Math.random() * 2 * Math.PI;
                double phi = Math.acos(2 * Math.random() - 1);
                double r = 3.0 + Math.random() * 5.0;
                double px = Math.sin(phi) * Math.cos(theta) * r;
                double py = Math.cos(phi) * r + 5.0;
                double pz = Math.sin(phi) * Math.sin(theta) * r;
                shardOffsets[i][0] = px;
                shardOffsets[i][1] = py;
                shardOffsets[i][2] = pz;

                // Each pair: DIAMOND_BLOCK + adjacent BLUE_ICE
                Location loc = center.clone().add(px, py, pz);
                BlockDisplayHandle ha = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                ha.scale(0.0f, 0.0f, 0.0f).glow(100, 180, 255).interpolation(20, i / 2);
                spawnedEntities.add(ha.entity());
                shardA.add(ha);
                scaleBlock(ha.entity(), 0.25f, 0.25f, 0.25f, 20);

                Location locB = loc.clone().add(0.3, 0, 0);
                BlockDisplayHandle hb = displayBuilder.spawnBlock(locB, Material.BLUE_ICE);
                hb.scale(0.0f, 0.0f, 0.0f).glow(120, 200, 255).interpolation(20, i / 2);
                spawnedEntities.add(hb.entity());
                shardB.add(hb);
                scaleBlock(hb.entity(), 0.15f, 0.15f, 0.15f, 20);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.0f, 0.8f);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, 5, 0), 80, 4, 3, 4, 0.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Shared-center orbit: all shards rotate around Y axis at small rate
            orbit += 0.03f;

            if (tick > 25 && tick % 2 == 0 && fallStartTick < 0) {
                // Update positions with orbital rotation
                for (int i = 0; i < 30; i++) {
                    double[] off = shardOffsets[i];
                    double r = Math.sqrt(off[0] * off[0] + off[2] * off[2]);
                    double a = Math.atan2(off[2], off[0]) + orbit;
                    float px = (float) (Math.cos(a) * r);
                    float pz = (float) (Math.sin(a) * r);
                    float py = (float) off[1] + (float) Math.sin(tick * 0.1 + i * 0.5) * 0.2f;
                    translateBlock(shardA.get(i).entity(), px, py, pz, 2);
                    translateBlock(shardB.get(i).entity(), px + 0.3f, py, pz, 2);
                }
            }

            // Periodic fall: every 30t, pick 5 random shards, animate them down to ground
            if (tick > 30 && tick % 30 == 0 && tick < 215) {
                for (int s = 0; s < 5; s++) {
                    int idx = (int) (Math.random() * 30);
                    fallingShardIdx[s] = idx;
                    // Translate down to Y=0 over 15t
                    double[] off = shardOffsets[idx];
                    double r = Math.sqrt(off[0] * off[0] + off[2] * off[2]);
                    double a = Math.atan2(off[2], off[0]) + orbit;
                    float px = (float) (Math.cos(a) * r);
                    float pz = (float) (Math.sin(a) * r);
                    BlockDisplay ea = shardA.get(idx).entity();
                    BlockDisplay eb = shardB.get(idx).entity();
                    Transformation ta = ea.getTransformation();
                    Transformation tb = eb.getTransformation();
                    setFullTransform(ea,
                            new Vector3f(px - 0.5f, 0.5f - 0.5f, pz - 0.5f),
                            new AxisAngle4f((float) Math.PI, 1f, 0f, 0f),
                            ta.getScale(), 15);
                    setFullTransform(eb,
                            new Vector3f(px + 0.3f - 0.5f, 0.5f - 0.5f, pz - 0.5f),
                            new AxisAngle4f((float) Math.PI, 1f, 0f, 0f),
                            tb.getScale(), 15);
                    // Trail + impact particles
                    Location fallLoc = c.clone().add(px, 0.5, pz);
                    DisplayBuilder.playSound(fallLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.4f);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        triggerImpactDamage(fallLoc);
                        if (fallLoc.getWorld() != null) {
                            fallLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, fallLoc, 30, 1, 0.3, 1, 0.2);
                            fallLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, fallLoc,
                                    20, 0.5, 0.3, 0.5, 0.3);
                        }
                    }, 15L);
                }
            }
            // Regen shards 25t after fall (return to orbit position)
            if (tick > 55 && (tick - 25) % 30 == 25 && tick < 240) {
                // This branch may run before fall happens if we're still in initial phase. Guard.
                // We just keep them down — orbit re-applies in the orbit-update loop next tick.
                // Practical: orbit loop will pull them back up.
            }

            // Particles for nebula glow
            if (tick % 2 == 0) {
                int idx = (int) (Math.random() * 30);
                double[] off = shardOffsets[idx];
                double r = Math.sqrt(off[0] * off[0] + off[2] * off[2]);
                double a = Math.atan2(off[2], off[0]) + orbit;
                Location p = c.clone().add(Math.cos(a) * r, off[1], Math.sin(a) * r);
                c.getWorld().spawnParticle(Particle.GLOW, p, 3, 0.2, 0.2, 0.2, 0.3);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, p, 2, 0.3, 0.3, 0.3, 0.05);
            }

            // Random chimes
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f,
                        0.6f + (float) Math.random() * 1.2f);
            }

            // Converge + burst at end (last 22t)
            if (tick == 223) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 1.0f);
                // Pull all toward center
                for (int i = 0; i < 30; i++) {
                    BlockDisplay ea = shardA.get(i).entity();
                    BlockDisplay eb = shardB.get(i).entity();
                    Transformation ta = ea.getTransformation();
                    Transformation tb = eb.getTransformation();
                    setFullTransform(ea,
                            new Vector3f(-0.5f, 5.0f - 0.5f, -0.5f),
                            new AxisAngle4f().set(ta.getLeftRotation()),
                            ta.getScale(), 12);
                    setFullTransform(eb,
                            new Vector3f(-0.5f, 5.0f - 0.5f, -0.5f),
                            new AxisAngle4f().set(tb.getLeftRotation()),
                            tb.getScale(), 12);
                }
            }
            if (tick == 235) {
                // Burst outward
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 1.2f);
                c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER,
                        c.clone().add(0, 5, 0), 3, 1, 1, 1, 0);
                for (int i = 0; i < 30; i++) {
                    double a = (2.0 * Math.PI * i) / 30;
                    float ox = (float) (Math.cos(a) * 9.0);
                    float oz = (float) (Math.sin(a) * 9.0);
                    BlockDisplayHandle ha = shardA.get(i);
                    BlockDisplayHandle hb = shardB.get(i);
                    ha.animateTo(new Vector3f(ox - 0.5f, 5.0f - 0.5f, oz - 0.5f),
                            new AxisAngle4f((float) Math.PI, 1f, 0f, 1f),
                            new Vector3f(0.0f, 0.0f, 0.0f), 10);
                    hb.animateTo(new Vector3f(ox - 0.5f, 5.0f - 0.5f, oz - 0.5f),
                            new AxisAngle4f((float) Math.PI, 1f, 0f, 1f),
                            new Vector3f(0.0f, 0.0f, 0.0f), 10);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenNebula(plugin); }
    }
}
