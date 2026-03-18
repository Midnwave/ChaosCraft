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
 * Phase 4D Environmental -- GROUP 10: FINAL FRACTURE EVENTS (Death Sequence)
 * 10 attacks (#91-100) forming the scripted death sequence when the Void Emperor
 * reaches 0 HP. These run in exact order, timed to the death beam.
 *
 * Design notes:
 * - No status effects
 * - Void Emperor palette: magenta (180,0,200), cyan (0,200,255), crimson (200,0,50), purple (128,0,255)
 * - Scripted sequence -- no random triggers, exact timing
 * - Events 93-100 map to the death animation from Dragon HP=0 to gem deposit
 * - Most attacks deal 0 damage (visual spectacle), some have residual damage
 */
public final class FinalFracture {

    private FinalFracture() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PillarCollapseWave(plugin));
        registry.register(new PreDeathChaos(plugin));
        registry.register(new DeathStreamLock(plugin));
        registry.register(new DeathGroundResonance(plugin));
        registry.register(new DeathCrystalFlash(plugin));
        registry.register(new DeathStreamConvergence(plugin));
        registry.register(new DeathBeamLayered(plugin));
        registry.register(new DeathFloorSeal(plugin));
        registry.register(new DeathStreamDissipation(plugin));
        registry.register(new DragonEggReactivation(plugin));
    }

    // =========================================================================
    // 91. (Doc #71) PILLAR COLLAPSE WAVE -- pillars collapse in rotating sequence at 8% HP
    // =========================================================================
    public static class PillarCollapseWave extends EnvironmentalAttack {

        private final Location[] pillarPositions = new Location[6];
        private boolean started = false;

        public PillarCollapseWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pillar_collapse_wave", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(240); // 12 seconds
            config.setCooldownTicks(99999); // Scripted at 8% HP
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Six pillars around arena perimeter
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                pillarPositions[i] = center.clone().add(Math.cos(angle) * 15, 0, Math.sin(angle) * 15);
            }

            // Warning: all pillars sparking
            for (Location pillar : pillarPositions) {
                if (pillar == null) continue;
                w.spawnParticle(Particle.END_ROD, pillar.clone().add(0, 3, 0), 15, 0.5, 2, 0.5, 0.03);
                w.spawnParticle(Particle.DRAGON_BREATH, pillar.clone().add(0, 5, 0), 10, 0.5, 1, 0.5, 0.02);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 50 ticks (2.5 seconds)
            if (ticksAlive <= 50) {
                if (ticksAlive % 8 == 0) {
                    for (Location pillar : pillarPositions) {
                        if (pillar == null) continue;
                        w.spawnParticle(Particle.END_ROD, pillar.clone().add(0, 4, 0), 10, 0.5, 2, 0.5, 0.02);
                        w.spawnParticle(Particle.DRAGON_BREATH, pillar.clone().add(0, 5, 0), 8, 0.5, 1, 0.5, 0.02);
                    }
                }
                return;
            }

            // Sequential collapse: one pillar every 10 ticks (0.5 seconds)
            if (!started) {
                started = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.4f);
            }

            int collapseTick = ticksAlive - 50;
            for (int i = 0; i < 6; i++) {
                if (pillarPositions[i] == null) continue;
                int collapseStart = i * 10;

                if (collapseTick == collapseStart) {
                    final int idx = i;

                    // Pillar collapse
                    DisplayBuilder.playSound(pillarPositions[idx], Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.7f);

                    w.spawnParticle(Particle.BLOCK, pillarPositions[idx], 80, 1, 3, 1, 0,
                            Material.AMETHYST_BLOCK.createBlockData());
                    w.spawnParticle(Particle.DRAGON_BREATH, pillarPositions[idx], 60, 2, 2, 2, 0.03);
                }

                // Ground cloud after collapse: 4 dmg/sec for 5 seconds (100 ticks)
                if (collapseTick > collapseStart && collapseTick <= collapseStart + 100) {
                    if ((collapseTick - collapseStart) % 6 == 0) {
                        w.spawnParticle(Particle.DRAGON_BREATH, pillarPositions[i].clone().add(0, 0.5, 0),
                                8, 2, 0.5, 2, 0.01);
                    }

                    if ((collapseTick - collapseStart) % 20 == 0) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(pillarPositions[i]) <= 16.0) {
                                p.damage(8.0); // 4 hearts per second
                            }
                        }
                    }
                }
            }

            // Aftermath: light smoke at all pillar bases
            if (collapseTick > 60 && ticksAlive % 15 == 0) {
                for (Location pillar : pillarPositions) {
                    if (pillar == null) continue;
                    w.spawnParticle(Particle.DRAGON_BREATH, pillar.clone().add(0, 0.3, 0), 3, 1.5, 0.3, 1.5, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PillarCollapseWave(plugin); }
    }

    // =========================================================================
    // 92. (Doc #72) PRE-DEATH CHAOS -- random attacks every 3 seconds at 3% HP
    // =========================================================================
    public static class PreDeathChaos extends EnvironmentalAttack {

        public PreDeathChaos(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pre_death_chaos", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(2400); // 2 minutes max (until Dragon dies)
            config.setCooldownTicks(99999); // Scripted at 3% HP
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // Immediate -- no warning
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Every 60 ticks (3 seconds), fire a random burst at a random point
            if (ticksAlive % 60 == 0) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 3 + Math.random() * 18;
                Location burstPoint = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                // Short warning (half buildup)
                DisplayBuilder.dustParticles(burstPoint, 10, 1.0, 200, 0, 50, 1.0f);

                // Delayed impact
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // Random burst type
                    int type = (int) (Math.random() * 4);
                    switch (type) {
                        case 0 -> {
                            w.spawnParticle(Particle.DRAGON_BREATH, burstPoint, 50, 2, 2, 2, 0.03);
                            DisplayBuilder.playSound(burstPoint, Sound.ENTITY_ENDER_DRAGON_SHOOT, 0.4f, 0.5f);
                        }
                        case 1 -> {
                            w.spawnParticle(Particle.FLAME, burstPoint, 40, 2, 2, 2, 0.05);
                            w.spawnParticle(Particle.LAVA, burstPoint, 15, 1, 1, 1, 0);
                            DisplayBuilder.playSound(burstPoint, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 0.6f);
                        }
                        case 2 -> {
                            w.spawnParticle(Particle.WITCH, burstPoint, 40, 2, 2, 2, 0);
                            w.spawnParticle(Particle.END_ROD, burstPoint, 20, 1.5, 3, 1.5, 0.05);
                            DisplayBuilder.playSound(burstPoint, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.4f, 0.7f);
                        }
                        case 3 -> {
                            w.spawnParticle(Particle.BLOCK, burstPoint, 40, 2, 2, 2, 0,
                                    Material.STONE.createBlockData());
                            w.spawnParticle(Particle.SMOKE, burstPoint.clone().add(0, 1, 0), 20, 1, 2, 1, 0.03);
                            DisplayBuilder.playSound(burstPoint, Sound.BLOCK_NETHERRACK_BREAK, 0.5f, 0.4f);
                        }
                    }

                    // 50% reduced damage: ~6-10 per hit
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(burstPoint) <= 12.25) { // 3.5 radius
                            p.damage(10.0); // ~50% of standard attack damage
                        }
                    }
                }, 15L); // Compressed half-buildup
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PreDeathChaos(plugin); }
    }

    // =========================================================================
    // 93. (Doc #93) DEATH STREAM LOCK -- all streams triple at death threshold
    // =========================================================================
    public static class DeathStreamLock extends EnvironmentalAttack {

        public DeathStreamLock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("death_stream_lock", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(60); // 3 seconds (0-2s of death sequence + overlap)
            config.setCooldownTicks(99999); // Scripted at Dragon death
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Dragon death sound begins
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // All streams at triple Phase 3 density -- sky is a solid sheet of light
            if (ticksAlive % 2 == 0) {
                for (int burst = 0; burst < 8; burst++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 25;
                    double skyY = center.getY() + 15 + Math.random() * 25;
                    Location skyPos = center.clone().add(Math.cos(angle) * dist, skyY - center.getY(), Math.sin(angle) * dist);

                    switch (burst % 4) {
                        case 0 -> DisplayBuilder.dustParticles(skyPos, 8, 0.8, 180, 0, 200, 2.0f);
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
        public AbstractAttack newInstance() { return new DeathStreamLock(plugin); }
    }

    // =========================================================================
    // 94. (Doc #94) DEATH GROUND RESONANCE -- arena floor breathes upward (2-4s)
    // =========================================================================
    public static class DeathGroundResonance extends EnvironmentalAttack {

        public DeathGroundResonance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("death_ground_resonance", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(60); // 3 seconds
            config.setCooldownTicks(99999); // Scripted
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Floor particles rising from all cracks and zones
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 15; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 22;
                    Location floorLoc = center.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);

                    // Mixed SPELL_WITCH + TOTEM rising
                    w.spawnParticle(Particle.WITCH, floorLoc, 5, 0.3, 3, 0.3, 0);
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, floorLoc, 4, 0.3, 4, 0.3, 0.03);
                }
            }

            // Cracked surfaces illuminated
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(center, 20, 8.0, 180, 0, 200, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeathGroundResonance(plugin); }
    }

    // =========================================================================
    // 95. (Doc #95) DEATH CRYSTAL FLASH -- END_ROD columns from all pillars (4-6s)
    // =========================================================================
    public static class DeathCrystalFlash extends EnvironmentalAttack {

        private final Location[] pillarBases = new Location[8];

        public DeathCrystalFlash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("death_crystal_flash", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(60); // 3 seconds
            config.setCooldownTicks(99999); // Scripted
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 pillar/crystal remnant bases around arena
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                pillarBases[i] = center.clone().add(Math.cos(angle) * 14, 0, Math.sin(angle) * 14);
            }

            // Staggered resonance sounds
            for (int i = 0; i < 8; i++) {
                final int idx = i;
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        DisplayBuilder.playSound(pillarBases[idx], Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.5f), idx * 4L);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // 8 columns of END_ROD rising 30 blocks from each pillar base
            if (ticksAlive % 3 == 0) {
                for (Location base : pillarBases) {
                    if (base == null) continue;
                    for (double y = 0; y < 30; y += 2) {
                        w.spawnParticle(Particle.END_ROD, base.clone().add(0, y, 0), 3, 0.3, 0.3, 0.3, 0.01);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeathCrystalFlash(plugin); }
    }

    // =========================================================================
    // 96. (Doc #96) DEATH STREAM CONVERGENCE -- all streams angle toward Dragon (6-9s)
    // =========================================================================
    public static class DeathStreamConvergence extends EnvironmentalAttack {

        public DeathStreamConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("death_stream_convergence", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // 4 seconds
            config.setCooldownTicks(99999); // Scripted
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // Silent except for underlying ENTITY_ENDER_DRAGON_DEATH continuing
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Dragon apex position (high above center)
            Location dragonApex = center.clone().add(0, 40, 0);

            // All streams converging on Dragon from all directions
            if (ticksAlive % 2 == 0) {
                for (int s = 0; s < 12; s++) {
                    double angle = (2 * Math.PI * s) / 12 + ticksAlive * 0.02;
                    double progress = (ticksAlive % 40) / 40.0;
                    double skyR = 30 * (1 - progress);
                    double skyY = center.getY() + 10 + 30 * progress;
                    Location streamPos = center.clone().add(Math.cos(angle) * skyR, skyY - center.getY(), Math.sin(angle) * skyR);

                    switch (s % 4) {
                        case 0 -> DisplayBuilder.dustParticles(streamPos, 8, 0.5, 180, 0, 200, 2.0f);
                        case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamPos, 6, 0.5, 0.5, 0.5, 0.03);
                        case 2 -> w.spawnParticle(Particle.END_ROD, streamPos, 6, 0.5, 0.5, 0.5, 0.02);
                        case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, streamPos, 6, 0.5, 0.5, 0.5, 0.02);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeathStreamConvergence(plugin); }
    }

    // =========================================================================
    // 97. (Doc #97) DEATH BEAM LAYERED BURST -- multi-colored concentric beam (9-13s)
    // =========================================================================
    public static class DeathBeamLayered extends EnvironmentalAttack {

        public DeathBeamLayered(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("death_beam_layered", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(99999); // Scripted
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.3f);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.5f), 20L);
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.3f), 40L);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Concentric multi-colored beam rising to maximum height
            if (ticksAlive % 2 == 0) {
                double beamHeight = Math.min(ticksAlive * 2, 80);

                for (double y = 0; y < beamHeight; y += 1.5) {
                    Location beamLoc = center.clone().add(0, y, 0);

                    // White core (0.5 block radius)
                    w.spawnParticle(Particle.END_ROD, beamLoc, 3, 0.15, 0.3, 0.15, 0.01);

                    // Magenta inner ring (1.0 block radius)
                    if (y % 3 == 0) {
                        double a = (y * 0.5) % (2 * Math.PI);
                        DisplayBuilder.dustParticles(
                                beamLoc.clone().add(Math.cos(a) * 0.7, 0, Math.sin(a) * 0.7),
                                2, 0.2, 180, 0, 200, 1.5f);
                    }

                    // Gold middle ring (1.5 block radius)
                    if (y % 4 == 0) {
                        double a = (y * 0.3 + Math.PI) % (2 * Math.PI);
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING,
                                beamLoc.clone().add(Math.cos(a) * 1.2, 0, Math.sin(a) * 1.2),
                                2, 0.1, 0.1, 0.1, 0.01);
                    }

                    // Purple outer shell (2.0 block radius)
                    if (y % 5 == 0) {
                        double a = (y * 0.2) % (2 * Math.PI);
                        w.spawnParticle(Particle.DRAGON_BREATH,
                                beamLoc.clone().add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5),
                                2, 0.2, 0.2, 0.2, 0.01);
                    }

                    // Gold sparkle throughout
                    if (y % 6 == 0) {
                        w.spawnParticle(Particle.CRIT, beamLoc, 2, 1.0, 0.3, 1.0, 0.05);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeathBeamLayered(plugin); }
    }

    // =========================================================================
    // 98. (Doc #98) DEATH FLOOR SEAL -- all hazard zones seal closed (13-16s)
    // =========================================================================
    public static class DeathFloorSeal extends EnvironmentalAttack {

        private boolean sealed = false;

        public DeathFloorSeal(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("death_floor_seal", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // 4 seconds
            config.setCooldownTicks(99999); // Scripted
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // No initial sound -- silence from beam fading
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Particles rushing inward from all zone locations toward center
            if (ticksAlive <= 60) {
                if (ticksAlive % 3 == 0) {
                    double progress = ticksAlive / 60.0;
                    for (int i = 0; i < 12; i++) {
                        double angle = (2 * Math.PI * i) / 12;
                        double startDist = 20 * (1 - progress);
                        Location zoneLoc = center.clone().add(
                                Math.cos(angle) * startDist, 0.5, Math.sin(angle) * startDist);
                        w.spawnParticle(Particle.WITCH, zoneLoc, 5, 0.3, 0.3, 0.3, 0);
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, zoneLoc, 4, 0.3, 0.3, 0.3, 0.02);
                    }
                }
            }

            // Final seal burst at center
            if (ticksAlive >= 60 && !sealed) {
                sealed = true;

                w.spawnParticle(Particle.WITCH, center, 100, 2, 1, 2, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 100, 2, 1, 2, 0.05);

                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.2f);

                // 1.5 second silence after seal
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeathFloorSeal(plugin); }
    }

    // =========================================================================
    // 99. (Doc #99) DEATH STREAM DISSIPATION -- streams fade to Phase 1 baseline (16-19s)
    // =========================================================================
    public static class DeathStreamDissipation extends EnvironmentalAttack {

        public DeathStreamDissipation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("death_stream_dissipation", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // 4 seconds
            config.setCooldownTicks(99999); // Scripted
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Barely perceptible background hum
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.05f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Linear fade from triple Phase 3 to Phase 1 baseline
            double density = Math.max(0.1, 1.0 - (ticksAlive / 60.0));
            int particleCount = Math.max(1, (int) (8 * density));

            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < particleCount; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 25;
                    double skyY = center.getY() + 15 + Math.random() * 20;
                    Location skyPos = center.clone().add(Math.cos(angle) * dist, skyY - center.getY(), Math.sin(angle) * dist);

                    switch (i % 4) {
                        case 0 -> DisplayBuilder.dustParticles(skyPos, 3, 0.5, 180, 0, 200, 1.0f);
                        case 1 -> w.spawnParticle(Particle.TOTEM_OF_UNDYING, skyPos, 2, 0.3, 0.3, 0.3, 0.01);
                        case 2 -> w.spawnParticle(Particle.END_ROD, skyPos, 2, 0.3, 0.3, 0.3, 0.01);
                        case 3 -> w.spawnParticle(Particle.DRAGON_BREATH, skyPos, 2, 0.3, 0.3, 0.3, 0.01);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeathStreamDissipation(plugin); }
    }

    // =========================================================================
    // 100. (Doc #100) DRAGON EGG REACTIVATION -- egg rises, gold pulse, gem ready (19-22s)
    // =========================================================================
    public static class DragonEggReactivation extends EnvironmentalAttack {

        private boolean activated = false;
        private boolean pulsing = false;

        public DragonEggReactivation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_egg_reactivation", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0); // No damage
            config.setDamageRadius(0.0);
            config.setDurationTicks(600); // 30 seconds (indefinite pulse until gem deposit)
            config.setCooldownTicks(99999); // Scripted
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // No initial effects -- silence after dissipation
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Egg position at top of central beacon pillar
            Location eggPos = center.clone().add(0, 10, 0);

            // Rising helix: TOTEM + CRIT spiral for first 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 2 == 0) {
                    double progress = ticksAlive / 40.0;
                    double helixY = progress * 10; // Rising alongside egg
                    double helixAngle = ticksAlive * 0.3;

                    for (int strand = 0; strand < 2; strand++) {
                        double a = helixAngle + strand * Math.PI;
                        Location helixLoc = center.clone().add(
                                Math.cos(a) * 1.5, helixY, Math.sin(a) * 1.5);
                        w.spawnParticle(Particle.TOTEM_OF_UNDYING, helixLoc, 5, 0.1, 0.1, 0.1, 0.01);
                        w.spawnParticle(Particle.CRIT, helixLoc, 3, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                return;
            }

            // Crown: DRAGON_BREATH burst at egg top
            if (!activated) {
                activated = true;

                w.spawnParticle(Particle.DRAGON_BREATH, eggPos.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.02);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.0f);
            }

            // Resting pulse: 4 TOTEM particles per second from egg surface
            if (ticksAlive > 50) {
                if (!pulsing) pulsing = true;

                if (ticksAlive % 5 == 0) { // ~4 per second
                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, eggPos, 2, 0.3, 0.3, 0.3, 0.01);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DragonEggReactivation(plugin); }
    }
}
