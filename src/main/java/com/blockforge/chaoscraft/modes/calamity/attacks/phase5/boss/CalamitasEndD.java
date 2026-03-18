package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Supreme Calamitas — Phase 4: "The End of Everything" (24%-0% HP)
 * Attacks #181-190
 *
 * Beam combos, player curses, and the Total Annihilation Grid.
 * Damage: 44-96 hearts. Peak arena saturation attacks.
 * Calamitas colors: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255), purple(128,0,255)
 *
 * NO status effects — damage only.
 */
public final class CalamitasEndD {

    private CalamitasEndD() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new Rupture(plugin));
        registry.register(new MarkOfCalamitas(plugin));
        registry.register(new BeamLock(plugin));
        registry.register(new CrossBeamGrid(plugin));
        registry.register(new TridentTrigger(plugin));
        registry.register(new ConvergenceBeam(plugin));
        registry.register(new BrimstoneCagePhase4(plugin));
        registry.register(new TheLastBeam(plugin));
        registry.register(new BeamSweepFinale(plugin));
        registry.register(new TotalAnnihilationGrid(plugin));
    }

    // ================================================================
    // #181 — RUPTURE
    // 9 brimstone columns erupt in a 3x3 grid across highest-density zone
    // ================================================================
    public static class Rupture extends BossAttack {
        private boolean firstRuptured = false;
        private boolean secondRuptured = false;

        public Rupture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_rupture", AttackType.BOSS, 5), "calamitas");
            config.setDamage(56.0); // 28 hearts
            config.setDamageRadius(3.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(56.0);
            config.setImpactRadius(3.5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 15, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // First rupture (tick 10)
            if (ticksAlive == 10 && !firstRuptured) {
                firstRuptured = true;
                eruptZone(center, 0, 0);
            }
            // Second rupture (tick 40)
            if (ticksAlive == 40 && !secondRuptured) {
                secondRuptured = true;
                double x = (Math.random() - 0.5) * 12;
                double z = (Math.random() - 0.5) * 12;
                eruptZone(center, x, z);
            }

            // Soul fire aftermath
            if ((firstRuptured || secondRuptured) && ticksAlive > 40 && ticksAlive % 15 == 0 && ticksAlive < 140) {
                for (int i = 0; i < 3; i++) {
                    double x = (Math.random() - 0.5) * 8;
                    double z = (Math.random() - 0.5) * 8;
                    DisplayBuilder.crimsonDust(center.clone().add(x, 0.5, z), 3, 0.5);
                }
            }
        }

        private void eruptZone(Location center, double xOff, double zOff) {
            Location zoneCenter = center.clone().add(xOff, 0, zOff);
            // 3x3 grid of columns
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Location colLoc = zoneCenter.clone().add(dx * 2, 0.1, dz * 2);
                    BlockDisplayHandle column = displayBuilder.spawnBlock(colLoc, Material.MAGMA_BLOCK);
                    column.scale(1.5f, 5.0f, 1.5f).glow(255, 100, 0).interpolation(3, 0);
                    spawnedEntities.add(column.entity());
                    triggerImpactDamage(colLoc);
                }
            }
            DisplayBuilder.crimsonDust(zoneCenter, 30, 3.0);
            DisplayBuilder.crimsonDust(zoneCenter, 20, 2.0);
            DisplayBuilder.playSound(zoneCenter, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
            DisplayBuilder.playSound(zoneCenter, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Rupture(plugin); }
    }

    // ================================================================
    // #182 — MARK OF CALAMITAS
    // Rune beneath player, detonates after 5 seconds, 45 hearts
    // ================================================================
    public static class MarkOfCalamitas extends BossAttack {
        private boolean marked = false;
        private int markTick = 0;
        private Location markLocation;

        public MarkOfCalamitas(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_mark", AttackType.BOSS, 5), "calamitas");
            config.setDamage(90.0); // 45 hearts detonation
            config.setDamageRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(90.0);
            config.setImpactRadius(4.0);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            // NO telegraph
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !marked) {
                marked = true;
                markTick = ticksAlive;
                markLocation = center.clone();
                // Rune glyph beneath player
                BlockDisplayHandle rune = displayBuilder.spawnBlock(
                    markLocation.clone().add(0, 0.02, 0), Material.RED_GLAZED_TERRACOTTA);
                rune.scale(4.0f, 0.03f, 4.0f).glow(200, 0, 0).interpolation(3, 0);
                spawnedEntities.add(rune.entity());
            }

            // Rune pulse — accelerating as it approaches detonation (0-100 ticks = 5 seconds)
            if (marked && ticksAlive < 100) {
                // Follow the player (mark moves with them)
                markLocation = center.clone();
                if (!spawnedEntities.isEmpty() && spawnedEntities.get(0) != null && spawnedEntities.get(0).isValid()) {
                    spawnedEntities.get(0).teleport(markLocation.clone().add(0, 0.02, 0));
                }

                // Accelerating pulse
                int pulseRate = Math.max(2, 10 - (ticksAlive / 10));
                if (ticksAlive % pulseRate == 0) {
                    DisplayBuilder.crimsonDust(markLocation, 10, 4.0);
                    DisplayBuilder.playSound(markLocation, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f,
                        0.5f + (ticksAlive / 100.0f) * 1.5f);
                }
            }

            // Detonation (tick 100)
            if (marked && ticksAlive == 100) {
                triggerImpactDamage(markLocation);
                DisplayBuilder.crimsonDust(markLocation, 2, 2.0);
                DisplayBuilder.crimsonDust(markLocation, 40, 4.0);
                DisplayBuilder.crimsonDust(markLocation, 20, 3.0);
                DisplayBuilder.playSound(markLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
                DisplayBuilder.playSound(markLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MarkOfCalamitas(plugin); }
    }

    // ================================================================
    // #183 — BEAM LOCK
    // 6-second tracking beam + pre-seeded tridents in dodge direction
    // ================================================================
    public static class BeamLock extends BossAttack {
        private boolean beamActive = false;

        public BeamLock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_beam_lock", AttackType.BOSS, 5), "calamitas");
            config.setDamage(32.0); // 16 hearts/second beam
            config.setDamageRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 6, 0), 15, 1.5);
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 6, 0), 10, 1.0);
                }
                return;
            }

            // Beam fires and tracks (10-130 ticks = 6 seconds)
            if (ticksAlive == 10 && !beamActive) {
                beamActive = true;
                // Pre-seed 4 tridents in dodge direction
                for (int i = 0; i < 4; i++) {
                    double angle = Math.PI / 2 + (Math.random() - 0.5) * Math.PI;
                    Location tLoc = center.clone().add(Math.cos(angle) * 6, 3, Math.sin(angle) * 6);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 20, 20).interpolation(2, 0);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.7f, 1.0f);
            }

            if (beamActive && ticksAlive >= 10 && ticksAlive < 130) {
                // Beam from hand to target
                if (ticksAlive % 2 == 0) {
                    Location handLoc = center.clone().add(0, 6, 2);
                    for (int d = 0; d < 15; d++) {
                        Vector dir = center.toVector().subtract(handLoc.toVector()).normalize();
                        Location beamLoc = handLoc.clone().add(dir.multiply(d));
                        DisplayBuilder.crimsonDust(beamLoc, 3, 0.3);
                        DisplayBuilder.crimsonDust(beamLoc, 1, 0.1);
                    }
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.4f, 0.7f);
                }
            }

            // Cleanup volley (tick 130)
            if (beamActive && ticksAlive == 130) {
                for (int i = 0; i < 6; i++) {
                    DisplayBuilder.crimsonDust(center.clone().add(
                        (Math.random() - 0.5) * 6, 3, (Math.random() - 0.5) * 6), 5, 0.3);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BeamLock(plugin); }
    }

    // ================================================================
    // #184 — CROSS BEAM GRID
    // 4 wall-to-wall beams dividing arena + 32 tridents into quadrants
    // ================================================================
    public static class CrossBeamGrid extends BossAttack {
        private boolean beamsActive = false;
        private int beamStartTick = 0;

        public CrossBeamGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_cross_beam_grid", AttackType.BOSS, 5), "calamitas");
            config.setDamage(36.0); // 18 hearts/second beam
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // Grid preview on floor
            DisplayBuilder.crimsonDust(center, 20, 10.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 20 && !beamsActive) {
                beamsActive = true;
                beamStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.6f);
            }

            // 4 cardinal beams (20-120 ticks = 5 seconds)
            if (beamsActive && ticksAlive >= 20 && ticksAlive < 120) {
                int elapsed = ticksAlive - beamStartTick;
                double rotation = elapsed * (Math.PI / 300); // Slow rotation

                if (elapsed % 3 == 0) {
                    // N-S and E-W beams with slow rotation
                    for (int dir = 0; dir < 4; dir++) {
                        double angle = (Math.PI / 2) * dir + rotation;
                        int r = dir == 0 ? 200 : dir == 1 ? 200 : dir == 2 ? 180 : 160;
                        int g = dir == 0 ? 0 : dir == 1 ? 0 : dir == 2 ? 0 : 20;
                        int b = dir == 0 ? 0 : dir == 1 ? 80 : dir == 2 ? 120 : 160;
                        for (int d = 1; d <= 10; d++) {
                            Location beamLoc = center.clone().add(
                                Math.cos(angle) * d, 3, Math.sin(angle) * d);
                            DisplayBuilder.crimsonDust(beamLoc, 3, 0.3);
                        }
                    }
                }

                // Trident bombardment (8 per quadrant = 32 total over 5 seconds)
                if (elapsed % 15 == 0 && elapsed > 0) {
                    for (int q = 0; q < 4; q++) {
                        double qAngle = (Math.PI / 4) + (Math.PI / 2) * q + rotation;
                        double tDist = 3 + Math.random() * 5;
                        Location tLoc = center.clone().add(
                            Math.cos(qAngle) * tDist, 15, Math.sin(qAngle) * tDist);
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                        trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                        spawnedEntities.add(trident.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 1.0f);
                }
            }

            // Animate falling tridents
            for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                Location loc = spawnedEntities.get(i).getLocation();
                if (loc.getY() > center.getY() + 0.5) {
                    loc.subtract(0, 1.5, 0);
                    spawnedEntities.get(i).teleport(loc);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrossBeamGrid(plugin); }
    }

    // ================================================================
    // #185 — TRIDENT TRIGGER
    // 8 trigger-tridents that fire beams on impact
    // ================================================================
    public static class TridentTrigger extends BossAttack {
        private boolean fired = false;

        public TridentTrigger(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_trigger", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts trident
            config.setDamageRadius(2.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(56.0); // 28 hearts beam burst
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) { /* NO telegraph */ }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !fired) {
                fired = true;
                // 8 tridents in wide fan
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i;
                    Location tLoc = center.clone().add(0, 10, 0);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(160, 0, 0).interpolation(2, 0);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.9f);
            }

            // Fan outward (0-20 ticks)
            if (fired && ticksAlive > 0 && ticksAlive < 20) {
                for (int i = 0; i < Math.min(8, spawnedEntities.size()); i++) {
                    if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                    double angle = (2 * Math.PI / 8) * i;
                    float dist = ticksAlive * 0.8f;
                    Location flyLoc = center.clone().add(
                        Math.cos(angle) * dist, 10 - ticksAlive * 0.5, Math.sin(angle) * dist);
                    spawnedEntities.get(i).teleport(flyLoc);
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.crimsonDust(flyLoc, 2, 0.2);
                    }
                }
            }

            // Impact + beam emitters fire (tick 20)
            if (fired && ticksAlive == 20) {
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i;
                    float dist = 16;
                    Location impactLoc = center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.crimsonDust(impactLoc, 2, 0.5);
                    // Beam fires toward center from each impact point
                    for (int d = 1; d < (int)dist; d += 2) {
                        double beamX = Math.cos(angle + Math.PI) * d;
                        double beamZ = Math.sin(angle + Math.PI) * d;
                        Location beamLoc = impactLoc.clone().add(beamX, 2, beamZ);
                        DisplayBuilder.crimsonDust(beamLoc, 3, 0.3);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentTrigger(plugin); }
    }

    // ================================================================
    // #186 — CONVERGENCE BEAM
    // 4 beams from walls converge at center, tridents in corners
    // ================================================================
    public static class ConvergenceBeam extends BossAttack {
        private boolean beamsActive = false;

        public ConvergenceBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_convergence_beam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(32.0); // 16 hearts/second
            config.setDamageRadius(1.5);
            config.setDurationTicks(140);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 15, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !beamsActive) {
                beamsActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.7f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.6f);
            }

            // 4 converging beams (10-90 ticks = 4 seconds)
            if (beamsActive && ticksAlive >= 10 && ticksAlive < 90) {
                if (ticksAlive % 3 == 0) {
                    for (int dir = 0; dir < 4; dir++) {
                        double angle = (Math.PI / 2) * dir;
                        for (int d = 1; d <= 10; d++) {
                            Location beamLoc = center.clone().add(Math.cos(angle) * d, 3, Math.sin(angle) * d);
                            DisplayBuilder.crimsonDust(beamLoc, 3, 0.3);
                        }
                    }
                    // Center convergence point
                    DisplayBuilder.crimsonDust(center.clone().add(0, 3, 0), 1, 0.3);
                }

                // Corner tridents (6 per corner = 24 total)
                if ((ticksAlive - 10) % 15 == 0) {
                    double[] cornerAngles = { Math.PI / 4, 3 * Math.PI / 4, 5 * Math.PI / 4, 7 * Math.PI / 4 };
                    for (double cAngle : cornerAngles) {
                        Location cornerLoc = center.clone().add(
                            Math.cos(cAngle) * 7, 15, Math.sin(cAngle) * 7);
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(cornerLoc, Material.PRISMARINE_BRICKS);
                        trident.scale(0.12f, 0.12f, 0.9f).glow(200, 20, 20).interpolation(2, 0);
                        spawnedEntities.add(trident.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 1.0f);
                }
            }

            // Animate falling corner tridents
            for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                Location loc = spawnedEntities.get(i).getLocation();
                if (loc.getY() > center.getY() + 0.5) {
                    loc.subtract(0, 1.5, 0);
                    spawnedEntities.get(i).teleport(loc);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConvergenceBeam(plugin); }
    }

    // ================================================================
    // #187 — BRIMSTONE CAGE (Phase 4 version)
    // 12 beam projectors, 8-second cage, 72-heart collapse
    // ================================================================
    public static class BrimstoneCagePhase4 extends BossAttack {
        private boolean cageActive = false;

        public BrimstoneCagePhase4(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_cage_p4", AttackType.BOSS, 5), "calamitas");
            config.setDamage(48.0); // 24 hearts/second
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 20, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 20 && !cageActive) {
                cageActive = true;
                // 12 beam bars forming a cage
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI / 12) * i;
                    Location barLoc = center.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2);
                    BlockDisplayHandle bar = displayBuilder.spawnBlock(barLoc, Material.ORANGE_STAINED_GLASS);
                    bar.scale(0.2f, 3.5f, 0.2f).glow(255, 80, 0).interpolation(3, 0);
                    spawnedEntities.add(bar.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.7f);
            }

            // Cage active (20-180 ticks = 8 seconds)
            if (cageActive && ticksAlive >= 20 && ticksAlive < 180) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(center, 10, 2.5);
                    DisplayBuilder.crimsonDust(center, 3, 1.0);
                    DisplayBuilder.crimsonDust(center, 5, 1.5);
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.4f,
                        0.5f + (ticksAlive % 60) * 0.02f);
                }
            }

            // Collapse (tick 180) — all beams fire inward
            if (cageActive && ticksAlive == 180) {
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 2, 2.0);
                DisplayBuilder.crimsonDust(center, 30, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCagePhase4(plugin); }
    }

    // ================================================================
    // #188 — THE LAST BEAM
    // 48 hearts direct hit, 2-second charge, one-time
    // ================================================================
    public static class TheLastBeam extends BossAttack {
        private boolean beamFired = false;

        public TheLastBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_last_beam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(96.0); // 48 hearts direct
            config.setDamageRadius(2.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(5);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            // 2-second charge telegraph
            DisplayBuilder.crimsonDust(center.clone().add(0, 5, 0), 20, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge phase (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !beamFired) {
                float chargeProgress = ticksAlive / 40.0f;
                if (ticksAlive % 2 == 0) {
                    float size = 1.0f + chargeProgress * 3.0f;
                    DisplayBuilder.crimsonDust(center.clone().add(0, 5, 0), (int)(5 + chargeProgress * 20), 1.5);
                    DisplayBuilder.crimsonDust(center.clone().add(0, 5, 0), (int)(chargeProgress * 2), 0.5);
                }
                // Rising charge pitch
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.3f + chargeProgress * 0.5f,
                        0.5f + chargeProgress * 1.5f);
                }
            }

            // FIRE (tick 40)
            if (ticksAlive == 40 && !beamFired) {
                beamFired = true;
                // Instantaneous beam across entire arena
                for (int d = -10; d <= 10; d++) {
                    Location beamLoc = center.clone().add(0, 5, d);
                    // White flash
                    DisplayBuilder.purpleDust(beamLoc, 10, 0.3);
                    // Then crimson burn
                    DisplayBuilder.crimsonDust(beamLoc, 5, 0.3);
                    DisplayBuilder.crimsonDust(beamLoc, 5, 0.5);
                }
                // The loudest sound in Phase 4
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            }

            // Scorch path (tick 41+)
            if (beamFired && ticksAlive > 40 && ticksAlive < 60) {
                if (ticksAlive % 3 == 0) {
                    for (int d = -10; d <= 10; d += 3) {
                        Location scorchLoc = center.clone().add(0, 0.3, d);
                        DisplayBuilder.crimsonDust(scorchLoc, 2, 0.3);
                        DisplayBuilder.crimsonDust(scorchLoc, 2, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLastBeam(plugin); }
    }

    // ================================================================
    // #189 — BEAM SWEEP FINALE
    // Dual rotating beams, 360 sweep, 12 seconds total
    // ================================================================
    public static class BeamSweepFinale extends BossAttack {
        private boolean sweeping = false;

        public BeamSweepFinale(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_beam_sweep_finale", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts/second
            config.setDamageRadius(1.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) { /* NO telegraph */ }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !sweeping) {
                sweeping = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.7f);
            }

            // Dual beams sweeping (0-240 ticks = 12 seconds, 3 full rotations)
            if (sweeping && ticksAlive < 240) {
                // Rotation: alternating direction per cycle
                int cycle = ticksAlive / 80; // 0, 1, 2
                int cycleElapsed = ticksAlive % 80;
                double direction = (cycle % 2 == 0) ? 1.0 : -1.0;
                double cwAngle = cycleElapsed * (Math.PI / 40) * direction;
                double ccwAngle = cwAngle + Math.PI;

                if (ticksAlive % 2 == 0) {
                    // CW beam (crimson-orange)
                    for (int d = 1; d <= 10; d++) {
                        Location cwLoc = center.clone().add(Math.cos(cwAngle) * d, 2, Math.sin(cwAngle) * d);
                        DisplayBuilder.crimsonDust(cwLoc, 3, 0.3);
                    }
                    // CCW beam (crimson-purple)
                    for (int d = 1; d <= 10; d++) {
                        Location ccwLoc = center.clone().add(Math.cos(ccwAngle) * d, 2, Math.sin(ccwAngle) * d);
                        DisplayBuilder.crimsonDust(ccwLoc, 3, 0.3);
                    }
                    // Floor contact
                    DisplayBuilder.crimsonDust(center.clone().add(Math.cos(cwAngle) * 8, 0.3, Math.sin(cwAngle) * 8), 2, 0.3);
                }

                // Direction reversal sound
                if (cycleElapsed == 0 && ticksAlive > 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BeamSweepFinale(plugin); }
    }

    // ================================================================
    // #190 — TOTAL ANNIHILATION GRID
    // Cross beams + crown rain + ground fissures simultaneously
    // ================================================================
    public static class TotalAnnihilationGrid extends BossAttack {
        private boolean gridActive = false;

        public TotalAnnihilationGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_total_annihilation", AttackType.BOSS, 5), "calamitas");
            config.setDamage(36.0); // 18 hearts/second beam
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) { /* NO telegraph */ }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !gridActive) {
                gridActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.6f);
            }

            // Everything fires simultaneously for 100 ticks (5 seconds)
            if (gridActive && ticksAlive < 100) {
                // (1) Cross beam grid
                if (ticksAlive % 3 == 0) {
                    for (int dir = 0; dir < 4; dir++) {
                        double angle = (Math.PI / 2) * dir;
                        for (int d = 2; d <= 10; d += 2) {
                            Location beamLoc = center.clone().add(Math.cos(angle) * d, 3, Math.sin(angle) * d);
                            DisplayBuilder.crimsonDust(beamLoc, 3, 0.3);
                        }
                    }
                }

                // (2) Crown rain tridents
                if (ticksAlive % 5 == 0) {
                    double x = (Math.random() - 0.5) * 18;
                    double z = (Math.random() - 0.5) * 18;
                    Location dropLoc = center.clone().add(x, 20, z);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(dropLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(trident.entity());
                    DisplayBuilder.crimsonDust(dropLoc, 3, 0.3);
                }

                // (3) Ground fissure eruptions
                if (ticksAlive % 12 == 0) {
                    double angle = Math.random() * 2 * Math.PI;
                    for (int d = 1; d <= 8; d += 2) {
                        Location fissureLoc = center.clone().add(Math.cos(angle) * d, 0.1, Math.sin(angle) * d);
                        DisplayBuilder.crimsonDust(fissureLoc, 5, 0.5);
                        DisplayBuilder.crimsonDust(fissureLoc, 3, 0.3);
                    }
                }

                // Combined audio
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.3f, 1.0f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.3f, 0.7f);
                }
            }

            // Animate falling tridents
            for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                Location loc = spawnedEntities.get(i).getLocation();
                if (loc.getY() > center.getY() + 0.5) {
                    loc.subtract(0, 2.0, 0);
                    spawnedEntities.get(i).teleport(loc);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TotalAnnihilationGrid(plugin); }
    }
}
