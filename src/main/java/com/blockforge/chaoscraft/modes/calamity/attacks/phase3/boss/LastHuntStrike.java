package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.boss;

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
 * Phase 3 Boss Attacks - TIER 5 "LAST HUNT" GROUP 1 (#81-90)
 * Dweller HP: 20%-0%. The end. Maximum speed, minimum cooldowns,
 * triple-chained attacks. Every attack can kill.
 * NO status effects - damage only.
 */
public final class LastHuntStrike {

    private LastHuntStrike() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new EnragedSprint(plugin));
        registry.register(new AnnihilationStamp(plugin));
        registry.register(new BloodTendrilStorm(plugin));
        registry.register(new MirrorStalk(plugin));
        registry.register(new VoidCollapse(plugin));
        registry.register(new ScorchChains(plugin));
        registry.register(new ObliterationDive(plugin));
        registry.register(new ScreamingVoid(plugin));
        registry.register(new TheLastCorruption(plugin));
        registry.register(new WrathConvergence(plugin));
    }

    // ================================================================
    // 81. ENRAGED SPRINT - Unstructured 10-second sprint with corruption trail
    // ================================================================
    public static class EnragedSprint extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> trailHandles = new ArrayList<>();
        private double currentX = 0;
        private double currentZ = 0;
        private double dirX = 1;
        private double dirZ = 0.5;
        private int wallBounces = 0;

        public EnragedSprint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_enraged_sprint", AttackType.BOSS, 3), "dweller");
            config.setDamage(24.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(220);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller body at sprint posture
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(-i * 0.5, 0.8 + i * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.8f, 0.6f, 0.8f).glow(200, 0, 50).interpolation(1, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // No telegraph - it just starts
            DisplayBuilder.crimsonDust(center, 10, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sprint movement (entire duration, 200 ticks = 10 seconds)
            if (ticksAlive < 200) {
                // Move at high speed
                currentX += dirX * 0.5;
                currentZ += dirZ * 0.5;

                // Wall bounce at arena boundaries
                if (Math.abs(currentX) > 16) {
                    dirX = -dirX;
                    dirZ += (Math.random() - 0.5) * 0.6;
                    wallBounces++;
                    DisplayBuilder.playSound(
                        center.clone().add(currentX, 1, currentZ),
                        Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.6f);
                }
                if (Math.abs(currentZ) > 16) {
                    dirZ = -dirZ;
                    dirX += (Math.random() - 0.5) * 0.6;
                    wallBounces++;
                }

                // Normalize direction
                double mag = Math.sqrt(dirX * dirX + dirZ * dirZ);
                if (mag > 0) { dirX /= mag; dirZ /= mag; }

                // Move body segments with trailing lag
                for (int i = 0; i < bodyHandles.size(); i++) {
                    double lagX = currentX - dirX * i * 0.5;
                    double lagZ = currentZ - dirZ * i * 0.5;
                    bodyHandles.get(i).entity().teleport(
                        center.clone().add(lagX, 0.8 + i * 0.3, lagZ));
                }

                // Corruption trail every 5 ticks
                if (ticksAlive % 5 == 0 && trailHandles.size() < 120) {
                    Location trailLoc = center.clone().add(currentX, 0.05, currentZ);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.NETHERRACK);
                    trail.scale(0.8f, 0.08f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                    trailHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                }

                // Comet tail particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(currentX, 1, currentZ), 5, 1.0);
                    DisplayBuilder.dustParticles(
                        center.clone().add(currentX - dirX, 1, currentZ - dirZ),
                        3, 0.5, 255, 100, 0, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EnragedSprint(plugin); }
    }

    // ================================================================
    // 82. ANNIHILATION STAMP - Levitating double-foot stomp with shockwave
    // ================================================================
    public static class AnnihilationStamp extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> waveHandles = new ArrayList<>();
        private boolean stamped = false;

        public AnnihilationStamp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_annihilation_stamp", AttackType.BOSS, 3), "dweller");
            config.setDamage(36.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(36.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller body pulling knees up (levitation frame)
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 4 + i * 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.9f, 0.7f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Feet pulled up
            BlockDisplayHandle leftFoot = displayBuilder.spawnBlock(
                center.clone().add(-0.4, 3.5, 0), Material.BLACKSTONE);
            leftFoot.scale(0.5f, 0.4f, 0.5f).glow(255, 100, 0).interpolation(2, 0);
            bodyHandles.add(leftFoot);
            spawnedEntities.add(leftFoot.entity());

            BlockDisplayHandle rightFoot = displayBuilder.spawnBlock(
                center.clone().add(0.4, 3.5, 0), Material.BLACKSTONE);
            rightFoot.scale(0.5f, 0.4f, 0.5f).glow(255, 100, 0).interpolation(2, 0);
            bodyHandles.add(rightFoot);
            spawnedEntities.add(rightFoot.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Levitation frame (0-16 ticks = 0.8 seconds)
            if (ticksAlive < 16 && !stamped) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 4, 0),
                        8, 1.5, 255, 100, 0, 1.5f);
                }
            }
            // STAMP DOWN (tick 16)
            else if (ticksAlive == 16 && !stamped) {
                stamped = true;
                // Slam body to ground
                for (int i = 0; i < bodyHandles.size(); i++) {
                    bodyHandles.get(i).entity().teleport(center.clone().add(0, 0.3, 0));
                }
                // Expanding shockwave ring
                for (int wave = 0; wave < 3; wave++) {
                    double radius = 3 + wave * 3;
                    int count = 12 + wave * 6;
                    for (int i = 0; i < count; i++) {
                        double angle = (Math.PI * 2 / count) * i;
                        Location waveLoc = center.clone().add(
                            Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius);
                        BlockDisplayHandle wh = displayBuilder.spawnBlock(waveLoc, Material.CRACKED_STONE_BRICKS);
                        wh.scale(1.0f, 0.15f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                        waveHandles.add(wh);
                        spawnedEntities.add(wh.entity());
                    }
                }
                DisplayBuilder.crimsonDust(center, 50, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.4f);
                triggerImpactDamage(center);
            }
            // Shockwave propagation visual (16-60 ticks)
            else if (stamped && ticksAlive < 60) {
                int t = ticksAlive - 16;
                if (t % 4 == 0) {
                    double waveRadius = t * 0.8;
                    DisplayBuilder.crimsonDust(center, 8, waveRadius);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AnnihilationStamp(plugin); }
    }

    // ================================================================
    // 83. BLOOD TENDRIL STORM - Six tendrils snap at all players
    // ================================================================
    public static class BloodTendrilStorm extends BossAttack {
        private final List<BlockDisplayHandle> tendrilHandles = new ArrayList<>();
        private boolean tendrils_fired = false;
        private int tendrilTick = 0;

        public BloodTendrilStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_blood_tendril_storm", AttackType.BOSS, 3), "dweller");
            config.setDamage(26.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Six tendrils emerge from Dweller's back
            for (int t = 0; t < 6; t++) {
                double baseAngle = (Math.PI * 2 / 6) * t;
                for (int seg = 0; seg < 6; seg++) {
                    double dist = 1 + seg * 1.5;
                    Location loc = center.clone().add(
                        Math.cos(baseAngle) * dist, 2 + seg * 0.3, Math.sin(baseAngle) * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                    h.scale(0.4f, 0.4f, 0.4f).glow(200, 0, 50).interpolation(2, 0);
                    tendrilHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 20, 4.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Tendrils writhe before targeting (0-20 ticks)
            if (ticksAlive < 20 && !tendrils_fired) {
                for (int t = 0; t < 6; t++) {
                    double baseAngle = (Math.PI * 2 / 6) * t + ticksAlive * 0.05;
                    for (int seg = 0; seg < 6; seg++) {
                        int idx = t * 6 + seg;
                        if (idx >= tendrilHandles.size()) break;
                        double dist = 1 + seg * 1.5;
                        double writhe = Math.sin(ticksAlive * 0.3 + seg * 0.5) * 0.5;
                        tendrilHandles.get(idx).entity().teleport(
                            center.clone().add(
                                Math.cos(baseAngle) * dist + writhe,
                                2 + seg * 0.3,
                                Math.sin(baseAngle) * dist + writhe));
                    }
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 8, 3.0);
                }
            }
            // Tendrils FIRE (tick 20)
            else if (ticksAlive == 20 && !tendrils_fired) {
                tendrils_fired = true;
                tendrilTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.8f);
            }
            // Tendrils snap outward at max speed (20-40 ticks)
            else if (tendrils_fired && tendrilTick < 20) {
                tendrilTick++;
                float progress = tendrilTick / 20.0f;
                for (int t = 0; t < 6; t++) {
                    double targetAngle = (Math.PI * 2 / 6) * t;
                    for (int seg = 0; seg < 6; seg++) {
                        int idx = t * 6 + seg;
                        if (idx >= tendrilHandles.size()) break;
                        float segProgress = Math.max(0, Math.min(1, progress - seg * 0.05f));
                        double dist = 1 + seg * 1.5 + segProgress * 8;
                        double y = 2 + seg * 0.3 - segProgress * (2 + seg * 0.3);
                        tendrilHandles.get(idx).entity().teleport(
                            center.clone().add(
                                Math.cos(targetAngle) * dist,
                                Math.max(0.2, y),
                                Math.sin(targetAngle) * dist));
                    }
                }
                if (tendrilTick % 3 == 0) {
                    for (int t = 0; t < 6; t++) {
                        double angle = (Math.PI * 2 / 6) * t;
                        DisplayBuilder.crimsonDust(
                            center.clone().add(
                                Math.cos(angle) * (5 + progress * 8), 0.5,
                                Math.sin(angle) * (5 + progress * 8)),
                            4, 0.5);
                    }
                }
            }
            // Tendrils at full extension (40+ ticks)
            else if (tendrilTick >= 20 && tendrilTick < 40) {
                tendrilTick++;
                // Corruption trails from impact back to center
                if (tendrilTick % 5 == 0) {
                    DisplayBuilder.crimsonDust(center, 6, 8.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BloodTendrilStorm(plugin); }
    }

    // ================================================================
    // 84. MIRROR STALK - Four synchronized clones, one breaks formation
    // ================================================================
    public static class MirrorStalk extends BossAttack {
        private final List<List<BlockDisplayHandle>> cloneGroups = new ArrayList<>();
        private boolean breakFormation = false;
        private int breakTick = 0;
        private int chargerIndex = 0;

        public MirrorStalk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_mirror_stalk", AttackType.BOSS, 3), "dweller");
            config.setDamage(30.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Five identical figures (1 real + 4 clones)
            chargerIndex = (int)(Math.random() * 5);
            double[][] positions = {{0, 0, 0}, {-6, 0, -6}, {6, 0, -6}, {-6, 0, 6}, {6, 0, 6}};
            for (int c = 0; c < 5; c++) {
                List<BlockDisplayHandle> group = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    Location loc = center.clone().add(
                        positions[c][0], i * 0.7, positions[c][2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    // Real one has slightly brighter brimstone
                    int glowR = (c == chargerIndex) ? 220 : 180;
                    h.scale(0.7f, 0.7f, 0.7f).glow(glowR, 0, 50).interpolation(2, 0);
                    group.add(h);
                    spawnedEntities.add(h.entity());
                }
                cloneGroups.add(group);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
            DisplayBuilder.crimsonDust(center, 15, 6.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            double[][] positions = {{0, 0, 0}, {-6, 0, -6}, {6, 0, -6}, {-6, 0, 6}, {6, 0, 6}};

            // Synchronized movement phase (0-60 ticks = 3 seconds)
            if (ticksAlive < 60 && !breakFormation) {
                // All five move in sync: slight forward step
                double syncStep = Math.sin(ticksAlive * 0.1) * 1.5;
                for (int c = 0; c < cloneGroups.size(); c++) {
                    List<BlockDisplayHandle> group = cloneGroups.get(c);
                    for (int i = 0; i < group.size(); i++) {
                        group.get(i).entity().teleport(center.clone().add(
                            positions[c][0] + syncStep, i * 0.7, positions[c][2]));
                    }
                }
                if (ticksAlive % 10 == 0) {
                    for (double[] pos : positions) {
                        DisplayBuilder.dustParticles(
                            center.clone().add(pos[0], 2, pos[2]),
                            3, 0.5, 0, 150, 255, 0.8f);
                    }
                }
            }
            // One breaks formation and charges (tick 60)
            else if (ticksAlive == 60 && !breakFormation) {
                breakFormation = true;
                breakTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.6f);
            }
            // Charge phase (60-100 ticks)
            else if (breakFormation && breakTick < 40) {
                breakTick++;
                float chargeProgress = breakTick / 40.0f;
                // Charger moves toward a target point
                List<BlockDisplayHandle> charger = cloneGroups.get(chargerIndex);
                double[] chargerPos = positions[chargerIndex];
                for (int i = 0; i < charger.size(); i++) {
                    double chargeX = chargerPos[0] + chargeProgress * (0 - chargerPos[0]) * 2;
                    double chargeZ = chargerPos[2] + chargeProgress * (0 - chargerPos[2]) * 2;
                    charger.get(i).entity().teleport(
                        center.clone().add(chargeX, i * 0.7, chargeZ));
                }
                if (breakTick % 4 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(chargerPos[0], 1, chargerPos[2]), 6, 1.5);
                }
            }
            // Post-charge: clones dissolve, real Dweller stunned (100-120)
            else if (breakTick >= 40 && breakTick < 60) {
                if (breakTick == 40) {
                    // Dissolve all clones
                    for (int c = 0; c < cloneGroups.size(); c++) {
                        if (c != chargerIndex) {
                            DisplayBuilder.crimsonDust(
                                cloneGroups.get(c).get(0).entity().getLocation(), 10, 1.5);
                        }
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.6f);
                }
                breakTick++;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MirrorStalk(plugin); }
    }

    // ================================================================
    // 85. VOID COLLAPSE - Suction rift pulls players inward
    // ================================================================
    public static class VoidCollapse extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private boolean riftOpen = false;
        private int riftTick = 0;

        public VoidCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_void_collapse", AttackType.BOSS, 3), "dweller");
            config.setDamage(28.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(220);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rift condensation point
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 0.5, Math.sin(angle) * 2.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.6f, 0.3f, 0.6f).glow(0, 150, 255).interpolation(3, 0);
                riftHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Central void core
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                center.clone().add(0, 0.3, 0), Material.BLACK_CONCRETE);
            core.scale(1.5f, 0.3f, 1.5f).glow(0, 150, 255).interpolation(5, 0);
            riftHandles.add(core);
            spawnedEntities.add(core.entity());

            DisplayBuilder.dustParticles(center, 15, 3.0, 0, 150, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rift forming (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !riftOpen) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center, 8, 3.0, 0, 150, 255, 1.0f);
                }
                // Rim particles spiral inward
                float progress = ticksAlive / 40.0f;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i + ticksAlive * 0.1;
                    double r = 2.5 * (1 - progress * 0.3);
                    if (i < riftHandles.size()) {
                        riftHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r));
                    }
                }
            }
            // Rift opens (tick 40)
            else if (ticksAlive == 40 && !riftOpen) {
                riftOpen = true;
                riftTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.3f);
                DisplayBuilder.dustParticles(center, 20, 5.0, 0, 150, 255, 2.0f);
            }
            // Rift active with suction (40-200 ticks = 8 seconds)
            else if (riftOpen && riftTick < 160) {
                riftTick++;
                // Rotate rim fragments
                for (int i = 0; i < 8 && i < riftHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i + riftTick * 0.08;
                    riftHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 2, 0.5, Math.sin(angle) * 2));
                }
                // Suction visual: particles flowing inward from 8 blocks
                if (riftTick % 6 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = 4 + Math.random() * 4;
                        DisplayBuilder.dustParticles(
                            center.clone().add(Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist),
                            3, 0.3, 0, 150, 255, 1.0f);
                    }
                }
                // Heartbeat pulse
                if (riftTick % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.4f);
                    DisplayBuilder.dustParticles(center, 10, 2.0, 0, 150, 255, 1.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidCollapse(plugin); }
    }

    // ================================================================
    // 86. SCORCH CHAINS - Simultaneous chain lockdown on all players
    // ================================================================
    public static class ScorchChains extends BossAttack {
        private final List<BlockDisplayHandle> chainHandles = new ArrayList<>();
        private boolean chainsLocked = false;

        public ScorchChains(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_scorch_chains", AttackType.BOSS, 3), "dweller");
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Chain eruption points in a grid pattern
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Location loc = center.clone().add(x * 4, 0.05, z * 4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                    h.scale(0.8f, 0.05f, 0.8f).glow(255, 100, 0).interpolation(2, 0);
                    chainHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 2.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Chain warning (0-30 ticks)
            if (ticksAlive < 30 && !chainsLocked) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center, 12, 8.0, 255, 100, 0, 1.0f);
                }
            }
            // Chains lock (tick 30)
            else if (ticksAlive == 30 && !chainsLocked) {
                chainsLocked = true;
                // Chains erupt upward at all grid points
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        Location chainLoc = center.clone().add(x * 4, 0, z * 4);
                        for (int y = 0; y < 3; y++) {
                            BlockDisplayHandle ch = displayBuilder.spawnBlock(
                                chainLoc.clone().add(0, y * 0.5, 0), Material.BASALT);
                            ch.scale(0.3f, 0.5f, 0.3f).glow(255, 100, 0).interpolation(2, 0);
                            chainHandles.add(ch);
                            spawnedEntities.add(ch.entity());
                        }
                    }
                }
                DisplayBuilder.dustParticles(center, 30, 10.0, 255, 100, 0, 1.5f);
                DisplayBuilder.crimsonDust(center, 15, 8.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.6f);
            }
            // Chains burn (30-70 ticks = 2 seconds of lock)
            else if (chainsLocked && ticksAlive < 70) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center, 8, 8.0, 255, 100, 0, 1.0f);
                    DisplayBuilder.crimsonDust(center, 5, 6.0);
                }
            }
            // Chains dissolve (tick 70)
            else if (ticksAlive == 70) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
                DisplayBuilder.crimsonDust(center, 15, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScorchChains(plugin); }
    }

    // ================================================================
    // 87. OBLITERATION DIVE - Fastest attack, massive non-threshold explosion
    // ================================================================
    public static class ObliterationDive extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> impactHandles = new ArrayList<>();
        private boolean diveStarted = false;
        private boolean impacted = false;

        public ObliterationDive(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_obliteration_dive", AttackType.BOSS, 3), "dweller");
            config.setDamage(28.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(12.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller at max height, rigid spear formation
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, 30 + i * 0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                float scale = 0.9f - i * 0.08f;
                h.scale(scale, 1.0f, scale).glow(200, 0, 50).interpolation(1, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Brief silhouette visible
            DisplayBuilder.crimsonDust(center.clone().add(0, 30, 0), 8, 1.5);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Silhouette visible (0-10 ticks = 0.5 seconds only)
            if (ticksAlive < 10 && !diveStarted) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 30, 0), 5, 1.0);
                }
            }
            // Dive starts (tick 10 -- 1 second total warning including spawn)
            else if (ticksAlive == 10 && !diveStarted) {
                diveStarted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.3f);
            }
            // Terminal velocity descent (10-25 ticks, extremely fast)
            else if (diveStarted && !impacted && ticksAlive < 25) {
                int t = ticksAlive - 10;
                float progress = t / 15.0f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float segProgress = Math.max(0, Math.min(1, progress - i * 0.02f));
                    double y = 30 * (1 - segProgress);
                    bodyHandles.get(i).entity().teleport(center.clone().add(0, y, 0));
                }
                if (t % 2 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0, 30 * (1 - progress), 0), 10, 1.5);
                    DisplayBuilder.dustParticles(
                        center.clone().add(0, 30 * (1 - progress), 0), 6, 1.0, 255, 100, 0, 2.0f);
                }
            }
            // IMPACT (tick 25)
            else if (ticksAlive == 25 && !impacted) {
                impacted = true;
                // 8x8 corruption super-zone
                for (int x = -4; x <= 4; x++) {
                    for (int z = -4; z <= 4; z++) {
                        if (x * x + z * z <= 16) {
                            Location tileLoc = center.clone().add(x, 0.05, z);
                            BlockDisplayHandle tile = displayBuilder.spawnBlock(tileLoc, Material.MAGMA_BLOCK);
                            tile.scale(1.0f, 0.1f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                            impactHandles.add(tile);
                            spawnedEntities.add(tile.entity());
                        }
                    }
                }
                // Scorched visual ring
                for (int i = 0; i < 20; i++) {
                    double angle = (Math.PI * 2 / 20) * i;
                    Location ringLoc = center.clone().add(
                        Math.cos(angle) * 10, 0.1, Math.sin(angle) * 10);
                    BlockDisplayHandle rh = displayBuilder.spawnBlock(ringLoc, Material.BLACK_CONCRETE);
                    rh.scale(1.2f, 0.1f, 1.2f).glow(200, 0, 50).interpolation(2, 0);
                    impactHandles.add(rh);
                    spawnedEntities.add(rh.entity());
                }
                DisplayBuilder.crimsonDust(center, 70, 14.0);
                DisplayBuilder.dustParticles(center, 50, 12.0, 255, 100, 0, 2.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.2f);
                triggerImpactDamage(center);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ObliterationDive(plugin); }
    }

    // ================================================================
    // 88. SCREAMING VOID - Chest-tear void cone, 60-degree 15-block range
    // ================================================================
    public static class ScreamingVoid extends BossAttack {
        private final List<BlockDisplayHandle> coreHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> coneHandles = new ArrayList<>();
        private boolean coneFired = false;

        public ScreamingVoid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_screaming_void", AttackType.BOSS, 3), "dweller");
            config.setDamage(36.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Chest cracking open - core visible
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                center.clone().add(0, 1.5, 0), Material.MAGMA_BLOCK);
            core.scale(0.8f, 0.8f, 0.8f).glow(200, 0, 50).interpolation(3, 0);
            coreHandles.add(core);
            spawnedEntities.add(core.entity());

            BlockDisplayHandle leftRib = displayBuilder.spawnBlock(
                center.clone().add(-0.5, 1.5, 0), Material.BLACKSTONE);
            leftRib.scale(0.4f, 0.8f, 0.6f).glow(200, 0, 50).interpolation(5, 0);
            coreHandles.add(leftRib);
            spawnedEntities.add(leftRib.entity());

            BlockDisplayHandle rightRib = displayBuilder.spawnBlock(
                center.clone().add(0.5, 1.5, 0), Material.BLACKSTONE);
            rightRib.scale(0.4f, 0.8f, 0.6f).glow(200, 0, 50).interpolation(5, 0);
            coreHandles.add(rightRib);
            spawnedEntities.add(rightRib.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Chest opens, scream builds (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !coneFired) {
                float progress = ticksAlive / 30.0f;
                // Ribs split apart
                if (coreHandles.size() >= 3) {
                    coreHandles.get(1).entity().teleport(
                        center.clone().add(-0.5 - progress * 0.4, 1.5, 0));
                    coreHandles.get(2).entity().teleport(
                        center.clone().add(0.5 + progress * 0.4, 1.5, 0));
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 1.5, 0), 8, 1.0);
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0),
                        5, 0.5, 0, 150, 255, 1.0f + progress);
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT,
                        1.0f + progress, 0.4f + progress * 0.3f);
                }
            }
            // CONE FIRES (tick 30)
            else if (ticksAlive == 30 && !coneFired) {
                coneFired = true;
                // 60-degree cone, 15 blocks forward
                for (int dist = 2; dist <= 15; dist += 2) {
                    double spread = dist * 0.55; // ~60 degree cone
                    int segsAtDist = Math.min(8, 2 + dist / 2);
                    for (int s = 0; s < segsAtDist; s++) {
                        double lateralOff = (s / (double)(segsAtDist - 1) - 0.5) * spread * 2;
                        Location coneLoc = center.clone().add(dist, 1.5, lateralOff);
                        BlockDisplayHandle ch = displayBuilder.spawnBlock(coneLoc, Material.CRYING_OBSIDIAN);
                        ch.scale(0.8f, 0.8f, 0.8f).glow(0, 150, 255).interpolation(2, 0);
                        coneHandles.add(ch);
                        spawnedEntities.add(ch.entity());
                    }
                }
                DisplayBuilder.crimsonDust(center.clone().add(8, 1.5, 0), 40, 8.0);
                DisplayBuilder.dustParticles(center.clone().add(8, 1.5, 0), 30, 6.0, 0, 150, 255, 2.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
            }
            // Cone dissipation (30-60 ticks)
            else if (coneFired && ticksAlive < 60) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(8, 1.5, 0),
                        6, 6.0, 0, 150, 255, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScreamingVoid(plugin); }
    }

    // ================================================================
    // 89. THE LAST CORRUPTION - Total arena floor conversion
    // ================================================================
    public static class TheLastCorruption extends BossAttack {
        private final List<BlockDisplayHandle> corruptionHandles = new ArrayList<>();
        private boolean converted = false;

        public TheLastCorruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_the_last_corruption", AttackType.BOSS, 3), "dweller");
            config.setDamage(2.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller stamps foot - initial crack
            BlockDisplayHandle stamp = displayBuilder.spawnBlock(
                center.clone().add(0, 0.05, 0), Material.CRACKED_STONE_BRICKS);
            stamp.scale(2.0f, 0.1f, 2.0f).glow(200, 0, 50).interpolation(3, 0);
            corruptionHandles.add(stamp);
            spawnedEntities.add(stamp.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning (0-20 ticks)
            if (ticksAlive < 20 && !converted) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center, 8, 2.0);
                }
            }
            // TOTAL CONVERSION (tick 20)
            else if (ticksAlive == 20 && !converted) {
                converted = true;
                // Corruption ripples outward in waves
                for (int ring = 0; ring < 6; ring++) {
                    double radius = 3 + ring * 3;
                    int segments = 8 + ring * 4;
                    for (int i = 0; i < segments; i++) {
                        double angle = (Math.PI * 2 / segments) * i;
                        Location tileLoc = center.clone().add(
                            Math.cos(angle) * radius, 0.05, Math.sin(angle) * radius);
                        BlockDisplayHandle tile = displayBuilder.spawnBlock(tileLoc, Material.NETHERRACK);
                        tile.scale(1.5f, 0.08f, 1.5f).glow(200, 0, 50).interpolation(3, ring);
                        corruptionHandles.add(tile);
                        spawnedEntities.add(tile.entity());
                    }
                }
                DisplayBuilder.crimsonDust(center, 60, 18.0);
                DisplayBuilder.dustParticles(center, 40, 16.0, 255, 100, 0, 2.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.3f);
            }
            // Persistent corruption damage (20+ ticks)
            else if (converted && ticksAlive % 20 == 0) {
                DisplayBuilder.crimsonDust(center, 8, 14.0);
                DisplayBuilder.dustParticles(center, 5, 12.0, 255, 100, 0, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLastCorruption(plugin); }
    }

    // ================================================================
    // 90. WRATH CONVERGENCE - Center teleport, absorb corruption, detonate
    // ================================================================
    public static class WrathConvergence extends BossAttack {
        private final List<BlockDisplayHandle> coreHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> streamHandles = new ArrayList<>();
        private boolean absorbing = false;
        private boolean detonated = false;

        public WrathConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_wrath_convergence", AttackType.BOSS, 3), "dweller");
            config.setDamage(24.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(36.0);
            config.setImpactRadius(20.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller at center, body blazing
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(1.0f, 0.6f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                coreHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Absorption begins immediately
            if (!absorbing && ticksAlive < 5) {
                absorbing = true;
            }

            // Absorption phase (0-60 ticks = 3 seconds)
            if (absorbing && !detonated && ticksAlive < 60) {
                // Energy streams flowing inward from all directions
                if (ticksAlive % 4 == 0 && streamHandles.size() < 80) {
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = 12 + Math.random() * 6;
                        Location streamLoc = center.clone().add(
                            Math.cos(angle) * dist, 0.3, Math.sin(angle) * dist);
                        BlockDisplayHandle sh = displayBuilder.spawnBlock(streamLoc, Material.ORANGE_STAINED_GLASS);
                        sh.scale(0.4f, 0.2f, 0.4f).glow(255, 100, 0).interpolation(2, 0);
                        streamHandles.add(sh);
                        spawnedEntities.add(sh.entity());
                    }
                }
                // Move streams toward center
                for (BlockDisplayHandle sh : streamHandles) {
                    Location loc = sh.entity().getLocation();
                    double dx = center.getX() - loc.getX();
                    double dz = center.getZ() - loc.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 1) {
                        sh.entity().teleport(loc.clone().add(dx * 0.08, 0, dz * 0.08));
                    }
                }
                // Core brightening
                if (ticksAlive % 8 == 0) {
                    float intensity = ticksAlive / 60.0f;
                    DisplayBuilder.dustParticles(center, (int)(10 + intensity * 20),
                        2.0 + intensity * 3, 255, 100, 0, 1.5f + intensity);
                    DisplayBuilder.crimsonDust(center, (int)(5 + intensity * 10), 1.0 + intensity);
                }
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT,
                        1.0f + ticksAlive / 60.0f, 0.5f + ticksAlive / 120.0f);
                }
            }
            // DETONATION (tick 60)
            else if (ticksAlive == 60 && !detonated) {
                detonated = true;
                // Full-arena explosion
                DisplayBuilder.crimsonDust(center, 100, 22.0);
                DisplayBuilder.dustParticles(center, 80, 20.0, 255, 100, 0, 3.0f);
                DisplayBuilder.dustParticles(center, 40, 18.0, 0, 150, 255, 2.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
                triggerImpactDamage(center);
            }
            // Post-detonation corruption re-forms (60-80)
            else if (detonated && ticksAlive < 80) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center, 10, 15.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WrathConvergence(plugin); }
    }
}
