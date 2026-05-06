package com.blockforge.chaoscraft.modes.fluffy.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluffy Mode — ABSTRACT / MYTHIC CUTE BLOCK DISPLAY ATTACKS (set 5, attacks 41-50)
 * 10 cute-but-deadly BlockDisplay attacks with 25-50 block displays each.
 * All animations via Transformation (AxisAngle4f rotation, Vector3f scale/translation).
 * Never teleport for rotation — use setInterpolationDuration for smooth animation.
 *
 * Fluffy palette:
 * - Cute pink: RGB(255, 120, 180)
 * - Soft cream: RGB(255, 230, 200)
 * - Sweet sky-blue: RGB(150, 220, 255)
 * - Pastel gold: RGB(255, 215, 130)
 * - Plush purple: RGB(200, 140, 230)
 *
 * Materials: PINK_CONCRETE, PINK_WOOL, CHERRY_LOG, OAK_PLANKS, RED_CONCRETE,
 *            WHITE_CONCRETE, LIGHT_BLUE_CONCRETE, GREEN_CONCRETE, GOLD_BLOCK,
 *            AMETHYST_BLOCK, GLASS, *_STAINED_GLASS, PINK_TERRACOTTA, PURPLE_WOOL
 *
 * Particles: HEART, ENCHANT, CRIT, SNOWFLAKE, DUST (pink, gold, white, purple)
 * Sounds: BLOCK_NOTE_BLOCK_BASEDRUM, ENTITY_BAT_AMBIENT, ENTITY_FOX_AMBIENT,
 *         ENTITY_WITCH_CELEBRATE, BLOCK_NOTE_BLOCK_CHIME, BLOCK_GLASS_BREAK,
 *         ENTITY_SLIME_SQUISH_SMALL, BLOCK_AMETHYST_BLOCK_CHIME,
 *         ENTITY_FIREWORK_ROCKET_BLAST, ENTITY_GENERIC_BIG_FALL, ENTITY_GHAST_AMBIENT
 *
 * Attacks (41-50):
 * 41. GiantPlushHeart        — 36 blocks, pulsing heart with HEART particles, constant damage
 * 42. FluffyTotem            — 36 blocks, 6-tier rotating totem with per-tier glow phases
 * 43. GiantBowRibbon         — 28 blocks, expanding/contracting loops with drifting ribbons
 * 44. StarMobiles            — 50 blocks, 5 hanging stars rotating + descending slowly
 * 45. CandyCaneForest        — 48 blocks, swaying candy cane cluster, dominoes on dissipate
 * 46. SpiralMilkshake        — 33 blocks, counter-rotating swirl + overflow impact burst
 * 47. GiantKaleidoscope      — 38 blocks, rainbow stained-glass rings rotating opposite
 * 48. FluffyNova             — 42 blocks, central orb with pulsing spike arms (impact tips)
 * 49. GiantSnowGlobe         — 42 blocks, glass dome falls and shatters (impact only)
 * 50. PlushHydra             — 41 blocks, 3 swaying necks with player-tracking lunge impacts
 */
public final class FluffyBlockDisplay5 {
    private FluffyBlockDisplay5() {}

    private static final String MODE_PATH = "modes/fluffy/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GiantPlushHeart(plugin));
        registry.register(new FluffyTotem(plugin));
        registry.register(new GiantBowRibbon(plugin));
        registry.register(new StarMobiles(plugin));
        registry.register(new CandyCaneForest(plugin));
        registry.register(new SpiralMilkshake(plugin));
        registry.register(new GiantKaleidoscope(plugin));
        registry.register(new FluffyNova(plugin));
        registry.register(new GiantSnowGlobe(plugin));
        registry.register(new PlushHydra(plugin));
    }

    // ================================================================
    // Helper: find nearest non-exempt survival player within range
    // ================================================================
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

    // ================================================================
    // #41 — GIANT PLUSH HEART
    // 36 blocks: 24 pink concrete in two round lobes, 8 V-bottom point,
    // 4 connecting bridge. Pulses scale 1.0→1.12→1.0 cycle. HEART particles,
    // pink dust spirals. Heartbeat BASEDRUM. Constant 10 hearts radius 3.5,
    // 12-tick interval, 15-tick delay.
    // ================================================================
    public static class GiantPlushHeart extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float pulsePhase = 0f;

        public GiantPlushHeart(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_plush_heart", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(150.0); // 10 hearts
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(280);
            config.setCooldownTicks(120);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Left lobe: 12 blocks in a ring around (-1.4, 1.5, 0)
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = -1.4 + Math.cos(angle) * 1.0;
                double py = 1.5 + Math.sin(angle) * 1.0;
                Location loc = center.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 80, 120).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Right lobe: 12 blocks in a ring around (+1.4, 1.5, 0)
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = 1.4 + Math.cos(angle) * 1.0;
                double py = 1.5 + Math.sin(angle) * 1.0;
                Location loc = center.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 80, 120).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // V-bottom point: 8 tapered blocks descending in a V from lobes to point
            double[][] vOffsets = {
                    {-2.0, 0.4, 0}, {-1.4, -0.3, 0}, {-0.9, -1.0, 0}, {-0.4, -1.5, 0},
                    {2.0, 0.4, 0}, {1.4, -0.3, 0}, {0.9, -1.0, 0}, {0.4, -1.5, 0}
            };
            for (double[] o : vOffsets) {
                Location loc = center.clone().add(o[0], o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 80, 120).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Bridge: 4 blocks connecting the two lobes across the top center
            for (int i = 0; i < 4; i++) {
                double px = -0.6 + i * 0.4;
                Location loc = center.clone().add(px, 1.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                h.scale(0.45f, 0.45f, 0.45f).glow(255, 80, 120).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
            }

            // Inflate-on-spawn: animate from 0.5 to full scale over 10 ticks
            for (BlockDisplayHandle h : allBlocks) {
                BlockDisplay entity = h.entity();
                Transformation t = entity.getTransformation();
                Vector3f startScale = new Vector3f(t.getScale()).mul(0.5f);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        startScale,
                        new AxisAngle4f().set(t.getRightRotation())
                ));
                entity.setInterpolationDuration(10);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f().set(t.getLeftRotation()),
                        t.getScale(),
                        new AxisAngle4f().set(t.getRightRotation())
                ));
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.2f, 0.6f);
            w.spawnParticle(Particle.HEART, center.clone().add(0, 1.5, 0), 12, 1.5, 1.5, 1.5, 0.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pulsing scale via Transformation (1.0 → 1.12 → 1.0, 20-tick period)
            pulsePhase += (float) (Math.PI * 2.0 / 20.0); // 20-tick cycle
            if (tick % 5 == 0) {
                float pulse = 1.0f + 0.06f + 0.06f * (float) Math.sin(pulsePhase);
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    // Each block has a different baseline scale; multiply by pulse
                    float bsx = 0.5f * pulse;
                    float bsy = 0.5f * pulse;
                    float bsz = 0.5f * pulse;
                    entity.setInterpolationDuration(10);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(bsx, bsy, bsz),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // HEART particles from heart surface
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.HEART, c.clone().add(0, 1.5, 0), 4, 2.0, 1.5, 1.0, 0.0);
            }

            // Pink dust spiraling outward
            if (tick % 2 == 0) {
                double a = tick * 0.18;
                double r = 2.0 + 0.4 * Math.sin(tick * 0.1);
                Location pLoc = c.clone().add(Math.cos(a) * r, 1.5 + 0.3 * Math.sin(a * 1.3), Math.sin(a) * r);
                DisplayBuilder.dustParticles(pLoc, 2, 0.1, 255, 120, 180, 1.2f);
            }

            // Heartbeat BASEDRUM every 10 ticks
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.9f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantPlushHeart(plugin); }
    }

    // ================================================================
    // #42 — FLUFFY TOTEM
    // 36 blocks: 6 tiers, each tier 4 frame + 2 feature blocks. Materials
    // alternate per tier (oak/cherry log + concrete features). Slowly
    // rotates Y. Tier eyes glow with offset phases. Constant 9 hearts at
    // base radius 2.5, 15-tick interval, 20-tick delay.
    // ================================================================
    public static class FluffyTotem extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> tiers = new ArrayList<>();
        private float yRotation = 0f;
        private float wigglePhase = 0f;

        public FluffyTotem(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fluffy_totem", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(135.0); // 9 hearts
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(360);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Each tier from bottom (owl) to top (bunny ears)
            Material[] frameMats = {
                    Material.OAK_LOG, Material.CHERRY_LOG, Material.OAK_LOG,
                    Material.CHERRY_LOG, Material.OAK_LOG, Material.CHERRY_LOG
            };
            Material[] featureMats = {
                    Material.YELLOW_CONCRETE,  // owl eyes
                    Material.RED_CONCRETE,     // fox diamond eyes
                    Material.BROWN_CONCRETE,   // dog floppy ears
                    Material.LIGHT_GRAY_CONCRETE, // bear rounds
                    Material.PINK_CONCRETE,    // cat triangles
                    Material.WHITE_CONCRETE    // bunny ears (top)
            };
            int[] tierGlow = {255, 200, 130, 220, 255, 255};

            for (int t = 0; t < 6; t++) {
                List<BlockDisplayHandle> tier = new ArrayList<>();
                double y = t * 1.0;
                // Frame (4 blocks at corners around tier center)
                double[][] frameOff = {{-0.45, 0, -0.45}, {0.45, 0, -0.45}, {0.45, 0, 0.45}, {-0.45, 0, 0.45}};
                for (double[] off : frameOff) {
                    Location loc = center.clone().add(off[0], y, off[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, frameMats[t]);
                    h.scale(0.55f, 1.0f, 0.55f).glow(180, 130, 90).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                    tier.add(h);
                }
                // 2 feature blocks (eyes/decoration)
                Location l1 = center.clone().add(-0.25, y + 0.55, 0);
                Location l2 = center.clone().add(0.25, y + 0.55, 0);
                BlockDisplayHandle f1 = displayBuilder.spawnBlock(l1, featureMats[t]);
                f1.scale(0.18f, 0.18f, 0.18f).glow(tierGlow[t], 200, 120).interpolation(5, 0);
                spawnedEntities.add(f1.entity());
                tier.add(f1);
                BlockDisplayHandle f2 = displayBuilder.spawnBlock(l2, featureMats[t]);
                f2.scale(0.18f, 0.18f, 0.18f).glow(tierGlow[t], 200, 120).interpolation(5, 0);
                spawnedEntities.add(f2.entity());
                tier.add(f2);

                // Stagger reveal: bottom tiers spawn first by translating from below
                final int delay = (5 - t) * 5;
                for (BlockDisplayHandle h : tier) {
                    BlockDisplay entity = h.entity();
                    Transformation tr = entity.getTransformation();
                    // Start displaced below ground
                    entity.setTransformation(new Transformation(
                            new Vector3f(tr.getTranslation().x, tr.getTranslation().y - 6f, tr.getTranslation().z),
                            new AxisAngle4f().set(tr.getLeftRotation()),
                            tr.getScale(),
                            new AxisAngle4f().set(tr.getRightRotation())
                    ));
                    entity.setInterpolationDuration(15);
                    entity.setInterpolationDelay(delay);
                    entity.setTransformation(new Transformation(
                            tr.getTranslation(),
                            new AxisAngle4f().set(tr.getLeftRotation()),
                            tr.getScale(),
                            new AxisAngle4f().set(tr.getRightRotation())
                    ));
                }
                tiers.add(tier);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_FOX_AMBIENT, 0.6f, 0.8f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BAT_AMBIENT, 0.5f, 1.2f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 3, 0), 25, 1, 3, 1, 0.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow Y rotation 0.015 rad/tick across all blocks
            yRotation += 0.015f;
            if (tick % 3 == 0) {
                for (List<BlockDisplayHandle> tier : tiers) {
                    for (BlockDisplayHandle h : tier) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(yRotation, 0f, 1f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // Per-tier eye glow phases (offset between tiers)
            if (tick % 8 == 0) {
                for (int ti = 0; ti < tiers.size(); ti++) {
                    double phase = tick * 0.1 + ti * 1.0;
                    int intensity = (int) (140 + 115 * (Math.sin(phase) * 0.5 + 0.5));
                    List<BlockDisplayHandle> tier = tiers.get(ti);
                    // Last 2 entries are eye/feature blocks
                    if (tier.size() >= 6) {
                        tier.get(4).glow(intensity, 220, 130);
                        tier.get(5).glow(intensity, 220, 130);
                    }
                }
            }

            // Top bunny tier wiggle (occasional X scale pulse)
            wigglePhase += 0.05f;
            if (tick % 6 == 0 && tiers.size() == 6) {
                List<BlockDisplayHandle> top = tiers.get(5);
                float wiggle = 1.0f + 0.08f * (float) Math.sin(wigglePhase);
                for (BlockDisplayHandle h : top) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    Vector3f s = t.getScale();
                    entity.setInterpolationDuration(6);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(s.x * wiggle, s.y, s.z),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // ENCHANT ring per tier
            if (tick % 10 == 0) {
                for (int ti = 0; ti < 6; ti++) {
                    Location ringLoc = c.clone().add(0, ti * 1.0 + 0.5, 0);
                    DisplayBuilder.particleRing(ringLoc, 0.85, Particle.ENCHANT, 12, null);
                }
            }

            // Sound layering
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_BAT_AMBIENT, 0.4f, 1.4f);
            }
            if (tick % 80 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_FOX_AMBIENT, 0.4f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FluffyTotem(plugin); }
    }

    // ================================================================
    // #43 — GIANT BOW RIBBON
    // 28 blocks: 16 RED_CONCRETE in two oval loops, 4 dark red knot,
    // 8 trailing ribbon blocks (thin scaled). Loops alternately expand/
    // contract. Constant 10 hearts in loop interiors radius 2.5,
    // 12-tick interval, 10-tick delay.
    // ================================================================
    public static class GiantBowRibbon extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftLoop = new ArrayList<>();
        private final List<BlockDisplayHandle> rightLoop = new ArrayList<>();
        private final List<BlockDisplayHandle> ribbons = new ArrayList<>();
        private float loopPhase = 0f;

        public GiantBowRibbon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_bow_ribbon", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(150.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(300);
            config.setCooldownTicks(120);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Left loop: 8 blocks oval ring at center -1.6,2,0
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double px = -1.6 + Math.cos(angle) * 1.0;
                double py = 2.0 + Math.sin(angle) * 0.8;
                Location loc = center.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                h.scale(0.45f, 0.45f, 0.45f).glow(255, 80, 120).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                leftLoop.add(h);
            }

            // Right loop: 8 blocks oval ring at center +1.6,2,0
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double px = 1.6 + Math.cos(angle) * 1.0;
                double py = 2.0 + Math.sin(angle) * 0.8;
                Location loc = center.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                h.scale(0.45f, 0.45f, 0.45f).glow(255, 80, 120).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                rightLoop.add(h);
            }

            // Center knot: 4 dark red blocks
            for (int i = 0; i < 4; i++) {
                double px = -0.2 + (i % 2) * 0.4;
                double py = 1.8 + (i / 2) * 0.4;
                Location loc = center.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_NETHER_BRICKS);
                h.scale(0.35f, 0.35f, 0.35f).glow(160, 20, 40).interpolation(8, 0);
                spawnedEntities.add(h.entity());
            }

            // 2 trailing ribbons: 4 blocks each, thin scaled
            for (int side = 0; side < 2; side++) {
                double xDir = (side == 0) ? -1.0 : 1.0;
                for (int i = 0; i < 4; i++) {
                    double px = xDir * 0.4 + xDir * i * 0.5;
                    double py = 1.5 - i * 0.4;
                    Location loc = center.clone().add(px, py, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                    h.scale(0.6f, 0.18f, 0.12f).glow(255, 80, 120).interpolation(8, 0);
                    spawnedEntities.add(h.entity());
                    ribbons.add(h);
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITCH_CELEBRATE, 1.0f, 1.4f);
            w.spawnParticle(Particle.CRIT, center.clone().add(0, 2, 0), 30, 2, 1, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Alternating loop expansion/contraction (25-tick period)
            loopPhase += (float) (Math.PI * 2.0 / 25.0);
            if (tick % 4 == 0) {
                float leftScale = 1.0f + 0.1f + 0.1f * (float) Math.sin(loopPhase);
                float rightScale = 1.0f + 0.1f + 0.1f * (float) Math.sin(loopPhase + Math.PI);
                for (BlockDisplayHandle h : leftLoop) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(4);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.45f * leftScale, 0.45f * leftScale, 0.45f * leftScale),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                for (BlockDisplayHandle h : rightLoop) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(4);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.45f * rightScale, 0.45f * rightScale, 0.45f * rightScale),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Ribbon lazy drift via small Z translation oscillation
            if (tick % 6 == 0) {
                for (int i = 0; i < ribbons.size(); i++) {
                    BlockDisplay entity = ribbons.get(i).entity();
                    Transformation t = entity.getTransformation();
                    float zOff = 0.15f * (float) Math.sin(tick * 0.05 + i * 0.4);
                    Vector3f cur = t.getTranslation();
                    entity.setInterpolationDuration(6);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            new Vector3f(cur.x, cur.y, zOff - 0.5f),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // CRIT sparkle from ribbon edges
            if (tick % 4 == 0) {
                for (BlockDisplayHandle r : ribbons) {
                    Location rl = r.entity().getLocation();
                    c.getWorld().spawnParticle(Particle.CRIT, rl, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            // Pink dust from loops
            if (tick % 3 == 0) {
                Location lLoc = c.clone().add(-1.6, 2.0, 0);
                Location rLoc = c.clone().add(1.6, 2.0, 0);
                DisplayBuilder.dustParticles(lLoc, 3, 0.8, 255, 120, 180, 1.0f);
                DisplayBuilder.dustParticles(rLoc, 3, 0.8, 255, 120, 180, 1.0f);
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WITCH_CELEBRATE, 0.7f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantBowRibbon(plugin); }
    }

    // ================================================================
    // #44 — STAR MOBILES
    // 50 blocks: 5 hanging stars (10 GOLD_BLOCK each, 5 arms × 2 blocks).
    // Each star rotates on different axis, bobs at its height. Constant
    // damage at each star radius 1.5, 10 hearts, 10-tick interval.
    // ================================================================
    public static class StarMobiles extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> stars = new ArrayList<>();
        private final double[] starHeights = {2.0, 3.5, 5.0, 6.5, 8.0};
        private final float[] starRotations = new float[5];

        public StarMobiles(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_mobiles", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(150.0);
            config.setDamageRadius(8.0); // covers all 5 star zones in cluster (each is 1.5)
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(320);
            config.setCooldownTicks(130);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            for (int s = 0; s < 5; s++) {
                List<BlockDisplayHandle> star = new ArrayList<>();
                double yPos = starHeights[s];
                // 5 arms, each 2 blocks (inner + outer tip)
                for (int a = 0; a < 5; a++) {
                    double armAngle = (2.0 * Math.PI * a) / 5 + (s * 0.3);
                    for (int b = 0; b < 2; b++) {
                        double r = 0.4 + b * 0.55;
                        double px = Math.cos(armAngle) * r;
                        double pz = Math.sin(armAngle) * r;
                        Location loc = center.clone().add(px, yPos, pz);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                        h.scale(0.4f, 0.4f, 0.9f).glow(255, 215, 130).interpolation(8, 0);
                        // Each arm rotates so its long axis points outward
                        h.rotate((float) armAngle, 0, 1, 0);
                        spawnedEntities.add(h.entity());
                        star.add(h);
                    }
                }

                // Drop animation: start at Y+12, settle to actual height, staggered (lowest first)
                int delay = s * 4;
                for (BlockDisplayHandle h : star) {
                    BlockDisplay entity = h.entity();
                    Transformation tr = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            new Vector3f(tr.getTranslation().x, tr.getTranslation().y + (12.0f - (float) yPos), tr.getTranslation().z),
                            new AxisAngle4f().set(tr.getLeftRotation()),
                            tr.getScale(),
                            new AxisAngle4f().set(tr.getRightRotation())
                    ));
                    entity.setInterpolationDuration(20);
                    entity.setInterpolationDelay(delay);
                    entity.setTransformation(new Transformation(
                            tr.getTranslation(),
                            new AxisAngle4f().set(tr.getLeftRotation()),
                            tr.getScale(),
                            new AxisAngle4f().set(tr.getRightRotation())
                    ));
                }
                stars.add(star);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 5, 0), 40, 2, 4, 2, 0.6);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Each star rotates at different speed and slowly descends 0.01 blocks/tick
            float[] speeds = {0.04f, -0.05f, 0.06f, -0.03f, 0.07f};
            float[] axisX = {0f, 0.3f, 0f, 0.5f, 0f};
            float[] axisY = {1f, 0.7f, 1f, 0.5f, 1f};
            if (tick % 3 == 0) {
                for (int s = 0; s < stars.size(); s++) {
                    starRotations[s] += speeds[s];
                    starHeights[s] -= 0.03; // slow descent
                    double bob = Math.sin(tick * 0.08 + s * 1.0) * 0.2;
                    for (BlockDisplayHandle h : stars.get(s)) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        Vector3f cur = t.getTranslation();
                        // Maintain X/Z, update Y for descent + bob
                        double targetY = starHeights[s] + bob - 0.5;
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                new Vector3f(cur.x, (float) targetY, cur.z),
                                new AxisAngle4f(starRotations[s], axisX[s], axisY[s], 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // Particles per star
            if (tick % 4 == 0) {
                for (int s = 0; s < stars.size(); s++) {
                    Location starLoc = c.clone().add(0, starHeights[s], 0);
                    c.getWorld().spawnParticle(Particle.ENCHANT, starLoc, 4, 0.6, 0.3, 0.6, 0.4);
                    DisplayBuilder.dustParticles(starLoc, 3, 0.5, 255, 215, 130, 1.2f);
                }
            }

            // Tuned chime sequence
            if (tick % 12 == 0) {
                float pitch = 0.8f + (tick % 60) / 100f;
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, pitch);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new StarMobiles(plugin); }
    }

    // ================================================================
    // #45 — CANDY CANE FOREST
    // 48 blocks: 6 candy canes in a cluster, each 8 alternating
    // RED/WHITE_CONCRETE blocks with spiral twist offset on X.
    // Cluster sways gently with ripple. Constant 10 hearts in cluster
    // radius 3.5, 12-tick interval, 15-tick delay.
    // ================================================================
    public static class CandyCaneForest extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> canes = new ArrayList<>();
        private final double[] caneAngles = new double[6];
        private final double[] caneXOffs = new double[6];
        private final double[] caneZOffs = new double[6];

        public CandyCaneForest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("candy_cane_forest", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(150.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(300);
            config.setCooldownTicks(120);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Place 6 canes in a small cluster pattern
            double[][] canePositions = {
                    {-1.5, 0, -1.5}, {1.5, 0, -1.0}, {0.0, 0, 0.0},
                    {-1.0, 0, 1.5}, {1.8, 0, 1.5}, {-2.0, 0, 0.5}
            };
            for (int c = 0; c < 6; c++) {
                List<BlockDisplayHandle> cane = new ArrayList<>();
                caneXOffs[c] = canePositions[c][0];
                caneZOffs[c] = canePositions[c][2];
                caneAngles[c] = c * 0.7;
                for (int b = 0; b < 8; b++) {
                    Material mat = (b % 2 == 0) ? Material.RED_CONCRETE : Material.WHITE_CONCRETE;
                    double xTwist = (b % 2 == 0) ? 0.25 : -0.25;
                    Location loc = center.clone().add(canePositions[c][0] + xTwist, b * 0.85, canePositions[c][2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.5f, 1.0f, 0.5f).glow(255, 100, 130).interpolation(8, 0);
                    spawnedEntities.add(h.entity());
                    cane.add(h);
                }

                // Stagger reveal: rise from ground over 1s, staggered between canes
                final int delay = c * 20;
                for (BlockDisplayHandle h : cane) {
                    BlockDisplay entity = h.entity();
                    Transformation tr = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            new Vector3f(tr.getTranslation().x, tr.getTranslation().y - 7f, tr.getTranslation().z),
                            new AxisAngle4f().set(tr.getLeftRotation()),
                            tr.getScale(),
                            new AxisAngle4f().set(tr.getRightRotation())
                    ));
                    entity.setInterpolationDuration(15);
                    entity.setInterpolationDelay(delay);
                    entity.setTransformation(new Transformation(
                            tr.getTranslation(),
                            new AxisAngle4f().set(tr.getLeftRotation()),
                            tr.getScale(),
                            new AxisAngle4f().set(tr.getRightRotation())
                    ));
                }
                canes.add(cane);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 4, 0), 40, 2, 3, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Each cane sways with slight phase offset (ripple effect)
            if (tick % 4 == 0) {
                for (int ci = 0; ci < canes.size(); ci++) {
                    double phaseOffset = ci * 0.4;
                    float swayAngle = (float) Math.toRadians(5.0 * Math.sin(tick * 0.05 + phaseOffset));
                    for (BlockDisplayHandle h : canes.get(ci)) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(4);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(swayAngle, 0f, 1f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // SNOWFLAKE drift from cane tops
            if (tick % 5 == 0) {
                for (int ci = 0; ci < canes.size(); ci++) {
                    Location top = c.clone().add(caneXOffs[ci], 7.5, caneZOffs[ci]);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, top, 4, 0.4, 0.2, 0.4, 0.02);
                }
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CandyCaneForest(plugin); }
    }

    // ================================================================
    // #46 — SPIRAL MILKSHAKE
    // 33 blocks: cup (rim ring 8 + body 8 + base 4 = 20), whipped cream
    // swirl 6 + cherry 2 + straw 5 = 33. Cup rotates Y, cream counter-
    // rotates. Constant 9 hearts at base radius 2.5, 15-tick interval,
    // 15-tick delay. Overflow at duration end: impact 4r 18 hearts.
    // ================================================================
    public static class SpiralMilkshake extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cupBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> creamBlocks = new ArrayList<>();
        private float cupRotation = 0f;
        private float creamRotation = 0f;
        private boolean overflowDone = false;

        public SpiralMilkshake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spiral_milkshake", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(135.0); // 9 hearts constant
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(280);
            config.setCooldownTicks(130);
            config.setImpactDamage(270.0); // 18 hearts overflow impact
            config.setImpactRadius(4.0);
            // NOTE: not impact-only — both constant + overflow impact
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Rim ring: 8 white concrete at Y+2.5, radius 1.4
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double px = Math.cos(angle) * 1.4;
                double pz = Math.sin(angle) * 1.4;
                Location loc = center.clone().add(px, 2.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(0.45f, 0.3f, 0.45f).glow(255, 230, 200).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                cupBlocks.add(h);
            }

            // Cup body: 8 blocks, graduated scale (rim wide, tapering down)
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8 + Math.toRadians(22.5);
                double y = 1.0 + (i / 8.0) * 1.0; // Stack mid-cup
                double r = 1.2;
                double px = Math.cos(angle) * r;
                double pz = Math.sin(angle) * r;
                Location loc = center.clone().add(px, y, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(0.55f, 0.45f, 0.55f).glow(255, 230, 200).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                cupBlocks.add(h);
            }

            // Base ring: 4 blocks at Y=0.3
            for (int i = 0; i < 4; i++) {
                double angle = (2.0 * Math.PI * i) / 4;
                double px = Math.cos(angle) * 0.9;
                double pz = Math.sin(angle) * 0.9;
                Location loc = center.clone().add(px, 0.3, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(0.5f, 0.3f, 0.5f).glow(220, 200, 180).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                cupBlocks.add(h);
            }

            // Whipped cream swirl: 6 blocks in diminishing spiral above rim
            for (int i = 0; i < 6; i++) {
                double angle = i * 0.9;
                double r = 1.0 - i * 0.13;
                double y = 2.7 + i * 0.25;
                Location loc = center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.5f - i * 0.04f, 0.35f, 0.5f - i * 0.04f).glow(255, 245, 230).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                creamBlocks.add(h);
            }

            // Cherry on top: 2 cherry log blocks
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, 4.4 + i * 0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHERRY_LOG);
                h.scale(0.3f, 0.3f, 0.3f).glow(255, 60, 80).interpolation(8, 0);
                spawnedEntities.add(h.entity());
            }

            // Straw: 5 light blue blocks at angle
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0.7 + i * 0.05, 2.6 + i * 0.45, 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_CONCRETE);
                h.scale(0.12f, 0.5f, 0.12f).glow(150, 220, 255).interpolation(8, 0);
                h.rotate((float) Math.toRadians(15), 0, 0, 1);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9f, 1.3f);
            w.spawnParticle(Particle.HEART, center.clone().add(0, 3, 0), 8, 0.7, 0.5, 0.7, 0.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Cup slowly rotates Y
            cupRotation += 0.025f;
            if (tick % 3 == 0) {
                for (BlockDisplayHandle h : cupBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(cupRotation, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Cream counter-rotates
            creamRotation -= 0.04f;
            if (tick % 3 == 0) {
                for (BlockDisplayHandle h : creamBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(3);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(creamRotation, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // White DUST cream drift
            if (tick % 4 == 0) {
                Location dustLoc = c.clone().add(0, 3.2, 0);
                DisplayBuilder.dustParticles(dustLoc, 4, 0.7, 255, 245, 230, 1.2f);
            }
            // ENCHANT sparkle from cherry
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 4.6, 0), 3, 0.2, 0.2, 0.2, 0.4);
            }

            // Overflow trigger: about 30 ticks before duration end
            if (!overflowDone && tick >= config.getDurationTicks() - 30) {
                overflowDone = true;
                // Cream scale 3x expansion
                for (BlockDisplayHandle h : creamBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    Vector3f s = t.getScale();
                    entity.setInterpolationDuration(8);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(s.x * 3.0f, s.y * 3.0f, s.z * 3.0f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                // Cup tip 90 deg X rotate
                for (BlockDisplayHandle h : cupBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(8);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f((float) Math.toRadians(90), 1f, 0f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                DisplayBuilder.playSound(c, Sound.ENTITY_SLIME_SQUISH_SMALL, 1.4f, 0.7f);
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 2.5, 0), 30, 1.5, 1.0, 1.5, 0.3);
                triggerImpactDamage(c.clone().add(0, 1.5, 0));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new SpiralMilkshake(plugin); }
    }

    // ================================================================
    // #47 — GIANT KALEIDOSCOPE
    // 38 blocks: outer ring 12 + inner ring 8 + 6 segment wedges (3 each
    // = 18). Stained glass colors. Outer rotates +0.04 rad/tick, inner
    // counter -0.06. Glow cycles all 16 dye colors. Constant 10 hearts
    // center radius 2.5, 10-tick interval, 15-tick delay.
    // ================================================================
    public static class GiantKaleidoscope extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private float outerRot = 0f;
        private float innerRot = 0f;
        private int colorIndex = 0;

        public GiantKaleidoscope(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_kaleidoscope", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(150.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(7);
            config.setDurationTicks(320);
            config.setCooldownTicks(130);
        }

        // 16 dye color RGB values cycling palette
        private static final int[][] DYE_COLORS = {
                {255, 255, 255}, {249, 128, 29}, {199, 78, 189}, {58, 179, 218},
                {254, 216, 61}, {128, 199, 31}, {243, 139, 170}, {71, 79, 82},
                {157, 157, 151}, {22, 156, 156}, {137, 50, 184}, {60, 68, 170},
                {131, 84, 50}, {93, 124, 21}, {176, 46, 38}, {29, 29, 33}
        };

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            Material[] glassPalette = {
                    Material.RED_STAINED_GLASS, Material.ORANGE_STAINED_GLASS,
                    Material.YELLOW_STAINED_GLASS, Material.LIME_STAINED_GLASS,
                    Material.LIGHT_BLUE_STAINED_GLASS, Material.PURPLE_STAINED_GLASS,
                    Material.PINK_STAINED_GLASS, Material.MAGENTA_STAINED_GLASS,
                    Material.CYAN_STAINED_GLASS, Material.GREEN_STAINED_GLASS,
                    Material.BLUE_STAINED_GLASS, Material.WHITE_STAINED_GLASS
            };

            // Outer ring: 12 blocks at radius 2.5, Y+2
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = Math.cos(angle) * 2.5;
                double pz = Math.sin(angle) * 2.5;
                Location loc = center.clone().add(px, 2.0, pz);
                Material mat = glassPalette[i % glassPalette.length];
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 255, 255).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                outerRing.add(h);
            }

            // Inner ring: 8 blocks at radius 1.4, Y+2
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double px = Math.cos(angle) * 1.4;
                double pz = Math.sin(angle) * 1.4;
                Location loc = center.clone().add(px, 2.0, pz);
                Material mat = glassPalette[(i + 3) % glassPalette.length];
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.45f, 0.45f, 0.45f).glow(255, 255, 255).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                innerRing.add(h);
            }

            // 6 wedges, 3 blocks each, between rings
            for (int s = 0; s < 6; s++) {
                double baseAngle = (2.0 * Math.PI * s) / 6;
                for (int b = 0; b < 3; b++) {
                    double r = 1.6 + b * 0.3;
                    double px = Math.cos(baseAngle) * r;
                    double pz = Math.sin(baseAngle) * r;
                    Location loc = center.clone().add(px, 2.0, pz);
                    Material mat = glassPalette[(s + b) % glassPalette.length];
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.3f, 0.3f, 0.3f).glow(255, 255, 255).interpolation(8, 0);
                    h.rotate((float) baseAngle, 0, 1, 0);
                    spawnedEntities.add(h.entity());
                    segments.add(h);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 2, 0), 35, 2, 1, 2, 0.6);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Outer + segments rotate +0.04 rad/tick
            outerRot += 0.04f;
            innerRot -= 0.06f;
            if (tick % 2 == 0) {
                for (BlockDisplayHandle h : outerRing) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(2);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(outerRot, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                for (BlockDisplayHandle h : segments) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(2);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(outerRot, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                for (BlockDisplayHandle h : innerRing) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(2);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(innerRot, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Glow cycles through 16 dye colors every 5 ticks
            if (tick % 5 == 0) {
                colorIndex = (colorIndex + 1) % DYE_COLORS.length;
                int[] outerColor = DYE_COLORS[colorIndex];
                int[] innerColor = DYE_COLORS[(colorIndex + 4) % DYE_COLORS.length];
                for (BlockDisplayHandle h : outerRing) {
                    h.glow(outerColor[0], outerColor[1], outerColor[2]);
                }
                for (BlockDisplayHandle h : innerRing) {
                    h.glow(innerColor[0], innerColor[1], innerColor[2]);
                }
                for (BlockDisplayHandle h : segments) {
                    h.glow(outerColor[0], outerColor[1], outerColor[2]);
                }
            }

            // ENCHANT + random color DUST every 2 ticks
            if (tick % 2 == 0) {
                int[] col = DYE_COLORS[colorIndex];
                Location pl = c.clone().add(
                        (Math.random() - 0.5) * 5.0,
                        2.0 + (Math.random() - 0.5) * 0.5,
                        (Math.random() - 0.5) * 5.0);
                DisplayBuilder.dustParticles(pl, 2, 0.2, col[0], col[1], col[2], 1.2f);
            }
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 2, 0), 5, 2, 1, 2, 0.4);
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantKaleidoscope(plugin); }
    }

    // ================================================================
    // #48 — FLUFFY NOVA
    // 42 blocks: 6 main spike arms (4 each = 24), 6 inter-arm spikes
    // (2 each = 12), central orb 6 amethyst blocks. Spikes pulse in/out.
    // Constant 12 hearts at orb radius 2.5, 10-tick interval. Spike tips
    // deal impact 0.8r 10 hearts on extension.
    // ================================================================
    public static class FluffyNova extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> orb = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> mainSpikes = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> interSpikes = new ArrayList<>();
        private final double[] mainAngles = new double[6];
        private final double[] interAngles = new double[6];
        private float pulsePhase = 0f;
        private boolean wasExtending = false;

        public FluffyNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fluffy_nova", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(180.0); // 12 hearts orb
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(5);
            config.setDurationTicks(280);
            config.setCooldownTicks(130);
            config.setImpactDamage(150.0); // 10 hearts spike tip
            config.setImpactRadius(0.8);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Central orb: 6 amethyst blocks in small sphere at center
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 6; i++) {
                double y = 1.0 - (2.0 * i / 5.0);
                double radiusAtY = Math.sqrt(Math.max(0, 1 - y * y));
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * radiusAtY * 0.5;
                double pz = Math.sin(theta) * radiusAtY * 0.5;
                Location loc = center.clone().add(px, y * 0.5 + 2.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 140, 230).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                orb.add(h);
            }

            // 6 main spikes (PINK_CONCRETE) pointing outward
            for (int s = 0; s < 6; s++) {
                List<BlockDisplayHandle> arm = new ArrayList<>();
                double angle = (2.0 * Math.PI * s) / 6;
                mainAngles[s] = angle;
                for (int b = 0; b < 4; b++) {
                    double r = 0.6 + b * 0.55;
                    double px = Math.cos(angle) * r;
                    double pz = Math.sin(angle) * r;
                    float scale = 0.8f - b * 0.18f;
                    Location loc = center.clone().add(px, 2.5, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_CONCRETE);
                    h.scale(scale, scale, scale).glow(255, 120, 180).interpolation(6, 0);
                    spawnedEntities.add(h.entity());
                    arm.add(h);
                }
                mainSpikes.add(arm);
            }

            // 6 inter-arm spikes (LIGHT_BLUE_CONCRETE) at +30 deg offset
            for (int s = 0; s < 6; s++) {
                List<BlockDisplayHandle> arm = new ArrayList<>();
                double angle = (2.0 * Math.PI * s) / 6 + Math.PI / 6;
                interAngles[s] = angle;
                for (int b = 0; b < 2; b++) {
                    double r = 0.5 + b * 0.5;
                    double px = Math.cos(angle) * r;
                    double pz = Math.sin(angle) * r;
                    float scale = 0.6f - b * 0.2f;
                    Location loc = center.clone().add(px, 2.5, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_CONCRETE);
                    h.scale(scale, scale, scale).glow(150, 220, 255).interpolation(6, 0);
                    spawnedEntities.add(h.entity());
                    arm.add(h);
                }
                interSpikes.add(arm);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.8f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 2.5, 0), 40, 1.5, 1.5, 1.5, 0.5);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spike pulse (15-tick period) — translate distance from center
            pulsePhase += (float) (Math.PI * 2.0 / 15.0);
            float push = 0.5f * (float) Math.sin(pulsePhase); // -0.5 to +0.5
            boolean extending = Math.cos(pulsePhase) < 0; // sin going up
            if (tick % 3 == 0) {
                for (int s = 0; s < mainSpikes.size(); s++) {
                    double angle = mainAngles[s];
                    List<BlockDisplayHandle> arm = mainSpikes.get(s);
                    for (int b = 0; b < arm.size(); b++) {
                        BlockDisplay entity = arm.get(b).entity();
                        Transformation t = entity.getTransformation();
                        Vector3f cur = t.getTranslation();
                        float tx = (float) (Math.cos(angle) * push) - 0.5f;
                        float tz = (float) (Math.sin(angle) * push) - 0.5f;
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                new Vector3f(tx, cur.y, tz),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
                for (int s = 0; s < interSpikes.size(); s++) {
                    double angle = interAngles[s];
                    List<BlockDisplayHandle> arm = interSpikes.get(s);
                    for (int b = 0; b < arm.size(); b++) {
                        BlockDisplay entity = arm.get(b).entity();
                        Transformation t = entity.getTransformation();
                        Vector3f cur = t.getTranslation();
                        float tx = (float) (Math.cos(angle) * push) - 0.5f;
                        float tz = (float) (Math.sin(angle) * push) - 0.5f;
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                new Vector3f(tx, cur.y, tz),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Orb glow intensity pulse
            if (tick % 4 == 0) {
                int g = (int) (140 + 60 * Math.sin(pulsePhase));
                int b2 = (int) (230 - 30 * Math.sin(pulsePhase));
                for (BlockDisplayHandle h : orb) {
                    h.glow(200, Math.max(80, Math.min(255, g)), Math.max(150, Math.min(255, b2)));
                }
            }

            // Spike tips deal impact damage on extension transition
            if (extending && !wasExtending) {
                for (int s = 0; s < mainSpikes.size(); s++) {
                    double angle = mainAngles[s];
                    double tipR = 1.65 + push;
                    Location tipLoc = c.clone().add(Math.cos(angle) * tipR, 2.5, Math.sin(angle) * tipR);
                    triggerImpactDamage(tipLoc);
                }
            }
            wasExtending = extending;

            // Purple DUST outward from spikes
            if (tick % 3 == 0) {
                for (int s = 0; s < 6; s++) {
                    double angle = mainAngles[s];
                    double r = 1.6 + push;
                    Location pl = c.clone().add(Math.cos(angle) * r, 2.5, Math.sin(angle) * r);
                    DisplayBuilder.dustParticles(pl, 3, 0.3, 200, 140, 230, 1.2f);
                }
            }
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 2.5, 0), 4, 0.5, 0.5, 0.5, 0.4);
            }

            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FluffyNova(plugin); }
    }

    // ================================================================
    // #49 — GIANT SNOW GLOBE
    // 42 blocks: 20 GLASS hemisphere dome, 12 WHITE_CONCRETE base ring,
    // mini house 6 pink terracotta, mini tree 4 green concrete. Drops
    // from Y+18 over 30 ticks. Impact-only at tick 30: 32 hearts radius
    // 5.5. Glass shatters outward.
    // ================================================================
    public static class GiantSnowGlobe extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> domeBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> miniBlocks = new ArrayList<>();
        private boolean impacted = false;
        private float miniRotation = 0f;

        public GiantSnowGlobe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_snow_globe", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(480.0); // 32 hearts
            config.setImpactRadius(5.5);
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Hemisphere dome: 20 glass blocks arranged in upper half-sphere
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 20; i++) {
                double y = (double) i / 19.0; // 0..1, upper hemisphere only
                double radiusAtY = Math.sqrt(Math.max(0, 1 - y * y));
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * radiusAtY * 3.0;
                double pz = Math.sin(theta) * radiusAtY * 3.0;
                double py = y * 3.0 + 0.5;
                Location loc = center.clone().add(px, py + 18.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GLASS);
                h.scale(0.7f, 0.7f, 0.7f).glow(220, 240, 255).interpolation(25, 0);
                spawnedEntities.add(h.entity());
                domeBlocks.add(h);

                // Animate to ground position over 30 ticks
                BlockDisplay entity = h.entity();
                Transformation tr = entity.getTransformation();
                entity.setInterpolationDuration(30);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        new Vector3f(tr.getTranslation().x, tr.getTranslation().y - 18f, tr.getTranslation().z),
                        new AxisAngle4f().set(tr.getLeftRotation()),
                        tr.getScale(),
                        new AxisAngle4f().set(tr.getRightRotation())
                ));
            }

            // Base ring: 12 white concrete blocks, drop with the dome
            for (int i = 0; i < 12; i++) {
                double angle = (2.0 * Math.PI * i) / 12;
                double px = Math.cos(angle) * 3.0;
                double pz = Math.sin(angle) * 3.0;
                Location loc = center.clone().add(px, 18.1, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(0.7f, 0.4f, 0.7f).glow(255, 230, 200).interpolation(25, 0);
                spawnedEntities.add(h.entity());
                baseBlocks.add(h);

                BlockDisplay entity = h.entity();
                Transformation tr = entity.getTransformation();
                entity.setInterpolationDuration(30);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        new Vector3f(tr.getTranslation().x, tr.getTranslation().y - 18f, tr.getTranslation().z),
                        new AxisAngle4f().set(tr.getLeftRotation()),
                        tr.getScale(),
                        new AxisAngle4f().set(tr.getRightRotation())
                ));
            }

            // Mini house: 6 pink terracotta blocks
            double[][] houseOffs = {
                    {-0.4, 18.5, -0.4}, {0.4, 18.5, -0.4}, {-0.4, 18.5, 0.4},
                    {0.4, 18.5, 0.4}, {0.0, 19.1, -0.4}, {0.0, 19.1, 0.4}
            };
            for (double[] off : houseOffs) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PINK_TERRACOTTA);
                h.scale(0.4f, 0.4f, 0.4f).glow(255, 180, 200).interpolation(25, 0);
                spawnedEntities.add(h.entity());
                miniBlocks.add(h);

                BlockDisplay entity = h.entity();
                Transformation tr = entity.getTransformation();
                entity.setInterpolationDuration(30);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        new Vector3f(tr.getTranslation().x, tr.getTranslation().y - 18f, tr.getTranslation().z),
                        new AxisAngle4f().set(tr.getLeftRotation()),
                        tr.getScale(),
                        new AxisAngle4f().set(tr.getRightRotation())
                ));
            }

            // Mini tree: 4 green concrete blocks
            for (int i = 0; i < 4; i++) {
                double y = 18.6 + i * 0.35;
                Location loc = center.clone().add(1.0, y, 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GREEN_CONCRETE);
                float scale = 0.45f - i * 0.06f;
                h.scale(scale, scale, scale).glow(80, 200, 100).interpolation(25, 0);
                spawnedEntities.add(h.entity());
                miniBlocks.add(h);

                BlockDisplay entity = h.entity();
                Transformation tr = entity.getTransformation();
                entity.setInterpolationDuration(30);
                entity.setInterpolationDelay(0);
                entity.setTransformation(new Transformation(
                        new Vector3f(tr.getTranslation().x, tr.getTranslation().y - 18f, tr.getTranslation().z),
                        new AxisAngle4f().set(tr.getLeftRotation()),
                        tr.getScale(),
                        new AxisAngle4f().set(tr.getRightRotation())
                ));
            }

            // Shadow ring grows on ground while falling
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_BIG_FALL, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // SNOWFLAKE particles inside globe each tick (dispersed, only after touching down)
            if (tick > 25 && tick % 2 == 0) {
                Location inside = c.clone().add(
                        (Math.random() - 0.5) * 4.0,
                        0.5 + Math.random() * 2.5,
                        (Math.random() - 0.5) * 4.0);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, inside, 1, 0.1, 0.1, 0.1, 0.0);
            }

            // Shadow DUST ring grows under landing zone (ticks 1-29)
            if (tick < 30 && tick % 2 == 0) {
                double ringR = 0.5 + (tick / 30.0) * 5.0;
                DisplayBuilder.particleRing(c.clone().add(0, 0.1, 0), ringR, Particle.DUST,
                        16, new Particle.DustOptions(Color.fromRGB(80, 80, 80), 1.5f));
            }

            // Inner house/tree slowly rotate Y once landed
            if (tick > 30 && tick % 4 == 0) {
                miniRotation += 0.02f;
                for (BlockDisplayHandle h : miniBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(4);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(miniRotation, 0f, 1f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
            }

            // Impact at tick 30
            if (!impacted && tick >= 30) {
                impacted = true;
                triggerImpactDamage(c.clone());
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_BIG_FALL, 1.6f, 0.4f);
                for (int i = 0; i < 5; i++) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.7f + i * 0.1f);
                }
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1, 0), 80, 3, 2, 3, 0.3);
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 1, 0), 60, 3, 2, 3, 0.4);

                // Glass dome blocks shatter outward radially
                for (BlockDisplayHandle h : domeBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    Vector3f cur = t.getTranslation();
                    // Radial outward direction in XZ
                    double mag = Math.sqrt(cur.x * cur.x + cur.z * cur.z);
                    if (mag < 0.001) mag = 1.0;
                    float dx = (float) (cur.x / mag) * 4.0f;
                    float dz = (float) (cur.z / mag) * 4.0f;
                    entity.setInterpolationDuration(8);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            new Vector3f(cur.x + dx, cur.y + 1.5f, cur.z + dz),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.1f, 0.1f, 0.1f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                // Base slides apart
                for (BlockDisplayHandle h : baseBlocks) {
                    BlockDisplay entity = h.entity();
                    Transformation t = entity.getTransformation();
                    Vector3f cur = t.getTranslation();
                    double mag = Math.sqrt(cur.x * cur.x + cur.z * cur.z);
                    if (mag < 0.001) mag = 1.0;
                    float dx = (float) (cur.x / mag) * 2.0f;
                    float dz = (float) (cur.z / mag) * 2.0f;
                    entity.setInterpolationDuration(8);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            new Vector3f(cur.x + dx, cur.y, cur.z + dz),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GiantSnowGlobe(plugin); }
    }

    // ================================================================
    // #50 — PLUSH HYDRA
    // 41 blocks: 8 PURPLE_WOOL base mound, 3 necks (5 each = 15) in
    // different colors, 3 heads (6 each = 18). Necks sway with different
    // sin frequencies. Every 30 ticks one head lunges at player. Constant
    // 11 hearts radius 1.5 per head zone. Lunge impact 2.0r, 20 hearts.
    // ================================================================
    public static class PlushHydra extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> base = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> necks = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> heads = new ArrayList<>();
        private final double[] neckFreqs = {0.08, 0.11, 0.06};
        private final double[] neckBaseAngles = {0, 2 * Math.PI / 3, 4 * Math.PI / 3};
        private final Material[] neckMats = {Material.PURPLE_WOOL, Material.GREEN_WOOL, Material.PINK_WOOL};
        private int lungeIndex = 0;
        private int lungeStartTick = -1;
        private double[] lungeOffset = new double[3];
        // Cached spawn-time lunge direction.
        private double cachedDirX = 0, cachedDirZ = -1;

        public PlushHydra(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plush_hydra", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(165.0); // 11 hearts constant per head zone
            config.setDamageRadius(6.0); // covers 3 head zones (each 1.5) + base
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(10);
            config.setDurationTicks(360);
            config.setCooldownTicks(150);
            config.setImpactDamage(300.0); // 20 hearts lunge
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Base mound: 8 purple wool blocks, scaled hemisphere
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 8; i++) {
                double y = (double) i / 7.0;
                double radiusAtY = Math.sqrt(Math.max(0, 1 - y * y));
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * radiusAtY * 1.6;
                double pz = Math.sin(theta) * radiusAtY * 1.6;
                Location loc = center.clone().add(px, y * 1.0 + 0.2, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_WOOL);
                h.scale(0.7f, 0.6f, 0.7f).glow(160, 80, 200).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                base.add(h);
            }

            // 3 necks, each 5 blocks in curved column
            for (int n = 0; n < 3; n++) {
                List<BlockDisplayHandle> neck = new ArrayList<>();
                double baseAngle = neckBaseAngles[n];
                double anchorX = Math.cos(baseAngle) * 0.8;
                double anchorZ = Math.sin(baseAngle) * 0.8;
                for (int b = 0; b < 5; b++) {
                    // Curve outward as it goes up
                    double curve = (b / 4.0) * 1.0;
                    double px = anchorX + Math.cos(baseAngle) * curve;
                    double pz = anchorZ + Math.sin(baseAngle) * curve;
                    double py = 1.0 + b * 0.7;
                    Location loc = center.clone().add(px, py, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, neckMats[n]);
                    h.scale(0.45f, 0.6f, 0.45f).glow(220, 140, 200).interpolation(6, 0);
                    spawnedEntities.add(h.entity());
                    neck.add(h);
                }

                // Stagger reveal: necks extend up over time
                int delay = n * 5;
                for (int b = 0; b < neck.size(); b++) {
                    BlockDisplay entity = neck.get(b).entity();
                    Transformation tr = entity.getTransformation();
                    entity.setTransformation(new Transformation(
                            new Vector3f(tr.getTranslation().x, tr.getTranslation().y - 4f, tr.getTranslation().z),
                            new AxisAngle4f().set(tr.getLeftRotation()),
                            tr.getScale(),
                            new AxisAngle4f().set(tr.getRightRotation())
                    ));
                    entity.setInterpolationDuration(15);
                    entity.setInterpolationDelay(delay + b * 2);
                    entity.setTransformation(new Transformation(
                            tr.getTranslation(),
                            new AxisAngle4f().set(tr.getLeftRotation()),
                            tr.getScale(),
                            new AxisAngle4f().set(tr.getRightRotation())
                    ));
                }
                necks.add(neck);
            }

            // 3 heads, each 6 blocks (4 round head + 2 jaw)
            for (int n = 0; n < 3; n++) {
                List<BlockDisplayHandle> head = new ArrayList<>();
                double baseAngle = neckBaseAngles[n];
                double cx = Math.cos(baseAngle) * 1.8;
                double cz = Math.sin(baseAngle) * 1.8;
                double cy = 4.6;

                // 4 round head blocks
                double[][] headOffs = {{-0.25, 0, -0.25}, {0.25, 0, -0.25}, {0.25, 0, 0.25}, {-0.25, 0, 0.25}};
                for (double[] off : headOffs) {
                    Location loc = center.clone().add(cx + off[0], cy + off[1], cz + off[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, neckMats[n]);
                    h.scale(0.4f, 0.4f, 0.4f).glow(255, 180, 220).interpolation(6, 0);
                    spawnedEntities.add(h.entity());
                    head.add(h);
                }
                // 2 jaw blocks
                for (int j = 0; j < 2; j++) {
                    double off = (j == 0) ? -0.2 : 0.2;
                    Location loc = center.clone().add(cx + off, cy - 0.3, cz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                    h.scale(0.18f, 0.18f, 0.4f).glow(255, 255, 255).interpolation(6, 0);
                    spawnedEntities.add(h.entity());
                    head.add(h);
                }
                heads.add(head);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GHAST_AMBIENT, 1.2f, 1.8f);
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 2.5, 0), 30, 1.5, 2, 1.5, 0.5);

            // Cache spawn-time lunge direction (all heads lunge along this fixed dir).
            Player initialTarget = findNearestPlayer(center, 16.0);
            if (initialTarget != null) {
                double dxs = initialTarget.getLocation().getX() - center.getX();
                double dzs = initialTarget.getLocation().getZ() - center.getZ();
                double mag = Math.sqrt(dxs * dxs + dzs * dzs);
                if (mag > 0.001) {
                    cachedDirX = dxs / mag;
                    cachedDirZ = dzs / mag;
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Each neck sways at its own frequency
            if (tick % 3 == 0) {
                for (int n = 0; n < necks.size(); n++) {
                    double freq = neckFreqs[n];
                    double swayAngle = Math.sin(tick * freq) * 0.4;
                    double baseAngle = neckBaseAngles[n];
                    double anchorX = Math.cos(baseAngle) * 0.8;
                    double anchorZ = Math.sin(baseAngle) * 0.8;
                    List<BlockDisplayHandle> neck = necks.get(n);
                    for (int b = 0; b < neck.size(); b++) {
                        double curveBase = (b / 4.0) * 1.0;
                        double curveSway = curveBase + swayAngle * (b / 4.0);
                        double px = anchorX + Math.cos(baseAngle) * curveSway;
                        double pz = anchorZ + Math.sin(baseAngle) * curveSway;
                        BlockDisplay entity = neck.get(b).entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(3);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                new Vector3f((float) px - 0.5f, t.getTranslation().y, (float) pz - 0.5f),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Heads rotate independently
            if (tick % 4 == 0) {
                for (int n = 0; n < heads.size(); n++) {
                    float headRot = (float) (Math.sin(tick * 0.07 + n) * 0.3);
                    for (BlockDisplayHandle h : heads.get(n)) {
                        BlockDisplay entity = h.entity();
                        Transformation t = entity.getTransformation();
                        entity.setInterpolationDuration(4);
                        entity.setInterpolationDelay(0);
                        entity.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(headRot, 0f, 1f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }

            // Lunge cycle every 30 ticks — uses cached spawn-time direction.
            if (tick > 0 && tick % 30 == 0) {
                lungeIndex = (lungeIndex + 1) % 3;
                lungeStartTick = tick;
                double dx = cachedDirX, dz = cachedDirZ;
                lungeOffset[0] = dx;
                lungeOffset[1] = 0;
                lungeOffset[2] = dz;

                // Animate head + connected neck top forward over 4 ticks
                List<BlockDisplayHandle> head = heads.get(lungeIndex);
                double baseAngle = neckBaseAngles[lungeIndex];
                double cx = Math.cos(baseAngle) * 1.8 + dx * 2.5;
                double cz = Math.sin(baseAngle) * 1.8 + dz * 2.5;
                double cy = 4.6;
                double[][] headOffs = {{-0.25, 0, -0.25}, {0.25, 0, -0.25}, {0.25, 0, 0.25}, {-0.25, 0, 0.25}};
                for (int i = 0; i < 4; i++) {
                    BlockDisplay entity = head.get(i).entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(4);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            new Vector3f((float) (cx + headOffs[i][0] - 0.5),
                                    (float) (cy - 0.5),
                                    (float) (cz + headOffs[i][2] - 0.5)),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                // Jaw blocks lunge too
                for (int j = 0; j < 2; j++) {
                    double off = (j == 0) ? -0.2 : 0.2;
                    BlockDisplay entity = head.get(4 + j).entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(4);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            new Vector3f((float) (cx + off - 0.5),
                                    (float) (cy - 0.8),
                                    (float) (cz - 0.5)),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }

                DisplayBuilder.playSound(c, Sound.ENTITY_GHAST_AMBIENT, 1.0f, 2.0f);
                Location lungeLoc = c.clone().add(Math.cos(baseAngle) * 1.8 + dx * 2.5, 4.6, Math.sin(baseAngle) * 1.8 + dz * 2.5);
                c.getWorld().spawnParticle(Particle.CRIT, lungeLoc, 25, 0.6, 0.6, 0.6, 0.4);
                triggerImpactDamage(lungeLoc);
            }

            // Retract lunged head 5 ticks after lunge
            if (lungeStartTick >= 0 && tick == lungeStartTick + 5) {
                List<BlockDisplayHandle> head = heads.get(lungeIndex);
                double baseAngle = neckBaseAngles[lungeIndex];
                double cx = Math.cos(baseAngle) * 1.8;
                double cz = Math.sin(baseAngle) * 1.8;
                double cy = 4.6;
                double[][] headOffs = {{-0.25, 0, -0.25}, {0.25, 0, -0.25}, {0.25, 0, 0.25}, {-0.25, 0, 0.25}};
                for (int i = 0; i < 4; i++) {
                    BlockDisplay entity = head.get(i).entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(4);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            new Vector3f((float) (cx + headOffs[i][0] - 0.5),
                                    (float) (cy - 0.5),
                                    (float) (cz + headOffs[i][2] - 0.5)),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                for (int j = 0; j < 2; j++) {
                    double off = (j == 0) ? -0.2 : 0.2;
                    BlockDisplay entity = head.get(4 + j).entity();
                    Transformation t = entity.getTransformation();
                    entity.setInterpolationDuration(4);
                    entity.setInterpolationDelay(0);
                    entity.setTransformation(new Transformation(
                            new Vector3f((float) (cx + off - 0.5),
                                    (float) (cy - 0.8),
                                    (float) (cz - 0.5)),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // ENCHANT from each head + colored DUST per neck
            if (tick % 5 == 0) {
                int[][] neckColors = {{160, 80, 200}, {80, 200, 100}, {255, 160, 200}};
                for (int n = 0; n < 3; n++) {
                    double baseAngle = neckBaseAngles[n];
                    Location headLoc = c.clone().add(Math.cos(baseAngle) * 1.8, 4.8, Math.sin(baseAngle) * 1.8);
                    c.getWorld().spawnParticle(Particle.ENCHANT, headLoc, 3, 0.3, 0.3, 0.3, 0.3);
                    Location neckLoc = c.clone().add(Math.cos(baseAngle) * 1.3, 2.5, Math.sin(baseAngle) * 1.3);
                    DisplayBuilder.dustParticles(neckLoc, 3, 0.4,
                            neckColors[n][0], neckColors[n][1], neckColors[n][2], 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PlushHydra(plugin); }
    }
}
