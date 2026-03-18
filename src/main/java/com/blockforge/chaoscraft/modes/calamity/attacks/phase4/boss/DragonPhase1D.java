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
 * Attacks #31-42 (12 attacks)
 * NO status effects — damage only.
 */
public final class DragonPhase1D {

    private DragonPhase1D() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FissureLine(plugin));
        registry.register(new ChromaticOverload(plugin));
        registry.register(new NullOrbit(plugin));
        registry.register(new FracturePointII(plugin));
        registry.register(new VoidSuppressionField(plugin));
        registry.register(new SerratedArc(plugin));
        registry.register(new VoidbornBreathSurge(plugin));
        registry.register(new GravityInversionPocket(plugin));
        registry.register(new FracturePointIII(plugin));
        registry.register(new VoidJudgment(plugin));
        registry.register(new ConsumingDark(plugin));
        registry.register(new PhaseBreakHowl(plugin));
    }

    // ================================================================
    // 31. FISSURE LINE — Erupting wall of PORTAL energy across island
    // ================================================================
    public static class FissureLine extends BossAttack {
        private final List<BlockDisplayHandle> fissureHandles = new ArrayList<>();
        private boolean fissureErupted = false;
        private int eruptTick = 0;

        public FissureLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_fissure_line", AttackType.BOSS, 4), "dragon");
            config.setDamage(14.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(400);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Two parallel ground crack lines from edge to edge
            for (int line = 0; line < 2; line++) {
                float offset = line == 0 ? -4 : 4;
                for (int seg = -15; seg <= 15; seg++) {
                    Location crackPt = center.clone().add(seg * 1.5, 0.05, offset);
                    BlockDisplayHandle crack = displayBuilder.spawnBlock(crackPt, Material.PURPLE_STAINED_GLASS);
                    crack.scale(1.2f, 0.04f, 0.8f).glow(50, 0, 100).interpolation(3, 0);
                    fissureHandles.add(crack);
                    spawnedEntities.add(crack.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ground lines pulse with increasing brightness (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !fissureErupted) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, -4), 8, 20.0);
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 4), 8, 20.0);
                }
            }
            // Fissures erupt (tick 30) — walls of PORTAL energy 10 blocks tall
            else if (ticksAlive == 30 && !fissureErupted) {
                fissureErupted = true;
                eruptTick = ticksAlive;
                for (int line = 0; line < 2; line++) {
                    float offset = line == 0 ? -4 : 4;
                    for (int seg = -10; seg <= 10; seg += 2) {
                        Location wallPt = center.clone().add(seg * 1.5, 0, offset);
                        BlockDisplayHandle wall = displayBuilder.spawnBlock(wallPt, Material.PURPLE_STAINED_GLASS);
                        wall.scale(1.5f, 10.0f, 0.8f).glow(60, 0, 130).interpolation(2, 0);
                        fissureHandles.add(wall);
                        spawnedEntities.add(wall.entity());
                    }
                }
                triggerImpactDamage(center.clone().add(0, 3, -4));
                triggerImpactDamage(center.clone().add(0, 3, 4));
                DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 30, 20.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 3.0f, 0.1f);
            }
            // Walls persist (30-90 ticks = 3 seconds)
            else if (fissureErupted && ticksAlive - eruptTick < 60) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, -4), 6, 15.0);
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 4), 6, 15.0);
                }
            }
            // Walls collapse (tick 90)
            else if (fissureErupted && ticksAlive - eruptTick == 60) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_FALL, 1.5f, 0.5f);
            }
            // Ground trench aftermath (90-190 ticks = 5 seconds)
            else if (fissureErupted && ticksAlive - eruptTick > 60 && ticksAlive - eruptTick < 160) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, -4), 4, 15.0);
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 4), 4, 15.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FissureLine(plugin); }
    }

    // ================================================================
    // 32. CHROMATIC OVERLOAD — Crystal chain detonation with color waves
    // ================================================================
    public static class ChromaticOverload extends BossAttack {
        private final List<BlockDisplayHandle> chromaHandles = new ArrayList<>();
        private boolean chainStarted = false;
        private int chainTick = 0;

        public ChromaticOverload(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_chromatic_overload", AttackType.BOSS, 4), "dragon");
            config.setDamage(12.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Crystal nodes begin color cycling — red/green/blue/pink
            Material[] cycleMats = {Material.RED_STAINED_GLASS, Material.LIME_STAINED_GLASS,
                Material.BLUE_STAINED_GLASS, Material.PINK_STAINED_GLASS};
            for (int i = 0; i < 5; i++) {
                double angle = (Math.PI * 2 / 5) * i;
                Location crystalLoc = center.clone().add(Math.cos(angle) * 14, 1.5, Math.sin(angle) * 14);
                BlockDisplayHandle crystal = displayBuilder.spawnBlock(crystalLoc, cycleMats[i % 4]);
                crystal.scale(1.0f, 1.5f, 1.0f).glow(200, 100, 255).interpolation(2, 0);
                chromaHandles.add(crystal);
                spawnedEntities.add(crystal.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 2.0f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Color cycling escalation (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !chainStarted) {
                if (ticksAlive % 4 == 0) {
                    for (BlockDisplayHandle crystal : chromaHandles) {
                        DisplayBuilder.cyanDust(crystal.entity().getLocation(), 4, 1.0);
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.0f, 2.0f);
                }
            }
            // Chain detonation begins (tick 40) — sequential crystal bursts
            else if (ticksAlive == 40 && !chainStarted) {
                chainStarted = true;
                chainTick = ticksAlive;
            }
            // Sequential crystal detonations (40-100 ticks = 3 seconds, 5 crystals)
            else if (chainStarted && ticksAlive - chainTick < 60) {
                int crystalIdx = (ticksAlive - chainTick) / 12; // ~12 ticks between each
                if ((ticksAlive - chainTick) % 12 == 0 && crystalIdx < 5 && crystalIdx < chromaHandles.size()) {
                    Location detonation = chromaHandles.get(crystalIdx).entity().getLocation();
                    // Expanding wave ring from this crystal
                    for (int ring = 0; ring < 8; ring++) {
                        double ringAngle = (Math.PI * 2 / 8) * ring;
                        Location ringPt = detonation.clone().add(
                            Math.cos(ringAngle) * 4, 1, Math.sin(ringAngle) * 4);
                        BlockDisplayHandle waveSeg = displayBuilder.spawnBlock(ringPt, Material.PINK_STAINED_GLASS);
                        waveSeg.scale(1.5f, 2.0f, 1.5f).glow(255, 0, 100).interpolation(1, 0);
                        chromaHandles.add(waveSeg);
                        spawnedEntities.add(waveSeg.entity());
                    }
                    triggerImpactDamage(detonation);
                    DisplayBuilder.cyanDust(detonation, 15, 4.0);
                    DisplayBuilder.playSound(detonation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.0f);
                }
            }
            // All crystals dark — gray drift (100-160 ticks)
            else if (chainStarted && ticksAlive - chainTick >= 60 && ticksAlive - chainTick < 120) {
                if (ticksAlive % 15 == 0) {
                    for (int i = 0; i < 5 && i < chromaHandles.size(); i++) {
                        DisplayBuilder.cyanDust(
                            chromaHandles.get(i).entity().getLocation().add(0, 1, 0), 3, 1.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ChromaticOverload(plugin); }
    }

    // ================================================================
    // 33. NULL ORBIT — Ground-level blade orbit at 25-block radius
    // ================================================================
    public static class NullOrbit extends BossAttack {
        private final List<BlockDisplayHandle> orbitHandles = new ArrayList<>();
        private boolean orbitLowered = false;
        private int lowerTick = 0;

        public NullOrbit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_null_orbit", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Blade orbit expands to 25-block radius — visible glowing ring
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location bladeLoc = center.clone().add(Math.cos(angle) * 12, 5, Math.sin(angle) * 12);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                blade.scale(0.15f, 0.15f, 1.0f).glow(255, 255, 255).interpolation(2, 0);
                orbitHandles.add(blade);
                spawnedEntities.add(blade.entity());
            }
            // Fill ring with PORTAL at 25-block radius
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location ringPt = center.clone().add(Math.cos(angle) * 25, 5, Math.sin(angle) * 25);
                BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.PURPLE_STAINED_GLASS);
                ring.scale(2.0f, 0.3f, 2.0f).glow(60, 0, 120).interpolation(3, 0);
                orbitHandles.add(ring);
                spawnedEntities.add(ring.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Orbit expands to 25-block radius (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !orbitLowered) {
                float radius = 12.0f + (ticksAlive / 40.0f) * 13.0f;
                double speed = 0.05;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i + ticksAlive * speed;
                    orbitHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 5, Math.sin(angle) * radius));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 8, radius);
                }
            }
            // Orbit descends to ground level (tick 40)
            else if (ticksAlive == 40 && !orbitLowered) {
                orbitLowered = true;
                lowerTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.6f);
            }
            // Ground-level orbit for 8 seconds (40-200 ticks)
            else if (orbitLowered && ticksAlive - lowerTick < 160) {
                double speed = 0.04;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i + ticksAlive * speed;
                    orbitHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 25, 1.5, Math.sin(angle) * 25));
                }
                if (ticksAlive % 6 == 0) {
                    int leadBlade = (int)((ticksAlive * speed / (Math.PI * 2 / 8)) % 8);
                    if (leadBlade >= 0 && leadBlade < orbitHandles.size()) {
                        Location bladeLoc = orbitHandles.get(leadBlade).entity().getLocation();
                        triggerImpactDamage(bladeLoc);
                        DisplayBuilder.cyanDust(bladeLoc, 4, 2.0);
                    }
                }
            }
            // Blades ascend back (tick 200)
            else if (orbitLowered && ticksAlive - lowerTick == 160) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
            }
            // Ground ring trail aftermath (200-280 ticks = 4 seconds)
            else if (orbitLowered && ticksAlive - lowerTick > 160 && ticksAlive - lowerTick < 240) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 8, 25.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NullOrbit(plugin); }
    }

    // ================================================================
    // 34. FRACTURE POINT II — Second summoning seed placement
    // ================================================================
    public static class FracturePointII extends BossAttack {
        private final List<BlockDisplayHandle> fractureHandles = new ArrayList<>();
        private boolean seedPlanted = false;

        public FracturePointII(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_fracture_point_ii", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Identical to Attack #21 — ground cracks at different location
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 / 6) * i;
                Location crackPt = center.clone().add(Math.cos(angle) * 1.5, 0.05, Math.sin(angle) * 1.5);
                BlockDisplayHandle crack = displayBuilder.spawnBlock(crackPt, Material.NETHER_BRICKS);
                crack.scale(0.6f, 0.06f, 0.6f).glow(80, 0, 0).interpolation(3, 0);
                fractureHandles.add(crack);
                spawnedEntities.add(crack.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ground crack pulsing (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !seedPlanted) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 8, 2.0);
                }
            }
            // Seed erupts (tick 40)
            else if (ticksAlive == 40 && !seedPlanted) {
                seedPlanted = true;
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 20, 3.0);
                // 3x3 end stone platform
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockDisplayHandle platform = displayBuilder.spawnBlock(
                            center.clone().add(dx, 0.02, dz), Material.END_STONE);
                        platform.scale(0.9f, 0.1f, 0.9f).glow(60, 50, 50).interpolation(2, 0);
                        fractureHandles.add(platform);
                        spawnedEntities.add(platform.entity());
                    }
                }
                // PORTAL column — 5 blocks tall
                BlockDisplayHandle seedColumn = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.5, 0), Material.PURPLE_STAINED_GLASS);
                seedColumn.scale(0.6f, 5.0f, 0.6f).glow(80, 0, 160).interpolation(4, 0);
                fractureHandles.add(seedColumn);
                spawnedEntities.add(seedColumn.entity());
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 2.0f, 0.7f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.3f);
            }
            // Seed column pulses permanently
            else if (seedPlanted && ticksAlive % 60 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 6, 1.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FracturePointII(plugin); }
    }

    // ================================================================
    // 35. VOID SUPPRESSION FIELD — 30-block cylinder diminishing projectiles
    // ================================================================
    public static class VoidSuppressionField extends BossAttack {
        private final List<BlockDisplayHandle> fieldHandles = new ArrayList<>();
        private boolean fieldActive = false;
        private int fieldTick = 0;

        public VoidSuppressionField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_suppression_field", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon at center, 20 blocks up, wings spread
            // PORTAL particles rain from wing edges
            BlockDisplayHandle dragonSilhouette = displayBuilder.spawnBlock(
                center.clone().add(0, 20, 0), Material.END_STONE);
            dragonSilhouette.scale(8.0f, 1.0f, 4.0f).glow(60, 0, 110).interpolation(4, 0);
            fieldHandles.add(dragonSilhouette);
            spawnedEntities.add(dragonSilhouette.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Wing-edge rain telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !fieldActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(-4, 18, 0), 6, 2.0);
                    DisplayBuilder.cyanDust(center.clone().add(4, 18, 0), 6, 2.0);
                }
            }
            // Suppression field descends (tick 40) — 30-block radius cylinder
            else if (ticksAlive == 40 && !fieldActive) {
                fieldActive = true;
                fieldTick = ticksAlive;
                // Cylinder wall segments at 30-block radius
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    Location wallPt = center.clone().add(Math.cos(angle) * 30, 10, Math.sin(angle) * 30);
                    BlockDisplayHandle wall = displayBuilder.spawnBlock(wallPt, Material.PURPLE_STAINED_GLASS);
                    wall.scale(4.0f, 20.0f, 4.0f).glow(60, 0, 110).interpolation(3, 0);
                    fieldHandles.add(wall);
                    spawnedEntities.add(wall.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT_LAND, 1.5f, 0.5f);
            }
            // Field sustained (40-200 ticks = 8 seconds)
            else if (fieldActive && ticksAlive - fieldTick < 160) {
                if (ticksAlive % 10 == 0) {
                    triggerImpactDamage(center);
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 12, 25.0);
                }
            }
            // Field disperses upward (tick 200) — rockets skyward
            else if (fieldActive && ticksAlive - fieldTick == 160) {
                DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 25, 20.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 1.5f);
            }
            // Dispersal aftermath (200-240 ticks = 2 seconds)
            else if (fieldActive && ticksAlive - fieldTick > 160 && ticksAlive - fieldTick < 200) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 25, 0), 8, 15.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSuppressionField(plugin); }
    }

    // ================================================================
    // 36. SERRATED ARC — 6 blades in horizontal wall sweep
    // ================================================================
    public static class SerratedArc extends BossAttack {
        private final List<BlockDisplayHandle> arcHandles = new ArrayList<>();
        private boolean arcFired = false;
        private int fireTick = 0;

        public SerratedArc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_serrated_arc", AttackType.BOSS, 4), "dragon");
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon banks to side — 6 blades align in a row along body axis
            for (int i = 0; i < 6; i++) {
                Location bladeLoc = center.clone().add(-7.5 + i * 3, 5, -20);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                blade.scale(0.15f, 0.15f, 1.2f).glow(200, 200, 255).interpolation(2, 0);
                arcHandles.add(blade);
                spawnedEntities.add(blade.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Alignment telegraph (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !arcFired) {
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle blade : arcHandles) {
                        DisplayBuilder.cyanDust(blade.entity().getLocation(), 3, 0.5);
                    }
                }
            }
            // Blades fire perpendicular to Dragon's flight (tick 30) — wall sweep
            else if (ticksAlive == 30 && !arcFired) {
                arcFired = true;
                fireTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.8f);
            }
            // Blades sweep across island (30-50 ticks = 1 second medium speed)
            else if (arcFired && ticksAlive - fireTick < 20) {
                float progress = (ticksAlive - fireTick) / 20.0f;
                float zPos = -20 + progress * 45;
                for (int i = 0; i < 6 && i < arcHandles.size(); i++) {
                    arcHandles.get(i).entity().teleport(
                        center.clone().add(-7.5 + i * 3, 4, zPos));
                }
                if (ticksAlive % 3 == 0) {
                    for (BlockDisplayHandle blade : arcHandles) {
                        DisplayBuilder.cyanDust(blade.entity().getLocation(), 3, 0.5);
                        triggerImpactDamage(blade.entity().getLocation());
                    }
                }
            }
            // Blades arc back to Dragon (tick 50)
            else if (arcFired && ticksAlive - fireTick == 20) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);
            }
            // Trail afterimages — 6 parallel lines (50-110 ticks = 3 seconds)
            else if (arcFired && ticksAlive - fireTick > 20 && ticksAlive - fireTick < 80) {
                if (ticksAlive % 10 == 0) {
                    for (int i = 0; i < 6; i++) {
                        DisplayBuilder.cyanDust(
                            center.clone().add(-7.5 + i * 3, 4, 0), 3, 20.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SerratedArc(plugin); }
    }

    // ================================================================
    // 37. VOIDBORN BREATH SURGE — Ground-hugging 180-degree sweep breath
    // ================================================================
    public static class VoidbornBreathSurge extends BossAttack {
        private final List<BlockDisplayHandle> surgeHandles = new ArrayList<>();
        private boolean sweepActive = false;
        private int sweepTick = 0;

        public VoidbornBreathSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_voidborn_breath_surge", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon at 5 blocks — jaw opens wide, deep violet pours from jaws
            BlockDisplayHandle jawPour = displayBuilder.spawnBlock(
                center.clone().add(0, 5, 2), Material.PURPLE_STAINED_GLASS);
            jawPour.scale(1.5f, 1.5f, 1.5f).glow(110, 0, 200).interpolation(3, 0);
            surgeHandles.add(jawPour);
            spawnedEntities.add(jawPour.entity());
            // Pool forming on ground below
            BlockDisplayHandle groundPool = displayBuilder.spawnBlock(
                center.clone().add(0, 0.05, 2), Material.PURPLE_STAINED_GLASS);
            groundPool.scale(2.0f, 0.06f, 2.0f).glow(110, 0, 200).interpolation(3, 0);
            surgeHandles.add(groundPool);
            spawnedEntities.add(groundPool.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Jaw pour telegraph — pool expanding (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !sweepActive) {
                float poolScale = 2.0f + (ticksAlive / 40.0f) * 4.0f;
                if (surgeHandles.size() > 1) {
                    surgeHandles.get(1).entity().setTransformation(new Transformation(
                        new Vector3f(-poolScale / 2, 0, 2 - poolScale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(poolScale, 0.06f, poolScale),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 2), 8, poolScale);
                }
            }
            // 180-degree sweep begins (tick 40) — left to right at ground level
            else if (ticksAlive == 40 && !sweepActive) {
                sweepActive = true;
                sweepTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.5f, 0.35f);
            }
            // Breath sweep (40-120 ticks = 4 seconds for 180-degree arc)
            else if (sweepActive && ticksAlive - sweepTick < 80) {
                float progress = (ticksAlive - sweepTick) / 80.0f;
                double sweepAngle = -Math.PI / 2 + progress * Math.PI;
                // Breath cone follows sweep angle
                if (ticksAlive % 4 == 0) {
                    for (int d = 2; d <= 10; d += 2) {
                        Location breathPt = center.clone().add(
                            Math.cos(sweepAngle) * d, 0.5, Math.sin(sweepAngle) * d + 2);
                        BlockDisplayHandle breathSeg = displayBuilder.spawnBlock(breathPt, Material.PURPLE_STAINED_GLASS);
                        breathSeg.scale(2.0f, 1.0f, 2.0f).glow(110, 0, 200).interpolation(1, 0);
                        surgeHandles.add(breathSeg);
                        spawnedEntities.add(breathSeg.entity());
                    }
                    Location sweepCenter = center.clone().add(
                        Math.cos(sweepAngle) * 5, 0.5, Math.sin(sweepAngle) * 5 + 2);
                    triggerImpactDamage(sweepCenter);
                    DisplayBuilder.cyanDust(sweepCenter, 10, 5.0);
                }
            }
            // Ground carpet aftermath (120-240 ticks = 6 seconds fade)
            else if (sweepActive && ticksAlive - sweepTick >= 80 && ticksAlive - sweepTick < 200) {
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 2), 6, 10.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidbornBreathSurge(plugin); }
    }

    // ================================================================
    // 38. GRAVITY INVERSION POCKET — Local gravity inversion sphere
    // ================================================================
    public static class GravityInversionPocket extends BossAttack {
        private final List<BlockDisplayHandle> pocketHandles = new ArrayList<>();
        private boolean pocketActive = false;
        private int pocketTick = 0;

        public GravityInversionPocket(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_gravity_inversion_pocket", AttackType.BOSS, 4), "dragon");
            config.setDamage(12.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Small sphere appears at random player position — gold-white particles
            BlockDisplayHandle sphereCore = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.YELLOW_STAINED_GLASS);
            sphereCore.scale(0.5f, 0.5f, 0.5f).glow(255, 245, 180).interpolation(4, 0);
            pocketHandles.add(sphereCore);
            spawnedEntities.add(sphereCore.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sphere expands from 1-block to 5-block radius (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !pocketActive) {
                float scale = 0.5f + (ticksAlive / 40.0f) * 9.5f;
                pocketHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-scale / 2, 2 - scale / 2, -scale / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 8, scale / 2);
                }
            }
            // Sphere activates — gravity inverted inside (tick 40)
            else if (ticksAlive == 40 && !pocketActive) {
                pocketActive = true;
                pocketTick = ticksAlive;
                // Sparkle interior displays
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    float radius = 2.0f + (float)(Math.random() * 2);
                    Location sparkLoc = center.clone().add(
                        Math.cos(angle) * radius, 2 + (Math.random() * 4), Math.sin(angle) * radius);
                    BlockDisplayHandle sparkle = displayBuilder.spawnBlock(sparkLoc, Material.YELLOW_STAINED_GLASS);
                    sparkle.scale(0.3f, 0.3f, 0.3f).glow(255, 255, 100).interpolation(2, 0);
                    pocketHandles.add(sparkle);
                    spawnedEntities.add(sparkle.entity());
                }
                triggerImpactDamage(center.clone().add(0, 5, 0));
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 2.0f, 1.8f);
            }
            // Sphere sustained (40-140 ticks = 5 seconds)
            else if (pocketActive && ticksAlive - pocketTick < 100) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 6, 4.0);
                }
            }
            // Sphere collapses — implosion then explosion (tick 140)
            else if (pocketActive && ticksAlive - pocketTick == 100) {
                triggerImpactDamage(center.clone().add(0, 2, 0));
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 25, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GravityInversionPocket(plugin); }
    }

    // ================================================================
    // 39. FRACTURE POINT III — Third and final summoning seed
    // ================================================================
    public static class FracturePointIII extends BossAttack {
        private final List<BlockDisplayHandle> fractureHandles = new ArrayList<>();
        private boolean seedPlanted = false;

        public FracturePointIII(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_fracture_point_iii", AttackType.BOSS, 4), "dragon");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Third seed crack — plus massive PORTAL pulse ring from Dragon
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 / 6) * i;
                Location crackPt = center.clone().add(Math.cos(angle) * 1.5, 0.05, Math.sin(angle) * 1.5);
                BlockDisplayHandle crack = displayBuilder.spawnBlock(crackPt, Material.NETHER_BRICKS);
                crack.scale(0.6f, 0.06f, 0.6f).glow(80, 0, 0).interpolation(3, 0);
                fractureHandles.add(crack);
                spawnedEntities.add(crack.entity());
            }
            // Massive pulse ring from Dragon acknowledging completion
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location pulsePt = center.clone().add(Math.cos(angle) * 5, 5, Math.sin(angle) * 5);
                BlockDisplayHandle pulse = displayBuilder.spawnBlock(pulsePt, Material.PURPLE_STAINED_GLASS);
                pulse.scale(2.0f, 2.0f, 2.0f).glow(80, 0, 160).interpolation(2, 0);
                fractureHandles.add(pulse);
                spawnedEntities.add(pulse.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ground crack + expanding pulse ring (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !seedPlanted) {
                // Pulse ring expands outward
                float pulseRadius = 5.0f + (ticksAlive / 40.0f) * 25.0f;
                for (int i = 6; i < 22 && i < fractureHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 16) * (i - 6);
                    fractureHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * pulseRadius, 5, Math.sin(angle) * pulseRadius));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 8, 2.0);
                }
            }
            // Third seed erupts + Dragon announcement (tick 40)
            else if (ticksAlive == 40 && !seedPlanted) {
                seedPlanted = true;
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 25, 3.0);
                // 3x3 end stone platform
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockDisplayHandle platform = displayBuilder.spawnBlock(
                            center.clone().add(dx, 0.02, dz), Material.END_STONE);
                        platform.scale(0.9f, 0.1f, 0.9f).glow(60, 50, 50).interpolation(2, 0);
                        fractureHandles.add(platform);
                        spawnedEntities.add(platform.entity());
                    }
                }
                // PORTAL column — synchronized with other two seeds
                BlockDisplayHandle seedColumn = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.5, 0), Material.PURPLE_STAINED_GLASS);
                seedColumn.scale(0.6f, 5.0f, 0.6f).glow(80, 0, 160).interpolation(4, 0);
                fractureHandles.add(seedColumn);
                spawnedEntities.add(seedColumn.entity());
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_BREAK, 2.0f, 0.7f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.2f);
            }
            // All three seeds now pulse in synchrony
            else if (seedPlanted && ticksAlive % 60 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 8, 1.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FracturePointIII(plugin); }
    }

    // ================================================================
    // 40. VOID JUDGMENT — Tri-component signature attack (fireball + blades + tendril)
    // ================================================================
    public static class VoidJudgment extends BossAttack {
        private final List<BlockDisplayHandle> judgmentHandles = new ArrayList<>();
        private boolean judgmentFired = false;
        private int fireTick = 0;

        public VoidJudgment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_judgment", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon cruciform pose — every particle system activates
            // Mouth glow
            BlockDisplayHandle mouthGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 6, 2), Material.MAGENTA_GLAZED_TERRACOTTA);
            mouthGlow.scale(1.0f, 1.0f, 1.0f).glow(130, 0, 180).interpolation(3, 0);
            judgmentHandles.add(mouthGlow);
            spawnedEntities.add(mouthGlow.entity());
            // Eye glow
            BlockDisplayHandle eyeGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 7, 2), Material.PURPLE_STAINED_GLASS);
            eyeGlow.scale(0.6f, 0.4f, 0.4f).glow(200, 50, 255).interpolation(2, 0);
            judgmentHandles.add(eyeGlow);
            spawnedEntities.add(eyeGlow.entity());
            // Wing-tip END_ROD displays
            BlockDisplayHandle leftTip = displayBuilder.spawnBlock(
                center.clone().add(-8, 6, 0), Material.WHITE_STAINED_GLASS);
            leftTip.scale(0.5f, 0.5f, 0.5f).glow(255, 255, 255).interpolation(2, 0);
            judgmentHandles.add(leftTip);
            spawnedEntities.add(leftTip.entity());
            BlockDisplayHandle rightTip = displayBuilder.spawnBlock(
                center.clone().add(8, 6, 0), Material.WHITE_STAINED_GLASS);
            rightTip.scale(0.5f, 0.5f, 0.5f).glow(255, 255, 255).interpolation(2, 0);
            judgmentHandles.add(rightTip);
            spawnedEntities.add(rightTip.entity());
            // 4 cardinal blades
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i + Math.PI / 4; // X-pattern
                Location bladeLoc = center.clone().add(Math.cos(angle) * 10, 5, Math.sin(angle) * 10);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                blade.scale(0.15f, 0.15f, 1.0f).glow(255, 255, 255).interpolation(2, 0);
                judgmentHandles.add(blade);
                spawnedEntities.add(blade.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Cruciform telegraph — all systems glow (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !judgmentFired) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 15, 8.0);
                }
            }
            // Tri-component attack fires simultaneously (tick 40)
            else if (ticksAlive == 40 && !judgmentFired) {
                judgmentFired = true;
                fireTick = ticksAlive;

                // Component 1: Enhanced fireball from mouth
                BlockDisplayHandle fireball = displayBuilder.spawnBlock(
                    center.clone().add(0, 5, 3), Material.MAGMA_BLOCK);
                fireball.scale(2.0f, 2.0f, 2.0f).glow(180, 60, 0).interpolation(2, 0);
                judgmentHandles.add(fireball);
                spawnedEntities.add(fireball.entity());

                // Component 2: 4 blade X-volley fires simultaneously
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 1.2f, 0.6f);

                // Component 3: Tail tendril toward most distant player
                for (int seg = 0; seg < 8; seg++) {
                    Location tendrilPt = center.clone().add(0, 4, -2 - seg * 2);
                    BlockDisplayHandle tendril = displayBuilder.spawnBlock(tendrilPt, Material.OBSIDIAN);
                    tendril.scale(0.2f, 0.2f, 1.5f).glow(80, 0, 150).interpolation(1, 0);
                    judgmentHandles.add(tendril);
                    spawnedEntities.add(tendril.entity());
                }

                triggerImpactDamage(center.clone().add(0, 3, 15)); // Fireball impact
                triggerImpactDamage(center); // Blade convergence
                triggerImpactDamage(center.clone().add(0, 4, -18)); // Tendril reach

                DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 30, 10.0);
            }
            // All three components in flight (40-60 ticks)
            else if (judgmentFired && ticksAlive - fireTick < 20) {
                float progress = (ticksAlive - fireTick) / 20.0f;
                // Fireball moves forward
                if (judgmentHandles.size() > 8) {
                    judgmentHandles.get(8).entity().teleport(
                        center.clone().add(0, 4, 3 + progress * 20));
                }
                // Blades converge inward
                for (int i = 4; i < 8 && i < judgmentHandles.size(); i++) {
                    double angle = (Math.PI / 2) * (i - 4) + Math.PI / 4;
                    float dist = 10 * (1.0f - progress);
                    judgmentHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * dist, 5, Math.sin(angle) * dist));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, progress * 20), 8, 4.0);
                }
            }
            // Components impact + aftermath (60-140 ticks)
            else if (judgmentFired && ticksAlive - fireTick >= 20 && ticksAlive - fireTick < 100) {
                if (ticksAlive - fireTick == 20) {
                    // Fireball AoE cloud
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 23), 20, 5.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                }
                if (ticksAlive % 12 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 23), 6, 5.0); // Fireball cloud
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 4, 3.0); // Blade gaps
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, -10), 4, 8.0); // Tendril trail
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidJudgment(plugin); }
    }

    // ================================================================
    // 41. CONSUMING DARK — Constricting ring from island perimeter
    // ================================================================
    public static class ConsumingDark extends BossAttack {
        private final List<BlockDisplayHandle> darkHandles = new ArrayList<>();
        private boolean ringActive = false;
        private int ringTick = 0;

        public ConsumingDark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_consuming_dark", AttackType.BOSS, 4), "dragon");
            config.setDamage(16.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(1000);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon circles low — painting PORTAL ring on island edge
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2 / 24) * i;
                Location edgePt = center.clone().add(Math.cos(angle) * 25, 0.1, Math.sin(angle) * 25);
                BlockDisplayHandle edgeSeg = displayBuilder.spawnBlock(edgePt, Material.PURPLE_STAINED_GLASS);
                edgeSeg.scale(3.0f, 2.0f, 3.0f).glow(70, 0, 130).interpolation(3, 0);
                darkHandles.add(edgeSeg);
                spawnedEntities.add(edgeSeg.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dragon paints the perimeter ring (0-60 ticks = 3 seconds lap)
            if (ticksAlive < 60 && !ringActive) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 10, 25.0);
                }
            }
            // Ring activates — begins constricting inward (tick 60)
            else if (ticksAlive == 60 && !ringActive) {
                ringActive = true;
                ringTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 2.0f, 0.2f);
            }
            // Ring constricts inward (60-260 ticks = 10 seconds, 25 blocks to ~4 block center)
            else if (ringActive && ticksAlive - ringTick < 200) {
                float progress = (ticksAlive - ringTick) / 200.0f;
                float radius = 25 * (1.0f - progress) + 4 * progress;
                for (int i = 0; i < 24 && i < darkHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 24) * i;
                    darkHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius));
                }
                if (ticksAlive % 10 == 0) {
                    triggerImpactDamage(center.clone().add(radius, 1, 0));
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 10, radius);
                }
            }
            // Ring snaps shut (tick 260)
            else if (ringActive && ticksAlive - ringTick == 200) {
                // Safe zone center pulse
                BlockDisplayHandle safeMarker = displayBuilder.spawnBlock(
                    center.clone().add(0, 0.1, 0), Material.YELLOW_STAINED_GLASS);
                safeMarker.scale(8.0f, 0.1f, 8.0f).glow(200, 255, 100).interpolation(3, 0);
                darkHandles.add(safeMarker);
                spawnedEntities.add(safeMarker.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);
            }
            // Ring dissipates fully (260-360 ticks = 5 seconds aftermath)
            else if (ringActive && ticksAlive - ringTick > 200 && ticksAlive - ringTick < 300) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 8, 20.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConsumingDark(plugin); }
    }

    // ================================================================
    // 42. PHASE BREAK HOWL — Phase transition trigger at 65% HP
    // ================================================================
    public static class PhaseBreakHowl extends BossAttack {
        private final List<BlockDisplayHandle> breakHandles = new ArrayList<>();
        private boolean howlFired = false;
        private int howlTick = 0;

        public PhaseBreakHowl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_phase_break_howl", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(1200);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon ascends to maximum altitude — all particles halt
            // Total stillness — 2 seconds of complete silence
            BlockDisplayHandle dragonCore = displayBuilder.spawnBlock(
                center.clone().add(0, 40, 0), Material.END_STONE);
            dragonCore.scale(4.0f, 4.0f, 4.0f).glow(60, 0, 80).interpolation(5, 0);
            breakHandles.add(dragonCore);
            spawnedEntities.add(dragonCore.entity());
            // Orbiting blades halt — positioned frozen in ring
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location bladeLoc = center.clone().add(Math.cos(angle) * 6, 40, Math.sin(angle) * 6);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                blade.scale(0.15f, 0.15f, 1.0f).glow(255, 255, 255).interpolation(2, 0);
                breakHandles.add(blade);
                spawnedEntities.add(blade.entity());
            }
            // 2 seconds of complete silence — no sound here
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Total stillness — silence telegraph (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !howlFired) {
                // Absolute nothing — the absence IS the telegraph
            }
            // PHASE BREAK HOWL (tick 40) — maximum intensity roar
            else if (ticksAlive == 40 && !howlFired) {
                howlFired = true;
                howlTick = ticksAlive;
                // Massive expanding sphere of purple-black particles
                for (int ring = 0; ring < 12; ring++) {
                    for (int i = 0; i < 16; i++) {
                        double angle = (Math.PI * 2 / 16) * i;
                        float radius = ring * 2.5f;
                        Location spherePt = center.clone().add(
                            Math.cos(angle) * radius, 40 + Math.sin(ring * 0.5) * 5, Math.sin(angle) * radius);
                        BlockDisplayHandle sphere = displayBuilder.spawnBlock(spherePt, Material.PURPLE_STAINED_GLASS);
                        sphere.scale(2.0f, 2.0f, 2.0f).glow(40, 0, 60).interpolation(1, 0);
                        breakHandles.add(sphere);
                        spawnedEntities.add(sphere.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 3.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
            }
            // Sphere expands outward at 20 blocks/second (40-70 ticks = 1.5 seconds)
            else if (howlFired && ticksAlive - howlTick < 30) {
                float radius = (ticksAlive - howlTick) * 1.0f;
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 20, radius);
                }
            }
            // All blades fire outward simultaneously (tick 70) — orbit explosion
            else if (howlFired && ticksAlive - howlTick == 30) {
                for (int i = 1; i <= 8 && i < breakHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 8) * (i - 1);
                    breakHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 25, 40, Math.sin(angle) * 25));
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 40, 0), 30, 25.0);
            }
            // Island flooded with DRAGON_BREATH particles (70-170 ticks = 5 seconds)
            else if (howlFired && ticksAlive - howlTick >= 30 && ticksAlive - howlTick < 130) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 15, 25.0);
                }
            }
            // Sky streams restart at 3x density (170-370 ticks = 10 seconds)
            else if (howlFired && ticksAlive - howlTick >= 130 && ticksAlive - howlTick < 330) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 12, 20.0);
                }
            }
            // Dragon descends — Phase 2 begins (tick 370)
            else if (howlFired && ticksAlive - howlTick == 330) {
                breakHandles.get(0).entity().teleport(center.clone().add(0, 10, 0));
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 3.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseBreakHowl(plugin); }
    }
}
