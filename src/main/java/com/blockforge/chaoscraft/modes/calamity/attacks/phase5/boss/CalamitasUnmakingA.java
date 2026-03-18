package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Supreme Calamitas — Phase 3: "The Unmaking" (49%-25% HP)
 * Attacks #101-110
 *
 * The elegance is gone. The control is gone. She is everywhere.
 * Telegraph time: 0.5 seconds. Some attacks have NO telegraph.
 * Calamitas colors: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255), purple(128,0,255)
 *
 * NO status effects — damage only.
 */
public final class CalamitasUnmakingA {

    private CalamitasUnmakingA() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SixteenTridentSpinningRing(plugin));
        registry.register(new TridentRainfall(plugin));
        registry.register(new LogarithmicTridentSpiral(plugin));
        registry.register(new CardinalCrossVolley(plugin));
        registry.register(new DiagonalCrossVolley(plugin));
        registry.register(new TridentHailstorm(plugin));
        registry.register(new BrimstoneSkullSplitter(plugin));
        registry.register(new TridentBrimstoneEcho(plugin));
        registry.register(new BrimstoneCage(plugin));
        registry.register(new SynchronizedSpiralBurst(plugin));
    }

    // ================================================================
    // #101 — SIXTEEN-TRIDENT SPINNING RING
    // 16 tridents fire radially outward from her body in a perfect ring
    // ================================================================
    public static class SixteenTridentSpinningRing extends BossAttack {
        private final List<BlockDisplayHandle> tridentHandles = new ArrayList<>();
        private boolean launched = false;
        private int launchTick = 0;

        public SixteenTridentSpinningRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_sixteen_trident_ring", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); // 14 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(80);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ascend to Y+30, spawn crimson ring telegraph
            Location elevated = center.clone().add(0, 30, 0);
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI / 16) * i;
                Location tridentLoc = elevated.clone().add(
                    Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(tridentLoc, Material.PRISMARINE_BRICKS);
                trident.scale(0.15f, 0.15f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                tridentHandles.add(trident);
                spawnedEntities.add(trident.entity());
            }
            DisplayBuilder.playSound(elevated, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location elevated = center.clone().add(0, 30, 0);

            // Telegraph spin (0-10 ticks = 0.5s)
            if (ticksAlive < 10 && !launched) {
                double spinSpeed = 0.4 + (ticksAlive / 10.0) * 0.6;
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI / 16) * i + ticksAlive * spinSpeed;
                    Location spinLoc = elevated.clone().add(
                        Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                    tridentHandles.get(i).entity().teleport(spinLoc);
                }
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(elevated, 80, 3.0);
                }
            }
            // Launch all 16 radially outward (tick 10)
            else if (ticksAlive == 10 && !launched) {
                launched = true;
                launchTick = ticksAlive;
                for (int i = 0; i < 16; i++) {
                    DisplayBuilder.playSound(elevated, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 1.0f + (i * 0.02f));
                }
            }
            // Tridents fly outward (10-50 ticks)
            else if (launched && ticksAlive - launchTick < 40) {
                float dist = (ticksAlive - launchTick) * 1.2f;
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI / 16) * i;
                    double x = Math.cos(angle) * (1.5 + dist);
                    double z = Math.sin(angle) * (1.5 + dist);
                    float yDrop = dist * 0.5f; // Gravity arc
                    Location flyLoc = elevated.clone().add(x, -yDrop, z);
                    if (i < tridentHandles.size()) {
                        tridentHandles.get(i).entity().teleport(flyLoc);
                    }
                    if (ticksAlive % 4 == 0) {
                        DisplayBuilder.purpleDust(flyLoc, 5, 0.1);
                    }
                }
            }
            // Impact — tridents embed (tick 50)
            else if (launched && ticksAlive - launchTick == 40) {
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI / 16) * i;
                    Location impactLoc = center.clone().add(
                        Math.cos(angle) * 49.5, 0.1, Math.sin(angle) * 49.5);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.purpleDust(impactLoc, 20, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SixteenTridentSpinningRing(plugin); }
    }

    // ================================================================
    // #102 — TRIDENT RAINFALL
    // 24 tridents rain vertically over 8 seconds, background threat
    // ================================================================
    public static class TridentRainfall extends BossAttack {
        private final List<BlockDisplayHandle> rainHandles = new ArrayList<>();
        private int tridentsFired = 0;
        private static final int MAX_TRIDENTS = 24;

        public TridentRainfall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_rainfall", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); // 12 hearts
            config.setDamageRadius(1.2);
            config.setDurationTicks(360);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(24.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            // No telegraph — tridents simply begin falling
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            // Fire one trident every ~7 ticks (0.33s) for 160 ticks (8 seconds)
            if (ticksAlive % 7 == 0 && tridentsFired < MAX_TRIDENTS && ticksAlive < 160) {
                tridentsFired++;
                // Random position within arena, weighted to avoid clustering
                double offsetX = (Math.random() - 0.5) * 40;
                double offsetZ = (Math.random() - 0.5) * 40;
                Location spawnLoc = center.clone().add(offsetX, 45, offsetZ);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.PRISMARINE_BRICKS);
                trident.scale(0.12f, 0.12f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                rainHandles.add(trident);
                spawnedEntities.add(trident.entity());
                DisplayBuilder.playSound(spawnLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.4f, 0.6f);
            }

            // Animate falling tridents
            for (int i = rainHandles.size() - 1; i >= 0; i--) {
                BlockDisplayHandle handle = rainHandles.get(i);
                if (handle.entity() == null || !handle.entity().isValid()) continue;
                Location loc = handle.entity().getLocation();
                loc.subtract(0, 2.5, 0); // Fall speed
                // Slight angular deviation
                loc.add((Math.random() - 0.5) * 0.3, 0, (Math.random() - 0.5) * 0.3);
                handle.entity().teleport(loc);

                // Trail particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(loc, 3, 0.2);
                }

                // Ground impact
                if (loc.getY() <= center.getY() + 0.5) {
                    triggerImpactDamage(loc);
                    DisplayBuilder.crimsonDust(loc, 1, 0.3);
                    DisplayBuilder.crimsonDust(loc, 30, 1.5);
                    DisplayBuilder.playSound(loc, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentRainfall(plugin); }
    }

    // ================================================================
    // #103 — LOGARITHMIC TRIDENT SPIRAL
    // 8 tridents converge on a single target from fanned angles
    // ================================================================
    public static class LogarithmicTridentSpiral extends BossAttack {
        private final List<BlockDisplayHandle> spiralHandles = new ArrayList<>();
        private boolean launched = false;
        private int launchTick = 0;

        public LogarithmicTridentSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_logarithmic_spiral", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0); // 13 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(120);
            config.setTicksBetweenDamage(5);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph — spiral line on floor
            DisplayBuilder.crimsonDust(center, 40, 4.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph phase (0-10 ticks = 0.5s)
            if (ticksAlive < 10 && !launched) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 15, 3.0);
                }
            }
            // Launch 8 tridents from fanned angles (tick 10)
            else if (ticksAlive == 10 && !launched) {
                launched = true;
                launchTick = ticksAlive;
                for (int i = 0; i < 8; i++) {
                    double angle = -Math.PI / 3 + (2 * Math.PI / 3) * (i / 7.0);
                    double dist = 20.0;
                    Location origin = center.clone().add(
                        Math.cos(angle) * dist, 8 + (i * 0.5), Math.sin(angle) * dist);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(origin, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    spiralHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                for (int i = 0; i < 8; i++) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.9f + (i * 0.03f));
                }
            }
            // Tridents converge (10-40 ticks = 1.5s flight)
            else if (launched && ticksAlive - launchTick < 30) {
                float progress = (ticksAlive - launchTick) / 30.0f;
                for (int i = 0; i < spiralHandles.size(); i++) {
                    double angle = -Math.PI / 3 + (2 * Math.PI / 3) * (i / 7.0);
                    double startDist = 20.0;
                    double currentDist = startDist * (1.0 - progress);
                    double startY = 8 + (i * 0.5);
                    double currentY = startY * (1.0 - progress);
                    Location flyLoc = center.clone().add(
                        Math.cos(angle) * currentDist, currentY, Math.sin(angle) * currentDist);
                    spiralHandles.get(i).entity().teleport(flyLoc);
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.purpleDust(flyLoc, 4, 0.1);
                    }
                }
            }
            // Convergence impact (tick 40)
            else if (launched && ticksAlive - launchTick == 30) {
                triggerImpactDamage(center);
                DisplayBuilder.purpleDust(center, 60, 2.0);
                DisplayBuilder.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LogarithmicTridentSpiral(plugin); }
    }

    // ================================================================
    // #104 — CARDINAL CROSS VOLLEY
    // 20 tridents from N/S/E/W converge on arena center
    // ================================================================
    public static class CardinalCrossVolley extends BossAttack {
        private final List<BlockDisplayHandle> volleyHandles = new ArrayList<>();
        private boolean launched = false;
        private int launchTick = 0;

        public CardinalCrossVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_cardinal_cross_volley", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); // 14 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(100);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph — beams from all 4 walls
            for (int dir = 0; dir < 4; dir++) {
                double angle = (Math.PI / 2) * dir;
                Location beamOrigin = center.clone().add(Math.cos(angle) * 25, 3, Math.sin(angle) * 25);
                DisplayBuilder.purpleDust(beamOrigin, 50, 2.0);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10 && !launched) {
                if (ticksAlive % 4 == 0) {
                    for (int dir = 0; dir < 4; dir++) {
                        double angle = (Math.PI / 2) * dir;
                        Location beamLoc = center.clone().add(Math.cos(angle) * 25, 3, Math.sin(angle) * 25);
                        DisplayBuilder.purpleDust(beamLoc, 20, 1.5);
                    }
                }
            }
            // Launch 5 tridents from each cardinal direction (tick 10)
            else if (ticksAlive == 10 && !launched) {
                launched = true;
                launchTick = ticksAlive;
                for (int dir = 0; dir < 4; dir++) {
                    double baseAngle = (Math.PI / 2) * dir;
                    for (int j = 0; j < 5; j++) {
                        double spreadAngle = baseAngle + (j - 2) * (Math.PI / 12);
                        Location origin = center.clone().add(
                            Math.cos(spreadAngle) * 25, 3 + j * 0.3, Math.sin(spreadAngle) * 25);
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(origin, Material.PRISMARINE_BRICKS);
                        trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                        volleyHandles.add(trident);
                        spawnedEntities.add(trident.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.9f, 1.0f);
                }
            }
            // Tridents fly inward (10-35 ticks)
            else if (launched && ticksAlive - launchTick < 25) {
                float progress = (ticksAlive - launchTick) / 25.0f;
                int idx = 0;
                for (int dir = 0; dir < 4; dir++) {
                    double baseAngle = (Math.PI / 2) * dir;
                    for (int j = 0; j < 5; j++) {
                        double spreadAngle = baseAngle + (j - 2) * (Math.PI / 12);
                        double startDist = 25.0;
                        double currentDist = startDist * (1.0 - progress);
                        Location flyLoc = center.clone().add(
                            Math.cos(spreadAngle) * currentDist,
                            3 + j * 0.3 - progress * 2,
                            Math.sin(spreadAngle) * currentDist);
                        if (idx < volleyHandles.size()) {
                            volleyHandles.get(idx).entity().teleport(flyLoc);
                        }
                        if (ticksAlive % 4 == 0) {
                            DisplayBuilder.crimsonDust(flyLoc, 6, 0.1);
                        }
                        idx++;
                    }
                }
            }
            // Central impact (tick 35)
            else if (launched && ticksAlive - launchTick == 25) {
                triggerImpactDamage(center);
                for (int i = 0; i < 3; i++) {
                    DisplayBuilder.crimsonDust(center, 1, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CardinalCrossVolley(plugin); }
    }

    // ================================================================
    // #105 — DIAGONAL CROSS VOLLEY
    // 20 tridents from NE/NW/SE/SW — fires immediately after #104
    // ================================================================
    public static class DiagonalCrossVolley extends BossAttack {
        private final List<BlockDisplayHandle> diagonalHandles = new ArrayList<>();
        private boolean launched = false;
        private int launchTick = 0;

        public DiagonalCrossVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_diagonal_cross_volley", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); // 14 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(80);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            // No telegraph — fires immediately after #104
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Immediate launch (tick 0)
            if (ticksAlive == 0 && !launched) {
                launched = true;
                launchTick = ticksAlive;
                double[] diagonals = { Math.PI / 4, 3 * Math.PI / 4, 5 * Math.PI / 4, 7 * Math.PI / 4 };
                for (int dir = 0; dir < 4; dir++) {
                    double baseAngle = diagonals[dir];
                    for (int j = 0; j < 5; j++) {
                        double spreadAngle = baseAngle + (j - 2) * (Math.PI / 12);
                        Location origin = center.clone().add(
                            Math.cos(spreadAngle) * 25, 3 + j * 0.3, Math.sin(spreadAngle) * 25);
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(origin, Material.PRISMARINE_BRICKS);
                        trident.scale(0.12f, 0.12f, 0.9f).glow(255, 180, 0).interpolation(2, 0);
                        diagonalHandles.add(trident);
                        spawnedEntities.add(trident.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 1.0f);
                }
            }
            // Tridents fly inward (0-25 ticks)
            else if (launched && ticksAlive - launchTick < 25 && ticksAlive > 0) {
                float progress = (ticksAlive - launchTick) / 25.0f;
                double[] diagonals = { Math.PI / 4, 3 * Math.PI / 4, 5 * Math.PI / 4, 7 * Math.PI / 4 };
                int idx = 0;
                for (int dir = 0; dir < 4; dir++) {
                    double baseAngle = diagonals[dir];
                    for (int j = 0; j < 5; j++) {
                        double spreadAngle = baseAngle + (j - 2) * (Math.PI / 12);
                        double currentDist = 25.0 * (1.0 - progress);
                        Location flyLoc = center.clone().add(
                            Math.cos(spreadAngle) * currentDist,
                            3 + j * 0.3 - progress * 2,
                            Math.sin(spreadAngle) * currentDist);
                        if (idx < diagonalHandles.size()) {
                            diagonalHandles.get(idx).entity().teleport(flyLoc);
                        }
                        if (ticksAlive % 3 == 0) {
                            DisplayBuilder.crimsonDust(flyLoc, 5, 0.2);
                        }
                        idx++;
                    }
                }
            }
            // Impact (tick 25)
            else if (launched && ticksAlive - launchTick == 25) {
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 20, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DiagonalCrossVolley(plugin); }
    }

    // ================================================================
    // #106 — TRIDENT HAILSTORM
    // 20 rapid-fire tridents at nearest player over 10 seconds
    // ================================================================
    public static class TridentHailstorm extends BossAttack {
        private final List<BlockDisplayHandle> hailHandles = new ArrayList<>();
        private int tridentsFired = 0;
        private static final int MAX_TRIDENTS = 20;

        public TridentHailstorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_hailstorm", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); // 12 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(160);
            config.setTicksBetweenDamage(5);
            config.setTracksPlayer(true);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(24.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph — rapid flickering, END_ROD particles
            Location elevated = center.clone().add(0, 25, 0);
            DisplayBuilder.purpleDust(elevated, 30, 2.0);
            DisplayBuilder.playSound(elevated, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 25, 0), 10, 1.5);
                }
                return;
            }

            // Fire one trident every 10 ticks (0.5s) for 200 ticks
            if (ticksAlive >= 10 && (ticksAlive - 10) % 10 == 0 && tridentsFired < MAX_TRIDENTS) {
                tridentsFired++;
                Location elevated = center.clone().add(0, 25, 0);
                // Higher arc trajectory
                BlockDisplayHandle trident = displayBuilder.spawnBlock(elevated, Material.PRISMARINE_BRICKS);
                trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                hailHandles.add(trident);
                spawnedEntities.add(trident.entity());
                DisplayBuilder.purpleDust(elevated, 10, 0.5);
                DisplayBuilder.playSound(elevated, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 1.0f);
            }

            // Animate falling tridents toward target
            for (int i = hailHandles.size() - 1; i >= 0; i--) {
                BlockDisplayHandle handle = hailHandles.get(i);
                if (handle.entity() == null || !handle.entity().isValid()) continue;
                Location loc = handle.entity().getLocation();
                // Arc toward target
                Vector direction = center.toVector().subtract(loc.toVector()).normalize();
                loc.add(direction.multiply(1.8));
                handle.entity().teleport(loc);
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(loc, 3, 0.2);
                }
                // Impact check
                if (loc.distanceSquared(center) < 4) {
                    triggerImpactDamage(loc);
                    DisplayBuilder.crimsonDust(loc, 1, 0.3);
                    DisplayBuilder.crimsonDust(loc, 6, 0.5);
                    DisplayBuilder.playSound(loc, Sound.ENTITY_ARROW_HIT_PLAYER, 0.8f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentHailstorm(plugin); }
    }

    // ================================================================
    // #107 — BRIMSTONE SKULL SPLITTER
    // 3 wither skulls that split into 4 tridents each on detonation
    // ================================================================
    public static class BrimstoneSkullSplitter extends BossAttack {
        private final List<BlockDisplayHandle> skullHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> splitHandles = new ArrayList<>();
        private boolean skulLaunched = false;
        private int launchTick = 0;
        private boolean detonated = false;

        public BrimstoneSkullSplitter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_skull_splitter", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0); // 10 hearts skull + 28 hearts trident
            config.setDamageRadius(2.5);
            config.setDurationTicks(240);
            config.setCooldownTicks(140);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(28.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph — purple skull outline
            DisplayBuilder.purpleDust(center.clone().add(0, 5, 3), 20, 1.5);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10 && !skulLaunched) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 5, 0), 8, 1.5);
                }
            }
            // Launch 3 skulls (tick 10)
            else if (ticksAlive == 10 && !skulLaunched) {
                skulLaunched = true;
                launchTick = ticksAlive;
                for (int i = 0; i < 3; i++) {
                    double angle = (2 * Math.PI / 3) * i;
                    Location skullLoc = center.clone().add(Math.cos(angle) * 2, 5, Math.sin(angle) * 2);
                    BlockDisplayHandle skull = displayBuilder.spawnBlock(skullLoc, Material.WITHER_SKELETON_SKULL);
                    skull.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(2, 0);
                    skullHandles.add(skull);
                    spawnedEntities.add(skull.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.9f, 0.8f + (i * 0.1f));
                }
            }
            // Skulls fly outward (10-70 ticks = 3s flight)
            else if (skulLaunched && !detonated && ticksAlive - launchTick < 60) {
                float dist = (ticksAlive - launchTick) * 0.6f;
                for (int i = 0; i < 3; i++) {
                    double angle = (2 * Math.PI / 3) * i;
                    Location flyLoc = center.clone().add(
                        Math.cos(angle) * (2 + dist), 5 - dist * 0.08, Math.sin(angle) * (2 + dist));
                    skullHandles.get(i).entity().teleport(flyLoc);
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.purpleDust(flyLoc, 8, 0.3);
                    }
                }
            }
            // Detonation — each skull splits into 4 tridents (tick 70)
            else if (skulLaunched && !detonated && ticksAlive - launchTick == 60) {
                detonated = true;
                for (int i = 0; i < 3; i++) {
                    double angle = (2 * Math.PI / 3) * i;
                    Location detonationLoc = center.clone().add(
                        Math.cos(angle) * 38, 1, Math.sin(angle) * 38);
                    DisplayBuilder.crimsonDust(detonationLoc, 1, 0.5);
                    DisplayBuilder.crimsonDust(detonationLoc, 60, 3.0);
                    DisplayBuilder.playSound(detonationLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.9f);
                    triggerImpactDamage(detonationLoc);

                    // Spawn 4 split tridents in + pattern
                    double[] splitAngles = { 0, Math.PI / 2, Math.PI, 3 * Math.PI / 2 };
                    for (double splitAngle : splitAngles) {
                        BlockDisplayHandle splitTrident = displayBuilder.spawnBlock(
                            detonationLoc.clone(), Material.PRISMARINE_BRICKS);
                        splitTrident.scale(0.1f, 0.1f, 0.7f).glow(200, 0, 50).interpolation(2, 0);
                        splitHandles.add(splitTrident);
                        spawnedEntities.add(splitTrident.entity());
                    }
                }
            }
            // Split tridents fly outward (70-90 ticks)
            else if (detonated && ticksAlive - launchTick >= 60 && ticksAlive - launchTick < 80) {
                float splitDist = (ticksAlive - launchTick - 60) * 0.8f;
                int idx = 0;
                for (int i = 0; i < 3; i++) {
                    double baseAngle = (2 * Math.PI / 3) * i;
                    Location detonationLoc = center.clone().add(
                        Math.cos(baseAngle) * 38, 1, Math.sin(baseAngle) * 38);
                    double[] splitAngles = { 0, Math.PI / 2, Math.PI, 3 * Math.PI / 2 };
                    for (double splitAngle : splitAngles) {
                        if (idx < splitHandles.size()) {
                            Location splitLoc = detonationLoc.clone().add(
                                Math.cos(splitAngle) * splitDist, 0, Math.sin(splitAngle) * splitDist);
                            splitHandles.get(idx).entity().teleport(splitLoc);
                            if (ticksAlive % 4 == 0) {
                                DisplayBuilder.purpleDust(splitLoc, 3, 0.1);
                            }
                        }
                        idx++;
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneSkullSplitter(plugin); }
    }

    // ================================================================
    // #108 — TRIDENT-BRIMSTONE ECHO
    // 6 tridents that create brimstone explosions on landing, chasing player
    // ================================================================
    public static class TridentBrimstoneEcho extends BossAttack {
        private final List<BlockDisplayHandle> echoHandles = new ArrayList<>();
        private int tridentsFired = 0;
        private static final int TOTAL_TRIDENTS = 6;

        public TridentBrimstoneEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_brimstone_echo", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0); // 13 hearts trident
            config.setDamageRadius(3.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(180);
            config.setTicksBetweenDamage(5);
            config.setTracksPlayer(true);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(22.0); // 11 hearts brimstone
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — eyes flare crimson-purple
            DisplayBuilder.crimsonDust(center.clone().add(0, 6, 0), 15, 1.0);
            DisplayBuilder.purpleDust(center.clone().add(0, 6, 0), 15, 1.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 6, 0), 8, 1.0);
                }
                return;
            }

            // Fire one trident every 6 ticks (0.3s) starting at tick 10
            int firePhase = ticksAlive - 10;
            if (firePhase >= 0 && firePhase % 6 == 0 && tridentsFired < TOTAL_TRIDENTS) {
                tridentsFired++;
                Location elevated = center.clone().add(0, 15, 0);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(elevated, Material.PRISMARINE_BRICKS);
                trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                echoHandles.add(trident);
                spawnedEntities.add(trident.entity());
                DisplayBuilder.playSound(elevated, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 1.0f);
            }

            // Animate tridents toward player and create explosions
            for (int i = echoHandles.size() - 1; i >= 0; i--) {
                BlockDisplayHandle handle = echoHandles.get(i);
                if (handle.entity() == null || !handle.entity().isValid()) continue;
                Location loc = handle.entity().getLocation();
                Vector direction = center.toVector().subtract(loc.toVector()).normalize();
                loc.add(direction.multiply(2.0));
                handle.entity().teleport(loc);

                // Trail particles
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.crimsonDust(loc, 4, 0.2);
                }

                // Impact — brimstone explosion
                if (loc.getY() <= center.getY() + 1.5 || loc.distanceSquared(center) < 9) {
                    triggerImpactDamage(loc);
                    // Brimstone explosion burst
                    DisplayBuilder.crimsonDust(loc, 25, 1.0);
                    DisplayBuilder.crimsonDust(loc, 40, 3.0);
                    DisplayBuilder.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.8f);
                    // Fire zone particles
                    DisplayBuilder.crimsonDust(loc, 15, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentBrimstoneEcho(plugin); }
    }

    // ================================================================
    // #109 — BRIMSTONE CAGE
    // 6x6x4 damaging particle cage around a player + tridents inside
    // ================================================================
    public static class BrimstoneCage extends BossAttack {
        private final List<BlockDisplayHandle> cageHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> cageTridents = new ArrayList<>();
        private int cageTridentsFired = 0;

        public BrimstoneCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_cage", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); // 12 hearts wall contact
            config.setDamageRadius(3.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph — crimson cage outline
            DisplayBuilder.crimsonDust(center, 30, 4.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 15, 4.0);
                }
                return;
            }

            // Build cage at tick 10
            if (ticksAlive == 10) {
                // 6x6x4 cage walls using block displays
                for (int face = 0; face < 6; face++) {
                    Material mat = Material.RED_STAINED_GLASS;
                    Location wallLoc;
                    float sx, sy, sz;
                    switch (face) {
                        case 0: wallLoc = center.clone().add(-3, 0, 0); sx = 0.1f; sy = 4.0f; sz = 6.0f; break;
                        case 1: wallLoc = center.clone().add(3, 0, 0); sx = 0.1f; sy = 4.0f; sz = 6.0f; break;
                        case 2: wallLoc = center.clone().add(0, 0, -3); sx = 6.0f; sy = 4.0f; sz = 0.1f; break;
                        case 3: wallLoc = center.clone().add(0, 0, 3); sx = 6.0f; sy = 4.0f; sz = 0.1f; break;
                        case 4: wallLoc = center.clone().add(0, 4, 0); sx = 6.0f; sy = 0.1f; sz = 6.0f; break;
                        default: wallLoc = center.clone().add(0, 0, 0); sx = 6.0f; sy = 0.1f; sz = 6.0f; break;
                    }
                    BlockDisplayHandle wall = displayBuilder.spawnBlock(wallLoc, mat);
                    wall.scale(sx, sy, sz).glow(200, 0, 50).interpolation(3, 0);
                    cageHandles.add(wall);
                    spawnedEntities.add(wall.entity());
                }
            }

            // Cage particle walls (10-130 ticks = 6 seconds)
            if (ticksAlive >= 10 && ticksAlive < 130) {
                if (ticksAlive % 4 == 0) {
                    // Wall particles
                    DisplayBuilder.crimsonDust(center, 24, 3.5);
                    // Interior mist
                    DisplayBuilder.purpleDust(center.clone().add(0, 2, 0), 5, 2.0);
                }

                // Throw tridents INTO the cage (8 tridents over 80 ticks)
                if (ticksAlive >= 20 && (ticksAlive - 20) % 10 == 0 && cageTridentsFired < 8) {
                    cageTridentsFired++;
                    double angle = Math.random() * 2 * Math.PI;
                    Location tridentOrigin = center.clone().add(
                        Math.cos(angle) * 10, 6 + Math.random() * 3, Math.sin(angle) * 10);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tridentOrigin, Material.PRISMARINE_BRICKS);
                    trident.scale(0.1f, 0.1f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                    cageTridents.add(trident);
                    spawnedEntities.add(trident.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.7f, 1.0f);
                }

                // Animate cage tridents toward center
                for (BlockDisplayHandle handle : cageTridents) {
                    if (handle.entity() == null || !handle.entity().isValid()) continue;
                    Location loc = handle.entity().getLocation();
                    Vector dir = center.toVector().subtract(loc.toVector()).normalize();
                    loc.add(dir.multiply(1.5));
                    handle.entity().teleport(loc);
                    if (ticksAlive % 4 == 0) {
                        DisplayBuilder.crimsonDust(loc, 4, 0.1);
                    }
                }
            }

            // Cage dissolves (tick 130)
            if (ticksAlive == 130) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCage(plugin); }
    }

    // ================================================================
    // #110 — SYNCHRONIZED SPIRAL BURST
    // Double helix of 8 brimstone fireballs + 8 tridents at different speeds
    // ================================================================
    public static class SynchronizedSpiralBurst extends BossAttack {
        private final List<BlockDisplayHandle> fireballHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> tridentHandles = new ArrayList<>();
        private boolean launched = false;
        private int launchTick = 0;

        public SynchronizedSpiralBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_synchronized_spiral", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0); // 11 hearts brimstone
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(160);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(28.0); // 14 hearts trident
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Telegraph — double helix particles
            Location elevated = center.clone().add(0, 35, 0);
            for (int i = 0; i < 20; i++) {
                double t = i / 20.0 * Math.PI * 2;
                DisplayBuilder.purpleDust(elevated.clone().add(Math.cos(t) * 1.5, -i * 0.3, Math.sin(t) * 1.5), 2, 0.1);
                DisplayBuilder.crimsonDust(elevated.clone().add(Math.cos(t + Math.PI) * 1.5, -i * 0.3, Math.sin(t + Math.PI) * 1.5), 2, 0.1);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location elevated = center.clone().add(0, 35, 0);

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10 && !launched) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(elevated, 5, 1.5);
                    DisplayBuilder.crimsonDust(elevated, 5, 1.5);
                }
            }
            // Launch double helix (tick 10)
            else if (ticksAlive == 10 && !launched) {
                launched = true;
                launchTick = ticksAlive;
                // 8 brimstone fireballs (helix 1)
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i;
                    Location fbLoc = elevated.clone().add(
                        Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                    BlockDisplayHandle fireball = displayBuilder.spawnBlock(fbLoc, Material.MAGMA_BLOCK);
                    fireball.scale(0.6f, 0.6f, 0.6f).glow(255, 100, 0).interpolation(2, 0);
                    fireballHandles.add(fireball);
                    spawnedEntities.add(fireball.entity());
                }
                // 8 tridents (helix 2, offset by PI)
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i + Math.PI;
                    Location tLoc = elevated.clone().add(
                        Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    tridentHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.crimsonDust(elevated, 2, 1.0);
                DisplayBuilder.playSound(elevated, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.8f);
            }
            // Spiral outward (10-60 ticks)
            else if (launched && ticksAlive - launchTick < 50) {
                int t = ticksAlive - launchTick;
                float brimstoneSpeed = 0.7f; // 70% speed
                float tridentSpeed = 1.2f; // 120% speed

                for (int i = 0; i < 8; i++) {
                    // Brimstone fireballs (slower, expanding spiral)
                    double fbAngle = (2 * Math.PI / 8) * i + t * 0.15;
                    float fbDist = t * brimstoneSpeed;
                    float fbYDrop = t * 0.4f;
                    Location fbLoc = elevated.clone().add(
                        Math.cos(fbAngle) * (1.5 + fbDist), -fbYDrop, Math.sin(fbAngle) * (1.5 + fbDist));
                    fireballHandles.get(i).entity().teleport(fbLoc);
                    if (t % 4 == 0) {
                        DisplayBuilder.crimsonDust(fbLoc, 5, 0.3);
                    }

                    // Tridents (faster, expanding spiral)
                    double tAngle = (2 * Math.PI / 8) * i + Math.PI + t * 0.15;
                    float tDist = t * tridentSpeed;
                    float tYDrop = t * 0.5f;
                    Location tLoc = elevated.clone().add(
                        Math.cos(tAngle) * (1.5 + tDist), -tYDrop, Math.sin(tAngle) * (1.5 + tDist));
                    tridentHandles.get(i).entity().teleport(tLoc);
                    if (t % 4 == 0) {
                        DisplayBuilder.purpleDust(tLoc, 5, 0.2);
                    }
                }
            }
            // Impact zone (tick 60)
            else if (launched && ticksAlive - launchTick == 50) {
                // All projectiles reach arena edge
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i;
                    Location impactLoc = center.clone().add(Math.cos(angle) * 30, 0.1, Math.sin(angle) * 30);
                    triggerImpactDamage(impactLoc);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SynchronizedSpiralBurst(plugin); }
    }
}
