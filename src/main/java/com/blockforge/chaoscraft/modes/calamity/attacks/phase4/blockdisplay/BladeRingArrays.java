package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.blockdisplay;

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
 * Phase 4D Block Display -- GROUP 2: SWORD/BLADE RING ARRAYS (#9-18)
 * 10 attacks forming rings of orbiting structures at three height levels.
 * The visual declaration that the Void Emperor is armed with the weapons
 * of every dimension.
 *
 * Structures:
 *  #9  LowBladeRingAlpha      -- Y+5, clockwise, 6 blades, radius 8
 *  #10 LowBladeRingBeta       -- Y+5, counter-clockwise, 4 blades, radius 5
 *  #11 MidBladeRingGamma      -- Y+10, clockwise, 8 blades, radius 12
 *  #12 MidBladeRingDelta      -- Y+10, counter-clockwise, 6 blades, radius 16
 *  #13 HighBladeRingEpsilon   -- Y+15, clockwise tilted, 5 blades, radius 6
 *  #14 HighBladeRingZeta      -- Y+15, counter-clockwise tilted, 4 blades, radius 9
 *  #15 BladeRingConnectors    -- 6 vertical obsidian spoke pillars
 *  #16 OrbitalCenterAxis      -- Central end rod axis Y+0 to Y+20
 *  #17 BladeLaunchBreachPts   -- 6 magenta glass breach point markers
 *  #18 BladeDebrisField       -- Scattered static blade debris on arena floor
 *
 * Rules applied:
 * - NO status effects
 * - Void Emperor palette: magenta(180,0,200), cyan(0,200,255), purple(128,0,255)
 * - AxisAngle4f ONLY, static DisplayBuilder methods
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Damage HP 4.0-12.0
 */
public final class BladeRingArrays {

    private BladeRingArrays() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new LowBladeRingAlpha(plugin));
        registry.register(new LowBladeRingBeta(plugin));
        registry.register(new MidBladeRingGamma(plugin));
        registry.register(new MidBladeRingDelta(plugin));
        registry.register(new HighBladeRingEpsilon(plugin));
        registry.register(new HighBladeRingZeta(plugin));
        registry.register(new BladeRingConnectors(plugin));
        registry.register(new OrbitalCenterAxis(plugin));
        registry.register(new BladeLaunchBreachPoints(plugin));
        registry.register(new BladeDebrisField(plugin));
    }

    // ================================================================
    // #9 -- LOW BLADE RING ALPHA (Y+5, Clockwise)
    // 6 obsidian blades at orbit radius 8, clockwise at 1.2 deg/tick.
    // Each blade oriented tangent, tilted 20 deg inward, self-rotates.
    // ================================================================
    public static class LowBladeRingAlpha extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blades = new ArrayList<>();
        private static final int BLADE_COUNT = 6;
        private static final double ORBIT_RADIUS = 8.0;
        private static final float ORBIT_SPEED = 1.2f; // deg/tick

        public LowBladeRingAlpha(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("low_blade_ring_alpha", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location ringCenter = center.clone().add(0, 5, 0);
            for (int i = 0; i < BLADE_COUNT; i++) {
                double angle = (2 * Math.PI * i) / BLADE_COUNT;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double z = Math.sin(angle) * ORBIT_RADIUS;
                Location loc = ringCenter.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.2f, 1.5f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                // Blade tangent to orbit, tilted 20 deg inward
                float tangentAngle = (float) (angle + Math.PI / 2);
                h.rotate(tangentAngle + (float) Math.toRadians(20), 0, 1, 0);
                blades.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(ringCenter, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location ringCenter = center.clone().add(0, 5, 0);

            // Orbit clockwise
            double orbitalOffset = Math.toRadians(ticksAlive * ORBIT_SPEED);
            for (int i = 0; i < blades.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / BLADE_COUNT;
                double angle = baseAngle + orbitalOffset;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double z = Math.sin(angle) * ORBIT_RADIUS;
                Location loc = ringCenter.clone().add(x, 0, z);
                blades.get(i).entity().teleport(loc);

                // Self-rotation on Z axis at 0.8 deg/tick
                float selfRot = (float) Math.toRadians(ticksAlive * 0.8);
                float tangentAngle = (float) (angle + Math.PI / 2);
                blades.get(i).rotate(tangentAngle, 0, 1, 0);
                blades.get(i).interpolation(2, 0);
            }

            // Sweep sound on full revolution (every 300 ticks at 1.2 deg/tick)
            if (ticksAlive % 300 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(ringCenter, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1.0f);
            }

            // Purple dust trail
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle blade : blades) {
                    DisplayBuilder.purpleDust(blade.entity().getLocation().clone().add(0, 0.5, 0), 2, 0.3);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LowBladeRingAlpha(plugin); }
    }

    // ================================================================
    // #10 -- LOW BLADE RING BETA (Y+5, Counter-Clockwise)
    // 4 dark prismarine blades at radius 5, CCW at 1.5 deg/tick.
    // Tilted 30 deg outward (opposite lean to Alpha).
    // ================================================================
    public static class LowBladeRingBeta extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blades = new ArrayList<>();
        private static final int BLADE_COUNT = 4;
        private static final double ORBIT_RADIUS = 5.0;
        private static final float ORBIT_SPEED = -1.5f; // negative = CCW

        public LowBladeRingBeta(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("low_blade_ring_beta", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(7.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location ringCenter = center.clone().add(0, 5, 0);
            for (int i = 0; i < BLADE_COUNT; i++) {
                double angle = (2 * Math.PI * i) / BLADE_COUNT;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double z = Math.sin(angle) * ORBIT_RADIUS;
                Location loc = ringCenter.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.2f, 1.3f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                blades.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location ringCenter = center.clone().add(0, 5, 0);

            double orbitalOffset = Math.toRadians(ticksAlive * ORBIT_SPEED);
            for (int i = 0; i < blades.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / BLADE_COUNT;
                double angle = baseAngle + orbitalOffset;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double z = Math.sin(angle) * ORBIT_RADIUS;
                Location loc = ringCenter.clone().add(x, 0, z);
                blades.get(i).entity().teleport(loc);

                // Tangent CCW, tilted 30 deg outward
                float tangentAngle = (float) (angle - Math.PI / 2);
                blades.get(i).rotate(tangentAngle, 0, 1, 0);
                blades.get(i).interpolation(2, 0);
            }

            // Subtle enderman teleport sound every 600 ticks (30 sec)
            if (ticksAlive % 600 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(ringCenter, Sound.ENTITY_ENDERMAN_TELEPORT, 0.15f, 1.5f);
            }

            // Cyan dust trail
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle blade : blades) {
                    DisplayBuilder.cyanDust(blade.entity().getLocation().clone().add(0, 0.5, 0), 2, 0.3);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LowBladeRingBeta(plugin); }
    }

    // ================================================================
    // #11 -- MID BLADE RING GAMMA (Y+10, Clockwise)
    // 8 purpur pillar blades at radius 12, CW at 0.8 deg/tick.
    // Horizontal orientation, wave undulation across ring.
    // ================================================================
    public static class MidBladeRingGamma extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blades = new ArrayList<>();
        private static final int BLADE_COUNT = 8;
        private static final double ORBIT_RADIUS = 12.0;
        private static final float ORBIT_SPEED = 0.8f;

        public MidBladeRingGamma(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mid_blade_ring_gamma", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(9.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location ringCenter = center.clone().add(0, 10, 0);
            for (int i = 0; i < BLADE_COUNT; i++) {
                double angle = (2 * Math.PI * i) / BLADE_COUNT;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double z = Math.sin(angle) * ORBIT_RADIUS;
                Location loc = ringCenter.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPUR_PILLAR);
                // Horizontal orientation: blade lies in XZ plane
                h.scale(2.0f, 0.2f, 0.5f).glow(180, 0, 200).interpolation(2, 0);
                blades.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location ringCenter = center.clone().add(0, 10, 0);

            double orbitalOffset = Math.toRadians(ticksAlive * ORBIT_SPEED);
            for (int i = 0; i < blades.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / BLADE_COUNT;
                double angle = baseAngle + orbitalOffset;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double z = Math.sin(angle) * ORBIT_RADIUS;

                // Wave undulation: tilt +/-15 deg, phase-offset per blade (45 deg / blade)
                float phaseOffset = (float) (i * Math.PI / 4);
                float tilt = (float) Math.toRadians(15) * (float) Math.sin(ticksAlive * Math.PI / 30 + phaseOffset);

                Location loc = ringCenter.clone().add(x, 0, z);
                blades.get(i).entity().teleport(loc);

                float tangentAngle = (float) (angle + Math.PI / 2);
                blades.get(i).rotate(tangentAngle, 0, 1, 0);
                blades.get(i).interpolation(2, 0);
            }

            // Magenta dust particles
            if (ticksAlive % 6 == 0) {
                for (BlockDisplayHandle blade : blades) {
                    DisplayBuilder.dustParticles(blade.entity().getLocation(), 2, 0.3,
                            180, 0, 200, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MidBladeRingGamma(plugin); }
    }

    // ================================================================
    // #12 -- MID BLADE RING DELTA (Y+10, Counter-Clockwise)
    // 6 gold blades at radius 16, CCW at 0.6 deg/tick. Vertical
    // orientation, blades tilted 45 deg inward toward center.
    // ================================================================
    public static class MidBladeRingDelta extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blades = new ArrayList<>();
        private static final int BLADE_COUNT = 6;
        private static final double ORBIT_RADIUS = 16.0;
        private static final float ORBIT_SPEED = -0.6f;

        public MidBladeRingDelta(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mid_blade_ring_delta", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(8.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location ringCenter = center.clone().add(0, 10, 0);
            for (int i = 0; i < BLADE_COUNT; i++) {
                double angle = (2 * Math.PI * i) / BLADE_COUNT;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double z = Math.sin(angle) * ORBIT_RADIUS;
                Location loc = ringCenter.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                // Vertical blade, tilted 45 deg toward center
                float inwardTilt = (float) Math.toRadians(45);
                h.scale(0.3f, 2.2f, 0.5f).glow(180, 0, 200).interpolation(2, 0);
                h.rotate((float) angle + inwardTilt, 0, 1, 0);
                blades.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location ringCenter = center.clone().add(0, 10, 0);

            double orbitalOffset = Math.toRadians(ticksAlive * ORBIT_SPEED);
            for (int i = 0; i < blades.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / BLADE_COUNT;
                double angle = baseAngle + orbitalOffset;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double z = Math.sin(angle) * ORBIT_RADIUS;

                // Synchronized yaw +/-10 deg on 4-second cycle (all at same time)
                float yaw = (float) Math.toRadians(10) * (float) Math.sin(ticksAlive * Math.PI / 40);
                Location loc = ringCenter.clone().add(x, 0, z);
                blades.get(i).entity().teleport(loc);
                blades.get(i).rotate((float) angle + yaw, 0, 1, 0);
                blades.get(i).interpolation(2, 0);
            }

            // CRIT sparkle trails from blade tips
            if (ticksAlive % 7 == 0) {
                for (BlockDisplayHandle blade : blades) {
                    Location tipLoc = blade.entity().getLocation().clone().add(0, 1.5, 0);
                    center.getWorld().spawnParticle(Particle.CRIT, tipLoc, 2, 0.1, 0.1, 0.1, 0.02);
                }
            }

            // CRIT sound when blade passes north vector
            if (ticksAlive % 100 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(ringCenter, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.2f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MidBladeRingDelta(plugin); }
    }

    // ================================================================
    // #13 -- HIGH BLADE RING EPSILON (Y+15, Clockwise, Tilted)
    // 5 obsidian blades at radius 6, tilted orbital plane 30 deg,
    // fast CW at 2.0 deg/tick. Phase 2 activation.
    // ================================================================
    public static class HighBladeRingEpsilon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blades = new ArrayList<>();
        private static final int BLADE_COUNT = 5;
        private static final double ORBIT_RADIUS = 6.0;
        private static final float ORBIT_SPEED = 2.0f;
        private static final float TILT_BASE = (float) Math.toRadians(30);

        public HighBladeRingEpsilon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("high_blade_ring_epsilon", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location ringCenter = center.clone().add(0, 15, 0);
            for (int i = 0; i < BLADE_COUNT; i++) {
                double angle = (2 * Math.PI * i) / BLADE_COUNT;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double tiltY = Math.sin(angle) * Math.sin(TILT_BASE) * ORBIT_RADIUS;
                double z = Math.sin(angle) * Math.cos(TILT_BASE) * ORBIT_RADIUS;
                Location loc = ringCenter.clone().add(x, tiltY, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.2f, 1.8f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                blades.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(ringCenter, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.25f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location ringCenter = center.clone().add(0, 15, 0);

            // Orbital plane tilt wobble: +/-10 deg on 12-second (240-tick) cycle
            float tiltWobble = (float) Math.toRadians(10) * (float) Math.sin(ticksAlive * Math.PI / 120);
            float currentTilt = TILT_BASE + tiltWobble;

            double orbitalOffset = Math.toRadians(ticksAlive * ORBIT_SPEED);
            for (int i = 0; i < blades.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / BLADE_COUNT;
                double angle = baseAngle + orbitalOffset;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double tiltY = Math.sin(angle) * Math.sin(currentTilt) * ORBIT_RADIUS;
                double z = Math.sin(angle) * Math.cos(currentTilt) * ORBIT_RADIUS;
                Location loc = ringCenter.clone().add(x, tiltY, z);
                blades.get(i).entity().teleport(loc);

                float tangentAngle = (float) (angle + Math.PI / 2);
                blades.get(i).rotate(tangentAngle, 0, 1, 0);
                blades.get(i).interpolation(2, 0);
            }

            // DRAGON_BREATH trail from each blade
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle blade : blades) {
                    center.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                            blade.entity().getLocation(), 4, 0.2, 0.2, 0.2, 0);
                }
            }

            // Flap sound every 200 ticks (10 sec)
            if (ticksAlive % 200 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(ringCenter, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.25f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HighBladeRingEpsilon(plugin); }
    }

    // ================================================================
    // #14 -- HIGH BLADE RING ZETA (Y+15, Counter-Clockwise, Tilted)
    // 4 amethyst blades at radius 9, opposite tilt to Epsilon,
    // CCW at 2.5 deg/tick. Wobble out of phase with Epsilon.
    // ================================================================
    public static class HighBladeRingZeta extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> blades = new ArrayList<>();
        private static final int BLADE_COUNT = 4;
        private static final double ORBIT_RADIUS = 9.0;
        private static final float ORBIT_SPEED = -2.5f;
        private static final float TILT_BASE = (float) Math.toRadians(-30); // opposite tilt

        public HighBladeRingZeta(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("high_blade_ring_zeta", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location ringCenter = center.clone().add(0, 15, 0);
            for (int i = 0; i < BLADE_COUNT; i++) {
                double angle = (2 * Math.PI * i) / BLADE_COUNT;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double tiltY = Math.sin(angle) * Math.sin(TILT_BASE) * ORBIT_RADIUS;
                double z = Math.sin(angle) * Math.cos(TILT_BASE) * ORBIT_RADIUS;
                Location loc = ringCenter.clone().add(x, tiltY, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 2.0f, 0.5f).glow(180, 0, 200).interpolation(2, 0);
                blades.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(ringCenter, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.25f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location ringCenter = center.clone().add(0, 15, 0);

            // Wobble phase-inverted from Epsilon (+ PI)
            float tiltWobble = (float) Math.toRadians(10) * (float) Math.sin(ticksAlive * Math.PI / 120 + Math.PI);
            float currentTilt = TILT_BASE + tiltWobble;

            double orbitalOffset = Math.toRadians(ticksAlive * ORBIT_SPEED);
            for (int i = 0; i < blades.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / BLADE_COUNT;
                double angle = baseAngle + orbitalOffset;
                double x = Math.cos(angle) * ORBIT_RADIUS;
                double tiltY = Math.sin(angle) * Math.sin(currentTilt) * ORBIT_RADIUS;
                double z = Math.sin(angle) * Math.cos(currentTilt) * ORBIT_RADIUS;
                Location loc = ringCenter.clone().add(x, tiltY, z);
                blades.get(i).entity().teleport(loc);

                float tangentAngle = (float) (angle - Math.PI / 2);
                blades.get(i).rotate(tangentAngle, 0, 1, 0);
                blades.get(i).interpolation(2, 0);
            }

            // TOTEM trail from each blade
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle blade : blades) {
                    center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                            blade.entity().getLocation(), 3, 0.2, 0.2, 0.2, 0.01);
                }
            }

            // Flap sound every 200 ticks
            if (ticksAlive % 200 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(ringCenter, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.25f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HighBladeRingZeta(plugin); }
    }

    // ================================================================
    // #15 -- BLADE RING CONNECTOR SPOKES (Y+5 to Y+15)
    // 6 extremely thin vertical obsidian pillars at radius 8, 60 deg apart.
    // Static reference frame, organic breathing on thickness.
    // ================================================================
    public static class BladeRingConnectors extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spokes = new ArrayList<>();
        private static final int SPOKE_COUNT = 6;
        private static final double SPOKE_RADIUS = 8.0;

        public BladeRingConnectors(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blade_ring_connectors", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SPOKE_COUNT; i++) {
                double angle = (2 * Math.PI * i) / SPOKE_COUNT;
                double x = Math.cos(angle) * SPOKE_RADIUS;
                double z = Math.sin(angle) * SPOKE_RADIUS;
                Location loc = center.clone().add(x, 5, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.05f, 10.0f, 0.05f).glow(128, 0, 255).interpolation(3, 0);
                spokes.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Organic breathing: random Z-scale noise per spoke
            for (int i = 0; i < spokes.size(); i++) {
                // Pseudo-random oscillation per spoke using different frequencies
                float noise = 0.01f * (float) Math.sin(ticksAlive * 0.07 + i * 1.7);
                float zScale = 0.05f + noise;
                spokes.get(i).scale(0.05f, 10.0f, zScale);
                spokes.get(i).interpolation(3, 0);
            }

            // Subtle purple ambient
            if (ticksAlive % 20 == 0) {
                for (BlockDisplayHandle spoke : spokes) {
                    DisplayBuilder.purpleDust(spoke.entity().getLocation().clone().add(0, 5, 0), 1, 0.2);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BladeRingConnectors(plugin); }
    }

    // ================================================================
    // #16 -- ORBITAL CENTER AXIS MARKER (Y+0 to Y+20)
    // Single thin end rod line from ground to sky. The visual center axis
    // of the entire ring system. END_ROD rain from top, TOTEM cross at mid.
    // ================================================================
    public static class OrbitalCenterAxis extends BlockDisplayAttack {

        private BlockDisplayHandle axisLine;

        public OrbitalCenterAxis(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbital_center_axis", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            axisLine = displayBuilder.spawnBlock(center, Material.END_ROD);
            axisLine.scale(0.1f, 20.0f, 0.1f).glow(0, 200, 255).interpolation(3, 0);
            spawnedEntities.add(axisLine.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Axis breathing: X and Z scale oscillate +/-0.02 independently
            if (axisLine != null) {
                float xScale = 0.1f + 0.02f * (float) Math.sin(ticksAlive * Math.PI / 100);
                float zScale = 0.1f + 0.02f * (float) Math.sin(ticksAlive * Math.PI / 100 + Math.PI / 3);
                axisLine.scale(xScale, 20.0f, zScale);
                axisLine.interpolation(3, 0);
            }

            // END_ROD rain from Y+20
            if (ticksAlive % 7 == 0) {
                Location top = center.clone().add(0, 20, 0);
                center.getWorld().spawnParticle(Particle.END_ROD, top, 2, 0.05, 0, 0.05, 0.02);
            }

            // TOTEM crosshair at Y+10
            if (ticksAlive % 10 == 0) {
                Location mid = center.clone().add(0, 10, 0);
                center.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, mid, 1, 0.5, 0, 0.5, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitalCenterAxis(plugin); }
    }

    // ================================================================
    // #17 -- BLADE LAUNCH BREACH POINTS (x6)
    // Small pulsing magenta glass markers at radius 12, Y+10.
    // Rapid anxious pulse, SPELL_WITCH particles, bass note on pulse peak.
    // ================================================================
    public static class BladeLaunchBreachPoints extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> centralGlass = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> crossElements = new ArrayList<>();
        private static final int POINT_COUNT = 6;
        private static final double POINT_RADIUS = 12.0;

        public BladeLaunchBreachPoints(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blade_launch_breach_points", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < POINT_COUNT; i++) {
                double angle = (2 * Math.PI * i) / POINT_COUNT;
                double x = Math.cos(angle) * POINT_RADIUS;
                double z = Math.sin(angle) * POINT_RADIUS;
                Location loc = center.clone().add(x, 10, z);

                // Central magenta glass
                BlockDisplayHandle glass = displayBuilder.spawnBlock(loc, Material.MAGENTA_STAINED_GLASS);
                glass.scale(0.4f, 0.4f, 0.4f).glow(180, 0, 200).interpolation(3, 0);
                centralGlass.add(glass);
                spawnedEntities.add(glass.entity());

                // 4 small amethyst cross elements
                List<BlockDisplayHandle> cross = new ArrayList<>();
                double[][] crossOff = {{0.3, 0, 0}, {-0.3, 0, 0}, {0, 0, 0.3}, {0, 0, -0.3}};
                for (double[] off : crossOff) {
                    BlockDisplayHandle c = displayBuilder.spawnBlock(
                            loc.clone().add(off[0], off[1], off[2]), Material.AMETHYST_BLOCK);
                    c.scale(0.1f, 0.1f, 0.1f).glow(128, 0, 255);
                    cross.add(c);
                    spawnedEntities.add(c.entity());
                }
                crossElements.add(cross);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rapid pulse: scale between 0.4 and 0.6 on 1.5-sec (30-tick) cycle
            float pulse = 0.5f + 0.1f * (float) Math.sin(ticksAlive * Math.PI / 15);
            for (BlockDisplayHandle glass : centralGlass) {
                glass.scale(pulse, pulse, pulse);
                glass.interpolation(3, 0);
            }

            // SPELL_WITCH particles from each marker
            if (ticksAlive % 2 == 0) {
                for (BlockDisplayHandle glass : centralGlass) {
                    center.getWorld().spawnParticle(Particle.WITCH,
                            glass.entity().getLocation(), 6, 0.5, 0.5, 0.5, 0.01);
                }
            }

            // Bass note on pulse peak (every 30 ticks, staggered per marker)
            for (int i = 0; i < centralGlass.size(); i++) {
                int stagger = i * 5;
                if ((ticksAlive + stagger) % 30 == 0) {
                    DisplayBuilder.playSound(centralGlass.get(i).entity().getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BladeLaunchBreachPoints(plugin); }
    }

    // ================================================================
    // #18 -- BLADE DEBRIS FIELD (Post-Launch Residue)
    // Scattered static obsidian blade debris on arena floor at random
    // positions. Accumulates up to 24 embedded blades at random tilts.
    // ================================================================
    public static class BladeDebrisField extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> debrisList = new ArrayList<>();
        private static final int MAX_DEBRIS = 24;
        private int spawnedCount = 0;

        public BladeDebrisField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blade_debris_field", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn initial scattered debris
            for (int i = 0; i < 8; i++) {
                spawnDebrisBlade(center);
            }
        }

        private void spawnDebrisBlade(Location center) {
            if (spawnedCount >= MAX_DEBRIS) return;

            double range = 20.0;
            double rx = (Math.random() - 0.5) * range * 2;
            double rz = (Math.random() - 0.5) * range * 2;
            float yHeight = 1.0f + (float) (Math.random() * 2.0);
            Location loc = center.clone().add(rx, 0.5, rz);

            // Random material from blade set
            Material[] bladeMats = {Material.OBSIDIAN, Material.DARK_PRISMARINE, Material.AMETHYST_BLOCK};
            Material mat = bladeMats[(int) (Math.random() * bladeMats.length)];

            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
            // Random tilt 30-75 deg as if struck into ground
            float tiltAngle = (float) Math.toRadians(30 + Math.random() * 45);
            float yawAngle = (float) (Math.random() * Math.PI * 2);
            h.scale(0.15f, 0.8f, 0.3f).glow(128, 0, 255)
                    .rotate(tiltAngle, (float) Math.cos(yawAngle), 0, (float) Math.sin(yawAngle));
            debrisList.add(h);
            spawnedEntities.add(h.entity());
            spawnedCount++;

            DisplayBuilder.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.6f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spawn additional debris every ~60 ticks (3 sec)
            if (ticksAlive % 60 == 0 && ticksAlive > 0) {
                spawnDebrisBlade(center);
            }

            // Particle emission from embedded blades: mixed colors
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < Math.min(debrisList.size(), 6); i++) {
                    BlockDisplayHandle h = debrisList.get(i);
                    Location bladeLoc = h.entity().getLocation().clone().add(0, 0.3, 0);
                    if (i % 3 == 0) {
                        center.getWorld().spawnParticle(Particle.CRIT, bladeLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    } else if (i % 3 == 1) {
                        center.getWorld().spawnParticle(Particle.DRAGON_BREATH, bladeLoc, 1, 0.1, 0.1, 0.1, 0);
                    } else {
                        center.getWorld().spawnParticle(Particle.SMOKE, bladeLoc, 1, 0.1, 0.1, 0.1, 0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BladeDebrisField(plugin); }
    }
}
