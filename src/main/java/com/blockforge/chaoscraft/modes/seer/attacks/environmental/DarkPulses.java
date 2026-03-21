package com.blockforge.chaoscraft.modes.seer.attacks.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import java.util.*;

/**
 * Seer Mode — DARK PULSES
 * 12 environmental dark pulse attacks.
 * Palette: Void black (20,0,40), Deep purple (80,0,160), Eye red (200,0,50)
 */
public final class DarkPulses {

    private DarkPulses() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShadowWave(plugin));
        registry.register(new DarkAoeBurst(plugin));
        registry.register(new CreepingDarkness(plugin));
        registry.register(new UmbralPulse(plugin));
        registry.register(new VoidShockwave(plugin));
        registry.register(new DarkCascade(plugin));
        registry.register(new ShadowRain(plugin));
        registry.register(new NightmareFog(plugin));
        registry.register(new DarkTremor(plugin));
        registry.register(new VoidScream(plugin));
        registry.register(new ShadowBolt(plugin));
        registry.register(new TotalVoid(plugin));
    }

    // ================================================================
    // 1. SHADOW WAVE — Dark expanding ring sweeping outward
    // ================================================================
    public static class ShadowWave extends EnvironmentalAttack {
        public ShadowWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_wave", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 10, 1.0, 20, 0, 40, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            double radius = ticksAlive * 0.4;
            if (radius <= 10) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), radius, Particle.DUST,
                        (int) Math.max(8, radius * 3), new Particle.DustOptions(Color.fromRGB(20, 0, 40), 2.0f));
                DisplayBuilder.particleRing(center.clone().add(0, 0.8, 0), radius * 0.9, Particle.DUST,
                        (int) Math.max(6, radius * 2), new Particle.DustOptions(Color.fromRGB(80, 0, 160), 1.5f));
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (Math.abs(dist - radius) < 1.5) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ShadowWave(plugin); }
    }

    // ================================================================
    // 2. DARK AOE BURST — Instant dark explosion
    // ================================================================
    public static class DarkAoeBurst extends EnvironmentalAttack {
        private boolean bursted = false;

        public DarkAoeBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_aoe_burst", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(15);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 8, 2.0, 20, 0, 40, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            if (!bursted && ticksAlive >= 5) {
                bursted = true;
                triggerImpactDamage(center);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 30, 6.0, 20, 0, 40, 2.5f);
                DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 15, 4.0, 200, 0, 50, 2.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DarkAoeBurst(plugin); }
    }

    // ================================================================
    // 3. CREEPING DARKNESS — Slow expanding dark zone on ground
    // ================================================================
    public static class CreepingDarkness extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public CreepingDarkness(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("creeping_darkness", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center, 10, 1.0, 20, 0, 40, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            double currentRadius = 2.0 + (ticksAlive / 200.0) * 8.0;
            config.setDamageRadius(currentRadius);
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * currentRadius;
                    DisplayBuilder.dustParticles(center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist),
                            2, 0.3, 20, 0, 40, 1.2f);
                }
            }
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), currentRadius, Particle.DUST,
                        (int) Math.max(8, currentRadius * 2), new Particle.DustOptions(Color.fromRGB(80, 0, 160), 0.8f));
            }
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CreepingDarkness(plugin); }
    }

    // ================================================================
    // 4. UMBRAL PULSE — Rhythmic dark pulses from center
    // ================================================================
    public static class UmbralPulse extends EnvironmentalAttack {
        public UmbralPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("umbral_pulse", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 10, 2.0, 20, 0, 40, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            // Pulse every 20 ticks
            if (ticksAlive % 20 == 0) {
                double r = 3.0 + ((ticksAlive / 20) % 3) * 1.5;
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r, Particle.DUST,
                        16, new Particle.DustOptions(Color.fromRGB(20, 0, 40), 2.0f));
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.8f);
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 4, 2.0, 80, 0, 160, 1.0f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new UmbralPulse(plugin); }
    }

    // ================================================================
    // 5. VOID SHOCKWAVE — Ground-level shockwave with knockback
    // ================================================================
    public static class VoidShockwave extends EnvironmentalAttack {
        public VoidShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_shockwave", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(8.0);
            config.setDurationTicks(25);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center, 15, 2.0, 20, 0, 40, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            double radius = ticksAlive * 0.4;
            if (radius <= 8) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), radius, Particle.DUST,
                        (int) Math.max(8, radius * 4), new Particle.DustOptions(Color.fromRGB(20, 0, 40), 2.0f));
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (Math.abs(dist - radius) < 1.5) {
                        player.damage(config.getImpactDamage());
                        player.setNoDamageTicks(0);
                        Vector push = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.8);
                        push.setY(0.4);
                        player.setVelocity(player.getVelocity().add(push));
                    }
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VoidShockwave(plugin); }
    }

    // ================================================================
    // 6. DARK CASCADE — Multiple dark bursts in sequence
    // ================================================================
    public static class DarkCascade extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final double[][] burstPositions = new double[5][2];

        public DarkCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_cascade", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(50);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 5; i++) {
                burstPositions[i][0] = (RNG.nextDouble() - 0.5) * 12;
                burstPositions[i][1] = (RNG.nextDouble() - 0.5) * 12;
            }
            DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 10, 5.0, 20, 0, 40, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            int burstIndex = ticksAlive / 10;
            if (burstIndex < 5 && ticksAlive % 10 == 0) {
                Location burstLoc = center.clone().add(burstPositions[burstIndex][0], 0, burstPositions[burstIndex][1]);
                DisplayBuilder.dustParticles(burstLoc.clone().add(0, 1, 0), 15, 3.0, 20, 0, 40, 2.0f);
                DisplayBuilder.dustParticles(burstLoc.clone().add(0, 2, 0), 8, 2.0, 80, 0, 160, 1.5f);
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(burstLoc) <= 9) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
                DisplayBuilder.playSound(burstLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
            }
            // Warning circle for next burst
            if (burstIndex < 4) {
                int nextBurst = burstIndex + 1;
                if (ticksAlive % 3 == 0) {
                    Location nextLoc = center.clone().add(burstPositions[nextBurst][0], 0.2, burstPositions[nextBurst][1]);
                    DisplayBuilder.particleRing(nextLoc, 3.0, Particle.DUST,
                            8, new Particle.DustOptions(Color.fromRGB(200, 0, 50), 1.0f));
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DarkCascade(plugin); }
    }

    // ================================================================
    // 7. SHADOW RAIN — Dark particles falling from above, damage on hit
    // ================================================================
    public static class ShadowRain extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public ShadowRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_rain", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 15; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                DisplayBuilder.dustParticles(center.clone().add(ox, 12, oz), 3, 1.0, 20, 0, 40, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 20;
                    double oz = (RNG.nextDouble() - 0.5) * 20;
                    double y = 10 + RNG.nextDouble() * 3;
                    DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 1, 0.3, 20, 0, 40, 1.0f);
                }
                for (int i = 0; i < 4; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 18;
                    double oz = (RNG.nextDouble() - 0.5) * 18;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 0.2, oz), 2, 0.3, 80, 0, 160, 0.8f);
                }
            }
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.WEATHER_RAIN_ABOVE, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ShadowRain(plugin); }
    }

    // ================================================================
    // 8. NIGHTMARE FOG — Dense dark fog obscuring vision, slow + damage
    // ================================================================
    public static class NightmareFog extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public NightmareFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_fog", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 25; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 16;
                double oz = (RNG.nextDouble() - 0.5) * 16;
                DisplayBuilder.dustParticles(center.clone().add(ox, RNG.nextDouble() * 3, oz), 3, 0.8, 20, 0, 40, 1.2f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 16;
                    double oz = (RNG.nextDouble() - 0.5) * 16;
                    double y = RNG.nextDouble() * 3;
                    DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 2, 0.5, 20, 0, 40, 1.0f);
                }
            }
            if (ticksAlive % 4 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 64) {
                        Vector vel = player.getVelocity();
                        player.setVelocity(new Vector(vel.getX() * 0.6, vel.getY(), vel.getZ() * 0.6));
                    }
                }
            }
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new NightmareFog(plugin); }
    }

    // ================================================================
    // 9. DARK TREMOR — Ground shaking with dark particles
    // ================================================================
    public static class DarkTremor extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public DarkTremor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_tremor", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center, 15, 6.0, 20, 0, 40, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DIG, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 2 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 64) {
                        player.setVelocity(player.getVelocity().add(
                                new Vector((RNG.nextDouble() - 0.5) * 0.1, 0.05, (RNG.nextDouble() - 0.5) * 0.1)));
                    }
                }
            }
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 16;
                    double oz = (RNG.nextDouble() - 0.5) * 16;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 0.1 + RNG.nextDouble() * 0.5, oz),
                            2, 0.3, 20, 0, 40, 1.2f);
                }
            }
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_STEP, 0.5f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DarkTremor(plugin); }
    }

    // ================================================================
    // 10. VOID SCREAM — Directional cone of damage
    // ================================================================
    public static class VoidScream extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double screamDirX, screamDirZ;
        private boolean screamed = false;

        public VoidScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_scream", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(10.0);
            config.setDurationTicks(25);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = RNG.nextDouble() * 2 * Math.PI;
            screamDirX = Math.cos(angle);
            screamDirZ = Math.sin(angle);
            for (int i = 0; i < 6; i++) {
                Location origin = center.clone().add(-screamDirX * 3, 1 + RNG.nextDouble(), -screamDirZ * 3);
                DisplayBuilder.dustParticles(origin, 3, 0.5, 200, 0, 50, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive < 10 && ticksAlive % 2 == 0) {
                for (int i = 0; i < 4; i++) {
                    double d = RNG.nextDouble() * 5;
                    double spread = (RNG.nextDouble() - 0.5) * 2;
                    Location loc = center.clone().add(-screamDirX * (3 - d) + (-screamDirZ * spread), 1.5, -screamDirZ * (3 - d) + (screamDirX * spread));
                    DisplayBuilder.dustParticles(loc, 2, 0.3, 20, 0, 40, 1.5f);
                }
            }
            if (!screamed && ticksAlive >= 10) {
                screamed = true;
                for (int d = 0; d <= 10; d++) {
                    double coneWidth = d * 0.4;
                    for (int i = 0; i < 4; i++) {
                        double spread = (RNG.nextDouble() - 0.5) * coneWidth * 2;
                        double y = 0.5 + RNG.nextDouble() * 2;
                        Location loc = center.clone().add(screamDirX * d + (-screamDirZ * spread), y, screamDirZ * d + (screamDirX * spread));
                        DisplayBuilder.dustParticles(loc, 2, 0.3, 20, 0, 40, 2.0f);
                    }
                }
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = ploc.getX() - center.getX();
                    double dz = ploc.getZ() - center.getZ();
                    double projection = dx * screamDirX + dz * screamDirZ;
                    if (projection > 0 && projection <= 10) {
                        double perpDist = Math.abs(dx * (-screamDirZ) + dz * screamDirX);
                        if (perpDist <= projection * 0.4) {
                            player.damage(config.getImpactDamage());
                            player.setNoDamageTicks(0);
                            player.setVelocity(player.getVelocity().add(new Vector(screamDirX * 0.5, 0.2, screamDirZ * 0.5)));
                        }
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VoidScream(plugin); }
    }

    // ================================================================
    // 11. SHADOW BOLT — Single fast projectile tracking nearest player
    // ================================================================
    public static class ShadowBolt extends EnvironmentalAttack {
        private Location boltPos;
        private boolean hit = false;

        public ShadowBolt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_bolt", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            boltPos = center.clone().add(0, 5, 0);
            DisplayBuilder.dustParticles(boltPos, 8, 1.0, 20, 0, 40, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_CHARGE, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            if (boltPos == null || boltPos.getWorld() == null || hit) return;
            World w = boltPos.getWorld();
            Player target = null;
            double closest = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double d = p.getLocation().distanceSquared(boltPos);
                if (d < closest) { closest = d; target = p; }
            }
            if (target != null) {
                Vector dir = target.getLocation().add(0, 1, 0).toVector().subtract(boltPos.toVector());
                if (dir.lengthSquared() < 4) {
                    hit = true;
                    triggerImpactDamage(boltPos);
                    DisplayBuilder.dustParticles(boltPos, 15, 2.0, 200, 0, 50, 2.0f);
                    DisplayBuilder.playSound(boltPos, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f);
                    return;
                }
                dir.normalize().multiply(0.8);
                boltPos.add(dir);
            }
            DisplayBuilder.dustParticles(boltPos, 4, 0.3, 20, 0, 40, 1.5f);
            DisplayBuilder.dustParticles(boltPos, 2, 0.2, 80, 0, 160, 1.0f);
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.playSound(boltPos, Sound.ENTITY_VEX_AMBIENT, 0.3f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ShadowBolt(plugin); }
    }

    // ================================================================
    // 12. TOTAL VOID — Everything goes dark, heavy damage zone
    // ================================================================
    public static class TotalVoid extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public TotalVoid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("total_void", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 30; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 24;
                double oz = (RNG.nextDouble() - 0.5) * 24;
                double y = RNG.nextDouble() * 10;
                DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 3, 1.0, 20, 0, 40, 2.0f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 15; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 24;
                    double oz = (RNG.nextDouble() - 0.5) * 24;
                    double y = RNG.nextDouble() * 8;
                    DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 2, 0.5, 20, 0, 40, 1.5f);
                }
            }
            if (ticksAlive % 4 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                DisplayBuilder.dustParticles(center.clone().add(ox, 0.5, oz), 5, 1.0, 200, 0, 50, 2.0f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new TotalVoid(plugin); }
    }
}
