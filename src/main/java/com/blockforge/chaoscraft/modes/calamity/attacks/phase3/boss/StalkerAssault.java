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
 * Phase 3 Boss (Dweller) — TIER 1 "THE STALKER" Attacks #11-20
 * HP range: 100%-80%. Psychological pressure attacks, wall climbing intro,
 * corruption expansion, and the Tier 1 signature charge.
 * Damage range: 8-14 HP (4-7 hearts).
 * NO status effects — damage only.
 */
public final class StalkerAssault {

    private StalkerAssault() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WhisperWalk(plugin));
        registry.register(new BrimstoneStep(plugin));
        registry.register(new EyeLock(plugin));
        registry.register(new FirstClimb(plugin));
        registry.register(new VoidWhisper(plugin));
        registry.register(new Reach(plugin));
        registry.register(new CorruptionPulse(plugin));
        registry.register(new DarkTendril(plugin));
        registry.register(new PatientWatch(plugin));
        registry.register(new FirstBlood(plugin));
    }

    // ================================================================
    // 11. WHISPER WALK — Ghostly circling, psychological pressure
    // ================================================================
    public static class WhisperWalk extends BossAttack {
        private final List<BlockDisplayHandle> ghostTrail = new ArrayList<>();
        private float circleAngle = 0;

        public WhisperWalk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_whisper_walk", AttackType.BOSS, 3), "dweller");
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Semi-transparent ghost form
            BlockDisplayHandle ghost = displayBuilder.spawnBlock(
                center.clone().add(8, 1.2, 0), Material.BLACKSTONE);
            ghost.scale(1.0f, 2.5f, 1.0f).glow(200, 0, 50).interpolation(5, 0);
            spawnedEntities.add(ghost.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Slow circle at 8-block radius
            circleAngle += 0.03f;
            double x = Math.cos(circleAngle) * 8;
            double z = Math.sin(circleAngle) * 8;

            // Portal particle orbit trail
            if (ticksAlive % 6 == 0) {
                Location trailLoc = center.clone().add(x, 1.2, z);
                DisplayBuilder.darkPurpleDust(trailLoc, 5, 0.8);
                // Small ghost fragment trail
                if (ticksAlive % 18 == 0) {
                    BlockDisplayHandle frag = displayBuilder.spawnBlock(trailLoc, Material.OBSIDIAN);
                    frag.scale(0.2f, 0.2f, 0.2f).glow(200, 0, 50).interpolation(3, 0);
                    ghostTrail.add(frag);
                    spawnedEntities.add(frag.entity());
                }
            }
            // Faint ambient sound loop
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(center.clone().add(x, 1.2, z),
                    Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WhisperWalk(plugin); }
    }

    // ================================================================
    // 12. BRIMSTONE STEP — Deliberate S-curve corruption path
    // ================================================================
    public static class BrimstoneStep extends BossAttack {
        private final List<BlockDisplayHandle> pathHandles = new ArrayList<>();
        private int stepIndex = 0;
        private boolean walking = false;

        public BrimstoneStep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_brimstone_step", AttackType.BOSS, 3), "dweller");
            config.setDamage(8.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller form at starting position
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(-10, 1, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Head turns to indicate direction (0-30 ticks)
            if (ticksAlive < 30 && !walking) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(-10, 2.5, 0), 4, 0.3,
                        255, 100, 0, 0.8f);
                }
            }
            // Walking begins (tick 30)
            else if (ticksAlive == 30) {
                walking = true;
            }
            // S-curve walk with corruption trail
            else if (walking && ticksAlive % 8 == 0 && stepIndex < 25) {
                stepIndex++;
                float t = stepIndex / 25.0f;
                double pathX = -10 + t * 20;
                double pathZ = Math.sin(t * Math.PI * 2) * 5;
                Location stepLoc = center.clone().add(pathX, 0.1, pathZ);
                // Corruption zone at each step
                BlockDisplayHandle step = displayBuilder.spawnBlock(stepLoc, Material.MAGMA_BLOCK);
                step.scale(0.8f, 0.12f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                pathHandles.add(step);
                spawnedEntities.add(step.entity());
                // Knee-high lava eruption particles
                DisplayBuilder.crimsonDust(stepLoc.clone().add(0, 0.5, 0), 6, 0.6);
                if (stepIndex % 4 == 0) {
                    DisplayBuilder.playSound(stepLoc, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneStep(plugin); }
    }

    // ================================================================
    // 13. EYE LOCK — Freezing cyan beam with advance
    // ================================================================
    public static class EyeLock extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean locked = false;

        public EyeLock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_eye_lock", AttackType.BOSS, 3), "dweller");
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller figure with intensifying eye glow
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(0, 150, 255).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Eye charge telegraph (0-50 ticks)
            if (ticksAlive < 50 && !locked) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 2.6, 0), 6, 0.4,
                        0, 150, 255, 1.0f);
                }
            }
            // Lock beam (tick 50)
            else if (ticksAlive == 50 && !locked) {
                locked = true;
                // Cyan beam extending outward from eyes
                for (int i = 0; i < 12; i++) {
                    Location beamLoc = center.clone().add(0, 2.6, -1 - i * 1.0);
                    BlockDisplayHandle beam = displayBuilder.spawnBlock(beamLoc, Material.ORANGE_STAINED_GLASS);
                    beam.scale(0.06f, 0.06f, 0.9f).glow(0, 150, 255).interpolation(2, 0);
                    beamHandles.add(beam);
                    spawnedEntities.add(beam.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.6f);
            }
            // Beam pulsing and Dweller advancing (50-110 ticks)
            else if (locked && ticksAlive < 110) {
                if (ticksAlive % 8 == 0) {
                    for (BlockDisplayHandle beam : beamHandles) {
                        Location loc = beam.entity().getLocation();
                        DisplayBuilder.dustParticles(loc, 2, 0.1, 0, 150, 255, 0.6f);
                    }
                }
                // Pulsing cyan glow at target end
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.dustParticles(
                        center.clone().add(0, 2.6, -12), 8, 1.0, 0, 150, 255, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EyeLock(plugin); }
    }

    // ================================================================
    // 14. FIRST CLIMB — Wall climbing introduction, drop landing
    // ================================================================
    public static class FirstClimb extends BossAttack {
        private final List<BlockDisplayHandle> scorchMarks = new ArrayList<>();
        private boolean climbing = false;
        private boolean dropped = false;

        public FirstClimb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_first_climb", AttackType.BOSS, 3), "dweller");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller approaches wall
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(12, 1, 0), Material.BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Walk to wall (0-30 ticks)
            if (ticksAlive < 30 && !climbing) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(12, 1, 0), 4, 0.5);
                }
            }
            // Climbing begins (tick 30)
            else if (ticksAlive == 30 && !climbing) {
                climbing = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.5f);
            }
            // Climbing — scorching handprints ascending (30-90 ticks)
            else if (climbing && !dropped && ticksAlive < 90) {
                int climbTick = ticksAlive - 30;
                if (climbTick % 10 == 0) {
                    float height = (climbTick / 60.0f) * 10;
                    Location handLoc = center.clone().add(13, height, 0);
                    BlockDisplayHandle scorch = displayBuilder.spawnBlock(handLoc, Material.NETHERRACK);
                    scorch.scale(0.3f, 0.3f, 0.1f).glow(255, 100, 0).interpolation(2, 0);
                    scorchMarks.add(scorch);
                    spawnedEntities.add(scorch.entity());
                    // Falling obsidian dust downward
                    DisplayBuilder.darkPurpleDust(handLoc.clone().add(0, -0.5, 0), 4, 0.5);
                    DisplayBuilder.playSound(handLoc, Sound.BLOCK_STONE_PLACE, 0.6f, 0.5f);
                }
            }
            // Pause at apex (90-130 ticks) — eyes staring down
            else if (climbing && !dropped && ticksAlive >= 90 && ticksAlive < 130) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(13, 10, 0), 6, 0.3,
                        0, 150, 255, 1.0f);
                }
            }
            // DROP (tick 130)
            else if (ticksAlive == 130 && !dropped) {
                dropped = true;
                Location landLoc = center.clone().add(10, 0.1, 0);
                // Corruption ring on landing
                for (int i = 0; i < 10; i++) {
                    double a = (Math.PI * 2 / 10) * i;
                    Location ringLoc = landLoc.clone().add(Math.cos(a) * 3, 0.1, Math.sin(a) * 3);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                    ring.scale(0.7f, 0.12f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(ring.entity());
                }
                DisplayBuilder.crimsonDust(landLoc, 25, 3.0);
                DisplayBuilder.playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                DisplayBuilder.playSound(landLoc, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.4f);
                triggerImpactDamage(landLoc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FirstClimb(plugin); }
    }

    // ================================================================
    // 15. VOID WHISPER — Sub-bass shockwave, arena-wide pressure
    // ================================================================
    public static class VoidWhisper extends BossAttack {
        private final List<BlockDisplayHandle> waveHandles = new ArrayList<>();
        private boolean released = false;

        public VoidWhisper(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_void_whisper", AttackType.BOSS, 3), "dweller");
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller raising head — particles condensing inward
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.5, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.3f, 3.0f, 1.3f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Particle condensation inward (0-40 ticks)
            if (ticksAlive < 40 && !released) {
                if (ticksAlive % 6 == 0) {
                    float shrink = 1.0f - (ticksAlive / 40.0f);
                    for (int i = 0; i < 6; i++) {
                        double a = (Math.PI * 2 / 6) * i + ticksAlive * 0.15;
                        float radius = 4 * shrink;
                        DisplayBuilder.darkPurpleDust(
                            center.clone().add(Math.cos(a) * radius, 1.5, Math.sin(a) * radius),
                            3, 0.4);
                    }
                }
            }
            // Release shockwave (tick 40)
            else if (ticksAlive == 40 && !released) {
                released = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.3f);
                DisplayBuilder.crimsonDust(center.clone().add(0, 1.5, 0), 30, 2.0);
            }
            // Shockwave expanding outward (40-70 ticks)
            else if (released && ticksAlive > 40 && ticksAlive <= 70) {
                float expand = (ticksAlive - 40) / 30.0f;
                float waveRadius = expand * 18;
                if (ticksAlive % 4 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double a = (Math.PI * 2 / 12) * i;
                        Location waveLoc = center.clone().add(
                            Math.cos(a) * waveRadius, 0.5, Math.sin(a) * waveRadius);
                        DisplayBuilder.darkPurpleDust(waveLoc, 3, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidWhisper(plugin); }
    }

    // ================================================================
    // 16. REACH — Extended arm grotesque swipe, 5-block range
    // ================================================================
    public static class Reach extends BossAttack {
        private final List<BlockDisplayHandle> armSegments = new ArrayList<>();
        private boolean swiped = false;

        public Reach(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_reach", AttackType.BOSS, 3), "dweller");
            config.setDamage(10.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller body with one arm extending
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Arm extending telegraph (0-40 ticks)
            if (ticksAlive < 40 && !swiped) {
                if (ticksAlive % 8 == 0) {
                    int segCount = ticksAlive / 8;
                    Location segLoc = center.clone().add(0, 1.5, -1 - segCount * 0.8);
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.BASALT);
                    seg.scale(0.25f, 0.25f, 0.7f).glow(255, 100, 0).interpolation(2, 0);
                    armSegments.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                // Brimstone crack glow along arm
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 1.5, -2), 4, 0.5,
                        255, 100, 0, 0.8f);
                }
            }
            // SWIPE (tick 40)
            else if (ticksAlive == 40 && !swiped) {
                swiped = true;
                // Crit particle fan at swipe endpoint
                for (int i = -4; i <= 4; i++) {
                    double a = Math.toRadians(i * 20);
                    Location swipeLoc = center.clone().add(
                        Math.cos(a) * 5, 1.5, -1 + Math.sin(a) * 5);
                    DisplayBuilder.crimsonDust(swipeLoc, 4, 0.5);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.2f, 1.2f);
            }
            // Arm retracting (40-60 ticks)
            else if (swiped && ticksAlive > 40 && ticksAlive <= 60) {
                float retract = (ticksAlive - 40) / 20.0f;
                for (int i = 0; i < armSegments.size(); i++) {
                    Location loc = armSegments.get(i).entity().getLocation();
                    armSegments.get(i).entity().teleport(
                        loc.clone().add(0, 0, retract * 0.6));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Reach(plugin); }
    }

    // ================================================================
    // 17. CORRUPTION PULSE — All corruption zones pulse and expand
    // ================================================================
    public static class CorruptionPulse extends BossAttack {
        private final List<BlockDisplayHandle> pulseHandles = new ArrayList<>();
        private boolean pulsed = false;

        public CorruptionPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_corruption_pulse", AttackType.BOSS, 3), "dweller");
            config.setDamage(10.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller at center, arms spread, head back
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.5f, 3.0f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            // Simulated corruption zone tiles around arena
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 / 8) * i;
                Location zoneLoc = center.clone().add(Math.cos(a) * 6, 0.1, Math.sin(a) * 6);
                BlockDisplayHandle zone = displayBuilder.spawnBlock(zoneLoc, Material.MAGMA_BLOCK);
                zone.scale(1.0f, 0.1f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                pulseHandles.add(zone);
                spawnedEntities.add(zone.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sound buildup from all zone locations (0-50 ticks)
            if (ticksAlive < 50 && !pulsed) {
                if (ticksAlive % 8 == 0) {
                    for (BlockDisplayHandle zone : pulseHandles) {
                        Location loc = zone.entity().getLocation();
                        DisplayBuilder.crimsonDust(loc.clone().add(0, 0.3, 0), 3, 0.4);
                    }
                }
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f, 0.8f);
                }
            }
            // PULSE (tick 50)
            else if (ticksAlive == 50 && !pulsed) {
                pulsed = true;
                // All zones erupt simultaneously
                for (BlockDisplayHandle zone : pulseHandles) {
                    Location loc = zone.entity().getLocation();
                    DisplayBuilder.crimsonDust(loc.clone().add(0, 1.5, 0), 12, 1.0);
                    // Expansion block in each cardinal direction
                    for (double[] dir : new double[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        Location expandLoc = loc.clone().add(dir[0], 0, dir[1]);
                        BlockDisplayHandle expand = displayBuilder.spawnBlock(expandLoc, Material.MAGMA_BLOCK);
                        expand.scale(0.8f, 0.08f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                        spawnedEntities.add(expand.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 20, 6.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionPulse(plugin); }
    }

    // ================================================================
    // 18. DARK TENDRIL — Ground-level whip projectile with trail
    // ================================================================
    public static class DarkTendril extends BossAttack {
        private final List<BlockDisplayHandle> tendrilHandles = new ArrayList<>();
        private boolean launched = false;
        private int travelTick = 0;

        public DarkTendril(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_dark_tendril", AttackType.BOSS, 3), "dweller");
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller extends left hand
            BlockDisplayHandle hand = displayBuilder.spawnBlock(
                center.clone().add(-0.8, 1.3, -0.5), Material.BLACKSTONE);
            hand.scale(0.3f, 0.3f, 0.5f).glow(200, 0, 50).interpolation(2, 0);
            spawnedEntities.add(hand.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Tendril condensing on ground telegraph (0-40 ticks)
            if (ticksAlive < 40 && !launched) {
                if (ticksAlive % 8 == 0) {
                    float dist = (ticksAlive / 40.0f) * 4;
                    DisplayBuilder.darkPurpleDust(
                        center.clone().add(0, 0.2, -dist), 4, 0.5);
                }
            }
            // Launch tendril (tick 40)
            else if (ticksAlive == 40 && !launched) {
                launched = true;
                travelTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.8f);
            }
            // Tendril racing along ground (40-80 ticks)
            else if (launched && travelTick < 40) {
                travelTick++;
                float progress = travelTick / 40.0f;
                float z = -progress * 18;
                // Tendril segment at leading edge
                if (travelTick % 3 == 0) {
                    Location tendrilLoc = center.clone().add(0, 0.15, z);
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(tendrilLoc, Material.SOUL_SOIL);
                    seg.scale(0.3f, 0.1f, 0.6f).glow(200, 0, 50).interpolation(1, 0);
                    tendrilHandles.add(seg);
                    spawnedEntities.add(seg.entity());
                    DisplayBuilder.darkPurpleDust(tendrilLoc.clone().add(0, 0.2, 0), 4, 0.3);
                }
                // Corruption trail left behind
                if (travelTick % 6 == 0) {
                    Location trailLoc = center.clone().add(0, 0.05, z + 2);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.MAGMA_BLOCK);
                    trail.scale(0.5f, 0.05f, 0.5f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(trail.entity());
                }
            }
            // Impact (tick 80)
            else if (launched && travelTick == 40) {
                travelTick++;
                Location impactLoc = center.clone().add(0, 0.3, -18);
                DisplayBuilder.crimsonDust(impactLoc, 15, 1.5);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkTendril(plugin); }
    }

    // ================================================================
    // 19. PATIENT WATCH — Stationary edge positioning, passive dread
    // ================================================================
    public static class PatientWatch extends BossAttack {

        public PatientWatch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_patient_watch", AttackType.BOSS, 3), "dweller");
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller at arena edge, hands at sides
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(14, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.2f, 3.0f, 1.2f).glow(0, 150, 255).interpolation(5, 0);
            spawnedEntities.add(body.entity());
            // Eye glow — small cyan points
            BlockDisplayHandle eyes = displayBuilder.spawnBlock(
                center.clone().add(14, 2.7, -0.5), Material.ORANGE_STAINED_GLASS);
            eyes.scale(0.3f, 0.1f, 0.05f).glow(0, 150, 255).interpolation(2, 0);
            spawnedEntities.add(eyes.entity());
            // Halo particles
            DisplayBuilder.darkPurpleDust(center.clone().add(14, 3.2, 0), 8, 0.6);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Slow halo orbit around head
            if (ticksAlive % 10 == 0) {
                double a = ticksAlive * 0.08;
                DisplayBuilder.darkPurpleDust(
                    center.clone().add(14 + Math.cos(a) * 0.5, 3.2, Math.sin(a) * 0.5),
                    3, 0.2);
            }
            // Eye tracking particles — subtle sweep
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.dustParticles(
                    center.clone().add(14, 2.7, -0.6), 4, 0.2, 0, 150, 255, 0.6f);
            }
            // Ambient silence broken by single deep sound
            if (ticksAlive == 80) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PatientWatch(plugin); }
    }

    // ================================================================
    // 20. FIRST BLOOD — Full-sprint signature charge, Tier 1 finisher
    // ================================================================
    public static class FirstBlood extends BossAttack {
        private final List<BlockDisplayHandle> cometTrail = new ArrayList<>();
        private boolean charging = false;
        private int chargeTick = 0;

        public FirstBlood(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_first_blood", AttackType.BOSS, 3), "dweller");
            config.setDamage(14.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(240);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller at max distance — all brimstone cracks flaring
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(-16, 1.2, 0), Material.BLACKSTONE);
            body.scale(1.4f, 3.0f, 1.4f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Head lowers, brimstone flares to max (0-50 ticks)
            if (ticksAlive < 50 && !charging) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(-16, 2, 0), 8, 0.8,
                        255, 100, 0, 1.5f);
                    DisplayBuilder.crimsonDust(center.clone().add(-16, 1, 0), 6, 0.6);
                }
            }
            // CHARGE (tick 50)
            else if (ticksAlive == 50 && !charging) {
                charging = true;
                chargeTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 1.0f);
            }
            // Sprint across entire arena (50-85 ticks = 1.75 seconds)
            else if (charging && chargeTick < 35) {
                chargeTick++;
                float progress = chargeTick / 35.0f;
                float chargeX = -16 + progress * 32;
                // Comet trail — soul flame behind
                if (chargeTick % 3 == 0) {
                    Location trailLoc = center.clone().add(chargeX - 2, 0.8, 0);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.NETHERRACK);
                    trail.scale(0.4f, 0.3f, 0.4f).glow(255, 100, 0).interpolation(1, 0);
                    cometTrail.add(trail);
                    spawnedEntities.add(trail.entity());
                    DisplayBuilder.crimsonDust(trailLoc, 6, 0.8);
                    DisplayBuilder.dustParticles(trailLoc, 4, 0.5, 255, 100, 0, 1.0f);
                }
                if (chargeTick % 8 == 0) {
                    DisplayBuilder.playSound(center.clone().add(chargeX, 1, 0),
                        Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.6f);
                }
            }
            // IMPACT — leap + shoulder tackle (tick 85)
            else if (charging && chargeTick == 35) {
                chargeTick++;
                Location impactLoc = center.clone().add(16, 0.1, 0);
                // 3x3 corruption zone cluster
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location zoneLoc = impactLoc.clone().add(x, 0.1, z);
                        BlockDisplayHandle zone = displayBuilder.spawnBlock(zoneLoc, Material.MAGMA_BLOCK);
                        zone.scale(0.9f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                        spawnedEntities.add(zone.entity());
                    }
                }
                // Explosion burst
                DisplayBuilder.crimsonDust(impactLoc, 30, 3.5);
                DisplayBuilder.dustParticles(impactLoc, 20, 2.0, 255, 100, 0, 1.5f);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.3f);
                triggerImpactDamage(impactLoc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FirstBlood(plugin); }
    }
}
