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
 * FreezingIce — BlockDisplay attacks file 6/8 (attacks 51-60).
 * Theme: Mechanical / Clockwork & Frozen Creatures II.
 *
 * Mechanical attacks rely on continuous AxisAngle4f rotation to animate
 * spinning gears, hands, pendulums, and orbits. Creature attacks emphasize
 * anatomical recognizability — kraken tentacles, leviathan coils, beholder
 * eye-stalks, mantis raptorial forearms.
 *
 * Ice palette: PACKED_ICE, BLUE_ICE, ICE, TINTED_GLASS, BLUE_STAINED_GLASS,
 *              COPPER_BLOCK (clockwork gold accents), IRON_BLOCK (machinery),
 *              AMETHYST_BLOCK, QUARTZ_BLOCK, DIAMOND_BLOCK, WHITE_CONCRETE
 *
 * Particles: SOUL_FIRE_FLAME (cold flame), END_ROD, CRIT (machine sparks),
 *            SNOWFLAKE, CLOUD (steam puffs)
 * Sounds: BLOCK_NOTE_BLOCK_BIT (mechanical clicks), BLOCK_BELL_USE (clock chime),
 *         ENTITY_IRON_GOLEM_ATTACK (heavy machinery), ENTITY_GLOW_SQUID_AMBIENT
 *
 * Attacks (all use 30+ BlockDisplays, all use constant damage radius 6-15):
 *  51. FrozenClockTower    — clock body + rotating hour/minute hands + swinging pendulum
 *  52. IceGearAssembly     — 3 interlocking gear rings rotating at different speeds
 *  53. FreezingPendulum    — long pendulum scythe swinging on huge arc
 *  54. FrozenOrrery        — central sun + 3 orbiting planets + moons + base
 *  55. IceClockworkSpider  — clockwork body, 8 mechanical legs, gear on back
 *  56. FrostKrakenRise     — central mantle + 8 writhing tentacles
 *  57. IceLeviathanCoil    — long serpentine body in 5 coil-loops
 *  58. GlacialBeholder     — central eye + radiating eye-stalks
 *  59. FrostWraithCircle   — 8 wraith figures circling player
 *  60. IceMantisStrike     — mantis body + huge raptorial forearms
 */
public final class FreezingIceBlockDisplay6 {
    private FreezingIceBlockDisplay6() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FrozenClockTower(plugin));
        registry.register(new IceGearAssembly(plugin));
        registry.register(new FreezingPendulum(plugin));
        registry.register(new FrozenOrrery(plugin));
        registry.register(new IceClockworkSpider(plugin));
        registry.register(new FrostKrakenRise(plugin));
        registry.register(new IceLeviathanCoil(plugin));
        registry.register(new GlacialBeholder(plugin));
        registry.register(new FrostWraithCircle(plugin));
        registry.register(new IceMantisStrike(plugin));
    }

    // ================================================================
    // Helpers
    // ================================================================
    private static void rotateGroup(List<BlockDisplayHandle> group, float angle,
                                     float ax, float ay, float az, int durTicks) {
        for (BlockDisplayHandle h : group) {
            BlockDisplay e = h.entity();
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(durTicks);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, ax, ay, az),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        }
    }

    private static void translateBlock(BlockDisplayHandle h, float x, float y, float z, int durTicks) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(durTicks);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                new AxisAngle4f().set(t.getLeftRotation()),
                t.getScale(),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    private static void scaleBlock(BlockDisplayHandle h, float sx, float sy, float sz, int durTicks) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(durTicks);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f().set(t.getLeftRotation()),
                new Vector3f(sx, sy, sz),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    // ================================================================
    // #51 — FROZEN CLOCK TOWER
    // Tall standing clock: 8 case (QUARTZ), 1 TINTED_GLASS face, 12 numerals
    // (CALCITE/QUARTZ), 5 minute-hand (PACKED_ICE) rotating fast, 4 hour-hand
    // (BLUE_ICE) rotating slow, pendulum bob (DIAMOND_BLOCK) + 3-rod ICE
    // swinging ±25°, 4 ornate top (BLUE_ICE), 4 base (QUARTZ_BLOCK). 41 blocks.
    // Periodic chime strike. Constant 8.0r, ~9 dmg / 12t.
    // ================================================================
    public static class FrozenClockTower extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> caseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> minuteHand = new ArrayList<>();
        private final List<BlockDisplayHandle> hourHand = new ArrayList<>();
        private final List<BlockDisplayHandle> pendulum = new ArrayList<>();
        private final List<BlockDisplayHandle> numerals = new ArrayList<>();
        private float minuteAngle = 0f;
        private float hourAngle = 0f;

        public FrozenClockTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_clock_tower", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(180.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(280);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Case: 6 stacked QUARTZ_BLOCK + 2 base wide blocks
            for (int s = 0; s < 6; s++) {
                Location loc = center.clone().add(0, 0.5 + s * 1.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(1.2f, 1.2f, 0.8f).glow(220, 235, 245).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                caseBlocks.add(h);
            }
            // Base widening
            for (int s = 0; s < 2; s++) {
                Location loc = center.clone().add(0, 0.2 + s * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(1.6f, 0.35f, 1.1f).glow(200, 220, 240).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                caseBlocks.add(h);
            }
            // Ornate top: 4 BLUE_ICE pinnacles
            for (int i = 0; i < 4; i++) {
                double ang = (Math.PI * 2 * i) / 4.0;
                Location loc = center.clone().add(Math.cos(ang) * 0.45, 7.9, Math.sin(ang) * 0.35);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.35f, 0.6f, 0.35f).glow(140, 200, 240).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                caseBlocks.add(h);
            }

            // Clock face — TINTED_GLASS disc at Y=6.0
            Location faceLoc = center.clone().add(0, 6.0, 0.5);
            BlockDisplayHandle face = displayBuilder.spawnBlock(faceLoc, Material.TINTED_GLASS);
            face.scale(1.5f, 1.5f, 0.1f).glow(180, 220, 255).interpolation(6, 0);
            spawnedEntities.add(face.entity());
            caseBlocks.add(face);

            // 12 numerals around face
            for (int i = 0; i < 12; i++) {
                double ang = (Math.PI * 2 * i) / 12.0 - Math.PI / 2.0;
                double nx = Math.cos(ang) * 0.95;
                double ny = Math.sin(ang) * 0.95 + 6.0;
                Location loc = center.clone().add(nx, ny, 0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc,
                        (i % 3 == 0) ? Material.DIAMOND_BLOCK : Material.QUARTZ_BLOCK);
                h.scale(0.2f, 0.2f, 0.08f).glow(220, 230, 250).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                numerals.add(h);
            }

            // Minute hand: 5 PACKED_ICE blocks extending up from center of face
            for (int s = 0; s < 5; s++) {
                Location loc = center.clone().add(0, 6.0, 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.12f, 0.12f, 0.1f).glow(200, 235, 255).interpolation(2, 0);
                BlockDisplay e = h.entity();
                // Pre-position along Y as a stick: 5 stacked segments
                e.setTransformation(new Transformation(
                        new Vector3f(-0.06f, -0.06f + s * 0.32f, -0.05f),
                        new AxisAngle4f(0, 0, 0, 1),
                        new Vector3f(0.12f, 0.32f, 0.1f),
                        new AxisAngle4f(0, 0, 0, 1)
                ));
                spawnedEntities.add(e);
                minuteHand.add(h);
            }
            // Hour hand: 4 BLUE_ICE blocks (shorter, thicker)
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(0, 6.0, 0.75);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.16f, 0.16f, 0.1f).glow(140, 200, 240).interpolation(2, 0);
                BlockDisplay e = h.entity();
                e.setTransformation(new Transformation(
                        new Vector3f(-0.08f, -0.08f + s * 0.25f, -0.05f),
                        new AxisAngle4f(0, 0, 0, 1),
                        new Vector3f(0.16f, 0.25f, 0.1f),
                        new AxisAngle4f(0, 0, 0, 1)
                ));
                spawnedEntities.add(e);
                hourHand.add(h);
            }

            // Pendulum: 3 ICE rods + 1 DIAMOND_BLOCK bob at bottom
            for (int s = 0; s < 3; s++) {
                Location loc = center.clone().add(0, 4.5 - s * 0.7, 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(0.12f, 0.7f, 0.12f).glow(200, 230, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                pendulum.add(h);
            }
            Location bobLoc = center.clone().add(0, 2.2, 0.4);
            BlockDisplayHandle bob = displayBuilder.spawnBlock(bobLoc, Material.DIAMOND_BLOCK);
            bob.scale(0.6f, 0.6f, 0.6f).glow(180, 240, 255).interpolation(3, 0);
            spawnedEntities.add(bob.entity());
            pendulum.add(bob);

            DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 1.4f, 0.7f);
            w.spawnParticle(Particle.END_ROD, faceLoc, 30, 1.2, 1.2, 0.3, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Minute hand rotates fast (6°/tick around Z axis — front-facing clock)
            if (tick % 2 == 0) {
                minuteAngle += (float) Math.toRadians(12.0);
                rotateGroup(minuteHand, minuteAngle, 0f, 0f, 1f, 2);
            }
            // Hour hand rotates slow (1°/4 ticks)
            if (tick % 8 == 0) {
                hourAngle += (float) Math.toRadians(2.0);
                rotateGroup(hourHand, hourAngle, 0f, 0f, 1f, 8);
            }
            // Pendulum swings ±25° on 40t period (rotate around Z about top pivot)
            if (tick % 2 == 0) {
                float pAngle = (float) Math.toRadians(25.0 * Math.sin(tick * 2.0 * Math.PI / 40.0));
                rotateGroup(pendulum, pAngle, 0f, 0f, 1f, 2);
            }

            // Striking chime every 60t
            if (tick > 0 && tick % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BELL_USE, 1.6f, 0.6f);
                c.getWorld().spawnParticle(Particle.END_ROD,
                        c.clone().add(0, 6.0, 0.5), 25, 1.5, 1.5, 0.5, 0.05);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 6.0, 0.5), 30, 2.0, 2.0, 0.5, 0.03);
            }
            // Mechanical tick
            if (tick % 20 == 10) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BIT, 0.6f, 1.3f);
            }
            // Ambient cold flame at base
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        c.clone().add(0, 0.5, 0), 4, 1.5, 0.2, 1.5, 0.01);
            }

            // Dissipate: shrink last 30t
            if (tick >= 250) {
                float remain = 1.0f - ((tick - 250) / 30f);
                if (remain < 0) remain = 0;
                if (tick % 3 == 0) {
                    for (BlockDisplayHandle h : caseBlocks) scaleBlock(h, 1.2f * remain, 1.2f * remain, 0.8f * remain, 3);
                    for (BlockDisplayHandle h : numerals) scaleBlock(h, 0.2f * remain, 0.2f * remain, 0.08f * remain, 3);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenClockTower(plugin); }
    }

    // ================================================================
    // #52 — ICE GEAR ASSEMBLY
    // 3 interlocking gear rings rotating at different speeds with alternating
    // directions, central axle, mount brackets, and connecting belt-links.
    // 36 gear-teeth + 3 axle + 4 mount + 4 belt-links = 47 blocks.
    // Constant 8.5r, ~7 dmg / 10t (grinding).
    // ================================================================
    public static class IceGearAssembly extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bigGear = new ArrayList<>();
        private final List<BlockDisplayHandle> midGear = new ArrayList<>();
        private final List<BlockDisplayHandle> smallGear = new ArrayList<>();
        private final List<BlockDisplayHandle> beltLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> mountBrackets = new ArrayList<>();
        private final List<BlockDisplayHandle> axleBlocks = new ArrayList<>();
        private float bigAngle = 0f;
        private float midAngle = 0f;
        private float smallAngle = 0f;

        public IceGearAssembly(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_gear_assembly", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(160.0);
            config.setDamageRadius(8.5);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(260);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Big gear (radius 2.0, 14 teeth at Y+3, left side)
            buildGear(center.clone().add(-2.2, 3, 0), bigGear, 14, 2.0, Material.PACKED_ICE, 200, 235, 255);
            // Mid gear (radius 1.5, 12 teeth at Y+3, center)
            buildGear(center.clone().add(0, 3, 0), midGear, 12, 1.5, Material.BLUE_ICE, 150, 210, 245);
            // Small gear (radius 1.0, 10 teeth at Y+3, right side)
            buildGear(center.clone().add(1.9, 3, 0), smallGear, 10, 1.0, Material.PACKED_ICE, 200, 235, 255);

            // Axle: 3 IRON_BLOCK behind center
            for (int s = 0; s < 3; s++) {
                Location loc = center.clone().add(-2.2 + s * 2.05, 3, -0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 200, 220).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                axleBlocks.add(h);
            }
            // 4 mount-brackets COPPER_BLOCK (gold accent)
            double[][] mounts = {{-2.6, 1.3, 0}, {-2.6, 4.7, 0}, {2.3, 1.3, 0}, {2.3, 4.7, 0}};
            for (double[] m : mounts) {
                Location loc = center.clone().add(m[0], m[1], m[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.COPPER_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 165, 110).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                mountBrackets.add(h);
            }
            // 4 belt-link TINTED_GLASS spanning between gears
            double[][] belts = {{-1.1, 3.0, 0.2}, {-1.1, 3.0, -0.2}, {0.95, 3.0, 0.2}, {0.95, 3.0, -0.2}};
            for (double[] b : belts) {
                Location loc = center.clone().add(b[0], b[1], b[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(1.0f, 0.15f, 0.15f).glow(180, 220, 255).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                beltLinks.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.2f, 0.9f);
            w.spawnParticle(Particle.CRIT, center.clone().add(0, 3, 0), 25, 1.5, 1.5, 0.5, 0.1);
        }

        private void buildGear(Location ringCenter, List<BlockDisplayHandle> bag,
                                int teeth, double radius, Material teethMat,
                                int r, int g, int b) {
            // Hub: 2 IRON_BLOCK at center
            for (int i = 0; i < 2; i++) {
                Location loc = ringCenter.clone().add(0, 0, i * 0.1 - 0.05);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.5f, 0.5f, 0.2f).glow(210, 215, 230).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                bag.add(h);
            }
            // Teeth around ring
            for (int i = 0; i < teeth; i++) {
                double ang = (Math.PI * 2 * i) / teeth;
                double px = Math.cos(ang) * radius;
                double py = Math.sin(ang) * radius;
                Location loc = ringCenter.clone().add(px, py, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, teethMat);
                h.scale(0.3f, 0.3f, 0.25f).glow(r, g, b).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                bag.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rotate gears (around Z axis — facing player). Alternate direction.
            if (tick % 2 == 0) {
                bigAngle += (float) Math.toRadians(4.0);    // slow
                midAngle -= (float) Math.toRadians(6.0);    // medium, opposite
                smallAngle += (float) Math.toRadians(9.0);  // fast
                rotateGroup(bigGear, bigAngle, 0f, 0f, 1f, 2);
                rotateGroup(midGear, midAngle, 0f, 0f, 1f, 2);
                rotateGroup(smallGear, smallAngle, 0f, 0f, 1f, 2);
            }
            // Belt-link pulse
            if (tick % 10 == 0) {
                float bp = 0.9f + 0.2f * (float) Math.sin(tick * 0.3);
                for (BlockDisplayHandle h : beltLinks) scaleBlock(h, 1.0f * bp, 0.15f, 0.15f, 6);
            }

            // Grinding particles
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT,
                        c.clone().add(0, 3, 0), 6, 2.5, 1.0, 0.5, 0.2);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 3, 0), 4, 2.5, 1.0, 0.5, 0.05);
            }
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.9f, 1.4f);
            }

            // Dissipate last 25t — shrink and wobble
            if (tick >= 235) {
                float remain = 1.0f - ((tick - 235) / 25f);
                if (remain < 0) remain = 0;
                if (tick % 3 == 0) {
                    for (BlockDisplayHandle h : bigGear) scaleBlock(h, 0.3f * remain, 0.3f * remain, 0.25f * remain, 3);
                    for (BlockDisplayHandle h : midGear) scaleBlock(h, 0.3f * remain, 0.3f * remain, 0.25f * remain, 3);
                    for (BlockDisplayHandle h : smallGear) scaleBlock(h, 0.3f * remain, 0.3f * remain, 0.25f * remain, 3);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceGearAssembly(plugin); }
    }

    // ================================================================
    // #53 — FREEZING PENDULUM
    // Pit-and-pendulum giant scythe blade. Suspension beam top, chain rod,
    // scythe blade crescent, counter-weight. 4 beam + 6 chain + 1 pivot +
    // 12 blade + 6 edge + 1 counter + 4 mount = 34 blocks.
    // Swings ±45° on 60t period. Constant 9.0r, ~12 dmg / 10t at low-arc.
    // ================================================================
    public static class FreezingPendulum extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> beam = new ArrayList<>();
        private final List<BlockDisplayHandle> swingGroup = new ArrayList<>();
        private float swingAngle = 0f;

        public FreezingPendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("freezing_pendulum", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(220.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(280);
            config.setCooldownTicks(150);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Suspension beam: 4 QUARTZ_BLOCK horizontal at top Y+10
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(-1.5 + i, 10.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(1.0f, 0.4f, 0.4f).glow(220, 240, 255).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                beam.add(h);
            }
            // 4 mount brackets at beam endpoints
            for (int i = 0; i < 4; i++) {
                double off = (i < 2) ? -1.6 : 1.6;
                double yo = (i % 2 == 0) ? 10.9 : 10.1;
                Location loc = center.clone().add(off, yo, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(220, 230, 240).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                beam.add(h);
            }

            // The swing group: pivot at Y+10. Everything below rotates as one rigid unit.
            // 6 chain rods ICE (vertical from pivot down)
            for (int s = 0; s < 6; s++) {
                Location loc = center.clone().add(0, 8.5 - s * 1.2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(0.15f, 1.2f, 0.15f).glow(200, 230, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                swingGroup.add(h);
            }
            // Pivot joint
            Location pivot = center.clone().add(0, 10.0, 0);
            BlockDisplayHandle piv = displayBuilder.spawnBlock(pivot, Material.DIAMOND_BLOCK);
            piv.scale(0.4f, 0.4f, 0.4f).glow(190, 240, 255).interpolation(3, 0);
            spawnedEntities.add(piv.entity());
            swingGroup.add(piv);

            // Scythe crescent blade: 12 BLUE_ICE in arc at Y+1 forming crescent
            for (int i = 0; i < 12; i++) {
                double ang = Math.PI * (i / 11.0) - Math.PI / 2.0;
                double bx = Math.sin(ang) * 3.5;
                double by = -Math.cos(ang) * 1.0 + 1.5;
                Location loc = center.clone().add(bx, by, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.35f, 0.25f, 0.3f).glow(160, 220, 250).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                swingGroup.add(h);
            }
            // Blade-edge: 6 IRON_BLOCK along cutting edge
            for (int i = 0; i < 6; i++) {
                double ang = Math.PI * (i / 5.0) - Math.PI / 2.0;
                double bx = Math.sin(ang) * 3.6;
                double by = -Math.cos(ang) * 1.0 + 1.3;
                Location loc = center.clone().add(bx, by, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.15f, 0.15f, 0.25f).glow(220, 230, 250).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                swingGroup.add(h);
            }
            // Counter-weight ball PACKED_ICE on opposite end (above pivot)
            Location cw = center.clone().add(0, 11.5, 0);
            BlockDisplayHandle cwh = displayBuilder.spawnBlock(cw, Material.PACKED_ICE);
            cwh.scale(0.8f, 0.8f, 0.8f).glow(210, 230, 250).interpolation(3, 0);
            spawnedEntities.add(cwh.entity());
            swingGroup.add(cwh);

            DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 1.3f, 0.5f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1, 0), 30, 3, 0.5, 1, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Swing ±45° on 60t period (rotation about Z axis through pivot at Y+10)
            if (tick % 2 == 0) {
                swingAngle = (float) Math.toRadians(45.0 * Math.sin(tick * 2.0 * Math.PI / 60.0));
                rotateGroup(swingGroup, swingAngle, 0f, 0f, 1f, 2);
            }

            // Swish at low-arc passes (sin near 0)
            int phaseInPeriod = tick % 60;
            if (phaseInPeriod == 0 || phaseInPeriod == 30) {
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.4f, 1.2f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 1.5, 0), 40, 3.5, 0.6, 1.0, 0.2);
                c.getWorld().spawnParticle(Particle.CRIT,
                        c.clone().add(0, 1.5, 0), 30, 3.5, 0.4, 0.8, 0.3);
            }
            if (tick % 7 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        c.clone().add(0, 1.5, 0), 4, 2.5, 0.3, 0.5, 0.02);
            }

            // Dissipate: blade falls (translate down) + scatter
            if (tick >= 255) {
                int drop = tick - 255;
                if (drop % 3 == 0) {
                    for (BlockDisplayHandle h : swingGroup) {
                        float remain = 1.0f - drop / 25f;
                        if (remain < 0) remain = 0;
                        Transformation t = h.entity().getTransformation();
                        scaleBlock(h, t.getScale().x * 0.93f, t.getScale().y * 0.93f, t.getScale().z * 0.93f, 3);
                    }
                }
                if (drop == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FreezingPendulum(plugin); }
    }

    // ================================================================
    // #54 — FROZEN ORRERY
    // Solar-system model: central sun + 3 orbit rings + 3 planets + 5 moons +
    // 4 base pillars + base disc + 16 axis spokes. ~37 blocks.
    // Planets orbit at different speeds; sun pulses; sun-pulse adds extra damage.
    // Constant 10.0r, ~8 dmg / 12t. Sun-pulse: extra 4 every 40t.
    // ================================================================
    public static class FrozenOrrery extends BlockDisplayAttack {
        private BlockDisplayHandle sun;
        private final List<BlockDisplayHandle> ringA = new ArrayList<>();
        private final List<BlockDisplayHandle> ringB = new ArrayList<>();
        private final List<BlockDisplayHandle> ringC = new ArrayList<>();
        private final List<BlockDisplayHandle> base = new ArrayList<>();
        private final List<BlockDisplayHandle> spokes = new ArrayList<>();
        private BlockDisplayHandle planetA;
        private BlockDisplayHandle planetB;
        private BlockDisplayHandle planetC;
        private final List<BlockDisplayHandle> moons = new ArrayList<>();

        public FrozenOrrery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_orrery", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(170.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(280);
            config.setCooldownTicks(150);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Sun at Y+4
            Location sunLoc = center.clone().add(0, 4.0, 0);
            sun = displayBuilder.spawnBlock(sunLoc, Material.DIAMOND_BLOCK);
            sun.scale(0.85f, 0.85f, 0.85f).glow(255, 235, 180).interpolation(5, 0);
            spawnedEntities.add(sun.entity());

            // Orbit rings at Y+4, radii 2/4/6. Each ring = 8 BLUE_STAINED_GLASS
            buildRing(center.clone().add(0, 4.0, 0), ringA, 2.0, 8);
            buildRing(center.clone().add(0, 4.0, 0), ringB, 4.0, 10);
            buildRing(center.clone().add(0, 4.0, 0), ringC, 6.0, 12);

            // Planets (positions seeded; updated each tick)
            planetA = spawnPlanet(center.clone().add(2.0, 4.0, 0), Material.PACKED_ICE, 0.4f, 200, 235, 255);
            planetB = spawnPlanet(center.clone().add(4.0, 4.0, 0), Material.AMETHYST_BLOCK, 0.55f, 180, 130, 220);
            planetC = spawnPlanet(center.clone().add(6.0, 4.0, 0), Material.BLUE_ICE, 0.65f, 140, 200, 240);

            // 5 moons around planetC
            for (int i = 0; i < 5; i++) {
                Location moonLoc = center.clone().add(6.5, 4.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(moonLoc, Material.QUARTZ_BLOCK);
                h.scale(0.22f, 0.22f, 0.22f).glow(230, 240, 250).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                moons.add(h);
            }

            // 4 base pillars QUARTZ_BLOCK
            for (int i = 0; i < 4; i++) {
                double ang = (Math.PI * 2 * i) / 4.0;
                Location loc = center.clone().add(Math.cos(ang) * 2.0, 1.0, Math.sin(ang) * 2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.4f, 2.0f, 0.4f).glow(220, 230, 245).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                base.add(h);
            }
            // Base disc
            Location disc = center.clone().add(0, 0.1, 0);
            BlockDisplayHandle dh = displayBuilder.spawnBlock(disc, Material.QUARTZ_BLOCK);
            dh.scale(2.5f, 0.2f, 2.5f).glow(220, 230, 245).interpolation(5, 0);
            spawnedEntities.add(dh.entity());
            base.add(dh);

            // 8 axis spokes TINTED_GLASS connecting base to sun
            for (int i = 0; i < 8; i++) {
                double ang = (Math.PI * 2 * i) / 8.0;
                Location loc = center.clone().add(Math.cos(ang) * 1.0, 2.0, Math.sin(ang) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.1f, 2.0f, 0.1f).glow(170, 210, 245).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                spokes.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BELL_USE, 1.2f, 1.1f);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, sunLoc, 30, 0.8, 0.8, 0.8, 0.05);
        }

        private void buildRing(Location ringCenter, List<BlockDisplayHandle> bag, double radius, int count) {
            for (int i = 0; i < count; i++) {
                double ang = (Math.PI * 2 * i) / count;
                Location loc = ringCenter.clone().add(Math.cos(ang) * radius, 0, Math.sin(ang) * radius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.18f, 0.1f, 0.18f).glow(140, 200, 240).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                bag.add(h);
            }
        }

        private BlockDisplayHandle spawnPlanet(Location loc, Material mat, float scale, int r, int g, int b) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            h.scale(scale, scale, scale).glow(r, g, b).interpolation(3, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Planet orbits via translation (rotating around sun on each ring)
            float aAngle = tick * 0.10f;
            float bAngle = tick * 0.06f;
            float cAngle = tick * 0.04f;

            // Planet A on ring A (radius 2). Offset from spawned position (2,4,0) — translate to (cos*2, 0, sin*2) - (2, 0, 0)
            if (tick % 2 == 0) {
                translateBlock(planetA, (float) (Math.cos(aAngle) * 2.0) - 2.0f, 0f, (float) (Math.sin(aAngle) * 2.0), 2);
                translateBlock(planetB, (float) (Math.cos(bAngle) * 4.0) - 4.0f, 0f, (float) (Math.sin(bAngle) * 4.0), 2);
                translateBlock(planetC, (float) (Math.cos(cAngle) * 6.0) - 6.0f, 0f, (float) (Math.sin(cAngle) * 6.0), 2);
                // Moons orbit planet C (smaller radius around planetC)
                for (int i = 0; i < moons.size(); i++) {
                    double mAng = tick * 0.18 + (Math.PI * 2 * i) / moons.size();
                    float mx = (float) (Math.cos(cAngle) * 6.0 + Math.cos(mAng) * 0.8) - 6.5f;
                    float mz = (float) (Math.sin(cAngle) * 6.0 + Math.sin(mAng) * 0.8);
                    translateBlock(moons.get(i), mx, 0f, mz, 2);
                }
            }
            // Sun pulse — sin scale modulation
            if (tick % 4 == 0) {
                float pulse = 0.85f + 0.12f * (float) Math.sin(tick * 0.12);
                scaleBlock(sun, pulse, pulse, pulse, 4);
            }

            // Particles
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        c.clone().add(0, 4.0, 0), 4, 0.6, 0.6, 0.6, 0.02);
                c.getWorld().spawnParticle(Particle.END_ROD,
                        c.clone().add(0, 4.0, 0), 6, 2.0, 0.3, 2.0, 0.05);
            }
            // Sun-pulse damage every 40t (just an extra triggered impact at the sun)
            if (tick > 30 && tick % 40 == 0) {
                triggerImpactDamage(c.clone().add(0, 1.0, 0));
                DisplayBuilder.playSound(c, Sound.BLOCK_BELL_USE, 1.4f, 1.0f);
                c.getWorld().spawnParticle(Particle.END_ROD,
                        c.clone().add(0, 4.0, 0), 40, 1.5, 1.5, 1.5, 0.15);
            }

            // Dissipate
            if (tick >= 250) {
                float remain = 1.0f - ((tick - 250) / 30f);
                if (remain < 0) remain = 0;
                if (tick % 4 == 0) {
                    scaleBlock(sun, 0.85f * remain, 0.85f * remain, 0.85f * remain, 4);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenOrrery(plugin); }
    }

    // ================================================================
    // #55 — ICE CLOCKWORK SPIDER
    // Mechanical spider: 6 body QUARTZ + 1 head PACKED_ICE + 2 eye BLUE_STAINED_GLASS
    // + 1 wind-up key COPPER + 8 legs × 3 segs BLUE_ICE (24) + 4 chest-light
    // CALCITE/DIAMOND. 8 legs visibly mechanical. 42 blocks. Constant 7.5r,
    // ~6 dmg / 12t.
    // ================================================================
    public static class IceClockworkSpider extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> legs = new ArrayList<>();
        private BlockDisplayHandle windKey;
        private float keyAngle = 0f;

        public IceClockworkSpider(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_clockwork_spider", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(140.0);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 6 body blocks QUARTZ (oval torso at Y+1.5)
            double[][] bodyOffs = {{-0.5, 1.5, 0}, {0.5, 1.5, 0},
                                   {-0.3, 1.8, 0.3}, {0.3, 1.8, 0.3},
                                   {-0.3, 1.2, -0.3}, {0.3, 1.2, -0.3}};
            for (double[] off : bodyOffs) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.55f, 0.55f, 0.55f).glow(220, 230, 250).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
            }
            // Head PACKED_ICE
            Location head = center.clone().add(0, 1.8, 1.0);
            BlockDisplayHandle hd = displayBuilder.spawnBlock(head, Material.PACKED_ICE);
            hd.scale(0.7f, 0.7f, 0.7f).glow(200, 230, 255).interpolation(4, 0);
            spawnedEntities.add(hd.entity());
            body.add(hd);
            // 2 eyes BLUE_STAINED_GLASS
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add((i == 0 ? -0.2 : 0.2), 1.9, 1.35);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                h.scale(0.18f, 0.18f, 0.08f).glow(120, 200, 255).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
            }
            // 4 chest lights DIAMOND_BLOCK
            for (int i = 0; i < 4; i++) {
                double ang = (Math.PI * 2 * i) / 4.0;
                Location loc = center.clone().add(Math.cos(ang) * 0.35, 1.5, Math.sin(ang) * 0.35 + 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.18f, 0.18f, 0.18f).glow(220, 240, 255).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
            }

            // Wind-up key COPPER on back (rotates)
            windKey = displayBuilder.spawnBlock(center.clone().add(0, 1.8, -0.8), Material.COPPER_BLOCK);
            windKey.scale(0.25f, 0.25f, 0.7f).glow(220, 165, 110).interpolation(2, 0);
            spawnedEntities.add(windKey.entity());

            // 8 legs × 3 segments BLUE_ICE, sprawling outward
            for (int legIdx = 0; legIdx < 8; legIdx++) {
                List<BlockDisplayHandle> leg = new ArrayList<>();
                double angle = (Math.PI * 2 * legIdx) / 8.0;
                for (int s = 0; s < 3; s++) {
                    float sx = (float) Math.cos(angle) * (0.7f + s * 0.55f);
                    float sz = (float) Math.sin(angle) * (0.7f + s * 0.55f);
                    float sy = 1.5f - s * 0.4f;
                    Location loc = center.clone().add(sx, sy, sz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    float scale = 0.32f - s * 0.06f;
                    h.scale(scale, scale, scale).glow(150, 210, 245).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    leg.add(h);
                }
                legs.add(leg);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BIT, 1.0f, 1.4f);
            w.spawnParticle(Particle.CRIT, center.clone().add(0, 1.8, -0.8), 15, 0.3, 0.3, 0.3, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Wind-up key rotates continuously (Z-axis facing back)
            if (tick % 2 == 0) {
                keyAngle += (float) Math.toRadians(20.0);
                BlockDisplay e = windKey.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(keyAngle, 0f, 0f, 1f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
            // Legs alternate-step: bob up/down in offset phases
            if (tick % 4 == 0) {
                for (int li = 0; li < legs.size(); li++) {
                    float phase = (float) Math.sin((tick + li * 4) * 0.2);
                    float yOff = phase * 0.2f;
                    for (BlockDisplayHandle h : legs.get(li)) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        Vector3f curT = t.getTranslation();
                        e.setInterpolationDuration(4);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(curT.x, yOff - 0.5f, curT.z),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }
            // Sparks from key, breath from head
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT,
                        c.clone().add(0, 1.8, -0.8), 6, 0.3, 0.3, 0.3, 0.15);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 1.8, 1.4), 5, 0.3, 0.2, 0.5, 0.06);
            }
            if (tick % 18 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BIT, 0.7f, 1.5f);
            }

            // Dissipate
            if (tick >= 215) {
                float remain = 1.0f - ((tick - 215) / 25f);
                if (remain < 0) remain = 0;
                if (tick % 3 == 0) {
                    for (BlockDisplayHandle h : body) {
                        Transformation t = h.entity().getTransformation();
                        Vector3f s = t.getScale();
                        scaleBlock(h, s.x * 0.9f, s.y * 0.9f, s.z * 0.9f, 3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceClockworkSpider(plugin); }
    }

    // ================================================================
    // #56 — FROST KRAKEN RISE
    // Sea-monster: 6 body mantle PACKED_ICE, 1 BLUE_ICE head bulb, 4 OBSIDIAN
    // eye-spike, 8 tentacles × 4 segments curving outward (32), 4 DIAMOND_BLOCK
    // beak, 8 CALCITE/QUARTZ suckers along tentacles. 59 blocks total.
    // Tentacles writhe via per-segment rotation wobble.
    // Constant 11.0r, ~10 dmg / 12t.
    // ================================================================
    public static class FrostKrakenRise extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> mantle = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> tentacles = new ArrayList<>();

        public FrostKrakenRise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_kraken_rise", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(190.0);
            config.setDamageRadius(11.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(35);
            config.setDurationTicks(260);
            config.setCooldownTicks(150);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Mantle: 6 PACKED_ICE blocks at Y+1.5..3
            double[][] mantleOffs = {{-0.7, 2.0, 0}, {0.7, 2.0, 0}, {0, 2.0, 0.7}, {0, 2.0, -0.7},
                                     {-0.4, 3.0, 0.4}, {0.4, 3.0, -0.4}};
            for (double[] off : mantleOffs) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(1.4f, 1.4f, 1.4f).glow(190, 225, 250).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                mantle.add(h);
            }
            // Head bulb BLUE_ICE on top
            Location bulb = center.clone().add(0, 4.0, 0);
            BlockDisplayHandle bh = displayBuilder.spawnBlock(bulb, Material.BLUE_ICE);
            bh.scale(1.6f, 1.4f, 1.6f).glow(160, 215, 245).interpolation(6, 0);
            spawnedEntities.add(bh.entity());
            mantle.add(bh);
            // 4 eye-spike OBSIDIAN
            for (int i = 0; i < 4; i++) {
                double ang = (Math.PI * 2 * i) / 4.0;
                Location loc = center.clone().add(Math.cos(ang) * 0.7, 4.3, Math.sin(ang) * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.35f, 0.5f, 0.35f).glow(60, 50, 80).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                mantle.add(h);
            }
            // 4 beak DIAMOND_BLOCK underneath
            for (int i = 0; i < 4; i++) {
                double ang = (Math.PI * 2 * i) / 4.0;
                Location loc = center.clone().add(Math.cos(ang) * 0.3, 1.0, Math.sin(ang) * 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.25f, 0.3f, 0.25f).glow(200, 240, 255).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                mantle.add(h);
            }

            // 8 tentacles × 4 segs, curving outward
            for (int ti = 0; ti < 8; ti++) {
                List<BlockDisplayHandle> tent = new ArrayList<>();
                double angle = (Math.PI * 2 * ti) / 8.0;
                for (int s = 0; s < 4; s++) {
                    // Curve outward: each segment extends further but also droops down
                    double dist = 1.0 + s * 1.1;
                    double droopY = 1.5 - s * 0.35;
                    double tx = Math.cos(angle) * dist;
                    double tz = Math.sin(angle) * dist;
                    Location loc = center.clone().add(tx, droopY, tz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                    float sc = 1.0f - s * 0.18f;
                    h.scale(sc, sc, sc).glow(180, 220, 250).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    tent.add(h);
                    // Add a CALCITE/QUARTZ sucker on the middle segment of every other tentacle
                    if (ti % 2 == 0 && s == 1) {
                        Location sucker = center.clone().add(tx + 0.1, droopY + 0.25, tz);
                        BlockDisplayHandle sh = displayBuilder.spawnBlock(sucker, Material.QUARTZ_BLOCK);
                        sh.scale(0.2f, 0.2f, 0.2f).glow(240, 245, 250).interpolation(3, 0);
                        spawnedEntities.add(sh.entity());
                        tent.add(sh);
                    }
                }
                tentacles.add(tent);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.6f, 0.5f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1, 0), 60, 4, 0.5, 4, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Body sway
            if (tick % 3 == 0) {
                float sway = (float) Math.toRadians(8.0 * Math.sin(tick * 0.07));
                rotateGroup(mantle, sway, 0f, 0f, 1f, 3);
            }
            // Tentacles writhe per-segment
            if (tick % 2 == 0) {
                for (int ti = 0; ti < tentacles.size(); ti++) {
                    for (int s = 0; s < tentacles.get(ti).size(); s++) {
                        BlockDisplayHandle h = tentacles.get(ti).get(s);
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        float wobble = (float) Math.toRadians(
                                10.0 * Math.sin((tick + ti * 6 + s * 4) * 0.18));
                        // wobble about Y axis (sideways swish) + slight pitch on X
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(wobble, 0.4f, 1f, 0.2f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                }
            }
            // Particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 2.5, 0), 10, 4, 1, 4, 0.05);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        c.clone().add(0, 3.5, 0), 3, 0.5, 0.5, 0.5, 0.02);
            }
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.4f);
            }
            // Periodic tentacle-lash (translation pulse outward, then back)
            if (tick > 40 && tick % 50 == 0) {
                int li = (tick / 50) % tentacles.size();
                List<BlockDisplayHandle> tent = tentacles.get(li);
                double angle = (Math.PI * 2 * li) / 8.0;
                for (int s = 0; s < tent.size(); s++) {
                    BlockDisplayHandle h = tent.get(s);
                    float ex = (float) Math.cos(angle) * 0.8f;
                    float ez = (float) Math.sin(angle) * 0.8f;
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    Vector3f curT = t.getTranslation();
                    e.setInterpolationDuration(6);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(curT.x + ex, curT.y, curT.z + ez),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.3f, 0.8f);
            }

            // Dissipate
            if (tick >= 235) {
                float remain = 1.0f - ((tick - 235) / 25f);
                if (remain < 0) remain = 0;
                if (tick % 3 == 0) {
                    for (BlockDisplayHandle h : mantle) {
                        Transformation t = h.entity().getTransformation();
                        Vector3f s = t.getScale();
                        scaleBlock(h, s.x * 0.9f, s.y * 0.9f, s.z * 0.9f, 3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostKrakenRise(plugin); }
    }

    // ================================================================
    // #57 — ICE LEVIATHAN COIL
    // Serpentine body in 5 coil-loops surrounding the player. Each coil is
    // 8 blocks (BLUE_ICE/PACKED_ICE) on a horizontal ring at varying heights;
    // head segment (3 large) + tail (3 small) + 2 fin-spines per coil (10).
    // 40 body + 6 head/tail = 46 blocks. Coils slowly rotate at different speeds.
    // Constant 10.0r, ~9 dmg / 12t.
    // ================================================================
    public static class IceLeviathanCoil extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> coils = new ArrayList<>();
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> tail = new ArrayList<>();
        private final float[] coilAngles = new float[5];

        public IceLeviathanCoil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_leviathan_coil", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(180.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(260);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 5 coil rings at heights Y=1,3,5,7,9 with alternating radii
            double[] radii = {3.5, 3.0, 4.0, 3.0, 3.5};
            int[] counts = {8, 8, 8, 8, 8};
            for (int ci = 0; ci < 5; ci++) {
                List<BlockDisplayHandle> coil = new ArrayList<>();
                double r = radii[ci];
                int count = counts[ci];
                double yh = 1.0 + ci * 1.8;
                for (int i = 0; i < count; i++) {
                    double ang = (Math.PI * 2 * i) / count;
                    double px = Math.cos(ang) * r;
                    double pz = Math.sin(ang) * r;
                    Location loc = center.clone().add(px, yh, pz);
                    Material mat = (i % 2 == 0) ? Material.PACKED_ICE : Material.BLUE_ICE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.6f, 0.6f, 0.6f).glow(170, 220, 250).interpolation(4, 0);
                    spawnedEntities.add(h.entity());
                    coil.add(h);
                    // Fin-spine: small AMETHYST_BLOCK along top of segment
                    if (i % 2 == 0) {
                        Location fin = center.clone().add(px, yh + 0.5, pz);
                        BlockDisplayHandle fh = displayBuilder.spawnBlock(fin, Material.AMETHYST_BLOCK);
                        fh.scale(0.18f, 0.32f, 0.18f).glow(180, 130, 220).interpolation(4, 0);
                        spawnedEntities.add(fh.entity());
                        coil.add(fh);
                    }
                }
                coils.add(coil);
            }

            // Head: 3 big blocks at top of coil 5
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(3.5, 9.0 + i * 0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.75f - i * 0.1f, 0.6f, 0.75f - i * 0.1f).glow(200, 240, 255).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                head.add(h);
            }
            // Tail: 3 small at bottom of coil 1
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-3.5, 0.6 - i * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.35f - i * 0.08f, 0.35f - i * 0.05f, 0.35f - i * 0.08f).glow(150, 210, 245).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                tail.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.5f, 0.6f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 4, 0), 60, 4, 4, 4, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Each coil rotates at its own speed in alternating directions
            float[] speeds = {0.05f, -0.07f, 0.04f, -0.06f, 0.05f};
            if (tick % 2 == 0) {
                for (int ci = 0; ci < coils.size(); ci++) {
                    coilAngles[ci] += speeds[ci];
                    rotateGroup(coils.get(ci), coilAngles[ci], 0f, 1f, 0f, 2);
                }
            }
            // Head bob, tail wag
            if (tick % 4 == 0) {
                float headAng = (float) Math.toRadians(15.0 * Math.sin(tick * 0.1));
                rotateGroup(head, headAng, 0f, 1f, 0f, 4);
                float tailAng = (float) Math.toRadians(20.0 * Math.sin(tick * 0.15));
                rotateGroup(tail, tailAng, 0f, 1f, 0f, 4);
            }

            // Particles
            if (tick % 3 == 0) {
                double pa = tick * 0.4;
                double px = Math.cos(pa) * 3.5;
                double pz = Math.sin(pa) * 3.5;
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(px, 4 + Math.sin(tick * 0.1) * 2, pz), 4, 0.4, 0.4, 0.4, 0.05);
            }
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        c.clone().add(3.5, 9.4, 0), 4, 0.4, 0.4, 0.4, 0.02);
            }
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.3f, 0.7f);
            }

            // Dissipate
            if (tick >= 235) {
                float remain = 1.0f - ((tick - 235) / 25f);
                if (remain < 0) remain = 0;
                if (tick % 3 == 0) {
                    for (List<BlockDisplayHandle> coil : coils) {
                        for (BlockDisplayHandle h : coil) {
                            Transformation t = h.entity().getTransformation();
                            Vector3f s = t.getScale();
                            scaleBlock(h, s.x * 0.9f, s.y * 0.9f, s.z * 0.9f, 3);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceLeviathanCoil(plugin); }
    }

    // ================================================================
    // #58 — GLACIAL BEHOLDER
    // Central spherical eye + radiating eye-stalks. 12 PACKED_ICE body sphere
    // + 1 huge DIAMOND_BLOCK central eye + 1 OBSIDIAN pupil + 8 eye-stalks of
    // 3 segments each BLUE_ICE (24) + 8 stalk-eye AMETHYST_BLOCK + 4 lower fang
    // OBSIDIAN. 58 blocks. Stalks rotate independently around body.
    // Constant 9.0r, ~8 dmg / 12t.
    // ================================================================
    public static class GlacialBeholder extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> stalks = new ArrayList<>();
        private final float[] stalkAngles = new float[8];
        private BlockDisplayHandle centralEye;
        private float bodyRot = 0f;

        public GlacialBeholder(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_beholder", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(170.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(260);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 12 body sphere (golden angle) PACKED_ICE at Y+3
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            double bodyRadius = 1.2;
            for (int i = 0; i < 12; i++) {
                double yn = 1.0 - (2.0 * i / 11.0);
                double rAtY = Math.sqrt(1 - yn * yn);
                double theta = goldenAngle * i;
                double px = Math.cos(theta) * rAtY * bodyRadius;
                double pz = Math.sin(theta) * rAtY * bodyRadius;
                Location loc = center.clone().add(px, yn * bodyRadius + 3.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.55f, 0.55f, 0.55f).glow(200, 230, 250).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
            }
            // Central eye DIAMOND_BLOCK (large)
            centralEye = displayBuilder.spawnBlock(center.clone().add(0, 3.5, 1.0), Material.DIAMOND_BLOCK);
            centralEye.scale(0.9f, 0.9f, 0.5f).glow(220, 245, 255).interpolation(5, 0);
            spawnedEntities.add(centralEye.entity());
            // Pupil OBSIDIAN
            Location pupil = center.clone().add(0, 3.5, 1.4);
            BlockDisplayHandle ph = displayBuilder.spawnBlock(pupil, Material.OBSIDIAN);
            ph.scale(0.4f, 0.4f, 0.15f).glow(40, 40, 60).interpolation(5, 0);
            spawnedEntities.add(ph.entity());
            body.add(ph);
            // 4 lower fang OBSIDIAN
            for (int i = 0; i < 4; i++) {
                double ang = (Math.PI * 2 * i) / 4.0;
                Location loc = center.clone().add(Math.cos(ang) * 0.5, 2.5, Math.sin(ang) * 0.5 + 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.18f, 0.4f, 0.18f).glow(50, 40, 60).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
            }

            // 8 eye-stalks radiating outward
            for (int si = 0; si < 8; si++) {
                List<BlockDisplayHandle> stalk = new ArrayList<>();
                double angle = (Math.PI * 2 * si) / 8.0;
                double pitch = Math.toRadians(20 + 40 * (si % 3));
                for (int s = 0; s < 3; s++) {
                    double dist = 1.5 + s * 0.7;
                    double xx = Math.cos(angle) * Math.cos(pitch) * dist;
                    double yy = 3.5 + Math.sin(pitch) * dist;
                    double zz = Math.sin(angle) * Math.cos(pitch) * dist;
                    Location loc = center.clone().add(xx, yy, zz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    float sc = 0.32f - s * 0.04f;
                    h.scale(sc, sc, sc).glow(140, 200, 240).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    stalk.add(h);
                }
                // Stalk-tip eye AMETHYST_BLOCK
                double dist = 1.5 + 3 * 0.7;
                double xx = Math.cos(angle) * Math.cos(pitch) * dist;
                double yy = 3.5 + Math.sin(pitch) * dist;
                double zz = Math.sin(angle) * Math.cos(pitch) * dist;
                Location loc = center.clone().add(xx, yy, zz);
                BlockDisplayHandle eh = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                eh.scale(0.32f, 0.32f, 0.32f).glow(190, 130, 230).interpolation(3, 0);
                spawnedEntities.add(eh.entity());
                stalk.add(eh);
                stalks.add(stalk);
                stalkAngles[si] = (float) angle;
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.5f, 0.6f);
            w.spawnParticle(Particle.END_ROD, center.clone().add(0, 3.5, 1.0), 25, 0.5, 0.5, 0.5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Body rotates slowly (yaw)
            if (tick % 2 == 0) {
                bodyRot += 0.03f;
                rotateGroup(body, bodyRot, 0f, 1f, 0f, 2);
                BlockDisplay ce = centralEye.entity();
                Transformation t = ce.getTransformation();
                ce.setInterpolationDuration(2);
                ce.setInterpolationDelay(0);
                ce.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(bodyRot, 0f, 1f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
            // Stalks wiggle independently
            if (tick % 3 == 0) {
                for (int si = 0; si < stalks.size(); si++) {
                    float wig = (float) Math.toRadians(15.0 * Math.sin((tick + si * 7) * 0.2));
                    rotateGroup(stalks.get(si), wig, 0f, 0f, 1f, 3);
                }
            }
            // Pupil pulse
            if (tick % 8 == 0) {
                float pp = 0.35f + 0.1f * (float) Math.sin(tick * 0.18);
                scaleBlock(centralEye, 0.7f + pp, 0.7f + pp, 0.5f, 8);
            }
            // Particles
            if (tick % 4 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD,
                        c.clone().add(0, 3.5, 1.0), 6, 0.4, 0.4, 0.2, 0.03);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        c.clone().add(0, 3.5, 0), 5, 1.2, 1.2, 1.2, 0.02);
            }
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.2f, 0.5f);
            }

            // Dissipate
            if (tick >= 235) {
                float remain = 1.0f - ((tick - 235) / 25f);
                if (remain < 0) remain = 0;
                if (tick % 3 == 0) {
                    for (BlockDisplayHandle h : body) {
                        Transformation t = h.entity().getTransformation();
                        Vector3f s = t.getScale();
                        scaleBlock(h, s.x * 0.9f, s.y * 0.9f, s.z * 0.9f, 3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GlacialBeholder(plugin); }
    }

    // ================================================================
    // #59 — FROST WRAITH CIRCLE
    // 8 wraith figures circling the player. Each wraith is 5 BLUE_ICE body
    // tapered torso + 1 PACKED_ICE head + 2 BLUE_STAINED_GLASS eyes +
    // 2 OBSIDIAN claw-hand = 10 per wraith × 8 = 80 blocks if maxed.
    // We use 4-block-per-wraith design: 32 wraith blocks + 4 base markers.
    // Circle slowly rotates. Constant 9.5r, ~8 dmg / 12t.
    // ================================================================
    public static class FrostWraithCircle extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> wraiths = new ArrayList<>();
        private final List<BlockDisplayHandle> baseMarkers = new ArrayList<>();
        private float circleAngle = 0f;

        public FrostWraithCircle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_wraith_circle", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(165.0);
            config.setDamageRadius(9.5);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(260);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double ringR = 6.0;
            // 8 wraiths around a ring
            for (int wi = 0; wi < 8; wi++) {
                double angle = (Math.PI * 2 * wi) / 8.0;
                double wx = Math.cos(angle) * ringR;
                double wz = Math.sin(angle) * ringR;
                List<BlockDisplayHandle> wraith = new ArrayList<>();
                // 3 tapered torso BLUE_ICE
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(wx, 1.0 + s * 0.7, wz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    float sc = 0.6f - s * 0.1f;
                    h.scale(sc, 0.7f, sc).glow(140, 200, 240).interpolation(4, 0);
                    spawnedEntities.add(h.entity());
                    wraith.add(h);
                }
                // Head PACKED_ICE
                Location head = center.clone().add(wx, 2.9, wz);
                BlockDisplayHandle hh = displayBuilder.spawnBlock(head, Material.PACKED_ICE);
                hh.scale(0.55f, 0.55f, 0.55f).glow(200, 230, 250).interpolation(4, 0);
                spawnedEntities.add(hh.entity());
                wraith.add(hh);
                // 2 eyes BLUE_STAINED_GLASS
                double inward = -Math.cos(angle) * 0.2;
                double inZ = -Math.sin(angle) * 0.2;
                for (int e = 0; e < 2; e++) {
                    double exo = (e == 0 ? -0.2 : 0.2) * -Math.sin(angle);
                    double ezo = (e == 0 ? -0.2 : 0.2) * Math.cos(angle);
                    Location loc = center.clone().add(wx + exo + inward, 2.95, wz + ezo + inZ);
                    BlockDisplayHandle eh = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                    eh.scale(0.13f, 0.13f, 0.08f).glow(120, 200, 255).interpolation(4, 0);
                    spawnedEntities.add(eh.entity());
                    wraith.add(eh);
                }
                // 2 claw OBSIDIAN
                for (int cl = 0; cl < 2; cl++) {
                    double clo = (cl == 0 ? -0.5 : 0.5);
                    double clx = -Math.sin(angle) * clo;
                    double clz = Math.cos(angle) * clo;
                    Location loc = center.clone().add(wx + clx, 1.4, wz + clz);
                    BlockDisplayHandle ch = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    ch.scale(0.18f, 0.4f, 0.18f).glow(60, 50, 80).interpolation(4, 0);
                    spawnedEntities.add(ch.entity());
                    wraith.add(ch);
                }
                wraiths.add(wraith);
            }
            // 4 base markers AMETHYST_BLOCK
            for (int i = 0; i < 4; i++) {
                double ang = (Math.PI * 2 * i) / 4.0;
                Location loc = center.clone().add(Math.cos(ang) * 1.5, 0.2, Math.sin(ang) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.25f, 0.4f).glow(180, 130, 220).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                baseMarkers.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.3f, 0.5f);
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 2, 0), 40, 4, 1, 4, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Circle rotates (translation each wraith around center)
            circleAngle += 0.025f;
            double ringR = 6.0;
            if (tick % 2 == 0) {
                for (int wi = 0; wi < wraiths.size(); wi++) {
                    double angle = circleAngle + (Math.PI * 2 * wi) / 8.0;
                    double wx = Math.cos(angle) * ringR;
                    double wz = Math.sin(angle) * ringR;
                    // base spawned at (cos(ang0)*R, _, sin(ang0)*R). Need translation to (wx,wz).
                    double ang0 = (Math.PI * 2 * wi) / 8.0;
                    float dx = (float) (wx - Math.cos(ang0) * ringR);
                    float dz = (float) (wz - Math.sin(ang0) * ringR);
                    for (BlockDisplayHandle h : wraiths.get(wi)) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                new Vector3f(dx - 0.5f, -0.5f, dz - 0.5f),
                                new AxisAngle4f().set(t.getLeftRotation()),
                                t.getScale(),
                                new AxisAngle4f().set(t.getRightRotation())
                        ));
                    }
                }
            }
            // Base markers pulse
            if (tick % 8 == 0) {
                float p = 0.35f + 0.1f * (float) Math.sin(tick * 0.2);
                for (BlockDisplayHandle h : baseMarkers) scaleBlock(h, p + 0.05f, 0.25f, p + 0.05f, 8);
            }
            // Particles
            if (tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        c.clone().add(0, 2, 0), 6, 5, 1, 5, 0.02);
            }
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.0f, 0.5f);
            }

            // Dissipate
            if (tick >= 235) {
                float remain = 1.0f - ((tick - 235) / 25f);
                if (remain < 0) remain = 0;
                if (tick % 3 == 0) {
                    for (List<BlockDisplayHandle> wraith : wraiths) {
                        for (BlockDisplayHandle h : wraith) {
                            Transformation t = h.entity().getTransformation();
                            Vector3f s = t.getScale();
                            scaleBlock(h, s.x * 0.9f, s.y * 0.9f, s.z * 0.9f, 3);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostWraithCircle(plugin); }
    }

    // ================================================================
    // #60 — ICE MANTIS STRIKE
    // Mantis: 4 PACKED_ICE thorax + 1 BLUE_ICE head + 2 BLUE_STAINED_GLASS eyes
    // + 2 raptorial forearms × 5 segments BLUE_ICE+IRON_BLOCK (10) + 4 walking
    // legs × 3 segs BLUE_ICE (12) + 2 wings TINTED_GLASS large + 4 antennae
    // ICE = 35 blocks. Forearms periodically strike forward. Constant 8.0r,
    // ~8 dmg / 12t. Strike pulse adds impact.
    // ================================================================
    public static class IceMantisStrike extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> leftArm = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArm = new ArrayList<>();
        private final List<BlockDisplayHandle> legs = new ArrayList<>();
        private final List<BlockDisplayHandle> wings = new ArrayList<>();
        private float armRetractAngle = 0f;

        public IceMantisStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mantis_strike", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(170.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(140);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Thorax: 4 PACKED_ICE elongated blocks
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(0, 1.8, -0.6 - s * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.5f, 0.45f, 0.5f).glow(200, 230, 250).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
            }
            // Head BLUE_ICE
            Location head = center.clone().add(0, 1.9, 0.0);
            BlockDisplayHandle hh = displayBuilder.spawnBlock(head, Material.BLUE_ICE);
            hh.scale(0.55f, 0.55f, 0.55f).glow(160, 220, 250).interpolation(5, 0);
            spawnedEntities.add(hh.entity());
            body.add(hh);
            // Eyes BLUE_STAINED_GLASS (large compound eyes on sides)
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add((i == 0 ? -0.35 : 0.35), 1.95, 0.1);
                BlockDisplayHandle eh = displayBuilder.spawnBlock(loc, Material.BLUE_STAINED_GLASS);
                eh.scale(0.25f, 0.25f, 0.18f).glow(120, 200, 255).interpolation(5, 0);
                spawnedEntities.add(eh.entity());
                body.add(eh);
            }
            // 4 antennae ICE
            for (int i = 0; i < 4; i++) {
                double off = (i % 2 == 0 ? -0.15 : 0.15);
                Location loc = center.clone().add(off, 2.4 + (i / 2) * 0.3, 0.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(0.06f, 0.5f + (i / 2) * 0.2f, 0.06f).glow(220, 240, 255).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                body.add(h);
            }

            // 2 raptorial forearms × 5 segments. Folded against body initially.
            buildArm(center, leftArm, -0.6, true);
            buildArm(center, rightArm, 0.6, false);

            // 4 walking legs × 3 segments outward/back
            for (int li = 0; li < 4; li++) {
                double sx = (li < 2 ? -0.4 : 0.4);
                double sz = -0.8 - (li % 2) * 0.7;
                for (int s = 0; s < 3; s++) {
                    Location loc = center.clone().add(sx + (li < 2 ? -0.3 - s * 0.3 : 0.3 + s * 0.3),
                            1.5 - s * 0.5, sz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    float sc = 0.22f - s * 0.04f;
                    h.scale(sc, sc, sc).glow(150, 210, 245).interpolation(4, 0);
                    spawnedEntities.add(h.entity());
                    legs.add(h);
                }
            }

            // 2 TINTED_GLASS wings stretched along back
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add((i == 0 ? -0.4 : 0.4), 2.0, -1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.7f, 0.1f, 1.5f).glow(170, 215, 250).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                wings.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BIT, 1.2f, 1.6f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 2, 0), 30, 2, 1, 2, 0.05);
        }

        private void buildArm(Location center, List<BlockDisplayHandle> arm, double xOff, boolean left) {
            // Folded: forearm bent at elbow, claw pointing back near body
            // 5 segments: shoulder-segment, upper, elbow, lower, claw
            for (int s = 0; s < 5; s++) {
                // Place initial along x out, then y up, z reaching forward — folded
                double dx = xOff + (left ? -1 : 1) * (s < 3 ? s * 0.25 : 0.5 - (s - 2) * 0.15);
                double dy = 1.8 + (s < 3 ? s * 0.15 : 0.3 - (s - 2) * 0.1);
                double dz = (s < 3 ? -s * 0.1 : -0.2 + (s - 2) * 0.4);
                Location loc = center.clone().add(dx, dy, dz);
                Material mat = (s == 4) ? Material.IRON_BLOCK : Material.BLUE_ICE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float sc = (s == 4) ? 0.3f : 0.28f;
                int r = (s == 4) ? 220 : 150;
                int g = (s == 4) ? 230 : 210;
                int b = (s == 4) ? 250 : 245;
                h.scale(sc, sc, sc + 0.05f).glow(r, g, b).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                arm.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Wing flutter: subtle scale oscillation
            if (tick % 3 == 0) {
                float sw = 0.7f + 0.1f * (float) Math.sin(tick * 0.4);
                for (BlockDisplayHandle h : wings) scaleBlock(h, sw, 0.1f, 1.5f, 3);
            }
            // Leg shuffle
            if (tick % 5 == 0) {
                for (int li = 0; li < legs.size(); li++) {
                    float yJiggle = (float) (Math.sin((tick + li * 4) * 0.18) * 0.08);
                    BlockDisplayHandle h = legs.get(li);
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(5);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            new Vector3f(-0.5f, yJiggle - 0.5f, -0.5f),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            t.getScale(),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }

            // Forearm strike cycle every 50t. Phases:
            //   strike (5t): rotate forearms forward fast
            //   hold (10t): held extended + impact
            //   retract (15t): rotate back
            int phase = tick % 50;
            if (phase == 0) {
                // Strike forward
                rotateGroup(leftArm, (float) Math.toRadians(-90), 1f, 0f, 0f, 5);
                rotateGroup(rightArm, (float) Math.toRadians(-90), 1f, 0f, 0f, 5);
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 1.2f, 1.4f);
            } else if (phase == 6) {
                // Impact damage at the extended position
                triggerImpactDamage(c.clone().add(0, 2.0, 1.5));
                c.getWorld().spawnParticle(Particle.CRIT,
                        c.clone().add(0, 2.0, 1.5), 35, 1.5, 0.5, 0.8, 0.3);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 2.0, 1.5), 30, 1.5, 0.5, 0.8, 0.1);
            } else if (phase == 20) {
                // Retract
                rotateGroup(leftArm, 0f, 1f, 0f, 0f, 8);
                rotateGroup(rightArm, 0f, 1f, 0f, 0f, 8);
            }

            // Particles
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        c.clone().add(0, 2.5, 0), 3, 0.5, 0.5, 0.5, 0.02);
            }
            if (tick % 30 == 15) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BIT, 0.7f, 1.7f);
            }

            // Dissipate
            if (tick >= 215) {
                float remain = 1.0f - ((tick - 215) / 25f);
                if (remain < 0) remain = 0;
                if (tick % 3 == 0) {
                    for (BlockDisplayHandle h : body) {
                        Transformation t = h.entity().getTransformation();
                        Vector3f s = t.getScale();
                        scaleBlock(h, s.x * 0.9f, s.y * 0.9f, s.z * 0.9f, 3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceMantisStrike(plugin); }
    }
}
