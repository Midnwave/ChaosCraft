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
 * Seer Mode — VOID HAZARDS
 * 12 environmental void hazard attacks.
 * Palette: Void black (20,0,40), Deep purple (80,0,160), Eye red (200,0,50)
 */
public final class VoidHazards {

    private VoidHazards() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidPool(plugin));
        registry.register(new GravityCollapse(plugin));
        registry.register(new VoidDrain(plugin));
        registry.register(new DimensionalTear(plugin));
        registry.register(new NullZone(plugin));
        registry.register(new EntropyFieldEnv(plugin));
        registry.register(new VoidQuake(plugin));
        registry.register(new DarkImplosion(plugin));
        registry.register(new VacuumBurst(plugin));
        registry.register(new VoidLightning(plugin));
        registry.register(new RealityFracture(plugin));
        registry.register(new AbsoluteZero(plugin));
    }

    // ================================================================
    // 1. VOID POOL — Dark ground zone, damage while standing on it
    // ================================================================
    public static class VoidPool extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public VoidPool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_pool", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 15; i++) {
                double angle = RNG.nextDouble() * 2 * Math.PI;
                double dist = RNG.nextDouble() * 5;
                DisplayBuilder.dustParticles(center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist),
                        3, 0.5, 20, 0, 40, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * 5;
                    DisplayBuilder.dustParticles(center.clone().add(Math.cos(angle) * dist, 0.1 + RNG.nextDouble() * 0.3, Math.sin(angle) * dist),
                            2, 0.3, 20, 0, 40, 1.2f);
                }
            }
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.1, 0), 5.0, Particle.DUST,
                        12, new Particle.DustOptions(Color.fromRGB(80, 0, 160), 0.8f));
            }
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.3f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VoidPool(plugin); }
    }

    // ================================================================
    // 2. GRAVITY COLLAPSE — Pull all players down and toward center
    // ================================================================
    public static class GravityCollapse extends EnvironmentalAttack {
        public GravityCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_collapse", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center, 20, 6.0, 80, 0, 160, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.3f);
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
                        Vector pull = center.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.1);
                        pull.setY(-0.15);
                        player.setVelocity(player.getVelocity().add(pull));
                    }
                }
            }
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = ticksAlive * 0.1 + i * (Math.PI / 4);
                    double r = 8.0 * (1.0 - ticksAlive / 80.0);
                    DisplayBuilder.dustParticles(center.clone().add(Math.cos(angle) * r, 0.3, Math.sin(angle) * r),
                            2, 0.3, 20, 0, 40, 1.5f);
                }
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.4f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GravityCollapse(plugin); }
    }

    // ================================================================
    // 3. VOID DRAIN — HP drains faster based on proximity to center
    // ================================================================
    public static class VoidDrain extends EnvironmentalAttack {
        public VoidDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_drain", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), 7.0, Particle.DUST,
                    20, new Particle.DustOptions(Color.fromRGB(20, 0, 40), 1.5f));
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 10 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double distSq = player.getLocation().distanceSquared(center);
                    if (distSq <= 49) {
                        double proximity = 1.0 - (Math.sqrt(distSq) / 7.0);
                        double damage = 3.0 + proximity * 7.0;
                        player.damage(damage);
                        player.setNoDamageTicks(0);
                        DisplayBuilder.dustParticles(player.getLocation().clone().add(0, 1, 0),
                                (int) (proximity * 6), 0.5, 80, 0, 160, 1.2f);
                    }
                }
            }
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 4, 5.0, 20, 0, 40, 1.0f);
            }
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VoidDrain(plugin); }
    }

    // ================================================================
    // 4. DIMENSIONAL TEAR — Line of damage across arena
    // ================================================================
    public static class DimensionalTear extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private double tearAngle;

        public DimensionalTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_tear", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            tearAngle = RNG.nextDouble() * Math.PI;
            for (int d = -8; d <= 8; d++) {
                Location pLoc = center.clone().add(Math.cos(tearAngle) * d, 1, Math.sin(tearAngle) * d);
                DisplayBuilder.dustParticles(pLoc, 3, 0.3, 80, 0, 160, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 2 == 0) {
                for (int d = -8; d <= 8; d++) {
                    Location pLoc = center.clone().add(Math.cos(tearAngle) * d, 0.5 + RNG.nextDouble() * 2, Math.sin(tearAngle) * d);
                    DisplayBuilder.dustParticles(pLoc, 1, 0.2, 20, 0, 40, 1.2f);
                }
            }
            if (ticksAlive % 8 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double dx = ploc.getX() - center.getX();
                    double dz = ploc.getZ() - center.getZ();
                    double perpDist = Math.abs(dx * Math.sin(tearAngle) - dz * Math.cos(tearAngle));
                    double paraDist = Math.abs(dx * Math.cos(tearAngle) + dz * Math.sin(tearAngle));
                    if (perpDist <= 2.0 && paraDist <= 8.0) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
            }
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DimensionalTear(plugin); }
    }

    // ================================================================
    // 5. NULL ZONE — No velocity zone, players freeze in place + damage
    // ================================================================
    public static class NullZone extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public NullZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("null_zone", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(3.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), 4.0, Particle.DUST,
                    16, new Particle.DustOptions(Color.fromRGB(20, 0, 40), 1.5f));
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 2 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 16) {
                        player.setVelocity(new Vector(0, Math.min(player.getVelocity().getY(), 0), 0));
                    }
                }
            }
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 4; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 8;
                    double oz = (RNG.nextDouble() - 0.5) * 8;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 0.2, oz), 2, 0.3, 20, 0, 40, 1.0f);
                }
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.3f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new NullZone(plugin); }
    }

    // ================================================================
    // 6. ENTROPY FIELD (ENV) — Players in zone take escalating damage
    // ================================================================
    public static class EntropyFieldEnv extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public EntropyFieldEnv(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("entropy_field_env", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(2.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0), 6.0, Particle.DUST,
                    16, new Particle.DustOptions(Color.fromRGB(200, 0, 50), 1.0f));
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            double escalation = 2.0 + (ticksAlive / 160.0) * 8.0;
            if (ticksAlive % 15 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 36) {
                        player.damage(escalation);
                        player.setNoDamageTicks(0);
                    }
                }
            }
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = RNG.nextDouble() * 2 * Math.PI;
                    double dist = RNG.nextDouble() * 6;
                    DisplayBuilder.dustParticles(center.clone().add(Math.cos(angle) * dist, 0.2 + RNG.nextDouble(), Math.sin(angle) * dist),
                            1, 0.3, 200, 0, 50, 0.8f);
                }
            }
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 1.0f + (float) (ticksAlive / 160.0));
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new EntropyFieldEnv(plugin); }
    }

    // ================================================================
    // 7. VOID QUAKE — Ground tremor, random damage spots
    // ================================================================
    public static class VoidQuake extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public VoidQuake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_quake", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center, 20, 8.0, 20, 0, 40, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DIG, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 4 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                Location quakeLoc = center.clone().add(ox, 0, oz);
                DisplayBuilder.dustParticles(quakeLoc, 8, 1.5, 20, 0, 40, 1.5f);
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(quakeLoc) <= 9) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                        player.setVelocity(player.getVelocity().add(new Vector(0, 0.3, 0)));
                    }
                }
            }
            if (ticksAlive % 2 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 100) {
                        player.setVelocity(player.getVelocity().add(
                                new Vector((RNG.nextDouble() - 0.5) * 0.08, 0, (RNG.nextDouble() - 0.5) * 0.08)));
                    }
                }
            }
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_STEP, 0.5f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VoidQuake(plugin); }
    }

    // ================================================================
    // 8. DARK IMPLOSION — Sucks everything inward then burst
    // ================================================================
    public static class DarkImplosion extends EnvironmentalAttack {
        private boolean imploded = false;

        public DarkImplosion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_implosion", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(700);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 15, 6.0, 80, 0, 160, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 0.5f);
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
                        Vector pull = center.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.2);
                        player.setVelocity(player.getVelocity().add(pull));
                    }
                }
                double r = 6.0 - (ticksAlive / 25.0) * 5.5;
                DisplayBuilder.particleRing(center.clone().add(0, 1, 0), r, Particle.DUST,
                        16, new Particle.DustOptions(Color.fromRGB(20, 0, 40), 1.5f));
            }
            if (!imploded && ticksAlive >= 25) {
                imploded = true;
                triggerImpactDamage(center);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 30, 5.0, 200, 0, 50, 2.5f);
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 25) {
                        Vector push = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.0);
                        push.setY(0.5);
                        player.setVelocity(push);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DarkImplosion(plugin); }
    }

    // ================================================================
    // 9. VACUUM BURST — Outward push wave
    // ================================================================
    public static class VacuumBurst extends EnvironmentalAttack {
        private boolean bursted = false;

        public VacuumBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("vacuum_burst", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
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
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 10, 1.0, 20, 0, 40, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 0.8f, 1.8f);
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
                        Vector push = player.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.5);
                        push.setY(0.6);
                        player.setVelocity(push);
                    }
                }
                for (double r = 1; r <= 8; r += 1.5) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r, Particle.DUST,
                            (int) (r * 4), new Particle.DustOptions(Color.fromRGB(80, 0, 160), 1.5f));
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VacuumBurst(plugin); }
    }

    // ================================================================
    // 10. VOID LIGHTNING — Dark purple lightning strikes
    // ================================================================
    public static class VoidLightning extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public VoidLightning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_lightning", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 10; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                DisplayBuilder.dustParticles(center.clone().add(ox, 15, oz), 3, 1.0, 80, 0, 160, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 8 == 0) {
                double ox = (RNG.nextDouble() - 0.5) * 18;
                double oz = (RNG.nextDouble() - 0.5) * 18;
                Location strikeLoc = center.clone().add(ox, 0, oz);
                double currentX = strikeLoc.getX();
                double currentZ = strikeLoc.getZ();
                for (int y = 15; y >= 0; y--) {
                    currentX += (RNG.nextDouble() - 0.5) * 0.8;
                    currentZ += (RNG.nextDouble() - 0.5) * 0.8;
                    Location boltPoint = new Location(w, currentX, strikeLoc.getY() + y, currentZ);
                    DisplayBuilder.dustParticles(boltPoint, 2, 0.1, 80, 0, 160, 1.5f);
                }
                DisplayBuilder.dustParticles(strikeLoc, 8, 1.0, 200, 0, 180, 1.5f);
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(strikeLoc) <= 9) {
                        player.damage(config.getDamage());
                        player.setNoDamageTicks(0);
                    }
                }
                DisplayBuilder.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 0.5f);
            }
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VoidLightning(plugin); }
    }

    // ================================================================
    // 11. REALITY FRACTURE — Multiple cracks appear, damage near them
    // ================================================================
    public static class RealityFracture extends EnvironmentalAttack {
        private static final Random RNG = new Random();
        private final double[][] crackPositions = new double[5][2];

        public RealityFracture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_fracture", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 5; i++) {
                crackPositions[i][0] = (RNG.nextDouble() - 0.5) * 14;
                crackPositions[i][1] = (RNG.nextDouble() - 0.5) * 14;
                Location crackLoc = center.clone().add(crackPositions[i][0], 0, crackPositions[i][1]);
                for (int y = 0; y < 5; y++) {
                    DisplayBuilder.dustParticles(crackLoc.clone().add(0, y * 0.5, 0), 3, 0.3, 80, 0, 160, 1.5f);
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 3 == 0) {
                for (double[] crack : crackPositions) {
                    Location crackLoc = center.clone().add(crack[0], 0, crack[1]);
                    double y = RNG.nextDouble() * 3;
                    DisplayBuilder.dustParticles(crackLoc.clone().add((RNG.nextDouble() - 0.5) * 0.5, y, 0),
                            2, 0.2, 80, 0, 160, 1.2f);
                }
            }
            if (ticksAlive % 12 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    for (double[] crack : crackPositions) {
                        Location crackLoc = center.clone().add(crack[0], 0, crack[1]);
                        if (player.getLocation().distanceSquared(crackLoc) <= 4) {
                            player.damage(config.getDamage());
                            player.setNoDamageTicks(0);
                            break;
                        }
                    }
                }
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_STEP, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new RealityFracture(plugin); }
    }

    // ================================================================
    // 12. ABSOLUTE ZERO — Freeze zone, heavy slow + damage
    // ================================================================
    public static class AbsoluteZero extends EnvironmentalAttack {
        private static final Random RNG = new Random();

        public AbsoluteZero(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("absolute_zero", AttackType.ENVIRONMENTAL, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 20, 5.0, 240, 230, 255, 2.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            if (ticksAlive % 2 == 0) {
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    if (player.getLocation().distanceSquared(center) <= 25) {
                        Vector vel = player.getVelocity();
                        player.setVelocity(new Vector(vel.getX() * 0.3, vel.getY(), vel.getZ() * 0.3));
                    }
                }
            }
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 10;
                    double oz = (RNG.nextDouble() - 0.5) * 10;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 0.2 + RNG.nextDouble() * 2, oz),
                            2, 0.3, 240, 230, 255, 1.2f);
                }
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 0.4f, 0.5f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AbsoluteZero(plugin); }
    }
}
