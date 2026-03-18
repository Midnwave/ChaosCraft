package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.environmental;

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
 * Phase 2 Environmental — GROUP 5: ATMOSPHERIC COSMIC EFFECTS
 * 10 arena-wide effects: gravity anomalies, pressure waves, cosmic distortions.
 * Attacks 41-50 from boss2-dog.md.
 *
 * Design notes:
 * - No status effects (all debuffs translated to velocity changes or damage)
 * - DoG crystalline plague palette: cyan (0,200,255), violet (128,0,255), white (240,240,255)
 * - Arena-wide scope — most attacks affect the full island
 * - Materials: SEA_LANTERN, LIGHT_BLUE_STAINED_GLASS, AMETHYST_BLOCK, PRISMARINE
 */
public final class AtmosphericCosmic {

    private AtmosphericCosmic() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CosmicPressureWave(plugin));
        registry.register(new GravityInversionField(plugin));
        registry.register(new TemporalStutter(plugin));
        registry.register(new VoidStatic(plugin));
        registry.register(new CrystalResonancePulse(plugin));
        registry.register(new AtmosphericCompression(plugin));
        registry.register(new MagneticPoleShift(plugin));
        registry.register(new SensoryOverloadFlash(plugin));
        registry.register(new CosmicDrain(plugin));
        registry.register(new Phase1Crescendo(plugin));
    }

    // =========================================================================
    // 41. COSMIC PRESSURE WAVE — sweeping wave across island pushes players
    // =========================================================================
    public static class CosmicPressureWave extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> waveHandles = new ArrayList<>();
        private double wavePosition = -20;
        private double waveDirX;
        private double waveDirZ;

        public CosmicPressureWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_pressure_wave", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // damage handled manually at wave front
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // ~8 seconds
            config.setCooldownTicks(600); // 30 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random sweep direction
            double angle = Math.random() * 2 * Math.PI;
            waveDirX = Math.cos(angle);
            waveDirZ = Math.sin(angle);

            // Wave front: 2-block-tall wall of teal glass blocks
            double perpX = -waveDirZ;
            double perpZ = waveDirX;
            for (int i = -10; i <= 10; i += 2) {
                for (int y = 0; y < 2; y++) {
                    Location loc = center.clone().add(
                            waveDirX * (-20) + perpX * i, y, waveDirZ * (-20) + perpZ * i);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                    h.scale(1.5f, 0.9f, 0.15f).glow(0, 200, 255).interpolation(3, 0);
                    waveHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Wave moves at 8 blocks per second (0.4 blocks/tick)
            wavePosition += 0.4;

            double perpX = -waveDirZ;
            double perpZ = waveDirX;

            // Move wave front blocks
            int idx = 0;
            for (int i = -10; i <= 10; i += 2) {
                for (int y = 0; y < 2; y++) {
                    if (idx < waveHandles.size()) {
                        Location newLoc = center.clone().add(
                                waveDirX * wavePosition + perpX * i, y,
                                waveDirZ * wavePosition + perpZ * i);
                        waveHandles.get(idx).entity().teleport(newLoc);
                        idx++;
                    }
                }
            }

            // Particles along wave front
            if (ticksAlive % 3 == 0) {
                for (int i = -8; i <= 8; i += 4) {
                    Location pLoc = center.clone().add(
                            waveDirX * wavePosition + perpX * i, 1,
                            waveDirZ * wavePosition + perpZ * i);
                    DisplayBuilder.cyanDust(pLoc, 4, 0.5);
                }
            }

            // Damage + push players hit by wave front: 4 HP (2 hearts) + 2.5 block push
            if (ticksAlive % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    // Project player onto wave direction to check if wave front is at them
                    double projDist = (pLoc.getX() - center.getX()) * waveDirX
                            + (pLoc.getZ() - center.getZ()) * waveDirZ;
                    if (Math.abs(projDist - wavePosition) <= 1.5) {
                        // Check perpendicular distance
                        double perpDist = Math.abs((pLoc.getX() - center.getX()) * perpX
                                + (pLoc.getZ() - center.getZ()) * perpZ);
                        if (perpDist <= 12.0) {
                            p.damage(4.0); // 2 hearts
                            p.setVelocity(p.getVelocity().add(
                                    new org.bukkit.util.Vector(waveDirX * 0.5, 0.1, waveDirZ * 0.5)));
                            DisplayBuilder.cyanDust(pLoc, 8, 0.5);
                        }
                    }
                }
            }

            // Fade wave alpha as it passes
            if (wavePosition > 15) {
                float fade = Math.max(0f, 1f - (float)((wavePosition - 15) / 10.0));
                for (BlockDisplayHandle h : waveHandles) {
                    h.entity().setTransformation(new Transformation(
                            new Vector3f(-0.75f, -0.45f, -0.075f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.5f * fade, 0.9f * fade, 0.15f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(4);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicPressureWave(plugin); }
    }

    // =========================================================================
    // 42. GRAVITY INVERSION FIELD — hemisphere suppresses jumping
    // =========================================================================
    public static class GravityInversionField extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> hemisphereHandles = new ArrayList<>();

        public GravityInversionField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_inversion_field", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // no damage, jump suppression
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds (2s warning + 8s active)
            config.setCooldownTicks(900); // 45 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: cave sound
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.8f);

            // Hemisphere at Y+20 — ring of blocks defining the dome's base
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 10, 20, Math.sin(angle) * 10);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                h.scale(0.6f, 0.3f, 0.6f).glow(128, 0, 255).interpolation(5, 0);
                hemisphereHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Top of hemisphere
            BlockDisplayHandle top = displayBuilder.spawnBlock(
                    center.clone().add(0, 30, 0), Material.PRISMARINE);
            top.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(5, 0);
            hemisphereHandles.add(top);
            spawnedEntities.add(top.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks
            if (ticksAlive <= 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 20, 0), 12, 5.0, 128, 0, 255, 1.0f);
                }
                return;
            }

            // Active: suppress jump by capping Y velocity
            if (ticksAlive % 2 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double hDist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - center.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                    if (hDist <= 10.0) {
                        org.bukkit.util.Vector vel = p.getVelocity();
                        if (vel.getY() > 0.05) {
                            p.setVelocity(vel.setY(0.05)); // suppress jump
                        }
                    }
                }
            }

            // Upward-flowing particles within field
            if (ticksAlive % 5 == 0) {
                double a = Math.random() * 2 * Math.PI;
                double r = Math.random() * 10;
                Location pLoc = center.clone().add(Math.cos(a) * r, Math.random() * 15, Math.sin(a) * r);
                DisplayBuilder.dustParticles(pLoc, 3, 0.5, 128, 0, 255, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravityInversionField(plugin); }
    }

    // =========================================================================
    // 43. TEMPORAL STUTTER — brief freeze, particles stall, attack timers spike
    // =========================================================================
    public static class TemporalStutter extends EnvironmentalAttack {

        private boolean stutterFired = false;

        public TemporalStutter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("temporal_stutter", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // no direct damage
            config.setDamageRadius(0.0);
            config.setDurationTicks(20); // 1 second (0.5s freeze + 0.5s acceleration)
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // No warning — instantaneous
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // STUTTER at tick 1
            if (ticksAlive == 1 && !stutterFired) {
                stutterFired = true;

                // Visual freeze: dense white particle burst (smeared trail effect)
                DisplayBuilder.dustParticles(center, 100, 25.0, 240, 240, 255, 0.6f);
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.2f);

                // Freeze all players briefly: zero velocity
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 900) {
                        p.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                    }
                }
            }

            // Acceleration phase: tick 10 — burst of catch-up particles
            if (ticksAlive == 10) {
                DisplayBuilder.cyanDust(center, 60, 20.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TemporalStutter(plugin); }
    }

    // =========================================================================
    // 44. VOID STATIC — interference particles, reduces combat effectiveness
    // =========================================================================
    public static class VoidStatic extends EnvironmentalAttack {

        public VoidStatic(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_static", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(4.0); // 2 hearts — replaces Mining Fatigue mechanical penalty
            config.setDamageRadius(25.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(700); // 35 seconds
            config.setTicksBetweenDamage(40); // every 2 seconds
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Harsh sonic boom warning
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dense interference static particles island-wide
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 40;
                    double oy = Math.random() * 10;
                    double oz = (Math.random() - 0.5) * 40;
                    Location pLoc = center.clone().add(ox, oy, oz);
                    DisplayBuilder.dustParticles(pLoc, 2, 0.1, 240, 240, 255, 0.4f);
                }
            }

            // Periodic interference sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidStatic(plugin); }
    }

    // =========================================================================
    // 45. CRYSTAL RESONANCE PULSE — all crystal structures flash simultaneously
    // =========================================================================
    public static class CrystalResonancePulse extends EnvironmentalAttack {

        private boolean pulseFired = false;

        public CrystalResonancePulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_resonance_pulse", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(4.0); // 2 hearts — replaces Slowness II near crystals
            config.setDamageRadius(10.0); // players near crystal structures
            config.setDurationTicks(10); // instantaneous pulse
            config.setCooldownTicks(560); // 28 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // No warning — flash is instantaneous
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // PULSE at tick 1
            if (ticksAlive == 1 && !pulseFired) {
                pulseFired = true;

                // Island-wide amethyst dust flash
                DisplayBuilder.cyanDust(center, 100, 25.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.0f);

                // Damage players near center (where crystal structures are)
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 100.0) { // 10 block radius
                        p.damage(4.0); // 2 hearts
                        // Velocity dampening replaces Slowness
                        org.bukkit.util.Vector vel = p.getVelocity();
                        p.setVelocity(vel.setX(vel.getX() * 0.3).setZ(vel.getZ() * 0.3));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalResonancePulse(plugin); }
    }

    // =========================================================================
    // 46. ATMOSPHERIC COMPRESSION — sky ceiling drops, damages elevated players
    // =========================================================================
    public static class AtmosphericCompression extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> ceilingHandles = new ArrayList<>();
        private double ceilingY = 120;

        public AtmosphericCompression(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("atmospheric_compression", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // damage handled manually by elevation
            config.setDamageRadius(0.0);
            config.setDurationTicks(360); // 18 seconds (8s compression + 10s hold)
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.4f);

            // Sky ceiling panels — large flat glass at Y+120
            for (int x = -15; x <= 15; x += 6) {
                for (int z = -15; z <= 15; z += 6) {
                    Location loc = center.clone().add(x, 120, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_STAINED_GLASS);
                    h.scale(5.0f, 0.08f, 5.0f).glow(240, 240, 255).interpolation(5, 0);
                    ceilingHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Compression: ceiling drops from Y+120 to Y+40 over 160 ticks (8 seconds)
            if (ticksAlive <= 160) {
                ceilingY = 120.0 - (ticksAlive / 160.0) * 80.0;
                for (BlockDisplayHandle h : ceilingHandles) {
                    Location loc = h.entity().getLocation();
                    loc.setY(center.getY() + ceilingY);
                    h.entity().teleport(loc);
                }
            }

            // Particles at ceiling level
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < 4; i++) {
                    double ox = (Math.random() - 0.5) * 20;
                    double oz = (Math.random() - 0.5) * 20;
                    Location pLoc = center.clone().add(ox, ceilingY - 2, oz);
                    DisplayBuilder.dustParticles(pLoc, 3, 1.0, 240, 240, 255, 0.6f);
                }
            }

            // Damage players above Y+35: 4 HP/sec (2 hearts/sec)
            if (ticksAlive % 20 == 0 && ceilingY <= 50) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().getY() > center.getY() + 35) {
                        double hDist = Math.sqrt(
                                Math.pow(p.getLocation().getX() - center.getX(), 2) +
                                Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                        if (hDist <= 20.0) {
                            p.damage(4.0); // 2 hearts
                            p.setVelocity(p.getVelocity().setY(-0.3));
                            DisplayBuilder.dustParticles(p.getLocation(), 6, 0.5, 240, 240, 255, 0.8f);
                        }
                    }
                }
            }

            // Ceiling returns to normal in last 40 ticks
            if (ticksAlive > 320) {
                float rise = (ticksAlive - 320) / 40.0f;
                ceilingY = 40.0 + rise * 80.0;
                for (BlockDisplayHandle h : ceilingHandles) {
                    Location loc = h.entity().getLocation();
                    loc.setY(center.getY() + ceilingY);
                    h.entity().teleport(loc);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AtmosphericCompression(plugin); }
    }

    // =========================================================================
    // 47. MAGNETIC POLE SHIFT — all particles reverse, players pushed backward
    // =========================================================================
    public static class MagneticPoleShift extends EnvironmentalAttack {

        private boolean shiftFired = false;

        public MagneticPoleShift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magnetic_pole_shift", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // no damage, fall risk from reversal
            config.setDamageRadius(0.0);
            config.setDurationTicks(30); // 1.5 seconds (1s warning + 0.5s shift)
            config.setCooldownTicks(760); // 38 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: enderman teleport sound
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning particles: 20 ticks (1 second)
            if (ticksAlive <= 20) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center, 20, 20.0);
                }
                return;
            }

            // SHIFT at tick 20
            if (ticksAlive == 21 && !shiftFired) {
                shiftFired = true;

                // Visual: dense burst of reversed particles
                DisplayBuilder.dustParticles(center, 80, 25.0, 0, 200, 255, 1.0f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);

                // Reverse all player movement directions: 1.5-block opposing force
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 900) {
                        org.bukkit.util.Vector vel = p.getVelocity();
                        p.setVelocity(new org.bukkit.util.Vector(
                                -vel.getX() * 2.0, vel.getY(), -vel.getZ() * 2.0));
                    }
                }
            }

            // Brief opposing force for 10 ticks (0.5 seconds)
            if (ticksAlive > 20 && ticksAlive <= 30) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 900) {
                        org.bukkit.util.Vector vel = p.getVelocity();
                        p.setVelocity(vel.setX(vel.getX() * 0.6).setZ(vel.getZ() * 0.6));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagneticPoleShift(plugin); }
    }

    // =========================================================================
    // 48. SENSORY OVERLOAD FLASH — blinding white particle burst + damage proxy
    // =========================================================================
    public static class SensoryOverloadFlash extends EnvironmentalAttack {

        private boolean flashFired = false;

        public SensoryOverloadFlash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sensory_overload_flash", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(4.0); // 2 hearts — replaces Blindness mechanical penalty
            config.setDamageRadius(25.0);
            config.setDurationTicks(70); // 3.5 seconds (0.5s flash + 3s aftermath)
            config.setCooldownTicks(900); // 45 seconds
            config.setTicksBetweenDamage(60); // single hit
        }

        @Override
        protected void onSpawn(Location center) {
            // No warning — flash is instantaneous
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // FLASH at tick 1
            if (ticksAlive == 1 && !flashFired) {
                flashFired = true;

                // Maximum density white particle burst
                DisplayBuilder.dustParticles(center, 150, 25.0, 240, 240, 255, 2.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 2.0f);
            }

            // Aftermath: fading white particles (simulates Blindness wearing off)
            if (ticksAlive > 10 && ticksAlive <= 60) {
                float intensity = 1f - (ticksAlive - 10) / 50.0f;
                int count = (int)(intensity * 20);
                if (count > 0 && ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(center, count, 15.0, 240, 240, 255, 1.5f * intensity);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SensoryOverloadFlash(plugin); }
    }

    // =========================================================================
    // 49. COSMIC DRAIN — all particles funnel toward random point, then detonate
    // =========================================================================
    public static class CosmicDrain extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> drainHandles = new ArrayList<>();
        private Location drainPoint;
        private boolean detonated = false;

        public CosmicDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_drain", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // detonation damage handled manually
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds (5s drain + detonation)
            config.setCooldownTicks(1100); // 55 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Random drain point on island
            drainPoint = center.clone().add(
                    (Math.random() - 0.5) * 20, 0, (Math.random() - 0.5) * 20);

            DisplayBuilder.playSound(drainPoint, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);

            // Visual drain accumulator at the point
            BlockDisplayHandle core = displayBuilder.spawnBlock(drainPoint.clone().add(0, 0.5, 0),
                    Material.SEA_LANTERN);
            core.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(5, 0);
            drainHandles.add(core);
            spawnedEntities.add(core.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Drain phase: 100 ticks (5 seconds)
            if (ticksAlive <= 100) {
                // Particles funnel toward drain point from all directions
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double a = Math.random() * 2 * Math.PI;
                        double dist = 8 + Math.random() * 12;
                        Location pLoc = drainPoint.clone().add(
                                Math.cos(a) * dist, 1 + Math.random() * 3, Math.sin(a) * dist);
                        DisplayBuilder.cyanDust(pLoc, 2, 0.2);
                    }
                }

                // Drain core grows
                float growth = 0.3f + (ticksAlive / 100.0f) * 1.2f;
                if (ticksAlive % 10 == 0 && !drainHandles.isEmpty()) {
                    BlockDisplay bd = (BlockDisplay) drainHandles.get(0).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-growth / 2, -growth / 2 + 0.5f, -growth / 2),
                            new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                            new Vector3f(growth, growth, growth),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }

                // Pull players toward drain: 0.2 blocks per second
                if (ticksAlive % 4 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = Math.sqrt(p.getLocation().distanceSquared(drainPoint));
                        if (dist <= 20.0 && dist > 0.5) {
                            double pullX = (drainPoint.getX() - p.getLocation().getX()) / dist * 0.04;
                            double pullZ = (drainPoint.getZ() - p.getLocation().getZ()) / dist * 0.04;
                            p.setVelocity(p.getVelocity().add(
                                    new org.bukkit.util.Vector(pullX, 0, pullZ)));
                        }
                    }
                }

                // Intensifying sound
                if (ticksAlive % 25 == 0) {
                    float pitch = 0.6f + ticksAlive * 0.005f;
                    DisplayBuilder.playSound(drainPoint, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, pitch);
                }
            }

            // DETONATION at tick 100
            if (ticksAlive == 100 && !detonated) {
                detonated = true;

                // Massive outward burst
                DisplayBuilder.cyanDust(drainPoint, 80, 8.0);
                DisplayBuilder.dustParticles(drainPoint, 40, 4.0, 240, 240, 255, 1.5f);
                DisplayBuilder.playSound(drainPoint, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                DisplayBuilder.playSound(drainPoint, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.7f);

                // Damage: 8 HP (4 hearts) in 4-block radius
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(drainPoint) <= 16.0) {
                        p.damage(8.0);
                    }
                }

                // Core collapses
                if (!drainHandles.isEmpty()) {
                    drainHandles.get(0).entity().setTransformation(new Transformation(
                            new Vector3f(0, 0.5f, 0),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0, 0, 0),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    drainHandles.get(0).entity().setInterpolationDelay(0);
                    drainHandles.get(0).entity().setInterpolationDuration(5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicDrain(plugin); }
    }

    // =========================================================================
    // 50. PHASE 1 CRESCENDO — all environmental systems fire simultaneously
    // =========================================================================
    public static class Phase1Crescendo extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> chaosHandles = new ArrayList<>();
        private boolean cascadeFired = false;

        public Phase1Crescendo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase1_crescendo", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(6.0); // 3 hearts — variable damage from overlapping effects
            config.setDamageRadius(25.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(999999); // fires once per fight at 55% HP
            config.setTicksBetweenDamage(20); // every second
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // No warning — the health threshold crossing is the only signal
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // CASCADE at tick 1
            if (ticksAlive == 1 && !cascadeFired) {
                cascadeFired = true;

                // Everything fires at once — visual chaos
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.4f);

                // Scar ignitions (simulated)
                for (int i = 0; i < 8; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = 3 + Math.random() * 12;
                    Location loc = center.clone().add(Math.cos(a) * d, 0.02, Math.sin(a) * d);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                    h.scale(1.5f, 0.06f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
                    chaosHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Rift openings (simulated)
                for (int i = 0; i < 4; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = 5 + Math.random() * 10;
                    Location loc = center.clone().add(Math.cos(a) * d, 0.5, Math.sin(a) * d);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                    h.scale(0.5f, 1.5f, 0.1f).glow(128, 0, 255).interpolation(4, 0);
                    chaosHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Ground ruptures (simulated)
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = 2 + Math.random() * 15;
                    Location loc = center.clone().add(Math.cos(a) * d, 0.01, Math.sin(a) * d);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(0.8f, 0.03f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                    chaosHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Massive particle burst
                DisplayBuilder.cyanDust(center, 80, 20.0);
                DisplayBuilder.dustParticles(center, 40, 15.0, 128, 0, 255, 1.5f);
                DisplayBuilder.dustParticles(center, 30, 10.0, 240, 240, 255, 1.0f);
            }

            // Ongoing chaos: continuous particle storm for 5 seconds
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.cyanDust(center, 12, 15.0);
                DisplayBuilder.dustParticles(center, 8, 12.0, 128, 0, 255, 0.8f);
            }

            // Periodic ground eruptions
            if (ticksAlive % 15 == 0) {
                double a = Math.random() * 2 * Math.PI;
                double d = Math.random() * 18;
                Location eruptLoc = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                DisplayBuilder.cyanDust(eruptLoc, 15, 1.5);
                DisplayBuilder.playSound(eruptLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.5f + (float)(Math.random() * 0.5));
            }

            // Sky events (simulated falling shards)
            if (ticksAlive % 20 == 0) {
                double ox = (Math.random() - 0.5) * 20;
                double oz = (Math.random() - 0.5) * 20;
                Location skyLoc = center.clone().add(ox, 10 + Math.random() * 10, oz);
                DisplayBuilder.dustParticles(skyLoc, 6, 1.0, 0, 200, 255, 1.0f);
            }

            // Fade out chaos visuals
            if (ticksAlive > 80) {
                float fade = Math.max(0f, 1f - (ticksAlive - 80) / 20.0f);
                for (BlockDisplayHandle h : chaosHandles) {
                    Transformation t = h.entity().getTransformation();
                    Vector3f s = t.getScale();
                    h.entity().setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s.x * fade, s.y * fade, s.z * fade),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    h.entity().setInterpolationDelay(0);
                    h.entity().setInterpolationDuration(5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Phase1Crescendo(plugin); }
    }
}
