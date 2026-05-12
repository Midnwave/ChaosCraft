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
 * FreezingIce Mode — BLOCK DISPLAY ATTACKS 61-70 (Reality-warping + Mobile)
 *
 * Spooky/surreal attacks (mirror echoes, hooded specters, frozen scenes) and
 * MOBILE attacks driven by the AbstractAttack.tickFollowAI() infrastructure
 * (slithering slime, climbing vine, rolling boulder, drifting specter, etc.).
 *
 * 7 of these 10 attacks opt-in to follow-AI in their constructor via
 * {@code config.setFollowAiEnabled(true)} + {@code setFollowAiWalkSpeed(x)}.
 * Each calls {@code tickFollowAI()} at the top of {@code onTick(int)} so the
 * center drifts toward the nearest non-exempt player at sub-walk pace, with
 * the AbstractAttack helper also teleporting spawned Display entities along.
 *
 * Ice/ghost palette:
 *  - PACKED_ICE, BLUE_ICE, ICE — frozen body
 *  - TINTED_GLASS, BLUE_STAINED_GLASS — ghost translucency / mirror panes
 *  - GRAY_STAINED_GLASS, WHITE_CONCRETE — fog / mist
 *  - BLACK_CONCRETE, OBSIDIAN — shadow / void / eye sockets
 *  - DIAMOND_BLOCK, AMETHYST_BLOCK — gem accents
 *  - SMOOTH_QUARTZ, QUARTZ_BLOCK, CALCITE — frame / bone / anchor
 *  - POWDER_SNOW — soft fog body
 *
 * Particles: SCULK_SOUL (ghost), GLOW (mirror shimmer), SNOWFLAKE,
 *            ELECTRIC_SPARK, REVERSE_PORTAL (warping), CLOUD,
 *            FALLING_DUST, SOUL_FIRE_FLAME, ITEM_SNOWBALL
 * Sounds: ENTITY_GLOW_SQUID_AMBIENT, BLOCK_AMETHYST_BLOCK_CHIME,
 *         BLOCK_GLASS_PLACE, BLOCK_GLASS_BREAK, BLOCK_GLASS_HIT,
 *         ITEM_TRIDENT_RIPTIDE, ENTITY_GENERIC_EXPLODE
 *
 * Attacks:
 *  61. MirroredPlayerEcho    — 39 blocks, mirror + doppelganger, FOLLOW-AI 0.11
 *  62. TimeFrozenObjects     — 36 blocks, suspended falling objects, NO follow
 *  63. FrozenGhostEcho       — 41 blocks, ice-knight ghost, FOLLOW-AI 0.09
 *  64. IceMirageMountain     — 38 blocks, distant-then-rushing mountain, NO follow
 *  65. SlitheringIceForm     — 34 blocks, lumpy slime, FOLLOW-AI 0.08
 *  66. DriftingFrostSpirit   — 35 blocks, hooded specter, FOLLOW-AI 0.11
 *  67. ClimbingIceVine       — 40 blocks, snake-segment vine, FOLLOW-AI 0.10
 *  68. RollingIceBoulder     — 36 blocks, spherical boulder, FOLLOW-AI 0.15
 *  69. StalkingFrostShadow   — 36 blocks, recursive mirror room, FOLLOW-AI 0.09
 *  70. PursuingIceWolves     — 36 blocks, drifting fog cluster, FOLLOW-AI 0.07
 */
public final class FreezingIceBlockDisplay7 {
    private FreezingIceBlockDisplay7() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new MirroredPlayerEcho(plugin));
        registry.register(new TimeFrozenObjects(plugin));
        registry.register(new FrozenGhostEcho(plugin));
        registry.register(new IceMirageMountain(plugin));
        registry.register(new SlitheringIceForm(plugin));
        registry.register(new DriftingFrostSpirit(plugin));
        registry.register(new ClimbingIceVine(plugin));
        registry.register(new RollingIceBoulder(plugin));
        registry.register(new StalkingFrostShadow(plugin));
        registry.register(new PursuingIceWolves(plugin));
    }

    // ================================================================
    // Helper: smoothly translate a single BlockDisplay by (x, y, z)
    // (relative to its current translation), using interpolation.
    // ================================================================
    private static void translateBy(BlockDisplay e, float dx, float dy, float dz, int dur) {
        Transformation t = e.getTransformation();
        Vector3f base = t.getTranslation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                new Vector3f(base.x + dx, base.y + dy, base.z + dz),
                new AxisAngle4f().set(t.getLeftRotation()),
                t.getScale(),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    // ================================================================
    // Helper: smoothly set absolute translation (overwrites base offset).
    // ================================================================
    private static void setTranslation(BlockDisplay e, float x, float y, float z, int dur) {
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                new AxisAngle4f().set(t.getLeftRotation()),
                t.getScale(),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    // ================================================================
    // Helper: apply absolute scale to a block display, interpolated.
    // ================================================================
    private static void setScale(BlockDisplay e, float sx, float sy, float sz, int dur) {
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

    // ================================================================
    // Helper: apply rotation about an axis, preserving translation/scale.
    // ================================================================
    private static void setRotation(BlockDisplay e, float angle, float ax, float ay, float az, int dur) {
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

    // ================================================================
    // #61 — MIRRORED PLAYER ECHO
    // 39 blocks: 4 torso, 1 head, 16 limb segments, 2 hands, 6 mirror frame,
    // 1 mirror surface, 4 cracks, 4 anchor + 1 (covered in design) = 39 total.
    // FOLLOW-AI 0.11 — doppelganger drifts toward player.
    // Constant radius 8.0, 5.5 hearts (~110 dmg) every 12t.
    // ================================================================
    public static class MirroredPlayerEcho extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> torso = new ArrayList<>();
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> limbs = new ArrayList<>();
        private final List<BlockDisplayHandle> hands = new ArrayList<>();
        private final List<BlockDisplayHandle> frame = new ArrayList<>();
        private final List<BlockDisplayHandle> mirrorSurface = new ArrayList<>();
        private final List<BlockDisplayHandle> cracks = new ArrayList<>();
        private final List<BlockDisplayHandle> anchors = new ArrayList<>();
        private float doppelEmerge = -1.4f;

        public MirroredPlayerEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mirrored_player_echo", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(110.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(200);
            config.setCooldownTicks(160);
            config.setDamageDelayTicks(25);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.11);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Mirror frame — 6 SMOOTH_QUARTZ blocks forming rectangular rim
            double[][] frameOffsets = {
                    {-1.2, 0.0, -1.4}, {1.2, 0.0, -1.4},
                    {-1.2, 1.4, -1.4}, {1.2, 1.4, -1.4},
                    { 0.0, 2.3, -1.4}, { 0.0,-0.7, -1.4}
            };
            for (double[] off : frameOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                h.scale(0.6f, 0.6f, 0.35f).glow(220, 230, 240).interpolation(6, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                frame.add(h);
            }

            // Mirror surface — 1 TINTED_GLASS large flat pane
            {
                Location loc = center.clone().add(0, 0.8, -1.35);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(2.5f, 2.5f, 0.1f).glow(160, 200, 235).interpolation(6, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                mirrorSurface.add(h);
            }

            // Mirror cracks — 4 BLUE_STAINED_GLASS small angled shards on the pane
            double[][] crackOffsets = {
                    {-0.6, 1.4, -1.30}, {0.7, 1.0, -1.30},
                    {-0.3, 0.3, -1.30}, {0.4, 1.7, -1.30}
            };
            for (double[] off : crackOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.15f, 0.6f, 0.05f).glow(120, 180, 230).interpolation(6, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                cracks.add(h);
            }

            // 4 floor anchors CALCITE under the mirror
            double[][] anchorOffsets = {{-1.3, -0.9, -1.4}, {1.3, -0.9, -1.4}, {-1.0, -0.9, -0.8}, {1.0, -0.9, -0.8}};
            for (double[] off : anchorOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.4f, 0.25f, 0.4f).glow(230, 230, 245).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                anchors.add(h);
            }

            // Doppelganger torso — 4 PACKED_ICE blocks (start inside mirror, Z = -1.3)
            double[][] torsoOffsets = {
                    {-0.25, 0.6, 0.0}, {0.25, 0.6, 0.0},
                    {-0.25, 1.2, 0.0}, {0.25, 1.2, 0.0}
            };
            for (double[] off : torsoOffsets) {
                Location loc = center.clone().add(off[0], off[1], -1.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.5f, 0.6f, 0.5f).glow(180, 220, 255).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                torso.add(h);
            }

            // Doppelganger head — 1 BLUE_ICE
            {
                Location loc = center.clone().add(0, 1.85, -1.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.8f, 0.8f, 0.6f).glow(150, 210, 255).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                head.add(h);
            }

            // 4 limbs × 4 segments = 16 PACKED_ICE blocks (arms + legs)
            double[][] limbBases = {
                    {-0.5, 1.2, 0.0},  // L arm shoulder
                    { 0.5, 1.2, 0.0},  // R arm shoulder
                    {-0.25, 0.5, 0.0}, // L leg hip
                    { 0.25, 0.5, 0.0}  // R leg hip
            };
            for (int i = 0; i < 4; i++) {
                double[] base = limbBases[i];
                double dy = i < 2 ? -0.32 : -0.35; // arms go down, legs go down
                for (int s = 0; s < 4; s++) {
                    Location loc = center.clone().add(base[0], base[1] + s * dy, -1.3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                    h.scale(0.25f, 0.3f, 0.25f).glow(180, 220, 255).interpolation(6, 0);
                    spawnedEntities.add(h.entity());
                    limbs.add(h);
                }
            }

            // 2 hands DIAMOND_BLOCK
            for (int s = 0; s < 2; s++) {
                double sx = s == 0 ? -0.55 : 0.55;
                Location loc = center.clone().add(sx, 0.0, -1.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.2f, 0.2f, 0.2f).glow(160, 240, 255).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                hands.add(h);
            }

            // Spawn FX
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.7f);
            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.2f, 0.6f);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, 1.1, -1.3), 30, 1.2, 1.2, 0.1, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            // Follow-AI: drift toward nearest non-exempt player
            tickFollowAI();

            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Emerge phase (0..25): translate doppelganger from inside mirror outward.
            if (tick <= 25) {
                doppelEmerge = -1.4f + (1.4f * (tick / 25f)); // -1.4 -> 0
                if (tick % 3 == 0) {
                    pushDoppelZ(doppelEmerge);
                }
                if (tick == 12) {
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            c.clone().add(0, 1.85, -1.3), 12, 0.3, 0.2, 0.2, 0.02);
                }
            }

            // Active: pose-snapshot mimic every 40t (tilt limbs subtly)
            if (tick > 25 && tick % 40 == 0) {
                float pose = (float) Math.toRadians(8 + Math.random() * 12);
                for (int i = 0; i < limbs.size(); i++) {
                    BlockDisplay e = limbs.get(i).entity();
                    setRotation(e, (i < 8 ? pose : -pose), 1f, 0f, 0f, 12);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.4f);
            }

            // Mirror cracks pulse (every 6t)
            if (tick > 25 && tick % 6 == 0) {
                float pulse = 1.0f + 0.15f * (float) Math.sin(tick * 0.25);
                for (BlockDisplayHandle h : cracks) {
                    setScale(h.entity(), 0.15f * pulse, 0.6f * pulse, 0.05f, 4);
                }
            }

            // Particles around doppelganger
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 1.0, 0), 4, 0.6, 0.8, 0.4, 0.02);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(0, 1.85, 0), 2, 0.2, 0.1, 0.1, 0.02);
            }

            // Dissipate: doppelganger sinks back into mirror over last 22t
            int duration = config.getDurationTicks();
            if (tick == duration - 22) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.8f);
            }
            if (tick >= duration - 22 && tick < duration) {
                int phase = tick - (duration - 22);
                float retreat = -((float) phase / 22f * 1.4f);
                if (phase % 3 == 0) {
                    pushDoppelZ(retreat);
                }
            }
        }

        private void pushDoppelZ(float zOff) {
            for (BlockDisplayHandle h : torso) translateBy(h.entity(), 0f, 0f, zOff, 6);
            for (BlockDisplayHandle h : head)  translateBy(h.entity(), 0f, 0f, zOff, 6);
            for (BlockDisplayHandle h : limbs) translateBy(h.entity(), 0f, 0f, zOff, 6);
            for (BlockDisplayHandle h : hands) translateBy(h.entity(), 0f, 0f, zOff, 6);
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new MirroredPlayerEcho(plugin); }
    }

    // ================================================================
    // #62 — TIME-FROZEN OBJECTS
    // 36 blocks: 8 rocks, 8 icicles, 8 shards, 8 dust-clouds, 4 anchor glyphs.
    // NO follow-AI. Constant 9.0r, 6.5 hearts (130 dmg) every 12t, 25t delay.
    // Every 60t, all objects jolt forward 1b then re-freeze. Unfreeze impact
    // bonus damage at radius 9.0 / 12 hearts (240) — handled inline.
    // ================================================================
    public static class TimeFrozenObjects extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> rocks = new ArrayList<>();
        private final List<BlockDisplayHandle> icicles = new ArrayList<>();
        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private final List<BlockDisplayHandle> dust = new ArrayList<>();
        private final List<BlockDisplayHandle> anchors = new ArrayList<>();
        private boolean dissipating = false;

        public TimeFrozenObjects(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("time_frozen_objects", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(130.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(240);
            config.setCooldownTicks(180);
            config.setDamageDelayTicks(25);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 8 frozen rocks PACKED_ICE at random suspended positions
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double r = 3.0 + (i % 3) * 0.8;
                double py = 1.0 + (i % 4) * 0.8;
                Location loc = center.clone().add(Math.cos(angle) * r, py, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.5f, 0.5f, 0.5f).glow(180, 220, 255).interpolation(18, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                rocks.add(h);
                // Random tilt
                setRotation(h.entity(),
                        (float) (Math.random() * Math.PI),
                        (float) Math.random(), (float) Math.random(), (float) Math.random(), 18);
            }

            // 8 frozen icicles BLUE_ICE mid-drop
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * (i + 0.5)) / 8;
                double r = 2.5 + (i % 3) * 0.7;
                double py = 2.0 + (i % 5) * 0.6;
                Location loc = center.clone().add(Math.cos(angle) * r, py, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.2f, 0.8f, 0.2f).glow(140, 200, 255).interpolation(18, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                icicles.add(h);
                setRotation(h.entity(),
                        (float) Math.toRadians(20 + i * 7),
                        1f, 0f, (float) Math.sin(angle), 18);
            }

            // 8 frozen shards TINTED_GLASS in flight
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8 + 0.4;
                double r = 4.0 + (i % 2) * 0.6;
                double py = 0.5 + (i % 5) * 0.6;
                Location loc = center.clone().add(Math.cos(angle) * r, py, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.3f, 0.3f, 0.3f).glow(160, 210, 240).interpolation(18, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                shards.add(h);
            }

            // 8 frozen dust-clouds WHITE_CONCRETE mid-burst
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * (i + 0.3)) / 8;
                double r = 5.0 + (i % 2) * 0.7;
                double py = 0.3 + (i % 4) * 0.5;
                Location loc = center.clone().add(Math.cos(angle) * r, py, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(0.6f, 0.6f, 0.6f).glow(245, 250, 255).interpolation(18, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                dust.add(h);
            }

            // 4 anchor glyphs DIAMOND_BLOCK on ground
            double[][] anchorOffsets = {{-3.5, 0.0, -3.5}, {3.5, 0.0, -3.5}, {-3.5, 0.0, 3.5}, {3.5, 0.0, 3.5}};
            for (double[] off : anchorOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.3f, 0.1f, 0.3f).glow(160, 240, 255).interpolation(18, 0);
                spawnedEntities.add(h.entity());
                anchors.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.6f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 1.5, 0), 40, 4, 1.5, 4, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Continuous shimmer scale pulse
            if (tick > 18 && tick % 12 == 0) {
                float p = 1.0f + 0.05f * (float) Math.sin(tick * 0.18);
                for (BlockDisplayHandle h : rocks)   setScale(h.entity(), 0.5f * p, 0.5f * p, 0.5f * p, 10);
                for (BlockDisplayHandle h : icicles) setScale(h.entity(), 0.2f * p, 0.8f * p, 0.2f * p, 10);
                for (BlockDisplayHandle h : shards)  setScale(h.entity(), 0.3f * p, 0.3f * p, 0.3f * p, 10);
            }

            // SCULK_SOUL ambient
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL,
                        c.clone().add(0, 1.0, 0), 6, 4.0, 1.5, 4.0, 0.01);
            }

            // Every 60t: time briefly "unfreezes" — jolt forward 1 block then re-freeze
            if (tick > 30 && tick % 60 == 0 && !dissipating) {
                joltObjects();
                applyUnfreezeImpact(c);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.7f);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(0, 1.5, 0), 35, 4, 1.5, 4, 0.1);
            }

            // Dissipate: final 20t — all objects fall through in normal physics
            int duration = config.getDurationTicks();
            if (tick == duration - 20) {
                dissipating = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 1.2f);
            }
            if (tick >= duration - 20 && tick < duration && tick % 2 == 0) {
                int phase = tick - (duration - 20);
                float drop = -0.15f * phase;
                for (BlockDisplayHandle h : rocks)   translateBy(h.entity(), 0f, drop * 0.05f, 0f, 2);
                for (BlockDisplayHandle h : icicles) translateBy(h.entity(), 0f, drop * 0.05f, 0f, 2);
                for (BlockDisplayHandle h : shards)  translateBy(h.entity(), 0f, drop * 0.05f, 0f, 2);
                for (BlockDisplayHandle h : dust)    translateBy(h.entity(), 0f, drop * 0.05f, 0f, 2);
                c.getWorld().spawnParticle(Particle.FALLING_DUST,
                        c.clone().add(0, 1, 0), 4, 4.0, 0.4, 4.0, 0, Material.WHITE_CONCRETE.createBlockData());
            }
        }

        private void joltObjects() {
            for (BlockDisplayHandle h : rocks)   translateBy(h.entity(), 0f, 0f, 1f, 4);
            for (BlockDisplayHandle h : icicles) translateBy(h.entity(), 0f, 0f, 1f, 4);
            for (BlockDisplayHandle h : shards)  translateBy(h.entity(), 0f, 0f, 1f, 4);
            for (BlockDisplayHandle h : dust)    translateBy(h.entity(), 0f, 0f, 1f, 4);
        }

        private void applyUnfreezeImpact(Location c) {
            double radius = 9.0;
            double damage = 240.0;
            for (org.bukkit.entity.Player p : c.getWorld().getPlayers()) {
                if (isExempt(p)) continue;
                if (isInCylinderRange(p.getLocation(), c, radius)) {
                    p.damage(damage);
                    p.setNoDamageTicks(0);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new TimeFrozenObjects(plugin); }
    }

    // ================================================================
    // #63 — FROZEN GHOST ECHO
    // 41 blocks: ice-knight ghost. Body 4, head 1, arms 6, legs 6, sword 6 + 2,
    // shield 4, cape 6, trail 4 + 2 padding = 41 total.
    // FOLLOW-AI 0.09. Constant 7.5r, 5.5 hearts (110), 12t interval.
    // Sword-slash: every 50t, impact-only at radius 6.0 / 8.0 hearts (160).
    // ================================================================
    public static class FrozenGhostEcho extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> arms = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>();
        private final List<BlockDisplayHandle> sword = new ArrayList<>();
        private final List<BlockDisplayHandle> shield = new ArrayList<>();
        private final List<BlockDisplayHandle> cape = new ArrayList<>();
        private final List<BlockDisplayHandle> trail = new ArrayList<>();
        private float driftAngle = 0f;
        private float swordAngle = 0f;

        public FrozenGhostEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_ghost_echo", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(110.0);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(12);
            config.setDurationTicks(220);
            config.setCooldownTicks(170);
            config.setDamageDelayTicks(25);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.09);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 4 torso TINTED_GLASS
            double[][] torsoOffsets = {
                    {-0.3, 1.0, 0}, {0.3, 1.0, 0},
                    {-0.3, 1.6, 0}, {0.3, 1.6, 0}
            };
            for (double[] off : torsoOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.55f, 0.55f, 0.45f).glow(160, 210, 255).interpolation(8, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                body.add(h);
            }
            // 1 head BLUE_STAINED_GLASS
            {
                Location loc = center.clone().add(0, 2.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.7f, 0.7f, 0.55f).glow(140, 200, 255).interpolation(8, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                body.add(h);
            }
            // 2 arms × 3 segments = 6 BLUE_ICE
            for (int side = 0; side < 2; side++) {
                double sx = side == 0 ? -0.6 : 0.6;
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(sx, 1.5 - s * 0.35, 0.1);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    h.scale(0.25f, 0.3f, 0.25f).glow(170, 220, 255).interpolation(8, 0).brightness(0, 15);
                    spawnedEntities.add(h.entity());
                    arms.add(h);
                }
            }
            // 2 legs × 3 = 6 BLUE_ICE
            for (int side = 0; side < 2; side++) {
                double sx = side == 0 ? -0.3 : 0.3;
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(sx, 0.8 - s * 0.35, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    h.scale(0.28f, 0.32f, 0.28f).glow(170, 220, 255).interpolation(8, 0).brightness(0, 15);
                    spawnedEntities.add(h.entity());
                    legs.add(h);
                }
            }
            // Sword: 6 PACKED_ICE blade segments + 2 DIAMOND_BLOCK hilt
            for (int s = 0; s < 6; s++) {
                Location loc = center.clone().add(0.7, 1.0 + s * 0.25, 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.15f, 0.25f, 0.15f).glow(200, 240, 255).interpolation(8, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                sword.add(h);
            }
            for (int s = 0; s < 2; s++) {
                Location loc = center.clone().add(0.7, 0.8 - s * 0.15, 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(160, 240, 255).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                sword.add(h);
            }
            // Shield: 4 BLUE_ICE
            double[][] shieldOffsets = {{-0.8, 1.3, 0.4}, {-0.8, 1.6, 0.4}, {-0.8, 1.3, 0.7}, {-0.8, 1.6, 0.7}};
            for (double[] off : shieldOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.65f, 0.05f, 0.65f).glow(150, 210, 255).interpolation(8, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                shield.add(h);
            }
            // Cape: 6 TINTED_GLASS hanging
            for (int s = 0; s < 6; s++) {
                double sx = -0.3 + (s % 3) * 0.3;
                double sy = 1.6 - (s / 3) * 0.5;
                Location loc = center.clone().add(sx, sy, -0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.4f, 0.55f, 0.05f).glow(120, 180, 230).interpolation(8, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                cape.add(h);
            }
            // 4 ethereal trail CALCITE remnant + 2 padding for total 41
            for (int i = 0; i < 6; i++) {
                double angle = (2.0 * Math.PI * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 1.3, 0.1 + (i % 2) * 0.3, Math.sin(angle) * 1.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.2f, 0.15f, 0.2f).glow(220, 235, 250).interpolation(8, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                trail.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.5f);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 1.5, 0), 40, 1.2, 1.5, 1.2, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow 360 patrol rotation around spawn — rotate full body
            driftAngle += 0.018f;
            if (tick % 3 == 0) {
                for (BlockDisplayHandle h : body)   setRotation(h.entity(), driftAngle, 0f, 1f, 0f, 4);
                for (BlockDisplayHandle h : arms)   setRotation(h.entity(), driftAngle, 0f, 1f, 0f, 4);
                for (BlockDisplayHandle h : shield) setRotation(h.entity(), driftAngle, 0f, 1f, 0f, 4);
                for (BlockDisplayHandle h : cape)   setRotation(h.entity(), driftAngle, 0f, 1f, 0f, 4);
            }

            // Sword slash every 50t
            if (tick > 30 && tick % 50 == 0) {
                swordAngle = (float) Math.toRadians(120);
                for (BlockDisplayHandle h : sword) {
                    setRotation(h.entity(), swordAngle + driftAngle, 1f, 0.4f, 0f, 8);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.8f, 1.3f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 1.4, 0), 35, 2.0, 1.0, 2.0, 0.06);
                applySlash(c);
            }
            if (tick > 30 && (tick % 50) == 10) {
                // reset sword to neutral after the slash
                swordAngle = 0f;
                for (BlockDisplayHandle h : sword) {
                    setRotation(h.entity(), driftAngle, 0f, 1f, 0f, 10);
                }
            }

            // Ambient ghost particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL,
                        c.clone().add(0, 1.5, 0), 4, 0.8, 1.0, 0.8, 0.02);
                c.getWorld().spawnParticle(Particle.GLOW,
                        c.clone().add(0, 2.2, 0), 2, 0.2, 0.1, 0.2, 0.02);
            }

            // Dissipate fade upward
            int duration = config.getDurationTicks();
            if (tick >= duration - 25 && tick < duration && tick % 2 == 0) {
                int phase = tick - (duration - 25);
                float lift = 0.06f;
                for (BlockDisplayHandle h : body)   translateBy(h.entity(), 0f, lift, 0f, 2);
                for (BlockDisplayHandle h : arms)   translateBy(h.entity(), 0f, lift, 0f, 2);
                for (BlockDisplayHandle h : legs)   translateBy(h.entity(), 0f, lift, 0f, 2);
                for (BlockDisplayHandle h : sword)  translateBy(h.entity(), 0f, lift, 0f, 2);
                for (BlockDisplayHandle h : shield) translateBy(h.entity(), 0f, lift, 0f, 2);
                for (BlockDisplayHandle h : cape)   translateBy(h.entity(), 0f, lift, 0f, 2);
                for (BlockDisplayHandle h : trail)  translateBy(h.entity(), 0f, lift, 0f, 2);
                float fade = 1.0f - (phase / 25f);
                if (phase % 6 == 0) {
                    for (BlockDisplayHandle h : body)
                        setScale(h.entity(), 0.55f * fade, 0.55f * fade, 0.45f * fade, 4);
                }
            }
        }

        private void applySlash(Location c) {
            double radius = 6.0;
            double damage = 160.0;
            for (org.bukkit.entity.Player p : c.getWorld().getPlayers()) {
                if (isExempt(p)) continue;
                if (isInCylinderRange(p.getLocation(), c, radius)) {
                    p.damage(damage);
                    p.setNoDamageTicks(0);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenGhostEcho(plugin); }
    }

    // ================================================================
    // #64 — ICE MIRAGE MOUNTAIN
    // 38 blocks: 16 mountain PACKED_ICE, 8 snow-cap, 6 cliff, 4 mist-base, 4 shimmer.
    // NO follow-AI. Constant 11.0r, 5.0 hearts (100) / 14t. Rush impact at t=160:
    // radius 14.0, 13 hearts (260 dmg).
    // ================================================================
    public static class IceMirageMountain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> mountain = new ArrayList<>();
        private final List<BlockDisplayHandle> snowCap = new ArrayList<>();
        private final List<BlockDisplayHandle> cliff = new ArrayList<>();
        private final List<BlockDisplayHandle> mist = new ArrayList<>();
        private final List<BlockDisplayHandle> shimmer = new ArrayList<>();
        private boolean rushed = false;

        public IceMirageMountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mirage_mountain", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(100.0);
            config.setDamageRadius(11.0);
            config.setTicksBetweenDamage(14);
            config.setDurationTicks(220);
            config.setCooldownTicks(180);
            config.setDamageDelayTicks(30);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 16 PACKED_ICE mountain in 4-layer pyramidal arrangement
            int[] layerCounts = {6, 5, 3, 2};
            double[] layerY = {0.0, 1.6, 2.9, 3.9};
            double[] layerR = {2.6, 1.9, 1.2, 0.6};
            int idx = 0;
            for (int L = 0; L < 4; L++) {
                int n = layerCounts[L];
                for (int i = 0; i < n; i++) {
                    double angle = (2.0 * Math.PI * i) / n;
                    double px = Math.cos(angle) * layerR[L];
                    double pz = Math.sin(angle) * layerR[L];
                    Location loc = center.clone().add(px, layerY[L], pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                    h.scale(1.5f, 1.5f, 1.5f).glow(170, 220, 255).interpolation(20, 0).brightness(0, 15);
                    spawnedEntities.add(h.entity());
                    mountain.add(h);
                    idx++;
                    if (idx >= 16) break;
                }
                if (idx >= 16) break;
            }
            // 8 snow-cap WHITE_CONCRETE at peak
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double px = Math.cos(angle) * 0.7;
                double pz = Math.sin(angle) * 0.7;
                Location loc = center.clone().add(px, 4.8, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(1.0f, 0.5f, 1.0f).glow(255, 255, 255).interpolation(20, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                snowCap.add(h);
            }
            // 6 cliff BLUE_ICE
            for (int i = 0; i < 6; i++) {
                double angle = (2.0 * Math.PI * i) / 6 + 0.5;
                double px = Math.cos(angle) * 2.0;
                double pz = Math.sin(angle) * 2.0;
                Location loc = center.clone().add(px, 1.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(1.2f, 1.5f, 0.5f).glow(160, 210, 255).interpolation(20, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                cliff.add(h);
            }
            // 4 mist-base TINTED_GLASS
            double[][] mistOffsets = {{-1.5, -0.4, -1.5}, {1.5, -0.4, -1.5}, {-1.5, -0.4, 1.5}, {1.5, -0.4, 1.5}};
            for (double[] off : mistOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(1.5f, 0.5f, 1.5f).glow(120, 170, 220).interpolation(20, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                mist.add(h);
            }
            // 4 mirage-shimmer BLUE_STAINED_GLASS
            for (int i = 0; i < 4; i++) {
                double angle = (2.0 * Math.PI * i) / 4 + 0.2;
                Location loc = center.clone().add(Math.cos(angle) * 3.5, 2.5, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.3f, 0.3f, 0.3f).glow(160, 220, 255).interpolation(20, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                shimmer.add(h);
            }

            // Fade-in: start scale at 0.1 then animate up
            for (BlockDisplayHandle h : mountain) setScale(h.entity(), 0.1f, 0.1f, 0.1f, 0);
            for (BlockDisplayHandle h : snowCap)  setScale(h.entity(), 0.1f, 0.1f, 0.1f, 0);
            for (BlockDisplayHandle h : cliff)    setScale(h.entity(), 0.1f, 0.1f, 0.1f, 0);

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.8f, 0.4f);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 2, 0), 60, 4, 3, 4, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Fade-in 0..30: scale 0->1
            if (tick <= 30 && tick % 5 == 0) {
                float s = tick / 30f;
                for (BlockDisplayHandle h : mountain) setScale(h.entity(), 1.5f * s, 1.5f * s, 1.5f * s, 5);
                for (BlockDisplayHandle h : snowCap)  setScale(h.entity(), 1.0f * s, 0.5f * s, 1.0f * s, 5);
                for (BlockDisplayHandle h : cliff)    setScale(h.entity(), 1.2f * s, 1.5f * s, 0.5f * s, 5);
            }

            // Periodic shimmer scale-pulse (every 30t starting at 60)
            if (tick > 60 && tick % 30 == 0 && !rushed) {
                float p = 0.95f;
                for (BlockDisplayHandle h : mountain) setScale(h.entity(), 1.5f * p, 1.5f * p, 1.5f * p, 8);
                for (BlockDisplayHandle h : snowCap)  setScale(h.entity(), 1.0f * p, 0.5f * p, 1.0f * p, 8);
                for (BlockDisplayHandle h : cliff)    setScale(h.entity(), 1.2f * p, 1.5f * p, 0.5f * p, 8);
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.8f, 0.6f);
            }
            if (tick > 60 && tick % 30 == 8 && !rushed) {
                for (BlockDisplayHandle h : mountain) setScale(h.entity(), 1.5f, 1.5f, 1.5f, 8);
                for (BlockDisplayHandle h : snowCap)  setScale(h.entity(), 1.0f, 0.5f, 1.0f, 8);
                for (BlockDisplayHandle h : cliff)    setScale(h.entity(), 1.2f, 1.5f, 0.5f, 8);
            }

            // At t=160: rush forward by +5b in the player direction
            if (tick == 160 && !rushed) {
                rushed = true;
                org.bukkit.entity.Player p = nearestPlayer(c, 60.0);
                float dz = 5f;
                float dx = 0f;
                if (p != null) {
                    double pdx = p.getLocation().getX() - c.getX();
                    double pdz = p.getLocation().getZ() - c.getZ();
                    double mag = Math.sqrt(pdx * pdx + pdz * pdz);
                    if (mag > 0.001) {
                        dx = (float) (pdx / mag) * 5f;
                        dz = (float) (pdz / mag) * 5f;
                    }
                }
                for (BlockDisplayHandle h : mountain) translateBy(h.entity(), dx, 0f, dz, 4);
                for (BlockDisplayHandle h : snowCap)  translateBy(h.entity(), dx, 0f, dz, 4);
                for (BlockDisplayHandle h : cliff)    translateBy(h.entity(), dx, 0f, dz, 4);
                for (BlockDisplayHandle h : mist)     translateBy(h.entity(), dx, 0f, dz, 4);
                for (BlockDisplayHandle h : shimmer)  translateBy(h.entity(), dx, 0f, dz, 4);
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 2.0f, 0.5f);
                applyRushImpact(c);
                c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 2, 0), 60, 4, 2, 4, 0,
                        Material.WHITE_CONCRETE.createBlockData());
            }

            // Particles
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 2.5, 0), 4, 3.5, 2, 3.5, 0.01);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 4.5, 0), 8, 1.5, 0.6, 1.5, 0.03);
            }
        }

        private org.bukkit.entity.Player nearestPlayer(Location c, double r) {
            org.bukkit.entity.Player nearest = null;
            double best = r * r;
            for (org.bukkit.entity.Player p : c.getWorld().getPlayers()) {
                if (isExempt(p)) continue;
                double d = p.getLocation().distanceSquared(c);
                if (d < best) { best = d; nearest = p; }
            }
            return nearest;
        }

        private void applyRushImpact(Location c) {
            double radius = 14.0;
            double damage = 260.0;
            for (org.bukkit.entity.Player p : c.getWorld().getPlayers()) {
                if (isExempt(p)) continue;
                if (isInCylinderRange(p.getLocation(), c, radius)) {
                    p.damage(damage);
                    p.setNoDamageTicks(0);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceMirageMountain(plugin); }
    }

    // ================================================================
    // #65 — SLITHERING ICE FORM
    // 34 blocks: 6 main blob + 4 inner-jelly + 8 surface-bumps + 4 eyes +
    // 8 drip-tendrils + 4 surface-spikes. FOLLOW-AI 0.08.
    // Constant 7.0r / 6.0 hearts (120) / 10t.
    // ================================================================
    public static class SlitheringIceForm extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> blob = new ArrayList<>();
        private final List<BlockDisplayHandle> inner = new ArrayList<>();
        private final List<BlockDisplayHandle> bumps = new ArrayList<>();
        private final List<BlockDisplayHandle> eyes = new ArrayList<>();
        private final List<BlockDisplayHandle> drips = new ArrayList<>();
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();

        public SlitheringIceForm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("slithering_ice_form", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(120.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(220);
            config.setCooldownTicks(180);
            config.setDamageDelayTicks(20);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.08);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 6 main blob PACKED_ICE
            double[][] blobOffsets = {
                    {-0.6, 0.5, 0.0}, {0.6, 0.5, 0.0},
                    {0.0, 0.5, -0.6}, {0.0, 0.5, 0.6},
                    {-0.3, 1.0, 0.0}, {0.3, 1.0, 0.0}
            };
            for (double[] off : blobOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(1.0f, 1.0f, 1.0f).glow(160, 210, 255).interpolation(8, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                blob.add(h);
            }
            // 4 inner-jelly BLUE_ICE
            double[][] innerOffsets = {{-0.25, 0.5, -0.25}, {0.25, 0.5, -0.25}, {-0.25, 0.5, 0.25}, {0.25, 0.5, 0.25}};
            for (double[] off : innerOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.7f, 0.7f, 0.7f).glow(140, 200, 255).interpolation(8, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                inner.add(h);
            }
            // 8 bumps TINTED_GLASS
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double r = 1.0;
                Location loc = center.clone().add(Math.cos(angle) * r, 0.7 + (i % 2) * 0.4, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.4f, 0.4f, 0.4f).glow(180, 230, 255).interpolation(8, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                bumps.add(h);
            }
            // 4 eyes OBSIDIAN
            double[][] eyeOffsets = {{-0.2, 0.7, 0.3}, {0.2, 0.7, 0.3}, {-0.15, 1.1, 0.2}, {0.15, 1.1, 0.2}};
            for (double[] off : eyeOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.2f, 0.2f, 0.2f).glow(255, 0, 30).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                eyes.add(h);
            }
            // 8 drip-tendrils ICE
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8 + 0.2;
                double r = 0.8;
                Location loc = center.clone().add(Math.cos(angle) * r, 0.0, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(0.2f, 0.6f, 0.2f).glow(180, 220, 255).interpolation(8, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                drips.add(h);
            }
            // 4 spikes DIAMOND_BLOCK
            for (int i = 0; i < 4; i++) {
                double angle = (2.0 * Math.PI * i) / 4 + 0.5;
                Location loc = center.clone().add(Math.cos(angle) * 0.6, 1.3, Math.sin(angle) * 0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.15f, 0.4f, 0.15f).glow(160, 240, 255).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                spikes.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.5f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_HIT, 1.0f, 0.5f);
            w.spawnParticle(Particle.BUBBLE, center.clone().add(0, 0.7, 0), 30, 1, 0.5, 1, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Whole blob breathes (scale pulse)
            if (tick % 4 == 0) {
                float p = 1.0f + 0.1f * (float) Math.sin(tick * 0.18);
                for (BlockDisplayHandle h : blob)   setScale(h.entity(), p, p, p, 4);
                for (BlockDisplayHandle h : inner)  setScale(h.entity(), 0.7f * p, 0.7f * p, 0.7f * p, 4);
            }
            // Bumps and spikes undulate
            if (tick % 5 == 0) {
                for (int i = 0; i < bumps.size(); i++) {
                    float p = 0.4f + 0.15f * (float) Math.sin((tick + i * 6) * 0.2);
                    setScale(bumps.get(i).entity(), p, p, p, 5);
                }
            }
            // Drips stretch and reabsorb
            if (tick % 6 == 0) {
                for (int i = 0; i < drips.size(); i++) {
                    float ly = 0.5f + 0.4f * (float) Math.abs(Math.sin((tick + i * 4) * 0.15));
                    setScale(drips.get(i).entity(), 0.2f, ly, 0.2f, 6);
                }
            }
            // Particles
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.BUBBLE, c.clone().add(0, 0.7, 0), 5, 1.0, 0.5, 1.0, 0.02);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.0, 0), 3, 1.2, 0.1, 1.2, 0.02);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 0.9, 0.3), 2, 0.3, 0.2, 0.2, 0.02);
            }
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 0.8f, 0.6f);
            }

            // Dissipate: body splashes flat, evaporates
            int duration = config.getDurationTicks();
            if (tick >= duration - 22 && tick < duration && tick % 3 == 0) {
                int phase = tick - (duration - 22);
                float flat = 1.0f - (phase / 22f);
                for (BlockDisplayHandle h : blob)   setScale(h.entity(), flat, 0.3f * flat, flat, 3);
                for (BlockDisplayHandle h : inner)  setScale(h.entity(), 0.7f * flat, 0.2f * flat, 0.7f * flat, 3);
                for (BlockDisplayHandle h : bumps)  setScale(h.entity(), 0.4f * flat, 0.4f * flat, 0.4f * flat, 3);
                for (BlockDisplayHandle h : drips)  setScale(h.entity(), 0.2f * flat, 0.6f * flat, 0.2f * flat, 3);
                for (BlockDisplayHandle h : spikes) setScale(h.entity(), 0.15f * flat, 0.4f * flat, 0.15f * flat, 3);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new SlitheringIceForm(plugin); }
    }

    // ================================================================
    // #66 — DRIFTING FROST SPIRIT (Specter)
    // 35 blocks: 8 cloak + 1 skull + 2 sockets + 8 arms + 8 fingers +
    // 4 floating-bones + 4 cloak-edge. FOLLOW-AI 0.11.
    // Constant 8.0r / 6.0 hearts (120) / 10t.
    // ================================================================
    public static class DriftingFrostSpirit extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cloak = new ArrayList<>();
        private final List<BlockDisplayHandle> skullParts = new ArrayList<>();
        private final List<BlockDisplayHandle> arms = new ArrayList<>();
        private final List<BlockDisplayHandle> fingers = new ArrayList<>();
        private final List<BlockDisplayHandle> bones = new ArrayList<>();
        private final List<BlockDisplayHandle> cloakEdge = new ArrayList<>();

        public DriftingFrostSpirit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("drifting_frost_spirit", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(120.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(240);
            config.setCooldownTicks(180);
            config.setDamageDelayTicks(25);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.11);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 8 cloak TINTED_GLASS layered (tattered hood + body)
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double r = 0.5 + (i % 2) * 0.15;
                double py = 0.5 + (i / 2) * 0.45;
                Location loc = center.clone().add(Math.cos(angle) * r, py, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.6f, 0.7f, 0.4f).glow(80, 120, 200).interpolation(10, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                cloak.add(h);
            }
            // 1 skull QUARTZ_BLOCK + 2 OBSIDIAN sockets
            {
                Location loc = center.clone().add(0, 2.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.6f, 0.6f, 0.55f).glow(240, 240, 250).interpolation(10, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                skullParts.add(h);
            }
            for (int s = 0; s < 2; s++) {
                double sx = s == 0 ? -0.18 : 0.18;
                Location loc = center.clone().add(sx, 2.25, 0.25);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.15f, 0.15f, 0.05f).glow(255, 60, 80).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                skullParts.add(h);
            }
            // 2 arms × 4 BLUE_ICE bone-segments = 8
            for (int side = 0; side < 2; side++) {
                double sx = side == 0 ? -0.5 : 0.5;
                for (int s = 0; s < 4; s++) {
                    Location loc = center.clone().add(sx + s * 0.18 * (side == 0 ? -1 : 1), 1.6 - s * 0.1, 0.2 + s * 0.18);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    h.scale(0.18f, 0.18f, 0.22f).glow(160, 210, 255).interpolation(10, 0).brightness(0, 15);
                    spawnedEntities.add(h.entity());
                    arms.add(h);
                }
            }
            // 8 finger-claws PACKED_ICE (4 per hand)
            for (int side = 0; side < 2; side++) {
                double baseX = side == 0 ? -1.2 : 1.2;
                double baseZ = 0.95;
                for (int s = 0; s < 4; s++) {
                    double offX = (s - 1.5) * 0.1;
                    Location loc = center.clone().add(baseX + offX, 1.2, baseZ);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                    h.scale(0.1f, 0.15f, 0.2f).glow(220, 245, 255).interpolation(10, 0).brightness(0, 15);
                    spawnedEntities.add(h.entity());
                    fingers.add(h);
                }
            }
            // 4 floating bones ICE
            for (int i = 0; i < 4; i++) {
                double angle = (2.0 * Math.PI * i) / 4 + 0.7;
                double r = 1.8;
                Location loc = center.clone().add(Math.cos(angle) * r, 1.5 + (i % 2) * 0.6, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(0.25f, 0.4f, 0.25f).glow(200, 230, 255).interpolation(10, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                bones.add(h);
            }
            // 4 cloak edge BLUE_STAINED_GLASS frayed
            for (int i = 0; i < 4; i++) {
                double angle = (2.0 * Math.PI * i) / 4 + 0.4;
                double r = 0.9;
                Location loc = center.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.4f, 0.5f, 0.05f).glow(60, 100, 180).interpolation(10, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                cloakEdge.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.6f, 0.4f);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 1.5, 0), 50, 1.0, 1.5, 1.0, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Body floats up/down (sine)
            float bob = 0.18f * (float) Math.sin(tick * 0.12);
            if (tick % 3 == 0) {
                for (BlockDisplayHandle h : cloak)      translateBy(h.entity(), 0f, bob * 0.05f, 0f, 3);
                for (BlockDisplayHandle h : skullParts) translateBy(h.entity(), 0f, bob * 0.05f, 0f, 3);
                for (BlockDisplayHandle h : arms)       translateBy(h.entity(), 0f, bob * 0.05f, 0f, 3);
                for (BlockDisplayHandle h : fingers)    translateBy(h.entity(), 0f, bob * 0.05f, 0f, 3);
                for (BlockDisplayHandle h : bones)      translateBy(h.entity(), 0f, bob * 0.05f, 0f, 3);
            }

            // Cloak ripples (per-segment scale pulse)
            if (tick % 4 == 0) {
                for (int i = 0; i < cloak.size(); i++) {
                    float p = 1.0f + 0.1f * (float) Math.sin((tick + i * 8) * 0.2);
                    setScale(cloak.get(i).entity(), 0.6f * p, 0.7f * p, 0.4f, 4);
                }
            }

            // Reach forward toward player every 35t (arms extend)
            if (tick > 30 && tick % 35 == 0) {
                for (BlockDisplayHandle h : arms)    translateBy(h.entity(), 0f, 0f, 0.4f, 12);
                for (BlockDisplayHandle h : fingers) translateBy(h.entity(), 0f, 0f, 0.5f, 12);
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.2f, 0.5f);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 1.2, 1.2), 20, 0.6, 0.4, 0.6, 0.05);
            }
            if (tick > 30 && tick % 35 == 18) {
                for (BlockDisplayHandle h : arms)    translateBy(h.entity(), 0f, 0f, -0.4f, 12);
                for (BlockDisplayHandle h : fingers) translateBy(h.entity(), 0f, 0f, -0.5f, 12);
            }

            // Particles
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 1.5, 0), 5, 1.0, 1.0, 1.0, 0.02);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.5, 0), 3, 1.2, 0.4, 1.2, 0.02);
            }
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 2.25, 0.25), 3, 0.2, 0.1, 0.05, 0.02);
            }

            // Dissipate fade up
            int duration = config.getDurationTicks();
            if (tick >= duration - 25 && tick < duration && tick % 2 == 0) {
                for (BlockDisplayHandle h : cloak)      translateBy(h.entity(), 0f, 0.08f, 0f, 2);
                for (BlockDisplayHandle h : skullParts) translateBy(h.entity(), 0f, 0.08f, 0f, 2);
                for (BlockDisplayHandle h : arms)       translateBy(h.entity(), 0f, 0.08f, 0f, 2);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DriftingFrostSpirit(plugin); }
    }

    // ================================================================
    // #67 — CLIMBING ICE VINE
    // 40 blocks: 14 vine-stem + 12 thorn + 6 leaf + 4 root + 4 sucker.
    // FOLLOW-AI 0.10. Constant 6.5r / 5.5 hearts (110) / 10t.
    // Strike-lunge impact: radius 5.0 / 7.0 hearts (140) every 40t.
    // ================================================================
    public static class ClimbingIceVine extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stem = new ArrayList<>();
        private final List<BlockDisplayHandle> thorns = new ArrayList<>();
        private final List<BlockDisplayHandle> leaves = new ArrayList<>();
        private final List<BlockDisplayHandle> roots = new ArrayList<>();
        private final List<BlockDisplayHandle> suckers = new ArrayList<>();

        public ClimbingIceVine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("climbing_ice_vine", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(110.0);
            config.setDamageRadius(6.5);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(240);
            config.setCooldownTicks(180);
            config.setDamageDelayTicks(25);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.10);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 14 vine-stem BLUE_ICE segments forming snake-line (initial Z trail)
            for (int s = 0; s < 14; s++) {
                Location loc = center.clone().add(0, 0.3, -s * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.3f, 0.3f, 0.6f).glow(140, 200, 255).interpolation(6, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                stem.add(h);
            }
            // 12 thorn OBSIDIAN spikes along vine
            for (int i = 0; i < 12; i++) {
                int segIdx = i + 1;
                double sx = (i % 2 == 0 ? -0.25 : 0.25);
                Location loc = center.clone().add(sx, 0.35, -segIdx * 0.55);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.18f, 0.25f, 0.18f).glow(40, 0, 30).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                thorns.add(h);
            }
            // 6 leaf BLUE_STAINED_GLASS frosted
            for (int i = 0; i < 6; i++) {
                int segIdx = (i + 1) * 2;
                double sx = (i % 2 == 0 ? -0.5 : 0.5);
                Location loc = center.clone().add(sx, 0.3, -segIdx * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.4f, 0.05f, 0.5f).glow(120, 200, 255).interpolation(6, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                leaves.add(h);
            }
            // 4 root-tip PACKED_ICE at head (segment 0)
            double[][] rootOffsets = {{-0.18, 0.3, 0.35}, {0.18, 0.3, 0.35}, {-0.05, 0.55, 0.4}, {0.05, 0.1, 0.4}};
            for (double[] off : rootOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.2f, 0.2f, 0.2f).glow(200, 230, 255).interpolation(6, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                roots.add(h);
            }
            // 4 sucker-tendril CALCITE mid-body
            for (int i = 0; i < 4; i++) {
                int segIdx = 3 + i * 2;
                double sx = (i % 2 == 0 ? -0.4 : 0.4);
                Location loc = center.clone().add(sx, 0.15, -segIdx * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.15f, 0.15f, 0.15f).glow(230, 240, 250).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                suckers.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.0f, 0.6f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 0.5, -3), 40, 0.6, 0.6, 4, 0.03);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // S-curve undulation: each segment sways perpendicular to its base offset
            if (tick % 2 == 0) {
                for (int i = 0; i < stem.size(); i++) {
                    float wave = 0.4f * (float) Math.sin((tick + i * 6) * 0.18);
                    BlockDisplay e = stem.get(i).entity();
                    Transformation t = e.getTransformation();
                    Vector3f base = t.getTranslation();
                    // We can't accumulate, so reset relative to the per-segment base offset
                    e.setInterpolationDuration(2);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(wave - 0.5f, base.y, base.z),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Strike-lunge: every 40t, head-segments lunge forward 2 blocks
            if (tick > 30 && tick % 40 == 0) {
                for (int i = 0; i < 4 && i < stem.size(); i++) {
                    translateBy(stem.get(i).entity(), 0f, 0f, 2f, 6);
                }
                for (BlockDisplayHandle h : roots) translateBy(h.entity(), 0f, 0f, 2f, 6);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 1.2f);
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 0.5, 2), 25, 1.2, 0.6, 1.2, 0.05);
                applyStrikeImpact(c.clone().add(0, 0.5, 2));
            }
            if (tick > 30 && tick % 40 == 14) {
                for (int i = 0; i < 4 && i < stem.size(); i++) {
                    translateBy(stem.get(i).entity(), 0f, 0f, -2f, 6);
                }
                for (BlockDisplayHandle h : roots) translateBy(h.entity(), 0f, 0f, -2f, 6);
            }

            // Ambient particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.4, -3), 4, 0.6, 0.3, 3.0, 0.02);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 0.4, -3), 2, 0.4, 0.2, 2.5, 0.02);
            }

            // Dissipate: wilt from tail, shrink to head
            int duration = config.getDurationTicks();
            if (tick >= duration - 25 && tick < duration && tick % 2 == 0) {
                int phase = tick - (duration - 25);
                int idx = stem.size() - 1 - (phase / 2);
                if (idx >= 0 && idx < stem.size()) {
                    setScale(stem.get(idx).entity(), 0.05f, 0.05f, 0.05f, 2);
                }
                int thornIdx = thorns.size() - 1 - (phase / 2);
                if (thornIdx >= 0 && thornIdx < thorns.size()) {
                    setScale(thorns.get(thornIdx).entity(), 0.05f, 0.05f, 0.05f, 2);
                }
            }
        }

        private void applyStrikeImpact(Location impactLoc) {
            double radius = 5.0;
            double damage = 140.0;
            for (org.bukkit.entity.Player p : impactLoc.getWorld().getPlayers()) {
                if (isExempt(p)) continue;
                if (isInCylinderRange(p.getLocation(), impactLoc, radius)) {
                    p.damage(damage);
                    p.setNoDamageTicks(0);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ClimbingIceVine(plugin); }
    }

    // ================================================================
    // #68 — ROLLING ICE BOULDER
    // 36 blocks: 16 sphere core + 8 surface-spike + 8 ice-crystal cap +
    // 4 trailing-snow-puff. FOLLOW-AI 0.15 (fastest).
    // Constant 6.5r / 7.0 hearts (140) / 8t.
    // ================================================================
    public static class RollingIceBoulder extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> core = new ArrayList<>();
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private final List<BlockDisplayHandle> caps = new ArrayList<>();
        private final List<BlockDisplayHandle> trail = new ArrayList<>();
        private float rotAngle = 0f;

        public RollingIceBoulder(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rolling_ice_boulder", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(140.0);
            config.setDamageRadius(6.5);
            config.setTicksBetweenDamage(8);
            config.setDurationTicks(240);
            config.setCooldownTicks(180);
            config.setDamageDelayTicks(18);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.15);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double sphereRadius = 1.4;
            // 16 PACKED_ICE in approximate sphere (fibonacci)
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 16; i++) {
                double y = 1.0 - (2.0 * i / 15.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * radiusAtY * sphereRadius;
                double pz = Math.sin(theta) * radiusAtY * sphereRadius;
                Location loc = center.clone().add(px, y * sphereRadius + 5.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.8f, 0.8f, 0.8f).glow(170, 220, 255).interpolation(6, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                core.add(h);
            }
            // 8 OBSIDIAN spikes jutting from boulder
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8;
                double py = (i % 2 == 0 ? 0.7 : -0.7);
                double r = sphereRadius * 1.1;
                Location loc = center.clone().add(Math.cos(angle) * r, py + 5.0, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.3f, 0.4f, 0.3f).glow(40, 30, 40).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                spikes.add(h);
            }
            // 8 BLUE_ICE crystal cap surface details
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8 + 0.4;
                double py = (i % 2 == 0 ? 0.3 : -0.3);
                double r = sphereRadius * 0.9;
                Location loc = center.clone().add(Math.cos(angle) * r, py + 5.0, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.4f, 0.4f, 0.4f).glow(140, 200, 255).interpolation(6, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                caps.add(h);
            }
            // 4 trailing WHITE_CONCRETE snow puffs behind
            for (int i = 0; i < 4; i++) {
                double pz = 1.5 + i * 0.6;
                double sx = (i % 2 == 0 ? -0.4 : 0.4);
                Location loc = center.clone().add(sx, 3.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(0.6f, 0.6f, 0.6f).glow(245, 250, 255).interpolation(6, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                trail.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.5f);
            w.spawnParticle(Particle.FALLING_DUST, center.clone().add(0, 5, 0), 50, 1.5, 1.5, 1.5, 0,
                    Material.WHITE_CONCRETE.createBlockData());
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Materialize: ticks 0..18 -> boulder drops from Y+5 to Y+1
            if (tick <= 18) {
                if (tick % 3 == 0) {
                    float dropY = -4f * (tick / 18f);
                    for (BlockDisplayHandle h : core)   translateBy(h.entity(), 0f, dropY * 0.05f, 0f, 3);
                    for (BlockDisplayHandle h : spikes) translateBy(h.entity(), 0f, dropY * 0.05f, 0f, 3);
                    for (BlockDisplayHandle h : caps)   translateBy(h.entity(), 0f, dropY * 0.05f, 0f, 3);
                }
                if (tick == 18) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.4f, 0.4f);
                    c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 0.5, 0), 80, 3, 0.5, 3, 0,
                            Material.WHITE_CONCRETE.createBlockData());
                }
            }

            // X-axis rotation 15 deg/tick (only after landed)
            if (tick > 18 && tick % 1 == 0) {
                rotAngle += (float) Math.toRadians(15);
                for (BlockDisplayHandle h : core)   setRotation(h.entity(), rotAngle, 1f, 0f, 0f, 1);
                for (BlockDisplayHandle h : spikes) setRotation(h.entity(), rotAngle, 1f, 0f, 0f, 1);
                for (BlockDisplayHandle h : caps)   setRotation(h.entity(), rotAngle, 1f, 0f, 0f, 1);
            }

            // Trail snow-puff fade
            if (tick % 4 == 0) {
                for (int i = 0; i < trail.size(); i++) {
                    float p = 0.6f - 0.1f * (i % 2);
                    setScale(trail.get(i).entity(), p, p, p, 4);
                }
            }

            // Heavy trail particles
            if (tick > 18 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 0.5, 2), 8, 1.0, 0.4, 1.0, 0,
                        Material.WHITE_CONCRETE.createBlockData());
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.8, 0), 6, 1.2, 0.6, 1.2, 0.04);
            }
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 1.0, 0), 3, 1.3, 0.5, 1.3, 0.04);
            }
            // Rolling grind sound looped
            if (tick > 18 && tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.4f);
            }

            // Dissipate: shatter at end
            int duration = config.getDurationTicks();
            if (tick == duration - 20) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.6f);
                c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 1, 0), 80, 2, 1.5, 2, 0,
                        Material.WHITE_CONCRETE.createBlockData());
            }
            if (tick >= duration - 20 && tick < duration && tick % 3 == 0) {
                int phase = tick - (duration - 20);
                float s = 1.0f - (phase / 20f);
                for (BlockDisplayHandle h : core)   setScale(h.entity(), 0.8f * s, 0.8f * s, 0.8f * s, 3);
                for (BlockDisplayHandle h : spikes) setScale(h.entity(), 0.3f * s, 0.4f * s, 0.3f * s, 3);
                for (BlockDisplayHandle h : caps)   setScale(h.entity(), 0.4f * s, 0.4f * s, 0.4f * s, 3);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new RollingIceBoulder(plugin); }
    }

    // ================================================================
    // #69 — STALKING FROST SHADOW (recursive mirror room)
    // 36 blocks: 4 mirror-panels + 16 mirror-frame + 8 corner-pillar +
    // 4 mirror-crack + 4 doppelganger-fragments. FOLLOW-AI 0.09 (the trap
    // creeps toward the player after spawn).
    // Constant 6.0r / 6.0 hearts (120) / 10t. Shatter-burst: r 9.0 / 8 hearts.
    // ================================================================
    public static class StalkingFrostShadow extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> panels = new ArrayList<>();
        private final List<BlockDisplayHandle> frames = new ArrayList<>();
        private final List<BlockDisplayHandle> pillars = new ArrayList<>();
        private final List<BlockDisplayHandle> cracks = new ArrayList<>();
        private final List<BlockDisplayHandle> fragments = new ArrayList<>();
        private boolean shattered = false;

        public StalkingFrostShadow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stalking_frost_shadow", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(120.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(220);
            config.setCooldownTicks(180);
            config.setDamageDelayTicks(25);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.09);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 4 mirror panels TINTED_GLASS facing inward (N/S/E/W)
            double[][] panelOffsets = {{0, 1.5, -2.5}, {0, 1.5, 2.5}, {-2.5, 1.5, 0}, {2.5, 1.5, 0}};
            float[][] panelScales = {{2.5f, 3.0f, 0.15f}, {2.5f, 3.0f, 0.15f}, {0.15f, 3.0f, 2.5f}, {0.15f, 3.0f, 2.5f}};
            for (int i = 0; i < 4; i++) {
                double[] off = panelOffsets[i];
                float[] sc = panelScales[i];
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(sc[0], sc[1], sc[2]).glow(140, 200, 255).interpolation(10, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                panels.add(h);
            }
            // 16 SMOOTH_QUARTZ frame parts (4 per panel)
            for (int i = 0; i < 4; i++) {
                double[] off = panelOffsets[i];
                boolean ns = i < 2;
                double[][] framePts;
                if (ns) {
                    framePts = new double[][]{{-1.5, 0, 0}, {1.5, 0, 0}, {0, 1.7, 0}, {0, -1.7, 0}};
                } else {
                    framePts = new double[][]{{0, 0, -1.5}, {0, 0, 1.5}, {0, 1.7, 0}, {0, -1.7, 0}};
                }
                for (double[] fp : framePts) {
                    Location loc = center.clone().add(off[0] + fp[0], off[1] + fp[1], off[2] + fp[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                    h.scale(ns ? 0.5f : 0.3f, 0.3f, ns ? 0.3f : 0.5f).glow(230, 240, 250).interpolation(10, 0);
                    spawnedEntities.add(h.entity());
                    frames.add(h);
                }
            }
            // 8 corner pillars PACKED_ICE
            double[][] pillarOffsets = {
                    {-2.5, 0, -2.5}, {2.5, 0, -2.5}, {-2.5, 0, 2.5}, {2.5, 0, 2.5},
                    {-2.5, 3, -2.5}, {2.5, 3, -2.5}, {-2.5, 3, 2.5}, {2.5, 3, 2.5}
            };
            for (double[] off : pillarOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.4f, 1.8f, 0.4f).glow(170, 220, 255).interpolation(10, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                pillars.add(h);
            }
            // 4 cracks BLUE_STAINED_GLASS
            for (int i = 0; i < 4; i++) {
                double[] off = panelOffsets[i];
                Location loc = center.clone().add(off[0] * 0.85, off[1] + 0.3, off[2] * 0.85);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.15f, 0.8f, 0.15f).glow(100, 180, 255).interpolation(10, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                cracks.add(h);
            }
            // 4 reflected-doppelganger fragments DIAMOND_BLOCK
            for (int i = 0; i < 4; i++) {
                double[] off = panelOffsets[i];
                Location loc = center.clone().add(off[0] * 0.6, off[1] + 0.3, off[2] * 0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.2f, 0.2f, 0.2f).glow(160, 240, 255).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                fragments.add(h);
            }

            // Setup: panels slide in from outside
            for (BlockDisplayHandle h : panels) translateBy(h.entity(), 0f, 0f, 0f, 0);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.5f);
            w.spawnParticle(Particle.GLOW, center.clone().add(0, 1.5, 0), 50, 2, 1.5, 2, 0.04);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pulse vibrate panels
            if (tick > 25 && tick % 6 == 0 && !shattered) {
                float p = 1.0f + 0.04f * (float) Math.sin(tick * 0.3);
                for (int i = 0; i < panels.size(); i++) {
                    boolean ns = i < 2;
                    setScale(panels.get(i).entity(),
                            (ns ? 2.5f : 0.15f) * p, 3.0f * p, (ns ? 0.15f : 2.5f) * p, 4);
                }
            }
            // Every 40t one new crack extension (re-grow cracks)
            if (tick > 30 && tick % 40 == 0 && !shattered) {
                int crackIdx = ((tick - 30) / 40) % cracks.size();
                if (crackIdx < cracks.size()) {
                    setScale(cracks.get(crackIdx).entity(), 0.3f, 1.4f, 0.3f, 8);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.3f, 1.4f);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            c.clone().add(0, 1.5, 0), 18, 2.0, 1.2, 2.0, 0.06);
                }
            }

            // Particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 1.5, 0), 6, 2.0, 1.0, 2.0, 0.02);
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 1.5, 0), 4, 2.2, 1.2, 2.2, 0.02);
            }

            // At t=180: all mirrors shatter outward
            if (tick == 180 && !shattered) {
                shattered = true;
                applyShatterBurst(c);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.7f);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 1.5, 0), 80, 3, 1.5, 3, 0.2);
                // Push panels outward + scale down
                for (int i = 0; i < panels.size(); i++) {
                    double[] off = {0, 0, 0};
                    if (i == 0) off[2] = -2f;
                    else if (i == 1) off[2] = 2f;
                    else if (i == 2) off[0] = -2f;
                    else off[0] = 2f;
                    translateBy(panels.get(i).entity(), (float) off[0], 1f, (float) off[2], 25);
                }
            }
            if (shattered && tick > 180 && tick % 4 == 0) {
                int phase = tick - 180;
                float s = Math.max(0.05f, 1.0f - (phase / 40f));
                for (int i = 0; i < panels.size(); i++) {
                    boolean ns = i < 2;
                    setScale(panels.get(i).entity(),
                            (ns ? 2.5f : 0.15f) * s, 3.0f * s, (ns ? 0.15f : 2.5f) * s, 4);
                }
            }
        }

        private void applyShatterBurst(Location c) {
            double radius = 9.0;
            double damage = 160.0;
            for (org.bukkit.entity.Player p : c.getWorld().getPlayers()) {
                if (isExempt(p)) continue;
                if (isInCylinderRange(p.getLocation(), c, radius)) {
                    p.damage(damage);
                    p.setNoDamageTicks(0);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new StalkingFrostShadow(plugin); }
    }

    // ================================================================
    // #70 — PURSUING ICE WOLVES (drifting frostbite fog)
    // 36 blocks: 24 POWDER_SNOW fog body + 6 BLUE_ICE inner cores +
    // 4 CALCITE frost-flame accents + 2 TINTED_GLASS wisp-arms.
    // FOLLOW-AI 0.07 (slowest — fog drifts toward warmth).
    // Constant 8.5r / 5.5 hearts (110) / 10t.
    // ================================================================
    public static class PursuingIceWolves extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> fog = new ArrayList<>();
        private final List<BlockDisplayHandle> cores = new ArrayList<>();
        private final List<BlockDisplayHandle> flames = new ArrayList<>();
        private final List<BlockDisplayHandle> wisps = new ArrayList<>();

        public PursuingIceWolves(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pursuing_ice_wolves", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(110.0);
            config.setDamageRadius(8.5);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(220);
            config.setCooldownTicks(180);
            config.setDamageDelayTicks(25);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.07);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 24 POWDER_SNOW fog blocks in lumpy cluster (radius 4)
            for (int i = 0; i < 24; i++) {
                double angle = (2.0 * Math.PI * i) / 24 + (i % 3) * 0.1;
                double r = 2.0 + (i % 4) * 0.7;
                double py = 0.4 + (i % 5) * 0.5;
                Location loc = center.clone().add(Math.cos(angle) * r, py, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POWDER_SNOW);
                float s = 0.8f + (i % 4) * 0.1f;
                h.scale(s, s, s).glow(220, 240, 255).interpolation(12, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                fog.add(h);
            }
            // 6 BLUE_ICE inner cores
            for (int i = 0; i < 6; i++) {
                double angle = (2.0 * Math.PI * i) / 6;
                double r = 1.5;
                Location loc = center.clone().add(Math.cos(angle) * r, 1.0 + (i % 2) * 0.5, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.4f, 0.4f, 0.4f).glow(150, 210, 255).interpolation(12, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                cores.add(h);
            }
            // 4 CALCITE frost-flame accents
            for (int i = 0; i < 4; i++) {
                double angle = (2.0 * Math.PI * i) / 4 + 0.5;
                Location loc = center.clone().add(Math.cos(angle) * 1.2, 1.6, Math.sin(angle) * 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.3f, 0.5f, 0.3f).glow(180, 220, 255).interpolation(12, 0);
                spawnedEntities.add(h.entity());
                flames.add(h);
            }
            // 2 TINTED_GLASS wisp-arms trailing
            for (int i = 0; i < 2; i++) {
                double angle = (i == 0 ? Math.PI : 0);
                Location loc = center.clone().add(Math.cos(angle) * 4, 0.8, Math.sin(angle) * 4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.5f, 0.5f, 1.6f).glow(180, 220, 255).interpolation(12, 0).brightness(0, 15);
                spawnedEntities.add(h.entity());
                wisps.add(h);
            }

            // Spawn FX
            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.6f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_HIT, 0.8f, 0.4f);
            w.spawnParticle(Particle.CLOUD, center.clone().add(0, 1, 0), 80, 3, 1.5, 3, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Internal cores pulse-bright
            if (tick % 5 == 0) {
                float p = 1.0f + 0.2f * (float) Math.sin(tick * 0.15);
                for (BlockDisplayHandle h : cores) setScale(h.entity(), 0.4f * p, 0.4f * p, 0.4f * p, 5);
            }

            // Periodic thicken every 50t (scale +20% for 10t)
            if (tick > 30 && tick % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.2f, 0.6f);
                for (BlockDisplayHandle h : fog) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    setScale(e, s.x * 1.2f, s.y * 1.2f, s.z * 1.2f, 6);
                }
                c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 1, 0), 40, 4, 1.5, 4, 0.05);
            }
            if (tick > 30 && tick % 50 == 10) {
                for (BlockDisplayHandle h : fog) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f s = t.getScale();
                    setScale(e, s.x / 1.2f, s.y / 1.2f, s.z / 1.2f, 6);
                }
            }

            // Wisp arms gentle rotation
            if (tick % 4 == 0) {
                float a = tick * 0.04f;
                for (int i = 0; i < wisps.size(); i++) {
                    setRotation(wisps.get(i).entity(), a + (i * (float) Math.PI), 0f, 1f, 0f, 4);
                }
            }

            // Particles
            if (tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 1, 0), 10, 3.5, 1.4, 3.5, 0.02);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.6, 0), 8, 3.5, 1.0, 3.5, 0.03);
            }
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, c.clone().add(0, 1, 0), 6, 3.5, 1.0, 3.5, 0.05);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 1.4, 0), 3, 1.4, 0.6, 1.4, 0.02);
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.9f, 0.6f);
            }

            // Dissipate: fog fades outward, scale -> 0
            int duration = config.getDurationTicks();
            if (tick >= duration - 25 && tick < duration && tick % 3 == 0) {
                int phase = tick - (duration - 25);
                float s = 1.0f - (phase / 25f);
                for (BlockDisplayHandle h : fog)    setScale(h.entity(), 0.9f * s, 0.9f * s, 0.9f * s, 3);
                for (BlockDisplayHandle h : cores)  setScale(h.entity(), 0.4f * s, 0.4f * s, 0.4f * s, 3);
                for (BlockDisplayHandle h : flames) setScale(h.entity(), 0.3f * s, 0.5f * s, 0.3f * s, 3);
                for (BlockDisplayHandle h : wisps)  setScale(h.entity(), 0.5f * s, 0.5f * s, 1.6f * s, 3);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PursuingIceWolves(plugin); }
    }
}
