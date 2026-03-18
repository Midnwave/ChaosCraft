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
 * Phase 2C: Attacks #69-84
 * Late Phase 2: Maximum terrain degradation, 3rd/4th mini boss gates,
 * combined absorption attacks, Phase 2->3 transition.
 * Phase 2 damage range: 14-24 HP.
 * NO status effects — damage only.
 */
public final class DragonPhase2C {

    private DragonPhase2C() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrystallineOverdrive(plugin));
        registry.register(new WingsOfTheFallen(plugin));
        registry.register(new TailSweepVoidLash(plugin));
        registry.register(new EmperorsGaze(plugin));
        registry.register(new CrystallineWeb(plugin));
        registry.register(new BrimstoneNova(plugin));
        registry.register(new VoidConsumption(plugin));
        registry.register(new ThirdSummonDescends(plugin));
        registry.register(new SpectralOrbitBarrage(plugin));
        registry.register(new SkyStreamConvergence(plugin));
        registry.register(new TearNetwork(plugin));
        registry.register(new CrystalSpineOverdrive(plugin));
        registry.register(new BrimstoneCrown(plugin));
        registry.register(new VoidMaelstromAscendant(plugin));
        registry.register(new TripleSurge(plugin));
        registry.register(new PhaseTransitionRemember(plugin));
    }

    // ================================================================
    // #69 — CRYSTALLINE OVERDRIVE — Mass spike eruption + sweeping chest beam
    // ================================================================
    public static class CrystallineOverdrive extends BossAttack {
        private final List<BlockDisplayHandle> overdriveHandles = new ArrayList<>();
        private boolean overdriveFired = false;
        private int fireTick = 0;

        public CrystallineOverdrive(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_crystalline_overdrive", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Terrain patches flare (0-20 ticks)
            if (ticksAlive < 20 && !overdriveFired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 12, 15.0);
                }
            }
            // Mass spike eruption (tick 20) — 8+ spikes
            else if (ticksAlive == 20 && !overdriveFired) {
                overdriveFired = true;
                fireTick = ticksAlive;
                for (int i = 0; i < 8; i++) {
                    double ox = (Math.random() - 0.5) * 26;
                    double oz = (Math.random() - 0.5) * 26;
                    Location spikeLoc = center.clone().add(ox, 0.1, oz);
                    for (int y = 0; y < 5; y++) {
                        BlockDisplayHandle spike = displayBuilder.spawnBlock(
                            spikeLoc.clone().add(0, y, 0), Material.AMETHYST_BLOCK);
                        spike.scale(1.0f, 1.0f, 1.0f).glow(160, 240, 255).interpolation(1, 0);
                        overdriveHandles.add(spike);
                        spawnedEntities.add(spike.entity());
                    }
                    triggerImpactDamage(spikeLoc);
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 20, 15.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.7f);
            }
            // Crystal chest beam sweeps (20-80 ticks = 3 seconds)
            else if (overdriveFired && ticksAlive - fireTick < 60) {
                float sweepAngle = ((ticksAlive - fireTick) / 60.0f) * (float) Math.PI;
                double beamX = Math.cos(sweepAngle) * 20;
                double beamZ = Math.sin(sweepAngle) * 20;
                Location beamEnd = center.clone().add(beamX, 5, beamZ);
                if (ticksAlive % 4 == 0) {
                    BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(
                        beamEnd, Material.AMETHYST_BLOCK);
                    beamSeg.scale(3.0f, 2.0f, 0.5f).glow(160, 240, 255).interpolation(1, 0);
                    spawnedEntities.add(beamSeg.entity());
                    triggerImpactDamage(beamEnd);
                    DisplayBuilder.cyanDust(beamEnd, 6, 2.0);
                }
            }
            // Spike collapse aftermath (80-140 ticks)
            else if (overdriveFired && ticksAlive - fireTick == 60) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineOverdrive(plugin); }
    }

    // ================================================================
    // #70 — WINGS OF THE FALLEN — 3 dives, each with different boss mechanic
    // ================================================================
    public static class WingsOfTheFallen extends BossAttack {
        private final List<BlockDisplayHandle> diveHandles = new ArrayList<>();
        private int diveCount = 0;

        public WingsOfTheFallen(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_wings_of_the_fallen", AttackType.BOSS, 4), "dragon");
            config.setDamage(22.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wings shift — left: PORTAL purple, right: brimstone orange, spine: crystal cyan
            BlockDisplayHandle leftWing = displayBuilder.spawnBlock(
                center.clone().add(-5, 10, 0), Material.PURPLE_STAINED_GLASS);
            leftWing.scale(5.0f, 0.5f, 3.0f).glow(100, 0, 200).interpolation(2, 0);
            diveHandles.add(leftWing);
            spawnedEntities.add(leftWing.entity());
            BlockDisplayHandle rightWing = displayBuilder.spawnBlock(
                center.clone().add(5, 10, 0), Material.MAGMA_BLOCK);
            rightWing.scale(5.0f, 0.5f, 3.0f).glow(255, 80, 0).interpolation(2, 0);
            diveHandles.add(rightWing);
            spawnedEntities.add(rightWing.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 3 dives at ticks 20, 60, 100 (40 ticks apart = 2 seconds)
            int[] diveTicks = {20, 60, 100};
            Material[] diveMats = {Material.PURPLE_STAINED_GLASS, Material.MAGMA_BLOCK, Material.AMETHYST_BLOCK};
            int[][] diveColors = {{100, 0, 200}, {255, 80, 0}, {0, 200, 255}};
            Sound[] diveSounds = {Sound.ENTITY_ENDERMAN_TELEPORT, Sound.ENTITY_BLAZE_SHOOT, Sound.BLOCK_AMETHYST_CLUSTER_BREAK};

            for (int d = 0; d < 3; d++) {
                if (ticksAlive == diveTicks[d] && diveCount <= d) {
                    diveCount = d + 1;
                    double ox = (Math.random() - 0.5) * 20;
                    double oz = (Math.random() - 0.5) * 20;
                    Location diveLoc = center.clone().add(ox, 0.2, oz);
                    BlockDisplayHandle impact = displayBuilder.spawnBlock(diveLoc, diveMats[d]);
                    impact.scale(3.0f, 0.3f, 3.0f).glow(diveColors[d][0], diveColors[d][1], diveColors[d][2]).interpolation(2, 0);
                    spawnedEntities.add(impact.entity());
                    triggerImpactDamage(diveLoc);
                    DisplayBuilder.cyanDust(diveLoc, 15, 3.0);
                    DisplayBuilder.playSound(diveLoc, diveSounds[d], 0.8f, 0.8f);
                }
            }

            // Aftermath patches — one per dive type
            if (ticksAlive > 100 && ticksAlive < 200 && ticksAlive % 15 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 4, 10.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WingsOfTheFallen(plugin); }
    }

    // ================================================================
    // #71 — TAIL SWEEP: VOID LASH — 270-degree tail with void shockwave
    // ================================================================
    public static class TailSweepVoidLash extends BossAttack {
        private final List<BlockDisplayHandle> tailHandles = new ArrayList<>();
        private boolean swept = false;

        public TailSweepVoidLash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_tail_sweep_void_lash", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Tail tip glows violet
            BlockDisplayHandle tailTip = displayBuilder.spawnBlock(
                center.clone().add(0, 2, -5), Material.CRYING_OBSIDIAN);
            tailTip.scale(0.5f, 0.5f, 3.0f).glow(100, 0, 200).interpolation(3, 0);
            tailHandles.add(tailTip);
            spawnedEntities.add(tailTip.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 0.8f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !swept) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, -5), 6, 1.5);
                }
            }
            // Tail sweeps 270 degrees (tick 20-40)
            else if (ticksAlive >= 20 && ticksAlive <= 40 && !swept) {
                float progress = (ticksAlive - 20) / 20.0f;
                double sweepAngle = progress * Math.PI * 1.5; // 270 degrees
                double tx = Math.cos(sweepAngle) * 8;
                double tz = Math.sin(sweepAngle) * 8;
                tailHandles.get(0).entity().teleport(
                    center.clone().add(tx, 2, tz));
                if (ticksAlive % 4 == 0) {
                    Location sweepLoc = center.clone().add(tx, 0.2, tz);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(sweepLoc, Material.PURPLE_STAINED_GLASS);
                    trail.scale(2.0f, 0.15f, 2.0f).glow(100, 0, 200).interpolation(2, 0);
                    spawnedEntities.add(trail.entity());
                    triggerImpactDamage(sweepLoc);
                }
                if (ticksAlive == 40) {
                    swept = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.7f);
                }
            }
            // Shockwave ring from tail endpoint (tick 44)
            else if (swept && ticksAlive == 44) {
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    Location ringPt = center.clone().add(Math.cos(angle) * 15, 0.2, Math.sin(angle) * 15);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.PURPLE_STAINED_GLASS);
                    ring.scale(1.5f, 0.2f, 1.5f).glow(120, 0, 200).interpolation(1, 0);
                    spawnedEntities.add(ring.entity());
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 20, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.0f);
            }
            // Sweep arc aftermath (44-204 ticks = 8 seconds)
            if (swept && ticksAlive > 44 && ticksAlive < 204 && ticksAlive % 15 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 4, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TailSweepVoidLash(plugin); }
    }

    // ================================================================
    // #72 — THE EMPEROR'S GAZE — Vulnerability debuff stare (no direct damage)
    // ================================================================
    public static class EmperorsGaze extends BossAttack {
        private final List<BlockDisplayHandle> gazeHandles = new ArrayList<>();
        private boolean gazeActive = false;
        private int gazeTick = 0;

        public EmperorsGaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_emperors_gaze", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eye radiance beams forming
            BlockDisplayHandle leftBeam = displayBuilder.spawnBlock(
                center.clone().add(-1, 12, 2), Material.WHITE_STAINED_GLASS);
            leftBeam.scale(0.2f, 0.2f, 8.0f).glow(255, 255, 255).interpolation(3, 0);
            gazeHandles.add(leftBeam);
            spawnedEntities.add(leftBeam.entity());
            BlockDisplayHandle rightBeam = displayBuilder.spawnBlock(
                center.clone().add(1, 12, 2), Material.WHITE_STAINED_GLASS);
            rightBeam.scale(0.2f, 0.2f, 8.0f).glow(255, 255, 255).interpolation(3, 0);
            gazeHandles.add(rightBeam);
            spawnedEntities.add(rightBeam.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !gazeActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 12, 2), 6, 2.0);
                }
            }
            else if (ticksAlive == 20 && !gazeActive) {
                gazeActive = true;
                gazeTick = ticksAlive;
            }
            // Stare active — Dragon watches for 4 seconds (20-100 ticks)
            else if (gazeActive && ticksAlive - gazeTick < 80) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 12, 2), 4, 4.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.6f, 1.0f);
                }
            }
            // Gaze completes — vulnerability halo applied (tick 100)
            else if (gazeActive && ticksAlive - gazeTick == 80) {
                // PORTAL halo above target
                BlockDisplayHandle halo = displayBuilder.spawnBlock(
                    center.clone().add(0, 4, 5), Material.PURPLE_STAINED_GLASS);
                halo.scale(2.0f, 0.2f, 2.0f).glow(120, 0, 200).interpolation(3, 0);
                gazeHandles.add(halo);
                spawnedEntities.add(halo.entity());
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 5), 10, 1.0);
            }
            // Vulnerability period (100-220 ticks = 6 seconds)
            if (gazeActive && ticksAlive - gazeTick > 80 && ticksAlive - gazeTick < 200) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 5), 3, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EmperorsGaze(plugin); }
    }

    // ================================================================
    // #73 — CRYSTALLINE WEB — Hexagonal razor web grid across arena
    // ================================================================
    public static class CrystallineWeb extends BossAttack {
        private final List<BlockDisplayHandle> webHandles = new ArrayList<>();
        private boolean webActive = false;

        public CrystallineWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_crystalline_web", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(760);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !webActive) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 12, 15.0);
                }
            }
            // Web erupts — hexagonal mesh strands (tick 20)
            else if (ticksAlive == 20 && !webActive) {
                webActive = true;
                // Create web strands in a hex grid pattern
                for (int line = 0; line < 6; line++) {
                    double angle = (Math.PI / 3) * line;
                    for (int seg = -6; seg <= 6; seg++) {
                        Location strandLoc = center.clone().add(
                            Math.cos(angle) * seg * 4, 0.5, Math.sin(angle) * seg * 4);
                        BlockDisplayHandle strand = displayBuilder.spawnBlock(strandLoc, Material.AMETHYST_BLOCK);
                        strand.scale(0.1f, 2.0f, 3.5f).glow(200, 250, 255).interpolation(2, 0);
                        webHandles.add(strand);
                        spawnedEntities.add(strand.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.7f, 0.9f);
            }
            // Web active (20-120 ticks = 5 seconds)
            else if (webActive && ticksAlive < 120) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 6, 12.0);
                }
            }
            // Web collapses (tick 120) — aftermath traces
            else if (webActive && ticksAlive == 120) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 20, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystallineWeb(plugin); }
    }

    // ================================================================
    // #74 — BRIMSTONE NOVA — Centered spherical explosion, 3 damage tiers
    // ================================================================
    public static class BrimstoneNova extends BossAttack {
        private final List<BlockDisplayHandle> novaHandles = new ArrayList<>();
        private boolean novaDetonated = false;

        public BrimstoneNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_brimstone_nova", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon's underbelly goes incandescent
            BlockDisplayHandle bellyGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 5, 0), Material.MAGMA_BLOCK);
            bellyGlow.scale(4.0f, 2.0f, 4.0f).glow(255, 60, 0).interpolation(3, 0);
            novaHandles.add(bellyGlow);
            spawnedEntities.add(bellyGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !novaDetonated) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 12, 3.0);
                }
            }
            // Nova detonates (tick 20) — 3 concentric explosion rings
            else if (ticksAlive == 20 && !novaDetonated) {
                novaDetonated = true;
                // Inner ring (R=3)
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location innerPt = center.clone().add(Math.cos(angle) * 3, 2, Math.sin(angle) * 3);
                    BlockDisplayHandle inner = displayBuilder.spawnBlock(innerPt, Material.MAGMA_BLOCK);
                    inner.scale(2.0f, 4.0f, 2.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(inner.entity());
                }
                // Middle ring (R=8)
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    Location midPt = center.clone().add(Math.cos(angle) * 8, 1, Math.sin(angle) * 8);
                    BlockDisplayHandle mid = displayBuilder.spawnBlock(midPt, Material.MAGMA_BLOCK);
                    mid.scale(1.5f, 2.0f, 1.5f).glow(255, 100, 0).interpolation(1, 0);
                    spawnedEntities.add(mid.entity());
                }
                // Outer ring (R=15)
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    Location outerPt = center.clone().add(Math.cos(angle) * 15, 0.5, Math.sin(angle) * 15);
                    BlockDisplayHandle outer = displayBuilder.spawnBlock(outerPt, Material.ORANGE_STAINED_GLASS);
                    outer.scale(1.0f, 1.0f, 1.0f).glow(255, 100, 0).interpolation(1, 0);
                    spawnedEntities.add(outer.entity());
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 35, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.6f);
            }
            // Aftermath — landing crater with burn patches (20-180 ticks = 8 seconds)
            if (novaDetonated && ticksAlive < 180 && ticksAlive % 15 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 4, 5.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneNova(plugin); }
    }

    // ================================================================
    // #75 — VOID CONSUMPTION — 8 tracking void orbs
    // ================================================================
    public static class VoidConsumption extends BossAttack {
        private final List<BlockDisplayHandle> orbHandles = new ArrayList<>();
        private boolean orbsReleased = false;
        private int releaseTick = 0;

        public VoidConsumption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_consumption", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(640);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 8 orbital void orbs form
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location orbLoc = center.clone().add(Math.cos(angle) * 6, 10, Math.sin(angle) * 6);
                BlockDisplayHandle orb = displayBuilder.spawnBlock(orbLoc, Material.CRYING_OBSIDIAN);
                orb.scale(1.5f, 1.5f, 1.5f).glow(120, 0, 200).interpolation(3, 0);
                orbHandles.add(orb);
                spawnedEntities.add(orb.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Orbs orbit slowly (0-20 ticks)
            if (ticksAlive < 20 && !orbsReleased) {
                for (int i = 0; i < orbHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i + ticksAlive * 0.05;
                    orbHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 6, 10, Math.sin(angle) * 6));
                }
            }
            // Orbs release — track outward (tick 20)
            else if (ticksAlive == 20 && !orbsReleased) {
                orbsReleased = true;
                releaseTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.1f);
            }
            // Orbs track outward (20-170 ticks = 7.5 seconds)
            else if (orbsReleased && ticksAlive - releaseTick < 150) {
                float dist = (ticksAlive - releaseTick) * 0.12f;
                for (int i = 0; i < orbHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i + (ticksAlive - releaseTick) * 0.03;
                    orbHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (6 + dist), 10 - dist * 0.3, Math.sin(angle) * (6 + dist)));
                }
                if (ticksAlive % 10 == 0) {
                    for (BlockDisplayHandle orb : orbHandles) {
                        DisplayBuilder.cyanDust(orb.entity().getLocation(), 3, 1.0);
                    }
                }
            }
            // Orbs dissipate (tick 170)
            else if (orbsReleased && ticksAlive - releaseTick == 150) {
                for (BlockDisplayHandle orb : orbHandles) {
                    DisplayBuilder.cyanDust(orb.entity().getLocation(), 6, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_HIT, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidConsumption(plugin); }
    }

    // ================================================================
    // #76 — THIRD SUMMON: THE EMPEROR DESCENDS — Vertical dive, Mini Boss 4
    // ================================================================
    public static class ThirdSummonDescends extends BossAttack {
        private final List<BlockDisplayHandle> descentHandles = new ArrayList<>();
        private boolean diveLanded = false;

        public ThirdSummonDescends(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_third_summon_descends", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon rises to Y+50 — prediction circles on ground
            for (int r = 0; r < 3; r++) {
                double radius = 5 + r * 5;
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    Location circlePt = center.clone().add(Math.cos(angle) * radius, 0.05, Math.sin(angle) * radius);
                    BlockDisplayHandle marker = displayBuilder.spawnBlock(circlePt, Material.CRYING_OBSIDIAN);
                    marker.scale(1.0f, 0.05f, 1.0f).glow(140, 0, 200).interpolation(3, 0);
                    descentHandles.add(marker);
                    spawnedEntities.add(marker.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.5f, 0.8f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 2.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dragon at Y+50 for 5 seconds (0-100 ticks)
            if (ticksAlive < 100 && !diveLanded) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 40, 0), 12, 5.0);
                }
                if (ticksAlive % 20 == 0) {
                    for (BlockDisplayHandle marker : descentHandles) {
                        DisplayBuilder.cyanDust(marker.entity().getLocation(), 2, 1.0);
                    }
                }
            }
            // Dragon dives — terminal velocity impact (tick 100)
            else if (ticksAlive == 100 && !diveLanded) {
                diveLanded = true;
                // Massive impact crater
                BlockDisplayHandle crater = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.1, 0), Material.CRYING_OBSIDIAN);
                crater.scale(12.0f, 0.5f, 12.0f).glow(100, 0, 160).interpolation(2, 0);
                descentHandles.add(crater);
                spawnedEntities.add(crater.entity());
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 50, 20.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.5f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
            }
            // Tri-color crater eruption + Mini Boss 4 emerges (tick 120)
            else if (diveLanded && ticksAlive == 120) {
                DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 30, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.8f, 0.7f);
            }
            // Crater remains as permanent hazard
            if (diveLanded && ticksAlive > 120 && ticksAlive % 20 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 8, 6.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThirdSummonDescends(plugin); }
    }

    // ================================================================
    // #77 — SPECTRAL ORBIT BARRAGE — Sequential tracking blade fire
    // ================================================================
    public static class SpectralOrbitBarrage extends BossAttack {
        private final List<BlockDisplayHandle> bladeHandles = new ArrayList<>();
        private int bladesFired = 0;

        public SpectralOrbitBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_spectral_orbit_barrage", AttackType.BOSS, 4), "dragon");
            config.setDamage(22.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Blades glow vivid violet
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 / 10) * i;
                Location bladeLoc = center.clone().add(Math.cos(angle) * 5, 10, Math.sin(angle) * 5);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                blade.scale(0.2f, 1.2f, 0.3f).glow(200, 50, 255).interpolation(2, 0);
                bladeHandles.add(blade);
                spawnedEntities.add(blade.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fire 2 blades per second for 5 seconds (20-120 ticks)
            if (ticksAlive >= 20 && ticksAlive < 120 && ticksAlive % 10 == 0 && bladesFired < 10) {
                int idx = bladesFired;
                bladesFired++;
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 0.7f, 1.3f);
            }
            // Blades track outward
            if (bladesFired > 0 && ticksAlive > 20) {
                for (int i = 0; i < bladesFired && i < bladeHandles.size(); i++) {
                    int fireAge = ticksAlive - (20 + i * 10);
                    if (fireAge > 0 && fireAge < 120) {
                        double angle = (Math.PI * 2 / 10) * i + fireAge * 0.04;
                        float dist = 5 + fireAge * 0.15f;
                        bladeHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(angle) * dist, 10 - fireAge * 0.05, Math.sin(angle) * dist));
                    }
                }
            }
            // Wall detonations — embedded blades explode (tick 180)
            if (ticksAlive == 180) {
                for (BlockDisplayHandle blade : bladeHandles) {
                    Location detonLoc = blade.entity().getLocation();
                    triggerImpactDamage(detonLoc);
                    DisplayBuilder.cyanDust(detonLoc, 6, 3.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpectralOrbitBarrage(plugin); }
    }

    // ================================================================
    // #78 — SKY STREAM CONVERGENCE — Sky streams weaponized
    // ================================================================
    public static class SkyStreamConvergence extends BossAttack {
        private final List<BlockDisplayHandle> streamHandles = new ArrayList<>();
        private boolean convergenceActive = false;
        private int convergeTick = 0;

        public SkyStreamConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_sky_stream_convergence", AttackType.BOSS, 4), "dragon");
            config.setDamage(22.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(2);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 sky streams bending downward — magenta, gold, white
            Material[] mats = {Material.MAGENTA_STAINED_GLASS, Material.YELLOW_STAINED_GLASS, Material.WHITE_STAINED_GLASS};
            int[][] colors = {{255, 50, 255}, {255, 215, 0}, {255, 255, 255}};
            for (int i = 0; i < 3; i++) {
                Location streamLoc = center.clone().add((i - 1) * 8, 30, 0);
                BlockDisplayHandle stream = displayBuilder.spawnBlock(streamLoc, mats[i]);
                stream.scale(3.0f, 30.0f, 3.0f).glow(colors[i][0], colors[i][1], colors[i][2]).interpolation(4, 0);
                streamHandles.add(stream);
                spawnedEntities.add(stream.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !convergenceActive) {
                // Streams curve toward target
                for (int i = 0; i < streamHandles.size(); i++) {
                    float bendProgress = ticksAlive / 20.0f;
                    double targetX = (i - 1) * 8 * (1 - bendProgress);
                    streamHandles.get(i).entity().teleport(
                        center.clone().add(targetX, 30 - bendProgress * 20, 0));
                }
            }
            else if (ticksAlive == 20 && !convergenceActive) {
                convergenceActive = true;
                convergeTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
            }
            // Convergence active — streams damage in path (20-100 ticks = 4 seconds)
            else if (convergenceActive && ticksAlive - convergeTick < 80) {
                if (ticksAlive % 4 == 0) {
                    triggerImpactDamage(center);
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 8, 3.0);
                }
            }
            // Convergence explosion or miss (tick 100)
            else if (convergenceActive && ticksAlive - convergeTick == 80) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 35, 20.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.9f);
            }
            // Streams return to normal (100-140 ticks)
            if (convergenceActive && ticksAlive - convergeTick > 80 && ticksAlive - convergeTick < 120) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 25, 0), 4, 8.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyStreamConvergence(plugin); }
    }

    // ================================================================
    // #79 — TEAR NETWORK — 5 linked dimensional tears, chain reaction
    // ================================================================
    public static class TearNetwork extends BossAttack {
        private final List<BlockDisplayHandle> tearHandles = new ArrayList<>();
        private boolean tearsActive = false;

        public TearNetwork(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_tear_network", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 5 tear positions flash
            for (int i = 0; i < 5; i++) {
                double ox = (Math.random() - 0.5) * 24;
                double oz = (Math.random() - 0.5) * 24;
                Location tearLoc = center.clone().add(ox, 0.1, oz);
                BlockDisplayHandle tearMarker = displayBuilder.spawnBlock(tearLoc, Material.PURPLE_STAINED_GLASS);
                tearMarker.scale(0.8f, 0.1f, 0.8f).glow(100, 0, 200).interpolation(3, 0);
                tearHandles.add(tearMarker);
                spawnedEntities.add(tearMarker.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !tearsActive) {
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle marker : tearHandles) {
                        DisplayBuilder.cyanDust(marker.entity().getLocation(), 4, 1.0);
                    }
                }
            }
            // Tears open simultaneously (tick 20)
            else if (ticksAlive == 20 && !tearsActive) {
                tearsActive = true;
                for (BlockDisplayHandle marker : tearHandles) {
                    Location tearLoc = marker.entity().getLocation();
                    for (int y = 0; y < 5; y++) {
                        BlockDisplayHandle tear = displayBuilder.spawnBlock(
                            tearLoc.clone().add(0, y, 0), Material.PURPLE_STAINED_GLASS);
                        tear.scale(2.0f, 1.0f, 0.3f).glow(120, 0, 200).interpolation(2, 0);
                        spawnedEntities.add(tear.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
            }
            // Tears active (20-140 ticks = 6 seconds)
            else if (tearsActive && ticksAlive < 140) {
                if (ticksAlive % 10 == 0) {
                    for (BlockDisplayHandle marker : tearHandles) {
                        DisplayBuilder.cyanDust(marker.entity().getLocation().add(0, 2.5, 0), 3, 1.0);
                    }
                }
            }
            // Chain reaction pulse (tick 80 — triggered mid-duration)
            if (tearsActive && ticksAlive == 80) {
                for (BlockDisplayHandle marker : tearHandles) {
                    Location pulseLoc = marker.entity().getLocation();
                    triggerImpactDamage(pulseLoc);
                    DisplayBuilder.cyanDust(pulseLoc, 12, 8.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.7f);
            }
            // Void patch aftermath
            if (tearsActive && ticksAlive > 140 && ticksAlive < 300 && ticksAlive % 15 == 0) {
                for (BlockDisplayHandle marker : tearHandles) {
                    DisplayBuilder.cyanDust(marker.entity().getLocation(), 2, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TearNetwork(plugin); }
    }

    // ================================================================
    // #80 — CRYSTAL SPINE BARRAGE: OVERDRIVE — 12 salvos of 6 spines
    // ================================================================
    public static class CrystalSpineOverdrive extends BossAttack {
        private final List<BlockDisplayHandle> spineHandles = new ArrayList<>();
        private int salvoCount = 0;

        public CrystalSpineOverdrive(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_crystal_spine_overdrive", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 2.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Full spine ignite telegraph (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 10, 4.0);
                }
            }
            // Fire salvos (20-140 ticks = 6 seconds, 1 salvo per 10 ticks)
            else if (ticksAlive >= 20 && ticksAlive < 140 && ticksAlive % 10 == 0 && salvoCount < 12) {
                salvoCount++;
                double dragonAngle = salvoCount * 0.4;
                for (int i = 0; i < 6; i++) {
                    double angle = dragonAngle + (Math.PI * 2 / 6) * i;
                    Location spineLoc = center.clone().add(Math.cos(angle) * 3, 10, Math.sin(angle) * 3);
                    BlockDisplayHandle spine = displayBuilder.spawnBlock(spineLoc, Material.AMETHYST_BLOCK);
                    spine.scale(0.2f, 0.2f, 0.8f).glow(180, 230, 255).interpolation(1, 0);
                    spineHandles.add(spine);
                    spawnedEntities.add(spine.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.5f, 1.3f);
            }
            // Spines travel outward
            if (ticksAlive > 20) {
                for (int s = 0; s < spineHandles.size(); s++) {
                    int salvoIdx = s / 6;
                    int spineIdx = s % 6;
                    int spineAge = ticksAlive - (20 + salvoIdx * 10);
                    if (spineAge > 0 && spineAge < 40) {
                        double dragonAngle = (salvoIdx + 1) * 0.4;
                        double angle = dragonAngle + (Math.PI * 2 / 6) * spineIdx;
                        float dist = 3 + spineAge * 0.8f;
                        spineHandles.get(s).entity().teleport(
                            center.clone().add(Math.cos(angle) * dist, 10 - spineAge * 0.15, Math.sin(angle) * dist));
                    }
                }
            }
            // Terrain spike impacts
            if (ticksAlive > 60 && ticksAlive % 20 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 6, 15.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalSpineOverdrive(plugin); }
    }

    // ================================================================
    // #81 — BRIMSTONE CROWN — Fire crown descends on lowest HP player
    // ================================================================
    public static class BrimstoneCrown extends BossAttack {
        private final List<BlockDisplayHandle> crownHandles = new ArrayList<>();
        private boolean crownDropped = false;

        public BrimstoneCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_brimstone_crown", AttackType.BOSS, 4), "dragon");
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
            // 8 crown columns at Y+15, 8-block radius
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location colLoc = center.clone().add(Math.cos(angle) * 8, 15, Math.sin(angle) * 8);
                BlockDisplayHandle col = displayBuilder.spawnBlock(colLoc, Material.MAGMA_BLOCK);
                col.scale(1.0f, 3.0f, 1.0f).glow(255, 80, 0).interpolation(3, 0);
                crownHandles.add(col);
                spawnedEntities.add(col.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Crown hovers and rotates (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !crownDropped) {
                float rot = ticksAlive * 0.07f;
                for (int i = 0; i < crownHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i + rot;
                    crownHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 8, 15, Math.sin(angle) * 8));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 15, 0), 6, 8.0);
                }
            }
            // Crown drops (tick 40)
            else if (ticksAlive == 40 && !crownDropped) {
                crownDropped = true;
            }
            // Crown descends (40-60 ticks)
            else if (crownDropped && ticksAlive < 60) {
                float dropY = 15 - (ticksAlive - 40) * 0.75f;
                for (int i = 0; i < crownHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    crownHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 8, dropY, Math.sin(angle) * 8));
                }
            }
            // Crown lands — impact (tick 60)
            else if (crownDropped && ticksAlive == 60) {
                for (BlockDisplayHandle col : crownHandles) {
                    Location landLoc = col.entity().getLocation();
                    triggerImpactDamage(landLoc);
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 25, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 0.9f, 0.9f);
            }
            // Burn patches aftermath (60-260 ticks = 10 seconds)
            if (crownDropped && ticksAlive > 60 && ticksAlive < 260 && ticksAlive % 15 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 4, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCrown(plugin); }
    }

    // ================================================================
    // #82 — VOID MAELSTROM: ASCENDANT — Upward-pulling vortex column
    // ================================================================
    public static class VoidMaelstromAscendant extends BossAttack {
        private final List<BlockDisplayHandle> vortexHandles = new ArrayList<>();
        private boolean vortexActive = false;
        private int vortexTick = 0;

        public VoidMaelstromAscendant(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_maelstrom_ascendant", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(760);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rising double helix telegraph
            for (int y = 0; y < 8; y++) {
                double helixAngle = y * Math.PI / 4;
                Location helixPt = center.clone().add(Math.cos(helixAngle) * 2, y * 4, Math.sin(helixAngle) * 2);
                BlockDisplayHandle helixSeg = displayBuilder.spawnBlock(helixPt, Material.PURPLE_STAINED_GLASS);
                helixSeg.scale(1.0f, 3.0f, 1.0f).glow(120, 0, 200).interpolation(3, 0);
                vortexHandles.add(helixSeg);
                spawnedEntities.add(helixSeg.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 20 && !vortexActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 15, 0), 10, 6.0);
                }
            }
            else if (ticksAlive == 20 && !vortexActive) {
                vortexActive = true;
                vortexTick = ticksAlive;
                // Full column visual
                BlockDisplayHandle column = displayBuilder.spawnBlock(
                    center.clone().add(0, 20, 0), Material.PURPLE_STAINED_GLASS);
                column.scale(6.0f, 40.0f, 6.0f).glow(100, 0, 200).interpolation(3, 0);
                vortexHandles.add(column);
                spawnedEntities.add(column.entity());
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.7f);
            }
            // Upward pull active (20-120 ticks = 5 seconds)
            else if (vortexActive && ticksAlive - vortexTick < 100) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 8, 6.0);
                }
                // Apex pulse damage every 20 ticks
                if (ticksAlive % 20 == 0) {
                    triggerImpactDamage(center.clone().add(0, 35, 0));
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
                }
            }
            // Column dissipation — downward compression (tick 120)
            else if (vortexActive && ticksAlive - vortexTick == 100) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 30, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidMaelstromAscendant(plugin); }
    }

    // ================================================================
    // #83 — THE CORRUPTION: TRIPLE SURGE — All 3 bosses simultaneously
    // ================================================================
    public static class TripleSurge extends BossAttack {
        private final List<BlockDisplayHandle> surgeHandles = new ArrayList<>();
        private boolean surgeActive = false;

        public TripleSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_triple_surge", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dragon body cycles 3 colors (0-20 ticks)
            if (ticksAlive < 20 && !surgeActive) {
                if (ticksAlive % 7 == 0) {
                    Material[] cycleMats = {Material.PURPLE_STAINED_GLASS, Material.MAGMA_BLOCK, Material.AMETHYST_BLOCK};
                    int cycleIdx = (ticksAlive / 7) % 3;
                    BlockDisplayHandle pulse = displayBuilder.spawnBlock(
                        center.clone().add(0, 10, 0), cycleMats[cycleIdx]);
                    pulse.scale(4.0f, 4.0f, 4.0f).glow(180, 0, 255).interpolation(2, 0);
                    surgeHandles.add(pulse);
                    spawnedEntities.add(pulse.entity());
                }
            }
            // Triple surge fires (tick 20)
            else if (ticksAlive == 20 && !surgeActive) {
                surgeActive = true;

                // TRACK 1 — Voidmaw beam
                BlockDisplayHandle voidBeam = displayBuilder.spawnBlock(
                    center.clone().add(10, 8, 0), Material.PURPLE_STAINED_GLASS);
                voidBeam.scale(20.0f, 2.0f, 3.0f).glow(100, 0, 200).interpolation(2, 0);
                spawnedEntities.add(voidBeam.entity());

                // TRACK 2 — DoG crystal spikes (6 in star pattern)
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 / 6) * i;
                    Location spikeLoc = center.clone().add(Math.cos(angle) * 10, 0.1, Math.sin(angle) * 10);
                    for (int y = 0; y < 4; y++) {
                        BlockDisplayHandle spike = displayBuilder.spawnBlock(
                            spikeLoc.clone().add(0, y, 0), Material.AMETHYST_BLOCK);
                        spike.scale(0.8f, 1.0f, 0.8f).glow(180, 230, 255).interpolation(1, 0);
                        spawnedEntities.add(spike.entity());
                    }
                    triggerImpactDamage(spikeLoc);
                }

                // TRACK 3 — Dweller brimstone column
                Location columnLoc = center.clone().add(-5, 0.1, -5);
                for (int y = 0; y < 10; y++) {
                    BlockDisplayHandle col = displayBuilder.spawnBlock(
                        columnLoc.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                    col.scale(3.0f, 1.0f, 3.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(col.entity());
                }
                triggerImpactDamage(columnLoc);

                DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 40, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 0.8f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 0.9f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.5f);
            }
            // Triple assault sustained (20-80 ticks = 3 seconds)
            else if (surgeActive && ticksAlive < 80) {
                if (ticksAlive % 8 == 0) {
                    triggerImpactDamage(center);
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 8, 10.0);
                }
            }
            // Aftermath — all 3 terrain types
            if (surgeActive && ticksAlive > 80 && ticksAlive % 15 == 0 && ticksAlive < 200) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 6, 12.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TripleSurge(plugin); }
    }

    // ================================================================
    // #84 — PHASE TRANSITION: THE EMPEROR REMEMBERS — Phase 2->3 shift
    // ================================================================
    public static class PhaseTransitionRemember extends BossAttack {
        private final List<BlockDisplayHandle> transitionHandles = new ArrayList<>();
        private boolean shockwaveFired = false;

        public PhaseTransitionRemember(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_phase_transition_remember", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(40.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 5 seconds of near silence — Dragon hovers, aura dims (0-100 ticks)
            if (ticksAlive < 100 && !shockwaveFired) {
                // Absorbed essences slowly drain away
                if (ticksAlive % 20 == 0) {
                    float dimFactor = 1.0f - (ticksAlive / 100.0f);
                    DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), (int)(6 * dimFactor), 4.0);
                }
                // Heartbeat at tick 60, 75, 85, 95 (accelerating)
                if (ticksAlive == 60 || ticksAlive == 75 || ticksAlive == 85 || ticksAlive == 95) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.6f);
                }
            }
            // FULL ARENA SHOCKWAVE — every particle type (tick 100)
            else if (ticksAlive == 100 && !shockwaveFired) {
                shockwaveFired = true;
                // Massive sphere burst
                for (int i = 0; i < 24; i++) {
                    double angle = (Math.PI * 2 / 24) * i;
                    for (int layer = 0; layer < 3; layer++) {
                        double yAngle = (layer - 1) * 0.5;
                        Location burstPt = center.clone().add(
                            Math.cos(angle) * 15, 20 + Math.sin(yAngle) * 10, Math.sin(angle) * 15);
                        Material[] burstMats = {Material.PURPLE_STAINED_GLASS, Material.MAGMA_BLOCK, Material.AMETHYST_BLOCK};
                        BlockDisplayHandle burst = displayBuilder.spawnBlock(burstPt, burstMats[layer]);
                        burst.scale(3.0f, 3.0f, 3.0f).glow(200, 100, 255).interpolation(1, 0);
                        transitionHandles.add(burst);
                        spawnedEntities.add(burst.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 50, 30.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
            }
            // Arena floor resets — clean end stone (100-120 ticks)
            else if (shockwaveFired && ticksAlive < 120) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 10, 20.0);
                }
            }
            // Phase 3 begins — Dragon descends, 4 sky streams active
            else if (shockwaveFired && ticksAlive == 120) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseTransitionRemember(plugin); }
    }
}
