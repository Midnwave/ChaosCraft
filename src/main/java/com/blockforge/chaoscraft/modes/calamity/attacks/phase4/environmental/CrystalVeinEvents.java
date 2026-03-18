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
 * Phase 4D Environmental -- GROUP 2: CRYSTAL VEIN EVENTS
 * Attacks #11-20: DoG's crystalline plague legacy -- spines, rain, cage traps,
 * pulsing veins, detonation clusters, creeping spread, and scar resonance.
 *
 * Design notes:
 * - No status effects
 * - Crystal palette: magenta (180,0,200), purple (128,0,255), orchid (220,100,255)
 * - Materials: AMETHYST_BLOCK, AMETHYST_CLUSTER, CALCITE, PURPUR_BLOCK, OBSIDIAN
 * - Damage range: 6.0-14.0 HP
 */
public final class CrystalVeinEvents {

    private CrystalVeinEvents() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrystalSpineEruption(plugin));
        registry.register(new CrystalRain(plugin));
        registry.register(new CrystalCageTrap(plugin));
        registry.register(new PulsingCrystalVein(plugin));
        registry.register(new CrystalDetonationCluster(plugin));
        registry.register(new CreepingCrystalSpread(plugin));
        registry.register(new DoGScarResonance(plugin));
        registry.register(new BrimstoneVentIgnition(plugin));
        registry.register(new SoulFireColumnArray(plugin));
        registry.register(new MagmaSurfaceEruption(plugin));
    }

    // =========================================================================
    // 11. CRYSTAL SPINE ERUPTION -- amethyst spires erupt from the ground
    // =========================================================================
    public static class CrystalSpineEruption extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> spireHandles = new ArrayList<>();
        private final List<Location> spireLocations = new ArrayList<>();
        private Location groundGlow;
        private boolean spiresFired = false;
        private int spireCount;

        public CrystalSpineEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_spine_eruption", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            spireCount = 5 + (int) (Math.random() * 5); // 5-9
            groundGlow = center.clone();

            // Ground glow warning
            DisplayBuilder.dustParticles(groundGlow, 15, 2.5, 180, 0, 255, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks -- ground glow intensifies
            if (ticksAlive <= 40) {
                int density = 10 + ticksAlive;
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(groundGlow.clone().add(0, 0.3, 0), density / 5, 2.5,
                            180, 0, 255, 1.0f);
                }
                if (ticksAlive == 20) {
                    DisplayBuilder.playSound(groundGlow, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 0.6f);
                }
                if (ticksAlive == 35) {
                    // Cracking ring
                    w.spawnParticle(Particle.BLOCK, groundGlow, 30, 1.0, 0.3, 1.0, 0,
                            Material.AMETHYST_BLOCK.createBlockData());
                }
                return;
            }

            // Erupt spires staggered: one every 4 ticks
            if (!spiresFired) {
                spiresFired = true;

                // Generate spire locations
                for (int i = 0; i < spireCount; i++) {
                    double a = Math.random() * 2 * Math.PI;
                    double d = Math.random() * 12;
                    spireLocations.add(groundGlow.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
                }
            }

            int eruptTick = ticksAlive - 40;

            // Staggered eruption
            for (int i = 0; i < spireCount; i++) {
                int spireStart = i * 4;
                if (eruptTick < spireStart) continue;
                int spireLocalTick = eruptTick - spireStart;

                // Eruption phase: 15 ticks
                if (spireLocalTick == 0) {
                    Location loc = spireLocations.get(i);
                    int spireHeight = new int[]{4, 5, 6, 5, 7, 4, 6, 5, 8}[i % 9];

                    // Spawn spire blocks
                    for (int y = 0; y < spireHeight; y++) {
                        Location blockLoc = loc.clone().add(0, y - 4, 0); // Start from below
                        Material mat = (y == spireHeight - 1) ? Material.AMETHYST_CLUSTER : Material.AMETHYST_BLOCK;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(blockLoc, mat);
                        float rotation = (float) (Math.random() * 0.26); // 5-15 degree twist
                        h.scale(0.8f, 1.0f, 0.8f).glow(180, 0, 200).interpolation(3, 0);
                        h.rotate(rotation, 0, 1, 0);
                        spireHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }

                    // Eruption particles + sound
                    w.spawnParticle(Particle.BLOCK, loc, 40, 0.5, 1.0, 0.5, 0,
                            Material.AMETHYST_BLOCK.createBlockData());
                    DisplayBuilder.dustParticles(loc, 15, 0.8, 200, 0, 255, 1.2f);
                    float pitch = 0.6f + (float) (Math.random() * 0.6f);
                    DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.9f, pitch);

                    // Eruption damage: 10 HP
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(loc) <= 4.0) {
                            p.damage(10.0);
                            p.setVelocity(p.getVelocity().setY(0.6)); // Levitation-like launch
                        }
                    }
                }

                // Standing spire: enchant at tip
                if (spireLocalTick > 15 && spireLocalTick % 10 == 0) {
                    Location loc = spireLocations.get(i);
                    w.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 5, 0), 3, 0.3, 0.3, 0.3, 0.5);
                }

                // Contact damage: 4 HP per second on standing spires
                if (spireLocalTick > 15 && spireLocalTick % 20 == 0) {
                    Location loc = spireLocations.get(i);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(loc) <= 2.25) {
                            p.damage(4.0);
                        }
                    }
                }
            }

            // Ambient spore blossom particles across eruption area
            if (eruptTick % 8 == 0) {
                w.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, groundGlow.clone().add(0, 3, 0),
                        5, 6.0, 1.0, 6.0, 0);
            }

            // Resonance loop while spires present
            if (eruptTick % 100 == 0) {
                DisplayBuilder.playSound(groundGlow, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.3f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalSpineEruption(plugin); }
    }

    // =========================================================================
    // 12. CRYSTAL RAIN -- 20-30 small amethyst shards fall from sky
    // =========================================================================
    public static class CrystalRain extends EnvironmentalAttack {

        private final List<Location> shardTargets = new ArrayList<>();
        private final List<Double> shardSpeeds = new ArrayList<>();
        private boolean shardsLaunched = false;
        private int shardCount;

        public CrystalRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_rain", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(999);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(1.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            shardCount = 20 + (int) (Math.random() * 11); // 20-30

            // Shimmer overhead
            w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 60, 0), 40, 15, 3, 15, 1.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 40 ticks -- enchant particles gather overhead
            if (ticksAlive <= 40) {
                if (ticksAlive % 5 == 0) {
                    w.spawnParticle(Particle.ENCHANT, center.clone().add(0, 60, 0),
                            20 + ticksAlive, 12, 2, 12, 1.0);
                }
                return;
            }

            // Launch shards
            if (!shardsLaunched) {
                shardsLaunched = true;

                // Generate targets: 60% near players, 40% random
                List<Player> players = new ArrayList<>(w.getPlayers());
                players.removeIf(this::isExempt);

                for (int i = 0; i < shardCount; i++) {
                    Location target;
                    if (Math.random() < 0.6 && !players.isEmpty()) {
                        Player p = players.get((int) (Math.random() * players.size()));
                        target = p.getLocation().clone().add(
                                (Math.random() - 0.5) * 10, 0, (Math.random() - 0.5) * 10);
                    } else {
                        double a = Math.random() * 2 * Math.PI;
                        double d = Math.random() * 25;
                        target = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                    }
                    shardTargets.add(target);
                    shardSpeeds.add(2.5 + Math.random()); // 2.5-3.5 blocks/tick
                }
            }

            int fallTick = ticksAlive - 40;

            // Shards falling
            for (int i = 0; i < shardTargets.size(); i++) {
                Location target = shardTargets.get(i);
                double speed = shardSpeeds.get(i);
                double currentY = 60 - (fallTick * speed);

                if (currentY <= 0 && currentY > -speed) {
                    // Landing: burst + damage
                    w.spawnParticle(Particle.BLOCK, target, 15, 0.5, 0.3, 0.5, 0,
                            Material.AMETHYST_BLOCK.createBlockData());
                    w.spawnParticle(Particle.CRIT, target.clone().add(0, 0.5, 0), 8, 0.3, 0.3, 0.3, 0.1);
                    DisplayBuilder.playSound(target, Sound.BLOCK_CALCITE_BREAK, 0.5f, 1.4f);

                    triggerImpactDamage(target);

                    // Occasional permanent shard debris (3-5 total)
                    if (i < 4 && Math.random() < 0.5) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(target.clone().add(0, 0.02, 0),
                                Material.AMETHYST_CLUSTER);
                        h.scale(0.4f, 0.4f, 0.4f).glow(180, 0, 200).interpolation(3, 0);
                        h.rotate((float) (Math.random() * Math.PI * 2), 0, 1, 0);
                        spawnedEntities.add(h.entity());
                    }
                } else if (currentY > 0) {
                    // Falling trail
                    if (fallTick % 2 == 0) {
                        Location shardPos = target.clone().add(0, currentY, 0);
                        w.spawnParticle(Particle.CRIT, shardPos, 2, 0.1, 0.1, 0.1, 0.01);
                        DisplayBuilder.dustParticles(shardPos, 2, 0.2, 220, 100, 255, 0.8f);
                    }
                }
            }

            // Rapid crystalline rain sound
            if (fallTick % 2 == 0 && fallTick <= 25) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.3f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalRain(plugin); }
    }

    // =========================================================================
    // 13. CRYSTAL CAGE TRAP -- 12-spire crystal cage around highest-threat player
    // =========================================================================
    public static class CrystalCageTrap extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> cageSpires = new ArrayList<>();
        private Location cageCenter;
        private boolean cageFormed = false;

        public CrystalCageTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_cage_trap", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1600);
            config.setTicksBetweenDamage(999);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Target player position
            Player target = getTargetPlayer();
            cageCenter = (target != null) ? target.getLocation().clone() : center.clone();

            // Subtle glow under feet
            DisplayBuilder.dustParticles(cageCenter, 10, 1.5, 160, 0, 255, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks -- glow intensifies under target
            if (ticksAlive <= 40) {
                int density = 10 + ticksAlive * 2;
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(cageCenter, density / 5, 1.5, 160, 0, 255, 1.5f);
                }
                if (ticksAlive == 30) {
                    // Cardinal placement sounds
                    for (int i = 0; i < 4; i++) {
                        double a = (Math.PI / 2) * i;
                        Location snd = cageCenter.clone().add(Math.cos(a) * 3, 0, Math.sin(a) * 3);
                        DisplayBuilder.playSound(snd, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.7f, 0.8f);
                    }
                }
                return;
            }

            // Cage eruption
            if (!cageFormed) {
                cageFormed = true;

                // 12 spires: 4 cardinal (3 blocks), 4 diagonal (4 blocks), 4 more cardinal (5 blocks)
                double[][] positions = new double[12][2];
                for (int i = 0; i < 4; i++) {
                    double a = (Math.PI / 2) * i;
                    positions[i] = new double[]{Math.cos(a) * 3, Math.sin(a) * 3};
                    positions[i + 4] = new double[]{Math.cos(a + Math.PI / 4) * 4, Math.sin(a + Math.PI / 4) * 4};
                    positions[i + 8] = new double[]{Math.cos(a) * 5, Math.sin(a) * 5};
                }

                for (int i = 0; i < 12; i++) {
                    Location spireBase = cageCenter.clone().add(positions[i][0], 0, positions[i][1]);

                    // 4-block spire + cluster tip
                    for (int y = 0; y < 5; y++) {
                        Material mat = (y == 4) ? Material.AMETHYST_CLUSTER : Material.AMETHYST_BLOCK;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                spireBase.clone().add(0, y, 0), mat);
                        float twist = (float) (y * 0.14); // ~8 degrees per block
                        h.scale(0.9f, 1.0f, 0.9f).glow(200, 50, 255).interpolation(3, 0);
                        h.rotate(twist, 0, 1, 0);
                        cageSpires.add(h);
                        spawnedEntities.add(h.entity());
                    }

                    // Eruption damage
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(spireBase) <= 2.25) {
                            p.damage(8.0);
                            p.setVelocity(p.getVelocity().setY(0.5));
                        }
                    }
                }

                // Rapid placement sounds
                DisplayBuilder.playSound(cageCenter, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.9f, 0.7f);

                // Trapped stare sound
                DisplayBuilder.playSound(cageCenter, Sound.ENTITY_ENDERMAN_STARE, 0.3f, 0.5f);
            }

            int cageTick = ticksAlive - 40;

            // Cage active: 300 ticks (15 seconds)
            if (cageTick > 300) return;

            // Particle curtains between spires
            if (cageTick % 4 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = (2 * Math.PI * i) / 12;
                    double r = (i < 4) ? 3 : ((i < 8) ? 4 : 5);
                    Location spireTop = cageCenter.clone().add(Math.cos(a) * r, 5, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(spireTop, 3, 0.5, 200, 50, 255, 1.0f);
                }

                // Interior reverse portal
                w.spawnParticle(Particle.REVERSE_PORTAL, cageCenter.clone().add(0, 0.5, 0),
                        5, 2.0, 0.3, 2.0, 0.01);
            }

            // Resonance loop
            if (cageTick % 80 == 0) {
                DisplayBuilder.playSound(cageCenter, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, 0.3f);
            }

            // Gap crossing damage: 6 HP per crossing
            if (cageTick % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - cageCenter.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - cageCenter.getZ(), 2));
                    if (dist >= 2.5 && dist <= 5.5) {
                        // In the spire ring zone
                        p.damage(6.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
            if (cageCenter != null) {
                DisplayBuilder.playSound(cageCenter, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.6f);
            }
        }

        @Override
        public AbstractAttack newInstance() { return new CrystalCageTrap(plugin); }
    }

    // =========================================================================
    // 14. PULSING CRYSTAL VEIN -- traveling pulse along ground-level crystal vein
    // =========================================================================
    public static class PulsingCrystalVein extends EnvironmentalAttack {

        private Location veinStart;
        private Location veinEnd;
        private double veinAngle;
        private int pulseDirection = 1; // 1 = forward, -1 = backward
        private double pulseProgress = 0;
        private int bounceCount = 0;
        private double intensityMultiplier = 1.0;

        public PulsingCrystalVein(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pulsing_crystal_vein", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            veinAngle = Math.random() * Math.PI; // diagonal line
            veinStart = center.clone().add(Math.cos(veinAngle) * -7.5, 0, Math.sin(veinAngle) * -7.5);
            veinEnd = center.clone().add(Math.cos(veinAngle) * 7.5, 0, Math.sin(veinAngle) * 7.5);

            // Passive glow along vein
            for (int i = -7; i <= 7; i++) {
                Location loc = center.clone().add(Math.cos(veinAngle) * i, 0.1, Math.sin(veinAngle) * i);
                DisplayBuilder.dustParticles(loc, 2, 0.2, 180, 80, 200, 0.4f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 20 ticks -- vein brightens
            if (ticksAlive <= 20) {
                int density = 5 + ticksAlive;
                if (ticksAlive % 4 == 0) {
                    for (int i = -7; i <= 7; i++) {
                        Location loc = center.clone().add(Math.cos(veinAngle) * i, 0.2, Math.sin(veinAngle) * i);
                        DisplayBuilder.dustParticles(loc, density / 10, 0.3, 220, 150, 255, 0.6f);
                    }
                }
                return;
            }

            // Pulse traveling
            pulseProgress += 1.5 * pulseDirection;

            // Bounce at ends
            if (pulseProgress >= 15 || pulseProgress <= -15) {
                pulseDirection *= -1;
                bounceCount++;
                intensityMultiplier *= 0.7;
                if (bounceCount >= 3) return; // Done after 3 bounces
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.8f);
            }

            // Pulse front position
            double pulseX = center.getX() + Math.cos(veinAngle) * pulseProgress;
            double pulseZ = center.getZ() + Math.sin(veinAngle) * pulseProgress;
            Location pulseLoc = new Location(w, pulseX, center.getY() + 0.2, pulseZ);

            // Pulse particles
            if (ticksAlive % 2 == 0) {
                int particleCount = (int) (15 * intensityMultiplier);
                DisplayBuilder.dustParticles(pulseLoc, particleCount, 1.0, 255, 100, 255, 2.5f);
                w.spawnParticle(Particle.ENCHANT, pulseLoc.clone().add(0, 0.5, 0),
                        (int) (8 * intensityMultiplier), 0.5, 0.3, 0.5, 0.5);
            }

            // Sound at pulse position
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.playSound(pulseLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.6f, 1.2f);
            }

            // Damage players near pulse
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                Location pl = p.getLocation();
                double dx = pl.getX() - pulseLoc.getX();
                double dz = pl.getZ() - pulseLoc.getZ();
                // Distance to pulse point
                if (dx * dx + dz * dz <= 4.0) { // 2 block radius
                    p.damage(4.0);
                    // Knockback perpendicular to vein
                    double perpX = -Math.sin(veinAngle);
                    double perpZ = Math.cos(veinAngle);
                    double side = (dx * perpX + dz * perpZ >= 0) ? 1.0 : -1.0;
                    p.setVelocity(p.getVelocity().add(
                            new org.bukkit.util.Vector(perpX * side * 0.5, 0.15, perpZ * side * 0.5)));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PulsingCrystalVein(plugin); }
    }

    // =========================================================================
    // 15. CRYSTAL DETONATION CLUSTER -- 8 formations detonate sequentially
    // =========================================================================
    public static class CrystalDetonationCluster extends EnvironmentalAttack {

        private final List<Location> formationLocs = new ArrayList<>();
        private boolean detonationStarted = false;
        private int detonationIndex = 0;
        private int detonationTick = 0;

        public CrystalDetonationCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_detonation_cluster", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(99999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 formation positions spread across arena
            for (int i = 0; i < 8; i++) {
                double a = (2 * Math.PI * i) / 8;
                double d = 10 + Math.random() * 8;
                formationLocs.add(center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }

            // All formations glow simultaneously
            for (Location loc : formationLocs) {
                DisplayBuilder.dustParticles(loc, 15, 1.5, 255, 200, 255, 2.0f);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Buildup: 80 ticks -- formations glow + beams rise
            if (ticksAlive <= 80) {
                float risingPitch = 0.3f + (ticksAlive / 80.0f) * 0.9f;
                if (ticksAlive % 10 == 0) {
                    for (Location loc : formationLocs) {
                        DisplayBuilder.dustParticles(loc, 10, 1.0, 255, 200, 255, 2.0f);
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, risingPitch);
                }
                if (ticksAlive >= 60 && ticksAlive % 5 == 0) {
                    for (Location loc : formationLocs) {
                        w.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 5, 0), 10, 0.3, 5, 0.3, 1.0);
                    }
                }
                return;
            }

            // Sequential detonation: one every 8 ticks
            if (!detonationStarted) {
                detonationStarted = true;
                detonationTick = 0;
            }

            detonationTick++;

            if (detonationIndex < 8 && detonationTick % 8 == 0) {
                Location detLoc = formationLocs.get(detonationIndex);

                // Detonation explosion
                w.spawnParticle(Particle.BLOCK, detLoc, 80, 2.5, 1.5, 2.5, 0,
                        Material.AMETHYST_BLOCK.createBlockData());
                DisplayBuilder.dustParticles(detLoc, 30, 2.5, 255, 0, 200, 3.0f);
                w.spawnParticle(Particle.CRIT, detLoc.clone().add(0, 1, 0), 25, 2.0, 1.0, 2.0, 0.2);
                DisplayBuilder.playSound(detLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.4f);

                // Fragment debris
                for (int f = 0; f < 5; f++) {
                    double fa = Math.random() * 2 * Math.PI;
                    double fd = 2 + Math.random() * 4;
                    Location fragLoc = detLoc.clone().add(Math.cos(fa) * fd, 0.02, Math.sin(fa) * fd);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(fragLoc, Material.AMETHYST_CLUSTER);
                    h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(5, 0);
                    spawnedEntities.add(h.entity());
                }

                // Damage: 16 HP close, 8 HP far
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distanceSquared(detLoc);
                    if (dist <= 4.0) {
                        p.damage(14.0); // 7 hearts close (capped at design range)
                    } else if (dist <= 25.0) {
                        p.damage(8.0); // 4 hearts splash
                    }
                }

                // Next indicator
                if (detonationIndex + 1 < 8) {
                    Location next = formationLocs.get(detonationIndex + 1);
                    DisplayBuilder.dustParticles(next, 20, 1.0, 255, 255, 0, 2.0f);
                    DisplayBuilder.playSound(next, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.7f, 0.8f);
                }

                detonationIndex++;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalDetonationCluster(plugin); }
    }

    // =========================================================================
    // 16. CREEPING CRYSTAL SPREAD -- passive crystal growths accumulate
    // =========================================================================
    public static class CreepingCrystalSpread extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> growthHandles = new ArrayList<>();

        public CreepingCrystalSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("creeping_crystal_spread", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Find valid spot not near players
            double a = Math.random() * 2 * Math.PI;
            double d = 5 + Math.random() * 18;
            Location growthLoc = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);

            boolean isLargeGrowth = Math.random() < 0.1; // 10% chance tall spire

            if (isLargeGrowth) {
                // 3-block spire
                for (int y = 0; y < 3; y++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            growthLoc.clone().add(0, y * 0.1, 0), Material.AMETHYST_BLOCK);
                    h.scale(0.1f, 0.1f, 0.1f).glow(180, 0, 200).interpolation(40, 0);
                    growthHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            } else {
                Material mat = Math.random() < 0.5 ? Material.AMETHYST_CLUSTER : Material.AMETHYST_BLOCK;
                float scale = (mat == Material.AMETHYST_BLOCK) ? 0.5f : 1.0f;
                BlockDisplayHandle h = displayBuilder.spawnBlock(growthLoc.clone().add(0, 0.02, 0), mat);
                h.scale(0.1f, 0.1f, 0.1f).glow(180, 0, 200).interpolation(40, 0);
                growthHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            float pitch = 0.7f + (float) (Math.random() * 0.5f);
            DisplayBuilder.playSound(growthLoc, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.4f, pitch);

            // Spore blossom during growth
            w.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, growthLoc.clone().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            // Growth animation: scale up over 40 ticks
            if (ticksAlive == 5) {
                for (BlockDisplayHandle h : growthHandles) {
                    h.scale(1.0f, 1.0f, 1.0f);
                    h.interpolation(35, 0);
                }
            }

            // Passive dust once grown
            if (ticksAlive == 45 && !growthHandles.isEmpty()) {
                Location loc = growthHandles.get(0).entity().getLocation();
                DisplayBuilder.dustParticles(loc, 3, 0.3, 180, 0, 200, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { /* Growths persist -- do not removeAll */ }

        @Override
        public AbstractAttack newInstance() { return new CreepingCrystalSpread(plugin); }
    }

    // =========================================================================
    // 17. DOG SCAR RESONANCE -- crystal scars vibrate and fire interference nodes
    // =========================================================================
    public static class DoGScarResonance extends EnvironmentalAttack {

        private final List<Location> scarPositions = new ArrayList<>();
        private final List<Location> nodePositions = new ArrayList<>();
        private boolean resonanceFired = false;

        public DoGScarResonance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_scar_resonance", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(10.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(1500);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Generate scar positions (legacy DoG crystal scars)
            for (int i = 0; i < 6; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 5 + Math.random() * 15;
                scarPositions.add(center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Tremor phase: 40 ticks -- scars vibrate
            if (ticksAlive <= 40) {
                if (ticksAlive % 2 == 0) {
                    for (Location scar : scarPositions) {
                        // Rapid buzzing sound
                        DisplayBuilder.playSound(scar, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.2f, 1.6f);

                        // Micro particles
                        double shake = (ticksAlive % 4 < 2) ? 0.08 : -0.08;
                        DisplayBuilder.dustParticles(scar.clone().add(shake, 0.3, shake), 3, 0.2,
                                220, 100, 255, 1.5f);
                    }
                }
                return;
            }

            // Resonance fires: calculate interference nodes
            if (!resonanceFired) {
                resonanceFired = true;

                // Burst from all formations
                for (Location scar : scarPositions) {
                    w.spawnParticle(Particle.ENCHANT, scar.clone().add(0, 1, 0), 40, 0.5, 1.0, 0.5, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.5f);

                // Calculate interference nodes (grid intersections)
                for (int i = 0; i < scarPositions.size(); i++) {
                    for (int j = i + 1; j < scarPositions.size(); j++) {
                        Location mid = scarPositions.get(i).clone().add(scarPositions.get(j)).multiply(0.5);
                        mid.setY(center.getY());
                        nodePositions.add(mid);
                    }
                }
            }

            int resTick = ticksAlive - 40;

            // Node warnings: 2 ticks
            if (resTick <= 2) {
                for (Location node : nodePositions) {
                    w.spawnParticle(Particle.CRIT, node.clone().add(0, 0.5, 0), 10, 0.5, 0.5, 0.5, 0.1);
                }
                return;
            }

            // Node damage: 5 ticks
            if (resTick <= 7) {
                if (resTick == 3) {
                    for (Location node : nodePositions) {
                        w.spawnParticle(Particle.EXPLOSION, node.clone().add(0, 0.5, 0), 8, 0.8, 0.5, 0.8, 0);
                        DisplayBuilder.playSound(node, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.4f, 1.8f);
                    }
                }

                // Damage at nodes
                if (resTick % 2 == 0) {
                    for (Location node : nodePositions) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(node) <= 2.25) {
                                p.damage(10.0);
                            }
                        }
                    }
                }
            }

            // Post-event: scar dim glow
            if (resTick > 7 && resTick % 10 == 0) {
                for (Location scar : scarPositions) {
                    w.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, scar.clone().add(0, 1, 0), 2, 0.5, 0.5, 0.5, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoGScarResonance(plugin); }
    }

    // =========================================================================
    // 18. BRIMSTONE VENT IGNITION -- lava geyser columns erupt from vent points
    // =========================================================================
    public static class BrimstoneVentIgnition extends EnvironmentalAttack {

        private final List<Location> ventLocations = new ArrayList<>();
        private final List<BlockDisplayHandle> ventDisplays = new ArrayList<>();
        private boolean ventsErupted = false;

        public BrimstoneVentIgnition(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_vent_ignition", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(8.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            int ventCount = 4 + (int) (Math.random() * 4); // 4-7

            for (int i = 0; i < ventCount; i++) {
                double a = Math.random() * 2 * Math.PI;
                double d = 5 + Math.random() * 15;
                Location ventLoc = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                ventLocations.add(ventLoc);

                // Permanent vent marker
                BlockDisplayHandle vent = displayBuilder.spawnBlock(ventLoc, Material.NETHERRACK);
                vent.scale(1.0f, 0.2f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                ventDisplays.add(vent);
                spawnedEntities.add(vent.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks -- smoke thickens from vents
            if (ticksAlive <= 40) {
                if (ticksAlive % 4 == 0) {
                    for (Location vent : ventLocations) {
                        w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, vent.clone().add(0, 0.5, 0),
                                3 + ticksAlive / 5, 0.3, 1.0, 0.3, 0.01);
                    }
                }
                if (ticksAlive >= 25 && ticksAlive % 3 == 0) {
                    for (Location vent : ventLocations) {
                        w.spawnParticle(Particle.LAVA, vent.clone().add(0, 0.5, 0), 2, 0.2, 0, 0.2, 0);
                    }
                }
                if (ticksAlive == 35) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.5f);
                    for (Location vent : ventLocations) {
                        DisplayBuilder.playSound(vent, Sound.BLOCK_NETHERRACK_BREAK, 0.6f, 0.6f);
                    }
                }
                return;
            }

            // Geyser eruption
            if (!ventsErupted) {
                ventsErupted = true;
                for (Location vent : ventLocations) {
                    DisplayBuilder.playSound(vent, Sound.ENTITY_GHAST_SHOOT, 0.8f, 0.6f);

                    // Orange glass column displays
                    for (int y = 1; y <= 6; y++) {
                        BlockDisplayHandle col = displayBuilder.spawnBlock(
                                vent.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                        col.scale(0.5f, 1.0f, 0.5f).glow(255, 100, 0).interpolation(3, 0);
                        spawnedEntities.add(col.entity());
                    }
                }
            }

            int geyserTick = ticksAlive - 40;

            // Geyser columns active: 60 ticks
            if (geyserTick <= 60) {
                if (geyserTick % 2 == 0) {
                    for (Location vent : ventLocations) {
                        // Fire/lava/smoke column
                        for (int y = 0; y <= 12; y += 3) {
                            Location colLoc = vent.clone().add(0, y, 0);
                            w.spawnParticle(Particle.FLAME, colLoc, 6, 0.3, 0.5, 0.3, 0.02);
                            w.spawnParticle(Particle.LAVA, colLoc, 2, 0.2, 0.5, 0.2, 0);
                            w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, colLoc, 3, 0.3, 0.5, 0.3, 0.01);
                        }
                        // Base explosion particles
                        w.spawnParticle(Particle.EXPLOSION, vent.clone().add(0, 0.5, 0), 1, 0.3, 0.2, 0.3, 0);
                    }
                }

                // Fire ambient loop
                if (geyserTick % 40 == 0) {
                    for (Location vent : ventLocations) {
                        DisplayBuilder.playSound(vent, Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.7f);
                    }
                }

                // Damage + launch
                if (geyserTick % 20 == 0) {
                    for (Location vent : ventLocations) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - vent.getX();
                            double dz = p.getLocation().getZ() - vent.getZ();
                            if (dx * dx + dz * dz <= 2.25 && p.getLocation().getY() <= vent.getY() + 12) {
                                p.damage(8.0);
                                p.setVelocity(p.getVelocity().setY(0.8)); // Launch upward
                            }
                        }
                    }
                }
            }

            // Gutter out: 20 ticks
            if (geyserTick > 60 && geyserTick <= 80) {
                double fadeRatio = 1.0 - ((geyserTick - 60) / 20.0);
                if (geyserTick % 4 == 0) {
                    for (Location vent : ventLocations) {
                        int count = (int) (4 * fadeRatio);
                        w.spawnParticle(Particle.FLAME, vent.clone().add(0, 3, 0), count, 0.3, 2.0, 0.3, 0.01);
                    }
                }
                if (geyserTick == 80) {
                    for (Location vent : ventLocations) {
                        DisplayBuilder.playSound(vent, Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 0.8f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneVentIgnition(plugin); }
    }

    // =========================================================================
    // 19. SOUL FIRE COLUMN ARRAY -- blue-white flame columns in diagonal line
    // =========================================================================
    public static class SoulFireColumnArray extends EnvironmentalAttack {

        private final List<Location> columnLocations = new ArrayList<>();
        private boolean columnsErupted = false;

        public SoulFireColumnArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_column_array", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(6.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            int colCount = 3 + (int) (Math.random() * 3); // 3-5
            double lineAngle = Math.random() * Math.PI; // diagonal

            // Columns along a diagonal line with 3-block gaps
            for (int i = 0; i < colCount; i++) {
                double offset = (i - colCount / 2.0) * 6;
                Location colLoc = center.clone().add(Math.cos(lineAngle) * offset, 0, Math.sin(lineAngle) * offset);
                columnLocations.add(colLoc);
            }

            // Passive soul fire ambient
            for (Location col : columnLocations) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, col.clone().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 0.01);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks -- precursor flames rise
            if (ticksAlive <= 40) {
                double precursorHeight = Math.min(ticksAlive * 0.15, 6);
                if (ticksAlive % 3 == 0) {
                    for (Location col : columnLocations) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, col.clone().add(0, precursorHeight / 2, 0),
                                5, 0.2, precursorHeight / 2, 0.2, 0.01);
                    }
                }
                if (ticksAlive == 30) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.4f, 0.4f);
                }
                return;
            }

            // Columns erupt to Y+18
            if (!columnsErupted) {
                columnsErupted = true;
                for (Location col : columnLocations) {
                    DisplayBuilder.playSound(col, Sound.ENTITY_WITHER_SHOOT, 0.7f, 0.5f);

                    // Soul sand core display at midpoint
                    BlockDisplayHandle core = displayBuilder.spawnBlock(
                            col.clone().add(0, 9, 0), Material.DARK_PRISMARINE);
                    core.scale(0.6f, 0.6f, 0.6f).glow(0, 200, 255).interpolation(5, 0);
                    spawnedEntities.add(core.entity());
                }
            }

            int colTick = ticksAlive - 40;

            // Columns hold: 80 ticks
            if (colTick <= 80) {
                if (colTick % 2 == 0) {
                    for (Location col : columnLocations) {
                        for (int y = 0; y <= 18; y += 3) {
                            Location flameLoc = col.clone().add(0, y, 0);
                            w.spawnParticle(Particle.SOUL_FIRE_FLAME, flameLoc, 8, 0.5, 0.5, 0.5, 0.01);
                            if (y >= 8 && y <= 14) {
                                DisplayBuilder.dustParticles(flameLoc, 3, 0.5, 100, 150, 255, 1.4f);
                            }
                        }
                        // Ash from top
                        w.spawnParticle(Particle.WHITE_ASH, col.clone().add(0, 18, 0), 3, 0.5, 0.5, 0.5, 0.01);
                        // Sculk soul at base
                        w.spawnParticle(Particle.SCULK_SOUL, col.clone().add(0, 0.5, 0), 1, 0.5, 0.3, 0.5, 0.01);
                    }
                }

                // Ambient loop
                if (colTick % 40 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_BREAK, 0.5f, 0.8f);
                }

                // Damage + Wither-like effect
                if (colTick % 20 == 0) {
                    for (Location col : columnLocations) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double dx = p.getLocation().getX() - col.getX();
                            double dz = p.getLocation().getZ() - col.getZ();
                            if (dx * dx + dz * dz <= 4.0 && p.getLocation().getY() <= col.getY() + 18) {
                                p.damage(6.0); // 3 hearts/sec
                            }
                        }
                    }
                }
            }

            // Deflation: top descends 1 block/tick
            if (colTick > 80 && colTick <= 98) {
                double topY = 18 - (colTick - 80);
                if (colTick % 2 == 0) {
                    for (Location col : columnLocations) {
                        w.spawnParticle(Particle.SOUL_FIRE_FLAME, col.clone().add(0, topY, 0),
                                4, 0.3, 0.5, 0.3, 0.01);
                    }
                }
                if (colTick == 98) {
                    for (Location col : columnLocations) {
                        DisplayBuilder.playSound(col, Sound.BLOCK_SOUL_SAND_BREAK, 0.5f, 0.6f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFireColumnArray(plugin); }
    }

    // =========================================================================
    // 20. MAGMA SURFACE ERUPTION -- 7x7 area becomes active magma surface
    // =========================================================================
    public static class MagmaSurfaceEruption extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> magmaDisplays = new ArrayList<>();
        private Location eruptionCenter;
        private boolean magmaActive = false;

        public MagmaSurfaceEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_surface_eruption", AttackType.ENVIRONMENTAL, 4));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double a = Math.random() * 2 * Math.PI;
            double d = Math.random() * 15;
            eruptionCenter = center.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 50 ticks -- smoke + lava pooling
            if (ticksAlive <= 50) {
                if (ticksAlive % 4 == 0) {
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, eruptionCenter.clone().add(0, 0.5, 0),
                            5 + ticksAlive / 3, 3.5, 0.5, 3.5, 0.01);
                }
                if (ticksAlive >= 30 && ticksAlive % 3 == 0) {
                    w.spawnParticle(Particle.LAVA, eruptionCenter.clone().add(0, 0.3, 0),
                            3, 3.0, 0.3, 3.0, 0);
                }
                if (ticksAlive == 40) {
                    DisplayBuilder.playSound(eruptionCenter, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.4f);
                }
                return;
            }

            // Activate magma surface
            if (!magmaActive) {
                magmaActive = true;

                // Spawn 7x7 magma block displays
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        Location tileLoc = eruptionCenter.clone().add(x, 0.01, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(tileLoc, Material.MAGMA_BLOCK);
                        h.scale(1.0f, 0.1f, 1.0f).glow(255, 100, 0).interpolation(5, 0);
                        magmaDisplays.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }

            int magmaTick = ticksAlive - 50;

            // Active surface: 80 ticks
            if (magmaTick <= 80) {
                // Lava/flame/smoke particles
                if (magmaTick % 3 == 0) {
                    w.spawnParticle(Particle.LAVA, eruptionCenter.clone().add(0, 0.5, 0),
                            10, 3.5, 0.5, 3.5, 0);
                    w.spawnParticle(Particle.FLAME, eruptionCenter.clone().add(0, 0.5, 0),
                            5, 3.0, 0.3, 3.0, 0.01);
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, eruptionCenter.clone().add(0, 1, 0),
                            6, 3.0, 1.0, 3.0, 0.01);
                }

                // Micro-burst: random lava geyser every 15 ticks
                if (magmaTick % 15 == 0) {
                    double bx = (Math.random() - 0.5) * 7;
                    double bz = (Math.random() - 0.5) * 7;
                    Location burstLoc = eruptionCenter.clone().add(bx, 0, bz);
                    w.spawnParticle(Particle.LAVA, burstLoc.clone().add(0, 3, 0), 15, 0.2, 3.0, 0.2, 0);

                    // Micro-burst display
                    BlockDisplayHandle burst = displayBuilder.spawnBlock(
                            burstLoc.clone().add(0, 1, 0), Material.MAGMA_BLOCK);
                    burst.scale(1.5f, 1.5f, 1.5f).glow(255, 100, 0).interpolation(2, 0);
                    spawnedEntities.add(burst.entity());
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> burst.entity().remove(), 5L);

                    // Micro-burst damage: 10 HP
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(burstLoc) <= 1.0) {
                            p.damage(10.0);
                        }
                    }
                }

                // Lava pop sound
                if (magmaTick % 60 == 0) {
                    DisplayBuilder.playSound(eruptionCenter, Sound.BLOCK_LAVA_POP, 0.4f, 1.0f);
                }

                // Contact damage: 4 HP per second
                if (magmaTick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = Math.abs(p.getLocation().getX() - eruptionCenter.getX());
                        double dz = Math.abs(p.getLocation().getZ() - eruptionCenter.getZ());
                        if (dx <= 3.5 && dz <= 3.5 && p.getLocation().getY() <= eruptionCenter.getY() + 1.5) {
                            p.damage(4.0);
                        }
                    }
                }
            }

            // Cooling: 10 ticks
            if (magmaTick > 80 && magmaTick == 81) {
                DisplayBuilder.playSound(eruptionCenter, Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaSurfaceEruption(plugin); }
    }
}
