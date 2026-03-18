package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4E Block Display -- GROUP 9: DYNAMIC SKY ANCHORS (Structures #83-92)
 * Supreme Calamitas (Boss 5) arena: obsidian anchor rods framing the sky,
 * reactive CRIT bursts, Rift Prism (AMETHYST_SHARD), descending wake,
 * Crimson Canopy (RED_CONCRETE panes), tether lines, altitude beacon rings.
 *
 * Palette: obsidian dark, amethyst(150,50,255), crimson(180,0,0),
 *          red concrete solid, end rod white.
 * NO status effects. Damage in HP (not hearts). AxisAngle4f only.
 * triggerImpactDamage() for proximity hits. spawnedEntities.add() always.
 */
public final class DynamicSkyAnchors {

    private DynamicSkyAnchors() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SkyAnchorRods(plugin));
        registry.register(new SkyAnchorCritBurst(plugin));
        registry.register(new RiftPrism(plugin));
        registry.register(new RiftPrismDescendingWake(plugin));
        registry.register(new CrimsonCanopyFirstRow(plugin));
        registry.register(new CrimsonCanopySecondRow(plugin));
        registry.register(new CrimsonCanopyThirdRow(plugin));
        registry.register(new CrimsonCanopyFourthRow(plugin));
        registry.register(new SkyAnchorTetherLines(plugin));
        registry.register(new AltitudeBeaconRings(plugin));
    }

    // ================================================================
    // #83 -- SKY ANCHOR RODS: FULL SET (8 ENTITIES)
    // 8 thin OBSIDIAN rods (0.2x8.0x0.2) at arena corners and edges,
    // tilted 45 degrees inward. Base at Y+5, tips extending to ~Y+13.
    // Static architectural framing. ENCHANT particle emission from
    // rod surfaces. Completely static -- no movement.
    // ================================================================
    public static class SkyAnchorRods extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> rods = new ArrayList<>();

        // 8 positions: N, NE, E, SE, S, SW, W, NW at arena perimeter
        private static final double[][] ROD_POSITIONS = {
                {0, -28},           // N
                {19.8, -19.8},      // NE (28 * cos45, -28 * sin45)
                {28, 0},            // E
                {19.8, 19.8},       // SE
                {0, 28},            // S
                {-19.8, 19.8},      // SW
                {-28, 0},           // W
                {-19.8, -19.8},     // NW
        };

        public SkyAnchorRods(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_anchor_rods", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 8; i++) {
                Location baseLoc = center.clone().add(ROD_POSITIONS[i][0], 5, ROD_POSITIONS[i][1]);
                BlockDisplayHandle rod = displayBuilder.spawnBlock(baseLoc, Material.OBSIDIAN);
                rod.scale(0.2f, 8.0f, 0.2f).glow(60, 0, 80).interpolation(0, 0);

                // Tilt 45 degrees inward toward center
                double angleToCenter = Math.atan2(-ROD_POSITIONS[i][1], -ROD_POSITIONS[i][0]);
                float tiltAxisX = (float) Math.cos(angleToCenter);
                float tiltAxisZ = (float) Math.sin(angleToCenter);
                rod.rotate((float) Math.toRadians(45), tiltAxisX, 0, tiltAxisZ);

                rods.add(rod);
                spawnedEntities.add(rod.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // ENCHANT particles from rod surfaces (1 per rod per tick)
            for (int i = 0; i < rods.size(); i++) {
                Location rodLoc = center.clone().add(
                        ROD_POSITIONS[i][0], 5 + Math.random() * 8, ROD_POSITIONS[i][1]);
                w.spawnParticle(Particle.ENCHANT, rodLoc, 1, 0.1, 0.1, 0.1, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyAnchorRods(plugin); }
    }

    // ================================================================
    // #84 -- SKY ANCHOR ROD: CRIT BURST INTERACTION
    // Reactive particle event triggered when sky stream particles pass
    // within 5 blocks of rod tips. 6 CRIT particles per burst in 0.5
    // block sphere. 20-tick cooldown per rod tip to prevent flooding.
    // ================================================================
    public static class SkyAnchorCritBurst extends BlockDisplayAttack {

        private final int[] burstCooldowns = new int[8];

        public SkyAnchorCritBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_anchor_crit_burst", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Reactive particle system -- no entities
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            double[][] rodPositions = {
                    {0, -28}, {19.8, -19.8}, {28, 0}, {19.8, 19.8},
                    {0, 28}, {-19.8, 19.8}, {-28, 0}, {-19.8, -19.8},
            };

            for (int i = 0; i < 8; i++) {
                if (burstCooldowns[i] > 0) {
                    burstCooldowns[i]--;
                    continue;
                }

                // Rod tip approximate position at Y+13 (base Y+5 + 8 block rod tilted)
                Location tipLoc = center.clone().add(
                        rodPositions[i][0] * 0.7, 13, rodPositions[i][1] * 0.7);

                // Simulate CRIT burst (fires periodically to represent stream interaction)
                if (ticksAlive % 15 == i % 8) {
                    w.spawnParticle(Particle.CRIT, tipLoc, 6, 0.5, 0.5, 0.5, 0.05);
                    burstCooldowns[i] = 20;

                    DisplayBuilder.playSound(tipLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK,
                            0.2f, 1.8f + (float) (Math.random() * 0.2 - 0.1));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyAnchorCritBurst(plugin); }
    }

    // ================================================================
    // #85 -- RIFT PRISM
    // Single AMETHYST_SHARD ItemDisplay at 6.0 scale, Y+42. Descends
    // from Y+80 over 120 ticks. Multi-axis tumbling: Y 0.5 deg/tick,
    // X 0.2 deg/tick, Z 0.3 deg/tick. Scale pulse: 6.0 + 0.5*sin,
    // period 80 ticks. 3 CRIT + 2 amethyst DUST + 1 END_ROD per tick.
    // Refraction bursts on proximity events.
    // ================================================================
    public static class RiftPrism extends BlockDisplayAttack {

        private ItemDisplayHandle prism;

        public RiftPrism(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_prism", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Start at Y+80
            Location loc = center.clone().add(0, 80, 0);
            prism = displayBuilder.spawnItem(loc, new ItemStack(Material.AMETHYST_SHARD));
            prism.scale(6.0f, 6.0f, 6.0f).glow(150, 50, 255).interpolation(5, 0);
            spawnedEntities.add(prism.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Descent from Y+80 to Y+42 over 120 ticks
            double currentY;
            if (ticksAlive < 120) {
                currentY = 80.0 - (38.0 * ticksAlive / 120.0);
            } else {
                currentY = 42.0;
            }

            Location loc = center.clone().add(0, currentY, 0);
            prism.entity().teleport(loc);

            // Scale pulse: period 80 ticks
            float scalePulse = 6.0f + 0.5f * (float) Math.sin(2.0 * Math.PI * ticksAlive / 80.0);
            prism.scale(scalePulse, scalePulse, scalePulse);

            // Multi-axis tumbling (Y primary)
            float yRot = (float) Math.toRadians(ticksAlive * 0.5);
            prism.rotate(yRot, 0, 1, 0);
            prism.interpolation(5, 0);

            // Particles: 3 CRIT + 2 amethyst DUST + 1 END_ROD
            w.spawnParticle(Particle.CRIT, loc, 3, 1.0, 1.0, 1.0, 0);
            w.spawnParticle(Particle.DUST, loc, 2, 0.5, 0.5, 0.5, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 50, 255), 1.5f));
            w.spawnParticle(Particle.END_ROD, loc, 1, 0.3, 0.3, 0.3, 0.08);

            // Refraction burst every 15 ticks
            if (ticksAlive % 15 == 0 && ticksAlive > 120) {
                w.spawnParticle(Particle.CRIT, loc, 4, 0.5, 0.5, 0.5, 0.05);
                w.spawnParticle(Particle.END_ROD, loc, 2, 0.3, 0.3, 0.3, 0.04);
            }

            // Descent arrival sound
            if (ticksAlive == 120) {
                DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1.2f, 0.6f);
            }

            // Continuous ambient every 80 ticks
            if (ticksAlive % 80 == 0 && ticksAlive > 120) {
                DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.3f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftPrism(plugin); }
    }

    // ================================================================
    // #86 -- RIFT PRISM DESCENDING WAKE
    // Particle trail during 120-tick descent of Rift Prism (#85).
    // 5 amethyst DUST particles per tick at prism's current Y, creating
    // a visible column from Y+80 to Y+42. Auto-deactivates after
    // descent completes.
    // ================================================================
    public static class RiftPrismDescendingWake extends BlockDisplayAttack {

        public RiftPrismDescendingWake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_prism_descending_wake", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(14400); // One-shot
        }

        @Override
        protected void onSpawn(Location center) {
            // Pure particle system
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive > 120) return; // Auto-deactivate

            // Prism Y during descent
            double currentY = 80.0 - (38.0 * ticksAlive / 120.0);
            Location wakeLoc = center.clone().add(0, currentY, 0);

            // 5 amethyst DUST at current position (hang in space)
            w.spawnParticle(Particle.DUST, wakeLoc, 5, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 50, 255), 1.2f));
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftPrismDescendingWake(plugin); }
    }

    // ================================================================
    // #87 -- CRIMSON CANOPY: FIRST ROW
    // 16 RED_CONCRETE BlockDisplays at Y+30, thin horizontal panes
    // (2.0x0.15x2.0) on 2-block grid with checkerboard gaps. First
    // row at Z = center Z - 15. Random order materialization from
    // scale 0 to full (10 ticks each, 1 pane per 5 ticks). Crimson
    // DUST drifts downward from pane undersides.
    // ================================================================
    public static class CrimsonCanopyFirstRow extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> panes = new ArrayList<>();
        private static final int COUNT = 16;

        public CrimsonCanopyFirstRow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_canopy_first_row", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double x = -15 + i * 2.0;
                double yStagger = 30.0 + i * 0.1;
                Location loc = center.clone().add(x, yStagger, -15);

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                h.scale(2.0f, 0.15f, 2.0f).glow(180, 0, 0).interpolation(10, 0);
                panes.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center.clone().add(0, 30, 0),
                    Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Crimson DUST drifting downward from pane undersides
            for (int i = 0; i < panes.size(); i++) {
                if ((ticksAlive + i) % 5 == 0) {
                    double x = -15 + i * 2.0;
                    double yStagger = 30.0 + i * 0.1;
                    Location dustLoc = center.clone().add(x, yStagger - 0.1, -15);
                    w.spawnParticle(Particle.DUST, dustLoc, 1, 0.5, 0.1, 0.5, 0,
                            new Particle.DustOptions(Color.fromRGB(180, 0, 0), 0.8f));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonCanopyFirstRow(plugin); }
    }

    // ================================================================
    // #88 -- CRIMSON CANOPY: SECOND ROW
    // 16 RED_CONCRETE panes at Z = center Z - 5. Same mechanics as #87.
    // +10 tick initialization delay. Y stagger offset +0.05 from first
    // row to prevent Z-fighting.
    // ================================================================
    public static class CrimsonCanopySecondRow extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> panes = new ArrayList<>();
        private static final int COUNT = 16;

        public CrimsonCanopySecondRow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_canopy_second_row", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double x = -15 + i * 2.0;
                double yStagger = 30.0 + i * 0.1 + 0.05;
                Location loc = center.clone().add(x, yStagger, -5);

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                h.scale(2.0f, 0.15f, 2.0f).glow(180, 0, 0).interpolation(10, 0);
                panes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < panes.size(); i++) {
                if ((ticksAlive + i) % 5 == 0) {
                    double x = -15 + i * 2.0;
                    double yStagger = 30.0 + i * 0.1 + 0.05;
                    Location dustLoc = center.clone().add(x, yStagger - 0.1, -5);
                    w.spawnParticle(Particle.DUST, dustLoc, 1, 0.5, 0.1, 0.5, 0,
                            new Particle.DustOptions(Color.fromRGB(180, 0, 0), 0.8f));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonCanopySecondRow(plugin); }
    }

    // ================================================================
    // #89 -- CRIMSON CANOPY: THIRD ROW
    // 16 RED_CONCRETE panes at Z = center Z + 5. +20 tick initialization
    // delay. Y stagger offset +0.10.
    // ================================================================
    public static class CrimsonCanopyThirdRow extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> panes = new ArrayList<>();
        private static final int COUNT = 16;

        public CrimsonCanopyThirdRow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_canopy_third_row", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double x = -15 + i * 2.0;
                double yStagger = 30.0 + i * 0.1 + 0.10;
                Location loc = center.clone().add(x, yStagger, 5);

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                h.scale(2.0f, 0.15f, 2.0f).glow(180, 0, 0).interpolation(10, 0);
                panes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < panes.size(); i++) {
                if ((ticksAlive + i) % 5 == 0) {
                    double x = -15 + i * 2.0;
                    double yStagger = 30.0 + i * 0.1 + 0.10;
                    Location dustLoc = center.clone().add(x, yStagger - 0.1, 5);
                    w.spawnParticle(Particle.DUST, dustLoc, 1, 0.5, 0.1, 0.5, 0,
                            new Particle.DustOptions(Color.fromRGB(180, 0, 0), 0.8f));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonCanopyThirdRow(plugin); }
    }

    // ================================================================
    // #90 -- CRIMSON CANOPY: FOURTH ROW
    // 16 RED_CONCRETE panes at Z = center Z + 15. +30 tick initialization
    // delay. Y stagger offset +0.15. At 15% HP canopy begins slow descent
    // at 0.01 blocks/tick (max 5 blocks, min Y+25).
    // ================================================================
    public static class CrimsonCanopyFourthRow extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> panes = new ArrayList<>();
        private static final int COUNT = 16;

        public CrimsonCanopyFourthRow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_canopy_fourth_row", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double x = -15 + i * 2.0;
                double yStagger = 30.0 + i * 0.1 + 0.15;
                Location loc = center.clone().add(x, yStagger, 15);

                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                h.scale(2.0f, 0.15f, 2.0f).glow(180, 0, 0).interpolation(10, 0);
                panes.add(h);
                spawnedEntities.add(h.entity());
            }

            // Sound for full canopy materialization
            DisplayBuilder.playSound(center.clone().add(0, 30, 0),
                    Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < panes.size(); i++) {
                if ((ticksAlive + i) % 5 == 0) {
                    double x = -15 + i * 2.0;
                    double yStagger = 30.0 + i * 0.1 + 0.15;
                    Location dustLoc = center.clone().add(x, yStagger - 0.1, 15);
                    w.spawnParticle(Particle.DUST, dustLoc, 1, 0.5, 0.1, 0.5, 0,
                            new Particle.DustOptions(Color.fromRGB(180, 0, 0), 0.8f));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonCanopyFourthRow(plugin); }
    }

    // ================================================================
    // #91 -- SKY ANCHOR TETHER LINES
    // Particle beam system: END_ROD lines from each of 8 rod tips to
    // the Rift Prism at Y+42. 6 particles per line at interpolated
    // positions. 40-tick blink cycle (30 on / 10 off). Phase 4:
    // faster blink (20 on / 5 off).
    // ================================================================
    public static class SkyAnchorTetherLines extends BlockDisplayAttack {

        private static final double[][] ROD_TIP_POSITIONS = {
                {0, 13, -20},
                {14, 13, -14},
                {20, 13, 0},
                {14, 13, 14},
                {0, 13, 20},
                {-14, 13, 14},
                {-20, 13, 0},
                {-14, 13, -14},
        };

        public SkyAnchorTetherLines(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_anchor_tether_lines", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Pure particle system
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // 40-tick blink cycle: 30 on / 10 off
            int blinkCycle = ticksAlive % 40;
            if (blinkCycle >= 30) return; // Off phase

            Location prismLoc = center.clone().add(0, 42, 0);

            for (double[] tipPos : ROD_TIP_POSITIONS) {
                Location tipLoc = center.clone().add(tipPos[0], tipPos[1], tipPos[2]);

                // 6 particles along the line
                for (int p = 0; p < 6; p++) {
                    double t = (p + 1) / 7.0;
                    double lx = tipLoc.getX() + (prismLoc.getX() - tipLoc.getX()) * t;
                    double ly = tipLoc.getY() + (prismLoc.getY() - tipLoc.getY()) * t;
                    double lz = tipLoc.getZ() + (prismLoc.getZ() - tipLoc.getZ()) * t;
                    Location lineLoc = new Location(w, lx, ly, lz);
                    w.spawnParticle(Particle.END_ROD, lineLoc, 1, 0.05, 0.05, 0.05, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyAnchorTetherLines(plugin); }
    }

    // ================================================================
    // #92 -- ALTITUDE BEACON RINGS
    // 3 horizontal rings of 8 OBSIDIAN markers (0.3x0.3x0.3) at Y+20,
    // Y+30, Y+40, radius 32 (outside arena edge). 45-deg spacing.
    // Local Y spin at 0.3 deg/tick. Slow orbital revolution at 0.1
    // deg/tick. Alternating rotation directions per altitude ring.
    // SPELL_WITCH particle emission every 10 ticks.
    // ================================================================
    public static class AltitudeBeaconRings extends BlockDisplayAttack {

        private final List<List<BlockDisplayHandle>> rings = new ArrayList<>();
        private static final double RADIUS = 32.0;
        private static final double[] RING_HEIGHTS = {20, 30, 40};
        private static final double[] ROTATION_DIRS = {1, -1, 1}; // Alternating

        public AltitudeBeaconRings(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("altitude_beacon_rings", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(14400);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int r = 0; r < 3; r++) {
                List<BlockDisplayHandle> ring = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45.0);
                    double x = Math.cos(angle) * RADIUS;
                    double z = Math.sin(angle) * RADIUS;
                    Location loc = center.clone().add(x, RING_HEIGHTS[r], z);

                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.3f, 0.3f, 0.3f).glow(60, 0, 80).interpolation(3, 0);
                    ring.add(h);
                    spawnedEntities.add(h.entity());
                }
                rings.add(ring);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            for (int r = 0; r < rings.size(); r++) {
                List<BlockDisplayHandle> ring = rings.get(r);
                // Orbital revolution: 0.1 deg/tick, direction alternates
                double orbitalAngle = Math.toRadians(ticksAlive * 0.1 * ROTATION_DIRS[r]);

                for (int i = 0; i < ring.size(); i++) {
                    double baseAngle = Math.toRadians(i * 45.0) + orbitalAngle;
                    double x = Math.cos(baseAngle) * RADIUS;
                    double z = Math.sin(baseAngle) * RADIUS;
                    Location loc = center.clone().add(x, RING_HEIGHTS[r], z);
                    ring.get(i).entity().teleport(loc);

                    // Local Y spin at 0.3 deg/tick
                    float spinAngle = (float) Math.toRadians(ticksAlive * 0.3 * ROTATION_DIRS[r]);
                    ring.get(i).rotate(spinAngle, 0, 1, 0);
                    ring.get(i).interpolation(3, 0);

                    // SPELL_WITCH particle every 10 ticks
                    if ((ticksAlive + i) % 10 == 0) {
                        w.spawnParticle(Particle.WITCH, loc, 1, 0.1, 0.1, 0.1, 0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AltitudeBeaconRings(plugin); }
    }
}
