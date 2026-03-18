package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.environmental;

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
 * Phase 3 Environmental -- GROUP 10: FINAL CONFLAGRATION
 * 10 attacks (#91-100). Sub-20% HP. The most visually spectacular and dangerous
 * attacks in the Boss 3 fight. Minimum 12 hearts per attack.
 *
 * Design notes:
 * - No status effects
 * - Dweller palette: crimson (200,0,50), orange glow (255,100,0), soul blue (0,150,255)
 * - Maximum visual density and damage
 * - Attack #100 is the cinematic island destruction finale
 * - Materials: MAGMA_BLOCK, NETHERRACK, OBSIDIAN, CRYING_OBSIDIAN, BLACKSTONE, BLACK_CONCRETE
 */
public final class FinalConflagration {

    private FinalConflagration() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrownOfFire(plugin));
        registry.register(new BrimstoneGenesis(plugin));
        registry.register(new ScreamingHellgate(plugin));
        registry.register(new ExtinctionBeam(plugin));
        registry.register(new MassEruption(plugin));
        registry.register(new FractureFinale(plugin));
        registry.register(new DwellersWrathIncarnate(plugin));
        registry.register(new SkyRupture(plugin));
        registry.register(new TheIslandScreams(plugin));
        registry.register(new TheEndOfTheIsland(plugin));
    }

    // =========================================================================
    // 91. CROWN OF FIRE -- massive ring of fire, 26-block diameter, 10 blocks tall
    // =========================================================================
    public static class CrownOfFire extends EnvironmentalAttack {

        private boolean crownActive = false;

        public CrownOfFire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crown_of_fire", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(300); // 15 seconds
            config.setCooldownTicks(240); // 12 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds) -- base positions glow
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (2 * Math.PI * i) / 12;
                        Location baseLoc = center.clone().add(Math.cos(angle) * 13, 0, Math.sin(angle) * 13);
                        w.spawnParticle(Particle.LAVA, baseLoc, 4, 0.3, 0.5, 0.3, 0);
                    }
                }
                return;
            }

            // Spawn crown
            if (!crownActive) {
                crownActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);

                // 12 spire positions
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI * i) / 12;
                    Location spireLoc = center.clone().add(Math.cos(angle) * 13, 0, Math.sin(angle) * 13);

                    // Crown wall columns -- alternating soul fire and flame blocks
                    for (int y = 0; y < 10; y++) {
                        Location colLoc = spireLoc.clone().add(0, y, 0);
                        Material mat = (y % 2 == 0) ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(colLoc, mat);
                        h.scale(2.0f, 1.0f, 0.6f).glow(255, 100, 0)
                                .rotate((float) angle, 0, 1, 0).interpolation(5, 0);
                        spawnedEntities.add(h.entity());
                    }

                    // Fire charge spire at top
                    Location topLoc = spireLoc.clone().add(0, 10, 0);
                    BlockDisplayHandle topH = displayBuilder.spawnBlock(topLoc, Material.NETHER_WART_BLOCK);
                    topH.scale(1.0f, 1.5f, 1.0f).glow(200, 0, 50).interpolation(5, 0);
                    spawnedEntities.add(topH.entity());
                }
            }

            // Crown rotation and particle effects
            if (ticksAlive % 2 == 0) {
                double rotOffset = ticksAlive * 0.02;

                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI * i) / 12 + rotOffset;
                    Location colLoc = center.clone().add(Math.cos(angle) * 13, 5, Math.sin(angle) * 13);

                    // Alternating column particles
                    DisplayBuilder.dustParticles(colLoc, 3, 0.3, 0, 255, 200, 0.8f);
                    DisplayBuilder.dustParticles(colLoc.clone().add(0, -3, 0), 3, 0.3, 255, 90, 0, 0.8f);
                    w.spawnParticle(Particle.LAVA, colLoc.clone().add(0, -4, 0), 2, 0.3, 2.0, 0.3, 0);

                    // Dripping lava from spires
                    if (i % 3 == 0) {
                        Location dripLoc = center.clone().add(Math.cos(angle) * 13, 9, Math.sin(angle) * 13);
                        w.spawnParticle(Particle.DRIPPING_LAVA, dripLoc, 3, 0.3, 2.0, 0.3, 0);
                    }
                }

                // Laser polygon connecting spire tops
                if (ticksAlive % 6 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double a1 = (2 * Math.PI * i) / 12;
                        double a2 = (2 * Math.PI * ((i + 1) % 12)) / 12;
                        Location from = center.clone().add(Math.cos(a1) * 13, 10, Math.sin(a1) * 13);
                        Location to = center.clone().add(Math.cos(a2) * 13, 10, Math.sin(a2) * 13);

                        int linePoints = 5;
                        for (int p = 0; p <= linePoints; p++) {
                            double t = (double) p / linePoints;
                            Location lineLoc = from.clone().add(
                                    (to.getX() - from.getX()) * t, 0, (to.getZ() - from.getZ()) * t);
                            DisplayBuilder.dustParticles(lineLoc, 2, 0.1, 0, 150, 255, 0.7f);
                        }
                    }
                }

                // Crimson haze inside crown
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 12;
                    Location hazeLoc = center.clone().add(Math.cos(angle) * dist, 3 + Math.random() * 4, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(hazeLoc, 2, 0.5, 220, 45, 20, 0.5f);
                }
            }

            // Damage zones
            if (ticksAlive % 4 == 0 && crownActive) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);

                    // Crown wall: instant death tier
                    if (dist >= 12 && dist <= 14) {
                        p.damage(12.0); // 12 hearts/sec
                        DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                    }

                    // Spire drip-lava at perimeter
                    if (dist >= 11 && dist < 12) {
                        p.damage(16.0); // 8 hearts
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrownOfFire(plugin); }
    }

    // =========================================================================
    // 92. BRIMSTONE GENESIS -- ripple wave from Dweller + permanent particle increase
    // =========================================================================
    public static class BrimstoneGenesis extends EnvironmentalAttack {

        private boolean waveTriggered = false;

        public BrimstoneGenesis(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_genesis", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(260); // 13 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Rapid basalt break series
            for (int i = 0; i < 10; i++) {
                int delay = i * 3;
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.6f), delay);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5 seconds) -- Dweller limb-plant visual
            if (ticksAlive <= 30) {
                if (ticksAlive % 5 == 0) {
                    // Ground impact point glows
                    DisplayBuilder.dustParticles(center, 8, 1.0, 255, 100, 0, 1.0f);
                    DisplayBuilder.dustParticles(center, 6, 0.5, 200, 0, 50, 0.8f);
                }
                return;
            }

            if (!waveTriggered) {
                waveTriggered = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);

                // Limb-plant bonus damage to nearby
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 9.0) {
                        p.damage(12.0); // 6 hearts proximity bonus
                        DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                    }
                }
            }

            int waveTick = ticksAlive - 30;
            double waveRadius = waveTick * 0.3; // ~6 blocks per second

            // Ripple wave expanding outward
            if (waveTick % 2 == 0 && waveRadius <= 20) {
                int points = Math.max(8, (int) (waveRadius * 3));
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    Location rippleLoc = center.clone().add(
                            Math.cos(angle) * waveRadius, 0.1, Math.sin(angle) * waveRadius);

                    // Multi-particle wave
                    w.spawnParticle(Particle.LAVA, rippleLoc, 2, 0.2, 0.3, 0.2, 0);
                    DisplayBuilder.dustParticles(rippleLoc, 3, 0.2, 255, 100, 0, 0.8f);
                    DisplayBuilder.dustParticles(rippleLoc, 2, 0.2, 215, 40, 18, 0.6f);
                }
            }

            // Ground-level display oscillation
            if (waveTick % 6 == 0 && waveRadius <= 18) {
                for (int i = 0; i < 6; i++) {
                    double angle = (2 * Math.PI * i) / 6;
                    Location tileLoc = center.clone().add(
                            Math.cos(angle) * waveRadius, 0.15, Math.sin(angle) * waveRadius);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(tileLoc, Material.MAGMA_BLOCK);
                    h.scale(1.2f, 0.3f, 1.2f).glow(200, 0, 50).interpolation(5, 0);
                    spawnedEntities.add(h.entity());

                    // Animate vertical oscillation
                    BlockDisplay bd = h.entity();
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (bd.isValid()) {
                            bd.setTransformation(new Transformation(
                                    new Vector3f(-0.6f, 0.0f, -0.6f),
                                    new AxisAngle4f(0, 0, 1, 0),
                                    new Vector3f(1.2f, 0.1f, 1.2f),
                                    new AxisAngle4f(0, 0, 1, 0)
                            ));
                            bd.setInterpolationDelay(0);
                            bd.setInterpolationDuration(5);
                        }
                    }, 5L);
                }
            }

            // Wave damage -- 12 hearts contact
            if (waveTick % 3 == 0 && waveRadius <= 20) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (Math.abs(dist - waveRadius) < 2.0) {
                        p.damage(24.0); // 12 hearts wave contact
                        DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneGenesis(plugin); }
    }

    // =========================================================================
    // 93. SCREAMING HELLGATE -- void portal in floor with rotating beam
    // =========================================================================
    public static class ScreamingHellgate extends EnvironmentalAttack {

        private Location gateLoc;
        private boolean gateOpen = false;

        public ScreamingHellgate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("screaming_hellgate", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(10.0); // 5 hearts/sec ground outflow
            config.setDamageRadius(6.0);
            config.setDurationTicks(240); // 12 seconds
            config.setCooldownTicks(280); // 14 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * 2 * Math.PI;
            double dist = 3 + Math.random() * 6;
            gateLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

            DisplayBuilder.playSound(gateLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5 seconds) -- warped spore at gate location
            if (ticksAlive <= 30) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(gateLoc, 8, 0.8, 0, 170, 150, 0.7f);
                }
                return;
            }

            // Open gate
            if (!gateOpen) {
                gateOpen = true;
                DisplayBuilder.playSound(gateLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.4f);

                // 4x4 void circle display
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location holeLoc = gateLoc.clone().add(x, -0.3, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(holeLoc, Material.BLACK_CONCRETE);
                        h.scale(1.2f, 0.5f, 1.2f).glow(0, 150, 255).interpolation(5, 0);
                        spawnedEntities.add(h.entity());
                    }
                }

                // Crying obsidian rim
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    Location rimLoc = gateLoc.clone().add(Math.cos(angle) * 2.5, 0.05, Math.sin(angle) * 2.5);
                    BlockDisplayHandle rimH = displayBuilder.spawnBlock(rimLoc, Material.CRYING_OBSIDIAN);
                    rimH.scale(0.8f, 0.2f, 0.8f).glow(200, 0, 50).interpolation(5, 0);
                    spawnedEntities.add(rimH.entity());
                }
            }

            int gateTick = ticksAlive - 30;

            // Rotating beam pillar
            double beamAngle = gateTick * 0.08; // Slow rotation

            if (gateTick % 2 == 0) {
                // Central eruption pillar
                for (int y = 0; y < 20; y += 2) {
                    Location pillarLoc = gateLoc.clone().add(0, y, 0);
                    DisplayBuilder.dustParticles(pillarLoc, 4, 0.3, 225, 40, 18, 1.0f);
                    DisplayBuilder.dustParticles(pillarLoc, 3, 0.2, 255, 70, 0, 0.8f);
                }

                // Rotating beam sweep at ground level
                for (int d = 1; d <= 12; d++) {
                    Location beamLoc = gateLoc.clone().add(
                            Math.cos(beamAngle) * d, 1.5, Math.sin(beamAngle) * d);
                    DisplayBuilder.dustParticles(beamLoc, 3, 0.2, 255, 70, 0, 0.8f);
                    DisplayBuilder.dustParticles(beamLoc, 2, 0.2, 0, 255, 200, 0.6f);
                }

                // Soul fire rim orbit
                for (int i = 0; i < 8; i++) {
                    double orbitAngle = (2 * Math.PI * i) / 8 + gateTick * 0.1;
                    Location orbitLoc = gateLoc.clone().add(Math.cos(orbitAngle) * 2.5, 0.5, Math.sin(orbitAngle) * 2.5);
                    DisplayBuilder.dustParticles(orbitLoc, 3, 0.2, 0, 150, 255, 0.7f);
                    w.spawnParticle(Particle.LAVA, orbitLoc, 1, 0.1, 0.1, 0.1, 0);
                }

                // Soul particle horizontal outflow
                for (int i = 0; i < 6; i++) {
                    double outAngle = Math.random() * 2 * Math.PI;
                    double outDist = 1 + Math.random() * 5;
                    Location outLoc = gateLoc.clone().add(Math.cos(outAngle) * outDist, 0.2, Math.sin(outAngle) * outDist);
                    DisplayBuilder.dustParticles(outLoc, 2, 0.3, 0, 255, 240, 0.5f);
                }

                // Sustained scream sound
                if (gateTick % 20 == 0) {
                    DisplayBuilder.playSound(gateLoc, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.6f);
                }
            }

            // Beam sweep damage
            if (gateTick % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    // Check if in beam path
                    double dx = p.getLocation().getX() - gateLoc.getX();
                    double dz = p.getLocation().getZ() - gateLoc.getZ();
                    double playerAngle = Math.atan2(dz, dx);
                    double angleDiff = Math.abs(playerAngle - beamAngle);
                    if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    if (angleDiff < 0.2 && dist > 2 && dist < 13) {
                        p.damage(14.0); // 14 hearts beam contact
                        DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScreamingHellgate(plugin); }
    }

    // =========================================================================
    // 94. EXTINCTION BEAM -- horizontal beam 3 blocks wide across arena
    // =========================================================================
    public static class ExtinctionBeam extends EnvironmentalAttack {

        private Location beamDirection;
        private double beamAngle;
        private boolean beamFired = false;

        public ExtinctionBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("extinction_beam", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds (2 charge + 4 beam)
            config.setCooldownTicks(300); // 15 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Target nearest player
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double d = p.getLocation().distanceSquared(center);
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = p;
                }
            }

            if (nearest != null) {
                beamAngle = Math.atan2(
                        nearest.getLocation().getZ() - center.getZ(),
                        nearest.getLocation().getX() - center.getX());
            } else {
                beamAngle = Math.random() * 2 * Math.PI;
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Charge: 40 ticks (2 seconds) -- energy build at Dweller head
            if (ticksAlive <= 40) {
                if (ticksAlive % 3 == 0) {
                    double chargeSize = ticksAlive / 40.0 * 2.0;
                    for (int i = 0; i < (int) (chargeSize * 5); i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        Location chargeLoc = center.clone().add(
                                Math.cos(angle) * chargeSize * 0.5, 1.5 + Math.random() * 0.5,
                                Math.sin(angle) * chargeSize * 0.5);
                        DisplayBuilder.dustParticles(chargeLoc, 3, 0.2, 0, 255, 220, 1.0f);
                    }

                    // Sustained wither ambient during charge
                    if (ticksAlive % 10 == 0) {
                        DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
                    }
                }
                return;
            }

            // FIRE BEAM
            if (!beamFired) {
                beamFired = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            }

            int beamTick = ticksAlive - 40;

            // Beam visualization: 3 blocks wide, 35 blocks long, player height
            if (beamTick % 2 == 0 && beamTick <= 80) {
                double perpAngle = beamAngle + Math.PI / 2;

                for (int d = 1; d <= 18; d++) {
                    for (double w2 = -1.5; w2 <= 1.5; w2 += 0.5) {
                        Location beamLoc = center.clone().add(
                                Math.cos(beamAngle) * d + Math.cos(perpAngle) * w2,
                                1.2,
                                Math.sin(beamAngle) * d + Math.sin(perpAngle) * w2);

                        // Dense opaque particle wall
                        DisplayBuilder.dustParticles(beamLoc, 3, 0.1, 255, 60, 0, 1.2f);
                        DisplayBuilder.dustParticles(beamLoc, 2, 0.1, 0, 255, 200, 0.8f);
                    }

                    // Lava particles in beam core
                    Location coreLoc = center.clone().add(Math.cos(beamAngle) * d, 1.2, Math.sin(beamAngle) * d);
                    w.spawnParticle(Particle.LAVA, coreLoc, 2, 0.3, 0.2, 0.3, 0);

                    // Fire charge displays in beam
                    if (d % 4 == 0 && beamTick % 6 == 0) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(coreLoc, Material.NETHER_WART_BLOCK);
                        float rot = beamTick * 0.3f + d;
                        h.scale(0.4f, 0.4f, 0.4f).glow(200, 0, 50)
                                .rotate(rot, 0.5f, 1, 0.3f).interpolation(3, 0);
                        spawnedEntities.add(h.entity());
                    }
                }

                // Smoke billowing from beam sides
                for (int d = 2; d <= 16; d += 3) {
                    double perpDist = 2.0;
                    for (int side = -1; side <= 1; side += 2) {
                        Location smokeLoc = center.clone().add(
                                Math.cos(beamAngle) * d + Math.cos(perpAngle) * perpDist * side,
                                1.5,
                                Math.sin(beamAngle) * d + Math.sin(perpAngle) * perpDist * side);
                        w.spawnParticle(Particle.SMOKE, smokeLoc, 3, 0.3, 0.5, 0.3, 0.02);
                    }
                }
            }

            // Beam damage: 15 hearts/sec inside beam
            if (beamTick % 4 == 0 && beamTick <= 80) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    double dx = pl.getX() - center.getX();
                    double dz = pl.getZ() - center.getZ();

                    // Project onto beam direction
                    double along = dx * Math.cos(beamAngle) + dz * Math.sin(beamAngle);
                    double perp = Math.abs(-dx * Math.sin(beamAngle) + dz * Math.cos(beamAngle));

                    if (along > 0 && along < 18 && perp < 1.5 && Math.abs(pl.getY() - center.getY() - 1.2) < 1.5) {
                        p.damage(15.0); // 15 hearts/sec
                        DisplayBuilder.crimsonDust(pl, 12, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ExtinctionBeam(plugin); }
    }

    // =========================================================================
    // 95. MASS ERUPTION -- entire island erupts, no safe zones
    // =========================================================================
    public static class MassEruption extends EnvironmentalAttack {

        private boolean erupted = false;

        public MassEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mass_eruption", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds total
            config.setCooldownTicks(320); // 16 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5 seconds) -- deep throb
            if (ticksAlive <= 30) {
                if (ticksAlive == 15) {
                    // Single visible ground pulse
                    for (int i = 0; i < 20; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 14;
                        Location pulseLoc = center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                        DisplayBuilder.dustParticles(pulseLoc, 3, 0.3, 255, 100, 0, 0.8f);
                    }
                }
                return;
            }

            // ERUPTION PEAK: 3 seconds (ticks 30-90)
            if (!erupted) {
                erupted = true;

                // All sounds simultaneously
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.6f);

                // Guaranteed 12 hearts to ALL players
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    p.damage(24.0); // 12 hearts
                    DisplayBuilder.crimsonDust(p.getLocation(), 15, 1.0);
                }
            }

            int eruptTick = ticksAlive - 30;

            // Forest of alternating orange-lava and teal-soul pillars
            if (eruptTick % 2 == 0 && eruptTick <= 60) {
                for (int i = 0; i < 30; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 14;
                    Location pillarBase = center.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);

                    boolean isLava = Math.random() > 0.4;
                    for (int y = 0; y < 8; y++) {
                        Location pillarLoc = pillarBase.clone().add(0, y, 0);
                        if (isLava) {
                            w.spawnParticle(Particle.LAVA, pillarLoc, 3, 0.2, 0.3, 0.2, 0);
                            DisplayBuilder.dustParticles(pillarLoc, 2, 0.2, 255, 100, 0, 0.8f);
                        } else {
                            DisplayBuilder.dustParticles(pillarLoc, 3, 0.2, 0, 255, 240, 0.8f);
                            DisplayBuilder.dustParticles(pillarLoc, 2, 0.2, 0, 170, 150, 0.6f);
                        }
                    }
                }

                // Smoke fills space between pillars at 4-block height
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 14;
                    Location smokeLoc = center.clone().add(Math.cos(angle) * dist, 4, Math.sin(angle) * dist);
                    w.spawnParticle(Particle.SMOKE, smokeLoc, 5, 0.5, 1.0, 0.5, 0.03);
                }

                // Sky fully obscured
                DisplayBuilder.dustParticles(center.clone().add(0, 10, 0), 15, 10.0, 80, 30, 10, 2.0f);
            }

            // Pillars recede after peak
            if (eruptTick > 60 && eruptTick % 4 == 0) {
                float fade = (eruptTick - 60) / 40.0f;
                int particleCount = Math.max(1, (int) (15 * (1 - fade)));
                for (int i = 0; i < particleCount; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 14;
                    Location loc = center.clone().add(Math.cos(angle) * dist, Math.random() * (8 * (1 - fade)), Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(loc, 2, 0.3, 255, 100, 0, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MassEruption(plugin); }
    }

    // =========================================================================
    // 96. FRACTURE FINALE -- four corners collapse simultaneously
    // =========================================================================
    public static class FractureFinale extends EnvironmentalAttack {

        private final Location[] cornerLocations = new Location[4];
        private boolean fractured = false;

        public FractureFinale(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fracture_finale", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(260); // 13 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);

            double[] angles = {Math.PI / 4, 3 * Math.PI / 4, 5 * Math.PI / 4, 7 * Math.PI / 4};
            for (int i = 0; i < 4; i++) {
                cornerLocations[i] = center.clone().add(Math.cos(angles[i]) * 12, 0, Math.sin(angles[i]) * 12);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds) -- corners flash
            if (ticksAlive <= 40) {
                if (ticksAlive % 3 == 0) {
                    boolean orange = (ticksAlive / 3) % 2 == 0;
                    for (Location corner : cornerLocations) {
                        if (corner == null) continue;
                        if (orange) {
                            DisplayBuilder.dustParticles(corner, 5, 1.0, 255, 100, 0, 0.8f);
                        } else {
                            DisplayBuilder.dustParticles(corner, 5, 1.0, 20, 20, 20, 0.8f);
                        }
                    }
                }
                return;
            }

            // FRACTURE
            if (!fractured) {
                fractured = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 2.0f, 0.3f);

                for (Location corner : cornerLocations) {
                    if (corner == null) continue;

                    // Corner collapse displays
                    for (int x = -2; x <= 2; x++) {
                        for (int z = -2; z <= 2; z++) {
                            Location tileLoc = corner.clone().add(x, 0.05, z);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(tileLoc, Material.BLACKSTONE);
                            h.scale(0.9f, 0.15f, 0.9f).glow(200, 0, 50).interpolation(5, 0);
                            spawnedEntities.add(h.entity());

                            // Sink tiles
                            BlockDisplay bd = h.entity();
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                if (bd.isValid()) {
                                    Location sinkLoc = bd.getLocation();
                                    sinkLoc.setY(sinkLoc.getY() - 15);
                                    bd.teleport(sinkLoc);
                                }
                            }, 10L);
                        }
                    }

                    // Massive void-lava interaction columns: 15 blocks tall
                    BlockDisplayHandle colH = displayBuilder.spawnBlock(corner.clone().add(0, 7, 0), Material.CRYING_OBSIDIAN);
                    colH.scale(2.0f, 12.0f, 2.0f).glow(0, 150, 255).interpolation(10, 0);
                    spawnedEntities.add(colH.entity());

                    // Lava explosion from each corner
                    w.spawnParticle(Particle.LAVA, corner.clone().add(0, 1, 0), 20, 2.0, 1.0, 2.0, 0);

                    // Soul particle cascading waterfalls from column tops
                    DisplayBuilder.dustParticles(corner.clone().add(0, 15, 0), 15, 2.0, 0, 255, 240, 1.0f);

                    // Damage players at corners
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(corner) <= 9.0) {
                            p.damage(20.0); // 10 hearts fall-impact
                            DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                        }
                    }
                }
            }

            int fracTick = ticksAlive - 40;

            // Alternating LAVA and SOUL_FIRE_FLAME vertical bands in columns
            if (fracTick % 3 == 0) {
                for (Location corner : cornerLocations) {
                    if (corner == null) continue;
                    for (int y = 0; y < 15; y += 2) {
                        Location bandLoc = corner.clone().add(0, y, 0);
                        if (y % 4 == 0) {
                            DisplayBuilder.dustParticles(bandLoc, 3, 0.5, 255, 85, 0, 1.0f);
                        } else {
                            DisplayBuilder.dustParticles(bandLoc, 3, 0.5, 0, 255, 200, 1.0f);
                        }
                    }

                    // Soul cascade from tops
                    DisplayBuilder.dustParticles(corner.clone().add(
                                    (Math.random() - 0.5) * 4, 14, (Math.random() - 0.5) * 4),
                            3, 0.5, 0, 255, 240, 0.8f);
                }
            }

            // Void-lava column contact damage
            if (fracTick % 5 == 0) {
                for (Location corner : cornerLocations) {
                    if (corner == null) continue;
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist2d = Math.sqrt(
                                Math.pow(p.getLocation().getX() - corner.getX(), 2) +
                                Math.pow(p.getLocation().getZ() - corner.getZ(), 2));
                        if (dist2d <= 3.0) {
                            p.damage(24.0); // 12 hearts column contact
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FractureFinale(plugin); }
    }

    // =========================================================================
    // 97. DWELLER'S WRATH INCARNATE -- 8 rotating laser lines from Dweller
    // =========================================================================
    public static class DwellersWrathIncarnate extends EnvironmentalAttack {

        private boolean wrathActive = false;

        public DwellersWrathIncarnate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dwellers_wrath_incarnate", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(300); // 15 seconds
            config.setCooldownTicks(280); // 14 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.5f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 2, 0), 30, 1.0, 1.5, 1.0, 0.03);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 50 ticks (2.5 seconds) -- supercharge build
            if (ticksAlive <= 50) {
                if (ticksAlive % 4 == 0) {
                    float scale = 1.0f + (ticksAlive / 50.0f) * 0.3f;
                    // Pulsing scale effect on Dweller position
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 10, scale, 0, 255, 200, 1.0f);
                    DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 8, scale, 255, 80, 0, 0.8f);
                    w.spawnParticle(Particle.SMOKE, center.clone().add(0, 2, 0), 5, 0.5, 1.0, 0.5, 0.02);
                }
                return;
            }

            if (!wrathActive) {
                wrathActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);

                // Crimson aura cloud display
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    Location aurLoc = center.clone().add(Math.cos(angle) * 3, 1, Math.sin(angle) * 3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(aurLoc, Material.NETHER_WART_BLOCK);
                    h.scale(0.5f, 0.5f, 0.5f).glow(225, 45, 20).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            int wrathTick = ticksAlive - 50;

            // 8 laser lines rotating clockwise -- 1 revolution per 5 seconds (100 ticks)
            double rotationOffset = wrathTick * (2 * Math.PI / 100);

            if (wrathTick % 2 == 0) {
                for (int beam = 0; beam < 8; beam++) {
                    double beamAngle = (2 * Math.PI * beam) / 8 + rotationOffset;

                    // Draw beam particles
                    for (int d = 2; d <= 15; d++) {
                        Location beamLoc = center.clone().add(
                                Math.cos(beamAngle) * d, 1.2, Math.sin(beamAngle) * d);
                        DisplayBuilder.dustParticles(beamLoc, 3, 0.1, 0, 255, 200, 0.9f);
                    }

                    // Floor burn patterns at beam intersection with brimstone
                    if (wrathTick % 8 == 0) {
                        for (int d = 4; d <= 14; d += 4) {
                            Location burnLoc = center.clone().add(
                                    Math.cos(beamAngle) * d, 0.02, Math.sin(beamAngle) * d);
                            DisplayBuilder.dustParticles(burnLoc, 3, 0.2, 200, 0, 50, 0.6f);
                        }
                    }
                }

                // Dweller body particles at 130% scale effect
                DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 6, 0.8, 0, 255, 200, 1.0f);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 5, 0.6, 255, 80, 0, 0.8f);

                // Crimson aura cloud
                DisplayBuilder.dustParticles(center.clone().add(0, 1.2, 0), 4, 3.0, 225, 45, 20, 0.6f);
            }

            // Laser damage: 13 hearts/sec contact
            if (wrathTick % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = p.getLocation().getX() - center.getX();
                    double dz = p.getLocation().getZ() - center.getZ();
                    double playerAngle = Math.atan2(dz, dx);
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    if (dist < 2 || dist > 16) continue;

                    for (int beam = 0; beam < 8; beam++) {
                        double beamAngle = (2 * Math.PI * beam) / 8 + rotationOffset;
                        double angleDiff = Math.abs(playerAngle - beamAngle);
                        if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;

                        if (angleDiff < 0.12) { // Narrow beam width
                            p.damage(13.0); // 13 hearts/sec
                            DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                            break;
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DwellersWrathIncarnate(plugin); }
    }

    // =========================================================================
    // 98. SKY RUPTURE -- sky tears open, three void-energy drops fall
    // =========================================================================
    public static class SkyRupture extends EnvironmentalAttack {

        private final Location[] dropTargets = new Location[3];
        private boolean dropsStarted = false;

        public SkyRupture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_rupture", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(6.0); // 3 hearts/sec soul curtain to all
            config.setDamageRadius(20.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(300); // 15 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);

            // Drop targets
            dropTargets[0] = center.clone().add(0, 0, 0); // Near center
            // Near player group
            Player nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double d = p.getLocation().distanceSquared(center);
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = p;
                }
            }
            dropTargets[1] = (nearest != null) ? nearest.getLocation().clone() :
                    center.clone().add(5, 0, 5);
            // Random edge
            double edgeAngle = Math.random() * 2 * Math.PI;
            dropTargets[2] = center.clone().add(Math.cos(edgeAngle) * 10, 0, Math.sin(edgeAngle) * 10);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rift appears: 60 ticks (3 seconds) of warning
            if (ticksAlive <= 60) {
                // Rift display at sky ceiling
                if (ticksAlive == 10) {
                    Location riftLoc = center.clone().add(0, 28, 0);
                    for (int x = -3; x <= 3; x++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockDisplayHandle h = displayBuilder.spawnBlock(
                                    riftLoc.clone().add(x * 1.5, z * 0.5, 0), Material.BLACK_CONCRETE);
                            h.scale(1.5f, 0.5f, 3.0f).glow(0, 150, 255).interpolation(10, 0);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }

                // Soul curtain from rift
                if (ticksAlive % 4 == 0 && ticksAlive > 20) {
                    for (int i = 0; i < 10; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 12;
                        double height = 25 - Math.random() * 20;
                        Location curtainLoc = center.clone().add(Math.cos(angle) * dist, height, Math.sin(angle) * dist);
                        DisplayBuilder.dustParticles(curtainLoc, 3, 0.5, 0, 255, 250, 0.7f);
                    }
                }

                // Rift edge particles
                if (ticksAlive % 6 == 0) {
                    for (int x = -4; x <= 4; x++) {
                        Location edgeLoc = center.clone().add(x * 1.5, 28, 0);
                        DisplayBuilder.dustParticles(edgeLoc, 3, 0.3, 0, 185, 165, 0.6f);
                    }
                }

                return;
            }

            // Drops descend
            if (!dropsStarted) {
                dropsStarted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            }

            int dropTick = ticksAlive - 60;

            // 3 drops descending over 100 ticks (5 seconds)
            for (int d = 0; d < 3; d++) {
                if (dropTargets[d] == null) continue;

                // Staggered start: each drop starts 15 ticks apart
                int dropStart = d * 15;
                int localTick = dropTick - dropStart;
                if (localTick < 0 || localTick > 100) continue;

                double height = 25 - (localTick / 100.0) * 25;

                if (height > 0) {
                    // Descending 3x3 cluster
                    Location dropLoc = dropTargets[d].clone().add(0, height, 0);

                    if (dropTick % 3 == 0) {
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                Location clusterLoc = dropLoc.clone().add(x, 0, z);
                                DisplayBuilder.dustParticles(clusterLoc, 3, 0.2, 0, 255, 210, 0.8f);
                                w.spawnParticle(Particle.LAVA, clusterLoc, 1, 0.1, 0.3, 0.1, 0);
                            }
                        }

                        // Trail particles
                        DisplayBuilder.dustParticles(dropLoc.clone().add(0, 2, 0), 4, 0.5, 0, 150, 255, 0.6f);
                    }

                    // Drop displays
                    if (localTick % 10 == 0) {
                        BlockDisplayHandle dh = displayBuilder.spawnBlock(dropLoc, Material.OBSIDIAN);
                        dh.scale(2.0f, 2.0f, 2.0f).glow(0, 150, 255).interpolation(5, 0);
                        spawnedEntities.add(dh.entity());

                        // Remove after 12 ticks
                        BlockDisplay bd = dh.entity();
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (bd.isValid()) bd.remove();
                        }, 12L);
                    }
                } else if (height <= 0 && localTick <= 102) {
                    // IMPACT
                    if (localTick == (int) (100.0) || (localTick == 100 - dropStart + dropStart)) {
                        // Only trigger once per drop
                    }

                    // Impact check on the exact frame
                    if (Math.abs(height) < 0.5 && localTick % 5 == 0) {
                        Location impactLoc = dropTargets[d];

                        // 5-block explosion
                        DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                        w.spawnParticle(Particle.LAVA, impactLoc.clone().add(0, 1, 0), 30, 5.0, 1.0, 5.0, 0);
                        DisplayBuilder.dustParticles(impactLoc, 15, 5.0, 255, 90, 0, 1.2f);
                        DisplayBuilder.dustParticles(impactLoc, 12, 4.0, 0, 255, 240, 1.0f);

                        // Tiered impact damage
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double playerDist = p.getLocation().distance(impactLoc);
                            if (playerDist <= 1.5) {
                                p.damage(28.0); // 14 hearts direct
                            } else if (playerDist <= 3.0) {
                                p.damage(20.0); // 10 hearts inner radius
                            } else if (playerDist <= 5.0) {
                                p.damage(14.0); // 7 hearts outer radius
                            }
                        }
                    }
                }
            }

            // Continuous soul curtain across entire arena
            if (dropTick % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 14;
                    double height = Math.random() * 20;
                    Location curtainLoc = center.clone().add(Math.cos(angle) * dist, height, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(curtainLoc, 2, 0.5, 0, 255, 250, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyRupture(plugin); }
    }

    // =========================================================================
    // 99. THE ISLAND SCREAMS -- 5 expanding damage rings from center
    // =========================================================================
    public static class TheIslandScreams extends EnvironmentalAttack {

        private final double[] ringProgress = new double[5];
        private boolean screaming = false;

        public TheIslandScreams(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_island_screams", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(160); // 8 seconds (Attack #100 follows)
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // No visual warning -- audio cacophony IS the signal
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (!screaming) {
                screaming = true;

                // ALL 7 sounds simultaneously -- sustained
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 2.0f, 0.5f);
            }

            // 5 rings, one per second, expanding at 5 blocks/sec
            // Each ring is 2 blocks wide, gaps are 3 blocks
            for (int ring = 0; ring < 5; ring++) {
                int ringStartTick = ring * 20; // 1 ring per second

                if (ticksAlive < ringStartTick) continue;

                int ringAge = ticksAlive - ringStartTick;
                double radius = ringAge * 0.25; // 5 blocks per second
                ringProgress[ring] = radius;

                if (radius > 25) continue; // Ring has passed arena boundary

                // Draw ring
                if (ticksAlive % 2 == 0) {
                    int points = Math.max(8, (int) (radius * 3));
                    for (int p = 0; p < points; p++) {
                        double angle = (2 * Math.PI * p) / points;
                        Location ringLoc = center.clone().add(
                                Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);
                        DisplayBuilder.dustParticles(ringLoc, 3, 0.1, 0, 255, 200, 1.0f);
                    }
                }

                // Ring damage: players within ring width (2 blocks)
                if (ticksAlive % 3 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distance(center);
                        if (Math.abs(dist - radius) < 1.0) { // 2-block wide ring
                            p.damage(24.0); // 12 hearts per ring
                            DisplayBuilder.crimsonDust(p.getLocation(), 12, 0.5);
                        }
                    }
                }
            }

            // Cracks racing across all surfaces
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 15;
                    Location crackLoc = center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(crackLoc, 2, 0.5, 140, 135, 125, 0.5f);
                }
            }

            // Lava fountains from every tile
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 15; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 14;
                    Location lavaLoc = center.clone().add(Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist);
                    w.spawnParticle(Particle.LAVA, lavaLoc, 3, 0.3, 1.0, 0.3, 0);
                }
            }

            // Void plumes from gaps
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI / 2) * i;
                    Location plumeLoc = center.clone().add(Math.cos(angle) * 14, 0, Math.sin(angle) * 14);
                    for (int y = 0; y < 10; y += 2) {
                        DisplayBuilder.dustParticles(plumeLoc.clone().add(0, y, 0), 3, 0.5, 0, 255, 240, 0.8f);
                        DisplayBuilder.dustParticles(plumeLoc.clone().add(0, y, 0), 2, 0.4, 0, 175, 155, 0.6f);
                    }
                }
            }

            // Smoke blocks sky
            if (ticksAlive % 8 == 0) {
                w.spawnParticle(Particle.SMOKE, center.clone().add(0, 10, 0), 20, 10.0, 2.0, 10.0, 0.05);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheIslandScreams(plugin); }
    }

    // =========================================================================
    // 100. THE END OF THE ISLAND -- cinematic multi-phase arena destruction
    // =========================================================================
    public static class TheEndOfTheIsland extends EnvironmentalAttack {

        private boolean phase1Started = false;
        private boolean phase2Started = false;
        private boolean phase3Started = false;
        private boolean phase4Started = false;

        public TheEndOfTheIsland(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_end_of_the_island", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(420); // 21 seconds
            config.setCooldownTicks(999999); // N/A -- this is the FINAL environmental attack
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // No warning -- begins immediately after Attack #99
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // ============================================
            // PHASE 1: THE CRACK BENEATH (0-60 ticks / 0-3 seconds)
            // ============================================
            if (ticksAlive <= 60) {
                if (!phase1Started) {
                    phase1Started = true;
                    // All ambient sounds cut to silence
                }

                // Every tile emits crack particles
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 14;
                        Location crackLoc = center.clone().add(Math.cos(angle) * dist, 0.05, Math.sin(angle) * dist);

                        // Shattered glass crack lines
                        DisplayBuilder.dustParticles(crackLoc, 3, 0.6, 140, 135, 125, 0.5f);

                        // Amber-void underglow from below
                        DisplayBuilder.dustParticles(crackLoc.clone().add(0, -0.3, 0), 2, 0.3, 255, 75, 0, 0.7f);
                        DisplayBuilder.dustParticles(crackLoc.clone().add(0, -0.3, 0), 2, 0.2, 0, 255, 245, 0.6f);
                    }

                    // Vibrating displays
                    if (ticksAlive % 6 == 0) {
                        for (int i = 0; i < 5; i++) {
                            double angle = Math.random() * 2 * Math.PI;
                            double dist = 2 + Math.random() * 10;
                            Location dLoc = center.clone().add(Math.cos(angle) * dist, 0.05, Math.sin(angle) * dist);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(dLoc, Material.MAGMA_BLOCK);
                            float jitter = (float) ((Math.random() - 0.5) * 0.2);
                            h.scale(1.0f, 0.1f, 1.0f).glow(200, 0, 50)
                                    .translate(-0.5f + jitter, -0.05f, -0.5f + jitter)
                                    .interpolation(2, 0);
                            spawnedEntities.add(h.entity());
                        }
                    }

                    // Dripping lava curtain underneath
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 14;
                        Location dripLoc = center.clone().add(Math.cos(angle) * dist, -1, Math.sin(angle) * dist);
                        w.spawnParticle(Particle.DRIPPING_LAVA, dripLoc, 2, 0.5, 0, 0.5, 0);
                    }
                }

                // Phase 1 damage: 4 hearts/sec unavoidable
                if (ticksAlive % 5 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        p.damage(4.0); // ~4 hearts/sec total
                    }
                }

                return;
            }

            // ============================================
            // PHASE 2: THE FIRST RUPTURE (60-120 ticks / 3-6 seconds)
            // ============================================
            if (ticksAlive <= 120) {
                if (!phase2Started) {
                    phase2Started = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.3f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.4f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.5f);

                    // Central column display -- 6 blocks wide, 40 blocks tall (capped at reasonable visual)
                    for (int y = 0; y < 20; y += 2) {
                        BlockDisplayHandle colH = displayBuilder.spawnBlock(
                                center.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                        colH.scale(4.0f, 2.0f, 4.0f).glow(0, 150, 255).interpolation(5, 0);
                        spawnedEntities.add(colH.entity());
                    }
                }

                // Central column particles -- tallest in fight
                if (ticksAlive % 2 == 0) {
                    for (int y = 0; y < 30; y += 2) {
                        Location colLoc = center.clone().add(0, y, 0);
                        DisplayBuilder.dustParticles(colLoc, 5, 1.5, 0, 255, 200, 1.2f);
                        DisplayBuilder.dustParticles(colLoc, 4, 1.0, 255, 50, 0, 1.0f);
                        w.spawnParticle(Particle.LAVA, colLoc, 3, 1.5, 0.5, 1.5, 0);
                        DisplayBuilder.dustParticles(colLoc, 3, 0.8, 230, 45, 20, 0.8f);
                    }
                }

                // 8 laser lines in octagon from column base
                if (ticksAlive % 2 == 0) {
                    for (int beam = 0; beam < 8; beam++) {
                        double beamAngle = (2 * Math.PI * beam) / 8;
                        for (int d = 3; d <= 20; d += 2) {
                            Location beamLoc = center.clone().add(
                                    Math.cos(beamAngle) * d, 0.3, Math.sin(beamAngle) * d);
                            DisplayBuilder.dustParticles(beamLoc, 3, 0.1, 0, 255, 215, 0.9f);
                        }
                    }

                    // Smoke at laser intersections
                    for (int beam = 0; beam < 8; beam += 2) {
                        double beamAngle = (2 * Math.PI * beam) / 8;
                        Location smokeLoc = center.clone().add(
                                Math.cos(beamAngle) * 12, 0.5, Math.sin(beamAngle) * 12);
                        w.spawnParticle(Particle.SMOKE, smokeLoc, 5, 0.5, 1.0, 0.5, 0.03);
                    }
                }

                // Phase 2 damage: lasers 12 hearts/sec, column lethal
                if (ticksAlive % 4 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distance(center);

                        // Column: lethal zone
                        if (dist <= 3) {
                            p.damage(30.0); // Lethal
                            continue;
                        }

                        // Laser lines
                        double dx = p.getLocation().getX() - center.getX();
                        double dz = p.getLocation().getZ() - center.getZ();
                        double playerAngle = Math.atan2(dz, dx);

                        for (int beam = 0; beam < 8; beam++) {
                            double beamAngle = (2 * Math.PI * beam) / 8;
                            double angleDiff = Math.abs(playerAngle - beamAngle);
                            if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;
                            if (angleDiff < 0.1 && dist > 3) {
                                p.damage(12.0); // 12 hearts/sec
                                DisplayBuilder.crimsonDust(p.getLocation(), 10, 0.5);
                                break;
                            }
                        }
                    }
                }

                return;
            }

            // ============================================
            // PHASE 3: THE CASCADE (120-240 ticks / 6-12 seconds)
            // ============================================
            if (ticksAlive <= 240) {
                if (!phase3Started) {
                    phase3Started = true;
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 2.0f, 0.3f);
                }

                int cascadeTick = ticksAlive - 120;
                double collapseRadius = 15 - (cascadeTick * 0.1); // Collapse inward at ~2 bps

                // Collapsing ring visual
                if (cascadeTick % 3 == 0 && collapseRadius > 3) {
                    int points = (int) (collapseRadius * 3);
                    for (int i = 0; i < points; i++) {
                        double angle = (2 * Math.PI * i) / points;
                        Location collapseLoc = center.clone().add(
                                Math.cos(angle) * collapseRadius, 0.05, Math.sin(angle) * collapseRadius);

                        // Sinking display
                        BlockDisplayHandle h = displayBuilder.spawnBlock(collapseLoc, Material.BLACKSTONE);
                        h.scale(0.9f, 0.1f, 0.9f).glow(200, 0, 50).interpolation(3, 0);
                        spawnedEntities.add(h.entity());

                        // Lava explosion per tile
                        w.spawnParticle(Particle.LAVA, collapseLoc, 3, 0.3, 0.5, 0.3, 0);
                        DisplayBuilder.playSound(collapseLoc, Sound.BLOCK_STONE_PLACE, 0.3f, 0.4f);

                        // Soul particles from void below
                        DisplayBuilder.dustParticles(collapseLoc.clone().add(0, -1, 0),
                                3, 0.3, 0, 255, 235, 0.7f);

                        // Sink tile
                        BlockDisplay bd = h.entity();
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (bd.isValid()) {
                                Location sinkLoc = bd.getLocation();
                                sinkLoc.setY(sinkLoc.getY() - 10);
                                bd.teleport(sinkLoc);
                            }
                        }, 8L);
                    }
                }

                // Void columns in collapsed outer ring
                if (cascadeTick % 6 == 0 && collapseRadius > 3) {
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = collapseRadius + 2 + Math.random() * 3;
                        Location voidColLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                        for (int y = 0; y < 8; y += 2) {
                            DisplayBuilder.dustParticles(voidColLoc.clone().add(0, y, 0), 3, 0.5, 0, 180, 155, 0.7f);
                            DisplayBuilder.dustParticles(voidColLoc.clone().add(0, y, 0), 2, 0.3, 0, 255, 240, 0.6f);
                        }
                    }
                }

                // Sky darkening
                if (cascadeTick % 10 == 0) {
                    float darkness = cascadeTick / 120.0f;
                    DisplayBuilder.dustParticles(center.clone().add(0, 15, 0), (int) (10 + darkness * 20),
                            12.0, (int) (50 * (1 - darkness)), (int) (15 * (1 - darkness)), (int) (5 * (1 - darkness)), 2.0f);
                }

                // Damage: on collapsing tiles + void fall protection
                if (cascadeTick % 4 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distance(center);

                        // On collapsing ring
                        if (Math.abs(dist - collapseRadius) < 2.0 && collapseRadius > 3) {
                            p.damage(15.0); // 15 hearts collapse-tile
                            // Launch toward center
                            double dx = center.getX() - p.getLocation().getX();
                            double dz = center.getZ() - p.getLocation().getZ();
                            double d = Math.sqrt(dx * dx + dz * dz);
                            if (d > 0.1) {
                                p.setVelocity(p.getVelocity().add(
                                        new org.bukkit.util.Vector(dx / d * 0.6, 0.4, dz / d * 0.6)));
                            }
                        }

                        // Outside the remaining island (fell into void)
                        if (dist > collapseRadius + 2) {
                            // Respawn at center with penalty
                            p.teleport(center.clone().add(0, 1, 0));
                            p.damage(12.0); // 6 hearts respawn penalty
                        }
                    }
                }

                // Central column still blazes
                if (cascadeTick % 4 == 0) {
                    for (int y = 0; y < 20; y += 4) {
                        DisplayBuilder.dustParticles(center.clone().add(0, y, 0), 4, 1.5, 0, 255, 200, 1.0f);
                    }
                }

                return;
            }

            // ============================================
            // PHASE 4: THE FINAL STAND (240-360 ticks / 12-18 seconds)
            // ============================================
            if (ticksAlive <= 360) {
                if (!phase4Started) {
                    phase4Started = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.3f);

                    // 6x6 surviving core platform
                    for (int x = -3; x <= 2; x++) {
                        for (int z = -3; z <= 2; z++) {
                            Location coreLoc = center.clone().add(x, 0.03, z);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(coreLoc, Material.MAGMA_BLOCK);
                            h.scale(0.95f, 0.1f, 0.95f).glow(200, 0, 50).interpolation(5, 0);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }

                int finalTick = ticksAlive - 240;

                // Soul fire halo around entire 6x6 edge
                if (finalTick % 3 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double angle = (2 * Math.PI * i) / 20;
                        Location edgeLoc = center.clone().add(Math.cos(angle) * 3.5, 0.3, Math.sin(angle) * 3.5);
                        DisplayBuilder.dustParticles(edgeLoc, 3, 0.1, 0, 255, 200, 0.8f);
                    }
                }

                // 4 corner lava fountain arcs
                if (finalTick % 4 == 0) {
                    double[][] corners = {{-3, -3}, {2, -3}, {-3, 2}, {2, 2}};
                    for (double[] corner : corners) {
                        Location cornerLoc = center.clone().add(corner[0], 0, corner[1]);
                        double outAngle = Math.atan2(corner[1] - center.getZ() + center.getZ(), corner[0] - center.getX() + center.getX());
                        for (double d = 0; d < 4; d += 0.5) {
                            double height = (4 - d) * 0.5;
                            Location arcLoc = cornerLoc.clone().add(
                                    Math.cos(outAngle) * d, height, Math.sin(outAngle) * d);
                            w.spawnParticle(Particle.LAVA, arcLoc, 1, 0.1, 0.1, 0.1, 0);
                        }
                    }
                }

                // Central column still present above
                if (finalTick % 4 == 0) {
                    for (int y = 5; y < 15; y += 3) {
                        DisplayBuilder.dustParticles(center.clone().add(0, y, 0), 4, 1.0, 0, 255, 200, 0.8f);
                    }
                    // Crimson spore rain from column
                    for (int i = 0; i < 5; i++) {
                        Location rainLoc = center.clone().add(
                                (Math.random() - 0.5) * 6, 10 - Math.random() * 8, (Math.random() - 0.5) * 6);
                        DisplayBuilder.dustParticles(rainLoc, 2, 0.3, 235, 50, 22, 0.6f);
                    }
                }

                // 8 laser lines now pointing inward to 6x6 core
                if (finalTick % 3 == 0) {
                    for (int beam = 0; beam < 8; beam++) {
                        double beamAngle = (2 * Math.PI * beam) / 8;
                        for (int d = 4; d <= 12; d++) {
                            Location beamLoc = center.clone().add(
                                    Math.cos(beamAngle) * d, 0.3, Math.sin(beamAngle) * d);
                            DisplayBuilder.dustParticles(beamLoc, 2, 0.1, 0, 255, 215, 0.7f);
                        }
                    }
                }

                // Void visible all around -- featureless, lit only by blood-orange glow
                if (finalTick % 8 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = 4 + Math.random() * 6;
                        Location voidLoc = center.clone().add(Math.cos(angle) * dist, -2, Math.sin(angle) * dist);
                        DisplayBuilder.dustParticles(voidLoc, 3, 0.5, 120, 40, 10, 0.5f);
                    }
                }

                // Sustained low moan
                if (finalTick % 40 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
                }

                // Phase 4 damage: corner arcs + edge halo
                if (finalTick % 5 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distance(center);

                        // Platform edge halo
                        if (dist >= 3.0 && dist <= 4.0) {
                            p.damage(10.0); // 5 hearts/sec
                        }

                        // Corner lava arcs (approximate)
                        Location pl = p.getLocation();
                        if ((Math.abs(pl.getX() - center.getX()) > 2) &&
                                (Math.abs(pl.getZ() - center.getZ()) > 2) && dist < 5) {
                            p.damage(24.0); // 12 hearts corner arc
                        }

                        // Off-platform void fall
                        if (dist > 4.5) {
                            p.teleport(center.clone().add(0, 1, 0));
                            p.damage(12.0); // 6 hearts void penalty
                        }
                    }
                }

                return;
            }

            // ============================================
            // PHASE 5: RESOLUTION (360-420 ticks / 18-21 seconds)
            // ============================================
            // If Dweller is alive: island stabilizes at 6x6, fight continues
            // If Dweller is dead: cinematic collapse sequence (handled by mode manager)
            // Either way: sounds fade, particles reduce

            int resolveTick = ticksAlive - 360;
            float fade = Math.min(1.0f, resolveTick / 60.0f);

            if (resolveTick % 4 == 0) {
                int particleCount = Math.max(1, (int) (10 * (1 - fade)));
                for (int i = 0; i < particleCount; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    Location loc = center.clone().add(Math.cos(angle) * 2, Math.random() * 5, Math.sin(angle) * 2);
                    DisplayBuilder.dustParticles(loc, 2, 0.3, 0, (int) (255 * (1 - fade)), (int) (200 * (1 - fade)), 0.6f);
                }
            }

            // Sounds fade
            if (resolveTick == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheEndOfTheIsland(plugin); }
    }
}
