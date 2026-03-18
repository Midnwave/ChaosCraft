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
 * Phase 2: "The Weight of Eternity" — HP 74% to 50%
 * Attacks #91-100 — Ground Combos, Uniques, Phase 2 Signature
 *
 * Grab combos. Robe fragments. Temporal echoes. Soul siphon.
 * Brimstone runes. Weight of Names. Fracture points. The Patience.
 * Eternity's Weight — Phase 2 capstone multi-element assault.
 * NO status effects — damage only.
 */
public final class CalamitasEternityE {

    private CalamitasEternityE() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ComboGrabOverheadSlam(plugin));
        registry.register(new RobeFragmentBarrage(plugin));
        registry.register(new EyeOfJudgment(plugin));
        registry.register(new TemporalEcho(plugin));
        registry.register(new SoulSiphonNova(plugin));
        registry.register(new BrimstoneLetter(plugin));
        registry.register(new WeightOfNames(plugin));
        registry.register(new FracturePoint(plugin));
        registry.register(new ThePatienceAttack(plugin));
        registry.register(new EternitysWeight(plugin));
    }

    // ================================================================
    // 91. COMBO: GRAB / OVERHEAD SLAM — Ground phase melee
    // ================================================================
    public static class ComboGrabOverheadSlam extends BossAttack {
        private int phase = 0;

        public ComboGrabOverheadSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_combo_grab_overhead_slam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); config.setDamageRadius(3.0);
            config.setDurationTicks(200); config.setCooldownTicks(500); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_SWOOP, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive == 20 && phase == 0) {
                phase = 1;
                BlockDisplayHandle arm = displayBuilder.spawnBlock(center.clone().add(0, 2, 1), Material.RED_STAINED_GLASS);
                arm.scale(0.4f, 0.4f, 6.0f).glow(180, 0, 0).interpolation(1, 0);
                spawnedEntities.add(arm.entity());
                triggerImpactDamage(center.clone().add(0, 2, 5));
                DisplayBuilder.crimsonDust(center.clone().add(0, 2, 5), 12, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ROAR, 1.3f, 0.9f);
            } else if (ticksAlive == 30 && phase == 1) {
                phase = 2;
                BlockDisplayHandle trident = displayBuilder.spawnBlock(center.clone().add(0, 6, 5), Material.IRON_BLOCK);
                trident.scale(0.3f, 4.0f, 0.3f).glow(220, 220, 255).interpolation(1, 0);
                spawnedEntities.add(trident.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);
            } else if (ticksAlive == 40 && phase == 2) {
                triggerImpactDamage(center.clone().add(0, 0.5, 5));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 5), 20, 3.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.8f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ComboGrabOverheadSlam(plugin); }
    }

    // ================================================================
    // 92. ROBE FRAGMENT BARRAGE — 15 random burning fragments
    // ================================================================
    public static class RobeFragmentBarrage extends BossAttack {
        private boolean fired = false;
        private final double[] fragX = new double[15];
        private final double[] fragZ = new double[15];

        public RobeFragmentBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_robe_fragment_barrage", AttackType.BOSS, 5), "calamitas");
            config.setDamage(12.0); config.setDamageRadius(1.5);
            config.setDurationTicks(200); config.setCooldownTicks(320); config.setTicksBetweenDamage(10);
            for (int i = 0; i < 15; i++) { fragX[i] = (Math.random() - 0.5) * 16; fragZ[i] = (Math.random() - 0.5) * 16; }
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !fired) {
                if (ticksAlive % 3 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 8, 2.0);
            } else if (ticksAlive == 20 && !fired) {
                fired = true;
                for (int i = 0; i < 15; i++) {
                    BlockDisplayHandle frag = displayBuilder.spawnBlock(
                        center.clone().add(0, 20, 0), Material.ORANGE_STAINED_GLASS);
                    frag.scale(0.3f, 0.3f, 0.3f).glow(255, 100, 0).interpolation(1, 0);
                    spawnedEntities.add(frag.entity());
                }
            } else if (fired && ticksAlive < 60) {
                float progress = (ticksAlive - 20) / 40.0f;
                for (int i = 0; i < 15; i++) {
                    Location loc = center.clone().add(fragX[i] * progress, 20 - progress * 20, fragZ[i] * progress);
                    if (i < spawnedEntities.size()) {
                        spawnedEntities.get(i).teleport(loc);
                    }
                }
            } else if (fired && ticksAlive == 60) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 20, 8.0);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new RobeFragmentBarrage(plugin); }
    }

    // ================================================================
    // 93. EYE OF JUDGMENT — Targeted bolt, 14 hearts
    // ================================================================
    public static class EyeOfJudgment extends BossAttack {
        private boolean fired = false;
        private int fireTick = 0;

        public EyeOfJudgment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_eye_of_judgment", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); config.setDamageRadius(2.0);
            config.setDurationTicks(150); config.setCooldownTicks(360); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !fired) {
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 5), 10, 1.5);
            } else if (ticksAlive == 20 && !fired) {
                fired = true; fireTick = ticksAlive;
                BlockDisplayHandle bolt = displayBuilder.spawnBlock(
                    center.clone().add(0, 20, 0), Material.SEA_LANTERN);
                bolt.scale(0.6f, 0.6f, 0.6f).glow(255, 200, 100).interpolation(0, 0);
                spawnedEntities.add(bolt.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.9f, 1.2f);
            } else if (fired && ticksAlive - fireTick < 15) {
                float dist = (ticksAlive - fireTick) * 4.0f;
                if (!spawnedEntities.isEmpty())
                    spawnedEntities.get(0).teleport(center.clone().add(0, 20, dist));
            } else if (fired && ticksAlive - fireTick == 15) {
                triggerImpactDamage(center.clone().add(0, 0.5, 60));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 60), 15, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.8f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new EyeOfJudgment(plugin); }
    }

    // ================================================================
    // 94. TEMPORAL ECHO — Ghost replay of previous position
    // ================================================================
    public static class TemporalEcho extends BossAttack {
        private boolean active = false;

        public TemporalEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_temporal_echo", AttackType.BOSS, 5), "calamitas");
            config.setDamage(12.0); config.setDamageRadius(3.0);
            config.setDurationTicks(200); config.setCooldownTicks(500); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !active) {
                if (ticksAlive % 4 == 0) DisplayBuilder.dustParticles(center.clone().add(0, 20, -5), 8, 2.0, 128, 0, 200, 1.2f);
            } else if (ticksAlive == 20 && !active) {
                active = true;
                BlockDisplayHandle echo = displayBuilder.spawnBlock(
                    center.clone().add(0, 20, -5), Material.PURPLE_STAINED_GLASS);
                echo.scale(2.0f, 3.0f, 2.0f).glow(100, 0, 160).interpolation(2, 0);
                spawnedEntities.add(echo.entity());
            } else if (active && ticksAlive < 50) {
                float progress = (ticksAlive - 20) / 30.0f;
                if (!spawnedEntities.isEmpty())
                    spawnedEntities.get(0).teleport(center.clone().add(0, 20, -5 + progress * 5));
                if (ticksAlive % 6 == 0) DisplayBuilder.dustParticles(center.clone().add(0, 20, -5 + progress * 5), 6, 1.5, 128, 0, 200, 1.0f);
            } else if (active && ticksAlive == 50) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.2f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new TemporalEcho(plugin); }
    }

    // ================================================================
    // 95. SOUL SIPHON NOVA — Inward spiral then outward 10-block burst
    // ================================================================
    public static class SoulSiphonNova extends BossAttack {
        private boolean detonated = false;

        public SoulSiphonNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_soul_siphon_nova", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0); config.setDamageRadius(10.0);
            config.setDurationTicks(200); config.setCooldownTicks(600); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 30 && !detonated) {
                float radius = 15 - (ticksAlive / 30.0f) * 15;
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 15, Math.max(1, radius));
            } else if (ticksAlive == 30 && !detonated) {
                detonated = true;
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 / 12) * i;
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 10, 20, Math.sin(angle) * 10), Material.ORANGE_STAINED_GLASS);
                    ring.scale(2.0f, 2.0f, 2.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(ring.entity());
                }
                triggerImpactDamage(center.clone().add(0, 20, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 30, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.3f, 0.8f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulSiphonNova(plugin); }
    }

    // ================================================================
    // 96. BRIMSTONE LETTER — Rune traced on ground, 15 hp/s in symbol
    // ================================================================
    public static class BrimstoneLetter extends BossAttack {
        private boolean ignited = false;

        public BrimstoneLetter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_letter", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0); config.setDamageRadius(3.0);
            config.setDurationTicks(300); config.setCooldownTicks(560); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 30 && !ignited) {
                // Draw rune: two diagonal lines + vertical bar
                float progress = ticksAlive / 30.0f;
                int segs = (int)(progress * 12);
                for (int s = 0; s < segs; s++) {
                    double x, z;
                    if (s < 4) { x = -3 + s * 1.5; z = -3 + s * 1.5; }
                    else if (s < 8) { x = 3 - (s - 4) * 1.5; z = -3 + (s - 4) * 1.5; }
                    else { x = 0; z = -3 + (s - 8) * 1.5; }
                    DisplayBuilder.crimsonDust(center.clone().add(x, 0.2, z + 5), 4, 0.5);
                }
            } else if (ticksAlive == 30 && !ignited) {
                ignited = true;
                // Ignite the rune shape
                double[][] runePoints = {{-3,-3},{-1.5,-1.5},{0,0},{1.5,1.5},{3,3},{3,-3},{1.5,-1.5},{-1.5,1.5},{-3,3},{0,-3},{0,-1.5},{0,1.5},{0,3}};
                for (double[] pt : runePoints) {
                    BlockDisplayHandle fire = displayBuilder.spawnBlock(
                        center.clone().add(pt[0], 0.1, pt[1] + 5), Material.MAGMA_BLOCK);
                    fire.scale(1.5f, 0.5f, 1.5f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(fire.entity());
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 5));
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, 5), 25, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.7f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneLetter(plugin); }
    }

    // ================================================================
    // 97. WEIGHT OF NAMES — Single-player targeted overhead column
    // ================================================================
    public static class WeightOfNames extends BossAttack {
        private boolean struck = false;

        public WeightOfNames(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_weight_of_names", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); config.setDamageRadius(3.0);
            config.setDurationTicks(200); config.setCooldownTicks(700); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 30 && !struck) {
                // Silence, pause, name display moment
                if (ticksAlive == 10) DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.0f);
            } else if (ticksAlive == 30 && !struck) {
                struck = true;
                Location target = center.clone().add(0, 0, 5);
                for (int y = 0; y < 30; y++) {
                    BlockDisplayHandle col = displayBuilder.spawnBlock(
                        target.clone().add(0, y, 0), Material.ORANGE_STAINED_GLASS);
                    col.scale(3.0f, 1.0f, 3.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(col.entity());
                }
                triggerImpactDamage(target.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(target.clone().add(0, 5, 0), 25, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.4f, 0.6f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new WeightOfNames(plugin); }
    }

    // ================================================================
    // 98. FRACTURE POINT — 5 floating detonation spheres, 6-second timer
    // ================================================================
    public static class FracturePoint extends BossAttack {
        private final List<BlockDisplayHandle> fractureHandles = new ArrayList<>();
        private boolean placed = false;
        private int placeStart = 0;

        public FracturePoint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_fracture_point", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); config.setDamageRadius(3.0);
            config.setDurationTicks(300); config.setCooldownTicks(480); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive == 20 && !placed) {
                placed = true; placeStart = ticksAlive;
                for (int i = 0; i < 5; i++) {
                    double x = (Math.random() - 0.5) * 20;
                    double z = (Math.random() - 0.5) * 20;
                    BlockDisplayHandle sphere = displayBuilder.spawnBlock(
                        center.clone().add(x, 1.5, z), Material.PURPLE_STAINED_GLASS);
                    sphere.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 200).interpolation(2, 0);
                    fractureHandles.add(sphere); spawnedEntities.add(sphere.entity());
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.0f + i * 0.1f);
                }
            } else if (placed && ticksAlive - placeStart < 120) {
                // Spheres hover, pulse
                if (ticksAlive % 20 == 0) {
                    for (int i = 0; i < fractureHandles.size(); i++) {
                        DisplayBuilder.dustParticles(fractureHandles.get(i).entity().getLocation(), 6, 1.0, 128, 0, 200, 1.0f);
                    }
                }
            } else if (placed && ticksAlive - placeStart == 120) {
                // Auto-detonate all
                for (int i = 0; i < fractureHandles.size(); i++) {
                    Location detLoc = fractureHandles.get(i).entity().getLocation();
                    BlockDisplayHandle blast = displayBuilder.spawnBlock(detLoc, Material.ORANGE_STAINED_GLASS);
                    blast.scale(3.0f, 3.0f, 3.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(blast.entity());
                    DisplayBuilder.crimsonDust(detLoc, 12, 3.0);
                }
                triggerImpactDamage(center.clone().add(0, 1, 0));
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FracturePoint(plugin); }
    }

    // ================================================================
    // 99. THE PATIENCE — 8s tracking watch then downward 12-fan burst
    // ================================================================
    public static class ThePatienceAttack extends BossAttack {
        private boolean watching = false;
        private int watchStart = 0;
        private boolean burst = false;

        public ThePatienceAttack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_the_patience", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0); config.setDamageRadius(2.0);
            config.setDurationTicks(400); config.setCooldownTicks(900); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 40) {
                // Descent to Y+8
                if (ticksAlive % 8 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 15, 0), 8, 3.0);
            } else if (ticksAlive == 40 && !watching) {
                watching = true; watchStart = ticksAlive;
            } else if (watching && !burst && ticksAlive - watchStart < 160) {
                // 8 seconds of hovering above player
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 8, 0), 10, 4.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 1.0f);
                }
            } else if (watching && !burst && ticksAlive - watchStart == 160) {
                burst = true;
                // Downward 12-fan
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(-90 + (180.0 / 11) * i);
                    BlockDisplayHandle t = displayBuilder.spawnBlock(
                        center.clone().add(Math.sin(angle) * 2, 8, Math.cos(angle) * 2), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.1f, 0.1f, 0.9f).glow(180, 0, 0).interpolation(1, 0);
                    spawnedEntities.add(t.entity());
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 4, 0), 20, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.85f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 0.85f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ThePatienceAttack(plugin); }
    }

    // ================================================================
    // 100. ETERNITY'S WEIGHT — Phase 2 capstone, 5-element simultaneous
    // ================================================================
    public static class EternitysWeight extends BossAttack {
        private int element = 0;

        public EternitysWeight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_eternitys_weight", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0); config.setDamageRadius(4.0);
            config.setDurationTicks(500); config.setCooldownTicks(1200); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 2-second telegraph (0-40)
            if (ticksAlive < 40) {
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 20, 10.0);
            }
            // Element 1 — Trident Spiral (tick 40)
            else if (ticksAlive == 40 && element == 0) {
                element = 1;
                for (int volley = 0; volley < 6; volley++) {
                    for (int spoke = 0; spoke < 4; spoke++) {
                        double angle = Math.toRadians(45 * volley + 90 * spoke);
                        BlockDisplayHandle t = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * 3, 20, Math.sin(angle) * 3), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                        t.scale(0.08f, 0.08f, 0.6f).glow(220, 220, 255).interpolation(1, 0);
                        spawnedEntities.add(t.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
            }
            // Element 2 — Dual Streams (tick 60)
            else if (ticksAlive == 60 && element == 1) {
                element = 2;
                for (int side = 0; side < 2; side++) {
                    double xOff = (side == 0) ? -3 : 3;
                    for (int seg = 0; seg < 8; seg++) {
                        BlockDisplayHandle s = displayBuilder.spawnBlock(
                            center.clone().add(xOff, 20, -8 + seg * 2), Material.RED_STAINED_GLASS);
                        s.scale(2.0f, 3.0f, 2.0f).glow(180, 0, 0).interpolation(1, 0);
                        spawnedEntities.add(s.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.1f);
            }
            // Element 3 — Fracture Points (tick 80)
            else if (ticksAlive == 80 && element == 2) {
                element = 3;
                for (int i = 0; i < 3; i++) {
                    double x = (Math.random() - 0.5) * 16;
                    double z = (Math.random() - 0.5) * 16;
                    BlockDisplayHandle sphere = displayBuilder.spawnBlock(
                        center.clone().add(x, 1.5, z), Material.PURPLE_STAINED_GLASS);
                    sphere.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 200).interpolation(1, 0);
                    spawnedEntities.add(sphere.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.0f);
            }
            // Element 4 — Ground Score (tick 100)
            else if (ticksAlive == 100 && element == 3) {
                element = 4;
                for (int seg = 0; seg < 10; seg++) {
                    BlockDisplayHandle line = displayBuilder.spawnBlock(
                        center.clone().add(-10 + seg * 2.5, 0.1, -10 + seg * 2.5), Material.ORANGE_STAINED_GLASS);
                    line.scale(2.0f, 0.5f, 2.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(line.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_FALL, 1.0f, 0.8f);
            }
            // Element 5 — Final targeted strike (tick 120)
            else if (ticksAlive == 120 && element == 4) {
                element = 5;
                BlockDisplayHandle bolt = displayBuilder.spawnBlock(
                    center.clone().add(0, 20, 5), Material.SEA_LANTERN);
                bolt.scale(0.8f, 0.8f, 0.8f).glow(255, 200, 100).interpolation(0, 0);
                spawnedEntities.add(bolt.entity());
                triggerImpactDamage(center.clone().add(0, 0.5, 5));
                DisplayBuilder.crimsonDust(center.clone().add(0, 10, 5), 25, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.8f, 0.5f);
            }
            // 6-second attack ends (tick 160) — silence
            else if (ticksAlive == 160) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.6f, 1.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new EternitysWeight(plugin); }
    }
}
