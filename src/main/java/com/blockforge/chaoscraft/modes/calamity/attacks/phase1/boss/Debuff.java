package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 Boss Attacks — GROUP 8: DEBUFF / STATUS (status-free damage version)
 * 10 attrition-style attacks replacing all status effects with damage-over-time zones,
 * pulsing damage fields, and high-DPS contact areas.
 * Attacks 71-80 from boss1-voidmaw.md.
 * NO status effects — all replaced with damage mechanics.
 */
public final class Debuff {

    private Debuff() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidBreath(plugin));
        registry.register(new SensoryPulse(plugin));
        registry.register(new LifeLeech(plugin));
        registry.register(new FearWave(plugin));
        registry.register(new VoidRot(plugin));
        registry.register(new MindShatter(plugin));
        registry.register(new SiphonAura(plugin));
        registry.register(new EntropyField(plugin));
        registry.register(new TheLongNight(plugin));
        registry.register(new VoidPlague(plugin));
    }

    // ================================================================
    // 71. VOID BREATH — Miasma cloud covers 50% of island, DoT inside
    // ================================================================
    public static class VoidBreath extends BossAttack {
        private final List<BlockDisplayHandle> miasmaHandles = new ArrayList<>();

        public VoidBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_breath", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Crack glows for 2-second telegraph
            for (int i = 0; i < 8; i++) {
                double a = (Math.PI * 2 / 8) * i;
                Location loc = center.clone().add(Math.cos(a) * 6, 0.1, Math.sin(a) * 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 0.1f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                miasmaHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            // Dense miasma cloud blocks at varying heights
            for (int x = -10; x <= 2; x += 2) {
                for (int z = -10; z <= 10; z += 2) {
                    for (int y = 1; y <= 5; y += 2) {
                        Location loc = center.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                        h.scale(1.6f, 1.6f, 1.6f).glow(80, 0, 160).interpolation(4, 0);
                        miasmaHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            // Miasma drifts slowly
            if (ticksAlive % 20 == 0) {
                for (BlockDisplayHandle h : miasmaHandles) {
                    Location loc = h.entity().getLocation();
                    double drift = Math.sin(ticksAlive * 0.05) * 0.1;
                    h.entity().teleport(loc.clone().add(drift, 0, 0));
                }
                DisplayBuilder.purpleDust(center.clone().add(-4, 3, 0), 10, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidBreath(plugin); }
    }

    // ================================================================
    // 72. SENSORY PULSE — Full-island damage pulse with minimal warning
    //     (Design doc: no damage + pure debuffs → replaced with damage pulse)
    // ================================================================
    public static class SensoryPulse extends BossAttack {
        private final List<BlockDisplayHandle> pulseHandles = new ArrayList<>();
        private boolean pulsed = false;

        public SensoryPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sensory_pulse", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(6.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Very brief flash ring — minimal telegraph (0.5s)
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2 / 16) * i;
                Location loc = center.clone().add(Math.cos(a) * 2, 1.5, Math.sin(a) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(1, 0);
                pulseHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Very short telegraph flash (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.purpleDust(center, 15, 3.0);
                }
            }
            // Pulse fires at tick 10
            else if (ticksAlive == 10 && !pulsed) {
                pulsed = true;
                // Expanding shockwave ring — full island
                for (int r = 1; r <= 20; r++) {
                    for (int i = 0; i < 24; i++) {
                        double a = (Math.PI * 2 / 24) * i;
                        DisplayBuilder.purpleDust(
                            center.clone().add(Math.cos(a) * r, 1.5, Math.sin(a) * r),
                            4, 0.8);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.4f);
                // Remove flash indicators
                for (BlockDisplayHandle h : pulseHandles) h.entity().remove();
                pulseHandles.clear();
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SensoryPulse(plugin); }
    }

    // ================================================================
    // 73. LIFE LEECH — Filaments attach to still players dealing DoT
    // ================================================================
    public static class LifeLeech extends BossAttack {
        private final List<BlockDisplayHandle> filamentHandles = new ArrayList<>();

        public LifeLeech(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("life_leech", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Filament network: thin vertical strands across the island
            for (int i = 0; i < 20; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 10;
                Location loc = center.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                // Base filament attachment point
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc.clone().add(0, 0.05, 0), Material.OBSIDIAN);
                h.scale(0.08f, 1.8f, 0.08f).glow(80, 0, 160).interpolation(2, 0);
                filamentHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Filaments sway subtly
            if (ticksAlive % 8 == 0) {
                float sway = (float)(Math.sin(ticksAlive * 0.12) * 0.06);
                for (BlockDisplayHandle h : filamentHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                        new Vector3f(-0.04f, 0, -0.04f),
                        new AxisAngle4f(sway, 1, 0, 0),
                        new Vector3f(0.08f, 1.8f, 0.08f),
                        new AxisAngle4f(0, 0, 1, 0)));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
            }
            // Drain pulse particles
            if (ticksAlive % 15 == 0) {
                for (int i = 0; i < filamentHandles.size(); i += 3) {
                    DisplayBuilder.purpleDust(filamentHandles.get(i).entity().getLocation(), 2, 0.3);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LifeLeech(plugin); }
    }

    // ================================================================
    // 74. FEAR WAVE — Full-island damage shockwave with pulsing rings
    //     (Design doc: mostly debuffs + 1 heart → upgraded to 6-heart damage wave)
    // ================================================================
    public static class FearWave extends BossAttack {
        private final List<BlockDisplayHandle> waveHandles = new ArrayList<>();
        private boolean waveFired = false;
        private float waveRadius = 1.0f;

        public FearWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fear_wave", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(12.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Inhalation indicator: ring contracting inward
            for (int i = 0; i < 24; i++) {
                double a = (Math.PI * 2 / 24) * i;
                Location loc = center.clone().add(Math.cos(a) * 15, 1.2, Math.sin(a) * 15);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                waveHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Contract inward (0-30 ticks telegraph)
            if (ticksAlive < 30 && !waveFired) {
                float shrink = 15 - (ticksAlive / 30.0f) * 14;
                for (int i = 0; i < waveHandles.size(); i++) {
                    double a = (Math.PI * 2 / waveHandles.size()) * i;
                    Location newLoc = center.clone().add(
                        Math.cos(a) * shrink, 1.2, Math.sin(a) * shrink);
                    waveHandles.get(i).entity().teleport(newLoc);
                }
            }
            // Pulse fires at tick 30
            else if (ticksAlive == 30 && !waveFired) {
                waveFired = true;
                waveRadius = 1.0f;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.5f);
                DisplayBuilder.purpleDust(center, 30, 2.0);
            }
            // Expand wave outward
            else if (waveFired && waveRadius < 25) {
                waveRadius += 0.8f;
                for (int i = 0; i < waveHandles.size(); i++) {
                    double a = (Math.PI * 2 / waveHandles.size()) * i;
                    Location newLoc = center.clone().add(
                        Math.cos(a) * waveRadius, 1.2, Math.sin(a) * waveRadius);
                    waveHandles.get(i).entity().teleport(newLoc);
                }
                if ((int) waveRadius % 4 == 0) {
                    DisplayBuilder.purpleDust(center, 10, waveRadius);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FearWave(plugin); }
    }

    // ================================================================
    // 75. VOID ROT — Tracking orb deals stacking DoT damage zones
    //     (Design doc: stacking debuffs → replaced with damage zones per hit)
    // ================================================================
    public static class VoidRot extends BossAttack {
        private BlockDisplayHandle orbHandle;
        private float moveProgress = 0;
        private boolean detonated = false;
        private final double[] targetOffset = {
            (Math.random() - 0.5) * 8, 1.5, (Math.random() - 0.5) * 8
        };

        public VoidRot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_rot", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dark orb drifting toward player
            Location startLoc = center.clone().add(0, 4, 0);
            orbHandle = displayBuilder.spawnBlock(startLoc, Material.OBSIDIAN);
            orbHandle.scale(0.5f, 0.5f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
            spawnedEntities.add(orbHandle.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (!detonated) {
                moveProgress = Math.min(1.0f, ticksAlive / 60.0f);
                // Drift toward target
                Location orbLoc = center.clone().add(
                    targetOffset[0] * moveProgress,
                    targetOffset[1],
                    targetOffset[2] * moveProgress);
                orbHandle.entity().teleport(orbLoc);
                // Trailing particles
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(orbLoc, 3, 0.5);
                }
                // Slow spin
                float angle = ticksAlive * 0.06f;
                BlockDisplay bd = (BlockDisplay) orbHandle.entity();
                bd.setTransformation(new Transformation(
                    new Vector3f(-0.25f, -0.25f, -0.25f),
                    new AxisAngle4f(angle, 0, 1, 0),
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new AxisAngle4f(0, 0, 1, 0)));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }
            // Detonate at tick 60 or on arrival
            if (ticksAlive == 60 && !detonated) {
                detonated = true;
                Location impactLoc = orbHandle.entity().getLocation();
                orbHandle.entity().remove();
                // Spreading rot zone
                for (int i = 0; i < 16; i++) {
                    double a = (Math.PI * 2 / 16) * i;
                    for (int r = 1; r <= 3; r++) {
                        DisplayBuilder.purpleDust(
                            impactLoc.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r), 4, 0.5);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidRot(plugin); }
    }

    // ================================================================
    // 76. MIND SHATTER — Concentric distortion rings, inner zone more damage
    // ================================================================
    public static class MindShatter extends BossAttack {
        private final List<BlockDisplayHandle> innerRingHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> outerRingHandles = new ArrayList<>();
        private boolean shattered = false;

        public MindShatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mind_shatter", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(6.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Build concentric rings outward over 40 ticks
            for (int i = 0; i < 16; i++) {
                double a = (Math.PI * 2 / 16) * i;
                // Inner ring (10-block radius)
                Location inner = center.clone().add(Math.cos(a) * 2, 1.2, Math.sin(a) * 2);
                BlockDisplayHandle h1 = displayBuilder.spawnBlock(inner, Material.OBSIDIAN);
                h1.scale(0.6f, 0.6f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
                innerRingHandles.add(h1);
                spawnedEntities.add(h1.entity());
                // Outer ring (20-block radius)
                Location outer = center.clone().add(Math.cos(a) * 4, 1.2, Math.sin(a) * 4);
                BlockDisplayHandle h2 = displayBuilder.spawnBlock(outer, Material.CRYING_OBSIDIAN);
                h2.scale(0.4f, 0.4f, 0.4f).glow(80, 0, 160).interpolation(2, 0);
                outerRingHandles.add(h2);
                spawnedEntities.add(h2.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Expand rings outward as telegraph (0-40 ticks)
            if (ticksAlive <= 40 && !shattered) {
                float expandFactor = ticksAlive / 40.0f;
                for (int i = 0; i < innerRingHandles.size(); i++) {
                    double a = (Math.PI * 2 / innerRingHandles.size()) * i;
                    float r = 2 + expandFactor * 8;
                    innerRingHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * r, 1.2, Math.sin(a) * r));
                }
                for (int i = 0; i < outerRingHandles.size(); i++) {
                    double a = (Math.PI * 2 / outerRingHandles.size()) * i;
                    float r = 4 + expandFactor * 16;
                    outerRingHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(a) * r, 1.2, Math.sin(a) * r));
                }
            }
            // Shatter at tick 40
            else if (ticksAlive == 40 && !shattered) {
                shattered = true;
                DisplayBuilder.purpleDust(center, 30, 10.0);
                DisplayBuilder.crimsonDust(center, 20, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MindShatter(plugin); }
    }

    // ================================================================
    // 77. SIPHON AURA — Sustained island-wide DoT drain aura
    //     (Design doc: heal reduction → replaced with direct sustained damage)
    // ================================================================
    public static class SiphonAura extends BossAttack {
        private final List<BlockDisplayHandle> auraHandles = new ArrayList<>();

        public SiphonAura(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("siphon_aura", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(60);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Swirling aura around island perimeter
            for (int i = 0; i < 24; i++) {
                double a = (Math.PI * 2 / 24) * i;
                for (int y = 0; y <= 6; y += 2) {
                    Location loc = center.clone().add(Math.cos(a) * 18, y, Math.sin(a) * 18);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(0.4f, 0.8f, 0.4f).glow(80, 0, 160).interpolation(5, 0);
                    auraHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spiral rotation of aura handles
            if (ticksAlive % 5 == 0) {
                float angle = ticksAlive * 0.02f;
                for (int i = 0; i < auraHandles.size(); i++) {
                    double baseAngle = (Math.PI * 2 / 24) * (i / 4) + angle;
                    int y = (i % 4) * 2;
                    Location newLoc = center.clone().add(
                        Math.cos(baseAngle) * 18, y, Math.sin(baseAngle) * 18);
                    auraHandles.get(i).entity().teleport(newLoc);
                }
            }
            // Drain pulse inward
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center, 6, 12.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SiphonAura(plugin); }
    }

    // ================================================================
    // 78. ENTROPY FIELD — 12-block damage zone with aggressive visuals
    //     (Design doc: durability drain → replaced with high-DPS damage field)
    // ================================================================
    public static class EntropyField extends BossAttack {
        private final List<BlockDisplayHandle> fieldHandles = new ArrayList<>();

        public EntropyField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("entropy_field", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Heat-haze dark mote field
            for (int x = -5; x <= 5; x += 2) {
                for (int z = -5; z <= 5; z += 2) {
                    if (x * x + z * z <= 36) {
                        for (int y = 0; y <= 4; y += 2) {
                            Location loc = center.clone().add(x, y, z);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                            float scale = 0.3f + (float)(Math.random() * 0.3);
                            h.scale(scale, scale, scale).glow(128, 0, 255).interpolation(3, 0);
                            fieldHandles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Motes drift and jitter
            if (ticksAlive % 8 == 0) {
                for (BlockDisplayHandle h : fieldHandles) {
                    Location loc = h.entity().getLocation();
                    double jx = (Math.random() - 0.5) * 0.15;
                    double jy = Math.sin(ticksAlive * 0.08 + Math.random()) * 0.05;
                    double jz = (Math.random() - 0.5) * 0.15;
                    h.entity().teleport(loc.clone().add(jx, jy, jz));
                }
            }
            // Entropy pulse
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center, 8, 5.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EntropyField(plugin); }
    }

    // ================================================================
    // 79. THE LONG NIGHT — Island-wide dark aura with DoT for proximity
    //     (Design doc: darkness + special mechanics → damage-over-time darkness zone)
    // ================================================================
    public static class TheLongNight extends BossAttack {
        private final List<BlockDisplayHandle> nightHandles = new ArrayList<>();

        public TheLongNight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_long_night", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Massive dark cloud descends over island — thick overhead layer
            for (int x = -16; x <= 16; x += 3) {
                for (int z = -16; z <= 16; z += 3) {
                    if (x * x + z * z <= 300) {
                        for (int y = 8; y <= 16; y += 4) {
                            Location loc = center.clone().add(x, y, z);
                            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                            h.scale(2.5f, 2.5f, 2.5f).glow(40, 0, 80).interpolation(6, 0);
                            nightHandles.add(h);
                            spawnedEntities.add(h.entity());
                        }
                    }
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Descend cloud slowly for first 60 ticks
            if (ticksAlive <= 60 && ticksAlive % 5 == 0) {
                for (BlockDisplayHandle h : nightHandles) {
                    Location loc = h.entity().getLocation();
                    h.entity().teleport(loc.clone().add(0, -0.1, 0));
                }
            }
            // Pulsing darkness
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 5, 0), 12, 15.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLongNight(plugin); }
    }

    // ================================================================
    // 80. VOID PLAGUE — Spreading spore cloud with persistent damage
    //     (Design doc: debuff amplifier → replaced with high-DPS spore zone)
    // ================================================================
    public static class VoidPlague extends BossAttack {
        private final List<BlockDisplayHandle> sporeHandles = new ArrayList<>();

        public VoidPlague(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_plague", AttackType.BOSS, 1), "voidmaw");
            config.setDamage(4.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Spore cloud blocks emerging from cracks
            for (int i = 0; i < 20; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 9;
                int yy = (int)(Math.random() * 4);
                Location loc = center.clone().add(Math.cos(a) * r, yy, Math.sin(a) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                float s = 0.4f + (float)(Math.random() * 0.6);
                h.scale(s, s, s).glow(80, 0, 160).interpolation(4, 0);
                sporeHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spores drift outward slowly
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : sporeHandles) {
                    Location loc = h.entity().getLocation();
                    double dx = loc.getX() - center.getX();
                    double dz = loc.getZ() - center.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0 && dist < 12) {
                        h.entity().teleport(loc.clone().add(dx / dist * 0.1, 0.02, dz / dist * 0.1));
                    }
                }
                DisplayBuilder.purpleDust(center, 6, 6.0);
            }
            // Plague pulse
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 1.0f);
                DisplayBuilder.purpleDust(center, 10, 9.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidPlague(plugin); }
    }
}
