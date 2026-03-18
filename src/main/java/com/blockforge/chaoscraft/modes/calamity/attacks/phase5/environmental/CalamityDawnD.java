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
 * Phase 4E (Boss 5: Supreme Calamitas) Environmental Attacks #31-40
 * SOUL ARCHITECTURE (#31-32) + DIMENSIONAL INSTABILITY (#33-40)
 *
 * Design: No status effects. Damage 6.0-18.0 HP. AxisAngle4f only.
 */
public final class CalamityDawnD {

    private CalamityDawnD() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new HauntedGround(plugin));
        registry.register(new TheConsumed(plugin));
        registry.register(new GravityInversionPatch(plugin));
        registry.register(new RiftTear(plugin));
        registry.register(new EndDistortionField(plugin));
        registry.register(new VoidTide(plugin));
        registry.register(new RealityCrackGrid(plugin));
        registry.register(new DimensionalEchoPulse(plugin));
        registry.register(new EndCrystalResonator(plugin));
        registry.register(new TheVoidSpeaks(plugin));
    }

    // =========================================================================
    // 31. HAUNTED GROUND -- migrating 3x3 soul damage patch
    // =========================================================================
    public static class HauntedGround extends EnvironmentalAttack {

        private Location patchCenter;
        private int screamCount = 0;

        public HauntedGround(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("haunted_ground", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts per scream
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(700); // 35 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double px = center.getX() + (Math.random() - 0.5) * 14;
            double pz = center.getZ() + (Math.random() - 0.5) * 14;
            patchCenter = new Location(w, px, center.getY(), pz);
            DisplayBuilder.playSound(patchCenter, Sound.BLOCK_SOUL_SAND_STEP, 0.4f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || patchCenter == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Flickering patch
            if (ticksAlive % 3 == 0) {
                boolean visible = (ticksAlive / 3) % 2 == 0 || ticksAlive > 20;
                if (visible) {
                    for (double dx = -1; dx <= 1; dx++) {
                        for (double dz = -1; dz <= 1; dz++) {
                            Location tile = patchCenter.clone().add(dx, 0.2, dz);
                            w.spawnParticle(Particle.SOUL, tile, 3, 0.2, 0.1, 0.2, 0.005);
                        }
                    }
                }
            }

            // Scream every 20 ticks
            if (ticksAlive % 20 == 0 && ticksAlive > 20) {
                w.spawnParticle(Particle.SOUL, patchCenter.clone().add(0, 1, 0), 60, 1, 2, 1, 0.03);
                DisplayBuilder.playSound(patchCenter, Sound.ENTITY_WITHER_HURT, 0.6f, 0.5f);

                // Scream damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = Math.abs(p.getLocation().getX() - patchCenter.getX());
                    double dz = Math.abs(p.getLocation().getZ() - patchCenter.getZ());
                    if (dx <= 2 && dz <= 2) {
                        p.damage(6.0);
                        w.spawnParticle(Particle.SOUL, p.getLocation().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0.01);
                    }
                }
                screamCount++;
            }

            // Migrate at tick 100
            if (ticksAlive == 100) {
                double shift = 1 + Math.random();
                int dir = (int) (Math.random() * 4);
                switch (dir) {
                    case 0 -> patchCenter.add(shift, 0, 0);
                    case 1 -> patchCenter.add(-shift, 0, 0);
                    case 2 -> patchCenter.add(0, 0, shift);
                    case 3 -> patchCenter.add(0, 0, -shift);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HauntedGround(plugin); }
    }

    // =========================================================================
    // 32. THE CONSUMED -- 40 spectral silhouettes manifest across arena
    // =========================================================================
    public static class TheConsumed extends EnvironmentalAttack {

        private final List<Location> silhouettes = new ArrayList<>();

        public TheConsumed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_consumed", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(8.0); // 4 hearts per proximity hit
            config.setDamageRadius(0.0);
            config.setDurationTicks(280); // 14 seconds
            config.setCooldownTicks(99999); // 40% HP threshold
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 40; i++) {
                double sx = center.getX() + (Math.random() - 0.5) * 18;
                double sz = center.getZ() + (Math.random() - 0.5) * 18;
                silhouettes.add(new Location(w, sx, center.getY(), sz));
            }
            DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-120 ticks (6 seconds) -- floor breathing
            if (ticksAlive <= 120) {
                if (ticksAlive % 4 == 0) {
                    double wavePos = (ticksAlive % 40) / 40.0 * 20 - 10;
                    for (double x = -10; x <= 10; x += 2) {
                        double intensity = Math.max(0, 1 - Math.abs(x - wavePos) / 3.0);
                        if (intensity > 0) {
                            for (double z = -10; z <= 10; z += 2) {
                                Location fp = center.clone().add(x, 0.3, z);
                                w.spawnParticle(Particle.SOUL, fp, (int) (3 * intensity), 0.5, 0.1, 0.5, 0.005);
                            }
                        }
                    }
                }
                if (ticksAlive == 80) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 1.0f, 0.3f);
                }
                return;
            }

            // Silhouettes manifest: 121-240 ticks (6 seconds)
            int silTick = ticksAlive - 121;
            if (silTick >= 0 && silTick <= 120) {
                if (silTick % 3 == 0) {
                    for (Location sp : silhouettes) {
                        // Humanoid particle outline
                        for (double y = 0; y < 1.8; y += 0.3) {
                            w.spawnParticle(Particle.SOUL, sp.clone().add(0, y, 0), 2, 0.1, 0.05, 0.1, 0.003);
                        }
                    }
                    // Throttled whisper sounds
                    if (silTick % 15 == 0) {
                        Location randomSil = silhouettes.get((int) (Math.random() * silhouettes.size()));
                        DisplayBuilder.playSound(randomSil, Sound.ENTITY_WITHER_AMBIENT, 0.3f,
                                0.5f + (float) (Math.random() * 0.3));
                    }
                }

                // Proximity damage
                if (silTick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        for (Location sp : silhouettes) {
                            if (p.getLocation().distanceSquared(sp) <= 2.25) { // 1.5 blocks
                                p.damage(8.0);
                                break;
                            }
                        }
                    }
                }
            }

            // Vanish burst: tick 241
            if (silTick == 120) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_DEATH, 0.5f, 0.6f);
                for (Location sp : silhouettes) {
                    w.spawnParticle(Particle.SOUL, sp.clone().add(0, 0.5, 0), 20, 0.5, 0.5, 0.5, 0.03);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheConsumed(plugin); }
    }

    // =========================================================================
    // 33. GRAVITY INVERSION PATCH -- 5x5 patch that floats players upward
    // =========================================================================
    public static class GravityInversionPatch extends EnvironmentalAttack {

        private Location patchPos;

        public GravityInversionPatch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_inversion_patch", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); // Fall damage is the hazard
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(900); // 45 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            patchPos = center.clone().add(
                    (Math.random() - 0.5) * 12, 0, (Math.random() - 0.5) * 12);
            DisplayBuilder.playSound(patchPos, Sound.BLOCK_PORTAL_TRAVEL, 0.5f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || patchPos == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-40 ticks
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    for (double dx = -2.5; dx <= 2.5; dx += 1.0) {
                        for (double dz = -2.5; dz <= 2.5; dz += 1.0) {
                            w.spawnParticle(Particle.PORTAL, patchPos.clone().add(dx, 0.5, dz),
                                    3, 0.2, 0.5, 0.2, 0.05);
                        }
                    }
                    // Boundary
                    DisplayBuilder.particleRing(patchPos.clone().add(0, 0.3, 0), 2.5,
                            Particle.END_ROD, 15, null);
                }
                return;
            }

            // Active: 41-200 ticks (8 seconds) -- inversion
            if (ticksAlive > 40) {
                // Upward drifting portal particles
                if (ticksAlive % 3 == 0) {
                    for (double dx = -2.5; dx <= 2.5; dx += 1.0) {
                        for (double dz = -2.5; dz <= 2.5; dz += 1.0) {
                            w.spawnParticle(Particle.PORTAL, patchPos.clone().add(dx, 0.5, dz),
                                    2, 0.2, 1, 0.2, 0.05);
                        }
                    }
                }

                // Launch players upward
                if (ticksAlive % 4 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = Math.abs(p.getLocation().getX() - patchPos.getX());
                        double dz = Math.abs(p.getLocation().getZ() - patchPos.getZ());
                        if (dx <= 2.5 && dz <= 2.5) {
                            // Apply upward velocity
                            org.bukkit.util.Vector vel = p.getVelocity();
                            vel.setY(Math.min(vel.getY() + 0.4, 1.5));
                            p.setVelocity(vel);
                            w.spawnParticle(Particle.PORTAL, p.getLocation(), 5, 0.3, 0.3, 0.3, 0.05);
                        }
                    }
                }

                if (ticksAlive % 40 == 0) {
                    DisplayBuilder.playSound(patchPos, Sound.AMBIENT_CAVE, 0.3f, 1.1f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravityInversionPatch(plugin); }
    }

    // =========================================================================
    // 34. RIFT TEAR -- suction rift at arena wall edge
    // =========================================================================
    public static class RiftTear extends EnvironmentalAttack {

        private Location riftPos;
        private int wallSide; // 0=N, 1=E, 2=S, 3=W

        public RiftTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rift_tear", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(260); // 13 seconds
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0); // 6 hearts rift contact
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            wallSide = (int) (Math.random() * 4);
            double offset = 10;
            riftPos = switch (wallSide) {
                case 0 -> center.clone().add(0, 1, -offset);
                case 1 -> center.clone().add(offset, 1, 0);
                case 2 -> center.clone().add(0, 1, offset);
                case 3 -> center.clone().add(-offset, 1, 0);
                default -> center.clone();
            };
            DisplayBuilder.playSound(riftPos, Sound.BLOCK_PORTAL_TRIGGER, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || riftPos == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-60 ticks
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    w.spawnParticle(Particle.PORTAL, riftPos, 15, 1, 1.5, 1, 0.1);
                    w.spawnParticle(Particle.END_ROD, riftPos, 5, 0.5, 1, 0.5, 0.02);
                }
                return;
            }

            // Active rift: 61-260 ticks (10 seconds)
            int riftTick = ticksAlive - 61;
            if (riftTick >= 0 && riftTick <= 200) {
                // Rift face particles
                if (riftTick % 3 == 0) {
                    w.spawnParticle(Particle.PORTAL, riftPos, 20, 1.5, 2.5, 1.5, 0.1);
                    w.spawnParticle(Particle.END_ROD, riftPos, 8, 1, 2, 1, 0.03);
                }

                // Suction pull toward rift
                if (riftTick % 4 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distance(riftPos);
                        if (dist <= 10 && dist > 0.5) {
                            double pullStrength = 0.1 + (dist <= 3 ? 0.2 : 0);
                            org.bukkit.util.Vector pull = riftPos.toVector()
                                    .subtract(p.getLocation().toVector()).normalize().multiply(pullStrength);
                            p.setVelocity(p.getVelocity().add(pull));
                            // Suction visual
                            w.spawnParticle(Particle.PORTAL, p.getLocation(), 3, 0.2, 0.2, 0.2, 0.05);
                        }

                        // Rift contact damage + bounce back
                        if (dist <= 1.5) {
                            p.damage(12.0);
                            org.bukkit.util.Vector bounce = p.getLocation().toVector()
                                    .subtract(riftPos.toVector()).normalize().multiply(2.5);
                            bounce.setY(0.5);
                            p.setVelocity(bounce);
                            DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.9f, 0.7f);
                        }
                    }
                }

                if (riftTick % 40 == 0) {
                    DisplayBuilder.playSound(riftPos, Sound.AMBIENT_UNDERWATER_LOOP, 0.4f, 0.6f);
                }
            }

            // Seal
            if (riftTick == 200) {
                DisplayBuilder.playSound(riftPos, Sound.BLOCK_PORTAL_TRAVEL, 0.9f, 1.0f);
                w.spawnParticle(Particle.PORTAL, riftPos, 100, 2, 3, 2, 0.1);
                w.spawnParticle(Particle.END_ROD, riftPos, 50, 2, 3, 2, 0.05);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftTear(plugin); }
    }

    // =========================================================================
    // 35. END DISTORTION FIELD -- 10x10 disorientation zone
    // =========================================================================
    public static class EndDistortionField extends EnvironmentalAttack {

        public EndDistortionField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("end_distortion_field", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0); // Disorientation only
            config.setDamageRadius(0.0);
            config.setDurationTicks(280); // 14 seconds
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-40 ticks
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double dx = center.getX() + (Math.random() - 0.5) * 10;
                        double dz = center.getZ() + (Math.random() - 0.5) * 10;
                        w.spawnParticle(Particle.PORTAL, new Location(w, dx, center.getY() + Math.random() * 3, dz),
                                3, 0.5, 0.5, 0.5, 0.05);
                    }
                }
                return;
            }

            // Active: 41-280 -- dense portal distortion
            if (ticksAlive > 40) {
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double dx = center.getX() + (Math.random() - 0.5) * 10;
                        double dy = center.getY() + Math.random() * 10;
                        double dz = center.getZ() + (Math.random() - 0.5) * 10;
                        w.spawnParticle(Particle.PORTAL, new Location(w, dx, dy, dz), 2, 0.3, 0.3, 0.3, 0.05);
                    }
                }

                // Distortion spikes every 30 ticks
                if (ticksAlive % 30 == 0) {
                    double sx = center.getX() + (Math.random() - 0.5) * 8;
                    double sy = center.getY() + Math.random() * 8;
                    double sz = center.getZ() + (Math.random() - 0.5) * 8;
                    Location spikePos = new Location(w, sx, sy, sz);
                    w.spawnParticle(Particle.PORTAL, spikePos, 50, 1, 1, 1, 0.1);
                    DisplayBuilder.playSound(spikePos, Sound.BLOCK_PORTAL_TRIGGER, 0.4f,
                            1.0f + (float) (Math.random() * 0.5));
                }

                // Boundary markers
                if (ticksAlive % 8 == 0) {
                    for (double d = -5; d <= 5; d += 2) {
                        w.spawnParticle(Particle.END_ROD, center.clone().add(d, 0.3, -5), 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.END_ROD, center.clone().add(d, 0.3, 5), 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.END_ROD, center.clone().add(-5, 0.3, d), 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.END_ROD, center.clone().add(5, 0.3, d), 1, 0, 0, 0, 0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EndDistortionField(plugin); }
    }

    // =========================================================================
    // 36. VOID TIDE -- rolling wave of void particles crosses arena
    // =========================================================================
    public static class VoidTide extends EnvironmentalAttack {

        private int tideDirection; // 0=north->south, 1=east->west

        public VoidTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tide", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(7.0); // 3.5 hearts per 8 ticks in tide
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(1400); // 70 seconds
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            tideDirection = Math.random() < 0.5 ? 0 : 1;
            DisplayBuilder.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_ULTRA_RARE, 0.7f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-80 ticks -- gathering mass at one edge
            if (ticksAlive <= 80) {
                double progress = ticksAlive / 80.0;
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < (int) (5 + 10 * progress); i++) {
                        double perpOffset = (Math.random() - 0.5) * 20;
                        Location gatherPos;
                        if (tideDirection == 0) {
                            gatherPos = center.clone().add(perpOffset, Math.random() * 2, -10);
                        } else {
                            gatherPos = center.clone().add(-10, Math.random() * 2, perpOffset);
                        }
                        w.spawnParticle(Particle.PORTAL, gatherPos, 3, 0.5, 0.3, 0.5, 0.05);
                        DisplayBuilder.dustParticles(gatherPos, 2, 0.5, 0, 0, 0, 1.5f);
                    }
                }
                return;
            }

            // First tide: 81-100 ticks (20 ticks, 1 block/tick)
            int tide1Tick = ticksAlive - 81;
            if (tide1Tick >= 0 && tide1Tick <= 20) {
                double tidePos = -10 + tide1Tick;
                if (tide1Tick % 2 == 0) {
                    for (double perp = -10; perp <= 10; perp += 1.0) {
                        Location tideLoc;
                        if (tideDirection == 0) {
                            tideLoc = center.clone().add(perp, 1, tidePos);
                        } else {
                            tideLoc = center.clone().add(tidePos, 1, perp);
                        }
                        w.spawnParticle(Particle.PORTAL, tideLoc, 5, 0.3, 0.5, 0.3, 0.05);
                        DisplayBuilder.dustParticles(tideLoc, 3, 0.3, 0, 0, 0, 1.5f);
                    }
                }

                // Tide damage
                if (tide1Tick % 8 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double playerPos = tideDirection == 0 ?
                                p.getLocation().getZ() - center.getZ() : p.getLocation().getX() - center.getX();
                        if (Math.abs(playerPos - tidePos) < 2.0 && p.getLocation().getY() < center.getY() + 2.5) {
                            p.damage(7.0);
                        }
                    }
                }
            }

            // Second tide from opposite: 111-131 ticks
            int tide2Tick = ticksAlive - 111;
            if (tide2Tick >= 0 && tide2Tick <= 20) {
                double tidePos = 10 - tide2Tick;
                if (tide2Tick % 2 == 0) {
                    for (double perp = -10; perp <= 10; perp += 1.0) {
                        Location tideLoc;
                        if (tideDirection == 0) {
                            tideLoc = center.clone().add(perp, 1, tidePos);
                        } else {
                            tideLoc = center.clone().add(tidePos, 1, perp);
                        }
                        w.spawnParticle(Particle.END_ROD, tideLoc, 5, 0.3, 0.5, 0.3, 0.02);
                        DisplayBuilder.dustParticles(tideLoc, 2, 0.3, 200, 200, 220, 1.0f);
                    }
                }

                if (tide2Tick % 8 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double playerPos = tideDirection == 0 ?
                                p.getLocation().getZ() - center.getZ() : p.getLocation().getX() - center.getX();
                        if (Math.abs(playerPos - tidePos) < 2.0 && p.getLocation().getY() < center.getY() + 2.5) {
                            p.damage(7.0);
                        }
                    }
                }
            }

            // Dissipation
            if (ticksAlive > 131 && ticksAlive <= 160) {
                if (ticksAlive == 132) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 0.8f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTide(plugin); }
    }

    // =========================================================================
    // 37. REALITY CRACK GRID -- ceiling grid shatters into falling shards
    // =========================================================================
    public static class RealityCrackGrid extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private final List<Location> shardPositions = new ArrayList<>();
        private boolean dropped = false;

        public RealityCrackGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_crack_grid", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(99999); // 45% HP threshold
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0); // 8 hearts direct
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Generate 16 shard positions in 4x4 grid
            for (int gx = 0; gx < 4; gx++) {
                for (int gz = 0; gz < 4; gz++) {
                    double sx = center.getX() - 7.5 + gx * 5;
                    double sz = center.getZ() - 7.5 + gz * 5;
                    shardPositions.add(new Location(w, sx, center.getY() + 18, sz));
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-100 ticks (5 seconds) -- grid forming
            if (ticksAlive <= 100) {
                double progress = ticksAlive / 100.0;
                if (ticksAlive % 4 == 0) {
                    // Draw grid lines at Y+18
                    for (int i = 0; i <= 4; i++) {
                        double linePos = -10 + i * 5;
                        double lineLen = 20 * progress;
                        // Horizontal
                        for (double d = -lineLen / 2; d <= lineLen / 2; d += 1.0) {
                            w.spawnParticle(Particle.END_ROD, center.clone().add(d, 18, linePos),
                                    1, 0, 0, 0, 0);
                        }
                        // Vertical
                        for (double d = -lineLen / 2; d <= lineLen / 2; d += 1.0) {
                            w.spawnParticle(Particle.END_ROD, center.clone().add(linePos, 18, d),
                                    1, 0, 0, 0, 0);
                        }
                    }
                    if ((int) (ticksAlive * progress) % 15 == 0) {
                        DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.4f, 0.6f);
                    }
                }
                return;
            }

            // Drop shards: 101-140 ticks (staggered)
            if (!dropped && ticksAlive > 100) {
                dropped = true;
                for (int i = 0; i < shardPositions.size(); i++) {
                    Location sp = shardPositions.get(i);
                    int delay = (int) (Math.random() * 40); // Random +-20 ticks
                    BlockDisplayHandle h = displayBuilder.spawnBlock(sp, Material.TINTED_GLASS);
                    h.scale(2.5f, 0.5f, 2.5f).glow(200, 200, 255).interpolation(20, delay);
                    shardHandles.add(h);
                    spawnedEntities.add(h.entity());
                    DisplayBuilder.playSound(sp, Sound.ENTITY_ARROW_SHOOT, 0.4f, 0.9f);
                }
            }

            // Shard fall simulation: 101-160 ticks
            if (ticksAlive > 100 && ticksAlive <= 160) {
                int fallTick = ticksAlive - 100;
                for (int i = 0; i < shardPositions.size(); i++) {
                    Location sp = shardPositions.get(i);
                    double fallProgress = Math.min(1.0, (fallTick + (i % 5) * 4) / 40.0);
                    double currentY = center.getY() + 18 - 18 * fallProgress;

                    // Trail particles
                    if (fallTick % 3 == 0) {
                        w.spawnParticle(Particle.END_ROD, new Location(w, sp.getX(), currentY + 1, sp.getZ()),
                                3, 0.5, 0.2, 0.5, 0.01);
                    }

                    // Ground impact
                    if (currentY <= center.getY() + 0.5 && fallProgress >= 0.99) {
                        Location impact = new Location(w, sp.getX(), center.getY(), sp.getZ());
                        w.spawnParticle(Particle.END_ROD, impact, 40, 1, 0.5, 1, 0.05);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 0.9f, 0.8f);

                        // Impact damage
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(impact) <= 4.0) {
                                p.damage(16.0); // 8 hearts direct
                            } else if (p.getLocation().distanceSquared(impact) <= 9.0) {
                                p.damage(8.0); // 4 hearts proximity
                            }
                        }
                    }
                }
            }

            // Permanent grid residue: 161-200 ticks
            if (ticksAlive > 160 && ticksAlive % 8 == 0) {
                for (int i = 0; i <= 4; i++) {
                    double linePos = -10 + i * 5;
                    for (double d = -10; d <= 10; d += 3.0) {
                        w.spawnParticle(Particle.END_ROD, center.clone().add(d, 18, linePos), 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.END_ROD, center.clone().add(linePos, 18, d), 1, 0, 0, 0, 0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityCrackGrid(plugin); }
    }

    // =========================================================================
    // 38. DIMENSIONAL ECHO PULSE -- inward ring then return pulse
    // =========================================================================
    public static class DimensionalEchoPulse extends EnvironmentalAttack {

        public DimensionalEchoPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_echo_pulse", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(1100); // 55 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts per ring
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-60 ticks -- edges glowing
            if (ticksAlive <= 60) {
                if (ticksAlive % 5 == 0) {
                    for (double d = -10; d <= 10; d += 2) {
                        w.spawnParticle(Particle.END_ROD, center.clone().add(d, 0.5, -10), 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.END_ROD, center.clone().add(d, 0.5, 10), 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.END_ROD, center.clone().add(-10, 0.5, d), 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.END_ROD, center.clone().add(10, 0.5, d), 1, 0, 0, 0, 0);
                    }
                }
                return;
            }

            // Inward ring: 61-74 ticks (14 ticks, 10 blocks at 1.5/tick)
            int pulseTick = ticksAlive - 61;
            if (pulseTick >= 0 && pulseTick <= 14) {
                double radius = 10 - pulseTick * 1.5;
                if (radius > 0) {
                    DisplayBuilder.particleRing(center.clone().add(0, 1.5, 0), radius, Particle.END_ROD,
                            (int) (radius * 5), null);
                    DisplayBuilder.particleRing(center.clone().add(0, 1.5, 0), radius, Particle.PORTAL,
                            (int) (radius * 3), null);

                    // Damage
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double pDist = p.getLocation().distance(center);
                        if (Math.abs(pDist - radius) < 1.5) {
                            p.damage(6.0);
                        }
                    }
                }
            }

            // Center collapse burst at tick 75
            if (pulseTick == 14) {
                w.spawnParticle(Particle.END_ROD, center, 100, 1, 1, 1, 0.1);
                w.spawnParticle(Particle.PORTAL, center, 100, 1, 1, 1, 0.1);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_HURT, 0.9f, 1.0f);
            }

            // Outward return pulse: 76-90 ticks
            if (pulseTick > 14 && pulseTick <= 28) {
                double radius = (pulseTick - 14) * 1.5;
                DisplayBuilder.particleRing(center.clone().add(0, 1.5, 0), radius, Particle.END_ROD,
                        (int) (radius * 3), null); // 50% density
                DisplayBuilder.particleRing(center.clone().add(0, 1.5, 0), radius, Particle.PORTAL,
                        (int) (radius * 2), null);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double pDist = p.getLocation().distance(center);
                    if (Math.abs(pDist - radius) < 1.5) {
                        p.damage(6.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalEchoPulse(plugin); }
    }

    // =========================================================================
    // 39. END CRYSTAL RESONATOR -- structure that pings and detonates
    // =========================================================================
    public static class EndCrystalResonator extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> structureHandles = new ArrayList<>();
        private Location resonatorPos;
        private boolean detonated = false;

        public EndCrystalResonator(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("end_crystal_resonator", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(460); // 23 seconds
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0); // 7 hearts detonation
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double rx = center.getX() + (Math.random() - 0.5) * 12;
            double rz = center.getZ() + (Math.random() - 0.5) * 12;
            resonatorPos = new Location(w, rx, center.getY(), rz);
            DisplayBuilder.playSound(resonatorPos, Sound.BLOCK_BEACON_POWER_SELECT, 0.9f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || resonatorPos == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-60 ticks -- pillar growing
            if (ticksAlive <= 60) {
                double progress = ticksAlive / 60.0;
                if (ticksAlive % 3 == 0) {
                    for (double y = 0; y < 10 * progress; y += 1.0) {
                        w.spawnParticle(Particle.END_ROD, resonatorPos.clone().add(0, y, 0),
                                3, 0.2, 0.2, 0.2, 0.01);
                    }
                }
                // Spawn structure at tick 60
                if (ticksAlive == 60) {
                    BlockDisplayHandle base = displayBuilder.spawnBlock(
                            resonatorPos.clone().add(0, 0.5, 0), Material.PURPUR_BLOCK);
                    base.scale(1.2f, 1.2f, 1.2f).glow(128, 0, 200).interpolation(5, 0);
                    structureHandles.add(base);
                    spawnedEntities.add(base.entity());

                    BlockDisplayHandle mid = displayBuilder.spawnBlock(
                            resonatorPos.clone().add(0, 1.7, 0), Material.ENDER_CHEST);
                    mid.scale(0.8f, 0.8f, 0.8f).glow(100, 0, 180).interpolation(5, 0);
                    structureHandles.add(mid);
                    spawnedEntities.add(mid.entity());
                }
                return;
            }

            // Active: 61-400 ticks (20 seconds) -- pinging
            int activeTick = ticksAlive - 61;
            if (activeTick >= 0 && activeTick <= 340) {
                // Radial ring emission
                if (activeTick % 4 == 0) {
                    DisplayBuilder.particleRing(resonatorPos.clone().add(0, 3, 0), 6.0,
                            Particle.END_ROD, 20, null);
                }

                // Ping every 40 ticks
                if (activeTick % 40 == 0) {
                    w.spawnParticle(Particle.END_ROD, resonatorPos.clone().add(0, 2.5, 0),
                            60, 3, 3, 3, 0.05);
                    DisplayBuilder.playSound(resonatorPos, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.8f);

                    // Ping damage
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(resonatorPos) <= 16.0) { // 4 blocks
                            p.damage(7.0); // 3.5 hearts per ping
                        }
                    }
                }
            }

            // Detonation at tick 401
            if (activeTick > 340 && !detonated) {
                detonated = true;
                triggerImpactDamage(resonatorPos);
                DisplayBuilder.playSound(resonatorPos, Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.2f, 0.9f);
                w.spawnParticle(Particle.END_ROD, resonatorPos.clone().add(0, 2, 0), 200, 5, 5, 5, 0.1);
                w.spawnParticle(Particle.PORTAL, resonatorPos.clone().add(0, 2, 0), 100, 5, 5, 5, 0.1);
                w.spawnParticle(Particle.FLASH, resonatorPos, 2, 0, 0, 0, 0);
            }

            // Residue: stump particles
            if (activeTick > 340 && ticksAlive % 6 == 0) {
                w.spawnParticle(Particle.END_ROD, resonatorPos.clone().add(0, 0.5, 0),
                        5, 0.3, 0.3, 0.3, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EndCrystalResonator(plugin); }
    }

    // =========================================================================
    // 40. THE VOID SPEAKS -- convergence starburst from arena edges
    // =========================================================================
    public static class TheVoidSpeaks extends EnvironmentalAttack {

        private boolean converged = false;

        public TheVoidSpeaks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_void_speaks", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(400); // 20 seconds
            config.setCooldownTicks(99999); // 30% HP threshold
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0); // 9 hearts -- unavoidable
            config.setImpactRadius(15.0);
        }

        @Override
        protected void onSpawn(Location center) {
            // 8 second buildup -- starts with silence
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-160 ticks (8 seconds) -- everything dims, silence
            if (ticksAlive <= 160) {
                // Sparse end ambience
                if (ticksAlive == 80) {
                    DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.2f, 0.3f);
                }
                if (ticksAlive == 120) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.3f);
                }
                return;
            }

            // Convergence: tick 161 -- 8 lines of PORTAL from edges to center
            if (!converged) {
                converged = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.3f);

                // 8 burst points at corners and edge midpoints
                double[][] origins = {
                        {-10, -10}, {0, -10}, {10, -10}, {10, 0},
                        {10, 10}, {0, 10}, {-10, 10}, {-10, 0}
                };
                for (double[] origin : origins) {
                    Location burstPoint = center.clone().add(origin[0], 0.5, origin[1]);
                    // Line from burst point to center
                    DisplayBuilder.particleLine(burstPoint, center.clone().add(0, 0.5, 0),
                            Particle.PORTAL, 3, null);
                }

                // Central detonation
                w.spawnParticle(Particle.PORTAL, center, 500, 5, 3, 5, 0.1);
                w.spawnParticle(Particle.FLASH, center, 3, 0, 0, 0, 0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_HURT, 2.0f, 0.3f);

                // Unavoidable damage
                triggerImpactDamage(center);
            }

            // Post-convergence drain: 162-260 ticks (5 seconds)
            int postTick = ticksAlive - 161;
            if (postTick > 0 && postTick <= 100) {
                if (postTick % 4 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double dx = center.getX() + (Math.random() - 0.5) * 20;
                        double dz = center.getZ() + (Math.random() - 0.5) * 20;
                        double dy = center.getY() + 2 - (postTick / 100.0) * 3; // Sinking
                        w.spawnParticle(Particle.PORTAL, new Location(w, dx, dy, dz), 3, 0.5, 0.5, 0.5, 0.03);
                    }
                }
            }

            // Silence: 261-320 (3 seconds)

            // Post-event surge: 321-400 (200% ambient emissions)
            if (postTick > 160 && postTick <= 240) {
                if (postTick == 161) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.5f);
                }
                if (postTick % 2 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double sx = center.getX() + (Math.random() - 0.5) * 20;
                        double sz = center.getZ() + (Math.random() - 0.5) * 20;
                        double sy = center.getY() + Math.random() * 30;
                        switch (i % 5) {
                            case 0 -> w.spawnParticle(Particle.WITCH, new Location(w, sx, sy, sz), 3, 0.5, 0.5, 0.5, 0);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, new Location(w, sx, sy, sz), 3, 0.5, 0.5, 0.5, 0.02);
                            case 2 -> w.spawnParticle(Particle.END_ROD, new Location(w, sx, sy, sz), 3, 0.5, 0.5, 0.5, 0.02);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, sx, sy, sz), 3, 0.5, 0.5, 0.5, 0.01);
                            case 4 -> w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w, sx, center.getY() + 1, sz), 3, 0.5, 0.2, 0.5, 0.01);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheVoidSpeaks(plugin); }
    }
}
