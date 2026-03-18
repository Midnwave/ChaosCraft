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
 * Phase 3 Boss (Dweller) — TIER 3 "THE WRATH" Attacks #51-60
 * HP range: 60%-40%. Maximum aggression. Screech-charges, brimstone rain,
 * shadow clones, gaze punishment, stomp chains, and the Tier 3 signature
 * arena-wide "Wrath Unleashed" at 40% HP threshold.
 * Damage range: 14-22 HP (7-11 hearts).
 * NO status effects — damage only.
 */
public final class WrathAssault {

    private WrathAssault() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShriekCharge(plugin));
        registry.register(new BrimstoneRain(plugin));
        registry.register(new ShadowTrio(plugin));
        registry.register(new GazeShatter(plugin));
        registry.register(new VoidStompChain(plugin));
        registry.register(new CorruptionEat(plugin));
        registry.register(new PressureWalk(plugin));
        registry.register(new LavaGeyserField(plugin));
        registry.register(new FaceToFace(plugin));
        registry.register(new WrathUnleashed(plugin));
    }

    // ================================================================
    // 51. SHRIEK CHARGE — Screaming sprint, audio + physical attack
    // ================================================================
    public static class ShriekCharge extends BossAttack {
        private final List<BlockDisplayHandle> trailHandles = new ArrayList<>();
        private boolean charging = false;
        private int chargeTick = 0;

        public ShriekCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_shriek_charge", AttackType.BOSS, 3), "dweller");
            config.setDamage(22.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Jaw opens — the instant IS the telegraph
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1, 0), Material.BLACKSTONE);
            body.scale(1.4f, 2.8f, 1.4f).glow(200, 0, 50).interpolation(2, 0);
            spawnedEntities.add(body.entity());
            // Jaw
            BlockDisplayHandle jaw = displayBuilder.spawnBlock(
                center.clone().add(0, 1.8, -0.6), Material.BLACKSTONE);
            jaw.scale(0.6f, 0.3f, 0.4f).glow(200, 0, 50).interpolation(2, 0);
            spawnedEntities.add(jaw.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Minimal telegraph — jaw opens, screech starts (0-10 ticks = 0.5s)
            if (ticksAlive < 10 && !charging) {
                if (ticksAlive == 5) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 1.5f);
                }
            }
            // CHARGE FIRES (tick 10 — Tier 3 reaction speed)
            else if (ticksAlive == 10 && !charging) {
                charging = true;
                chargeTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 1.2f);
            }
            // Screaming sprint (10-35 ticks)
            else if (charging && chargeTick < 25) {
                chargeTick++;
                float progress = chargeTick / 25.0f;
                float chargeZ = -progress * 18;
                // Screech cone particles from open jaw
                if (chargeTick % 3 == 0) {
                    for (int spread = -2; spread <= 2; spread++) {
                        DisplayBuilder.crimsonDust(
                            center.clone().add(spread * 0.8, 2.2, chargeZ - 1),
                            3, 0.4);
                    }
                    // Trail
                    Location trailLoc = center.clone().add(0, 0.8, chargeZ + 2);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.NETHERRACK);
                    trail.scale(0.3f, 0.2f, 0.3f).glow(200, 0, 50).interpolation(1, 0);
                    trailHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                }
                // Screech aura sound panning across
                if (chargeTick % 8 == 0) {
                    DisplayBuilder.playSound(center.clone().add(0, 1, chargeZ),
                        Sound.ENTITY_RAVAGER_ROAR, 1.2f, 1.5f);
                }
            }
            // Contact impact
            else if (charging && chargeTick == 25) {
                chargeTick++;
                Location impactLoc = center.clone().add(0, 0.5, -18);
                DisplayBuilder.crimsonDust(impactLoc, 25, 2.5);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShriekCharge(plugin); }
    }

    // ================================================================
    // 52. BRIMSTONE RAIN — 20 projectiles carpet-bombing the arena
    // ================================================================
    public static class BrimstoneRain extends BossAttack {
        private final List<BlockDisplayHandle> projectiles = new ArrayList<>();
        private boolean raining = false;
        private int rainTick = 0;

        public BrimstoneRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_brimstone_rain", AttackType.BOSS, 3), "dweller");
            config.setDamage(18.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(280);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Arms skyward
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.5, 0), Material.BLACKSTONE);
            body.scale(1.4f, 3.5f, 1.4f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Arms raise + dripping lava telegraph (0-30 ticks)
            if (ticksAlive < 30 && !raining) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 5, 0), 8, 2.0,
                        255, 100, 0, 1.0f);
                    // Arena-wide drip
                    for (int i = 0; i < 3; i++) {
                        double rx = (Math.random() - 0.5) * 24;
                        double rz = (Math.random() - 0.5) * 24;
                        DisplayBuilder.crimsonDust(
                            center.clone().add(rx, 0.2, rz), 2, 0.3);
                    }
                }
            }
            // RAIN BEGINS (tick 30)
            else if (ticksAlive == 30 && !raining) {
                raining = true;
                rainTick = 0;
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.5f);
            }
            // 20 projectiles over 120 ticks (6 seconds), one every 6 ticks
            else if (raining && rainTick < 120) {
                rainTick++;
                if (rainTick % 6 == 0) {
                    int projIndex = rainTick / 6;
                    // First 10: random spread. Last 10: cluster toward center
                    double targetX, targetZ;
                    if (projIndex <= 10) {
                        targetX = (Math.random() - 0.5) * 24;
                        targetZ = (Math.random() - 0.5) * 24;
                    } else {
                        targetX = (Math.random() - 0.5) * 12;
                        targetZ = (Math.random() - 0.5) * 12;
                    }
                    // Spawn projectile high and falling
                    Location projLoc = center.clone().add(targetX, 18, targetZ);
                    BlockDisplayHandle proj = displayBuilder.spawnBlock(projLoc, Material.MAGMA_BLOCK);
                    proj.scale(0.5f, 0.5f, 0.5f).glow(255, 100, 0).interpolation(1, 0);
                    projectiles.add(proj);
                    spawnedEntities.add(proj.entity());
                }
                // Animate falling projectiles
                for (int i = 0; i < projectiles.size(); i++) {
                    BlockDisplay proj = projectiles.get(i).entity();
                    Location loc = proj.getLocation();
                    if (loc.getY() > center.getY() + 0.5) {
                        proj.teleport(loc.clone().add(0, -0.8, 0));
                        if (rainTick % 4 == 0) {
                            DisplayBuilder.dustParticles(loc, 2, 0.3, 255, 100, 0, 0.6f);
                        }
                    } else if (loc.getY() > center.getY() - 0.5 && loc.getY() <= center.getY() + 0.5) {
                        // Ground impact
                        Location impactLoc = loc.clone();
                        impactLoc.setY(center.getY() + 0.1);
                        // Corruption zone at impact
                        BlockDisplayHandle zone = displayBuilder.spawnBlock(impactLoc, Material.MAGMA_BLOCK);
                        zone.scale(0.6f, 0.08f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                        spawnedEntities.add(zone.entity());
                        DisplayBuilder.crimsonDust(impactLoc, 8, 1.0);
                        if (i % 3 == 0) {
                            DisplayBuilder.playSound(impactLoc,
                                Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
                        }
                        proj.teleport(impactLoc.clone().add(0, -10, 0)); // Move off-screen
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneRain(plugin); }
    }

    // ================================================================
    // 53. SHADOW TRIO — Three copies attack three players simultaneously
    // ================================================================
    public static class ShadowTrio extends BossAttack {
        private boolean split = false;

        public ShadowTrio(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_shadow_trio", AttackType.BOSS, 3), "dweller");
            config.setDamage(22.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Original form
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Portal burst + triple teleport flash (0-20 ticks = 1s)
            if (ticksAlive < 20 && !split) {
                if (ticksAlive == 10) {
                    // Triple teleport sounds
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                    DisplayBuilder.playSound(center.clone().add(8, 0, -8),
                        Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                    DisplayBuilder.playSound(center.clone().add(-8, 0, -8),
                        Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, 0), 15, 1.0);
                }
            }
            // SPLIT into three (tick 20)
            else if (ticksAlive == 20 && !split) {
                split = true;
                // Clone 1 — left
                BlockDisplayHandle clone1 = displayBuilder.spawnBlock(
                    center.clone().add(-8, 1.2, -8), Material.BLACKSTONE);
                clone1.scale(1.0f, 2.5f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                spawnedEntities.add(clone1.entity());
                DisplayBuilder.darkPurpleDust(center.clone().add(-8, 1.5, -8), 10, 0.8);
                // Clone 2 — right
                BlockDisplayHandle clone2 = displayBuilder.spawnBlock(
                    center.clone().add(8, 1.2, -8), Material.BLACKSTONE);
                clone2.scale(1.0f, 2.5f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                spawnedEntities.add(clone2.entity());
                DisplayBuilder.darkPurpleDust(center.clone().add(8, 1.5, -8), 10, 0.8);
                // Real Dweller — center, identical appearance
                BlockDisplayHandle real = displayBuilder.spawnBlock(
                    center.clone().add(0, 1.2, -10), Material.BLACKSTONE);
                real.scale(1.0f, 2.5f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                spawnedEntities.add(real.entity());
                DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, -10), 10, 0.8);
            }
            // All three charge simultaneously (tick 35)
            else if (split && ticksAlive == 35) {
                // Charge trails from three directions
                // Left clone
                for (int i = 0; i < 4; i++) {
                    float t = i / 4.0f;
                    DisplayBuilder.darkPurpleDust(
                        center.clone().add(-8 + t * 8, 1, -8 + t * 3), 4, 0.4);
                }
                // Right clone
                for (int i = 0; i < 4; i++) {
                    float t = i / 4.0f;
                    DisplayBuilder.darkPurpleDust(
                        center.clone().add(8 - t * 8, 1, -8 + t * 3), 4, 0.4);
                }
                // Real Dweller — crimson trail (indistinguishable to players)
                for (int i = 0; i < 4; i++) {
                    float t = i / 4.0f;
                    DisplayBuilder.darkPurpleDust(
                        center.clone().add(0, 1, -10 + t * 5), 4, 0.4);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.8f);
            }
            // Clones dissolve, real hit registers (tick 45)
            else if (split && ticksAlive == 45) {
                // Clone dissolution
                DisplayBuilder.darkPurpleDust(center.clone().add(-4, 1.5, -5), 15, 1.5);
                DisplayBuilder.darkPurpleDust(center.clone().add(4, 1.5, -5), 15, 1.5);
                // Real impact
                DisplayBuilder.crimsonDust(center.clone().add(0, 1.5, -5), 20, 1.5);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowTrio(plugin); }
    }

    // ================================================================
    // 54. GAZE SHATTER — Punishes longest eye contact, shatters gaze link
    // ================================================================
    public static class GazeShatter extends BossAttack {
        private boolean shattered = false;

        public GazeShatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_gaze_shatter", AttackType.BOSS, 3), "dweller");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller eyes shifting to white-hot
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.POLISHED_BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Eyes shift to white-hot (0-30 ticks)
            if (ticksAlive < 30 && !shattered) {
                if (ticksAlive % 6 == 0) {
                    float intensity = ticksAlive / 30.0f;
                    int white = (int) (150 + intensity * 105);
                    DisplayBuilder.dustParticles(center.clone().add(0, 2.7, 0), 6, 0.3,
                        white, white, white, 1.0f + intensity * 0.5f);
                }
            }
            // SHATTER (tick 30) — gaze link explodes outward from target
            else if (ticksAlive == 30 && !shattered) {
                shattered = true;
                Location targetLoc = center.clone().add(0, 1.5, -12);
                // Glass-break explosion of particles from target's eyes
                for (int r = 1; r <= 6; r++) {
                    for (int i = 0; i < r * 4; i++) {
                        double a = (Math.PI * 2 / (r * 4)) * i;
                        DisplayBuilder.dustParticles(
                            targetLoc.clone().add(Math.cos(a) * r * 0.5, Math.sin(a) * r * 0.3, 0),
                            2, 0.2, 255, 255, 255, 1.0f);
                    }
                }
                // Beam shattering along gaze line
                for (int i = 0; i < 10; i++) {
                    Location beamLoc = center.clone().add(0, 2.7, -i * 1.2);
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(beamLoc, Material.ORANGE_STAINED_GLASS);
                    shard.scale(0.1f, 0.1f, 0.1f)
                        .rotate((float) (Math.random() * Math.PI), 1, 1, 0)
                        .glow(255, 255, 255).interpolation(1, 0);
                    spawnedEntities.add(shard.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.0f);
            }
            // White-then-black vision flash aftermath (30-50 ticks)
            else if (shattered && ticksAlive <= 50) {
                if (ticksAlive % 8 == 0) {
                    Location targetLoc = center.clone().add(0, 1.5, -12);
                    DisplayBuilder.darkPurpleDust(targetLoc, 6, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GazeShatter(plugin); }
    }

    // ================================================================
    // 55. VOID STOMP CHAIN — Three escalating concentric stomp waves
    // ================================================================
    public static class VoidStompChain extends BossAttack {
        private int stompPhase = 0;

        public VoidStompChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_void_stomp_chain", AttackType.BOSS, 3), "dweller");
            config.setDamage(20.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(12.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller in stomp stance
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 0), Material.BLACKSTONE);
            body.scale(1.5f, 3.0f, 1.5f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // STOMP 1 — 4-block radius (tick 10)
            if (ticksAlive == 10 && stompPhase == 0) {
                stompPhase = 1;
                for (int i = 0; i < 12; i++) {
                    double a = (Math.PI * 2 / 12) * i;
                    Location ringLoc = center.clone().add(Math.cos(a) * 4, 0.1, Math.sin(a) * 4);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                    ring.scale(0.7f, 0.12f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(ring.entity());
                }
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 20, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                triggerImpactDamage(center);
            }
            // STOMP 2 — 8-block radius (tick 40 — 1.5s after first)
            else if (ticksAlive == 40 && stompPhase == 1) {
                stompPhase = 2;
                for (int i = 0; i < 20; i++) {
                    double a = (Math.PI * 2 / 20) * i;
                    Location ringLoc = center.clone().add(Math.cos(a) * 8, 0.1, Math.sin(a) * 8);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                    ring.scale(0.7f, 0.12f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(ring.entity());
                }
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 25, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.45f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.4f);
            }
            // STOMP 3 — 12-block radius (tick 60 — 1s after second)
            else if (ticksAlive == 60 && stompPhase == 2) {
                stompPhase = 3;
                for (int i = 0; i < 28; i++) {
                    double a = (Math.PI * 2 / 28) * i;
                    Location ringLoc = center.clone().add(Math.cos(a) * 12, 0.1, Math.sin(a) * 12);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                    ring.scale(0.7f, 0.12f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(ring.entity());
                }
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 30, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.5f);
            }
            // Expanding wave particles between stomps
            else if (stompPhase >= 1 && stompPhase < 3) {
                if (ticksAlive % 5 == 0) {
                    float waveR = stompPhase == 1 ? 4 + ((ticksAlive - 10) / 30.0f) * 4 :
                        8 + ((ticksAlive - 40) / 20.0f) * 4;
                    for (int i = 0; i < 8; i++) {
                        double a = (Math.PI * 2 / 8) * i;
                        DisplayBuilder.crimsonDust(
                            center.clone().add(Math.cos(a) * waveR, 0.3, Math.sin(a) * waveR),
                            2, 0.3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidStompChain(plugin); }
    }

    // ================================================================
    // 56. CORRUPTION EAT — Dweller absorbs corruption zones, heals, re-expands
    // ================================================================
    public static class CorruptionEat extends BossAttack {
        private boolean absorbing = false;
        private boolean reExpanded = false;

        public CorruptionEat(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_corruption_eat", AttackType.BOSS, 3), "dweller");
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller approaches corruption cluster
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(5, 1, 5), Material.POLISHED_BLACKSTONE);
            body.scale(1.2f, 2.8f, 1.2f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(body.entity());
            // Corruption cluster
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle zone = displayBuilder.spawnBlock(
                        center.clone().add(5 + x, 0.1, 5 + z), Material.MAGMA_BLOCK);
                    zone.scale(0.9f, 0.1f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(zone.entity());
                }
            }
            DisplayBuilder.playSound(center.clone().add(5, 0, 5),
                Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location clusterLoc = center.clone().add(5, 0, 5);

            // Kneeling approach (0-30 ticks)
            if (ticksAlive < 30 && !absorbing) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(clusterLoc.clone().add(0, 0.5, 0), 5, 1.0);
                }
            }
            // ABSORPTION (tick 30)
            else if (ticksAlive == 30 && !absorbing) {
                absorbing = true;
                // Energy flowing from tiles into Dweller body
                DisplayBuilder.playSound(clusterLoc, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.3f);
            }
            // Absorption animation (30-70 ticks)
            else if (absorbing && !reExpanded && ticksAlive < 70) {
                if (ticksAlive % 6 == 0) {
                    float absorbProgress = (ticksAlive - 30) / 40.0f;
                    // Particles flowing upward from zone into body
                    for (int i = 0; i < 4; i++) {
                        double ox = (Math.random() - 0.5) * 2;
                        double oz = (Math.random() - 0.5) * 2;
                        float py = absorbProgress * 2;
                        DisplayBuilder.crimsonDust(
                            clusterLoc.clone().add(ox, 0.2 + py, oz), 3, 0.3);
                    }
                }
                // Cracks flare violently
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.dustParticles(clusterLoc.clone().add(0, 2, 0), 8, 0.5,
                        255, 100, 0, 1.2f);
                }
            }
            // RE-EXPANSION — zone doubles (tick 70)
            else if (ticksAlive == 70 && !reExpanded) {
                reExpanded = true;
                // Doubled corruption zone
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        Location expandLoc = clusterLoc.clone().add(x, 0.08, z);
                        BlockDisplayHandle expand = displayBuilder.spawnBlock(expandLoc, Material.MAGMA_BLOCK);
                        expand.scale(0.9f, 0.08f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                        spawnedEntities.add(expand.entity());
                    }
                }
                DisplayBuilder.crimsonDust(clusterLoc.clone().add(0, 0.5, 0), 25, 3.0);
                DisplayBuilder.playSound(clusterLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                // Dweller healing pulse
                DisplayBuilder.dustParticles(clusterLoc.clone().add(0, 2.5, 0), 15, 1.0,
                    255, 100, 0, 1.5f);
                DisplayBuilder.playSound(clusterLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionEat(plugin); }
    }

    // ================================================================
    // 57. PRESSURE WALK — Slow unstoppable advance, psychological horror
    // ================================================================
    public static class PressureWalk extends BossAttack {
        private int walkTick = 0;

        public PressureWalk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_pressure_walk", AttackType.BOSS, 3), "dweller");
            config.setDamage(20.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller at starting position — no warning, walk starts
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.2, 10), Material.BLACKSTONE);
            body.scale(1.4f, 3.2f, 1.4f).glow(200, 0, 50).interpolation(5, 0);
            spawnedEntities.add(body.entity());
            // Eyes — unblinking stare
            BlockDisplayHandle eyes = displayBuilder.spawnBlock(
                center.clone().add(0, 2.8, 9.4), Material.ORANGE_STAINED_GLASS);
            eyes.scale(0.3f, 0.1f, 0.05f).glow(0, 150, 255).interpolation(2, 0);
            spawnedEntities.add(eyes.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            walkTick++;
            // Slow relentless walk (entire duration)
            float walkZ = 10 - (walkTick / 160.0f) * 14;

            // Purple bleed particles continuously
            if (walkTick % 8 == 0) {
                DisplayBuilder.darkPurpleDust(
                    center.clone().add(0, 1.5, walkZ), 5, 0.8);
            }
            // Footstep sounds — deliberate, heavy
            if (walkTick % 25 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 0, walkZ),
                    Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.3f);
            }
            // Eye glow — constant unblinking track
            if (walkTick % 15 == 0) {
                DisplayBuilder.dustParticles(
                    center.clone().add(0, 2.8, walkZ - 0.6), 3, 0.1,
                    0, 150, 255, 0.6f);
            }
            // Corruption trail behind
            if (walkTick % 20 == 0) {
                Location trailLoc = center.clone().add(0, 0.05, walkZ + 1);
                BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.SOUL_SOIL);
                trail.scale(0.6f, 0.05f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(trail.entity());
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PressureWalk(plugin); }
    }

    // ================================================================
    // 58. LAVA GEYSER FIELD — Six simultaneous geyser eruptions
    // ================================================================
    public static class LavaGeyserField extends BossAttack {
        private final List<BlockDisplayHandle> geyserHandles = new ArrayList<>();
        private boolean erupted = false;

        public LavaGeyserField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_lava_geyser_field", AttackType.BOSS, 3), "dweller");
            config.setDamage(20.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(20.0);
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Six target zone tiles pulsing
            double[][] positions = {{3, -5}, {-4, -3}, {6, 2}, {-2, 6}, {5, 5}, {-6, -6}};
            for (double[] pos : positions) {
                Location tileLoc = center.clone().add(pos[0], 0.1, pos[1]);
                BlockDisplayHandle tile = displayBuilder.spawnBlock(tileLoc, Material.MAGMA_BLOCK);
                tile.scale(0.8f, 0.1f, 0.8f).glow(200, 0, 50).interpolation(3, 0);
                spawnedEntities.add(tile.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            double[][] positions = {{3, -5}, {-4, -3}, {6, 2}, {-2, 6}, {5, 5}, {-6, -6}};

            // Rapid pulsing telegraph (0-30 ticks)
            if (ticksAlive < 30 && !erupted) {
                if (ticksAlive % 4 == 0) {
                    for (double[] pos : positions) {
                        DisplayBuilder.crimsonDust(
                            center.clone().add(pos[0], 0.3, pos[1]), 3, 0.3);
                    }
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.5f,
                        0.8f + (ticksAlive / 30.0f) * 0.6f);
                }
            }
            // SIX SIMULTANEOUS GEYSERS (tick 30)
            else if (ticksAlive == 30 && !erupted) {
                erupted = true;
                for (double[] pos : positions) {
                    Location geyserBase = center.clone().add(pos[0], 0, pos[1]);
                    // 6-block tall column
                    for (int y = 0; y < 6; y++) {
                        Location colLoc = geyserBase.clone().add(0, y, 0);
                        BlockDisplayHandle col = displayBuilder.spawnBlock(colLoc, Material.MAGMA_BLOCK);
                        float diameter = 0.8f - y * 0.08f;
                        col.scale(diameter, 0.9f, diameter).glow(255, 100, 0).interpolation(2, 0);
                        geyserHandles.add(col);
                        spawnedEntities.add(col.entity());
                    }
                    DisplayBuilder.crimsonDust(geyserBase.clone().add(0, 3, 0), 15, 1.5);
                    DisplayBuilder.dustParticles(geyserBase.clone().add(0, 5, 0), 10, 1.0,
                        255, 100, 0, 1.2f);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.3f);
                triggerImpactDamage(center);
            }
            // Geyser persistence with rain-down phase (30-90 ticks = 3 seconds)
            else if (erupted && ticksAlive < 90) {
                if (ticksAlive % 8 == 0) {
                    for (double[] pos : positions) {
                        DisplayBuilder.dustParticles(
                            center.clone().add(pos[0], 5, pos[1]), 4, 0.8,
                            255, 100, 0, 0.8f);
                    }
                }
            }
            // Rain-down phase (90-130 ticks)
            else if (erupted && ticksAlive >= 90 && ticksAlive < 130) {
                if (ticksAlive % 6 == 0) {
                    for (double[] pos : positions) {
                        double rx = (Math.random() - 0.5) * 2;
                        double rz = (Math.random() - 0.5) * 2;
                        DisplayBuilder.crimsonDust(
                            center.clone().add(pos[0] + rx, 4 - (ticksAlive - 90) * 0.08, pos[1] + rz),
                            3, 0.3);
                    }
                }
            }
            // Super-corrupted tiles after collapse (tick 130)
            else if (erupted && ticksAlive == 130) {
                for (double[] pos : positions) {
                    Location superLoc = center.clone().add(pos[0], 0.12, pos[1]);
                    BlockDisplayHandle superZone = displayBuilder.spawnBlock(superLoc, Material.NETHERRACK);
                    superZone.scale(1.2f, 0.15f, 1.2f).glow(255, 100, 0).interpolation(2, 0);
                    spawnedEntities.add(superZone.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LavaGeyserField(plugin); }
    }

    // ================================================================
    // 59. FACE TO FACE — Teleport in front of target, 3s stare, roar
    // ================================================================
    public static class FaceToFace extends BossAttack {
        private boolean appeared = false;
        private boolean roared = false;

        public FaceToFace(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_face_to_face", AttackType.BOSS, 3), "dweller");
            config.setDamage(22.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Teleport directly in front of Marked player
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Instant appearance 1 block in front (tick 5)
            if (ticksAlive == 5 && !appeared) {
                appeared = true;
                // Dweller face — close, filling view
                BlockDisplayHandle face = displayBuilder.spawnBlock(
                    center.clone().add(0, 1.5, -1), Material.POLISHED_BLACKSTONE);
                face.scale(1.2f, 1.5f, 0.3f).glow(200, 0, 50).interpolation(3, 0);
                spawnedEntities.add(face.entity());
                // Glowing eyes at close range
                BlockDisplayHandle leftEye = displayBuilder.spawnBlock(
                    center.clone().add(-0.25, 2.2, -1.2), Material.ORANGE_STAINED_GLASS);
                leftEye.scale(0.2f, 0.15f, 0.05f).glow(0, 150, 255).interpolation(2, 0);
                spawnedEntities.add(leftEye.entity());
                BlockDisplayHandle rightEye = displayBuilder.spawnBlock(
                    center.clone().add(0.25, 2.2, -1.2), Material.ORANGE_STAINED_GLASS);
                rightEye.scale(0.2f, 0.15f, 0.05f).glow(0, 150, 255).interpolation(2, 0);
                spawnedEntities.add(rightEye.entity());
                DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, -1), 10, 0.5);
            }
            // 3-second stare (5-65 ticks)
            else if (appeared && !roared && ticksAlive < 65) {
                // Particles bleeding from space between eyes
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.darkPurpleDust(
                        center.clone().add(0, 2, -1.1), 4, 0.2);
                    DisplayBuilder.dustParticles(
                        center.clone().add(0, 2.2, -1.2), 3, 0.1,
                        0, 150, 255, 0.6f);
                }
                // Ambient subsonic hum
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center,
                        Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.1f);
                }
            }
            // ROAR (tick 65) — lean forward and devastating roar
            else if (ticksAlive == 65 && !roared) {
                roared = true;
                // Full-face roar blast
                DisplayBuilder.crimsonDust(center.clone().add(0, 2, -1), 25, 2.0);
                DisplayBuilder.dustParticles(center.clone().add(0, 2, -1), 15, 1.5,
                    255, 100, 0, 1.5f);
                // Shockwave cone in front of face
                for (int i = 1; i <= 4; i++) {
                    for (int s = -2; s <= 2; s++) {
                        DisplayBuilder.crimsonDust(
                            center.clone().add(s * 0.5, 2, -1 - i), 3, 0.3);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FaceToFace(plugin); }
    }

    // ================================================================
    // 60. WRATH UNLEASHED — Tier 3 signature, 40% HP threshold transition
    // ================================================================
    public static class WrathUnleashed extends BossAttack {
        private boolean unleashed = false;

        public WrathUnleashed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dweller_wrath_unleashed", AttackType.BOSS, 3), "dweller");
            config.setDamage(18.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(0); // Used once at 40% HP threshold
            config.setTicksBetweenDamage(200); // Single hit
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller stops at center, all cracks blazing to white-orange
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 1.5, 0), Material.BLACKSTONE);
            body.scale(1.8f, 3.5f, 1.8f).glow(255, 100, 0).interpolation(5, 0);
            spawnedEntities.add(body.entity());
            // Full-body glow buildup
            DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 15, 1.0,
                255, 100, 0, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 2-second full-body glow buildup (0-40 ticks)
            if (ticksAlive < 40 && !unleashed) {
                float intensity = ticksAlive / 40.0f;
                if (ticksAlive % 4 == 0) {
                    int white = (int) (100 + intensity * 155);
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0),
                        (int) (8 + intensity * 20), 1.5,
                        white, (int) (100 * (1 - intensity * 0.5)), 0, 1.5f);
                    DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0),
                        (int) (5 + intensity * 10), 2.0);
                }
                // Building layered roar
                if (ticksAlive == 20) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.2f);
                }
                if (ticksAlive == 35) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.5f);
                }
            }
            // WRATH UNLEASHED (tick 40) — sustained arena-filling roar
            else if (ticksAlive == 40 && !unleashed) {
                unleashed = true;
                // Arena-wide explosion burst from body
                for (int r = 2; r <= 20; r += 3) {
                    for (int i = 0; i < r * 2; i++) {
                        double a = (Math.PI * 2 / (r * 2)) * i;
                        DisplayBuilder.crimsonDust(
                            center.clone().add(Math.cos(a) * r, 0.5, Math.sin(a) * r), 2, 0.3);
                    }
                }
                // Corruption zone eruption — all existing zones pulse
                for (int ring = 3; ring <= 15; ring += 3) {
                    for (int i = 0; i < ring * 2; i++) {
                        double a = (Math.PI * 2 / (ring * 2)) * i;
                        Location zoneLoc = center.clone().add(
                            Math.cos(a) * ring, 0.1, Math.sin(a) * ring);
                        BlockDisplayHandle zone = displayBuilder.spawnBlock(zoneLoc, Material.MAGMA_BLOCK);
                        zone.scale(0.7f, 0.12f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                        spawnedEntities.add(zone.entity());
                    }
                }
                // Maximum particle saturation
                DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 50, 15.0);
                DisplayBuilder.dustParticles(center.clone().add(0, 3, 0), 40, 10.0,
                    255, 100, 0, 2.0f);
                DisplayBuilder.darkPurpleDust(center.clone().add(0, 1, 0), 30, 12.0);
                // Layered roar — multiple sounds for massive presence
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.1f);
            }
            // Arena darkening + sustained effect (40-80 ticks = 2 second roar)
            else if (unleashed && ticksAlive > 40 && ticksAlive <= 80) {
                if (ticksAlive % 6 == 0) {
                    // Expanding rings of corruption
                    float ring = (ticksAlive - 40) / 40.0f * 20;
                    for (int i = 0; i < 12; i++) {
                        double a = (Math.PI * 2 / 12) * i;
                        DisplayBuilder.crimsonDust(
                            center.clone().add(Math.cos(a) * ring, 0.5, Math.sin(a) * ring),
                            4, 0.5);
                    }
                }
            }
            // Speed increase indicator — brimstone flare (tick 80)
            else if (ticksAlive == 80) {
                DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 20, 1.0,
                    255, 200, 0, 1.8f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.2f);
            }
            // Teleport to Marked player and chain to Infernal Stomp (tick 90)
            else if (ticksAlive == 90) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
                DisplayBuilder.crimsonDust(center.clone().add(0, 1.5, -8), 15, 1.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WrathUnleashed(plugin); }
    }
}
