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
 * Attacks #11-20
 *
 * Sky stream weaponization begins. Fans widen. Mines deploy.
 * The arena stops being safe ground.
 * NO status effects — damage only.
 */
public final class CalamitasArrivalB {

    private CalamitasArrivalB() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new CrimsonSweep(plugin));
        registry.register(new BrimstoneArc(plugin));
        registry.register(new DivideAndPunish(plugin));
        registry.register(new TridentRain(plugin));
        registry.register(new CalculatedPrecision(plugin));
        registry.register(new BrimstoneCrown(plugin));
        registry.register(new DiveStrafe(plugin));
        registry.register(new TridentRicochet(plugin));
        registry.register(new SpellWitchArc(plugin));
        registry.register(new FivefoldArc(plugin));
    }

    // ================================================================
    // 11. CRIMSON SWEEP — Sky stream pulled to ground, 180-degree sweep
    // ================================================================
    public static class CrimsonSweep extends BossAttack {
        private final List<BlockDisplayHandle> sweepHandles = new ArrayList<>();
        private boolean sweepActive = false;
        private int sweepStartTick = 0;

        public CrimsonSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crimson_sweep", AttackType.BOSS, 5), "calamitas");
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Hand reaches upward to crimson sky stream
            BlockDisplayHandle streamGrab = displayBuilder.spawnBlock(
                center.clone().add(1, 22, 0), Material.RED_STAINED_GLASS);
            streamGrab.scale(0.5f, 8.0f, 0.5f).glow(180, 0, 0).interpolation(3, 0);
            sweepHandles.add(streamGrab);
            spawnedEntities.add(streamGrab.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — stream bends toward hand (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !sweepActive) {
                float bend = ticksAlive / 40.0f;
                if (!sweepHandles.isEmpty()) {
                    sweepHandles.get(0).entity().teleport(
                        center.clone().add(1, 22 - bend * 20, 0));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(1, 22 - bend * 10, 0), 10, 1.5);
                }
            }
            // Sweep activates (tick 40) — 180 degrees at ground level
            else if (ticksAlive == 40 && !sweepActive) {
                sweepActive = true;
                sweepStartTick = ticksAlive;
                // Ground sweep band segments
                for (int seg = 0; seg < 6; seg++) {
                    double angle = -Math.PI / 2 + (Math.PI / 6) * seg;
                    Location segLoc = center.clone().add(
                        Math.cos(angle) * 12, 0.5, Math.sin(angle) * 12);
                    BlockDisplayHandle band = displayBuilder.spawnBlock(segLoc, Material.RED_STAINED_GLASS);
                    band.scale(1.5f, 1.0f, 3.0f).glow(180, 0, 0).interpolation(2, 0);
                    sweepHandles.add(band);
                    spawnedEntities.add(band.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.5f, 0.5f);
            }
            // Sweep rotates across 180 degrees (40-100 ticks = 3 seconds)
            else if (sweepActive && ticksAlive - sweepStartTick < 60) {
                float progress = (ticksAlive - sweepStartTick) / 60.0f;
                double sweepAngle = -Math.PI / 2 + Math.PI * progress;
                for (int seg = 1; seg <= 6 && seg < sweepHandles.size(); seg++) {
                    double angle = sweepAngle + (Math.PI / 60) * (seg - 3);
                    Location segLoc = center.clone().add(
                        Math.cos(angle) * 12, 0.5, Math.sin(angle) * 12);
                    sweepHandles.get(seg).entity().teleport(segLoc);
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(Math.cos(sweepAngle) * 12, 1.0, Math.sin(sweepAngle) * 12),
                        12, 2.0);
                }
            }
            // Stream returns to normal (tick 100)
            else if (sweepActive && ticksAlive - sweepStartTick == 60) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonSweep(plugin); }
    }

    // ================================================================
    // 12. BRIMSTONE ARC — 5 skull projectiles in lateral sweep
    // ================================================================
    public static class BrimstoneArc extends BossAttack {
        private final List<BlockDisplayHandle> arcHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public BrimstoneArc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_arc", AttackType.BOSS, 5), "calamitas");
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 soul fire lines tracing 60-degree lateral fan
            for (int i = -1; i <= 1; i++) {
                double angle = Math.toRadians(20 * i);
                Location linePt = center.clone().add(Math.sin(angle) * 4, 20, Math.cos(angle) * 4);
                BlockDisplayHandle line = displayBuilder.spawnBlock(linePt, Material.SOUL_SAND);
                line.scale(0.15f, 0.15f, 2.5f).glow(100, 180, 255).interpolation(3, 0);
                arcHandles.add(line);
                spawnedEntities.add(line.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-36 ticks = 1.8 seconds)
            if (ticksAlive < 36 && !fired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 3), 8, 3.0);
                }
            }
            // Fire 5 skulls sequentially (tick 36 — one per 4 ticks, 60-degree sweep)
            else if (ticksAlive == 36 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int i = 0; i < 5; i++) {
                    double angle = Math.toRadians(-30 + 15 * i);
                    Location spawnLoc = center.clone().add(Math.sin(angle) * 2, 20, Math.cos(angle) * 2);
                    BlockDisplayHandle skull = displayBuilder.spawnBlock(spawnLoc, Material.SOUL_SAND);
                    skull.scale(0.6f, 0.6f, 0.6f).glow(180, 80, 0).interpolation(1, 0);
                    arcHandles.add(skull);
                    spawnedEntities.add(skull.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.7f, 0.7f);
            }
            // Skulls travel in arc (36-96 ticks = 3 seconds staggered)
            else if (fired && ticksAlive - fireTick < 60) {
                int skullTick = ticksAlive - fireTick;
                for (int i = 0; i < 5; i++) {
                    int handleIdx = 3 + i;
                    if (handleIdx < arcHandles.size()) {
                        int delay = i * 4;
                        if (skullTick > delay) {
                            float dist = (skullTick - delay) * 1.1f;
                            double angle = Math.toRadians(-30 + 15 * i);
                            arcHandles.get(handleIdx).entity().teleport(
                                center.clone().add(Math.sin(angle) * (2 + dist),
                                    20 - dist * 0.05 * 2, Math.cos(angle) * (2 + dist)));
                        }
                    }
                }
                if (skullTick % 8 == 0) {
                    float pitch = 0.7f + skullTick * 0.005f;
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.7f, pitch);
                }
            }
            // Impact (tick 96)
            else if (fired && ticksAlive - fireTick == 60) {
                triggerImpactDamage(center.clone().add(0, 0.5, 60));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 60), 15, 3.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneArc(plugin); }
    }

    // ================================================================
    // 13. DIVIDE AND PUNISH — Targets isolated players spread too far
    // ================================================================
    public static class DivideAndPunish extends BossAttack {
        private final List<BlockDisplayHandle> divideHandles = new ArrayList<>();
        private boolean detected = false;
        private int detectTick = 0;

        public DivideAndPunish(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_divide_and_punish", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon breath ring expanding from her outward
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 / 12) * i;
                Location ringPt = center.clone().add(Math.cos(angle) * 3, 20, Math.sin(angle) * 3);
                BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.PURPLE_STAINED_GLASS);
                ring.scale(0.6f, 0.1f, 0.6f).glow(128, 0, 200).interpolation(3, 0);
                divideHandles.add(ring);
                spawnedEntities.add(ring.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — detection ring expands (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !detected) {
                float radius = 3.0f + (ticksAlive / 40.0f) * 17.0f;
                for (int i = 0; i < 12 && i < divideHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    divideHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 20, Math.sin(angle) * radius));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 10, radius);
                }
            }
            // Detection and trident fan at isolated players (tick 40)
            else if (ticksAlive == 40 && !detected) {
                detected = true;
                detectTick = ticksAlive;
                // 3-spread trident fan at isolated target
                for (int i = -1; i <= 1; i++) {
                    double angle = Math.toRadians(15 * i);
                    Location spawnLoc = center.clone().add(Math.sin(angle) * 2, 20, Math.cos(angle) * 2);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    divideHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 1.0f);
            }
            // Tridents travel (40-80 ticks)
            else if (detected && ticksAlive - detectTick < 40) {
                float dist = (ticksAlive - detectTick) * 2.0f;
                for (int i = -1; i <= 1; i++) {
                    int idx = 12 + (i + 1);
                    if (idx < divideHandles.size()) {
                        double angle = Math.toRadians(15 * i);
                        divideHandles.get(idx).entity().teleport(
                            center.clone().add(Math.sin(angle) * (2 + dist), 20 - dist * 0.2, Math.cos(angle) * (2 + dist)));
                    }
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 15, dist), 6, 2.0);
                }
            }
            // Impact (tick 80)
            else if (detected && ticksAlive - detectTick == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 80));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 80), 12, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.4f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DivideAndPunish(plugin); }
    }

    // ================================================================
    // 14. TRIDENT RAIN — 12 tridents fired up then rain down scattered
    // ================================================================
    public static class TridentRain extends BossAttack {
        private final List<BlockDisplayHandle> rainHandles = new ArrayList<>();
        private boolean ascending = false;
        private boolean descending = false;
        private int launchTick = 0;

        public TridentRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_rain", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Hands raised — spark shower upward
            BlockDisplayHandle glow = displayBuilder.spawnBlock(
                center.clone().add(0, 22, 0), Material.GLOWSTONE);
            glow.scale(1.0f, 1.0f, 1.0f).glow(220, 220, 255).interpolation(3, 0);
            rainHandles.add(glow);
            spawnedEntities.add(glow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.8f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !ascending) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 25, 0), 8, 2.0);
                }
            }
            // Launch 12 tridents upward (tick 40)
            else if (ticksAlive == 40 && !ascending) {
                ascending = true;
                launchTick = ticksAlive;
                for (int i = 0; i < 12; i++) {
                    double spreadX = (Math.random() - 0.5) * 6;
                    double spreadZ = (Math.random() - 0.5) * 6;
                    Location spawnLoc = center.clone().add(spreadX, 25, spreadZ);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.1f, 0.1f, 0.8f).glow(220, 220, 255).interpolation(1, 0);
                    rainHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.8f, 1.4f);
            }
            // Ascending (40-70 ticks = 1.5 seconds)
            else if (ascending && !descending && ticksAlive - launchTick < 30) {
                float rise = (ticksAlive - launchTick) * 0.5f;
                for (int i = 1; i <= 12 && i < rainHandles.size(); i++) {
                    Location loc = rainHandles.get(i).entity().getLocation();
                    rainHandles.get(i).entity().teleport(loc.clone().add(0, rise * 0.1, 0));
                }
            }
            // Begin descent (tick 70)
            else if (ascending && !descending && ticksAlive - launchTick == 30) {
                descending = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.5f);
            }
            // Descending rain (70-130 ticks = 3 seconds)
            else if (descending && ticksAlive - launchTick < 90) {
                float drop = (ticksAlive - launchTick - 30) * 0.6f;
                for (int i = 1; i <= 12 && i < rainHandles.size(); i++) {
                    double groundY = 35 - drop;
                    if (groundY < 0.1) groundY = 0.1;
                    Location curLoc = rainHandles.get(i).entity().getLocation();
                    rainHandles.get(i).entity().teleport(
                        curLoc.getWorld().getSpawnLocation().clone()
                            .add(curLoc.getX() - center.getX() + center.getX(),
                                groundY, curLoc.getZ() - center.getZ() + center.getZ()));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, Math.max(0.5, 35 - drop), 0), 10, 5.0);
                }
            }
            // Ground impacts (tick 130)
            else if (descending && ticksAlive - launchTick == 90) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 20, 8.0);
                for (int i = 0; i < 8; i++) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_STONE_HIT, 0.6f, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentRain(plugin); }
    }

    // ================================================================
    // 15. CALCULATED PRECISION — Double-tap aimed throws, prediction shot
    // ================================================================
    public static class CalculatedPrecision extends BossAttack {
        private final List<BlockDisplayHandle> precisionHandles = new ArrayList<>();
        private boolean firstFired = false;
        private boolean secondFired = false;
        private int firstFireTick = 0;

        public CalculatedPrecision(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_calculated_precision", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Both hands charge — dual spirals
            for (int i = 0; i < 2; i++) {
                double xOff = (i == 0) ? -1.0 : 1.0;
                BlockDisplayHandle hand = displayBuilder.spawnBlock(
                    center.clone().add(xOff, 20, 0), Material.IRON_BLOCK);
                hand.scale(0.25f, 0.25f, 0.25f).glow(200, 200, 255).interpolation(3, 0);
                precisionHandles.add(hand);
                spawnedEntities.add(hand.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — dual spirals build (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !firstFired) {
                double spiral = ticksAlive * 0.4;
                float radius = 0.8f - (ticksAlive / 40.0f) * 0.5f;
                for (int i = 0; i < 2 && i < precisionHandles.size(); i++) {
                    double xOff = (i == 0) ? -1.0 : 1.0;
                    precisionHandles.get(i).entity().teleport(
                        center.clone().add(xOff + Math.cos(spiral + Math.PI * i) * radius,
                            20 + Math.sin(spiral) * radius, 0));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(-1, 20, 0), 4, 0.6);
                    DisplayBuilder.crimsonDust(center.clone().add(1, 20, 0), 4, 0.6);
                }
            }
            // First trident fires — direct aim (tick 40)
            else if (ticksAlive == 40 && !firstFired) {
                firstFired = true;
                firstFireTick = ticksAlive;
                BlockDisplayHandle trident1 = displayBuilder.spawnBlock(
                    center.clone().add(-1, 20, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                trident1.scale(0.12f, 0.12f, 1.2f).glow(220, 220, 255).interpolation(1, 0);
                precisionHandles.add(trident1);
                spawnedEntities.add(trident1.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.2f);
            }
            // First trident travels + second fires at tick 50 (10 ticks later)
            else if (firstFired && !secondFired && ticksAlive - firstFireTick < 40) {
                float dist1 = (ticksAlive - firstFireTick) * 2.0f;
                if (precisionHandles.size() > 2) {
                    precisionHandles.get(2).entity().teleport(
                        center.clone().add(-1, 20, dist1));
                }
                // Second shot fires at tick 50 — predictive aim
                if (ticksAlive - firstFireTick == 10 && !secondFired) {
                    secondFired = true;
                    BlockDisplayHandle trident2 = displayBuilder.spawnBlock(
                        center.clone().add(1, 20, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident2.scale(0.12f, 0.12f, 1.2f).glow(255, 200, 180).interpolation(1, 0);
                    precisionHandles.add(trident2);
                    spawnedEntities.add(trident2.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.9f);
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(-1, 20, dist1), 4, 0.4);
                }
            }
            // Both tridents travel (after second fired)
            else if (secondFired && ticksAlive - firstFireTick < 40) {
                float dist1 = (ticksAlive - firstFireTick) * 2.0f;
                float dist2 = (ticksAlive - firstFireTick - 10) * 2.0f;
                if (precisionHandles.size() > 2) {
                    precisionHandles.get(2).entity().teleport(
                        center.clone().add(-1, 20, dist1));
                }
                if (dist2 > 0 && precisionHandles.size() > 3) {
                    precisionHandles.get(3).entity().teleport(
                        center.clone().add(1.5, 20, dist2));
                }
            }
            // Impact (tick 80)
            else if (firstFired && ticksAlive - firstFireTick == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 80));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 80), 12, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CalculatedPrecision(plugin); }
    }

    // ================================================================
    // 16. BRIMSTONE CROWN — 4 orbiting skulls that seek players on release
    // ================================================================
    public static class BrimstoneCrown extends BossAttack {
        private final List<BlockDisplayHandle> crownHandles = new ArrayList<>();
        private boolean released = false;
        private int releaseTick = 0;

        public BrimstoneCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_crown", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 4 skulls orbiting at Y+20, radius 5
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 / 4) * i;
                Location skullLoc = center.clone().add(Math.cos(angle) * 5, 20, Math.sin(angle) * 5);
                BlockDisplayHandle skull = displayBuilder.spawnBlock(skullLoc, Material.SOUL_SAND);
                skull.scale(0.7f, 0.7f, 0.7f).glow(180, 80, 0).interpolation(2, 0);
                crownHandles.add(skull);
                spawnedEntities.add(skull.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Orbiting phase (0-40 ticks = 2 seconds, accelerating)
            if (ticksAlive < 40 && !released) {
                double speed = 0.1 + (ticksAlive / 40.0) * 0.3;
                for (int i = 0; i < 4 && i < crownHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 4) * i + ticksAlive * speed;
                    crownHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 5, 20, Math.sin(angle) * 5));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 8, 5.0);
                }
            }
            // Skulls release outward then seek (tick 40)
            else if (ticksAlive == 40 && !released) {
                released = true;
                releaseTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.8f, 0.6f);
            }
            // Outward travel (40-56 ticks = 0.8 seconds outward 8 blocks)
            else if (released && ticksAlive - releaseTick < 16) {
                float dist = (ticksAlive - releaseTick) * 0.5f;
                for (int i = 0; i < 4 && i < crownHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 4) * i;
                    crownHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (5 + dist), 20, Math.sin(angle) * (5 + dist)));
                }
            }
            // Seeking phase (56-116 ticks = 3 seconds homing)
            else if (released && ticksAlive - releaseTick >= 16 && ticksAlive - releaseTick < 76) {
                float seekDist = (ticksAlive - releaseTick - 16) * 1.0f;
                for (int i = 0; i < 4 && i < crownHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 4) * i + seekDist * 0.05;
                    float totalDist = 13 + seekDist;
                    crownHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * totalDist, 20 - seekDist * 0.15,
                            Math.sin(angle) * totalDist));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 15, 0), 6, 10.0);
                }
            }
            // Detonation (tick 116)
            else if (released && ticksAlive - releaseTick == 76) {
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 / 4) * i;
                    Location detLoc = center.clone().add(Math.cos(angle) * 50, 0.1, Math.sin(angle) * 50);
                    DisplayBuilder.crimsonDust(detLoc, 15, 3.0);
                    DisplayBuilder.playSound(detLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneCrown(plugin); }
    }

    // ================================================================
    // 17. DIVE STRAFE — Low-altitude curved dive pass across arena
    // ================================================================
    public static class DiveStrafe extends BossAttack {
        private final List<BlockDisplayHandle> strafeHandles = new ArrayList<>();
        private boolean diving = false;
        private int diveStartTick = 0;

        public DiveStrafe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_dive_strafe", AttackType.BOSS, 5), "calamitas");
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Flight path preview on ground — curved arc line
            for (int seg = 0; seg < 8; seg++) {
                double t = seg / 8.0;
                double arcX = Math.sin(t * Math.PI) * 12;
                double arcZ = -12 + t * 24;
                Location pathPt = center.clone().add(arcX, 0.1, arcZ);
                BlockDisplayHandle path = displayBuilder.spawnBlock(pathPt, Material.ORANGE_STAINED_GLASS);
                path.scale(1.0f, 0.05f, 3.0f).glow(255, 100, 0).interpolation(2, 0);
                strafeHandles.add(path);
                spawnedEntities.add(path.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !diving) {
                if (ticksAlive % 8 == 0) {
                    for (int seg = 0; seg < 8; seg++) {
                        double t = seg / 8.0;
                        double arcX = Math.sin(t * Math.PI) * 12;
                        double arcZ = -12 + t * 24;
                        DisplayBuilder.crimsonDust(center.clone().add(arcX, 0.5, arcZ), 4, 1.0);
                    }
                }
            }
            // Dive begins (tick 40)
            else if (ticksAlive == 40 && !diving) {
                diving = true;
                diveStartTick = ticksAlive;
                // Create her diving model
                BlockDisplayHandle diveModel = displayBuilder.spawnBlock(
                    center.clone().add(0, 20, -12), Material.RED_STAINED_GLASS);
                diveModel.scale(1.5f, 2.5f, 1.5f).glow(180, 0, 0).interpolation(2, 0);
                strafeHandles.add(diveModel);
                spawnedEntities.add(diveModel.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 1.1f);
            }
            // Dive arc (40-90 ticks = 2.5 seconds across arena)
            else if (diving && ticksAlive - diveStartTick < 50) {
                float progress = (ticksAlive - diveStartTick) / 50.0f;
                double arcX = Math.sin(progress * Math.PI) * 12;
                double arcZ = -12 + progress * 24;
                double arcY = 20 - Math.sin(progress * Math.PI) * 16; // Dips to Y+4
                int modelIdx = 8;
                if (modelIdx < strafeHandles.size()) {
                    strafeHandles.get(modelIdx).entity().teleport(
                        center.clone().add(arcX, arcY, arcZ));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(arcX, arcY, arcZ), 8, 2.0);
                }
            }
            // Pull-up aftermath (tick 90)
            else if (diving && ticksAlive - diveStartTick == 50) {
                triggerImpactDamage(center.clone().add(0, 2, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 4, 12), 15, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DiveStrafe(plugin); }
    }

    // ================================================================
    // 18. TRIDENT RICOCHET — Wall bounce trident from unexpected angle
    // ================================================================
    public static class TridentRicochet extends BossAttack {
        private final List<BlockDisplayHandle> ricochetHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;
        private boolean bounced = false;

        public TridentRicochet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_ricochet", AttackType.BOSS, 5), "calamitas");
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
            // Wall aim telegraph — spark flash on wall surface
            BlockDisplayHandle wallMark = displayBuilder.spawnBlock(
                center.clone().add(0, 20, -20), Material.IRON_BLOCK);
            wallMark.scale(0.3f, 0.3f, 0.3f).glow(200, 200, 255).interpolation(3, 0);
            ricochetHandles.add(wallMark);
            spawnedEntities.add(wallMark.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — wall spark (0-36 ticks = 1.8 seconds)
            if (ticksAlive < 36 && !fired) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, -20), 6, 0.5);
                }
            }
            // Fire at wall (tick 36)
            else if (ticksAlive == 36 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                BlockDisplayHandle trident = displayBuilder.spawnBlock(
                    center.clone().add(0, 20, -1), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                trident.scale(0.12f, 0.12f, 1.2f).glow(220, 220, 255).interpolation(1, 0);
                ricochetHandles.add(trident);
                spawnedEntities.add(trident.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
            }
            // Trident flies to wall (36-56 ticks = 1 second)
            else if (fired && !bounced && ticksAlive - fireTick < 20) {
                float dist = (ticksAlive - fireTick) * 1.0f;
                if (ricochetHandles.size() > 1) {
                    ricochetHandles.get(1).entity().teleport(
                        center.clone().add(0, 20, -1 - dist));
                }
            }
            // Bounce (tick 56) — reflect angle
            else if (fired && !bounced && ticksAlive - fireTick == 20) {
                bounced = true;
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, -21), 10, 1.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_METAL_HIT, 1.2f, 1.4f);
            }
            // Reflected travel back through arena (56-116 ticks = 3 seconds)
            else if (bounced && ticksAlive - fireTick < 80) {
                float bounceDist = (ticksAlive - fireTick - 20) * 1.5f;
                if (ricochetHandles.size() > 1) {
                    ricochetHandles.get(1).entity().teleport(
                        center.clone().add(2, 20 - bounceDist * 0.02, -21 + bounceDist));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(2, 20 - bounceDist * 0.02, -21 + bounceDist), 4, 0.5);
                }
            }
            // Embed or despawn (tick 116)
            else if (bounced && ticksAlive - fireTick == 80) {
                triggerImpactDamage(center.clone().add(2, 0.5, 69));
                DisplayBuilder.crimsonDust(center.clone().add(2, 0.5, 69), 10, 1.5);
                DisplayBuilder.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentRicochet(plugin); }
    }

    // ================================================================
    // 19. SPELL_WITCH ARC — Magenta sky stream sweep, slower + debuff zone
    // ================================================================
    public static class SpellWitchArc extends BossAttack {
        private final List<BlockDisplayHandle> witchHandles = new ArrayList<>();
        private boolean sweepActive = false;
        private int sweepStartTick = 0;

        public SpellWitchArc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_spell_witch_arc", AttackType.BOSS, 5), "calamitas");
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Magenta stream bends toward extended left hand
            BlockDisplayHandle streamGrab = displayBuilder.spawnBlock(
                center.clone().add(-1, 22, 0), Material.MAGENTA_STAINED_GLASS);
            streamGrab.scale(0.5f, 8.0f, 0.5f).glow(200, 0, 200).interpolation(3, 0);
            witchHandles.add(streamGrab);
            spawnedEntities.add(streamGrab.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !sweepActive) {
                float bend = ticksAlive / 40.0f;
                if (!witchHandles.isEmpty()) {
                    witchHandles.get(0).entity().teleport(
                        center.clone().add(-1, 22 - bend * 20, 0));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(-1, 22 - bend * 10, 0),
                        10, 1.5, 200, 0, 200, 1.2f);
                }
            }
            // Sweep activates (tick 40) — 90 degrees, slower than crimson
            else if (ticksAlive == 40 && !sweepActive) {
                sweepActive = true;
                sweepStartTick = ticksAlive;
                for (int seg = 0; seg < 5; seg++) {
                    double angle = -Math.PI / 4 + (Math.PI / 2 / 5) * seg;
                    Location segLoc = center.clone().add(Math.cos(angle) * 10, 0.5, Math.sin(angle) * 10);
                    BlockDisplayHandle band = displayBuilder.spawnBlock(segLoc, Material.MAGENTA_STAINED_GLASS);
                    band.scale(1.0f, 1.0f, 3.0f).glow(200, 0, 200).interpolation(2, 0);
                    witchHandles.add(band);
                    spawnedEntities.add(band.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.2f, 0.5f);
            }
            // Sweep 90 degrees over 80 ticks = 4 seconds (slower)
            else if (sweepActive && ticksAlive - sweepStartTick < 80) {
                float progress = (ticksAlive - sweepStartTick) / 80.0f;
                double sweepAngle = -Math.PI / 4 + (Math.PI / 2) * progress;
                for (int seg = 1; seg <= 5 && seg < witchHandles.size(); seg++) {
                    double angle = sweepAngle + (Math.PI / 40) * (seg - 3);
                    witchHandles.get(seg).entity().teleport(
                        center.clone().add(Math.cos(angle) * 10, 0.5, Math.sin(angle) * 10));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(
                        center.clone().add(Math.cos(sweepAngle) * 10, 1.0, Math.sin(sweepAngle) * 10),
                        12, 2.0, 200, 0, 200, 1.2f);
                }
            }
            // Stream returns (tick 120)
            else if (sweepActive && ticksAlive - sweepStartTick == 80) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpellWitchArc(plugin); }
    }

    // ================================================================
    // 20. FIVEFOLD ARC — 5-spread wide fan, 80-degree coverage
    // ================================================================
    public static class FivefoldArc extends BossAttack {
        private final List<BlockDisplayHandle> fiveHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public FivefoldArc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_fivefold_arc", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 5 spark points in 80-degree fan
            for (int i = -2; i <= 2; i++) {
                double angle = Math.toRadians(20 * i);
                Location pt = center.clone().add(Math.sin(angle) * 3, 20, Math.cos(angle) * 3);
                BlockDisplayHandle point = displayBuilder.spawnBlock(pt, Material.IRON_BLOCK);
                point.scale(0.2f, 0.2f, 0.2f).glow(200, 200, 255).interpolation(3, 0);
                fiveHandles.add(point);
                spawnedEntities.add(point.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !fired) {
                if (ticksAlive % 8 == 0) {
                    for (int i = -2; i <= 2; i++) {
                        double angle = Math.toRadians(20 * i);
                        DisplayBuilder.crimsonDust(
                            center.clone().add(Math.sin(angle) * 3, 20, Math.cos(angle) * 3), 4, 0.5);
                    }
                }
            }
            // Fire 5 tridents simultaneously (tick 40)
            else if (ticksAlive == 40 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int i = -2; i <= 2; i++) {
                    double angle = Math.toRadians(20 * i);
                    Location spawnLoc = center.clone().add(Math.sin(angle) * 3, 20, Math.cos(angle) * 3);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    float yOff = (i == 0) ? 0 : ((Math.abs(i) == 2) ? 0.5f : -0.3f);
                    trident.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    fiveHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.8f, 1.0f);
            }
            // Tridents travel in fan (40-80 ticks)
            else if (fired && ticksAlive - fireTick < 40) {
                float dist = (ticksAlive - fireTick) * 1.6f;
                for (int i = -2; i <= 2; i++) {
                    int idx = 5 + (i + 2);
                    if (idx < fiveHandles.size()) {
                        double angle = Math.toRadians(20 * i);
                        float yOff = (i == 0) ? 0 : ((Math.abs(i) == 2) ? 0.5f : -0.3f);
                        fiveHandles.get(idx).entity().teleport(
                            center.clone().add(Math.sin(angle) * (3 + dist),
                                20 + yOff, Math.cos(angle) * (3 + dist)));
                    }
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 3 + dist), 8, 3.0);
                }
            }
            // Impact (tick 80)
            else if (fired && ticksAlive - fireTick == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 67));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 67), 15, 4.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FivefoldArc(plugin); }
    }
}
