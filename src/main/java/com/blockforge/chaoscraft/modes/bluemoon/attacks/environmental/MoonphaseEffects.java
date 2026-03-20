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
 * Blue Moon Mode — MOONPHASE EFFECTS
 * 13 moon-phase themed environmental effects.
 * Particles: DUST (various moon colors), SNOWFLAKE, END_ROD, SOUL_FIRE_FLAME
 * Sounds: ENTITY_WOLF_HOWL, BLOCK_AMETHYST_BLOCK_CHIME, AMBIENT_CAVE
 * Blue Moon palette: pale blue (180,210,255) / silver (200,200,220) / frost cyan (150,230,255)
 */
public final class MoonphaseEffects {

    private MoonphaseEffects() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new NewMoonDarkness(plugin));
        registry.register(new CrescentSlashWave(plugin));
        registry.register(new HalfMoonSplit(plugin));
        registry.register(new GibbousSwell(plugin));
        registry.register(new FullMoonFrenzy(plugin));
        registry.register(new BloodMoonPulse(plugin));
        registry.register(new WaningChill(plugin));
        registry.register(new WaxingPower(plugin));
        registry.register(new PhaseTransition(plugin));
        registry.register(new LunarAlignment(plugin));
        registry.register(new Moonrise(plugin));
        registry.register(new Moonset(plugin));
        registry.register(new Syzygy(plugin));
    }

    // ================================================================
    // 1. NEW MOON DARKNESS — Dark particle fog in wide radius
    // ================================================================
    public static class NewMoonDarkness extends EnvironmentalAttack {

        private final Random random = new Random();

        public NewMoonDarkness(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("new_moon_darkness", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
            DisplayBuilder.dustParticles(center, 80, 15.0, 20, 20, 40, 2.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Dense dark fog particles
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 15; i++) {
                    double ox = (random.nextDouble() - 0.5) * 40.0;
                    double oy = random.nextDouble() * 4.0;
                    double oz = (random.nextDouble() - 0.5) * 40.0;
                    Location pLoc = c.clone().add(ox, oy, oz);
                    if (pLoc.distance(c) <= 20.0) {
                        DisplayBuilder.dustParticles(pLoc, 3, 1.0, 15, 15, 30, 2.0f);
                    }
                }
            }

            // Occasional dark swirling ring
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.particleRing(c, 10.0 + random.nextDouble() * 10.0, Particle.SOUL_FIRE_FLAME, 12, null);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.7f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new NewMoonDarkness(plugin); }
    }

    // ================================================================
    // 2. CRESCENT SLASH WAVE — Crescent-shaped dust particle wave
    // ================================================================
    public static class CrescentSlashWave extends EnvironmentalAttack {

        private double sweepAngle;

        public CrescentSlashWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crescent_slash_wave", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            sweepAngle = new Random().nextDouble() * 2 * Math.PI;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Crescent sweeps outward from center
            double progress = ticksAlive / 40.0;
            double radius = 1.0 + progress * 12.0;
            double arcWidth = Math.toRadians(120); // 120-degree crescent arc

            // Draw crescent arc at current radius
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 16; i++) {
                    double angle = sweepAngle - arcWidth / 2 + (arcWidth * i / 15.0);
                    Location pLoc = c.clone().add(Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);
                    DisplayBuilder.dustParticles(pLoc, 3, 0.3, 200, 200, 220, 1.5f);
                }
            }

            // Damage players caught in the crescent wave
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;

                double dist = player.getLocation().distance(c);
                if (Math.abs(dist - radius) > 2.0) continue;

                // Check if within crescent arc
                double dx = player.getLocation().getX() - c.getX();
                double dz = player.getLocation().getZ() - c.getZ();
                double playerAngle = Math.atan2(dz, dx);
                double angleDiff = Math.abs(normalizeAngle(playerAngle - sweepAngle));
                if (angleDiff <= arcWidth / 2) {
                    // Push player outward along the wave
                    Vector push = player.getLocation().toVector().subtract(c.toVector()).normalize().multiply(0.4);
                    player.setVelocity(player.getVelocity().add(push));
                }
            }

            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f + (float) progress);
            }
        }

        private double normalizeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CrescentSlashWave(plugin); }
    }

    // ================================================================
    // 3. HALF MOON SPLIT — Area split by particle line, different colors each side
    // ================================================================
    public static class HalfMoonSplit extends EnvironmentalAttack {

        private Vector splitNormal;

        public HalfMoonSplit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("half_moon_split", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random split direction
            double angle = new Random().nextDouble() * Math.PI;
            splitNormal = new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize();

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);

            // Draw the split line
            Vector lineDir = new Vector(-splitNormal.getZ(), 0, splitNormal.getX());
            Location from = center.clone().add(lineDir.clone().multiply(-10));
            Location to = center.clone().add(lineDir.clone().multiply(10));
            DisplayBuilder.particleLine(from, to, Particle.END_ROD, 2, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || splitNormal == null) return;
            World w = c.getWorld();

            Vector lineDir = new Vector(-splitNormal.getZ(), 0, splitNormal.getX());

            // Redraw split line
            if (ticksAlive % 4 == 0) {
                Location from = c.clone().add(lineDir.clone().multiply(-10));
                Location to = c.clone().add(lineDir.clone().multiply(10));
                DisplayBuilder.particleLine(from, to, Particle.END_ROD, 2, null);

                // Side A: pale blue dust
                Random rand = new Random();
                for (int i = 0; i < 6; i++) {
                    double ox = (rand.nextDouble() - 0.5) * 20.0;
                    double oz = (rand.nextDouble() - 0.5) * 20.0;
                    Location pLoc = c.clone().add(ox, 0.5, oz);
                    Vector toPoint = pLoc.toVector().subtract(c.toVector());
                    double side = toPoint.dot(splitNormal);
                    if (pLoc.distance(c) <= 10.0) {
                        if (side > 0) {
                            DisplayBuilder.dustParticles(pLoc, 2, 0.5, 180, 210, 255, 1.2f);
                        } else {
                            DisplayBuilder.dustParticles(pLoc, 2, 0.5, 255, 200, 150, 1.2f);
                        }
                    }
                }
            }

            // Push players away from the split line
            if (ticksAlive % 5 == 0) {
                for (Player player : w.getPlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                    if (player.getLocation().distance(c) > 10.0) continue;

                    Vector toPlayer = player.getLocation().toVector().subtract(c.toVector());
                    double side = toPlayer.dot(splitNormal);
                    // Push player away from line on their respective side
                    double distToLine = Math.abs(side);
                    if (distToLine < 2.0) {
                        Vector push = splitNormal.clone().multiply(side > 0 ? 0.3 : -0.3);
                        player.setVelocity(player.getVelocity().add(push));
                    }
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HalfMoonSplit(plugin); }
    }

    // ================================================================
    // 4. GIBBOUS SWELL — Warning howl, no direct damage, spawns 3 extra attacks
    // ================================================================
    public static class GibbousSwell extends EnvironmentalAttack {

        private boolean attacksSpawned = false;

        public GibbousSwell(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gibbous_swell", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_HOWL, 1.5f, 0.8f);
            DisplayBuilder.dustParticles(center, 60, 10.0, 255, 240, 200, 2.0f);
            DisplayBuilder.particleRing(center, 8.0, Particle.END_ROD, 24, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Building warning visual
            if (ticksAlive % 3 == 0) {
                double pulse = 4.0 + Math.sin(ticksAlive * 0.3) * 3.0;
                DisplayBuilder.particleRing(c, pulse, Particle.END_ROD, 12, null);
                DisplayBuilder.dustParticles(c, 6, pulse, 255, 240, 200, 1.5f);
            }

            // Howl echo
            if (ticksAlive == 15) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_HOWL, 1.0f, 1.0f);
            }

            // At tick 30, signal that extra attacks should be spawned
            // The attack scheduler reads this as a cue to spawn 3 random attacks
            if (ticksAlive == 30 && !attacksSpawned) {
                attacksSpawned = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_HOWL, 1.2f, 1.2f);
                DisplayBuilder.dustParticles(c, 100, 12.0, 200, 200, 220, 2.5f);

                // Spawn 3 burst particle rings as warning
                DisplayBuilder.particleRing(c, 5.0, Particle.SOUL_FIRE_FLAME, 20, null);
                DisplayBuilder.particleRing(c, 10.0, Particle.SOUL_FIRE_FLAME, 20, null);
                DisplayBuilder.particleRing(c, 15.0, Particle.SOUL_FIRE_FLAME, 20, null);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GibbousSwell(plugin); }
    }

    // ================================================================
    // 5. FULL MOON FRENZY — All players take burst damage, silver explosion
    // ================================================================
    public static class FullMoonFrenzy extends EnvironmentalAttack {

        public FullMoonFrenzy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("full_moon_frenzy", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_HOWL, 1.5f, 1.2f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);

            // Massive silver explosion
            DisplayBuilder.dustParticles(center, 120, 15.0, 200, 200, 220, 2.5f);
            DisplayBuilder.particleRing(center, 8.0, Particle.END_ROD, 40, null);
            DisplayBuilder.particleRing(center, 16.0, Particle.END_ROD, 40, null);
            DisplayBuilder.particleRing(center, 24.0, Particle.END_ROD, 40, null);

            // Knockback all players
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                double dist = player.getLocation().distance(center);
                if (dist > 25.0) continue;

                Vector push = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.4);
                push.setY(0.2);
                player.setVelocity(player.getVelocity().add(push));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Fading silver rings
            if (ticksAlive % 2 == 0) {
                double radius = 5.0 + ticksAlive * 1.5;
                DisplayBuilder.particleRing(c, Math.min(radius, 25.0), Particle.END_ROD, 20, null);
                DisplayBuilder.dustParticles(c, 15, radius, 200, 200, 220, 1.5f - ticksAlive * 0.05f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FullMoonFrenzy(plugin); }
    }

    // ================================================================
    // 6. BLOOD MOON PULSE — Red dust wave, unavoidable damage to all
    // ================================================================
    public static class BloodMoonPulse extends EnvironmentalAttack {

        public BloodMoonPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blood_moon_pulse", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(10);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_HOWL, 1.5f, 0.4f);

            // Blood-red particle explosion
            DisplayBuilder.dustParticles(center, 150, 20.0, 180, 30, 30, 2.5f);
            DisplayBuilder.particleRing(center, 10.0, Particle.SOUL_FIRE_FLAME, 30, null);
            DisplayBuilder.particleRing(center, 20.0, Particle.SOUL_FIRE_FLAME, 30, null);
            DisplayBuilder.particleRing(center, 30.0, Particle.SOUL_FIRE_FLAME, 30, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Expanding blood-red ring
            if (ticksAlive % 2 == 0) {
                double radius = ticksAlive * 3.0;
                DisplayBuilder.particleRing(c, Math.min(radius, 30.0), Particle.SOUL_FIRE_FLAME, 16, null);
                DisplayBuilder.dustParticles(c, 20, Math.min(radius, 30.0), 180, 30, 30, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BloodMoonPulse(plugin); }
    }

    // ================================================================
    // 7. WANING CHILL — Frost particles, standing still = damage
    // ================================================================
    public static class WaningChill extends EnvironmentalAttack {

        private final Map<UUID, Location> lastPositions = new HashMap<>();
        private final Map<UUID, Integer> stillTicks = new HashMap<>();

        public WaningChill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("waning_chill", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            lastPositions.clear();
            stillTicks.clear();

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.3f);
            DisplayBuilder.dustParticles(center, 60, 15.0, 150, 230, 255, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Frost particles
            if (ticksAlive % 3 == 0) {
                Random rand = new Random();
                for (int i = 0; i < 10; i++) {
                    double ox = (rand.nextDouble() - 0.5) * 30.0;
                    double oy = rand.nextDouble() * 3.0;
                    double oz = (rand.nextDouble() - 0.5) * 30.0;
                    Location pLoc = c.clone().add(ox, oy, oz);
                    if (pLoc.distance(c) <= 15.0) {
                        DisplayBuilder.dustParticles(pLoc, 2, 0.5, 150, 230, 255, 1.0f);
                        pLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, pLoc, 1, 0.3, 0.3, 0.3, 0);
                    }
                }
            }

            // Track player movement and apply velocity penalty to stationary players
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                if (player.getLocation().distance(c) > 15.0) continue;

                UUID uuid = player.getUniqueId();
                Location currentLoc = player.getLocation();
                Location lastLoc = lastPositions.get(uuid);

                if (lastLoc != null && currentLoc.distanceSquared(lastLoc) < 0.1) {
                    int still = stillTicks.getOrDefault(uuid, 0) + 1;
                    stillTicks.put(uuid, still);

                    // After 2 seconds (40 ticks) of standing still, apply downward drag
                    if (still >= 40) {
                        Vector vel = player.getVelocity();
                        player.setVelocity(new Vector(vel.getX() * 0.6, vel.getY() - 0.05, vel.getZ() * 0.6));

                        // Frost around standing player
                        if (ticksAlive % 5 == 0) {
                            DisplayBuilder.dustParticles(currentLoc, 6, 1.0, 150, 230, 255, 1.5f);
                            currentLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, currentLoc, 4, 0.5, 0.5, 0.5, 0);
                        }
                    }
                } else {
                    stillTicks.put(uuid, 0);
                }

                lastPositions.put(uuid, currentLoc.clone());
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            lastPositions.clear();
            stillTicks.clear();
            super.onCleanup();
        }

        @Override
        public AbstractAttack newInstance() { return new WaningChill(plugin); }
    }

    // ================================================================
    // 8. WAXING POWER — Visual only, glowing particles on boss location
    // ================================================================
    public static class WaxingPower extends EnvironmentalAttack {

        public WaxingPower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("waxing_power", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(60);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Growing glow around center (boss location)
            double intensity = Math.min(ticksAlive / 60.0, 1.0);
            int particleCount = (int) (4 + intensity * 16);

            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(c, particleCount, 3.0, 255, 240, 200, (float) (1.0 + intensity));
                DisplayBuilder.particleRing(c, 2.0 + intensity * 3.0, Particle.END_ROD, (int) (8 + intensity * 12), null);

                // Rising column of light
                for (int y = 0; y < (int) (intensity * 8); y++) {
                    Location pLoc = c.clone().add(0, y, 0);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.5, 255, 240, 200, 0.8f);
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.5f + (float) intensity * 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new WaxingPower(plugin); }
    }

    // ================================================================
    // 9. PHASE TRANSITION — Particle burst, all players briefly knocked back
    // ================================================================
    public static class PhaseTransition extends EnvironmentalAttack {

        public PhaseTransition(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase_transition", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_HOWL, 0.8f, 1.5f);

            // Massive particle burst
            DisplayBuilder.dustParticles(center, 100, 12.0, 180, 210, 255, 2.5f);
            DisplayBuilder.dustParticles(center, 80, 8.0, 200, 200, 220, 2.0f);
            DisplayBuilder.particleRing(center, 10.0, Particle.END_ROD, 40, null);
            DisplayBuilder.particleRing(center, 20.0, Particle.END_ROD, 40, null);

            // Knock all players back
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                double dist = player.getLocation().distance(center);
                if (dist > 20.0) continue;

                Vector push = player.getLocation().toVector().subtract(center.toVector()).normalize();
                double strength = 0.6 * (1.0 - dist / 20.0);
                push.multiply(strength).setY(0.3);
                player.setVelocity(player.getVelocity().add(push));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Dissipating shockwave rings
            if (ticksAlive % 2 == 0) {
                double radius = ticksAlive * 1.5;
                DisplayBuilder.particleRing(c, Math.min(radius, 20.0), Particle.END_ROD, 16, null);
                DisplayBuilder.dustParticles(c, 8, radius, 180, 210, 255, 1.5f - ticksAlive * 0.05f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseTransition(plugin); }
    }

    // ================================================================
    // 10. LUNAR ALIGNMENT — Beam particles from all directions at one player
    // ================================================================
    public static class LunarAlignment extends EnvironmentalAttack {

        private Player targetPlayer;

        public LunarAlignment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_alignment", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pick a random survival player as the alignment target
            List<Player> candidates = new ArrayList<>();
            for (Player player : w.getPlayers()) {
                if (player.getGameMode() == GameMode.SURVIVAL && !player.isInvulnerable()
                        && player.getLocation().distance(center) <= 30.0) {
                    candidates.add(player);
                }
            }
            if (!candidates.isEmpty()) {
                targetPlayer = candidates.get(new Random().nextInt(candidates.size()));
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || targetPlayer == null) return;
            if (!targetPlayer.isOnline() || targetPlayer.isDead()) return;

            Location tLoc = targetPlayer.getLocation().add(0, 1, 0);

            // Draw beam lines converging from 8 compass directions
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    double beamLength = 15.0;
                    Location from = tLoc.clone().add(Math.cos(angle) * beamLength, 5, Math.sin(angle) * beamLength);
                    DisplayBuilder.particleLine(from, tLoc, Particle.END_ROD, 2, null);
                }
                DisplayBuilder.dustParticles(tLoc, 8, 1.0, 200, 200, 220, 1.5f);
            }

            // Converging ring around target
            if (ticksAlive % 4 == 0) {
                double shrinkRadius = 8.0 - (ticksAlive % 20) * 0.4;
                DisplayBuilder.particleRing(tLoc, Math.max(shrinkRadius, 1.0), Particle.SOUL_FIRE_FLAME, 12, null);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(tLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new LunarAlignment(plugin); }
    }

    // ================================================================
    // 11. MOONRISE — Particle sphere rises from horizon, AoE at zenith
    // ================================================================
    public static class Moonrise extends EnvironmentalAttack {

        private Location horizonStart;
        private boolean burstFired = false;

        public Moonrise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moonrise", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Start at a random horizontal offset at ground level
            double angle = new Random().nextDouble() * 2 * Math.PI;
            horizonStart = center.clone().add(Math.cos(angle) * 12, -2, Math.sin(angle) * 12);

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.8f, 1.2f);
            DisplayBuilder.dustParticles(horizonStart, 20, 2.0, 200, 200, 220, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || horizonStart == null) return;

            // Moon rises in an arc from horizon to zenith (directly above center)
            double progress = ticksAlive / 80.0; // reach zenith at tick 80
            progress = Math.min(progress, 1.0);

            // Interpolate position: horizontal toward center, vertical upward
            double horizX = horizonStart.getX() + (c.getX() - horizonStart.getX()) * progress;
            double horizZ = horizonStart.getZ() + (c.getZ() - horizonStart.getZ()) * progress;
            double height = horizonStart.getY() + progress * 15.0; // Rise 15 blocks

            Location moonLoc = new Location(c.getWorld(), horizX, height, horizZ);

            // Draw moon sphere
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(moonLoc, 12, 1.5, 200, 200, 220, 1.5f);
                DisplayBuilder.particleRing(moonLoc, 1.5, Particle.END_ROD, 10, null);

                // Trail particles
                if (ticksAlive > 5) {
                    double prevProgress = (ticksAlive - 5) / 80.0;
                    prevProgress = Math.min(prevProgress, 1.0);
                    double prevX = horizonStart.getX() + (c.getX() - horizonStart.getX()) * prevProgress;
                    double prevZ = horizonStart.getZ() + (c.getZ() - horizonStart.getZ()) * prevProgress;
                    double prevY = horizonStart.getY() + prevProgress * 15.0;
                    Location trail = new Location(c.getWorld(), prevX, prevY, prevZ);
                    DisplayBuilder.dustParticles(trail, 4, 0.5, 150, 230, 255, 0.8f);
                }
            }

            // At zenith (tick 80): AoE burst
            if (ticksAlive >= 80 && !burstFired) {
                burstFired = true;
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.5f);
                DisplayBuilder.dustParticles(moonLoc, 100, 10.0, 200, 200, 220, 2.5f);
                DisplayBuilder.particleRing(c, 15.0, Particle.END_ROD, 40, null);

                // Push all players outward from center
                World w = c.getWorld();
                for (Player player : w.getPlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                    if (player.getLocation().distance(c) > 15.0) continue;

                    Vector push = player.getLocation().toVector().subtract(c.toVector()).normalize().multiply(0.5);
                    push.setY(0.2);
                    player.setVelocity(player.getVelocity().add(push));
                }
            }

            // Post-burst dissipation
            if (ticksAlive > 80 && ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(moonLoc, 6, 3.0 + (ticksAlive - 80) * 0.3, 200, 200, 220, 1.0f);
            }

            if (ticksAlive % 20 == 0 && ticksAlive < 80) {
                DisplayBuilder.playSound(moonLoc, Sound.AMBIENT_CAVE, 0.4f, 1.0f + (float) progress * 0.5f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new Moonrise(plugin); }
    }

    // ================================================================
    // 12. MOONSET — Reverse of moonrise, particle descends, AoE at end
    // ================================================================
    public static class Moonset extends EnvironmentalAttack {

        private Location horizonEnd;
        private boolean burstFired = false;

        public Moonset(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moonset", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // End point at a random horizontal offset at ground level
            double angle = new Random().nextDouble() * 2 * Math.PI;
            horizonEnd = center.clone().add(Math.cos(angle) * 12, -2, Math.sin(angle) * 12);

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.8f, 0.6f);
            DisplayBuilder.dustParticles(center.clone().add(0, 13, 0), 30, 2.0, 150, 230, 255, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || horizonEnd == null) return;

            // Moon descends from zenith (above center) to horizon
            double progress = ticksAlive / 80.0;
            progress = Math.min(progress, 1.0);

            // Start above center, move toward horizon end point
            double moonX = c.getX() + (horizonEnd.getX() - c.getX()) * progress;
            double moonZ = c.getZ() + (horizonEnd.getZ() - c.getZ()) * progress;
            double height = c.getY() + 13.0 - progress * 15.0; // Descend from 13 to -2

            Location moonLoc = new Location(c.getWorld(), moonX, height, moonZ);

            // Draw descending moon
            if (ticksAlive % 2 == 0) {
                DisplayBuilder.dustParticles(moonLoc, 12, 1.5, 150, 230, 255, 1.5f);
                DisplayBuilder.particleRing(moonLoc, 1.5, Particle.SNOWFLAKE, 8, null);

                // Fading trail
                if (ticksAlive > 5) {
                    double prevProgress = (ticksAlive - 5) / 80.0;
                    prevProgress = Math.min(prevProgress, 1.0);
                    double prevX = c.getX() + (horizonEnd.getX() - c.getX()) * prevProgress;
                    double prevZ = c.getZ() + (horizonEnd.getZ() - c.getZ()) * prevProgress;
                    double prevY = c.getY() + 13.0 - prevProgress * 15.0;
                    Location trail = new Location(c.getWorld(), prevX, prevY, prevZ);
                    DisplayBuilder.dustParticles(trail, 4, 0.5, 200, 200, 220, 0.8f);
                }
            }

            // Color shifts from silver to deep blue as it descends
            if (ticksAlive % 6 == 0) {
                int r = (int) (200 - progress * 150);
                int g = (int) (200 - progress * 150);
                int b = (int) (220 - progress * 80);
                DisplayBuilder.dustParticles(moonLoc, 6, 2.0, r, g, b, 1.8f);
            }

            // At horizon (tick 80): ground AoE burst
            if (ticksAlive >= 80 && !burstFired) {
                burstFired = true;
                DisplayBuilder.playSound(horizonEnd, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.4f);
                DisplayBuilder.playSound(horizonEnd, Sound.ENTITY_WOLF_HOWL, 0.8f, 0.6f);
                DisplayBuilder.dustParticles(horizonEnd, 100, 12.0, 50, 50, 140, 2.5f);
                DisplayBuilder.particleRing(horizonEnd, 15.0, Particle.SOUL_FIRE_FLAME, 30, null);

                // Knockback from impact point
                World w = c.getWorld();
                for (Player player : w.getPlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                    if (player.getLocation().distance(horizonEnd) > 15.0) continue;

                    Vector push = player.getLocation().toVector().subtract(horizonEnd.toVector()).normalize().multiply(0.6);
                    push.setY(0.3);
                    player.setVelocity(player.getVelocity().add(push));
                }
            }

            // Post-burst ground tremor particles
            if (ticksAlive > 80 && ticksAlive % 4 == 0) {
                double radius = (ticksAlive - 80) * 0.8;
                DisplayBuilder.particleRing(horizonEnd, radius, Particle.SOUL_FIRE_FLAME, 10, null);
            }

            if (ticksAlive % 20 == 0 && ticksAlive < 80) {
                DisplayBuilder.playSound(moonLoc, Sound.AMBIENT_CAVE, 0.4f, 0.8f - (float) progress * 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new Moonset(plugin); }
    }

    // ================================================================
    // 13. SYZYGY — Pull all players into a line, then damage along line
    // ================================================================
    public static class Syzygy extends EnvironmentalAttack {

        private Vector lineDirection;
        private boolean lineDamageFired = false;

        public Syzygy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("syzygy", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(60);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random alignment direction
            double angle = new Random().nextDouble() * Math.PI;
            lineDirection = new Vector(Math.cos(angle), 0, Math.sin(angle)).normalize();

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_HOWL, 1.2f, 1.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);

            // Show the alignment line
            Location from = center.clone().add(lineDirection.clone().multiply(-12));
            Location to = center.clone().add(lineDirection.clone().multiply(12));
            DisplayBuilder.particleLine(from, to, Particle.END_ROD, 2, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || lineDirection == null) return;
            World w = c.getWorld();

            Vector perpendicular = new Vector(-lineDirection.getZ(), 0, lineDirection.getX());

            if (ticksAlive <= 40) {
                // Phase 1: Pull players toward the alignment line (40 ticks)
                for (Player player : w.getPlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                    if (player.getLocation().distance(c) > 12.0) continue;

                    // Calculate perpendicular distance to line
                    Vector toPlayer = player.getLocation().toVector().subtract(c.toVector());
                    double perpDist = toPlayer.dot(perpendicular);

                    // Pull toward the line (reduce perpendicular distance)
                    if (Math.abs(perpDist) > 0.5) {
                        Vector pullToLine = perpendicular.clone().multiply(-perpDist * 0.08);
                        player.setVelocity(player.getVelocity().add(pullToLine));
                    }
                }

                // Alignment line visual with converging particles
                if (ticksAlive % 3 == 0) {
                    Location from = c.clone().add(lineDirection.clone().multiply(-12));
                    Location to = c.clone().add(lineDirection.clone().multiply(12));
                    DisplayBuilder.particleLine(from, to, Particle.END_ROD, 2, null);

                    // Side particles pulling inward
                    for (int i = -4; i <= 4; i += 2) {
                        Location side1 = c.clone().add(lineDirection.clone().multiply(i)).add(perpendicular.clone().multiply(5 - ticksAlive * 0.1));
                        Location side2 = c.clone().add(lineDirection.clone().multiply(i)).add(perpendicular.clone().multiply(-(5 - ticksAlive * 0.1)));
                        DisplayBuilder.dustParticles(side1, 2, 0.3, 180, 210, 255, 1.0f);
                        DisplayBuilder.dustParticles(side2, 2, 0.3, 180, 210, 255, 1.0f);
                    }
                }

                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.5f + ticksAlive * 0.025f);
                }
            } else if (!lineDamageFired) {
                // Phase 2: Damage along the line
                lineDamageFired = true;

                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_HOWL, 1.5f, 1.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 2.0f);

                // Intense line burst
                Location from = c.clone().add(lineDirection.clone().multiply(-12));
                Location to = c.clone().add(lineDirection.clone().multiply(12));
                DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 3, null);
                DisplayBuilder.particleLine(from, to, Particle.END_ROD, 3, null);
                DisplayBuilder.dustParticles(c, 80, 6.0, 200, 200, 220, 2.5f);

                // Knockback players along the line outward from center
                for (Player player : w.getPlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL || player.isInvulnerable()) continue;
                    if (player.getLocation().distance(c) > 12.0) continue;

                    Vector toPlayer = player.getLocation().toVector().subtract(c.toVector());
                    double alongLine = toPlayer.dot(lineDirection);
                    Vector push = lineDirection.clone().multiply(alongLine > 0 ? 0.8 : -0.8);
                    push.setY(0.3);
                    player.setVelocity(player.getVelocity().add(push));
                }
            }

            // Post-burst fading
            if (ticksAlive > 40 && ticksAlive % 4 == 0) {
                Location from = c.clone().add(lineDirection.clone().multiply(-12));
                Location to = c.clone().add(lineDirection.clone().multiply(12));
                DisplayBuilder.particleLine(from, to, Particle.END_ROD, 1, null);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new Syzygy(plugin); }
    }
}
