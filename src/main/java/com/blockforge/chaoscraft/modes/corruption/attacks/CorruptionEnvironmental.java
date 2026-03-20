package com.blockforge.chaoscraft.modes.corruption.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Corruption Mode — ENVIRONMENTAL ATTACKS (40 attacks)
 * Void/glitch/digital corruption themed particle & sound attacks.
 * NO potion effects — damage, velocity, particles, and sounds only.
 *
 * Color palette:
 * - Dark purple:        RGB(80, 0, 160)
 * - Glitch green:       RGB(0, 255, 100)
 * - Void black:         RGB(20, 0, 40)
 * - Corruption magenta: RGB(200, 0, 180)
 *
 * 3 sections:
 *   Section 1: Void Distortions    (14 attacks)
 *   Section 2: Corruption Hazards  (13 attacks)
 *   Section 3: Digital Horror       (13 attacks)
 */
public final class CorruptionEnvironmental {

    private CorruptionEnvironmental() {}

    // Shared dust colors
    private static final Particle.DustOptions DUST_PURPLE  = new Particle.DustOptions(Color.fromRGB(80, 0, 160), 1.2f);
    private static final Particle.DustOptions DUST_GREEN   = new Particle.DustOptions(Color.fromRGB(0, 255, 100), 1.0f);
    private static final Particle.DustOptions DUST_VOID    = new Particle.DustOptions(Color.fromRGB(20, 0, 40), 1.5f);
    private static final Particle.DustOptions DUST_MAGENTA = new Particle.DustOptions(Color.fromRGB(200, 0, 180), 1.1f);
    private static final Particle.DustOptions DUST_RED     = new Particle.DustOptions(Color.fromRGB(200, 30, 30), 1.3f);
    private static final Particle.DustOptions DUST_BLUE    = new Particle.DustOptions(Color.fromRGB(40, 120, 255), 1.4f);

    private static final String MODE_PATH = "modes/corruption/attacks";
    private static final Random RNG = new Random();

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // Section 1: Void Distortions
        registry.register(new VoidPulse(plugin));
        registry.register(new RealityTear(plugin));
        registry.register(new GlitchZone(plugin));
        registry.register(new CorruptionWave(plugin));
        registry.register(new DataStorm(plugin));
        registry.register(new NullVoid(plugin));
        registry.register(new PixelScatter(plugin));
        registry.register(new ErrorSpike(plugin));
        registry.register(new BufferOverflow(plugin));
        registry.register(new MemoryLeak(plugin));
        registry.register(new StackOverflow(plugin));
        registry.register(new Fragmentation(plugin));
        registry.register(new DesyncPull(plugin));
        registry.register(new VoidCollapse(plugin));
        // Section 2: Corruption Hazards
        registry.register(new CorruptedGround(plugin));
        registry.register(new GlitchLightning(plugin));
        registry.register(new StaticField(plugin));
        registry.register(new CorruptionGeyser(plugin));
        registry.register(new VirusSpread(plugin));
        registry.register(new MalwarePulse(plugin));
        registry.register(new TrojanBurst(plugin));
        registry.register(new WormTrail(plugin));
        registry.register(new Rootkit(plugin));
        registry.register(new EntropyDrain(plugin));
        registry.register(new BitRot(plugin));
        registry.register(new CrashDump(plugin));
        registry.register(new Deadlock(plugin));
        // Section 3: Digital Horror
        registry.register(new ScreenTear(plugin));
        registry.register(new BlueScreen(plugin));
        registry.register(new ArtifactRain(plugin));
        registry.register(new RenderFail(plugin));
        registry.register(new LagSpike(plugin));
        registry.register(new TextureMissing(plugin));
        registry.register(new GhostImage(plugin));
        registry.register(new FrameDrop(plugin));
        registry.register(new SignalLoss(plugin));
        registry.register(new Overclock(plugin));
        registry.register(new KernelPanic(plugin));
        registry.register(new SystemRestore(plugin));
        registry.register(new TotalCorruption(plugin));
    }

    // ---- Helpers ----

    /** Spawn a ring of dust particles at given Y offset. */
    private static void spawnRing(World w, Location center, double radius, double yOff, int points, Particle.DustOptions dust) {
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            w.spawnParticle(Particle.DUST, x, center.getY() + yOff, z, 1, 0, 0, 0, 0, dust);
        }
    }

    /** Get survival, non-invulnerable players within radius of a location. */
    private static List<Player> getNearbyTargets(Location center, double radius) {
        List<Player> targets = new ArrayList<>();
        if (center.getWorld() == null) return targets;
        double r2 = radius * radius;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL && !p.isInvulnerable()
                    && p.getLocation().distanceSquared(center) <= r2) {
                targets.add(p);
            }
        }
        return targets;
    }

    // ================================================================
    //  SECTION 1: VOID DISTORTIONS (14 attacks)
    // ================================================================

    // ----------------------------------------------------------------
    // 1. VOID PULSE — Expanding dark particle ring, damage in ring
    // ----------------------------------------------------------------
    public static class VoidPulse extends EnvironmentalAttack {
        public VoidPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_pulse", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(5);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double progress = (double) tick / config.getDurationTicks();
            double radius = progress * config.getDamageRadius();
            spawnRing(w, getCenter(), radius, 0.5, 40, DUST_VOID);
            spawnRing(w, getCenter(), radius, 1.0, 30, DUST_PURPLE);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VoidPulse(plugin); }
    }

    // ----------------------------------------------------------------
    // 2. REALITY TEAR — Vertical crack of purple particles
    // ----------------------------------------------------------------
    public static class RealityTear extends EnvironmentalAttack {
        public RealityTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_tear", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 0.8f, 0.4f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 3 == 0) {
                for (double y = 0; y < 6; y += 0.4) {
                    double xJag = Math.sin(y * 3) * 0.5;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + xJag, getCenter().getY() + y,
                            getCenter().getZ(), 2, 0.1, 0.1, 0.1, 0, DUST_PURPLE);
                }
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_GLASS_BREAK, 0.3f, 0.2f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new RealityTear(plugin); }
    }

    // ----------------------------------------------------------------
    // 3. GLITCH ZONE — Area flickers with green particles, random damage
    // ----------------------------------------------------------------
    public static class GlitchZone extends EnvironmentalAttack {
        public GlitchZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glitch_zone", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 1.8f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 4 == 0) {
                double r = config.getDamageRadius();
                for (int i = 0; i < 12; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * r * 2;
                    double oz = (RNG.nextDouble() - 0.5) * r * 2;
                    double oy = RNG.nextDouble() * 3;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + ox, getCenter().getY() + oy,
                            getCenter().getZ() + oz, 1, 0, 0, 0, 0,
                            RNG.nextBoolean() ? DUST_GREEN : DUST_PURPLE);
                }
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_BREAK, 0.6f, 2.0f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlitchZone(plugin); }
    }

    // ----------------------------------------------------------------
    // 4. CORRUPTION WAVE — Wall of purple particles sweeping one direction
    // ----------------------------------------------------------------
    public static class CorruptionWave extends EnvironmentalAttack {
        private double dirX, dirZ;

        public CorruptionWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_wave", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(8);
        }
        @Override protected void onSpawn(Location center) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double offset = (double) tick / config.getDurationTicks() * config.getDamageRadius() * 2 - config.getDamageRadius();
            for (int i = -8; i <= 8; i++) {
                double px = getCenter().getX() + dirX * offset - dirZ * i;
                double pz = getCenter().getZ() + dirZ * offset + dirX * i;
                for (double y = 0; y < 4; y += 0.8) {
                    w.spawnParticle(Particle.DUST, px, getCenter().getY() + y, pz, 1, 0, 0, 0, 0, DUST_PURPLE);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CorruptionWave(plugin); }
    }

    // ----------------------------------------------------------------
    // 5. DATA STORM — Random green/purple particle hits across area
    // ----------------------------------------------------------------
    public static class DataStorm extends EnvironmentalAttack {
        public DataStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("data_storm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.8f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 3 == 0) {
                double r = config.getDamageRadius();
                for (int i = 0; i < 6; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * r * 2;
                    double oz = (RNG.nextDouble() - 0.5) * r * 2;
                    Location strike = getCenter().clone().add(ox, 8 + RNG.nextDouble() * 4, oz);
                    Particle.DustOptions dust = RNG.nextBoolean() ? DUST_GREEN : DUST_PURPLE;
                    for (double y = 0; y < 8; y += 0.5) {
                        w.spawnParticle(Particle.DUST, strike.getX(), strike.getY() - y, strike.getZ(),
                                1, 0.2, 0, 0.2, 0, dust);
                    }
                }
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.3f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DataStorm(plugin); }
    }

    // ----------------------------------------------------------------
    // 6. NULL VOID — Circular dark zone, damage/tick inside
    // ----------------------------------------------------------------
    public static class NullVoid extends EnvironmentalAttack {
        public NullVoid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("null_void", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.2f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 4 == 0) {
                spawnRing(w, getCenter(), config.getDamageRadius(), 0.2, 30, DUST_VOID);
                double r = config.getDamageRadius();
                for (int i = 0; i < 15; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * r * 2;
                    double oz = (RNG.nextDouble() - 0.5) * r * 2;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + ox, getCenter().getY() + RNG.nextDouble() * 2,
                            getCenter().getZ() + oz, 1, 0, 0, 0, 0, DUST_VOID);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new NullVoid(plugin); }
    }

    // ----------------------------------------------------------------
    // 7. PIXEL SCATTER — Random short knockback bursts
    // ----------------------------------------------------------------
    public static class PixelScatter extends EnvironmentalAttack {
        public PixelScatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pixel_scatter", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(12);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 8 == 0) {
                for (int i = 0; i < 10; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * 20;
                    double oz = (RNG.nextDouble() - 0.5) * 20;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + ox, getCenter().getY() + 1,
                            getCenter().getZ() + oz, 3, 0.3, 0.3, 0.3, 0, DUST_GREEN);
                }
                for (Player p : getNearbyTargets(getCenter(), config.getDamageRadius())) {
                    Vector kb = new Vector((RNG.nextDouble() - 0.5) * 0.8, 0.3, (RNG.nextDouble() - 0.5) * 0.8);
                    p.setVelocity(p.getVelocity().add(kb));
                }
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 2.0f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PixelScatter(plugin); }
    }

    // ----------------------------------------------------------------
    // 8. ERROR SPIKE — Narrow pillar of red particles, high damage tiny radius
    // ----------------------------------------------------------------
    public static class ErrorSpike extends EnvironmentalAttack {
        public ErrorSpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("error_spike", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(6);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 2 == 0) {
                for (double y = 0; y < 8; y += 0.3) {
                    w.spawnParticle(Particle.DUST, getCenter().getX(), getCenter().getY() + y,
                            getCenter().getZ(), 2, 0.15, 0, 0.15, 0, DUST_RED);
                }
                w.spawnParticle(Particle.DUST, getCenter().getX(), getCenter().getY() + 8,
                        getCenter().getZ(), 5, 0.5, 0.5, 0.5, 0, DUST_MAGENTA);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ErrorSpike(plugin); }
    }

    // ----------------------------------------------------------------
    // 9. BUFFER OVERFLOW — Expanding corruption circle on ground
    // ----------------------------------------------------------------
    public static class BufferOverflow extends EnvironmentalAttack {
        public BufferOverflow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("buffer_overflow", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(12);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.5f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double progress = (double) tick / config.getDurationTicks();
            double radius = 5.0 + progress * 5.0; // 5 -> 10
            if (tick % 4 == 0) {
                spawnRing(w, getCenter(), radius, 0.2, (int)(radius * 6), DUST_PURPLE);
                for (int i = 0; i < 8; i++) {
                    double a = RNG.nextDouble() * Math.PI * 2;
                    double r = RNG.nextDouble() * radius;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + Math.cos(a) * r, getCenter().getY() + 0.3,
                            getCenter().getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0, DUST_VOID);
                }
            }
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_SPREAD, 0.6f, 0.4f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new BufferOverflow(plugin); }
    }

    // ----------------------------------------------------------------
    // 10. MEMORY LEAK — Lingering purple fog, drifts slowly
    // ----------------------------------------------------------------
    public static class MemoryLeak extends EnvironmentalAttack {
        private double driftX, driftZ;

        public MemoryLeak(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("memory_leak", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            driftX = Math.cos(angle) * 0.08;
            driftZ = Math.sin(angle) * 0.08;
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            setCenter(getCenter().add(driftX, 0, driftZ));
            World w = getCenter().getWorld();
            if (tick % 3 == 0) {
                double r = config.getDamageRadius();
                for (int i = 0; i < 10; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * r * 2;
                    double oz = (RNG.nextDouble() - 0.5) * r * 2;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + ox,
                            getCenter().getY() + RNG.nextDouble() * 2.5,
                            getCenter().getZ() + oz, 1, 0, 0, 0, 0, DUST_PURPLE);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MemoryLeak(plugin); }
    }

    // ----------------------------------------------------------------
    // 11. STACK OVERFLOW — 3 sequential bursts, each bigger
    // ----------------------------------------------------------------
    public static class StackOverflow extends EnvironmentalAttack {
        public StackOverflow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stack_overflow", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.8f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            // Burst at tick 10, 30, 50
            if (tick == 10 || tick == 30 || tick == 50) {
                int burstIndex = tick == 10 ? 0 : tick == 30 ? 1 : 2;
                double radius = 3.0 + burstIndex * 2.0;
                int particles = 20 + burstIndex * 15;
                spawnRing(w, getCenter(), radius, 0.5, particles, DUST_MAGENTA);
                spawnRing(w, getCenter(), radius, 1.5, particles, DUST_PURPLE);
                w.spawnParticle(Particle.EXPLOSION, getCenter().getX(), getCenter().getY() + 1,
                        getCenter().getZ(), 2 + burstIndex, 1, 1, 1, 0);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 0.6f + burstIndex * 0.2f, 1.5f - burstIndex * 0.3f);
                // Manual extra damage for escalating bursts
                double dmg = burstIndex == 0 ? 6.0 : burstIndex == 1 ? 8.0 : 10.0;
                for (Player p : getNearbyTargets(getCenter(), radius)) {
                    p.damage(dmg);
                    p.setNoDamageTicks(0);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new StackOverflow(plugin); }
    }

    // ----------------------------------------------------------------
    // 12. FRAGMENTATION — Particle shrapnel flies outward from center
    // ----------------------------------------------------------------
    public static class Fragmentation extends EnvironmentalAttack {
        public Fragmentation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fragmentation", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(20);
            config.setCooldownTicks(180);
            config.setTicksBetweenDamage(5);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
            if (center.getWorld() == null) return;
            World w = center.getWorld();
            w.spawnParticle(Particle.EXPLOSION, center.getX(), center.getY() + 1, center.getZ(), 3, 0.5, 0.5, 0.5, 0);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double expandRadius = ((double) tick / config.getDurationTicks()) * config.getDamageRadius();
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                double x = getCenter().getX() + Math.cos(angle) * expandRadius;
                double z = getCenter().getZ() + Math.sin(angle) * expandRadius;
                double y = getCenter().getY() + 0.5 + RNG.nextDouble();
                w.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0,
                        RNG.nextBoolean() ? DUST_GREEN : DUST_MAGENTA);
            }
            // Knockback outward
            for (Player p : getNearbyTargets(getCenter(), expandRadius + 1)) {
                Vector dir = p.getLocation().toVector().subtract(getCenter().toVector()).normalize().multiply(0.4);
                dir.setY(0.2);
                p.setVelocity(p.getVelocity().add(dir));
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new Fragmentation(plugin); }
    }

    // ----------------------------------------------------------------
    // 13. DESYNC PULL — Pulls players inward then flings outward
    // ----------------------------------------------------------------
    public static class DesyncPull extends EnvironmentalAttack {
        public DesyncPull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("desync_pull", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            boolean pullPhase = tick < 25;
            if (tick % 3 == 0) {
                spawnRing(w, getCenter(), pullPhase ? config.getDamageRadius() : 2, 1.0, 30, DUST_PURPLE);
            }
            for (Player p : getNearbyTargets(getCenter(), config.getDamageRadius())) {
                Vector dir = getCenter().toVector().subtract(p.getLocation().toVector());
                if (dir.lengthSquared() > 0.1) dir.normalize();
                if (pullPhase) {
                    p.setVelocity(p.getVelocity().add(dir.multiply(0.15)));
                } else if (tick == 25) {
                    // Fling outward
                    p.setVelocity(dir.multiply(-1.2).setY(0.5));
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.5f);
                }
            }
            if (tick % 5 == 0) {
                w.spawnParticle(Particle.DUST, getCenter().getX(), getCenter().getY() + 1,
                        getCenter().getZ(), 10, 2, 2, 2, 0, DUST_VOID);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DesyncPull(plugin); }
    }

    // ----------------------------------------------------------------
    // 14. VOID COLLAPSE — Imploding particles then burst
    // ----------------------------------------------------------------
    public static class VoidCollapse extends EnvironmentalAttack {
        public VoidCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_collapse", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(50);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(50); // Damage only on burst
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 0.8f, 0.2f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick < 40) {
                // Imploding particles
                double shrink = config.getDamageRadius() * (1.0 - (double) tick / 40.0);
                if (tick % 2 == 0) {
                    spawnRing(w, getCenter(), shrink, 0.5, 25, DUST_VOID);
                    spawnRing(w, getCenter(), shrink, 1.5, 20, DUST_PURPLE);
                }
                // Pull players inward
                for (Player p : getNearbyTargets(getCenter(), config.getDamageRadius())) {
                    Vector pull = getCenter().toVector().subtract(p.getLocation().toVector());
                    if (pull.lengthSquared() > 0.1) pull.normalize();
                    p.setVelocity(p.getVelocity().add(pull.multiply(0.1)));
                }
            } else if (tick == 40) {
                // Burst
                w.spawnParticle(Particle.EXPLOSION, getCenter().getX(), getCenter().getY() + 1,
                        getCenter().getZ(), 5, 2, 2, 2, 0);
                spawnRing(w, getCenter(), config.getDamageRadius(), 1.0, 50, DUST_MAGENTA);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                for (Player p : getNearbyTargets(getCenter(), config.getDamageRadius())) {
                    p.damage(10.0);
                    p.setNoDamageTicks(0);
                    Vector fling = p.getLocation().toVector().subtract(getCenter().toVector()).normalize().multiply(1.0).setY(0.6);
                    p.setVelocity(fling);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VoidCollapse(plugin); }
    }

    // ================================================================
    //  SECTION 2: CORRUPTION HAZARDS (13 attacks)
    // ================================================================

    // ----------------------------------------------------------------
    // 15. CORRUPTED GROUND — Ground particles, standing = damage
    // ----------------------------------------------------------------
    public static class CorruptedGround extends EnvironmentalAttack {
        public CorruptedGround(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corrupted_ground", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.4f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 5 == 0) {
                double r = config.getDamageRadius();
                for (int i = 0; i < 20; i++) {
                    double a = RNG.nextDouble() * Math.PI * 2;
                    double d = RNG.nextDouble() * r;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + Math.cos(a) * d,
                            getCenter().getY() + 0.1 + RNG.nextDouble() * 0.3,
                            getCenter().getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0,
                            RNG.nextBoolean() ? DUST_PURPLE : DUST_VOID);
                }
            }
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_BREAK, 0.5f, 0.3f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CorruptedGround(plugin); }
    }

    // ----------------------------------------------------------------
    // 16. GLITCH LIGHTNING — Random particle lightning strikes
    // ----------------------------------------------------------------
    public static class GlitchLightning extends EnvironmentalAttack {
        public GlitchLightning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glitch_lightning", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(15);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.5f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 10 == 0) {
                double r = config.getDamageRadius();
                double ox = (RNG.nextDouble() - 0.5) * r * 2;
                double oz = (RNG.nextDouble() - 0.5) * r * 2;
                Location strike = getCenter().clone().add(ox, 0, oz);
                // Bolt particles from sky
                for (double y = 0; y < 12; y += 0.4) {
                    double jitter = (RNG.nextDouble() - 0.5) * 0.6;
                    w.spawnParticle(Particle.DUST, strike.getX() + jitter, strike.getY() + y,
                            strike.getZ() + jitter, 1, 0, 0, 0, 0, DUST_GREEN);
                }
                w.spawnParticle(Particle.DUST, strike.getX(), strike.getY() + 0.5, strike.getZ(),
                        8, 1, 0.3, 1, 0, DUST_MAGENTA);
                DisplayBuilder.playSound(strike, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 1.8f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlitchLightning(plugin); }
    }

    // ----------------------------------------------------------------
    // 17. STATIC FIELD — Electric particle zone with knockback
    // ----------------------------------------------------------------
    public static class StaticField extends EnvironmentalAttack {
        public StaticField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("static_field", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(12);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.8f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 4 == 0) {
                double r = config.getDamageRadius();
                for (int i = 0; i < 15; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * r * 2;
                    double oz = (RNG.nextDouble() - 0.5) * r * 2;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + ox,
                            getCenter().getY() + RNG.nextDouble() * 3,
                            getCenter().getZ() + oz, 1, 0, 0, 0, 0, DUST_GREEN);
                }
                // Tiny random knockback for players inside
                for (Player p : getNearbyTargets(getCenter(), config.getDamageRadius())) {
                    Vector jolt = new Vector((RNG.nextDouble() - 0.5) * 0.3, 0.1, (RNG.nextDouble() - 0.5) * 0.3);
                    p.setVelocity(p.getVelocity().add(jolt));
                }
            }
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new StaticField(plugin); }
    }

    // ----------------------------------------------------------------
    // 18. CORRUPTION GEYSER — Upward particle burst + launch velocity
    // ----------------------------------------------------------------
    public static class CorruptionGeyser extends EnvironmentalAttack {
        public CorruptionGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_geyser", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(8);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
            if (center.getWorld() == null) return;
            center.getWorld().spawnParticle(Particle.DUST, center.getX(), center.getY(), center.getZ(),
                    15, 1, 0.3, 1, 0, DUST_PURPLE);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 2 == 0) {
                double height = (double) tick / config.getDurationTicks() * 10;
                for (double y = 0; y < height; y += 0.5) {
                    w.spawnParticle(Particle.DUST, getCenter().getX() + (RNG.nextDouble() - 0.5) * 1.5,
                            getCenter().getY() + y,
                            getCenter().getZ() + (RNG.nextDouble() - 0.5) * 1.5,
                            1, 0, 0, 0, 0, DUST_MAGENTA);
                }
                for (Player p : getNearbyTargets(getCenter(), config.getDamageRadius())) {
                    p.setVelocity(p.getVelocity().add(new Vector(0, 0.6, 0)));
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CorruptionGeyser(plugin); }
    }

    // ----------------------------------------------------------------
    // 19. VIRUS SPREAD — Starts small, expands rapidly
    // ----------------------------------------------------------------
    public static class VirusSpread extends EnvironmentalAttack {
        public VirusSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("virus_spread", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 1.5f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double progress = (double) tick / config.getDurationTicks();
            double radius = 3.0 + progress * 9.0; // 3 -> 12
            if (tick % 4 == 0) {
                spawnRing(w, getCenter(), radius, 0.2, (int)(radius * 5), DUST_GREEN);
                for (int i = 0; i < (int)(radius * 2); i++) {
                    double a = RNG.nextDouble() * Math.PI * 2;
                    double r = RNG.nextDouble() * radius;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + Math.cos(a) * r,
                            getCenter().getY() + 0.3,
                            getCenter().getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0, DUST_PURPLE);
                }
            }
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_SPREAD, 0.6f, 1.2f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VirusSpread(plugin); }
    }

    // ----------------------------------------------------------------
    // 20. MALWARE PULSE — Rapid damage pulses every 5 ticks
    // ----------------------------------------------------------------
    public static class MalwarePulse extends EnvironmentalAttack {
        public MalwarePulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("malware_pulse", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(5);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 1.5f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 5 == 0) {
                double radius = config.getDamageRadius();
                spawnRing(w, getCenter(), radius, 0.5, 24, DUST_MAGENTA);
                w.spawnParticle(Particle.DUST, getCenter().getX(), getCenter().getY() + 1,
                        getCenter().getZ(), 8, 2, 1, 2, 0, DUST_GREEN);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.4f, 0.5f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MalwarePulse(plugin); }
    }

    // ----------------------------------------------------------------
    // 21. TROJAN BURST — Harmless for 40 ticks, then massive burst
    // ----------------------------------------------------------------
    public static class TrojanBurst extends EnvironmentalAttack {
        public TrojanBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trojan_burst", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(14.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(50);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(50); // Only damage on burst
            config.setDamageDelayTicks(40);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick < 40) {
                // Harmless subtle particles
                if (tick % 8 == 0) {
                    w.spawnParticle(Particle.DUST, getCenter().getX(), getCenter().getY() + 1,
                            getCenter().getZ(), 5, 1.5, 1, 1.5, 0, DUST_GREEN);
                }
            } else if (tick == 40) {
                // Massive burst
                w.spawnParticle(Particle.EXPLOSION, getCenter().getX(), getCenter().getY() + 1,
                        getCenter().getZ(), 4, 2, 2, 2, 0);
                spawnRing(w, getCenter(), config.getDamageRadius(), 1.0, 50, DUST_RED);
                spawnRing(w, getCenter(), config.getDamageRadius(), 2.0, 40, DUST_MAGENTA);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
                for (Player p : getNearbyTargets(getCenter(), config.getDamageRadius())) {
                    p.damage(14.0);
                    p.setNoDamageTicks(0);
                    Vector kb = p.getLocation().toVector().subtract(getCenter().toVector()).normalize().multiply(0.8).setY(0.4);
                    p.setVelocity(kb);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new TrojanBurst(plugin); }
    }

    // ----------------------------------------------------------------
    // 22. WORM TRAIL — Particle trail snakes across ground randomly
    // ----------------------------------------------------------------
    public static class WormTrail extends EnvironmentalAttack {
        private double headX, headZ;
        private double headAngle;

        public WormTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("worm_trail", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(8);
        }
        @Override protected void onSpawn(Location center) {
            headX = center.getX();
            headZ = center.getZ();
            headAngle = RNG.nextDouble() * Math.PI * 2;
            DisplayBuilder.playSound(center, Sound.ENTITY_SILVERFISH_AMBIENT, 0.8f, 0.4f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            // Wander randomly
            headAngle += (RNG.nextDouble() - 0.5) * 0.6;
            headX += Math.cos(headAngle) * 0.4;
            headZ += Math.sin(headAngle) * 0.4;
            Location head = new Location(w, headX, getCenter().getY(), headZ);
            setCenter(head);
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.DUST, headX, head.getY() + 0.2, headZ, 5, 0.5, 0.1, 0.5, 0, DUST_GREEN);
                w.spawnParticle(Particle.DUST, headX, head.getY() + 0.5, headZ, 3, 0.3, 0.2, 0.3, 0, DUST_PURPLE);
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(head, Sound.ENTITY_SILVERFISH_STEP, 0.5f, 0.5f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new WormTrail(plugin); }
    }

    // ----------------------------------------------------------------
    // 23. ROOTKIT — Invisible until player enters, then reveals + damages
    // ----------------------------------------------------------------
    public static class Rootkit extends EnvironmentalAttack {
        private boolean triggered = false;

        public Rootkit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rootkit", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(80); // No auto-damage until triggered
        }
        @Override protected void onSpawn(Location center) {
            // Silent spawn — no sound or particles
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (!triggered) {
                // Very faint hint particle every 20 ticks
                if (tick % 20 == 0) {
                    w.spawnParticle(Particle.DUST, getCenter().getX(), getCenter().getY() + 0.1,
                            getCenter().getZ(), 1, 0.5, 0, 0.5, 0, DUST_VOID);
                }
                // Check if any player enters
                if (!getNearbyTargets(getCenter(), config.getDamageRadius()).isEmpty()) {
                    triggered = true;
                    config.setDamageDelayTicks(0); // Enable damage
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_WARDEN_EMERGE, 0.8f, 1.5f);
                    w.spawnParticle(Particle.EXPLOSION, getCenter().getX(), getCenter().getY() + 1,
                            getCenter().getZ(), 2, 1, 1, 1, 0);
                }
            } else {
                // Revealed — aggressive particles
                if (tick % 3 == 0) {
                    double r = config.getDamageRadius();
                    for (int i = 0; i < 15; i++) {
                        double a = RNG.nextDouble() * Math.PI * 2;
                        double d = RNG.nextDouble() * r;
                        w.spawnParticle(Particle.DUST, getCenter().getX() + Math.cos(a) * d,
                                getCenter().getY() + RNG.nextDouble() * 3,
                                getCenter().getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0, DUST_RED);
                    }
                    spawnRing(w, getCenter(), r, 0.3, 20, DUST_MAGENTA);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new Rootkit(plugin); }
    }

    // ----------------------------------------------------------------
    // 24. ENTROPY DRAIN — Slow continuous damage
    // ----------------------------------------------------------------
    public static class EntropyDrain extends EnvironmentalAttack {
        public EntropyDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("entropy_drain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 5 == 0) {
                double r = config.getDamageRadius();
                for (int i = 0; i < 12; i++) {
                    double a = RNG.nextDouble() * Math.PI * 2;
                    double d = RNG.nextDouble() * r;
                    double oy = RNG.nextDouble() * 2;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + Math.cos(a) * d,
                            getCenter().getY() + oy,
                            getCenter().getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0, DUST_VOID);
                }
                // Wisps flowing toward center
                for (int i = 0; i < 4; i++) {
                    double a = RNG.nextDouble() * Math.PI * 2;
                    double startR = r;
                    w.spawnParticle(Particle.DUST,
                            getCenter().getX() + Math.cos(a) * startR,
                            getCenter().getY() + 1,
                            getCenter().getZ() + Math.sin(a) * startR,
                            2, 0, 0, 0, 0, DUST_PURPLE);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new EntropyDrain(plugin); }
    }

    // ----------------------------------------------------------------
    // 25. BIT ROT — Ground area slowly expands with corruption particles
    // ----------------------------------------------------------------
    public static class BitRot extends EnvironmentalAttack {
        public BitRot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bit_rot", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(340);
            config.setTicksBetweenDamage(12);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.8f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double progress = (double) tick / config.getDurationTicks();
            double radius = 3.0 + progress * 7.0; // 3 -> 10
            if (tick % 5 == 0) {
                spawnRing(w, getCenter(), radius, 0.1, (int)(radius * 4), DUST_VOID);
                for (int i = 0; i < (int)(radius * 1.5); i++) {
                    double a = RNG.nextDouble() * Math.PI * 2;
                    double d = RNG.nextDouble() * radius;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + Math.cos(a) * d,
                            getCenter().getY() + 0.15,
                            getCenter().getZ() + Math.sin(a) * d, 1, 0, 0, 0, 0,
                            RNG.nextBoolean() ? DUST_PURPLE : DUST_GREEN);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new BitRot(plugin); }
    }

    // ----------------------------------------------------------------
    // 26. CRASH DUMP — Massive particle explosion, single burst
    // ----------------------------------------------------------------
    public static class CrashDump extends EnvironmentalAttack {
        public CrashDump(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crash_dump", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(12.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(10);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            if (center.getWorld() == null) return;
            World w = center.getWorld();
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.4f);
            w.spawnParticle(Particle.EXPLOSION, center.getX(), center.getY() + 2, center.getZ(), 8, 4, 3, 4, 0);
            spawnRing(w, center, 10, 1.0, 60, DUST_MAGENTA);
            spawnRing(w, center, 7, 2.0, 45, DUST_PURPLE);
            spawnRing(w, center, 4, 3.0, 30, DUST_GREEN);
            for (int i = 0; i < 40; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 20;
                double oz = (RNG.nextDouble() - 0.5) * 20;
                w.spawnParticle(Particle.DUST, center.getX() + ox, center.getY() + RNG.nextDouble() * 5,
                        center.getZ() + oz, 1, 0, 0, 0, 0, DUST_VOID);
            }
        }
        @Override protected void onTick(int tick) {
            // All damage happens from the base system tick
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrashDump(plugin); }
    }

    // ----------------------------------------------------------------
    // 27. DEADLOCK — Players in area get slowed via velocity drag
    // ----------------------------------------------------------------
    public static class Deadlock extends EnvironmentalAttack {
        public Deadlock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("deadlock", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(12);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 3 == 0) {
                spawnRing(w, getCenter(), config.getDamageRadius(), 0.3, 25, DUST_VOID);
                w.spawnParticle(Particle.DUST, getCenter().getX(), getCenter().getY() + 1,
                        getCenter().getZ(), 6, 3, 0.5, 3, 0, DUST_PURPLE);
            }
            // Velocity drag — slow players by reducing horizontal velocity
            if (tick % 4 == 0) {
                for (Player p : getNearbyTargets(getCenter(), config.getDamageRadius())) {
                    Vector vel = p.getVelocity();
                    p.setVelocity(new Vector(vel.getX() * 0.3, vel.getY(), vel.getZ() * 0.3));
                }
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CHAIN_STEP, 0.5f, 0.3f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new Deadlock(plugin); }
    }

    // ================================================================
    //  SECTION 3: DIGITAL HORROR (13 attacks)
    // ================================================================

    // ----------------------------------------------------------------
    // 28. SCREEN TEAR — Horizontal particle line sweeps vertically
    // ----------------------------------------------------------------
    public static class ScreenTear extends EnvironmentalAttack {
        public ScreenTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("screen_tear", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(7.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(8);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double progress = (double) tick / config.getDurationTicks();
            double yPos = getCenter().getY() + progress * 6;
            double r = config.getDamageRadius();
            for (double x = -r; x <= r; x += 0.5) {
                w.spawnParticle(Particle.DUST, getCenter().getX() + x, yPos, getCenter().getZ(),
                        1, 0, 0, 0, 0, DUST_GREEN);
                w.spawnParticle(Particle.DUST, getCenter().getX() + x, yPos + 0.3, getCenter().getZ(),
                        1, 0, 0, 0, 0, DUST_MAGENTA);
            }
            if (tick % 10 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_GLASS_BREAK, 0.4f, 1.8f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ScreenTear(plugin); }
    }

    // ----------------------------------------------------------------
    // 29. BLUE SCREEN — Bright blue particle flash, instant AoE
    // ----------------------------------------------------------------
    public static class BlueScreen extends EnvironmentalAttack {
        public BlueScreen(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blue_screen", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(5);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(5);
        }
        @Override protected void onSpawn(Location center) {
            if (center.getWorld() == null) return;
            World w = center.getWorld();
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 2.0f);
            // Massive blue particle flash
            Particle.DustOptions blueBright = new Particle.DustOptions(Color.fromRGB(60, 140, 255), 2.0f);
            for (int i = 0; i < 80; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 30;
                double oz = (RNG.nextDouble() - 0.5) * 30;
                double oy = RNG.nextDouble() * 6;
                w.spawnParticle(Particle.DUST, center.getX() + ox, center.getY() + oy,
                        center.getZ() + oz, 1, 0, 0, 0, 0, blueBright);
            }
            spawnRing(w, center, 15, 1.0, 60, DUST_BLUE);
        }
        @Override protected void onTick(int tick) {
            // Instant — all visual in onSpawn
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new BlueScreen(plugin); }
    }

    // ----------------------------------------------------------------
    // 30. ARTIFACT RAIN — Glitch-colored particles raining
    // ----------------------------------------------------------------
    public static class ArtifactRain extends EnvironmentalAttack {
        public ArtifactRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("artifact_rain", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 2.0f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 3 == 0) {
                double r = config.getDamageRadius();
                Particle.DustOptions[] colors = { DUST_GREEN, DUST_PURPLE, DUST_MAGENTA, DUST_VOID };
                for (int i = 0; i < 10; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * r * 2;
                    double oz = (RNG.nextDouble() - 0.5) * r * 2;
                    double dropY = 8 + RNG.nextDouble() * 4;
                    Particle.DustOptions color = colors[RNG.nextInt(colors.length)];
                    for (double y = dropY; y > 0; y -= 1.0) {
                        w.spawnParticle(Particle.DUST, getCenter().getX() + ox,
                                getCenter().getY() + y, getCenter().getZ() + oz,
                                1, 0.1, 0, 0.1, 0, color);
                    }
                }
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.4f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ArtifactRain(plugin); }
    }

    // ----------------------------------------------------------------
    // 31. RENDER FAIL — Area goes dark with dark particles
    // ----------------------------------------------------------------
    public static class RenderFail extends EnvironmentalAttack {
        public RenderFail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("render_fail", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.2f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 2 == 0) {
                double r = config.getDamageRadius();
                // Dense dark cloud
                for (int i = 0; i < 25; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * r * 2;
                    double oz = (RNG.nextDouble() - 0.5) * r * 2;
                    double oy = RNG.nextDouble() * 4;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + ox,
                            getCenter().getY() + oy, getCenter().getZ() + oz,
                            1, 0, 0, 0, 0, DUST_VOID);
                }
            }
            if (tick % 15 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_BREAK, 0.4f, 0.2f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new RenderFail(plugin); }
    }

    // ----------------------------------------------------------------
    // 32. LAG SPIKE — Freeze effect: massive velocity drag then release
    // ----------------------------------------------------------------
    public static class LagSpike extends EnvironmentalAttack {
        public LagSpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lag_spike", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(30);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.2f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick < 20) {
                // "Freeze" — heavy velocity drag
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.DUST, getCenter().getX(), getCenter().getY() + 2,
                            getCenter().getZ(), 15, 5, 2, 5, 0, DUST_BLUE);
                }
                for (Player p : getNearbyTargets(getCenter(), config.getDamageRadius())) {
                    p.setVelocity(new Vector(0, p.getVelocity().getY() * 0.5, 0));
                }
            } else if (tick == 20) {
                // Release — burst
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.5f);
                spawnRing(w, getCenter(), config.getDamageRadius(), 1.0, 40, DUST_GREEN);
                for (Player p : getNearbyTargets(getCenter(), config.getDamageRadius())) {
                    p.damage(6.0);
                    p.setNoDamageTicks(0);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new LagSpike(plugin); }
    }

    // ----------------------------------------------------------------
    // 33. TEXTURE MISSING — Magenta/black checkerboard particles
    // ----------------------------------------------------------------
    public static class TextureMissing extends EnvironmentalAttack {
        public TextureMissing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("texture_missing", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BIT, 0.8f, 0.3f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick % 4 == 0) {
                double r = config.getDamageRadius();
                // Checkerboard pattern on ground
                for (double x = -r; x <= r; x += 1.5) {
                    for (double z = -r; z <= r; z += 1.5) {
                        if (x * x + z * z > r * r) continue;
                        boolean pink = ((int)(x + r) + (int)(z + r)) % 2 == 0;
                        Particle.DustOptions color = pink ? DUST_MAGENTA : DUST_VOID;
                        w.spawnParticle(Particle.DUST, getCenter().getX() + x,
                                getCenter().getY() + 0.2, getCenter().getZ() + z,
                                1, 0, 0, 0, 0, color);
                    }
                }
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.5f, 2.0f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new TextureMissing(plugin); }
    }

    // ----------------------------------------------------------------
    // 34. GHOST IMAGE — Damages at player's position from 2 sec ago
    // ----------------------------------------------------------------
    public static class GhostImage extends EnvironmentalAttack {
        private final Map<UUID, Location> pastLocations = new HashMap<>();

        public GhostImage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ghost_image", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(20);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_AMBIENT, 0.7f, 0.4f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double searchRadius = 15.0;
            // Record positions and check old ones
            if (tick % 10 == 0) {
                for (Player p : getNearbyTargets(getCenter(), searchRadius)) {
                    Location old = pastLocations.get(p.getUniqueId());
                    pastLocations.put(p.getUniqueId(), p.getLocation().clone());
                    if (old != null && tick >= 40) {
                        // Ghost at old position
                        w.spawnParticle(Particle.DUST, old.getX(), old.getY() + 1, old.getZ(),
                                10, 0.3, 0.8, 0.3, 0, DUST_PURPLE);
                        // Damage if player is still near their old position
                        if (p.getLocation().distanceSquared(old) <= config.getDamageRadius() * config.getDamageRadius()) {
                            p.damage(config.getDamage());
                            p.setNoDamageTicks(0);
                            DisplayBuilder.playSound(old, Sound.ENTITY_VEX_HURT, 0.6f, 0.5f);
                        }
                    }
                }
            }
            // Ambient particles
            if (tick % 6 == 0) {
                w.spawnParticle(Particle.DUST, getCenter().getX(), getCenter().getY() + 1,
                        getCenter().getZ(), 5, 4, 1, 4, 0, DUST_VOID);
            }
        }
        @Override protected void onCleanup() { pastLocations.clear(); super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GhostImage(plugin); }
    }

    // ----------------------------------------------------------------
    // 35. FRAME DROP — Stuttering particle bursts, damage on each stutter
    // ----------------------------------------------------------------
    public static class FrameDrop extends EnvironmentalAttack {
        public FrameDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frame_drop", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(12);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BIT, 0.7f, 0.5f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            // "Stutter" — particles only on certain ticks for a choppy feel
            boolean stutter = (tick % 12 < 4);
            if (stutter && tick % 2 == 0) {
                double r = config.getDamageRadius();
                for (int i = 0; i < 20; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * r * 2;
                    double oz = (RNG.nextDouble() - 0.5) * r * 2;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + ox,
                            getCenter().getY() + RNG.nextDouble() * 3,
                            getCenter().getZ() + oz, 1, 0, 0, 0, 0,
                            RNG.nextBoolean() ? DUST_GREEN : DUST_MAGENTA);
                }
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.3f, RNG.nextFloat() * 2);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrameDrop(plugin); }
    }

    // ----------------------------------------------------------------
    // 36. SIGNAL LOSS — Particles fade in and out, damage during visible
    // ----------------------------------------------------------------
    public static class SignalLoss extends EnvironmentalAttack {
        public SignalLoss(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("signal_loss", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(8);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 1.5f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            // Sine wave visibility: visible when sin > 0
            double phase = Math.sin(tick * 0.15);
            boolean visible = phase > 0;
            if (visible && tick % 3 == 0) {
                double r = config.getDamageRadius();
                float size = (float)(phase * 1.5);
                Particle.DustOptions fadeDust = new Particle.DustOptions(Color.fromRGB(80, 0, 160), Math.max(0.3f, size));
                for (int i = 0; i < 15; i++) {
                    double ox = (RNG.nextDouble() - 0.5) * r * 2;
                    double oz = (RNG.nextDouble() - 0.5) * r * 2;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + ox,
                            getCenter().getY() + RNG.nextDouble() * 3,
                            getCenter().getZ() + oz, 1, 0, 0, 0, 0, fadeDust);
                }
                spawnRing(w, getCenter(), r, 0.3, 20, fadeDust);
            }
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 1.0f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SignalLoss(plugin); }
    }

    // ----------------------------------------------------------------
    // 37. OVERCLOCK — Fast pulsing area damage with speed particles
    // ----------------------------------------------------------------
    public static class Overclock extends EnvironmentalAttack {
        public Overclock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("overclock", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(6);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 2.0f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            // Fast spinning ring effect
            if (tick % 2 == 0) {
                double r = config.getDamageRadius();
                double spinAngle = tick * 0.3;
                for (int i = 0; i < 8; i++) {
                    double a = spinAngle + (Math.PI * 2 * i) / 8;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + Math.cos(a) * r,
                            getCenter().getY() + 1.0, getCenter().getZ() + Math.sin(a) * r,
                            2, 0.2, 0.2, 0.2, 0, DUST_RED);
                }
                // Inner ring spins opposite
                for (int i = 0; i < 6; i++) {
                    double a = -spinAngle + (Math.PI * 2 * i) / 6;
                    w.spawnParticle(Particle.DUST, getCenter().getX() + Math.cos(a) * (r * 0.5),
                            getCenter().getY() + 1.5, getCenter().getZ() + Math.sin(a) * (r * 0.5),
                            2, 0.1, 0.1, 0.1, 0, DUST_GREEN);
                }
            }
            if (tick % 12 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.4f, 2.0f);
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new Overclock(plugin); }
    }

    // ----------------------------------------------------------------
    // 38. KERNEL PANIC — Expanding red shockwave, must jump over
    // ----------------------------------------------------------------
    public static class KernelPanic extends EnvironmentalAttack {
        public KernelPanic(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("kernel_panic", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(5);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.2f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            double progress = (double) tick / config.getDurationTicks();
            double radius = progress * config.getDamageRadius();
            // Low shockwave ring — only damages grounded players
            spawnRing(w, getCenter(), radius, 0.3, (int)(radius * 8), DUST_RED);
            spawnRing(w, getCenter(), radius, 0.6, (int)(radius * 5), DUST_MAGENTA);
            // Only damage players on the ground (can jump over)
            if (tick % 5 == 0) {
                double ringWidth = 2.0;
                for (Player p : getNearbyTargets(getCenter(), radius + ringWidth)) {
                    double dist = p.getLocation().distance(getCenter());
                    boolean inRing = dist >= radius - ringWidth && dist <= radius + ringWidth;
                    boolean onGround = p.isOnGround();
                    if (inRing && onGround) {
                        p.damage(8.0);
                        p.setNoDamageTicks(0);
                        p.setVelocity(p.getVelocity().add(new Vector(0, 0.4, 0)));
                    }
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new KernelPanic(plugin); }
    }

    // ----------------------------------------------------------------
    // 39. SYSTEM RESTORE — Reverse implosion, pull then damage
    // ----------------------------------------------------------------
    public static class SystemRestore extends EnvironmentalAttack {
        public SystemRestore(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("system_restore", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(7.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(50);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(12);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 0.6f, 1.5f);
        }
        @Override protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            if (tick < 35) {
                // Reverse implosion — particles move outward then reverse inward
                double progress = (double) tick / 35.0;
                double radius = config.getDamageRadius() * (1.0 - Math.abs(progress - 0.5) * 2);
                if (tick % 3 == 0) {
                    spawnRing(w, getCenter(), radius, 1.0, 30, DUST_PURPLE);
                    spawnRing(w, getCenter(), radius, 2.0, 20, DUST_GREEN);
                }
                // Pull toward center
                for (Player p : getNearbyTargets(getCenter(), config.getDamageRadius())) {
                    Vector pull = getCenter().toVector().subtract(p.getLocation().toVector());
                    if (pull.lengthSquared() > 0.1) pull.normalize();
                    p.setVelocity(p.getVelocity().add(pull.multiply(0.12)));
                }
            } else if (tick == 35) {
                // Damage burst at center
                w.spawnParticle(Particle.EXPLOSION, getCenter().getX(), getCenter().getY() + 1,
                        getCenter().getZ(), 3, 1, 1, 1, 0);
                spawnRing(w, getCenter(), 3, 1.0, 30, DUST_MAGENTA);
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
                for (Player p : getNearbyTargets(getCenter(), 4.0)) {
                    p.damage(7.0);
                    p.setNoDamageTicks(0);
                }
            }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SystemRestore(plugin); }
    }

    // ----------------------------------------------------------------
    // 40. TOTAL CORRUPTION — Everything in radius takes damage, dark explosion
    // ----------------------------------------------------------------
    public static class TotalCorruption extends EnvironmentalAttack {
        public TotalCorruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("total_corruption", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(10);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }
        @Override protected void onSpawn(Location center) {
            if (center.getWorld() == null) return;
            World w = center.getWorld();
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
            // Massive dark explosion
            w.spawnParticle(Particle.EXPLOSION, center.getX(), center.getY() + 2, center.getZ(), 10, 6, 4, 6, 0);
            for (double r = 2; r <= 20; r += 3) {
                spawnRing(w, center, r, 0.5, (int)(r * 5), DUST_VOID);
                spawnRing(w, center, r, 1.5, (int)(r * 3), DUST_PURPLE);
            }
            for (int i = 0; i < 60; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 40;
                double oz = (RNG.nextDouble() - 0.5) * 40;
                double oy = RNG.nextDouble() * 8;
                w.spawnParticle(Particle.DUST, center.getX() + ox, center.getY() + oy,
                        center.getZ() + oz, 1, 0, 0, 0, 0,
                        RNG.nextBoolean() ? DUST_MAGENTA : DUST_VOID);
            }
        }
        @Override protected void onTick(int tick) {
            // All visual in onSpawn — base system handles damage
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new TotalCorruption(plugin); }
    }
}
