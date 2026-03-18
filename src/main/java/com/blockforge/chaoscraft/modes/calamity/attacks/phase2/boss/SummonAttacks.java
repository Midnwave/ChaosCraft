package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.boss;

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
 * Phase 2 Boss Attacks — GROUP 4: SUMMON ATTACKS (#31-40)
 * Spawning cosmic sentinel entities, crystal guardians, segment clones.
 * Crystalline summoned constructs that persist as hazards.
 * NO status effects — damage only.
 */
public final class SummonAttacks {

    private SummonAttacks() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CosmicSentinelSpawn(plugin));
        registry.register(new CrystalGuardianEmergence(plugin));
        registry.register(new SegmentCloneDetach(plugin));
        registry.register(new CosmicSwarmRelease(plugin));
        registry.register(new RiftAnchorSentinels(plugin));
        registry.register(new SpineSplinterCluster(plugin));
        registry.register(new ConstellationArray(plugin));
        registry.register(new GuardianTrioFormation(plugin));
        registry.register(new CelestialHorrorSummon(plugin));
        registry.register(new TailFragmentBomb(plugin));
    }

    // ================================================================
    // 31. COSMIC SENTINEL SPAWN — Spatial rift spawns hostile sentinels
    // ================================================================
    public static class CosmicSentinelSpawn extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> sentinelHandles = new ArrayList<>();
        private boolean sentinelsSpawned = false;

        public CosmicSentinelSpawn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_cosmic_sentinel_spawn", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spatial tear forming — vertical rift of portal particles
            Location riftLoc = center.clone().add(10, 5, 0);
            for (int y = 0; y <= 4; y++) {
                BlockDisplayHandle rift = displayBuilder.spawnBlock(
                    riftLoc.clone().add(0, y - 2, 0), Material.CRYING_OBSIDIAN);
                rift.scale(0.1f, 0.8f, 0.1f).glow(128, 0, 255).interpolation(4, 0);
                riftHandles.add(rift);
                spawnedEntities.add(rift.entity());
            }
            DisplayBuilder.cyanDust(riftLoc, 15, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rift widens from hairline to 2-block opening (0-30 ticks)
            if (ticksAlive < 30 && !sentinelsSpawned) {
                float width = 0.1f + (ticksAlive / 30.0f) * 1.0f;
                for (BlockDisplayHandle h : riftHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-width / 2, -0.4f, -0.05f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(width, 0.8f, 0.1f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(10, 5, 0), 6, 2.0);
                }
            }
            // Sentinels emerge (tick 30)
            else if (ticksAlive == 30 && !sentinelsSpawned) {
                sentinelsSpawned = true;
                Location riftCenter = center.clone().add(10, 5, 0);
                // Spawn 2 sentinel display constructs
                for (int s = 0; s < 2; s++) {
                    Location sentLoc = riftCenter.clone().add(s * 3 - 1.5, 0, 0);
                    // Sentinel body: floating crystal entity
                    BlockDisplayHandle body = displayBuilder.spawnBlock(sentLoc, Material.AMETHYST_BLOCK);
                    body.scale(0.8f, 1.2f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                    sentinelHandles.add(body);
                    spawnedEntities.add(body.entity());
                    // Sentinel core glow
                    BlockDisplayHandle core = displayBuilder.spawnBlock(sentLoc.clone().add(0, 0.5, 0),
                        Material.SEA_LANTERN);
                    core.scale(0.4f, 0.4f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                    sentinelHandles.add(core);
                    spawnedEntities.add(core.entity());
                }
                DisplayBuilder.cyanDust(riftCenter, 25, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.8f);
            }
            // Sentinels drift around the arena (30+ ticks)
            else if (sentinelsSpawned) {
                int driftTick = ticksAlive - 30;
                for (int s = 0; s < 2 && s * 2 < sentinelHandles.size(); s++) {
                    double angle = driftTick * 0.04 + s * Math.PI;
                    double driftR = 6 + Math.sin(driftTick * 0.02) * 3;
                    Location driftLoc = center.clone().add(
                        Math.cos(angle) * driftR, 4 + Math.sin(driftTick * 0.05) * 1.5, Math.sin(angle) * driftR);
                    sentinelHandles.get(s * 2).entity().teleport(driftLoc);
                    sentinelHandles.get(s * 2 + 1).entity().teleport(driftLoc.clone().add(0, 0.5, 0));
                }
                // Sentinel bolt particles every 40 ticks (2 seconds)
                if (driftTick % 40 == 0 && driftTick > 0) {
                    for (int s = 0; s < 2 && s * 2 < sentinelHandles.size(); s++) {
                        DisplayBuilder.cyanDust(sentinelHandles.get(s * 2).entity().getLocation(), 8, 2.0);
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicSentinelSpawn(plugin); }
    }

    // ================================================================
    // 32. CRYSTAL GUARDIAN EMERGENCE — 3 stationary crystal turrets
    // ================================================================
    public static class CrystalGuardianEmergence extends BossAttack {
        private final List<BlockDisplayHandle> guardianHandles = new ArrayList<>();
        private boolean guardiansActive = false;
        private int guardianTick = 0;

        public CrystalGuardianEmergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_crystal_guardian_emergence", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 ground impact points glowing
            double[][] positions = {{-8, 6}, {8, 6}, {0, -10}};
            for (double[] pos : positions) {
                Location glowLoc = center.clone().add(pos[0], 0.1, pos[1]);
                BlockDisplayHandle glow = displayBuilder.spawnBlock(glowLoc, Material.SEA_LANTERN);
                glow.scale(2.0f, 0.15f, 2.0f).glow(0, 200, 255).interpolation(4, 0);
                guardianHandles.add(glow);
                spawnedEntities.add(glow.entity());
            }
            DisplayBuilder.cyanDust(center, 15, 10.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ground glow telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !guardiansActive) {
                if (ticksAlive % 8 == 0) {
                    double[][] positions = {{-8, 6}, {8, 6}, {0, -10}};
                    for (double[] pos : positions) {
                        DisplayBuilder.cyanDust(center.clone().add(pos[0], 0.5, pos[1]), 6, 2.0);
                    }
                }
            }
            // Guardians erupt (tick 40)
            else if (ticksAlive == 40 && !guardiansActive) {
                guardiansActive = true;
                guardianTick = 0;
                double[][] positions = {{-8, 6}, {8, 6}, {0, -10}};
                for (double[] pos : positions) {
                    Location guardLoc = center.clone().add(pos[0], 0, pos[1]);
                    // Crystal formation — stacked rotating crystals
                    for (int y = 0; y <= 3; y++) {
                        BlockDisplayHandle crystal = displayBuilder.spawnBlock(
                            guardLoc.clone().add(0, y, 0), Material.AMETHYST_CLUSTER);
                        float scale = 1.2f - y * 0.2f;
                        crystal.scale(scale, 0.8f, scale).glow(128, 0, 255).interpolation(2, 0);
                        guardianHandles.add(crystal);
                        spawnedEntities.add(crystal.entity());
                    }
                    DisplayBuilder.cyanDust(guardLoc, 15, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.2f);
            }
            // Guardians fire shard volleys every 60 ticks (3 seconds)
            else if (guardiansActive) {
                guardianTick++;
                if (guardianTick % 60 == 0) {
                    double[][] positions = {{-8, 6}, {8, 6}, {0, -10}};
                    for (double[] pos : positions) {
                        Location guardLoc = center.clone().add(pos[0], 2, pos[1]);
                        // 4 crystal shards in plus-pattern (N/S/E/W)
                        double[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                        for (double[] dir : dirs) {
                            for (int seg = 1; seg <= 6; seg++) {
                                Location shardLoc = guardLoc.clone().add(dir[0] * seg * 1.5, 0, dir[1] * seg * 1.5);
                                BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc,
                                    Material.AMETHYST_CLUSTER);
                                shard.scale(0.3f, 0.5f, 0.3f).glow(0, 200, 255).interpolation(1, 0);
                                guardianHandles.add(shard);
                                spawnedEntities.add(shard.entity());
                            }
                        }
                        DisplayBuilder.cyanDust(guardLoc, 12, 3.0);
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalGuardianEmergence(plugin); }
    }

    // ================================================================
    // 33. SEGMENT CLONE DETACH — Independent mini-worm segments
    // ================================================================
    public static class SegmentCloneDetach extends BossAttack {
        private final List<BlockDisplayHandle> cloneHandles = new ArrayList<>();
        private boolean clonesDetached = false;
        private double[] cloneAngles = {0, Math.PI};

        public SegmentCloneDetach(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_segment_clone_detach", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(500);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Mid-section segments glowing brighter
            for (int s = 0; s < 2; s++) {
                Location segLoc = center.clone().add(-2 + s * 4, 6, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                seg.scale(1.2f, 1.0f, 1.2f).glow(0, 200, 255).interpolation(3, 0);
                cloneHandles.add(seg);
                spawnedEntities.add(seg.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 10, 4.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Brightening telegraph (0-20 ticks)
            if (ticksAlive < 20 && !clonesDetached) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 6, 3.0);
                }
            }
            // Detach (tick 20) — spark flash at separation
            else if (ticksAlive == 20 && !clonesDetached) {
                clonesDetached = true;
                // Add trailing segments for each clone
                for (int clone = 0; clone < 2; clone++) {
                    for (int tail = 1; tail <= 3; tail++) {
                        BlockDisplayHandle tailSeg = displayBuilder.spawnBlock(
                            center.clone().add(-2 + clone * 4, 6, tail * 0.8),
                            Material.DARK_PRISMARINE);
                        float scale = 1.0f - tail * 0.15f;
                        tailSeg.scale(scale, 0.8f, scale).glow(0, 200, 255).interpolation(1, 0);
                        cloneHandles.add(tailSeg);
                        spawnedEntities.add(tailSeg.entity());
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 20, 4.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 1.0f);
            }
            // Clones track independently (20-260 ticks = 12 seconds)
            else if (clonesDetached) {
                int cloneTick = ticksAlive - 20;
                if (cloneTick > 240) return;

                for (int clone = 0; clone < 2; clone++) {
                    cloneAngles[clone] += 0.06; // 70% DoG speed
                    double r = 8 + Math.sin(cloneTick * 0.03 + clone * 2) * 4;
                    double x = Math.cos(cloneAngles[clone]) * r;
                    double z = Math.sin(cloneAngles[clone]) * r;
                    double y = 4 + Math.sin(cloneTick * 0.05) * 1.5;

                    // Update head
                    cloneHandles.get(clone).entity().teleport(center.clone().add(x, y, z));

                    // Update tail segments
                    for (int tail = 0; tail < 3; tail++) {
                        int tailIdx = 2 + clone * 3 + tail;
                        if (tailIdx < cloneHandles.size()) {
                            double tailAngle = cloneAngles[clone] - (tail + 1) * 0.15;
                            double tailR = r - (tail + 1) * 0.3;
                            cloneHandles.get(tailIdx).entity().teleport(
                                center.clone().add(
                                    Math.cos(tailAngle) * tailR, y - tail * 0.2,
                                    Math.sin(tailAngle) * tailR));
                        }
                    }
                }

                // Trail particles
                if (cloneTick % 8 == 0) {
                    for (int clone = 0; clone < 2; clone++) {
                        DisplayBuilder.cyanDust(cloneHandles.get(clone).entity().getLocation(), 4, 1.0);
                    }
                }

                // Dissolve at end
                if (cloneTick == 240) {
                    for (BlockDisplayHandle h : cloneHandles) {
                        DisplayBuilder.cyanDust(h.entity().getLocation(), 8, 1.5);
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SegmentCloneDetach(plugin); }
    }

    // ================================================================
    // 34. COSMIC SWARM RELEASE — 12 homing motes from DoG's mouth
    // ================================================================
    public static class CosmicSwarmRelease extends BossAttack {
        private final List<BlockDisplayHandle> moteHandles = new ArrayList<>();
        private final double[][] moteVectors = new double[12][2];
        private boolean swarmReleased = false;
        private boolean motesHoming = false;

        public CosmicSwarmRelease(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_cosmic_swarm_release", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Mouth glow building with coalescing light points
            BlockDisplayHandle mouthGlow = displayBuilder.spawnBlock(
                center.clone().add(-10, 8, 0), Material.SEA_LANTERN);
            mouthGlow.scale(1.5f, 1.5f, 1.5f).glow(0, 200, 255).interpolation(4, 0);
            moteHandles.add(mouthGlow);
            spawnedEntities.add(mouthGlow.entity());
            DisplayBuilder.cyanDust(center.clone().add(-10, 8, 0), 12, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge-up with forming motes (0-30 ticks)
            if (ticksAlive < 30 && !swarmReleased) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-10, 8, 0), 8, 2.0);
                }
            }
            // Release swarm (tick 30) — 12 motes scatter outward
            else if (ticksAlive == 30 && !swarmReleased) {
                swarmReleased = true;
                Location mouthLoc = center.clone().add(-10, 8, 0);
                for (int i = 0; i < 12; i++) {
                    double scatterAngle = Math.random() * Math.PI * 2;
                    moteVectors[i][0] = Math.cos(scatterAngle);
                    moteVectors[i][1] = Math.sin(scatterAngle);
                    BlockDisplayHandle mote = displayBuilder.spawnBlock(mouthLoc.clone(),
                        Material.AMETHYST_CLUSTER);
                    mote.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(1, 0);
                    moteHandles.add(mote);
                    spawnedEntities.add(mote.entity());
                }
                DisplayBuilder.playSound(mouthLoc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.5f);
            }
            // Scatter phase (30-40 ticks = 0.5 seconds)
            else if (swarmReleased && !motesHoming && ticksAlive < 40) {
                int scatterTick = ticksAlive - 30;
                for (int i = 1; i <= 12 && i < moteHandles.size(); i++) {
                    double dist = scatterTick * 0.6;
                    moteHandles.get(i).entity().teleport(
                        center.clone().add(
                            -10 + moteVectors[i - 1][0] * dist,
                            8,
                            moteVectors[i - 1][1] * dist));
                }
            }
            // Homing phase (40+ ticks)
            else if (swarmReleased) {
                if (!motesHoming) {
                    motesHoming = true;
                    DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 1.5f);
                }
                int homeTick = ticksAlive - 40;
                if (homeTick > 120) return; // Despawn after 6 seconds

                for (int i = 1; i <= 12 && i < moteHandles.size(); i++) {
                    Location moteLoc = moteHandles.get(i).entity().getLocation();
                    // Home toward center at 10 blocks/sec with narrow turn radius
                    double dx = center.getX() - moteLoc.getX();
                    double dy = 2 - moteLoc.getY();
                    double dz = center.getZ() - moteLoc.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0.5) {
                        double turnRate = 0.1;
                        moteVectors[i - 1][0] += (dx / dist) * turnRate;
                        moteVectors[i - 1][1] += (dz / dist) * turnRate;
                        double mag = Math.sqrt(moteVectors[i - 1][0] * moteVectors[i - 1][0]
                            + moteVectors[i - 1][1] * moteVectors[i - 1][1]);
                        if (mag > 0) {
                            moteVectors[i - 1][0] = (moteVectors[i - 1][0] / mag) * 0.5;
                            moteVectors[i - 1][1] = (moteVectors[i - 1][1] / mag) * 0.5;
                        }
                        moteHandles.get(i).entity().teleport(
                            moteLoc.clone().add(moteVectors[i - 1][0], dy * 0.05, moteVectors[i - 1][1]));
                    }
                }
                if (homeTick % 8 == 0) {
                    for (int i = 1; i <= 12 && i < moteHandles.size(); i++) {
                        DisplayBuilder.cyanDust(moteHandles.get(i).entity().getLocation(), 2, 0.3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicSwarmRelease(plugin); }
    }

    // ================================================================
    // 35. RIFT ANCHOR SENTINELS — Persistent sentinel spawning rifts
    // ================================================================
    public static class RiftAnchorSentinels extends BossAttack {
        private final List<BlockDisplayHandle> anchorHandles = new ArrayList<>();
        private boolean anchorsPlaced = false;
        private int lastSpawnTick = 0;
        private int sentinelCount = 0;

        public RiftAnchorSentinels(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_rift_anchor_sentinels", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 2 expanding portal circles on ground
            for (int anchor = 0; anchor < 2; anchor++) {
                float offsetX = anchor == 0 ? -8 : 8;
                Location groundLoc = center.clone().add(offsetX, 0.1, 0);
                BlockDisplayHandle circle = displayBuilder.spawnBlock(groundLoc, Material.CRYING_OBSIDIAN);
                circle.scale(2.0f, 0.1f, 2.0f).glow(128, 0, 255).interpolation(4, 0);
                anchorHandles.add(circle);
                spawnedEntities.add(circle.entity());
            }
            DisplayBuilder.cyanDust(center, 12, 10.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ground markers pulse (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !anchorsPlaced) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-8, 0.5, 0), 5, 2.0);
                    DisplayBuilder.cyanDust(center.clone().add(8, 0.5, 0), 5, 2.0);
                }
            }
            // Anchors form (tick 40) — vertical rift pillars
            else if (ticksAlive == 40 && !anchorsPlaced) {
                anchorsPlaced = true;
                lastSpawnTick = ticksAlive;
                for (int anchor = 0; anchor < 2; anchor++) {
                    float offsetX = anchor == 0 ? -8 : 8;
                    Location anchorLoc = center.clone().add(offsetX, 0, 0);
                    // Vertical rift pillar
                    for (int y = 0; y <= 6; y++) {
                        BlockDisplayHandle rift = displayBuilder.spawnBlock(
                            anchorLoc.clone().add(0, y * 0.5, 0), Material.CRYING_OBSIDIAN);
                        rift.scale(0.6f, 0.5f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                        anchorHandles.add(rift);
                        spawnedEntities.add(rift.entity());
                    }
                    // Core glow
                    BlockDisplayHandle core = displayBuilder.spawnBlock(
                        anchorLoc.clone().add(0, 1.5, 0), Material.SEA_LANTERN);
                    core.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
                    anchorHandles.add(core);
                    spawnedEntities.add(core.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.6f);
            }
            // Anchors spawn sentinels every 120 ticks (6 seconds)
            else if (anchorsPlaced && ticksAlive - lastSpawnTick >= 120 && sentinelCount < 4) {
                lastSpawnTick = ticksAlive;
                sentinelCount++;
                for (int anchor = 0; anchor < 2; anchor++) {
                    float offsetX = anchor == 0 ? -8 : 8;
                    Location spawnLoc = center.clone().add(offsetX, 3, 0);
                    BlockDisplayHandle sentinel = displayBuilder.spawnBlock(spawnLoc, Material.AMETHYST_BLOCK);
                    sentinel.scale(0.6f, 0.8f, 0.6f).glow(0, 200, 255).interpolation(2, 0);
                    anchorHandles.add(sentinel);
                    spawnedEntities.add(sentinel.entity());
                    DisplayBuilder.cyanDust(spawnLoc, 10, 1.5);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
            // Pulse anchor cores
            if (anchorsPlaced && ticksAlive % 20 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(-8, 1.5, 0), 4, 1.0);
                DisplayBuilder.cyanDust(center.clone().add(8, 1.5, 0), 4, 1.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftAnchorSentinels(plugin); }
    }

    // ================================================================
    // 36. SPINE SPLINTER CLUSTER — Sequential vertical spine eruptions
    // ================================================================
    public static class SpineSplinterCluster extends BossAttack {
        private final List<BlockDisplayHandle> spineHandles = new ArrayList<>();
        private int eruptionsTriggered = 0;
        private int lastEruptionTick = 0;

        public SpineSplinterCluster(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_spine_splinter_cluster", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 5 body segments with running light indicator
            for (int i = 0; i < 5; i++) {
                Location segLoc = center.clone().add(-8 + i * 4, 6, 0);
                BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                seg.scale(1.0f, 0.8f, 1.0f).glow(0, 200, 255).interpolation(3, 0);
                spineHandles.add(seg);
                spawnedEntities.add(seg.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Running light telegraph (0-24 ticks)
            if (ticksAlive < 24) {
                int litSeg = (ticksAlive / 5) % 5;
                for (int i = 0; i < 5 && i < spineHandles.size(); i++) {
                    if (i == litSeg) {
                        DisplayBuilder.cyanDust(spineHandles.get(i).entity().getLocation(), 6, 1.0);
                    }
                }
            }
            // Sequential eruptions: one every 6 ticks, 5 total (tick 24+)
            else if (eruptionsTriggered < 5 && ticksAlive - lastEruptionTick >= 6) {
                lastEruptionTick = ticksAlive;
                Location eruptLoc = center.clone().add(-8 + eruptionsTriggered * 4, 6, 0);

                // Vertical crystal spine eruption — 4 blocks upward
                for (int y = 0; y <= 4; y++) {
                    BlockDisplayHandle spine = displayBuilder.spawnBlock(
                        eruptLoc.clone().add(0, y, 0), Material.AMETHYST_CLUSTER);
                    spine.scale(0.5f, 0.8f, 0.5f).glow(128, 0, 255).interpolation(1, 0);
                    spineHandles.add(spine);
                    spawnedEntities.add(spine.entity());
                }
                DisplayBuilder.cyanDust(eruptLoc, 12, 2.0);
                DisplayBuilder.playSound(eruptLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);
                eruptionsTriggered++;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpineSplinterCluster(plugin); }
    }

    // ================================================================
    // 37. CONSTELLATION ARRAY — 6 nodes forming a laser web
    // ================================================================
    public static class ConstellationArray extends BossAttack {
        private final List<BlockDisplayHandle> nodeHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> webHandles = new ArrayList<>();
        private int nodesPlaced = 0;
        private boolean webActive = false;

        public ConstellationArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_constellation_array", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.cyanDust(center, 8, 5.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Place 6 nodes one at a time every 10 ticks as DoG passes
            if (nodesPlaced < 6 && ticksAlive % 10 == 0 && ticksAlive > 0) {
                double angle = (Math.PI * 2 / 6) * nodesPlaced;
                double r = 8 + (nodesPlaced % 2) * 4; // Alternating radii for interesting web
                Location nodeLoc = center.clone().add(Math.cos(angle) * r, 2, Math.sin(angle) * r);
                BlockDisplayHandle node = displayBuilder.spawnBlock(nodeLoc, Material.SEA_LANTERN);
                node.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(3, 0);
                nodeHandles.add(node);
                spawnedEntities.add(node.entity());
                DisplayBuilder.cyanDust(nodeLoc, 8, 1.0);
                DisplayBuilder.playSound(nodeLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.0f + nodesPlaced * 0.15f);
                nodesPlaced++;
            }
            // Web activates when all 6 nodes are placed
            else if (nodesPlaced >= 6 && !webActive) {
                webActive = true;
                // Create beam connections between all node pairs (15 connections)
                for (int a = 0; a < 6; a++) {
                    for (int b = a + 1; b < 6; b++) {
                        Location locA = nodeHandles.get(a).entity().getLocation();
                        Location locB = nodeHandles.get(b).entity().getLocation();
                        double dx = locB.getX() - locA.getX();
                        double dy = locB.getY() - locA.getY();
                        double dz = locB.getZ() - locA.getZ();
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        int segs = Math.max(3, (int) (dist / 1.5));

                        for (int s = 0; s <= segs; s++) {
                            float t = s / (float) segs;
                            Location segLoc = locA.clone().add(dx * t, dy * t, dz * t);
                            BlockDisplayHandle beam = displayBuilder.spawnBlock(segLoc, Material.END_ROD);
                            beam.scale(0.08f, 0.08f, 0.3f).glow(0, 200, 255).interpolation(1, 0);
                            webHandles.add(beam);
                            spawnedEntities.add(beam.entity());
                        }
                    }
                }
                DisplayBuilder.cyanDust(center, 25, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 1.0f);
            }
            // Web persists and pulses (160 ticks = 8 seconds)
            else if (webActive && ticksAlive % 20 == 0) {
                for (BlockDisplayHandle node : nodeHandles) {
                    DisplayBuilder.cyanDust(node.entity().getLocation(), 4, 0.8);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConstellationArray(plugin); }
    }

    // ================================================================
    // 38. GUARDIAN TRIO FORMATION — Triangular crystal turret trap
    // ================================================================
    public static class GuardianTrioFormation extends BossAttack {
        private final List<BlockDisplayHandle> guardianHandles = new ArrayList<>();
        private boolean trioActive = false;
        private int trioTick = 0;

        public GuardianTrioFormation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_guardian_trio_formation", AttackType.BOSS, 2), "dog");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Converging triangular ground markers
            double[][] triPositions = {
                {Math.cos(Math.PI / 6) * 8, Math.sin(Math.PI / 6) * 8},
                {Math.cos(5 * Math.PI / 6) * 8, Math.sin(5 * Math.PI / 6) * 8},
                {Math.cos(3 * Math.PI / 2) * 8, Math.sin(3 * Math.PI / 2) * 8}
            };
            for (double[] pos : triPositions) {
                Location markerLoc = center.clone().add(pos[0], 0.1, pos[1]);
                BlockDisplayHandle marker = displayBuilder.spawnBlock(markerLoc, Material.SEA_LANTERN);
                marker.scale(1.5f, 0.1f, 1.5f).glow(0, 200, 255).interpolation(4, 0);
                guardianHandles.add(marker);
                spawnedEntities.add(marker.entity());
            }
            DisplayBuilder.cyanDust(center, 15, 8.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Triangle markers converge (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !trioActive) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center, 8, 8.0);
                }
            }
            // Guardians spawn (tick 40)
            else if (ticksAlive == 40 && !trioActive) {
                trioActive = true;
                trioTick = 0;
                double[][] triPositions = {
                    {Math.cos(Math.PI / 6) * 8, Math.sin(Math.PI / 6) * 8},
                    {Math.cos(5 * Math.PI / 6) * 8, Math.sin(5 * Math.PI / 6) * 8},
                    {Math.cos(3 * Math.PI / 2) * 8, Math.sin(3 * Math.PI / 2) * 8}
                };
                for (double[] pos : triPositions) {
                    Location guardLoc = center.clone().add(pos[0], 0, pos[1]);
                    // Crystal guardian turret
                    for (int y = 0; y <= 3; y++) {
                        BlockDisplayHandle crystal = displayBuilder.spawnBlock(
                            guardLoc.clone().add(0, y, 0), Material.AMETHYST_CLUSTER);
                        float scale = 1.0f - y * 0.15f;
                        crystal.scale(scale, 0.7f, scale).glow(128, 0, 255).interpolation(2, 0);
                        guardianHandles.add(crystal);
                        spawnedEntities.add(crystal.entity());
                    }
                    DisplayBuilder.cyanDust(guardLoc, 12, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
            }
            // Guardians fire synchronized shards every 60 ticks
            else if (trioActive) {
                trioTick++;
                if (trioTick % 60 == 0) {
                    double[][] triPositions = {
                        {Math.cos(Math.PI / 6) * 8, Math.sin(Math.PI / 6) * 8},
                        {Math.cos(5 * Math.PI / 6) * 8, Math.sin(5 * Math.PI / 6) * 8},
                        {Math.cos(3 * Math.PI / 2) * 8, Math.sin(3 * Math.PI / 2) * 8}
                    };
                    for (double[] pos : triPositions) {
                        Location guardLoc = center.clone().add(pos[0], 2, pos[1]);
                        // Inward-facing shard lines toward center
                        double dx = -pos[0];
                        double dz = -pos[1];
                        double len = Math.sqrt(dx * dx + dz * dz);
                        for (int seg = 1; seg <= 6; seg++) {
                            Location shardLoc = guardLoc.clone().add(
                                (dx / len) * seg * 1.2, 0, (dz / len) * seg * 1.2);
                            BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc,
                                Material.AMETHYST_CLUSTER);
                            shard.scale(0.3f, 0.5f, 0.3f).glow(0, 200, 255).interpolation(1, 0);
                            guardianHandles.add(shard);
                            spawnedEntities.add(shard.entity());
                        }
                        DisplayBuilder.cyanDust(guardLoc, 8, 2.0);
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GuardianTrioFormation(plugin); }
    }

    // ================================================================
    // 39. CELESTIAL HORROR SUMMON — Large elite entity with 8-dir volleys
    // ================================================================
    public static class CelestialHorrorSummon extends BossAttack {
        private final List<BlockDisplayHandle> horrorHandles = new ArrayList<>();
        private boolean horrorSpawned = false;
        private int horrorTick = 0;

        public CelestialHorrorSummon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_celestial_horror_summon", AttackType.BOSS, 2), "dog");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Large overhead rift forming
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 / 12) * i;
                Location riftLoc = center.clone().add(Math.cos(angle) * 4, 8, Math.sin(angle) * 4);
                BlockDisplayHandle rift = displayBuilder.spawnBlock(riftLoc, Material.CRYING_OBSIDIAN);
                rift.scale(0.8f, 0.3f, 0.8f).glow(128, 0, 255).interpolation(4, 0);
                horrorHandles.add(rift);
                spawnedEntities.add(rift.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 20, 5.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rift formation (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !horrorSpawned) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 10, 5.0);
                }
            }
            // Horror descends (tick 40)
            else if (ticksAlive == 40 && !horrorSpawned) {
                horrorSpawned = true;
                horrorTick = 0;
                // Celestial Horror construct — multi-block crystalline entity
                Location horrorLoc = center.clone().add(0, 5, 0);
                // Body core
                BlockDisplayHandle body = displayBuilder.spawnBlock(horrorLoc, Material.DARK_PRISMARINE);
                body.scale(2.5f, 2.0f, 2.5f).glow(128, 0, 255).interpolation(2, 0);
                horrorHandles.add(body);
                spawnedEntities.add(body.entity());
                // Crystal crown
                for (int spike = 0; spike < 6; spike++) {
                    double angle = (Math.PI * 2 / 6) * spike;
                    BlockDisplayHandle crown = displayBuilder.spawnBlock(
                        horrorLoc.clone().add(Math.cos(angle) * 1.5, 2, Math.sin(angle) * 1.5),
                        Material.AMETHYST_CLUSTER);
                    crown.scale(0.5f, 1.0f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                    horrorHandles.add(crown);
                    spawnedEntities.add(crown.entity());
                }
                // Wings — crystal plates extending from sides
                for (int wing = 0; wing < 2; wing++) {
                    float sideZ = wing == 0 ? -3 : 3;
                    for (int seg = 0; seg < 3; seg++) {
                        BlockDisplayHandle wingSeg = displayBuilder.spawnBlock(
                            horrorLoc.clone().add(0, 1 + seg * 0.5, sideZ + (wing == 0 ? -seg : seg)),
                            Material.AMETHYST_BLOCK);
                        wingSeg.scale(0.5f, 0.3f, 1.5f).glow(0, 200, 255).interpolation(2, 0);
                        horrorHandles.add(wingSeg);
                        spawnedEntities.add(wingSeg.entity());
                    }
                }
                // Eye core
                BlockDisplayHandle eye = displayBuilder.spawnBlock(
                    horrorLoc.clone().add(0, 1.2, 0), Material.SEA_LANTERN);
                eye.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                horrorHandles.add(eye);
                spawnedEntities.add(eye.entity());

                DisplayBuilder.cyanDust(horrorLoc, 30, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.4f);
            }
            // Horror fires 8-directional volleys every 80 ticks (4 seconds)
            else if (horrorSpawned) {
                horrorTick++;
                // Slow hover bob
                if (horrorTick % 4 == 0) {
                    float bob = (float) Math.sin(horrorTick * 0.05) * 0.5f;
                    for (int i = 12; i < horrorHandles.size(); i++) {
                        Location loc = horrorHandles.get(i).entity().getLocation();
                        horrorHandles.get(i).entity().teleport(
                            loc.clone().add(0, bob * 0.1, 0));
                    }
                }
                // Projectile volleys
                if (horrorTick % 80 == 0 && horrorTick > 0) {
                    Location horrorLoc = center.clone().add(0, 5, 0);
                    for (int dir = 0; dir < 8; dir++) {
                        double angle = (Math.PI * 2 / 8) * dir;
                        for (int seg = 1; seg <= 8; seg++) {
                            Location projLoc = horrorLoc.clone().add(
                                Math.cos(angle) * seg * 1.5, -seg * 0.2, Math.sin(angle) * seg * 1.5);
                            BlockDisplayHandle proj = displayBuilder.spawnBlock(projLoc,
                                Material.AMETHYST_CLUSTER);
                            proj.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255).interpolation(1, 0);
                            horrorHandles.add(proj);
                            spawnedEntities.add(proj.entity());
                        }
                    }
                    DisplayBuilder.cyanDust(horrorLoc, 20, 6.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CelestialHorrorSummon(plugin); }
    }

    // ================================================================
    // 40. TAIL FRAGMENT BOMB — Guided tail missile with explosion
    // ================================================================
    public static class TailFragmentBomb extends BossAttack {
        private final List<BlockDisplayHandle> fragHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> explosionHandles = new ArrayList<>();
        private boolean fragLaunched = false;
        private boolean fragDetonated = false;
        private double fragX = 14, fragY = 8, fragZ = 0;
        private double fragVelX = 0, fragVelZ = 0;

        public TailFragmentBomb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dog_tail_fragment_bomb", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Tail separation flash
            BlockDisplayHandle tailSeg = displayBuilder.spawnBlock(
                center.clone().add(14, 8, 0), Material.AMETHYST_BLOCK);
            tailSeg.scale(1.5f, 1.0f, 1.5f).glow(0, 200, 255).interpolation(2, 0);
            fragHandles.add(tailSeg);
            spawnedEntities.add(tailSeg.entity());
            // Trailing crystal pieces
            for (int t = 1; t <= 3; t++) {
                BlockDisplayHandle trail = displayBuilder.spawnBlock(
                    center.clone().add(14 + t, 8, 0), Material.AMETHYST_CLUSTER);
                trail.scale(0.6f, 0.5f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                fragHandles.add(trail);
                spawnedEntities.add(trail.entity());
            }
            DisplayBuilder.cyanDust(center.clone().add(14, 8, 0), 15, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Separation flash and hang (0-16 ticks)
            if (ticksAlive < 16 && !fragLaunched) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(fragX, fragY, fragZ), 6, 1.5);
                }
            }
            // Fragment launches (tick 16)
            else if (ticksAlive == 16 && !fragLaunched) {
                fragLaunched = true;
                // Initial velocity toward center
                double dx = -fragX;
                double dz = -fragZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                fragVelX = (dx / dist) * 0.8;
                fragVelZ = (dz / dist) * 0.8;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.0f);
            }
            // Fragment homes aggressively (16+ ticks)
            else if (fragLaunched && !fragDetonated) {
                // Aggressive homing at 20 blocks/sec
                double dx = center.getX() - (center.getX() + fragX);
                double dz = center.getZ() - (center.getZ() + fragZ);
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist > 1.5) {
                    double turnRate = 0.2;
                    fragVelX += (dx / dist) * turnRate;
                    fragVelZ += (dz / dist) * turnRate;
                    double velMag = Math.sqrt(fragVelX * fragVelX + fragVelZ * fragVelZ);
                    double speed = 1.0; // 20 blocks/sec
                    if (velMag > 0) {
                        fragVelX = (fragVelX / velMag) * speed;
                        fragVelZ = (fragVelZ / velMag) * speed;
                    }
                    fragX += fragVelX;
                    fragZ += fragVelZ;
                    fragY = Math.max(2, fragY - 0.1);

                    // Update fragment position
                    fragHandles.get(0).entity().teleport(center.clone().add(fragX, fragY, fragZ));
                    for (int t = 1; t < fragHandles.size(); t++) {
                        fragHandles.get(t).entity().teleport(
                            center.clone().add(fragX - fragVelX * t * 0.8, fragY + t * 0.2, fragZ - fragVelZ * t * 0.8));
                    }

                    // Contrail particles
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.cyanDust(center.clone().add(fragX, fragY, fragZ), 5, 1.0);
                    }
                } else {
                    // DETONATION — arrived at target
                    fragDetonated = true;
                    Location detonationLoc = center.clone().add(fragX, fragY, fragZ);

                    // 5-block radius explosion
                    for (int i = 0; i < 20; i++) {
                        double angle = (Math.PI * 2 / 20) * i;
                        for (int r = 1; r <= 5; r++) {
                            Location blastLoc = detonationLoc.clone().add(
                                Math.cos(angle) * r, 0.3, Math.sin(angle) * r);
                            BlockDisplayHandle blast = displayBuilder.spawnBlock(blastLoc,
                                Material.POLISHED_BLACKSTONE);
                            blast.scale(0.5f, 0.3f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
                            explosionHandles.add(blast);
                            spawnedEntities.add(blast.entity());
                        }
                    }
                    // Lingering crystal debris
                    for (int d = 0; d < 8; d++) {
                        double angle = Math.random() * Math.PI * 2;
                        double r = Math.random() * 4;
                        BlockDisplayHandle debris = displayBuilder.spawnBlock(
                            detonationLoc.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r),
                            Material.AMETHYST_CLUSTER);
                        debris.scale(0.3f, 0.4f, 0.3f).glow(0, 200, 255).interpolation(3, 0);
                        explosionHandles.add(debris);
                        spawnedEntities.add(debris.entity());
                    }

                    DisplayBuilder.cyanDust(detonationLoc, 50, 5.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.6f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TailFragmentBomb(plugin); }
    }
}
