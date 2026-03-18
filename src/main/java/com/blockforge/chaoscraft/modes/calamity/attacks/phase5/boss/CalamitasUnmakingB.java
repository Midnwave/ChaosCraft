package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Supreme Calamitas — Phase 3: "The Unmaking" (49%-25% HP)
 * Attacks #111-120
 *
 * Synchronized chaos + ground slam escalation attacks.
 * Calamitas colors: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255), purple(128,0,255)
 *
 * NO status effects — damage only.
 */
public final class CalamitasUnmakingB {

    private CalamitasUnmakingB() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TridentGravityWell(plugin));
        registry.register(new ChainLightningTridents(plugin));
        registry.register(new BrimstoneTide(plugin));
        registry.register(new FissureStrike(plugin));
        registry.register(new MeleeFlurry(plugin));
        registry.register(new CorruptionRing(plugin));
        registry.register(new SeismicStompGrid(plugin));
        registry.register(new DragWalk(plugin));
        registry.register(new PermanentBurnZone(plugin));
        registry.register(new Devastation(plugin));
    }

    // ================================================================
    // #111 — TRIDENT GRAVITY WELL
    // Pull zone at center + 12 tridents from perimeter converging inward
    // ================================================================
    public static class TridentGravityWell extends BossAttack {
        private final List<BlockDisplayHandle> wellTridents = new ArrayList<>();
        private BlockDisplayHandle wellCore;
        private boolean launched = false;
        private int launchTick = 0;

        public TridentGravityWell(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_gravity_well", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0); // 10 hearts pull contact
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(false);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph — inward-pulling vortex
            DisplayBuilder.purpleDust(center, 30, 4.0);
            DisplayBuilder.crimsonDust(center, 20, 15.0);
            wellCore = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.CRYING_OBSIDIAN);
            wellCore.scale(2.0f, 0.5f, 2.0f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(wellCore.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(center, 15, 4.0);
                }
                return;
            }

            // Gravity pull effect on players (10-70 ticks = 3 seconds)
            if (ticksAlive >= 10 && ticksAlive < 70) {
                World w = center.getWorld();
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (dist <= 15.0 && dist > 1.0) {
                        Vector pullDir = center.toVector().subtract(player.getLocation().toVector()).normalize();
                        player.setVelocity(player.getVelocity().add(pullDir.multiply(0.15)));
                    }
                }

                // Vortex particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(center, 30, 3.0);
                }
            }

            // Launch 12 perimeter tridents at tick 20
            if (ticksAlive == 20 && !launched) {
                launched = true;
                launchTick = ticksAlive;
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI / 12) * i;
                    Location perimeterLoc = center.clone().add(
                        Math.cos(angle) * 25, 3, Math.sin(angle) * 25);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(perimeterLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(255, 180, 0).interpolation(2, 0);
                    wellTridents.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                for (int i = 0; i < 12; i++) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 0.8f + (i * 0.03f));
                }
            }

            // Tridents converge on center (20-70 ticks = 2.5s flight)
            if (launched && ticksAlive - launchTick < 50 && ticksAlive - launchTick > 0) {
                float progress = (ticksAlive - launchTick) / 50.0f;
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI / 12) * i;
                    double currentDist = 25.0 * (1.0 - progress);
                    double currentY = 3 - progress * 2.5;
                    Location flyLoc = center.clone().add(
                        Math.cos(angle) * currentDist, currentY, Math.sin(angle) * currentDist);
                    if (i < wellTridents.size()) {
                        wellTridents.get(i).entity().teleport(flyLoc);
                    }
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.crimsonDust(flyLoc, 4, 0.1);
                    }
                }
            }

            // Convergence impact (tick 70)
            if (launched && ticksAlive - launchTick == 50) {
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 2, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentGravityWell(plugin); }
    }

    // ================================================================
    // #112 — CHAIN LIGHTNING TRIDENTS
    // 5 electrified tridents that summon lightning on impact + chain
    // ================================================================
    public static class ChainLightningTridents extends BossAttack {
        private final List<BlockDisplayHandle> lightningHandles = new ArrayList<>();
        private boolean launched = false;
        private int launchTick = 0;

        public ChainLightningTridents(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_chain_lightning", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0); // 13 hearts per trident
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0); // 8 hearts lightning
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — lightning arcs between hands
            DisplayBuilder.purpleDust(center.clone().add(0, 6, 0), 15, 1.0);
            DisplayBuilder.purpleDust(center.clone().add(0, 6, 0), 10, 0.5);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10 && !launched) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 6, 0), 8, 0.8);
                }
                return;
            }

            // Launch 5 electrified tridents (tick 10, staggered 0.4s = 8 ticks each)
            if (ticksAlive >= 10 && ticksAlive < 50 && (ticksAlive - 10) % 8 == 0) {
                int tridentIdx = (ticksAlive - 10) / 8;
                if (tridentIdx < 5 && lightningHandles.size() <= tridentIdx) {
                    if (!launched) { launched = true; launchTick = 10; }
                    double angle = (Math.PI / 4) * (tridentIdx - 2);
                    Location origin = center.clone().add(0, 8, 0);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(origin, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 1.0f).glow(0, 150, 255).interpolation(2, 0);
                    lightningHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.9f, 1.3f);
                }
            }

            // Animate tridents outward
            for (int i = 0; i < lightningHandles.size(); i++) {
                BlockDisplayHandle handle = lightningHandles.get(i);
                if (handle.entity() == null || !handle.entity().isValid()) continue;
                Location loc = handle.entity().getLocation();
                double angle = (Math.PI / 4) * (i - 2);
                int age = ticksAlive - (10 + i * 8);
                if (age > 0 && age < 30) {
                    float dist = age * 1.3f; // 130% speed
                    Location flyLoc = center.clone().add(
                        Math.cos(angle) * dist, 8 - age * 0.25, Math.sin(angle) * dist);
                    handle.entity().teleport(flyLoc);
                    // Electrified trail
                    if (age % 2 == 0) {
                        DisplayBuilder.purpleDust(flyLoc, 3, 0.2);
                        DisplayBuilder.purpleDust(flyLoc, 3, 0.2);
                    }
                }
                // Impact — lightning bolt
                if (age == 30) {
                    Location impactLoc = handle.entity().getLocation();
                    triggerImpactDamage(impactLoc);
                    // Lightning visual
                    DisplayBuilder.purpleDust(impactLoc, 30, 1.5);
                    DisplayBuilder.purpleDust(impactLoc, 15, 3.0);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f);
                    // Fire zone
                    DisplayBuilder.crimsonDust(impactLoc, 10, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChainLightningTridents(plugin); }
    }

    // ================================================================
    // #113 — BRIMSTONE TIDE
    // Wall of brimstone sweeps across arena + aerial tridents
    // ================================================================
    public static class BrimstoneTide extends BossAttack {
        private final List<BlockDisplayHandle> tideHandles = new ArrayList<>();
        private boolean tideActive = false;
        private int tideTick = 0;
        private int tideDirection = 1; // 1 = north to south

        public BrimstoneTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_tide", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0); // 10 hearts per second in tide
            config.setDamageRadius(2.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph — crimson liquid rises at arena edge
            Location edgeLoc = center.clone().add(0, 0, -25);
            for (int x = -10; x <= 10; x++) {
                DisplayBuilder.crimsonDust(edgeLoc.clone().add(x, 1, 0), 5, 0.5);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    Location edge = center.clone().add(0, 1, -25);
                    DisplayBuilder.crimsonDust(edge, 20, 10.0);
                    DisplayBuilder.crimsonDust(edge, 10, 5.0);
                }
                return;
            }

            // Tide spawns and sweeps (10-110 ticks = 5 seconds)
            if (ticksAlive == 10 && !tideActive) {
                tideActive = true;
                tideTick = ticksAlive;
                // Create tide wall segments
                for (int x = -12; x <= 12; x += 2) {
                    for (int y = 0; y < 4; y++) {
                        Location tideLoc = center.clone().add(x, y + 0.1, -25);
                        BlockDisplayHandle tideBlock = displayBuilder.spawnBlock(tideLoc, Material.RED_STAINED_GLASS);
                        tideBlock.scale(2.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                        tideHandles.add(tideBlock);
                        spawnedEntities.add(tideBlock.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.AMBIENT_BASALT_DELTAS_LOOP, 0.8f, 0.6f);
            }

            // Sweep tide across arena
            if (tideActive && ticksAlive - tideTick < 100) {
                float sweepProgress = (ticksAlive - tideTick) / 100.0f;
                double zOffset = -25 + (50 * sweepProgress * tideDirection);
                int idx = 0;
                for (int x = -12; x <= 12; x += 2) {
                    for (int y = 0; y < 4; y++) {
                        if (idx < tideHandles.size()) {
                            Location newLoc = center.clone().add(x, y + 0.1, zOffset);
                            tideHandles.get(idx).entity().teleport(newLoc);
                        }
                        idx++;
                    }
                }
                // Tide particles
                if (ticksAlive % 3 == 0) {
                    Location tideFront = center.clone().add(0, 2, zOffset);
                    DisplayBuilder.crimsonDust(tideFront, 60, 12.0);
                    DisplayBuilder.crimsonDust(tideFront, 40, 12.0);
                }
            }

            // Aerial tridents at airborne players (every 30 ticks = 1.5s)
            if (tideActive && ticksAlive % 30 == 0 && ticksAlive - tideTick < 100) {
                Location elevated = center.clone().add(0, 20, 0);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(elevated, Material.PRISMARINE_BRICKS);
                trident.scale(0.12f, 0.12f, 0.9f).glow(255, 100, 0).interpolation(2, 0);
                spawnedEntities.add(trident.entity());
                DisplayBuilder.playSound(elevated, Sound.ENTITY_GUARDIAN_ATTACK, 0.7f, 1.0f);
                DisplayBuilder.crimsonDust(elevated, 5, 0.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneTide(plugin); }
    }

    // ================================================================
    // #114 — GROUND SLAM — FISSURE STRIKE
    // Aerial descent + 3 fissure lines radiating in Y-fork
    // ================================================================
    public static class FissureStrike extends BossAttack {
        private final List<BlockDisplayHandle> fissureHandles = new ArrayList<>();
        private boolean impacted = false;

        public FissureStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_fissure_strike", AttackType.BOSS, 5), "calamitas");
            config.setDamage(36.0); // 18 hearts direct slam
            config.setDamageRadius(3.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(160);
            config.setTicksBetweenDamage(20); // 10 hearts per second in fissure
            config.setDamageOnImpactOnly(false);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — fast descent with crimson trail
            DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 15, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Descent telegraph (0-10 ticks)
            if (ticksAlive < 10 && !impacted) {
                float yPos = 20 - (ticksAlive * 2.0f);
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, yPos, 0), 15, 1.0);
                }
            }
            // Impact (tick 10)
            else if (ticksAlive == 10 && !impacted) {
                impacted = true;
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 1, 0.5);
                DisplayBuilder.crimsonDust(center, 80, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.9f, 0.6f);

                // Create 3 fissure lines (Y-fork: straight, +30deg, -30deg)
                double[] angles = { 0, Math.PI / 6, -Math.PI / 6 };
                for (double angle : angles) {
                    for (int d = 1; d <= 12; d++) {
                        Location fissureLoc = center.clone().add(
                            Math.cos(angle) * d * 1.0, 0.05, Math.sin(angle) * d * 1.0);
                        BlockDisplayHandle fissure = displayBuilder.spawnBlock(fissureLoc, Material.MAGMA_BLOCK);
                        fissure.scale(2.0f, 0.05f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                        fissureHandles.add(fissure);
                        spawnedEntities.add(fissure.entity());
                    }
                }
            }
            // Fissure active damage zone (10-170 ticks = 8 seconds)
            if (impacted && ticksAlive > 10 && ticksAlive < 170) {
                if (ticksAlive % 6 == 0) {
                    double[] angles = { 0, Math.PI / 6, -Math.PI / 6 };
                    for (double angle : angles) {
                        for (int d = 2; d <= 12; d += 3) {
                            Location fissureLoc = center.clone().add(
                                Math.cos(angle) * d, 0.3, Math.sin(angle) * d);
                            DisplayBuilder.crimsonDust(fissureLoc, 3, 0.5);
                            DisplayBuilder.crimsonDust(fissureLoc, 2, 0.5);
                        }
                    }
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FissureStrike(plugin); }
    }

    // ================================================================
    // #115 — GROUND PHASE — MELEE FLURRY
    // 5-hit melee combo at close range
    // ================================================================
    public static class MeleeFlurry extends BossAttack {
        private int hitsDelivered = 0;
        private boolean engaged = false;

        public MeleeFlurry(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_melee_flurry", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0); // 13 hearts per hit
            config.setDamageRadius(2.5);
            config.setDurationTicks(100);
            config.setCooldownTicks(100);
            config.setTicksBetweenDamage(8); // 5 hits in 2 seconds
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — speed dash toward target
            DisplayBuilder.crimsonDust(center, 20, 2.0);
            DisplayBuilder.purpleDust(center, 10, 1.5);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dash approach (0-10 ticks)
            if (ticksAlive < 10 && !engaged) {
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.crimsonDust(center, 20, 1.5);
                }
            }
            // Engaged — 5 hits over 40 ticks (8 ticks each)
            else if (ticksAlive >= 10 && ticksAlive < 50) {
                engaged = true;
                if ((ticksAlive - 10) % 8 == 0 && hitsDelivered < 5) {
                    hitsDelivered++;
                    DisplayBuilder.crimsonDust(center, 15, 1.0);
                    DisplayBuilder.crimsonDust(center, 1, 0.5);
                    DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.9f + (hitsDelivered * 0.05f));
                }
            }
            // Stagger recovery (tick 50-56 = 0.3s)
            else if (ticksAlive >= 50 && ticksAlive < 56) {
                if (ticksAlive == 50) {
                    DisplayBuilder.purpleDust(center, 5, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MeleeFlurry(plugin); }
    }

    // ================================================================
    // #116 — GROUND PHASE — CORRUPTION RING
    // Expanding ring wave + second faster ring
    // ================================================================
    public static class CorruptionRing extends BossAttack {
        private boolean firstRingFired = false;
        private boolean secondRingFired = false;
        private int firstRingTick = 0;
        private int secondRingTick = 0;

        public CorruptionRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_corruption_ring", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); // 14 hearts on ring contact
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(160);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — palm slam, expanding ring on floor
            DisplayBuilder.purpleDust(center, 20, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.9f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(center, 10, 1.5);
                }
                return;
            }

            // First ring (tick 10, expands at 8 blocks/second)
            if (ticksAlive == 10 && !firstRingFired) {
                firstRingFired = true;
                firstRingTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
            }

            // Animate first ring
            if (firstRingFired && ticksAlive - firstRingTick < 40) {
                float radius = (ticksAlive - firstRingTick) * 0.4f; // 8 blocks/second
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 24; i++) {
                        double angle = (2 * Math.PI / 24) * i;
                        Location ringLoc = center.clone().add(
                            Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);
                        DisplayBuilder.purpleDust(ringLoc, 2, 0.2);
                        DisplayBuilder.crimsonDust(ringLoc, 2, 0.3);
                    }
                }
            }

            // Second ring (tick 70, faster at 12 blocks/second)
            if (ticksAlive == 70 && !secondRingFired) {
                secondRingFired = true;
                secondRingTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 1.0f);
            }

            // Animate second ring (faster)
            if (secondRingFired && ticksAlive - secondRingTick < 30) {
                float radius = (ticksAlive - secondRingTick) * 0.6f; // 12 blocks/second
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 24; i++) {
                        double angle = (2 * Math.PI / 24) * i;
                        Location ringLoc = center.clone().add(
                            Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);
                        DisplayBuilder.purpleDust(ringLoc, 3, 0.2);
                        DisplayBuilder.crimsonDust(ringLoc, 3, 0.3);
                    }
                }
            }

            // Post-ring tridents at withered players (tick 100)
            if (ticksAlive == 100) {
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI / 2) * i;
                    Location tridentLoc = center.clone().add(Math.cos(angle) * 8, 2, Math.sin(angle) * 8);
                    DisplayBuilder.crimsonDust(tridentLoc, 10, 0.5);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionRing(plugin); }
    }

    // ================================================================
    // #117 — GROUND PHASE — SEISMIC STOMP GRID
    // 6 stomps over 9 seconds, 5x5 shockwave each
    // ================================================================
    public static class SeismicStompGrid extends BossAttack {
        private int stompsDelivered = 0;
        private static final int MAX_STOMPS = 6;

        public SeismicStompGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_seismic_stomp", AttackType.BOSS, 5), "calamitas");
            config.setDamage(32.0); // 16 hearts per stomp
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(5);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            // No telegraph — first stomp has no warning
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Stomp every 30 ticks (1.5 seconds) for 6 stomps
            if (ticksAlive % 30 == 0 && stompsDelivered < MAX_STOMPS) {
                stompsDelivered++;
                // 5x5 shockwave at current position
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 1, 1.0);
                DisplayBuilder.crimsonDust(center, 60, 3.0);
                DisplayBuilder.purpleDust(center, 20, 2.5);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_STEP, 1.0f, 0.7f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);

                // Stomp crater mark
                BlockDisplayHandle crater = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.02, 0), Material.CRACKED_STONE_BRICKS);
                crater.scale(5.0f, 0.04f, 5.0f).glow(128, 0, 255).interpolation(3, 0);
                spawnedEntities.add(crater.entity());

                // Walk trail
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 2, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SeismicStompGrid(plugin); }
    }

    // ================================================================
    // #118 — GROUND PHASE — DRAG WALK
    // Slow deliberate walk toward player, grab + fling on contact
    // ================================================================
    public static class DragWalk extends BossAttack {
        private boolean grabbed = false;
        private int walkStartTick = 0;

        public DragWalk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_drag_walk", AttackType.BOSS, 5), "calamitas");
            config.setDamage(56.0); // 28 hearts on grab completion
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(5);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — lock-on beam to target
            DisplayBuilder.purpleDust(center, 10, 1.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(center, 10, 2.0);
                }
                return;
            }

            // Walk phase (10-110 ticks = 5 seconds)
            if (ticksAlive >= 10 && ticksAlive < 110 && !grabbed) {
                if (walkStartTick == 0) walkStartTick = ticksAlive;

                // Walk trail particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 3, 0.5);
                }

                // Lock-on beam
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center, 10, 1.5);
                }

                // Passive trident throws at other players every 40 ticks
                if ((ticksAlive - 10) % 40 == 0) {
                    Location tridentLoc = center.clone().add(0, 5, 0);
                    DisplayBuilder.crimsonDust(tridentLoc, 8, 0.5);
                    DisplayBuilder.playSound(tridentLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 1.0f);
                }
            }

            // Grab check — if close enough
            if (ticksAlive >= 30 && !grabbed && center.getWorld() != null) {
                Player target = getTargetPlayer();
                if (target != null && target.isOnline()) {
                    if (target.getLocation().distanceSquared(center) <= 4.0) {
                        grabbed = true;
                        // Fling target backward
                        Vector knockback = target.getLocation().toVector().subtract(center.toVector()).normalize().multiply(2.5);
                        knockback.setY(0.5);
                        target.setVelocity(knockback);
                        DisplayBuilder.crimsonDust(center, 3, 1.0);
                        DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);
                        DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.7f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DragWalk(plugin); }
    }

    // ================================================================
    // #119 — GROUND PHASE — PERMANENT BURN ZONE
    // 7-block-radius corruption zone that persists all of Phase 3
    // ================================================================
    public static class PermanentBurnZone extends BossAttack {
        private boolean zonePlaced = false;
        private final List<BlockDisplayHandle> zoneHandles = new ArrayList<>();

        public PermanentBurnZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_permanent_burn_zone", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0); // 8 hearts per second
            config.setDamageRadius(7.0);
            config.setDurationTicks(6000); // Long duration — persists
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — circular flare on floor
            DisplayBuilder.crimsonDust(center, 20, 4.0);
            DisplayBuilder.crimsonDust(center, 30, 7.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 10, 5.0);
                    DisplayBuilder.crimsonDust(center, 15, 7.0);
                }
                return;
            }

            // Place zone (tick 10)
            if (ticksAlive == 10 && !zonePlaced) {
                zonePlaced = true;
                // Create circular burn zone using block displays
                for (int dx = -7; dx <= 7; dx += 2) {
                    for (int dz = -7; dz <= 7; dz += 2) {
                        if (dx * dx + dz * dz <= 49) { // Within radius 7
                            Location zoneLoc = center.clone().add(dx, 0.03, dz);
                            BlockDisplayHandle zone = displayBuilder.spawnBlock(zoneLoc, Material.MAGMA_BLOCK);
                            zone.scale(2.0f, 0.04f, 2.0f).glow(200, 0, 50).interpolation(5, 0);
                            zoneHandles.add(zone);
                            spawnedEntities.add(zone.entity());
                        }
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
            }

            // Persistent zone particles
            if (zonePlaced && ticksAlive % 10 == 0) {
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * 7;
                    Location particleLoc = center.clone().add(
                        Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist);
                    DisplayBuilder.crimsonDust(particleLoc, 1, 0.3);
                    DisplayBuilder.crimsonDust(particleLoc, 2, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PermanentBurnZone(plugin); }
    }

    // ================================================================
    // #120 — DEVASTATION
    // Ground slam follow-up: eruption wave + trident carpet
    // ================================================================
    public static class Devastation extends BossAttack {
        private boolean erupted = false;
        private final List<BlockDisplayHandle> eruptionHandles = new ArrayList<>();

        public Devastation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_devastation", AttackType.BOSS, 5), "calamitas");
            config.setDamage(30.0); // 15 hearts eruption
            config.setDamageRadius(4.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(32.0); // 16 hearts
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — ground tremor
            DisplayBuilder.crimsonDust(center, 25, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.9f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 10, 4.0);
                }
                return;
            }

            // Eruption wave (tick 10)
            if (ticksAlive == 10 && !erupted) {
                erupted = true;
                // 8 eruption pillars in a ring
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i;
                    Location eruptLoc = center.clone().add(
                        Math.cos(angle) * 6, 0.1, Math.sin(angle) * 6);
                    BlockDisplayHandle pillar = displayBuilder.spawnBlock(eruptLoc, Material.MAGMA_BLOCK);
                    pillar.scale(1.0f, 4.0f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                    eruptionHandles.add(pillar);
                    spawnedEntities.add(pillar.entity());
                    triggerImpactDamage(eruptLoc);
                }
                DisplayBuilder.crimsonDust(center, 1, 3.0);
                DisplayBuilder.crimsonDust(center, 40, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
            }

            // Eruption pillars grow and pulse (10-60 ticks)
            if (erupted && ticksAlive > 10 && ticksAlive < 60) {
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double angle = (2 * Math.PI / 8) * i;
                        Location pillarTop = center.clone().add(
                            Math.cos(angle) * 6, 4.5, Math.sin(angle) * 6);
                        DisplayBuilder.crimsonDust(pillarTop, 5, 0.5);
                        DisplayBuilder.crimsonDust(pillarTop, 3, 0.5);
                    }
                }
            }

            // Trident carpet (tick 60-100)
            if (ticksAlive >= 60 && ticksAlive < 100 && (ticksAlive - 60) % 5 == 0) {
                double x = (Math.random() - 0.5) * 20;
                double z = (Math.random() - 0.5) * 20;
                Location tridentLoc = center.clone().add(x, 0.1, z);
                DisplayBuilder.crimsonDust(tridentLoc, 8, 0.5);
                DisplayBuilder.playSound(tridentLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Devastation(plugin); }
    }
}
