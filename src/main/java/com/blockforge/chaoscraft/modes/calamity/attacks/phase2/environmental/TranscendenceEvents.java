package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * GROUP 10: TRANSCENDENCE EVENTS (Attacks 91-100)
 * Phase 2 (50% HP) — DoG's power radiating into the environment.
 * White radiance, god pressure, transcendent scars, cosmic exhale,
 * celestial impacts, apex manifestation, white silence, the gaze,
 * transcendence wave, and the final enrage "The Hunger is Complete."
 *
 * Palette: cyan (0,200,255), violet (128,0,255), white (240,240,255)
 * Materials: AMETHYST_BLOCK, SEA_LANTERN, END_ROD, POLISHED_BLACKSTONE, DARK_PRISMARINE
 */
public final class TranscendenceEvents {

    private TranscendenceEvents() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WhiteRadiance(plugin));
        registry.register(new GodPressure(plugin));
        registry.register(new TranscendentScar(plugin));
        registry.register(new CosmicExhale(plugin));
        registry.register(new CelestialImpact(plugin));
        registry.register(new ApexManifestation(plugin));
        registry.register(new WhiteSilence(plugin));
        registry.register(new TheGaze(plugin));
        registry.register(new TranscendenceWave(plugin));
        registry.register(new TheHungerIsComplete(plugin));
    }

    // =========================================================================
    // 91. WHITE RADIANCE
    // DoG's body emits a sudden bloom of pure white END_ROD particles —
    // a sphere of blinding light expands at 6 blocks/second from center.
    // The wave passes through all terrain and cover.
    // 12 HP per wave contact, 45s cooldown.
    // =========================================================================
    public static class WhiteRadiance extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double waveRadius = 0;

        public WhiteRadiance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("white_radiance", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(0.0); // Custom expanding check
            config.setDurationTicks(120);
            config.setCooldownTicks(900);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Central burst marker
            BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 1, 0),
                    Material.SEA_LANTERN);
            h.scale(1.0f, 1.0f, 1.0f).glow(240, 240, 255).interpolation(2, 0);
            handles.add(h);
            spawnedEntities.add(h.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 20 ticks (1 second) — speed spike indicator
            if (ticksAlive < 20) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 1, 0),
                            10, 1.0, 240, 240, 255, 1.5f);
                }
                return;
            }

            // Wave expansion: 6 blocks/second = 0.3 blocks/tick
            waveRadius += 0.3;

            // Expanding white sphere particles
            if (ticksAlive % 2 == 0 && waveRadius < 25) {
                World w = center.getWorld();
                if (w != null) {
                    // Horizontal ring at the wave front
                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0),
                            waveRadius, Particle.END_ROD, (int)(waveRadius * 3) + 6, null);
                    // Additional vertical rings for sphere appearance
                    if (waveRadius > 3) {
                        DisplayBuilder.particleRing(center.clone().add(0, 3, 0),
                                waveRadius * 0.8, Particle.END_ROD, (int)(waveRadius * 2), null);
                    }
                }
                DisplayBuilder.dustParticles(center, 6, (float)waveRadius,
                        240, 240, 255, 1.8f);
            }

            // Central burst pulsing
            if (!handles.isEmpty()) {
                float burstScale = 1.0f + (float)waveRadius * 0.1f;
                handles.get(0).scale(burstScale, burstScale, burstScale).interpolation(3, 0);
            }

            // Wave contact damage — ring check
            if (ticksAlive % 8 == 0 && waveRadius > 1) {
                World w = center.getWorld();
                if (w == null) return;
                for (var p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = p.getLocation().getX() - center.getX();
                    double dz = p.getLocation().getZ() - center.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    // Wave front is 3 blocks wide
                    if (Math.abs(dist - waveRadius) <= 3.0) {
                        p.damage(config.getDamage());
                        DisplayBuilder.dustParticles(p.getLocation(), 10, 0.5,
                                240, 240, 255, 1.6f);
                    }
                }
            }

            // Radiance sound
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WhiteRadiance(plugin); }
    }

    // =========================================================================
    // 92. GOD PRESSURE
    // Subtle attack — all particles on the island slow down, time drags.
    // A low-frequency tone increases in volume. Arena-wide damage aura
    // communicating DoG's total physical superiority.
    // 8 HP arena-wide over duration, 40s cooldown.
    // =========================================================================
    public static class GodPressure extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();

        public GodPressure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("god_pressure", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(8.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(80); // Once in the middle
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pressure field blocks — low, wide, nearly invisible
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double dist = 10.0;
                Location loc = center.clone().add(
                        Math.cos(angle) * dist, 0.3, Math.sin(angle) * dist);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(6.0f, 0.02f, 6.0f).glow(128, 0, 255).interpolation(5, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Gradual volume increase — tone building
            float volume = Math.min(1.5f, ticksAlive / 160.0f * 1.5f);
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, volume, 0.1f);
            }

            // Subtle: particles across island move slower — represented by sparse,
            // slow-moving dust that barely drifts
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < 4; i++) {
                    double ox = (Math.random() - 0.5) * 20;
                    double oz = (Math.random() - 0.5) * 20;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 1, oz),
                            2, 0.1, 128, 0, 255, 0.8f);
                }
            }

            // Pressure field visual — subtle ground shimmer
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.dustParticles(center, 4, 10.0, 240, 240, 255, 0.6f);
            }

            // Pressure pulse at midpoint
            if (ticksAlive == 80) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.1f);
                DisplayBuilder.dustParticles(center, 15, 12.0, 128, 0, 255, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GodPressure(plugin); }
    }

    // =========================================================================
    // 93. TRANSCENDENT SCAR
    // DoG's passage leaves a white scar — 4 blocks wide, emitting pure
    // END_ROD jets vertically. A nether star (amethyst block) rests at the
    // midpoint. Higher damage than Phase 1 scars, wider area.
    // 16 HP per second of contact, permanent for Phase 2.
    // =========================================================================
    public static class TranscendentScar extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location scarCenter;
        private double scarAngle;

        public TranscendentScar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("transcendent_scar", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(16.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600); // Long duration
            config.setCooldownTicks(400); // Fires often
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            scarAngle = Math.random() * Math.PI;
            scarCenter = center.clone();
            // Wide white scar — 4 blocks wide, 10 blocks long
            for (int i = -5; i <= 5; i++) {
                Location loc = center.clone().add(
                        Math.cos(scarAngle) * i, 0.05,
                        Math.sin(scarAngle) * i);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(4.0f, 0.1f, 1.0f)
                 .rotate((float)scarAngle, 0, 1, 0)
                 .glow(240, 240, 255)
                 .interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Nether star at midpoint (amethyst)
            BlockDisplayHandle star = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.3, 0), Material.AMETHYST_BLOCK);
            star.scale(0.5f, 0.5f, 0.5f).glow(240, 240, 255).interpolation(2, 0);
            handles.add(star);
            spawnedEntities.add(star.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // END_ROD jets from scar surface
            if (ticksAlive % 4 == 0) {
                World w = scarCenter.getWorld();
                if (w != null) {
                    for (int i = -4; i <= 4; i += 2) {
                        Location jetBase = scarCenter.clone().add(
                                Math.cos(scarAngle) * i, 0.2,
                                Math.sin(scarAngle) * i);
                        w.spawnParticle(Particle.END_ROD, jetBase, 3, 0.3, 2.0, 0.3, 0.02);
                    }
                }
                DisplayBuilder.dustParticles(scarCenter, 6, 3.0, 240, 240, 255, 1.5f);
            }

            // Rotate nether star slowly
            if (ticksAlive % 6 == 0) {
                BlockDisplayHandle star = handles.get(handles.size() - 1);
                BlockDisplay bd = (BlockDisplay) star.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.25f, -0.25f, -0.25f),
                        new AxisAngle4f(ticksAlive * 0.05f, 0, 1, 0),
                        new Vector3f(0.5f, 0.5f, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(4);
            }

            // Ambient glow
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(scarCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TranscendentScar(plugin); }
    }

    // =========================================================================
    // 94. COSMIC EXHALE
    // DoG's head exhales a dense stream of white END_ROD and CLOUD particles
    // in a wide 20-block cone aimed at the player cluster. Applies damage
    // burst to all caught in the cone.
    // 10 HP in exhale cone, 40s cooldown.
    // =========================================================================
    public static class CosmicExhale extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double coneAngle;
        private boolean exhaled = false;

        public CosmicExhale(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_exhale", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(10.0);
            config.setDamageRadius(0.0); // Custom cone check
            config.setDurationTicks(100);
            config.setCooldownTicks(800);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            coneAngle = Math.random() * 2 * Math.PI;
            // Hesitation marker — small amethyst block at source
            BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 2, 0),
                    Material.AMETHYST_BLOCK);
            h.scale(0.6f, 0.6f, 0.6f).glow(240, 240, 255).interpolation(2, 0);
            handles.add(h);
            spawnedEntities.add(h.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Hesitation: 16 ticks (0.8 seconds)
            if (ticksAlive < 16) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0),
                            6, 0.5, 240, 240, 255, 1.2f);
                }
                return;
            }

            // Exhale: 40 ticks (2 seconds)
            if (!exhaled && ticksAlive >= 16 && ticksAlive < 56) {
                // Dense cone of particles
                if (ticksAlive % 2 == 0) {
                    World w = center.getWorld();
                    if (w != null) {
                        for (int step = 2; step <= 20; step += 2) {
                            double spread = step * 0.3; // Wider further out
                            double baseX = Math.cos(coneAngle) * step;
                            double baseZ = Math.sin(coneAngle) * step;
                            Location pt = center.clone().add(baseX, 1, baseZ);
                            w.spawnParticle(Particle.END_ROD, pt, 3, spread, 0.5, spread, 0.01);
                            w.spawnParticle(Particle.CLOUD, pt, 2, spread, 0.3, spread, 0.01);
                        }
                    }
                    DisplayBuilder.dustParticles(center.clone().add(
                            Math.cos(coneAngle) * 10, 1, Math.sin(coneAngle) * 10),
                            8, 4.0, 240, 240, 255, 1.6f);
                }

                // Damage check in cone at tick 30 (midpoint of exhale)
                if (ticksAlive == 30) {
                    exhaled = true;
                    World w = center.getWorld();
                    if (w == null) return;
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (isInCone(p.getLocation(), center, coneAngle, 20, Math.PI / 4)) {
                            p.damage(config.getDamage());
                            DisplayBuilder.dustParticles(p.getLocation(), 10, 0.5,
                                    240, 240, 255, 1.5f);
                        }
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.2f, 1.8f);
                }
            }

            // Post-exhale dissipation
            if (ticksAlive >= 56 && ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(
                        Math.cos(coneAngle) * 8, 1, Math.sin(coneAngle) * 8),
                        4, 3.0, 240, 240, 255, 1.0f);
            }
        }

        private boolean isInCone(Location point, Location origin, double direction,
                                 double maxDist, double halfAngle) {
            double dx = point.getX() - origin.getX();
            double dz = point.getZ() - origin.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > maxDist || dist < 1.0) return false;
            double pointAngle = Math.atan2(dz, dx);
            double diff = Math.abs(normalizeAngle(pointAngle - direction));
            return diff <= halfAngle;
        }

        private double normalizeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicExhale(plugin); }
    }

    // =========================================================================
    // 95. CELESTIAL IMPACT
    // A massive 3x3x3 white end_stone projectile descends from Y+100,
    // blazing with END_ROD and ELECTRIC_SPARK particles. Targets island
    // center. Creates a 10-block shockwave on impact that launches players.
    // 10 HP inner zone (6-block), 12 HP outer zone (10-block), 75s cooldown.
    // =========================================================================
    public static class CelestialImpact extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double projectileY;
        private boolean impacted = false;

        public CelestialImpact(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("celestial_impact", AttackType.ENVIRONMENTAL, 2));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(1500);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            projectileY = 100.0;
            // 3x3x3 projectile block
            Location spawnLoc = center.clone().add(0, projectileY, 0);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                spawnLoc.clone().add(x, y, z), Material.SEA_LANTERN);
                        h.scale(1.0f, 1.0f, 1.0f).glow(240, 240, 255).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
            // Warning flash at Y+100
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 1.0f);
            World w2 = spawnLoc.getWorld();
            if (w2 != null) {
                w2.spawnParticle(Particle.END_ROD, spawnLoc, 30, 3, 3, 3, 0.05);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 60 ticks (3 seconds)
            if (ticksAlive < 60) {
                // Ground marker
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.particleRing(center, 6.0, Particle.ELECTRIC_SPARK, 16, null);
                    DisplayBuilder.dustParticles(center, 8, 3.0, 240, 240, 255, 1.2f);
                }
                return;
            }

            if (impacted) {
                // Post-impact: shockwave ring expanding
                int impactTick = ticksAlive - 60;
                if (impactTick < 40 && impactTick % 2 == 0) {
                    double shockRadius = impactTick * 0.5;
                    DisplayBuilder.particleRing(center, shockRadius,
                            Particle.ELECTRIC_SPARK, (int)(shockRadius * 3), null);
                    DisplayBuilder.dustParticles(center, 6, (float)shockRadius,
                            240, 240, 255, 1.5f);
                }
                return;
            }

            // Fall phase: drop projectile
            double fallSpeed = 1.5;
            projectileY -= fallSpeed;

            // Move all 27 blocks
            for (int i = 0; i < handles.size(); i++) {
                int x = (i % 3) - 1;
                int y = ((i / 3) % 3) - 1;
                int z = (i / 9) - 1;
                Location newLoc = center.clone().add(x, projectileY + y, z);
                handles.get(i).entity().teleport(newLoc);
            }

            // Trail particles
            if (ticksAlive % 2 == 0) {
                Location trailLoc = center.clone().add(0, projectileY, 0);
                World w = trailLoc.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.END_ROD, trailLoc, 6, 1, 1, 1, 0.02);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, trailLoc, 8, 1.5, 1.5, 1.5, 0.03);
                }
                DisplayBuilder.dustParticles(trailLoc, 8, 1.5, 240, 240, 255, 1.6f);
            }

            // Impact check
            if (projectileY <= 1.0) {
                impacted = true;
                triggerImpactDamage(center);
                // Massive shockwave burst
                DisplayBuilder.dustParticles(center, 60, 6.0, 240, 240, 255, 2.0f);
                DisplayBuilder.cyanDust(center, 30, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 2.0f, 0.3f);
                World w = center.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 1, 0),
                            50, 5, 3, 5, 0.1);
                    // Outer zone damage (6-10 block annular zone)
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double d2 = p.getLocation().distanceSquared(center);
                        if (d2 > 36.0 && d2 <= 100.0) {
                            p.damage(12.0);
                        }
                    }
                }
                // Hide projectile blocks
                for (BlockDisplayHandle h : handles) {
                    h.scale(0f, 0f, 0f).interpolation(0, 3);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CelestialImpact(plugin); }
    }

    // =========================================================================
    // 96. APEX MANIFESTATION
    // DoG's body flares — a 30-block sphere of END_ROD particles expands,
    // then collapses back inward creating a reverse-drag pulling players in.
    // 16 HP as sphere expands, 4 HP from inward drag, 60s cooldown.
    // =========================================================================
    public static class ApexManifestation extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double sphereRadius = 0;
        private boolean collapsing = false;

        public ApexManifestation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("apex_manifestation", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(16.0);
            config.setDamageRadius(0.0); // Custom
            config.setDurationTicks(100);
            config.setCooldownTicks(1200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Central flare marker
            BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 3, 0),
                    Material.AMETHYST_BLOCK);
            h.scale(2.0f, 2.0f, 2.0f).glow(240, 240, 255).interpolation(2, 0);
            handles.add(h);
            spawnedEntities.add(h.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 30 ticks (1.5 seconds)
            if (ticksAlive < 30) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 3, 0),
                            15, 2.0, 240, 240, 255, 1.8f);
                }
                return;
            }

            // Expand phase: 30 ticks — sphere grows to 15 blocks radius
            int actionTick = ticksAlive - 30;
            if (actionTick < 30 && !collapsing) {
                sphereRadius = (actionTick / 30.0) * 15.0;
                // Sphere particle ring at multiple heights
                if (actionTick % 2 == 0) {
                    World w = center.getWorld();
                    if (w != null) {
                        for (double yOff = -2; yOff <= 6; yOff += 2) {
                            double ringRadius = Math.sqrt(Math.max(0,
                                    sphereRadius * sphereRadius - yOff * yOff));
                            if (ringRadius > 0.5) {
                                DisplayBuilder.particleRing(center.clone().add(0, 3 + yOff, 0),
                                        ringRadius, Particle.END_ROD,
                                        (int)(ringRadius * 2) + 4, null);
                            }
                        }
                    }
                    DisplayBuilder.dustParticles(center, 8, (float)sphereRadius,
                            240, 240, 255, 1.5f);
                }

                // Damage at sphere front
                if (actionTick % 8 == 0) {
                    World w = center.getWorld();
                    if (w == null) return;
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = p.getLocation().distance(center.clone().add(0, 3, 0));
                        if (Math.abs(dist - sphereRadius) <= 3.0) {
                            p.damage(config.getDamage());
                            DisplayBuilder.dustParticles(p.getLocation(), 10, 0.5,
                                    240, 240, 255, 1.6f);
                        }
                    }
                }
            }

            // Collapse phase: 30 ticks — sphere shrinks back, pulls players in
            if (actionTick >= 30 && actionTick < 60) {
                collapsing = true;
                sphereRadius = 15.0 * (1.0 - (actionTick - 30) / 30.0);

                // Inward particle streams
                if (actionTick % 3 == 0) {
                    World w = center.getWorld();
                    if (w != null) {
                        for (int i = 0; i < 8; i++) {
                            double a = (2 * Math.PI * i) / 8;
                            Location outer = center.clone().add(
                                    Math.cos(a) * (sphereRadius + 5), 3,
                                    Math.sin(a) * (sphereRadius + 5));
                            DisplayBuilder.particleLine(outer, center.clone().add(0, 3, 0),
                                    Particle.END_ROD, 1, null);
                        }
                    }
                }

                // Pull players toward center
                if (actionTick % 4 == 0) {
                    World w = center.getWorld();
                    if (w == null) return;
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = center.getX() - p.getLocation().getX();
                        double dz = center.getZ() - p.getLocation().getZ();
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist > 1.0 && dist < 20.0) {
                            p.setVelocity(p.getVelocity().add(
                                    new org.bukkit.util.Vector(
                                            dx / dist * 0.15, 0,
                                            dz / dist * 0.15)));
                        }
                        // Drag damage if pulled close
                        if (dist <= 3.0) {
                            p.damage(4.0);
                        }
                    }
                }
            }

            // Scale central flare
            if (!handles.isEmpty()) {
                float flareScale = 2.0f + (float)sphereRadius * 0.2f;
                handles.get(0).scale(flareScale, flareScale, flareScale).interpolation(3, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ApexManifestation(plugin); }
    }

    // =========================================================================
    // 97. WHITE SILENCE
    // All audio hard-cuts to silence. Entire island covered in white CLOUD
    // particles at high density. Combined audio/visual blackout that removes
    // all sensory feedback simultaneously.
    // No direct damage, 70s cooldown.
    // =========================================================================
    public static class WhiteSilence extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();

        public WhiteSilence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("white_silence", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(1400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // White fog blocks — dense hemisphere
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                double dist = 8.0 + Math.random() * 6.0;
                double y = 2.0 + Math.random() * 4.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * dist, y, Math.sin(angle) * dist),
                        Material.SEA_LANTERN);
                h.scale(5.0f, 2.0f, 5.0f).glow(240, 240, 255).interpolation(5, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }
            // No warning — simultaneous onset
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dense white cloud particles
            if (ticksAlive % 2 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.CLOUD, center.clone().add(0, 2, 0),
                            20, 12, 4, 12, 0.01);
                }
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 24;
                    double oz = (Math.random() - 0.5) * 24;
                    DisplayBuilder.dustParticles(center.clone().add(ox, 1 + Math.random() * 4, oz),
                            4, 1.5, 240, 240, 255, 2.0f);
                }
            }

            // Scale fog blocks up for peak density
            if (ticksAlive < 40 && ticksAlive % 10 == 0) {
                float growth = 5.0f + (ticksAlive / 40.0f) * 5.0f;
                for (BlockDisplayHandle h : handles) {
                    h.scale(growth, 2.0f + ticksAlive / 40.0f * 2.0f, growth)
                     .interpolation(8, 0);
                }
            }

            // Fade out in last 20 ticks
            if (ticksAlive > 100 && ticksAlive % 5 == 0) {
                float fade = (ticksAlive - 100) / 20.0f;
                float s = 10.0f * (1.0f - fade);
                for (BlockDisplayHandle h : handles) {
                    h.scale(Math.max(0.1f, s), Math.max(0.1f, s * 0.4f), Math.max(0.1f, s))
                     .interpolation(4, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WhiteSilence(plugin); }
    }

    // =========================================================================
    // 98. THE GAZE
    // DoG's head turns toward a specific player — a narrow END_ROD beam locks
    // onto them. The targeted player gets Glowing effect (highlighted). DoG
    // prioritizes that player for the next 10 seconds.
    // 12 HP if DoG strikes the glowing target, 50s cooldown.
    // =========================================================================
    public static class TheGaze extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private Location gazeOrigin;
        private Location gazeTarget;
        private boolean gazeLocked = false;

        public TheGaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_gaze", AttackType.ENVIRONMENTAL, 2));
            config.setTracksPlayer(true);
            config.setDamage(12.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            gazeOrigin = center.clone().add(0, 8, 0); // DoG head position
            // Eye/lens block
            BlockDisplayHandle eye = displayBuilder.spawnBlock(gazeOrigin, Material.SEA_LANTERN);
            eye.scale(1.2f, 1.2f, 1.2f).glow(240, 240, 255).interpolation(2, 0);
            handles.add(eye);
            spawnedEntities.add(eye.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Find target player (nearest if none set)
            if (gazeTarget == null && getTargetPlayer() != null) {
                gazeTarget = getTargetPlayer().getLocation();
            }
            if (gazeTarget == null) {
                // Fallback: aim at center
                gazeTarget = center.clone();
            }

            // Gaze lock phase: beam from eye to target
            if (!gazeLocked && ticksAlive >= 10) {
                gazeLocked = true;
                DisplayBuilder.playSound(gazeOrigin, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.3f);
            }

            if (gazeLocked) {
                // Update target location if tracking
                if (getTargetPlayer() != null && getTargetPlayer().isOnline()) {
                    gazeTarget = getTargetPlayer().getLocation();
                }

                // Beam from eye to target
                if (ticksAlive % 3 == 0) {
                    World w = gazeOrigin.getWorld();
                    if (w != null) {
                        DisplayBuilder.particleLine(gazeOrigin, gazeTarget.clone().add(0, 1, 0),
                                Particle.END_ROD, 2, null);
                    }
                    DisplayBuilder.dustParticles(gazeTarget, 4, 0.5, 240, 240, 255, 1.2f);
                }

                // Pulse eye
                if (ticksAlive % 8 == 0 && !handles.isEmpty()) {
                    BlockDisplay bd = (BlockDisplay) handles.get(0).entity();
                    float pulse = 1.2f + (float)Math.sin(ticksAlive * 0.2) * 0.3f;
                    bd.setTransformation(new Transformation(
                            new Vector3f(-pulse / 2, -pulse / 2, -pulse / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, pulse, pulse),
                            new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }

                // Gaze effect particles around target
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(gazeTarget.clone().add(0, 2.2, 0), 4, 0.3);
                }
            }

            // Ambient gaze sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(gazeOrigin, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheGaze(plugin); }
    }

    // =========================================================================
    // 99. TRANSCENDENCE WAVE
    // A perfect white hemisphere of END_ROD particles expands from island
    // center upward and outward — a dome of white light covering the full
    // island. Cannot be avoided by position — only by jumping as the wave
    // passes ground level.
    // 12 HP to every player on the island, 55s cooldown.
    // =========================================================================
    public static class TranscendenceWave extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double domeRadius = 0;
        private boolean damageFired = false;

        public TranscendenceWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("transcendence_wave", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(12.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Central origin marker
            BlockDisplayHandle h = displayBuilder.spawnBlock(center, Material.AMETHYST_BLOCK);
            h.scale(1.5f, 1.5f, 1.5f).glow(240, 240, 255).interpolation(2, 0);
            handles.add(h);
            spawnedEntities.add(h.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center, 12, 2.0, 240, 240, 255, 1.5f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                            0.8f, 1.8f + ticksAlive / 40.0f * 0.4f);
                }
                return;
            }

            // Dome expansion: 60 ticks (3 seconds)
            int waveTick = ticksAlive - 40;
            if (waveTick < 60) {
                domeRadius = (waveTick / 60.0) * 25.0;
                double domeHeight = (waveTick / 60.0) * 25.0;

                // Hemisphere particles at multiple elevations
                if (waveTick % 2 == 0) {
                    World w = center.getWorld();
                    if (w != null) {
                        // Ground-level ring
                        DisplayBuilder.particleRing(center, domeRadius,
                                Particle.END_ROD, (int)(domeRadius * 2) + 8, null);
                        // Mid-height ring (smaller radius)
                        if (domeHeight > 5) {
                            double midRadius = domeRadius * 0.7;
                            DisplayBuilder.particleRing(center.clone().add(0, domeHeight * 0.5, 0),
                                    midRadius, Particle.END_ROD, (int)(midRadius * 2), null);
                        }
                        // Top ring (small)
                        if (domeHeight > 10) {
                            double topRadius = domeRadius * 0.3;
                            DisplayBuilder.particleRing(center.clone().add(0, domeHeight * 0.8, 0),
                                    topRadius, Particle.END_ROD, (int)(topRadius * 2), null);
                        }
                    }
                    DisplayBuilder.dustParticles(center, 6, (float)domeRadius,
                            240, 240, 255, 1.6f);
                }

                // Damage check — when wave passes ground level (~tick 10-20 of wave)
                if (waveTick >= 10 && waveTick <= 20 && !damageFired) {
                    damageFired = true;
                    World w = center.getWorld();
                    if (w == null) return;
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        // Players on the ground take damage; airborne players avoid it
                        // "On ground" = player Y within 1 block of center Y
                        if (Math.abs(p.getLocation().getY() - center.getY()) <= 1.5) {
                            p.damage(config.getDamage());
                            DisplayBuilder.dustParticles(p.getLocation(), 10, 0.5,
                                    240, 240, 255, 1.5f);
                        }
                    }
                }
            }

            // Scale origin marker to match dome
            if (!handles.isEmpty()) {
                float s = 1.5f + (float)domeRadius * 0.05f;
                handles.get(0).scale(s, s, s).interpolation(3, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TranscendenceWave(plugin); }
    }

    // =========================================================================
    // 100. THE HUNGER IS COMPLETE
    // Final enrage at 5% HP. The entire island burns white — maximum density
    // ELECTRIC_SPARK and END_ROD blanket every surface. A perfectly black
    // cube descends from Y+30 at 0.5 blocks/second — the fight timer. If
    // DoG dies the cube dissolves; if the cube lands all players die.
    // Enrage multipliers on all attacks; cube landing = instant death.
    // =========================================================================
    public static class TheHungerIsComplete extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private BlockDisplayHandle blackCube;
        private double cubeY = 30.0;

        public TheHungerIsComplete(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_hunger_is_complete", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(16.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(1300); // ~65 seconds until cube lands
            config.setCooldownTicks(999999); // Fires exactly once
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // The black cube — perfectly black, full-block size
            Location cubeSpawn = center.clone().add(0, cubeY, 0);
            blackCube = displayBuilder.spawnBlock(cubeSpawn, Material.POLISHED_BLACKSTONE);
            blackCube.scale(1.0f, 1.0f, 1.0f).glow(0, 0, 0).interpolation(3, 0);
            handles.add(blackCube);
            spawnedEntities.add(blackCube.entity());
            // Single slow roar
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Descend cube: 0.5 blocks/second = 0.025 blocks/tick
            cubeY -= 0.025;
            Location cubeLoc = center.clone().add(0, cubeY, 0);
            if (blackCube != null) {
                blackCube.entity().teleport(cubeLoc);
            }

            // Island-wide white chaos
            if (ticksAlive % 2 == 0) {
                World w = center.getWorld();
                if (w != null) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 2, 0),
                            20, 15, 4, 15, 0.08);
                    w.spawnParticle(Particle.END_ROD, center.clone().add(0, 3, 0),
                            12, 12, 5, 12, 0.03);
                }
                DisplayBuilder.dustParticles(center, 15, 15.0, 240, 240, 255, 2.0f);
                DisplayBuilder.cyanDust(center, 8, 10.0);
            }

            // Scar ignition: random ground bursts
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 4; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = Math.random() * 16;
                    Location burst = center.clone().add(
                            Math.cos(a) * d, 0.3, Math.sin(a) * d);
                    World w = burst.getWorld();
                    if (w != null) {
                        w.spawnParticle(Particle.ELECTRIC_SPARK, burst, 10, 0.5, 0.5, 0.5, 0.04);
                    }
                    DisplayBuilder.dustParticles(burst, 6, 1.0, 240, 240, 255, 1.5f);
                }
            }

            // Black cube visual — the only dark object, stark contrast
            if (ticksAlive % 5 == 0) {
                // Dark aura around cube
                DisplayBuilder.dustParticles(cubeLoc, 4, 0.8, 0, 0, 0, 1.5f);
                DisplayBuilder.dustParticles(cubeLoc, 2, 1.0, 128, 0, 255, 1.0f);
            }

            // Cube rotation as it descends
            if (ticksAlive % 4 == 0 && blackCube != null) {
                BlockDisplay bd = (BlockDisplay) blackCube.entity();
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.5f, -0.5f, -0.5f),
                        new AxisAngle4f(ticksAlive * 0.01f, 0, 1, 0),
                        new Vector3f(1.0f, 1.0f, 1.0f),
                        new AxisAngle4f(ticksAlive * 0.005f, 1, 0, 0)));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(4);
            }

            // Ambient sounds — escalating chaos
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.3f);
            }
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 0.5f);
            }

            // Cube landed check — enrage kill
            if (cubeY <= 0.5) {
                // Enrage: kill all players (void damage)
                World w = center.getWorld();
                if (w != null) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.1f);
                    DisplayBuilder.dustParticles(center, 80, 15.0, 0, 0, 0, 3.0f);
                    for (var p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        p.damage(9999.0); // Instant death
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheHungerIsComplete(plugin); }
    }
}
