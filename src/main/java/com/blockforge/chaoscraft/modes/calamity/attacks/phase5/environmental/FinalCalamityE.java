package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;

/**
 * Phase 4E Environmental -- FINAL CALAMITY E
 * Attacks #191-200: The shadow stops, death resonance pillar cracks, sigil goes dark,
 * 3 seconds absolute silence, first thing that happens (aftermath), island final
 * transformation, streams return soft, egg re-activates, title card, final state.
 *
 * Design notes:
 * - Calamitas palette: crimson (200,0,50), orange (255,100,0), soul blue (0,150,255), purple (128,0,255)
 * - NO DAMAGE in any of these events (#191-200)
 * - These are the death and aftermath sequence -- the island watching her die
 */
public final class FinalCalamityE {

    private FinalCalamityE() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TheShadowStops(plugin));
        registry.register(new DeathResonancePillarCracks(plugin));
        registry.register(new SigilGoesDark(plugin));
        registry.register(new AbsoluteSilence(plugin));
        registry.register(new TheFirstThing(plugin));
        registry.register(new IslandFinalTransformation(plugin));
        registry.register(new StreamsReturnSoft(plugin));
        registry.register(new EggReactivates(plugin));
        registry.register(new WhatThePlayersSee(plugin));
        registry.register(new TheFinalState(plugin));
    }

    // =========================================================================
    // 191. THE SHADOW STOPS -- Calamitas' shadow disconnects from her body
    // =========================================================================
    public static class TheShadowStops extends EnvironmentalAttack {
        private boolean stopped = false;
        public TheShadowStops(ChaosCraftPlugin p) { super(p, new AttackConfig("the_shadow_stops", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (!stopped) {
                stopped = true;
                // Shadow stays at current position -- black dust drifts upward
                BlockDisplayHandle shadow = displayBuilder.spawnBlock(center.clone().add(0, 0.1, 0), Material.BLACK_CONCRETE);
                shadow.scale(1.5f, 0.05f, 1.5f).interpolation(5, 0);
                spawnedEntities.add(shadow.entity());
            }
            // Shadow substance bleeding into air -- no sound
            if (t % 5 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.3, 0), 3, 0.5, 0, 0, 0, 1.0f);
            }
        }
        @Override protected void onCleanup() { /* Shadow stays permanently */ }
        @Override public AbstractAttack newInstance() { return new TheShadowStops(plugin); }
    }

    // =========================================================================
    // 192. DEATH RESONANCE PILLAR CRACKS
    // =========================================================================
    public static class DeathResonancePillarCracks extends EnvironmentalAttack {
        private boolean cracked = false;
        public DeathResonancePillarCracks(ChaosCraftPlugin p) { super(p, new AttackConfig("death_resonance_pillar_cracks", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(160); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Pillar location (offset from arena)
            Location pillarPos = center.clone().add(20, 8, 0);
            if (!cracked) {
                cracked = true;
                DisplayBuilder.playSound(pillarPos, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.4f);
            }
            if (t % 4 == 0) {
                DisplayBuilder.dustParticles(pillarPos, 20, 0.5, 200, 150, 80, 2.0f);
                w.spawnParticle(Particle.BLOCK, pillarPos, 5, 0.3, 0.5, 0.3, 0, Material.STONE.createBlockData());
            }
            // Settling
            if (t % 16 == 0 && t > 20) {
                DisplayBuilder.playSound(pillarPos, Sound.BLOCK_STONE_STEP, 0.2f, 0.2f);
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DeathResonancePillarCracks(plugin); }
    }

    // =========================================================================
    // 193. SIGIL GOES DARK -- crying obsidian circuit extinguished
    // =========================================================================
    public static class SigilGoesDark extends EnvironmentalAttack {
        private boolean darkened = false;
        public SigilGoesDark(ChaosCraftPlugin p) { super(p, new AttackConfig("sigil_goes_dark", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(100); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (!darkened) {
                darkened = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 0.4f);
            }
            // Weeping obsidian tears -- minimal
            if (t % 8 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = (2 * Math.PI * i) / 12;
                    Location nodePos = center.clone().add(Math.cos(a) * 5, 0.2, Math.sin(a) * 5);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, nodePos, 2, 0.1, 0.1, 0.1, 0);
                }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SigilGoesDark(plugin); }
    }

    // =========================================================================
    // 194. 3 SECONDS OF ABSOLUTE SILENCE -- everything stops
    // =========================================================================
    public static class AbsoluteSilence extends EnvironmentalAttack {
        public AbsoluteSilence(ChaosCraftPlugin p) { super(p, new AttackConfig("absolute_silence", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(60); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            // Nothing. No particles. No sound. No animation.
            // For exactly 60 ticks the arena is motionless and silent.
            // Only Calamitas herself is active.
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new AbsoluteSilence(plugin); }
    }

    // =========================================================================
    // 195. THE FIRST THING THAT HAPPENS -- frozen particles resume, single rod rises
    // =========================================================================
    public static class TheFirstThing extends EnvironmentalAttack {
        public TheFirstThing(ChaosCraftPlugin p) { super(p, new AttackConfig("the_first_thing", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Frozen particles resume -- slow drift for 20 ticks
            if (t <= 20 && t % 4 == 0) {
                for (int i = 0; i < 5; i++) {
                    Location driftPos = center.clone().add((Math.random()-0.5)*20, 15+Math.random()*10, (Math.random()-0.5)*20);
                    w.spawnParticle(Particle.END_ROD, driftPos, 2, 0.2, 0.2, 0.2, 0.001);
                }
            }
            // 10 ticks of empty sky (t=20-30)
            // Single END_ROD rises from center (t=30+)
            if (t >= 30) {
                double height = (t - 30) * 0.5;
                if (t == 30) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BIT, 0.4f, 2.0f);
                }
                if (t % 2 == 0 && height <= 80) {
                    w.spawnParticle(Particle.END_ROD, center.clone().add(0, height, 0), 1, 0, 0, 0, 0);
                }
                // Reaches Y+80, expands radial pulse
                if (height >= 80 && t % 2 == 0) {
                    double pulseRadius = (t - 190) * 0.5;
                    if (pulseRadius > 0 && pulseRadius <= 20) {
                        DisplayBuilder.particleRing(center.clone().add(0, 80, 0), pulseRadius,
                                Particle.END_ROD, (int)(pulseRadius * 3), null);
                    }
                }
                if (t == 190) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                }
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new TheFirstThing(plugin); }
    }

    // =========================================================================
    // 196. ISLAND FINAL TRANSFORMATION -- floor bleaches warm white
    // =========================================================================
    public static class IslandFinalTransformation extends EnvironmentalAttack {
        public IslandFinalTransformation(ChaosCraftPlugin p) { super(p, new AttackConfig("island_final_transformation", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Smoke and dust residuals fade
            if (t <= 120 && t % 6 == 0) {
                for (int i = 0; i < 3; i++) {
                    Location smokePos = center.clone().add((Math.random()-0.5)*14, 0.3, (Math.random()-0.5)*14);
                    double fade = 1.0 - t / 120.0;
                    w.spawnParticle(Particle.SMOKE, smokePos, (int)(3 * fade), 0.3, 0.2, 0.3, 0.005);
                }
            }
            // White light drifting inward from void edges
            if (t % 4 == 0) {
                for (int edge = 0; edge < 4; edge++) {
                    Location edgePos;
                    switch (edge) {
                        case 0: edgePos = center.clone().add(-8, 0.5, (Math.random()-0.5)*14); break;
                        case 1: edgePos = center.clone().add(8, 0.5, (Math.random()-0.5)*14); break;
                        case 2: edgePos = center.clone().add((Math.random()-0.5)*14, 0.5, -8); break;
                        default: edgePos = center.clone().add((Math.random()-0.5)*14, 0.5, 8); break;
                    }
                    DisplayBuilder.dustParticles(edgePos, 5, 0.5, 255, 255, 255, 0.5f);
                }
            }
            // Sigil node tears
            if (t % 8 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = (2*Math.PI*i)/8;
                    Location nodePos = center.clone().add(Math.cos(a)*5, 0.2, Math.sin(a)*5);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, nodePos, 2, 0.1, 0.1, 0.1, 0);
                }
            }
            if (t == 5) DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.3f, 0.4f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IslandFinalTransformation(plugin); }
    }

    // =========================================================================
    // 197. STREAMS RETURN SOFT -- 15% density, 50% speed, desaturated
    // =========================================================================
    public static class StreamsReturnSoft extends EnvironmentalAttack {
        public StreamsReturnSoft(ChaosCraftPlugin p) { super(p, new AttackConfig("streams_return_soft", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(300); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Soft desaturated streams at 15% density, 50% speed
            if (t % 8 == 0) {
                for (int i = 0; i < 3; i++) {
                    Location pos = center.clone().add((Math.random()-0.5)*20, 15+Math.random()*15, (Math.random()-0.5)*20);
                    // Desaturated colors
                    switch ((int)(Math.random()*5)) {
                        case 0: DisplayBuilder.dustParticles(pos, 2, 0.5, 200, 180, 230, 1.0f); break; // lavender (was magenta)
                        case 1: DisplayBuilder.dustParticles(pos, 2, 0.5, 255, 240, 180, 1.0f); break; // pale gold (was orange-gold)
                        case 2: w.spawnParticle(Particle.END_ROD, pos, 2, 0.3, 0.3, 0.3, 0.001); break; // white unchanged
                        case 3: w.spawnParticle(Particle.DRAGON_BREATH, pos, 2, 0.5, 0.5, 0.5, 0.001); break; // lighter purple
                        default: DisplayBuilder.dustParticles(pos, 2, 0.5, 180, 120, 120, 1.0f); break; // dusty rose (was crimson)
                    }
                }
            }
            if (t == 5) DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.3f, 1.0f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new StreamsReturnSoft(plugin); }
    }

    // =========================================================================
    // 198. EGG RE-ACTIVATES -- single slow pulse, then stillness
    // =========================================================================
    public static class EggReactivates extends EnvironmentalAttack {
        public EggReactivates(ChaosCraftPlugin p) { super(p, new AttackConfig("egg_reactivates", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(120); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // Single slow pulse: scale 1.0 -> 1.6 (20 ticks) -> 1.0 (20 ticks)
            if (t == 20) {
                // Pulse peak -- dragon_breath expansion
                w.spawnParticle(Particle.DRAGON_BREATH, center.clone().add(0, 1, 0), 100, 5.0, 2.0, 5.0, 0.01);
                DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.8f, 0.6f);
            }
            // End_rod orbit for 60 ticks
            if (t >= 20 && t <= 80 && t % 2 == 0) {
                double orbitAngle = t * Math.toRadians(6);
                Location orbitPos = center.clone().add(Math.cos(orbitAngle), 1, Math.sin(orbitAngle));
                w.spawnParticle(Particle.END_ROD, orbitPos, 3, 0.1, 0.1, 0.1, 0.005);
            }
            // After: stillness
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new EggReactivates(plugin); }
    }

    // =========================================================================
    // 199. WHAT THE PLAYERS SEE -- title card and completion
    // =========================================================================
    public static class WhatThePlayersSee extends EnvironmentalAttack {
        private boolean titleSent = false;
        public WhatThePlayersSee(ChaosCraftPlugin p) { super(p, new AttackConfig("what_the_players_see", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(200); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            if (!titleSent) {
                titleSent = true;
                for (Player p : w.getPlayers()) {
                    p.sendTitle(
                            "\u00A7f\u00A7lC A L A M I T Y",
                            "\u00A77It ends with you.",
                            20, 100, 40
                    );
                }
            }
            // Achievement sound at title fade
            if (t == 160) {
                DisplayBuilder.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new WhatThePlayersSee(plugin); }
    }

    // =========================================================================
    // 200. THE FINAL STATE -- the island at rest (not an attack, just state)
    // =========================================================================
    public static class TheFinalState extends EnvironmentalAttack {
        public TheFinalState(ChaosCraftPlugin p) { super(p, new AttackConfig("the_final_state", AttackType.ENVIRONMENTAL, 5)); config.setDamage(0.0); config.setDurationTicks(600); config.setCooldownTicks(99999); config.setTicksBetweenDamage(999); }
        @Override protected void onSpawn(Location c) {}
        @Override protected void onTick(int t) {
            Location center = getCenter(); if (center == null) return; World w = center.getWorld(); if (w == null) return;
            // The natural ambient of The End, undistorted
            if (t == 5) DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.25f, 1.0f);

            // Soft desaturated streams continue at minimum
            if (t % 10 == 0) {
                for (int i = 0; i < 2; i++) {
                    Location pos = center.clone().add((Math.random()-0.5)*20, 15+Math.random()*15, (Math.random()-0.5)*20);
                    switch ((int)(Math.random()*5)) {
                        case 0: DisplayBuilder.dustParticles(pos, 1, 0.5, 200, 180, 230, 0.8f); break;
                        case 1: DisplayBuilder.dustParticles(pos, 1, 0.5, 255, 240, 180, 0.8f); break;
                        case 2: w.spawnParticle(Particle.END_ROD, pos, 1, 0.2, 0.2, 0.2, 0.001); break;
                        case 3: w.spawnParticle(Particle.DRAGON_BREATH, pos, 1, 0.3, 0.3, 0.3, 0.001); break;
                        default: DisplayBuilder.dustParticles(pos, 1, 0.5, 180, 120, 120, 0.8f); break;
                    }
                }
            }

            // White void edge light
            if (t % 6 == 0) {
                for (int edge = 0; edge < 4; edge++) {
                    Location edgePos;
                    switch (edge) {
                        case 0: edgePos = center.clone().add(-8, 0.5, (Math.random()-0.5)*14); break;
                        case 1: edgePos = center.clone().add(8, 0.5, (Math.random()-0.5)*14); break;
                        case 2: edgePos = center.clone().add((Math.random()-0.5)*14, 0.5, -8); break;
                        default: edgePos = center.clone().add((Math.random()-0.5)*14, 0.5, 8); break;
                    }
                    DisplayBuilder.dustParticles(edgePos, 3, 0.3, 255, 255, 255, 0.5f);
                }
            }

            // Sigil tears
            if (t % 10 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = (2*Math.PI*i)/6;
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, center.clone().add(Math.cos(a)*5, 0.2, Math.sin(a)*5),
                            2, 0.1, 0.1, 0.1, 0);
                }
            }

            // Void holes still glow
            if (t % 8 == 0) {
                w.spawnParticle(Particle.PORTAL, center.clone().add(3, -1, 3), 5, 0.3, 1.0, 0.3, 0.005);
                w.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(3, 0.5, 3), 3, 0.3, 0.3, 0.3, 0.005);
            }

            // Echo figures gentle bob
            if (t % 6 == 0) {
                for (int i = 0; i < 7; i++) {
                    double a = (2*Math.PI*i)/7;
                    Location figPos = center.clone().add(Math.cos(a)*6, 1.0 + 0.5*Math.sin(t*0.05), Math.sin(a)*6);
                    DisplayBuilder.dustParticles(figPos, 3, 0.2, 200, 200, 220, 0.8f);
                }
            }

            // Shadow remains
            if (t % 10 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.2, 0), 1, 0.3, 0, 0, 0, 0.5f);
            }
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new TheFinalState(plugin); }
    }
}
