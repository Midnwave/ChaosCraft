package com.blockforge.chaoscraft.modes.calamity.egg;

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
 * EGG ATTACKS 21-30 (LATE)
 * Between Boss 3, Boss 4, and Boss 5 — intense, rapid, multi-hit patterns.
 * The egg is fully charged with unstable void energy. Attacks are fast, overlap,
 * and require precise movement. Tight dodge windows, large damage areas.
 * Colors: deep purple (80,0,160), violet (128,0,255), dark crimson (120,0,30).
 * Damage range: 10.0 - 18.0 HP.
 * NO status effects. All attacks originate from the egg outward.
 */
public final class EggAttacksLate {

    private EggAttacksLate() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new OctoBoltBarrage(plugin));
        registry.register(new FullArenaVoidWave(plugin));
        registry.register(new ContractionDeathRing(plugin));
        registry.register(new RealityFractureBeams(plugin));
        registry.register(new VoidExplosionCascade(plugin));
        registry.register(new ObsidianStormwall(plugin));
        registry.register(new VoidVortexPull(plugin));
        registry.register(new TripleSpiralSweep(plugin));
        registry.register(new ShatteringSkyfall(plugin));
        registry.register(new AbsoluteVoidNova(plugin));
    }

    // =========================================================================
    // ATTACK 21 — Octo-Bolt Barrage
    // 8 directional bolts fire simultaneously from the egg, followed by a
    // second wave at offset angles 0.5s later, then a THIRD wave 0.5s after
    // that. 24 total bolts in rapid succession. Very fast projectile speed.
    // =========================================================================
    public static class OctoBoltBarrage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> boltHandles = new ArrayList<>();
        private final List<double[]> boltDirs = new ArrayList<>();
        private final List<Integer> boltSpawnTick = new ArrayList<>();
        private int wavesLaunched = 0;

        public OctoBoltBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_octo_bolt_barrage", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Fast warning pulse
            DisplayBuilder.dustParticles(center.clone().add(0, 1.5, 0), 20, 2.0, 120, 0, 30, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 1.5f);
        }

        private void launchWave(Location center, double angleOffset, int tick) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8 + angleOffset;
                double dx = Math.cos(angle);
                double dz = Math.sin(angle);
                Location loc = center.clone().add(dx * 1.5, 1.5, dz * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.3f, 0.3f, 0.6f).glow(80, 0, 160).interpolation(1, 0);
                boltHandles.add(h);
                spawnedEntities.add(h.entity());
                boltDirs.add(new double[]{dx, dz});
                boltSpawnTick.add(tick);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_SHOOT, 1.5f, 0.5f + wavesLaunched * 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Quick telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, 0), 8, 1.0);
                }
                return;
            }

            // Wave 1 (tick 10) — cardinal + diagonal
            if (ticksAlive == 10 && wavesLaunched == 0) {
                wavesLaunched = 1;
                launchWave(center, 0, ticksAlive);
            }
            // Wave 2 (tick 20) — rotated 22.5 degrees
            if (ticksAlive == 20 && wavesLaunched == 1) {
                wavesLaunched = 2;
                launchWave(center, Math.PI / 8, ticksAlive);
            }
            // Wave 3 (tick 30) — rotated 11.25 degrees
            if (ticksAlive == 30 && wavesLaunched == 2) {
                wavesLaunched = 3;
                launchWave(center, Math.PI / 16, ticksAlive);
            }

            // Move all bolts — 1.0 blocks/tick (fast!)
            for (int i = 0; i < boltHandles.size(); i++) {
                BlockDisplayHandle h = boltHandles.get(i);
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                double[] dir = boltDirs.get(i);
                h.entity().teleport(loc.clone().add(dir[0] * 1.0, 0, dir[1] * 1.0));
            }

            // Trail particles
            if (ticksAlive % 2 == 0) {
                for (BlockDisplayHandle h : boltHandles) {
                    if (h.entity().isValid()) {
                        DisplayBuilder.darkPurpleDust(h.entity().getLocation(), 2, 0.2);
                    }
                }
            }

            // Damage check every 2 ticks
            if (ticksAlive % 2 == 0) {
                for (BlockDisplayHandle h : boltHandles) {
                    if (!h.entity().isValid()) continue;
                    Location boltLoc = h.entity().getLocation();
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        if (player.getLocation().distanceSquared(boltLoc) < 6.25) {
                            triggerImpactDamage(player.getLocation());
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            boltHandles.clear();
            boltDirs.clear();
            boltSpawnTick.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new OctoBoltBarrage(plugin); }
    }

    // =========================================================================
    // ATTACK 22 — Full Arena Void Wave
    // A massive wall of obsidian 20 blocks wide and 4 blocks tall sweeps
    // across the entire arena from one side. Players must find the single
    // 3-block gap to survive. Very fast sweep speed.
    // =========================================================================
    public static class FullArenaVoidWave extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private double wallX;
        private double sweepDir;
        private int gapIndex; // Which column is the gap (0-19)
        private boolean wallSpawned = false;

        public FullArenaVoidWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_full_arena_void_wave", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            sweepDir = Math.random() < 0.5 ? 1.0 : -1.0;
            wallX = center.getX() - sweepDir * 25;
            gapIndex = 8 + (int)(Math.random() * 4); // Gap in middle region

            // Warning: edge flash line
            for (int z = -10; z <= 10; z++) {
                DisplayBuilder.dustParticles(
                    new Location(w, wallX + sweepDir * 3, center.getY() + 2, center.getZ() + z),
                    2, 0.3, 120, 0, 30, 1.5f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Short telegraph (0-15 ticks)
            if (ticksAlive < 15) {
                if (ticksAlive % 3 == 0) {
                    for (int z = -10; z <= 10; z += 3) {
                        DisplayBuilder.dustParticles(
                            new Location(w, wallX + sweepDir * 3, center.getY() + 2, center.getZ() + z),
                            2, 0.3, 120, 0, 30, 1.2f);
                    }
                }
                return;
            }

            // Spawn wall (tick 15)
            if (!wallSpawned) {
                wallSpawned = true;
                for (int z = -10; z <= 10; z++) {
                    int colIndex = z + 10; // 0-20
                    // Skip gap columns (3-wide gap)
                    if (colIndex >= gapIndex && colIndex < gapIndex + 3) continue;

                    for (int y = 0; y < 4; y++) {
                        Location loc = new Location(w, wallX, center.getY() + y, center.getZ() + z);
                        Material mat = (y == 0 || y == 3) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                        h.scale(0.8f, 1.0f, 0.8f).glow(80, 0, 160).interpolation(1, 0);
                        wallHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 0.5f);
            }

            // Sweep wall — 0.8 blocks/tick (fast)
            wallX += sweepDir * 0.8;
            for (BlockDisplayHandle h : wallHandles) {
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                h.entity().teleport(loc.clone().add(sweepDir * 0.8, 0, 0));
            }

            // Ground smear particles
            if (ticksAlive % 2 == 0) {
                for (int z = -10; z <= 10; z += 3) {
                    int colIndex = z + 10;
                    if (colIndex >= gapIndex && colIndex < gapIndex + 3) continue;
                    DisplayBuilder.darkPurpleDust(
                        new Location(w, wallX, center.getY() + 0.2, center.getZ() + z), 2, 0.3);
                }
            }

            // Damage players hit by wall (not in gap)
            if (ticksAlive % 2 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    if (Math.abs(ploc.getX() - wallX) < 2.0 && ploc.getY() < center.getY() + 4.5) {
                        // Check if in gap
                        int playerCol = (int)(ploc.getZ() - center.getZ()) + 10;
                        if (playerCol < gapIndex || playerCol >= gapIndex + 3) {
                            triggerImpactDamage(ploc);
                            player.setVelocity(player.getVelocity().add(
                                new org.bukkit.util.Vector(sweepDir * 1.5, 0.5, 0)));
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            wallHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new FullArenaVoidWave(plugin); }
    }

    // =========================================================================
    // ATTACK 23 — Contraction Death Ring
    // A large ring of crying obsidian spawns at radius 18, then contracts
    // inward toward the egg. Players must run INTO the ring center before it
    // closes. The ring contracts over 4 seconds to radius 2, dealing heavy
    // damage to anyone caught in the collapsing wall.
    // =========================================================================
    public static class ContractionDeathRing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private double ringRadius = 18.0;
        private boolean ringSpawned = false;

        public ContractionDeathRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_contraction_death_ring", AttackType.BLOCK_DISPLAY, 0));
            config.setDamage(14.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning: outer ring flash
            DisplayBuilder.particleRing(center.clone().add(0, 1.0, 0), 18.0,
                Particle.DUST, 30, new Particle.DustOptions(Color.fromRGB(120, 0, 30), 2.0f));
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph (0-20 ticks)
            if (ticksAlive < 20) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.particleRing(center.clone().add(0, 1.0, 0), 18.0,
                        Particle.DUST, 24, new Particle.DustOptions(Color.fromRGB(120, 0, 30), 1.5f));
                }
                return;
            }

            // Spawn ring (tick 20)
            if (!ringSpawned) {
                ringSpawned = true;
                for (int i = 0; i < 24; i++) {
                    double angle = (2 * Math.PI * i) / 24;
                    Location loc = center.clone().add(
                        Math.cos(angle) * ringRadius, 0.5, Math.sin(angle) * ringRadius);
                    Material mat = (i % 2 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.8f, 2.0f, 0.8f).glow(120, 0, 30).interpolation(2, 0);
                    ringHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 0.4f);
            }

            // Contract ring — 0.2 blocks/tick (18 to 2 over 80 ticks = 4 seconds)
            ringRadius = Math.max(2.0, ringRadius - 0.2);

            for (int i = 0; i < ringHandles.size(); i++) {
                if (!ringHandles.get(i).entity().isValid()) continue;
                double angle = (2 * Math.PI * i) / ringHandles.size();
                Location loc = center.clone().add(
                    Math.cos(angle) * ringRadius, 0.5, Math.sin(angle) * ringRadius);
                ringHandles.get(i).entity().teleport(loc);

                // Blocks glow brighter as ring contracts
                float intensity = (float)(1.0 - (ringRadius - 2) / 16.0);
                int glowR = (int)(80 + intensity * 80);
                int glowG = 0;
                int glowB = (int)(160 - intensity * 130);
                ringHandles.get(i).glow(glowR, glowG, glowB);
            }

            // Contracting particles
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), ringRadius,
                    Particle.DUST, 20, new Particle.DustOptions(Color.fromRGB(120, 0, 30), 1.5f));
            }

            // Damage players caught in the wall
            if (ticksAlive % 3 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double pDist = player.getLocation().toVector().setY(0)
                        .distance(center.toVector().setY(0));
                    if (Math.abs(pDist - ringRadius) < 2.5 && player.getLocation().getY() < center.getY() + 3) {
                        triggerImpactDamage(player.getLocation());
                    }
                }
            }

            // Sound intensifies
            if (ticksAlive % 10 == 0) {
                float pitch = 0.4f + (float)((18.0 - ringRadius) / 16.0) * 1.2f;
                DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, pitch);
            }
        }

        @Override
        protected void onCleanup() {
            ringHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new ContractionDeathRing(plugin); }
    }

    // =========================================================================
    // ATTACK 24 — Reality Fracture Beams
    // 6 beams of purple stained glass extend from the egg in random directions,
    // each 15 blocks long. The beams rotate slowly for 3 seconds, then
    // "shatter" — each block in the beam explodes into particles with impact
    // damage at its location. Dodge the beams, then dodge the shatter zones.
    // =========================================================================
    public static class RealityFractureBeams extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private final List<Double> beamAngles = new ArrayList<>();
        private double rotationOffset = 0;
        private boolean beamsSpawned = false;
        private boolean beamsShattered = false;

        public RealityFractureBeams(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_reality_fracture_beams", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 6 random beam angles
            for (int i = 0; i < 6; i++) {
                beamAngles.add(Math.random() * Math.PI * 2);
            }
            // Flash warnings
            for (double angle : beamAngles) {
                Location end = center.clone().add(Math.cos(angle) * 8, 1.5, Math.sin(angle) * 8);
                DisplayBuilder.dustParticles(end, 5, 0.5, 128, 0, 255, 1.2f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Short telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 1.5, 0), 10, 1.0);
                }
                return;
            }

            // Spawn beams (tick 10)
            if (!beamsSpawned) {
                beamsSpawned = true;
                for (int b = 0; b < 6; b++) {
                    double angle = beamAngles.get(b);
                    for (int seg = 1; seg <= 15; seg++) {
                        Location loc = center.clone().add(
                            Math.cos(angle) * seg, 1.5, Math.sin(angle) * seg);
                        Material mat = (seg % 3 == 0) ? Material.AMETHYST_BLOCK : Material.PURPLE_STAINED_GLASS;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                        h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(1, 0);
                        beamHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.5f);
            }

            // Beams rotate slowly (10-70 ticks = 3 seconds)
            if (beamsSpawned && !beamsShattered && ticksAlive < 70) {
                rotationOffset += 0.015;
                for (int i = 0; i < beamHandles.size(); i++) {
                    if (!beamHandles.get(i).entity().isValid()) continue;
                    int beamIdx = i / 15;
                    int seg = (i % 15) + 1;
                    double angle = beamAngles.get(beamIdx) + rotationOffset;
                    Location loc = center.clone().add(
                        Math.cos(angle) * seg, 1.5, Math.sin(angle) * seg);
                    beamHandles.get(i).entity().teleport(loc);
                }

                // Damage from beams
                if (ticksAlive % 4 == 0) {
                    for (BlockDisplayHandle h : beamHandles) {
                        if (!h.entity().isValid()) continue;
                        Location bLoc = h.entity().getLocation();
                        for (org.bukkit.entity.Player player : w.getPlayers()) {
                            if (isExempt(player)) continue;
                            if (player.getLocation().distanceSquared(bLoc) < 4.0) {
                                triggerImpactDamage(player.getLocation());
                            }
                        }
                    }
                }

                // Beam flicker
                if (ticksAlive > 55 && ticksAlive % 2 == 0) {
                    for (BlockDisplayHandle h : beamHandles) {
                        if (!h.entity().isValid()) continue;
                        float flicker = (float)(0.3 + Math.random() * 0.3);
                        h.entity().setTransformation(new Transformation(
                            new Vector3f(-flicker / 2, -flicker / 2, -flicker / 2),
                            new AxisAngle4f((float)(ticksAlive * 0.1), 0.5f, 1.0f, 0.3f),
                            new Vector3f(flicker, flicker, flicker),
                            new AxisAngle4f(0, 0, 1, 0)
                        ));
                        h.entity().setInterpolationDuration(1);
                        h.entity().setInterpolationDelay(0);
                    }
                }
            }

            // Shatter (tick 70) — all beam blocks explode
            if (ticksAlive == 70 && !beamsShattered) {
                beamsShattered = true;
                for (BlockDisplayHandle h : beamHandles) {
                    if (!h.entity().isValid()) continue;
                    Location loc = h.entity().getLocation();
                    DisplayBuilder.dustParticles(loc, 6, 1.0, 128, 0, 255, 1.5f);
                    triggerImpactDamage(loc);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            beamHandles.clear();
            beamAngles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new RealityFractureBeams(plugin); }
    }

    // =========================================================================
    // ATTACK 25 — Void Explosion Cascade
    // A chain of 8 void explosions detonates in rapid sequence, each 4 blocks
    // from the last, forming a winding path from the egg outward. Each
    // explosion has a 0.3-second warning circle before detonating. Players must
    // stay ahead of the cascade chain.
    // =========================================================================
    public static class VoidExplosionCascade extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> warningHandles = new ArrayList<>();
        private final List<Location> explosionLocations = new ArrayList<>();
        private final boolean[] detonated = new boolean[8];
        private static final int CASCADE_COUNT = 8;

        public VoidExplosionCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_void_explosion_cascade", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(3.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(340);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Generate winding cascade path
            double angle = Math.random() * Math.PI * 2;
            Location current = center.clone();
            for (int i = 0; i < CASCADE_COUNT; i++) {
                angle += (Math.random() - 0.5) * 1.2; // Winding path
                current = current.clone().add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4);
                explosionLocations.add(current.clone());
            }
            // First warning
            DisplayBuilder.dustParticles(explosionLocations.get(0), 8, 1.5, 120, 0, 30, 1.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Each explosion triggers 8 ticks apart (0.4 seconds)
            for (int i = 0; i < CASCADE_COUNT; i++) {
                int warningTick = 10 + i * 8;
                int detonateTick = warningTick + 6;

                // Warning circle (6 ticks before detonation)
                if (ticksAlive == warningTick && !detonated[i]) {
                    Location loc = explosionLocations.get(i);
                    BlockDisplayHandle warning = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                    warning.scale(3.0f, 0.05f, 3.0f).glow(120, 0, 30).interpolation(2, 0);
                    warningHandles.add(warning);
                    spawnedEntities.add(warning.entity());
                    DisplayBuilder.particleRing(loc.clone().add(0, 0.2, 0), 1.5,
                        Particle.DUST, 12, new Particle.DustOptions(Color.fromRGB(120, 0, 30), 1.2f));
                    DisplayBuilder.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.5f);
                }

                // Detonate
                if (ticksAlive == detonateTick && !detonated[i]) {
                    detonated[i] = true;
                    Location loc = explosionLocations.get(i);
                    triggerImpactDamage(loc);
                    // Visual explosion
                    DisplayBuilder.dustParticles(loc, 30, 3.0, 80, 0, 160, 2.5f);
                    DisplayBuilder.dustParticles(loc, 20, 2.0, 120, 0, 30, 2.0f);

                    // Explosion block display
                    BlockDisplayHandle blast = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    blast.scale(2.0f, 2.0f, 2.0f).glow(128, 0, 255).interpolation(3, 0);
                    spawnedEntities.add(blast.entity());
                    // Animate blast shrinking
                    blast.animateTo(
                        new Vector3f(-0.1f, -0.1f, -0.1f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.2f, 0.2f, 0.2f), 10);

                    DisplayBuilder.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
                }
            }
        }

        @Override
        protected void onCleanup() {
            warningHandles.clear();
            explosionLocations.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new VoidExplosionCascade(plugin); }
    }

    // =========================================================================
    // ATTACK 26 — Obsidian Stormwall
    // Two massive obsidian walls sweep from opposite sides of the arena toward
    // the center, each with a gap at a DIFFERENT height. One gap is at ground
    // level (crouch through), the other at jump height. Players must be at the
    // correct height when each wall passes.
    // =========================================================================
    public static class ObsidianStormwall extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftWallHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWallHandles = new ArrayList<>();
        private double leftWallX;
        private double rightWallX;
        private boolean wallsSpawned = false;

        public ObsidianStormwall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_obsidian_stormwall", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(420);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            leftWallX = center.getX() - 24;
            rightWallX = center.getX() + 24;

            // Warning edges
            for (int z = -10; z <= 10; z += 2) {
                DisplayBuilder.dustParticles(
                    new Location(w, leftWallX + 3, center.getY() + 2, center.getZ() + z),
                    2, 0.3, 80, 0, 160, 1.0f);
                DisplayBuilder.dustParticles(
                    new Location(w, rightWallX - 3, center.getY() + 2, center.getZ() + z),
                    2, 0.3, 80, 0, 160, 1.0f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Short telegraph (0-10 ticks)
            if (ticksAlive < 10) return;

            // Spawn walls (tick 10)
            if (!wallsSpawned) {
                wallsSpawned = true;
                // Left wall: gap at y=0-1 (crouch gap at bottom)
                for (int z = -10; z <= 10; z++) {
                    for (int y = 0; y < 5; y++) {
                        if (y <= 1) continue; // Bottom gap
                        Location loc = new Location(w, leftWallX, center.getY() + y, center.getZ() + z);
                        Material mat = (y % 2 == 0) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                        h.scale(0.9f, 1.0f, 0.9f).glow(80, 0, 160).interpolation(1, 0);
                        leftWallHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                // Right wall: gap at y=2-3 (jump gap in middle)
                for (int z = -10; z <= 10; z++) {
                    for (int y = 0; y < 5; y++) {
                        if (y >= 2 && y <= 3) continue; // Middle gap
                        Location loc = new Location(w, rightWallX, center.getY() + y, center.getZ() + z);
                        Material mat = (y % 2 == 0) ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                        h.scale(0.9f, 1.0f, 0.9f).glow(120, 0, 30).interpolation(1, 0);
                        rightWallHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.3f);
            }

            // Move walls toward center — 0.7 blocks/tick
            leftWallX += 0.7;
            rightWallX -= 0.7;

            for (BlockDisplayHandle h : leftWallHandles) {
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                h.entity().teleport(loc.clone().add(0.7, 0, 0));
            }
            for (BlockDisplayHandle h : rightWallHandles) {
                if (!h.entity().isValid()) continue;
                Location loc = h.entity().getLocation();
                h.entity().teleport(loc.clone().add(-0.7, 0, 0));
            }

            // Edge particles
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.darkPurpleDust(
                    new Location(w, leftWallX, center.getY() + 2, center.getZ()), 4, 1.0);
                DisplayBuilder.dustParticles(
                    new Location(w, rightWallX, center.getY() + 2, center.getZ()),
                    4, 1.0, 120, 0, 30, 1.2f);
            }

            // Damage from walls
            if (ticksAlive % 2 == 0) {
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    Location ploc = player.getLocation();
                    double py = ploc.getY() - center.getY();

                    // Left wall (gap at y=0-1)
                    if (Math.abs(ploc.getX() - leftWallX) < 2.0) {
                        if (py > 1.0 || py < 0) { // Not in bottom gap
                            triggerImpactDamage(ploc);
                        }
                    }
                    // Right wall (gap at y=2-3)
                    if (Math.abs(ploc.getX() - rightWallX) < 2.0) {
                        if (py < 2.0 || py > 3.0) { // Not in middle gap
                            triggerImpactDamage(ploc);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            leftWallHandles.clear();
            rightWallHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new ObsidianStormwall(plugin); }
    }

    // =========================================================================
    // ATTACK 27 — Void Vortex Pull
    // A gravitational vortex of purple energy forms around the egg, pulling
    // players inward. Obsidian debris orbits the vortex at high speed, damaging
    // players that get close. Players must fight the pull. After 3 seconds the
    // vortex explodes outward.
    // =========================================================================
    public static class VoidVortexPull extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> debrisHandles = new ArrayList<>();
        private double orbitAngle = 0;
        private boolean vortexActive = false;
        private boolean vortexExploded = false;
        private int vortexStartTick = 0;

        public VoidVortexPull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_void_vortex_pull", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(460);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.darkPurpleDust(center, 20, 3.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph (0-15 ticks)
            if (ticksAlive < 15) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.darkPurpleDust(center, 10, 2.0);
                }
                return;
            }

            // Activate vortex (tick 15)
            if (ticksAlive == 15 && !vortexActive) {
                vortexActive = true;
                vortexStartTick = ticksAlive;
                // Spawn orbiting debris — 10 pieces
                for (int i = 0; i < 10; i++) {
                    double angle = (2 * Math.PI * i) / 10;
                    double r = 4 + Math.random() * 2;
                    double y = 0.5 + Math.random() * 2.5;
                    Location loc = center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                    Material mat = (i % 3 == 0) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(1, 0);
                    debrisHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.2f, 0.5f);
            }

            // Vortex active phase (15-75 ticks = 3 seconds)
            if (vortexActive && !vortexExploded && ticksAlive < 75) {
                // Orbit debris rapidly
                orbitAngle += 0.15;
                for (int i = 0; i < debrisHandles.size(); i++) {
                    if (!debrisHandles.get(i).entity().isValid()) continue;
                    double baseAngle = orbitAngle + (2 * Math.PI * i) / debrisHandles.size();
                    double r = 3.5 + Math.sin(ticksAlive * 0.1 + i) * 1.5;
                    double y = 1.0 + Math.sin(ticksAlive * 0.08 + i * 0.5) * 1.5;
                    Location loc = center.clone().add(
                        Math.cos(baseAngle) * r, y, Math.sin(baseAngle) * r);
                    debrisHandles.get(i).entity().teleport(loc);

                    // Spin debris
                    float rot = (float)(orbitAngle * 3 + i);
                    debrisHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-0.15f, -0.15f, -0.15f),
                        new AxisAngle4f(rot, 0.5f, 1.0f, 0.3f),
                        new Vector3f(0.3f, 0.3f, 0.3f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    debrisHandles.get(i).entity().setInterpolationDuration(1);
                    debrisHandles.get(i).entity().setInterpolationDelay(0);
                }

                // Pull players toward center
                if (ticksAlive % 2 == 0) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        double dist = player.getLocation().distance(center);
                        if (dist < 15 && dist > 2) {
                            org.bukkit.util.Vector pull = center.toVector()
                                .subtract(player.getLocation().toVector()).normalize().multiply(0.15);
                            player.setVelocity(player.getVelocity().add(pull));
                        }
                        // Damage if too close to debris
                        if (dist < 5) {
                            triggerImpactDamage(player.getLocation());
                        }
                    }
                }

                // Vortex particles
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.3, 0), 4.0,
                        Particle.DUST, 16, new Particle.DustOptions(Color.fromRGB(80, 0, 160), 1.5f));
                    DisplayBuilder.particleRing(center.clone().add(0, 2.0, 0), 3.0,
                        Particle.DUST, 12, new Particle.DustOptions(Color.fromRGB(128, 0, 255), 1.2f));
                }

                // Sounds
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.3f);
                }
            }

            // Vortex explodes (tick 75) — outward blast
            if (ticksAlive == 75 && !vortexExploded) {
                vortexExploded = true;
                triggerImpactDamage(center);
                DisplayBuilder.dustParticles(center, 40, 5.0, 80, 0, 160, 3.0f);
                DisplayBuilder.dustParticles(center, 30, 4.0, 120, 0, 30, 2.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 0.5f);

                // Knockback all nearby players outward
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (dist < 10) {
                        org.bukkit.util.Vector push = player.getLocation().toVector()
                            .subtract(center.toVector()).normalize().multiply(2.5);
                        push.setY(0.8);
                        player.setVelocity(push);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() {
            debrisHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new VoidVortexPull(plugin); }
    }

    // =========================================================================
    // ATTACK 28 — Triple Spiral Sweep
    // Three arms of obsidian rotate around the egg at different heights
    // (ground, mid, high) and different speeds. Creates a complex pattern
    // where all three heights align at specific moments — must time movement
    // between the sweeping arms.
    // =========================================================================
    public static class TripleSpiralSweep extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> armHandles = new ArrayList<>();
        private double angle1 = 0, angle2 = 0, angle3 = 0;
        private boolean armsSpawned = false;

        public TripleSpiralSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_triple_spiral_sweep", AttackType.BLOCK_DISPLAY, 0));
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(380);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Random start angles
            angle1 = Math.random() * Math.PI * 2;
            angle2 = Math.random() * Math.PI * 2;
            angle3 = Math.random() * Math.PI * 2;

            DisplayBuilder.darkPurpleDust(center.clone().add(0, 1.5, 0), 15, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Short telegraph (0-15 ticks)
            if (ticksAlive < 15) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center.clone().add(0, 0.5, 0), 4, 1.0);
                    DisplayBuilder.purpleDust(center.clone().add(0, 1.5, 0), 4, 1.0);
                    DisplayBuilder.purpleDust(center.clone().add(0, 2.5, 0), 4, 1.0);
                }
                return;
            }

            // Spawn 3 arms (tick 15) — 10 segments each, 30 total
            if (!armsSpawned) {
                armsSpawned = true;
                // Heights: 0.3, 1.5, 2.7
                double[] heights = {0.3, 1.5, 2.7};
                Material[] mats = {Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.BLACK_CONCRETE};
                for (int arm = 0; arm < 3; arm++) {
                    double angle = (arm == 0) ? angle1 : (arm == 1) ? angle2 : angle3;
                    for (int seg = 1; seg <= 10; seg++) {
                        Location loc = center.clone().add(
                            Math.cos(angle) * seg * 1.2, heights[arm], Math.sin(angle) * seg * 1.2);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[arm]);
                        h.scale(0.4f, 0.6f, 0.4f).glow(80, 0, 160).interpolation(1, 0);
                        armHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.0f);
            }

            // Rotate arms at different speeds
            angle1 += 0.05;  // Slow
            angle2 += 0.08;  // Medium
            angle3 += 0.11;  // Fast

            double[] heights = {0.3, 1.5, 2.7};
            double[] angles = {angle1, angle2, angle3};

            for (int i = 0; i < armHandles.size(); i++) {
                if (!armHandles.get(i).entity().isValid()) continue;
                int arm = i / 10;
                int seg = (i % 10) + 1;
                double angle = angles[arm];
                Location loc = center.clone().add(
                    Math.cos(angle) * seg * 1.2, heights[arm], Math.sin(angle) * seg * 1.2);
                armHandles.get(i).entity().teleport(loc);
            }

            // Tip particles for each arm
            if (ticksAlive % 3 == 0) {
                for (int arm = 0; arm < 3; arm++) {
                    double angle = angles[arm];
                    Location tip = center.clone().add(
                        Math.cos(angle) * 12, heights[arm], Math.sin(angle) * 12);
                    int r = (arm == 0) ? 80 : (arm == 1) ? 128 : 120;
                    int g = 0;
                    int b = (arm == 0) ? 160 : (arm == 1) ? 255 : 30;
                    DisplayBuilder.dustParticles(tip, 4, 0.5, r, g, b, 1.2f);
                }
            }
        }

        @Override
        protected void onCleanup() {
            armHandles.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new TripleSpiralSweep(plugin); }
    }

    // =========================================================================
    // ATTACK 29 — Shattering Skyfall
    // 15 obsidian meteors rain from directly above in rapid succession, each
    // targeting a different random location in the arena. Minimal warning
    // time (0.5s per meteor), fast fall speed. Covers the arena with
    // overlapping impact zones.
    // =========================================================================
    public static class ShatteringSkyfall extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> meteorHandles = new ArrayList<>();
        private final List<double[]> meteorTargets = new ArrayList<>();
        private final List<Double> meteorHeights = new ArrayList<>();
        private final boolean[] meteorImpacted = new boolean[15];
        private int meteorsSpawned = 0;
        private static final int METEOR_COUNT = 15;

        public ShatteringSkyfall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_shattering_skyfall", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-compute random targets
            for (int i = 0; i < METEOR_COUNT; i++) {
                double ox = (Math.random() - 0.5) * 28;
                double oz = (Math.random() - 0.5) * 28;
                meteorTargets.add(new double[]{ox, oz});
                meteorHeights.add(15.0); // Start height
            }
            DisplayBuilder.darkPurpleDust(center.clone().add(0, 10, 0), 20, 5.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spawn new meteor every 4 ticks (0.2s), starting at tick 5
            if (ticksAlive >= 5 && ticksAlive % 4 == 0 && meteorsSpawned < METEOR_COUNT) {
                int idx = meteorsSpawned;
                double[] target = meteorTargets.get(idx);
                Location spawnLoc = center.clone().add(target[0], 15, target[1]);

                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnLoc, Material.CRYING_OBSIDIAN);
                h.scale(0.8f, 0.8f, 0.8f).glow(120, 0, 30).interpolation(1, 0);
                meteorHandles.add(h);
                spawnedEntities.add(h.entity());
                meteorsSpawned++;

                // Target warning circle
                Location targetLoc = center.clone().add(target[0], 0.2, target[1]);
                DisplayBuilder.dustParticles(targetLoc, 6, 1.5, 120, 0, 30, 1.2f);
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
            }

            // Animate all falling meteors
            for (int i = 0; i < meteorHandles.size(); i++) {
                if (meteorImpacted[i]) continue;
                BlockDisplayHandle h = meteorHandles.get(i);
                if (!h.entity().isValid()) continue;

                // Fall fast — 0.6 blocks/tick
                double height = meteorHeights.get(i) - 0.6;
                meteorHeights.set(i, height);

                double[] target = meteorTargets.get(i);
                Location loc = center.clone().add(target[0], Math.max(0.1, height), target[1]);
                h.entity().teleport(loc);

                // Spin
                float rot = (float)((15 - height) * 0.2);
                h.entity().setTransformation(new Transformation(
                    new Vector3f(-0.4f, -0.4f, -0.4f),
                    new AxisAngle4f(rot, 0.5f, 1.0f, 0.3f),
                    new Vector3f(0.8f, 0.8f, 0.8f),
                    new AxisAngle4f(0, 0, 1, 0)
                ));
                h.entity().setInterpolationDuration(1);
                h.entity().setInterpolationDelay(0);

                // Trail
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.darkPurpleDust(loc, 3, 0.3);
                }

                // Impact
                if (height <= 0.1) {
                    meteorImpacted[i] = true;
                    Location impactLoc = center.clone().add(target[0], 0.1, target[1]);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.dustParticles(impactLoc, 15, 2.0, 80, 0, 160, 2.0f);
                    DisplayBuilder.dustParticles(impactLoc, 10, 1.5, 120, 0, 30, 1.5f);
                    DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() {
            meteorHandles.clear();
            meteorTargets.clear();
            meteorHeights.clear();
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new ShatteringSkyfall(plugin); }
    }

    // =========================================================================
    // ATTACK 30 — Absolute Void Nova
    // The egg's ultimate attack. A massive void sphere grows from the egg,
    // reaching radius 20, then collapses into a singularity point that detonates
    // with arena-wide damage. Three phases: expansion (outward damage ring),
    // collapse (inward pull), detonation (flat damage + knockback).
    // The most dangerous egg attack in the entire mode.
    // =========================================================================
    public static class AbsoluteVoidNova extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> sphereHandles = new ArrayList<>();
        private BlockDisplayHandle coreHandle;
        private double sphereRadius = 1.0;
        private boolean expanding = false;
        private boolean collapsing = false;
        private boolean detonated = false;

        public AbsoluteVoidNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("egg_absolute_void_nova", AttackType.BLOCK_DISPLAY, 0));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(18.0);
            config.setImpactRadius(8.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(600);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Ominous warning — the egg hums
            DisplayBuilder.darkPurpleDust(center, 25, 3.0);
            DisplayBuilder.dustParticles(center, 15, 2.0, 120, 0, 30, 2.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Telegraph (0-20 ticks) — deep rumble
            if (ticksAlive < 20) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.darkPurpleDust(center, (int)(8 + ticksAlive * 0.5), 2.0);
                    DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 5, 1.0, 120, 0, 30, 1.5f);
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f,
                        0.3f + ticksAlive * 0.02f);
                }
                return;
            }

            // PHASE 1: Expansion (tick 20-80 = 3 seconds)
            if (ticksAlive == 20 && !expanding) {
                expanding = true;
                // Spawn core
                coreHandle = displayBuilder.spawnBlock(center, Material.CRYING_OBSIDIAN);
                coreHandle.scale(2.0f, 2.0f, 2.0f).glow(128, 0, 255).interpolation(3, 0);
                spawnedEntities.add(coreHandle.entity());

                // Spawn sphere shell — 16 blocks in a spherical pattern
                for (int i = 0; i < 16; i++) {
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / 16);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    Location loc = center.clone().add(
                        Math.cos(theta) * Math.sin(phi) * 2,
                        Math.cos(phi) * 2,
                        Math.sin(theta) * Math.sin(phi) * 2);
                    Material mat = (i % 3 == 0) ? Material.AMETHYST_BLOCK :
                                   (i % 3 == 1) ? Material.PURPLE_STAINED_GLASS : Material.OBSIDIAN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.5f, 0.5f, 0.5f).glow(80, 0, 160).interpolation(2, 0);
                    sphereHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.3f);
            }

            // Expand sphere
            if (expanding && !collapsing && ticksAlive > 20 && ticksAlive < 80) {
                sphereRadius = 2.0 + (ticksAlive - 20) * 0.3; // 2 to 20

                // Move sphere shell
                for (int i = 0; i < sphereHandles.size(); i++) {
                    if (!sphereHandles.get(i).entity().isValid()) continue;
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / sphereHandles.size());
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    Location loc = center.clone().add(
                        Math.cos(theta) * Math.sin(phi) * sphereRadius,
                        Math.cos(phi) * sphereRadius,
                        Math.sin(theta) * Math.sin(phi) * sphereRadius);
                    sphereHandles.get(i).entity().teleport(loc);

                    // Rotate blocks
                    float rot = (float)(ticksAlive * 0.1 + i);
                    sphereHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-0.25f, -0.25f, -0.25f),
                        new AxisAngle4f(rot, 0.5f, 1.0f, 0.3f),
                        new Vector3f(0.5f, 0.5f, 0.5f),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    sphereHandles.get(i).entity().setInterpolationDuration(2);
                    sphereHandles.get(i).entity().setInterpolationDelay(0);
                }

                // Grow core
                if (coreHandle != null && coreHandle.entity().isValid()) {
                    float coreScale = 2.0f + (float)((ticksAlive - 20) * 0.05);
                    float halfCore = coreScale / 2;
                    coreHandle.entity().setTransformation(new Transformation(
                        new Vector3f(-halfCore, -halfCore, -halfCore),
                        new AxisAngle4f((float)(ticksAlive * 0.03), 0, 1, 0),
                        new Vector3f(coreScale, coreScale, coreScale),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    coreHandle.entity().setInterpolationDuration(3);
                    coreHandle.entity().setInterpolationDelay(0);
                }

                // Expanding ring damage
                if (ticksAlive % 5 == 0) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        double dist = player.getLocation().distance(center);
                        if (Math.abs(dist - sphereRadius) < 2.5) {
                            triggerImpactDamage(player.getLocation());
                        }
                    }
                }

                // Sphere particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.particleRing(center.clone().add(0, 0.5, 0), sphereRadius,
                        Particle.DUST, 20, new Particle.DustOptions(Color.fromRGB(80, 0, 160), 1.5f));
                }

                // Sounds escalate
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f,
                        0.3f + (float)((sphereRadius - 2) / 18.0) * 1.0f);
                }
            }

            // PHASE 2: Collapse (tick 80-120 = 2 seconds)
            if (ticksAlive == 80 && !collapsing) {
                collapsing = true;
                expanding = false;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
            }

            if (collapsing && !detonated && ticksAlive >= 80 && ticksAlive < 120) {
                // Sphere contracts rapidly
                sphereRadius = Math.max(1.0, 20.0 - (ticksAlive - 80) * 0.475);

                for (int i = 0; i < sphereHandles.size(); i++) {
                    if (!sphereHandles.get(i).entity().isValid()) continue;
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / sphereHandles.size());
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    Location loc = center.clone().add(
                        Math.cos(theta) * Math.sin(phi) * sphereRadius,
                        Math.cos(phi) * sphereRadius,
                        Math.sin(theta) * Math.sin(phi) * sphereRadius);
                    sphereHandles.get(i).entity().teleport(loc);
                }

                // Pull players inward
                if (ticksAlive % 2 == 0) {
                    for (org.bukkit.entity.Player player : w.getPlayers()) {
                        if (isExempt(player)) continue;
                        double dist = player.getLocation().distance(center);
                        if (dist < 20 && dist > 2) {
                            org.bukkit.util.Vector pull = center.toVector()
                                .subtract(player.getLocation().toVector()).normalize().multiply(0.2);
                            player.setVelocity(player.getVelocity().add(pull));
                        }
                    }
                }

                // Core shrinks to singularity
                if (coreHandle != null && coreHandle.entity().isValid()) {
                    float coreScale = (float)(sphereRadius * 0.3);
                    float halfCore = coreScale / 2;
                    coreHandle.entity().setTransformation(new Transformation(
                        new Vector3f(-halfCore, -halfCore, -halfCore),
                        new AxisAngle4f((float)(ticksAlive * 0.2), 0, 1, 0),
                        new Vector3f(coreScale, coreScale, coreScale),
                        new AxisAngle4f(0, 0, 1, 0)
                    ));
                    coreHandle.entity().setInterpolationDuration(1);
                    coreHandle.entity().setInterpolationDelay(0);
                }

                // Collapse particles
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.dustParticles(center, 15, sphereRadius, 128, 0, 255, 2.0f);
                }
            }

            // PHASE 3: Detonation (tick 120)
            if (ticksAlive == 120 && !detonated) {
                detonated = true;
                // Massive arena-wide damage
                triggerImpactDamage(center);

                // Visual explosion
                DisplayBuilder.dustParticles(center, 60, 8.0, 80, 0, 160, 3.0f);
                DisplayBuilder.dustParticles(center, 40, 6.0, 128, 0, 255, 2.5f);
                DisplayBuilder.dustParticles(center, 30, 5.0, 120, 0, 30, 2.0f);

                // Knockback everyone
                for (org.bukkit.entity.Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (dist < 20) {
                        double force = 3.0 * (1.0 - dist / 20.0);
                        org.bukkit.util.Vector push = player.getLocation().toVector()
                            .subtract(center.toVector()).normalize().multiply(force);
                        push.setY(1.2);
                        player.setVelocity(push);
                    }
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            sphereHandles.clear();
            coreHandle = null;
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() { return new AbsoluteVoidNova(plugin); }
    }
}
