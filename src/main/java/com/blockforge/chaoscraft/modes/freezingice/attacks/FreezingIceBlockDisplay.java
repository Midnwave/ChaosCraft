package com.blockforge.chaoscraft.modes.freezingice.attacks;

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
 * FreezingIce Mode — BLOCK DISPLAY ATTACKS 1-10 (Frozen Creatures I)
 *
 * Frozen-themed boss structures (creature-shaped block display sculptures)
 * that animate via Transformation (AxisAngle4f rotation, Vector3f scale/
 * translation). 30+ blocks minimum per attack. Each uses ice palette
 * materials and the standard FreezingIce particle/sound toolkit.
 *
 * Ice palette materials: PACKED_ICE, BLUE_ICE, ICE, SNOW_BLOCK,
 *                        TINTED_GLASS, BLUE_STAINED_GLASS, QUARTZ_BLOCK,
 *                        SMOOTH_QUARTZ, DIAMOND_BLOCK, AMETHYST_BLOCK,
 *                        CALCITE, LIGHT_BLUE_CONCRETE, WHITE_WOOL,
 *                        POWDER_SNOW, OBSIDIAN, WHITE_CONCRETE
 *
 * Particles: SNOWFLAKE, GLOW, ELECTRIC_SPARK, CLOUD, FALLING_DUST,
 *            ITEM_SNOWBALL, SCULK_SOUL, CRIT, EXPLOSION_EMITTER, ENCHANT
 * Sounds: ITEM_TRIDENT_RIPTIDE, ENTITY_GLOW_SQUID_AMBIENT, BLOCK_GLASS_BREAK,
 *         BLOCK_AMETHYST_BLOCK_CHIME, ITEM_ARMOR_EQUIP_ICE, BLOCK_GLASS_HIT,
 *         BLOCK_NOTE_BLOCK_CHIME
 *
 * Attacks:
 *  1. GlacialWyrmKing          — 39 blocks, serpentine dragon, undulating, constant
 *  2. FrostPhoenixAscendant    — 35 blocks, spread-wing bird, flapping, constant
 *  3. IronboundIceGolem        — 31 blocks, bipedal humanoid, slams, constant+impact
 *  4. PermafrostMammothCharge  — 37 blocks, mammoth profile, charging, constant
 *  5. YetiAbominable           — 39 blocks, hunched yeti, pounds ground, impact-only
 *  6. IceBasiliskGaze          — 33 blocks, coiled crowned serpent, constant
 *  7. GlacierTitanRising       — 38 blocks, colossal mountain humanoid, impact-only
 *  8. FrostReaperHarbinger     — 38 blocks, hooded reaper w/ scythe, constant+impact
 *  9. IceValkyrieDescent       — 41 blocks, winged warrior, spear stab, impact-only
 * 10. FrostFairyFlight         — 36 blocks, 6 orbiting fairies hex ring, constant
 *
 * No follow-AI in this file (boss-anchored creatures — too large to chase).
 */
public final class FreezingIceBlockDisplay {
    private FreezingIceBlockDisplay() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GlacialWyrmKing(plugin));
        registry.register(new FrostPhoenixAscendant(plugin));
        registry.register(new IronboundIceGolem(plugin));
        registry.register(new PermafrostMammothCharge(plugin));
        registry.register(new YetiAbominable(plugin));
        registry.register(new IceBasiliskGaze(plugin));
        registry.register(new GlacierTitanRising(plugin));
        registry.register(new FrostReaperHarbinger(plugin));
        registry.register(new IceValkyrieDescent(plugin));
        registry.register(new FrostFairyFlight(plugin));
    }

    // ================================================================
    // Helper: find nearest non-exempt survival player within range
    // ================================================================
    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
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
    // #1 — GLACIAL WYRM KING
    // 39 blocks: 1 horned skull-head + 6 jaw fangs + 18 spine segments
    // (S-curve) + 8 rib spikes + 4 horn pairs + 2 glowing eyes.
    // Constant 9.5r, 6h/12t, 35t delay.
    // ================================================================
    public static class GlacialWyrmKing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spine = new ArrayList<>();
        private final List<BlockDisplayHandle> fangs = new ArrayList<>();
        private final List<BlockDisplayHandle> ribs = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final List<BlockDisplayHandle> headAndHorns = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float jawAngle = 0f;

        public GlacialWyrmKing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_wyrm_king", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1440.0);
            config.setDamageRadius(14.25);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(220);
            config.setCooldownTicks(60);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Skull head: 1 PACKED_ICE block at Y+8, scale 1.6
            BlockDisplayHandle skull = displayBuilder.spawnBlock(center.clone().add(0, 8, 0), Material.PACKED_ICE);
            skull.scale(0.0f, 0.0f, 0.0f).glow(200, 230, 255).interpolation(30, 0);
            spawnedEntities.add(skull.entity());
            headAndHorns.add(skull);
            allBlocks.add(skull);
            growBlock(skull, 1.6f, 30);

            // Jaw fangs: 6 QUARTZ_BLOCK blocks, scale 0.25, in two rows below the skull
            for (int i = 0; i < 6; i++) {
                double px = -0.7 + (i % 3) * 0.7;
                double py = 7.4 + (i / 3) * 0.25;
                Location loc = center.clone().add(px, py, 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.25f, 0.4f, 0.25f).glow(255, 255, 255).interpolation(20, 5);
                spawnedEntities.add(h.entity());
                fangs.add(h);
                allBlocks.add(h);
            }

            // Spine: 18 BLUE_ICE blocks forming S-curve along X axis at Y+8, then trailing back
            for (int i = 0; i < 18; i++) {
                double progress = i / 17.0;
                double xOff = -1.2 - i * 0.8; // trails back from head
                double zOff = Math.sin(progress * Math.PI * 1.5) * 1.6; // S-curve
                double yOff = 8.0 + Math.cos(progress * Math.PI * 1.5) * 0.4; // gentle undulation
                Location loc = center.clone().add(xOff, yOff, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                float taper = 1.4f - (float) progress * 0.8f; // 1.4 -> 0.6
                h.scale(taper, taper, taper).glow(140, 200, 255).interpolation(2 + i, 30 + i * 2);
                spawnedEntities.add(h.entity());
                spine.add(h);
                allBlocks.add(h);
            }

            // Rib spikes: 8 TINTED_GLASS jutting out, 0.4 x 1.8 x 0.4
            for (int i = 0; i < 8; i++) {
                double xOff = -2.0 - i * 1.5;
                double yOff = 8.5 + (i % 2 == 0 ? 1.4 : -1.4);
                Location loc = center.clone().add(xOff, yOff, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.4f, 1.8f, 0.4f).glow(80, 100, 160).interpolation(15, 20);
                spawnedEntities.add(h.entity());
                ribs.add(h);
                allBlocks.add(h);
            }

            // Horn pairs: 4 DIAMOND_BLOCK on the skull (two pairs)
            double[][] hornOffsets = {{-0.4, 8.9, 0.0}, {0.4, 8.9, 0.0}, {-0.55, 9.4, -0.2}, {0.55, 9.4, -0.2}};
            for (double[] off : hornOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.3f, 0.6f, 0.3f).glow(170, 230, 255).interpolation(20, 8);
                spawnedEntities.add(h.entity());
                headAndHorns.add(h);
                allBlocks.add(h);
            }

            // Eyes: 2 AMETHYST_BLOCK glowing
            for (int i = 0; i < 2; i++) {
                double px = (i == 0) ? -0.35 : 0.35;
                Location loc = center.clone().add(px, 8.2, 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.2f, 0.2f, 0.2f).glow(180, 130, 255).interpolation(15, 10);
                spawnedEntities.add(h.entity());
                eyes.add(h);
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.6f, 0.5f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 8, 0), 50, 3, 1, 3, 0.05);
        }

        private void growBlock(BlockDisplayHandle h, float targetScale, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(targetScale, targetScale, targetScale),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Body undulation: Y sine wave on spine, period 40t, amplitude 0.6
            if (tick > 30 && tick % 4 == 0) {
                for (int i = 0; i < spine.size(); i++) {
                    BlockDisplayHandle h = spine.get(i);
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    float yOff = (float) (Math.sin((tick + i * 4) * 0.157) * 0.6);
                    e.setInterpolationDuration(4);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, yOff - 0.5f, -0.5f),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Jaw scissor every 30t
            if (tick > 40 && tick % 30 == 0) {
                jawAngle = (jawAngle > 0f) ? 0f : (float) Math.toRadians(25);
                for (int i = 0; i < fangs.size(); i++) {
                    BlockDisplayHandle h = fangs.get(i);
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    float sign = (i < 3) ? 1f : -1f;
                    e.setInterpolationDuration(8);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(sign * jawAngle, 1f, 0f, 0f),
                            t.getScale(),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                }
                // CLOUD breath puff at jaw
                c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 7.5, 1.8), 15, 0.5, 0.3, 0.5, 0.05);
            }

            // Eye glow scale pulse 0.15 ↔ 0.25
            if (tick % 8 == 0) {
                float pulse = 0.15f + 0.05f * (1f + (float) Math.sin(tick * 0.2));
                for (BlockDisplayHandle h : eyes) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(8);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(pulse, pulse, pulse),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                // ELECTRIC_SPARK from eyes
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 8.2, 0.7), 5, 0.3, 0.2, 0.3, 0.05);
            }

            // SNOWFLAKE trail along spine
            if (tick % 3 == 0) {
                for (int i = 0; i < spine.size(); i += 3) {
                    Location sp = spine.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, sp, 1, 0.2, 0.2, 0.2, 0.01);
                }
            }

            // Ambient glow squid sound every 40t
            if (tick > 0 && tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.2f, 0.5f);
            }

            // Dissipate: body collapses joint-by-joint scale->0 over last 25t
            if (tick >= 195) {
                int dissTick = tick - 195;
                // Shrink one segment per tick from tail-end forward
                int shrinkIdx = spine.size() - 1 - dissTick;
                if (shrinkIdx >= 0 && shrinkIdx < spine.size()) {
                    shrinkToZero(spine.get(shrinkIdx), 5);
                }
                if (dissTick == 20) {
                    for (BlockDisplayHandle h : ribs) shrinkToZero(h, 5);
                    for (BlockDisplayHandle h : fangs) shrinkToZero(h, 5);
                    for (BlockDisplayHandle h : eyes) shrinkToZero(h, 5);
                }
                if (dissTick == 24) {
                    for (BlockDisplayHandle h : headAndHorns) shrinkToZero(h, 4);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.7f);
                }
            }
        }

        private void shrinkToZero(BlockDisplayHandle h, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GlacialWyrmKing(plugin); }
    }

    // ================================================================
    // #2 — FROST PHOENIX ASCENDANT
    // 35 blocks: 6 body BLUE_ICE + 18 wing feathers BLUE_STAINED_GLASS
    // + 1 head PACKED_ICE + 2 CALCITE beak shards + 8 tail ICE plumes.
    // Constant 8.0r, 5.5h/14t, 25t delay.
    // ================================================================
    public static class FrostPhoenixAscendant extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> leftWing = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWing = new ArrayList<>();
        private final List<BlockDisplayHandle> tail = new ArrayList<>();
        private final List<BlockDisplayHandle> headBeak = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float wingPhase = 0f;

        public FrostPhoenixAscendant(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_phoenix_ascendant", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1320.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(240);
            config.setCooldownTicks(60);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Body: 6 BLUE_ICE egg cluster, scale 0.9, at Y+5
            double[][] bodyOffsets = {
                    {0, 5.0, 0}, {-0.5, 5.0, 0}, {0.5, 5.0, 0},
                    {0, 5.5, -0.3}, {-0.3, 5.5, 0.3}, {0.3, 5.5, 0.3}
            };
            for (double[] off : bodyOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(140, 200, 255).interpolation(20, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
                allBlocks.add(h);
                growBlock(h, 0.9f, 25);
            }

            // Left wing: 9 BLUE_STAINED_GLASS feathers, fanned
            for (int i = 0; i < 9; i++) {
                double along = -0.6 - i * 0.45;
                double yOff = 5.0 + Math.sin(i * 0.3) * 0.3;
                Location loc = center.clone().add(along, yOff, -0.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 180, 240).interpolation(20, 5);
                spawnedEntities.add(h.entity());
                leftWing.add(h);
                allBlocks.add(h);
                growFeather(h, 0.4f, 0.15f, 1.2f, 22);
            }

            // Right wing: 9 BLUE_STAINED_GLASS feathers mirror
            for (int i = 0; i < 9; i++) {
                double along = 0.6 + i * 0.45;
                double yOff = 5.0 + Math.sin(i * 0.3) * 0.3;
                Location loc = center.clone().add(along, yOff, -0.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(120, 180, 240).interpolation(20, 5);
                spawnedEntities.add(h.entity());
                rightWing.add(h);
                allBlocks.add(h);
                growFeather(h, 0.4f, 0.15f, 1.2f, 22);
            }

            // Head: 1 PACKED_ICE
            BlockDisplayHandle head = displayBuilder.spawnBlock(center.clone().add(0, 5.8, 0.7), Material.PACKED_ICE);
            head.scale(0.0f, 0.0f, 0.0f).glow(200, 230, 255).interpolation(20, 10);
            spawnedEntities.add(head.entity());
            headBeak.add(head);
            allBlocks.add(head);
            growBlock(head, 0.7f, 22);

            // Beak: 2 CALCITE shards
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, 5.7 - i * 0.15, 1.1 + i * 0.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.0f, 0.0f, 0.0f).glow(240, 240, 230).interpolation(18, 12);
                spawnedEntities.add(h.entity());
                headBeak.add(h);
                allBlocks.add(h);
                growBlock(h, 0.2f, 18);
            }

            // Tail: 8 ICE plumes trailing back, tapering
            for (int i = 0; i < 8; i++) {
                double along = -0.3 - i * 0.5;
                double yOff = 5.0 - i * 0.05;
                Location loc = center.clone().add(0, yOff, along);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(170, 220, 255).interpolation(18, 8);
                spawnedEntities.add(h.entity());
                tail.add(h);
                allBlocks.add(h);
                float taper = 0.5f - i * 0.04f;
                growBlock(h, Math.max(0.15f, taper), 18);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.2f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 5, 0), 40, 2, 1, 2, 0.05);
        }

        private void growBlock(BlockDisplayHandle h, float targetScale, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(targetScale, targetScale, targetScale),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        private void growFeather(BlockDisplayHandle h, float sx, float sy, float sz, int duration) {
            BlockDisplay e = h.entity();
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

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Wing flap: ±45 deg Z-axis rotation, 30t cycle
            if (tick > 30 && tick % 3 == 0) {
                wingPhase += 0.21f;
                float flapAngle = (float) (Math.sin(wingPhase) * Math.toRadians(45));
                for (BlockDisplayHandle h : leftWing) {
                    rotateAroundZ(h, -flapAngle);
                }
                for (BlockDisplayHandle h : rightWing) {
                    rotateAroundZ(h, flapAngle);
                }
            }

            // Body hover Y±0.4 sine
            if (tick > 30 && tick % 4 == 0) {
                float hover = (float) (Math.sin(tick * 0.1) * 0.4);
                for (BlockDisplayHandle h : body) hover(h, hover);
                for (BlockDisplayHandle h : headBeak) hover(h, hover);
                for (BlockDisplayHandle h : tail) hover(h, hover);
            }

            // Snowflake feathers shedding from wings
            if (tick % 4 == 0) {
                for (BlockDisplayHandle h : leftWing) {
                    if (Math.random() < 0.2) {
                        c.getWorld().spawnParticle(Particle.SNOWFLAKE, h.entity().getLocation(), 2, 0.1, 0.1, 0.1, 0.02);
                    }
                }
                for (BlockDisplayHandle h : rightWing) {
                    if (Math.random() < 0.2) {
                        c.getWorld().spawnParticle(Particle.SNOWFLAKE, h.entity().getLocation(), 2, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            }

            // GLOW around body
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 5.2, 0), 4, 0.6, 0.6, 0.6, 0.02);
            }

            // Sound on flap (every 30t)
            if (tick > 30 && tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.9f, 1.4f);
                c.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, c.clone().add(0, 5, 0), 12, 1, 0.5, 1, 0.1);
            }

            // Dissipate: ticks 210-235, wings fold inward + shards rain
            if (tick >= 210) {
                int dt = tick - 210;
                if (dt == 0) {
                    for (BlockDisplayHandle h : leftWing) rotateAroundZ(h, (float) Math.toRadians(-90));
                    for (BlockDisplayHandle h : rightWing) rotateAroundZ(h, (float) Math.toRadians(90));
                }
                if (dt == 10) {
                    for (BlockDisplayHandle h : allBlocks) shrinkToZero(h, 15);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 1.0f);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 5, 0), 40, 2, 2, 2, 0.1);
                }
            }
        }

        private void rotateAroundZ(BlockDisplayHandle h, float angle) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(3);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, 0f, 0f, 1f),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        }

        private void hover(BlockDisplayHandle h, float yOffset) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(4);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(-0.5f, yOffset - 0.5f, -0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        private void shrinkToZero(BlockDisplayHandle h, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostPhoenixAscendant(plugin); }
    }

    // ================================================================
    // #3 — IRONBOUND ICE GOLEM
    // 31 blocks: 6 torso BLUE_ICE + 1 head PACKED_ICE + 2 CALCITE eyes
    // + 8 arm BLUE_ICE + 8 leg BLUE_ICE + 6 spike TINTED_GLASS.
    // Constant 7.0r + impact slam 10.0r 8.0h, 30t delay.
    // ================================================================
    public static class IronboundIceGolem extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> torso = new ArrayList<>();
        private final List<BlockDisplayHandle> leftArm = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArm = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>();
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private final List<BlockDisplayHandle> headEyes = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private boolean armRaised = false;

        public IronboundIceGolem(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ironbound_ice_golem", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1200.0);
            config.setDamageRadius(10.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            // Impact slam on arm strikes
            config.setDamageOnImpactOnly(false);
            config.setImpactDamage(1920.0);
            config.setImpactRadius(15.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(60);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Torso: 6 BLUE_ICE stacked, scale 1.4x2.0x1.0
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, 1.0 + i * 0.9, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(140, 200, 255).interpolation(20, 0);
                spawnedEntities.add(h.entity());
                torso.add(h);
                allBlocks.add(h);
                growXYZ(h, 1.4f, 1.0f, 1.0f, 20);
            }

            // Head: 1 PACKED_ICE, scale 1.2
            BlockDisplayHandle head = displayBuilder.spawnBlock(center.clone().add(0, 7.0, 0), Material.PACKED_ICE);
            head.scale(0.0f, 0.0f, 0.0f).glow(200, 230, 255).interpolation(20, 5);
            spawnedEntities.add(head.entity());
            headEyes.add(head);
            allBlocks.add(head);
            growBlock(head, 1.2f, 22);

            // Eyes: 2 CALCITE
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add((i == 0 ? -0.4 : 0.4), 7.1, 0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.0f, 0.0f, 0.0f).glow(255, 255, 240).interpolation(18, 10);
                spawnedEntities.add(h.entity());
                headEyes.add(h);
                allBlocks.add(h);
                growBlock(h, 0.2f, 18);
            }

            // Left arm: 4 BLUE_ICE segments, hang down
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(-1.6, 5.5 - s * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(140, 200, 255).interpolation(20, 8);
                spawnedEntities.add(h.entity());
                leftArm.add(h);
                allBlocks.add(h);
                float taper = 0.85f - s * 0.05f;
                growBlock(h, taper, 20);
            }
            // Right arm
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(1.6, 5.5 - s * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(140, 200, 255).interpolation(20, 8);
                spawnedEntities.add(h.entity());
                rightArm.add(h);
                allBlocks.add(h);
                float taper = 0.85f - s * 0.05f;
                growBlock(h, taper, 20);
            }

            // Legs: 2 sets of 4 BLUE_ICE
            for (int side = 0; side < 2; side++) {
                double xOff = (side == 0) ? -0.6 : 0.6;
                for (int s = 0; s < 4; s++) {
                    Location loc = center.clone().add(xOff, 0.6 - s * 0.4, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    h.scale(0.0f, 0.0f, 0.0f).glow(140, 200, 255).interpolation(22, 12);
                    spawnedEntities.add(h.entity());
                    legs.add(h);
                    allBlocks.add(h);
                    growBlock(h, 0.9f, 22);
                }
            }

            // Shoulder spikes: 6 TINTED_GLASS
            double[][] spikeOffsets = {
                    {-1.0, 6.0, -0.6}, {0.0, 6.2, -0.6}, {1.0, 6.0, -0.6},
                    {-0.8, 5.5, -0.8}, {0.8, 5.5, -0.8}, {0.0, 5.8, -0.9}
            };
            for (double[] off : spikeOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(60, 90, 140).interpolation(20, 15);
                spawnedEntities.add(h.entity());
                spikes.add(h);
                allBlocks.add(h);
                growXYZ(h, 0.3f, 1.4f, 0.3f, 22);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.6f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 3, 0), 50, 2, 3, 2, 0.05);
        }

        private void growBlock(BlockDisplayHandle h, float s, int duration) {
            growXYZ(h, s, s, s, duration);
        }

        private void growXYZ(BlockDisplayHandle h, float sx, float sy, float sz, int duration) {
            BlockDisplay e = h.entity();
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

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Constant joint particles
            if (tick % 5 == 0) {
                for (BlockDisplayHandle h : torso) {
                    c.getWorld().spawnParticle(Particle.FALLING_DUST,
                            h.entity().getLocation(), 1, 0.1, 0.1, 0.1,
                            Material.WHITE_CONCRETE.createBlockData());
                }
            }

            // Arms raise overhead at tick 60, slam at tick 90, repeat every 60t after
            int slamCycle = (tick - 60) % 60;
            if (tick >= 60 && slamCycle == 0 && !armRaised) {
                // Raise overhead
                armRaised = true;
                for (BlockDisplayHandle h : leftArm) rotateOnZ(h, (float) Math.toRadians(-130), 10);
                for (BlockDisplayHandle h : rightArm) rotateOnZ(h, (float) Math.toRadians(130), 10);
            }
            if (tick >= 60 && slamCycle == 30) {
                // SLAM down + impact damage
                for (BlockDisplayHandle h : leftArm) rotateOnZ(h, (float) Math.toRadians(-10), 3);
                for (BlockDisplayHandle h : rightArm) rotateOnZ(h, (float) Math.toRadians(10), 3);
                triggerImpactDamage(c.clone());
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.5f);
                c.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, c.clone().add(0, 0.3, 0), 50, 4, 0.3, 4, 0.3);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 60, 4.0, 140, 200, 255, 1.6f);
            }
            if (tick >= 60 && slamCycle == 45) {
                armRaised = false;
            }

            // Slow Y rotation toward player every 60t
            if (tick > 0 && tick % 60 == 0) {
                Player target = findNearestPlayer(c, 30.0);
                if (target != null) {
                    double dx = target.getLocation().getX() - c.getX();
                    double dz = target.getLocation().getZ() - c.getZ();
                    float yaw = (float) Math.atan2(dz, dx);
                    for (BlockDisplayHandle h : torso) rotateOnY(h, yaw, 30);
                    for (BlockDisplayHandle h : headEyes) rotateOnY(h, yaw, 30);
                }
            }

            // Dissipate ticks 195-225 — crumble from feet up
            if (tick >= 195) {
                int dt = tick - 195;
                if (dt < legs.size() && dt % 1 == 0) {
                    shrinkToZero(legs.get(dt), 4);
                }
                if (dt == 10) for (BlockDisplayHandle h : spikes) shrinkToZero(h, 5);
                if (dt == 14) for (BlockDisplayHandle h : leftArm) shrinkToZero(h, 5);
                if (dt == 14) for (BlockDisplayHandle h : rightArm) shrinkToZero(h, 5);
                if (dt == 18) for (BlockDisplayHandle h : torso) shrinkToZero(h, 5);
                if (dt == 22) for (BlockDisplayHandle h : headEyes) shrinkToZero(h, 5);
            }
        }

        private void rotateOnZ(BlockDisplayHandle h, float angle, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, 0f, 0f, 1f),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        }

        private void rotateOnY(BlockDisplayHandle h, float angle, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, 0f, 1f, 0f),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        }

        private void shrinkToZero(BlockDisplayHandle h, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronboundIceGolem(plugin); }
    }

    // ================================================================
    // #4 — PERMAFROST MAMMOTH CHARGE
    // 37 blocks: 10 SNOW_BLOCK torso + 2 PACKED_ICE head + 8 ICE tusks
    // + 12 SNOW_BLOCK legs + 5 PACKED_ICE trunk.
    // Constant 8.0r, 6.5h/10t, 30t delay. Slow forward translation.
    // ================================================================
    public static class PermafrostMammothCharge extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>();

        public PermafrostMammothCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_mammoth_charge", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1560.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(260);
            config.setCooldownTicks(60);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Torso: 10 SNOW_BLOCK, oblong, scale 1.3x1.2x2.0 (use scaleXYZ)
            for (int i = 0; i < 10; i++) {
                double along = -2.0 + i * 0.5;
                Location loc = center.clone().add(0, 2.5, along);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SNOW_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(240, 250, 255).interpolation(15, 5 + i);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
                growXYZ(h, 1.3f, 1.2f, 1.0f, 18);
            }

            // Head: 2 PACKED_ICE at the front (+Z direction)
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, 2.6 + i * 0.4, 3.0 + i * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 230, 255).interpolation(15, 0);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
                growBlock(h, 1.0f, 16);
            }

            // Tusks: 2 sets of 4 ICE blocks, curved outward from head
            for (int side = 0; side < 2; side++) {
                double xSign = (side == 0) ? -1 : 1;
                for (int s = 0; s < 4; s++) {
                    double curveOut = xSign * (0.4 + s * 0.25);
                    double curveZ = 3.5 + s * 0.4;
                    double curveY = 2.3 - s * 0.15;
                    Location loc = center.clone().add(curveOut, curveY, curveZ);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                    h.scale(0.0f, 0.0f, 0.0f).glow(220, 240, 255).interpolation(15, 8);
                    spawnedEntities.add(h.entity());
                    allBlocks.add(h);
                    growXYZ(h, 0.3f, 0.3f, 0.8f, 18);
                }
            }

            // Legs: 4 columns of 3 SNOW_BLOCK segments
            double[][] legBases = {{-0.9, -1.5}, {0.9, -1.5}, {-0.9, 1.5}, {0.9, 1.5}};
            for (double[] base : legBases) {
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(base[0], 0.7 - s * 0.7, base[1]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SNOW_BLOCK);
                    h.scale(0.0f, 0.0f, 0.0f).glow(240, 250, 255).interpolation(15, 4);
                    spawnedEntities.add(h.entity());
                    allBlocks.add(h);
                    legs.add(h);
                    growBlock(h, 0.7f, 16);
                }
            }

            // Trunk: 5 PACKED_ICE tapered curls (forward then down)
            for (int i = 0; i < 5; i++) {
                double along = 3.2 + i * 0.4;
                double yOff = 2.0 - i * 0.3;
                Location loc = center.clone().add(0, yOff, along);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 230, 255).interpolation(15, 10);
                spawnedEntities.add(h.entity());
                allBlocks.add(h);
                float taper = 0.5f - i * 0.07f;
                growBlock(h, Math.max(0.2f, taper), 16);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.6f, 0.4f);
            w.spawnParticle(Particle.CLOUD, center.clone().add(0, 3, 3), 25, 1, 1, 1, 0.05);
        }

        private void growBlock(BlockDisplayHandle h, float s, int duration) {
            growXYZ(h, s, s, s, duration);
        }

        private void growXYZ(BlockDisplayHandle h, float sx, float sy, float sz, int duration) {
            BlockDisplay e = h.entity();
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

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow forward translation +0.05/tick toward nearest player (XZ only)
            if (tick > 30) {
                Player target = findNearestPlayer(c, 40.0);
                if (target != null) {
                    double dx = target.getLocation().getX() - c.getX();
                    double dz = target.getLocation().getZ() - c.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 1.0) {
                        double mvX = (dx / dist) * 0.05;
                        double mvZ = (dz / dist) * 0.05;
                        Location newCenter = c.clone().add(mvX, 0, mvZ);
                        setCenter(newCenter);
                        // Teleport all entities by same delta
                        for (var ent : spawnedEntities) {
                            if (ent != null && ent.isValid()) {
                                try { ent.teleport(ent.getLocation().add(mvX, 0, mvZ)); } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
            }

            // Leg bounce: alternating Y offsets
            if (tick > 30 && tick % 6 == 0) {
                for (int i = 0; i < legs.size(); i++) {
                    BlockDisplayHandle h = legs.get(i);
                    float bounce = (float) (Math.sin((tick + i * 8) * 0.25) * 0.1);
                    bounce(h, bounce);
                }
            }

            // Particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_DUST,
                        c.clone().add(0, 0.2, 0), 4, 1.5, 0.1, 1.5,
                        Material.WHITE_CONCRETE.createBlockData());
                c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 2.0, 3.5), 3, 0.3, 0.3, 0.3, 0.02);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3, 0), 4, 1.5, 1, 1.5, 0.05);
            }

            if (tick > 0 && tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.0f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.6f, 0.4f);
            }

            // Dissipate ticks 235-260
            if (tick >= 235) {
                int dt = tick - 235;
                if (dt == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.6f);
                }
                if (dt < allBlocks.size()) {
                    shrinkToZero(allBlocks.get(dt), 5);
                }
            }
        }

        private void bounce(BlockDisplayHandle h, float yOff) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(6);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(-0.5f, yOff - 0.5f, -0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        private void shrinkToZero(BlockDisplayHandle h, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PermafrostMammothCharge(plugin); }
    }

    // ================================================================
    // #5 — YETI ABOMINABLE
    // 39 blocks: 8 WHITE_WOOL torso + 1 SNOW_BLOCK head + 2 OBSIDIAN eye pits
    // + 6 QUARTZ fangs + 12 WHITE_WOOL arms + 6 WHITE_WOOL legs + 4 POWDER_SNOW tufts.
    // Impact-only 9.0r 8.0h on each pound (every 50t).
    // ================================================================
    public static class YetiAbominable extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> torso = new ArrayList<>();
        private final List<BlockDisplayHandle> headFaceFangs = new ArrayList<>();
        private final List<BlockDisplayHandle> leftArm = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArm = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>();
        private final List<BlockDisplayHandle> tufts = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();

        public YetiAbominable(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("yeti_abominable", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(1920.0);
            config.setImpactRadius(13.5);
            config.setDamage(0.0);
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(220);
            config.setCooldownTicks(60);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Torso: 8 WHITE_WOOL stacked
            for (int i = 0; i < 8; i++) {
                double yOff = 1.5 + (i / 2) * 0.9;
                double xOff = (i % 2 == 0) ? -0.5 : 0.5;
                Location loc = center.clone().add(xOff, yOff, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                h.scale(0.0f, 0.0f, 0.0f).glow(245, 245, 245).interpolation(20, 0);
                spawnedEntities.add(h.entity());
                torso.add(h);
                allBlocks.add(h);
                growBlock(h, 1.5f, 22);
            }

            // Head: 1 SNOW_BLOCK
            BlockDisplayHandle head = displayBuilder.spawnBlock(center.clone().add(0, 5.5, 0), Material.SNOW_BLOCK);
            head.scale(0.0f, 0.0f, 0.0f).glow(245, 250, 255).interpolation(20, 5);
            spawnedEntities.add(head.entity());
            headFaceFangs.add(head);
            allBlocks.add(head);
            growBlock(head, 1.0f, 22);

            // Eye pits: 2 OBSIDIAN
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add((i == 0 ? -0.3 : 0.3), 5.6, 0.55);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.0f, 0.0f, 0.0f).glow(20, 20, 40).interpolation(18, 10);
                spawnedEntities.add(h.entity());
                headFaceFangs.add(h);
                allBlocks.add(h);
                growBlock(h, 0.15f, 18);
            }

            // Mouth fangs: 6 QUARTZ
            for (int i = 0; i < 6; i++) {
                double xOff = -0.5 + (i % 3) * 0.5;
                double yOff = 5.2 - (i / 3) * 0.1;
                Location loc = center.clone().add(xOff, yOff, 0.55);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(255, 255, 255).interpolation(18, 12);
                spawnedEntities.add(h.entity());
                headFaceFangs.add(h);
                allBlocks.add(h);
                growBlock(h, 0.15f, 18);
            }

            // Long arms: 2 sets of 6 segments, scale 1.0 -> 0.6
            for (int side = 0; side < 2; side++) {
                double xSign = (side == 0) ? -1 : 1;
                List<BlockDisplayHandle> arm = (side == 0) ? leftArm : rightArm;
                for (int s = 0; s < 6; s++) {
                    double xOff = xSign * (1.4 + s * 0.2);
                    double yOff = 3.5 - s * 0.45;
                    Location loc = center.clone().add(xOff, yOff, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                    h.scale(0.0f, 0.0f, 0.0f).glow(245, 245, 245).interpolation(20, 8);
                    spawnedEntities.add(h.entity());
                    arm.add(h);
                    allBlocks.add(h);
                    float taper = 1.0f - s * 0.08f;
                    growBlock(h, taper, 22);
                }
            }

            // Short legs: 2 sets of 3
            for (int side = 0; side < 2; side++) {
                double xSign = (side == 0) ? -0.5 : 0.5;
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(xSign, 0.9 - s * 0.4, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_WOOL);
                    h.scale(0.0f, 0.0f, 0.0f).glow(245, 245, 245).interpolation(20, 5);
                    spawnedEntities.add(h.entity());
                    legs.add(h);
                    allBlocks.add(h);
                    growBlock(h, 0.95f, 22);
                }
            }

            // 4 shoulder fur tufts: POWDER_SNOW
            double[][] tuftOffsets = {{-1.2, 4.5, 0}, {1.2, 4.5, 0}, {-1.0, 4.2, -0.5}, {1.0, 4.2, -0.5}};
            for (double[] off : tuftOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POWDER_SNOW);
                h.scale(0.0f, 0.0f, 0.0f).glow(255, 255, 255).interpolation(20, 12);
                spawnedEntities.add(h.entity());
                tufts.add(h);
                allBlocks.add(h);
                growBlock(h, 0.7f, 22);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.7f);
            w.spawnParticle(Particle.ITEM_SNOWBALL, center.clone().add(0, 2, 0), 50, 2, 3, 2, 0.1);
        }

        private void growBlock(BlockDisplayHandle h, float s, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(s, s, s),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Body rock left/right (Z rotation ±15°), period ~80t
            if (tick > 30 && tick % 5 == 0) {
                float rockAngle = (float) (Math.sin(tick * 0.08) * Math.toRadians(15));
                for (BlockDisplayHandle h : torso) rotateOnZ(h, rockAngle, 5);
                for (BlockDisplayHandle h : headFaceFangs) rotateOnZ(h, rockAngle, 5);
            }

            // Arms raise + pound every 50t
            int poundCycle = (tick - 50) % 50;
            if (tick >= 50 && poundCycle == 0) {
                // Raise arms overhead
                for (BlockDisplayHandle h : leftArm) rotateOnZ(h, (float) Math.toRadians(-150), 12);
                for (BlockDisplayHandle h : rightArm) rotateOnZ(h, (float) Math.toRadians(150), 12);
            }
            if (tick >= 50 && poundCycle == 20) {
                // Pound down + impact
                for (BlockDisplayHandle h : leftArm) rotateOnZ(h, 0f, 4);
                for (BlockDisplayHandle h : rightArm) rotateOnZ(h, 0f, 4);
                triggerImpactDamage(c.clone());
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.8f, 0.4f);
                c.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, c.clone().add(0, 0.3, 0), 80, 4, 0.5, 4, 0.4);
                c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 0.3, 0), 30, 3, 0.3, 3,
                        Material.WHITE_CONCRETE.createBlockData());
            }

            // Roar sound every 60t
            if (tick > 0 && tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.6f, 0.3f);
            }

            // Tufts shed FALLING_DUST
            if (tick % 5 == 0) {
                for (BlockDisplayHandle h : tufts) {
                    c.getWorld().spawnParticle(Particle.FALLING_DUST, h.entity().getLocation(), 2, 0.2, 0.2, 0.2,
                            Material.WHITE_CONCRETE.createBlockData());
                }
            }

            // Dissipate: roar + scale up 20% for 6t at tick 200, then disintegrate
            if (tick == 200) {
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    e.setInterpolationDuration(6);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(s.x * 1.2f, s.y * 1.2f, s.z * 1.2f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 2.0f, 0.2f);
            }
            if (tick == 210) {
                for (BlockDisplayHandle h : allBlocks) shrinkToZero(h, 10);
                c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 3, 0), 60, 3, 3, 3, 0.1);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3, 0), 50, 3, 3, 3, 0.1);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.5f);
            }
        }

        private void rotateOnZ(BlockDisplayHandle h, float angle, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, 0f, 0f, 1f),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        }

        private void shrinkToZero(BlockDisplayHandle h, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new YetiAbominable(plugin); }
    }

    // ================================================================
    // #6 — ICE BASILISK GAZE
    // 33 blocks: 16 BLUE_ICE coil + 1 PACKED_ICE head + 4 neck segments
    // + 6 AMETHYST crown spikes + 4 QUARTZ fangs + 2 DIAMOND eyes.
    // Constant 10.0r (gaze), 5.5h/14t, 30t delay.
    // ================================================================
    public static class IceBasiliskGaze extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> coil = new ArrayList<>();
        private final List<BlockDisplayHandle> neck = new ArrayList<>();
        private final List<BlockDisplayHandle> headCrownFangs = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float coilRotation = 0f;

        public IceBasiliskGaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_basilisk_gaze", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1320.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(240);
            config.setCooldownTicks(60);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Coil: 16 BLUE_ICE in flat spiral, radius 4 -> 1, Y=0
            for (int i = 0; i < 16; i++) {
                double t = i / 15.0;
                double angle = t * Math.PI * 4; // 2 loops
                double radius = 4.0 - t * 3.0;
                double px = Math.cos(angle) * radius;
                double pz = Math.sin(angle) * radius;
                Location loc = center.clone().add(px, 0.3, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(140, 200, 255).interpolation(15, i);
                spawnedEntities.add(h.entity());
                coil.add(h);
                allBlocks.add(h);
                growBlock(h, 0.9f, 18);
            }

            // Neck: 4 BLUE_ICE rising from center over 25t
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(0, 0.5 + s * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(140, 200, 255).interpolation(15, 16 + s * 2);
                spawnedEntities.add(h.entity());
                neck.add(h);
                allBlocks.add(h);
                growBlock(h, 0.7f, 20);
            }

            // Head: 1 PACKED_ICE atop neck
            BlockDisplayHandle head = displayBuilder.spawnBlock(center.clone().add(0, 3.4, 0), Material.PACKED_ICE);
            head.scale(0.0f, 0.0f, 0.0f).glow(200, 230, 255).interpolation(15, 24);
            spawnedEntities.add(head.entity());
            headCrownFangs.add(head);
            allBlocks.add(head);
            growBlock(head, 1.0f, 22);

            // Crown spikes: 6 AMETHYST atop head
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * i) / 6;
                double px = Math.cos(a) * 0.5;
                double pz = Math.sin(a) * 0.5;
                Location loc = center.clone().add(px, 4.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(180, 130, 255).interpolation(15, 26);
                spawnedEntities.add(h.entity());
                headCrownFangs.add(h);
                allBlocks.add(h);
                growXYZ(h, 0.25f, 0.6f, 0.25f, 20);
            }

            // Fangs: 4 QUARTZ_BLOCK below head
            for (int i = 0; i < 4; i++) {
                double xOff = -0.3 + i * 0.2;
                Location loc = center.clone().add(xOff, 3.15, 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(255, 255, 255).interpolation(15, 28);
                spawnedEntities.add(h.entity());
                headCrownFangs.add(h);
                allBlocks.add(h);
                growBlock(h, 0.15f, 18);
            }

            // Eyes: 2 DIAMOND_BLOCK
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add((i == 0 ? -0.3 : 0.3), 3.5, 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(170, 230, 255).interpolation(15, 28);
                spawnedEntities.add(h.entity());
                eyes.add(h);
                allBlocks.add(h);
                growBlock(h, 0.2f, 18);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.6f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 0.5, 0), 40, 4, 0.3, 4, 0.02);
        }

        private void growBlock(BlockDisplayHandle h, float s, int duration) {
            growXYZ(h, s, s, s, duration);
        }

        private void growXYZ(BlockDisplayHandle h, float sx, float sy, float sz, int duration) {
            BlockDisplay e = h.entity();
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

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Coil rotates slowly on Y axis 0.5°/tick (~0.0087 rad/tick)
            coilRotation += 0.0087f;
            if (tick > 30 && tick % 3 == 0) {
                for (BlockDisplayHandle h : coil) {
                    rotateOnY(h, coilRotation, 3);
                }
            }

            // Head tracks toward player every 30t
            if (tick > 30 && tick % 30 == 0) {
                Player target = findNearestPlayer(c, 30.0);
                if (target != null) {
                    double dx = target.getLocation().getX() - c.getX();
                    double dz = target.getLocation().getZ() - c.getZ();
                    float yaw = (float) Math.atan2(dz, dx);
                    for (BlockDisplayHandle h : headCrownFangs) rotateOnY(h, yaw, 15);
                    for (BlockDisplayHandle h : eyes) rotateOnY(h, yaw, 15);
                    for (BlockDisplayHandle h : neck) rotateOnY(h, yaw, 15);
                }
            }

            // Eye flash every 40t — scale pulse + light burst
            if (tick > 30 && tick % 40 == 0) {
                for (BlockDisplayHandle h : eyes) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(4);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0.4f, 0.4f, 0.4f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 3.5, 0.5), 30, 1, 0.5, 1, 0.3);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.3f);
            }
            // After 8t, reset eye scale
            if (tick > 30 && (tick - 8) % 40 == 0) {
                for (BlockDisplayHandle h : eyes) growBlock(h, 0.2f, 4);
            }

            // SNOWFLAKE from coil constantly
            if (tick % 3 == 0) {
                for (int i = 0; i < coil.size(); i += 3) {
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            coil.get(i).entity().getLocation(), 1, 0.2, 0.2, 0.2, 0.02);
                }
            }

            // Dissipate: head sinks into coil, coil scatters
            if (tick == 215) {
                for (BlockDisplayHandle h : headCrownFangs) shrinkToZero(h, 15);
                for (BlockDisplayHandle h : eyes) shrinkToZero(h, 15);
                for (BlockDisplayHandle h : neck) shrinkToZero(h, 15);
            }
            if (tick == 230) {
                for (int i = 0; i < coil.size(); i++) {
                    double a = (2.0 * Math.PI * i) / coil.size();
                    float ox = (float) (Math.cos(a) * 6.0);
                    float oz = (float) (Math.sin(a) * 6.0);
                    coil.get(i).animateTo(
                            new Vector3f(ox - 0.5f, -0.2f, oz - 0.5f),
                            new AxisAngle4f((float) Math.PI, 1f, 0f, 0f),
                            new Vector3f(0.2f, 0.2f, 0.2f), 10);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.7f);
            }
        }

        private void rotateOnY(BlockDisplayHandle h, float angle, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, 0f, 1f, 0f),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        }

        private void shrinkToZero(BlockDisplayHandle h, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceBasiliskGaze(plugin); }
    }

    // ================================================================
    // #7 — GLACIER TITAN RISING
    // 38 blocks: 12 PACKED_ICE torso + 2 BLUE_ICE head + 10 arm boulders
    // + 6 leg segments + 8 OBSIDIAN chest spikes.
    // Impact-only 13.0r 12.0h on overhead slam (every 120t).
    // ================================================================
    public static class GlacierTitanRising extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> torsoHead = new ArrayList<>();
        private final List<BlockDisplayHandle> leftArm = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArm = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>();
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private boolean armsRaised = false;

        public GlacierTitanRising(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacier_titan_rising", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(2880.0);
            config.setImpactRadius(18.0);
            config.setDamage(0.0);
            config.setDamageRadius(18.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(3);
            config.setDurationTicks(280);
            config.setCooldownTicks(60);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Torso/mountain-base: 12 PACKED_ICE, 3.0 scale
            double[][] torsoOffsets = {
                    {-1.5, 2.0, -1.0}, {0.0, 2.0, -1.0}, {1.5, 2.0, -1.0},
                    {-1.5, 4.0, -1.0}, {0.0, 4.0, -1.0}, {1.5, 4.0, -1.0},
                    {-1.0, 6.0, -1.0}, {0.0, 6.0, -1.0}, {1.0, 6.0, -1.0},
                    {-1.0, 8.0, -1.0}, {0.0, 8.0, -1.0}, {1.0, 8.0, -1.0}
            };
            for (double[] off : torsoOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 230, 255).interpolation(25, 0);
                spawnedEntities.add(h.entity());
                torsoHead.add(h);
                allBlocks.add(h);
                growBlock(h, 2.0f, 28);
            }

            // Head: 2 BLUE_ICE
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add((i == 0 ? -0.6 : 0.6), 10.0, -1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(140, 200, 255).interpolation(25, 6);
                spawnedEntities.add(h.entity());
                torsoHead.add(h);
                allBlocks.add(h);
                growBlock(h, 1.8f, 28);
            }

            // Left arm: 5 boulder segments at side (initially hidden / Y at torso)
            for (int s = 0; s < 5; s++) {
                Location loc = center.clone().add(-3.0 - s * 0.6, 6.0 - s * 0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 230, 255).interpolation(25, 35);
                spawnedEntities.add(h.entity());
                leftArm.add(h);
                allBlocks.add(h);
                float taper = 1.4f - s * 0.08f;
                growBlock(h, taper, 30);
            }
            // Right arm
            for (int s = 0; s < 5; s++) {
                Location loc = center.clone().add(3.0 + s * 0.6, 6.0 - s * 0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(200, 230, 255).interpolation(25, 35);
                spawnedEntities.add(h.entity());
                rightArm.add(h);
                allBlocks.add(h);
                float taper = 1.4f - s * 0.08f;
                growBlock(h, taper, 30);
            }

            // Legs: 2 sets of 3, embedded in ground
            for (int side = 0; side < 2; side++) {
                double xSign = (side == 0) ? -1.0 : 1.0;
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(xSign, 0.5 - s * 0.5, -1.0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                    h.scale(0.0f, 0.0f, 0.0f).glow(200, 230, 255).interpolation(25, 0);
                    spawnedEntities.add(h.entity());
                    legs.add(h);
                    allBlocks.add(h);
                    growBlock(h, 1.4f, 28);
                }
            }

            // Chest spikes: 8 OBSIDIAN, 0.4x2.0x0.4
            for (int i = 0; i < 8; i++) {
                double xOff = -1.4 + (i % 4) * 0.9;
                double yOff = 4.5 + (i / 4) * 1.5;
                Location loc = center.clone().add(xOff, yOff, -0.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.0f, 0.0f, 0.0f).glow(40, 30, 60).interpolation(25, 12);
                spawnedEntities.add(h.entity());
                spikes.add(h);
                allBlocks.add(h);
                growXYZ(h, 0.4f, 2.0f, 0.4f, 30);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 2.0f, 0.3f);
            w.spawnParticle(Particle.CLOUD, center.clone().add(0, 6, 0), 80, 4, 4, 4, 0.05);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 6, 0), 60, 3, 3, 3, 0.1);
        }

        private void growBlock(BlockDisplayHandle h, float s, int duration) {
            growXYZ(h, s, s, s, duration);
        }

        private void growXYZ(BlockDisplayHandle h, float sx, float sy, float sz, int duration) {
            BlockDisplay e = h.entity();
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

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Body breathe scale pulse between slams
            if (tick > 60 && tick % 6 == 0) {
                float pulse = 1.0f + 0.04f * (float) Math.sin(tick * 0.08);
                for (BlockDisplayHandle h : torsoHead) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    e.setInterpolationDuration(6);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(2.0f * pulse, 2.0f * pulse, 2.0f * pulse),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Overhead slam at tick 120, again at 240
            int[] slamTicks = {120, 240};
            for (int slamAt : slamTicks) {
                if (tick == slamAt - 30 && !armsRaised) {
                    armsRaised = true;
                    for (BlockDisplayHandle h : leftArm) rotateOnZ(h, (float) Math.toRadians(-160), 25);
                    for (BlockDisplayHandle h : rightArm) rotateOnZ(h, (float) Math.toRadians(160), 25);
                }
                if (tick == slamAt) {
                    armsRaised = false;
                    for (BlockDisplayHandle h : leftArm) rotateOnZ(h, 0f, 4);
                    for (BlockDisplayHandle h : rightArm) rotateOnZ(h, 0f, 4);
                    triggerImpactDamage(c.clone());
                    for (int s = 0; s < 6; s++) {
                        DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f + s * 0.05f);
                    }
                    c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 0.3, 0), 100, 6, 0.5, 6,
                            Material.WHITE_CONCRETE.createBlockData());
                    c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 1, 0), 80, 5, 1, 5, 0.2);
                }
            }

            // Shoulder cloud particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(-2.5, 7, 0), 3, 0.3, 0.3, 0.3, 0.02);
                c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(2.5, 7, 0), 3, 0.3, 0.3, 0.3, 0.02);
            }

            // Dissipate ticks 255-280: falls forward, blocks crash outward
            if (tick == 255) {
                for (BlockDisplayHandle h : torsoHead) rotateOnZ(h, (float) Math.toRadians(75), 25);
            }
            if (tick == 275) {
                for (BlockDisplayHandle h : allBlocks) shrinkToZero(h, 5);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.4f);
                c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, c.clone().add(0, 3, 0), 5, 4, 2, 4, 0);
            }
        }

        private void rotateOnZ(BlockDisplayHandle h, float angle, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, 0f, 0f, 1f),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        }

        private void shrinkToZero(BlockDisplayHandle h, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GlacierTitanRising(plugin); }
    }

    // ================================================================
    // #8 — FROST REAPER HARBINGER
    // 38 blocks: 8 TINTED_GLASS hood cone + 1 QUARTZ skull + 2 OBSIDIAN sockets
    // + 6 sleeve TINTED_GLASS + 6 SMOOTH_QUARTZ scythe shaft + 8 BLUE_ICE blade
    // + 6 TINTED_GLASS cloak hem. Drifts toward player. Constant 8.0r + impact sweep 11.0r 9h.
    // ================================================================
    public static class FrostReaperHarbinger extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> hood = new ArrayList<>();
        private final List<BlockDisplayHandle> skullFace = new ArrayList<>();
        private final List<BlockDisplayHandle> sleeves = new ArrayList<>();
        private final List<BlockDisplayHandle> scytheShaft = new ArrayList<>();
        private final List<BlockDisplayHandle> scytheBlade = new ArrayList<>();
        private final List<BlockDisplayHandle> cloakHem = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();

        public FrostReaperHarbinger(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_reaper_harbinger", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1200.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            // Impact sweep on scythe swing
            config.setImpactDamage(2160.0);
            config.setImpactRadius(16.5);
            config.setDamageOnImpactOnly(false);
            config.setDurationTicks(240);
            config.setCooldownTicks(60);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Hood/body cone: 8 TINTED_GLASS, tall cone (scale 1.2x2.5x1.2)
            for (int i = 0; i < 8; i++) {
                double yOff = 2.0 + (i / 2) * 0.7;
                double xOff = (i % 2 == 0) ? -0.4 : 0.4;
                Location loc = center.clone().add(xOff, yOff, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(60, 60, 100).interpolation(25, 0);
                spawnedEntities.add(h.entity());
                hood.add(h);
                allBlocks.add(h);
                growXYZ(h, 1.2f, 1.4f, 1.2f, 25);
            }

            // Skull face: 1 QUARTZ_BLOCK
            BlockDisplayHandle skull = displayBuilder.spawnBlock(center.clone().add(0, 5.0, 0.4), Material.QUARTZ_BLOCK);
            skull.scale(0.0f, 0.0f, 0.0f).glow(250, 250, 250).interpolation(25, 5);
            spawnedEntities.add(skull.entity());
            skullFace.add(skull);
            allBlocks.add(skull);
            growBlock(skull, 0.7f, 25);

            // Eye sockets: 2 OBSIDIAN
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add((i == 0 ? -0.2 : 0.2), 5.1, 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.0f, 0.0f, 0.0f).glow(20, 0, 30).interpolation(20, 10);
                spawnedEntities.add(h.entity());
                skullFace.add(h);
                allBlocks.add(h);
                growBlock(h, 0.15f, 20);
            }

            // Sleeve-arms: 2 sets of 3 TINTED_GLASS
            for (int side = 0; side < 2; side++) {
                double xSign = (side == 0) ? -1 : 1;
                for (int s = 0; s < 3; s++) {
                    double xOff = xSign * (1.0 + s * 0.3);
                    double yOff = 3.5 - s * 0.4;
                    Location loc = center.clone().add(xOff, yOff, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                    h.scale(0.0f, 0.0f, 0.0f).glow(70, 70, 110).interpolation(22, 12);
                    spawnedEntities.add(h.entity());
                    sleeves.add(h);
                    allBlocks.add(h);
                    growBlock(h, 0.5f, 22);
                }
            }

            // Scythe shaft: 6 SMOOTH_QUARTZ, scale 0.25x3.0x0.25 along right side
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(1.8, 2.5 + i * 0.7, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                h.scale(0.0f, 0.0f, 0.0f).glow(240, 240, 250).interpolation(22, 15);
                spawnedEntities.add(h.entity());
                scytheShaft.add(h);
                allBlocks.add(h);
                growXYZ(h, 0.25f, 0.8f, 0.25f, 22);
            }

            // Scythe blade: 8 BLUE_ICE in curved crescent at top of shaft
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 0.5 + (i / 7.0) * Math.PI * 0.7;
                double px = 1.8 + Math.cos(a) * 1.6;
                double py = 6.5 + Math.sin(a) * 1.0;
                Location loc = center.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.0f, 0.0f, 0.0f).glow(160, 220, 255).interpolation(22, 18);
                spawnedEntities.add(h.entity());
                scytheBlade.add(h);
                allBlocks.add(h);
                growBlock(h, 0.6f, 22);
            }

            // Cloak hem: 6 TINTED_GLASS frayed at base
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * i) / 6;
                double px = Math.cos(a) * 0.9;
                double pz = Math.sin(a) * 0.9;
                Location loc = center.clone().add(px, 0.6, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.0f, 0.0f, 0.0f).glow(50, 50, 90).interpolation(22, 20);
                spawnedEntities.add(h.entity());
                cloakHem.add(h);
                allBlocks.add(h);
                growXYZ(h, 0.35f, 1.0f, 0.35f, 22);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.5f, 0.4f);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 2.5, 0), 30, 1.5, 1.5, 1.5, 0.02);
        }

        private void growBlock(BlockDisplayHandle h, float s, int duration) {
            growXYZ(h, s, s, s, duration);
        }

        private void growXYZ(BlockDisplayHandle h, float sx, float sy, float sz, int duration) {
            BlockDisplay e = h.entity();
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

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drift toward player (every 3 ticks)
            if (tick > 30 && tick % 3 == 0) {
                Player target = findNearestPlayer(c, 30.0);
                if (target != null) {
                    double dx = target.getLocation().getX() - c.getX();
                    double dz = target.getLocation().getZ() - c.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 1.5) {
                        double mvX = (dx / dist) * 0.12;
                        double mvZ = (dz / dist) * 0.12;
                        Location nc = c.clone().add(mvX, 0, mvZ);
                        setCenter(nc);
                        for (var ent : spawnedEntities) {
                            if (ent != null && ent.isValid()) {
                                try { ent.teleport(ent.getLocation().add(mvX, 0, mvZ)); } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
            }

            // SCULK_SOUL trail at cloak hem
            if (tick % 4 == 0) {
                for (BlockDisplayHandle h : cloakHem) {
                    c.getWorld().spawnParticle(Particle.SCULK_SOUL, h.entity().getLocation(), 1, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Scythe sweep every 50t — full 180° Z rotation on shaft + blade
            if (tick > 40 && tick % 50 == 0) {
                // Sweep blade + shaft together
                for (BlockDisplayHandle h : scytheShaft) rotateOnY(h, (float) Math.toRadians(180), 12);
                for (BlockDisplayHandle h : scytheBlade) rotateOnY(h, (float) Math.toRadians(180), 12);
                triggerImpactDamage(c.clone().add(0, 2, 0));
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_2, 1.6f, 0.7f);
                c.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, c.clone().add(0, 3, 0), 80, 4, 1, 4, 0.3);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3, 0), 60, 4, 1, 4, 0.2);
            }
            // Reset scythe after 14t
            if (tick > 40 && (tick - 14) % 50 == 0) {
                for (BlockDisplayHandle h : scytheShaft) rotateOnY(h, 0f, 6);
                for (BlockDisplayHandle h : scytheBlade) rotateOnY(h, 0f, 6);
            }

            // Haunting sound every 50t
            if (tick > 0 && tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.3f);
            }

            // Dissipate: body fades upward into snow shower
            if (tick == 215) {
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(25);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, 4.0f - 0.5f, -0.5f),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0f, 0f, 0f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 4, 0), 80, 3, 3, 3, 0.1);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 0.6f);
            }
        }

        private void rotateOnY(BlockDisplayHandle h, float angle, int duration) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, 0f, 1f, 0f),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostReaperHarbinger(plugin); }
    }

    // ================================================================
    // #9 — ICE VALKYRIE DESCENT
    // 41 blocks: 5 DIAMOND torso + 1 BLUE_ICE helm + 2 CALCITE horns
    // + 5 QUARTZ spear shaft + 3 PACKED_ICE spearhead + 16 BLUE_STAINED_GLASS wings
    // + 4 LIGHT_BLUE_CONCRETE shield + 4 ICE skirt.
    // Impact-only 7.5r 10h on spear stab (every 60t).
    // ================================================================
    public static class IceValkyrieDescent extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> leftWing = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWing = new ArrayList<>();
        private final List<BlockDisplayHandle> spear = new ArrayList<>();
        private final List<BlockDisplayHandle> shield = new ArrayList<>();
        private final List<BlockDisplayHandle> skirt = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float wingPhase = 0f;

        public IceValkyrieDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_valkyrie_descent", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(2400.0);
            config.setImpactRadius(11.25);
            config.setDamage(0.0);
            config.setDamageRadius(11.25);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(3);
            config.setDurationTicks(220);
            config.setCooldownTicks(60);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Body/armor: 5 DIAMOND_BLOCK
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 15 + i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.9f, 0.9f, 0.9f).glow(170, 230, 255).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
                allBlocks.add(h);
            }

            // Winged helm: 1 BLUE_ICE
            BlockDisplayHandle helm = displayBuilder.spawnBlock(center.clone().add(0, 19, 0), Material.BLUE_ICE);
            helm.scale(0.8f, 0.8f, 0.8f).glow(140, 200, 255).interpolation(5, 0);
            spawnedEntities.add(helm.entity());
            body.add(helm);
            allBlocks.add(helm);

            // 2 CALCITE horns
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add((i == 0 ? -0.5 : 0.5), 19.3, -0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.3f, 1.0f, 0.3f).glow(245, 245, 235).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
                allBlocks.add(h);
            }

            // Spear shaft: 5 QUARTZ_BLOCK pointing down (tip at low end)
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0.9, 14 + i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.2f, 0.8f, 0.2f).glow(255, 255, 255).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                spear.add(h);
                allBlocks.add(h);
            }

            // Spearhead: 3 PACKED_ICE diamond shape at tip
            for (int i = 0; i < 3; i++) {
                double yOff = 13.0 + i * 0.3;
                Location loc = center.clone().add(0.9, yOff, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 230, 255).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                spear.add(h);
                allBlocks.add(h);
            }

            // Wings: 2 sets of 8 BLUE_STAINED_GLASS feathers
            for (int side = 0; side < 2; side++) {
                double xSign = (side == 0) ? -1 : 1;
                List<BlockDisplayHandle> wing = (side == 0) ? leftWing : rightWing;
                for (int i = 0; i < 8; i++) {
                    double xOff = xSign * (0.8 + i * 0.4);
                    double yOff = 17 + Math.sin(i * 0.3) * 0.4;
                    Location loc = center.clone().add(xOff, yOff, -0.3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                    h.scale(0.5f, 0.1f, 1.5f).glow(140, 200, 240).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                    wing.add(h);
                    allBlocks.add(h);
                }
            }

            // Shield: 4 LIGHT_BLUE_CONCRETE round (left arm)
            double[][] shieldOffsets = {{-0.9, 16.5, 0.3}, {-1.2, 17.0, 0.3}, {-1.2, 16.0, 0.3}, {-0.9, 17.2, 0.3}};
            for (double[] off : shieldOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_CONCRETE);
                h.scale(0.6f, 0.6f, 0.2f).glow(140, 200, 240).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                shield.add(h);
                allBlocks.add(h);
            }

            // Skirt: 4 ICE segments
            for (int i = 0; i < 4; i++) {
                double a = (2.0 * Math.PI * i) / 4;
                double px = Math.cos(a) * 0.7;
                double pz = Math.sin(a) * 0.7;
                Location loc = center.clone().add(px, 15.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(0.5f, 0.9f, 0.5f).glow(180, 220, 255).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                skirt.add(h);
                allBlocks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.6f, 0.8f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 17, 0), 40, 3, 2, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Descent phase: ticks 0-35, drop Y -12 (from Y+15 -> Y+3)
            if (tick <= 35) {
                float dropY = -12f * (tick / 35f);
                if (tick % 3 == 0) {
                    for (BlockDisplayHandle h : allBlocks) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(3);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(-0.5f, dropY - 0.5f, -0.5f),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0.9, 13 + dropY, 0), 8, 0.3, 1, 0.3, 0.2);
                    c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 17 + dropY, 0), 6, 0.5, 0.5, 0.5, 0.05);
                }
            }

            // Hover Y+3 from tick 35
            // Wings flap (25t cycle ±30°)
            if (tick > 35 && tick % 3 == 0) {
                wingPhase += 0.25f;
                float flap = (float) (Math.sin(wingPhase) * Math.toRadians(30));
                for (BlockDisplayHandle h : leftWing) rotateOnZ(h, -flap, 3, -12f);
                for (BlockDisplayHandle h : rightWing) rotateOnZ(h, flap, 3, -12f);
            }

            // Snowflake shedding from wings
            if (tick % 5 == 0) {
                for (BlockDisplayHandle h : leftWing) {
                    if (Math.random() < 0.3) {
                        c.getWorld().spawnParticle(Particle.SNOWFLAKE, h.entity().getLocation(), 1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            }

            // Spear stab every 60t after tick 40 — translate +Z 2 blocks rapidly, return
            if (tick >= 60 && (tick - 60) % 60 == 0) {
                for (BlockDisplayHandle h : spear) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(3);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f + 0f, -12f - 0.5f, -0.5f + 2.5f),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                Location stabAt = c.clone().add(0.9, 3, 2.5);
                triggerImpactDamage(stabAt);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.5f);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, stabAt, 50, 1, 1, 1, 0.5);
            }
            // Reset spear after 8t
            if (tick >= 60 && (tick - 68) % 60 == 0) {
                for (BlockDisplayHandle h : spear) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(4);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, -12f - 0.5f, -0.5f),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Dissipate: ascend out of view + shrink (last 25t)
            if (tick == 195) {
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(25);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, 10f - 0.5f, -0.5f),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0f, 0f, 0f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.4f, 1.3f);
            }
        }

        private void rotateOnZ(BlockDisplayHandle h, float angle, int duration, float baseY) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(-0.5f, baseY - 0.5f, -0.5f),
                    new AxisAngle4f(angle, 0f, 0f, 1f),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceValkyrieDescent(plugin); }
    }

    // ================================================================
    // #10 — FROST FAIRY FLIGHT
    // 36 blocks: 6 fairies × 6 blocks each (1 PACKED_ICE body + 2 BLUE_STAINED_GLASS
    // wings + 2 DIAMOND sparkle points + 1 extra PACKED_ICE trailing block).
    // Hexagon ring at Y+3. Constant 9.0r, 5h/10t, 18t delay.
    // ================================================================
    public static class FrostFairyFlight extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> fairies = new ArrayList<>();
        private final List<BlockDisplayHandle> allBlocks = new ArrayList<>();
        private float orbitAngle = 0f;
        private int divingFairy = -1;
        private int diveStartTick = -1;

        public FrostFairyFlight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_fairy_flight", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(1140.0);
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDurationTicks(260);
            config.setCooldownTicks(60);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double ringRadius = 4.5;
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * i) / 6;
                double px = Math.cos(a) * ringRadius;
                double pz = Math.sin(a) * ringRadius;
                Location fairyLoc = center.clone().add(px, 3, pz);
                List<BlockDisplayHandle> fairy = new ArrayList<>();

                // Body: 1 PACKED_ICE
                BlockDisplayHandle body = displayBuilder.spawnBlock(fairyLoc, Material.PACKED_ICE);
                body.scale(0.0f, 0.0f, 0.0f).glow(200, 230, 255).interpolation(15, 0);
                spawnedEntities.add(body.entity());
                fairy.add(body);
                allBlocks.add(body);
                growBlock(body, 0.3f, 18);

                // 2 wings: BLUE_STAINED_GLASS
                for (int s = 0; s < 2; s++) {
                    double xSign = (s == 0) ? -1 : 1;
                    Location wingLoc = fairyLoc.clone().add(xSign * 0.25, 0, 0);
                    BlockDisplayHandle wing = displayBuilder.spawnBlock(wingLoc, Material.BLUE_STAINED_GLASS);
                    wing.scale(0.0f, 0.0f, 0.0f).glow(140, 200, 240).interpolation(15, 3);
                    spawnedEntities.add(wing.entity());
                    fairy.add(wing);
                    allBlocks.add(wing);
                    growXYZ(wing, 0.15f, 0.05f, 0.35f, 18);
                }

                // 2 sparkle points: DIAMOND_BLOCK
                for (int s = 0; s < 2; s++) {
                    double offX = (s == 0) ? -0.15 : 0.15;
                    double offY = (s == 0) ? 0.25 : -0.25;
                    Location sparkLoc = fairyLoc.clone().add(offX, offY, 0);
                    BlockDisplayHandle spark = displayBuilder.spawnBlock(sparkLoc, Material.DIAMOND_BLOCK);
                    spark.scale(0.0f, 0.0f, 0.0f).glow(220, 240, 255).interpolation(15, 5);
                    spawnedEntities.add(spark.entity());
                    fairy.add(spark);
                    allBlocks.add(spark);
                    growBlock(spark, 0.1f, 18);
                }

                // 1 trailing PACKED_ICE (5th block listed as 6 blocks per fairy, this is the 6th)
                Location trailLoc = fairyLoc.clone().add(0, 0, -0.3);
                BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.PACKED_ICE);
                trail.scale(0.0f, 0.0f, 0.0f).glow(180, 220, 255).interpolation(15, 7);
                spawnedEntities.add(trail.entity());
                fairy.add(trail);
                allBlocks.add(trail);
                growBlock(trail, 0.2f, 18);

                fairies.add(fairy);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.5f);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, 3, 0), 60, 4, 1, 4, 0.05);
        }

        private void growBlock(BlockDisplayHandle h, float s, int duration) {
            growXYZ(h, s, s, s, duration);
        }

        private void growXYZ(BlockDisplayHandle h, float sx, float sy, float sz, int duration) {
            BlockDisplay e = h.entity();
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

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Orbit rotation: each fairy moves around the shared center
            orbitAngle += 0.04f;
            double ringRadius = 4.5;
            if (tick > 18 && tick % 2 == 0) {
                for (int i = 0; i < fairies.size(); i++) {
                    double fairyOffset = (2.0 * Math.PI * i) / 6;
                    double a = orbitAngle + fairyOffset;
                    float px = (float) (Math.cos(a) * ringRadius);
                    float pz = (float) (Math.sin(a) * ringRadius);
                    // Default Y offset 3, diving fairy gets reduced Y
                    float yOff = 3f;
                    if (i == divingFairy && diveStartTick >= 0) {
                        int diveProgress = tick - diveStartTick;
                        if (diveProgress <= 10) {
                            yOff = 3f - 3f * (diveProgress / 10f); // dive to ground
                        } else if (diveProgress <= 20) {
                            yOff = -0f + 3f * ((diveProgress - 10) / 10f); // rise back
                        } else {
                            divingFairy = -1;
                            diveStartTick = -1;
                        }
                    }
                    for (BlockDisplayHandle h : fairies.get(i)) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(px - 0.5f, yOff - 0.5f, pz - 0.5f),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Wing flutter: rapid scale pulse
            if (tick > 18 && tick % 2 == 0) {
                float pulse = 0.15f + 0.05f * (float) Math.sin(tick * 1.0);
                for (List<BlockDisplayHandle> fairy : fairies) {
                    // wings are at index 1 and 2
                    for (int wi = 1; wi <= 2; wi++) {
                        BlockDisplayHandle h = fairy.get(wi);
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                new Vector3f(pulse, 0.05f, 0.35f),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }

            // Periodic dive every 25t (one random fairy)
            if (tick > 40 && tick % 25 == 0 && divingFairy == -1) {
                divingFairy = (int) (Math.random() * 6);
                diveStartTick = tick;
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2, 0), 30, 3, 1, 3, 0.1);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
            }

            // GLOW + ENCHANT trail per fairy
            if (tick % 3 == 0) {
                for (List<BlockDisplayHandle> fairy : fairies) {
                    Location fairyLoc = fairy.get(0).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.GLOW, fairyLoc, 2, 0.2, 0.2, 0.2, 0.01);
                    c.getWorld().spawnParticle(Particle.ENCHANT, fairyLoc, 3, 0.3, 0.3, 0.3, 0.5);
                }
            }

            // Random chime per fairy
            if (tick > 0 && tick % 30 == 0) {
                float randomPitch = 1.0f + (float) Math.random() * 0.8f;
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, randomPitch);
            }

            // Dissipate: converge to center then burst (last 18t)
            if (tick == 242) {
                for (BlockDisplayHandle h : allBlocks) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(12);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, 3f - 0.5f, -0.5f),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }
            if (tick == 256) {
                for (int i = 0; i < allBlocks.size(); i++) {
                    BlockDisplayHandle h = allBlocks.get(i);
                    double a = (2.0 * Math.PI * i) / allBlocks.size();
                    float ox = (float) (Math.cos(a) * 6.0);
                    float oz = (float) (Math.sin(a) * 6.0);
                    h.animateTo(
                            new Vector3f(ox - 0.5f, 3f - 0.5f, oz - 0.5f),
                            new AxisAngle4f((float) Math.PI, 1f, 1f, 0f),
                            new Vector3f(0f, 0f, 0f), 4);
                }
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 3, 0), 80, 3, 1, 3, 0.2);
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 3, 0), 60, 3, 1, 3, 0.1);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.6f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostFairyFlight(plugin); }
    }
}
