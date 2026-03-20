package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Frozen Mob Effects — 13 ice-creature-themed environmental attacks.
 * Particle + damage based (no block displays). Uses frost color palette:
 * Primary: RGB(100,180,255), Secondary: RGB(220,240,255).
 */
public final class FrozenMobEffects {
    private FrozenMobEffects() {}

    // Ice color palette
    private static final int ICE_R = 100, ICE_G = 180, ICE_B = 255;
    private static final int FROST_R = 220, FROST_G = 240, FROST_B = 255;

    /** Damage all non-exempt players within radius of a location. */
    private static void dealDamage(AbstractAttack attack, Location loc, double radius, double damage) {
        if (loc == null || loc.getWorld() == null) return;
        double r2 = radius * radius;
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.hasPermission("chaoscraft.mode.exempt")) continue;
            if (p.getLocation().distanceSquared(loc) <= r2) {
                p.damage(damage);
                p.setNoDamageTicks(0);
            }
        }
    }

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FrostSwarm(plugin));
        registry.register(new IceWraithPass(plugin));
        registry.register(new CrystalBeastRoar(plugin));
        registry.register(new FrostBiteAttack(plugin));
        registry.register(new GlacialCharge(plugin));
        registry.register(new IceSpiderWeb(plugin));
        registry.register(new FrostHowl(plugin));
        registry.register(new CrystallineScream(plugin));
        registry.register(new IceBreath(plugin));
        registry.register(new PermafrostStomp(plugin));
        registry.register(new FrostHunterDash(plugin));
        registry.register(new GlacialRoarWave(plugin));
        registry.register(new IceStalkerAmbush(plugin));
    }

    // ========================================================================
    // 1. FROST SWARM — 5 small damage zones orbiting center at radius 6
    // ========================================================================
    public static class FrostSwarm extends EnvironmentalAttack {
        private Location center;
        private final double[] offsets = new double[5]; // phase offsets per swarm member

        public FrostSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_swarm_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            // Each swarm member gets an evenly spaced angular offset plus a small random nudge
            for (int i = 0; i < 5; i++) {
                offsets[i] = (2 * Math.PI * i) / 5 + (Math.random() * 0.4 - 0.2);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BEE_LOOP, 0.7f, 1.8f);
            DisplayBuilder.dustParticles(center, 40, 3.0, ICE_R, ICE_G, ICE_B, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double orbitRadius = 6.0;
            double speed = 0.06; // radians per tick

            for (int i = 0; i < 5; i++) {
                // Each zone orbits independently — slightly different speeds
                double angle = offsets[i] + tick * (speed + i * 0.008);
                // Wobble the radius slightly per-zone
                double r = orbitRadius + Math.sin(tick * 0.1 + i) * 0.8;
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                Location zoneLoc = c.clone().add(x, 0.5, z);

                // Frost particles at each swarm zone
                DisplayBuilder.dustParticles(zoneLoc, 8, 0.6, ICE_R, ICE_G, ICE_B, 1.2f);
                DisplayBuilder.dustParticles(zoneLoc, 4, 0.3, FROST_R, FROST_G, FROST_B, 0.8f);
                w.spawnParticle(Particle.SNOWFLAKE, zoneLoc, 3, 0.4, 0.3, 0.4, 0.01);

                // Damage at each zone (1.5 block radius per zone)
                if (tick % 5 == 0) {
                    dealDamage(this, zoneLoc, 1.5, config.getDamage() * 0.4);
                }
            }

            // Ambient buzzing/swarming sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_BEE_LOOP, 0.4f, 2.0f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostSwarm(plugin); }
    }

    // ========================================================================
    // 2. ICE WRAITH PASS — Fast damage line sweeps through area, frost trail
    // ========================================================================
    public static class IceWraithPass extends EnvironmentalAttack {
        private Location center;
        private double sweepAngle;    // direction the wraith travels
        private Location sweepStart;
        private double sweepDirX, sweepDirZ;

        public IceWraithPass(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_wraith_pass", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(60); // fast sweep — 3 seconds
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            // Random sweep direction
            sweepAngle = Math.random() * 2 * Math.PI;
            sweepDirX = Math.cos(sweepAngle);
            sweepDirZ = Math.sin(sweepAngle);
            // Start 12 blocks behind center in sweep direction
            sweepStart = center.clone().add(-sweepDirX * 12, 0, -sweepDirZ * 12);

            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Wraith position moves fast across the arena (24 blocks in 60 ticks)
            double progress = (double) tick / config.getDurationTicks();
            double dist = progress * 24.0;
            Location wraithPos = sweepStart.clone().add(sweepDirX * dist, 1.0, sweepDirZ * dist);

            // Main wraith body particles — bright ice
            DisplayBuilder.dustParticles(wraithPos, 20, 0.8, ICE_R, ICE_G, ICE_B, 1.8f);
            DisplayBuilder.dustParticles(wraithPos, 10, 0.5, FROST_R, FROST_G, FROST_B, 1.3f);
            w.spawnParticle(Particle.SOUL, wraithPos, 3, 0.3, 0.5, 0.3, 0.02);

            // Frost trail behind wraith (particles linger)
            for (int i = 1; i <= 4; i++) {
                Location trail = wraithPos.clone().add(-sweepDirX * i * 0.8, -0.5, -sweepDirZ * i * 0.8);
                DisplayBuilder.dustParticles(trail, 5, 0.6, FROST_R, FROST_G, FROST_B, 1.0f);
                w.spawnParticle(Particle.SNOWFLAKE, trail, 2, 0.5, 0.2, 0.5, 0.005);
            }

            // Damage along wraith path (2 block radius)
            if (tick % 2 == 0) {
                dealDamage(this, wraithPos, 2.0, config.getDamage() * 0.5);
            }

            // Whooshing sound as it passes
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(wraithPos, Sound.ENTITY_PHANTOM_FLAP, 0.8f, 0.6f);
            }
        }

        @Override public AbstractAttack newInstance() { return new IceWraithPass(plugin); }
    }

    // ========================================================================
    // 3. CRYSTAL BEAST ROAR — Loud roar + expanding ring of frost damage
    // ========================================================================
    public static class CrystalBeastRoar extends EnvironmentalAttack {
        private Location center;

        public CrystalBeastRoar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_beast_roar", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(80); // 4 seconds for ring to expand
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            // Loud roar
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 1.0f, 1.8f);
            // Initial burst at center
            DisplayBuilder.dustParticles(center, 60, 1.0, ICE_R, ICE_G, ICE_B, 2.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Ring expands from 0 to 8 blocks over duration
            double maxRadius = 8.0;
            double ringRadius = (maxRadius * tick) / config.getDurationTicks();
            double ringThickness = 1.5;
            int points = Math.max(16, (int) (ringRadius * 10));

            // Draw frost particle ring
            Particle.DustOptions iceOpt = new Particle.DustOptions(Color.fromRGB(ICE_R, ICE_G, ICE_B), 1.6f);
            Particle.DustOptions frostOpt = new Particle.DustOptions(Color.fromRGB(FROST_R, FROST_G, FROST_B), 1.2f);
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI * i) / points;
                double x = Math.cos(angle) * ringRadius;
                double z = Math.sin(angle) * ringRadius;
                Location ringPoint = c.clone().add(x, 0.3, z);
                w.spawnParticle(Particle.DUST, ringPoint, 2, 0.2, 0.3, 0.2, 0, iceOpt);
                if (i % 3 == 0) {
                    w.spawnParticle(Particle.DUST, ringPoint.clone().add(0, 0.5, 0), 1, 0.1, 0.2, 0.1, 0, frostOpt);
                }
            }
            w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 1, 0), 5, ringRadius * 0.5, 0.5, ringRadius * 0.5, 0.01);

            // Damage players on the expanding ring edge (within ring thickness)
            if (tick % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(c);
                    if (dist >= ringRadius - ringThickness && dist <= ringRadius + ringThickness) {
                        p.damage(config.getDamage() * 0.35);
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Rumble sound as ring expands
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new CrystalBeastRoar(plugin); }
    }

    // ========================================================================
    // 4. FROST BITE ATTACK — Precise high-damage hit at target player
    // ========================================================================
    public static class FrostBiteAttack extends EnvironmentalAttack {
        private Location center;
        private boolean bitten = false;

        public FrostBiteAttack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_bite_attack", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(40); // short, precise attack
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.bitten = false;
            // Warning particles converge on center
            DisplayBuilder.dustParticles(center, 20, 2.0, ICE_R, ICE_G, ICE_B, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            Player target = getTargetPlayer();
            if (target == null || !target.isOnline()) return;
            Location targetLoc = target.getLocation();

            if (tick < 20) {
                // Wind-up phase: converging ice particles toward target
                double convergeFactor = 1.0 - (tick / 20.0);
                double spread = 4.0 * convergeFactor;
                for (int i = 0; i < 12; i++) {
                    double ox = (Math.random() - 0.5) * spread * 2;
                    double oy = Math.random() * spread;
                    double oz = (Math.random() - 0.5) * spread * 2;
                    Location particleLoc = targetLoc.clone().add(ox, oy + 0.5, oz);
                    // Particles drift toward target
                    double dx = (targetLoc.getX() - particleLoc.getX()) * 0.05;
                    double dy = (targetLoc.getY() + 1.0 - particleLoc.getY()) * 0.05;
                    double dz = (targetLoc.getZ() - particleLoc.getZ()) * 0.05;
                    w.spawnParticle(Particle.DUST, particleLoc, 1, dx, dy, dz, 0.3,
                            new Particle.DustOptions(Color.fromRGB(ICE_R, ICE_G, ICE_B), 1.2f));
                }
                // Ominous ticking
                if (tick % 5 == 0) {
                    DisplayBuilder.playSound(targetLoc, Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.5f + tick * 0.05f);
                }
            } else if (!bitten) {
                // BITE — single massive hit at tick 20
                bitten = true;
                Location biteLoc = targetLoc.clone().add(0, 0.5, 0);

                // Jaw-snap sound
                DisplayBuilder.playSound(biteLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.5f);
                DisplayBuilder.playSound(biteLoc, Sound.BLOCK_BONE_BLOCK_BREAK, 1.2f, 0.8f);

                // Sharp burst of frost particles
                DisplayBuilder.dustParticles(biteLoc, 60, 1.0, FROST_R, FROST_G, FROST_B, 2.0f);
                DisplayBuilder.dustParticles(biteLoc, 30, 0.5, ICE_R, ICE_G, ICE_B, 2.5f);
                w.spawnParticle(Particle.CRIT, biteLoc, 20, 0.5, 0.5, 0.5, 0.3);

                // High damage — full damage in one hit
                dealDamage(this, biteLoc, 2.0, config.getDamage());
            } else {
                // Post-bite: lingering frost particles dissipate
                DisplayBuilder.dustParticles(targetLoc, 5, 1.5, FROST_R, FROST_G, FROST_B, 0.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostBiteAttack(plugin); }
    }

    // ========================================================================
    // 5. GLACIAL CHARGE — Damage zone starts 15 blocks away, charges to player
    // ========================================================================
    public static class GlacialCharge extends EnvironmentalAttack {
        private Location center;
        private Location chargeStart;
        private Location chargeTarget;
        private double chargeDirX, chargeDirZ;

        public GlacialCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_charge", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(60); // fast charge
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            Player target = getTargetPlayer();
            if (target != null && target.isOnline()) {
                chargeTarget = target.getLocation().clone();
            } else {
                chargeTarget = center.clone();
            }
            // Start 15 blocks away in a random direction
            double angle = Math.random() * 2 * Math.PI;
            chargeDirX = Math.cos(angle);
            chargeDirZ = Math.sin(angle);
            chargeStart = chargeTarget.clone().add(chargeDirX * 15, 0, chargeDirZ * 15);
            // Reverse direction — charge points FROM start TO target
            chargeDirX = (chargeTarget.getX() - chargeStart.getX());
            chargeDirZ = (chargeTarget.getZ() - chargeStart.getZ());
            double len = Math.sqrt(chargeDirX * chargeDirX + chargeDirZ * chargeDirZ);
            if (len > 0) { chargeDirX /= len; chargeDirZ /= len; }

            // Distant roar
            DisplayBuilder.playSound(chargeStart, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.5f);
            DisplayBuilder.dustParticles(chargeStart, 30, 1.5, ICE_R, ICE_G, ICE_B, 2.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null || chargeStart == null) return;
            World w = c.getWorld();

            // Progress: 0 to 1 over the duration
            double progress = (double) tick / config.getDurationTicks();
            // Accelerating charge — ease-in
            double easedProgress = progress * progress;
            double dist = easedProgress * 15.0;
            Location chargePos = chargeStart.clone().add(chargeDirX * dist, 0.5, chargeDirZ * dist);

            // Charging beast particles — big ice cloud
            DisplayBuilder.dustParticles(chargePos, 25, 1.2, ICE_R, ICE_G, ICE_B, 2.0f);
            DisplayBuilder.dustParticles(chargePos, 15, 0.8, FROST_R, FROST_G, FROST_B, 1.5f);
            w.spawnParticle(Particle.CLOUD, chargePos, 5, 0.6, 0.3, 0.6, 0.02);

            // Ground frost trail
            for (int i = 1; i <= 3; i++) {
                Location trail = chargePos.clone().add(-chargeDirX * i * 0.7, -0.3, -chargeDirZ * i * 0.7);
                DisplayBuilder.dustParticles(trail, 4, 0.5, FROST_R, FROST_G, FROST_B, 0.9f);
                w.spawnParticle(Particle.SNOWFLAKE, trail, 2, 0.3, 0.1, 0.3, 0.005);
            }

            // Damage at charge position (2.5 block radius)
            if (tick % 3 == 0) {
                dealDamage(this, chargePos, 2.5, config.getDamage() * 0.4);
            }

            // Stomping sound
            if (tick % 8 == 0) {
                DisplayBuilder.playSound(chargePos, Sound.ENTITY_IRON_GOLEM_STEP, 1.0f, 0.5f);
            }

            // Impact burst at end
            if (tick == config.getDurationTicks() - 1) {
                DisplayBuilder.playSound(chargePos, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
                DisplayBuilder.dustParticles(chargePos, 80, 2.0, ICE_R, ICE_G, ICE_B, 2.5f);
                w.spawnParticle(Particle.EXPLOSION, chargePos, 3, 0.5, 0.5, 0.5, 0);
                dealDamage(this, chargePos, 3.5, config.getDamage() * 0.6);
            }
        }

        @Override public AbstractAttack newInstance() { return new GlacialCharge(plugin); }
    }

    // ========================================================================
    // 6. ICE SPIDER WEB — Expanding lattice of damage lines in 8 directions
    // ========================================================================
    public static class IceSpiderWeb extends EnvironmentalAttack {
        private Location center;

        public IceSpiderWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_spider_web", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 0.4f);
            DisplayBuilder.dustParticles(center, 30, 0.5, FROST_R, FROST_G, FROST_B, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Web extends outward over time — max 7 blocks
            double maxLen = 7.0;
            double currentLen = Math.min(maxLen, (maxLen * tick) / 60.0); // fully extended by tick 60

            Particle.DustOptions iceOpt = new Particle.DustOptions(Color.fromRGB(ICE_R, ICE_G, ICE_B), 1.0f);
            Particle.DustOptions frostOpt = new Particle.DustOptions(Color.fromRGB(FROST_R, FROST_G, FROST_B), 0.8f);

            // 8 web strands: N, NE, E, SE, S, SW, W, NW
            double[] dirs = {0, Math.PI/4, Math.PI/2, 3*Math.PI/4, Math.PI, 5*Math.PI/4, 3*Math.PI/2, 7*Math.PI/4};

            for (double dir : dirs) {
                double dx = Math.cos(dir);
                double dz = Math.sin(dir);

                // Draw particles along each strand
                int pointsPerStrand = (int) (currentLen * 3);
                for (int p = 0; p <= pointsPerStrand; p++) {
                    double dist = (currentLen * p) / Math.max(1, pointsPerStrand);
                    Location point = c.clone().add(dx * dist, 0.3, dz * dist);
                    w.spawnParticle(Particle.DUST, point, 1, 0.05, 0.05, 0.05, 0, iceOpt);
                }
            }

            // Cross-web connecting rings at intervals
            if (currentLen > 2.0 && tick % 4 == 0) {
                for (double ringR = 2.0; ringR <= currentLen; ringR += 2.5) {
                    int ringPoints = (int) (ringR * 6);
                    for (int i = 0; i < ringPoints; i++) {
                        double angle = (2 * Math.PI * i) / ringPoints;
                        Location rp = c.clone().add(Math.cos(angle) * ringR, 0.3, Math.sin(angle) * ringR);
                        w.spawnParticle(Particle.DUST, rp, 1, 0.03, 0.02, 0.03, 0, frostOpt);
                    }
                }
            }

            // Snowflake ambient
            if (tick % 3 == 0) {
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 0.5, 0), 3, currentLen * 0.4, 0.2, currentLen * 0.4, 0.005);
            }

            // Damage along web strands
            if (tick % 6 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    double distFromCenter = pLoc.distance(c);
                    if (distFromCenter > currentLen + 0.5) continue;

                    // Check if player is near any strand (within 0.8 blocks)
                    for (double dir : dirs) {
                        double dx = Math.cos(dir);
                        double dz = Math.sin(dir);
                        // Project player onto strand line
                        double projDist = (pLoc.getX() - c.getX()) * dx + (pLoc.getZ() - c.getZ()) * dz;
                        if (projDist < 0 || projDist > currentLen) continue;
                        double closestX = c.getX() + dx * projDist;
                        double closestZ = c.getZ() + dz * projDist;
                        double distToLine = Math.sqrt(Math.pow(pLoc.getX() - closestX, 2) + Math.pow(pLoc.getZ() - closestZ, 2));
                        if (distToLine <= 0.8) {
                            p.damage(config.getDamage() * 0.25);
                            p.setNoDamageTicks(0);
                            break; // only damage once per tick per player
                        }
                    }
                }
            }

            // Skittering spider sounds
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_SPIDER_STEP, 0.6f, 1.2f);
            }
        }

        @Override public AbstractAttack newInstance() { return new IceSpiderWeb(plugin); }
    }

    // ========================================================================
    // 7. FROST HOWL — Howling wind + directional cone of damage from random dir
    // ========================================================================
    public static class FrostHowl extends EnvironmentalAttack {
        private Location center;
        private double coneAngle; // direction the howl blows from
        private double coneDirX, coneDirZ;

        public FrostHowl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_howl", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            coneAngle = Math.random() * 2 * Math.PI;
            coneDirX = Math.cos(coneAngle);
            coneDirZ = Math.sin(coneAngle);

            // Howling wind
            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_HOWL, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.8f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double coneLength = 8.0;
            double coneHalfAngle = Math.PI / 5; // ~36 degree half angle (~72 degree cone)

            Particle.DustOptions iceOpt = new Particle.DustOptions(Color.fromRGB(ICE_R, ICE_G, ICE_B), 1.5f);
            Particle.DustOptions frostOpt = new Particle.DustOptions(Color.fromRGB(FROST_R, FROST_G, FROST_B), 1.0f);

            // Wind particles flowing through the cone
            for (int i = 0; i < 15; i++) {
                // Random position within cone
                double dist = Math.random() * coneLength;
                double spreadAngle = coneAngle + (Math.random() - 0.5) * 2 * coneHalfAngle;
                double px = Math.cos(spreadAngle) * dist;
                double pz = Math.sin(spreadAngle) * dist;
                double py = Math.random() * 2.5;
                Location partLoc = c.clone().add(px, py, pz);

                // Wind direction particles
                w.spawnParticle(Particle.DUST, partLoc, 1, coneDirX * 0.3, 0.05, coneDirZ * 0.3, 0.2, iceOpt);
                if (i % 3 == 0) {
                    w.spawnParticle(Particle.DUST, partLoc, 1, 0.1, 0.1, 0.1, 0, frostOpt);
                }
            }

            // Snowflake stream
            w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(coneDirX * 4, 1.2, coneDirZ * 4), 8,
                    2.5, 1.0, 2.5, 0.03);

            // Damage players within cone
            if (tick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    double dx = pLoc.getX() - c.getX();
                    double dz = pLoc.getZ() - c.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > coneLength || dist < 0.5) continue;

                    // Check if within cone angle
                    double dot = (dx * coneDirX + dz * coneDirZ) / dist;
                    if (dot >= Math.cos(coneHalfAngle)) {
                        p.damage(config.getDamage() * 0.3);
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Ongoing howl
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_HOWL, 1.0f, 0.4f);
            }
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.5f, 0.2f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostHowl(plugin); }
    }

    // ========================================================================
    // 8. CRYSTALLINE SCREAM — Glass break sound + spherical damage burst
    // ========================================================================
    public static class CrystallineScream extends EnvironmentalAttack {
        private Location center;
        private boolean screamed = false;

        public CrystallineScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_scream", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.screamed = false;
            // Pre-scream buildup particles
            DisplayBuilder.dustParticles(center, 15, 0.5, ICE_R, ICE_G, ICE_B, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < 25) {
                // Buildup phase — particles gathering inward, pitch rising
                double spread = 5.0 * (1.0 - tick / 25.0);
                int count = 8 + tick;
                for (int i = 0; i < count; i++) {
                    double ox = (Math.random() - 0.5) * spread * 2;
                    double oy = (Math.random() - 0.5) * spread;
                    double oz = (Math.random() - 0.5) * spread * 2;
                    Location partLoc = c.clone().add(ox, oy + 1.5, oz);
                    DisplayBuilder.dustParticles(partLoc, 1, 0.05, FROST_R, FROST_G, FROST_B, 0.8f + tick * 0.03f);
                }
                // Rising pitch sound
                if (tick % 5 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 0.5f + tick * 0.06f);
                }
            } else if (!screamed) {
                // THE SCREAM — tick 25
                screamed = true;

                // Glass shatter sounds
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.8f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 1.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 2.0f);

                // Spherical burst — particles in all directions
                double burstRadius = 7.0;
                int burstPoints = 80;
                double goldenAngle = Math.PI * (3 - Math.sqrt(5));
                for (int i = 0; i < burstPoints; i++) {
                    double y = 1 - (2.0 * i / (burstPoints - 1));
                    double rAtY = Math.sqrt(1 - y * y);
                    double theta = goldenAngle * i;
                    double bx = Math.cos(theta) * rAtY * burstRadius;
                    double bz = Math.sin(theta) * rAtY * burstRadius;
                    Location bp = c.clone().add(bx, y * burstRadius + 1.5, bz);
                    DisplayBuilder.dustParticles(bp, 3, 0.2, ICE_R, ICE_G, ICE_B, 2.0f);
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 100, 3.0, FROST_R, FROST_G, FROST_B, 2.5f);

                // Spherical damage — radius 6
                dealDamage(this, c.clone().add(0, 1.0, 0), 6.0, config.getDamage() * 0.7);
            } else {
                // Post-scream: fading shards
                int remaining = config.getDurationTicks() - tick;
                double fade = (double) remaining / (config.getDurationTicks() - 25);
                int shards = (int) (12 * fade);
                for (int i = 0; i < shards; i++) {
                    double ox = (Math.random() - 0.5) * 8;
                    double oy = Math.random() * 4;
                    double oz = (Math.random() - 0.5) * 8;
                    Location shardLoc = c.clone().add(ox, oy, oz);
                    DisplayBuilder.dustParticles(shardLoc, 2, 0.2, FROST_R, FROST_G, FROST_B, 0.6f);
                }
                w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(0, 2, 0), 3, 3.0, 1.5, 3.0, 0.01);
            }
        }

        @Override public AbstractAttack newInstance() { return new CrystallineScream(plugin); }
    }

    // ========================================================================
    // 9. ICE BREATH — Cone-shaped frost + damage from center aimed at player
    // ========================================================================
    public static class IceBreath extends EnvironmentalAttack {
        private Location center;
        private double breathDirX, breathDirZ;

        public IceBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_breath", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            Player target = getTargetPlayer();
            if (target != null && target.isOnline()) {
                Location tLoc = target.getLocation();
                double dx = tLoc.getX() - center.getX();
                double dz = tLoc.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0) { breathDirX = dx / len; breathDirZ = dz / len; }
                else { breathDirX = 1; breathDirZ = 0; }
            } else {
                double angle = Math.random() * 2 * Math.PI;
                breathDirX = Math.cos(angle);
                breathDirZ = Math.sin(angle);
            }

            // Inhale sound
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            double breathLength = 8.0;
            double coneHalfAngle = Math.PI / 6; // 30 degree half cone
            double breathAngle = Math.atan2(breathDirZ, breathDirX);

            // Intensity ramps up and down over duration
            double intensity;
            if (tick < 20) {
                intensity = tick / 20.0; // ramp up
            } else if (tick > config.getDurationTicks() - 20) {
                intensity = (config.getDurationTicks() - tick) / 20.0; // ramp down
            } else {
                intensity = 1.0;
            }

            Particle.DustOptions iceOpt = new Particle.DustOptions(Color.fromRGB(ICE_R, ICE_G, ICE_B), 1.5f * (float) intensity);
            Particle.DustOptions frostOpt = new Particle.DustOptions(Color.fromRGB(FROST_R, FROST_G, FROST_B), 1.2f * (float) intensity);

            // Breath particles flowing in cone
            int particleCount = (int) (20 * intensity);
            for (int i = 0; i < particleCount; i++) {
                double dist = Math.random() * breathLength;
                double spreadAngle = breathAngle + (Math.random() - 0.5) * 2 * coneHalfAngle;
                double px = Math.cos(spreadAngle) * dist;
                double pz = Math.sin(spreadAngle) * dist;
                double py = Math.random() * 1.5 + 0.3;
                Location partLoc = c.clone().add(px, py, pz);

                // Directional velocity particles
                w.spawnParticle(Particle.DUST, partLoc, 1, breathDirX * 0.2, 0, breathDirZ * 0.2, 0.15, iceOpt);
                if (i % 4 == 0) {
                    w.spawnParticle(Particle.DUST, partLoc, 1, 0.05, 0.05, 0.05, 0, frostOpt);
                }
            }

            // Snowflakes streaming
            w.spawnParticle(Particle.SNOWFLAKE, c.clone().add(breathDirX * 4, 1, breathDirZ * 4),
                    (int) (6 * intensity), 2.0, 0.5, 2.0, 0.04);

            // Mouth frost at center
            DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), (int) (5 * intensity), 0.3, FROST_R, FROST_G, FROST_B, 1.5f);

            // Damage players within breath cone
            if (tick % 5 == 0 && intensity > 0.3) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    double dx = pLoc.getX() - c.getX();
                    double dz = pLoc.getZ() - c.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > breathLength || dist < 0.5) continue;

                    double dot = (dx * breathDirX + dz * breathDirZ) / dist;
                    if (dot >= Math.cos(coneHalfAngle)) {
                        p.damage(config.getDamage() * 0.3 * intensity);
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Breathing/hissing sounds
            if (tick % 15 == 0 && intensity > 0.2) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f * (float) intensity, 0.4f);
                DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_STEP, 0.7f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new IceBreath(plugin); }
    }

    // ========================================================================
    // 10. PERMAFROST STOMP — Impact at center + expanding shockwave ring
    // ========================================================================
    public static class PermafrostStomp extends EnvironmentalAttack {
        private Location center;
        private boolean stomped = false;

        public PermafrostStomp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_stomp", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.stomped = false;
            // Warning tremor
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < 15) {
                // Wind-up — gathering frost energy at ground level
                double shrink = 3.0 * (1.0 - tick / 15.0);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.2, 0), 10 + tick * 2, shrink, ICE_R, ICE_G, ICE_B, 1.0f + tick * 0.05f);
                if (tick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_STONE_STEP, 0.7f, 0.3f + tick * 0.05f);
                }
            } else if (!stomped) {
                // STOMP — tick 15
                stomped = true;

                // Massive impact sound
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DEATH, 1.5f, 0.3f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);

                // Central impact burst
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 80, 1.5, ICE_R, ICE_G, ICE_B, 2.5f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.0, 0), 40, 1.0, FROST_R, FROST_G, FROST_B, 2.0f);
                w.spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.5, 0), 2, 0.3, 0.2, 0.3, 0);

                // Immediate center damage (3 block radius)
                dealDamage(this, c, 3.0, config.getDamage() * 0.5);
            } else {
                // Shockwave expanding outward from stomp point
                int shockTick = tick - 15;
                double maxShockRadius = 8.0;
                int shockDuration = config.getDurationTicks() - 15;
                double shockRadius = (maxShockRadius * shockTick) / shockDuration;
                double ringWidth = 1.2;

                // Shockwave ring particles
                int ringPoints = Math.max(16, (int) (shockRadius * 8));
                Particle.DustOptions shockOpt = new Particle.DustOptions(Color.fromRGB(ICE_R, ICE_G, ICE_B), 1.5f);
                for (int i = 0; i < ringPoints; i++) {
                    double angle = (2 * Math.PI * i) / ringPoints;
                    double rx = Math.cos(angle) * shockRadius;
                    double rz = Math.sin(angle) * shockRadius;
                    Location ringPt = c.clone().add(rx, 0.2, rz);
                    w.spawnParticle(Particle.DUST, ringPt, 2, 0.15, 0.1, 0.15, 0, shockOpt);
                }

                // Ground crack particles along ring
                if (shockTick % 2 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        Location crackPt = c.clone().add(Math.cos(angle) * shockRadius, 0.1, Math.sin(angle) * shockRadius);
                        w.spawnParticle(Particle.SNOWFLAKE, crackPt, 2, 0.3, 0.05, 0.3, 0.01);
                    }
                }

                // Damage on the expanding ring
                if (shockTick % 4 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distance(c);
                        if (dist >= shockRadius - ringWidth && dist <= shockRadius + ringWidth) {
                            p.damage(config.getDamage() * 0.3);
                            p.setNoDamageTicks(0);
                        }
                    }
                }

                // Rumble
                if (shockTick % 15 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_STONE_BREAK, 0.5f, 0.3f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new PermafrostStomp(plugin); }
    }

    // ========================================================================
    // 11. FROST HUNTER DASH — 3 rapid sequential damage zones toward player
    // ========================================================================
    public static class FrostHunterDash extends EnvironmentalAttack {
        private Location center;
        private Location[] dashTargets = new Location[3];
        private double dashDirX, dashDirZ;

        public FrostHunterDash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_hunter_dash", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(60); // 3 seconds total
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            Player target = getTargetPlayer();
            Location targetLoc;
            if (target != null && target.isOnline()) {
                targetLoc = target.getLocation();
            } else {
                targetLoc = center.clone().add(5, 0, 5);
            }

            double dx = targetLoc.getX() - center.getX();
            double dz = targetLoc.getZ() - center.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0) { dashDirX = dx / len; dashDirZ = dz / len; }
            else { dashDirX = 1; dashDirZ = 0; }

            // 3 dash zones spaced 4 blocks apart in line toward player
            for (int i = 0; i < 3; i++) {
                double dist = 2.0 + i * 4.0;
                dashTargets[i] = center.clone().add(dashDirX * dist, 0, dashDirZ * dist);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_STUNNED, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Dash 1 at tick 0, Dash 2 at tick 10, Dash 3 at tick 20
            for (int i = 0; i < 3; i++) {
                int dashTick = i * 10;
                int localTick = tick - dashTick;

                if (localTick < 0 || localTick > 20) continue;

                Location dashLoc = dashTargets[i];

                if (localTick == 0) {
                    // IMPACT — damage burst
                    DisplayBuilder.playSound(dashLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 1.0f + i * 0.2f);
                    DisplayBuilder.dustParticles(dashLoc, 50, 1.5, ICE_R, ICE_G, ICE_B, 2.0f);
                    DisplayBuilder.dustParticles(dashLoc, 30, 1.0, FROST_R, FROST_G, FROST_B, 1.8f);
                    w.spawnParticle(Particle.CRIT, dashLoc.clone().add(0, 0.5, 0), 15, 1.0, 0.5, 1.0, 0.2);

                    // Damage at dash zone (2.5 radius)
                    dealDamage(this, dashLoc, 2.5, config.getDamage() * 0.4);
                } else {
                    // Lingering frost after each dash
                    double fade = 1.0 - (localTick / 20.0);
                    int count = (int) (8 * fade);
                    DisplayBuilder.dustParticles(dashLoc.clone().add(0, 0.3, 0), count, 1.2, FROST_R, FROST_G, FROST_B, 1.0f * (float) fade);
                    if (localTick % 4 == 0 && fade > 0.3) {
                        w.spawnParticle(Particle.SNOWFLAKE, dashLoc.clone().add(0, 0.5, 0), 2, 0.8, 0.2, 0.8, 0.005);
                    }
                }
            }

            // Connecting dash trail between zones (particles along the line)
            if (tick < 30 && tick % 2 == 0) {
                int activeZone = Math.min(2, tick / 10);
                for (int i = 0; i <= activeZone; i++) {
                    if (i < 2) {
                        // Trail between zone i and i+1
                        Location start = dashTargets[i];
                        Location end = dashTargets[Math.min(i + 1, 2)];
                        for (int p = 0; p < 4; p++) {
                            double t = p / 4.0;
                            Location trail = start.clone().add(
                                    (end.getX() - start.getX()) * t,
                                    0.2,
                                    (end.getZ() - start.getZ()) * t);
                            DisplayBuilder.dustParticles(trail, 2, 0.2, ICE_R, ICE_G, ICE_B, 0.8f);
                        }
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostHunterDash(plugin); }
    }

    // ========================================================================
    // 12. GLACIAL ROAR WAVE — Cone of frost pushing outward, escalating damage
    // ========================================================================
    public static class GlacialRoarWave extends EnvironmentalAttack {
        private Location center;
        private double waveDirX, waveDirZ;
        private double waveAngle;

        public GlacialRoarWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_roar_wave", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            Player target = getTargetPlayer();
            if (target != null && target.isOnline()) {
                Location tLoc = target.getLocation();
                double dx = tLoc.getX() - center.getX();
                double dz = tLoc.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0) { waveDirX = dx / len; waveDirZ = dz / len; }
                else { waveDirX = 1; waveDirZ = 0; }
            } else {
                waveAngle = Math.random() * 2 * Math.PI;
                waveDirX = Math.cos(waveAngle);
                waveDirZ = Math.sin(waveAngle);
            }
            waveAngle = Math.atan2(waveDirZ, waveDirX);

            // Deep roar
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Wave front advances outward over time — 0 to 8 blocks
            double maxDist = 8.0;
            double waveFront = (maxDist * tick) / config.getDurationTicks();
            double coneHalfAngle = Math.PI / 4; // 45 degree half = 90 degree total cone

            // Damage escalation — increases as wave moves outward
            double damageScale = 0.2 + 0.6 * (waveFront / maxDist);

            Particle.DustOptions iceOpt = new Particle.DustOptions(Color.fromRGB(ICE_R, ICE_G, ICE_B), 1.5f + (float)(waveFront * 0.1));
            Particle.DustOptions frostOpt = new Particle.DustOptions(Color.fromRGB(FROST_R, FROST_G, FROST_B), 1.2f);

            // Wave front particles — dense arc
            int arcPoints = (int) (waveFront * 8) + 8;
            for (int i = 0; i < arcPoints; i++) {
                double arcAngle = waveAngle + (((double) i / arcPoints) - 0.5) * 2 * coneHalfAngle;
                double px = Math.cos(arcAngle) * waveFront;
                double pz = Math.sin(arcAngle) * waveFront;
                Location arcPt = c.clone().add(px, 0.3, pz);
                w.spawnParticle(Particle.DUST, arcPt, 2, 0.15, 0.2, 0.15, 0, iceOpt);
                if (i % 3 == 0) {
                    w.spawnParticle(Particle.DUST, arcPt.clone().add(0, 0.6, 0), 1, 0.1, 0.2, 0.1, 0, frostOpt);
                }
            }

            // Fill particles behind wave front
            if (tick % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double dist = Math.random() * waveFront;
                    double fillAngle = waveAngle + (Math.random() - 0.5) * 2 * coneHalfAngle;
                    double fx = Math.cos(fillAngle) * dist;
                    double fz = Math.sin(fillAngle) * dist;
                    Location fillPt = c.clone().add(fx, 0.2 + Math.random() * 0.5, fz);
                    w.spawnParticle(Particle.SNOWFLAKE, fillPt, 1, 0.2, 0.1, 0.2, 0.005);
                }
            }

            // Damage on wave front
            if (tick % 5 == 0) {
                double waveWidth = 1.5;
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    double dx = pLoc.getX() - c.getX();
                    double dz = pLoc.getZ() - c.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    // Check if on wave front
                    if (dist < waveFront - waveWidth || dist > waveFront + waveWidth) continue;
                    if (dist < 0.5) continue;

                    // Check if within cone angle
                    double dot = (dx * waveDirX + dz * waveDirZ) / dist;
                    if (dot >= Math.cos(coneHalfAngle)) {
                        p.damage(config.getDamage() * damageScale);
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Ongoing roar rumble
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new GlacialRoarWave(plugin); }
    }

    // ========================================================================
    // 13. ICE STALKER AMBUSH — Damage behind player, no warning particles
    // ========================================================================
    public static class IceStalkerAmbush extends EnvironmentalAttack {
        private Location center;
        private boolean ambushed = false;

        public IceStalkerAmbush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_stalker_ambush", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(50);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.ambushed = false;
            // Completely silent — no warning
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            Player target = getTargetPlayer();
            if (target == null || !target.isOnline()) return;
            Location targetLoc = target.getLocation();

            if (tick < 15) {
                // Silent stalk — NO particles at all (ambush is invisible)
                // Only a very faint sound on tick 10 as a barely-noticeable cue
                if (tick == 10) {
                    // Extremely quiet foot crunch behind them
                    Location behindLoc = getBehindPlayer(target, 3.0);
                    DisplayBuilder.playSound(behindLoc, Sound.BLOCK_POWDER_SNOW_STEP, 0.15f, 0.5f);
                }
            } else if (!ambushed) {
                // AMBUSH at tick 15 — behind the player
                ambushed = true;
                Location ambushLoc = getBehindPlayer(target, 2.5);

                // Sudden attack sounds
                DisplayBuilder.playSound(ambushLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.5f);
                DisplayBuilder.playSound(ambushLoc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 1.2f, 1.5f);
                DisplayBuilder.playSound(ambushLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);

                // Explosion of frost particles from behind
                DisplayBuilder.dustParticles(ambushLoc, 60, 1.5, ICE_R, ICE_G, ICE_B, 2.0f);
                DisplayBuilder.dustParticles(ambushLoc, 40, 1.0, FROST_R, FROST_G, FROST_B, 2.5f);
                w.spawnParticle(Particle.CRIT, ambushLoc, 20, 0.8, 0.5, 0.8, 0.3);
                w.spawnParticle(Particle.SNOWFLAKE, ambushLoc, 15, 1.0, 0.8, 1.0, 0.05);

                // Heavy damage at ambush location
                dealDamage(this, ambushLoc, 2.5, config.getDamage() * 0.7);
            } else {
                // Post-ambush: frost lingers where the ambush happened
                int postTick = tick - 15;
                double fade = 1.0 - (double) postTick / (config.getDurationTicks() - 15);
                if (fade > 0) {
                    Location ambushLoc = getBehindPlayer(target, 2.5);
                    int count = (int) (10 * fade);
                    DisplayBuilder.dustParticles(ambushLoc, count, 1.5, FROST_R, FROST_G, FROST_B, 1.0f * (float) fade);
                    if (postTick % 6 == 0 && fade > 0.3) {
                        w.spawnParticle(Particle.SNOWFLAKE, ambushLoc, 2, 1.0, 0.3, 1.0, 0.01);
                    }
                }
            }
        }

        /** Get a location behind the player (opposite their facing direction). */
        private Location getBehindPlayer(Player player, double distance) {
            Location loc = player.getLocation();
            // Player's facing direction
            Vector dir = loc.getDirection();
            // Behind = opposite direction
            return loc.clone().add(-dir.getX() * distance, 0, -dir.getZ() * distance);
        }

        @Override public AbstractAttack newInstance() { return new IceStalkerAmbush(plugin); }
    }
}
