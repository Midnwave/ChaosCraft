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
 * Attacks #1-10
 * NO status effects — damage only.
 */
public final class DragonPhase1A {

    private DragonPhase1A() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CinderProclamation(plugin));
        registry.register(new OrbitalHarassment(plugin));
        registry.register(new VoidExhale(plugin));
        registry.register(new GravitySpike(plugin));
        registry.register(new WingTempest(plugin));
        registry.register(new VoidTendrilReach(plugin));
        registry.register(new ResonancePulse(plugin));
        registry.register(new DiveStrafingRun(plugin));
        registry.register(new BladeOrbitCompression(plugin));
        registry.register(new VoidHowl(plugin));
    }

    // ================================================================
    // 1. CINDER PROCLAMATION — Oversized fireball with AoE detonation
    // ================================================================
    public static class CinderProclamation extends BossAttack {
        private final List<BlockDisplayHandle> cinderHandles = new ArrayList<>();
        private boolean fireballLaunched = false;
        private int launchTick = 0;

        public CinderProclamation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_cinder_proclamation", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon halts — swirling violet column from jaw
            BlockDisplayHandle jawCore = displayBuilder.spawnBlock(
                center.clone().add(0, 5, 0), Material.MAGENTA_GLAZED_TERRACOTTA);
            jawCore.scale(1.2f, 1.2f, 1.2f).glow(120, 0, 180).interpolation(3, 0);
            cinderHandles.add(jawCore);
            spawnedEntities.add(jawCore.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — jaw column charges (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !fireballLaunched) {
                float scale = 1.2f + (ticksAlive / 30.0f) * 0.8f;
                cinderHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-scale / 2, 5 - scale / 2, -scale / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 8, 2.0);
                }
            }
            // Fireball launches (tick 30)
            else if (ticksAlive == 30 && !fireballLaunched) {
                fireballLaunched = true;
                launchTick = ticksAlive;
                // Fireball projectile
                BlockDisplayHandle fireball = displayBuilder.spawnBlock(
                    center.clone().add(0, 4, 2), Material.MAGMA_BLOCK);
                fireball.scale(1.5f, 1.5f, 1.5f).glow(180, 60, 0).interpolation(2, 0);
                cinderHandles.add(fireball);
                spawnedEntities.add(fireball.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.9f);
            }
            // Fireball travels forward (30-50 ticks)
            else if (fireballLaunched && ticksAlive - launchTick < 20) {
                float dist = (ticksAlive - launchTick) * 1.2f;
                if (cinderHandles.size() > 1) {
                    cinderHandles.get(1).entity().teleport(
                        center.clone().add(0, 4 - dist * 0.15, 2 + dist));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 2 + dist), 6, 1.5);
                }
            }
            // Impact detonation (tick 50) — 7x7 ground carpet
            else if (fireballLaunched && ticksAlive - launchTick == 20) {
                Location impactLoc = center.clone().add(0, 0.1, 26);
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        BlockDisplayHandle carpet = displayBuilder.spawnBlock(
                            impactLoc.clone().add(dx, 0.05, dz), Material.PURPLE_STAINED_GLASS);
                        carpet.scale(0.9f, 0.06f, 0.9f).glow(130, 0, 160).interpolation(2, 0);
                        spawnedEntities.add(carpet.entity());
                    }
                }
                triggerImpactDamage(impactLoc);
                DisplayBuilder.cyanDust(impactLoc, 30, 5.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.9f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 0.5f);
            }
            // Ground carpet pulses outward (50-110 ticks = 60 ticks of AoE)
            else if (fireballLaunched && ticksAlive - launchTick > 20 && ticksAlive - launchTick < 80) {
                if (ticksAlive % 8 == 0) {
                    Location impactLoc = center.clone().add(0, 0.2, 26);
                    float radius = ((ticksAlive - launchTick - 20) / 60.0f) * 4.0f;
                    DisplayBuilder.cyanDust(impactLoc, 10, radius + 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CinderProclamation(plugin); }
    }

    // ================================================================
    // 2. ORBITAL HARASSMENT — Two blade projectiles targeting nearest players
    // ================================================================
    public static class OrbitalHarassment extends BossAttack {
        private final List<BlockDisplayHandle> bladeHandles = new ArrayList<>();
        private boolean bladesFired = false;
        private int fireTick = 0;

        public OrbitalHarassment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_orbital_harassment", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(250);
            config.setCooldownTicks(160);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Two orbiting swords peel away and spin around the Dragon's neck
            for (int i = 0; i < 2; i++) {
                double angle = Math.PI * i;
                Location bladeLoc = center.clone().add(Math.cos(angle) * 2, 5, Math.sin(angle) * 2);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                blade.scale(0.15f, 0.15f, 1.2f).glow(255, 255, 255).interpolation(2, 0);
                bladeHandles.add(blade);
                spawnedEntities.add(blade.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spinning telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !bladesFired) {
                double spinSpeed = 0.3 + (ticksAlive / 40.0) * 0.5;
                for (int i = 0; i < 2; i++) {
                    double angle = Math.PI * i + ticksAlive * spinSpeed;
                    Location spinLoc = center.clone().add(
                        Math.cos(angle) * 1.5, 5, Math.sin(angle) * 1.5);
                    bladeHandles.get(i).entity().teleport(spinLoc);
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 4, 2.0);
                }
            }
            // Blades fire at players (tick 40)
            else if (ticksAlive == 40 && !bladesFired) {
                bladesFired = true;
                fireTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_HIT, 1.0f, 0.6f);
            }
            // Blades travel outward (40-70 ticks)
            else if (bladesFired && ticksAlive - fireTick < 30) {
                float dist = (ticksAlive - fireTick) * 0.8f;
                for (int i = 0; i < 2; i++) {
                    double spreadAngle = Math.PI * i + Math.toRadians(15) * (i == 0 ? 1 : -1);
                    Location travelLoc = center.clone().add(
                        Math.cos(spreadAngle) * dist, 4, Math.sin(spreadAngle) * dist);
                    bladeHandles.get(i).entity().teleport(travelLoc);
                }
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle blade : bladeHandles) {
                        DisplayBuilder.cyanDust(blade.entity().getLocation(), 3, 0.5);
                    }
                }
            }
            // Blades arc back to orbit (70-130 ticks = 3 seconds return)
            else if (bladesFired && ticksAlive - fireTick >= 30 && ticksAlive - fireTick < 90) {
                float progress = (ticksAlive - fireTick - 30) / 60.0f;
                for (int i = 0; i < 2; i++) {
                    double angle = Math.PI * i;
                    float returnDist = 24.0f * (1.0f - progress);
                    Location returnLoc = center.clone().add(
                        Math.cos(angle) * (2 + returnDist * (1 - progress)),
                        4 + progress, Math.sin(angle) * (2 + returnDist * (1 - progress)));
                    bladeHandles.get(i).entity().teleport(returnLoc);
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OrbitalHarassment(plugin); }
    }

    // ================================================================
    // 3. VOID EXHALE — Wide breath cone with ground carpet
    // ================================================================
    public static class VoidExhale extends BossAttack {
        private final List<BlockDisplayHandle> breathHandles = new ArrayList<>();
        private boolean breathActive = false;
        private int breathStartTick = 0;

        public VoidExhale(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_exhale", AttackType.BOSS, 4), "dragon");
            config.setDamage(12.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon dips head low — ring of PORTAL particles expanding from neck
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location ringPt = center.clone().add(Math.cos(angle) * 3, 3, Math.sin(angle) * 3);
                BlockDisplayHandle ringSeg = displayBuilder.spawnBlock(ringPt, Material.PURPLE_STAINED_GLASS);
                ringSeg.scale(1.0f, 0.1f, 1.0f).glow(0, 160, 140).interpolation(3, 0);
                breathHandles.add(ringSeg);
                spawnedEntities.add(ringSeg.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph ring expands (0-36 ticks = 1.8 seconds)
            if (ticksAlive < 36 && !breathActive) {
                float radius = 3.0f + (ticksAlive / 36.0f) * 7.0f;
                for (int i = 0; i < breathHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    breathHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 3, Math.sin(angle) * radius));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 12, radius);
                }
            }
            // Breath stream activates (tick 36) — 8-block wide cone, 18 blocks long
            else if (ticksAlive == 36 && !breathActive) {
                breathActive = true;
                breathStartTick = ticksAlive;
                // Breath cone segments
                for (int seg = 0; seg < 9; seg++) {
                    float width = 1.0f + seg * 0.8f;
                    Location segLoc = center.clone().add(0, 2, 2 + seg * 2);
                    BlockDisplayHandle breathSeg = displayBuilder.spawnBlock(segLoc, Material.PURPLE_STAINED_GLASS);
                    breathSeg.scale(width, 2.0f, 1.5f).glow(100, 0, 180).interpolation(2, 0);
                    breathHandles.add(breathSeg);
                    spawnedEntities.add(breathSeg.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.4f);
            }
            // Breath sustained (36-116 ticks = 4 seconds)
            else if (breathActive && ticksAlive - breathStartTick < 80) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 10), 12, 6.0);
                    triggerImpactDamage(center.clone().add(0, 2, 10));
                }
            }
            // Residual cloud at far end (116-216 ticks = 5 seconds)
            else if (breathActive && ticksAlive - breathStartTick >= 80 && ticksAlive - breathStartTick < 180) {
                if (ticksAlive % 12 == 0) {
                    Location residualLoc = center.clone().add(0, 1.5, 18);
                    DisplayBuilder.cyanDust(residualLoc, 6, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidExhale(plugin); }
    }

    // ================================================================
    // 4. GRAVITY SPIKE — Launches players upward with void column
    // ================================================================
    public static class GravitySpike extends BossAttack {
        private final List<BlockDisplayHandle> spikeHandles = new ArrayList<>();
        private boolean spikeFired = false;

        public GravitySpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_gravity_spike", AttackType.BOSS, 4), "dragon");
            config.setDamage(12.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Gold-white column shoots up from random player position
            BlockDisplayHandle pillar = displayBuilder.spawnBlock(
                center.clone().add(0, 0.1, 0), Material.GLOWSTONE);
            pillar.scale(0.5f, 30.0f, 0.5f).glow(255, 240, 180).interpolation(3, 0);
            spikeHandles.add(pillar);
            spawnedEntities.add(pillar.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph column visible (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !spikeFired) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 15, 0), 8, 1.0);
                }
            }
            // Spike fires — upward velocity impulse (tick 30)
            else if (ticksAlive == 30 && !spikeFired) {
                spikeFired = true;
                // Ground impact ring
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    Location ringPt = center.clone().add(Math.cos(angle) * 6, 0.1, Math.sin(angle) * 6);
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.SOUL_LANTERN);
                    ring.scale(0.8f, 0.08f, 0.8f).glow(100, 180, 255).interpolation(2, 0);
                    spikeHandles.add(ring);
                    spawnedEntities.add(ring.entity());
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center, 25, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
            }
            // Ground afterglow ring persists (30-110 ticks = 4 seconds)
            else if (spikeFired && ticksAlive < 110) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 8, 6.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravitySpike(plugin); }
    }

    // ================================================================
    // 5. WING TEMPEST — 180-degree semicircle wing sweep
    // ================================================================
    public static class WingTempest extends BossAttack {
        private final List<BlockDisplayHandle> wingHandles = new ArrayList<>();
        private boolean sweepActive = false;
        private int sweepTick = 0;

        public WingTempest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_wing_tempest", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Wing-tips glow — CRIT streams from leading edges
            BlockDisplayHandle leftWing = displayBuilder.spawnBlock(
                center.clone().add(-6, 5, 0), Material.END_STONE);
            leftWing.scale(3.0f, 0.3f, 1.0f).glow(255, 180, 0).interpolation(3, 0);
            wingHandles.add(leftWing);
            spawnedEntities.add(leftWing.entity());

            BlockDisplayHandle rightWing = displayBuilder.spawnBlock(
                center.clone().add(6, 5, 0), Material.END_STONE);
            rightWing.scale(3.0f, 0.3f, 1.0f).glow(255, 180, 0).interpolation(3, 0);
            wingHandles.add(rightWing);
            spawnedEntities.add(rightWing.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — wings extended with crit trails (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !sweepActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-6, 5, 0), 4, 1.0);
                    DisplayBuilder.cyanDust(center.clone().add(6, 5, 0), 4, 1.0);
                }
            }
            // Sweep executes (tick 40) — 180-degree arc
            else if (ticksAlive == 40 && !sweepActive) {
                sweepActive = true;
                sweepTick = ticksAlive;
                // Fan of block display wedges representing the sweep arc
                for (int arc = 0; arc < 9; arc++) {
                    double sweepAngle = -Math.PI / 2 + (Math.PI / 9) * arc;
                    Location arcPt = center.clone().add(
                        Math.cos(sweepAngle) * 8, 4, Math.sin(sweepAngle) * 8);
                    BlockDisplayHandle arcBlock = displayBuilder.spawnBlock(arcPt, Material.END_STONE);
                    arcBlock.scale(2.0f, 0.4f, 2.0f).glow(255, 200, 50).interpolation(1, 0);
                    wingHandles.add(arcBlock);
                    spawnedEntities.add(arcBlock.entity());
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 30, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 1.2f);
            }
            // Sweep dissipates (40-60 ticks = 1 second)
            else if (sweepActive && ticksAlive - sweepTick < 20) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 8, 10.0);
                }
            }
            // Aftermath wind drift (60-100 ticks = 2 seconds)
            else if (sweepActive && ticksAlive - sweepTick >= 20 && ticksAlive - sweepTick < 60) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 4, 8.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WingTempest(plugin); }
    }

    // ================================================================
    // 6. VOID TENDRIL REACH — Tendril projectile that pulls players
    // ================================================================
    public static class VoidTendrilReach extends BossAttack {
        private final List<BlockDisplayHandle> tendrilHandles = new ArrayList<>();
        private boolean tendrilFired = false;
        private int fireTick = 0;

        public VoidTendrilReach(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_tendril_reach", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Thin PORTAL particle line extends from Dragon's claw
            BlockDisplayHandle clawBase = displayBuilder.spawnBlock(
                center.clone().add(2, 4, 0), Material.OBSIDIAN);
            clawBase.scale(0.3f, 0.3f, 0.3f).glow(80, 0, 150).interpolation(2, 0);
            tendrilHandles.add(clawBase);
            spawnedEntities.add(clawBase.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Tendril reaches outward telegraph (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !tendrilFired) {
                float reachDist = (ticksAlive / 30.0f) * 15.0f;
                // Growing line of segments toward player
                if (ticksAlive % 6 == 0) {
                    Location segLoc = center.clone().add(2 + reachDist * 0.3, 3, reachDist * 0.7);
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.OBSIDIAN);
                    seg.scale(0.15f, 0.15f, 0.6f).glow(80, 0, 150).interpolation(1, 0);
                    tendrilHandles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(2, 3, reachDist * 0.7), 4, 0.5);
                }
            }
            // Tendril fires (tick 30)
            else if (ticksAlive == 30 && !tendrilFired) {
                tendrilFired = true;
                fireTick = ticksAlive;
                // Full tendril line
                for (int i = 0; i < 15; i++) {
                    Location segLoc = center.clone().add(2, 3, i * 1.3);
                    BlockDisplayHandle seg = displayBuilder.spawnBlock(segLoc, Material.OBSIDIAN);
                    seg.scale(0.2f, 0.2f, 1.0f).glow(50, 0, 100).interpolation(1, 0);
                    tendrilHandles.add(seg);
                    spawnedEntities.add(seg.entity());
                }
                triggerImpactDamage(center.clone().add(2, 3, 15));
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_HURT, 1.2f, 0.5f);
            }
            // Pull phase (30-60 ticks = 1.5 seconds)
            else if (tendrilFired && ticksAlive - fireTick < 30) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(2, 3, 10), 6, 2.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_HURT, 0.8f, 0.7f);
                }
            }
            // Tendril snaps — backward knockback (tick 60)
            else if (tendrilFired && ticksAlive - fireTick == 30) {
                DisplayBuilder.cyanDust(center.clone().add(2, 3, 5), 15, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_HURT, 1.2f, 0.5f);
            }
            // Ground trail persists (60-120 ticks = 3 seconds)
            else if (tendrilFired && ticksAlive - fireTick > 30 && ticksAlive - fireTick < 90) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(2, 0.2, 8), 4, 8.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTendrilReach(plugin); }
    }

    // ================================================================
    // 7. RESONANCE PULSE — Crystal structures fire radial projectile bursts
    // ================================================================
    public static class ResonancePulse extends BossAttack {
        private final List<BlockDisplayHandle> crystalHandles = new ArrayList<>();
        private boolean pulseFired = false;
        private int pulseTick = 0;

        public ResonancePulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_resonance_pulse", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Crystal clusters vibrate — place 4 crystal node displays
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 / 4) * i;
                Location crystalLoc = center.clone().add(Math.cos(angle) * 12, 1.5, Math.sin(angle) * 12);
                BlockDisplayHandle crystal = displayBuilder.spawnBlock(crystalLoc, Material.AMETHYST_BLOCK);
                crystal.scale(1.0f, 1.5f, 1.0f).glow(200, 100, 255).interpolation(2, 0);
                crystalHandles.add(crystal);
                spawnedEntities.add(crystal.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 2.0f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Crystal vibration telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !pulseFired) {
                if (ticksAlive % 4 == 0) {
                    for (BlockDisplayHandle crystal : crystalHandles) {
                        Location cLoc = crystal.entity().getLocation();
                        float jitter = (float) (Math.random() - 0.5) * 0.3f;
                        crystal.entity().teleport(cLoc.clone().add(jitter, 0, jitter));
                    }
                    DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 6, 12.0);
                }
            }
            // Crystals fire 8 projectiles each (tick 40)
            else if (ticksAlive == 40 && !pulseFired) {
                pulseFired = true;
                pulseTick = ticksAlive;
                // Each crystal fires 8 radial projectile shards
                for (BlockDisplayHandle crystal : crystalHandles) {
                    Location cLoc = crystal.entity().getLocation();
                    for (int j = 0; j < 8; j++) {
                        double projAngle = (Math.PI * 2 / 8) * j;
                        Location projLoc = cLoc.clone().add(Math.cos(projAngle) * 1.5, 0, Math.sin(projAngle) * 1.5);
                        BlockDisplayHandle proj = displayBuilder.spawnBlock(projLoc, Material.AMETHYST_BLOCK);
                        proj.scale(0.2f, 0.2f, 0.2f).glow(210, 120, 255).interpolation(1, 0);
                        crystalHandles.add(proj);
                        spawnedEntities.add(proj.entity());
                    }
                    triggerImpactDamage(cLoc);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.6f);
            }
            // Projectiles travel outward (40-70 ticks)
            else if (pulseFired && ticksAlive - pulseTick < 30) {
                float dist = (ticksAlive - pulseTick) * 0.5f;
                // Move projectile displays outward
                int projIdx = 4; // after the 4 crystal bases
                for (int c = 0; c < 4; c++) {
                    Location cLoc = crystalHandles.get(c).entity().getLocation();
                    for (int j = 0; j < 8; j++) {
                        if (projIdx < crystalHandles.size()) {
                            double projAngle = (Math.PI * 2 / 8) * j;
                            crystalHandles.get(projIdx).entity().teleport(
                                cLoc.clone().add(Math.cos(projAngle) * (1.5 + dist),
                                    0, Math.sin(projAngle) * (1.5 + dist)));
                            projIdx++;
                        }
                    }
                }
            }
            // Afterglow cooldown glow on crystals (70-170 ticks = 5 seconds)
            else if (pulseFired && ticksAlive - pulseTick >= 30 && ticksAlive - pulseTick < 130) {
                if (ticksAlive % 12 == 0) {
                    for (int c = 0; c < 4 && c < crystalHandles.size(); c++) {
                        DisplayBuilder.cyanDust(crystalHandles.get(c).entity().getLocation(), 4, 1.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ResonancePulse(plugin); }
    }

    // ================================================================
    // 8. DIVE STRAFING RUN — High-speed aerial dive through island
    // ================================================================
    public static class DiveStrafingRun extends BossAttack {
        private final List<BlockDisplayHandle> diveHandles = new ArrayList<>();
        private boolean diveActive = false;
        private int diveTick = 0;

        public DiveStrafingRun(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_dive_strafing_run", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon climbs to 40 blocks — apex position
            BlockDisplayHandle apexGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 40, 0), Material.YELLOW_STAINED_GLASS);
            apexGlow.scale(3.0f, 3.0f, 3.0f).glow(255, 255, 200).interpolation(4, 0);
            diveHandles.add(apexGlow);
            spawnedEntities.add(apexGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Apex telegraph — silhouetted at height (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !diveActive) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 40, 0), 8, 3.0);
                }
            }
            // Dive begins (tick 30)
            else if (ticksAlive == 30 && !diveActive) {
                diveActive = true;
                diveTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.8f);
            }
            // Dive descent (30-60 ticks = 1.5 seconds)
            else if (diveActive && ticksAlive - diveTick < 30) {
                float progress = (ticksAlive - diveTick) / 30.0f;
                float height = 40 * (1.0f - progress);
                diveHandles.get(0).entity().teleport(center.clone().add(0, height, progress * 30));
                // Wing ribbon trails
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-4, height, progress * 30), 4, 1.0);
                    DisplayBuilder.cyanDust(center.clone().add(4, height, progress * 30), 4, 1.0);
                }
            }
            // Impact with island surface (tick 60)
            else if (diveActive && ticksAlive - diveTick == 30) {
                Location impactLoc = center.clone().add(0, 0.5, 30);
                // Scorch path along dive trajectory
                for (int i = 0; i < 15; i++) {
                    Location scorchPt = center.clone().add(0, 0.1, i * 2);
                    BlockDisplayHandle scorch = displayBuilder.spawnBlock(scorchPt, Material.SOUL_SAND);
                    scorch.scale(1.5f, 0.08f, 1.5f).glow(100, 180, 255).interpolation(2, 0);
                    diveHandles.add(scorch);
                    spawnedEntities.add(scorch.entity());
                }
                triggerImpactDamage(impactLoc);
                DisplayBuilder.cyanDust(impactLoc, 25, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            }
            // Scorch aftermath (60-140 ticks = 4 seconds)
            else if (diveActive && ticksAlive - diveTick > 30 && ticksAlive - diveTick < 110) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 15), 6, 15.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DiveStrafingRun(plugin); }
    }

    // ================================================================
    // 9. BLADE ORBIT COMPRESSION — Orbit expansion and return
    // ================================================================
    public static class BladeOrbitCompression extends BossAttack {
        private final List<BlockDisplayHandle> bladeHandles = new ArrayList<>();
        private boolean orbitsExpanded = false;
        private int expandTick = 0;

        public BladeOrbitCompression(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_blade_orbit_compression", AttackType.BOSS, 4), "dragon");
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Orbit array accelerates — 8 blades in ring at close distance
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location bladeLoc = center.clone().add(Math.cos(angle) * 3, 5, Math.sin(angle) * 3);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                blade.scale(0.15f, 0.15f, 1.0f).glow(255, 255, 255).interpolation(2, 0);
                bladeHandles.add(blade);
                spawnedEntities.add(blade.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.5f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Orbit acceleration telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !orbitsExpanded) {
                double speed = 0.2 + (ticksAlive / 40.0) * 0.6;
                float radius = 3.0f - (ticksAlive / 40.0f) * 1.0f; // Narrows
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i + ticksAlive * speed;
                    bladeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 5, Math.sin(angle) * radius));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 8, radius);
                }
            }
            // Blades explode outward (tick 40)
            else if (ticksAlive == 40 && !orbitsExpanded) {
                orbitsExpanded = true;
                expandTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 1.2f, 0.5f);
            }
            // Blades travel to 20-block endpoints (40-60 ticks)
            else if (orbitsExpanded && ticksAlive - expandTick < 20) {
                float dist = 2.0f + (ticksAlive - expandTick) * 0.9f;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    bladeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * dist, 5, Math.sin(angle) * dist));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 6, dist);
                }
            }
            // Blades hover at endpoints — PORTAL drip (60-120 ticks = 3 seconds)
            else if (orbitsExpanded && ticksAlive - expandTick >= 20 && ticksAlive - expandTick < 80) {
                if (ticksAlive % 8 == 0) {
                    for (BlockDisplayHandle blade : bladeHandles) {
                        DisplayBuilder.cyanDust(blade.entity().getLocation().add(0, -1, 0), 3, 0.3);
                    }
                }
            }
            // Blades return to orbit (120-160 ticks = homing return)
            else if (orbitsExpanded && ticksAlive - expandTick >= 80 && ticksAlive - expandTick < 120) {
                float progress = (ticksAlive - expandTick - 80) / 40.0f;
                float returnDist = 20.0f * (1.0f - progress) + 3.0f * progress;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i + (ticksAlive - expandTick - 80) * 0.15;
                    bladeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * returnDist, 5, Math.sin(angle) * returnDist));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BladeOrbitCompression(plugin); }
    }

    // ================================================================
    // 10. VOID HOWL — Shockwave roar ring expanding outward
    // ================================================================
    public static class VoidHowl extends BossAttack {
        private final List<BlockDisplayHandle> howlHandles = new ArrayList<>();
        private boolean howlFired = false;
        private int howlTick = 0;

        public VoidHowl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_howl", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon jaw opens wide — deep purple particles stream upward
            BlockDisplayHandle jawGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 6, 1), Material.PURPLE_STAINED_GLASS);
            jawGlow.scale(0.8f, 0.8f, 0.8f).glow(90, 0, 160).interpolation(3, 0);
            howlHandles.add(jawGlow);
            spawnedEntities.add(jawGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Jaw windup telegraph (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !howlFired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 6, 1), 6, 1.0);
                }
            }
            // Howl fires (tick 30) — expanding shockwave ring
            else if (ticksAlive == 30 && !howlFired) {
                howlFired = true;
                howlTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_DEATH, 0.4f, 0.6f);
            }
            // Shockwave ring expands outward (30-72 ticks = ~2 seconds to reach 25 blocks)
            else if (howlFired && ticksAlive - howlTick < 42) {
                float radius = (ticksAlive - howlTick) * 0.6f;
                // Ring of block displays at current radius
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (Math.PI * 2 / 12) * i;
                        Location ringPt = center.clone().add(
                            Math.cos(angle) * radius, 4, Math.sin(angle) * radius);
                        BlockDisplayHandle ringSeg = displayBuilder.spawnBlock(ringPt, Material.PURPLE_STAINED_GLASS);
                        ringSeg.scale(1.5f, 2.0f, 1.5f).glow(90, 0, 160).interpolation(1, 0);
                        howlHandles.add(ringSeg);
                        spawnedEntities.add(ringSeg.entity());
                    }
                    triggerImpactDamage(center.clone().add(radius, 4, 0));
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 10, radius);
                }
            }
            // Shockwave ring settles on ground (72-132 ticks = 3 seconds aftermath)
            else if (howlFired && ticksAlive - howlTick >= 42 && ticksAlive - howlTick < 102) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 8, 25.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidHowl(plugin); }
    }
}
