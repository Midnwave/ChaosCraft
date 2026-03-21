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
 * Seer Mode — MIND EFFECTS
 * 13 environmental psychic attacks using particles, velocity manipulation, damage.
 * Palette: Deep purple (80,0,160), Psychic magenta (200,0,180), Pale eye white (240,230,255)
 */
public final class MindEffects {

    private MindEffects() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ConfusionPush(plugin));
        registry.register(new DisorientationSpin(plugin));
        registry.register(new FearPulse(plugin));
        registry.register(new PsychicDrain(plugin));
        registry.register(new MentalOverload(plugin));
        registry.register(new ThoughtEcho(plugin));
        registry.register(new NeuralStatic(plugin));
        registry.register(new MindWipe(plugin));
        registry.register(new ParanoiaZone(plugin));
        registry.register(new ConsciousnessRip(plugin));
        registry.register(new PsychicScream(plugin));
        registry.register(new Brainfreeze(plugin));
        registry.register(new EgoDeath(plugin));
    }

    // ================================================================
    // 1. CONFUSION PUSH — Random velocity pushes with purple particles
    // ================================================================
    public static class ConfusionPush extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public ConfusionPush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("confusion_push", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 15, 4.0, 200, 0, 180, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 8 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 36) {
                        double pushX = (RNG.nextDouble() - 0.5) * 0.6;
                        double pushZ = (RNG.nextDouble() - 0.5) * 0.6;
                        player.setVelocity(player.getVelocity().add(new Vector(pushX, 0.1, pushZ)));
                        DisplayBuilder.dustParticles(player.getLocation().clone().add(0, 1, 0), 4, 0.5, 200, 0, 180, 1.2f);
                    }
                }
            }
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 5; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * 6;
                    Location pLoc = center.clone().add(Math.cos(angle) * dist, 0.3 + RNG.nextDouble() * 2, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.3, 80, 0, 160, 1.0f);
                }
            }
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 1.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ConfusionPush(plugin); }
    }

    // ================================================================
    // 2. DISORIENTATION SPIN — Rotational velocity applied to players
    // ================================================================
    public static class DisorientationSpin extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public DisorientationSpin(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("disorientation_spin", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), 5.0, Particle.DUST,
                    16, new Particle.DustOptions(Color.fromRGB(200, 0, 180), 1.2f));
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 4 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 25) {
                        Vector toCenter = center.toVector().subtract(player.getLocation().toVector());
                        Vector tangent = new Vector(-toCenter.getZ(), 0.05, toCenter.getX()).normalize().multiply(0.3);
                        player.setVelocity(player.getVelocity().add(tangent));
                        DisplayBuilder.dustParticles(player.getLocation().clone().add(0, 1.5, 0), 3, 0.4, 200, 0, 180, 1.0f);
                    }
                }
            }
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = ticksAlive * 0.15 + i * (Math.PI / 2);
                    double r = 5.0;
                    Location pLoc = center.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.3, 80, 0, 160, 1.2f);
                }
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.3f, 0.8f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DisorientationSpin(plugin); }
    }

    // ================================================================
    // 3. FEAR PULSE — Expanding ring that pushes players outward
    // ================================================================
    public static class FearPulse extends EnvironmentalAttack {
        public FearPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fear_pulse", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(8.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 10, 1.0, 200, 0, 50, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            double radius = ticksAlive * 0.5;
            if (radius <= 8) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), radius, Particle.DUST,
                        (int) (radius * 3), new Particle.DustOptions(Color.fromRGB(200, 0, 50), 1.5f));
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (Math.abs(dist - radius) < 1.5) {
                        Vector push = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.8);
                        push.setY(0.3);
                        player.setVelocity(player.getVelocity().add(push));
                        player.damage(config.getImpactDamage() * 0.5);
                        player.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FearPulse(plugin); }
    }

    // ================================================================
    // 4. PSYCHIC DRAIN — Players in zone lose HP slowly, particles spiral inward
    // ================================================================
    public static class PsychicDrain extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public PsychicDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("psychic_drain", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), 6.0, Particle.DUST,
                    16, new Particle.DustOptions(Color.fromRGB(80, 0, 160), 1.2f));
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = ticksAlive * 0.12 + i * (Math.PI / 3);
                    double r = 6.0 * (1.0 - (i / 12.0));
                    Location pLoc = center.clone().add(Math.cos(angle) * r, 0.3 + i * 0.15, Math.sin(angle) * r);
                    DisplayBuilder.dustParticles(pLoc, 1, 0.2, 80, 0, 160, 1.2f);
                }
            }
            if (ticksAlive % 6 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 36) {
                        Location ploc = player.getLocation().clone().add(0, 1, 0);
                        Vector toCenter = center.toVector().subtract(ploc.toVector()).normalize();
                        DisplayBuilder.dustParticles(ploc.clone().add(toCenter.getX() * 0.5, toCenter.getY() * 0.5, toCenter.getZ() * 0.5),
                                2, 0.3, 200, 0, 180, 1.0f);
                    }
                }
            }
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PsychicDrain(plugin); }
    }

    // ================================================================
    // 5. MENTAL OVERLOAD — Rapid particle bursts + escalating damage
    // ================================================================
    public static class MentalOverload extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public MentalOverload(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mental_overload", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 15, 3.0, 240, 230, 255, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 0.6f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            config.setDamage(4.0 + (ticksAlive / 100.0) * 6.0);
            int particleCount = 3 + ticksAlive / 10;
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < particleCount; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 10;
                    double oy = RNG.nextDouble() * 4;
                    double oz = (RNG.nextDouble() - 0.5) * 10;
                    DisplayBuilder.dustParticles(center.clone().add(ox, oy, oz), 2, 0.3, 200, 0, 180, 1.5f);
                }
            }
            if (ticksAlive % 8 == 0) {
                float pitch = 1.0f + (ticksAlive / 100.0f) * 1.0f;
                DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, Math.min(2.0f, pitch));
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MentalOverload(plugin); }
    }

    // ================================================================
    // 6. THOUGHT ECHO — Delayed damage pulses repeating
    // ================================================================
    public static class ThoughtEcho extends EnvironmentalAttack {
        public ThoughtEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thought_echo", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 10, 2.0, 240, 230, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            // Echo pulses every 20 ticks
            if (ticksAlive % 20 == 0) {
                double radius = 2.0 + (ticksAlive % 60) / 10.0;
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), radius, Particle.DUST,
                        16, new Particle.DustOptions(Color.fromRGB(240, 230, 255), 1.5f));
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.0f + (ticksAlive % 60) * 0.02f);
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 3, 2.0, 80, 0, 160, 1.0f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ThoughtEcho(plugin); }
    }

    // ================================================================
    // 7. NEURAL STATIC — Erratic particles and random small damage ticks
    // ================================================================
    public static class NeuralStatic extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public NeuralStatic(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("neural_static", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 20; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 14;
                double oy = RNG.nextDouble() * 3;
                double oz = (RNG.nextDouble() - 0.5) * 14;
                DisplayBuilder.dustParticles(center.clone().add(ox, oy, oz), 2, 0.5, 240, 230, 255, 0.8f);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.8f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 14;
                    double oy = RNG.nextDouble() * 3;
                    double oz = (RNG.nextDouble() - 0.5) * 14;
                    if (RNG.nextBoolean()) {
                        DisplayBuilder.dustParticles(center.clone().add(ox, oy, oz), 1, 0.3, 240, 230, 255, 0.8f);
                    } else {
                        DisplayBuilder.dustParticles(center.clone().add(ox, oy, oz), 1, 0.3, 80, 0, 160, 0.8f);
                    }
                }
            }
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.3f, 1.5f + RNG.nextFloat());
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new NeuralStatic(plugin); }
    }

    // ================================================================
    // 8. MIND WIPE — Brief intense burst then lingering low damage zone
    // ================================================================
    public static class MindWipe extends EnvironmentalAttack {
        private boolean wiped = false;

        public MindWipe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mind_wipe", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 20, 4.0, 240, 230, 255, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (!wiped && ticksAlive >= 10) {
                wiped = true;
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 36) {
                        player.damage(8.0);
                        player.setNoDamageTicks(0);
                        DisplayBuilder.dustParticles(player.getLocation().clone().add(0, 1, 0), 10, 1.0, 240, 230, 255, 2.0f);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.5f);
            }
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 4, 5.0, 80, 0, 160, 0.8f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MindWipe(plugin); }
    }

    // ================================================================
    // 9. PARANOIA ZONE — Players take more damage the longer they stay
    // ================================================================
    public static class ParanoiaZone extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final Map<UUID, Integer> ticksInZone = new HashMap<>();

        public ParanoiaZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("paranoia_zone", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), 7.0, Particle.DUST,
                    20, new Particle.DustOptions(Color.fromRGB(200, 0, 50), 1.0f));
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 15 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 49) {
                        int ticks = ticksInZone.getOrDefault(player.getUniqueId(), 0) + 15;
                        ticksInZone.put(player.getUniqueId(), ticks);
                        double dmg = 2.0 + (ticks / 60.0) * 4.0;
                        player.damage(Math.min(10.0, dmg));
                        player.setNoDamageTicks(0);
                    } else {
                        ticksInZone.remove(player.getUniqueId());
                    }
                }
            }
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * 7;
                    DisplayBuilder.dustParticles(center.clone().add(Math.cos(angle) * dist, 0.3 + RNG.nextDouble(), Math.sin(angle) * dist),
                            1, 0.3, 200, 0, 50, 0.8f);
                }
            }
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            ticksInZone.clear();
            super.onCleanup();
        }

        @Override public AbstractAttack newInstance() { return new ParanoiaZone(plugin); }
    }

    // ================================================================
    // 10. CONSCIOUSNESS RIP — Pulls player toward center then burst damage
    // ================================================================
    public static class ConsciousnessRip extends EnvironmentalAttack {
        private boolean ripped = false;

        public ConsciousnessRip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("consciousness_rip", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 15, 5.0, 200, 0, 180, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive < 25) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 64) {
                        Vector pull = center.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.15);
                        player.setVelocity(player.getVelocity().add(pull));
                    }
                }
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 8, 4.0 - ticksAlive * 0.15, 80, 0, 160, 1.2f);
            }
            if (!ripped && ticksAlive >= 25) {
                ripped = true;
                triggerImpactDamage(center);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 25, 5.0, 200, 0, 50, 2.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ConsciousnessRip(plugin); }
    }

    // ================================================================
    // 11. PSYCHIC SCREAM — Large AoE burst of damage + knockback
    // ================================================================
    public static class PsychicScream extends EnvironmentalAttack {
        private boolean screamed = false;

        public PsychicScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("psychic_scream", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(10.0);
            config.setDurationTicks(15);
            config.setCooldownTicks(500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 10, 2.0, 200, 0, 180, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (!screamed && ticksAlive >= 5) {
                screamed = true;
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 100) {
                        player.damage(config.getImpactDamage());
                        player.setNoDamageTicks(0);
                        Vector push = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.2);
                        push.setY(0.5);
                        player.setVelocity(player.getVelocity().add(push));
                    }
                }
                for (double r = 1; r <= 10; r += 2) {
                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0), r, Particle.DUST,
                            (int) (r * 4), new Particle.DustOptions(Color.fromRGB(200, 0, 180), 2.0f));
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PsychicScream(plugin); }
    }

    // ================================================================
    // 12. BRAINFREEZE — Slows movement + damage zone
    // ================================================================
    public static class Brainfreeze extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public Brainfreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brainfreeze", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 15, 4.0, 240, 230, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.6f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 4 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 25) {
                        Vector vel = player.getVelocity();
                        player.setVelocity(new Vector(vel.getX() * 0.5, vel.getY(), vel.getZ() * 0.5));
                        DisplayBuilder.dustParticles(player.getLocation().clone().add(0, 1, 0), 3, 0.3, 240, 230, 255, 1.0f);
                    }
                }
            }
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 4; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 10;
                    double oz = (RNG.nextDouble() - 0.5) * 10;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 0.2, oz), 2, 0.3, 200, 0, 180, 1.0f);
                }
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_STEP, 0.3f, 2.0f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new Brainfreeze(plugin); }
    }

    // ================================================================
    // 13. EGO DEATH — All players in range get pulled to center, burst, scatter
    // ================================================================
    public static class EgoDeath extends EnvironmentalAttack {
        private boolean detonated = false;

        public EgoDeath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ego_death", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(50);
            config.setCooldownTicks(800);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 20, 6.0, 80, 0, 160, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive < 30) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 64) {
                        Vector pull = center.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.2);
                        player.setVelocity(player.getVelocity().add(pull));
                    }
                }
                double shrink = 6.0 - (ticksAlive / 30.0) * 5.0;
                DisplayBuilder.particleRing(center.clone().add(0, 1, 0), shrink, Particle.DUST,
                        16, new Particle.DustOptions(Color.fromRGB(200, 0, 180), 1.5f));
            }
            if (!detonated && ticksAlive >= 30) {
                detonated = true;
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 36) {
                        player.damage(config.getImpactDamage());
                        player.setNoDamageTicks(0);
                        Vector scatter = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.5);
                        scatter.setY(0.8);
                        player.setVelocity(scatter);
                    }
                }
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 30, 6.0, 200, 0, 50, 2.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new EgoDeath(plugin); }
    }
}
