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
 * Phase 3 Boss (Dweller) — TIER 1 "THE STALKER" Attacks #1-10
 * HP range: 100%-80%. The Dweller sizes up players with slow, deliberate
 * attacks. Corruption trail begins. Line-of-sight tension introduced.
 * Damage range: 8-14 HP (4-7 hearts).
 * NO status effects — damage only.
 */
public final class StalkerStrike {

    private StalkerStrike() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SlowMarch(plugin));
        registry.register(new VoidGaze(plugin));
        registry.register(new Lunge(plugin));
        registry.register(new ShadowStep(plugin));
        registry.register(new CorruptionSeed(plugin));
        registry.register(new TheStare(plugin));
        registry.register(new BrimstoneSpit(plugin));
        registry.register(new CreepingDread(plugin));
        registry.register(new MarkSurge(plugin));
        registry.register(new GroundCrack(plugin));
    }

    // ================================================================
    // 1. SLOW MARCH — Relentless slow advance toward Marked player
    // ================================================================
    public static class SlowMarch extends BossAttack {
        private final List<BlockDisplayHandle> trailHandles = new ArrayList<>();
        private int stepTick = 0;

        public SlowMarch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_slow_march", AttackType.BOSS, 3), "dweller");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller silhouette block — dark looming figure
            BlockDisplayHandle body = displayBuilder.spawnBlock(center.clone().add(0, 1, 0), Material.BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 10, 1.5);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            stepTick++;
            // Slow deliberate footstep trail every 20 ticks
            if (stepTick % 20 == 0) {
                float advanceX = (ticksAlive / 200.0f) * 12;
                Location stepLoc = center.clone().add(advanceX, 0.1, 0);
                BlockDisplayHandle step = displayBuilder.spawnBlock(stepLoc, Material.SOUL_SOIL);
                step.scale(0.6f, 0.1f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                trailHandles.add(step);
                spawnedEntities.add(step.entity());
                DisplayBuilder.crimsonDust(stepLoc, 6, 0.8);
                DisplayBuilder.playSound(stepLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.4f);
            }
            // Crimson eye glow intensifying
            if (ticksAlive % 10 == 0) {
                float advanceX = (ticksAlive / 200.0f) * 12;
                DisplayBuilder.crimsonDust(center.clone().add(advanceX, 2.5, 0), 4, 0.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SlowMarch(plugin); }
    }

    // ================================================================
    // 2. VOID GAZE — Staring beam that builds dread pressure
    // ================================================================
    public static class VoidGaze extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();

        public VoidGaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_void_gaze", AttackType.BOSS, 3), "dweller");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller eyes widen — dual eye blocks
            BlockDisplayHandle leftEye = displayBuilder.spawnBlock(
                center.clone().add(-0.3, 2.6, -0.6), Material.ORANGE_STAINED_GLASS);
            leftEye.scale(0.25f, 0.15f, 0.1f).glow(0, 150, 255).interpolation(2, 0);
            spawnedEntities.add(leftEye.entity());
            BlockDisplayHandle rightEye = displayBuilder.spawnBlock(
                center.clone().add(0.3, 2.6, -0.6), Material.ORANGE_STAINED_GLASS);
            rightEye.scale(0.25f, 0.15f, 0.1f).glow(0, 150, 255).interpolation(2, 0);
            spawnedEntities.add(rightEye.entity());
            // Faint gaze beam extending outward
            for (int i = 0; i < 8; i++) {
                Location beamLoc = center.clone().add(0, 2.6, -1.0 - i * 1.2);
                BlockDisplayHandle beam = displayBuilder.spawnBlock(beamLoc, Material.ORANGE_STAINED_GLASS);
                beam.scale(0.08f + i * 0.02f, 0.08f + i * 0.02f, 1.0f)
                    .glow(0, 150, 255).interpolation(3, 0);
                beamHandles.add(beam);
                spawnedEntities.add(beam.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pulsing gaze beam particles
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < 6; i++) {
                    Location particleLoc = center.clone().add(0, 2.6, -1.0 - i * 1.5);
                    DisplayBuilder.dustParticles(particleLoc, 3, 0.3, 0, 150, 255, 0.8f);
                }
            }
            // Eye pulse every 20 ticks
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.crimsonDust(center.clone().add(0, 2.6, 0), 8, 0.6);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidGaze(plugin); }
    }

    // ================================================================
    // 3. LUNGE — Explosive close-range charge with corruption landing
    // ================================================================
    public static class Lunge extends BossAttack {
        private final List<BlockDisplayHandle> smokeTrail = new ArrayList<>();
        private boolean lunged = false;
        private int lungeTick = 0;

        public Lunge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_lunge", AttackType.BOSS, 3), "dweller");
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Crouching form — compact blackstone mass
            BlockDisplayHandle crouch = displayBuilder.spawnBlock(
                center.clone().add(0, 0.5, 0), Material.BLACKSTONE);
            crouch.scale(1.4f, 1.5f, 1.4f).glow(255, 100, 0).interpolation(2, 0);
            spawnedEntities.add(crouch.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 1.2f);
            DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 12, 1.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Crouch telegraph (0-40 ticks)
            if (ticksAlive < 40 && !lunged) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 0.8, 0), 6, 0.8);
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 4, 0.4,
                        255, 100, 0, 1.0f);
                }
            }
            // Lunge fires at tick 40
            else if (ticksAlive == 40 && !lunged) {
                lunged = true;
                lungeTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
            }
            // Lunge travel (40-55 ticks)
            else if (lunged && lungeTick < 15) {
                lungeTick++;
                float progress = lungeTick / 15.0f;
                float lungeZ = progress * -10;
                // Smoke trail behind lunge path
                if (lungeTick % 3 == 0) {
                    Location trailLoc = center.clone().add(0, 0.5, lungeZ + 2);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.SOUL_SOIL);
                    trail.scale(0.5f, 0.3f, 0.5f).glow(200, 0, 50).interpolation(1, 0);
                    smokeTrail.add(trail);
                    spawnedEntities.add(trail.entity());
                    DisplayBuilder.darkPurpleDust(trailLoc, 8, 1.0);
                }
            }
            // Impact — corruption zone created (tick 55)
            else if (lunged && lungeTick == 15) {
                lungeTick++;
                Location impactLoc = center.clone().add(0, 0, -10);
                // Corruption zone ring at landing
                for (int i = 0; i < 8; i++) {
                    double a = (Math.PI * 2 / 8) * i;
                    Location corruptLoc = impactLoc.clone().add(Math.cos(a) * 1.5, 0.1, Math.sin(a) * 1.5);
                    BlockDisplayHandle corrupt = displayBuilder.spawnBlock(corruptLoc, Material.MAGMA_BLOCK);
                    corrupt.scale(0.8f, 0.15f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(corrupt.entity());
                }
                DisplayBuilder.crimsonDust(impactLoc, 20, 2.5);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.5f);
                triggerImpactDamage(impactLoc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Lunge(plugin); }
    }

    // ================================================================
    // 4. SHADOW STEP — Teleport behind target with melee swipe
    // ================================================================
    public static class ShadowStep extends BossAttack {
        private boolean teleported = false;

        public ShadowStep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_shadow_step", AttackType.BOSS, 3), "dweller");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-teleport vanish particles at origin
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, 0), 20, 1.5);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Vanish particles building (0-20 ticks)
            if (ticksAlive < 20 && !teleported) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, 0), 8, 1.2);
                }
            }
            // Teleport behind target (tick 20)
            else if (ticksAlive == 20 && !teleported) {
                teleported = true;
                Location behindLoc = center.clone().add(0, 0, 3);
                // Ghostly translucent reappearance
                BlockDisplayHandle ghost = displayBuilder.spawnBlock(
                    behindLoc.clone().add(0, 1, 0), Material.BLACKSTONE);
                ghost.scale(1.0f, 2.5f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                spawnedEntities.add(ghost.entity());
                DisplayBuilder.darkPurpleDust(behindLoc.clone().add(0, 1.5, 0), 15, 1.0);
                DisplayBuilder.playSound(behindLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.6f);
            }
            // Solidify and swipe (tick 30)
            else if (ticksAlive == 30) {
                Location swipeLoc = center.clone().add(0, 1, 3);
                // Swipe arc — fan of crimson particles
                for (int i = -3; i <= 3; i++) {
                    double a = Math.toRadians(i * 25);
                    Location arcLoc = swipeLoc.clone().add(Math.cos(a) * 2, 0.5, Math.sin(a) * 2);
                    DisplayBuilder.crimsonDust(arcLoc, 4, 0.5);
                }
                DisplayBuilder.playSound(swipeLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowStep(plugin); }
    }

    // ================================================================
    // 5. CORRUPTION SEED — Star-pattern ground corruption deployment
    // ================================================================
    public static class CorruptionSeed extends BossAttack {
        private final List<BlockDisplayHandle> crackHandles = new ArrayList<>();
        private boolean planted = false;

        public CorruptionSeed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_corruption_seed", AttackType.BOSS, 3), "dweller");
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Arm raise — tall narrow block reaching up
            BlockDisplayHandle arm = displayBuilder.spawnBlock(
                center.clone().add(0, 2.5, 0), Material.BLACKSTONE);
            arm.scale(0.3f, 1.5f, 0.3f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(arm.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Arm raise telegraph with dripping particles (0-40 ticks)
            if (ticksAlive < 40 && !planted) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 3.5, 0), 6, 0.5,
                        255, 100, 0, 1.2f);
                }
            }
            // Plant impact (tick 40)
            else if (ticksAlive == 40 && !planted) {
                planted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
                // Star pattern — 4 diagonal lines extending 4 blocks
                double[][] diagonals = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
                for (double[] dir : diagonals) {
                    for (int d = 1; d <= 4; d++) {
                        Location crackLoc = center.clone().add(dir[0] * d, 0.1, dir[1] * d);
                        BlockDisplayHandle crack = displayBuilder.spawnBlock(crackLoc, Material.MAGMA_BLOCK);
                        crack.scale(0.7f, 0.1f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                        crackHandles.add(crack);
                        spawnedEntities.add(crack.entity());
                    }
                }
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 25, 4.0);
            }
            // Corruption sizzle particles (post-plant)
            else if (planted && ticksAlive % 12 == 0) {
                for (BlockDisplayHandle crack : crackHandles) {
                    Location loc = crack.entity().getLocation();
                    DisplayBuilder.crimsonDust(loc.clone().add(0, 0.3, 0), 2, 0.3);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionSeed(plugin); }
    }

    // ================================================================
    // 6. THE STARE — Center-arena pressure stare with dual penalty
    // ================================================================
    public static class TheStare extends BossAttack {
        private final List<BlockDisplayHandle> orbitHandles = new ArrayList<>();

        public TheStare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_the_stare", AttackType.BOSS, 3), "dweller");
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller figure at arena center
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.3f, 3.0f, 1.3f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            // Orbiting soul particles — ring of obsidian fragments
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2 / 6) * i;
                Location orbitLoc = center.clone().add(Math.cos(a) * 1.5, 1.2, Math.sin(a) * 1.5);
                BlockDisplayHandle orb = displayBuilder.spawnBlock(orbitLoc, Material.CRYING_OBSIDIAN);
                orb.scale(0.2f, 0.2f, 0.2f).glow(0, 150, 255).interpolation(3, 0);
                orbitHandles.add(orb);
                spawnedEntities.add(orb.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Slow orbit of fragments around Dweller
            float orbitAngle = ticksAlive * 0.04f;
            for (int i = 0; i < orbitHandles.size(); i++) {
                double a = orbitAngle + (Math.PI * 2 / orbitHandles.size()) * i;
                orbitHandles.get(i).entity().teleport(
                    center.clone().add(Math.cos(a) * 1.5, 1.2, Math.sin(a) * 1.5));
            }
            // White eye glow activates at tick 60
            if (ticksAlive == 60) {
                DisplayBuilder.dustParticles(center.clone().add(0, 2.8, 0), 12, 0.3,
                    255, 255, 255, 1.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.3f);
            }
            // Pulsing white eye during active stare (60-180 ticks)
            if (ticksAlive >= 60 && ticksAlive < 180 && ticksAlive % 15 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 2.8, 0), 6, 0.2,
                    255, 255, 255, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheStare(plugin); }
    }

    // ================================================================
    // 7. BRIMSTONE SPIT — Molten projectile with lava splash
    // ================================================================
    public static class BrimstoneSpit extends BossAttack {
        private BlockDisplayHandle projectile;
        private boolean fired = false;
        private int fireTick = 0;

        public BrimstoneSpit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_brimstone_spit", AttackType.BOSS, 3), "dweller");
            config.setDamage(10.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Head tilting back — throat glow building
            BlockDisplayHandle throat = displayBuilder.spawnBlock(
                center.clone().add(0, 2.2, 0), Material.MAGMA_BLOCK);
            throat.scale(0.4f, 0.4f, 0.4f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(throat.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Throat buildup telegraph (0-50 ticks)
            if (ticksAlive < 50 && !fired) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2.4, 0), 5, 0.3,
                        255, 100, 0, 1.0f);
                }
            }
            // Fire projectile (tick 50)
            else if (ticksAlive == 50 && !fired) {
                fired = true;
                fireTick = 0;
                projectile = displayBuilder.spawnBlock(
                    center.clone().add(0, 2.2, -1), Material.MAGMA_BLOCK);
                projectile.scale(0.6f, 0.6f, 0.6f).glow(255, 100, 0).interpolation(1, 0);
                spawnedEntities.add(projectile.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.8f);
            }
            // Projectile travel (50-90 ticks)
            else if (fired && fireTick < 40) {
                fireTick++;
                float progress = fireTick / 40.0f;
                float z = -1 - progress * 16;
                float arc = (float) Math.sin(progress * Math.PI) * 3;
                if (projectile != null) {
                    projectile.entity().teleport(center.clone().add(0, 2.2 + arc, z));
                }
                // Flame trail
                if (fireTick % 4 == 0) {
                    DisplayBuilder.dustParticles(
                        center.clone().add(0, 2.2 + arc, z + 1), 4, 0.5,
                        255, 100, 0, 0.8f);
                }
            }
            // Ground impact (tick 90)
            else if (fired && fireTick == 40) {
                fireTick++;
                Location impactLoc = center.clone().add(0, 0.1, -17);
                // Lava splash — ring of magma blocks
                for (int i = 0; i < 6; i++) {
                    double a = (Math.PI * 2 / 6) * i;
                    Location splashLoc = impactLoc.clone().add(Math.cos(a) * 2, 0.1, Math.sin(a) * 2);
                    BlockDisplayHandle splash = displayBuilder.spawnBlock(splashLoc, Material.MAGMA_BLOCK);
                    splash.scale(0.5f, 0.1f, 0.5f).glow(255, 100, 0).interpolation(2, 0);
                    spawnedEntities.add(splash.entity());
                }
                DisplayBuilder.crimsonDust(impactLoc, 20, 2.5);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
                triggerImpactDamage(impactLoc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneSpit(plugin); }
    }

    // ================================================================
    // 8. CREEPING DREAD — Low crawl approach, nearly invisible
    // ================================================================
    public static class CreepingDread extends BossAttack {
        private final List<BlockDisplayHandle> crawlTrail = new ArrayList<>();
        private int crawlTick = 0;

        public CreepingDread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_creeping_dread", AttackType.BOSS, 3), "dweller");
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(220);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Low crouching form — barely visible
            BlockDisplayHandle crawlBody = displayBuilder.spawnBlock(
                center.clone().add(0, 0.3, 0), Material.BLACKSTONE);
            crawlBody.scale(1.6f, 0.5f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(crawlBody.entity());
            // Dim eye pinpoints
            BlockDisplayHandle eyes = displayBuilder.spawnBlock(
                center.clone().add(0, 0.5, -0.5), Material.ORANGE_STAINED_GLASS);
            eyes.scale(0.1f, 0.05f, 0.05f).glow(0, 150, 255).interpolation(2, 0);
            spawnedEntities.add(eyes.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            crawlTick++;
            // Slow crawl forward with corruption trail
            if (crawlTick % 15 == 0) {
                float crawlZ = -(crawlTick / 15.0f) * 1.2f;
                Location trailLoc = center.clone().add(0, 0.05, crawlZ);
                BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.SOUL_SOIL);
                trail.scale(0.8f, 0.05f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                crawlTrail.add(trail);
                spawnedEntities.add(trail.entity());
            }
            // Faint purple mist particles
            if (ticksAlive % 12 == 0) {
                float crawlZ = -(crawlTick / 15.0f) * 1.2f;
                DisplayBuilder.darkPurpleDust(
                    center.clone().add(0, 0.4, crawlZ), 3, 0.6);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CreepingDread(plugin); }
    }

    // ================================================================
    // 9. MARK SURGE — Triple homing projectile volley at Marked player
    // ================================================================
    public static class MarkSurge extends BossAttack {
        private final List<BlockDisplayHandle> projectiles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public MarkSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_mark_surge", AttackType.BOSS, 3), "dweller");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(6);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Glow flare from Dweller's position
            DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 15, 1.0);
            DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 10, 0.8,
                255, 0, 128, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Aura flare telegraph (0-40 ticks)
            if (ticksAlive < 40 && !fired) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 8, 0.6,
                        255, 0, 128, 1.2f);
                }
            }
            // Fire 3 projectiles (tick 40)
            else if (ticksAlive == 40 && !fired) {
                fired = true;
                fireTick = 0;
                for (int i = -1; i <= 1; i++) {
                    Location spawnLoc = center.clone().add(i * 1.5, 2, -1);
                    BlockDisplayHandle proj = displayBuilder.spawnBlock(spawnLoc, Material.NETHERRACK);
                    proj.scale(0.35f, 0.35f, 0.35f).glow(200, 0, 50).interpolation(1, 0);
                    projectiles.add(proj);
                    spawnedEntities.add(proj.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 1.0f);
            }
            // Projectiles travel with slight spread (40-70 ticks)
            else if (fired && fireTick < 30) {
                fireTick++;
                float progress = fireTick / 30.0f;
                for (int i = 0; i < projectiles.size(); i++) {
                    float spreadX = (i - 1) * (1.5f - progress * 0.8f);
                    float z = -1 - progress * 14;
                    float arc = (float) Math.sin(progress * Math.PI) * 1.5f;
                    projectiles.get(i).entity().teleport(
                        center.clone().add(spreadX, 2 + arc, z));
                    if (fireTick % 4 == 0) {
                        DisplayBuilder.crimsonDust(
                            center.clone().add(spreadX, 2 + arc, z), 3, 0.3);
                    }
                }
            }
            // Impact (tick 70)
            else if (fired && fireTick == 30) {
                fireTick++;
                Location impactLoc = center.clone().add(0, 0.5, -15);
                DisplayBuilder.crimsonDust(impactLoc, 25, 2.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                triggerImpactDamage(impactLoc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MarkSurge(plugin); }
    }

    // ================================================================
    // 10. GROUND CRACK — Radial shockwave ring with corruption zones
    // ================================================================
    public static class GroundCrack extends BossAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private boolean slammed = false;

        public GroundCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_ground_crack", AttackType.BOSS, 3), "dweller");
            config.setDamage(12.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Arms overhead — tall narrow form
            BlockDisplayHandle arms = displayBuilder.spawnBlock(
                center.clone().add(0, 3.5, 0), Material.BLACKSTONE);
            arms.scale(0.8f, 1.5f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(arms.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Arms-overhead telegraph (0-50 ticks)
            if (ticksAlive < 50 && !slammed) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 4.5, 0), 8, 0.6,
                        255, 100, 0, 1.2f);
                }
            }
            // SLAM (tick 50)
            else if (ticksAlive == 50 && !slammed) {
                slammed = true;
                // Shockwave ring of corruption at 3-block radius
                for (int i = 0; i < 16; i++) {
                    double a = (Math.PI * 2 / 16) * i;
                    Location ringLoc = center.clone().add(Math.cos(a) * 3, 0.1, Math.sin(a) * 3);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                    ring.scale(0.7f, 0.15f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                    ringHandles.add(ring);
                    spawnedEntities.add(ring.entity());
                }
                // Explosion burst at center
                BlockDisplayHandle burst = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.3, 0), Material.NETHERRACK);
                burst.scale(1.5f, 0.4f, 1.5f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(burst.entity());
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 30, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.3f);
                triggerImpactDamage(center);
            }
            // Bleed shockwave expanding outward (50-70 ticks)
            else if (slammed && ticksAlive > 50 && ticksAlive <= 70) {
                float expand = (ticksAlive - 50) / 20.0f;
                float bleedRadius = 3 + expand * 2;
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = (Math.PI * 2 / 8) * i;
                        DisplayBuilder.crimsonDust(
                            center.clone().add(Math.cos(a) * bleedRadius, 0.3, Math.sin(a) * bleedRadius),
                            3, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundCrack(plugin); }
    }
}
