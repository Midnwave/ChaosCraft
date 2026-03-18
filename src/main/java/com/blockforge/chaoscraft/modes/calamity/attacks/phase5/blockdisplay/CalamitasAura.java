package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.blockdisplay;

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
 * Phase 5E Block Display -- GROUP 5: CALAMITAS AURA STRUCTURES (#39-50)
 * 12 structures forming Supreme Calamitas's personal aura system.
 * These are tracked to her position and represent her visual presence --
 * from subtle robe trailing particles to the iconic totem apex.
 *
 * NOTE: Particle-only structures (#39-41, #44-45, #48) use a single
 * invisible anchor BlockDisplay at her position as the entity tracker.
 * The visual output is particles only, spawned from computed positions.
 *
 * Structures:
 *  #39  RobeTrailingEmitters     -- 8-point crimson dust trailing her robe hem
 *  #40  HandEmissionLeft          -- Left hand crimson vortex, attack profile switch
 *  #41  HandEmissionRight         -- Right hand mirrored, counter-clockwise vortex
 *  #42  ObsidianCrownShards       -- 7 obsidian fragments orbiting her head
 *  #43  ShadowProjection          -- Ground-level shadow tracking her XZ
 *  #44  CrimsonEyeBeams           -- Directional eye light beams (Phase 2+)
 *  #45  AscensionDescensionAura   -- Burst aura during altitude changes
 *  #46  OrbitingToolsOfCalamity   -- 5 representative items orbiting close
 *  #47  HaloDiskAbove             -- 12 end rod "blaze rod" halo above head
 *  #48  Phase4FullBodyAura        -- Full-body gradient particle emission
 *  #49  SpectralArrowPersonalOrbit -- 16 end rod "arrows" fast orbit (Phase 4)
 *  #50  TotemApexDisplay           -- Oversized sea lantern apex, gold fountain
 *
 * Rules applied:
 * - NO status effects
 * - Calamitas palette: crimson(200,0,50), brimstone(255,100,0), soul blue(0,150,255),
 *   purple(128,0,255), white(240,240,255)
 * - Damage HP 4.0-14.0
 * - AxisAngle4f ONLY (never Quaternionf)
 * - Static DisplayBuilder methods
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Location center = getCenter(); if (center == null) return; EVERY onTick
 */
public final class CalamitasAura {

    private CalamitasAura() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new RobeTrailingEmitters(plugin));
        registry.register(new HandEmissionLeft(plugin));
        registry.register(new HandEmissionRight(plugin));
        registry.register(new ObsidianCrownShards(plugin));
        registry.register(new ShadowProjection(plugin));
        registry.register(new CrimsonEyeBeams(plugin));
        registry.register(new AscensionDescensionAura(plugin));
        registry.register(new OrbitingToolsOfCalamity(plugin));
        registry.register(new HaloDiskAbove(plugin));
        registry.register(new Phase4FullBodyAura(plugin));
        registry.register(new SpectralArrowPersonalOrbit(plugin));
        registry.register(new TotemApexDisplay(plugin));
    }

    // ================================================================
    // #39 -- ROBE TRAILING EMITTERS
    // 8 particle emitter points around her robe hem (-1.2 to +1.2 XZ,
    // -1.5 to -2.5 Y below her). Crimson dust trailing on movement.
    // Emission rate scales with movement speed.
    // ================================================================
    public static class RobeTrailingEmitters extends BlockDisplayAttack {

        private BlockDisplayHandle anchor;

        public RobeTrailingEmitters(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_robe_trailing", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Invisible anchor at her position
            anchor = displayBuilder.spawnBlock(center, Material.BLACKSTONE);
            anchor.scale(0.01f, 0.01f, 0.01f).brightness(0, 0);
            spawnedEntities.add(anchor.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 8 emitter points around her hem
            for (int i = 0; i < 8; i++) {
                double angle = (2.0 * Math.PI * i) / 8.0;
                double xOff = Math.cos(angle) * 1.2;
                double zOff = Math.sin(angle) * 1.2;
                double yOff = -1.5 - (i % 2) * 1.0; // Alternate -1.5 and -2.5

                Location emitLoc = center.clone().add(xOff, yOff, zOff);

                // Crimson dust: emission rate at default (idle ~5/sec -> every 4 ticks per emitter)
                if (ticksAlive % 4 == i % 4) {
                    DisplayBuilder.crimsonDust(emitLoc, 2, 0.3);
                }

                // Dark crimson upward drift
                if (ticksAlive % 5 == (i + 2) % 5) {
                    DisplayBuilder.dustParticles(emitLoc, 1, 0.2, 80, 0, 0, 1.2f);
                }
            }

            // Slower breathing pulse during descent simulation (every 60 ticks)
            if (ticksAlive % 60 < 30) {
                // Brighter phase
                if (ticksAlive % 2 == 0) {
                    Location hemCenter = center.clone().add(0, -2.0, 0);
                    DisplayBuilder.crimsonDust(hemCenter, 3, 0.8);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RobeTrailingEmitters(plugin); }
    }

    // ================================================================
    // #40 -- HAND EMISSION LEFT
    // Particle vortex at left hand position (-0.7 XZ, Y+2.1).
    // Idle: crimson dust cyclonic spiral, soul fire, rare gold sparkle.
    // Attack profile: crimson + spell_witch + CRIT burst.
    // ================================================================
    public static class HandEmissionLeft extends BlockDisplayAttack {

        private BlockDisplayHandle anchor;

        public HandEmissionLeft(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_hand_left", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            anchor = displayBuilder.spawnBlock(center.clone().add(-0.7, 2.1, 0), Material.BLACKSTONE);
            anchor.scale(0.01f, 0.01f, 0.01f).brightness(0, 0);
            spawnedEntities.add(anchor.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location handLoc = center.clone().add(-0.7, 2.1, 0);

            // Cyclonic spiral: dust particles with tangential velocity
            double spiralAngle = Math.toRadians(ticksAlive * 3.0);
            double spiralR = 0.15;
            double sx = Math.cos(spiralAngle) * spiralR;
            double sz = Math.sin(spiralAngle) * spiralR;
            Location spiralLoc = handLoc.clone().add(sx, 0, sz);

            // Idle crimson dust: 6/sec
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.crimsonDust(spiralLoc, 1, 0.15);
            }

            // Soul fire flame: 2/sec
            if (ticksAlive % 10 == 0) {
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, handLoc, 1, 0.08, 0.08, 0.08, 0.005);
            }

            // Rare gold sparkle: ~0.5/sec
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.dustParticles(handLoc, 1, 0.1, 255, 200, 50, 0.6f);
            }

            // Attack profile simulation: burst every 200 ticks for 40 ticks
            if (ticksAlive % 200 < 40 && ticksAlive % 200 > 0) {
                DisplayBuilder.crimsonDust(handLoc, 3, 0.2);
                center.getWorld().spawnParticle(Particle.WITCH, handLoc, 2, 0.1, 0.1, 0.1, 0.02);
                if (ticksAlive % 2 == 0) {
                    center.getWorld().spawnParticle(Particle.CRIT, handLoc, 2, 0.15, 0.1, 0.15, 0.03);
                }

                // Attack profile activation sound
                if (ticksAlive % 200 == 1) {
                    DisplayBuilder.playSound(handLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.15f, 1.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HandEmissionLeft(plugin); }
    }

    // ================================================================
    // #41 -- HAND EMISSION RIGHT
    // Mirror of #40 at +0.7 XZ. Counter-clockwise spiral.
    // Slightly dimmer idle (gold sparkle at 0.3/sec vs 0.5/sec).
    // ================================================================
    public static class HandEmissionRight extends BlockDisplayAttack {

        private BlockDisplayHandle anchor;

        public HandEmissionRight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_hand_right", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            anchor = displayBuilder.spawnBlock(center.clone().add(0.7, 2.1, 0), Material.BLACKSTONE);
            anchor.scale(0.01f, 0.01f, 0.01f).brightness(0, 0);
            spawnedEntities.add(anchor.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location handLoc = center.clone().add(0.7, 2.1, 0);

            // Counter-clockwise spiral
            double spiralAngle = Math.toRadians(-ticksAlive * 3.0);
            double spiralR = 0.15;
            double sx = Math.cos(spiralAngle) * spiralR;
            double sz = Math.sin(spiralAngle) * spiralR;
            Location spiralLoc = handLoc.clone().add(sx, 0, sz);

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.crimsonDust(spiralLoc, 1, 0.15);
            }

            if (ticksAlive % 10 == 0) {
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, handLoc, 1, 0.08, 0.08, 0.08, 0.005);
            }

            // Slightly less frequent gold sparkle: ~0.3/sec
            if (ticksAlive % 66 == 0) {
                DisplayBuilder.dustParticles(handLoc, 1, 0.1, 255, 200, 50, 0.6f);
            }

            // Attack profile burst
            if (ticksAlive % 200 < 40 && ticksAlive % 200 > 0) {
                DisplayBuilder.crimsonDust(handLoc, 3, 0.2);
                center.getWorld().spawnParticle(Particle.WITCH, handLoc, 2, 0.1, 0.1, 0.1, 0.02);
                if (ticksAlive % 2 == 0) {
                    center.getWorld().spawnParticle(Particle.CRIT, handLoc, 2, 0.15, 0.1, 0.15, 0.03);
                }

                if (ticksAlive % 200 == 1) {
                    DisplayBuilder.playSound(handLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.15f, 1.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HandEmissionRight(plugin); }
    }

    // ================================================================
    // #42 -- OBSIDIAN CROWN SHARDS (x7)
    // 7 small obsidian fragments orbiting her head at radius 0.8.
    // Multiple orbital planes (0, 30, 60, 15 deg inclination).
    // Varying speeds (2.5-3.5 deg/tick). Realignment every 600 ticks.
    // SMOKE + soul blue pulse. Phase 4: radius expands, speed increases.
    // ================================================================
    public static class ObsidianCrownShards extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shards = new ArrayList<>();
        private final float[] shardScaleX = {0.2f, 0.2f, 0.2f, 0.2f, 0.2f, 0.3f, 0.4f};
        private final float[] shardScaleY = {0.4f, 0.4f, 0.4f, 0.4f, 0.4f, 0.6f, 0.8f};
        private final float[] shardScaleZ = {0.15f, 0.15f, 0.15f, 0.15f, 0.15f, 0.2f, 0.25f};
        private final double[] inclinations = {0, 0, 0, 30, 30, 60, 15}; // degrees
        private final double[] speeds = {3.0, 2.8, 3.2, 2.5, 3.5, 2.7, 3.0}; // deg/tick
        private double orbitRadius = 0.8;

        public ObsidianCrownShards(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crown_shards", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location headPos = center.clone().add(0, 3.2, 0);

            for (int i = 0; i < 7; i++) {
                double angle = (2.0 * Math.PI * i) / 7.0;
                double px = Math.cos(angle) * orbitRadius;
                double pz = Math.sin(angle) * orbitRadius;
                Location loc = headPos.clone().add(px, 0, pz);

                BlockDisplayHandle shard = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                shard.scale(shardScaleX[i], shardScaleY[i], shardScaleZ[i]);
                shard.glow(128, 0, 255).interpolation(2, 0);
                shards.add(shard);
                spawnedEntities.add(shard.entity());
            }

            DisplayBuilder.playSound(headPos, Sound.BLOCK_STONE_PLACE, 0.2f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location headPos = center.clone().add(0, 3.2, 0);

            // Phase 4 radius expansion (after tick 3000)
            if (ticksAlive > 3000 && ticksAlive <= 3300) {
                orbitRadius = 0.8 + 0.6 * ((ticksAlive - 3000) / 300.0);
            }

            for (int i = 0; i < shards.size(); i++) {
                double speed = speeds[i] * (ticksAlive > 3000 ? 1.67 : 1.0); // Phase 4: 5 deg/tick
                double orbitAngle = Math.toRadians(ticksAlive * speed);
                double incRad = Math.toRadians(inclinations[i]);

                // Compute 3D orbital position with inclination
                double px = Math.cos(orbitAngle) * orbitRadius;
                double flatZ = Math.sin(orbitAngle) * orbitRadius;
                double py = flatZ * Math.sin(incRad);
                double pz = flatZ * Math.cos(incRad);

                Location newLoc = headPos.clone().add(px, py, pz);
                shards.get(i).entity().teleport(newLoc);

                // SMOKE trail
                if (ticksAlive % 20 == i % 20) {
                    center.getWorld().spawnParticle(Particle.SMOKE, newLoc, 1, 0.03, 0.03, 0.03, 0.002);
                }

                // Soul blue pulse (rare)
                if (ticksAlive % 66 == i * 9 % 66) {
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, newLoc, 1, 0.02, 0.02, 0.02, 0.001);
                }
            }

            // Realignment event every 600 ticks
            if (ticksAlive % 600 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(headPos, Sound.BLOCK_STONE_PLACE, 0.2f, 0.9f);
            }

            // Periodic click
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(headPos, Sound.BLOCK_STONE_PLACE, 0.1f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ObsidianCrownShards(plugin); }
    }

    // ================================================================
    // #43 -- SHADOW PROJECTION
    // Flat obsidian panels at ground level tracking her XZ.
    // Center panel 2.5x0.05x2.5 + 4 radial elongation panels.
    // Elongation adjusts with altitude. Shadow stretches behind movement.
    // ================================================================
    public static class ShadowProjection extends BlockDisplayAttack {

        private BlockDisplayHandle centerShadow;
        private final List<BlockDisplayHandle> radialPanels = new ArrayList<>();

        public ShadowProjection(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_shadow_projection", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location groundLoc = center.clone();
            groundLoc.setY(center.getY() - 18); // Assume hover at Y+18 offset
            Location shadowLoc = new Location(w, center.getX(), center.getY() + 0.05, center.getZ());

            centerShadow = displayBuilder.spawnBlock(shadowLoc, Material.OBSIDIAN);
            centerShadow.scale(2.5f, 0.05f, 2.5f).glow(0, 0, 0).brightness(2, 2);
            spawnedEntities.add(centerShadow.entity());

            // 4 radial elongation panels
            for (int i = 0; i < 4; i++) {
                double angle = Math.toRadians(90.0 * i);
                double px = Math.cos(angle) * 2.0;
                double pz = Math.sin(angle) * 2.0;
                Location panelLoc = shadowLoc.clone().add(px, 0, pz);

                BlockDisplayHandle panel = displayBuilder.spawnBlock(panelLoc, Material.OBSIDIAN);
                panel.scale(0.8f, 0.05f, 4.0f).glow(0, 0, 0).brightness(2, 2);
                // Orient each panel outward
                float rotAngle = (float) (angle);
                panel.rotate(rotAngle, 0, 1, 0);
                radialPanels.add(panel);
                spawnedEntities.add(panel.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Shadow at ground level (Y+0.05 of arena floor, approximate)
            Location shadowCenter = center.clone();
            shadowCenter.setY(center.getY() + 0.05);

            if (centerShadow != null) {
                centerShadow.entity().teleport(shadowCenter);
            }

            // Elongation based on simulated altitude (default 4.0, adjust dynamically)
            float elongation = 4.0f; // Default hover altitude elongation

            for (int i = 0; i < radialPanels.size(); i++) {
                double angle = Math.toRadians(90.0 * i);
                double px = Math.cos(angle) * 2.0;
                double pz = Math.sin(angle) * 2.0;
                Location panelLoc = shadowCenter.clone().add(px, 0, pz);
                radialPanels.get(i).entity().teleport(panelLoc);
                radialPanels.get(i).scale(0.8f, 0.05f, elongation);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowProjection(plugin); }
    }

    // ================================================================
    // #44 -- CRIMSON EYE BEAMS
    // Two directional particle beams from eye positions (Y+3.0, +/-0.25 XZ).
    // END_ROD at 3/sec, 0.02 spread, aimed forward. 2.4-block range.
    // Phase 4: switches to bright white dust, extends to 6 blocks.
    // ================================================================
    public static class CrimsonEyeBeams extends BlockDisplayAttack {

        private BlockDisplayHandle anchor;

        public CrimsonEyeBeams(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_eye_beams", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            anchor = displayBuilder.spawnBlock(center.clone().add(0, 3, 0), Material.BLACKSTONE);
            anchor.scale(0.01f, 0.01f, 0.01f).brightness(0, 0);
            spawnedEntities.add(anchor.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Two eye positions
            Location leftEye = center.clone().add(-0.25, 3.0, 0);
            Location rightEye = center.clone().add(0.25, 3.0, 0);

            // Beam direction: forward (south by default, would track facing in real impl)
            double beamDirX = 0;
            double beamDirZ = 1;

            // Determine beam properties based on phase (tick 3000+ = Phase 4)
            boolean phase4 = ticksAlive > 3000;
            double beamLength = phase4 ? 6.0 : 2.4;
            int emitInterval = phase4 ? 3 : 7;

            if (ticksAlive % emitInterval == 0) {
                for (double d = 0.3; d <= beamLength; d += 0.4) {
                    Location leftBeamPt = leftEye.clone().add(beamDirX * d, 0, beamDirZ * d);
                    Location rightBeamPt = rightEye.clone().add(beamDirX * d, 0, beamDirZ * d);

                    if (phase4) {
                        DisplayBuilder.dustParticles(leftBeamPt, 1, 0.02, 255, 255, 255, 0.5f);
                        DisplayBuilder.dustParticles(rightBeamPt, 1, 0.02, 255, 255, 255, 0.5f);
                    } else {
                        center.getWorld().spawnParticle(Particle.END_ROD, leftBeamPt, 1, 0.02, 0.02, 0.02, 0.001);
                        center.getWorld().spawnParticle(Particle.END_ROD, rightBeamPt, 1, 0.02, 0.02, 0.02, 0.001);
                    }
                }
            }

            // Enderman stare ambient
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 3, 0), Sound.ENTITY_ENDERMAN_TELEPORT, 0.05f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonEyeBeams(plugin); }
    }

    // ================================================================
    // #45 -- ASCENSION/DESCENSION AURA
    // Burst effects at altitude changes. Descent: radial crimson disk +
    // soul fire cone up + ash down. Ascent: cone burst + spell_witch +
    // gold sparkle. Single-tick bursts + continuous drift during transit.
    // ================================================================
    public static class AscensionDescensionAura extends BlockDisplayAttack {

        private BlockDisplayHandle anchor;
        private boolean hasDescended = false;
        private boolean hasAscended = false;

        public AscensionDescensionAura(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_ascension_aura", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(2000);
            config.setCooldownTicks(800);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            anchor = displayBuilder.spawnBlock(center, Material.BLACKSTONE);
            anchor.scale(0.01f, 0.01f, 0.01f).brightness(0, 0);
            spawnedEntities.add(anchor.entity());

            // Descent burst at spawn
            DisplayBuilder.crimsonDust(center, 30, 3.0);
            center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1, 0), 15, 0.5, 1.0, 0.5, 0.05);
            center.getWorld().spawnParticle(Particle.ASH, center.clone().add(0, -0.5, 0), 20, 1.0, 0.5, 1.0, 0.02);

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 1.3f);
            hasDescended = true;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Continuous downward drift during transit
            if (ticksAlive < 800) {
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.dustParticles(center, 2, 0.5, 80, 0, 0, 1.2f);
                }
            }

            // Ascension burst at tick 800
            if (ticksAlive == 800 && !hasAscended) {
                hasAscended = true;
                // Cone burst upward
                DisplayBuilder.crimsonDust(center, 40, 3.0);
                center.getWorld().spawnParticle(Particle.WITCH, center, 20, 2.0, 1.0, 2.0, 0.05);
                DisplayBuilder.dustParticles(center, 10, 1.5, 255, 200, 50, 0.8f);

                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.3f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AscensionDescensionAura(plugin); }
    }

    // ================================================================
    // #46 -- ORBITING TOOLS OF CALAMITY (x5)
    // 5 representative BlockDisplay items orbiting her at radius 2.5,
    // Y+1.5. Each has unique material and particle signature.
    // Orbit at 2 deg/tick CCW. Radius oscillates 2.2-2.8 on 60-tick period.
    // ================================================================
    public static class OrbitingToolsOfCalamity extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tools = new ArrayList<>();
        private static final Material[] TOOL_MATERIALS = {
            Material.NETHER_WART_BLOCK,   // Scythe stand-in
            Material.END_ROD,              // Staff stand-in
            Material.RED_STAINED_GLASS,    // Blade stand-in
            Material.END_ROD,              // Trident stand-in
            Material.SEA_LANTERN           // Nether star stand-in
        };
        private static final float[][] TOOL_SCALES = {
            {0.15f, 1.0f, 0.08f},  // Scythe: thin blade
            {0.08f, 0.9f, 0.08f},  // Staff: thin rod
            {0.12f, 0.8f, 0.04f},  // Blade: flat edge
            {0.1f, 1.1f, 0.1f},    // Trident: elongated
            {0.3f, 0.3f, 0.3f}     // Star: cube
        };

        public OrbitingToolsOfCalamity(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_orbiting_tools", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 5; i++) {
                double angle = Math.toRadians(72.0 * i);
                double px = Math.cos(angle) * 2.5;
                double pz = Math.sin(angle) * 2.5;
                Location loc = center.clone().add(px, 1.5, pz);

                BlockDisplayHandle tool = displayBuilder.spawnBlock(loc, TOOL_MATERIALS[i]);
                tool.scale(TOOL_SCALES[i][0], TOOL_SCALES[i][1], TOOL_SCALES[i][2]);

                // Color by type
                switch (i) {
                    case 0 -> tool.glow(80, 0, 0);      // Scythe: dark crimson
                    case 1 -> tool.glow(255, 100, 0);    // Staff: brimstone
                    case 2 -> tool.glow(200, 0, 50);     // Blade: crimson
                    case 3 -> tool.glow(240, 240, 255);  // Trident: white
                    case 4 -> tool.glow(240, 240, 255);  // Star: white
                }
                tool.interpolation(2, 0);
                tools.add(tool);
                spawnedEntities.add(tool.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.2f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            double orbitAngle = Math.toRadians(ticksAlive * 2.0);

            for (int i = 0; i < tools.size(); i++) {
                double baseAngle = Math.toRadians(72.0 * i);
                double angle = baseAngle + orbitAngle;

                // Radius oscillation with phase offset per tool
                double phaseOffset = (2.0 * Math.PI * i) / 5.0;
                double radius = 2.5 + 0.3 * Math.sin(ticksAlive * 2.0 * Math.PI / 60.0 + phaseOffset);

                double px = Math.cos(angle) * radius;
                double pz = Math.sin(angle) * radius;
                Location newLoc = center.clone().add(px, 1.5, pz);
                tools.get(i).entity().teleport(newLoc);

                // Per-tool particles
                if (ticksAlive % 5 == i) {
                    switch (i) {
                        case 0 -> DisplayBuilder.dustParticles(newLoc, 1, 0.15, 80, 0, 0, 1.0f);
                        case 1 -> {
                            center.getWorld().spawnParticle(Particle.FLAME, newLoc, 1, 0.05, 0.05, 0.05, 0.005);
                            center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, newLoc, 1, 0.05, 0.02, 0.05, 0);
                        }
                        case 2 -> DisplayBuilder.crimsonDust(newLoc, 1, 0.15);
                        case 3 -> {
                            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, newLoc, 1, 0.05, 0.05, 0.05, 0.01);
                            center.getWorld().spawnParticle(Particle.CRIT, newLoc, 1, 0.05, 0.05, 0.05, 0.01);
                        }
                        case 4 -> center.getWorld().spawnParticle(Particle.END_ROD, newLoc, 2, 0.15, 0.15, 0.15, 0.01);
                    }
                }

                // Star tool: spin on all axes
                if (i == 4) {
                    float spinAngle = (float) Math.toRadians(ticksAlive * 3.0);
                    tools.get(i).rotate(spinAngle, 0.5f, 1.0f, 0.3f);
                }
            }

            // Sound every 72 ticks (once per revolution pass through north)
            if (ticksAlive % 72 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.2f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitingToolsOfCalamity(plugin); }
    }

    // ================================================================
    // #47 -- HALO DISK ABOVE
    // 12 end rod BlockDisplays in flat horizontal ring above her head.
    // Radius 1.2, Y+4.5. Orbit clockwise at 4 deg/tick.
    // FLAME at 1/sec per rod. Dynamic tilt based on movement direction.
    // ================================================================
    public static class HaloDiskAbove extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> haloRods = new ArrayList<>();
        private static final int COUNT = 12;
        private static final double RADIUS = 1.2;
        private static final double Y_OFF = 4.5;

        public HaloDiskAbove(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_halo_disk", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location haloCenter = center.clone().add(0, Y_OFF, 0);

            for (int i = 0; i < COUNT; i++) {
                double angle = (2.0 * Math.PI * i) / COUNT;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;
                Location loc = haloCenter.clone().add(px, 0, pz);

                BlockDisplayHandle rod = displayBuilder.spawnBlock(loc, Material.END_ROD);
                rod.scale(0.08f, 0.3f, 0.08f);
                // Orient tangentially
                float tangentAngle = (float) (angle + Math.PI / 2.0);
                rod.rotate(tangentAngle, 0, 1, 0);
                rod.glow(255, 100, 0).interpolation(2, 0);
                haloRods.add(rod);
                spawnedEntities.add(rod.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location haloCenter = center.clone().add(0, Y_OFF, 0);

            // Clockwise orbit at 4 deg/tick
            double orbitAngle = -Math.toRadians(ticksAlive * 4.0);

            for (int i = 0; i < haloRods.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / COUNT;
                double angle = baseAngle + orbitAngle;
                double px = Math.cos(angle) * RADIUS;
                double pz = Math.sin(angle) * RADIUS;

                Location newLoc = haloCenter.clone().add(px, 0, pz);
                haloRods.get(i).entity().teleport(newLoc);

                // Update tangent orientation
                float tangentAngle = (float) (angle + Math.PI / 2.0);
                haloRods.get(i).rotate(tangentAngle, 0, 1, 0);
                haloRods.get(i).interpolation(2, 0);

                // FLAME from each rod
                if (ticksAlive % 20 == i % 12) {
                    center.getWorld().spawnParticle(Particle.FLAME, newLoc, 1, 0.03, 0.03, 0.03, 0.003);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HaloDiskAbove(plugin); }
    }

    // ================================================================
    // #48 -- PHASE 4 FULL BODY AURA
    // 12 emission points vertically Y+0 to Y+3.5 at 0.3-block intervals.
    // Color gradient: dark crimson at feet -> crimson mid -> white at head.
    // Rates pulse with attack cadence (80%/150%/200% of base).
    // ================================================================
    public static class Phase4FullBodyAura extends BlockDisplayAttack {

        private BlockDisplayHandle anchor;

        public Phase4FullBodyAura(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_phase4_body_aura", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(4000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            anchor = displayBuilder.spawnBlock(center, Material.BLACKSTONE);
            anchor.scale(0.01f, 0.01f, 0.01f).brightness(0, 0);
            spawnedEntities.add(anchor.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Attack cadence pulse: cycle between idle/windup/execution
            // Simulated 200-tick cycle: 120 idle (80%), 40 windup (150%), 40 execution (200%)
            int cyclePos = ticksAlive % 200;
            float multiplier;
            if (cyclePos < 120) multiplier = 0.8f;
            else if (cyclePos < 160) multiplier = 1.5f;
            else multiplier = 2.0f;

            int baseInterval = Math.max(1, (int) (3.0 / multiplier));

            if (ticksAlive % baseInterval == 0) {
                // 12 emission points from Y+0 to Y+3.5
                for (int p = 0; p < 12; p++) {
                    double yOff = p * 0.3;
                    Location emitLoc = center.clone().add(0, yOff, 0);

                    if (yOff <= 1.2) {
                        // Foot zone: dark crimson + lava
                        DisplayBuilder.dustParticles(emitLoc, 2, 0.3, 80, 0, 0, 1.5f);
                        if (p % 3 == 0) {
                            center.getWorld().spawnParticle(Particle.DRIPPING_LAVA, emitLoc, 1, 0.2, 0.1, 0.2, 0);
                        }
                    } else if (yOff <= 2.4) {
                        // Mid zone: crimson + soul fire
                        DisplayBuilder.crimsonDust(emitLoc, 2, 0.3);
                        if (p % 2 == 0) {
                            center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, emitLoc, 1, 0.15, 0.15, 0.15, 0.01);
                        }
                    } else {
                        // Head zone: spell_witch + end rod
                        center.getWorld().spawnParticle(Particle.WITCH, emitLoc, 2, 0.15, 0.15, 0.15, 0.02);
                        center.getWorld().spawnParticle(Particle.END_ROD, emitLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Phase4FullBodyAura(plugin); }
    }

    // ================================================================
    // #49 -- SPECTRAL ARROW PERSONAL ORBIT (x16)
    // 16 end rod "arrows" at radius 3.5, Y+1.8. Fast: 8 deg/tick CCW.
    // Tips outward. END_ROD + CRIT trails. 4 marked with ELECTRIC_SPARK
    // at 90-deg intervals. Radius oscillates 3.0-4.0 on 30-tick period.
    // ================================================================
    public static class SpectralArrowPersonalOrbit extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> arrows = new ArrayList<>();
        private static final int COUNT = 16;
        private static final double BASE_RADIUS = 3.5;
        private static final double Y_OFF = 1.8;

        public SpectralArrowPersonalOrbit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_spectral_arrow_orbit", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(4000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < COUNT; i++) {
                double angle = (2.0 * Math.PI * i) / COUNT;
                double px = Math.cos(angle) * BASE_RADIUS;
                double pz = Math.sin(angle) * BASE_RADIUS;
                Location loc = center.clone().add(px, Y_OFF, pz);

                BlockDisplayHandle arrow = displayBuilder.spawnBlock(loc, Material.END_ROD);
                arrow.scale(0.06f, 0.8f, 0.06f);
                // Tip pointing radially outward
                float outAngle = (float) Math.toRadians(90);
                float axisX = (float) -Math.sin(angle);
                float axisZ = (float) Math.cos(angle);
                arrow.rotate(outAngle, axisX, 0, axisZ);
                arrow.glow(240, 240, 255).interpolation(2, 0);
                arrows.add(arrow);
                spawnedEntities.add(arrow.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.08f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fast orbit: 8 deg/tick CCW
            double orbitAngle = Math.toRadians(ticksAlive * 8.0);

            // Radius oscillation: 3.0 to 4.0 on 30-tick period
            double radiusOsc = BASE_RADIUS + 0.5 * Math.sin(ticksAlive * 2.0 * Math.PI / 30.0);

            for (int i = 0; i < arrows.size(); i++) {
                double baseAngle = (2.0 * Math.PI * i) / COUNT;
                double angle = baseAngle + orbitAngle;
                double px = Math.cos(angle) * radiusOsc;
                double pz = Math.sin(angle) * radiusOsc;

                Location newLoc = center.clone().add(px, Y_OFF, pz);
                arrows.get(i).entity().teleport(newLoc);

                // Update rotation
                float outAngle = (float) Math.toRadians(90);
                float axisX = (float) -Math.sin(angle);
                float axisZ = (float) Math.cos(angle);
                arrows.get(i).rotate(outAngle, axisX, 0, axisZ);
                arrows.get(i).interpolation(2, 0);

                // END_ROD from tip
                if (ticksAlive % 10 == i % 10) {
                    center.getWorld().spawnParticle(Particle.END_ROD, newLoc, 1, 0.05, 0.05, 0.05, 0.005);
                }

                // CRIT from tail
                if (ticksAlive % 7 == i % 7) {
                    center.getWorld().spawnParticle(Particle.CRIT, newLoc, 1, 0.05, 0.05, 0.05, 0.01);
                }

                // 4 marked arrows at 90-deg intervals: extra ELECTRIC_SPARK
                if (i % 4 == 0 && ticksAlive % 4 == 0) {
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, newLoc, 2, 0.08, 0.08, 0.08, 0.015);
                }
            }

            // Sound every 45 ticks (once per revolution)
            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.08f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpectralArrowPersonalOrbit(plugin); }
    }

    // ================================================================
    // #50 -- TOTEM APEX DISPLAY
    // Single oversized sea lantern at Y+5.5, tracked. Scale 1.5.
    // Spins on Y at 2 deg/tick. Y oscillation +/-0.3 on 100-tick period.
    // Gold dust fountain at 15/sec + SOUL_FIRE_FLAME up + crimson dust down.
    // Phase 4: scale grows to 2.2, emission rate triples.
    // Death animation: scale compresses to 0 with spin acceleration.
    // ================================================================
    public static class TotemApexDisplay extends BlockDisplayAttack {

        private BlockDisplayHandle totemBlock;
        private static final double Y_OFF = 5.5;

        public TotemApexDisplay(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_totem_apex", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location totemLoc = center.clone().add(0, Y_OFF, 0);
            totemBlock = displayBuilder.spawnBlock(totemLoc, Material.SEA_LANTERN);
            totemBlock.scale(1.5f, 1.5f, 1.5f).glow(255, 200, 50).interpolation(5, 0);
            spawnedEntities.add(totemBlock.entity());

            DisplayBuilder.playSound(totemLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Y oscillation
            double yOsc = 0.3 * Math.sin(ticksAlive * 2.0 * Math.PI / 100.0);

            // Phase 4 scale growth (after tick 3000)
            float currentScale;
            if (ticksAlive > 3000 && ticksAlive <= 3600) {
                currentScale = 1.5f + 0.7f * ((ticksAlive - 3000) / 600.0f);
            } else if (ticksAlive > 3600) {
                currentScale = 2.2f;
                // Increased oscillation amplitude in Phase 4
                yOsc = 0.8 * Math.sin(ticksAlive * 2.0 * Math.PI / 100.0);
            } else {
                currentScale = 1.5f;
            }

            Location totemLoc = center.clone().add(0, Y_OFF + yOsc, 0);

            if (totemBlock != null) {
                totemBlock.entity().teleport(totemLoc);

                // Spin on Y at 2 deg/tick
                float spinAngle = (float) Math.toRadians(ticksAlive * 2.0);
                totemBlock.rotate(spinAngle, 0, 1, 0);
                totemBlock.scale(currentScale, currentScale, currentScale);
                totemBlock.interpolation(5, 0);
            }

            // Gold dust fountain (TOTEM-like particles)
            boolean phase4 = ticksAlive > 3000;
            int goldInterval = phase4 ? 1 : 2;
            if (ticksAlive % goldInterval == 0) {
                int count = phase4 ? 6 : 3;
                DisplayBuilder.dustParticles(totemLoc, count, 0.5, 255, 200, 50, 0.8f);
            }

            // SOUL_FIRE_FLAME upward
            if (ticksAlive % 7 == 0) {
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, totemLoc.clone().add(0, 0.5, 0), 1, 0.1, 0.15, 0.1, 0.015);
            }

            // Crimson dust downward
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.crimsonDust(totemLoc.clone().add(0, -0.5, 0), 1, 0.3);
            }

            // Totem activation sound: every 200 ticks (Phases 1-3), every 60 in Phase 4
            int soundInterval = phase4 ? 60 : 200;
            if (ticksAlive % soundInterval == 0) {
                DisplayBuilder.playSound(totemLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 0.6f);
            }

            // Death animation: last 100 ticks
            int remaining = config.getDurationTicks() - ticksAlive;
            if (remaining <= 100 && remaining > 0) {
                // Scale compresses to 0
                float deathScale = currentScale * (remaining / 100.0f);
                totemBlock.scale(deathScale, deathScale, deathScale);

                // Spin acceleration: up to 20 deg/tick
                float deathSpin = (float) Math.toRadians(ticksAlive * (2.0 + (100 - remaining) * 0.18));
                totemBlock.rotate(deathSpin, 0, 1, 0);

                // Burst particles during first 40 ticks of death
                if (remaining > 60) {
                    DisplayBuilder.dustParticles(totemLoc, 10, 1.0, 255, 200, 50, 1.0f);
                }

                // Final sound at death start
                if (remaining == 100) {
                    DisplayBuilder.playSound(totemLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TotemApexDisplay(plugin); }
    }
}
