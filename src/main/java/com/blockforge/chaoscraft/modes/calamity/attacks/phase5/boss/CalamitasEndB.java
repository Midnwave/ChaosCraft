package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Supreme Calamitas — Phase 4: "The End of Everything" (24%-0% HP)
 * Attacks #161-170
 *
 * Crown weaponization, trident-beam hybrids, and ground devastation.
 * Damage: 36-52 hearts. Arena 20x20.
 * Calamitas colors: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255), purple(128,0,255)
 *
 * NO status effects — damage only.
 */
public final class CalamitasEndB {

    private CalamitasEndB() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrownBurstVolley(plugin));
        registry.register(new TridentCascade(plugin));
        registry.register(new BrimstoneLanceCharge(plugin));
        registry.register(new EncirclingDeath(plugin));
        registry.register(new AscendingSpiralSlam(plugin));
        registry.register(new TridentBlizzard(plugin));
        registry.register(new CrimsonArtillery(plugin));
        registry.register(new SeismicTridentGrid(plugin));
        registry.register(new BrimstoneMirrorImage(plugin));
        registry.register(new VortexDrainField(plugin));
    }

    // ================================================================
    // #161 — CROWN BURST VOLLEY
    // Crown fires 60 tridents downward in 2-second burst
    // ================================================================
    public static class CrownBurstVolley extends BossAttack {
        private int tridentsFired = 0;

        public CrownBurstVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crown_burst_volley", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(140);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(3);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 30, 8.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 60 tridents from 12 crown points over 40 ticks (2 seconds)
            if (ticksAlive >= 5 && ticksAlive < 45 && ticksAlive % 1 == 0 && tridentsFired < 60) {
                tridentsFired++;
                int crownPoint = tridentsFired % 12;
                double angle = (2 * Math.PI / 12) * crownPoint;
                Location crownLoc = center.clone().add(Math.cos(angle) * 8, 20, Math.sin(angle) * 8);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(crownLoc, Material.PRISMARINE_BRICKS);
                trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(trident.entity());
                if (tridentsFired % 12 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 0.8f);
                }
            }

            // Animate falling
            for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                Location loc = spawnedEntities.get(i).getLocation();
                if (loc.getY() > center.getY() + 0.5) {
                    loc.subtract(0, 2.5, 0);
                    spawnedEntities.get(i).teleport(loc);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrownBurstVolley(plugin); }
    }

    // ================================================================
    // #162 — TRIDENT CASCADE
    // Layered waves: wave 1 ground-level, wave 2 mid, wave 3 high
    // ================================================================
    public static class TridentCascade extends BossAttack {
        private int wavesFired = 0;

        public TridentCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_cascade", AttackType.BOSS, 5), "calamitas");
            config.setDamage(44.0); // 22 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 15, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Wave 1 at Y+1 (tick 10)
            if (ticksAlive == 10 && wavesFired == 0) {
                wavesFired++;
                fireWave(center, 1.0);
            }
            // Wave 2 at Y+4 (tick 40)
            if (ticksAlive == 40 && wavesFired == 1) {
                wavesFired++;
                fireWave(center, 4.0);
            }
            // Wave 3 at Y+7 (tick 70)
            if (ticksAlive == 70 && wavesFired == 2) {
                wavesFired++;
                fireWave(center, 7.0);
            }
        }

        private void fireWave(Location center, double yOffset) {
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI / 12) * i;
                Location tLoc = center.clone().add(Math.cos(angle) * 10, yOffset, Math.sin(angle) * 10);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(trident.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.9f, 0.8f + (float)(yOffset * 0.05));
            DisplayBuilder.crimsonDust(center.clone().add(0, yOffset, 0), 20, 10.0);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentCascade(plugin); }
    }

    // ================================================================
    // #163 — BRIMSTONE LANCE CHARGE
    // Dash attack with trailing brimstone lance, leaves fire path
    // ================================================================
    public static class BrimstoneLanceCharge extends BossAttack {
        private boolean charging = false;
        private final List<BlockDisplayHandle> lanceHandles = new ArrayList<>();

        public BrimstoneLanceCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_lance_charge", AttackType.BOSS, 5), "calamitas");
            config.setDamage(52.0); // 26 hearts
            config.setDamageRadius(2.5);
            config.setDurationTicks(120);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(5);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 20, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 10, 2.0);
                }
                return;
            }

            // Charge (tick 10-60)
            if (ticksAlive == 10 && !charging) {
                charging = true;
                // Brimstone lance visual
                BlockDisplayHandle lance = displayBuilder.spawnBlock(center.clone().add(0, 3, 0), Material.MAGMA_BLOCK);
                lance.scale(0.3f, 0.3f, 4.0f).glow(255, 100, 0).interpolation(2, 0);
                lanceHandles.add(lance);
                spawnedEntities.add(lance.entity());
            }

            if (charging && ticksAlive > 10 && ticksAlive < 60) {
                // Trail particles (fire path)
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.crimsonDust(center, 8, 1.0);
                    DisplayBuilder.crimsonDust(center, 5, 1.5);
                    DisplayBuilder.crimsonDust(center, 3, 0.5);
                }
                if (ticksAlive % 4 == 0) {
                    // Leave fire trail block display
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(
                        center.clone().add(0, 0.03, 0), Material.MAGMA_BLOCK);
                    trail.scale(1.0f, 0.04f, 1.0f).glow(255, 80, 0).interpolation(3, 0);
                    spawnedEntities.add(trail.entity());
                }
                // Lance follows center
                if (!lanceHandles.isEmpty()) {
                    lanceHandles.get(0).entity().teleport(center.clone().add(0, 3, 0));
                }
            }

            // Impact stagger (tick 60)
            if (charging && ticksAlive == 60) {
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 1, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneLanceCharge(plugin); }
    }

    // ================================================================
    // #164 — ENCIRCLING DEATH
    // Ring of tridents orbits arena, shrinks and converges
    // ================================================================
    public static class EncirclingDeath extends BossAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private boolean active = false;

        public EncirclingDeath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_encircling_death", AttackType.BOSS, 5), "calamitas");
            config.setDamage(44.0); // 22 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 20, 12.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 5 && !active) {
                active = true;
                for (int i = 0; i < 24; i++) {
                    double angle = (2 * Math.PI / 24) * i;
                    Location tLoc = center.clone().add(Math.cos(angle) * 12, 3, Math.sin(angle) * 12);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    ringHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.6f);
            }

            // Orbit and shrink (5-120 ticks)
            if (active && ticksAlive > 5 && ticksAlive < 120) {
                int elapsed = ticksAlive - 5;
                float radius = 12.0f - (elapsed / 115.0f) * 10.0f; // 12 -> 2
                double spinSpeed = 0.08 + elapsed * 0.001;
                for (int i = 0; i < ringHandles.size(); i++) {
                    double angle = (2 * Math.PI / 24) * i + elapsed * spinSpeed;
                    Location orbLoc = center.clone().add(
                        Math.cos(angle) * radius, 3 + Math.sin(elapsed * 0.05) * 0.5, Math.sin(angle) * radius);
                    ringHandles.get(i).entity().teleport(orbLoc);
                }
                if (elapsed % 5 == 0) {
                    DisplayBuilder.crimsonDust(center, 8, radius);
                }
            }

            // Convergence impact (tick 120)
            if (active && ticksAlive == 120) {
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 2, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EncirclingDeath(plugin); }
    }

    // ================================================================
    // #165 — ASCENDING SPIRAL SLAM
    // Tridents spiral upward then slam back down en masse
    // ================================================================
    public static class AscendingSpiralSlam extends BossAttack {
        private final List<BlockDisplayHandle> spiralHandles = new ArrayList<>();
        private boolean ascending = true;
        private boolean descending = false;

        public AscendingSpiralSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_ascending_spiral_slam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(48.0); // 24 hearts
            config.setDamageRadius(2.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI / 20) * i;
                Location tLoc = center.clone().add(Math.cos(angle) * 3, 1, Math.sin(angle) * 3);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                spiralHandles.add(trident);
                spawnedEntities.add(trident.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 0.9f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ascend spiral (0-60 ticks = 3 seconds)
            if (ascending && ticksAlive < 60) {
                float yRise = ticksAlive * 0.6f;
                float radius = 3.0f + ticksAlive * 0.05f;
                for (int i = 0; i < spiralHandles.size(); i++) {
                    double angle = (2 * Math.PI / 20) * i + ticksAlive * 0.15;
                    Location spiralLoc = center.clone().add(
                        Math.cos(angle) * radius, 1 + yRise, Math.sin(angle) * radius);
                    spiralHandles.get(i).entity().teleport(spiralLoc);
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, yRise, 0), 10, radius);
                }
            }

            // Apex pause (60-80 ticks)
            if (ticksAlive == 60) {
                ascending = false;
                DisplayBuilder.crimsonDust(center.clone().add(0, 36, 0), 2, 3.0);
            }

            // Descend slam (80-95 ticks = fast)
            if (ticksAlive == 80) {
                descending = true;
            }
            if (descending && ticksAlive >= 80 && ticksAlive < 95) {
                int elapsed = ticksAlive - 80;
                float yDrop = elapsed * 2.4f;
                for (int i = 0; i < spiralHandles.size(); i++) {
                    double angle = (2 * Math.PI / 20) * i;
                    Location slamLoc = center.clone().add(
                        Math.cos(angle) * (3 + i * 0.3), 36 - yDrop, Math.sin(angle) * (3 + i * 0.3));
                    spiralHandles.get(i).entity().teleport(slamLoc);
                }
            }

            // Ground impact (tick 95)
            if (descending && ticksAlive == 95) {
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 2, 5.0);
                DisplayBuilder.crimsonDust(center, 50, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AscendingSpiralSlam(plugin); }
    }

    // ================================================================
    // #166 — TRIDENT BLIZZARD
    // Horizontal trident storm sweeping across the arena
    // ================================================================
    public static class TridentBlizzard extends BossAttack {
        private int tridentsFired = 0;

        public TridentBlizzard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_blizzard", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(3);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fire tridents from west wall, sweeping pattern, every 3 ticks for 120 ticks
            if (ticksAlive >= 5 && ticksAlive % 3 == 0 && tridentsFired < 40) {
                tridentsFired++;
                double yOffset = 1 + Math.random() * 6;
                double zOffset = (Math.random() - 0.5) * 20;
                Location tLoc = center.clone().add(-12, yOffset, zOffset);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(trident.entity());
                if (tridentsFired % 8 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 1.0f);
                }
            }

            // Animate all tridents flying east
            for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                Location loc = spawnedEntities.get(i).getLocation();
                loc.add(2.0, 0, 0); // Eastward
                spawnedEntities.get(i).teleport(loc);
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(loc, 2, 0.2);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentBlizzard(plugin); }
    }

    // ================================================================
    // #167 — CRIMSON ARTILLERY
    // Lobbed brimstone shells that explode into trident shrapnel
    // ================================================================
    public static class CrimsonArtillery extends BossAttack {
        private int shellsFired = 0;

        public CrimsonArtillery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crimson_artillery", AttackType.BOSS, 5), "calamitas");
            config.setDamage(44.0); // 22 hearts
            config.setDamageRadius(3.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(48.0); // 24 hearts
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 12, 0), 15, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fire 6 shells sequentially
            if (ticksAlive >= 10 && (ticksAlive - 10) % 20 == 0 && shellsFired < 6) {
                shellsFired++;
                Location fireLoc = center.clone().add(0, 12, 0);
                BlockDisplayHandle shell = displayBuilder.spawnBlock(fireLoc, Material.MAGMA_BLOCK);
                shell.scale(1.0f, 1.0f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                spawnedEntities.add(shell.entity());
                DisplayBuilder.playSound(fireLoc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
            }

            // Animate shells in parabolic arc
            for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                Location loc = spawnedEntities.get(i).getLocation();
                // Arc trajectory
                double xVel = (Math.random() - 0.5) * 1.5;
                double zVel = (Math.random() - 0.5) * 1.5;
                loc.add(xVel, -0.8, zVel);
                spawnedEntities.get(i).teleport(loc);
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(loc, 5, 0.5);
                    DisplayBuilder.crimsonDust(loc, 3, 0.3);
                }
                // Ground impact
                if (loc.getY() <= center.getY() + 0.5) {
                    triggerImpactDamage(loc);
                    DisplayBuilder.crimsonDust(loc, 1, 3.0);
                    DisplayBuilder.crimsonDust(loc, 20, 3.5);
                    DisplayBuilder.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.8f);
                    // Shrapnel tridents
                    for (int j = 0; j < 4; j++) {
                        double sAngle = (Math.PI / 2) * j;
                        Location shrapLoc = loc.clone().add(Math.cos(sAngle) * 3, 2, Math.sin(sAngle) * 3);
                        DisplayBuilder.crimsonDust(shrapLoc, 5, 0.3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonArtillery(plugin); }
    }

    // ================================================================
    // #168 — SEISMIC TRIDENT GRID
    // 4x4 grid of tridents slam into ground simultaneously
    // ================================================================
    public static class SeismicTridentGrid extends BossAttack {
        private final List<BlockDisplayHandle> gridHandles = new ArrayList<>();
        private boolean dropped = false;

        public SeismicTridentGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_seismic_grid", AttackType.BOSS, 5), "calamitas");
            config.setDamage(44.0); // 22 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(44.0);
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            // Grid shadow on floor
            for (int gx = 0; gx < 4; gx++) {
                for (int gz = 0; gz < 4; gz++) {
                    double x = (gx - 1.5) * 5;
                    double z = (gz - 1.5) * 5;
                    DisplayBuilder.crimsonDust(center.clone().add(x, 0.3, z), 5, 1.0);
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !dropped) {
                dropped = true;
                // 4x4 = 16 tridents at Y+30
                for (int gx = 0; gx < 4; gx++) {
                    for (int gz = 0; gz < 4; gz++) {
                        double x = (gx - 1.5) * 5;
                        double z = (gz - 1.5) * 5;
                        Location dropLoc = center.clone().add(x, 30, z);
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(dropLoc, Material.PRISMARINE_BRICKS);
                        trident.scale(0.15f, 0.15f, 1.2f).glow(200, 0, 50).interpolation(2, 0);
                        gridHandles.add(trident);
                        spawnedEntities.add(trident.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.7f);
            }

            // All slam down simultaneously (10-25 ticks)
            if (dropped && ticksAlive > 10 && ticksAlive < 25) {
                float fallSpeed = 2.5f;
                for (BlockDisplayHandle handle : gridHandles) {
                    if (handle.entity() == null || !handle.entity().isValid()) continue;
                    Location loc = handle.entity().getLocation();
                    loc.subtract(0, fallSpeed, 0);
                    handle.entity().teleport(loc);
                }
            }

            // Ground impact (tick 25)
            if (dropped && ticksAlive == 25) {
                for (int gx = 0; gx < 4; gx++) {
                    for (int gz = 0; gz < 4; gz++) {
                        double x = (gx - 1.5) * 5;
                        double z = (gz - 1.5) * 5;
                        Location impactLoc = center.clone().add(x, 0.1, z);
                        triggerImpactDamage(impactLoc);
                    }
                }
                DisplayBuilder.crimsonDust(center, 3, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SeismicTridentGrid(plugin); }
    }

    // ================================================================
    // #169 — BRIMSTONE MIRROR IMAGE
    // Creates a perfect mirror of her attack pattern on the opposite side
    // ================================================================
    public static class BrimstoneMirrorImage extends BossAttack {
        private boolean mirrored = false;

        public BrimstoneMirrorImage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_mirror_image", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.purpleDust(center, 15, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !mirrored) {
                mirrored = true;
                // Two mirror positions fire simultaneously
                Location pos1 = center.clone().add(6, 8, 0);
                Location pos2 = center.clone().add(-6, 8, 0);

                // Each fires 8 tridents in a fan
                for (int side = 0; side < 2; side++) {
                    Location origin = side == 0 ? pos1 : pos2;
                    for (int j = 0; j < 8; j++) {
                        double angle = -Math.PI / 4 + (Math.PI / 2) * (j / 7.0);
                        if (side == 1) angle = Math.PI - angle; // Mirror
                        Location tLoc = origin.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2);
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                        trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                        spawnedEntities.add(trident.entity());
                    }
                    // Mirror image visual
                    BlockDisplayHandle image = displayBuilder.spawnBlock(origin, Material.CRIMSON_HYPHAE);
                    image.scale(0.8f, 1.8f, 0.4f).glow(200, 0, 50).interpolation(3, 0);
                    spawnedEntities.add(image.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.9f);
            }

            // Mirror particles
            if (mirrored && ticksAlive > 10 && ticksAlive < 80) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(6, 8, 0), 5, 1.0);
                    DisplayBuilder.crimsonDust(center.clone().add(-6, 8, 0), 5, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneMirrorImage(plugin); }
    }

    // ================================================================
    // #170 — VORTEX DRAIN FIELD
    // Pull + sustained damage zone + trident bombardment
    // ================================================================
    public static class VortexDrainField extends BossAttack {
        private boolean fieldActive = false;

        public VortexDrainField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_vortex_drain", AttackType.BOSS, 5), "calamitas");
            config.setDamage(36.0); // 18 hearts per second
            config.setDamageRadius(6.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.purpleDust(center, 40, 6.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            if (ticksAlive == 10 && !fieldActive) {
                fieldActive = true;
                // Drain field visual
                BlockDisplayHandle field = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0), Material.CRYING_OBSIDIAN);
                field.scale(12.0f, 0.06f, 12.0f).glow(128, 0, 255).interpolation(5, 0);
                spawnedEntities.add(field.entity());
            }

            // Active drain (10-180 ticks = 8.5 seconds)
            if (fieldActive && ticksAlive > 10 && ticksAlive < 180) {
                // Pull effect
                World w = center.getWorld();
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (dist <= 12.0 && dist > 1.0) {
                        Vector pull = center.toVector().subtract(player.getLocation().toVector()).normalize();
                        player.setVelocity(player.getVelocity().add(pull.multiply(0.08)));
                    }
                }

                // Field particles
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.purpleDust(center, 20, 6.0);
                    DisplayBuilder.purpleDust(center, 10, 6.0);
                }

                // Trident bombardment every 25 ticks
                if ((ticksAlive - 10) % 25 == 0) {
                    Location tLoc = center.clone().add((Math.random() - 0.5) * 10, 20, (Math.random() - 0.5) * 10);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(trident.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VortexDrainField(plugin); }
    }
}
