package com.blockforge.chaoscraft.modes.bluemoon.attacks.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import java.util.*;

/**
 * Blue Moon Mode — LUNAR QUAKES
 * 13 ground/quake environmental attacks.
 * Palette: pale blue (180,210,255), silver (200,200,220), frost cyan (150,230,255)
 */
public final class LunarQuakes {

    private LunarQuakes() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new Tremor(plugin));
        registry.register(new FissureLine(plugin));
        registry.register(new Upheaval(plugin));
        registry.register(new Sinkhole(plugin));
        registry.register(new Shockwave(plugin));
        registry.register(new Aftershock(plugin));
        registry.register(new FaultSplit(plugin));
        registry.register(new SeismicSlam(plugin));
        registry.register(new RollingQuake(plugin));
        registry.register(new TectonicShift(plugin));
        registry.register(new CraterFormation(plugin));
        registry.register(new GroundPound(plugin));
        registry.register(new ResonanceCascade(plugin));
    }

    // ================================================================
    // 1. TREMOR — Screen shake via tiny random knockback velocity
    // ================================================================
    public static class Tremor extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public Tremor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tremor", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.3f);
            // Ground crack particles
            for (int i = 0; i < 20; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 30;
                double oz = (RNG.nextDouble() - 0.5) * 30;
                DisplayBuilder.dustParticles(center.clone().add(ox, 0.1, oz), 3, 0.3, 150, 150, 160, 1.2f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Apply tiny random knockback for screen-shake effect
            for (Player player : w.getPlayers()) {
                if (isExempt(player)) continue;
                if (player.getLocation().distanceSquared(center) <= 225) {
                    double shakeX = (RNG.nextDouble() - 0.5) * 0.12;
                    double shakeY = RNG.nextDouble() * 0.04;
                    double shakeZ = (RNG.nextDouble() - 0.5) * 0.12;
                    player.setVelocity(player.getVelocity().add(new Vector(shakeX, shakeY, shakeZ)));
                }
            }

            // Ground dust particles
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 28;
                    double oz = (RNG.nextDouble() - 0.5) * 28;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 0.2, oz), 2, 0.4, 180, 180, 190, 1.0f);
                }
            }

            if (ticksAlive % 5 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.8f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new Tremor(plugin);
        }
    }

    // ================================================================
    // 2. FISSURE LINE — Crack particles along ground line, damage on crack
    // ================================================================
    public static class FissureLine extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double fissureDirX, fissureDirZ;
        private double fissureProgress = 0;

        public FissureLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fissure_line", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = RNG.nextDouble() * Math.PI;
            fissureDirX = Math.cos(angle);
            fissureDirZ = Math.sin(angle);
            fissureProgress = 0;
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Advance fissure
            fissureProgress = Math.min(1.0, ticksAlive / 30.0);
            double length = fissureProgress * 16;

            // Draw crack line from -length/2 to +length/2
            if (ticksAlive % 2 == 0) {
                for (double d = -length / 2; d <= length / 2; d += 0.5) {
                    double ox = fissureDirX * d;
                    double oz = fissureDirZ * d;
                    Location crackLoc = center.clone().add(ox, 0.1, oz);
                    DisplayBuilder.dustParticles(crackLoc, 2, 0.2, 100, 100, 120, 1.2f);
                    // Upward debris
                    if (RNG.nextFloat() < 0.1) {
                        w.spawnParticle(Particle.BLOCK, crackLoc.clone().add(0, 0.5, 0),
                                3, 0.2, 0.3, 0.2, 0.05, Material.STONE.createBlockData());
                    }
                }
            }

            // Damage players on the fissure line
            if (ticksAlive % 10 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    // Project player position onto fissure line, check distance
                    double dx = ploc.getX() - center.getX();
                    double dz = ploc.getZ() - center.getZ();
                    double projection = dx * fissureDirX + dz * fissureDirZ;
                    if (Math.abs(projection) <= length / 2) {
                        double perpDist = Math.abs(dx * fissureDirZ - dz * fissureDirX);
                        if (perpDist <= 2.0) {
                            player.damage(config.getDamage());
                            player.setNoDamageTicks(0);
                        }
                    }
                }
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new FissureLine(plugin);
        }
    }

    // ================================================================
    // 3. UPHEAVAL — Upward velocity for players, fall damage after
    // ================================================================
    public static class Upheaval extends EnvironmentalAttack {
        private boolean launched = false;

        public Upheaval(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("upheaval", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning rumble
            DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 20, 3.0, 180, 180, 190, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            if (!launched && ticksAlive >= 10) {
                launched = true;
                // Launch all players in radius upward
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 36) {
                        player.setVelocity(player.getVelocity().add(new Vector(0, 1.2, 0)));
                    }
                }
                // Eruption particles
                for (int i = 0; i < 25; i++) {
                    double ox = (new Random().nextDouble() - 0.5) * 10;
                    double oz = (new Random().nextDouble() - 0.5) * 10;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 0.5, oz), 3, 0.5, 200, 200, 220, 1.5f);
                    w.spawnParticle(Particle.BLOCK, center.clone().add(ox, 1, oz),
                            5, 0.5, 1.0, 0.5, 0.2, Material.STONE.createBlockData());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
            }

            // Warning phase particles
            if (!launched && ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.2, 0), 5, 2.5, 150, 150, 160, 1.0f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new Upheaval(plugin);
        }
    }

    // ================================================================
    // 4. SINKHOLE — Pull downward velocity, damage at center
    // ================================================================
    public static class Sinkhole extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public Sinkhole(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sinkhole", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning: dark circle on ground
            DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 5.0, Particle.DUST,
                    20, new Particle.DustOptions(Color.fromRGB(80, 80, 100), 1.5f));
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Sucking spiral particles
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = ticksAlive * 0.3 + i * (Math.PI / 3);
                    double shrinkRadius = 5.0 * (1.0 - (double) ticksAlive / config.getDurationTicks());
                    double ox = Math.cos(angle) * shrinkRadius;
                    double oz = Math.sin(angle) * shrinkRadius;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 0.2, oz), 2, 0.3, 100, 100, 120, 1.2f);
                }
            }

            // Pull players toward center
            if (ticksAlive % 3 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    if (ploc.distanceSquared(center) <= 25) {
                        Vector pull = center.toVector().subtract(ploc.toVector()).normalize().multiply(0.2);
                        pull.setY(-0.05);
                        player.setVelocity(player.getVelocity().add(pull));
                    }
                }
            }

            // Center dark vortex
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 5, 1.0, 60, 60, 80, 1.5f);
                w.spawnParticle(Particle.BLOCK, center.clone().add(0, 0.3, 0),
                        3, 0.5, 0.2, 0.5, 0.02, Material.STONE.createBlockData());
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new Sinkhole(plugin);
        }
    }

    // ================================================================
    // 5. SHOCKWAVE — Expanding particle ring from center, must jump over
    // ================================================================
    public static class Shockwave extends EnvironmentalAttack {
        private double waveRadius = 0;

        public Shockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shockwave", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            waveRadius = 0;
            // Central impact
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 15, 1.0, 200, 200, 220, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Expand ring
            waveRadius = (ticksAlive / 30.0) * 14.0;

            // Draw ring
            if (ticksAlive % 2 == 0) {
                int points = Math.max(16, (int) (waveRadius * 4));
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    double ox = Math.cos(angle) * waveRadius;
                    double oz = Math.sin(angle) * waveRadius;
                    Location pLoc = center.clone().add(ox, 0.3, oz);
                    DisplayBuilder.dustParticles(pLoc, 1, 0.2, 180, 210, 255, 1.5f);
                }
                // Inner edge dust
                for (int i = 0; i < points / 2; i++) {
                    double angle = (2 * Math.PI * i) / (points / 2);
                    double ox = Math.cos(angle) * (waveRadius - 0.5);
                    double oz = Math.sin(angle) * (waveRadius - 0.5);
                    w.spawnParticle(Particle.BLOCK, center.clone().add(ox, 0.2, oz),
                            1, 0.1, 0.1, 0.1, 0.01, Material.STONE.createBlockData());
                }
            }

            // Damage players on the ring (must jump over it)
            for (Player player : w.getPlayers()) {
                if (isExempt(player)) continue;
                Location ploc = player.getLocation();
                double dist = Math.sqrt(ploc.distanceSquared(center));
                double ringDiff = Math.abs(dist - waveRadius);
                // Only damage if on the ring (within 1.5 blocks) and on the ground
                if (ringDiff <= 1.5 && ploc.getY() - center.getY() < 1.0) {
                    player.damage(config.getDamage());
                    player.setNoDamageTicks(0);
                }
            }

            if (ticksAlive % 6 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new Shockwave(plugin);
        }
    }

    // ================================================================
    // 6. AFTERSHOCK — Follows previous location with delay, 50% more damage
    // ================================================================
    public static class Aftershock extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private Location delayedCenter;

        public Aftershock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("aftershock", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Offset from center to simulate "previous attack location"
            double ox = (RNG.nextDouble() - 0.5) * 8;
            double oz = (RNG.nextDouble() - 0.5) * 8;
            delayedCenter = center.clone().add(ox, 0, oz);

            // Warning cracks at delayed location
            DisplayBuilder.dustParticles(delayedCenter.clone().add(0, 0.1, 0), 10, 2.5, 255, 150, 100, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            if (delayedCenter == null || delayedCenter.getWorld() == null) return;
            World w = delayedCenter.getWorld();

            // Warning phase (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.dustParticles(delayedCenter.clone().add(0, 0.2, 0), 5, 2.0, 255, 180, 100, 1.2f);
                    DisplayBuilder.particleRing(delayedCenter.clone().add(0, 0.1, 0), 3.0, Particle.DUST,
                            12, new Particle.DustOptions(Color.fromRGB(255, 150, 100), 1.2f));
                }
                return;
            }

            // Aftershock hit at tick 10
            if (ticksAlive == 10) {
                // Massive ground burst
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(delayedCenter) <= 36) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                        player.setVelocity(player.getVelocity().add(new Vector(
                                (RNG.nextDouble() - 0.5) * 0.3, 0.4, (RNG.nextDouble() - 0.5) * 0.3)));
                    }
                }
                DisplayBuilder.dustParticles(delayedCenter, 25, 3.0, 255, 200, 150, 2.0f);
                w.spawnParticle(Particle.BLOCK, delayedCenter.clone().add(0, 0.5, 0),
                        20, 2.0, 1.0, 2.0, 0.1, Material.STONE.createBlockData());
                DisplayBuilder.playSound(delayedCenter, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.4f);
            }

            // Post-shock dust
            if (ticksAlive > 10 && ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(delayedCenter.clone().add(0, 0.3, 0), 3, 2.0, 180, 180, 190, 1.0f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new Aftershock(plugin);
        }
    }

    // ================================================================
    // 7. FAULT SPLIT — Two diverging crack lines, damage between them
    // ================================================================
    public static class FaultSplit extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double splitDirX, splitDirZ;
        private double splitProgress = 0;

        public FaultSplit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fault_split", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = RNG.nextDouble() * Math.PI;
            splitDirX = Math.cos(angle);
            splitDirZ = Math.sin(angle);
            splitProgress = 0;
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.2f, 0.3f);
            DisplayBuilder.dustParticles(center.clone().add(0, 0.2, 0), 10, 1.0, 150, 150, 160, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            splitProgress = Math.min(1.0, ticksAlive / 30.0);
            double length = splitProgress * 10;
            double gapWidth = 1.0 + splitProgress * 3.0;

            // Draw two diverging crack lines
            double perpX = -splitDirZ;
            double perpZ = splitDirX;

            if (ticksAlive % 2 == 0) {
                for (double d = -length; d <= length; d += 0.5) {
                    // Line 1 (offset +)
                    Location line1 = center.clone().add(
                            splitDirX * d + perpX * gapWidth, 0.1,
                            splitDirZ * d + perpZ * gapWidth);
                    DisplayBuilder.dustParticles(line1, 1, 0.2, 120, 120, 140, 1.0f);

                    // Line 2 (offset -)
                    Location line2 = center.clone().add(
                            splitDirX * d - perpX * gapWidth, 0.1,
                            splitDirZ * d - perpZ * gapWidth);
                    DisplayBuilder.dustParticles(line2, 1, 0.2, 120, 120, 140, 1.0f);
                }

                // Fill between lines with danger particles
                for (int i = 0; i < 5; i++) {
                    double d = (RNG.nextDouble() - 0.5) * length * 2;
                    double gap = (RNG.nextDouble() - 0.5) * gapWidth * 2;
                    Location danger = center.clone().add(
                            splitDirX * d + perpX * gap, 0.2,
                            splitDirZ * d + perpZ * gap);
                    DisplayBuilder.dustParticles(danger, 1, 0.3, 200, 100, 80, 1.0f);
                }
            }

            // Damage players between the lines
            if (ticksAlive % 10 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = ploc.getX() - center.getX();
                    double dz = ploc.getZ() - center.getZ();
                    double projection = dx * splitDirX + dz * splitDirZ;
                    double perpDist = Math.abs(dx * perpX + dz * perpZ);
                    if (Math.abs(projection) <= length && perpDist <= gapWidth) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new FaultSplit(plugin);
        }
    }

    // ================================================================
    // 8. SEISMIC SLAM — Single point massive impact, warning 20 ticks before
    // ================================================================
    public static class SeismicSlam extends EnvironmentalAttack {
        private boolean impacted = false;

        public SeismicSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("seismic_slam", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(800);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning circle at impact point
            DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 3.0, Particle.DUST,
                    16, new Particle.DustOptions(Color.fromRGB(255, 80, 80), 1.5f));
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Warning phase (20 ticks): pulsing danger ring
            if (ticksAlive < 20) {
                if (ticksAlive % 3 == 0) {
                    double pulseRadius = 3.0 - (ticksAlive / 20.0) * 1.5;
                    DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), pulseRadius, Particle.DUST,
                            12, new Particle.DustOptions(Color.fromRGB(255, 100, 80), 1.5f));
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f + ticksAlive * 0.02f);
                }
                return;
            }

            // Impact at tick 20
            if (!impacted) {
                impacted = true;
                triggerImpactDamage(center);

                // Massive ground eruption
                DisplayBuilder.dustParticles(center, 30, 2.5, 200, 200, 220, 2.5f);
                w.spawnParticle(Particle.BLOCK, center.clone().add(0, 1, 0),
                        30, 2.0, 2.0, 2.0, 0.2, Material.STONE.createBlockData());

                // Knock nearby players back
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    if (ploc.distanceSquared(center) <= 25) {
                        Vector knockback = ploc.toVector().subtract(center.toVector()).normalize().multiply(0.6);
                        knockback.setY(0.3);
                        player.setVelocity(player.getVelocity().add(knockback));
                    }
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
            }

            // Post-impact dust
            if (ticksAlive > 20 && ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.3, 0), 3, 1.5, 150, 150, 160, 1.0f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SeismicSlam(plugin);
        }
    }

    // ================================================================
    // 9. ROLLING QUAKE — Moving line of tremor particles, damage wave
    // ================================================================
    public static class RollingQuake extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double waveDirX, waveDirZ;
        private double wavePosition = -12;

        public RollingQuake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rolling_quake", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = RNG.nextDouble() * 2 * Math.PI;
            waveDirX = Math.cos(angle);
            waveDirZ = Math.sin(angle);
            wavePosition = -12;
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Advance wave
            wavePosition = -12 + (24.0 * ticksAlive / 60.0);
            double perpX = -waveDirZ;
            double perpZ = waveDirX;

            // Draw wave front
            if (ticksAlive % 2 == 0) {
                for (double d = -8; d <= 8; d += 0.8) {
                    Location waveLoc = center.clone().add(
                            waveDirX * wavePosition + perpX * d, 0.2,
                            waveDirZ * wavePosition + perpZ * d);
                    DisplayBuilder.dustParticles(waveLoc, 2, 0.3, 180, 180, 200, 1.2f);
                    if (RNG.nextFloat() < 0.15) {
                        w.spawnParticle(Particle.BLOCK, waveLoc.clone().add(0, 0.5, 0),
                                2, 0.2, 0.3, 0.2, 0.05, Material.STONE.createBlockData());
                    }
                }
            }

            // Damage players at the wave front
            for (Player player : w.getPlayers()) {
                if (isExempt(player)) continue;
                Location ploc = player.getLocation();
                double dx = ploc.getX() - center.getX();
                double dz = ploc.getZ() - center.getZ();
                double projection = dx * waveDirX + dz * waveDirZ;
                double perpDist = Math.abs(dx * perpX + dz * perpZ);
                if (Math.abs(projection - wavePosition) <= 2.0 && perpDist <= 8.0
                        && ploc.getY() - center.getY() < 1.0) {
                    player.damage(config.getDamage());
                    player.setNoDamageTicks(0);
                    player.setVelocity(player.getVelocity().add(
                            new Vector((RNG.nextDouble() - 0.5) * 0.15, 0.15, (RNG.nextDouble() - 0.5) * 0.15)));
                }
            }

            if (ticksAlive % 6 == 0) {
                DisplayBuilder.playSound(center.clone().add(waveDirX * wavePosition, 0, waveDirZ * wavePosition),
                        Sound.BLOCK_STONE_BREAK, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new RollingQuake(plugin);
        }
    }

    // ================================================================
    // 10. TECTONIC SHIFT — Slow push all players one direction
    // ================================================================
    public static class TectonicShift extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double pushDirX, pushDirZ;

        public TectonicShift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tectonic_shift", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = RNG.nextDouble() * 2 * Math.PI;
            pushDirX = Math.cos(angle);
            pushDirZ = Math.sin(angle);

            // Directional warning
            for (int i = 0; i < 12; i++) {
                double d = (RNG.nextDouble() - 0.5) * 30;
                double perpX = -pushDirZ * d;
                double perpZ = pushDirX * d;
                Location arrowLoc = center.clone().add(-pushDirX * 12 + perpX, 0.3, -pushDirZ * 12 + perpZ);
                DisplayBuilder.dustParticles(arrowLoc, 3, 0.4, 180, 210, 255, 1.2f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Push all players slowly in one direction
            if (ticksAlive % 4 == 0) {
                Vector push = new Vector(pushDirX * 0.15, 0, pushDirZ * 0.15);
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 400) {
                        player.setVelocity(player.getVelocity().add(push));
                    }
                }
            }

            // Directional ground particles
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double spread = (RNG.nextDouble() - 0.5) * 30;
                    double perpX = -pushDirZ * spread;
                    double perpZ = pushDirX * spread;
                    double pos = (RNG.nextDouble() - 0.5) * 30;
                    Location pLoc = center.clone().add(pushDirX * pos + perpX, 0.2, pushDirZ * pos + perpZ);
                    DisplayBuilder.dustParticles(pLoc, 1, 0.3, 150, 150, 170, 1.0f);
                }
            }

            // Rumble sound
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TectonicShift(plugin);
        }
    }

    // ================================================================
    // 11. CRATER FORMATION — Central impact + ring of secondary impacts
    // ================================================================
    public static class CraterFormation extends EnvironmentalAttack {
        private boolean centerImpacted = false;
        private boolean ringImpacted = false;

        public CraterFormation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crater_formation", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning: target circle
            DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 2.0, Particle.DUST,
                    12, new Particle.DustOptions(Color.fromRGB(255, 100, 80), 1.2f));
            DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 6.0, Particle.DUST,
                    20, new Particle.DustOptions(Color.fromRGB(200, 80, 60), 1.0f));
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Warning pulse (0-15 ticks)
            if (ticksAlive < 15 && ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0),
                        2.0 + ticksAlive * 0.1, Particle.DUST,
                        12, new Particle.DustOptions(Color.fromRGB(255, 120, 80), 1.2f));
            }

            // Central impact at tick 15
            if (!centerImpacted && ticksAlive >= 15) {
                centerImpacted = true;
                triggerImpactDamage(center);
                DisplayBuilder.dustParticles(center, 25, 2.0, 200, 200, 220, 2.0f);
                w.spawnParticle(Particle.BLOCK, center.clone().add(0, 1, 0),
                        20, 1.5, 1.5, 1.5, 0.15, Material.STONE.createBlockData());
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
            }

            // Ring of secondary impacts at tick 25
            if (!ringImpacted && ticksAlive >= 25) {
                ringImpacted = true;
                for (int i = 0; i < 8; i++) {
                    double angle = (i / 8.0) * 2 * Math.PI;
                    Location ringLoc = center.clone().add(Math.cos(angle) * 6, 0, Math.sin(angle) * 6);
                    triggerImpactDamage(ringLoc);
                    DisplayBuilder.dustParticles(ringLoc, 10, 1.5, 180, 180, 200, 1.5f);
                    w.spawnParticle(Particle.BLOCK, ringLoc.clone().add(0, 0.5, 0),
                            8, 0.8, 0.8, 0.8, 0.1, Material.STONE.createBlockData());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
            }

            // Lingering dust
            if (ticksAlive > 25 && ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.3, 0), 4, 3.0, 150, 150, 160, 1.0f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new CraterFormation(plugin);
        }
    }

    // ================================================================
    // 12. GROUND POUND — 3 sequential impacts, each bigger: 6/8/10 dmg, 4/6/8 radius
    // ================================================================
    public static class GroundPound extends EnvironmentalAttack {
        private int poundsLanded = 0;

        public GroundPound(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ground_pound", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning rumble
            DisplayBuilder.dustParticles(center.clone().add(0, 0.2, 0), 8, 1.5, 180, 180, 200, 1.2f);
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Pound 1 at tick 10 (6 dmg, 4 radius)
            if (poundsLanded == 0 && ticksAlive >= 10) {
                poundsLanded = 1;
                doPound(center, w, 6.0, 4.0, 8, 1.5);
            }
            // Pound 2 at tick 30 (8 dmg, 6 radius)
            if (poundsLanded == 1 && ticksAlive >= 30) {
                poundsLanded = 2;
                doPound(center, w, 8.0, 6.0, 15, 2.0);
            }
            // Pound 3 at tick 50 (10 dmg, 8 radius)
            if (poundsLanded == 2 && ticksAlive >= 50) {
                poundsLanded = 3;
                doPound(center, w, 10.0, 8.0, 25, 2.5);
            }

            // Warning before each pound
            if (poundsLanded == 0 && ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 4.0, Particle.DUST,
                        10, new Particle.DustOptions(Color.fromRGB(255, 150, 100), 1.0f));
            }
            if (poundsLanded == 1 && ticksAlive < 30 && ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 6.0, Particle.DUST,
                        14, new Particle.DustOptions(Color.fromRGB(255, 120, 80), 1.2f));
            }
            if (poundsLanded == 2 && ticksAlive < 50 && ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 8.0, Particle.DUST,
                        18, new Particle.DustOptions(Color.fromRGB(255, 80, 60), 1.5f));
            }
        }

        private void doPound(Location center, World w, double damage, double radius, int particleCount, double particleSpread) {
            for (Player player : w.getPlayers()) {
                if (isExempt(player)) continue;
                if (player.getLocation().distanceSquared(center) <= radius * radius) {
                    player.damage(damage);
                    player.setNoDamageTicks(0);
                    player.setVelocity(player.getVelocity().add(new Vector(0, 0.3, 0)));
                }
            }
            DisplayBuilder.dustParticles(center, particleCount, particleSpread, 200, 200, 220, 2.0f);
            w.spawnParticle(Particle.BLOCK, center.clone().add(0, 0.5, 0),
                    particleCount, particleSpread, 1.0, particleSpread, 0.1, Material.STONE.createBlockData());
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f + poundsLanded * 0.3f, 0.6f - poundsLanded * 0.1f);
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new GroundPound(plugin);
        }
    }

    // ================================================================
    // 13. RESONANCE CASCADE — Chain reaction A -> B -> C in line
    // ================================================================
    public static class ResonanceCascade extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double lineDirX, lineDirZ;
        private int pointsTriggered = 0;
        private final Location[] points = new Location[3];

        public ResonanceCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("resonance_cascade", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = RNG.nextDouble() * 2 * Math.PI;
            lineDirX = Math.cos(angle);
            lineDirZ = Math.sin(angle);

            // Three points along line
            points[0] = center.clone().add(-lineDirX * 6, 0, -lineDirZ * 6);
            points[1] = center.clone();
            points[2] = center.clone().add(lineDirX * 6, 0, lineDirZ * 6);

            // Warning markers at all three points
            for (Location point : points) {
                DisplayBuilder.particleRing(point.clone().add(0, 0.1, 0), 2.5, Particle.DUST,
                        10, new Particle.DustOptions(Color.fromRGB(180, 210, 255), 1.2f));
            }
            // Connecting line
            DisplayBuilder.particleLine(points[0].clone().add(0, 0.2, 0),
                    points[2].clone().add(0, 0.2, 0), Particle.DUST, 3,
                    new Particle.DustOptions(Color.fromRGB(150, 180, 220), 0.8f));
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Point A triggers at tick 10
            if (pointsTriggered == 0 && ticksAlive >= 10) {
                pointsTriggered = 1;
                triggerPoint(points[0], w);
                // Energy traveling to B
                DisplayBuilder.particleLine(points[0].clone().add(0, 0.5, 0),
                        points[1].clone().add(0, 0.5, 0), Particle.END_ROD, 4, null);
            }
            // Point B triggers at tick 25
            if (pointsTriggered == 1 && ticksAlive >= 25) {
                pointsTriggered = 2;
                triggerPoint(points[1], w);
                DisplayBuilder.particleLine(points[1].clone().add(0, 0.5, 0),
                        points[2].clone().add(0, 0.5, 0), Particle.END_ROD, 4, null);
            }
            // Point C triggers at tick 40
            if (pointsTriggered == 2 && ticksAlive >= 40) {
                pointsTriggered = 3;
                triggerPoint(points[2], w);
            }

            // Warning pulse at upcoming points
            if (ticksAlive % 4 == 0) {
                for (int i = pointsTriggered; i < 3; i++) {
                    DisplayBuilder.dustParticles(points[i].clone().add(0, 0.3, 0), 3, 1.0, 180, 210, 255, 1.0f);
                }
            }

            // Energy travel visual between trigger windows
            if (pointsTriggered == 1 && ticksAlive > 10 && ticksAlive < 25 && ticksAlive % 3 == 0) {
                double t = (ticksAlive - 10) / 15.0;
                Location mid = points[0].clone().add(
                        (points[1].getX() - points[0].getX()) * t, 0.5,
                        (points[1].getZ() - points[0].getZ()) * t);
                DisplayBuilder.dustParticles(mid, 4, 0.3, 200, 220, 255, 1.5f);
            }
            if (pointsTriggered == 2 && ticksAlive > 25 && ticksAlive < 40 && ticksAlive % 3 == 0) {
                double t = (ticksAlive - 25) / 15.0;
                Location mid = points[1].clone().add(
                        (points[2].getX() - points[1].getX()) * t, 0.5,
                        (points[2].getZ() - points[1].getZ()) * t);
                DisplayBuilder.dustParticles(mid, 4, 0.3, 200, 220, 255, 1.5f);
            }
        }

        private void triggerPoint(Location point, World w) {
            triggerImpactDamage(point);
            DisplayBuilder.dustParticles(point, 20, 2.0, 200, 200, 220, 2.0f);
            w.spawnParticle(Particle.BLOCK, point.clone().add(0, 0.5, 0),
                    15, 1.5, 1.0, 1.5, 0.1, Material.STONE.createBlockData());
            // Knock back players
            for (Player player : w.getPlayers()) {
                if (isExempt(player)) continue;
                if (player.getLocation().distanceSquared(point) <= 16) {
                    Vector kb = player.getLocation().toVector().subtract(point.toVector()).normalize().multiply(0.5);
                    kb.setY(0.25);
                    player.setVelocity(player.getVelocity().add(kb));
                }
            }
            DisplayBuilder.playSound(point, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ResonanceCascade(plugin);
        }
    }
}
