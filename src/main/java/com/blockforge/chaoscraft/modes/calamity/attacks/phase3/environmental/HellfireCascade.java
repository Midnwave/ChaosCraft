package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.environmental;

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
 * Phase 3 Environmental -- GROUP 8: HELLFIRE CASCADE EVENTS
 * 10 attacks (#71-80) treating fire as a propagating system.
 * Chain reaction infernos, spreading fire, the island burning.
 *
 * Design notes:
 * - No status effects
 * - Dweller palette: crimson (200,0,50), orange glow (255,100,0), soul blue (0,150,255)
 * - Fire cascade materials: MAGMA_BLOCK, NETHERRACK, NETHER_WART_BLOCK, CRIMSON_NYLIUM
 * - Higher damage floor (8-14 HP per attack), shorter warnings
 */
public final class HellfireCascade {

    private HellfireCascade() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IgnitionChain(plugin));
        registry.register(new CascadeSurge(plugin));
        registry.register(new InfernoWall(plugin));
        registry.register(new FireMemory(plugin));
        registry.register(new SkyDropEmbers(plugin));
        registry.register(new Backdraft(plugin));
        registry.register(new CinderStorm(plugin));
        registry.register(new HelicalInferno(plugin));
        registry.register(new SuperheatedHalo(plugin));
        registry.register(new ConflagrationPulse(plugin));
    }

    // =========================================================================
    // 71. IGNITION CHAIN -- fire jumps tile-to-tile, then branches
    // =========================================================================
    public static class IgnitionChain extends EnvironmentalAttack {

        private Location chainPos;
        private Location branchA;
        private Location branchB;
        private double chainAngle;
        private int jumpCount = 0;
        private boolean branched = false;

        public IgnitionChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ignition_chain", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(10.0); // 5 hearts/sec sustained
            config.setDamageRadius(1.0);
            config.setDurationTicks(240); // 12 seconds
            config.setCooldownTicks(320); // 16 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            chainPos = center.clone();
            chainAngle = Math.random() * 2 * Math.PI;

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.8f);

            // Warning: crimson spore cluster at start
            DisplayBuilder.dustParticles(center, 10, 0.5, 200, 40, 20, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5 seconds)
            if (ticksAlive <= 30) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(chainPos, 6, 0.4, 200, 40, 20, 0.6f);
                }
                return;
            }

            int chainTick = ticksAlive - 30;

            // Jump every 20 ticks (1 jump/sec)
            if (chainTick % 20 == 0 && !branched) {
                jumpCount++;
                chainPos = chainPos.clone().add(Math.cos(chainAngle) * 1.2, 0, Math.sin(chainAngle) * 1.2);

                // Soul fire pillar at ignition point
                BlockDisplayHandle h = displayBuilder.spawnBlock(chainPos, Material.MAGMA_BLOCK);
                h.scale(1.0f, 0.15f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                spawnedEntities.add(h.entity());

                DisplayBuilder.dustParticles(chainPos.clone().add(0, 3, 0), 8, 0.3, 0, 255, 200, 1.0f);
                DisplayBuilder.dustParticles(chainPos, 5, 0.3, 255, 130, 0, 0.8f);
                DisplayBuilder.playSound(chainPos, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.7f);

                // Damage on ignition
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(chainPos) <= 2.25) {
                        p.damage(18.0); // 9 hearts on ignition burst
                    }
                }

                // Pillar grows taller with each jump
                float pillarHeight = 1.0f + jumpCount * 0.5f;
                BlockDisplayHandle pillarH = displayBuilder.spawnBlock(chainPos.clone().add(0, 0.5, 0), Material.NETHERRACK);
                pillarH.scale(0.3f, pillarHeight, 0.3f).glow(0, 150, 255).interpolation(5, 0);
                spawnedEntities.add(pillarH.entity());

                // Branch after 8 jumps
                if (jumpCount >= 8) {
                    branched = true;
                    branchA = chainPos.clone();
                    branchB = chainPos.clone();
                }
            }

            // After branching: two simultaneous chains
            if (branched && chainTick % 10 == 0) {
                double branchAngleA = chainAngle + 0.4;
                double branchAngleB = chainAngle - 0.4;

                branchA = branchA.clone().add(Math.cos(branchAngleA) * 1.2, 0, Math.sin(branchAngleA) * 1.2);
                branchB = branchB.clone().add(Math.cos(branchAngleB) * 1.2, 0, Math.sin(branchAngleB) * 1.2);

                for (Location bLoc : new Location[]{branchA, branchB}) {
                    BlockDisplayHandle bh = displayBuilder.spawnBlock(bLoc, Material.MAGMA_BLOCK);
                    bh.scale(1.0f, 0.15f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                    spawnedEntities.add(bh.entity());

                    DisplayBuilder.dustParticles(bLoc.clone().add(0, 2, 0), 6, 0.3, 255, 130, 0, 0.7f);
                    DisplayBuilder.playSound(bLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.4f, 0.8f);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(bLoc) <= 2.25) {
                            p.damage(18.0); // 9 hearts
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IgnitionChain(plugin); }
    }

    // =========================================================================
    // 72. CASCADE SURGE -- entire island fire intensity quadruples
    // =========================================================================
    public static class CascadeSurge extends EnvironmentalAttack {

        private boolean surging = false;

        public CascadeSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cascade_surge", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(12.0); // 6 hearts/sec doubled brimstone
            config.setDamageRadius(20.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(360); // 18 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds) -- soul fire rain from above
            if (ticksAlive <= 40) {
                float volume = 0.5f + (ticksAlive / 40.0f) * 1.5f;
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, volume, 0.5f);
                    for (int i = 0; i < 15; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 15;
                        Location loc = center.clone().add(Math.cos(angle) * dist, 8, Math.sin(angle) * dist);
                        DisplayBuilder.dustParticles(loc, 3, 0.5, 0, 200, 255, 0.7f);
                    }
                }
                return;
            }

            if (!surging) {
                surging = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            }

            // Dense fire carpet across entire island
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 25; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 15;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);

                    // Quadrupled particle output
                    w.spawnParticle(Particle.LAVA, loc.clone().add(0, 0.5, 0), 4, 0.2, 0.5, 0.2, 0);
                    w.spawnParticle(Particle.DRIPPING_LAVA, loc.clone().add(0, 1, 0), 2, 0.3, 0, 0.3, 0);
                    DisplayBuilder.dustParticles(loc, 3, 0.3, 255, 100, 0, 0.8f);
                }

                // Smoke columns 8 blocks high
                for (int i = 0; i < 8; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 12;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 4, Math.sin(angle) * dist);
                    w.spawnParticle(Particle.SMOKE, loc, 6, 0.5, 3.0, 0.5, 0.02);
                }

                // Fire charge item displays
                if (ticksAlive % 9 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = 3 + Math.random() * 10;
                        Location loc = center.clone().add(Math.cos(angle) * dist, 2, Math.sin(angle) * dist);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                        float rot = (float) (Math.random() * Math.PI);
                        h.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 0)
                                .rotate(rot, 0.5f, 1, 0.3f).interpolation(10, 0);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            // Sky darkening effect
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 12, 0), 20, 12.0, 120, 30, 10, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CascadeSurge(plugin); }
    }

    // =========================================================================
    // 73. INFERNO WALL -- solid fire wall sweeps across arena
    // =========================================================================
    public static class InfernoWall extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private double sweepAngle;
        private Location sweepOrigin;
        private boolean sweeping = false;

        public InfernoWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("inferno_wall", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(400); // 20 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            sweepAngle = Math.random() * 2 * Math.PI;
            sweepOrigin = center.clone().add(Math.cos(sweepAngle) * -18, 0, Math.sin(sweepAngle) * -18);

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.2f, 0.8f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds) -- wall appears at edge
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    double perpAngle = sweepAngle + Math.PI / 2;
                    for (int i = -5; i <= 5; i++) {
                        Location loc = sweepOrigin.clone().add(
                                Math.cos(perpAngle) * i, 2.5, Math.sin(perpAngle) * i);
                        DisplayBuilder.dustParticles(loc, 4, 0.3, 255, 80, 0, 1.0f);
                        DisplayBuilder.dustParticles(loc.clone().add(0, 1, 0), 3, 0.2, 0, 230, 220, 0.8f);
                    }
                }
                return;
            }

            if (!sweeping) {
                sweeping = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
            }

            int sweepTick = ticksAlive - 40;
            double advanceDist = sweepTick * 0.3; // ~6 blocks/second

            // Current wall position
            Location wallCenter = sweepOrigin.clone().add(
                    Math.cos(sweepAngle) * advanceDist, 0, Math.sin(sweepAngle) * advanceDist);

            // Remove old wall displays
            for (BlockDisplayHandle h : wallHandles) {
                h.entity().remove();
            }
            wallHandles.clear();

            // Spawn wall: 10 blocks wide, 5 blocks tall
            double perpAngle = sweepAngle + Math.PI / 2;
            if (advanceDist <= 36) { // Still in arena
                for (int i = -5; i <= 5; i++) {
                    for (int y = 0; y < 5; y++) {
                        Location loc = wallCenter.clone().add(
                                Math.cos(perpAngle) * i, y, Math.sin(perpAngle) * i);
                        Material mat = (y % 2 == 0) ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                        h.scale(0.9f, 0.9f, 0.3f).glow(255, 100, 0).interpolation(2, 0);
                        wallHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // Dense particles around wall
                if (sweepTick % 2 == 0) {
                    for (int i = -5; i <= 5; i++) {
                        Location pLoc = wallCenter.clone().add(
                                Math.cos(perpAngle) * i, 2.5, Math.sin(perpAngle) * i);
                        DisplayBuilder.dustParticles(pLoc, 4, 0.3, 255, 80, 0, 1.0f);
                        DisplayBuilder.dustParticles(pLoc, 3, 0.2, 0, 230, 220, 0.8f);
                        w.spawnParticle(Particle.SMOKE, pLoc.clone().add(-Math.cos(sweepAngle) * 0.5, 0, -Math.sin(sweepAngle) * 0.5),
                                3, 0.3, 1.0, 0.3, 0.02);
                    }

                    // Floor laser line
                    DisplayBuilder.dustParticles(wallCenter, 6, 5.0, 0, 150, 255, 0.6f);
                }
            }

            // Damage: players inside the wall
            if (sweepTick % 4 == 0 && advanceDist <= 36) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    // Check if player is within wall bounds
                    double alongWall = (pl.getX() - wallCenter.getX()) * Math.cos(perpAngle) +
                            (pl.getZ() - wallCenter.getZ()) * Math.sin(perpAngle);
                    double throughWall = (pl.getX() - wallCenter.getX()) * Math.cos(sweepAngle) +
                            (pl.getZ() - wallCenter.getZ()) * Math.sin(sweepAngle);
                    if (Math.abs(alongWall) <= 5.5 && Math.abs(throughWall) <= 1.0 && pl.getY() - wallCenter.getY() < 5) {
                        p.damage(10.0); // 10 hearts/sec equivalent
                        DisplayBuilder.crimsonDust(pl, 8, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InfernoWall(plugin); }
    }

    // =========================================================================
    // 74. FIRE MEMORY -- all previously burned tiles re-ignite
    // =========================================================================
    public static class FireMemory extends EnvironmentalAttack {

        private final List<Location> burnLocations = new ArrayList<>();
        private boolean reignited = false;

        public FireMemory(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fire_memory", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(300); // 15 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.6f);

            // Simulate fire history -- near-total coverage since most arena is burned
            for (int i = 0; i < 40; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = 1 + Math.random() * 14;
                burnLocations.add(center.clone().add(Math.cos(angle) * dist, 0.05, Math.sin(angle) * dist));
            }

            // Ash warning rain
            for (int i = 0; i < 15; i++) {
                double angle = Math.random() * 2 * Math.PI;
                double dist = Math.random() * 14;
                Location loc = center.clone().add(Math.cos(angle) * dist, 4, Math.sin(angle) * dist);
                DisplayBuilder.dustParticles(loc, 3, 0.5, 140, 135, 125, 0.6f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 14;
                        Location loc = center.clone().add(Math.cos(angle) * dist, 3, Math.sin(angle) * dist);
                        DisplayBuilder.dustParticles(loc, 2, 0.4, 140, 135, 125, 0.5f);
                    }
                }
                return;
            }

            // Re-ignition
            if (!reignited) {
                reignited = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);

                // All burn locations ignite
                for (Location loc : burnLocations) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    h.scale(0.9f, 0.08f, 0.9f).glow(255, 100, 0).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            int fireTick = ticksAlive - 40;

            // Fire particles from all re-ignited locations
            if (fireTick % 4 == 0) {
                for (int i = 0; i < burnLocations.size(); i += 3) {
                    Location loc = burnLocations.get(i);
                    w.spawnParticle(Particle.LAVA, loc.clone().add(0, 0.5, 0), 2, 0.2, 0.5, 0.2, 0);
                    DisplayBuilder.dustParticles(loc, 3, 0.3, 255, 110, 0, 0.7f);
                    w.spawnParticle(Particle.DRIPPING_LAVA, loc.clone().add(0, 2, 0), 1, 0.3, 0, 0.3, 0);
                }

                // Crimson spores from oldest zones, soul from newest
                DisplayBuilder.dustParticles(burnLocations.get(0).clone().add(0, 0.3, 0), 3, 1.0, 180, 25, 15, 0.5f);
                DisplayBuilder.dustParticles(burnLocations.get(burnLocations.size() - 1).clone().add(0, 0.3, 0),
                        3, 1.0, 0, 255, 220, 0.5f);
            }

            // Damage on any re-ignited tile
            if (fireTick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (Location loc : burnLocations) {
                        if (p.getLocation().distanceSquared(loc) <= 1.5) {
                            p.damage(16.0); // 8 hearts
                            break;
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FireMemory(plugin); }
    }

    // =========================================================================
    // 75. SKY DROP EMBERS -- massive embers fall from sky in clusters
    // =========================================================================
    public static class SkyDropEmbers extends EnvironmentalAttack {

        private final Location[] clusterTargets = new Location[6];
        private final List<BlockDisplayHandle> emberHandles = new ArrayList<>();
        private boolean impacted = false;

        public SkyDropEmbers(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_drop_embers", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(8.0); // 4 hearts/sec crater
            config.setDamageRadius(3.0);
            config.setDurationTicks(320); // 16 seconds (6 falling + 10 crater)
            config.setCooldownTicks(340); // 17 seconds
            config.setDamageOnImpactOnly(false);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.7f);

            // 6 cluster target locations
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6 + Math.random() * 0.5;
                double dist = 3 + Math.random() * 10;
                clusterTargets[i] = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Falling phase: 120 ticks (6 seconds) -- visible from 60 ticks (3s warning)
            if (ticksAlive <= 120) {
                if (ticksAlive <= 60) {
                    // Ember trajectories visible from 3 seconds before impact
                    if (ticksAlive % 6 == 0) {
                        for (Location target : clusterTargets) {
                            if (target == null) continue;
                            double height = 20 - (ticksAlive / 60.0) * 10;
                            Location emberLoc = target.clone().add(0, height, 0);
                            DisplayBuilder.dustParticles(emberLoc, 4, 0.3, 255, 90, 0, 1.0f);
                            w.spawnParticle(Particle.SMOKE, emberLoc.clone().add(0, 1, 0), 3, 0.2, 0.5, 0.2, 0.01);
                        }
                    }
                    return;
                }

                // Spawn and animate falling ember displays
                int fallTick = ticksAlive - 60;
                double height = 15 - (fallTick / 60.0) * 15;

                if (fallTick % 4 == 0) {
                    // Remove old ember displays
                    for (BlockDisplayHandle h : emberHandles) {
                        h.entity().remove();
                    }
                    emberHandles.clear();

                    for (int i = 0; i < 6; i++) {
                        if (clusterTargets[i] == null) continue;

                        // 3-5 embers per cluster
                        int clusterSize = 3 + (i % 3);
                        for (int j = 0; j < clusterSize; j++) {
                            Location emberLoc = clusterTargets[i].clone().add(
                                    (Math.random() - 0.5) * 2, height + Math.random() * 2, (Math.random() - 0.5) * 2);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(emberLoc, Material.MAGMA_BLOCK);
                            float rot = fallTick * 0.1f + j;
                            h.scale(0.6f, 0.6f, 0.6f).glow(255, 100, 0)
                                    .rotate(rot, 0.5f, 1, 0.3f).interpolation(3, 0);
                            emberHandles.add(h);
                            spawnedEntities.add(h.entity());

                            // Flame orbit + smoke trail
                            DisplayBuilder.dustParticles(emberLoc, 3, 0.3, 255, 90, 0, 0.8f);
                            w.spawnParticle(Particle.SMOKE, emberLoc.clone().add(0, 0.5, 0), 2, 0.1, 0.3, 0.1, 0.01);
                        }
                    }
                }

                // Impact at tick 120
                if (ticksAlive >= 118 && !impacted) {
                    impacted = true;
                    for (Location target : clusterTargets) {
                        if (target == null) continue;

                        DisplayBuilder.playSound(target, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);

                        // Ground burst
                        w.spawnParticle(Particle.LAVA, target.clone().add(0, 0.5, 0), 20, 3.0, 0.5, 3.0, 0);

                        // Impact damage
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dist = p.getLocation().distance(target);
                            if (dist <= 1.5) {
                                p.damage(20.0); // 10 hearts direct
                            } else if (dist <= 3.0) {
                                p.damage(10.0); // 5 hearts splash
                            }
                        }

                        // Crater display
                        BlockDisplayHandle craterH = displayBuilder.spawnBlock(target, Material.MAGMA_BLOCK);
                        craterH.scale(3.0f, 0.1f, 3.0f).glow(200, 0, 50).interpolation(5, 0);
                        spawnedEntities.add(craterH.entity());
                    }
                }

                return;
            }

            // Crater burn phase -- remaining ticks
            if (ticksAlive % 6 == 0) {
                for (Location target : clusterTargets) {
                    if (target == null) continue;
                    w.spawnParticle(Particle.DRIPPING_LAVA, target.clone().add(0, 0.5, 0), 3, 1.5, 0, 1.5, 0);
                    DisplayBuilder.dustParticles(target, 3, 1.5, 255, 100, 0, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyDropEmbers(plugin); }
    }

    // =========================================================================
    // 76. BACKDRAFT -- fire pulled inward then explosive outward burst
    // =========================================================================
    public static class Backdraft extends EnvironmentalAttack {

        private boolean detonated = false;

        public Backdraft(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("backdraft", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(440); // 22 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Quiet warning -- sound vacuum
            // Ambient sounds drop (simulated by playing nothing for 2 seconds)
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Inward pull phase: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                // Particles lean inward toward center
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = 5 + Math.random() * 10;
                        Location from = center.clone().add(Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist);
                        // Pull direction toward center
                        double pullDist = dist * (1 - ticksAlive / 40.0) + 1;
                        Location pullLoc = center.clone().add(Math.cos(angle) * pullDist, 0.5, Math.sin(angle) * pullDist);
                        DisplayBuilder.dustParticles(pullLoc, 3, 0.3, 255, 100, 0, 0.7f);
                    }
                }
                return;
            }

            // DETONATION
            if (!detonated) {
                detonated = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);

                // Massive radial burst
                for (int ring = 0; ring < 20; ring++) {
                    double radius = ring;
                    int points = Math.max(4, (int) (radius * 3));
                    for (int p = 0; p < points; p++) {
                        double angle = (2 * Math.PI * p) / points;
                        Location loc = center.clone().add(Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);

                        // Multi-particle explosion
                        DisplayBuilder.dustParticles(loc, 2, 0.3, 0, 255, 200, 0.8f);
                        DisplayBuilder.dustParticles(loc, 2, 0.3, 255, 100, 0, 0.8f);
                    }
                }

                w.spawnParticle(Particle.LAVA, center.clone().add(0, 1, 0), 40, 8.0, 1.0, 8.0, 0);

                // Fire charge projectile displays flying outward
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI * i) / 12;
                    Location fcLoc = center.clone().add(Math.cos(angle) * 3, 1, Math.sin(angle) * 3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(fcLoc, Material.NETHER_WART_BLOCK);
                    h.scale(0.4f, 0.4f, 0.4f).glow(200, 0, 50).interpolation(10, 0);
                    spawnedEntities.add(h.entity());
                }

                // Distance-based damage to all players
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist <= 5) {
                        p.damage(20.0); // 20 hearts close range -- very dangerous
                    } else if (dist <= 12) {
                        p.damage(12.0); // 12 hearts mid-range
                    } else {
                        p.damage(16.0); // 8 hearts far range
                    }
                    // Knockback outward
                    double dx = p.getLocation().getX() - center.getX();
                    double dz = p.getLocation().getZ() - center.getZ();
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > 0.1) {
                        double force = Math.max(0.3, 1.5 - dist * 0.1);
                        p.setVelocity(p.getVelocity().add(
                                new org.bukkit.util.Vector(dx / d * force, 0.4, dz / d * force)));
                    }
                }
            }

            int detTick = ticksAlive - 40;

            // Aftermath particles
            if (detTick % 4 == 0 && detTick <= 60) {
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = detTick * 0.3 + Math.random() * 3;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(loc, 3, 0.5, 200, 0, 50, 0.6f);
                    w.spawnParticle(Particle.LAVA, loc, 1, 0.2, 0.3, 0.2, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Backdraft(plugin); }
    }

    // =========================================================================
    // 77. CINDER STORM -- horizontal storm at mid-height, crouch to dodge
    // =========================================================================
    public static class CinderStorm extends EnvironmentalAttack {

        private double stormAngle;
        private boolean stormActive = false;

        public CinderStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cinder_storm", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(320); // 16 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            stormAngle = Math.random() * 2 * Math.PI;
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds) -- smoke streaming from upwind edge
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double perpAngle = stormAngle + Math.PI / 2;
                        double perpDist = (Math.random() - 0.5) * 20;
                        Location loc = center.clone().add(
                                Math.cos(stormAngle) * -15 + Math.cos(perpAngle) * perpDist,
                                2.5,
                                Math.sin(stormAngle) * -15 + Math.sin(perpAngle) * perpDist);
                        w.spawnParticle(Particle.SMOKE, loc, 3, 0.3, 0.3, 0.3, 0.02);
                    }
                    float pitch = 0.5f + (ticksAlive / 40.0f) * 0.8f;
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f + ticksAlive / 40.0f, pitch);
                }
                return;
            }

            if (!stormActive) {
                stormActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.2f);
            }

            // Horizontal storm at 2-3 blocks height
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 20; i++) {
                    double perpAngle = stormAngle + Math.PI / 2;
                    double perpDist = (Math.random() - 0.5) * 20;
                    double alongDist = (Math.random() - 0.5) * 30;
                    double height = 2.0 + Math.random() * 1.0;

                    Location cinderLoc = center.clone().add(
                            Math.cos(stormAngle) * alongDist + Math.cos(perpAngle) * perpDist,
                            height,
                            Math.sin(stormAngle) * alongDist + Math.sin(perpAngle) * perpDist);

                    // Dense in center lane, thinner at edges
                    double density = 1.0 - Math.abs(perpDist) / 15.0;
                    if (density <= 0) continue;

                    DisplayBuilder.dustParticles(cinderLoc, (int) (3 * density), 0.3, 255, 70, 0, 0.8f);
                    DisplayBuilder.dustParticles(cinderLoc, (int) (2 * density), 0.2, 150, 145, 140, 0.5f);
                }

                // Tumbling fire charge displays
                if (ticksAlive % 6 == 0) {
                    double perpAngle = stormAngle + Math.PI / 2;
                    Location fcLoc = center.clone().add(
                            Math.cos(perpAngle) * (Math.random() - 0.5) * 12, 2.5,
                            Math.sin(perpAngle) * (Math.random() - 0.5) * 12);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(fcLoc, Material.NETHER_WART_BLOCK);
                    float rot = (float) (Math.random() * Math.PI * 2);
                    h.scale(0.3f, 0.3f, 0.3f).glow(255, 100, 0)
                            .rotate(rot, 0.7f, 0.5f, 0.3f).interpolation(4, 0);
                    spawnedEntities.add(h.entity());

                    w.spawnParticle(Particle.SMOKE, fcLoc.clone().add(0, 0.3, 0), 2, 0.2, 0.1, 0.2, 0.01);
                }
            }

            // Damage based on player height
            if (ticksAlive % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double eyeHeight = p.getEyeHeight();
                    boolean isSneaking = p.isSneaking();

                    if (isSneaking) {
                        // Crouching: reduced damage
                        p.damage(8.0); // 4 hearts/sec
                    } else {
                        // Standing upright
                        p.damage(16.0); // 8 hearts/sec
                    }

                    DisplayBuilder.dustParticles(p.getLocation().clone().add(0, 2, 0), 4, 0.3, 255, 70, 0, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CinderStorm(plugin); }
    }

    // =========================================================================
    // 78. HELICAL INFERNO -- rotating double-helix fire column from center
    // =========================================================================
    public static class HelicalInferno extends EnvironmentalAttack {

        private boolean helixActive = false;

        public HelicalInferno(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("helical_inferno", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(380); // 19 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
            DisplayBuilder.dustParticles(center, 12, 1.0, 200, 30, 20, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                if (ticksAlive % 6 == 0) {
                    double swirl = ticksAlive * 0.2;
                    for (int p = 0; p < 8; p++) {
                        double angle = swirl + (Math.PI * 2 * p) / 8;
                        double r = 2.0;
                        Location swirlLoc = center.clone().add(Math.cos(angle) * r, 0.2, Math.sin(angle) * r);
                        DisplayBuilder.dustParticles(swirlLoc, 3, 0.2, 200, 30, 20, 0.6f);
                    }
                }
                return;
            }

            if (!helixActive) {
                helixActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);

                // Center column block
                BlockDisplayHandle colH = displayBuilder.spawnBlock(center.clone().add(0, 4, 0), Material.MAGMA_BLOCK);
                colH.scale(2.0f, 8.0f, 2.0f).glow(0, 150, 255).interpolation(10, 0);
                spawnedEntities.add(colH.entity());
            }

            int helixTick = ticksAlive - 40;

            // Column height grows from 2 to 16
            float columnHeight = Math.min(16, 2 + helixTick * 0.2f);

            // Double helix arms -- inner (soul fire) and outer (flame), opposite rotation
            if (helixTick % 2 == 0) {
                double innerAngle = helixTick * 0.15; // Inner helix rotation
                double outerAngle = -helixTick * 0.12; // Outer helix counter-rotation

                for (float y = 0; y < columnHeight; y += 0.5f) {
                    // Inner helix: soul fire
                    double innerR = 2.0 + Math.sin(y * 0.5) * 0.5;
                    double iAngle = innerAngle + y * 0.4;
                    Location innerLoc = center.clone().add(Math.cos(iAngle) * innerR, y, Math.sin(iAngle) * innerR);
                    DisplayBuilder.dustParticles(innerLoc, 2, 0.1, 0, 255, 210, 0.8f);

                    // Outer helix: flame
                    double outerR = 4.0 + Math.sin(y * 0.3) * 1.0;
                    double oAngle = outerAngle + y * 0.4;
                    Location outerLoc = center.clone().add(Math.cos(oAngle) * outerR, y, Math.sin(oAngle) * outerR);
                    DisplayBuilder.dustParticles(outerLoc, 2, 0.1, 255, 80, 0, 0.8f);

                    // Lava cascade at 4-block intervals from outer arm
                    if (Math.abs(y % 4) < 0.5) {
                        w.spawnParticle(Particle.LAVA, outerLoc, 2, 0.3, 0.2, 0.3, 0);
                    }
                }

                // Embedded displays at 8-block height mark
                if (helixTick % 20 == 0 && columnHeight >= 8) {
                    double embedAngle = outerAngle + 8 * 0.4;
                    Location embedLoc = center.clone().add(Math.cos(embedAngle) * 4, 8, Math.sin(embedAngle) * 4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(embedLoc, Material.BLACKSTONE);
                    h.scale(0.4f, 0.4f, 0.4f).glow(200, 0, 50)
                            .rotate((float) embedAngle, 0, 1, 0).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            // Ground-level arm sweep damage
            if (helixTick % 4 == 0) {
                double innerAngle = helixTick * 0.15;
                double outerAngle = -helixTick * 0.12;

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);

                    // Center column contact
                    if (dist <= 2.0) {
                        p.damage(14.0); // 14 hearts direct column
                        DisplayBuilder.dustParticles(p.getLocation(), 8, 0.3, 0, 150, 255, 1.0f);
                        continue;
                    }

                    // Outer arm sweep at ground level
                    double playerAngle = Math.atan2(
                            p.getLocation().getZ() - center.getZ(),
                            p.getLocation().getX() - center.getX());
                    double angleDiff = Math.abs(playerAngle - outerAngle);
                    if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;
                    if (angleDiff < 0.3 && dist >= 3 && dist <= 5) {
                        p.damage(18.0); // 9 hearts helix arm sweep
                        DisplayBuilder.dustParticles(p.getLocation(), 6, 0.3, 255, 80, 0, 0.8f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HelicalInferno(plugin); }
    }

    // =========================================================================
    // 79. SUPERHEATED HALO -- fire ring at island perimeter
    // =========================================================================
    public static class SuperheatedHalo extends EnvironmentalAttack {

        private boolean haloActive = false;

        public SuperheatedHalo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("superheated_halo", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(400); // 20 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 50 ticks (2.5 seconds) -- crimson spore ring outline
            if (ticksAlive <= 50) {
                if (ticksAlive % 5 == 0) {
                    int points = 24;
                    for (int i = 0; i < points; i++) {
                        double angle = (2 * Math.PI * i) / points;
                        Location loc = center.clone().add(Math.cos(angle) * 14, 0.2, Math.sin(angle) * 14);
                        DisplayBuilder.dustParticles(loc, 3, 0.3, 190, 25, 10, 0.7f);
                    }
                }
                return;
            }

            // Activate halo
            if (!haloActive) {
                haloActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);

                // Spawn magma block ring displays
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI * i) / 16;
                    Location ringLoc = center.clone().add(Math.cos(angle) * 14, 0.05, Math.sin(angle) * 14);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                    h.scale(2.0f, 0.2f, 2.0f).glow(200, 0, 50).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                }
            }

            // Ring particles
            if (ticksAlive % 3 == 0) {
                int points = 32;
                for (int i = 0; i < points; i++) {
                    double angle = (2 * Math.PI * i) / points;
                    for (double r = 13; r <= 15; r += 0.5) {
                        Location loc = center.clone().add(Math.cos(angle) * r, Math.random() * 6, Math.sin(angle) * r);
                        if (r < 13.5) {
                            DisplayBuilder.dustParticles(loc, 2, 0.2, 255, 110, 0, 0.8f);
                        } else {
                            DisplayBuilder.dustParticles(loc, 2, 0.2, 0, 230, 220, 0.7f);
                        }
                    }

                    // Dripping lava over outer edge
                    if (i % 4 == 0) {
                        Location dripLoc = center.clone().add(Math.cos(angle) * 15.5, 3, Math.sin(angle) * 15.5);
                        w.spawnParticle(Particle.DRIPPING_LAVA, dripLoc, 2, 0.2, 1.0, 0.2, 0);
                    }
                }

                // Inner-edge lava fountains reaching 6 blocks inward at 45 degrees
                if (ticksAlive % 6 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (2 * Math.PI * i) / 12 + (ticksAlive * 0.02);
                        for (double d = 0; d < 6; d += 1.0) {
                            double height = (6 - d) * 0.7; // 45-degree arc
                            Location arcLoc = center.clone().add(
                                    Math.cos(angle) * (14 - d), height, Math.sin(angle) * (14 - d));
                            w.spawnParticle(Particle.LAVA, arcLoc, 1, 0.2, 0.2, 0.2, 0);
                        }
                    }
                }
            }

            // Damage: ring band + fountain arcs
            if (ticksAlive % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);

                    // Ring band (13-15 blocks)
                    if (dist >= 13 && dist <= 15) {
                        p.damage(12.0); // 6 hearts
                        DisplayBuilder.crimsonDust(p.getLocation(), 6, 0.4);
                    }
                    // Fountain arc zone (8-14 blocks)
                    else if (dist >= 8 && dist <= 14) {
                        // Fountain arcs reaching 6 blocks inward
                        double height = p.getLocation().getY() - center.getY();
                        double expectedHeight = (14 - dist) * 0.7;
                        if (Math.abs(height - expectedHeight) < 2.0) {
                            p.damage(20.0); // 10 hearts fountain arc
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SuperheatedHalo(plugin); }
    }

    // =========================================================================
    // 80. CONFLAGRATION PULSE -- catastrophic synchronized eruption, all players hit
    // =========================================================================
    public static class ConflagrationPulse extends EnvironmentalAttack {

        private boolean pulsed = false;

        public ConflagrationPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("conflagration_pulse", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80); // short but devastating
            config.setCooldownTicks(500); // 25 seconds -- transition marker
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            // All sounds cut to silence for 2 seconds -- no sound on spawn
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2 seconds) of total silence
            if (ticksAlive <= 40) {
                return;
            }

            // THE PULSE
            if (!pulsed) {
                pulsed = true;

                // ALL sounds simultaneously at max volume
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 2.0f, 0.5f);

                // Maximum particle output across entire island
                for (int i = 0; i < 60; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 15;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 0.2, Math.sin(angle) * dist);

                    w.spawnParticle(Particle.LAVA, loc.clone().add(0, Math.random() * 8, 0), 5, 0.3, 2.0, 0.3, 0);
                    w.spawnParticle(Particle.DRIPPING_LAVA, loc.clone().add(0, 4, 0), 3, 0.5, 2.0, 0.5, 0);
                    w.spawnParticle(Particle.SMOKE, loc.clone().add(0, 6, 0), 4, 0.5, 2.0, 0.5, 0.03);
                    DisplayBuilder.dustParticles(loc, 3, 0.5, 255, 100, 0, 1.2f);
                    DisplayBuilder.dustParticles(loc.clone().add(0, 2, 0), 3, 0.5, 0, 255, 200, 1.0f);
                    DisplayBuilder.dustParticles(loc.clone().add(0, 1, 0), 3, 0.5, 200, 0, 50, 0.8f);
                }

                // Soul particles raining down
                for (int i = 0; i < 30; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 15;
                    Location loc = center.clone().add(Math.cos(angle) * dist, 8, Math.sin(angle) * dist);
                    DisplayBuilder.dustParticles(loc, 4, 0.5, 0, 255, 240, 0.8f);
                }

                // Pulse displays to max scale
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8;
                    double dist = 5 + Math.random() * 8;
                    Location dLoc = center.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(dLoc, Material.MAGMA_BLOCK);
                    h.scale(3.0f, 0.2f, 3.0f).glow(200, 0, 50).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                }

                // 12 hearts to ALL players -- unavoidable
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    p.damage(24.0); // 12 hearts
                    DisplayBuilder.crimsonDust(p.getLocation(), 15, 1.0);
                    DisplayBuilder.dustParticles(p.getLocation().clone().add(0, 1, 0), 10, 0.5, 255, 100, 0, 1.2f);
                }
            }

            // Sustained maximum particle output for 2 seconds (40 ticks)
            int pulseTick = ticksAlive - 40;
            if (pulseTick > 0 && pulseTick <= 40) {
                if (pulseTick % 2 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        double dist = Math.random() * 15;
                        Location loc = center.clone().add(Math.cos(angle) * dist, Math.random() * 8, Math.sin(angle) * dist);
                        DisplayBuilder.dustParticles(loc, 2, 0.3, 255, 100, 0, 0.8f);
                        w.spawnParticle(Particle.LAVA, loc, 2, 0.3, 0.5, 0.3, 0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConflagrationPulse(plugin); }
    }
}
