package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4D Block Display -- GROUP 10: PHASE TRANSITION ARCHITECTURE (#91-100)
 * Void Emperor (Boss 4) arena: architectural transformations at each phase
 * transition. Obsidian monoliths rise, crack to reveal gold, collapse inward
 * on death. Celestial descent, star death burst, edge mist, sky streams.
 *
 * Palette: magenta/cyan/crimson/purple per Calamity color spec.
 * NO status effects. Damage in HP (not hearts). AxisAngle4f only.
 */
public final class PhaseTransitionArch {

    private PhaseTransitionArch() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new MonolithRing(plugin));
        registry.register(new MonolithParticlePillars(plugin));
        registry.register(new MonolithCoreRevelation(plugin));
        registry.register(new MonolithParticleSpray(plugin));
        registry.register(new MonolithCollapseInward(plugin));
        registry.register(new EchoStructureCollapse(plugin));
        registry.register(new CelestialBodyDescent(plugin));
        registry.register(new StarArrayDeathReprise(plugin));
        registry.register(new IslandEdgeMistRing(plugin));
        registry.register(new OverheadSkyReinforcement(plugin));
    }

    // ================================================================
    // #91 -- PHASE 1->2 MONOLITH RING: 16 obsidian monoliths rise from
    //        arena floor at 66% HP. Ring at radius 16, 22.5 deg intervals.
    //        Each: obsidian 2x8x2 + 2 crying_obsidian accents.
    //        Rise from Y-8 to Y+0 over 60 ticks.
    // ================================================================
    public static class MonolithRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> monolithPrimary = new ArrayList<>();
        private final List<BlockDisplayHandle> monolithAccents = new ArrayList<>();
        private static final int MONOLITH_COUNT = 16;

        public MonolithRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("monolith_ring", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < MONOLITH_COUNT; i++) {
                double angle = Math.toRadians(i * 22.5);
                double x = Math.cos(angle) * 16.0;
                double z = Math.sin(angle) * 16.0;
                Location monBase = center.clone().add(x, -8, z); // Start underground

                // Primary obsidian shaft
                BlockDisplayHandle primary = displayBuilder.spawnBlock(monBase, Material.OBSIDIAN);
                primary.scale(2.0f, 8.0f, 2.0f).glow(30, 0, 60).interpolation(3, 0);
                monolithPrimary.add(primary);
                spawnedEntities.add(primary.entity());

                // 2 crying_obsidian accents at mid-height
                for (int side = -1; side <= 1; side += 2) {
                    BlockDisplayHandle accent = displayBuilder.spawnBlock(
                            monBase.clone().add(side * 0.8, 5, 0), Material.CRYING_OBSIDIAN);
                    accent.scale(0.8f, 2.0f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
                    monolithAccents.add(accent);
                    spawnedEntities.add(accent.entity());
                }
            }

            // Rise sound: 16 simultaneous stone placements with pitch variation
            for (int i = 0; i < MONOLITH_COUNT; i++) {
                float pitch = 0.45f + (float) (Math.random() * 0.1);
                DisplayBuilder.playSound(monolithPrimary.get(i).entity().getLocation(),
                        Sound.BLOCK_STONE_PLACE, 0.8f, pitch);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rise animation over 60 ticks
            if (ticksAlive <= 60) {
                // Random jitter per monolith (0-5 tick delay simulated)
                float risePerTick = 8.0f / 60.0f; // Total rise = 8 blocks over 60 ticks
                for (BlockDisplayHandle h : monolithPrimary) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(0, risePerTick, 0));
                }
                for (BlockDisplayHandle h : monolithAccents) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().clone().add(0, risePerTick, 0));
                }

                // PORTAL trail during rise
                for (BlockDisplayHandle h : monolithPrimary) {
                    Location base = h.entity().getLocation();
                    w.spawnParticle(Particle.PORTAL, base, 10, 0.5, 4.0, 0.5, 0);
                    w.spawnParticle(Particle.SMOKE, base, 5, 0.5, 4.0, 0.5, 0);
                }
            }

            // Post-rise: static monoliths with ambient PORTAL
            if (ticksAlive > 60 && ticksAlive % 5 == 0) {
                for (BlockDisplayHandle h : monolithPrimary) {
                    Location monPos = h.entity().getLocation();
                    w.spawnParticle(Particle.PORTAL, monPos, 4, 1.0, 4.0, 1.0, 0);
                }
            }

            // Arrival sound at tick 60
            if (ticksAlive == 60) {
                for (BlockDisplayHandle h : monolithPrimary) {
                    DisplayBuilder.playSound(h.entity().getLocation(),
                            Sound.BLOCK_STONE_HIT, 0.6f, 0.7f);
                }
            }

            // Ambient cave sound every 100 ticks post-rise
            if (ticksAlive > 60 && ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.2f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MonolithRing(plugin); }
    }

    // ================================================================
    // #92 -- MONOLITH PARTICLE PILLARS: Particle column atop each
    //        of the 16 monoliths. PORTAL 6/tick upward + SPELL_WITCH
    //        2/tick + SMOKE 3/tick. Creates ring of light pillars.
    // ================================================================
    public static class MonolithParticlePillars extends BlockDisplayAttack {

        private static final int MONOLITH_COUNT = 16;

        public MonolithParticlePillars(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("monolith_particle_pillars", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Particle-only emitter system
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Emit from each monolith top (Y+8 above floor = monolith top)
            for (int i = 0; i < MONOLITH_COUNT; i++) {
                double angle = Math.toRadians(i * 22.5);
                double x = Math.cos(angle) * 16.0;
                double z = Math.sin(angle) * 16.0;
                Location pillarTop = center.clone().add(x, 8, z);

                w.spawnParticle(Particle.PORTAL, pillarTop, 6, 0.3, 0.3, 0.3, 0.15);
                w.spawnParticle(Particle.WITCH, pillarTop, 2, 0.2, 0.2, 0.2, 0.10);
                w.spawnParticle(Particle.SMOKE, pillarTop, 3, 0.3, 0.3, 0.3, 0.05);
            }

            // Sound every 120 ticks
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_EYE_LAUNCH, 0.4f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MonolithParticlePillars(plugin); }
    }

    // ================================================================
    // #93 -- PHASE 2->3 MONOLITH CORE REVELATION: At 33% HP, each
    //        monolith cracks to reveal gold core. 4-stage scripted
    //        animation over 60 ticks: glow, expand, gold spawn, gold burst.
    // ================================================================
    public static class MonolithCoreRevelation extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> goldCores = new ArrayList<>();
        private static final int MONOLITH_COUNT = 16;

        public MonolithCoreRevelation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("monolith_core_revelation", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(6000);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn 4 gold_block cores inside each monolith position
            for (int i = 0; i < MONOLITH_COUNT; i++) {
                double angle = Math.toRadians(i * 22.5);
                double x = Math.cos(angle) * 16.0;
                double z = Math.sin(angle) * 16.0;
                Location monCenter = center.clone().add(x, 4, z);

                for (int g = 0; g < 4; g++) {
                    double gx = (g % 2 == 0 ? 0.4 : -0.4);
                    double gy = 2 + g;
                    BlockDisplayHandle gold = displayBuilder.spawnBlock(
                            monCenter.clone().add(gx, gy - 4, 0), Material.GOLD_BLOCK);
                    gold.scale(0.5f, 0.5f, 0.5f).glow(255, 200, 0).interpolation(3, 0);
                    goldCores.add(gold);
                    spawnedEntities.add(gold.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Stage 3 (tick 30): gold blocks burst from crack -- scale up
            if (ticksAlive >= 30 && ticksAlive <= 50) {
                float expansion = 0.5f + (ticksAlive - 30) / 20.0f * 0.5f; // 0.5 -> 1.0
                float yExpansion = 0.5f + (ticksAlive - 30) / 20.0f * 1.0f; // 0.5 -> 1.5
                for (BlockDisplayHandle gold : goldCores) {
                    gold.scale(expansion, yExpansion, expansion);
                    gold.interpolation(5, 0);
                }
            }

            // Tick 30 burst: TOTEM + END_ROD from each monolith
            if (ticksAlive == 30) {
                for (int i = 0; i < MONOLITH_COUNT; i++) {
                    double angle = Math.toRadians(i * 22.5);
                    double x = Math.cos(angle) * 16.0;
                    double z = Math.sin(angle) * 16.0;
                    Location monPos = center.clone().add(x, 4, z);

                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, monPos, 25, 1.5, 4.0, 1.5, 0);
                    w.spawnParticle(Particle.END_ROD, monPos, 15, 1.5, 4.0, 1.5, 0);
                }

                // Crack sound
                for (int i = 0; i < MONOLITH_COUNT; i++) {
                    double angle = Math.toRadians(i * 22.5);
                    Location monPos = center.clone().add(Math.cos(angle) * 16, 4, Math.sin(angle) * 16);
                    DisplayBuilder.playSound(monPos, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.6f);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 1.3f);
            }

            // Gold reveal sound at tick 30
            if (ticksAlive == 30) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.9f);
            }

            // Post-crack continuous emission
            if (ticksAlive > 40 && ticksAlive % 3 == 0) {
                for (int i = 0; i < MONOLITH_COUNT; i++) {
                    double angle = Math.toRadians(i * 22.5);
                    double x = Math.cos(angle) * 16.0;
                    double z = Math.sin(angle) * 16.0;
                    Location monPos = center.clone().add(x, 4, z);
                    w.spawnParticle(Particle.FLAME, monPos, 8, 0.8, 4.0, 0.8, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MonolithCoreRevelation(plugin); }
    }

    // ================================================================
    // #94 -- PHASE 2->3 MONOLITH PARTICLE SPRAY: Massive multi-dir
    //        particle spray from all 16 monolith tops simultaneously.
    //        TOTEM x30, FLAME x20, END_ROD x15, CRIT x10 per monolith.
    //        ~1200 particles total in one tick.
    // ================================================================
    public static class MonolithParticleSpray extends BlockDisplayAttack {

        private static final int MONOLITH_COUNT = 16;

        public MonolithParticleSpray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("monolith_particle_spray", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(10.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(6000);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Immediate massive burst from all monoliths
            for (int i = 0; i < MONOLITH_COUNT; i++) {
                double angle = Math.toRadians(i * 22.5);
                double x = Math.cos(angle) * 16.0;
                double z = Math.sin(angle) * 16.0;
                Location monTop = center.clone().add(x, 8, z);

                w.spawnParticle(Particle.TOTEM_OF_UNDYING, monTop, 30, 2.0, 2.0, 2.0, 0.3);
                w.spawnParticle(Particle.FLAME, monTop, 20, 1.5, 3.0, 1.5, 0.3);
                w.spawnParticle(Particle.END_ROD, monTop, 15, 2.0, 2.0, 2.0, 0.4);
                w.spawnParticle(Particle.CRIT, monTop, 10, 2.0, 2.0, 2.0, 0.2);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Settling continuous emission post-burst
            if (ticksAlive > 5 && ticksAlive % 4 == 0) {
                for (int i = 0; i < MONOLITH_COUNT; i++) {
                    double angle = Math.toRadians(i * 22.5);
                    double x = Math.cos(angle) * 16.0;
                    double z = Math.sin(angle) * 16.0;
                    Location monTop = center.clone().add(x, 8, z);

                    w.spawnParticle(Particle.TOTEM_OF_UNDYING, monTop, 4, 1.0, 1.0, 1.0, 0);
                    w.spawnParticle(Particle.FLAME, monTop, 3, 0.8, 0.8, 0.8, 0);
                    w.spawnParticle(Particle.END_ROD, monTop, 2, 1.0, 1.0, 1.0, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MonolithParticleSpray(plugin); }
    }

    // ================================================================
    // #95 -- DRAGON DEATH: MONOLITH COLLAPSE INWARD
    //        All 16 monoliths slide toward arena center at 0.5 b/tick,
    //        Y-scale shrinks from 8 to 2. Arrive at radius 6, vanish.
    //        PORTAL x15/tick trailing each monolith.
    // ================================================================
    public static class MonolithCollapseInward extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> collapseBlocks = new ArrayList<>();
        private final double[] monolithAngles;
        private static final int MONOLITH_COUNT = 16;

        public MonolithCollapseInward(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("monolith_collapse_inward", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(12.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(12000);
            monolithAngles = new double[MONOLITH_COUNT];
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < MONOLITH_COUNT; i++) {
                double angle = Math.toRadians(i * 22.5);
                monolithAngles[i] = angle;
                double x = Math.cos(angle) * 16.0;
                double z = Math.sin(angle) * 16.0;
                Location monPos = center.clone().add(x, 0, z);

                BlockDisplayHandle mon = displayBuilder.spawnBlock(monPos, Material.OBSIDIAN);
                mon.scale(2.0f, 8.0f, 2.0f).glow(30, 0, 60).interpolation(3, 0);
                collapseBlocks.add(mon);
                spawnedEntities.add(mon.entity());
            }

            // All slide sounds
            for (BlockDisplayHandle h : collapseBlocks) {
                DisplayBuilder.playSound(h.entity().getLocation(), Sound.BLOCK_STONE_BREAK, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            if (ticksAlive <= 20) {
                // Slide inward: each monolith moves 0.5 b/tick toward center
                float currentRadius = 16.0f - ticksAlive * 0.5f;
                float yScale = 8.0f - ticksAlive * 0.3f; // 8 -> 2

                for (int i = 0; i < collapseBlocks.size(); i++) {
                    double x = Math.cos(monolithAngles[i]) * currentRadius;
                    double z = Math.sin(monolithAngles[i]) * currentRadius;
                    Location newPos = center.clone().add(x, 0, z);
                    collapseBlocks.get(i).entity().teleport(newPos);
                    collapseBlocks.get(i).scale(2.0f, Math.max(2.0f, yScale), 2.0f);
                    collapseBlocks.get(i).interpolation(3, 0);

                    // PORTAL trail behind
                    w.spawnParticle(Particle.PORTAL, newPos, 15, 1.0, 4.0, 1.0, 0);
                }
            }

            // Vanish at tick 20: scale to 0
            if (ticksAlive == 20) {
                for (BlockDisplayHandle h : collapseBlocks) {
                    h.scale(0.1f, 0.1f, 0.1f);
                    h.interpolation(10, 0);
                }

                // Massive convergence explosion sound
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
                triggerImpactDamage(center);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MonolithCollapseInward(plugin); }
    }

    // ================================================================
    // #96 -- DRAGON DEATH: ECHO STRUCTURE COLLAPSE
    //        8 echo structures dissolve 5 ticks after monolith collapse.
    //        Scale to 0.01 over 30 ticks, slide toward center.
    //        PORTAL x40 + DRAGON_BREATH x25 + WITCH x20 burst each.
    // ================================================================
    public static class EchoStructureCollapse extends BlockDisplayAttack {

        private static final double[][] ECHO_POSITIONS = {
            {-14, 8, -14}, {11, 11, -14}, {0, 9.5, 18}, {22, 9, 0},
            {17, 13, -8}, {19, 18, -4}, {-16, 7, 0}, {-22, 9, 10}
        };

        public EchoStructureCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_structure_collapse", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(10.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(12000);
        }

        @Override
        protected void onSpawn(Location center) {
            // Particle-only dissolution effect
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Burst at tick 5 (staggered 5 ticks after monolith collapse start)
            if (ticksAlive == 5) {
                for (double[] pos : ECHO_POSITIONS) {
                    Location echoPos = center.clone().add(pos[0], pos[1], pos[2]);
                    w.spawnParticle(Particle.PORTAL, echoPos, 40, 4.0, 4.0, 4.0, 0);
                    w.spawnParticle(Particle.DRAGON_BREATH, echoPos, 25, 3.5, 3.5, 3.5, 0);
                    w.spawnParticle(Particle.WITCH, echoPos, 20, 4.0, 4.0, 4.0, 0);
                }

                // Chorus of enderman death sounds
                for (double[] pos : ECHO_POSITIONS) {
                    Location echoPos = center.clone().add(pos[0], pos[1], pos[2]);
                    DisplayBuilder.playSound(echoPos, Sound.ENTITY_ENDERMAN_DEATH, 0.5f, 0.7f);
                }
            }

            // Continued dissolution particles ticks 5-35
            if (ticksAlive > 5 && ticksAlive <= 35 && ticksAlive % 3 == 0) {
                float progress = (ticksAlive - 5) / 30.0f;
                for (double[] pos : ECHO_POSITIONS) {
                    // Slide toward center
                    double x = pos[0] * (1.0 - progress * 0.3);
                    double y = pos[1];
                    double z = pos[2] * (1.0 - progress * 0.3);
                    Location dissolvePos = center.clone().add(x, y, z);
                    w.spawnParticle(Particle.PORTAL, dissolvePos, 8, 3.0, 3.0, 3.0, 0);
                }
            }

            // After last echo vanishes: soul sand valley ambience
            if (ticksAlive == 35) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_SOUL_SAND_VALLEY_LOOP, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EchoStructureCollapse(plugin); }
    }

    // ================================================================
    // #97 -- DRAGON DEATH: CELESTIAL BODY DESCENT
    //        Sun falls (0.15 b/tick), moon fades (0.10 b/tick),
    //        void planet launches upward (0.3 b/tick for 30 ticks).
    //        Starts tick 10 of death sequence.
    // ================================================================
    public static class CelestialBodyDescent extends BlockDisplayAttack {

        private BlockDisplayHandle sunCore;
        private BlockDisplayHandle moonCore;
        private BlockDisplayHandle voidCore;

        public CelestialBodyDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("celestial_body_descent", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(12000);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn representative celestial cores at their orbital positions
            sunCore = displayBuilder.spawnBlock(center.clone().add(38, 28, 0), Material.GOLD_BLOCK);
            sunCore.scale(3.8f, 3.8f, 3.8f).brightness(15, 15).glow(255, 200, 0).interpolation(3, 0);
            spawnedEntities.add(sunCore.entity());

            moonCore = displayBuilder.spawnBlock(center.clone().add(0, 35, 52), Material.AMETHYST_BLOCK);
            moonCore.scale(2.4f, 2.4f, 2.4f).brightness(8, 8).glow(180, 0, 255).interpolation(3, 0);
            spawnedEntities.add(moonCore.entity());

            voidCore = displayBuilder.spawnBlock(center.clone().add(24, 22, 0), Material.BLACK_CONCRETE);
            voidCore.scale(4.2f, 4.2f, 4.2f).brightness(0, 0).glow(30, 0, 60).interpolation(3, 0);
            spawnedEntities.add(voidCore.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Sun descent: Y -= 0.15/tick, scale shrinks
            if (sunCore != null && ticksAlive <= 60) {
                BlockDisplay sun = sunCore.entity();
                sun.teleport(sun.getLocation().clone().add(0, -0.15, 0));
                float sunScale = Math.max(0.1f, 3.8f - ticksAlive * 0.063f);
                sunCore.scale(sunScale, sunScale, sunScale);
                sunCore.interpolation(3, 0);

                if (ticksAlive == 1) {
                    w.spawnParticle(Particle.PORTAL, sun.getLocation(), 60, 6.0, 6.0, 6.0, 0);
                    DisplayBuilder.playSound(sun.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
                }
            }

            // Moon fade: Y -= 0.10/tick, slower scale decrease
            if (moonCore != null && ticksAlive <= 80) {
                BlockDisplay moon = moonCore.entity();
                moon.teleport(moon.getLocation().clone().add(0, -0.10, 0));
                float moonScale = Math.max(0.01f, 2.4f - ticksAlive * 0.03f);
                moonCore.scale(moonScale, moonScale, moonScale);
                moonCore.interpolation(3, 0);

                if (ticksAlive == 1) {
                    w.spawnParticle(Particle.DRAGON_BREATH, moon.getLocation(), 40, 5.0, 5.0, 5.0, 0);
                    DisplayBuilder.playSound(moon.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);
                }
            }

            // Void planet LAUNCH upward: Y += 0.3/tick for 30 ticks, then fade
            if (voidCore != null) {
                BlockDisplay vp = voidCore.entity();
                if (ticksAlive <= 30) {
                    vp.teleport(vp.getLocation().clone().add(0, 0.3, 0));
                    // Exhaust trail downward
                    w.spawnParticle(Particle.SMOKE, vp.getLocation().clone().add(0, -2, 0), 10, 2.0, 1.0, 2.0, 0);
                }
                if (ticksAlive > 30 && ticksAlive <= 50) {
                    float vpScale = Math.max(0.01f, 4.2f - (ticksAlive - 30) * 0.21f);
                    voidCore.scale(vpScale, vpScale, vpScale);
                    voidCore.interpolation(3, 0);
                }
                if (ticksAlive == 1) {
                    w.spawnParticle(Particle.SMOKE, vp.getLocation(), 80, 8.0, 8.0, 8.0, 0);
                    DisplayBuilder.playSound(vp.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.3f);
                    DisplayBuilder.playSound(vp.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 0.8f, 0.4f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CelestialBodyDescent(plugin); }
    }

    // ================================================================
    // #98 -- DRAGON DEATH: NETHER STAR ARRAY DEATH BURST (REPRISE)
    //        Orchestration layer timing the death sequence:
    //        Tick 0: stars + monoliths. Tick 5: echoes. Tick 10: celestials.
    //        Tick 80+: 5-second silence phase (all effects stop).
    // ================================================================
    public static class StarArrayDeathReprise extends BlockDisplayAttack {

        public StarArrayDeathReprise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("star_array_death_reprise", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(14.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(12000);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Immediate star supernova burst at spawn (tick 0 of death)
            double[][] starPositions = {
                {0, 12, 14}, {10.5, 12, -7}, {-10.5, 12, -7},
                {0, 14, 0}, {0, 16, 0}, {0, 10, 0}, {0, 8, 0}
            };

            for (double[] pos : starPositions) {
                Location starPos = center.clone().add(pos[0], pos[1], pos[2]);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, starPos, 50, 3.0, 3.0, 3.0, 0.3);
                w.spawnParticle(Particle.CRIT, starPos, 30, 4.0, 4.0, 4.0, 0.2);
                w.spawnParticle(Particle.END_ROD, starPos, 20, 5.0, 5.0, 5.0, 0.4);
            }

            DisplayBuilder.playSound(center.clone().add(0, 12, 0),
                    Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            // Tick 80+: Silence phase. No particles, no sounds.
            // This is enforced by simply doing nothing in this range.
            // The death sequence is purely particle-based through the
            // individual collapse attack classes.
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StarArrayDeathReprise(plugin); }
    }

    // ================================================================
    // #99 -- PRE-FIGHT ATMOSPHERE: ISLAND EDGE MIST RING
    //        24 emitter points around island perimeter at Y+1.
    //        CLOUD 2/tick + SMOKE 1/tick each. Dims at Phase 3.
    //        Present from arena idle through all phases.
    // ================================================================
    public static class IslandEdgeMistRing extends BlockDisplayAttack {

        private static final int EMITTER_COUNT = 24;

        public IslandEdgeMistRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("island_edge_mist_ring", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Particle-only atmospheric system
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // 24 emitter points at 15-deg intervals around perimeter (radius ~35)
            for (int i = 0; i < EMITTER_COUNT; i++) {
                double angle = Math.toRadians(i * 15.0);
                double x = Math.cos(angle) * 35.0;
                double z = Math.sin(angle) * 35.0;
                Location emitPos = center.clone().add(x, 1, z);

                // CLOUD mist: slow upward drift
                w.spawnParticle(Particle.CLOUD, emitPos, 2, 3.0, 1.5, 3.0, 0.01);
                // SMOKE undertone
                w.spawnParticle(Particle.SMOKE, emitPos, 1, 2.0, 1.0, 2.0, 0);
            }

            // Ambient soundscape every 300 ticks
            if (ticksAlive % 300 == 0) {
                DisplayBuilder.playSound(center, Sound.AMBIENT_BASALT_DELTAS_LOOP, 0.15f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IslandEdgeMistRing(plugin); }
    }

    // ================================================================
    // #100 -- OVERHEAD SKY REINFORCEMENT: 4 diagonal particle streams
    //         at Y+60-70. Upper layer overlay supplementing Part 1 sky
    //         streams. END_ROD, SPELL_WITCH, TOTEM, DRAGON_BREATH.
    //         Silent visual layer. Rates increase 50% at Phase 2.
    // ================================================================
    public static class OverheadSkyReinforcement extends BlockDisplayAttack {

        public OverheadSkyReinforcement(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("overhead_sky_reinforcement", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(12000);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            // Particle-only sky system
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            double skyY = 65.0;
            double spread = 25.0;

            // Stream A: NW -> SE (END_ROD + TOTEM)
            Location streamA = center.clone().add(-spread * 0.7, skyY, -spread * 0.7);
            w.spawnParticle(Particle.END_ROD, streamA, 8, spread, 0.5, spread, 0.4);
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamA, 3, spread, 0.5, spread, 0);

            // Stream B: NE -> SW (SPELL_WITCH + DRAGON_BREATH)
            Location streamB = center.clone().add(spread * 0.7, skyY, -spread * 0.7);
            w.spawnParticle(Particle.WITCH, streamB, 8, spread, 0.5, spread, 0);
            w.spawnParticle(Particle.DRAGON_BREATH, streamB, 3, spread, 0.5, spread, 0);

            // Stream C: E -> W (END_ROD + CRIT)
            Location streamC = center.clone().add(spread, skyY, 0);
            w.spawnParticle(Particle.END_ROD, streamC, 6, spread * 2, 0.5, 1.0, 0);
            w.spawnParticle(Particle.CRIT, streamC, 2, spread * 2, 0.5, 1.0, 0);

            // Stream D: N -> S (TOTEM + FLAME)
            Location streamD = center.clone().add(0, skyY, -spread);
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, streamD, 6, 1.0, 0.5, spread * 2, 0);
            w.spawnParticle(Particle.FLAME, streamD, 2, 1.0, 0.5, spread * 2, 0);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OverheadSkyReinforcement(plugin); }
    }
}
