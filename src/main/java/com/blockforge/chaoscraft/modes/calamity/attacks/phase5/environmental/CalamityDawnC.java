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
 * Phase 4E (Boss 5: Supreme Calamitas) Environmental Attacks #21-30
 * ISLAND MEMORY ERUPTIONS (#21-22) + SOUL ARCHITECTURE EVENTS (#23-30)
 *
 * Design: No status effects. Damage 6.0-18.0 HP. AxisAngle4f only.
 */
public final class CalamityDawnC {

    private CalamityDawnC() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PillarBeamSweep(plugin));
        registry.register(new MemoryFaultLine(plugin));
        registry.register(new SoulDrainField(plugin));
        registry.register(new EchoGalleryActivation(plugin));
        registry.register(new SoulFireStorm(plugin));
        registry.register(new WhisperSurge(plugin));
        registry.register(new MemoryFlood(plugin));
        registry.register(new SoulAnchorTethers(plugin));
        registry.register(new GriefEcho(plugin));
        registry.register(new SoulWellCollapse(plugin));
    }

    // =========================================================================
    // 21. PILLAR BEAM SWEEP -- Death Resonance Pillar fires a rotating beam
    // =========================================================================
    public static class PillarBeamSweep extends EnvironmentalAttack {

        private Location pillarPos;
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private double beamAngle = 0;

        public PillarBeamSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pillar_beam_sweep", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0); // 6 hearts per beam hit
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            pillarPos = center.clone().add(2, 0, -1);
            beamAngle = Math.random() * 2 * Math.PI;
            DisplayBuilder.playSound(pillarPos, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || pillarPos == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-60 ticks (3 seconds)
            if (ticksAlive <= 60) {
                if (ticksAlive % 5 == 0) {
                    for (int dir = 0; dir < 4; dir++) {
                        double dAngle = (Math.PI / 2) * dir;
                        for (double d = 0; d < 5; d += 1.0) {
                            Location beamPos = pillarPos.clone().add(
                                    Math.cos(dAngle) * d, 8, Math.sin(dAngle) * d);
                            DisplayBuilder.dustParticles(beamPos, 3, 0.2, 80, 0, 80, 1.5f);
                        }
                    }
                    DisplayBuilder.playSound(pillarPos, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);
                }
                return;
            }

            // Beam sweep: 61-160 ticks (5 seconds = 100 ticks)
            int sweepTick = ticksAlive - 61;
            // Accelerate in final quarter
            double rotSpeed = sweepTick > 75 ? 0.126 : 0.063; // 7.2 or 3.6 deg/tick
            beamAngle += rotSpeed;

            if (sweepTick % 2 == 0) {
                // Draw beam line from pillar, 12 blocks long at Y+8
                for (double d = 0; d < 12; d += 0.5) {
                    double bx = pillarPos.getX() + Math.cos(beamAngle) * d;
                    double bz = pillarPos.getZ() + Math.sin(beamAngle) * d;
                    Location beamPos = new Location(w, bx, pillarPos.getY() + 8, bz);
                    DisplayBuilder.dustParticles(beamPos, 3, 0.15, 80, 0, 80, 2.0f);
                    // Beam tip glow
                    if (d > 11) {
                        DisplayBuilder.dustParticles(beamPos, 5, 0.3, 150, 0, 255, 2.5f);
                    }
                }

                // Beam block displays (update positions)
                if (sweepTick == 0) {
                    for (int i = 0; i < 12; i++) {
                        double bx = pillarPos.getX() + Math.cos(beamAngle) * i;
                        double bz = pillarPos.getZ() + Math.sin(beamAngle) * i;
                        Location blockPos = new Location(w, bx, pillarPos.getY() + 8, bz);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(blockPos, Material.PURPUR_BLOCK);
                        h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 200).interpolation(2, 0);
                        beamHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Beam player damage
            if (sweepTick % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    // Check if player is in beam path
                    double dx = pl.getX() - pillarPos.getX();
                    double dz = pl.getZ() - pillarPos.getZ();
                    double playerAngle = Math.atan2(dz, dx);
                    double angleDiff = Math.abs(normalizeAngle(playerAngle - beamAngle));
                    double playerDist = Math.sqrt(dx * dx + dz * dz);
                    if (angleDiff < 0.15 && playerDist <= 12 && Math.abs(pl.getY() - pillarPos.getY() - 8) < 3) {
                        p.damage(12.0); // 6 hearts
                        // Vertical knockback
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 1.0, 0)));
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, pl, 20, 0.5, 0.5, 0.5, 0.05);
                        DisplayBuilder.playSound(pl, Sound.ENTITY_WARDEN_HURT, 0.9f, 0.8f);
                    }
                }
            }

            // End sound
            if (sweepTick == 99) {
                DisplayBuilder.playSound(pillarPos, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 1.0f);
            }
        }

        private double normalizeAngle(double a) {
            while (a > Math.PI) a -= 2 * Math.PI;
            while (a < -Math.PI) a += 2 * Math.PI;
            return a;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PillarBeamSweep(plugin); }
    }

    // =========================================================================
    // 22. MEMORY FAULT LINE -- crack line across arena with fire jets
    // =========================================================================
    public static class MemoryFaultLine extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> faultHandles = new ArrayList<>();
        private double lineAngle;
        private boolean sealed = false;

        public MemoryFaultLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("memory_fault_line", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts per 10 ticks on line
            config.setDamageRadius(0.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(2000); // 100 seconds
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            lineAngle = Math.random() * Math.PI; // Random orientation
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-100 ticks (5 seconds) -- crack line appearing
            if (ticksAlive <= 100) {
                double progress = ticksAlive / 100.0;
                double lineLength = 10 * progress;
                if (ticksAlive % 4 == 0) {
                    for (double d = -lineLength; d <= lineLength; d += 1.0) {
                        double lx = center.getX() + Math.cos(lineAngle) * d;
                        double lz = center.getZ() + Math.sin(lineAngle) * d;
                        Location lp = new Location(w, lx, center.getY() + 0.1, lz);
                        DisplayBuilder.dustParticles(lp, 3, 0.2, 255, 80, 0, 1.2f);
                        w.spawnParticle(Particle.PORTAL, lp, 2, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                return;
            }

            // Active: 101-300 ticks (10 seconds) -- jets firing in sequence
            int activeTick = ticksAlive - 101;
            if (activeTick >= 0 && activeTick <= 200) {
                // Line particles
                if (activeTick % 4 == 0) {
                    for (double d = -10; d <= 10; d += 1.0) {
                        double lx = center.getX() + Math.cos(lineAngle) * d;
                        double lz = center.getZ() + Math.sin(lineAngle) * d;
                        Location lp = new Location(w, lx, center.getY() + 0.1, lz);
                        DisplayBuilder.dustParticles(lp, 2, 0.2, 255, 80, 0, 1.0f);
                    }
                }

                // Sequential jets every 10 ticks
                if (activeTick % 10 == 0) {
                    int jetSegment = (activeTick / 10) % 10;
                    double jetD = -10 + jetSegment * 2;
                    double jetX = center.getX() + Math.cos(lineAngle) * jetD;
                    double jetZ = center.getZ() + Math.sin(lineAngle) * jetD;
                    Location jetBase = new Location(w, jetX, center.getY(), jetZ);

                    // Jet column
                    for (double y = 0; y < 10; y += 0.5) {
                        w.spawnParticle(Particle.FLAME, jetBase.clone().add(0, y, 0), 5, 0.3, 0.2, 0.3, 0.02);
                        w.spawnParticle(Particle.PORTAL, jetBase.clone().add(0, y, 0), 3, 0.3, 0.2, 0.3, 0.05);
                    }
                    DisplayBuilder.playSound(jetBase, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);

                    // Jet damage
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = Math.abs(p.getLocation().getX() - jetX);
                        double dz = Math.abs(p.getLocation().getZ() - jetZ);
                        if (dx <= 1.5 && dz <= 1.5 && p.getLocation().getY() <= center.getY() + 10) {
                            p.damage(10.0); // 5 hearts jet hit
                            p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 2.0, 0)));
                        }
                    }
                }

                // Line contact damage
                if (activeTick % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location pl = p.getLocation();
                        // Project player onto line
                        double dx = pl.getX() - center.getX();
                        double dz = pl.getZ() - center.getZ();
                        double proj = dx * Math.cos(lineAngle) + dz * Math.sin(lineAngle);
                        double perpDist = Math.abs(-dx * Math.sin(lineAngle) + dz * Math.cos(lineAngle));
                        if (perpDist <= 1.0 && Math.abs(proj) <= 10) {
                            p.damage(6.0); // 3 hearts line contact
                        }
                    }
                }
            }

            // Seal: last 20 ticks -- smoke burst and permanent scar
            if (activeTick > 200 && !sealed) {
                sealed = true;
                for (double d = -10; d <= 10; d += 1.0) {
                    double lx = center.getX() + Math.cos(lineAngle) * d;
                    double lz = center.getZ() + Math.sin(lineAngle) * d;
                    Location lp = new Location(w, lx, center.getY(), lz);
                    w.spawnParticle(Particle.SMOKE, lp, 5, 0.2, 0.5, 0.2, 0.02);
                    DisplayBuilder.playSound(lp, Sound.BLOCK_FIRE_EXTINGUISH, 0.9f, 0.6f);

                    // Permanent scar display
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            new Location(w, lx, center.getY() + 0.05, lz), Material.BLACKSTONE);
                    h.scale(1.0f, 0.2f, 1.0f).glow(40, 0, 0).interpolation(5, 0);
                    faultHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MemoryFaultLine(plugin); }
    }

    // =========================================================================
    // 23. SOUL DRAIN FIELD -- players standing still take escalating drain damage
    // =========================================================================
    public static class SoulDrainField extends EnvironmentalAttack {

        public SoulDrainField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_drain_field", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); // Custom damage logic
            config.setDamageRadius(0.0);
            config.setDurationTicks(600); // 30 seconds active window
            config.setCooldownTicks(100); // Continuous passive
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // Silent -- no spawn effects
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive % 10 != 0) return;

            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                Location pl = p.getLocation();
                // Check if near arena center
                if (center.distanceSquared(pl) > 400) continue; // 20 block range

                // Check velocity -- if player is essentially stationary
                double speed = p.getVelocity().lengthSquared();
                if (speed < 0.01) {
                    // Soul particles rising from feet
                    w.spawnParticle(Particle.SOUL, pl.clone().add(0, 0.2, 0), 5, 0.3, 0.3, 0.3, 0.01);

                    // Damage based on how long stationary (simplified: damage every 40 ticks)
                    if (ticksAlive % 40 == 0) {
                        p.damage(6.0); // 3 hearts
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, pl.clone().add(0, 0.5, 0),
                                5, 0.2, 0.3, 0.2, 0.01);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulDrainField(plugin); }
    }

    // =========================================================================
    // 24. ECHO GALLERY ACTIVATION -- spectral shapes move toward players
    // =========================================================================
    public static class EchoGalleryActivation extends EnvironmentalAttack {

        private final List<Location> spectrePositions = new ArrayList<>();
        private final List<Location> spectreTargets = new ArrayList<>();

        public EchoGalleryActivation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_gallery_activation", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(380); // 19 seconds
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0); // 4 hearts per contact
            config.setImpactRadius(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 7 spectral shapes around perimeter
            for (int i = 0; i < 7; i++) {
                double angle = (2 * Math.PI * i) / 7;
                Location sp = center.clone().add(Math.cos(angle) * 9, 0, Math.sin(angle) * 9);
                spectrePositions.add(sp);
                spectreTargets.add(sp.clone());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT, 0.3f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-80 ticks (4 seconds) -- vibrating spectres
            if (ticksAlive <= 80) {
                if (ticksAlive % 5 == 0) {
                    for (Location sp : spectrePositions) {
                        w.spawnParticle(Particle.SOUL, sp.clone().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.01);
                    }
                }
                return;
            }

            // Active: 81-380 -- spectres move toward nearest player
            int activeTick = ticksAlive - 81;
            if (activeTick >= 0 && activeTick <= 300) {
                // Find nearest player for each spectre
                List<Player> players = new ArrayList<>(w.getPlayers());
                players.removeIf(this::isExempt);

                for (int i = 0; i < spectrePositions.size(); i++) {
                    Location sp = spectrePositions.get(i);

                    // Move toward nearest player at 0.15 blocks/tick
                    Player nearest = null;
                    double nearestDist = Double.MAX_VALUE;
                    for (Player p : players) {
                        double d = p.getLocation().distanceSquared(sp);
                        if (d < nearestDist) {
                            nearestDist = d;
                            nearest = p;
                        }
                    }

                    if (nearest != null) {
                        org.bukkit.util.Vector dir = nearest.getLocation().toVector()
                                .subtract(sp.toVector()).normalize().multiply(0.15);
                        sp.add(dir.getX(), 0, dir.getZ());
                    }

                    // Spectre particles
                    if (activeTick % 4 == 0) {
                        w.spawnParticle(Particle.SOUL, sp.clone().add(0, 0.5, 0), 8, 0.2, 0.5, 0.2, 0.01);
                    }

                    // Contact damage
                    for (Player p : players) {
                        if (p.getLocation().distanceSquared(sp) <= 0.64) { // 0.8 blocks
                            p.damage(8.0); // 4 hearts
                            w.spawnParticle(Particle.SOUL, sp, 30, 0.5, 0.5, 0.5, 0.03);
                            DisplayBuilder.playSound(sp, Sound.ENTITY_PLAYER_HURT, 0.8f, 0.5f);
                            // Pass through -- push spectre to edge
                            org.bukkit.util.Vector away = sp.toVector()
                                    .subtract(center.toVector()).normalize().multiply(8);
                            sp.add(away.getX(), 0, away.getZ());
                            break;
                        }
                    }
                }

                if (activeTick % 30 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_BREATH, 0.2f, 0.5f);
                }
            }

            // Return: final 80 ticks
            if (activeTick > 300) {
                if (activeTick == 301) {
                    DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.3f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EchoGalleryActivation(plugin); }
    }

    // =========================================================================
    // 25. SOUL FIRE STORM -- soul soil patches erupt with blue fire geysers
    // =========================================================================
    public static class SoulFireStorm extends EnvironmentalAttack {

        private final List<Location> soulPatches = new ArrayList<>();

        public SoulFireStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_storm", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts per 6 ticks
            config.setDamageRadius(0.0);
            config.setDurationTicks(220); // 11 seconds
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(6);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 8; i++) {
                double px = center.getX() + (Math.random() - 0.5) * 16;
                double pz = center.getZ() + (Math.random() - 0.5) * 16;
                soulPatches.add(new Location(w, px, center.getY(), pz));
            }
            DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-60 ticks (3 seconds) -- warning ring
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    double ringRadius = 2 + (ticksAlive / 60.0) * 8;
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), ringRadius, Particle.DUST,
                            30, new Particle.DustOptions(Color.fromRGB(0, 100, 255), 1.2f));
                    for (Location sp : soulPatches) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, sp.clone().add(0, 0.3, 0),
                                5, 0.3, 0.2, 0.3, 0.01);
                    }
                }
                return;
            }

            // Eruption: 61-220 ticks (8 seconds)
            int eruptTick = ticksAlive - 61;
            if (eruptTick >= 0) {
                if (eruptTick % 3 == 0) {
                    for (Location sp : soulPatches) {
                        // Eruption column Y+6
                        for (double y = 0; y < 6; y += 1.0) {
                            w.spawnParticle(Particle.SOUL_FIRE_FLAME, sp.clone().add(0, y, 0),
                                    5, 0.5, 0.3, 0.5, 0.02);
                        }
                    }
                }

                // Geyser events (3 random, between ticks 40-140 of eruption)
                if (eruptTick >= 40 && eruptTick <= 140 && eruptTick % 50 == 0) {
                    Location geyserPatch = soulPatches.get((int) (Math.random() * soulPatches.size()));
                    // Tall geyser to Y+15
                    for (double y = 0; y < 15; y += 0.5) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, geyserPatch.clone().add(0, y, 0),
                                10, 0.5, 0.2, 0.5, 0.03);
                    }
                    DisplayBuilder.playSound(geyserPatch, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.3f);

                    // Geyser damage and launch
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(geyserPatch) <= 2.25) {
                            p.damage(14.0); // 7 hearts geyser
                            p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 3.0, 0)));
                        }
                    }
                }

                // Regular patch damage
                if (eruptTick % 6 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location sp : soulPatches) {
                            if (p.getLocation().distanceSquared(sp) <= 2.25) {
                                p.damage(6.0);
                                break;
                            }
                        }
                    }
                }

                if (eruptTick % 30 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFireStorm(plugin); }
    }

    // =========================================================================
    // 26. WHISPER SURGE -- radial soul wave sweeps from random point, reflects
    // =========================================================================
    public static class WhisperSurge extends EnvironmentalAttack {

        private Location surgeOrigin;
        private boolean reflected = false;

        public WhisperSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("whisper_surge", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts primary
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Random origin not at center
            double angle = Math.random() * 2 * Math.PI;
            double dist = 3 + Math.random() * 5;
            surgeOrigin = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.5f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || surgeOrigin == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-40 ticks -- floor soul layer
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double sx = center.getX() + (Math.random() - 0.5) * 20;
                        double sz = center.getZ() + (Math.random() - 0.5) * 20;
                        w.spawnParticle(Particle.SOUL, new Location(w, sx, center.getY() + 0.3, sz),
                                3, 0.5, 0.1, 0.5, 0.01);
                    }
                }
                return;
            }

            // Outward wave: 41-60 ticks (~1 second at 2 blocks/tick)
            int waveTick = ticksAlive - 41;
            if (waveTick >= 0 && waveTick <= 20) {
                double waveRadius = waveTick * 2.0;
                DisplayBuilder.particleRing(surgeOrigin.clone().add(0, 1, 0), waveRadius, Particle.SOUL,
                        (int) (waveRadius * 5), null);
                DisplayBuilder.particleRing(surgeOrigin.clone().add(0, 1.5, 0), waveRadius, Particle.SOUL,
                        (int) (waveRadius * 3), null);

                // Wave damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double pDist = p.getLocation().distance(surgeOrigin);
                    if (Math.abs(pDist - waveRadius) < 1.5) {
                        p.damage(6.0); // 3 hearts primary wave
                        DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.7f, 0.6f);
                    }
                }
            }

            // Reflected wave: 61-80 ticks (return from perimeter)
            int reflectTick = ticksAlive - 61;
            if (reflectTick >= 0 && reflectTick <= 20) {
                double maxRadius = 20;
                double waveRadius = maxRadius - reflectTick * 2.0;
                if (waveRadius > 0) {
                    DisplayBuilder.particleRing(surgeOrigin.clone().add(0, 1, 0), waveRadius, Particle.SOUL,
                            (int) (waveRadius * 3), null); // 50% density

                    // Reflected wave damage (half)
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double pDist = p.getLocation().distance(surgeOrigin);
                        if (Math.abs(pDist - waveRadius) < 1.5) {
                            p.damage(6.0); // 3 hearts reflected
                        }
                    }
                }

                if (reflectTick == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_HIT, 0.6f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WhisperSurge(plugin); }
    }

    // =========================================================================
    // 27. MEMORY FLOOD -- phantom replay of a prior boss attack in grey tones
    // =========================================================================
    public static class MemoryFlood extends EnvironmentalAttack {

        private int replayType; // 0-3 for different patterns

        public MemoryFlood(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("memory_flood", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 40% of original ~15 hearts = ~6
            config.setDamageRadius(4.0);
            config.setDurationTicks(400); // 20 seconds (doubled speed)
            config.setCooldownTicks(2400); // 120 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            replayType = (int) (Math.random() * 4);
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-100 ticks (5 seconds) -- darkened canopy
            if (ticksAlive <= 100) {
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double dx = center.getX() + (Math.random() - 0.5) * 20;
                        double dz = center.getZ() + (Math.random() - 0.5) * 20;
                        double dy = center.getY() + 25 + Math.random() * 10;
                        w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, dx, dy, dz), 3, 1, 1, 1, 0.01);
                        DisplayBuilder.dustParticles(new Location(w, dx, dy, dz), 3, 1.0, 50, 50, 50, 1.5f);
                    }
                }
                if (ticksAlive == 60) {
                    // Ghost boss sound
                    Sound[] bossSounds = {Sound.ENTITY_ENDER_DRAGON_GROWL, Sound.ENTITY_ELDER_GUARDIAN_CURSE,
                            Sound.ENTITY_WARDEN_AMBIENT, Sound.ENTITY_ENDER_DRAGON_GROWL};
                    DisplayBuilder.playSound(center, bossSounds[replayType], 0.6f, 0.4f);
                }
                return;
            }

            // Phantom replay: 101-360 ticks -- desaturated attack pattern
            int replayTick = ticksAlive - 101;
            if (replayTick >= 0 && replayTick <= 260) {
                // All replays use END_ROD (ghostly white) as substitute particles
                if (replayTick % 4 == 0) {
                    switch (replayType) {
                        case 0 -> { // Phantom shockwave ring
                            double radius = (replayTick % 40) * 0.5;
                            DisplayBuilder.particleRing(center.clone().add(0, 1, 0), radius, Particle.END_ROD,
                                    (int) (radius * 4), null);
                        }
                        case 1 -> { // Phantom rain
                            for (int i = 0; i < 5; i++) {
                                double rx = center.getX() + (Math.random() - 0.5) * 18;
                                double rz = center.getZ() + (Math.random() - 0.5) * 18;
                                double ry = center.getY() + 20 - (replayTick % 40) * 0.5;
                                w.spawnParticle(Particle.END_ROD, new Location(w, rx, ry, rz),
                                        2, 0.2, 0.2, 0.2, 0.01);
                            }
                        }
                        case 2 -> { // Phantom beam sweep
                            double beamAngle = replayTick * 0.03;
                            for (double d = 0; d < 10; d += 1.0) {
                                double bx = center.getX() + Math.cos(beamAngle) * d;
                                double bz = center.getZ() + Math.sin(beamAngle) * d;
                                w.spawnParticle(Particle.END_ROD, new Location(w, bx, center.getY() + 3, bz),
                                        3, 0.1, 0.1, 0.1, 0.01);
                            }
                        }
                        case 3 -> { // Phantom column barrage
                            if (replayTick % 20 == 0) {
                                double cx = center.getX() + (Math.random() - 0.5) * 14;
                                double cz = center.getZ() + (Math.random() - 0.5) * 14;
                                for (double y = 0; y < 15; y += 1.0) {
                                    w.spawnParticle(Particle.END_ROD, new Location(w, cx, center.getY() + y, cz),
                                            5, 0.3, 0.2, 0.3, 0.02);
                                }
                            }
                        }
                    }
                }

                // Canopy maintained
                if (replayTick % 8 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double dx = center.getX() + (Math.random() - 0.5) * 20;
                        double dz = center.getZ() + (Math.random() - 0.5) * 20;
                        DisplayBuilder.dustParticles(new Location(w, dx, center.getY() + 30, dz),
                                3, 1.0, 50, 50, 50, 1.5f);
                    }
                }
            }

            // Canopy dispersal: 361-400
            if (replayTick > 260) {
                if (replayTick == 261) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MemoryFlood(plugin); }
    }

    // =========================================================================
    // 28. SOUL ANCHOR TETHERS -- four anchor columns with player tethers
    // =========================================================================
    public static class SoulAnchorTethers extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> anchorHandles = new ArrayList<>();
        private final List<Location> anchorPositions = new ArrayList<>();

        public SoulAnchorTethers(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_anchor_tethers", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(320); // 16 seconds
            config.setCooldownTicks(6000); // HP threshold
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts anchor contact
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // N, S, E, W anchors 2 blocks from perimeter
            double offset = 8;
            anchorPositions.add(center.clone().add(0, 0, -offset));  // N
            anchorPositions.add(center.clone().add(0, 0, offset));   // S
            anchorPositions.add(center.clone().add(offset, 0, 0));   // E
            anchorPositions.add(center.clone().add(-offset, 0, 0));  // W
            for (Location ap : anchorPositions) {
                DisplayBuilder.playSound(ap, Sound.BLOCK_SOUL_SAND_PLACE, 0.9f, 0.7f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-80 ticks
            if (ticksAlive <= 80) {
                if (ticksAlive % 5 == 0) {
                    for (Location ap : anchorPositions) {
                        DisplayBuilder.particleRing(ap.clone().add(0, 0.5, 0), 1.0,
                                Particle.SOUL_FIRE_FLAME, 12, null);
                    }
                }
                // Spawn anchor displays at tick 80
                if (ticksAlive == 80) {
                    for (Location ap : anchorPositions) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(ap, Material.SOUL_SAND);
                        h.scale(1.0f, 4.0f, 1.0f).glow(0, 100, 180).interpolation(5, 0);
                        anchorHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                return;
            }

            // Active: 81-320 ticks (12 seconds)
            int activeTick = ticksAlive - 81;
            if (activeTick >= 0 && activeTick <= 240) {
                // Column particles
                if (activeTick % 4 == 0) {
                    for (Location ap : anchorPositions) {
                        for (double y = 0; y < 4; y += 1.0) {
                            w.spawnParticle(Particle.SOUL_FIRE_FLAME, ap.clone().add(0, y, 0),
                                    5, 0.3, 0.3, 0.3, 0.01);
                        }
                    }
                }

                // Tether lines to players
                if (activeTick % 5 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location ap : anchorPositions) {
                            // Draw tether line
                            DisplayBuilder.particleLine(ap.clone().add(0, 4, 0),
                                    p.getLocation().clone().add(0, 1, 0),
                                    Particle.SOUL, 2, null);

                            // Pull if within 3 blocks
                            double dist = p.getLocation().distance(ap);
                            if (dist <= 3 && dist > 0.5) {
                                org.bukkit.util.Vector pull = ap.toVector()
                                        .subtract(p.getLocation().toVector()).normalize().multiply(0.3);
                                p.setVelocity(p.getVelocity().add(pull));
                            }

                            // Contact damage
                            if (dist <= 1.5) {
                                p.damage(10.0); // 5 hearts
                                w.spawnParticle(Particle.SOUL, ap, 20, 0.5, 0.5, 0.5, 0.03);
                            }
                        }
                    }
                }

                if (activeTick % 40 == 0) {
                    DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.2f, 0.8f);
                }
            }

            // Dissolve: last ticks
            if (activeTick > 240) {
                if (activeTick == 241) {
                    for (Location ap : anchorPositions) {
                        DisplayBuilder.playSound(ap, Sound.BLOCK_SOUL_SAND_BREAK, 0.8f, 0.8f);
                        w.spawnParticle(Particle.SOUL, ap.clone().add(0, 2, 0), 20, 1, 2, 1, 0.03);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulAnchorTethers(plugin); }
    }

    // =========================================================================
    // 29. GRIEF ECHO -- text formation and soul silhouettes
    // =========================================================================
    public static class GriefEcho extends EnvironmentalAttack {

        private final List<Location> silhouettePositions = new ArrayList<>();

        public GriefEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("grief_echo", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(1600); // 80 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts per silhouette proximity
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_DEATH, 0.3f, 0.5f);
            // Generate 7 silhouette positions
            for (int i = 0; i < 7; i++) {
                double sx = center.getX() + (Math.random() - 0.5) * 16;
                double sz = center.getZ() + (Math.random() - 0.5) * 16;
                silhouettePositions.add(new Location(w, sx, center.getY(), sz));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-60 ticks -- SOUL text particles at Y+8 (abstract)
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    // Abstract "text" -- scattered SOUL particles in a line at Y+8
                    for (int c = 0; c < 8; c++) {
                        double tx = center.getX() - 4 + c;
                        w.spawnParticle(Particle.SOUL, new Location(w, tx, center.getY() + 8, center.getZ()),
                                3, 0.2, 0.3, 0.1, 0.01);
                    }
                }
                return;
            }

            // Text explosion at tick 61
            if (ticksAlive == 61) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_DEATH, 0.5f, 0.6f);
                w.spawnParticle(Particle.SOUL, center.clone().add(0, 8, 0), 120, 5, 2, 5, 0.05);
            }

            // Silhouettes: 61-80 ticks (20 ticks flash)
            if (ticksAlive > 60 && ticksAlive <= 80) {
                if (ticksAlive % 3 == 0) {
                    for (Location sp : silhouettePositions) {
                        // Humanoid outline
                        for (double y = 0; y < 1.8; y += 0.3) {
                            w.spawnParticle(Particle.SOUL, sp.clone().add(0, y, 0), 2, 0.1, 0.05, 0.1, 0.005);
                        }
                    }
                }

                // Proximity damage
                if (ticksAlive % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location sp : silhouettePositions) {
                            if (p.getLocation().distanceSquared(sp) <= 4.0) { // 2 blocks
                                p.damage(6.0); // 3 hearts
                                break;
                            }
                        }
                    }
                }
            }

            // Silence: 81-140 ticks
            // Secondary wave: 141-160 ticks
            if (ticksAlive > 140 && ticksAlive <= 160) {
                if (ticksAlive == 141) {
                    for (Location sp : silhouettePositions) {
                        w.spawnParticle(Particle.SOUL, sp, 30, 2, 1, 2, 0.03);
                        DisplayBuilder.playSound(sp, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.5f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GriefEcho(plugin); }
    }

    // =========================================================================
    // 30. SOUL WELL COLLAPSE -- central drain reverses into upward surge
    // =========================================================================
    public static class SoulWellCollapse extends EnvironmentalAttack {

        private BlockDisplayHandle wellHandle;
        private boolean surged = false;

        public SoulWellCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_well_collapse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(2000); // 100 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0); // 6 hearts
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 0.6f, 0.4f);
            wellHandle = displayBuilder.spawnBlock(center, Material.SOUL_SAND);
            wellHandle.scale(0.1f, 0.1f, 0.1f).glow(0, 100, 180).interpolation(5, 0);
            spawnedEntities.add(wellHandle.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Drain phase: 0-80 ticks (4 seconds) -- spiraling inward
            if (ticksAlive <= 80) {
                double rate = 20 + ticksAlive * 0.75; // 20 -> 80 particles
                if (ticksAlive % 3 == 0) {
                    int count = (int) (rate / 10);
                    for (int i = 0; i < count; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = 4 + Math.random() * 4;
                        double progress = 1 - (dist / 8.0);
                        Location particlePos = center.clone().add(
                                Math.cos(angle) * dist * (1 - ticksAlive / 80.0),
                                0.5 + Math.random(),
                                Math.sin(angle) * dist * (1 - ticksAlive / 80.0));
                        w.spawnParticle(Particle.SOUL, particlePos, 2, 0.1, 0.1, 0.1, 0.01);
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, particlePos, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                return;
            }

            // Surge at tick 81
            if (!surged) {
                surged = true;
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.3f, 0.3f);
                DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 1.0f, 0.5f);

                // Massive upward surge
                for (int i = 0; i < 80; i++) {
                    double sx = center.getX() + (Math.random() - 0.5) * 8;
                    double sy = center.getY() + Math.random() * 8;
                    double sz = center.getZ() + (Math.random() - 0.5) * 8;
                    w.spawnParticle(Particle.SOUL, new Location(w, sx, sy, sz), 3, 0.5, 2, 0.5, 0.05);
                }

                // Pop display
                if (wellHandle != null) {
                    wellHandle.animateTo(
                            new Vector3f(-2, -2, -2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(4.0f, 4.0f, 4.0f), 1);
                }
            }

            // Post-surge floor emission: 81-200
            if (ticksAlive > 80 && ticksAlive <= 200 && ticksAlive % 5 == 0) {
                for (int i = 0; i < 5; i++) {
                    double fx = center.getX() + (Math.random() - 0.5) * 16;
                    double fz = center.getZ() + (Math.random() - 0.5) * 16;
                    w.spawnParticle(Particle.SOUL, new Location(w, fx, center.getY() + 0.3, fz),
                            2, 0.3, 0.1, 0.3, 0.01);
                }
            }

            if (ticksAlive == 120) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulWellCollapse(plugin); }
    }
}
