package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Supreme Calamitas — Phase 4: "The End of Everything" (24%-0% HP)
 * Attacks #171-180
 *
 * Ground devastation, arena destruction, and tracking horrors.
 * Damage: 40-80 hearts. Maximum lethality.
 * Calamitas colors: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255), purple(128,0,255)
 *
 * NO status effects — damage only.
 */
public final class CalamitasEndC {

    private CalamitasEndC() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TridentNetVolley(plugin));
        registry.register(new BrimstoneFloorEruption(plugin));
        registry.register(new AbsoluteTridentField(plugin));
        registry.register(new CrownDescentHammer(plugin));
        registry.register(new ArenaCrack(plugin));
        registry.register(new BrimstoneStomp(plugin));
        registry.register(new CorruptionPulse(plugin));
        registry.register(new DescentSlam(plugin));
        registry.register(new GroundFissure(plugin));
        registry.register(new ThousandEyes(plugin));
    }

    // ================================================================
    // #171 — TRIDENT NET VOLLEY
    // Dense crosshatch pattern of tridents forming a lethal net
    // ================================================================
    public static class TridentNetVolley extends BossAttack {
        private boolean fired = false;

        public TridentNetVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_net", AttackType.BOSS, 5), "calamitas");
            config.setDamage(44.0); // 22 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(120);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 10, 0), 20, 5.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 5 && !fired) {
                fired = true;
                // Horizontal row from north
                for (int i = 0; i < 10; i++) {
                    double x = (i - 4.5) * 2.0;
                    Location tLoc = center.clone().add(x, 3, -10);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(trident.entity());
                }
                // Vertical row from east
                for (int i = 0; i < 10; i++) {
                    double z = (i - 4.5) * 2.0;
                    Location tLoc = center.clone().add(10, 3, z);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(255, 100, 0).interpolation(2, 0);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.8f);
            }

            // Animate: north row flies south, east row flies west (5-20 ticks)
            if (fired && ticksAlive > 5 && ticksAlive < 20) {
                for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                    if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                    Location loc = spawnedEntities.get(i).getLocation();
                    double relX = loc.getX() - center.getX();
                    if (Math.abs(relX) > 9) { // East row -> west
                        loc.add(-2.0, 0, 0);
                    } else { // North row -> south
                        loc.add(0, 0, 2.0);
                    }
                    spawnedEntities.get(i).teleport(loc);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentNetVolley(plugin); }
    }

    // ================================================================
    // #172 — BRIMSTONE FLOOR ERUPTION
    // Floor erupts in cascading sections across the arena
    // ================================================================
    public static class BrimstoneFloorEruption extends BossAttack {
        private int sectionsErupted = 0;

        public BrimstoneFloorEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_floor_eruption", AttackType.BOSS, 5), "calamitas");
            config.setDamage(48.0); // 24 hearts
            config.setDamageRadius(3.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(48.0);
            config.setImpactRadius(3.5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 20, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Erupt 5 sections sequentially
            if (ticksAlive >= 10 && (ticksAlive - 10) % 20 == 0 && sectionsErupted < 5) {
                sectionsErupted++;
                double sectionX = (sectionsErupted - 3) * 4.0;
                // Eruption strip across Z axis
                for (int z = -8; z <= 8; z += 4) {
                    Location eruptLoc = center.clone().add(sectionX, 0.1, z);
                    BlockDisplayHandle pillar = displayBuilder.spawnBlock(eruptLoc, Material.MAGMA_BLOCK);
                    pillar.scale(3.0f, 4.0f, 3.0f).glow(255, 100, 0).interpolation(3, 0);
                    spawnedEntities.add(pillar.entity());
                    triggerImpactDamage(eruptLoc);
                    DisplayBuilder.crimsonDust(eruptLoc, 15, 2.0);
                    DisplayBuilder.crimsonDust(eruptLoc, 10, 1.5);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneFloorEruption(plugin); }
    }

    // ================================================================
    // #173 — ABSOLUTE TRIDENT FIELD
    // Every position within the arena has a trident within 2 blocks
    // ================================================================
    public static class AbsoluteTridentField extends BossAttack {
        private boolean deployed = false;

        public AbsoluteTridentField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_absolute_field", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 25, 0), 30, 10.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 15 && !deployed) {
                deployed = true;
                // Dense 5x5 grid at altitude = 25 tridents
                for (int gx = 0; gx < 5; gx++) {
                    for (int gz = 0; gz < 5; gz++) {
                        double x = (gx - 2) * 4.0;
                        double z = (gz - 2) * 4.0;
                        Location tLoc = center.clone().add(x, 25 + Math.random() * 5, z);
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                        trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                        spawnedEntities.add(trident.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.7f);
            }

            // All drop simultaneously (15-30 ticks)
            if (deployed && ticksAlive > 15 && ticksAlive < 30) {
                for (int i = spawnedEntities.size() - 1; i >= 0; i--) {
                    if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                    Location loc = spawnedEntities.get(i).getLocation();
                    loc.subtract(0, 2.5, 0);
                    spawnedEntities.get(i).teleport(loc);
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.crimsonDust(loc, 2, 0.3);
                    }
                }
            }

            // Impact (tick 30)
            if (deployed && ticksAlive == 30) {
                for (int gx = 0; gx < 5; gx++) {
                    for (int gz = 0; gz < 5; gz++) {
                        double x = (gx - 2) * 4.0;
                        double z = (gz - 2) * 4.0;
                        triggerImpactDamage(center.clone().add(x, 0.1, z));
                    }
                }
                DisplayBuilder.crimsonDust(center, 3, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbsoluteTridentField(plugin); }
    }

    // ================================================================
    // #174 — CROWN DESCENT HAMMER
    // Crown structure drops to Y+5, hammering ground with brimstone
    // ================================================================
    public static class CrownDescentHammer extends BossAttack {
        private boolean descended = false;

        public CrownDescentHammer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crown_hammer", AttackType.BOSS, 5), "calamitas");
            config.setDamage(56.0); // 28 hearts
            config.setDamageRadius(5.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(56.0);
            config.setImpactRadius(6.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 30, 8.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Crown descends (10-25 ticks = fast)
            if (ticksAlive >= 10 && ticksAlive < 25 && !descended) {
                float yPos = 20 - (ticksAlive - 10) * 1.3f;
                // Crown visual at current height
                if (ticksAlive % 2 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (2 * Math.PI / 12) * i;
                        Location crownLoc = center.clone().add(Math.cos(angle) * 8, yPos, Math.sin(angle) * 8);
                        DisplayBuilder.crimsonDust(crownLoc, 5, 0.5);
                    }
                }
            }

            // Impact (tick 25)
            if (ticksAlive == 25 && !descended) {
                descended = true;
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 2, 3.0);
                DisplayBuilder.crimsonDust(center, 60, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                // Crown structure at ground level
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI / 12) * i;
                    Location crownLoc = center.clone().add(Math.cos(angle) * 8, 2, Math.sin(angle) * 8);
                    BlockDisplayHandle spike = displayBuilder.spawnBlock(crownLoc, Material.CRIMSON_HYPHAE);
                    spike.scale(0.5f, 3.0f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                    spawnedEntities.add(spike.entity());
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrownDescentHammer(plugin); }
    }

    // ================================================================
    // #175 — ARENA CRACK
    // Cross-shaped crack divides arena into quadrants, erupts brimstone
    // ================================================================
    public static class ArenaCrack extends BossAttack {
        private boolean cracked = false;

        public ArenaCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_arena_crack", AttackType.BOSS, 5), "calamitas");
            config.setDamage(60.0); // 30 hearts fall
            config.setDamageRadius(2.0);
            config.setDurationTicks(6000); // Permanent
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0); // 20 hearts eruption
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 4, 0), 20, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-20 ticks = 1 second)
            if (ticksAlive < 20) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 3, 0), 10, 1.5);
                }
                return;
            }

            // Slam and crack (tick 20)
            if (ticksAlive == 20 && !cracked) {
                cracked = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                DisplayBuilder.crimsonDust(center, 1, 1.0);
                DisplayBuilder.crimsonDust(center, 40, 3.0);
                // Crack lines in 4 cardinal directions
                for (int dir = 0; dir < 4; dir++) {
                    double angle = (Math.PI / 2) * dir;
                    for (int d = 1; d <= 10; d++) {
                        Location crackLoc = center.clone().add(
                            Math.cos(angle) * d, 0.02, Math.sin(angle) * d);
                        BlockDisplayHandle crack = displayBuilder.spawnBlock(crackLoc, Material.NETHERRACK);
                        crack.scale(1.2f, 0.04f, 0.6f).glow(200, 0, 50).interpolation(3, 0);
                        spawnedEntities.add(crack.entity());
                    }
                }
            }

            // Eruption at T+0.5s (tick 30)
            if (cracked && ticksAlive == 30) {
                for (int dir = 0; dir < 4; dir++) {
                    double angle = (Math.PI / 2) * dir;
                    for (int d = 2; d <= 10; d += 2) {
                        Location eruptLoc = center.clone().add(Math.cos(angle) * d, 0.1, Math.sin(angle) * d);
                        triggerImpactDamage(eruptLoc);
                        DisplayBuilder.crimsonDust(eruptLoc, 10, 1.0);
                        DisplayBuilder.crimsonDust(eruptLoc, 8, 1.0);
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.6f);
            }

            // Re-eruptions every 160 ticks (8 seconds)
            if (cracked && ticksAlive > 30 && (ticksAlive - 30) % 160 == 0) {
                for (int dir = 0; dir < 4; dir++) {
                    double angle = (Math.PI / 2) * dir;
                    for (int d = 2; d <= 10; d += 3) {
                        Location eruptLoc = center.clone().add(Math.cos(angle) * d, 0.5, Math.sin(angle) * d);
                        DisplayBuilder.crimsonDust(eruptLoc, 8, 1.0);
                        DisplayBuilder.crimsonDust(eruptLoc, 5, 0.8);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ArenaCrack(plugin); }
    }

    // ================================================================
    // #176 — BRIMSTONE STOMP
    // 3 rapid shockwave rings, jump-timing required
    // ================================================================
    public static class BrimstoneStomp extends BossAttack {
        private int stompsFired = 0;

        public BrimstoneStomp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_stomp", AttackType.BOSS, 5), "calamitas");
            config.setDamage(52.0); // 26 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(120);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) { /* NO telegraph */ }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 3 stomps at 30-tick (1.5s) intervals
            int[] stompTicks = { 0, 30, 60 };
            for (int i = 0; i < 3; i++) {
                if (ticksAlive == stompTicks[i] && stompsFired <= i) {
                    stompsFired++;
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.6f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_NETHERRACK_BREAK, 1.0f, 0.8f);
                }
                // Animate expanding ring
                int ringAge = ticksAlive - stompTicks[i];
                if (stompsFired > i && ringAge > 0 && ringAge < 35) {
                    float radius = ringAge * 0.4f; // 6 blocks/second
                    float dustSize = 1.5f + i * 0.3f; // Rings get bigger
                    if (ringAge % 2 == 0) {
                        int segments = Math.max(8, (int)(radius * 4));
                        for (int s = 0; s < segments; s++) {
                            double angle = (2 * Math.PI / segments) * s;
                            Location ringLoc = center.clone().add(
                                Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius);
                            DisplayBuilder.crimsonDust(ringLoc, 2, 0.3);
                            DisplayBuilder.crimsonDust(ringLoc, 1, 0.2);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneStomp(plugin); }
    }

    // ================================================================
    // #177 — CORRUPTION PULSE
    // 8-block sustained radiation field, 10 seconds, stationary DPS window
    // ================================================================
    public static class CorruptionPulse extends BossAttack {
        private boolean pulsing = false;

        public CorruptionPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_corruption_pulse", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0); // 8 hearts per second
            config.setDamageRadius(8.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.darkPurpleDust(center, 15, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.7f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !pulsing) {
                pulsing = true;
            }

            // Radiation field (10-210 ticks = 10 seconds)
            if (pulsing && ticksAlive >= 10 && ticksAlive < 210) {
                if (ticksAlive % 3 == 0) {
                    // Dense body radiation
                    DisplayBuilder.darkPurpleDust(center, 15, 2.5);
                    // 8-block boundary sphere
                    for (int i = 0; i < 8; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        Location boundLoc = center.clone().add(
                            Math.cos(angle) * 8, Math.random() * 4, Math.sin(angle) * 8);
                        DisplayBuilder.purpleDust(boundLoc, 2, 0.3);
                        DisplayBuilder.crimsonDust(boundLoc, 1, 0.2);
                    }
                    // Floor particles
                    DisplayBuilder.purpleDust(center, 5, 4.0);
                }
            }

            // End of pulse
            if (pulsing && ticksAlive == 210) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorruptionPulse(plugin); }
    }

    // ================================================================
    // #178 — THE DESCENT SLAM
    // Maximum single-hit: 40 hearts, dive-bomb from Y+30
    // ================================================================
    public static class DescentSlam extends BossAttack {
        private boolean diving = false;

        public DescentSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_descent_slam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(80.0); // 40 hearts direct
            config.setDamageRadius(5.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(80.0);
            config.setImpactRadius(5.0);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            // 1.5 second telegraph — hovers at Y+30
            Location elevated = center.clone().add(0, 30, 0);
            DisplayBuilder.crimsonDust(elevated, 30, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Hover telegraph (0-30 ticks = 1.5s)
            if (ticksAlive < 30 && !diving) {
                Location elevated = center.clone().add(0, 30, 0);
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(elevated, 20, 2.0);
                }
            }

            // Dive (tick 30-46 = 0.8 seconds)
            if (ticksAlive == 30 && !diving) {
                diving = true;
                DisplayBuilder.playSound(center.clone().add(0, 30, 0), Sound.ENTITY_WITHER_DEATH, 0.5f, 0.5f);
            }
            if (diving && ticksAlive >= 30 && ticksAlive < 46) {
                float yPos = 30 - (ticksAlive - 30) * 1.875f; // 30 blocks in 16 ticks
                if (ticksAlive % 2 == 0) {
                    Location diveLoc = center.clone().add(0, yPos, 0);
                    DisplayBuilder.crimsonDust(diveLoc, 10, 1.0);
                    DisplayBuilder.crimsonDust(diveLoc, 8, 0.5);
                }
            }

            // Impact (tick 46)
            if (diving && ticksAlive == 46) {
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 2, 3.0);
                DisplayBuilder.crimsonDust(center, 60, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                // Crater
                BlockDisplayHandle crater = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.02, 0), Material.CRACKED_STONE_BRICKS);
                crater.scale(5.0f, 0.04f, 5.0f).glow(200, 0, 50).interpolation(5, 0);
                spawnedEntities.add(crater.entity());
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DescentSlam(plugin); }
    }

    // ================================================================
    // #179 — GROUND FISSURE
    // Tracking ground fissure that curves toward target
    // ================================================================
    public static class GroundFissure extends BossAttack {
        private boolean fissureActive = false;

        public GroundFissure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_ground_fissure", AttackType.BOSS, 5), "calamitas");
            config.setDamage(44.0); // 22 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(5);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            // NO telegraph
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !fissureActive) {
                fissureActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.6f);
            }

            // Fissure travels toward target (0-60 ticks = 3 seconds at ~5 blocks/second)
            if (fissureActive && ticksAlive < 60) {
                // Fissure head
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.crimsonDust(center, 5, 0.5);
                    DisplayBuilder.crimsonDust(center, 8, 1.0);
                    // Trail
                    BlockDisplayHandle fissureSeg = displayBuilder.spawnBlock(
                        center.clone().add(0, 0.02, 0), Material.MAGMA_BLOCK);
                    fissureSeg.scale(1.0f, 0.04f, 0.3f).glow(255, 100, 0).interpolation(2, 0);
                    spawnedEntities.add(fissureSeg.entity());
                }

                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.6f, 1.2f);
                }
            }

            // Wall eruption if fissure reaches edge
            if (fissureActive && ticksAlive == 60) {
                DisplayBuilder.crimsonDust(center, 3, 1.5);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_HURT, 0.8f, 0.7f);
                DisplayBuilder.playSound(center, Sound.BLOCK_NETHERRACK_BREAK, 0.7f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundFissure(plugin); }
    }

    // ================================================================
    // #180 — THOUSAND EYES
    // 12 tracking crimson orbs, 15 seconds duration, detonate at end
    // ================================================================
    public static class ThousandEyes extends BossAttack {
        private final List<BlockDisplayHandle> orbHandles = new ArrayList<>();
        private boolean orbsActive = false;

        public ThousandEyes(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_thousand_eyes", AttackType.BOSS, 5), "calamitas");
            config.setDamage(8.0); // 4 hearts per second per orb
            config.setDamageRadius(2.0);
            config.setDurationTicks(340);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // NO telegraph
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !orbsActive) {
                orbsActive = true;
                for (int i = 0; i < 12; i++) {
                    double angle = (2 * Math.PI / 12) * i;
                    Location orbLoc = center.clone().add(Math.cos(angle) * 8, 18, Math.sin(angle) * 8);
                    BlockDisplayHandle orb = displayBuilder.spawnBlock(orbLoc, Material.REDSTONE_BLOCK);
                    orb.scale(0.8f, 0.8f, 0.8f).glow(255, 0, 40).interpolation(3, 0);
                    orbHandles.add(orb);
                    spawnedEntities.add(orb.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.5f);
            }

            // Orbs track players (0-300 ticks = 15 seconds)
            if (orbsActive && ticksAlive < 300) {
                for (BlockDisplayHandle orb : orbHandles) {
                    if (orb.entity() == null || !orb.entity().isValid()) continue;
                    Location orbLoc = orb.entity().getLocation();
                    // Slow tracking toward target
                    Vector dir = center.toVector().subtract(orbLoc.toVector()).normalize();
                    dir.multiply(0.15); // Slightly slower than player sprint
                    orbLoc.add(dir);
                    orb.entity().teleport(orbLoc);
                    if (ticksAlive % 5 == 0) {
                        DisplayBuilder.crimsonDust(orbLoc, 3, 0.8);
                        DisplayBuilder.crimsonDust(orbLoc, 2, 0.3);
                    }
                }
                // Ambient chime
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f,
                        0.5f + (ticksAlive % 12) * 0.08f);
                }
            }

            // Detonation (tick 300)
            if (orbsActive && ticksAlive == 300) {
                for (BlockDisplayHandle orb : orbHandles) {
                    if (orb.entity() == null || !orb.entity().isValid()) continue;
                    Location orbLoc = orb.entity().getLocation();
                    triggerImpactDamage(orbLoc);
                    DisplayBuilder.crimsonDust(orbLoc, 1, 1.5);
                    DisplayBuilder.crimsonDust(orbLoc, 20, 3.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThousandEyes(plugin); }
    }
}
