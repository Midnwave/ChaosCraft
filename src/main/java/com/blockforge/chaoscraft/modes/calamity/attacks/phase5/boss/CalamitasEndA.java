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
 * Attacks #151-160
 *
 * TRIDENT APOCALYPSE. Arena is 20x20. No mercy.
 * Damage: 36-48 hearts (escalating). This is the finale.
 * Calamitas colors: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255), purple(128,0,255)
 *
 * NO status effects — damage only.
 */
public final class CalamitasEndA {

    private CalamitasEndA() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new OmnidirectionalBurst(plugin));
        registry.register(new RainfallProtocol(plugin));
        registry.register(new SpiralGalaxy(plugin));
        registry.register(new TridentGod(plugin));
        registry.register(new WallOfDeath(plugin));
        registry.register(new CrownRain(plugin));
        registry.register(new TridentGatlingGun(plugin));
        registry.register(new BrimstoneBeamCannon(plugin));
        registry.register(new CrimsonTornadoTridents(plugin));
        registry.register(new TridentMeteorShower(plugin));
    }

    // ================================================================
    // #151 — OMNIDIRECTIONAL BURST
    // 32 tridents radiate in perfect sphere from center, no telegraph
    // ================================================================
    public static class OmnidirectionalBurst extends BossAttack {
        private final List<BlockDisplayHandle> burstHandles = new ArrayList<>();
        private boolean fired = false;

        public OmnidirectionalBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_omnidirectional_burst", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            // NO telegraph
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location elevated = center.clone().add(0, 10, 0);

            if (ticksAlive == 0 && !fired) {
                fired = true;
                // 32 tridents in Fibonacci sphere
                double goldenRatio = (1 + Math.sqrt(5)) / 2;
                for (int i = 0; i < 32; i++) {
                    double theta = Math.acos(1 - 2.0 * (i + 0.5) / 32);
                    double phi = 2 * Math.PI * i / goldenRatio;
                    Location tLoc = elevated.clone().add(
                        Math.sin(theta) * Math.cos(phi) * 1.5,
                        Math.sin(theta) * Math.sin(phi) * 1.5,
                        Math.cos(theta) * 1.5);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    burstHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.crimsonDust(elevated, 1, 0.5);
                // Rapid-fire throw sounds
                for (int i = 0; i < 8; i++) {
                    DisplayBuilder.playSound(elevated, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.9f + (i * 0.02f));
                }
                DisplayBuilder.playSound(elevated, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
            }

            // Tridents radiate outward at 1.8x speed (0-25 ticks)
            if (fired && ticksAlive > 0 && ticksAlive < 25) {
                double goldenRatio = (1 + Math.sqrt(5)) / 2;
                float speed = ticksAlive * 1.8f;
                for (int i = 0; i < burstHandles.size(); i++) {
                    double theta = Math.acos(1 - 2.0 * (i + 0.5) / 32);
                    double phi = 2 * Math.PI * i / goldenRatio;
                    Location flyLoc = elevated.clone().add(
                        Math.sin(theta) * Math.cos(phi) * speed,
                        Math.sin(theta) * Math.sin(phi) * speed,
                        Math.cos(theta) * speed);
                    burstHandles.get(i).entity().teleport(flyLoc);
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.crimsonDust(flyLoc, 3, 0.3);
                        DisplayBuilder.crimsonDust(flyLoc, 1, 0.1);
                    }
                }
            }

            // Wall impacts (tick 25)
            if (fired && ticksAlive == 25) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI / 2) * i;
                    Location wallImpact = center.clone().add(Math.cos(angle) * 10, 3, Math.sin(angle) * 10);
                    DisplayBuilder.crimsonDust(wallImpact, 5, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OmnidirectionalBurst(plugin); }
    }

    // ================================================================
    // #152 — RAINFALL PROTOCOL
    // 50 tridents rain over 15 seconds, 70% weighted to player positions
    // ================================================================
    public static class RainfallProtocol extends BossAttack {
        private int tridentsFired = 0;
        private static final int MAX = 50;

        public RainfallProtocol(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_rainfall_protocol", AttackType.BOSS, 5), "calamitas");
            config.setDamage(36.0); // 18 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(36.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 30, 0), 20, 5.0);
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fire 1 trident every 6 ticks (0.3s) for 300 ticks (15s)
            if (ticksAlive >= 10 && (ticksAlive - 10) % 6 == 0 && tridentsFired < MAX) {
                tridentsFired++;
                // 70% near player, 30% random
                double x, z;
                if (Math.random() < 0.7) {
                    x = (Math.random() - 0.5) * 8; // Near center/target
                    z = (Math.random() - 0.5) * 8;
                } else {
                    x = (Math.random() - 0.5) * 20;
                    z = (Math.random() - 0.5) * 20;
                }
                Location dropLoc = center.clone().add(x, 35, z);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(dropLoc, Material.PRISMARINE_BRICKS);
                trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(trident.entity());
                DisplayBuilder.playSound(dropLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 1.2f);
            }

            // Animate all falling tridents
            for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                Location loc = spawnedEntities.get(i).getLocation();
                if (loc.getY() > center.getY() + 1) {
                    loc.subtract(0, 2.5, 0);
                    loc.add((Math.random() - 0.5) * 0.3, 0, (Math.random() - 0.5) * 0.3);
                    spawnedEntities.get(i).teleport(loc);
                    if (ticksAlive % 4 == 0) {
                        DisplayBuilder.crimsonDust(loc, 2, 0.3);
                    }
                }
            }

            // Overhead drizzle effect
            if (ticksAlive % 5 == 0 && ticksAlive < 310) {
                DisplayBuilder.crimsonDust(center.clone().add(0, 30, 0), 5, 10.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RainfallProtocol(plugin); }
    }

    // ================================================================
    // #153 — SPIRAL GALAXY
    // 48 tridents in 3 spiral arms at 120-degree offsets
    // ================================================================
    public static class SpiralGalaxy extends BossAttack {
        private final List<BlockDisplayHandle> armHandles = new ArrayList<>();
        private boolean fired = false;

        public SpiralGalaxy(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_spiral_galaxy", AttackType.BOSS, 5), "calamitas");
            config.setDamage(44.0); // 22 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            // NO telegraph
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location elevated = center.clone().add(0, 18, 0);

            // Fire 3 arms of 16 tridents each, staggered
            if (!fired && ticksAlive == 0) {
                fired = true;
                for (int arm = 0; arm < 3; arm++) {
                    double armOffset = (2 * Math.PI / 3) * arm;
                    for (int t = 0; t < 16; t++) {
                        double spiralAngle = armOffset + t * (Math.PI / 12);
                        float velocity = 1.0f + (t / 16.0f) * 1.2f;
                        Location tLoc = elevated.clone().add(
                            Math.cos(spiralAngle) * 1.5, 0, Math.sin(spiralAngle) * 1.5);
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                        // Different color per arm
                        int r = arm == 0 ? 200 : arm == 1 ? 160 : 220;
                        int g = arm == 0 ? 30 : arm == 1 ? 0 : 80;
                        int b = arm == 0 ? 30 : arm == 1 ? 80 : 20;
                        trident.scale(0.12f, 0.12f, 0.9f).glow(r, g, b).interpolation(2, 0);
                        armHandles.add(trident);
                        spawnedEntities.add(trident.entity());
                    }
                }
                DisplayBuilder.cyanDust(elevated, 20, 2.0);
                DisplayBuilder.playSound(elevated, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.8f, 0.6f);
            }

            // Animate spiral arms expanding (0-80 ticks = ~4 seconds)
            if (fired && ticksAlive > 0 && ticksAlive < 80) {
                for (int arm = 0; arm < 3; arm++) {
                    int armDelay = arm * 26; // 1.28s stagger
                    int armElapsed = ticksAlive - armDelay;
                    if (armElapsed < 0) continue;
                    double armOffset = (2 * Math.PI / 3) * arm;
                    for (int t = 0; t < 16; t++) {
                        int idx = arm * 16 + t;
                        if (idx >= armHandles.size()) continue;
                        double spiralAngle = armOffset + t * (Math.PI / 12) + armElapsed * 0.08;
                        float velocity = 1.0f + (t / 16.0f) * 1.2f;
                        float dist = armElapsed * velocity * 0.5f;
                        float yDrop = armElapsed * 0.3f;
                        Location flyLoc = elevated.clone().add(
                            Math.cos(spiralAngle) * (1.5 + dist), -yDrop, Math.sin(spiralAngle) * (1.5 + dist));
                        armHandles.get(idx).entity().teleport(flyLoc);
                    }
                }
                // Per-arm fire sounds
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.playSound(elevated, Sound.ENTITY_GUARDIAN_ATTACK, 0.4f, 1.0f + (ticksAlive * 0.005f));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpiralGalaxy(plugin); }
    }

    // ================================================================
    // #154 — TRIDENT GOD
    // 48 tridents in 8 seconds (6/second), maximum rate of fire
    // ================================================================
    public static class TridentGod extends BossAttack {
        private int tridentsFired = 0;
        private boolean active = false;

        public TridentGod(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_god", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(260);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(3);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            Location elevated = center.clone().add(0, 12, 0);
            DisplayBuilder.crimsonDust(elevated, 30, 3.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location elevated = center.clone().add(0, 12, 0);

            // 1 second telegraph (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(elevated, 15, 2.0);
                    DisplayBuilder.crimsonDust(elevated, 8, 1.5);
                    DisplayBuilder.crimsonDust(elevated, 5, 1.0);
                }
                return;
            }

            if (!active && ticksAlive == 20) {
                active = true;
            }

            // 6 tridents per second = 1 every 3.33 ticks ~ every 3 ticks
            if (active && ticksAlive >= 20 && (ticksAlive - 20) % 3 == 0 && tridentsFired < 48) {
                tridentsFired++;
                // Burst of 3 in 30-degree spread
                if (tridentsFired % 3 == 0) {
                    for (int j = -1; j <= 1; j++) {
                        double spreadAngle = j * (Math.PI / 12);
                        Location tLoc = elevated.clone().add(
                            Math.cos(spreadAngle) * 1.5, 0, Math.sin(spreadAngle) * 1.5);
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                        trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                        spawnedEntities.add(trident.entity());
                    }
                }
                // Firing particles
                DisplayBuilder.crimsonDust(elevated, 5, 0.5);
                DisplayBuilder.crimsonDust(elevated, 3, 0.5);
                // At this rate, becomes continuous roar
                if (tridentsFired % 6 == 0) {
                    DisplayBuilder.playSound(elevated, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 1.0f);
                }
            }

            // Sustained aura
            if (active && ticksAlive % 4 == 0) {
                DisplayBuilder.crimsonDust(elevated, 5, 1.0);
                DisplayBuilder.crimsonDust(elevated, 3, 0.8);
            }

            // End — silence (tick 180)
            if (active && ticksAlive == 180) {
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentGod(plugin); }
    }

    // ================================================================
    // #155 — WALL OF DEATH
    // 20 tridents in a perfect horizontal line, full arena width
    // ================================================================
    public static class WallOfDeath extends BossAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private boolean fired = false;

        public WallOfDeath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_wall_of_death", AttackType.BOSS, 5), "calamitas");
            config.setDamage(48.0); // 24 hearts
            config.setDamageRadius(1.2);
            config.setDurationTicks(100);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            // NO telegraph
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !fired) {
                fired = true;
                // 20 tridents at north wall, 1 block apart, Y+5
                for (int i = 0; i < 20; i++) {
                    double x = (i - 9.5) * 1.0;
                    Location tLoc = center.clone().add(x, 5, -10);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 20, 20).interpolation(2, 0);
                    wallHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                // Single massive THWOOM
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.6f);
            }

            // Wall sweeps south (0-16 ticks = 0.8 seconds)
            if (fired && ticksAlive > 0 && ticksAlive < 16) {
                float zProgress = ticksAlive * 1.25f; // Travel 20 blocks in 16 ticks
                for (int i = 0; i < wallHandles.size(); i++) {
                    double x = (i - 9.5) * 1.0;
                    Location flyLoc = center.clone().add(x, 5, -10 + zProgress);
                    wallHandles.get(i).entity().teleport(flyLoc);
                }
                // Crimson curtain trail
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 20; i += 3) {
                        double x = (i - 9.5) * 1.0;
                        Location trailLoc = center.clone().add(x, 5, -10 + zProgress);
                        DisplayBuilder.crimsonDust(trailLoc, 3, 0.3);
                    }
                }
            }

            // South wall impact (tick 16)
            if (fired && ticksAlive == 16) {
                for (int i = 0; i < 20; i++) {
                    Location impactLoc = center.clone().add((i - 9.5) * 1.0, 5, 10);
                    DisplayBuilder.crimsonDust(impactLoc, 1, 0.3);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WallOfDeath(plugin); }
    }

    // ================================================================
    // #156 — CROWN RAIN
    // Crimson crown fires tridents downward from 12 crown points
    // ================================================================
    public static class CrownRain extends BossAttack {
        private int volleysFired = 0;

        public CrownRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crown_rain", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 20, 5.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Volleys every 30 ticks (1.5s) — 5 volleys
            if (ticksAlive >= 10 && (ticksAlive - 10) % 30 == 0 && volleysFired < 5) {
                volleysFired++;
                // 12 crown points fire 1 trident each
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI / 12) * i;
                    Location crownPoint = center.clone().add(
                        Math.cos(angle) * 8, 20, Math.sin(angle) * 8);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(crownPoint, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(trident.entity());
                    DisplayBuilder.crimsonDust(crownPoint, 3, 0.3);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.8f);
            }

            // Animate falling crown tridents
            for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                Location loc = spawnedEntities.get(i).getLocation();
                if (loc.getY() > center.getY() + 1) {
                    loc.subtract(0, 2.0, 0);
                    spawnedEntities.get(i).teleport(loc);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrownRain(plugin); }
    }

    // ================================================================
    // #157 — TRIDENT GATLING GUN
    // Ultra-rapid single-direction barrage, 30 tridents in 3 seconds
    // ================================================================
    public static class TridentGatlingGun extends BossAttack {
        private int tridentsFired = 0;

        public TridentGatlingGun(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_gatling", AttackType.BOSS, 5), "calamitas");
            config.setDamage(36.0); // 18 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(120);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(3);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 10, 0), 15, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location elevated = center.clone().add(0, 10, 0);

            // Fire 1 trident every 2 ticks for 60 ticks (30 tridents)
            if (ticksAlive >= 5 && ticksAlive % 2 == 0 && tridentsFired < 30) {
                tridentsFired++;
                double spread = (Math.random() - 0.5) * 0.3;
                Location tLoc = elevated.clone().add(spread, spread, 0);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                trident.scale(0.1f, 0.1f, 0.8f).glow(255, 100, 0).interpolation(2, 0);
                spawnedEntities.add(trident.entity());
                DisplayBuilder.crimsonDust(elevated, 3, 0.3);
                if (tridentsFired % 5 == 0) {
                    DisplayBuilder.playSound(elevated, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 1.2f);
                }
            }

            // Animate outward
            for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                Location loc = spawnedEntities.get(i).getLocation();
                Vector dir = center.toVector().subtract(elevated.toVector()).normalize();
                if (dir.lengthSquared() < 0.01) dir = new Vector(0, -1, 0);
                loc.add(dir.multiply(2.5));
                spawnedEntities.get(i).teleport(loc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentGatlingGun(plugin); }
    }

    // ================================================================
    // #158 — BRIMSTONE BEAM CANNON
    // Charged beam + trident spread combo
    // ================================================================
    public static class BrimstoneBeamCannon extends BossAttack {
        private boolean beamFired = false;

        public BrimstoneBeamCannon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_beam_cannon", AttackType.BOSS, 5), "calamitas");
            config.setDamage(48.0); // 24 hearts beam
            config.setDamageRadius(2.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 8, 0), 20, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge (0-30 ticks = 1.5s)
            if (ticksAlive < 30) {
                float chargeScale = ticksAlive / 30.0f;
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 8, 0), (int)(10 * chargeScale + 5), 1.5);
                }
                return;
            }

            // Fire beam (tick 30)
            if (ticksAlive == 30 && !beamFired) {
                beamFired = true;
                // Beam across entire arena
                for (int d = -10; d <= 10; d++) {
                    Location beamLoc = center.clone().add(0, 5, d);
                    DisplayBuilder.crimsonDust(beamLoc, 5, 0.5);
                    DisplayBuilder.crimsonDust(beamLoc, 3, 0.3);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.7f);
                DisplayBuilder.crimsonDust(center, 3, 2.0);
            }

            // Beam persists (30-70 ticks)
            if (beamFired && ticksAlive > 30 && ticksAlive < 70) {
                if (ticksAlive % 3 == 0) {
                    for (int d = -10; d <= 10; d += 2) {
                        Location beamLoc = center.clone().add(0, 5, d);
                        DisplayBuilder.crimsonDust(beamLoc, 3, 0.5);
                    }
                }
            }

            // Follow-up trident spread (tick 50)
            if (beamFired && ticksAlive == 50) {
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI / 4) * i;
                    Location tLoc = center.clone().add(Math.cos(angle) * 2, 8, Math.sin(angle) * 2);
                    DisplayBuilder.crimsonDust(tLoc, 5, 0.3);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneBeamCannon(plugin); }
    }

    // ================================================================
    // #159 — CRIMSON TORNADO TRIDENTS
    // Spinning vortex of tridents orbiting outward
    // ================================================================
    public static class CrimsonTornadoTridents extends BossAttack {
        private final List<BlockDisplayHandle> tornadoHandles = new ArrayList<>();
        private boolean active = false;

        public CrimsonTornadoTridents(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crimson_tornado", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 20, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 5 && !active) {
                active = true;
                // 16 tridents in initial tight orbit
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI / 16) * i;
                    Location tLoc = center.clone().add(Math.cos(angle) * 2, 3, Math.sin(angle) * 2);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    tornadoHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.7f);
            }

            // Spiral outward while spinning (5-120 ticks)
            if (active && ticksAlive > 5 && ticksAlive < 120) {
                int elapsed = ticksAlive - 5;
                double spinSpeed = 0.15 + elapsed * 0.002;
                float expandRadius = 2.0f + elapsed * 0.15f;
                float yOscillation = (float)Math.sin(elapsed * 0.1) * 2;

                for (int i = 0; i < tornadoHandles.size(); i++) {
                    double angle = (2 * Math.PI / 16) * i + elapsed * spinSpeed;
                    Location orbLoc = center.clone().add(
                        Math.cos(angle) * expandRadius, 3 + yOscillation + (i % 4), Math.sin(angle) * expandRadius);
                    tornadoHandles.get(i).entity().teleport(orbLoc);
                    if (elapsed % 4 == 0) {
                        DisplayBuilder.crimsonDust(orbLoc, 2, 0.2);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonTornadoTridents(plugin); }
    }

    // ================================================================
    // #160 — TRIDENT METEOR SHOWER
    // Large brimstone meteors that split into trident clusters
    // ================================================================
    public static class TridentMeteorShower extends BossAttack {
        private int meteorsFired = 0;

        public TridentMeteorShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_meteor_shower", AttackType.BOSS, 5), "calamitas");
            config.setDamage(44.0); // 22 hearts
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(48.0); // 24 hearts
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 35, 0), 20, 5.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Launch 4 meteors sequentially
            int[] meteorTicks = { 10, 40, 70, 100 };
            for (int i = 0; i < meteorTicks.length; i++) {
                if (ticksAlive == meteorTicks[i] && meteorsFired <= i) {
                    meteorsFired++;
                    double x = (Math.random() - 0.5) * 16;
                    double z = (Math.random() - 0.5) * 16;
                    Location meteorLoc = center.clone().add(x, 35, z);
                    BlockDisplayHandle meteor = displayBuilder.spawnBlock(meteorLoc, Material.MAGMA_BLOCK);
                    meteor.scale(2.0f, 2.0f, 2.0f).glow(255, 100, 0).interpolation(3, 0);
                    spawnedEntities.add(meteor.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
                }
            }

            // Animate meteors falling
            for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                Location loc = spawnedEntities.get(i).getLocation();
                if (loc.getY() > center.getY() + 1) {
                    loc.subtract(0, 2.0, 0);
                    spawnedEntities.get(i).teleport(loc);
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.crimsonDust(loc, 8, 1.0);
                        DisplayBuilder.crimsonDust(loc, 3, 0.5);
                    }
                } else if (loc.getY() <= center.getY() + 1) {
                    // Impact — split into 6 trident fragments
                    triggerImpactDamage(loc);
                    DisplayBuilder.crimsonDust(loc, 1, 3.0);
                    DisplayBuilder.crimsonDust(loc, 30, 4.0);
                    DisplayBuilder.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
                    for (int j = 0; j < 6; j++) {
                        double angle = (Math.PI / 3) * j;
                        Location fragmentLoc = loc.clone().add(Math.cos(angle) * 2, 2, Math.sin(angle) * 2);
                        DisplayBuilder.crimsonDust(fragmentLoc, 5, 0.3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentMeteorShower(plugin); }
    }
}
