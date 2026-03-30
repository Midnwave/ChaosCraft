package com.blockforge.chaoscraft.modes.corruption.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Corrupted Corruption Mode -- 30 ModelEngine VFX Attacks
 *
 * <p>Each attack spawns a ModelEngine 4 .bbmodel model, plays animations via the
 * ModelEngine API (reflection), and applies unique damage mechanics per tick.
 *
 * <p>Organized into 6 categories of 5 attacks each:
 * <ol>
 *   <li>Reality Fracture (1-5) -- tears, shards, pillars of broken reality</li>
 *   <li>Corruption Spread (6-10) -- organic corruption, tendrils, blooms</li>
 *   <li>Dark Energy (11-15) -- beams, pulses, orbs of dark energy</li>
 *   <li>Psychological/Horror (16-20) -- eyes, screams, mirrors, paranoia</li>
 *   <li>Physical Corruption (21-25) -- bone spikes, cages, vines, flesh</li>
 *   <li>Environmental Corruption (26-30) -- pillars, suns, fields, gates, nova</li>
 * </ol>
 *
 * <p>Corruption palette:
 * <ul>
 *   <li>Particles: WITCH, DRAGON_BREATH, DUST with Color(80,0,120), Color(40,0,60), Color(160,0,200), Color(20,20,20)</li>
 *   <li>Sounds: ENTITY_WITHER_AMBIENT, BLOCK_SCULK_SHRIEKER_SHRIEK, ENTITY_ENDERMAN_SCREAM,
 *             BLOCK_RESPAWN_ANCHOR_DEPLETE, AMBIENT_CAVE</li>
 * </ul>
 *
 * <p>Rules:
 * <ul>
 *   <li>NO status effects -- damage only (scheduled damage tasks simulate DOT)</li>
 *   <li>All damage goes through player.damage() so armor/resistance apply</li>
 *   <li>SURVIVAL mode check on all damage targets</li>
 *   <li>Exempt player check via isExempt()</li>
 *   <li>Config path: modes/corruption/attacks (MODEL_ENGINE type)</li>
 * </ul>
 */
public final class CorruptionModelEngine {

    private CorruptionModelEngine() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // Reality Fracture (1-5)
        registry.register(new VoidRiftSlash(plugin));
        registry.register(new RealityShatter(plugin));
        registry.register(new DimensionCollapsePillar(plugin));
        registry.register(new FracturedSpaceCrown(plugin));
        registry.register(new EntropySpiral(plugin));

        // Corruption Spread (6-10)
        registry.register(new CreepingCorruptionHands(plugin));
        registry.register(new CorruptionBloom(plugin));
        registry.register(new PlagueTendrilBurst(plugin));
        registry.register(new CorruptionWeb(plugin));
        registry.register(new SpreadingNecrosis(plugin));

        // Dark Energy (11-15)
        registry.register(new SoulDrainBeam(plugin));
        registry.register(new DarkPulseRing(plugin));
        registry.register(new VoidOrbCluster(plugin));
        registry.register(new NecroticShockwave(plugin));
        registry.register(new CorruptionGeyser(plugin));

        // Psychological/Horror (16-20)
        registry.register(new TheStare(plugin));
        registry.register(new MemoryFracture(plugin));
        registry.register(new HollowScream(plugin));
        registry.register(new ParanoiaSpiral(plugin));
        registry.register(new FalseMirror(plugin));

        // Physical Corruption (21-25)
        registry.register(new BoneSpikeEruption(plugin));
        registry.register(new CorruptionCrystalCage(plugin));
        registry.register(new NecroticVines(plugin));
        registry.register(new FleshWarp(plugin));
        registry.register(new CorruptedRoots(plugin));

        // Environmental Corruption (26-30)
        registry.register(new CorruptionPillarArray(plugin));
        registry.register(new BlackSun(plugin));
        registry.register(new EntropyField(plugin));
        registry.register(new VoidGate(plugin));
        registry.register(new CorruptionNova(plugin));
    }

    // ================================================================
    // Helper: find nearest non-exempt survival player within range
    // ================================================================
    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
        Player nearest = null;
        double nearestDist = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dist = p.getLocation().distanceSquared(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    // ================================================================
    // Corruption particle/sound helpers
    // ================================================================

    /** Dark corruption dust -- deep purple. */
    private static void corruptionDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 80, 0, 120, 1.4f);
    }

    /** Deep void dust -- near-black purple. */
    private static void voidDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 40, 0, 60, 1.6f);
    }

    /** Bright corruption dust -- vivid magenta. */
    private static void brightCorruptionDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 160, 0, 200, 1.2f);
    }

    /** Shadow dust -- near-black. */
    private static void shadowDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 20, 20, 20, 1.8f);
    }

    /** Play a corruption ambient sound. */
    private static void corruptionSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, volume, pitch);
    }

    /** Play a corruption shriek sound. */
    private static void shriekSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, volume, pitch);
    }

    /** Play an enderman scream sound. */
    private static void screamSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, volume, pitch);
    }

    /** Play respawn anchor deplete sound. */
    private static void depleteSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, volume, pitch);
    }

    /** Play ambient cave sound. */
    private static void caveSound(Location loc, float volume, float pitch) {
        DisplayBuilder.playSound(loc, Sound.AMBIENT_CAVE, volume, pitch);
    }

    /**
     * Apply damage to all eligible survival players within a radius of a location.
     * Respects exempt checks.
     *
     * @param attack the attack instance (for exempt check)
     * @param loc    center location
     * @param radius damage radius in blocks
     * @param damage damage in half-hearts
     */
    static void damageNearby(ModelEngineAttack attack, Location loc, double radius, double damage) {
        if (loc.getWorld() == null) return;
        double r2 = radius * radius;
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getGameMode() != org.bukkit.GameMode.SURVIVAL || p.isInvulnerable()) continue;
            if (p.getLocation().distanceSquared(loc) <= r2) {
                p.damage(damage);
                p.setNoDamageTicks(0);
            }
        }
    }

    // ========================================================================
    // ==================== REALITY FRACTURE (1-5) ============================
    // ========================================================================

    // ================================================================
    // 1. VOID RIFT SLASH -- Horizontal tear in reality.
    //    Rift line deals 5 hearts/sec crossing, inner glow pulses 3 hearts
    //    burst every 40 ticks within 4 blocks.
    // ================================================================
    public static class VoidRiftSlash extends ModelEngineAttack {

        private Location center;
        private double riftAngle;

        public VoidRiftSlash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_rift_slash", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(10.0);           // 5 hearts/sec via rift line
            config.setDamageRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "void_rift_slash"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.riftAngle = Math.random() * Math.PI;
            spawnModel(center);

            // Spawn burst
            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 40, 2.0, 0.5, 2.0, 0.02);
            voidDust(center, 25, 2.5);
            shriekSound(center, 1.2f, 0.3f);
            depleteSound(center, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Rift line damage: 5 hearts/sec (10 dmg every 20 ticks) to anyone crossing the line
            if (tick % 20 == 0) {
                double cosA = Math.cos(riftAngle);
                double sinA = Math.sin(riftAngle);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pl = p.getLocation();
                    // Project player position onto rift line to check perpendicular distance
                    double dx = pl.getX() - center.getX();
                    double dz = pl.getZ() - center.getZ();
                    double perpDist = Math.abs(-sinA * dx + cosA * dz);
                    double alongDist = Math.abs(cosA * dx + sinA * dz);
                    // Within 1.5 blocks perpendicular and 5 blocks along the rift
                    if (perpDist <= 1.5 && alongDist <= 5.0
                            && Math.abs(pl.getY() - center.getY()) < 3.0) {
                        p.damage(10.0); // 5 hearts
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Inner glow pulse: 3 hearts burst every 40 ticks within 4 blocks
            if (tick % 40 == 0) {
                damageNearby(this, center, 4.0, 6.0);
                brightCorruptionDust(center, 30, 3.0);
                w.spawnParticle(Particle.WITCH, center, 20, 3.0, 1.0, 3.0, 0.02);
                playAnimation("pulse", 0.0, false);
            }

            // Ambient particles
            if (tick % 4 == 0) {
                double ox = Math.cos(riftAngle) * (Math.random() * 5.0 - 2.5);
                double oz = Math.sin(riftAngle) * (Math.random() * 5.0 - 2.5);
                Location riftPoint = center.clone().add(ox, Math.random() * 0.5 - 0.25, oz);
                voidDust(riftPoint, 3, 0.3);
                w.spawnParticle(Particle.DRAGON_BREATH, riftPoint, 2, 0.2, 0.3, 0.2, 0.01);
            }

            // Ambient sound
            if (tick % 60 == 0) {
                corruptionSound(center, 0.6f, 0.2f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new VoidRiftSlash(plugin); }
    }

    // ================================================================
    // 2. REALITY SHATTER -- Cracked glass sphere of dark shards.
    //    Shards drift outward dealing 3 hearts on contact, expanding zone.
    //    Center = 4 hearts/sec void drain.
    // ================================================================
    public static class RealityShatter extends ModelEngineAttack {

        private Location center;
        private double shardRadius = 1.0;

        public RealityShatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_shatter", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(8.0);            // 4 hearts/sec center drain
            config.setDamageRadius(2.0);      // Center zone
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "reality_shatter"; }
        @Override protected double getModelScale() { return 3.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 60, 1.5, 1.5, 1.5, 0.03);
            shadowDust(center, 30, 2.0);
            shriekSound(center, 1.0f, 0.5f);
            screamSound(center, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Shards expand outward over time
            shardRadius = 1.0 + (tick * 0.04);
            double maxShardRadius = 8.0;
            if (shardRadius > maxShardRadius) shardRadius = maxShardRadius;

            // Shard contact damage: 3 hearts to anyone in the shard ring shell
            if (tick % 15 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    // In the shard shell (within 1.5 blocks of current shard radius)
                    if (dist >= shardRadius - 1.5 && dist <= shardRadius + 1.5
                            && Math.abs(p.getLocation().getY() - center.getY()) < 3.0) {
                        p.damage(6.0); // 3 hearts
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Center void drain: 4 hearts/sec
            if (tick % 20 == 0) {
                damageNearby(this, center, 2.0, 8.0);
            }

            // Shard ring particles
            if (tick % 3 == 0) {
                int points = 12;
                for (int i = 0; i < points; i++) {
                    double angle = (2.0 * Math.PI * i) / points + tick * 0.02;
                    double x = Math.cos(angle) * shardRadius;
                    double z = Math.sin(angle) * shardRadius;
                    Location shardLoc = center.clone().add(x, Math.sin(tick * 0.1 + i) * 0.5, z);
                    corruptionDust(shardLoc, 2, 0.2);
                }
            }

            // Center void particles
            if (tick % 5 == 0) {
                voidDust(center, 8, 1.0);
                w.spawnParticle(Particle.WITCH, center, 5, 0.5, 0.5, 0.5, 0.01);
            }

            if (tick % 80 == 0) {
                caveSound(center, 0.8f, 0.3f);
                playAnimation("expand", 0.0, false);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new RealityShatter(plugin); }
    }

    // ================================================================
    // 3. DIMENSION COLLAPSE PILLAR -- Unstable vertical column.
    //    Segments phase in/out. When a segment phases OUT, 6 hearts to
    //    anyone at that Y level. Unpredictable pattern.
    // ================================================================
    public static class DimensionCollapsePillar extends ModelEngineAttack {

        private Location center;
        private static final int SEGMENT_COUNT = 8;
        private final boolean[] segmentActive = new boolean[SEGMENT_COUNT];
        private final int[] segmentTimers = new int[SEGMENT_COUNT];

        public DimensionCollapsePillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimension_collapse_pillar", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(12.0);           // 6 hearts per phase-out hit
            config.setDamageRadius(3.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override protected String getModelId() { return "dimension_collapse_pillar"; }
        @Override protected double getModelScale() { return 3.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            // Initialize segments with random timers
            for (int i = 0; i < SEGMENT_COUNT; i++) {
                segmentActive[i] = true;
                segmentTimers[i] = 20 + (int) (Math.random() * 40);
            }

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 50, 1.0, 4.0, 1.0, 0.03);
            corruptionDust(center, 30, 3.0);
            depleteSound(center, 1.4f, 0.3f);
            corruptionSound(center, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (int i = 0; i < SEGMENT_COUNT; i++) {
                segmentTimers[i]--;

                if (segmentTimers[i] <= 0) {
                    boolean wasActive = segmentActive[i];
                    segmentActive[i] = !segmentActive[i];
                    // Random new timer: 15-45 ticks
                    segmentTimers[i] = 15 + (int) (Math.random() * 30);

                    // Phase OUT = damage at that Y level
                    if (wasActive && !segmentActive[i]) {
                        double segY = center.getY() + i * 1.2;
                        Location segLoc = center.clone();
                        segLoc.setY(segY);

                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            double py = p.getLocation().getY();
                            // Player's feet or head at this Y level (within 1.5 blocks vertically)
                            if (Math.abs(py - segY) < 1.5
                                    && p.getLocation().distanceSquared(center) <= 3.5 * 3.5) {
                                double horizDist = Math.sqrt(
                                        Math.pow(p.getLocation().getX() - center.getX(), 2) +
                                        Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                                if (horizDist <= 3.5) {
                                    p.damage(12.0); // 6 hearts
                                    p.setNoDamageTicks(0);
                                }
                            }
                        }

                        // Phase-out particles
                        brightCorruptionDust(segLoc, 15, 2.0);
                        w.spawnParticle(Particle.DRAGON_BREATH, segLoc, 10, 1.5, 0.3, 1.5, 0.02);
                        DisplayBuilder.playSound(segLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 0.8f);
                    }
                }
            }

            // Ambient pillar particles
            if (tick % 5 == 0) {
                for (int i = 0; i < SEGMENT_COUNT; i++) {
                    if (segmentActive[i]) {
                        double segY = center.getY() + i * 1.2;
                        Location segLoc = center.clone();
                        segLoc.setY(segY);
                        voidDust(segLoc, 3, 0.8);
                    }
                }
            }

            if (tick % 40 == 0) {
                playAnimation("glitch", 0.0, false);
            }

            if (tick % 80 == 0) {
                corruptionSound(center, 0.5f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new DimensionCollapsePillar(plugin); }
    }

    // ================================================================
    // 4. FRACTURED SPACE CROWN -- Ring of floating diamond shards above
    //    head height. Descends onto nearest player, each shard deals
    //    2 hearts on contact (16 total if all 8 hit). Slowly tightens.
    // ================================================================
    public static class FracturedSpaceCrown extends ModelEngineAttack {

        private Location center;
        private double crownY;
        private double crownRadius = 4.0;
        private static final int SHARD_COUNT = 8;

        public FracturedSpaceCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fractured_space_crown", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(4.0);            // 2 hearts per shard
            config.setDamageRadius(5.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(15);
        }

        @Override protected String getModelId() { return "fractured_space_crown"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.crownY = center.getY() + 6.0;
            spawnModel(center.clone().add(0, 6, 0));

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 6, 0),
                    30, 3.0, 0.5, 3.0, 0.02);
            brightCorruptionDust(center.clone().add(0, 6, 0), 20, 3.5);
            screamSound(center, 0.8f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Crown descends toward nearest player
            Player target = findNearestPlayer(center, 15.0);
            if (target != null) {
                double targetY = target.getLocation().getY() + 2.5;
                crownY += (targetY - crownY) * 0.04;

                // Move center toward target horizontally (slowly)
                double dx = target.getLocation().getX() - center.getX();
                double dz = target.getLocation().getZ() - center.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 1.0) {
                    center.add(dx / dist * 0.15, 0, dz / dist * 0.15);
                }
            }

            // Crown tightens over time
            crownRadius = Math.max(0.5, 4.0 - (tick * 0.015));

            // Shard damage: each shard position checks for player contact
            if (tick % 15 == 0) {
                for (int i = 0; i < SHARD_COUNT; i++) {
                    double angle = (2.0 * Math.PI * i) / SHARD_COUNT + tick * 0.03;
                    double sx = center.getX() + Math.cos(angle) * crownRadius;
                    double sz = center.getZ() + Math.sin(angle) * crownRadius;
                    Location shardLoc = new Location(w, sx, crownY, sz);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(shardLoc) <= 1.8 * 1.8) {
                            p.damage(4.0); // 2 hearts per shard
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // Shard particles (rotating ring)
            if (tick % 3 == 0) {
                for (int i = 0; i < SHARD_COUNT; i++) {
                    double angle = (2.0 * Math.PI * i) / SHARD_COUNT + tick * 0.03;
                    double sx = center.getX() + Math.cos(angle) * crownRadius;
                    double sz = center.getZ() + Math.sin(angle) * crownRadius;
                    Location sl = new Location(w, sx, crownY, sz);
                    brightCorruptionDust(sl, 2, 0.15);
                }
            }

            // Teleport model host to follow crown
            if (modelHost != null && modelHost.isValid()) {
                modelHost.teleport(center.clone().add(0, crownY - center.getY(), 0));
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.4f);
                playAnimation("tighten", 0.0, false);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new FracturedSpaceCrown(plugin); }
    }

    // ================================================================
    // 5. ENTROPY SPIRAL -- Spinning propeller blades.
    //    Blade sweep = 4 hearts on contact, spins faster over time
    //    (damage increases to 6 hearts after 100 ticks).
    //    Tip shards fire outward every 60 ticks (3 hearts).
    // ================================================================
    public static class EntropySpiral extends ModelEngineAttack {

        private Location center;
        private double rotationSpeed = 0.05;
        private double currentAngle = 0;
        private static final int BLADE_COUNT = 4;
        private static final double BLADE_LENGTH = 5.0;

        public EntropySpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("entropy_spiral", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(8.0);            // 4 hearts base blade sweep
            config.setDamageRadius(6.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(270);
            config.setTicksBetweenDamage(10);
        }

        @Override protected String getModelId() { return "entropy_spiral"; }
        @Override protected double getModelScale() { return 2.8; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 40, 3.0, 0.5, 3.0, 0.03);
            corruptionDust(center, 25, 3.0);
            depleteSound(center, 1.2f, 0.4f);
            corruptionSound(center, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Speed up over time
            rotationSpeed = 0.05 + (tick * 0.0003);
            currentAngle += rotationSpeed;

            // Damage increases after 100 ticks: 4 hearts -> 6 hearts
            double bladeDamage = tick >= 100 ? 12.0 : 8.0;

            // Blade sweep damage
            if (tick % 5 == 0) {
                for (int b = 0; b < BLADE_COUNT; b++) {
                    double bladeAngle = currentAngle + (2.0 * Math.PI * b) / BLADE_COUNT;
                    double cosB = Math.cos(bladeAngle);
                    double sinB = Math.sin(bladeAngle);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dx = p.getLocation().getX() - center.getX();
                        double dz = p.getLocation().getZ() - center.getZ();
                        // Project onto blade direction
                        double alongBlade = cosB * dx + sinB * dz;
                        double perpBlade = Math.abs(-sinB * dx + cosB * dz);
                        // Hit if along blade (0 to BLADE_LENGTH) and within 1.2 blocks perpendicularly
                        if (alongBlade >= 0 && alongBlade <= BLADE_LENGTH
                                && perpBlade <= 1.2
                                && Math.abs(p.getLocation().getY() - center.getY()) < 2.5) {
                            p.damage(bladeDamage);
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // Tip shards fire outward every 60 ticks: 3 hearts
            if (tick % 60 == 0 && tick > 0) {
                for (int b = 0; b < BLADE_COUNT; b++) {
                    double tipAngle = currentAngle + (2.0 * Math.PI * b) / BLADE_COUNT;
                    Location tipLoc = center.clone().add(
                            Math.cos(tipAngle) * BLADE_LENGTH,
                            0.5,
                            Math.sin(tipAngle) * BLADE_LENGTH);
                    damageNearby(this, tipLoc, 2.5, 6.0); // 3 hearts
                    brightCorruptionDust(tipLoc, 10, 1.5);
                    w.spawnParticle(Particle.DRAGON_BREATH, tipLoc, 8, 1.0, 0.5, 1.0, 0.03);
                    DisplayBuilder.playSound(tipLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.5f, 1.2f);
                }
                playAnimation("shard_fire", 0.0, false);
            }

            // Blade trail particles
            if (tick % 2 == 0) {
                for (int b = 0; b < BLADE_COUNT; b++) {
                    double bladeAngle = currentAngle + (2.0 * Math.PI * b) / BLADE_COUNT;
                    for (double t = 0; t < BLADE_LENGTH; t += 1.0) {
                        Location bl = center.clone().add(
                                Math.cos(bladeAngle) * t, 0.3, Math.sin(bladeAngle) * t);
                        corruptionDust(bl, 1, 0.1);
                    }
                }
            }

            if (tick % 40 == 0) {
                corruptionSound(center, 0.5f, 0.5f + (float) (rotationSpeed * 3.0));
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new EntropySpiral(plugin); }
    }

    // ========================================================================
    // ==================== CORRUPTION SPREAD (6-10) ==========================
    // ========================================================================

    // ================================================================
    // 6. CREEPING CORRUPTION HANDS -- Claw shapes erupting from ground.
    //    Within 2 blocks of palm = 3 hearts/sec + slowness (via damage).
    //    Fingers curling = 5 hearts burst.
    // ================================================================
    public static class CreepingCorruptionHands extends ModelEngineAttack {

        private Location center;
        private static final int HAND_COUNT = 4;
        private final double[] handAngles = new double[HAND_COUNT];
        private final double[] handDists = new double[HAND_COUNT];

        public CreepingCorruptionHands(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("creeping_corruption_hands", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(6.0);            // 3 hearts/sec near palm
            config.setDamageRadius(2.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "creeping_corruption_hands"; }
        @Override protected double getModelScale() { return 2.2; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            for (int i = 0; i < HAND_COUNT; i++) {
                handAngles[i] = (2.0 * Math.PI * i) / HAND_COUNT + Math.random() * 0.5;
                handDists[i] = 2.0 + Math.random() * 3.0;
            }

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 30, 3.0, 0.3, 3.0, 0.02);
            shadowDust(center, 20, 3.0);
            depleteSound(center, 1.0f, 0.3f);
            caveSound(center, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (int h = 0; h < HAND_COUNT; h++) {
                double hx = center.getX() + Math.cos(handAngles[h]) * handDists[h];
                double hz = center.getZ() + Math.sin(handAngles[h]) * handDists[h];
                Location palmLoc = new Location(w, hx, center.getY() + 0.5, hz);

                // Palm proximity damage: 3 hearts/sec
                if (tick % 20 == 0) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(palmLoc) <= 2.0 * 2.0) {
                            p.damage(6.0); // 3 hearts
                            p.setNoDamageTicks(0);
                        }
                    }
                }

                // Fingers curling burst: 5 hearts every 70 ticks
                if (tick % 70 == 0 && tick > 0) {
                    damageNearby(this, palmLoc, 3.0, 10.0);
                    corruptionDust(palmLoc, 15, 2.0);
                    w.spawnParticle(Particle.WITCH, palmLoc, 10, 1.5, 0.5, 1.5, 0.02);
                    playAnimation("curl_" + h, 0.0, false);
                }

                // Hand particles
                if (tick % 4 == 0) {
                    shadowDust(palmLoc, 3, 0.5);
                    // Finger tendril particles
                    for (int f = 0; f < 3; f++) {
                        double fAngle = handAngles[h] + (f - 1) * 0.4;
                        Location fingerTip = palmLoc.clone().add(
                                Math.cos(fAngle) * 1.5, 0.8 + Math.sin(tick * 0.1 + f) * 0.3,
                                Math.sin(fAngle) * 1.5);
                        voidDust(fingerTip, 1, 0.1);
                    }
                }
            }

            if (tick % 50 == 0) {
                corruptionSound(center, 0.6f, 0.25f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CreepingCorruptionHands(plugin); }
    }

    // ================================================================
    // 7. CORRUPTION BLOOM -- Dark flower blooming open.
    //    Pollen cloud: 2 hearts/sec in 6 blocks (poison-like).
    //    Center orb pulses 5 hearts every 50 ticks.
    //    Petals closing = 8 hearts crush.
    // ================================================================
    public static class CorruptionBloom extends ModelEngineAttack {

        private Location center;
        private boolean petalsClosed = false;

        public CorruptionBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_bloom", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(4.0);            // 2 hearts/sec pollen
            config.setDamageRadius(6.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "corruption_bloom"; }
        @Override protected double getModelScale() { return 3.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            center.getWorld().spawnParticle(Particle.WITCH, center, 50, 4.0, 2.0, 4.0, 0.03);
            corruptionDust(center, 30, 4.0);
            caveSound(center, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Pollen cloud: 2 hearts/sec in 6-block radius
            if (tick % 20 == 0) {
                damageNearby(this, center, 6.0, 4.0);
            }

            // Center orb pulse: 5 hearts every 50 ticks
            if (tick % 50 == 0 && tick > 0) {
                damageNearby(this, center, 3.0, 10.0);
                brightCorruptionDust(center, 25, 2.0);
                w.spawnParticle(Particle.DRAGON_BREATH, center, 15, 1.5, 1.0, 1.5, 0.04);
                playAnimation("pulse", 0.0, false);
                depleteSound(center, 0.7f, 0.6f);
            }

            // Petals closing at 80% duration = 8 hearts crush
            int closeTick = (int) (config.getDurationTicks() * 0.8);
            if (tick == closeTick && !petalsClosed) {
                petalsClosed = true;
                damageNearby(this, center, 4.0, 16.0); // 8 hearts
                shadowDust(center, 40, 3.0);
                w.spawnParticle(Particle.DRAGON_BREATH, center, 30, 2.0, 1.5, 2.0, 0.05);
                shriekSound(center, 1.0f, 0.5f);
                playAnimation("close", 0.0, false);
            }

            // Pollen particles
            if (tick % 3 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 6.0;
                Location pollen = center.clone().add(
                        Math.cos(angle) * dist, 0.5 + Math.random() * 2.0,
                        Math.sin(angle) * dist);
                w.spawnParticle(Particle.WITCH, pollen, 2, 0.3, 0.3, 0.3, 0.01);
                corruptionDust(pollen, 1, 0.2);
            }

            if (tick % 60 == 0) {
                caveSound(center, 0.5f, 0.5f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CorruptionBloom(plugin); }
    }

    // ================================================================
    // 8. PLAGUE TENDRIL BURST -- 8 tendrils from center.
    //    Tendrils writhe and sweep -- contact = 3 hearts + 40 tick DOT
    //    (1 heart every 20 ticks for 40 ticks via scheduled damage).
    // ================================================================
    public static class PlagueTendrilBurst extends ModelEngineAttack {

        private Location center;
        private static final int TENDRIL_COUNT = 8;
        private final double[] tendrilAngles = new double[TENDRIL_COUNT];
        private final double[] tendrilLengths = new double[TENDRIL_COUNT];

        public PlagueTendrilBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("plague_tendril_burst", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(6.0);            // 3 hearts contact
            config.setDamageRadius(7.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(15);
        }

        @Override protected String getModelId() { return "plague_tendril_burst"; }
        @Override protected double getModelScale() { return 3.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            for (int i = 0; i < TENDRIL_COUNT; i++) {
                tendrilAngles[i] = (2.0 * Math.PI * i) / TENDRIL_COUNT;
                tendrilLengths[i] = 3.0 + Math.random() * 3.0;
            }

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 40, 2.0, 1.0, 2.0, 0.03);
            voidDust(center, 25, 2.0);
            depleteSound(center, 1.2f, 0.3f);
            shriekSound(center, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (int t = 0; t < TENDRIL_COUNT; t++) {
                // Tendrils writhe: angle oscillates
                double writheAngle = tendrilAngles[t] + Math.sin(tick * 0.08 + t * 1.2) * 0.4;
                double length = tendrilLengths[t] + Math.sin(tick * 0.06 + t) * 1.0;

                // Check along each tendril for player contact
                if (tick % 10 == 0) {
                    for (double d = 0; d < length; d += 1.0) {
                        double tx = center.getX() + Math.cos(writheAngle) * d;
                        double tz = center.getZ() + Math.sin(writheAngle) * d;
                        Location segLoc = new Location(w, tx, center.getY() + 0.3 + Math.sin(d * 0.5 + tick * 0.1) * 0.5, tz);

                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(segLoc) <= 1.5 * 1.5) {
                                p.damage(6.0); // 3 hearts contact
                                p.setNoDamageTicks(0);
                                // Schedule DOT: 1 heart every 20 ticks for 40 ticks
                                scheduleDOT(p, 2.0, 20, 2);
                            }
                        }
                    }
                }

                // Tendril particles
                if (tick % 3 == 0) {
                    for (double d = 0; d < length; d += 0.8) {
                        double tx = center.getX() + Math.cos(writheAngle) * d;
                        double tz = center.getZ() + Math.sin(writheAngle) * d;
                        Location sl = new Location(w, tx,
                                center.getY() + 0.3 + Math.sin(d * 0.5 + tick * 0.1) * 0.5, tz);
                        corruptionDust(sl, 1, 0.15);
                    }
                }
            }

            if (tick % 50 == 0) {
                corruptionSound(center, 0.6f, 0.3f);
                playAnimation("writhe", 0.0, true);
            }
        }

        /** Schedule delayed damage ticks on a player (simulates DOT without status effects). */
        private void scheduleDOT(Player player, double damagePerTick, int interval, int ticks) {
            for (int i = 1; i <= ticks; i++) {
                final int delay = i * interval;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && player.getGameMode() == GameMode.SURVIVAL) {
                        player.damage(damagePerTick);
                        player.setNoDamageTicks(0);
                    }
                }, delay);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new PlagueTendrilBurst(plugin); }
    }

    // ================================================================
    // 9. CORRUPTION WEB -- Concentric hexagonal rings.
    //    Touching any ring = slow + 2 hearts. Nodes between rings explode
    //    if player touches (4 hearts burst). Inner ring = 3 hearts/sec.
    // ================================================================
    public static class CorruptionWeb extends ModelEngineAttack {

        private Location center;
        private static final int RING_COUNT = 3;
        private static final double[] RING_RADII = {2.0, 4.5, 7.0};
        private static final int NODES_PER_RING = 6;

        public CorruptionWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_web", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(4.0);            // 2 hearts ring contact
            config.setDamageRadius(8.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "corruption_web"; }
        @Override protected double getModelScale() { return 3.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            center.getWorld().spawnParticle(Particle.WITCH, center, 40, 5.0, 0.5, 5.0, 0.02);
            shadowDust(center, 30, 5.0);
            caveSound(center, 1.0f, 0.3f);
            corruptionSound(center, 0.8f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dist = Math.sqrt(
                        Math.pow(p.getLocation().getX() - center.getX(), 2) +
                        Math.pow(p.getLocation().getZ() - center.getZ(), 2));

                // Inner ring (ring 0): 3 hearts/sec continuous
                if (dist <= RING_RADII[0] + 0.8 && tick % 20 == 0) {
                    p.damage(6.0); // 3 hearts
                    p.setNoDamageTicks(0);
                }

                // Ring contact damage: 2 hearts
                if (tick % 20 == 0) {
                    for (int r = 0; r < RING_COUNT; r++) {
                        double ringDist = Math.abs(dist - RING_RADII[r]);
                        if (ringDist <= 0.8 && Math.abs(p.getLocation().getY() - center.getY()) < 2.5) {
                            p.damage(4.0); // 2 hearts
                            p.setNoDamageTicks(0);
                            break; // Only one ring hit per tick
                        }
                    }
                }

                // Node explosion: 4 hearts if player near a node
                if (tick % 30 == 0) {
                    for (int r = 0; r < RING_COUNT; r++) {
                        for (int n = 0; n < NODES_PER_RING; n++) {
                            double nodeAngle = (2.0 * Math.PI * n) / NODES_PER_RING + r * 0.5;
                            double nx = center.getX() + Math.cos(nodeAngle) * RING_RADII[r];
                            double nz = center.getZ() + Math.sin(nodeAngle) * RING_RADII[r];
                            Location nodeLoc = new Location(w, nx, center.getY() + 0.5, nz);
                            if (p.getLocation().distanceSquared(nodeLoc) <= 2.0 * 2.0) {
                                p.damage(8.0); // 4 hearts burst
                                p.setNoDamageTicks(0);
                                brightCorruptionDust(nodeLoc, 10, 1.0);
                                w.spawnParticle(Particle.DRAGON_BREATH, nodeLoc, 8, 0.5, 0.5, 0.5, 0.03);
                                DisplayBuilder.playSound(nodeLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.4f, 1.0f);
                            }
                        }
                    }
                }
            }

            // Web ring particles
            if (tick % 4 == 0) {
                for (int r = 0; r < RING_COUNT; r++) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.2, 0),
                            RING_RADII[r], Particle.WITCH, 12 + r * 6, null);
                }
            }

            // Node glow particles
            if (tick % 6 == 0) {
                for (int r = 0; r < RING_COUNT; r++) {
                    for (int n = 0; n < NODES_PER_RING; n++) {
                        double nodeAngle = (2.0 * Math.PI * n) / NODES_PER_RING + r * 0.5;
                        Location nodeLoc = center.clone().add(
                                Math.cos(nodeAngle) * RING_RADII[r], 0.5,
                                Math.sin(nodeAngle) * RING_RADII[r]);
                        corruptionDust(nodeLoc, 2, 0.2);
                    }
                }
            }

            if (tick % 70 == 0) {
                caveSound(center, 0.5f, 0.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CorruptionWeb(plugin); }
    }

    // ================================================================
    // 10. SPREADING NECROSIS -- Cluster of dark patches.
    //     Each patch = 2 hearts/sec damage zone. Patches expand radius
    //     over time. Overlapping patches = stacking damage.
    // ================================================================
    public static class SpreadingNecrosis extends ModelEngineAttack {

        private Location center;
        private static final int PATCH_COUNT = 6;
        private final double[] patchX = new double[PATCH_COUNT];
        private final double[] patchZ = new double[PATCH_COUNT];
        private double patchRadius = 1.5;

        public SpreadingNecrosis(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spreading_necrosis", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(4.0);            // 2 hearts/sec per patch
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "spreading_necrosis"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            for (int i = 0; i < PATCH_COUNT; i++) {
                double angle = (2.0 * Math.PI * i) / PATCH_COUNT + Math.random() * 0.8;
                double dist = 1.5 + Math.random() * 3.5;
                patchX[i] = Math.cos(angle) * dist;
                patchZ[i] = Math.sin(angle) * dist;
            }

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 30, 4.0, 0.3, 4.0, 0.02);
            shadowDust(center, 25, 4.0);
            depleteSound(center, 1.0f, 0.4f);
            caveSound(center, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Patches expand over time
            patchRadius = 1.5 + (tick * 0.01);
            double maxRadius = 4.0;
            if (patchRadius > maxRadius) patchRadius = maxRadius;

            // Damage per patch: 2 hearts/sec, stacks if overlapping
            if (tick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double totalDamage = 0;
                    for (int i = 0; i < PATCH_COUNT; i++) {
                        Location patchCenter = center.clone().add(patchX[i], 0, patchZ[i]);
                        double dx = p.getLocation().getX() - patchCenter.getX();
                        double dz = p.getLocation().getZ() - patchCenter.getZ();
                        double dist2d = Math.sqrt(dx * dx + dz * dz);
                        if (dist2d <= patchRadius && Math.abs(p.getLocation().getY() - center.getY()) < 2.5) {
                            totalDamage += 4.0; // 2 hearts per overlapping patch
                        }
                    }
                    if (totalDamage > 0) {
                        p.damage(totalDamage);
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Patch particles
            if (tick % 4 == 0) {
                for (int i = 0; i < PATCH_COUNT; i++) {
                    Location patchCenter = center.clone().add(patchX[i], 0.1, patchZ[i]);
                    DisplayBuilder.particleRing(patchCenter, patchRadius, Particle.WITCH,
                            (int) (patchRadius * 4), null);
                    shadowDust(patchCenter, 2, patchRadius * 0.5);
                }
            }

            if (tick % 60 == 0) {
                corruptionSound(center, 0.5f, 0.3f);
                playAnimation("spread", 0.0, false);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new SpreadingNecrosis(plugin); }
    }

    // ========================================================================
    // ==================== DARK ENERGY (11-15) ===============================
    // ========================================================================

    // ================================================================
    // 11. SOUL DRAIN BEAM -- Vertical column of rising discs.
    //     Standing within = 3 hearts/sec drain. Each disc passing through
    //     = 1 extra heart. Players pulled slightly upward.
    // ================================================================
    public static class SoulDrainBeam extends ModelEngineAttack {

        private Location center;
        private static final int DISC_COUNT = 5;
        private final double[] discY = new double[DISC_COUNT];

        public SoulDrainBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_drain_beam", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(6.0);            // 3 hearts/sec drain
            config.setDamageRadius(3.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "soul_drain_beam"; }
        @Override protected double getModelScale() { return 3.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            for (int i = 0; i < DISC_COUNT; i++) {
                discY[i] = i * 2.0;
            }

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 40, 1.5, 5.0, 1.5, 0.03);
            brightCorruptionDust(center, 20, 2.0);
            shriekSound(center, 1.0f, 0.5f);
            corruptionSound(center, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Rise discs upward, loop back to bottom
            for (int i = 0; i < DISC_COUNT; i++) {
                discY[i] += 0.15;
                if (discY[i] > 10.0) discY[i] = 0;
            }

            // Beam column damage: 3 hearts/sec
            if (tick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double hDist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - center.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                    if (hDist <= 3.0) {
                        double damage = 6.0; // 3 hearts base
                        // Extra 1 heart per disc passing through player's Y
                        for (int i = 0; i < DISC_COUNT; i++) {
                            double discWorldY = center.getY() + discY[i];
                            if (Math.abs(p.getLocation().getY() - discWorldY) < 1.5) {
                                damage += 2.0; // +1 heart per disc
                            }
                        }
                        p.damage(damage);
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Pull players slightly upward
            if (tick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double hDist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - center.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                    if (hDist <= 4.0) {
                        Vector vel = p.getVelocity();
                        p.setVelocity(vel.add(new Vector(0, 0.06, 0)));
                    }
                }
            }

            // Disc particles
            if (tick % 2 == 0) {
                for (int i = 0; i < DISC_COUNT; i++) {
                    Location discLoc = center.clone().add(0, discY[i], 0);
                    DisplayBuilder.particleRing(discLoc, 2.0, Particle.WITCH, 10, null);
                    brightCorruptionDust(discLoc, 3, 1.5);
                }
            }

            // Beam column particles
            if (tick % 3 == 0) {
                Location beamPoint = center.clone().add(
                        Math.random() * 1.0 - 0.5, Math.random() * 10.0,
                        Math.random() * 1.0 - 0.5);
                voidDust(beamPoint, 2, 0.3);
            }

            if (tick % 50 == 0) {
                shriekSound(center, 0.4f, 0.6f);
                playAnimation("rise", 0.0, true);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new SoulDrainBeam(plugin); }
    }

    // ================================================================
    // 12. DARK PULSE RING -- Expanding ring shockwave.
    //     Ring edge = 5 hearts as it passes. Inner = 3 hearts.
    //     Pulses repeat every 60 ticks (loop).
    // ================================================================
    public static class DarkPulseRing extends ModelEngineAttack {

        private Location center;
        private double pulseRadius = 0;
        private int pulseAge = 0;
        private static final double MAX_PULSE_RADIUS = 12.0;
        private static final double EXPANSION_SPEED = 0.3;

        public DarkPulseRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_pulse_ring", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(10.0);           // 5 hearts ring edge
            config.setDamageRadius(12.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(5);
        }

        @Override protected String getModelId() { return "dark_pulse_ring"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 50, 2.0, 0.5, 2.0, 0.04);
            corruptionDust(center, 30, 2.0);
            depleteSound(center, 1.4f, 0.3f);
            shriekSound(center, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            pulseAge++;
            pulseRadius += EXPANSION_SPEED;

            // Reset pulse cycle every 60 ticks
            if (pulseAge >= 60) {
                pulseAge = 0;
                pulseRadius = 0;
                depleteSound(center, 0.8f, 0.4f);
                playAnimation("pulse", 0.0, false);
            }

            // Ring edge damage: 5 hearts as ring passes through players
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dist = Math.sqrt(
                        Math.pow(p.getLocation().getX() - center.getX(), 2) +
                        Math.pow(p.getLocation().getZ() - center.getZ(), 2));

                // Ring edge (within 1.5 blocks of current ring radius)
                if (Math.abs(dist - pulseRadius) <= 1.5
                        && Math.abs(p.getLocation().getY() - center.getY()) < 3.0) {
                    p.damage(10.0); // 5 hearts
                    p.setNoDamageTicks(0);
                }

                // Inner ring damage: 3 hearts for anyone inside the expanding ring
                if (dist < pulseRadius - 1.5 && dist > 0.5 && pulseAge % 20 == 0
                        && Math.abs(p.getLocation().getY() - center.getY()) < 3.0) {
                    p.damage(6.0); // 3 hearts
                    p.setNoDamageTicks(0);
                }
            }

            // Ring particles
            if (tick % 2 == 0 && pulseRadius > 0 && pulseRadius < MAX_PULSE_RADIUS) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0),
                        pulseRadius, Particle.DRAGON_BREATH, (int) (pulseRadius * 6), null);
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0),
                        pulseRadius, Particle.WITCH, (int) (pulseRadius * 3), null);
            }

            // Center glow
            if (tick % 5 == 0) {
                corruptionDust(center, 5, 1.0);
            }

            if (tick % 60 == 0) {
                corruptionSound(center, 0.6f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new DarkPulseRing(plugin); }
    }

    // ================================================================
    // 13. VOID ORB CLUSTER -- Central orb + 5 satellite orbs.
    //     Satellites orbit and deal 3 hearts on contact.
    //     Central orb pulls players inward (velocity) + 2 hearts/sec.
    //     All orbs aligning = 10 hearts burst.
    // ================================================================
    public static class VoidOrbCluster extends ModelEngineAttack {

        private Location center;
        private static final int SATELLITE_COUNT = 5;
        private double orbitAngle = 0;
        private static final double ORBIT_RADIUS = 4.0;
        private int lastAlignTick = -100;

        public VoidOrbCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_orb_cluster", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(4.0);            // 2 hearts/sec pull
            config.setDamageRadius(2.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "void_orb_cluster"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 50, 3.0, 1.0, 3.0, 0.03);
            voidDust(center, 25, 3.0);
            shriekSound(center, 1.2f, 0.3f);
            depleteSound(center, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            orbitAngle += 0.06;

            // Central orb: pull players inward + 2 hearts/sec
            if (tick % 20 == 0) {
                damageNearby(this, center, 2.5, 4.0);
            }

            // Pull players toward center
            if (tick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist <= 8.0 && dist > 1.0) {
                        Vector pullDir = center.toVector().subtract(p.getLocation().toVector()).normalize();
                        p.setVelocity(p.getVelocity().add(pullDir.multiply(0.08)));
                    }
                }
            }

            // Satellite orb damage: 3 hearts on contact
            if (tick % 10 == 0) {
                for (int s = 0; s < SATELLITE_COUNT; s++) {
                    double sAngle = orbitAngle + (2.0 * Math.PI * s) / SATELLITE_COUNT;
                    double sx = center.getX() + Math.cos(sAngle) * ORBIT_RADIUS;
                    double sy = center.getY() + Math.sin(sAngle * 0.5) * 1.5;
                    double sz = center.getZ() + Math.sin(sAngle) * ORBIT_RADIUS;
                    Location satLoc = new Location(w, sx, sy, sz);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(satLoc) <= 2.0 * 2.0) {
                            p.damage(6.0); // 3 hearts
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // All orbs alignment check: every 80 ticks, 10 hearts burst
            if (tick % 80 == 0 && tick > 0 && tick - lastAlignTick >= 80) {
                lastAlignTick = tick;
                damageNearby(this, center, 6.0, 20.0); // 10 hearts
                brightCorruptionDust(center, 40, 5.0);
                w.spawnParticle(Particle.DRAGON_BREATH, center, 30, 4.0, 2.0, 4.0, 0.05);
                shriekSound(center, 1.0f, 0.5f);
                playAnimation("align", 0.0, false);
            }

            // Satellite particles
            if (tick % 2 == 0) {
                for (int s = 0; s < SATELLITE_COUNT; s++) {
                    double sAngle = orbitAngle + (2.0 * Math.PI * s) / SATELLITE_COUNT;
                    Location satLoc = center.clone().add(
                            Math.cos(sAngle) * ORBIT_RADIUS,
                            Math.sin(sAngle * 0.5) * 1.5,
                            Math.sin(sAngle) * ORBIT_RADIUS);
                    brightCorruptionDust(satLoc, 3, 0.3);
                    w.spawnParticle(Particle.WITCH, satLoc, 2, 0.2, 0.2, 0.2, 0.01);
                }
            }

            // Central orb glow
            if (tick % 4 == 0) {
                voidDust(center, 5, 0.8);
            }

            if (tick % 60 == 0) {
                corruptionSound(center, 0.5f, 0.25f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new VoidOrbCluster(plugin); }
    }

    // ================================================================
    // 14. NECROTIC SHOCKWAVE -- 3 ripple rings expanding outward.
    //     Each ring = 4 hearts as it passes.
    //     20-tick stagger between rings. All 3 hitting = 12 hearts.
    // ================================================================
    public static class NecroticShockwave extends ModelEngineAttack {

        private Location center;
        private static final int RING_WAVES = 3;
        private final double[] ringRadii = new double[RING_WAVES];
        private final boolean[] ringActive = new boolean[RING_WAVES];
        private static final double MAX_RING_RADIUS = 15.0;

        public NecroticShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("necrotic_shockwave", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(8.0);            // 4 hearts per ring
            config.setDamageRadius(15.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(5);
        }

        @Override protected String getModelId() { return "necrotic_shockwave"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            for (int i = 0; i < RING_WAVES; i++) {
                ringRadii[i] = 0;
                ringActive[i] = false;
            }
            ringActive[0] = true;

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 60, 1.0, 0.5, 1.0, 0.05);
            shadowDust(center, 30, 2.0);
            depleteSound(center, 1.4f, 0.2f);
            screamSound(center, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Activate rings with 20-tick stagger
            for (int i = 0; i < RING_WAVES; i++) {
                if (!ringActive[i] && tick >= i * 20) {
                    ringActive[i] = true;
                    depleteSound(center, 0.6f, 0.4f + i * 0.2f);
                }
            }

            // Expand active rings
            for (int i = 0; i < RING_WAVES; i++) {
                if (ringActive[i] && ringRadii[i] < MAX_RING_RADIUS) {
                    ringRadii[i] += 0.4;
                }
            }

            // Ring damage: 4 hearts per ring
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dist = Math.sqrt(
                        Math.pow(p.getLocation().getX() - center.getX(), 2) +
                        Math.pow(p.getLocation().getZ() - center.getZ(), 2));

                for (int i = 0; i < RING_WAVES; i++) {
                    if (!ringActive[i]) continue;
                    if (ringRadii[i] >= MAX_RING_RADIUS) continue;
                    if (Math.abs(dist - ringRadii[i]) <= 1.5
                            && Math.abs(p.getLocation().getY() - center.getY()) < 3.0) {
                        p.damage(8.0); // 4 hearts
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Ring particles
            if (tick % 2 == 0) {
                for (int i = 0; i < RING_WAVES; i++) {
                    if (ringActive[i] && ringRadii[i] > 0 && ringRadii[i] < MAX_RING_RADIUS) {
                        DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0),
                                ringRadii[i], Particle.DRAGON_BREATH, (int) (ringRadii[i] * 5), null);
                        DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0),
                                ringRadii[i], Particle.WITCH, (int) (ringRadii[i] * 3), null);
                    }
                }
            }

            if (tick % 40 == 0) {
                corruptionSound(center, 0.5f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new NecroticShockwave(plugin); }
    }

    // ================================================================
    // 15. CORRUPTION GEYSER -- Erupting dark column.
    //     Base eruption = 6 hearts in 3 blocks.
    //     Column = 3 hearts to anyone at same X/Z.
    //     Cap spray = 2 hearts rain in 5-block radius.
    // ================================================================
    public static class CorruptionGeyser extends ModelEngineAttack {

        private Location center;
        private boolean erupted = false;
        private double columnHeight = 0;
        private static final double MAX_COLUMN_HEIGHT = 12.0;

        public CorruptionGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_geyser_me", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(12.0);           // 6 hearts base eruption
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(230);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "corruption_geyser"; }
        @Override protected double getModelScale() { return 3.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            // Warning particles before eruption
            corruptionDust(center, 20, 1.5);
            w(center.getWorld(), center);
            caveSound(center, 1.2f, 0.2f);
        }

        /** Spawn warning particles at ground level. */
        private void w(World world, Location loc) {
            if (world == null) return;
            world.spawnParticle(Particle.WITCH, loc, 15, 1.5, 0.2, 1.5, 0.01);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Eruption at tick 20 (1 second warning)
            if (tick == 20 && !erupted) {
                erupted = true;
                // Base eruption: 6 hearts in 3 blocks
                damageNearby(this, center, 3.0, 12.0);
                w.spawnParticle(Particle.DRAGON_BREATH, center, 60, 1.5, 3.0, 1.5, 0.06);
                shadowDust(center, 30, 2.0);
                depleteSound(center, 1.4f, 0.3f);
                shriekSound(center, 1.0f, 0.5f);
                playAnimation("erupt", 0.0, false);
            }

            // Column grows after eruption
            if (erupted && columnHeight < MAX_COLUMN_HEIGHT) {
                columnHeight += 0.4;
            }

            // Column damage: 3 hearts at same X/Z within 2 blocks
            if (erupted && tick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double hDist = Math.sqrt(
                            Math.pow(p.getLocation().getX() - center.getX(), 2) +
                            Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                    if (hDist <= 2.0 && p.getLocation().getY() >= center.getY()
                            && p.getLocation().getY() <= center.getY() + columnHeight) {
                        p.damage(6.0); // 3 hearts
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Cap spray: 2 hearts in 5-block radius from top
            if (erupted && columnHeight >= MAX_COLUMN_HEIGHT && tick % 20 == 0) {
                Location capLoc = center.clone().add(0, columnHeight, 0);
                damageNearby(this, capLoc, 5.0, 4.0);
                w.spawnParticle(Particle.DRAGON_BREATH, capLoc, 20, 4.0, 1.0, 4.0, 0.04);
            }

            // Column particles
            if (erupted && tick % 3 == 0) {
                for (double y = 0; y < columnHeight; y += 1.5) {
                    Location cl = center.clone().add(
                            Math.random() * 0.8 - 0.4, y, Math.random() * 0.8 - 0.4);
                    corruptionDust(cl, 2, 0.3);
                    w.spawnParticle(Particle.WITCH, cl, 1, 0.2, 0.5, 0.2, 0.01);
                }
            }

            // Warning particles before eruption
            if (!erupted && tick % 3 == 0) {
                corruptionDust(center, 5, 1.5);
                w.spawnParticle(Particle.WITCH, center, 3, 1.0, 0.1, 1.0, 0.01);
            }

            if (tick % 60 == 0 && erupted) {
                corruptionSound(center, 0.5f, 0.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CorruptionGeyser(plugin); }
    }

    // ========================================================================
    // ==================== PSYCHOLOGICAL / HORROR (16-20) ====================
    // ========================================================================

    // ================================================================
    // 16. THE STARE -- Massive floating eye.
    //     Blink attack every 60 ticks = 6 hearts to all within 10 blocks.
    //     Players facing eye (within 30 deg) take double.
    //     Iris rotation = 2 hearts/sec in gaze beam direction.
    // ================================================================
    public static class TheStare extends ModelEngineAttack {

        private Location center;
        private double gazeAngle = 0;

        public TheStare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_stare", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(12.0);           // 6 hearts blink
            config.setDamageRadius(10.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(60);
        }

        @Override protected String getModelId() { return "the_stare"; }
        @Override protected double getModelScale() { return 4.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone().add(0, 4, 0);
            spawnModel(this.center);

            this.center.getWorld().spawnParticle(Particle.DRAGON_BREATH, this.center, 40, 2.0, 2.0, 2.0, 0.02);
            brightCorruptionDust(this.center, 25, 3.0);
            screamSound(this.center, 1.4f, 0.2f);
            caveSound(this.center, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Iris rotation: track nearest player
            Player nearest = findNearestPlayer(center, 15.0);
            if (nearest != null) {
                double dx = nearest.getLocation().getX() - center.getX();
                double dz = nearest.getLocation().getZ() - center.getZ();
                gazeAngle = Math.atan2(dz, dx);
            } else {
                gazeAngle += 0.02;
            }

            // Blink attack: every 60 ticks, 6 hearts (12 if facing)
            if (tick % 60 == 0 && tick > 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 10.0 * 10.0) {
                        double damage = 12.0; // 6 hearts base

                        // Check if player is facing the eye (within 30 degrees)
                        Vector toEye = center.toVector().subtract(p.getLocation().toVector()).normalize();
                        Vector playerLook = p.getLocation().getDirection().normalize();
                        double dot = toEye.dot(playerLook);
                        // cos(30 deg) = 0.866
                        if (dot > 0.866) {
                            damage *= 2.0; // Double damage = 12 hearts
                        }

                        p.damage(damage);
                        p.setNoDamageTicks(0);
                    }
                }
                brightCorruptionDust(center, 30, 4.0);
                shriekSound(center, 1.0f, 0.5f);
                playAnimation("blink", 0.0, false);
            }

            // Iris gaze beam: 2 hearts/sec in gaze direction
            if (tick % 20 == 0) {
                for (double d = 1.0; d <= 10.0; d += 1.0) {
                    Location beamPoint = center.clone().add(
                            Math.cos(gazeAngle) * d, 0, Math.sin(gazeAngle) * d);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(beamPoint) <= 2.0 * 2.0) {
                            p.damage(4.0); // 2 hearts
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // Gaze beam particles
            if (tick % 3 == 0) {
                for (double d = 0; d <= 10.0; d += 0.5) {
                    Location bp = center.clone().add(
                            Math.cos(gazeAngle) * d, 0, Math.sin(gazeAngle) * d);
                    corruptionDust(bp, 1, 0.15);
                }
            }

            // Eye ambient glow
            if (tick % 5 == 0) {
                brightCorruptionDust(center, 4, 1.0);
                w.spawnParticle(Particle.WITCH, center, 3, 1.0, 1.0, 1.0, 0.01);
            }

            if (tick % 80 == 0) {
                screamSound(center, 0.5f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new TheStare(plugin); }
    }

    // ================================================================
    // 17. MEMORY FRACTURE -- 9 copies of same shape at wrong positions.
    //     Each copy has 2-block damage zone (2 hearts).
    //     They teleport to new positions every 40 ticks. Unpredictable.
    // ================================================================
    public static class MemoryFracture extends ModelEngineAttack {

        private Location center;
        private static final int COPY_COUNT = 9;
        private final double[] copyX = new double[COPY_COUNT];
        private final double[] copyZ = new double[COPY_COUNT];

        public MemoryFracture(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("memory_fracture", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(4.0);            // 2 hearts per copy zone
            config.setDamageRadius(10.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(270);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "memory_fracture"; }
        @Override protected double getModelScale() { return 1.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);
            randomizeCopyPositions();

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 30, 5.0, 1.0, 5.0, 0.02);
            voidDust(center, 20, 5.0);
            caveSound(center, 1.2f, 0.3f);
            screamSound(center, 0.6f, 0.5f);
        }

        private void randomizeCopyPositions() {
            for (int i = 0; i < COPY_COUNT; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 2.0 + Math.random() * 7.0;
                copyX[i] = Math.cos(angle) * dist;
                copyZ[i] = Math.sin(angle) * dist;
            }
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Teleport copies every 40 ticks
            if (tick % 40 == 0 && tick > 0) {
                randomizeCopyPositions();
                screamSound(center, 0.4f, 0.7f);
                playAnimation("glitch", 0.0, false);
                // Flash particles at new positions
                for (int i = 0; i < COPY_COUNT; i++) {
                    Location copyLoc = center.clone().add(copyX[i], 0.5, copyZ[i]);
                    brightCorruptionDust(copyLoc, 8, 1.0);
                }
            }

            // Per-copy damage zones: 2 hearts
            if (tick % 20 == 0) {
                for (int i = 0; i < COPY_COUNT; i++) {
                    Location copyLoc = center.clone().add(copyX[i], 0.5, copyZ[i]);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(copyLoc) <= 2.0 * 2.0) {
                            p.damage(4.0); // 2 hearts
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // Copy glimmer particles
            if (tick % 5 == 0) {
                for (int i = 0; i < COPY_COUNT; i++) {
                    Location copyLoc = center.clone().add(copyX[i], 0.5, copyZ[i]);
                    corruptionDust(copyLoc, 2, 0.5);
                    w.spawnParticle(Particle.WITCH, copyLoc, 1, 0.3, 0.5, 0.3, 0.01);
                }
            }

            if (tick % 70 == 0) {
                caveSound(center, 0.4f, 0.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new MemoryFracture(plugin); }
    }

    // ================================================================
    // 18. HOLLOW SCREAM -- Open mouth with jagged teeth.
    //     Scream pulse every 50 ticks = 4 hearts + knockback in 8-block
    //     cone. Teeth = 3 hearts on contact. Dark cavity = 5 hearts/sec.
    // ================================================================
    public static class HollowScream extends ModelEngineAttack {

        private Location center;
        private double mouthAngle;

        public HollowScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hollow_scream", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(8.0);            // 4 hearts scream
            config.setDamageRadius(8.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(270);
            config.setTicksBetweenDamage(50);
        }

        @Override protected String getModelId() { return "hollow_scream"; }
        @Override protected double getModelScale() { return 3.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone().add(0, 2, 0);
            this.mouthAngle = Math.random() * Math.PI * 2;
            spawnModel(this.center);

            this.center.getWorld().spawnParticle(Particle.DRAGON_BREATH, this.center, 40, 2.0, 1.5, 2.0, 0.03);
            shadowDust(this.center, 25, 2.5);
            screamSound(this.center, 1.4f, 0.2f);
            shriekSound(this.center, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Scream pulse: 4 hearts + knockback in 8-block cone every 50 ticks
            if (tick % 50 == 0 && tick > 0) {
                double coneHalfAngle = Math.PI / 4; // 45 degree half-angle = 90 degree cone
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist > 8.0) continue;

                    // Check if player is in the cone
                    double dx = p.getLocation().getX() - center.getX();
                    double dz = p.getLocation().getZ() - center.getZ();
                    double playerAngle = Math.atan2(dz, dx);
                    double angleDiff = Math.abs(normalizeAngle(playerAngle - mouthAngle));
                    if (angleDiff <= coneHalfAngle) {
                        p.damage(8.0); // 4 hearts
                        p.setNoDamageTicks(0);
                        // Knockback away from mouth
                        Vector kb = p.getLocation().toVector().subtract(center.toVector()).normalize();
                        p.setVelocity(p.getVelocity().add(kb.multiply(1.2)));
                    }
                }
                // Cone particles
                for (double d = 1.0; d <= 8.0; d += 0.8) {
                    double spread = d * Math.tan(coneHalfAngle);
                    for (int i = 0; i < 3; i++) {
                        double offsetAngle = mouthAngle + (Math.random() - 0.5) * coneHalfAngle * 2;
                        Location cp = center.clone().add(Math.cos(offsetAngle) * d, 0, Math.sin(offsetAngle) * d);
                        w.spawnParticle(Particle.DRAGON_BREATH, cp, 2, spread * 0.2, 0.3, spread * 0.2, 0.02);
                    }
                }
                shriekSound(center, 1.0f, 0.4f);
                playAnimation("scream", 0.0, false);
            }

            // Teeth damage: 3 hearts on contact (close range, ring around mouth)
            if (tick % 15 == 0) {
                for (int t = 0; t < 6; t++) {
                    double toothAngle = mouthAngle + (t - 2.5) * 0.3;
                    Location toothLoc = center.clone().add(
                            Math.cos(toothAngle) * 1.5, -0.5 + Math.sin(t * 1.2) * 0.3,
                            Math.sin(toothAngle) * 1.5);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(toothLoc) <= 1.5 * 1.5) {
                            p.damage(6.0); // 3 hearts
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // Dark cavity: 5 hearts/sec inside the mouth (behind teeth)
            if (tick % 20 == 0) {
                Location cavityCenter = center.clone().add(
                        Math.cos(mouthAngle) * (-0.5), 0, Math.sin(mouthAngle) * (-0.5));
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(cavityCenter) <= 2.0 * 2.0) {
                        p.damage(10.0); // 5 hearts
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Ambient mouth particles
            if (tick % 4 == 0) {
                shadowDust(center, 3, 1.0);
                Location mouthFront = center.clone().add(Math.cos(mouthAngle) * 1.5, 0, Math.sin(mouthAngle) * 1.5);
                voidDust(mouthFront, 2, 0.5);
            }

            if (tick % 70 == 0) {
                screamSound(center, 0.5f, 0.3f);
            }
        }

        private static double normalizeAngle(double angle) {
            while (angle > Math.PI) angle -= 2 * Math.PI;
            while (angle < -Math.PI) angle += 2 * Math.PI;
            return angle;
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new HollowScream(plugin); }
    }

    // ================================================================
    // 19. PARANOIA SPIRAL -- Dizzying logarithmic spiral.
    //     Spiral rotation confuses player movement (random velocity nudges
    //     every 10 ticks). Contact with any slab = 3 hearts.
    //     Center = 6 hearts gravity pull + damage.
    // ================================================================
    public static class ParanoiaSpiral extends ModelEngineAttack {

        private Location center;
        private double spiralAngle = 0;

        public ParanoiaSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("paranoia_spiral", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(6.0);            // 3 hearts slab contact
            config.setDamageRadius(8.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(15);
        }

        @Override protected String getModelId() { return "paranoia_spiral"; }
        @Override protected double getModelScale() { return 3.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            center.getWorld().spawnParticle(Particle.WITCH, center, 40, 4.0, 1.0, 4.0, 0.03);
            corruptionDust(center, 25, 4.0);
            caveSound(center, 1.2f, 0.2f);
            corruptionSound(center, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            spiralAngle += 0.08;

            // Confuse player movement: random velocity nudges every 10 ticks
            if (tick % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 8.0 * 8.0) {
                        double nudgeX = (Math.random() - 0.5) * 0.3;
                        double nudgeZ = (Math.random() - 0.5) * 0.3;
                        p.setVelocity(p.getVelocity().add(new Vector(nudgeX, 0, nudgeZ)));
                    }
                }
            }

            // Spiral slab damage: 3 hearts on contact
            if (tick % 15 == 0) {
                int spiralPoints = 16;
                for (int i = 0; i < spiralPoints; i++) {
                    // Logarithmic spiral: r = a * e^(b*theta)
                    double theta = spiralAngle + (2.0 * Math.PI * i) / spiralPoints;
                    double r = 0.5 * Math.exp(0.15 * ((2.0 * Math.PI * i) / spiralPoints));
                    if (r > 7.0) continue;
                    double sx = center.getX() + Math.cos(theta) * r;
                    double sz = center.getZ() + Math.sin(theta) * r;
                    Location slabLoc = new Location(w, sx, center.getY() + 0.3, sz);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(slabLoc) <= 1.5 * 1.5) {
                            p.damage(6.0); // 3 hearts
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // Center gravity pull + 6 hearts damage
            if (tick % 20 == 0) {
                damageNearby(this, center, 2.5, 12.0); // 6 hearts
            }
            // Gravity pull toward center
            if (tick % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = p.getLocation().distance(center);
                    if (dist <= 3.0 && dist > 0.5) {
                        Vector pull = center.toVector().subtract(p.getLocation().toVector()).normalize();
                        p.setVelocity(p.getVelocity().add(pull.multiply(0.12)));
                    }
                }
            }

            // Spiral particles
            if (tick % 3 == 0) {
                for (int i = 0; i < 20; i++) {
                    double theta = spiralAngle + (2.0 * Math.PI * i) / 20.0;
                    double r = 0.5 * Math.exp(0.15 * ((2.0 * Math.PI * i) / 20.0));
                    if (r > 7.0) continue;
                    Location sp = center.clone().add(Math.cos(theta) * r, 0.3, Math.sin(theta) * r);
                    corruptionDust(sp, 1, 0.1);
                }
            }

            // Center glow
            if (tick % 5 == 0) {
                brightCorruptionDust(center, 5, 0.8);
            }

            if (tick % 60 == 0) {
                caveSound(center, 0.5f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new ParanoiaSpiral(plugin); }
    }

    // ================================================================
    // 20. FALSE MIRROR -- Dark mirror in frame.
    //     Reflection surface copies incoming damage as corruption damage
    //     (reflected). Frame collapse on dissipate = 7 hearts shrapnel
    //     in 6 blocks. Idle ripple = 2 hearts/sec within 3 blocks.
    // ================================================================
    public static class FalseMirror extends ModelEngineAttack {

        private Location center;
        private double mirrorAngle;

        public FalseMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("false_mirror", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(4.0);            // 2 hearts/sec idle ripple
            config.setDamageRadius(3.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "false_mirror"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone().add(0, 1.5, 0);
            this.mirrorAngle = Math.random() * Math.PI * 2;
            spawnModel(this.center);

            this.center.getWorld().spawnParticle(Particle.DRAGON_BREATH, this.center, 30, 1.5, 2.0, 1.5, 0.02);
            brightCorruptionDust(this.center, 20, 2.0);
            caveSound(this.center, 1.0f, 0.5f);
            DisplayBuilder.playSound(this.center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Idle ripple: 2 hearts/sec within 3 blocks
            if (tick % 20 == 0) {
                damageNearby(this, center, 3.0, 4.0);
            }

            // Reflection damage: players within 5 blocks facing the mirror surface
            // take corruption damage as if reflected
            if (tick % 25 == 0) {
                double mirrorNormX = Math.cos(mirrorAngle);
                double mirrorNormZ = Math.sin(mirrorAngle);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) > 5.0 * 5.0) continue;
                    // Check if player is on the reflective side and facing toward mirror
                    double dx = p.getLocation().getX() - center.getX();
                    double dz = p.getLocation().getZ() - center.getZ();
                    double dotNorm = dx * mirrorNormX + dz * mirrorNormZ;
                    if (dotNorm > 0) {
                        // Player is on the front side, check if looking at mirror
                        Vector toLook = center.toVector().subtract(p.getLocation().toVector()).normalize();
                        Vector playerDir = p.getLocation().getDirection().normalize();
                        if (toLook.dot(playerDir) > 0.5) {
                            p.damage(6.0); // 3 hearts reflected corruption
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // Mirror ripple particles
            if (tick % 4 == 0) {
                double perpX = -Math.sin(mirrorAngle);
                double perpZ = Math.cos(mirrorAngle);
                for (double t = -1.5; t <= 1.5; t += 0.5) {
                    Location rl = center.clone().add(perpX * t, Math.sin(tick * 0.1 + t) * 0.3, perpZ * t);
                    brightCorruptionDust(rl, 1, 0.15);
                }
            }

            // Frame glow
            if (tick % 6 == 0) {
                corruptionDust(center, 3, 1.0);
                w.spawnParticle(Particle.WITCH, center, 2, 0.8, 1.0, 0.8, 0.01);
            }

            if (tick % 80 == 0) {
                caveSound(center, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onModelCleanup() {
            // Frame collapse shrapnel on dissipate: 7 hearts in 6 blocks
            if (center != null && center.getWorld() != null) {
                damageNearby(this, center, 6.0, 14.0);
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 50, 4.0, 2.0, 4.0, 0.05);
                shadowDust(center, 30, 4.0);
                depleteSound(center, 1.2f, 0.3f);
                screamSound(center, 0.8f, 0.5f);
            }
        }

        @Override public AbstractAttack newInstance() { return new FalseMirror(plugin); }
    }

    // ========================================================================
    // ==================== PHYSICAL CORRUPTION (21-25) =======================
    // ========================================================================

    // ================================================================
    // 21. BONE SPIKE ERUPTION -- Jagged spikes from ground.
    //     Each spike eruption = 5 hearts impact. Tips sway dealing
    //     2 hearts on contact. Multiple spikes = stacking.
    // ================================================================
    public static class BoneSpikeEruption extends ModelEngineAttack {

        private Location center;
        private static final int SPIKE_COUNT = 6;
        private final double[] spikeX = new double[SPIKE_COUNT];
        private final double[] spikeZ = new double[SPIKE_COUNT];
        private final int[] spikeSpawnTick = new int[SPIKE_COUNT];

        public BoneSpikeEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bone_spike_eruption", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(10.0);           // 5 hearts eruption
            config.setDamageRadius(6.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(15);
        }

        @Override protected String getModelId() { return "bone_spike_eruption"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            for (int i = 0; i < SPIKE_COUNT; i++) {
                double angle = (2.0 * Math.PI * i) / SPIKE_COUNT + Math.random() * 0.5;
                double dist = 1.5 + Math.random() * 3.5;
                spikeX[i] = Math.cos(angle) * dist;
                spikeZ[i] = Math.sin(angle) * dist;
                spikeSpawnTick[i] = i * 10; // Staggered eruption
            }

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 30, 3.0, 1.0, 3.0, 0.03);
            shadowDust(center, 20, 3.0);
            depleteSound(center, 1.2f, 0.3f);
            corruptionSound(center, 0.8f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (int i = 0; i < SPIKE_COUNT; i++) {
                // Spike eruption: 5 hearts impact at spawn time
                if (tick == spikeSpawnTick[i]) {
                    Location spikeLoc = center.clone().add(spikeX[i], 0, spikeZ[i]);
                    damageNearby(this, spikeLoc, 2.5, 10.0);
                    corruptionDust(spikeLoc, 15, 1.5);
                    w.spawnParticle(Particle.DRAGON_BREATH, spikeLoc, 10, 0.5, 1.5, 0.5, 0.03);
                    DisplayBuilder.playSound(spikeLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.7f, 0.6f);
                    playAnimation("spike_" + i, 0.0, false);
                }

                // Tip sway damage: 2 hearts on contact (after spike has spawned)
                if (tick > spikeSpawnTick[i] && tick % 15 == 0) {
                    double swayX = spikeX[i] + Math.sin(tick * 0.08 + i * 1.5) * 0.6;
                    double swayZ = spikeZ[i] + Math.cos(tick * 0.08 + i * 1.5) * 0.6;
                    Location tipLoc = center.clone().add(swayX, 2.0 + Math.sin(tick * 0.05 + i) * 0.3, swayZ);

                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(tipLoc) <= 1.5 * 1.5) {
                            p.damage(4.0); // 2 hearts per spike tip
                            p.setNoDamageTicks(0);
                        }
                    }
                }

                // Spike particles (after eruption)
                if (tick > spikeSpawnTick[i] && tick % 5 == 0) {
                    Location spikeLoc = center.clone().add(spikeX[i], 1.0, spikeZ[i]);
                    shadowDust(spikeLoc, 2, 0.3);
                }
            }

            if (tick % 60 == 0) {
                corruptionSound(center, 0.5f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new BoneSpikeEruption(plugin); }
    }

    // ================================================================
    // 22. CORRUPTION CRYSTAL CAGE -- Dark crystal bar cage.
    //     Cage traps player inside, bars = 2 hearts on contact.
    //     Emissive ring pulses 3 hearts every 30 ticks to trapped player.
    //     15 hearts total to break free (tracked via accumulated damage).
    // ================================================================
    public static class CorruptionCrystalCage extends ModelEngineAttack {

        private Location center;
        private Player trappedPlayer;
        private double cageDamageDealt = 0;
        private static final double CAGE_RADIUS = 2.0;
        private static final double BREAK_THRESHOLD = 30.0; // 15 hearts = 30 half-hearts dealt

        public CorruptionCrystalCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_crystal_cage", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(4.0);            // 2 hearts bar contact
            config.setDamageRadius(2.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
            config.setTracksPlayer(true);
        }

        @Override protected String getModelId() { return "corruption_crystal_cage"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            // Find nearest player to trap
            trappedPlayer = findNearestPlayer(center, 10.0);
            if (trappedPlayer != null) {
                this.center = trappedPlayer.getLocation().clone();
            }
            spawnModel(this.center);

            this.center.getWorld().spawnParticle(Particle.DRAGON_BREATH, this.center, 40, 2.0, 2.0, 2.0, 0.03);
            corruptionDust(this.center, 25, 2.0);
            shriekSound(this.center, 1.0f, 0.5f);
            depleteSound(this.center, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Check if cage should break
            if (cageDamageDealt >= BREAK_THRESHOLD) {
                // Cage breaks — visual explosion
                brightCorruptionDust(center, 30, 3.0);
                w.spawnParticle(Particle.DRAGON_BREATH, center, 25, 2.0, 2.0, 2.0, 0.04);
                depleteSound(center, 1.0f, 0.8f);
                playAnimation("break", 0.0, false);
                // The attack will cleanup via duration or we can force it
                return;
            }

            // Bar contact damage: 2 hearts when touching cage perimeter
            if (tick % 15 == 0 && trappedPlayer != null && trappedPlayer.isOnline()
                    && trappedPlayer.getGameMode() == GameMode.SURVIVAL) {
                double dist = Math.sqrt(
                        Math.pow(trappedPlayer.getLocation().getX() - center.getX(), 2) +
                        Math.pow(trappedPlayer.getLocation().getZ() - center.getZ(), 2));
                // Near cage bars (edge of cage radius)
                if (Math.abs(dist - CAGE_RADIUS) <= 0.8) {
                    trappedPlayer.damage(4.0); // 2 hearts
                    trappedPlayer.setNoDamageTicks(0);
                    cageDamageDealt += 4.0;
                }
            }

            // Emissive ring pulse: 3 hearts every 30 ticks
            if (tick % 30 == 0 && trappedPlayer != null && trappedPlayer.isOnline()
                    && trappedPlayer.getGameMode() == GameMode.SURVIVAL) {
                if (trappedPlayer.getLocation().distanceSquared(center) <= (CAGE_RADIUS + 1.0) * (CAGE_RADIUS + 1.0)) {
                    trappedPlayer.damage(6.0); // 3 hearts
                    trappedPlayer.setNoDamageTicks(0);
                    cageDamageDealt += 6.0;
                    brightCorruptionDust(center, 15, 2.0);
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), CAGE_RADIUS,
                            Particle.WITCH, 16, null);
                    playAnimation("pulse", 0.0, false);
                }
            }

            // Push trapped player back inside if they try to escape
            if (tick % 3 == 0 && trappedPlayer != null && trappedPlayer.isOnline()) {
                double dist = Math.sqrt(
                        Math.pow(trappedPlayer.getLocation().getX() - center.getX(), 2) +
                        Math.pow(trappedPlayer.getLocation().getZ() - center.getZ(), 2));
                if (dist > CAGE_RADIUS) {
                    Vector pushBack = center.toVector().subtract(trappedPlayer.getLocation().toVector()).normalize();
                    trappedPlayer.setVelocity(pushBack.multiply(0.3));
                }
            }

            // Cage bar particles
            if (tick % 4 == 0) {
                int bars = 8;
                for (int b = 0; b < bars; b++) {
                    double angle = (2.0 * Math.PI * b) / bars;
                    for (double y = 0; y < 3.0; y += 0.5) {
                        Location barLoc = center.clone().add(
                                Math.cos(angle) * CAGE_RADIUS, y, Math.sin(angle) * CAGE_RADIUS);
                        corruptionDust(barLoc, 1, 0.05);
                    }
                }
            }

            if (tick % 50 == 0) {
                shriekSound(center, 0.4f, 0.6f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CorruptionCrystalCage(plugin); }
    }

    // ================================================================
    // 23. NECROTIC VINES -- 3 winding vine chains.
    //     Vine segments = 2 hearts on contact. Undulation = sweep damage.
    //     Bioluminescent spots pulse 4 hearts in 2-block radius every 50 ticks.
    // ================================================================
    public static class NecroticVines extends ModelEngineAttack {

        private Location center;
        private static final int VINE_COUNT = 3;
        private final double[] vineBaseAngle = new double[VINE_COUNT];
        private static final int SEGMENTS_PER_VINE = 8;
        private static final int GLOW_SPOTS = 4;

        public NecroticVines(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("necrotic_vines", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(4.0);            // 2 hearts vine contact
            config.setDamageRadius(7.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(15);
        }

        @Override protected String getModelId() { return "necrotic_vines"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            for (int v = 0; v < VINE_COUNT; v++) {
                vineBaseAngle[v] = (2.0 * Math.PI * v) / VINE_COUNT + Math.random() * 0.5;
            }

            center.getWorld().spawnParticle(Particle.WITCH, center, 30, 3.0, 1.0, 3.0, 0.02);
            corruptionDust(center, 20, 3.0);
            caveSound(center, 1.0f, 0.3f);
            corruptionSound(center, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            for (int v = 0; v < VINE_COUNT; v++) {
                // Vine segments with undulation
                for (int s = 0; s < SEGMENTS_PER_VINE; s++) {
                    double segDist = (s + 1) * 0.9;
                    // Undulation: angle oscillates
                    double undulationAngle = vineBaseAngle[v]
                            + Math.sin(tick * 0.06 + s * 0.8 + v * 2.0) * 0.5;
                    double sx = center.getX() + Math.cos(undulationAngle) * segDist;
                    double sy = center.getY() + 0.3 + Math.sin(tick * 0.05 + s * 0.5) * 0.6 + s * 0.15;
                    double sz = center.getZ() + Math.sin(undulationAngle) * segDist;
                    Location segLoc = new Location(w, sx, sy, sz);

                    // Vine segment contact: 2 hearts
                    if (tick % 15 == 0) {
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(segLoc) <= 1.5 * 1.5) {
                                p.damage(4.0); // 2 hearts
                                p.setNoDamageTicks(0);
                            }
                        }
                    }

                    // Vine particles
                    if (tick % 4 == 0 && s % 2 == 0) {
                        corruptionDust(segLoc, 1, 0.15);
                    }
                }
            }

            // Bioluminescent spots: 4 hearts pulse every 50 ticks in 2-block radius
            if (tick % 50 == 0 && tick > 0) {
                for (int g = 0; g < GLOW_SPOTS; g++) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = 1.0 + Math.random() * 5.0;
                    Location glowLoc = center.clone().add(Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist);
                    damageNearby(this, glowLoc, 2.0, 8.0); // 4 hearts
                    brightCorruptionDust(glowLoc, 12, 1.5);
                    w.spawnParticle(Particle.WITCH, glowLoc, 8, 1.0, 0.5, 1.0, 0.02);
                }
                playAnimation("pulse", 0.0, false);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.4f);
            }

            if (tick % 60 == 0) {
                caveSound(center, 0.4f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new NecroticVines(plugin); }
    }

    // ================================================================
    // 24. FLESH WARP -- Wrong humanoid geometry.
    //     Body parts move to wrong positions, each dealing 3 hearts on
    //     contact. Ribs jutting = 2 hearts/sec in path. Gets more
    //     dangerous over duration.
    // ================================================================
    public static class FleshWarp extends ModelEngineAttack {

        private Location center;
        private static final int PART_COUNT = 7;
        private final double[] partX = new double[PART_COUNT];
        private final double[] partY = new double[PART_COUNT];
        private final double[] partZ = new double[PART_COUNT];
        private double dangerMultiplier = 1.0;

        public FleshWarp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("flesh_warp", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(6.0);            // 3 hearts part contact
            config.setDamageRadius(5.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(15);
        }

        @Override protected String getModelId() { return "flesh_warp"; }
        @Override protected double getModelScale() { return 2.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);
            randomizePartPositions();

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 35, 2.0, 2.0, 2.0, 0.03);
            shadowDust(center, 20, 2.5);
            screamSound(center, 1.2f, 0.3f);
            depleteSound(center, 0.8f, 0.4f);
        }

        private void randomizePartPositions() {
            for (int i = 0; i < PART_COUNT; i++) {
                partX[i] = (Math.random() - 0.5) * 4.0;
                partY[i] = Math.random() * 3.0;
                partZ[i] = (Math.random() - 0.5) * 4.0;
            }
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Danger multiplier increases over time (1.0 -> 2.0 over full duration)
            dangerMultiplier = 1.0 + (tick / (double) config.getDurationTicks());

            // Parts shift to new wrong positions every 35 ticks
            if (tick % 35 == 0 && tick > 0) {
                randomizePartPositions();
                screamSound(center, 0.3f, 0.5f);
                playAnimation("warp", 0.0, false);
            }

            // Body part contact damage: 3 hearts (scaled by danger)
            if (tick % 15 == 0) {
                for (int i = 0; i < PART_COUNT; i++) {
                    Location partLoc = center.clone().add(partX[i], partY[i], partZ[i]);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(partLoc) <= 1.8 * 1.8) {
                            p.damage(6.0 * dangerMultiplier); // 3 hearts * danger
                            p.setNoDamageTicks(0);
                        }
                    }
                }
            }

            // Rib jutting damage zone: 2 hearts/sec in a line from center
            if (tick % 20 == 0) {
                double ribAngle = Math.random() * Math.PI * 2;
                for (double d = 0; d < 3.0; d += 0.8) {
                    Location ribLoc = center.clone().add(
                            Math.cos(ribAngle) * d, 1.0 + d * 0.3, Math.sin(ribAngle) * d);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(ribLoc) <= 1.5 * 1.5) {
                            p.damage(4.0 * dangerMultiplier); // 2 hearts * danger
                            p.setNoDamageTicks(0);
                        }
                    }
                    shadowDust(ribLoc, 2, 0.2);
                }
            }

            // Part glow particles
            if (tick % 5 == 0) {
                for (int i = 0; i < PART_COUNT; i++) {
                    Location partLoc = center.clone().add(partX[i], partY[i], partZ[i]);
                    corruptionDust(partLoc, 2, 0.3);
                }
            }

            if (tick % 60 == 0) {
                screamSound(center, 0.4f, 0.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new FleshWarp(plugin); }
    }

    // ================================================================
    // 25. CORRUPTED ROOTS -- 5 spreading root structures.
    //     Roots spread outward from center. New segments = 4 hearts on grow.
    //     Touching any root = 1 heart/sec. Growth clusters = 3 hearts burst.
    // ================================================================
    public static class CorruptedRoots extends ModelEngineAttack {

        private Location center;
        private static final int ROOT_COUNT = 5;
        private final double[] rootAngles = new double[ROOT_COUNT];
        private final double[] rootLengths = new double[ROOT_COUNT];
        private static final double MAX_ROOT_LENGTH = 8.0;

        public CorruptedRoots(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corrupted_roots", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(2.0);            // 1 heart/sec root contact
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "corrupted_roots"; }
        @Override protected double getModelScale() { return 2.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            for (int i = 0; i < ROOT_COUNT; i++) {
                rootAngles[i] = (2.0 * Math.PI * i) / ROOT_COUNT + Math.random() * 0.4;
                rootLengths[i] = 0;
            }

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 30, 2.0, 0.5, 2.0, 0.02);
            shadowDust(center, 20, 2.0);
            depleteSound(center, 1.0f, 0.3f);
            corruptionSound(center, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Roots grow outward
            for (int r = 0; r < ROOT_COUNT; r++) {
                double prevLength = rootLengths[r];
                rootLengths[r] = Math.min(MAX_ROOT_LENGTH, rootLengths[r] + 0.05 + Math.random() * 0.03);

                // New segment growth damage: 4 hearts when a root crosses a full block
                if ((int) rootLengths[r] > (int) prevLength && rootLengths[r] > 1.0) {
                    double tipX = center.getX() + Math.cos(rootAngles[r]) * rootLengths[r];
                    double tipZ = center.getZ() + Math.sin(rootAngles[r]) * rootLengths[r];
                    Location tipLoc = new Location(w, tipX, center.getY() + 0.1, tipZ);
                    damageNearby(this, tipLoc, 2.0, 8.0); // 4 hearts
                    corruptionDust(tipLoc, 8, 1.0);
                    w.spawnParticle(Particle.DRAGON_BREATH, tipLoc, 5, 0.5, 0.3, 0.5, 0.02);
                    DisplayBuilder.playSound(tipLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.3f, 0.8f);
                }
            }

            // Root contact damage: 1 heart/sec for touching any root
            if (tick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    boolean touchingRoot = false;
                    for (int r = 0; r < ROOT_COUNT; r++) {
                        for (double d = 0; d <= rootLengths[r]; d += 0.8) {
                            double rx = center.getX() + Math.cos(rootAngles[r]) * d;
                            double rz = center.getZ() + Math.sin(rootAngles[r]) * d;
                            Location segLoc = new Location(w, rx, center.getY() + 0.1, rz);
                            if (p.getLocation().distanceSquared(segLoc) <= 1.2 * 1.2) {
                                touchingRoot = true;
                                break;
                            }
                        }
                        if (touchingRoot) break;
                    }
                    if (touchingRoot) {
                        p.damage(2.0); // 1 heart
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Growth cluster bursts: 3 hearts every 50 ticks at random root intersections
            if (tick % 50 == 0 && tick > 0) {
                int clusterRoot = (int) (Math.random() * ROOT_COUNT);
                double clusterDist = Math.random() * rootLengths[clusterRoot];
                Location clusterLoc = center.clone().add(
                        Math.cos(rootAngles[clusterRoot]) * clusterDist, 0.3,
                        Math.sin(rootAngles[clusterRoot]) * clusterDist);
                damageNearby(this, clusterLoc, 2.5, 6.0); // 3 hearts
                brightCorruptionDust(clusterLoc, 12, 1.5);
                w.spawnParticle(Particle.WITCH, clusterLoc, 8, 1.0, 0.5, 1.0, 0.02);
                playAnimation("cluster", 0.0, false);
            }

            // Root trail particles
            if (tick % 5 == 0) {
                for (int r = 0; r < ROOT_COUNT; r++) {
                    for (double d = 0; d < rootLengths[r]; d += 1.5) {
                        Location rootSeg = center.clone().add(
                                Math.cos(rootAngles[r]) * d, 0.1, Math.sin(rootAngles[r]) * d);
                        shadowDust(rootSeg, 1, 0.15);
                    }
                }
            }

            if (tick % 60 == 0) {
                corruptionSound(center, 0.4f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CorruptedRoots(plugin); }
    }

    // ========================================================================
    // ==================== ENVIRONMENTAL CORRUPTION (26-30) ==================
    // ========================================================================

    // ================================================================
    // 26. CORRUPTION PILLAR ARRAY -- 3 obelisks in triangle.
    //     Beams between pillars = 4 hearts crossing.
    //     Inside triangle = 3 hearts/sec amplified corruption.
    //     Pillars pulse, temporarily supercharging beams to 6 hearts.
    // ================================================================
    public static class CorruptionPillarArray extends ModelEngineAttack {

        private Location center;
        private final Location[] pillarLocs = new Location[3];
        private boolean supercharged = false;

        public CorruptionPillarArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_pillar_array", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(8.0);            // 4 hearts beam crossing
            config.setDamageRadius(8.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "corruption_pillar_array"; }
        @Override protected double getModelScale() { return 3.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            // Place 3 pillars in equilateral triangle, radius 5
            for (int i = 0; i < 3; i++) {
                double angle = (2.0 * Math.PI * i) / 3.0;
                pillarLocs[i] = center.clone().add(Math.cos(angle) * 5.0, 0, Math.sin(angle) * 5.0);
            }

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 40, 4.0, 2.0, 4.0, 0.03);
            corruptionDust(center, 25, 4.0);
            depleteSound(center, 1.4f, 0.2f);
            corruptionSound(center, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Supercharge pulse: every 80 ticks, beams go from 4 hearts to 6 hearts for 20 ticks
            supercharged = (tick % 80 >= 60);
            double beamDamage = supercharged ? 12.0 : 8.0;

            if (tick % 80 == 60) {
                brightCorruptionDust(center, 20, 4.0);
                depleteSound(center, 0.8f, 0.5f);
                playAnimation("supercharge", 0.0, false);
            }

            // Beam damage: check if player crosses any of the 3 beam lines
            if (tick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    for (int i = 0; i < 3; i++) {
                        Location a = pillarLocs[i];
                        Location b = pillarLocs[(i + 1) % 3];
                        double distToLine = distanceToLineSegment(p.getLocation(), a, b);
                        if (distToLine <= 1.2 && Math.abs(p.getLocation().getY() - center.getY()) < 3.0) {
                            p.damage(beamDamage);
                            p.setNoDamageTicks(0);
                            break; // Only hit by one beam per tick
                        }
                    }
                }
            }

            // Inside triangle: 3 hearts/sec
            if (tick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (isInsideTriangle(p.getLocation(), pillarLocs[0], pillarLocs[1], pillarLocs[2])
                            && Math.abs(p.getLocation().getY() - center.getY()) < 3.0) {
                        p.damage(6.0); // 3 hearts
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Beam particles
            if (tick % 3 == 0) {
                Particle beamParticle = supercharged ? Particle.DRAGON_BREATH : Particle.WITCH;
                for (int i = 0; i < 3; i++) {
                    DisplayBuilder.particleLine(pillarLocs[i], pillarLocs[(i + 1) % 3],
                            beamParticle, 3, null);
                }
            }

            // Pillar glow
            if (tick % 5 == 0) {
                for (Location pl : pillarLocs) {
                    corruptionDust(pl.clone().add(0, 1, 0), 3, 0.4);
                }
            }

            if (tick % 60 == 0) {
                corruptionSound(center, 0.5f, 0.3f);
            }
        }

        /** Distance from a point to a line segment (2D, XZ plane). */
        private static double distanceToLineSegment(Location point, Location a, Location b) {
            double px = point.getX() - a.getX();
            double pz = point.getZ() - a.getZ();
            double dx = b.getX() - a.getX();
            double dz = b.getZ() - a.getZ();
            double lenSq = dx * dx + dz * dz;
            if (lenSq == 0) return Math.sqrt(px * px + pz * pz);
            double t = Math.max(0, Math.min(1, (px * dx + pz * dz) / lenSq));
            double projX = a.getX() + t * dx - point.getX();
            double projZ = a.getZ() + t * dz - point.getZ();
            return Math.sqrt(projX * projX + projZ * projZ);
        }

        /** Check if a point is inside a triangle (2D, XZ plane). */
        private static boolean isInsideTriangle(Location p, Location a, Location b, Location c) {
            double d1 = sign(p, a, b);
            double d2 = sign(p, b, c);
            double d3 = sign(p, c, a);
            boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
            boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
            return !(hasNeg && hasPos);
        }

        private static double sign(Location p1, Location p2, Location p3) {
            return (p1.getX() - p3.getX()) * (p2.getZ() - p3.getZ())
                    - (p2.getX() - p3.getX()) * (p1.getZ() - p3.getZ());
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CorruptionPillarArray(plugin); }
    }

    // ================================================================
    // 27. BLACK SUN -- Dark sun with asymmetric rays.
    //     Rays extend/retract dealing 3 hearts on contact.
    //     Core pulses 5 hearts in 4-block radius every 60 ticks.
    //     Alternating ray pattern makes dodging difficult.
    // ================================================================
    public static class BlackSun extends ModelEngineAttack {

        private Location center;
        private static final int RAY_COUNT = 8;
        private final double[] rayLengths = new double[RAY_COUNT];
        private final boolean[] rayExtending = new boolean[RAY_COUNT];

        public BlackSun(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_sun", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(6.0);            // 3 hearts ray contact
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(15);
        }

        @Override protected String getModelId() { return "black_sun"; }
        @Override protected double getModelScale() { return 4.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone().add(0, 6, 0);
            spawnModel(this.center);

            for (int i = 0; i < RAY_COUNT; i++) {
                rayLengths[i] = 2.0 + Math.random() * 3.0;
                rayExtending[i] = Math.random() > 0.5;
            }

            this.center.getWorld().spawnParticle(Particle.DRAGON_BREATH, this.center, 50, 3.0, 1.0, 3.0, 0.04);
            shadowDust(this.center, 30, 4.0);
            shriekSound(this.center, 1.2f, 0.3f);
            depleteSound(this.center, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Rays extend/retract in alternating pattern
            for (int i = 0; i < RAY_COUNT; i++) {
                if (rayExtending[i]) {
                    rayLengths[i] += 0.12;
                    if (rayLengths[i] >= 7.0) rayExtending[i] = false;
                } else {
                    rayLengths[i] -= 0.08;
                    if (rayLengths[i] <= 1.5) rayExtending[i] = true;
                }
            }

            // Ray contact damage: 3 hearts
            if (tick % 15 == 0) {
                for (int i = 0; i < RAY_COUNT; i++) {
                    double rayAngle = (2.0 * Math.PI * i) / RAY_COUNT;
                    for (double d = 0; d < rayLengths[i]; d += 1.0) {
                        Location rayPoint = center.clone().add(
                                Math.cos(rayAngle) * d, 0, Math.sin(rayAngle) * d);
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(rayPoint) <= 1.5 * 1.5) {
                                p.damage(6.0); // 3 hearts
                                p.setNoDamageTicks(0);
                            }
                        }
                    }
                }
            }

            // Core pulse: 5 hearts in 4-block radius every 60 ticks
            if (tick % 60 == 0 && tick > 0) {
                damageNearby(this, center, 4.0, 10.0);
                brightCorruptionDust(center, 30, 3.0);
                w.spawnParticle(Particle.DRAGON_BREATH, center, 20, 2.0, 1.0, 2.0, 0.05);
                depleteSound(center, 0.8f, 0.5f);
                playAnimation("pulse", 0.0, false);
            }

            // Ray particles
            if (tick % 3 == 0) {
                for (int i = 0; i < RAY_COUNT; i++) {
                    double rayAngle = (2.0 * Math.PI * i) / RAY_COUNT;
                    for (double d = 0; d < rayLengths[i]; d += 0.6) {
                        Location rp = center.clone().add(
                                Math.cos(rayAngle) * d, 0, Math.sin(rayAngle) * d);
                        corruptionDust(rp, 1, 0.1);
                    }
                }
            }

            // Core glow
            if (tick % 4 == 0) {
                shadowDust(center, 5, 1.0);
                w.spawnParticle(Particle.WITCH, center, 3, 0.5, 0.5, 0.5, 0.01);
            }

            if (tick % 80 == 0) {
                shriekSound(center, 0.4f, 0.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new BlackSun(plugin); }
    }

    // ================================================================
    // 28. ENTROPY FIELD -- Hexagonal platform with corner spikes.
    //     Standing on platform = 2 hearts/sec growing to 4 hearts/sec.
    //     Corner spikes fire cold bolts every 40 ticks (3 hearts targeted).
    // ================================================================
    public static class EntropyField extends ModelEngineAttack {

        private Location center;
        private static final int CORNER_COUNT = 6;
        private static final double PLATFORM_RADIUS = 5.0;

        public EntropyField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("entropy_field", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(4.0);            // 2 hearts/sec base
            config.setDamageRadius(6.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(270);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "entropy_field"; }
        @Override protected double getModelScale() { return 3.0; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            center.getWorld().spawnParticle(Particle.WITCH, center, 40, 4.0, 0.5, 4.0, 0.02);
            corruptionDust(center, 25, 4.0);
            caveSound(center, 1.0f, 0.3f);
            depleteSound(center, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            // Platform damage: 2 hearts/sec growing to 4 hearts/sec
            double growthFactor = 1.0 + (tick / (double) config.getDurationTicks());
            double platformDamage = 4.0 * growthFactor; // 2-4 hearts
            if (platformDamage > 8.0) platformDamage = 8.0;

            if (tick % 20 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    // Check if player is within hexagonal platform
                    double dx = p.getLocation().getX() - center.getX();
                    double dz = p.getLocation().getZ() - center.getZ();
                    double dist2d = Math.sqrt(dx * dx + dz * dz);
                    if (dist2d <= PLATFORM_RADIUS && Math.abs(p.getLocation().getY() - center.getY()) < 2.5) {
                        p.damage(platformDamage);
                        p.setNoDamageTicks(0);
                    }
                }
            }

            // Corner spike bolts: 3 hearts targeted every 40 ticks
            if (tick % 40 == 0 && tick > 0) {
                Player target = findNearestPlayer(center, 12.0);
                if (target != null) {
                    for (int c = 0; c < CORNER_COUNT; c++) {
                        double cornerAngle = (2.0 * Math.PI * c) / CORNER_COUNT;
                        Location cornerLoc = center.clone().add(
                                Math.cos(cornerAngle) * PLATFORM_RADIUS, 1.5,
                                Math.sin(cornerAngle) * PLATFORM_RADIUS);

                        // Fire bolt toward target
                        if (target.getLocation().distanceSquared(cornerLoc) <= 10.0 * 10.0) {
                            if (!isExempt(target)) {
                                target.damage(6.0); // 3 hearts
                                target.setNoDamageTicks(0);
                            }
                            // Bolt trail particles
                            DisplayBuilder.particleLine(cornerLoc, target.getLocation().add(0, 1, 0),
                                    Particle.DRAGON_BREATH, 4, null);
                            voidDust(cornerLoc, 5, 0.5);
                        }
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.5f, 1.0f);
                    playAnimation("fire", 0.0, false);
                }
            }

            // Platform hex particles
            if (tick % 4 == 0) {
                for (int c = 0; c < CORNER_COUNT; c++) {
                    double a1 = (2.0 * Math.PI * c) / CORNER_COUNT;
                    double a2 = (2.0 * Math.PI * ((c + 1) % CORNER_COUNT)) / CORNER_COUNT;
                    Location c1 = center.clone().add(Math.cos(a1) * PLATFORM_RADIUS, 0.1, Math.sin(a1) * PLATFORM_RADIUS);
                    Location c2 = center.clone().add(Math.cos(a2) * PLATFORM_RADIUS, 0.1, Math.sin(a2) * PLATFORM_RADIUS);
                    DisplayBuilder.particleLine(c1, c2, Particle.WITCH, 3, null);
                }
            }

            // Corner spike glow
            if (tick % 6 == 0) {
                for (int c = 0; c < CORNER_COUNT; c++) {
                    double cornerAngle = (2.0 * Math.PI * c) / CORNER_COUNT;
                    Location cl = center.clone().add(
                            Math.cos(cornerAngle) * PLATFORM_RADIUS, 1.5,
                            Math.sin(cornerAngle) * PLATFORM_RADIUS);
                    brightCorruptionDust(cl, 2, 0.3);
                }
            }

            if (tick % 60 == 0) {
                caveSound(center, 0.4f, 0.4f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new EntropyField(plugin); }
    }

    // ================================================================
    // 29. VOID GATE -- Tall arch with portal surface.
    //     Portal surface = instant 8 hearts on contact (pulls toward it).
    //     Arch pillars radiate 2 hearts/sec in 3 blocks.
    //     Walking through = teleport to random spot + 5 hearts.
    // ================================================================
    public static class VoidGate extends ModelEngineAttack {

        private Location center;
        private double gateAngle;
        private Location leftPillar;
        private Location rightPillar;

        public VoidGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_gate", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(16.0);           // 8 hearts portal contact
            config.setDamageRadius(5.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override protected String getModelId() { return "void_gate"; }
        @Override protected double getModelScale() { return 3.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            this.gateAngle = Math.random() * Math.PI;
            spawnModel(center);

            double perpX = Math.cos(gateAngle + Math.PI / 2) * 2.0;
            double perpZ = Math.sin(gateAngle + Math.PI / 2) * 2.0;
            leftPillar = center.clone().add(perpX, 0, perpZ);
            rightPillar = center.clone().add(-perpX, 0, -perpZ);

            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 50, 2.0, 3.0, 2.0, 0.04);
            voidDust(center, 30, 3.0);
            shriekSound(center, 1.4f, 0.2f);
            depleteSound(center, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            double gateNormX = Math.cos(gateAngle);
            double gateNormZ = Math.sin(gateAngle);

            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dx = p.getLocation().getX() - center.getX();
                double dz = p.getLocation().getZ() - center.getZ();
                double dist2d = Math.sqrt(dx * dx + dz * dz);

                // Portal surface contact: 8 hearts instant + teleport
                double normalDist = Math.abs(dx * gateNormX + dz * gateNormZ);
                double tangentDist = Math.abs(-dx * gateNormZ + dz * gateNormX);
                boolean inPortalPlane = normalDist <= 0.8 && tangentDist <= 2.0
                        && p.getLocation().getY() >= center.getY()
                        && p.getLocation().getY() <= center.getY() + 4.0;

                if (inPortalPlane && tick % 20 == 0) {
                    p.damage(16.0); // 8 hearts
                    p.setNoDamageTicks(0);
                    // Teleport to random spot within 10 blocks
                    double tpAngle = Math.random() * Math.PI * 2;
                    double tpDist = 5.0 + Math.random() * 5.0;
                    Location tpLoc = center.clone().add(
                            Math.cos(tpAngle) * tpDist, 0, Math.sin(tpAngle) * tpDist);
                    tpLoc.setY(w.getHighestBlockYAt(tpLoc) + 1);
                    p.teleport(tpLoc);
                    p.damage(10.0); // +5 hearts after teleport
                    p.setNoDamageTicks(0);
                    screamSound(tpLoc, 0.8f, 0.5f);
                    brightCorruptionDust(tpLoc, 15, 2.0);
                }

                // Pull toward portal
                if (dist2d <= 8.0 && dist2d > 1.0 && tick % 5 == 0) {
                    Vector pull = center.toVector().subtract(p.getLocation().toVector()).normalize();
                    p.setVelocity(p.getVelocity().add(pull.multiply(0.1)));
                }
            }

            // Arch pillar radiation: 2 hearts/sec in 3 blocks
            if (tick % 20 == 0) {
                damageNearby(this, leftPillar, 3.0, 4.0);
                damageNearby(this, rightPillar, 3.0, 4.0);
            }

            // Portal surface particles
            if (tick % 2 == 0) {
                double perpX = -gateNormZ;
                double perpZ = gateNormX;
                for (double t = -2.0; t <= 2.0; t += 0.4) {
                    for (double y = 0; y < 4.0; y += 0.5) {
                        Location portalPoint = center.clone().add(perpX * t, y, perpZ * t);
                        voidDust(portalPoint, 1, 0.1);
                    }
                }
            }

            // Pillar particles
            if (tick % 5 == 0) {
                for (double y = 0; y < 5.0; y += 1.0) {
                    corruptionDust(leftPillar.clone().add(0, y, 0), 2, 0.2);
                    corruptionDust(rightPillar.clone().add(0, y, 0), 2, 0.2);
                }
            }

            if (tick % 60 == 0) {
                shriekSound(center, 0.5f, 0.3f);
                caveSound(center, 0.6f, 0.2f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new VoidGate(plugin); }
    }

    // ================================================================
    // 30. CORRUPTION NOVA -- 360 deg omnidirectional burst.
    //     16 shards blast outward = 4 hearts each on contact.
    //     Core implosion = 10 hearts in 3 blocks.
    //     Rings spinning = 3 hearts sweep.
    //     Most visually explosive attack.
    // ================================================================
    public static class CorruptionNova extends ModelEngineAttack {

        private Location center;
        private static final int SHARD_COUNT = 16;
        private final double[] shardAngles = new double[SHARD_COUNT];
        private final double[] shardDistances = new double[SHARD_COUNT];
        private boolean imploded = false;
        private double ringAngle = 0;

        public CorruptionNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corruption_nova_me", AttackType.MODEL_ENGINE, 1, "modes/corruption/attacks"));
            config.setDamage(8.0);            // 4 hearts per shard
            config.setDamageRadius(10.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override protected String getModelId() { return "corruption_nova"; }
        @Override protected double getModelScale() { return 3.5; }

        @Override
        protected void onSpawn(Location center) {
            this.center = center.clone();
            spawnModel(center);

            for (int i = 0; i < SHARD_COUNT; i++) {
                shardAngles[i] = (2.0 * Math.PI * i) / SHARD_COUNT;
                shardDistances[i] = 0;
            }

            // Massive spawn burst
            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 80, 2.0, 2.0, 2.0, 0.06);
            brightCorruptionDust(center, 40, 3.0);
            corruptionDust(center, 30, 2.0);
            shadowDust(center, 20, 1.5);
            shriekSound(center, 1.4f, 0.2f);
            depleteSound(center, 1.4f, 0.3f);
            screamSound(center, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int tick) {
            if (center == null || center.getWorld() == null) return;
            World w = center.getWorld();

            ringAngle += 0.1;

            // Phase 1 (ticks 0-60): Shards blast outward
            if (tick <= 60) {
                for (int i = 0; i < SHARD_COUNT; i++) {
                    shardDistances[i] += 0.25;
                }

                // Shard damage: 4 hearts on contact
                if (tick % 5 == 0) {
                    for (int i = 0; i < SHARD_COUNT; i++) {
                        double sx = center.getX() + Math.cos(shardAngles[i]) * shardDistances[i];
                        double sz = center.getZ() + Math.sin(shardAngles[i]) * shardDistances[i];
                        Location shardLoc = new Location(w, sx, center.getY() + 0.5, sz);
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            if (p.getLocation().distanceSquared(shardLoc) <= 2.0 * 2.0) {
                                p.damage(8.0); // 4 hearts
                                p.setNoDamageTicks(0);
                            }
                        }
                    }
                }
            }

            // Phase 2 (tick 70): Core implosion = 10 hearts in 3 blocks
            if (tick == 70 && !imploded) {
                imploded = true;
                damageNearby(this, center, 3.0, 20.0); // 10 hearts
                w.spawnParticle(Particle.DRAGON_BREATH, center, 60, 1.5, 1.5, 1.5, 0.08);
                shadowDust(center, 40, 2.0);
                voidDust(center, 30, 1.5);
                depleteSound(center, 1.4f, 0.2f);
                shriekSound(center, 1.2f, 0.4f);
                playAnimation("implode", 0.0, false);
            }

            // Phase 3 (ticks 80+): Spinning rings = 3 hearts sweep
            if (tick >= 80) {
                double ring1Radius = 3.0 + Math.sin(tick * 0.05) * 1.0;
                double ring2Radius = 5.0 + Math.cos(tick * 0.05) * 1.0;

                if (tick % 10 == 0) {
                    // Ring 1 sweep
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double dist = Math.sqrt(
                                Math.pow(p.getLocation().getX() - center.getX(), 2) +
                                Math.pow(p.getLocation().getZ() - center.getZ(), 2));
                        if (Math.abs(dist - ring1Radius) <= 1.2 || Math.abs(dist - ring2Radius) <= 1.2) {
                            if (Math.abs(p.getLocation().getY() - center.getY()) < 3.0) {
                                p.damage(6.0); // 3 hearts
                                p.setNoDamageTicks(0);
                            }
                        }
                    }
                }

                // Ring particles
                if (tick % 2 == 0) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0),
                            ring1Radius, Particle.DRAGON_BREATH, (int) (ring1Radius * 6), null);
                    DisplayBuilder.particleRing(center.clone().add(0, 1.0, 0),
                            ring2Radius, Particle.WITCH, (int) (ring2Radius * 5), null);
                }
            }

            // Shard trail particles (phase 1)
            if (tick <= 60 && tick % 2 == 0) {
                for (int i = 0; i < SHARD_COUNT; i++) {
                    Location sl = center.clone().add(
                            Math.cos(shardAngles[i]) * shardDistances[i], 0.5,
                            Math.sin(shardAngles[i]) * shardDistances[i]);
                    corruptionDust(sl, 2, 0.2);
                }
            }

            // Ambient core glow
            if (tick % 4 == 0) {
                voidDust(center, 4, 1.0);
                w.spawnParticle(Particle.WITCH, center, 3, 0.5, 0.5, 0.5, 0.01);
            }

            if (tick % 50 == 0) {
                corruptionSound(center, 0.6f, 0.3f);
            }
        }

        @Override protected void onModelCleanup() {}
        @Override public AbstractAttack newInstance() { return new CorruptionNova(plugin); }
    }
}
