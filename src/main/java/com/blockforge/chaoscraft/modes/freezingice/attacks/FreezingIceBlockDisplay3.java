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
 * FreezingIce Mode — BLOCK DISPLAY ATTACKS 21-30 (Architecture theme).
 *
 * Massive frozen architectural structures rendered as recognizable shapes.
 * Cathedrals look like cathedrals; lighthouses look like lighthouses.
 * Each attack uses 30+ block displays with smooth multi-phase interpolated
 * animation (rise → idle/active → collapse).
 *
 * Palette:
 *  - PACKED_ICE, BLUE_ICE, ICE, FROSTED_ICE, SNOW_BLOCK
 *  - TINTED_GLASS, BLUE_STAINED_GLASS
 *  - LIGHT_BLUE_CONCRETE, CYAN_CONCRETE, BLUE_CONCRETE, WHITE_CONCRETE, WHITE_WOOL
 *  - AMETHYST_BLOCK, CALCITE, QUARTZ_BLOCK, SMOOTH_QUARTZ, DIAMOND_BLOCK
 *
 * Particles: SNOWFLAKE, ELECTRIC_SPARK, GLOW, SCULK_SOUL, ENCHANT, CLOUD, FALLING_DUST
 * Sounds:    BLOCK_GLASS_BREAK, BLOCK_GLASS_PLACE, BLOCK_AMETHYST_BLOCK_CHIME,
 *            ITEM_TRIDENT_THROW, ENTITY_GLOW_SQUID_AMBIENT, ITEM_ARMOR_EQUIP_IRON
 *
 * Attacks (none use follow-AI):
 * 21. IceCathedral             — Gothic cathedral with spires, rose window, buttresses (45 blocks)
 * 22. GlassLabyrinth           — Spiral maze of glass walls + pillars (41 blocks)
 * 23. FrozenLighthouse         — Tall tower w/ rotating beacon, 4 light shafts (30 blocks)
 * 24. IcePalaceTower           — Fairy-tale castle keep w/ 4 corner spires (44 blocks)
 * 25. GlacierBridgeArch        — Mid-air arch bridge that cracks and collapses (34 blocks)
 * 26. FrostGazebo              — Hex gazebo with altar, pillars, hanging lanterns (42 blocks)
 * 27. CrystalCathedralWindow   — Triumphal arch w/ swirling portal disc + icicle bolts (33 blocks)
 * 28. IceMonastery             — Stepped pyramid tomb that opens periodically (35 blocks)
 * 29. FrozenColosseum          — Round arena ruin w/ falling columns (41 blocks)
 * 30. CrystallineObservatory   — Domed observatory + tracking telescope beam (32 blocks)
 */
public final class FreezingIceBlockDisplay3 {
    private FreezingIceBlockDisplay3() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceCathedral(plugin));
        registry.register(new GlassLabyrinth(plugin));
        registry.register(new FrozenLighthouse(plugin));
        registry.register(new IcePalaceTower(plugin));
        registry.register(new GlacierBridgeArch(plugin));
        registry.register(new FrostGazebo(plugin));
        registry.register(new CrystalCathedralWindow(plugin));
        registry.register(new IceMonastery(plugin));
        registry.register(new FrozenColosseum(plugin));
        registry.register(new CrystallineObservatory(plugin));
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
    // Shared helpers
    // ================================================================
    private static void interp(BlockDisplay e, Vector3f translate, AxisAngle4f rot, Vector3f scale, int duration) {
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                translate != null ? translate : t.getTranslation(),
                rot != null ? rot : new AxisAngle4f().set(t.getLeftRotation()),
                scale != null ? scale : t.getScale(),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    private static void setScale(BlockDisplay e, float sx, float sy, float sz, int duration) {
        interp(e, null, null, new Vector3f(sx, sy, sz), duration);
    }

    private static void setTranslate(BlockDisplay e, float x, float y, float z, int duration) {
        interp(e, new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f), null, null, duration);
    }

    private static void setRotation(BlockDisplay e, float angle, float ax, float ay, float az, int duration) {
        interp(e, null, new AxisAngle4f(angle, ax, ay, az), null, duration);
    }

    // ================================================================
    // #21 — ICE CATHEDRAL (gothic cathedral)
    // 45 blocks: 2 spires (6 ea = 12), 8 wall sections, 1 rose window + 6 spokes,
    // 4 doorway arch, 4 buttresses, 4 cross-finial, 4 gargoyles, 2 base pillars.
    // ================================================================
    public static class IceCathedral extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spireLeft = new ArrayList<>();
        private final List<BlockDisplayHandle> spireRight = new ArrayList<>();
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<BlockDisplayHandle> roseSpokes = new ArrayList<>();
        private BlockDisplayHandle roseWindow;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private float roseAngle = 0f;

        public IceCathedral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_cathedral", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(132.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(290);
            config.setCooldownTicks(60);
        }

        private BlockDisplayHandle place(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b, int interp) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(0.001f, 0.001f, 0.001f).glow(r, g, b).interpolation(interp, 0);
            spawnedEntities.add(h.entity());
            all.add(h);
            setScale(h.entity(), sx, sy, sz, interp);
            return h;
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 2 spires — tapering 1.0 -> 0.2, 6 blocks each
            for (int s = 0; s < 6; s++) {
                float sc = 1.0f - s * 0.13f;
                spireLeft.add(place(center.clone().add(-2.5, 4 + s * 0.9, 0), Material.BLUE_ICE,
                        sc, 0.9f, sc, 140, 200, 240, 14));
                spireRight.add(place(center.clone().add(2.5, 4 + s * 0.9, 0), Material.BLUE_ICE,
                        sc, 0.9f, sc, 140, 200, 240, 14));
            }
            // 8 main hall PACKED_ICE walls (1.5 x 3.0 x 1.0)
            double[][] wallOffsets = {
                    {-1.6, 1.5, -1.0}, {0.0, 1.5, -1.0}, {1.6, 1.5, -1.0},
                    {-1.6, 1.5,  1.0}, {0.0, 1.5,  1.0}, {1.6, 1.5,  1.0},
                    {-2.4, 1.5, 0.0}, {2.4, 1.5, 0.0}
            };
            for (double[] o : wallOffsets) {
                walls.add(place(center.clone().add(o[0], o[1], o[2]), Material.PACKED_ICE,
                        1.5f, 3.0f, 1.0f, 160, 210, 240, 16));
            }
            // Rose window — central TINTED_GLASS disc
            roseWindow = place(center.clone().add(0, 3.5, -1.05), Material.TINTED_GLASS,
                    2.0f, 2.0f, 0.2f, 100, 140, 200, 18);
            // 6 spoke segments — BLUE_STAINED_GLASS
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 * i) / 6;
                double dx = Math.cos(a) * 0.9;
                double dy = Math.sin(a) * 0.9;
                BlockDisplayHandle sp = place(center.clone().add(dx, 3.5 + dy, -1.0), Material.BLUE_STAINED_GLASS,
                        0.45f, 0.18f, 0.2f, 80, 170, 230, 18);
                BlockDisplay e = sp.entity();
                interp(e, null, new AxisAngle4f((float) a, 0f, 0f, 1f), new Vector3f(0.45f, 0.18f, 0.2f), 18);
                roseSpokes.add(sp);
            }
            // Arched doorway — 4 QUARTZ_BLOCK
            double[][] door = {{-0.4, 0.4, 1.05}, {0.4, 0.4, 1.05}, {-0.3, 1.3, 1.05}, {0.3, 1.3, 1.05}};
            for (double[] o : door) {
                place(center.clone().add(o[0], o[1], o[2]), Material.QUARTZ_BLOCK,
                        0.8f, 0.8f, 0.2f, 235, 240, 245, 16);
            }
            // 4 buttress walls 0.8 x 2.5 x 0.5
            double[][] butt = {{-3.0, 1.4, -0.8}, {-3.0, 1.4, 0.8}, {3.0, 1.4, -0.8}, {3.0, 1.4, 0.8}};
            for (double[] o : butt) {
                place(center.clone().add(o[0], o[1], o[2]), Material.SMOOTH_QUARTZ,
                        0.8f, 2.5f, 0.5f, 220, 230, 245, 18);
            }
            // Cross-finial — 4 DIAMOND_BLOCK atop main spire
            double[][] cross = {{0, 10.2, 0}, {0, 10.7, 0}, {-0.4, 10.5, 0}, {0.4, 10.5, 0}};
            for (double[] o : cross) {
                place(center.clone().add(o[0], o[1], o[2]), Material.DIAMOND_BLOCK,
                        0.3f, 0.3f, 0.3f, 200, 240, 255, 22);
            }
            // 4 gargoyle CALCITE 0.5
            double[][] garg = {{-2.0, 4.5, -1.4}, {2.0, 4.5, -1.4}, {-2.0, 4.5, 1.4}, {2.0, 4.5, 1.4}};
            for (double[] o : garg) {
                place(center.clone().add(o[0], o[1], o[2]), Material.CALCITE,
                        0.5f, 0.5f, 0.5f, 230, 240, 245, 20);
            }
            // 2 base pillar fillers
            place(center.clone().add(-2.5, 1.5, 0), Material.PACKED_ICE, 0.6f, 2.8f, 0.6f, 160, 210, 240, 18);
            place(center.clone().add(2.5, 1.5, 0), Material.PACKED_ICE, 0.6f, 2.8f, 0.6f, 160, 210, 240, 18);

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.4f, 0.6f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 6, 0), 40, 3, 3, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Bell-toll pulse every 60t
            if (tick > 40 && tick < 240 && tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.6f, 0.5f);
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 3.5, 0), 40, 2, 2, 2, 0.05);
                // Whole-structure pulse
                for (BlockDisplayHandle h : all) {
                    BlockDisplay e = h.entity();
                    Vector3f s = e.getTransformation().getScale();
                    setScale(e, s.x * 1.08f, s.y * 1.04f, s.z * 1.08f, 8);
                }
            } else if (tick > 40 && tick < 240 && tick % 60 == 12) {
                // Settle back from pulse
                rebuildOriginalScale();
            }

            // Rose window rotates Y-axis 10°/tick
            if (roseWindow != null && tick > 35 && tick < 250 && tick % 2 == 0) {
                roseAngle += 0.175f;
                setRotation(roseWindow.entity(), roseAngle, 0f, 0f, 1f, 2);
            }

            // Spires sway slight Y +-0.15
            if (tick > 35 && tick < 250 && tick % 8 == 0) {
                float sway = (float) Math.sin(tick * 0.05) * 0.15f;
                for (int i = 0; i < spireLeft.size(); i++) {
                    BlockDisplay e = spireLeft.get(i).entity();
                    setTranslate(e, 0f, sway * (i / 5f), 0f, 8);
                }
                for (int i = 0; i < spireRight.size(); i++) {
                    BlockDisplay e = spireRight.get(i).entity();
                    setTranslate(e, 0f, -sway * (i / 5f), 0f, 8);
                }
            }

            // Ambient particles
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 8, 0), 4, 2, 1.5, 2, 0.02);
            }
            if (tick % 14 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 4, 0), 2, 1.5, 1.5, 1.5, 0.01);
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 0.5f, 0.5f);
            }

            // Collapse phase: ticks 250-285 — spires topple inward, walls fall
            if (tick >= 250 && tick < 285) {
                if (tick == 250) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.4f);
                if (tick % 5 == 0) {
                    float dropY = -(tick - 250) * 0.3f;
                    float tilt = (float) Math.toRadians((tick - 250) * 1.0);
                    for (int i = 0; i < spireLeft.size(); i++) {
                        BlockDisplay e = spireLeft.get(i).entity();
                        interp(e, new Vector3f(-0.5f, dropY - 0.5f, -0.5f),
                                new AxisAngle4f(tilt, 0f, 0f, 1f), null, 5);
                    }
                    for (int i = 0; i < spireRight.size(); i++) {
                        BlockDisplay e = spireRight.get(i).entity();
                        interp(e, new Vector3f(-0.5f, dropY - 0.5f, -0.5f),
                                new AxisAngle4f(-tilt, 0f, 0f, 1f), null, 5);
                    }
                    for (BlockDisplayHandle h : walls) {
                        BlockDisplay e = h.entity();
                        Vector3f s = e.getTransformation().getScale();
                        setScale(e, s.x * 0.85f, s.y * 0.85f, s.z * 0.85f, 5);
                    }
                    c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 3, 0),
                            12, 3, 2, 3, 0, Material.PACKED_ICE.createBlockData());
                }
            }
        }

        private void rebuildOriginalScale() {
            // Approximate: shrink back by inverse
            for (BlockDisplayHandle h : all) {
                BlockDisplay e = h.entity();
                Vector3f s = e.getTransformation().getScale();
                setScale(e, s.x / 1.08f, s.y / 1.04f, s.z / 1.08f, 8);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceCathedral(plugin); }
    }

    // ================================================================
    // #22 — GLASS LABYRINTH (spiral maze)
    // 41 blocks: 30 wall segments in spiral, 4 corner pillars, 3 arch, 4 floor tiles.
    // Constant 9.0r 6.0 hearts 10t interval 30t delay.
    // ================================================================
    public static class GlassLabyrinth extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<BlockDisplayHandle> pillars = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public GlassLabyrinth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glass_labyrinth", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(144.0);
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(255);
            config.setCooldownTicks(60);
        }

        private BlockDisplayHandle place(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(0.001f, 0.001f, 0.001f).glow(r, g, b).interpolation(14, 0);
            spawnedEntities.add(h.entity());
            all.add(h);
            setScale(h.entity(), sx, sy, sz, 14);
            return h;
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 30 spiral wall segments — radius 3 -> 8
            for (int i = 0; i < 30; i++) {
                double t = i / 29.0;
                double r = 3.0 + t * 5.0;
                double a = t * Math.PI * 4.2;
                double px = Math.cos(a) * r;
                double pz = Math.sin(a) * r;
                Location loc = center.clone().add(px, 1.0, pz);
                BlockDisplayHandle h = place(loc, Material.TINTED_GLASS, 0.5f, 2.0f, 1.0f, 90, 150, 210);
                // Rotate wall to face tangent
                BlockDisplay e = h.entity();
                interp(e, null, new AxisAngle4f((float) (a + Math.PI / 2), 0f, 1f, 0f),
                        new Vector3f(0.5f, 2.0f, 1.0f), 14);
                walls.add(h);
                DisplayBuilder.playSound(loc, Sound.BLOCK_GLASS_PLACE, 0.4f, 1.0f + (float) (i / 30f));
            }
            // 4 corner pillars — PACKED_ICE 0.6 x 2.5 x 0.6 at radius 9
            double[][] corners = {{-6, 1.3, -6}, {6, 1.3, -6}, {-6, 1.3, 6}, {6, 1.3, 6}};
            for (double[] o : corners) {
                pillars.add(place(center.clone().add(o[0], o[1], o[2]), Material.PACKED_ICE,
                        0.6f, 2.5f, 0.6f, 140, 200, 240));
            }
            // 3 entry-arch BLUE_ICE 0.8
            place(center.clone().add(-1.0, 0.5, -2.8), Material.BLUE_ICE, 0.8f, 0.8f, 0.8f, 100, 180, 230);
            place(center.clone().add(1.0, 0.5, -2.8), Material.BLUE_ICE, 0.8f, 0.8f, 0.8f, 100, 180, 230);
            place(center.clone().add(0, 1.5, -2.8), Material.BLUE_ICE, 1.8f, 0.5f, 0.5f, 100, 180, 230);
            // 4 BLUE_STAINED_GLASS floor tiles
            double[][] floor = {{-2, 0.05, 0}, {2, 0.05, 0}, {0, 0.05, -2}, {0, 0.05, 2}};
            for (double[] o : floor) {
                place(center.clone().add(o[0], o[1], o[2]), Material.BLUE_STAINED_GLASS,
                        1.0f, 0.1f, 1.0f, 80, 160, 220);
            }

            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1, 0), 30, 5, 1, 5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Periodic wall "lock" pulse — random wall scales 1.0 -> 1.1
            if (tick > 35 && tick < 235 && tick % 25 == 0 && !walls.isEmpty()) {
                int idx = (tick / 25) % walls.size();
                BlockDisplayHandle h = walls.get(idx);
                BlockDisplay e = h.entity();
                setScale(e, 0.55f, 2.2f, 1.1f, 4);
                c.getWorld().spawnParticle(Particle.GLOW, e.getLocation().add(0, 1.0, 0), 6, 0.3, 0.5, 0.3, 0.05);
                DisplayBuilder.playSound(e.getLocation(), Sound.BLOCK_GLASS_PLACE, 0.7f, 1.4f);
            }
            if (tick > 35 && tick < 235 && tick % 25 == 8 && !walls.isEmpty()) {
                int idx = (tick / 25) % walls.size();
                BlockDisplayHandle h = walls.get(idx);
                setScale(h.entity(), 0.5f, 2.0f, 1.0f, 4);
            }
            // Wall-top GLOW + drifting SNOWFLAKE
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 2.5, 0), 4, 4, 0.2, 4, 0.02);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2, 0), 6, 4, 1, 4, 0.03);
            }
            // Shatter at tick 235-250
            if (tick >= 235 && tick < 250) {
                if (tick == 235) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.7f);
                if (tick % 3 == 0) {
                    for (BlockDisplayHandle h : all) {
                        BlockDisplay e = h.entity();
                        Vector3f s = e.getTransformation().getScale();
                        setScale(e, s.x * 0.75f, s.y * 0.75f, s.z * 0.75f, 3);
                    }
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1.5, 0), 30, 6, 2, 6, 0.1);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GlassLabyrinth(plugin); }
    }

    // ================================================================
    // #23 — FROZEN LIGHTHOUSE
    // 30 blocks: 10 stacked tower cylinder, 4 observation hex, 3 cone roof,
    // 1 beacon + 4 light shafts, 4 base, 4 railing.
    // ================================================================
    public static class FrozenLighthouse extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> tower = new ArrayList<>();
        private final List<BlockDisplayHandle> roof = new ArrayList<>();
        private final List<BlockDisplayHandle> beams = new ArrayList<>();
        private BlockDisplayHandle beacon;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private float beaconAngle = 0f;

        public FrozenLighthouse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_lighthouse", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(120.0);
            config.setDamageRadius(16.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(320);
            config.setCooldownTicks(60);
        }

        private BlockDisplayHandle place(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b, int interp) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(0.001f, 0.001f, 0.001f).glow(r, g, b).interpolation(interp, 0);
            spawnedEntities.add(h.entity());
            all.add(h);
            setScale(h.entity(), sx, sy, sz, interp);
            return h;
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 4 base PACKED_ICE 1.8 wide
            double[][] base = {{-1.0, 0.5, -1.0}, {1.0, 0.5, -1.0}, {-1.0, 0.5, 1.0}, {1.0, 0.5, 1.0}};
            for (double[] o : base) {
                place(center.clone().add(o[0], o[1], o[2]), Material.PACKED_ICE,
                        1.8f, 0.8f, 1.8f, 140, 200, 240, 18);
            }
            // 10 tower BLUE_ICE 1.5 stacked
            for (int s = 0; s < 10; s++) {
                tower.add(place(center.clone().add(0, 1.5 + s * 1.5, 0), Material.BLUE_ICE,
                        1.5f, 1.5f, 1.5f, 130, 190, 235, 16));
            }
            // 4 TINTED_GLASS observation room hex (square pattern)
            double[][] obs = {{-0.7, 16.0, -0.7}, {0.7, 16.0, -0.7}, {-0.7, 16.0, 0.7}, {0.7, 16.0, 0.7}};
            for (double[] o : obs) {
                place(center.clone().add(o[0], o[1], o[2]), Material.TINTED_GLASS,
                        1.2f, 1.2f, 1.2f, 100, 150, 200, 18);
            }
            // 3 conical roof QUARTZ tapered 0.8 -> 0.2
            roof.add(place(center.clone().add(0, 17.2, 0), Material.QUARTZ_BLOCK, 1.5f, 0.6f, 1.5f, 235, 240, 245, 20));
            roof.add(place(center.clone().add(0, 17.8, 0), Material.QUARTZ_BLOCK, 1.0f, 0.6f, 1.0f, 235, 240, 245, 20));
            roof.add(place(center.clone().add(0, 18.4, 0), Material.QUARTZ_BLOCK, 0.5f, 0.6f, 0.5f, 235, 240, 245, 20));
            // Beacon DIAMOND_BLOCK
            beacon = place(center.clone().add(0, 16.6, 0), Material.DIAMOND_BLOCK,
                    0.6f, 0.6f, 0.6f, 200, 240, 255, 20);
            // 4 GLOW beam shafts BLUE_STAINED_GLASS 0.15 x 0.15 x 4.0
            double[][] beamOff = {{2.5, 16.6, 0}, {-2.5, 16.6, 0}, {0, 16.6, 2.5}, {0, 16.6, -2.5}};
            for (int i = 0; i < beamOff.length; i++) {
                double[] o = beamOff[i];
                BlockDisplayHandle h = place(center.clone().add(o[0], o[1], o[2]), Material.BLUE_STAINED_GLASS,
                        0.15f, 0.15f, 4.0f, 150, 220, 255, 18);
                // rotate beam to point outward
                BlockDisplay e = h.entity();
                float angle = (float) (i * Math.PI / 2);
                interp(e, null, new AxisAngle4f(angle, 0f, 1f, 0f), new Vector3f(0.15f, 0.15f, 4.0f), 18);
                beams.add(h);
            }
            // 4 railing SMOOTH_QUARTZ
            double[][] rail = {{1.0, 15.5, 0}, {-1.0, 15.5, 0}, {0, 15.5, 1.0}, {0, 15.5, -1.0}};
            for (double[] o : rail) {
                place(center.clone().add(o[0], o[1], o[2]), Material.SMOOTH_QUARTZ,
                        1.0f, 0.2f, 0.2f, 220, 230, 245, 18);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.5f, 0.5f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 8, 0), 40, 2, 6, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Beacon rotates Y-axis ~4°/tick (0.07 rad/tick)
            if (tick > 30 && tick < 280 && tick % 2 == 0) {
                beaconAngle += 0.14f;
                if (beacon != null) setRotation(beacon.entity(), beaconAngle, 0f, 1f, 0f, 2);
                // Rotate beam shafts around the beacon
                for (int i = 0; i < beams.size(); i++) {
                    BlockDisplayHandle h = beams.get(i);
                    BlockDisplay e = h.entity();
                    float a = beaconAngle + (float) (i * Math.PI / 2);
                    double bx = Math.cos(a) * 2.5;
                    double bz = Math.sin(a) * 2.5;
                    interp(e, new Vector3f((float) bx - 0.5f, 16.6f - 0.5f, (float) bz - 0.5f),
                            new AxisAngle4f(a, 0f, 1f, 0f), new Vector3f(0.15f, 0.15f, 4.0f), 2);
                }
            }
            // Beacon glow particles continuous
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 16.6, 0), 6, 1, 0.3, 1, 0.05);
            }
            // Sweep ELECTRIC_SPARK along beam paths
            if (tick % 5 == 0) {
                for (int i = 0; i < 4; i++) {
                    float a = beaconAngle + (float) (i * Math.PI / 2);
                    for (int d = 1; d <= 4; d++) {
                        double bx = Math.cos(a) * 2.5 + Math.cos(a) * d;
                        double bz = Math.sin(a) * 2.5 + Math.sin(a) * d;
                        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(bx, 16.6, bz), 2, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
            // Beacon chime
            if (tick > 30 && tick < 280 && tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 0.5f);
            }
            if (tick > 30 && tick < 280 && tick % 70 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.0f, 0.4f);
            }

            // Collapse 280-310 — top falls first then tower buckles
            if (tick >= 280 && tick < 310) {
                if (tick == 280) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.8f, 0.4f);
                if (tick % 4 == 0) {
                    float dropY = -(tick - 280) * 0.5f;
                    float tilt = (float) Math.toRadians((tick - 280) * 1.2);
                    for (int i = tower.size() - 1; i >= 0; i--) {
                        BlockDisplay e = tower.get(i).entity();
                        interp(e, new Vector3f(-0.5f, dropY * (i / 9f) - 0.5f, -0.5f),
                                new AxisAngle4f(tilt * (i / 9f), 0f, 0f, 1f), null, 4);
                    }
                    for (BlockDisplayHandle h : roof) {
                        BlockDisplay e = h.entity();
                        Vector3f s = e.getTransformation().getScale();
                        setScale(e, s.x * 0.8f, s.y * 0.8f, s.z * 0.8f, 4);
                    }
                    c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 10, 0),
                            12, 2, 5, 2, 0, Material.BLUE_ICE.createBlockData());
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenLighthouse(plugin); }
    }

    // ================================================================
    // #24 — ICE PALACE TOWER (fairy-tale castle keep)
    // 44 blocks: 4 corner spires (5 each = 20), 6 central keep cylinder,
    // 8 battlements, 4 windows, 4 banners, 2 gate arch.
    // ================================================================
    public static class IcePalaceTower extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> spires = new ArrayList<>();
        private final List<BlockDisplayHandle> banners = new ArrayList<>();
        private final List<BlockDisplayHandle> windows = new ArrayList<>();
        private final List<BlockDisplayHandle> keep = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IcePalaceTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_palace_tower", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(144.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(300);
            config.setCooldownTicks(60);
        }

        private BlockDisplayHandle place(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b, int interp) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(0.001f, 0.001f, 0.001f).glow(r, g, b).interpolation(interp, 0);
            spawnedEntities.add(h.entity());
            all.add(h);
            setScale(h.entity(), sx, sy, sz, interp);
            return h;
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 6 central keep PACKED_ICE 1.5 cylinder
            for (int s = 0; s < 6; s++) {
                keep.add(place(center.clone().add(0, 1.5 + s * 1.5, 0), Material.PACKED_ICE,
                        2.5f, 1.5f, 2.5f, 150, 210, 240, 18));
            }
            // 4 corner spires — telescoping, BLUE_ICE 5 each
            double[][] corners = {{-2.5, 0, -2.5}, {2.5, 0, -2.5}, {-2.5, 0, 2.5}, {2.5, 0, 2.5}};
            for (double[] cn : corners) {
                List<BlockDisplayHandle> col = new ArrayList<>();
                for (int s = 0; s < 5; s++) {
                    float sc = 0.9f - s * 0.12f;
                    col.add(place(center.clone().add(cn[0], 2.0 + s * 1.4, cn[2]), Material.BLUE_ICE,
                            sc, 1.4f, sc, 130, 190, 235, 20));
                }
                spires.add(col);
            }
            // 8 battlement crenellations QUARTZ_BLOCK 0.5x0.5x0.4 atop central keep
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 * i) / 8;
                double px = Math.cos(a) * 1.6;
                double pz = Math.sin(a) * 1.6;
                place(center.clone().add(px, 10.3, pz), Material.QUARTZ_BLOCK,
                        0.5f, 0.5f, 0.4f, 235, 240, 245, 20);
            }
            // 4 arched window slits BLUE_STAINED_GLASS
            double[][] win = {{1.3, 5.5, 0}, {-1.3, 5.5, 0}, {0, 5.5, 1.3}, {0, 5.5, -1.3}};
            for (double[] o : win) {
                windows.add(place(center.clone().add(o[0], o[1], o[2]), Material.BLUE_STAINED_GLASS,
                        0.4f, 1.0f, 0.4f, 80, 150, 220, 18));
            }
            // 4 banner flags atop spires
            for (double[] cn : corners) {
                banners.add(place(center.clone().add(cn[0] + 0.3, 9.5, cn[2]), Material.BLUE_ICE,
                        0.2f, 0.8f, 0.4f, 100, 180, 240, 20));
            }
            // 2 gate arch SMOOTH_QUARTZ
            place(center.clone().add(0, 0.8, 2.8), Material.SMOOTH_QUARTZ, 2.2f, 0.4f, 0.5f, 230, 240, 245, 18);
            place(center.clone().add(0, 1.6, 2.8), Material.SMOOTH_QUARTZ, 1.8f, 0.4f, 0.5f, 230, 240, 245, 18);

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.5f, 0.5f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 6, 0), 50, 3, 4, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Banner sway
            if (tick > 40 && tick < 260 && tick % 4 == 0) {
                float sway = (float) Math.toRadians(20 * Math.sin(tick * 0.08));
                for (BlockDisplayHandle h : banners) {
                    setRotation(h.entity(), sway, 0f, 1f, 0f, 4);
                }
            }
            // Window glow pulse
            if (tick > 40 && tick < 260 && tick % 30 == 0) {
                for (BlockDisplayHandle h : windows) {
                    setScale(h.entity(), 0.45f, 1.15f, 0.45f, 8);
                    c.getWorld().spawnParticle(Particle.GLOW, h.entity().getLocation().add(0, 0.5, 0), 6, 0.3, 0.5, 0.3, 0.05);
                }
            }
            if (tick > 40 && tick < 260 && tick % 30 == 12) {
                for (BlockDisplayHandle h : windows) {
                    setScale(h.entity(), 0.4f, 1.0f, 0.4f, 8);
                }
            }
            // Ambient SNOWFLAKE around spires
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 8, 0), 6, 3, 2, 3, 0.03);
            }
            // ELECTRIC_SPARK banners
            if (tick % 12 == 0) {
                for (BlockDisplayHandle h : banners) {
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, h.entity().getLocation(), 4, 0.3, 0.5, 0.3, 0.02);
                }
            }

            // Collapse: spires topple in sequence (1-2-3-4) 260-295
            if (tick >= 260 && tick < 295) {
                if (tick == 260) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.5f);
                int spireIdx = (tick - 260) / 8;
                if (spireIdx < spires.size() && (tick - 260) % 8 == 0) {
                    List<BlockDisplayHandle> sp = spires.get(spireIdx);
                    for (int i = 0; i < sp.size(); i++) {
                        BlockDisplay e = sp.get(i).entity();
                        float tilt = (float) Math.toRadians(60);
                        interp(e, new Vector3f(-0.5f, -2f - i * 0.3f - 0.5f, -0.5f),
                                new AxisAngle4f(tilt, 1f, 0f, 0.5f), null, 8);
                    }
                    c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 5, 0),
                            15, 3, 3, 3, 0, Material.BLUE_ICE.createBlockData());
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.6f);
                }
                if (tick == 290) {
                    for (BlockDisplayHandle h : keep) {
                        BlockDisplay e = h.entity();
                        Vector3f s = e.getTransformation().getScale();
                        setScale(e, s.x * 0.6f, s.y * 0.6f, s.z * 0.6f, 5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IcePalaceTower(plugin); }
    }

    // ================================================================
    // #25 — GLACIER BRIDGE ARCH (mid-air bridge over chasm)
    // 34 blocks: 14 arch arc, 8 roadway deck, 6 supports, 4 railings, 2 abutments.
    // Impact-only on collapse.
    // ================================================================
    public static class GlacierBridgeArch extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> arch = new ArrayList<>();
        private final List<BlockDisplayHandle> deck = new ArrayList<>();
        private final List<BlockDisplayHandle> supports = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private boolean collapsed = false;

        public GlacierBridgeArch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacier_bridge_arch", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(2640.0);
            config.setImpactRadius(16.5);
            config.setDamage(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(60);
        }

        private BlockDisplayHandle place(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b, int interp) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(0.001f, 0.001f, 0.001f).glow(r, g, b).interpolation(interp, 0);
            spawnedEntities.add(h.entity());
            all.add(h);
            setScale(h.entity(), sx, sy, sz, interp);
            return h;
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double yBase = 5.0;
            // 14 arch BLUE_ICE forming arc radius 6
            for (int i = 0; i < 14; i++) {
                double t = (i - 6.5) / 6.5; // -1..1
                double px = t * 7.0;
                double py = yBase + Math.sqrt(Math.max(0, 1 - t * t)) * 3.0;
                arch.add(place(center.clone().add(px, py, 0), Material.BLUE_ICE,
                        1.0f, 1.0f, 1.0f, 130, 190, 235, 18));
            }
            // 8 PACKED_ICE roadway deck across top
            for (int i = 0; i < 8; i++) {
                double t = (i - 3.5) / 3.5;
                double px = t * 6.0;
                deck.add(place(center.clone().add(px, yBase + 3.2, 0), Material.PACKED_ICE,
                        1.5f, 0.4f, 1.5f, 150, 210, 240, 20));
            }
            // 6 SMOOTH_QUARTZ supports hanging under arch
            for (int i = 0; i < 6; i++) {
                double t = (i - 2.5) / 2.5;
                double px = t * 5.0;
                double py = yBase + Math.sqrt(Math.max(0, 1 - t * t)) * 3.0 - 1.5;
                supports.add(place(center.clone().add(px, py, 0), Material.SMOOTH_QUARTZ,
                        0.6f, 2.0f, 0.6f, 220, 230, 245, 20));
            }
            // 4 TINTED_GLASS railings
            double[][] rails = {{-3, yBase + 3.6, 0.7}, {3, yBase + 3.6, 0.7}, {-3, yBase + 3.6, -0.7}, {3, yBase + 3.6, -0.7}};
            for (double[] o : rails) {
                place(center.clone().add(o[0], o[1], o[2]), Material.TINTED_GLASS,
                        0.2f, 0.6f, 3.0f, 90, 150, 210, 18);
            }
            // 2 abutment QUARTZ at ends
            place(center.clone().add(-7.5, yBase, 0), Material.QUARTZ_BLOCK, 1.5f, 1.5f, 1.5f, 235, 240, 245, 18);
            place(center.clone().add(7.5, yBase, 0), Material.QUARTZ_BLOCK, 1.5f, 1.5f, 1.5f, 235, 240, 245, 18);

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.5f, 0.6f);
            w.spawnParticle(Particle.CLOUD, center.clone().add(0, yBase + 2, 0), 40, 7, 1, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slight Y sway during active
            if (tick > 30 && tick < 80 && tick % 5 == 0) {
                float sway = (float) Math.sin(tick * 0.1) * 0.3f;
                for (BlockDisplayHandle h : deck) setTranslate(h.entity(), 0f, sway, 0f, 5);
            }
            // FALLING_DUST from underside
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 5, 0),
                        8, 5, 1, 1, 0, Material.WHITE_CONCRETE.createBlockData());
                c.getWorld().spawnParticle(Particle.CLOUD, c.clone().add(0, 7, 0), 4, 5, 1, 1, 0.02);
            }
            // Tick 80 — central deck cracks (4 center blocks fall)
            if (tick == 80) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.7f);
                for (int i = 2; i < 6 && i < deck.size(); i++) {
                    BlockDisplay e = deck.get(i).entity();
                    setTranslate(e, 0f, -8f, 0f, 20);
                }
            }
            // Collapse at tick 110 — whole bridge buckles
            if (tick == 110 && !collapsed) {
                collapsed = true;
                Location impact = c.clone().add(0, 2, 0);
                triggerImpactDamage(impact);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.4f);
                for (int i = 0; i < 8; i++) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f + i * 0.05f);
                c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 6, 0),
                        80, 7, 3, 3, 0, Material.PACKED_ICE.createBlockData());
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 6, 0), 80, 7, 3, 3, 0.1);
            }
            // Pivot pieces down after collapse
            if (tick > 110 && tick < 135 && tick % 3 == 0) {
                float drop = -(tick - 110) * 0.4f;
                float tilt = (float) Math.toRadians((tick - 110) * 1.5);
                for (int i = 0; i < arch.size(); i++) {
                    BlockDisplay e = arch.get(i).entity();
                    int sign = (i < arch.size() / 2) ? -1 : 1;
                    interp(e, new Vector3f(-0.5f, drop - 0.5f, -0.5f),
                            new AxisAngle4f(tilt * sign, 0f, 0f, 1f), null, 3);
                }
                for (BlockDisplayHandle h : supports) {
                    BlockDisplay e = h.entity();
                    Vector3f s = e.getTransformation().getScale();
                    setScale(e, s.x * 0.8f, s.y * 0.8f, s.z * 0.8f, 3);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GlacierBridgeArch(plugin); }
    }

    // ================================================================
    // #26 — FROST GAZEBO (open hexagonal shrine)
    // 42 blocks: 6 pillars x 4 = 24, 6 roof panels, 1 altar + 4 base, 6 lanterns.
    // Constant 8.0r 5.5 hearts 12t interval. Shockwave pulses every 60t.
    // ================================================================
    public static class FrostGazebo extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pillars = new ArrayList<>();
        private final List<BlockDisplayHandle> roofPanels = new ArrayList<>();
        private final List<BlockDisplayHandle> lanterns = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private BlockDisplayHandle altar;

        public FrostGazebo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_gazebo", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(132.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(290);
            config.setCooldownTicks(60);
        }

        private BlockDisplayHandle place(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b, int interp) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(0.001f, 0.001f, 0.001f).glow(r, g, b).interpolation(interp, 0);
            spawnedEntities.add(h.entity());
            all.add(h);
            setScale(h.entity(), sx, sy, sz, interp);
            return h;
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double radius = 4.0;
            // 6 pillars × 4 stacked = 24 PACKED_ICE
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 * i) / 6;
                double px = Math.cos(a) * radius;
                double pz = Math.sin(a) * radius;
                for (int s = 0; s < 4; s++) {
                    pillars.add(place(center.clone().add(px, 1.0 + s * 1.2, pz), Material.PACKED_ICE,
                            0.7f, 1.2f, 0.7f, 150, 210, 240, 16));
                }
            }
            // 6 BLUE_ICE roof triangular panels — start folded up, then fold in
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 * i) / 6;
                double px = Math.cos(a) * (radius * 0.7);
                double pz = Math.sin(a) * (radius * 0.7);
                BlockDisplayHandle h = place(center.clone().add(px, 5.2, pz), Material.BLUE_ICE,
                        1.5f, 0.2f, 1.5f, 130, 190, 235, 20);
                BlockDisplay e = h.entity();
                interp(e, null, new AxisAngle4f((float) a, 0f, 1f, 0f), new Vector3f(1.5f, 0.2f, 1.5f), 20);
                roofPanels.add(h);
            }
            // Central altar AMETHYST_BLOCK + 4 CALCITE base
            altar = place(center.clone().add(0, 1.2, 0), Material.AMETHYST_BLOCK,
                    0.8f, 0.8f, 0.8f, 200, 140, 255, 22);
            double[][] alt = {{-0.5, 0.6, -0.5}, {0.5, 0.6, -0.5}, {-0.5, 0.6, 0.5}, {0.5, 0.6, 0.5}};
            for (double[] o : alt) {
                place(center.clone().add(o[0], o[1], o[2]), Material.CALCITE,
                        0.4f, 0.4f, 0.4f, 230, 240, 245, 22);
            }
            // 6 hanging lanterns BLUE_STAINED_GLASS between pillars
            for (int i = 0; i < 6; i++) {
                double a = ((Math.PI * 2 * i) / 6) + (Math.PI / 6);
                double px = Math.cos(a) * radius * 0.85;
                double pz = Math.sin(a) * radius * 0.85;
                lanterns.add(place(center.clone().add(px, 4.0, pz), Material.BLUE_STAINED_GLASS,
                        0.3f, 0.3f, 0.3f, 100, 180, 240, 24));
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.4f, 0.5f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 3, 0), 40, 3, 2, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Altar pulse (scale 0.8 <-> 1.0)
            if (altar != null && tick > 30 && tick < 270 && tick % 20 == 0) {
                setScale(altar.entity(), 1.0f, 1.0f, 1.0f, 10);
                c.getWorld().spawnParticle(Particle.GLOW, altar.entity().getLocation().add(0, 0.4, 0), 12, 0.6, 0.6, 0.6, 0.05);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
            }
            if (altar != null && tick > 30 && tick < 270 && tick % 20 == 10) {
                setScale(altar.entity(), 0.8f, 0.8f, 0.8f, 10);
            }
            // Lanterns sway
            if (tick > 30 && tick < 270 && tick % 5 == 0) {
                float sway = (float) Math.sin(tick * 0.07) * 0.2f;
                for (BlockDisplayHandle h : lanterns) {
                    setTranslate(h.entity(), 0f, sway, 0f, 5);
                }
                if (tick % 25 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.2f);
            }
            // Shockwave from altar every 60t
            if (tick > 30 && tick < 270 && tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.6f, 0.5f);
                for (int r = 1; r <= 13; r++) {
                    double rad = r;
                    for (int i = 0; i < 20; i++) {
                        double a = (Math.PI * 2 * i) / 20;
                        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                                c.clone().add(Math.cos(a) * rad, 1.0, Math.sin(a) * rad), 1, 0.05, 0.05, 0.05, 0.02);
                    }
                }
                // Bonus damage outward shockwave
                Player p = findNearestPlayer(c, 13.0);
                if (p != null) p.damage(6.0);
            }

            // Collapse 270-300 — lanterns fall, then pillars topple outward
            if (tick >= 270 && tick < 300) {
                if (tick == 270) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
                if (tick == 272) {
                    for (BlockDisplayHandle h : lanterns) {
                        setTranslate(h.entity(), 0f, -4f, 0f, 16);
                    }
                }
                if (tick > 280 && tick % 4 == 0) {
                    int p = ((tick - 280) / 4) % 6;
                    double a = (Math.PI * 2 * p) / 6;
                    for (int s = 0; s < 4; s++) {
                        int idx = p * 4 + s;
                        if (idx < pillars.size()) {
                            BlockDisplay e = pillars.get(idx).entity();
                            interp(e, new Vector3f((float) (Math.cos(a) * 2) - 0.5f, -1.5f - 0.5f, (float) (Math.sin(a) * 2) - 0.5f),
                                    new AxisAngle4f((float) Math.toRadians(60), (float) -Math.sin(a), 0f, (float) Math.cos(a)), null, 4);
                        }
                    }
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.7f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostGazebo(plugin); }
    }

    // ================================================================
    // #27 — CRYSTAL CATHEDRAL WINDOW (triumphal arch portal)
    // 33 blocks: 2 side pillars (6 ea = 12), 8 arch keystone curve,
    // 1 portal disc, 4 outer frame, 8 relief carvings.
    // Constant + impact bolts every 40t.
    // ================================================================
    public static class CrystalCathedralWindow extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pillars = new ArrayList<>();
        private final List<BlockDisplayHandle> archCurve = new ArrayList<>();
        private BlockDisplayHandle portalDisc;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private float discAngle = 0f;

        public CrystalCathedralWindow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_cathedral_window", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(120.0);
            config.setDamageRadius(10.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(290);
            config.setCooldownTicks(60);
        }

        private BlockDisplayHandle place(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b, int interp) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(0.001f, 0.001f, 0.001f).glow(r, g, b).interpolation(interp, 0);
            spawnedEntities.add(h.entity());
            all.add(h);
            setScale(h.entity(), sx, sy, sz, interp);
            return h;
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 2 side pillars — 6 PACKED_ICE each stacked
            for (int s = 0; s < 6; s++) {
                pillars.add(place(center.clone().add(-2.5, 0.8 + s * 1.5, 0), Material.PACKED_ICE,
                        1.0f, 1.5f, 1.0f, 150, 210, 240, 16));
                pillars.add(place(center.clone().add(2.5, 0.8 + s * 1.5, 0), Material.PACKED_ICE,
                        1.0f, 1.5f, 1.0f, 150, 210, 240, 16));
            }
            // 8 BLUE_ICE arch curve atop pillars
            for (int i = 0; i < 8; i++) {
                double t = (i - 3.5) / 3.5;
                double px = t * 2.5;
                double py = 10.0 + Math.sqrt(Math.max(0, 1 - t * t)) * 1.5;
                BlockDisplayHandle h = place(center.clone().add(px, py, 0), Material.BLUE_ICE,
                        0.8f, 0.8f, 0.8f, 130, 190, 235, 18);
                archCurve.add(h);
            }
            // Portal disc BLUE_STAINED_GLASS 2.5
            portalDisc = place(center.clone().add(0, 5.5, 0), Material.BLUE_STAINED_GLASS,
                    2.5f, 2.5f, 0.3f, 80, 160, 230, 22);
            // 4 outer frame DIAMOND_BLOCK
            double[][] frame = {{-2.0, 5.5, 0.1}, {2.0, 5.5, 0.1}, {0, 7.5, 0.1}, {0, 3.5, 0.1}};
            for (double[] o : frame) {
                place(center.clone().add(o[0], o[1], o[2]), Material.DIAMOND_BLOCK,
                        0.4f, 0.4f, 0.4f, 200, 240, 255, 20);
            }
            // 8 relief CALCITE
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 * i) / 8;
                double px = Math.cos(a) * 1.6;
                double py = Math.sin(a) * 1.6 + 5.5;
                place(center.clone().add(px, py, 0.2), Material.CALCITE,
                        0.4f, 0.4f, 0.2f, 230, 240, 245, 20);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.2f, 0.4f);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 5.5, 0), 25, 1.5, 1.5, 1.5, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Portal disc swirls
            if (portalDisc != null && tick > 30 && tick < 270 && tick % 2 == 0) {
                discAngle += 0.07f;
                setRotation(portalDisc.entity(), discAngle, 0f, 0f, 1f, 2);
            }
            // SCULK_SOUL swirl + ELECTRIC_SPARK ring
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 5.5, 0), 4, 0.8, 0.8, 0.3, 0.02);
            }
            if (tick % 4 == 0) {
                for (int i = 0; i < 16; i++) {
                    double a = (Math.PI * 2 * i) / 16 + tick * 0.05;
                    double px = Math.cos(a) * 1.3;
                    double py = Math.sin(a) * 1.3 + 5.5;
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(px, py, 0), 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
            if (tick % 50 == 0 && tick > 30 && tick < 270) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.3f, 0.4f);
            }

            // Icicle-bolt every 40t
            if (tick > 50 && tick < 260 && tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.3f, 1.2f);
                Player target = findNearestPlayer(c, 20.0);
                Location boltTarget = target != null ? target.getLocation() : c.clone().add(0, 1, 5);
                Location boltStart = c.clone().add(0, 5.5, 0);
                // Trail particle line
                for (int d = 0; d < 14; d++) {
                    double t = d / 14.0;
                    Location lp = boltStart.clone().add(
                            (boltTarget.getX() - boltStart.getX()) * t,
                            (boltTarget.getY() - boltStart.getY()) * t,
                            (boltTarget.getZ() - boltStart.getZ()) * t);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, lp, 4, 0.1, 0.1, 0.1, 0.01);
                }
                // Impact at target
                for (Player p : c.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;
                    if (p.getLocation().distance(boltTarget) <= 5.0) {
                        p.damage(16.0);
                    }
                }
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, boltTarget, 30, 2, 1, 2, 0.1);
            }

            // Portal closes (scale -> 0), arch crumbles 270-295
            if (tick >= 270 && tick < 295) {
                if (tick == 270) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.4f);
                if (tick == 271 && portalDisc != null) {
                    setScale(portalDisc.entity(), 0.01f, 0.01f, 0.01f, 14);
                }
                if (tick % 4 == 0) {
                    for (BlockDisplayHandle h : archCurve) {
                        BlockDisplay e = h.entity();
                        Vector3f s = e.getTransformation().getScale();
                        setScale(e, s.x * 0.8f, s.y * 0.8f, s.z * 0.8f, 4);
                    }
                    for (int i = 0; i < pillars.size(); i++) {
                        BlockDisplay e = pillars.get(i).entity();
                        setTranslate(e, 0f, -(tick - 270) * 0.2f, 0f, 4);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalCathedralWindow(plugin); }
    }

    // ================================================================
    // #28 — ICE MONASTERY (stepped pyramid tomb)
    // 35 blocks: 9 layer-1 PACKED_ICE, 5 layer-2 BLUE_ICE cross, 4 layer-3 DIAMOND_BLOCK,
    // 1 capstone AMETHYST_BLOCK, 8 corner spikes, 4 inscription glyphs, 4 hanging corners.
    // ================================================================
    public static class IceMonastery extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> layer1 = new ArrayList<>();
        private final List<BlockDisplayHandle> layer2 = new ArrayList<>();
        private final List<BlockDisplayHandle> layer3 = new ArrayList<>();
        private BlockDisplayHandle capstone;
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private float capRotation = 0f;

        public IceMonastery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_monastery", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(120.0);
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(270);
            config.setCooldownTicks(60);
        }

        private BlockDisplayHandle place(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b, int interp) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(0.001f, 0.001f, 0.001f).glow(r, g, b).interpolation(interp, 0);
            spawnedEntities.add(h.entity());
            all.add(h);
            setScale(h.entity(), sx, sy, sz, interp);
            return h;
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Layer 1: 9 PACKED_ICE 1.5 (3x3)
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    layer1.add(place(center.clone().add(x * 1.5, 0.8, z * 1.5), Material.PACKED_ICE,
                            1.5f, 1.5f, 1.5f, 150, 210, 240, 18));
                }
            }
            // Layer 2: 5 BLUE_ICE cross
            double[][] l2 = {{0, 2.3, 0}, {-1.3, 2.3, 0}, {1.3, 2.3, 0}, {0, 2.3, -1.3}, {0, 2.3, 1.3}};
            for (double[] o : l2) {
                layer2.add(place(center.clone().add(o[0], o[1], o[2]), Material.BLUE_ICE,
                        1.3f, 1.3f, 1.3f, 130, 190, 235, 20));
            }
            // Layer 3: 4 DIAMOND_BLOCK
            double[][] l3 = {{-0.6, 3.7, -0.6}, {0.6, 3.7, -0.6}, {-0.6, 3.7, 0.6}, {0.6, 3.7, 0.6}};
            for (double[] o : l3) {
                layer3.add(place(center.clone().add(o[0], o[1], o[2]), Material.DIAMOND_BLOCK,
                        1.0f, 1.0f, 1.0f, 200, 240, 255, 22));
            }
            // Capstone AMETHYST_BLOCK
            capstone = place(center.clone().add(0, 4.9, 0), Material.AMETHYST_BLOCK,
                    0.8f, 0.8f, 0.8f, 200, 140, 255, 24);
            // 8 corner spikes OBSIDIAN
            double[][] spikes = {{-2.5, 0.3, -2.5}, {2.5, 0.3, -2.5}, {-2.5, 0.3, 2.5}, {2.5, 0.3, 2.5},
                    {-2.5, 0.3, 0}, {2.5, 0.3, 0}, {0, 0.3, -2.5}, {0, 0.3, 2.5}};
            for (double[] o : spikes) {
                place(center.clone().add(o[0], o[1], o[2]), Material.OBSIDIAN,
                        0.3f, 0.8f, 0.3f, 30, 20, 50, 20);
            }
            // 4 inscription glyph CALCITE face-plates
            double[][] glyphs = {{-1.6, 1.5, 0}, {1.6, 1.5, 0}, {0, 1.5, -1.6}, {0, 1.5, 1.6}};
            for (double[] o : glyphs) {
                place(center.clone().add(o[0], o[1], o[2]), Material.CALCITE,
                        0.4f, 0.4f, 0.2f, 230, 240, 245, 22);
            }
            // 4 hanging corners LIGHT_BLUE_CONCRETE
            double[][] hang = {{-1.5, 0.8, -1.5}, {1.5, 0.8, -1.5}, {-1.5, 0.8, 1.5}, {1.5, 0.8, 1.5}};
            for (double[] o : hang) {
                place(center.clone().add(o[0], o[1], o[2]), Material.LIGHT_BLUE_CONCRETE,
                        0.6f, 0.4f, 0.6f, 120, 200, 240, 22);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.4f, 0.4f);
            w.spawnParticle(Particle.SCULK_SOUL, center.clone().add(0, 3, 0), 20, 2, 2, 2, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Capstone hovers + rotates
            if (capstone != null && tick > 30 && tick < 240) {
                if (tick % 2 == 0) {
                    capRotation += 0.06f;
                    float hover = (float) Math.sin(tick * 0.08) * 0.2f;
                    interp(capstone.entity(), new Vector3f(-0.5f, hover - 0.5f, -0.5f),
                            new AxisAngle4f(capRotation, 0f, 1f, 0f), new Vector3f(0.8f, 0.8f, 0.8f), 2);
                }
            }
            // GLOW + SCULK_SOUL ambient
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 4.9, 0), 4, 0.5, 0.5, 0.5, 0.02);
            }
            if (tick % 10 == 0) {
                c.getWorld().spawnParticle(Particle.SCULK_SOUL, c.clone().add(0, 3, 0), 2, 1.5, 1, 1.5, 0.01);
            }

            // Open pulse every 60t — top 3 layers split outward then reclose
            if (tick > 40 && tick < 230 && tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.5f);
                // Split outward
                for (int i = 0; i < layer3.size(); i++) {
                    BlockDisplay e = layer3.get(i).entity();
                    double a = (Math.PI * 2 * i) / 4 + Math.PI / 4;
                    setTranslate(e, (float) (Math.cos(a) * 1.0), 0f, (float) (Math.sin(a) * 1.0), 6);
                }
                for (int i = 0; i < layer2.size(); i++) {
                    BlockDisplay e = layer2.get(i).entity();
                    if (i == 0) continue; // center
                    double a = (Math.PI * 2 * (i - 1)) / 4;
                    setTranslate(e, (float) (Math.cos(a) * 0.8), 0f, (float) (Math.sin(a) * 0.8), 6);
                }
                // ELECTRIC_SPARK burst
                for (int r = 1; r <= 14; r++) {
                    for (int i = 0; i < 16; i++) {
                        double a = (Math.PI * 2 * i) / 16;
                        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                                c.clone().add(Math.cos(a) * r, 4.0, Math.sin(a) * r), 1, 0.05, 0.05, 0.05, 0.02);
                    }
                }
                // Burst damage
                Player p = findNearestPlayer(c, 14.0);
                if (p != null) p.damage(12.0);
            }
            // Reclose 30t later
            if (tick > 40 && tick < 230 && tick % 60 == 30) {
                for (BlockDisplayHandle h : layer3) setTranslate(h.entity(), 0f, 0f, 0f, 8);
                for (BlockDisplayHandle h : layer2) setTranslate(h.entity(), 0f, 0f, 0f, 8);
            }

            // Collapse 240-265 — layers explode upward then crash down
            if (tick == 240) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.7f, 0.4f);
                for (BlockDisplayHandle h : all) {
                    setTranslate(h.entity(), 0f, 3f, 0f, 8);
                }
            }
            if (tick == 255) {
                for (BlockDisplayHandle h : all) {
                    BlockDisplay e = h.entity();
                    Vector3f s = e.getTransformation().getScale();
                    interp(e, new Vector3f(-0.5f, -3f - 0.5f, -0.5f), null,
                            new Vector3f(s.x * 0.5f, s.y * 0.5f, s.z * 0.5f), 10);
                }
                c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 3, 0),
                        50, 3, 3, 3, 0, Material.PACKED_ICE.createBlockData());
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceMonastery(plugin); }
    }

    // ================================================================
    // #29 — FROZEN COLOSSEUM (round arena ruin)
    // 41 blocks: 16 outer columns, 8 inner arches, 1 altar + 4 base CALCITE,
    // 8 floor tiles, 4 broken fragments.
    // ================================================================
    public static class FrozenColosseum extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> columns = new ArrayList<>();
        private final List<BlockDisplayHandle> arches = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private BlockDisplayHandle altar;
        private final java.util.Set<Integer> toppled = new java.util.HashSet<>();

        public FrozenColosseum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_colosseum", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(120.0);
            config.setDamageRadius(16.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(310);
            config.setCooldownTicks(60);
        }

        private BlockDisplayHandle place(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b, int interp) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(0.001f, 0.001f, 0.001f).glow(r, g, b).interpolation(interp, 0);
            spawnedEntities.add(h.entity());
            all.add(h);
            setScale(h.entity(), sx, sy, sz, interp);
            return h;
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 16 PACKED_ICE columns at radius 8
            double outerR = 8.0;
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2 * i) / 16;
                double px = Math.cos(a) * outerR;
                double pz = Math.sin(a) * outerR;
                BlockDisplayHandle h = place(center.clone().add(px, 1.5, pz), Material.PACKED_ICE,
                        0.8f, 2.5f, 0.8f, 150, 210, 240, 18);
                // Pre-topple a couple for ruin look
                if (i == 2 || i == 9) {
                    BlockDisplay e = h.entity();
                    interp(e, new Vector3f(-0.5f, -1f - 0.5f, -0.5f),
                            new AxisAngle4f((float) Math.toRadians(75), (float) -Math.sin(a), 0f, (float) Math.cos(a)),
                            new Vector3f(0.8f, 2.5f, 0.8f), 18);
                }
                columns.add(h);
            }
            // 8 BLUE_ICE inner arches at radius 4
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 * i) / 8;
                double px = Math.cos(a) * 4.0;
                double pz = Math.sin(a) * 4.0;
                BlockDisplayHandle h = place(center.clone().add(px, 2.0, pz), Material.BLUE_ICE,
                        1.2f, 1.2f, 1.2f, 130, 190, 235, 20);
                BlockDisplay e = h.entity();
                interp(e, null, new AxisAngle4f((float) a, 0f, 1f, 0f), new Vector3f(1.2f, 1.2f, 1.2f), 20);
                arches.add(h);
            }
            // Central altar OBSIDIAN + 4 CALCITE base
            altar = place(center.clone().add(0, 1.0, 0), Material.OBSIDIAN,
                    1.0f, 1.0f, 1.0f, 60, 40, 80, 24);
            double[][] alt = {{-0.7, 0.4, -0.7}, {0.7, 0.4, -0.7}, {-0.7, 0.4, 0.7}, {0.7, 0.4, 0.7}};
            for (double[] o : alt) {
                place(center.clone().add(o[0], o[1], o[2]), Material.CALCITE,
                        0.5f, 0.5f, 0.5f, 230, 240, 245, 24);
            }
            // 8 BLUE_STAINED_GLASS floor tiles
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 * i) / 8 + Math.PI / 8;
                double px = Math.cos(a) * 5.5;
                double pz = Math.sin(a) * 5.5;
                place(center.clone().add(px, 0.05, pz), Material.BLUE_STAINED_GLASS,
                        1.2f, 0.1f, 1.2f, 80, 160, 220, 22);
            }
            // 4 broken fragments ICE
            double[][] frags = {{-2, 0.3, -3.5}, {2.5, 0.3, -2}, {-3, 0.3, 2.8}, {3.2, 0.3, 2.5}};
            for (double[] o : frags) {
                place(center.clone().add(o[0], o[1], o[2]), Material.ICE,
                        0.5f, 0.5f, 0.5f, 180, 220, 240, 22);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.5f, 0.4f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 2, 0), 60, 7, 1, 7, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Altar pulse + ENCHANT-style ELECTRIC_SPARK ring
            if (altar != null && tick > 35 && tick < 280 && tick % 25 == 0) {
                setScale(altar.entity(), 1.2f, 1.2f, 1.2f, 8);
                for (int i = 0; i < 24; i++) {
                    double a = (Math.PI * 2 * i) / 24;
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            c.clone().add(Math.cos(a) * 2, 1.5, Math.sin(a) * 2), 1, 0.05, 0.05, 0.05, 0.05);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);
            }
            if (altar != null && tick > 35 && tick < 280 && tick % 25 == 10) {
                setScale(altar.entity(), 1.0f, 1.0f, 1.0f, 8);
            }
            // Rim columns sway
            if (tick > 35 && tick < 280 && tick % 6 == 0) {
                for (int i = 0; i < columns.size(); i++) {
                    if (toppled.contains(i)) continue;
                    if (i == 2 || i == 9) continue;
                    float sway = (float) Math.toRadians(3 * Math.sin(tick * 0.05 + i));
                    setRotation(columns.get(i).entity(), sway, 0f, 0f, 1f, 6);
                }
            }
            // Column collapse every 50t — toward player
            if (tick > 50 && tick < 270 && tick % 50 == 0) {
                Player p = findNearestPlayer(c, 14.0);
                int target = -1;
                if (p != null) {
                    double dx = p.getLocation().getX() - c.getX();
                    double dz = p.getLocation().getZ() - c.getZ();
                    double a = Math.atan2(dz, dx);
                    if (a < 0) a += Math.PI * 2;
                    target = (int) Math.round(a / (Math.PI * 2) * 16) % 16;
                }
                if (target < 0) target = (tick / 50) % 16;
                int tries = 0;
                while ((toppled.contains(target) || target == 2 || target == 9) && tries < 16) {
                    target = (target + 1) % 16;
                    tries++;
                }
                if (tries < 16) {
                    toppled.add(target);
                    double a = (Math.PI * 2 * target) / 16;
                    BlockDisplay e = columns.get(target).entity();
                    interp(e, new Vector3f((float) (-Math.cos(a) * 3) - 0.5f, -1f - 0.5f, (float) (-Math.sin(a) * 3) - 0.5f),
                            new AxisAngle4f((float) Math.toRadians(85), (float) Math.sin(a), 0f, (float) -Math.cos(a)), null, 8);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
                    c.getWorld().spawnParticle(Particle.FALLING_DUST,
                            c.clone().add(Math.cos(a) * 8, 1.5, Math.sin(a) * 8),
                            30, 1.5, 2, 1.5, 0, Material.WHITE_CONCRETE.createBlockData());
                    // Bonus damage along collapse line
                    if (p != null) p.damage(16.0);
                }
            }
            // FALLING_DUST ambient + ENTITY_GLOW_SQUID_AMBIENT
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.FALLING_DUST, c.clone().add(0, 4, 0),
                        4, 7, 1, 7, 0, Material.WHITE_CONCRETE.createBlockData());
            }
            if (tick % 70 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.0f, 0.4f);
            }

            // Final collapse 280-310 — all remaining columns inward
            if (tick >= 280 && tick < 305) {
                if (tick == 280) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.4f);
                if (tick % 3 == 0) {
                    for (int i = 0; i < columns.size(); i++) {
                        if (toppled.contains(i)) continue;
                        double a = (Math.PI * 2 * i) / 16;
                        BlockDisplay e = columns.get(i).entity();
                        float tilt = (float) Math.toRadians((tick - 280) * 2.5);
                        interp(e, new Vector3f((float) (-Math.cos(a) * 0.3 * (tick - 280)) - 0.5f, -0.5f, (float) (-Math.sin(a) * 0.3 * (tick - 280)) - 0.5f),
                                new AxisAngle4f(tilt, (float) Math.sin(a), 0f, (float) -Math.cos(a)), null, 3);
                    }
                    for (BlockDisplayHandle h : arches) {
                        BlockDisplay e = h.entity();
                        Vector3f s = e.getTransformation().getScale();
                        setScale(e, s.x * 0.85f, s.y * 0.85f, s.z * 0.85f, 3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenColosseum(plugin); }
    }

    // ================================================================
    // #30 — CRYSTALLINE OBSERVATORY (domed observatory + telescope)
    // 32 blocks: 12 BLUE_STAINED_GLASS dome panels, 5 telescope barrel, 4 yoke,
    // 4 base pillars, 1 floor disc, 1 eyepiece, 4 control panel, 1 mounting.
    // Tracks player, fires beam every 80t.
    // ================================================================
    public static class CrystallineObservatory extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> domePanels = new ArrayList<>();
        private final List<BlockDisplayHandle> telescope = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private BlockDisplayHandle eyepiece;
        private float scopeYaw = 0f;

        public CrystallineObservatory(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_observatory", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(108.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(2);
            config.setDurationTicks(290);
            config.setCooldownTicks(60);
        }

        private BlockDisplayHandle place(Location loc, Material mat, float sx, float sy, float sz, int r, int g, int b, int interp) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(0.001f, 0.001f, 0.001f).glow(r, g, b).interpolation(interp, 0);
            spawnedEntities.add(h.entity());
            all.add(h);
            setScale(h.entity(), sx, sy, sz, interp);
            return h;
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 1 floor disc LIGHT_BLUE_CONCRETE 4.0
            place(center.clone().add(0, 0.1, 0), Material.LIGHT_BLUE_CONCRETE,
                    4.0f, 0.2f, 4.0f, 130, 200, 240, 18);
            // 4 base pillars (1.0 x 2.5 x 1.0)
            double[][] base = {{-1.5, 1.3, -1.5}, {1.5, 1.3, -1.5}, {-1.5, 1.3, 1.5}, {1.5, 1.3, 1.5}};
            for (double[] o : base) {
                place(center.clone().add(o[0], o[1], o[2]), Material.PACKED_ICE,
                        1.0f, 2.5f, 1.0f, 150, 210, 240, 18);
            }
            // 12 BLUE_STAINED_GLASS dome panels hemispherical radius 4
            for (int i = 0; i < 12; i++) {
                double phi;
                double theta;
                if (i < 6) {
                    phi = Math.PI / 4;
                    theta = (Math.PI * 2 * i) / 6;
                } else {
                    phi = Math.PI / 8;
                    theta = (Math.PI * 2 * (i - 6)) / 6 + Math.PI / 6;
                }
                double radius = 3.5;
                double px = radius * Math.sin(phi) * Math.cos(theta);
                double py = 3.0 + radius * Math.cos(phi);
                double pz = radius * Math.sin(phi) * Math.sin(theta);
                BlockDisplayHandle h = place(center.clone().add(px, py, pz), Material.BLUE_STAINED_GLASS,
                        1.5f, 0.3f, 1.5f, 80, 160, 220, 20);
                BlockDisplay e = h.entity();
                interp(e, null, new AxisAngle4f((float) (theta + Math.PI / 2), 0f, 1f, 0f),
                        new Vector3f(1.5f, 0.3f, 1.5f), 20);
                domePanels.add(h);
            }
            // 4 mounting yoke PACKED_ICE
            double[][] yoke = {{-0.6, 3.5, 0}, {0.6, 3.5, 0}, {0, 3.5, -0.6}, {0, 3.5, 0.6}};
            for (double[] o : yoke) {
                place(center.clone().add(o[0], o[1], o[2]), Material.PACKED_ICE,
                        0.8f, 0.4f, 0.8f, 150, 210, 240, 20);
            }
            // 5 telescope barrel SMOOTH_QUARTZ stretched
            for (int s = 0; s < 5; s++) {
                telescope.add(place(center.clone().add(0, 4.0 + s * 0.6, 0), Material.SMOOTH_QUARTZ,
                        0.4f, 0.4f, 3.0f, 220, 230, 245, 22));
            }
            // Eyepiece DIAMOND_BLOCK
            eyepiece = place(center.clone().add(0, 3.8, 0), Material.DIAMOND_BLOCK,
                    0.4f, 0.4f, 0.4f, 200, 240, 255, 24);
            // 4 control panel CALCITE
            double[][] ctrl = {{-1.8, 1.0, 0}, {1.8, 1.0, 0}, {0, 1.0, -1.8}, {0, 1.0, 1.8}};
            for (double[] o : ctrl) {
                place(center.clone().add(o[0], o[1], o[2]), Material.CALCITE,
                        0.3f, 0.3f, 0.3f, 230, 240, 245, 22);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.4f, 0.5f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 5, 0), 30, 4, 2, 4, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Telescope tracks player — yaw rotates to face nearest player
            if (tick > 30 && tick < 270 && tick % 4 == 0) {
                Player p = findNearestPlayer(c, 25.0);
                float targetYaw;
                if (p != null) {
                    double dx = p.getLocation().getX() - c.getX();
                    double dz = p.getLocation().getZ() - c.getZ();
                    targetYaw = (float) Math.atan2(dz, dx);
                } else {
                    targetYaw = scopeYaw + 0.05f;
                }
                // Smooth-rotate toward target
                float diff = targetYaw - scopeYaw;
                while (diff > Math.PI) diff -= (float) (Math.PI * 2);
                while (diff < -Math.PI) diff += (float) (Math.PI * 2);
                scopeYaw += diff * 0.15f;
                for (int i = 0; i < telescope.size(); i++) {
                    BlockDisplay e = telescope.get(i).entity();
                    interp(e, null, new AxisAngle4f(scopeYaw, 0f, 1f, 0f),
                            new Vector3f(0.4f, 0.4f, 3.0f), 4);
                }
            }
            // Eyepiece glow
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.GLOW, c.clone().add(0, 3.8, 0), 4, 0.3, 0.3, 0.3, 0.02);
            }
            // SNOWFLAKE through dome slit
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 6.5, 0), 6, 2, 1, 2, 0.03);
            }
            // Periodic sweep sound
            if (tick > 30 && tick < 270 && tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 0.8f, 1.4f);
            }

            // Beam fire every 80t
            if (tick > 60 && tick < 260 && tick % 80 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 1.2f);
                Player target = findNearestPlayer(c, 25.0);
                Location origin = c.clone().add(Math.cos(scopeYaw) * 2.0, 4.5, Math.sin(scopeYaw) * 2.0);
                Location aim = target != null ? target.getLocation() : origin.clone().add(Math.cos(scopeYaw) * 8, 0, Math.sin(scopeYaw) * 8);
                // Particle beam
                for (int d = 0; d < 18; d++) {
                    double t = d / 18.0;
                    Location lp = origin.clone().add(
                            (aim.getX() - origin.getX()) * t,
                            (aim.getY() - origin.getY()) * t,
                            (aim.getZ() - origin.getZ()) * t);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, lp, 4, 0.1, 0.1, 0.1, 0.02);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, lp, 2, 0.1, 0.1, 0.1, 0.01);
                }
                // Beam impact damage
                if (target != null && target.getGameMode() == GameMode.SURVIVAL) {
                    target.damage(24.0);
                }
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, aim, 30, 2, 1, 2, 0.1);
            }

            // Dome collapse 270-295 — panels fall outward like petals
            if (tick >= 270 && tick < 295) {
                if (tick == 270) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.7f, 0.7f);
                if (tick % 4 == 0) {
                    for (int i = 0; i < domePanels.size(); i++) {
                        BlockDisplay e = domePanels.get(i).entity();
                        double a = (Math.PI * 2 * i) / domePanels.size();
                        float drop = -(tick - 270) * 0.3f;
                        interp(e, new Vector3f((float) (Math.cos(a) * (tick - 270) * 0.15) - 0.5f, drop - 0.5f, (float) (Math.sin(a) * (tick - 270) * 0.15) - 0.5f),
                                new AxisAngle4f((float) Math.toRadians((tick - 270) * 3), (float) Math.sin(a), 0f, (float) -Math.cos(a)),
                                null, 4);
                    }
                    for (BlockDisplayHandle h : telescope) {
                        BlockDisplay e = h.entity();
                        Vector3f s = e.getTransformation().getScale();
                        setScale(e, s.x * 0.85f, s.y * 0.85f, s.z * 0.85f, 4);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineObservatory(plugin); }
    }
}
