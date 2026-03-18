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
 * Phase 4D Environmental -- GROUP 7: ARENA DEGRADATION (Phase 3 Escalation)
 * 10 attacks (#61-70) layered on top of Phase 1+2 effects as the island dies.
 * Phase 3 begins at 29% HP. Arena cracks, pillars erupt, sectors collapse.
 *
 * Design notes:
 * - No status effects
 * - Void Emperor palette: magenta (180,0,200), cyan (0,200,255), crimson (200,0,50), purple (128,0,255)
 * - Persistent hazard zones -- many attacks leave permanent damage areas
 * - Late fight -- damage range 8.0-18.0 HP
 */
public final class ArenaDegradation {

    private ArenaDegradation() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CascadingCrack(plugin));
        registry.register(new EruptionCascade(plugin));
        registry.register(new VoidFissureChain(plugin));
        registry.register(new Phase3Convergence(plugin));
        registry.register(new RapidSuccessionChain(plugin));
        registry.register(new CollapseSectorNW(plugin));
        registry.register(new CollapseSectorSE(plugin));
        registry.register(new SigilDamageWave(plugin));
        registry.register(new EclipseBlackout(plugin));
        registry.register(new FinalSigilDetonation(plugin));
    }

    // =========================================================================
    // 61. CASCADING CRACK -- arena floor fault line at Phase 3 entry
    // =========================================================================
    public static class CascadingCrack extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> crackHandles = new ArrayList<>();
        private Location northEdge;
        private Location southEdge;
        private boolean cracked = false;

        public CascadingCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cascading_crack", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(4.0); // 2 hearts per second near crack
            config.setDamageRadius(1.5);
            config.setDurationTicks(6000); // Persists ~5 minutes (rest of fight)
            config.setCooldownTicks(99999); // Scripted once
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Fault line from north to south, offset slightly from center
            double offsetX = (Math.random() - 0.5) * 8;
            northEdge = center.clone().add(offsetX, 0, -22);
            southEdge = center.clone().add(offsetX + 2, 0, 22);

            // Seismic rumble
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERRACK_BREAK, 0.4f, 0.3f);
            for (int i = 1; i <= 7; i++) {
                final int idx = i;
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        DisplayBuilder.playSound(center, Sound.BLOCK_NETHERRACK_BREAK, 0.4f, 0.3f), idx * 3L);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.3f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 0.8f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || northEdge == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Formation: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                double progress = ticksAlive / 40.0;
                int numPoints = (int) (progress * 20);
                for (int i = 0; i < numPoints; i++) {
                    double t = (double) i / 20;
                    Location crackPoint = northEdge.clone().add(
                            (southEdge.getX() - northEdge.getX()) * t,
                            0,
                            (southEdge.getZ() - northEdge.getZ()) * t
                    );
                    w.spawnParticle(Particle.BLOCK, crackPoint, 5, 0.3, 0.2, 0.3, 0,
                            Material.OBSIDIAN.createBlockData());
                }
                return;
            }

            // Crack established
            if (!cracked) {
                cracked = true;

                // Place crack displays along the fault line
                for (int i = 0; i < 15; i++) {
                    double t = (double) i / 15;
                    Location crackLoc = northEdge.clone().add(
                            (southEdge.getX() - northEdge.getX()) * t,
                            0.05,
                            (southEdge.getZ() - northEdge.getZ()) * t
                    );
                    BlockDisplayHandle h = displayBuilder.spawnBlock(crackLoc, Material.OBSIDIAN);
                    h.scale(1.0f, 0.1f, 3.0f).glow(128, 0, 255).interpolation(5, 0);
                    crackHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Persistent DRAGON_BREATH rising from crack
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 8; i++) {
                    double t = Math.random();
                    Location crackPoint = northEdge.clone().add(
                            (southEdge.getX() - northEdge.getX()) * t,
                            0.3,
                            (southEdge.getZ() - northEdge.getZ()) * t
                    );
                    w.spawnParticle(Particle.DRAGON_BREATH, crackPoint, 4, 0.2, 2, 0.2, 0.01);
                }
            }

            // Damage players near crack (handled by base class via damageRadius,
            // but we also do manual line-based checks)
            if (ticksAlive % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    // Check distance to crack line
                    Location pl = p.getLocation();
                    double distToLine = distanceToLineSegment(pl, northEdge, southEdge);
                    if (distToLine <= 1.5) {
                        p.damage(4.0); // 2 hearts per second
                    }
                }
            }
        }

        private double distanceToLineSegment(Location point, Location lineStart, Location lineEnd) {
            double dx = lineEnd.getX() - lineStart.getX();
            double dz = lineEnd.getZ() - lineStart.getZ();
            double lenSq = dx * dx + dz * dz;
            if (lenSq == 0) return point.distance(lineStart);
            double t = Math.max(0, Math.min(1, ((point.getX() - lineStart.getX()) * dx + (point.getZ() - lineStart.getZ()) * dz) / lenSq));
            double projX = lineStart.getX() + t * dx;
            double projZ = lineStart.getZ() + t * dz;
            double pdx = point.getX() - projX;
            double pdz = point.getZ() - projZ;
            return Math.sqrt(pdx * pdx + pdz * pdz);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CascadingCrack(plugin); }
    }

    // =========================================================================
    // 62. ERUPTION CASCADE -- all crystal pillars erupt simultaneously
    // =========================================================================
    public static class EruptionCascade extends EnvironmentalAttack {

        private final Location[] pillarPositions = new Location[6];
        private boolean erupted = false;
        private int eruptStartTick = -1;

        public EruptionCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eruption_cascade", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(99999); // Scripted once
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Six pillars at evenly spaced positions around arena
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                pillarPositions[i] = center.clone().add(Math.cos(angle) * 15, 0, Math.sin(angle) * 15);
            }

            // Warning: all pillars vent
            for (Location pillar : pillarPositions) {
                if (pillar == null) continue;
                w.spawnParticle(Particle.DRAGON_BREATH, pillar.clone().add(0, 5, 0), 20, 0.5, 2, 0.5, 0.03);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 20 ticks (1 second)
            if (ticksAlive <= 20) {
                if (ticksAlive % 5 == 0) {
                    for (Location pillar : pillarPositions) {
                        if (pillar == null) continue;
                        w.spawnParticle(Particle.DRAGON_BREATH, pillar.clone().add(0, 6, 0), 15, 0.5, 3, 0.5, 0.02);
                    }
                }
                return;
            }

            // Eruption: stagger 2 ticks apart for each pillar
            if (!erupted) {
                erupted = true;
                eruptStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);

                for (int i = 0; i < 6; i++) {
                    if (pillarPositions[i] == null) continue;
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        DisplayBuilder.playSound(pillarPositions[idx], Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.7f);
                        w.spawnParticle(Particle.DRAGON_BREATH, pillarPositions[idx].clone().add(0, 3, 0), 60, 1, 3, 1, 0.05);
                    }, idx * 2L);
                }
            }

            // Ground clouds: 4-block radius around each pillar, 6 dmg/sec, 4 seconds
            if (eruptStartTick > 0 && ticksAlive - eruptStartTick <= 80) {
                // Cloud particles
                if ((ticksAlive - eruptStartTick) % 4 == 0) {
                    for (Location pillar : pillarPositions) {
                        if (pillar == null) continue;
                        w.spawnParticle(Particle.DRAGON_BREATH, pillar.clone().add(0, 0.5, 0), 15, 2, 0.5, 2, 0.01);
                    }
                }

                // Damage in each cloud
                if ((ticksAlive - eruptStartTick) % 20 == 0) {
                    for (Location pillar : pillarPositions) {
                        if (pillar == null) continue;
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(pillar) <= 16.0) { // 4 block radius
                                p.damage(12.0); // 6 hearts per second
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EruptionCascade(plugin); }
    }

    // =========================================================================
    // 63. VOID FISSURE CHAIN -- three fissure points forming a triangle
    // =========================================================================
    public static class VoidFissureChain extends EnvironmentalAttack {

        private final Location[] fissurePoints = new Location[3];
        private boolean opened = false;
        private int openTick = -1;

        public VoidFissureChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_fissure_chain", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(140); // 7 seconds
            config.setCooldownTicks(600); // 30 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Three points forming a triangle
            double baseAngle = Math.random() * 2 * Math.PI;
            for (int i = 0; i < 3; i++) {
                double angle = baseAngle + (2 * Math.PI * i) / 3;
                double dist = 8 + Math.random() * 5;
                fissurePoints[i] = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks (2 seconds) -- staggered pulsing
            if (ticksAlive <= 40) {
                for (int i = 0; i < 3; i++) {
                    if (fissurePoints[i] == null) continue;
                    int startTick = i * 10; // Staggered start
                    if (ticksAlive >= startTick && ticksAlive % 5 == 0) {
                        w.spawnParticle(Particle.DRAGON_BREATH, fissurePoints[i], 10, 0.5, 0.5, 0.5, 0.02);
                    }
                }
                return;
            }

            // Open fissures
            if (!opened) {
                opened = true;
                openTick = ticksAlive;

                for (int i = 0; i < 3; i++) {
                    if (fissurePoints[i] == null) continue;
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        DisplayBuilder.playSound(fissurePoints[idx], Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.6f, 0.6f);
                        w.spawnParticle(Particle.DRAGON_BREATH, fissurePoints[idx], 60, 1.5, 1, 1.5, 0.03);
                    }, idx * 6L);
                }

                DisplayBuilder.playSound(center, Sound.BLOCK_NETHERRACK_BREAK, 0.5f, 0.4f);
            }

            // Active fissures: 10 entry damage + 5/sec for 2 seconds (40 ticks)
            if (openTick > 0 && ticksAlive - openTick <= 40) {
                // Fissure particles
                if ((ticksAlive - openTick) % 4 == 0) {
                    for (Location fissure : fissurePoints) {
                        if (fissure == null) continue;
                        w.spawnParticle(Particle.DRAGON_BREATH, fissure.clone().add(0, 0.5, 0), 8, 1.5, 0.5, 1.5, 0.01);
                        w.spawnParticle(Particle.SMOKE, fissure.clone().add(0, 1, 0), 4, 1.5, 0.3, 1.5, 0.02);
                    }
                }

                // Damage: 10 per second in fissures
                if ((ticksAlive - openTick) % 20 == 0) {
                    for (Location fissure : fissurePoints) {
                        if (fissure == null) continue;
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(fissure) <= 9.0) { // 3 block radius
                                p.damage(10.0); // 5 hearts
                            }
                        }
                    }
                }
            }

            // Aftermath: smoke for 10 seconds
            if (openTick > 0 && ticksAlive - openTick > 40 && ticksAlive - openTick <= 240) {
                if (ticksAlive % 15 == 0) {
                    for (Location fissure : fissurePoints) {
                        if (fissure == null) continue;
                        w.spawnParticle(Particle.DRAGON_BREATH, fissure.clone().add(0, 0.3, 0), 5, 1, 0.3, 1, 0.01);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidFissureChain(plugin); }
    }

    // =========================================================================
    // 64. PHASE 3 CONVERGENCE -- scaled version of Total Sky Convergence
    // =========================================================================
    public static class Phase3Convergence extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> voidHandles = new ArrayList<>();
        private boolean impacted = false;
        private int fogStartTick = -1;

        public Phase3Convergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase3_convergence", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(500); // 25 seconds
            config.setCooldownTicks(99999); // Scripted once
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Title: "The Emperor wills it closed."
            for (Player p : w.getPlayers()) {
                if (p.getLocation().distanceSquared(center) <= 10000) {
                    p.sendTitle(" ", "\u00a75The Emperor wills it closed.", 10, 50, 20);
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks (4 seconds) -- double Phase 3 density streams
            if (ticksAlive <= 80) {
                if (ticksAlive % 3 == 0) {
                    double progress = ticksAlive / 80.0;
                    for (int s = 0; s < 12; s++) {
                        double sa = (2 * Math.PI * s) / 12;
                        double skyR = 30 * (1 - progress);
                        double skyY = center.getY() + 40 * (1 - progress);
                        Location streamPos = center.clone().add(Math.cos(sa) * skyR, skyY - center.getY(), Math.sin(sa) * skyR);

                        switch (s % 4) {
                            case 0 -> DisplayBuilder.dustParticles(streamPos, 10, 0.5, 180, 0, 200, 2.0f);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamPos, 8, 0.3, 0.3, 0.3, 0.03);
                            case 2 -> w.spawnParticle(Particle.END_ROD, streamPos, 8, 0.3, 0.3, 0.3, 0.02);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, streamPos, 8, 0.3, 0.3, 0.3, 0.02);
                        }
                    }
                }
                return;
            }

            // Impact at tick 81
            if (!impacted) {
                impacted = true;
                fogStartTick = ticksAlive;

                // Scaled Phase 3 damage: 50/30/15
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist <= 6) p.damage(50.0);
                    else if (dist <= 12) p.damage(30.0);
                    else if (dist <= 20) p.damage(15.0);
                }

                // Sound cluster at 1.3x volume
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.4f, 1.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 1.3f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.7f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 0.4f, 0.5f);

                // Triple CRIT explosions staggered
                w.spawnParticle(Particle.CRIT, center, 150, 3, 3, 3, 0.3);
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        w.spawnParticle(Particle.CRIT, center, 150, 4, 4, 4, 0.3), 10L);
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        w.spawnParticle(Particle.CRIT, center, 150, 5, 5, 5, 0.3), 20L);

                // All particle types at double density
                w.spawnParticle(Particle.WITCH, center, 300, 5, 5, 5, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 200, 5, 5, 5, 0.1);
                w.spawnParticle(Particle.END_ROD, center, 200, 5, 5, 5, 0.05);
                w.spawnParticle(Particle.DRAGON_BREATH, center, 400, 5, 5, 5, 0.05);

                // Void zone displays
                for (int i = 0; i < 8; i++) {
                    double a = (2 * Math.PI * i) / 8;
                    Location vLoc = center.clone().add(Math.cos(a) * 2.5, 0.1, Math.sin(a) * 2.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(vLoc, Material.OBSIDIAN);
                    h.scale(2.0f, 0.3f, 2.0f).glow(128, 0, 255).interpolation(5, 0);
                    voidHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Void zone: 12 dmg/sec for 12 seconds, 5-block radius
            if (fogStartTick > 0 && ticksAlive - fogStartTick <= 240) {
                if ((ticksAlive - fogStartTick) % 4 == 0) {
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 1, 0), 30, 2.5, 2, 2.5, 0.01);
                }
                if ((ticksAlive - fogStartTick) % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 25.0) {
                            p.damage(24.0); // 12 hearts per second
                        }
                    }
                }
            }

            // Sigil shard particles after impact
            if (fogStartTick > 0 && ticksAlive - fogStartTick <= 600 && ticksAlive % 20 == 0) {
                w.spawnParticle(Particle.BLOCK, center, 15, 3, 0.3, 3, 0,
                        Material.OBSIDIAN.createBlockData());
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Phase3Convergence(plugin); }
    }

    // =========================================================================
    // 65. RAPID SUCCESSION CHAIN -- 6 random attacks in 8 seconds
    // =========================================================================
    public static class RapidSuccessionChain extends EnvironmentalAttack {

        private final Location[] burstPoints = new Location[6];
        private final boolean[] fired = new boolean[6];

        public RapidSuccessionChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rapid_succession_chain", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Six random burst points across the arena
            for (int i = 0; i < 6; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 3 + Math.random() * 15;
                burstPoints[i] = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                fired[i] = false;
            }

            // Brief warning
            DisplayBuilder.dustParticles(center, 15, 4.0, 180, 0, 200, 1.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // 10 tick warning, then 6 events at intervals of ~27 ticks across 160 ticks
            for (int i = 0; i < 6; i++) {
                if (burstPoints[i] == null) continue;
                int eventTick = 10 + i * 27;

                // Pre-warning
                if (ticksAlive >= eventTick - 10 && ticksAlive < eventTick && !fired[i]) {
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.dustParticles(burstPoints[i], 8, 1.0, 200, 0, 50, 1.0f);
                    }
                }

                if (ticksAlive >= eventTick && !fired[i]) {
                    fired[i] = true;

                    // Varied burst types simulating different attacks
                    switch (i % 3) {
                        case 0 -> {
                            // Eruption-style
                            w.spawnParticle(Particle.DRAGON_BREATH, burstPoints[i], 50, 2, 3, 2, 0.03);
                            w.spawnParticle(Particle.LAVA, burstPoints[i].clone().add(0, 1, 0), 15, 1, 2, 1, 0);
                            DisplayBuilder.playSound(burstPoints[i], Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.5f, 0.6f);
                        }
                        case 1 -> {
                            // Void zone style
                            w.spawnParticle(Particle.DRAGON_BREATH, burstPoints[i], 40, 1.5, 1, 1.5, 0.02);
                            w.spawnParticle(Particle.SMOKE, burstPoints[i].clone().add(0, 1, 0), 20, 1, 1, 1, 0.03);
                            DisplayBuilder.playSound(burstPoints[i], Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.4f, 0.5f);
                        }
                        case 2 -> {
                            // Convergence style
                            w.spawnParticle(Particle.WITCH, burstPoints[i], 40, 2, 2, 2, 0);
                            w.spawnParticle(Particle.CRIT, burstPoints[i], 25, 1.5, 1.5, 1.5, 0.2);
                            DisplayBuilder.playSound(burstPoints[i], Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.6f);
                        }
                    }

                    // Damage: compressed, 50% of normal range (half buildup theme)
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(burstPoints[i]) <= 12.25) { // 3.5 radius
                            p.damage(12.0); // 6 hearts per burst
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RapidSuccessionChain(plugin); }
    }

    // =========================================================================
    // 66. COLLAPSE SECTOR: NORTHWEST -- persistent hazard zone
    // =========================================================================
    public static class CollapseSectorNW extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> rubbleHandles = new ArrayList<>();
        private Location sectorCenter;
        private boolean collapsed = false;

        public CollapseSectorNW(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("collapse_sector_nw", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(6.0); // 3 hearts per second
            config.setDamageRadius(10.0);
            config.setDurationTicks(6000); // Persists for fight
            config.setCooldownTicks(99999); // Scripted once at 20% HP
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Northwest sector center
            sectorCenter = center.clone().add(-10, 0, -10);

            // Warning: cracking particles across sector
            for (int i = 0; i < 30; i++) {
                Location crackLoc = sectorCenter.clone().add(
                        (Math.random() - 0.5) * 15, 0.2, (Math.random() - 0.5) * 15);
                w.spawnParticle(Particle.BLOCK, crackLoc, 5, 0.3, 0.2, 0.3, 0,
                        Material.STONE.createBlockData());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || sectorCenter == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks (3 seconds)
            if (ticksAlive <= 60) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 10; i++) {
                        Location crackLoc = sectorCenter.clone().add(
                                (Math.random() - 0.5) * 15, 0.2, (Math.random() - 0.5) * 15);
                        w.spawnParticle(Particle.BLOCK, crackLoc, 3, 0.2, 0.1, 0.2, 0,
                                Material.STONE.createBlockData());
                    }
                    // Gold structure sparking
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, sectorCenter.clone().add(0, 3, 0), 10, 3, 2, 3, 0.05);
                    w.spawnParticle(Particle.CRIT, sectorCenter.clone().add(0, 4, 0), 8, 2, 1, 2, 0.1);
                }
                return;
            }

            // Collapse
            if (!collapsed) {
                collapsed = true;

                // Collapse sounds
                for (int i = 0; i < 5; i++) {
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            DisplayBuilder.playSound(sectorCenter, Sound.BLOCK_NETHERRACK_BREAK, 0.8f, 0.4f), idx * 3L);
                }
                DisplayBuilder.playSound(sectorCenter, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 0.8f, 0.3f);
                DisplayBuilder.playSound(sectorCenter, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.4f);

                // Debris block displays
                w.spawnParticle(Particle.BLOCK, sectorCenter, 200, 7, 1, 7, 0,
                        Material.STONE.createBlockData());

                for (int i = 0; i < 8; i++) {
                    Location rubbleLoc = sectorCenter.clone().add(
                            (Math.random() - 0.5) * 12, 0.05, (Math.random() - 0.5) * 12);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(rubbleLoc, Material.DEEPSLATE);
                    h.scale(1.5f, 0.15f, 1.5f).glow(128, 0, 255).interpolation(5, 0);
                    rubbleHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Persistent hazard: DRAGON_BREATH ground particles
            if (collapsed && ticksAlive % 6 == 0) {
                for (int i = 0; i < 6; i++) {
                    Location hazardLoc = sectorCenter.clone().add(
                            (Math.random() - 0.5) * 15, 0.3, (Math.random() - 0.5) * 15);
                    w.spawnParticle(Particle.DRAGON_BREATH, hazardLoc, 4, 0.5, 0.3, 0.5, 0.01);
                }
            }

            // Persistent damage handled by base class (damageRadius set)
            // But we also do a sector-check for square zone
            if (collapsed && ticksAlive % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = Math.abs(p.getLocation().getX() - sectorCenter.getX());
                    double dz = Math.abs(p.getLocation().getZ() - sectorCenter.getZ());
                    if (dx <= 7.5 && dz <= 7.5) {
                        p.damage(6.0); // 3 hearts per second
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CollapseSectorNW(plugin); }
    }

    // =========================================================================
    // 67. COLLAPSE SECTOR: SOUTHEAST -- identical to NW but SE quadrant
    // =========================================================================
    public static class CollapseSectorSE extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> rubbleHandles = new ArrayList<>();
        private Location sectorCenter;
        private boolean collapsed = false;

        public CollapseSectorSE(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("collapse_sector_se", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(6.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(99999); // Scripted at 15% HP
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Southeast sector center
            sectorCenter = center.clone().add(10, 0, 10);

            for (int i = 0; i < 30; i++) {
                Location crackLoc = sectorCenter.clone().add(
                        (Math.random() - 0.5) * 15, 0.2, (Math.random() - 0.5) * 15);
                w.spawnParticle(Particle.BLOCK, crackLoc, 5, 0.3, 0.2, 0.3, 0,
                        Material.STONE.createBlockData());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || sectorCenter == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 60 ticks (3 seconds)
            if (ticksAlive <= 60) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 10; i++) {
                        Location crackLoc = sectorCenter.clone().add(
                                (Math.random() - 0.5) * 15, 0.2, (Math.random() - 0.5) * 15);
                        w.spawnParticle(Particle.BLOCK, crackLoc, 3, 0.2, 0.1, 0.2, 0,
                                Material.STONE.createBlockData());
                    }
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, sectorCenter.clone().add(0, 3, 0), 10, 3, 2, 3, 0.05);
                    w.spawnParticle(Particle.CRIT, sectorCenter.clone().add(0, 4, 0), 8, 2, 1, 2, 0.1);
                }
                return;
            }

            // Collapse
            if (!collapsed) {
                collapsed = true;

                for (int i = 0; i < 5; i++) {
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                            DisplayBuilder.playSound(sectorCenter, Sound.BLOCK_NETHERRACK_BREAK, 0.8f, 0.4f), idx * 3L);
                }
                DisplayBuilder.playSound(sectorCenter, Sound.BLOCK_ANCIENT_DEBRIS_BREAK, 0.8f, 0.3f);
                DisplayBuilder.playSound(sectorCenter, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 0.4f);

                w.spawnParticle(Particle.BLOCK, sectorCenter, 200, 7, 1, 7, 0,
                        Material.STONE.createBlockData());

                // SE-specific: amethyst cluster shatter
                w.spawnParticle(Particle.BLOCK, sectorCenter.clone().add(3, 2, 3), 60, 2, 2, 2, 0,
                        Material.AMETHYST_BLOCK.createBlockData());
                w.spawnParticle(Particle.DRAGON_BREATH, sectorCenter.clone().add(3, 2, 3), 30, 1, 1, 1, 0.03);

                for (int i = 0; i < 8; i++) {
                    Location rubbleLoc = sectorCenter.clone().add(
                            (Math.random() - 0.5) * 12, 0.05, (Math.random() - 0.5) * 12);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(rubbleLoc, Material.DEEPSLATE);
                    h.scale(1.5f, 0.15f, 1.5f).glow(128, 0, 255).interpolation(5, 0);
                    rubbleHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Persistent hazard
            if (collapsed && ticksAlive % 6 == 0) {
                for (int i = 0; i < 6; i++) {
                    Location hazardLoc = sectorCenter.clone().add(
                            (Math.random() - 0.5) * 15, 0.3, (Math.random() - 0.5) * 15);
                    w.spawnParticle(Particle.DRAGON_BREATH, hazardLoc, 4, 0.5, 0.3, 0.5, 0.01);
                }
            }

            if (collapsed && ticksAlive % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dx = Math.abs(p.getLocation().getX() - sectorCenter.getX());
                    double dz = Math.abs(p.getLocation().getZ() - sectorCenter.getZ());
                    if (dx <= 7.5 && dz <= 7.5) {
                        p.damage(6.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CollapseSectorSE(plugin); }
    }

    // =========================================================================
    // 68. SIGIL DAMAGE WAVE -- radial shockwave from cracked sigil
    // =========================================================================
    public static class SigilDamageWave extends EnvironmentalAttack {

        private boolean waveStarted = false;

        public SigilDamageWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sigil_damage_wave", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(400); // 20 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Sigil glow warning
            DisplayBuilder.dustParticles(center, 20, 2.0, 180, 0, 200, 1.5f);
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 0.5, 0), 15, 1.5, 0.3, 1.5, 0.03);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 30 ticks (1.5 seconds)
            if (ticksAlive <= 30) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center, 15, 1.5, 180, 0, 200, 1.2f);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 0.3, 0), 10, 1, 0.2, 1, 0.02);
                }
                return;
            }

            // Wave emission
            if (!waveStarted) {
                waveStarted = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
            }

            // Expanding ring: 10 blocks/second = 0.5 blocks/tick
            int waveTick = ticksAlive - 30;
            double ringRadius = waveTick * 0.5;

            if (ringRadius <= 25 && waveTick % 2 == 0) {
                // Particle ring
                int ringPoints = Math.max(12, (int) (ringRadius * 3));
                for (int i = 0; i < ringPoints; i++) {
                    double a = (2 * Math.PI * i) / ringPoints;
                    Location ringLoc = center.clone().add(Math.cos(a) * ringRadius, 0.5, Math.sin(a) * ringRadius);
                    DisplayBuilder.dustParticles(ringLoc, 2, 0.2, 180, 0, 200, 1.0f);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, ringLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }

                // Damage players hit by ring (ground level, can jump over)
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    double dist = Math.sqrt(Math.pow(pl.getX() - center.getX(), 2) + Math.pow(pl.getZ() - center.getZ(), 2));
                    // Within ring band (1.5 block width) and at ground level
                    if (Math.abs(dist - ringRadius) <= 1.5 && pl.getY() < center.getY() + 2.0) {
                        p.damage(18.0); // 9 hearts
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SigilDamageWave(plugin); }
    }

    // =========================================================================
    // 69. ECLIPSE BLACKOUT -- all streams go dark, massive center eruption
    // =========================================================================
    public static class EclipseBlackout extends EnvironmentalAttack {

        private boolean erupted = false;

        public EclipseBlackout(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eclipse_blackout", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(99999); // Scripted once at 10% HP
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Streams begin dimming -- simulated by silence
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.2f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Blackout: 40 ticks (2 seconds) dimming
            if (ticksAlive <= 40) {
                // Progressively fewer particles (simulating streams going dark)
                if (ticksAlive % 10 == 0) {
                    int density = Math.max(1, 10 - ticksAlive / 4);
                    for (int i = 0; i < density; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = 10 + Math.random() * 15;
                        double skyY = center.getY() + 20 + Math.random() * 10;
                        Location skyPos = center.clone().add(Math.cos(angle) * dist, skyY - center.getY(), Math.sin(angle) * dist);
                        w.spawnParticle(Particle.SMOKE, skyPos, 3, 0.5, 0.5, 0.5, 0.01);
                    }
                }
                return;
            }

            // 3-second silence/darkness: ticks 41-100

            // Eruption at tick 80 (midpoint of darkness)
            if (ticksAlive >= 80 && !erupted) {
                erupted = true;

                // Massive DRAGON_BREATH eruption at center
                w.spawnParticle(Particle.DRAGON_BREATH, center, 300, 5, 5, 5, 0.05);

                // Damage all within 8 blocks: 20 damage (10 hearts)
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 64.0) {
                        p.damage(20.0);
                    }
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.3f);
            }

            // Streams return at max density after tick 100
            if (ticksAlive > 100 && ticksAlive <= 140 && ticksAlive % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = 5 + Math.random() * 20;
                    double skyY = center.getY() + 15 + Math.random() * 20;
                    Location skyPos = center.clone().add(Math.cos(angle) * dist, skyY - center.getY(), Math.sin(angle) * dist);

                    switch (i % 4) {
                        case 0 -> DisplayBuilder.dustParticles(skyPos, 8, 0.5, 180, 0, 200, 2.0f);
                        case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, skyPos, 6, 0.5, 0.5, 0.5, 0.03);
                        case 2 -> w.spawnParticle(Particle.END_ROD, skyPos, 6, 0.5, 0.5, 0.5, 0.02);
                        case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, skyPos, 6, 0.5, 0.5, 0.5, 0.02);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EclipseBlackout(plugin); }
    }

    // =========================================================================
    // 70. FINAL SIGIL DETONATION -- biggest non-death explosion at 5% HP
    // =========================================================================
    public static class FinalSigilDetonation extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> voidHandles = new ArrayList<>();
        private boolean detonated = false;
        private int fogStartTick = -1;

        public FinalSigilDetonation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("final_sigil_detonation", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(500); // 25 seconds
            config.setCooldownTicks(99999); // Scripted once at 5% HP
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Title: "THE VOID EMPEROR FALLS."
            for (Player p : w.getPlayers()) {
                if (p.getLocation().distanceSquared(center) <= 10000) {
                    p.sendTitle(" ", "\u00a75THE VOID EMPEROR FALLS.", 10, 60, 20);
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks (4 seconds)
            if (ticksAlive <= 80) {
                if (ticksAlive % 4 == 0) {
                    // Every crack lights up
                    DisplayBuilder.dustParticles(center, 25, 4.0, 180, 0, 200, 2.0f);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, center.clone().add(0, 0.5, 0), 15, 3, 0.3, 3, 0.03);

                    // All streams converge
                    double progress = ticksAlive / 80.0;
                    for (int s = 0; s < 10; s++) {
                        double sa = (2 * Math.PI * s) / 10;
                        double skyR = 25 * (1 - progress);
                        double skyY = center.getY() + 30 * (1 - progress);
                        Location streamPos = center.clone().add(Math.cos(sa) * skyR, skyY - center.getY(), Math.sin(sa) * skyR);
                        switch (s % 4) {
                            case 0 -> DisplayBuilder.dustParticles(streamPos, 6, 0.5, 180, 0, 200, 1.5f);
                            case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamPos, 5, 0.3, 0.3, 0.3, 0.02);
                            case 2 -> w.spawnParticle(Particle.END_ROD, streamPos, 5, 0.3, 0.3, 0.3, 0.01);
                            case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, streamPos, 5, 0.3, 0.3, 0.3, 0.01);
                        }
                    }
                }
                return;
            }

            // Detonation at tick 81
            if (!detonated) {
                detonated = true;
                fogStartTick = ticksAlive;

                // Tiered damage: 60/35/20
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist <= 5) p.damage(60.0);       // 30 hearts
                    else if (dist <= 10) p.damage(35.0);  // 17.5 hearts
                    else if (dist <= 20) p.damage(20.0);  // 10 hearts
                }

                // Full sound barrage
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.6f, 1.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.4f);
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.4f), 5L);
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.4f), 10L);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.3f);

                // MAXIMUM particle burst -- all types
                w.spawnParticle(Particle.WITCH, center, 200, 5, 5, 5, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 150, 5, 5, 5, 0.1);
                w.spawnParticle(Particle.END_ROD, center, 150, 5, 5, 5, 0.05);
                w.spawnParticle(Particle.DRAGON_BREATH, center, 250, 5, 5, 5, 0.05);
                w.spawnParticle(Particle.CRIT, center, 100, 4, 4, 4, 0.3);

                // Void zone displays
                for (int i = 0; i < 10; i++) {
                    double a = (2 * Math.PI * i) / 10;
                    Location vLoc = center.clone().add(Math.cos(a) * 2.5, 0.1, Math.sin(a) * 2.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(vLoc, Material.OBSIDIAN);
                    h.scale(2.0f, 0.4f, 2.0f).glow(128, 0, 255).interpolation(5, 0);
                    voidHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Stream midpoint flashes
                for (int i = 0; i < 8; i++) {
                    double a = (2 * Math.PI * i) / 8;
                    Location flashLoc = center.clone().add(Math.cos(a) * 18, 20, Math.sin(a) * 18);
                    w.spawnParticle(Particle.FLASH, flashLoc, 1, 0, 0, 0, 0);
                }
            }

            // Void zone: 15 dmg/sec for 8 seconds, 5-block radius
            if (fogStartTick > 0 && ticksAlive - fogStartTick <= 160) {
                if ((ticksAlive - fogStartTick) % 3 == 0) {
                    w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 1, 0), 30, 2.5, 2, 2.5, 0.01);
                }
                if ((ticksAlive - fogStartTick) % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 25.0) {
                            p.damage(30.0); // 15 hearts per second
                        }
                    }
                }
            }

            // Persistent rubble particles after void zone fades
            if (fogStartTick > 0 && ticksAlive - fogStartTick > 160 && ticksAlive % 15 == 0) {
                w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 0.3, 0), 10, 3, 0.3, 3, 0.01);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FinalSigilDetonation(plugin); }
    }
}
