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
 * Phase 4D Boss — THE VOID EMPEROR (Ender Dragon)
 * Phase 3A: Attacks #85-100
 * "The Emperor Ascendant" — HP 29% and below.
 * Absorbed essences now CONTROLLED, not bleeding. Telegraphs: 0.2-0.5 seconds.
 * Environmental hazards run on independent timers. Arena permanently shrinks.
 * Phase 3 damage range: 18-36 HP.
 * NO status effects — damage only.
 */
public final class DragonPhase3A {

    private DragonPhase3A() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new Phase3InitiationRoar(plugin));
        registry.register(new FractureDive(plugin));
        registry.register(new VoidScreamPulse(plugin));
        registry.register(new CrystalShardSalvo(plugin));
        registry.register(new BrimstoneWake(plugin));
        registry.register(new SonicCharge(plugin));
        registry.register(new RiftClaw(plugin));
        registry.register(new StreamConvergenceSpear(plugin));
        registry.register(new WitherBarrage(plugin));
        registry.register(new CascadeChain(plugin));
        registry.register(new FloorCollapseAbyss(plugin));
        registry.register(new LeakingVoidSurge(plugin));
        registry.register(new TailSweepArenaWidth(plugin));
        registry.register(new VoidEmperorsGaze(plugin));
        registry.register(new CrystalCageDrop(plugin));
        registry.register(new VoidEmperorsGambit(plugin));
    }

    // ================================================================
    // #85 — PHASE 3 INITIATION ROAR — Announcement, no damage
    // ================================================================
    public static class Phase3InitiationRoar extends BossAttack {
        private final List<BlockDisplayHandle> roarHandles = new ArrayList<>();
        private boolean roarFired = false;

        public Phase3InitiationRoar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_phase3_initiation_roar", AttackType.BOSS, 4), "dragon");
            config.setDamage(0.0);
            config.setDamageRadius(40.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 0.4 seconds silence (0-8 ticks)
            // Roar fires (tick 8)
            if (ticksAlive == 8 && !roarFired) {
                roarFired = true;
                // Explosion ring around Dragon
                for (int i = 0; i < 24; i++) {
                    double angle = (Math.PI * 2 / 24) * i;
                    Location ringPt = center.clone().add(Math.cos(angle) * 8, 10, Math.sin(angle) * 8);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.WHITE_STAINED_GLASS);
                    ring.scale(1.5f, 1.5f, 1.5f).glow(255, 255, 255).interpolation(1, 0);
                    roarHandles.add(ring);
                    spawnedEntities.add(ring.entity());
                }
                // Eye bursts — white END_ROD visual
                BlockDisplayHandle leftEye = displayBuilder.spawnBlock(
                    center.clone().add(-1, 12, 0), Material.GLOWSTONE);
                leftEye.scale(0.5f, 0.5f, 0.5f).glow(255, 255, 255).interpolation(2, 0);
                roarHandles.add(leftEye);
                spawnedEntities.add(leftEye.entity());
                BlockDisplayHandle rightEye = displayBuilder.spawnBlock(
                    center.clone().add(1, 12, 0), Material.GLOWSTONE);
                rightEye.scale(0.5f, 0.5f, 0.5f).glow(255, 255, 255).interpolation(2, 0);
                roarHandles.add(rightEye);
                spawnedEntities.add(rightEye.entity());
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 40, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 1.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.4f);
            }
            // Screen shake equivalent (8-68 ticks = 3 seconds)
            if (roarFired && ticksAlive < 68 && ticksAlive % 6 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 8, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Phase3InitiationRoar(plugin); }
    }

    // ================================================================
    // #86 — FRACTURE DIVE — Ballistic straight-line charge + void crater
    // ================================================================
    public static class FractureDive extends BossAttack {
        private final List<BlockDisplayHandle> diveHandles = new ArrayList<>();
        private boolean diveLanded = false;

        public FractureDive(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_fracture_dive", AttackType.BOSS, 4), "dragon");
            config.setDamage(28.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(180);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Reverse-portal cone telegraph (0.4 second)
            BlockDisplayHandle cone = displayBuilder.spawnBlock(
                center.clone().add(0, 12, 2), Material.PURPLE_STAINED_GLASS);
            cone.scale(2.0f, 2.0f, 4.0f).glow(120, 0, 200).interpolation(2, 0);
            diveHandles.add(cone);
            spawnedEntities.add(cone.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.5f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Very short telegraph (0-8 ticks = 0.4 seconds)
            if (ticksAlive < 8 && !diveLanded) {
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 12, 2), 6, 2.0);
                }
            }
            // Dive impact (tick 8)
            else if (ticksAlive == 8 && !diveLanded) {
                diveLanded = true;
                double ox = (Math.random() - 0.5) * 20;
                double oz = (Math.random() - 0.5) * 20;
                Location impactLoc = center.clone().add(ox, 0.1, oz);
                // Void crater
                BlockDisplayHandle crater = displayBuilder.spawnBlock(impactLoc, Material.CRYING_OBSIDIAN);
                crater.scale(5.0f, 0.5f, 5.0f).glow(80, 0, 140).interpolation(2, 0);
                diveHandles.add(crater);
                spawnedEntities.add(crater.entity());
                triggerImpactDamage(impactLoc);
                DisplayBuilder.cyanDust(impactLoc, 30, 6.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.7f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_STONE_PLACE, 1.5f, 0.9f);
            }
            // Crater persists with smoke (8-408 ticks = 20 seconds)
            if (diveLanded && ticksAlive < 408 && ticksAlive % 20 == 0) {
                if (diveHandles.size() > 1) {
                    DisplayBuilder.cyanDust(diveHandles.get(1).entity().getLocation(), 4, 5.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FractureDive(plugin); }
    }

    // ================================================================
    // #87 — VOID SCREAM PULSE — Omnidirectional radial ring
    // ================================================================
    public static class VoidScreamPulse extends BossAttack {
        private final List<BlockDisplayHandle> pulseHandles = new ArrayList<>();
        private boolean pulseFired = false;

        public VoidScreamPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_scream_pulse", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(40.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Purple-black sphere at Dragon's jaw
            BlockDisplayHandle jawSphere = displayBuilder.spawnBlock(
                center.clone().add(0, 10, 1), Material.PURPLE_STAINED_GLASS);
            jawSphere.scale(1.5f, 1.5f, 1.5f).glow(120, 0, 180).interpolation(2, 0);
            pulseHandles.add(jawSphere);
            spawnedEntities.add(jawSphere.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge (0-6 ticks = 0.3 seconds)
            if (ticksAlive < 6 && !pulseFired) {
                float grow = 1.5f + ticksAlive * 0.3f;
                pulseHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-grow / 2, 10 - grow / 2, 1 - grow / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(grow, grow, grow),
                    new AxisAngle4f(0, 0, 1, 0)));
            }
            // Pulse fires (tick 6)
            else if (ticksAlive == 6 && !pulseFired) {
                pulseFired = true;
                // Expanding shockwave ring
                for (int i = 0; i < 20; i++) {
                    double angle = (Math.PI * 2 / 20) * i;
                    Location ringPt = center.clone().add(Math.cos(angle) * 2, 10, Math.sin(angle) * 2);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.PURPLE_STAINED_GLASS);
                    ring.scale(2.0f, 3.0f, 0.5f).glow(120, 0, 180).interpolation(2, 0);
                    pulseHandles.add(ring);
                    spawnedEntities.add(ring.entity());
                }
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.6f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 2.0f, 0.4f);
            }
            // Ring expands to arena edge (6-60 ticks)
            else if (pulseFired && ticksAlive < 60) {
                float dist = (ticksAlive - 6) * 0.8f;
                for (int i = 1; i < pulseHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 20) * (i - 1);
                    pulseHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (2 + dist), 10, Math.sin(angle) * (2 + dist)));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidScreamPulse(plugin); }
    }

    // ================================================================
    // #88 — CRYSTAL SHARD SALVO — 36 DoG crystal shards, predictive targeting
    // ================================================================
    public static class CrystalShardSalvo extends BossAttack {
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private boolean shardsFired = false;
        private int fireTick = 0;

        public CrystalShardSalvo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_crystal_shard_salvo", AttackType.BOSS, 4), "dragon");
            config.setDamage(22.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 12 crystal formations ring at 4-block radius
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 / 12) * i;
                Location crystLoc = center.clone().add(Math.cos(angle) * 4, 10, Math.sin(angle) * 4);
                BlockDisplayHandle cryst = displayBuilder.spawnBlock(crystLoc, Material.AMETHYST_BLOCK);
                cryst.scale(0.6f, 0.6f, 0.6f).glow(0, 220, 255).interpolation(2, 0);
                shardHandles.add(cryst);
                spawnedEntities.add(cryst.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Brief telegraph (0-8 ticks)
            if (ticksAlive < 8 && !shardsFired) {
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 8, 4.0);
                }
            }
            // All shatter — 36 shards (3 per crystal) fire (tick 8)
            else if (ticksAlive == 8 && !shardsFired) {
                shardsFired = true;
                fireTick = ticksAlive;
                for (int c = 0; c < 12; c++) {
                    for (int s = 0; s < 3; s++) {
                        double angle = (Math.PI * 2 / 12) * c + (s - 1) * 0.15;
                        Location shardLoc = center.clone().add(Math.cos(angle) * 5, 10, Math.sin(angle) * 5);
                        BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.AMETHYST_BLOCK);
                        shard.scale(0.15f, 0.15f, 0.4f).glow(0, 220, 255).interpolation(1, 0);
                        shardHandles.add(shard);
                        spawnedEntities.add(shard.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 2.0f, 1.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 1.8f, 1.6f);
            }
            // Shards travel outward at high velocity (8-58 ticks)
            else if (shardsFired && ticksAlive - fireTick < 50) {
                float dist = (ticksAlive - fireTick) * 0.8f;
                for (int i = 12; i < shardHandles.size(); i++) {
                    int localIdx = i - 12;
                    int crystIdx = localIdx / 3;
                    int shardIdx = localIdx % 3;
                    double angle = (Math.PI * 2 / 12) * crystIdx + (shardIdx - 1) * 0.15;
                    shardHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (5 + dist), 10 - dist * 0.15, Math.sin(angle) * (5 + dist)));
                }
            }
            // Shard despawn (tick 58)
            else if (shardsFired && ticksAlive - fireTick == 50) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalShardSalvo(plugin); }
    }

    // ================================================================
    // #89 — BRIMSTONE WAKE — Dragon leaves fire trail for 8 seconds
    // ================================================================
    public static class BrimstoneWake extends BossAttack {
        private final List<BlockDisplayHandle> wakeHandles = new ArrayList<>();
        private boolean wakeActive = false;
        private int wakeTick = 0;

        public BrimstoneWake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_brimstone_wake", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wing ignition — instant (no telegraph)
            wakeActive = true;
            wakeTick = 0;
            DisplayBuilder.playSound(center, Sound.ITEM_FIRECHARGE_USE, 2.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fire trail deposited along flight path (0-160 ticks = 8 seconds)
            if (wakeActive && ticksAlive < 160) {
                double pathAngle = ticksAlive * 0.04;
                double radius = 15 + Math.sin(ticksAlive * 0.02) * 5;
                Location trailPt = center.clone().add(
                    Math.cos(pathAngle) * radius, 0.15, Math.sin(pathAngle) * radius);

                if (ticksAlive % 3 == 0) {
                    BlockDisplayHandle firePatch = displayBuilder.spawnBlock(trailPt, Material.MAGMA_BLOCK);
                    firePatch.scale(3.0f, 0.15f, 3.0f).glow(180, 0, 120).interpolation(2, 0);
                    wakeHandles.add(firePatch);
                    spawnedEntities.add(firePatch.entity());
                    triggerImpactDamage(trailPt);
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(trailPt, 4, 2.0);
                    DisplayBuilder.playSound(trailPt, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.7f);
                }
            }
            // Fire patches extinguish gradually (160-260 ticks = 5 seconds)
            else if (ticksAlive >= 160 && ticksAlive < 260) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 4, 15.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneWake(plugin); }
    }

    // ================================================================
    // #90 — SONIC CHARGE — 500% speed, 3 consecutive charges with afterimages
    // ================================================================
    public static class SonicCharge extends BossAttack {
        private final List<BlockDisplayHandle> chargeHandles = new ArrayList<>();
        private int chargeCount = 0;

        public SonicCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_sonic_charge", AttackType.BOSS, 4), "dragon");
            config.setDamage(32.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Body flash white (0.2 second telegraph)
            BlockDisplayHandle flash = displayBuilder.spawnBlock(
                center.clone().add(0, 10, 0), Material.WHITE_STAINED_GLASS);
            flash.scale(6.0f, 6.0f, 6.0f).glow(255, 255, 255).interpolation(1, 0);
            chargeHandles.add(flash);
            spawnedEntities.add(flash.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.5f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 3 charges at ticks 4, 20, 36
            int[] chargeTicks = {4, 20, 36};
            double[][] chargeVectors = {{1, 0}, {-0.7, 0.7}, {0, -1}};

            for (int c = 0; c < 3; c++) {
                if (ticksAlive == chargeTicks[c] && chargeCount <= c) {
                    chargeCount = c + 1;
                    // Afterimage trail — 8 positions along charge line
                    for (int img = 0; img < 8; img++) {
                        double dist = img * 4;
                        Location imgLoc = center.clone().add(
                            chargeVectors[c][0] * dist, 10, chargeVectors[c][1] * dist);
                        BlockDisplayHandle afterimage = displayBuilder.spawnBlock(imgLoc, Material.WHITE_STAINED_GLASS);
                        afterimage.scale(3.0f, 4.0f, 3.0f).glow(200, 200, 220).interpolation(1, 0);
                        chargeHandles.add(afterimage);
                        spawnedEntities.add(afterimage.entity());
                    }
                    Location endLoc = center.clone().add(
                        chargeVectors[c][0] * 30, 0.5, chargeVectors[c][1] * 30);
                    triggerImpactDamage(endLoc);
                    DisplayBuilder.cyanDust(endLoc, 15, 3.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 3.0f, 2.0f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SonicCharge(plugin); }
    }

    // ================================================================
    // #91 — RIFT CLAW — Double foreclaw swipe leaving rift scars
    // ================================================================
    public static class RiftClaw extends BossAttack {
        private final List<BlockDisplayHandle> clawHandles = new ArrayList<>();
        private int swipeCount = 0;

        public RiftClaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_rift_claw", AttackType.BOSS, 4), "dragon");
            config.setDamage(30.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Claw void arc telegraph
            BlockDisplayHandle clawArc = displayBuilder.spawnBlock(
                center.clone().add(2, 8, 0), Material.PURPLE_STAINED_GLASS);
            clawArc.scale(1.0f, 0.3f, 2.0f).glow(100, 0, 200).interpolation(2, 0);
            clawHandles.add(clawArc);
            spawnedEntities.add(clawArc.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Double swipe: tick 6 (left), tick 12 (right)
            if ((ticksAlive == 6 || ticksAlive == 12) && swipeCount < 2) {
                swipeCount++;
                float side = swipeCount == 1 ? -5 : 5;
                Location swipeLoc = center.clone().add(side, 3, 5);
                triggerImpactDamage(swipeLoc);
                DisplayBuilder.cyanDust(swipeLoc, 15, 2.0);
                DisplayBuilder.playSound(swipeLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.5f);

                // Rift scar — persistent tear
                for (int y = 0; y < 2; y++) {
                    BlockDisplayHandle scar = displayBuilder.spawnBlock(
                        swipeLoc.clone().add(0, y, 0), Material.PURPLE_STAINED_GLASS);
                    scar.scale(3.0f, 1.0f, 0.3f).glow(80, 0, 140).interpolation(2, 0);
                    clawHandles.add(scar);
                    spawnedEntities.add(scar.entity());
                }
                DisplayBuilder.playSound(swipeLoc, Sound.BLOCK_END_PORTAL_SPAWN, 2.5f, 1.2f);
            }
            // Rift scars persist pulling (12-252 ticks = 12 seconds)
            if (swipeCount > 0 && ticksAlive > 12 && ticksAlive < 252 && ticksAlive % 10 == 0) {
                for (int i = 1; i < clawHandles.size(); i++) {
                    DisplayBuilder.cyanDust(clawHandles.get(i).entity().getLocation(), 3, 1.5);
                }
            }
            // Scars close (tick 252)
            if (ticksAlive == 252) {
                for (int i = 1; i < clawHandles.size(); i++) {
                    DisplayBuilder.cyanDust(clawHandles.get(i).entity().getLocation(), 10, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiftClaw(plugin); }
    }

    // ================================================================
    // #92 — STREAM CONVERGENCE I: SPEAR — Sky streams form a downward spear
    // ================================================================
    public static class StreamConvergenceSpear extends BossAttack {
        private final List<BlockDisplayHandle> spearHandles = new ArrayList<>();
        private boolean spearFired = false;

        public StreamConvergenceSpear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_stream_convergence_spear", AttackType.BOSS, 4), "dragon");
            config.setDamage(36.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 streams converge overhead
            Location convergePt = center.clone().add(0, 25, 0);
            BlockDisplayHandle convergeSphere = displayBuilder.spawnBlock(convergePt, Material.GLOWSTONE);
            convergeSphere.scale(2.0f, 2.0f, 2.0f).glow(255, 255, 255).interpolation(3, 0);
            spearHandles.add(convergeSphere);
            spawnedEntities.add(convergeSphere.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 3.0f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Convergence (0-10 ticks = 0.5 seconds)
            if (ticksAlive < 10 && !spearFired) {
                float shrink = 2.0f - ticksAlive * 0.17f;
                spearHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-shrink / 2, 25 - shrink / 2, -shrink / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(shrink, shrink, shrink),
                    new AxisAngle4f(0, 0, 1, 0)));
                DisplayBuilder.cyanDust(center.clone().add(0, 25, 0), 8, 3.0);
            }
            // Spear fires downward (tick 10)
            else if (ticksAlive == 10 && !spearFired) {
                spearFired = true;
                // Spear body
                BlockDisplayHandle spear = displayBuilder.spawnBlock(
                    center.clone().add(0, 12, 0), Material.GLOWSTONE);
                spear.scale(0.3f, 24.0f, 0.3f).glow(255, 215, 0).interpolation(1, 0);
                spearHandles.add(spear);
                spawnedEntities.add(spear.entity());
                // Impact
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 35, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0f, 1.0f);
            }
            // Crater persists (10-610 ticks = 30 seconds)
            if (spearFired && ticksAlive < 610 && ticksAlive % 20 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 4, 3.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamConvergenceSpear(plugin); }
    }

    // ================================================================
    // #93 — WITHER BARRAGE — 18 fast wither-style skulls in 3 bursts
    // ================================================================
    public static class WitherBarrage extends BossAttack {
        private final List<BlockDisplayHandle> skullHandles = new ArrayList<>();
        private int burstCount = 0;

        public WitherBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_wither_barrage", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Jaw darkens
            BlockDisplayHandle jawCharge = displayBuilder.spawnBlock(
                center.clone().add(0, 10, 1), Material.OBSIDIAN);
            jawCharge.scale(1.5f, 1.5f, 1.5f).glow(20, 0, 30).interpolation(2, 0);
            skullHandles.add(jawCharge);
            spawnedEntities.add(jawCharge.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 3 bursts of 6 skulls: tick 8, 12, 16
            int[] burstTicks = {8, 12, 16};
            double[] burstAngles = {-0.5, 0, 0.5}; // left, center, right

            for (int b = 0; b < 3; b++) {
                if (ticksAlive == burstTicks[b] && burstCount <= b) {
                    burstCount = b + 1;
                    for (int s = 0; s < 6; s++) {
                        double angle = burstAngles[b] + (s - 2.5) * 0.17;
                        Location skullLoc = center.clone().add(Math.cos(angle) * 3, 10, Math.sin(angle) * 3);
                        BlockDisplayHandle skull = displayBuilder.spawnBlock(skullLoc, Material.OBSIDIAN);
                        skull.scale(0.5f, 0.5f, 0.5f).glow(20, 0, 30).interpolation(1, 0);
                        skullHandles.add(skull);
                        spawnedEntities.add(skull.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.5f, 1.2f);
                }
            }
            // Skulls travel outward
            if (burstCount > 0 && ticksAlive > 8) {
                for (int i = 1; i < skullHandles.size(); i++) {
                    int burstIdx = (i - 1) / 6;
                    int skullIdx = (i - 1) % 6;
                    int age = ticksAlive - burstTicks[Math.min(burstIdx, 2)];
                    if (age > 0 && age < 80) {
                        double angle = burstAngles[Math.min(burstIdx, 2)] + (skullIdx - 2.5) * 0.17;
                        float dist = age * 0.5f;
                        skullHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(angle) * (3 + dist), 10 - dist * 0.1, Math.sin(angle) * (3 + dist)));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WitherBarrage(plugin); }
    }

    // ================================================================
    // #94 — CASCADE CHAIN — 3-hit combo: void breath, crystal spike, brimstone
    // ================================================================
    public static class CascadeChain extends BossAttack {
        private final List<BlockDisplayHandle> comboHandles = new ArrayList<>();
        private int hitCount = 0;

        public CascadeChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_cascade_chain", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Hit 1 — Void Breath (tick 10)
            if (ticksAlive == 10 && hitCount == 0) {
                hitCount = 1;
                Location breathLoc = center.clone().add(5, 4, 0);
                BlockDisplayHandle breath = displayBuilder.spawnBlock(breathLoc, Material.PURPLE_STAINED_GLASS);
                breath.scale(10.0f, 2.0f, 2.0f).glow(100, 0, 200).interpolation(1, 0);
                comboHandles.add(breath);
                spawnedEntities.add(breath.entity());
                triggerImpactDamage(breathLoc);
                DisplayBuilder.cyanDust(breathLoc, 15, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_SHOOT, 2.5f, 1.3f);
            }
            // Hit 2 — Crystal Spike Underfoot (tick 18)
            else if (ticksAlive == 18 && hitCount == 1) {
                hitCount = 2;
                Location spikeLoc = center.clone().add(8, 0.1, 3);
                for (int y = 0; y < 3; y++) {
                    BlockDisplayHandle spike = displayBuilder.spawnBlock(
                        spikeLoc.clone().add(0, y, 0), Material.AMETHYST_BLOCK);
                    spike.scale(1.5f, 1.0f, 1.5f).glow(0, 200, 255).interpolation(1, 0);
                    comboHandles.add(spike);
                    spawnedEntities.add(spike.entity());
                }
                triggerImpactDamage(spikeLoc);
                DisplayBuilder.cyanDust(spikeLoc, 12, 2.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 2.0f, 0.9f);
            }
            // Hit 3 — Brimstone Recall from above (tick 26)
            else if (ticksAlive == 26 && hitCount == 2) {
                hitCount = 3;
                Location flameLoc = center.clone().add(6, 0.5, 6);
                BlockDisplayHandle column = displayBuilder.spawnBlock(
                    flameLoc.clone().add(0, 10, 0), Material.MAGMA_BLOCK);
                column.scale(2.0f, 20.0f, 2.0f).glow(200, 0, 100).interpolation(1, 0);
                comboHandles.add(column);
                spawnedEntities.add(column.entity());
                triggerImpactDamage(flameLoc);
                DisplayBuilder.cyanDust(flameLoc, 12, 2.0);
                DisplayBuilder.playSound(center, Sound.ITEM_FIRECHARGE_USE, 2.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CascadeChain(plugin); }
    }

    // ================================================================
    // #95 — FLOOR COLLAPSE: THE ABYSS OPENS — Permanent arena reduction
    // ================================================================
    public static class FloorCollapseAbyss extends BossAttack {
        private final List<BlockDisplayHandle> collapseHandles = new ArrayList<>();
        private boolean collapsed = false;

        public FloorCollapseAbyss(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_floor_collapse_abyss", AttackType.BOSS, 4), "dragon");
            config.setDamage(36.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Crack warning (0-10 ticks = 0.5 seconds)
            if (ticksAlive < 10 && !collapsed) {
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 15, 20.0);
                }
            }
            // Collapse (tick 10)
            else if (ticksAlive == 10 && !collapsed) {
                collapsed = true;
                // Outer ring destruction visual
                for (int i = 0; i < 24; i++) {
                    double angle = (Math.PI * 2 / 24) * i;
                    Location edgeLoc = center.clone().add(Math.cos(angle) * 22, 0.1, Math.sin(angle) * 22);
                    BlockDisplayHandle debris = displayBuilder.spawnBlock(edgeLoc, Material.END_STONE);
                    debris.scale(3.0f, 0.5f, 3.0f).glow(200, 180, 160).interpolation(1, 0);
                    collapseHandles.add(debris);
                    spawnedEntities.add(debris.entity());
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 50, 22.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 3.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.3f);
            }
            // Debris falls into void (10-30 ticks)
            if (collapsed && ticksAlive < 30) {
                float fallDist = (ticksAlive - 10) * 0.5f;
                for (BlockDisplayHandle debris : collapseHandles) {
                    Location loc = debris.entity().getLocation();
                    debris.entity().teleport(loc.clone().add(0, -fallDist, 0));
                }
            }
            // Void edge particles — permanent
            if (collapsed && ticksAlive > 30 && ticksAlive % 10 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 6, 18.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloorCollapseAbyss(plugin); }
    }

    // ================================================================
    // #96 — LEAKING VOID: SURGE — All passive void tears expand and pulse
    // ================================================================
    public static class LeakingVoidSurge extends BossAttack {
        private final List<BlockDisplayHandle> surgeHandles = new ArrayList<>();
        private boolean surgeActive = false;

        public LeakingVoidSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_leaking_void_surge", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // All void tears pulse white
            DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 15, 15.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 6 && !surgeActive) {
                surgeActive = true;
                // Simulate 5 expanded tears
                for (int i = 0; i < 5; i++) {
                    double ox = (Math.random() - 0.5) * 24;
                    double oz = (Math.random() - 0.5) * 24;
                    Location tearLoc = center.clone().add(ox, 0.1, oz);
                    BlockDisplayHandle expandedTear = displayBuilder.spawnBlock(tearLoc, Material.PURPLE_STAINED_GLASS);
                    expandedTear.scale(5.0f, 0.3f, 5.0f).glow(100, 0, 200).interpolation(3, 0);
                    surgeHandles.add(expandedTear);
                    spawnedEntities.add(expandedTear.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 2.5f, 0.7f);
            }
            // Surge waves from each tear (6-106 ticks = 5 seconds, every 30 ticks)
            if (surgeActive && ticksAlive > 6 && ticksAlive < 106 && ticksAlive % 30 == 0) {
                for (BlockDisplayHandle tear : surgeHandles) {
                    Location tearLoc = tear.entity().getLocation();
                    triggerImpactDamage(tearLoc);
                    DisplayBuilder.cyanDust(tearLoc, 10, 5.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 2.0f, 0.5f);
            }
            // Tears snap back to normal (tick 106)
            if (surgeActive && ticksAlive == 106) {
                for (BlockDisplayHandle tear : surgeHandles) {
                    DisplayBuilder.cyanDust(tear.entity().getLocation(), 8, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_HURT, 2.0f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LeakingVoidSurge(plugin); }
    }

    // ================================================================
    // #97 — TAIL SWEEP: ARENA WIDTH — Full 180-degree sweep, 8-block knockback
    // ================================================================
    public static class TailSweepArenaWidth extends BossAttack {
        private final List<BlockDisplayHandle> sweepHandles = new ArrayList<>();
        private boolean swept = false;

        public TailSweepArenaWidth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_tail_sweep_arena_width", AttackType.BOSS, 4), "dragon");
            config.setDamage(26.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Tail raise (0-6 ticks = 0.3 seconds)
            if (ticksAlive < 6 && !swept) {
                DisplayBuilder.cyanDust(center.clone().add(0, 3, -4), 6, 3.0);
            }
            // Sweep (6-20 ticks = 0.7 seconds)
            else if (ticksAlive >= 6 && ticksAlive <= 20 && !swept) {
                float progress = (ticksAlive - 6) / 14.0f;
                double sweepAngle = progress * Math.PI;
                double tailX = Math.cos(sweepAngle) * 15;
                double tailZ = Math.sin(sweepAngle) * 15;
                Location sweepPt = center.clone().add(tailX, 1.5, tailZ);
                if (ticksAlive % 3 == 0) {
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(sweepPt, Material.CRYING_OBSIDIAN);
                    trail.scale(3.0f, 3.0f, 1.0f).glow(140, 0, 200).interpolation(1, 0);
                    sweepHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                    triggerImpactDamage(sweepPt);
                }
                if (ticksAlive == 20) {
                    swept = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_BIG_FALL, 2.0f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TailSweepArenaWidth(plugin); }
    }

    // ================================================================
    // #98 — VOID EMPEROR'S GAZE — Targeted damage-over-time lock
    // ================================================================
    public static class VoidEmperorsGaze extends BossAttack {
        private final List<BlockDisplayHandle> gazeHandles = new ArrayList<>();
        private boolean gazeActive = false;
        private int gazeTick = 0;

        public VoidEmperorsGaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_emperors_gaze_p3", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(340);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eye beams converge on target
            BlockDisplayHandle leftBeam = displayBuilder.spawnBlock(
                center.clone().add(-1, 12, 2), Material.WHITE_STAINED_GLASS);
            leftBeam.scale(0.2f, 0.2f, 10.0f).glow(255, 255, 255).interpolation(2, 0);
            gazeHandles.add(leftBeam);
            spawnedEntities.add(leftBeam.entity());
            BlockDisplayHandle rightBeam = displayBuilder.spawnBlock(
                center.clone().add(1, 12, 2), Material.WHITE_STAINED_GLASS);
            rightBeam.scale(0.2f, 0.2f, 10.0f).glow(255, 255, 255).interpolation(2, 0);
            gazeHandles.add(rightBeam);
            spawnedEntities.add(rightBeam.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 3.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !gazeActive) {
                gazeActive = true;
                gazeTick = ticksAlive;
            }
            // Gaze active — 7 HP/sec for 4 seconds (10-90 ticks)
            if (gazeActive && ticksAlive - gazeTick < 80) {
                if (ticksAlive % 10 == 0) {
                    triggerImpactDamage(center.clone().add(0, 2, 5));
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 5), 6, 1.5);
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 2.0f, 0.5f);
                }
            }
            // Gaze breaks (tick 90)
            if (gazeActive && ticksAlive - gazeTick == 80) {
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 5), 12, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_DEATH, 1.5f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEmperorsGaze(plugin); }
    }

    // ================================================================
    // #99 — CRYSTAL CAGE DROP — 8 pillars cage targeted player
    // ================================================================
    public static class CrystalCageDrop extends BossAttack {
        private final List<BlockDisplayHandle> cageHandles = new ArrayList<>();
        private boolean cageDropped = false;
        private int dropTick = 0;

        public CrystalCageDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_crystal_cage_drop", AttackType.BOSS, 4), "dragon");
            config.setDamage(30.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 8 crystal pillars hovering above
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location pillarLoc = center.clone().add(Math.cos(angle) * 3, 15, Math.sin(angle) * 3);
                BlockDisplayHandle pillar = displayBuilder.spawnBlock(pillarLoc, Material.AMETHYST_BLOCK);
                pillar.scale(0.8f, 4.0f, 0.8f).glow(0, 210, 255).interpolation(2, 0);
                cageHandles.add(pillar);
                spawnedEntities.add(pillar.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pillars descend (0-8 ticks = 0.4 seconds)
            if (ticksAlive < 8 && !cageDropped) {
                float dropY = 15 - ticksAlive * 1.5f;
                for (int i = 0; i < cageHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    cageHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 3, dropY, Math.sin(angle) * 3));
                }
            }
            // Cage forms (tick 8)
            else if (ticksAlive == 8 && !cageDropped) {
                cageDropped = true;
                dropTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 2.0f, 0.8f);
            }
            // Cage active — crystal pulse damage (8-108 ticks = 5 seconds)
            else if (cageDropped && ticksAlive - dropTick < 100) {
                if (ticksAlive % 30 == 0) {
                    triggerImpactDamage(center);
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 8, 3.0);
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 4, 3.0);
                }
            }
            // Cage collapses — crushing implosion (tick 108)
            else if (cageDropped && ticksAlive - dropTick == 100) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 30, 5.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 2.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalCageDrop(plugin); }
    }

    // ================================================================
    // #100 — THE VOID EMPEROR'S GAMBIT — 4-stage combo, 15 seconds
    // ================================================================
    public static class VoidEmperorsGambit extends BossAttack {
        private final List<BlockDisplayHandle> gambitHandles = new ArrayList<>();
        private int subAttack = 0;

        public VoidEmperorsGambit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_emperors_gambit", AttackType.BOSS, 4), "dragon");
            config.setDamage(36.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(6);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon rises to center, wings spread, massive particle bloom
            BlockDisplayHandle bloom = displayBuilder.spawnBlock(
                center.clone().add(0, 30, 0), Material.WHITE_STAINED_GLASS);
            bloom.scale(10.0f, 10.0f, 10.0f).glow(255, 255, 255).interpolation(3, 0);
            gambitHandles.add(bloom);
            spawnedEntities.add(bloom.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 3.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 1-second announcement hold (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 30, 0), 12, 10.0);
                }
            }

            // Sub-Attack 1 — Convergence Call (tick 20): all blades fire inward
            if (ticksAlive == 20 && subAttack == 0) {
                subAttack = 1;
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    Location bladeLoc = center.clone().add(Math.cos(angle) * 20, 8, Math.sin(angle) * 20);
                    BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                    blade.scale(0.3f, 1.0f, 0.3f).glow(255, 255, 255).interpolation(1, 0);
                    gambitHandles.add(blade);
                    spawnedEntities.add(blade.entity());
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 8, 0), 20, 15.0);
            }

            // Sub-Attack 2 — Void Lattice (tick 26): 8x8 bolt grid downward
            if (ticksAlive == 26 && subAttack == 1) {
                subAttack = 2;
                for (int x = -3; x <= 4; x++) {
                    for (int z = -3; z <= 4; z++) {
                        Location boltLoc = center.clone().add(x * 4, 0.2, z * 4);
                        BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltLoc, Material.PURPLE_STAINED_GLASS);
                        bolt.scale(0.5f, 0.3f, 0.5f).glow(100, 0, 200).interpolation(1, 0);
                        spawnedEntities.add(bolt.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 15, 0), 25, 20.0);
            }

            // Sub-Attack 3 — Essence Release (tick 32): 3 concentric rings
            if (ticksAlive == 32 && subAttack == 2) {
                subAttack = 3;
                double[][] ringData = {{5, 100, 0, 200}, {10, 0, 200, 255}, {15, 255, 0, 100}};
                Material[] ringMats = {Material.PURPLE_STAINED_GLASS, Material.AMETHYST_BLOCK, Material.MAGMA_BLOCK};
                for (int r = 0; r < 3; r++) {
                    double radius = ringData[r][0];
                    for (int i = 0; i < 12; i++) {
                        double angle = (Math.PI * 2 / 12) * i;
                        Location ringPt = center.clone().add(Math.cos(angle) * radius, 1, Math.sin(angle) * radius);
                        BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, ringMats[r]);
                        ring.scale(2.0f, 2.0f, 0.5f).glow(
                            (int) ringData[r][1], (int) ringData[r][2], (int) ringData[r][3]).interpolation(1, 0);
                        spawnedEntities.add(ring.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 20, 15.0);
            }

            // Sub-Attack 4 — The Final Word (tick 38): single concentrated beam
            if (ticksAlive == 38 && subAttack == 3) {
                subAttack = 4;
                BlockDisplayHandle beam = displayBuilder.spawnBlock(
                    center.clone().add(0, 5, 0), Material.GLOWSTONE);
                beam.scale(1.0f, 1.0f, 40.0f).glow(255, 255, 255).interpolation(1, 0);
                gambitHandles.add(beam);
                spawnedEntities.add(beam.entity());
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 30, 20.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 2.5f, 0.7f);
            }

            // Environmental hazards resume (tick 50)
            if (ticksAlive == 50) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 3.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEmperorsGambit(plugin); }
    }
}
