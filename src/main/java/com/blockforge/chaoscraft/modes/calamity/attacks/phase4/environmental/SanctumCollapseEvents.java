package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.environmental;

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
 * Phase 4D Environmental -- GROUP 4: SANCTUM COLLAPSE EVENTS
 * Attacks #31-40: All four fracture systems fire at once, dimensional echoes,
 * void scar bleeds, Voidmaw resonance, and the Dragon's passive presence attacks.
 *
 * Design notes:
 * - No status effects
 * - Dragon palette: magenta (180,0,200), cyan (0,200,255), purple (128,0,255)
 * - Void palette: deep indigo (10,0,40), obsidian purple (50,0,80)
 * - Materials: OBSIDIAN, CRYING_OBSIDIAN, POLISHED_BLACKSTONE, END_STONE, PURPUR_BLOCK
 * - Damage range: 6.0-14.0 HP
 */
public final class SanctumCollapseEvents {

    private SanctumCollapseEvents() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidFractureStorm(plugin));
        registry.register(new DimensionalEcho(plugin));
        registry.register(new VoidScarBleed(plugin));
        registry.register(new VoidmawResonancePulse(plugin));
        registry.register(new AuraDensitySurge(plugin));
        registry.register(new LightFlickerCascade(plugin));
        registry.register(new DragonBreathGroundSaturation(plugin));
        registry.register(new VoidEmperorGaze(plugin));
        registry.register(new SanctumResonancePulse(plugin));
        registry.register(new DragonThermalUpdraft(plugin));
    }

    // =========================================================================
    // 31. VOID FRACTURE STORM -- all four fracture channels open simultaneously
    // =========================================================================
    public static class VoidFractureStorm extends EnvironmentalAttack {

        private final double[] channelAngles = {0, Math.PI / 2, Math.PI, 3 * Math.PI / 2};
        private final List<BlockDisplayHandle> centerCluster = new ArrayList<>();
        private boolean storming = false;

        public VoidFractureStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_fracture_storm", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(10.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // All four fractures glow
            for (double angle : channelAngles) {
                for (int i = -8; i <= 8; i++) {
                    Location pt = center.clone().add(Math.cos(angle) * i, 0.2, Math.sin(angle) * i);
                    w.spawnParticle(Particle.REVERSE_PORTAL, pt, 3, 0.2, 0.3, 0.2, 0.01);
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks -- cross-shaped glow pulses
            if (ticksAlive <= 60) {
                if (ticksAlive % 5 == 0) {
                    for (double angle : channelAngles) {
                        for (int i = -8; i <= 8; i += 2) {
                            Location pt = center.clone().add(Math.cos(angle) * i, 0.3, Math.sin(angle) * i);
                            w.spawnParticle(Particle.REVERSE_PORTAL, pt, 5, 0.3, 0.5, 0.3, 0.01);
                        }
                    }
                }
                if (ticksAlive == 55) {
                    // Silence before storm (conceptual)
                }
                return;
            }

            // Storm fires
            if (!storming) {
                storming = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.2f);

                // Center intersection cluster
                for (int i = 0; i < 8; i++) {
                    double a = (2 * Math.PI * i) / 8;
                    Location orbitLoc = center.clone().add(Math.cos(a), 0.5, Math.sin(a));
                    BlockDisplayHandle h = displayBuilder.spawnBlock(orbitLoc, Material.OBSIDIAN);
                    h.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
                    centerCluster.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            int stormTick = ticksAlive - 60;
            if (stormTick > 80) return;

            // All four channels active
            if (stormTick % 2 == 0) {
                for (double angle : channelAngles) {
                    for (int i = -10; i <= 10; i += 2) {
                        Location pt = center.clone().add(Math.cos(angle) * i, 0.5, Math.sin(angle) * i);
                        w.spawnParticle(Particle.REVERSE_PORTAL, pt, 8, 0.3, 1.0, 0.3, 0.01);
                        w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, pt, 3, 0.2, 0, 0.2, 0);
                    }
                }
                // Center pure black
                DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 20, 1.0, 0, 0, 0, 3.0f);

                // Island-wide pull indicators
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = 5 + Math.random() * 15;
                    DisplayBuilder.dustParticles(center.clone().add(Math.cos(a) * d, 0.3, Math.sin(a) * d),
                            2, 0.3, 30, 0, 60, 0.5f);
                }
            }

            // Enderman stare ambient
            if (stormTick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 0.3f);
            }

            // Pull toward nearest channel
            if (stormTick % 3 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();

                    // Find nearest channel direction
                    double nearestDist = Double.MAX_VALUE;
                    double pullX = 0, pullZ = 0;
                    for (double angle : channelAngles) {
                        double perpX = -Math.sin(angle);
                        double perpZ = Math.cos(angle);
                        double dx = pl.getX() - center.getX();
                        double dz = pl.getZ() - center.getZ();
                        double perpDist = Math.abs(dx * perpX + dz * perpZ);
                        if (perpDist < nearestDist) {
                            nearestDist = perpDist;
                            double side = (dx * perpX + dz * perpZ >= 0) ? -1.0 : 1.0;
                            double strength = 0.1 + Math.max(0, (1.0 - nearestDist / 8.0)) * 0.3;
                            pullX = perpX * side * strength;
                            pullZ = perpZ * side * strength;
                        }
                    }
                    p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(pullX, 0, pullZ)));
                }
            }

            // Channel fall damage
            if (stormTick % 10 == 0) {
                for (double angle : channelAngles) {
                    double perpX = -Math.sin(angle);
                    double perpZ = Math.cos(angle);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - center.getX();
                        double dz = p.getLocation().getZ() - center.getZ();
                        double perpDist = Math.abs(dx * perpX + dz * perpZ);
                        double parDist = Math.abs(dx * Math.cos(angle) + dz * Math.sin(angle));
                        if (perpDist <= 1.0 && parDist <= 10.0) {
                            p.damage(10.0);
                            p.setVelocity(p.getVelocity().setY(1.8)); // Eject to Y+15
                            DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.5f);
                        }
                    }
                }
            }

            // Center cluster rotation
            if (stormTick % 2 == 0) {
                double rot = stormTick * 0.35;
                for (int i = 0; i < centerCluster.size(); i++) {
                    double a = rot + (2 * Math.PI * i) / centerCluster.size();
                    Location newPos = center.clone().add(Math.cos(a), 0.5, Math.sin(a));
                    centerCluster.get(i).entity().teleport(newPos);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidFractureStorm(plugin); }
    }

    // =========================================================================
    // 32. DIMENSIONAL ECHO -- ripple passes through arena, inverts controls
    // =========================================================================
    public static class DimensionalEcho extends EnvironmentalAttack {

        private Location rippleOrigin;
        private double rippleRadius = 0;

        public DimensionalEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_echo", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0); // No damage -- control disruption
            config.setDamageRadius(0.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double a = Math.random() * 2 * Math.PI;
            double d = Math.random() * 15;
            rippleOrigin = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);

            // 5-tick audio-only warning
            DisplayBuilder.playSound(rippleOrigin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive <= 5) return; // Warning period

            rippleRadius += 6; // 6 blocks per tick
            if (rippleRadius > 30) return;

            // Ripple ring particles
            DisplayBuilder.particleRing(rippleOrigin.clone().add(0, 1, 0), rippleRadius,
                    Particle.REVERSE_PORTAL, (int) (rippleRadius * 3), null);
            DisplayBuilder.particleRing(rippleOrigin.clone().add(0, 1.5, 0), rippleRadius,
                    Particle.PORTAL, (int) (rippleRadius * 2), null);
            DisplayBuilder.particleRing(rippleOrigin.clone().add(0, 2, 0), rippleRadius,
                    Particle.DUST, (int) rippleRadius,
                    new Particle.DustOptions(Color.fromRGB(80, 0, 120), 1.8f));

            // Hit players: scramble movement for 3 seconds
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dist = Math.sqrt(
                        Math.pow(p.getLocation().getX() - rippleOrigin.getX(), 2) +
                        Math.pow(p.getLocation().getZ() - rippleOrigin.getZ(), 2));
                if (Math.abs(dist - rippleRadius) <= 3.0) {
                    // Control inversion: reverse velocity briefly
                    org.bukkit.util.Vector vel = p.getVelocity();
                    p.setVelocity(new org.bukkit.util.Vector(-vel.getX() * 0.5, vel.getY(), -vel.getZ() * 0.5));
                    DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 0.5f, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalEcho(plugin); }
    }

    // =========================================================================
    // 33. VOID SCAR BLEED -- passive persistent hazard points accumulate
    // =========================================================================
    public static class VoidScarBleed extends EnvironmentalAttack {

        private Location bleedPoint;

        public VoidScarBleed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_scar_bleed", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(2.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            double a = Math.random() * 2 * Math.PI;
            double d = 3 + Math.random() * 18;
            bleedPoint = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);

            // Activation sound
            DisplayBuilder.playSound(bleedPoint, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.3f, 0.4f);

            // Permanent crying obsidian display
            BlockDisplayHandle marker = displayBuilder.spawnBlock(
                    bleedPoint.clone().add(0, 0.1, 0), Material.CRYING_OBSIDIAN);
            marker.scale(0.4f, 0.2f, 0.4f).glow(128, 0, 255).interpolation(5, 0);
            spawnedEntities.add(marker.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            if (bleedPoint == null) return;
            World w = bleedPoint.getWorld();
            if (w == null) return;

            // Permanent particles
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, bleedPoint.clone().add(0, 0.5, 0),
                        1, 0.1, 0.3, 0.1, 0);
                w.spawnParticle(Particle.REVERSE_PORTAL, bleedPoint.clone().add(0, 0.5, 0),
                        2, 1.0, 0.3, 1.0, 0.01);
                DisplayBuilder.dustParticles(bleedPoint.clone().add(0, 0.3, 0), 1, 0.3, 50, 0, 100, 0.5f);
            }

            // Damage within 1-block radius
            if (ticksAlive % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(bleedPoint) <= 1.0) {
                        p.damage(2.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { /* Bleed points persist */ }

        @Override
        public AbstractAttack newInstance() { return new VoidScarBleed(plugin); }
    }

    // =========================================================================
    // 34. VOIDMAW RESONANCE PULSE -- large expanding ring from disc overhead
    // =========================================================================
    public static class VoidmawResonancePulse extends EnvironmentalAttack {

        private boolean pulseFired = false;
        private int pulseTick = 0;
        private int aftershockCount = 0;

        public VoidmawResonancePulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("voidmaw_resonance_pulse", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(12.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Heartbeat buildup begins
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 100 ticks -- all void features activate + overhead disc
            if (ticksAlive <= 100) {
                // Accelerating heartbeat
                int heartbeatInterval = Math.max(2, 8 - ticksAlive / 15);
                if (ticksAlive % heartbeatInterval == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.2f);
                }

                // Void disc forms at Y+30
                if (ticksAlive >= 60 && ticksAlive % 3 == 0) {
                    double discRadius = 20.0 * (1.0 - (ticksAlive - 60) / 40.0);
                    if (discRadius < 2) discRadius = 2;
                    DisplayBuilder.particleRing(center.clone().add(0, 30, 0), discRadius,
                            Particle.REVERSE_PORTAL, 20, null);
                }

                // Void features glow
                if (ticksAlive % 10 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = 3 + Math.random() * 15;
                        w.spawnParticle(Particle.REVERSE_PORTAL,
                                center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d),
                                5, 0.5, 0.5, 0.5, 0.01);
                    }
                }
                return;
            }

            // Pulse fires
            if (!pulseFired) {
                pulseFired = true;
                pulseTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.3f);
            }

            pulseTick++;

            // Initial ring: expands at 4 blocks/tick, Y+1 to Y+3
            if (pulseTick <= 7) {
                double ringRadius = pulseTick * 4;
                DisplayBuilder.particleRing(center.clone().add(0, 1, 0), ringRadius,
                        Particle.REVERSE_PORTAL, (int) (ringRadius * 4), null);
                DisplayBuilder.particleRing(center.clone().add(0, 2, 0), ringRadius,
                        Particle.DUST, (int) (ringRadius * 2),
                        new Particle.DustOptions(Color.fromRGB(60, 0, 100), 3.0f));
                w.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, center.clone().add(0, 1.5, 0),
                        (int) (ringRadius * 2), ringRadius, 0.5, ringRadius, 0);

                // Enderman scream
                if (pulseTick == 1) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 0.8f, 0.4f);
                }

                // Ring hits ground: damage all
                if (pulseTick == 7) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().getY() > center.getY() + 3.5) {
                            p.damage(4.0); // Jumped -- reduced
                        } else {
                            p.damage(12.0); // Full hit
                        }
                    }
                }
            }

            // Aftershock rings: 3 total, every 20 ticks
            if (pulseTick > 7 && aftershockCount < 3) {
                int afterTick = pulseTick - 7;
                if (afterTick % 20 == 0) {
                    aftershockCount++;
                    double afterRadius = aftershockCount * 8;
                    double density = 0.6 / aftershockCount;

                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0), afterRadius,
                            Particle.REVERSE_PORTAL, (int) (afterRadius * density * 6), null);
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0),
                            (int) (afterRadius * density * 3), afterRadius, 60, 0, 100, 2.0f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.5f);

                    // Aftershock damage: 6 HP
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().getY() <= center.getY() + 3.5) {
                            p.damage(6.0);
                        }
                    }
                }
            }

            // Post-event: all void features glow enhanced
            if (pulseTick > 70 && pulseTick % 10 == 0) {
                for (int i = 0; i < 10; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = Math.random() * 20;
                    w.spawnParticle(Particle.REVERSE_PORTAL,
                            center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d),
                            3, 0.5, 0.5, 0.5, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidmawResonancePulse(plugin); }
    }

    // =========================================================================
    // 35. AURA DENSITY SURGE -- Dragon's ambient cloud spikes 600%
    // =========================================================================
    public static class AuraDensitySurge extends EnvironmentalAttack {

        public AuraDensitySurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("aura_density_surge", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0); // Visibility disruption only
            config.setDamageRadius(0.0);
            config.setDurationTicks(50);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Peak: 30 ticks of particle saturation
            if (ticksAlive <= 30) {
                w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 3, 0),
                        30, 15, 3, 15, 0.01);
                DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 20, 20.0,
                        160, 0, 160, 2.5f);
                w.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0, 1, 0),
                        10, 15, 1, 15, 0.01);
            }

            // Fade: 20 ticks
            if (ticksAlive > 30 && ticksAlive <= 50) {
                double fade = 1.0 - ((ticksAlive - 30) / 20.0);
                int count = (int) (20 * fade);
                DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), count, 15.0,
                        100, 0, 100, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AuraDensitySurge(plugin); }
    }

    // =========================================================================
    // 36. LIGHT FLICKER CASCADE -- all arena lights cut to black for 1 second
    // =========================================================================
    public static class LightFlickerCascade extends EnvironmentalAttack {

        public LightFlickerCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("light_flicker_cascade", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // Precursor flickers at T-10
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Precursor flickers: 10 ticks
            if (ticksAlive <= 10) {
                if (ticksAlive == 2 || ticksAlive == 5 || ticksAlive == 8) {
                    // Scattered burnout sounds
                    for (int i = 0; i < (ticksAlive / 3 + 1); i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 20;
                        Location flickPt = center.clone().add(Math.cos(a) * d, 1, Math.sin(a) * d);
                        DisplayBuilder.playSound(flickPt, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.5f, 0.8f);
                    }
                }
                return;
            }

            // Dark phase: 20 ticks
            if (ticksAlive <= 30) {
                // Smoke from all display positions
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 20;
                        w.spawnParticle(Particle.SMOKE, center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d),
                                3, 0.3, 0.3, 0.3, 0.01);
                    }
                }

                // Enderman stare quietly
                if (ticksAlive == 15) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.2f, 0.5f);
                }
                return;
            }

            // Lights restore: snap
            if (ticksAlive == 31) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LightFlickerCascade(plugin); }
    }

    // =========================================================================
    // 37. DRAGON BREATH GROUND SATURATION -- breath wash sweeps arena floor
    // =========================================================================
    public static class DragonBreathGroundSaturation extends EnvironmentalAttack {

        private double sweepAngle;
        private double sweepAngularVelocity;

        public DragonBreathGroundSaturation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_breath_ground_saturation", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            sweepAngle = Math.random() * 2 * Math.PI;
            sweepAngularVelocity = 0.06; // ~3.4 degrees per tick

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Dragon descends in orbit: 40 tick buildup
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    double orbitHeight = 40 - ticksAlive * 0.5;
                    Location dragonPos = center.clone().add(
                            Math.cos(sweepAngle) * 20, orbitHeight, Math.sin(sweepAngle) * 20);
                    w.spawnParticle(Particle.DRAGON_BREATH, dragonPos, 15, 2, 2, 2, 0.01);
                }
                return;
            }

            // Sweep active: 80 ticks
            int sweepTick = ticksAlive - 40;
            if (sweepTick > 80) return;

            sweepAngle += sweepAngularVelocity;

            // Ground saturation band: 6 blocks wide beneath dragon orbit
            double bandX = center.getX() + Math.cos(sweepAngle) * 15;
            double bandZ = center.getZ() + Math.sin(sweepAngle) * 15;
            Location bandCenter = new Location(w, bandX, center.getY() + 0.5, bandZ);

            if (sweepTick % 2 == 0) {
                w.spawnParticle(Particle.DRAGON_BREATH, bandCenter, 20, 3, 0.5, 3, 0.01);
                DisplayBuilder.dustParticles(bandCenter, 10, 3.0, 120, 0, 130, 2.0f);
                w.spawnParticle(Particle.SMOKE, bandCenter, 5, 2.0, 1.0, 2.0, 0.01);
            }

            // Dragon wingbeat
            if (sweepTick % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.6f);
            }

            // Damage in band
            if (sweepTick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(bandCenter) <= 9.0) { // 3 block radius
                        p.damage(6.0);
                        DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.0f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DragonBreathGroundSaturation(plugin); }
    }

    // =========================================================================
    // 38. VOID EMPEROR GAZE -- Dragon targets closest player with breath cone
    // =========================================================================
    public static class VoidEmperorGaze extends EnvironmentalAttack {

        private Player gazedPlayer;
        private int gazeTick = 0;

        public VoidEmperorGaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_emperor_gaze", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(1300);
            config.setTicksBetweenDamage(999);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Find closest player
            gazedPlayer = null;
            double nearest = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dist = p.getLocation().distanceSquared(center);
                if (dist < nearest) {
                    nearest = dist;
                    gazedPlayer = p;
                }
            }

            if (gazedPlayer != null) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.8f, 1.4f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || gazedPlayer == null || !gazedPlayer.isOnline()) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 20 ticks -- beam forms
            if (ticksAlive <= 20) {
                if (ticksAlive % 3 == 0) {
                    Location dragonHead = center.clone().add(0, 30, 0);
                    DisplayBuilder.particleLine(dragonHead, gazedPlayer.getLocation().clone().add(0, 1, 0),
                            Particle.DRAGON_BREATH, 2, null);
                }
                return;
            }

            gazeTick++;
            if (gazeTick > 60) return;

            // Gaze active: 60 ticks
            Location dragonHead = center.clone().add(0, 30, 0);
            Location playerPos = gazedPlayer.getLocation().clone().add(0, 1, 0);

            // Continuous beam
            if (gazeTick % 3 == 0) {
                DisplayBuilder.particleLine(dragonHead, playerPos, Particle.DRAGON_BREATH, 2, null);

                // Player aura
                w.spawnParticle(Particle.DRAGON_BREATH, playerPos, 5, 0.5, 0.5, 0.5, 0.01);
            }

            // Cone pulse every 10 ticks
            if (gazeTick % 10 == 0) {
                DisplayBuilder.playSound(gazedPlayer.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.4f, 0.8f);

                // Cone particles toward player
                double dx = playerPos.getX() - dragonHead.getX();
                double dy = playerPos.getY() - dragonHead.getY();
                double dz = playerPos.getZ() - dragonHead.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > 0) {
                    for (int step = 0; step < 10; step++) {
                        double t = step / 10.0;
                        Location conePt = dragonHead.clone().add(dx * t, dy * t, dz * t);
                        w.spawnParticle(Particle.DRAGON_BREATH, conePt, 5, 0.5 + t * 1.5, 0.5, 0.5 + t * 1.5, 0.01);
                        DisplayBuilder.dustParticles(conePt, 3, 0.5 + t, 130, 0, 130, 1.8f);
                    }
                }

                // Cone damage: 4 HP
                if (!isExempt(gazedPlayer) && gazedPlayer.getLocation().distanceSquared(center) <= 625) { // 25 range
                    gazedPlayer.damage(4.0);
                }
            }

            // Gaze break check: player moved >25 blocks from dragon
            if (gazedPlayer.getLocation().distanceSquared(center) > 625) {
                DisplayBuilder.playSound(gazedPlayer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f);
                gazeTick = 999; // End early
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEmperorGaze(plugin); }
    }

    // =========================================================================
    // 39. SANCTUM RESONANCE PULSE -- purely ambient, no damage
    // =========================================================================
    public static class SanctumResonancePulse extends EnvironmentalAttack {

        public SanctumResonancePulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_resonance_pulse", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // All amethyst positions emit enchant burst simultaneously
            for (int i = 0; i < 20; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 3 + Math.random() * 18;
                Location amethystPt = center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d);
                w.spawnParticle(Particle.ENCHANT, amethystPt, 15, 0.3, 1.0, 0.3, 1.0);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            // Brief 5-tick upwell
            if (ticksAlive <= 5) {
                Location center = getCenter();
                if (center == null) return;
                World w = center.getWorld();
                if (w == null) return;

                for (int i = 0; i < 10; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = 3 + Math.random() * 18;
                    Location pt = center.clone().add(Math.cos(a) * d, 0.5 + ticksAlive * 0.3, Math.sin(a) * d);
                    w.spawnParticle(Particle.ENCHANT, pt, 5, 0.2, 0.5, 0.2, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SanctumResonancePulse(plugin); }
    }

    // =========================================================================
    // 40. DRAGON THERMAL UPDRAFT -- upward thermal column with dragon orbit dip
    // =========================================================================
    public static class DragonThermalUpdraft extends EnvironmentalAttack {

        private Location updraftCenter;
        private boolean updraftActive = false;
        private boolean dragonDipped = false;

        public DragonThermalUpdraft(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_thermal_updraft", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            double a = Math.random() * 2 * Math.PI;
            double d = 3 + Math.random() * 12;
            updraftCenter = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 30 ticks -- heating zone
            if (ticksAlive <= 30) {
                if (ticksAlive % 3 == 0) {
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, updraftCenter.clone().add(0, 0.5, 0),
                            3 + ticksAlive / 5, 6.0, 0.3, 6.0, 0.01);
                }
                if (ticksAlive == 25) {
                    w.spawnParticle(Particle.FLAME, updraftCenter.clone().add(0, 0.5, 0), 15, 5, 0.3, 5, 0.01);
                }
                return;
            }

            // Updraft active: 60 ticks
            if (!updraftActive) {
                updraftActive = true;
                DisplayBuilder.playSound(updraftCenter, Sound.AMBIENT_BASALT_DELTAS_ADDITIONS, 0.6f, 1.2f);
            }

            int updraftTick = ticksAlive - 30;
            if (updraftTick > 60) return;

            // Column particles
            if (updraftTick % 2 == 0) {
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, updraftCenter.clone().add(0, 5, 0),
                        8, 5, 5, 5, 0.01);
                w.spawnParticle(Particle.FLAME, updraftCenter.clone().add(0, 2, 0),
                        3, 4, 1, 4, 0.01);
                w.spawnParticle(Particle.SMOKE, updraftCenter.clone().add(0, 10, 0),
                        5, 5, 5, 5, 0.02);
                DisplayBuilder.dustParticles(updraftCenter.clone().add(0, 8, 0), 4, 5.0,
                        255, 180, 80, 0.8f);
            }

            // Blaze ambient loop
            if (updraftTick % 60 == 0) {
                DisplayBuilder.playSound(updraftCenter, Sound.ENTITY_BLAZE_AMBIENT, 0.3f, 0.5f);
            }

            // Updraft lift: push players upward
            if (updraftTick % 3 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = p.getLocation().getX() - updraftCenter.getX();
                    double dz = p.getLocation().getZ() - updraftCenter.getZ();
                    if (dx * dx + dz * dz <= 144) { // 12 block radius
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 0.12, 0)));
                    }
                }
            }

            // Dragon orbit dip at midpoint (30 ticks in)
            if (updraftTick >= 25 && updraftTick <= 35 && !dragonDipped) {
                if (updraftTick == 25) {
                    dragonDipped = true;
                    DisplayBuilder.playSound(updraftCenter, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.7f);

                    // Dragon passes through at Y+20
                    w.spawnParticle(Particle.DRAGON_BREATH, updraftCenter.clone().add(0, 20, 0),
                            30, 5, 2, 5, 0.05);

                    // Damage players near Y+20 in dragon path
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - updraftCenter.getX();
                        double dz = p.getLocation().getZ() - updraftCenter.getZ();
                        if (dx * dx + dz * dz <= 16 && Math.abs(p.getLocation().getY() - (updraftCenter.getY() + 20)) <= 4) {
                            p.damage(12.0);
                            // Knock outward
                            double angle = Math.atan2(dz, dx);
                            p.setVelocity(p.getVelocity().add(
                                    new org.bukkit.util.Vector(Math.cos(angle) * 1.0, 0.3, Math.sin(angle) * 1.0)));
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DragonThermalUpdraft(plugin); }
    }
}
