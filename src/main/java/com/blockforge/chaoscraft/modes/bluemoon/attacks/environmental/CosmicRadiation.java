package com.blockforge.chaoscraft.modes.bluemoon.attacks.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import java.util.*;

/**
 * Blue Moon Mode — COSMIC RADIATION
 * 13 radiation/cosmic energy environmental attacks.
 * Palette: cyan-green (80,255,150), toxic green (100,255,80), radiation yellow (255,255,100)
 */
public final class CosmicRadiation {

    private CosmicRadiation() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new RadiationZone(plugin));
        registry.register(new SolarFlare(plugin));
        registry.register(new GammaBurst(plugin));
        registry.register(new RadiationCloud(plugin));
        registry.register(new ChargedParticles(plugin));
        registry.register(new CosmicDrain(plugin));
        registry.register(new IonStorm(plugin));
        registry.register(new MagneticField(plugin));
        registry.register(new UVBurst(plugin));
        registry.register(new ParticleAccelerator(plugin));
        registry.register(new DecayAura(plugin));
        registry.register(new NuclearWinter(plugin));
        registry.register(new EntropyField(plugin));
    }

    // ================================================================
    // 1. RADIATION ZONE — Cyan-green dust circle, damage/tick, lingers
    // ================================================================
    public static class RadiationZone extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public RadiationZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("radiation_zone", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Initial radiation burst
            for (int i = 0; i < 20; i++) {
                double angle = RNG.nextDouble() * 2 * Math.PI;
                double dist = RNG.nextDouble() * 6;
                Location pLoc = center.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);
                DisplayBuilder.dustParticles(pLoc, 3, 0.5, 80, 255, 150, 1.2f);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Radiation particles in zone
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * 6;
                    double ox = Math.cos(angle) * dist;
                    double oz = Math.sin(angle) * dist;
                    double y = RNG.nextDouble() * 2;
                    Location pLoc = center.clone().add(ox, y, oz);
                    if (RNG.nextBoolean()) {
                        DisplayBuilder.dustParticles(pLoc, 2, 0.4, 80, 255, 150, 1.0f);
                    } else {
                        DisplayBuilder.dustParticles(pLoc, 2, 0.4, 100, 255, 80, 1.0f);
                    }
                }
            }

            // Boundary ring
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), 6.0, Particle.DUST,
                        16, new Particle.DustOptions(Color.fromRGB(80, 255, 150), 1.0f));
            }

            // Geiger counter click sound
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.4f, 2.0f);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new RadiationZone(plugin);
        }
    }

    // ================================================================
    // 2. SOLAR FLARE — Bright particle burst from one direction, cone damage
    // ================================================================
    public static class SolarFlare extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double flareDirX, flareDirZ;
        private boolean flared = false;

        public SolarFlare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("solar_flare", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(8.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = RNG.nextDouble() * 2 * Math.PI;
            flareDirX = Math.cos(angle);
            flareDirZ = Math.sin(angle);
            // Warning glow from direction
            for (int i = 0; i < 8; i++) {
                Location origin = center.clone().add(-flareDirX * 12, 5 + RNG.nextDouble() * 3, -flareDirZ * 12);
                DisplayBuilder.dustParticles(origin, 4, 1.0, 255, 255, 100, 2.0f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Warning buildup (0-10 ticks)
            if (ticksAlive < 10 && ticksAlive % 2 == 0) {
                for (int i = 0; i < 5; i++) {
                    double d = RNG.nextDouble() * 8;
                    double spread = (RNG.nextDouble() - 0.5) * 4;
                    double perpX = -flareDirZ * spread;
                    double perpZ = flareDirX * spread;
                    Location loc = center.clone().add(
                            -flareDirX * (12 - d) + perpX, 4 + RNG.nextDouble() * 2, -flareDirZ * (12 - d) + perpZ);
                    DisplayBuilder.dustParticles(loc, 2, 0.5, 255, 200, 100, 1.5f);
                }
            }

            // Flare at tick 10
            if (!flared && ticksAlive >= 10) {
                flared = true;

                // Cone of bright particles
                for (int d = 0; d <= 12; d++) {
                    double coneWidth = d * 0.5;
                    for (int i = 0; i < 5; i++) {
                        double spread = (RNG.nextDouble() - 0.5) * coneWidth * 2;
                        double perpX = -flareDirZ * spread;
                        double perpZ = flareDirX * spread;
                        double y = 0.5 + RNG.nextDouble() * 3;
                        Location loc = center.clone().add(flareDirX * d + perpX, y, flareDirZ * d + perpZ);
                        DisplayBuilder.dustParticles(loc, 2, 0.3, 255, 255, 100, 2.0f);
                        w.spawnParticle(Particle.FLAME, loc, 1, 0.2, 0.2, 0.2, 0.02);
                    }
                }

                // Cone damage: players in front of the flare direction
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = ploc.getX() - center.getX();
                    double dz = ploc.getZ() - center.getZ();
                    double projection = dx * flareDirX + dz * flareDirZ;
                    if (projection > 0 && projection <= 12) {
                        double perpDist = Math.abs(dx * (-flareDirZ) + dz * flareDirX);
                        double coneWidthAtDist = projection * 0.5;
                        if (perpDist <= coneWidthAtDist) {
                            player.damage(config.getImpactDamage());
                            player.setNoDamageTicks(0);
                            player.setVelocity(player.getVelocity().add(
                                    new Vector(flareDirX * 0.4, 0.15, flareDirZ * 0.4)));
                        }
                    }
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 1.0f);
            }

            // Lingering heat haze
            if (flared && ticksAlive % 4 == 0) {
                double d = RNG.nextDouble() * 10;
                Location loc = center.clone().add(flareDirX * d, 0.3, flareDirZ * d);
                DisplayBuilder.dustParticles(loc, 2, 0.5, 255, 200, 100, 1.0f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SolarFlare(plugin);
        }
    }

    // ================================================================
    // 3. GAMMA BURST — Narrow intense beam, sweeps. Very high damage tiny radius
    // ================================================================
    public static class GammaBurst extends EnvironmentalAttack {
        private double sweepAngle = 0;
        private final double sweepSpeed = Math.PI / 60.0;

        public GammaBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gamma_burst", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            sweepAngle = 0;
            DisplayBuilder.dustParticles(center.clone().add(0, 20, 0), 15, 2.0, 100, 255, 80, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            sweepAngle += sweepSpeed;
            double beamX = Math.cos(sweepAngle) * 8;
            double beamZ = Math.sin(sweepAngle) * 8;
            Location beamGround = center.clone().add(beamX, 0, beamZ);

            // Draw intense beam from sky
            for (int y = 0; y <= 20; y++) {
                Location beamPoint = beamGround.clone().add(0, y, 0);
                DisplayBuilder.dustParticles(beamPoint, 2, 0.1, 100, 255, 80, 2.5f);
                if (y % 2 == 0) {
                    DisplayBuilder.dustParticles(beamPoint, 1, 0.05, 255, 255, 100, 1.5f);
                }
            }

            // Ground impact glow
            DisplayBuilder.dustParticles(beamGround.clone().add(0, 0.3, 0), 6, 0.8, 80, 255, 150, 1.5f);

            // Damage at beam position
            for (Player player : w.getPlayers()) {
                if (isExempt(player)) continue;
                if (player.getLocation().distanceSquared(beamGround) <= 4) {
                    player.damage(config.getDamage());
                    player.setNoDamageTicks(0);
                }
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.playSound(beamGround, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 2.0f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new GammaBurst(plugin);
        }
    }

    // ================================================================
    // 4. RADIATION CLOUD — Drifting particle cloud, damage inside, moves
    // ================================================================
    public static class RadiationCloud extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double cloudX, cloudZ;
        private double driftX, driftZ;

        public RadiationCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("radiation_cloud", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            cloudX = 0;
            cloudZ = 0;
            double angle = RNG.nextDouble() * 2 * Math.PI;
            driftX = Math.cos(angle) * 0.15;
            driftZ = Math.sin(angle) * 0.15;
            DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 15, 2.0, 80, 255, 150, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEEHIVE_DRIP, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Drift cloud
            cloudX += driftX;
            cloudZ += driftZ;
            Location cloudCenter = center.clone().add(cloudX, 0, cloudZ);

            // Cloud particles
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 10;
                    double oz = (RNG.nextDouble() - 0.5) * 10;
                    double y = 1 + RNG.nextDouble() * 4;
                    Location pLoc = cloudCenter.clone().add(ox, y, oz);
                    if (RNG.nextInt(3) == 0) {
                        DisplayBuilder.dustParticles(pLoc, 2, 0.5, 100, 255, 80, 1.5f);
                    } else {
                        DisplayBuilder.dustParticles(pLoc, 2, 0.5, 80, 255, 150, 1.2f);
                    }
                }
            }

            // Dripping particles
            if (ticksAlive % 6 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 8;
                double oz = (RNG.nextDouble() - 0.5) * 8;
                w.spawnParticle(Particle.DRIPPING_WATER, cloudCenter.clone().add(ox, 2, oz),
                        2, 0.1, 0.1, 0.1, 0);
            }

            // Damage players inside cloud
            if (ticksAlive % 12 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(cloudCenter) <= 25) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(cloudCenter, Sound.BLOCK_BEEHIVE_DRIP, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new RadiationCloud(plugin);
        }
    }

    // ================================================================
    // 5. CHARGED PARTICLES — Players gain particle aura, after 80 ticks burst
    // ================================================================
    public static class ChargedParticles extends EnvironmentalAttack {
        private final Set<UUID> chargedPlayers = new HashSet<>();
        private boolean detonated = false;

        public ChargedParticles(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("charged_particles", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(700);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Charge all nearby players
            for (Player player : w.getPlayers()) {
                if (isExempt(player)) continue;
                if (player.getLocation().distanceSquared(center) <= 100) {
                    chargedPlayers.add(player.getUniqueId());
                }
            }
            DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 20, 5.0, 100, 255, 80, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Show charge aura on affected players
            if (ticksAlive % 3 == 0) {
                for (Player player : w.getPlayers()) {
                    if (!chargedPlayers.contains(player.getUniqueId())) continue;
                    if (!player.isOnline()) continue;
                    Location ploc = player.getLocation();

                    // Intensity grows as detonation approaches
                    int count = 2 + (ticksAlive / 20);
                    double spread = 0.5 + (ticksAlive / 80.0) * 0.5;
                    DisplayBuilder.dustParticles(ploc.clone().add(0, 1, 0), count, spread, 100, 255, 80, 1.2f);

                    // Electric sparks
                    if (ticksAlive > 40) {
                        w.spawnParticle(Particle.END_ROD, ploc.clone().add(0, 1.5, 0),
                                1, 0.3, 0.3, 0.3, 0.02);
                    }
                }
            }

            // Warning sound escalation
            if (ticksAlive % 10 == 0 && ticksAlive > 40) {
                DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.0f + ticksAlive * 0.01f);
            }

            // Detonation at tick 75
            if (!detonated && ticksAlive >= 75) {
                detonated = true;
                for (Player player : w.getPlayers()) {
                    if (!chargedPlayers.contains(player.getUniqueId())) continue;
                    if (!player.isOnline()) continue;
                    Location ploc = player.getLocation();

                    // Burst damage to self and nearby
                    triggerImpactDamage(ploc);
                    DisplayBuilder.dustParticles(ploc, 20, 2.5, 80, 255, 150, 2.0f);
                    DisplayBuilder.dustParticles(ploc, 10, 1.5, 255, 255, 100, 1.5f);
                    w.spawnParticle(Particle.END_ROD, ploc.clone().add(0, 1, 0), 15, 2.0, 2.0, 2.0, 0.1);
                    DisplayBuilder.playSound(ploc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() {
            chargedPlayers.clear();
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ChargedParticles(plugin);
        }
    }

    // ================================================================
    // 6. COSMIC DRAIN — Players in zone lose HP slowly
    // ================================================================
    public static class CosmicDrain extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public CosmicDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_drain", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), 6.0, Particle.DUST,
                    16, new Particle.DustOptions(Color.fromRGB(100, 80, 255), 1.2f));
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Drain vortex particles spiraling inward
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = ticksAlive * 0.15 + i * (Math.PI / 3);
                    double radius = 6.0 * (1.0 - (i / 12.0));
                    Location pLoc = center.clone().add(
                            Math.cos(angle) * radius, 0.3 + i * 0.2, Math.sin(angle) * radius);
                    DisplayBuilder.dustParticles(pLoc, 1, 0.2, 100, 80, 255, 1.2f);
                }
            }

            // Energy wisps being drained from players
            if (ticksAlive % 6 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 36) {
                        Location ploc = player.getLocation().clone().add(0, 1, 0);
                        Vector toCenter = center.toVector().subtract(ploc.toVector()).normalize();
                        Location wispLoc = ploc.clone().add(toCenter.getX() * 0.5, toCenter.getY() * 0.5, toCenter.getZ() * 0.5);
                        DisplayBuilder.dustParticles(wispLoc, 2, 0.3, 150, 100, 255, 1.0f);
                    }
                }
            }

            // Center absorption glow
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 3, 0.5, 80, 255, 150, 1.5f);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new CosmicDrain(plugin);
        }
    }

    // ================================================================
    // 7. ION STORM — Lightning-like particle effects, random strikes
    // ================================================================
    public static class IonStorm extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public IonStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ion_storm", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Storm clouds overhead
            for (int i = 0; i < 15; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                DisplayBuilder.dustParticles(center.clone().add(ox, 15, oz), 3, 1.0, 100, 200, 255, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Random lightning strike every 8 ticks
            if (ticksAlive % 8 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 18;
                double oz = (RNG.nextDouble() - 0.5) * 18;
                Location strikeLoc = center.clone().add(ox, 0, oz);

                // Lightning bolt particles (jagged line from sky)
                Location skyStart = strikeLoc.clone().add(0, 15, 0);
                double currentX = skyStart.getX();
                double currentZ = skyStart.getZ();
                for (int y = 15; y >= 0; y--) {
                    currentX += (RNG.nextDouble() - 0.5) * 0.8;
                    currentZ += (RNG.nextDouble() - 0.5) * 0.8;
                    Location boltPoint = new Location(w, currentX, strikeLoc.getY() + y, currentZ);
                    DisplayBuilder.dustParticles(boltPoint, 2, 0.1, 150, 200, 255, 1.5f);
                }

                // Ground impact
                DisplayBuilder.dustParticles(strikeLoc, 8, 1.0, 100, 255, 255, 1.5f);

                // Strike damage
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(strikeLoc) <= 9) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }

                DisplayBuilder.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.5f);
            }

            // Storm ambient
            if (ticksAlive % 3 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                DisplayBuilder.dustParticles(center.clone().add(ox, 14 + RNG.nextDouble() * 2, oz),
                        2, 0.6, 80, 80, 120, 1.2f);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new IonStorm(plugin);
        }
    }

    // ================================================================
    // 8. MAGNETIC FIELD — Players with iron armor take more damage
    // ================================================================
    public static class MagneticField extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public MagneticField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magnetic_field", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Magnetic field visualization
            for (int ring = 0; ring < 3; ring++) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.5 + ring, 0),
                        10.0 - ring * 2, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(100, 100, 255), 1.2f));
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Magnetic field lines
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = ticksAlive * 0.05 + i * (Math.PI / 2);
                    for (double r = 2; r <= 10; r += 2) {
                        double y = Math.sin((r / 10.0) * Math.PI) * 3;
                        Location loc = center.clone().add(
                                Math.cos(angle) * r, y + 0.5, Math.sin(angle) * r);
                        DisplayBuilder.dustParticles(loc, 1, 0.2, 100, 100, 255, 1.0f);
                    }
                }
            }

            // Check and damage players, extra for iron armor
            if (ticksAlive % 15 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) > 100) continue;

                    double damage = config.getDamage();

                    // Check for iron armor pieces
                    int ironPieces = 0;
                    for (ItemStack armor : player.getInventory().getArmorContents()) {
                        if (armor != null && armor.getType().name().contains("IRON")) {
                            ironPieces++;
                        }
                    }
                    // 25% more damage per iron piece
                    damage += ironPieces * (config.getDamage() * 0.25);

                    player.damage(damage);
                    player.setNoDamageTicks(0);

                    // Visual feedback for iron wearers
                    if (ironPieces > 0) {
                        DisplayBuilder.dustParticles(player.getLocation().clone().add(0, 1, 0),
                                5, 0.5, 255, 100, 100, 1.5f);
                        DisplayBuilder.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.3f, 2.0f);
                    }
                }
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new MagneticField(plugin);
        }
    }

    // ================================================================
    // 9. UV BURST — Bright white flash, instant damage burst
    // ================================================================
    public static class UVBurst extends EnvironmentalAttack {
        private boolean bursted = false;

        public UVBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("uv_burst", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(8.0);
            config.setDurationTicks(10);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Brief warning flash
            DisplayBuilder.dustParticles(center.clone().add(0, 5, 0), 10, 3.0, 255, 255, 200, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Flash at tick 5
            if (!bursted && ticksAlive >= 5) {
                bursted = true;
                triggerImpactDamage(center);

                // Massive white flash
                DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 40, 5.0, 255, 255, 255, 3.0f);
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 3, 0), 30, 4.0, 3.0, 4.0, 0.15);
                w.spawnParticle(Particle.FLASH, center.clone().add(0, 3, 0), 1, 0, 0, 0, 0);

                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 2.0f);
            }

            // Rapid fadeout
            if (bursted && ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0),
                        10 - ticksAlive, 3.0, 255, 255, 200, 2.0f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new UVBurst(plugin);
        }
    }

    // ================================================================
    // 10. PARTICLE ACCELERATOR — Spinning ring speeds up, damage increases
    // ================================================================
    public static class ParticleAccelerator extends EnvironmentalAttack {
        private double rotationAngle = 0;
        private double currentDamage = 4.0;

        public ParticleAccelerator(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("particle_accelerator", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            rotationAngle = 0;
            currentDamage = 4.0;
            DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), 6.0, Particle.DUST,
                    16, new Particle.DustOptions(Color.fromRGB(100, 255, 255), 1.0f));
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Speed increases over time
            double speedMultiplier = 1.0 + (ticksAlive / 120.0) * 3.0;
            rotationAngle += 0.1 * speedMultiplier;

            // Damage scales from 4 to 10
            currentDamage = 4.0 + (ticksAlive / 120.0) * 6.0;
            config.setDamage(currentDamage);

            // Draw spinning ring
            if (ticksAlive % 2 == 0) {
                int points = 16;
                for (int i = 0; i < points; i++) {
                    double angle = rotationAngle + (2 * Math.PI * i) / points;
                    double ox = Math.cos(angle) * 6;
                    double oz = Math.sin(angle) * 6;
                    Location pLoc = center.clone().add(ox, 0.3, oz);

                    // Color shifts from blue to intense green as speed increases
                    int r = (int) (100 - (ticksAlive / 120.0) * 80);
                    int g = (int) (200 + (ticksAlive / 120.0) * 55);
                    int b = (int) (255 - (ticksAlive / 120.0) * 155);
                    DisplayBuilder.dustParticles(pLoc, 1, 0.2, Math.max(0, r), Math.min(255, g), Math.max(0, b), 1.5f);
                }

                // Particle "orbs" on the ring
                for (int orb = 0; orb < 3; orb++) {
                    double orbAngle = rotationAngle + orb * (2 * Math.PI / 3);
                    Location orbLoc = center.clone().add(Math.cos(orbAngle) * 6, 0.5, Math.sin(orbAngle) * 6);
                    w.spawnParticle(Particle.END_ROD, orbLoc, 2, 0.1, 0.1, 0.1, 0.02);
                }
            }

            // Sound pitch increases
            if (ticksAlive % 10 == 0) {
                float pitch = 0.5f + (ticksAlive / 120.0f) * 1.5f;
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.4f, Math.min(2.0f, pitch));
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ParticleAccelerator(plugin);
        }
    }

    // ================================================================
    // 11. DECAY AURA — Follows target player for 60 ticks, must outrun
    // ================================================================
    public static class DecayAura extends EnvironmentalAttack {
        private Location auraCenter;
        private static final Random RNG = new Random();

        public DecayAura(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("decay_aura", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            auraCenter = center.clone();
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 10, 2.0, 100, 255, 80, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            if (auraCenter == null || auraCenter.getWorld() == null) return;
            World w = auraCenter.getWorld();

            Player target = getTargetPlayer();
            if (target != null && target.isOnline() && ticksAlive < 60) {
                // Chase player slowly
                Location targetLoc = target.getLocation();
                Vector dir = targetLoc.toVector().subtract(auraCenter.toVector());
                if (dir.lengthSquared() > 1) {
                    dir.normalize().multiply(0.2); // Slow chase speed
                    auraCenter.add(dir);
                }
            }
            // After 60 ticks, stops chasing but lingers

            // Decay aura particles
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * 4;
                    double y = RNG.nextDouble() * 3;
                    Location pLoc = auraCenter.clone().add(
                            Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.3, 100, 255, 80, 1.2f);
                }
            }

            // Decay wisps
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.SMOKE, auraCenter.clone().add(0, 1, 0), 3, 1.0, 0.5, 1.0, 0.01);
            }

            // Damage players in aura
            if (ticksAlive % 10 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(auraCenter) <= 16) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(auraCenter, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new DecayAura(plugin);
        }
    }

    // ================================================================
    // 12. NUCLEAR WINTER — Frost + radiation combo, wide frost particles + damage
    // ================================================================
    public static class NuclearWinter extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public NuclearWinter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nuclear_winter", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Initial frost-radiation burst
            for (int i = 0; i < 25; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 24;
                double oz = (RNG.nextDouble() - 0.5) * 24;
                double y = RNG.nextDouble() * 8;
                if (RNG.nextBoolean()) {
                    DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 3, 0.8, 200, 230, 255, 1.5f);
                } else {
                    DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 3, 0.8, 100, 255, 150, 1.2f);
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Frost particles falling
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 24;
                    double oz = (RNG.nextDouble() - 0.5) * 24;
                    double y = 8 + RNG.nextDouble() * 5;
                    Location pLoc = center.clone().add(ox, y, oz);
                    // Mix of frost and radiation colors
                    if (RNG.nextInt(3) == 0) {
                        DisplayBuilder.dustParticles(pLoc, 1, 0.4, 100, 255, 150, 1.0f);
                    } else {
                        DisplayBuilder.dustParticles(pLoc, 1, 0.4, 200, 230, 255, 1.2f);
                    }
                }
            }

            // Ground frost accumulation
            if (ticksAlive % 4 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 22;
                double oz = (RNG.nextDouble() - 0.5) * 22;
                DisplayBuilder.dustParticles(center.clone().add(ox, 0.1, oz), 3, 0.4, 220, 240, 255, 1.0f);
            }

            // Radiation glow spots on ground
            if (ticksAlive % 8 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                DisplayBuilder.dustParticles(center.clone().add(ox, 0.2, oz), 3, 0.5, 80, 255, 150, 1.5f);
            }

            // Wind push effect
            if (ticksAlive % 6 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 144) {
                        player.setVelocity(player.getVelocity().add(
                                new Vector((RNG.nextDouble() - 0.5) * 0.05, 0, (RNG.nextDouble() - 0.5) * 0.05)));
                    }
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new NuclearWinter(plugin);
        }
    }

    // ================================================================
    // 13. ENTROPY FIELD — Subtle particles marking area where attacks deal double
    // ================================================================
    public static class EntropyField extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public EntropyField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("entropy_field", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Subtle field boundary
            DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), 8.0, Particle.DUST,
                    20, new Particle.DustOptions(Color.fromRGB(255, 100, 100), 0.8f));
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Subtle entropy particles
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * 8;
                    double ox = Math.cos(angle) * dist;
                    double oz = Math.sin(angle) * dist;
                    // Dim red-orange particles indicating danger zone
                    DisplayBuilder.dustParticles(center.clone().add(ox, 0.2 + RNG.nextDouble(), oz),
                            1, 0.3, 255, 100, 80, 0.8f);
                }
            }

            // Boundary shimmer
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = (i / 6.0) * 2 * Math.PI + ticksAlive * 0.03;
                    Location edge = center.clone().add(Math.cos(angle) * 8, 0.3, Math.sin(angle) * 8);
                    DisplayBuilder.dustParticles(edge, 1, 0.2, 255, 150, 100, 0.8f);
                }
            }

            // Entropy effect: players in zone take more base damage (meta-effect)
            // The base 2.0 damage represents the entropy field's own tick
            // Additionally deal bonus damage to already-damaged players
            if (ticksAlive % 20 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 64) {
                        double baseDamage = config.getDamage();
                        // If player is below half health, entropy is more potent
                        double healthRatio = player.getHealth() / player.getMaxHealth();
                        if (healthRatio < 0.5) {
                            baseDamage *= 2.0;
                            DisplayBuilder.dustParticles(player.getLocation().clone().add(0, 1, 0),
                                    5, 0.5, 255, 50, 50, 1.5f);
                        }
                        player.damage(baseDamage);
                        player.setNoDamageTicks(0);
                    }
                }
            }

            // Distortion effect visual
            if (ticksAlive % 10 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 14;
                double oz = (RNG.nextDouble() - 0.5) * 14;
                w.spawnParticle(Particle.END_ROD, center.clone().add(ox, 0.5, oz), 1, 0.1, 0.2, 0.1, 0.01);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new EntropyField(plugin);
        }
    }
}
