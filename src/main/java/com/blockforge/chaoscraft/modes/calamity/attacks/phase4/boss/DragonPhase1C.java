package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.boss;

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
 * Phase 4D Boss — THE VOID EMPEROR (Dragon)
 * Phase 1: "The Awakening" — HP 100% to 65%
 * Attacks #21-30
 * NO status effects — damage only.
 */
public final class DragonPhase1C {

    private DragonPhase1C() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FracturePoint(plugin));
        registry.register(new VoidGaze(plugin));
        registry.register(new TempestCircuit(plugin));
        registry.register(new NullSphere(plugin));
        registry.register(new IronSkyHail(plugin));
        registry.register(new BreathOfConsumed(plugin));
        registry.register(new OrbitCrush(plugin));
        registry.register(new StellarDescent(plugin));
        registry.register(new LatticeOfAnguish(plugin));
        registry.register(new DrowningGrip(plugin));
    }

    // ================================================================
    // 21. FRACTURE POINT — Summon seed eruption (phase 2 preparation)
    // ================================================================
    public static class FracturePoint extends BossAttack {
        private final List<BlockDisplayHandle> fractureHandles = new ArrayList<>();
        private boolean seedPlanted = false;

        public FracturePoint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_fracture_point", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ground cracks — dark red-black particles rising in 3-block circle
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 / 6) * i;
                Location crackPt = center.clone().add(Math.cos(angle) * 1.5, 0.05, Math.sin(angle) * 1.5);
                BlockDisplayHandle crack = displayBuilder.spawnBlock(crackPt, Material.NETHER_BRICKS);
                crack.scale(0.6f, 0.06f, 0.6f).glow(80, 0, 0).interpolation(3, 0);
                fractureHandles.add(crack);
                spawnedEntities.add(crack.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ground crack pulsing (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !seedPlanted) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 8, 2.0);
                }
            }
            // Crack erupts — seed structure spawns (tick 40)
            else if (ticksAlive == 40 && !seedPlanted) {
                seedPlanted = true;
                // Eruption burst
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 20, 3.0);
                // 3x3 end stone platform
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockDisplayHandle platform = displayBuilder.spawnBlock(
                            center.clone().add(dx, 0.02, dz), Material.END_STONE);
                        platform.scale(0.9f, 0.1f, 0.9f).glow(60, 50, 50).interpolation(2, 0);
                        fractureHandles.add(platform);
                        spawnedEntities.add(platform.entity());
                    }
                }
                // PORTAL column above seed — 5 blocks tall
                BlockDisplayHandle seedColumn = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.5, 0), Material.PURPLE_STAINED_GLASS);
                seedColumn.scale(0.6f, 5.0f, 0.6f).glow(80, 0, 160).interpolation(4, 0);
                fractureHandles.add(seedColumn);
                spawnedEntities.add(seedColumn.entity());
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 2.0f, 0.7f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.3f);
            }
            // Seed column pulses (40+ ticks — permanent until phase 2)
            else if (seedPlanted && ticksAlive % 60 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 6, 1.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FracturePoint(plugin); }
    }

    // ================================================================
    // 22. VOID GAZE — Concentrated void bolt from Dragon's eyes
    // ================================================================
    public static class VoidGaze extends BossAttack {
        private final List<BlockDisplayHandle> gazeHandles = new ArrayList<>();
        private boolean boltFired = false;
        private int fireTick = 0;

        public VoidGaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_gaze", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon eye glow — bright violet PORTAL particles from eye positions
            BlockDisplayHandle leftEye = displayBuilder.spawnBlock(
                center.clone().add(-0.5, 7, 2), Material.PURPLE_STAINED_GLASS);
            leftEye.scale(0.3f, 0.3f, 0.3f).glow(200, 50, 255).interpolation(2, 0);
            gazeHandles.add(leftEye);
            spawnedEntities.add(leftEye.entity());

            BlockDisplayHandle rightEye = displayBuilder.spawnBlock(
                center.clone().add(0.5, 7, 2), Material.PURPLE_STAINED_GLASS);
            rightEye.scale(0.3f, 0.3f, 0.3f).glow(200, 50, 255).interpolation(2, 0);
            gazeHandles.add(rightEye);
            spawnedEntities.add(rightEye.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 2.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Eye glow with beam tracing to player (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !boltFired) {
                float beamLength = (ticksAlive / 30.0f) * 20.0f;
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(
                        center.clone().add(0, 7 - beamLength * 0.1, 2 + beamLength), 4, 0.5);
                }
            }
            // Bolt fires from eyes (tick 30) — fast straight line
            else if (ticksAlive == 30 && !boltFired) {
                boltFired = true;
                fireTick = ticksAlive;
                // Bolt beam segments
                for (int seg = 0; seg < 25; seg++) {
                    Location segLoc = center.clone().add(0, 6 - seg * 0.2, 2 + seg * 1.6);
                    BlockDisplayHandle bolt = displayBuilder.spawnBlock(segLoc, Material.PURPLE_STAINED_GLASS);
                    bolt.scale(0.2f, 0.2f, 1.2f).glow(200, 50, 255).interpolation(1, 0);
                    gazeHandles.add(bolt);
                    spawnedEntities.add(bolt.entity());
                }
                triggerImpactDamage(center.clone().add(0, 1, 42));
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 1.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_HURT, 1.0f, 0.7f);
            }
            // Trajectory afterimage line (30-70 ticks = 2 seconds)
            else if (boltFired && ticksAlive - fireTick < 40) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 20), 6, 20.0);
                }
            }
            // Marked player halo (70-170 ticks = 5 seconds)
            else if (boltFired && ticksAlive - fireTick >= 40 && ticksAlive - fireTick < 140) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 42), 4, 1.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidGaze(plugin); }
    }

    // ================================================================
    // 23. TEMPEST CIRCUIT — Three rapid dive passes at decreasing heights
    // ================================================================
    public static class TempestCircuit extends BossAttack {
        private final List<BlockDisplayHandle> circuitHandles = new ArrayList<>();
        private boolean circuitActive = false;
        private int circuitTick = 0;

        public TempestCircuit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_tempest_circuit", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon begins circling — centrifugal spiral particles
            BlockDisplayHandle circleMarker = displayBuilder.spawnBlock(
                center.clone().add(0, 10, 0), Material.PURPLE_STAINED_GLASS);
            circleMarker.scale(2.0f, 2.0f, 2.0f).glow(130, 0, 180).interpolation(3, 0);
            circuitHandles.add(circleMarker);
            spawnedEntities.add(circleMarker.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Circling telegraph — 1.5 loops (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !circuitActive) {
                double circleAngle = ticksAlive * 0.25;
                circuitHandles.get(0).entity().teleport(
                    center.clone().add(Math.cos(circleAngle) * 15, 10, Math.sin(circleAngle) * 15));
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(circuitHandles.get(0).entity().getLocation(), 6, 2.0);
                }
            }
            // Passes begin (tick 40) — 3 passes at different heights
            else if (ticksAlive == 40 && !circuitActive) {
                circuitActive = true;
                circuitTick = ticksAlive;
            }
            // Pass 1 — height 10 (40-70 ticks)
            else if (circuitActive && ticksAlive - circuitTick < 30) {
                float progress = (ticksAlive - circuitTick) / 30.0f;
                Location passLoc = center.clone().add(-25 + progress * 50, 10, 0);
                circuitHandles.get(0).entity().teleport(passLoc);
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(passLoc, 6, 3.0);
                    triggerImpactDamage(passLoc);
                }
            }
            // Pass 2 — height 6 (70-100 ticks)
            else if (circuitActive && ticksAlive - circuitTick >= 30 && ticksAlive - circuitTick < 60) {
                float progress = (ticksAlive - circuitTick - 30) / 30.0f;
                Location passLoc = center.clone().add(0, 6, -25 + progress * 50);
                circuitHandles.get(0).entity().teleport(passLoc);
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(passLoc, 6, 3.0);
                    triggerImpactDamage(passLoc);
                }
                if (ticksAlive - circuitTick == 30) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                }
            }
            // Pass 3 — height 3 (100-130 ticks)
            else if (circuitActive && ticksAlive - circuitTick >= 60 && ticksAlive - circuitTick < 90) {
                float progress = (ticksAlive - circuitTick - 60) / 30.0f;
                Location passLoc = center.clone().add(25 - progress * 50, 3, 5);
                circuitHandles.get(0).entity().teleport(passLoc);
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(passLoc, 6, 3.0);
                    triggerImpactDamage(passLoc);
                }
                if (ticksAlive - circuitTick == 60) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                }
            }
            // Crossed trail aftermath (130-230 ticks = 5 seconds)
            else if (circuitActive && ticksAlive - circuitTick >= 90 && ticksAlive - circuitTick < 190) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 6, 20.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TempestCircuit(plugin); }
    }

    // ================================================================
    // 24. NULL SPHERE — Displacement teleport sphere (no direct damage)
    // ================================================================
    public static class NullSphere extends BossAttack {
        private final List<BlockDisplayHandle> sphereHandles = new ArrayList<>();
        private boolean sphereLaunched = false;
        private int launchTick = 0;

        public NullSphere(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_null_sphere", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Sphere assembles from nothing 15 blocks in front of Dragon
            BlockDisplayHandle sphereCore = displayBuilder.spawnBlock(
                center.clone().add(0, 5, 15), Material.CYAN_STAINED_GLASS);
            sphereCore.scale(0.2f, 0.2f, 0.2f).glow(0, 140, 130).interpolation(4, 0);
            sphereHandles.add(sphereCore);
            spawnedEntities.add(sphereCore.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sphere growing from nothing to 3-block radius (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !sphereLaunched) {
                float scale = 0.2f + (ticksAlive / 40.0f) * 5.8f;
                sphereHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-scale / 2, 5 - scale / 2, 15 - scale / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 15), 8, scale / 2);
                }
            }
            // Sphere launches forward (tick 40)
            else if (ticksAlive == 40 && !sphereLaunched) {
                sphereLaunched = true;
                launchTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.0f);
            }
            // Sphere travels forward (40-100 ticks = 30 blocks at moderate speed)
            else if (sphereLaunched && ticksAlive - launchTick < 60) {
                float dist = (ticksAlive - launchTick) * 0.5f;
                sphereHandles.get(0).entity().teleport(
                    center.clone().add(0, 4, 15 + dist));
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(
                        center.clone().add(0, 4, 15 + dist), 6, 3.0);
                }
            }
            // Sphere dissipates (tick 100)
            else if (sphereLaunched && ticksAlive - launchTick == 60) {
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 45), 15, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 0.6f);
            }
            // Flight trail aftermath (100-160 ticks = 3 seconds dotted line)
            else if (sphereLaunched && ticksAlive - launchTick > 60 && ticksAlive - launchTick < 120) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 30), 4, 15.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NullSphere(plugin); }
    }

    // ================================================================
    // 25. IRON SKY HAIL — 15 blade-shard projectiles raining from above
    // ================================================================
    public static class IronSkyHail extends BossAttack {
        private final List<BlockDisplayHandle> hailHandles = new ArrayList<>();
        private boolean shardsRaining = false;
        private int rainTick = 0;

        public IronSkyHail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_iron_sky_hail", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 4 highest blades shatter — bright spark bursts from above
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 / 4) * i;
                Location shatterPt = center.clone().add(Math.cos(angle) * 5, 15, Math.sin(angle) * 5);
                BlockDisplayHandle shatter = displayBuilder.spawnBlock(shatterPt, Material.IRON_BLOCK);
                shatter.scale(0.4f, 0.4f, 0.4f).glow(255, 255, 255).interpolation(2, 0);
                hailHandles.add(shatter);
                spawnedEntities.add(shatter.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_BREAK, 1.5f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Shattering telegraph (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !shardsRaining) {
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle shatter : hailHandles) {
                        DisplayBuilder.cyanDust(shatter.entity().getLocation(), 6, 1.0);
                    }
                }
            }
            // Shards begin raining (tick 30) — 15 falling blade-shards
            else if (ticksAlive == 30 && !shardsRaining) {
                shardsRaining = true;
                rainTick = ticksAlive;
                for (int i = 0; i < 15; i++) {
                    double ox = (Math.random() - 0.5) * 40;
                    double oz = (Math.random() - 0.5) * 40;
                    Location shardStart = center.clone().add(ox, 30, oz);
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(shardStart, Material.IRON_BLOCK);
                    shard.scale(0.15f, 0.6f, 0.15f).glow(255, 255, 255).interpolation(1, 0);
                    hailHandles.add(shard);
                    spawnedEntities.add(shard.entity());
                }
            }
            // Shards fall (30-60 ticks = 1.5 seconds fast descent)
            else if (shardsRaining && ticksAlive - rainTick < 30) {
                float progress = (ticksAlive - rainTick) / 30.0f;
                for (int i = 4; i < hailHandles.size() && i < 19; i++) {
                    Location shardLoc = hailHandles.get(i).entity().getLocation();
                    hailHandles.get(i).entity().teleport(
                        shardLoc.clone().add(
                            (Math.random() - 0.5) * 0.3,
                            -30 * (1.0 / 30.0),
                            (Math.random() - 0.5) * 0.3));
                }
                // Staggered impacts
                if (ticksAlive % 2 == 0) {
                    int impactIdx = 4 + (ticksAlive - rainTick) / 2;
                    if (impactIdx < hailHandles.size() && impactIdx < 19) {
                        Location impactLoc = hailHandles.get(impactIdx).entity().getLocation();
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 1.0f);
                    }
                }
            }
            // Ground spark afterglow (60-140 ticks = 4 seconds)
            else if (shardsRaining && ticksAlive - rainTick >= 30 && ticksAlive - rainTick < 110) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 8, 20.0);
                }
            }
            // Blade reassembly animation (140-240 ticks = 5 seconds)
            else if (shardsRaining && ticksAlive - rainTick >= 110 && ticksAlive - rainTick < 210) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 6, 5.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IronSkyHail(plugin); }
    }

    // ================================================================
    // 26. BREATH OF THE CONSUMED — Tricolor breath (3 boss essences)
    // ================================================================
    public static class BreathOfConsumed extends BossAttack {
        private final List<BlockDisplayHandle> breathHandles = new ArrayList<>();
        private boolean breathActive = false;
        private int breathTick = 0;

        public BreathOfConsumed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_breath_of_consumed", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon lands — grounded position (rare behavior)
            BlockDisplayHandle bodyCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.END_STONE);
            bodyCore.scale(4.0f, 3.0f, 6.0f).glow(60, 0, 80).interpolation(4, 0);
            breathHandles.add(bodyCore);
            spawnedEntities.add(bodyCore.entity());
            // Mouth cycles through colors — purple/blue/red
            BlockDisplayHandle mouthGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 4), Material.MAGENTA_STAINED_GLASS);
            mouthGlow.scale(1.0f, 1.0f, 1.0f).glow(130, 0, 180).interpolation(3, 0);
            breathHandles.add(mouthGlow);
            spawnedEntities.add(mouthGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Color cycling telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !breathActive) {
                // Cycle mouth glow color
                if (ticksAlive % 10 == 0) {
                    int cycle = (ticksAlive / 10) % 3;
                    Material mouthMat;
                    if (cycle == 0) mouthMat = Material.PURPLE_STAINED_GLASS; // Void
                    else if (cycle == 1) mouthMat = Material.LIGHT_BLUE_STAINED_GLASS; // Hellhound
                    else mouthMat = Material.ORANGE_STAINED_GLASS; // Dweller
                    BlockDisplayHandle newMouth = displayBuilder.spawnBlock(
                        center.clone().add(0, 3, 4), mouthMat);
                    newMouth.scale(1.0f, 1.0f, 1.0f).glow(180, 80, 120).interpolation(2, 0);
                    breathHandles.add(newMouth);
                    spawnedEntities.add(newMouth.entity());
                }
            }
            // Breath fires — 120-degree cone, 15 blocks long (tick 40)
            else if (ticksAlive == 40 && !breathActive) {
                breathActive = true;
                breathTick = ticksAlive;
                // Tricolor cone segments — purple, blue, and orange-red
                for (int seg = 0; seg < 8; seg++) {
                    float width = 1.0f + seg * 1.5f;
                    Location segLoc = center.clone().add(0, 1.5, 4 + seg * 1.8);
                    // Purple stream
                    BlockDisplayHandle purpleSeg = displayBuilder.spawnBlock(segLoc, Material.PURPLE_STAINED_GLASS);
                    purpleSeg.scale(width * 0.4f, 1.5f, 1.2f).glow(130, 0, 180).interpolation(2, 0);
                    breathHandles.add(purpleSeg);
                    spawnedEntities.add(purpleSeg.entity());
                    // Blue stream (soul fire flame = Boss 2)
                    BlockDisplayHandle blueSeg = displayBuilder.spawnBlock(
                        segLoc.clone().add(-width * 0.15, 0, 0), Material.LIGHT_BLUE_STAINED_GLASS);
                    blueSeg.scale(width * 0.3f, 1.0f, 1.0f).glow(100, 180, 255).interpolation(2, 0);
                    breathHandles.add(blueSeg);
                    spawnedEntities.add(blueSeg.entity());
                    // Orange-red stream (flame = Boss 3)
                    BlockDisplayHandle flameSeg = displayBuilder.spawnBlock(
                        segLoc.clone().add(width * 0.15, 0, 0), Material.ORANGE_STAINED_GLASS);
                    flameSeg.scale(width * 0.3f, 1.0f, 1.0f).glow(255, 120, 0).interpolation(2, 0);
                    breathHandles.add(flameSeg);
                    spawnedEntities.add(flameSeg.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.4f);
            }
            // Breath sustained (40-140 ticks = 5 seconds)
            else if (breathActive && ticksAlive - breathTick < 100) {
                if (ticksAlive % 8 == 0) {
                    triggerImpactDamage(center.clone().add(0, 1.5, 12));
                    DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 10), 12, 8.0);
                }
            }
            // Residual tricolor cloud lingers (140-220 ticks = 4 seconds)
            else if (breathActive && ticksAlive - breathTick >= 100 && ticksAlive - breathTick < 180) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 12), 8, 6.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BreathOfConsumed(plugin); }
    }

    // ================================================================
    // 27. ORBIT CRUSH — Blades contract to halo, Dragon patrols low
    // ================================================================
    public static class OrbitCrush extends BossAttack {
        private final List<BlockDisplayHandle> crushHandles = new ArrayList<>();
        private boolean patrolActive = false;
        private int patrolTick = 0;

        public OrbitCrush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_orbit_crush", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Orbit array begins spiraling inward — 8 blades with PORTAL trails
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location bladeLoc = center.clone().add(Math.cos(angle) * 12, 5, Math.sin(angle) * 12);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                blade.scale(0.15f, 0.15f, 1.0f).glow(255, 255, 255).interpolation(2, 0);
                crushHandles.add(blade);
                spawnedEntities.add(blade.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Inward spiral contraction (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !patrolActive) {
                float radius = 12.0f - (ticksAlive / 40.0f) * 10.0f;
                double rotSpeed = 0.15 + (ticksAlive / 40.0) * 0.3;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i + ticksAlive * rotSpeed;
                    crushHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 5, Math.sin(angle) * radius));
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 8, radius);
                }
            }
            // Patrol begins — figure-8 at low altitude (tick 40)
            else if (ticksAlive == 40 && !patrolActive) {
                patrolActive = true;
                patrolTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 2.0f, 1.0f);
            }
            // Figure-8 patrol (40-160 ticks = 6 seconds)
            else if (patrolActive && ticksAlive - patrolTick < 120) {
                float t = (ticksAlive - patrolTick) / 120.0f * (float)(Math.PI * 2);
                // Lemniscate (figure-8) parameterization
                float patrolX = (float)(15 * Math.sin(t));
                float patrolZ = (float)(15 * Math.sin(t) * Math.cos(t));
                Location patrolLoc = center.clone().add(patrolX, 3.5, patrolZ);
                // Move blades as tight halo around patrol position
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i + ticksAlive * 0.5;
                    crushHandles.get(i).entity().teleport(
                        patrolLoc.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2));
                }
                if (ticksAlive % 6 == 0) {
                    triggerImpactDamage(patrolLoc);
                    DisplayBuilder.cyanDust(patrolLoc, 10, 4.0);
                }
            }
            // Blades expand back to normal orbit (160-220 ticks = 3 seconds)
            else if (patrolActive && ticksAlive - patrolTick >= 120 && ticksAlive - patrolTick < 180) {
                float progress = (ticksAlive - patrolTick - 120) / 60.0f;
                float radius = 2.0f + progress * 10.0f;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i + ticksAlive * 0.2;
                    crushHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 5, Math.sin(angle) * radius));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitCrush(plugin); }
    }

    // ================================================================
    // 28. STELLAR DESCENT — Corkscrew spiral dive from maximum height
    // ================================================================
    public static class StellarDescent extends BossAttack {
        private final List<BlockDisplayHandle> descentHandles = new ArrayList<>();
        private boolean descentActive = false;
        private int descentTick = 0;

        public StellarDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_stellar_descent", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon ascends to maximum height — sky streams converge
            BlockDisplayHandle apexBurst = displayBuilder.spawnBlock(
                center.clone().add(0, 50, 0), Material.YELLOW_STAINED_GLASS);
            apexBurst.scale(3.0f, 3.0f, 3.0f).glow(255, 255, 200).interpolation(4, 0);
            descentHandles.add(apexBurst);
            spawnedEntities.add(apexBurst.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Apex telegraph — streams converge (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !descentActive) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 50, 0), 10, 5.0);
                }
            }
            // Corkscrew descent begins (tick 40)
            else if (ticksAlive == 40 && !descentActive) {
                descentActive = true;
                descentTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 1.5f);
            }
            // Spiral descent (40-100 ticks = 3 seconds)
            else if (descentActive && ticksAlive - descentTick < 60) {
                float progress = (ticksAlive - descentTick) / 60.0f;
                float height = 50 * (1.0f - progress);
                // Spiral tightens: 12-block diameter top to 3-block bottom
                float spiralRadius = 6.0f * (1.0f - progress) + 1.5f * progress;
                double spiralAngle = progress * Math.PI * 6; // 3 full rotations
                float spiralX = (float)(Math.cos(spiralAngle) * spiralRadius);
                float spiralZ = (float)(Math.sin(spiralAngle) * spiralRadius);

                descentHandles.get(0).entity().teleport(
                    center.clone().add(spiralX, height, spiralZ));

                // Leave END_ROD helix trail
                if (ticksAlive % 3 == 0) {
                    Location trailPt = center.clone().add(spiralX, height, spiralZ);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailPt, Material.WHITE_STAINED_GLASS);
                    trail.scale(0.4f, 0.4f, 0.4f).glow(255, 255, 255).interpolation(2, 0);
                    descentHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                    DisplayBuilder.cyanDust(trailPt, 4, 1.0);
                    triggerImpactDamage(trailPt);
                }
            }
            // Pullout at bottom (tick 100)
            else if (descentActive && ticksAlive - descentTick == 60) {
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 20, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
            }
            // Helical column lingers (100-160 ticks = 3 seconds)
            else if (descentActive && ticksAlive - descentTick > 60 && ticksAlive - descentTick < 120) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 25, 0), 8, 6.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StellarDescent(plugin); }
    }

    // ================================================================
    // 29. LATTICE OF ANGUISH — Crystal-Dragon beam lattice grid
    // ================================================================
    public static class LatticeOfAnguish extends BossAttack {
        private final List<BlockDisplayHandle> latticeHandles = new ArrayList<>();
        private boolean latticeActive = false;
        private int latticeTick = 0;

        public LatticeOfAnguish(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_lattice_of_anguish", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon above center — crystals orient upward toward it
            BlockDisplayHandle dragonMarker = displayBuilder.spawnBlock(
                center.clone().add(0, 25, 0), Material.END_STONE);
            dragonMarker.scale(2.0f, 2.0f, 2.0f).glow(130, 0, 180).interpolation(3, 0);
            latticeHandles.add(dragonMarker);
            spawnedEntities.add(dragonMarker.entity());
            // 4 crystal nodes at ground level
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 / 4) * i;
                Location crystalLoc = center.clone().add(Math.cos(angle) * 15, 1, Math.sin(angle) * 15);
                BlockDisplayHandle crystal = displayBuilder.spawnBlock(crystalLoc, Material.AMETHYST_BLOCK);
                crystal.scale(1.0f, 1.5f, 1.0f).glow(210, 120, 255).interpolation(3, 0);
                latticeHandles.add(crystal);
                spawnedEntities.add(crystal.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Crystal emissions angle toward Dragon (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !latticeActive) {
                if (ticksAlive % 6 == 0) {
                    for (int i = 1; i <= 4 && i < latticeHandles.size(); i++) {
                        Location cLoc = latticeHandles.get(i).entity().getLocation();
                        DisplayBuilder.cyanDust(cLoc.clone().add(0, 3, 0), 6, 2.0);
                    }
                }
            }
            // Lattice beams fire (tick 40) — crossing beam pattern
            else if (ticksAlive == 40 && !latticeActive) {
                latticeActive = true;
                latticeTick = ticksAlive;
                // Beams from each crystal to Dragon AND to other crystals
                for (int i = 1; i <= 4 && i < latticeHandles.size(); i++) {
                    Location crystalLoc = latticeHandles.get(i).entity().getLocation();
                    // Beam to Dragon above
                    for (int seg = 0; seg < 8; seg++) {
                        float progress = seg / 8.0f;
                        Location beamPt = crystalLoc.clone().add(
                            -crystalLoc.getX() + center.getX() * progress + crystalLoc.getX() * (1 - progress),
                            1 + progress * 24,
                            -crystalLoc.getZ() + center.getZ() * progress + crystalLoc.getZ() * (1 - progress));
                        // Simplify: linear interpolation from crystal to center+25y
                        double bx = crystalLoc.getX() + (center.getX() - crystalLoc.getX()) * progress;
                        double bz = crystalLoc.getZ() + (center.getZ() - crystalLoc.getZ()) * progress;
                        beamPt = new Location(center.getWorld(), bx, 1 + progress * 24, bz);
                        BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(beamPt, Material.AMETHYST_BLOCK);
                        beamSeg.scale(0.3f, 0.3f, 0.3f).glow(210, 120, 255).interpolation(1, 0);
                        latticeHandles.add(beamSeg);
                        spawnedEntities.add(beamSeg.entity());
                    }
                }
                triggerImpactDamage(center.clone().add(0, 5, 0));
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.8f);
            }
            // Beams active (40-100 ticks = 3 seconds)
            else if (latticeActive && ticksAlive - latticeTick < 60) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 12, 0), 10, 10.0);
                }
            }
            // Beams fade — ground imprint (100-180 ticks = 4 seconds)
            else if (latticeActive && ticksAlive - latticeTick >= 60 && ticksAlive - latticeTick < 140) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 6, 15.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LatticeOfAnguish(plugin); }
    }

    // ================================================================
    // 30. DROWNING GRIP — Full island grip, immobilizes all players
    // ================================================================
    public static class DrowningGrip extends BossAttack {
        private final List<BlockDisplayHandle> gripHandles = new ArrayList<>();
        private boolean gripActive = false;
        private int gripTick = 0;

        public DrowningGrip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_drowning_grip", AttackType.BOSS, 4), "dragon");
            config.setDamage(12.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // All PORTAL particles freeze then rush inward
            // Visual: a large contracting ring of purple glass
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location portalPt = center.clone().add(Math.cos(angle) * 25, 5, Math.sin(angle) * 25);
                BlockDisplayHandle portal = displayBuilder.spawnBlock(portalPt, Material.PURPLE_STAINED_GLASS);
                portal.scale(2.0f, 5.0f, 2.0f).glow(80, 0, 150).interpolation(3, 0);
                gripHandles.add(portal);
                spawnedEntities.add(portal.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Particles freeze (0-20 ticks = 1 second freeze)
            if (ticksAlive < 20 && !gripActive) {
                // Static — no movement
            }
            // Particles rush inward (20-40 ticks = 1 second)
            else if (ticksAlive >= 20 && ticksAlive < 40 && !gripActive) {
                float progress = (ticksAlive - 20) / 20.0f;
                float radius = 25 * (1.0f - progress) + 5 * progress;
                for (int i = 0; i < 16 && i < gripHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    gripHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 5, Math.sin(angle) * radius));
                }
            }
            // Grip activates (tick 40) — sustained for 6 seconds
            else if (ticksAlive == 40 && !gripActive) {
                gripActive = true;
                gripTick = ticksAlive;
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 2.0f, 0.3f);
            }
            // Grip sustained (40-160 ticks = 6 seconds)
            else if (gripActive && ticksAlive - gripTick < 120) {
                // Swirling PORTAL particles around each grip point
                if (ticksAlive % 8 == 0) {
                    double swirl = ticksAlive * 0.2;
                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2 / 8) * i + swirl;
                        Location swirlPt = center.clone().add(
                            Math.cos(angle) * 15, 3, Math.sin(angle) * 15);
                        DisplayBuilder.cyanDust(swirlPt, 4, 2.0);
                    }
                }
                // Slowness pulses (every 40 ticks)
                if (ticksAlive % 40 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 12, 20.0);
                }
            }
            // Grip releases (tick 160) — spring-release burst
            else if (gripActive && ticksAlive - gripTick == 120) {
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 25, 20.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DrowningGrip(plugin); }
    }
}
