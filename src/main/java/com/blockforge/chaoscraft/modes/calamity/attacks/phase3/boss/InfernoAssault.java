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
 * Phase 3 Boss Attacks - TIER 4 "THE INFERNO" GROUP 2 (#71-80)
 * Dweller HP: 40%-20%. Lattice mazes, death marches, eye storms,
 * and the Inferno Ascendant threshold transition into Tier 5.
 * NO status effects - damage only.
 */
public final class InfernoAssault {

    private InfernoAssault() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BrimstoneLattice(plugin));
        registry.register(new DeathMarch(plugin));
        registry.register(new EyeStorm(plugin));
        registry.register(new VanishPunish(plugin));
        registry.register(new ZoneMimic(plugin));
        registry.register(new CorruptionBurstVolley(plugin));
        registry.register(new InfernalGrip(plugin));
        registry.register(new BrimstoneSupernova(plugin));
        registry.register(new TrailPredator(plugin));
        registry.register(new InfernoAscendant(plugin));
    }

    // ================================================================
    // 71. BRIMSTONE LATTICE - Corruption walls form a fire maze
    // ================================================================
    public static class BrimstoneLattice extends BossAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private boolean latticeUp = false;

        public BrimstoneLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_brimstone_lattice", AttackType.BOSS, 3), "dweller");
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph: corruption tiles flare
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 / 12) * i;
                Location loc = center.clone().add(Math.cos(angle) * 6, 0.05, Math.sin(angle) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(1.5f, 0.1f, 1.5f).glow(255, 100, 0).interpolation(3, 0);
                wallHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.dustParticles(center, 20, 8.0, 255, 100, 0, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph flare (0-30 ticks)
            if (ticksAlive < 30 && !latticeUp) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center, 10, 8.0, 255, 100, 0, 1.2f);
                }
            }
            // Lattice walls erupt (tick 30)
            else if (ticksAlive == 30 && !latticeUp) {
                latticeUp = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                // Cross-pattern walls
                for (int axis = 0; axis < 4; axis++) {
                    double wallAngle = axis * Math.PI / 4;
                    for (int seg = 0; seg < 8; seg++) {
                        double dist = 2 + seg * 1.5;
                        for (int y = 0; y < 3; y++) {
                            Location wallLoc = center.clone().add(
                                Math.cos(wallAngle) * dist, y, Math.sin(wallAngle) * dist);
                            BlockDisplayHandle wh = displayBuilder.spawnBlock(wallLoc, Material.MAGMA_BLOCK);
                            wh.scale(0.8f, 1.0f, 0.8f).glow(200, 0, 50).interpolation(3, 0);
                            wallHandles.add(wh);
                            spawnedEntities.add(wh.entity());
                        }
                    }
                }
                DisplayBuilder.crimsonDust(center, 30, 10.0);
            }
            // Lattice active (30-230 ticks = 10 seconds)
            else if (latticeUp && ticksAlive < 230) {
                // Walls pulse with heat
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.dustParticles(center, 8, 10.0, 255, 100, 0, 1.0f);
                    DisplayBuilder.crimsonDust(center, 5, 8.0);
                }
            }
            // Lattice collapses (tick 230)
            else if (ticksAlive == 230) {
                DisplayBuilder.crimsonDust(center, 20, 10.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneLattice(plugin); }
    }

    // ================================================================
    // 72. DEATH MARCH - Unstoppable walk with lava footstep eruptions
    // ================================================================
    public static class DeathMarch extends BossAttack {
        private final List<BlockDisplayHandle> footprintHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private boolean marching = false;
        private int marchTick = 0;
        private double marchX = 0;

        public DeathMarch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_death_march", AttackType.BOSS, 3), "dweller");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller body segments
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, i * 0.7, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.9f, 0.7f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.crimsonDust(center, 12, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph rumble (0-20 ticks)
            if (ticksAlive < 20 && !marching) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center, 6, 1.5);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.4f);
                }
            }
            // March begins (tick 20)
            else if (ticksAlive == 20 && !marching) {
                marching = true;
                marchTick = 0;
                marchX = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
            }
            // Marching (20-260 ticks = 12 seconds)
            else if (marching && marchTick < 240) {
                marchTick++;
                marchX += 0.06; // slow, methodical pace

                // Move body forward
                for (int i = 0; i < bodyHandles.size(); i++) {
                    double sway = Math.sin(marchTick * 0.15) * 0.2;
                    bodyHandles.get(i).entity().teleport(
                        center.clone().add(marchX, i * 0.7, sway));
                }

                // Footstep eruption every 20 ticks (each step)
                if (marchTick % 20 == 0 && footprintHandles.size() < 60) {
                    double footZ = (marchTick / 20 % 2 == 0) ? 0.4 : -0.4;
                    Location stepLoc = center.clone().add(marchX, 0.05, footZ);

                    // Lava splash tile
                    BlockDisplayHandle footprint = displayBuilder.spawnBlock(stepLoc, Material.MAGMA_BLOCK);
                    footprint.scale(1.2f, 0.1f, 1.2f).glow(255, 100, 0).interpolation(2, 0);
                    footprintHandles.add(footprint);
                    spawnedEntities.add(footprint.entity());

                    // Eruption column
                    BlockDisplayHandle eruption = displayBuilder.spawnBlock(
                        stepLoc.clone().add(0, 0.5, 0), Material.NETHERRACK);
                    eruption.scale(0.5f, 1.5f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                    footprintHandles.add(eruption);
                    spawnedEntities.add(eruption.entity());

                    DisplayBuilder.crimsonDust(stepLoc, 8, 2.0);
                    DisplayBuilder.dustParticles(stepLoc, 5, 1.0, 255, 100, 0, 1.2f);
                    DisplayBuilder.playSound(stepLoc, Sound.BLOCK_STONE_PLACE, 1.0f, 0.3f);
                }
            }
            // Final contact hit zone
            else if (marchTick >= 240 && marchTick == 240) {
                marchTick++;
                Location endLoc = center.clone().add(marchX, 0, 0);
                DisplayBuilder.crimsonDust(endLoc, 30, 3.0);
                DisplayBuilder.playSound(endLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeathMarch(plugin); }
    }

    // ================================================================
    // 73. EYE STORM - Six rotating beams sweep 360 degrees
    // ================================================================
    public static class EyeStorm extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean stormActive = false;
        private int stormTick = 0;

        public EyeStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_eye_storm", AttackType.BOSS, 3), "dweller");
            config.setDamage(20.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Six beam segments emanating from center (eye position)
            for (int beam = 0; beam < 6; beam++) {
                double baseAngle = (Math.PI * 2 / 6) * beam;
                for (int seg = 0; seg < 8; seg++) {
                    double dist = 2 + seg * 1.5;
                    Location loc = center.clone().add(
                        Math.cos(baseAngle) * dist, 2, Math.sin(baseAngle) * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ORANGE_STAINED_GLASS);
                    h.scale(0.5f, 0.5f, 1.2f).glow(0, 150, 255).interpolation(2, 0);
                    beamHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 15, 2.0, 0, 150, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-20 ticks)
            if (ticksAlive < 20 && !stormActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0),
                        10, 3.0, 0, 150, 255, 1.2f);
                }
            }
            // Storm activates (tick 20)
            else if (ticksAlive == 20 && !stormActive) {
                stormActive = true;
                stormTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 0.6f);
            }
            // Rotating beams (20-180 ticks = 8 seconds)
            else if (stormActive && stormTick < 160) {
                stormTick++;
                // Rotation: full 360 every 40 ticks (2 seconds)
                float rotationAngle = stormTick * (float)(Math.PI * 2 / 40);

                for (int beam = 0; beam < 6; beam++) {
                    double baseAngle = rotationAngle + (Math.PI * 2 / 6) * beam;
                    for (int seg = 0; seg < 8; seg++) {
                        int idx = beam * 8 + seg;
                        if (idx >= beamHandles.size()) break;
                        double dist = 2 + seg * 1.5;
                        beamHandles.get(idx).entity().teleport(
                            center.clone().add(Math.cos(baseAngle) * dist, 2, Math.sin(baseAngle) * dist));
                    }
                }

                // Sweep particles
                if (stormTick % 5 == 0) {
                    double particleAngle = rotationAngle;
                    DisplayBuilder.dustParticles(
                        center.clone().add(Math.cos(particleAngle) * 8, 2, Math.sin(particleAngle) * 8),
                        6, 1.0, 0, 150, 255, 1.5f);
                }
                // Sound pulses
                if (stormTick % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EyeStorm(plugin); }
    }

    // ================================================================
    // 74. VANISH PUNISH - No-telegraph reappearance slam after Vanish Phase
    // ================================================================
    public static class VanishPunish extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private boolean appeared = false;
        private boolean impacted = false;

        public VanishPunish(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_vanish_punish", AttackType.BOSS, 3), "dweller");
            config.setDamage(30.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(30.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // No telegraph - silent spawn at height
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(0, 12 + i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.9f, 1.0f, 0.9f).glow(200, 0, 50).interpolation(1, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Barely audible reappearance
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.3f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Immediate drop (0-15 ticks, very fast)
            if (!impacted && ticksAlive < 15) {
                float progress = ticksAlive / 15.0f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float segProgress = Math.max(0, Math.min(1, progress - i * 0.03f));
                    double y = 12 * (1 - segProgress);
                    bodyHandles.get(i).entity().teleport(center.clone().add(0, y, 0));
                }
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0, 12 * (1 - progress), 0), 5, 1.0);
                }
            }
            // Impact (tick 15)
            else if (ticksAlive == 15 && !impacted) {
                impacted = true;
                // Corruption zone expansion
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    Location impactLoc = center.clone().add(
                        Math.cos(angle) * 4, 0.1, Math.sin(angle) * 4);
                    BlockDisplayHandle sh = displayBuilder.spawnBlock(impactLoc, Material.NETHERRACK);
                    sh.scale(1.0f, 0.15f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(sh.entity());
                }
                DisplayBuilder.crimsonDust(center, 35, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
                triggerImpactDamage(center);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VanishPunish(plugin); }
    }

    // ================================================================
    // 75. ZONE MIMIC - Three static decoys on corruption, real charges
    // ================================================================
    public static class ZoneMimic extends BossAttack {
        private final List<BlockDisplayHandle> decoyHandlesA = new ArrayList<>();
        private final List<BlockDisplayHandle> decoyHandlesB = new ArrayList<>();
        private final List<BlockDisplayHandle> decoyHandlesC = new ArrayList<>();
        private boolean chargeStarted = false;

        public ZoneMimic(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_zone_mimic", AttackType.BOSS, 3), "dweller");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Three decoy silhouettes at different corruption positions
            double[][] decoyPositions = {{-8, 0, -6}, {7, 0, 5}, {-5, 0, 8}};
            List<List<BlockDisplayHandle>> allDecoys = List.of(decoyHandlesA, decoyHandlesB, decoyHandlesC);

            for (int d = 0; d < 3; d++) {
                List<BlockDisplayHandle> decoyList = allDecoys.get(d);
                double[] pos = decoyPositions[d];
                for (int i = 0; i < 4; i++) {
                    Location loc = center.clone().add(pos[0], i * 0.7, pos[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.7f, 0.7f, 0.7f).glow(0, 150, 255).interpolation(2, 0);
                    decoyList.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);
            DisplayBuilder.crimsonDust(center, 15, 8.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Decoys standing still, glowing (0-60 ticks = 3 seconds)
            if (ticksAlive < 60 && !chargeStarted) {
                // Subtle eye glow pulse
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(-8, 2, -6),
                        3, 0.5, 0, 150, 255, 1.0f);
                    DisplayBuilder.dustParticles(center.clone().add(7, 2, 5),
                        3, 0.5, 0, 150, 255, 1.0f);
                    DisplayBuilder.dustParticles(center.clone().add(-5, 2, 8),
                        3, 0.5, 0, 150, 255, 1.0f);
                }
            }
            // Real Dweller charges (tick 60)
            else if (ticksAlive == 60 && !chargeStarted) {
                chargeStarted = true;
                // Decoys dissolve
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.6f);
                DisplayBuilder.crimsonDust(center, 20, 8.0);
            }
            // Charge phase particles (60-120)
            else if (chargeStarted && ticksAlive < 120) {
                if (ticksAlive % 5 == 0) {
                    float progress = (ticksAlive - 60) / 60.0f;
                    DisplayBuilder.crimsonDust(
                        center.clone().add(progress * 10, 1, 0), 8, 1.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ZoneMimic(plugin); }
    }

    // ================================================================
    // 76. CORRUPTION BURST VOLLEY - Rapid-fire corruption orbs
    // ================================================================
    public static class CorruptionBurstVolley extends BossAttack {
        private final List<BlockDisplayHandle> orbHandles = new ArrayList<>();
        private boolean volleyFired = false;
        private int volleyTick = 0;

        public CorruptionBurstVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_corruption_burst_volley", AttackType.BOSS, 3), "dweller");
            config.setDamage(20.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(6);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller rooted stance - dark energy gathering in hands
            BlockDisplayHandle leftHand = displayBuilder.spawnBlock(
                center.clone().add(-0.8, 2, 0), Material.CRYING_OBSIDIAN);
            leftHand.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
            orbHandles.add(leftHand);
            spawnedEntities.add(leftHand.entity());

            BlockDisplayHandle rightHand = displayBuilder.spawnBlock(
                center.clone().add(0.8, 2, 0), Material.CRYING_OBSIDIAN);
            rightHand.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
            orbHandles.add(rightHand);
            spawnedEntities.add(rightHand.entity());

            DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 12, 1.5);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge buildup (0-30 ticks)
            if (ticksAlive < 30 && !volleyFired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 8, 1.5);
                    float growScale = 0.5f + ticksAlive / 60.0f;
                    for (BlockDisplayHandle h : orbHandles) {
                        h.scale(growScale, growScale, growScale);
                    }
                }
            }
            // Volley fires (tick 30)
            else if (ticksAlive == 30 && !volleyFired) {
                volleyFired = true;
                volleyTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.8f);
            }
            // Rapid-fire orbs (30-110 ticks = 12 orbs in 4 seconds)
            else if (volleyFired && volleyTick < 80) {
                volleyTick++;
                // Fire orb every ~7 ticks (12 orbs over 80 ticks)
                if (volleyTick % 7 == 0 && orbHandles.size() < 50) {
                    boolean leftHand = (volleyTick / 7) % 2 == 0;
                    double handX = leftHand ? -0.8 : 0.8;
                    int orbNum = volleyTick / 7;
                    double orbDist = orbNum * 1.5;

                    Location orbLoc = center.clone().add(handX + orbDist * 0.5, 2, 0);
                    BlockDisplayHandle orb = displayBuilder.spawnBlock(orbLoc, Material.OBSIDIAN);
                    orb.scale(0.4f, 0.4f, 0.4f).glow(200, 0, 50).interpolation(1, 0);
                    orbHandles.add(orb);
                    spawnedEntities.add(orb.entity());

                    DisplayBuilder.crimsonDust(orbLoc, 6, 0.8);
                    DisplayBuilder.playSound(orbLoc, Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.2f);
                }
                // Move existing projectile orbs forward
                if (volleyTick % 2 == 0) {
                    for (int i = 2; i < orbHandles.size(); i++) {
                        Location loc = orbHandles.get(i).entity().getLocation();
                        orbHandles.get(i).entity().teleport(loc.clone().add(0.8, 0, 0));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionBurstVolley(plugin); }
    }

    // ================================================================
    // 77. INFERNAL GRIP - Dual arm grab slams two players together
    // ================================================================
    public static class InfernalGrip extends BossAttack {
        private final List<BlockDisplayHandle> leftArmHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> rightArmHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> impactHandles = new ArrayList<>();
        private boolean extended = false;
        private boolean slammed = false;

        public InfernalGrip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_infernal_grip", AttackType.BOSS, 3), "dweller");
            config.setDamage(24.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(24.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Left arm extending
            for (int i = 0; i < 7; i++) {
                Location loc = center.clone().add(-1 - i * 0.8, 2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.5f, 0.4f, 0.4f).glow(200, 0, 50).interpolation(2, 0);
                leftArmHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Right arm extending
            for (int i = 0; i < 7; i++) {
                Location loc = center.clone().add(1 + i * 0.8, 2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.5f, 0.4f, 0.4f).glow(200, 0, 50).interpolation(2, 0);
                rightArmHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Arms extend outward (0-30 ticks)
            if (ticksAlive < 30 && !extended) {
                float progress = ticksAlive / 30.0f;
                for (int i = 0; i < leftArmHandles.size(); i++) {
                    double dist = (1 + i * 0.8) * progress;
                    leftArmHandles.get(i).entity().teleport(
                        center.clone().add(-dist, 2, 0));
                    rightArmHandles.get(i).entity().teleport(
                        center.clone().add(dist, 2, 0));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(-4 * progress, 2, 0), 4, 0.5);
                    DisplayBuilder.crimsonDust(center.clone().add(4 * progress, 2, 0), 4, 0.5);
                }
            }
            // Arms fully extended (tick 30)
            else if (ticksAlive == 30 && !extended) {
                extended = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.8f);
            }
            // Hold phase - squeeze (30-70 ticks = 2 seconds)
            else if (extended && !slammed && ticksAlive < 70) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(-6, 2, 0), 5, 0.8);
                    DisplayBuilder.crimsonDust(center.clone().add(6, 2, 0), 5, 0.8);
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.5f);
                }
            }
            // SLAM together (tick 70)
            else if (ticksAlive == 70 && !slammed) {
                slammed = true;
                // Arms snap to center
                for (BlockDisplayHandle h : leftArmHandles) {
                    h.entity().teleport(center.clone().add(0, 2, 0));
                }
                for (BlockDisplayHandle h : rightArmHandles) {
                    h.entity().teleport(center.clone().add(0, 2, 0));
                }
                // Impact zone
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    Location impLoc = center.clone().add(
                        Math.cos(angle) * 2, 0.1, Math.sin(angle) * 2);
                    BlockDisplayHandle ih = displayBuilder.spawnBlock(impLoc, Material.MAGMA_BLOCK);
                    ih.scale(0.8f, 0.2f, 0.8f).glow(255, 100, 0).interpolation(2, 0);
                    impactHandles.add(ih);
                    spawnedEntities.add(ih.entity());
                }
                DisplayBuilder.crimsonDust(center, 35, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.4f);
                triggerImpactDamage(center);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InfernalGrip(plugin); }
    }

    // ================================================================
    // 78. BRIMSTONE SUPERNOVA - Chest-tear massive AoE detonation
    // ================================================================
    public static class BrimstoneSupernova extends BossAttack {
        private final List<BlockDisplayHandle> coreHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> blastHandles = new ArrayList<>();
        private boolean detonated = false;

        public BrimstoneSupernova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_brimstone_supernova", AttackType.BOSS, 3), "dweller");
            config.setDamage(22.0);
            config.setDamageRadius(14.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(32.0);
            config.setImpactRadius(20.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Chest core beginning to crack open
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                center.clone().add(0, 1.5, 0), Material.MAGMA_BLOCK);
            core.scale(1.2f, 1.2f, 1.2f).glow(255, 100, 0).interpolation(5, 0);
            coreHandles.add(core);
            spawnedEntities.add(core.entity());

            // Chest plates splitting apart
            BlockDisplayHandle leftPlate = displayBuilder.spawnBlock(
                center.clone().add(-0.5, 1.5, 0), Material.BLACKSTONE);
            leftPlate.scale(0.6f, 1.0f, 0.8f).glow(200, 0, 50).interpolation(5, 0);
            coreHandles.add(leftPlate);
            spawnedEntities.add(leftPlate.entity());

            BlockDisplayHandle rightPlate = displayBuilder.spawnBlock(
                center.clone().add(0.5, 1.5, 0), Material.BLACKSTONE);
            rightPlate.scale(0.6f, 1.0f, 0.8f).glow(200, 0, 50).interpolation(5, 0);
            coreHandles.add(rightPlate);
            spawnedEntities.add(rightPlate.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Chest splits open - charge (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !detonated) {
                float progress = ticksAlive / 40.0f;
                // Plates split apart
                if (coreHandles.size() >= 3) {
                    coreHandles.get(1).entity().teleport(
                        center.clone().add(-0.5 - progress * 0.5, 1.5, 0));
                    coreHandles.get(2).entity().teleport(
                        center.clone().add(0.5 + progress * 0.5, 1.5, 0));
                }
                // Core brightens
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0),
                        10, 1.0, 255, 100, 0, 1.5f + progress);
                    DisplayBuilder.crimsonDust(center.clone().add(0, 1.5, 0), 6, 0.8);
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f + progress, 1.0f + progress);
                }
            }
            // DETONATION (tick 40)
            else if (ticksAlive == 40 && !detonated) {
                detonated = true;
                // Massive blast rings
                for (int ring = 0; ring < 4; ring++) {
                    double radius = 4 + ring * 4;
                    int count = 12 + ring * 4;
                    for (int i = 0; i < count; i++) {
                        double angle = (Math.PI * 2 / count) * i;
                        Location blastLoc = center.clone().add(
                            Math.cos(angle) * radius, 0.2 + ring * 0.3, Math.sin(angle) * radius);
                        BlockDisplayHandle bh = displayBuilder.spawnBlock(blastLoc, Material.MAGMA_BLOCK);
                        bh.scale(1.0f, 0.3f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                        blastHandles.add(bh);
                        spawnedEntities.add(bh.entity());
                    }
                }
                DisplayBuilder.crimsonDust(center, 60, 20.0);
                DisplayBuilder.dustParticles(center, 40, 16.0, 255, 100, 0, 2.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
                triggerImpactDamage(center);
            }
            // Post-detonation stun window (40-70 ticks = 1.5 seconds)
            else if (detonated && ticksAlive < 70) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center, 8, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneSupernova(plugin); }
    }

    // ================================================================
    // 79. TRAIL PREDATOR - Hunts along corruption trails at high speed
    // ================================================================
    public static class TrailPredator extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private boolean hunting = false;
        private int huntTick = 0;
        private double huntAngle = 0;

        public TrailPredator(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_trail_predator", AttackType.BOSS, 3), "dweller");
            config.setDamage(28.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller drops low into stalking posture
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(-i * 0.6, 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.8f, 0.5f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 10, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Enter trail-hunt stance (0-20 ticks)
            if (ticksAlive < 20 && !hunting) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 5, 2.0);
                }
            }
            // Hunt begins (tick 20)
            else if (ticksAlive == 20 && !hunting) {
                hunting = true;
                huntTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
            }
            // Trail hunting - erratic fast movement along a path (20-260 ticks = 12 seconds)
            else if (hunting && huntTick < 240) {
                huntTick++;
                // Fast erratic pathing along corruption
                huntAngle += 0.12 + Math.sin(huntTick * 0.1) * 0.08;
                double huntRadius = 8 + Math.sin(huntTick * 0.05) * 4;
                double huntX = Math.cos(huntAngle) * huntRadius;
                double huntZ = Math.sin(huntAngle) * huntRadius;

                for (int i = 0; i < bodyHandles.size(); i++) {
                    double lagAngle = huntAngle - i * 0.15;
                    double lagR = huntRadius - i * 0.3;
                    bodyHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(lagAngle) * lagR, 0.5, Math.sin(lagAngle) * lagR));
                }

                // Lava wake particles
                if (huntTick % 6 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(huntX, 0.3, huntZ), 5, 1.0);
                    DisplayBuilder.dustParticles(
                        center.clone().add(huntX, 0.5, huntZ), 3, 0.5, 255, 100, 0, 1.0f);
                }
                if (huntTick % 30 == 0) {
                    DisplayBuilder.playSound(center.clone().add(huntX, 0, huntZ),
                        Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TrailPredator(plugin); }
    }

    // ================================================================
    // 80. INFERNO ASCENDANT - Tier 4 finale, 20% HP threshold crossing
    // ================================================================
    public static class InfernoAscendant extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> impactHandles = new ArrayList<>();
        private boolean atApex = false;
        private boolean dropped = false;
        private boolean impacted = false;

        public InfernoAscendant(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_inferno_ascendant", AttackType.BOSS, 3), "dweller");
            config.setDamage(10.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(32.0);
            config.setImpactRadius(15.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller begins to climb
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                float scale = 1.0f - i * 0.05f;
                h.scale(scale, 0.8f, scale).glow(200, 0, 50).interpolation(3, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.crimsonDust(center, 15, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Climb to apex (0-40 ticks)
            if (ticksAlive < 40 && !atApex) {
                float progress = ticksAlive / 40.0f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    double y = i * 0.8 + progress * 30;
                    bodyHandles.get(i).entity().teleport(center.clone().add(0, y, 0));
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0, progress * 30, 0), 8, 2.0);
                }
            }
            // Apex pause (tick 40)
            else if (ticksAlive == 40 && !atApex) {
                atApex = true;
                // Arms spread, full blaze
                DisplayBuilder.crimsonDust(center.clone().add(0, 30, 0), 30, 5.0);
                DisplayBuilder.dustParticles(center.clone().add(0, 30, 0), 20, 4.0, 255, 100, 0, 2.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.3f);
            }
            // Apex hold + scream (40-80 ticks = 2 seconds)
            else if (atApex && !dropped && ticksAlive < 80) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 30, 0), 15, 6.0);
                    DisplayBuilder.dustParticles(center.clone().add(0, 30, 0),
                        10, 5.0, 255, 100, 0, 2.0f);
                }
            }
            // DROP (tick 80)
            else if (ticksAlive == 80 && !dropped) {
                dropped = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.2f);
            }
            // Terminal velocity descent (80-100 ticks)
            else if (dropped && !impacted && ticksAlive < 100) {
                int t = ticksAlive - 80;
                float progress = t / 20.0f;
                for (int i = 0; i < bodyHandles.size(); i++) {
                    float segProgress = Math.max(0, Math.min(1, progress - i * 0.02f));
                    double y = 30 * (1 - segProgress);
                    bodyHandles.get(i).entity().teleport(center.clone().add(0, y, 0));
                }
                if (t % 2 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0, 30 * (1 - progress), 0), 10, 2.0);
                }
            }
            // IMPACT (tick 100)
            else if (ticksAlive == 100 && !impacted) {
                impacted = true;
                // 7x7 corruption super-zone
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        Location tileLoc = center.clone().add(x, 0.05, z);
                        BlockDisplayHandle tile = displayBuilder.spawnBlock(tileLoc, Material.NETHERRACK);
                        tile.scale(1.0f, 0.1f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                        impactHandles.add(tile);
                        spawnedEntities.add(tile.entity());
                    }
                }
                // Concentric corruption rings at 5, 10, 15 blocks
                for (int ringDist : new int[]{5, 10, 15}) {
                    int segments = 8 + ringDist;
                    for (int i = 0; i < segments; i++) {
                        double angle = (Math.PI * 2 / segments) * i;
                        Location ringLoc = center.clone().add(
                            Math.cos(angle) * ringDist, 0.05, Math.sin(angle) * ringDist);
                        BlockDisplayHandle rh = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                        rh.scale(1.2f, 0.1f, 1.2f).glow(255, 100, 0).interpolation(3, 0);
                        impactHandles.add(rh);
                        spawnedEntities.add(rh.entity());
                    }
                }
                // Massive visual explosion
                DisplayBuilder.crimsonDust(center, 80, 18.0);
                DisplayBuilder.dustParticles(center, 50, 15.0, 255, 100, 0, 2.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
                triggerImpactDamage(center);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new InfernoAscendant(plugin); }
    }
}
