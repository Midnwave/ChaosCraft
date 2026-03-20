package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class IceQuakes {
    private IceQuakes() {}

    // ── Ice color palette ──
    private static final int ICE_R = 100, ICE_G = 180, ICE_B = 255;       // Standard ice blue
    private static final int FROST_R = 220, FROST_G = 240, FROST_B = 255;  // Bright frost white-blue
    private static final int DEEP_R = 30, DEEP_G = 80, DEEP_B = 160;       // Deep glacial blue

    private static Particle.DustOptions iceDust(float size) {
        return new Particle.DustOptions(Color.fromRGB(ICE_R, ICE_G, ICE_B), size);
    }

    private static Particle.DustOptions frostDust(float size) {
        return new Particle.DustOptions(Color.fromRGB(FROST_R, FROST_G, FROST_B), size);
    }

    private static Particle.DustOptions deepDust(float size) {
        return new Particle.DustOptions(Color.fromRGB(DEEP_R, DEEP_G, DEEP_B), size);
    }

    /** Damage all non-exempt survival players within radius of a location. */
    private static void damageNearby(AbstractAttack attack, Location loc, double radius, double damage) {
        if (loc == null || loc.getWorld() == null) return;
        double r2 = radius * radius;
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            if (p.isInvulnerable()) continue;
            if (p.hasPermission("chaoscraft.mode.exempt")) continue;
            if (p.getLocation().distanceSquared(loc) <= r2) {
                p.damage(damage);
                p.setNoDamageTicks(0);
            }
        }
    }

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FrostQuake(plugin));
        registry.register(new GlacialShift(plugin));
        registry.register(new IceCrackSpread(plugin));
        registry.register(new PermafrostSplit(plugin));
        registry.register(new AvalancheRumble(plugin));
        registry.register(new FrostTremor(plugin));
        registry.register(new GlacialCollapse(plugin));
        registry.register(new IcePlateTectonics(plugin));
        registry.register(new SeismicFrost(plugin));
        registry.register(new CryogenicBurst(plugin));
        registry.register(new FrostfaultLine(plugin));
        registry.register(new IceShatter(plugin));
        registry.register(new SubglacialRumble(plugin));
    }

    // ════════════════════════════════════════════════════════════════════
    // 1. FrostQuake — Expanding ring of damage from epicenter
    // ════════════════════════════════════════════════════════════════════
    public static class FrostQuake extends EnvironmentalAttack {
        private Location center;
        private double currentRadius;

        public FrostQuake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_quake", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.currentRadius = 0.5;

            // Initial crack sound
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 2.0f, 0.4f);

            // Epicenter burst particles
            DisplayBuilder.dustParticles(center, 40, 1.0, FROST_R, FROST_G, FROST_B, 1.8f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double maxRadius = config.getDamageRadius();
            // Expand ring outward over the duration; reset and repeat
            currentRadius += maxRadius / 60.0; // Full expansion every 60 ticks (3 seconds)
            if (currentRadius > maxRadius) {
                currentRadius = 0.5;
                // New crack sound each cycle
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.6f);
            }

            // Particle ring at current radius
            int ringPoints = Math.max(16, (int) (currentRadius * 8));
            DisplayBuilder.particleRing(center, currentRadius, Particle.DUST, ringPoints, iceDust(1.4f));

            // Smaller inner ring for depth effect
            if (currentRadius > 1.5) {
                DisplayBuilder.particleRing(center, currentRadius - 1.0, Particle.DUST, ringPoints / 2, deepDust(1.0f));
            }

            // Frost ground particles along the expanding edge
            if (tick % 3 == 0) {
                DisplayBuilder.dustParticles(center, 8, currentRadius * 0.5, FROST_R, FROST_G, FROST_B, 0.8f);
            }

            // Crack sounds every 20 ticks
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.5f + (float) (currentRadius / maxRadius) * 0.5f);
            }

            // Damage players within the expanding ring band (1.5 block band width)
            if (tick % 10 == 0) {
                double bandInner = Math.max(0, currentRadius - 1.5);
                for (Player p : center.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.hasPermission("chaoscraft.mode.exempt")) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist >= bandInner && dist <= currentRadius) {
                        p.damage(config.getDamage() * 0.3);
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostQuake(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 2. GlacialShift — Damage zone teleports to random positions
    // ════════════════════════════════════════════════════════════════════
    public static class GlacialShift extends EnvironmentalAttack {
        private Location center;
        private Location activeZone;
        private final double ZONE_RADIUS = 3.0;

        public GlacialShift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_shift", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.activeZone = center.clone();

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.7f);
            DisplayBuilder.dustParticles(center, 30, 2.0, ICE_R, ICE_G, ICE_B, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Teleport zone to new random position every 15 ticks
            if (tick % 15 == 0) {
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                double offsetX = rng.nextDouble(-10.0, 10.0);
                double offsetZ = rng.nextDouble(-10.0, 10.0);
                activeZone = center.clone().add(offsetX, 0, offsetZ);

                // Shift sound
                DisplayBuilder.playSound(activeZone, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.4f);
                DisplayBuilder.playSound(activeZone, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);

                // Arrival burst
                DisplayBuilder.dustParticles(activeZone, 35, 1.5, FROST_R, FROST_G, FROST_B, 1.6f);
            }

            // Active zone particles — ring outline showing danger area
            DisplayBuilder.particleRing(activeZone, ZONE_RADIUS, Particle.DUST, 24, iceDust(1.2f));

            // Interior frost particles
            if (tick % 2 == 0) {
                DisplayBuilder.dustParticles(activeZone, 10, ZONE_RADIUS * 0.6, DEEP_R, DEEP_G, DEEP_B, 1.0f);
            }

            // Damage players inside the active zone every 10 ticks
            if (tick % 10 == 0) {
                damageNearby(this, activeZone, ZONE_RADIUS, config.getDamage() * 0.35);
            }
        }

        @Override public AbstractAttack newInstance() { return new GlacialShift(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 3. IceCrackSpread — 4 damage lines spreading from center
    // ════════════════════════════════════════════════════════════════════
    public static class IceCrackSpread extends EnvironmentalAttack {
        private Location center;
        private double[] angles;
        private double lineLength;

        public IceCrackSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_crack_spread", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.lineLength = 0.5;

            // 4 random directions
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            angles = new double[4];
            double baseAngle = rng.nextDouble(0, Math.PI / 2);
            for (int i = 0; i < 4; i++) {
                angles[i] = baseAngle + (Math.PI / 2.0) * i + rng.nextDouble(-0.3, 0.3);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
            DisplayBuilder.dustParticles(center, 50, 0.5, FROST_R, FROST_G, FROST_B, 2.0f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double maxLength = config.getDamageRadius();
            // Grow lines outward
            lineLength += maxLength / 80.0; // Full extension over 80 ticks (4 seconds)
            if (lineLength > maxLength) lineLength = maxLength;

            Particle.DustOptions dust = iceDust(1.3f);
            Particle.DustOptions deeperDust = deepDust(1.0f);

            for (double angle : angles) {
                double endX = Math.cos(angle) * lineLength;
                double endZ = Math.sin(angle) * lineLength;
                Location end = center.clone().add(endX, 0, endZ);

                // Draw the crack line
                DisplayBuilder.particleLine(center, end, Particle.DUST, 3, dust);

                // Tip particles — bright frost burst at the leading edge
                DisplayBuilder.dustParticles(end, 6, 0.4, FROST_R, FROST_G, FROST_B, 1.5f);

                // Deeper color along the line every other tick
                if (tick % 2 == 0) {
                    Location mid = center.clone().add(endX * 0.5, 0, endZ * 0.5);
                    DisplayBuilder.dustParticles(mid, 4, 0.3, DEEP_R, DEEP_G, DEEP_B, 0.9f);
                }
            }

            // Crack sound as lines grow
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.6f);
            }

            // Damage players near any of the 4 lines every 10 ticks
            if (tick % 10 == 0) {
                double dmg = config.getDamage() * 0.25;
                int segments = Math.max(3, (int) (lineLength * 2));
                for (double angle : angles) {
                    for (int s = 0; s <= segments; s++) {
                        double t = (double) s / segments;
                        double px = Math.cos(angle) * lineLength * t;
                        double pz = Math.sin(angle) * lineLength * t;
                        Location point = center.clone().add(px, 0, pz);
                        damageNearby(this, point, 1.5, dmg);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IceCrackSpread(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 4. PermafrostSplit — Damage line splitting area in half
    // ════════════════════════════════════════════════════════════════════
    public static class PermafrostSplit extends EnvironmentalAttack {
        private Location center;
        private double splitAngle;
        private double splitLength;

        public PermafrostSplit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_split", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.splitAngle = ThreadLocalRandom.current().nextDouble(0, Math.PI);
            this.splitLength = 0;

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.4f);
            DisplayBuilder.dustParticles(center, 40, 0.3, FROST_R, FROST_G, FROST_B, 2.0f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double maxLen = config.getDamageRadius();

            // Extend the split line outward in both directions from center
            splitLength += maxLen / 40.0; // Full in 2 seconds
            if (splitLength > maxLen) splitLength = maxLen;

            // Two endpoints in opposite directions
            double dx = Math.cos(splitAngle) * splitLength;
            double dz = Math.sin(splitAngle) * splitLength;
            Location endA = center.clone().add(dx, 0, dz);
            Location endB = center.clone().add(-dx, 0, -dz);

            // Draw the split line
            DisplayBuilder.particleLine(endB, endA, Particle.DUST, 4, iceDust(1.5f));

            // Deep blue underlayer
            if (tick % 2 == 0) {
                DisplayBuilder.particleLine(endB, endA, Particle.DUST, 2, deepDust(1.8f));
            }

            // Frost spray at tips
            DisplayBuilder.dustParticles(endA, 8, 0.5, FROST_R, FROST_G, FROST_B, 1.3f);
            DisplayBuilder.dustParticles(endB, 8, 0.5, FROST_R, FROST_G, FROST_B, 1.3f);

            // Rising frost particles along the line
            if (tick % 4 == 0) {
                int count = (int) (splitLength * 2);
                for (int i = 0; i <= count; i++) {
                    double t = (double) i / Math.max(1, count) * 2.0 - 1.0;
                    Location point = center.clone().add(dx * t, 0.5, dz * t);
                    DisplayBuilder.dustParticles(point, 2, 0.2, ICE_R, ICE_G, ICE_B, 0.7f);
                }
            }

            // Crack sounds
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.2f, 0.5f);
            }

            // Damage along the line every 10 ticks
            if (tick % 10 == 0) {
                double dmg = config.getDamage() * 0.3;
                int segments = Math.max(4, (int) (splitLength * 2));
                for (int s = 0; s <= segments; s++) {
                    double t = (double) s / segments * 2.0 - 1.0;
                    Location point = center.clone().add(dx * t, 0, dz * t);
                    damageNearby(this, point, 1.5, dmg);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new PermafrostSplit(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 5. AvalancheRumble — Escalating damage (starts low, increases every 20 ticks)
    // ════════════════════════════════════════════════════════════════════
    public static class AvalancheRumble extends EnvironmentalAttack {
        private Location center;
        private int escalationLevel;

        public AvalancheRumble(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("avalanche_rumble", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.escalationLevel = 0;

            DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 2.0f, 0.3f);
            DisplayBuilder.dustParticles(center, 30, 3.0, DEEP_R, DEEP_G, DEEP_B, 1.2f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Escalate every 20 ticks
            if (tick % 20 == 0) {
                escalationLevel++;

                // Rumble sound escalates in volume and pitch
                float volume = Math.min(2.0f, 0.5f + escalationLevel * 0.2f);
                float pitch = 0.3f + escalationLevel * 0.05f;
                DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_BREAK, volume, pitch);
                DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, volume * 0.8f, pitch);

                // Bigger burst each escalation
                int burstCount = 15 + escalationLevel * 5;
                float burstSpread = 2.0f + escalationLevel * 0.5f;
                DisplayBuilder.dustParticles(center, burstCount, burstSpread, ICE_R, ICE_G, ICE_B, 1.3f);
            }

            double radius = config.getDamageRadius();

            // Continuous shaking particles — intensity scales with escalation
            int particleCount = 5 + escalationLevel * 3;
            double spread = radius * 0.6;
            DisplayBuilder.dustParticles(center, particleCount, spread, DEEP_R, DEEP_G, DEEP_B, 1.0f);

            // Ground-level frost spray
            if (tick % 3 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), particleCount / 2, spread, FROST_R, FROST_G, FROST_B, 0.8f);
            }

            // Ring showing danger area — pulses larger with escalation
            if (tick % 5 == 0) {
                double ringRadius = radius * (0.5 + escalationLevel * 0.05);
                ringRadius = Math.min(ringRadius, radius);
                DisplayBuilder.particleRing(center, ringRadius, Particle.DUST, 20, iceDust(1.0f));
            }

            // Escalating damage every 10 ticks
            if (tick % 10 == 0) {
                // Damage scales from 10% to ~100% of base over the duration
                double dmgMultiplier = Math.min(1.0, 0.1 + escalationLevel * 0.09);
                damageNearby(this, center, radius, config.getDamage() * dmgMultiplier);
            }
        }

        @Override public AbstractAttack newInstance() { return new AvalancheRumble(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 6. FrostTremor — Light damage pulse every 20 ticks with particle ring
    // ════════════════════════════════════════════════════════════════════
    public static class FrostTremor extends EnvironmentalAttack {
        private Location center;

        public FrostTremor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_tremor", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.5f, 0.5f);
            DisplayBuilder.dustParticles(center, 25, 1.5, ICE_R, ICE_G, ICE_B, 1.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double radius = config.getDamageRadius();

            // Ambient tremor particles — light, scattered
            if (tick % 3 == 0) {
                DisplayBuilder.dustParticles(center, 6, radius * 0.4, FROST_R, FROST_G, FROST_B, 0.7f);
            }

            // Pulse every 20 ticks — ring + damage + crack sound
            if (tick % 20 == 0) {
                // Crack sound
                DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.2f, 0.6f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.8f);

                // Expanding pulse ring
                DisplayBuilder.particleRing(center, radius, Particle.DUST, 32, iceDust(1.5f));
                DisplayBuilder.particleRing(center, radius * 0.6, Particle.DUST, 20, deepDust(1.2f));

                // Center burst
                DisplayBuilder.dustParticles(center, 20, 1.0, FROST_R, FROST_G, FROST_B, 1.4f);

                // Light damage pulse
                damageNearby(this, center, radius, config.getDamage() * 0.2);
            }

            // Subtle ground frost between pulses
            if (tick % 8 == 0) {
                DisplayBuilder.particleRing(center, radius * 0.3, Particle.DUST, 10, frostDust(0.6f));
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostTremor(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 7. GlacialCollapse — High initial damage, decreasing over time
    // ════════════════════════════════════════════════════════════════════
    public static class GlacialCollapse extends EnvironmentalAttack {
        private Location center;

        public GlacialCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_collapse", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            // Massive initial collapse sound
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.2f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);

            // Massive initial particle burst
            double radius = config.getDamageRadius();
            DisplayBuilder.dustParticles(center, 80, radius * 0.7, FROST_R, FROST_G, FROST_B, 2.0f);
            DisplayBuilder.dustParticles(center, 60, radius * 0.5, ICE_R, ICE_G, ICE_B, 1.8f);
            DisplayBuilder.dustParticles(center, 40, radius * 0.3, DEEP_R, DEEP_G, DEEP_B, 2.2f);
            DisplayBuilder.particleRing(center, radius, Particle.DUST, 40, iceDust(2.0f));

            // Heavy initial damage
            damageNearby(this, center, radius, config.getDamage());
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            int duration = config.getDurationTicks();
            double radius = config.getDamageRadius();

            // Intensity fades over time (1.0 at start -> ~0.1 at end)
            double intensity = Math.max(0.1, 1.0 - ((double) tick / duration));

            // Particle count and size decrease with intensity
            int count = (int) (30 * intensity);
            float spread = (float) (radius * 0.5 * intensity);
            float size = (float) (1.5 * intensity + 0.5);

            DisplayBuilder.dustParticles(center, count, spread, ICE_R, ICE_G, ICE_B, size);

            // Fading ring
            if (tick % 5 == 0) {
                double ringRadius = radius * intensity;
                int ringPoints = Math.max(10, (int) (24 * intensity));
                DisplayBuilder.particleRing(center, ringRadius, Particle.DUST, ringPoints, deepDust(size * 0.8f));
            }

            // Residual cracking sounds — less frequent over time
            int soundInterval = Math.max(10, (int) (40 * (1.0 - intensity)));
            if (tick % soundInterval == 0) {
                float volume = (float) (1.5 * intensity);
                DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, volume, 0.5f);
            }

            // Decreasing damage every 10 ticks
            if (tick % 10 == 0) {
                double dmg = config.getDamage() * intensity * 0.4;
                if (dmg > 0.5) {
                    damageNearby(this, center, radius * intensity + 2.0, dmg);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new GlacialCollapse(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 8. IcePlateTectonics — Two damage zones moving in opposite directions
    // ════════════════════════════════════════════════════════════════════
    public static class IcePlateTectonics extends EnvironmentalAttack {
        private Location center;
        private double moveAngle;
        private double offset;
        private final double PLATE_RADIUS = 3.0;

        public IcePlateTectonics(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_plate_tectonics", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.moveAngle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
            this.offset = 0;

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 2.0f, 0.3f);
            DisplayBuilder.dustParticles(center, 40, 2.0, ICE_R, ICE_G, ICE_B, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double maxOffset = config.getDamageRadius();

            // Plates drift apart, then come back together
            offset += 0.06;
            double currentOffset = Math.sin(offset) * maxOffset * 0.5;

            double dx = Math.cos(moveAngle) * currentOffset;
            double dz = Math.sin(moveAngle) * currentOffset;

            Location plateA = center.clone().add(dx, 0, dz);
            Location plateB = center.clone().add(-dx, 0, -dz);

            // Plate A particles (ice blue)
            DisplayBuilder.particleRing(plateA, PLATE_RADIUS, Particle.DUST, 20, iceDust(1.3f));
            DisplayBuilder.dustParticles(plateA, 8, PLATE_RADIUS * 0.4, ICE_R, ICE_G, ICE_B, 1.0f);

            // Plate B particles (deep blue)
            DisplayBuilder.particleRing(plateB, PLATE_RADIUS, Particle.DUST, 20, deepDust(1.3f));
            DisplayBuilder.dustParticles(plateB, 8, PLATE_RADIUS * 0.4, DEEP_R, DEEP_G, DEEP_B, 1.0f);

            // Frost line between the two plates — the "fault line"
            if (tick % 2 == 0) {
                DisplayBuilder.particleLine(plateA, plateB, Particle.DUST, 2, frostDust(0.8f));
            }

            // Grinding sound when plates move
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 1.2f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 0.8f, 0.5f);
            }

            // Damage at both plate locations every 10 ticks
            if (tick % 10 == 0) {
                double dmg = config.getDamage() * 0.25;
                damageNearby(this, plateA, PLATE_RADIUS, dmg);
                damageNearby(this, plateB, PLATE_RADIUS, dmg);
            }

            // Extra damage along the gap/fault between plates every 20 ticks
            if (tick % 20 == 0) {
                int segments = 5;
                for (int s = 0; s <= segments; s++) {
                    double t = (double) s / segments;
                    Location fault = plateA.clone().add(
                            (plateB.getX() - plateA.getX()) * t,
                            0,
                            (plateB.getZ() - plateA.getZ()) * t);
                    damageNearby(this, fault, 1.5, config.getDamage() * 0.15);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IcePlateTectonics(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 9. SeismicFrost — Bouncing pulse: expand, contract, re-expand
    // ════════════════════════════════════════════════════════════════════
    public static class SeismicFrost extends EnvironmentalAttack {
        private Location center;
        private double pulsePhase;

        public SeismicFrost(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("seismic_frost", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.pulsePhase = 0;

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
            DisplayBuilder.dustParticles(center, 35, 1.0, ICE_R, ICE_G, ICE_B, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double maxRadius = config.getDamageRadius();

            // Bouncing sine wave: expand -> contract -> expand
            pulsePhase += 0.08; // ~78 ticks per full cycle
            double currentRadius = (Math.sin(pulsePhase) * 0.5 + 0.5) * maxRadius;
            currentRadius = Math.max(1.0, currentRadius); // Minimum 1 block

            // Ring at current pulse radius
            int ringPoints = Math.max(12, (int) (currentRadius * 6));
            DisplayBuilder.particleRing(center, currentRadius, Particle.DUST, ringPoints, iceDust(1.4f));

            // Inner ring half the size
            if (currentRadius > 2.0) {
                DisplayBuilder.particleRing(center, currentRadius * 0.5, Particle.DUST, ringPoints / 2, deepDust(1.0f));
            }

            // Center core particles
            if (tick % 2 == 0) {
                DisplayBuilder.dustParticles(center, 6, 0.5, FROST_R, FROST_G, FROST_B, 1.2f);
            }

            // Sound at peaks and troughs of the wave
            double sineVal = Math.sin(pulsePhase);
            if (Math.abs(sineVal) > 0.98) {
                if (sineVal > 0) {
                    // Peak — expansion maximum
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.7f);
                } else {
                    // Trough — contraction
                    DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.9f);
                }
            }

            // Damage within current pulse radius every 8 ticks
            if (tick % 8 == 0) {
                damageNearby(this, center, currentRadius, config.getDamage() * 0.2);
            }
        }

        @Override public AbstractAttack newInstance() { return new SeismicFrost(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 10. CryogenicBurst — 60 tick warmup (particles inward), then massive burst
    // ════════════════════════════════════════════════════════════════════
    public static class CryogenicBurst extends EnvironmentalAttack {
        private Location center;
        private boolean hasBurst;
        private static final int WARMUP_TICKS = 60;

        public CryogenicBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryogenic_burst", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.hasBurst = false;

            // Warning sound — building up
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.3f);
            DisplayBuilder.dustParticles(center, 15, 0.5, DEEP_R, DEEP_G, DEEP_B, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double maxRadius = config.getDamageRadius();

            if (tick < WARMUP_TICKS) {
                // ── Warmup phase: particles gather inward toward center ──
                double progress = (double) tick / WARMUP_TICKS; // 0.0 to 1.0
                double gatherRadius = maxRadius * (1.0 - progress); // Shrinking circle

                // Inward-gathering ring
                int ringPoints = 24;
                DisplayBuilder.particleRing(center, gatherRadius, Particle.DUST, ringPoints, iceDust(1.0f + (float) progress));

                // Particles along the gathering radius
                if (tick % 2 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
                        double r = gatherRadius * (0.8 + ThreadLocalRandom.current().nextDouble(0.4));
                        Location p = center.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
                        DisplayBuilder.dustParticles(p, 3, 0.2, FROST_R, FROST_G, FROST_B, 0.8f);
                    }
                }

                // Building core glow
                float coreSize = 0.5f + (float) progress * 1.5f;
                DisplayBuilder.dustParticles(center, (int) (5 + progress * 15), 0.3 + progress * 0.5, ICE_R, ICE_G, ICE_B, coreSize);

                // Escalating warning sound
                if (tick % 15 == 0) {
                    float warmupPitch = 0.4f + (float) progress * 1.0f;
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, warmupPitch);
                }

            } else if (!hasBurst) {
                // ── BURST! ──
                hasBurst = true;

                // Massive sound
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);

                // Massive particle explosion
                DisplayBuilder.dustParticles(center, 120, maxRadius * 0.8, FROST_R, FROST_G, FROST_B, 2.5f);
                DisplayBuilder.dustParticles(center, 80, maxRadius * 0.6, ICE_R, ICE_G, ICE_B, 2.0f);
                DisplayBuilder.dustParticles(center, 60, maxRadius * 0.4, DEEP_R, DEEP_G, DEEP_B, 2.2f);

                // Concentric burst rings
                for (double r = 1; r <= maxRadius; r += 1.5) {
                    DisplayBuilder.particleRing(center, r, Particle.DUST, (int) (r * 6), iceDust(1.8f));
                }

                // Heavy damage on burst
                damageNearby(this, center, maxRadius, config.getDamage());

            } else {
                // ── Post-burst: fading residual frost ──
                int postTick = tick - WARMUP_TICKS;
                double fade = Math.max(0.1, 1.0 - (double) postTick / (config.getDurationTicks() - WARMUP_TICKS));

                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(center, (int) (15 * fade), maxRadius * 0.4 * fade,
                            ICE_R, ICE_G, ICE_B, (float) (1.2 * fade));
                }

                // Residual ring
                if (tick % 10 == 0) {
                    DisplayBuilder.particleRing(center, maxRadius * fade, Particle.DUST, 16, deepDust((float) (0.8 * fade)));
                }

                // Residual damage (light)
                if (tick % 20 == 0 && fade > 0.2) {
                    damageNearby(this, center, maxRadius * fade, config.getDamage() * 0.15 * fade);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CryogenicBurst(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 11. FrostfaultLine — Permanent damage line for full duration
    // ════════════════════════════════════════════════════════════════════
    public static class FrostfaultLine extends EnvironmentalAttack {
        private Location center;
        private Location lineStart;
        private Location lineEnd;
        private double faultAngle;

        public FrostfaultLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frostfault_line", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.faultAngle = ThreadLocalRandom.current().nextDouble(0, Math.PI);

            double halfLen = config.getDamageRadius();
            double dx = Math.cos(faultAngle) * halfLen;
            double dz = Math.sin(faultAngle) * halfLen;
            lineStart = center.clone().add(-dx, 0, -dz);
            lineEnd = center.clone().add(dx, 0, dz);

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.4f);

            // Initial crack appearance — full line immediately
            DisplayBuilder.particleLine(lineStart, lineEnd, Particle.DUST, 4, iceDust(1.8f));
            DisplayBuilder.dustParticles(center, 40, 1.0, FROST_R, FROST_G, FROST_B, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Persistent frost line — always visible
            DisplayBuilder.particleLine(lineStart, lineEnd, Particle.DUST, 3, iceDust(1.3f));

            // Alternating deep and frost particles for shimmer
            Particle.DustOptions altDust = (tick % 4 < 2) ? deepDust(1.1f) : frostDust(1.0f);
            DisplayBuilder.particleLine(lineStart, lineEnd, Particle.DUST, 2, altDust);

            // Rising frost along the fault
            if (tick % 3 == 0) {
                double halfLen = config.getDamageRadius();
                int risers = 6;
                for (int i = 0; i < risers; i++) {
                    double t = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
                    double dx = Math.cos(faultAngle) * halfLen * t;
                    double dz = Math.sin(faultAngle) * halfLen * t;
                    Location risePoint = center.clone().add(dx, 0.3 + ThreadLocalRandom.current().nextDouble(0.5), dz);
                    DisplayBuilder.dustParticles(risePoint, 3, 0.2, FROST_R, FROST_G, FROST_B, 0.7f);
                }
            }

            // Periodic crack sounds
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.6f);
            }

            // Continuous damage along the line every 10 ticks
            if (tick % 10 == 0) {
                double halfLen = config.getDamageRadius();
                double dmg = config.getDamage() * 0.2;
                int segments = Math.max(5, (int) (halfLen * 2));
                for (int s = 0; s <= segments; s++) {
                    double t = (double) s / segments * 2.0 - 1.0;
                    double dx = Math.cos(faultAngle) * halfLen * t;
                    double dz = Math.sin(faultAngle) * halfLen * t;
                    Location point = center.clone().add(dx, 0, dz);
                    damageNearby(this, point, 1.5, dmg);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostfaultLine(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 12. IceShatter — Random burst damage at 3-5 spots every 20 ticks
    // ════════════════════════════════════════════════════════════════════
    public static class IceShatter extends EnvironmentalAttack {
        private Location center;

        public IceShatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_shatter", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
            DisplayBuilder.dustParticles(center, 30, 2.0, ICE_R, ICE_G, ICE_B, 1.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double radius = config.getDamageRadius();

            // Ambient frost particles around the area
            if (tick % 4 == 0) {
                DisplayBuilder.dustParticles(center, 5, radius * 0.5, FROST_R, FROST_G, FROST_B, 0.6f);
            }

            // Every 20 ticks: 3-5 random shatter spots
            if (tick % 20 == 0) {
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                int spotCount = rng.nextInt(3, 6); // 3 to 5

                for (int i = 0; i < spotCount; i++) {
                    double ox = rng.nextDouble(-radius, radius);
                    double oz = rng.nextDouble(-radius, radius);
                    Location spot = center.clone().add(ox, 0, oz);

                    // Glass break sound at each spot
                    DisplayBuilder.playSound(spot, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f + rng.nextFloat() * 0.6f);

                    // Shatter burst — bright explosion of ice particles
                    DisplayBuilder.dustParticles(spot, 25, 1.0, FROST_R, FROST_G, FROST_B, 1.6f);
                    DisplayBuilder.dustParticles(spot, 15, 0.8, ICE_R, ICE_G, ICE_B, 1.3f);

                    // Small ring at impact
                    DisplayBuilder.particleRing(spot, 2.0, Particle.DUST, 12, deepDust(1.2f));

                    // Shatter fragments — scattered outward particles
                    for (int f = 0; f < 6; f++) {
                        double fx = rng.nextDouble(-1.5, 1.5);
                        double fz = rng.nextDouble(-1.5, 1.5);
                        DisplayBuilder.dustParticles(spot.clone().add(fx, 0.3, fz), 3, 0.1, FROST_R, FROST_G, FROST_B, 0.9f);
                    }

                    // Damage at each shatter spot
                    damageNearby(this, spot, 2.5, config.getDamage() * 0.3);
                }
            }

            // Residual sparkle at previous spots (light ambient)
            if (tick % 6 == 0) {
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                double ox = rng.nextDouble(-radius, radius);
                double oz = rng.nextDouble(-radius, radius);
                DisplayBuilder.dustParticles(center.clone().add(ox, 0.1, oz), 2, 0.1, FROST_R, FROST_G, FROST_B, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new IceShatter(plugin); }
    }

    // ════════════════════════════════════════════════════════════════════
    // 13. SubglacialRumble — Deep bass every 40 ticks + escalating area damage
    // ════════════════════════════════════════════════════════════════════
    public static class SubglacialRumble extends EnvironmentalAttack {
        private Location center;
        private int rumbleCount;

        public SubglacialRumble(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("subglacial_rumble", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.rumbleCount = 0;

            // Deep initial rumble from below
            DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 2.0f, 0.1f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.5f, 0.5f);
            DisplayBuilder.dustParticles(center.clone().add(0, -0.3, 0), 25, 2.0, DEEP_R, DEEP_G, DEEP_B, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            double radius = config.getDamageRadius();

            // Subtle ground-level fog particles (constant)
            if (tick % 3 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.1, 0), 6, radius * 0.5, DEEP_R, DEEP_G, DEEP_B, 0.8f);
            }

            // Underground glow — particles below ground level
            if (tick % 5 == 0) {
                Location below = center.clone().add(0, -0.5, 0);
                DisplayBuilder.dustParticles(below, 10, radius * 0.4, ICE_R, ICE_G, ICE_B, 1.0f);
            }

            // Deep bass rumble every 40 ticks
            if (tick % 40 == 0) {
                rumbleCount++;

                // Deep bass sounds — warden heartbeat + stone breaking for subglacial feel
                float bassVolume = Math.min(2.0f, 1.0f + rumbleCount * 0.15f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, bassVolume, 0.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_BREAK, bassVolume * 0.8f, 0.1f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, bassVolume * 0.6f, 0.2f);

                // Rumble burst from below — particles erupt upward
                int burstCount = 20 + rumbleCount * 8;
                DisplayBuilder.dustParticles(center, burstCount, radius * 0.5, DEEP_R, DEEP_G, DEEP_B, 1.5f);
                DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), burstCount / 2, radius * 0.3, ICE_R, ICE_G, ICE_B, 1.2f);

                // Concentric rings erupting from ground
                DisplayBuilder.particleRing(center, radius * 0.3, Particle.DUST, 16, deepDust(1.4f));
                DisplayBuilder.particleRing(center, radius * 0.6, Particle.DUST, 24, iceDust(1.2f));
            }

            // Escalating area damage every 10 ticks — damage increases with each rumble
            if (tick % 10 == 0) {
                // Damage scales: 10% at start → ~100% after many rumbles
                double dmgMultiplier = Math.min(1.0, 0.1 + rumbleCount * 0.12);
                double dmg = config.getDamage() * dmgMultiplier * 0.3;
                if (dmg > 0.5) {
                    damageNearby(this, center, radius, dmg);
                }
            }

            // Upward-rising particles simulating pressure from below every 6 ticks
            if (tick % 6 == 0) {
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                for (int i = 0; i < 3; i++) {
                    double ox = rng.nextDouble(-radius * 0.5, radius * 0.5);
                    double oz = rng.nextDouble(-radius * 0.5, radius * 0.5);
                    Location risePoint = center.clone().add(ox, -0.2 + rng.nextDouble(0.8), oz);
                    DisplayBuilder.dustParticles(risePoint, 3, 0.15, FROST_R, FROST_G, FROST_B, 0.7f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SubglacialRumble(plugin); }
    }
}
