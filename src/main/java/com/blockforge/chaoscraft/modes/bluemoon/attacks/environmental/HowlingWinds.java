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
 * Blue Moon Environmental -- GROUP: HOWLING WINDS
 * 13 wind-themed environmental attacks.
 * Particles: CLOUD, CAMPFIRE_SIGNAL_SMOKE, SWEEP_ATTACK, DUST(200,200,220)
 * Sounds: ENTITY_PHANTOM_FLAP, ITEM_ELYTRA_FLYING, ENTITY_ENDER_DRAGON_FLAP
 * NO status effects. Velocity for knockback/push only.
 */
public final class HowlingWinds {

    private HowlingWinds() {}

    private static final Particle.DustOptions WIND_DUST =
            new Particle.DustOptions(Color.fromRGB(200, 200, 220), 1.0f);
    private static final Particle.DustOptions WIND_DUST_LARGE =
            new Particle.DustOptions(Color.fromRGB(200, 200, 220), 1.5f);

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GaleForce(plugin));
        registry.register(new Cyclone(plugin));
        registry.register(new WindShear(plugin));
        registry.register(new Downdraft(plugin));
        registry.register(new Tailwind(plugin));
        registry.register(new Crosswind(plugin));
        registry.register(new Vortex(plugin));
        registry.register(new SonicBoom(plugin));
        registry.register(new Whisper(plugin));
        registry.register(new DustDevil(plugin));
        registry.register(new JetStream(plugin));
        registry.register(new CalmBeforeStorm(plugin));
        registry.register(new HurricaneEye(plugin));
    }

    // ================================================================
    // 1. GALE FORCE -- Strong directional push (velocity 0.8 in random direction).
    //    Damage: 5, radius: 15, duration: 20.
    // ================================================================
    public static class GaleForce extends EnvironmentalAttack {

        private double dirX, dirZ;

        public GaleForce(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gale_force", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Directional wind particles
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 20; i++) {
                    double ox = (Math.random() - 0.5) * 30;
                    double oz = (Math.random() - 0.5) * 30;
                    Location pLoc = center.clone().add(ox, Math.random() * 3, oz);
                    w.spawnParticle(Particle.CLOUD, pLoc, 1, dirX * 0.5, 0, dirZ * 0.5, 0.1);
                    w.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, WIND_DUST);
                }
                w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center.clone().add(0, 2, 0), 5,
                        8, 1, 8, 0.02);
            }

            // Push all players in radius
            if (ticksAlive % 4 == 0) {
                Vector push = new Vector(dirX * 0.8, 0.05, dirZ * 0.8);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 225) {
                        p.setVelocity(p.getVelocity().add(push));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GaleForce(plugin); }
    }

    // ================================================================
    // 2. CYCLONE -- Swirling particle funnel. Players orbit upward.
    //    Damage: 6, radius: 5, duration: 80.
    // ================================================================
    public static class Cyclone extends EnvironmentalAttack {

        public Cyclone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cyclone", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Swirling funnel particles
            if (ticksAlive % 2 == 0) {
                double funnelHeight = Math.min(ticksAlive * 0.15, 8.0);
                for (double y = 0; y < funnelHeight; y += 0.4) {
                    double radius = 5.0 * (1.0 - y / 10.0);
                    double angle = y * 2.0 + ticksAlive * 0.3;
                    Location pLoc = center.clone().add(
                            Math.cos(angle) * radius, y,
                            Math.sin(angle) * radius);
                    w.spawnParticle(Particle.CLOUD, pLoc, 1, 0.2, 0, 0.2, 0.01);
                    w.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, WIND_DUST);
                }
                w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center.clone().add(0, funnelHeight, 0),
                        3, 1, 0.5, 1, 0.05);
            }

            // Pull players into cyclone + orbit upward
            if (ticksAlive % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double distSq = p.getLocation().distanceSquared(center);
                    if (distSq > 25) continue;

                    double dist = Math.sqrt(distSq);
                    if (dist < 0.5) dist = 0.5;

                    // Pull toward center
                    Vector toCenter = center.toVector().subtract(p.getLocation().toVector()).normalize();
                    // Tangential orbit component
                    Vector tangent = new Vector(-toCenter.getZ(), 0, toCenter.getX());
                    Vector pull = toCenter.multiply(0.3).add(tangent.multiply(0.4));
                    pull.setY(0.15); // Upward lift
                    p.setVelocity(p.getVelocity().add(pull));
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.7f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new Cyclone(plugin); }
    }

    // ================================================================
    // 3. WIND SHEAR -- Horizontal particle blade line.
    //    Damage: 8, radius: 10, duration: 30.
    // ================================================================
    public static class WindShear extends EnvironmentalAttack {

        private double dirX, dirZ;
        private double perpX, perpZ;

        public WindShear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wind_shear", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
            perpX = -dirZ;
            perpZ = dirX;
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Blade sweeps forward at high speed
            double offset = -15.0 + ticksAlive * 1.0;
            Location bladeCenter = center.clone().add(dirX * offset, 1.5, dirZ * offset);

            if (ticksAlive % 1 == 0) {
                // Blade is 10 blocks wide, thin
                for (int i = -5; i <= 5; i++) {
                    Location bLoc = bladeCenter.clone().add(perpX * i, 0, perpZ * i);
                    w.spawnParticle(Particle.SWEEP_ATTACK, bLoc, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.DUST, bLoc, 2, 0.1, 0.1, 0.1, 0, WIND_DUST);
                    w.spawnParticle(Particle.CLOUD, bLoc, 1, 0.1, 0, 0.1, 0.01);
                }
            }

            // Damage players hit by blade
            if (ticksAlive % 3 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    double forwardDist = (pLoc.getX() - bladeCenter.getX()) * dirX
                            + (pLoc.getZ() - bladeCenter.getZ()) * dirZ;
                    double sideDist = (pLoc.getX() - bladeCenter.getX()) * perpX
                            + (pLoc.getZ() - bladeCenter.getZ()) * perpZ;
                    if (Math.abs(forwardDist) <= 1.5 && Math.abs(sideDist) <= 5.5
                            && Math.abs(pLoc.getY() - bladeCenter.getY()) < 2) {
                        p.damage(config.getDamage());
                        p.setVelocity(p.getVelocity().add(new Vector(dirX * 0.5, 0.3, dirZ * 0.5)));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new WindShear(plugin); }
    }

    // ================================================================
    // 4. DOWNDRAFT -- Downward push in circle. Slam to ground.
    //    Damage: 8, radius: 6, duration: 20.
    // ================================================================
    public static class Downdraft extends EnvironmentalAttack {

        private boolean slammed = false;

        public Downdraft(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("downdraft", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.3f);
            // Downward particle cone warning
            for (int i = 0; i < 20; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * 6;
                Location pLoc = center.clone().add(Math.cos(angle) * r, 6, Math.sin(angle) * r);
                w.spawnParticle(Particle.CLOUD, pLoc, 1, 0, -0.5, 0, 0.1);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Descending wind column
            if (ticksAlive % 2 == 0) {
                double height = 6.0 - ticksAlive * 0.3;
                if (height < 0) height = 0;
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = Math.random() * 6;
                    Location pLoc = center.clone().add(Math.cos(angle) * r, height + Math.random() * 3, Math.sin(angle) * r);
                    w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, pLoc, 1, 0, -0.3, 0, 0.05);
                    w.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, WIND_DUST);
                }
            }

            // Slam at tick 8
            if (ticksAlive == 8 && !slammed) {
                slammed = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.2f);
                w.spawnParticle(Particle.CLOUD, center, 40, 6, 0.5, 6, 0.1);
                w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center, 20, 5, 0.2, 5, 0.05);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 36) {
                        // Slam downward
                        p.setVelocity(new Vector(p.getVelocity().getX() * 0.3, -1.5, p.getVelocity().getZ() * 0.3));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new Downdraft(plugin); }
    }

    // ================================================================
    // 5. TAILWIND -- Push in one direction fast (velocity 1.0).
    //    Damage: 4, radius: 12, duration: 30.
    // ================================================================
    public static class Tailwind extends EnvironmentalAttack {

        private double dirX, dirZ;

        public Tailwind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tailwind", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.2f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Streaking wind particles in direction
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 15; i++) {
                    double ox = (Math.random() - 0.5) * 24;
                    double oz = (Math.random() - 0.5) * 24;
                    Location pLoc = center.clone().add(ox, Math.random() * 2, oz);
                    w.spawnParticle(Particle.CLOUD, pLoc, 1, dirX * 0.8, 0, dirZ * 0.8, 0.15);
                }
                w.spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 8, 10, 1, 10, 0, WIND_DUST);
            }

            // Strong push every 5 ticks
            if (ticksAlive % 5 == 0) {
                Vector push = new Vector(dirX * 1.0, 0.02, dirZ * 1.0);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 144) {
                        p.setVelocity(p.getVelocity().add(push));
                    }
                }
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new Tailwind(plugin); }
    }

    // ================================================================
    // 6. CROSSWIND -- Sideways push. Disrupts movement.
    //    Damage: 3, radius: 10, duration: 60.
    // ================================================================
    public static class Crosswind extends EnvironmentalAttack {

        private double pushX, pushZ;

        public Crosswind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crosswind", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * Math.PI * 2;
            pushX = Math.cos(angle) * 0.4;
            pushZ = Math.sin(angle) * 0.4;
            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Drifting particles
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 12; i++) {
                    double ox = (Math.random() - 0.5) * 20;
                    double oz = (Math.random() - 0.5) * 20;
                    Location pLoc = center.clone().add(ox, 0.5 + Math.random() * 2, oz);
                    w.spawnParticle(Particle.CLOUD, pLoc, 1, pushX * 0.3, 0, pushZ * 0.3, 0.05);
                }
                w.spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 5, 8, 1, 8, 0, WIND_DUST);
            }

            // Constant side push
            if (ticksAlive % 8 == 0) {
                Vector push = new Vector(pushX, 0, pushZ);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 100) {
                        p.setVelocity(p.getVelocity().add(push));
                    }
                }
            }

            // Direction shifts slightly over time
            if (ticksAlive % 20 == 0) {
                double shift = (Math.random() - 0.5) * 0.2;
                double oldAngle = Math.atan2(pushZ, pushX);
                double newAngle = oldAngle + shift;
                pushX = Math.cos(newAngle) * 0.4;
                pushZ = Math.sin(newAngle) * 0.4;
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new Crosswind(plugin); }
    }

    // ================================================================
    // 7. VORTEX -- Suction + orbital push. Spiral inward.
    //    Damage: 7 at center, radius: 8, duration: 80.
    // ================================================================
    public static class Vortex extends EnvironmentalAttack {

        public Vortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("vortex", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.2f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spiraling inward particle pattern
            if (ticksAlive % 2 == 0) {
                for (int arm = 0; arm < 3; arm++) {
                    for (double r = 1; r <= 8; r += 0.5) {
                        double spiralAngle = ticksAlive * 0.2 + (Math.PI * 2 * arm) / 3 + r * 0.8;
                        Location pLoc = center.clone().add(
                                Math.cos(spiralAngle) * r, 0.5,
                                Math.sin(spiralAngle) * r);
                        w.spawnParticle(Particle.CLOUD, pLoc, 1, 0, 0, 0, 0);
                        if (r % 1.5 < 0.5) {
                            w.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, WIND_DUST);
                        }
                    }
                }
                // Center intensity
                w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center.clone().add(0, 1, 0), 3,
                        0.5, 0.5, 0.5, 0.02);
            }

            // Suction: pull players inward + orbital rotation
            if (ticksAlive % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double distSq = p.getLocation().distanceSquared(center);
                    if (distSq > 64) continue;

                    double dist = Math.sqrt(distSq);
                    if (dist < 0.5) dist = 0.5;

                    Vector toCenter = center.toVector().subtract(p.getLocation().toVector()).normalize();
                    Vector tangent = new Vector(-toCenter.getZ(), 0, toCenter.getX());
                    // Stronger pull when closer
                    double pullStrength = 0.2 + (1.0 - dist / 8.0) * 0.3;
                    Vector combined = toCenter.multiply(pullStrength).add(tangent.multiply(0.3));
                    combined.setY(0.05);
                    p.setVelocity(p.getVelocity().add(combined));

                    // Extra damage at center (within 2 blocks)
                    if (dist <= 2.0 && ticksAlive % 10 == 0) {
                        p.damage(config.getDamage());
                    }
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.7f,
                        0.4f + (float)(ticksAlive * 0.008));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new Vortex(plugin); }
    }

    // ================================================================
    // 8. SONIC BOOM -- Expanding particle ring + loud sound. Knockback outward.
    //    Damage: 8, radius: 12, duration: 15.
    // ================================================================
    public static class SonicBoom extends EnvironmentalAttack {

        private boolean boomed = false;

        public SonicBoom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sonic_boom", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(15);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Brief compression warning
            w.spawnParticle(Particle.DUST, center, 15, 2, 1, 2, 0, WIND_DUST_LARGE);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Boom at tick 3
            if (ticksAlive == 3 && !boomed) {
                boomed = true;

                // Loud layered boom sounds
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.5f, 0.5f);

                // Knockback all players outward
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double distSq = p.getLocation().distanceSquared(center);
                    if (distSq <= 144) {
                        Vector knockback = p.getLocation().toVector().subtract(center.toVector());
                        double dist = Math.sqrt(distSq);
                        if (dist < 0.5) {
                            knockback = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
                        }
                        knockback.normalize();
                        double strength = 1.2 * (1.0 - dist / 12.0);
                        knockback.multiply(strength).setY(0.4);
                        p.setVelocity(p.getVelocity().add(knockback));
                    }
                }
            }

            // Expanding ring particles
            if (ticksAlive >= 3) {
                double radius = (ticksAlive - 3) * 1.5;
                if (radius > 0 && radius <= 15) {
                    int points = (int) (radius * 6);
                    for (int i = 0; i < points; i++) {
                        double angle = (2 * Math.PI * i) / points;
                        Location pLoc = center.clone().add(Math.cos(angle) * radius, 1, Math.sin(angle) * radius);
                        w.spawnParticle(Particle.SWEEP_ATTACK, pLoc, 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.CLOUD, pLoc, 1, 0, 0, 0, 0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new SonicBoom(plugin); }
    }

    // ================================================================
    // 9. WHISPER -- No visible warning until last second.
    //    Sudden damage at player position. Audio-only warning (quiet wind).
    //    Damage: 10, radius: 3, duration: 40.
    // ================================================================
    public static class Whisper extends EnvironmentalAttack {

        private boolean struck = false;

        public Whisper(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("whisper", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(999); // Manual
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Very quiet wind whisper -- barely audible warning
            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.15f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Gradually increasing whisper sounds -- no particles
            if (ticksAlive == 10) {
                DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.2f, 1.2f);
            }
            if (ticksAlive == 20) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.3f, 1.5f);
            }
            if (ticksAlive == 30) {
                DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.5f, 0.8f);
            }

            // At tick 35: sudden visible strike at player locations
            if (ticksAlive == 35 && !struck) {
                struck = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.5f);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 9) {
                        p.damage(config.getDamage());
                        // Brief burst at player location
                        w.spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(0, 1, 0), 8,
                                0.5, 0.5, 0.5, 0);
                        w.spawnParticle(Particle.CLOUD, p.getLocation().add(0, 1, 0), 10,
                                0.5, 0.5, 0.5, 0.1);
                        w.spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 5,
                                0.3, 0.3, 0.3, 0, WIND_DUST_LARGE);
                        p.setVelocity(p.getVelocity().add(new Vector(0, 0.3, 0)));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new Whisper(plugin); }
    }

    // ================================================================
    // 10. DUST DEVIL -- Small particle tornado wandering randomly.
    //     Damage on contact. Damage: 6, radius: 3, duration: 120.
    // ================================================================
    public static class DustDevil extends EnvironmentalAttack {

        private double devilX, devilZ;
        private double wanderDirX, wanderDirZ;

        public DustDevil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dust_devil", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(999); // Manual
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            devilX = center.getX();
            devilZ = center.getZ();
            double angle = Math.random() * Math.PI * 2;
            wanderDirX = Math.cos(angle) * 0.2;
            wanderDirZ = Math.sin(angle) * 0.2;
            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Wander randomly
            devilX += wanderDirX;
            devilZ += wanderDirZ;

            // Random direction changes
            if (ticksAlive % 15 == 0) {
                wanderDirX += (Math.random() - 0.5) * 0.15;
                wanderDirZ += (Math.random() - 0.5) * 0.15;
                double speed = Math.sqrt(wanderDirX * wanderDirX + wanderDirZ * wanderDirZ);
                if (speed > 0.3) {
                    wanderDirX = wanderDirX / speed * 0.3;
                    wanderDirZ = wanderDirZ / speed * 0.3;
                }
            }

            // Keep within 15 blocks of original center
            double dx = devilX - center.getX();
            double dz = devilZ - center.getZ();
            if (dx * dx + dz * dz > 225) {
                wanderDirX = -dx * 0.05;
                wanderDirZ = -dz * 0.05;
            }

            Location devilLoc = new Location(w, devilX, center.getY(), devilZ);

            // Small tornado particles
            if (ticksAlive % 2 == 0) {
                for (double y = 0; y < 4; y += 0.3) {
                    double radius = 1.5 * (1.0 - y / 5.0);
                    double angle = y * 3.0 + ticksAlive * 0.5;
                    Location pLoc = devilLoc.clone().add(
                            Math.cos(angle) * radius, y,
                            Math.sin(angle) * radius);
                    w.spawnParticle(Particle.CLOUD, pLoc, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, WIND_DUST);
                }
                w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, devilLoc.clone().add(0, 3, 0),
                        2, 0.3, 0.3, 0.3, 0.02);
            }

            // Damage on contact
            if (ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(devilLoc) <= 9) {
                        p.damage(config.getDamage());
                        // Fling upward + outward
                        Vector fling = p.getLocation().toVector().subtract(devilLoc.toVector());
                        if (fling.lengthSquared() > 0) fling.normalize().multiply(0.5);
                        fling.setY(0.5);
                        p.setVelocity(p.getVelocity().add(fling));
                    }
                }
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(devilLoc, Sound.ENTITY_PHANTOM_FLAP, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new DustDevil(plugin); }
    }

    // ================================================================
    // 11. JET STREAM -- Fast particle line at Y+3. Horizontal push.
    //     Damage: 5, radius: 8, duration: 40.
    // ================================================================
    public static class JetStream extends EnvironmentalAttack {

        private double dirX, dirZ;
        private double perpX, perpZ;

        public JetStream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("jet_stream", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
            perpX = -dirZ;
            perpZ = dirX;
            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.2f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Fast-moving particle line at Y+3
            double offset = -20.0 + ticksAlive * 1.2;

            if (ticksAlive % 1 == 0) {
                Location streamCenter = center.clone().add(dirX * offset, 3, dirZ * offset);

                // Stream is 8 blocks wide
                for (int i = -4; i <= 4; i++) {
                    Location sLoc = streamCenter.clone().add(perpX * i, 0, perpZ * i);
                    w.spawnParticle(Particle.CLOUD, sLoc, 2, dirX * 0.5, 0, dirZ * 0.5, 0.1);
                    w.spawnParticle(Particle.DUST, sLoc, 1, 0, 0, 0, 0, WIND_DUST);
                    // Trail behind
                    Location trail = sLoc.clone().add(-dirX * 2, 0, -dirZ * 2);
                    w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, trail, 1, 0.3, 0, 0.3, 0.01);
                }

                // Push + damage players in stream path
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    double forwardDist = (pLoc.getX() - streamCenter.getX()) * dirX
                            + (pLoc.getZ() - streamCenter.getZ()) * dirZ;
                    double sideDist = (pLoc.getX() - streamCenter.getX()) * perpX
                            + (pLoc.getZ() - streamCenter.getZ()) * perpZ;
                    if (Math.abs(forwardDist) <= 2 && Math.abs(sideDist) <= 4
                            && Math.abs(pLoc.getY() - streamCenter.getY()) < 3) {
                        p.setVelocity(p.getVelocity().add(new Vector(dirX * 0.6, 0.1, dirZ * 0.6)));
                    }
                }
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.6f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new JetStream(plugin); }
    }

    // ================================================================
    // 12. CALM BEFORE STORM -- Silence for 60 ticks, then massive AoE burst.
    //     Damage: 12 on burst, radius: 15, duration: 80.
    // ================================================================
    public static class CalmBeforeStorm extends EnvironmentalAttack {

        private boolean burstFired = false;

        public CalmBeforeStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calm_before_storm", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(12.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(999); // Manual
        }

        @Override
        protected void onSpawn(Location center) {
            // Intentionally silent and invisible on spawn
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Phase 1: Eerie calm (0-55) -- almost nothing
            if (ticksAlive < 55) {
                // Very rare, subtle ambient particle
                if (ticksAlive % 20 == 0) {
                    w.spawnParticle(Particle.DUST, center.clone().add(0, 2, 0), 2, 5, 1, 5, 0, WIND_DUST);
                }
                return;
            }

            // Phase 2: Warning buildup (55-60)
            if (ticksAlive >= 55 && ticksAlive < 60) {
                DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.3f + (ticksAlive - 55) * 0.15f, 0.5f);
                w.spawnParticle(Particle.CLOUD, center.clone().add(0, 1, 0),
                        (ticksAlive - 55) * 5, 10, 2, 10, 0.02);
            }

            // Phase 3: STORM BURST at tick 60
            if (ticksAlive == 60 && !burstFired) {
                burstFired = true;

                // Massive sound
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 2.0f, 0.3f);

                // Massive particle explosion
                w.spawnParticle(Particle.CLOUD, center.clone().add(0, 2, 0), 120, 15, 4, 15, 0.2);
                w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center.clone().add(0, 3, 0), 60,
                        12, 3, 12, 0.15);
                w.spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(0, 1, 0), 30, 10, 2, 10, 0);
                DisplayBuilder.dustParticles(center, 80, 15.0, 200, 200, 220, 2.0f);

                // Damage + massive knockback
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 225) {
                        p.damage(config.getDamage());
                        Vector knockback = p.getLocation().toVector().subtract(center.toVector());
                        if (knockback.lengthSquared() > 0) {
                            knockback.normalize().multiply(1.5).setY(0.6);
                        } else {
                            knockback = new Vector(0, 0.6, 0);
                        }
                        p.setVelocity(p.getVelocity().add(knockback));
                    }
                }
            }

            // Phase 4: Aftermath turbulence (60-80)
            if (ticksAlive > 60) {
                if (ticksAlive % 4 == 0) {
                    w.spawnParticle(Particle.CLOUD, center.clone().add(0, 1, 0), 10, 12, 2, 12, 0.05);
                    w.spawnParticle(Particle.DUST, center.clone().add(0, 1, 0), 5, 10, 2, 10, 0, WIND_DUST);
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CalmBeforeStorm(plugin); }
    }

    // ================================================================
    // 13. HURRICANE EYE -- Safe center, damage in ring. Ring contracts.
    //     Damage: 6 in ring, radius: 10 shrinking, duration: 120.
    // ================================================================
    public static class HurricaneEye extends EnvironmentalAttack {

        public HurricaneEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hurricane_eye", AttackType.ENVIRONMENTAL, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(999); // Manual
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ring contracts from radius 10 to radius 2 over duration
            double progress = (double) ticksAlive / config.getDurationTicks();
            double outerRadius = 10.0 - progress * 8.0; // 10 -> 2
            double innerRadius = Math.max(outerRadius - 3.0, 0.5); // Ring is 3 blocks thick
            double safeRadius = innerRadius; // Center of eye is safe

            // Draw the hurricane wall
            if (ticksAlive % 2 == 0) {
                // Outer wall particles (spinning)
                int outerPoints = (int) (outerRadius * 8);
                for (int i = 0; i < outerPoints; i++) {
                    double angle = (2 * Math.PI * i) / outerPoints + ticksAlive * 0.15;
                    for (double y = 0; y < 4; y += 0.8) {
                        Location pLoc = center.clone().add(
                                Math.cos(angle) * outerRadius, y,
                                Math.sin(angle) * outerRadius);
                        w.spawnParticle(Particle.CLOUD, pLoc, 1, 0.2, 0, 0.2, 0.02);
                    }
                }

                // Inner wall particles
                int innerPoints = (int) (innerRadius * 6);
                for (int i = 0; i < innerPoints; i++) {
                    double angle = (2 * Math.PI * i) / Math.max(innerPoints, 1) - ticksAlive * 0.1;
                    Location pLoc = center.clone().add(
                            Math.cos(angle) * innerRadius, 1,
                            Math.sin(angle) * innerRadius);
                    w.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, WIND_DUST);
                }

                // Between walls: heavy wind
                double midRadius = (outerRadius + innerRadius) / 2;
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double r = innerRadius + Math.random() * (outerRadius - innerRadius);
                    Location pLoc = center.clone().add(
                            Math.cos(angle) * r, Math.random() * 3,
                            Math.sin(angle) * r);
                    w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, pLoc, 1, 0.5, 0.2, 0.5, 0.05);
                }
            }

            // Damage players in the ring (between inner and outer radius)
            if (ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist >= safeRadius && dist <= outerRadius + 1) {
                        p.damage(config.getDamage());
                        // Orbital push
                        double px = p.getLocation().getX() - center.getX();
                        double pz = p.getLocation().getZ() - center.getZ();
                        double pAngle = Math.atan2(pz, px);
                        Vector tangent = new Vector(-Math.sin(pAngle) * 0.5, 0.1, Math.cos(pAngle) * 0.5);
                        p.setVelocity(p.getVelocity().add(tangent));
                    }
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.6f,
                        0.5f + (float)(progress * 0.8));
            }

            // Warning: ring getting small
            if (progress > 0.7 && ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HurricaneEye(plugin); }
    }
}
