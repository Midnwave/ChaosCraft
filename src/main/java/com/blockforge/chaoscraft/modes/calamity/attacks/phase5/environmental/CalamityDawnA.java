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
 * Phase 4E (Boss 5: Supreme Calamitas) Environmental Attacks #1-10
 * THE LIVING SKY -- sky streams weaponized as arena hazards.
 *
 * Calamitas palette: crimson (180,0,0), magenta (200,0,180), gold (255,215,0),
 *   purple (128,0,200), white END_ROD, brimstone (200,40,0), void (20,0,40),
 *   sigil purple (100,0,200), soul blue (0,180,255)
 *
 * Design: No status effects. Damage 6.0-18.0 HP. AxisAngle4f only.
 *   static DisplayBuilder helpers. triggerImpactDamage(). Container pattern.
 */
public final class CalamityDawnA {

    private CalamityDawnA() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrimsonCurtain(plugin));
        registry.register(new GoldCascade(plugin));
        registry.register(new WhiteFracture(plugin));
        registry.register(new StreamLockdown(plugin));
        registry.register(new MagentaWeb(plugin));
        registry.register(new PurpleDescent(plugin));
        registry.register(new StreamConvergenceSpike(plugin));
        registry.register(new CrimsonPulseRing(plugin));
        registry.register(new TotemCrown(plugin));
        registry.register(new StreamStutter(plugin));
    }

    // =========================================================================
    // 1. CRIMSON CURTAIN -- four crimson particle walls descend and sweep west
    // =========================================================================
    public static class CrimsonCurtain extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private boolean descended = false;

        public CrimsonCurtain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_curtain", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts per tick cycle
            config.setDamageRadius(0.0);
            config.setDurationTicks(180); // 9 seconds
            config.setCooldownTicks(900); // 45 seconds
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(false);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.AMBIENT_CRIMSON_FOREST_MOOD, 0.8f, 0.6f);
            // Ground warning lines at Y+2
            for (int z = -8; z <= 8; z += 4) {
                Location line = center.clone().add(-10, 2, z);
                DisplayBuilder.dustParticles(line, 30, 10.0, 180, 0, 0, 1.2f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-80 ticks (4 seconds) -- walls descending from Y+40 to Y+5
            if (ticksAlive <= 80) {
                if (ticksAlive % 4 == 0) {
                    double progress = ticksAlive / 80.0;
                    double currentY = center.getY() + 40 - 35 * progress;
                    for (int wall = 0; wall < 4; wall++) {
                        double wallZ = center.getZ() - 6 + wall * 4;
                        for (double x = -10; x <= 10; x += 1.0) {
                            Location pos = new Location(w, center.getX() + x, currentY, wallZ);
                            DisplayBuilder.dustParticles(pos, 3, 0.2, 180, 0, 0, 1.2f);
                        }
                    }
                }
                if (ticksAlive % 30 == 0) {
                    DisplayBuilder.playSound(center, Sound.AMBIENT_CRIMSON_FOREST_MOOD, 0.8f, 0.6f);
                }
                return;
            }

            // Spawn wall displays at descent completion
            if (!descended) {
                descended = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.2f, 0.5f);
                for (int wall = 0; wall < 4; wall++) {
                    double wallZ = center.getZ() - 6 + wall * 4;
                    Location wallBase = new Location(w, center.getX() + 10, center.getY() + 1, wallZ);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(wallBase, Material.RED_STAINED_GLASS);
                    h.scale(0.1f, 5.0f, 20.0f).glow(180, 0, 0).interpolation(5, 0);
                    wallHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Sweep phase: 81-180 ticks -- walls move westward
            int sweepTick = ticksAlive - 81;
            if (sweepTick >= 0 && sweepTick <= 100) {
                double sweepX = center.getX() + 10 - (sweepTick * 0.2);
                for (int wall = 0; wall < 4; wall++) {
                    double wallZ = center.getZ() - 6 + wall * 4;
                    // Particle emission along wall face
                    if (sweepTick % 2 == 0) {
                        for (double y = 0; y < 5; y += 1.0) {
                            Location wallPos = new Location(w, sweepX, center.getY() + y, wallZ);
                            DisplayBuilder.dustParticles(wallPos, 5, 0.3, 180, 0, 0, 1.2f);
                            // Flame trail at lower edge
                            if (y < 0.5) {
                                w.spawnParticle(Particle.FLAME, wallPos, 2, 0.1, 0.1, 0.1, 0.01);
                            }
                        }
                        // Splatter on ground contact
                        Location ground = new Location(w, sweepX, center.getY(), wallZ);
                        DisplayBuilder.dustParticles(ground, 8, 1.0, 220, 50, 50, 0.7f);
                    }

                    // Damage players in wall path
                    if (sweepTick % 10 == 0) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            Location pLoc = p.getLocation();
                            if (Math.abs(pLoc.getX() - sweepX) < 1.5 &&
                                Math.abs(pLoc.getZ() - wallZ) < 1.5 &&
                                pLoc.getY() < center.getY() + 5) {
                                p.damage(6.0); // 3 hearts
                            }
                        }
                    }
                }
                if (sweepTick % 24 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_HURT, 0.6f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonCurtain(plugin); }
    }

    // =========================================================================
    // 2. GOLD CASCADE -- TOTEM streams spiral down as helix columns
    // =========================================================================
    public static class GoldCascade extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> pillarHandles = new ArrayList<>();
        private final List<Location> columnPositions = new ArrayList<>();
        private boolean anchored = false;
        private int columnCount;

        public GoldCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gold_cascade", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0); // 5 hearts per column contact
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            columnCount = 3 + (int) (Math.random() * 4); // 3-6 columns
            for (int i = 0; i < columnCount; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 3 + Math.random() * 7;
                Location col = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                columnPositions.add(col);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-60 ticks -- helix descending
            if (ticksAlive <= 60) {
                double progress = ticksAlive / 60.0;
                double currentY = center.getY() + 50 * (1 - progress);
                for (Location col : columnPositions) {
                    double angle = ticksAlive * 0.314; // 18 degrees per tick
                    double hx = col.getX() + Math.cos(angle) * 4;
                    double hz = col.getZ() + Math.sin(angle) * 4;
                    Location helixPos = new Location(w, hx, currentY, hz);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, helixPos, 8, 0.3, 0.3, 0.3, 0.02);
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.5f);
                }
                // Ground preview rings
                if (ticksAlive % 10 == 0) {
                    for (Location col : columnPositions) {
                        DisplayBuilder.dustParticles(col, 15, 1.5, 255, 215, 0, 1.0f);
                    }
                }
                return;
            }

            // Anchor columns at tick 61
            if (!anchored) {
                anchored = true;
                for (Location col : columnPositions) {
                    triggerImpactDamage(col);
                    DisplayBuilder.playSound(col, Sound.BLOCK_BEACON_POWER_SELECT, 1.2f, 0.8f);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, col, 60, 1.5, 1.5, 1.5, 0.1);
                    DisplayBuilder.dustParticles(col, 50, 3.0, 255, 215, 0, 1.5f);

                    // Golden pillar display
                    BlockDisplayHandle h = displayBuilder.spawnBlock(col.clone().add(0, 1, 0), Material.GOLD_BLOCK);
                    h.scale(0.5f, 8.0f, 0.5f).glow(255, 215, 0).interpolation(5, 0);
                    pillarHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Column damage phase: 61-160 ticks
            int columnTick = ticksAlive - 61;
            if (columnTick >= 0 && columnTick <= 100) {
                if (columnTick % 4 == 0) {
                    for (Location col : columnPositions) {
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, col.clone().add(0, 1, 0), 15, 0.5, 4, 0.5, 0.02);
                    }
                }
                // Pulse burst every 20 ticks
                if (columnTick % 20 == 0) {
                    for (Location col : columnPositions) {
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, col.clone().add(0, 4, 0), 60, 3, 3, 3, 0.05);
                        DisplayBuilder.playSound(col, Sound.ENTITY_EVOKER_CAST_SPELL, 0.7f, 1.1f);
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(col) <= 9.0) {
                                p.damage(6.0); // 3 hearts per pulse while in column
                            }
                        }
                    }
                }
            }

            // Dissolve: 161-200 ticks
            if (columnTick > 100 && columnTick <= 140) {
                if (columnTick == 101) {
                    for (Location col : columnPositions) {
                        DisplayBuilder.playSound(col, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.9f, 1.3f);
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, col.clone().add(0, 2, 0), 40, 1, 4, 1, 0.15);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GoldCascade(plugin); }
    }

    // =========================================================================
    // 3. WHITE FRACTURE -- END_ROD streams shatter into ballistic fragment rain
    // =========================================================================
    public static class WhiteFracture extends EnvironmentalAttack {

        private boolean shattered = false;

        public WhiteFracture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("white_fracture", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(360); // 18 seconds (10 rain + 8 dark)
            config.setCooldownTicks(1400); // 70 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts per fragment hit
            config.setImpactRadius(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Jittering streams -- rapid glass sounds
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-100 ticks (5 seconds) -- streams jittering
            if (ticksAlive <= 100) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.8f);
                    // Jittering white particles at sky level
                    for (int i = 0; i < 6; i++) {
                        double sx = center.getX() + (Math.random() - 0.5) * 20;
                        double sz = center.getZ() + (Math.random() - 0.5) * 20;
                        Location skyPos = new Location(w, sx, center.getY() + 30 + Math.random() * 10, sz);
                        w.spawnParticle(Particle.END_ROD, skyPos, 5, 0.5, 0.5, 0.5, 0.02);
                    }
                }
                // Crack lines growing upward
                if (ticksAlive > 60 && ticksAlive % 8 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = 2 + Math.random() * 6;
                        Location crackBase = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                        for (double y = 0; y < 20; y += 2) {
                            w.spawnParticle(Particle.END_ROD, crackBase.clone().add(0, y, 0), 1, 0.05, 0.05, 0.05, 0);
                        }
                    }
                }
                return;
            }

            // Shatter at tick 101
            if (!shattered) {
                shattered = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 1.4f);
                // Cloud burst at stream positions
                for (int i = 0; i < 5; i++) {
                    double sx = center.getX() + (Math.random() - 0.5) * 16;
                    double sz = center.getZ() + (Math.random() - 0.5) * 16;
                    Location streamPos = new Location(w, sx, center.getY() + 35, sz);
                    w.spawnParticle(Particle.CLOUD, streamPos, 30, 3, 3, 3, 0.05);
                    w.spawnParticle(Particle.END_ROD, streamPos, 40, 4, 4, 4, 0.1);
                }
            }

            // Fragment rain: ticks 101-300 (10 seconds)
            int rainTick = ticksAlive - 101;
            if (rainTick >= 0 && rainTick <= 200) {
                // ~3 fragments per tick = 50-70 per second
                int fragmentsThisTick = 2 + (int) (Math.random() * 3);
                for (int f = 0; f < fragmentsThisTick; f++) {
                    double fx = center.getX() + (Math.random() - 0.5) * 20;
                    double fz = center.getZ() + (Math.random() - 0.5) * 20;
                    double fallProgress = Math.min(1.0, (rainTick + Math.random() * 10) / 40.0);
                    double fy = center.getY() + 40 * (1 - fallProgress);
                    if (fy <= center.getY() + 0.5) {
                        // Ground impact
                        Location impact = new Location(w, fx, center.getY(), fz);
                        w.spawnParticle(Particle.END_ROD, impact, 8, 0.3, 0.3, 0.3, 0.02);
                        // Check player hits
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(impact) <= 1.0) {
                                p.damage(6.0); // 3 hearts per fragment
                            }
                        }
                    } else {
                        Location fragPos = new Location(w, fx, fy, fz);
                        w.spawnParticle(Particle.END_ROD, fragPos, 1, 0, 0, 0, 0);
                    }
                }
                // Random impact sounds
                if (rainTick % 5 == 0) {
                    double rx = center.getX() + (Math.random() - 0.5) * 18;
                    double rz = center.getZ() + (Math.random() - 0.5) * 18;
                    DisplayBuilder.playSound(new Location(w, rx, center.getY(), rz),
                            Sound.BLOCK_GLASS_BREAK, 0.3f, 1.4f + (float) Math.random() * 0.5f);
                }
            }

            // Silence window: 301-360 (dark sky, no streams)
            if (rainTick > 200 && rainTick % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.1f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WhiteFracture(plugin); }
    }

    // =========================================================================
    // 4. STREAM LOCKDOWN -- all 5 streams form a shrinking rectangular barrier
    // =========================================================================
    public static class StreamLockdown extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> cornerPillars = new ArrayList<>();
        private double currentSize = 20.0;

        public StreamLockdown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_lockdown", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(8.0); // 4 hearts per cycle outside
            config.setDamageRadius(0.0);
            config.setDurationTicks(1140); // ~57 seconds total
            config.setCooldownTicks(6000); // HP threshold, not random
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_CONDUIT_ACTIVATE, 1.3f, 0.7f);
            currentSize = 20.0;
            // Corner pillars
            for (int c = 0; c < 4; c++) {
                double px = (c < 2) ? center.getX() - 10 : center.getX() + 10;
                double pz = (c % 2 == 0) ? center.getZ() - 10 : center.getZ() + 10;
                Location pillarPos = new Location(w, px, center.getY(), pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(pillarPos, Material.CRYING_OBSIDIAN);
                h.scale(1.5f, 8.0f, 1.5f).glow(128, 0, 200).interpolation(20, 0);
                cornerPillars.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-80 ticks (4 seconds)
            if (ticksAlive <= 80) {
                if (ticksAlive % 10 == 0) {
                    double half = currentSize / 2.0;
                    // Perimeter particles -- all 5 types mixed
                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0), half, Particle.WITCH, 40, null);
                    DisplayBuilder.particleRing(center.clone().add(0, 2, 0), half, Particle.TOTEM_OF_UNDYING, 30, null);
                }
                if (ticksAlive == 40) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 1.0f, 0.5f);
                }
                return;
            }

            // Shrink phase: 81-1080 ticks -- box shrinks by 1 block every 20 ticks
            if (ticksAlive > 80 && ticksAlive <= 1080) {
                if ((ticksAlive - 80) % 20 == 0 && currentSize > 10.0) {
                    currentSize -= 1.0;
                }

                double half = currentSize / 2.0;

                // Wall particles every 4 ticks
                if (ticksAlive % 4 == 0) {
                    for (double y = 1; y <= 5; y += 1.0) {
                        // North/South walls
                        for (double x = -half; x <= half; x += 2.0) {
                            Location n = new Location(w, center.getX() + x, center.getY() + y, center.getZ() - half);
                            Location s = new Location(w, center.getX() + x, center.getY() + y, center.getZ() + half);
                            w.spawnParticle(Particle.WITCH, n, 1, 0.1, 0.1, 0.1, 0);
                            w.spawnParticle(Particle.END_ROD, s, 1, 0.1, 0.1, 0.1, 0);
                        }
                        // East/West walls
                        for (double z = -half; z <= half; z += 2.0) {
                            Location e = new Location(w, center.getX() + half, center.getY() + y, center.getZ() + z);
                            Location wl = new Location(w, center.getX() - half, center.getY() + y, center.getZ() + z);
                            w.spawnParticle(Particle.DRAGON_BREATH, e, 1, 0.1, 0.1, 0.1, 0);
                            DisplayBuilder.dustParticles(wl, 1, 0.1, 180, 0, 0, 1.0f);
                        }
                    }
                }

                // Damage players outside boundary
                if (ticksAlive % 10 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location pl = p.getLocation();
                        double dx = Math.abs(pl.getX() - center.getX());
                        double dz = Math.abs(pl.getZ() - center.getZ());
                        if (dx > half || dz > half) {
                            p.damage(8.0); // 4 hearts per cycle
                        }
                    }
                }

                if (ticksAlive % 40 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.6f);
                }
            }

            // Dissolution: 1081-1140 ticks
            if (ticksAlive > 1080) {
                if (ticksAlive == 1081) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.9f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
                    double half = currentSize / 2.0;
                    // Explosion burst of all 5 types
                    w.spawnParticle(Particle.WITCH, center.clone().add(0, 3, 0), 120, half, 3, half, 0.1);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 3, 0), 120, half, 3, half, 0.1);
                    w.spawnParticle(Particle.END_ROD, center.clone().add(0, 3, 0), 120, half, 3, half, 0.1);
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 3, 0), 120, half, 3, half, 0.1);
                    DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 120, half, 180, 0, 0, 1.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamLockdown(plugin); }
    }

    // =========================================================================
    // 5. MAGENTA WEB -- 8-spoke rotating web of SPELL_WITCH particles
    // =========================================================================
    public static class MagentaWeb extends EnvironmentalAttack {

        private BlockDisplayHandle spinnerHandle;
        private boolean anchored = false;
        private double rotationAngle = 0;

        public MagentaWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magenta_web", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts per damage tick
            config.setDamageRadius(0.0);
            config.setDurationTicks(240); // 12 seconds
            config.setCooldownTicks(1100); // 55 seconds
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_WITCH_AMBIENT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-60 ticks (3 seconds) -- web descending
            if (ticksAlive <= 60) {
                double progress = ticksAlive / 60.0;
                double currentY = center.getY() + 30 * (1 - progress);
                if (ticksAlive % 3 == 0) {
                    for (int spoke = 0; spoke < 8; spoke++) {
                        double angle = (2 * Math.PI * spoke) / 8;
                        for (double r = 0; r <= 10; r += 1.5) {
                            double sx = center.getX() + Math.cos(angle) * r;
                            double sz = center.getZ() + Math.sin(angle) * r;
                            Location spokePos = new Location(w, sx, currentY, sz);
                            w.spawnParticle(Particle.WITCH, spokePos, 2, 0.1, 0.1, 0.1, 0);
                        }
                    }
                }
                if (ticksAlive == 40) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITCH_THROW, 0.9f, 0.7f);
                }
                return;
            }

            // Anchor and start rotation
            if (!anchored) {
                anchored = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);
                spinnerHandle = displayBuilder.spawnBlock(center.clone().add(0, 1, 0), Material.MAGENTA_CONCRETE);
                spinnerHandle.scale(2.0f, 0.5f, 2.0f).glow(200, 0, 180).interpolation(5, 0);
                spawnedEntities.add(spinnerHandle.entity());
            }

            // Rotating phase: 61-220 ticks (8 seconds)
            int rotTick = ticksAlive - 61;
            if (rotTick >= 0 && rotTick <= 160) {
                rotationAngle += Math.toRadians(1.0); // 1 degree per tick

                if (rotTick % 3 == 0) {
                    for (int spoke = 0; spoke < 8; spoke++) {
                        double angle = (2 * Math.PI * spoke) / 8 + rotationAngle;
                        for (double r = 0; r <= 10; r += 1.0) {
                            double sx = center.getX() + Math.cos(angle) * r;
                            double sz = center.getZ() + Math.sin(angle) * r;
                            Location spokePos = new Location(w, sx, center.getY() + 0.5, sz);
                            w.spawnParticle(Particle.WITCH, spokePos, 2, 0.1, 0.1, 0.1, 0);
                            // Tip particles
                            if (r > 9) {
                                DisplayBuilder.dustParticles(spokePos, 2, 0.2, 180, 0, 180, 1.0f);
                            }
                        }
                    }
                }

                // Damage: players standing on a spoke
                if (rotTick % 15 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location pl = p.getLocation();
                        double pdx = pl.getX() - center.getX();
                        double pdz = pl.getZ() - center.getZ();
                        double playerAngle = Math.atan2(pdz, pdx);
                        double playerDist = Math.sqrt(pdx * pdx + pdz * pdz);
                        if (playerDist > 10) continue;
                        // Check if on any spoke (2 block width = ~11 degrees at most ranges)
                        for (int spoke = 0; spoke < 8; spoke++) {
                            double spokeAngle = (2 * Math.PI * spoke) / 8 + rotationAngle;
                            double angleDiff = Math.abs(normalizeAngle(playerAngle - spokeAngle));
                            if (angleDiff < 0.2) { // ~11 degrees
                                p.damage(6.0); // 3 hearts
                                break;
                            }
                        }
                    }
                }

                if (rotTick % 30 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITCH_AMBIENT, 0.3f, 0.7f);
                }
            }

            // Collapse: 221-240 ticks
            if (rotTick > 160) {
                if (rotTick == 161) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITCH_DEATH, 0.8f, 1.0f);
                    w.spawnParticle(Particle.WITCH, center.clone().add(0, 1, 0), 120, 5, 1, 5, 0.05);
                }
            }
        }

        private double normalizeAngle(double a) {
            while (a > Math.PI) a -= 2 * Math.PI;
            while (a < -Math.PI) a += 2 * Math.PI;
            return a;
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagentaWeb(plugin); }
    }

    // =========================================================================
    // 6. PURPLE DESCENT -- massive DRAGON_BREATH cloud descends over arena
    // =========================================================================
    public static class PurpleDescent extends EnvironmentalAttack {

        private boolean splashed = false;

        public PurpleDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("purple_descent", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts per 8 ticks in cloud
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds total
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-80 ticks (4 seconds) -- purple haze intensifying at sky level
            if (ticksAlive <= 80) {
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double hx = center.getX() + (Math.random() - 0.5) * 18;
                        double hz = center.getZ() + (Math.random() - 0.5) * 18;
                        double hy = center.getY() + 20 + Math.random() * 10;
                        w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, hx, hy, hz), 3, 1, 1, 1, 0.01);
                    }
                }
                return;
            }

            // Descent: 81-120 ticks (2 seconds) -- cloud drops from Y+20 to Y+0
            if (ticksAlive > 80 && ticksAlive <= 120) {
                double progress = (ticksAlive - 80) / 40.0;
                double cloudY = center.getY() + 20 * (1 - progress);
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 30; i++) {
                        double cx = center.getX() + (Math.random() - 0.5) * 18;
                        double cz = center.getZ() + (Math.random() - 0.5) * 18;
                        double cy = cloudY + (Math.random() - 0.5) * 8;
                        w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, cx, cy, cz), 2, 0.5, 0.5, 0.5, 0.01);
                    }
                }

                // Damage players inside the cloud
                if (ticksAlive % 8 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double py = p.getLocation().getY();
                        if (py >= cloudY - 4 && py <= cloudY + 4) {
                            double dx = Math.abs(p.getLocation().getX() - center.getX());
                            double dz = Math.abs(p.getLocation().getZ() - center.getZ());
                            if (dx <= 9 && dz <= 9) {
                                p.damage(6.0); // 3 hearts
                            }
                        }
                    }
                }

                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 0.5f);
                }
            }

            // Splash at ground level
            if (ticksAlive > 120 && !splashed) {
                splashed = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_HURT, 1.2f, 0.8f);
                for (int i = 0; i < 80; i++) {
                    double sx = center.getX() + (Math.random() - 0.5) * 20;
                    double sz = center.getZ() + (Math.random() - 0.5) * 20;
                    w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, sx, center.getY() + 0.5, sz), 3, 0.5, 0.2, 0.5, 0.05);
                }
            }

            // Evaporation: 121-200 ticks
            if (ticksAlive > 120 && ticksAlive <= 200) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double ex = center.getX() + (Math.random() - 0.5) * 18;
                        double ez = center.getZ() + (Math.random() - 0.5) * 18;
                        double ey = center.getY() + ((ticksAlive - 120) / 80.0) * 15;
                        w.spawnParticle(Particle.DRAGON_BREATH, new Location(w, ex, ey, ez), 2, 0.5, 0.5, 0.5, 0.02);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PurpleDescent(plugin); }
    }

    // =========================================================================
    // 7. STREAM CONVERGENCE SPIKE -- all 5 streams merge then spike downward
    // =========================================================================
    public static class StreamConvergenceSpike extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> scorchHandles = new ArrayList<>();
        private boolean spiked = false;

        public StreamConvergenceSpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_convergence_spike", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(6000); // HP threshold
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0); // 8 hearts direct hit
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-100 ticks (5 seconds) -- streams merging to center sky
            if (ticksAlive <= 100) {
                double progress = ticksAlive / 100.0;
                if (ticksAlive % 4 == 0) {
                    for (int s = 0; s < 5; s++) {
                        double angle = (2 * Math.PI * s) / 5;
                        double dist = 15 * (1 - progress);
                        double sx = center.getX() + Math.cos(angle) * dist;
                        double sz = center.getZ() + Math.sin(angle) * dist;
                        double sy = center.getY() + 40 + 20 * (1 - progress);
                        Location streamPos = new Location(w, sx, sy, sz);
                        // Each stream emits its type
                        switch (s) {
                            case 0 -> w.spawnParticle(Particle.WITCH, streamPos, 8, 0.5, 0.5, 0.5, 0);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamPos, 8, 0.5, 0.5, 0.5, 0.02);
                            case 2 -> w.spawnParticle(Particle.END_ROD, streamPos, 8, 0.5, 0.5, 0.5, 0.02);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, streamPos, 8, 0.5, 0.5, 0.5, 0.01);
                            case 4 -> DisplayBuilder.dustParticles(streamPos, 8, 0.5, 180, 0, 0, 1.5f);
                        }
                    }
                    // Merged column pulsing in last 2 seconds
                    if (progress > 0.6) {
                        Location mergePoint = center.clone().add(0, 60, 0);
                        w.spawnParticle(Particle.WITCH, mergePoint, 20, 2 * progress, 2, 2 * progress, 0);
                        w.spawnParticle(Particle.END_ROD, mergePoint, 10, 1.5, 1.5, 1.5, 0.02);
                    }
                }
                return;
            }

            // Spike descent: tick 101-115 (15 ticks from Y+60 to Y+0)
            if (ticksAlive > 100 && ticksAlive <= 115) {
                double progress = (ticksAlive - 100) / 15.0;
                double spikeY = center.getY() + 60 * (1 - progress);
                Location spikePos = center.clone().add(0, spikeY - center.getY(), 0);
                // Mixed particle cylinder
                w.spawnParticle(Particle.WITCH, spikePos, 30, 2, 2, 2, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, spikePos, 20, 2, 2, 2, 0.05);
                w.spawnParticle(Particle.END_ROD, spikePos, 20, 2, 2, 2, 0.05);
                w.spawnParticle(Particle.DRAGON_BREATH, spikePos, 20, 2, 2, 2, 0.02);
                DisplayBuilder.dustParticles(spikePos, 20, 2.0, 180, 0, 0, 1.5f);
            }

            // Impact at tick 116
            if (ticksAlive > 115 && !spiked) {
                spiked = true;
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.6f);

                // Impact burst
                w.spawnParticle(Particle.WITCH, center, 200, 4, 2, 4, 0.1);
                w.spawnParticle(Particle.END_ROD, center, 100, 4, 2, 4, 0.1);
                w.spawnParticle(Particle.FLASH, center, 2, 0, 0, 0, 0);

                // Shockwave ring damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist > 4 && dist <= 10) {
                        p.damage(8.0); // 4 hearts shockwave
                        // Knockback away from center
                        org.bukkit.util.Vector kb = p.getLocation().toVector()
                                .subtract(center.toVector()).normalize().multiply(1.5);
                        kb.setY(0.4);
                        p.setVelocity(kb);
                    }
                }

                // Scorch mark -- 5x5 alternating blackstone/magma
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        Location spos = center.clone().add(x, 0.05, z);
                        Material mat = ((x + z) % 2 == 0) ? Material.BLACKSTONE : Material.MAGMA_BLOCK;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(spos, mat);
                        h.scale(1.0f, 0.05f, 1.0f).glow(128, 0, 128).interpolation(5, 0);
                        scorchHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Shockwave ring animation: ticks 116-126
            if (ticksAlive > 115 && ticksAlive <= 126) {
                double ringRadius = (ticksAlive - 115) * 2.0;
                DisplayBuilder.particleRing(center.clone().add(0, 1.5, 0), ringRadius, Particle.WITCH, 40, null);
                DisplayBuilder.particleRing(center.clone().add(0, 1.5, 0), ringRadius, Particle.END_ROD, 20, null);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamConvergenceSpike(plugin); }
    }

    // =========================================================================
    // 8. CRIMSON PULSE RING -- expanding crimson rings from center
    // =========================================================================
    public static class CrimsonPulseRing extends EnvironmentalAttack {

        private boolean fired = false;

        public CrimsonPulseRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_pulse_ring", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(700); // 35 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(7.0); // 3.5 hearts per ring
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.9f, 0.5f);
            // Warning pulse
            DisplayBuilder.particleRing(center.clone().add(0, 1, 0), 1.0, Particle.DUST,
                    20, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-40 ticks (2 seconds) -- pulsing ring at center
            if (ticksAlive <= 40) {
                if (ticksAlive % 10 == 0) {
                    double pulseRadius = 1 + (ticksAlive % 20 == 0 ? 1 : 0);
                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0), pulseRadius, Particle.DUST,
                            20, new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.7f, 0.5f);
                }
                return;
            }

            // Three rings expanding outward: tick 41, 51, 61
            int ringTick = ticksAlive - 41;
            if (ringTick >= 0) {
                // Ring 1
                double r1 = ringTick * 1.5;
                if (r1 <= 15) {
                    DisplayBuilder.particleRing(center.clone().add(0, 1, 0), r1, Particle.DUST,
                            (int) (r1 * 5), new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), r1, Particle.DUST,
                            (int) (r1 * 3), new Particle.DustOptions(Color.fromRGB(255, 100, 100), 0.8f));
                    // Damage
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double pDist = p.getLocation().distance(center);
                        if (Math.abs(pDist - r1) < 1.5) {
                            p.damage(7.0); // 3.5 hearts
                        }
                    }
                }

                // Ring 2 (10 ticks later)
                int r2Tick = ringTick - 10;
                if (r2Tick >= 0) {
                    double r2 = r2Tick * 1.5;
                    if (r2 <= 15) {
                        DisplayBuilder.particleRing(center.clone().add(0, 1, 0), r2, Particle.DUST,
                                (int) (r2 * 5), new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));
                    }
                }

                // Ring 3 (20 ticks later)
                int r3Tick = ringTick - 20;
                if (r3Tick >= 0) {
                    double r3 = r3Tick * 1.5;
                    if (r3 <= 15) {
                        DisplayBuilder.particleRing(center.clone().add(0, 1, 0), r3, Particle.DUST,
                                (int) (r3 * 5), new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));
                    }
                }

                // Perimeter climb sound
                if (ringTick % 10 == 0 && r1 >= 10) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_HIT, 0.6f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonPulseRing(plugin); }
    }

    // =========================================================================
    // 9. TOTEM CROWN -- golden crown drops 8 prongs that rotate
    // =========================================================================
    public static class TotemCrown extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> prongHandles = new ArrayList<>();
        private final double[] prongAngles = new double[8];
        private boolean dropped = false;
        private double crownRotation = 0;

        public TotemCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("totem_crown", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(6.0); // 3 hearts per 15 ticks
            config.setDamageRadius(0.0);
            config.setDurationTicks(260); // 13 seconds
            config.setCooldownTicks(1600); // 80 seconds
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.9f);
            for (int i = 0; i < 8; i++) {
                prongAngles[i] = (2 * Math.PI * i) / 8;
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-60 ticks (3 seconds) -- crown forming at Y+15
            if (ticksAlive <= 60) {
                if (ticksAlive % 4 == 0) {
                    crownRotation += 0.05;
                    for (int i = 0; i < 8; i++) {
                        double angle = prongAngles[i] + crownRotation;
                        double rx = center.getX() + Math.cos(angle) * 6;
                        double rz = center.getZ() + Math.sin(angle) * 6;
                        Location crownPos = new Location(w, rx, center.getY() + 15, rz);
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, crownPos, 5, 0.2, 0.2, 0.2, 0.02);
                    }
                }
                return;
            }

            // Drop prongs: tick 61
            if (!dropped) {
                dropped = true;
                for (int i = 0; i < 8; i++) {
                    double angle = prongAngles[i];
                    double px = center.getX() + Math.cos(angle) * 6;
                    double pz = center.getZ() + Math.sin(angle) * 6;
                    Location prongBase = new Location(w, px, center.getY() + 4, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(prongBase, Material.GOLD_BLOCK);
                    h.scale(0.4f, 6.0f, 0.4f).glow(255, 215, 0).interpolation(5, 0);
                    prongHandles.add(h);
                    spawnedEntities.add(h.entity());
                    DisplayBuilder.playSound(prongBase, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 0.7f);
                }
            }

            // Prong phase: 61-180 ticks
            int prongTick = ticksAlive - 61;
            if (prongTick >= 0 && prongTick <= 120) {
                // Stationary for first 60 ticks, rotating for next 60
                boolean rotating = prongTick > 60;
                if (rotating) {
                    crownRotation += Math.toRadians(1.125); // 22.5 degrees every 20 ticks
                }

                if (prongTick % 4 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = prongAngles[i] + (rotating ? crownRotation : 0);
                        double px = center.getX() + Math.cos(angle) * 6;
                        double pz = center.getZ() + Math.sin(angle) * 6;
                        // Prong column particles
                        for (double y = 0; y < 4; y += 1.0) {
                            Location prongPos = new Location(w, px, center.getY() + y, pz);
                            w.spawnParticle(Particle.TOTEM_OF_UNDYING, prongPos, 3, 0.3, 0.3, 0.3, 0.01);
                        }
                        // Rotation trail
                        if (rotating) {
                            DisplayBuilder.dustParticles(new Location(w, px, center.getY() + 2, pz),
                                    5, 0.5, 255, 215, 0, 0.9f);
                        }
                    }
                }

                // Damage players touching prong zones
                if (prongTick % 15 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location pl = p.getLocation();
                        for (int i = 0; i < 8; i++) {
                            double angle = prongAngles[i] + (rotating ? crownRotation : 0);
                            double px = center.getX() + Math.cos(angle) * 6;
                            double pz = center.getZ() + Math.sin(angle) * 6;
                            double distSq = (pl.getX() - px) * (pl.getX() - px) + (pl.getZ() - pz) * (pl.getZ() - pz);
                            if (distSq <= 2.25 && pl.getY() < center.getY() + 4) { // 1.5 block radius
                                p.damage(6.0); // 3 hearts
                                break;
                            }
                        }
                    }
                }
            }

            // Retract: 181-200 ticks
            if (prongTick > 120) {
                if (prongTick == 121) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.9f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TotemCrown(plugin); }
    }

    // =========================================================================
    // 10. STREAM STUTTER -- all streams burst-fire particle volleys downward
    // =========================================================================
    public static class StreamStutter extends EnvironmentalAttack {

        public StreamStutter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("stream_stutter", AttackType.ENVIRONMENTAL, 5));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(170); // ~8.5 seconds
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0); // 3 hearts per volley hit
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.3f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 0-20 ticks (1 second) -- flicker effect
            if (ticksAlive <= 20) {
                if (ticksAlive % 5 == 0) {
                    boolean surge = (ticksAlive / 5) % 2 == 1;
                    int count = surge ? 30 : 3;
                    for (int i = 0; i < 5; i++) {
                        double sx = center.getX() + (Math.random() - 0.5) * 16;
                        double sz = center.getZ() + (Math.random() - 0.5) * 16;
                        Location skyPos = new Location(w, sx, center.getY() + 35, sz);
                        w.spawnParticle(Particle.WITCH, skyPos, count, 1, 1, 1, 0);
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.3f, 2.0f);
                }
                return;
            }

            // Stutter phase: 21-170 ticks (150 ticks = 5 cycles of 30 ticks)
            int stutterTick = (ticksAlive - 21) % 30;
            int cycleNum = (ticksAlive - 21) / 30;

            // Active emission phase: 0-19 ticks in cycle (300% density)
            if (stutterTick < 20) {
                if (stutterTick % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double sx = center.getX() + (Math.random() - 0.5) * 20;
                        double sz = center.getZ() + (Math.random() - 0.5) * 20;
                        Location skyPos = new Location(w, sx, center.getY() + 30 + Math.random() * 10, sz);
                        // Alternate particle types
                        switch (i % 5) {
                            case 0 -> w.spawnParticle(Particle.WITCH, skyPos, 5, 0.5, 0.5, 0.5, 0);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, skyPos, 5, 0.5, 0.5, 0.5, 0.02);
                            case 2 -> w.spawnParticle(Particle.END_ROD, skyPos, 5, 0.5, 0.5, 0.5, 0.02);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, skyPos, 5, 0.5, 0.5, 0.5, 0.01);
                            case 4 -> DisplayBuilder.dustParticles(skyPos, 5, 0.5, 180, 0, 0, 1.2f);
                        }
                    }
                }
            }

            // Zero-emission volley: 20-29 ticks in cycle -- particles rain down
            if (stutterTick >= 20 && stutterTick <= 29) {
                if (stutterTick == 20) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 0.8f, 1.0f);
                }
                // Volley particles raining down
                for (int v = 0; v < 8; v++) {
                    double vx = center.getX() + (Math.random() - 0.5) * 20;
                    double vz = center.getZ() + (Math.random() - 0.5) * 20;
                    double vy = center.getY() + 30 - (stutterTick - 20) * 3.0;
                    if (vy <= center.getY() + 0.5) {
                        Location impact = new Location(w, vx, center.getY(), vz);
                        // Ground impact burst
                        switch (v % 5) {
                            case 0 -> w.spawnParticle(Particle.WITCH, impact, 5, 0.3, 0.1, 0.3, 0);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, impact, 5, 0.3, 0.1, 0.3, 0.02);
                            case 2 -> w.spawnParticle(Particle.END_ROD, impact, 5, 0.3, 0.1, 0.3, 0.02);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, impact, 5, 0.3, 0.1, 0.3, 0.01);
                            case 4 -> DisplayBuilder.dustParticles(impact, 5, 0.3, 180, 0, 0, 1.0f);
                        }
                        // Damage on landing
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(impact) <= 2.25) {
                                p.damage(6.0); // 3 hearts
                            }
                        }
                    } else {
                        Location volleyPos = new Location(w, vx, vy, vz);
                        w.spawnParticle(Particle.END_ROD, volleyPos, 1, 0, 0, 0, 0);
                    }
                }
            }

            if (stutterTick == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_SMALL_FALL, 0.4f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamStutter(plugin); }
    }
}
