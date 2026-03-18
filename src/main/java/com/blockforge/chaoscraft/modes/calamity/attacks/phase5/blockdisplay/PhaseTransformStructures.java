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
 * Phase 5E Block Display -- GROUP 4: PHASE TRANSFORMATION STRUCTURES (#29-38)
 * 10 structures that appear at phase transition thresholds. These mark
 * the escalation points of the Supreme Calamitas fight -- each transition
 * permanently alters the arena's visual state.
 *
 * Structures:
 *  #29  MonolithCrackGeometry      -- Red glass cracks on all 8 monoliths at Phase 1->2
 *  #30  ArenaFloorCrimsonVeins     -- 8 crimson streak activations + border markers
 *  #31  CrimsonCrystalEruption     -- 12 crystal sites erupting in outer zone
 *  #32  FaultCrackLavaActivation   -- Lava particle layer on existing fault crack
 *  #33  ArenaBorderInwardCollapse  -- 16 leaning obsidian wall segments at Phase 3->4
 *  #34  CentralSigilEnhancement    -- Arm spikes, node expansion, glow layer
 *  #35  DeathResonanceFullActivate -- Crimson beam, extra wraps, base crystals
 *  #36  SkyTransitionMarker        -- Nether star at Y+40, always present, pulses on transitions
 *  #37  PhaseTransitionShockwave   -- Expanding ring of obsidian fragments
 *  #38  CrimsonEndgameColumns      -- 4 tall obsidian columns rising at Phase 4 start
 *
 * Rules applied:
 * - NO status effects
 * - Calamitas palette: crimson(200,0,50), brimstone(255,100,0), soul blue(0,150,255)
 * - Damage HP 4.0-14.0
 * - AxisAngle4f ONLY (never Quaternionf)
 * - Static DisplayBuilder methods
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - Location center = getCenter(); if (center == null) return; EVERY onTick
 */
public final class PhaseTransformStructures {

    private PhaseTransformStructures() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new MonolithCrackGeometry(plugin));
        registry.register(new ArenaFloorCrimsonVeins(plugin));
        registry.register(new CrimsonCrystalEruption(plugin));
        registry.register(new FaultCrackLavaActivation(plugin));
        registry.register(new ArenaBorderInwardCollapse(plugin));
        registry.register(new CentralSigilEnhancement(plugin));
        registry.register(new DeathResonanceFullActivate(plugin));
        registry.register(new SkyTransitionMarker(plugin));
        registry.register(new PhaseTransitionShockwave(plugin));
        registry.register(new CrimsonEndgameColumns(plugin));
    }

    // ================================================================
    // #29 -- MONOLITH CRACK GEOMETRY
    // Red stained glass crack panels on all 8 monolith surfaces.
    // 3-5 cracks per monolith, spreading from center outward.
    // Sequence: pylons first, E/W, then S, then N. Over 8 seconds.
    // DUST(180,0,0) from each crack at 6/sec for 5s, then 2/sec permanent.
    // ================================================================
    public static class MonolithCrackGeometry extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crackPanels = new ArrayList<>();
        private final List<Location> crackLocations = new ArrayList<>();
        private static final double[][] MONOLITH_POSITIONS = {
            // Diagonal pylons first (indices 0-3), then E/W (4-5), then S (6), then N (7)
            {20, 0, -20}, {-20, 0, -20}, {20, 0, 20}, {-20, 0, 20},   // NE, NW, SE, SW pylons
            {34, 0, 0}, {-34, 0, 0},                                     // East, West
            {0, 0, 34},                                                    // South
            {0, 0, -34}                                                    // North (last, largest)
        };

        public MonolithCrackGeometry(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_monolith_cracks", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            // Cracks are spawned progressively via onTick
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spawn cracks progressively over 160 ticks (8 seconds)
            // Each monolith gets cracks at its scheduled time
            int[] spawnSchedule = {0, 10, 20, 30, 60, 70, 110, 140}; // ticks per monolith
            int[] cracksPerMonolith = {3, 3, 3, 3, 4, 4, 4, 5};

            for (int m = 0; m < MONOLITH_POSITIONS.length; m++) {
                if (ticksAlive == spawnSchedule[m]) {
                    Location monolith = center.clone().add(
                        MONOLITH_POSITIONS[m][0], 0, MONOLITH_POSITIONS[m][2]);

                    for (int c = 0; c < cracksPerMonolith[m]; c++) {
                        float yOffset = 2.0f + c * 2.0f;
                        float rotAngle = (float) Math.toRadians(10 + c * 8 + m * 3);
                        float crackLength = 1.5f + c * 0.7f;

                        Location crackLoc = monolith.clone().add(0.05, yOffset, 0);
                        BlockDisplayHandle crack = displayBuilder.spawnBlock(crackLoc, Material.RED_STAINED_GLASS);
                        crack.scale(0.04f, crackLength, 0.04f);
                        crack.rotate(rotAngle, 0, 0, 1);
                        crack.glow(200, 0, 50).interpolation(12, 0);
                        crackPanels.add(crack);
                        crackLocations.add(crackLoc);
                        spawnedEntities.add(crack.entity());
                    }

                    DisplayBuilder.playSound(monolith, Sound.BLOCK_GLASS_BREAK, 0.25f, 0.7f);

                    // Completion sound for the monolith
                    DisplayBuilder.playSound(monolith, Sound.BLOCK_STONE_PLACE, 0.4f, 0.8f);
                }
            }

            // DUST(180,0,0) from cracks
            if (!crackLocations.isEmpty()) {
                for (int i = 0; i < crackLocations.size(); i++) {
                    int crackAge = ticksAlive - (i < 12 ? 0 : i < 20 ? 60 : i < 28 ? 110 : 140);
                    if (crackAge < 0) continue;

                    // 6/sec for first 100 ticks (5s), then 2/sec
                    int interval = crackAge < 100 ? 4 : 10;
                    if (ticksAlive % interval == i % interval) {
                        DisplayBuilder.crimsonDust(crackLocations.get(i), 2, 0.2);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MonolithCrackGeometry(plugin); }
    }

    // ================================================================
    // #30 -- ARENA FLOOR CRIMSON VEINS
    // 8 radial crimson dust particle streaks from center outward.
    // Red stained glass end markers at 20-block border.
    // Pulsing emission rate synced to 120-tick period.
    // ================================================================
    public static class ArenaFloorCrimsonVeins extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> endMarkers = new ArrayList<>();

        public ArenaFloorCrimsonVeins(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_floor_crimson_veins", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 red stained glass end markers at streak termination points
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(45.0 * i);
                double mx = Math.cos(angle) * 20.0;
                double mz = Math.sin(angle) * 20.0;
                Location markerLoc = center.clone().add(mx, 0.05, mz);
                BlockDisplayHandle marker = displayBuilder.spawnBlock(markerLoc, Material.RED_STAINED_GLASS);
                marker.scale(0.3f, 0.5f, 0.3f).glow(200, 0, 50);
                endMarkers.add(marker);
                spawnedEntities.add(marker.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.1f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pulsing crimson dust along 8 radial streaks
            float emissionPulse = 1.0f + 3.0f * (float) Math.sin(ticksAlive * 2.0 * Math.PI / 120.0);
            int baseInterval = Math.max(1, (int) (20.0 / emissionPulse));

            if (ticksAlive % baseInterval == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(45.0 * i);
                    // Emit along the streak from sigil outward
                    for (double d = 2.0; d <= 20.0; d += 3.0) {
                        double px = Math.cos(angle) * d;
                        double pz = Math.sin(angle) * d;
                        Location dustLoc = center.clone().add(px, 0.2, pz);
                        DisplayBuilder.crimsonDust(dustLoc, 1, 0.3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ArenaFloorCrimsonVeins(plugin); }
    }

    // ================================================================
    // #31 -- CRIMSON CRYSTAL ERUPTION (x12 sites)
    // 12 crystal sites in outer zone (22-35 block radius).
    // Each site: 3 red stained glass spires (center + 2 flanking at 20 deg lean).
    // Scale interpolation from 0 to full over 80 ticks per site.
    // Subtle vibration after eruption. Phase 4: slow rotation as units.
    // ================================================================
    public static class CrimsonCrystalEruption extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crystals = new ArrayList<>();
        private final List<Location> siteLocations = new ArrayList<>();
        private static final int SITES = 12;

        public CrimsonCrystalEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crimson_crystal_eruption", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            java.util.Random rng = new java.util.Random(center.hashCode() + 31);
            for (int i = 0; i < SITES; i++) {
                double angle = (2.0 * Math.PI * i) / SITES + rng.nextDouble() * 0.3;
                double radius = 22.0 + rng.nextDouble() * 13.0;
                double sx = Math.cos(angle) * radius;
                double sz = Math.sin(angle) * radius;
                Location siteLoc = center.clone().add(sx, 0, sz);
                siteLocations.add(siteLoc);

                float centerHeight = 1.8f + rng.nextFloat() * 1.4f; // 1.8 to 3.2

                // Central spire
                BlockDisplayHandle centerCrystal = displayBuilder.spawnBlock(siteLoc, Material.RED_STAINED_GLASS);
                centerCrystal.scale(0.7f, centerHeight, 0.5f).glow(200, 0, 50).interpolation(80, i * 20);
                crystals.add(centerCrystal);
                spawnedEntities.add(centerCrystal.entity());

                // Left flanking spire (70% scale, lean -20 deg)
                float flankHeight = centerHeight * 0.7f;
                Location leftLoc = siteLoc.clone().add(-0.8, 0, 0);
                BlockDisplayHandle leftCrystal = displayBuilder.spawnBlock(leftLoc, Material.RED_STAINED_GLASS);
                float leftLean = (float) Math.toRadians(-20);
                leftCrystal.scale(0.5f, flankHeight, 0.35f).rotate(leftLean, 0, 0, 1);
                leftCrystal.glow(200, 0, 50).interpolation(80, i * 20);
                crystals.add(leftCrystal);
                spawnedEntities.add(leftCrystal.entity());

                // Right flanking spire (70% scale, lean +20 deg)
                Location rightLoc = siteLoc.clone().add(0.8, 0, 0);
                BlockDisplayHandle rightCrystal = displayBuilder.spawnBlock(rightLoc, Material.RED_STAINED_GLASS);
                float rightLean = (float) Math.toRadians(20);
                rightCrystal.scale(0.5f, flankHeight, 0.35f).rotate(rightLean, 0, 0, 1);
                rightCrystal.glow(200, 0, 50).interpolation(80, i * 20);
                crystals.add(rightCrystal);
                spawnedEntities.add(rightCrystal.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Subtle Y-scale vibration after eruption
            for (int i = 0; i < crystals.size(); i++) {
                int siteIdx = i / 3;
                int eruptionEnd = siteIdx * 20 + 80;
                if (ticksAlive > eruptionEnd) {
                    // Small vibration: Y scale oscillates 1.0 to 1.03
                    Transformation t = crystals.get(i).entity().getTransformation();
                    float baseY = t.getScale().y;
                    float vibY = baseY * (1.0f + 0.03f * (float) Math.sin(ticksAlive * 2.0 * Math.PI / 30.0));
                    crystals.get(i).scale(t.getScale().x, vibY, t.getScale().z);
                }
            }

            // DUST(180,0,0) from crystal tops
            if (ticksAlive % 3 == 0) {
                for (int s = 0; s < siteLocations.size(); s++) {
                    if (ticksAlive > s * 20 + 80) {
                        Location topLoc = siteLocations.get(s).clone().add(0, 3.0, 0);
                        DisplayBuilder.crimsonDust(topLoc, 2, 0.5);
                    }
                }
            }

            // Eruption sound per site
            for (int s = 0; s < SITES; s++) {
                if (ticksAlive == s * 20) {
                    DisplayBuilder.playSound(siteLocations.get(s), Sound.BLOCK_GLASS_BREAK, 0.4f, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonCrystalEruption(plugin); }
    }

    // ================================================================
    // #32 -- FAULT CRACK LAVA ACTIVATION
    // No new geometry -- pure particle emitter along NW-SE fault crack.
    // LAVA at 3/sec per segment, FLAME at 2/sec, SMOKE at 4/sec.
    // Pulses: LAVA triples briefly when synced to crimson sky stream.
    // ================================================================
    public static class FaultCrackLavaActivation extends BlockDisplayAttack {

        private final List<Location> crackSegments = new ArrayList<>();

        public FaultCrackLavaActivation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_fault_crack_lava", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(5.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pre-compute NW-SE fault crack path (diagonal through center)
            for (double t = -25; t <= 25; t += 2.0) {
                Location seg = center.clone().add(-t * 0.707, 0.1, t * 0.707);
                crackSegments.add(seg);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.15f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Check for crimson sky pulse sync (every 200 ticks)
            boolean isPulse = (ticksAlive % 200 < 4);

            for (int i = 0; i < crackSegments.size(); i++) {
                Location seg = crackSegments.get(i);

                // LAVA (triple during pulse)
                int lavaRate = isPulse ? 1 : 7;
                if (ticksAlive % lavaRate == i % lavaRate) {
                    center.getWorld().spawnParticle(Particle.LAVA, seg, 1, 0.3, 0.1, 0.3, 0);
                }

                // FLAME
                if (ticksAlive % 10 == i % 10) {
                    center.getWorld().spawnParticle(Particle.FLAME, seg, 1, 0.2, 0.1, 0.2, 0.005);
                }

                // SMOKE
                if (ticksAlive % 5 == i % 5) {
                    center.getWorld().spawnParticle(Particle.SMOKE, seg.clone().add(0, 0.3, 0), 1, 0.2, 0.2, 0.2, 0.005);
                }
            }

            // Ambient lava sound
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.15f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FaultCrackLavaActivation(plugin); }
    }

    // ================================================================
    // #33 -- ARENA BORDER INWARD COLLAPSE
    // 16 obsidian wall panels at 22-block radius, each tilted 15 deg
    // inward. Appear sequentially clockwise over 6 seconds (120 ticks).
    // DUST(80,0,0) from inner faces. Phase 4: SOUL_FIRE_FLAME added.
    // ================================================================
    public static class ArenaBorderInwardCollapse extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallPanels = new ArrayList<>();
        private static final int PANEL_COUNT = 16;

        public ArenaBorderInwardCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_border_collapse", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            // Panels spawn progressively via onTick
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spawn panels sequentially: 1 every 8 ticks over first 128 ticks
            if (ticksAlive < 128 && ticksAlive % 8 == 0) {
                int panelIdx = ticksAlive / 8;
                if (panelIdx >= PANEL_COUNT) return;

                double angle = (2.0 * Math.PI * panelIdx) / PANEL_COUNT;
                double px = Math.cos(angle) * 22.0;
                double pz = Math.sin(angle) * 22.0;
                Location panelLoc = center.clone().add(px, 0, pz);

                BlockDisplayHandle panel = displayBuilder.spawnBlock(panelLoc, Material.OBSIDIAN);
                // 15 deg inward tilt: lean top toward center
                float tiltAngle = (float) Math.toRadians(15);
                float tiltAxisX = (float) -Math.sin(angle);
                float tiltAxisZ = (float) Math.cos(angle);
                panel.scale(2.5f, 4.0f, 0.4f);
                panel.rotate(tiltAngle, tiltAxisX, 0, tiltAxisZ);
                // Perpendicular to radius
                float facingAngle = (float) angle;
                panel.glow(200, 0, 50).interpolation(60, 0);
                wallPanels.add(panel);
                spawnedEntities.add(panel.entity());

                DisplayBuilder.playSound(panelLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.3f, 0.6f);

                // Completion sound on last panel
                if (panelIdx == PANEL_COUNT - 1) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.15f, 1.6f);
                }
            }

            // DUST(80,0,0) from inner faces of existing panels
            if (ticksAlive % 7 == 0) {
                for (int i = 0; i < wallPanels.size(); i++) {
                    double angle = (2.0 * Math.PI * i) / PANEL_COUNT;
                    double px = Math.cos(angle) * 21.5; // Slightly inward from panel
                    double pz = Math.sin(angle) * 21.5;
                    Location dustLoc = center.clone().add(px, 2, pz);
                    DisplayBuilder.dustParticles(dustLoc, 1, 0.3, 80, 0, 0, 1.2f);
                }
            }

            // Phase 4 addition: soul fire flame at tick 3000+
            if (ticksAlive > 3000 && ticksAlive % 20 == 0) {
                for (int i = 0; i < wallPanels.size(); i++) {
                    double angle = (2.0 * Math.PI * i) / PANEL_COUNT;
                    double px = Math.cos(angle) * 22.0;
                    double pz = Math.sin(angle) * 22.0;
                    Location flameLoc = center.clone().add(px, 2, pz);
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, flameLoc, 1, 0.2, 0.3, 0.2, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ArenaBorderInwardCollapse(plugin); }
    }

    // ================================================================
    // #34 -- CENTRAL SIGIL ENHANCEMENT
    // 3 enhancements to the sigil: 8 arm spikes, center node expansion,
    // red stained glass glow overlay. Pulsing brightness synced to attacks.
    // ================================================================
    public static class CentralSigilEnhancement extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> armSpikes = new ArrayList<>();
        private BlockDisplayHandle expandedNode;
        private BlockDisplayHandle glowLayer;

        public CentralSigilEnhancement(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_sigil_enhancement", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 1. Arm spikes: obsidian at each arm tip (radius 8)
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(45.0 * i);
                double sx = Math.cos(angle) * 8.0;
                double sz = Math.sin(angle) * 8.0;
                Location spikeLoc = center.clone().add(sx, 0.1, sz);
                BlockDisplayHandle spike = displayBuilder.spawnBlock(spikeLoc, Material.OBSIDIAN);
                spike.scale(0.5f, 1.0f, 0.5f).glow(128, 0, 255);
                armSpikes.add(spike);
                spawnedEntities.add(spike.entity());
            }

            // 2. Center node expansion: from 0.5 to 1.5 over 120 ticks
            Location nodeLoc = center.clone().add(0, 0.05, 0);
            expandedNode = displayBuilder.spawnBlock(nodeLoc, Material.CRYING_OBSIDIAN);
            expandedNode.scale(0.5f, 0.5f, 0.5f).glow(0, 150, 255).interpolation(120, 0);
            spawnedEntities.add(expandedNode.entity());

            // 3. Glow layer: flat red glass overlay
            Location glowLoc = center.clone().add(0, 0.1, 0);
            glowLayer = displayBuilder.spawnBlock(glowLoc, Material.RED_STAINED_GLASS);
            glowLayer.scale(20.0f, 0.05f, 20.0f).glow(200, 0, 50).brightness(8, 8);
            spawnedEntities.add(glowLayer.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Node expansion over first 120 ticks
            if (ticksAlive <= 120 && expandedNode != null) {
                float scale = 0.5f + 1.0f * (ticksAlive / 120.0f);
                expandedNode.scale(scale, scale * 1.5f, scale);
            }

            // SOUL_FIRE_FLAME from expanded node
            if (ticksAlive % 2 == 0 && ticksAlive > 120) {
                Location nodeLoc = center.clone().add(0, 0.5, 0);
                center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, nodeLoc, 2, 0.1, 0.2, 0.1, 0.02);
            }

            // Glow layer brightness pulse: simulating attack sync at 80-tick period
            if (glowLayer != null) {
                float brightness = 8.0f + 7.0f * (float) Math.sin(ticksAlive * 2.0 * Math.PI / 80.0);
                int bv = Math.min(15, Math.max(0, (int) brightness));
                glowLayer.brightness(bv, bv);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CentralSigilEnhancement(plugin); }
    }

    // ================================================================
    // #35 -- DEATH RESONANCE FULL ACTIVATION
    // Crimson beam upward from pillar top, 4 extra glass wraps,
    // 4 base crystal growths. Beam oscillates width on 80-tick period.
    // ================================================================
    public static class DeathResonanceFullActivate extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> extraWraps = new ArrayList<>();
        private final List<BlockDisplayHandle> baseCrystals = new ArrayList<>();

        public DeathResonanceFullActivate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_death_resonance_full", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(8.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location pillar = center.clone().add(21, 0, -21);

            // 4 additional red stained glass wraps at different heights
            float[] wrapHeights = {3.0f, 6.5f, 9.5f, 12.0f};
            float[][] wrapFaceOffsets = {{0.6f, 0}, {-0.6f, 0}, {0, 0.6f}, {0, -0.6f}};

            for (int i = 0; i < 4; i++) {
                Location wrapLoc = pillar.clone().add(wrapFaceOffsets[i][0], wrapHeights[i], wrapFaceOffsets[i][1]);
                BlockDisplayHandle wrap = displayBuilder.spawnBlock(wrapLoc, Material.RED_STAINED_GLASS);
                float rotAngle = (float) Math.toRadians(20 + i * 8);
                wrap.scale(0.1f, 3.5f, 0.1f).rotate(rotAngle, 0, 0, 1);
                wrap.glow(200, 0, 50).brightness(12, 12);
                extraWraps.add(wrap);
                spawnedEntities.add(wrap.entity());
            }

            // 4 base crystal growths leaning outward at 25 deg
            for (int i = 0; i < 4; i++) {
                double angle = Math.toRadians(90.0 * i + 45);
                double cx = Math.cos(angle) * 1.2;
                double cz = Math.sin(angle) * 1.2;
                Location crystalLoc = pillar.clone().add(cx, 0, cz);

                BlockDisplayHandle crystal = displayBuilder.spawnBlock(crystalLoc, Material.RED_STAINED_GLASS);
                float leanAngle = (float) Math.toRadians(25);
                float leanAxisX = (float) Math.cos(angle);
                float leanAxisZ = (float) Math.sin(angle);
                crystal.scale(0.5f, 2.0f, 0.5f).rotate(leanAngle, leanAxisX, 0, leanAxisZ);
                crystal.glow(200, 0, 50).interpolation(60, 0);
                baseCrystals.add(crystal);
                spawnedEntities.add(crystal.entity());
            }

            DisplayBuilder.playSound(pillar, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location pillar = center.clone().add(21, 0, -21);

            // Crimson beam: DUST(180,0,0) upward from pillar top
            if (ticksAlive % 1 == 0) {
                // Beam width oscillation
                float spread = 0.1f + 0.3f * (float) Math.abs(Math.sin(ticksAlive * 2.0 * Math.PI / 80.0));
                for (double y = 14; y < 60; y += 2.0) {
                    Location beamLoc = pillar.clone().add(0, y, 0);
                    DisplayBuilder.crimsonDust(beamLoc, 1, spread);
                }
            }

            // Wrap brightness pulse: 40-tick offset from sigil
            for (BlockDisplayHandle wrap : extraWraps) {
                float brightness = 8.0f + 7.0f * (float) Math.sin(ticksAlive * 2.0 * Math.PI / 80.0 + Math.PI);
                int bv = Math.min(15, Math.max(0, (int) brightness));
                wrap.brightness(bv, bv);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeathResonanceFullActivate(plugin); }
    }

    // ================================================================
    // #36 -- SKY TRANSITION MARKER
    // Single sea lantern at Y+40, spinning at 3 deg/tick.
    // TOTEM-like gold dust falling downward. Pulses at phase transitions.
    // Always present from fight start.
    // ================================================================
    public static class SkyTransitionMarker extends BlockDisplayAttack {

        private BlockDisplayHandle starBlock;

        public SkyTransitionMarker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_sky_transition_marker", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location starLoc = center.clone().add(0, 40, 0);
            starBlock = displayBuilder.spawnBlock(starLoc, Material.SEA_LANTERN);
            starBlock.scale(0.3f, 0.3f, 0.3f).glow(240, 240, 255).interpolation(3, 0);
            spawnedEntities.add(starBlock.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location starLoc = center.clone().add(0, 40, 0);

            // Spinning at 3 deg/tick
            float rotAngle = (float) Math.toRadians(ticksAlive * 3.0);
            starBlock.rotate(rotAngle, 0, 1, 0);
            starBlock.interpolation(3, 0);

            // Gold dust falling downward
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(starLoc, 2, 0.3, 255, 200, 50, 0.8f);
            }

            // Phase transition pulse simulation (every 3000 ticks)
            if (ticksAlive % 3000 == 0 && ticksAlive > 0) {
                // Scale pulse: 0.3 -> 0.8 -> 0.3
                starBlock.scale(0.8f, 0.8f, 0.8f);
                starBlock.interpolation(10, 0);

                // END_ROD burst
                center.getWorld().spawnParticle(Particle.END_ROD, starLoc, 30, 1.5, 1.5, 1.5, 0.05);

                DisplayBuilder.playSound(starLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.15f, 0.5f);
            }

            // Return to normal scale after pulse
            if (ticksAlive % 3000 == 30) {
                starBlock.scale(0.3f, 0.3f, 0.3f);
                starBlock.interpolation(20, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyTransitionMarker(plugin); }
    }

    // ================================================================
    // #37 -- PHASE TRANSITION SHOCKWAVE
    // Expanding ring of 16 small obsidian fragments from center.
    // Radius 0.5 -> 40 over 25 ticks. Y rises 1.5 -> 5.
    // DUST(180,0,0) at 15/sec per fragment. Turbulent Y oscillation.
    // Fires once, purely visual. Despawn at edge.
    // ================================================================
    public static class PhaseTransitionShockwave extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> fragments = new ArrayList<>();
        private final double[] fragmentAngles = new double[16];
        private static final int FRAGMENT_COUNT = 16;

        public PhaseTransitionShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_shockwave", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(50);
            config.setCooldownTicks(3000);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < FRAGMENT_COUNT; i++) {
                double angle = (2.0 * Math.PI * i) / FRAGMENT_COUNT;
                fragmentAngles[i] = angle;

                Location loc = center.clone().add(Math.cos(angle) * 0.5, 1.5, Math.sin(angle) * 0.5);
                BlockDisplayHandle frag = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                frag.scale(0.4f, 0.4f, 0.4f).glow(200, 0, 50);
                fragments.add(frag);
                spawnedEntities.add(frag.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive > 25) {
                // All fragments despawn at tick 25
                return;
            }

            // Linear expansion: 0.5 to 40 over 25 ticks
            double radius = 0.5 + (39.5 * ticksAlive / 25.0);
            // Y rises from 1.5 to 5.0
            double baseY = 1.5 + (3.5 * ticksAlive / 25.0);

            for (int i = 0; i < fragments.size(); i++) {
                double angle = fragmentAngles[i];
                // Turbulent Y oscillation
                double yTurbulence = 0.3 * Math.sin(ticksAlive * Math.PI / 2.0 + i);
                double px = Math.cos(angle) * radius;
                double pz = Math.sin(angle) * radius;

                Location newLoc = center.clone().add(px, baseY + yTurbulence, pz);
                fragments.get(i).entity().teleport(newLoc);

                // Dense crimson dust trail
                DisplayBuilder.crimsonDust(newLoc, 3, 0.3);
            }

            // Edge arrival sound
            if (ticksAlive == 25) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseTransitionShockwave(plugin); }
    }

    // ================================================================
    // #38 -- CRIMSON ENDGAME COLUMNS (x4)
    // Tall obsidian columns at 16 blocks from center N/S/E/W.
    // Y+0 to Y+30. Rise via Y-scale interpolation over 160 ticks.
    // 4 red glass horizontal bands per column. Crimson particle
    // fountains from tops. Slow X/Z scale pulse (1.5 to 1.7, 100-tick).
    // ================================================================
    public static class CrimsonEndgameColumns extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> columnBodies = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> columnBands = new ArrayList<>();
        private static final double[][] POSITIONS = {{0, 0, -16}, {0, 0, 16}, {16, 0, 0}, {-16, 0, 0}};

        public CrimsonEndgameColumns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crimson_columns", AttackType.BLOCK_DISPLAY, 5));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int c = 0; c < 4; c++) {
                Location colBase = center.clone().add(POSITIONS[c][0], 0, POSITIONS[c][2]);

                // Main column: obsidian, starts small, grows to 30 tall
                BlockDisplayHandle column = displayBuilder.spawnBlock(colBase, Material.OBSIDIAN);
                column.scale(1.5f, 0.1f, 1.5f).glow(128, 0, 255).interpolation(160, 0);
                columnBodies.add(column);
                spawnedEntities.add(column.entity());

                // 4 red glass bands at Y+7, Y+14, Y+21, Y+28
                List<BlockDisplayHandle> bands = new ArrayList<>();
                float[] bandHeights = {7, 14, 21, 28};
                for (float bh : bandHeights) {
                    Location bandLoc = colBase.clone().add(0, bh, 0);
                    BlockDisplayHandle band = displayBuilder.spawnBlock(bandLoc, Material.RED_STAINED_GLASS);
                    band.scale(1.7f, 0.8f, 1.7f).glow(200, 0, 50);
                    band.brightness(0, 0); // Hidden until column reaches height
                    bands.add(band);
                    spawnedEntities.add(band.entity());
                }
                columnBands.add(bands);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Column rise over first 160 ticks
            if (ticksAlive <= 160) {
                float yScale = 30.0f * (ticksAlive / 160.0f);
                for (BlockDisplayHandle column : columnBodies) {
                    column.scale(1.5f, yScale, 1.5f);
                }

                // Sound: placement sounds during rise
                if (ticksAlive % 20 == 0) {
                    for (double[] pos : POSITIONS) {
                        Location soundLoc = center.clone().add(pos[0], yScale, pos[2]);
                        DisplayBuilder.playSound(soundLoc, Sound.BLOCK_STONE_PLACE, 0.5f, 0.6f);
                    }
                }
            }

            // Reveal bands as column reaches their height
            for (int c = 0; c < 4; c++) {
                float currentHeight = Math.min(30.0f, 30.0f * (ticksAlive / 160.0f));
                float[] bandHeights = {7, 14, 21, 28};
                for (int b = 0; b < 4; b++) {
                    if (currentHeight >= bandHeights[b]) {
                        columnBands.get(c).get(b).brightness(15, 15);
                    }
                }
            }

            // Slow X/Z scale pulse after rise completes
            if (ticksAlive > 160) {
                // N/S in phase, E/W offset by 50 ticks
                float pulseNS = 1.5f + 0.2f * (float) Math.sin(ticksAlive * 2.0 * Math.PI / 100.0);
                float pulseEW = 1.5f + 0.2f * (float) Math.sin(ticksAlive * 2.0 * Math.PI / 100.0 + Math.PI);

                columnBodies.get(0).scale(pulseNS, 30.0f, pulseNS);
                columnBodies.get(1).scale(pulseNS, 30.0f, pulseNS);
                columnBodies.get(2).scale(pulseEW, 30.0f, pulseEW);
                columnBodies.get(3).scale(pulseEW, 30.0f, pulseEW);
            }

            // Crimson particle fountains from column tops
            if (ticksAlive > 160 && ticksAlive % 2 == 0) {
                for (double[] pos : POSITIONS) {
                    Location topLoc = center.clone().add(pos[0], 30, pos[2]);
                    DisplayBuilder.crimsonDust(topLoc, 3, 1.5);
                }
            }

            // Ambient elder guardian from columns
            if (ticksAlive % 200 == 0 && ticksAlive > 160) {
                for (double[] pos : POSITIONS) {
                    DisplayBuilder.playSound(center.clone().add(pos[0], 15, pos[2]),
                        Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.1f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonEndgameColumns(plugin); }
    }
}
