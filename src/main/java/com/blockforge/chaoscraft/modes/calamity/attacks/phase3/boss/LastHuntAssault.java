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
 * Phase 3 Boss Attacks - TIER 5 "LAST HUNT" GROUP 2 (#91-100)
 * Dweller HP: 20%-0%. The final attacks. Frenzy strikes, void screams,
 * mark executions, and The Final Gaze — the last attack of the fight.
 * NO status effects - damage only.
 */
public final class LastHuntAssault {

    private LastHuntAssault() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ThousandCuts(plugin));
        registry.register(new VoidScream(plugin));
        registry.register(new MarkExecution(plugin));
        registry.register(new AbsoluteZeroGaze(plugin));
        registry.register(new ChaosStep(plugin));
        registry.register(new GroundConsumption(plugin));
        registry.register(new DeathFromAbove(plugin));
        registry.register(new FourHorsemen(plugin));
        registry.register(new ObliterationField(plugin));
        registry.register(new TheFinalGaze(plugin));
    }

    // ================================================================
    // 91. THOUSAND CUTS - 8-hit melee frenzy on nearest player
    // ================================================================
    public static class ThousandCuts extends BossAttack {
        private final List<BlockDisplayHandle> slashHandles = new ArrayList<>();
        private boolean frenzyActive = false;
        private int frenzyTick = 0;
        private int hitCount = 0;

        public ThousandCuts(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_thousand_cuts", AttackType.BOSS, 3), "dweller");
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Brimstone cracks flash simultaneously
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1, 0), Material.POLISHED_BLACKSTONE);
            body.scale(0.8f, 1.5f, 0.8f).glow(200, 0, 50).interpolation(1, 0);
            slashHandles.add(body);
            spawnedEntities.add(body.entity());
            DisplayBuilder.crimsonDust(center, 10, 1.5);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Brief warning flash (0-10 ticks = 0.5 seconds)
            if (ticksAlive < 10 && !frenzyActive) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 5, 1.0);
                }
            }
            // Frenzy begins (tick 10)
            else if (ticksAlive == 10 && !frenzyActive) {
                frenzyActive = true;
                frenzyTick = 0;
                hitCount = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1.2f);
            }
            // 8 strikes in 40 ticks (2 seconds)
            else if (frenzyActive && frenzyTick < 40) {
                frenzyTick++;
                // Strike every 5 ticks
                if (frenzyTick % 5 == 0 && hitCount < 8) {
                    hitCount++;
                    // Slash visual - alternating sides
                    double slashX = (hitCount % 2 == 0) ? -0.6 : 0.6;
                    double slashY = 0.8 + (hitCount % 3) * 0.4;
                    Location slashLoc = center.clone().add(slashX, slashY, 0);
                    BlockDisplayHandle slash = displayBuilder.spawnBlock(slashLoc, Material.NETHERRACK);
                    slash.scale(0.3f, 0.6f, 0.15f)
                         .rotate((float)(hitCount * Math.PI / 4), 0, 0, 1)
                         .glow(255, 100, 0).interpolation(1, 0);
                    slashHandles.add(slash);
                    spawnedEntities.add(slash.entity());

                    DisplayBuilder.crimsonDust(slashLoc, 4, 0.5);
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 1.5f);
                }
            }
            // Final shove (tick 50)
            else if (frenzyTick >= 40 && frenzyTick == 40) {
                frenzyTick++;
                DisplayBuilder.crimsonDust(center.clone().add(3, 1, 0), 15, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThousandCuts(plugin); }
    }

    // ================================================================
    // 92. VOID SCREAM - Expanding darkness sphere, total sensory deprivation
    // ================================================================
    public static class VoidScream extends BossAttack {
        private final List<BlockDisplayHandle> sphereHandles = new ArrayList<>();
        private boolean screaming = false;
        private int screamTick = 0;

        public VoidScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_void_scream", AttackType.BOSS, 3), "dweller");
            config.setDamage(0.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Head tilts back - initial void core
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                center.clone().add(0, 2.5, 0), Material.BLACK_CONCRETE);
            core.scale(0.5f, 0.5f, 0.5f).glow(0, 150, 255).interpolation(5, 0);
            sphereHandles.add(core);
            spawnedEntities.add(core.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 0.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Head tilt buildup (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !screaming) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2.5, 0),
                        5, 0.5, 0, 150, 255, 0.8f);
                }
            }
            // Scream begins (tick 30)
            else if (ticksAlive == 30 && !screaming) {
                screaming = true;
                screamTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.2f);
            }
            // Expanding void sphere (30-190 ticks = 8 seconds)
            else if (screaming && screamTick < 160) {
                screamTick++;
                float expansionRate = screamTick / 160.0f;
                double currentRadius = expansionRate * 12;

                // Expand core
                if (!sphereHandles.isEmpty()) {
                    float coreScale = 0.5f + (float)currentRadius * 0.4f;
                    sphereHandles.get(0).scale(
                        Math.min(coreScale, 6.0f),
                        Math.min(coreScale, 6.0f),
                        Math.min(coreScale, 6.0f));
                }

                // Add sphere shell segments as it expands
                if (screamTick % 10 == 0 && currentRadius > 2 && sphereHandles.size() < 80) {
                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2 / 8) * i + screamTick * 0.05;
                        double y = 1 + Math.sin(i * 0.8) * currentRadius * 0.5;
                        Location shellLoc = center.clone().add(
                            Math.cos(angle) * currentRadius, y, Math.sin(angle) * currentRadius);
                        BlockDisplayHandle sh = displayBuilder.spawnBlock(shellLoc, Material.BLACK_CONCRETE);
                        sh.scale(0.8f, 0.8f, 0.8f).glow(0, 150, 255).interpolation(3, 0);
                        sphereHandles.add(sh);
                        spawnedEntities.add(sh.entity());
                    }
                }

                // Animate existing shell outward
                for (int i = 1; i < sphereHandles.size(); i++) {
                    Location loc = sphereHandles.get(i).entity().getLocation();
                    double dx = loc.getX() - center.getX();
                    double dz = loc.getZ() - center.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0 && dist < currentRadius) {
                        double scale = (currentRadius / dist) * 0.05;
                        sphereHandles.get(i).entity().teleport(
                            loc.clone().add(dx * scale, 0, dz * scale));
                    }
                }

                // Event horizon particles
                if (screamTick % 8 == 0) {
                    DisplayBuilder.dustParticles(center, 8, currentRadius, 0, 150, 255, 1.0f);
                }
                // Heartbeat sound
                if (screamTick % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidScream(plugin); }
    }

    // ================================================================
    // 93. MARK EXECUTION - Homing blood orbs + final slam
    // ================================================================
    public static class MarkExecution extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> orbHandles = new ArrayList<>();
        private boolean executionStarted = false;
        private int execTick = 0;
        private int orbsFired = 0;

        public MarkExecution(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_mark_execution", AttackType.BOSS, 3), "dweller");
            config.setDamage(28.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller pointing arm
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.5, 0), Material.BLACKSTONE);
            body.scale(0.8f, 1.5f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
            bodyHandles.add(body);
            spawnedEntities.add(body.entity());

            // Extended pointing arm
            for (int i = 0; i < 5; i++) {
                Location armLoc = center.clone().add(1 + i * 0.6, 2, 0);
                BlockDisplayHandle arm = displayBuilder.spawnBlock(armLoc, Material.POLISHED_BLACKSTONE);
                arm.scale(0.3f, 0.25f, 0.25f).glow(200, 0, 50).interpolation(2, 0);
                bodyHandles.add(arm);
                spawnedEntities.add(arm.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.7f);
            DisplayBuilder.crimsonDust(center, 12, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Mark turns blood-red warning (0-30 ticks)
            if (ticksAlive < 30 && !executionStarted) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 8, 2.0);
                    DisplayBuilder.dustParticles(center.clone().add(4, 2, 0),
                        5, 1.0, 200, 0, 50, 1.5f);
                }
            }
            // Execution starts (tick 30)
            else if (ticksAlive == 30 && !executionStarted) {
                executionStarted = true;
                execTick = 0;
                orbsFired = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
            }
            // Walking + firing orbs (30-210 ticks)
            else if (executionStarted && execTick < 180) {
                execTick++;
                // Dweller walks forward slowly
                float walkProgress = execTick / 180.0f;
                double walkX = walkProgress * 12;
                for (BlockDisplayHandle bh : bodyHandles) {
                    Location loc = bh.entity().getLocation();
                    bh.entity().teleport(center.clone().add(
                        walkX, loc.getY() - center.getY(), 0));
                }

                // Fire blood orb every 30 ticks (6 orbs total)
                if (execTick % 30 == 0 && orbsFired < 6) {
                    orbsFired++;
                    Location orbSpawn = center.clone().add(walkX + 1.5, 2, 0);
                    BlockDisplayHandle orb = displayBuilder.spawnBlock(orbSpawn, Material.RED_STAINED_GLASS);
                    orb.scale(0.6f, 0.6f, 0.6f).glow(200, 0, 50).interpolation(1, 0);
                    orbHandles.add(orb);
                    spawnedEntities.add(orb.entity());

                    DisplayBuilder.crimsonDust(orbSpawn, 8, 0.8);
                    DisplayBuilder.playSound(orbSpawn, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.6f);
                }

                // Move fired orbs forward (homing visual)
                for (BlockDisplayHandle orb : orbHandles) {
                    Location loc = orb.entity().getLocation();
                    orb.entity().teleport(loc.clone().add(0.6, 0, 0));
                }

                // Walk particles
                if (execTick % 8 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(walkX, 0.5, 0), 5, 0.8);
                }
            }
            // Final slam (tick 210)
            else if (execTick >= 180 && execTick == 180) {
                execTick++;
                Location slamLoc = center.clone().add(12, 0, 0);
                DisplayBuilder.crimsonDust(slamLoc, 40, 4.0);
                DisplayBuilder.playSound(slamLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(slamLoc, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MarkExecution(plugin); }
    }

    // ================================================================
    // 94. ABSOLUTE ZERO GAZE - Cold-themed gaze that overrides brimstone
    // ================================================================
    public static class AbsoluteZeroGaze extends BossAttack {
        private final List<BlockDisplayHandle> iceHandles = new ArrayList<>();
        private boolean gazing = false;
        private int gazeTick = 0;

        public AbsoluteZeroGaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_absolute_zero_gaze", AttackType.BOSS, 3), "dweller");
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eyes shift to glacial blue-white
            BlockDisplayHandle eyeCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2.2, 0), Material.BLUE_ICE);
            eyeCore.scale(0.4f, 0.3f, 0.4f).glow(0, 150, 255).interpolation(3, 0);
            iceHandles.add(eyeCore);
            spawnedEntities.add(eyeCore.entity());

            // Frost particles blooming
            DisplayBuilder.dustParticles(center, 12, 3.0, 180, 220, 255, 1.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Gaze warning (0-30 ticks)
            if (ticksAlive < 30 && !gazing) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0),
                        6, 1.0, 180, 220, 255, 1.2f);
                }
            }
            // Gaze locks on (tick 30)
            else if (ticksAlive == 30 && !gazing) {
                gazing = true;
                gazeTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.5f);
            }
            // Freeze gaze active (30-110 ticks = 4 seconds)
            else if (gazing && gazeTick < 80) {
                gazeTick++;
                // Frost crystals forming around target area
                if (gazeTick % 10 == 0 && iceHandles.size() < 40) {
                    for (int i = 0; i < 3; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = 1 + Math.random() * 4;
                        Location frostLoc = center.clone().add(
                            Math.cos(angle) * dist, Math.random() * 2, Math.sin(angle) * dist);
                        BlockDisplayHandle frost = displayBuilder.spawnBlock(frostLoc, Material.BLUE_ICE);
                        frost.scale(0.3f, 0.3f, 0.3f).glow(0, 150, 255).interpolation(3, 0);
                        iceHandles.add(frost);
                        spawnedEntities.add(frost.entity());
                    }
                }
                // Cold particles
                if (gazeTick % 6 == 0) {
                    DisplayBuilder.dustParticles(center, 8, 5.0, 180, 220, 255, 1.5f);
                    DisplayBuilder.dustParticles(center, 5, 3.0, 0, 150, 255, 1.0f);
                }
                // Freezing sound
                if (gazeTick % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.3f);
                }
            }
            // Gaze ends - warmth returns
            else if (gazeTick >= 80 && gazeTick == 80) {
                gazeTick++;
                DisplayBuilder.crimsonDust(center, 15, 4.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbsoluteZeroGaze(plugin); }
    }

    // ================================================================
    // 95. CHAOS STEP - 12 random teleports creating corruption chaos
    // ================================================================
    public static class ChaosStep extends BossAttack {
        private final List<BlockDisplayHandle> portalHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> corruptionHandles = new ArrayList<>();
        private int teleportCount = 0;
        private int teleportTick = 0;

        public ChaosStep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_chaos_step", AttackType.BOSS, 3), "dweller");
            config.setDamage(0.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // First teleport flash
            BlockDisplayHandle portal = displayBuilder.spawnBlock(
                center.clone().add(0, 1, 0), Material.CRYING_OBSIDIAN);
            portal.scale(0.8f, 1.5f, 0.8f).glow(0, 150, 255).interpolation(1, 0);
            portalHandles.add(portal);
            spawnedEntities.add(portal.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 12 teleports over 200 ticks (~16 ticks apart)
            teleportTick++;
            if (teleportTick % 16 == 0 && teleportCount < 12) {
                teleportCount++;
                // Random position
                double rx = (Math.random() - 0.5) * 28;
                double rz = (Math.random() - 0.5) * 28;
                Location teleportLoc = center.clone().add(rx, 0, rz);

                // Move portal display
                if (!portalHandles.isEmpty()) {
                    portalHandles.get(0).entity().teleport(teleportLoc.clone().add(0, 1, 0));
                }

                // Leave corruption zone at departure
                BlockDisplayHandle corr = displayBuilder.spawnBlock(
                    teleportLoc.clone().add(0, 0.05, 0), Material.NETHERRACK);
                corr.scale(1.2f, 0.08f, 1.2f).glow(200, 0, 50).interpolation(2, 0);
                corruptionHandles.add(corr);
                spawnedEntities.add(corr.entity());

                // Portal burst particles
                DisplayBuilder.dustParticles(teleportLoc, 10, 1.5, 0, 150, 255, 1.2f);
                DisplayBuilder.crimsonDust(teleportLoc, 5, 1.0);
                DisplayBuilder.playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f,
                    0.5f + (float)Math.random() * 0.5f);
            }

            // After all 12 teleports, prepare for follow-up
            if (teleportCount >= 12 && teleportTick > 200) {
                // Final position: particles intensify
                if (teleportTick % 5 == 0) {
                    Location finalLoc = portalHandles.get(0).entity().getLocation();
                    DisplayBuilder.crimsonDust(finalLoc, 10, 2.0);
                    DisplayBuilder.dustParticles(finalLoc, 6, 1.0, 0, 150, 255, 1.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChaosStep(plugin); }
    }

    // ================================================================
    // 96. GROUND CONSUMPTION - Dweller eats corruption to heal
    // ================================================================
    public static class GroundConsumption extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> consumeHandles = new ArrayList<>();
        private boolean crawling = false;
        private int crawlTick = 0;
        private double crawlAngle = 0;

        public GroundConsumption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_ground_consumption", AttackType.BOSS, 3), "dweller");
            config.setDamage(0.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller drops to hands and knees
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(-i * 0.8, 0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.8f, 0.4f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning (0-30 ticks)
            if (ticksAlive < 30 && !crawling) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 0.3, 0), 5, 1.5);
                }
            }
            // Crawl begins (tick 30)
            else if (ticksAlive == 30 && !crawling) {
                crawling = true;
                crawlTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.4f);
            }
            // Crawling and consuming (30-110 ticks = 4 seconds, 8 tiles)
            else if (crawling && crawlTick < 80) {
                crawlTick++;
                crawlAngle += 0.08;
                double crawlRadius = 5;
                double crawlX = Math.cos(crawlAngle) * crawlRadius;
                double crawlZ = Math.sin(crawlAngle) * crawlRadius;

                // Move body along crawl path
                for (int i = 0; i < bodyHandles.size(); i++) {
                    double lagAngle = crawlAngle - i * 0.2;
                    bodyHandles.get(i).entity().teleport(
                        center.clone().add(
                            Math.cos(lagAngle) * crawlRadius, 0.4,
                            Math.sin(lagAngle) * crawlRadius));
                }

                // Consume tile every 10 ticks (8 total)
                if (crawlTick % 10 == 0 && consumeHandles.size() < 20) {
                    Location consumeLoc = center.clone().add(crawlX, 0.05, crawlZ);
                    // Consumed tile glows then fades
                    BlockDisplayHandle consumed = displayBuilder.spawnBlock(consumeLoc, Material.SOUL_SOIL);
                    consumed.scale(1.0f, 0.08f, 1.0f).glow(255, 100, 0).interpolation(5, 0);
                    consumeHandles.add(consumed);
                    spawnedEntities.add(consumed.entity());

                    // Lava flowing into mouth
                    DisplayBuilder.dustParticles(consumeLoc, 8, 0.5, 255, 100, 0, 1.5f);
                    DisplayBuilder.crimsonDust(consumeLoc, 5, 0.5);
                    DisplayBuilder.playSound(consumeLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.8f);
                }

                // Body brightens as it consumes more
                if (crawlTick % 20 == 0) {
                    float intensity = crawlTick / 80.0f;
                    DisplayBuilder.dustParticles(
                        center.clone().add(crawlX, 0.5, crawlZ),
                        6, 0.8, 255, (int)(100 + intensity * 100), 0, 1.5f);
                }
            }
            // Standing up - powered up (tick 110+)
            else if (crawlTick >= 80 && crawlTick == 80) {
                crawlTick++;
                // Full blaze on standing
                DisplayBuilder.crimsonDust(center, 20, 4.0);
                DisplayBuilder.dustParticles(center, 15, 3.0, 255, 100, 0, 2.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundConsumption(plugin); }
    }

    // ================================================================
    // 97. DEATH FROM ABOVE - Mini-vanish with false outline misdirection
    // ================================================================
    public static class DeathFromAbove extends BossAttack {
        private final List<BlockDisplayHandle> outlineHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> realHandles = new ArrayList<>();
        private boolean vanished = false;
        private boolean outlinesShown = false;
        private boolean dropped = false;
        private int realIndex = 0;

        public DeathFromAbove(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_death_from_above", AttackType.BOSS, 3), "dweller");
            config.setDamage(36.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(44.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller vanishes - nothing visible initially
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Silent vanish phase (0-60 ticks = 3 seconds)
            if (ticksAlive < 60 && !outlinesShown) {
                // Dead silence - no particles, no sounds
            }
            // False outlines appear (tick 60)
            else if (ticksAlive == 60 && !outlinesShown) {
                outlinesShown = true;
                realIndex = (int)(Math.random() * 3);
                // Three outlines at different heights/walls
                double[][] outlinePositions = {{-10, 20, 0}, {8, 15, -8}, {0, 25, 10}};
                for (int o = 0; o < 3; o++) {
                    double[] pos = outlinePositions[o];
                    for (int i = 0; i < 4; i++) {
                        Location loc = center.clone().add(pos[0], pos[1] + i * 0.6, pos[2]);
                        Material mat = (o == realIndex) ? Material.BLACKSTONE : Material.BASALT;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                        // Real one has full brimstone glow, fakes glow uniformly
                        if (o == realIndex) {
                            h.scale(0.7f, 0.7f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                            realHandles.add(h);
                        } else {
                            h.scale(0.6f, 0.6f, 0.6f).glow(100, 0, 25).interpolation(2, 0);
                        }
                        outlineHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
            }
            // Outlines visible, real one drops (tick 90, 1.5s after outlines)
            else if (outlinesShown && !dropped && ticksAlive < 90) {
                // Subtle pulse on outlines
                if (ticksAlive % 10 == 0) {
                    double[][] outlinePositions = {{-10, 20, 0}, {8, 15, -8}, {0, 25, 10}};
                    for (double[] pos : outlinePositions) {
                        DisplayBuilder.crimsonDust(
                            center.clone().add(pos[0], pos[1], pos[2]), 3, 0.5);
                    }
                }
            }
            // Real one drops (tick 90)
            else if (ticksAlive == 90 && !dropped) {
                dropped = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.3f);
            }
            // Drop descent (90-105 ticks)
            else if (dropped && ticksAlive < 105) {
                int t = ticksAlive - 90;
                float progress = t / 15.0f;
                double[][] outlinePositions = {{-10, 20, 0}, {8, 15, -8}, {0, 25, 10}};
                double[] realPos = outlinePositions[realIndex];
                for (int i = 0; i < realHandles.size(); i++) {
                    float segProgress = Math.max(0, Math.min(1, progress - i * 0.05f));
                    double y = realPos[1] * (1 - segProgress);
                    realHandles.get(i).entity().teleport(
                        center.clone().add(realPos[0], y + i * 0.6 * (1 - segProgress), realPos[2]));
                }
                if (t % 2 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(realPos[0], realPos[1] * (1 - progress), realPos[2]),
                        8, 1.0);
                }
            }
            // Impact (tick 105)
            else if (ticksAlive == 105 && dropped) {
                double[][] outlinePositions = {{-10, 20, 0}, {8, 15, -8}, {0, 25, 10}};
                double[] realPos = outlinePositions[realIndex];
                Location impactLoc = center.clone().add(realPos[0], 0, realPos[2]);
                // Corruption zone at landing
                for (int i = 0; i < 10; i++) {
                    double angle = (Math.PI * 2 / 10) * i;
                    BlockDisplayHandle tile = displayBuilder.spawnBlock(
                        impactLoc.clone().add(Math.cos(angle) * 3, 0.05, Math.sin(angle) * 3),
                        Material.MAGMA_BLOCK);
                    tile.scale(1.0f, 0.1f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(tile.entity());
                }
                DisplayBuilder.crimsonDust(impactLoc, 40, 5.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.2f);
                triggerImpactDamage(impactLoc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeathFromAbove(plugin); }
    }

    // ================================================================
    // 98. FOUR HORSEMEN - Four full-opacity clones, all lethal, 8 seconds
    // ================================================================
    public static class FourHorsemen extends BossAttack {
        private final List<List<BlockDisplayHandle>> entityGroups = new ArrayList<>();
        private boolean deployed = false;
        private int deployTick = 0;

        public FourHorsemen(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_four_horsemen", AttackType.BOSS, 3), "dweller");
            config.setDamage(24.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Four entities at cardinal positions
            double[][] spawnPositions = {{0, 0, 0}, {-10, 0, 0}, {10, 0, 0}, {0, 0, -10}};
            for (int e = 0; e < 4; e++) {
                List<BlockDisplayHandle> group = new ArrayList<>();
                double[] pos = spawnPositions[e];
                for (int i = 0; i < 4; i++) {
                    Location loc = center.clone().add(pos[0], i * 0.7, pos[2]);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.8f, 0.7f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                    group.add(h);
                    spawnedEntities.add(h.entity());
                }
                entityGroups.add(group);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.4f);
            DisplayBuilder.crimsonDust(center, 20, 8.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Brief formation (0-10 ticks = 0.5 second warning)
            if (ticksAlive < 10 && !deployed) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 10, 8.0);
                }
            }
            // Deploy (tick 10)
            else if (ticksAlive == 10 && !deployed) {
                deployed = true;
                deployTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.5f);
            }
            // All four hunt (10-170 ticks = 8 seconds)
            else if (deployed && deployTick < 160) {
                deployTick++;
                // Each entity circles/hunts at different angles
                for (int e = 0; e < entityGroups.size(); e++) {
                    List<BlockDisplayHandle> group = entityGroups.get(e);
                    double angle = deployTick * 0.06 + (Math.PI * 2 / 4) * e;
                    double radius = 8 + Math.sin(deployTick * 0.04 + e * 1.5) * 4;

                    for (int i = 0; i < group.size(); i++) {
                        double lagAngle = angle - i * 0.1;
                        group.get(i).entity().teleport(center.clone().add(
                            Math.cos(lagAngle) * radius, i * 0.7,
                            Math.sin(lagAngle) * radius));
                    }
                }
                // Corruption trails from all four
                if (deployTick % 8 == 0) {
                    for (int e = 0; e < entityGroups.size(); e++) {
                        Location entityLoc = entityGroups.get(e).get(0).entity().getLocation();
                        DisplayBuilder.crimsonDust(entityLoc, 4, 0.8);
                    }
                }
                // Sound from all directions
                if (deployTick % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.6f);
                }
            }
            // Clones dissolve - 1 second stun window (tick 170)
            else if (deployTick >= 160 && deployTick == 160) {
                deployTick++;
                // All dissolve simultaneously
                for (List<BlockDisplayHandle> group : entityGroups) {
                    Location dissolveLoc = group.get(0).entity().getLocation();
                    DisplayBuilder.crimsonDust(dissolveLoc, 10, 1.5);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FourHorsemen(plugin); }
    }

    // ================================================================
    // 99. OBLITERATION FIELD - Eight escalating shockwave rings
    // ================================================================
    public static class ObliterationField extends BossAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private boolean ringsActive = false;
        private int ringTick = 0;
        private int ringsFired = 0;

        public ObliterationField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_obliteration_field", AttackType.BOSS, 3), "dweller");
            config.setDamage(26.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller plants feet at center
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.5, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.0f, 2.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
            ringHandles.add(body);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Feet plant (0-20 ticks)
            if (ticksAlive < 20 && !ringsActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center, 8, 2.0);
                }
            }
            // Rings begin (tick 20)
            else if (ticksAlive == 20 && !ringsActive) {
                ringsActive = true;
                ringTick = 0;
                ringsFired = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.5f);
            }
            // Eight rings, one per second (20 ticks each)
            else if (ringsActive && ringsFired < 8) {
                ringTick++;
                // Fire ring every 20 ticks
                if (ringTick % 20 == 0) {
                    ringsFired++;
                    // Ring sizes: 3, 5, 7, 9, 11, 13, 15, 17
                    double ringRadius = 1 + ringsFired * 2;
                    int segments = (int)(8 + ringRadius);

                    for (int i = 0; i < segments; i++) {
                        double angle = (Math.PI * 2 / segments) * i;
                        Location ringLoc = center.clone().add(
                            Math.cos(angle) * ringRadius, 0.2, Math.sin(angle) * ringRadius);
                        Material ringMat = (ringsFired <= 4) ? Material.MAGMA_BLOCK : Material.NETHERRACK;
                        BlockDisplayHandle rh = displayBuilder.spawnBlock(ringLoc, ringMat);
                        rh.scale(1.0f, 0.3f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                        ringHandles.add(rh);
                        spawnedEntities.add(rh.entity());
                    }

                    // Escalating damage visuals
                    DisplayBuilder.crimsonDust(center, 15 + ringsFired * 3, ringRadius);
                    if (ringsFired >= 5) {
                        DisplayBuilder.dustParticles(center, 10, ringRadius, 255, 100, 0, 2.0f);
                    }

                    // Escalating sounds
                    float volume = 1.0f + ringsFired * 0.15f;
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, volume, 0.4f);
                    if (ringsFired >= 5) {
                        DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                            volume, 0.3f);
                    }
                }
            }
            // Post-rings: 2-second vulnerability window (after all 8 rings)
            else if (ringsFired >= 8) {
                if (ringTick == 160) { // First tick of vulnerability
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.5f);
                }
                ringTick++;
                if (ringTick % 10 == 0 && ringTick < 200) {
                    DisplayBuilder.crimsonDust(center, 5, 1.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ObliterationField(plugin); }
    }

    // ================================================================
    // 100. THE FINAL GAZE - The last attack. Below 5% HP. Decisive.
    // ================================================================
    public static class TheFinalGaze extends BossAttack {
        private final List<BlockDisplayHandle> bodyHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean silenceStarted = false;
        private boolean beamFired = false;
        private boolean windDown = false;
        private int phaseTick = 0;

        public TheFinalGaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_the_final_gaze", AttackType.BOSS, 3), "dweller");
            config.setDamage(50.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(50.0);
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller stands at full height - completely upright
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                float scale = 0.9f - i * 0.04f;
                h.scale(scale, 0.6f, scale).glow(200, 0, 50).interpolation(5, 0);
                bodyHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Eyes - initially dim cyan
            BlockDisplayHandle eyes = displayBuilder.spawnBlock(
                center.clone().add(0, 4.5, 0.3), Material.SOUL_SOIL);
            eyes.scale(0.3f, 0.15f, 0.15f).glow(0, 150, 255).interpolation(5, 0);
            bodyHandles.add(eyes);
            spawnedEntities.add(eyes.entity());
            // No sound. Silence.
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Phase 1: Silence (0-80 ticks = 4 seconds of condensation)
            if (ticksAlive < 80 && !beamFired) {
                if (!silenceStarted) {
                    silenceStarted = true;
                    phaseTick = 0;
                }
                phaseTick++;

                // Particles condense from arena edges toward the eyes (slow, deliberate)
                if (phaseTick % 8 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = 10 + Math.random() * 8;
                        Location particleLoc = center.clone().add(
                            Math.cos(angle) * dist, 1 + Math.random() * 3, Math.sin(angle) * dist);
                        // Subtle void particles drawn inward
                        DisplayBuilder.dustParticles(particleLoc, 2, 0.2, 0, 150, 255, 0.8f);
                    }
                }

                // At 2 seconds (tick 40): Mark aura goes dark
                if (phaseTick == 40) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 4.5, 0.3),
                        3, 0.2, 0, 150, 255, 0.5f);
                }

                // At 3 seconds (tick 60): others blinded (particle representation)
                if (phaseTick == 60) {
                    DisplayBuilder.dustParticles(center, 15, 16.0, 0, 0, 0, 2.0f);
                }

                // At 3.5 seconds: void pinpoint forms between eyes
                if (phaseTick >= 70) {
                    // Tiny concentrated core between eyes
                    if (phaseTick == 70) {
                        BlockDisplayHandle voidCore = displayBuilder.spawnBlock(
                            center.clone().add(0, 4.5, 0.4), Material.BLACK_CONCRETE);
                        voidCore.scale(0.1f, 0.1f, 0.1f).glow(0, 150, 255).interpolation(3, 0);
                        beamHandles.add(voidCore);
                        spawnedEntities.add(voidCore.entity());
                    }
                    // Core grows to critical mass
                    float coreGrowth = (phaseTick - 70) / 10.0f;
                    if (!beamHandles.isEmpty()) {
                        float s = 0.1f + coreGrowth * 0.15f;
                        beamHandles.get(0).scale(s, s, s);
                    }
                }

                // Heartbeat sound - accelerating
                if (phaseTick < 40 && phaseTick % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.3f, 0.2f);
                }
                else if (phaseTick >= 40 && phaseTick < 60 && phaseTick % 15 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.3f);
                }
                else if (phaseTick >= 60 && phaseTick % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.4f);
                }
            }
            // Phase 2: BEAM FIRES (tick 80 - instantaneous)
            else if (ticksAlive == 80 && !beamFired) {
                beamFired = true;
                phaseTick = 0;

                // Beam line - instantaneous, straight forward, surgical
                for (int seg = 0; seg < 15; seg++) {
                    Location beamLoc = center.clone().add(seg * 1.0, 4.5, 0.4);
                    BlockDisplayHandle bh = displayBuilder.spawnBlock(beamLoc, Material.BLACK_CONCRETE);
                    bh.scale(1.0f, 0.05f, 0.05f).glow(0, 150, 255).interpolation(0, 0);
                    beamHandles.add(bh);
                    spawnedEntities.add(bh.entity());
                }

                // Impact at target distance
                Location beamEnd = center.clone().add(15, 4.5, 0.4);
                triggerImpactDamage(beamEnd);

                // Single sharp sound
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 2.0f);
            }
            // Phase 3: Beam collapses (80-84 ticks, 0.2 seconds)
            else if (beamFired && !windDown && ticksAlive < 84) {
                // Beam exists for only a few ticks then vanishes
            }
            // Phase 4: Beam gone, beam handles removed (tick 84)
            else if (ticksAlive == 84 && !windDown) {
                windDown = true;
                phaseTick = 0;
                // Remove beam segments
                for (BlockDisplayHandle bh : beamHandles) {
                    bh.entity().remove();
                }
                beamHandles.clear();
            }
            // Phase 5: Wind-down (84-244 ticks = 8 seconds)
            else if (windDown && phaseTick < 160) {
                phaseTick++;

                // Dweller looks down at itself
                if (phaseTick == 1) {
                    // Eyes dim
                    if (bodyHandles.size() > 8) {
                        bodyHandles.get(8).glow(0, 50, 80);
                    }
                }

                // Slow steps, decelerating
                if (phaseTick == 20) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.3f, 0.3f);
                }
                if (phaseTick == 60) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.2f, 0.2f);
                }

                // Brimstone cracks dimming over time
                if (phaseTick % 20 == 0) {
                    float dimFactor = 1.0f - phaseTick / 160.0f;
                    int glowR = (int)(200 * dimFactor);
                    int glowB = (int)(50 * dimFactor);
                    for (int i = 0; i < Math.min(8, bodyHandles.size()); i++) {
                        bodyHandles.get(i).glow(Math.max(20, glowR), 0, Math.max(5, glowB));
                    }
                }

                // Void bleeding from the Dweller (quiet dissolution)
                if (phaseTick % 15 == 0 && phaseTick > 60) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0),
                        3, 1.0, 0, 150, 255, 0.6f);
                }

                // Eyes fully extinguish at the end
                if (phaseTick == 140 && bodyHandles.size() > 8) {
                    bodyHandles.get(8).glow(0, 0, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheFinalGaze(plugin); }
    }
}
