package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

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
 * Supreme Calamitas — Phase 4E Boss (Boss 5, Final Boss)
 * Phase 1: "The Arrival" — HP 100% to 75%
 * Attacks #41-50
 *
 * Mind-games and combo finishers. The Mirror. Ground phase.
 * Phase threshold warning. First Silence — her signature closer.
 * NO status effects — damage only.
 */
public final class CalamitasArrivalE {

    private CalamitasArrivalE() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TheMirror(plugin));
        registry.register(new CrimsonOrbit(plugin));
        registry.register(new TheImpatient(plugin));
        registry.register(new GroundPhaseDescent(plugin));
        registry.register(new SixPointStar(plugin));
        registry.register(new BrimstoneChain(plugin));
        registry.register(new TridentCurtainCross(plugin));
        registry.register(new Convergence(plugin));
        registry.register(new PhaseThresholdWarning(plugin));
        registry.register(new FirstSilence(plugin));
    }

    // ================================================================
    // 41. THE MIRROR — Fires away from player, bounces back from wall
    // ================================================================
    public static class TheMirror extends BossAttack {
        private final List<BlockDisplayHandle> mirrorHandles = new ArrayList<>();
        private boolean fired = false;
        private boolean bounced = false;
        private int fireTick = 0;

        public TheMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_the_mirror", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle backGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 20, -2), Material.IRON_BLOCK);
            backGlow.scale(0.3f, 0.3f, 0.3f).glow(200, 200, 255).interpolation(3, 0);
            mirrorHandles.add(backGlow);
            spawnedEntities.add(backGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 36 && !fired) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, -3), 5, 0.5);
                }
            } else if (ticksAlive == 36 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                BlockDisplayHandle trident = displayBuilder.spawnBlock(
                    center.clone().add(0, 20, -1), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                trident.scale(0.12f, 0.12f, 1.2f).glow(220, 220, 255).interpolation(1, 0);
                mirrorHandles.add(trident);
                spawnedEntities.add(trident.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
            } else if (fired && !bounced && ticksAlive - fireTick < 20) {
                float dist = (ticksAlive - fireTick) * 2.2f;
                if (mirrorHandles.size() > 1) {
                    mirrorHandles.get(1).entity().teleport(center.clone().add(0, 20, -1 - dist));
                }
            } else if (fired && !bounced && ticksAlive - fireTick == 20) {
                bounced = true;
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, -45), 12, 1.5);
                DisplayBuilder.playSound(center, Sound.BLOCK_METAL_HIT, 1.2f, 1.5f);
            } else if (bounced && ticksAlive - fireTick < 60) {
                float returnDist = (ticksAlive - fireTick - 20) * 2.2f * 0.9f;
                if (mirrorHandles.size() > 1) {
                    mirrorHandles.get(1).entity().teleport(
                        center.clone().add(0, 20, -45 + returnDist));
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0, 20, -45 + returnDist), 4, 0.5);
                }
            } else if (bounced && ticksAlive - fireTick == 60) {
                triggerImpactDamage(center.clone().add(0, 0.5, 40));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 40), 12, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheMirror(plugin); }
    }

    // ================================================================
    // 42. CRIMSON ORBIT — Spiraling skull expands outward from center
    // ================================================================
    public static class CrimsonOrbit extends BossAttack {
        private final List<BlockDisplayHandle> orbitHandles = new ArrayList<>();
        private boolean released = false;
        private int releaseTick = 0;

        public CrimsonOrbit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crimson_orbit", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle skull = displayBuilder.spawnBlock(
                center.clone().add(3, 24, 0), Material.SOUL_SAND);
            skull.scale(0.7f, 0.7f, 0.7f).glow(180, 80, 0).interpolation(2, 0);
            orbitHandles.add(skull);
            spawnedEntities.add(skull.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 40 && !released) {
                double angle = ticksAlive * 0.3;
                if (!orbitHandles.isEmpty()) {
                    orbitHandles.get(0).entity().teleport(
                        center.clone().add(Math.cos(angle) * 3, 24, Math.sin(angle) * 3));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 24, 0), 6, 3.0);
                }
            } else if (ticksAlive == 40 && !released) {
                released = true;
                releaseTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.6f);
            } else if (released && ticksAlive - releaseTick < 80) {
                int sTick = ticksAlive - releaseTick;
                double angle = sTick * 0.25;
                float radius = 3 + sTick * 0.2f;
                double y = 24 - sTick * 0.1;
                if (!orbitHandles.isEmpty()) {
                    orbitHandles.get(0).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, Math.max(2, y), Math.sin(angle) * radius));
                }
                if (sTick % 6 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(Math.cos(angle) * radius, Math.max(2, y), Math.sin(angle) * radius), 6, 1.0);
                }
            } else if (released && ticksAlive - releaseTick == 80) {
                Location detLoc = center.clone().add(16, 0.1, 0);
                BlockDisplayHandle blast = displayBuilder.spawnBlock(detLoc, Material.ORANGE_STAINED_GLASS);
                blast.scale(4.0f, 4.0f, 4.0f).glow(255, 60, 0).interpolation(1, 0);
                spawnedEntities.add(blast.entity());
                triggerImpactDamage(detLoc);
                DisplayBuilder.crimsonDust(detLoc, 20, 4.0);
                DisplayBuilder.playSound(detLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                DisplayBuilder.playSound(detLoc, Sound.BLOCK_LAVA_POP, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonOrbit(plugin); }
    }

    // ================================================================
    // 43. THE IMPATIENT — Rapid 3 aimed tridents in 0.4s intervals
    // ================================================================
    public static class TheImpatient extends BossAttack {
        private final List<BlockDisplayHandle> impHandles = new ArrayList<>();
        private boolean active = false;
        private int activeStartTick = 0;

        public TheImpatient(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_the_impatient", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle hand = displayBuilder.spawnBlock(
                center.clone().add(1, 20, 0), Material.IRON_BLOCK);
            hand.scale(0.3f, 0.3f, 0.3f).glow(200, 200, 255).interpolation(2, 0);
            impHandles.add(hand);
            spawnedEntities.add(hand.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 30 && !active) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(1, 20, 0), 8, 0.8);
                }
            } else if (ticksAlive == 30 && !active) {
                active = true;
                activeStartTick = ticksAlive;
            } else if (active && ticksAlive - activeStartTick < 24) {
                int aTick = ticksAlive - activeStartTick;
                if (aTick % 8 == 0 && aTick / 8 < 3) {
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(
                        center.clone().add(1, 20, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.2f).glow(220, 220, 255).interpolation(1, 0);
                    impHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.1f);
                }
                for (int i = 1; i < impHandles.size(); i++) {
                    int tFire = (i - 1) * 8;
                    if (aTick > tFire) {
                        float dist = (aTick - tFire) * 2.5f;
                        impHandles.get(i).entity().teleport(
                            center.clone().add(1, 20, dist));
                    }
                }
            } else if (active && ticksAlive - activeStartTick >= 24 && ticksAlive - activeStartTick < 50) {
                int aTick = ticksAlive - activeStartTick;
                for (int i = 1; i < impHandles.size(); i++) {
                    int tFire = (i - 1) * 8;
                    float dist = (aTick - tFire) * 2.5f;
                    impHandles.get(i).entity().teleport(
                        center.clone().add(1, 20, dist));
                }
            } else if (active && ticksAlive - activeStartTick == 50) {
                triggerImpactDamage(center.clone().add(1, 0.5, 80));
                DisplayBuilder.crimsonDust(center.clone().add(1, 0.5, 80), 12, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheImpatient(plugin); }
    }

    // ================================================================
    // 44. GROUND PHASE DESCENT — Descends to Y+2, proximity aura, trident
    // ================================================================
    public static class GroundPhaseDescent extends BossAttack {
        private final List<BlockDisplayHandle> groundHandles = new ArrayList<>();
        private boolean grounded = false;
        private int groundStartTick = 0;

        public GroundPhaseDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_ground_phase_descent", AttackType.BOSS, 5), "calamitas");
            config.setDamage(12.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle model = displayBuilder.spawnBlock(
                center.clone().add(0, 20, 0), Material.RED_STAINED_GLASS);
            model.scale(2.0f, 3.0f, 2.0f).glow(180, 0, 0).interpolation(3, 0);
            groundHandles.add(model);
            spawnedEntities.add(model.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 40 && !grounded) {
                float progress = ticksAlive / 40.0f;
                double y = 20 - progress * 18;
                if (!groundHandles.isEmpty()) {
                    groundHandles.get(0).entity().teleport(center.clone().add(0, y, 0));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, y, 0), 10, 2.0);
                }
            } else if (ticksAlive == 40 && !grounded) {
                grounded = true;
                groundStartTick = ticksAlive;
                // Proximity aura ring
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location auraPt = center.clone().add(Math.cos(angle) * 4, 0.5, Math.sin(angle) * 4);
                    BlockDisplayHandle aura = displayBuilder.spawnBlock(auraPt, Material.RED_STAINED_GLASS);
                    aura.scale(1.0f, 2.0f, 1.0f).glow(180, 0, 0).interpolation(2, 0);
                    groundHandles.add(aura);
                    spawnedEntities.add(aura.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.6f);
            } else if (grounded && ticksAlive - groundStartTick < 240) {
                int gTick = ticksAlive - groundStartTick;
                double patrolAngle = gTick * 0.02;
                double px = Math.sin(patrolAngle) * 6;
                double pz = Math.cos(patrolAngle) * 6;
                if (!groundHandles.isEmpty()) {
                    groundHandles.get(0).entity().teleport(center.clone().add(px, 2, pz));
                }
                if (gTick % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(px, 2, pz), 8, 4.0);
                }
                if (gTick % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.7f);
                }
                // Fire aimed trident every 240 ticks (12 seconds)
                if (gTick == 120) {
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(
                        center.clone().add(px, 2, pz), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.2f).glow(220, 220, 255).interpolation(1, 0);
                    spawnedEntities.add(trident.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
                }
            } else if (grounded && ticksAlive - groundStartTick == 240) {
                // Re-ascend
                DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 15, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundPhaseDescent(plugin); }
    }

    // ================================================================
    // 45. SIX-POINT STAR — 6 tridents at 60-degree intervals
    // ================================================================
    public static class SixPointStar extends BossAttack {
        private final List<BlockDisplayHandle> starHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public SixPointStar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_six_point_star", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(60 * i);
                Location pt = center.clone().add(Math.cos(angle) * 3, 22, Math.sin(angle) * 3);
                BlockDisplayHandle point = displayBuilder.spawnBlock(pt, Material.IRON_BLOCK);
                point.scale(0.2f, 0.2f, 0.2f).glow(200, 200, 255).interpolation(3, 0);
                starHandles.add(point);
                spawnedEntities.add(point.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 40 && !fired) {
                double rot = ticksAlive * 0.1;
                for (int i = 0; i < 6 && i < starHandles.size(); i++) {
                    double angle = Math.toRadians(60 * i) + rot;
                    starHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 3, 22, Math.sin(angle) * 3));
                }
            } else if (ticksAlive == 40 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(60 * i);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 3, 22, Math.sin(angle) * 3),
                        Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    starHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 1.0f);
            } else if (fired && ticksAlive - fireTick < 50) {
                float dist = (ticksAlive - fireTick) * 1.7f;
                for (int i = 0; i < 6; i++) {
                    int idx = 6 + i;
                    if (idx < starHandles.size()) {
                        double angle = Math.toRadians(60 * i);
                        starHandles.get(idx).entity().teleport(
                            center.clone().add(Math.cos(angle) * (3 + dist), 22, Math.sin(angle) * (3 + dist)));
                    }
                }
            } else if (fired && ticksAlive - fireTick == 50) {
                triggerImpactDamage(center.clone().add(0, 22, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 22, 0), 12, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SixPointStar(plugin); }
    }

    // ================================================================
    // 46. BRIMSTONE CHAIN — Skull that chains to 4 players sequentially
    // ================================================================
    public static class BrimstoneChain extends BossAttack {
        private final List<BlockDisplayHandle> chainHandles = new ArrayList<>();
        private boolean launched = false;
        private int launchTick = 0;
        private int chainCount = 0;

        public BrimstoneChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_chain", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle skull = displayBuilder.spawnBlock(
                center.clone().add(0.5, 20, 0), Material.SOUL_SAND);
            skull.scale(0.7f, 0.7f, 0.7f).glow(180, 80, 0).interpolation(3, 0);
            chainHandles.add(skull);
            spawnedEntities.add(skull.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 40 && !launched) {
                float pulse = 0.7f + (float) Math.sin(ticksAlive * 0.4) * 0.15f;
                if (!chainHandles.isEmpty()) {
                    chainHandles.get(0).entity().setTransformation(new Transformation(
                        new Vector3f(-pulse / 2, 20 - pulse / 2, -pulse / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(pulse, pulse, pulse),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0.5, 20, 0), 6, 0.8);
                }
            } else if (ticksAlive == 40 && !launched) {
                launched = true;
                launchTick = ticksAlive;
                chainCount = 0;
            } else if (launched) {
                int lTick = ticksAlive - launchTick;
                int chainPhase = lTick / 30; // 4 chains, 30 ticks each (1.5s travel)
                if (chainPhase > chainCount && chainCount < 4) {
                    chainCount = chainPhase;
                    DisplayBuilder.crimsonDust(center.clone().add(0, 10, chainCount * 15), 12, 2.0);
                    float pitch = 0.7f + chainCount * 0.05f;
                    DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.8f, 0.9f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, pitch);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f + chainCount * 0.05f);
                }
                if (chainPhase < 4) {
                    float dist = (lTick % 30) * 1.0f + chainPhase * 15;
                    float vel = 1.0f + chainPhase * 0.1f;
                    if (!chainHandles.isEmpty()) {
                        chainHandles.get(0).entity().teleport(
                            center.clone().add(0.5, 20 - dist * 0.1, dist * vel));
                    }
                }
                if (chainPhase >= 4 && lTick == 120) {
                    triggerImpactDamage(center.clone().add(0, 0.5, 60));
                    DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 60), 15, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneChain(plugin); }
    }

    // ================================================================
    // 47. TRIDENT CURTAIN CROSS — Two perpendicular 5-trident sweeps
    // ================================================================
    public static class TridentCurtainCross extends BossAttack {
        private final List<BlockDisplayHandle> crossHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public TridentCurtainCross(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_curtain_cross", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Perpendicular line telegraphs
            BlockDisplayHandle hLine = displayBuilder.spawnBlock(
                center.clone().add(0, 22, 0), Material.IRON_BLOCK);
            hLine.scale(20.0f, 0.08f, 0.15f).glow(200, 200, 255).interpolation(3, 0);
            crossHandles.add(hLine);
            spawnedEntities.add(hLine.entity());
            BlockDisplayHandle vLine = displayBuilder.spawnBlock(
                center.clone().add(0, 22, 0), Material.IRON_BLOCK);
            vLine.scale(0.15f, 0.08f, 20.0f).glow(200, 200, 255).interpolation(3, 0);
            crossHandles.add(vLine);
            spawnedEntities.add(vLine.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 40 && !fired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 22, 0), 12, 8.0);
                }
            } else if (ticksAlive == 40 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                // Horizontal sweep: 5 tridents
                for (int i = 0; i < 5; i++) {
                    double x = -8 + i * 4;
                    BlockDisplayHandle t = displayBuilder.spawnBlock(
                        center.clone().add(x, 22, -10), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    crossHandles.add(t);
                    spawnedEntities.add(t.entity());
                }
                // Vertical sweep: 5 tridents
                for (int i = 0; i < 5; i++) {
                    double z = -8 + i * 4;
                    BlockDisplayHandle t = displayBuilder.spawnBlock(
                        center.clone().add(-10, 22, z), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.12f, 0.12f, 1.0f).glow(255, 200, 200).interpolation(1, 0);
                    crossHandles.add(t);
                    spawnedEntities.add(t.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.2f);
            } else if (fired && ticksAlive - fireTick < 80) {
                int fTick = ticksAlive - fireTick;
                // Horizontal sweep left to right (staggered 8 ticks)
                for (int i = 0; i < 5; i++) {
                    int idx = 2 + i;
                    int delay = i * 8;
                    if (fTick > delay && idx < crossHandles.size()) {
                        float dist = (fTick - delay) * 1.5f;
                        double x = -8 + i * 4;
                        crossHandles.get(idx).entity().teleport(
                            center.clone().add(x, 22, -10 + dist));
                    }
                }
                // Vertical sweep far to near (staggered 8 ticks)
                for (int i = 0; i < 5; i++) {
                    int idx = 7 + i;
                    int delay = i * 8;
                    if (fTick > delay && idx < crossHandles.size()) {
                        float dist = (fTick - delay) * 1.5f;
                        double z = -8 + i * 4;
                        crossHandles.get(idx).entity().teleport(
                            center.clone().add(-10 + dist, 22, z));
                    }
                }
                if (fTick % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 22, 0), 8, 5.0);
                }
            } else if (fired && ticksAlive - fireTick == 80) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentCurtainCross(plugin); }
    }

    // ================================================================
    // 48. CONVERGENCE — One trident aimed at every player simultaneously
    // ================================================================
    public static class Convergence extends BossAttack {
        private final List<BlockDisplayHandle> convHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public Convergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_convergence", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eyes sweep — sequence marker
            BlockDisplayHandle eyeGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 22, 0), Material.GLOWSTONE);
            eyeGlow.scale(0.4f, 0.4f, 0.4f).glow(255, 255, 200).interpolation(3, 0);
            convHandles.add(eyeGlow);
            spawnedEntities.add(eyeGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.4f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 40 && !fired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 22, 0), 10, 3.0);
                }
            } else if (ticksAlive == 40 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                // Fire 4 tridents in different directions (simulating per-player targeting)
                for (int i = 0; i < 4; i++) {
                    double angle = Math.toRadians(90 * i + 45);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(
                        center.clone().add(0, 22, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.2f).glow(220, 220, 255).interpolation(1, 0);
                    convHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 1.0f);
            } else if (fired && ticksAlive - fireTick < 40) {
                float dist = (ticksAlive - fireTick) * 2.0f;
                for (int i = 0; i < 4; i++) {
                    int idx = 1 + i;
                    if (idx < convHandles.size()) {
                        double angle = Math.toRadians(90 * i + 45);
                        convHandles.get(idx).entity().teleport(
                            center.clone().add(Math.cos(angle) * dist, 22 - dist * 0.2,
                                Math.sin(angle) * dist));
                    }
                }
            } else if (fired && ticksAlive - fireTick == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 15, 6.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Convergence(plugin); }
    }

    // ================================================================
    // 49. PHASE THRESHOLD WARNING — Triple chain at 76% HP, cinematic combo
    // ================================================================
    public static class PhaseThresholdWarning extends BossAttack {
        private final List<BlockDisplayHandle> threshHandles = new ArrayList<>();
        private int phase = 0;
        private int phaseStartTick = 0;

        public PhaseThresholdWarning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_phase_threshold_warning", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(999999);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Massive crimson aura — 10 block radius
            BlockDisplayHandle aura = displayBuilder.spawnBlock(
                center.clone().add(0, 20, 0), Material.RED_STAINED_GLASS);
            aura.scale(10.0f, 10.0f, 10.0f).glow(180, 0, 0).interpolation(3, 0);
            threshHandles.add(aura);
            spawnedEntities.add(aura.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Phase 0: 3-second dramatic pause (0-60 ticks)
            if (ticksAlive < 60 && phase == 0) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 20, 10.0);
                }
            }
            // Phase 1: 8-trident radial burst (tick 60)
            else if (ticksAlive == 60 && phase == 0) {
                phase = 1;
                phaseStartTick = ticksAlive;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location spawnLoc = center.clone().add(Math.cos(angle) * 2, 22, Math.sin(angle) * 2);
                    BlockDisplayHandle t = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    threshHandles.add(t);
                    spawnedEntities.add(t.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 1.0f);
            }
            // Radial burst travel (60-100)
            else if (phase == 1 && ticksAlive - phaseStartTick < 40) {
                float dist = (ticksAlive - phaseStartTick) * 1.6f;
                for (int i = 1; i <= 8 && i < threshHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * (i - 1);
                    threshHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (2 + dist), 22, Math.sin(angle) * (2 + dist)));
                }
            }
            // Phase 2: 4 seeking skulls (tick 120)
            else if (ticksAlive == 120 && phase == 1) {
                phase = 2;
                phaseStartTick = ticksAlive;
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 / 4) * i;
                    Location spawnLoc = center.clone().add(Math.cos(angle) * 4, 20, Math.sin(angle) * 4);
                    BlockDisplayHandle skull = displayBuilder.spawnBlock(spawnLoc, Material.SOUL_SAND);
                    skull.scale(0.6f, 0.6f, 0.6f).glow(180, 80, 0).interpolation(1, 0);
                    threshHandles.add(skull);
                    spawnedEntities.add(skull.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.8f, 0.5f);
            }
            // Skulls seek (120-180)
            else if (phase == 2 && ticksAlive - phaseStartTick < 60) {
                float dist = (ticksAlive - phaseStartTick) * 1.2f;
                for (int i = 9; i <= 12 && i < threshHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 4) * (i - 9) + dist * 0.02;
                    threshHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (4 + dist), 20 - dist * 0.1, Math.sin(angle) * (4 + dist)));
                }
            }
            // Phase 3: Dive strafe (tick 200)
            else if (ticksAlive == 200 && phase == 2) {
                phase = 3;
                phaseStartTick = ticksAlive;
                BlockDisplayHandle diveModel = displayBuilder.spawnBlock(
                    center.clone().add(-12, 20, 0), Material.RED_STAINED_GLASS);
                diveModel.scale(2.0f, 3.0f, 2.0f).glow(180, 0, 0).interpolation(1, 0);
                threshHandles.add(diveModel);
                spawnedEntities.add(diveModel.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 1.2f);
            }
            // Dive strafe arc (200-250)
            else if (phase == 3 && ticksAlive - phaseStartTick < 50) {
                float progress = (ticksAlive - phaseStartTick) / 50.0f;
                double x = -12 + progress * 24;
                double y = 20 - Math.sin(progress * Math.PI) * 16;
                int lastIdx = threshHandles.size() - 1;
                if (lastIdx > 0) {
                    threshHandles.get(lastIdx).entity().teleport(center.clone().add(x, y, 0));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(x, y, 0), 10, 2.0);
                }
            }
            // Combo complete (tick 250)
            else if (phase == 3 && ticksAlive - phaseStartTick == 50) {
                triggerImpactDamage(center.clone().add(0, 2, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 20, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseThresholdWarning(plugin); }
    }

    // ================================================================
    // 50. FIRST SILENCE — Dual-element split: laser + triple trident
    // ================================================================
    public static class FirstSilence extends BossAttack {
        private final List<BlockDisplayHandle> silenceHandles = new ArrayList<>();
        private boolean active = false;
        private int activeStartTick = 0;

        public FirstSilence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_first_silence", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 1 second of stillness — no particles, no sound (the silence)
            // Then hands ignite
            BlockDisplayHandle leftHand = displayBuilder.spawnBlock(
                center.clone().add(-1.5, 23, 0), Material.IRON_BLOCK);
            leftHand.scale(0.01f, 0.01f, 0.01f).glow(200, 200, 255).interpolation(5, 0);
            silenceHandles.add(leftHand);
            spawnedEntities.add(leftHand.entity());

            BlockDisplayHandle rightHand = displayBuilder.spawnBlock(
                center.clone().add(1.5, 23, 0), Material.MAGMA_BLOCK);
            rightHand.scale(0.01f, 0.01f, 0.01f).glow(255, 80, 0).interpolation(5, 0);
            silenceHandles.add(rightHand);
            spawnedEntities.add(rightHand.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Silence — 20 ticks = 1 second of nothing
            if (ticksAlive < 20) {
                // Pure silence. No particles. No sound. This is the point.
            }
            // Hands ignite (tick 20-56 = 1.8 seconds telegraph)
            else if (ticksAlive >= 20 && ticksAlive < 56 && !active) {
                float progress = (ticksAlive - 20) / 36.0f;
                float scale = 0.01f + progress * 0.5f;
                if (silenceHandles.size() >= 2) {
                    silenceHandles.get(0).entity().setTransformation(new Transformation(
                        new Vector3f(-scale / 2, 23 - scale / 2, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)));
                    silenceHandles.get(1).entity().setTransformation(new Transformation(
                        new Vector3f(-scale / 2, 23 - scale / 2, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive == 20) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(-1.5, 23, 0), 4, 0.5);
                    DisplayBuilder.crimsonDust(center.clone().add(1.5, 23, 0), 4, 0.5);
                }
            }
            // Dual attack fires (tick 56)
            else if (ticksAlive == 56 && !active) {
                active = true;
                activeStartTick = ticksAlive;
                // Right hand: brimstone laser (8 segments)
                for (int seg = 0; seg < 8; seg++) {
                    Location segLoc = center.clone().add(1.5, 23, 2 + seg * 3);
                    BlockDisplayHandle beam = displayBuilder.spawnBlock(segLoc, Material.ORANGE_STAINED_GLASS);
                    beam.scale(0.5f, 0.5f, 3.0f).glow(255, 80, 0).interpolation(1, 0);
                    silenceHandles.add(beam);
                    spawnedEntities.add(beam.entity());
                }
                // Left hand: 3 rapid tridents
                for (int i = 0; i < 3; i++) {
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(
                        center.clone().add(-1.5, 23, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.2f).glow(220, 220, 255).interpolation(1, 0);
                    silenceHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.8f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.1f);
            }
            // Dual attack sustain (56-126 = 3.5 seconds)
            else if (active && ticksAlive - activeStartTick < 70) {
                int aTick = ticksAlive - activeStartTick;
                // Laser beam with slow tracking
                double rotAngle = (aTick < 40) ? Math.toRadians(aTick * 0.15) : Math.toRadians(6.0);
                for (int seg = 0; seg < 8; seg++) {
                    int idx = 2 + seg;
                    if (idx < silenceHandles.size()) {
                        double dist = 2 + seg * 3;
                        silenceHandles.get(idx).entity().teleport(
                            center.clone().add(1.5 + Math.sin(rotAngle) * dist, 23, Math.cos(rotAngle) * dist));
                    }
                }
                // Rapid tridents staggered
                for (int i = 0; i < 3; i++) {
                    int tIdx = 10 + i;
                    int tDelay = i * 8;
                    if (aTick > tDelay && tIdx < silenceHandles.size()) {
                        float dist = (aTick - tDelay) * 2.5f;
                        silenceHandles.get(tIdx).entity().teleport(
                            center.clone().add(-1.5, 23, dist));
                    }
                }
                if (aTick % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(1.5, 23, 12), 10, 2.0);
                    DisplayBuilder.crimsonDust(center.clone().add(-1.5, 23, aTick * 1.5f), 5, 0.5);
                }
            }
            // Combo ends (tick 126)
            else if (active && ticksAlive - activeStartTick == 70) {
                // Laser burn spot
                Location burnSpot = center.clone().add(2, 0.1, 26);
                BlockDisplayHandle zone = displayBuilder.spawnBlock(burnSpot, Material.MAGMA_BLOCK);
                zone.scale(4.0f, 0.1f, 4.0f).glow(255, 80, 0).interpolation(2, 0);
                spawnedEntities.add(zone.entity());
                triggerImpactDamage(burnSpot);
                DisplayBuilder.crimsonDust(burnSpot, 15, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FirstSilence(plugin); }
    }
}
