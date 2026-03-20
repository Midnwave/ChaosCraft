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

/**
 * WHITEOUT EVENTS — 13 environmental attacks themed around disorientation and visibility.
 * Pure particle + damage attacks, no block displays.
 *
 * Palette:
 *   Icy white:  RGB(220, 240, 255)
 *   Pure white:  RGB(255, 255, 255)
 *   Cold blue:   RGB(100, 180, 255)
 *   Dark cold:   RGB(30, 30, 50)
 *
 * All radii 5-10 blocks. No potion effects.
 */
public final class WhiteoutEvents {
    private WhiteoutEvents() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WhiteoutPulse(plugin));
        registry.register(new FrostHaze(plugin));
        registry.register(new SnowBlind(plugin));
        registry.register(new IceMirrorFlash(plugin));
        registry.register(new ColdDarkness(plugin));
        registry.register(new FrostStatic(plugin));
        registry.register(new ArcticNight(plugin));
        registry.register(new CrystalRefraction(plugin));
        registry.register(new BlindingSnow(plugin));
        registry.register(new FrostFlicker(plugin));
        registry.register(new GlacialGlare(plugin));
        registry.register(new IceVeilDrop(plugin));
        registry.register(new PermafrostMirage(plugin));
    }

    // =========================================================================
    // 1. WhiteoutPulse — Dense white particle burst every 30 ticks in 8-block
    //    radius. Damage only during the pulse tick.
    // =========================================================================
    public static class WhiteoutPulse extends EnvironmentalAttack {
        private Location center;

        public WhiteoutPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("whiteout_pulse", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_PLACE, 1.2f, 0.5f);
            // Initial ambient haze
            DisplayBuilder.dustParticles(center, 30, 4.0, 220, 240, 255, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Dense pulse every 30 ticks
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.6f);

                // Massive white burst — fill the 8-block radius with particles
                for (int i = 0; i < 80; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 8.0;
                    double y = Math.random() * 3.0;
                    Location loc = center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(loc, 3, 0.3, 255, 255, 255, 1.8f);
                }

                // Expanding ring at ground level
                DisplayBuilder.particleRing(center, 8.0, Particle.DUST, 40,
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.5f));

                // Damage all players in radius during pulse
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 64) { // 8^2
                        triggerImpactDamage(p.getLocation());
                    }
                }
            }

            // Ambient swirl between pulses (light particles)
            if (tick % 5 == 0) {
                double angle = (tick * 0.15) % (2 * Math.PI);
                Location swirl = center.clone().add(Math.cos(angle) * 4.0, 1.5, Math.sin(angle) * 4.0);
                DisplayBuilder.dustParticles(swirl, 5, 1.0, 220, 240, 255, 0.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new WhiteoutPulse(plugin); }
    }

    // =========================================================================
    // 2. FrostHaze — Persistent mild white dust haze across entire area.
    //    Low constant damage every 20 ticks.
    // =========================================================================
    public static class FrostHaze extends EnvironmentalAttack {
        private Location center;

        public FrostHaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_haze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.8f, 1.8f);
            // Initial haze blanket
            for (int i = 0; i < 40; i++) {
                double x = (Math.random() - 0.5) * 20.0;
                double y = Math.random() * 3.0;
                double z = (Math.random() - 0.5) * 20.0;
                DisplayBuilder.dustParticles(center.clone().add(x, y, z), 2, 0.5, 220, 240, 255, 1.0f);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;

            // Persistent haze — scatter particles every 3 ticks across entire 10-block radius
            if (tick % 3 == 0) {
                for (int i = 0; i < 15; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 10.0;
                    double y = Math.random() * 2.5 + 0.5;
                    Location loc = center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(loc, 2, 0.8, 255, 255, 255, 0.7f);
                }
            }

            // Thicker patches that drift slowly
            if (tick % 8 == 0) {
                double driftAngle = tick * 0.05;
                for (int i = 0; i < 3; i++) {
                    double patchAngle = driftAngle + (i * 2.094); // 120 degrees apart
                    double r = 4.0 + Math.sin(tick * 0.02 + i) * 3.0;
                    Location patch = center.clone().add(Math.cos(patchAngle) * r, 1.0, Math.sin(patchAngle) * r);
                    DisplayBuilder.dustParticles(patch, 8, 1.5, 220, 240, 255, 1.2f);
                }
            }

            // Soft ambient sound
            if (tick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_PLACE, 0.5f, 1.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostHaze(plugin); }
    }

    // =========================================================================
    // 3. SnowBlind — Intense white particle burst (200 particles) + damage spike.
    //    40-tick duration bursts with gaps between.
    // =========================================================================
    public static class SnowBlind extends EnvironmentalAttack {
        private Location center;
        private boolean burstActive = false;
        private int burstStart = 0;

        public SnowBlind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_blind", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            burstActive = true;
            burstStart = 0;
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.7f, 1.8f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Burst pattern: 40 ticks ON, 30 ticks OFF, repeat
            int cyclePos = tick % 70;
            burstActive = cyclePos < 40;

            if (burstActive) {
                // 200 particles — absolute whiteout
                for (int i = 0; i < 50; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 7.0;
                    double y = Math.random() * 4.0;
                    Location loc = center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(loc, 4, 0.4, 255, 255, 255, 2.0f);
                }

                // Bright core
                DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 20, 2.0, 220, 240, 255, 2.5f);

                // END_ROD for extra brightness
                if (tick % 4 == 0) {
                    w.spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 10, 4.0, 2.0, 4.0, 0.02);
                }

                // Damage during burst
                if (tick % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 49) { // 7^2
                            triggerImpactDamage(p.getLocation());
                        }
                    }
                }

                // Burst sound
                if (cyclePos == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_SNOW_GOLEM_AMBIENT, 1.0f, 0.3f);
                }
            } else {
                // Calm between bursts — faint remnant particles
                if (tick % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 5, 3.0, 220, 240, 255, 0.5f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SnowBlind(plugin); }
    }

    // =========================================================================
    // 4. IceMirrorFlash — Bright white flash (END_ROD burst) + damage burst
    //    every 50 ticks.
    // =========================================================================
    public static class IceMirrorFlash extends EnvironmentalAttack {
        private Location center;

        public IceMirrorFlash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mirror_flash", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.0f, 2.0f);
            // Crystalline ambient shimmer
            DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 15, 3.0, 100, 180, 255, 0.8f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Flash every 50 ticks
            if (tick % 50 == 0) {
                // Massive END_ROD flash — blinding
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 1.5, 0), 80, 6.0, 3.0, 6.0, 0.05);
                w.spawnParticle(Particle.FLASH, center.clone().add(0, 2, 0), 1, 0, 0, 0, 0);

                // White dust explosion
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 40, 5.0, 255, 255, 255, 2.0f);

                // Radial lines outward (mirror reflections)
                for (int i = 0; i < 8; i++) {
                    double angle = (i / 8.0) * 2 * Math.PI;
                    Location lineEnd = center.clone().add(Math.cos(angle) * 8.0, 1.0, Math.sin(angle) * 8.0);
                    DisplayBuilder.particleLine(center.clone().add(0, 1, 0), lineEnd, Particle.DUST, 4,
                            new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.0f));
                }

                // Sound + damage
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.2f, 2.0f);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 64) { // 8^2
                        triggerImpactDamage(p.getLocation());
                    }
                }
            }

            // Between flashes: subtle shimmer
            if (tick % 10 == 0) {
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 1, 0), 3, 2.0, 1.0, 2.0, 0.01);
                DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 3, 4.0, 100, 180, 255, 0.5f);
            }

            // Slow rotating ice reflections
            if (tick % 5 == 0) {
                double a = tick * 0.1;
                Location shimmer = center.clone().add(Math.cos(a) * 3.0, 1.0, Math.sin(a) * 3.0);
                w.spawnParticle(Particle.END_ROD, shimmer, 2, 0.2, 0.2, 0.2, 0);
            }
        }

        @Override public AbstractAttack newInstance() { return new IceMirrorFlash(plugin); }
    }

    // =========================================================================
    // 5. ColdDarkness — Dark particles (RGB 30,30,50) mixed with ice dust.
    //    Damage in dark zones.
    // =========================================================================
    public static class ColdDarkness extends EnvironmentalAttack {
        private Location center;
        private final List<Location> darkZones = new ArrayList<>();

        public ColdDarkness(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_darkness", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            darkZones.clear();
            // Generate 4 random dark zones within the area
            for (int i = 0; i < 4; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 2.0 + Math.random() * 6.0;
                darkZones.add(center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            }
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Overall ambient darkness
            if (tick % 4 == 0) {
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 9.0;
                    double y = Math.random() * 3.0;
                    Location loc = center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(loc, 2, 0.5, 30, 30, 50, 1.5f);
                }
            }

            // Dark zone effects — concentrated darkness + ice dust + damage
            for (Location zone : darkZones) {
                // Dense dark particles
                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(zone.clone().add(0, 1, 0), 8, 2.0, 30, 30, 50, 2.0f);
                    // Ice dust mixed in
                    DisplayBuilder.dustParticles(zone.clone().add(0, 1.5, 0), 3, 1.5, 100, 180, 255, 0.8f);
                }

                // Dark zone ring indicator
                if (tick % 10 == 0) {
                    DisplayBuilder.particleRing(zone, 3.0, Particle.DUST, 16,
                            new Particle.DustOptions(Color.fromRGB(30, 30, 50), 1.2f));
                }

                // Damage players in dark zones every 15 ticks (3-block radius per zone)
                if (tick % 15 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(zone) <= 9) { // 3^2
                            triggerImpactDamage(p.getLocation());
                        }
                    }
                }
            }

            // Dark zones slowly drift
            if (tick % 40 == 0) {
                for (int i = 0; i < darkZones.size(); i++) {
                    Location zone = darkZones.get(i);
                    zone.add((Math.random() - 0.5) * 2.0, 0, (Math.random() - 0.5) * 2.0);
                }
            }

            // Ambient sounds
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.6f, 0.3f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ColdDarkness(plugin); }
    }

    // =========================================================================
    // 6. FrostStatic — END_ROD particles flickering randomly + damage ticks
    //    at random intervals (10-30 ticks).
    // =========================================================================
    public static class FrostStatic extends EnvironmentalAttack {
        private Location center;
        private int nextDamageTick;

        public FrostStatic(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_static", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            nextDamageTick = 10 + ThreadLocalRandom.current().nextInt(21); // 10-30
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();
            ThreadLocalRandom rand = ThreadLocalRandom.current();

            // Random static flickers — END_ROD at random positions
            if (tick % 2 == 0) {
                int flickerCount = rand.nextInt(3, 8);
                for (int i = 0; i < flickerCount; i++) {
                    double x = (rand.nextDouble() - 0.5) * 14.0;
                    double y = rand.nextDouble() * 4.0;
                    double z = (rand.nextDouble() - 0.5) * 14.0;
                    Location loc = center.clone().add(x, y, z);
                    w.spawnParticle(Particle.END_ROD, loc, 1, 0.1, 0.1, 0.1, 0);
                }
            }

            // Occasional bright static bursts
            if (tick % 7 == 0) {
                double bx = (rand.nextDouble() - 0.5) * 10.0;
                double bz = (rand.nextDouble() - 0.5) * 10.0;
                Location burst = center.clone().add(bx, 1.5, bz);
                w.spawnParticle(Particle.END_ROD, burst, 6, 0.5, 0.5, 0.5, 0.02);
                DisplayBuilder.dustParticles(burst, 4, 0.5, 220, 240, 255, 1.0f);
            }

            // Damage at random intervals
            if (tick >= nextDamageTick) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);

                // Static shock burst at center
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 1, 0), 20, 5.0, 2.0, 5.0, 0.03);
                DisplayBuilder.dustParticles(center.clone().add(0, 1, 0), 15, 4.0, 255, 255, 255, 1.5f);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 49) { // 7^2
                        triggerImpactDamage(p.getLocation());
                    }
                }

                // Schedule next random damage
                nextDamageTick = tick + 10 + rand.nextInt(21);
            }

            // Low static hum
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 2.0f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostStatic(plugin); }
    }

    // =========================================================================
    // 7. ArcticNight — Gradual darkening particles overhead. Cold damage
    //    increases over the duration.
    // =========================================================================
    public static class ArcticNight extends EnvironmentalAttack {
        private Location center;

        public ArcticNight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("arctic_night", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.8f, 0.4f);
            // Initial sky darkening
            DisplayBuilder.dustParticles(center.clone().add(0, 8, 0), 20, 6.0, 30, 30, 50, 1.0f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Progress: 0.0 to 1.0 over duration
            double progress = Math.min(1.0, tick / 300.0);

            // Overhead darkening — gets denser over time
            if (tick % 4 == 0) {
                int particleCount = (int) (5 + progress * 25); // 5 to 30 particles
                for (int i = 0; i < particleCount; i++) {
                    double x = (Math.random() - 0.5) * 20.0;
                    double z = (Math.random() - 0.5) * 20.0;
                    double y = 5.0 + Math.random() * 5.0;
                    Location loc = center.clone().add(x, y, z);
                    // Particles get darker as attack progresses
                    int r = (int) (80 - progress * 50);
                    int g = (int) (80 - progress * 50);
                    int b = (int) (100 - progress * 50);
                    DisplayBuilder.dustParticles(loc, 2, 0.8, Math.max(r, 20), Math.max(g, 20), Math.max(b, 40), 1.5f);
                }
            }

            // Cold particles descending — more frequent as it progresses
            if (tick % Math.max(2, (int) (8 - progress * 6)) == 0) {
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 10.0;
                    double y = 3.0 + Math.random() * 3.0;
                    Location loc = center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(loc, 2, 0.3, 100, 180, 255, 0.8f);
                }
            }

            // Increasing damage — damage every 20 ticks at start, every 8 ticks at end
            int damageInterval = Math.max(8, (int) (20 - progress * 12));
            if (tick % damageInterval == 0 && tick > 20) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 100) { // 10^2
                        triggerImpactDamage(p.getLocation());
                    }
                }
            }

            // Ambient sounds that deepen
            if (tick % 50 == 0) {
                float pitch = (float) (0.6 - progress * 0.3);
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.7f, pitch);
            }

            // Ground frost ring expanding with progress
            if (tick % 15 == 0) {
                double radius = 3.0 + progress * 7.0;
                DisplayBuilder.particleRing(center, radius, Particle.DUST, 24,
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 0.8f));
            }
        }

        @Override public AbstractAttack newInstance() { return new ArcticNight(plugin); }
    }

    // =========================================================================
    // 8. CrystalRefraction — Prismatic colored dust beams in random directions.
    //    Damage at beam endpoints.
    // =========================================================================
    public static class CrystalRefraction extends EnvironmentalAttack {
        private Location center;
        private final List<double[]> beamAngles = new ArrayList<>();
        private final List<double[]> beamColors = new ArrayList<>();

        public CrystalRefraction(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_refraction", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            beamAngles.clear();
            beamColors.clear();
            // Generate 6 prismatic beams
            for (int i = 0; i < 6; i++) {
                double hAngle = Math.random() * 2 * Math.PI;
                double vAngle = 0.1 + Math.random() * 0.4; // Slight upward tilt
                double length = 5.0 + Math.random() * 5.0; // 5-10 block length
                beamAngles.add(new double[]{hAngle, vAngle, length});
                // Prismatic colors — cycling through icy spectrum
                switch (i % 6) {
                    case 0 -> beamColors.add(new double[]{220, 240, 255}); // icy white
                    case 1 -> beamColors.add(new double[]{100, 180, 255}); // cold blue
                    case 2 -> beamColors.add(new double[]{180, 130, 255}); // lavender
                    case 3 -> beamColors.add(new double[]{140, 255, 220}); // cyan-green
                    case 4 -> beamColors.add(new double[]{255, 200, 220}); // pink ice
                    case 5 -> beamColors.add(new double[]{200, 220, 255}); // pale blue
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.5f);
            // Central crystal glow
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 15, 0.3, 0.3, 0.3, 0.02);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Central crystal point
            if (tick % 5 == 0) {
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 3, 0.1, 0.1, 0.1, 0.01);
            }

            // Render each beam
            if (tick % 3 == 0) {
                for (int i = 0; i < beamAngles.size(); i++) {
                    double[] angles = beamAngles.get(i);
                    double[] color = beamColors.get(i);
                    double hAngle = angles[0] + tick * 0.02; // Slow rotation
                    double vAngle = angles[1];
                    double length = angles[2];

                    // Calculate beam endpoint
                    double ex = Math.cos(hAngle) * Math.cos(vAngle) * length;
                    double ey = Math.sin(vAngle) * length;
                    double ez = Math.sin(hAngle) * Math.cos(vAngle) * length;

                    Location beamStart = center.clone().add(0, 2, 0);
                    Location beamEnd = center.clone().add(ex, 2 + ey, ez);

                    // Draw beam
                    DisplayBuilder.particleLine(beamStart, beamEnd, Particle.DUST, 3,
                            new Particle.DustOptions(Color.fromRGB((int) color[0], (int) color[1], (int) color[2]), 0.8f));

                    // Bright endpoint
                    DisplayBuilder.dustParticles(beamEnd, 4, 0.5, (int) color[0], (int) color[1], (int) color[2], 1.2f);
                }
            }

            // Damage at beam endpoints every 15 ticks
            if (tick % 15 == 0) {
                for (double[] angles : beamAngles) {
                    double hAngle = angles[0] + tick * 0.02;
                    double vAngle = angles[1];
                    double length = angles[2];

                    double ex = Math.cos(hAngle) * Math.cos(vAngle) * length;
                    double ey = Math.sin(vAngle) * length;
                    double ez = Math.sin(hAngle) * Math.cos(vAngle) * length;
                    Location beamEnd = center.clone().add(ex, 2 + ey, ez);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(beamEnd) <= 4) { // 2-block radius per endpoint
                            triggerImpactDamage(p.getLocation());
                        }
                    }
                }
            }

            // Prismatic shimmer sound
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.8f);
            }
        }

        @Override public AbstractAttack newInstance() { return new CrystalRefraction(plugin); }
    }

    // =========================================================================
    // 9. BlindingSnow — Directional white particles from one direction + damage
    //    from that direction side.
    // =========================================================================
    public static class BlindingSnow extends EnvironmentalAttack {
        private Location center;
        private double windAngle; // Direction the blizzard blows FROM

        public BlindingSnow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blinding_snow", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            windAngle = Math.random() * 2 * Math.PI;
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Direction vectors for particle travel
            double dirX = Math.cos(windAngle);
            double dirZ = Math.sin(windAngle);

            // Spawn directional snow particles — originate from one side, blow across
            if (tick % 2 == 0) {
                for (int i = 0; i < 20; i++) {
                    // Origin: 10 blocks from center in the wind direction
                    double originOffset = -8.0 + Math.random() * 2.0;
                    double lateralSpread = (Math.random() - 0.5) * 16.0;
                    double y = Math.random() * 4.0;

                    // Perpendicular vector for lateral spread
                    double perpX = -dirZ;
                    double perpZ = dirX;

                    Location origin = center.clone().add(
                            dirX * originOffset + perpX * lateralSpread,
                            y,
                            dirZ * originOffset + perpZ * lateralSpread
                    );

                    // Particle with velocity in wind direction
                    w.spawnParticle(Particle.DUST, origin, 1,
                            dirX * 0.5, -0.05, dirZ * 0.5, 0.3,
                            new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.5f));
                }
            }

            // Dense core along wind path
            if (tick % 4 == 0) {
                for (double d = -6; d <= 6; d += 1.5) {
                    Location path = center.clone().add(dirX * d, 1.5, dirZ * d);
                    DisplayBuilder.dustParticles(path, 5, 1.0, 220, 240, 255, 1.2f);
                }
            }

            // Damage zone — the side the wind is blowing TOWARD (center + wind direction side)
            if (tick % 12 == 0) {
                Location damageCenter = center.clone().add(dirX * 3.0, 0, dirZ * 3.0);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(damageCenter) <= 36) { // 6^2, shifted toward wind
                        triggerImpactDamage(p.getLocation());
                    }
                }
            }

            // Wind gusts
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.8f, 0.4f);
            }
        }

        @Override public AbstractAttack newInstance() { return new BlindingSnow(plugin); }
    }

    // =========================================================================
    // 10. FrostFlicker — Alternating white burst / dark burst every 15 ticks.
    //     Damage only on white pulses.
    // =========================================================================
    public static class FrostFlicker extends EnvironmentalAttack {
        private Location center;

        public FrostFlicker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_flicker", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(210);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f, 2.0f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Alternating phases: 15 ticks white, 15 ticks dark
            boolean whitePulse = (tick / 15) % 2 == 0;

            if (whitePulse) {
                // WHITE BURST — bright particles + damage
                if (tick % 3 == 0) {
                    for (int i = 0; i < 25; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 7.0;
                        double y = Math.random() * 3.0;
                        Location loc = center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                        DisplayBuilder.dustParticles(loc, 3, 0.3, 255, 255, 255, 1.8f);
                    }
                    w.spawnParticle(Particle.END_ROD, center.clone().add(0, 1.5, 0), 5, 4.0, 2.0, 4.0, 0.02);
                }

                // Damage during white phase
                if (tick % 15 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.6f, 2.0f);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 49) { // 7^2
                            triggerImpactDamage(p.getLocation());
                        }
                    }
                }
            } else {
                // DARK BURST — dark cold particles, no damage
                if (tick % 3 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 7.0;
                        double y = Math.random() * 3.0;
                        Location loc = center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist);
                        DisplayBuilder.dustParticles(loc, 3, 0.3, 30, 30, 50, 1.5f);
                    }
                }

                // Transition sound
                if (tick % 15 == 0) {
                    DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.5f, 1.5f);
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrostFlicker(plugin); }
    }

    // =========================================================================
    // 11. GlacialGlare — Single intense END_ROD point at center, cone of damage
    //     radiating outward. Closer = more particles, damage extends in cone.
    // =========================================================================
    public static class GlacialGlare extends EnvironmentalAttack {
        private Location center;
        private double coneAngle; // Direction the cone faces

        public GlacialGlare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_glare", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            coneAngle = Math.random() * 2 * Math.PI;
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT_LAND, 1.0f, 2.0f);
            // Initial central flash
            center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 30, 0.2, 0.2, 0.2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Cone slowly rotates
            double currentAngle = coneAngle + tick * 0.03;
            double coneWidth = Math.PI / 3; // 60-degree cone

            // Intense center point — always bright
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 8, 0.15, 0.15, 0.15, 0.03);
                DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 5, 0.3, 255, 255, 255, 2.0f);
            }

            // Cone beam particles — fan out from center
            if (tick % 3 == 0) {
                for (int i = 0; i < 15; i++) {
                    double beamAngle = currentAngle + (Math.random() - 0.5) * coneWidth;
                    double dist = 1.0 + Math.random() * 9.0;
                    double y = 2.0 - (dist * 0.1); // Slight downward tilt
                    Location beamPoint = center.clone().add(
                            Math.cos(beamAngle) * dist, y, Math.sin(beamAngle) * dist);
                    DisplayBuilder.dustParticles(beamPoint, 2, 0.3, 220, 240, 255, 1.0f);

                    // END_ROD at shorter distances for intensity
                    if (dist < 5) {
                        w.spawnParticle(Particle.END_ROD, beamPoint, 1, 0.1, 0.1, 0.1, 0);
                    }
                }
            }

            // Cone edge lines
            if (tick % 8 == 0) {
                for (int side = -1; side <= 1; side += 2) {
                    double edgeAngle = currentAngle + side * (coneWidth / 2);
                    Location edgeEnd = center.clone().add(
                            Math.cos(edgeAngle) * 10.0, 1.0, Math.sin(edgeAngle) * 10.0);
                    DisplayBuilder.particleLine(center.clone().add(0, 2, 0), edgeEnd, Particle.DUST, 3,
                            new Particle.DustOptions(Color.fromRGB(100, 180, 255), 0.6f));
                }
            }

            // Damage in the cone area
            if (tick % 12 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    double dx = pLoc.getX() - center.getX();
                    double dz = pLoc.getZ() - center.getZ();
                    double distSq = dx * dx + dz * dz;
                    if (distSq > 100) continue; // Beyond 10-block radius

                    // Check if player is within cone angle
                    double playerAngle = Math.atan2(dz, dx);
                    double angleDiff = Math.abs(normalizeAngle(playerAngle - currentAngle));
                    if (angleDiff <= coneWidth / 2) {
                        triggerImpactDamage(pLoc);
                    }
                }
            }

            // Pulse sound
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 2.0f);
            }
        }

        /** Normalize angle to [-PI, PI] */
        private double normalizeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        @Override public AbstractAttack newInstance() { return new GlacialGlare(plugin); }
    }

    // =========================================================================
    // 12. IceVeilDrop — Curtain of white particles falling from y+10.
    //     Damage on contact as curtain descends.
    // =========================================================================
    public static class IceVeilDrop extends EnvironmentalAttack {
        private Location center;
        private double curtainY; // Current Y position of the descending curtain

        public IceVeilDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_veil_drop", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            curtainY = 10.0; // Start at +10 above center
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.8f, 0.5f);
            // Initial overhead particles
            DisplayBuilder.dustParticles(center.clone().add(0, 10, 0), 30, 5.0, 255, 255, 255, 1.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Curtain descends: 10 blocks over 160 ticks = 0.0625 blocks/tick
            if (curtainY > 0) {
                curtainY -= 0.0625;
            }

            // Draw the curtain — a horizontal plane of particles descending
            if (tick % 2 == 0) {
                for (int i = 0; i < 30; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 8.0;
                    Location particle = center.clone().add(
                            Math.cos(angle) * dist, curtainY, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(particle, 2, 0.3, 255, 255, 255, 1.5f);
                }

                // Trailing wisps above the curtain
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 6.0;
                    Location wisp = center.clone().add(
                            Math.cos(angle) * dist, curtainY + 0.5 + Math.random() * 2.0, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(wisp, 1, 0.5, 220, 240, 255, 0.8f);
                }
            }

            // Leading edge ring
            if (tick % 6 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, curtainY, 0), 8.0, Particle.DUST, 32,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.0f));
            }

            // Damage: players at or below the curtain's current Y within the radius
            if (tick % 10 == 0 && curtainY < 8) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    double dx = pLoc.getX() - center.getX();
                    double dz = pLoc.getZ() - center.getZ();
                    double horizontalDistSq = dx * dx + dz * dz;
                    if (horizontalDistSq > 64) continue; // 8^2

                    // Damage if player's head (y+1.8) is near or above curtain level
                    double playerTop = pLoc.getY() + 1.8;
                    double curtainWorldY = center.getY() + curtainY;
                    if (Math.abs(playerTop - curtainWorldY) < 2.0 || curtainWorldY < pLoc.getY() + 2.5) {
                        triggerImpactDamage(pLoc);
                        // Contact particles on player
                        DisplayBuilder.dustParticles(pLoc.clone().add(0, 1.5, 0), 5, 0.3, 255, 255, 255, 1.2f);
                    }
                }
            }

            // Sound as curtain descends
            if (tick % 30 == 0) {
                float pitch = (float) (1.5 - (curtainY / 10.0) * 0.8); // Pitch rises as it descends
                DisplayBuilder.playSound(center, Sound.BLOCK_POWDER_SNOW_FALL, 1.0f, pitch);
            }
        }

        @Override public AbstractAttack newInstance() { return new IceVeilDrop(plugin); }
    }

    // =========================================================================
    // 13. PermafrostMirage — Visible frost particles at one spot, but actual
    //     damage zone is 5 blocks offset. Deceptive attack.
    // =========================================================================
    public static class PermafrostMirage extends EnvironmentalAttack {
        private Location center;
        private Location visibleSpot;   // Where the particles appear (fake)
        private Location actualDamageZone; // Where damage actually is (5 blocks offset)

        public PermafrostMirage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_mirage", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();

            // Visible decoy spot — near center
            double visAngle = Math.random() * 2 * Math.PI;
            double visDist = Math.random() * 3.0;
            visibleSpot = center.clone().add(Math.cos(visAngle) * visDist, 0, Math.sin(visAngle) * visDist);

            // Actual damage zone — exactly 5 blocks offset from visible spot
            double offsetAngle = Math.random() * 2 * Math.PI;
            actualDamageZone = visibleSpot.clone().add(Math.cos(offsetAngle) * 5.0, 0, Math.sin(offsetAngle) * 5.0);

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // === VISIBLE DECOY — lots of obvious frost particles ===
            if (tick % 3 == 0) {
                // Dense frost cloud at the visible (fake) location
                DisplayBuilder.dustParticles(visibleSpot.clone().add(0, 1, 0), 15, 2.0, 220, 240, 255, 1.5f);
                DisplayBuilder.dustParticles(visibleSpot.clone().add(0, 0.5, 0), 8, 2.5, 255, 255, 255, 1.2f);
                w.spawnParticle(Particle.END_ROD, visibleSpot.clone().add(0, 1.5, 0), 3, 1.0, 0.5, 1.0, 0.01);
            }

            // Decoy ring
            if (tick % 10 == 0) {
                DisplayBuilder.particleRing(visibleSpot, 3.0, Particle.DUST, 20,
                        new Particle.DustOptions(Color.fromRGB(100, 180, 255), 1.0f));
            }

            // Decoy pillar particles (very visible, looks dangerous)
            if (tick % 6 == 0) {
                for (double y = 0; y < 4; y += 0.5) {
                    DisplayBuilder.dustParticles(visibleSpot.clone().add(0, y, 0), 2, 0.5, 220, 240, 255, 0.8f);
                }
            }

            // === ACTUAL DAMAGE ZONE — very subtle, nearly invisible particles ===
            if (tick % 8 == 0) {
                // Barely-visible cold shimmer at actual damage zone
                DisplayBuilder.dustParticles(actualDamageZone.clone().add(0, 0.5, 0), 2, 1.5, 220, 240, 255, 0.3f);
            }

            // Faint ground frost at actual zone — hard to notice
            if (tick % 20 == 0) {
                DisplayBuilder.particleRing(actualDamageZone, 2.5, Particle.DUST, 8,
                        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 0.3f));
            }

            // Damage at the ACTUAL zone (not the visible one)
            if (tick % 15 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(actualDamageZone) <= 25) { // 5^2
                        triggerImpactDamage(p.getLocation());
                        // Subtle frost on impact — reveals the trick
                        DisplayBuilder.dustParticles(p.getLocation().clone().add(0, 1, 0),
                                3, 0.3, 100, 180, 255, 0.6f);
                    }
                }
            }

            // Mirage shimmer sounds
            if (tick % 35 == 0) {
                DisplayBuilder.playSound(visibleSpot, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
                DisplayBuilder.playSound(actualDamageZone, Sound.BLOCK_POWDER_SNOW_STEP, 0.2f, 1.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new PermafrostMirage(plugin); }
    }
}
