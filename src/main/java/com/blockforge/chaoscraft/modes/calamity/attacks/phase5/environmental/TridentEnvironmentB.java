package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.environmental;

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
 * Phase 4E Environmental -- TRIDENT ENVIRONMENT B
 * Attacks #111-120: Sinkhole trap, spectral echo, wall web, thunderhead formation,
 * void darkness pulse, gravity fracture, boss-echo stream corruption, floor acid,
 * resonant feedback, and magenta lightning.
 *
 * Design notes:
 * - Calamitas palette: crimson (200,0,50), orange (255,100,0), soul blue (0,150,255), purple (128,0,255)
 * - Damage range: 10.0-20.0 HP (escalating)
 * - Reality decay themed environmental attacks
 */
public final class TridentEnvironmentB {

    private TridentEnvironmentB() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TridentSinkholeTrap(plugin));
        registry.register(new SpectralTridentEcho(plugin));
        registry.register(new TridentWallWeb(plugin));
        registry.register(new TridentThunderhead(plugin));
        registry.register(new VoidDarknessPulse(plugin));
        registry.register(new GravityFracturePatches(plugin));
        registry.register(new StreamCorruption(plugin));
        registry.register(new FloorAcidSeep(plugin));
        registry.register(new ResonantFeedback(plugin));
        registry.register(new MagentaLightning(plugin));
    }

    // =========================================================================
    // 111. TRIDENT SINKHOLE TRAP -- stationary player punished with eruption
    // =========================================================================
    public static class TridentSinkholeTrap extends EnvironmentalAttack {

        private Location trapCenter;
        private boolean erupted = false;

        public TridentSinkholeTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trident_sinkhole_trap", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(11.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Target a random player position
            List<Player> players = new ArrayList<>(w.getPlayers());
            if (!players.isEmpty()) {
                trapCenter = players.get((int) (Math.random() * players.size())).getLocation().clone();
            } else {
                trapCenter = center.clone();
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            if (trapCenter == null) return;
            World w = trapCenter.getWorld();
            if (w == null) return;

            // Buildup: 30 ticks -- VERY short warning
            if (ticksAlive <= 30) {
                double intensity = ticksAlive / 30.0;
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.dustParticles(trapCenter.clone().add(0, 0.3, 0),
                            (int) (15 * intensity), 1.5, 255, 60, 0, 2.0f);
                }
                if (ticksAlive == 20) {
                    DisplayBuilder.playSound(trapCenter, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.4f);
                }
                return;
            }

            // Eruption
            if (!erupted) {
                erupted = true;
                DisplayBuilder.playSound(trapCenter, Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 0.8f);
            }

            int eruptTick = ticksAlive - 30;

            // 9 tridents from 3x3 grid upward
            if (eruptTick <= 15) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location tilePos = trapCenter.clone().add(x, 0, z);
                        double height = eruptTick * 1.0;
                        double spread = Math.toRadians(15) * (Math.random() - 0.5);
                        Location tridentPos = tilePos.clone().add(
                                Math.sin(spread) * height * 0.15, height, Math.cos(spread) * height * 0.15);

                        if (eruptTick % 2 == 0) {
                            w.spawnParticle(Particle.LAVA, tridentPos, 4, 0.1, 0.3, 0.1, 0);
                            w.spawnParticle(Particle.CRIT, tridentPos, 6, 0.1, 0.3, 0.1, 0.05);
                        }
                    }
                }

                // Launch damage
                if (eruptTick % 5 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - trapCenter.getX();
                        double dz = p.getLocation().getZ() - trapCenter.getZ();
                        if (dx * dx + dz * dz <= 2.25) {
                            p.damage(11.0);
                        }
                    }
                }
            }

            // Falling re-land phase: ticks 25-45
            if (eruptTick >= 25 && eruptTick <= 45) {
                if (eruptTick % 3 == 0) {
                    for (int i = 0; i < 9; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 6;
                        Location fallPos = trapCenter.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, fallPos, 5, 0.3, 0.5, 0.3, 0.1);
                    }
                }

                // Re-land damage
                if (eruptTick % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distanceSquared(trapCenter);
                        if (dist <= 36.0) {
                            p.damage(12.0);
                        }
                    }
                }
            }

            // Scorched aftermath
            if (eruptTick > 45 && eruptTick % 8 == 0) {
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, trapCenter.clone().add(0, 0.3, 0),
                        4, 1.0, 0.2, 1.0, 0.005);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentSinkholeTrap(plugin); }
    }

    // =========================================================================
    // 112. SPECTRAL TRIDENT ECHO -- ghost repeat of previous trident attack
    // =========================================================================
    public static class SpectralTridentEcho extends EnvironmentalAttack {

        private final List<Location> echoPath = new ArrayList<>();
        private boolean echoFired = false;

        public SpectralTridentEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_trident_echo", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(10.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // Generate echo paths (simulating previous trident trajectory)
            int pathCount = 4 + (int) (Math.random() * 4);
            for (int i = 0; i < pathCount; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 3 + Math.random() * 7;
                echoPath.add(center.clone().add(Math.cos(a) * d, 1.0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks -- ghost copies appear along paths
            if (ticksAlive <= 40) {
                if (ticksAlive % 4 == 0) {
                    for (Location path : echoPath) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, path, 5, 0.3, 0.5, 0.3, 0.01);
                        w.spawnParticle(Particle.END_ROD, path, 3, 0.2, 0.3, 0.2, 0.005);
                        DisplayBuilder.dustParticles(path, 8, 0.3, 0, 150, 255, 1.2f);
                    }
                }
                if (ticksAlive == 35) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 1.5f);
                }
                return;
            }

            // Echo fires at 60% speed along same vectors
            if (!echoFired) {
                echoFired = true;
            }

            int echoTick = ticksAlive - 40;

            // Ghost tridents travel along echo paths
            if (echoTick <= 40) {
                double progress = echoTick / 40.0;
                for (Location path : echoPath) {
                    // Ghost tridents drift toward center at 60% speed
                    Location ghostPos = path.clone().add(
                            (center.getX() - path.getX()) * progress * 0.6,
                            0,
                            (center.getZ() - path.getZ()) * progress * 0.6);

                    if (echoTick % 2 == 0) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, ghostPos, 12, 0.2, 0.3, 0.2, 0.01);
                        DisplayBuilder.dustParticles(ghostPos, 8, 0.2, 0, 150, 255, 1.5f);
                        w.spawnParticle(Particle.WITCH, ghostPos, 4, 0.3, 0.3, 0.3, 0);
                    }

                    // Echo damage at 50% of original
                    if (echoTick % 10 == 0) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(ghostPos) <= 2.25) {
                                p.damage(10.0);
                            }
                        }
                    }
                }
            }

            // Soul fire wisps aftermath
            if (echoTick > 40 && echoTick % 6 == 0) {
                for (Location path : echoPath) {
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, path, 2, 0.5, 0.3, 0.5, 0.005);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpectralTridentEcho(plugin); }
    }

    // =========================================================================
    // 113. TRIDENT WALL WEB -- beam grid connecting wall-mounted tridents
    // =========================================================================
    public static class TridentWallWeb extends EnvironmentalAttack {

        private final List<Location> wallTridents = new ArrayList<>();
        private boolean webActive = false;

        public TridentWallWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trident_wall_web", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(12.0);
            config.setDamageRadius(0.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(2000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // 20 tridents on 4 walls at varied heights
            for (int wall = 0; wall < 4; wall++) {
                for (int i = 0; i < 5; i++) {
                    double height = 0.5 + i * 0.5;
                    double wallPos = -8 + i * 4;
                    Location pos;
                    switch (wall) {
                        case 0: pos = center.clone().add(-10, height, wallPos); break;
                        case 1: pos = center.clone().add(10, height, wallPos); break;
                        case 2: pos = center.clone().add(wallPos, height, -10); break;
                        default: pos = center.clone().add(wallPos, height, 10); break;
                    }
                    wallTridents.add(pos);
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks -- walls emit crimson scanning lines
            if (ticksAlive <= 80) {
                if (ticksAlive % 5 == 0) {
                    for (int wall = 0; wall < 4; wall++) {
                        double scanY = 3.0 - (ticksAlive % 60) * 0.05;
                        for (int i = -10; i <= 10; i += 2) {
                            Location scanPos;
                            switch (wall) {
                                case 0: scanPos = center.clone().add(-10, scanY, i); break;
                                case 1: scanPos = center.clone().add(10, scanY, i); break;
                                case 2: scanPos = center.clone().add(i, scanY, -10); break;
                                default: scanPos = center.clone().add(i, scanY, 10); break;
                            }
                            DisplayBuilder.dustParticles(scanPos, 3, 0.2, 200, 0, 50, 1.0f);
                        }
                    }
                    // Dragon breath mist on walls
                    for (Location wt : wallTridents) {
                        w.spawnParticle(Particle.DRAGON_BREATH, wt, 3, 0.3, 0.3, 0.3, 0.01);
                    }
                }
                if (ticksAlive == 70) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 0.7f);
                }
                return;
            }

            // Web active
            if (!webActive) {
                webActive = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP, 0.8f, 1.3f);
            }

            int webTick = ticksAlive - 80;

            // Draw beam lines connecting opposite-wall tridents
            if (webTick <= 120 && webTick % 2 == 0) {
                // Connect north wall to south wall tridents
                for (int i = 0; i < 5; i++) {
                    Location northTrident = wallTridents.get(i);         // wall 0
                    Location southTrident = wallTridents.get(5 + i);     // wall 1
                    int beamSteps = 20;
                    for (int s = 0; s < beamSteps; s++) {
                        double t = s / (double) beamSteps;
                        Location beamPos = northTrident.clone().add(
                                (southTrident.getX() - northTrident.getX()) * t,
                                (southTrident.getY() - northTrident.getY()) * t,
                                (southTrident.getZ() - northTrident.getZ()) * t);
                        w.spawnParticle(Particle.END_ROD, beamPos, 1, 0, 0, 0, 0);
                    }
                }

                // Connect east wall to west wall tridents
                for (int i = 0; i < 5; i++) {
                    Location eastTrident = wallTridents.get(10 + i);     // wall 2
                    Location westTrident = wallTridents.get(15 + i);     // wall 3
                    int beamSteps = 20;
                    for (int s = 0; s < beamSteps; s++) {
                        double t = s / (double) beamSteps;
                        Location beamPos = eastTrident.clone().add(
                                (westTrident.getX() - eastTrident.getX()) * t,
                                (westTrident.getY() - eastTrident.getY()) * t,
                                (westTrident.getZ() - eastTrident.getZ()) * t);
                        w.spawnParticle(Particle.END_ROD, beamPos, 1, 0, 0, 0, 0);
                    }
                }

                // Beam collision check -- damage players intersecting beams
                if (webTick % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location pLoc = p.getLocation();
                        // Simplified: check if player is near any beam line Y-level
                        for (int h = 0; h < 5; h++) {
                            double beamY = 0.5 + h * 0.5;
                            if (Math.abs(pLoc.getY() - (center.getY() + beamY)) <= 0.5) {
                                p.damage(12.0);
                                DisplayBuilder.playSound(pLoc, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 1.0f);
                                break;
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentWallWeb(plugin); }
    }

    // =========================================================================
    // 114. TRIDENT THUNDERHEAD -- 24 trident grid descends from sky
    // =========================================================================
    public static class TridentThunderhead extends EnvironmentalAttack {

        private final List<Location> gridPositions = new ArrayList<>();
        private boolean mainBarrageFired = false;
        private boolean finalSixFired = false;

        public TridentThunderhead(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trident_thunderhead", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // 6x4 rectangular grid 20 blocks above arena
            for (int x = -3; x <= 2; x++) {
                for (int z = -2; z <= 1; z++) {
                    gridPositions.add(center.clone().add(x * 3 + 1.5, 20, z * 3 + 1.5));
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 100 ticks -- formation descends with electric cascade
            if (ticksAlive <= 100) {
                double descend = ticksAlive * 0.09; // 9 blocks over 100 ticks
                if (ticksAlive % 3 == 0) {
                    for (Location grid : gridPositions) {
                        Location gridPos = grid.clone().add(0, -descend, 0);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, gridPos, 8, 0.3, 1.0, 0.3, 0.05);
                        w.spawnParticle(Particle.CRIT, gridPos, 4, 0.2, 0.5, 0.2, 0.03);
                    }
                }
                if (ticksAlive == 40 || ticksAlive == 80) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.7f);
                }
                if (ticksAlive == 90) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.4f);
                }
                return;
            }

            // Main barrage: all 24 fire simultaneously
            if (!mainBarrageFired) {
                mainBarrageFired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.8f);

                // Massive totem gold explosion
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 0.5, 0),
                        100, 5.0, 1.0, 5.0, 0.1);
            }

            int barrageTick = ticksAlive - 100;

            // Tridents falling at 2.5 blocks/tick -- 11 blocks in ~4 ticks
            if (barrageTick <= 5) {
                double fallProgress = barrageTick / 4.0;
                for (Location grid : gridPositions) {
                    Location impactPos = grid.clone().add(0, -9 - (11 * fallProgress), 0);
                    DisplayBuilder.dustParticles(impactPos, 15, 0.3, 200, 0, 50, 2.0f);
                }

                if (barrageTick == 4) {
                    // Impact damage
                    for (Location grid : gridPositions) {
                        Location ground = center.clone().add(
                                grid.getX() - center.getX(), 0.5, grid.getZ() - center.getZ());
                        w.spawnParticle(Particle.ELECTRIC_SPARK, ground, 35, 0.8, 0.5, 0.8, 0.1);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, ground, 10, 0.5, 1.0, 0.5, 0.02);

                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(ground) <= 2.25) {
                                p.damage(14.0);
                            }
                        }
                    }
                }
            }

            // Final 6 close-range shots at 5 blocks height
            if (barrageTick == 25 && !finalSixFired) {
                finalSixFired = true;
                DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 1.1f);

                // Corner positions
                int[] corners = {0, 3, 5, 18, 20, 23};
                for (int idx : corners) {
                    if (idx < gridPositions.size()) {
                        Location grid = gridPositions.get(idx);
                        Location ground = center.clone().add(
                                grid.getX() - center.getX(), 0.5, grid.getZ() - center.getZ());
                        w.spawnParticle(Particle.CRIT, ground, 20, 0.5, 1.0, 0.5, 0.1);

                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(ground) <= 2.25) {
                                p.damage(18.0);
                            }
                        }
                    }
                }
            }

            // Aftermath: electrical hazard zones
            if (barrageTick > 30 && barrageTick % 8 == 0) {
                for (int i = 0; i < Math.min(6, gridPositions.size()); i++) {
                    Location grid = gridPositions.get(i);
                    Location ground = center.clone().add(
                            grid.getX() - center.getX(), 0.3, grid.getZ() - center.getZ());
                    w.spawnParticle(Particle.ELECTRIC_SPARK, ground, 4, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentThunderhead(plugin); }
    }

    // =========================================================================
    // 115. VOID DARKNESS PULSE -- arena plunges into darkness
    // =========================================================================
    public static class VoidDarknessPulse extends EnvironmentalAttack {

        private boolean darkActive = false;

        public VoidDarknessPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_darkness_pulse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(1800);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- sky streams flicker to 10%
            if (ticksAlive <= 60) {
                if (ticksAlive % 10 == 0) {
                    // Subtle stream flicker particles
                    for (int i = 0; i < 5; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = 5 + Math.random() * 5;
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(
                                Math.cos(a) * d, 0.5, Math.sin(a) * d), 3, 0.3, 0.3, 0.3, 0.005);
                    }
                }
                return;
            }

            // Darkness phase: 160 ticks (8 seconds)
            if (!darkActive) {
                darkActive = true;
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.5f);

                // Apply darkness-equivalent visual
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.6f);
                }
            }

            int darkTick = ticksAlive - 60;

            if (darkTick <= 160) {
                // Sky streams at 150% are only light source
                if (darkTick % 4 == 0) {
                    // Soul fire boundary markers
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), 10.0,
                            Particle.SOUL_FIRE_FLAME, 8, null);
                }
                // Near-black particles fill mid-arena
                if (darkTick % 3 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0),
                            50, 8.0, 5, 5, 5, 2.5f);
                }
                // Bat ambient sounds
                if (darkTick % 40 == 0) {
                    DisplayBuilder.playSound(center.clone().add(
                            (Math.random() - 0.5) * 16, 3, (Math.random() - 0.5) * 16),
                            Sound.ENTITY_BAT_AMBIENT, 0.3f, 0.7f);
                }
            }

            // Light snap back
            if (darkTick == 160) {
                DisplayBuilder.playSound(center, Sound.BLOCK_CONDUIT_DEACTIVATE, 1.0f, 0.9f);
            }

            // Recovery surge: streams at 120% for remaining time
            if (darkTick > 160 && darkTick % 5 == 0) {
                for (int i = 0; i < 3; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    Location streamPos = center.clone().add(Math.cos(a) * 8, 10 + Math.random() * 5, Math.sin(a) * 8);
                    w.spawnParticle(Particle.END_ROD, streamPos, 6, 1.0, 1.0, 1.0, 0.02);
                    w.spawnParticle(Particle.WITCH, streamPos, 4, 1.0, 1.0, 1.0, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidDarknessPulse(plugin); }
    }

    // =========================================================================
    // 116. GRAVITY FRACTURE PATCHES -- reversed gravity zones
    // =========================================================================
    public static class GravityFracturePatches extends EnvironmentalAttack {

        private final List<Location> patches = new ArrayList<>();
        private boolean patchesActive = false;

        public GravityFracturePatches(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_fracture_patches", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 3; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 3 + Math.random() * 6;
                patches.add(center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks -- purple upward drift
            if (ticksAlive <= 40) {
                if (ticksAlive % 3 == 0) {
                    for (Location patch : patches) {
                        DisplayBuilder.dustParticles(patch.clone().add(0, 0.5, 0),
                                20, 1.5, 150, 0, 255, 1.5f);
                        w.spawnParticle(Particle.END_ROD, patch.clone().add(0, 0.5, 0),
                                4, 1.0, 0.3, 1.0, 0.01);
                    }
                }
                return;
            }

            // Patches active: 100 ticks
            if (!patchesActive) {
                patchesActive = true;
                for (Location patch : patches) {
                    DisplayBuilder.playSound(patch, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.8f);
                }
            }

            int activeTick = ticksAlive - 40;

            if (activeTick <= 100) {
                // Visual upward drift from patches
                if (activeTick % 3 == 0) {
                    for (Location patch : patches) {
                        DisplayBuilder.dustParticles(patch.clone().add(0, 1.0, 0),
                                15, 1.5, 150, 0, 255, 1.5f);
                    }
                }

                // Levitate players on patches
                if (activeTick % 5 == 0) {
                    for (Location patch : patches) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - patch.getX();
                            double dz = p.getLocation().getZ() - patch.getZ();
                            if (dx * dx + dz * dz <= 2.25) {
                                p.setVelocity(p.getVelocity().add(
                                        new org.bukkit.util.Vector(0, 0.4, 0)));
                            }
                        }
                    }
                }

                // Gravity snap back particles
                if (activeTick == 60) {
                    for (Location patch : patches) {
                        DisplayBuilder.playSound(patch, Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.4f, 1.2f);
                        w.spawnParticle(Particle.CLOUD, patch.clone().add(0, 12, 0),
                                15, 1.0, 0.5, 1.0, 0.05);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravityFracturePatches(plugin); }
    }

    // =========================================================================
    // 117. STREAM CORRUPTION -- sky streams briefly invade ground level
    // =========================================================================
    public static class StreamCorruption extends EnvironmentalAttack {

        private boolean corruptionActive = false;

        public StreamCorruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_corruption", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- streams descend
            if (ticksAlive <= 60) {
                double descend = ticksAlive * 0.3;
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 10;
                        Location streamPos = center.clone().add(Math.cos(a) * d, 20 - descend, Math.sin(a) * d);
                        w.spawnParticle(Particle.WITCH, streamPos, 6, 0.5, 0.5, 0.5, 0);
                        w.spawnParticle(Particle.END_ROD, streamPos, 3, 0.3, 0.3, 0.3, 0.01);
                    }
                }
                return;
            }

            // Corruption active: streams at ground level
            if (!corruptionActive) {
                corruptionActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.5f);
            }

            int corruptTick = ticksAlive - 60;

            if (corruptTick <= 80) {
                // Ground-level stream particles
                if (corruptTick % 2 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 10;
                        Location groundStream = center.clone().add(Math.cos(a) * d, 1.5, Math.sin(a) * d);
                        w.spawnParticle(Particle.DRAGON_BREATH, groundStream, 8, 0.5, 0.3, 0.5, 0.01);
                        w.spawnParticle(Particle.WITCH, groundStream, 4, 0.5, 0.5, 0.5, 0);
                        DisplayBuilder.dustParticles(groundStream, 6, 0.5, 200, 0, 50, 1.5f);
                    }
                }

                // Damage players in corrupted zone
                if (corruptTick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distanceSquared(center);
                        if (dist <= 100.0 && p.getLocation().getY() <= center.getY() + 3) {
                            p.damage(12.0);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamCorruption(plugin); }
    }

    // =========================================================================
    // 118. FLOOR ACID SEEP -- crimson acid pools appear on the arena floor
    // =========================================================================
    public static class FloorAcidSeep extends EnvironmentalAttack {

        private final List<Location> acidPools = new ArrayList<>();
        private final List<BlockDisplayHandle> poolDisplays = new ArrayList<>();

        public FloorAcidSeep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floor_acid_seep", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 5; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 2 + Math.random() * 8;
                acidPools.add(center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- seeping crimson at pool locations
            if (ticksAlive <= 60) {
                double radius = ticksAlive * 0.03;
                if (ticksAlive % 3 == 0) {
                    for (Location pool : acidPools) {
                        DisplayBuilder.dustParticles(pool.clone().add(0, 0.2, 0),
                                8, radius, 200, 0, 50, 1.0f);
                    }
                }
                // Spawn pool display at threshold
                if (ticksAlive == 50) {
                    for (Location pool : acidPools) {
                        BlockDisplayHandle display = displayBuilder.spawnBlock(
                                pool.clone().add(0, 0.05, 0), Material.MAGMA_BLOCK);
                        display.scale(2.0f, 0.05f, 2.0f).glow(200, 0, 50).interpolation(5, 0);
                        poolDisplays.add(display);
                        spawnedEntities.add(display.entity());
                    }
                }
                return;
            }

            int poolTick = ticksAlive - 60;

            // Active pools: 120 ticks
            if (poolTick <= 120) {
                if (poolTick % 4 == 0) {
                    for (Location pool : acidPools) {
                        DisplayBuilder.dustParticles(pool.clone().add(0, 0.3, 0),
                                10, 1.5, 200, 0, 50, 1.2f);
                        w.spawnParticle(Particle.SMOKE, pool.clone().add(0, 0.5, 0),
                                3, 0.8, 0.2, 0.8, 0.01);
                    }
                }

                // Pool damage
                if (poolTick % 20 == 0) {
                    for (Location pool : acidPools) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - pool.getX();
                            double dz = p.getLocation().getZ() - pool.getZ();
                            if (dx * dx + dz * dz <= 4.0) {
                                p.damage(10.0);
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloorAcidSeep(plugin); }
    }

    // =========================================================================
    // 119. RESONANT FEEDBACK -- arena structures vibrate and emit shockwaves
    // =========================================================================
    public static class ResonantFeedback extends EnvironmentalAttack {

        private boolean shockwaveFired = false;
        private double waveRadius = 0;

        public ResonantFeedback(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("resonant_feedback", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {}

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- structures vibrate with orange dust
            if (ticksAlive <= 60) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = (2 * Math.PI * i) / 4;
                        Location structPos = center.clone().add(Math.cos(a) * 9, 2, Math.sin(a) * 9);
                        DisplayBuilder.dustParticles(structPos, 12, 0.5, 255, 100, 0, 1.5f);
                        w.spawnParticle(Particle.CRIT, structPos, 5, 0.3, 0.5, 0.3, 0.02);
                    }
                }
                if (ticksAlive == 50) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.5f);
                }
                return;
            }

            // Shockwave from structures toward center
            if (!shockwaveFired) {
                shockwaveFired = true;
                waveRadius = 10;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
            }

            int waveTick = ticksAlive - 60;

            // Converging shockwave: radius decreases
            if (waveRadius > 0) {
                waveRadius -= 0.8;
                if (waveRadius < 0) waveRadius = 0;

                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), waveRadius,
                        Particle.CRIT, (int) (waveRadius * 3), null);
                DisplayBuilder.particleRing(center.clone().add(0, 1.0, 0), waveRadius,
                        Particle.FLAME, (int) (waveRadius * 2), null);

                // Damage players in wave ring
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - center.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                    if (Math.abs(dist - waveRadius) <= 2.0) {
                        p.damage(14.0);
                    }
                }
            }

            // Impact at center
            if (waveTick == 13) {
                w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 1, 0),
                        60, 2.0, 1.0, 2.0, 0.1);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.2f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ResonantFeedback(plugin); }
    }

    // =========================================================================
    // 120. MAGENTA LIGHTNING -- spell_witch lightning strikes from sky streams
    // =========================================================================
    public static class MagentaLightning extends EnvironmentalAttack {

        private final List<Location> strikeTargets = new ArrayList<>();
        private boolean strikesActive = false;

        public MagentaLightning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magenta_lightning", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(16.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 6; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 2 + Math.random() * 8;
                strikeTargets.add(center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- magenta particles descend in bolts
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    for (Location target : strikeTargets) {
                        double boltY = 20 - (ticksAlive * 0.25);
                        w.spawnParticle(Particle.WITCH, target.clone().add(
                                (Math.random() - 0.5) * 2, boltY, (Math.random() - 0.5) * 2),
                                8, 0.3, 1.0, 0.3, 0);
                    }
                }
                if (ticksAlive == 55) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
                }
                return;
            }

            // Strikes: staggered every 5 ticks
            if (!strikesActive) {
                strikesActive = true;
            }

            int strikeTick = ticksAlive - 60;

            for (int i = 0; i < strikeTargets.size(); i++) {
                int triggerTime = i * 5;
                Location target = strikeTargets.get(i);

                if (strikeTick == triggerTime) {
                    // Lightning bolt visual
                    for (int y = 0; y <= 20; y += 2) {
                        Location boltPos = target.clone().add(
                                (Math.random() - 0.5) * 0.5, y, (Math.random() - 0.5) * 0.5);
                        w.spawnParticle(Particle.WITCH, boltPos, 15, 0.2, 0.3, 0.2, 0);
                        DisplayBuilder.dustParticles(boltPos, 8, 0.3, 128, 0, 255, 2.0f);
                    }

                    // Impact at ground
                    w.spawnParticle(Particle.ELECTRIC_SPARK, target.clone().add(0, 0.5, 0),
                            40, 1.5, 0.5, 1.5, 0.1);
                    DisplayBuilder.dustParticles(target.clone().add(0, 0.3, 0),
                            30, 2.0, 200, 0, 50, 2.5f);
                    DisplayBuilder.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.2f);

                    // Damage
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(target) <= 6.25) {
                            p.damage(16.0);
                        }
                    }
                }

                // Residual glow
                if (strikeTick > triggerTime && strikeTick <= triggerTime + 20 && strikeTick % 4 == 0) {
                    DisplayBuilder.dustParticles(target.clone().add(0, 0.3, 0),
                            6, 1.0, 128, 0, 255, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagentaLightning(plugin); }
    }
}
