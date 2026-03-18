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
 * Attacks #191-200
 *
 * SKY STREAM FINALE (#191-195) + FINAL SEQUENCE (#196-200)
 * Damage: 36-128 hearts. The end of the fight.
 * Calamitas colors: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255), purple(128,0,255)
 *
 * NO status effects — damage only.
 */
public final class CalamitasEndE {

    private CalamitasEndE() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FiveStreamsActive(plugin));
        registry.register(new CurtainOfHeaven(plugin));
        registry.register(new StreamDescent(plugin));
        registry.register(new TheLastSky(plugin));
        registry.register(new StreamSpiral(plugin));
        registry.register(new TheReckoning(plugin));
        registry.register(new Judgment(plugin));
        registry.register(new CalamitasDescends(plugin));
        registry.register(new TheLastCalamity(plugin));
        registry.register(new Requiem(plugin));
    }

    // ================================================================
    // #191 — FIVE STREAMS ACTIVE
    // All 5 sky streams weaponize simultaneously for 10 seconds
    // ================================================================
    public static class FiveStreamsActive extends BossAttack {
        private boolean streamsActive = false;

        public FiveStreamsActive(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_five_streams", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); // Variable per stream
            config.setDamageRadius(2.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            // She raises one hand skyward
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !streamsActive) {
                streamsActive = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.8f, 0.5f);
            }

            // All 5 streams fire weapons (0-200 ticks = 10 seconds)
            if (streamsActive && ticksAlive < 200) {
                // Stream 1 — crimson particle clusters downward every 40 ticks
                if (ticksAlive % 40 == 0) {
                    Location s1 = center.clone().add((Math.random() - 0.5) * 16, 15, (Math.random() - 0.5) * 16);
                    DisplayBuilder.crimsonDust(s1, 10, 1.0);
                }
                // Stream 2 — lateral brimstone bolts every 7 ticks
                if (ticksAlive % 7 == 0) {
                    double x = (Math.random() - 0.5) * 16;
                    Location s2 = center.clone().add(x, 15, -10);
                    DisplayBuilder.crimsonDust(s2, 5, 0.5);
                    DisplayBuilder.crimsonDust(s2, 3, 0.3);
                }
                // Stream 3 — void pulses every 60 ticks
                if (ticksAlive % 60 == 0) {
                    Location s3 = center.clone().add((Math.random() - 0.5) * 12, 15, (Math.random() - 0.5) * 12);
                    DisplayBuilder.purpleDust(s3, 8, 3.0);
                }
                // Stream 4 — corruption homing spheres every 40 ticks
                if (ticksAlive % 40 == 0) {
                    Location s4 = center.clone().add((Math.random() - 0.5) * 10, 12, (Math.random() - 0.5) * 10);
                    BlockDisplayHandle sphere = displayBuilder.spawnBlock(s4, Material.CRIMSON_HYPHAE);
                    sphere.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(sphere.entity());
                }
                // Stream 5 — soul fire patches every 20 ticks
                if (ticksAlive % 20 == 0) {
                    double x = (Math.random() - 0.5) * 16;
                    double z = (Math.random() - 0.5) * 16;
                    Location s5 = center.clone().add(x, 0.1, z);
                    DisplayBuilder.crimsonDust(s5, 8, 1.5);
                }

                // Combined overhead chaos
                if (ticksAlive % 5 == 0) {
                    for (int i = 0; i < 3; i++) {
                        Location overLoc = center.clone().add(
                            (Math.random() - 0.5) * 18, 14 + Math.random() * 2, (Math.random() - 0.5) * 18);
                        Particle[] colors = { Particle.FLAME, Particle.SOUL_FIRE_FLAME, Particle.DRAGON_BREATH,
                            Particle.END_ROD, Particle.WITCH };
                        DisplayBuilder.crimsonDust(overLoc, 3, 0.5);
                    }
                }

                // Stream fire sounds
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.3f,
                        0.4f + (ticksAlive % 100) * 0.01f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FiveStreamsActive(plugin); }
    }

    // ================================================================
    // #192 — CURTAIN OF HEAVEN
    // All 5 streams form a particle curtain that sweeps north to south + rebounds
    // ================================================================
    public static class CurtainOfHeaven extends BossAttack {
        private boolean curtainActive = false;

        public CurtainOfHeaven(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_curtain_heaven", AttackType.BOSS, 5), "calamitas");
            config.setDamage(32.0); // 16 hearts per curtain wall contact
            config.setDamageRadius(2.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !curtainActive) {
                curtainActive = true;
            }

            // Forward sweep (10-210 ticks = 10 seconds, north to south)
            if (curtainActive && ticksAlive >= 10 && ticksAlive < 210) {
                float progress = (ticksAlive - 10) / 200.0f;
                double zPos = -10 + (20 * progress);
                if (ticksAlive % 2 == 0) {
                    for (int x = -10; x <= 10; x += 2) {
                        for (int y = 0; y < 15; y += 2) {
                            Location curtainLoc = center.clone().add(x, y, zPos);
                            int colorChoice = (x + y) % 5;
                            int r = colorChoice < 2 ? 200 : colorChoice < 3 ? 160 : colorChoice < 4 ? 80 : 200;
                            int g = colorChoice < 2 ? 20 : colorChoice < 3 ? 0 : colorChoice < 4 ? 0 : 100;
                            int b = colorChoice < 2 ? 20 : colorChoice < 3 ? 80 : colorChoice < 4 ? 40 : 0;
                            DisplayBuilder.crimsonDust(curtainLoc, 2, 0.5);
                        }
                    }
                }
            }

            // Rebound sweep (220-330 ticks, south to north at 2.4x speed)
            if (curtainActive && ticksAlive >= 220 && ticksAlive < 330) {
                float progress = (ticksAlive - 220) / 110.0f;
                double zPos = 10 - (20 * progress);
                if (ticksAlive % 2 == 0) {
                    for (int x = -10; x <= 10; x += 2) {
                        for (int y = 0; y < 15; y += 2) {
                            Location curtainLoc = center.clone().add(x, y, zPos);
                            DisplayBuilder.crimsonDust(curtainLoc, 2, 0.5);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CurtainOfHeaven(plugin); }
    }

    // ================================================================
    // #193 — STREAM DESCENT
    // Streams drop from Y+15 to Y+6, forces continuous jumping
    // ================================================================
    public static class StreamDescent extends BossAttack {
        private boolean descended = false;

        public StreamDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_stream_descent", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0); // 10 hearts/second
            config.setDamageRadius(2.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(640);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Descent (10-70 ticks = 3 seconds)
            if (ticksAlive >= 10 && ticksAlive < 70) {
                float progress = (ticksAlive - 10) / 60.0f;
                double currentY = 15 - (progress * 9); // 15 -> 6
                if (ticksAlive % 3 == 0) {
                    for (int s = 0; s < 5; s++) {
                        double zOffset = (s - 2) * 4.0;
                        for (int x = -8; x <= 8; x += 4) {
                            Location streamLoc = center.clone().add(x, currentY, zOffset);
                            DisplayBuilder.crimsonDust(streamLoc, 3, 1.5);
                            DisplayBuilder.crimsonDust(streamLoc, 1, 0.3);
                        }
                    }
                }
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.4f,
                        (float)(1.0 - progress * 0.6));
                }
            }

            // Hold at Y+6 (70-230 ticks = 8 seconds)
            if (ticksAlive >= 70 && ticksAlive < 230) {
                if (!descended) {
                    descended = true;
                }
                if (ticksAlive % 4 == 0) {
                    for (int s = 0; s < 5; s++) {
                        double zOffset = (s - 2) * 4.0;
                        for (int x = -8; x <= 8; x += 4) {
                            Location streamLoc = center.clone().add(x, 6, zOffset);
                            DisplayBuilder.crimsonDust(streamLoc, 3, 1.5);
                            DisplayBuilder.crimsonDust(streamLoc, 1, 0.3);
                        }
                    }
                    // Waist-height fog
                    DisplayBuilder.crimsonDust(center.clone().add(0, 6, 0), 5, 8.0);
                }
            }

            // Ascent back to Y+15 (230-260 ticks)
            if (ticksAlive >= 230 && ticksAlive < 260) {
                float progress = (ticksAlive - 230) / 30.0f;
                double currentY = 6 + (progress * 9);
                if (ticksAlive % 3 == 0) {
                    for (int x = -8; x <= 8; x += 6) {
                        DisplayBuilder.crimsonDust(center.clone().add(x, currentY, 0), 3, 1.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamDescent(plugin); }
    }

    // ================================================================
    // #194 — THE LAST SKY
    // 4-second silence then maximum density burst
    // ================================================================
    public static class TheLastSky extends BossAttack {
        private boolean silencePhase = false;
        private boolean burstFired = false;

        public TheLastSky(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_last_sky", AttackType.BOSS, 5), "calamitas");
            config.setDamage(52.0); // 26 hearts per burst particle
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            // Streams decelerate (no spawn particles — unsettling)
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Deceleration (0-40 ticks = 2 seconds)
            // Silence (40-120 ticks = 4 seconds)
            if (ticksAlive >= 40 && ticksAlive < 120) {
                // Eerie silence — no particles, no sound
                silencePhase = true;
            }

            // BURST (tick 120)
            if (ticksAlive == 120 && !burstFired) {
                burstFired = true;
                // Maximum density — all 5 stream colors at 50x density in 1-second pulse
                for (int i = 0; i < 50; i++) {
                    double x = (Math.random() - 0.5) * 20;
                    double z = (Math.random() - 0.5) * 20;
                    double y = Math.random() * 15;
                    Location burstLoc = center.clone().add(x, y, z);
                    Particle[] allParticles = { Particle.FLAME, Particle.SOUL_FIRE_FLAME,
                        Particle.DRAGON_BREATH, Particle.END_ROD, Particle.WITCH };
                    DisplayBuilder.crimsonDust(burstLoc, 5, 1.0);
                }
                // Flash
                DisplayBuilder.purpleDust(center, 50, 10.0);
                // Sound explosion
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f);
            }

            // Continued burst particles (120-140 ticks = 1 second)
            if (burstFired && ticksAlive > 120 && ticksAlive < 140) {
                for (int i = 0; i < 20; i++) {
                    double x = (Math.random() - 0.5) * 20;
                    double z = (Math.random() - 0.5) * 20;
                    double y = Math.random() * 15;
                    Location burstLoc = center.clone().add(x, y, z);
                    DisplayBuilder.crimsonDust(burstLoc, 3, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLastSky(plugin); }
    }

    // ================================================================
    // #195 — STREAM SPIRAL
    // 5 streams rotate into spiral, converge on center at T=12
    // ================================================================
    public static class StreamSpiral extends BossAttack {
        private boolean spiraling = false;

        public StreamSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_stream_spiral", AttackType.BOSS, 5), "calamitas");
            config.setDamage(36.0); // 18 hearts per bolt
            config.setDamageRadius(2.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) { /* NO telegraph */ }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !spiraling) {
                spiraling = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.8f, 0.3f);
            }

            // Spiral tightens over 240 ticks (12 seconds)
            if (spiraling && ticksAlive < 240) {
                float progress = ticksAlive / 240.0f;
                double radius = 10 - (progress * 10); // 10 -> 0
                double rotation = ticksAlive * 0.05; // 0.5 RPM

                if (ticksAlive % 3 == 0) {
                    for (int s = 0; s < 5; s++) {
                        double streamAngle = (2 * Math.PI / 5) * s + rotation;
                        Location streamLoc = center.clone().add(
                            Math.cos(streamAngle) * radius, 15, Math.sin(streamAngle) * radius);
                        int r = s == 0 ? 200 : s == 1 ? 160 : s == 2 ? 128 : s == 3 ? 255 : 200;
                        int g = s == 0 ? 0 : s == 1 ? 0 : s == 2 ? 0 : s == 3 ? 100 : 100;
                        int b = s == 0 ? 50 : s == 1 ? 80 : s == 2 ? 255 : s == 3 ? 0 : 0;
                        DisplayBuilder.crimsonDust(streamLoc, 5, 1.0);
                    }
                }

                // Stream weapon bolts every 20 ticks
                if (ticksAlive % 20 == 0 && ticksAlive > 0) {
                    for (int s = 0; s < 5; s++) {
                        double streamAngle = (2 * Math.PI / 5) * s + rotation;
                        Location boltLoc = center.clone().add(
                            Math.cos(streamAngle) * radius, 15, Math.sin(streamAngle) * radius);
                        DisplayBuilder.crimsonDust(boltLoc, 8, 0.5);
                    }
                }

                // Pitch rises as spiral tightens
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.3f,
                        0.3f + progress * 1.5f);
                }
            }

            // Center convergence (tick 240) — 5x18 = 90 hearts at center
            if (spiraling && ticksAlive == 240) {
                triggerImpactDamage(center.clone().add(0, 10, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 10, 0), 3, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.7f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamSpiral(plugin); }
    }

    // ================================================================
    // #196 — THE RECKONING (Scripted)
    // At 10% HP: all attacks stop, she ascends to Y+50, 10s silence
    // ================================================================
    public static class TheReckoning extends BossAttack {
        public TheReckoning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_the_reckoning", AttackType.BOSS, 5), "calamitas");
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(340);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) { }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ascent (0-60 ticks = 3 seconds)
            if (ticksAlive < 60) {
                float yPos = (ticksAlive / 60.0f) * 50;
                if (ticksAlive % 4 == 0) {
                    Location ascentLoc = center.clone().add(0, yPos, 0);
                    DisplayBuilder.purpleDust(ascentLoc, 5, 1.0);
                    DisplayBuilder.crimsonDust(ascentLoc, 5, 1.5);
                }
                if (ticksAlive == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.3f);
                }
            }

            // Hover at Y+50 (60-260 ticks = 10 seconds)
            if (ticksAlive >= 60 && ticksAlive < 260) {
                Location hovLoc = center.clone().add(0, 50, 0);
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.crimsonDust(hovLoc, 5, 2.5);
                }
                // Silence. Absolute silence.
            }

            // Title card (tick 260)
            if (ticksAlive == 260) {
                // Title: "The Reckoning."
                // Subtitle: "She judges you."
                // (Handled by mode manager / title packet system)
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheReckoning(plugin); }
    }

    // ================================================================
    // #197 — JUDGMENT (Scripted)
    // 100 tridents in a Fibonacci sphere from Y+50
    // ================================================================
    public static class Judgment extends BossAttack {
        private boolean fired = false;

        public Judgment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_judgment", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts per trident
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(3);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            // 2-second telegraph — arms out, 100 tridents materialize
            Location sky = center.clone().add(0, 50, 0);
            DisplayBuilder.crimsonDust(sky, 30, 5.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location sky = center.clone().add(0, 50, 0);

            // Telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40) {
                if (ticksAlive % 2 == 0) {
                    double goldenRatio = (1 + Math.sqrt(5)) / 2;
                    for (int i = 0; i < 100; i++) {
                        double theta = Math.acos(1 - 2.0 * (i + 0.5) / 100);
                        double phi = 2 * Math.PI * i / goldenRatio;
                        Location tLoc = sky.clone().add(
                            Math.sin(theta) * Math.cos(phi) * 3,
                            Math.sin(theta) * Math.sin(phi) * 3,
                            Math.cos(theta) * 3);
                        DisplayBuilder.crimsonDust(tLoc, 1, 0.1);
                    }
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.playSound(sky, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f,
                        0.5f + (ticksAlive / 40.0f) * 1.0f);
                }
                return;
            }

            // FIRE (tick 40)
            if (ticksAlive == 40 && !fired) {
                fired = true;
                // 100 tridents in Fibonacci sphere
                double goldenRatio = (1 + Math.sqrt(5)) / 2;
                for (int i = 0; i < 100; i++) {
                    double theta = Math.acos(1 - 2.0 * (i + 0.5) / 100);
                    double phi = 2 * Math.PI * i / goldenRatio;
                    Location tLoc = sky.clone().add(
                        Math.sin(theta) * Math.cos(phi) * 3,
                        Math.sin(theta) * Math.sin(phi) * 3,
                        Math.cos(theta) * 3);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.1f, 0.1f, 0.8f).glow(200, 0, 0).interpolation(2, 0);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.crimsonDust(sky, 2, 3.0);
                DisplayBuilder.playSound(sky, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 0.6f);
                DisplayBuilder.playSound(sky, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.7f);
            }

            // Tridents radiate outward + gravity pulls them down (40-80 ticks)
            if (fired && ticksAlive > 40 && ticksAlive < 80) {
                double goldenRatio = (1 + Math.sqrt(5)) / 2;
                int elapsed = ticksAlive - 40;
                for (int i = 0; i < Math.min(100, spawnedEntities.size()); i++) {
                    if (spawnedEntities.get(i) == null || !spawnedEntities.get(i).isValid()) continue;
                    double theta = Math.acos(1 - 2.0 * (i + 0.5) / 100);
                    double phi = 2 * Math.PI * i / goldenRatio;
                    float dist = elapsed * 1.5f;
                    float yOffset = (float)(Math.sin(theta) * Math.sin(phi) * dist) - elapsed * 0.8f;
                    Location flyLoc = sky.clone().add(
                        Math.sin(theta) * Math.cos(phi) * dist,
                        yOffset,
                        Math.cos(theta) * dist);
                    spawnedEntities.get(i).teleport(flyLoc);
                    if (elapsed % 3 == 0 && i % 5 == 0) {
                        DisplayBuilder.crimsonDust(flyLoc, 2, 0.1);
                        DisplayBuilder.crimsonDust(flyLoc, 1, 0.1);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Judgment(plugin); }
    }

    // ================================================================
    // #198 — CALAMITAS DESCENDS (Scripted)
    // 15-second slow descent from Y+50 to Y+0, no attacks
    // ================================================================
    public static class CalamitasDescends extends BossAttack {
        public CalamitasDescends(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_descends", AttackType.BOSS, 5), "calamitas");
            config.setDamage(0);
            config.setDamageRadius(0);
            config.setDurationTicks(340);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) { }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Slow descent over 300 ticks (15 seconds)
            if (ticksAlive < 300) {
                float yPos = 50 - (ticksAlive / 300.0f) * 50;
                Location descendLoc = center.clone().add(0, yPos, 0);

                // Air steps
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(descendLoc, Sound.BLOCK_STONE_STEP, 0.3f, 0.3f);
                }

                // Eye glow at Y+40
                if (yPos <= 40 && yPos > 38 && ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(descendLoc.clone().add(0, 1.5, 0), 5, 0.3);
                }

                // Crown descent particles
                if (yPos <= 20 && ticksAlive % 5 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (2 * Math.PI / 12) * i;
                        Location crownLoc = descendLoc.clone().add(Math.cos(angle) * 5, 20, Math.sin(angle) * 5);
                        DisplayBuilder.purpleDust(crownLoc, 2, 0.3);
                        DisplayBuilder.crimsonDust(crownLoc, 2, 0.3);
                    }
                }

                // Trident collection at Y+5
                if (yPos <= 5 && yPos > 3 && ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(descendLoc, 10, 3.0);
                    DisplayBuilder.playSound(descendLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.3f, 0.6f);
                }

                // Y+20 beacon tone
                if (yPos <= 20 && yPos > 18 && ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 0.5f);
                }
            }

            // Landing (tick 300)
            if (ticksAlive == 300) {
                DisplayBuilder.crimsonDust(center, 1, 0.5);
                DisplayBuilder.playSound(center, Sound.BLOCK_NETHERRACK_BREAK, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CalamitasDescends(plugin); }
    }

    // ================================================================
    // #199 — THE LAST CALAMITY (Scripted)
    // 10 seconds of EVERYTHING at maximum output simultaneously
    // ================================================================
    public static class TheLastCalamity extends BossAttack {
        private boolean active = false;

        public TheLastCalamity(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_last_calamity", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // All Phase 4 damage values
            config.setDamageRadius(3.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) { }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !active) {
                active = true;
                // Burst orbiting tridents
                for (int i = 0; i < 20; i++) {
                    double angle = (2 * Math.PI / 20) * i;
                    Location tLoc = center.clone().add(Math.cos(angle) * 3, 2, Math.sin(angle) * 3);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    spawnedEntities.add(trident.entity());
                }
            }

            // 10 seconds of everything (0-200 ticks)
            if (active && ticksAlive < 200) {
                // TRIDENTS: 10 per second sustained fire
                if (ticksAlive % 2 == 0) {
                    Location tLoc = center.clone().add(
                        (Math.random() - 0.5) * 4, 5 + Math.random() * 3, (Math.random() - 0.5) * 4);
                    DisplayBuilder.crimsonDust(tLoc, 3, 0.3);
                }

                // BEAMS: 4 cardinal rotating at 45deg/second
                if (ticksAlive % 3 == 0) {
                    double beamAngle = ticksAlive * (Math.PI / 80); // 45deg/second
                    for (int dir = 0; dir < 4; dir++) {
                        double angle = beamAngle + (Math.PI / 2) * dir;
                        for (int d = 2; d <= 10; d += 2) {
                            Location beamLoc = center.clone().add(Math.cos(angle) * d, 3, Math.sin(angle) * d);
                            DisplayBuilder.crimsonDust(beamLoc, 2, 0.3);
                        }
                    }
                }

                // SKULL PROJECTILES: every 40 ticks
                if (ticksAlive % 40 == 0) {
                    for (int i = 0; i < 6; i++) {
                        Location skullLoc = center.clone().add(
                            (Math.random() - 0.5) * 8, 6, (Math.random() - 0.5) * 8);
                        DisplayBuilder.purpleDust(skullLoc, 8, 1.0);
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.5f, 0.7f);
                }

                // GROUND FIRE: soul fire on movement path
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(
                        (Math.random() - 0.5) * 3, 0.1, (Math.random() - 0.5) * 3), 5, 0.5);
                }

                // STREAM WEAPONIZATION: max density
                if (ticksAlive % 4 == 0) {
                    for (int s = 0; s < 4; s++) {
                        double zOff = (s - 1.5) * 5;
                        Location streamLoc = center.clone().add(
                            (Math.random() - 0.5) * 16, 15, zOff);
                        DisplayBuilder.crimsonDust(streamLoc, 3, 1.0);
                    }
                }

                // CROWN: massive volley at t=100 (midpoint)
                if (ticksAlive == 100) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (2 * Math.PI / 12) * i;
                        for (int j = 0; j < 5; j++) {
                            Location crownLoc = center.clone().add(
                                Math.cos(angle) * 8, 20 - j * 0.3, Math.sin(angle) * 8);
                            DisplayBuilder.crimsonDust(crownLoc, 5, 0.3);
                        }
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.7f);
                }

                // STRUCTURE DETONATION at t=140
                if (ticksAlive == 140) {
                    DisplayBuilder.purpleDust(center, 200, 15.0); // White flash
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                }

                // ALL particles active
                if (ticksAlive % 2 == 0) {
                    Particle[] allTypes = { Particle.FLAME, Particle.LAVA, Particle.CRIT, Particle.SOUL_FIRE_FLAME,
                        Particle.DRAGON_BREATH, Particle.WITCH, Particle.END_ROD, Particle.SMOKE };
                    for (int i = 0; i < 5; i++) {
                        double x = (Math.random() - 0.5) * 18;
                        double y = Math.random() * 10;
                        double z = (Math.random() - 0.5) * 18;
                        DisplayBuilder.crimsonDust(center.clone().add(x, y, z), 3, 0.5);
                    }
                }

                // Continuous overlapping sounds
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.3f, 1.0f);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.2f, 0.7f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.3f, 0.8f);
                    DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.3f, 0.7f);
                }
            }

            // T=200: SILENCE. Everything stops.
            if (active && ticksAlive == 200) {
                // Immediate and complete audio cut
                // Title: "THE LAST CALAMITY" — red, massive, centered, pulsing
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLastCalamity(plugin); }
    }

    // ================================================================
    // #200 — REQUIEM (Scripted — The Final Attack)
    // 20-second wind-up, pillar of converged energy, layered detonation
    // ================================================================
    public static class Requiem extends BossAttack {
        private boolean windingUp = false;
        private boolean detonated = false;

        public Requiem(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_requiem", AttackType.BOSS, 5), "calamitas");
            config.setDamage(128.0); // 64 hearts (Layer 1 max)
            config.setDamageRadius(12.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            // She kneels. The arena is silent.
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !windingUp) {
                windingUp = true;
            }

            // Wind-up phase (0-400 ticks = 20 seconds)
            if (windingUp && !detonated && ticksAlive < 400) {
                // Hands glow white (0-100 ticks = 5 seconds)
                if (ticksAlive < 100) {
                    float intensity = ticksAlive / 100.0f;
                    if (ticksAlive % 4 == 0) {
                        DisplayBuilder.crimsonDust(center.clone().add(0, 2.5, 0), (int)(5 + intensity * 15), 0.5);
                    }
                }

                // Streams converge (100-160 ticks)
                if (ticksAlive >= 100 && ticksAlive < 160) {
                    float convergence = (ticksAlive - 100) / 60.0f;
                    for (int s = 0; s < 5; s++) {
                        double angle = (2 * Math.PI / 5) * s;
                        double radius = 8 * (1.0 - convergence);
                        Location streamLoc = center.clone().add(
                            Math.cos(angle) * radius, 12, Math.sin(angle) * radius);
                        int r = (int)(200 * (1 - convergence) + 255 * convergence);
                        int g = (int)(0 * (1 - convergence) + 255 * convergence);
                        int b = (int)(50 * (1 - convergence) + 255 * convergence);
                        DisplayBuilder.crimsonDust(streamLoc, 5, 1.0);
                    }
                }

                // Pillar grows (160-240 ticks)
                if (ticksAlive >= 160 && ticksAlive < 240) {
                    float pillarProgress = (ticksAlive - 160) / 80.0f;
                    double pillarHeight = 8 + pillarProgress * 72; // Y+8 to Y+80
                    double pillarWidth = 1 + pillarProgress * 4; // 1 to 5 blocks
                    if (ticksAlive % 3 == 0) {
                        for (double y = 8; y < pillarHeight; y += 3) {
                            Location pillarLoc = center.clone().add(0, y, 0);
                            DisplayBuilder.purpleDust(pillarLoc, 5, pillarWidth / 2);
                            DisplayBuilder.cyanDust(pillarLoc, 3, pillarWidth / 2);
                            DisplayBuilder.crimsonDust(pillarLoc, 5, (float)(pillarWidth / 2));
                        }
                    }
                }

                // Maximum density (240-300 ticks)
                if (ticksAlive >= 240 && ticksAlive < 300) {
                    if (ticksAlive % 2 == 0) {
                        for (double y = 0; y < 80; y += 4) {
                            Location pillarLoc = center.clone().add(0, y, 0);
                            DisplayBuilder.crimsonDust(pillarLoc, 8, 2.5);
                            DisplayBuilder.purpleDust(pillarLoc, 5, 2.5);
                        }
                    }
                }

                // Heartbeat pulse (300-360 ticks)
                if (ticksAlive >= 300 && ticksAlive < 360) {
                    if (ticksAlive % 20 == 0) {
                        DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 1, 0.5);
                    }
                }

                // Title at T=360: "Requiem."
                if (ticksAlive == 360) {
                    // Title card — plain white, small font, centered
                }
            }

            // DETONATION (tick 400)
            if (windingUp && !detonated && ticksAlive == 400) {
                detonated = true;

                // Pillar collapses in 0.1 seconds (2 ticks) — instantaneous
                // White flash — screen override
                DisplayBuilder.purpleDust(center, 200, 15.0);

                // Layer 1 (0-3 blocks): 64 hearts
                triggerImpactDamage(center);

                // Layer 2 (3-8 blocks): 40 hearts — knockback
                World w = center.getWorld();
                if (w != null) {
                    for (Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        double dist = player.getLocation().distance(center);
                        if (dist > 3 && dist <= 8) {
                            Vector knockback = player.getLocation().toVector()
                                .subtract(center.toVector()).normalize().multiply(2.0);
                            knockback.setY(0.5);
                            player.setVelocity(knockback);
                        }
                    }
                }

                // Layer 3 (8-12 blocks): 22 hearts
                // (Handled by damageRadius = 12.0)

                // Massive sound
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
            }

            // Post-detonation (400-600 ticks = 10 seconds)
            // Arena scoured clean. Pillar disperses from Y+80 downward.
            if (detonated && ticksAlive > 400 && ticksAlive < 600) {
                float disperseProgress = (ticksAlive - 400) / 200.0f;
                double disperseHeight = 80 * (1 - disperseProgress);
                if (ticksAlive % 5 == 0) {
                    for (double y = disperseHeight; y < 80; y += 5) {
                        Location disperseLoc = center.clone().add(0, y, 0);
                        DisplayBuilder.purpleDust(disperseLoc, 3, 2.0);
                        DisplayBuilder.purpleDust(disperseLoc, 2, 1.0);
                    }
                }
            }

            // She collapses. Crown falls. The streams do not return.
            if (detonated && ticksAlive == 600) {
                // Crown pieces fall
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI / 2) * i;
                    Location crownPiece = center.clone().add(Math.cos(angle) * 3, 1, Math.sin(angle) * 3);
                    DisplayBuilder.crimsonDust(crownPiece, 10, 0.5);
                    DisplayBuilder.playSound(crownPiece, Sound.BLOCK_STONE_FALL, 0.3f, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Requiem(plugin); }
    }
}
