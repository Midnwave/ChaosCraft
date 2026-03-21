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
 * Seer Mode — PSYCHIC STORMS
 * 13 environmental psychic storm attacks.
 * Palette: Deep purple (80,0,160), Psychic magenta (200,0,180), Pale eye white (240,230,255)
 */
public final class PsychicStorms {

    private PsychicStorms() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PsiStorm(plugin));
        registry.register(new MindBoltRain(plugin));
        registry.register(new NeuralOverloadWave(plugin));
        registry.register(new ThoughtHail(plugin));
        registry.register(new PsychicWind(plugin));
        registry.register(new BrainwaveShockwave(plugin));
        registry.register(new TelepathyPulse(plugin));
        registry.register(new KineticBurst(plugin));
        registry.register(new PsiVortex(plugin));
        registry.register(new MentalFog(plugin));
        registry.register(new PsychicLightning(plugin));
        registry.register(new ConsciousnessStorm(plugin));
        registry.register(new PsionicEruption(plugin));
    }

    // ================================================================
    // 1. PSI STORM — Wide AoE particle storm with damage
    // ================================================================
    public static class PsiStorm extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public PsiStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("psi_storm", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 20; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                DisplayBuilder.dustParticles(center.clone().add(ox, 10 + RNG.nextDouble() * 5, oz), 3, 1.0, 80, 0, 160, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 20;
                    double oz = (RNG.nextDouble() - 0.5) * 20;
                    double y = RNG.nextDouble() * 12;
                    if (RNG.nextBoolean()) {
                        DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 2, 0.5, 80, 0, 160, 1.2f);
                    } else {
                        DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 2, 0.5, 200, 0, 180, 1.0f);
                    }
                }
            }
            if (ticksAlive % 6 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 16;
                double oz = (RNG.nextDouble() - 0.5) * 16;
                DisplayBuilder.dustParticles(center.clone().add(ox, 0.5, oz), 6, 1.0, 240, 230, 255, 2.0f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 0.8f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PsiStorm(plugin); }
    }

    // ================================================================
    // 2. MIND BOLT RAIN — Purple bolts falling from sky at random locations
    // ================================================================
    public static class MindBoltRain extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public MindBoltRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mind_bolt_rain", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 15, 0), 20, 8.0, 200, 0, 180, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 4 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                Location strikeLoc = center.clone().add(ox, 0, oz);
                for (int y = 0; y <= 12; y++) {
                    DisplayBuilder.dustParticles(strikeLoc.clone().add(0, y, 0), 2, 0.2, 200, 0, 180, 1.5f);
                }
                DisplayBuilder.dustParticles(strikeLoc, 6, 1.0, 240, 230, 255, 1.5f);
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(strikeLoc) <= 4) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
                DisplayBuilder.playSound(strikeLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.5f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MindBoltRain(plugin); }
    }

    // ================================================================
    // 3. NEURAL OVERLOAD WAVE — Expanding shockwave ring
    // ================================================================
    public static class NeuralOverloadWave extends EnvironmentalAttack {
        public NeuralOverloadWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("neural_overload_wave", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 15, 2.0, 200, 0, 180, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            double radius = ticksAlive * 0.5;
            if (radius <= 12) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), radius, Particle.DUST,
                        (int) Math.max(8, radius * 3), new Particle.DustOptions(Color.fromRGB(200, 0, 180), 2.0f));
                DisplayBuilder.particleRing(center.clone().add(0, 1.5, 0), radius * 0.8, Particle.DUST,
                        (int) Math.max(6, radius * 2), new Particle.DustOptions(Color.fromRGB(80, 0, 160), 1.5f));
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (Math.abs(dist - radius) < 1.5) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                        Vector push = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.5);
                        push.setY(0.2);
                        player.setVelocity(player.getVelocity().add(push));
                    }
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new NeuralOverloadWave(plugin); }
    }

    // ================================================================
    // 4. THOUGHT HAIL — Falling particle projectiles from sky
    // ================================================================
    public static class ThoughtHail extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public ThoughtHail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thought_hail", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 12, 0), 20, 8.0, 240, 230, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_BITE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 3; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 18;
                    double oz = (RNG.nextDouble() - 0.5) * 18;
                    Location dropLoc = center.clone().add(ox, 0, oz);
                    for (int y = 10; y >= 0; y -= 2) {
                        DisplayBuilder.dustParticles(dropLoc.clone().add(0, y, 0), 1, 0.2, 240, 230, 255, 1.0f);
                    }
                    for (Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        if (player.getLocation().distanceSquared(dropLoc) <= 4) {
                            player.damage(config.getDamage());
                            player.setNoDamageTicks(0);
                        }
                    }
                }
            }
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 18;
                    double oz = (RNG.nextDouble() - 0.5) * 18;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 10 + RNG.nextDouble() * 3, oz), 2, 0.5, 80, 0, 160, 1.0f);
                }
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.4f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ThoughtHail(plugin); }
    }

    // ================================================================
    // 5. PSYCHIC WIND — Directional push with particles
    // ================================================================
    public static class PsychicWind extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double windX, windZ;

        public PsychicWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("psychic_wind", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = RNG.nextDouble() * 2 * Math.PI;
            windX = Math.cos(angle);
            windZ = Math.sin(angle);
            DisplayBuilder.dustParticles(center.clone().add(-windX * 8, 3, -windZ * 8), 15, 3.0, 200, 0, 180, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 2 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 144) {
                        player.setVelocity(player.getVelocity().add(new Vector(windX * 0.08, 0, windZ * 0.08)));
                    }
                }
            }
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double d = RNG.nextDouble() * 12;
                    double spread = (RNG.nextDouble() - 0.5) * 6;
                    double perpX = -windZ * spread;
                    double perpZ = windX * spread;
                    double y = 0.5 + RNG.nextDouble() * 3;
                    Location loc = center.clone().add(windX * d + perpX, y, windZ * d + perpZ);
                    DisplayBuilder.dustParticles(loc, 1, 0.4, 200, 0, 180, 1.0f);
                }
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PsychicWind(plugin); }
    }

    // ================================================================
    // 6. BRAINWAVE SHOCKWAVE — Multiple concentric rings
    // ================================================================
    public static class BrainwaveShockwave extends EnvironmentalAttack {
        public BrainwaveShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brainwave_shockwave", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 10, 1.0, 80, 0, 160, 2.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            // 3 waves, spaced 15 ticks apart
            for (int wave = 0; wave < 3; wave++) {
                int waveStart = wave * 15;
                if (ticksAlive >= waveStart) {
                    double radius = (ticksAlive - waveStart) * 0.6;
                    if (radius <= 8 && radius > 0) {
                        DisplayBuilder.particleRing(center.clone().add(0, 0.5 + wave * 0.5, 0), radius, Particle.DUST,
                                (int) Math.max(6, radius * 2), new Particle.DustOptions(Color.fromRGB(200, 0, 180), 1.5f));
                    }
                }
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 0.8f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new BrainwaveShockwave(plugin); }
    }

    // ================================================================
    // 7. TELEPATHY PULSE — Connects two random players with damage beam
    // ================================================================
    public static class TelepathyPulse extends EnvironmentalAttack {
        public TelepathyPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("telepathy_pulse", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 15, 5.0, 240, 230, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            List<Player> nearby = new ArrayList<>();
            for (Player p : w.getPlayers()) {
                if (!isExempt(p) && p.getLocation().distanceSquared(center) <= 400) {
                    nearby.add(p);
                }
            }
            if (nearby.size() >= 2 && ticksAlive % 5 == 0) {
                Player a = nearby.get(0);
                Player b = nearby.get(1);
                Location locA = a.getLocation().clone().add(0, 1.5, 0);
                Location locB = b.getLocation().clone().add(0, 1.5, 0);
                Vector dir = locB.toVector().subtract(locA.toVector());
                double dist = dir.length();
                dir.normalize();
                for (double d = 0; d < dist; d += 0.8) {
                    Location beamPoint = locA.clone().add(dir.clone().multiply(d));
                    DisplayBuilder.dustParticles(beamPoint, 1, 0.2, 200, 0, 180, 1.2f);
                }
            }
            if (ticksAlive % 10 == 0 && nearby.size() >= 2) {
                nearby.get(0).damage(config.getDamage());
                nearby.get(0).setNoDamageTicks(0);
                nearby.get(1).damage(config.getDamage());
                nearby.get(1).setNoDamageTicks(0);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new TelepathyPulse(plugin); }
    }

    // ================================================================
    // 8. KINETIC BURST — All players knocked away from center with damage
    // ================================================================
    public static class KineticBurst extends EnvironmentalAttack {
        private boolean bursted = false;

        public KineticBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("kinetic_burst", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(8.0);
            config.setDurationTicks(15);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 10, 1.5, 80, 0, 160, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_CHARGE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (!bursted && ticksAlive >= 5) {
                bursted = true;
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 64) {
                        player.damage(config.getImpactDamage());
                        player.setNoDamageTicks(0);
                        Vector push = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.8);
                        push.setY(0.6);
                        player.setVelocity(push);
                    }
                }
                for (double r = 1; r <= 8; r += 1.5) {
                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0), r, Particle.DUST,
                            (int) (r * 4), new Particle.DustOptions(Color.fromRGB(200, 0, 180), 2.0f));
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new KineticBurst(plugin); }
    }

    // ================================================================
    // 9. PSI VORTEX — Spinning particle vortex pulling players in
    // ================================================================
    public static class PsiVortex extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public PsiVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("psi_vortex", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 15, 5.0, 80, 0, 160, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = ticksAlive * 0.15 + i * (Math.PI / 4);
                    double r = 6.0 - (i / 8.0) * 3.0;
                    double y = i * 0.5;
                    DisplayBuilder.dustParticles(center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r),
                            1, 0.2, 200, 0, 180, 1.2f);
                }
            }
            if (ticksAlive % 4 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 36) {
                        Vector pull = center.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.15);
                        player.setVelocity(player.getVelocity().add(pull));
                    }
                }
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PsiVortex(plugin); }
    }

    // ================================================================
    // 10. MENTAL FOG — Wide slow zone with obscuring particles
    // ================================================================
    public static class MentalFog extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public MentalFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mental_fog", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 25; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                DisplayBuilder.dustParticles(center.clone().add(ox, RNG.nextDouble() * 3, oz), 3, 1.0, 80, 0, 160, 1.0f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 20;
                    double oz = (RNG.nextDouble() - 0.5) * 20;
                    double y = RNG.nextDouble() * 3;
                    DisplayBuilder.dustParticles(center.clone().add(ox, y, oz), 2, 0.5, 80, 0, 160, 0.8f);
                }
            }
            if (ticksAlive % 4 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 100) {
                        Vector vel = player.getVelocity();
                        player.setVelocity(new Vector(vel.getX() * 0.7, vel.getY(), vel.getZ() * 0.7));
                    }
                }
            }
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MentalFog(plugin); }
    }

    // ================================================================
    // 11. PSYCHIC LIGHTNING — Purple jagged bolts from sky
    // ================================================================
    public static class PsychicLightning extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public PsychicLightning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("psychic_lightning", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 15, 0), 15, 6.0, 200, 0, 180, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 6 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 18;
                double oz = (RNG.nextDouble() - 0.5) * 18;
                Location strike = center.clone().add(ox, 0, oz);
                double bx = strike.getX(), bz = strike.getZ();
                for (int y = 15; y >= 0; y--) {
                    bx += (RNG.nextDouble() - 0.5) * 0.6;
                    bz += (RNG.nextDouble() - 0.5) * 0.6;
                    DisplayBuilder.dustParticles(new Location(w, bx, strike.getY() + y, bz), 2, 0.1, 200, 0, 180, 1.5f);
                }
                DisplayBuilder.dustParticles(strike, 6, 1.0, 240, 230, 255, 1.5f);
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(strike) <= 4) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
                DisplayBuilder.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PsychicLightning(plugin); }
    }

    // ================================================================
    // 12. CONSCIOUSNESS STORM — Chaotic particle storm + AoE damage
    // ================================================================
    public static class ConsciousnessStorm extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public ConsciousnessStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("consciousness_storm", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 5, 0), 20, 6.0, 80, 0, 160, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 16;
                    double oy = RNG.nextDouble() * 8;
                    double oz = (RNG.nextDouble() - 0.5) * 16;
                    int colorChoice = RNG.nextInt(3);
                    switch (colorChoice) {
                        case 0 -> DisplayBuilder.dustParticles(center.clone().add(ox, oy, oz), 2, 0.4, 80, 0, 160, 1.2f);
                        case 1 -> DisplayBuilder.dustParticles(center.clone().add(ox, oy, oz), 2, 0.4, 200, 0, 180, 1.0f);
                        case 2 -> DisplayBuilder.dustParticles(center.clone().add(ox, oy, oz), 2, 0.4, 240, 230, 255, 0.8f);
                    }
                }
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.4f, RNG.nextFloat() * 1.5f + 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ConsciousnessStorm(plugin); }
    }

    // ================================================================
    // 13. PSIONIC ERUPTION — Ground erupts with purple energy
    // ================================================================
    public static class PsionicEruption extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private boolean erupted = false;

        public PsionicEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("psionic_eruption", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), 6.0, Particle.DUST,
                    16, new Particle.DustOptions(Color.fromRGB(200, 0, 180), 1.5f));
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DIG, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive < 15 && ticksAlive % 2 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), 6.0 - ticksAlive * 0.3, Particle.DUST,
                        12, new Particle.DustOptions(Color.fromRGB(200, 0, 180), 1.0f + ticksAlive * 0.1f));
                DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.5f + ticksAlive * 0.1f);
            }
            if (!erupted && ticksAlive >= 15) {
                erupted = true;
                for (int i = 0; i < 20; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * 6;
                    double y = RNG.nextDouble() * 8;
                    DisplayBuilder.dustParticles(center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist),
                            3, 0.5, 200, 0, 180, 2.0f);
                }
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 36) {
                        player.damage(config.getImpactDamage());
                        player.setNoDamageTicks(0);
                        player.setVelocity(player.getVelocity().add(new Vector(0, 1.0, 0)));
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PsionicEruption(plugin); }
    }
}
